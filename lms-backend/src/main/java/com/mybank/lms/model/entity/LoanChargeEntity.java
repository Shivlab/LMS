package com.mybank.lms.model.entity;

import jakarta.persistence.*;
import lombok.Data;
import lombok.NoArgsConstructor;
import lombok.AllArgsConstructor;
import org.hibernate.annotations.CreationTimestamp;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.UUID;

@Entity
@Table(name = "loan_charges")
@Data
@NoArgsConstructor
@AllArgsConstructor
public class LoanChargeEntity {
    
    @Id
    @GeneratedValue(strategy = GenerationType.AUTO)
    private UUID id;
    
    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "loan_id", nullable = false)
    private LoanEntity loan;
    
    @Column(name = "charge_type", length = 100)
    private String chargeType;
    
    @Column(name = "payable_to", length = 100)
    private String payableTo;
    
    @Column(name = "is_recurring")
    private Boolean isRecurring;
    
    @Column(name = "amount", precision = 18, scale = 2)
    private BigDecimal amount;
    
    @CreationTimestamp
    @Column(name = "created_at")
    private LocalDateTime createdAt;
}
