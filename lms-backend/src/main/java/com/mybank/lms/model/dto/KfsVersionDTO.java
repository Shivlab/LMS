package com.mybank.lms.model.dto;

import com.fasterxml.jackson.annotation.JsonFormat;
import lombok.Data;
import lombok.NoArgsConstructor;
import lombok.AllArgsConstructor;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.UUID;

@Data
@NoArgsConstructor
@AllArgsConstructor
public class KfsVersionDTO {
    
    private UUID id;
    private UUID loanId;
    private Integer versionNumber;
    private String kfsData; // JSON representation of the KFS
    
    private BigDecimal generatedForRate;
    private BigDecimal generatedForSpread;
    private BigDecimal generatedForApr;
    private String benchmarkName;
    
    private String triggerReason;
    private String memo;
    
    @JsonFormat(pattern = "yyyy-MM-dd HH:mm:ss")
    private LocalDateTime createdAt;
    
    private String createdBy;
}
