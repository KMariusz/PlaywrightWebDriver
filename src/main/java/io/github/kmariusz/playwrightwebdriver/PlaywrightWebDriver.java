package io.github.kmariusz.playwrightwebdriver;

import com.microsoft.playwright.Browser;
import com.microsoft.playwright.BrowserContext;
import com.microsoft.playwright.Page;
import com.microsoft.playwright.Playwright;
import io.github.kmariusz.playwrightwebdriver.config.PlaywrightWebDriverOptions;
import io.github.kmariusz.playwrightwebdriver.util.JavaScriptUtils;
import io.github.kmariusz.playwrightwebdriver.util.SelectorUtils;
import org.openqa.selenium.Alert;
import org.openqa.selenium.By;
import org.openqa.selenium.Cookie;
import org.openqa.selenium.Dimension;
import org.openqa.selenium.OutputType;
import org.openqa.selenium.Point;
import org.openqa.selenium.WebDriver;
import org.openqa.selenium.WebDriverException;
import org.openqa.selenium.WebElement;
import org.openqa.selenium.WindowType;
import org.openqa.selenium.bidi.BiDi;
import org.openqa.selenium.bidi.HasBiDi;
import org.openqa.selenium.logging.Logs;
import org.openqa.selenium.remote.RemoteWebDriver;

import java.net.URL;
import java.time.Duration;
import java.util.Date;
import java.util.HashMap;
import java.util.HashSet;
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
     * The Page instance representing a single tab or window within the browser.
     */
    private final Page page;

    /**
     * Map of window handles (UUIDs) to their URLs, for multi-window session management.
     */
    private final Map<String, String> windowHandles = new HashMap<>();

    /**
     * The identifier for the currently active window handle.
     */
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
        return page.locator(selector)
                .all()
                .stream()
                .map(locator -> new PlaywrightWebElement(this, locator, selector))
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
        return new PlaywrightWebElement(this, page.locator(selector), selector);
    }

    /**
     * Gets the source code of the current page.
     *
     * @return the HTML source code of the current page as a string
     */
    @Override
    public String getPageSource() {
        return page.content();
    }

    /**
     * Closes the current browser session and releases all associated resources.
     * This method closes the page, context, browser, and playwright instances in sequence.
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
     * This is equivalent to calling {@link #close()} in this implementation.
     */
    @Override
    public void quit() {
        close();
    }

    /**
     * Gets the set of window handles available to the driver.
     *
     * @return a set of window handle identifiers for all open windows/tabs
     */
    @Override
    public Set<String> getWindowHandles() {
        return new HashSet<>(windowHandles.keySet());
    }

    /**
     * Gets the handle of the current window.
     *
     * @return the window handle identifier for the currently active window
     */
    @Override
    public String getWindowHandle() {
        return currentWindowHandle;
    }

    @Override
    public TargetLocator switchTo() {
        return new PlaywrightTargetLocator();
    }

    @Override
    public Navigation navigate() {
        return new PlaywrightNavigation();
    }

    @Override
    public Options manage() {
        return new PlaywrightOptions();
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
         * @param index the index of the frame to switch to
         * @return the WebDriver focused on the specified frame
         */
        @Override
        public WebDriver frame(int index) {
            // Switch to frame by index
            // Using page.frame(index) which returns the frame or null
            com.microsoft.playwright.Frame frame = page.frames().size() > index ? page.frames().get(index) : null;
            if (frame != null) {
                page.setContent(frame.content());
            }
            return PlaywrightWebDriver.this;
        }

        /**
         * Switches the focus to a frame by its name or ID attribute.
         *
         * @param nameOrId the name or ID attribute of the frame to switch to
         * @return the WebDriver focused on the specified frame
         */
        @Override
        public WebDriver frame(String nameOrId) {
            // Switch to frame by name or ID attribute
            page.frame(nameOrId);
            return PlaywrightWebDriver.this;
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
            if (frameElement instanceof PlaywrightWebElement) {
                PlaywrightWebElement element = (PlaywrightWebElement) frameElement;
                // Get the locator for the frame element
                String frameSelector = element.locator().toString();

                try {
                    // First, try to get the frame element
                    com.microsoft.playwright.ElementHandle frameHandle = page.querySelector(frameSelector);
                    if (frameHandle != null) {
                        // Navigate to the content frame associated with this element
                        com.microsoft.playwright.Frame contentFrame = frameHandle.contentFrame();
                        if (contentFrame != null) {
                            // Switched successfully to frame
                            return PlaywrightWebDriver.this;
                        }
                    }

                    // As fallback, try using frameLocator if direct approach fails
                    page.frameLocator(frameSelector).first();
                } catch (Exception e) {
                    throw new WebDriverException("Failed to switch to frame: " + e.getMessage(), e);
                }
            } else {
                throw new WebDriverException("Frame element must be a PlaywrightWebElement");
            }
            return PlaywrightWebDriver.this;
        }

        /**
         * Switches the focus back to the parent frame.
         *
         * @return the WebDriver focused on the parent frame
         */
        @Override
        public WebDriver parentFrame() {
            // Go back to the parent frame
            com.microsoft.playwright.Frame parentFrame = page.mainFrame().parentFrame();
            if (parentFrame != null) {
                page.setContent(parentFrame.content());
            }
            return PlaywrightWebDriver.this;
        }

        @Override
        public WebDriver window(String nameOrHandle) {
            // Switch to window with the given handle
            if (windowHandles.containsKey(nameOrHandle)) {
                currentWindowHandle = nameOrHandle;
                // In Playwright, we would need to switch to the relevant page
                // This is a simplified implementation
            }
            return PlaywrightWebDriver.this;
        }

        @Override
        public WebDriver newWindow(WindowType typeHint) {
            // Create a new window/tab
            Page newPage = context.newPage();
            String newWindowHandle = UUID.randomUUID().toString();
            windowHandles.put(newWindowHandle, newPage.url());
            currentWindowHandle = newWindowHandle;
            return PlaywrightWebDriver.this;
        }

        @Override
        public WebDriver defaultContent() {
            // Switch back to the top-level frame
            page.mainFrame();
            return PlaywrightWebDriver.this;
        }

        @Override
        public Alert alert() {
            // Handle JavaScript alerts
            return new PlaywrightAlert();
        }

        @Override
        public WebElement activeElement() {
            // Get the currently focused element
            String script = "return document.activeElement";
            return new PlaywrightWebElement(PlaywrightWebDriver.this,
                    page.locator("*:focus"),
                    "*:focus");
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

    /**
     * Implementation of WebDriver.Options for Playwright.
     * This class handles browser-level options like cookies, timeouts, and window management.
     */
    private class PlaywrightOptions implements Options {
        @Override
        public void addCookie(Cookie cookie) {
            com.microsoft.playwright.options.Cookie playwrightCookie =
                    new com.microsoft.playwright.options.Cookie(cookie.getName(), cookie.getValue())
                            .setDomain(cookie.getDomain())
                            .setPath(cookie.getPath())
                            .setSecure(cookie.isSecure())
                            .setHttpOnly(cookie.isHttpOnly())
                            .setSameSite(convertSameSitePolicy(cookie.getSameSite()))
                            .setExpires(cookie.getExpiry() != null ? cookie.getExpiry().getTime() / 1000.0 : null);

            context.addCookies(List.of(playwrightCookie));
        }

        @Override
        public void deleteCookieNamed(String name) {
            // Delete cookie with the specified name
            List<com.microsoft.playwright.options.Cookie> cookies = context.cookies();
            cookies.stream()
                    .filter(c -> c.name.equals(name))
                    .forEach(c -> context.clearCookies());
        }

        @Override
        public void deleteCookie(Cookie cookie) {
            deleteCookieNamed(cookie.getName());
        }

        @Override
        public void deleteAllCookies() {
            context.clearCookies();
        }

        @Override
        public Set<Cookie> getCookies() {
            List<com.microsoft.playwright.options.Cookie> playwrightCookies = context.cookies();
            Set<Cookie> seleniumCookies = new HashSet<>();

            for (com.microsoft.playwright.options.Cookie pwCookie : playwrightCookies) {
                Cookie.Builder builder = new Cookie.Builder(pwCookie.name, pwCookie.value)
                        .path(pwCookie.path)
                        .domain(pwCookie.domain)
                        .isSecure(pwCookie.secure)
                        .isHttpOnly(pwCookie.httpOnly);

                if (pwCookie.expires != null) {
                    builder.expiresOn(new Date((long) (pwCookie.expires * 1000)));
                }

                seleniumCookies.add(builder.build());
            }

            return seleniumCookies;
        }

        @Override
        public Cookie getCookieNamed(String name) {
            return getCookies().stream()
                    .filter(cookie -> name.equals(cookie.getName()))
                    .findFirst()
                    .orElse(null);
        }

        @Override
        public Timeouts timeouts() {
            return new PlaywrightTimeouts();
        }

        @Override
        public Window window() {
            return new PlaywrightWindow();
        }

        @Override
        public Logs logs() {
            // Not fully implemented in this adapter
            throw new UnsupportedOperationException("Logs functionality is not implemented");
        }

        private com.microsoft.playwright.options.SameSiteAttribute convertSameSitePolicy(String sameSite) {
            if (sameSite == null) {
                return null;
            }

            switch (sameSite.toLowerCase()) {
                case "lax":
                    return com.microsoft.playwright.options.SameSiteAttribute.LAX;
                case "strict":
                    return com.microsoft.playwright.options.SameSiteAttribute.STRICT;
                case "none":
                    return com.microsoft.playwright.options.SameSiteAttribute.NONE;
                default:
                    return null;
            }
        }
    }

    /**
     * Implementation of WebDriver.Timeouts for Playwright.
     */
    private class PlaywrightTimeouts implements Timeouts {
        @Override
        public Timeouts implicitlyWait(Duration duration) {
            // Playwright doesn't have a direct equivalent to Selenium's implicit wait
            // We'll implement a basic version using page timeout
            page.setDefaultTimeout(duration.toMillis());
            return this;
        }

        @Override
        public Timeouts pageLoadTimeout(Duration duration) {
            // Set the timeout for page navigation
            page.setDefaultNavigationTimeout(duration.toMillis());
            return this;
        }

        @Override
        public Timeouts scriptTimeout(Duration duration) {
            // This has no direct equivalent in Playwright
            // We'll use it to set timeout for JavaScript execution
            return this;
        }
    }

    /**
     * Implementation of WebDriver.Window for Playwright.
     */
    private class PlaywrightWindow implements Window {
        @Override
        public void setSize(Dimension targetSize) {
            page.setViewportSize(targetSize.getWidth(), targetSize.getHeight());
        }

        @Override
        public void setPosition(Point targetPosition) {
            // Not directly supported in Playwright
            // This is a no-op in this implementation
        }

        @Override
        public Dimension getSize() {
            com.microsoft.playwright.options.ViewportSize size = page.viewportSize();
            return new Dimension(size.width, size.height);
        }

        @Override
        public Point getPosition() {
            // Not directly supported in Playwright
            return new Point(0, 0);
        }

        @Override
        public void maximize() {
            // Set a large viewport size as an approximation
            page.setViewportSize(1920, 1080);
        }

        @Override
        public void minimize() {
            // Not directly supported in Playwright
            // This is a no-op in this implementation
        }

        @Override
        public void fullscreen() {
            page.evaluate("() => { document.documentElement.requestFullscreen(); }");
        }
    }

    /**
     * Implementation of WebDriver.Alert for Playwright.
     */
    private class PlaywrightAlert implements Alert {
        @Override
        public void dismiss() {
            page.onDialog(dialog -> dialog.dismiss());
            // Trigger any pending dialogs
            page.evaluate("() => {}");
        }

        @Override
        public void accept() {
            page.onDialog(dialog -> dialog.accept());
            // Trigger any pending dialogs
            page.evaluate("() => {}");
        }

        @Override
        public String getText() {
            final String[] message = {""};
            page.onDialog(dialog -> message[0] = dialog.message());
            // Trigger any pending dialogs
            page.evaluate("() => {}");
            return message[0];
        }

        @Override
        public void sendKeys(String keysToSend) {
            page.onDialog(dialog -> dialog.accept(keysToSend));
            // Trigger any pending dialogs
            page.evaluate("() => {}");
        }
    }
}