package com.mseventconsumer.mseventconsumer.config;

import org.springframework.context.annotation.*;
import org.springframework.kafka.listener.DefaultErrorHandler;
import org.springframework.util.backoff.FixedBackOff;

@Configuration
public class KafkaConsumerConfig {

 @Bean
 public DefaultErrorHandler kafkaErrorHandler() {

  var handler = new DefaultErrorHandler(new FixedBackOff(2000L,FixedBackOff.UNLIMITED_ATTEMPTS));
  handler.setClassifications(java.util.Map.of(),true);
  handler.setRetryListeners(new org.springframework.kafka.listener.RetryListener() {
   private final org.slf4j.Logger log = org.slf4j.LoggerFactory.getLogger(KafkaConsumerConfig.class);

   @Override public void failedDelivery(org.apache.kafka.clients.consumer.ConsumerRecord<?, ?> record, Exception error, int attempt) {
    report(error, attempt);
   }

   @Override public void failedDelivery(org.apache.kafka.clients.consumer.ConsumerRecords<?, ?> records, Exception error, int attempt) {
    report(error, attempt);
   }

   private void report(Exception error, int attempt) {
    if (attempt == 1 || attempt % 30 == 0) log.error("Consumo fallido; intento {}. Los offsets siguen sin confirmar", attempt, error);
   }
  });

  return handler;

 }

}
