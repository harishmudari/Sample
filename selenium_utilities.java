package com.enterprise.selenium.utils;

import org.openqa.selenium.*;
import org.openqa.selenium.support.ui.WebDriverWait;
import org.openqa.selenium.support.ui.ExpectedConditions;
import org.openqa.selenium.support.ui.Select;
import org.openqa.selenium.interactions.Actions;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.time.Duration;
import java.util.List;
import java.util.Set;

/**
 * Advanced Selenium Utility Keywords for Complex Scenarios
 * Handles dropdowns, alerts, windows, frames, tables, and more
 */
public class AdvancedSeleniumKeywords {
    
    private static final Logger log = LoggerFactory.getLogger(AdvancedSeleniumKeywords.class);
    
    /**
     * Select dropdown option by visible text
     * 
     * @param locator Dropdown locator
     * @param optionText Text of option to select
     * @return boolean Success status
     */
    public static boolean selectDropdownByText(By locator, String optionText) {
        String methodName = "selectDropdownByText";
        
        if (locator == null || optionText == null) {
            log.error("{} - Locator or optionText cannot be null", methodName);
            return false;
        }
        
        WebDriver driver = EnterpriseSeleniumKeywords.getDriver();
        WebDriverWait wait = EnterpriseSeleniumKeywords.getWait();
        
        log.info("{} - Selecting option '{}' from dropdown: {}", methodName, optionText, locator.toString());
        
        try {
            WebElement dropdownElement = wait.until(ExpectedConditions.elementToBeClickable(locator));
            Select dropdown = new Select(dropdownElement);
            
            dropdown.selectByVisibleText(optionText);
            
            // Verify selection
            WebElement selectedOption = dropdown.getFirstSelectedOption();
            String selectedText = selectedOption.getText();
            
            if (optionText.equals(selectedText)) {
                log.info("{} - Successfully selected option: '{}'", methodName, optionText);
                return true;
            } else {
                log.error("{} - Selection failed. Expected: '{}', Actual: '{}'", 
                         methodName, optionText, selectedText);
                return false;
            }
            
        } catch (Exception e) {
            log.error("{} - Error selecting dropdown option: {}", methodName, e.getMessage());
            EnterpriseSeleniumKeywords.takeScreenshotOnFailure(methodName);
            return false;
        }
    }
    
    /**
     * Select dropdown option by value
     * 
     * @param locator Dropdown locator
     * @param value Value attribute of option to select
     * @return boolean Success status
     */
    public static boolean selectDropdownByValue(By locator, String value) {
        String methodName = "selectDropdownByValue";
        
        if (locator == null || value == null) {
            log.error("{} - Locator or value cannot be null", methodName);
            return false;
        }
        
        WebDriverWait wait = EnterpriseSeleniumKeywords.getWait();
        
        log.info("{} - Selecting option with value '{}' from dropdown: {}", methodName, value, locator.toString());
        
        try {
            WebElement dropdownElement = wait.until(ExpectedConditions.elementToBeClickable(locator));
            Select dropdown = new Select(dropdownElement);
            
            dropdown.selectByValue(value);
            
            log.info("{} - Successfully selected option with value: '{}'", methodName, value);
            return true;
            
        } catch (Exception e) {
            log.error("{} - Error selecting dropdown by value: {}", methodName, e.getMessage());
            EnterpriseSeleniumKeywords.takeScreenshotOnFailure(methodName);
            return false;
        }
    }
    
    /**
     * Switch to window by title or URL pattern
     * 
     * @param titleOrUrlPattern Window title or URL pattern to switch to
     * @param isUrl True if pattern is URL, false if title
     * @return boolean Success status
     */
    public static boolean switchToWindow(String titleOrUrlPattern, boolean isUrl) {
        String methodName = "switchToWindow";
        WebDriver driver = EnterpriseSeleniumKeywords.getDriver();
        
        if (driver == null || titleOrUrlPattern == null) {
            log.error("{} - Driver or pattern cannot be null", methodName);
            return false;
        }
        
        log.info("{} - Switching to window with {}: '{}'", 
                methodName, (isUrl ? "URL" : "title"), titleOrUrlPattern);
        
        try {
            String originalWindow = driver.getWindowHandle();
            Set<String> windowHandles = driver.getWindowHandles();
            
            for (String windowHandle : windowHandles) {
                driver.switchTo().window(windowHandle);
                
                String currentValue = isUrl ? driver.getCurrentUrl() : driver.getTitle();
                
                if (currentValue.contains(titleOrUrlPattern)) {
                    log.info("{} - Successfully switched to window: '{}'", methodName, currentValue);
                    return true;
                }
            }
            
            // Switch back to original window if target not found
            driver.switchTo().window(originalWindow);
            log.error("{} - Window not found with pattern: '{}'", methodName, titleOrUrlPattern);
            return false;
            
        } catch (Exception e) {
            log.error("{} - Error switching window: {}", methodName, e.getMessage());
            return false;
        }
    }
    
    /**
     * Switch to frame by locator or name/id
     * 
     * @param frameIdentifier Frame locator, name, or id
     * @return boolean Success status
     */
    public static boolean switchToFrame(Object frameIdentifier) {
        String methodName = "switchToFrame";
        WebDriver driver = EnterpriseSeleniumKeywords.getDriver();
        WebDriverWait wait = EnterpriseSeleniumKeywords.getWait();
        
        if (driver == null || frameIdentifier == null) {
            log.error("{} - Driver or frameIdentifier cannot be null", methodName);
            return false;
        }
        
        log.info("{} - Switching to frame: {}", methodName, frameIdentifier.toString());
        
        try {
            if (frameIdentifier instanceof By) {
                WebElement frameElement = wait.until(
                    ExpectedConditions.frameToBeAvailableAndSwitchToIt((By) frameIdentifier));
            } else if (frameIdentifier instanceof String) {
                driver.switchTo().frame((String) frameIdentifier);
            } else if (frameIdentifier instanceof Integer) {
                driver.switchTo().frame((Integer) frameIdentifier);
            } else {
                log.error("{} - Invalid frame identifier type", methodName);
                return false;
            }
            
            log.info("{} - Successfully switched to frame", methodName);
            return true;
            
        } catch (Exception e) {
            log.error("{} - Error switching to frame: {}", methodName, e.getMessage());
            return false;
        }
    }
    
    /**
     * Switch back to default content (main page)
     * 
     * @return boolean Success status
     */
    public static boolean switchToDefaultContent() {
        String methodName = "switchToDefaultContent";
        WebDriver driver = EnterpriseSeleniumKeywords.getDriver();
        
        if (driver == null) {
            log.error("{} - WebDriver is not initialized", methodName);
            return false;
        }
        
        try {
            driver.switchTo().defaultContent();
            log.info("{} - Successfully switched to default content", methodName);
            return true;
        } catch (Exception e) {
            log.error("{} - Error switching to default content: {}", methodName, e.getMessage());
            return false;
        }
    }
    
    /**
     * Hover over element using Actions
     * 
     * @param locator Element locator
     * @return boolean Success status
     */
    public static boolean hoverOverElement(By locator) {
        String methodName = "hoverOverElement";
        WebDriver driver = EnterpriseSeleniumKeywords.getDriver();
        WebDriverWait wait = EnterpriseSeleniumKeywords.getWait();
        
        if (driver == null || locator == null) {
            log.error("{} - Driver or locator cannot be null", methodName);
            return false;
        }
        
        log.info("{} - Hovering over element: {}", methodName, locator.toString());
        
        try {
            WebElement element = wait.until(ExpectedConditions.visibilityOfElementLocated(locator));
            
            Actions actions = new Actions(driver);
            actions.moveToElement(element).perform();
            
            log.info("{} - Successfully hovered over element", methodName);
            return true;
            
        } catch (Exception e) {
            log.error("{} - Error hovering over element: {}", methodName, e.getMessage());
            EnterpriseSeleniumKeywords.takeScreenshotOnFailure(methodName);
            return false;
        }
    }
    
    /**
     * Drag and drop from source to target element
     * 
     * @param sourceLocator Source element locator
     * @param targetLocator Target element locator
     * @return boolean Success status
     */
    public static boolean dragAndDrop(By sourceLocator, By targetLocator) {
        String methodName = "dragAndDrop";
        WebDriver driver = EnterpriseSeleniumKeywords.getDriver();
        WebDriverWait wait = EnterpriseSeleniumKeywords.getWait();
        
        if (driver == null || sourceLocator == null || targetLocator == null) {
            log.error("{} - Driver or locators cannot be null", methodName);
            return false;
        }
        
        log.info("{} - Dragging from {} to {}", methodName, 
                sourceLocator.toString(), targetLocator.toString());
        
        try {
            WebElement sourceElement = wait.until(ExpectedConditions.elementToBeClickable(sourceLocator));
            WebElement targetElement = wait.until(ExpectedConditions.elementToBeClickable(targetLocator));
            
            Actions actions = new Actions(driver);
            actions.dragAndDrop(sourceElement, targetElement).perform();
            
            log.info("{} - Successfully performed drag and drop", methodName);
            return true;
            
        } catch (Exception e) {
            log.error("{} - Error performing drag and drop: {}", methodName, e.getMessage());
            EnterpriseSeleniumKeywords.takeScreenshotOnFailure(methodName);
            return false;
        }
    }
    
    /**
     * Scroll to element and bring it into view
     * 
     * @param locator Element locator
     * @return boolean Success status
     */
    public static boolean scrollToElement(By locator) {
        String methodName = "scrollToElement";
        WebDriver driver = EnterpriseSeleniumKeywords.getDriver();
        WebDriverWait wait = EnterpriseSeleniumKeywords.getWait();
        
        if (driver == null || locator == null) {
            log.error("{} - Driver or locator cannot be null", methodName);
            return false;
        }
        
        log.info("{} - Scrolling to element: {}", methodName, locator.toString());
        
        try {
            WebElement element = wait.until(ExpectedConditions.presenceOfElementLocated(locator));
            
            JavascriptExecutor js = (JavascriptExecutor) driver;
            js.executeScript("arguments[0].scrollIntoView({behavior: 'smooth', block: 'center'});", element);
            
            // Wait a bit for smooth scrolling to complete
            Thread.sleep(1000);
            
            log.info("{} - Successfully scrolled to element", methodName);
            return true;
            
        } catch (Exception e) {
            log.error("{} - Error scrolling to element: {}", methodName, e.getMessage());
            return false;
        }
    }
    
    /**
     * Get table data from specified row and column
     * 
     * @param tableLocator Table locator
     * @param row Row number (1-based)
     * @param column Column number (1-based)
     * @return String Cell text or null if error
     */
    public static String getTableCellData(By tableLocator, int row, int column) {
        String methodName = "getTableCellData";
        WebDriverWait wait = EnterpriseSeleniumKeywords.getWait();
        
        if (tableLocator == null || row < 1 || column < 1) {
            log.error("{} - Invalid parameters", methodName);
            return null;
        }
        
        log.info("{} - Getting data from table row {} column {}", methodName, row, column);
        
        try {
            WebElement table = wait.until(ExpectedConditions.presenceOfElementLocated(tableLocator));
            
            // Find the specific cell
            By cellLocator = By.xpath(".//tr[" + row + "]/td[" + column + "] | .//tr[" + row + "]/th[" + column + "]");
            WebElement cell = table.findElement(cellLocator);
            
            String cellText = cell.getText().trim();
            log.info("{} - Retrieved cell data: '{}'", methodName, cellText);
            
            return cellText;
            
        } catch (Exception e) {
            log.error("{} - Error getting table cell data: {}", methodName, e.getMessage());
            return null;
        }
    }
    
    /**
     * Wait for element to disappear
     * 
     * @param locator Element locator
     * @param timeoutSeconds Timeout in seconds
     * @return boolean Success status (true if element disappeared)
     */
    public static boolean waitForElementToDisappear(By locator, int timeoutSeconds) {
        String methodName = "waitForElementToDisappear";
        WebDriver driver = EnterpriseSeleniumKeywords.getDriver();
        
        if (driver == null || locator == null) {
            log.error("{} - Driver or locator cannot be null", methodName);
            return false;
        }
        
        log.info("{} - Waiting for element to disappear: {} (timeout: {}s)", 
                methodName, locator.toString(), timeoutSeconds);
        
        try {
            WebDriverWait wait = new WebDriverWait(driver, Duration.ofSeconds(timeoutSeconds));
            wait.until(ExpectedConditions.invisibilityOfElementLocated(locator));
            
            log.info("{} - Element disappeared successfully", methodName);
            return true;
            
        } catch (Exception e) {
            log.error("{} - Element did not disappear within timeout: {}", methodName, e.getMessage());
            return false;
        }
    }
    
    /**
     * Check if element exists without waiting
     * 
     * @param locator Element locator
     * @return boolean True if element exists
     */
    public static boolean isElementPresent(By locator) {
        String methodName = "isElementPresent";
        WebDriver driver = EnterpriseSeleniumKeywords.getDriver();
        
        if (driver == null || locator == null) {
            log.error("{} - Driver or locator cannot be null", methodName);
            return false;
        }
        
        try {
            List<WebElement> elements = driver.findElements(locator);
            boolean isPresent = !elements.isEmpty();
            
            log.debug("{} - Element {} present: {}", methodName, locator.toString(), isPresent);
            return isPresent;
            
        } catch (Exception e) {
            log.error("{} - Error checking element presence: {}", methodName, e.getMessage());
            return false;
        }
    }
    
    /**
     * Execute JavaScript and return result
     * 
     * @param script JavaScript code to execute
     * @param args Arguments to pass to script
     * @return Object Script execution result
     */
    public static Object executeJavaScript(String script, Object... args) {
        String methodName = "executeJavaScript";
        WebDriver driver = EnterpriseSeleniumKeywords.getDriver();
        
        if (driver == null || script == null) {
            log.error("{} - Driver or script cannot be null", methodName);
            return null;
        }
        
        log.info("{} - Executing JavaScript: {}", methodName, script);
        
        try {
            JavascriptExecutor js = (JavascriptExecutor) driver;
            Object result = js.executeScript(script, args);
            
            log.info("{} - JavaScript executed successfully", methodName);
            return result;
            
        } catch (Exception e) {
            log.error("{} - Error executing JavaScript: {}", methodName, e.getMessage());
            return null;
        }
    }
    
    /**
     * Handle JavaScript alerts/confirmations
     * 
     * @param accept True to accept, false to dismiss
     * @return String Alert text or null if no alert
     */
    public static String handleAlert(boolean accept) {
        String methodName = "handleAlert";
        WebDriver driver = EnterpriseSeleniumKeywords.getDriver();
        
        if (driver == null) {
            log.error("{} - WebDriver is not initialized", methodName);
            return null;
        }
        
        try {
            Alert alert = driver.switchTo().alert();
            String alertText = alert.getText();
            
            if (accept) {
                alert.accept();
                log.info("{} - Accepted alert with text: '{}'", methodName, alertText);
            } else {
                alert.dismiss();
                log.info("{} - Dismissed alert with text: '{}'", methodName, alertText);
            }
            
            return alertText;
            
        } catch (NoAlertPresentException e) {
            log.debug("{} - No alert present", methodName);
            return null;
        } catch (Exception e) {
            log.error("{} - Error handling alert: {}", methodName, e.getMessage());
            return null;
        }
    }
    
    /**
     * Upload file using file input element
     * 
     * @param fileInputLocator File input element locator
     * @param filePath Absolute path to file
     * @return boolean Success status
     */
    public static boolean uploadFile(By fileInputLocator, String filePath) {
        String methodName = "uploadFile";
        WebDriverWait wait = EnterpriseSeleniumKeywords.getWait();
        
        if (fileInputLocator == null || filePath == null) {
            log.error("{} - Locator or file path cannot be null", methodName);
            return false;
        }
        
        log.info("{} - Uploading file: {} using locator: {}", 
                methodName, filePath, fileInputLocator.toString());
        
        try {
            // Verify file exists
            java.io.File file = new java.io.File(filePath);
            if (!file.exists()) {
                log.error("{} - File does not exist: {}", methodName, filePath);
                return false;
            }
            
            WebElement fileInput = wait.until(ExpectedConditions.presenceOfElementLocated(fileInputLocator));
            fileInput.sendKeys(filePath);
            
            log.info("{} - File uploaded successfully", methodName);
            return true;
            
        } catch (Exception e) {
            log.error("{} - Error uploading file: {}", methodName, e.getMessage());
            EnterpriseSeleniumKeywords.takeScreenshotOnFailure(methodName);
            return false;
        }
    }
    
    /**
     * Wait for page title to contain specific text
     * 
     * @param titleText Expected title text
     * @param timeoutSeconds Timeout in seconds
     * @return boolean Success status
     */
    public static boolean waitForTitleContains(String titleText, int timeoutSeconds) {
        String methodName = "waitForTitleContains";
        WebDriver driver = EnterpriseSeleniumKeywords.getDriver();
        
        if (driver == null || titleText == null) {
            log.error("{} - Driver or title text cannot be null", methodName);
            return false;
        }
        
        log.info("{} - Waiting for title to contain: '{}'", methodName, titleText);
        
        try {
            WebDriverWait wait = new WebDriverWait(driver, Duration.ofSeconds(timeoutSeconds));
            wait.until(ExpectedConditions.titleContains(titleText));
            
            log.info("{} - Title contains expected text: '{}'", methodName, titleText);
            return true;
            
        } catch (Exception e) {
            log.error("{} - Title did not contain expected text within timeout: {}", methodName, e.getMessage());
            return false;
        }
    }
    
    /**
     * Get current page URL
     * 
     * @return String Current URL or null if error
     */
    public static String getCurrentUrl() {
        String methodName = "getCurrentUrl";
        WebDriver driver = EnterpriseSeleniumKeywords.getDriver();
        
        if (driver == null) {
            log.error("{} - WebDriver is not initialized", methodName);
            return null;
        }
        
        try {
            String currentUrl = driver.getCurrentUrl();
            log.debug("{} - Current URL: {}", methodName, currentUrl);
            return currentUrl;
            
        } catch (Exception e) {
            log.error("{} - Error getting current URL: {}", methodName, e.getMessage());
            return null;
        }
    }
    
    /**
     * Get current page title
     * 
     * @return String Current title or null if error
     */
    public static String getCurrentTitle() {
        String methodName = "getCurrentTitle";
        WebDriver driver = EnterpriseSeleniumKeywords.getDriver();
        
        if (driver == null) {
            log.error("{} - WebDriver is not initialized", methodName);
            return null;
        }
        
        try {
            String currentTitle = driver.getTitle();
            log.debug("{} - Current title: {}", methodName, currentTitle);
            return currentTitle;
            
        } catch (Exception e) {
            log.error("{} - Error getting current title: {}", methodName, e.getMessage());
            return null;
        }
    }
    
    /**
     * Close current window
     * 
     * @return boolean Success status
     */
    public static boolean closeCurrentWindow() {
        String methodName = "closeCurrentWindow";
        WebDriver driver = EnterpriseSeleniumKeywords.getDriver();
        
        if (driver == null) {
            log.error("{} - WebDriver is not initialized", methodName);
            return false;
        }
        
        try {
            driver.close();
            log.info("{} - Current window closed successfully", methodName);
            return true;
            
        } catch (Exception e) {
            log.error("{} - Error closing current window: {}", methodName, e.getMessage());
            return false;
        }
    }
    
    /**
     * Navigate back in browser history
     * 
     * @return boolean Success status
     */
    public static boolean navigateBack() {
        String methodName = "navigateBack";
        WebDriver driver = EnterpriseSeleniumKeywords.getDriver();
        
        if (driver == null) {
            log.error("{} - WebDriver is not initialized", methodName);
            return false;
        }
        
        try {
            driver.navigate().back();
            log.info("{} - Navigated back successfully", methodName);
            return true;
            
        } catch (Exception e) {
            log.error("{} - Error navigating back: {}", methodName, e.getMessage());
            return false;
        }
    }
    
    /**
     * Navigate forward in browser history
     * 
     * @return boolean Success status
     */
    public static boolean navigateForward() {
        String methodName = "navigateForward";
        WebDriver driver = EnterpriseSeleniumKeywords.getDriver();
        
        if (driver == null) {
            log.error("{} - WebDriver is not initialized", methodName);
            return false;
        }
        
        try {
            driver.navigate().forward();
            log.info("{} - Navigated forward successfully", methodName);
            return true;
            
        } catch (Exception e) {
            log.error("{} - Error navigating forward: {}", methodName, e.getMessage());
            return false;
        }
    }
    
    /**
     * Refresh current page
     * 
     * @return boolean Success status
     */
    public static boolean refreshPage() {
        String methodName = "refreshPage";
        WebDriver driver = EnterpriseSeleniumKeywords.getDriver();
        
        if (driver == null) {
            log.error("{} - WebDriver is not initialized", methodName);
            return false;
        }
        
        try {
            driver.navigate().refresh();
            EnterpriseSeleniumKeywords.waitForPageLoad();
            log.info("{} - Page refreshed successfully", methodName);
            return true;
            
        } catch (Exception e) {
            log.error("{} - Error refreshing page: {}", methodName, e.getMessage());
            return false;
        }
    }
}

/**
 * Retry Utility for implementing retry logic in keywords
 */
class RetryUtils {
    
    private static final Logger log = LoggerFactory.getLogger(RetryUtils.class);
    
    @FunctionalInterface
    public interface RetryableOperation {
        boolean execute() throws Exception;
    }
    
    /**
     * Execute operation with retry logic
     * 
     * @param operation Operation to retry
     * @param maxAttempts Maximum retry attempts
     * @param delaySeconds Delay between retries in seconds
     * @param operationName Name for logging
     * @return boolean Success status
     */
    public static boolean executeWithRetry(RetryableOperation operation, 
                                         int maxAttempts, 
                                         int delaySeconds, 
                                         String operationName) {
        
        for (int attempt = 1; attempt <= maxAttempts; attempt++) {
            try {
                if (operation.execute()) {
                    if (attempt > 1) {
                        log.info("Operation '{}' succeeded on attempt {}", operationName, attempt);
                    }
                    return true;
                }
            } catch (Exception e) {
                log.warn("Operation '{}' failed on attempt {}: {}", operationName, attempt, e.getMessage());
            }
            
            if (attempt < maxAttempts) {
                try {
                    Thread.sleep(delaySeconds * 1000);
                } catch (InterruptedException ie) {
                    Thread.currentThread().interrupt();
                    log.error("Retry interrupted for operation: {}", operationName);
                    return false;
                }
            }
        }
        
        log.error("Operation '{}' failed after {} attempts", operationName, maxAttempts);
        return false;
    }
}

/**
 * Response Object for methods that need to return multiple values
 */
class SeleniumResponse {
    private boolean success;
    private String message;
    private Object data;
    private long executionTimeMs;
    
    public SeleniumResponse(boolean success, String message, Object data, long executionTimeMs) {
        this.success = success;
        this.message = message;
        this.data = data;
        this.executionTimeMs = executionTimeMs;
    }
    
    // Getters
    public boolean isSuccess() { return success; }
    public String getMessage() { return message; }
    public Object getData() { return data; }
    public long getExecutionTimeMs() { return executionTimeMs; }
    
    // Builder pattern for easy creation
    public static class Builder {
        private boolean success;
        private String message;
        private Object data;
        private long executionTimeMs;
        
        public Builder success(boolean success) {
            this.success = success;
            return this;
        }
        
        public Builder message(String message) {
            this.message = message;
            return this;
        }
        
        public Builder data(Object data) {
            this.data = data;
            return this;
        }
        
        public Builder executionTime(long executionTimeMs) {
            this.executionTimeMs = executionTimeMs;
            return this;
        }
        
        public SeleniumResponse build() {
            return new SeleniumResponse(success, message, data, executionTimeMs);
        }
    }
}

/**
 * Locator Repository - Centralized locator management
 */
class LocatorRepository {
    
    // Login Page Locators
    public static final By LOGIN_USERNAME = By.id("username");
    public static final By LOGIN_PASSWORD = By.id("password");
    public static final By LOGIN_BUTTON = By.id("loginBtn");
    public static final By LOGIN_ERROR_MESSAGE = By.className("error-message");
    
    // Dashboard Page Locators
    public static final By DASHBOARD_WELCOME = By.id("welcomeMessage");
    public static final By DASHBOARD_MENU = By.className("main-menu");
    public static final By LOGOUT_BUTTON = By.id("logoutBtn");
    
    // Common Elements
    public static final By LOADING_SPINNER = By.className("loading-spinner");
    public static final By SUCCESS_MESSAGE = By.className("success-message");
    public static final By ERROR_MESSAGE = By.className("error-message");
    public static final By CONFIRMATION_DIALOG = By.className("confirmation-dialog");
    public static final By CLOSE_BUTTON = By.className("close-btn");
    
    // Form Elements
    public static final By SUBMIT_BUTTON = By.xpath("//button[@type='submit']");
    public static final By CANCEL_BUTTON = By.xpath("//button[contains(text(),'Cancel')]");
    public static final By SAVE_BUTTON = By.xpath("//button[contains(text(),'Save')]");
    
    // Table Elements
    public static final By DATA_TABLE = By.id("dataTable");
    public static final By TABLE_ROWS = By.xpath("//table[@id='dataTable']//tbody//tr");
    public static final By TABLE_HEADERS = By.xpath("//table[@id='dataTable']//thead//th");
    
    // Navigation Elements
    public static final By HOME_LINK = By.linkText("Home");
    public static final By PROFILE_LINK = By.linkText("Profile");
    public static final By SETTINGS_LINK = By.linkText("Settings");
}

/**
 * Browser Configuration Manager
 */
class BrowserConfigManager {
    
    private static final Logger log = LoggerFactory.getLogger(BrowserConfigManager.class);
    
    /**
     * Get Chrome options for different environments
     * 
     * @param environment Environment type (dev, qa, prod)
     * @param headless Run in headless mode
     * @return ChromeOptions configured for environment
     */
    public static org.openqa.selenium.chrome.ChromeOptions getChromeOptions(String environment, boolean headless) {
        org.openqa.selenium.chrome.ChromeOptions options = new org.openqa.selenium.chrome.ChromeOptions();
        
        // Common options for all environments
        options.addArguments("--no-sandbox");
        options.addArguments("--disable-dev-shm-usage");
        
        if (headless) {
            options.addArguments("--headless=new");
        }
        
        switch (environment.toLowerCase()) {
            case "dev":
                // Development environment - more permissive
                options.addArguments("--disable-web-security");
                options.addArguments("--ignore-certificate-errors");
                options.addArguments("--allow-running-insecure-content");
                break;
                
            case "qa":
                // QA environment - balanced security
                options.addArguments("--ignore-certificate-errors-spki-list");
                options.addArguments("--disable-extensions");
                break;
                
            case "prod":
                // Production environment - strict security
                options.addArguments("--enable-strict-mixed-content-checking");
                break;
                
            default:
                log.warn("Unknown environment: {}. Using default configuration.", environment);
                break;
        }
        
        return options;
    }
    
    /**
     * Get Firefox options for different environments
     * 
     * @param environment Environment type (dev, qa, prod)
     * @param headless Run in headless mode
     * @return FirefoxOptions configured for environment
     */
    public static org.openqa.selenium.firefox.FirefoxOptions getFirefoxOptions(String environment, boolean headless) {
        org.openqa.selenium.firefox.FirefoxOptions options = new org.openqa.selenium.firefox.FirefoxOptions();
        org.openqa.selenium.firefox.FirefoxProfile profile = new org.openqa.selenium.firefox.FirefoxProfile();
        
        if (headless) {
            options.addArguments("--headless");
        }
        
        switch (environment.toLowerCase()) {
            case "dev":
                // Development environment preferences
                profile.setPreference("security.mixed_content.block_active_content", false);
                profile.setPreference("security.mixed_content.block_display_content", false);
                break;
                
            case "qa":
                // QA environment preferences
                profile.setPreference("dom.webnotifications.enabled", false);
                break;
                
            case "prod":
                // Production environment preferences
                profile.setPreference("security.tls.unrestricted_rc4_fallback", false);
                break;
                
            default:
                log.warn("Unknown environment: {}. Using default configuration.", environment);
                break;
        }
        
        options.setProfile(profile);
        return options;
    }
}