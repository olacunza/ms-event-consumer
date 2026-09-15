package com.mseventconsumer.mseventconsumer.repository;

import com.mseventconsumer.mseventconsumer.model.entity.BatchRecord;
import jakarta.persistence.LockModeType;
import org.springframework.data.jpa.repository.*;
import org.springframework.data.repository.query.Param;
import java.util.Optional;

public interface IBatchRecordRepository extends JpaRepository<BatchRecord, Long> {

 @Lock(LockModeType.PESSIMISTIC_WRITE)
 @Query("select r from BatchRecord r where r.id = :id")
 Optional<BatchRecord> lockById(@Param("id") Long id);

}
