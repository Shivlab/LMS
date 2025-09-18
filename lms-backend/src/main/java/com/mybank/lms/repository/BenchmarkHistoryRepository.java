package com.mybank.lms.repository;

import com.mybank.lms.model.entity.BenchmarkHistoryEntity;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.time.LocalDate;
import java.util.List;
import java.util.Optional;
import java.util.UUID;

@Repository
public interface BenchmarkHistoryRepository extends JpaRepository<BenchmarkHistoryEntity, UUID> {
    
    List<BenchmarkHistoryEntity> findByBenchmarkNameOrderByBenchmarkDateDesc(String benchmarkName);
    
    @Query("SELECT bh FROM BenchmarkHistoryEntity bh WHERE bh.benchmarkName = :benchmarkName AND bh.benchmarkDate = (SELECT MAX(bh2.benchmarkDate) FROM BenchmarkHistoryEntity bh2 WHERE bh2.benchmarkName = :benchmarkName)")
    Optional<BenchmarkHistoryEntity> findLatestByBenchmarkName(@Param("benchmarkName") String benchmarkName);
    
    @Query("SELECT bh FROM BenchmarkHistoryEntity bh WHERE bh.benchmarkName = :benchmarkName AND bh.benchmarkDate <= :date ORDER BY bh.benchmarkDate DESC LIMIT 1")
    Optional<BenchmarkHistoryEntity> findLatestByBenchmarkNameAndDate(@Param("benchmarkName") String benchmarkName, @Param("date") LocalDate date);
    
    List<BenchmarkHistoryEntity> findByBenchmarkDateAfter(LocalDate date);
    
    @Query("SELECT DISTINCT bh.benchmarkName FROM BenchmarkHistoryEntity bh")
    List<String> findDistinctBenchmarkNames();
}
