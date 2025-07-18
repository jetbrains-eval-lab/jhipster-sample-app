package io.github.jhipster.sample.aop.logging;

import static org.assertj.core.api.Assertions.assertThat;

import ch.qos.logback.classic.Level;
import ch.qos.logback.classic.Logger;
import ch.qos.logback.classic.LoggerContext;
import ch.qos.logback.classic.spi.ILoggingEvent;
import ch.qos.logback.core.read.ListAppender;
import java.util.List;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.context.annotation.EnableAspectJAutoProxy;
import org.springframework.context.annotation.Import;
import org.springframework.stereotype.Service;

/**
 * Test for verifying the functionality of the logging aspect.
 */
@SpringBootTest
@EnableAspectJAutoProxy
@Import(LoggingAspectTest.TestService.class)
class LoggingAspectTest {

    @Autowired
    private TestService testService;

    private ListAppender<ILoggingEvent> listAppender;
    private Logger serviceLogger;

    @BeforeEach
    void setUp() {
        // Set up logger capturing
        LoggerContext loggerContext = (LoggerContext) LoggerFactory.getILoggerFactory();
        serviceLogger = loggerContext.getLogger(TestService.class);

        // Set to debug to capture all logs
        serviceLogger.setLevel(Level.DEBUG);

        // Create and start a list appender to capture logs
        listAppender = new ListAppender<>();
        listAppender.setContext(loggerContext);
        listAppender.start();

        // Add the appender to the logger
        serviceLogger.addAppender(listAppender);
    }

    @AfterEach
    void tearDown() {
        // Remove appender after test
        serviceLogger.detachAppender(listAppender);
    }

    @Test
    void testMethodLogging() {
        testService.testMethod();

        // Get captured logs
        List<ILoggingEvent> logsList = listAppender.list;

        // Verify that the method execution was logged at DEBUG level
        assertThat(logsList)
            .isNotEmpty()
            .anyMatch(
                event ->
                    event.getLevel() == Level.DEBUG &&
                    event.getFormattedMessage().matches(".*Execution time of TestService.testMethod\\(\\): \\d+ ms.*")
            );
    }

    /**
     * Test service that will be intercepted by the logging aspect.
     */
    @Service
    public static class TestService {

        /**
         * Fast method that executes quickly.
         */
        public String testMethod() {
            return "This method is fast";
        }

        /**
         * Slow method that simulates a long-running operation.
         */
        public String slowMethod() {
            try {
                // Sleep for 150ms to trigger the slow method warning
                Thread.sleep(150);
            } catch (InterruptedException e) {
                Thread.currentThread().interrupt();
            }
            return "This method is slow";
        }
    }
}
