package com.enterprise.selenium.keywords.fluent;

import org.openqa.selenium.By;
import org.openqa.selenium.Keys;
import com.enterprise.selenium.keywords.KeyboardEventsHandler;
import com.enterprise.selenium.keywords.KeyboardEventsHandler.KeyboardActionResponse;
import com.enterprise.selenium.keywords.KeyboardEventsHandler.KeyboardActionException;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import java.util.Arrays;
import java.util.List;
import java.util.ArrayList;

/**
 * Fluent API for keyboard events providing chainable methods
 * Example: KeyboardActions.onElement(locator).clearFirst().typeText("Hello").pressEnter()
 */
public class FluentKeyboardActions {
    private static final Logger log = LoggerFactory.getLogger(FluentKeyboardActions.class);
    
    private By locator;
    private Integer timeout;
    private boolean clearBeforeType = false;
    private boolean verifyInput = true;
    private boolean humanLikeTyping = false;
    private int minTypingDelay = 50;
    private int maxTypingDelay = 150;
    
    private FluentKeyboardActions(By locator) {
        this.locator = locator;
    }
    
    /**
     * Create fluent keyboard actions for specific element
     */
    public static FluentKeyboardActions onElement(By locator) {
        return new FluentKeyboardActions(locator);
    }
    
    /**
     * Create fluent keyboard actions for global shortcuts (no specific element)
     */
    public static FluentKeyboardActions global() {
        return new FluentKeyboardActions(null);
    }
    
    /**
     * Set timeout for element operations
     */
    public FluentKeyboardActions withTimeout(int seconds) {
        this.timeout = seconds;
        return this;
    }
    
    /**
     * Clear element before typing
     */
    public FluentKeyboardActions clearFirst() {
        this.clearBeforeType = true;
        return this;
    }
    
    /**
     * Skip clearing element (default behavior)
     */
    public FluentKeyboardActions appendText() {
        this.clearBeforeType = false;
        return this;
    }
    
    /**
     * Disable input verification
     */
    public FluentKeyboardActions skipVerification() {
        this.verifyInput = false;
        return this;
    }
    
    /**
     * Enable human-like typing with default delays
     */
    public FluentKeyboardActions humanLike() {
        this.humanLikeTyping = true;
        return this;
    }
    
    /**
     * Enable human-like typing with custom delays
     */
    public FluentKeyboardActions humanLike(int minDelayMs, int maxDelayMs) {
        this.humanLikeTyping = true;
        this.minTypingDelay = minDelayMs;
        this.maxTypingDelay = maxDelayMs;
        return this;
    }
    
    /**
     * Type text to element
     */
    public KeyboardActionResponse typeText(String text) throws KeyboardActionException {
        validateElementRequired();
        
        if (humanLikeTyping) {
            return KeyboardEventsHandler.typeHumanLike(locator, text, clearBeforeType, 
                                                      minTypingDelay, maxTypingDelay, timeout);
        } else {
            return KeyboardEventsHandler.sendText(locator, text, clearBeforeType, timeout);
        }
    }
    
    /**
     * Press Enter key
     */
    public KeyboardActionResponse pressEnter() throws KeyboardActionException {
        validateElementRequired();
        return KeyboardEventsHandler.sendSpecialKey(locator, Keys.ENTER, timeout);
    }
    
    /**
     * Press Tab key
     */
    public KeyboardActionResponse pressTab() throws KeyboardActionException {
        validateElementRequired();
        return KeyboardEventsHandler.sendSpecialKey(locator, Keys.TAB, timeout);
    }
    
    /**
     * Press Escape key
     */
    public KeyboardActionResponse pressEscape() throws KeyboardActionException {
        validateElementRequired();
        return KeyboardEventsHandler.sendSpecialKey(locator, Keys.ESCAPE, timeout);
    }
    
    /**
     * Press Space key
     */
    public KeyboardActionResponse pressSpace() throws KeyboardActionException {
        validateElementRequired();
        return KeyboardEventsHandler.sendSpecialKey(locator, Keys.SPACE, timeout);
    }
    
    /**
     * Press Backspace key
     */
    public KeyboardActionResponse pressBackspace() throws KeyboardActionException {
        validateElementRequired();
        return KeyboardEventsHandler.sendSpecialKey(locator, Keys.BACK_SPACE, timeout);
    }
    
    /**
     * Press Delete key
     */
    public KeyboardActionResponse pressDelete() throws KeyboardActionException {
        validateElementRequired();
        return KeyboardEventsHandler.sendSpecialKey(locator, Keys.DELETE, timeout);
    }
    
    /**
     * Press arrow keys
     */
    public KeyboardActionResponse pressArrowUp() throws KeyboardActionException {
        validateElementRequired();
        return KeyboardEventsHandler.sendSpecialKey(locator, Keys.ARROW_UP, timeout);
    }
    
    public KeyboardActionResponse pressArrowDown() throws KeyboardActionException {
        validateElementRequired();
        return KeyboardEventsHandler.sendSpecialKey(locator, Keys.ARROW_DOWN, timeout);
    }
    
    public KeyboardActionResponse pressArrowLeft() throws KeyboardActionException {
        validateElementRequired();
        return KeyboardEventsHandler.sendSpecialKey(locator, Keys.ARROW_LEFT, timeout);
    }
    
    public KeyboardActionResponse pressArrowRight() throws KeyboardActionException {
        validateElementRequired();
        return KeyboardEventsHandler.sendSpecialKey(locator, Keys.ARROW_RIGHT, timeout);
    }
    
    /**
     * Press function keys
     */
    public KeyboardActionResponse pressF1() throws KeyboardActionException {
        return KeyboardEventsHandler.sendSpecialKey(locator, Keys.F1, timeout);
    }
    
    public KeyboardActionResponse pressF5() throws KeyboardActionException {
        return KeyboardEventsHandler.sendSpecialKey(locator, Keys.F5, timeout);
    }
    
    public KeyboardActionResponse pressF12() throws KeyboardActionException {
        return KeyboardEventsHandler.sendSpecialKey(locator, Keys.F12, timeout);
    }
    
    /**
     * Common key combinations
     */
    public KeyboardActionResponse selectAll() throws KeyboardActionException {
        validateElementRequired();
        return KeyboardEventsHandler.sendKeyCombo(locator, Arrays.asList(Keys.CONTROL), 
                                                Keys.chord("a"), timeout);
    }
    
    public KeyboardActionResponse copy() throws KeyboardActionException {
        validateElementRequired();
        return KeyboardEventsHandler.sendKeyCombo(locator, Arrays.asList(Keys.CONTROL), 
                                                Keys.chord("c"), timeout);
    }
    
    public KeyboardActionResponse paste() throws KeyboardActionException {
        validateElementRequired();
        return KeyboardEventsHandler.sendKeyCombo(locator, Arrays.asList(Keys.CONTROL), 
                                                Keys.chord("v"), timeout);
    }
    
    public KeyboardActionResponse cut() throws KeyboardActionException {
        validateElementRequired();
        return KeyboardEventsHandler.sendKeyCombo(locator, Arrays.asList(Keys.CONTROL), 
                                                Keys.chord("x"), timeout);
    }
    
    public KeyboardActionResponse undo() throws KeyboardActionException {
        validateElementRequired();
        return KeyboardEventsHandler.sendKeyCombo(locator, Arrays.asList(Keys.CONTROL), 
                                                Keys.chord("z"), timeout);
    }
    
    public KeyboardActionResponse redo() throws KeyboardActionException {
        validateElementRequired();
        return KeyboardEventsHandler.sendKeyCombo(locator, Arrays.asList(Keys.CONTROL), 
                                                Keys.chord("y"), timeout);
    }
    
    /**
     * Save operations
     */
    public KeyboardActionResponse save() throws KeyboardActionException {
        return KeyboardEventsHandler.sendKeyCombo(locator, Arrays.asList(Keys.CONTROL), 
                                                Keys.chord("s"), timeout);
    }
    
    /**
     * Browser shortcuts
     */
    public KeyboardActionResponse refresh() throws KeyboardActionException {
        return KeyboardEventsHandler.sendSpecialKey(null, Keys.F5, timeout);
    }
    
    public KeyboardActionResponse newTab() throws KeyboardActionException {
        return KeyboardEventsHandler.sendKeyCombo(null, Arrays.asList(Keys.CONTROL), 
                                                Keys.chord("t"), timeout);
    }
    
    public KeyboardActionResponse closeTab() throws KeyboardActionException {
        return KeyboardEventsHandler.sendKeyCombo(null, Arrays.asList(Keys.CONTROL), 
                                                Keys.chord("w"), timeout);
    }
    
    /**
     * Custom key combination
     */
    public KeyboardActionResponse keyCombo(List<Keys> modifiers, Keys key) throws KeyboardActionException {
        return KeyboardEventsHandler.sendKeyCombo(locator, modifiers, key, timeout);
    }
    
    /**
     * Custom key combination with string keys
     */
    public KeyboardActionResponse keyCombo(String combo) throws KeyboardActionException {
        // Parse combo string like "CTRL+SHIFT+A"
        String[] parts = combo.toUpperCase().split("\\+");
        if (parts.length < 2) {
            throw new KeyboardActionException("Invalid key combination format: " + combo, 
                                            KeyboardEventsHandler.KeyboardActionType.SEND_KEY_COMBINATION, locator);
        }
        
        List<Keys> modifiers = new ArrayList<>();
        for (int i = 0; i < parts.length - 1; i++) {
            Keys modifier = parseKeyString(parts[i]);
            modifiers.add(modifier);
        }
        
        Keys mainKey = parseKeyString(parts[parts.length - 1]);
        
        return KeyboardEventsHandler.sendKeyCombo(locator, modifiers, mainKey, timeout);
    }
    
    /**
     * Type text and press Enter
     */
    public KeyboardActionResponse typeAndSubmit(String text) throws KeyboardActionException {
        validateElementRequired();
        KeyboardActionResponse typeResponse = typeText(text);
        if (typeResponse.isSuccess()) {
            return pressEnter();
        }
        return typeResponse;
    }
    
    /**
     * Clear field and type new text
     */
    public KeyboardActionResponse replaceText(String newText) throws KeyboardActionException {
        validateElementRequired();
        return clearFirst().typeText(newText);
    }
    
    private void validateElementRequired() throws KeyboardActionException {
        if (locator == null) {
            throw new KeyboardActionException("Element locator is required for this operation", 
                                            KeyboardEventsHandler.KeyboardActionType.SEND_TEXT, null);
        }
    }
    
    private Keys parseKeyString(String keyString) throws KeyboardActionException {
        switch (keyString.toUpperCase()) {
            case "CTRL":
            case "CONTROL": return Keys.CONTROL;
            case "SHIFT": return Keys.SHIFT;
            case "ALT": return Keys.ALT;
            case "CMD":
            case "META": return Keys.META;
            default:
                if (keyString.length() == 1) {
                    return Keys.chord(keyString.toLowerCase());
                } else {
                    throw new KeyboardActionException("Unknown key: " + keyString, 
                                                    KeyboardEventsHandler.KeyboardActionType.SEND_KEY_COMBINATION, locator);
                }
        }
    }
}

/**
 * Keyboard Actions Builder Pattern for Complex Sequences
 */
package com.enterprise.selenium.keywords.builder;

import org.openqa.selenium.By;
import org.openqa.selenium.Keys;
import com.enterprise.selenium.keywords.KeyboardEventsHandler;
import com.enterprise.selenium.keywords.KeyboardEventsHandler.KeyboardActionResponse;
import com.enterprise.selenium.keywords.KeyboardEventsHandler.KeyboardActionException;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import java.util.ArrayList;
import java.util.List;

/**
 * Builder for creating complex keyboard action sequences
 * Example: KeyboardSequence.create().type("user").tab().type("pass").enter().execute()
 */
public class KeyboardSequenceBuilder {
    private static final Logger log = LoggerFactory.getLogger(KeyboardSequenceBuilder.class);
    
    private List<KeyboardAction> actions = new ArrayList<>();
    private By currentLocator = null;
    private Integer defaultTimeout = null;
    
    private enum ActionType {
        TYPE_TEXT, SEND_KEY, KEY_COMBO, WAIT, FOCUS_ELEMENT
    }
    
    private static class KeyboardAction {
        ActionType type;
        By locator;
        String text;
        Keys key;
        List<Keys> modifiers;
        long waitTimeMs;
        boolean clearFirst;
        
        KeyboardAction(ActionType type) {
            this.type = type;
        }
    }
    
    public static class KeyboardSequenceResponse {
        private final List<KeyboardActionResponse> responses;
        private final boolean allSuccess;
        private final long totalExecutionTime;
        
        public KeyboardSequenceResponse(List<KeyboardActionResponse> responses, long totalExecutionTime) {
            this.responses = responses;
            this.totalExecutionTime = totalExecutionTime;
            this.allSuccess = responses.stream().allMatch(KeyboardActionResponse::isSuccess);
        }
        
        public List<KeyboardActionResponse> getResponses() { return responses; }
        public boolean isAllSuccess() { return allSuccess; }
        public long getTotalExecutionTime() { return totalExecutionTime; }
        
        @Override
        public String toString() {
            return String.format("KeyboardSequence[success=%s, actions=%d, time=%dms]",
                    allSuccess, responses.size(), totalExecutionTime);
        }
    }
    
    public static KeyboardSequenceBuilder create() {
        return new KeyboardSequenceBuilder();
    }
    
    public KeyboardSequenceBuilder withDefaultTimeout(int seconds) {
        this.defaultTimeout = seconds;
        return this;
    }
    
    public KeyboardSequenceBuilder onElement(By locator) {
        this.currentLocator = locator;
        return this;
    }
    
    public KeyboardSequenceBuilder type(String text) {
        KeyboardAction action = new KeyboardAction(ActionType.TYPE_TEXT);
        action.locator = currentLocator;
        action.text = text;
        action.clearFirst = false;
        actions.add(action);
        return this;
    }
    
    public KeyboardSequenceBuilder typeAndClear(String text) {
        KeyboardAction action = new KeyboardAction(ActionType.TYPE_TEXT);
        action.locator = currentLocator;
        action.text = text;
        action.clearFirst = true;
        actions.add(action);
        return this;
    }
    
    public KeyboardSequenceBuilder enter() {
        return sendKey(Keys.ENTER);
    }
    
    public KeyboardSequenceBuilder tab() {
        return sendKey(Keys.TAB);
    }
    
    public KeyboardSequenceBuilder escape() {
        return sendKey(Keys.ESCAPE);
    }
    
    public KeyboardSequenceBuilder space() {
        return sendKey(Keys.SPACE);
    }
    
    public KeyboardSequenceBuilder backspace() {
        return sendKey(Keys.BACK_SPACE);
    }
    
    public KeyboardSequenceBuilder delete() {
        return sendKey(Keys.DELETE);
    }
    
    public KeyboardSequenceBuilder arrowUp() {
        return sendKey(Keys.ARROW_UP);
    }
    
    public KeyboardSequenceBuilder arrowDown() {
        return sendKey(Keys.ARROW_DOWN);
    }
    
    public KeyboardSequenceBuilder arrowLeft() {
        return sendKey(Keys.ARROW_LEFT);
    }
    
    public KeyboardSequenceBuilder arrowRight() {
        return sendKey(Keys.ARROW_RIGHT);
    }
    
    public KeyboardSequenceBuilder sendKey(Keys key) {
        KeyboardAction action = new KeyboardAction(ActionType.SEND_KEY);
        action.locator = currentLocator;
        action.key = key;
        actions.add(action);
        return this;
    }
    
    public KeyboardSequenceBuilder ctrlA() {
        return keyCombo(List.of(Keys.CONTROL), Keys.chord("a"));
    }
    
    public KeyboardSequenceBuilder ctrlC() {
        return keyCombo(List.of(Keys.CONTROL), Keys.chord("c"));
    }
    
    public KeyboardSequenceBuilder ctrlV() {
        return keyCombo(List.of(Keys.CONTROL), Keys.chord("v"));
    }
    
    public KeyboardSequenceBuilder ctrlS() {
        return keyCombo(List.of(Keys.CONTROL), Keys.chord("s"));
    }
    
    public KeyboardSequenceBuilder ctrlZ() {
        return keyCombo(List.of(Keys.CONTROL), Keys.chord("z"));
    }
    
    public KeyboardSequenceBuilder keyCombo(List<Keys> modifiers, Keys key) {
        KeyboardAction action = new KeyboardAction(ActionType.KEY_COMBO);
        action.locator = currentLocator;
        action.modifiers = modifiers;
        action.key = key;
        actions.add(action);
        return this;
    }
    
    public KeyboardSequenceBuilder wait(long milliseconds) {
        KeyboardAction action = new KeyboardAction(ActionType.WAIT);
        action.waitTimeMs = milliseconds;
        actions.add(action);
        return this;
    }
    
    public KeyboardSequenceBuilder focusElement(By locator) {
        KeyboardAction action = new KeyboardAction(ActionType.FOCUS_ELEMENT);
        action.locator = locator;
        actions.add(action);
        return this;
    }
    
    /**
     * Execute the keyboard sequence
     */
    public KeyboardSequenceResponse execute() throws KeyboardActionException {
        long startTime = System.currentTimeMillis();
        List<KeyboardActionResponse> responses = new ArrayList<>();
        
        log.info("Executing keyboard sequence with {} actions", actions.size());
        
        for (int i = 0; i < actions.size(); i++) {
            KeyboardAction action = actions.get(i);
            KeyboardActionResponse response = null;
            
            try {
                log.debug("Executing action {} of {}: {}", i + 1, actions.size(), action.type);
                
                switch (action.type) {
                    case TYPE_TEXT:
                        response = KeyboardEventsHandler.sendText(action.locator, action.text, 
                                                                action.clearFirst, defaultTimeout);
                        break;
                        
                    case SEND_KEY:
                        response = KeyboardEventsHandler.sendSpecialKey(action.locator, action.key, defaultTimeout);
                        break;
                        
                    case KEY_COMBO:
                        response = KeyboardEventsHandler.sendKeyCombo(action.locator, action.modifiers, 
                                                                    action.key, defaultTimeout);
                        break;
                        
                    case WAIT:
                        Thread.sleep(action.waitTimeMs);
                        response = new KeyboardActionResponse(true, 
                                "Wait completed: " + action.waitTimeMs + "ms", 
                                action.waitTimeMs, null, null, null);
                        break;
                        
                    case FOCUS_ELEMENT:
                        // Focus element by clicking
                        WebDriver driver = KeyboardEventsHandler.getDriver();
                        WebElement element = driver.findElement(action.locator);
                        element.click();
                        response = new KeyboardActionResponse(true, 
                                "Element focused: " + action.locator.toString(), 
                                0, null, null, action.locator);
                        break;
                }
                
                if (response != null) {
                    responses.add(response);
                    
                    if (!response.isSuccess()) {
                        log.error("Keyboard sequence action failed at step {}: {}", i + 1, response.getMessage());
                        break; // Stop sequence on failure
                    }
                }
                
            } catch (Exception e) {
                log.error("Exception during keyboard sequence at step {}: {}", i + 1, e.getMessage());
                
                // Create error response
                response = new KeyboardActionResponse(false, 
                        "Exception: " + e.getMessage(), 
                        0, action.type != null ? 
                            KeyboardEventsHandler.KeyboardActionType.valueOf(action.type.name()) : null, 
                        action.text, action.locator);
                responses.add(response);
                break; // Stop sequence on exception
            }
        }
        
        long totalTime = System.currentTimeMillis() - startTime;
        KeyboardSequenceResponse sequenceResponse = new KeyboardSequenceResponse(responses, totalTime);
        
        log.info("Keyboard sequence execution completed: {}", sequenceResponse.toString());
        
        return sequenceResponse;
    }
    
    /**
     * Execute sequence and return boolean success result
     */
    public boolean executeAndVerify() {
        try {
            KeyboardSequenceResponse response = execute();
            return response.isAllSuccess();
        } catch (KeyboardActionException e) {
            log.error("Keyboard sequence execution failed: {}", e.getMessage());
            return false;
        }
    }
}

/**
 * Page Object Base Class with Keyboard Event Integration
 */
package com.enterprise.selenium.pages.base;

import org.openqa.selenium.By;
import org.openqa.selenium.WebDriver;
import com.enterprise.selenium.keywords.KeyboardEventsHandler;
import com.enterprise.selenium.keywords.fluent.FluentKeyboardActions;
import com.enterprise.selenium.keywords.builder.KeyboardSequenceBuilder;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public abstract class KeyboardEnabledBasePage {
    protected WebDriver driver;
    protected Logger log = LoggerFactory.getLogger(this.getClass());
    
    public KeyboardEnabledBasePage(WebDriver driver) {
        this.driver = driver;
        KeyboardEventsHandler.setDriver(driver);
    }
    
    /**
     * Safe text input with keyboard events
     */
    protected boolean safeTypeText(By locator, String text, boolean clearFirst) {
        try {
            KeyboardEventsHandler.KeyboardActionResponse response = 
                KeyboardEventsHandler.sendText(locator, text, clearFirst, 10);
            return response.isSuccess();
        } catch (Exception e) {
            log.error("Safe type text failed for {}: {}", locator, e.getMessage());
            return false;
        }
    }
    
    /**
     * Submit form using Enter key
     */
    protected boolean submitWithEnter(By locator) {
        try {
            return FluentKeyboardActions.onElement(locator).pressEnter().isSuccess();
        } catch (Exception e) {
            log.error("Submit with Enter failed for {}: {}", locator, e.getMessage());
            return false;
        }
    }
    
    /**
     * Navigate between fields using Tab
     */
    protected boolean navigateToNextField(By currentField) {
        try {
            return FluentKeyboardActions.onElement(currentField).pressTab().isSuccess();
        } catch (Exception e) {
            log.error("Tab navigation failed for {}: {}", currentField, e.getMessage());
            return false;
        }
    }
    
    /**
     * Human-like form filling
     */
    protected boolean fillFormHumanLike(By locator, String text) {
        try {
            return FluentKeyboardActions.onElement(locator)
                    .clearFirst()
                    .humanLike(80, 200)
                    .typeText(text)
                    .isSuccess();
        } catch (Exception e) {
            log.error("Human-like form filling failed for {}: {}", locator, e.getMessage());
            return false;
        }
    }
    
    /**
     * Execute complex keyboard sequence
     */
    protected boolean executeKeyboardSequence(KeyboardSequenceBuilder.KeyboardSequenceResponse... sequences) {
        for (KeyboardSequenceBuilder.KeyboardSequenceResponse sequence : sequences) {
            if (!sequence.isAllSuccess()) {
                log.error("Keyboard sequence failed: {}", sequence.toString());
                return false;
            }
        }
        return true;
    }
}