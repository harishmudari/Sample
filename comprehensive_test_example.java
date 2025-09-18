package com.enterprise.tests;

import org.testng.annotations.*;
import org.testng.Assert;
import org.openqa.selenium.WebDriver;
import org.openqa.selenium.chrome.ChromeDriver;
import org.openqa.selenium.firefox.FirefoxDriver;
import org.openqa.selenium.edge.EdgeDriver;
import org.openqa.selenium.chrome.ChromeOptions;
import org.openqa.selenium.By;
import io.github.bonigarcia.wdm.WebDriverManager;
import com.enterprise.selenium.keywords.ElementStateValidator;
import com.enterprise.selenium.keywords.ElementStateValidator.ElementStateResponse;
import com.enterprise.selenium.keywords.ElementStateValidator.ElementStateValidationException;
import com.enterprise.selenium.keywords.fluent.FluentElementValidator;
import com.enterprise.selenium.keywords.builder.ElementStateBuilder;
import com.enterprise.selenium.assertions.ElementStateAssertions;
import com.enterprise.selenium.utils.RetryUtil;
import com.enterprise.selenium.pages.base.BasePage;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * Comprehensive test class demonstrating enterprise-level element state validation
 * Features demonstrated:
 * - Cross-browser testing
 * - Data-driven testing
 * - Parallel execution support
 * - Retry mechanisms
 * - Custom assertions
 * - Page Object integration
 * - Performance monitoring
 * - Exception handling
 */
public class ComprehensiveElementStateTest {
    
    private static final Logger log = LoggerFactory.getLogger(ComprehensiveElementStateTest.class);
    
    private WebDriver driver;
    private String baseUrl;
    private String browserName;
    
    // Test data
    private static final String TEST_PAGE_URL = "https://the-internet.herokuapp.com/";
    
    // Page locators
    private static final By LOGIN_LINK = By.linkText("Form Authentication");
    private static final By USERNAME_FIELD = By.id("username");
    private static final By PASSWORD_FIELD = By.id("password");
    private static final By LOGIN_BUTTON = By.cssSelector("button[type='submit']");
    private static final By FLASH_MESSAGE = By.id("flash");
    private static final By LOGOUT_BUTTON = By.linkText("Logout");
    
    private static final By CHECKBOXES_LINK = By.linkText("Checkboxes");
    private static final By CHECKBOX_1 = By.cssSelector("input[type='checkbox']:first-of-type");
    private static final By CHECKBOX_2 = By.cssSelector("input[type='checkbox']:last-of-type");
    
    @Parameters({"browser", "baseUrl"})
    @BeforeMethod
    public void setUp(@Optional("chrome") String browser, @Optional(TEST_PAGE_URL) String url) {
        this.browserName = browser;
        this.baseUrl = url;
        
        log.info("Setting up test with browser: {} and URL: {}", browser, url);
        
        // Initialize WebDriver based on browser parameter
        driver = initializeDriver(browser);
        ElementStateValidator.setDriver(driver);
        
        // Navigate to base URL
        driver.get(baseUrl);
        log.info("Navigated to: {}", baseUrl);
    }
    
    @AfterMethod
    public void tearDown() {
        try {
            if (driver != null) {
                log.info("Closing browser: {}", browserName);
                driver.quit();
            }
        } finally {
            ElementStateValidator.cleanup();
        }
    }
    
    /**
     * Initialize WebDriver based on browser type
     */
    private WebDriver initializeDriver(String browserType) {
        WebDriver webDriver;
        
        switch (browserType.toLowerCase()) {
            case "chrome":
                WebDriverManager.chromedriver().setup();
                ChromeOptions chromeOptions = new ChromeOptions();
                chromeOptions.addArguments("--disable-extensions");
                chromeOptions.addArguments("--no-sandbox");
                chromeOptions.addArguments("--disable-dev-shm-usage");
                // Uncomment for headless mode
                // chromeOptions.addArguments("--headless");
                webDriver = new ChromeDriver(chromeOptions);
                break;
                
            case "firefox":
                WebDriverManager.firefoxdriver().setup();
                webDriver = new FirefoxDriver();
                break;
                
            case "edge":
                WebDriverManager.edgedriver().setup();
                webDriver = new EdgeDriver();
                break;
                
            default:
                throw new IllegalArgumentException("Browser not supported: " + browserType);
        }
        
        webDriver.manage().window().maximize();
        return webDriver;
    }
    
    /**
     * Test 1: Basic element state validation with all three states
     */
    @Test(priority = 1, description = "Validate basic element states: displayed, enabled, selected")
    public void testBasicElementStateValidation() throws ElementStateValidationException {
        log.info("Starting basic element state validation test");
        
        // Navigate to checkboxes page
        driver.findElement(CHECKBOXES_LINK).click();
        
        // Test checkbox 1 (initially unchecked)
        ElementStateResponse checkbox1Response = ElementStateValidator.validateElementState(CHECKBOX_1);
        
        Assert.assertTrue(checkbox1Response.isSuccess(), "Checkbox 1 validation should succeed");
        Assert.assertTrue(checkbox1Response.isDisplayed(), "Checkbox 1 should be displayed");
        Assert.assertTrue(checkbox1Response.isEnabled(), "Checkbox 1 should be enabled");
        Assert.assertFalse(checkbox1Response.isSelected(), "Checkbox 1 should not be initially selected");
        
        log.info("Checkbox 1 validation completed in {}ms", checkbox1Response.getExecutionTimeMs());
        
        // Click checkbox 1 to select it
        driver.findElement(CHECKBOX_1).click();
        
        // Validate selected state
        ElementStateResponse selectedResponse = ElementStateValidator.validateElementState(CHECKBOX_1, 5, true);
        Assert.assertTrue(selectedResponse.isSelected(), "Checkbox 1 should be selected after click");
        
        log.info("Basic element state validation test completed successfully");
    }
    
    /**
     * Test 2: Fluent API style validation
     */
    @Test(priority = 2, description = "Demonstrate fluent API style element validation")
    public void testFluentApiValidation() throws ElementStateValidationException {
        log.info("Starting fluent API validation test");
        
        // Navigate to login page
        driver.findElement(LOGIN_LINK).click();
        
        // Use fluent API to validate form elements
        FluentElementValidator.element(USERNAME_FIELD)
            .withTimeout(10)
            .skipSelection()
            .shouldBeDisplayed()
            .shouldBeEnabled();
            
        FluentElementValidator.element(PASSWORD_FIELD)
            .withTimeout(5)
            .skipSelection()
            .shouldBeDisplayed()
            .shouldBeEnabled();
            
        FluentElementValidator.element(LOGIN_BUTTON)
            .withTimeout(5)
            .skipSelection()
            .shouldBeDisplayed()
            .shouldBeEnabled();
        
        log.info("Fluent API validation test completed successfully");
    }
    
    /**
     * Test 3: Builder pattern with custom conditions
     */
    @Test(priority = 3, description = "Demonstrate builder pattern with custom validation conditions")
    public void testBuilderPatternValidation() {
        log.info("Starting builder pattern validation test");
        
        driver.findElement(LOGIN_LINK).click();
        
        // Use builder pattern for complex validations
        boolean isUsernameReady = ElementStateBuilder.forElement(USERNAME_FIELD)
            .mustBeDisplayed()
            .mustBeEnabled()
            .mustHaveAttribute("type", "text")
            .mustHaveAttribute("name", "username")
            .withTimeout(10)
            .verify();
            
        Assert.assertTrue(isUsernameReady, "Username field should meet all builder conditions");
        
        boolean isLoginButtonReady = ElementStateBuilder.forElement(LOGIN_BUTTON)
            .mustBeDisplayed()
            .mustBeEnabled()
            .mustHaveAttribute("type", "submit")
            .mustContainText("Login")
            .withTimeout(5)
            .verify();
            
        Assert.assertTrue(isLoginButtonReady, "Login button should meet all builder conditions");
        
        log.info("Builder pattern validation test completed successfully");
    }
    
    /**
     * Test 4: Data-driven login test with element validation
     */
    @Test(priority = 4, dataProvider = "loginData", 
          description = "Data-driven login test with comprehensive element validation")
    public void testDataDrivenLoginWithValidation(String username, String password, boolean shouldSucceed) {
        log.info("Starting data-driven login test with credentials: {}/{}", username, "****");
        
        driver.findElement(LOGIN_LINK).click();
        
        // Validate form is ready before interaction
        ElementStateAssertions.assertElementReady(USERNAME_FIELD, "Username field should be ready");
        ElementStateAssertions.assertElementReady(PASSWORD_FIELD, "Password field should be ready");
        ElementStateAssertions.assertElementReady(LOGIN_BUTTON, "Login button should be ready");
        
        // Perform login
        driver.findElement(USERNAME_FIELD).sendKeys(username);
        driver.findElement(PASSWORD_FIELD).sendKeys(password);
        driver.findElement(LOGIN_BUTTON).click();
        
        if (shouldSucceed) {
            // Validate successful login
            ElementStateAssertions.assertElementDisplayed(LOGOUT_BUTTON, "Logout button should be visible after successful login");
            
            // Validate flash message contains success text
            try {
                ElementStateResponse flashResponse = ElementStateValidator.validateElementState(FLASH_MESSAGE, 5, false);
                Assert.assertTrue(flashResponse.isSuccess(), "Flash message should be present");
                String flashText = driver.findElement(FLASH_MESSAGE).getText();
                Assert.assertTrue(flashText.contains("You logged into a secure area!"), 
                    "Flash message should indicate successful login");
            } catch (ElementStateValidationException e) {
                Assert.fail("Failed to validate flash message: " + e.getMessage());
            }
            
        } else {
            // Validate failed login
            try {
                ElementStateResponse flashResponse = ElementStateValidator.validateElementState(FLASH_MESSAGE, 5, false);
                Assert.assertTrue(flashResponse.isSuccess(), "Error flash message should be present");
                String flashText = driver.findElement(FLASH_MESSAGE).getText();
                Assert.assertTrue(flashText.contains("Your username is invalid!") || 
                                flashText.contains("Your password is invalid!"), 
                    "Flash message should indicate login failure");
            } catch (ElementStateValidationException e) {
                Assert.fail("Failed to validate error flash message: " + e.getMessage());
            }
        }
        
        log.info("Data-driven login test completed for user: {}", username);
    }
    
    @DataProvider(name = "loginData")
    public Object[][] getLoginData() {
        return new Object[][] {
            {"tomsmith", "SuperSecretPassword!", true},   // Valid credentials
            {"invaliduser", "invalidpass", false},         // Invalid credentials
            {"", "", false},                              // Empty credentials
            {"tomsmith", "wrongpassword", false}          // Valid user, wrong password
        };
    }
    
    /**
     * Test 5: Performance monitoring and timeout testing
     */
    @Test(priority = 5, description = "Monitor performance and test timeout scenarios")
    public void testPerformanceAndTimeouts() {
        log.info("Starting performance and timeout testing");
        
        driver.findElement(LOGIN_LINK).click();
        
        // Test with different timeout values
        long startTime = System.currentTimeMillis();
        
        try {
            // Test quick validation (should pass)
            ElementStateResponse quickResponse = ElementStateValidator.validateElementState(USERNAME_FIELD, 2, false);
            long quickTime = quickResponse.getExecutionTimeMs();
            Assert.assertTrue(quickResponse.isSuccess(), "Quick validation should succeed");
            Assert.assertTrue(quickTime < 3000, "Quick validation should complete within 3 seconds");
            
            // Test with longer timeout
            ElementStateResponse slowResponse = ElementStateValidator.validateElementState(PASSWORD_FIELD, 10, false);
            long slowTime = slowResponse.getExecutionTimeMs();
            Assert.assertTrue(slowResponse.isSuccess(), "Slow validation should succeed");
            
            log.info("Performance test results - Quick: {}ms, Slow: {}ms", quickTime, slowTime);
            
        } catch (ElementStateValidationException e) {
            Assert.fail("Performance test failed: " + e.getMessage());
        }
        
        // Test timeout scenario with non-existent element
        try {
            By nonExistentElement = By.id("this-element-does-not-exist");
            ElementStateResponse timeoutResponse = ElementStateValidator.validateElementState(nonExistentElement, 2, false);
            
            Assert.assertFalse(timeoutResponse.isSuccess(), "Non-existent element validation should fail");
            Assert.assertTrue(timeoutResponse.getExecutionTimeMs() >= 2000,
                "Timeout validation should take at least the specified timeout duration");
            
            log.info("Timeout test completed in {}ms with message: {}", 
                    timeoutResponse.getExecutionTimeMs(), timeoutResponse.getMessage());
                    
        } catch (ElementStateValidationException e) {
            log.info("Expected timeout exception occurred: {}", e.getMessage());
        }
        
        log.info("Performance and timeout testing completed");
    }
    
    /**
     * Test 6: Retry mechanism with intermittent failures
     */
    @Test(priority = 6, description = "Test retry mechanism with simulated failures")
    public void testRetryMechanism() {
        log.info("Starting retry mechanism test");
        
        driver.findElement(LOGIN_LINK).click();
        
        // Use retry utility with element validation
        boolean result = RetryUtil.retryUntilTrue(() -> {
            try {
                ElementStateResponse response = ElementStateValidator.validateElementState(USERNAME_FIELD, 3, false);
                return response.isSuccess() && response.isDisplayed() && response.isEnabled();
            } catch (ElementStateValidationException e) {
                log.warn("Element validation failed in retry: {}", e.getMessage());
                return false;
            }
        }, 3, 1000, "Username field readiness");
        
        Assert.assertTrue(result, "Retry mechanism should eventually succeed");
        
        log.info("Retry mechanism test completed successfully");
    }
    
    /**
     * Test 7: Exception handling and error scenarios
     */
    @Test(priority = 7, description = "Test comprehensive exception handling")
    public void testExceptionHandling() {
        log.info("Starting exception handling test");
        
        // Test null locator handling
        try {
            ElementStateValidator.validateElementState(null, 5, false);
            Assert.fail("Should throw exception for null locator");
        } catch (ElementStateValidationException e) {
            Assert.assertTrue(e.getMessage().contains("null"), "Exception should mention null locator");
            log.info("Correctly handled null locator exception: {}", e.getMessage());
        }
        
        // Test non-existent element
        try {
            By invalidLocator = By.id("absolutely-does-not-exist-123456");
            ElementStateResponse response = ElementStateValidator.validateElementState(invalidLocator, 2, false);
            
            Assert.assertFalse(response.isSuccess(), "Invalid element validation should fail");
            Assert.assertTrue(response.getMessage().contains("failed"), "Error message should indicate failure");
            
            log.info("Non-existent element handled correctly: {}", response.getMessage());
            
        } catch (ElementStateValidationException e) {
            log.info("Critical exception for non-existent element: {}", e.getMessage());
        }
        
        log.info("Exception handling test completed");
    }
    
    /**
     * Test 8: Cross-browser element state validation
     */
    @Test(priority = 8, description = "Cross-browser compatibility test")
    public void testCrossBrowserCompatibility() throws ElementStateValidationException {
        log.info("Starting cross-browser compatibility test for: {}", browserName);
        
        driver.findElement(CHECKBOXES_LINK).click();
        
        // Validate checkbox behavior across browsers
        ElementStateResponse checkbox1State = ElementStateValidator.validateElementState(CHECKBOX_1);
        ElementStateResponse checkbox2State = ElementStateValidator.validateElementState(CHECKBOX_2);
        
        // Log browser-specific results
        log.info("Browser: {} - Checkbox 1: displayed={}, enabled={}, selected={}", 
                browserName, checkbox1State.isDisplayed(), checkbox1State.isEnabled(), checkbox1State.isSelected());
        log.info("Browser: {} - Checkbox 2: displayed={}, enabled={}, selected={}", 
                browserName, checkbox2State.isDisplayed(), checkbox2State.isEnabled(), checkbox2State.isSelected());
        
        // Assert common behavior across browsers
        Assert.assertTrue(checkbox1State.isSuccess(), "Checkbox 1 should be accessible in " + browserName);
        Assert.assertTrue(checkbox2State.isSuccess(), "Checkbox 2 should be accessible in " + browserName);
        
        // Usually checkbox 2 is pre-selected on this demo page
        Assert.assertTrue(checkbox2State.isSelected(), "Checkbox 2 should be pre-selected");
        
        log.info("Cross-browser compatibility test completed for: {}", browserName);
    }
    
    /**
     * Test 9: Performance benchmarking
     */
    @Test(priority = 9, description = "Performance benchmarking for element validation")
    public void testPerformanceBenchmarking() throws ElementStateValidationException {
        log.info("Starting performance benchmarking test");
        
        driver.findElement(LOGIN_LINK).click();
        
        // Benchmark multiple validations
        long totalTime = 0;
        int validationCount = 5;
        
        for (int i = 0; i < validationCount; i++) {
            ElementStateResponse response = ElementStateValidator.validateElementState(USERNAME_FIELD, 10, false);
            totalTime += response.getExecutionTimeMs();
            
            Assert.assertTrue(response.isSuccess(), "Validation " + (i+1) + " should succeed");
            log.debug("Validation {} completed in {}ms", i+1, response.getExecutionTimeMs());
        }
        
        double averageTime = (double) totalTime / validationCount;
        log.info("Performance benchmark - Total: {}ms, Average: {:.2f}ms, Count: {}", 
                totalTime, averageTime, validationCount);
        
        // Assert performance criteria (adjust based on your requirements)
        Assert.assertTrue(averageTime < 1000, "Average validation time should be under 1 second");
        
        log.info("Performance benchmarking test completed");
    }
    
    /**
     * Test 10: Page Object Model integration
     */
    @Test(priority = 10, description = "Demonstrate Page Object Model integration")
    public void testPageObjectIntegration() {
        log.info("Starting Page Object Model integration test");
        
        LoginPage loginPage = new LoginPage(driver);
        
        // Navigate to login page
        driver.findElement(LOGIN_LINK).click();
        
        // Verify page readiness using Page Object
        Assert.assertTrue(loginPage.isPageReady(), "Login page should be ready");
        
        // Perform login using Page Object methods
        boolean loginResult = loginPage.login("tomsmith", "SuperSecretPassword!");
        Assert.assertTrue(loginResult, "Login should succeed with valid credentials");
        
        // Verify successful login
        ElementStateAssertions.assertElementDisplayed(LOGOUT_BUTTON, "Logout button should be visible after login");
        
        log.info("Page Object Model integration test completed successfully");
    }
    
    /**
     * Page Object implementation for demonstration
     */
    public static class LoginPage extends BasePage {
        
        public LoginPage(WebDriver driver) {
            super(driver);
        }
        
        @Override
        public boolean isPageReady() {
            return verifyPageReadiness("The Internet", USERNAME_FIELD, PASSWORD_FIELD, LOGIN_BUTTON);
        }
        
        @Override
        public String getExpectedPageTitle() {
            return "The Internet";
        }
        
        public boolean login(String username, String password) {
            try {
                // Validate form readiness
                if (!isPageReady()) {
                    log.error("Login page is not ready for interaction");
                    return false;
                }
                
                // Perform login with safe operations
                boolean inputSuccess = safeInput(USERNAME_FIELD, username) && 
                                     safeInput(PASSWORD_FIELD, password);
                
                if (!inputSuccess) {
                    log.error("Failed to input login credentials");
                    return false;
                }
                
                // Click login button safely
                if (safeClick(LOGIN_BUTTON)) {
                    driver.findElement(LOGIN_BUTTON).click();
                    
                    // Wait for login result
                    Thread.sleep(2000);
                    
                    // Check if logout button appears (successful login)
                    boolean isLoggedIn = ElementStateValidator.isElementReady(LOGOUT_BUTTON, 5);
                    
                    if (isLoggedIn) {
                        log.info("Login successful for user: {}", username);
                        return true;
                    } else {
                        log.warn("Login failed for user: {}", username);
                        return false;
                    }
                } else {
                    log.error("Login button is not clickable");
                    return false;
                }
                
            } catch (Exception e) {
                log.error("Login process failed: {}", e.getMessage());
                return false;
            }
        }
    }
}

/**
 * Test configuration and utility class
 */
package com.enterprise.selenium.config;

import java.io.IOException;
import java.io.InputStream;
import java.util.Properties;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class TestConfig {
    private static final Logger log = LoggerFactory.getLogger(TestConfig.class);
    private static Properties properties;
    
    static {
        loadProperties();
    }
    
    private static void loadProperties() {
        properties = new Properties();
        try (InputStream input = TestConfig.class.getClassLoader()
                .getResourceAsStream("selenium-config.properties")) {
            if (input != null) {
                properties.load(input);
                log.info("Test configuration loaded successfully");
            } else {
                log.warn("selenium-config.properties not found, using defaults");
                setDefaultProperties();
            }
        } catch (IOException e) {
            log.error("Error loading test configuration: {}", e.getMessage());
            setDefaultProperties();
        }
    }
    
    private static void setDefaultProperties() {
        properties.setProperty("element.timeout", "10");
        properties.setProperty("retry.count", "3");
        properties.setProperty("retry.interval", "1000");
        properties.setProperty("screenshot.on.failure", "true");
        properties.setProperty("log.level", "INFO");
    }
    
    public static String getProperty(String key) {
        return properties.getProperty(key);
    }
    
    public static String getProperty(String key, String defaultValue) {
        return properties.getProperty(key, defaultValue);
    }
    
    public static int getIntProperty(String key, int defaultValue) {
        try {
            return Integer.parseInt(properties.getProperty(key, String.valueOf(defaultValue)));
        } catch (NumberFormatException e) {
            log.warn("Invalid integer property for key: {}, using default: {}", key, defaultValue);
            return defaultValue;
        }
    }
    
    public static boolean getBooleanProperty(String key, boolean defaultValue) {
        return Boolean.parseBoolean(properties.getProperty(key, String.valueOf(defaultValue)));
    }
}

/**
 * Test listener for ExtentReports integration
 */
package com.enterprise.listeners;

import org.testng.ITestListener;
import org.testng.ITestResult;
import com.aventstack.extentreports.ExtentReports;
import com.aventstack.extentreports.ExtentTest;
import com.aventstack.extentreports.Status;
import com.aventstack.extentreports.reporter.ExtentSparkReporter;
import com.aventstack.extentreports.reporter.configuration.Theme;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class ExtentReportListener implements ITestListener {
    private static final Logger log = LoggerFactory.getLogger(ExtentReportListener.class);
    private static ExtentReports extent;
    private static ThreadLocal<ExtentTest> extentTest = new ThreadLocal<>();
    
    @Override
    public void onStart(ITestContext context) {
        ExtentSparkReporter sparkReporter = new ExtentSparkReporter("target/extent-reports/ElementStateValidation.html");
        sparkReporter.config().setDocumentTitle("Element State Validation Report");
        sparkReporter.config().setReportName("Enterprise Selenium Test Results");
        sparkReporter.config().setTheme(Theme.STANDARD);
        
        extent = new ExtentReports();
        extent.attachReporter(sparkReporter);
        extent.setSystemInfo("OS", System.getProperty("os.name"));
        extent.setSystemInfo("Java Version", System.getProperty("java.version"));
        extent.setSystemInfo("User", System.getProperty("user.name"));
        
        log.info("ExtentReports initialized");
    }
    
    @Override
    public void onTestStart(ITestResult result) {
        ExtentTest test = extent.createTest(result.getMethod().getMethodName(), 
                                          result.getMethod().getDescription());
        extentTest.set(test);
        log.info("Test started: {}", result.getMethod().getMethodName());
    }
    
    @Override
    public void onTestSuccess(ITestResult result) {
        extentTest.get().log(Status.PASS, "Test passed successfully");
        log.info("Test passed: {}", result.getMethod().getMethodName());
    }
    
    @Override
    public void onTestFailure(ITestResult result) {
        extentTest.get().log(Status.FAIL, "Test failed: " + result.getThrowable().getMessage());
        
        // Attach screenshot if available
        try {
            String screenshotPath = captureScreenshot(result.getMethod().getMethodName());
            if (screenshotPath != null) {
                extentTest.get().addScreenCaptureFromPath(screenshotPath);
            }
        } catch (Exception e) {
            log.error("Failed to attach screenshot: {}", e.getMessage());
        }
        
        log.error("Test failed: {}", result.getMethod().getMethodName());
    }
    
    @Override
    public void onTestSkipped(ITestResult result) {
        extentTest.get().log(Status.SKIP, "Test skipped: " + result.getThrowable().getMessage());
        log.warn("Test skipped: {}", result.getMethod().getMethodName());
    }
    
    @Override
    public void onFinish(ITestContext context) {
        if (extent != null) {
            extent.flush();
            log.info("ExtentReports report generated");
        }
    }
    
    public static ExtentTest getTest() {
        return extentTest.get();
    }
    
    private String captureScreenshot(String testName) {
        // Implementation would depend on your screenshot utility
        // This is a placeholder for the actual screenshot capture logic
        return null;
    }
}

/**
 * Advanced Test Suite with parallel execution support
 */
package com.enterprise.tests.parallel;

import org.testng.annotations.*;
import org.testng.Assert;
import org.openqa.selenium.WebDriver;
import org.openqa.selenium.By;
import io.github.bonigarcia.wdm.WebDriverManager;
import com.enterprise.selenium.keywords.ElementStateValidator;
import com.enterprise.selenium.keywords.ElementStateValidator.ElementStateResponse;
import java.util.concurrent.TimeUnit;

public class ParallelElementStateTest {
    
    private static final Logger log = LoggerFactory.getLogger(ParallelElementStateTest.class);
    
    @Factory(dataProvider = "browserProvider")
    public ParallelElementStateTest(String browserName) {
        this.browserName = browserName;
    }
    
    private String browserName;
    private WebDriver driver;
    
    @DataProvider(name = "browserProvider", parallel = true)
    public static Object[][] browserProvider() {
        return new Object[][] {
            {"chrome"},
            {"firefox"},
            {"edge"}
        };
    }
    
    @BeforeMethod
    public void setUpParallel() {
        log.info("Setting up parallel test for browser: {}", browserName);
        
        switch (browserName.toLowerCase()) {
            case "chrome":
                WebDriverManager.chromedriver().setup();
                driver = new ChromeDriver();
                break;
            case "firefox":
                WebDriverManager.firefoxdriver().setup();
                driver = new FirefoxDriver();
                break;
            case "edge":
                WebDriverManager.edgedriver().setup();
                driver = new EdgeDriver();
                break;
        }
        
        ElementStateValidator.setDriver(driver);
        driver.manage().timeouts().implicitlyWait(5, TimeUnit.SECONDS);
        driver.manage().window().maximize();
        
        log.info("Browser {} initialized for thread: {}", browserName, Thread.currentThread().getName());
    }
    
    @AfterMethod
    public void tearDownParallel() {
        try {
            if (driver != null) {
                driver.quit();
                log.info("Browser {} closed for thread: {}", browserName, Thread.currentThread().getName());
            }
        } finally {
            ElementStateValidator.cleanup();
        }
    }
    
    @Test(description = "Parallel cross-browser element validation test")
    public void testParallelElementValidation() throws Exception {
        log.info("Running parallel test on browser: {}", browserName);
        
        driver.get("https://the-internet.herokuapp.com/");
        driver.findElement(By.linkText("Checkboxes")).click();
        
        // Validate elements in parallel across browsers
        By checkbox1 = By.cssSelector("input[type='checkbox']:first-of-type");
        By checkbox2 = By.cssSelector("input[type='checkbox']:last-of-type");
        
        ElementStateResponse response1 = ElementStateValidator.validateElementState(checkbox1);
        ElementStateResponse response2 = ElementStateValidator.validateElementState(checkbox2);
        
        // Assert results
        Assert.assertTrue(response1.isSuccess(), "Checkbox 1 validation should succeed in " + browserName);
        Assert.assertTrue(response2.isSuccess(), "Checkbox 2 validation should succeed in " + browserName);
        
        // Log browser-specific performance
        log.info("Browser: {} - Validation times: checkbox1={}ms, checkbox2={}ms", 
                browserName, response1.getExecutionTimeMs(), response2.getExecutionTimeMs());
        
        // Test interaction
        if (!response1.isSelected()) {
            driver.findElement(checkbox1).click();
            
            // Re-validate after interaction
            ElementStateResponse postClickResponse = ElementStateValidator.validateElementState(checkbox1, 5, true);
            Assert.assertTrue(postClickResponse.isSelected(), "Checkbox 1 should be selected after click in " + browserName);
        }
        
        log.info("Parallel test completed successfully for browser: {}", browserName);
    }
}

/**
 * Utility class for common element operations with state validation
 */
package com.enterprise.selenium.utils;

import org.openqa.selenium.By;
import org.openqa.selenium.WebDriver;
import org.openqa.selenium.WebElement;
import org.openqa.selenium.JavascriptExecutor;
import org.openqa.selenium.support.ui.Select;
import com.enterprise.selenium.keywords.ElementStateValidator;
import com.enterprise.selenium.keywords.ElementStateValidator.ElementStateResponse;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class SafeWebElementOperations {
    private static final Logger log = LoggerFactory.getLogger(SafeWebElementOperations.class);
    
    /**
     * Safe click with JavaScript fallback
     */
    public static boolean safeClickWithFallback(By locator, int timeoutSeconds) {
        try {
            // First validate element state
            ElementStateResponse response = ElementStateValidator.validateElementState(locator, timeoutSeconds, false);
            
            if (!response.isSuccess() || !response.isDisplayed() || !response.isEnabled()) {
                log.warn("Element not ready for click: {}", response.getMessage());
                return false;
            }
            
            WebDriver driver = ElementStateValidator.getDriver();
            WebElement element = driver.findElement(locator);
            
            try {
                // Try normal click first
                element.click();
                log.info("Successfully clicked element: {}", locator);
                return true;
                
            } catch (Exception normalClickException) {
                log.warn("Normal click failed, trying JavaScript click: {}", normalClickException.getMessage());
                
                // Fallback to JavaScript click
                try {
                    JavascriptExecutor js = (JavascriptExecutor) driver;
                    js.executeScript("arguments[0].click();", element);
                    log.info("Successfully clicked element via JavaScript: {}", locator);
                    return true;
                    
                } catch (Exception jsClickException) {
                    log.error("Both normal and JavaScript click failed for element: {} - {}", 
                            locator, jsClickException.getMessage());
                    return false;
                }
            }
            
        } catch (Exception e) {
            log.error("Safe click operation failed for element: {} - {}", locator, e.getMessage());
            return false;
        }
    }
    
    /**
     * Safe text input with validation and clearing
     */
    public static boolean safeTextInput(By locator, String text, boolean clearFirst, int timeoutSeconds) {
        if (text == null) {
            log.warn("Cannot input null text to element: {}", locator);
            return false;
        }
        
        try {
            ElementStateResponse response = ElementStateValidator.validateElementState(locator, timeoutSeconds, false);
            
            if (!response.isSuccess() || !response.isDisplayed() || !response.isEnabled()) {
                log.warn("Element not ready for text input: {}", response.getMessage());
                return false;
            }
            
            WebDriver driver = ElementStateValidator.getDriver();
            WebElement element = driver.findElement(locator);
            
            if (clearFirst) {
                element.clear();
            }
            
            element.sendKeys(text);
            log.info("Successfully input text to element: {} (length: {})", locator, text.length());
            return true;
            
        } catch (Exception e) {
            log.error("Safe text input failed for element: {} - {}", locator, e.getMessage());
            return false;
        }
    }
    
    /**
     * Safe dropdown selection with validation
     */
    public static boolean safeSelectDropdown(By locator, String optionText, int timeoutSeconds) {
        try {
            ElementStateResponse response = ElementStateValidator.validateElementState(locator, timeoutSeconds, false);
            
            if (!response.isSuccess() || !response.isDisplayed() || !response.isEnabled()) {
                log.warn("Dropdown not ready for selection: {}", response.getMessage());
                return false;
            }
            
            WebDriver driver = ElementStateValidator.getDriver();
            WebElement element = driver.findElement(locator);
            
            Select select = new Select(element);
            select.selectByVisibleText(optionText);
            
            // Validate selection was successful
            String selectedText = select.getFirstSelectedOption().getText();
            if (selectedText.equals(optionText)) {
                log.info("Successfully selected option '{}' in dropdown: {}", optionText, locator);
                return true;
            } else {
                log.error("Selection failed. Expected: '{}', Actual: '{}'", optionText, selectedText);
                return false;
            }
            
        } catch (Exception e) {
            log.error("Safe dropdown selection failed for element: {} - {}", locator, e.getMessage());
            return false;
        }
    }
}