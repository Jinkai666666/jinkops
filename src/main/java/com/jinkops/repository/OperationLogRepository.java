package com.jinkops.repository;

import com.jinkops.entity.OperationLogEntity;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

// 日志入库操作接口
@Repository
public interface OperationLogRepository extends JpaRepository<OperationLogEntity, Long> {

    // 模糊查询操作日志（根据用户名或操作描述）
    @Query("SELECT l FROM OperationLogEntity l " +
            "WHERE LOWER(l.username) LIKE LOWER(CONCAT('%', :keyword, '%')) " +
            "OR LOWER(l.operation) LIKE LOWER(CONCAT('%', :keyword, '%')) " +
            "ORDER BY l.timestamp DESC")
    Page<OperationLogEntity> searchLogs(@Param("keyword") String keyword, Pageable pageable);
}
