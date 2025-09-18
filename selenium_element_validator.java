package com.enterprise.selenium.keywords;

import org.openqa.selenium.*;
import org.openqa.selenium.support.ui.WebDriverWait;
import org.openqa.selenium.support.ui.ExpectedConditions;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import java.time.Duration;
import java.util.Properties;
import java.io.InputStream;
import java.io.IOException;

/**
 * Enterprise-level Selenium keyword for element state validation
 * Handles isDisplayed(), isEnabled(), and isSelected() with fail-safe design
 * 
 * @author Enterprise Test Team
 * @version 1.0
 */
public class ElementStateValidator {
    
    private static final Logger log = LoggerFactory.getLogger(ElementStateValidator.class);
    private static final ThreadLocal<WebDriver> driverThreadLocal = new ThreadLocal<>();
    private static Properties config;
    
    // Configuration constants
    private static final int DEFAULT_TIMEOUT = 10;
    private static final int DEFAULT_RETRY_COUNT = 3;
    private static final int RETRY_INTERVAL = 1000; // milliseconds
    
    static {
        loadConfiguration();
    }
    
    /**
     * Response object for element state validation
     */
    public static class ElementStateResponse {
        private final boolean isDisplayed;
        private final boolean isEnabled;
        private final boolean isSelected;
        private final boolean isSuccess;
        private final String message;
        private final long executionTimeMs;
        
        public ElementStateResponse(boolean isDisplayed, boolean isEnabled, 
                                  boolean isSelected, boolean isSuccess, 
                                  String message, long executionTimeMs) {
            this.isDisplayed = isDisplayed;
            this.isEnabled = isEnabled;
            this.isSelected = isSelected;
            this.isSuccess = isSuccess;
            this.message = message;
            this.executionTimeMs = executionTimeMs;
        }
        
        // Getters
        public boolean isDisplayed() { return isDisplayed; }
        public boolean isEnabled() { return isEnabled; }
        public boolean isSelected() { return isSelected; }
        public boolean isSuccess() { return isSuccess; }
        public String getMessage() { return message; }
        public long getExecutionTimeMs() { return executionTimeMs; }
        
        @Override
        public String toString() {
            return String.format("ElementState[displayed=%s, enabled=%s, selected=%s, success=%s, time=%dms] - %s",
                    isDisplayed, isEnabled, isSelected, isSuccess, executionTimeMs, message);
        }
    }
    
    /**
     * Custom exception for element state validation failures
     */
    public static class ElementStateValidationException extends Exception {
        public ElementStateValidationException(String message, Throwable cause) {
            super(message, cause);
        }
        
        public ElementStateValidationException(String message) {
            super(message);
        }
    }
    
    /**
     * Load configuration from properties file
     */
    private static void loadConfiguration() {
        config = new Properties();
        try (InputStream input = ElementStateValidator.class.getClassLoader()
                .getResourceAsStream("selenium-config.properties")) {
            if (input != null) {
                config.load(input);
                log.info("Configuration loaded successfully");
            } else {
                log.warn("selenium-config.properties not found, using default values");
            }
        } catch (IOException e) {
            log.error("Error loading configuration: {}", e.getMessage());
        }
    }
    
    /**
     * Set WebDriver for current thread
     */
    public static void setDriver(WebDriver driver) {
        driverThreadLocal.set(driver);
        log.debug("WebDriver set for thread: {}", Thread.currentThread().getName());
    }
    
    /**
     * Get WebDriver for current thread
     */
    private static WebDriver getDriver() {
        WebDriver driver = driverThreadLocal.get();
        if (driver == null) {
            throw new IllegalStateException("WebDriver not initialized for current thread");
        }
        return driver;
    }
    
    /**
     * Validates element state with all three conditions: isDisplayed, isEnabled, isSelected
     * 
     * @param locator Element locator (By object)
     * @param timeoutSeconds Explicit wait timeout (optional, uses config default)
     * @param validateSelected Whether to check isSelected (set false for non-selectable elements)
     * @return ElementStateResponse with validation results
     * @throws ElementStateValidationException if validation fails critically
     */
    public static ElementStateResponse validateElementState(By locator, 
                                                           Integer timeoutSeconds, 
                                                           boolean validateSelected) 
            throws ElementStateValidationException {
        
        long startTime = System.currentTimeMillis();
        String methodName = "validateElementState";
        
        // Input validation - fail fast
        if (locator == null) {
            log.error("{}: Locator cannot be null", methodName);
            throw new ElementStateValidationException("Locator parameter is null");
        }
        
        // Get timeout from config or parameter
        int timeout = timeoutSeconds != null ? timeoutSeconds : 
                     Integer.parseInt(config.getProperty("element.timeout", String.valueOf(DEFAULT_TIMEOUT)));
        
        int retryCount = Integer.parseInt(config.getProperty("retry.count", String.valueOf(DEFAULT_RETRY_COUNT)));
        
        log.info("{}: Starting validation for locator: {} with timeout: {}s, retries: {}", 
                methodName, locator.toString(), timeout, retryCount);
        
        // Retry logic with exponential backoff
        Exception lastException = null;
        for (int attempt = 1; attempt <= retryCount; attempt++) {
            try {
                log.debug("{}: Attempt {} of {}", methodName, attempt, retryCount);
                
                WebDriverWait wait = new WebDriverWait(getDriver(), Duration.ofSeconds(timeout));
                
                // Wait for element presence first
                WebElement element = wait.until(ExpectedConditions.presenceOfElementLocated(locator));
                
                if (element == null) {
                    throw new ElementStateValidationException("Element not found after waiting");
                }
                
                // Check all states
                boolean displayed = false;
                boolean enabled = false;
                boolean selected = false;
                
                // Check isDisplayed()
                try {
                    displayed = element.isDisplayed();
                    log.debug("{}: isDisplayed() = {}", methodName, displayed);
                } catch (StaleElementReferenceException e) {
                    log.warn("{}: Stale element reference on isDisplayed(), re-finding element", methodName);
                    element = getDriver().findElement(locator);
                    displayed = element.isDisplayed();
                }
                
                // Check isEnabled()
                try {
                    enabled = element.isEnabled();
                    log.debug("{}: isEnabled() = {}", methodName, enabled);
                } catch (StaleElementReferenceException e) {
                    log.warn("{}: Stale element reference on isEnabled(), re-finding element", methodName);
                    element = getDriver().findElement(locator);
                    enabled = element.isEnabled();
                }
                
                // Check isSelected() only if requested
                if (validateSelected) {
                    try {
                        selected = element.isSelected();
                        log.debug("{}: isSelected() = {}", methodName, selected);
                    } catch (StaleElementReferenceException e) {
                        log.warn("{}: Stale element reference on isSelected(), re-finding element", methodName);
                        element = getDriver().findElement(locator);
                        selected = element.isSelected();
                    } catch (UnsupportedOperationException e) {
                        log.warn("{}: Element doesn't support isSelected(), setting to false", methodName);
                        selected = false;
                    }
                }
                
                long executionTime = System.currentTimeMillis() - startTime;
                String successMessage = String.format("Element state validated successfully on attempt %d", attempt);
                
                log.info("{}: SUCCESS - displayed={}, enabled={}, selected={}, time={}ms", 
                        methodName, displayed, enabled, selected, executionTime);
                
                return new ElementStateResponse(displayed, enabled, selected, true, 
                                              successMessage, executionTime);
                
            } catch (TimeoutException e) {
                lastException = e;
                log.warn("{}: Timeout on attempt {} - Element not found within {}s", 
                        methodName, attempt, timeout);
                
                if (attempt < retryCount) {
                    try {
                        Thread.sleep(RETRY_INTERVAL * attempt); // Exponential backoff
                    } catch (InterruptedException ie) {
                        Thread.currentThread().interrupt();
                        throw new ElementStateValidationException("Thread interrupted during retry", ie);
                    }
                }
                
            } catch (WebDriverException e) {
                lastException = e;
                log.error("{}: WebDriver exception on attempt {}: {}", methodName, attempt, e.getMessage());
                
                // Take screenshot on failure
                takeScreenshotOnFailure(methodName, locator.toString());
                
                if (attempt < retryCount) {
                    try {
                        Thread.sleep(RETRY_INTERVAL);
                    } catch (InterruptedException ie) {
                        Thread.currentThread().interrupt();
                        throw new ElementStateValidationException("Thread interrupted during retry", ie);
                    }
                } else {
                    // Critical failure - rethrow
                    throw new ElementStateValidationException(
                            String.format("Element state validation failed after %d attempts for locator: %s", 
                                    retryCount, locator.toString()), e);
                }
            }
        }
        
        // All retries exhausted
        long executionTime = System.currentTimeMillis() - startTime;
        String errorMessage = String.format("Element state validation failed after %d attempts for locator: %s", 
                                           retryCount, locator.toString());
        
        log.error("{}: FAILED - {}, time={}ms", methodName, errorMessage, executionTime);
        takeScreenshotOnFailure(methodName, locator.toString());
        
        return new ElementStateResponse(false, false, false, false, errorMessage, executionTime);
    }
    
    /**
     * Overloaded method with default timeout and selection check
     */
    public static ElementStateResponse validateElementState(By locator) 
            throws ElementStateValidationException {
        return validateElementState(locator, null, true);
    }
    
    /**
     * Overloaded method with custom timeout but default selection check
     */
    public static ElementStateResponse validateElementState(By locator, int timeoutSeconds) 
            throws ElementStateValidationException {
        return validateElementState(locator, timeoutSeconds, true);
    }
    
    /**
     * Quick check method that returns boolean for simple validation
     */
    public static boolean isElementReady(By locator, int timeoutSeconds) {
        try {
            ElementStateResponse response = validateElementState(locator, timeoutSeconds, false);
            return response.isSuccess() && response.isDisplayed() && response.isEnabled();
        } catch (ElementStateValidationException e) {
            log.error("isElementReady failed for locator: {} - {}", locator.toString(), e.getMessage());
            return false;
        }
    }
    
    /**
     * Take screenshot on failure and attach to reports
     */
    private static void takeScreenshotOnFailure(String methodName, String locatorInfo) {
        try {
            if (getDriver() instanceof TakesScreenshot) {
                byte[] screenshot = ((TakesScreenshot) getDriver()).getScreenshotAs(OutputType.BYTES);
                log.info("{}: Screenshot captured for failed element: {}", methodName, locatorInfo);
                
                // Here you would integrate with your reporting framework
                // Example: ExtentReports, Allure, TestNG Reporter etc.
                // ExtentTestManager.getTest().addScreenCaptureFromBase64String(Base64.getEncoder().encodeToString(screenshot));
                
            }
        } catch (Exception e) {
            log.error("{}: Failed to capture screenshot: {}", methodName, e.getMessage());
        }
    }
    
    /**
     * Clean up thread local driver
     */
    public static void cleanup() {
        driverThreadLocal.remove();
        log.debug("WebDriver removed from thread: {}", Thread.currentThread().getName());
    }
}