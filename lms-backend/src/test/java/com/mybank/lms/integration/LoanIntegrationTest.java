package com.mybank.lms.integration;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.mybank.lms.model.dto.LoanInputDTO;
import com.mybank.lms.model.entity.LoanEntity;
import com.mybank.lms.repository.LoanRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureWebMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.http.MediaType;
import org.springframework.test.context.DynamicPropertyRegistry;
import org.springframework.test.context.DynamicPropertySource;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.transaction.annotation.Transactional;
import org.testcontainers.containers.PostgreSQLContainer;
import org.testcontainers.junit.jupiter.Container;
import org.testcontainers.junit.jupiter.Testcontainers;

import java.math.BigDecimal;
import java.time.LocalDate;

import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.*;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

@SpringBootTest
@AutoConfigureWebMvc
@Testcontainers
@Transactional
class LoanIntegrationTest {

    @Container
    static PostgreSQLContainer<?> postgres = new PostgreSQLContainer<>("postgres:15-alpine")
            .withDatabaseName("lms_test")
            .withUsername("test")
            .withPassword("test");

    @DynamicPropertySource
    static void configureProperties(DynamicPropertyRegistry registry) {
        registry.add("spring.datasource.url", postgres::getJdbcUrl);
        registry.add("spring.datasource.username", postgres::getUsername);
        registry.add("spring.datasource.password", postgres::getPassword);
        registry.add("spring.jpa.hibernate.ddl-auto", () -> "create-drop");
        registry.add("spring.flyway.enabled", () -> "false");
    }

    @Autowired
    private MockMvc mockMvc;

    @Autowired
    private ObjectMapper objectMapper;

    @Autowired
    private LoanRepository loanRepository;

    private LoanInputDTO loanInputDTO;

    @BeforeEach
    void setUp() {
        loanInputDTO = new LoanInputDTO();
        loanInputDTO.setProductType(LoanEntity.ProductType.HOME_LOAN);
        loanInputDTO.setPrincipal(BigDecimal.valueOf(5000000));
        loanInputDTO.setAnnualRate(BigDecimal.valueOf(8.5));
        loanInputDTO.setMonths(240);
        loanInputDTO.setLoanIssueDate(LocalDate.now());
        loanInputDTO.setStartDate(LocalDate.now().plusDays(30));
    }

    @Test
    void testCreateLoanEndToEnd() throws Exception {
        // When - Create loan
        String response = mockMvc.perform(post("/api/v1/loans")
                .contentType(MediaType.APPLICATION_JSON)
                .content(objectMapper.writeValueAsString(loanInputDTO)))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.loanId").exists())
                .andExpect(jsonPath("$.principal").value(5000000))
                .andExpect(jsonPath("$.annualRate").value(8.5))
                .andReturn().getResponse().getContentAsString();

        // Extract loan ID from response
        String loanId = objectMapper.readTree(response).get("loanId").asText();

        // Then - Verify loan exists in database
        var savedLoan = loanRepository.findById(loanId);
        assert savedLoan.isPresent();
        assert savedLoan.get().getPrincipal().equals(BigDecimal.valueOf(5000000));

        // Then - Get KFS
        mockMvc.perform(get("/api/v1/loans/" + loanId))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.loanId").value(loanId))
                .andExpect(jsonPath("$.principal").value(5000000));

        // Then - Get repayment schedule
        mockMvc.perform(get("/api/v1/loans/" + loanId + "/schedule"))
                .andExpect(status().isOk())
                .andExpected(jsonPath("$.loanId").value(loanId))
                .andExpect(jsonPath("$.repaymentRows").isArray());
    }

    @Test
    void testFloatingRateLoanWithBenchmark() throws Exception {
        // Given - Floating rate loan
        loanInputDTO.setRateType(LoanEntity.RateType.FLOATING);
        loanInputDTO.setBenchmarkName("MCLR");
        loanInputDTO.setSpread(BigDecimal.valueOf(0.25));
        loanInputDTO.setResetPeriodicityMonths(12);

        // When - Create loan
        String response = mockMvc.perform(post("/api/v1/loans")
                .contentType(MediaType.APPLICATION_JSON)
                .content(objectMapper.writeValueAsString(loanInputDTO)))
                .andExpect(status().isCreated())
                .andReturn().getResponse().getContentAsString();

        String loanId = objectMapper.readTree(response).get("loanId").asText();

        // Then - Force rate reset
        mockMvc.perform(post("/api/v1/loans/" + loanId + "/force-reset")
                .param("benchmarkName", "MCLR")
                .param("newRate", "8.0"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.loanId").value(loanId));
    }
}
