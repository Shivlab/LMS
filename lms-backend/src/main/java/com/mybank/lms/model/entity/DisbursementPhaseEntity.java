package com.mybank.lms.model.entity;

import jakarta.persistence.*;
import lombok.Data;
import lombok.NoArgsConstructor;
import lombok.AllArgsConstructor;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.UUID;

@Entity
@Table(name = "disbursement_phases")
@Data
@NoArgsConstructor
@AllArgsConstructor
public class DisbursementPhaseEntity {
    
    @Id
    @GeneratedValue(strategy = GenerationType.AUTO)
    private UUID id;
    
    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "loan_id", nullable = false)
    private LoanEntity loan;
    
    @Column(name = "disbursement_date", nullable = false)
    private LocalDate disbursementDate;
    
    @Column(name = "amount", nullable = false, precision = 18, scale = 2)
    private BigDecimal amount;
    
    @Column(name = "description")
    private String description;
    
    @Column(name = "sequence")
    private Integer sequence;
}
