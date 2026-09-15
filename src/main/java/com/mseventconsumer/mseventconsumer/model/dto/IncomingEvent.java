package com.mseventconsumer.mseventconsumer.model.dto;

import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.util.HexFormat;

public record IncomingEvent(String topic, int partition, long offset, String payload,
                            RecordReadyEvent event, String decodingError) {
    public String rejectionId() {
        try {
            var digest = MessageDigest.getInstance("SHA-256");
            String identity = topic + "\n" + partition + "\n" + offset + "\n" + payload;
            return HexFormat.of().formatHex(digest.digest(identity.getBytes(StandardCharsets.UTF_8)));
        } catch (NoSuchAlgorithmException e) {
            throw new IllegalStateException(e);
        }
    }
}
