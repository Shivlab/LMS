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
public class RepaymentScheduleDTO {
    
    private UUID snapshotId;
    
    @JsonFormat(pattern = "yyyy-MM-dd")
    private LocalDate snapshotDate;
    
    private BigDecimal principalBalance;
    private Integer monthsRemaining;
    private BigDecimal annualRate;
    private String rateType;
    private BigDecimal apr;
    private String memo;
    
    private List<RepaymentRowDTO> repaymentRows;
    private List<RepaymentRowDTO> installments; // Alias for frontend compatibility
    
    @JsonFormat(pattern = "yyyy-MM-dd HH:mm:ss")
    private LocalDateTime createdAt;
    
    @Data
    @NoArgsConstructor
    @AllArgsConstructor
    public static class RepaymentRowDTO {
        private Integer monthNumber;
        private Integer installmentNumber; // Alias for frontend
        
        @JsonFormat(pattern = "yyyy-MM-dd")
        private LocalDate paymentDate;
        
        @JsonFormat(pattern = "yyyy-MM-dd")
        private LocalDate dueDate; // Alias for frontend
        
        private BigDecimal emi;
        private BigDecimal installmentAmount; // Alias for frontend
        private BigDecimal principalPaid;
        private BigDecimal principal; // Alias for frontend
        private BigDecimal principalComponent; // Alias for frontend
        private BigDecimal interestPaid;
        private BigDecimal interest; // Alias for frontend
        private BigDecimal interestComponent; // Alias for frontend
        private BigDecimal remainingBalance;
        private BigDecimal principalOutstanding; // Alias for frontend
        private String paymentType;
        
        @JsonFormat(pattern = "yyyy-MM-dd HH:mm:ss")
        private LocalDateTime createdAt;
    }
}
