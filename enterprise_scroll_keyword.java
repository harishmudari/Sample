package com.automation.keywords;

import org.openqa.selenium.WebDriver;
import org.openqa.selenium.WebElement;
import org.openqa.selenium.JavascriptExecutor;
import org.openqa.selenium.By;
import org.openqa.selenium.support.ui.WebDriverWait;
import org.openqa.selenium.support.ui.ExpectedConditions;
import org.openqa.selenium.TimeoutException;
import org.openqa.selenium.NoSuchElementException;
import org.openqa.selenium.StaleElementReferenceException;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import java.time.Duration;
import java.util.Properties;
import java.io.InputStream;
import java.io.IOException;

/**
 * Enterprise-level ScrollToElementWhenNeeded Keyword
 * Handles scrolling to elements only when necessary with comprehensive error handling,
 * logging, retry mechanism, and fail-safe design.
 * 
 * @author QA Automation Team
 * @version 1.0
 * @since 2025-08-09
 */
public class ScrollToElementWhenNeeded {
    
    private static final Logger log = LoggerFactory.getLogger(ScrollToElementWhenNeeded.class);
    private final WebDriver driver;
    private final JavascriptExecutor jsExecutor;
    private final WebDriverWait wait;
    private final Properties config;
    private final ScreenshotHelper screenshotHelper;
    
    // Configuration constants loaded from properties
    private final int DEFAULT_TIMEOUT;
    private final int RETRY_COUNT;
    private final int SCROLL_PAUSE_MS;
    private final boolean ENABLE_SCREENSHOTS;
    
    /**
     * Constructor - Initializes the keyword with driver and configuration
     * 
     * @param driver WebDriver instance (should be ThreadLocal managed)
     * @param screenshotHelper Helper for capturing screenshots on failure
     * @throws ScrollKeywordException if initialization fails
     */
    public ScrollToElementWhenNeeded(WebDriver driver, ScreenshotHelper screenshotHelper) {
        long startTime = System.currentTimeMillis();
        
        // Fail-fast validation
        if (driver == null) {
            throw new ScrollKeywordException("WebDriver cannot be null");
        }
        if (screenshotHelper == null) {
            throw new ScrollKeywordException("ScreenshotHelper cannot be null");
        }
        
        this.driver = driver;
        this.jsExecutor = (JavascriptExecutor) driver;
        this.screenshotHelper = screenshotHelper;
        this.config = loadConfiguration();
        
        // Load configuration with defaults
        this.DEFAULT_TIMEOUT = Integer.parseInt(config.getProperty("scroll.timeout", "10"));
        this.RETRY_COUNT = Integer.parseInt(config.getProperty("scroll.retry.count", "3"));
        this.SCROLL_PAUSE_MS = Integer.parseInt(config.getProperty("scroll.pause.ms", "500"));
        this.ENABLE_SCREENSHOTS = Boolean.parseBoolean(config.getProperty("screenshots.enabled", "true"));
        
        this.wait = new WebDriverWait(driver, Duration.ofSeconds(DEFAULT_TIMEOUT));
        
        long endTime = System.currentTimeMillis();
        log.info("ScrollToElementWhenNeeded initialized successfully in {}ms", (endTime - startTime));
    }
    
    /**
     * Main method - Scrolls to element only when needed with comprehensive validation
     * Single Responsibility: Check viewport visibility and scroll if required
     * 
     * @param locator By locator to find the element
     * @return ScrollResult containing success status and details
     */
    public ScrollResult scrollToElementWhenNeeded(By locator) {
        String methodName = "scrollToElementWhenNeeded";
        long startTime = System.currentTimeMillis();
        
        log.info("{} - Starting execution with locator: {}", methodName, locator);
        
        // Fail-fast validation
        if (locator == null) {
            String errorMsg = "Locator cannot be null";
            log.error("{} - {}", methodName, errorMsg);
            return ScrollResult.failure(errorMsg);
        }
        
        try {
            // Find element with retry mechanism
            WebElement element = findElementWithRetry(locator);
            if (element == null) {
                return handleElementNotFound(methodName, locator);
            }
            
            // Check if element is already in viewport
            boolean isInViewport = isElementInViewport(element);
            log.debug("{} - Element in viewport check: {}", methodName, isInViewport);
            
            ScrollResult result;
            if (!isInViewport) {
                result = performScrollToElement(element, locator);
            } else {
                result = ScrollResult.success("Element already in viewport, no scroll needed");
                log.info("{} - Element already visible, skipping scroll", methodName);
            }
            
            long endTime = System.currentTimeMillis();
            log.info("{} - Completed in {}ms with result: {}", methodName, (endTime - startTime), result.getStatus());
            
            return result;
            
        } catch (ScrollKeywordException e) {
            return handleScrollException(methodName, locator, e);
        } catch (Exception e) {
            return handleUnexpectedException(methodName, locator, e);
        }
    }
    
    /**
     * Overloaded method - Scrolls to WebElement when needed
     * 
     * @param element WebElement to scroll to
     * @param elementDescription Description for logging purposes
     * @return ScrollResult containing success status and details
     */
    public ScrollResult scrollToElementWhenNeeded(WebElement element, String elementDescription) {
        String methodName = "scrollToElementWhenNeeded(WebElement)";
        long startTime = System.currentTimeMillis();
        
        log.info("{} - Starting execution for element: {}", methodName, elementDescription);
        
        // Fail-fast validation
        if (element == null) {
            String errorMsg = "WebElement cannot be null";
            log.error("{} - {}", methodName, errorMsg);
            return ScrollResult.failure(errorMsg);
        }
        
        try {
            // Validate element is still attached to DOM
            if (!isElementAttachedToDom(element)) {
                throw new ScrollKeywordException("Element is stale or not attached to DOM: " + elementDescription);
            }
            
            // Check viewport visibility
            boolean isInViewport = isElementInViewport(element);
            log.debug("{} - Element in viewport check: {}", methodName, isInViewport);
            
            ScrollResult result;
            if (!isInViewport) {
                result = performScrollToElement(element, elementDescription);
            } else {
                result = ScrollResult.success("Element already in viewport, no scroll needed");
                log.info("{} - Element already visible, skipping scroll", methodName);
            }
            
            long endTime = System.currentTimeMillis();
            log.info("{} - Completed in {}ms with result: {}", methodName, (endTime - startTime), result.getStatus());
            
            return result;
            
        } catch (ScrollKeywordException e) {
            return handleScrollException(methodName, elementDescription, e);
        } catch (Exception e) {
            return handleUnexpectedException(methodName, elementDescription, e);
        }
    }
    
    /**
     * Advanced method - Scrolls with custom options
     * 
     * @param locator By locator to find the element
     * @param scrollOptions Custom scroll configuration
     * @return ScrollResult containing success status and details
     */
    public ScrollResult scrollToElementWhenNeeded(By locator, ScrollOptions scrollOptions) {
        String methodName = "scrollToElementWhenNeeded(withOptions)";
        long startTime = System.currentTimeMillis();
        
        log.info("{} - Starting execution with locator: {} and options: {}", 
                methodName, locator, scrollOptions);
        
        // Fail-fast validation
        if (locator == null || scrollOptions == null) {
            String errorMsg = "Locator and ScrollOptions cannot be null";
            log.error("{} - {}", methodName, errorMsg);
            return ScrollResult.failure(errorMsg);
        }
        
        try {
            WebElement element = findElementWithRetry(locator);
            if (element == null) {
                return handleElementNotFound(methodName, locator);
            }
            
            // Check viewport with custom margin
            boolean isInViewport = isElementInViewportWithMargin(element, scrollOptions.getViewportMargin());
            log.debug("{} - Element in viewport check (margin: {}px): {}", 
                    methodName, scrollOptions.getViewportMargin(), isInViewport);
            
            ScrollResult result;
            if (!isInViewport) {
                result = performScrollToElementWithOptions(element, locator, scrollOptions);
            } else {
                result = ScrollResult.success("Element already in viewport with specified margin, no scroll needed");
                log.info("{} - Element already visible with margin, skipping scroll", methodName);
            }
            
            long endTime = System.currentTimeMillis();
            log.info("{} - Completed in {}ms with result: {}", methodName, (endTime - startTime), result.getStatus());
            
            return result;
            
        } catch (ScrollKeywordException e) {
            return handleScrollException(methodName, locator, e);
        } catch (Exception e) {
            return handleUnexpectedException(methodName, locator, e);
        }
    }
    
    /**
     * Finds element with retry mechanism
     * Fail-safe design with multiple attempts
     */
    private WebElement findElementWithRetry(By locator) {
        for (int attempt = 1; attempt <= RETRY_COUNT; attempt++) {
            try {
                log.debug("Finding element attempt {}/{} for locator: {}", attempt, RETRY_COUNT, locator);
                return wait.until(ExpectedConditions.presenceOfElementLocated(locator));
            } catch (TimeoutException e) {
                log.warn("Element not found on attempt {}/{} for locator: {}", attempt, RETRY_COUNT, locator);
                if (attempt == RETRY_COUNT) {
                    log.error("Element not found after {} attempts for locator: {}", RETRY_COUNT, locator);
                    return null;
                }
                // Brief pause before retry
                pauseExecution(500);
            }
        }
        return null;
    }
    
    /**
     * Checks if element is currently visible in viewport
     * Uses JavaScript to determine viewport visibility
     */
    private boolean isElementInViewport(WebElement element) {
        try {
            String script = 
                "var rect = arguments[0].getBoundingClientRect();" +
                "var windowHeight = window.innerHeight || document.documentElement.clientHeight;" +
                "var windowWidth = window.innerWidth || document.documentElement.clientWidth;" +
                "return (rect.top >= 0 && rect.left >= 0 && " +
                "rect.bottom <= windowHeight && rect.right <= windowWidth);";
                
            Boolean result = (Boolean) jsExecutor.executeScript(script, element);
            return result != null ? result : false;
        } catch (Exception e) {
            log.warn("Error checking viewport visibility, assuming element is not visible: {}", e.getMessage());
            return false;
        }
    }
    
    /**
     * Checks viewport visibility with custom margin
     */
    private boolean isElementInViewportWithMargin(WebElement element, int margin) {
        try {
            String script = 
                "var rect = arguments[0].getBoundingClientRect();" +
                "var margin = arguments[1];" +
                "var windowHeight = window.innerHeight || document.documentElement.clientHeight;" +
                "var windowWidth = window.innerWidth || document.documentElement.clientWidth;" +
                "return (rect.top >= -margin && rect.left >= -margin && " +
                "rect.bottom <= windowHeight + margin && rect.right <= windowWidth + margin);";
                
            Boolean result = (Boolean) jsExecutor.executeScript(script, element, margin);
            return result != null ? result : false;
        } catch (Exception e) {
            log.warn("Error checking viewport visibility with margin, assuming element is not visible: {}", e.getMessage());
            return false;
        }
    }
    
    /**
     * Checks if element is still attached to DOM
     */
    private boolean isElementAttachedToDom(WebElement element) {
        try {
            element.isDisplayed(); // This will throw if element is stale
            return true;
        } catch (StaleElementReferenceException e) {
            log.warn("Element is stale/detached from DOM");
            return false;
        }
    }
    
    /**
     * Performs the actual scroll operation with retry mechanism
     */
    private ScrollResult performScrollToElement(WebElement element, Object locatorOrDescription) {
        for (int attempt = 1; attempt <= RETRY_COUNT; attempt++) {
            try {
                log.debug("Scroll attempt {}/{} for: {}", attempt, RETRY_COUNT, locatorOrDescription);
                
                // Perform scroll using JavaScript
                jsExecutor.executeScript("arguments[0].scrollIntoView({behavior: 'smooth', block: 'center'});", element);
                
                // Wait for scroll to complete
                pauseExecution(SCROLL_PAUSE_MS);
                
                // Verify scroll was successful
                if (isElementInViewport(element)) {
                    log.info("Successfully scrolled to element: {}", locatorOrDescription);
                    return ScrollResult.success("Element successfully scrolled into view");
                } else {
                    log.warn("Scroll attempt {} failed - element still not in viewport: {}", attempt, locatorOrDescription);
                }
                
            } catch (Exception e) {
                log.warn("Scroll attempt {} failed with exception: {}", attempt, e.getMessage());
                if (attempt == RETRY_COUNT) {
                    throw new ScrollKeywordException("Failed to scroll after " + RETRY_COUNT + " attempts", e);
                }
            }
        }
        
        throw new ScrollKeywordException("Failed to scroll element into viewport after " + RETRY_COUNT + " attempts");
    }
    
    /**
     * Performs scroll with custom options
     */
    private ScrollResult performScrollToElementWithOptions(WebElement element, Object locatorOrDescription, ScrollOptions options) {
        for (int attempt = 1; attempt <= RETRY_COUNT; attempt++) {
            try {
                log.debug("Scroll attempt {}/{} with custom options for: {}", attempt, RETRY_COUNT, locatorOrDescription);
                
                String script = String.format(
                    "arguments[0].scrollIntoView({behavior: '%s', block: '%s', inline: '%s'});",
                    options.getBehavior(), options.getBlock(), options.getInline()
                );
                
                jsExecutor.executeScript(script, element);
                
                // Wait based on behavior type
                int waitTime = "smooth".equals(options.getBehavior()) ? 1000 : SCROLL_PAUSE_MS;
                pauseExecution(waitTime);
                
                // Verify with margin
                if (isElementInViewportWithMargin(element, options.getViewportMargin())) {
                    log.info("Successfully scrolled to element with custom options: {}", locatorOrDescription);
                    return ScrollResult.success("Element successfully scrolled into view with custom options");
                }
                
            } catch (Exception e) {
                log.warn("Custom scroll attempt {} failed: {}", attempt, e.getMessage());
                if (attempt == RETRY_COUNT) {
                    throw new ScrollKeywordException("Failed to scroll with custom options after " + RETRY_COUNT + " attempts", e);
                }
            }
        }
        
        throw new ScrollKeywordException("Failed to scroll element with custom options after " + RETRY_COUNT + " attempts");
    }
    
    /**
     * Handles element not found scenario
     */
    private ScrollResult handleElementNotFound(String methodName, Object locatorOrDescription) {
        String errorMsg = "Element not found: " + locatorOrDescription;
        log.error("{} - {}", methodName, errorMsg);
        
        if (ENABLE_SCREENSHOTS) {
            screenshotHelper.captureScreenshot("element_not_found_" + System.currentTimeMillis());
        }
        
        return ScrollResult.failure(errorMsg);
    }
    
    /**
     * Handles scroll-specific exceptions
     */
    private ScrollResult handleScrollException(String methodName, Object locatorOrDescription, ScrollKeywordException e) {
        String errorMsg = String.format("Scroll operation failed for %s: %s", locatorOrDescription, e.getMessage());
        log.error("{} - {}", methodName, errorMsg, e);
        
        if (ENABLE_SCREENSHOTS) {
            screenshotHelper.captureScreenshot("scroll_failed_" + System.currentTimeMillis());
        }
        
        return ScrollResult.failure(errorMsg);
    }
    
    /**
     * Handles unexpected exceptions
     */
    private ScrollResult handleUnexpectedException(String methodName, Object locatorOrDescription, Exception e) {
        String errorMsg = String.format("Unexpected error during scroll operation for %s: %s", locatorOrDescription, e.getMessage());
        log.error("{} - {}", methodName, errorMsg, e);
        
        if (ENABLE_SCREENSHOTS) {
            screenshotHelper.captureScreenshot("scroll_unexpected_error_" + System.currentTimeMillis());
        }
        
        return ScrollResult.failure(errorMsg);
    }
    
    /**
     * Thread-safe pause execution
     */
    private void pauseExecution(int milliseconds) {
        try {
            Thread.sleep(milliseconds);
        } catch (InterruptedException e) {
            Thread.currentThread().interrupt();
            log.warn("Thread interrupted during pause: {}", e.getMessage());
        }
    }
    
    /**
     * Loads configuration from properties file
     */
    private Properties loadConfiguration() {
        Properties props = new Properties();
        try (InputStream input = getClass().getClassLoader().getResourceAsStream("scroll-config.properties")) {
            if (input != null) {
                props.load(input);
                log.info("Configuration loaded successfully");
            } else {
                log.warn("Configuration file not found, using default values");
            }
        } catch (IOException e) {
            log.warn("Error loading configuration, using default values: {}", e.getMessage());
        }
        return props;
    }
}

/**
 * Custom exception for scroll-related failures
 */
class ScrollKeywordException extends RuntimeException {
    public ScrollKeywordException(String message) {
        super(message);
    }
    
    public ScrollKeywordException(String message, Throwable cause) {
        super(message, cause);
    }
}

/**
 * Result object for scroll operations
 * Returns meaningful information about the operation
 */
class ScrollResult {
    private final boolean success;
    private final String message;
    private final long timestamp;
    
    private ScrollResult(boolean success, String message) {
        this.success = success;
        this.message = message;
        this.timestamp = System.currentTimeMillis();
    }
    
    public static ScrollResult success(String message) {
        return new ScrollResult(true, message);
    }
    
    public static ScrollResult failure(String message) {
        return new ScrollResult(false, message);
    }
    
    public boolean isSuccess() { return success; }
    public String getMessage() { return message; }
    public String getStatus() { return success ? "SUCCESS" : "FAILURE"; }
    public long getTimestamp() { return timestamp; }
    
    @Override
    public String toString() {
        return String.format("ScrollResult{status=%s, message='%s', timestamp=%d}", 
                getStatus(), message, timestamp);
    }
}

/**
 * Configuration object for custom scroll options
 */
class ScrollOptions {
    private String behavior = "smooth";  // 'auto' or 'smooth'
    private String block = "center";     // 'start', 'center', 'end', 'nearest'
    private String inline = "center";    // 'start', 'center', 'end', 'nearest'
    private int viewportMargin = 0;      // Pixel margin for viewport check
    
    // Builder pattern for easy configuration
    public static ScrollOptions builder() {
        return new ScrollOptions();
    }
    
    public ScrollOptions behavior(String behavior) {
        this.behavior = behavior;
        return this;
    }
    
    public ScrollOptions block(String block) {
        this.block = block;
        return this;
    }
    
    public ScrollOptions inline(String inline) {
        this.inline = inline;
        return this;
    }
    
    public ScrollOptions viewportMargin(int margin) {
        this.viewportMargin = margin;
        return this;
    }
    
    // Getters
    public String getBehavior() { return behavior; }
    public String getBlock() { return block; }
    public String getInline() { return inline; }
    public int getViewportMargin() { return viewportMargin; }
    
    @Override
    public String toString() {
        return String.format("ScrollOptions{behavior='%s', block='%s', inline='%s', margin=%d}",
                behavior, block, inline, viewportMargin);
    }
}

/**
 * Helper interface for screenshot functionality
 * Allows plugging in different screenshot implementations
 */
interface ScreenshotHelper {
    void captureScreenshot(String filename);
}

/**
 * Example usage and testing class
 */
class ScrollToElementWhenNeededExample {
    
    public static void demonstrateUsage(WebDriver driver, ScreenshotHelper screenshotHelper) {
        ScrollToElementWhenNeeded scrollKeyword = new ScrollToElementWhenNeeded(driver, screenshotHelper);
        
        // Example 1: Basic usage with locator
        ScrollResult result1 = scrollKeyword.scrollToElementWhenNeeded(By.id("submit-button"));
        if (result1.isSuccess()) {
            System.out.println("Scroll successful: " + result1.getMessage());
        } else {
            System.err.println("Scroll failed: " + result1.getMessage());
        }
        
        // Example 2: Usage with WebElement
        WebElement element = driver.findElement(By.className("footer-link"));
        ScrollResult result2 = scrollKeyword.scrollToElementWhenNeeded(element, "Footer Link");
        
        // Example 3: Usage with custom options
        ScrollOptions options = ScrollOptions.builder()
                .behavior("smooth")
                .block("start")
                .viewportMargin(50)
                .build();
        
        ScrollResult result3 = scrollKeyword.scrollToElementWhenNeeded(By.tagName("h1"), options);
        
        // Example 4: Chain with other actions
        ScrollResult scrollResult = scrollKeyword.scrollToElementWhenNeeded(By.id("login-btn"));
        if (scrollResult.isSuccess()) {
            driver.findElement(By.id("login-btn")).click();
        }
    }
}