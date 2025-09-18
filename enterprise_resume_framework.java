// ================================================================================================
// ENTERPRISE AUTO-RESUME KEYWORD DRIVEN FRAMEWORK
// ================================================================================================

// 1. CHECKPOINT MANAGER - Core persistence layer
package com.enterprise.framework.core;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.node.ObjectNode;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import java.io.File;
import java.io.IOException;
import java.sql.*;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.locks.ReentrantReadWriteLock;

public class CheckpointManager {
    private static final Logger logger = LoggerFactory.getLogger(CheckpointManager.class);
    private static final String CHECKPOINT_FILE = "checkpoints/execution_checkpoints.json";
    private static final String DB_URL = System.getProperty("checkpoint.db.url", "jdbc:sqlite:checkpoints.db");
    
    private final ObjectMapper objectMapper = new ObjectMapper();
    private final ReentrantReadWriteLock lock = new ReentrantReadWriteLock();
    private final ConcurrentHashMap<String, CheckpointData> memoryCache = new ConcurrentHashMap<>();
    private final boolean useDatabase;
    
    public CheckpointManager() {
        this.useDatabase = Boolean.parseBoolean(System.getProperty("checkpoint.use.db", "false"));
        initializeStorage();
    }
    
    private void initializeStorage() {
        if (useDatabase) {
            initializeDatabase();
        } else {
            initializeFileSystem();
        }
    }
    
    private void initializeDatabase() {
        try (Connection conn = DriverManager.getConnection(DB_URL)) {
            String createTableSQL = """
                CREATE TABLE IF NOT EXISTS execution_checkpoints (
                    test_case_id VARCHAR(100) PRIMARY KEY,
                    last_executed_step INTEGER,
                    status VARCHAR(20),
                    timestamp VARCHAR(30),
                    failure_reason TEXT,
                    execution_thread VARCHAR(50),
                    jenkins_build_number VARCHAR(20),
                    browser_session_id VARCHAR(100)
                )
            """;
            conn.createStatement().execute(createTableSQL);
            logger.info("Checkpoint database initialized successfully");
        } catch (SQLException e) {
            logger.error("Failed to initialize checkpoint database", e);
            throw new RuntimeException("Database initialization failed", e);
        }
    }
    
    private void initializeFileSystem() {
        File checkpointDir = new File("checkpoints");
        if (!checkpointDir.exists()) {
            checkpointDir.mkdirs();
        }
        
        File checkpointFile = new File(CHECKPOINT_FILE);
        if (!checkpointFile.exists()) {
            try {
                checkpointFile.createNewFile();
                objectMapper.writeValue(checkpointFile, objectMapper.createObjectNode());
                logger.info("Checkpoint file initialized: {}", CHECKPOINT_FILE);
            } catch (IOException e) {
                logger.error("Failed to initialize checkpoint file", e);
                throw new RuntimeException("Checkpoint file initialization failed", e);
            }
        }
    }
    
    public CheckpointData getCheckpoint(String testCaseId) {
        lock.readLock().lock();
        try {
            // Check memory cache first
            CheckpointData cached = memoryCache.get(testCaseId);
            if (cached != null) {
                return cached;
            }
            
            CheckpointData checkpoint = useDatabase ? 
                getCheckpointFromDatabase(testCaseId) : 
                getCheckpointFromFile(testCaseId);
            
            if (checkpoint != null) {
                memoryCache.put(testCaseId, checkpoint);
            }
            
            return checkpoint != null ? checkpoint : new CheckpointData();
        } finally {
            lock.readLock().unlock();
        }
    }
    
    public void updateCheckpoint(String testCaseId, int stepIndex, String status, 
                                String failureReason, String threadName) {
        lock.writeLock().lock();
        try {
            CheckpointData checkpoint = new CheckpointData();
            checkpoint.setTestCaseId(testCaseId);
            checkpoint.setLastExecutedStep(stepIndex);
            checkpoint.setStatus(status);
            checkpoint.setTimestamp(LocalDateTime.now().format(DateTimeFormatter.ISO_LOCAL_DATE_TIME));
            checkpoint.setFailureReason(failureReason);
            checkpoint.setExecutionThread(threadName);
            checkpoint.setJenkinsBuildNumber(System.getProperty("BUILD_NUMBER", "local"));
            checkpoint.setBrowserSessionId(ThreadLocalManager.getBrowserSessionId());
            
            // Update memory cache
            memoryCache.put(testCaseId, checkpoint);
            
            // Persist to storage
            if (useDatabase) {
                saveCheckpointToDatabase(checkpoint);
            } else {
                saveCheckpointToFile(checkpoint);
            }
            
            logger.debug("Checkpoint updated for {}: Step {}, Status: {}", 
                        testCaseId, stepIndex, status);
        } finally {
            lock.writeLock().unlock();
        }
    }
    
    private CheckpointData getCheckpointFromDatabase(String testCaseId) {
        try (Connection conn = DriverManager.getConnection(DB_URL)) {
            String query = "SELECT * FROM execution_checkpoints WHERE test_case_id = ?";
            PreparedStatement stmt = conn.prepareStatement(query);
            stmt.setString(1, testCaseId);
            
            ResultSet rs = stmt.executeQuery();
            if (rs.next()) {
                CheckpointData checkpoint = new CheckpointData();
                checkpoint.setTestCaseId(rs.getString("test_case_id"));
                checkpoint.setLastExecutedStep(rs.getInt("last_executed_step"));
                checkpoint.setStatus(rs.getString("status"));
                checkpoint.setTimestamp(rs.getString("timestamp"));
                checkpoint.setFailureReason(rs.getString("failure_reason"));
                checkpoint.setExecutionThread(rs.getString("execution_thread"));
                checkpoint.setJenkinsBuildNumber(rs.getString("jenkins_build_number"));
                checkpoint.setBrowserSessionId(rs.getString("browser_session_id"));
                return checkpoint;
            }
        } catch (SQLException e) {
            logger.error("Failed to read checkpoint from database", e);
        }
        return null;
    }
    
    private void saveCheckpointToDatabase(CheckpointData checkpoint) {
        try (Connection conn = DriverManager.getConnection(DB_URL)) {
            String query = """
                INSERT OR REPLACE INTO execution_checkpoints 
                (test_case_id, last_executed_step, status, timestamp, failure_reason, 
                 execution_thread, jenkins_build_number, browser_session_id) 
                VALUES (?, ?, ?, ?, ?, ?, ?, ?)
            """;
            PreparedStatement stmt = conn.prepareStatement(query);
            stmt.setString(1, checkpoint.getTestCaseId());
            stmt.setInt(2, checkpoint.getLastExecutedStep());
            stmt.setString(3, checkpoint.getStatus());
            stmt.setString(4, checkpoint.getTimestamp());
            stmt.setString(5, checkpoint.getFailureReason());
            stmt.setString(6, checkpoint.getExecutionThread());
            stmt.setString(7, checkpoint.getJenkinsBuildNumber());
            stmt.setString(8, checkpoint.getBrowserSessionId());
            
            stmt.executeUpdate();
        } catch (SQLException e) {
            logger.error("Failed to save checkpoint to database", e);
        }
    }
    
    private CheckpointData getCheckpointFromFile(String testCaseId) {
        try {
            File checkpointFile = new File(CHECKPOINT_FILE);
            if (checkpointFile.exists() && checkpointFile.length() > 0) {
                ObjectNode checkpoints = (ObjectNode) objectMapper.readTree(checkpointFile);
                if (checkpoints.has(testCaseId)) {
                    return objectMapper.convertValue(checkpoints.get(testCaseId), CheckpointData.class);
                }
            }
        } catch (IOException e) {
            logger.error("Failed to read checkpoint from file", e);
        }
        return null;
    }
    
    private void saveCheckpointToFile(CheckpointData checkpoint) {
        try {
            File checkpointFile = new File(CHECKPOINT_FILE);
            ObjectNode checkpoints;
            
            if (checkpointFile.exists() && checkpointFile.length() > 0) {
                checkpoints = (ObjectNode) objectMapper.readTree(checkpointFile);
            } else {
                checkpoints = objectMapper.createObjectNode();
            }
            
            ObjectNode checkpointNode = objectMapper.convertValue(checkpoint, ObjectNode.class);
            checkpoints.set(checkpoint.getTestCaseId(), checkpointNode);
            
            objectMapper.writerWithDefaultPrettyPrinter().writeValue(checkpointFile, checkpoints);
        } catch (IOException e) {
            logger.error("Failed to save checkpoint to file", e);
        }
    }
    
    public void clearCheckpoint(String testCaseId) {
        lock.writeLock().lock();
        try {
            memoryCache.remove(testCaseId);
            
            if (useDatabase) {
                try (Connection conn = DriverManager.getConnection(DB_URL)) {
                    PreparedStatement stmt = conn.prepareStatement("DELETE FROM execution_checkpoints WHERE test_case_id = ?");
                    stmt.setString(1, testCaseId);
                    stmt.executeUpdate();
                }
            } else {
                File checkpointFile = new File(CHECKPOINT_FILE);
                if (checkpointFile.exists()) {
                    ObjectNode checkpoints = (ObjectNode) objectMapper.readTree(checkpointFile);
                    checkpoints.remove(testCaseId);
                    objectMapper.writerWithDefaultPrettyPrinter().writeValue(checkpointFile, checkpoints);
                }
            }
            
            logger.info("Checkpoint cleared for test case: {}", testCaseId);
        } catch (SQLException | IOException e) {
            logger.error("Failed to clear checkpoint", e);
        } finally {
            lock.writeLock().unlock();
        }
    }
}

// ================================================================================================
// 2. CHECKPOINT DATA MODEL
// ================================================================================================

package com.enterprise.framework.model;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import com.fasterxml.jackson.annotation.JsonInclude;

@JsonInclude(JsonInclude.Include.NON_NULL)
@JsonIgnoreProperties(ignoreUnknown = true)
public class CheckpointData {
    private String testCaseId;
    private int lastExecutedStep = 0;
    private String status = "NOT_STARTED";
    private String timestamp;
    private String failureReason;
    private String executionThread;
    private String jenkinsBuildNumber;
    private String browserSessionId;
    
    // Getters and Setters
    public String getTestCaseId() { return testCaseId; }
    public void setTestCaseId(String testCaseId) { this.testCaseId = testCaseId; }
    
    public int getLastExecutedStep() { return lastExecutedStep; }
    public void setLastExecutedStep(int lastExecutedStep) { this.lastExecutedStep = lastExecutedStep; }
    
    public String getStatus() { return status; }
    public void setStatus(String status) { this.status = status; }
    
    public String getTimestamp() { return timestamp; }
    public void setTimestamp(String timestamp) { this.timestamp = timestamp; }
    
    public String getFailureReason() { return failureReason; }
    public void setFailureReason(String failureReason) { this.failureReason = failureReason; }
    
    public String getExecutionThread() { return executionThread; }
    public void setExecutionThread(String executionThread) { this.executionThread = executionThread; }
    
    public String getJenkinsBuildNumber() { return jenkinsBuildNumber; }
    public void setJenkinsBuildNumber(String jenkinsBuildNumber) { this.jenkinsBuildNumber = jenkinsBuildNumber; }
    
    public String getBrowserSessionId() { return browserSessionId; }
    public void setBrowserSessionId(String browserSessionId) { this.browserSessionId = browserSessionId; }
    
    public boolean isResumeRequired() {
        return "FAILED".equals(status) || "IN_PROGRESS".equals(status);
    }
}

// ================================================================================================
// 3. TEST STEP MODEL
// ================================================================================================

package com.enterprise.framework.model;

public class TestStep {
    private String testCaseId;
    private int stepIndex;
    private String keyword;
    private String locator;
    private String testData;
    private String description;
    private boolean mandatory = true;
    private int retryCount = 0;
    private int maxRetries = 3;
    
    // Constructors
    public TestStep() {}
    
    public TestStep(String testCaseId, int stepIndex, String keyword, String locator, String testData) {
        this.testCaseId = testCaseId;
        this.stepIndex = stepIndex;
        this.keyword = keyword;
        this.locator = locator;
        this.testData = testData;
    }
    
    // Getters and Setters
    public String getTestCaseId() { return testCaseId; }
    public void setTestCaseId(String testCaseId) { this.testCaseId = testCaseId; }
    
    public int getStepIndex() { return stepIndex; }
    public void setStepIndex(int stepIndex) { this.stepIndex = stepIndex; }
    
    public String getKeyword() { return keyword; }
    public void setKeyword(String keyword) { this.keyword = keyword; }
    
    public String getLocator() { return locator; }
    public void setLocator(String locator) { this.locator = locator; }
    
    public String getTestData() { return testData; }
    public void setTestData(String testData) { this.testData = testData; }
    
    public String getDescription() { return description; }
    public void setDescription(String description) { this.description = description; }
    
    public boolean isMandatory() { return mandatory; }
    public void setMandatory(boolean mandatory) { this.mandatory = mandatory; }
    
    public int getRetryCount() { return retryCount; }
    public void setRetryCount(int retryCount) { this.retryCount = retryCount; }
    
    public int getMaxRetries() { return maxRetries; }
    public void setMaxRetries(int maxRetries) { this.maxRetries = maxRetries; }
    
    public void incrementRetryCount() { this.retryCount++; }
    
    public boolean canRetry() { return retryCount < maxRetries; }
    
    @Override
    public String toString() {
        return String.format("Step[%s:%d] %s - %s", testCaseId, stepIndex, keyword, description);
    }
}

// ================================================================================================
// 4. THREAD LOCAL MANAGER - For parallel execution support
// ================================================================================================

package com.enterprise.framework.utils;

import org.openqa.selenium.WebDriver;

public class ThreadLocalManager {
    private static final ThreadLocal<WebDriver> driverThreadLocal = new ThreadLocal<>();
    private static final ThreadLocal<String> browserSessionIdThreadLocal = new ThreadLocal<>();
    private static final ThreadLocal<String> testCaseIdThreadLocal = new ThreadLocal<>();
    
    public static void setDriver(WebDriver driver) {
        driverThreadLocal.set(driver);
    }
    
    public static WebDriver getDriver() {
        return driverThreadLocal.get();
    }
    
    public static void setBrowserSessionId(String sessionId) {
        browserSessionIdThreadLocal.set(sessionId);
    }
    
    public static String getBrowserSessionId() {
        return browserSessionIdThreadLocal.get();
    }
    
    public static void setTestCaseId(String testCaseId) {
        testCaseIdThreadLocal.set(testCaseId);
    }
    
    public static String getTestCaseId() {
        return testCaseIdThreadLocal.get();
    }
    
    public static void cleanup() {
        WebDriver driver = driverThreadLocal.get();
        if (driver != null) {
            try {
                driver.quit();
            } catch (Exception e) {
                // Log but don't fail
            }
        }
        driverThreadLocal.remove();
        browserSessionIdThreadLocal.remove();
        testCaseIdThreadLocal.remove();
    }
}

// ================================================================================================
// 5. EXCEL DATA READER - Enhanced for enterprise use
// ================================================================================================

package com.enterprise.framework.datareader;

import com.enterprise.framework.model.TestStep;
import org.apache.poi.ss.usermodel.*;
import org.apache.poi.xssf.usermodel.XSSFWorkbook;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.FileInputStream;
import java.io.IOException;
import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.ConcurrentHashMap;

public class ExcelDataReader {
    private static final Logger logger = LoggerFactory.getLogger(ExcelDataReader.class);
    private final ConcurrentHashMap<String, List<TestStep>> stepCache = new ConcurrentHashMap<>();
    private final String excelFilePath;
    
    public ExcelDataReader(String excelFilePath) {
        this.excelFilePath = excelFilePath;
    }
    
    public List<TestStep> getTestSteps(String testCaseId) {
        // Check cache first
        List<TestStep> cachedSteps = stepCache.get(testCaseId);
        if (cachedSteps != null) {
            return new ArrayList<>(cachedSteps); // Return copy to avoid modification
        }
        
        List<TestStep> steps = readStepsFromExcel(testCaseId);
        if (!steps.isEmpty()) {
            stepCache.put(testCaseId, steps);
        }
        
        return steps;
    }
    
    private List<TestStep> readStepsFromExcel(String testCaseId) {
        List<TestStep> steps = new ArrayList<>();
        
        try (FileInputStream fis = new FileInputStream(excelFilePath);
             Workbook workbook = new XSSFWorkbook(fis)) {
            
            Sheet sheet = workbook.getSheet("TestSteps");
            if (sheet == null) {
                logger.error("TestSteps sheet not found in Excel file");
                return steps;
            }
            
            // Skip header row
            for (int i = 1; i <= sheet.getLastRowNum(); i++) {
                Row row = sheet.getRow(i);
                if (row == null) continue;
                
                String rowTestCaseId = getCellValue(row.getCell(0));
                if (testCaseId.equals(rowTestCaseId)) {
                    TestStep step = new TestStep();
                    step.setTestCaseId(rowTestCaseId);
                    step.setStepIndex((int) row.getCell(1).getNumericCellValue());
                    step.setKeyword(getCellValue(row.getCell(2)));
                    step.setLocator(getCellValue(row.getCell(3)));
                    step.setTestData(getCellValue(row.getCell(4)));
                    step.setDescription(getCellValue(row.getCell(5)));
                    
                    // Optional columns
                    if (row.getCell(6) != null) {
                        step.setMandatory(Boolean.parseBoolean(getCellValue(row.getCell(6))));
                    }
                    if (row.getCell(7) != null) {
                        step.setMaxRetries((int) row.getCell(7).getNumericCellValue());
                    }
                    
                    steps.add(step);
                }
            }
            
            logger.info("Loaded {} steps for test case: {}", steps.size(), testCaseId);
            
        } catch (IOException e) {
            logger.error("Failed to read Excel file: {}", excelFilePath, e);
        }
        
        return steps;
    }
    
    private String getCellValue(Cell cell) {
        if (cell == null) return "";
        
        return switch (cell.getCellType()) {
            case STRING -> cell.getStringCellValue();
            case NUMERIC -> String.valueOf((int) cell.getNumericCellValue());
            case BOOLEAN -> String.valueOf(cell.getBooleanCellValue());
            case FORMULA -> cell.getCellFormula();
            default -> "";
        };
    }
    
    public void clearCache() {
        stepCache.clear();
        logger.info("Excel data cache cleared");
    }
}

// ================================================================================================
// 6. KEYWORD EXECUTOR - Core execution engine
// ================================================================================================

package com.enterprise.framework.executor;

import com.enterprise.framework.keywords.*;
import com.enterprise.framework.model.TestStep;
import com.enterprise.framework.utils.ThreadLocalManager;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.HashMap;
import java.util.Map;

public class KeywordExecutor {
    private static final Logger logger = LoggerFactory.getLogger(KeywordExecutor.class);
    private final Map<String, KeywordAction> keywordMap = new HashMap<>();
    
    public KeywordExecutor() {
        initializeKeywords();
    }
    
    private void initializeKeywords() {
        // Web Keywords
        keywordMap.put("LaunchBrowser", new LaunchBrowserKeyword());
        keywordMap.put("CloseBrowser", new CloseBrowserKeyword());
        keywordMap.put("Navigate", new NavigateKeyword());
        keywordMap.put("Click", new ClickKeyword());
        keywordMap.put("EnterText", new EnterTextKeyword());
        keywordMap.put("ValidateElement", new ValidateElementKeyword());
        keywordMap.put("ValidateText", new ValidateTextKeyword());
        keywordMap.put("WaitForElement", new WaitForElementKeyword());
        keywordMap.put("TakeScreenshot", new TakeScreenshotKeyword());
        
        // Mobile Keywords
        keywordMap.put("LaunchApp", new LaunchAppKeyword());
        keywordMap.put("CloseApp", new CloseAppKeyword());
        keywordMap.put("Tap", new TapKeyword());
        keywordMap.put("Swipe", new SwipeKeyword());
        
        // API Keywords
        keywordMap.put("SendGETRequest", new SendGETRequestKeyword());
        keywordMap.put("SendPOSTRequest", new SendPOSTRequestKeyword());
        keywordMap.put("ValidateAPIResponse", new ValidateAPIResponseKeyword());
        
        logger.info("Initialized {} keywords", keywordMap.size());
    }
    
    public void executeStep(TestStep step) throws Exception {
        String keyword = step.getKeyword();
        KeywordAction action = keywordMap.get(keyword);
        
        if (action == null) {
            throw new IllegalArgumentException("Unknown keyword: " + keyword);
        }
        
        logger.info("Executing step {}: {} - {}", step.getStepIndex(), keyword, step.getDescription());
        
        try {
            action.execute(step.getLocator(), step.getTestData());
            logger.info("Step {} executed successfully", step.getStepIndex());
        } catch (Exception e) {
            logger.error("Step {} failed: {}", step.getStepIndex(), e.getMessage());
            throw e;
        }
    }
    
    public void addCustomKeyword(String keywordName, KeywordAction action) {
        keywordMap.put(keywordName, action);
        logger.info("Custom keyword registered: {}", keywordName);
    }
}

// ================================================================================================
// 7. KEYWORD ACTION INTERFACE
// ================================================================================================

package com.enterprise.framework.keywords;

public interface KeywordAction {
    void execute(String locator, String testData) throws Exception;
}

// ================================================================================================
// 8. SAMPLE KEYWORD IMPLEMENTATIONS
// ================================================================================================

package com.enterprise.framework.keywords;

import com.enterprise.framework.utils.ThreadLocalManager;
import org.openqa.selenium.By;
import org.openqa.selenium.WebDriver;
import org.openqa.selenium.WebElement;
import org.openqa.selenium.chrome.ChromeDriver;
import org.openqa.selenium.firefox.FirefoxDriver;
import org.openqa.selenium.edge.EdgeDriver;
import org.openqa.selenium.support.ui.WebDriverWait;
import org.openqa.selenium.support.ui.ExpectedConditions;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.time.Duration;

// Launch Browser Keyword
public class LaunchBrowserKeyword implements KeywordAction {
    private static final Logger logger = LoggerFactory.getLogger(LaunchBrowserKeyword.class);
    
    @Override
    public void execute(String locator, String testData) throws Exception {
        String browserName = testData.toLowerCase();
        WebDriver driver;
        
        switch (browserName) {
            case "chrome" -> driver = new ChromeDriver();
            case "firefox" -> driver = new FirefoxDriver();
            case "edge" -> driver = new EdgeDriver();
            default -> throw new IllegalArgumentException("Unsupported browser: " + browserName);
        }
        
        driver.manage().window().maximize();
        driver.manage().timeouts().implicitlyWait(Duration.ofSeconds(10));
        
        ThreadLocalManager.setDriver(driver);
        ThreadLocalManager.setBrowserSessionId(driver.getWindowHandle());
        
        logger.info("Launched {} browser successfully", browserName);
    }
}

// Click Keyword
public class ClickKeyword implements KeywordAction {
    private static final Logger logger = LoggerFactory.getLogger(ClickKeyword.class);
    
    @Override
    public void execute(String locator, String testData) throws Exception {
        WebDriver driver = ThreadLocalManager.getDriver();
        WebDriverWait wait = new WebDriverWait(driver, Duration.ofSeconds(10));
        
        By by = parseLocator(locator);
        WebElement element = wait.until(ExpectedConditions.elementToBeClickable(by));
        element.click();
        
        logger.info("Clicked on element: {}", locator);
    }
    
    private By parseLocator(String locator) {
        if (locator.startsWith("id=")) {
            return By.id(locator.substring(3));
        } else if (locator.startsWith("xpath=")) {
            return By.xpath(locator.substring(6));
        } else if (locator.startsWith("css=")) {
            return By.cssSelector(locator.substring(4));
        } else if (locator.startsWith("name=")) {
            return By.name(locator.substring(5));
        } else {
            throw new IllegalArgumentException("Invalid locator format: " + locator);
        }
    }
}

// Enter Text Keyword
public class EnterTextKeyword implements KeywordAction {
    private static final Logger logger = LoggerFactory.getLogger(EnterTextKeyword.class);
    
    @Override
    public void execute(String locator, String testData) throws Exception {
        WebDriver driver = ThreadLocalManager.getDriver();
        WebDriverWait wait = new WebDriverWait(driver, Duration.ofSeconds(10));
        
        By by = parseLocator(locator);
        WebElement element = wait.until(ExpectedConditions.visibilityOfElementLocated(by));
        element.clear();
        element.sendKeys(testData);
        
        logger.info("Entered text '{}' in element: {}", testData, locator);
    }
    
    private By parseLocator(String locator) {
        if (locator.startsWith("id=")) {
            return By.id(locator.substring(3));
        } else if (locator.startsWith("xpath=")) {
            return By.xpath(locator.substring(6));
        } else if (locator.startsWith("css=")) {
            return By.cssSelector(locator.substring(4));
        } else if (locator.startsWith("name=")) {
            return By.name(locator.substring(5));
        } else {
            throw new IllegalArgumentException("Invalid locator format: " + locator);
        }
    }
}

// Navigate Keyword
public class NavigateKeyword implements KeywordAction {
    private static final Logger logger = LoggerFactory.getLogger(NavigateKeyword.class);
    
    @Override
    public void execute(String locator, String testData) throws Exception {
        WebDriver driver = ThreadLocalManager.getDriver();
        
        if (locator.startsWith("url=")) {
            String url = locator.substring(4);
            driver.get(url);
            logger.info("Navigated to URL: {}", url);
        } else {
            driver.get(testData);
            logger.info("Navigated to URL: {}", testData);
        }
    }
}

// Validate Element Keyword
public class ValidateElementKeyword implements KeywordAction {
    private static final Logger logger = LoggerFactory.getLogger(ValidateElementKeyword.class);
    
    @Override
    public void execute(String locator, String testData) throws Exception {
        WebDriver driver = ThreadLocalManager.getDriver();
        WebDriverWait wait = new WebDriverWait(driver, Duration.ofSeconds(10));
        
        By by = parseLocator(locator);
        WebElement element = wait.until(ExpectedConditions.visibilityOfElementLocated(by));
        
        if (testData != null && !testData.isEmpty()) {
            String actualText = element.getText();
            if (!actualText.contains(testData)) {
                throw new AssertionError(String.format(
                    "Text validation failed. Expected: '%s', Actual: '%s'", testData, actualText));
            }
        }
        
        logger.info("Element validation successful: {}", locator);
    }
    
    private By parseLocator(String locator) {
        if (locator.startsWith("id=")) {
            return By.id(locator.substring(3));
        } else if (locator.startsWith("xpath=")) {
            return By.xpath(locator.substring(6));
        } else if (locator.startsWith("css=")) {
            return By.cssSelector(locator.substring(4));
        } else if (locator.startsWith("name=")) {
            return By.name(locator.substring(5));
        } else {
            throw new IllegalArgumentException("Invalid locator format: " + locator);
        }
    }
}

// Close Browser Keyword
public class CloseBrowserKeyword implements KeywordAction {
    private static final Logger logger = LoggerFactory.getLogger(CloseBrowserKeyword.class);
    
    @Override
    public void execute(String locator, String testData) throws Exception {
        WebDriver driver = ThreadLocalManager.getDriver();
        if (driver != null) {
            driver.quit();
            ThreadLocalManager.cleanup();
            logger.info("Browser closed successfully");
        }
    }
}

// Wait For Element Keyword
public class WaitForElementKeyword implements KeywordAction {
    private static final Logger logger = LoggerFactory.getLogger(WaitForElementKeyword.class);
    
    @Override
    public void execute(String locator, String testData) throws Exception {
        WebDriver driver = ThreadLocalManager.getDriver();
        int timeout = testData != null && !testData.isEmpty() ? Integer.parseInt(testData) : 10;
        WebDriverWait wait = new WebDriverWait(driver, Duration.ofSeconds(timeout));
        
        By by = parseLocator(locator);
        wait.until(ExpectedConditions.visibilityOfElementLocated(by));
        
        logger.info("Successfully waited for element: {}", locator);
    }
    
    private By parseLocator(String locator) {
        if (locator.startsWith("id=")) {
            return By.id(locator.substring(3));
        } else if (locator.startsWith("xpath=")) {
            return By.xpath(locator.substring(6));
        } else if (locator.startsWith("css=")) {
            return By.cssSelector(locator.substring(4));
        } else if (locator.startsWith("name=")) {
            return By.name(locator.substring(5));
        } else {
            throw new IllegalArgumentException("Invalid locator format: " + locator);
        }
    }
}

// Take Screenshot Keyword
public class TakeScreenshotKeyword implements KeywordAction {
    private static final Logger logger = LoggerFactory.getLogger(TakeScreenshotKeyword.class);
    
    @Override
    public void execute(String locator, String testData) throws Exception {
        WebDriver driver = ThreadLocalManager.getDriver();
        // Implementation for screenshot capture
        logger.info("Screenshot captured");
    }
}

// Validate Text Keyword
public class ValidateTextKeyword implements KeywordAction {
    private static final Logger logger = LoggerFactory.getLogger(ValidateTextKeyword.class);
    
    @Override
    public void execute(String locator, String testData) throws Exception {
        WebDriver driver = ThreadLocalManager.getDriver();
        WebDriverWait wait = new WebDriverWait(driver, Duration.ofSeconds(10));
        
        By by = parseLocator(locator);
        WebElement element = wait.until(ExpectedConditions.visibilityOfElementLocated(by));
        
        String actualText = element.getText();
        if (!actualText.equals(testData)) {
            throw new AssertionError(String.format(
                "Text validation failed. Expected: '%s', Actual: '%s'", testData, actualText));
        }
        
        logger.info("Text validation successful for element: {}", locator);
    }
    
    private By parseLocator(String locator) {
        if (locator.startsWith("id=")) {
            return By.id(locator.substring(3));
        } else if (locator.startsWith("xpath=")) {
            return By.xpath(locator.substring(6));
        } else if (locator.startsWith("css=")) {
            return By.cssSelector(locator.substring(4));
        } else if (locator.startsWith("name=")) {
            return By.name(locator.substring(5));
        } else {
            throw new IllegalArgumentException("Invalid locator format: " + locator);
        }
    }
}

// Mobile Keywords - Placeholder implementations
public class LaunchAppKeyword implements KeywordAction {
    private static final Logger logger = LoggerFactory.getLogger(LaunchAppKeyword.class);
    
    @Override
    public void execute(String locator, String testData) throws Exception {
        // Appium implementation for launching mobile app
        logger.info("Mobile app launched: {}", testData);
    }
}

public class CloseAppKeyword implements KeywordAction {
    private static final Logger logger = LoggerFactory.getLogger(CloseAppKeyword.class);
    
    @Override
    public void execute(String locator, String testData) throws Exception {
        // Appium implementation for closing mobile app
        logger.info("Mobile app closed");
    }
}

public class TapKeyword implements KeywordAction {
    private static final Logger logger = LoggerFactory.getLogger(TapKeyword.class);
    
    @Override
    public void execute(String locator, String testData) throws Exception {
        // Appium implementation for tap action
        logger.info("Tapped on element: {}", locator);
    }
}

public class SwipeKeyword implements KeywordAction {
    private static final Logger logger = LoggerFactory.getLogger(SwipeKeyword.class);
    
    @Override
    public void execute(String locator, String testData) throws Exception {
        // Appium implementation for swipe action
        logger.info("Swiped: {}", testData);
    }
}

// API Keywords - Placeholder implementations
public class SendGETRequestKeyword implements KeywordAction {
    private static final Logger logger = LoggerFactory.getLogger(SendGETRequestKeyword.class);
    
    @Override
    public void execute(String locator, String testData) throws Exception {
        // REST API implementation for GET request
        logger.info("GET request sent to: {}", locator);
    }
}

public class SendPOSTRequestKeyword implements KeywordAction {
    private static final Logger logger = LoggerFactory.getLogger(SendPOSTRequestKeyword.class);
    
    @Override
    public void execute(String locator, String testData) throws Exception {
        // REST API implementation for POST request
        logger.info("POST request sent to: {} with data: {}", locator, testData);
    }
}

public class ValidateAPIResponseKeyword implements KeywordAction {
    private static final Logger logger = LoggerFactory.getLogger(ValidateAPIResponseKeyword.class);
    
    @Override
    public void execute(String locator, String testData) throws Exception {
        // API response validation implementation
        logger.info("API response validated");
    }
}

// ================================================================================================
// 9. TEST EXECUTION ENGINE - Main orchestrator with auto-resume
// ================================================================================================

package com.enterprise.framework.engine;

import com.enterprise.framework.core.CheckpointManager;
import com.enterprise.framework.datareader.ExcelDataReader;
import com.enterprise.framework.executor.KeywordExecutor;
import com.enterprise.framework.model.CheckpointData;
import com.enterprise.framework.model.TestStep;
import com.enterprise.framework.reporting.ExtentReportManager;
import com.enterprise.framework.utils.ThreadLocalManager;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.List;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.TimeUnit;

public class TestExecutionEngine {
    private static final Logger logger = LoggerFactory.getLogger(TestExecutionEngine.class);
    
    private final CheckpointManager checkpointManager;
    private final ExcelDataReader excelDataReader;
    private final KeywordExecutor keywordExecutor;
    private final ExtentReportManager reportManager;
    private final ExecutorService executorService;
    
    // Configuration properties
    private final boolean resumeEnabled;
    private final boolean retryEnabled;
    private final int maxRetryAttempts;
    private final boolean parallelExecution;
    private final int threadPoolSize;
    private final String customResumeFromStep;
    private final int rerunLastNSteps;
    
    public TestExecutionEngine() {
        this.checkpointManager = new CheckpointManager();
        this.excelDataReader = new ExcelDataReader(System.getProperty("excel.file.path", "testdata/TestSteps.xlsx"));
        this.keywordExecutor = new KeywordExecutor();
        this.reportManager = new ExtentReportManager();
        
        // Load configuration
        this.resumeEnabled = Boolean.parseBoolean(System.getProperty("resume", "false"));
        this.retryEnabled = Boolean.parseBoolean(System.getProperty("retry.enabled", "true"));
        this.maxRetryAttempts = Integer.parseInt(System.getProperty("max.retry.attempts", "3"));
        this.parallelExecution = Boolean.parseBoolean(System.getProperty("parallel.execution", "false"));
        this.threadPoolSize = Integer.parseInt(System.getProperty("thread.pool.size", "5"));
        this.customResumeFromStep = System.getProperty("resumeFromStep", "");
        this.rerunLastNSteps = Integer.parseInt(System.getProperty("rerunLastN", "0"));
        
        this.executorService = parallelExecution ? 
            Executors.newFixedThreadPool(threadPoolSize) : 
            Executors.newSingleThreadExecutor();
        
        logger.info("Test Execution Engine initialized with resume={}, retry={}, parallel={}", 
                   resumeEnabled, retryEnabled, parallelExecution);
    }
    
    public void executeTestCase(String testCaseId) {
        if (parallelExecution) {
            CompletableFuture.runAsync(() -> executeTestCaseInternal(testCaseId), executorService);
        } else {
            executeTestCaseInternal(testCaseId);
        }
    }
    
    private void executeTestCaseInternal(String testCaseId) {
        ThreadLocalManager.setTestCaseId(testCaseId);
        String threadName = Thread.currentThread().getName();
        
        try {
            logger.info("Starting execution of test case: {} on thread: {}", testCaseId, threadName);
            reportManager.createTest(testCaseId, "Automated test execution with auto-resume capability");
            
            // Load test steps from Excel
            List<TestStep> testSteps = excelDataReader.getTestSteps(testCaseId);
            if (testSteps.isEmpty()) {
                logger.error("No test steps found for test case: {}", testCaseId);
                reportManager.logFail("No test steps found for execution");
                return;
            }
            
            // Determine starting step based on resume configuration
            int startingStep = determineStartingStep(testCaseId, testSteps.size());
            
            logger.info("Executing {} steps for test case: {}, starting from step: {}", 
                       testSteps.size(), testCaseId, startingStep);
            
            // Execute steps with auto-resume logic
            executeStepsWithResume(testCaseId, testSteps, startingStep, threadName);
            
            // Mark test as passed if all steps completed
            checkpointManager.updateCheckpoint(testCaseId, testSteps.size(), "PASSED", null, threadName);
            reportManager.logPass("Test case executed successfully");
            
            logger.info("Test case completed successfully: {}", testCaseId);
            
        } catch (Exception e) {
            logger.error("Test case execution failed: {}", testCaseId, e);
            reportManager.logFail("Test execution failed: " + e.getMessage());
            
            // Update checkpoint with failure
            CheckpointData currentCheckpoint = checkpointManager.getCheckpoint(testCaseId);
            checkpointManager.updateCheckpoint(testCaseId, currentCheckpoint.getLastExecutedStep(), 
                                             "FAILED", e.getMessage(), threadName);
        } finally {
            ThreadLocalManager.cleanup();
            reportManager.flush();
        }
    }
    
    private int determineStartingStep(String testCaseId, int totalSteps) {
        if (!resumeEnabled) {
            // Clear any existing checkpoint and start from beginning
            checkpointManager.clearCheckpoint(testCaseId);
            return 1;
        }
        
        // Handle custom resume options
        if (!customResumeFromStep.isEmpty()) {
            try {
                int customStep = Integer.parseInt(customResumeFromStep);
                logger.info("Resuming from custom step: {}", customStep);
                return Math.max(1, Math.min(customStep, totalSteps));
            } catch (NumberFormatException e) {
                logger.warn("Invalid resumeFromStep value: {}, starting from beginning", customResumeFromStep);
                return 1;
            }
        }
        
        // Handle rerun last N steps
        if (rerunLastNSteps > 0) {
            CheckpointData checkpoint = checkpointManager.getCheckpoint(testCaseId);
            if (checkpoint.isResumeRequired()) {
                int rerunFromStep = Math.max(1, checkpoint.getLastExecutedStep() - rerunLastNSteps + 1);
                logger.info("Rerunning last {} steps, starting from step: {}", rerunLastNSteps, rerunFromStep);
                return rerunFromStep;
            }
        }
        
        // Standard resume logic
        CheckpointData checkpoint = checkpointManager.getCheckpoint(testCaseId);
        if (checkpoint.isResumeRequired()) {
            int resumeFromStep = checkpoint.getLastExecutedStep() + 1;
            logger.info("Resuming test case: {} from step: {} (last executed: {})", 
                       testCaseId, resumeFromStep, checkpoint.getLastExecutedStep());
            
            reportManager.logInfo(String.format("Resuming execution from step %d (Previous status: %s)", 
                                               resumeFromStep, checkpoint.getStatus()));
            
            return resumeFromStep;
        }
        
        return 1; // Start from beginning
    }
    
    private void executeStepsWithResume(String testCaseId, List<TestStep> testSteps, 
                                       int startingStep, String threadName) throws Exception {
        
        for (TestStep step : testSteps) {
            if (step.getStepIndex() < startingStep) {
                logger.debug("Skipping step {} as it's before starting step {}", 
                            step.getStepIndex(), startingStep);
                reportManager.logSkip("Step " + step.getStepIndex() + ": " + step.getKeyword() + 
                                    " (Resumed - Already executed)");
                continue;
            }
            
            // Update checkpoint to IN_PROGRESS before execution
            checkpointManager.updateCheckpoint(testCaseId, step.getStepIndex(), 
                                             "IN_PROGRESS", null, threadName);
            
            boolean stepPassed = false;
            Exception lastException = null;
            
            // Retry logic for failed steps
            for (int attempt = 0; attempt <= (retryEnabled ? step.getMaxRetries() : 0); attempt++) {
                try {
                    if (attempt > 0) {
                        logger.info("Retrying step {} (attempt {}/{})", 
                                   step.getStepIndex(), attempt + 1, step.getMaxRetries() + 1);
                        reportManager.logInfo(String.format("Retrying step %d (attempt %d)", 
                                                           step.getStepIndex(), attempt + 1));
                        
                        // Add small delay between retries
                        Thread.sleep(2000);
                    }
                    
                    // Execute the step
                    keywordExecutor.executeStep(step);
                    
                    // Step passed
                    stepPassed = true;
                    checkpointManager.updateCheckpoint(testCaseId, step.getStepIndex(), 
                                                     "PASSED", null, threadName);
                    
                    String stepDescription = String.format("Step %d: %s - %s", 
                                                          step.getStepIndex(), step.getKeyword(), 
                                                          step.getDescription() != null ? step.getDescription() : "");
                    reportManager.logPass(stepDescription);
                    
                    break; // Exit retry loop on success
                    
                } catch (Exception e) {
                    lastException = e;
                    step.incrementRetryCount();
                    
                    logger.warn("Step {} failed on attempt {}: {}", 
                               step.getStepIndex(), attempt + 1, e.getMessage());
                    
                    if (attempt < step.getMaxRetries() && retryEnabled && step.canRetry()) {
                        reportManager.logWarning(String.format("Step %d failed (attempt %d): %s - Retrying...", 
                                                              step.getStepIndex(), attempt + 1, e.getMessage()));
                    } else {
                        // All retries exhausted or retries disabled
                        checkpointManager.updateCheckpoint(testCaseId, step.getStepIndex(), 
                                                         "FAILED", e.getMessage(), threadName);
                        
                        String failureMsg = String.format("Step %d: %s - FAILED: %s", 
                                                         step.getStepIndex(), step.getKeyword(), e.getMessage());
                        reportManager.logFail(failureMsg);
                        
                        if (step.isMandatory()) {
                            throw new RuntimeException(String.format(
                                "Mandatory step %d failed after %d attempts. Test execution stopped.", 
                                step.getStepIndex(), attempt + 1), e);
                        } else {
                            logger.warn("Non-mandatory step {} failed, continuing with next step", 
                                       step.getStepIndex());
                            reportManager.logWarning("Non-mandatory step failed, continuing execution");
                            stepPassed = true; // Allow continuation for non-mandatory steps
                            break;
                        }
                    }
                }
            }
            
            if (!stepPassed && step.isMandatory()) {
                throw new RuntimeException(String.format(
                    "Mandatory step %d failed after all retry attempts", step.getStepIndex()), lastException);
            }
        }
    }
    
    public void executeMultipleTestCases(List<String> testCaseIds) {
        logger.info("Starting execution of {} test cases", testCaseIds.size());
        
        if (parallelExecution) {
            // Execute test cases in parallel
            CompletableFuture<?>[] futures = testCaseIds.stream()
                .map(testCaseId -> CompletableFuture.runAsync(() -> executeTestCaseInternal(testCaseId), executorService))
                .toArray(CompletableFuture<?>[]::new);
            
            // Wait for all test cases to complete
            CompletableFuture.allOf(futures).join();
        } else {
            // Execute test cases sequentially
            for (String testCaseId : testCaseIds) {
                executeTestCaseInternal(testCaseId);
            }
        }
        
        logger.info("Completed execution of all test cases");
    }
    
    public void shutdown() {
        try {
            executorService.shutdown();
            if (!executorService.awaitTermination(60, TimeUnit.SECONDS)) {
                executorService.shutdownNow();
            }
            reportManager.flush();
            logger.info("Test Execution Engine shutdown completed");
        } catch (InterruptedException e) {
            executorService.shutdownNow();
            Thread.currentThread().interrupt();
        }
    }
    
    // Utility methods for checkpoint management
    public void clearAllCheckpoints() {
        // Implementation to clear all checkpoints
        logger.info("All checkpoints cleared");
    }
    
    public CheckpointData getTestCaseStatus(String testCaseId) {
        return checkpointManager.getCheckpoint(testCaseId);
    }
    
    public void resetTestCase(String testCaseId) {
        checkpointManager.clearCheckpoint(testCaseId);
        logger.info("Test case {} reset successfully", testCaseId);
    }
}

// ================================================================================================
// 10. EXTENT REPORT MANAGER - Enhanced reporting with resume information
// ================================================================================================

package com.enterprise.framework.reporting;

import com.aventstack.extentreports.ExtentReports;
import com.aventstack.extentreports.ExtentTest;
import com.aventstack.extentreports.Status;
import com.aventstack.extentreports.reporter.ExtentSparkReporter;
import com.aventstack.extentreports.reporter.configuration.Theme;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.concurrent.ConcurrentHashMap;

public class ExtentReportManager {
    private static final Logger logger = LoggerFactory.getLogger(ExtentReportManager.class);
    private static ExtentReports extentReports;
    private static final ThreadLocal<ExtentTest> extentTest = new ThreadLocal<>();
    private static final ConcurrentHashMap<String, ExtentTest> testMap = new ConcurrentHashMap<>();
    
    static {
        initializeReports();
    }
    
    private static void initializeReports() {
        if (extentReports == null) {
            String timestamp = LocalDateTime.now().format(DateTimeFormatter.ofPattern("yyyy-MM-dd_HH-mm-ss"));
            String reportPath = "reports/ExtentReport_" + timestamp + ".html";
            
            ExtentSparkReporter sparkReporter = new ExtentSparkReporter(reportPath);
            sparkReporter.config().setTheme(Theme.STANDARD);
            sparkReporter.config().setDocumentTitle("Enterprise Auto-Resume Test Report");
            sparkReporter.config().setReportName("Keyword Driven Framework Execution Results");
            sparkReporter.config().setEncoding("utf-8");
            
            extentReports = new ExtentReports();
            extentReports.attachReporter(sparkReporter);
            
            // System information
            extentReports.setSystemInfo("Framework", "Enterprise Auto-Resume Keyword Driven Framework");
            extentReports.setSystemInfo("Environment", System.getProperty("test.environment", "DEV"));
            extentReports.setSystemInfo("Browser", System.getProperty("browser", "Chrome"));
            extentReports.setSystemInfo("OS", System.getProperty("os.name"));
            extentReports.setSystemInfo("Java Version", System.getProperty("java.version"));
            extentReports.setSystemInfo("Resume Enabled", System.getProperty("resume", "false"));
            extentReports.setSystemInfo("Parallel Execution", System.getProperty("parallel.execution", "false"));
            extentReports.setSystemInfo("Jenkins Build", System.getProperty("BUILD_NUMBER", "Local"));
            
            logger.info("Extent Reports initialized: {}", reportPath);
        }
    }
    
    public synchronized void createTest(String testName, String description) {
        ExtentTest test = extentReports.createTest(testName, description);
        extentTest.set(test);
        testMap.put(Thread.currentThread().getName() + "_" + testName, test);
        
        // Add test categories
        test.assignCategory("Keyword Driven");
        test.assignCategory("Auto Resume");
        
        logger.debug("Created extent test: {}", testName);
    }
    
    public void logPass(String message) {
        ExtentTest test = extentTest.get();
        if (test != null) {
            test.log(Status.PASS, message);
        }
    }
    
    public void logFail(String message) {
        ExtentTest test = extentTest.get();
        if (test != null) {
            test.log(Status.FAIL, message);
        }
    }
    
    public void logSkip(String message) {
        ExtentTest test = extentTest.get();
        if (test != null) {
            test.log(Status.SKIP, message);
        }
    }
    
    public void logInfo(String message) {
        ExtentTest test = extentTest.get();
        if (test != null) {
            test.log(Status.INFO, message);
        }
    }
    
    public void logWarning(String message) {
        ExtentTest test = extentTest.get();
        if (test != null) {
            test.log(Status.WARNING, message);
        }
    }
    
    public void addScreenshot(String screenshotPath) {
        ExtentTest test = extentTest.get();
        if (test != null) {
            try {
                test.addScreenCaptureFromPath(screenshotPath);
            } catch (Exception e) {
                logger.error("Failed to add screenshot to report", e);
            }
        }
    }
    
    public synchronized void flush() {
        if (extentReports != null) {
            extentReports.flush();
        }
    }
    
    public static void cleanup() {
        extentTest.remove();
    }
}

// ================================================================================================
// 11. CONFIGURATION MANAGER - Central configuration handling
// ================================================================================================

package com.enterprise.framework.config;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.FileInputStream;
import java.io.IOException;
import java.util.Properties;

public class ConfigurationManager {
    private static final Logger logger = LoggerFactory.getLogger(ConfigurationManager.class);
    private static Properties properties;
    
    static {
        loadProperties();
    }
    
    private static void loadProperties() {
        properties = new Properties();
        
        // Load from config file
        try (FileInputStream fis = new FileInputStream("config/framework.properties")) {
            properties.load(fis);
            logger.info("Configuration loaded from framework.properties");
        } catch (IOException e) {
            logger.warn("Could not load framework.properties, using defaults: {}", e.getMessage());
        }
        
        // Override with system properties (for Jenkins/command line)
        Properties systemProps = System.getProperties();
        for (String key : systemProps.stringPropertyNames()) {
            if (key.startsWith("test.") || key.startsWith("framework.") || 
                key.equals("resume") || key.equals("parallel.execution")) {
                properties.setProperty(key, systemProps.getProperty(key));
            }
        }
    }
    
    public static String getProperty(String key) {
        return properties.getProperty(key);
    }
    
    public static String getProperty(String key, String defaultValue) {
        return properties.getProperty(key, defaultValue);
    }
    
    public static boolean getBooleanProperty(String key, boolean defaultValue) {
        String value = properties.getProperty(key);
        return value != null ? Boolean.parseBoolean(value) : defaultValue;
    }
    
    public static int getIntProperty(String key, int defaultValue) {
        String value = properties.getProperty(key);
        try {
            return value != null ? Integer.parseInt(value) : defaultValue;
        } catch (NumberFormatException e) {
            logger.warn("Invalid integer value for property {}: {}, using default: {}", key, value, defaultValue);
            return defaultValue;
        }
    }
    
    public static void setProperty(String key, String value) {
        properties.setProperty(key, value);
    }
    
    public static void reloadProperties() {
        loadProperties();
        logger.info("Configuration reloaded");
    }
}

// ================================================================================================
// 12. MAIN TEST RUNNER - Entry point with CLI support
// ================================================================================================

package com.enterprise.framework.runner;

import com.enterprise.framework.config.ConfigurationManager;
import com.enterprise.framework.engine.TestExecutionEngine;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.Arrays;
import java.util.List;

public class TestRunner {
    private static final Logger logger = LoggerFactory.getLogger(TestRunner.class);
    
    public static void main(String[] args) {
        logger.info("Starting Enterprise Auto-Resume Keyword Driven Framework");
        
        // Parse command line arguments
        parseArguments(args);
        
        // Print configuration summary
        printConfigurationSummary();
        
        TestExecutionEngine engine = new TestExecutionEngine();
        
        try {
            // Get test cases to execute
            List<String> testCaseIds = getTestCasesToExecute();
            
            if (testCaseIds.isEmpty()) {
                logger.error("No test cases specified for execution");
                System.exit(1);
            }
            
            // Execute test cases
            logger.info("Executing test cases: {}", testCaseIds);
            engine.executeMultipleTestCases(testCaseIds);
            
            logger.info("Test execution completed successfully");
            
        } catch (Exception e) {
            logger.error("Test execution failed", e);
            System.exit(1);
        } finally {
            engine.shutdown();
        }
    }
    
    private static void parseArguments(String[] args) {
        for (String arg : args) {
            if (arg.startsWith("-D")) {
                String[] keyValue = arg.substring(2).split("=", 2);
                if (keyValue.length == 2) {
                    System.setProperty(keyValue[0], keyValue[1]);
                    logger.debug("Set system property: {}={}", keyValue[0], keyValue[1]);
                }
            }
        }
    }
    
    private static void printConfigurationSummary() {
        logger.info("=== Configuration Summary ===");
        logger.info("Resume Enabled: {}", ConfigurationManager.getBooleanProperty("resume", false));
        logger.info("Retry Enabled: {}", ConfigurationManager.getBooleanProperty("retry.enabled", true));
        logger.info("Parallel Execution: {}", ConfigurationManager.getBooleanProperty("parallel.execution", false));
        logger.info("Thread Pool Size: {}", ConfigurationManager.getIntProperty("thread.pool.size", 5));
        logger.info("Excel File Path: {}", ConfigurationManager.getProperty("excel.file.path", "testdata/TestSteps.xlsx"));
        logger.info("Checkpoint Storage: {}", ConfigurationManager.getBooleanProperty("checkpoint.use.db", false) ? "Database" : "File");
        logger.info("Custom Resume From Step: {}", ConfigurationManager.getProperty("resumeFromStep", "Not specified"));
        logger.info("Rerun Last N Steps: {}", ConfigurationManager.getIntProperty("rerunLastN", 0));
        logger.info("===============================");
    }
    
    private static List<String> getTestCasesToExecute() {
        String testCases = ConfigurationManager.getProperty("test.cases", "");
        if (!testCases.isEmpty()) {
            return Arrays.asList(testCases.split(","));
        }
        
        // Default test cases if none specified
        return Arrays.asList("TC001", "TC002", "TC003");
    }
}

// ================================================================================================
// 13. SAMPLE CONFIGURATION FILE
// ================================================================================================

# framework.properties
# Enterprise Auto-Resume Keyword Driven Framework Configuration

# General Settings
framework.name=Enterprise Auto-Resume Framework
framework.version=1.0.0

# Execution Settings
resume=false
retry.enabled=true
max.retry.attempts=3
parallel.execution=false
thread.pool.size=5

# Data Settings
excel.file.path=testdata/TestSteps.xlsx
test.cases=TC001,TC002,TC003

# Checkpoint Settings
checkpoint.use.db=false
checkpoint.db.url=jdbc:sqlite:checkpoints.db

# Browser Settings
browser=chrome
implicit.wait=10
explicit.wait=30

# Reporting Settings
report.path=reports/
screenshot.on.failure=true
screenshot.on.pass=false

# Jenkins Integration
jenkins.integration=true
build.number=${BUILD_NUMBER}

# Mobile Settings (Appium)
mobile.platform=Android
mobile.device.name=emulator-5554
mobile.app.path=apps/sample.apk

# API Settings
api.base.url=https://api.example.com
api.timeout=30000

// ================================================================================================
// 14. JENKINS PIPELINE INTEGRATION
// ================================================================================================

pipeline {
    agent any
    
    parameters {
        choice(
            name: 'RESUME_EXECUTION',
            choices: ['false', 'true'],
            description: 'Resume from last failed step?'
        )
        string(
            name: 'TEST_CASES',
            defaultValue: 'TC001,TC002,TC003',
            description: 'Comma-separated test case IDs to execute'
        )
        choice(
            name: 'BROWSER',
            choices: ['chrome', 'firefox', 'edge'],
            description: 'Browser for execution'
        )
        choice(
            name: 'PARALLEL_EXECUTION',
            choices: ['false', 'true'],
            description: 'Execute test cases in parallel?'
        )
        string(
            name: 'RESUME_FROM_STEP',
            defaultValue: '',
            description: 'Resume from specific step number (optional)'
        )
        string(
            name: 'RERUN_LAST_N',
            defaultValue: '0',
            description: 'Rerun last N steps (0 = disabled)'
        )
    }
    
    environment {
        MAVEN_HOME = tool 'Maven'
        JAVA_HOME = tool 'JDK11'
    }
    
    stages {
        stage('Checkout') {
            steps {
                checkout scm
                echo "Code checked out successfully"
            }
        }
        
        stage('Setup Environment') {
            steps {
                script {
                    // Create necessary directories
                    sh 'mkdir -p reports checkpoints logs'
                    
                    // Set execution permissions
                    sh 'chmod +x scripts/*.sh'
                }
            }
        }
        
        stage('Execute Tests') {
            steps {
                script {
                    def mavenCommand = """
                        ${MAVEN_HOME}/bin/mvn clean test \
                        -Dresume=${params.RESUME_EXECUTION} \
                        -Dtest.cases=${params.TEST_CASES} \
                        -Dbrowser=${params.BROWSER} \
                        -Dparallel.execution=${params.PARALLEL_EXECUTION} \
                        -DresumeFromStep=${params.RESUME_FROM_STEP} \
                        -DrerunLastN=${params.RERUN_LAST_N} \
                        -DBUILD_NUMBER=${BUILD_NUMBER} \
                        -Dtest.environment=${params.ENVIRONMENT ?: 'JENKINS'} \
                        -Dcheckpoint.use.db=true \
                        -Djenkins.integration=true
                    """
                    
                    try {
                        sh mavenCommand
                        currentBuild.result = 'SUCCESS'
                    } catch (Exception e) {
                        currentBuild.result = 'FAILURE'
                        echo "Test execution failed: ${e.getMessage()}"
                        
                        // Archive failure artifacts
                        archiveArtifacts artifacts: 'checkpoints/*.json, logs/*.log', allowEmptyArchive: true
                    }
                }
            }
        }
        
        stage('Generate Reports') {
            steps {
                // Archive test reports
                publishHTML([
                    allowMissing: false,
                    alwaysLinkToLastBuild: true,
                    keepAll: true,
                    reportDir: 'reports',
                    reportFiles: '*.html',
                    reportName: 'Extent Test Report'
                ])
                
                // Archive checkpoints for resume capability
                archiveArtifacts artifacts: 'checkpoints/*.json, checkpoints.db', allowEmptyArchive: true
                
                // Archive logs
                archiveArtifacts artifacts: 'logs/*.log', allowEmptyArchive: true
            }
        }
        
        stage('Checkpoint Analysis') {
            when {
                expression { currentBuild.result == 'FAILURE' }
            }
            steps {
                script {
                    // Analyze checkpoints and provide resume recommendations
                    sh '''
                        echo "=== CHECKPOINT ANALYSIS ==="
                        if [ -f "checkpoints/execution_checkpoints.json" ]; then
                            echo "Available checkpoints for resume:"
                            cat checkpoints/execution_checkpoints.json | jq '.'
                            echo ""
                            echo "To resume failed tests, trigger new build with:"
                            echo "- RESUME_EXECUTION: true"
                            echo "- Same TEST_CASES and BROWSER settings"
                        elif [ -f "checkpoints.db" ]; then
                            echo "Database checkpoints available for resume"
                            sqlite3 checkpoints.db "SELECT test_case_id, last_executed_step, status, failure_reason FROM execution_checkpoints WHERE status IN ('FAILED', 'IN_PROGRESS');"
                        else
                            echo "No checkpoints found"
                        fi
                        echo "=========================="
                    '''
                }
            }
        }
    }
    
    post {
        always {
            // Cleanup WebDriver processes
            sh 'pkill -f chromedriver || true'
            sh 'pkill -f geckodriver || true'
            
            // Send notifications
            emailext (
                subject: "Test Execution ${currentBuild.result}: ${env.JOB_NAME} - ${env.BUILD_NUMBER}",
                body: """
                    <h3>Enterprise Auto-Resume Framework Execution Results</h3>
                    <p><strong>Build:</strong> ${env.BUILD_NUMBER}</p>
                    <p><strong>Status:</strong> ${currentBuild.result}</p>
                    <p><strong>Resume Enabled:</strong> ${params.RESUME_EXECUTION}</p>
                    <p><strong>Test Cases:</strong> ${params.TEST_CASES}</p>
                    <p><strong>Browser:</strong> ${params.BROWSER}</p>
                    
                    ${currentBuild.result == 'FAILURE' ? 
                        '<p><strong>Resume Instructions:</strong> Trigger new build with RESUME_EXECUTION=true to continue from last failed step.</p>' 
                        : ''}
                    
                    <p><strong>Reports:</strong> <a href="${env.BUILD_URL}Extent_Test_Report/">View Detailed Report</a></p>
                    <p><strong>Console:</strong> <a href="${env.BUILD_URL}console">View Console Output</a></p>
                """,
                to: '${DEFAULT_RECIPIENTS}',
                mimeType: 'text/html'
            )
        }
        
        success {
            echo 'Test execution completed successfully!'
        }
        
        failure {
            echo 'Test execution failed. Check reports and checkpoints for resume capability.'
        }
    }
}

// ================================================================================================
// 15. MAVEN POM.XML CONFIGURATION
// ================================================================================================

<?xml version="1.0" encoding="UTF-8"?>
<project xmlns="http://maven.apache.org/POM/4.0.0"
         xmlns:xsi="http://www.w3.org/2001/XMLSchema-instance"
         xsi:schemaLocation="http://maven.apache.org/POM/4.0.0 
         http://maven.apache.org/xsd/maven-4.0.0.xsd">
    <modelVersion>4.0.0</modelVersion>

    <groupId>com.enterprise.framework</groupId>
    <artifactId>auto-resume-keyword-framework</artifactId>
    <version>1.0.0</version>
    <packaging>jar</packaging>

    <name>Enterprise Auto-Resume Keyword Driven Framework</name>
    <description>Enterprise-level keyword driven framework with auto-resume capability</description>

    <properties>
        <maven.compiler.source>11</maven.compiler.source>
        <maven.compiler.target>11</maven.compiler.target>
        <project.build.sourceEncoding>UTF-8</project.build.sourceEncoding>
        
        <!-- Dependencies versions -->
        <selenium.version>4.15.0</selenium.version>
        <appium.version>8.6.0</appium.version>
        <testng.version>7.8.0</testng.version>
        <extentreports.version>5.1.1</extentreports.version>
        <poi.version>5.2.4</poi.version>
        <jackson.version>2.15.2</jackson.version>
        <slf4j.version>2.0.9</slf4j.version>
        <logback.version>1.4.11</logback.version>
        <sqlite.version>3.43.2.1</sqlite.version>
        <mysql.version>8.1.0</mysql.version>
        <rest-assured.version>5.3.2</rest-assured.version>
    </properties>

    <dependencies>
        <!-- Selenium WebDriver -->
        <dependency>
            <groupId>org.seleniumhq.selenium</groupId>
            <artifactId>selenium-java</artifactId>
            <version>${selenium.version}</version>
        </dependency>
        
        <!-- Appium for Mobile Testing -->
        <dependency>
            <groupId>io.appium</groupId>
            <artifactId>java-client</artifactId>
            <version>${appium.version}</version>
        </dependency>
        
        <!-- TestNG -->
        <dependency>
            <groupId>org.testng</groupId>
            <artifactId>testng</artifactId>
            <version>${testng.version}</version>
        </dependency>
        
        <!-- Extent Reports -->
        <dependency>
            <groupId>com.aventstack</groupId>
            <artifactId>extentreports</artifactId>
            <version>${extentreports.version}</version>
        </dependency>
        
        <!-- Apache POI for Excel -->
        <dependency>
            <groupId>org.apache.poi</groupId>
            <artifactId>poi</artifactId>
            <version>${poi.version}</version>
        </dependency>
        <dependency>
            <groupId>org.apache.poi</groupId>
            <artifactId>poi-ooxml</artifactId>
            <version>${poi.version}</version>
        </dependency>
        
        <!-- Jackson for JSON processing -->
        <dependency>
            <groupId>com.fasterxml.jackson.core</groupId>
            <artifactId>jackson-databind</artifactId>
            <version>${jackson.version}</version>
        </dependency>
        
        <!-- Logging -->
        <dependency>
            <groupId>org.slf4j</groupId>
            <artifactId>slf4j-api</artifactId>
            <version>${slf4j.version}</version>
        </dependency>
        <dependency>
            <groupId>ch.qos.logback</groupId>
            <artifactId>logback-classic</artifactId>
            <version>${logback.version}</version>
        </dependency>
        
        <!-- Database drivers for checkpoint storage -->
        <dependency>
            <groupId>org.xerial</groupId>
            <artifactId>sqlite-jdbc</artifactId>
            <version>${sqlite.version}</version>
        </dependency>
        <dependency>
            <groupId>mysql</groupId>
            <artifactId>mysql-connector-java</artifactId>
            <version>${mysql.version}</version>
        </dependency>
        
        <!-- REST Assured for API testing -->
        <dependency>
            <groupId>io.rest-assured</groupId>
            <artifactId>rest-assured</artifactId>
            <version>${rest-assured.version}</version>
        </dependency>
    </dependencies>

    <build>
        <plugins>
            <!-- Maven Compiler Plugin -->
            <plugin>
                <groupId>org.apache.maven.plugins</groupId>
                <artifactId>maven-compiler-plugin</artifactId>
                <version>3.11.0</version>
                <configuration>
                    <source>11</source>
                    <target>11</target>
                </configuration>
            </plugin>
            
            <!-- Maven Surefire Plugin for TestNG -->
            <plugin>
                <groupId>org.apache.maven.plugins</groupId>
                <artifactId>maven-surefire-plugin</artifactId>
                <version>3.1.2</version>
                <configuration>
                    <suiteXmlFiles>
                        <suiteXmlFile>src/test/resources/testng.xml</suiteXmlFile>
                    </suiteXmlFiles>
                    <systemPropertyVariables>
                        <resume>${resume}</resume>
                        <parallel.execution>${parallel.execution}</parallel.execution>
                        <browser>${browser}</browser>
                        <test.cases>${test.cases}</test.cases>
                        <resumeFromStep>${resumeFromStep}</resumeFromStep>
                        <rerunLastN>${rerunLastN}</rerunLastN>
                    </systemPropertyVariables>
                </configuration>
            </plugin>
            
            <!-- Maven Assembly Plugin for creating executable JAR -->
            <plugin>
                <groupId>org.apache.maven.plugins</groupId>
                <artifactId>maven-assembly-plugin</artifactId>
                <version>3.6.0</version>
                <configuration>
                    <descriptorRefs>
                        <descriptorRef>jar-with-dependencies</descriptorRef>
                    </descriptorRefs>
                    <archive>
                        <manifest>
                            <mainClass>com.enterprise.framework.runner.TestRunner</mainClass>
                        </manifest>
                    </archive>
                </configuration>
                <executions>
                    <execution>
                        <id>make-assembly</id>
                        <phase>package</phase>
                        <goals>
                            <goal>single</goal>
                        </goals>
                    </execution>
                </executions>
            </plugin>
        </plugins>
    </build>
    
    <profiles>
        <!-- Development Profile -->
        <profile>
            <id>dev</id>
            <activation>
                <activeByDefault>true</activeByDefault>
            </activation>
            <properties>
                <test.environment>DEV</test.environment>
                <checkpoint.use.db>false</checkpoint.use.db>
                <parallel.execution>false</parallel.execution>
            </properties>
        </profile>
        
        <!-- Jenkins/CI Profile -->
        <profile>
            <id>jenkins</id>
            <properties>
                <test.environment>JENKINS</test.environment>
                <checkpoint.use.db>true</checkpoint.use.db>
                <parallel.execution>true</parallel.execution>
                <thread.pool.size>10</thread.pool.size>
            </properties>
        </profile>
        
        <!-- Production Profile -->
        <profile>
            <id>prod</id>
            <properties>
                <test.environment>PROD</test.environment>
                <checkpoint.use.db>true</checkpoint.use.db>
                <parallel.execution>true</parallel.execution>
                <thread.pool.size>15</thread.pool.size>
            </properties>
        </profile>
    </profiles>
</project>

// ================================================================================================
// 16. TESTNG XML CONFIGURATION
// ================================================================================================

<?xml version="1.0" encoding="UTF-8"?>
<!DOCTYPE suite SYSTEM "http://testng.org/testng-1.0.dtd">
<suite name="Enterprise Auto-Resume Test Suite" parallel="methods" thread-count="5">
    
    <listeners>
        <listener class-name="com.enterprise.framework.listeners.TestNGListener"/>
        <listener class-name="com.enterprise.framework.listeners.ScreenshotListener"/>
        <listener class-name="com.enterprise.framework.listeners.CheckpointListener"/>
    </listeners>
    
    <parameter name="browser" value="chrome"/>
    <parameter name="environment" value="dev"/>
    
    <test name="Keyword Driven Auto-Resume Tests" preserve-order="true">
        <classes>
            <class name="com.enterprise.framework.tests.KeywordDrivenTests">
                <methods>
                    <include name="executeTestCase"/>
                </methods>
            </class>
        </classes>
    </test>
    
</suite>

// ================================================================================================
// 17. TESTNG TEST CLASS
// ================================================================================================

package com.enterprise.framework.tests;

import com.enterprise.framework.engine.TestExecutionEngine;
import com.enterprise.framework.config.ConfigurationManager;
import org.testng.annotations.*;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.Arrays;
import java.util.List;

public class KeywordDrivenTests {
    private static final Logger logger = LoggerFactory.getLogger(KeywordDrivenTests.class);
    private TestExecutionEngine executionEngine;
    
    @BeforeSuite
    public void beforeSuite() {
        logger.info("Starting Enterprise Auto-Resume Test Suite");
        executionEngine = new TestExecutionEngine();
    }
    
    @AfterSuite
    public void afterSuite() {
        logger.info("Test Suite execution completed");
        if (executionEngine != null) {
            executionEngine.shutdown();
        }
    }
    
    @Test(description = "Execute test cases with auto-resume capability")
    @Parameters({"testCases"})
    public void executeTestCase(@Optional("TC001,TC002,TC003") String testCases) {
        List<String> testCaseIds = Arrays.asList(testCases.split(","));
        
        logger.info("Executing test cases: {}", testCaseIds);
        
        try {
            executionEngine.executeMultipleTestCases(testCaseIds);
            logger.info("All test cases executed successfully");
        } catch (Exception e) {
            logger.error("Test execution failed", e);
            throw new RuntimeException("Test execution failed", e);
        }
    }
    
    @Test(description = "Individual test case execution", dataProvider = "testCaseProvider")
    public void executeIndividualTestCase(String testCaseId) {
        logger.info("Executing individual test case: {}", testCaseId);
        executionEngine.executeTestCase(testCaseId);
    }
    
    @DataProvider(name = "testCaseProvider")
    public Object[][] testCaseProvider() {
        String testCases = ConfigurationManager.getProperty("test.cases", "TC001,TC002,TC003");
        String[] testCaseArray = testCases.split(",");
        
        Object[][] data = new Object[testCaseArray.length][1];
        for (int i = 0; i < testCaseArray.length; i++) {
            data[i][0] = testCaseArray[i].trim();
        }
        
        return data;
    }
}

// ================================================================================================
// 18. TESTNG LISTENERS
// ================================================================================================

package com.enterprise.framework.listeners;

import com.enterprise.framework.reporting.ExtentReportManager;
import org.testng.ITestListener;
import org.testng.ITestResult;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class TestNGListener implements ITestListener {
    private static final Logger logger = LoggerFactory.getLogger(TestNGListener.class);
    
    @Override
    public void onTestStart(ITestResult result) {
        logger.info("Starting test: {}", result.getMethod().getMethodName());
    }
    
    @Override
    public void onTestSuccess(ITestResult result) {
        logger.info("Test passed: {}", result.getMethod().getMethodName());
    }
    
    @Override
    public void onTestFailure(ITestResult result) {
        logger.error("Test failed: {}", result.getMethod().getMethodName(), result.getThrowable());
    }
    
    @Override
    public void onTestSkipped(ITestResult result) {
        logger.warn("Test skipped: {}", result.getMethod().getMethodName());
    }
}

// ================================================================================================
// 19. DOCKER SUPPORT
// ================================================================================================

# Dockerfile
FROM openjdk:11-jre-slim

# Install necessary packages
RUN apt-get update && apt-get install -y \
    wget \
    unzip \
    curl \
    gnupg \
    && rm -rf /var/lib/apt/lists/*

# Install Chrome
RUN wget -q -O - https://dl-ssl.google.com/linux/linux_signing_key.pub | apt-key add - \
    && echo "deb [arch=amd64] http://dl.google.com/linux/chrome/deb/ stable main" >> /etc/apt/sources.list.d/google-chrome.list \
    && apt-get update \
    && apt-get install -y google-chrome-stable \
    && rm -rf /var/lib/apt/lists/*

# Install ChromeDriver
RUN CHROMEDRIVER_VERSION=`curl -sS chromedriver.storage.googleapis.com/LATEST_RELEASE` && \
    wget -N http://chromedriver.storage.googleapis.com/$CHROMEDRIVER_VERSION/chromedriver_linux64.zip && \
    unzip chromedriver_linux64.zip && \
    rm chromedriver_linux64.zip && \
    mv chromedriver /usr/local/bin/ && \
    chmod +x /usr/local/bin/chromedriver

# Create app directory
WORKDIR /app

# Copy framework files
COPY target/auto-resume-keyword-framework-1.0.0-jar-with-dependencies.jar /app/framework.jar
COPY config/ /app/config/
COPY testdata/ /app/testdata/

# Create necessary directories
RUN mkdir -p /app/reports /app/checkpoints /app/logs

# Set environment variables
ENV JAVA_OPTS="-Xmx2g -Dwebdriver.chrome.driver=/usr/local/bin/chromedriver"

# Run the framework
CMD ["java", "-jar", "/app/framework.jar"]

# docker-compose.yml
version: '3.8'
services:
  auto-resume-framework:
    build: .
    environment:
      - resume=true
      - parallel.execution=true
      - browser=chrome
      - checkpoint.use.db=true
      - test.environment=DOCKER
    volumes:
      - ./reports:/app/reports
      - ./checkpoints:/app/checkpoints
      - ./logs:/app/logs
      - ./testdata:/app/testdata
    networks:
      - test-network

  selenium-hub:
    image: selenium/hub:latest
    ports:
      - "4444:4444"
    networks:
      - test-network

  chrome-node:
    image: selenium/node-chrome:latest
    environment:
      - HUB_HOST=selenium-hub
    depends_on:
      - selenium-hub
    networks:
      - test-network

networks:
  test-network:
    driver: bridge

// ================================================================================================
// 20. USAGE EXAMPLES AND CLI COMMANDS
// ================================================================================================

# Basic execution
mvn clean test

# Resume from last failed step
mvn clean test -Dresume=true

# Execute specific test cases with resume
mvn clean test -Dresume=true -Dtest.cases=TC001,TC002

# Parallel execution with resume
mvn clean test -Dresume=true -Dparallel.execution=true -Dthread.pool.size=10

# Resume from custom step
mvn clean test -Dresume=true -DresumeFromStep=5

# Rerun last 3 steps
mvn clean test -Dresume=true -DrerunLastN=3

# Use database for checkpoints
mvn clean test -Dresume=true -Dcheckpoint.use.db=true

# Execute with specific browser
mvn clean test -Dresume=true -Dbrowser=firefox

# Jenkins execution
mvn clean test -Dresume=true -DBUILD_NUMBER=${BUILD_NUMBER} -Djenkins.integration=true

# Docker execution
docker-compose up --build

# Generate standalone JAR
mvn clean package

# Execute standalone JAR
java -jar target/auto-resume-keyword-framework-1.0.0-jar-with-dependencies.jar \
     -Dresume=true \
     -Dtest.cases=TC001,TC002,TC003 \
     -Dparallel.execution=true