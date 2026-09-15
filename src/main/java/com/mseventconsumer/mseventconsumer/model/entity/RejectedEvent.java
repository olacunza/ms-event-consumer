package com.mseventconsumer.mseventconsumer.model.entity;

import com.mseventconsumer.mseventconsumer.model.dto.IncomingEvent;
import jakarta.persistence.*;
import lombok.Getter;
import lombok.NoArgsConstructor;
import java.time.LocalDateTime;
import java.time.ZoneOffset;

@Entity
@Table(name = "rejected_event")
@Getter
@NoArgsConstructor
public class RejectedEvent {

    @Id
    @Column(length = 64)
    private String id;

    @Column(nullable = false, length = 249)
    private String topic;

    @Column(name = "partition_number", nullable = false)
    private int partition;

    @Column(name = "record_offset", nullable = false)
    private long offset;

    @org.hibernate.annotations.Nationalized
    @Lob
    private String payload;

    @Column(nullable = false, length = 2000)
    private String reason;

    @Column(nullable = false)
    private LocalDateTime rejectedAt;


    public RejectedEvent(IncomingEvent incoming, String reason) {
        this.id = incoming.rejectionId();
        this.topic = incoming.topic();
        this.partition = incoming.partition();
        this.offset = incoming.offset();
        this.payload = incoming.payload();
        this.reason = reason.substring(0, Math.min(2000, reason.length()));
        this.rejectedAt = LocalDateTime.now(ZoneOffset.UTC);
    }
}
