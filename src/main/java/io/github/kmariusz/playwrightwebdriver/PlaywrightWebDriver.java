package io.github.kmariusz.playwrightwebdriver;

import com.microsoft.playwright.Browser;
import com.microsoft.playwright.BrowserContext;
import com.microsoft.playwright.Frame;
import com.microsoft.playwright.Page;
import com.microsoft.playwright.Playwright;
import io.github.kmariusz.playwrightwebdriver.config.PlaywrightWebDriverOptions;
import io.github.kmariusz.playwrightwebdriver.util.JavaScriptUtils;
import io.github.kmariusz.playwrightwebdriver.util.SelectorUtils;
import org.openqa.selenium.Alert;
import org.openqa.selenium.By;
import org.openqa.selenium.NoSuchFrameException;
import org.openqa.selenium.OutputType;
import org.openqa.selenium.WebDriver;
import org.openqa.selenium.WebDriverException;
import org.openqa.selenium.WebElement;
import org.openqa.selenium.WindowType;
import org.openqa.selenium.bidi.BiDi;
import org.openqa.selenium.bidi.HasBiDi;
import org.openqa.selenium.remote.RemoteWebDriver;

import java.net.URL;
import java.util.LinkedHashMap;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.Set;
import java.util.UUID;
import java.util.stream.Collectors;

/**
 * A Selenium WebDriver implementation that uses Playwright as the underlying automation engine.
 * This class extends RemoteWebDriver to provide Playwright's capabilities within the Selenium API.
 * <p>
 * PlaywrightWebDriver enables using Playwright's modern automation capabilities while maintaining
 * compatibility with the Selenium WebDriver interface. This allows existing Selenium-based test
 * suites to leverage Playwright's performance and reliability advantages with minimal code changes.
 * <p>
 * Note that some advanced Playwright features may not be directly accessible through the standard
 * WebDriver interfaces. For these cases, use the {@link #getPlaywrightPage()} and
 * {@link #getPlaywrightContext()} methods to access the native Playwright objects.
 */
public class PlaywrightWebDriver extends RemoteWebDriver implements HasBiDi {
    /**
     * The Playwright instance used for browser automation.
     */
    private final Playwright playwright;

    /**
     * The Browser instance representing the automated browser.
     */
    private final Browser browser;

    /**
     * The BrowserContext instance representing an isolated browser session.
     */
    private final BrowserContext context;

    /**
     * A map of window handles to their corresponding Page instances.
     * This allows tracking multiple windows/tabs opened during the session.
     */
    private final Map<String, Page> windowHandles = new LinkedHashMap<>();
    /**
     * The Page instance representing a single tab or window within the browser.
     */
    private Page page;

    /**
     * The current frame in focus for WebDriver operations.
     * This helps track which frame should be used for element finding and other operations.
     */
    private Frame frame;

    /**
     * Creates a new PlaywrightWebDriver instance with default options.
     */
    public PlaywrightWebDriver() {
        this(new PlaywrightWebDriverOptions());
    }

    /**
     * Creates a new PlaywrightWebDriver instance with the specified options.
     *
     * @param options the configuration options for this WebDriver instance, or null for default options
     * @throws IllegalArgumentException if an unsupported browser type is specified in the options
     * @throws RuntimeException         if there's a failure initializing Playwright components
     */
    public PlaywrightWebDriver(PlaywrightWebDriverOptions options) {
        if (options == null) {
            options = new PlaywrightWebDriverOptions();
        }

        this.playwright = Playwright.create(options.getCreateOptions());
        this.browser = createBrowser(options);
        this.context = browser.newContext(options.getContextOptions());
        this.page = context.newPage();
        this.windowHandles.put(UUID.randomUUID().toString(), page);
        setFrame(page.mainFrame());
    }

    /**
     * Creates and configures a Browser instance based on the provided options.
     *
     * @param options the configuration options for the browser
     * @return a configured Browser instance
     * @throws IllegalArgumentException if the browser type specified in options is not supported
     */
    private Browser createBrowser(PlaywrightWebDriverOptions options) {
        switch (options.getBrowserType()) {
            case CHROMIUM:
                return playwright.chromium().launch(options.getLaunchOptions());
            case FIREFOX:
                return playwright.firefox().launch(options.getLaunchOptions());
            case WEBKIT:
                return playwright.webkit().launch(options.getLaunchOptions());
            default:
                throw new IllegalArgumentException("Unsupported browser type: " + options.getBrowserType());
        }
    }

    /**
     * Sets the current frame for WebDriver operations and waits for it to load.
     *
     * @param frame the Playwright Frame to set as current
     * @return this WebDriver instance for method chaining
     */
    private WebDriver setFrame(Frame frame) {
        this.frame = frame;
        this.frame.waitForLoadState();
        return this;
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
     * The search criteria are converted to Playwright-compatible selectors.
     *
     * @param by the locating mechanism to use
     * @return a list of WebElements matching the search criteria
     */
    @Override
    public List<WebElement> findElements(By by) {
        String selector = SelectorUtils.convertToPlaywrightSelector(by);
        return frame.locator(selector)
                .all()
                .stream()
                .map(locator -> new PlaywrightWebElement(this, locator))
                .collect(Collectors.toList());
    }

    /**
     * Finds the first element within the current page that matches the given search criteria.
     * The search criteria are converted to Playwright-compatible selectors.
     *
     * @param by the locating mechanism to use
     * @return the first WebElement matching the search criteria
     * @throws org.openqa.selenium.NoSuchElementException if no matching elements are found
     */
    @Override
    public WebElement findElement(By by) {
        String selector = SelectorUtils.convertToPlaywrightSelector(by);
        return new PlaywrightWebElement(this, frame.locator(selector));
    }

    /**
     * Gets the source code of the current page.
     *
     * @return the HTML source code of the current page as a string
     */
    @Override
    public String getPageSource() {
        return frame.content();
    }

    /**
     * Closes the current browser window
     */
    @Override
    public void close() {
        // Close the current page and remove it from the window handles
        if (page != null) {
            String handle = getWindowHandle();
            page.close();
            windowHandles.remove(handle);
            // If no pages left, quit the browser
            if (windowHandles.isEmpty()) {
                quit();
            } else {
                // Switch to another page if available
                page = context.pages().get(0);
                setFrame(page.mainFrame());
            }
        }
    }

    /**
     * Quits the driver, closing all associated windows/tabs and releasing resources.
     */
    @Override
    public void quit() {
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
     * Gets the set of window handles available to the driver.
     *
     * @return a set of window handle identifiers for all open windows/tabs
     */
    @Override
    public Set<String> getWindowHandles() {
        // Interact with Playwright to refresh pages list
        String tmp = page.title();

        List<Page> pages = context.pages();
        windowHandles.entrySet().removeIf(entry -> !pages.contains(entry.getValue()));
        pages.forEach(p -> {
                    if (!windowHandles.containsValue(p)) {
                        windowHandles.put(UUID.randomUUID().toString(), p);
                    }
                }
        );
        return new LinkedHashSet<>(windowHandles.keySet());
    }

    /**
     * Gets the handle of the current window.
     *
     * @return the window handle identifier for the currently active window
     */
    @Override
    public String getWindowHandle() {
        return windowHandles.entrySet().stream()
                .filter(entry -> entry.getValue().equals(page))
                .map(Map.Entry::getKey)
                .findFirst()
                .orElseThrow(() -> new WebDriverException("Current page not found in window handles map"));
    }

    /**
     * Returns a TargetLocator instance that can be used to switch to a different frame, window,
     * or handle alerts within the browser.
     *
     * @return a TargetLocator implementation for Playwright
     */
    @Override
    public TargetLocator switchTo() {
        return new PlaywrightTargetLocator();
    }

    /**
     * Returns a Navigation instance that allows controlling browser navigation
     * (back, forward, refresh, etc.).
     *
     * @return a Navigation implementation for Playwright
     */
    @Override
    public Navigation navigate() {
        return new PlaywrightNavigation();
    }

    /**
     * Returns an Options instance that provides access to browser-specific capabilities
     * such as cookies, timeouts, and window settings.
     *
     * @return an Options implementation for Playwright
     */
    @Override
    public Options manage() {
        //return new PlaywrightOptions();
        throw new UnsupportedOperationException("Options management is not yet implemented in PlaywrightWebDriver.");
    }

    /**
     * Executes JavaScript code in the context of the current page.
     *
     * @param script the JavaScript code to execute
     * @param args   the arguments to pass to the script
     * @return the value returned by the script, or null if the script returns undefined
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
     * @param args   the arguments to pass to the script
     * @return the value returned by the script when the Promise resolves
     */
    @Override
    public Object executeAsyncScript(String script, Object... args) {
        return JavaScriptUtils.executeAsyncScript(page, script, args);
    }

    /**
     * Takes a screenshot of the current page.
     *
     * @param target the output type for the screenshot
     * @param <X>    the return type of the screenshot output
     * @return the screenshot as the specified output type
     * @throws WebDriverException if the screenshot could not be taken or processed
     */
    @Override
    public <X> X getScreenshotAs(OutputType<X> target) throws WebDriverException {
        byte[] screenshot = page.screenshot(new Page.ScreenshotOptions()
                .setFullPage(true));
        return target.convertFromPngBytes(screenshot);
    }

    /**
     * Gets the Playwright Page instance used by this driver.
     * This provides direct access to Playwright-specific functionality not available through the WebDriver interface.
     *
     * @return the Playwright Page instance
     */
    public Page getPlaywrightPage() {
        return page;
    }

    /**
     * Gets the Playwright BrowserContext instance used by this driver.
     * This provides direct access to context-level Playwright functionality such as cookies, permissions, and network control.
     *
     * @return the Playwright BrowserContext instance
     */
    public BrowserContext getPlaywrightContext() {
        return context;
    }

    /**
     * Returns an empty {@link Optional} for BiDi, as PlaywrightWebDriver does not yet support the WebDriver BiDi protocol.
     *
     * @return an empty {@link Optional} for BiDi
     */
    @Override
    public Optional<BiDi> maybeGetBiDi() {
        return Optional.empty();
    }

    /**
     * Implementation of WebDriver.TargetLocator for Playwright.
     * This class handles switching between frames, windows, and alert handling.
     */
    private class PlaywrightTargetLocator implements TargetLocator {
        /**
         * Switches the focus to a frame by its index in the page's frames list.
         *
         * @param index the zero-based index of the frame to switch to
         * @return the WebDriver focused on the specified frame
         * @throws org.openqa.selenium.NoSuchFrameException if the frame cannot be found at the specified index
         */
        @Override
        public WebDriver frame(int index) {
            List<Frame> frames = page.frames();
            if (index < 0 || index >= frames.size()) {
                throw new NoSuchFrameException("No frame found with index: " + index);
            }
            return setFrame(frames.get(index));
        }

        /**
         * Switches the focus to a frame by its name or ID attribute.
         *
         * @param nameOrId the name or ID attribute of the frame to switch to
         * @return the WebDriver focused on the specified frame
         */
        @Override
        public WebDriver frame(String nameOrId) {
            List<Frame> frames = page.frames();
            Optional<Frame> targetFrame = frames.stream()
                    .filter(frame -> nameOrId.equals(frame.name()) || nameOrId.equals(frame.frameElement().getAttribute("id")))
                    .findFirst();
            if (targetFrame.isEmpty()) {
                throw new NoSuchFrameException("No frame found with name or ID: " + nameOrId);
            }
            return setFrame(targetFrame.get());
        }

        /**
         * Switches the focus to a frame using a WebElement that references the frame.
         *
         * @param frameElement the WebElement representing the frame to switch to
         * @return the WebDriver focused on the specified frame
         * @throws WebDriverException if the frame cannot be found or switched to,
         *                            or if the element is not a PlaywrightWebElement
         */
        @Override
        public WebDriver frame(WebElement frameElement) {
            if (PlaywrightWebElement.instanceOf(frameElement)) {
                PlaywrightWebElement element = (PlaywrightWebElement) frameElement;
                return setFrame(element.locator().elementHandle().contentFrame());
            }

            throw new WebDriverException("The provided WebElement is not a PlaywrightWebElement or does not reference a frame.");
        }

        /**
         * Switches the focus back to the parent frame.
         *
         * @return the WebDriver focused on the parent frame
         */
        @Override
        public WebDriver parentFrame() {
            return setFrame(frame.parentFrame());
        }

        /**
         * Switches the focus to a window identified by nameOrHandle.
         *
         * @param nameOrHandle the name of the window or the handle as returned by getWindowHandle
         * @return the WebDriver focused on the specified window
         */
        @Override
        public WebDriver window(String nameOrHandle) {
            if (windowHandles.containsKey(nameOrHandle)) {
                page = windowHandles.get(nameOrHandle);
                page.bringToFront();
                frame = page.mainFrame();
                return PlaywrightWebDriver.this;
            }
            throw new WebDriverException("No window found with handle: " + nameOrHandle);
        }

        /**
         * Creates a new window or tab and switches to it.
         *
         * @param typeHint indicates whether to open a new window or a new tab
         * @return the WebDriver focused on the new window or tab
         */
        @Override
        public WebDriver newWindow(WindowType typeHint) {
            String newWindowHandle = UUID.randomUUID().toString();
            windowHandles.put(newWindowHandle, context.newPage());
            return window(newWindowHandle);
        }

        /**
         * Switches focus to the default content (top-level frame).
         *
         * @return the WebDriver focused on the default content
         */
        @Override
        public WebDriver defaultContent() {
            return setFrame(page.mainFrame());
        }

        /**
         * Returns an Alert instance that allows interacting with Javascript dialogs.
         *
         * @return an Alert implementation for Playwright
         */
        @Override
        public Alert alert() {
            //return new PlaywrightAlert();
            throw new UnsupportedOperationException("Alert handling is not yet implemented in PlaywrightWebDriver.");
        }

        /**
         * Gets the currently focused element on the page.
         *
         * @return the currently focused WebElement
         */
        @Override
        public WebElement activeElement() {
            return new PlaywrightWebElement(PlaywrightWebDriver.this,
                    page.locator("*:focus"));
        }
    }

    /**
     * Implementation of WebDriver.Navigation for Playwright.
     * This class handles browser navigation functionality.
     */
    private class PlaywrightNavigation implements Navigation {
        @Override
        public void back() {
            page.goBack();
        }

        @Override
        public void forward() {
            page.goForward();
        }

        @Override
        public void to(String url) {
            page.navigate(url);
        }

        @Override
        public void to(URL url) {
            page.navigate(url.toString());
        }

        @Override
        public void refresh() {
            page.reload();
        }
    }
}