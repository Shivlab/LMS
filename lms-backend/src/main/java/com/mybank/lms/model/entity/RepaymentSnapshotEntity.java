package com.mybank.lms.model.entity;

import jakarta.persistence.*;
import lombok.Data;
import lombok.NoArgsConstructor;
import lombok.AllArgsConstructor;
import org.hibernate.annotations.CreationTimestamp;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.List;
import java.util.UUID;

@Entity
@Table(name = "repayment_snapshot")
@Data
@NoArgsConstructor
@AllArgsConstructor
public class RepaymentSnapshotEntity {
    
    @Id
    @GeneratedValue(strategy = GenerationType.AUTO)
    private UUID id;
    
    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "loan_id", nullable = false)
    private LoanEntity loan;
    
    @Column(name = "snapshot_date", nullable = false)
    private LocalDate snapshotDate;
    
    @Column(name = "principal_balance", nullable = false, precision = 18, scale = 2)
    private BigDecimal principalBalance;
    
    @Column(name = "months_remaining", nullable = false)
    private Integer monthsRemaining;
    
    @Column(name = "annual_rate", nullable = false, precision = 8, scale = 4)
    private BigDecimal annualRate;
    
    @Column(name = "rate_type", length = 20)
    private String rateType;
    
    @Column(name = "apr", precision = 10, scale = 4)
    private BigDecimal apr;
    
    @Column(name = "memo", length = 500)
    private String memo;
    
    @Column(name = "version", nullable = false)
    private Integer version = 1;
    
    @CreationTimestamp
    @Column(name = "created_at")
    private LocalDateTime createdAt;
    
    @OneToMany(mappedBy = "snapshot", cascade = CascadeType.ALL, fetch = FetchType.LAZY)
    private List<RepaymentRowEntity> repaymentRows;
}
