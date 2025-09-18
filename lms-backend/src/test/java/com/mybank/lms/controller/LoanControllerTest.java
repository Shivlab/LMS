package com.mybank.lms.controller;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.mybank.lms.model.dto.LoanInputDTO;
import com.mybank.lms.model.dto.LoanOutputDTO;
import com.mybank.lms.model.dto.RepaymentScheduleDTO;
import com.mybank.lms.model.entity.LoanEntity;
import com.mybank.lms.service.LoanService;
import com.mybank.lms.service.RepaymentSnapshotService;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.WebMvcTest;
import org.springframework.boot.test.mock.mockito.MockBean;
import org.springframework.http.MediaType;
import org.springframework.test.web.servlet.MockMvc;

import java.math.BigDecimal;
import java.time.LocalDate;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.*;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

@WebMvcTest(LoanController.class)
class LoanControllerTest {

    @Autowired
    private MockMvc mockMvc;

    @MockBean
    private LoanService loanService;

    @MockBean
    private RepaymentSnapshotService repaymentSnapshotService;

    @Autowired
    private ObjectMapper objectMapper;

    private LoanInputDTO loanInputDTO;
    private LoanOutputDTO loanOutputDTO;
    private RepaymentScheduleDTO scheduleDTO;

    @BeforeEach
    void setUp() {
        loanInputDTO = new LoanInputDTO();
        loanInputDTO.setProductType(LoanEntity.ProductType.HOME_LOAN);
        loanInputDTO.setPrincipal(BigDecimal.valueOf(5000000));
        loanInputDTO.setAnnualRate(BigDecimal.valueOf(8.5));
        loanInputDTO.setMonths(240);
        loanInputDTO.setLoanIssueDate(LocalDate.now());
        loanInputDTO.setStartDate(LocalDate.now().plusDays(30));

        loanOutputDTO = new LoanOutputDTO();
        loanOutputDTO.setLoanId("LOAN001");
        loanOutputDTO.setProductType(LoanEntity.ProductType.HOME_LOAN);
        loanOutputDTO.setPrincipal(BigDecimal.valueOf(5000000));
        loanOutputDTO.setAnnualRate(BigDecimal.valueOf(8.5));
        loanOutputDTO.setMonths(240);
        loanOutputDTO.setInitialEmi(BigDecimal.valueOf(43000));

        scheduleDTO = new RepaymentScheduleDTO();
        scheduleDTO.setLoanId("LOAN001");
        scheduleDTO.setPrincipalBalance(BigDecimal.valueOf(4800000));
        scheduleDTO.setCurrentRate(BigDecimal.valueOf(8.5));
        scheduleDTO.setMonthsRemaining(230);
    }

    @Test
    void testCreateLoan_Success() throws Exception {
        // Given
        when(loanService.createLoan(any(LoanInputDTO.class))).thenReturn(loanOutputDTO);

        // When & Then
        mockMvc.perform(post("/api/v1/loans")
                .contentType(MediaType.APPLICATION_JSON)
                .content(objectMapper.writeValueAsString(loanInputDTO)))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.loanId").value("LOAN001"))
                .andExpect(jsonPath("$.principal").value(5000000))
                .andExpect(jsonPath("$.annualRate").value(8.5))
                .andExpect(jsonPath("$.months").value(240));
    }

    @Test
    void testCreateLoan_ValidationError() throws Exception {
        // Given - Invalid loan input (missing required fields)
        LoanInputDTO invalidInput = new LoanInputDTO();

        // When & Then
        mockMvc.perform(post("/api/v1/loans")
                .contentType(MediaType.APPLICATION_JSON)
                .content(objectMapper.writeValueAsString(invalidInput)))
                .andExpect(status().isBadRequest());
    }

    @Test
    void testGetLoanKFS_Success() throws Exception {
        // Given
        when(loanService.getLoanKFS("LOAN001")).thenReturn(loanOutputDTO);

        // When & Then
        mockMvc.perform(get("/api/v1/loans/LOAN001"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.loanId").value("LOAN001"))
                .andExpect(jsonPath("$.principal").value(5000000));
    }

    @Test
    void testGetLoanKFS_NotFound() throws Exception {
        // Given
        when(loanService.getLoanKFS("INVALID")).thenThrow(new RuntimeException("Loan not found"));

        // When & Then
        mockMvc.perform(get("/api/v1/loans/INVALID"))
                .andExpect(status().isNotFound());
    }

    @Test
    void testGetRepaymentSchedule_Success() throws Exception {
        // Given
        when(repaymentSnapshotService.getRepaymentSchedule("LOAN001", null)).thenReturn(scheduleDTO);

        // When & Then
        mockMvc.perform(get("/api/v1/loans/LOAN001/schedule"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.loanId").value("LOAN001"))
                .andExpect(jsonPath("$.principalBalance").value(4800000))
                .andExpect(jsonPath("$.currentRate").value(8.5));
    }

    @Test
    void testGetRepaymentSchedule_WithSnapshotId() throws Exception {
        // Given
        when(repaymentSnapshotService.getRepaymentSchedule("LOAN001", "SNAP001")).thenReturn(scheduleDTO);

        // When & Then
        mockMvc.perform(get("/api/v1/loans/LOAN001/schedule")
                .param("snapshotId", "SNAP001"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.loanId").value("LOAN001"));
    }

    @Test
    void testForceRateReset_Success() throws Exception {
        // Given
        when(loanService.applyBenchmarkToLoan("LOAN001", "MCLR", BigDecimal.valueOf(8.0)))
                .thenReturn(loanOutputDTO);

        // When & Then
        mockMvc.perform(post("/api/v1/loans/LOAN001/force-reset")
                .param("benchmarkName", "MCLR")
                .param("newRate", "8.0"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.loanId").value("LOAN001"));
    }

    @Test
    void testForceRateReset_MissingParameters() throws Exception {
        // When & Then
        mockMvc.perform(post("/api/v1/loans/LOAN001/force-reset"))
                .andExpect(status().isBadRequest());
    }
}
