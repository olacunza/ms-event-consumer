package com.mseventconsumer.mseventconsumer.model.enums;

public enum RecordStatus {
    PENDING,
    PROCESSED,
    FAILED;

    public boolean isTerminal() {
        return this == PROCESSED || this == FAILED;
    }
}
