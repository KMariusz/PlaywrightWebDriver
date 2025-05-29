package io.github.kmariusz.playwrightwebdriver;

import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.UUID;
import java.util.stream.Collectors;

import org.openqa.selenium.By;
import org.openqa.selenium.OutputType;
import org.openqa.selenium.WebDriverException;
import org.openqa.selenium.WebElement;
import org.openqa.selenium.remote.RemoteWebDriver;

import com.microsoft.playwright.Browser;
import com.microsoft.playwright.BrowserContext;
import com.microsoft.playwright.Page;
import com.microsoft.playwright.Playwright;

/**
 * A Selenium WebDriver implementation that uses Playwright as the underlying automation engine.
 * This class extends RemoteWebDriver to provide Playwright's capabilities within the Selenium API.
 */
public class PlaywrightWebDriver extends RemoteWebDriver {
    /** The Playwright instance used for browser automation. */
    private final Playwright playwright;
    
    /** The Browser instance representing the automated browser. */
    private final Browser browser;
    
    /** The BrowserContext instance representing an isolated browser session. */
    private final BrowserContext context;
    
    /** The Page instance representing a single tab or window within the browser. */
    private final Page page;
    
    /** Map of window handles to their URLs, used for window handle management. */
    private final Map<String, String> windowHandles = new HashMap<>();
    
    /** The current window handle identifier. */
    private String currentWindowHandle;

    /**
     * Creates a new PlaywrightWebDriver instance with default options.
     */
    public PlaywrightWebDriver() {
        this(new PlaywrightWebDriverOptions());
    }

    /**
     * Creates a new PlaywrightWebDriver instance with the specified options.
     *
     * @param options the configuration options for this WebDriver instance
     */
    public PlaywrightWebDriver(PlaywrightWebDriverOptions options) {
        if (options == null) {
            options = new PlaywrightWebDriverOptions();
        }

        this.playwright = Playwright.create();
        this.browser = createBrowser(options);
        this.context = browser.newContext(new Browser.NewContextOptions()
                .setViewportSize(options.getWindowWidth(), options.getWindowHeight())
                .setIgnoreHTTPSErrors(options.isIgnoreHTTPSErrors()));
        this.page = context.newPage();
        this.currentWindowHandle = UUID.randomUUID().toString();

        // Initialize window handles map with the initial page
        if (page != null && page.context() != null && page.context().browser() != null) {
            List<BrowserContext> contexts = page.context().browser().contexts();
            if (contexts != null && !contexts.isEmpty()) {
                List<Page> pages = contexts.get(0).pages();
                if (pages != null && !pages.isEmpty()) {
                    this.windowHandles.put(currentWindowHandle, pages.get(0).url());
                    return;
                }
            }
        }
        this.windowHandles.put(currentWindowHandle, "");
    }

    /**
     * Creates and configures a Browser instance based on the provided options.
     *
     * @param options the configuration options for the browser
     * @return a configured Browser instance
     * @throws IllegalArgumentException if the browser type is not supported
     */
    private Browser createBrowser(PlaywrightWebDriverOptions options) {
        com.microsoft.playwright.BrowserType.LaunchOptions launchOptions = new com.microsoft.playwright.BrowserType.LaunchOptions()
                .setHeadless(options.isHeadless())
                .setSlowMo(options.getSlowMo());

        switch (options.getBrowserType()) {
            case CHROMIUM:
                return playwright.chromium().launch(launchOptions);
            case FIREFOX:
                return playwright.firefox().launch(launchOptions);
            case WEBKIT:
                return playwright.webkit().launch(launchOptions);
            default:
                throw new IllegalArgumentException("Unsupported browser type: " + options.getBrowserType());
        }
    }

    /**
     * Navigates to the specified URL.
     *
     * @param url the URL to navigate to
     */
    @Override
    public void get(String url) {
        page.navigate(url);
    }

    /**
     * Gets the URL of the current page.
     *
     * @return the URL of the current page
     */
    @Override
    public String getCurrentUrl() {
        return page.url();
    }

    /**
     * Gets the title of the current page.
     *
     * @return the title of the current page
     */
    @Override
    public String getTitle() {
        return page.title();
    }

    /**
     * Finds all elements within the current page that match the given search criteria.
     *
     * @param by the locating mechanism to use
     * @return a list of WebElements matching the search criteria
     */
    @Override
    public List<WebElement> findElements(By by) {
        String selector = SelectorUtils.convertToPlaywrightSelector(by);
        return page.locator(selector)
                .all()
                .stream()
                .map(locator -> new PlaywrightWebElement(this, locator))
                .collect(Collectors.toList());
    }

    /**
     * Finds the first element within the current page that matches the given search criteria.
     *
     * @param by the locating mechanism to use
     * @return the first WebElement matching the search criteria
     * @throws org.openqa.selenium.NoSuchElementException if no matching elements are found
     */
    @Override
    public WebElement findElement(By by) {
        String selector = SelectorUtils.convertToPlaywrightSelector(by);
        return new PlaywrightWebElement(this, page.locator(selector));
    }

    /**
     * Gets the source code of the current page.
     *
     * @return the source code of the current page as a string
     */
    @Override
    public String getPageSource() {
        return page.content();
    }

    /**
     * Closes the current browser session and releases all associated resources.
     */
    @Override
    public void close() {
        if (page != null) {
            page.close();
        }
        if (context != null) {
            context.close();
        }
        if (browser != null) {
            browser.close();
        }
        if (playwright != null) {
            playwright.close();
        }
    }

    /**
     * Quits the driver, closing all associated windows/tabs and releasing resources.
     */
    @Override
    public void quit() {
        close();
    }

    /**
     * Gets the set of window handles available to the driver.
     *
     * @return a set of window handle identifiers
     */
    @Override
    public Set<String> getWindowHandles() {
        return new HashSet<>(windowHandles.keySet());
    }

    /**
     * Gets the handle of the current window.
     *
     * @return the window handle identifier for the current window
     */
    @Override
    public String getWindowHandle() {
        return currentWindowHandle;
    }

    /**
     * Executes JavaScript code in the context of the current page.
     *
     * @param script the JavaScript code to execute
     * @param args the arguments to pass to the script
     * @return the value returned by the script
     */
    @Override
    public Object executeScript(String script, Object... args) {
        return JavaScriptUtils.executeScript(page, script, args);
    }

    /**
     * Executes asynchronous JavaScript code in the context of the current page.
     * The script is wrapped in a Promise-based function to handle async execution.
     *
     * @param script the JavaScript code to execute asynchronously
     * @param args the arguments to pass to the script
     * @return the value returned by the script
     */
    @Override
    public Object executeAsyncScript(String script, Object... args) {
        return JavaScriptUtils.executeAsyncScript(page, script, args);
    }

    /**
     * Takes a screenshot of the current page.
     *
     * @param target the output type for the screenshot
     * @param <X> the return type of the screenshot output
     * @return the screenshot as the specified output type
     * @throws WebDriverException if the screenshot could not be taken
     */
    @Override
    public <X> X getScreenshotAs(OutputType<X> target) throws WebDriverException {
        byte[] screenshot = page.screenshot(new Page.ScreenshotOptions()
                .setFullPage(true));
        return target.convertFromPngBytes(screenshot);
    }

    /**
     * Gets the Playwright Page instance used by this driver.
     *
     * @return the Playwright Page instance
     */
    public Page getPlaywrightPage() {
        return page;
    }

    /**
     * Gets the Playwright BrowserContext instance used by this driver.
     *
     * @return the Playwright BrowserContext instance
     */
    public BrowserContext getPlaywrightContext() {
        return context;
    }
}