package com.mybank.lms.repository;

import com.mybank.lms.model.entity.DisbursementPhaseEntity;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.UUID;

@Repository
public interface DisbursementRepository extends JpaRepository<DisbursementPhaseEntity, UUID> {
    
    List<DisbursementPhaseEntity> findByLoanIdOrderBySequence(UUID loanId);
    
    List<DisbursementPhaseEntity> findByLoanIdOrderByDisbursementDate(UUID loanId);
}
