# selenium-config.properties
# =================================
# Enterprise Selenium Configuration
# =================================

# Element wait timeouts (seconds)
element.timeout=15
element.polling.interval=0.5

# Retry configuration
retry.count=3
retry.interval=1000

# Screenshot configuration  
screenshot.on.failure=true
screenshot.path=./screenshots/

# Logging configuration
log.level=INFO
log.method.execution.time=true

# Browser configuration
browser.implicit.wait=5
browser.page.load.timeout=30

# Test data configuration
test.data.path=./testdata/
config.environment=QA

# =================================
# Usage Examples
# =================================

/**
 * Example usage class demonstrating the ElementStateValidator keyword
 */
package com.enterprise.tests.examples;

import org.openqa.selenium.By;
import org.openqa.selenium.WebDriver;
import org.openqa.selenium.chrome.ChromeDriver;
import org.testng.Assert;
import org.testng.annotations.*;
import com.enterprise.selenium.keywords.ElementStateValidator;
import com.enterprise.selenium.keywords.ElementStateValidator.ElementStateResponse;
import com.enterprise.selenium.keywords.ElementStateValidator.ElementStateValidationException;

public class ElementStateValidatorUsageExample {
    
    private WebDriver driver;
    
    @BeforeMethod
    public void setUp() {
        driver = new ChromeDriver();
        ElementStateValidator.setDriver(driver);
        driver.get("https://example.com/login");
    }
    
    @AfterMethod
    public void tearDown() {
        ElementStateValidator.cleanup();
        if (driver != null) {
            driver.quit();
        }
    }
    
    /**
     * Example 1: Complete element state validation
     */
    @Test
    public void testCompleteElementValidation() throws ElementStateValidationException {
        By loginButton = By.id("login-btn");
        
        ElementStateResponse response = ElementStateValidator.validateElementState(loginButton);
        
        // Assert all conditions
        Assert.assertTrue(response.isSuccess(), "Element validation should succeed");
        Assert.assertTrue(response.isDisplayed(), "Login button should be displayed");
        Assert.assertTrue(response.isEnabled(), "Login button should be enabled");
        
        // Log execution time for performance monitoring
        System.out.println("Validation completed in: " + response.getExecutionTimeMs() + "ms");
        System.out.println("Full response: " + response.toString());
    }
    
    /**
     * Example 2: Checkbox/Radio button validation (with isSelected)
     */
    @Test
    public void testCheckboxValidation() throws ElementStateValidationException {
        By rememberMeCheckbox = By.id("remember-me");
        
        // Validate with selection check enabled
        ElementStateResponse response = ElementStateValidator.validateElementState(
            rememberMeCheckbox, 10, true);
        
        if (response.isSuccess()) {
            System.out.println("Checkbox displayed: " + response.isDisplayed());
            System.out.println("Checkbox enabled: " + response.isEnabled());
            System.out.println("Checkbox selected: " + response.isSelected());
        } else {
            Assert.fail("Checkbox validation failed: " + response.getMessage());
        }
    }
    
    /**
     * Example 3: Quick ready check for non-selectable elements
     */
    @Test
    public void testQuickElementReadyCheck() {
        By usernameField = By.name("username");
        
        boolean isReady = ElementStateValidator.isElementReady(usernameField, 8);
        
        Assert.assertTrue(isReady, "Username field should be ready for interaction");
        
        if (isReady) {
            // Proceed with test actions
            driver.findElement(usernameField).sendKeys("testuser");
        }
    }
    
    /**
     * Example 4: Handling validation exceptions
     */
    @Test
    public void testElementValidationWithExceptionHandling() {
        By nonExistentElement = By.id("non-existent-element");
        
        try {
            ElementStateResponse response = ElementStateValidator.validateElementState(
                nonExistentElement, 5, false);
            
            if (!response.isSuccess()) {
                System.out.println("Expected failure: " + response.getMessage());
                System.out.println("Execution time: " + response.getExecutionTimeMs() + "ms");
                
                // Handle the failure gracefully
                // Perhaps skip this test step or use alternative locator
                handleElementNotFound();
            }
            
        } catch (ElementStateValidationException e) {
            System.err.println("Critical validation failure: " + e.getMessage());
            // Log for debugging, take screenshot, etc.
            throw new RuntimeException("Test cannot continue", e);
        }
    }
    
    /**
     * Example 5: Page Object Model integration
     */
    public static class LoginPage {
        private static final By USERNAME_FIELD = By.name("username");
        private static final By PASSWORD_FIELD = By.name("password");
        private static final By LOGIN_BUTTON = By.id("login-btn");
        private static final By REMEMBER_CHECKBOX = By.id("remember-me");
        
        public boolean isPageReady() {
            try {
                // Validate all critical elements are ready
                ElementStateResponse usernameState = ElementStateValidator.validateElementState(USERNAME_FIELD, 10, false);
                ElementStateResponse passwordState = ElementStateValidator.validateElementState(PASSWORD_FIELD, 5, false);
                ElementStateResponse buttonState = ElementStateValidator.validateElementState(LOGIN_BUTTON, 5, false);
                
                return usernameState.isSuccess() && usernameState.isDisplayed() && usernameState.isEnabled() &&
                       passwordState.isSuccess() && passwordState.isDisplayed() && passwordState.isEnabled() &&
                       buttonState.isSuccess() && buttonState.isDisplayed() && buttonState.isEnabled();
                       
            } catch (ElementStateValidationException e) {
                System.err.println("Page readiness check failed: " + e.getMessage());
                return false;
            }
        }
        
        public void login(String username, String password, boolean rememberMe) throws ElementStateValidationException {
            // Validate page is ready first
            if (!isPageReady()) {
                throw new RuntimeException("Login page is not ready for interaction");
            }
            
            // Check if remember me checkbox should be handled
            if (rememberMe) {
                ElementStateResponse checkboxState = ElementStateValidator.validateElementState(REMEMBER_CHECKBOX);
                if (checkboxState.isSuccess() && checkboxState.isDisplayed() && checkboxState.isEnabled()) {
                    if (!checkboxState.isSelected()) {
                        // Click checkbox to select it
                        WebDriver driver = ElementStateValidator.getDriver();
                        driver.findElement(REMEMBER_CHECKBOX).click();
                    }
                }
            }
            
            // Proceed with login actions...
        }
    }
    
    /**
     * Example 6: Data-driven testing with element validation
     */
    @Test(dataProvider = "loginData")
    public void testDataDrivenLogin(String username, String password, boolean shouldSucceed) {
        try {
            LoginPage loginPage = new LoginPage();
            
            if (loginPage.isPageReady()) {
                loginPage.login(username, password, true);
                
                // Validate post-login state
                if (shouldSucceed) {
                    By dashboardElement = By.id("dashboard");
                    ElementStateResponse dashboardState = ElementStateValidator.validateElementState(dashboardElement);
                    Assert.assertTrue(dashboardState.isSuccess() && dashboardState.isDisplayed(), 
                                    "Dashboard should be visible after successful login");
                } else {
                    By errorMessage = By.cssSelector(".error-message");
                    ElementStateResponse errorState = ElementStateValidator.validateElementState(errorMessage, 5, false);
                    Assert.assertTrue(errorState.isSuccess() && errorState.isDisplayed(), 
                                    "Error message should be visible after failed login");
                }
            }
        } catch (ElementStateValidationException e) {
            Assert.fail("Element validation failed during login test: " + e.getMessage());
        }
    }
    
    @DataProvider
    public Object[][] loginData() {
        return new Object[][] {
            {"validuser", "validpass", true},
            {"invaliduser", "invalidpass", false},
            {"", "", false}
        };
    }
    
    private void handleElementNotFound() {
        // Custom logic for handling missing elements
        // Could involve using alternative locators, skipping test steps, etc.
        System.out.println("Implementing fallback logic for missing element...");
    }
}