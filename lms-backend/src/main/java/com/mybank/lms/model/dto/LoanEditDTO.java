package com.mybank.lms.model.dto;

import com.fasterxml.jackson.annotation.JsonFormat;
import lombok.Data;
import lombok.NoArgsConstructor;
import lombok.AllArgsConstructor;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.UUID;

@Data
@NoArgsConstructor
@AllArgsConstructor
public class LoanEditDTO {
    
    private UUID loanId;
    
    // Editable loan parameters
    private BigDecimal principal;
    private BigDecimal annualRate;
    private Integer months;
    private Integer moratoriumMonths;
    private String moratoriumType; // FULL, PARTIAL, NONE
    private BigDecimal partialPaymentEmi;
    private String rateType; // FIXED, FLOATING
    private String floatingStrategy; // EMI_CONSTANT, TENURE_CONSTANT
    private String compoundingFrequency; // MONTHLY, QUARTERLY, ANNUALLY
    private Integer resetPeriodicityMonths;
    private String benchmarkName;
    private BigDecimal spread;
    
    @JsonFormat(pattern = "yyyy-MM-dd")
    private LocalDate loanIssueDate;
    
    @JsonFormat(pattern = "yyyy-MM-dd")
    private LocalDate startDate;
    
    private String productType;
    private String customerId;
    
    // Edit metadata
    private String changeReason; // Enum value as string
    private String changeDescription;
    private String editedBy;
    
    @JsonFormat(pattern = "yyyy-MM-dd HH:mm:ss")
    private java.time.LocalDateTime effectiveFrom;
}
