package com.mybank.lms.repository;

import com.mybank.lms.model.entity.RepaymentSnapshotEntity;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

@Repository
public interface RepaymentSnapshotRepository extends JpaRepository<RepaymentSnapshotEntity, UUID> {
    
    List<RepaymentSnapshotEntity> findByLoanIdOrderBySnapshotDateDesc(UUID loanId);
    
    @Query("SELECT rs FROM RepaymentSnapshotEntity rs WHERE rs.loan.id = :loanId ORDER BY rs.version DESC LIMIT 1")
    Optional<RepaymentSnapshotEntity> findLatestByLoanId(@Param("loanId") UUID loanId);
    
    @Query("SELECT MAX(rs.version) FROM RepaymentSnapshotEntity rs WHERE rs.loan.id = :loanId")
    Integer findMaxVersionByLoanId(@Param("loanId") UUID loanId);
    
    @Query("SELECT rs FROM RepaymentSnapshotEntity rs WHERE rs.loan.id = :loanId AND rs.version = :version")
    Optional<RepaymentSnapshotEntity> findByLoanIdAndVersion(@Param("loanId") UUID loanId, @Param("version") Integer version);
    
    @Query("SELECT rs FROM RepaymentSnapshotEntity rs WHERE rs.loan.id = :loanId AND rs.snapshotDate = (SELECT MAX(rs2.snapshotDate) FROM RepaymentSnapshotEntity rs2 WHERE rs2.loan.id = :loanId)")
    Optional<RepaymentSnapshotEntity> findMostRecentByLoanId(@Param("loanId") UUID loanId);
}
