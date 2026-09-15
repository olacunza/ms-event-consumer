package com.mseventconsumer.mseventconsumer.service;

import com.mseventconsumer.mseventconsumer.model.dto.IncomingEvent;
import java.util.List;

public interface IRecordProcessingService {
    void process(List<IncomingEvent> events);
}
