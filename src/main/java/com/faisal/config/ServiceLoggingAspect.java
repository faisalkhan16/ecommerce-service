package com.faisal.config;

import lombok.extern.slf4j.Slf4j;
import org.aspectj.lang.ProceedingJoinPoint;
import org.aspectj.lang.annotation.Around;
import org.aspectj.lang.annotation.Aspect;
import org.springframework.stereotype.Component;

@Slf4j
@Aspect
@Component
public class ServiceLoggingAspect {

    @Around("within(com.faisal.service..*)")
    public Object logServiceCalls(ProceedingJoinPoint pjp) throws Throwable {
        long start = System.currentTimeMillis();
        String signature = pjp.getSignature().toShortString();

        try {
            Object result = pjp.proceed();
            long tookMs = System.currentTimeMillis() - start;

            log.debug("SERVICE OK  {} ({} ms)", signature, tookMs);
            return result;
        } catch (Throwable ex) {
            long tookMs = System.currentTimeMillis() - start;
            log.warn("SERVICE FAIL {} ({} ms): {}", signature, tookMs, ex.getMessage());
            throw ex;
        }
    }
}