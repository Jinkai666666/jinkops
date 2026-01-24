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
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.stereotype.Component;
import org.springframework.web.context.request.RequestContextHolder;
import org.springframework.web.context.request.ServletRequestAttributes;

import jakarta.servlet.http.HttpServletRequest;
import java.lang.reflect.Method;
import java.time.LocalDateTime;
import java.util.Arrays;

/**
 * 只记录读日志场景，避免和业务日志混在一起
 */
@Aspect
@Component
@Slf4j
@RequiredArgsConstructor
public class OperationLogAspect {

    private final OperationLogRepository repository;
    private final EventLogService eventLogService;

    /**
     * 拦截带注解的方法
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
        if (traceId == null || traceId.isBlank()) {
            traceId = "N/A";
        }
        Authentication authentication = SecurityContextHolder.getContext().getAuthentication();
        String username = authentication != null ? authentication.getName() : "anonymous";

        ServletRequestAttributes attrs =
                (ServletRequestAttributes) RequestContextHolder.getRequestAttributes();
        HttpServletRequest request = attrs != null ? attrs.getRequest() : null;
        String uri = request != null ? request.getRequestURI() : "";
        String httpMethod = request != null ? request.getMethod() : "";
        String ip = request != null ? request.getRemoteAddr() : "";

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
            entity.setUri(uri);
            entity.setHttpMethod(httpMethod);
            entity.setIp(ip);
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
                entity.setOperation(desc + " (异常)");
                entity.setTraceId(traceId);
                entity.setClassName(className);
                entity.setMethodName(methodName);
                entity.setArgs(args);
                entity.setDescription(desc + " (异常)");
                entity.setElapsedTime(time);
                entity.setCreateTime(LocalDateTime.now());
                entity.setUri(uri);
                entity.setHttpMethod(httpMethod);
                entity.setIp(ip);

                repository.save(entity);
                eventLogService.sendOperationLog(entity);
            } catch (Exception logError) {
                log.warn("Failed to persist/emit operation log after exception", logError);
            }

            throw e;
        }
    }
}
