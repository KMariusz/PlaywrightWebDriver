package io.github.kmariusz.playwrightwebdriver;

import lombok.AccessLevel;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;
import lombok.experimental.Accessors;

/**
 * Configuration options for Playwright WebDriver.
 * This class provides a way to configure browser behavior and properties
 * such as browser type, headless mode, window dimensions, and other settings.
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor(access = AccessLevel.PRIVATE)
@Accessors(chain = true)
public class PlaywrightWebDriverOptions {
    /**
     * The type of browser to use.
     * Default value is {@link BrowserTypes#CHROMIUM}.
     */
    @Builder.Default
    private BrowserTypes browserType = BrowserTypes.CHROMIUM;
    
    /**
     * Whether to run the browser in headless mode (without UI).
     * Default value is {@code true}.
     */
    @Builder.Default
    private boolean headless = true;
    
    /**
     * The width of the browser window in pixels.
     * Default value is 1280.
     */
    @Builder.Default
    private int windowWidth = 1280;
    
    /**
     * The height of the browser window in pixels.
     * Default value is 800.
     */
    @Builder.Default
    private int windowHeight = 800;
    
    /**
     * Whether to ignore HTTPS errors during navigation.
     * Default value is {@code false}.
     */
    @Builder.Default
    private boolean ignoreHTTPSErrors = false;
    
    /**
     * Slows down Playwright operations by the specified amount of milliseconds.
     * Default value is 0 (no slowdown).
     */
    @Builder.Default
    private int slowMo = 0;
}
