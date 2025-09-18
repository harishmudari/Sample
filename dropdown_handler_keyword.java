package com.automation.keywords;

import org.openqa.selenium.*;
import org.openqa.selenium.support.ui.Select;
import org.openqa.selenium.support.ui.WebDriverWait;
import org.openqa.selenium.support.ui.ExpectedConditions;
import org.openqa.selenium.interactions.Actions;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import java.time.Duration;
import java.util.List;
import java.util.ArrayList;
import java.util.Properties;
import java.io.FileInputStream;
import java.io.IOException;

/**
 * Enterprise-level Dropdown Handler for Selenium WebDriver
 * 
 * Provides comprehensive dropdown handling capabilities with:
 * - Standard HTML Select element support
 * - Custom dropdown (non-Select) support
 * - Fail-safe design with explicit waits
 * - Proper exception handling and logging
 * - Auto screenshot on failures
 * - Thread-safe implementation
 * - Configurable timeouts and retry logic
 * 
 * @author Automation Team
 * @version 1.0
 */
public class DropdownHandler {
    
    private static final Logger log = LoggerFactory.getLogger(DropdownHandler.class);
    private static final String CONFIG_FILE = "config/automation.properties";
    
    // Thread-safe WebDriver instance
    private static ThreadLocal<WebDriver> driver = new ThreadLocal<>();
    private static Properties config;
    
    // Configuration constants
    private static final int DEFAULT_TIMEOUT = 10;
    private static final int DEFAULT_RETRY_COUNT = 3;
    private static final int DROPDOWN_OPEN_WAIT = 500; // milliseconds
    private static final int OPTION_CLICK_PAUSE = 200; // milliseconds
    
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
        config.setProperty("dropdown.timeout", String.valueOf(DEFAULT_TIMEOUT));
        config.setProperty("dropdown.retry.count", String.valueOf(DEFAULT_RETRY_COUNT));
        config.setProperty("dropdown.open.wait", String.valueOf(DROPDOWN_OPEN_WAIT));
        config.setProperty("dropdown.option.pause", String.valueOf(OPTION_CLICK_PAUSE));
        config.setProperty("screenshot.on.failure", "true");
    }
    
    /**
     * Set WebDriver instance for current thread
     * @param webDriver WebDriver instance
     */
    public static void setDriver(WebDriver webDriver) {
        if (webDriver == null) {
            throw new CustomDropdownException("WebDriver instance cannot be null");
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
            throw new CustomDropdownException("WebDriver not initialized for current thread");
        }
        return webDriver;
    }
    
    // ========== SELECT CLASS DROPDOWN METHODS ==========
    
    /**
     * Select option by visible text using Select class
     * 
     * @param dropdownLocator By locator for the dropdown select element
     * @param visibleText Visible text of the option to select
     * @return DropdownOperationResult indicating success/failure with details
     */
    public static DropdownOperationResult selectByVisibleText(By dropdownLocator, String visibleText) {
        return selectByVisibleText(dropdownLocator, visibleText, "Select option by visible text");
    }
    
    /**
     * Select option by visible text using Select class with custom description
     * 
     * @param dropdownLocator By locator for the dropdown select element
     * @param visibleText Visible text of the option to select
     * @param description Custom description for logging
     * @return DropdownOperationResult indicating success/failure with details
     */
    public static DropdownOperationResult selectByVisibleText(By dropdownLocator, String visibleText, String description) {
        long startTime = System.currentTimeMillis();
        String methodName = "selectByVisibleText";
        
        log.info("[{}] Starting operation - Description: {} | Locator: {} | Text: {}", 
                methodName, description, dropdownLocator, visibleText);
        
        // Input validation - fail fast
        if (dropdownLocator == null || visibleText == null || visibleText.trim().isEmpty()) {
            String errorMsg = "Dropdown locator and visible text cannot be null or empty";
            log.error("[{}] {}", methodName, errorMsg);
            return createFailureResult(errorMsg, startTime);
        }
        
        return executeSelectOperation(() -> {
            WebElement dropdownElement = waitForElementToBeClickable(dropdownLocator);
            Select dropdown = new Select(dropdownElement);
            
            // Verify dropdown is enabled
            if (!dropdownElement.isEnabled()) {
                throw new CustomDropdownException("Dropdown is disabled");
            }
            
            dropdown.selectByVisibleText(visibleText);
            
            // Verify selection
            String selectedText = dropdown.getFirstSelectedOption().getText();
            if (!selectedText.equals(visibleText)) {
                throw new CustomDropdownException("Selection verification failed. Expected: " + visibleText + ", Actual: " + selectedText);
            }
            
            log.info("[{}] Successfully selected option - Description: {} | Selected: {}", 
                    methodName, description, visibleText);
            return createSuccessResult("Option selected successfully: " + visibleText, startTime, selectedText);
            
        }, methodName, description + " | Locator: " + dropdownLocator + " | Text: " + visibleText, startTime);
    }
    
    /**
     * Select option by value using Select class
     * 
     * @param dropdownLocator By locator for the dropdown select element
     * @param value Value attribute of the option to select
     * @return DropdownOperationResult indicating success/failure with details
     */
    public static DropdownOperationResult selectByValue(By dropdownLocator, String value) {
        return selectByValue(dropdownLocator, value, "Select option by value");
    }
    
    /**
     * Select option by value using Select class with custom description
     * 
     * @param dropdownLocator By locator for the dropdown select element
     * @param value Value attribute of the option to select
     * @param description Custom description for logging
     * @return DropdownOperationResult indicating success/failure with details
     */
    public static DropdownOperationResult selectByValue(By dropdownLocator, String value, String description) {
        long startTime = System.currentTimeMillis();
        String methodName = "selectByValue";
        
        log.info("[{}] Starting operation - Description: {} | Locator: {} | Value: {}", 
                methodName, description, dropdownLocator, value);
        
        if (dropdownLocator == null || value == null) {
            String errorMsg = "Dropdown locator and value cannot be null";
            log.error("[{}] {}", methodName, errorMsg);
            return createFailureResult(errorMsg, startTime);
        }
        
        return executeSelectOperation(() -> {
            WebElement dropdownElement = waitForElementToBeClickable(dropdownLocator);
            Select dropdown = new Select(dropdownElement);
            
            if (!dropdownElement.isEnabled()) {
                throw new CustomDropdownException("Dropdown is disabled");
            }
            
            dropdown.selectByValue(value);
            
            // Verify selection
            String selectedValue = dropdown.getFirstSelectedOption().getAttribute("value");
            if (!selectedValue.equals(value)) {
                throw new CustomDropdownException("Selection verification failed. Expected value: " + value + ", Actual value: " + selectedValue);
            }
            
            String selectedText = dropdown.getFirstSelectedOption().getText();
            log.info("[{}] Successfully selected option - Description: {} | Value: {} | Text: {}", 
                    methodName, description, value, selectedText);
            return createSuccessResult("Option selected by value successfully: " + value, startTime, selectedText);
            
        }, methodName, description + " | Locator: " + dropdownLocator + " | Value: " + value, startTime);
    }
    
    /**
     * Select option by index using Select class
     * 
     * @param dropdownLocator By locator for the dropdown select element
     * @param index Index of the option to select (0-based)
     * @return DropdownOperationResult indicating success/failure with details
     */
    public static DropdownOperationResult selectByIndex(By dropdownLocator, int index) {
        return selectByIndex(dropdownLocator, index, "Select option by index");
    }
    
    /**
     * Select option by index using Select class with custom description
     * 
     * @param dropdownLocator By locator for the dropdown select element
     * @param index Index of the option to select (0-based)
     * @param description Custom description for logging
     * @return DropdownOperationResult indicating success/failure with details
     */
    public static DropdownOperationResult selectByIndex(By dropdownLocator, int index, String description) {
        long startTime = System.currentTimeMillis();
        String methodName = "selectByIndex";
        
        log.info("[{}] Starting operation - Description: {} | Locator: {} | Index: {}", 
                methodName, description, dropdownLocator, index);
        
        if (dropdownLocator == null || index < 0) {
            String errorMsg = "Dropdown locator cannot be null and index cannot be negative";
            log.error("[{}] {}", methodName, errorMsg);
            return createFailureResult(errorMsg, startTime);
        }
        
        return executeSelectOperation(() -> {
            WebElement dropdownElement = waitForElementToBeClickable(dropdownLocator);
            Select dropdown = new Select(dropdownElement);
            
            if (!dropdownElement.isEnabled()) {
                throw new CustomDropdownException("Dropdown is disabled");
            }
            
            // Verify index is within bounds
            List<WebElement> options = dropdown.getOptions();
            if (index >= options.size()) {
                throw new CustomDropdownException("Index " + index + " is out of bounds. Available options: " + options.size());
            }
            
            dropdown.selectByIndex(index);
            
            // Verify selection
            String selectedText = dropdown.getFirstSelectedOption().getText();
            log.info("[{}] Successfully selected option - Description: {} | Index: {} | Text: {}", 
                    methodName, description, index, selectedText);
            return createSuccessResult("Option selected by index successfully: " + index, startTime, selectedText);
            
        }, methodName, description + " | Locator: " + dropdownLocator + " | Index: " + index, startTime);
    }
    
    /**
     * Get all options from Select dropdown
     * 
     * @param dropdownLocator By locator for the dropdown select element
     * @return DropdownOperationResult with list of option texts
     */
    public static DropdownOperationResult getAllSelectOptions(By dropdownLocator) {
        return getAllSelectOptions(dropdownLocator, "Get all dropdown options");
    }
    
    /**
     * Get all options from Select dropdown with custom description
     * 
     * @param dropdownLocator By locator for the dropdown select element
     * @param description Custom description for logging
     * @return DropdownOperationResult with list of option texts
     */
    public static DropdownOperationResult getAllSelectOptions(By dropdownLocator, String description) {
        long startTime = System.currentTimeMillis();
        String methodName = "getAllSelectOptions";
        
        log.info("[{}] Starting operation - Description: {} | Locator: {}", methodName, description, dropdownLocator);
        
        if (dropdownLocator == null) {
            String errorMsg = "Dropdown locator cannot be null";
            log.error("[{}] {}", methodName, errorMsg);
            return createFailureResult(errorMsg, startTime);
        }
        
        return executeSelectOperation(() -> {
            WebElement dropdownElement = waitForElementToBeVisible(dropdownLocator);
            Select dropdown = new Select(dropdownElement);
            
            List<WebElement> options = dropdown.getOptions();
            List<String> optionTexts = new ArrayList<>();
            
            for (WebElement option : options) {
                optionTexts.add(option.getText());
            }
            
            log.info("[{}] Successfully retrieved {} options - Description: {} | Options: {}", 
                    methodName, optionTexts.size(), description, optionTexts);
            return createSuccessResult("Retrieved " + optionTexts.size() + " options", startTime, optionTexts.toString());
            
        }, methodName, description + " | Locator: " + dropdownLocator, startTime);
    }
    
    /**
     * Get currently selected option from Select dropdown
     * 
     * @param dropdownLocator By locator for the dropdown select element
     * @return DropdownOperationResult with selected option text
     */
    public static DropdownOperationResult getSelectedOption(By dropdownLocator) {
        return getSelectedOption(dropdownLocator, "Get selected option");
    }
    
    /**
     * Get currently selected option from Select dropdown with custom description
     * 
     * @param dropdownLocator By locator for the dropdown select element
     * @param description Custom description for logging
     * @return DropdownOperationResult with selected option text
     */
    public static DropdownOperationResult getSelectedOption(By dropdownLocator, String description) {
        long startTime = System.currentTimeMillis();
        String methodName = "getSelectedOption";
        
        log.info("[{}] Starting operation - Description: {} | Locator: {}", methodName, description, dropdownLocator);
        
        if (dropdownLocator == null) {
            String errorMsg = "Dropdown locator cannot be null";
            log.error("[{}] {}", methodName, errorMsg);
            return createFailureResult(errorMsg, startTime);
        }
        
        return executeSelectOperation(() -> {
            WebElement dropdownElement = waitForElementToBeVisible(dropdownLocator);
            Select dropdown = new Select(dropdownElement);
            
            WebElement selectedOption = dropdown.getFirstSelectedOption();
            String selectedText = selectedOption.getText();
            String selectedValue = selectedOption.getAttribute("value");
            
            log.info("[{}] Successfully retrieved selected option - Description: {} | Text: {} | Value: {}", 
                    methodName, description, selectedText, selectedValue);
            return createSuccessResult("Selected option: " + selectedText, startTime, selectedText);
            
        }, methodName, description + " | Locator: " + dropdownLocator, startTime);
    }
    
    // ========== CUSTOM DROPDOWN METHODS ==========
    
    /**
     * Select option from custom dropdown by clicking trigger and then option
     * 
     * @param triggerLocator By locator for the dropdown trigger element
     * @param optionLocator By locator for the specific option to select
     * @return DropdownOperationResult indicating success/failure with details
     */
    public static DropdownOperationResult selectCustomDropdownOption(By triggerLocator, By optionLocator) {
        return selectCustomDropdownOption(triggerLocator, optionLocator, "Select custom dropdown option");
    }
    
    /**
     * Select option from custom dropdown by clicking trigger and then option with custom description
     * 
     * @param triggerLocator By locator for the dropdown trigger element
     * @param optionLocator By locator for the specific option to select
     * @param description Custom description for logging
     * @return DropdownOperationResult indicating success/failure with details
     */
    public static DropdownOperationResult selectCustomDropdownOption(By triggerLocator, By optionLocator, String description) {
        long startTime = System.currentTimeMillis();
        String methodName = "selectCustomDropdownOption";
        
        log.info("[{}] Starting operation - Description: {} | Trigger: {} | Option: {}", 
                methodName, description, triggerLocator, optionLocator);
        
        if (triggerLocator == null || optionLocator == null) {
            String errorMsg = "Trigger and option locators cannot be null";
            log.error("[{}] {}", methodName, errorMsg);
            return createFailureResult(errorMsg, startTime);
        }
        
        return executeCustomDropdownOperation(() -> {
            // Step 1: Click trigger to open dropdown
            WebElement triggerElement = waitForElementToBeClickable(triggerLocator);
            triggerElement.click();
            
            // Wait for dropdown to open
            Thread.sleep(getDropdownOpenWait());
            
            // Step 2: Wait for option to be clickable and click it
            WebElement optionElement = waitForElementToBeClickable(optionLocator);
            String optionText = optionElement.getText();
            
            optionElement.click();
            
            // Wait for selection to complete
            Thread.sleep(getOptionClickPause());
            
            log.info("[{}] Successfully selected custom dropdown option - Description: {} | Option: {}", 
                    methodName, description, optionText);
            return createSuccessResult("Custom dropdown option selected: " + optionText, startTime, optionText);
            
        }, methodName, description + " | Trigger: " + triggerLocator + " | Option: " + optionLocator, startTime);
    }
    
    /**
     * Select option from custom dropdown by text content
     * 
     * @param triggerLocator By locator for the dropdown trigger element
     * @param optionsContainerLocator By locator for the container holding all options
     * @param optionText Text content of the option to select
     * @return DropdownOperationResult indicating success/failure with details
     */
    public static DropdownOperationResult selectCustomDropdownByText(By triggerLocator, By optionsContainerLocator, String optionText) {
        return selectCustomDropdownByText(triggerLocator, optionsContainerLocator, optionText, "Select custom dropdown by text");
    }
    
    /**
     * Select option from custom dropdown by text content with custom description
     * 
     * @param triggerLocator By locator for the dropdown trigger element
     * @param optionsContainerLocator By locator for the container holding all options
     * @param optionText Text content of the option to select
     * @param description Custom description for logging
     * @return DropdownOperationResult indicating success/failure with details
     */
    public static DropdownOperationResult selectCustomDropdownByText(By triggerLocator, By optionsContainerLocator, 
                                                                   String optionText, String description) {
        long startTime = System.currentTimeMillis();
        String methodName = "selectCustomDropdownByText";
        
        log.info("[{}] Starting operation - Description: {} | Trigger: {} | Container: {} | Text: {}", 
                methodName, description, triggerLocator, optionsContainerLocator, optionText);
        
        if (triggerLocator == null || optionsContainerLocator == null || optionText == null || optionText.trim().isEmpty()) {
            String errorMsg = "Trigger locator, options container locator, and option text cannot be null or empty";
            log.error("[{}] {}", methodName, errorMsg);
            return createFailureResult(errorMsg, startTime);
        }
        
        return executeCustomDropdownOperation(() -> {
            // Step 1: Click trigger to open dropdown
            WebElement triggerElement = waitForElementToBeClickable(triggerLocator);
            triggerElement.click();
            
            // Wait for dropdown to open
            Thread.sleep(getDropdownOpenWait());
            
            // Step 2: Find and click the option with matching text
            WebElement optionsContainer = waitForElementToBeVisible(optionsContainerLocator);
            List<WebElement> options = optionsContainer.findElements(By.xpath(".//*"));
            
            WebElement targetOption = null;
            for (WebElement option : options) {
                if (option.getText().trim().equals(optionText.trim())) {
                    targetOption = option;
                    break;
                }
            }
            
            if (targetOption == null) {
                throw new CustomDropdownException("Option with text '" + optionText + "' not found in dropdown");
            }
            
            // Scroll to option if needed
            ((JavascriptExecutor) getDriver()).executeScript("arguments[0].scrollIntoView(true);", targetOption);
            
            // Click the option
            targetOption.click();
            
            // Wait for selection to complete
            Thread.sleep(getOptionClickPause());
            
            log.info("[{}] Successfully selected custom dropdown option by text - Description: {} | Text: {}", 
                    methodName, description, optionText);
            return createSuccessResult("Custom dropdown option selected by text: " + optionText, startTime, optionText);
            
        }, methodName, description + " | Trigger: " + triggerLocator + " | Text: " + optionText, startTime);
    }
    
    /**
     * Select option from custom dropdown using Actions class (for complex interactions)
     * 
     * @param triggerLocator By locator for the dropdown trigger element
     * @param optionLocator By locator for the specific option to select
     * @return DropdownOperationResult indicating success/failure with details
     */
    public static DropdownOperationResult selectCustomDropdownWithActions(By triggerLocator, By optionLocator) {
        return selectCustomDropdownWithActions(triggerLocator, optionLocator, "Select custom dropdown with Actions");
    }
    
    /**
     * Select option from custom dropdown using Actions class with custom description
     * 
     * @param triggerLocator By locator for the dropdown trigger element
     * @param optionLocator By locator for the specific option to select
     * @param description Custom description for logging
     * @return DropdownOperationResult indicating success/failure with details
     */
    public static DropdownOperationResult selectCustomDropdownWithActions(By triggerLocator, By optionLocator, String description) {
        long startTime = System.currentTimeMillis();
        String methodName = "selectCustomDropdownWithActions";
        
        log.info("[{}] Starting operation - Description: {} | Trigger: {} | Option: {}", 
                methodName, description, triggerLocator, optionLocator);
        
        if (triggerLocator == null || optionLocator == null) {
            String errorMsg = "Trigger and option locators cannot be null";
            log.error("[{}] {}", methodName, errorMsg);
            return createFailureResult(errorMsg, startTime);
        }
        
        return executeCustomDropdownOperation(() -> {
            Actions actions = new Actions(getDriver());
            
            // Step 1: Hover over trigger and click to open dropdown
            WebElement triggerElement = waitForElementToBeClickable(triggerLocator);
            actions.moveToElement(triggerElement).click().perform();
            
            // Wait for dropdown to open
            Thread.sleep(getDropdownOpenWait());
            
            // Step 2: Move to option and click
            WebElement optionElement = waitForElementToBeClickable(optionLocator);
            String optionText = optionElement.getText();
            
            actions.moveToElement(optionElement).click().perform();
            
            // Wait for selection to complete
            Thread.sleep(getOptionClickPause());
            
            log.info("[{}] Successfully selected custom dropdown option with Actions - Description: {} | Option: {}", 
                    methodName, description, optionText);
            return createSuccessResult("Custom dropdown option selected with Actions: " + optionText, startTime, optionText);
            
        }, methodName, description + " | Trigger: " + triggerLocator + " | Option: " + optionLocator, startTime);
    }
    
    /**
     * Get all options from custom dropdown
     * 
     * @param triggerLocator By locator for the dropdown trigger element
     * @param optionsContainerLocator By locator for the container holding all options
     * @return DropdownOperationResult with list of option texts
     */
    public static DropdownOperationResult getAllCustomDropdownOptions(By triggerLocator, By optionsContainerLocator) {
        return getAllCustomDropdownOptions(triggerLocator, optionsContainerLocator, "Get all custom dropdown options");
    }
    
    /**
     * Get all options from custom dropdown with custom description
     * 
     * @param triggerLocator By locator for the dropdown trigger element
     * @param optionsContainerLocator By locator for the container holding all options
     * @param description Custom description for logging
     * @return DropdownOperationResult with list of option texts
     */
    public static DropdownOperationResult getAllCustomDropdownOptions(By triggerLocator, By optionsContainerLocator, String description) {
        long startTime = System.currentTimeMillis();
        String methodName = "getAllCustomDropdownOptions";
        
        log.info("[{}] Starting operation - Description: {} | Trigger: {} | Container: {}", 
                methodName, description, triggerLocator, optionsContainerLocator);
        
        if (triggerLocator == null || optionsContainerLocator == null) {
            String errorMsg = "Trigger and options container locators cannot be null";
            log.error("[{}] {}", methodName, errorMsg);
            return createFailureResult(errorMsg, startTime);
        }
        
        return executeCustomDropdownOperation(() -> {
            // Step 1: Click trigger to open dropdown
            WebElement triggerElement = waitForElementToBeClickable(triggerLocator);
            triggerElement.click();
            
            // Wait for dropdown to open
            Thread.sleep(getDropdownOpenWait());
            
            // Step 2: Get all options
            WebElement optionsContainer = waitForElementToBeVisible(optionsContainerLocator);
            List<WebElement> options = optionsContainer.findElements(By.xpath(".//*[text()]"));
            
            List<String> optionTexts = new ArrayList<>();
            for (WebElement option : options) {
                String text = option.getText().trim();
                if (!text.isEmpty()) {
                    optionTexts.add(text);
                }
            }
            
            // Close dropdown by clicking outside or pressing Escape
            Actions actions = new Actions(getDriver());
            actions.sendKeys(Keys.ESCAPE).perform();
            
            log.info("[{}] Successfully retrieved {} custom dropdown options - Description: {} | Options: {}", 
                    methodName, optionTexts.size(), description, optionTexts);
            return createSuccessResult("Retrieved " + optionTexts.size() + " custom dropdown options", startTime, optionTexts.toString());
            
        }, methodName, description + " | Trigger: " + triggerLocator + " | Container: " + optionsContainerLocator, startTime);
    }
    
    /**
     * Check if custom dropdown is open/expanded
     * 
     * @param dropdownContainerLocator By locator for the dropdown container
     * @return DropdownOperationResult indicating if dropdown is open
     */
    public static DropdownOperationResult isCustomDropdownOpen(By dropdownContainerLocator) {
        return isCustomDropdownOpen(dropdownContainerLocator, "Check if custom dropdown is open");
    }
    
    /**
     * Check if custom dropdown is open/expanded with custom description
     * 
     * @param dropdownContainerLocator By locator for the dropdown container
     * @param description Custom description for logging
     * @return DropdownOperationResult indicating if dropdown is open
     */
    public static DropdownOperationResult isCustomDropdownOpen(By dropdownContainerLocator, String description) {
        long startTime = System.currentTimeMillis();
        String methodName = "isCustomDropdownOpen";
        
        log.info("[{}] Starting operation - Description: {} | Container: {}", methodName, description, dropdownContainerLocator);
        
        if (dropdownContainerLocator == null) {
            String errorMsg = "Dropdown container locator cannot be null";
            log.error("[{}] {}", methodName, errorMsg);
            return createFailureResult(errorMsg, startTime);
        }
        
        try {
            List<WebElement> containers = getDriver().findElements(dropdownContainerLocator);
            boolean isOpen = !containers.isEmpty() && containers.get(0).isDisplayed();
            
            String result = isOpen ? "Custom dropdown is open" : "Custom dropdown is closed";
            log.info("[{}] {} - Description: {}", methodName, result, description);
            
            return createSuccessResult(result, startTime, String.valueOf(isOpen));
            
        } catch (Exception e) {
            String errorMsg = "Failed to check custom dropdown state: " + e.getMessage();
            log.error("[{}] {} - Description: {}", methodName, errorMsg, description, e);
            
            if (shouldTakeScreenshot()) {
                takeScreenshot(methodName);
            }
            
            return createFailureResult(errorMsg, startTime);
        }
    }
    
    // ========== UTILITY METHODS ==========
    
    /**
     * Execute Select dropdown operation with retry logic and error handling
     */
    private static DropdownOperationResult executeSelectOperation(
            DropdownOperation operation, String methodName, String context, long startTime) {
        
        int retryCount = getRetryCount();
        Exception lastException = null;
        
        for (int attempt = 1; attempt <= retryCount; attempt++) {
            try {
                log.debug("[{}] Attempt {} of {} - Context: {}", methodName, attempt, retryCount, context);
                
                return operation.execute();
                
            } catch (TimeoutException e) {
                lastException = e;
                String errorMsg = String.format("Timeout waiting for dropdown element - Attempt %d/%d", attempt, retryCount);
                log.warn("[{}] {} - Context: {}", methodName, errorMsg, context);
                
                if (attempt < retryCount) {
                    waitBetweenRetries();
                }
                
            } catch (NoSuchElementException e) {
                lastException = e;
                String errorMsg = "Dropdown element not found";
                log.error("[{}] {} - Context: {}", methodName, errorMsg, context);
                break; // Don't retry for element not found
                
            } catch (UnexpectedTagNameException e) {
                lastException = e;
                String errorMsg = "Element is not a valid Select dropdown";
                log.error("[{}] {} - Context: {}", methodName, errorMsg, context);
                break; // Don't retry for wrong element type
                
            } catch (StaleElementReferenceException e) {
                lastException = e;
                String errorMsg = String.format("Stale element reference - Attempt %d/%d", attempt, retryCount);
                log.warn("[{}] {} - Context: {}", methodName, errorMsg, context);
                
                if (attempt < retryCount) {
                    waitBetweenRetries();
                }
                
            } catch (Exception e) {
                lastException = e;
                String errorMsg = String.format("Unexpected error in dropdown operation - Attempt %d/%d", attempt, retryCount);
                log.error("[{}] {} - Context: {} - Error: {}", methodName, errorMsg, context, e.getMessage(), e);
                
                if (attempt < retryCount) {
                    waitBetweenRetries();
                }
            }
        }
        
        // All attempts failed
        String finalError = String.format("Dropdown operation failed after %d attempts: %s", 
                retryCount, lastException.getMessage());
        log.error("[{}] {} - Context: {}", methodName, finalError, context);
        
        if (shouldTakeScreenshot()) {
            takeScreenshot(methodName);
        }
        
        return createFailureResult(finalError, startTime);
    }
    
    /**
     * Execute custom dropdown operation with retry logic and error handling
     */
    private static DropdownOperationResult executeCustomDropdownOperation(
            DropdownOperation operation, String methodName, String context, long startTime) {
        
        int retryCount = getRetryCount();
        Exception lastException = null;
        
        for (int attempt = 1; attempt <= retryCount; attempt++) {
            try {
                log.debug("[{}] Attempt {} of {} - Context: {}", methodName, attempt, retryCount, context);
                
                return operation.execute();
                
            } catch (TimeoutException e) {
                lastException = e;
                String errorMsg = String.format("Timeout waiting for dropdown element - Attempt %d/%d", attempt, retryCount);
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
                
            } catch (ElementClickInterceptedException e) {
                lastException = e;
                String errorMsg = String.format("Element click intercepted - Attempt %d/%d", attempt, retryCount);
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
                String errorMsg = "Dropdown element not found";
                log.error("[{}] {} - Context: {}", methodName, errorMsg, context);
                break; // Don't retry for element not found
                
            } catch (Exception e) {
                lastException = e;
                String errorMsg = String.format("Unexpected error in custom dropdown operation - Attempt %d/%d", attempt, retryCount);
                log.error("[{}] {} - Context: {} - Error: {}", methodName, errorMsg, context, e.getMessage(), e);
                
                if (attempt < retryCount) {
                    waitBetweenRetries();
                }
            }
        }
        
        // All attempts failed
        String finalError = String.format("Custom dropdown operation failed after %d attempts: %s", 
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
        return Integer.parseInt(config.getProperty("dropdown.timeout", String.valueOf(DEFAULT_TIMEOUT)));
    }
    
    /**
     * Get retry count from configuration
     */
    private static int getRetryCount() {
        return Integer.parseInt(config.getProperty("dropdown.retry.count", String.valueOf(DEFAULT_RETRY_COUNT)));
    }
    
    /**
     * Get dropdown open wait time from configuration
     */
    private static int getDropdownOpenWait() {
        return Integer.parseInt(config.getProperty("dropdown.open.wait", String.valueOf(DROPDOWN_OPEN_WAIT)));
    }
    
    /**
     * Get option click pause time from configuration
     */
    private static int getOptionClickPause() {
        return Integer.parseInt(config.getProperty("dropdown.option.pause", String.valueOf(OPTION_CLICK_PAUSE)));
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
     * Create success result
     */
    private static DropdownOperationResult createSuccessResult(String message, long startTime) {
        return createSuccessResult(message, startTime, null);
    }
    
    /**
     * Create success result with additional data
     */
    private static DropdownOperationResult createSuccessResult(String message, long startTime, String data) {
        long executionTime = System.currentTimeMillis() - startTime;
        log.info("Dropdown operation completed successfully in {}ms: {}", executionTime, message);
        
        return new DropdownOperationResult(true, message, executionTime, data);
    }
    
    /**
     * Create failure result
     */
    private static DropdownOperationResult createFailureResult(String errorMessage, long startTime) {
        long executionTime = System.currentTimeMillis() - startTime;
        log.error("Dropdown operation failed after {}ms: {}", executionTime, errorMessage);
        
        return new DropdownOperationResult(false, errorMessage, executionTime, null);
    }
    
    /**
     * Functional interface for dropdown operations
     */
    @FunctionalInterface
    private interface DropdownOperation {
        DropdownOperationResult execute() throws Exception;
    }
    
    /**
     * Result class for dropdown operations
     */
    public static class DropdownOperationResult {
        private final boolean success;
        private final String message;
        private final long executionTimeMs;
        private final String data;
        
        public DropdownOperationResult(boolean success, String message, long executionTimeMs, String data) {
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
            return String.format("DropdownOperationResult{success=%s, message='%s', executionTime=%dms, data='%s'}", 
                    success, message, executionTimeMs, data);
        }
    }
    
    /**
     * Custom exception for dropdown operations
     */
    public static class CustomDropdownException extends RuntimeException {
        public CustomDropdownException(String message) {
            super(message);
        }
        
        public CustomDropdownException(String message, Throwable cause) {
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