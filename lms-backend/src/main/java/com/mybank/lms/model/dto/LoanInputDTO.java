package com.mybank.lms.model.dto;

import com.fasterxml.jackson.annotation.JsonFormat;
import lombok.Data;
import lombok.NoArgsConstructor;
import lombok.AllArgsConstructor;

import jakarta.validation.constraints.*;
import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.List;
import java.util.UUID;

@Data
@NoArgsConstructor
@AllArgsConstructor
public class LoanInputDTO {
    
    private String customerId;
    
    @NotBlank(message = "Product type is required")
    private String productType;
    
    @JsonFormat(pattern = "yyyy-MM-dd")
    private LocalDate loanIssueDate;
    
    @JsonFormat(pattern = "yyyy-MM-dd")
    private LocalDate startDate;
    
    @NotNull(message = "Principal amount is required")
    @DecimalMin(value = "0.01", message = "Principal must be greater than 0")
    private BigDecimal principal;
    
    // Alias for principal to match LoanEntity
    public BigDecimal getLoanAmount() {
        return principal;
    }
    
    public void setLoanAmount(BigDecimal loanAmount) {
        this.principal = loanAmount;
    }
    
    @NotNull(message = "Annual rate is required")
    @DecimalMin(value = "0.01", message = "Annual rate must be greater than 0")
    @DecimalMax(value = "50.00", message = "Annual rate must be less than 50%")
    private BigDecimal annualRate;
    
    @NotBlank(message = "Rate type is required")
    private String rateType; // FIXED, FLOATING
    
    public enum RateType {
        FIXED, FLOATING
    }
    
    public RateType getRateTypeEnum() {
        return rateType != null ? RateType.valueOf(rateType) : null;
    }
    
    public void setRateType(RateType rateType) {
        this.rateType = rateType != null ? rateType.name() : null;
    }
    
    private String floatingStrategy; // EMI_CONSTANT, TENURE_CONSTANT
    
    @NotNull(message = "Loan tenure in months is required")
    @Min(value = 1, message = "Months must be at least 1")
    @Max(value = 600, message = "Months cannot exceed 600")
    private Integer months;
    
    // Alias for months to match LoanEntity
    public Integer getTenureMonths() {
        return months;
    }
    
    public void setTenureMonths(Integer tenureMonths) {
        this.months = tenureMonths;
    }
    
    @Min(value = 0, message = "Moratorium months cannot be negative")
    private Integer moratoriumMonths = 0;
    
    // Alias for moratoriumMonths to match LoanEntity
    public Integer getMoratoriumPeriod() {
        return moratoriumMonths;
    }
    
    public void setMoratoriumPeriod(Integer moratoriumPeriod) {
        this.moratoriumMonths = moratoriumPeriod;
    }
    
    private String moratoriumType; // FULL, INTEREST_ONLY, PARTIAL
    
    private BigDecimal partialPaymentEmi;
    
    @NotBlank(message = "Compounding frequency is required")
    private String compoundingFrequency; // DAILY, MONTHLY
    
    private Integer resetPeriodicityMonths;
    
    private String benchmarkName;
    
    private BigDecimal spread;
    
    private List<DisbursementPhaseDTO> disbursementPhases;
    
    private List<LoanChargeDTO> charges;
    
    @Data
    @NoArgsConstructor
    @AllArgsConstructor
    public static class DisbursementPhaseDTO {
        @JsonFormat(pattern = "yyyy-MM-dd")
        @NotNull(message = "Disbursement date is required")
        private LocalDate disbursementDate;
        
        @NotNull(message = "Disbursement amount is required")
        @DecimalMin(value = "0.01", message = "Amount must be greater than 0")
        private BigDecimal amount;
        
        private String description;
        
        private Integer sequence;
    }
    
    @Data
    @NoArgsConstructor
    @AllArgsConstructor
    public static class LoanChargeDTO {
        @NotBlank(message = "Charge type is required")
        private String chargeType;
        
        private String payableTo;
        
        private Boolean isRecurring = false;
        
        @NotNull(message = "Charge amount is required")
        @DecimalMin(value = "0.01", message = "Amount must be greater than 0")
        private BigDecimal amount;
    }
}
