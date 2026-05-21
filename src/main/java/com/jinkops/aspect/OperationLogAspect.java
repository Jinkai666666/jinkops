package com.jinkops.aspect;

import com.jinkops.annotation.OperationLog;
import com.jinkops.audit.AuditContext;
import com.jinkops.entity.log.OperationLogEntity;
import com.jinkops.mq.producer.EventLogService;
import com.jinkops.repository.OperationLogRepository;
import jakarta.servlet.http.HttpServletRequest;
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

import java.lang.reflect.Method;
import java.time.LocalDateTime;
import java.util.Arrays;

/**
 * 操作日誌切面。
 *
 * 只要 Controller 方法加了 @OperationLog，就會在這裡統一記錄：
 * 誰做的、打到哪個接口、花了多久、traceId 是什麼。
 * 如果業務層有補充 Redis / ES / DB 的資訊，也會一起塞進描述和參數裡，方便面試時直接翻日誌看證據。
 */
@Aspect
@Component
@Slf4j
@RequiredArgsConstructor
public class OperationLogAspect {

    private final OperationLogRepository repository;
    private final EventLogService eventLogService;

    // 攔截所有有 @OperationLog 的方法，成功或失敗都會盡量落一筆操作日誌。
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
        if (desc == null || desc.isBlank()) {
            // 如果註解沒寫描述，就用類名#方法名補上，避免前台看到空白操作。
            desc = className + "#" + methodName;
        }

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
            saveLog(username, desc, className, methodName, args, traceId, uri, httpMethod, ip, time, false);
            return result;
        } catch (Throwable e) {
            long time = System.currentTimeMillis() - start;
            try {
                saveLog(username, desc, className, methodName, args, traceId, uri, httpMethod, ip, time, true);
            } catch (Exception logError) {
                log.warn("Failed to persist/emit operation log after exception", logError);
            }
            throw e;
        } finally {
            // 這次請求的補充資訊用完就清掉，不留到下一個請求。
            AuditContext.clear();
        }
    }

    // 真正組裝並保存操作日誌；成功和異常都走這裡，欄位比較不會漏。
    private void saveLog(String username,
                         String desc,
                         String className,
                         String methodName,
                         String args,
                         String traceId,
                         String uri,
                         String httpMethod,
                         String ip,
                         long elapsedTime,
                         boolean failed) {
        String auditSummary = AuditContext.summary();
        String operation = failed ? desc + " (failed)" : desc;
        // description 欄位是 255，這裡截一下，完整證據還會放在 args 裡。
        String finalDesc = truncate(appendAuditSummary(operation, auditSummary), 255);
        String finalArgs = appendAuditArgs(args, auditSummary);

        log.info("[AUDIT] user={} action={} target={}#{} result={}",
                username, finalDesc, className, methodName, failed ? "failed" : "success");

        OperationLogEntity entity = new OperationLogEntity();
        entity.setUsername(username);
        entity.setOperation(operation);
        entity.setTraceId(traceId);
        entity.setClassName(className);
        entity.setMethodName(methodName);
        entity.setArgs(finalArgs);
        entity.setDescription(finalDesc);
        entity.setElapsedTime(elapsedTime);
        entity.setCreateTime(LocalDateTime.now());
        entity.setUri(uri);
        entity.setHttpMethod(httpMethod);
        entity.setIp(ip);
        repository.save(entity);
        eventLogService.sendOperationLog(entity);
    }

    private String appendAuditSummary(String desc, String auditSummary) {
        if (auditSummary == null || auditSummary.isBlank()) {
            return desc;
        }
        return desc + " | " + auditSummary;
    }

    // args 是 TEXT，比較適合放完整的補充證據。
    private String appendAuditArgs(String args, String auditSummary) {
        if (auditSummary == null || auditSummary.isBlank()) {
            return args;
        }
        return args + " | audit={" + auditSummary + "}";
    }

    private String truncate(String value, int maxLength) {
        if (value == null || value.length() <= maxLength) {
            return value;
        }
        return value.substring(0, maxLength);
    }
}
