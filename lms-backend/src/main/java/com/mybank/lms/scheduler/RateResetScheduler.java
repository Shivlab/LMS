package com.mybank.lms.scheduler;

import com.mybank.lms.service.BenchmarkService;
import com.mybank.lms.service.LoanService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;

@Component
@RequiredArgsConstructor
@Slf4j
public class RateResetScheduler {
    
    private final BenchmarkService benchmarkService;
    private final LoanService loanService;
    
    /**
     * Scheduled job that runs daily at 2 AM to check for rate resets
     * Cron expression: 0 0 2 * * ? (second, minute, hour, day, month, day-of-week)
     */
    @Scheduled(cron = "0 0 2 * * ?")
    public void processFloatingRateResets() {
        log.info("Starting scheduled floating rate reset process");
        
        try {
            // Get all active floating loans
            var activeFloatingLoans = loanService.getActiveFloatingLoans();
            log.info("Found {} active floating loans", activeFloatingLoans.size());
            
            // Process each loan for potential rate reset
            for (var loan : activeFloatingLoans) {
                try {
                    processLoanForRateReset(loan);
                } catch (Exception e) {
                    log.error("Error processing rate reset for loan: {}", loan.getId(), e);
                }
            }
            
            log.info("Completed scheduled floating rate reset process");
            
        } catch (Exception e) {
            log.error("Error in scheduled rate reset process", e);
        }
    }
    
    private void processLoanForRateReset(com.mybank.lms.model.entity.LoanEntity loan) {
        // Check if loan has a benchmark and is eligible for reset
        if (loan.getBenchmarkName() == null || loan.getResetPeriodicityMonths() == null) {
            return;
        }
        
        // Get latest benchmark rate
        var latestBenchmark = benchmarkService.getLatestBenchmark(loan.getBenchmarkName());
        if (latestBenchmark == null) {
            return;
        }
        
        // Calculate new rate with spread
        var newRate = latestBenchmark.getBenchmarkRate();
        if (loan.getSpread() != null) {
            newRate = newRate.add(loan.getSpread());
        }
        
        // Check if rate has changed
        if (newRate.compareTo(loan.getAnnualRate()) != 0) {
            log.info("Rate reset required for loan: {} from {}% to {}%", 
                loan.getId(), loan.getAnnualRate(), newRate);
            
            loanService.applyBenchmarkToLoan(loan.getId(), loan.getBenchmarkName(), latestBenchmark.getBenchmarkRate());
        }
    }
}
