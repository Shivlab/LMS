package com.mybank.lms.service;

import com.mybank.lms.calculator.LoanInput;
import com.mybank.lms.calculator.LoanOutput;
import com.mybank.lms.model.dto.LoanInputDTO;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.junit.jupiter.MockitoExtension;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.time.LocalDate;
import java.util.ArrayList;
import java.util.List;

import static org.junit.jupiter.api.Assertions.*;

@ExtendWith(MockitoExtension.class)
class AprCalculationServiceTest {

    @InjectMocks
    private AprCalculationService aprCalculationService;

    private LoanInput loanInput;
    private LoanOutput loanOutput;
    private List<LoanInputDTO.LoanChargeDTO> charges;

    @BeforeEach
    void setUp() {
        // Setup LoanInput
        loanInput = new LoanInput();
        loanInput.setPrincipal(5000000.0); // 50 lakh
        loanInput.setMonths(12);
        loanInput.setAnnualRate(8.5);
        
        // Setup LoanOutput
        loanOutput = new LoanOutput();
        loanOutput.setInitialEMI(43000.0);
        
        // Create sample payment schedule
        List<LoanOutput.MonthlyPayment> schedule = new ArrayList<>();
        LocalDate startDate = LocalDate.now();
        
        for (int i = 1; i <= 12; i++) {
            LoanOutput.MonthlyPayment payment = new LoanOutput.MonthlyPayment(
                i, 43000.0, 20000.0, 23000.0, 4800000.0 - (i * 20000.0), 
                8.5, startDate.plusMonths(i), "NORMAL"
            );
            schedule.add(payment);
        }
        loanOutput.setPaymentSchedule(schedule);

        // Setup charges
        charges = new ArrayList<>();
        LoanInputDTO.LoanChargeDTO processingFee = new LoanInputDTO.LoanChargeDTO();
        processingFee.setChargeType("Processing Fee");
        processingFee.setPayableTo("Bank");
        processingFee.setAmount(BigDecimal.valueOf(50000.0));
        processingFee.setIsRecurring(false);
        charges.add(processingFee);
        
        LoanInputDTO.LoanChargeDTO legalFee = new LoanInputDTO.LoanChargeDTO();
        legalFee.setChargeType("Legal Fee");
        legalFee.setPayableTo("Lawyer");
        legalFee.setAmount(BigDecimal.valueOf(25000.0));
        legalFee.setIsRecurring(false);
        charges.add(legalFee);
    }

    @Test
    void testCalculateAPR_WithCharges() {
        // When
        BigDecimal apr = aprCalculationService.calculateAPR(loanInput, loanOutput, charges);

        // Then
        assertNotNull(apr);
        assertTrue(apr.compareTo(BigDecimal.ZERO) > 0);
        
        // Calculate expected APR manually
        // Total interest = 12 * 23000 = 276000
        // Total fees = 50000 + 25000 = 75000
        // Fee and interest rate = ((75000 + 276000) / 5000000 / 12) * 100 = 0.585%
        // APR = 0.585% + 8.5% = 9.085%
        BigDecimal expectedAPR = BigDecimal.valueOf(9.085).setScale(4, RoundingMode.HALF_UP);
        assertEquals(expectedAPR, apr);
    }

    @Test
    void testCalculateAPR_WithoutCharges() {
        // Given
        List<LoanInputDTO.LoanChargeDTO> emptyCharges = new ArrayList<>();

        // When
        BigDecimal apr = aprCalculationService.calculateAPR(loanInput, loanOutput, emptyCharges);

        // Then
        assertNotNull(apr);
        assertTrue(apr.compareTo(BigDecimal.ZERO) > 0);
        
        // Calculate expected APR manually
        // Total interest = 12 * 23000 = 276000
        // Total fees = 0
        // Fee and interest rate = ((0 + 276000) / 5000000 / 12) * 100 = 0.46%
        // APR = 0.46% + 8.5% = 8.96%
        BigDecimal expectedAPR = BigDecimal.valueOf(8.96).setScale(4, RoundingMode.HALF_UP);
        assertEquals(expectedAPR, apr);
    }

    @Test
    void testCalculateAPR_WithRecurringCharges() {
        // Given
        List<LoanInputDTO.LoanChargeDTO> recurringCharges = new ArrayList<>();
        LoanInputDTO.LoanChargeDTO monthlyFee = new LoanInputDTO.LoanChargeDTO();
        monthlyFee.setChargeType("Monthly Service Fee");
        monthlyFee.setPayableTo("Bank");
        monthlyFee.setAmount(BigDecimal.valueOf(1000.0));
        monthlyFee.setIsRecurring(true);
        recurringCharges.add(monthlyFee);

        // When
        BigDecimal apr = aprCalculationService.calculateAPR(loanInput, loanOutput, recurringCharges);

        // Then
        assertNotNull(apr);
        assertTrue(apr.compareTo(BigDecimal.ZERO) > 0);
        
        // Calculate expected APR manually
        // Total interest = 12 * 23000 = 276000
        // Total fees = 1000 * 12 = 12000 (recurring)
        // Fee and interest rate = ((12000 + 276000) / 5000000 / 12) * 100 = 0.48%
        // APR = 0.48% + 8.5% = 8.98%
        BigDecimal expectedAPR = BigDecimal.valueOf(8.98).setScale(4, RoundingMode.HALF_UP);
        assertEquals(expectedAPR, apr);
    }

    @Test
    void testCalculateAPR_FallbackToNominalRate() {
        // Given - null loanOutput to trigger exception
        LoanOutput nullOutput = null;

        // When
        BigDecimal apr = aprCalculationService.calculateAPR(loanInput, nullOutput, charges);

        // Then
        assertNotNull(apr);
        assertEquals(BigDecimal.valueOf(8.5).setScale(4, RoundingMode.HALF_UP), apr);
    }
}
