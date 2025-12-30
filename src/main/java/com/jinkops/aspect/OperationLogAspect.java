package com.jinkops.aspect;

import com.jinkops.annotation.OperationLog;
import com.jinkops.mq.producer.EventLogService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.aspectj.lang.ProceedingJoinPoint;
import org.aspectj.lang.annotation.Around;
import org.aspectj.lang.annotation.Aspect;
import org.aspectj.lang.reflect.MethodSignature;
import org.slf4j.MDC;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.stereotype.Component;
import com.jinkops.entity.log.OperationLogEntity;
import com.jinkops.repository.OperationLogRepository;

import java.lang.reflect.Method;
import java.time.LocalDateTime;
import java.util.Arrays;
import java.util.UUID;

/**
 * 操作日誌切面
 * Operation Log Aspect
 * 自動打印方法名、參數、耗時、traceId
 */
@Aspect // 切面類
@Component //  Spring 掃描加載到容器
@Slf4j
@RequiredArgsConstructor
public class OperationLogAspect {


    //入庫配置
    private final OperationLogRepository repository;
    private final EventLogService eventLogService;
    /**
     * 環繞通知（Around Advice）
     * 攔截所有標註了 @OperationLog 的方法
     * @param joinPoint      代表當前被調用的方法 (method execution context)
     * @param operationLog   註解本身，可取出 value() 的描述文字
     */
    @Around("@annotation(com.jinkops.annotation.OperationLog)")
    public Object recordLog(ProceedingJoinPoint joinPoint) throws Throwable{
        // 記錄方法開始時間（用來計算執行耗時）
        long start = System.currentTimeMillis();

        // 獲取方法信息 (method info)
        String methodName = joinPoint.getSignature().getName();        // 方法名 (method name)
        String className = joinPoint.getTarget().getClass().getSimpleName(); // 類名 (class name)
        String args = Arrays.toString(joinPoint.getArgs());            // 參數數組轉字符串 (args)
        MethodSignature signature = (MethodSignature) joinPoint.getSignature();
        Method method = signature.getMethod();

        OperationLog operationLog = method.getAnnotation(OperationLog.class);
        String desc = operationLog != null ? operationLog.value() : "";



        //全鏈路tranceId 從MDC 取得
        // traceId 從 MDC 取，缺失則生成一個，避免入庫時空值報錯
        String traceId = MDC.get("traceId");
        if (traceId == null || traceId.isBlank()) {
            traceId = UUID.randomUUID().toString();
            MDC.put("traceId", traceId);
        }

        // 當前用戶
        String username = SecurityContextHolder.getContext().getAuthentication().getName();


        try {
            // 執行目標方法 (execute the original method)
            Object result = joinPoint.proceed();

            // 計算耗時
            long time = System.currentTimeMillis() - start;

            //  打印正常日誌（INFO）
            // traceId 已自動從 MDC 輸出
            log.info("操作日誌 => [{}#{}] 描述: {} 參數: {} 耗時: {}ms [traceId={} user={}]",
                    className, methodName, desc, args, time,traceId,username);

            //日誌入庫
            OperationLogEntity entity = new OperationLogEntity();
            entity.setUsername(username);
            entity.setOperation(desc);  // <<< 對應 operation 字段
            entity.setTraceId(traceId);
            entity.setClassName(className);
            entity.setMethodName(methodName);
            entity.setArgs(args);
            entity.setDescription(desc);
            entity.setElapsedTime(time);  // <<< 對應 elapsedTime 字段
            entity.setTimestamp(LocalDateTime.now()); // <<< 明確寫入
            repository.save(entity);
            // 返回原方法的執行結果
            eventLogService.sendOperationLog(entity);
            return result;

        } catch (Throwable e) {
            // 捕獲異常時打印錯誤日誌
            long time = System.currentTimeMillis() - start;

            log.error("操作異常 => [{}#{}] 描述: {} 參數: {} 耗時: {}ms 錯誤: {} [traceId={} user={}]",
                    className, methodName, desc, args, time, e.getMessage(), traceId, username);


            // 異常日誌入庫
            OperationLogEntity entity = new OperationLogEntity();
            entity.setUsername(username);
            entity.setOperation(desc + " (異常)");
            entity.setTraceId(traceId);
            entity.setClassName(className);
            entity.setMethodName(methodName);
            entity.setArgs(args);
            entity.setDescription(desc + " (異常)");
            entity.setElapsedTime(time);
            entity.setTimestamp(LocalDateTime.now());

            repository.save(entity);

            eventLogService.sendOperationLog(entity);

            // 把異常繼續往外拋
            throw e;
        }
    }
}
