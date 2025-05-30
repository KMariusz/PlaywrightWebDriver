package io.github.kmariusz.playwrightwebdriver.config;

import com.microsoft.playwright.Browser;
import com.microsoft.playwright.BrowserType;
import com.microsoft.playwright.Playwright;
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
     * Options for creating a Playwright instance.
     * These options can control environment variables and other Playwright initialization parameters.
     */
    @Builder.Default
    private Playwright.CreateOptions createOptions = new Playwright.CreateOptions();

    /**
     * Options for launching a browser instance.
     * These can include settings like headless mode, browser arguments, executable path, etc.
     */
    @Builder.Default
    private BrowserType.LaunchOptions launchOptions = new BrowserType.LaunchOptions();

    /**
     * Options for creating a new browser context.
     * These control settings like viewport size, geolocation, permissions, and other browser context properties.
     */
    @Builder.Default
    private Browser.NewContextOptions contextOptions = new Browser.NewContextOptions();

    /**
     * The type of browser to use.
     * Default value is {@link BrowserTypes#CHROMIUM}.
     */
    @Builder.Default
    private BrowserTypes browserType = BrowserTypes.CHROMIUM;
}
