package com.mybank.lms.calculator;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import java.time.LocalDate;
import java.util.Arrays;

import static org.junit.jupiter.api.Assertions.*;

class LoanCalculatorTest {

    private LoanInput loanInput;

    @BeforeEach
    void setUp() {
        loanInput = new LoanInput();
        loanInput.setPrincipal(5000000.0);
        loanInput.setAnnualRate(8.5);
        loanInput.setMonths(240);
        loanInput.setCompoundingFrequency(LoanInput.CompoundingFrequency.DAILY);
        loanInput.setLoanIssueDate(LocalDate.of(2024, 1, 1));
        loanInput.setStartDate(LocalDate.of(2024, 2, 1));
    }

    @Test
    void testCalculateLoan_BasicLoan() {
        // When
        LoanOutput output = LoanCalculator.calculateLoan(loanInput);

        // Then
        assertNotNull(output);
        assertTrue(output.getInitialEMI() > 0);
        assertNotNull(output.getPaymentSchedule());
        assertFalse(output.getPaymentSchedule().isEmpty());
        assertEquals(240, output.getPaymentSchedule().size());
        
        // Verify first payment
        LoanOutput.MonthlyPayment firstPayment = output.getPaymentSchedule().get(0);
        assertEquals(1, firstPayment.getMonthNumber());
        assertTrue(firstPayment.getEmi() > 0);
        assertTrue(firstPayment.getPrincipalPaid() > 0);
        assertTrue(firstPayment.getInterestPaid() > 0);
    }

    @Test
    void testCalculateLoan_WithMoratorium() {
        // Given
        loanInput.setMoratoriumMonths(6);
        loanInput.setMoratoriumType(LoanInput.MoratoriumType.FULL);

        // When
        LoanOutput output = LoanCalculator.calculateLoan(loanInput);

        // Then
        assertNotNull(output);
        assertTrue(output.getInitialEMI() > 0);
        
        // Check moratorium months have zero EMI
        for (int i = 0; i < 6; i++) {
            LoanOutput.MonthlyPayment payment = output.getPaymentSchedule().get(i);
            assertEquals(0.0, payment.getEmi(), 0.01);
            assertEquals(0.0, payment.getPrincipalPaid(), 0.01);
        }
        
        // Check post-moratorium payments
        LoanOutput.MonthlyPayment postMoratoriumPayment = output.getPaymentSchedule().get(6);
        assertTrue(postMoratoriumPayment.getEmi() > 0);
    }

    @Test
    void testCalculateLoan_MonthlyCompounding() {
        // Given
        loanInput.setCompoundingFrequency(LoanInput.CompoundingFrequency.MONTHLY);

        // When
        LoanOutput output = LoanCalculator.calculateLoan(loanInput);

        // Then
        assertNotNull(output);
        assertTrue(output.getInitialEMI() > 0);
        assertEquals(240, output.getPaymentSchedule().size());
        
        // Verify balance reduces to zero
        LoanOutput.MonthlyPayment lastPayment = output.getPaymentSchedule().get(239);
        assertTrue(Math.abs(lastPayment.getRemainingBalance()) < 1.0);
    }

    @Test
    void testCalculateBPI_WithBrokenPeriod() {
        // Given
        loanInput.setLoanIssueDate(LocalDate.of(2024, 1, 15)); // Mid-month
        loanInput.setStartDate(LocalDate.of(2024, 2, 1));

        // When
        LoanOutput.BrokenPeriodInterest bpi = LoanCalculator.calculateBPI(loanInput);

        // Then
        assertNotNull(bpi);
        assertTrue(bpi.getInterestAmount() > 0);
        assertEquals(17, bpi.getDays()); // 15th to 31st January
        assertTrue(bpi.isAddedToFirstEMI());
    }

    @Test
    void testGoalSeekMonthlyEMI() {
        // When
        double emi = LoanCalculator.goalSeekMonthlyEMI(loanInput);

        // Then
        assertTrue(emi > 0);
        assertTrue(emi > 40000); // Reasonable EMI for 50L loan
        assertTrue(emi < 50000);
    }

    @Test
    void testCalculateLoan_EdgeCase_SingleMonth() {
        // Given
        loanInput.setMonths(1);

        // When
        LoanOutput output = LoanCalculator.calculateLoan(loanInput);

        // Then
        assertNotNull(output);
        assertEquals(1, output.getPaymentSchedule().size());
        
        LoanOutput.MonthlyPayment payment = output.getPaymentSchedule().get(0);
        assertTrue(Math.abs(payment.getRemainingBalance()) < 1.0);
    }

    @Test
    void testCalculateLoan_ZeroRate() {
        // Given
        loanInput.setAnnualRate(0.0);

        // When
        LoanOutput output = LoanCalculator.calculateLoan(loanInput);

        // Then
        assertNotNull(output);
        double expectedEMI = loanInput.getPrincipal() / loanInput.getMonths();
        assertEquals(expectedEMI, output.getInitialEMI(), 0.01);
        
        // All payments should have zero interest
        for (LoanOutput.MonthlyPayment payment : output.getPaymentSchedule()) {
            assertEquals(0.0, payment.getInterestPaid(), 0.01);
        }
    }
}
