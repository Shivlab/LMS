package com.mybank.lms.service;

import com.mybank.lms.calculator.LoanInput;
import com.mybank.lms.calculator.LoanOutput;
import com.mybank.lms.calculator.HomeLoan;
import com.mybank.lms.model.dto.LoanInputDTO;
import com.mybank.lms.model.dto.LoanOutputDTO;
import com.mybank.lms.model.dto.LoanVersionDTO;
import com.mybank.lms.model.dto.KfsVersionDTO;
import com.mybank.lms.model.dto.RepaymentScheduleDTO;
import com.mybank.lms.model.entity.LoanEntity;
import com.mybank.lms.model.entity.LoanAuditEntity;
import com.mybank.lms.model.entity.LoanVersionEntity;
import com.mybank.lms.model.entity.KfsVersionEntity;
import com.mybank.lms.model.entity.DisbursementPhaseEntity;
import com.mybank.lms.model.entity.LoanChargeEntity;
import com.mybank.lms.model.entity.RepaymentSnapshotEntity;
import com.mybank.lms.repository.DisbursementRepository;
import com.mybank.lms.repository.LoanAuditRepository;
import com.mybank.lms.repository.LoanChargesRepository;
import com.mybank.lms.repository.LoanRepository;
import com.mybank.lms.service.AprCalculationService;
import com.mybank.lms.service.KfsVersionService;
import com.mybank.lms.service.LoanVersionService;
import com.mybank.lms.service.RepaymentSnapshotService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.*;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.UUID;
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
@Slf4j
public class LoanService {
    
    private final LoanRepository loanRepository;
    private final DisbursementRepository disbursementRepository;
    private final LoanChargesRepository loanChargesRepository;
    private final RepaymentSnapshotService repaymentSnapshotService;
    private final AprCalculationService aprCalculationService;
    private final LoanVersionService loanVersionService;
    private final KfsVersionService kfsVersionService;
    private final LoanAuditRepository loanAuditRepository;
    
    @Transactional
    public LoanOutputDTO createLoan(LoanInputDTO loanInputDTO) {
        log.info("Creating loan for customer: {}", loanInputDTO.getCustomerId());
        
        // Validate and set defaults
        validateAndSetDefaults(loanInputDTO);
        
        // Calculate repayment schedule FIRST
        LoanInput calculatorInput = mapToCalculatorInput(loanInputDTO);
        LoanOutput calculatorOutput = calculateLoanSchedule(calculatorInput, loanInputDTO);
        
        // Calculate APR
        BigDecimal apr = aprCalculationService.calculateAPR(calculatorInput, calculatorOutput, 
            loanInputDTO.getCharges());
        
        // Create loan entity AFTER calculations are successful
        LoanEntity loanEntity = mapToLoanEntity(loanInputDTO);
        loanEntity = loanRepository.save(loanEntity);
        
        // Save disbursement phases
        if (loanInputDTO.getDisbursementPhases() != null && !loanInputDTO.getDisbursementPhases().isEmpty()) {
            saveDisbursementPhases(loanEntity, loanInputDTO.getDisbursementPhases());
        }
        
        // Save charges
        if (loanInputDTO.getCharges() != null && !loanInputDTO.getCharges().isEmpty()) {
            saveLoanCharges(loanEntity, loanInputDTO.getCharges());
        }
        
        // Create initial repayment snapshot with calculated schedule
        repaymentSnapshotService.createInitialSnapshot(loanEntity, calculatorOutput, apr);
        
        // Create initial loan version
        loanVersionService.createInitialVersion(loanEntity, "system");
        
        // Create initial KFS version
        LoanOutputDTO initialKfs = mapToLoanOutputDTO(loanEntity);
        kfsVersionService.createKfsVersion(loanEntity.getId(), initialKfs, 
            KfsVersionEntity.TriggerReason.INITIAL_LOAN_CREATION, 
            "Initial KFS created with loan", "system");
        
        log.info("Loan created successfully with ID: {}", loanEntity.getId());
        return initialKfs;
    }
    
    private void validateAndSetDefaults(LoanInputDTO loanInputDTO) {
        // Set default dates if not provided
        if (loanInputDTO.getLoanIssueDate() == null) {
            loanInputDTO.setLoanIssueDate(LocalDate.now());
        }
        if (loanInputDTO.getStartDate() == null) {
            loanInputDTO.setStartDate(LocalDate.now());
        }
        
        // Set default moratorium type if not provided
        if (loanInputDTO.getMoratoriumType() == null) {
            loanInputDTO.setMoratoriumType("FULL");
        }
        
        // Set default floating strategy for floating rate loans
        if ("FLOATING".equals(loanInputDTO.getRateType()) && loanInputDTO.getFloatingStrategy() == null) {
            loanInputDTO.setFloatingStrategy("EMI_CONSTANT");
        }
        
        // Set default reset periodicity for floating loans
        if ("FLOATING".equals(loanInputDTO.getRateType()) && loanInputDTO.getResetPeriodicityMonths() == null) {
            loanInputDTO.setResetPeriodicityMonths(12);
        }
    }
    
    public List<LoanOutputDTO> getAllLoans() {
        log.info("Fetching all loans");
        
        List<LoanEntity> loanEntities = loanRepository.findAllByOrderByCreatedAtDesc();
        return loanEntities.stream()
            .map(this::mapToLoanOutputDTO)
            .collect(Collectors.toList());
    }
    
    public LoanOutputDTO getLoanKFS(UUID loanId) {
        log.info("Fetching KFS for loan: {}", loanId);
        
        LoanEntity loanEntity = loanRepository.findById(loanId)
            .orElseThrow(() -> new RuntimeException("Loan not found: " + loanId));
        
        // Return current KFS view without creating a new version on read
        return mapToLoanOutputDTO(loanEntity);
    }
    
    public LoanOutputDTO getLoanById(UUID id) {
        log.info("Fetching loan by ID: {}", id);
        
        LoanEntity loanEntity = loanRepository.findById(id)
            .orElseThrow(() -> new RuntimeException("Loan not found: " + id));
        
        // Fetch from persisted snapshot - NO regeneration on GET
        return mapToLoanOutputDTOFromSnapshot(loanEntity);
    }
    
    @Transactional
    public void applyBenchmarkToLoan(UUID loanId, String benchmarkName, BigDecimal newRate) {
        log.info("Applying benchmark {} with rate {} to loan: {}", benchmarkName, newRate, loanId);
        
        LoanEntity loanEntity = loanRepository.findById(loanId)
            .orElseThrow(() -> new RuntimeException("Loan not found: " + loanId));
        
        if (loanEntity.getRateType() != LoanEntity.RateType.FLOATING) {
            throw new RuntimeException("Cannot apply benchmark to fixed rate loan");
        }
        
        // Get latest snapshot to determine remaining balance and months
        RepaymentSnapshotEntity latestSnapshot = repaymentSnapshotService.getLatestSnapshot(loanId).orElseThrow(() -> new RuntimeException("No snapshot found for loan: " + loanId));
        
        // Calculate new rate including spread
        BigDecimal finalRate = newRate.add(loanEntity.getSpread() != null ? loanEntity.getSpread() : BigDecimal.ZERO);
        
        // Create new loan input for recalculation
        LoanInput recalcInput = createRecalculationInput(loanEntity, latestSnapshot, finalRate);
        
        // Recalculate schedule
        LoanOutput recalcOutput = HomeLoan.calculateLoan(recalcInput);
        
        // Calculate new APR
        BigDecimal newApr = aprCalculationService.calculateAPR(recalcInput, recalcOutput, null);
        
        // Create new snapshot
        repaymentSnapshotService.createRateResetSnapshot(loanEntity, recalcOutput, newApr, 
            loanEntity.getAnnualRate(), finalRate);
        
        // Update loan's current rate
        loanEntity.setAnnualRate(finalRate);
        loanRepository.save(loanEntity);
        
        log.info("Benchmark applied successfully to loan: {}", loanId);
    }
    
    public List<LoanEntity> getActiveFloatingLoans() {
        return loanRepository.findActiveFloatingLoans();
    }
    
    public List<LoanEntity> getActiveFloatingLoansByBenchmark(String benchmarkName) {
        return loanRepository.findActiveFloatingLoansByBenchmark(benchmarkName);
    }
    
    private LoanEntity mapToLoanEntity(LoanInputDTO dto) {
        LoanEntity entity = new LoanEntity();
        entity.setCustomerId(dto.getCustomerId());
        entity.setProductType(dto.getProductType());
        entity.setLoanIssueDate(dto.getLoanIssueDate());
        entity.setStartDate(dto.getStartDate());
        entity.setPrincipal(dto.getPrincipal());
        entity.setAnnualRate(dto.getAnnualRate());
        entity.setRateType(LoanEntity.RateType.valueOf(dto.getRateType()));
        
        if (dto.getFloatingStrategy() != null) {
            entity.setFloatingStrategy(LoanEntity.FloatingStrategy.valueOf(dto.getFloatingStrategy()));
        }
        
        entity.setMonths(dto.getMonths());
        entity.setMoratoriumMonths(dto.getMoratoriumMonths());
        
        if (dto.getMoratoriumType() != null) {
            entity.setMoratoriumType(LoanEntity.MoratoriumType.valueOf(dto.getMoratoriumType()));
        }
        
        entity.setPartialPaymentEmi(dto.getPartialPaymentEmi());
        entity.setCompoundingFrequency(LoanEntity.CompoundingFrequency.valueOf(dto.getCompoundingFrequency()));
        entity.setResetPeriodicityMonths(dto.getResetPeriodicityMonths());
        entity.setBenchmarkName(dto.getBenchmarkName());
        entity.setSpread(dto.getSpread());
        
        return entity;
    }
    
    private void saveDisbursementPhases(LoanEntity loanEntity, List<LoanInputDTO.DisbursementPhaseDTO> phases) {
        if (phases == null || phases.isEmpty()) {
            return;
        }
        
        List<DisbursementPhaseEntity> entities = phases.stream()
            .map(dto -> {
                DisbursementPhaseEntity entity = new DisbursementPhaseEntity();
                entity.setLoan(loanEntity);
                entity.setDisbursementDate(dto.getDisbursementDate());
                entity.setAmount(dto.getAmount());
                entity.setDescription(dto.getDescription());
                entity.setSequence(dto.getSequence());
                return entity;
            })
            .collect(Collectors.toList());
        
        disbursementRepository.saveAll(entities);
    }
    
    private void saveLoanCharges(LoanEntity loanEntity, List<LoanInputDTO.LoanChargeDTO> charges) {
        if (charges == null || charges.isEmpty()) {
            return;
        }
        
        List<LoanChargeEntity> entities = charges.stream()
            .map(dto -> {
                LoanChargeEntity entity = new LoanChargeEntity();
                entity.setLoan(loanEntity);
                entity.setChargeType(dto.getChargeType());
                entity.setPayableTo(dto.getPayableTo());
                entity.setIsRecurring(dto.getIsRecurring());
                entity.setAmount(dto.getAmount());
                return entity;
            })
            .collect(Collectors.toList());
        
        loanChargesRepository.saveAll(entities);
    }
    
    private LoanInput mapToCalculatorInput(LoanInputDTO dto) {
        LoanInput input = new LoanInput();
        input.setPrincipal(dto.getPrincipal().doubleValue());
        input.setAnnualRate(dto.getAnnualRate().doubleValue());
        input.setMonths(dto.getMonths());
        input.setMoratoriumMonths(dto.getMoratoriumMonths());
        input.setStartDate(dto.getStartDate());
        input.setLoanIssueDate(dto.getLoanIssueDate());
        
        if (dto.getMoratoriumType() != null) {
            input.setMoratoriumType(LoanInput.MoratoriumType.valueOf(dto.getMoratoriumType()));
        }
        
        if (dto.getPartialPaymentEmi() != null) {
            input.setPartialPaymentEMIDuringMoratorium(dto.getPartialPaymentEmi().doubleValue());
        }
        
        if (dto.getFloatingStrategy() != null) {
            input.setStrategy(LoanInput.FloatingStrategy.valueOf(dto.getFloatingStrategy()));
        }
        
        input.setCompoundingFrequency(LoanInput.CompoundingFrequency.valueOf(dto.getCompoundingFrequency()));
        
        // Map disbursement phases
        if (dto.getDisbursementPhases() != null) {
            dto.getDisbursementPhases().forEach(phase -> 
                input.addDisbursementPhase(phase.getDisbursementDate(), 
                    phase.getAmount().doubleValue(), phase.getDescription()));
        }
        
        return input;
    }
    
    private LoanOutput calculateLoanSchedule(LoanInput input, LoanInputDTO dto) {
        if (dto.getDisbursementPhases() != null && !dto.getDisbursementPhases().isEmpty()) {
            return HomeLoan.calculateHomeLoan(input);
        } else {
            return HomeLoan.calculateLoan(input);
        }
    }
    
    private LoanInput createRecalculationInput(LoanEntity loanEntity, RepaymentSnapshotEntity snapshot, BigDecimal newRate) {
        LoanInput input = new LoanInput();
        input.setPrincipal(snapshot.getPrincipalBalance().doubleValue());
        input.setAnnualRate(newRate.doubleValue());
        input.setMonths(snapshot.getMonthsRemaining());
        input.setStartDate(LocalDate.now());
        input.setCompoundingFrequency(LoanInput.CompoundingFrequency.valueOf(loanEntity.getCompoundingFrequency().name()));
        
        if (loanEntity.getFloatingStrategy() != null) {
            input.setStrategy(LoanInput.FloatingStrategy.valueOf(loanEntity.getFloatingStrategy().name()));
        }
        
        return input;
    }
    
    private LoanOutputDTO mapToLoanOutputDTO(LoanEntity entity) {
        LoanOutputDTO dto = new LoanOutputDTO();
        dto.setLoanId(entity.getId());
        dto.setProductType(entity.getProductType());
        dto.setPrincipal(entity.getPrincipal());
        dto.setAnnualRate(entity.getAnnualRate());
        dto.setRateType(entity.getRateType().name());
        dto.setMonths(entity.getMonths());
        dto.setStatus(entity.getStatus().name());
        dto.setLoanIssueDate(entity.getLoanIssueDate());
        dto.setStartDate(entity.getStartDate());
        dto.setBenchmarkName(entity.getBenchmarkName());
        dto.setSpread(entity.getSpread());
        dto.setCreatedAt(entity.getCreatedAt());
        dto.setUpdatedAt(entity.getUpdatedAt());
        
        // Map disbursement phases
        if (entity.getDisbursementPhases() != null) {
            dto.setDisbursementPhases(entity.getDisbursementPhases().stream()
                .map(phase -> new LoanOutputDTO.DisbursementPhaseDTO(
                    phase.getDisbursementDate(),
                    phase.getAmount(),
                    null, // Calculate cumulative in service if needed
                    phase.getDescription(),
                    phase.getSequence()
                ))
                .collect(Collectors.toList()));
        }
        
        // Map charges
        if (entity.getCharges() != null) {
            dto.setCharges(entity.getCharges().stream()
                .map(charge -> new LoanOutputDTO.LoanChargeDTO(
                    charge.getChargeType(),
                    charge.getPayableTo(),
                    charge.getIsRecurring(),
                    charge.getAmount(),
                    charge.getCreatedAt()
                ))
                .collect(Collectors.toList()));
        }
        
        // Get latest snapshot
        Optional<RepaymentSnapshotEntity> latestSnapshotOpt = repaymentSnapshotService.getLatestSnapshot(entity.getId());
        RepaymentSnapshotEntity latestSnapshot = latestSnapshotOpt.orElse(null);
        if (latestSnapshot != null) {
            dto.setLatestSnapshot(new LoanOutputDTO.RepaymentSnapshotDTO(
                latestSnapshot.getId(),
                latestSnapshot.getSnapshotDate(),
                latestSnapshot.getPrincipalBalance(),
                latestSnapshot.getMonthsRemaining(),
                latestSnapshot.getAnnualRate(),
                latestSnapshot.getRateType(),
                latestSnapshot.getApr(),
                latestSnapshot.getMemo(),
                latestSnapshot.getCreatedAt()
            ));
        }
        
        // Set initial EMI from the latest snapshot
        try {
            RepaymentScheduleDTO scheduleDTO = repaymentSnapshotService.getRepaymentSchedule(entity.getId());
            dto.setRepaymentSchedule(scheduleDTO);
                
            // Set initial EMI from first installment
            if (scheduleDTO != null && scheduleDTO.getRepaymentRows() != null && !scheduleDTO.getRepaymentRows().isEmpty()) {
                RepaymentScheduleDTO.RepaymentRowDTO firstInstallment = scheduleDTO.getRepaymentRows().get(0);
                dto.setInitialEmi(firstInstallment.getEmi());
                log.info("Set initial EMI for loan {}: {}", entity.getId(), firstInstallment.getEmi());
                
                // Set APR from schedule
                if (scheduleDTO.getApr() != null) {
                    dto.setApr(scheduleDTO.getApr());
                }
                
                // Calculate and set totals
                BigDecimal totalInterest = scheduleDTO.getRepaymentRows().stream()
                    .filter(row -> row.getEmi() != null && row.getEmi().compareTo(BigDecimal.ZERO) > 0)
                    .map(row -> row.getInterestPaid() != null ? row.getInterestPaid() : BigDecimal.ZERO)
                    .reduce(BigDecimal.ZERO, BigDecimal::add);
                // Total amount payable = Principal + Total Interest
                BigDecimal totalAmount = entity.getPrincipal().add(totalInterest);
                
                dto.setTotalInterest(totalInterest);
                dto.setTotalAmountPayable(totalAmount);
            } else {
                log.warn("No installments or repayment rows found for loan {}", entity.getId());
            }
        } catch (Exception e) {
            log.warn("Could not fetch repayment schedule for loan {}: {}", entity.getId(), e.getMessage());
        }
        
        return dto;
    }
    
    @Transactional
    public LoanOutputDTO applyLoanChange(UUID loanId, LoanInputDTO newInput, String changedBy, LocalDate cutoffDate) {
        log.info("Applying loan changes for loan: {} with cutoff date: {}", loanId, cutoffDate);
        
        LoanEntity loanEntity = loanRepository.findById(loanId)
            .orElseThrow(() -> new RuntimeException("Loan not found: " + loanId));
        
        log.info("Original loan - Principal: {}, Rate: {}, Months: {}, Moratorium: {}", 
            loanEntity.getLoanAmount(), loanEntity.getAnnualRate(), loanEntity.getTenureMonths(), loanEntity.getMoratoriumPeriod());
        log.info("New input - Principal: {}, Rate: {}, Months: {}, Moratorium: {}", 
            newInput.getLoanAmount(), newInput.getAnnualRate(), newInput.getTenureMonths(), newInput.getMoratoriumPeriod());
        
        // Compare with existing loan and detect changes
        Map<String, String[]> changes = detectLoanChanges(loanEntity, newInput);
        
        log.info("Detected {} changes: {}", changes.size(), changes.keySet());
        for (Map.Entry<String, String[]> change : changes.entrySet()) {
            log.info("Change detected - {}: {} -> {}", change.getKey(), change.getValue()[0], change.getValue()[1]);
        }
        
        if (changes.isEmpty()) {
            log.info("No changes detected for loan: {}", loanId);
            return mapToLoanOutputDTOFromSnapshot(loanEntity);
        }
        
        // Log changes to audit table
        logChangesToAudit(loanEntity, changes, changedBy);
        
        // Apply changes to loan entity
        applyChangesToLoanEntity(loanEntity, newInput);
        loanEntity = loanRepository.save(loanEntity);
        
        // Create new versioned snapshot with hybrid schedule
        createVersionedSnapshot(loanEntity, changes, changedBy, cutoffDate);
        
        // Generate new KFS version
        LoanOutputDTO kfsData = mapToLoanOutputDTOFromSnapshot(loanEntity);
        kfsVersionService.createKfsVersion(loanEntity.getId(), kfsData, 
            KfsVersionEntity.TriggerReason.LOAN_MODIFICATION, 
            "KFS regenerated due to loan modification", changedBy);
        
        log.info("Loan {} updated successfully with {} changes. New KFS version created.", loanId, changes.size());
        return kfsData;
    }
    
    private Map<String, String[]> detectLoanChanges(LoanEntity existing, LoanInputDTO newInput) {
        Map<String, String[]> changes = new HashMap<>();
        
        // Compare interest rate
        if (existing.getAnnualRate().compareTo(newInput.getAnnualRate()) != 0) {
            changes.put("annualRate", new String[]{existing.getAnnualRate().toString(), newInput.getAnnualRate().toString()});
        }
        
        // Compare loan amount
        if (existing.getLoanAmount().compareTo(newInput.getLoanAmount()) != 0) {
            changes.put("loanAmount", new String[]{existing.getLoanAmount().toString(), newInput.getLoanAmount().toString()});
        }
        
        // Compare tenure
        if (!existing.getTenureMonths().equals(newInput.getTenureMonths())) {
            changes.put("tenureMonths", new String[]{existing.getTenureMonths().toString(), newInput.getTenureMonths().toString()});
        }
        
        // Compare moratorium period
        if (!Objects.equals(existing.getMoratoriumPeriod(), newInput.getMoratoriumPeriod())) {
            changes.put("moratoriumPeriod", new String[]{
                existing.getMoratoriumPeriod() != null ? existing.getMoratoriumPeriod().toString() : "null",
                newInput.getMoratoriumPeriod() != null ? newInput.getMoratoriumPeriod().toString() : "null"
            });
        }
        
        
        return changes;
    }
    
    @Transactional
    public LoanOutputDTO applyMoratoriumToLoan(UUID loanId, Integer startMonth, Integer endMonth,
            String moratoriumType, Double partialPaymentEMI, String changedBy) {

        log.info("*** MORATORIUM METHOD CALLED *** Applying moratorium to loan: {} (months {}-{}) with type: {}",
            loanId, startMonth, endMonth, moratoriumType);

        // Fetch the loan entity
        LoanEntity loanEntity = loanRepository.findById(loanId)
            .orElseThrow(() -> new RuntimeException("Loan not found"));

        // Create LoanInput from the entity for calculation
        LoanInput calculatorInput = new LoanInput();
        calculatorInput.setPrincipal(loanEntity.getPrincipal().doubleValue());
        calculatorInput.setAnnualRate(loanEntity.getAnnualRate().doubleValue());
        calculatorInput.setMonths(loanEntity.getMonths());
        calculatorInput.setStartDate(loanEntity.getStartDate());
        calculatorInput.setLoanIssueDate(loanEntity.getLoanIssueDate());
        calculatorInput.setCompoundingFrequency(LoanInput.CompoundingFrequency.valueOf(loanEntity.getCompoundingFrequency().name()));

        // Add existing disbursement phases if this is a home loan
        if ("HOME_LOAN".equals(loanEntity.getProductType()) && loanEntity.getDisbursementPhases() != null) {
            loanEntity.getDisbursementPhases().forEach(phase -> 
                calculatorInput.addDisbursementPhase(phase.getDisbursementDate(), 
                    phase.getAmount().doubleValue(), phase.getDescription()));
            log.info("Added {} disbursement phases to moratorium calculation", loanEntity.getDisbursementPhases().size());
        }

        // Add the moratorium period using the correct method signature
        if (startMonth != null && endMonth != null) {
            LoanInput.MoratoriumType type = LoanInput.MoratoriumType.FULL;
            if ("PARTIAL".equalsIgnoreCase(moratoriumType)) {
                type = LoanInput.MoratoriumType.PARTIAL;
            }

            double partialEmi = partialPaymentEMI != null ? partialPaymentEMI : 0.0;
            calculatorInput.addMoratoriumPeriod(startMonth, endMonth, type, partialEmi);
            log.info("Added moratorium period: months {}-{}, type: {}", startMonth, endMonth, type);
        }

        // Calculate the loan with moratorium - use appropriate method based on loan type
        LoanOutput calculatorOutput;
        if ("HOME_LOAN".equals(loanEntity.getProductType()) && loanEntity.getDisbursementPhases() != null && !loanEntity.getDisbursementPhases().isEmpty()) {
            calculatorOutput = HomeLoan.calculateHomeLoan(calculatorInput);
            log.info("Used HomeLoan.calculateHomeLoan() for loan with disbursement phases");
        } else {
            calculatorOutput = HomeLoan.calculateLoan(calculatorInput);
            log.info("Used HomeLoan.calculateLoan() for regular loan");
        }
        log.info("Calculated loan with moratorium - total installments: {}", calculatorOutput.getPaymentSchedule().size());

        // Calculate new APR
        BigDecimal newApr = aprCalculationService.calculateAPR(calculatorInput, calculatorOutput, null);

        // Create change tracking map for moratorium
        Map<String, String[]> changes = new HashMap<>();
        String moratoriumDesc = String.format("Added %s moratorium: months %d to %d",
            moratoriumType, startMonth, endMonth);
        if (partialPaymentEMI != null) {
            moratoriumDesc += String.format(" (partial EMI: %.2f)", partialPaymentEMI);
        }
        changes.put("moratorium", new String[]{"None", moratoriumDesc});

        // Log the changes
        logChangesToAudit(loanEntity, changes, changedBy);

        // Update loan entity with moratorium details
        loanEntity.setMoratoriumMonths(startMonth);
        if (moratoriumType != null) {
            loanEntity.setMoratoriumType(LoanEntity.MoratoriumType.valueOf(moratoriumType));
        }
        if (partialPaymentEMI != null) {
            loanEntity.setPartialPaymentEmi(BigDecimal.valueOf(partialPaymentEMI));
        }
        loanEntity = loanRepository.save(loanEntity);

        // Get the next version number
        Integer maxVersion = repaymentSnapshotService.getMaxVersionForLoan(loanEntity.getId());
        int nextVersion = (maxVersion != null ? maxVersion : 0) + 1;

        // Create new snapshot directly with the moratorium-adjusted schedule
        repaymentSnapshotService.createSnapshot(loanEntity, calculatorOutput, newApr, changedBy, nextVersion);
        log.info("Created new repayment snapshot version {} with moratorium schedule", nextVersion);

        // Generate new KFS
        LoanOutputDTO kfsData = mapToLoanOutputDTOFromSnapshot(loanEntity);
        kfsVersionService.createKfsVersion(loanEntity.getId(), kfsData,
            KfsVersionEntity.TriggerReason.LOAN_MODIFICATION,
            "KFS regenerated due to moratorium addition", changedBy);

        log.info("Loan {} updated successfully with moratorium. New KFS version created.", loanId);
        return kfsData;
    }

    private void logChangesToAudit(LoanEntity loanEntity, Map<String, String[]> changes, String changedBy) {
        for (Map.Entry<String, String[]> change : changes.entrySet()) {
            LoanAuditEntity auditEntry = new LoanAuditEntity();
            auditEntry.setLoan(loanEntity);
            auditEntry.setFieldName(change.getKey());
            auditEntry.setOldValue(change.getValue()[0]);
            auditEntry.setNewValue(change.getValue()[1]);
            auditEntry.setChangedBy(changedBy);
            loanAuditRepository.save(auditEntry);
        }
    }
    
    private void applyChangesToLoanEntity(LoanEntity loanEntity, LoanInputDTO newInput) {
        loanEntity.setAnnualRate(newInput.getAnnualRate());
        loanEntity.setLoanAmount(newInput.getLoanAmount());
        loanEntity.setTenureMonths(newInput.getTenureMonths());
        loanEntity.setMoratoriumPeriod(newInput.getMoratoriumPeriod());
        loanEntity.setRateType(LoanEntity.RateType.valueOf(newInput.getRateType()));
        // Add other fields as needed
    }
    
    private void createVersionedSnapshot(LoanEntity loanEntity, Map<String, String[]> changes, String changedBy, LocalDate cutoffDate) {
        // Get the next version number
        Integer maxVersion = repaymentSnapshotService.getMaxVersionForLoan(loanEntity.getId());
        int nextVersion = (maxVersion != null ? maxVersion : 0) + 1;
        
        // Get the last snapshot to preserve paid installments
        Optional<RepaymentSnapshotEntity> lastSnapshot = repaymentSnapshotService.getLatestSnapshot(loanEntity.getId());
        
        if (lastSnapshot.isPresent()) {
            // Create hybrid schedule with change markers
            repaymentSnapshotService.createVersionedHybridSnapshot(loanEntity, lastSnapshot.get(), 
                changes, changedBy, nextVersion, cutoffDate);
        } else {
            // First time - create initial snapshot
            LoanInput calculatorInput = createLoanInputFromEntity(loanEntity);
            LoanOutput calculatorOutput = HomeLoan.calculateLoan(calculatorInput);
            LoanInput loanInput = createLoanInputFromEntity(loanEntity);
            LoanOutput loanOutput = HomeLoan.calculateLoan(loanInput);
            BigDecimal newApr = aprCalculationService.calculateAPR(loanInput, loanOutput, null);
            
            repaymentSnapshotService.createSnapshot(loanEntity, calculatorOutput, newApr, changedBy, nextVersion);
        }
    }
    
    private LoanOutputDTO mapToLoanOutputDTOFromSnapshot(LoanEntity entity) {
        LoanOutputDTO dto = new LoanOutputDTO();
        
        // Map basic loan details
        dto.setLoanId(entity.getId());
        // Map entity fields to DTO - using available setter methods
        dto.setPrincipal(entity.getPrincipal());
        dto.setAnnualRate(entity.getAnnualRate());
        dto.setMonths(entity.getMonths()); // Set loan term
        dto.setInitialEmi(BigDecimal.ZERO); // Will be set from schedule
        dto.setProductType(entity.getProductType());
        dto.setRateType(entity.getRateType().name());
        dto.setLoanIssueDate(entity.getLoanIssueDate());
        dto.setStartDate(entity.getStartDate());
        dto.setStatus(entity.getStatus().name());
        
        // Fetch repayment schedule from latest snapshot - NO recalculation
        try {
            RepaymentScheduleDTO schedule = repaymentSnapshotService.getLatestRepaymentSchedule(entity.getId());
            dto.setRepaymentSchedule(schedule);
            
            if (schedule != null && schedule.getRepaymentRows() != null && !schedule.getRepaymentRows().isEmpty()) {
                var firstRow = schedule.getRepaymentRows().get(0);
                dto.setInitialEmi(firstRow.getEmi());
                
                // Set APR from latest snapshot
                if (schedule.getApr() != null) {
                    dto.setApr(schedule.getApr());
                }
                
                // Calculate totals from non-zero EMI rows only
                BigDecimal totalInterest = schedule.getRepaymentRows().stream()
                    .filter(row -> row.getEmi() != null && row.getEmi().compareTo(BigDecimal.ZERO) > 0)
                    .map(row -> row.getInterestPaid() != null ? row.getInterestPaid() : BigDecimal.ZERO)
                    .reduce(BigDecimal.ZERO, BigDecimal::add);
                // Total amount payable = Principal + Total Interest
                BigDecimal totalAmount = entity.getPrincipal().add(totalInterest);
                
                // Set the calculated totals in DTO
                dto.setTotalInterest(totalInterest);
                dto.setTotalAmountPayable(totalAmount);
            }
        } catch (Exception e) {
            log.warn("Could not fetch repayment schedule for loan {}: {}", entity.getId(), e.getMessage());
        }
        
        return dto;
    }
    
    @Transactional
    public void createHybridRepaymentSchedule(LoanEntity loanEntity, String updatedBy, LocalDate effectiveFrom) {
        log.info("Creating hybrid repayment schedule for loan: {} effective from: {}", loanEntity.getId(), effectiveFrom);
        
        // Get current repayment schedule
        Optional<RepaymentSnapshotEntity> latestSnapshotOpt = repaymentSnapshotService.getLatestSnapshot(loanEntity.getId());
        if (latestSnapshotOpt.isEmpty()) {
            log.warn("No existing snapshot found for loan: {}", loanEntity.getId());
            return;
        }
        RepaymentSnapshotEntity latestSnapshot = latestSnapshotOpt.get();
        
        // ... (rest of the code remains the same)
        RepaymentScheduleDTO currentSchedule = repaymentSnapshotService.getLatestRepaymentSchedule(loanEntity.getId());
        if (currentSchedule == null || currentSchedule.getRepaymentRows() == null) {
            log.warn("No repayment schedule found for loan: {}", loanEntity.getId());
            return;
        }
        
        List<RepaymentScheduleDTO.RepaymentRowDTO> existingRows = currentSchedule.getRepaymentRows();
        
        // Find payments that have already been made (before effective date)
        List<RepaymentScheduleDTO.RepaymentRowDTO> paidInstallments = existingRows.stream()
            .filter(row -> row.getPaymentDate().isBefore(effectiveFrom))
            .collect(Collectors.toList());
        
        // Calculate remaining principal after last paid installment
        double remainingPrincipal = loanEntity.getPrincipal().doubleValue();
        LocalDate nextDueDate = loanEntity.getStartDate();
        
        if (!paidInstallments.isEmpty()) {
            RepaymentScheduleDTO.RepaymentRowDTO lastPaid = paidInstallments.get(paidInstallments.size() - 1);
            remainingPrincipal = lastPaid.getRemainingBalance().doubleValue();
            nextDueDate = lastPaid.getPaymentDate().plusMonths(1);
        }
        
        // Calculate remaining months
        int totalMonths = loanEntity.getMonths();
        int paidMonths = paidInstallments.size();
        int remainingMonths = totalMonths - paidMonths;
        
        if (remainingMonths <= 0) {
            log.warn("No remaining months for loan: {}", loanEntity.getId());
            return;
        }
        
        // Create loan input for recalculation with new rate
        LoanInput recalcInput = new LoanInput();
        recalcInput.setPrincipal(remainingPrincipal);
        recalcInput.setAnnualRate(loanEntity.getAnnualRate().doubleValue());
        recalcInput.setMonths(remainingMonths);
        recalcInput.setStartDate(nextDueDate);
        recalcInput.setLoanIssueDate(nextDueDate);
        recalcInput.setCompoundingFrequency(LoanInput.CompoundingFrequency.valueOf(loanEntity.getCompoundingFrequency().name()));
        if (loanEntity.getFloatingStrategy() != null) {
            recalcInput.setStrategy(LoanInput.FloatingStrategy.valueOf(loanEntity.getFloatingStrategy().name()));
        }
        
        // Recalculate schedule for remaining months
        LoanOutput recalcOutput = HomeLoan.calculateLoan(recalcInput);
        
        // Calculate new APR for the entire loan (considering both old and new schedules)
        BigDecimal newApr = aprCalculationService.calculateAPR(recalcInput, recalcOutput, null);
        
        // Create hybrid repayment snapshot
        repaymentSnapshotService.createHybridSnapshot(loanEntity, paidInstallments, recalcOutput, newApr, updatedBy, effectiveFrom);
    }
    
    private LoanInput createLoanInputFromEntity(LoanEntity entity) {
        LoanInput input = new LoanInput();
        input.setPrincipal(entity.getPrincipal().doubleValue());
        input.setAnnualRate(entity.getAnnualRate().doubleValue());
        input.setMonths(entity.getMonths());
        input.setMoratoriumMonths(entity.getMoratoriumMonths());
        input.setStartDate(entity.getStartDate());
        input.setLoanIssueDate(entity.getLoanIssueDate());
        
        if (entity.getMoratoriumType() != null) {
            input.setMoratoriumType(LoanInput.MoratoriumType.valueOf(entity.getMoratoriumType().name()));
        }
        
        if (entity.getPartialPaymentEmi() != null) {
            input.setPartialPaymentEMIDuringMoratorium(entity.getPartialPaymentEmi().doubleValue());
        }
        
        if (entity.getFloatingStrategy() != null) {
            input.setStrategy(LoanInput.FloatingStrategy.valueOf(entity.getFloatingStrategy().name()));
        }
        
        input.setCompoundingFrequency(LoanInput.CompoundingFrequency.valueOf(entity.getCompoundingFrequency().name()));
        
        return input;
    }
    
    public List<LoanVersionDTO> getLoanVersionHistory(UUID loanId) {
        return loanVersionService.getLoanVersionHistory(loanId).stream()
            .map(this::mapToLoanVersionDTO)
            .collect(Collectors.toList());
    }
    
    public List<KfsVersionDTO> getKfsVersionHistory(UUID loanId) {
        return kfsVersionService.getKfsVersionHistory(loanId).stream()
            .map(this::mapToKfsVersionDTO)
            .collect(Collectors.toList());
    }
    
    public LoanOutputDTO getKfsVersion(UUID loanId, Integer versionNumber) {
        log.info("Fetching KFS version {} for loan: {}", versionNumber, loanId);
        
        return kfsVersionService.getKfsVersion(loanId, versionNumber)
            .map(kfsVersionService::deserializeKfsData)
            .orElseThrow(() -> new RuntimeException("KFS version not found: " + versionNumber + " for loan: " + loanId));
    }
    
    private LoanVersionDTO mapToLoanVersionDTO(LoanVersionEntity entity) {
        LoanVersionDTO dto = new LoanVersionDTO();
        dto.setId(entity.getId());
        dto.setLoanId(entity.getLoan().getId());
        dto.setVersionNumber(entity.getVersionNumber());
        dto.setPrincipal(entity.getPrincipal());
        dto.setAnnualRate(entity.getAnnualRate());
        dto.setMonths(entity.getMonths());
        dto.setMoratoriumMonths(entity.getMoratoriumMonths());
        dto.setMoratoriumType(entity.getMoratoriumType() != null ? entity.getMoratoriumType().name() : null);
        dto.setPartialPaymentEmi(entity.getPartialPaymentEmi());
        dto.setRateType(entity.getRateType().name());
        dto.setFloatingStrategy(entity.getFloatingStrategy() != null ? entity.getFloatingStrategy().name() : null);
        dto.setCompoundingFrequency(entity.getCompoundingFrequency().name());
        dto.setResetPeriodicityMonths(entity.getResetPeriodicityMonths());
        dto.setBenchmarkName(entity.getBenchmarkName());
        dto.setSpread(entity.getSpread());
        dto.setLoanIssueDate(entity.getLoanIssueDate());
        dto.setStartDate(entity.getStartDate());
        dto.setProductType(entity.getProductType());
        dto.setCustomerId(entity.getCustomerId());
        dto.setChangeReason(entity.getChangeReason().name());
        dto.setChangeDescription(entity.getChangeDescription());
        dto.setChangedFields(entity.getChangedFields());
        dto.setPreviousValues(entity.getPreviousValues());
        dto.setCreatedAt(entity.getCreatedAt());
        dto.setCreatedBy(entity.getCreatedBy());
        dto.setEffectiveFrom(entity.getEffectiveFrom());
        return dto;
    }
    
    private KfsVersionDTO mapToKfsVersionDTO(KfsVersionEntity entity) {
        KfsVersionDTO dto = new KfsVersionDTO();
        dto.setId(entity.getId());
        dto.setLoanId(entity.getLoan().getId());
        dto.setVersionNumber(entity.getVersionNumber());
        dto.setKfsData(entity.getKfsData());
        dto.setGeneratedForRate(entity.getGeneratedForRate());
        dto.setGeneratedForSpread(entity.getGeneratedForSpread());
        dto.setGeneratedForApr(entity.getGeneratedForApr());
        dto.setBenchmarkName(entity.getBenchmarkName());
        dto.setTriggerReason(entity.getTriggerReason().name());
        dto.setMemo(entity.getMemo());
        dto.setCreatedAt(entity.getCreatedAt());
        dto.setCreatedBy(entity.getCreatedBy());
        return dto;
    }
}
