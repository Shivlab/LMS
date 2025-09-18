package com.mybank.lms.model.entity;

import jakarta.persistence.*;
import lombok.Data;
import lombok.NoArgsConstructor;
import lombok.AllArgsConstructor;
import org.hibernate.annotations.CreationTimestamp;
import org.hibernate.annotations.UpdateTimestamp;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.List;
import java.util.UUID;

@Entity
@Table(name = "loans")
@Data
@NoArgsConstructor
@AllArgsConstructor
public class LoanEntity {
    
    @Id
    @GeneratedValue(strategy = GenerationType.AUTO)
    private UUID id;
    
    @Column(name = "customer_id")
    private String customerId;
    
    @Column(name = "product_type", nullable = false, length = 50)
    private String productType;
    
    @Column(name = "loan_issue_date")
    private LocalDate loanIssueDate;
    
    @Column(name = "start_date")
    private LocalDate startDate;
    
    @Column(name = "principal", nullable = false, precision = 18, scale = 2)
    private BigDecimal principal;
    
    // Alias methods for compatibility
    public BigDecimal getLoanAmount() {
        return principal;
    }
    
    public void setLoanAmount(BigDecimal loanAmount) {
        this.principal = loanAmount;
    }
    
    @Column(name = "annual_rate", nullable = false, precision = 8, scale = 4)
    private BigDecimal annualRate;
    
    @Column(name = "rate_type", nullable = false, length = 20)
    @Enumerated(EnumType.STRING)
    private RateType rateType;
    
    @Column(name = "floating_strategy", length = 20)
    @Enumerated(EnumType.STRING)
    private FloatingStrategy floatingStrategy;
    
    @Column(name = "months")
    private Integer months;
    
    // Alias methods for compatibility
    public Integer getTenureMonths() {
        return months;
    }
    
    public void setTenureMonths(Integer tenureMonths) {
        this.months = tenureMonths;
    }
    
    @Column(name = "moratorium_months")
    private Integer moratoriumMonths = 0;
    
    // Alias methods for compatibility
    public Integer getMoratoriumPeriod() {
        return moratoriumMonths;
    }
    
    public void setMoratoriumPeriod(Integer moratoriumPeriod) {
        this.moratoriumMonths = moratoriumPeriod;
    }
    
    @Column(name = "moratorium_type", length = 20)
    @Enumerated(EnumType.STRING)
    private MoratoriumType moratoriumType;
    
    @Column(name = "partial_payment_emi", precision = 18, scale = 2)
    private BigDecimal partialPaymentEmi;
    
    @Column(name = "compounding_frequency", length = 10)
    @Enumerated(EnumType.STRING)
    private CompoundingFrequency compoundingFrequency;
    
    @Column(name = "reset_periodicity_months")
    private Integer resetPeriodicityMonths;
    
    @Column(name = "benchmark_name", length = 50)
    private String benchmarkName;
    
    @Column(name = "spread", precision = 8, scale = 4)
    private BigDecimal spread;
    
    @Column(name = "status", length = 20)
    @Enumerated(EnumType.STRING)
    private LoanStatus status = LoanStatus.ACTIVE;
    
    @CreationTimestamp
    @Column(name = "created_at")
    private LocalDateTime createdAt;
    
    @UpdateTimestamp
    @Column(name = "updated_at")
    private LocalDateTime updatedAt;
    
    // Relationships
    @OneToMany(mappedBy = "loan", cascade = CascadeType.ALL, fetch = FetchType.EAGER)
    private List<DisbursementPhaseEntity> disbursementPhases;
    
    @OneToMany(mappedBy = "loan", cascade = CascadeType.ALL, fetch = FetchType.LAZY)
    private List<LoanChargeEntity> charges;
    
    @OneToMany(mappedBy = "loan", cascade = CascadeType.ALL, fetch = FetchType.LAZY)
    private List<RepaymentSnapshotEntity> snapshots;
    
    // Enums
    public enum RateType {
        FIXED, FLOATING
    }
    
    public enum FloatingStrategy {
        EMI_CONSTANT, TENURE_CONSTANT
    }
    
    public enum MoratoriumType {
        FULL, INTEREST_ONLY, PARTIAL
    }
    
    public enum CompoundingFrequency {
        DAILY, MONTHLY
    }
    
    public enum LoanStatus {
        ACTIVE, CLOSED, CANCELLED
    }
}
