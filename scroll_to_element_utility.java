import org.openqa.selenium.WebDriver;
import org.openqa.selenium.WebElement;
import org.openqa.selenium.JavascriptExecutor;
import org.openqa.selenium.By;
import org.openqa.selenium.support.ui.WebDriverWait;
import org.openqa.selenium.support.ui.ExpectedConditions;
import org.openqa.selenium.TimeoutException;
import java.time.Duration;

/**
 * ScrollToElement Utility Class
 * Provides methods to scroll to elements when needed using JavaScriptExecutor
 */
public class ScrollToElementKeyword {
    
    private WebDriver driver;
    private JavascriptExecutor jsExecutor;
    private WebDriverWait wait;
    
    // Constructor
    public ScrollToElementKeyword(WebDriver driver) {
        this.driver = driver;
        this.jsExecutor = (JavascriptExecutor) driver;
        this.wait = new WebDriverWait(driver, Duration.ofSeconds(10));
    }
    
    /**
     * Scroll to element using WebElement - Basic scroll
     * @param element - WebElement to scroll to
     */
    public void scrollToElement(WebElement element) {
        try {
            jsExecutor.executeScript("arguments[0].scrollIntoView(true);", element);
            Thread.sleep(500); // Small pause for smooth scrolling
        } catch (Exception e) {
            System.err.println("Error scrolling to element: " + e.getMessage());
        }
    }
    
    /**
     * Scroll to element using locator - Basic scroll
     * @param locator - By locator to find the element
     */
    public void scrollToElement(By locator) {
        try {
            WebElement element = wait.until(ExpectedConditions.presenceOfElementLocated(locator));
            scrollToElement(element);
        } catch (TimeoutException e) {
            System.err.println("Element not found within timeout: " + locator.toString());
        }
    }
    
    /**
     * Scroll to element with smooth behavior
     * @param element - WebElement to scroll to
     */
    public void scrollToElementSmooth(WebElement element) {
        try {
            jsExecutor.executeScript("arguments[0].scrollIntoView({behavior: 'smooth', block: 'center'});", element);
            Thread.sleep(1000); // Wait for smooth scroll to complete
        } catch (Exception e) {
            System.err.println("Error smooth scrolling to element: " + e.getMessage());
        }
    }
    
    /**
     * Scroll to element with custom options
     * @param element - WebElement to scroll to
     * @param behavior - 'auto' or 'smooth'
     * @param block - 'start', 'center', 'end', or 'nearest'
     * @param inline - 'start', 'center', 'end', or 'nearest'
     */
    public void scrollToElementWithOptions(WebElement element, String behavior, String block, String inline) {
        try {
            String script = String.format(
                "arguments[0].scrollIntoView({behavior: '%s', block: '%s', inline: '%s'});", 
                behavior, block, inline
            );
            jsExecutor.executeScript(script, element);
            Thread.sleep(behavior.equals("smooth") ? 1000 : 500);
        } catch (Exception e) {
            System.err.println("Error scrolling to element with options: " + e.getMessage());
        }
    }
    
    /**
     * Scroll to element only if it's not visible in viewport
     * @param element - WebElement to check and scroll to if needed
     */
    public void scrollToElementIfNeeded(WebElement element) {
        try {
            // Check if element is in viewport
            Boolean isInViewport = (Boolean) jsExecutor.executeScript(
                "var rect = arguments[0].getBoundingClientRect();" +
                "return (rect.top >= 0 && rect.left >= 0 && " +
                "rect.bottom <= (window.innerHeight || document.documentElement.clientHeight) && " +
                "rect.right <= (window.innerWidth || document.documentElement.clientWidth));", 
                element
            );
            
            if (!isInViewport) {
                scrollToElement(element);
                System.out.println("Element scrolled into view");
            } else {
                System.out.println("Element already in viewport, no scroll needed");
            }
        } catch (Exception e) {
            System.err.println("Error checking viewport or scrolling: " + e.getMessage());
        }
    }
    
    /**
     * Scroll to element and wait for it to be clickable
     * @param element - WebElement to scroll to and wait for
     * @return boolean - true if element becomes clickable, false otherwise
     */
    public boolean scrollToElementAndWaitClickable(WebElement element) {
        try {
            scrollToElement(element);
            wait.until(ExpectedConditions.elementToBeClickable(element));
            return true;
        } catch (TimeoutException e) {
            System.err.println("Element not clickable after scrolling: " + e.getMessage());
            return false;
        }
    }
    
    /**
     * Scroll to element by locator and wait for it to be clickable
     * @param locator - By locator to find the element
     * @return WebElement - the clickable element, or null if not found/clickable
     */
    public WebElement scrollToElementAndWaitClickable(By locator) {
        try {
            WebElement element = wait.until(ExpectedConditions.presenceOfElementLocated(locator));
            scrollToElement(element);
            return wait.until(ExpectedConditions.elementToBeClickable(element));
        } catch (TimeoutException e) {
            System.err.println("Element not found or not clickable: " + locator.toString());
            return null;
        }
    }
    
    /**
     * Scroll to top of page
     */
    public void scrollToTop() {
        try {
            jsExecutor.executeScript("window.scrollTo(0, 0);");
            Thread.sleep(500);
        } catch (Exception e) {
            System.err.println("Error scrolling to top: " + e.getMessage());
        }
    }
    
    /**
     * Scroll to bottom of page
     */
    public void scrollToBottom() {
        try {
            jsExecutor.executeScript("window.scrollTo(0, document.body.scrollHeight);");
            Thread.sleep(500);
        } catch (Exception e) {
            System.err.println("Error scrolling to bottom: " + e.getMessage());
        }
    }
    
    /**
     * Scroll by specific pixel amount
     * @param x - horizontal pixels (positive = right, negative = left)
     * @param y - vertical pixels (positive = down, negative = up)
     */
    public void scrollByPixels(int x, int y) {
        try {
            jsExecutor.executeScript(String.format("window.scrollBy(%d, %d);", x, y));
            Thread.sleep(300);
        } catch (Exception e) {
            System.err.println("Error scrolling by pixels: " + e.getMessage());
        }
    }
    
    /**
     * Get element's position relative to viewport
     * @param element - WebElement to get position for
     * @return String - position info (top, left, bottom, right)
     */
    public String getElementViewportPosition(WebElement element) {
        try {
            return (String) jsExecutor.executeScript(
                "var rect = arguments[0].getBoundingClientRect();" +
                "return 'Top: ' + rect.top + ', Left: ' + rect.left + " +
                "', Bottom: ' + rect.bottom + ', Right: ' + rect.right;", 
                element
            );
        } catch (Exception e) {
            System.err.println("Error getting element position: " + e.getMessage());
            return "Position unavailable";
        }
    }
}

// Example Usage Class
class ScrollToElementExample {
    
    public static void demonstrateUsage(WebDriver driver) {
        ScrollToElementKeyword scrollUtil = new ScrollToElementKeyword(driver);
        
        try {
            // Example 1: Basic scroll to element
            WebElement submitButton = driver.findElement(By.id("submit-btn"));
            scrollUtil.scrollToElement(submitButton);
            
            // Example 2: Scroll to element using locator
            scrollUtil.scrollToElement(By.className("footer-link"));
            
            // Example 3: Smooth scroll to element
            WebElement header = driver.findElement(By.tagName("h1"));
            scrollUtil.scrollToElementSmooth(header);
            
            // Example 4: Conditional scroll (only if needed)
            WebElement navMenu = driver.findElement(By.id("nav-menu"));
            scrollUtil.scrollToElementIfNeeded(navMenu);
            
            // Example 5: Scroll and wait for element to be clickable
            WebElement loginBtn = scrollUtil.scrollToElementAndWaitClickable(By.id("login-button"));
            if (loginBtn != null) {
                loginBtn.click();
            }
            
            // Example 6: Custom scroll options
            WebElement targetElement = driver.findElement(By.className("target"));
            scrollUtil.scrollToElementWithOptions(targetElement, "smooth", "center", "center");
            
            // Example 7: Utility scrolls
            scrollUtil.scrollToTop();
            Thread.sleep(1000);
            scrollUtil.scrollToBottom();
            
            // Example 8: Scroll by specific pixels
            scrollUtil.scrollByPixels(0, 200); // Scroll down 200px
            
        } catch (Exception e) {
            System.err.println("Demo error: " + e.getMessage());
        }
    }
}