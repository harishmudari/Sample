package com.enterprise.selenium.tests;

import com.enterprise.selenium.keywords.EnterpriseSeleniumKeywords;
import com.enterprise.selenium.utils.AdvancedSeleniumKeywords;
import com.enterprise.selenium.utils.LocatorRepository;
import org.testng.annotations.*;
import org.testng.Assert;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * Comprehensive Enterprise Test Suite demonstrating all keyword functionalities
 * This test covers:
 * - Multi-browser support with all security configurations
 * - SSL certificate handling
 * - Notification management  
 * - Geolocation handling
 * - File uploads/downloads
 * - Advanced interactions
 * - Error handling and recovery
 * - Parallel execution
 * - Data-driven testing
 */
public class EnterpriseTestSuite {
    
    private static final Logger log = LoggerFactory.getLogger(EnterpriseTestSuite.class);
    private static final String TEST_URL = "https://demo-enterprise-app.com";
    
    /**
     * Test Class Setup - runs once before all test methods
     */
    @BeforeClass
    public void classSetup() {
        log.info("=== Starting Enterprise Test Suite ===");
        log.info("Test Environment: {}", System.getProperty("test.env", "qa"));
        log.info("Browser: {}", System.getProperty("browser", "chrome"));
    }
    
    /**
     * Method Setup - runs before each test method
     * Configures browser based on test requirements
     */
    @BeforeMethod
    @Parameters({"browser", "headless", "environment"})
    public void setUp(@Optional("chrome") String browser, 
                     @Optional("false") boolean headless,
                     @Optional("qa") String environment) {
        
        log.info("Setting up test with browser: {}, headless: {}, environment: {}", 
                browser, headless, environment);
        
        boolean driverInitialized = false;
        
        switch (browser.toLowerCase()) {
            case "firefox":
                driverInitialized = EnterpriseSeleniumKeywords.initializeFirefoxDriver(
                    headless, // headless mode
                    false,    // disable notifications for clean testing
                    false,    // disable geolocation for privacy
                    true      // ignore SSL certificates for test environments
                );
                break;
                
            case "chrome":
            default:
                driverInitialized = EnterpriseSeleniumKeywords.initializeChromeDriver(
                    headless, // headless mode based on parameter
                    false,    // disable notifications
                    false,    // disable geolocation  
                    true      // ignore certificate errors for test environments
                );
                break;
        }
        
        Assert.assertTrue(driverInitialized, 
            "Failed to initialize " + browser + " driver for environment: " + environment);
        
        log.info("Browser initialized successfully: {}", browser);
    }
    
    /**
     * Test 1: Basic Navigation and SSL Handling
     * Verifies that the browser can navigate to HTTPS sites with certificate issues
     */
    @Test(priority = 1)
    public void testBasicNavigationWithSSL() {
        log.info("=== Test: Basic Navigation with SSL Handling ===");
        
        // Navigate to test URL (may have SSL certificate issues)
        boolean navigated = EnterpriseSeleniumKeywords.navigateToUrl(TEST_URL);
        Assert.assertTrue(navigated, "Failed to navigate to test URL with SSL handling");
        
        // Verify page loaded by checking title
        String currentTitle = AdvancedSeleniumKeywords.getCurrentTitle();
        Assert.assertNotNull(currentTitle, "Page title should not be null");
        Assert.assertFalse(currentTitle.isEmpty(), "Page title should not be empty");
        
        log.info("Successfully navigated to: {} with title: {}", TEST_URL, currentTitle);
    }
    
    /**
     * Test 2: Login Flow with Smart Interactions
     * Tests form interactions, smart clicking, and text validation
     */
    @Test(priority = 2, dependsOnMethods = "testBasicNavigationWithSSL")
    public void testLoginFlowWithSmartInteractions() {
        log.info("=== Test: Login Flow with Smart Interactions ===");
        
        // Navigate to login page
        boolean loginPageLoaded = EnterpriseSeleniumKeywords.navigateToUrl(TEST_URL + "/login");
        Assert.assertTrue(loginPageLoaded, "Failed to load login page");
        
        // Wait for login form to be visible
        boolean loginFormVisible = EnterpriseSeleniumKeywords.waitForElementVisible(
            LocatorRepository.LOGIN_USERNAME, 10);
        Assert.assertTrue(loginFormVisible, "Login form not visible");
        
        // Enter credentials using enterprise keywords
        boolean usernameEntered = EnterpriseSeleniumKeywords.enterText(
            LocatorRepository.LOGIN_USERNAME, "enterprise.test@company.com");
        Assert.assertTrue(usernameEntered, "Failed to enter username");
        
        boolean passwordEntered = EnterpriseSeleniumKeywords.enterText(
            LocatorRepository.LOGIN_PASSWORD, "EnterpriseTest123!");
        Assert.assertTrue(passwordEntered, "Failed to enter password");
        
        // Use smart click to handle any clicking scenarios (normal, JS, Actions)
        boolean loginClicked = EnterpriseSeleniumKeywords.clickElementSmart(
            LocatorRepository.LOGIN_BUTTON);
        Assert.assertTrue(loginClicked, "Failed to click login button");
        
        // Handle any browser notifications that might appear
        String notificationText = AdvancedSeleniumKeywords.handleAlert(false);
        if (notificationText != null) {
            log.info("Handled notification: {}", notificationText);
        }
        
        // Wait for dashboard to load
        boolean dashboardLoaded = EnterpriseSeleniumKeywords.waitForElementVisible(
            LocatorRepository.DASHBOARD_WELCOME, 15);
        Assert.assertTrue(dashboardLoaded, "Dashboard not loaded after login");
        
        // Validate login success
        String welcomeMessage = EnterpriseSeleniumKeywords.getElementText(
            LocatorRepository.DASHBOARD_WELCOME);
        Assert.assertNotNull(welcomeMessage, "Welcome message should be present");
        Assert.assertTrue(welcomeMessage.toLowerCase().contains("welcome"), 
            "Welcome message should contain 'welcome'");
        
        log.info("Login successful. Welcome message: {}", welcomeMessage);
    }
    
    /**
     * Test 3: Advanced Dropdown and Form Interactions
     * Tests dropdown selections and complex form handling
     */
    @Test(priority = 3, dependsOnMethods = "testLoginFlowWithSmartInteractions")
    public void testAdvancedFormInteractions() {
        log.info("=== Test: Advanced Form Interactions ===");
        
        // Navigate to form page
        EnterpriseSeleniumKeywords.navigateToUrl(TEST_URL + "/forms");
        
        // Test dropdown selection by text
        boolean countrySelected = AdvancedSeleniumKeywords.selectDropdownByText(
            By.id("countryDropdown"), "United States");
        Assert.assertTrue(countrySelected, "Failed to select country from dropdown");
        
        // Test dropdown selection by value
        boolean stateSelected = AdvancedSeleniumKeywords.selectDropdownByValue(
            By.id("stateDropdown"), "CA");
        Assert.assertTrue(stateSelected, "Failed to select state by value");
        
        // Test file upload functionality
        String testFilePath = System.getProperty("user.dir") + "/src/test/resources/test-document.pdf";
        boolean fileUploaded = AdvancedSeleniumKeywords.uploadFile(
            By.id("fileUpload"), testFilePath);
        // Note: This will only pass if test file exists
        log.info("File upload attempt result: {}", fileUploaded);
        
        // Test hover interactions
        boolean hoverSuccessful = AdvancedSeleniumKeywords.hoverOverElement(
            By.id("hoverElement"));
        Assert.assertTrue(hoverSuccessful, "Failed to hover over element");
        
        // Test scroll to element
        boolean scrollSuccessful = AdvancedSeleniumKeywords.scrollToElement(
            By.id("bottomElement"));
        Assert.assertTrue(scrollSuccessful, "Failed to scroll to bottom element");
        
        log.info("Advanced form interactions completed successfully");
    }
    
    /**
     * Test 4: Window and Frame Management
     * Tests multi-window and iframe handling
     */
    @Test(priority = 4, dependsOnMethods = "testAdvancedFormInteractions")
    public void testWindowAndFrameManagement() {
        log.info("=== Test: Window and Frame Management ===");
        
        // Navigate to page with multiple windows/frames
        EnterpriseSeleniumKeywords.navigateToUrl(TEST_URL + "/windows-frames");
        
        // Open new window
        EnterpriseSeleniumKeywords.clickElementSmart(By.id("openNewWindow"));
        
        // Switch to new window by title
        boolean windowSwitched = AdvancedSeleniumKeywords.switchToWindow("New Window", false);
        Assert.assertTrue(windowSwitched, "Failed to switch to new window");
        
        // Perform actions in new window
        String newWindowTitle = AdvancedSeleniumKeywords.getCurrentTitle();
        log.info("New window title: {}", newWindowTitle);
        
        // Close current window and switch back
        AdvancedSeleniumKeywords.closeCurrentWindow();
        
        // Switch to main window
        AdvancedSeleniumKeywords.switchToWindow("Main Window", false);
        
        // Test iframe switching
        boolean frameSwitch = AdvancedSeleniumKeywords.switchToFrame(By.id("testFrame"));
        if (frameSwitch) {
            // Perform actions inside frame
            EnterpriseSeleniumKeywords.clickElementSmart(By.id("frameButton"));
            
            // Switch back to main content
            AdvancedSeleniumKeywords.switchToDefaultContent();
        }
        
        log.info("Window and frame management tests completed");
    }
    
    /**
     * Test 5: Table Data Extraction
     * Tests data extraction from tables
     */
    @Test(priority = 5, dependsOnMethods = "testWindowAndFrameManagement")
    public void testTableDataExtraction() {
        log.info("=== Test: Table Data Extraction ===");
        
        // Navigate to page with data table
        EnterpriseSeleniumKeywords.navigateToUrl(TEST_URL + "/tables");
        
        // Wait for table to load
        boolean tableVisible = EnterpriseSeleniumKeywords.waitForElementVisible(
            LocatorRepository.DATA_TABLE, 10);
        Assert.assertTrue(tableVisible, "Data table not visible");
        
        // Extract data from specific cells
        String cell1Data = AdvancedSeleniumKeywords.getTableCellData(
            LocatorRepository.DATA_TABLE, 1, 1);
        Assert.assertNotNull(cell1Data, "Cell (1,1) data should not be null");
        
        String cell2Data = AdvancedSeleniumKeywords.getTableCellData(
            LocatorRepository.DATA_TABLE, 2, 3);
        Assert.assertNotNull(cell2Data, "Cell (2,3) data should not be null");
        
        log.info("Extracted table data - Cell(1,1): {}, Cell(2,3): {}", cell1Data, cell2Data);
        
        // Validate table has expected number of rows
        // This would require additional implementation to count rows
        log.info("Table data extraction completed successfully");
    }
    
    /**
     * Test 6: JavaScript Execution and Dynamic Content
     * Tests JavaScript execution and dynamic content handling
     */
    @Test(priority = 6, dependsOnMethods = "testTableDataExtraction")
    public void testJavaScriptExecution() {
        log.info("=== Test: JavaScript Execution ===");
        
        // Execute JavaScript to get page information
        Object pageHeight = AdvancedSeleniumKeywords.executeJavaScript(
            "return document.body.scrollHeight;");
        Assert.assertNotNull(pageHeight, "Page height should not be null");
        
        log.info("Page height: {} pixels", pageHeight);
        
        // Execute JavaScript to modify page content
        Object result = AdvancedSeleniumKeywords.executeJavaScript(
            "document.getElementById('dynamicContent').innerHTML = 'Modified by JavaScript'; return true;");
        
        if (result != null && (Boolean) result) {
            // Verify the content was modified
            String modifiedContent = EnterpriseSeleniumKeywords.getElementText(By.id("dynamicContent"));
            Assert.assertEquals(modifiedContent, "Modified by JavaScript", 
                "Content should be modified by JavaScript");
        }
        
        // Test scrolling with JavaScript
        AdvancedSeleniumKeywords.executeJavaScript("window.scrollTo(0, 0);");
        
        log.info("JavaScript execution tests completed successfully");
    }
    
    /**
     * Test 7: Error Handling and Recovery
     * Tests robust error handling and recovery mechanisms
     */
    @Test(priority = 7, dependsOnMethods = "testJavaScriptExecution")
    public void testErrorHandlingAndRecovery() {
        log.info("=== Test: Error Handling and Recovery ===");
        
        // Test navigation to non-existent page (should handle gracefully)
        boolean invalidNavigation = EnterpriseSeleniumKeywords.navigateToUrl(TEST_URL + "/non-existent-page");
        // This might return false, which is expected behavior
        
        // Navigate back to valid page
        boolean validNavigation = EnterpriseSeleniumKeywords.navigateToUrl(TEST_URL + "/dashboard");
        Assert.assertTrue(validNavigation, "Should be able to navigate to valid page after error");
        
        // Test clicking non-existent element (should return false, not throw exception)
        boolean nonExistentClick = EnterpriseSeleniumKeywords.clickElementSmart(
            By.id("thisElementDoesNotExist"));
        Assert.assertFalse(nonExistentClick, "Should return false for non-existent element");
        
        // Test getting text from non-existent element
        String nonExistentText = EnterpriseSeleniumKeywords.getElementText(
            By.id("alsoDoesNotExist"));
        Assert.assertNull(nonExistentText, "Should return null for non-existent element");
        
        // Verify we can still interact with valid elements after errors
        boolean validClick = EnterpriseSeleniumKeywords.clickElementSmart(
            LocatorRepository.DASHBOARD_MENU);
        // This should work if we're on a valid dashboard page
        
        log.info("Error handling and recovery tests completed");
    }
    
    /**
     * Test 8: Performance and Timing
     * Tests performance-related functionality and timing
     */
    @Test(priority = 8, dependsOnMethods = "testErrorHandlingAndRecovery")
    public void testPerformanceAndTiming() {
        log.info("=== Test: Performance and Timing ===");
        
        long startTime = System.currentTimeMillis();
        
        // Test page load timing
        boolean pageLoaded = EnterpriseSeleniumKeywords.navigateToUrl(TEST_URL + "/heavy-page");
        long navigationTime = System.currentTimeMillis() - startTime;
        
        log.info("Page navigation took: {} milliseconds", navigationTime);
        Assert.assertTrue(navigationTime < 30000, "Page should load within 30 seconds");
        
        // Test element wait timing
        startTime = System.currentTimeMillis();
        boolean elementAppeared = EnterpriseSeleniumKeywords.waitForElementVisible(
            By.id("slowLoadingElement"), 15);
        long waitTime = System.currentTimeMillis() - startTime;
        
        log.info("Element wait took: {} milliseconds, appeared: {}", waitTime, elementAppeared);
        
        // Test element disappearance
        if (AdvancedSeleniumKeywords.isElementPresent(By.id("temporaryElement"))) {
            startTime = System.currentTimeMillis();
            boolean elementDisappeared = AdvancedSeleniumKeywords.waitForElementToDisappear(
                By.id("temporaryElement"), 10);
            long disappearTime = System.currentTimeMillis() - startTime;
            
            log.info("Element disappear wait took: {} milliseconds, disappeared: {}", 
                    disappearTime, elementDisappeared);
        }
        
        log.info("Performance and timing tests completed");
    }
    
    /**
     * Test 9: Browser Navigation and History
     * Tests browser navigation functionality
     */
    @Test(priority = 9, dependsOnMethods = "testPerformanceAndTiming")
    public void testBrowserNavigation() {
        log.info("=== Test: Browser Navigation ===");
        
        // Navigate to first page
        EnterpriseSeleniumKeywords.navigateToUrl(TEST_URL + "/page1");
        String page1Url = AdvancedSeleniumKeywords.getCurrentUrl();
        
        // Navigate to second page
        EnterpriseSeleniumKeywords.navigateToUrl(TEST_URL + "/page2");
        String page2Url = AdvancedSeleniumKeywords.getCurrentUrl();
        
        Assert.assertNotEquals(page1Url, page2Url, "URLs should be different");
        
        // Test back navigation
        boolean backSuccess = AdvancedSeleniumKeywords.navigateBack();
        Assert.assertTrue(backSuccess, "Back navigation should succeed");
        
        String backUrl = AdvancedSeleniumKeywords.getCurrentUrl();
        Assert.assertTrue(backUrl.contains("page1"), "Should be back on page1");
        
        // Test forward navigation
        boolean forwardSuccess = AdvancedSeleniumKeywords.navigateForward();
        Assert.assertTrue(forwardSuccess, "Forward navigation should succeed");
        
        String forwardUrl = AdvancedSeleniumKeywords.getCurrentUrl();
        Assert.assertTrue(forwardUrl.contains("page2"), "Should be forward on page2");
        
        // Test page refresh
        boolean refreshSuccess = AdvancedSeleniumKeywords.refreshPage();
        Assert.assertTrue(refreshSuccess, "Page refresh should succeed");
        
        // Verify we're still on the same page after refresh
        String refreshedUrl = AdvancedSeleniumKeywords.getCurrentUrl();
        Assert.assertTrue(refreshedUrl.contains("page2"), "Should still be on page2 after refresh");
        
        log.info("Browser navigation tests completed successfully");
    }
    
    /**
     * Test 10: Logout and Session Management
     * Tests proper session termination and cleanup
     */
    @Test(priority = 10, dependsOnMethods = "testBrowserNavigation")
    public void testLogoutAndSessionManagement() {
        log.info("=== Test: Logout and Session Management ===");
        
        // Navigate to dashboard to ensure we're logged in
        EnterpriseSeleniumKeywords.navigateToUrl(TEST_URL + "/dashboard");
        
        // Verify we're logged in
        boolean welcomeVisible = EnterpriseSeleniumKeywords.waitForElementVisible(
            LocatorRepository.DASHBOARD_WELCOME, 5);
        
        if (welcomeVisible) {
            // Click logout button
            boolean logoutClicked = EnterpriseSeleniumKeywords.clickElementSmart(
                LocatorRepository.LOGOUT_BUTTON);
            Assert.assertTrue(logoutClicked, "Should be able to click logout button");
            
            // Handle any confirmation dialogs
            String confirmationText = AdvancedSeleniumKeywords.handleAlert(true); // Accept logout
            if (confirmationText != null) {
                log.info("Handled logout confirmation: {}", confirmationText);
            }
            
            // Wait for redirect to login page
            boolean loginPageReached = AdvancedSeleniumKeywords.waitForTitleContains("Login", 10);
            Assert.assertTrue(loginPageReached, "Should be redirected to login page after logout");
            
            // Verify logout by checking if login form is present
            boolean loginFormPresent = AdvancedSeleniumKeywords.isElementPresent(
                LocatorRepository.LOGIN_USERNAME);
            Assert.assertTrue(loginFormPresent, "Login form should be present after logout");
            
            log.info("Logout completed successfully");
        } else {
            log.info("Not logged in, skipping logout test");
        }
    }
    
    /**
     * Method Teardown - runs after each test method
     * Ensures proper cleanup and screenshot on failure
     */
    @AfterMethod
    public void tearDown(ITestResult result) {
        if (result.getStatus() == ITestResult.FAILURE) {
            log.error("Test failed: {}", result.getName());
            String screenshotPath = EnterpriseSeleniumKeywords.takeScreenshotOnFailure(result.getName());
            if (screenshotPath != null) {
                log.info("Screenshot saved for failed test: {}", screenshotPath);
            }
        }
        
        // Clean up WebDriver resources
        boolean driverQuit = EnterpriseSeleniumKeywords.quitDriver();
        if (driverQuit) {
            log.info("WebDriver resources cleaned up successfully");
        } else {
            log.warn("Failed to clean up WebDriver resources");
        }
    }
    
    /**
     * Class Teardown - runs once after all test methods
     */
    @AfterClass
    public void classTearDown() {
        log.info("=== Enterprise Test Suite Completed ===");
    }
}

/**
 * Data-Driven Test Examples using TestNG DataProvider
 */
class DataDrivenEnterpriseTests {
    
    private static final Logger log = LoggerFactory.getLogger(DataDrivenEnterpriseTests.class);
    
    /**
     * Data Provider for login test scenarios
     * Provides different combinations of valid/invalid credentials
     */
    @DataProvider(name = "loginTestData")
    public Object[][] getLoginTestData() {
        return new Object[][]{
            // {username, password, expectedResult, description}
            {"valid.user@company.com", "ValidPass123!", true, "Valid credentials"},
            {"invalid.user@company.com", "ValidPass123!", false, "Invalid username"},
            {"valid.user@company.com", "InvalidPass", false, "Invalid password"},
            {"", "ValidPass123!", false, "Empty username"},
            {"valid.user@company.com", "", false, "Empty password"},
            {"admin@company.com", "AdminPass456!", true, "Admin credentials"}
        };
    }
    
    /**
     * Data-driven login test
     * Tests multiple login scenarios with different credential combinations
     */
    @Test(dataProvider = "loginTestData")
    public void testLoginScenarios(String username, String password, 
                                 boolean expectedSuccess, String description) {
        
        log.info("=== Testing Login Scenario: {} ===", description);
        
        // Initialize browser for each test iteration
        boolean driverInitialized = EnterpriseSeleniumKeywords.initializeChromeDriver(
            false, false, false, true);
        Assert.assertTrue(driverInitialized, "Driver initialization failed");
        
        try {
            // Navigate to login page
            EnterpriseSeleniumKeywords.navigateToUrl("https://demo-enterprise-app.com/login");
            
            // Enter credentials
            EnterpriseSeleniumKeywords.enterText(LocatorRepository.LOGIN_USERNAME, username);
            EnterpriseSeleniumKeywords.enterText(LocatorRepository.LOGIN_PASSWORD, password);
            
            // Click login
            EnterpriseSeleniumKeywords.clickElementSmart(LocatorRepository.LOGIN_BUTTON);
            
            if (expectedSuccess) {
                // Verify successful login
                boolean dashboardVisible = EnterpriseSeleniumKeywords.waitForElementVisible(
                    LocatorRepository.DASHBOARD_WELCOME, 10);
                Assert.assertTrue(dashboardVisible, "Dashboard should be visible for valid login");
                log.info("Login successful as expected for: {}", description);
            } else {
                // Verify failed login
                boolean errorVisible = EnterpriseSeleniumKeywords.waitForElementVisible(
                    LocatorRepository.LOGIN_ERROR_MESSAGE, 5);
                Assert.assertTrue(errorVisible, "Error message should be visible for invalid login");
                
                String errorMessage = EnterpriseSeleniumKeywords.getElementText(
                    LocatorRepository.LOGIN_ERROR_MESSAGE);
                Assert.assertNotNull(errorMessage, "Error message should not be null");
                log.info("Login failed as expected for: {}. Error: {}", description, errorMessage);
            }
            
        } finally {
            // Clean up after each iteration
            EnterpriseSeleniumKeywords.quitDriver();
        }
    }
    
    /**
     * Data Provider for cross-browser testing
     */
    @DataProvider(name = "browserTestData")
    public Object[][] getBrowserTestData() {
        return new Object[][]{
            {"chrome", false, "Chrome Desktop"},
            {"chrome", true, "Chrome Headless"},
            {"firefox", false, "Firefox Desktop"},
            {"firefox", true, "Firefox Headless"}
        };
    }
    
    /**
     * Cross-browser compatibility test
     */
    @Test(dataProvider = "browserTestData")
    public void testCrossBrowserCompatibility(String browser, boolean headless, String description) {
        log.info("=== Testing Cross-Browser Compatibility: {} ===", description);
        
        boolean driverInitialized = false;
        
        // Initialize appropriate browser
        switch (browser.toLowerCase()) {
            case "chrome":
                driverInitialized = EnterpriseSeleniumKeywords.initializeChromeDriver(
                    headless, false, false, true);
                break;
            case "firefox":
                driverInitialized = EnterpriseSeleniumKeywords.initializeFirefoxDriver(
                    headless, false, false, true);
                break;
        }
        
        Assert.assertTrue(driverInitialized, 
            "Failed to initialize browser: " + description);
        
        try {
            // Test basic functionality across browsers
            EnterpriseSeleniumKeywords.navigateToUrl("https://demo-enterprise-app.com");
            
            // Test page title
            String title = AdvancedSeleniumKeywords.getCurrentTitle();
            Assert.assertNotNull(title, "Title should not be null in " + description);
            
            // Test basic interaction
            if (AdvancedSeleniumKeywords.isElementPresent(By.id("testButton"))) {
                boolean clicked = EnterpriseSeleniumKeywords.clickElementSmart(By.id("testButton"));
                log.info("Button click result in {}: {}", description, clicked);
            }
            
            log.info("Cross-browser test completed for: {}", description);
            
        } finally {
            EnterpriseSeleniumKeywords.quitDriver();
        }
    }
}

/**
 * Parallel Execution Test Example
 * Demonstrates how ThreadLocal WebDriver enables parallel test execution
 */
class ParallelExecutionTests {
    
    private static final Logger log = LoggerFactory.getLogger(ParallelExecutionTests.class);
    
    @BeforeMethod
    public void setUp() {
        // Each thread gets its own WebDriver instance due to ThreadLocal implementation
        boolean initialized = EnterpriseSeleniumKeywords.initializeChromeDriver(
            true, false, false, true); // Headless for faster parallel execution
        Assert.assertTrue(initialized, "Driver initialization failed in thread: " + 
            Thread.currentThread().getName());
    }
    
    @AfterMethod
    public void tearDown() {
        EnterpriseSeleniumKeywords.quitDriver();
    }
    
    /**
     * Parallel test execution - each thread operates independently
     */
    @Test(threadPoolSize = 3, invocationCount = 6, timeOut = 60000)
    public void testParallelExecution() {
        String threadName = Thread.currentThread().getName();
        log.info("Executing parallel test in thread: {}", threadName);
        
        // Each thread navigates to a different page to avoid conflicts
        String testUrl = "https://demo-enterprise-app.com/test?thread=" + 
            threadName.replaceAll("[^a-zA-Z0-9]", "");
        
        boolean navigated = EnterpriseSeleniumKeywords.navigateToUrl(testUrl);
        Assert.assertTrue(navigated, "Navigation failed in thread: " + threadName);
        
        // Perform thread-specific operations
        String currentUrl = AdvancedSeleniumKeywords.getCurrentUrl();
        Assert.assertTrue(currentUrl.contains("thread"), 
            "URL should contain thread identifier in: " + threadName);
        
        // Simulate some work
        try {
            Thread.sleep(2000); // 2 seconds of work
        } catch (InterruptedException e) {
            Thread.currentThread().interrupt();
        }
        
        log.info("Parallel test completed successfully in thread: {}", threadName);
    }
}

/**
 * Performance Testing Integration
 * Demonstrates how to integrate performance monitoring with functional tests
 */
class PerformanceIntegratedTests {
    
    private static final Logger log = LoggerFactory.getLogger(PerformanceIntegratedTests.class);
    
    @BeforeMethod
    public void setUp() {
        EnterpriseSeleniumKeywords.initializeChromeDriver(false, false, false, true);
    }
    
    @AfterMethod  
    public void tearDown() {
        EnterpriseSeleniumKeywords.quitDriver();
    }
    
    /**
     * Test that includes performance assertions
     */
    @Test
    public void testPageLoadPerformance() {
        log.info("=== Performance Integration Test ===");
        
        long startTime = System.currentTimeMillis();
        
        // Navigate and measure time
        boolean navigated = EnterpriseSeleniumKeywords.navigateToUrl(
            "https://demo-enterprise-app.com/heavy-content");
        long navigationTime = System.currentTimeMillis() - startTime;
        
        Assert.assertTrue(navigated, "Navigation should succeed");
        Assert.assertTrue(navigationTime < 10000, 
            "Page should load within 10 seconds. Actual: " + navigationTime + "ms");
        
        // Test interactive element response time
        startTime = System.currentTimeMillis();
        boolean elementFound = EnterpriseSeleniumKeywords.waitForElementVisible(
            By.id("interactiveElement"), 5);
        long interactionTime = System.currentTimeMillis() - startTime;
        
        if (elementFound) {
            Assert.assertTrue(interactionTime < 3000, 
                "Interactive element should be ready within 3 seconds. Actual: " + interactionTime + "ms");
        }
        
        // Test JavaScript execution performance
        startTime = System.currentTimeMillis();
        Object result = AdvancedSeleniumKeywords.executeJavaScript(
            "return document.readyState;");
        long jsTime = System.currentTimeMillis() - startTime;
        
        Assert.assertEquals(result, "complete", "Page should be fully loaded");
        Assert.assertTrue(jsTime < 1000, 
            "JavaScript execution should be fast. Actual: " + jsTime + "ms");
        
        log.info("Performance test completed - Navigation: {}ms, Interaction: {}ms, JS: {}ms", 
                navigationTime, interactionTime, jsTime);
    }
}

/**
 * TestNG XML Configuration Example
 * 
 * Save this as testng.xml in your project root:
 * 
 * <?xml version="1.0" encoding="UTF-8"?>
 * <suite name="EnterpriseSeleniumSuite" parallel="methods" thread-count="3">
 *     
 *     <!-- Main Test Suite -->
 *     <test name="Enterprise-Tests">
 *         <parameter name="browser" value="chrome"/>
 *         <parameter name="headless" value="false"/>
 *         <parameter name="environment" value="qa"/>
 *         <classes>
 *             <class name="com.enterprise.selenium.tests.EnterpriseTestSuite"/>
 *         </classes>
 *     </test>
 *     
 *     <!-- Cross-Browser Tests -->
 *     <test name="Cross-Browser-Tests">
 *         <classes>
 *             <class name="com.enterprise.selenium.tests.DataDrivenEnterpriseTests"/>
 *         </classes>
 *     </test>
 *     
 *     <!-- Parallel Execution Tests -->
 *     <test name="Parallel-Tests">
 *         <classes>
 *             <class name="com.enterprise.selenium.tests.ParallelExecutionTests"/>
 *         </classes>
 *     </test>
 *     
 *     <!-- Performance Tests -->
 *     <test name="Performance-Tests">
 *         <classes>
 *             <class name="com.enterprise.selenium.tests.PerformanceIntegratedTests"/>
 *         </classes>
 *     </test>
 *     
 * </suite>
 */