package com.mybank.lms.controller;

import com.mybank.lms.model.dto.BenchmarkDTO;
import com.mybank.lms.service.BenchmarkService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.math.BigDecimal;
import java.util.List;

@RestController
@RequestMapping("/api/v1/benchmarks")
@RequiredArgsConstructor
@Slf4j
@CrossOrigin(origins = "http://localhost:3000")
public class BenchmarkController {

    private final BenchmarkService benchmarkService;

    @PostMapping("/{benchmarkName}/rates")
    public ResponseEntity<String> addBenchmarkRate(
            @PathVariable String benchmarkName,
            @RequestParam BigDecimal rate) {
        try {
            benchmarkService.addBenchmark(benchmarkName, rate);
            return ResponseEntity.ok("Benchmark rate added successfully and applied to all floating loans");
        } catch (Exception e) {
            log.error("Error adding benchmark rate", e);
            return ResponseEntity.badRequest().body("Error: " + e.getMessage());
        }
    }

    @GetMapping("/{benchmarkName}/history")
    public ResponseEntity<List<BenchmarkDTO>> getBenchmarkHistory(@PathVariable String benchmarkName) {
        try {
            List<BenchmarkDTO> history = benchmarkService.getBenchmarkHistory(benchmarkName);
            return ResponseEntity.ok(history);
        } catch (Exception e) {
            log.error("Error retrieving benchmark history", e);
            return ResponseEntity.badRequest().build();
        }
    }

    @GetMapping("/{benchmarkName}/current")
    public ResponseEntity<BenchmarkDTO> getCurrentBenchmarkRate(@PathVariable String benchmarkName) {
        try {
            BenchmarkDTO current = benchmarkService.getCurrentBenchmarkRate(benchmarkName);
            if (current != null) {
                return ResponseEntity.ok(current);
            } else {
                return ResponseEntity.notFound().build();
            }
        } catch (Exception e) {
            log.error("Error retrieving current benchmark rate", e);
            return ResponseEntity.badRequest().build();
        }
    }

    @GetMapping
    public ResponseEntity<List<String>> getAllBenchmarkNames() {
        try {
            List<String> benchmarkNames = benchmarkService.getAllBenchmarkNames();
            return ResponseEntity.ok(benchmarkNames);
        } catch (Exception e) {
            log.error("Error retrieving benchmark names", e);
            return ResponseEntity.badRequest().build();
        }
    }
}
