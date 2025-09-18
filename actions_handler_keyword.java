package com.automation.keywords;

import org.openqa.selenium.*;
import org.openqa.selenium.interactions.Actions;
import org.openqa.selenium.support.ui.WebDriverWait;
import org.openqa.selenium.support.ui.ExpectedConditions;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import java.time.Duration;
import java.util.Properties;
import java.io.FileInputStream;
import java.io.IOException;

/**
 * Enterprise-level Actions Handler for Selenium WebDriver
 * 
 * Provides comprehensive mouse and keyboard interaction capabilities with:
 * - Fail-safe design with explicit waits
 * - Proper exception handling and logging
 * - Auto screenshot on failures
 * - Thread-safe implementation
 * - Configurable timeouts and retry logic
 * - Support for complex user interactions
 * 
 * @author Automation Team
 * @version 1.0
 */
public class ActionsHandler {
    
    private static final Logger log = LoggerFactory.getLogger(ActionsHandler.class);
    private static final String CONFIG_FILE = "config/automation.properties";
    
    // Thread-safe WebDriver and Actions instances
    private static ThreadLocal<WebDriver> driver = new ThreadLocal<>();
    private static ThreadLocal<Actions> actions = new ThreadLocal<>();
    private static Properties config;
    
    // Configuration constants
    private static final int DEFAULT_TIMEOUT = 10;
    private static final int DEFAULT_RETRY_COUNT = 3;
    private static final int HOVER_WAIT_TIME = 500; // milliseconds
    private static final int DRAG_DROP_PAUSE = 200; // milliseconds
    
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
        config.setProperty("actions.timeout", String.valueOf(DEFAULT_TIMEOUT));
        config.setProperty("actions.retry.count", String.valueOf(DEFAULT_RETRY_COUNT));
        config.setProperty("actions.hover.wait", String.valueOf(HOVER_WAIT_TIME));
        config.setProperty("actions.drag.pause", String.valueOf(DRAG_DROP_PAUSE));
        config.setProperty("screenshot.on.failure", "true");
    }
    
    /**
     * Set WebDriver instance for current thread
     * @param webDriver WebDriver instance
     */
    public static void setDriver(WebDriver webDriver) {
        if (webDriver == null) {
            throw new CustomActionsException("WebDriver instance cannot be null");
        }
        driver.set(webDriver);
        actions.set(new Actions(webDriver));
        log.debug("WebDriver and Actions set for thread: {}", Thread.currentThread().getName());
    }
    
    /**
     * Get WebDriver instance for current thread
     */
    private static WebDriver getDriver() {
        WebDriver webDriver = driver.get();
        if (webDriver == null) {
            throw new CustomActionsException("WebDriver not initialized for current thread");
        }
        return webDriver;
    }
    
    /**
     * Get Actions instance for current thread
     */
    private static Actions getActions() {
        Actions actionsInstance = actions.get();
        if (actionsInstance == null) {
            throw new CustomActionsException("Actions not initialized for current thread");
        }
        return actionsInstance;
    }
    
    /**
     * Click on element using Actions class
     * 
     * @param locator By locator for the element
     * @return ActionOperationResult indicating success/failure with details
     */
    public static ActionOperationResult clickElement(By locator) {
        return clickElement(locator, "Click on element");
    }
    
    /**
     * Click on element using Actions class with custom description
     * 
     * @param locator By locator for the element
     * @param description Custom description for logging
     * @return ActionOperationResult indicating success/failure with details
     */
    public static ActionOperationResult clickElement(By locator, String description) {
        long startTime = System.currentTimeMillis();
        String methodName = "clickElement";
        
        log.info("[{}] Starting action - Description: {} | Locator: {}", methodName, description, locator);
        
        // Input validation - fail fast
        if (locator == null) {
            String errorMsg = "Element locator cannot be null";
            log.error("[{}] {}", methodName, errorMsg);
            return createFailureResult(errorMsg, startTime);
        }
        
        return executeActionOperation(() -> {
            WebElement element = waitForElementToBeClickable(locator);
            
            getActions()
                .click(element)
                .perform();
            
            log.info("[{}] Successfully clicked element - Description: {} | Locator: {}", 
                    methodName, description, locator);
            return createSuccessResult("Element clicked successfully: " + description, startTime);
            
        }, methodName, description + " | Locator: " + locator, startTime);
    }
    
    /**
     * Double click on element using Actions class
     * 
     * @param locator By locator for the element
     * @return ActionOperationResult indicating success/failure with details
     */
    public static ActionOperationResult doubleClickElement(By locator) {
        return doubleClickElement(locator, "Double click on element");
    }
    
    /**
     * Double click on element using Actions class with custom description
     * 
     * @param locator By locator for the element
     * @param description Custom description for logging
     * @return ActionOperationResult indicating success/failure with details
     */
    public static ActionOperationResult doubleClickElement(By locator, String description) {
        long startTime = System.currentTimeMillis();
        String methodName = "doubleClickElement";
        
        log.info("[{}] Starting action - Description: {} | Locator: {}", methodName, description, locator);
        
        if (locator == null) {
            String errorMsg = "Element locator cannot be null";
            log.error("[{}] {}", methodName, errorMsg);
            return createFailureResult(errorMsg, startTime);
        }
        
        return executeActionOperation(() -> {
            WebElement element = waitForElementToBeClickable(locator);
            
            getActions()
                .doubleClick(element)
                .perform();
            
            log.info("[{}] Successfully double-clicked element - Description: {} | Locator: {}", 
                    methodName, description, locator);
            return createSuccessResult("Element double-clicked successfully: " + description, startTime);
            
        }, methodName, description + " | Locator: " + locator, startTime);
    }
    
    /**
     * Right click (context click) on element using Actions class
     * 
     * @param locator By locator for the element
     * @return ActionOperationResult indicating success/failure with details
     */
    public static ActionOperationResult rightClickElement(By locator) {
        return rightClickElement(locator, "Right click on element");
    }
    
    /**
     * Right click (context click) on element using Actions class with custom description
     * 
     * @param locator By locator for the element
     * @param description Custom description for logging
     * @return ActionOperationResult indicating success/failure with details
     */
    public static ActionOperationResult rightClickElement(By locator, String description) {
        long startTime = System.currentTimeMillis();
        String methodName = "rightClickElement";
        
        log.info("[{}] Starting action - Description: {} | Locator: {}", methodName, description, locator);
        
        if (locator == null) {
            String errorMsg = "Element locator cannot be null";
            log.error("[{}] {}", methodName, errorMsg);
            return createFailureResult(errorMsg, startTime);
        }
        
        return executeActionOperation(() -> {
            WebElement element = waitForElementToBeClickable(locator);
            
            getActions()
                .contextClick(element)
                .perform();
            
            log.info("[{}] Successfully right-clicked element - Description: {} | Locator: {}", 
                    methodName, description, locator);
            return createSuccessResult("Element right-clicked successfully: " + description, startTime);
            
        }, methodName, description + " | Locator: " + locator, startTime);
    }
    
    /**
     * Hover over element using Actions class
     * 
     * @param locator By locator for the element
     * @return ActionOperationResult indicating success/failure with details
     */
    public static ActionOperationResult hoverOverElement(By locator) {
        return hoverOverElement(locator, "Hover over element");
    }
    
    /**
     * Hover over element using Actions class with custom description
     * 
     * @param locator By locator for the element
     * @param description Custom description for logging
     * @return ActionOperationResult indicating success/failure with details
     */
    public static ActionOperationResult hoverOverElement(By locator, String description) {
        long startTime = System.currentTimeMillis();
        String methodName = "hoverOverElement";
        
        log.info("[{}] Starting action - Description: {} | Locator: {}", methodName, description, locator);
        
        if (locator == null) {
            String errorMsg = "Element locator cannot be null";
            log.error("[{}] {}", methodName, errorMsg);
            return createFailureResult(errorMsg, startTime);
        }
        
        return executeActionOperation(() -> {
            WebElement element = waitForElementToBeVisible(locator);
            
            getActions()
                .moveToElement(element)
                .perform();
            
            // Wait for hover effect to take place
            int hoverWait = getHoverWaitTime();
            Thread.sleep(hoverWait);
            
            log.info("[{}] Successfully hovered over element - Description: {} | Locator: {}", 
                    methodName, description, locator);
            return createSuccessResult("Hovered over element successfully: " + description, startTime);
            
        }, methodName, description + " | Locator: " + locator, startTime);
    }
    
    /**
     * Drag and drop from source to target element
     * 
     * @param sourceLocator By locator for the source element
     * @param targetLocator By locator for the target element
     * @return ActionOperationResult indicating success/failure with details
     */
    public static ActionOperationResult dragAndDrop(By sourceLocator, By targetLocator) {
        return dragAndDrop(sourceLocator, targetLocator, "Drag and drop operation");
    }
    
    /**
     * Drag and drop from source to target element with custom description
     * 
     * @param sourceLocator By locator for the source element
     * @param targetLocator By locator for the target element
     * @param description Custom description for logging
     * @return ActionOperationResult indicating success/failure with details
     */
    public static ActionOperationResult dragAndDrop(By sourceLocator, By targetLocator, String description) {
        long startTime = System.currentTimeMillis();
        String methodName = "dragAndDrop";
        
        log.info("[{}] Starting action - Description: {} | Source: {} | Target: {}", 
                methodName, description, sourceLocator, targetLocator);
        
        if (sourceLocator == null || targetLocator == null) {
            String errorMsg = "Source and target locators cannot be null";
            log.error("[{}] {}", methodName, errorMsg);
            return createFailureResult(errorMsg, startTime);
        }
        
        return executeActionOperation(() -> {
            WebElement sourceElement = waitForElementToBeClickable(sourceLocator);
            WebElement targetElement = waitForElementToBeVisible(targetLocator);
            
            getActions()
                .dragAndDrop(sourceElement, targetElement)
                .perform();
            
            // Pause to allow drop action to complete
            int dragPause = getDragDropPause();
            Thread.sleep(dragPause);
            
            log.info("[{}] Successfully completed drag and drop - Description: {} | Source: {} | Target: {}", 
                    methodName, description, sourceLocator, targetLocator);
            return createSuccessResult("Drag and drop completed successfully: " + description, startTime);
            
        }, methodName, description + " | Source: " + sourceLocator + " | Target: " + targetLocator, startTime);
    }
    
    /**
     * Drag element by offset
     * 
     * @param locator By locator for the element
     * @param xOffset X offset in pixels
     * @param yOffset Y offset in pixels
     * @return ActionOperationResult indicating success/failure with details
     */
    public static ActionOperationResult dragElementByOffset(By locator, int xOffset, int yOffset) {
        return dragElementByOffset(locator, xOffset, yOffset, "Drag element by offset");
    }
    
    /**
     * Drag element by offset with custom description
     * 
     * @param locator By locator for the element
     * @param xOffset X offset in pixels
     * @param yOffset Y offset in pixels
     * @param description Custom description for logging
     * @return ActionOperationResult indicating success/failure with details
     */
    public static ActionOperationResult dragElementByOffset(By locator, int xOffset, int yOffset, String description) {
        long startTime = System.currentTimeMillis();
        String methodName = "dragElementByOffset";
        
        log.info("[{}] Starting action - Description: {} | Locator: {} | Offset: ({}, {})", 
                methodName, description, locator, xOffset, yOffset);
        
        if (locator == null) {
            String errorMsg = "Element locator cannot be null";
            log.error("[{}] {}", methodName, errorMsg);
            return createFailureResult(errorMsg, startTime);
        }
        
        return executeActionOperation(() -> {
            WebElement element = waitForElementToBeClickable(locator);
            
            getActions()
                .dragAndDropBy(element, xOffset, yOffset)
                .perform();
            
            log.info("[{}] Successfully dragged element by offset - Description: {} | Locator: {} | Offset: ({}, {})", 
                    methodName, description, locator, xOffset, yOffset);
            return createSuccessResult("Element dragged by offset successfully: " + description, startTime);
            
        }, methodName, description + " | Locator: " + locator + " | Offset: (" + xOffset + ", " + yOffset + ")", startTime);
    }
    
    /**
     * Send keys to element using Actions class
     * 
     * @param locator By locator for the element
     * @param keys Keys to send
     * @return ActionOperationResult indicating success/failure with details
     */
    public static ActionOperationResult sendKeysToElement(By locator, CharSequence... keys) {
        return sendKeysToElement(locator, "Send keys to element", keys);
    }
    
    /**
     * Send keys to element using Actions class with custom description
     * 
     * @param locator By locator for the element
     * @param description Custom description for logging
     * @param keys Keys to send
     * @return ActionOperationResult indicating success/failure with details
     */
    public static ActionOperationResult sendKeysToElement(By locator, String description, CharSequence... keys) {
        long startTime = System.currentTimeMillis();
        String methodName = "sendKeysToElement";
        
        log.info("[{}] Starting action - Description: {} | Locator: {} | Keys: {}", 
                methodName, description, locator, maskSensitiveData(keys));
        
        if (locator == null || keys == null || keys.length == 0) {
            String errorMsg = "Element locator and keys cannot be null or empty";
            log.error("[{}] {}", methodName, errorMsg);
            return createFailureResult(errorMsg, startTime);
        }
        
        return executeActionOperation(() -> {
            WebElement element = waitForElementToBeClickable(locator);
            
            getActions()
                .click(element)
                .sendKeys(keys)
                .perform();
            
            log.info("[{}] Successfully sent keys to element - Description: {} | Locator: {}", 
                    methodName, description, locator);
            return createSuccessResult("Keys sent to element successfully: " + description, startTime);
            
        }, methodName, description + " | Locator: " + locator, startTime);
    }
    
    /**
     * Perform key combination (e.g., Ctrl+C, Ctrl+V)
     * 
     * @param keys Key combination to perform
     * @return ActionOperationResult indicating success/failure with details
     */
    public static ActionOperationResult performKeyCombo(CharSequence... keys) {
        return performKeyCombo("Perform key combination", keys);
    }
    
    /**
     * Perform key combination with custom description
     * 
     * @param description Custom description for logging
     * @param keys Key combination to perform
     * @return ActionOperationResult indicating success/failure with details
     */
    public static ActionOperationResult performKeyCombo(String description, CharSequence... keys) {
        long startTime = System.currentTimeMillis();
        String methodName = "performKeyCombo";
        
        log.info("[{}] Starting action - Description: {} | Keys: {}", 
                methodName, description, maskSensitiveData(keys));
        
        if (keys == null || keys.length == 0) {
            String errorMsg = "Keys cannot be null or empty";
            log.error("[{}] {}", methodName, errorMsg);
            return createFailureResult(errorMsg, startTime);
        }
        
        return executeActionOperation(() -> {
            Actions actionChain = getActions();
            
            // Build key combination
            for (CharSequence key : keys) {
                actionChain = actionChain.keyDown(key);
            }
            
            // Release keys in reverse order
            for (int i = keys.length - 1; i >= 0; i--) {
                actionChain = actionChain.keyUp(keys[i]);
            }
            
            actionChain.perform();
            
            log.info("[{}] Successfully performed key combination - Description: {}", methodName, description);
            return createSuccessResult("Key combination performed successfully: " + description, startTime);
            
        }, methodName, description, startTime);
    }
    
    /**
     * Move to element and click at specific offset
     * 
     * @param locator By locator for the element
     * @param xOffset X offset from element center
     * @param yOffset Y offset from element center
     * @return ActionOperationResult indicating success/failure with details
     */
    public static ActionOperationResult clickAtOffset(By locator, int xOffset, int yOffset) {
        return clickAtOffset(locator, xOffset, yOffset, "Click at offset");
    }
    
    /**
     * Move to element and click at specific offset with custom description
     * 
     * @param locator By locator for the element
     * @param xOffset X offset from element center
     * @param yOffset Y offset from element center
     * @param description Custom description for logging
     * @return ActionOperationResult indicating success/failure with details
     */
    public static ActionOperationResult clickAtOffset(By locator, int xOffset, int yOffset, String description) {
        long startTime = System.currentTimeMillis();
        String methodName = "clickAtOffset";
        
        log.info("[{}] Starting action - Description: {} | Locator: {} | Offset: ({}, {})", 
                methodName, description, locator, xOffset, yOffset);
        
        if (locator == null) {
            String errorMsg = "Element locator cannot be null";
            log.error("[{}] {}", methodName, errorMsg);
            return createFailureResult(errorMsg, startTime);
        }
        
        return executeActionOperation(() -> {
            WebElement element = waitForElementToBeClickable(locator);
            
            getActions()
                .moveToElement(element, xOffset, yOffset)
                .click()
                .perform();
            
            log.info("[{}] Successfully clicked at offset - Description: {} | Locator: {} | Offset: ({}, {})", 
                    methodName, description, locator, xOffset, yOffset);
            return createSuccessResult("Clicked at offset successfully: " + description, startTime);
            
        }, methodName, description + " | Locator: " + locator + " | Offset: (" + xOffset + ", " + yOffset + ")", startTime);
    }
    
    /**
     * Scroll to element using Actions class
     * 
     * @param locator By locator for the element
     * @return ActionOperationResult indicating success/failure with details
     */
    public static ActionOperationResult scrollToElement(By locator) {
        return scrollToElement(locator, "Scroll to element");
    }
    
    /**
     * Scroll to element using Actions class with custom description
     * 
     * @param locator By locator for the element
     * @param description Custom description for logging
     * @return ActionOperationResult indicating success/failure with details
     */
    public static ActionOperationResult scrollToElement(By locator, String description) {
        long startTime = System.currentTimeMillis();
        String methodName = "scrollToElement";
        
        log.info("[{}] Starting action - Description: {} | Locator: {}", methodName, description, locator);
        
        if (locator == null) {
            String errorMsg = "Element locator cannot be null";
            log.error("[{}] {}", methodName, errorMsg);
            return createFailureResult(errorMsg, startTime);
        }
        
        return executeActionOperation(() -> {
            WebElement element = waitForElementToBeVisible(locator);
            
            getActions()
                .scrollToElement(element)
                .perform();
            
            log.info("[{}] Successfully scrolled to element - Description: {} | Locator: {}", 
                    methodName, description, locator);
            return createSuccessResult("Scrolled to element successfully: " + description, startTime);
            
        }, methodName, description + " | Locator: " + locator, startTime);
    }
    
    /**
     * Execute action operation with retry logic and error handling
     */
    private static ActionOperationResult executeActionOperation(
            ActionOperation operation, String methodName, String context, long startTime) {
        
        int retryCount = getRetryCount();
        Exception lastException = null;
        
        for (int attempt = 1; attempt <= retryCount; attempt++) {
            try {
                log.debug("[{}] Attempt {} of {} - Context: {}", methodName, attempt, retryCount, context);
                
                return operation.execute();
                
            } catch (TimeoutException e) {
                lastException = e;
                String errorMsg = String.format("Timeout waiting for element - Attempt %d/%d", attempt, retryCount);
                log.warn("[{}] {} - Context: {}", methodName, errorMsg, context);
                
                if (attempt < retryCount) {
                    waitBetweenRetries();
                }
                
            } catch (ElementNotInteractableException e) {
                lastException = e;
                String errorMsg = String.format("Element not interactable - Attempt %d/%d", attempt, retryCount);
                log.warn("[{}] {} - Context: {}", methodName, errorMsg, context);
                
                if (attempt < retryCount) {
                    waitBetweenRetries();
                }
                
            } catch (StaleElementReferenceException e) {
                lastException = e;
                String errorMsg = String.format("Stale element reference - Attempt %d/%d", attempt, retryCount);
                log.warn("[{}] {} - Context: {}", methodName, errorMsg, context);
                
                if (attempt < retryCount) {
                    waitBetweenRetries();
                }
                
            } catch (NoSuchElementException e) {
                lastException = e;
                String errorMsg = "Element not found";
                log.error("[{}] {} - Context: {}", methodName, errorMsg, context);
                break; // Don't retry for element not found
                
            } catch (Exception e) {
                lastException = e;
                String errorMsg = String.format("Unexpected error in action operation - Attempt %d/%d", attempt, retryCount);
                log.error("[{}] {} - Context: {} - Error: {}", methodName, errorMsg, context, e.getMessage(), e);
                
                if (attempt < retryCount) {
                    waitBetweenRetries();
                }
            }
        }
        
        // All attempts failed
        String finalError = String.format("Action operation failed after %d attempts: %s", 
                retryCount, lastException.getMessage());
        log.error("[{}] {} - Context: {}", methodName, finalError, context);
        
        if (shouldTakeScreenshot()) {
            takeScreenshot(methodName);
        }
        
        return createFailureResult(finalError, startTime);
    }
    
    /**
     * Wait for element to be clickable
     */
    private static WebElement waitForElementToBeClickable(By locator) {
        WebDriverWait wait = createWebDriverWait();
        return wait.until(ExpectedConditions.elementToBeClickable(locator));
    }
    
    /**
     * Wait for element to be visible
     */
    private static WebElement waitForElementToBeVisible(By locator) {
        WebDriverWait wait = createWebDriverWait();
        return wait.until(ExpectedConditions.visibilityOfElementLocated(locator));
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
        return Integer.parseInt(config.getProperty("actions.timeout", String.valueOf(DEFAULT_TIMEOUT)));
    }
    
    /**
     * Get retry count from configuration
     */
    private static int getRetryCount() {
        return Integer.parseInt(config.getProperty("actions.retry.count", String.valueOf(DEFAULT_RETRY_COUNT)));
    }
    
    /**
     * Get hover wait time from configuration
     */
    private static int getHoverWaitTime() {
        return Integer.parseInt(config.getProperty("actions.hover.wait", String.valueOf(HOVER_WAIT_TIME)));
    }
    
    /**
     * Get drag drop pause time from configuration
     */
    private static int getDragDropPause() {
        return Integer.parseInt(config.getProperty("actions.drag.pause", String.valueOf(DRAG_DROP_PAUSE)));
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
            Thread.sleep(500);
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
                
                // Integration with reporting framework
                log.info("Screenshot captured: {}", screenshotName);
            }
        } catch (Exception e) {
            log.warn("Failed to capture screenshot: {}", e.getMessage());
        }
    }
    
    /**
     * Mask sensitive data in logs (passwords, etc.)
     */
    private static String maskSensitiveData(CharSequence... keys) {
        if (keys == null || keys.length == 0) {
            return "[]";
        }
        
        StringBuilder sb = new StringBuilder("[");
        for (int i = 0; i < keys.length; i++) {
            if (i > 0) sb.append(", ");
            
            String keyStr = keys[i].toString();
            // Mask if it looks like sensitive data
            if (keyStr.length() > 4 && (keyStr.matches(".*[a-zA-Z].*[0-9].*") || keyStr.contains("password"))) {
                sb.append("***MASKED***");
            } else {
                sb.append(keyStr);
            }
        }
        sb.append("]");
        return sb.toString();
    }
    
    /**
     * Create success result
     */
    private static ActionOperationResult createSuccessResult(String message, long startTime) {
        return createSuccessResult(message, startTime, null);
    }
    
    /**
     * Create success result with additional data
     */
    private static ActionOperationResult createSuccessResult(String message, long startTime, String data) {
        long executionTime = System.currentTimeMillis() - startTime;
        log.info("Action operation completed successfully in {}ms: {}", executionTime, message);
        
        return new ActionOperationResult(true, message, executionTime, data);
    }
    
    /**
     * Create failure result
     */
    private static ActionOperationResult createFailureResult(String errorMessage, long startTime) {
        long executionTime = System.currentTimeMillis() - startTime;
        log.error("Action operation failed after {}ms: {}", executionTime, errorMessage);
        
        return new ActionOperationResult(false, errorMessage, executionTime, null);
    }
    
    /**
     * Functional interface for action operations
     */
    @FunctionalInterface
    private interface ActionOperation {
        ActionOperationResult execute() throws Exception;
    }
    
    /**
     * Result class for action operations
     */
    public static class ActionOperationResult {
        private final boolean success;
        private final String message;
        private final long executionTimeMs;
        private final String data;
        
        public ActionOperationResult(boolean success, String message, long executionTimeMs, String data) {
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
            return String.format("ActionOperationResult{success=%s, message='%s', executionTime=%dms, data='%s'}", 
                    success, message, executionTimeMs, data);
        }
    }
    
    /**
     * Custom exception for action operations
     */
    public static class CustomActionsException extends RuntimeException {
        public CustomActionsException(String message) {
            super(message);
        }
        
        public CustomActionsException(String message, Throwable cause) {
            super(message, cause);
        }
    }
    
    /**
     * Clean up ThreadLocal to prevent memory leaks
     */
    public static void cleanup() {
        driver.remove();
        actions.remove();
        log.debug("WebDriver and Actions ThreadLocal cleaned up for thread: {}", Thread.currentThread().getName());
    }
}