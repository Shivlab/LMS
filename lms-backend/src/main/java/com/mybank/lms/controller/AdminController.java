package com.mybank.lms.controller;

import com.mybank.lms.model.dto.BenchmarkDTO;
import com.mybank.lms.service.BenchmarkService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import jakarta.validation.Valid;
import java.util.List;

@RestController
@RequestMapping("/api/v1/admin")
@RequiredArgsConstructor
@Slf4j
@CrossOrigin(origins = "*")
public class AdminController {
    
    private final BenchmarkService benchmarkService;
    
    @PostMapping("/benchmarks")
    public ResponseEntity<BenchmarkDTO> addBenchmark(@Valid @RequestBody BenchmarkDTO benchmarkDTO) {
        log.info("Adding benchmark: {} for date: {}", benchmarkDTO.getBenchmarkName(), benchmarkDTO.getBenchmarkDate());
        
        try {
            BenchmarkDTO savedBenchmark = benchmarkService.addBenchmark(benchmarkDTO);
            return ResponseEntity.status(HttpStatus.CREATED).body(savedBenchmark);
        } catch (Exception e) {
            log.error("Error adding benchmark", e);
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).build();
        }
    }
    
    @GetMapping("/benchmarks")
    public ResponseEntity<List<BenchmarkDTO>> getAllBenchmarks() {
        log.info("Fetching all benchmarks");
        
        try {
            List<BenchmarkDTO> benchmarks = benchmarkService.getAllBenchmarks();
            return ResponseEntity.ok(benchmarks);
        } catch (Exception e) {
            log.error("Error fetching benchmarks", e);
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).build();
        }
    }
    
    @GetMapping("/benchmarks/{benchmarkName}")
    public ResponseEntity<List<BenchmarkDTO>> getBenchmarkHistory(@PathVariable String benchmarkName) {
        log.info("Fetching benchmark history for: {}", benchmarkName);
        
        try {
            List<BenchmarkDTO> history = benchmarkService.getBenchmarkHistory(benchmarkName);
            return ResponseEntity.ok(history);
        } catch (Exception e) {
            log.error("Error fetching benchmark history", e);
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).build();
        }
    }
    
    @GetMapping("/benchmarks/{benchmarkName}/latest")
    public ResponseEntity<BenchmarkDTO> getLatestBenchmark(@PathVariable String benchmarkName) {
        log.info("Fetching latest benchmark for: {}", benchmarkName);
        
        try {
            BenchmarkDTO benchmark = benchmarkService.getLatestBenchmark(benchmarkName);
            if (benchmark != null) {
                return ResponseEntity.ok(benchmark);
            } else {
                return ResponseEntity.notFound().build();
            }
        } catch (Exception e) {
            log.error("Error fetching latest benchmark", e);
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).build();
        }
    }
}
