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
@Table(name = "repayment_row")
@Data
@NoArgsConstructor
@AllArgsConstructor
public class RepaymentRowEntity {
    
    @Id
    @GeneratedValue(strategy = GenerationType.AUTO)
    private UUID id;
    
    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "snapshot_id", nullable = false)
    private RepaymentSnapshotEntity snapshot;
    
    @Column(name = "month_number", nullable = false)
    private Integer monthNumber;
    
    @Column(name = "payment_date")
    private LocalDate paymentDate;
    
    @Column(name = "emi", precision = 18, scale = 2)
    private BigDecimal emi;
    
    @Column(name = "principal_paid", precision = 18, scale = 2)
    private BigDecimal principalPaid;
    
    @Column(name = "interest_paid", precision = 18, scale = 2)
    private BigDecimal interestPaid;
    
    @Column(name = "remaining_balance", precision = 18, scale = 2)
    private BigDecimal remainingBalance;
    
    @Column(name = "payment_type", length = 50)
    private String paymentType;
    
    @Column(name = "change_marker")
    private Boolean changeMarker = false;
    
    @CreationTimestamp
    @Column(name = "created_at")
    private LocalDateTime createdAt;
}
