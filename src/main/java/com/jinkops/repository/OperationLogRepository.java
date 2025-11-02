package com.jinkops.repository;

import com.jinkops.entity.OperationLogEntity;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;
//日志入库操作接口
@Repository
public interface OperationLogRepository extends JpaRepository<OperationLogEntity, Long> {
}
