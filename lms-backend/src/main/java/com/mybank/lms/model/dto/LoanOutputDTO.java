package com.mybank.lms.model.dto;

import com.fasterxml.jackson.annotation.JsonFormat;
import lombok.Data;
import lombok.NoArgsConstructor;
import lombok.AllArgsConstructor;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.List;
import java.util.UUID;

@Data
@NoArgsConstructor
@AllArgsConstructor
public class LoanOutputDTO {
    
    private UUID loanId;
    private String productType;
    private BigDecimal principal;
    private BigDecimal annualRate;
    private String rateType;
    private Integer months;
    private String status;
    
    @JsonFormat(pattern = "yyyy-MM-dd")
    private LocalDate loanIssueDate;
    
    @JsonFormat(pattern = "yyyy-MM-dd")
    private LocalDate startDate;
    
    private BigDecimal initialEmi;
    private BigDecimal apr;
    private BigDecimal totalAmountPayable;
    private BigDecimal totalInterest;
    private String benchmarkName;
    private BigDecimal spread;
    
    private List<DisbursementPhaseDTO> disbursementPhases;
    private List<LoanChargeDTO> charges;
    private RepaymentSnapshotDTO latestSnapshot;
    private RepaymentScheduleDTO repaymentSchedule;
    
    @JsonFormat(pattern = "yyyy-MM-dd HH:mm:ss")
    private LocalDateTime createdAt;
    
    @JsonFormat(pattern = "yyyy-MM-dd HH:mm:ss")
    private LocalDateTime updatedAt;
    
    @Data
    @NoArgsConstructor
    @AllArgsConstructor
    public static class DisbursementPhaseDTO {
        @JsonFormat(pattern = "yyyy-MM-dd")
        private LocalDate disbursementDate;
        private BigDecimal amount;
        private BigDecimal cumulativeDisbursed;
        private String description;
        private Integer sequence;
    }
    
    @Data
    @NoArgsConstructor
    @AllArgsConstructor
    public static class LoanChargeDTO {
        private String chargeType;
        private String payableTo;
        private Boolean isRecurring;
        private BigDecimal amount;
        
        @JsonFormat(pattern = "yyyy-MM-dd HH:mm:ss")
        private LocalDateTime createdAt;
    }
    
    @Data
    @NoArgsConstructor
    @AllArgsConstructor
    public static class RepaymentSnapshotDTO {
        private UUID snapshotId;
        
        @JsonFormat(pattern = "yyyy-MM-dd")
        private LocalDate snapshotDate;
        
        private BigDecimal principalBalance;
        private Integer monthsRemaining;
        private BigDecimal annualRate;
        private String rateType;
        private BigDecimal apr;
        private String memo;
        
        @JsonFormat(pattern = "yyyy-MM-dd HH:mm:ss")
        private LocalDateTime createdAt;
    }
}
