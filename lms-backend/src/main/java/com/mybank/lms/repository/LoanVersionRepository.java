package com.mybank.lms.repository;

import com.mybank.lms.model.entity.LoanVersionEntity;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

@Repository
public interface LoanVersionRepository extends JpaRepository<LoanVersionEntity, UUID> {
    
    List<LoanVersionEntity> findByLoanIdOrderByVersionNumberDesc(UUID loanId);
    
    @Query("SELECT lv FROM LoanVersionEntity lv WHERE lv.loan.id = :loanId ORDER BY lv.versionNumber DESC LIMIT 1")
    Optional<LoanVersionEntity> findLatestByLoanId(@Param("loanId") UUID loanId);
    
    @Query("SELECT COALESCE(MAX(lv.versionNumber), 0) FROM LoanVersionEntity lv WHERE lv.loan.id = :loanId")
    Integer findMaxVersionNumberByLoanId(@Param("loanId") UUID loanId);
    
    Optional<LoanVersionEntity> findByLoanIdAndVersionNumber(UUID loanId, Integer versionNumber);
    
    List<LoanVersionEntity> findByChangeReason(LoanVersionEntity.ChangeReason changeReason);
    
    @Query("SELECT lv FROM LoanVersionEntity lv WHERE lv.loan.id = :loanId AND lv.effectiveFrom <= CURRENT_TIMESTAMP ORDER BY lv.versionNumber DESC LIMIT 1")
    Optional<LoanVersionEntity> findCurrentEffectiveVersion(@Param("loanId") UUID loanId);
}
