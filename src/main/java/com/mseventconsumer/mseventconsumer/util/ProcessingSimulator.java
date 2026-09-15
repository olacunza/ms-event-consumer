package com.mseventconsumer.mseventconsumer.util;

import com.mseventconsumer.mseventconsumer.exception.BusinessException;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;

import javax.xml.XMLConstants;
import javax.xml.parsers.DocumentBuilderFactory;
import org.xml.sax.InputSource;
import org.xml.sax.SAXException;
import org.xml.sax.helpers.DefaultHandler;
import java.io.StringReader;

@Component
public class ProcessingSimulator {

 private final long delayMs;

 public ProcessingSimulator(@Value("${app.processing.delay-ms:10}") long delayMs) {

  if (delayMs < 0 || delayMs > 1000)
   throw new IllegalArgumentException("delay-ms debe estar entre 0 y 1000");

  this.delayMs = delayMs;

 }

 public void process(String xml) {

  if(xml == null || xml.length()>1048576)
   throw new BusinessException("XML ausente o demasiado grande");

  try {
   var f=DocumentBuilderFactory.newInstance();
   f.setFeature(XMLConstants.FEATURE_SECURE_PROCESSING,true);
   f.setFeature("http://apache.org/xml/features/disallow-doctype-decl",true);
   f.setFeature("http://xml.org/sax/features/external-general-entities",false);
   f.setFeature("http://xml.org/sax/features/external-parameter-entities",false);
   f.setAttribute(XMLConstants.ACCESS_EXTERNAL_DTD, ""); f.setAttribute(XMLConstants.ACCESS_EXTERNAL_SCHEMA, "");
   f.setXIncludeAware(false); f.setExpandEntityReferences(false);

   var parser=f.newDocumentBuilder();

   parser.setErrorHandler(new DefaultHandler(){ @Override public void fatalError(org.xml.sax.SAXParseException e) throws SAXException {throw e;} });

   var doc=parser.parse(new InputSource(new StringReader(xml)));

   if(!"root".equals(doc.getDocumentElement().getTagName()))
    throw new BusinessException("La raiz debe ser root");

  } catch(SAXException | java.io.IOException e) {
   throw new BusinessException("XML invalido o no permitido");
  } catch(javax.xml.parsers.ParserConfigurationException e) {
   throw new IllegalStateException("No se pudo configurar el parser seguro",e);
  }

  try {
   Thread.sleep(delayMs);
  } catch(InterruptedException e) {
   Thread.currentThread().interrupt();
   throw new IllegalStateException("Procesamiento interrumpido",e);
  }

 }

}
