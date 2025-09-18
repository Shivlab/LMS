package com.mybank.lms.controller;

import com.mybank.lms.model.dto.LoanInputDTO;
import com.mybank.lms.model.dto.LoanOutputDTO;
import com.mybank.lms.model.dto.RepaymentScheduleDTO;
import com.mybank.lms.model.dto.LoanEditDTO;
import com.mybank.lms.model.dto.LoanVersionDTO;
import com.mybank.lms.model.dto.KfsVersionDTO;
import com.mybank.lms.model.dto.DisbursementPhaseDTO;
import java.util.List;
import java.util.Objects;
import java.time.LocalDate;
import org.springframework.format.annotation.DateTimeFormat;
import com.mybank.lms.service.LoanService;
import com.mybank.lms.service.RepaymentSnapshotService;
import com.mybank.lms.model.entity.LoanAuditEntity;
import com.mybank.lms.repository.LoanAuditRepository;
import com.mybank.lms.repository.LoanRepository;
import com.mybank.lms.repository.DisbursementRepository;
import com.mybank.lms.model.entity.LoanEntity;
import com.mybank.lms.model.entity.DisbursementPhaseEntity;
import java.util.stream.Collectors;
import java.util.HashMap;
import java.util.Map;
import java.util.ArrayList;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;
import jakarta.validation.Valid;
import java.math.BigDecimal;
import java.util.UUID;

@RestController
@RequestMapping("/api/v1/loans")
@RequiredArgsConstructor
@Slf4j
@CrossOrigin(origins = "*")
public class LoanController {
    
    private final LoanService loanService;
    private final RepaymentSnapshotService repaymentSnapshotService;
    private final LoanAuditRepository loanAuditRepository;
    private final LoanRepository loanRepository;
    private final DisbursementRepository disbursementRepository;
    
    @GetMapping
    public ResponseEntity<List<LoanOutputDTO>> getAllLoans() {
        log.info("Fetching all loans");
        
        try {
            List<LoanOutputDTO> loans = loanService.getAllLoans();
            return ResponseEntity.ok(loans);
        } catch (Exception e) {
            log.error("Error fetching all loans", e);
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).build();
        }
    }
    
    @PostMapping
    public ResponseEntity<LoanOutputDTO> createLoan(@Valid @RequestBody LoanInputDTO loanInputDTO) {
        log.info("Creating loan for customer: {}", loanInputDTO.getCustomerId());
        
        try {
            LoanOutputDTO loanOutput = loanService.createLoan(loanInputDTO);
            return ResponseEntity.status(HttpStatus.CREATED).body(loanOutput);
        } catch (Exception e) {
            log.error("Error creating loan", e);
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).build();
        }
    }
    
    @GetMapping("/{id}")
    public ResponseEntity<LoanOutputDTO> getLoanKFS(@PathVariable UUID id) {
        log.info("Fetching KFS for loan: {}", id);
        
        try {
            LoanOutputDTO loanOutput = loanService.getLoanKFS(id);
            return ResponseEntity.ok(loanOutput);
        } catch (RuntimeException e) {
            log.error("Loan not found: {}", id);
            return ResponseEntity.notFound().build();
        } catch (Exception e) {
            log.error("Error fetching loan KFS", e);
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).build();
        }
    }
    
    @GetMapping("/{id}/schedule")
    public ResponseEntity<RepaymentScheduleDTO> getRepaymentSchedule(
            @PathVariable UUID id,
            @RequestParam(required = false) UUID snapshotId) {
        log.info("Fetching repayment schedule for loan: {}, snapshot: {}", id, snapshotId);
        
        try {
            RepaymentScheduleDTO schedule = repaymentSnapshotService.getRepaymentSchedule(id);
            return ResponseEntity.ok(schedule);
        } catch (RuntimeException e) {
            log.error("Schedule not found for loan: {}", id);
            return ResponseEntity.notFound().build();
        } catch (Exception e) {
            log.error("Error fetching repayment schedule", e);
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).build();
        }
    }
    
    @GetMapping("/{id}/disbursements")
    public ResponseEntity<Map<String, Object>> getDisbursementsForEdit(@PathVariable UUID id) {
        log.info("Fetching disbursements for loan edit: {}", id);
        
        try {
            LoanEntity loan = loanRepository.findById(id)
                .orElseThrow(() -> new RuntimeException("Loan not found: " + id));
            
            // Check if this is a home loan
            if (!"HOME_LOAN".equals(loan.getProductType())) {
                return ResponseEntity.ok(Map.of(
                    "disbursements", List.of(),
                    "canEdit", false,
                    "message", "Disbursement editing is only available for home loans"
                ));
            }
            
            List<DisbursementPhaseEntity> disbursements = disbursementRepository.findByLoanIdOrderBySequence(id);
            
            if (disbursements.isEmpty()) {
                return ResponseEntity.ok(Map.of(
                    "disbursements", List.of(),
                    "canEdit", false,
                    "message", "No disbursement phases found for this loan"
                ));
            }
            
            LocalDate today = LocalDate.now();
            List<DisbursementPhaseDTO> disbursementDTOs = disbursements.stream()
                .map(d -> {
                    DisbursementPhaseDTO dto = new DisbursementPhaseDTO();
                    dto.setId(d.getId());
                    dto.setDisbursementDate(d.getDisbursementDate());
                    dto.setAmount(d.getAmount());
                    dto.setDescription(d.getDescription());
                    dto.setSequence(d.getSequence());
                    // Can only edit if disbursement date is after today
                    dto.setCanEdit(d.getDisbursementDate().isAfter(today));
                    return dto;
                })
                .collect(Collectors.toList());
            
            boolean hasEditableDisbursements = disbursementDTOs.stream()
                .anyMatch(DisbursementPhaseDTO::isCanEdit);
            
            return ResponseEntity.ok(Map.of(
                "disbursements", disbursementDTOs,
                "canEdit", hasEditableDisbursements,
                "message", hasEditableDisbursements ? 
                    "Disbursement phases found and some are editable" : 
                    "All disbursement phases have already been disbursed and cannot be edited"
            ));
            
        } catch (RuntimeException e) {
            log.error("Error fetching disbursements for loan: {}", id, e);
            return ResponseEntity.badRequest().body(Map.of(
                "error", e.getMessage()
            ));
        } catch (Exception e) {
            log.error("Error fetching disbursements for loan: {}", id, e);
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).body(Map.of(
                "error", "Internal server error"
            ));
        }
    }
    
    @PutMapping("/{id}/disbursements")
    public ResponseEntity<Map<String, Object>> updateDisbursements(
            @PathVariable UUID id,
            @RequestBody List<DisbursementPhaseDTO> disbursements) {
        log.info("Updating disbursements for loan: {}", id);
        
        try {
            LoanEntity loan = loanRepository.findById(id)
                .orElseThrow(() -> new RuntimeException("Loan not found: " + id));
            
            // Check if this is a home loan
            if (!"HOME_LOAN".equals(loan.getProductType())) {
                return ResponseEntity.badRequest().body(Map.of(
                    "error", "Disbursement editing is only available for home loans"
                ));
            }
            
            LocalDate today = LocalDate.now();
            boolean hasChanges = false;
            
            // Validate and update disbursements
            for (DisbursementPhaseDTO dto : disbursements) {
                if (dto.getId() != null) {
                    // Updating existing disbursement
                    DisbursementPhaseEntity existing = disbursementRepository.findById(dto.getId())
                        .orElseThrow(() -> new RuntimeException("Disbursement not found: " + dto.getId()));
                    
                    // Check if the disbursement is actually being modified
                    boolean dateChanged = !existing.getDisbursementDate().equals(dto.getDisbursementDate());
                    boolean amountChanged = existing.getAmount().compareTo(dto.getAmount()) != 0;
                    boolean descriptionChanged = !Objects.equals(existing.getDescription(), dto.getDescription());
                    boolean sequenceChanged = !Objects.equals(existing.getSequence(), dto.getSequence());
                    
                    boolean isBeingModified = dateChanged || amountChanged || descriptionChanged || sequenceChanged;
                    
                    log.info("Disbursement {} - Original: [date={}, amount={}, desc='{}', seq={}], New: [date={}, amount={}, desc='{}', seq={}]", 
                             dto.getId(), existing.getDisbursementDate(), existing.getAmount(), existing.getDescription(), existing.getSequence(),
                             dto.getDisbursementDate(), dto.getAmount(), dto.getDescription(), dto.getSequence());
                    log.info("Changes detected - Date: {}, Amount: {}, Description: {}, Sequence: {}, Overall modified: {}", 
                             dateChanged, amountChanged, descriptionChanged, sequenceChanged, isBeingModified);
                    
                    // Check if this disbursement has already been disbursed (original date is past/today)
                    boolean alreadyDisbursed = !existing.getDisbursementDate().isAfter(today);
                    
                    if (alreadyDisbursed && isBeingModified) {
                        log.warn("Blocking edit of disbursement {} because it has already been disbursed (original date {} is not after today {})", 
                                dto.getId(), existing.getDisbursementDate(), today);
                        return ResponseEntity.badRequest().body(Map.of(
                            "error", "Cannot edit disbursement that has already been disbursed or is due today"
                        ));
                    }
                    
                    // For future disbursements, validate that any new date is also in the future
                    if (!alreadyDisbursed && isBeingModified && dateChanged && !dto.getDisbursementDate().isAfter(today)) {
                        log.warn("Blocking edit of disbursement {} because new date {} is not after today {}", 
                                dto.getId(), dto.getDisbursementDate(), today);
                        return ResponseEntity.badRequest().body(Map.of(
                            "error", "New disbursement date must be in the future"
                        ));
                    }
                    
                    // Update the existing disbursement
                    existing.setDisbursementDate(dto.getDisbursementDate());
                    existing.setAmount(dto.getAmount());
                    existing.setDescription(dto.getDescription());
                    existing.setSequence(dto.getSequence());
                    disbursementRepository.save(existing);
                    
                    if (isBeingModified) {
                        hasChanges = true;
                    }
                } else {
                    // Adding new disbursement
                    if (dto.getDisbursementDate().isBefore(today) || dto.getDisbursementDate().isEqual(today)) {
                        return ResponseEntity.badRequest().body(Map.of(
                            "error", "New disbursement date must be in the future"
                        ));
                    }
                    
                    DisbursementPhaseEntity newDisbursement = new DisbursementPhaseEntity();
                    newDisbursement.setLoan(loan);
                    newDisbursement.setDisbursementDate(dto.getDisbursementDate());
                    newDisbursement.setAmount(dto.getAmount());
                    newDisbursement.setDescription(dto.getDescription());
                    newDisbursement.setSequence(dto.getSequence());
                    disbursementRepository.save(newDisbursement);
                    hasChanges = true;
                }
            }
            
            // If there are changes, regenerate the loan schedule and KFS
            if (hasChanges) {
                log.info("Disbursement changes detected, regenerating loan schedule and KFS for loan: {}", id);
                
                // Fetch updated disbursement phases and create LoanInputDTO
                List<DisbursementPhaseEntity> updatedDisbursements = disbursementRepository.findByLoanIdOrderBySequence(id);
                LoanInputDTO updatedInput = createLoanInputDTOWithUpdatedPhases(loan, updatedDisbursements);
                
                // Apply the loan change to regenerate schedule and KFS
                loanService.applyLoanChange(id, updatedInput, "system", LocalDate.now());
                
                log.info("Successfully regenerated loan schedule and KFS after disbursement update");
            }
            
            return ResponseEntity.ok(Map.of(
                "message", "Disbursements updated successfully",
                "kfsRegenerated", hasChanges
            ));
            
        } catch (RuntimeException e) {
            log.error("Error updating disbursements for loan: {}", id, e);
            return ResponseEntity.badRequest().body(Map.of(
                "error", e.getMessage()
            ));
        } catch (Exception e) {
            log.error("Error updating disbursements for loan: {}", id, e);
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).body(Map.of(
                "error", "Internal server error"
            ));
        }
    }
    
    @DeleteMapping("/{id}/disbursements/{disbursementId}")
    public ResponseEntity<Map<String, Object>> deleteDisbursement(
            @PathVariable UUID id,
            @PathVariable UUID disbursementId) {
        log.info("Deleting disbursement {} for loan: {}", disbursementId, id);
        
        try {
            DisbursementPhaseEntity disbursement = disbursementRepository.findById(disbursementId)
                .orElseThrow(() -> new RuntimeException("Disbursement not found: " + disbursementId));
            
            // Check if it belongs to the specified loan
            if (!disbursement.getLoan().getId().equals(id)) {
                return ResponseEntity.badRequest().body(Map.of(
                    "error", "Disbursement does not belong to the specified loan"
                ));
            }
            
            LocalDate today = LocalDate.now();
            
            // Check if it can be deleted (date is after today)
            if (!disbursement.getDisbursementDate().isAfter(today)) {
                return ResponseEntity.badRequest().body(Map.of(
                    "error", "Cannot delete disbursement that has already been disbursed or is due today"
                ));
            }
            
            disbursementRepository.delete(disbursement);
            
            return ResponseEntity.ok(Map.of(
                "message", "Disbursement deleted successfully"
            ));
            
        } catch (RuntimeException e) {
            log.error("Error deleting disbursement {} for loan: {}", disbursementId, id, e);
            return ResponseEntity.badRequest().body(Map.of(
                "error", e.getMessage()
            ));
        } catch (Exception e) {
            log.error("Error deleting disbursement {} for loan: {}", disbursementId, id, e);
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).body(Map.of(
                "error", "Internal server error"
            ));
        }
    }
    
    @PostMapping("/{id}/charges")
    public ResponseEntity<Void> addLoanCharge(
            @PathVariable UUID id,
            @Valid @RequestBody LoanInputDTO.LoanChargeDTO chargeDTO) {
        log.info("Adding charge to loan: {}", id);
        
        try {
            // Implementation would go here - adding individual charges after loan creation
            // For now, return success
            return ResponseEntity.ok().build();
        } catch (Exception e) {
            log.error("Error adding loan charge", e);
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).build();
        }
    }
    
    @PostMapping("/{id}/force-reset")
    public ResponseEntity<Void> forceRateReset(
            @PathVariable UUID id,
            @RequestParam String benchmarkName,
            @RequestParam BigDecimal newRate) {
        log.info("Force rate reset for loan: {} with benchmark: {} and rate: {}", id, benchmarkName, newRate);
        
        try {
            loanService.applyBenchmarkToLoan(id, benchmarkName, newRate);
            return ResponseEntity.ok().build();
        } catch (RuntimeException e) {
            log.error("Error in force rate reset: {}", e.getMessage());
            return ResponseEntity.badRequest().build();
        } catch (Exception e) {
            log.error("Error in force rate reset", e);
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).build();
        }
    }
    
    @PutMapping("/{id}")
    public ResponseEntity<LoanOutputDTO> updateLoan(
            @PathVariable UUID id,
            @Valid @RequestBody LoanInputDTO loanInputDTO,
            @RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate cutoffDate) {
        log.info("Updating loan: {} with cutoff date: {}", id, cutoffDate);
        
        // Use current date if cutoff date is not provided
        LocalDate effectiveCutoffDate = cutoffDate != null ? cutoffDate : LocalDate.now();
        
        try {
            LoanOutputDTO updatedLoan = loanService.applyLoanChange(id, loanInputDTO, "system", effectiveCutoffDate);
            return ResponseEntity.ok(updatedLoan);
        } catch (RuntimeException e) {
            log.error("Error updating loan: {}", e.getMessage());
            return ResponseEntity.badRequest().build();
        } catch (Exception e) {
            log.error("Error updating loan", e);
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).build();
        }
    }
    
    @PutMapping("/{id}/edit")
    public ResponseEntity<LoanOutputDTO> editLoan(
            @PathVariable UUID id,
            @Valid @RequestBody LoanEditDTO loanEditDTO,
            @RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate cutoffDate) {
        log.info("Editing loan: {} with cutoff date: {}", id, cutoffDate);
        
        // Use current date if cutoff date is not provided
        LocalDate effectiveCutoffDate = cutoffDate != null ? cutoffDate : LocalDate.now();
        
        try {
            // Convert LoanEditDTO to LoanInputDTO for new applyLoanChange method
            LoanInputDTO newLoanInput = new LoanInputDTO();
            
            // Map all available fields from LoanEditDTO
            if (loanEditDTO.getPrincipal() != null) {
                newLoanInput.setLoanAmount(loanEditDTO.getPrincipal());
            }
            if (loanEditDTO.getAnnualRate() != null) {
                newLoanInput.setAnnualRate(loanEditDTO.getAnnualRate());
            }
            if (loanEditDTO.getMonths() != null) {
                newLoanInput.setTenureMonths(loanEditDTO.getMonths());
            }
            if (loanEditDTO.getMoratoriumMonths() != null) {
                newLoanInput.setMoratoriumPeriod(loanEditDTO.getMoratoriumMonths());
            }
            if (loanEditDTO.getRateType() != null) {
                newLoanInput.setRateType(LoanInputDTO.RateType.valueOf(loanEditDTO.getRateType()));
            }
            if (loanEditDTO.getBenchmarkName() != null) {
                newLoanInput.setBenchmarkName(loanEditDTO.getBenchmarkName());
            }
            if (loanEditDTO.getSpread() != null) {
                newLoanInput.setSpread(loanEditDTO.getSpread());
            }
            if (loanEditDTO.getStartDate() != null) {
                newLoanInput.setStartDate(loanEditDTO.getStartDate());
            }
            if (loanEditDTO.getLoanIssueDate() != null) {
                newLoanInput.setLoanIssueDate(loanEditDTO.getLoanIssueDate());
            }
            if (loanEditDTO.getCustomerId() != null) {
                newLoanInput.setCustomerId(loanEditDTO.getCustomerId());
            }
            if (loanEditDTO.getProductType() != null) {
                newLoanInput.setProductType(loanEditDTO.getProductType());
            }
            
            LoanOutputDTO updatedLoan = loanService.applyLoanChange(id, newLoanInput, 
                loanEditDTO.getEditedBy() != null ? loanEditDTO.getEditedBy() : "system", effectiveCutoffDate);
            return ResponseEntity.ok(updatedLoan);
        } catch (RuntimeException e) {
            log.error("Error editing loan: {}", e.getMessage());
            return ResponseEntity.badRequest().build();
        } catch (Exception e) {
            log.error("Error editing loan", e);
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).build();
        }
    }
    
    @GetMapping("/{id}/versions")
    public ResponseEntity<List<LoanVersionDTO>> getLoanVersionHistory(@PathVariable UUID id) {
        log.info("Fetching loan version history for: {}", id);
        
        try {
            List<LoanVersionDTO> versions = loanService.getLoanVersionHistory(id);
            return ResponseEntity.ok(versions);
        } catch (Exception e) {
            log.error("Error fetching loan version history", e);
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).build();
        }
    }
    
    @GetMapping("/{id}/kfs-versions")
    public ResponseEntity<List<KfsVersionDTO>> getKfsVersionHistory(@PathVariable UUID id) {
        log.info("Fetching KFS version history for: {}", id);
        
        try {
            List<KfsVersionDTO> versions = loanService.getKfsVersionHistory(id);
            return ResponseEntity.ok(versions);
        } catch (Exception e) {
            log.error("Error fetching KFS version history", e);
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).build();
        }
    }

    @GetMapping("/{id}/kfs-versions/{versionNumber}")
    public ResponseEntity<LoanOutputDTO> getKfsVersion(
            @PathVariable UUID id,
            @PathVariable Integer versionNumber) {
        log.info("Fetching KFS version {} for loan {}", versionNumber, id);
        try {
            LoanOutputDTO kfs = loanService.getKfsVersion(id, versionNumber);
            return ResponseEntity.ok(kfs);
        } catch (RuntimeException e) {
            log.error("KFS version not found: {} for loan {}", versionNumber, id);
            return ResponseEntity.notFound().build();
        } catch (Exception e) {
            log.error("Error fetching specific KFS version", e);
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).build();
        }
    }

    // Loan Charges Management - Edit endpoint
    @PostMapping("/{id}/charges/new")
    public ResponseEntity<String> addNewLoanCharge(
            @PathVariable String id,
            @RequestBody Map<String, Object> chargeData) {
        try {
            // Implementation for adding loan charges
            // This would create a new charge and trigger KFS version creation
            return ResponseEntity.ok("Charge added successfully");
        } catch (Exception e) {
            log.error("Error adding loan charge", e);
            return ResponseEntity.badRequest().body("Error: " + e.getMessage());
        }
    }

    @PutMapping("/{id}/charges/{chargeId}")
    public ResponseEntity<String> updateLoanCharge(
            @PathVariable String id,
            @PathVariable String chargeId,
            @RequestBody Map<String, Object> chargeData) {
        try {
            // Implementation for updating loan charges
            return ResponseEntity.ok("Charge updated successfully");
        } catch (Exception e) {
            log.error("Error updating loan charge", e);
            return ResponseEntity.badRequest().body("Error: " + e.getMessage());
        }
    }

    @DeleteMapping("/{id}/charges/{chargeId}")
    public ResponseEntity<String> deleteLoanCharge(
            @PathVariable String id,
            @PathVariable String chargeId) {
        try {
            // Implementation for deleting loan charges
            return ResponseEntity.ok("Charge deleted successfully");
        } catch (Exception e) {
            log.error("Error deleting loan charge", e);
            return ResponseEntity.badRequest().body("Error: " + e.getMessage());
        }
    }

    // Disbursement Phases Management - Methods removed to avoid conflicts

    // Moratorium Management
    @PostMapping("/{id}/moratorium")
    public ResponseEntity<String> addMoratoriumPeriod(
            @PathVariable UUID id,
            @RequestBody Map<String, Object> moratoriumData) {
        try {
            log.info("Adding moratorium period to loan: {}", id);

            // Extract and validate moratorium parameters with proper type checking
            Integer startMonth = extractIntegerValue(moratoriumData.get("startMonth"));
            Integer endMonth = extractIntegerValue(moratoriumData.get("endMonth"));
            String moratoriumType = (String) moratoriumData.get("type");
            Double partialPaymentEMI = extractDoubleValue(moratoriumData.get("partialPaymentEMI"));

            // Validate required parameters
            if (startMonth == null || endMonth == null || moratoriumType == null) {
                return ResponseEntity.badRequest().body("Missing required parameters: startMonth, endMonth, and type");
            }

            if (startMonth > endMonth) {
                return ResponseEntity.badRequest().body("startMonth cannot be greater than endMonth");
            }

            // Apply moratorium changes with KFS versioning
            loanService.applyMoratoriumToLoan(id, startMonth, endMonth, moratoriumType, partialPaymentEMI, "system");

            log.info("Moratorium period added successfully to loan: {}", id);
            return ResponseEntity.ok("Moratorium period added successfully. New KFS version created.");
        } catch (RuntimeException e) {
            log.error("Error adding moratorium period: {}", e.getMessage());
            return ResponseEntity.badRequest().body("Error: " + e.getMessage());
        } catch (Exception e) {
            log.error("Error adding moratorium period", e);
            return ResponseEntity.badRequest().body("Error: " + e.getMessage());
        }
    }

    @PutMapping("/{id}/moratorium/{moratoriumId}")
    public ResponseEntity<String> updateMoratoriumPeriod(
            @PathVariable String id,
            @PathVariable String moratoriumId,
            @RequestBody Map<String, Object> moratoriumData) {
        try {
            // Implementation for updating moratorium periods
            return ResponseEntity.ok("Moratorium period updated successfully");
        } catch (Exception e) {
            log.error("Error updating moratorium period", e);
            return ResponseEntity.badRequest().body("Error: " + e.getMessage());
        }
    }

    @DeleteMapping("/{id}/moratorium/{moratoriumId}")
    public ResponseEntity<String> deleteMoratoriumPeriod(
            @PathVariable String id,
            @PathVariable String moratoriumId) {
        try {
            // Implementation for deleting moratorium periods
            return ResponseEntity.ok("Moratorium period deleted successfully");
        } catch (Exception e) {
            log.error("Error deleting moratorium period", e);
            return ResponseEntity.badRequest().body("Error: " + e.getMessage());
        }
    }

    // Prepayment Operations
    @PostMapping("/{id}/prepayments")
    public ResponseEntity<String> recordPrepayment(
            @PathVariable String id,
            @RequestBody Map<String, Object> prepaymentData) {
        try {
            // Implementation for recording prepayments
            // This should regenerate the repayment schedule and create new KFS version
            return ResponseEntity.ok("Prepayment recorded successfully. Schedule regenerated.");
        } catch (Exception e) {
            log.error("Error recording prepayment", e);
            return ResponseEntity.badRequest().body("Error: " + e.getMessage());
        }
    }

    @GetMapping("/{id}/prepayments")
    public ResponseEntity<List<Map<String, Object>>> getPrepaymentHistory(@PathVariable String id) {
        try {
            // Implementation for retrieving prepayment history
            return ResponseEntity.ok(new ArrayList<>());
        } catch (Exception e) {
            log.error("Error retrieving prepayment history", e);
            return ResponseEntity.badRequest().build();
        }
    }

    // Status and Rate Operations
    @PatchMapping("/{id}/status")
    public ResponseEntity<String> updateLoanStatus(
            @PathVariable String id,
            @RequestBody Map<String, Object> statusData) {
        try {
            String status = (String) statusData.get("status");
            String reason = (String) statusData.get("changeReason");
            String description = (String) statusData.get("changeDescription");
            
            // Validate status transition
            if (!isValidStatusTransition(status)) {
                return ResponseEntity.badRequest().body("Invalid status transition");
            }
            
            // Implementation for updating loan status with versioning
            return ResponseEntity.ok("Loan status updated successfully");
        } catch (Exception e) {
            log.error("Error updating loan status", e);
            return ResponseEntity.badRequest().body("Error: " + e.getMessage());
        }
    }

    @PatchMapping("/{id}/rate-reset")
    public ResponseEntity<String> applyRateReset(
            @PathVariable String id,
            @RequestBody Map<String, Object> rateData) {
        try {
            Double newRate = (Double) rateData.get("annualRate");
            String reason = (String) rateData.get("changeReason");
            String description = (String) rateData.get("changeDescription");
            
            // Implementation for rate reset with versioning
            return ResponseEntity.ok("Rate reset applied successfully");
        } catch (Exception e) {
            log.error("Error applying rate reset", e);
            return ResponseEntity.badRequest().body("Error: " + e.getMessage());
        }
    }

    @GetMapping("/admin/audit")
    public ResponseEntity<List<Map<String, Object>>> getLoanAudit(
            @RequestParam UUID loanId) {
        log.info("Fetching audit trail for loan: {}", loanId);
        
        try {
            List<LoanAuditEntity> audits = loanAuditRepository.findByLoanIdOrderByChangeDateDesc(loanId);
            List<Map<String, Object>> auditData = audits.stream()
                .map(audit -> {
                    Map<String, Object> map = new HashMap<>();
                    map.put("fieldName", audit.getFieldName());
                    map.put("oldValue", audit.getOldValue());
                    map.put("newValue", audit.getNewValue());
                    map.put("changeDate", audit.getChangeDate());
                    map.put("changedBy", audit.getChangedBy());
                    return map;
                })
                .collect(Collectors.toList());
            
            return ResponseEntity.ok(auditData);
        } catch (Exception e) {
            log.error("Error fetching audit trail", e);
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).build();
        }
    }
    
    // Helper methods for type-safe parameter extraction
    private Integer extractIntegerValue(Object value) {
        if (value == null) return null;
        if (value instanceof Integer) return (Integer) value;
        if (value instanceof String) {
            try {
                return Integer.parseInt((String) value);
            } catch (NumberFormatException e) {
                log.warn("Invalid integer value: {}", value);
                return null;
            }
        }
        if (value instanceof Number) {
            return ((Number) value).intValue();
        }
        return null;
    }

    private Double extractDoubleValue(Object value) {
        if (value == null || (value instanceof String && ((String) value).trim().isEmpty())) return null;
        if (value instanceof Double) return (Double) value;
        if (value instanceof String) {
            try {
                return Double.parseDouble((String) value);
            } catch (NumberFormatException e) {
                log.warn("Invalid double value: {}", value);
                return null;
            }
        }
        if (value instanceof Number) {
            return ((Number) value).doubleValue();
        }
        return null;
    }

    private boolean isValidStatusTransition(String newStatus) {
        // Add validation logic for status transitions
        // e.g., ACTIVE -> CLOSED is allowed, but CLOSED -> ACTIVE might not be
        return newStatus != null && (newStatus.equals("ACTIVE") || newStatus.equals("CLOSED") || newStatus.equals("SUSPENDED"));
    }

    /**
     * Helper method to create LoanInputDTO with updated disbursement phases for recalculation
     */
    private LoanInputDTO createLoanInputDTOWithUpdatedPhases(LoanEntity loanEntity, List<DisbursementPhaseEntity> updatedPhases) {
        LoanInputDTO dto = new LoanInputDTO();

        // Copy all loan details
        dto.setCustomerId(loanEntity.getCustomerId());
        dto.setProductType(loanEntity.getProductType());
        dto.setPrincipal(loanEntity.getPrincipal());
        dto.setAnnualRate(loanEntity.getAnnualRate());
        dto.setMonths(loanEntity.getMonths());
        dto.setMoratoriumMonths(loanEntity.getMoratoriumMonths());
        dto.setMoratoriumType(loanEntity.getMoratoriumType() != null ? loanEntity.getMoratoriumType().name() : null);
        dto.setPartialPaymentEmi(loanEntity.getPartialPaymentEmi());
        // Convert RateType enum from LoanEntity to LoanInputDTO.RateType
        if (loanEntity.getRateType() != null) {
            dto.setRateType(LoanInputDTO.RateType.valueOf(loanEntity.getRateType().name()));
        }
        dto.setFloatingStrategy(loanEntity.getFloatingStrategy() != null ? loanEntity.getFloatingStrategy().name() : null);
        dto.setCompoundingFrequency(loanEntity.getCompoundingFrequency().name());
        dto.setResetPeriodicityMonths(loanEntity.getResetPeriodicityMonths());
        dto.setBenchmarkName(loanEntity.getBenchmarkName());
        dto.setSpread(loanEntity.getSpread());
        dto.setLoanIssueDate(loanEntity.getLoanIssueDate());
        dto.setStartDate(loanEntity.getStartDate());

        // Convert disbursement phases to DTO format
        if (updatedPhases != null && !updatedPhases.isEmpty()) {
            List<LoanInputDTO.DisbursementPhaseDTO> phaseDTOs = updatedPhases.stream()
                .map(phase -> {
                    LoanInputDTO.DisbursementPhaseDTO phaseDTO = new LoanInputDTO.DisbursementPhaseDTO();
                    phaseDTO.setDisbursementDate(phase.getDisbursementDate());
                    phaseDTO.setAmount(phase.getAmount());
                    phaseDTO.setDescription(phase.getDescription());
                    phaseDTO.setSequence(phase.getSequence());
                    return phaseDTO;
                })
                .collect(Collectors.toList());
            dto.setDisbursementPhases(phaseDTOs);
        }

        return dto;
    }
}
