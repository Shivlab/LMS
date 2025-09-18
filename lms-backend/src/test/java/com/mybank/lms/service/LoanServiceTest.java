package com.mybank.lms.service;

import com.mybank.lms.calculator.LoanCalculator;
import com.mybank.lms.calculator.LoanInput;
import com.mybank.lms.calculator.LoanOutput;
import com.mybank.lms.model.dto.LoanInputDTO;
import com.mybank.lms.model.dto.LoanOutputDTO;
import com.mybank.lms.model.entity.LoanEntity;
import com.mybank.lms.repository.LoanRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.Optional;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class LoanServiceTest {

    @Mock
    private LoanRepository loanRepository;

    @Mock
    private RepaymentSnapshotService repaymentSnapshotService;

    @Mock
    private AprCalculationService aprCalculationService;

    @InjectMocks
    private LoanService loanService;

    private LoanInputDTO loanInputDTO;
    private LoanEntity loanEntity;

    @BeforeEach
    void setUp() {
        loanInputDTO = new LoanInputDTO();
        loanInputDTO.setProductType(LoanEntity.ProductType.HOME_LOAN);
        loanInputDTO.setPrincipal(BigDecimal.valueOf(5000000));
        loanInputDTO.setAnnualRate(BigDecimal.valueOf(8.5));
        loanInputDTO.setMonths(240);
        loanInputDTO.setRateType(LoanEntity.RateType.FIXED);
        loanInputDTO.setCompoundingFrequency(LoanInput.CompoundingFrequency.DAILY);
        loanInputDTO.setLoanIssueDate(LocalDate.now());
        loanInputDTO.setStartDate(LocalDate.now().plusDays(30));

        loanEntity = new LoanEntity();
        loanEntity.setLoanId("LOAN001");
        loanEntity.setProductType(LoanEntity.ProductType.HOME_LOAN);
        loanEntity.setPrincipal(BigDecimal.valueOf(5000000));
        loanEntity.setAnnualRate(BigDecimal.valueOf(8.5));
        loanEntity.setMonths(240);
        loanEntity.setStatus(LoanEntity.LoanStatus.ACTIVE);
    }

    @Test
    void testCreateLoan_Success() {
        // Given
        when(loanRepository.save(any(LoanEntity.class))).thenReturn(loanEntity);
        when(aprCalculationService.calculateAPR(any(), any())).thenReturn(BigDecimal.valueOf(8.75));

        // When
        LoanOutputDTO result = loanService.createLoan(loanInputDTO);

        // Then
        assertNotNull(result);
        assertEquals("LOAN001", result.getLoanId());
        assertEquals(LoanEntity.ProductType.HOME_LOAN, result.getProductType());
        assertEquals(BigDecimal.valueOf(5000000), result.getPrincipal());
        
        verify(loanRepository).save(any(LoanEntity.class));
        verify(repaymentSnapshotService).createInitialSnapshot(any(LoanEntity.class), any(LoanOutput.class));
    }

    @Test
    void testGetLoanKFS_Success() {
        // Given
        when(loanRepository.findById("LOAN001")).thenReturn(Optional.of(loanEntity));

        // When
        LoanOutputDTO result = loanService.getLoanKFS("LOAN001");

        // Then
        assertNotNull(result);
        assertEquals("LOAN001", result.getLoanId());
        verify(loanRepository).findById("LOAN001");
    }

    @Test
    void testGetLoanKFS_NotFound() {
        // Given
        when(loanRepository.findById("INVALID")).thenReturn(Optional.empty());

        // When & Then
        assertThrows(RuntimeException.class, () -> loanService.getLoanKFS("INVALID"));
    }

    @Test
    void testConvertToLoanInput() {
        // When
        LoanInput result = loanService.convertToLoanInput(loanInputDTO);

        // Then
        assertNotNull(result);
        assertEquals(5000000.0, result.getPrincipal());
        assertEquals(8.5, result.getAnnualRate());
        assertEquals(240, result.getMonths());
        assertEquals(LoanInput.CompoundingFrequency.DAILY, result.getCompoundingFrequency());
    }

    @Test
    void testApplyBenchmarkToLoan_FloatingRate() {
        // Given
        loanEntity.setRateType(LoanEntity.RateType.FLOATING);
        loanEntity.setBenchmarkName("MCLR");
        loanEntity.setSpread(BigDecimal.valueOf(0.25));
        
        when(loanRepository.findById("LOAN001")).thenReturn(Optional.of(loanEntity));
        when(loanRepository.save(any(LoanEntity.class))).thenReturn(loanEntity);

        // When
        loanService.applyBenchmarkToLoan("LOAN001", "MCLR", BigDecimal.valueOf(8.0));

        // Then
        verify(loanRepository).save(any(LoanEntity.class));
        verify(repaymentSnapshotService).createSnapshotForRateReset(any(LoanEntity.class), any(LoanOutput.class));
    }

    @Test
    void testApplyBenchmarkToLoan_FixedRate() {
        // Given
        loanEntity.setRateType(LoanEntity.RateType.FIXED);
        when(loanRepository.findById("LOAN001")).thenReturn(Optional.of(loanEntity));

        // When & Then
        assertThrows(IllegalArgumentException.class, 
            () -> loanService.applyBenchmarkToLoan("LOAN001", "MCLR", BigDecimal.valueOf(8.0)));
    }
}
