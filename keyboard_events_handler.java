package com.enterprise.selenium.keywords;

import org.openqa.selenium.*;
import org.openqa.selenium.interactions.Actions;
import org.openqa.selenium.support.ui.WebDriverWait;
import org.openqa.selenium.support.ui.ExpectedConditions;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import java.time.Duration;
import java.util.Properties;
import java.io.InputStream;
import java.io.IOException;
import java.util.Arrays;
import java.util.List;
import java.util.Map;
import java.util.HashMap;

/**
 * Enterprise-level Selenium keyword for keyboard events handling
 * Supports sendKeys, special keys (ENTER, TAB, ESC), key combinations, and complex keyboard actions
 * 
 * @author Enterprise Test Team
 * @version 1.0
 */
public class KeyboardEventsHandler {
    
    private static final Logger log = LoggerFactory.getLogger(KeyboardEventsHandler.class);
    private static final ThreadLocal<WebDriver> driverThreadLocal = new ThreadLocal<>();
    private static Properties config;
    
    // Configuration constants
    private static final int DEFAULT_TIMEOUT = 10;
    private static final int DEFAULT_RETRY_COUNT = 3;
    private static final int RETRY_INTERVAL = 1000; // milliseconds
    private static final int TYPING_DELAY_MS = 50; // For human-like typing
    
    // Special keys mapping for better usability
    private static final Map<String, Keys> SPECIAL_KEYS_MAP = new HashMap<>();
    
    static {
        loadConfiguration();
        initializeSpecialKeysMap();
    }
    
    /**
     * Keyboard action types for different scenarios
     */
    public enum KeyboardActionType {
        SEND_TEXT,
        SEND_SPECIAL_KEY,
        SEND_KEY_COMBINATION,
        CLEAR_AND_TYPE,
        APPEND_TEXT,
        HUMAN_LIKE_TYPING
    }
    
    /**
     * Response object for keyboard operations
     */
    public static class KeyboardActionResponse {
        private final boolean isSuccess;
        private final String message;
        private final long executionTimeMs;
        private final KeyboardActionType actionType;
        private final String inputData;
        private final By locator;
        
        public KeyboardActionResponse(boolean isSuccess, String message, long executionTimeMs, 
                                    KeyboardActionType actionType, String inputData, By locator) {
            this.isSuccess = isSuccess;
            this.message = message;
            this.executionTimeMs = executionTimeMs;
            this.actionType = actionType;
            this.inputData = inputData;
            this.locator = locator;
        }
        
        // Getters
        public boolean isSuccess() { return isSuccess; }
        public String getMessage() { return message; }
        public long getExecutionTimeMs() { return executionTimeMs; }
        public KeyboardActionType getActionType() { return actionType; }
        public String getInputData() { return inputData; }
        public By getLocator() { return locator; }
        
        @Override
        public String toString() {
            return String.format("KeyboardAction[success=%s, type=%s, time=%dms, data='%s'] - %s",
                    isSuccess, actionType, executionTimeMs, inputData, message);
        }
    }
    
    /**
     * Custom exception for keyboard operation failures
     */
    public static class KeyboardActionException extends Exception {
        private final KeyboardActionType actionType;
        private final By locator;
        
        public KeyboardActionException(String message, KeyboardActionType actionType, By locator) {
            super(message);
            this.actionType = actionType;
            this.locator = locator;
        }
        
        public KeyboardActionException(String message, Throwable cause, KeyboardActionType actionType, By locator) {
            super(message, cause);
            this.actionType = actionType;
            this.locator = locator;
        }
        
        public KeyboardActionType getActionType() { return actionType; }
        public By getLocator() { return locator; }
    }
    
    /**
     * Load configuration from properties file
     */
    private static void loadConfiguration() {
        config = new Properties();
        try (InputStream input = KeyboardEventsHandler.class.getClassLoader()
                .getResourceAsStream("selenium-config.properties")) {
            if (input != null) {
                config.load(input);
                log.info("Keyboard events configuration loaded successfully");
            } else {
                log.warn("selenium-config.properties not found, using default values");
            }
        } catch (IOException e) {
            log.error("Error loading keyboard events configuration: {}", e.getMessage());
        }
    }
    
    /**
     * Initialize special keys mapping for easier access
     */
    private static void initializeSpecialKeysMap() {
        SPECIAL_KEYS_MAP.put("ENTER", Keys.ENTER);
        SPECIAL_KEYS_MAP.put("RETURN", Keys.RETURN);
        SPECIAL_KEYS_MAP.put("TAB", Keys.TAB);
        SPECIAL_KEYS_MAP.put("ESCAPE", Keys.ESCAPE);
        SPECIAL_KEYS_MAP.put("ESC", Keys.ESCAPE);
        SPECIAL_KEYS_MAP.put("SPACE", Keys.SPACE);
        SPECIAL_KEYS_MAP.put("BACKSPACE", Keys.BACK_SPACE);
        SPECIAL_KEYS_MAP.put("DELETE", Keys.DELETE);
        SPECIAL_KEYS_MAP.put("HOME", Keys.HOME);
        SPECIAL_KEYS_MAP.put("END", Keys.END);
        SPECIAL_KEYS_MAP.put("PAGE_UP", Keys.PAGE_UP);
        SPECIAL_KEYS_MAP.put("PAGE_DOWN", Keys.PAGE_DOWN);
        SPECIAL_KEYS_MAP.put("ARROW_UP", Keys.ARROW_UP);
        SPECIAL_KEYS_MAP.put("ARROW_DOWN", Keys.ARROW_DOWN);
        SPECIAL_KEYS_MAP.put("ARROW_LEFT", Keys.ARROW_LEFT);
        SPECIAL_KEYS_MAP.put("ARROW_RIGHT", Keys.ARROW_RIGHT);
        SPECIAL_KEYS_MAP.put("F1", Keys.F1);
        SPECIAL_KEYS_MAP.put("F2", Keys.F2);
        SPECIAL_KEYS_MAP.put("F3", Keys.F3);
        SPECIAL_KEYS_MAP.put("F4", Keys.F4);
        SPECIAL_KEYS_MAP.put("F5", Keys.F5);
        SPECIAL_KEYS_MAP.put("F6", Keys.F6);
        SPECIAL_KEYS_MAP.put("F7", Keys.F7);
        SPECIAL_KEYS_MAP.put("F8", Keys.F8);
        SPECIAL_KEYS_MAP.put("F9", Keys.F9);
        SPECIAL_KEYS_MAP.put("F10", Keys.F10);
        SPECIAL_KEYS_MAP.put("F11", Keys.F11);
        SPECIAL_KEYS_MAP.put("F12", Keys.F12);
        SPECIAL_KEYS_MAP.put("SHIFT", Keys.SHIFT);
        SPECIAL_KEYS_MAP.put("CONTROL", Keys.CONTROL);
        SPECIAL_KEYS_MAP.put("CTRL", Keys.CONTROL);
        SPECIAL_KEYS_MAP.put("ALT", Keys.ALT);
        SPECIAL_KEYS_MAP.put("CMD", Keys.COMMAND);
        SPECIAL_KEYS_MAP.put("META", Keys.META);
    }
    
    /**
     * Set WebDriver for current thread
     */
    public static void setDriver(WebDriver driver) {
        driverThreadLocal.set(driver);
        log.debug("WebDriver set for keyboard events in thread: {}", Thread.currentThread().getName());
    }
    
    /**
     * Get WebDriver for current thread
     */
    private static WebDriver getDriver() {
        WebDriver driver = driverThreadLocal.get();
        if (driver == null) {
            throw new IllegalStateException("WebDriver not initialized for keyboard events in current thread");
        }
        return driver;
    }
    
    /**
     * Send text to element with comprehensive validation and retry logic
     * 
     * @param locator Element locator
     * @param text Text to send
     * @param clearFirst Whether to clear the field before typing
     * @param timeoutSeconds Explicit wait timeout
     * @return KeyboardActionResponse with operation results
     * @throws KeyboardActionException if operation fails critically
     */
    public static KeyboardActionResponse sendText(By locator, String text, boolean clearFirst, 
                                                Integer timeoutSeconds) throws KeyboardActionException {
        
        long startTime = System.currentTimeMillis();
        String methodName = "sendText";
        KeyboardActionType actionType = clearFirst ? KeyboardActionType.CLEAR_AND_TYPE : KeyboardActionType.SEND_TEXT;
        
        // Input validation - fail fast
        if (locator == null) {
            log.error("{}: Locator cannot be null", methodName);
            throw new KeyboardActionException("Locator parameter is null", actionType, null);
        }
        
        if (text == null) {
            log.error("{}: Text cannot be null for locator: {}", methodName, locator.toString());
            throw new KeyboardActionException("Text parameter is null", actionType, locator);
        }
        
        // Get configuration values
        int timeout = timeoutSeconds != null ? timeoutSeconds : 
                     Integer.parseInt(config.getProperty("element.timeout", String.valueOf(DEFAULT_TIMEOUT)));
        
        int retryCount = Integer.parseInt(config.getProperty("retry.count", String.valueOf(DEFAULT_RETRY_COUNT)));
        
        log.info("{}: Sending text to locator: {} | Text length: {} | Clear first: {} | Timeout: {}s", 
                methodName, locator.toString(), text.length(), clearFirst, timeout);
        
        // Retry logic with exponential backoff
        Exception lastException = null;
        for (int attempt = 1; attempt <= retryCount; attempt++) {
            try {
                log.debug("{}: Attempt {} of {} for text input", methodName, attempt, retryCount);
                
                WebDriverWait wait = new WebDriverWait(getDriver(), Duration.ofSeconds(timeout));
                
                // Wait for element to be present and visible
                WebElement element = wait.until(ExpectedConditions.elementToBeClickable(locator));
                
                if (element == null) {
                    throw new KeyboardActionException("Element not found after waiting", actionType, locator);
                }
                
                // Focus on element first
                focusElement(element, locator);
                
                // Clear field if requested
                if (clearFirst) {
                    clearElementSafely(element, locator);
                }
                
                // Send the text
                element.sendKeys(text);
                
                // Verify text was entered (optional verification)
                if (Boolean.parseBoolean(config.getProperty("keyboard.verify.input", "true"))) {
                    verifyTextInput(element, text, clearFirst);
                }
                
                long executionTime = System.currentTimeMillis() - startTime;
                String successMessage = String.format("Text sent successfully on attempt %d", attempt);
                
                log.info("{}: SUCCESS - Text sent to element | Length: {} | Time: {}ms", 
                        methodName, text.length(), executionTime);
                
                return new KeyboardActionResponse(true, successMessage, executionTime, actionType, text, locator);
                
            } catch (TimeoutException e) {
                lastException = e;
                log.warn("{}: Timeout on attempt {} - Element not ready for text input within {}s", 
                        methodName, attempt, timeout);
                
                if (attempt < retryCount) {
                    performRetryDelay(attempt);
                }
                
            } catch (StaleElementReferenceException e) {
                lastException = e;
                log.warn("{}: Stale element reference on attempt {} - Element refreshed", methodName, attempt);
                
                if (attempt < retryCount) {
                    performRetryDelay(attempt);
                }
                
            } catch (WebDriverException e) {
                lastException = e;
                log.error("{}: WebDriver exception on attempt {}: {}", methodName, attempt, e.getMessage());
                
                // Take screenshot on failure
                takeScreenshotOnFailure(methodName, locator.toString(), actionType);
                
                if (attempt < retryCount) {
                    performRetryDelay(attempt);
                } else {
                    throw new KeyboardActionException(
                            String.format("Text input failed after %d attempts for locator: %s", 
                                    retryCount, locator.toString()), e, actionType, locator);
                }
            }
        }
        
        // All retries exhausted
        long executionTime = System.currentTimeMillis() - startTime;
        String errorMessage = String.format("Text input failed after %d attempts for locator: %s", 
                                           retryCount, locator.toString());
        
        log.error("{}: FAILED - {} | Time: {}ms", methodName, errorMessage, executionTime);
        takeScreenshotOnFailure(methodName, locator.toString(), actionType);
        
        return new KeyboardActionResponse(false, errorMessage, executionTime, actionType, text, locator);
    }
    
    /**
     * Send special key (ENTER, TAB, ESC, etc.) to element
     * 
     * @param locator Element locator
     * @param specialKey Special key name or Keys enum
     * @param timeoutSeconds Explicit wait timeout
     * @return KeyboardActionResponse with operation results
     */
    public static KeyboardActionResponse sendSpecialKey(By locator, Object specialKey, 
                                                      Integer timeoutSeconds) throws KeyboardActionException {
        
        long startTime = System.currentTimeMillis();
        String methodName = "sendSpecialKey";
        KeyboardActionType actionType = KeyboardActionType.SEND_SPECIAL_KEY;
        
        // Input validation
        if (locator == null) {
            throw new KeyboardActionException("Locator parameter is null", actionType, null);
        }
        
        if (specialKey == null) {
            throw new KeyboardActionException("Special key parameter is null", actionType, locator);
        }
        
        // Convert special key to Keys enum
        Keys keyToSend = convertToKeys(specialKey);
        String keyName = keyToSend.toString();
        
        int timeout = timeoutSeconds != null ? timeoutSeconds : 
                     Integer.parseInt(config.getProperty("element.timeout", String.valueOf(DEFAULT_TIMEOUT)));
        
        int retryCount = Integer.parseInt(config.getProperty("retry.count", String.valueOf(DEFAULT_RETRY_COUNT)));
        
        log.info("{}: Sending special key '{}' to locator: {} | Timeout: {}s", 
                methodName, keyName, locator.toString(), timeout);
        
        // Retry logic
        Exception lastException = null;
        for (int attempt = 1; attempt <= retryCount; attempt++) {
            try {
                log.debug("{}: Attempt {} of {} for special key: {}", methodName, attempt, retryCount, keyName);
                
                WebDriverWait wait = new WebDriverWait(getDriver(), Duration.ofSeconds(timeout));
                WebElement element = wait.until(ExpectedConditions.elementToBeClickable(locator));
                
                // Focus on element
                focusElement(element, locator);
                
                // Send special key
                element.sendKeys(keyToSend);
                
                long executionTime = System.currentTimeMillis() - startTime;
                String successMessage = String.format("Special key '%s' sent successfully on attempt %d", 
                                                     keyName, attempt);
                
                log.info("{}: SUCCESS - Special key '{}' sent | Time: {}ms", methodName, keyName, executionTime);
                
                return new KeyboardActionResponse(true, successMessage, executionTime, 
                                                actionType, keyName, locator);
                
            } catch (Exception e) {
                lastException = e;
                log.warn("{}: Exception on attempt {} for special key '{}': {}", 
                        methodName, attempt, keyName, e.getMessage());
                
                if (attempt < retryCount) {
                    performRetryDelay(attempt);
                }
            }
        }
        
        // All retries failed
        long executionTime = System.currentTimeMillis() - startTime;
        String errorMessage = String.format("Special key '%s' sending failed after %d attempts", 
                                           keyName, retryCount);
        
        log.error("{}: FAILED - {} | Time: {}ms", methodName, errorMessage, executionTime);
        takeScreenshotOnFailure(methodName, locator.toString(), actionType);
        
        return new KeyboardActionResponse(false, errorMessage, executionTime, actionType, keyName, locator);
    }
    
    /**
     * Send key combination (Ctrl+A, Ctrl+C, Alt+F4, etc.)
     * 
     * @param locator Element locator (can be null for global shortcuts)
     * @param modifierKeys Modifier keys (CTRL, ALT, SHIFT)
     * @param key Main key
     * @param timeoutSeconds Timeout for element operations
     * @return KeyboardActionResponse with operation results
     */
    public static KeyboardActionResponse sendKeyCombo(By locator, List<Keys> modifierKeys, Keys key, 
                                                    Integer timeoutSeconds) throws KeyboardActionException {
        
        long startTime = System.currentTimeMillis();
        String methodName = "sendKeyCombo";
        KeyboardActionType actionType = KeyboardActionType.SEND_KEY_COMBINATION;
        
        // Input validation
        if (modifierKeys == null || modifierKeys.isEmpty()) {
            throw new KeyboardActionException("Modifier keys cannot be null or empty", actionType, locator);
        }
        
        if (key == null) {
            throw new KeyboardActionException("Main key cannot be null", actionType, locator);
        }
        
        String comboDescription = buildKeyComboDescription(modifierKeys, key);
        
        int timeout = timeoutSeconds != null ? timeoutSeconds : 
                     Integer.parseInt(config.getProperty("element.timeout", String.valueOf(DEFAULT_TIMEOUT)));
        
        log.info("{}: Sending key combination '{}' to locator: {} | Timeout: {}s", 
                methodName, comboDescription, locator != null ? locator.toString() : "GLOBAL", timeout);
        
        try {
            WebDriver driver = getDriver();
            Actions actions = new Actions(driver);
            
            if (locator != null) {
                // Target specific element
                WebDriverWait wait = new WebDriverWait(driver, Duration.ofSeconds(timeout));
                WebElement element = wait.until(ExpectedConditions.elementToBeClickable(locator));
                focusElement(element, locator);
                
                // Build key combination for specific element
                Actions actionChain = actions;
                for (Keys modifier : modifierKeys) {
                    actionChain = actionChain.keyDown(modifier);
                }
                actionChain = actionChain.sendKeys(element, key);
                for (Keys modifier : modifierKeys) {
                    actionChain = actionChain.keyUp(modifier);
                }
                actionChain.perform();
                
            } else {
                // Global key combination
                Actions actionChain = actions;
                for (Keys modifier : modifierKeys) {
                    actionChain = actionChain.keyDown(modifier);
                }
                actionChain = actionChain.sendKeys(key);
                for (Keys modifier : modifierKeys) {
                    actionChain = actionChain.keyUp(modifier);
                }
                actionChain.perform();
            }
            
            long executionTime = System.currentTimeMillis() - startTime;
            String successMessage = String.format("Key combination '%s' sent successfully", comboDescription);
            
            log.info("{}: SUCCESS - Key combination '{}' sent | Time: {}ms", 
                    methodName, comboDescription, executionTime);
            
            return new KeyboardActionResponse(true, successMessage, executionTime, 
                                            actionType, comboDescription, locator);
            
        } catch (Exception e) {
            long executionTime = System.currentTimeMillis() - startTime;
            String errorMessage = String.format("Key combination '%s' failed: %s", comboDescription, e.getMessage());
            
            log.error("{}: FAILED - {} | Time: {}ms", methodName, errorMessage, executionTime);
            takeScreenshotOnFailure(methodName, locator != null ? locator.toString() : "GLOBAL", actionType);
            
            throw new KeyboardActionException(errorMessage, e, actionType, locator);
        }
    }
    
    /**
     * Human-like typing with natural delays between keystrokes
     * 
     * @param locator Element locator
     * @param text Text to type
     * @param clearFirst Whether to clear field first
     * @param minDelayMs Minimum delay between keystrokes
     * @param maxDelayMs Maximum delay between keystrokes
     * @param timeoutSeconds Element timeout
     * @return KeyboardActionResponse with operation results
     */
    public static KeyboardActionResponse typeHumanLike(By locator, String text, boolean clearFirst,
                                                      int minDelayMs, int maxDelayMs, 
                                                      Integer timeoutSeconds) throws KeyboardActionException {
        
        long startTime = System.currentTimeMillis();
        String methodName = "typeHumanLike";
        KeyboardActionType actionType = KeyboardActionType.HUMAN_LIKE_TYPING;
        
        // Input validation
        if (locator == null) {
            throw new KeyboardActionException("Locator parameter is null", actionType, null);
        }
        
        if (text == null || text.isEmpty()) {
            throw new KeyboardActionException("Text cannot be null or empty", actionType, locator);
        }
        
        if (minDelayMs < 0 || maxDelayMs < minDelayMs) {
            throw new KeyboardActionException("Invalid delay parameters", actionType, locator);
        }
        
        int timeout = timeoutSeconds != null ? timeoutSeconds : 
                     Integer.parseInt(config.getProperty("element.timeout", String.valueOf(DEFAULT_TIMEOUT)));
        
        log.info("{}: Starting human-like typing for locator: {} | Text length: {} | Delay: {}-{}ms", 
                methodName, locator.toString(), text.length(), minDelayMs, maxDelayMs);
        
        try {
            WebDriverWait wait = new WebDriverWait(getDriver(), Duration.ofSeconds(timeout));
            WebElement element = wait.until(ExpectedConditions.elementToBeClickable(locator));
            
            // Focus on element
            focusElement(element, locator);
            
            // Clear field if requested
            if (clearFirst) {
                clearElementSafely(element, locator);
            }
            
            // Type each character with human-like delay
            for (int i = 0; i < text.length(); i++) {
                char currentChar = text.charAt(i);
                element.sendKeys(String.valueOf(currentChar));
                
                // Add random delay between keystrokes (except for last character)
                if (i < text.length() - 1) {
                    int delay = minDelayMs + (int) (Math.random() * (maxDelayMs - minDelayMs));
                    Thread.sleep(delay);
                }
                
                log.debug("{}: Typed character '{}' ({}/{})", methodName, currentChar, i + 1, text.length());
            }
            
            long executionTime = System.currentTimeMillis() - startTime;
            String successMessage = String.format("Human-like typing completed successfully for %d characters", 
                                                 text.length());
            
            log.info("{}: SUCCESS - Human-like typing completed | Characters: {} | Time: {}ms", 
                    methodName, text.length(), executionTime);
            
            return new KeyboardActionResponse(true, successMessage, executionTime, actionType, text, locator);
            
        } catch (InterruptedException e) {
            Thread.currentThread().interrupt();
            throw new KeyboardActionException("Human-like typing interrupted", e, actionType, locator);
            
        } catch (Exception e) {
            long executionTime = System.currentTimeMillis() - startTime;
            String errorMessage = String.format("Human-like typing failed: %s", e.getMessage());
            
            log.error("{}: FAILED - {} | Time: {}ms", methodName, errorMessage, executionTime);
            takeScreenshotOnFailure(methodName, locator.toString(), actionType);
            
            throw new KeyboardActionException(errorMessage, e, actionType, locator);
        }
    }
    
    /**
     * Overloaded methods for easier usage
     */
    public static KeyboardActionResponse sendText(By locator, String text) throws KeyboardActionException {
        return sendText(locator, text, false, null);
    }
    
    public static KeyboardActionResponse sendTextAndClear(By locator, String text) throws KeyboardActionException {
        return sendText(locator, text, true, null);
    }
    
    public static KeyboardActionResponse pressEnter(By locator) throws KeyboardActionException {
        return sendSpecialKey(locator, "ENTER", null);
    }
    
    public static KeyboardActionResponse pressTab(By locator) throws KeyboardActionException {
        return sendSpecialKey(locator, "TAB", null);
    }
    
    public static KeyboardActionResponse pressEscape(By locator) throws KeyboardActionException {
        return sendSpecialKey(locator, "ESCAPE", null);
    }
    
    public static KeyboardActionResponse selectAllText(By locator) throws KeyboardActionException {
        return sendKeyCombo(locator, Arrays.asList(Keys.CONTROL), Keys.chord("a"), null);
    }
    
    public static KeyboardActionResponse copyText(By locator) throws KeyboardActionException {
        return sendKeyCombo(locator, Arrays.asList(Keys.CONTROL), Keys.chord("c"), null);
    }
    
    public static KeyboardActionResponse pasteText(By locator) throws KeyboardActionException {
        return sendKeyCombo(locator, Arrays.asList(Keys.CONTROL), Keys.chord("v"), null);
    }
    
    public static KeyboardActionResponse typeHumanLike(By locator, String text) throws KeyboardActionException {
        return typeHumanLike(locator, text, false, 50, 150, null);
    }
    
    /**
     * Helper methods
     */
    private static void focusElement(WebElement element, By locator) {
        try {
            // Try clicking to focus
            element.click();
            log.debug("Element focused by clicking: {}", locator.toString());
        } catch (Exception e) {
            try {
                // Fallback to JavaScript focus
                JavascriptExecutor js = (JavascriptExecutor) getDriver();
                js.executeScript("arguments[0].focus();", element);
                log.debug("Element focused by JavaScript: {}", locator.toString());
            } catch (Exception jsException) {
                log.warn("Could not focus element: {} - {}", locator.toString(), jsException.getMessage());
            }
        }
    }
    
    private static void clearElementSafely(WebElement element, By locator) {
        try {
            element.clear();
            log.debug("Element cleared successfully: {}", locator.toString());
        } catch (Exception e) {
            try {
                // Fallback: Select all and delete
                element.sendKeys(Keys.CONTROL + "a");
                element.sendKeys(Keys.DELETE);
                log.debug("Element cleared using select-all + delete: {}", locator.toString());
            } catch (Exception fallbackException) {
                log.warn("Could not clear element: {} - {}", locator.toString(), fallbackException.getMessage());
            }
        }
    }
    
    private static void verifyTextInput(WebElement element, String expectedText, boolean wasCleared) {
        try {
            String actualValue = element.getAttribute("value");
            if (actualValue == null) {
                actualValue = element.getText();
            }
            
            boolean isMatch = wasCleared ? expectedText.equals(actualValue) : 
                            actualValue != null && actualValue.contains(expectedText);
            
            if (!isMatch) {
                log.warn("Text verification failed - Expected: '{}', Actual: '{}', WasCleared: {}", 
                        expectedText, actualValue, wasCleared);
            } else {
                log.debug("Text verification passed - Text matches expected value");
            }
        } catch (Exception e) {
            log.warn("Could not verify text input: {}", e.getMessage());
        }
    }
    
    private static Keys convertToKeys(Object keyInput) throws KeyboardActionException {
        if (keyInput instanceof Keys) {
            return (Keys) keyInput;
        } else if (keyInput instanceof String) {
            String keyString = ((String) keyInput).toUpperCase();
            Keys mappedKey = SPECIAL_KEYS_MAP.get(keyString);
            if (mappedKey != null) {
                return mappedKey;
            } else {
                throw new KeyboardActionException("Unknown special key: " + keyString, 
                                                KeyboardActionType.SEND_SPECIAL_KEY, null);
            }
        } else {
            throw new KeyboardActionException("Invalid key input type: " + keyInput.getClass().getSimpleName(), 
                                            KeyboardActionType.SEND_SPECIAL_KEY, null);
        }
    }
    
    private static String buildKeyComboDescription(List<Keys> modifierKeys, Keys key) {
        StringBuilder combo = new StringBuilder();
        for (int i = 0; i < modifierKeys.size(); i++) {
            combo.append(modifierKeys.get(i).toString());
            if (i < modifierKeys.size() - 1) {
                combo.append("+");
            }
        }
        combo.append("+").append(key.toString());
        return combo.toString();
    }
    
    private static void performRetryDelay(int attempt) {
        try {
            Thread.sleep(RETRY_INTERVAL * attempt); // Exponential backoff
        } catch (InterruptedException ie) {
            Thread.currentThread().interrupt();
            throw new RuntimeException("Thread interrupted during retry delay", ie);
        }
    }
    
    private static void takeScreenshotOnFailure(String methodName, String locatorInfo, KeyboardActionType actionType) {
        try {
            WebDriver driver = getDriver();
            if (driver instanceof TakesScreenshot) {
                byte[] screenshot = ((TakesScreenshot) driver).getScreenshotAs(OutputType.BYTES);
                log.info("{}: Screenshot captured for failed keyboard action: {} | Type: {}", 
                        methodName, locatorInfo, actionType);
                
                // Here integrate with reporting framework
                // ExtentTestManager.getTest().addScreenCaptureFromBase64String(Base64.getEncoder().encodeToString(screenshot));
                
            }
        } catch (Exception e) {
            log.error("{}: Failed to capture screenshot for keyboard action failure: {}", methodName, e.getMessage());
        }
    }
    
    /**
     * Clean up thread local driver
     */
    public static void cleanup() {
        driverThreadLocal.remove();
        log.debug("WebDriver removed from thread for keyboard events: {}", Thread.currentThread().getName());
    }
}