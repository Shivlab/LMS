package com.mybank.lms.service;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.mybank.lms.model.dto.LoanOutputDTO;
import com.mybank.lms.model.entity.KfsVersionEntity;
import com.mybank.lms.model.entity.LoanEntity;
import com.mybank.lms.repository.KfsVersionRepository;
import com.mybank.lms.repository.LoanRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;
import java.util.UUID;

@Service
@RequiredArgsConstructor
@Slf4j
public class KfsVersionService {
    
    private final KfsVersionRepository kfsVersionRepository;
    private final LoanRepository loanRepository;
    private final ObjectMapper objectMapper;
    
    @Transactional
    public KfsVersionEntity createKfsVersion(UUID loanId, LoanOutputDTO kfsData, 
                                           KfsVersionEntity.TriggerReason triggerReason, 
                                           String memo, String createdBy) {
        log.info("Creating KFS version for loan: {} with trigger: {}", loanId, triggerReason);
        
        LoanEntity loan = loanRepository.findById(loanId)
            .orElseThrow(() -> new RuntimeException("Loan not found: " + loanId));
        
        Integer nextVersion = getNextVersionNumber(loanId);
        
        KfsVersionEntity kfsVersion = new KfsVersionEntity();
        kfsVersion.setLoan(loan);
        kfsVersion.setVersionNumber(nextVersion);
        kfsVersion.setTriggerReason(triggerReason);
        kfsVersion.setMemo(memo);
        kfsVersion.setCreatedBy(createdBy);
        
        // Store current loan parameters
        kfsVersion.setGeneratedForRate(loan.getAnnualRate());
        kfsVersion.setGeneratedForSpread(loan.getSpread());
        kfsVersion.setBenchmarkName(loan.getBenchmarkName());
        
        if (kfsData.getApr() != null) {
            kfsVersion.setGeneratedForApr(kfsData.getApr());
        }
        
        // Serialize KFS data to JSON
        try {
            String kfsJson = objectMapper.writeValueAsString(kfsData);
            kfsVersion.setKfsData(kfsJson);
        } catch (Exception e) {
            log.error("Failed to serialize KFS data for loan: {}", loanId, e);
            throw new RuntimeException("Failed to serialize KFS data", e);
        }
        
        kfsVersion = kfsVersionRepository.save(kfsVersion);
        log.info("KFS version {} created for loan: {}", nextVersion, loanId);
        
        return kfsVersion;
    }
    
    public List<KfsVersionEntity> getKfsVersionHistory(UUID loanId) {
        return kfsVersionRepository.findByLoanIdOrderByVersionNumberDesc(loanId);
    }
    
    public Optional<KfsVersionEntity> getLatestKfsVersion(UUID loanId) {
        return kfsVersionRepository.findLatestByLoanId(loanId);
    }
    
    public Optional<KfsVersionEntity> getKfsVersion(UUID loanId, Integer versionNumber) {
        return kfsVersionRepository.findByLoanIdAndVersionNumber(loanId, versionNumber);
    }
    
    public LoanOutputDTO deserializeKfsData(KfsVersionEntity kfsVersion) {
        try {
            return objectMapper.readValue(kfsVersion.getKfsData(), LoanOutputDTO.class);
        } catch (Exception e) {
            log.error("Failed to deserialize KFS data for version: {}", kfsVersion.getId(), e);
            throw new RuntimeException("Failed to deserialize KFS data", e);
        }
    }
    
    private Integer getNextVersionNumber(UUID loanId) {
        Integer maxVersion = kfsVersionRepository.findMaxVersionNumberByLoanId(loanId);
        return maxVersion + 1;
    }
    
    @Transactional
    public void deleteKfsVersion(UUID kfsVersionId) {
        log.info("Deleting KFS version: {}", kfsVersionId);
        kfsVersionRepository.deleteById(kfsVersionId);
    }
    
    public List<KfsVersionEntity> getKfsVersionsByTrigger(KfsVersionEntity.TriggerReason triggerReason) {
        return kfsVersionRepository.findByTriggerReason(triggerReason);
    }
}
