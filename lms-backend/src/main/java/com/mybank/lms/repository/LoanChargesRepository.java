package com.mybank.lms.repository;

import com.mybank.lms.model.entity.LoanChargeEntity;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.UUID;

@Repository
public interface LoanChargesRepository extends JpaRepository<LoanChargeEntity, UUID> {
    
    List<LoanChargeEntity> findByLoanId(UUID loanId);
    
    List<LoanChargeEntity> findByLoanIdAndIsRecurring(UUID loanId, Boolean isRecurring);
}
