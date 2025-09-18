package com.mybank.lms.service;

import com.mybank.lms.calculator.HomeLoan;
import com.mybank.lms.calculator.LoanInput;
import com.mybank.lms.calculator.LoanOutput;
import com.mybank.lms.model.dto.RepaymentScheduleDTO;
import com.mybank.lms.model.entity.*;
import com.mybank.lms.repository.*;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.ArrayList;
import java.util.List;
import java.util.UUID;
import java.util.stream.Collectors;
import java.util.Optional;
import java.util.Map;
import java.util.HashMap;

@Service
@RequiredArgsConstructor
@Slf4j
public class RepaymentSnapshotService {
    
    private final RepaymentSnapshotRepository snapshotRepository;
    private final RepaymentSnapshotRepository repaymentSnapshotRepository;
    private final RepaymentRowRepository rowRepository;
    private final RateResetAuditRepository rateResetAuditRepository;
    
    @Transactional
    public RepaymentSnapshotEntity createInitialSnapshot(LoanEntity loanEntity, LoanOutput calculatorOutput, BigDecimal apr) {
        log.info("Creating initial repayment snapshot for loan: {}", loanEntity.getId());
        
        RepaymentSnapshotEntity snapshot = new RepaymentSnapshotEntity();
        snapshot.setLoan(loanEntity);
        snapshot.setSnapshotDate(LocalDate.now());
        snapshot.setPrincipalBalance(loanEntity.getPrincipal());
        snapshot.setMonthsRemaining(loanEntity.getMonths());
        snapshot.setAnnualRate(loanEntity.getAnnualRate());
        snapshot.setRateType(loanEntity.getRateType().name());
        snapshot.setApr(apr);
        snapshot.setVersion(1);
        snapshot.setMemo("Initial repayment schedule");
        
        snapshot = repaymentSnapshotRepository.save(snapshot);
        
        // Create repayment rows
        createRepaymentRows(snapshot, calculatorOutput);
        
        log.info("Initial snapshot created with ID: {}", snapshot.getId());
        return snapshot;
    }
    
    @Transactional
    public RepaymentSnapshotEntity createRateResetSnapshot(LoanEntity loanEntity, LoanOutput calculatorOutput, 
                                                          BigDecimal apr, BigDecimal previousRate, BigDecimal newRate) {
        log.info("Creating rate reset snapshot for loan: {}", loanEntity.getId());
        
        RepaymentSnapshotEntity snapshot = new RepaymentSnapshotEntity();
        snapshot.setLoan(loanEntity);
        snapshot.setSnapshotDate(LocalDate.now());
        snapshot.setPrincipalBalance(BigDecimal.valueOf(calculatorOutput.getPaymentSchedule().get(0).getRemainingBalance()));
        snapshot.setMonthsRemaining(calculatorOutput.getActualTenure());
        snapshot.setAnnualRate(newRate);
        snapshot.setRateType(loanEntity.getRateType().name());
        snapshot.setApr(apr);
        snapshot.setMemo(String.format("Rate reset from %.4f%% to %.4f%%", previousRate, newRate));
        
        snapshot = repaymentSnapshotRepository.save(snapshot);
        
        // Create repayment rows
        createRepaymentRows(snapshot, calculatorOutput);
        
        // Create rate reset audit entry
        createRateResetAudit(loanEntity, previousRate, newRate, snapshot);
        
        log.info("Rate reset snapshot created with ID: {}", snapshot.getId());
        return snapshot;
    }
    
    @Transactional
    public RepaymentSnapshotEntity createHybridSnapshot(LoanEntity loanEntity, 
                                                       List<RepaymentScheduleDTO.RepaymentRowDTO> paidInstallments,
                                                       LoanOutput newCalculatorOutput, 
                                                       BigDecimal apr, 
                                                       String updatedBy, 
                                                       LocalDate effectiveFrom) {
        log.info("Creating hybrid snapshot for loan: {} with {} paid installments", loanEntity.getId(), paidInstallments.size());
        
        RepaymentSnapshotEntity snapshot = new RepaymentSnapshotEntity();
        snapshot.setLoan(loanEntity);
        snapshot.setSnapshotDate(LocalDate.now());
        
        // Calculate remaining principal from last paid installment or original principal
        BigDecimal remainingPrincipal = loanEntity.getPrincipal();
        LocalDate nextDueDate = null;
        if (!paidInstallments.isEmpty()) {
            RepaymentScheduleDTO.RepaymentRowDTO lastPaid = paidInstallments.get(paidInstallments.size() - 1);
            remainingPrincipal = lastPaid.getRemainingBalance();
            nextDueDate = lastPaid.getPaymentDate().plusMonths(1);
        }
        
        snapshot.setPrincipalBalance(remainingPrincipal);
        snapshot.setMonthsRemaining(newCalculatorOutput.getActualTenure());
        snapshot.setAnnualRate(loanEntity.getAnnualRate());
        snapshot.setRateType(loanEntity.getRateType().name());
        snapshot.setApr(apr);
        snapshot.setMemo(String.format("Loan modified on %s - hybrid schedule with %d paid installments", 
                                     effectiveFrom, paidInstallments.size()));
        
        snapshot = repaymentSnapshotRepository.save(snapshot);
        
        // Create hybrid repayment rows (old + new)
        createHybridRepaymentRows(snapshot, paidInstallments, newCalculatorOutput, effectiveFrom);
        
        log.info("Hybrid snapshot created with ID: {}", snapshot.getId());
        return snapshot;
    }
    
    public RepaymentScheduleDTO getRepaymentSchedule(UUID loanId) {
        return getLatestRepaymentSchedule(loanId);
    }
    
    public RepaymentScheduleDTO getLatestRepaymentSchedule(UUID loanId) {
        log.info("Fetching latest repayment schedule for loan: {}", loanId);
        
        Optional<RepaymentSnapshotEntity> snapshotOpt = repaymentSnapshotRepository.findLatestByLoanId(loanId);
        
        if (snapshotOpt.isEmpty()) {
            log.warn("No repayment snapshot found for loan: {}", loanId);
            return null;
        }
        
        List<RepaymentRowEntity> rows = rowRepository.findBySnapshotIdOrderByMonthNumber(snapshotOpt.get().getId());
        return mapToRepaymentScheduleDTO(snapshotOpt.get(), rows);
    }
    
    public Integer getMaxVersionForLoan(UUID loanId) {
        return repaymentSnapshotRepository.findMaxVersionByLoanId(loanId);
    }
    
    public Optional<RepaymentSnapshotEntity> getLatestSnapshot(UUID loanId) {
        return repaymentSnapshotRepository.findLatestByLoanId(loanId);
    }
    
    public List<RepaymentSnapshotEntity> getAllSnapshots(UUID loanId) {
        return repaymentSnapshotRepository.findByLoanIdOrderBySnapshotDateDesc(loanId);
    }
    
    public RepaymentSnapshotEntity getLatestSnapshotEntity(UUID loanId) {
        return repaymentSnapshotRepository.findLatestByLoanId(loanId).orElse(null);
    }

    public List<RepaymentRowEntity> getRowsForSnapshot(UUID snapshotId) {
        return rowRepository.findBySnapshotIdOrderByMonthNumber(snapshotId);
    }

    /**
     * Build a combined schedule suitable for KFS versioning on modification:
     * - All rows from the previous snapshot up to the effective change point (paid/historical portion)
     * - One separator row indicating the modification (memo)
     * - All rows from the latest snapshot from the next installment onward
     */
    public RepaymentScheduleDTO getCombinedRepaymentSchedule(UUID loanId) {
        // Get snapshots ordered desc (latest first)
        List<RepaymentSnapshotEntity> snapshots = getAllSnapshots(loanId);
        if (snapshots == null || snapshots.isEmpty()) {
            throw new RuntimeException("No snapshots found for loan: " + loanId);
        }

        if (snapshots.size() == 1) {
            // Only one snapshot exists (initial) – return its schedule
            return getLatestRepaymentSchedule(loanId);
        }

        RepaymentSnapshotEntity latest = snapshots.get(0);
        RepaymentSnapshotEntity previous = snapshots.get(1);

        // Build previous schedule rows (historical part)
        List<RepaymentRowEntity> prevRows = rowRepository.findBySnapshotIdOrderByMonthNumber(previous.getId());
        RepaymentScheduleDTO prevDto = mapToRepaymentScheduleDTO(previous, prevRows);

        // Build latest schedule rows (new terms)
        RepaymentScheduleDTO schedule = getLatestRepaymentSchedule(latest.getLoan().getId());

        // Determine the first due date of the new terms to use as cutover
        java.time.LocalDate firstNewDueDate = schedule.getRepaymentRows() != null && !schedule.getRepaymentRows().isEmpty()
            ? schedule.getRepaymentRows().get(0).getDueDate()
            : latest.getSnapshotDate();

        RepaymentScheduleDTO combined = new RepaymentScheduleDTO();
        combined.setSnapshotId(latest.getId());
        combined.setSnapshotDate(latest.getSnapshotDate());
        combined.setPrincipalBalance(latest.getPrincipalBalance());
        combined.setMonthsRemaining(latest.getMonthsRemaining());
        combined.setAnnualRate(latest.getAnnualRate());
        combined.setRateType(latest.getRateType());
        combined.setApr(latest.getApr());
        combined.setMemo("Schedule combines history until modification, then new terms");
        combined.setCreatedAt(latest.getCreatedAt());

        // Take only previous rows strictly before the first new due date
        List<RepaymentScheduleDTO.RepaymentRowDTO> rows = prevDto.getRepaymentRows().stream()
            .filter(r -> r.getDueDate() != null && r.getDueDate().isBefore(firstNewDueDate))
            .collect(java.util.stream.Collectors.toList());

        // Insert a separator row only if there are historical rows to show
        if (!rows.isEmpty()) {
            RepaymentScheduleDTO.RepaymentRowDTO separator = new RepaymentScheduleDTO.RepaymentRowDTO();
            separator.setMonthNumber(rows.size() + 1);
            separator.setInstallmentNumber(rows.size() + 1);
            separator.setDueDate(firstNewDueDate);
            separator.setPaymentDate(firstNewDueDate);
            separator.setPaymentType("MODIFICATION");
            separator.setPrincipalPaid(java.math.BigDecimal.ZERO);
            separator.setInterestPaid(java.math.BigDecimal.ZERO);
            separator.setEmi(java.math.BigDecimal.ZERO);
            separator.setRemainingBalance(rows.get(rows.size()-1).getPrincipalOutstanding());
            rows.add(separator);
        }

        // Append the latest schedule rows (new terms)
        if (schedule != null && schedule.getRepaymentRows() != null) {
            rows.addAll(schedule.getRepaymentRows());
        }

        combined.setRepaymentRows(rows);
        combined.setInstallments(rows);
        return combined;
    }
    
    private void createRepaymentRows(RepaymentSnapshotEntity snapshot, LoanOutput calculatorOutput) {
        List<RepaymentRowEntity> rows = new ArrayList<>();
        
        for (int i = 0; i < calculatorOutput.getPaymentSchedule().size(); i++) {
            LoanOutput.MonthlyPayment payment = calculatorOutput.getPaymentSchedule().get(i);
            
            RepaymentRowEntity row = new RepaymentRowEntity();
            row.setSnapshot(snapshot);
            row.setMonthNumber(payment.getMonthNumber());
            row.setPaymentDate(payment.getPaymentDate());
            row.setEmi(BigDecimal.valueOf(payment.getEmi()));
            row.setPrincipalPaid(BigDecimal.valueOf(payment.getPrincipalPaid()));
            row.setInterestPaid(BigDecimal.valueOf(payment.getInterestPaid()));
            row.setRemainingBalance(BigDecimal.valueOf(payment.getRemainingBalance()));
            row.setPaymentType(payment.getPaymentType());
            
            rows.add(row);
        }
        
        rowRepository.saveAll(rows);
        log.info("Created {} repayment rows for snapshot: {}", rows.size(), snapshot.getId());
    }
    
    private void createHybridRepaymentRows(RepaymentSnapshotEntity snapshot, 
                                         List<RepaymentScheduleDTO.RepaymentRowDTO> paidInstallments,
                                         LoanOutput newCalculatorOutput, 
                                         LocalDate effectiveFrom) {
        List<RepaymentRowEntity> rows = new ArrayList<>();
        int monthNumber = 1;
        
        // Add existing paid installments
        for (RepaymentScheduleDTO.RepaymentRowDTO paidRow : paidInstallments) {
            RepaymentRowEntity row = new RepaymentRowEntity();
            row.setSnapshot(snapshot);
            row.setMonthNumber(monthNumber++);
            row.setPaymentDate(paidRow.getPaymentDate());
            row.setEmi(paidRow.getEmi());
            row.setPrincipalPaid(paidRow.getPrincipalPaid());
            row.setInterestPaid(paidRow.getInterestPaid());
            row.setRemainingBalance(paidRow.getRemainingBalance());
            row.setPaymentType("PAID");
            
            rows.add(row);
        }
        
        // Add modification marker row if there are paid installments
        if (!paidInstallments.isEmpty()) {
            RepaymentRowEntity modificationRow = new RepaymentRowEntity();
            modificationRow.setSnapshot(snapshot);
            modificationRow.setMonthNumber(monthNumber++);
            modificationRow.setPaymentDate(effectiveFrom);
            modificationRow.setEmi(BigDecimal.ZERO);
            modificationRow.setPrincipalPaid(BigDecimal.ZERO);
            modificationRow.setInterestPaid(BigDecimal.ZERO);
            
            // Get remaining balance from last paid installment
            RepaymentScheduleDTO.RepaymentRowDTO lastPaid = paidInstallments.get(paidInstallments.size() - 1);
            modificationRow.setRemainingBalance(lastPaid.getRemainingBalance());
            modificationRow.setPaymentType("MODIFICATION");
            
            rows.add(modificationRow);
        }
        
        // Add new calculated installments
        for (LoanOutput.MonthlyPayment payment : newCalculatorOutput.getPaymentSchedule()) {
            RepaymentRowEntity row = new RepaymentRowEntity();
            row.setSnapshot(snapshot);
            row.setMonthNumber(monthNumber++);
            row.setPaymentDate(payment.getPaymentDate());
            row.setEmi(BigDecimal.valueOf(payment.getEmi()));
            row.setPrincipalPaid(BigDecimal.valueOf(payment.getPrincipalPaid()));
            row.setInterestPaid(BigDecimal.valueOf(payment.getInterestPaid()));
            row.setRemainingBalance(BigDecimal.valueOf(payment.getRemainingBalance()));
            row.setPaymentType("FUTURE");
            
            rows.add(row);
        }
        
        rowRepository.saveAll(rows);
        log.info("Created {} hybrid repayment rows for snapshot: {}", rows.size(), snapshot.getId());
    }
    
    private void createRateResetAudit(LoanEntity loanEntity, BigDecimal previousRate, BigDecimal newRate, 
                                    RepaymentSnapshotEntity snapshot) {
        RateResetAuditEntity audit = new RateResetAuditEntity();
        audit.setLoan(loanEntity);
        audit.setPreviousRate(previousRate);
        audit.setNewRate(newRate);
        audit.setResetDate(LocalDate.now());
        audit.setSnapshot(snapshot);
        
        rateResetAuditRepository.save(audit);
    }
    
    private RepaymentScheduleDTO mapToRepaymentScheduleDTO(RepaymentSnapshotEntity snapshot, List<RepaymentRowEntity> rows) {
        RepaymentScheduleDTO dto = new RepaymentScheduleDTO();
        dto.setSnapshotId(snapshot.getId());
        dto.setSnapshotDate(snapshot.getSnapshotDate());
        dto.setPrincipalBalance(snapshot.getPrincipalBalance());
        dto.setAnnualRate(snapshot.getAnnualRate());
        dto.setRateType(snapshot.getRateType());
        dto.setApr(snapshot.getApr());
        dto.setMemo(snapshot.getMemo());
        dto.setCreatedAt(snapshot.getCreatedAt());
        dto.setRateType(snapshot.getRateType());
        List<RepaymentScheduleDTO.RepaymentRowDTO> installmentList = rows.stream()
            .map(row -> {
                RepaymentScheduleDTO.RepaymentRowDTO rowDto = new RepaymentScheduleDTO.RepaymentRowDTO();
                rowDto.setMonthNumber(row.getMonthNumber());
                rowDto.setInstallmentNumber(row.getMonthNumber()); // Frontend compatibility
                rowDto.setPaymentDate(row.getPaymentDate());
                rowDto.setDueDate(row.getPaymentDate()); // Frontend compatibility
                rowDto.setEmi(row.getEmi());
                rowDto.setInstallmentAmount(row.getEmi()); // Frontend compatibility
                rowDto.setPrincipalPaid(row.getPrincipalPaid());
                rowDto.setPrincipal(row.getPrincipalPaid()); // Frontend compatibility
                rowDto.setPrincipalComponent(row.getPrincipalPaid()); // Frontend compatibility
                rowDto.setInterestPaid(row.getInterestPaid());
                rowDto.setInterest(row.getInterestPaid()); // Frontend compatibility
                rowDto.setInterestComponent(row.getInterestPaid()); // Frontend compatibility
                rowDto.setRemainingBalance(row.getRemainingBalance());
                rowDto.setPrincipalOutstanding(row.getRemainingBalance()); // Frontend compatibility
                rowDto.setPaymentType(row.getPaymentType());
                rowDto.setCreatedAt(row.getCreatedAt());
                return rowDto;
            })
            .collect(Collectors.toList());
        
        dto.setRepaymentRows(installmentList);
        dto.setInstallments(installmentList); // Set both for compatibility
        
        return dto;
    }
    
    @Transactional
    public RepaymentSnapshotEntity createSnapshot(LoanEntity loanEntity, LoanOutput calculatorOutput, 
                                                 BigDecimal apr, String createdBy) {
        return createSnapshot(loanEntity, calculatorOutput, apr, createdBy, 1);
    }
    
    @Transactional
    public RepaymentSnapshotEntity createSnapshot(LoanEntity loanEntity, LoanOutput calculatorOutput, 
                                                 BigDecimal apr, String createdBy, Integer version) {
        log.info("Creating modification snapshot for loan: {}", loanEntity.getId());
        
        RepaymentSnapshotEntity snapshot = new RepaymentSnapshotEntity();
        snapshot.setLoan(loanEntity);
        snapshot.setSnapshotDate(LocalDate.now());
        snapshot.setPrincipalBalance(loanEntity.getPrincipal());
        snapshot.setMonthsRemaining(loanEntity.getMonths());
        snapshot.setAnnualRate(loanEntity.getAnnualRate());
        snapshot.setRateType(loanEntity.getRateType().name());
        snapshot.setApr(apr);
        snapshot.setVersion(version);
        snapshot.setMemo(version == 1 ? "Initial repayment schedule" : "Updated repayment schedule v" + version);
        
        snapshot = repaymentSnapshotRepository.save(snapshot);
        
        // Save repayment rows
        createRepaymentRows(snapshot, calculatorOutput);
        
        log.info("Modification snapshot created with ID: {}", snapshot.getId());
        return snapshot;
    }
    
    @Transactional
    public RepaymentSnapshotEntity createVersionedHybridSnapshot(LoanEntity loanEntity, 
                                                               RepaymentSnapshotEntity lastSnapshot,
                                                               Map<String, String[]> changes, 
                                                               String changedBy, 
                                                               Integer version,
                                                               LocalDate cutoffDate) {
        log.info("Creating versioned hybrid snapshot for loan: {} version: {} with cutoff date: {}", loanEntity.getId(), version, cutoffDate);
        
        // Get all rows from last snapshot
        List<RepaymentRowEntity> lastRows = rowRepository.findBySnapshotIdOrderByMonthNumber(lastSnapshot.getId());
        
        // Find the cutoff point - installments before the cutoff date
        int paidInstallments = 0;
        BigDecimal remainingPrincipal = loanEntity.getPrincipal();
        
        for (RepaymentRowEntity row : lastRows) {
            if (row.getPaymentDate().isBefore(cutoffDate) || row.getPaymentDate().isEqual(cutoffDate)) {
                paidInstallments++;
                if (row.getRemainingBalance() != null) {
                    remainingPrincipal = row.getRemainingBalance();
                }
            } else {
                break;
            }
        }
        
        // If no remaining principal found from paid rows, use original loan principal
        if (remainingPrincipal == null) {
            remainingPrincipal = loanEntity.getPrincipal();
        }
        
        log.info("Paid installments: {}, Remaining principal: {}", paidInstallments, remainingPrincipal);
        
        RepaymentSnapshotEntity snapshot = new RepaymentSnapshotEntity();
        snapshot.setLoan(loanEntity);
        snapshot.setSnapshotDate(LocalDate.now());
        snapshot.setPrincipalBalance(remainingPrincipal);
        snapshot.setMonthsRemaining(loanEntity.getMonths() - paidInstallments);
        snapshot.setAnnualRate(loanEntity.getAnnualRate());
        snapshot.setRateType(loanEntity.getRateType().name());
        snapshot.setVersion(version);
        
        // Create memo describing the changes
        StringBuilder memoBuilder = new StringBuilder("Loan modified: ");
        changes.forEach((field, values) -> {
            memoBuilder.append(field).append(" changed from ").append(values[0])
                      .append(" to ").append(values[1]).append("; ");
        });
        snapshot.setMemo(memoBuilder.toString());
        
        // Calculate APR for the modified loan
        LoanInput modifiedInput = new LoanInput();
        modifiedInput.setPrincipal(remainingPrincipal.doubleValue());
        modifiedInput.setAnnualRate(loanEntity.getAnnualRate().doubleValue());
        modifiedInput.setMonths(loanEntity.getMonths() - paidInstallments);
        modifiedInput.setCompoundingFrequency(LoanInput.CompoundingFrequency.valueOf(loanEntity.getCompoundingFrequency().name()));
        
        // Include moratorium period in the calculation
        if (loanEntity.getMoratoriumPeriod() != null) {
            modifiedInput.setMoratoriumMonths(loanEntity.getMoratoriumPeriod());
        }
        
        // Start date should be the first day of the month after the cutoff date
        LocalDate nextMonthStart = cutoffDate.withDayOfMonth(1).plusMonths(1);
        modifiedInput.setStartDate(nextMonthStart);
        modifiedInput.setLoanIssueDate(loanEntity.getLoanIssueDate());
        
        // Add disbursement phases if this is a home loan with phases
        boolean hasActiveDisbursements = false;
        if ("HOME_LOAN".equals(loanEntity.getProductType()) && loanEntity.getDisbursementPhases() != null) {
            // Only add disbursement phases that are after the cutoff date
            for (var phase : loanEntity.getDisbursementPhases()) {
                if (phase.getDisbursementDate().isAfter(cutoffDate)) {
                    modifiedInput.addDisbursementPhase(phase.getDisbursementDate(), 
                        phase.getAmount().doubleValue(), phase.getDescription());
                    hasActiveDisbursements = true;
                }
            }
            log.info("Added future disbursement phases to hybrid calculation: {}", hasActiveDisbursements);
        }
        
        // Calculate using appropriate method based on loan type and disbursement phases
        LoanOutput modifiedOutput;
        if ("HOME_LOAN".equals(loanEntity.getProductType()) && hasActiveDisbursements) {
            modifiedOutput = HomeLoan.calculateHomeLoan(modifiedInput);
            log.info("Used HomeLoan.calculateHomeLoan() for hybrid snapshot with disbursement phases");
        } else {
            modifiedOutput = HomeLoan.calculateLoan(modifiedInput);
            log.info("Used HomeLoan.calculateLoan() for hybrid snapshot without disbursement phases");
        }
        BigDecimal calculatedApr = loanEntity.getAnnualRate();
        snapshot.setApr(calculatedApr);
        
        snapshot = repaymentSnapshotRepository.save(snapshot);
        
        // Create hybrid schedule: preserve paid + recalculate remaining
        createHybridRepaymentRows(snapshot, lastRows, paidInstallments, modifiedOutput);
        
        log.info("Hybrid snapshot created with ID: {}", snapshot.getId());
        return snapshot;
    }
    
    private void createHybridRepaymentRows(RepaymentSnapshotEntity snapshot, 
                                         List<RepaymentRowEntity> originalRows,
                                         int paidInstallments,
                                         LoanOutput newCalculation) {
        List<RepaymentRowEntity> hybridRows = new ArrayList<>();
        
        // 1. Copy only actual paid installments (skip zero EMI rows)
        LocalDate today = LocalDate.now();
        for (int i = 0; i < paidInstallments && i < originalRows.size(); i++) {
            RepaymentRowEntity originalRow = originalRows.get(i);
            
            // Skip rows with zero EMI or null values
            if (originalRow.getEmi() == null || originalRow.getEmi().compareTo(BigDecimal.ZERO) == 0) {
                continue;
            }
            
            if (originalRow.getPaymentDate().isBefore(today) || originalRow.getPaymentDate().isEqual(today)) {
                // Copy paid installment as-is
                RepaymentRowEntity paidRow = new RepaymentRowEntity();
                paidRow.setSnapshot(snapshot);
                paidRow.setMonthNumber(originalRow.getMonthNumber());
                paidRow.setPaymentDate(originalRow.getPaymentDate());
                paidRow.setEmi(originalRow.getEmi());
                paidRow.setPrincipalPaid(originalRow.getPrincipalPaid());
                paidRow.setInterestPaid(originalRow.getInterestPaid());
                paidRow.setRemainingBalance(originalRow.getRemainingBalance());
                paidRow.setPaymentType("PAID");
                paidRow.setChangeMarker(false);
                hybridRows.add(paidRow);
            } else {
                break; // Stop at first unpaid installment
            }
        }
        
        // Skip change marker row to avoid extra zero row
        
        // Add new calculated installments from LoanOutput with proper termination logic
        int actualPaidRows = (int) hybridRows.stream().filter(row -> row.getEmi().compareTo(BigDecimal.ZERO) > 0).count();
        int newRowsAdded = 0;
        
        for (int i = 0; i < newCalculation.getPaymentSchedule().size(); i++) {
            var entry = newCalculation.getPaymentSchedule().get(i);
            
            // Skip rows with zero or negative EMI amounts
            if (entry.getEmi() <= 0.01) {
                continue;
            }
            
            RepaymentRowEntity newRow = new RepaymentRowEntity();
            newRow.setSnapshot(snapshot);
            newRow.setMonthNumber(actualPaidRows + 1 + newRowsAdded);
            newRow.setPaymentDate(LocalDate.now().plusMonths(newRowsAdded + 1));
            newRow.setEmi(BigDecimal.valueOf(entry.getEmi()));
            newRow.setPrincipalPaid(BigDecimal.valueOf(entry.getPrincipalPaid()));
            newRow.setInterestPaid(BigDecimal.valueOf(entry.getInterestPaid()));
            newRow.setRemainingBalance(BigDecimal.valueOf(Math.max(0, entry.getRemainingBalance())));
            newRow.setPaymentType("NORMAL");
            newRow.setChangeMarker(false);
            hybridRows.add(newRow);
            newRowsAdded++;
            
            // Stop after adding the row that brings balance to zero or very close to zero
            if (entry.getRemainingBalance() <= 0.01) {
                break;
            }
        }
        
        rowRepository.saveAll(hybridRows);
        log.info("Created {} hybrid repayment rows ({} paid + {} new)", 
                hybridRows.size(), paidInstallments, newCalculation.getPaymentSchedule().size());
    }
    
    private void copyPaidRows(RepaymentSnapshotEntity snapshot, List<RepaymentRowEntity> paidRows) {
        List<RepaymentRowEntity> copiedRows = new ArrayList<>();
        
        for (RepaymentRowEntity paidRow : paidRows) {
            RepaymentRowEntity newRow = new RepaymentRowEntity();
            newRow.setSnapshot(snapshot);
            newRow.setMonthNumber(paidRow.getMonthNumber());
            newRow.setPaymentDate(paidRow.getPaymentDate());
            newRow.setEmi(paidRow.getEmi());
            newRow.setPrincipalPaid(paidRow.getPrincipalPaid());
            newRow.setInterestPaid(paidRow.getInterestPaid());
            newRow.setRemainingBalance(paidRow.getRemainingBalance());
            newRow.setPaymentType("PAID");
            // newRow.setChangeMarker(false); // Remove if field doesn't exist
            copiedRows.add(newRow);
        }
        
        rowRepository.saveAll(copiedRows);
    }
}
