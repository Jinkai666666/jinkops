package com.jinkops.repository;

import com.jinkops.entity.log.OperationLogEntity;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.time.LocalDateTime;
import java.util.List;

// 日誌入庫操作接口
@Repository
public interface OperationLogRepository extends JpaRepository<OperationLogEntity, Long> {

    // 模糊查詢操作日誌（根據用戶名或操作描述）
    @Query(
            "SELECT l FROM OperationLogEntity l " +
                    "WHERE LOWER(l.username) LIKE LOWER(CONCAT('%', :keyword, '%')) " +
                    "   OR LOWER(l.operation) LIKE LOWER(CONCAT('%', :keyword, '%')) " +
                    "ORDER BY l.timestamp DESC"
    )
    Page<OperationLogEntity> searchLogs(
            @Param("keyword") String keyword,
            Pageable pageable
    );

    // 時間區間查詢
    @Query("""
        SELECT l FROM OperationLogEntity l
        WHERE l.timestamp BETWEEN :start AND :end
        ORDER BY l.timestamp DESC
        """)
    Page<OperationLogEntity> findByTimestampRange(
            @Param("start") LocalDateTime start,
            @Param("end") LocalDateTime end,
            Pageable pageable
    );

    // Quartz 專用：掃描失敗且超過指定時間的操作日誌（只讀）
    @Query(
            "SELECT l FROM OperationLogEntity l " +
                    "WHERE l.timestamp < :threshold " +
                    "ORDER BY l.timestamp ASC"
    )
    List<OperationLogEntity> findBefore(
            @Param("threshold") LocalDateTime threshold
    );

}
