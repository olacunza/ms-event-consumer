package com.mseventconsumer.mseventconsumer.model.dto;

import java.time.LocalDateTime;
public record RecordReadyEvent(
        String eventId,
        int schemaVersion,
        Long recordId,
        Long businessKey,
        String uploadId,
        @com.fasterxml.jackson.annotation.JsonFormat(shape = com.fasterxml.jackson.annotation.JsonFormat.Shape.STRING) LocalDateTime occurredAt) {}
