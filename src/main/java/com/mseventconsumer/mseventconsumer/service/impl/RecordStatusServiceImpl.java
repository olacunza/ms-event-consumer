package com.mseventconsumer.mseventconsumer.service.impl;

import com.mseventconsumer.mseventconsumer.service.IRecordStatusService;
import com.mseventconsumer.mseventconsumer.model.entity.BatchRecord;
import com.mseventconsumer.mseventconsumer.model.enums.RecordStatus;
import org.springframework.stereotype.Service;
import java.time.*;

@Service
public class RecordStatusServiceImpl implements IRecordStatusService {

 public void complete(BatchRecord record, String error) {
  record.setStatus(error == null ? RecordStatus.PROCESSED : RecordStatus.FAILED);
  record.setErrorMessage(error == null ? null : error.substring(0,Math.min(error.length(),2000)));
  record.setUpdatedAt(LocalDateTime.now(ZoneOffset.UTC));
 }

}
