package com.mybank.lms.service;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.mybank.lms.model.entity.LoanEntity;
import com.mybank.lms.model.entity.LoanVersionEntity;
import com.mybank.lms.repository.LoanRepository;
import com.mybank.lms.repository.LoanVersionRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.UUID;

@Service
@RequiredArgsConstructor
@Slf4j
public class LoanVersionService {
    
    private final LoanVersionRepository loanVersionRepository;
    private final LoanRepository loanRepository;
    private final ObjectMapper objectMapper;
    
    @Transactional
    public LoanVersionEntity createLoanVersion(UUID loanId, LoanVersionEntity.ChangeReason changeReason,
                                             String changeDescription, String createdBy,
                                             Map<String, Object> changedFields) {
        log.info("Creating loan version for loan: {} with reason: {}", loanId, changeReason);
        
        LoanEntity loan = loanRepository.findById(loanId)
            .orElseThrow(() -> new RuntimeException("Loan not found: " + loanId));
        
        Integer nextVersion = getNextVersionNumber(loanId);
        
        LoanVersionEntity loanVersion = new LoanVersionEntity();
        loanVersion.setLoan(loan);
        loanVersion.setVersionNumber(nextVersion);
        loanVersion.setChangeReason(changeReason);
        loanVersion.setChangeDescription(changeDescription);
        loanVersion.setCreatedBy(createdBy);
        loanVersion.setEffectiveFrom(LocalDateTime.now());
        
        // Copy current loan state
        copyLoanStateToVersion(loan, loanVersion);
        
        // Store changed fields information
        if (changedFields != null && !changedFields.isEmpty()) {
            try {
                loanVersion.setChangedFields(objectMapper.writeValueAsString(changedFields.keySet()));
                loanVersion.setPreviousValues(objectMapper.writeValueAsString(changedFields));
            } catch (Exception e) {
                log.error("Failed to serialize changed fields for loan: {}", loanId, e);
            }
        }
        
        loanVersion = loanVersionRepository.save(loanVersion);
        log.info("Loan version {} created for loan: {}", nextVersion, loanId);
        
        return loanVersion;
    }
    
    @Transactional
    public LoanVersionEntity createInitialVersion(LoanEntity loan, String createdBy) {
        log.info("Creating initial version for loan: {}", loan.getId());
        
        LoanVersionEntity initialVersion = new LoanVersionEntity();
        initialVersion.setLoan(loan);
        initialVersion.setVersionNumber(1);
        initialVersion.setChangeReason(LoanVersionEntity.ChangeReason.INITIAL_CREATION);
        initialVersion.setChangeDescription("Initial loan creation");
        initialVersion.setCreatedBy(createdBy);
        initialVersion.setEffectiveFrom(LocalDateTime.now());
        
        copyLoanStateToVersion(loan, initialVersion);
        
        return loanVersionRepository.save(initialVersion);
    }
    
    private void copyLoanStateToVersion(LoanEntity loan, LoanVersionEntity version) {
        version.setPrincipal(loan.getPrincipal());
        version.setAnnualRate(loan.getAnnualRate());
        version.setMonths(loan.getMonths());
        version.setMoratoriumMonths(loan.getMoratoriumMonths());
        version.setMoratoriumType(loan.getMoratoriumType());
        version.setPartialPaymentEmi(loan.getPartialPaymentEmi());
        version.setRateType(loan.getRateType());
        version.setFloatingStrategy(loan.getFloatingStrategy());
        version.setCompoundingFrequency(loan.getCompoundingFrequency());
        version.setResetPeriodicityMonths(loan.getResetPeriodicityMonths());
        version.setBenchmarkName(loan.getBenchmarkName());
        version.setSpread(loan.getSpread());
        version.setLoanIssueDate(loan.getLoanIssueDate());
        version.setStartDate(loan.getStartDate());
        version.setProductType(loan.getProductType());
        version.setCustomerId(loan.getCustomerId());
    }
    
    public List<LoanVersionEntity> getLoanVersionHistory(UUID loanId) {
        return loanVersionRepository.findByLoanIdOrderByVersionNumberDesc(loanId);
    }
    
    public Optional<LoanVersionEntity> getLatestLoanVersion(UUID loanId) {
        return loanVersionRepository.findLatestByLoanId(loanId);
    }
    
    public Optional<LoanVersionEntity> getLoanVersion(UUID loanId, Integer versionNumber) {
        return loanVersionRepository.findByLoanIdAndVersionNumber(loanId, versionNumber);
    }
    
    public Optional<LoanVersionEntity> getCurrentEffectiveVersion(UUID loanId) {
        return loanVersionRepository.findCurrentEffectiveVersion(loanId);
    }
    
    private Integer getNextVersionNumber(UUID loanId) {
        Integer maxVersion = loanVersionRepository.findMaxVersionNumberByLoanId(loanId);
        return maxVersion + 1;
    }
    
    public Map<String, Object> compareVersions(LoanVersionEntity oldVersion, LoanVersionEntity newVersion) {
        Map<String, Object> differences = new HashMap<>();
        
        if (!oldVersion.getPrincipal().equals(newVersion.getPrincipal())) {
            differences.put("principal", Map.of("old", oldVersion.getPrincipal(), "new", newVersion.getPrincipal()));
        }
        if (!oldVersion.getAnnualRate().equals(newVersion.getAnnualRate())) {
            differences.put("annualRate", Map.of("old", oldVersion.getAnnualRate(), "new", newVersion.getAnnualRate()));
        }
        if (!oldVersion.getSpread().equals(newVersion.getSpread())) {
            differences.put("spread", Map.of("old", oldVersion.getSpread(), "new", newVersion.getSpread()));
        }
        if (!oldVersion.getMonths().equals(newVersion.getMonths())) {
            differences.put("months", Map.of("old", oldVersion.getMonths(), "new", newVersion.getMonths()));
        }
        
        return differences;
    }
}
