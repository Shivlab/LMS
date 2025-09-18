package com.mybank.lms.service;

import com.mybank.lms.model.dto.BenchmarkDTO;
import com.mybank.lms.model.dto.LoanOutputDTO;
import com.mybank.lms.model.entity.BenchmarkHistoryEntity;
import com.mybank.lms.model.entity.KfsVersionEntity;
import com.mybank.lms.repository.BenchmarkHistoryRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDate;
import java.util.List;
import java.util.UUID;
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
@Slf4j
public class BenchmarkService {
    
    private final BenchmarkHistoryRepository benchmarkRepository;
    private final LoanService loanService;
    private final KfsVersionService kfsVersionService;
    
    @Transactional
    public BenchmarkDTO addBenchmark(String benchmarkName, java.math.BigDecimal rate) {
        log.info("Adding benchmark: {} with rate: {}", benchmarkName, rate);
        
        BenchmarkHistoryEntity entity = new BenchmarkHistoryEntity();
        entity.setBenchmarkName(benchmarkName);
        entity.setBenchmarkRate(rate);
        entity.setBenchmarkDate(LocalDate.now());
        
        BenchmarkHistoryEntity saved = benchmarkRepository.save(entity);
        
        // Trigger rate reset for all floating loans using this benchmark
        triggerRateResetForBenchmark(benchmarkName, rate);
        
        return mapToDTO(saved);
    }

    @Transactional
    public BenchmarkDTO addBenchmark(BenchmarkDTO benchmarkDTO) {
        log.info("Adding benchmark: {} for date: {}", benchmarkDTO.getBenchmarkName(), benchmarkDTO.getBenchmarkDate());
        
        BenchmarkHistoryEntity entity = new BenchmarkHistoryEntity();
        entity.setBenchmarkName(benchmarkDTO.getBenchmarkName());
        entity.setBenchmarkDate(benchmarkDTO.getBenchmarkDate());
        entity.setBenchmarkRate(benchmarkDTO.getBenchmarkRate());
        
        entity = benchmarkRepository.save(entity);
        
        // Trigger rate reset for affected loans
        triggerRateResetForBenchmark(benchmarkDTO.getBenchmarkName(), benchmarkDTO.getBenchmarkRate());
        
        return mapToDTO(entity);
    }
    
    public List<BenchmarkDTO> getAllBenchmarks() {
        return benchmarkRepository.findAll().stream()
            .map(this::mapToDTO)
            .collect(Collectors.toList());
    }
    
    public List<BenchmarkDTO> getBenchmarkHistory(String benchmarkName) {
        return benchmarkRepository.findByBenchmarkNameOrderByBenchmarkDateDesc(benchmarkName).stream()
            .map(this::mapToDTO)
            .collect(Collectors.toList());
    }

    public BenchmarkDTO getCurrentBenchmarkRate(String benchmarkName) {
        List<BenchmarkHistoryEntity> history = benchmarkRepository.findByBenchmarkNameOrderByBenchmarkDateDesc(benchmarkName);
        if (!history.isEmpty()) {
            return mapToDTO(history.get(0));
        }
        return null;
    }

    public List<String> getAllBenchmarkNames() {
        return benchmarkRepository.findDistinctBenchmarkNames();
    }
    
    public BenchmarkDTO getLatestBenchmark(String benchmarkName) {
        return benchmarkRepository.findLatestByBenchmarkName(benchmarkName)
            .map(this::mapToDTO)
            .orElse(null);
    }
    
    private void triggerRateResetForBenchmark(String benchmarkName, java.math.BigDecimal newRate) {
        log.info("Triggering rate reset for benchmark: {} with rate: {}", benchmarkName, newRate);
        
        var affectedLoans = loanService.getActiveFloatingLoansByBenchmark(benchmarkName);
        
        if (affectedLoans == null || affectedLoans.isEmpty()) {
            log.info("No active floating loans found for benchmark: {}", benchmarkName);
            return;
        }
        
        for (var loan : affectedLoans) {
            try {
                loanService.applyBenchmarkToLoan(loan.getId(), benchmarkName, newRate);
                
                // Generate new KFS version for rate change
                LoanOutputDTO kfsData = loanService.getLoanKFS(loan.getId());
                kfsVersionService.createKfsVersion(loan.getId(), kfsData, 
                    KfsVersionEntity.TriggerReason.BENCHMARK_RESET, 
                    "KFS regenerated due to benchmark rate reset: " + benchmarkName, "system");
                
                log.info("Rate reset applied to loan: {}", loan.getId());
            } catch (Exception e) {
                log.error("Failed to apply rate reset to loan: {}", loan.getId(), e);
            }
        }
        
        log.info("Rate reset completed for {} loans", affectedLoans.size());
    }
    
    private BenchmarkDTO mapToDTO(BenchmarkHistoryEntity entity) {
        BenchmarkDTO dto = new BenchmarkDTO();
        dto.setId(entity.getId());
        dto.setBenchmarkName(entity.getBenchmarkName());
        dto.setBenchmarkDate(entity.getBenchmarkDate());
        dto.setBenchmarkRate(entity.getBenchmarkRate());
        dto.setCreatedAt(entity.getCreatedAt());
        return dto;
    }
}
