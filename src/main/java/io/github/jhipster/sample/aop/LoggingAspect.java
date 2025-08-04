package io.github.jhipster.sample.aop;

import org.aspectj.lang.ProceedingJoinPoint;
import org.aspectj.lang.annotation.Around;
import org.aspectj.lang.annotation.Aspect;
import org.aspectj.lang.annotation.Pointcut;
import org.aspectj.lang.reflect.MethodSignature;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Component;
import org.springframework.util.StopWatch;

/**
 * Aspect for logging execution time of methods in service and repository layers.
 * This helps identify performance bottlenecks in the application.
 */
@Aspect
@Component
public class LoggingAspect {

    /**
     * Pointcut that matches all Spring services.
     */
    @Pointcut("within(@org.springframework.stereotype.Service *)")
    public void springServicePointcut() {
        // Method is empty as this is just a pointcut declaration
    }

    /**
     * Pointcut that matches all Spring repositories.
     */
    @Pointcut("within(@org.springframework.stereotype.Repository *)")
    public void springRepositoryPointcut() {
        // Method is empty as this is just a pointcut declaration
    }

    /**
     * Pointcut that matches all Spring components with Service or Repository annotations.
     */
    @Pointcut("springServicePointcut() || springRepositoryPointcut()")
    public void serviceOrRepositoryPointcut() {
        // Method is empty as this is just a pointcut declaration
    }

    /**
     * Advice that logs when a method is entered and exited, along with execution time.
     *
     * @param joinPoint join point for advice
     * @return result
     * @throws Throwable throws IllegalArgumentException
     */
    @Around("serviceOrRepositoryPointcut()")
    public Object logExecutionTime(ProceedingJoinPoint joinPoint) throws Throwable {
        Logger log = LoggerFactory.getLogger(joinPoint.getSignature().getDeclaringTypeName());

        if (!log.isDebugEnabled()) {
            return joinPoint.proceed();
        }

        MethodSignature methodSignature = (MethodSignature) joinPoint.getSignature();
        String className = methodSignature.getDeclaringType().getSimpleName();
        String methodName = methodSignature.getName();

        final StopWatch stopWatch = new StopWatch();

        stopWatch.start();
        try {
            return joinPoint.proceed();
        } finally {
            stopWatch.stop();
            log.debug("Execution time of {}.{}(): {} ms", className, methodName, stopWatch.getTotalTimeMillis());
        }
    }
}
