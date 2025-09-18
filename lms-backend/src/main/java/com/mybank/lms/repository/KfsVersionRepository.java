package com.mybank.lms.repository;

import com.mybank.lms.model.entity.KfsVersionEntity;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

@Repository
public interface KfsVersionRepository extends JpaRepository<KfsVersionEntity, UUID> {
    
    List<KfsVersionEntity> findByLoanIdOrderByVersionNumberDesc(UUID loanId);
    
    @Query("SELECT k FROM KfsVersionEntity k WHERE k.loan.id = :loanId ORDER BY k.versionNumber DESC LIMIT 1")
    Optional<KfsVersionEntity> findLatestByLoanId(@Param("loanId") UUID loanId);
    
    @Query("SELECT COALESCE(MAX(k.versionNumber), 0) FROM KfsVersionEntity k WHERE k.loan.id = :loanId")
    Integer findMaxVersionNumberByLoanId(@Param("loanId") UUID loanId);
    
    Optional<KfsVersionEntity> findByLoanIdAndVersionNumber(UUID loanId, Integer versionNumber);
    
    List<KfsVersionEntity> findByTriggerReason(KfsVersionEntity.TriggerReason triggerReason);
}
