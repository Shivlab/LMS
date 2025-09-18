package com.mybank.lms.repository;

import com.mybank.lms.model.entity.RateResetAuditEntity;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.UUID;

@Repository
public interface RateResetAuditRepository extends JpaRepository<RateResetAuditEntity, UUID> {
    
    List<RateResetAuditEntity> findByLoanIdOrderByResetDateDesc(UUID loanId);
    
    List<RateResetAuditEntity> findByLoanId(UUID loanId);
}
