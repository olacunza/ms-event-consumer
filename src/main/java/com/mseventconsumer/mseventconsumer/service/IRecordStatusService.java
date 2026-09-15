package com.mseventconsumer.mseventconsumer.service;

import com.mseventconsumer.mseventconsumer.model.entity.BatchRecord;

public interface IRecordStatusService {

    void complete(BatchRecord record, String error);

}
