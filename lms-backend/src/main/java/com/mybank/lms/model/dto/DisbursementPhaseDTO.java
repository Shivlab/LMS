package com.mybank.lms.model.dto;

import lombok.Data;
import lombok.NoArgsConstructor;
import lombok.AllArgsConstructor;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.UUID;

@Data
@NoArgsConstructor
@AllArgsConstructor
public class DisbursementPhaseDTO {
    
    private UUID id;
    private LocalDate disbursementDate;
    private BigDecimal amount;
    private String description;
    private Integer sequence;
    private boolean canEdit; // Whether this disbursement can be edited (based on date)
}
