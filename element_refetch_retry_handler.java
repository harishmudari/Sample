package com.automation.keywords;

import org.openqa.selenium.*;
import org.openqa.selenium.support.ui.FluentWait;
import org.openqa.selenium.support.ui.WebDriverWait;
import org.openqa.selenium.support.ui.ExpectedConditions;
import org.openqa.selenium.interactions.Actions;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import java.time.Duration;
import java.util.List;
import java.util.function.Function;
import java.util.function.Supplier;
import java.util.Properties;
import java.io.FileInputStream;
import java.io.IOException;

/**
 * Enterprise-level Element Re-fetch and Retry Handler for Selenium WebDriver
 * 
 * Provides comprehensive element handling with smart re-fetching and retry capabilities:
 * - FluentWait-based element location and interaction
 * - Automatic stale element re-fetching
 * - Configurable retry mechanisms with exponential backoff
 * - Proper exception handling and logging
 * - Auto screenshot on failures
 * - Thread-safe implementation
 * - Performance tracking and optimization
 * 
 * @author Automation Team
 * @version 1.0
 */
public class ElementRefetchRetryHandler {
    
    private static final Logger log = LoggerFactory.getLogger(ElementRefetchRetryHandler.class);
    private static final String CONFIG_FILE = "config/automation.properties";
    
    // Thread-safe WebDriver instance
    private static ThreadLocal<WebDriver> driver = new ThreadLocal<>();
    private static Properties config;
    
    // Configuration constants
    private static final int DEFAULT_TIMEOUT = 30;
    private static final int DEFAULT_POLLING_INTERVAL = 500;
    private static final int DEFAULT_RETRY_COUNT = 5;
    private static final int DEFAULT_EXPONENTIAL_BACKOFF_MULTIPLIER = 2;
    private static final int DEFAULT_MAX_RETRY_DELAY = 5000;
    
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
        config.setProperty("element.timeout", String.valueOf(DEFAULT_TIMEOUT));
        config.setProperty("element.polling.interval", String.valueOf(DEFAULT_POLLING_INTERVAL));
        config.setProperty("element.retry.count", String.valueOf(DEFAULT_RETRY_COUNT));
        config.setProperty("element.backoff.multiplier", String.valueOf(DEFAULT_EXPONENTIAL_BACKOFF_MULTIPLIER));
        config.setProperty("element.max.retry.delay", String.valueOf(DEFAULT_MAX_RETRY_DELAY));
        config.setProperty("screenshot.on.failure", "true");
    }
    
    /**
     * Set WebDriver instance for current thread
     * @param webDriver WebDriver instance
     */
    public static void setDriver(WebDriver webDriver) {
        if (webDriver == null) {
            throw new CustomElementException("WebDriver instance cannot be null");
        }
        driver.set(webDriver);
        log.debug("WebDriver set for thread: {}", Thread.currentThread().getName());
    }
    
    /**
     * Get WebDriver instance for current thread
     */
    private static WebDriver getDriver() {
        WebDriver webDriver = driver.get();
        if (webDriver == null) {
            throw new CustomElementException("WebDriver not initialized for current thread");
        }
        return webDriver;
    }
    
    // ========== FLUENT WAIT ELEMENT LOCATION METHODS ==========
    
    /**
     * Find element with FluentWait and automatic re-fetching on stale reference
     * 
     * @param locator By locator for the element
     * @return ElementOperationResult with the found WebElement
     */
    public static ElementOperationResult findElementWithRetry(By locator) {
        return findElementWithRetry(locator, "Find element with retry");
    }
    
    /**
     * Find element with FluentWait and automatic re-fetching with custom description
     * 
     * @param locator By locator for the element
     * @param description Custom description for logging
     * @return ElementOperationResult with the found WebElement
     */
    public static ElementOperationResult findElementWithRetry(By locator, String description) {
        long startTime = System.currentTimeMillis();
        String methodName = "findElementWithRetry";
        
        log.info("[{}] Starting operation - Description: {} | Locator: {}", methodName, description, locator);
        
        if (locator == null) {
            String errorMsg = "Element locator cannot be null";
            log.error("[{}] {}", methodName, errorMsg);
            return createFailureResult(errorMsg, startTime);
        }
        
        return executeWithFluentWait(() -> {
            FluentWait<WebDriver> fluentWait = createFluentWait();
            
            WebElement element = fluentWait.until(driver -> {
                try {
                    WebElement foundElement = driver.findElement(locator);
                    log.debug("[{}] Element found - Locator: {}", methodName, locator);
                    return foundElement;
                } catch (NoSuchElementException e) {
                    log.debug("[{}] Element not found, continuing to wait - Locator: {}", methodName, locator);
                    return null;
                }
            });
            
            log.info("[{}] Successfully found element - Description: {} | Locator: {}", 
                    methodName, description, locator);
            return createSuccessResult("Element found successfully", startTime, element);
            
        }, methodName, description + " | Locator: " + locator, startTime);
    }
    
    /**
     * Find multiple elements with FluentWait and retry mechanism
     * 
     * @param locator By locator for the elements
     * @return ElementOperationResult with list of found WebElements
     */
    public static ElementOperationResult findElementsWithRetry(By locator) {
        return findElementsWithRetry(locator, "Find elements with retry");
    }
    
    /**
     * Find multiple elements with FluentWait and retry mechanism with custom description
     * 
     * @param locator By locator for the elements
     * @param description Custom description for logging
     * @return ElementOperationResult with list of found WebElements
     */
    public static ElementOperationResult findElementsWithRetry(By locator, String description) {
        long startTime = System.currentTimeMillis();
        String methodName = "findElementsWithRetry";
        
        log.info("[{}] Starting operation - Description: {} | Locator: {}", methodName, description, locator);
        
        if (locator == null) {
            String errorMsg = "Element locator cannot be null";
            log.error("[{}] {}", methodName, errorMsg);
            return createFailureResult(errorMsg, startTime);
        }
        
        return executeWithFluentWait(() -> {
            FluentWait<WebDriver> fluentWait = createFluentWait();
            
            List<WebElement> elements = fluentWait.until(driver -> {
                List<WebElement> foundElements = driver.findElements(locator);
                if (foundElements.isEmpty()) {
                    log.debug("[{}] No elements found, continuing to wait - Locator: {}", methodName, locator);
                    return null;
                }
                log.debug("[{}] Found {} elements - Locator: {}", methodName, foundElements.size(), locator);
                return foundElements;
            });
            
            log.info("[{}] Successfully found {} elements - Description: {} | Locator: {}", 
                    methodName, elements.size(), description, locator);
            return createSuccessResult("Found " + elements.size() + " elements", startTime, elements);
            
        }, methodName, description + " | Locator: " + locator, startTime);
    }
    
    /**
     * Wait for element to be clickable with FluentWait and re-fetch on stale reference
     * 
     * @param locator By locator for the element
     * @return ElementOperationResult with clickable WebElement
     */
    public static ElementOperationResult waitForClickableWithRetry(By locator) {
        return waitForClickableWithRetry(locator, "Wait for element to be clickable");
    }
    
    /**
     * Wait for element to be clickable with FluentWait and re-fetch with custom description
     * 
     * @param locator By locator for the element
     * @param description Custom description for logging
     * @return ElementOperationResult with clickable WebElement
     */
    public static ElementOperationResult waitForClickableWithRetry(By locator, String description) {
        long startTime = System.currentTimeMillis();
        String methodName = "waitForClickableWithRetry";
        
        log.info("[{}] Starting operation - Description: {} | Locator: {}", methodName, description, locator);
        
        if (locator == null) {
            String errorMsg = "Element locator cannot be null";
            log.error("[{}] {}", methodName, errorMsg);
            return createFailureResult(errorMsg, startTime);
        }
        
        return executeWithFluentWait(() -> {
            FluentWait<WebDriver> fluentWait = createFluentWait()
                .ignoring(StaleElementReferenceException.class)
                .ignoring(ElementNotInteractableException.class);
            
            WebElement element = fluentWait.until(driver -> {
                try {
                    WebElement foundElement = driver.findElement(locator);
                    if (foundElement.isDisplayed() && foundElement.isEnabled()) {
                        log.debug("[{}] Element is clickable - Locator: {}", methodName, locator);
                        return foundElement;
                    } else {
                        log.debug("[{}] Element not clickable yet, continuing to wait - Locator: {}", methodName, locator);
                        return null;
                    }
                } catch (NoSuchElementException | StaleElementReferenceException e) {
                    log.debug("[{}] Element not found or stale, continuing to wait - Locator: {}", methodName, locator);
                    return null;
                }
            });
            
            log.info("[{}] Successfully found clickable element - Description: {} | Locator: {}", 
                    methodName, description, locator);
            return createSuccessResult("Element is clickable", startTime, element);
            
        }, methodName, description + " | Locator: " + locator, startTime);
    }
    
    /**
     * Wait for element to be visible with FluentWait and re-fetch on stale reference
     * 
     * @param locator By locator for the element
     * @return ElementOperationResult with visible WebElement
     */
    public static ElementOperationResult waitForVisibleWithRetry(By locator) {
        return waitForVisibleWithRetry(locator, "Wait for element to be visible");
    }
    
    /**
     * Wait for element to be visible with FluentWait and re-fetch with custom description
     * 
     * @param locator By locator for the element
     * @param description Custom description for logging
     * @return ElementOperationResult with visible WebElement
     */
    public static ElementOperationResult waitForVisibleWithRetry(By locator, String description) {
        long startTime = System.currentTimeMillis();
        String methodName = "waitForVisibleWithRetry";
        
        log.info("[{}] Starting operation - Description: {} | Locator: {}", methodName, description, locator);
        
        if (locator == null) {
            String errorMsg = "Element locator cannot be null";
            log.error("[{}] {}", methodName, errorMsg);
            return createFailureResult(errorMsg, startTime);
        }
        
        return executeWithFluentWait(() -> {
            FluentWait<WebDriver> fluentWait = createFluentWait()
                .ignoring(StaleElementReferenceException.class);
            
            WebElement element = fluentWait.until(driver -> {
                try {
                    WebElement foundElement = driver.findElement(locator);
                    if (foundElement.isDisplayed()) {
                        log.debug("[{}] Element is visible - Locator: {}", methodName, locator);
                        return foundElement;
                    } else {
                        log.debug("[{}] Element not visible yet, continuing to wait - Locator: {}", methodName, locator);
                        return null;
                    }
                } catch (NoSuchElementException | StaleElementReferenceException e) {
                    log.debug("[{}] Element not found or stale, continuing to wait - Locator: {}", methodName, locator);
                    return null;
                }
            });
            
            log.info("[{}] Successfully found visible element - Description: {} | Locator: {}", 
                    methodName, description, locator);
            return createSuccessResult("Element is visible", startTime, element);
            
        }, methodName, description + " | Locator: " + locator, startTime);
    }
    
    // ========== ELEMENT INTERACTION WITH RETRY METHODS ==========
    
    /**
     * Click element with automatic re-fetching and retry on stale reference
     * 
     * @param locator By locator for the element
     * @return ElementOperationResult indicating success/failure
     */
    public static ElementOperationResult clickWithRetry(By locator) {
        return clickWithRetry(locator, "Click element with retry");
    }
    
    /**
     * Click element with automatic re-fetching and retry with custom description
     * 
     * @param locator By locator for the element
     * @param description Custom description for logging
     * @return ElementOperationResult indicating success/failure
     */
    public static ElementOperationResult clickWithRetry(By locator, String description) {
        long startTime = System.currentTimeMillis();
        String methodName = "clickWithRetry";
        
        log.info("[{}] Starting operation - Description: {} | Locator: {}", methodName, description, locator);
        
        if (locator == null) {
            String errorMsg = "Element locator cannot be null";
            log.error("[{}] {}", methodName, errorMsg);
            return createFailureResult(errorMsg, startTime);
        }
        
        return executeElementInteractionWithRetry(() -> {
            // Re-fetch element each time to avoid stale reference
            WebElement element = refetchElement(locator);
            element.click();
            
            log.info("[{}] Successfully clicked element - Description: {} | Locator: {}", 
                    methodName, description, locator);
            return createSuccessResult("Element clicked successfully", startTime);
            
        }, methodName, description + " | Locator: " + locator, startTime);
    }
    
    /**
     * Send keys to element with automatic re-fetching and retry on stale reference
     * 
     * @param locator By locator for the element
     * @param keys Keys to send
     * @return ElementOperationResult indicating success/failure
     */
    public static ElementOperationResult sendKeysWithRetry(By locator, CharSequence... keys) {
        return sendKeysWithRetry(locator, "Send keys to element", keys);
    }
    
    /**
     * Send keys to element with automatic re-fetching and retry with custom description
     * 
     * @param locator By locator for the element
     * @param description Custom description for logging
     * @param keys Keys to send
     * @return ElementOperationResult indicating success/failure
     */
    public static ElementOperationResult sendKeysWithRetry(By locator, String description, CharSequence... keys) {
        long startTime = System.currentTimeMillis();
        String methodName = "sendKeysWithRetry";
        
        log.info("[{}] Starting operation - Description: {} | Locator: {} | Keys: {}", 
                methodName, description, locator, maskSensitiveData(keys));
        
        if (locator == null || keys == null || keys.length == 0) {
            String errorMsg = "Element locator and keys cannot be null or empty";
            log.error("[{}] {}", methodName, errorMsg);
            return createFailureResult(errorMsg, startTime);
        }
        
        return executeElementInteractionWithRetry(() -> {
            // Re-fetch element each time to avoid stale reference
            WebElement element = refetchElement(locator);
            element.clear(); // Clear existing text first
            element.sendKeys(keys);
            
            log.info("[{}] Successfully sent keys to element - Description: {} | Locator: {}", 
                    methodName, description, locator);
            return createSuccessResult("Keys sent successfully", startTime);
            
        }, methodName, description + " | Locator: " + locator, startTime);
    }
    
    /**
     * Get text from element with automatic re-fetching and retry on stale reference
     * 
     * @param locator By locator for the element
     * @return ElementOperationResult with element text
     */
    public static ElementOperationResult getTextWithRetry(By locator) {
        return getTextWithRetry(locator, "Get text from element");
    }
    
    /**
     * Get text from element with automatic re-fetching and retry with custom description
     * 
     * @param locator By locator for the element
     * @param description Custom description for logging
     * @return ElementOperationResult with element text
     */
    public static ElementOperationResult getTextWithRetry(By locator, String description) {
        long startTime = System.currentTimeMillis();
        String methodName = "getTextWithRetry";
        
        log.info("[{}] Starting operation - Description: {} | Locator: {}", methodName, description, locator);
        
        if (locator == null) {
            String errorMsg = "Element locator cannot be null";
            log.error("[{}] {}", methodName, errorMsg);
            return createFailureResult(errorMsg, startTime);
        }
        
        return executeElementInteractionWithRetry(() -> {
            // Re-fetch element each time to avoid stale reference
            WebElement element = refetchElement(locator);
            String elementText = element.getText().trim();
            
            log.info("[{}] Successfully retrieved text from element - Description: {} | Locator: {} | Text: {}", 
                    methodName, description, locator, elementText);
            return createSuccessResult("Text retrieved successfully", startTime, elementText);
            
        }, methodName, description + " | Locator: " + locator, startTime);
    }
    
    /**
     * Get attribute from element with automatic re-fetching and retry on stale reference
     * 
     * @param locator By locator for the element
     * @param attributeName Name of the attribute to retrieve
     * @return ElementOperationResult with attribute value
     */
    public static ElementOperationResult getAttributeWithRetry(By locator, String attributeName) {
        return getAttributeWithRetry(locator, attributeName, "Get attribute from element");
    }
    
    /**
     * Get attribute from element with automatic re-fetching and retry with custom description
     * 
     * @param locator By locator for the element
     * @param attributeName Name of the attribute to retrieve
     * @param description Custom description for logging
     * @return ElementOperationResult with attribute value
     */
    public static ElementOperationResult getAttributeWithRetry(By locator, String attributeName, String description) {
        long startTime = System.currentTimeMillis();
        String methodName = "getAttributeWithRetry";
        
        log.info("[{}] Starting operation - Description: {} | Locator: {} | Attribute: {}", 
                methodName, description, locator, attributeName);
        
        if (locator == null || attributeName == null || attributeName.trim().isEmpty()) {
            String errorMsg = "Element locator and attribute name cannot be null or empty";
            log.error("[{}] {}", methodName, errorMsg);
            return createFailureResult(errorMsg, startTime);
        }
        
        return executeElementInteractionWithRetry(() -> {
            // Re-fetch element each time to avoid stale reference
            WebElement element = refetchElement(locator);
            String attributeValue = element.getAttribute(attributeName);
            
            log.info("[{}] Successfully retrieved attribute from element - Description: {} | Locator: {} | Attribute: {} | Value: {}", 
                    methodName, description, locator, attributeName, attributeValue);
            return createSuccessResult("Attribute retrieved successfully", startTime, attributeValue);
            
        }, methodName, description + " | Locator: " + locator + " | Attribute: " + attributeName, startTime);
    }
    
    /**
     * Check if element is displayed with automatic re-fetching and retry
     * 
     * @param locator By locator for the element
     * @return ElementOperationResult with boolean result
     */
    public static ElementOperationResult isDisplayedWithRetry(By locator) {
        return isDisplayedWithRetry(locator, "Check if element is displayed");
    }
    
    /**
     * Check if element is displayed with automatic re-fetching and retry with custom description
     * 
     * @param locator By locator for the element
     * @param description Custom description for logging
     * @return ElementOperationResult with boolean result
     */
    public static ElementOperationResult isDisplayedWithRetry(By locator, String description) {
        long startTime = System.currentTimeMillis();
        String methodName = "isDisplayedWithRetry";
        
        log.info("[{}] Starting operation - Description: {} | Locator: {}", methodName, description, locator);
        
        if (locator == null) {
            String errorMsg = "Element locator cannot be null";
            log.error("[{}] {}", methodName, errorMsg);
            return createFailureResult(errorMsg, startTime);
        }
        
        try {
            FluentWait<WebDriver> fluentWait = createFluentWait()
                .ignoring(StaleElementReferenceException.class)
                .ignoring(NoSuchElementException.class);
            
            Boolean isDisplayed = fluentWait.until(driver -> {
                try {
                    WebElement element = driver.findElement(locator);
                    return element.isDisplayed();
                } catch (NoSuchElementException | StaleElementReferenceException e) {
                    return false;
                }
            });
            
            String result = isDisplayed ? "Element is displayed" : "Element is not displayed";
            log.info("[{}] {} - Description: {} | Locator: {}", methodName, result, description, locator);
            
            return createSuccessResult(result, startTime, String.valueOf(isDisplayed));
            
        } catch (Exception e) {
            String errorMsg = "Failed to check element display status: " + e.getMessage();
            log.error("[{}] {} - Description: {} | Locator: {}", methodName, errorMsg, description, locator, e);
            
            if (shouldTakeScreenshot()) {
                takeScreenshot(methodName);
            }
            
            return createFailureResult(errorMsg, startTime);
        }
    }
    
    // ========== ADVANCED FLUENT WAIT METHODS ==========
    
    /**
     * Wait for custom condition with FluentWait and element re-fetching
     * 
     * @param locator By locator for the element
     * @param condition Custom condition function
     * @param conditionDescription Description of the condition
     * @return ElementOperationResult indicating success/failure
     */
    public static ElementOperationResult waitForCustomCondition(By locator, 
                                                              Function<WebElement, Boolean> condition, 
                                                              String conditionDescription) {
        return waitForCustomCondition(locator, condition, conditionDescription, "Wait for custom condition");
    }
    
    /**
     * Wait for custom condition with FluentWait and element re-fetching with custom description
     * 
     * @param locator By locator for the element
     * @param condition Custom condition function
     * @param conditionDescription Description of the condition
     * @param description Custom description for logging
     * @return ElementOperationResult indicating success/failure
     */
    public static ElementOperationResult waitForCustomCondition(By locator, 
                                                              Function<WebElement, Boolean> condition, 
                                                              String conditionDescription,
                                                              String description) {
        long startTime = System.currentTimeMillis();
        String methodName = "waitForCustomCondition";
        
        log.info("[{}] Starting operation - Description: {} | Locator: {} | Condition: {}", 
                methodName, description, locator, conditionDescription);
        
        if (locator == null || condition == null) {
            String errorMsg = "Element locator and condition cannot be null";
            log.error("[{}] {}", methodName, errorMsg);
            return createFailureResult(errorMsg, startTime);
        }
        
        return executeWithFluentWait(() -> {
            FluentWait<WebDriver> fluentWait = createFluentWait()
                .ignoring(StaleElementReferenceException.class);
            
            WebElement element = fluentWait.until(driver -> {
                try {
                    WebElement foundElement = driver.findElement(locator);
                    if (condition.apply(foundElement)) {
                        log.debug("[{}] Custom condition met - Locator: {} | Condition: {}", 
                                methodName, locator, conditionDescription);
                        return foundElement;
                    } else {
                        log.debug("[{}] Custom condition not met yet, continuing to wait - Locator: {} | Condition: {}", 
                                methodName, locator, conditionDescription);
                        return null;
                    }
                } catch (NoSuchElementException | StaleElementReferenceException e) {
                    log.debug("[{}] Element not found or stale, continuing to wait - Locator: {}", methodName, locator);
                    return null;
                }
            });
            
            log.info("[{}] Custom condition met successfully - Description: {} | Locator: {} | Condition: {}", 
                    methodName, description, locator, conditionDescription);
            return createSuccessResult("Custom condition met: " + conditionDescription, startTime, element);
            
        }, methodName, description + " | Locator: " + locator + " | Condition: " + conditionDescription, startTime);
    }
    
    /**
     * Wait for text to be present in element with automatic re-fetching
     * 
     * @param locator By locator for the element
     * @param expectedText Text expected to be present
     * @return ElementOperationResult indicating success/failure
     */
    public static ElementOperationResult waitForTextToBePresentWithRetry(By locator, String expectedText) {
        return waitForTextToBePresentWithRetry(locator, expectedText, "Wait for text to be present");
    }
    
    /**
     * Wait for text to be present in element with automatic re-fetching and custom description
     * 
     * @param locator By locator for the element
     * @param expectedText Text expected to be present
     * @param description Custom description for logging
     * @return ElementOperationResult indicating success/failure
     */
    public static ElementOperationResult waitForTextToBePresentWithRetry(By locator, String expectedText, String description) {
        return waitForCustomCondition(
            locator,
            element -> element.getText().contains(expectedText),
            "Text contains: " + expectedText,
            description
        );
    }
    
    /**
     * Wait for attribute to have specific value with automatic re-fetching
     * 
     * @param locator By locator for the element
     * @param attributeName Name of the attribute
     * @param expectedValue Expected attribute value
     * @return ElementOperationResult indicating success/failure
     */
    public static ElementOperationResult waitForAttributeValueWithRetry(By locator, String attributeName, String expectedValue) {
        return waitForAttributeValueWithRetry(locator, attributeName, expectedValue, "Wait for attribute value");
    }
    
    /**
     * Wait for attribute to have specific value with automatic re-fetching and custom description
     * 
     * @param locator By locator for the element
     * @param attributeName Name of the attribute
     * @param expectedValue Expected attribute value
     * @param description Custom description for logging
     * @return ElementOperationResult indicating success/failure
     */
    public static ElementOperationResult waitForAttributeValueWithRetry(By locator, String attributeName, String expectedValue, String description) {
        return waitForCustomCondition(
            locator,
            element -> expectedValue.equals(element.getAttribute(attributeName)),
            attributeName + " equals: " + expectedValue,
            description
        );
    }
    
    // ========== UTILITY METHODS ==========
    
    /**
     * Re-fetch element to avoid stale element reference
     * 
     * @param locator By locator for the element
     * @return Fresh WebElement instance
     */
    private static WebElement refetchElement(By locator) {
        try {
            WebDriverWait wait = new WebDriverWait(getDriver(), Duration.ofSeconds(getTimeout()));
            return wait.until(ExpectedConditions.presenceOfElementLocated(locator));
        } catch (Exception e) {
            throw new CustomElementException("Failed to re-fetch element: " + locator, e);
        }
    }
    
    /**
     * Execute operation with FluentWait error handling
     */
    private static ElementOperationResult executeWithFluentWait(
            ElementOperation operation, String methodName, String context, long startTime) {
        
        try {
            return operation.execute();
            
        } catch (TimeoutException e) {
            String errorMsg = "Timeout waiting for element condition to be met";
            log.error("[{}] {} - Context: {}", methodName, errorMsg, context, e);
            
            if (shouldTakeScreenshot()) {
                takeScreenshot(methodName);
            }
            
            return createFailureResult(errorMsg + ": " + e.getMessage(), startTime);
            
        } catch (Exception e) {
            String errorMsg = "Unexpected error in FluentWait operation";
            log.error("[{}] {} - Context: {} - Error: {}", methodName, errorMsg, context, e.getMessage(), e);
            
            if (shouldTakeScreenshot()) {
                takeScreenshot(methodName);
            }
            
            return createFailureResult(errorMsg + ": " + e.getMessage(), startTime);
        }
    }
    
    /**
     * Execute element interaction with exponential backoff retry
     */
    private static ElementOperationResult executeElementInteractionWithRetry(
            ElementOperation operation, String methodName, String context, long startTime) {
        
        int retryCount = getRetryCount();
        int backoffMultiplier = getBackoffMultiplier();
        int maxRetryDelay = getMaxRetryDelay();
        
        Exception lastException = null;
        int currentDelay = getPollingInterval();
        
        for (int attempt = 1; attempt <= retryCount; attempt++) {
            try {
                log.debug("[{}] Attempt {} of {} - Context: {}", methodName, attempt, retryCount, context);
                
                return operation.execute();
                
            