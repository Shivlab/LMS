package com.mybank.lms.repository;

import com.mybank.lms.model.entity.RepaymentRowEntity;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.UUID;

@Repository
public interface RepaymentRowRepository extends JpaRepository<RepaymentRowEntity, UUID> {
    
    List<RepaymentRowEntity> findBySnapshotIdOrderByMonthNumber(UUID snapshotId);
    
    List<RepaymentRowEntity> findBySnapshotId(UUID snapshotId);
}
