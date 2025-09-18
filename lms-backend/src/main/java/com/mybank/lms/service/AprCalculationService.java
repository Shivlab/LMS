package com.mybank.lms.service;

import com.mybank.lms.calculator.LoanInput;
import com.mybank.lms.calculator.LoanOutput;
import com.mybank.lms.model.dto.LoanInputDTO;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.util.List;

@Service
@Slf4j
public class AprCalculationService {
    
    /**
     * Calculate APR using formula: ((total fees + total interest)/loan amount/number of months)*100 + interest rate
     * APR includes the effect of fees and charges on the effective cost of borrowing plus the base interest rate
     */
    public BigDecimal calculateAPR(LoanInput loanInput, LoanOutput loanOutput, List<LoanInputDTO.LoanChargeDTO> charges) {
        log.debug("Calculating APR for loan with principal: {}", loanInput.getPrincipal());
        
        try {
            double loanAmount = loanInput.getPrincipal();
            int numberOfMonths = loanInput.getMonths();
            
            // Calculate total interest from payment schedule
            double totalInterest = loanOutput.getPaymentSchedule().stream()
                .mapToDouble(payment -> payment.getInterestPaid())
                .sum();
            
            // Calculate total fees
            double totalFees = 0.0;
            if (charges != null) {
                // One-time charges
                double oneTimeFees = charges.stream()
                    .filter(charge -> !charge.getIsRecurring())
                    .mapToDouble(charge -> charge.getAmount().doubleValue())
                    .sum();
                
                // Recurring charges (multiply by number of months)
                double recurringFees = charges.stream()
                    .filter(charge -> charge.getIsRecurring())
                    .mapToDouble(charge -> charge.getAmount().doubleValue())
                    .sum() * numberOfMonths;
                
                totalFees = oneTimeFees + recurringFees;
            }
            
            // APR formula: ((total fees + total interest)/loan amount/number of months)*100 + interest rate
            double feeAndInterestRate = ((totalFees + totalInterest) / loanAmount / numberOfMonths) * 100;
            double baseInterestRate = loanInput.getAnnualRate();
            double apr = feeAndInterestRate + baseInterestRate;
            
            BigDecimal aprPercentage = BigDecimal.valueOf(apr)
                .setScale(4, RoundingMode.HALF_UP);
            
            log.info("Calculated APR: {}% (Total Interest: {}, Total Fees: {}, Loan Amount: {}, Months: {}, Fee Rate: {})", 
                aprPercentage, totalInterest, totalFees, loanAmount, numberOfMonths, feeAndInterestRate);
            
            // Add validation to prevent overflow
            if (aprPercentage.compareTo(BigDecimal.valueOf(9999.9999)) > 0) {
                log.warn("APR calculation resulted in overflow value: {}. Using fallback to nominal rate.", aprPercentage);
                return BigDecimal.valueOf(loanInput.getAnnualRate()).setScale(4, RoundingMode.HALF_UP);
            }
            
            return aprPercentage;
            
        } catch (Exception e) {
            log.error("Error calculating APR", e);
            // Fallback to nominal rate if APR calculation fails
            return BigDecimal.valueOf(loanInput.getAnnualRate())
                .setScale(4, RoundingMode.HALF_UP);
        }
    }
}
