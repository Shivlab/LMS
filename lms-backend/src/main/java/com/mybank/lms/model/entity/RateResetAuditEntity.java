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
@Table(name = "rate_reset_audit")
@Data
@NoArgsConstructor
@AllArgsConstructor
public class RateResetAuditEntity {
    
    @Id
    @GeneratedValue(strategy = GenerationType.AUTO)
    private UUID id;
    
    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "loan_id")
    private LoanEntity loan;
    
    @Column(name = "previous_rate", precision = 6, scale = 4)
    private BigDecimal previousRate;
    
    @Column(name = "new_rate", precision = 6, scale = 4)
    private BigDecimal newRate;
    
    @Column(name = "reset_date")
    private LocalDate resetDate;
    
    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "snapshot_id")
    private RepaymentSnapshotEntity snapshot;
    
    @CreationTimestamp
    @Column(name = "created_at")
    private LocalDateTime createdAt;
}
