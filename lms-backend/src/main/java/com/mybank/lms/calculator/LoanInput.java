package com.mybank.lms.calculator;

import java.time.LocalDate;
import java.util.List;
import java.util.ArrayList;

/**
 * Class to store all loan input parameters
 */
public class LoanInput {
    private double principal;
    private double annualRate;
    private int months;
    private int moratoriumMonths;
    private MoratoriumType moratoriumType;
    private double partialPaymentEMIDuringMoratorium;
    private LocalDate startDate;
    private LocalDate loanIssueDate;
    private FloatingStrategy strategy;
    private CompoundingFrequency compoundingFrequency;
    private List<MoratoriumPeriod> moratoriumPeriods;
    private List<DisbursementPhase> disbursementPhases;
    
    // Enums
    public enum MoratoriumType { FULL, INTEREST_ONLY, PARTIAL }
    public enum FloatingStrategy { EMI_CONSTANT, TENURE_CONSTANT }
    public enum CompoundingFrequency { DAILY, MONTHLY }
    
    // Inner class for disbursement phases
    public static class DisbursementPhase {
        private LocalDate disbursementDate;
        private double amount;
        private String description;
        
        // Default constructor for JSON deserialization
        public DisbursementPhase() {}
        
        public DisbursementPhase(LocalDate disbursementDate, double amount, String description) {
            this.disbursementDate = disbursementDate;
            this.amount = amount;
            this.description = description;
        }
        
        // Getters and Setters
        public LocalDate getDisbursementDate() { return disbursementDate; }
        public void setDisbursementDate(LocalDate disbursementDate) { this.disbursementDate = disbursementDate; }
        
        public double getAmount() { return amount; }
        public void setAmount(double amount) { this.amount = amount; }
        
        public String getDescription() { return description; }
        public void setDescription(String description) { this.description = description; }
    }
    
    // Inner class for flexible moratorium periods
    public static class MoratoriumPeriod {
        private int startMonth;
        private int endMonth;
        private MoratoriumType type;
        private double partialPaymentEMI;
        
        // Default constructor for JSON deserialization
        public MoratoriumPeriod() {}
        
        public MoratoriumPeriod(int startMonth, int endMonth, MoratoriumType type, double partialPaymentEMI) {
            this.startMonth = startMonth;
            this.endMonth = endMonth;
            this.type = type;
            this.partialPaymentEMI = partialPaymentEMI;
        }
        
        // Getters and Setters
        public int getStartMonth() { return startMonth; }
        public void setStartMonth(int startMonth) { this.startMonth = startMonth; }
        
        public int getEndMonth() { return endMonth; }
        public void setEndMonth(int endMonth) { this.endMonth = endMonth; }
        
        public MoratoriumType getType() { return type; }
        public void setType(MoratoriumType type) { this.type = type; }
        
        public double getPartialPaymentEMI() { return partialPaymentEMI; }
        public void setPartialPaymentEMI(double partialPaymentEMI) { this.partialPaymentEMI = partialPaymentEMI; }
        
        public boolean isInMoratorium(int month) {
            return month >= startMonth && month <= endMonth;
        }
    }
    
    
    // Constructor
    public LoanInput() {
        this.startDate = LocalDate.now();
        this.loanIssueDate = LocalDate.now();
        this.moratoriumType = MoratoriumType.FULL;
        this.strategy = FloatingStrategy.EMI_CONSTANT;
        this.compoundingFrequency = CompoundingFrequency.DAILY;
        this.moratoriumPeriods = new ArrayList<>();
        this.disbursementPhases = new ArrayList<>();
    }
    
    // Getters and Setters
    public double getPrincipal() { return principal; }
    public void setPrincipal(double principal) { this.principal = principal; }
    
    public double getAnnualRate() { return annualRate; }
    public void setAnnualRate(double annualRate) { this.annualRate = annualRate; }
    
    public int getMonths() { return months; }
    public void setMonths(int months) { this.months = months; }
    
    public int getMoratoriumMonths() { return moratoriumMonths; }
    public void setMoratoriumMonths(int moratoriumMonths) { this.moratoriumMonths = moratoriumMonths; }
    
    public MoratoriumType getMoratoriumType() { return moratoriumType; }
    public void setMoratoriumType(MoratoriumType moratoriumType) { this.moratoriumType = moratoriumType; }
    
    public double getPartialPaymentEMIDuringMoratorium() { return partialPaymentEMIDuringMoratorium; }
    public void setPartialPaymentEMIDuringMoratorium(double partialPaymentEMIDuringMoratorium) { this.partialPaymentEMIDuringMoratorium = partialPaymentEMIDuringMoratorium; }
    
    public LocalDate getStartDate() { return startDate; }
    public void setStartDate(LocalDate startDate) { this.startDate = startDate; }
    
    public LocalDate getLoanIssueDate() { return loanIssueDate; }
    public void setLoanIssueDate(LocalDate loanIssueDate) { this.loanIssueDate = loanIssueDate; }
    
    public FloatingStrategy getStrategy() { return strategy; }
    public void setStrategy(FloatingStrategy strategy) { this.strategy = strategy; }
    
    public CompoundingFrequency getCompoundingFrequency() { return compoundingFrequency; }
    public void setCompoundingFrequency(CompoundingFrequency compoundingFrequency) { this.compoundingFrequency = compoundingFrequency; }
    
    public List<MoratoriumPeriod> getMoratoriumPeriods() { return moratoriumPeriods; }
    public void setMoratoriumPeriods(List<MoratoriumPeriod> moratoriumPeriods) { this.moratoriumPeriods = moratoriumPeriods; }
    
    public List<DisbursementPhase> getDisbursementPhases() { return disbursementPhases; }
    public void setDisbursementPhases(List<DisbursementPhase> disbursementPhases) { this.disbursementPhases = disbursementPhases; }
    
    // Helper methods
    public boolean isInMoratorium(int month) {
        return moratoriumPeriods.stream().anyMatch(period -> period.isInMoratorium(month));
    }
    
    public MoratoriumPeriod getMoratoriumPeriodForMonth(int month) {
        return moratoriumPeriods.stream()
            .filter(period -> period.isInMoratorium(month))
            .findFirst()
            .orElse(null);
    }
    
    public void addMoratoriumPeriod(int startMonth, int endMonth, MoratoriumType type, double partialPaymentEMI) {
        moratoriumPeriods.add(new MoratoriumPeriod(startMonth, endMonth, type, partialPaymentEMI));
    }
    
    public void addDisbursementPhase(LocalDate disbursementDate, double amount, String description) {
        disbursementPhases.add(new DisbursementPhase(disbursementDate, amount, description));
    }
    
    public boolean hasPhasesDisbursement() {
        return disbursementPhases != null && !disbursementPhases.isEmpty();
    }
    
    @Override
    public String toString() {
        return String.format("LoanInput{principal=%.2f, rate=%.2f%%, months=%d, moratorium=%s(%d), strategy=%s, compounding=%s, flexibleMoratoriums=%d, disbursementPhases=%d}", 
            principal, annualRate, months, moratoriumType, moratoriumMonths, strategy, compoundingFrequency, moratoriumPeriods.size(), 
            disbursementPhases != null ? disbursementPhases.size() : 0);
    }
}
