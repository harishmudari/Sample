package com.enterprise.selenium.keywords;

import org.openqa.selenium.*;
import org.openqa.selenium.support.ui.WebDriverWait;
import org.openqa.selenium.support.ui.ExpectedConditions;
import org.openqa.selenium.support.ui.Select;
import org.openqa.selenium.interactions.Actions;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import org.apache.commons.io.FileUtils;
import com.aventstack.extentreports.ExtentTest;
import com.aventstack.extentreports.Status;

import java.io.File;
import java.io.IOException;
import java.time.Duration;
import java.time.Instant;
import java.util.Properties;
import java.util.List;
import java.util.concurrent.ThreadLocalRandom;

/**
 * Enterprise-level Selenium keyword operations library
 * Follows all production best practices including fail-safe design,
 * proper exception handling, logging, and thread-safety
 * 
 * @author Enterprise Automation Team
 * @version 1.0
 */
public class SeleniumKeywordOperations {
    
    private static final Logger log = LogManager.getLogger(SeleniumKeywordOperations.class);
    private static final ThreadLocal<WebDriver> driver = new ThreadLocal<>();
    private static final ThreadLocal<ExtentTest> extentTest = new ThreadLocal<>();
    private static Properties config;
    
    // Configuration constants - loaded from properties
    private static final int DEFAULT_TIMEOUT = getConfigInt("default.timeout", 10);
    private static final int RETRY_COUNT = getConfigInt("retry.count", 3);
    private static final int RETRY_DELAY = getConfigInt("retry.delay.ms", 1000);
    
    /**
     * Custom exception types for better error handling
     */
    public static class CustomElementNotFoundException extends RuntimeException {
        public CustomElementNotFoundException(String message) {
            super(message);
        }
    }
    
    public static class CustomTimeoutException extends RuntimeException {
        public CustomTimeoutException(String message) {
            super(message);
        }
    }
    
    /**
     * Response object for meaningful returns
     */
    public static class OperationResponse {
        private boolean success;
        private String message;
        private Object data;
        
        public OperationResponse(boolean success, String message, Object data) {
            this.success = success;
            this.message = message;
            this.data = data;
        }
        
        // Getters
        public boolean isSuccess() { return success; }
        public String getMessage() { return message; }
        public Object getData() { return data; }
    }
    
    /**
     * Set WebDriver for current thread
     */
    public static void setDriver(WebDriver webDriver) {
        driver.set(webDriver);
    }
    
    /**
     * Get WebDriver for current thread
     */
    public static WebDriver getDriver() {
        return driver.get();
    }
    
    /**
     * Set ExtentTest for current thread
     */
    public static void setExtentTest(ExtentTest test) {
        extentTest.set(test);
    }
    
    /**
     * Wait for element to be present and visible with retry mechanism
     * 
     * @param locator Element locator
     * @param timeoutSeconds Timeout in seconds
     * @return OperationResponse with WebElement or failure info
     */
    public static OperationResponse waitForElementVisible(By locator, int timeoutSeconds) {
        Instant startTime = Instant.now();
        String methodName = "waitForElementVisible";
        
        log.info("{} - Starting with locator: {}, timeout: {}s", methodName, locator, timeoutSeconds);
        
        // Fail-fast: Input validation
        if (locator == null) {
            String errorMsg = "Locator cannot be null";
            log.error("{} - {}", methodName, errorMsg);
            logToExtentReport(Status.FAIL, errorMsg);
            return new OperationResponse(false, errorMsg, null);
        }
        
        if (getDriver() == null) {
            String errorMsg = "WebDriver is not initialized";
            log.error("{} - {}", methodName, errorMsg);
            logToExtentReport(Status.FAIL, errorMsg);
            return new OperationResponse(false, errorMsg, null);
        }
        
        WebElement element = null;
        Exception lastException = null;
        
        // Retry mechanism
        for (int attempt = 1; attempt <= RETRY_COUNT; attempt++) {
            try {
                log.debug("{} - Attempt {} of {}", methodName, attempt, RETRY_COUNT);
                
                WebDriverWait wait = new WebDriverWait(getDriver(), Duration.ofSeconds(timeoutSeconds));
                element = wait.until(ExpectedConditions.visibilityOfElementLocated(locator));
                
                long executionTime = Duration.between(startTime, Instant.now()).toMillis();
                String successMsg = String.format("Element found and visible on attempt %d - Execution time: %dms", 
                                                 attempt, executionTime);
                
                log.info("{} - {}", methodName, successMsg);
                logToExtentReport(Status.PASS, successMsg);
                
                return new OperationResponse(true, successMsg, element);
                
            } catch (TimeoutException e) {
                lastException = e;
                log.warn("{} - Attempt {} failed: Timeout waiting for element visibility", methodName, attempt);
                
                if (attempt < RETRY_COUNT) {
                    sleep(RETRY_DELAY);
                }
            } catch (Exception e) {
                lastException = e;
                log.error("{} - Attempt {} failed with unexpected error: {}", methodName, attempt, e.getMessage());
                break; // Don't retry on unexpected exceptions
            }
        }
        
        // All attempts failed
        String errorMsg = String.format("Failed to find visible element after %d attempts. Last error: %s", 
                                       RETRY_COUNT, lastException.getMessage());
        log.error("{} - {}", methodName, errorMsg);
        
        // Auto screenshot on failure
        captureScreenshot(methodName + "_failure");
        logToExtentReport(Status.FAIL, errorMsg);
        
        throw new CustomElementNotFoundException(errorMsg);
    }
    
    /**
     * Click element when visible with fallback mechanisms
     * 
     * @param locator Element locator
     * @param timeoutSeconds Timeout for waiting
     * @return OperationResponse indicating success/failure
     */
    public static OperationResponse clickWhenVisible(By locator, int timeoutSeconds) {
        Instant startTime = Instant.now();
        String methodName = "clickWhenVisible";
        
        log.info("{} - Starting with locator: {}", methodName, locator);
        
        // Get element first
        OperationResponse elementResponse = waitForElementVisible(locator, timeoutSeconds);
        if (!elementResponse.isSuccess()) {
            return elementResponse; // Propagate the failure
        }
        
        WebElement element = (WebElement) elementResponse.getData();
        
        // Try normal click first
        try {
            element.click();
            long executionTime = Duration.between(startTime, Instant.now()).toMillis();
            String successMsg = String.format("Element clicked successfully - Execution time: %dms", executionTime);
            
            log.info("{} - {}", methodName, successMsg);
            logToExtentReport(Status.PASS, successMsg);
            
            return new OperationResponse(true, successMsg, null);
            
        } catch (ElementClickInterceptedException | ElementNotInteractableException e) {
            log.warn("{} - Normal click failed, trying JavaScript click: {}", methodName, e.getMessage());
            
            // Fallback to JavaScript click
            try {
                JavascriptExecutor js = (JavascriptExecutor) getDriver();
                js.executeScript("arguments[0].click();", element);
                
                long executionTime = Duration.between(startTime, Instant.now()).toMillis();
                String successMsg = String.format("Element clicked via JavaScript fallback - Execution time: %dms", 
                                                 executionTime);
                
                log.info("{} - {}", methodName, successMsg);
                logToExtentReport(Status.PASS, successMsg);
                
                return new OperationResponse(true, successMsg, null);
                
            } catch (Exception jsException) {
                String errorMsg = String.format("Both normal and JavaScript click failed. Errors - Normal: %s, JS: %s", 
                                               e.getMessage(), jsException.getMessage());
                
                log.error("{} - {}", methodName, errorMsg);
                captureScreenshot(methodName + "_failure");
                logToExtentReport(Status.FAIL, errorMsg);
                
                return new OperationResponse(false, errorMsg, null);
            }
        } catch (Exception e) {
            String errorMsg = "Unexpected error during click: " + e.getMessage();
            log.error("{} - {}", methodName, errorMsg);
            captureScreenshot(methodName + "_failure");
            logToExtentReport(Status.FAIL, errorMsg);
            
            return new OperationResponse(false, errorMsg, null);
        }
    }
    
    /**
     * Enter text into input field with validation
     * 
     * @param locator Input field locator
     * @param text Text to enter
     * @param clearFirst Whether to clear field first
     * @return OperationResponse indicating success/failure
     */
    public static OperationResponse enterText(By locator, String text, boolean clearFirst) {
        Instant startTime = Instant.now();
        String methodName = "enterText";
        
        log.info("{} - Starting with locator: {}, text: '{}', clearFirst: {}", 
                methodName, locator, text, clearFirst);
        
        // Fail-fast: Input validation
        if (text == null) {
            String errorMsg = "Text to enter cannot be null";
            log.error("{} - {}", methodName, errorMsg);
            logToExtentReport(Status.FAIL, errorMsg);
            return new OperationResponse(false, errorMsg, null);
        }
        
        // Get element first
        OperationResponse elementResponse = waitForElementVisible(locator, DEFAULT_TIMEOUT);
        if (!elementResponse.isSuccess()) {
            return elementResponse;
        }
        
        WebElement element = (WebElement) elementResponse.getData();
        
        try {
            if (clearFirst) {
                element.clear();
                log.debug("{} - Field cleared", methodName);
            }
            
            element.sendKeys(text);
            
            // Validate text was entered correctly
            String actualText = element.getAttribute("value");
            if (!text.equals(actualText)) {
                String warningMsg = String.format("Text validation warning - Expected: '%s', Actual: '%s'", 
                                                 text, actualText);
                log.warn("{} - {}", methodName, warningMsg);
                logToExtentReport(Status.WARNING, warningMsg);
            }
            
            long executionTime = Duration.between(startTime, Instant.now()).toMillis();
            String successMsg = String.format("Text entered successfully - Execution time: %dms", executionTime);
            
            log.info("{} - {}", methodName, successMsg);
            logToExtentReport(Status.PASS, successMsg);
            
            return new OperationResponse(true, successMsg, actualText);
            
        } catch (Exception e) {
            String errorMsg = "Failed to enter text: " + e.getMessage();
            log.error("{} - {}", methodName, errorMsg);
            captureScreenshot(methodName + "_failure");
            logToExtentReport(Status.FAIL, errorMsg);
            
            return new OperationResponse(false, errorMsg, null);
        }
    }
    
    /**
     * Get validated text from element with retry
     * 
     * @param locator Element locator
     * @param attributeType Type of text to get ("text", "value", or attribute name)
     * @return OperationResponse with text or failure info
     */
    public static OperationResponse getValidatedText(By locator, String attributeType) {
        Instant startTime = Instant.now();
        String methodName = "getValidatedText";
        
        log.info("{} - Starting with locator: {}, attributeType: '{}'", 
                methodName, locator, attributeType);
        
        // Fail-fast: Input validation
        if (attributeType == null || attributeType.trim().isEmpty()) {
            String errorMsg = "Attribute type cannot be null or empty";
            log.error("{} - {}", methodName, errorMsg);
            logToExtentReport(Status.FAIL, errorMsg);
            return new OperationResponse(false, errorMsg, null);
        }
        
        // Get element first
        OperationResponse elementResponse = waitForElementVisible(locator, DEFAULT_TIMEOUT);
        if (!elementResponse.isSuccess()) {
            return elementResponse;
        }
        
        WebElement element = (WebElement) elementResponse.getData();
        
        try {
            String text;
            switch (attributeType.toLowerCase()) {
                case "text":
                    text = element.getText();
                    break;
                case "value":
                    text = element.getAttribute("value");
                    break;
                default:
                    text = element.getAttribute(attributeType);
                    break;
            }
            
            if (text == null) {
                text = ""; // Convert null to empty string
            }
            
            long executionTime = Duration.between(startTime, Instant.now()).toMillis();
            String successMsg = String.format("Text retrieved successfully: '%s' - Execution time: %dms", 
                                             text, executionTime);
            
            log.info("{} - {}", methodName, successMsg);
            logToExtentReport(Status.PASS, successMsg);
            
            return new OperationResponse(true, successMsg, text);
            
        } catch (Exception e) {
            String errorMsg = "Failed to get text: " + e.getMessage();
            log.error("{} - {}", methodName, errorMsg);
            captureScreenshot(methodName + "_failure");
            logToExtentReport(Status.FAIL, errorMsg);
            
            return new OperationResponse(false, errorMsg, null);
        }
    }
    
    /**
     * Select dropdown option by visible text or value
     * 
     * @param locator Dropdown locator
     * @param optionText Option text or value
     * @param byValue Whether to select by value (true) or visible text (false)
     * @return OperationResponse indicating success/failure
     */
    public static OperationResponse selectDropdownOption(By locator, String optionText, boolean byValue) {
        Instant startTime = Instant.now();
        String methodName = "selectDropdownOption";
        
        log.info("{} - Starting with locator: {}, optionText: '{}', byValue: {}", 
                methodName, locator, optionText, byValue);
        
        // Fail-fast: Input validation
        if (optionText == null || optionText.trim().isEmpty()) {
            String errorMsg = "Option text cannot be null or empty";
            log.error("{} - {}", methodName, errorMsg);
            logToExtentReport(Status.FAIL, errorMsg);
            return new OperationResponse(false, errorMsg, null);
        }
        
        // Get element first
        OperationResponse elementResponse = waitForElementVisible(locator, DEFAULT_TIMEOUT);
        if (!elementResponse.isSuccess()) {
            return elementResponse;
        }
        
        WebElement element = (WebElement) elementResponse.getData();
        
        try {
            Select dropdown = new Select(element);
            
            if (byValue) {
                dropdown.selectByValue(optionText);
            } else {
                dropdown.selectByVisibleText(optionText);
            }
            
            // Validate selection
            String selectedText = dropdown.getFirstSelectedOption().getText();
            String selectedValue = dropdown.getFirstSelectedOption().getAttribute("value");
            
            long executionTime = Duration.between(startTime, Instant.now()).toMillis();
            String successMsg = String.format("Dropdown option selected - Text: '%s', Value: '%s' - Execution time: %dms", 
                                             selectedText, selectedValue, executionTime);
            
            log.info("{} - {}", methodName, successMsg);
            logToExtentReport(Status.PASS, successMsg);
            
            return new OperationResponse(true, successMsg, selectedText);
            
        } catch (Exception e) {
            String errorMsg = "Failed to select dropdown option: " + e.getMessage();
            log.error("{} - {}", methodName, errorMsg);
            captureScreenshot(methodName + "_failure");
            logToExtentReport(Status.FAIL, errorMsg);
            
            return new OperationResponse(false, errorMsg, null);
        }
    }
    
    /**
     * Scroll element into view with validation
     * 
     * @param locator Element locator
     * @return OperationResponse indicating success/failure
     */
    public static OperationResponse scrollIntoView(By locator) {
        Instant startTime = Instant.now();
        String methodName = "scrollIntoView";
        
        log.info("{} - Starting with locator: {}", methodName, locator);
        
        // Get element first (using presence, not visibility for scroll)
        try {
            WebDriverWait wait = new WebDriverWait(getDriver(), Duration.ofSeconds(DEFAULT_TIMEOUT));
            WebElement element = wait.until(ExpectedConditions.presenceOfElementLocated(locator));
            
            JavascriptExecutor js = (JavascriptExecutor) getDriver();
            js.executeScript("arguments[0].scrollIntoView({behavior: 'smooth', block: 'center'});", element);
            
            // Wait a moment for scroll to complete
            sleep(500);
            
            long executionTime = Duration.between(startTime, Instant.now()).toMillis();
            String successMsg = String.format("Element scrolled into view - Execution time: %dms", executionTime);
            
            log.info("{} - {}", methodName, successMsg);
            logToExtentReport(Status.PASS, successMsg);
            
            return new OperationResponse(true, successMsg, element);
            
        } catch (Exception e) {
            String errorMsg = "Failed to scroll element into view: " + e.getMessage();
            log.error("{} - {}", methodName, errorMsg);
            captureScreenshot(methodName + "_failure");
            logToExtentReport(Status.FAIL, errorMsg);
            
            return new OperationResponse(false, errorMsg, null);
        }
    }
    
    /**
     * Hover over element
     * 
     * @param locator Element locator
     * @return OperationResponse indicating success/failure
     */
    public static OperationResponse hoverOverElement(By locator) {
        Instant startTime = Instant.now();
        String methodName = "hoverOverElement";
        
        log.info("{} - Starting with locator: {}", methodName, locator);
        
        // Get element first
        OperationResponse elementResponse = waitForElementVisible(locator, DEFAULT_TIMEOUT);
        if (!elementResponse.isSuccess()) {
            return elementResponse;
        }
        
        WebElement element = (WebElement) elementResponse.getData();
        
        try {
            Actions actions = new Actions(getDriver());
            actions.moveToElement(element).perform();
            
            long executionTime = Duration.between(startTime, Instant.now()).toMillis();
            String successMsg = String.format("Hover action completed - Execution time: %dms", executionTime);
            
            log.info("{} - {}", methodName, successMsg);
            logToExtentReport(Status.PASS, successMsg);
            
            return new OperationResponse(true, successMsg, null);
            
        } catch (Exception e) {
            String errorMsg = "Failed to hover over element: " + e.getMessage();
            log.error("{} - {}", methodName, errorMsg);
            captureScreenshot(methodName + "_failure");
            logToExtentReport(Status.FAIL, errorMsg);
            
            return new OperationResponse(false, errorMsg, null);
        }
    }
    
    /**
     * Check if element is present on page
     * 
     * @param locator Element locator
     * @param timeoutSeconds Timeout for waiting
     * @return boolean indicating element presence
     */
    public static boolean isElementPresent(By locator, int timeoutSeconds) {
        String methodName = "isElementPresent";
        log.debug("{} - Checking presence of locator: {}", methodName, locator);
        
        try {
            WebDriverWait wait = new WebDriverWait(getDriver(), Duration.ofSeconds(timeoutSeconds));
            wait.until(ExpectedConditions.presenceOfElementLocated(locator));
            return true;
        } catch (TimeoutException e) {
            log.debug("{} - Element not present within {}s", methodName, timeoutSeconds);
            return false;
        }
    }
    
    /**
     * Get all elements matching locator
     * 
     * @param locator Element locator
     * @return OperationResponse with list of elements
     */
    public static OperationResponse getAllElements(By locator) {
        Instant startTime = Instant.now();
        String methodName = "getAllElements";
        
        log.info("{} - Starting with locator: {}", methodName, locator);
        
        try {
            List<WebElement> elements = getDriver().findElements(locator);
            
            long executionTime = Duration.between(startTime, Instant.now()).toMillis();
            String successMsg = String.format("Found %d elements - Execution time: %dms", 
                                             elements.size(), executionTime);
            
            log.info("{} - {}", methodName, successMsg);
            logToExtentReport(Status.INFO, successMsg);
            
            return new OperationResponse(true, successMsg, elements);
            
        } catch (Exception e) {
            String errorMsg = "Failed to get elements: " + e.getMessage();
            log.error("{} - {}", methodName, errorMsg);
            logToExtentReport(Status.FAIL, errorMsg);
            
            return new OperationResponse(false, errorMsg, null);
        }
    }
    
    // Utility methods
    
    /**
     * Capture screenshot for failure analysis
     */
    private static String captureScreenshot(String testName) {
        try {
            TakesScreenshot screenshot = (TakesScreenshot) getDriver();
            byte[] screenshotBytes = screenshot.getScreenshotAs(OutputType.BYTES);
            String fileName = testName + "_" + System.currentTimeMillis() + ".png";
            String filePath = "screenshots/" + fileName;
            
            File screenshotFile = new File(filePath);
            FileUtils.writeByteArrayToFile(screenshotFile, screenshotBytes);
            
            log.info("Screenshot captured: {}", filePath);
            return filePath;
        } catch (IOException e) {
            log.error("Failed to capture screenshot: {}", e.getMessage());
            return null;
        }
    }
    
    /**
     * Log to ExtentReport if available
     */
    private static void logToExtentReport(Status status, String message) {
        ExtentTest test = extentTest.get();
        if (test != null) {
            test.log(status, message);
        }
    }
    
    /**
     * Thread-safe sleep method
     */
    private static void sleep(long milliseconds) {
        try {
            Thread.sleep(milliseconds);
        } catch (InterruptedException e) {
            Thread.currentThread().interrupt();
            log.warn("Sleep interrupted: {}", e.getMessage());
        }
    }
    
    /**
     * Get integer configuration value
     */
    private static int getConfigInt(String key, int defaultValue) {
        if (config == null) {
            // In real implementation, load from properties file
            return defaultValue;
        }
        return Integer.parseInt(config.getProperty(key, String.valueOf(defaultValue)));
    }
    
    /**
     * Clean up method to remove ThreadLocal instances
     */
    public static void cleanup() {
        driver.remove();
        extentTest.remove();
    }
}