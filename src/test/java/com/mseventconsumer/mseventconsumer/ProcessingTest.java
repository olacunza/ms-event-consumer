package com.mseventconsumer.mseventconsumer;
import com.mseventconsumer.mseventconsumer.util.ProcessingSimulator;
import com.mseventconsumer.mseventconsumer.exception.BusinessException;
import org.junit.jupiter.api.Test;
import static org.junit.jupiter.api.Assertions.*;

class ProcessingTest {

 private final ProcessingSimulator processor=new ProcessingSimulator(0);

 @Test void valid() {assertDoesNotThrow(() -> processor.process("<root><value>1</value></root>"));}
 @Test void malformed() {assertThrows(BusinessException.class,()->processor.process("<root>"));}
 @Test void wrongRoot() {assertThrows(BusinessException.class,()->processor.process("<other/>"));}
 @Test void xxe() {assertThrows(BusinessException.class,()->processor.process("<!DOCTYPE root [<!ENTITY x SYSTEM 'file:///etc/passwd'>]><root>&x;</root>"));}
 @Test void boundedDelay() {assertThrows(IllegalArgumentException.class,()->new ProcessingSimulator(1001));}

}
