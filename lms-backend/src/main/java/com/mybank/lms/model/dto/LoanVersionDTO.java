package com.mybank.lms.model.dto;

import com.fasterxml.jackson.annotation.JsonFormat;
import lombok.Data;
import lombok.NoArgsConstructor;
import lombok.AllArgsConstructor;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.UUID;

@Data
@NoArgsConstructor
@AllArgsConstructor
public class LoanVersionDTO {
    
    private UUID id;
    private UUID loanId;
    private Integer versionNumber;
    
    // Loan parameters at this version
    private BigDecimal principal;
    private BigDecimal annualRate;
    private Integer months;
    private Integer moratoriumMonths;
    private String moratoriumType;
    private BigDecimal partialPaymentEmi;
    private String rateType;
    private String floatingStrategy;
    private String compoundingFrequency;
    private Integer resetPeriodicityMonths;
    private String benchmarkName;
    private BigDecimal spread;
    
    @JsonFormat(pattern = "yyyy-MM-dd")
    private LocalDate loanIssueDate;
    
    @JsonFormat(pattern = "yyyy-MM-dd")
    private LocalDate startDate;
    
    private String productType;
    private String customerId;
    
    // Version metadata
    private String changeReason;
    private String changeDescription;
    private String changedFields;
    private String previousValues;
    
    @JsonFormat(pattern = "yyyy-MM-dd HH:mm:ss")
    private LocalDateTime createdAt;
    
    private String createdBy;
    
    @JsonFormat(pattern = "yyyy-MM-dd HH:mm:ss")
    private LocalDateTime effectiveFrom;
}
