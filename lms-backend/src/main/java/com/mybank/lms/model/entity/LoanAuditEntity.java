package com.mybank.lms.model.entity;

import jakarta.persistence.*;
import lombok.Data;
import lombok.NoArgsConstructor;
import lombok.AllArgsConstructor;
import org.hibernate.annotations.CreationTimestamp;

import java.time.LocalDateTime;
import java.util.UUID;

@Entity
@Table(name = "loan_audit")
@Data
@NoArgsConstructor
@AllArgsConstructor
public class LoanAuditEntity {
    
    @Id
    @GeneratedValue(strategy = GenerationType.AUTO)
    private UUID id;
    
    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "loan_id", nullable = false)
    private LoanEntity loan;
    
    @Column(name = "field_name", nullable = false, length = 50)
    private String fieldName;
    
    @Column(name = "old_value", length = 255)
    private String oldValue;
    
    @Column(name = "new_value", length = 255)
    private String newValue;
    
    @Column(name = "changed_by", length = 100)
    private String changedBy;
    
    @CreationTimestamp
    @Column(name = "change_date")
    private LocalDateTime changeDate;
}
