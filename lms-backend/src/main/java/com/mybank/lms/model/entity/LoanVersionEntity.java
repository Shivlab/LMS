package com.mybank.lms.model.entity;

import jakarta.persistence.*;
import lombok.Data;
import lombok.NoArgsConstructor;
import lombok.AllArgsConstructor;
import org.hibernate.annotations.CreationTimestamp;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.UUID;

@Entity
@Table(name = "loan_versions")
@Data
@NoArgsConstructor
@AllArgsConstructor
public class LoanVersionEntity {
    
    @Id
    @GeneratedValue(strategy = GenerationType.UUID)
    private UUID id;
    
    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "loan_id", nullable = false)
    private LoanEntity loan;
    
    @Column(name = "version_number", nullable = false)
    private Integer versionNumber;
    
    // Loan parameters at this version
    @Column(name = "principal", precision = 15, scale = 2, nullable = false)
    private BigDecimal principal;
    
    @Column(name = "annual_rate", precision = 10, scale = 6, nullable = false)
    private BigDecimal annualRate;
    
    @Column(name = "months", nullable = false)
    private Integer months;
    
    @Column(name = "moratorium_months")
    private Integer moratoriumMonths;
    
    @Enumerated(EnumType.STRING)
    @Column(name = "moratorium_type")
    private LoanEntity.MoratoriumType moratoriumType;
    
    @Column(name = "partial_payment_emi", precision = 15, scale = 2)
    private BigDecimal partialPaymentEmi;
    
    @Enumerated(EnumType.STRING)
    @Column(name = "rate_type", nullable = false)
    private LoanEntity.RateType rateType;
    
    @Enumerated(EnumType.STRING)
    @Column(name = "floating_strategy")
    private LoanEntity.FloatingStrategy floatingStrategy;
    
    @Enumerated(EnumType.STRING)
    @Column(name = "compounding_frequency", nullable = false)
    private LoanEntity.CompoundingFrequency compoundingFrequency;
    
    @Column(name = "reset_periodicity_months")
    private Integer resetPeriodicityMonths;
    
    @Column(name = "benchmark_name")
    private String benchmarkName;
    
    @Column(name = "spread", precision = 10, scale = 6)
    private BigDecimal spread;
    
    @Column(name = "loan_issue_date")
    private LocalDate loanIssueDate;
    
    @Column(name = "start_date")
    private LocalDate startDate;
    
    @Column(name = "product_type")
    private String productType;
    
    @Column(name = "customer_id")
    private String customerId;
    
    // Version metadata
    @Enumerated(EnumType.STRING)
    @Column(name = "change_reason", nullable = false)
    private ChangeReason changeReason;
    
    @Column(name = "change_description")
    private String changeDescription;
    
    @Column(name = "changed_fields", columnDefinition = "TEXT")
    private String changedFields; // JSON array of field names that changed
    
    @Column(name = "previous_values", columnDefinition = "TEXT")
    private String previousValues; // JSON object of previous values
    
    @CreationTimestamp
    @Column(name = "created_at", nullable = false)
    private LocalDateTime createdAt;
    
    @Column(name = "created_by")
    private String createdBy;
    
    @Column(name = "effective_from")
    private LocalDateTime effectiveFrom;
    
    public enum ChangeReason {
        INITIAL_CREATION,
        RATE_MODIFICATION,
        SPREAD_MODIFICATION,
        TERM_MODIFICATION,
        BENCHMARK_CHANGE,
        MANUAL_CORRECTION,
        REGULATORY_CHANGE,
        CUSTOMER_REQUEST
    }
}
