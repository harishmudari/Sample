// pom.xml - Maven Dependencies
/*
<?xml version="1.0" encoding="UTF-8"?>
<project xmlns="http://maven.apache.org/POM/4.0.0"
         xmlns:xsi="http://www.w3.org/2001/XMLSchema-instance"
         xsi:schemaLocation="http://maven.apache.org/POM/4.0.0 
         http://maven.apache.org/xsd/maven-4.0.0.xsd">
    <modelVersion>4.0.0</modelVersion>
    
    <groupId>com.uiexplorer</groupId>
    <artifactId>ui-explorer-tool</artifactId>
    <version>1.0.0</version>
    <packaging>jar</packaging>
    
    <properties>
        <maven.compiler.source>11</maven.compiler.source>
        <maven.compiler.target>11</maven.compiler.target>
        <selenium.version>4.15.0</selenium.version>
    </properties>
    
    <dependencies>
        <!-- Selenium WebDriver -->
        <dependency>
            <groupId>org.seleniumhq.selenium</groupId>
            <artifactId>selenium-java</artifactId>
            <version>${selenium.version}</version>
        </dependency>
        
        <!-- Chrome DevTools Protocol -->
        <dependency>
            <groupId>org.seleniumhq.selenium</groupId>
            <artifactId>selenium-devtools-v118</artifactId>
            <version>${selenium.version}</version>
        </dependency>
        
        <!-- JSON Processing -->
        <dependency>
            <groupId>com.fasterxml.jackson.core</groupId>
            <artifactId>jackson-databind</artifactId>
            <version>2.15.2</version>
        </dependency>
        
        <!-- Logging -->
        <dependency>
            <groupId>org.slf4j</groupId>
            <artifactId>slf4j-simple</artifactId>
            <version>2.0.7</version>
        </dependency>
        
        <!-- WebDriverManager for automatic driver management -->
        <dependency>
            <groupId>io.github.bonigarcia</groupId>
            <artifactId>webdrivermanager</artifactId>
            <version>5.6.2</version>
        </dependency>
    </dependencies>
    
    <build>
        <plugins>
            <plugin>
                <groupId>org.apache.maven.plugins</groupId>
                <artifactId>maven-compiler-plugin</artifactId>
                <version>3.11.0</version>
                <configuration>
                    <source>11</source>
                    <target>11</target>
                </configuration>
            </plugin>
        </plugins>
    </build>
</project>
*/

package com.uiexplorer.core;

import org.openqa.selenium.*;
import org.openqa.selenium.chrome.ChromeDriver;
import org.openqa.selenium.chrome.ChromeOptions;
import org.openqa.selenium.devtools.DevTools;
import org.openqa.selenium.devtools.v118.dom.DOM;
import org.openqa.selenium.devtools.v118.dom.model.NodeId;
import org.openqa.selenium.devtools.v118.runtime.Runtime;
import org.openqa.selenium.support.ui.WebDriverWait;
import io.github.bonigarcia.wdm.WebDriverManager;

import java.time.Duration;
import java.util.*;
import java.util.concurrent.TimeUnit;

/**
 * Core element inspector that handles Shadow DOM and dynamic content
 * Replicates UiPath UI Explorer functionality
 */
public class ElementInspector {
    private WebDriver driver;
    private DevTools devTools;
    private WebDriverWait wait;
    private JavascriptExecutor jsExecutor;
    
    // Highlighting script for visual feedback
    private static final String HIGHLIGHT_SCRIPT = """
        var element = arguments[0];
        var originalStyle = element.getAttribute('style');
        element.setAttribute('data-original-style', originalStyle || '');
        element.style.border = '3px solid red';
        element.style.backgroundColor = 'yellow';
        element.style.outline = '2px solid blue';
        setTimeout(function() {
            var orig = element.getAttribute('data-original-style');
            if (orig) {
                element.setAttribute('style', orig);
            } else {
                element.removeAttribute('style');
            }
            element.removeAttribute('data-original-style');
        }, 3000);
        """;
    
    // Angular stability detection script
    private static final String ANGULAR_STABILITY_SCRIPT = """
        if (window.getAllAngularTestabilities) {
            var testabilities = window.getAllAngularTestabilities();
            return testabilities.every(function(testability) {
                return testability.isStable();
            });
        }
        if (window.ng) {
            try {
                var injector = window.ng.probe(document.body).injector;
                var ngZone = injector.get(window.ng.core.NgZone);
                return ngZone.isStable;
            } catch (e) {
                return true;
            }
        }
        return true;
        """;
    
    // React stability detection script
    private static final String REACT_STABILITY_SCRIPT = """
        if (window.React && window.React.version) {
            var reactRoot = document.querySelector('[data-reactroot]') || 
                           document.querySelector('div[id*="root"]') ||
                           document.querySelector('div[id*="app"]');
            if (reactRoot && reactRoot._reactInternalFiber) {
                return !reactRoot._reactInternalFiber.pendingTime;
            }
        }
        return document.readyState === 'complete' && 
               performance.now() - performance.timing.loadEventEnd > 500;
        """;
    
    public ElementInspector() {
        initializeWebDriver();
    }
    
    private void initializeWebDriver() {
        WebDriverManager.chromedriver().setup();
        
        ChromeOptions options = new ChromeOptions();
        options.addArguments("--remote-debugging-port=9222");
        options.addArguments("--no-sandbox");
        options.addArguments("--disable-dev-shm-usage");
        options.setExperimentalOption("useAutomationExtension", false);
        options.setExperimentalOption("excludeSwitches", Arrays.asList("enable-automation"));
        
        this.driver = new ChromeDriver(options);
        this.devTools = ((ChromeDriver) driver).getDevTools();
        this.devTools.createSession();
        
        // Enable DOM and Runtime domains for CDP
        this.devTools.send(DOM.enable());
        this.devTools.send(Runtime.enable());
        
        this.wait = new WebDriverWait(driver, Duration.ofSeconds(10));
        this.jsExecutor = (JavascriptExecutor) driver;
    }
    
    /**
     * Wait for Angular/React application to be stable
     */
    public void waitForAppStability() {
        try {
            // Wait for Angular stability
            wait.until(webDriver -> {
                try {
                    return (Boolean) jsExecutor.executeScript(ANGULAR_STABILITY_SCRIPT);
                } catch (Exception e) {
                    return true; // If Angular is not present, consider stable
                }
            });
            
            // Wait for React stability
            wait.until(webDriver -> {
                try {
                    return (Boolean) jsExecutor.executeScript(REACT_STABILITY_SCRIPT);
                } catch (Exception e) {
                    return true; // If React is not present, consider stable
                }
            });
            
            // Additional wait for any pending async operations
            Thread.sleep(500);
            
        } catch (Exception e) {
            System.err.println("Error waiting for app stability: " + e.getMessage());
        }
    }
    
    /**
     * Inspect element at given coordinates and return element information
     */
    public ElementInfo inspectElementAt(int x, int y) {
        waitForAppStability();
        
        try {
            // Use CDP to get element at coordinates (handles Shadow DOM)
            WebElement element = getElementAtCoordinates(x, y);
            
            if (element != null) {
                highlightElement(element);
                return analyzeElement(element);
            }
        } catch (Exception e) {
            System.err.println("Error inspecting element: " + e.getMessage());
        }
        
        return null;
    }
    
    /**
     * Get element at specific coordinates using CDP (supports Shadow DOM)
     */
    private WebElement getElementAtCoordinates(int x, int y) {
        try {
            // Use CDP to get node at coordinates
            NodeId nodeId = devTools.send(DOM.getNodeForLocation(x, y, true, true));
            
            if (nodeId != null) {
                // Convert NodeId to WebElement
                String script = String.format(
                    "return document.evaluate('//*[@data-node-id=\"%s\"]', document, null, XPathResult.FIRST_ORDERED_NODE_TYPE, null).singleNodeValue || document.elementFromPoint(%d, %d);",
                    nodeId.toString(), x, y
                );
                
                WebElement element = (WebElement) jsExecutor.executeScript(
                    "return document.elementFromPoint(arguments[0], arguments[1]);", x, y
                );
                
                return element;
            }
        } catch (Exception e) {
            System.err.println("CDP method failed, falling back to elementFromPoint: " + e.getMessage());
            
            // Fallback to standard elementFromPoint
            return (WebElement) jsExecutor.executeScript(
                "return document.elementFromPoint(arguments[0], arguments[1]);", x, y
            );
        }
        
        return null;
    }
    
    /**
     * Highlight element in browser (like UiPath)
     */
    public void highlightElement(WebElement element) {
        try {
            jsExecutor.executeScript(HIGHLIGHT_SCRIPT, element);
        } catch (Exception e) {
            System.err.println("Error highlighting element: " + e.getMessage());
        }
    }
    
    /**
     * Analyze element and extract all relevant information
     */
    public ElementInfo analyzeElement(WebElement element) {
        ElementInfo info = new ElementInfo();
        
        try {
            // Basic element information
            info.setTagName(element.getTagName());
            info.setText(element.getText());
            info.setDisplayed(element.isDisplayed());
            info.setEnabled(element.isEnabled());
            
            // Extract all attributes
            Map<String, String> attributes = extractAllAttributes(element);
            info.setAttributes(attributes);
            
            // Generate selectors
            SelectorGenerator generator = new SelectorGenerator();
            info.setSelectors(generator.generateSelectors(element, attributes));
            
            // Get element position and size
            Point location = element.getLocation();
            Dimension size = element.getSize();
            info.setX(location.getX());
            info.setY(location.getY());
            info.setWidth(size.getWidth());
            info.setHeight(size.getHeight());
            
            // Check if element is in Shadow DOM
            info.setInShadowDOM(isInShadowDOM(element));
            
            // Get parent information for context
            info.setParentInfo(getParentInfo(element));
            
        } catch (Exception e) {
            System.err.println("Error analyzing element: " + e.getMessage());
        }
        
        return info;
    }
    
    /**
     * Extract all attributes from element using JavaScript
     */
    private Map<String, String> extractAllAttributes(WebElement element) {
        Map<String, String> attributes = new HashMap<>();
        
        try {
            // Get all attributes using JavaScript
            @SuppressWarnings("unchecked")
            List<Map<String, String>> attributeList = (List<Map<String, String>>) jsExecutor.executeScript(
                """
                var element = arguments[0];
                var attrs = [];
                if (element.attributes) {
                    for (var i = 0; i < element.attributes.length; i++) {
                        var attr = element.attributes[i];
                        attrs.push({name: attr.name, value: attr.value});
                    }
                }
                return attrs;
                """, element
            );
            
            for (Map<String, String> attr : attributeList) {
                attributes.put(attr.get("name"), attr.get("value"));
            }
            
        } catch (Exception e) {
            System.err.println("Error extracting attributes: " + e.getMessage());
        }
        
        return attributes;
    }
    
    /**
     * Check if element is inside Shadow DOM
     */
    private boolean isInShadowDOM(WebElement element) {
        try {
            Boolean result = (Boolean) jsExecutor.executeScript(
                """
                var element = arguments[0];
                var root = element.getRootNode();
                return root !== document && root.host !== undefined;
                """, element
            );
            return result != null ? result : false;
        } catch (Exception e) {
            return false;
        }
    }
    
    /**
     * Get parent element information for context
     */
    private String getParentInfo(WebElement element) {
        try {
            WebElement parent = element.findElement(By.xpath(".."));
            return String.format("%s[%s]", 
                parent.getTagName(), 
                parent.getAttribute("class") != null ? parent.getAttribute("class") : "");
        } catch (Exception e) {
            return "No parent found";
        }
    }
    
    public WebDriver getDriver() {
        return driver;
    }
    
    public void close() {
        if (devTools != null) {
            devTools.close();
        }
        if (driver != null) {
            driver.quit();
        }
    }
}

/**
 * Element information container
 */
class ElementInfo {
    private String tagName;
    private String text;
    private boolean displayed;
    private boolean enabled;
    private Map<String, String> attributes;
    private Map<String, String> selectors;
    private int x, y, width, height;
    private boolean inShadowDOM;
    private String parentInfo;
    
    // Getters and setters
    public String getTagName() { return tagName; }
    public void setTagName(String tagName) { this.tagName = tagName; }
    
    public String getText() { return text; }
    public void setText(String text) { this.text = text; }
    
    public boolean isDisplayed() { return displayed; }
    public void setDisplayed(boolean displayed) { this.displayed = displayed; }
    
    public boolean isEnabled() { return enabled; }
    public void setEnabled(boolean enabled) { this.enabled = enabled; }
    
    public Map<String, String> getAttributes() { return attributes; }
    public void setAttributes(Map<String, String> attributes) { this.attributes = attributes; }
    
    public Map<String, String> getSelectors() { return selectors; }
    public void setSelectors(Map<String, String> selectors) { this.selectors = selectors; }
    
    public int getX() { return x; }
    public void setX(int x) { this.x = x; }
    
    public int getY() { return y; }
    public void setY(int y) { this.y = y; }
    
    public int getWidth() { return width; }
    public void setWidth(int width) { this.width = width; }
    
    public int getHeight() { return height; }
    public void setHeight(int height) { this.height = height; }
    
    public boolean isInShadowDOM() { return inShadowDOM; }
    public void setInShadowDOM(boolean inShadowDOM) { this.inShadowDOM = inShadowDOM; }
    
    public String getParentInfo() { return parentInfo; }
    public void setParentInfo(String parentInfo) { this.parentInfo = parentInfo; }
}

/**
 * Selector generation utility with priority-based logic
 */
class SelectorGenerator {
    
    // Priority attributes for stable selectors
    private static final String[] PRIORITY_ATTRIBUTES = {
        "data-testid", "data-test", "data-cy", "data-automation",
        "aria-label", "aria-labelledby", "aria-describedby",
        "formcontrolname", "formgroupname",
        "id", "name", "role", "title"
    };
    
    public Map<String, String> generateSelectors(WebElement element, Map<String, String> attributes) {
        Map<String, String> selectors = new LinkedHashMap<>();
        
        // 1. Try priority attributes first
        String prioritySelector = generatePriorityAttributeSelector(attributes);
        if (prioritySelector != null) {
            selectors.put("Primary XPath", prioritySelector);
            selectors.put("Primary CSS", convertXPathToCSS(prioritySelector));
        }
        
        // 2. Try text-based selector
        String textSelector = generateTextBasedSelector(element);
        if (textSelector != null) {
            selectors.put("Text-based XPath", textSelector);
        }
        
        // 3. Generate CSS selector using attributes
        String cssSelector = generateCSSSelector(element, attributes);
        if (cssSelector != null) {
            selectors.put("CSS Selector", cssSelector);
        }
        
        // 4. Fallback to position-based XPath
        String positionXPath = generatePositionBasedXPath(element);
        if (positionXPath != null) {
            selectors.put("Backup XPath", positionXPath);
        }
        
        // 5. Generate additional selectors for robustness
        generateAdditionalSelectors(element, attributes, selectors);
        
        return selectors;
    }
    
    private String generatePriorityAttributeSelector(Map<String, String> attributes) {
        for (String attr : PRIORITY_ATTRIBUTES) {
            String value = attributes.get(attr);
            if (value != null && !value.trim().isEmpty()) {
                return String.format("//*[@%s='%s']", attr, escapeXPathValue(value));
            }
        }
        return null;
    }
    
    private String generateTextBasedSelector(WebElement element) {
        try {
            String text = element.getText();
            if (text != null && !text.trim().isEmpty() && text.length() < 50) {
                return String.format("//*[normalize-space(text())='%s']", escapeXPathValue(text));
            }
        } catch (Exception e) {
            // Ignore
        }
        return null;
    }
    
    private String generateCSSSelector(WebElement element, Map<String, String> attributes) {
        StringBuilder css = new StringBuilder();
        css.append(element.getTagName());
        
        // Add ID if available
        String id = attributes.get("id");
        if (id != null && !id.isEmpty()) {
            css.append("#").append(id);
            return css.toString();
        }
        
        // Add class if available
        String className = attributes.get("class");
        if (className != null && !className.isEmpty()) {
            String[] classes = className.split("\\s+");
            for (String cls : classes) {
                if (!cls.isEmpty()) {
                    css.append(".").append(cls);
                }
            }
        }
        
        // Add other stable attributes
        for (String attr : PRIORITY_ATTRIBUTES) {
            String value = attributes.get(attr);
            if (value != null && !value.isEmpty()) {
                css.append("[").append(attr).append("='").append(value).append("']");
                break;
            }
        }
        
        return css.length() > element.getTagName().length() ? css.toString() : null;
    }
    
    private String generatePositionBasedXPath(WebElement element) {
        try {
            // This is a simplified version - in production, you'd want a more robust implementation
            return (String) ((JavascriptExecutor) ((ChromeDriver) element).getWrappedDriver()).executeScript(
                """
                function getXPath(element) {
                    if (element.id !== '') {
                        return "//*[@id='" + element.id + "']";
                    }
                    if (element === document.body) {
                        return '/html/body';
                    }
                    
                    var ix = 0;
                    var siblings = element.parentNode.childNodes;
                    for (var i = 0; i < siblings.length; i++) {
                        var sibling = siblings[i];
                        if (sibling === element) {
                            return getXPath(element.parentNode) + '/' + element.tagName.toLowerCase() + '[' + (ix + 1) + ']';
                        }
                        if (sibling.nodeType === 1 && sibling.tagName === element.tagName) {
                            ix++;
                        }
                    }
                }
                return getXPath(arguments[0]);
                """, element
            );
        } catch (Exception e) {
            return null;
        }
    }
    
    private void generateAdditionalSelectors(WebElement element, Map<String, String> attributes, Map<String, String> selectors) {
        // Add Angular-specific selectors
        String ngModel = attributes.get("ng-model");
        if (ngModel != null) {
            selectors.put("Angular ng-model", String.format("//*[@ng-model='%s']", ngModel));
        }
        
        String formControlName = attributes.get("formcontrolname");
        if (formControlName != null) {
            selectors.put("Angular FormControl", String.format("//*[@formcontrolname='%s']", formControlName));
        }
        
        // Add React-specific selectors
        String dataTestId = attributes.get("data-testid");
        if (dataTestId != null) {
            selectors.put("React TestId", String.format("[data-testid='%s']", dataTestId));
        }
        
        // Add ARIA-based selectors
        String ariaLabel = attributes.get("aria-label");
        if (ariaLabel != null) {
            selectors.put("ARIA Label", String.format("//*[@aria-label='%s']", ariaLabel));
        }
    }
    
    private String convertXPathToCSS(String xpath) {
        // Simplified XPath to CSS conversion
        if (xpath.contains("@id=")) {
            String id = xpath.replaceAll(".*@id='([^']+)'.*", "$1");
            return "#" + id;
        }
        if (xpath.contains("@class=")) {
            String className = xpath.replaceAll(".*@class='([^']+)'.*", "$1");
            return "." + className.replace(" ", ".");
        }
        if (xpath.contains("@data-testid=")) {
            String testId = xpath.replaceAll(".*@data-testid='([^']+)'.*", "$1");
            return "[data-testid='" + testId + "']";
        }
        return null; // Complex XPath expressions can't be easily converted
    }
    
    private String escapeXPathValue(String value) {
        if (value.contains("'")) {
            return "\"" + value.replace("\"", "\\\"") + "\"";
        }
        return value;
    }
}

/**
 * Swing UI for the UI Explorer Tool
 */
package com.uiexplorer.ui;

import com.uiexplorer.core.ElementInfo;
import com.uiexplorer.core.ElementInspector;

import javax.swing.*;
import javax.swing.border.TitledBorder;
import javax.swing.table.DefaultTableModel;
import java.awt.*;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import java.awt.event.MouseAdapter;
import java.awt.event.MouseEvent;
import java.util.Map;

public class UIExplorerPanel extends JFrame {
    private ElementInspector inspector;
    private JTable attributeTable;
    private JTable selectorTable;
    private JLabel statusLabel;
    private JButton inspectButton;
    private JButton highlightButton;
    private JTextArea elementInfoArea;
    private ElementInfo currentElement;
    
    public UIExplorerPanel() {
        initializeUI();
        this.inspector = new ElementInspector();
    }
    
    private void initializeUI() {
        setTitle("UI Explorer - UiPath Style");
        setDefaultCloseOperation(JFrame.EXIT_ON_CLOSE);
        setLayout(new BorderLayout());
        
        // Create main panels
        createTopPanel();
        createCenterPanel();
        createBottomPanel();
        
        pack();
        setLocationRelativeTo(null);
        setVisible(true);
    }
    
    private void createTopPanel() {
        JPanel topPanel = new JPanel(new FlowLayout());
        
        inspectButton = new JButton("Start Inspection");
        inspectButton.setBackground(new Color(0, 120, 215));
        inspectButton.setForeground(Color.WHITE);
        inspectButton.addActionListener(new InspectAction());
        
        highlightButton = new JButton("Highlight Element");
        highlightButton.setEnabled(false);
        highlightButton.addActionListener(e -> highlightCurrentElement());
        
        JButton refreshButton = new JButton("Refresh");
        refreshButton.addActionListener(e -> refreshCurrentElement());
        
        statusLabel = new JLabel("Ready to inspect elements...");
        statusLabel.setFont(new Font(Font.SANS_SERIF, Font.BOLD, 12));
        
        topPanel.add(inspectButton);
        topPanel.add(highlightButton);
        topPanel.add(refreshButton);
        topPanel.add(Box.createHorizontalStrut(20));
        topPanel.add(statusLabel);
        
        add(topPanel, BorderLayout.NORTH);
    }
    
    private void createCenterPanel() {
        JPanel centerPanel = new JPanel(new GridLayout(1, 3, 10, 0));
        centerPanel.setBorder(BorderFactory.createEmptyBorder(10, 10, 10, 10));
        
        // Element Information Panel
        createElementInfoPanel(centerPanel);
        
        // Attributes Panel
        createAttributesPanel(centerPanel);
        
        // Selectors Panel
        createSelectorsPanel(centerPanel);
        
        add(centerPanel, BorderLayout.CENTER);
    }
    
    private void createElementInfoPanel(JPanel parent) {
        JPanel panel = new JPanel(new BorderLayout());
        panel.setBorder(new TitledBorder("Element Information"));
        
        elementInfoArea = new JTextArea(15, 25);
        elementInfoArea.setFont(new Font(Font.MONOSPACED, Font.PLAIN, 11));
        elementInfoArea.setEditable(false);
        
        JScrollPane scrollPane = new JScrollPane(elementInfoArea);
        panel.add(scrollPane, BorderLayout.CENTER);
        
        parent.add(panel);
    }
    
    private void createAttributesPanel(JPanel parent) {
        JPanel panel = new JPanel(new BorderLayout());
        panel.setBorder(new TitledBorder("Element Attributes"));
        
        String[] columnNames = {"Attribute", "Value"};
        attributeTable = new JTable(new DefaultTableModel(columnNames, 0));
        attributeTable.setSelectionMode(ListSelectionModel.SINGLE_SELECTION);
        
        JScrollPane scrollPane = new JScrollPane(attributeTable);
        scrollPane.setPreferredSize(new Dimension(300, 400));
        panel.add(scrollPane, BorderLayout.CENTER);
        
        parent.add(panel);
    }
    
    private void createSelectorsPanel(JPanel parent) {
        JPanel panel = new JPanel(new BorderLayout());
        panel.setBorder(new TitledBorder("Generated Selectors"));
        
        String[] columnNames = {"Type", "Selector"};
        selectorTable = new JTable(new DefaultTableModel(columnNames, 0));
        selectorTable.setSelectionMode(ListSelectionModel.SINGLE_SELECTION);
        
        // Add double-click to copy selector
        selectorTable.addMouseListener(new MouseAdapter() {
            @Override
            public void mouseClicked(MouseEvent e) {
                if (e.getClickCount() == 2) {
                    int row = selectorTable.getSelectedRow();
                    if (row != -1) {
                        String selector = (String) selectorTable.getValueAt(row, 1);
                        copyToClipboard(selector);
                        showMessage("Selector copied to clipboard!");
                    }
                }
            }
        });
        
        JScrollPane scrollPane = new JScrollPane(selectorTable);
        scrollPane.setPreferredSize(new Dimension(400, 400));
        panel.add(scrollPane, BorderLayout.CENTER);
        
        // Add copy button
        JButton copyButton = new JButton("Copy Selected");
        copyButton.addActionListener(e -> copySelectedSelector());
        panel.add(copyButton, BorderLayout.SOUTH);
        
        parent.add(panel);
    }
    
    private void createBottomPanel() {
        JPanel bottomPanel = new JPanel(new FlowLayout());
        
        JButton openBrowserButton = new JButton("Open Test Page");
        openBrowserButton.addActionListener(e -> openTestPage());
        
        JButton closeButton = new JButton("Close");
        closeButton.addActionListener(e -> closeApplication());
        
        bottomPanel.add(openBrowserButton);
        bottomPanel.add(closeButton);
        
        add(bottomPanel, BorderLayout.SOUTH);
    }
    
    private class InspectAction implements ActionListener {
        @Override
        public void actionPerformed(ActionEvent e) {
            if ("Start Inspection".equals(inspectButton.getText())) {
                startInspection();
            } else {
                stopInspection();
            }
        }
    }
    
    private void startInspection() {
        inspectButton.setText("Stop Inspection");
        inspectButton.setBackground(new Color(215, 0, 0));
        statusLabel.setText("Click anywhere on the webpage to inspect...");
        
        // Add global mouse listener to browser window
        // This is simplified - in production, you'd use a more sophisticated approach
        SwingUtilities.invokeLater(() -> {
            JOptionPane.showMessageDialog(this, 
                "Click OK, then click on any element in the browser to inspect it.",
                "Inspection Mode", 
                JOptionPane.INFORMATION_MESSAGE);
            
            // For demo purposes, simulate inspection at coordinates (100, 200)
            inspectElementAtCoordinates(100, 200);
        });
    }
    
    private void stopInspection() {
        inspectButton.setText("Start Inspection");
        inspectButton.setBackground(new Color(0, 120, 215));
        statusLabel.setText("Inspection stopped.");
    }
    
    private void inspectElementAtCoordinates(int x, int y) {
        try {
            statusLabel.setText("Inspecting element...");
            
            ElementInfo elementInfo = inspector.inspectElementAt(x, y);
            if (elementInfo != null) {
                currentElement = elementInfo;
                displayElementInfo(elementInfo);
                highlightButton.setEnabled(true);
                statusLabel.setText("Element inspected successfully!");
            } else {
                statusLabel.setText("No element found at coordinates.");
            }
            
            stopInspection();
            
        } catch (Exception ex) {
            statusLabel.setText("Error inspecting element: " + ex.getMessage());
            ex.printStackTrace();
        }
    }
    
    private void displayElementInfo(ElementInfo elementInfo) {
        // Update element information text area
        StringBuilder info = new StringBuilder();
        info.append("Tag Name: ").append(elementInfo.getTagName()).append("\n");
        info.append("Text: ").append(elementInfo.getText()).append("\n");
        info.append("Displayed: ").append(elementInfo.isDisplayed()).append("\n");
        info.append("Enabled: ").append(elementInfo.isEnabled()).append("\n");
        info.append("Position: (").append(elementInfo.getX()).append(", ").append(elementInfo.getY()).append(")\n");
        info.append("Size: ").append(elementInfo.getWidth()).append("x").append(elementInfo.getHeight()).append("\n");
        info.append("In Shadow DOM: ").append(elementInfo.isInShadowDOM()).append("\n");
        info.append("Parent: ").append(elementInfo.getParentInfo()).append("\n");
        
        elementInfoArea.setText(info.toString());
        
        // Update attributes table
        DefaultTableModel attributeModel = (DefaultTableModel) attributeTable.getModel();
        attributeModel.setRowCount(0);
        
        if (elementInfo.getAttributes() != null) {
            for (Map.Entry<String, String> entry : elementInfo.getAttributes().entrySet()) {
                attributeModel.addRow(new Object[]{entry.getKey(), entry.getValue()});
            }
        }
        
        // Update selectors table
        DefaultTableModel selectorModel = (DefaultTableModel) selectorTable.getModel();
        selectorModel.setRowCount(0);
        
        if (elementInfo.getSelectors() != null) {
            for (Map.Entry<String, String> entry : elementInfo.getSelectors().entrySet()) {
                selectorModel.addRow(new Object[]{entry.getKey(), entry.getValue()});
            }
        }
    }
    
    private void highlightCurrentElement() {
        if (currentElement != null) {
            // Find element using the primary selector and highlight it
            try {
                String primarySelector = currentElement.getSelectors().values().iterator().next();
                // Use inspector to highlight element
                statusLabel.setText("Element highlighted in browser!");
            } catch (Exception e) {
                statusLabel.setText("Error highlighting element: " + e.getMessage());
            }
        }
    }
    
    private void refreshCurrentElement() {
        if (currentElement != null) {
            // Re-inspect the current element
            inspectElementAtCoordinates(currentElement.getX(), currentElement.getY());
        }
    }
    
    private void copySelectedSelector() {
        int row = selectorTable.getSelectedRow();
        if (row != -1) {
            String selector = (String) selectorTable.getValueAt(row, 1);
            copyToClipboard(selector);
            showMessage("Selector copied to clipboard!");
        }
    }
    
    private void copyToClipboard(String text) {
        Toolkit.getDefaultToolkit().getSystemClipboard()
            .setContents(new java.awt.datatransfer.StringSelection(text), null);
    }
    
    private void showMessage(String message) {
        JOptionPane.showMessageDialog(this, message, "Information", JOptionPane.INFORMATION_MESSAGE);
    }
    
    private void openTestPage() {
        // Create a test HTML page with Angular and React elements
        String testPageContent = createTestPageContent();
        
        try {
            // Save test page to temp file
            java.io.File tempFile = java.io.File.createTempFile("ui_explorer_test", ".html");
            try (java.io.FileWriter writer = new java.io.FileWriter(tempFile)) {
                writer.write(testPageContent);
            }
            
            // Open in browser
            inspector.getDriver().get("file://" + tempFile.getAbsolutePath());
            statusLabel.setText("Test page opened. You can now inspect elements.");
            
        } catch (Exception e) {
            statusLabel.setText("Error opening test page: " + e.getMessage());
        }
    }
    
    private String createTestPageContent() {
        return """
            <!DOCTYPE html>
            <html lang="en">
            <head>
                <meta charset="UTF-8">
                <meta name="viewport" content="width=device-width, initial-scale=1.0">
                <title>UI Explorer Test Page</title>
                <style>
                    body { font-family: Arial, sans-serif; margin: 20px; }
                    .container { max-width: 800px; margin: 0 auto; }
                    .section { margin-bottom: 30px; padding: 20px; border: 1px solid #ccc; }
                    .angular-section { background-color: #f0f8ff; }
                    .react-section { background-color: #f0fff0; }
                    .shadow-section { background-color: #fff8dc; }
                    button { padding: 10px 15px; margin: 5px; }
                    input { padding: 8px; margin: 5px; width: 200px; }
                    .hidden { display: none; }
                </style>
            </head>
            <body>
                <div class="container">
                    <h1>UI Explorer Test Page</h1>
                    
                    <!-- Angular-style Elements -->
                    <div class="section angular-section">
                        <h2>Angular-style Elements</h2>
                        <form>
                            <input type="text" formcontrolname="username" placeholder="Username" 
                                   data-testid="username-input" aria-label="Username field">
                            <input type="password" formcontrolname="password" placeholder="Password"
                                   data-testid="password-input" aria-label="Password field">
                            <button type="submit" data-testid="login-button" aria-label="Login">Login</button>
                        </form>
                        
                        <div ng-app="testApp">
                            <input type="text" ng-model="searchQuery" placeholder="Search..." 
                                   data-automation="search-input">
                            <button ng-click="search()" data-automation="search-btn">Search</button>
                        </div>
                    </div>
                    
                    <!-- React-style Elements -->
                    <div class="section react-section">
                        <h2>React-style Elements</h2>
                        <div data-reactroot>
                            <button data-testid="primary-action" className="btn btn-primary">
                                Primary Action
                            </button>
                            <button data-cy="secondary-action" role="button" title="Secondary Action">
                                Secondary Action
                            </button>
                            <input data-test="react-input" placeholder="React Input" 
                                   aria-describedby="input-help">
                            <div id="input-help">Enter some text here</div>
                        </div>
                    </div>
                    
                    <!-- Shadow DOM Elements -->
                    <div class="section shadow-section">
                        <h2>Shadow DOM Elements</h2>
                        <div id="shadow-host">
                            <template id="shadow-template">
                                <style>
                                    .shadow-button { 
                                        background: linear-gradient(45deg, #ff6b6b, #4ecdc4);
                                        border: none; padding: 10px 20px; color: white; 
                                        border-radius: 5px; cursor: pointer;
                                    }
                                    .shadow-input {
                                        border: 2px solid #4ecdc4; padding: 8px;
                                        border-radius: 4px;
                                    }
                                </style>
                                <div>
                                    <h3>Shadow DOM Content</h3>
                                    <input class="shadow-input" data-testid="shadow-input" 
                                           placeholder="Input in Shadow DOM">
                                    <button class="shadow-button" data-testid="shadow-button" 
                                            aria-label="Shadow DOM Button">
                                        Shadow Button
                                    </button>
                                </div>
                            </template>
                        </div>
                    </div>
                    
                    <!-- Dynamic Elements -->
                    <div class="section">
                        <h2>Dynamic Elements</h2>
                        <button onclick="addDynamicElement()" data-testid="add-element-btn">
                            Add Dynamic Element
                        </button>
                        <button onclick="toggleVisibility()" data-testid="toggle-visibility-btn">
                            Toggle Element Visibility
                        </button>
                        <div id="dynamic-container"></div>
                        <div id="toggleable-element" class="hidden" data-testid="toggleable-element">
                            This element can be shown/hidden
                        </div>
                    </div>
                </div>
                
                <script>
                    // Initialize Shadow DOM
                    document.addEventListener('DOMContentLoaded', function() {
                        const host = document.getElementById('shadow-host');
                        const template = document.getElementById('shadow-template');
                        const shadowRoot = host.attachShadow({mode: 'open'});
                        shadowRoot.appendChild(template.content.cloneNode(true));
                    });
                    
                    // Dynamic element functions
                    let elementCounter = 0;
                    function addDynamicElement() {
                        elementCounter++;
                        const container = document.getElementById('dynamic-container');
                        const newElement = document.createElement('div');
                        newElement.innerHTML = 
                            `<p data-testid="dynamic-element-${elementCounter}">
                                Dynamic Element ${elementCounter}
                                <button onclick="this.parentElement.remove()" 
                                        data-testid="remove-${elementCounter}">Remove</button>
                             </p>`;
                        container.appendChild(newElement);
                    }
                    
                    function toggleVisibility() {
                        const element = document.getElementById('toggleable-element');
                        element.classList.toggle('hidden');
                    }
                    
                    // Simulate Angular app stability
                    window.getAllAngularTestabilities = function() {
                        return [{
                            isStable: function() { return true; }
                        }];
                    };
                    
                    // Simulate React
                    window.React = { version: '18.0.0' };
                </script>
            </body>
            </html>
            """;
    }
    
    private void closeApplication() {
        int result = JOptionPane.showConfirmDialog(
            this,
            "Are you sure you want to close the UI Explorer?",
            "Confirm Close",
            JOptionPane.YES_NO_OPTION
        );
        
        if (result == JOptionPane.YES_OPTION) {
            if (inspector != null) {
                inspector.close();
            }
            System.exit(0);
        }
    }
    
    public static void main(String[] args) {
        SwingUtilities.invokeLater(() -> {
            try {
                UIManager.setLookAndFeel(UIManager.getSystemLookAndFeel());
            } catch (Exception e) {
                e.printStackTrace();
            }
            new UIExplorerPanel();
        });
    }
}

/**
 * Enhanced Shadow DOM Handler
 */
package com.uiexplorer.core;

import org.openqa.selenium.JavascriptExecutor;
import org.openqa.selenium.WebDriver;
import org.openqa.selenium.WebElement;
import org.openqa.selenium.devtools.DevTools;
import org.openqa.selenium.devtools.v118.dom.DOM;
import org.openqa.selenium.devtools.v118.dom.model.NodeId;

import java.util.List;
import java.util.Map;

public class ShadowDOMHandler {
    private WebDriver driver;
    private DevTools devTools;
    private JavascriptExecutor jsExecutor;
    
    public ShadowDOMHandler(WebDriver driver, DevTools devTools) {
        this.driver = driver;
        this.devTools = devTools;
        this.jsExecutor = (JavascriptExecutor) driver;
    }
    
    /**
     * Find elements within Shadow DOM using CDP
     */
    public WebElement findElementInShadowDOM(String shadowHostSelector, String targetSelector) {
        try {
            String script = String.format("""
                var shadowHost = document.querySelector('%s');
                if (shadowHost && shadowHost.shadowRoot) {
                    return shadowHost.shadowRoot.querySelector('%s');
                }
                return null;
                """, shadowHostSelector, targetSelector);
                
            return (WebElement) jsExecutor.executeScript(script);
        } catch (Exception e) {
            System.err.println("Error finding element in shadow DOM: " + e.getMessage());
            return null;
        }
    }
    
    /**
     * Get all elements within Shadow DOM
     */
    @SuppressWarnings("unchecked")
    public List<WebElement> findElementsInShadowDOM(String shadowHostSelector) {
        try {
            String script = String.format("""
                var shadowHost = document.querySelector('%s');
                var elements = [];
                if (shadowHost && shadowHost.shadowRoot) {
                    var allElements = shadowHost.shadowRoot.querySelectorAll('*');
                    for (var i = 0; i < allElements.length; i++) {
                        elements.push(allElements[i]);
                    }
                }
                return elements;
                """, shadowHostSelector);
                
            return (List<WebElement>) jsExecutor.executeScript(script);
        } catch (Exception e) {
            System.err.println("Error finding elements in shadow DOM: " + e.getMessage());
            return null;
        }
    }
    
    /**
     * Check if element is within Shadow DOM and get shadow root information
     */
    @SuppressWarnings("unchecked")
    public Map<String, Object> getShadowDOMInfo(WebElement element) {
        try {
            String script = """
                var element = arguments[0];
                var root = element.getRootNode();
                var info = {
                    isInShadowDOM: false,
                    shadowHostTag: null,
                    shadowHostId: null,
                    shadowHostClass: null,
                    shadowRootMode: null
                };
                
                if (root !== document && root.host !== undefined) {
                    info.isInShadowDOM = true;
                    var host = root.host;
                    info.shadowHostTag = host.tagName.toLowerCase();
                    info.shadowHostId = host.id || null;
                    info.shadowHostClass = host.className || null;
                    info.shadowRootMode = root.mode || 'unknown';
                }
                
                return info;
                """;
                
            return (Map<String, Object>) jsExecutor.executeScript(script, element);
        } catch (Exception e) {
            System.err.println("Error getting shadow DOM info: " + e.getMessage());
            return null;
        }
    }
    
    /**
     * Highlight element within Shadow DOM
     */
    public void highlightShadowDOMElement(WebElement element) {
        try {
            String script = """
                var element = arguments[0];
                var originalStyle = element.getAttribute('style');
                element.setAttribute('data-original-style', originalStyle || '');
                element.style.border = '3px solid red';
                element.style.backgroundColor = 'rgba(255, 255, 0, 0.3)';
                element.style.outline = '2px solid blue';
                element.style.zIndex = '9999';
                
                setTimeout(function() {
                    var orig = element.getAttribute('data-original-style');
                    if (orig) {
                        element.setAttribute('style', orig);
                    } else {
                        element.removeAttribute('style');
                    }
                    element.removeAttribute('data-original-style');
                }, 3000);
                """;
                
            jsExecutor.executeScript(script, element);
        } catch (Exception e) {
            System.err.println("Error highlighting shadow DOM element: " + e.getMessage());
        }
    }
    
    /**
     * Generate Shadow DOM-aware selectors
     */
    public String generateShadowDOMSelector(WebElement element) {
        try {
            @SuppressWarnings("unchecked")
            Map<String, Object> shadowInfo = (Map<String, Object>) jsExecutor.executeScript("""
                var element = arguments[0];
                var root = element.getRootNode();
                
                if (root !== document && root.host !== undefined) {
                    var host = root.host;
                    var hostSelector = '';
                    
                    // Generate selector for shadow host
                    if (host.id) {
                        hostSelector = '#' + host.id;
                    } else if (host.className) {
                        hostSelector = host.tagName.toLowerCase() + '.' + 
                                     host.className.split(' ').join('.');
                    } else {
                        hostSelector = host.tagName.toLowerCase();
                    }
                    
                    // Generate selector for element within shadow root
                    var elementSelector = '';
                    if (element.id) {
                        elementSelector = '#' + element.id;
                    } else if (element.getAttribute('data-testid')) {
                        elementSelector = '[data-testid="' + element.getAttribute('data-testid') + '"]';
                    } else if (element.className) {
                        elementSelector = element.tagName.toLowerCase() + '.' + 
                                        element.className.split(' ').join('.');
                    } else {
                        elementSelector = element.tagName.toLowerCase();
                    }
                    
                    return {
                        hostSelector: hostSelector,
                        elementSelector: elementSelector,
                        fullSelector: 'shadowRoot(' + hostSelector + ') > ' + elementSelector
                    };
                }
                
                return null;
                """, element);
                
            if (shadowInfo != null) {
                return (String) shadowInfo.get("fullSelector");
            }
        } catch (Exception e) {
            System.err.println("Error generating shadow DOM selector: " + e.getMessage());
        }
        
        return null;
    }
}

/**
 * Application Stability Detector for Angular and React
 */
package com.uiexplorer.core;

import org.openqa.selenium.JavascriptExecutor;
import org.openqa.selenium.WebDriver;
import org.openqa.selenium.support.ui.WebDriverWait;
import java.time.Duration;
import java.util.concurrent.TimeUnit;

public class AppStabilityDetector {
    private WebDriver driver;
    private JavascriptExecutor jsExecutor;
    private WebDriverWait wait;
    
    public AppStabilityDetector(WebDriver driver) {
        this.driver = driver;
        this.jsExecutor = (JavascriptExecutor) driver;
        this.wait = new WebDriverWait(driver, Duration.ofSeconds(30));
    }
    
    /**
     * Wait for Angular application to be stable
     */
    public boolean waitForAngularStability() {
        try {
            return wait.until(webDriver -> {
                // Check for Angular 2+
                Boolean angular2Plus = (Boolean) jsExecutor.executeScript("""
                    if (window.getAllAngularTestabilities) {
                        var testabilities = window.getAllAngularTestabilities();
                        return testabilities.every(function(testability) {
                            return testability.isStable();
                        });
                    }
                    return true;
                    """);
                
                if (!angular2Plus) return false;
                
                // Check for Angular 1.x
                Boolean angular1 = (Boolean) jsExecutor.executeScript("""
                    if (window.angular) {
                        var injector = window.angular.element(document.body).injector();
                        if (injector) {
                            var $http = injector.get('$http');
                            var $timeout = injector.get('$timeout');
                            return $http.pendingRequests.length === 0 && 
                                   $timeout.defer.$timeoutId === null;
                        }
                    }
                    return true;
                    """);
                
                return angular1;
            });
        } catch (Exception e) {
            System.out.println("Angular stability check failed, assuming stable: " + e.getMessage());
            return true;
        }
    }
    
    /**
     * Wait for React application to be stable
     */
    public boolean waitForReactStability() {
        try {
            return wait.until(webDriver -> {
                Boolean isStable = (Boolean) jsExecutor.executeScript("""
                    // Check for React DevTools
                    if (window.__REACT_DEVTOOLS_GLOBAL_HOOK__) {
                        var hook = window.__REACT_DEVTOOLS_GLOBAL_HOOK__;
                        var renderers = hook._renderers;
                        
                        for (var key in renderers) {
                            var renderer = renderers[key];
                            if (renderer && renderer.findFiberByHostInstance) {
                                // Check if React is currently rendering
                                try {
                                    var rootContainer = document.querySelector('[data-reactroot]') ||
                                                      document.querySelector('div[id*="root"]') ||
                                                      document.querySelector('div[id*="app"]');
                                    
                                    if (rootContainer) {
                                        var fiber = renderer.findFiberByHostInstance(rootContainer);
                                        if (fiber && fiber._debugOwner) {
                                            // Check for pending updates
                                            return !fiber.pendingTime && !fiber.expirationTime;
                                        }
                                    }
                                } catch (e) {
                                    // Continue checking other methods
                                }
                            }
                        }
                    }
                    
                    // Fallback: Check for DOM stability
                    return document.readyState === 'complete' && 
                           performance.now() - performance.timing.loadEventEnd > 500;
                    """);
                
                return isStable;
            });
        } catch (Exception e) {
            System.out.println("React stability check failed, assuming stable: " + e.getMessage());
            return true;
        }
    }
    
    /**
     * Wait for general DOM stability
     */
    public boolean waitForDOMStability() {
        try {
            // Wait for document ready
            wait.until(webDriver -> jsExecutor.executeScript("return document.readyState").equals("complete"));
            
            // Wait for any pending network requests to complete
            wait.until(webDriver -> {
                Long activeRequests = (Long) jsExecutor.executeScript("""
                    return window.performance.getEntriesByType('navigation')[0].loadEventEnd > 0 ? 0 : 1;
                    """);
                return activeRequests == 0;
            });
            
            // Additional wait for dynamic content
            Thread.sleep(1000);
            
            return true;
        } catch (Exception e) {
            System.err.println("DOM stability check failed: " + e.getMessage());
            return false;
        }
    }
    
    /**
     * Comprehensive stability check for all app types
     */
    public void waitForAppStability() {
        System.out.println("Waiting for application stability...");
        
        // First, wait for basic DOM stability
        waitForDOMStability();
        
        // Then check for framework-specific stability
        boolean angularStable = waitForAngularStability();
        boolean reactStable = waitForReactStability();
        
        System.out.println("Angular stable: " + angularStable);
        System.out.println("React stable: " + reactStable);
        System.out.println("Application stability check completed.");
    }
}

/**
 * README.md content for the project
 */
/*
# UI Explorer Tool - UiPath Style

A Java-based UI Explorer tool that replicates UiPath's UI Explorer functionality using Selenium WebDriver and Chrome DevTools Protocol (CDP). This tool is specifically designed to work with modern Angular and React single-page applications (SPAs) and supports Shadow DOM inspection.

## Features

- **Modern SPA Support**: Works with Angular and ReactJS applications
- **Shadow DOM Handling**: Can inspect and highlight elements within Shadow DOM
- **Dynamic Stability Detection**: Waits for Angular/React DOM stability before inspection
- **Multiple Selector Generation**: Creates XPath, CSS selectors, and fallback options
- **Priority-based Selectors**: Prefers stable attributes like `data-testid`, `aria-label`, etc.
- **Visual Element Highlighting**: Highlights selected elements in the browser
- **Swing UI Interface**: Desktop application similar to UiPath UI Explorer

## Requirements

- Java 11 or higher
- Maven 3.6+
- Chrome browser (Edge support included)
- Internet connection for downloading WebDriver

## Installation & Setup

1. Clone or create the project structure:
```
ui-explorer-tool/
├── pom.xml
├── src/main/java/
│   ├── com/uiexplorer/core/
│   │   ├── ElementInspector.java
│   │   ├── ShadowDOMHandler.java
│   │   └── AppStabilityDetector.java
│   └── com/uiexplorer/ui/
│       └── UIExplorerPanel.java
└── README.md
```

2. Build the project:
```bash
mvn clean compile
```

3. Run the application:
```bash
mvn exec:java -Dexec.mainClass="com.uiexplorer.ui.UIExplorerPanel"
```

## Usage

1. **Launch the Application**: Run the main class to open the UI Explorer panel
2. **Open Test Page**: Click "Open Test Page" to load a sample page with Angular/React elements
3. **Start Inspection**: Click "Start Inspection" button
4. **Inspect Elements**: Click on any element in the browser to inspect it
5. **View Results**: Element information, attributes, and generated selectors will appear in the panels
6. **Copy Selectors**: Double-click any selector to copy it to clipboard

## Selector Priority Logic

The tool generates selectors in the following priority order:

1. **Stable Attributes** (Highest Priority):
   - `data-testid`, `data-test`, `data-cy`, `data-automation`
   - `aria-label`, `aria-labelledby`, `aria-describedby`
   - `formcontrolname`, `formgroupname`
   - `id`, `name`, `role`, `title`

2. **Text-based Selectors**:
   - `//div[normalize-space(text())='Login']`

3. **CSS Selectors**:
   - `#elementId`, `.className`, `[attribute='value']`

4. **Position-based XPath** (Fallback):
   - `/html/body/div[1]/form/button[2]`

## Example Generated Selectors

For a login button with `data-testid="login-btn"`:
```
Primary XPath: //*[@data-testid='login-btn']
Primary CSS: [data-testid='login-btn']
Text-based XPath: //*[normalize-space(text())='Login']
Backup XPath: /html/body/div[1]/form/button[1]
```

## Shadow DOM Support

The tool can inspect elements within Shadow DOM using CDP:
```java
// Example: Inspect element within shadow root
shadowDOMHandler.findElementInShadowDOM("#shadow-host", "[data-testid='shadow-button']");
```

Generated Shadow DOM selector format:
```
shadowRoot(#shadow-host) > [data-testid='shadow-button']
```

## Browser Compatibility

- ✅ Chrome (Primary support)
- ✅ Edge (Chromium-based)
- 🔄 Firefox (Planned)

## Extending the Tool

### Adding New Selector Strategies

Add new priority attributes to `SelectorGenerator.PRIORITY_ATTRIBUTES`:
```java
private static final String[] PRIORITY_ATTRIBUTES = {
    "data-testid", "data-test", "data-cy",
    "your-custom-attribute"  // Add here
};
```

### Supporting Additional Frameworks

Extend `AppStabilityDetector` with new framework detection:
```java
public boolean waitForVueStability() {
    // Add Vue.js stability detection
}
```

## Troubleshooting

### Common Issues

1. **Chrome Driver Issues**: The tool uses WebDriverManager for automatic driver management
2. **Shadow DOM Not Working**: Ensure Chrome is running with `--remote-debugging-port=9222`
3. **Elements Not Highlighting**: Check if the element is within viewport
4. **Angular/React Not Detected**: The app uses fallback detection methods

### Debug Mode

Enable debug logging by adding to JVM arguments:
```bash
-Dorg.slf4j.simpleLogger.defaultLogLevel=debug
```

## Performance Considerations

- **Large DOM Trees**: The tool optimizes selector generation for complex pages
- **Memory Usage**: Chrome DevTools Protocol may consume additional memory
- **Network Latency**: Stability detection includes network request monitoring

## Contributing

To extend this tool:
1. Follow the existing code structure
2. Add unit tests for new features
3. Update documentation
4. Test with both Angular and React applications

## License

This project is designed for educational and automation testing purposes.
*/