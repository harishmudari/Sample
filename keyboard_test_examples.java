package com.enterprise.tests;

import org.testng.annotations.*;
import org.testng.Assert;
import org.openqa.selenium.WebDriver;
import org.openqa.selenium.chrome.ChromeDriver;
import org.openqa.selenium.firefox.FirefoxDriver;
import org.openqa.selenium.By;
import org.openqa.selenium.Keys;
import org.openqa.selenium.WebElement;
import io.github.bonigarcia.wdm.WebDriverManager;
import com.enterprise.selenium.keywords.KeyboardEventsHandler;
import com.enterprise.selenium.keywords.KeyboardEventsHandler.KeyboardActionResponse;
import com.enterprise.selenium.keywords.KeyboardEventsHandler.KeyboardActionException;
import com.enterprise.selenium.keywords.fluent.FluentKeyboardActions;
import com.enterprise.selenium.keywords.builder.KeyboardSequenceBuilder;
import com.enterprise.selenium.keywords.builder.KeyboardSequenceBuilder.KeyboardSequenceResponse;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import java.util.Arrays;
import java.util.List;

/**
 * Comprehensive test suite for keyboard events handling
 * Demonstrates all enterprise-level features with real-world scenarios
 */
public class KeyboardEventsComprehensiveTest {
    
    private static final Logger log = LoggerFactory.getLogger(KeyboardEventsComprehensiveTest.class);
    
    private WebDriver driver;
    private String browserName;
    
    // Test page URLs and locators
    private static final String TEST_FORM_URL = "https://the-internet.herokuapp.com/login";
    private static final String DROPDOWN_URL = "https://the-internet.herokuapp.com/dropdown";
    private static final String CHECKBOXES_URL = "https://the-internet.herokuapp.com/checkboxes";
    
    // Login page locators
    private static final By USERNAME_FIELD = By.id("username");
    private static final By PASSWORD_FIELD = By.id("password");
    private static final By LOGIN_BUTTON = By.cssSelector("button[type='submit']");
    private static final By FLASH_MESSAGE = By.id("flash");
    
    // Dropdown page locators
    private static final By DROPDOWN_SELECT = By.id("dropdown");
    
    @Parameters({"browser"})
    @BeforeMethod
    public void setUp(@Optional("chrome") String browser) {
        this.browserName = browser;
        log.info("Setting up keyboard events test with browser: {}", browser);
        
        driver = initializeDriver(browser);
        KeyboardEventsHandler.setDriver(driver);
        
        log.info("WebDriver initialized for keyboard events testing");
    }
    
    @AfterMethod
    public void tearDown() {
        try {
            if (driver != null) {
                log.info("Closing browser: {}", browserName);
                driver.quit();
            }
        } finally {
            KeyboardEventsHandler.cleanup();
        }
    }
    
    private WebDriver initializeDriver(String browserType) {
        switch (browserType.toLowerCase()) {
            case "chrome":
                WebDriverManager.chromedriver().setup();
                return new ChromeDriver();
            case "firefox":
                WebDriverManager.firefoxdriver().setup();
                return new FirefoxDriver();
            default:
                throw new IllegalArgumentException("Browser not supported: " + browserType);
        }
    }
    
    /**
     * Test 1: Basic text input with sendKeys
     */
    @Test(priority = 1, description = "Test basic text input using sendKeys")
    public void testBasicTextInput() throws KeyboardActionException {
        log.info("Starting basic text input test");
        
        driver.get(TEST_FORM_URL);
        
        // Test basic text input
        KeyboardActionResponse response1 = KeyboardEventsHandler.sendText(
            USERNAME_FIELD, "testuser", false, 10);
        
        Assert.assertTrue(response1.isSuccess(), "Username text input should succeed");
        Assert.assertTrue(response1.getExecutionTimeMs() < 5000, "Text input should complete within 5 seconds");
        
        // Test text input with clearing
        KeyboardActionResponse response2 = KeyboardEventsHandler.sendText(
            PASSWORD_FIELD, "testpassword", true, 10);
        
        Assert.assertTrue(response2.isSuccess(), "Password text input with clear should succeed");
        
        // Verify text was entered
        WebElement usernameElement = driver.findElement(USERNAME_FIELD);
        String enteredText = usernameElement.getAttribute("value");
        Assert.assertEquals(enteredText, "testuser", "Entered username text should match");
        
        log.info("Basic text input test completed successfully");
    }
    
    /**
     * Test 2: Special keys (ENTER, TAB, ESC, etc.)
     */
    @Test(priority = 2, description = "Test special key events")
    public void testSpecialKeys() throws KeyboardActionException {
        log.info("Starting special keys test");
        
        driver.get(TEST_FORM_URL);
        
        // Input username
        KeyboardEventsHandler.sendText(USERNAME_FIELD, "tomsmith", false, 10);
        
        // Test TAB navigation
        KeyboardActionResponse tabResponse = KeyboardEventsHandler.sendSpecialKey(
            USERNAME_FIELD, "TAB", 5);
        
        Assert.assertTrue(tabResponse.isSuccess(), "TAB key should work successfully");
        
        // Verify focus moved to password field (check active element)
        WebElement activeElement = driver.switchTo().activeElement();
        String activeElementId = activeElement.getAttribute("id");
        Assert.assertEquals(activeElementId, "password", "Focus should move to password field after TAB");
        
        // Test ENTER key for form submission
        KeyboardEventsHandler.sendText(PASSWORD_FIELD, "SuperSecretPassword!", false, 5);
        
        KeyboardActionResponse enterResponse = KeyboardEventsHandler.sendSpecialKey(
            PASSWORD_FIELD, Keys.ENTER, 5);
        
        Assert.assertTrue(enterResponse.isSuccess(), "ENTER key should work successfully");
        
        // Wait for form submission and verify
        Thread.sleep(2000);
        WebElement flashMessage = driver.findElement(FLASH_MESSAGE);
        String flashText = flashMessage.getText();
        Assert.assertTrue(flashText.contains("You logged into a secure area!"), 
            "Form should be submitted successfully with ENTER key");
        
        log.info("Special keys test completed successfully");
    }
    
    /**
     * Test 3: Key combinations (Ctrl+A, Ctrl+C, Ctrl+V, etc.)
     */
    @Test(priority = 3, description = "Test key combinations and shortcuts")
    public void testKeyCombinations() throws KeyboardActionException {
        log.info("Starting key combinations test");
        
        driver.get(TEST_FORM_URL);
        
        // Input some text first
        KeyboardEventsHandler.sendText(USERNAME_FIELD, "initialtext", false, 10);
        
        // Test Ctrl+A (Select All)
        KeyboardActionResponse selectAllResponse = KeyboardEventsHandler.sendKeyCombo(
            USERNAME_FIELD, Arrays.asList(Keys.CONTROL), Keys.chord("a"), 5);
        
        Assert.assertTrue(selectAllResponse.isSuccess(), "Ctrl+A should work successfully");
        
        // Test Ctrl+C (Copy)
        KeyboardActionResponse copyResponse = KeyboardEventsHandler.sendKeyCombo(
            USERNAME_FIELD, Arrays.asList(Keys.CONTROL), Keys.chord("c"), 5);
        
        Assert.assertTrue(copyResponse.isSuccess(), "Ctrl+C should work successfully");
        
        // Move to password field and test Ctrl+V (Paste)
        driver.findElement(PASSWORD_FIELD).click();
        
        KeyboardActionResponse pasteResponse = KeyboardEventsHandler.sendKeyCombo(
            PASSWORD_FIELD, Arrays.asList(Keys.CONTROL), Keys.chord("v"), 5);
        
        Assert.assertTrue(pasteResponse.isSuccess(), "Ctrl+V should work successfully");
        
        // Verify paste worked
        Thread.sleep(1000);
        WebElement passwordElement = driver.findElement(PASSWORD_FIELD);
        String pastedText = passwordElement.getAttribute("value");
        Assert.assertEquals(pastedText, "initialtext", "Pasted text should match copied text");
        
        log.info("Key combinations test completed successfully");
    }
    
    /**
     * Test 4: Fluent API usage
     */
    @Test(priority = 4, description = "Test fluent API for keyboard actions")
    public void testFluentApiKeyboardActions() throws KeyboardActionException {
        log.info("Starting fluent API keyboard actions test");
        
        driver.get(TEST_FORM_URL);
        
        // Use fluent API for complex keyboard interactions
        KeyboardActionResponse response1 = FluentKeyboardActions.onElement(USERNAME_FIELD)
            .withTimeout(10)
            .clearFirst()
            .typeText("fluentuser");
        
        Assert.assertTrue(response1.isSuccess(), "Fluent API text input should succeed");
        
        // Chain multiple actions
        KeyboardActionResponse response2 = FluentKeyboardActions.onElement(USERNAME_FIELD)
            .selectAll();
        
        Assert.assertTrue(response2.isSuccess(), "Fluent API select all should succeed");
        
        KeyboardActionResponse response3 = FluentKeyboardActions.onElement(USERNAME_FIELD)
            .copy();
        
        Assert.assertTrue(response3.isSuccess(), "Fluent API copy should succeed");
        
        // Test navigation and paste
        KeyboardActionResponse response4 = FluentKeyboardActions.onElement(USERNAME_FIELD)
            .pressTab();
        
        Assert.assertTrue(response4.isSuccess(), "Fluent API tab navigation should succeed");
        
        KeyboardActionResponse response5 = FluentKeyboardActions.onElement(PASSWORD_FIELD)
            .paste();
        
        Assert.assertTrue(response5.isSuccess(), "Fluent API paste should succeed");
        
        // Submit using fluent API
        KeyboardActionResponse response6 = FluentKeyboardActions.onElement(PASSWORD_FIELD)
            .pressEnter();
        
        Assert.assertTrue(response6.isSuccess(), "Fluent API enter should succeed");
        
        log.info("Fluent API keyboard actions test completed successfully");
    }
    
    /**
     * Test 5: Human-like typing simulation
     */
    @Test(priority = 5, description = "Test human-like typing with variable delays")
    public void testHumanLikeTyping() throws KeyboardActionException {
        log.info("Starting human-like typing test");
        
        driver.get(TEST_FORM_URL);
        
        long startTime = System.currentTimeMillis();
        
        // Test human-like typing with custom delays
        KeyboardActionResponse response = KeyboardEventsHandler.typeHumanLike(
            USERNAME_FIELD, "humanuser", true, 80, 200, 10);
        
        long executionTime = System.currentTimeMillis() - startTime;
        
        Assert.assertTrue(response.isSuccess(), "Human-like typing should succeed");
        
        // Verify timing - should take longer due to delays
        String testText = "humanuser";
        int expectedMinTime = testText.length() * 80; // minimum delays
        Assert.assertTrue(executionTime >= expectedMinTime, 
            "Human-like typing should take at least the minimum expected time");
        
        // Verify text was entered correctly
        WebElement usernameElement = driver.findElement(USERNAME_FIELD);
        String enteredText = usernameElement.getAttribute("value");
        Assert.assertEquals(enteredText, testText, "Human-like typed text should match input");
        
        // Test fluent API human-like typing
        KeyboardActionResponse fluentResponse = FluentKeyboardActions.onElement(PASSWORD_FIELD)
            .humanLike(60, 150)
            .clearFirst()
            .typeText("humanpassword");
        
        Assert.assertTrue(fluentResponse.isSuccess(), "Fluent API human-like typing should succeed");
        
        log.info("Human-like typing test completed successfully");
    }
    
    /**
     * Test 6: Keyboard sequence builder
     */
    @Test(priority = 6, description = "Test keyboard sequence builder for complex workflows")
    public void testKeyboardSequenceBuilder() throws KeyboardActionException {
        log.info("Starting keyboard sequence builder test");
        
        driver.get(TEST_FORM_URL);
        
        // Build and execute complex keyboard sequence
        KeyboardSequenceResponse sequenceResponse = KeyboardSequenceBuilder.create()
            .withDefaultTimeout(10)
            .onElement(USERNAME_FIELD)
            .typeAndClear("sequenceuser")
            .tab()
            .onElement(PASSWORD_FIELD)
            .type("sequencepass")
            .wait(500)
            .enter()
            .execute();
        
        Assert.assertTrue(sequenceResponse.isAllSuccess(), "Keyboard sequence should execute successfully");
        Assert.assertTrue(sequenceResponse.getTotalExecutionTime() > 500, 
            "Sequence should include wait time");
        
        // Verify form submission
        Thread.sleep(2000);
        WebElement flashMessage = driver.findElement(FLASH_MESSAGE);
        String flashText = flashMessage.getText();
        Assert.assertTrue(flashText.contains("You logged into a secure area!"), 
            "Form should be submitted by keyboard sequence");
        
        log.info("Keyboard sequence builder test completed successfully - Total time: {}ms", 
                sequenceResponse.getTotalExecutionTime());
    }
    
    /**
     * Test 7: Arrow key navigation
     */
    @Test(priority = 7, description = "Test arrow key navigation in dropdown")
    public void testArrowKeyNavigation() throws KeyboardActionException {
        log.info("Starting arrow key navigation test");
        
        driver.get(DROPDOWN_URL);
        
        // Click on dropdown to open
        WebElement dropdown = driver.findElement(DROPDOWN_SELECT);
        dropdown.click();
        
        // Use arrow keys to navigate
        KeyboardActionResponse downResponse = FluentKeyboardActions.onElement(DROPDOWN_SELECT)
            .pressArrowDown();
        
        Assert.assertTrue(downResponse.isSuccess(), "Arrow down should work successfully");
        
        KeyboardActionResponse downResponse2 = FluentKeyboardActions.onElement(DROPDOWN_SELECT)
            .pressArrowDown();
        
        Assert.assertTrue(downResponse2.isSuccess(), "Second arrow down should work successfully");
        
        // Press Enter to select
        KeyboardActionResponse enterResponse = FluentKeyboardActions.onElement(DROPDOWN_SELECT)
            .pressEnter();
        
        Assert.assertTrue(enterResponse.isSuccess(), "Enter to select should work successfully");
        
        log.info("Arrow key navigation test completed successfully");
    }
    
    /**
     * Test 8: Function keys and global shortcuts
     */
    @Test(priority = 8, description = "Test function keys and global shortcuts")
    public void testFunctionKeysAndGlobalShortcuts() throws KeyboardActionException {
        log.info("Starting function keys and global shortcuts test");
        
        driver.get(TEST_FORM_URL);
        
        // Test F5 for refresh
        KeyboardActionResponse f5Response = FluentKeyboardActions.global()
            .pressF5();
        
        Assert.assertTrue(f5Response.isSuccess(), "F5 refresh should work successfully");
        
        // Wait for page reload
        Thread.sleep(3000);
        
        // Verify page was refreshed (form fields should be empty)
        WebElement usernameElement = driver.findElement(USERNAME_FIELD);
        String usernameValue = usernameElement.getAttribute("value");
        Assert.assertTrue(usernameValue.isEmpty(), "Username field should be empty after refresh");
        
        // Test Ctrl+Shift+I (Developer Tools) - Note: This might not work in all environments
        try {
            KeyboardActionResponse devToolsResponse = FluentKeyboardActions.global()
                .keyCombo("CTRL+SHIFT+I");
            
            // Don't assert success since dev tools behavior varies
            log.info("Developer tools shortcut attempted: {}", devToolsResponse.isSuccess());
            
        } catch (Exception e) {
            log.warn("Developer tools shortcut not available in this environment: {}", e.getMessage());
        }
        
        log.info("Function keys and global shortcuts test completed");
    }
    
    /**
     * Test 9: Error handling and retry mechanisms
     */
    @Test(priority = 9, description = "Test error handling and retry mechanisms")
    public void testErrorHandlingAndRetry() {
        log.info("Starting error handling and retry test");
        
        driver.get(TEST_FORM_URL);
        
        // Test with non-existent element
        By nonExistentElement = By.id("does-not-exist");
        
        try {
            KeyboardActionResponse response = KeyboardEventsHandler.sendText(
                nonExistentElement, "test", false, 2);
            
            Assert.assertFalse(response.isSuccess(), "Non-existent element should fail");
            Assert.assertTrue(response.getExecutionTimeMs() >= 2000, 
                "Should wait for timeout duration");
            
            log.info("Non-existent element correctly failed: {}", response.getMessage());
            
        } catch (KeyboardActionException e) {
            log.info("Expected exception for non-existent element: {}", e.getMessage());
            Assert.assertTrue(e.getMessage().contains("failed"), "Exception should indicate failure");
        }
        
        // Test null input handling
        try {
            KeyboardEventsHandler.sendText(USERNAME_FIELD, null, false, 5);
            Assert.fail("Should throw exception for null text");
        } catch (KeyboardActionException e) {
            Assert.assertTrue(e.getMessage().contains("null"), "Should mention null parameter");
            log.info("Null text correctly rejected: {}", e.getMessage());
        }
        
        // Test null locator handling
        try {
            KeyboardEventsHandler.sendText(null, "test", false, 5);
            Assert.fail("Should throw exception for null locator");
        } catch (KeyboardActionException e) {
            Assert.assertTrue(e.getMessage().contains("null"), "Should mention null locator");
            log.info("Null locator correctly rejected: {}", e.getMessage());
        }
        
        log.info("Error handling and retry test completed successfully");
    }
    
    /**
     * Test 10: Performance benchmarking
     */
    @Test(priority = 10, description = "Performance benchmarking for keyboard actions")
    public void testPerformanceBenchmarking() throws KeyboardActionException {
        log.info("Starting performance benchmarking test");
        
        driver.get(TEST_FORM_URL);
        
        int iterations = 5;
        long totalTime = 0;
        
        for (int i = 0; i < iterations; i++) {
            // Clear field first
            driver.findElement(USERNAME_FIELD).clear();
            
            long startTime = System.currentTimeMillis();
            
            KeyboardActionResponse response = KeyboardEventsHandler.sendText(
                USERNAME_FIELD, "benchmarkuser" + i, false, 10);
            
            long executionTime = response.getExecutionTimeMs();
            totalTime += executionTime;
            
            Assert.assertTrue(response.isSuccess(), "Benchmark iteration " + (i+1) + " should succeed");
            
            log.debug("Benchmark iteration {}: {}ms", i+1, executionTime);
        }
        
        double averageTime = (double) totalTime / iterations;
        
        log.info("Performance benchmark completed - Total: {}ms, Average: {:.2f}ms, Iterations: {}", 
                totalTime, averageTime, iterations);
        
        // Assert reasonable performance (adjust threshold based on requirements)
        Assert.assertTrue(averageTime < 2000, "Average keyboard action time should be under 2 seconds");
        
        // Test human-like typing performance
        long humanTypingStart = System.currentTimeMillis();
        
        KeyboardActionResponse humanResponse = KeyboardEventsHandler.typeHumanLike(
            PASSWORD_FIELD, "performancetest", true, 30, 80, 10);
        
        long humanTypingTime = System.currentTimeMillis() - humanTypingStart;
        
        Assert.assertTrue(humanResponse.isSuccess(), "Human-like typing benchmark should succeed");
        
        log.info("Human-like typing benchmark: {}ms for {} characters", 
                humanTypingTime, "performancetest".length());
    }
    
    /**
     * Test 11: Cross-browser keyboard compatibility
     */
    @Test(priority = 11, description = "Test keyboard events across different browsers")
    public void testCrossBrowserKeyboardCompatibility() throws KeyboardActionException {
        log.info("Starting cross-browser keyboard compatibility test for: {}", browserName);
        
        driver.get(TEST_FORM_URL);
        
        // Test basic keyboard operations in current browser
        KeyboardActionResponse typeResponse = KeyboardEventsHandler.sendText(
            USERNAME_FIELD, "browsertest_" + browserName, false, 10);
        
        Assert.assertTrue(typeResponse.isSuccess(), 
            "Text input should work in " + browserName);
        
        KeyboardActionResponse tabResponse = FluentKeyboardActions.onElement(USERNAME_FIELD)
            .pressTab();
        
        Assert.assertTrue(tabResponse.isSuccess(), 
            "Tab navigation should work in " + browserName);
        
        KeyboardActionResponse comboResponse = FluentKeyboardActions.onElement(PASSWORD_FIELD)
            .typeText("password123")
            .selectAll()
            .copy();
        
        Assert.assertTrue(comboResponse.isSuccess(), 
            "Key combinations should work in " + browserName);
        
        // Log browser-specific performance
        log.info("Browser {} - Type: {}ms, Tab: {}ms, Combo: {}ms", 
                browserName, 
                typeResponse.getExecutionTimeMs(),
                tabResponse.getExecutionTimeMs(),
                comboResponse.getExecutionTimeMs());
        
        log.info("Cross-browser keyboard compatibility test completed for: {}", browserName);
    }
    
    /**
     * Data provider for cross-browser testing
     */
    @DataProvider(name = "browsers")
    public Object[][] getBrowsers() {
        return new Object[][] {
            {"chrome"},
            {"firefox"}
        };
    }
    
    /**
     * Test 12: Real-world form filling scenario
     */
    @Test(priority = 12, description = "Real-world form filling with keyboard events")
    public void testRealWorldFormFilling() throws KeyboardActionException {
        log.info("Starting real-world form filling test");
        
        driver.get(TEST_FORM_URL);
        
        // Simulate real user behavior: type username, tab to password, type password, submit
        KeyboardSequenceResponse formFillSequence = KeyboardSequenceBuilder.create()
            .withDefaultTimeout(10)
            .onElement(USERNAME_FIELD)
            .typeAndClear("realworlduser")
            .wait(300) // Pause like a real user
            .tab()
            .onElement(PASSWORD_FIELD)
            .type("MySecureP@ssw0rd!")
            .wait(200)
            .enter()
            .execute();
        
        Assert.assertTrue(formFillSequence.isAllSuccess(), "Real-world form filling should succeed");
        
        // Verify successful login
        Thread.sleep(2000);
        WebElement flashMessage = driver.findElement(FLASH_MESSAGE);
        String flashText = flashMessage.getText();
        
        if (flashText.contains("You logged into a secure area!")) {
            log.info("Real-world form filling test: LOGIN SUCCESSFUL");
        } else {
            log.info("Real-world form filling test: LOGIN FAILED (Expected with demo credentials)");
            Assert.assertTrue(flashText.contains("invalid"), "Should show invalid credentials message");
        }
        
        log.info("Real-world form filling test completed - Execution time: {}ms", 
                formFillSequence.getTotalExecutionTime());
    }
}

/**
 * Utility class for advanced keyboard operations
 */
package com.enterprise.selenium.utils;

import org.openqa.selenium.By;
import org.openqa.selenium.Keys;
import org.openqa.selenium.WebDriver;
import org.openqa.selenium.WebElement;
import org.openqa.selenium.JavascriptExecutor;
import com.enterprise.selenium.keywords.KeyboardEventsHandler;
import com.enterprise.selenium.keywords.fluent.FluentKeyboardActions;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import java.util.List;
import java.util.Arrays;

public class AdvancedKeyboardUtils {
    private static final Logger log = LoggerFactory.getLogger(AdvancedKeyboardUtils.class);
    
    /**
     * Simulate realistic typing speed based on text complexity
     */
    public static boolean typeWithRealisticSpeed(By locator, String text) {
        try {
            // Calculate delays based on text complexity
            int baseDelay = 60;
            int maxDelay = 180;
            
            // Adjust for special characters and numbers
            if (text.matches(".*[!@#$%^&*()_+\\-=\\[\\]{};':\"\\\\|,.<>\\?].*")) {
                baseDelay += 20; // Special chars take longer
                maxDelay += 40;
            }
            
            if (text.matches(".*\\d.*")) {
                baseDelay += 10; // Numbers take slightly longer
                maxDelay += 20;
            }
            
            return FluentKeyboardActions.onElement(locator)
                    .humanLike(baseDelay, maxDelay)
                    .typeText(text)
                    .isSuccess();
                    
        } catch (Exception e) {
            log.error("Realistic typing failed: {}", e.getMessage());
            return false;
        }
    }
    
    /**
     * Smart paste operation with clipboard verification
     */
    public static boolean smartPaste(By sourceLocator, By targetLocator) {
        try {
            // Select and copy from source
            boolean copySuccess = FluentKeyboardActions.onElement(sourceLocator)
                    .selectAll()
                    .copy()
                    .isSuccess();
            
            if (!copySuccess) {
                return false;
            }
            
            // Paste to target
            return FluentKeyboardActions.onElement(targetLocator)
                    .clearFirst()
                    .paste()
                    .isSuccess();
                    
        } catch (Exception e) {
            log.error("Smart paste failed: {}", e.getMessage());
            return false;
        }
    }
    
    /**
     * Navigate through form fields using Tab with validation
     */
    public static boolean navigateFormFields(List<By> fieldLocators, List<String> values) {
        if (fieldLocators.size() != values.size()) {
            log.error("Field locators and values count mismatch");
            return false;
        }
        
        try {
            for (int i = 0; i < fieldLocators.size(); i++) {
                By locator = fieldLocators.get(i);
                String value = values.get(i);
                
                // Type value in current field
                boolean typeSuccess = FluentKeyboardActions.onElement(locator)
                        .clearFirst()
                        .typeText(value)
                        .isSuccess();
                
                if (!typeSuccess) {
                    log.error("Failed to type value in field {}", i);
                    return false;
                }
                
                // Tab to next field (except for last field)
                if (i < fieldLocators.size() - 1) {
                    boolean tabSuccess = FluentKeyboardActions.onElement(locator)
                            .pressTab()
                            .isSuccess();
                    
                    if (!tabSuccess) {
                        log.error("Failed to tab from field {}", i);
                        return false;
                    }
                }
            }
            
            return true;
            
        } catch (Exception e) {
            log.error("Form field navigation failed: {}", e.getMessage());
            return false;
        }
    }
    
    /**
     * Handle dropdown navigation with arrow keys
     */
    public static boolean selectDropdownWithArrows(By dropdownLocator, int optionIndex) {
        try {
            // Click to open dropdown
            WebDriver driver = KeyboardEventsHandler.getDriver();
            driver.findElement(dropdownLocator).click();
            
            // Navigate to desired option
            for (int i = 0; i < optionIndex; i++) {
                boolean arrowSuccess = FluentKeyboardActions.onElement(dropdownLocator)
                        .pressArrowDown()
                        .isSuccess();
                
                if (!arrowSuccess) {
                    log.error("Failed to navigate with arrow key at index {}", i);
                    return false;
                }
                
                Thread.sleep(100); // Small delay between arrow presses
            }
            
            // Select option with Enter
            return FluentKeyboardActions.onElement(dropdownLocator)
                    .pressEnter()
                    .isSuccess();
                    
        } catch (Exception e) {
            log.error("Dropdown arrow navigation failed: {}", e.getMessage());
            return false;
        }
    }
    
    /**
     * Execute emergency escape sequence (useful for modal dialogs, etc.)
     */
    public static boolean emergencyEscape() {
        try {
            // Try multiple escape strategies
            boolean escapeSuccess = FluentKeyboardActions.global()
                    .pressEscape()
                    .isSuccess();
            
            if (!escapeSuccess) {
                // Try Alt+F4 as fallback
                return FluentKeyboardActions.global()
                        .keyCombo(Arrays.asList(Keys.ALT), Keys.F4)
                        .isSuccess();
            }
            
            return true;
            
        } catch (Exception e) {
            log.error("Emergency escape failed: {}", e.getMessage());
            return false;
        }
    }
    
    /**
     * Clear all form fields on page
     */
    public static int clearAllInputFields() {
        int clearedCount = 0;
        
        try {
            WebDriver driver = KeyboardEventsHandler.getDriver();
            List<WebElement> inputFields = driver.findElements(By.tagName("input"));
            
            for (WebElement field : inputFields) {
                String type = field.getAttribute("type");
                
                if ("text".equals(type) || "password".equals(type) || 
                    "email".equals(type) || "number".equals(type) || type == null) {
                    
                    try {
                        field.click();
                        boolean clearSuccess = FluentKeyboardActions.onElement(
                                By.xpath("//input[@id='" + field.getAttribute("id") + "']"))
                                .selectAll()
                                .pressDelete()
                                .isSuccess();
                        
                        if (clearSuccess) {
                            clearedCount++;
                        }
                        
                    } catch (Exception e) {
                        log.warn("Could not clear field: {}", e.getMessage());
                    }
                }
            }
            
        } catch (Exception e) {
            log.error("Clear all fields failed: {}", e.getMessage());
        }
        
        log.info("Cleared {} input fields", clearedCount);
        return clearedCount;
    }
}