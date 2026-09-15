package com.mseventconsumer.mseventconsumer.model.entity;

import jakarta.persistence.*;
import lombok.*;
import java.time.LocalDateTime;

@Entity
@Table(name="consumed_event")
@Getter
@NoArgsConstructor
public class ConsumedEvent {

 @Id
 @Column(length=36)
 private String eventId;

 @Column(nullable=false)
 private Long recordId;

 @Column(nullable=false)
 private LocalDateTime completedAt;

 public ConsumedEvent(String eventId, Long recordId) {
  this.eventId = eventId;
  this.recordId = recordId;
  this.completedAt = LocalDateTime.now(java.time.ZoneOffset.UTC);
 }

}
