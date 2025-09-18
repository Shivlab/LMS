package com.mybank.lms.calculator;

import java.time.LocalDate;
import java.util.List;
import java.util.ArrayList;

/**
 * Class to store loan calculation results and repayment schedule
 */
public class LoanOutput {
    private double initialEMI;
    private List<MonthlyPayment> paymentSchedule;
    private double totalInterestPaid;
    private double totalAmountPaid;
    private int actualTenure;
    private String summary;
    private List<DisbursementEntry> disbursementSchedule;
    private List<PreEmiPayment> preEmiPayments;
    private BrokenPeriodInterest brokenPeriodInterest;
    
    // Inner class to represent broken period interest (BPI)
    public static class BrokenPeriodInterest {
        private LocalDate loanIssueDate;
        private LocalDate emiStartDate;
        private int daysDifference;
        private double interestAmount;
        private boolean addedToFirstEMI;
        private String description;
        
        public BrokenPeriodInterest() {}
        
        public BrokenPeriodInterest(LocalDate loanIssueDate, LocalDate emiStartDate, 
                                   int daysDifference, double interestAmount, 
                                   boolean addedToFirstEMI, String description) {
            this.loanIssueDate = loanIssueDate;
            this.emiStartDate = emiStartDate;
            this.daysDifference = daysDifference;
            this.interestAmount = interestAmount;
            this.addedToFirstEMI = addedToFirstEMI;
            this.description = description;
        }
        
        // Getters and Setters
        public LocalDate getLoanIssueDate() { return loanIssueDate; }
        public void setLoanIssueDate(LocalDate loanIssueDate) { this.loanIssueDate = loanIssueDate; }
        
        public LocalDate getEmiStartDate() { return emiStartDate; }
        public void setEmiStartDate(LocalDate emiStartDate) { this.emiStartDate = emiStartDate; }
        
        public int getDaysDifference() { return daysDifference; }
        public void setDaysDifference(int daysDifference) { this.daysDifference = daysDifference; }
        
        public double getInterestAmount() { return interestAmount; }
        public void setInterestAmount(double interestAmount) { this.interestAmount = interestAmount; }
        
        public boolean isAddedToFirstEMI() { return addedToFirstEMI; }
        public void setAddedToFirstEMI(boolean addedToFirstEMI) { this.addedToFirstEMI = addedToFirstEMI; }
        
        public String getDescription() { return description; }
        public void setDescription(String description) { this.description = description; }
    }
    
    // Inner class to represent disbursement entries
    public static class DisbursementEntry {
        private LocalDate disbursementDate;
        private double amount;
        private double cumulativeDisbursed;
        private String description;
        
        public DisbursementEntry() {}
        
        public DisbursementEntry(LocalDate disbursementDate, double amount, double cumulativeDisbursed, String description) {
            this.disbursementDate = disbursementDate;
            this.amount = amount;
            this.cumulativeDisbursed = cumulativeDisbursed;
            this.description = description;
        }
        
        // Getters and Setters
        public LocalDate getDisbursementDate() { return disbursementDate; }
        public void setDisbursementDate(LocalDate disbursementDate) { this.disbursementDate = disbursementDate; }
        
        public double getAmount() { return amount; }
        public void setAmount(double amount) { this.amount = amount; }
        
        public double getCumulativeDisbursed() { return cumulativeDisbursed; }
        public void setCumulativeDisbursed(double cumulativeDisbursed) { this.cumulativeDisbursed = cumulativeDisbursed; }
        
        public String getDescription() { return description; }
        public void setDescription(String description) { this.description = description; }
    }
    
    // Inner class to represent pre-EMI payments
    public static class PreEmiPayment {
        private LocalDate paymentDate;
        private double interestAmount;
        private double disbursedBalance;
        private double currentRate;
        private int daysInPeriod;
        
        public PreEmiPayment() {}
        
        public PreEmiPayment(LocalDate paymentDate, double interestAmount, double disbursedBalance, double currentRate, int daysInPeriod) {
            this.paymentDate = paymentDate;
            this.interestAmount = interestAmount;
            this.disbursedBalance = disbursedBalance;
            this.currentRate = currentRate;
            this.daysInPeriod = daysInPeriod;
        }
        
        // Getters and Setters
        public LocalDate getPaymentDate() { return paymentDate; }
        public void setPaymentDate(LocalDate paymentDate) { this.paymentDate = paymentDate; }
        
        public double getInterestAmount() { return interestAmount; }
        public void setInterestAmount(double interestAmount) { this.interestAmount = interestAmount; }
        
        public double getDisbursedBalance() { return disbursedBalance; }
        public void setDisbursedBalance(double disbursedBalance) { this.disbursedBalance = disbursedBalance; }
        
        public double getCurrentRate() { return currentRate; }
        public void setCurrentRate(double currentRate) { this.currentRate = currentRate; }
        
        public int getDaysInPeriod() { return daysInPeriod; }
        public void setDaysInPeriod(int daysInPeriod) { this.daysInPeriod = daysInPeriod; }
    }
    
    // Inner class to represent each monthly payment
    public static class MonthlyPayment {
        private int monthNumber;
        private double emi;
        private double principalPaid;
        private double interestPaid;
        private double remainingBalance;
        private double currentRate;
        private LocalDate paymentDate;
        private String paymentType;
        
        public MonthlyPayment() {}
        
        public MonthlyPayment(int monthNumber, double emi, double principalPaid, 
                            double interestPaid, double remainingBalance, 
                            double currentRate, LocalDate paymentDate, String paymentType) {
            this.monthNumber = monthNumber;
            this.emi = emi;
            this.principalPaid = principalPaid;
            this.interestPaid = interestPaid;
            this.remainingBalance = remainingBalance;
            this.currentRate = currentRate;
            this.paymentDate = paymentDate;
            this.paymentType = paymentType;
        }
        
        // Getters and Setters
        public int getMonthNumber() { return monthNumber; }
        public void setMonthNumber(int monthNumber) { this.monthNumber = monthNumber; }
        
        public double getEmi() { return emi; }
        public void setEmi(double emi) { this.emi = emi; }
        
        public double getPrincipalPaid() { return principalPaid; }
        public void setPrincipalPaid(double principalPaid) { this.principalPaid = principalPaid; }
        
        public double getInterestPaid() { return interestPaid; }
        public void setInterestPaid(double interestPaid) { this.interestPaid = interestPaid; }
        
        public double getRemainingBalance() { return remainingBalance; }
        public void setRemainingBalance(double remainingBalance) { this.remainingBalance = remainingBalance; }
        
        public double getCurrentRate() { return currentRate; }
        public void setCurrentRate(double currentRate) { this.currentRate = currentRate; }
        
        public LocalDate getPaymentDate() { return paymentDate; }
        public void setPaymentDate(LocalDate paymentDate) { this.paymentDate = paymentDate; }
        
        public String getPaymentType() { return paymentType; }
        public void setPaymentType(String paymentType) { this.paymentType = paymentType; }
    }
    
    // Inner class to represent loan charges
    public static class LoanCharge {
        private String chargeType;
        private String payableTo;
        private double amount;
        private boolean isRecurring;
        
        public LoanCharge() {}
        
        public LoanCharge(String chargeType, String payableTo, double amount, boolean isRecurring) {
            this.chargeType = chargeType;
            this.payableTo = payableTo;
            this.amount = amount;
            this.isRecurring = isRecurring;
        }
        
        // Getters and Setters
        public String getChargeType() { return chargeType; }
        public void setChargeType(String chargeType) { this.chargeType = chargeType; }
        
        public String getPayableTo() { return payableTo; }
        public void setPayableTo(String payableTo) { this.payableTo = payableTo; }
        
        public double getAmount() { return amount; }
        public void setAmount(double amount) { this.amount = amount; }
        
        public boolean isRecurring() { return isRecurring; }
        public void setRecurring(boolean recurring) { isRecurring = recurring; }
    }
    
    // Constructor
    public LoanOutput() {
        this.paymentSchedule = new ArrayList<>();
        this.disbursementSchedule = new ArrayList<>();
        this.preEmiPayments = new ArrayList<>();
    }
    
    // Methods to add payment entries
    public void addPayment(int monthNumber, double emi, double principalPaid, 
                          double interestPaid, double remainingBalance, 
                          double currentRate, LocalDate paymentDate, String paymentType) {
        MonthlyPayment payment = new MonthlyPayment(monthNumber, emi, principalPaid, 
                                                   interestPaid, remainingBalance, 
                                                   currentRate, paymentDate, paymentType);
        paymentSchedule.add(payment);
        
        // Update totals
        totalInterestPaid += interestPaid;
        totalAmountPaid += emi;
    }
    
    // Getters and Setters
    public double getInitialEMI() { return initialEMI; }
    public void setInitialEMI(double initialEMI) { this.initialEMI = initialEMI; }
    
    public List<MonthlyPayment> getPaymentSchedule() { return paymentSchedule; }
    
    public double getTotalInterestPaid() { return totalInterestPaid; }
    public void setTotalInterestPaid(double totalInterestPaid) { this.totalInterestPaid = totalInterestPaid; }
    
    public double getTotalAmountPaid() { return totalAmountPaid; }
    public void setTotalAmountPaid(double totalAmountPaid) { this.totalAmountPaid = totalAmountPaid; }
    
    public int getActualTenure() { return actualTenure; }
    public void setActualTenure(int actualTenure) { this.actualTenure = actualTenure; }
    
    public String getSummary() { return summary; }
    public void setSummary(String summary) { this.summary = summary; }
    
    public List<DisbursementEntry> getDisbursementSchedule() { return disbursementSchedule; }
    public void setDisbursementSchedule(List<DisbursementEntry> disbursementSchedule) { this.disbursementSchedule = disbursementSchedule; }
    
    public List<PreEmiPayment> getPreEmiPayments() { return preEmiPayments; }
    public void setPreEmiPayments(List<PreEmiPayment> preEmiPayments) { this.preEmiPayments = preEmiPayments; }
    
    public BrokenPeriodInterest getBrokenPeriodInterest() { return brokenPeriodInterest; }
    public void setBrokenPeriodInterest(BrokenPeriodInterest brokenPeriodInterest) { this.brokenPeriodInterest = brokenPeriodInterest; }
    
    // Utility methods
    public void calculateTotals() {
        totalInterestPaid = paymentSchedule.stream()
            .mapToDouble(MonthlyPayment::getInterestPaid)
            .sum();
        totalAmountPaid = paymentSchedule.stream()
            .mapToDouble(MonthlyPayment::getEmi)
            .sum();
        totalAmountPaid += preEmiPayments.stream()
            .mapToDouble(PreEmiPayment::getInterestAmount)
            .sum();
        actualTenure = paymentSchedule.size();
    }
}
