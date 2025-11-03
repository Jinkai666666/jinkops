package com.jinkops.service;
import com.jinkops.entity.OperationLogEntity;
import com.jinkops.repository.OperationLogRepository;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;


//操作日志服务接口
@Service
public class OperationLogService {
   private final OperationLogRepository operationLogRepository;


   public OperationLogService(OperationLogRepository operationLogRepository){
       this.operationLogRepository = operationLogRepository;
   }

    /**
     * 分页查询日志
     * @param pageable 分页参数
     * @return 分页结果
     */
    public Page<OperationLogEntity> getLogs(Pageable pageable){
        return operationLogRepository.findAll(pageable);
    }
}
