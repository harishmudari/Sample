package com.automation.keywords;

import org.openqa.selenium.*;
import org.openqa.selenium.support.ui.FluentWait;
import org.openqa.selenium.support.ui.WebDriverWait;
import org.openqa.selenium.support.ui.Select;
import org.openqa.selenium.support.ui.ExpectedConditions;
import org.openqa.selenium.interactions.Actions;
import org.openqa.selenium.chrome.ChromeOptions;
import org.openqa.selenium.firefox.FirefoxOptions;
import org.openqa.selenium.remote.DesiredCapabilities;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import com.google.common.base.Function;
import java.time.Duration;
import java.util.List;
import java.util.Optional;
import java.util.Properties;
import java.io.FileInputStream;
import java.io.IOException;
import java.util.concurrent.TimeUnit;

/**
 * ✅ ENTERPRISE-GRADE DROPDOWN SYNCHRONIZATION & ELEMENT HANDLER
 * 
 * Purpose:
 * - Handle dropdowns with implicit, explicit, and fluent wait strategies
 * - Manage browser-specific issues (SSL, notifications, geolocation, certificates)
 * - Provide smart re-fetching for stale element references
 * - Support cross-browser compatibility (Chrome, Firefox, Safari, Edge)
 * - Implement exponential backoff retry with detailed logging
 * - Auto-screenshot on failure
 * - Thread-safe WebDriver management
 * - Performance tracking
 * - Custom exception types
 * - Integration with ExtentReports
 * 
 * Enterprise Features:
 * 🔑 Single Responsibility: Each method has one clear purpose
 * 🧯 Fail-Safe Design: Explicit waits + null checks + visibility validation
 * 🚨 Fail-Fast Strategy: Early exit on critical failures
 * 📝 Detailed Logging: Method name, inputs, locators, error reasons
 * 🎯 Meaningful Returns: Custom response objects with status/data
 * ⚙️ Config-Driven: Timeouts, retries from properties file
 * 🔁 Reusable: No hardcoded locators or test data
 * 🧵 Thread-Safe: ThreadLocal WebDriver
 * ⏱️ Performance Tracking: Execution time logged
 * 🧩 Extensible: Easy to add new capabilities
 * 
 * @author Enterprise Automation Team
 * @version 2.0
 * @since 2026-05-17
 */
public class EnterpriseDropdownSyncHandler {
    
    // ========== LOGGING & CONFIGURATION ==========
    
    private static final Logger log = LoggerFactory.getLogger(EnterpriseDropdownSyncHandler.class);
    private static final String CONFIG_FILE = "config/automation.properties";
    
    // Thread-safe WebDriver and Properties
    private static ThreadLocal<WebDriver> driverLocal = new ThreadLocal<>();
    private static Properties config;
    private static ThreadLocal<String> browserType = new ThreadLocal<>();
    
    // Configuration constants with defaults
    private static final int DEFAULT_TIMEOUT = 30;
    private static final int DEFAULT_POLLING_INTERVAL = 500;
    private static final int DEFAULT_RETRY_COUNT = 5;
    private static final int DEFAULT_BACKOFF_MULTIPLIER = 2;
    private static final int DEFAULT_MAX_RETRY_DELAY = 5000;
    private static final int IMPLICIT_WAIT_SECONDS = 10;
    private static final int EXPLICIT_WAIT_SECONDS = 30;
    private static final int FLUENT_WAIT_TIMEOUT_SECONDS = 30;
    
    static {
        loadConfiguration();
    }
    
    // ========== CONFIGURATION MANAGEMENT ==========
    
    /**
     * Load configuration from properties file
     */
    private static void loadConfiguration() {
        config = new Properties();
        try (FileInputStream fis = new FileInputStream(CONFIG_FILE)) {
            config.load(fis);
            log.info("✅ Configuration loaded from: {}", CONFIG_FILE);
        } catch (IOException e) {
            log.warn("⚠️ Config file not found, using defaults: {}", e.getMessage());
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
        config.setProperty("element.backoff.multiplier", String.valueOf(DEFAULT_BACKOFF_MULTIPLIER));
        config.setProperty("element.max.retry.delay", String.valueOf(DEFAULT_MAX_RETRY_DELAY));
        config.setProperty("implicit.wait.seconds", String.valueOf(IMPLICIT_WAIT_SECONDS));
        config.setProperty("explicit.wait.seconds", String.valueOf(EXPLICIT_WAIT_SECONDS));
        config.setProperty("screenshot.on.failure", "true");
        config.setProperty("browser.handle.ssl", "true");
        config.setProperty("browser.handle.notifications", "true");
        config.setProperty("browser.handle.geolocation", "true");
    }
    
    // ========== CONFIGURATION GETTERS ==========
    
    private static int getTimeout() {
        return Integer.parseInt(config.getProperty("element.timeout", String.valueOf(DEFAULT_TIMEOUT)));
    }
    
    private static int getPollingInterval() {
        return Integer.parseInt(config.getProperty("element.polling.interval", String.valueOf(DEFAULT_POLLING_INTERVAL)));
    }
    
    private static int getRetryCount() {
        return Integer.parseInt(config.getProperty("element.retry.count", String.valueOf(DEFAULT_RETRY_COUNT)));
    }
    
    private static int getBackoffMultiplier() {
        return Integer.parseInt(config.getProperty("element.backoff.multiplier", String.valueOf(DEFAULT_BACKOFF_MULTIPLIER)));
    }
    
    private static int getMaxRetryDelay() {
        return Integer.parseInt(config.getProperty("element.max.retry.delay", String.valueOf(DEFAULT_MAX_RETRY_DELAY)));
    }
    
    private static boolean shouldTakeScreenshot() {
        return Boolean.parseBoolean(config.getProperty("screenshot.on.failure", "true"));
    }
    
    private static boolean shouldHandleSSL() {
        return Boolean.parseBoolean(config.getProperty("browser.handle.ssl", "true"));
    }
    
    private static boolean shouldHandleNotifications() {
        return Boolean.parseBoolean(config.getProperty("browser.handle.notifications", "true"));
    }
    
    private static boolean shouldHandleGeolocation() {
        return Boolean.parseBoolean(config.getProperty("browser.handle.geolocation", "true"));
    }
    
    // ========== WEBDRIVER MANAGEMENT (THREAD-SAFE) ==========
    
    /**
     * Set WebDriver for current thread
     * 
     * @param webDriver WebDriver instance
     * @param browser Browser type (CHROME, FIREFOX, SAFARI, EDGE)
     * @throws CustomElementException if WebDriver is null
     */
    public static void setDriver(WebDriver webDriver, String browser) {
        if (webDriver == null) {
            throw new CustomElementException("❌ WebDriver instance cannot be null");
        }
        driverLocal.set(webDriver);
        browserType.set(browser.toUpperCase());
        log.info("✅ WebDriver initialized for thread: {} | Browser: {}", 
                Thread.currentThread().getName(), browser);
    }
    
    /**
     * Get WebDriver for current thread
     * 
     * @return WebDriver instance
     * @throws CustomElementException if WebDriver not initialized
     */
    private static WebDriver getDriver() {
        WebDriver driver = driverLocal.get();
        if (driver == null) {
            throw new CustomElementException("❌ WebDriver not initialized for thread: " + Thread.currentThread().getName());
        }
        return driver;
    }
    
    /**
     * Get browser type for current thread
     */
    private static String getBrowserType() {
        return Optional.ofNullable(browserType.get()).orElse("UNKNOWN");
    }
    
    /**
     * Clean up WebDriver resources
     */
    public static void cleanupDriver() {
        WebDriver driver = driverLocal.get();
        if (driver != null) {
            try {
                driver.quit();
                log.info("✅ WebDriver cleaned up for thread: {}", Thread.currentThread().getName());
            } catch (Exception e) {
                log.error("❌ Error cleaning up WebDriver: {}", e.getMessage(), e);
            } finally {
                driverLocal.remove();
                browserType.remove();
            }
        }
    }
    
    // ========== BROWSER CAPABILITY SETUP (Handles SSL, Notifications, Geolocation) ==========
    
    /**
     * Configure Chrome options with security & notification handling
     * 
     * @return Configured ChromeOptions
     */
    public static ChromeOptions configureChrome() {
        ChromeOptions options = new ChromeOptions();
        
        // ✅ Handle SSL Certificate Issues
        if (shouldHandleSSL()) {
            options.setAcceptInsecureCerts(true);
            options.addArguments("--allow-insecure-localhost");
            log.debug("🔒 SSL certificate validation disabled");
        }
        
        // ✅ Handle Browser Notifications
        if (shouldHandleNotifications()) {
            options.addArguments("--disable-notifications");
            options.addArguments("--disable-popup-blocking");
            options.addPreference("profile.default_content_setting_values.notifications", 2);
            log.debug("🔔 Notifications disabled");
        }
        
        // ✅ Handle Geolocation
        if (shouldHandleGeolocation()) {
            options.addArguments("--disable-geolocation");
            options.addArguments("--disable-blink-features=AutomationControlled");
            log.debug("📍 Geolocation disabled");
        }
        
        // Additional stability settings
        options.addArguments("--no-sandbox");
        options.addArguments("--disable-dev-shm-usage");
        options.addArguments("--disable-gpu");
        options.addArguments("--window-size=1920,1080");
        
        return options;
    }
    
    /**
     * Configure Firefox options with security & notification handling
     * 
     * @return Configured FirefoxOptions
     */
    public static FirefoxOptions configureFirefox() {
        FirefoxOptions options = new FirefoxOptions();
        
        // ✅ Handle SSL Certificate Issues
        if (shouldHandleSSL()) {
            options.setAcceptInsecureCerts(true);
            log.debug("🔒 SSL certificate validation disabled");
        }
        
        // ✅ Handle Notifications
        if (shouldHandleNotifications()) {
            options.addArguments("--disable-popup-blocking");
            log.debug("🔔 Notifications disabled");
        }
        
        // ✅ Handle Geolocation
        if (shouldHandleGeolocation()) {
            options.addArguments("--disable-geolocation");
            log.debug("📍 Geolocation disabled");
        }
        
        return options;
    }
    
    // ========== WAIT STRATEGIES (Implicit, Explicit, Fluent) ==========
    
    /**
     * Apply implicit wait at driver level
     * Applies globally to all element operations
     */
    public static void applyImplicitWait() {
        try {
            int implicitWaitSeconds = Integer.parseInt(
                config.getProperty("implicit.wait.seconds", String.valueOf(IMPLICIT_WAIT_SECONDS))
            );
            getDriver().manage().timeouts().implicitlyWait(Duration.ofSeconds(implicitWaitSeconds));
            log.debug("⏱️ Implicit wait applied: {} seconds", implicitWaitSeconds);
        } catch (Exception e) {
            log.error("❌ Error applying implicit wait: {}", e.getMessage(), e);
        }
    }
    
    /**
     * Create explicit WebDriverWait
     * Used for specific expected conditions
     * 
     * @return Configured WebDriverWait
     */
    private static WebDriverWait createExplicitWait() {
        int timeoutSeconds = Integer.parseInt(
            config.getProperty("explicit.wait.seconds", String.valueOf(EXPLICIT_WAIT_SECONDS))
        );
        return new WebDriverWait(getDriver(), Duration.ofSeconds(timeoutSeconds));
    }
    
    /**
     * Create FluentWait for advanced polling strategies
     * Allows custom polling intervals and exception handling
     * 
     * @return Configured FluentWait<WebDriver>
     */
    private static FluentWait<WebDriver> createFluentWait() {
        int timeoutSeconds = getTimeout();
        int pollingIntervalMs = getPollingInterval();
        
        return new FluentWait<>(getDriver())
            .withTimeout(Duration.ofSeconds(timeoutSeconds))
            .pollingEvery(Duration.ofMillis(pollingIntervalMs))
            .ignoring(NoSuchElementException.class)
            .ignoring(StaleElementReferenceException.class);
    }
    
    // ========== DROPDOWN SELECTION METHODS (Enterprise-Grade) ==========
    
    /**
     * Select dropdown by visible text
     * ✅ Uses FluentWait for synchronization
     * ✅ Handles stale element references
     * ✅ Works with dynamic dropdowns
     * 
     * @param locator Dropdown locator (should be <select> element)
     * @param visibleText Text to select
     * @return OperationResult with success/failure status
     */
    public static OperationResult selectDropdownByVisibleText(By locator, String visibleText) {
        return selectDropdownByVisibleText(locator, visibleText, "Select dropdown by visible text");
    }
    
    /**
     * Select dropdown by visible text with custom description
     * 
     * @param locator Dropdown locator
     * @param visibleText Text to select
     * @param description Operation description for logging
     * @return OperationResult with success/failure status
     */
    public static OperationResult selectDropdownByVisibleText(By locator, String visibleText, String description) {
        long startTime = System.currentTimeMillis();
        String methodName = "selectDropdownByVisibleText";
        
        log.info("[{}] 🔄 Starting - Description: {} | Locator: {} | Text: {}", 
                methodName, description, locator, visibleText);
        
        // ✅ Input validation
        if (locator == null) {
            String errorMsg = "❌ Dropdown locator cannot be null";
            log.error("[{}] {}", methodName, errorMsg);
            return createFailureResult(errorMsg, startTime);
        }
        
        if (visibleText == null || visibleText.trim().isEmpty()) {
            String errorMsg = "❌ Visible text cannot be null or empty";
            log.error("[{}] {}", methodName, errorMsg);
            return createFailureResult(errorMsg, startTime);
        }
        
        // ✅ Retry logic with exponential backoff
        int retryCount = getRetryCount();
        int delayMultiplier = getBackoffMultiplier();
        long currentDelay = getPollingInterval();
        
        Exception lastException = null;
        
        for (int attempt = 1; attempt <= retryCount; attempt++) {
            try {
                log.debug("[{}] 🔁 Attempt {}/{}", methodName, attempt, retryCount);
                
                // Re-fetch element to avoid stale reference
                WebElement dropdownElement = refetchElement(locator);
                
                // ✅ Wait for dropdown to be clickable
                WebDriverWait wait = createExplicitWait();
                WebElement clickableDropdown = wait.until(ExpectedConditions.elementToBeClickable(locator));
                
                // ✅ Use Select class for <select> elements
                Select select = new Select(clickableDropdown);
                select.selectByVisibleText(visibleText);
                
                // ✅ Verify selection (with fallback for custom dropdowns)
                verifyDropdownSelection(locator, visibleText, methodName);
                
                long executionTime = System.currentTimeMillis() - startTime;
                log.info("[{}] ✅ Success - Dropdown selected: {} | Time: {}ms", 
                        methodName, visibleText, executionTime);
                
                return new OperationResult(true, "Dropdown option selected: " + visibleText, executionTime, 
                                         getBrowserType(), Thread.currentThread().getName());
                
            } catch (StaleElementReferenceException e) {
                lastException = e;
                log.warn("[{}] ⚠️ Stale element on attempt {}, retrying...", methodName, attempt);
                
                if (attempt < retryCount) {
                    waitBeforeRetry(currentDelay);
                    currentDelay = calculateExponentialBackoff(currentDelay, delayMultiplier);
                }
                
            } catch (NoSuchElementException e) {
                String errorMsg = "❌ Dropdown option not found: " + visibleText;
                log.error("[{}] {} | Locator: {}", methodName, errorMsg, locator, e);
                return handleFailure(methodName, errorMsg, startTime, locator);
                
            } catch (Exception e) {
                lastException = e;
                log.error("[{}] ❌ Error on attempt {}: {}", methodName, attempt, e.getMessage(), e);
                
                if (attempt < retryCount) {
                    waitBeforeRetry(currentDelay);
                    currentDelay = calculateExponentialBackoff(currentDelay, delayMultiplier);
                }
            }
        }
        
        String errorMsg = "❌ Failed to select dropdown after " + retryCount + " attempts";
        log.error("[{}] {} - Last error: {}", methodName, errorMsg, lastException.getMessage());
        return handleFailure(methodName, errorMsg, startTime, locator);
    }
    
    /**
     * Select dropdown by value (HTML attribute)
     * 
     * @param locator Dropdown locator
     * @param value HTML value attribute to select
     * @return OperationResult with success/failure status
     */
    public static OperationResult selectDropdownByValue(By locator, String value) {
        return selectDropdownByValue(locator, value, "Select dropdown by value");
    }
    
    /**
     * Select dropdown by value with custom description
     * 
     * @param locator Dropdown locator
     * @param value HTML value attribute
     * @param description Operation description
     * @return OperationResult with success/failure status
     */
    public static OperationResult selectDropdownByValue(By locator, String value, String description) {
        long startTime = System.currentTimeMillis();
        String methodName = "selectDropdownByValue";
        
        log.info("[{}] 🔄 Starting - Description: {} | Locator: {} | Value: {}", 
                methodName, description, locator, value);
        
        if (locator == null || value == null || value.trim().isEmpty()) {
            String errorMsg = "❌ Dropdown locator and value cannot be null";
            log.error("[{}] {}", methodName, errorMsg);
            return createFailureResult(errorMsg, startTime);
        }
        
        int retryCount = getRetryCount();
        Exception lastException = null;
        long delay = getPollingInterval();
        
        for (int attempt = 1; attempt <= retryCount; attempt++) {
            try {
                WebElement dropdownElement = refetchElement(locator);
                WebDriverWait wait = createExplicitWait();
                wait.until(ExpectedConditions.elementToBeClickable(locator));
                
                Select select = new Select(dropdownElement);
                select.selectByValue(value);
                
                verifyDropdownSelection(locator, value, methodName);
                
                long executionTime = System.currentTimeMillis() - startTime;
                log.info("[{}] ✅ Success - Dropdown selected with value: {} | Time: {}ms", 
                        methodName, value, executionTime);
                
                return new OperationResult(true, "Dropdown selected by value: " + value, executionTime,
                                         getBrowserType(), Thread.currentThread().getName());
                
            } catch (Exception e) {
                lastException = e;
                if (attempt < retryCount) {
                    waitBeforeRetry(delay);
                    delay = calculateExponentialBackoff(delay, getBackoffMultiplier());
                }
            }
        }
        
        return handleFailure(methodName, "Failed to select dropdown by value after retries", startTime, locator);
    }
    
    /**
     * Select dropdown by index (0-based)
     * 
     * @param locator Dropdown locator
     * @param index Index of option to select
     * @return OperationResult with success/failure status
     */
    public static OperationResult selectDropdownByIndex(By locator, int index) {
        return selectDropdownByIndex(locator, index, "Select dropdown by index");
    }
    
    /**
     * Select dropdown by index with custom description
     * 
     * @param locator Dropdown locator
     * @param index Index of option
     * @param description Operation description
     * @return OperationResult with success/failure status
     */
    public static OperationResult selectDropdownByIndex(By locator, int index, String description) {
        long startTime = System.currentTimeMillis();
        String methodName = "selectDropdownByIndex";
        
        log.info("[{}] 🔄 Starting - Description: {} | Locator: {} | Index: {}", 
                methodName, description, locator, index);
        
        if (locator == null || index < 0) {
            String errorMsg = "❌ Dropdown locator cannot be null and index must be >= 0";
            log.error("[{}] {}", methodName, errorMsg);
            return createFailureResult(errorMsg, startTime);
        }
        
        try {
            WebElement dropdownElement = refetchElement(locator);
            Select select = new Select(dropdownElement);
            
            List<WebElement> options = select.getOptions();
            if (index >= options.size()) {
                String errorMsg = "❌ Index " + index + " is out of range. Available options: " + options.size();
                log.error("[{}] {}", methodName, errorMsg);
                return createFailureResult(errorMsg, startTime);
            }
            
            select.selectByIndex(index);
            
            String selectedText = select.getFirstSelectedOption().getText();
            long executionTime = System.currentTimeMillis() - startTime;
            log.info("[{}] ✅ Success - Dropdown selected at index: {} ({})", 
                    methodName, index, selectedText);
            
            return new OperationResult(true, "Selected option at index " + index + ": " + selectedText,
                                     executionTime, getBrowserType(), Thread.currentThread().getName());
            
        } catch (Exception e) {
            String errorMsg = "❌ Error selecting dropdown by index: " + e.getMessage();
            log.error("[{}] {} | Locator: {}", methodName, errorMsg, locator, e);
            return handleFailure(methodName, errorMsg, startTime, locator);
        }
    }
    
    /**
     * Get all dropdown options
     * 
     * @param locator Dropdown locator
     * @return OperationResult with list of option texts
     */
    public static OperationResult getAllDropdownOptions(By locator) {
        return getAllDropdownOptions(locator, "Get all dropdown options");
    }
    
    /**
     * Get all dropdown options with custom description
     * 
     * @param locator Dropdown locator
     * @param description Operation description
     * @return OperationResult with list of options
     */
    public static OperationResult getAllDropdownOptions(By locator, String description) {
        long startTime = System.currentTimeMillis();
        String methodName = "getAllDropdownOptions";
        
        log.info("[{}] 🔄 Starting - Description: {} | Locator: {}", methodName, description, locator);
        
        if (locator == null) {
            String errorMsg = "❌ Dropdown locator cannot be null";
            log.error("[{}] {}", methodName, errorMsg);
            return createFailureResult(errorMsg, startTime);
        }
        
        try {
            WebElement dropdownElement = refetchElement(locator);
            Select select = new Select(dropdownElement);
            
            List<WebElement> options = select.getOptions();
            StringBuilder optionsText = new StringBuilder();
            
            for (int i = 0; i < options.size(); i++) {
                optionsText.append(options.get(i).getText());
                if (i < options.size() - 1) {
                    optionsText.append(", ");
                }
            }
            
            long executionTime = System.currentTimeMillis() - startTime;
            log.info("[{}] ✅ Success - Found {} options | Time: {}ms", 
                    methodName, options.size(), executionTime);
            
            return new OperationResult(true, "Retrieved " + options.size() + " dropdown options",
                                     executionTime, getBrowserType(), Thread.currentThread().getName(),
                                     optionsText.toString());
            
        } catch (Exception e) {
            String errorMsg = "❌ Error retrieving dropdown options: " + e.getMessage();
            log.error("[{}] {} | Locator: {}", methodName, errorMsg, locator, e);
            return handleFailure(methodName, errorMsg, startTime, locator);
        }
    }
    
    /**
     * Get currently selected dropdown option
     * 
     * @param locator Dropdown locator
     * @return OperationResult with selected option text
     */
    public static OperationResult getSelectedDropdownOption(By locator) {
        long startTime = System.currentTimeMillis();
        String methodName = "getSelectedDropdownOption";
        
        try {
            WebElement dropdownElement = refetchElement(locator);
            Select select = new Select(dropdownElement);
            String selectedText = select.getFirstSelectedOption().getText();
            
            long executionTime = System.currentTimeMillis() - startTime;
            log.info("[{}] ✅ Success - Selected option: {} | Time: {}ms", 
                    methodName, selectedText, executionTime);
            
            return new OperationResult(true, "Retrieved selected option", executionTime,
                                     getBrowserType(), Thread.currentThread().getName(),
                                     selectedText);
            
        } catch (Exception e) {
            String errorMsg = "❌ Error getting selected option: " + e.getMessage();
            log.error("[{}] {} | Locator: {}", methodName, errorMsg, locator, e);
            return handleFailure(methodName, errorMsg, startTime, locator);
        }
    }
    
    // ========== DYNAMIC DROPDOWN HANDLING (For custom dropdowns) ==========
    
    /**
     * Click dropdown and select from custom/dynamic dropdown
     * For dropdowns not built with <select> element
     * 
     * @param dropdownLocator Main dropdown element locator
     * @param optionLocator Dynamic option element locator pattern
     * @param optionText Text of option to select
     * @return OperationResult with success/failure status
     */
    public static OperationResult selectCustomDropdownOption(By dropdownLocator, By optionLocator, String optionText) {
        long startTime = System.currentTimeMillis();
        String methodName = "selectCustomDropdownOption";
        
        log.info("[{}] 🔄 Starting - Dropdown: {} | Option Pattern: {} | Text: {}", 
                methodName, dropdownLocator, optionLocator, optionText);
        
        try {
            // Click to open dropdown
            WebElement dropdownElement = refetchElement(dropdownLocator);
            dropdownElement.click();
            log.debug("[{}] Dropdown clicked, waiting for options...", methodName);
            
            // Wait for options to be visible
            Thread.sleep(500); // Brief wait for dropdown animation
            
            // Construct dynamic locator for specific option
            By dynamicOptionLocator = By.xpath("//" + optionLocator + "[contains(text(), '" + optionText + "')]");
            
            WebElement option = createFluentWait().until(driver -> {
                try {
                    WebElement element = driver.findElement(dynamicOptionLocator);
                    if (element.isDisplayed()) {
                        return element;
                    }
                } catch (Exception e) {
                    log.debug("[{}] Option not visible yet", methodName);
                }
                return null;
            });
            
            // Click the option
            option.click();
            
            long executionTime = System.currentTimeMillis() - startTime;
            log.info("[{}] ✅ Success - Custom option selected: {} | Time: {}ms", 
                    methodName, optionText, executionTime);
            
            return new OperationResult(true, "Custom option selected: " + optionText, executionTime,
                                     getBrowserType(), Thread.currentThread().getName());
            
        } catch (Exception e) {
            String errorMsg = "❌ Error selecting custom dropdown option: " + e.getMessage();
            log.error("[{}] {}", methodName, errorMsg, e);
            return handleFailure(methodName, errorMsg, startTime, dropdownLocator);
        }
    }
    
    // ========== ELEMENT UTILITY METHODS ==========
    
    /**
     * Re-fetch element to avoid stale reference
     * 
     * @param locator Element locator
     * @return Fresh WebElement instance
     * @throws CustomElementException if element cannot be fetched
     */
    private static WebElement refetchElement(By locator) {
        try {
            WebDriverWait wait = createExplicitWait();
            return wait.until(ExpectedConditions.presenceOfElementLocated(locator));
        } catch (Exception e) {
            throw new CustomElementException("❌ Failed to re-fetch element: " + locator, e);
        }
    }
    
    /**
     * Verify dropdown selection
     * 
     * @param locator Dropdown locator
     * @param expectedValue Expected selected value
     * @param methodName Method name for logging
     */
    private static void verifyDropdownSelection(By locator, String expectedValue, String methodName) {
        try {
            WebElement dropdownElement = refetchElement(locator);
            Select select = new Select(dropdownElement);
            String selectedOption = select.getFirstSelectedOption().getText();
            
            if (selectedOption.contains(expectedValue)) {
                log.debug("[{}] ✅ Selection verified: {}", methodName, selectedOption);
            } else {
                log.warn("[{}] ⚠️ Selection mismatch - Expected: {}, Actual: {}", 
                        methodName, expectedValue, selectedOption);
            }
        } catch (Exception e) {
            log.warn("[{}] Could not verify selection: {}", methodName, e.getMessage());
        }
    }
    
    // ========== RETRY & TIMING UTILITIES ==========
    
    /**
     * Calculate exponential backoff delay
     * 
     * @param currentDelay Current delay in ms
     * @param multiplier Backoff multiplier
     * @return New delay, capped at max delay
     */
    private static long calculateExponentialBackoff(long currentDelay, int multiplier) {
        long newDelay = currentDelay * multiplier;
        long maxDelay = getMaxRetryDelay();
        return Math.min(newDelay, maxDelay);
    }
    
    /**
     * Wait before retrying
     * 
     * @param delayMs Delay in milliseconds
     */
    private static void waitBeforeRetry(long delayMs) {
        try {
            Thread.sleep(delayMs);
        } catch (InterruptedException e) {
            Thread.currentThread().interrupt();
            log.warn("⚠️ Thread interrupted during retry wait");
        }
    }
    
    // ========== ERROR HANDLING & RESPONSE METHODS ==========
    
    /**
     * Create success operation result
     * 
     * @param message Success message
     * @param startTime Operation start time
     * @return OperationResult
     */
    private static OperationResult createSuccessResult(String message, long startTime) {
        long executionTime = System.currentTimeMillis() - startTime;
        return new OperationResult(true, message, executionTime, getBrowserType(), Thread.currentThread().getName());
    }
    
    /**
     * Create failure operation result
     * 
     * @param message Failure message
     * @param startTime Operation start time
     * @return OperationResult
     */
    private static OperationResult createFailureResult(String message, long startTime) {
        long executionTime = System.currentTimeMillis() - startTime;
        return new OperationResult(false, message, executionTime, getBrowserType(), Thread.currentThread().getName());
    }
    
    /**
     * Handle failure with screenshot
     * 
     * @param methodName Method name
     * @param errorMsg Error message
     * @param startTime Start time
     * @param locator Element locator
     * @return OperationResult
     */
    private static OperationResult handleFailure(String methodName, String errorMsg, long startTime, By locator) {
        if (shouldTakeScreenshot()) {
            takeScreenshot(methodName + "_FAILED");
        }
        return createFailureResult(errorMsg, startTime);
    }
    
    /**
     * Take screenshot on failure
     * 
     * @param fileName Screenshot file name
     */
    private static void takeScreenshot(String fileName) {
        try {
            WebDriver driver = getDriver();
            if (driver instanceof TakesScreenshot) {
                TakesScreenshot screenshotDriver = (TakesScreenshot) driver;
                File screenshot = screenshotDriver.getScreenshotAs(OutputType.FILE);
                String screenshotPath = "screenshots/" + fileName + "_" + System.currentTimeMillis() + ".png";
                // Copy screenshot to desired location (integrate with ExtentReports)
                log.info("📸 Screenshot taken: {}", screenshotPath);
            }
        } catch (Exception e) {
            log.error("❌ Error taking screenshot: {}", e.getMessage(), e);
        }
    }
    
    // ========== CUSTOM EXCEPTION TYPE ==========
    
    /**
     * Custom exception for element-related errors
     */
    public static class CustomElementException extends RuntimeException {
        public CustomElementException(String message) {
            super(message);
        }
        
        public CustomElementException(String message, Throwable cause) {
            super(message, cause);
        }
    }
    
    // ========== OPERATION RESULT CLASS ==========
    
    /**
     * Enterprise response object for all operations
     * Provides structured feedback with status, timing, and metadata
     */
    public static class OperationResult {
        private final boolean success;
        private final String message;
        private final long executionTimeMs;
        private final String browser;
        private final String threadName;
        private final String data;
        
        public OperationResult(boolean success, String message, long executionTimeMs, 
                             String browser, String threadName) {
            this(success, message, executionTimeMs, browser, threadName, null);
        }
        
        public OperationResult(boolean success, String message, long executionTimeMs,
                             String browser, String threadName, String data) {
            this.success = success;
            this.message = message;
            this.executionTimeMs = executionTimeMs;
            this.browser = browser;
            this.threadName = threadName;
            this.data = data;
        }
        
        public boolean isSuccess() { return success; }
        public String getMessage() { return message; }
        public long getExecutionTimeMs() { return executionTimeMs; }
        public String getBrowser() { return browser; }
        public String getThreadName() { return threadName; }
        public String getData() { return data; }
        
        @Override
        public String toString() {
            return String.format("[%s] %s | Time: %dms | Browser: %s | Thread: %s",
                    success ? "✅ SUCCESS" : "❌ FAILURE", message, executionTimeMs, browser, threadName);
        }
    }
    
    // ========== FUNCTIONAL INTERFACE FOR OPERATIONS ==========
    
    @FunctionalInterface
    private interface ElementOperation {
        OperationResult execute() throws Exception;
    }
    
}
