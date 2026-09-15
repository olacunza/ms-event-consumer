package com.mseventconsumer.mseventconsumer.service.impl;

import com.mseventconsumer.mseventconsumer.service.*;
import com.mseventconsumer.mseventconsumer.model.dto.*;
import com.mseventconsumer.mseventconsumer.model.entity.*;
import com.mseventconsumer.mseventconsumer.model.enums.RecordStatus;
import com.mseventconsumer.mseventconsumer.repository.IBatchRecordRepository;
import com.mseventconsumer.mseventconsumer.util.ProcessingSimulator;
import com.mseventconsumer.mseventconsumer.exception.BusinessException;
import jakarta.persistence.EntityManager;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import java.util.*;

@Service
@RequiredArgsConstructor
@Slf4j
public class RecordProcessingServiceImpl implements IRecordProcessingService {
    private final IBatchRecordRepository records;
    private final EntityManager em;
    private final ProcessingSimulator processor;
    private final IRecordStatusService statuses;

    @Transactional(timeout = 60)
    public void process(List<IncomingEvent> messages) {
        var valid = new ArrayList<IncomingEvent>();
        for (var incoming : messages) {
            String error = incoming.decodingError() != null ? incoming.decodingError() : validationError(incoming.event());
            if (error != null) reject(incoming, error);
            else valid.add(incoming);
        }

        // A stable lock order limits deadlocks between batches and replicas.
        var locked = new HashMap<Long, BatchRecord>();
        valid.stream().map(item -> item.event().recordId()).distinct().sorted().forEach(id ->
                records.lockById(id).ifPresent(record -> locked.put(id, record)));

        int completed = 0;
        int duplicates = 0;
        for (var incoming : valid) {
            var event = incoming.event();
            var record = locked.get(event.recordId());
            if (record == null) {
                reject(incoming, "Registro inexistente: " + event.recordId());
                continue;
            }
            if (!Objects.equals(record.getUploadId(), event.uploadId())
                    || !Objects.equals(record.getBusinessKey(), event.businessKey())) {
                reject(incoming, "El evento no corresponde al registro " + event.recordId());
                continue;
            }
            var previous = em.find(ConsumedEvent.class, event.eventId());
            if (previous != null) {
                if (!previous.getRecordId().equals(event.recordId())) reject(incoming, "eventId reutilizado para otro registro");
                else duplicates++;
                continue;
            }
            if (record.getStatus() == RecordStatus.PENDING) {
                String error = null;
                try {
                    processor.process(record.getPayload());
                } catch (BusinessException e) {
                    error = e.getMessage();
                }
                statuses.complete(record, error);
                completed++;
            }
            em.persist(new ConsumedEvent(event.eventId(), event.recordId()));
        }
        em.flush();
        log.debug("Lote preparado: mensajes={}, procesados={}, duplicados={}", messages.size(), completed, duplicates);
    }

    private void reject(IncomingEvent incoming, String reason) {
        String id = incoming.rejectionId();
        if (em.find(RejectedEvent.class, id) == null) {
            em.persist(new RejectedEvent(incoming, reason));
            log.warn("Evento enviado a cuarentena: id={}, topic={}, partition={}, offset={}, motivo={}",
                    id, incoming.topic(), incoming.partition(), incoming.offset(), reason);
        }
    }

    private static String validationError(RecordReadyEvent e) {
        if (e == null || e.schemaVersion() != 2 || e.recordId() == null || e.recordId() <= 0
                || e.businessKey() == null || e.businessKey() <= 0 || e.occurredAt() == null) {
            return "Contrato de evento v2 invalido";
        }
        try {
            if (e.eventId() == null || e.uploadId() == null
                    || !UUID.fromString(e.eventId()).toString().equals(e.eventId())
                    || !UUID.fromString(e.uploadId()).toString().equals(e.uploadId())) return "UUID invalido";
        } catch (IllegalArgumentException ex) {
            return "UUID invalido";
        }
        return null;
    }
}
