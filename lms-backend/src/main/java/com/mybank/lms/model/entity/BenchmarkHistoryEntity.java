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
@Table(name = "benchmark_history")
@Data
@NoArgsConstructor
@AllArgsConstructor
public class BenchmarkHistoryEntity {
    
    @Id
    @GeneratedValue(strategy = GenerationType.AUTO)
    private UUID id;
    
    @Column(name = "benchmark_name", length = 100)
    private String benchmarkName;
    
    @Column(name = "benchmark_date")
    private LocalDate benchmarkDate;
    
    @Column(name = "benchmark_rate", precision = 6, scale = 4)
    private BigDecimal benchmarkRate;
    
    @CreationTimestamp
    @Column(name = "created_at")
    private LocalDateTime createdAt;
}
