package com.yeoun.aop.aspect;

import org.aspectj.lang.ProceedingJoinPoint;
import org.aspectj.lang.annotation.Around;
import org.aspectj.lang.annotation.Aspect;
import org.springframework.stereotype.Component;

import lombok.extern.slf4j.Slf4j;

@Aspect
@Component
@Slf4j
public class ExecutionTimeAspect {
	
    @Around("execution(* com.yeoun..service..*(..))")
    public Object logExecutionTime(ProceedingJoinPoint joinPoint) throws Throwable {
        long start = System.currentTimeMillis();
        Object result = null;
        try {
            result = joinPoint.proceed();
        } finally {
            long end = System.currentTimeMillis();
            log.info("[PERF] {}.{} 실행시간={}ms", 
                     joinPoint.getTarget().getClass().getSimpleName(),
                     joinPoint.getSignature().getName(),
                     (end - start));
        }
        return result;
    }
}
