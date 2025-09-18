package com.mybank.lms.model.entity;

import jakarta.persistence.*;
import lombok.Data;
import lombok.NoArgsConstructor;
import lombok.AllArgsConstructor;
import org.hibernate.annotations.CreationTimestamp;

import java.time.LocalDateTime;
import java.util.UUID;

@Entity
@Table(name = "kfs_versions")
@Data
@NoArgsConstructor
@AllArgsConstructor
public class KfsVersionEntity {
    
    @Id
    @GeneratedValue(strategy = GenerationType.UUID)
    private UUID id;
    
    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "loan_id", nullable = false)
    private LoanEntity loan;
    
    @Column(name = "version_number", nullable = false)
    private Integer versionNumber;
    
    @Column(name = "kfs_data", columnDefinition = "TEXT", nullable = false)
    private String kfsData; // JSON representation of the KFS
    
    @Column(name = "generated_for_rate", precision = 10, scale = 6)
    private java.math.BigDecimal generatedForRate;
    
    @Column(name = "generated_for_spread", precision = 10, scale = 6)
    private java.math.BigDecimal generatedForSpread;
    
    @Column(name = "generated_for_apr", precision = 10, scale = 6)
    private java.math.BigDecimal generatedForApr;
    
    @Column(name = "benchmark_name")
    private String benchmarkName;
    
    @Enumerated(EnumType.STRING)
    @Column(name = "trigger_reason", nullable = false)
    private TriggerReason triggerReason;
    
    @Column(name = "memo")
    private String memo;
    
    @CreationTimestamp
    @Column(name = "created_at", nullable = false)
    private LocalDateTime createdAt;
    
    @Column(name = "created_by")
    private String createdBy;
    
    public enum TriggerReason {
        INITIAL_LOAN_CREATION,
        RATE_CHANGE,
        SPREAD_CHANGE,
        BENCHMARK_RESET,
        MANUAL_REGENERATION,
        LOAN_MODIFICATION
    }
}
