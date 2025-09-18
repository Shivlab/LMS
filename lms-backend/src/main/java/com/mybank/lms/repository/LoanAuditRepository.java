package com.mybank.lms.repository;

import com.mybank.lms.model.entity.LoanAuditEntity;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.UUID;

@Repository
public interface LoanAuditRepository extends JpaRepository<LoanAuditEntity, UUID> {
    
    List<LoanAuditEntity> findByLoanIdOrderByChangeDateDesc(UUID loanId);
    
    @Query("SELECT la FROM LoanAuditEntity la WHERE la.loan.id = :loanId AND la.fieldName = :fieldName ORDER BY la.changeDate DESC")
    List<LoanAuditEntity> findByLoanIdAndFieldNameOrderByChangeDateDesc(@Param("loanId") UUID loanId, @Param("fieldName") String fieldName);
}
