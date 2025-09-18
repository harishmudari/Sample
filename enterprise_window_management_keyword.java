package com.automation.keywords;

import org.openqa.selenium.WebDriver;
import org.openqa.selenium.WebDriverException;
import org.openqa.selenium.Dimension;
import org.openqa.selenium.Point;
import org.openqa.selenium.JavascriptExecutor;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import java.util.Properties;
import java.io.InputStream;
import java.io.IOException;
import java.awt.Toolkit;

/**
 * Enterprise-level Window Management Keyword
 * Handles all browser window operations including maximize, minimize, fullscreen,
 * resize, positioning with comprehensive error handling and fail-safe design.
 * 
 * @author QA Automation Team
 * @version 1.0
 * @since 2025-08-09
 */

public class WindowManagementKeyword {
    
    private static final Logger log = LoggerFactory.getLogger(WindowManagementKeyword.class);
    private final WebDriver driver;
    private final JavascriptExecutor jsExecutor;
    private final Properties config;
    private final ScreenshotHelper screenshotHelper;
    
    // Configuration constants loaded from properties
    private final int DEFAULT_TIMEOUT;
    private final int RETRY_COUNT;
    private final int OPERATION_PAUSE_MS;
    private final boolean ENABLE_SCREENSHOTS;
    private final boolean VERIFY_OPERATIONS;
    private final int DEFAULT_WINDOW_WIDTH;
    private final int DEFAULT_WINDOW_HEIGHT;
    
    // Window state tracking
    private WindowState previousState;
    private Dimension previousSize;
    private Point previousPosition;
    
    /**
     * Constructor - Initializes the window management keyword with driver and configuration
     * 
     * @param driver WebDriver instance (should be ThreadLocal managed)
     * @param screenshotHelper Helper for capturing screenshots on failure
     * @throws WindowManagementException if initialization fails
     */
    public WindowManagementKeyword(WebDriver driver, ScreenshotHelper screenshotHelper) {
        long startTime = System.currentTimeMillis();
        
        // Fail-fast validation
        if (driver == null) {
            throw new WindowManagementException("WebDriver cannot be null");
        }
        if (screenshotHelper == null) {
            throw new WindowManagementException("ScreenshotHelper cannot be null");
        }
        
        this.driver = driver;
        this.jsExecutor = (JavascriptExecutor) driver;
        this.screenshotHelper = screenshotHelper;
        this.config = loadConfiguration();
        
        // Load configuration with defaults
        this.DEFAULT_TIMEOUT = Integer.parseInt(config.getProperty("window.timeout", "10"));
        this.RETRY_COUNT = Integer.parseInt(config.getProperty("window.retry.count", "3"));
        this.OPERATION_PAUSE_MS = Integer.parseInt(config.getProperty("window.operation.pause.ms", "500"));
        this.ENABLE_SCREENSHOTS = Boolean.parseBoolean(config.getProperty("screenshots.enabled", "true"));
        this.VERIFY_OPERATIONS = Boolean.parseBoolean(config.getProperty("window.verify.operations", "true"));
        this.DEFAULT_WINDOW_WIDTH = Integer.parseInt(config.getProperty("window.default.width", "1920"));
        this.DEFAULT_WINDOW_HEIGHT = Integer.parseInt(config.getProperty("window.default.height", "1080"));
        
        // Initialize window state tracking
        this.previousState = getCurrentWindowState();
        this.previousSize = getCurrentWindowSize();
        this.previousPosition = getCurrentWindowPosition();
        
        long endTime = System.currentTimeMillis();
        log.info("WindowManagementKeyword initialized successfully in {}ms", (endTime - startTime));
    }
    
    /**
     * Maximize browser window
     * Single Responsibility: Maximize the browser window to full screen
     * 
     * @return WindowResult containing success status and details
     */
    public WindowResult maximizeWindow() {
        String methodName = "maximizeWindow";
        long startTime = System.currentTimeMillis();
        
        log.info("{} - Starting window maximization", methodName);
        
        try {
            // Store current state for potential rollback
            storeCurrentWindowState();
            
            // Perform maximize operation with retry
            WindowResult result = performMaximizeWithRetry();
            
            long endTime = System.currentTimeMillis();
            log.info("{} - Completed in {}ms with result: {}", methodName, (endTime - startTime), result.getStatus());
            
            return result;
            
        } catch (WindowManagementException e) {
            return handleWindowException(methodName, "maximize", e);
        } catch (Exception e) {
            return handleUnexpectedException(methodName, "maximize", e);
        }
    }
    
    /**
     * Minimize browser window
     * Single Responsibility: Minimize the browser window
     * 
     * @return WindowResult containing success status and details
     */
    public WindowResult minimizeWindow() {
        String methodName = "minimizeWindow";
        long startTime = System.currentTimeMillis();
        
        log.info("{} - Starting window minimization", methodName);
        
        try {
            // Store current state for potential rollback
            storeCurrentWindowState();
            
            // Perform minimize operation with retry
            WindowResult result = performMinimizeWithRetry();
            
            long endTime = System.currentTimeMillis();
            log.info("{} - Completed in {}ms with result: {}", methodName, (endTime - startTime), result.getStatus());
            
            return result;
            
        } catch (WindowManagementException e) {
            return handleWindowException(methodName, "minimize", e);
        } catch (Exception e) {
            return handleUnexpectedException(methodName, "minimize", e);
        }
    }
    
    /**
     * Set browser window to fullscreen mode
     * Single Responsibility: Enter fullscreen mode (F11 equivalent)
     * 
     * @return WindowResult containing success status and details
     */
    public WindowResult setFullscreen() {
        String methodName = "setFullscreen";
        long startTime = System.currentTimeMillis();
        
        log.info("{} - Starting fullscreen mode", methodName);
        
        try {
            // Store current state for potential rollback
            storeCurrentWindowState();
            
            // Perform fullscreen operation with retry
            WindowResult result = performFullscreenWithRetry();
            
            long endTime = System.currentTimeMillis();
            log.info("{} - Completed in {}ms with result: {}", methodName, (endTime - startTime), result.getStatus());
            
            return result;
            
        } catch (WindowManagementException e) {
            return handleWindowException(methodName, "fullscreen", e);
        } catch (Exception e) {
            return handleUnexpectedException(methodName, "fullscreen", e);
        }
    }
    
    /**
     * Exit fullscreen mode
     * Single Responsibility: Exit fullscreen mode and restore normal window
     * 
     * @return WindowResult containing success status and details
     */
    public WindowResult exitFullscreen() {
        String methodName = "exitFullscreen";
        long startTime = System.currentTimeMillis();
        
        log.info("{} - Exiting fullscreen mode", methodName);
        
        try {
            // Perform exit fullscreen operation with retry
            WindowResult result = performExitFullscreenWithRetry();
            
            long endTime = System.currentTimeMillis();
            log.info("{} - Completed in {}ms with result: {}", methodName, (endTime - startTime), result.getStatus());
            
            return result;
            
        } catch (WindowManagementException e) {
            return handleWindowException(methodName, "exitFullscreen", e);
        } catch (Exception e) {
            return handleUnexpectedException(methodName, "exitFullscreen", e);
        }
    }
    
    /**
     * Resize window to specific dimensions
     * Single Responsibility: Set window to exact width and height
     * 
     * @param width Window width in pixels
     * @param height Window height in pixels
     * @return WindowResult containing success status and details
     */
    public WindowResult resizeWindow(int width, int height) {
        String methodName = "resizeWindow";
        long startTime = System.currentTimeMillis();
        
        log.info("{} - Resizing window to {}x{}", methodName, width, height);
        
        // Fail-fast validation
        if (width <= 0 || height <= 0) {
            String errorMsg = "Window dimensions must be positive values. Width: " + width + ", Height: " + height;
            log.error("{} - {}", methodName, errorMsg);
            return WindowResult.failure(errorMsg);
        }
        
        try {
            // Store current state for potential rollback
            storeCurrentWindowState();
            
            // Validate dimensions against screen size
            if (!isValidWindowSize(width, height)) {
                String errorMsg = String.format("Window size %dx%d exceeds screen dimensions", width, height);
                log.warn("{} - {}", methodName, errorMsg);
                // Continue with warning, let WebDriver handle it
            }
            
            // Perform resize operation with retry
            WindowResult result = performResizeWithRetry(width, height);
            
            long endTime = System.currentTimeMillis();
            log.info("{} - Completed in {}ms with result: {}", methodName, (endTime - startTime), result.getStatus());
            
            return result;
            
        } catch (WindowManagementException e) {
            return handleWindowException(methodName, String.format("resize to %dx%d", width, height), e);
        } catch (Exception e) {
            return handleUnexpectedException(methodName, String.format("resize to %dx%d", width, height), e);
        }
    }
    
    /**
     * Position window at specific coordinates
     * Single Responsibility: Move window to exact x,y position
     * 
     * @param x X coordinate (left edge)
     * @param y Y coordinate (top edge)
     * @return WindowResult containing success status and details
     */
    public WindowResult positionWindow(int x, int y) {
        String methodName = "positionWindow";
        long startTime = System.currentTimeMillis();
        
        log.info("{} - Positioning window at ({}, {})", methodName, x, y);
        
        try {
            // Store current state for potential rollback
            storeCurrentWindowState();
            
            // Perform position operation with retry
            WindowResult result = performPositionWithRetry(x, y);
            
            long endTime = System.currentTimeMillis();
            log.info("{} - Completed in {}ms with result: {}", methodName, (endTime - startTime), result.getStatus());
            
            return result;
            
        } catch (WindowManagementException e) {
            return handleWindowException(methodName, String.format("position to (%d, %d)", x, y), e);
        } catch (Exception e) {
            return handleUnexpectedException(methodName, String.format("position to (%d, %d)", x, y), e);
        }
    }
    
    /**
     * Set window to default size from configuration
     * Single Responsibility: Reset window to configured default dimensions
     * 
     * @return WindowResult containing success status and details
     */
    public WindowResult setDefaultWindowSize() {
        String methodName = "setDefaultWindowSize";
        log.info("{} - Setting window to default size: {}x{}", methodName, DEFAULT_WINDOW_WIDTH, DEFAULT_WINDOW_HEIGHT);
        
        return resizeWindow(DEFAULT_WINDOW_WIDTH, DEFAULT_WINDOW_HEIGHT);
    }
    
    /**
     * Center window on screen
     * Single Responsibility: Position window in the center of the screen
     * 
     * @return WindowResult containing success status and details
     */
    public WindowResult centerWindow() {
        String methodName = "centerWindow";
        long startTime = System.currentTimeMillis();
        
        log.info("{} - Centering window on screen", methodName);
        
        try {
            // Get screen dimensions
            Dimension screenSize = getScreenSize();
            Dimension windowSize = getCurrentWindowSize();
            
            // Calculate center position
            int centerX = (screenSize.width - windowSize.width) / 2;
            int centerY = (screenSize.height - windowSize.height) / 2;
            
            // Ensure position is not negative
            centerX = Math.max(0, centerX);
            centerY = Math.max(0, centerY);
            
            log.debug("{} - Calculated center position: ({}, {})", methodName, centerX, centerY);
            
            WindowResult result = positionWindow(centerX, centerY);
            
            long endTime = System.currentTimeMillis();
            log.info("{} - Completed in {}ms with result: {}", methodName, (endTime - startTime), result.getStatus());
            
            return result;
            
        } catch (Exception e) {
            return handleUnexpectedException(methodName, "center window", e);
        }
    }
    
    /**
     * Restore window to previous state
     * Single Responsibility: Restore window to previously stored state
     * 
     * @return WindowResult containing success status and details
     */
    public WindowResult restoreWindow() {
        String methodName = "restoreWindow";
        long startTime = System.currentTimeMillis();
        
        log.info("{} - Restoring window to previous state", methodName);
        
        if (previousSize == null || previousPosition == null) {
            String errorMsg = "No previous window state stored for restoration";
            log.error("{} - {}", methodName, errorMsg);
            return WindowResult.failure(errorMsg);
        }
        
        try {
            // First restore size, then position
            WindowResult resizeResult = resizeWindow(previousSize.width, previousSize.height);
            if (!resizeResult.isSuccess()) {
                return resizeResult;
            }
            
            WindowResult positionResult = positionWindow(previousPosition.x, previousPosition.y);
            
            long endTime = System.currentTimeMillis();
            log.info("{} - Completed in {}ms with result: {}", methodName, (endTime - startTime), positionResult.getStatus());
            
            return positionResult;
            
        } catch (Exception e) {
            return handleUnexpectedException(methodName, "restore window", e);
        }
    }
    
    /**
     * Get current window information
     * 
     * @return WindowInfo object with current window details
     */
    public WindowInfo getWindowInfo() {
        try {
            Dimension size = getCurrentWindowSize();
            Point position = getCurrentWindowPosition();
            WindowState state = getCurrentWindowState();
            
            return new WindowInfo(size, position, state, getScreenSize());
            
        } catch (Exception e) {
            log.error("Error getting window information: {}", e.getMessage());
            return WindowInfo.unavailable();
        }
    }
    
    /**
     * Check if window is maximized
     * 
     * @return boolean indicating if window is maximized
     */
    public boolean isWindowMaximized() {
        try {
            return getCurrentWindowState() == WindowState.MAXIMIZED;
        } catch (Exception e) {
            log.warn("Error checking window maximized state: {}", e.getMessage());
            return false;
        }
    }
    
    /**
     * Check if window is in fullscreen mode
     * 
     * @return boolean indicating if window is in fullscreen
     */
    public boolean isWindowFullscreen() {
        try {
            return getCurrentWindowState() == WindowState.FULLSCREEN;
        } catch (Exception e) {
            log.warn("Error checking window fullscreen state: {}", e.getMessage());
            return false;
        }
    }
    
    /**
     * Performs maximize operation with retry mechanism
     */
    private WindowResult performMaximizeWithRetry() {
        Exception lastException = null;
        
        for (int attempt = 1; attempt <= RETRY_COUNT; attempt++) {
            try {
                log.debug("Maximize attempt {}/{}", attempt, RETRY_COUNT);
                
                // Perform maximize operation
                driver.manage().window().maximize();
                pauseExecution(OPERATION_PAUSE_MS);
                
                // Verify operation if enabled
                if (VERIFY_OPERATIONS) {
                    if (isOperationSuccessful(WindowState.MAXIMIZED)) {
                        log.info("Window maximize operation successful");
                        return WindowResult.success("Window successfully maximized");
                    } else {
                        log.warn("Maximize attempt {} - verification failed", attempt);
                    }
                } else {
                    return WindowResult.success("Window maximize command executed");
                }
                
            } catch (WebDriverException e) {
                lastException = e;
                log.warn("Maximize attempt {} failed: {}", attempt, e.getMessage());
            }
            
            if (attempt < RETRY_COUNT) {
                pauseExecution(1000);
            }
        }
        
        throw new WindowManagementException("Window maximize failed after " + RETRY_COUNT + " attempts", lastException);
    }
    
    /**
     * Performs minimize operation with retry mechanism
     */
    private WindowResult performMinimizeWithRetry() {
        Exception lastException = null;
        
        for (int attempt = 1; attempt <= RETRY_COUNT; attempt++) {
            try {
                log.debug("Minimize attempt {}/{}", attempt, RETRY_COUNT);
                
                // Use JavaScript as WebDriver minimize() is not universally supported
                jsExecutor.executeScript("window.minimize();");
                pauseExecution(OPERATION_PAUSE_MS);
                
                // Verify operation if possible (minimize verification is challenging)
                log.info("Window minimize command executed successfully");
                return WindowResult.success("Window minimize command executed");
                
            } catch (Exception e) {
                lastException = e;
                log.warn("Minimize attempt {} failed: {}", attempt, e.getMessage());
            }
            
            if (attempt < RETRY_COUNT) {
                pauseExecution(1000);
            }
        }
        
        throw new WindowManagementException("Window minimize failed after " + RETRY_COUNT + " attempts", lastException);
    }
    
    /**
     * Performs fullscreen operation with retry mechanism
     */
    private WindowResult performFullscreenWithRetry() {
        Exception lastException = null;
        
        for (int attempt = 1; attempt <= RETRY_COUNT; attempt++) {
            try {
                log.debug("Fullscreen attempt {}/{}", attempt, RETRY_COUNT);
                
                // Try WebDriver fullscreen first
                try {
                    driver.manage().window().fullscreen();
                } catch (Exception e) {
                    // Fallback to JavaScript method
                    log.debug("WebDriver fullscreen failed, trying JavaScript fallback");
                    jsExecutor.executeScript(
                        "if (document.documentElement.requestFullscreen) {" +
                        "  document.documentElement.requestFullscreen();" +
                        "} else if (document.documentElement.webkitRequestFullscreen) {" +
                        "  document.documentElement.webkitRequestFullscreen();" +
                        "} else if (document.documentElement.msRequestFullscreen) {" +
                        "  document.documentElement.msRequestFullscreen();" +
                        "}"
                    );
                }
                
                pauseExecution(OPERATION_PAUSE_MS);
                
                // Verify operation if enabled
                if (VERIFY_OPERATIONS) {
                    if (isOperationSuccessful(WindowState.FULLSCREEN)) {
                        log.info("Fullscreen operation successful");
                        return WindowResult.success("Window successfully set to fullscreen");
                    } else {
                        log.warn("Fullscreen attempt {} - verification failed", attempt);
                    }
                } else {
                    return WindowResult.success("Fullscreen command executed");
                }
                
            } catch (Exception e) {
                lastException = e;
                log.warn("Fullscreen attempt {} failed: {}", attempt, e.getMessage());
            }
            
            if (attempt < RETRY_COUNT) {
                pauseExecution(1000);
            }
        }
        
        throw new WindowManagementException("Fullscreen operation failed after " + RETRY_COUNT + " attempts", lastException);
    }
    
    /**
     * Performs exit fullscreen operation with retry mechanism
     */
    private WindowResult performExitFullscreenWithRetry() {
        Exception lastException = null;
        
        for (int attempt = 1; attempt <= RETRY_COUNT; attempt++) {
            try {
                log.debug("Exit fullscreen attempt {}/{}", attempt, RETRY_COUNT);
                
                // Use JavaScript to exit fullscreen
                jsExecutor.executeScript(
                    "if (document.exitFullscreen) {" +
                    "  document.exitFullscreen();" +
                    "} else if (document.webkitExitFullscreen) {" +
                    "  document.webkitExitFullscreen();" +
                    "} else if (document.msExitFullscreen) {" +
                    "  document.msExitFullscreen();" +
                    "}"
                );
                
                pauseExecution(OPERATION_PAUSE_MS);
                
                log.info("Exit fullscreen command executed successfully");
                return WindowResult.success("Successfully exited fullscreen mode");
                
            } catch (Exception e) {
                lastException = e;
                log.warn("Exit fullscreen attempt {} failed: {}", attempt, e.getMessage());
            }
            
            if (attempt < RETRY_COUNT) {
                pauseExecution(1000);
            }
        }
        
        throw new WindowManagementException("Exit fullscreen failed after " + RETRY_COUNT + " attempts", lastException);
    }
    
    /**
     * Performs resize operation with retry mechanism
     */
    private WindowResult performResizeWithRetry(int width, int height) {
        Exception lastException = null;
        Dimension targetSize = new Dimension(width, height);
        
        for (int attempt = 1; attempt <= RETRY_COUNT; attempt++) {
            try {
                log.debug("Resize attempt {}/{} to {}x{}", attempt, RETRY_COUNT, width, height);
                
                driver.manage().window().setSize(targetSize);
                pauseExecution(OPERATION_PAUSE_MS);
                
                // Verify operation if enabled
                if (VERIFY_OPERATIONS) {
                    Dimension actualSize = getCurrentWindowSize();
                    if (isSizeMatching(targetSize, actualSize)) {
                        log.info("Window resize successful to {}x{}", width, height);
                        return WindowResult.success(String.format("Window resized to %dx%d", width, height));
                    } else {
                        log.warn("Resize attempt {} - size mismatch. Target: {}x{}, Actual: {}x{}", 
                                attempt, width, height, actualSize.width, actualSize.height);
                    }
                } else {
                    return WindowResult.success("Window resize command executed");
                }
                
            } catch (WebDriverException e) {
                lastException = e;
                log.warn("Resize attempt {} failed: {}", attempt, e.getMessage());
            }
            
            if (attempt < RETRY_COUNT) {
                pauseExecution(1000);
            }
        }
        
        throw new WindowManagementException(
            String.format("Window resize to %dx%d failed after %d attempts", width, height, RETRY_COUNT), 
            lastException
        );
    }
    
    /**
     * Performs position operation with retry mechanism
     */
    private WindowResult performPositionWithRetry(int x, int y) {
        Exception lastException = null;
        Point targetPosition = new Point(x, y);
        
        for (int attempt = 1; attempt <= RETRY_COUNT; attempt++) {
            try {
                log.debug("Position attempt {}/{} to ({}, {})", attempt, RETRY_COUNT, x, y);
                
                driver.manage().window().setPosition(targetPosition);
                pauseExecution(OPERATION_PAUSE_MS);
                
                // Verify operation if enabled
                if (VERIFY_OPERATIONS) {
                    Point actualPosition = getCurrentWindowPosition();
                    if (isPositionMatching(targetPosition, actualPosition)) {
                        log.info("Window position successful to ({}, {})", x, y);
                        return WindowResult.success(String.format("Window positioned at (%d, %d)", x, y));
                    } else {
                        log.warn("Position attempt {} - position mismatch. Target: ({}, {}), Actual: ({}, {})", 
                                attempt, x, y, actualPosition.x, actualPosition.y);
                    }
                } else {
                    return WindowResult.success("Window position command executed");
                }
                
            } catch (WebDriverException e) {
                lastException = e;
                log.warn("Position attempt {} failed: {}", attempt, e.getMessage());
            }
            
            if (attempt < RETRY_COUNT) {
                pauseExecution(1000);
            }
        }
        
        throw new WindowManagementException(
            String.format("Window position to (%d, %d) failed after %d attempts", x, y, RETRY_COUNT), 
            lastException
        );
    }
    
    /**
     * Gets current window size with error handling
     */
    private Dimension getCurrentWindowSize() {
        try {
            return driver.manage().window().getSize();
        } catch (Exception e) {
            log.warn("Error getting window size: {}", e.getMessage());
            return new Dimension(0, 0);
        }
    }
    
    /**
     * Gets current window position with error handling
     */
    private Point getCurrentWindowPosition() {
        try {
            return driver.manage().window().getPosition();
        } catch (Exception e) {
            log.warn("Error getting window position: {}", e.getMessage());
            return new Point(0, 0);
        }
    }
    
    /**
     * Determines current window state
     */
    private WindowState getCurrentWindowState() {
        try {
            // Use JavaScript to determine window state
            String script = 
                "return {" +
                "  isFullscreen: !!(document.fullscreenElement || document.webkitFullscreenElement || document.msFullscreenElement)," +
                "  outerWidth: window.outerWidth," +
                "  outerHeight: window.outerHeight," +
                "  screenWidth: screen.width," +
                "  screenHeight: screen.height" +
                "};";
            
            @SuppressWarnings("unchecked")
            java.util.Map<String, Object> result = (java.util.Map<String, Object>) jsExecutor.executeScript(script);
            
            boolean isFullscreen = (Boolean) result.get("isFullscreen");
            if (isFullscreen) {
                return WindowState.FULLSCREEN;
            }
            
            // Check if maximized by comparing window size to screen size
            long outerWidth = ((Number) result.get("outerWidth")).longValue();
            long outerHeight = ((Number) result.get("outerHeight")).longValue();
            long screenWidth = ((Number) result.get("screenWidth")).longValue();
            long screenHeight = ((Number) result.get("screenHeight")).longValue();
            
            if (outerWidth >= screenWidth && outerHeight >= screenHeight) {
                return WindowState.MAXIMIZED;
            }
            
            return WindowState.NORMAL;
            
        } catch (Exception e) {
            log.warn("Error determining window state: {}", e.getMessage());
            return WindowState.UNKNOWN;
        }
    }
    
    /**
     * Gets screen dimensions
     */
    private Dimension getScreenSize() {
        try {
            java.awt.Dimension screenSize = Toolkit.getDefaultToolkit().getScreenSize();
            return new Dimension(screenSize.width, screenSize.height);
        } catch (Exception e) {
            log.warn("Error getting screen size, using default: {}", e.getMessage());
            return new Dimension(1920, 1080);
        }
    }
    
    /**
     * Validates if window size is reasonable
     */
    private boolean isValidWindowSize(int width, int height) {
        try {
            Dimension screenSize = getScreenSize();
            return width <= screenSize.width && height <= screenSize.height && width >= 100 && height >= 100;
        } catch (Exception e) {
            log.warn("Error validating window size: {}", e.getMessage());
            return true; // Allow operation to proceed
        }
    }
    
    /**
     * Stores current window state for potential rollback
     */
    private void storeCurrentWindowState() {
        try {
            this.previousState = getCurrentWindowState();
            this.previousSize = getCurrentWindowSize();
            this.previousPosition = getCurrentWindowPosition();
            
            log.debug("Stored window state - State: {}, Size: {}x{}, Position: ({}, {})",
                    previousState, previousSize.width, previousSize.height, 
                    previousPosition.x, previousPosition.y);
        } catch (Exception e) {
            log.warn("Error storing current window state: {}", e.getMessage());
        }
    }
    
    /**
     * Checks if operation was successful based on expected state
     */
    private boolean isOperationSuccessful(WindowState expectedState) {
        try {
            WindowState currentState = getCurrentWindowState();
            return currentState == expectedState;
        } catch (Exception e) {
            log.warn("Error verifying operation success: {}", e.getMessage());
            return false;
        }
    }
    
    /**
     * Checks if sizes match within tolerance
     */
    private boolean isSizeMatching(Dimension expected, Dimension actual) {
        int tolerance = 10; // pixels
        return Math.abs(expected.width - actual.width) <= tolerance && 
               Math.abs(expected.height - actual.height) <= tolerance;
    }
    
    /**
     * Checks if positions match within tolerance
     */
    private boolean isPositionMatching(Point expected, Point actual) {
        int tolerance = 10; // pixels
        return Math.abs(expected.x - actual.x) <= tolerance && 
               Math.abs(expected.y - actual.y) <= tolerance;
    }
    
    /**
     * Handles window management specific exceptions
     */
    private WindowResult handleWindowException(String methodName, String operation, WindowManagementException e) {
        String errorMsg = String.format("Window %s operation failed in %s: %s", operation, methodName, e.getMessage());
        log.error("{} - {}", methodName, errorMsg, e);
        
        if (ENABLE_SCREENSHOTS) {
            screenshotHelper.captureScreenshot("window_" + operation + "_failed_" + System.currentTimeMillis());
        }
        
        return WindowResult.failure(errorMsg);
    }
    
    /**
     * Handles unexpected exceptions
     */
    private WindowResult handleUnexpectedException(String methodName, String operation, Exception e) {
        String errorMsg = String.format("Unexpected error during window %s operation in %s: %s", 
                operation, methodName, e.getMessage());
        log.error("{} - {}", methodName, errorMsg, e);
        
        if (ENABLE_SCREENSHOTS) {
            screenshotHelper.captureScreenshot("window_unexpected_error_" + methodName + "_" + System.currentTimeMillis());
        }
        
        return WindowResult.failure(errorMsg);
    }
    
    /**
     * Thread-safe pause execution
     */
    private void pauseExecution(int milliseconds) {
        try {
            Thread.sleep(milliseconds);
        } catch (InterruptedException e) {
            Thread.currentThread().interrupt();
            log.warn("Thread interrupted during pause: {}", e.getMessage());
        }
    }
    
    /**
     * Loads configuration from properties file
     */
    private Properties loadConfiguration() {
        Properties props = new Properties();
        try (InputStream input = getClass().getClassLoader().getResourceAsStream("window-config.properties")) {
            if (input != null) {
                props.load(input);
                log.info("Window configuration loaded successfully");
            } else {
                log.warn("Window configuration file not found, using default values");
            }
        } catch (IOException e) {
            log.warn("Error loading window configuration, using default values: {}", e.getMessage());
        }
        return props;
            log.warn("Thread interrupted during pause: {}", e.getMessage());
        }
    }
    
    /**
     * Loads configuration from properties file
     */
    private Properties loadConfiguration() {
        Properties props = new Properties();
        try (InputStream input = getClass().getClassLoader().getResourceAsStream("window-config.properties")) {
            if (input != null) {
                props.load(input);
                log.info("Window configuration loaded successfully");
            } else {
                log.warn("Window configuration file not found, using default values");
            }
        } catch (IOException e) {
            log.warn("Error loading window configuration, using default values: {}", e.getMessage());
        }
        return props;
    }
}

/**
 * Custom exception for window management failures
 */
class WindowManagementException extends RuntimeException {
    public WindowManagementException(String message) {
        super(message);
    }
    
    public WindowManagementException(String message, Throwable cause) {
        super(message, cause);
    }
}

/**
 * Enumeration for window states
 */
enum WindowState {
    NORMAL,
    MAXIMIZED,
    MINIMIZED,
    FULLSCREEN,
    UNKNOWN
}

/**
 * Result object for window management operations
 * Returns meaningful information about the operation
 */
class WindowResult {
    private final boolean success;
    private final String message;
    private final long timestamp;
    private final WindowInfo windowInfo;
    
    private WindowResult(boolean success, String message, WindowInfo windowInfo) {
        this.success = success;
        this.message = message;
        this.windowInfo = windowInfo;
        this.timestamp = System.currentTimeMillis();
    }
    
    public static WindowResult success(String message) {
        return new WindowResult(true, message, null);
    }
    
    public static WindowResult successWithInfo(String message, WindowInfo windowInfo) {
        return new WindowResult(true, message, windowInfo);
    }
    
    public static WindowResult failure(String message) {
        return new WindowResult(false, message, null);
    }
    
    public boolean isSuccess() { return success; }
    public String getMessage() { return message; }
    public String getStatus() { return success ? "SUCCESS" : "FAILURE"; }
    public WindowInfo getWindowInfo() { return windowInfo; }
    public long getTimestamp() { return timestamp; }
    
    @Override
    public String toString() {
        return String.format("WindowResult{status=%s, message='%s', timestamp=%d}", 
                getStatus(), message, timestamp);
    }
}

/**
 * Information object containing window details
 */
class WindowInfo {
    private final Dimension size;
    private final Point position;
    private final WindowState state;
    private final Dimension screenSize;
    private final boolean available;
    
    public WindowInfo(Dimension size, Point position, WindowState state, Dimension screenSize) {
        this.size = size;
        this.position = position;
        this.state = state;
        this.screenSize = screenSize;
        this.available = true;
    }
    
    public static WindowInfo unavailable() {
        return new WindowInfo(null, null, WindowState.UNKNOWN, null, false);
    }
    
    private WindowInfo(Dimension size, Point position, WindowState state, Dimension screenSize, boolean available) {
        this.size = size;
        this.position = position;
        this.state = state;
        this.screenSize = screenSize;
        this.available = available;
    }
    
    public Dimension getSize() { return size; }
    public Point getPosition() { return position; }
    public WindowState getState() { return state; }
    public Dimension getScreenSize() { return screenSize; }
    public boolean isAvailable() { return available; }
    
    @Override
    public String toString() {
        if (!available) {
            return "WindowInfo{unavailable}";
        }
        return String.format("WindowInfo{size=%dx%d, position=(%d,%d), state=%s, screenSize=%dx%d}",
                size.width, size.height, position.x, position.y, state, 
                screenSize.width, screenSize.height);
    }
}

/**
 * Helper interface for screenshot functionality
 */
interface ScreenshotHelper {
    void captureScreenshot(String filename);
}

/**
 * Example usage and testing class
 */
class WindowManagementExample {
    
    public static void demonstrateUsage(WebDriver driver, ScreenshotHelper screenshotHelper) {
        WindowManagementKeyword windowKeyword = new WindowManagementKeyword(driver, screenshotHelper);
        
        // Example 1: Basic window operations
        WindowResult maximizeResult = windowKeyword.maximizeWindow();
        if (maximizeResult.isSuccess()) {
            System.out.println("Window maximized: " + maximizeResult.getMessage());
        }
        
        // Example 2: Window resizing
        WindowResult resizeResult = windowKeyword.resizeWindow(1280, 720);
        if (resizeResult.isSuccess()) {
            System.out.println("Window resized: " + resizeResult.getMessage());
        }
        
        // Example 3: Window positioning
        WindowResult positionResult = windowKeyword.positionWindow(100, 100);
        if (positionResult.isSuccess()) {
            System.out.println("Window positioned: " + positionResult.getMessage());
        }
        
        // Example 4: Fullscreen operations
        WindowResult fullscreenResult = windowKeyword.setFullscreen();
        if (fullscreenResult.isSuccess()) {
            // Wait a moment then exit fullscreen
            try { Thread.sleep(2000); } catch (InterruptedException ignored) {}
            WindowResult exitResult = windowKeyword.exitFullscreen();
            System.out.println("Fullscreen toggled: " + exitResult.getMessage());
        }
        
        // Example 5: Window state checking
        boolean isMaximized = windowKeyword.isWindowMaximized();
        boolean isFullscreen = windowKeyword.isWindowFullscreen();
        System.out.println("Is Maximized: " + isMaximized);
        System.out.println("Is Fullscreen: " + isFullscreen);
        
        // Example 6: Get window information
        WindowInfo info = windowKeyword.getWindowInfo();
        System.out.println("Window Info: " + info);
        
        // Example 7: Utility operations
        windowKeyword.centerWindow();
        windowKeyword.setDefaultWindowSize();
        
        // Example 8: State restoration
        windowKeyword.restoreWindow();
    }
}

/**
 * Advanced Window Management Utilities
 */
class AdvancedWindowManagement {
    
    private final WindowManagementKeyword windowKeyword;
    
    public AdvancedWindowManagement(WindowManagementKeyword windowKeyword) {
        this.windowKeyword = windowKeyword;
    }
    
    /**
     * Set window to common predefined sizes
     */
    public WindowResult setWindowSize(WindowSize size) {
        switch (size) {
            case MOBILE_PORTRAIT:
                return windowKeyword.resizeWindow(375, 667);
            case MOBILE_LANDSCAPE:
                return windowKeyword.resizeWindow(667, 375);
            case TABLET_PORTRAIT:
                return windowKeyword.resizeWindow(768, 1024);
            case TABLET_LANDSCAPE:
                return windowKeyword.resizeWindow(1024, 768);
            case DESKTOP_SMALL:
                return windowKeyword.resizeWindow(1366, 768);
            case DESKTOP_MEDIUM:
                return windowKeyword.resizeWindow(1440, 900);
            case DESKTOP_LARGE:
                return windowKeyword.resizeWindow(1920, 1080);
            case DESKTOP_4K:
                return windowKeyword.resizeWindow(3840, 2160);
            default:
                return WindowResult.failure("Unknown window size: " + size);
        }
    }
    
    /**
     * Test responsive design by cycling through different window sizes
     */
    public void testResponsiveDesign(ResponsiveTestCallback callback) {
        WindowSize[] sizes = {
            WindowSize.MOBILE_PORTRAIT,
            WindowSize.TABLET_PORTRAIT,
            WindowSize.DESKTOP_SMALL,
            WindowSize.DESKTOP_LARGE
        };
        
        for (WindowSize size : sizes) {
            WindowResult result = setWindowSize(size);
            if (result.isSuccess()) {
                try {
                    Thread.sleep(1000); // Allow layout to settle
                    callback.testAtSize(size, windowKeyword.getWindowInfo());
                } catch (InterruptedException e) {
                    Thread.currentThread().interrupt();
                    break;
                }
            }
        }
    }
    
    /**
     * Position windows for multi-monitor setup testing
     */
    public WindowResult positionForMultiMonitor(MonitorPosition position) {
        WindowInfo info = windowKeyword.getWindowInfo();
        if (!info.isAvailable()) {
            return WindowResult.failure("Unable to get current window information");
        }
        
        Dimension screenSize = info.getScreenSize();
        Dimension windowSize = info.getSize();
        
        int x, y;
        switch (position) {
            case TOP_LEFT:
                x = 0;
                y = 0;
                break;
            case TOP_RIGHT:
                x = screenSize.width - windowSize.width;
                y = 0;
                break;
            case BOTTOM_LEFT:
                x = 0;
                y = screenSize.height - windowSize.height;
                break;
            case BOTTOM_RIGHT:
                x = screenSize.width - windowSize.width;
                y = screenSize.height - windowSize.height;
                break;
            case CENTER:
                return windowKeyword.centerWindow();
            case LEFT_HALF:
                windowKeyword.resizeWindow(screenSize.width / 2, screenSize.height);
                return windowKeyword.positionWindow(0, 0);
            case RIGHT_HALF:
                windowKeyword.resizeWindow(screenSize.width / 2, screenSize.height);
                return windowKeyword.positionWindow(screenSize.width / 2, 0);
            default:
                return WindowResult.failure("Unknown monitor position: " + position);
        }
        
        return windowKeyword.positionWindow(x, y);
    }
}

/**
 * Predefined window sizes for responsive testing
 */
enum WindowSize {
    MOBILE_PORTRAIT,
    MOBILE_LANDSCAPE,
    TABLET_PORTRAIT,
    TABLET_LANDSCAPE,
    DESKTOP_SMALL,
    DESKTOP_MEDIUM,
    DESKTOP_LARGE,
    DESKTOP_4K
}

/**
 * Monitor positions for multi-monitor testing
 */
enum MonitorPosition {
    TOP_LEFT,
    TOP_RIGHT,
    BOTTOM_LEFT,
    BOTTOM_RIGHT,
    CENTER,
    LEFT_HALF,
    RIGHT_HALF
}

/**
 * Callback interface for responsive design testing
 */
interface ResponsiveTestCallback {
    void testAtSize(WindowSize size, WindowInfo windowInfo);
}

/**
 * Window Management Test Suite
 */
class WindowManagementTestSuite {
    
    public static void runComprehensiveTests(WebDriver driver, ScreenshotHelper screenshotHelper) {
        WindowManagementKeyword windowKeyword = new WindowManagementKeyword(driver, screenshotHelper);
        AdvancedWindowManagement advancedWindow = new AdvancedWindowManagement(windowKeyword);
        
        System.out.println("=== Window Management Test Suite ===");
        
        // Test 1: Basic Operations
        System.out.println("\n1. Testing Basic Window Operations...");
        testBasicOperations(windowKeyword);
        
        // Test 2: Resize Operations
        System.out.println("\n2. Testing Window Resize Operations...");
        testResizeOperations(windowKeyword);
        
        // Test 3: Position Operations
        System.out.println("\n3. Testing Window Position Operations...");
        testPositionOperations(windowKeyword);
        
        // Test 4: State Management
        System.out.println("\n4. Testing Window State Management...");
        testStateManagement(windowKeyword);
        
        // Test 5: Responsive Design Testing
        System.out.println("\n5. Testing Responsive Design...");
        testResponsiveDesign(advancedWindow);
        
        // Test 6: Multi-Monitor Positioning
        System.out.println("\n6. Testing Multi-Monitor Positioning...");
        testMultiMonitorPositioning(advancedWindow);
        
        System.out.println("\n=== Test Suite Completed ===");
    }
    
    private static void testBasicOperations(WindowManagementKeyword windowKeyword) {
        // Test maximize
        WindowResult result = windowKeyword.maximizeWindow();
        System.out.println("Maximize: " + (result.isSuccess() ? "✅ PASS" : "❌ FAIL - " + result.getMessage()));
        
        // Test minimize (note: verification may be limited)
        result = windowKeyword.minimizeWindow();
        System.out.println("Minimize: " + (result.isSuccess() ? "✅ PASS" : "❌ FAIL - " + result.getMessage()));
        
        // Restore to normal state
        windowKeyword.maximizeWindow(); // Restore from minimize
    }
    
    private static void testResizeOperations(WindowManagementKeyword windowKeyword) {
        // Test standard sizes
        int[][] sizes = {{1280, 720}, {1920, 1080}, {800, 600}};
        
        for (int[] size : sizes) {
            WindowResult result = windowKeyword.resizeWindow(size[0], size[1]);
            System.out.printf("Resize to %dx%d: %s%n", 
                size[0], size[1], 
                result.isSuccess() ? "✅ PASS" : "❌ FAIL - " + result.getMessage());
        }
        
        // Test default size
        WindowResult result = windowKeyword.setDefaultWindowSize();
        System.out.println("Set Default Size: " + (result.isSuccess() ? "✅ PASS" : "❌ FAIL - " + result.getMessage()));
    }
    
    private static void testPositionOperations(WindowManagementKeyword windowKeyword) {
        // Test various positions
        int[][] positions = {{0, 0}, {100, 100}, {200, 150}};
        
        for (int[] pos : positions) {
            WindowResult result = windowKeyword.positionWindow(pos[0], pos[1]);
            System.out.printf("Position to (%d, %d): %s%n", 
                pos[0], pos[1], 
                result.isSuccess() ? "✅ PASS" : "❌ FAIL - " + result.getMessage());
        }
        
        // Test center window
        WindowResult result = windowKeyword.centerWindow();
        System.out.println("Center Window: " + (result.isSuccess() ? "✅ PASS" : "❌ FAIL - " + result.getMessage()));
    }
    
    private static void testStateManagement(WindowManagementKeyword windowKeyword) {
        // Test fullscreen
        WindowResult result = windowKeyword.setFullscreen();
        System.out.println("Set Fullscreen: " + (result.isSuccess() ? "✅ PASS" : "❌ FAIL - " + result.getMessage()));
        
        try { Thread.sleep(1000); } catch (InterruptedException ignored) {}
        
        result = windowKeyword.exitFullscreen();
        System.out.println("Exit Fullscreen: " + (result.isSuccess() ? "✅ PASS" : "❌ FAIL - " + result.getMessage()));
        
        // Test state checking
        boolean isMaximized = windowKeyword.isWindowMaximized();
        boolean isFullscreen = windowKeyword.isWindowFullscreen();
        System.out.println("State Check - Maximized: " + isMaximized + ", Fullscreen: " + isFullscreen);
        
        // Test restoration
        result = windowKeyword.restoreWindow();
        System.out.println("Restore Window: " + (result.isSuccess() ? "✅ PASS" : "❌ FAIL - " + result.getMessage()));
    }
    
    private static void testResponsiveDesign(AdvancedWindowManagement advancedWindow) {
        WindowSize[] testSizes = {WindowSize.MOBILE_PORTRAIT, WindowSize.TABLET_PORTRAIT, WindowSize.DESKTOP_LARGE};
        
        for (WindowSize size : testSizes) {
            WindowResult result = advancedWindow.setWindowSize(size);
            System.out.println("Responsive Size " + size + ": " + 
                (result.isSuccess() ? "✅ PASS" : "❌ FAIL - " + result.getMessage()));
        }
    }
    
    private static void testMultiMonitorPositioning(AdvancedWindowManagement advancedWindow) {
        MonitorPosition[] positions = {
            MonitorPosition.TOP_LEFT, 
            MonitorPosition.CENTER, 
            MonitorPosition.RIGHT_HALF
        };
        
        for (MonitorPosition position : positions) {
            WindowResult result = advancedWindow.positionForMultiMonitor(position);
            System.out.println("Monitor Position " + position + ": " + 
                (result.isSuccess() ? "✅ PASS" : "❌ FAIL - " + result.getMessage()));
        }
    }
}WindowSize();
        System.out.println("Set Default Size: " + (result.isSuccess() ? "✅ PASS" : "❌ FAIL - " + result.getMessage()));
    }
    
    private static void testPositionOperations(WindowManagementKeyword windowKeyword) {
        // Test various positions
        int[][] positions = {{0, 0}, {100, 100}, {200, 150}};
        
        for (int[] pos : positions) {
            WindowResult result = windowKeyword.positionWindow(pos[0], pos[1]);
            System.out.printf("Position to (%d, %d): %s%n", 
                pos[0], pos[1], 
                result.isSuccess() ? "✅ PASS" : "❌ FAIL - " + result.getMessage());
        }
        
        // Test center window
        WindowResult result = windowKeyword.centerWindow();
        System.out.println("Center Window: " + (result.isSuccess() ? "✅ PASS" : "❌ FAIL - " + result.getMessage()));
    }
    
    private static void testStateManagement(WindowManagementKeyword windowKeyword) {
        // Test fullscreen
        WindowResult result = windowKeyword.setFullscreen();
        System.out.println("Set Fullscreen: " + (result.isSuccess() ? "✅ PASS" : "❌ FAIL - " + result.getMessage()));
        
        try { Thread.sleep(1000); } catch (InterruptedException ignored) {}
        
        result = windowKeyword.exitFullscreen();
        System.out.println("Exit Fullscreen: " + (result.isSuccess() ? "✅ PASS" : "❌ FAIL - " + result.getMessage()));
        
        // Test state checking
        boolean isMaximized = windowKeyword.isWindowMaximized();
        boolean isFullscreen = windowKeyword.isWindowFullscreen();
        System.out.println("State Check - Maximized: " + isMaximized + ", Fullscreen: " + isFullscreen);
        
        // Test restoration
        result = windowKeyword.restoreWindow();
        System.out.println("Restore Window: " + (result.isSuccess() ? "✅ PASS" : "❌ FAIL - " + result.getMessage()));
    }
    
    private static void testResponsiveDesign(AdvancedWindowManagement advancedWindow) {
        WindowSize[] testSizes = {WindowSize.MOBILE_PORTRAIT, WindowSize.TABLET_PORTRAIT, WindowSize.DESKTOP_LARGE};
        
        for (WindowSize size : testSizes) {
            WindowResult result = advancedWindow.setWindowSize(size);
            System.out.println("Responsive Size " + size + ": " + 
                (result.isSuccess() ? "✅ PASS" : "❌ FAIL - " + result.getMessage()));
        }
    }
    
    private static void testMultiMonitorPositioning(AdvancedWindowManagement advancedWindow) {
        MonitorPosition[] positions = {
            MonitorPosition.TOP_LEFT, 
            MonitorPosition.CENTER, 
            MonitorPosition.RIGHT_HALF
        };
        
        for (MonitorPosition position : positions) {
            WindowResult result = advancedWindow.positionForMultiMonitor(position);
            System.out.println("Monitor Position " + position + ": " + 
                (result.isSuccess() ? "✅ PASS" : "❌ FAIL - " + result.getMessage()));
        }
    }
            