package com.mybank.lms.model.dto;

import com.fasterxml.jackson.annotation.JsonFormat;
import lombok.Data;
import lombok.NoArgsConstructor;
import lombok.AllArgsConstructor;

import jakarta.validation.constraints.*;
import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.UUID;

@Data
@NoArgsConstructor
@AllArgsConstructor
public class BenchmarkDTO {
    
    private UUID id;
    
    @NotBlank(message = "Benchmark name is required")
    private String benchmarkName;
    
    @JsonFormat(pattern = "yyyy-MM-dd")
    @NotNull(message = "Benchmark date is required")
    private LocalDate benchmarkDate;
    
    @NotNull(message = "Benchmark rate is required")
    @DecimalMin(value = "0.01", message = "Benchmark rate must be greater than 0")
    @DecimalMax(value = "50.00", message = "Benchmark rate must be less than 50%")
    private BigDecimal benchmarkRate;
    
    @JsonFormat(pattern = "yyyy-MM-dd HH:mm:ss")
    private LocalDateTime createdAt;
}
