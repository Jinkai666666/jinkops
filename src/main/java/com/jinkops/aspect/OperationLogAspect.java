package com.jinkops.aspect;

import com.jinkops.annotation.OperationLog;
import com.jinkops.entity.log.OperationLogEntity;
import com.jinkops.mq.producer.EventLogService;
import com.jinkops.repository.OperationLogRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.aspectj.lang.ProceedingJoinPoint;
import org.aspectj.lang.annotation.Around;
import org.aspectj.lang.annotation.Aspect;
import org.aspectj.lang.reflect.MethodSignature;
import org.slf4j.MDC;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.stereotype.Component;

import java.lang.reflect.Method;
import java.time.LocalDateTime;
import java.util.Arrays;
import java.util.UUID;

/**
 * 只記錄業務審計行為，避免和執行日誌混在一起
 */
@Aspect
@Component
@Slf4j
@RequiredArgsConstructor
public class OperationLogAspect {

    private final OperationLogRepository repository;
    private final EventLogService eventLogService;

    /**
     * 攔截寫操作用的審計註解
     */
    @Around("@annotation(com.jinkops.annotation.OperationLog)")
    public Object recordLog(ProceedingJoinPoint joinPoint) throws Throwable {
        long start = System.currentTimeMillis();

        String methodName = joinPoint.getSignature().getName();
        String className = joinPoint.getTarget().getClass().getSimpleName();
        String args = Arrays.toString(joinPoint.getArgs());
        MethodSignature signature = (MethodSignature) joinPoint.getSignature();
        Method method = signature.getMethod();

        OperationLog operationLog = method.getAnnotation(OperationLog.class);
        String desc = operationLog != null ? operationLog.value() : "";

        String traceId = MDC.get("traceId");
        boolean traceAdded = false;
        if (traceId == null || traceId.isBlank()) {
            traceId = UUID.randomUUID().toString();
            MDC.put("traceId", traceId);
            traceAdded = true;
        }

        String username = SecurityContextHolder.getContext().getAuthentication().getName();

        try {
            Object result = joinPoint.proceed();

            long time = System.currentTimeMillis() - start;

            log.info("[AUDIT] user={} action={} target={}#{} result=success",
                    username, desc, className, methodName);

            OperationLogEntity entity = new OperationLogEntity();
            entity.setUsername(username);
            entity.setOperation(desc);
            entity.setTraceId(traceId);
            entity.setClassName(className);
            entity.setMethodName(methodName);
            entity.setArgs(args);
            entity.setDescription(desc);
            entity.setElapsedTime(time);
            entity.setCreateTime(LocalDateTime.now());
            repository.save(entity);
            eventLogService.sendOperationLog(entity);
            return result;

        } catch (Throwable e) {
            long time = System.currentTimeMillis() - start;

            log.info("[AUDIT] user={} action={} target={}#{} result=failed",
                    username, desc, className, methodName);

            try {
                OperationLogEntity entity = new OperationLogEntity();
                entity.setUsername(username);
                entity.setOperation(desc + " (異常)");
                entity.setTraceId(traceId);
                entity.setClassName(className);
                entity.setMethodName(methodName);
                entity.setArgs(args);
                entity.setDescription(desc + " (異常)");
                entity.setElapsedTime(time);
                entity.setCreateTime(LocalDateTime.now());

                repository.save(entity);
                eventLogService.sendOperationLog(entity);
            } catch (Exception logError) {
                log.warn("Failed to persist/emit operation log after exception", logError);
            }

            throw e;
        } finally {
            if (traceAdded) {
                MDC.remove("traceId");
            }
        }
    }
}
