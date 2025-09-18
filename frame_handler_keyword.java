package com.automation.keywords;

import org.openqa.selenium.*;
import org.openqa.selenium.support.ui.WebDriverWait;
import org.openqa.selenium.support.ui.ExpectedConditions;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import java.time.Duration;
import java.util.List;
import java.util.Properties;
import java.io.FileInputStream;
import java.io.IOException;

/**
 * Enterprise-level Frame Handler for Selenium WebDriver
 * 
 * Provides comprehensive frame switching capabilities with:
 * - Fail-safe design with explicit waits
 * - Proper exception handling and logging
 * - Auto screenshot on failures
 * - Thread-safe implementation
 * - Configurable timeouts and retry logic
 * 
 * @author Automation Team
 * @version 1.0
 */
public class FrameHandler {
    
    private static final Logger log = LoggerFactory.getLogger(FrameHandler.class);
    private static final String CONFIG_FILE = "config/automation.properties";
    
    // Thread-safe WebDriver instance
    private static ThreadLocal<WebDriver> driver = new ThreadLocal<>();
    private static Properties config;
    
    // Configuration constants
    private static final int DEFAULT_TIMEOUT = 10;
    private static final int DEFAULT_RETRY_COUNT = 3;
    private static final int POLLING_INTERVAL = 500; // milliseconds
    
    static {
        loadConfiguration();
    }
    
    /**
     * Load configuration from properties file
     */
    private static void loadConfiguration() {
        config = new Properties();
        try (FileInputStream fis = new FileInputStream(CONFIG_FILE)) {
            config.load(fis);
            log.info("Configuration loaded successfully from: {}", CONFIG_FILE);
        } catch (IOException e) {
            log.warn("Failed to load config file, using default values: {}", e.getMessage());
            setDefaultConfig();
        }
    }
    
    /**
     * Set default configuration values
     */
    private static void setDefaultConfig() {
        config.setProperty("frame.timeout", String.valueOf(DEFAULT_TIMEOUT));
        config.setProperty("frame.retry.count", String.valueOf(DEFAULT_RETRY_COUNT));
        config.setProperty("screenshot.on.failure", "true");
    }
    
    /**
     * Set WebDriver instance for current thread
     * @param webDriver WebDriver instance
     */
    public static void setDriver(WebDriver webDriver) {
        if (webDriver == null) {
            throw new CustomFrameException("WebDriver instance cannot be null");
        }
        driver.set(webDriver);
        log.debug("WebDriver set for thread: {}", Thread.currentThread().getName());
    }
    
    /**
     * Get WebDriver instance for current thread
     * @return WebDriver instance
     */
    private static WebDriver getDriver() {
        WebDriver webDriver = driver.get();
        if (webDriver == null) {
            throw new CustomFrameException("WebDriver not initialized for current thread");
        }
        return webDriver;
    }
    
    /**
     * Switch to frame by index with fail-safe design
     * 
     * @param frameIndex Index of the frame (0-based)
     * @return FrameOperationResult indicating success/failure with details
     */
    public static FrameOperationResult switchToFrameByIndex(int frameIndex) {
        long startTime = System.currentTimeMillis();
        String methodName = "switchToFrameByIndex";
        
        log.info("[{}] Starting frame switch operation - Frame Index: {}", methodName, frameIndex);
        
        // Input validation - fail fast
        if (frameIndex < 0) {
            String errorMsg = "Frame index cannot be negative: " + frameIndex;
            log.error("[{}] {}", methodName, errorMsg);
            return createFailureResult(errorMsg, startTime);
        }
        
        return executeFrameOperation(() -> {
            WebDriverWait wait = createWebDriverWait();
            
            // Wait for frame to be available and switch
            wait.until(ExpectedConditions.frameToBeAvailableAndSwitchToIt(frameIndex));
            
            log.info("[{}] Successfully switched to frame with index: {}", methodName, frameIndex);
            return createSuccessResult("Switched to frame by index: " + frameIndex, startTime);
            
        }, methodName, "Frame Index: " + frameIndex, startTime);
    }
    
    /**
     * Switch to frame by name or ID with fail-safe design
     * 
     * @param frameNameOrId Name or ID of the frame
     * @return FrameOperationResult indicating success/failure with details
     */
    public static FrameOperationResult switchToFrameByNameOrId(String frameNameOrId) {
        long startTime = System.currentTimeMillis();
        String methodName = "switchToFrameByNameOrId";
        
        log.info("[{}] Starting frame switch operation - Frame Name/ID: {}", methodName, frameNameOrId);
        
        // Input validation - fail fast
        if (frameNameOrId == null || frameNameOrId.trim().isEmpty()) {
            String errorMsg = "Frame name or ID cannot be null or empty";
            log.error("[{}] {}", methodName, errorMsg);
            return createFailureResult(errorMsg, startTime);
        }
        
        return executeFrameOperation(() -> {
            WebDriverWait wait = createWebDriverWait();
            
            // Wait for frame to be available and switch
            wait.until(ExpectedConditions.frameToBeAvailableAndSwitchToIt(frameNameOrId));
            
            log.info("[{}] Successfully switched to frame: {}", methodName, frameNameOrId);
            return createSuccessResult("Switched to frame: " + frameNameOrId, startTime);
            
        }, methodName, "Frame Name/ID: " + frameNameOrId, startTime);
    }
    
    /**
     * Switch to frame by WebElement with fail-safe design
     * 
     * @param frameElement WebElement representing the frame
     * @return FrameOperationResult indicating success/failure with details
     */
    public static FrameOperationResult switchToFrameByElement(WebElement frameElement) {
        long startTime = System.currentTimeMillis();
        String methodName = "switchToFrameByElement";
        
        log.info("[{}] Starting frame switch operation using WebElement", methodName);
        
        // Input validation - fail fast
        if (frameElement == null) {
            String errorMsg = "Frame WebElement cannot be null";
            log.error("[{}] {}", methodName, errorMsg);
            return createFailureResult(errorMsg, startTime);
        }
        
        return executeFrameOperation(() -> {
            WebDriverWait wait = createWebDriverWait();
            
            // Wait for frame element to be available and switch
            wait.until(ExpectedConditions.frameToBeAvailableAndSwitchToIt(frameElement));
            
            String elementInfo = getElementInfo(frameElement);
            log.info("[{}] Successfully switched to frame using element: {}", methodName, elementInfo);
            return createSuccessResult("Switched to frame using element: " + elementInfo, startTime);
            
        }, methodName, "WebElement: " + getElementInfo(frameElement), startTime);
    }
    
    /**
     * Switch to frame by locator with fail-safe design
     * 
     * @param frameLocator By locator for the frame element
     * @return FrameOperationResult indicating success/failure with details
     */
    public static FrameOperationResult switchToFrameByLocator(By frameLocator) {
        long startTime = System.currentTimeMillis();
        String methodName = "switchToFrameByLocator";
        
        log.info("[{}] Starting frame switch operation - Locator: {}", methodName, frameLocator);
        
        // Input validation - fail fast
        if (frameLocator == null) {
            String errorMsg = "Frame locator cannot be null";
            log.error("[{}] {}", methodName, errorMsg);
            return createFailureResult(errorMsg, startTime);
        }
        
        return executeFrameOperation(() -> {
            WebDriverWait wait = createWebDriverWait();
            
            // Wait for frame to be available and switch
            wait.until(ExpectedConditions.frameToBeAvailableAndSwitchToIt(frameLocator));
            
            log.info("[{}] Successfully switched to frame using locator: {}", methodName, frameLocator);
            return createSuccessResult("Switched to frame using locator: " + frameLocator, startTime);
            
        }, methodName, "Locator: " + frameLocator, startTime);
    }
    
    /**
     * Switch to parent frame with fail-safe design
     * 
     * @return FrameOperationResult indicating success/failure with details
     */
    public static FrameOperationResult switchToParentFrame() {
        long startTime = System.currentTimeMillis();
        String methodName = "switchToParentFrame";
        
        log.info("[{}] Starting switch to parent frame operation", methodName);
        
        return executeFrameOperation(() -> {
            getDriver().switchTo().parentFrame();
            
            log.info("[{}] Successfully switched to parent frame", methodName);
            return createSuccessResult("Switched to parent frame", startTime);
            
        }, methodName, "Parent Frame", startTime);
    }
    
    /**
     * Switch to default content (main document) with fail-safe design
     * 
     * @return FrameOperationResult indicating success/failure with details
     */
    public static FrameOperationResult switchToDefaultContent() {
        long startTime = System.currentTimeMillis();
        String methodName = "switchToDefaultContent";
        
        log.info("[{}] Starting switch to default content operation", methodName);
        
        return executeFrameOperation(() -> {
            getDriver().switchTo().defaultContent();
            
            log.info("[{}] Successfully switched to default content", methodName);
            return createSuccessResult("Switched to default content", startTime);
            
        }, methodName, "Default Content", startTime);
    }
    
    /**
     * Get count of frames on current page
     * 
     * @return FrameOperationResult with frame count
     */
    public static FrameOperationResult getFrameCount() {
        long startTime = System.currentTimeMillis();
        String methodName = "getFrameCount";
        
        log.info("[{}] Getting frame count on current page", methodName);
        
        try {
            List<WebElement> frames = getDriver().findElements(By.tagName("frame"));
            List<WebElement> iframes = getDriver().findElements(By.tagName("iframe"));
            
            int totalFrames = frames.size() + iframes.size();
            
            log.info("[{}] Found {} frames and {} iframes (Total: {})", 
                    methodName, frames.size(), iframes.size(), totalFrames);
            
            return createSuccessResult("Frame count: " + totalFrames, startTime, String.valueOf(totalFrames));
            
        } catch (Exception e) {
            String errorMsg = "Failed to get frame count: " + e.getMessage();
            log.error("[{}] {}", methodName, errorMsg, e);
            
            if (shouldTakeScreenshot()) {
                takeScreenshot(methodName);
            }
            
            return createFailureResult(errorMsg, startTime);
        }
    }
    
    /**
     * Check if frame exists by locator
     * 
     * @param frameLocator By locator for the frame
     * @return FrameOperationResult indicating if frame exists
     */
    public static FrameOperationResult isFramePresent(By frameLocator) {
        long startTime = System.currentTimeMillis();
        String methodName = "isFramePresent";
        
        log.info("[{}] Checking if frame exists - Locator: {}", methodName, frameLocator);
        
        // Input validation - fail fast
        if (frameLocator == null) {
            String errorMsg = "Frame locator cannot be null";
            log.error("[{}] {}", methodName, errorMsg);
            return createFailureResult(errorMsg, startTime);
        }
        
        try {
            List<WebElement> frames = getDriver().findElements(frameLocator);
            boolean isPresent = !frames.isEmpty();
            
            String result = isPresent ? "Frame is present" : "Frame is not present";
            log.info("[{}] {} - Locator: {}", methodName, result, frameLocator);
            
            return createSuccessResult(result, startTime, String.valueOf(isPresent));
            
        } catch (Exception e) {
            String errorMsg = "Failed to check frame presence: " + e.getMessage();
            log.error("[{}] {} - Locator: {}", methodName, errorMsg, frameLocator, e);
            
            if (shouldTakeScreenshot()) {
                takeScreenshot(methodName);
            }
            
            return createFailureResult(errorMsg, startTime);
        }
    }
    
    /**
     * Execute frame operation with retry logic and error handling
     */
    private static FrameOperationResult executeFrameOperation(
            FrameOperation operation, String methodName, String context, long startTime) {
        
        int retryCount = getRetryCount();
        Exception lastException = null;
        
        for (int attempt = 1; attempt <= retryCount; attempt++) {
            try {
                log.debug("[{}] Attempt {} of {} - Context: {}", methodName, attempt, retryCount, context);
                
                return operation.execute();
                
            } catch (TimeoutException e) {
                lastException = e;
                String errorMsg = String.format("Timeout waiting for frame operation - Attempt %d/%d", attempt, retryCount);
                log.warn("[{}] {} - Context: {}", methodName, errorMsg, context);
                
                if (attempt < retryCount) {
                    waitBetweenRetries();
                }
                
            } catch (NoSuchFrameException e) {
                lastException = e;
                String errorMsg = "Frame not found";
                log.error("[{}] {} - Context: {}", methodName, errorMsg, context);
                break; // Don't retry for frame not found
                
            } catch (Exception e) {
                lastException = e;
                String errorMsg = String.format("Unexpected error in frame operation - Attempt %d/%d", attempt, retryCount);
                log.error("[{}] {} - Context: {} - Error: {}", methodName, errorMsg, context, e.getMessage(), e);
                
                if (attempt < retryCount) {
                    waitBetweenRetries();
                }
            }
        }
        
        // All attempts failed
        String finalError = String.format("Frame operation failed after %d attempts: %s", 
                retryCount, lastException.getMessage());
        log.error("[{}] {} - Context: {}", methodName, finalError, context);
        
        if (shouldTakeScreenshot()) {
            takeScreenshot(methodName);
        }
        
        return createFailureResult(finalError, startTime);
    }
    
    /**
     * Create WebDriverWait instance with configured timeout
     */
    private static WebDriverWait createWebDriverWait() {
        int timeout = getTimeout();
        return new WebDriverWait(getDriver(), Duration.ofSeconds(timeout));
    }
    
    /**
     * Get timeout value from configuration
     */
    private static int getTimeout() {
        return Integer.parseInt(config.getProperty("frame.timeout", String.valueOf(DEFAULT_TIMEOUT)));
    }
    
    /**
     * Get retry count from configuration
     */
    private static int getRetryCount() {
        return Integer.parseInt(config.getProperty("frame.retry.count", String.valueOf(DEFAULT_RETRY_COUNT)));
    }
    
    /**
     * Check if screenshot should be taken on failure
     */
    private static boolean shouldTakeScreenshot() {
        return Boolean.parseBoolean(config.getProperty("screenshot.on.failure", "true"));
    }
    
    /**
     * Wait between retry attempts
     */
    private static void waitBetweenRetries() {
        try {
            Thread.sleep(POLLING_INTERVAL);
        } catch (InterruptedException e) {
            Thread.currentThread().interrupt();
            log.warn("Thread interrupted during retry wait");
        }
    }
    
    /**
     * Take screenshot for debugging
     */
    private static void takeScreenshot(String methodName) {
        try {
            if (getDriver() instanceof TakesScreenshot) {
                byte[] screenshot = ((TakesScreenshot) getDriver()).getScreenshotAs(OutputType.BYTES);
                String timestamp = String.valueOf(System.currentTimeMillis());
                String screenshotName = String.format("%s_failure_%s.png", methodName, timestamp);
                
                // Here you would integrate with your reporting framework
                // For example: ExtentReports, Allure, etc.
                log.info("Screenshot captured: {}", screenshotName);
            }
        } catch (Exception e) {
            log.warn("Failed to capture screenshot: {}", e.getMessage());
        }
    }
    
    /**
     * Get element information for logging
     */
    private static String getElementInfo(WebElement element) {
        try {
            String tagName = element.getTagName();
            String id = element.getAttribute("id");
            String name = element.getAttribute("name");
            
            StringBuilder info = new StringBuilder(tagName);
            if (id != null && !id.isEmpty()) {
                info.append("[id='").append(id).append("']");
            }
            if (name != null && !name.isEmpty()) {
                info.append("[name='").append(name).append("']");
            }
            
            return info.toString();
        } catch (Exception e) {
            return "Element info unavailable";
        }
    }
    
    /**
     * Create success result
     */
    private static FrameOperationResult createSuccessResult(String message, long startTime) {
        return createSuccessResult(message, startTime, null);
    }
    
    /**
     * Create success result with additional data
     */
    private static FrameOperationResult createSuccessResult(String message, long startTime, String data) {
        long executionTime = System.currentTimeMillis() - startTime;
        log.info("Frame operation completed successfully in {}ms: {}", executionTime, message);
        
        return new FrameOperationResult(true, message, executionTime, data);
    }
    
    /**
     * Create failure result
     */
    private static FrameOperationResult createFailureResult(String errorMessage, long startTime) {
        long executionTime = System.currentTimeMillis() - startTime;
        log.error("Frame operation failed after {}ms: {}", executionTime, errorMessage);
        
        return new FrameOperationResult(false, errorMessage, executionTime, null);
    }
    
    /**
     * Functional interface for frame operations
     */
    @FunctionalInterface
    private interface FrameOperation {
        FrameOperationResult execute() throws Exception;
    }
    
    /**
     * Result class for frame operations
     */
    public static class FrameOperationResult {
        private final boolean success;
        private final String message;
        private final long executionTimeMs;
        private final String data;
        
        public FrameOperationResult(boolean success, String message, long executionTimeMs, String data) {
            this.success = success;
            this.message = message;
            this.executionTimeMs = executionTimeMs;
            this.data = data;
        }
        
        public boolean isSuccess() { return success; }
        public String getMessage() { return message; }
        public long getExecutionTimeMs() { return executionTimeMs; }
        public String getData() { return data; }
        
        @Override
        public String toString() {
            return String.format("FrameOperationResult{success=%s, message='%s', executionTime=%dms, data='%s'}", 
                    success, message, executionTimeMs, data);
        }
    }
    
    /**
     * Custom exception for frame operations
     */
    public static class CustomFrameException extends RuntimeException {
        public CustomFrameException(String message) {
            super(message);
        }
        
        public CustomFrameException(String message, Throwable cause) {
            super(message, cause);
        }
    }
    
    /**
     * Clean up ThreadLocal to prevent memory leaks
     */
    public static void cleanup() {
        driver.remove();
        log.debug("WebDriver ThreadLocal cleaned up for thread: {}", Thread.currentThread().getName());
    }
}