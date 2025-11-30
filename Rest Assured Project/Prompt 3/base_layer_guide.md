# PART 3: Base Layer Implementation Guide

## Overview
The base layer (BaseClient and BaseTest) provides the foundation for all API clients and test classes in the banking microservices automation framework.

---

## File Structure

```
src/
├── main/java/com/harish/api/framework/
│   └── base/
│       ├── BaseClient.java      ← All API clients extend this
│       └── BaseTest.java        ← All test classes extend this
├── test/resources/
│   └── log4j2.xml              ← Logging configuration
```

---

## 1. BaseClient.java

### Purpose
- Parent class for all REST API service clients (e.g., AccountServiceClient, PaymentServiceClient)
- Encapsulates common HTTP request setup and configuration
- Provides factory methods for creating clean RequestSpecification instances

### Key Responsibilities

#### Constructor
```java
public BaseClient() {
    this.config = EnvironmentManager.getConfig();
    this.baseRequestSpec = buildBaseRequestSpec();
}
```
- Initializes `FrameworkConfig` from EnvironmentManager (singleton)
- Builds base `RequestSpecification` with default headers, timeouts, and URI

#### givenBaseRequest() Method
```java
protected RequestSpecification givenBaseRequest() {
    return new RequestSpecBuilder()
            .addRequestSpecification(baseRequestSpec)
            .build();
}
```
- **Critical**: Returns a **fresh copy** of base RequestSpec
- Prevents state contamination between requests (path params, query params, headers)
- Should be called at the start of every API call in child classes

#### Helper Methods
- `givenAuthenticatedRequest(token)` - adds Bearer token header
- `givenRequestWithHeader(name, value)` - adds custom headers

### SOLID Principles Applied

| Principle | Application |
|-----------|-------------|
| **Single Responsibility** | BaseClient only manages HTTP request setup |
| **Open/Closed** | Subclasses extend without modifying BaseClient |
| **Liskov Substitution** | All service clients follow same interface contract |
| **Interface Segregation** | givenBaseRequest() provides minimal necessary interface |
| **Dependency Inversion** | Depends on FrameworkConfig abstraction (not hardcoded URLs) |

### Usage Example in Child Class

```java
public class AccountServiceClient extends BaseClient {
    
    public Account getAccountById(String accountId) {
        logger.info("Fetching account: {}", accountId);
        
        // Always use givenBaseRequest() to get fresh spec
        return givenBaseRequest()
                .pathParam("id", accountId)
                .when()
                .get("/api/accounts/{id}")
                .then()
                .statusCode(200)
                .extract()
                .as(Account.class);  // Strongly typed response
    }
    
    public void transferFunds(String fromAccountId, String toAccountId, BigDecimal amount) {
        TransferRequest request = new TransferRequest(fromAccountId, toAccountId, amount);
        
        // Using authenticated request
        givenAuthenticatedRequest(getAuthToken())
                .body(request)
                .when()
                .post("/api/transfers")
                .then()
                .statusCode(201);
    }
}
```

### Configuration Dependency

BaseClient expects `FrameworkConfig` to provide:
- `getBaseUrl()` - Base URI for API
- `getConnectTimeout()` - Connection timeout in milliseconds
- `getReadTimeout()` - Read timeout in milliseconds

### TODO Comments in Code
```java
// TODO: In production, retrieve tokens from secure credential manager 
// (Vault, Secrets Manager, AWS Secrets Manager)
protected RequestSpecification givenAuthenticatedRequest(String token) { ... }
```

---

## 2. BaseTest.java

### Purpose
- Parent class for all TestNG test classes
- Manages test lifecycle: initialization, execution, reporting
- Integrates ExtentReports for HTML reporting
- Provides logging hooks for each test

### Key Responsibilities

#### Lifecycle Annotations

| Annotation | Level | Purpose |
|-----------|-------|---------|
| `@BeforeSuite` | Suite | Initialize ExtentReports (once per run) |
| `@BeforeMethod` | Test | Create ExtentTest node, log start |
| `@AfterMethod` | Test | Capture result, attach logs, screenshot (if applicable) |
| `@AfterSuite` | Suite | Flush reports, close resources |

#### BeforeSuite (initializeReporting)
```java
@BeforeSuite(alwaysRun = true)
public void initializeReporting() {
    // 1. Create target/extent-reports/ directory
    // 2. Generate timestamped report file
    // 3. Initialize ExtentReports with Spark Reporter
    // 4. Set system information (OS, Java, Environment)
}
```

**Important**: `alwaysRun = true` ensures this runs even if tests are skipped/fail.

#### BeforeMethod (setup)
```java
@BeforeMethod(alwaysRun = true)
public void setup() {
    // 1. Capture test method name
    // 2. Create ExtentTest node for current test
    // 3. Log test start time
}
```

#### AfterMethod (teardown)
```java
@AfterMethod(alwaysRun = true)
public void teardown(ITestResult result) {
    // 1. Check test status: PASS / FAIL / SKIP
    // 2. Attach status to ExtentTest
    // 3. Log execution duration
    // 4. For FAIL: attach exception details
    // 5. For UI tests: capture screenshot and attach
}
```

#### AfterSuite (flushReports)
```java
@AfterSuite(alwaysRun = true)
public void flushReports() {
    // 1. Flush ExtentReports
    // 2. Generate final HTML report
    // 3. Close all resources
}
```

### TODO Comments in Code
```java
// TODO: In a UI automation framework, this is where you would:
//   - Capture screenshots on failure
//   - Attach network logs
//   - Collect performance metrics
```

For API tests, this section can remain as-is (no screenshots needed).

### How to Extend BaseTest in Test Classes

#### Basic Template
```java
public class AccountServiceTests extends BaseTest {
    
    private AccountServiceClient accountClient;
    
    @BeforeMethod
    @Override
    public void setup() {
        super.setup();  // Call parent setup (initializes logging/reporting)
        accountClient = new AccountServiceClient();
    }
    
    @Test(description = "Verify account can be fetched by ID")
    public void testGetAccountById() {
        logInfo("Starting test: testGetAccountById");
        
        // Test logic
        Account account = accountClient.getAccountById("ACC123");
        
        // Assertions
        assert account.getId().equals("ACC123");
        assert account.getStatus().equals("ACTIVE");
        
        logInfo("Test: testGetAccountById - PASSED");
    }
    
    @Test(description = "Verify account transfer succeeds")
    public void testTransferFunds() {
        logInfo("Starting test: testTransferFunds");
        
        accountClient.transferFunds("ACC001", "ACC002", new BigDecimal("1000.00"));
        
        logInfo("Transfer completed successfully");
    }
}
```

#### Using logInfo, logWarning, logError Methods
```java
@Test(description = "Test with custom logging")
public void testWithLogging() {
    logInfo("This goes to console, file, AND Extent Report");
    
    try {
        // Test logic
        Account account = accountClient.getAccountById("ACC123");
    } catch (Exception e) {
        logError("Test failed: " + e.getMessage());
        throw e;
    }
    
    logWarning("Warning: Account balance is low");
}
```

### Reporting Output

After test execution, reports are generated at:
```
target/extent-reports/TestReport_YYYY_MM_DD_HH_MM_SS.html
```

The HTML report includes:
- Test name and description
- Execution duration
- Pass/Fail/Skip status
- Exception stack traces (on failure)
- All log entries (via logInfo, logWarning, logError)
- System information (OS, Java version, Environment)

---

## 3. log4j2.xml Configuration

### Location
```
src/main/resources/log4j2.xml
```

### Appenders Configured

| Name | Purpose | Output | Level |
|------|---------|--------|-------|
| **ConsoleAppender** | Terminal output | System.out | INFO and above |
| **FileAppender** | General logs | `target/logs/application.log` | DEBUG and above |
| **AsyncFileAppender** | Non-blocking file write | Uses FileAppender | DEBUG+ |
| **ErrorAppender** | Error-specific logs | `target/logs/error.log` | ERROR/FATAL only |
| **TestAppender** | Test execution logs | `target/logs/test-execution.log` | DEBUG+ |

### Logger Configuration

#### Framework Loggers (HIGH detail)
```xml
<Logger name="com.harish.api.framework" level="DEBUG">
    <AppenderRef ref="ConsoleAppender"/>
    <AppenderRef ref="AsyncFileAppender"/>
    <AppenderRef ref="TestAppender"/>
</Logger>
```
- Logs from `com.harish.api.framework.*` are captured at DEBUG level
- Routed to console, async file, and test appender

#### REST Assured Logger (API call tracing)
```xml
<Logger name="io.restassured" level="DEBUG">
    <AppenderRef ref="FileAppender"/>
</Logger>
```
- Captures HTTP request/response details

#### Test Logger
```xml
<Logger name="com.harish.api.tests" level="INFO">
    <AppenderRef ref="ConsoleAppender"/>
    <AppenderRef ref="TestAppender"/>
</AppenderRef>
```

#### Suppressed Loggers (reduce noise)
```xml
<Logger name="org.apache.http" level="WARN"/>
<Logger name="org.apache.commons" level="WARN"/>
<Logger name="com.aventstack.extentreports" level="INFO"/>
```

### Rolling Policy

Files roll over based on:
1. **Time-based**: Daily (modulate="true" aligns to day boundary)
2. **Size-based**: When file exceeds 10 MB (FileAppender) or 20 MB (TestAppender)

### Backup Retention
- General logs: Keep 30 backup files
- Error logs: Keep 20 backup files
- Test logs: Keep 10 backup files

### Log Output Example

**Console Output:**
```
14:32:01 INFO  com.harish.api.framework.base.BaseClient - BaseClient initialized with baseUrl: http://localhost:8080
14:32:02 INFO  com.harish.api.framework.base.BaseTest - ========== TEST START: testGetAccountById ==========
14:32:03 INFO  com.harish.api.framework.client.AccountServiceClient - Fetching account: ACC123
14:32:04 INFO  com.harish.api.framework.base.BaseTest - ========== TEST PASSED: testGetAccountById (1234ms) ==========
```

**File Output (application.log):**
```
2024-01-15 14:32:01.123 [main] INFO  com.harish.api.framework.base.BaseClient - BaseClient initialized with baseUrl: http://localhost:8080
2024-01-15 14:32:02.456 [main] INFO  com.harish.api.framework.base.BaseTest - ========== TEST START: testGetAccountById ==========
2024-01-15 14:32:03.789 [main] INFO  com.harish.api.framework.client.AccountServiceClient - Fetching account: ACC123
2024-01-15 14:32:04.012 [main] DEBUG io.restassured.internal.RequestSpecificationImpl - Sending request: GET http://localhost:8080/api/accounts/ACC123
2024-01-15 14:32:04.567 [main] INFO  com.harish.api.framework.base.BaseTest - ========== TEST PASSED: testGetAccountById (1234ms) ==========
```

---

## 4. Maven Dependencies

Add to `pom.xml`:

```xml
<!-- REST Assured -->
<dependency>
    <groupId>io.rest-assured</groupId>
    <artifactId>rest-assured</artifactId>
    <version>5.4.0</version>
    <scope>test</scope>
</dependency>

<!-- TestNG -->
<dependency>
    <groupId>org.testng</groupId>
    <artifactId>testng</artifactId>
    <version>7.8.1</version>
    <scope>test</scope>
</dependency>

<!-- Extent Reports -->
<dependency>
    <groupId>com.aventstack</groupId>
    <artifactId>extentreports</artifactId>
    <version>5.1.1</version>
</dependency>

<!-- Log4j2 -->
<dependency>
    <groupId>org.apache.logging.log4j</groupId>
    <artifactId>log4j-core</artifactId>
    <version>2.21.1</version>
</dependency>

<dependency>
    <groupId>org.apache.logging.log4j</groupId>
    <artifactId>log4j-api</artifactId>
    <version>2.21.1</version>
</dependency>
```

---

## 5. Design Patterns Applied

### 1. **Singleton Pattern** (FrameworkConfig via EnvironmentManager)
- Single instance of configuration per JVM
- EnvironmentManager loads and caches config

### 2. **Factory Pattern** (RequestSpecification creation)
- `givenBaseRequest()` acts as factory method
- Returns configured instances without exposing construction logic

### 3. **Template Method Pattern** (BaseTest lifecycle)
- `@BeforeSuite`, `@AfterSuite` define algorithm structure
- Subclasses can override specific lifecycle methods

### 4. **Strategy Pattern** (Logger implementations)
- Log4j2 strategies: Console, File, Async, Rolling
- Selected via XML configuration

### 5. **Builder Pattern** (RequestSpecBuilder)
- Fluent API for constructing RequestSpecification
- Chainable configuration methods

---

## 6. Next Steps (PART 4)

The next part will build upon this base layer:
- **Custom Response Models** (POJOs with immutability)
- **Service Client Implementations** (AccountServiceClient, PaymentServiceClient)
- **Test Example Classes**

All these will extend BaseClient and BaseTest.

---

## Checklist Before Moving to Part 4

- [ ] BaseClient.java added to `src/main/java/com/harish/api/framework/base/`
- [ ] BaseTest.java added to `src/main/java/com/harish/api/framework/base/`
- [ ] log4j2.xml added to `src/main/resources/`
- [ ] Maven dependencies added to pom.xml
- [ ] FrameworkConfig and EnvironmentManager from PART 2 are available
- [ ] Build with `mvn clean compile` succeeds
- [ ] No compilation errors

---

## Key Takeaways

1. **BaseClient**: Use `givenBaseRequest()` for every API call to prevent state contamination
2. **BaseTest**: Always call `super.setup()` in child test classes to initialize logging
3. **Logging**: Three levels of logging - console, file, and Extent Report
4. **Immutability**: Response POJOs (coming in PART 4) should be immutable for thread safety
5. **Configuration**: All URLs, timeouts, and credentials come from FrameworkConfig (never hardcoded)
