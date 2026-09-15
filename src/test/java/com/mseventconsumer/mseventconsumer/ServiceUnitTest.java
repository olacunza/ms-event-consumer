package com.mseventconsumer.mseventconsumer;

import com.mseventconsumer.mseventconsumer.service.impl.*;
import com.mseventconsumer.mseventconsumer.repository.IBatchRecordRepository;
import com.mseventconsumer.mseventconsumer.util.ProcessingSimulator;
import com.mseventconsumer.mseventconsumer.model.dto.RecordReadyEvent;
import jakarta.persistence.EntityManager;
import com.mseventconsumer.mseventconsumer.model.dto.IncomingEvent;
import com.mseventconsumer.mseventconsumer.model.entity.RejectedEvent;
import org.junit.jupiter.api.Test;
import java.time.LocalDateTime;
import java.util.*;
import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.*;

class ServiceUnitTest {

 @Test void repositoryFailureIsNotConvertedToBusinessFailure() {
  var repo=mock(IBatchRecordRepository.class);var em=mock(EntityManager.class);var processor=mock(ProcessingSimulator.class);
  var service=new RecordProcessingServiceImpl(repo,em,processor,new RecordStatusServiceImpl());
  when(repo.lockById(1L)).thenThrow(new IllegalStateException("database unavailable"));
  var e=new RecordReadyEvent(UUID.randomUUID().toString(),2,1L,1L,UUID.randomUUID().toString(),LocalDateTime.now());
  assertThrows(IllegalStateException.class,()->service.process(List.of(new IncomingEvent("test",0,1,"json",e,null))));verifyNoInteractions(em,processor);
 }

 @Test void unsupportedVersionDoesNotReachDatabase() {
  var repo=mock(IBatchRecordRepository.class);var em=mock(EntityManager.class);var processor=mock(ProcessingSimulator.class);
  var service=new RecordProcessingServiceImpl(repo,em,processor,new RecordStatusServiceImpl());
  var e=new RecordReadyEvent(UUID.randomUUID().toString(),1,1L,1L,UUID.randomUUID().toString(),LocalDateTime.now());
  service.process(List.of(new IncomingEvent("test",0,1,"json",e,null)));verifyNoInteractions(repo,processor);verify(em).persist(any(RejectedEvent.class));
 }

}
