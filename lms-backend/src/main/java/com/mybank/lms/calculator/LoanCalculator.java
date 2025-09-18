package com.mybank.lms.calculator;

import java.time.LocalDate;
import java.time.YearMonth;
import java.time.temporal.ChronoUnit;

/**
 * Pure calculation engine for loan calculations
 */
public class LoanCalculator {
    
    /**
     * Calculate loan with flexible moratorium and compounding options
     */
    public static LoanOutput calculateLoan(LoanInput input) {
        LoanOutput output = new LoanOutput();
        
        if (input.getCompoundingFrequency() == LoanInput.CompoundingFrequency.MONTHLY) {
            calculateLoanWithMonthlyCompounding(input, output);
        } else {
            double emi = goalSeekMonthlyEMI(input);
            output.setInitialEMI(emi);
            simulateLoanWithDailyCompounding(input, output, emi);
        }
        
        // Calculate BPI if applicable
        LoanOutput.BrokenPeriodInterest bpi = calculateBPI(input);
        if (bpi != null) {
            output.setBrokenPeriodInterest(bpi);
            
            if (bpi.isAddedToFirstEMI() && !output.getPaymentSchedule().isEmpty()) {
                LoanOutput.MonthlyPayment firstPayment = output.getPaymentSchedule().get(0);
                double newEMI = firstPayment.getEmi() + bpi.getInterestAmount();
                
                LoanOutput.MonthlyPayment updatedFirstPayment = new LoanOutput.MonthlyPayment(
                    firstPayment.getMonthNumber(),
                    newEMI,
                    firstPayment.getPrincipalPaid(),
                    firstPayment.getInterestPaid() + bpi.getInterestAmount(),
                    firstPayment.getRemainingBalance(),
                    firstPayment.getCurrentRate(),
                    firstPayment.getPaymentDate(),
                    "NORMAL_WITH_BPI"
                );
                
                output.getPaymentSchedule().set(0, updatedFirstPayment);
            }
        }
        
        output.calculateTotals();
        return output;
    }
    
    private static void calculateLoanWithMonthlyCompounding(LoanInput input, LoanOutput output) {
        double principal = input.getPrincipal();
        double monthlyRate = input.getAnnualRate() / 12 / 100.0;
        int totalMonths = input.getMonths();
        
        double standardEMI = (principal * monthlyRate * Math.pow(1 + monthlyRate, totalMonths)) /
                            (Math.pow(1 + monthlyRate, totalMonths) - 1);
        
        output.setInitialEMI(standardEMI);
        generateRepaymentSchedule(input, output, standardEMI, monthlyRate);
    }
    
    private static void generateRepaymentSchedule(LoanInput input, LoanOutput output, double standardEMI, double monthlyRate) {
        double balance = input.getPrincipal();
        LocalDate date = input.getStartDate();
        int monthCount = 0;
        int maxMonths = 1200; // Safety limit: 100 years max

        for (int i = 0; balance >= 1 && monthCount < maxMonths; i++) {
            monthCount++;

            double monthlyInterest = balance * monthlyRate;

            boolean isInMoratorium = false;
            LoanInput.MoratoriumPeriod moratoriumPeriod = null;

            if (input.isInMoratorium(i + 1)) {
                isInMoratorium = true;
                moratoriumPeriod = input.getMoratoriumPeriodForMonth(i + 1);
            } else if (i < input.getMoratoriumMonths()) {
                isInMoratorium = true;
            }

            String paymentType = "NORMAL";
            double actualEMI = standardEMI;
            double principalPaid = 0;
            double interestPaid = monthlyInterest;

            if (isInMoratorium) {
                LoanInput.MoratoriumType moratoriumType = moratoriumPeriod != null ?
                    moratoriumPeriod.getType() : input.getMoratoriumType();
                double partialPayment = moratoriumPeriod != null ?
                    moratoriumPeriod.getPartialPaymentEMI() : input.getPartialPaymentEMIDuringMoratorium();

                switch (moratoriumType) {
                    case INTEREST_ONLY:
                        paymentType = "MORATORIUM_INTEREST";
                        actualEMI = monthlyInterest;
                        principalPaid = 0;
                        break;
                    case PARTIAL:
                        paymentType = "MORATORIUM_PARTIAL";
                        actualEMI = partialPayment;
                        principalPaid = Math.max(0, partialPayment - monthlyInterest);
                        balance -= (partialPayment - monthlyInterest);
                        break;
                    case FULL:
                        paymentType = "MORATORIUM_FULL";
                        actualEMI = 0;
                        principalPaid = 0;
                        interestPaid = 0;
                        balance += monthlyInterest;
                        break;
                }
            } else {
                balance -= standardEMI;
                principalPaid = standardEMI - monthlyInterest;

                // Handle final EMI - adjust if remaining balance is less than calculated principal
                // if (balance < principalPaid) {
                //     principalPaid = balance;
                //     actualEMI = principalPaid + monthlyInterest;
                // }

                // balance -= principalPaid;

                // Ensure balance doesn't go negative
                if (balance < 1) {
                    balance = 0;
                }
            }

            output.addPayment(i + 1, actualEMI, principalPaid, interestPaid,
                            balance, input.getAnnualRate(), date, paymentType);

            date = date.plusMonths(1);

            // Emergency break if too many iterations
            if (monthCount >= maxMonths) {
                break;
            }
        }

        output.setActualTenure(monthCount);
    }
    
    private static void simulateLoanWithDailyCompounding(LoanInput input, LoanOutput output, double emi) {
        double balance = input.getPrincipal();
        LocalDate date = input.getStartDate();
        double currentRate = input.getAnnualRate();
        int monthCount = 0;
        int maxMonths = 1200; // Safety limit: 100 years max

        for (int i = 0; balance >= 1 && monthCount < maxMonths; i++) {
            monthCount++;

            YearMonth currentMonth = YearMonth.from(date);
            int daysInMonth = currentMonth.lengthOfMonth();
            int yearLength = date.isLeapYear() ? 366 : 365;
            double dailyRate = currentRate / yearLength / 100.0;

            double monthlyInterest = 0;
            // Optimize daily compounding calculation to prevent memory issues
            if (daysInMonth > 31) daysInMonth = 31; // Safety limit

            for (int d = 0; d < daysInMonth; d++) {
                monthlyInterest += balance * dailyRate;
                balance += balance * dailyRate;

                // Prevent infinite growth due to compounding
                if (monthlyInterest > balance * 10) {
                    break; // Emergency break if interest becomes too large
                }
            }

            boolean isInMoratorium = false;
            LoanInput.MoratoriumPeriod moratoriumPeriod = null;

            if (input.isInMoratorium(i + 1)) {
                isInMoratorium = true;
                moratoriumPeriod = input.getMoratoriumPeriodForMonth(i + 1);
            } else if (i < input.getMoratoriumMonths()) {
                isInMoratorium = true;
            }

            String paymentType = "NORMAL";
            double actualEMI = emi;
            double principalPaid = 0;
            double interestPaid = monthlyInterest;

            if (isInMoratorium) {
                LoanInput.MoratoriumType moratoriumType = moratoriumPeriod != null ?
                    moratoriumPeriod.getType() : input.getMoratoriumType();
                double partialPayment = moratoriumPeriod != null ?
                    moratoriumPeriod.getPartialPaymentEMI() : input.getPartialPaymentEMIDuringMoratorium();

                switch (moratoriumType) {
                    case INTEREST_ONLY:
                        paymentType = "MORATORIUM_INTEREST";
                        actualEMI = monthlyInterest;
                        balance -= monthlyInterest;
                        principalPaid = 0;
                        break;
                    case PARTIAL:
                        paymentType = "MORATORIUM_PARTIAL";
                        actualEMI = partialPayment;
                        balance -= partialPayment;
                        principalPaid = partialPayment - monthlyInterest;
                        break;
                    case FULL:
                        paymentType = "MORATORIUM_FULL";
                        actualEMI = 0;
                        principalPaid = 0;
                        interestPaid = 0;
                        break;
                }
            } else {
                balance -= emi;
                principalPaid = emi - monthlyInterest;

                // Handle final EMI - adjust if remaining balance is less than calculated principal
                // if (balance < principalPaid) {
                //     principalPaid = balance;
                //     actualEMI = principalPaid + monthlyInterest;
                // }

                // balance -= principalPaid;

                // Ensure balance doesn't go negative
                if (balance < 1) {
                    balance = 0;
                }
            }

            output.addPayment(i + 1, actualEMI, principalPaid, interestPaid,
                            balance, currentRate, date, paymentType);

            date = date.plusMonths(1);

            // Emergency break if too many iterations
            if (monthCount >= maxMonths) {
                break;
            }
        }

        if (input.getStrategy() == LoanInput.FloatingStrategy.EMI_CONSTANT && balance > 0.01) {
            while (balance > 0.01 && monthCount < maxMonths) {
                monthCount++;

                YearMonth currentMonth = YearMonth.from(date);
                int daysInMonth = currentMonth.lengthOfMonth();
                int yearLength = date.isLeapYear() ? 366 : 365;
                double dailyRate = currentRate / yearLength / 100.0;

                double monthlyInterest = 0;
                for (int d = 0; d < Math.min(daysInMonth, 31); d++) {
                    monthlyInterest += balance * dailyRate;
                    balance += balance * dailyRate;
                }

                balance -= emi;
                double principalPaid = emi - monthlyInterest;

                output.addPayment(monthCount, emi, principalPaid, monthlyInterest,
                                balance, currentRate, date, "EXTENDED");

                date = date.plusMonths(1);
            }
        }

        output.setActualTenure(monthCount);
    }
    
    public static double goalSeekMonthlyEMI(LoanInput input) {
        return goalSeekEMIForBalance(input);
    }
    
    private static double goalSeekEMIForBalance(LoanInput input) {
        double monthlyRate = input.getAnnualRate() / 12 / 100.0;
        double estimatedEMI = (input.getPrincipal() * monthlyRate * Math.pow(1 + monthlyRate, input.getMonths())) /
                              (Math.pow(1 + monthlyRate, input.getMonths()) - 1);
        
        double low = estimatedEMI * 0.8;
        double high = estimatedEMI * 1.2;
        double tolerance = 0.01;
        
        while ((high - low) > tolerance) {
            double mid = (low + high) / 2;
            double balance = calculateFinalBalance(input, mid);
            if (balance > 0) {
                low = mid;
            } else {
                high = mid;
            }
        }
        
        return (low + high) / 2;
    }
    
    private static double calculateFinalBalance(LoanInput input, double emi) {
        double balance = input.getPrincipal();
        LocalDate date = input.getStartDate();
        
        for (int i = 0; i < input.getMonths(); i++) {
            YearMonth currentMonth = YearMonth.from(date);
            int daysInMonth = currentMonth.lengthOfMonth();
            int yearLength = date.isLeapYear() ? 366 : 365;
            double dailyRate = input.getAnnualRate() / yearLength / 100.0;
            
            for (int d = 0; d < daysInMonth; d++) {
                balance += balance * dailyRate;
            }
            
            boolean isInMoratorium = false;
            LoanInput.MoratoriumPeriod moratoriumPeriod = null;
            
            if (input.isInMoratorium(i + 1)) {
                isInMoratorium = true;
                moratoriumPeriod = input.getMoratoriumPeriodForMonth(i + 1);
            } else if (i < input.getMoratoriumMonths()) {
                isInMoratorium = true;
            }
            
            if (isInMoratorium) {
                LoanInput.MoratoriumType moratoriumType = moratoriumPeriod != null ? 
                    moratoriumPeriod.getType() : input.getMoratoriumType();
                double partialPayment = moratoriumPeriod != null ? 
                    moratoriumPeriod.getPartialPaymentEMI() : input.getPartialPaymentEMIDuringMoratorium();
                
                if (moratoriumType == LoanInput.MoratoriumType.INTEREST_ONLY) {
                    double monthlyInterest = balance * dailyRate * daysInMonth;
                    balance -= monthlyInterest;
                } else if (moratoriumType == LoanInput.MoratoriumType.PARTIAL) {
                    balance -= partialPayment;
                }
            } else {
                balance -= emi;
            }
            
            date = date.plusMonths(1);
        }
        return balance;
    }
    
    public static LoanOutput.BrokenPeriodInterest calculateBPI(LoanInput input) {
        LocalDate loanIssueDate = input.getLoanIssueDate();
        LocalDate emiStartDate = input.getStartDate();
        
        if (loanIssueDate == null || emiStartDate == null) {
            return null;
        }        
        
        long daysDifference = ChronoUnit.DAYS.between(loanIssueDate, emiStartDate);
        
        if (daysDifference <= 0) {
            return null;
        }
        
        double principal = input.getPrincipal();
        double dailyRate = input.getAnnualRate() / 365 / 100.0;
        double bpiAmount = principal * dailyRate * daysDifference;
        
        boolean addToFirstEMI = daysDifference < 15;
        
        String description = String.format(
            "BPI for %d days from %s to %s. %s",
            daysDifference,
            loanIssueDate,
            emiStartDate,
            addToFirstEMI ? "Added to first EMI" : "Charged as separate EMI in issue month"
        );
        
        return new LoanOutput.BrokenPeriodInterest(
            loanIssueDate,
            emiStartDate,
            (int) daysDifference,
            bpiAmount,
            addToFirstEMI,
            description
        );
    }
}
