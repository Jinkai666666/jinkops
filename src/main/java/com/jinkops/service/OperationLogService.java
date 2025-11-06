package com.jinkops.service;
import com.jinkops.entity.OperationLogEntity;
import com.jinkops.repository.OperationLogRepository;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Sort;
import org.springframework.stereotype.Service;


//操作日志服务类
@Service
public class OperationLogService {
   private final OperationLogRepository operationLogRepository;


   public OperationLogService(OperationLogRepository operationLogRepository){
       this.operationLogRepository = operationLogRepository;
   }

    // 分页获取全部日志
    public Page<OperationLogEntity> getLogs(Pageable pageable){
        Pageable sortedPageable = PageRequest.of(
                pageable.getPageNumber(),
                pageable.getPageSize(),
                Sort.by(Sort.Direction.DESC, "timestamp") // 按 createTime 倒序
        );
        return operationLogRepository.findAll(sortedPageable);
    }

    // 模糊搜索日志（可按用户名或操作名称模糊匹配）
    public Page<OperationLogEntity> searchLogs(String keyword, Pageable pageable) {
        if (keyword == null || keyword.trim().isEmpty()) {
            // 没有关键字时返回全部
            return getLogs(pageable);
        }
        // 有关键字时执行模糊搜索
        return operationLogRepository.searchLogs(keyword, pageable);
    }



}
