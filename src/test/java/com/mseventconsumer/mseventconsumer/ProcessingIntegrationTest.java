package com.mseventconsumer.mseventconsumer;

import com.mseventconsumer.mseventconsumer.model.dto.RecordReadyEvent;
import com.mseventconsumer.mseventconsumer.model.dto.IncomingEvent;
import com.mseventconsumer.mseventconsumer.model.entity.*;
import com.mseventconsumer.mseventconsumer.model.enums.RecordStatus;
import com.mseventconsumer.mseventconsumer.repository.IBatchRecordRepository;
import com.mseventconsumer.mseventconsumer.service.IRecordProcessingService;
import com.mseventconsumer.mseventconsumer.util.ProcessingSimulator;
import org.junit.jupiter.api.*;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.test.mock.mockito.SpyBean;
import org.springframework.jdbc.core.JdbcTemplate;
import java.time.LocalDateTime;
import java.util.*;
import java.util.concurrent.*;
import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.*;

@SpringBootTest
class ProcessingIntegrationTest {

 @Autowired IRecordProcessingService service;
 @Autowired IBatchRecordRepository records;
 @Autowired JdbcTemplate jdbc;
 @SpyBean ProcessingSimulator processor;

 @BeforeEach void clean() {jdbc.update("delete from rejected_event");jdbc.update("delete from consumed_event");records.deleteAll();reset(processor);}
 BatchRecord record(String xml) {
  var r=new BatchRecord();r.setBusinessKey(1L);r.setUploadId(UUID.randomUUID().toString());r.setSourceName("test.xml");r.setPayload(xml);r.setStatus(RecordStatus.PENDING);r.setCreatedAt(LocalDateTime.now());r.setUpdatedAt(LocalDateTime.now());return records.saveAndFlush(r);
 }

 IncomingEvent event(BatchRecord r) {
  var event = new RecordReadyEvent(UUID.randomUUID().toString(),2,r.getId(),r.getBusinessKey(),r.getUploadId(),LocalDateTime.now());
  return new IncomingEvent("test",0,r.getId(),event.toString(),event,null);
 }

 long inbox() {return jdbc.queryForObject("select count(*) from consumed_event",Long.class);}

 @Test void duplicateWithinBatchAndRedelivery() {
  var r=record("<root/>");var e=event(r);service.process(List.of(e,e));service.process(List.of(e));
  assertEquals(RecordStatus.PROCESSED,records.findById(r.getId()).orElseThrow().getStatus());assertEquals(1,inbox());verify(processor,times(1)).process("<root/>");
 }

 @Test void businessFailureCommits() {
  var r=record("<root>");service.process(List.of(event(r)));
  assertEquals(RecordStatus.FAILED,records.findById(r.getId()).orElseThrow().getStatus());assertEquals(1,inbox());
 }

 @Test void technicalFailureRollsBackWholeBatch() {
  var a=record("<root/>");var b=record("<root>fail</root>");
  doThrow(new IllegalStateException("temporary")).when(processor).process(b.getPayload());
  assertThrows(IllegalStateException.class,()->service.process(List.of(event(a),event(b))));
  assertEquals(0,inbox());assertEquals(RecordStatus.PENDING,records.findById(a.getId()).orElseThrow().getStatus());
 }

 @Test void differentEventForTerminalRecordDoesNotProcessAgain() {
  var r=record("<root/>");service.process(List.of(event(r)));service.process(List.of(event(r)));
  assertEquals(2,inbox());verify(processor,times(1)).process("<root/>");
 }

 @Test void mismatchIsQuarantinedWithoutChangingRecord() {
  var r=record("<root/>");var e=event(r).event();
  var wrong=new RecordReadyEvent(e.eventId(),2,e.recordId(),2L,e.uploadId(),e.occurredAt());
  service.process(List.of(new IncomingEvent("test",0,1,"wrong",wrong,null)));
  assertEquals(0,inbox());verifyNoInteractions(processor);
  assertEquals(1,jdbc.queryForObject("select count(*) from rejected_event",Integer.class));
  assertEquals(RecordStatus.PENDING,records.findById(r.getId()).orElseThrow().getStatus());
 }

 @Test void orphanAndMalformedDoNotBlockValidRecord() {
  var r=record("<root/>");
  var missing=new RecordReadyEvent(UUID.randomUUID().toString(),2,999999L,1L,UUID.randomUUID().toString(),LocalDateTime.now());
  var orphan=new IncomingEvent("test",0,11,"orphan",missing,null);
  var malformed=new IncomingEvent("test",0,12,"{broken",null,"Invalid JSON");
  service.process(List.of(orphan,malformed,event(r)));
  service.process(List.of(orphan,malformed));
  assertEquals(2,jdbc.queryForObject("select count(*) from rejected_event",Integer.class));
  assertEquals(1,inbox());
  assertEquals(RecordStatus.PROCESSED,records.findById(r.getId()).orElseThrow().getStatus());
 }

 @Test void quarantineRollsBackWithTechnicalFailure() {
  var r=record("<root/>");
  doThrow(new IllegalStateException("database/network failure")).when(processor).process(anyString());
  var invalid=new IncomingEvent("test",0,1,"bad",null,"Invalid JSON");
  assertThrows(IllegalStateException.class,()->service.process(List.of(invalid,event(r))));
  assertEquals(0,jdbc.queryForObject("select count(*) from rejected_event",Integer.class));
  assertEquals(0,inbox());
 }

 @Test void concurrentDuplicateTransactions() throws Exception {
  var r=record("<root/>");var e=event(r);var start=new CountDownLatch(1);
  try(var pool=Executors.newFixedThreadPool(3)) {
   var tasks=new ArrayList<Future<?>>();
   for(int i=0;i<3;i++) tasks.add(pool.submit(()->{try{start.await();service.process(List.of(e));}catch(InterruptedException x){throw new RuntimeException(x);}}));
   start.countDown();for(var t:tasks)t.get(10,TimeUnit.SECONDS);
  }

  assertEquals(1,inbox());verify(processor,times(1)).process("<root/>");

 }

}
