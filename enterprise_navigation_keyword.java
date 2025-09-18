package com.automation.keywords;

import org.openqa.selenium.WebDriver;
import org.openqa.selenium.TimeoutException;
import org.openqa.selenium.WebDriverException;
import org.openqa.selenium.support.ui.WebDriverWait;
import org.openqa.selenium.support.ui.ExpectedConditions;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import java.time.Duration;
import java.util.Properties;
import java.io.InputStream;
import java.io.IOException;
import java.net.URL;
import java.net.MalformedURLException;
import java.util.regex.Pattern;

/**
 * Enterprise-level Browser Navigation Keyword
 * Handles all browser navigation operations with comprehensive error handling,
 * validation, retry mechanism, and fail-safe design.
 * 
 * @author QA Automation Team
 * @version 1.0
 * @since 2025-08-09
 */
public class BrowserNavigationKeyword {
    
    private static final Logger log = LoggerFactory.getLogger(BrowserNavigationKeyword.class);
    private final WebDriver driver;
    private final WebDriverWait wait;
    private final Properties config;
    private final ScreenshotHelper screenshotHelper;
    
    // Configuration constants loaded from properties
    private final int DEFAULT_TIMEOUT;
    private final int PAGE_LOAD_TIMEOUT;
    private final int RETRY_COUNT;
    private final int NAVIGATION_PAUSE_MS;
    private final boolean ENABLE_SCREENSHOTS;
    private final boolean VALIDATE_SSL;
    private final String BASE_URL;
    
    // URL validation patterns
    private static final Pattern URL_PATTERN = Pattern.compile(
        "^(https?|ftp|file)://[-a-zA-Z0-9+&@#/%?=~_|!:,.;]*[-a-zA-Z0-9+&@#/%=~_|]"
    );
    
    /**
     * Constructor - Initializes the navigation keyword with driver and configuration
     * 
     * @param driver WebDriver instance (should be ThreadLocal managed)
     * @param screenshotHelper Helper for capturing screenshots on failure
     * @throws NavigationKeywordException if initialization fails
     */
    public BrowserNavigationKeyword(WebDriver driver, ScreenshotHelper screenshotHelper) {
        long startTime = System.currentTimeMillis();
        
        // Fail-fast validation
        if (driver == null) {
            throw new NavigationKeywordException("WebDriver cannot be null");
        }
        if (screenshotHelper == null) {
            throw new NavigationKeywordException("ScreenshotHelper cannot be null");
        }
        
        this.driver = driver;
        this.screenshotHelper = screenshotHelper;
        this.config = loadConfiguration();
        
        // Load configuration with defaults
        this.DEFAULT_TIMEOUT = Integer.parseInt(config.getProperty("navigation.timeout", "30"));
        this.PAGE_LOAD_TIMEOUT = Integer.parseInt(config.getProperty("navigation.page.load.timeout", "60"));
        this.RETRY_COUNT = Integer.parseInt(config.getProperty("navigation.retry.count", "3"));
        this.NAVIGATION_PAUSE_MS = Integer.parseInt(config.getProperty("navigation.pause.ms", "1000"));
        this.ENABLE_SCREENSHOTS = Boolean.parseBoolean(config.getProperty("screenshots.enabled", "true"));
        this.VALIDATE_SSL = Boolean.parseBoolean(config.getProperty("navigation.validate.ssl", "true"));
        this.BASE_URL = config.getProperty("base.url", "");
        
        this.wait = new WebDriverWait(driver, Duration.ofSeconds(DEFAULT_TIMEOUT));
        
        // Set page load timeout
        driver.manage().timeouts().pageLoadTimeout(Duration.ofSeconds(PAGE_LOAD_TIMEOUT));
        
        long endTime = System.currentTimeMillis();
        log.info("BrowserNavigationKeyword initialized successfully in {}ms", (endTime - startTime));
    }
    
    /**
     * Navigate to URL with comprehensive validation and error handling
     * Single Responsibility: Navigate to a specific URL
     * 
     * @param url Target URL to navigate to
     * @return NavigationResult containing success status and details
     */
    public NavigationResult navigateTo(String url) {
        String methodName = "navigateTo";
        long startTime = System.currentTimeMillis();
        
        log.info("{} - Starting navigation to URL: {}", methodName, url);
        
        // Fail-fast validation
        if (url == null || url.trim().isEmpty()) {
            String errorMsg = "URL cannot be null or empty";
            log.error("{} - {}", methodName, errorMsg);
            return NavigationResult.failure(errorMsg);
        }
        
        try {
            // Normalize and validate URL
            String normalizedUrl = normalizeUrl(url.trim());
            if (!isValidUrl(normalizedUrl)) {
                String errorMsg = "Invalid URL format: " + normalizedUrl;
                log.error("{} - {}", methodName, errorMsg);
                return NavigationResult.failure(errorMsg);
            }
            
            // Perform navigation with retry mechanism
            NavigationResult result = performNavigationWithRetry(normalizedUrl, methodName);
            
            long endTime = System.currentTimeMillis();
            log.info("{} - Completed in {}ms with result: {} for URL: {}", 
                    methodName, (endTime - startTime), result.getStatus(), normalizedUrl);
            
            return result;
            
        } catch (NavigationKeywordException e) {
            return handleNavigationException(methodName, url, e);
        } catch (Exception e) {
            return handleUnexpectedException(methodName, url, e);
        }
    }
    
    /**
     * Navigate to relative URL (appends to base URL)
     * 
     * @param relativePath Relative path to append to base URL
     * @return NavigationResult containing success status and details
     */
    public NavigationResult navigateToRelativePath(String relativePath) {
        String methodName = "navigateToRelativePath";
        log.info("{} - Starting navigation to relative path: {}", methodName, relativePath);
        
        // Fail-fast validation
        if (relativePath == null) {
            String errorMsg = "Relative path cannot be null";
            log.error("{} - {}", methodName, errorMsg);
            return NavigationResult.failure(errorMsg);
        }
        
        if (BASE_URL.isEmpty()) {
            String errorMsg = "Base URL not configured for relative navigation";
            log.error("{} - {}", methodName, errorMsg);
            return NavigationResult.failure(errorMsg);
        }
        
        // Construct full URL
        String fullUrl = BASE_URL + (relativePath.startsWith("/") ? relativePath : "/" + relativePath);
        log.debug("{} - Constructed full URL: {}", methodName, fullUrl);
        
        return navigateTo(fullUrl);
    }
    
    /**
     * Navigate back in browser history
     * Single Responsibility: Go back one step in browser history
     * 
     * @return NavigationResult containing success status and details
     */
    public NavigationResult navigateBack() {
        String methodName = "navigateBack";
        long startTime = System.currentTimeMillis();
        
        log.info("{} - Starting back navigation", methodName);
        
        try {
            String currentUrl = getCurrentUrl();
            log.debug("{} - Current URL before back navigation: {}", methodName, currentUrl);
            
            // Perform back navigation with retry
            NavigationResult result = performBackNavigationWithRetry();
            
            if (result.isSuccess()) {
                String newUrl = getCurrentUrl();
                log.info("{} - Successfully navigated back from {} to {}", methodName, currentUrl, newUrl);
                result = NavigationResult.success("Successfully navigated back to: " + newUrl);
            }
            
            long endTime = System.currentTimeMillis();
            log.info("{} - Completed in {}ms with result: {}", methodName, (endTime - startTime), result.getStatus());
            
            return result;
            
        } catch (NavigationKeywordException e) {
            return handleNavigationException(methodName, "back", e);
        } catch (Exception e) {
            return handleUnexpectedException(methodName, "back", e);
        }
    }
    
    /**
     * Navigate forward in browser history
     * Single Responsibility: Go forward one step in browser history
     * 
     * @return NavigationResult containing success status and details
     */
    public NavigationResult navigateForward() {
        String methodName = "navigateForward";
        long startTime = System.currentTimeMillis();
        
        log.info("{} - Starting forward navigation", methodName);
        
        try {
            String currentUrl = getCurrentUrl();
            log.debug("{} - Current URL before forward navigation: {}", methodName, currentUrl);
            
            // Perform forward navigation with retry
            NavigationResult result = performForwardNavigationWithRetry();
            
            if (result.isSuccess()) {
                String newUrl = getCurrentUrl();
                log.info("{} - Successfully navigated forward from {} to {}", methodName, currentUrl, newUrl);
                result = NavigationResult.success("Successfully navigated forward to: " + newUrl);
            }
            
            long endTime = System.currentTimeMillis();
            log.info("{} - Completed in {}ms with result: {}", methodName, (endTime - startTime), result.getStatus());
            
            return result;
            
        } catch (NavigationKeywordException e) {
            return handleNavigationException(methodName, "forward", e);
        } catch (Exception e) {
            return handleUnexpectedException(methodName, "forward", e);
        }
    }
    
    /**
     * Refresh/reload the current page
     * Single Responsibility: Refresh the current page
     * 
     * @return NavigationResult containing success status and details
     */
    public NavigationResult refreshPage() {
        String methodName = "refreshPage";
        long startTime = System.currentTimeMillis();
        
        log.info("{} - Starting page refresh", methodName);
        
        try {
            String currentUrl = getCurrentUrl();
            log.debug("{} - Refreshing page: {}", methodName, currentUrl);
            
            // Perform refresh with retry mechanism
            NavigationResult result = performRefreshWithRetry();
            
            long endTime = System.currentTimeMillis();
            log.info("{} - Completed in {}ms with result: {} for URL: {}", 
                    methodName, (endTime - startTime), result.getStatus(), currentUrl);
            
            return result;
            
        } catch (NavigationKeywordException e) {
            return handleNavigationException(methodName, "refresh", e);
        } catch (Exception e) {
            return handleUnexpectedException(methodName, "refresh", e);
        }
    }
    
    /**
     * Get current URL with error handling
     * 
     * @return Current URL or error message if unable to retrieve
     */
    public String getCurrentUrl() {
        try {
            return driver.getCurrentUrl();
        } catch (Exception e) {
            log.warn("Unable to get current URL: {}", e.getMessage());
            return "URL_UNAVAILABLE";
        }
    }
    
    /**
     * Get page title with error handling
     * 
     * @return Current page title or error message if unable to retrieve
     */
    public String getPageTitle() {
        try {
            return driver.getTitle();
        } catch (Exception e) {
            log.warn("Unable to get page title: {}", e.getMessage());
            return "TITLE_UNAVAILABLE";
        }
    }
    
    /**
     * Wait for page to be fully loaded
     * 
     * @return NavigationResult indicating if page loaded successfully
     */
    public NavigationResult waitForPageLoad() {
        String methodName = "waitForPageLoad";
        long startTime = System.currentTimeMillis();
        
        log.info("{} - Waiting for page to load completely", methodName);
        
        try {
            // Wait for document ready state
            wait.until(driver -> 
                ((org.openqa.selenium.JavascriptExecutor) driver)
                    .executeScript("return document.readyState").equals("complete")
            );
            
            long endTime = System.currentTimeMillis();
            log.info("{} - Page loaded successfully in {}ms", methodName, (endTime - startTime));
            
            return NavigationResult.success("Page loaded successfully");
            
        } catch (TimeoutException e) {
            String errorMsg = "Page load timeout exceeded: " + DEFAULT_TIMEOUT + " seconds";
            log.error("{} - {}", methodName, errorMsg);
            return NavigationResult.failure(errorMsg);
        } catch (Exception e) {
            return handleUnexpectedException(methodName, "pageLoad", e);
        }
    }
    
    /**
     * Navigate with page load wait
     * Combines navigation and page load wait
     * 
     * @param url Target URL
     * @return NavigationResult with comprehensive status
     */
    public NavigationResult navigateToAndWaitForLoad(String url) {
        NavigationResult navResult = navigateTo(url);
        if (navResult.isSuccess()) {
            NavigationResult loadResult = waitForPageLoad();
            if (!loadResult.isSuccess()) {
                return NavigationResult.failure("Navigation successful but page load failed: " + loadResult.getMessage());
            }
        }
        return navResult;
    }
    
    /**
     * Normalizes URL by adding protocol if missing
     */
    private String normalizeUrl(String url) {
        if (!url.toLowerCase().startsWith("http://") && 
            !url.toLowerCase().startsWith("https://") && 
            !url.toLowerCase().startsWith("file://")) {
            return "https://" + url;
        }
        return url;
    }
    
    /**
     * Validates URL format and SSL if configured
     */
    private boolean isValidUrl(String url) {
        try {
            // Basic URL format validation
            if (!URL_PATTERN.matcher(url).matches()) {
                return false;
            }
            
            // Create URL object for additional validation
            URL urlObj = new URL(url);
            
            // SSL validation if enabled
            if (VALIDATE_SSL && url.toLowerCase().startsWith("http://")) {
                log.warn("HTTP URL detected, consider using HTTPS: {}", url);
            }
            
            return true;
            
        } catch (MalformedURLException e) {
            log.error("Malformed URL: {}", url, e);
            return false;
        }
    }
    
    /**
     * Performs navigation with retry mechanism
     */
    private NavigationResult performNavigationWithRetry(String url, String operation) {
        Exception lastException = null;
        
        for (int attempt = 1; attempt <= RETRY_COUNT; attempt++) {
            try {
                log.debug("{} - Navigation attempt {}/{} for URL: {}", operation, attempt, RETRY_COUNT, url);
                
                // Perform navigation
                driver.navigate().to(url);
                
                // Wait for navigation to complete
                pauseExecution(NAVIGATION_PAUSE_MS);
                
                // Verify navigation was successful
                String currentUrl = getCurrentUrl();
                if (isNavigationSuccessful(url, currentUrl)) {
                    log.info("{} - Navigation successful to: {}", operation, url);
                    return NavigationResult.success("Successfully navigated to: " + url);
                } else {
                    log.warn("{} - Navigation attempt {} appears unsuccessful. Expected: {}, Actual: {}", 
                            operation, attempt, url, currentUrl);
                }
                
            } catch (TimeoutException e) {
                lastException = e;
                log.warn("{} - Navigation attempt {} timed out for URL: {}", operation, attempt, url);
            } catch (WebDriverException e) {
                lastException = e;
                log.warn("{} - Navigation attempt {} failed with WebDriver exception: {}", 
                        operation, attempt, e.getMessage());
            }
            
            if (attempt < RETRY_COUNT) {
                pauseExecution(1000); // Wait before retry
            }
        }
        
        throw new NavigationKeywordException(
            String.format("Navigation failed after %d attempts for URL: %s", RETRY_COUNT, url), 
            lastException
        );
    }
    
    /**
     * Performs back navigation with retry
     */
    private NavigationResult performBackNavigationWithRetry() {
        Exception lastException = null;
        String originalUrl = getCurrentUrl();
        
        for (int attempt = 1; attempt <= RETRY_COUNT; attempt++) {
            try {
                log.debug("Back navigation attempt {}/{}", attempt, RETRY_COUNT);
                
                driver.navigate().back();
                pauseExecution(NAVIGATION_PAUSE_MS);
                
                String newUrl = getCurrentUrl();
                if (!newUrl.equals(originalUrl)) {
                    return NavigationResult.success("Successfully navigated back");
                } else {
                    log.warn("Back navigation attempt {} - URL unchanged: {}", attempt, originalUrl);
                }
                
            } catch (Exception e) {
                lastException = e;
                log.warn("Back navigation attempt {} failed: {}", attempt, e.getMessage());
            }
            
            if (attempt < RETRY_COUNT) {
                pauseExecution(1000);
            }
        }
        
        throw new NavigationKeywordException("Back navigation failed after " + RETRY_COUNT + " attempts", lastException);
    }
    
    /**
     * Performs forward navigation with retry
     */
    private NavigationResult performForwardNavigationWithRetry() {
        Exception lastException = null;
        String originalUrl = getCurrentUrl();
        
        for (int attempt = 1; attempt <= RETRY_COUNT; attempt++) {
            try {
                log.debug("Forward navigation attempt {}/{}", attempt, RETRY_COUNT);
                
                driver.navigate().forward();
                pauseExecution(NAVIGATION_PAUSE_MS);
                
                String newUrl = getCurrentUrl();
                if (!newUrl.equals(originalUrl)) {
                    return NavigationResult.success("Successfully navigated forward");
                } else {
                    log.warn("Forward navigation attempt {} - URL unchanged: {}", attempt, originalUrl);
                }
                
            } catch (Exception e) {
                lastException = e;
                log.warn("Forward navigation attempt {} failed: {}", attempt, e.getMessage());
            }
            
            if (attempt < RETRY_COUNT) {
                pauseExecution(1000);
            }
        }
        
        throw new NavigationKeywordException("Forward navigation failed after " + RETRY_COUNT + " attempts", lastException);
    }
    
    /**
     * Performs page refresh with retry
     */
    private NavigationResult performRefreshWithRetry() {
        Exception lastException = null;
        String originalUrl = getCurrentUrl();
        
        for (int attempt = 1; attempt <= RETRY_COUNT; attempt++) {
            try {
                log.debug("Refresh attempt {}/{} for URL: {}", attempt, RETRY_COUNT, originalUrl);
                
                driver.navigate().refresh();
                pauseExecution(NAVIGATION_PAUSE_MS);
                
                // Verify page is responsive after refresh
                String title = getPageTitle();
                if (!"TITLE_UNAVAILABLE".equals(title)) {
                    return NavigationResult.success("Page refreshed successfully");
                }
                
            } catch (Exception e) {
                lastException = e;
                log.warn("Refresh attempt {} failed: {}", attempt, e.getMessage());
            }
            
            if (attempt < RETRY_COUNT) {
                pauseExecution(1000);
            }
        }
        
        throw new NavigationKeywordException("Page refresh failed after " + RETRY_COUNT + " attempts", lastException);
    }
    
    /**
     * Checks if navigation was successful by comparing URLs
     */
    private boolean isNavigationSuccessful(String expectedUrl, String actualUrl) {
        if (actualUrl == null || "URL_UNAVAILABLE".equals(actualUrl)) {
            return false;
        }
        
        // Handle redirects and URL variations
        try {
            URL expected = new URL(expectedUrl);
            URL actual = new URL(actualUrl);
            
            // Compare host and path (ignore query parameters and fragments for basic check)
            return expected.getHost().equals(actual.getHost()) && 
                   expected.getPath().equals(actual.getPath());
                   
        } catch (MalformedURLException e) {
            log.warn("Error comparing URLs for navigation verification: {}", e.getMessage());
            return actualUrl.contains(expectedUrl) || expectedUrl.contains(actualUrl);
        }
    }
    
    /**
     * Handles navigation-specific exceptions
     */
    private NavigationResult handleNavigationException(String methodName, Object target, NavigationKeywordException e) {
        String errorMsg = String.format("Navigation operation failed for %s in %s: %s", target, methodName, e.getMessage());
        log.error("{} - {}", methodName, errorMsg, e);
        
        if (ENABLE_SCREENSHOTS) {
            screenshotHelper.captureScreenshot("navigation_failed_" + methodName + "_" + System.currentTimeMillis());
        }
        
        return NavigationResult.failure(errorMsg);
    }
    
    /**
     * Handles unexpected exceptions
     */
    private NavigationResult handleUnexpectedException(String methodName, Object target, Exception e) {
        String errorMsg = String.format("Unexpected error during navigation operation for %s in %s: %s", 
                target, methodName, e.getMessage());
        log.error("{} - {}", methodName, errorMsg, e);
        
        if (ENABLE_SCREENSHOTS) {
            screenshotHelper.captureScreenshot("navigation_unexpected_error_" + methodName + "_" + System.currentTimeMillis());
        }
        
        return NavigationResult.failure(errorMsg);
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
        try (InputStream input = getClass().getClassLoader().getResourceAsStream("navigation-config.properties")) {
            if (input != null) {
                props.load(input);
                log.info("Navigation configuration loaded successfully");
            } else {
                log.warn("Navigation configuration file not found, using default values");
            }
        } catch (IOException e) {
            log.warn("Error loading navigation configuration, using default values: {}", e.getMessage());
        }
        return props;
    }
}

/**
 * Custom exception for navigation-related failures
 */
class NavigationKeywordException extends RuntimeException {
    public NavigationKeywordException(String message) {
        super(message);
    }
    
    public NavigationKeywordException(String message, Throwable cause) {
        super(message, cause);
    }
}

/**
 * Result object for navigation operations
 * Returns meaningful information about the operation
 */
class NavigationResult {
    private final boolean success;
    private final String message;
    private final long timestamp;
    private final String currentUrl;
    
    private NavigationResult(boolean success, String message, String currentUrl) {
        this.success = success;
        this.message = message;
        this.currentUrl = currentUrl;
        this.timestamp = System.currentTimeMillis();
    }
    
    public static NavigationResult success(String message) {
        return new NavigationResult(true, message, null);
    }
    
    public static NavigationResult failure(String message) {
        return new NavigationResult(false, message, null);
    }
    
    public static NavigationResult successWithUrl(String message, String currentUrl) {
        return new NavigationResult(true, message, currentUrl);
    }
    
    public boolean isSuccess() { return success; }
    public String getMessage() { return message; }
    public String getStatus() { return success ? "SUCCESS" : "FAILURE"; }
    public String getCurrentUrl() { return currentUrl; }
    public long getTimestamp() { return timestamp; }
    
    @Override
    public String toString() {
        return String.format("NavigationResult{status=%s, message='%s', url='%s', timestamp=%d}", 
                getStatus(), message, currentUrl, timestamp);
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
class BrowserNavigationExample {
    
    public static void demonstrateUsage(WebDriver driver, ScreenshotHelper screenshotHelper) {
        BrowserNavigationKeyword navKeyword = new BrowserNavigationKeyword(driver, screenshotHelper);
        
        // Example 1: Navigate to URL
        NavigationResult result1 = navKeyword.navigateTo("https://www.example.com");
        if (result1.isSuccess()) {
            System.out.println("Navigation successful: " + result1.getMessage());
        } else {
            System.err.println("Navigation failed: " + result1.getMessage());
        }
        
        // Example 2: Navigate to relative path
        NavigationResult result2 = navKeyword.navigateToRelativePath("/login");
        
        // Example 3: Navigate and wait for page load
        NavigationResult result3 = navKeyword.navigateToAndWaitForLoad("https://www.google.com");
        
        // Example 4: Browser history navigation
        NavigationResult backResult = navKeyword.navigateBack();
        NavigationResult forwardResult = navKeyword.navigateForward();
        
        // Example 5: Page refresh
        NavigationResult refreshResult = navKeyword.refreshPage();
        
        // Example 6: Get current page info
        String currentUrl = navKeyword.getCurrentUrl();
        String pageTitle = navKeyword.getPageTitle();
        System.out.println("Current URL: " + currentUrl);
        System.out.println("Page Title: " + pageTitle);
        
        // Example 7: Wait for page load
        NavigationResult loadResult = navKeyword.waitForPageLoad();
    }
}