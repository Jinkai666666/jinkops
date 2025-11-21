package com.jinkops.aspect;

import com.jinkops.annotation.OperationLog;
import lombok.extern.slf4j.Slf4j;
import org.aspectj.lang.ProceedingJoinPoint;
import org.aspectj.lang.annotation.Around;
import org.aspectj.lang.annotation.Aspect;
import org.slf4j.MDC;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.stereotype.Component;
import com.jinkops.entity.log.OperationLogEntity;
import com.jinkops.repository.OperationLogRepository;

import java.time.LocalDateTime;
import java.util.Arrays;

/**
 * 操作日志切面
 * Operation Log Aspect
 * 自动打印方法名、参数、耗时、traceId
 */
@Aspect // 切面类
@Component //  Spring 扫描加载到容器
@Slf4j
public class OperationLogAspect {


    //入库配置
    private final OperationLogRepository repository;

    public OperationLogAspect(OperationLogRepository operationLogRepository) {
        this.repository = operationLogRepository;
    }

    /**
     * 环绕通知（Around Advice）
     * 拦截所有标注了 @OperationLog 的方法
     * @param joinPoint      代表当前被调用的方法 (method execution context)
     * @param operationLog   注解本身，可取出 value() 的描述文字
     */
    @Around("@annotation(operationLog)")
    public Object recordLog(ProceedingJoinPoint joinPoint, OperationLog operationLog) throws Throwable {

        // 记录方法开始时间（用来计算执行耗时）
        long start = System.currentTimeMillis();

        // 获取方法信息 (method info)
        String methodName = joinPoint.getSignature().getName();        // 方法名 (method name)
        String className = joinPoint.getTarget().getClass().getSimpleName(); // 类名 (class name)
        String args = Arrays.toString(joinPoint.getArgs());            // 参数数组转字符串 (args)
        String desc = operationLog.value();                            // 从注解里取描述 (annotation description)


        //全链路tranceId 从MDC 取得
        String traceId = MDC.get("traceId");

        // 当前用户
        String username = SecurityContextHolder.getContext().getAuthentication().getName();


        try {
            // 执行目标方法 (execute the original method)
            Object result = joinPoint.proceed();

            // 计算耗时
            long time = System.currentTimeMillis() - start;

            //  打印正常日志（INFO）
            // traceId 已自动从 MDC 输出
            log.info("操作日志 => [{}#{}] 描述: {} 参数: {} 耗时: {}ms [traceId={} user={}]",
                    className, methodName, desc, args, time,traceId,username);

            //日志入库
            OperationLogEntity entity = new OperationLogEntity();
            entity.setUsername(username);
            entity.setOperation(desc);  // <<< 对应 operation 字段
            entity.setTraceId(traceId);
            entity.setClassName(className);
            entity.setMethodName(methodName);
            entity.setArgs(args);
            entity.setDescription(desc);
            entity.setElapsedTime(time);  // <<< 对应 elapsedTime 字段
            entity.setTimestamp(LocalDateTime.now()); // <<< 明确写入
            repository.save(entity);
            // 返回原方法的执行结果
            return result;

        } catch (Throwable e) {
            // 捕获异常时打印错误日志
            long time = System.currentTimeMillis() - start;

            log.error("操作异常 => [{}#{}] 描述: {} 参数: {} 耗时: {}ms 错误: {} [traceId={} user={}]",
                    className, methodName, desc, args, time, e.getMessage(), traceId, username);


            // 异常日志入库
            OperationLogEntity entity = new OperationLogEntity();
            entity.setUsername(username);
            entity.setOperation(desc + " (异常)");
            entity.setTraceId(traceId);
            entity.setClassName(className);
            entity.setMethodName(methodName);
            entity.setArgs(args);
            entity.setDescription(desc + " (异常)");
            entity.setElapsedTime(time);
            entity.setTimestamp(LocalDateTime.now());

            repository.save(entity);

            // 把异常继续往外抛
            throw e;
        }
    }
}
