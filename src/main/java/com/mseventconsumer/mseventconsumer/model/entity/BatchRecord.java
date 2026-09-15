package com.mseventconsumer.mseventconsumer.model.entity;

import com.mseventconsumer.mseventconsumer.model.enums.RecordStatus;
import jakarta.persistence.*;
import lombok.*;
import java.time.LocalDateTime;

@Entity
@Table(name = "batch_record", indexes = @Index(name = "ix_record_upload_status", columnList = "upload_id,status"),
       uniqueConstraints = @UniqueConstraint(name = "uk_record_upload_key", columnNames = {"upload_id", "business_key"}))
@Getter
@Setter
@NoArgsConstructor
public class BatchRecord {

    @Id
    @GeneratedValue(strategy = GenerationType.SEQUENCE, generator = "record_ids")
    @SequenceGenerator(name = "record_ids", sequenceName = "batch_record_seq", allocationSize = 50)
    private Long id;

    @Column(nullable = false)
    private Long businessKey;

    @Column(nullable = false, length = 36)
    private String uploadId;

    @Column(nullable = false, length = 512)
    private String sourceName;

    @org.hibernate.annotations.Nationalized
    @Lob
    @Column(nullable = false)
    private String payload;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 20)
    private RecordStatus status;

    @Column(length = 2000)
    private String errorMessage;

    @Column(nullable = false)
    private LocalDateTime createdAt;

    @Column(nullable = false)
    private LocalDateTime updatedAt;

    @Version
    private long version;

}
