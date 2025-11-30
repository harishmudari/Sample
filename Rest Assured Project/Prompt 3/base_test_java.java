package com.harish.api.framework.base;

import com.aventstack.extentreports.ExtentReports;
import com.aventstack.extentreports.ExtentTest;
import com.aventstack.extentreports.reporter.ExtentSparkReporter;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import org.testng.ITestResult;
import org.testng.annotations.*;
import java.io.File;
import java.text.SimpleDateFormat;
import java.util.Date;

/**
 * BaseTest is the parent class for all TestNG test classes in the framework.
 * 
 * Responsibilities:
 * - Initialize and manage ExtentReports for HTML reporting.
 * - Set up logging for each test method.
 * - Capture test execution results and attach logs to reports.
 * - Provide hook points for test lifecycle (BeforeSuite, AfterSuite, BeforeMethod, AfterMethod).
 * 
 * Usage:
 * All test classes should extend BaseTest. TestNG annotations will be inherited.
 * Example:
 *   
 *   public class AccountServiceTests extends BaseTest {
 *       private AccountServiceClient accountClient;
 *       
 *       @BeforeMethod
 *       @Override
 *       public void setup() {
 *           super.setup();  // Call parent setup
 *           accountClient = new AccountServiceClient();
 *       }
 *       
 *       @Test(description = "Test fetch account by ID")
 *       public void testGetAccountById() {
 *           logger.info("Test: testGetAccountById started");
 *           Account account = accountClient.getAccountById("ACC123");
 *           assert account.getId().equals("ACC123");
 *           logger.info("Test: testGetAccountById completed successfully");
 *       }
 *   }
 * 
 * Reporting Flow:
 * 1. @BeforeSuite -> Initialize ExtentReports
 * 2. @BeforeMethod -> Create ExtentTest and log test start
 * 3. Test execution
 * 4. @AfterMethod -> Capture result, log status
 * 5. @AfterSuite -> Flush reports and close resources
 */
public class BaseTest {
    
    // Logger for framework-level logs
    protected static final Logger logger = LogManager.getLogger(BaseTest.class);
    
    // Extent Reports objects
    protected static ExtentReports extent;
    protected ExtentTest test;
    
    // Report file path
    private static final String REPORTS_PATH = "target/extent-reports/";
    private static final String TIMESTAMP_FORMAT = "yyyy_MM_dd_HH_mm_ss";
    
    /**
     * Suite-level setup: Initialize Extent Reports.
     * Executed once before all tests in the suite.
     * 
     * Creates:
     * - Reports directory if it doesn't exist
     * - Timestamped HTML report file
     * - ExtentSparkReporter with default configuration
     */
    @BeforeSuite(alwaysRun = true)
    public void initializeReporting() {
        logger.info("========== INITIALIZING EXTENT REPORTS ==========");
        
        // Create reports directory
        File reportsDir = new File(REPORTS_PATH);
        if (!reportsDir.exists()) {
            boolean created = reportsDir.mkdirs();
            logger.info("Reports directory created: {} (Success: {})", REPORTS_PATH, created);
        }
        
        // Generate timestamped report file name
        String timestamp = new SimpleDateFormat(TIMESTAMP_FORMAT).format(new Date());
        String reportFilePath = REPORTS_PATH + "TestReport_" + timestamp + ".html";
        
        // Initialize ExtentReports with Spark Reporter
        ExtentSparkReporter sparkReporter = new ExtentSparkReporter(reportFilePath);
        sparkReporter.config().setReportName("Banking API Automation Report");
        sparkReporter.config().setDocumentTitle("API Test Execution Report");
        sparkReporter.config().setTheme(com.aventstack.extentreports.reporter.configuration.Theme.DARK);
        
        extent = new ExtentReports();
        extent.attachReporter(sparkReporter);
        extent.setSystemInfo("Environment", System.getProperty("env", "DEV"));
        extent.setSystemInfo("Java Version", System.getProperty("java.version"));
        extent.setSystemInfo("OS Name", System.getProperty("os.name"));
        extent.setSystemInfo("User Name", System.getProperty("user.name"));
        
        logger.info("Extent Reports initialized at: {}", reportFilePath);
        logger.info("========== SUITE INITIALIZATION COMPLETE ==========\n");
    }
    
    /**
     * Test-level setup: Create ExtentTest and log test start.
     * Executed before each @Test method.
     * 
     * Creates an ExtentTest node for the current test method.
     * Logs test metadata and execution start time.
     */
    @BeforeMethod(alwaysRun = true)
    public void setup() {
        // Note: Test name is captured from @Test(description) or method name by TestNG
        String testName = Thread.currentThread().getStackTrace()[2].getMethodName();
        
        // Create ExtentTest node
        test = extent.createTest(testName);
        
        logger.info("========== TEST START: {} ==========", testName);
        test.info("Test Method: " + testName);
        test.info("Execution Started at: " + new Date());
    }
    
    /**
     * Test-level teardown: Capture test result and attach logs/status.
     * Executed after each @Test method (pass or fail).
     * 
     * Captures:
     * - Test execution status (PASS/FAIL/SKIP)
     * - Failure exception details if test failed
     * - Test execution duration
     * - End timestamp
     * 
     * TODO: In a UI automation framework, this is where you would:
     *   - Capture screenshots on failure
     *   - Attach network logs
     *   - Collect performance metrics
     */
    @AfterMethod(alwaysRun = true)
    public void teardown(ITestResult result) {
        String testName = result.getMethod().getMethodName();
        long executionTime = result.getEndMillis() - result.getStartMillis();
        
        // Determine test status and add to report
        switch (result.getStatus()) {
            case ITestResult.SUCCESS:
                logger.info("========== TEST PASSED: {} (Duration: {}ms) ==========\n", 
                           testName, executionTime);
                test.pass("Test passed successfully in " + executionTime + " ms");
                break;
                
            case ITestResult.FAILURE:
                logger.error("========== TEST FAILED: {} (Duration: {}ms) ==========", 
                            testName, executionTime);
                logger.error("Failure Exception: {}", result.getThrowable());
                test.fail("Test failed with exception: " + result.getThrowable().getMessage());
                test.assignCategory("FAILED");
                if (result.getThrowable() != null) {
                    test.fail(result.getThrowable());
                }
                break;
                
            case ITestResult.SKIP:
                logger.warn("========== TEST SKIPPED: {} ==========\n", testName);
                test.skip("Test skipped");
                test.assignCategory("SKIPPED");
                break;
                
            default:
                logger.warn("========== TEST UNKNOWN STATUS: {} ==========\n", testName);
                test.info("Unknown test status");
        }
        
        // Add execution metadata
        test.info("Execution Duration: " + executionTime + " ms");
        test.info("Test Completed at: " + new Date());
    }
    
    /**
     * Suite-level teardown: Flush and close Extent Reports.
     * Executed once after all tests in the suite.
     * 
     * This generates the final HTML report file and closes all resources.
     * Report is available at: target/extent-reports/TestReport_*.html
     */
    @AfterSuite(alwaysRun = true)
    public void flushReports() {
        if (extent != null) {
            logger.info("\n========== FLUSHING EXTENT REPORTS ==========");
            extent.flush();
            logger.info("Report generation completed. Check target/extent-reports/ for HTML reports.");
            logger.info("========== SUITE EXECUTION COMPLETE ==========");
        }
    }
    
    /**
     * Utility method to log test information.
     * Use this in test methods to add custom log entries to both console and report.
     * 
     * @param message the message to log
     */
    protected void logInfo(String message) {
        logger.info(message);
        if (test != null) {
            test.info(message);
        }
    }
    
    /**
     * Utility method to log warnings.
     * 
     * @param message the message to log
     */
    protected void logWarning(String message) {
        logger.warn(message);
        if (test != null) {
            test.warning(message);
        }
    }
    
    /**
     * Utility method to log errors.
     * 
     * @param message the message to log
     */
    protected void logError(String message) {
        logger.error(message);
        if (test != null) {
            test.fail(message);
        }
    }
}
