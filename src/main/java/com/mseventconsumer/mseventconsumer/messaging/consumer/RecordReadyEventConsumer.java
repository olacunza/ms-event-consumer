package com.mseventconsumer.mseventconsumer.messaging.consumer;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.mseventconsumer.mseventconsumer.model.dto.IncomingEvent;
import com.mseventconsumer.mseventconsumer.model.dto.RecordReadyEvent;
import com.mseventconsumer.mseventconsumer.service.IRecordProcessingService;
import org.apache.kafka.clients.consumer.ConsumerRecord;
import org.springframework.kafka.annotation.KafkaListener;
import org.springframework.stereotype.Component;
import java.util.ArrayList;
import java.util.List;

@Component
public class RecordReadyEventConsumer {
    private final ObjectMapper mapper;
    private final IRecordProcessingService service;

    public RecordReadyEventConsumer(ObjectMapper mapper, IRecordProcessingService service) {
        this.mapper = mapper;
        this.service = service;
    }

    @KafkaListener(topics = "${app.kafka.topic.record-ready}")
    public void consume(List<ConsumerRecord<String, String>> messages) {
        var events = new ArrayList<IncomingEvent>();
        for (var message : messages) {
            RecordReadyEvent event = null;
            String error = null;
            if (message.value() == null) {
                error = "Mensaje sin contenido (tombstone)";
            } else {
                try {
                    event = mapper.readValue(message.value(), RecordReadyEvent.class);
                } catch (JsonProcessingException e) {
                    error = "JSON incompatible con RecordReadyEvent v2";
                }
            }
            events.add(new IncomingEvent(message.topic(), message.partition(), message.offset(),
                    message.value(), event, error));
        }
        service.process(events);
    }
}
