package io.github.kmariusz.playwrightwebdriver;

/**
 * Enum representing different browser types supported by Playwright.
 * <p>
 * This enum provides constants for the three browser engines that Playwright supports:
 * Chromium (Chrome/Edge), Firefox, and WebKit (Safari).
 * </p>
 */
public enum BrowserTypes {
    /**
     * Represents the Chromium browser engine.
     * Used by browsers like Google Chrome and Microsoft Edge.
     */
    CHROMIUM,
    
    /**
     * Represents the Firefox browser engine.
     * Used by Mozilla Firefox browser.
     */
    FIREFOX,
    
    /**
     * Represents the WebKit browser engine.
     * Used by Apple Safari browser.
     */
    WEBKIT
}
