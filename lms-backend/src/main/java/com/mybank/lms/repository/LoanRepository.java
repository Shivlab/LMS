package com.mybank.lms.repository;

import com.mybank.lms.model.entity.LoanEntity;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.UUID;

@Repository
public interface LoanRepository extends JpaRepository<LoanEntity, UUID> {
    
    List<LoanEntity> findByStatus(LoanEntity.LoanStatus status);
    
    @Query("SELECT l FROM LoanEntity l WHERE l.rateType = 'FLOATING' AND l.status = 'ACTIVE'")
    List<LoanEntity> findActiveFloatingLoans();
    
    @Query("SELECT l FROM LoanEntity l WHERE l.benchmarkName = :benchmarkName AND l.rateType = 'FLOATING' AND l.status = 'ACTIVE'")
    List<LoanEntity> findActiveFloatingLoansByBenchmark(@Param("benchmarkName") String benchmarkName);
    
    List<LoanEntity> findByCustomerId(String customerId);
    
    // Find all loans ordered by creation date (newest first)
    List<LoanEntity> findAllByOrderByCreatedAtDesc();
}
