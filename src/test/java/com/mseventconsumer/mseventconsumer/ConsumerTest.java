package com.mseventconsumer.mseventconsumer;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.mseventconsumer.mseventconsumer.messaging.consumer.RecordReadyEventConsumer;
import com.mseventconsumer.mseventconsumer.service.IRecordProcessingService;
import org.apache.kafka.clients.consumer.ConsumerRecord;
import org.junit.jupiter.api.Test;
import java.util.List;
import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.*;
class ConsumerTest {
 @Test void malformedJsonIsPassedToDurableQuarantine() {
  var service=mock(IRecordProcessingService.class);var consumer=new RecordReadyEventConsumer(new ObjectMapper(),service);
  consumer.consume(List.of(new ConsumerRecord<>("test",1,42L,"key","invalid")));
  verify(service).process(argThat(items -> items.size()==1 && items.getFirst().offset()==42
    && items.getFirst().decodingError()!=null && items.getFirst().payload().equals("invalid")));
 }
 @Test void sqlFailurePropagatesToKafka() {
  var service=mock(IRecordProcessingService.class);var consumer=new RecordReadyEventConsumer(new ObjectMapper(),service);
  doThrow(new IllegalStateException("SQL unavailable")).when(service).process(anyList());
  assertThrows(IllegalStateException.class,()->consumer.consume(List.of(new ConsumerRecord<>("test",0,1L,"key","{}"))));
 }
 @Test void tombstoneIsPassedToQuarantine() {
  var service=mock(IRecordProcessingService.class);var consumer=new RecordReadyEventConsumer(new ObjectMapper(),service);
  consumer.consume(List.of(new ConsumerRecord<>("test",0,2L,"key",null)));
  verify(service).process(argThat(items -> items.getFirst().decodingError()!=null));
 }
}
