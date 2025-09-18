package com.mybank.lms.calculator;

import java.time.LocalDate;
import java.time.YearMonth;
import java.util.Comparator;

/**
 * Home Loan Calculator with phased disbursement and pre-EMI payments
 */
public class HomeLoan {
    
    public static LoanOutput calculateHomeLoan(LoanInput input) {
        LoanOutput output = new LoanOutput();
        
        if (!input.hasPhasesDisbursement()) {
            return calculateLoan(input);
        }
        
        input.getDisbursementPhases().sort(Comparator.comparing(LoanInput.DisbursementPhase::getDisbursementDate));
        
        generateDisbursementScheduleAndPreEMI(input, output);
        
        LocalDate fullDisbursementDate = getFullDisbursementDate(input);
        int preEmiMonthCount = output.getPreEmiPayments().size();
        generatePostDisbursementEMISchedule(input, output, fullDisbursementDate, preEmiMonthCount);
        
        output.calculateTotals();
        
        return output;
    }
    
    public static LoanOutput calculateLoan(LoanInput input) {
        return LoanCalculator.calculateLoan(input);
    }
    
    private static void generateDisbursementScheduleAndPreEMI(LoanInput input, LoanOutput output) {
        double cumulativeDisbursed = 0.0;

        for (LoanInput.DisbursementPhase phase : input.getDisbursementPhases()) {
            cumulativeDisbursed += phase.getAmount();
            output.getDisbursementSchedule().add(new LoanOutput.DisbursementEntry(
                phase.getDisbursementDate(),
                phase.getAmount(),
                cumulativeDisbursed,
                phase.getDescription()
            ));
        }

        double disbursedBalance = 0.0;
        int globalMonthNumber = 1;
        int maxPreEmiMonths = 1200; // Safety limit: 100 years max
        int preEmiCount = 0;

        for (int i = 0; i < input.getDisbursementPhases().size(); i++) {
            LoanInput.DisbursementPhase phase = input.getDisbursementPhases().get(i);

            disbursedBalance += phase.getAmount();

            LocalDate nextDate;
            if (i < input.getDisbursementPhases().size() - 1) {
                nextDate = input.getDisbursementPhases().get(i + 1).getDisbursementDate();
            } else {
                // For the last phase, pre-EMI should continue until regular EMI starts
                // Use the full disbursement date (after final disbursement)
                nextDate = getFullDisbursementDate(input);
            }

            // Start pre-EMI from the disbursement month itself, not the next month
            LocalDate preEmiDate = phase.getDisbursementDate();

            // Safety check to prevent infinite loops
            while (preEmiDate.isBefore(nextDate) && preEmiCount < maxPreEmiMonths && disbursedBalance > 0.01) {
                double preEmiAmount = calculatePreEMIForMonth(disbursedBalance, input.getAnnualRate(),
                    preEmiDate, input.getCompoundingFrequency());

                int daysInPeriod = getDaysInMonth(preEmiDate);

                output.getPreEmiPayments().add(new LoanOutput.PreEmiPayment(
                    preEmiDate, preEmiAmount, disbursedBalance, input.getAnnualRate(), daysInPeriod
                ));

                output.addPayment(
                    globalMonthNumber,
                    preEmiAmount,
                    0.0,
                    preEmiAmount,
                    disbursedBalance,
                    input.getAnnualRate(),
                    preEmiDate,
                    "PRE_EMI"
                );

                // Move to next month for pre-EMI calculation
                preEmiDate = preEmiDate.plusMonths(1).withDayOfMonth(phase.getDisbursementDate().getDayOfMonth());
                globalMonthNumber++;
                preEmiCount++;

                // Emergency break if too many iterations
                if (preEmiCount >= maxPreEmiMonths) {
                    break;
                }
            }

            // Emergency break if too many iterations overall
            if (preEmiCount >= maxPreEmiMonths) {
                break;
            }
        }
    }
    
    private static double calculatePreEMIForMonth(double disbursedBalance, double annualRate, 
                                                LocalDate paymentDate, LoanInput.CompoundingFrequency compounding) {
        if (compounding == LoanInput.CompoundingFrequency.MONTHLY) {
            double monthlyRate = annualRate / 12 / 100.0;
            return disbursedBalance * monthlyRate;
        } else {
            int daysInMonth = getDaysInMonth(paymentDate);
            int yearLength = paymentDate.isLeapYear() ? 366 : 365;
            double dailyRate = annualRate / yearLength / 100.0;
            
            double monthlyInterest = 0;
            double balance = disbursedBalance;
            for (int d = 0; d < daysInMonth; d++) {
                double dailyInterest = balance * dailyRate;
                monthlyInterest += dailyInterest;
                balance += dailyInterest;
            }
            return monthlyInterest;
        }
    }
    
    private static void generatePostDisbursementEMISchedule(LoanInput input, LoanOutput output, LocalDate startDate, int preEmiMonthCount) {
        LoanInput modifiedInput = createModifiedInputForEMICalculation(input, startDate);
        
        LoanOutput regularLoanOutput = LoanCalculator.calculateLoan(modifiedInput);
        
        output.setInitialEMI(regularLoanOutput.getInitialEMI());
        
        // Get the current maximum month number from existing payments (pre-EMI payments)
        int currentMaxMonth = output.getPaymentSchedule().stream()
            .mapToInt(LoanOutput.MonthlyPayment::getMonthNumber)
            .max()
            .orElse(0);
        
        for (LoanOutput.MonthlyPayment payment : regularLoanOutput.getPaymentSchedule()) {
            output.addPayment(
                currentMaxMonth + payment.getMonthNumber(),
                payment.getEmi(),
                payment.getPrincipalPaid(),
                payment.getInterestPaid(),
                payment.getRemainingBalance(),
                payment.getCurrentRate(),
                payment.getPaymentDate(),
                payment.getPaymentType()
            );
        }
        
        output.setActualTenure(regularLoanOutput.getActualTenure() + preEmiMonthCount);
    }
    
    private static LoanInput createModifiedInputForEMICalculation(LoanInput originalInput, LocalDate startDate) {
        LoanInput modifiedInput = new LoanInput();
        
        modifiedInput.setPrincipal(originalInput.getPrincipal());
        modifiedInput.setAnnualRate(originalInput.getAnnualRate());
        modifiedInput.setMonths(originalInput.getMonths());
        modifiedInput.setMoratoriumMonths(originalInput.getMoratoriumMonths());
        modifiedInput.setMoratoriumType(originalInput.getMoratoriumType());
        modifiedInput.setPartialPaymentEMIDuringMoratorium(originalInput.getPartialPaymentEMIDuringMoratorium());
        modifiedInput.setStrategy(originalInput.getStrategy());
        modifiedInput.setCompoundingFrequency(originalInput.getCompoundingFrequency());
        modifiedInput.setMoratoriumPeriods(originalInput.getMoratoriumPeriods());
        
        modifiedInput.setStartDate(startDate);
        
        return modifiedInput;
    }
    
    private static LocalDate getFullDisbursementDate(LoanInput input) {
        return input.getDisbursementPhases().stream()
            .max(Comparator.comparing(LoanInput.DisbursementPhase::getDisbursementDate))
            .map(phase -> phase.getDisbursementDate().plusMonths(1).withDayOfMonth(phase.getDisbursementDate().getDayOfMonth()))
            .orElse(input.getStartDate());
    }
    
    private static int getDaysInMonth(LocalDate date) {
        return YearMonth.from(date).lengthOfMonth();
    }
    
    public static boolean validateHomeLoanInput(LoanInput input) {
        if (!input.hasPhasesDisbursement()) {
            return false;
        }
        
        double totalDisbursement = input.getDisbursementPhases().stream()
            .mapToDouble(LoanInput.DisbursementPhase::getAmount)
            .sum();
        
        if (Math.abs(totalDisbursement - input.getPrincipal()) > 0.01) {
            System.err.println("Error: Total disbursement amount (" + totalDisbursement + 
                             ") does not match principal (" + input.getPrincipal() + ")");
            return false;
        }
        
        LocalDate previousDate = null;
        for (LoanInput.DisbursementPhase phase : input.getDisbursementPhases()) {
            if (previousDate != null && phase.getDisbursementDate().isBefore(previousDate)) {
                System.err.println("Error: Disbursement dates must be in chronological order");
                return false;
            }
            previousDate = phase.getDisbursementDate();
        }
        
        return true;
    }
}
