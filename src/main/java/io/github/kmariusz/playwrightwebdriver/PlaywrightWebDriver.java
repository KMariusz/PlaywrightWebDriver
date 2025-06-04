package io.github.kmariusz.playwrightwebdriver;

import com.google.common.net.InternetDomainName;
import com.microsoft.playwright.Browser;
import com.microsoft.playwright.BrowserContext;
import com.microsoft.playwright.ConsoleMessage;
import com.microsoft.playwright.Dialog;
import com.microsoft.playwright.Frame;
import com.microsoft.playwright.Page;
import com.microsoft.playwright.Playwright;
import io.github.kmariusz.playwrightwebdriver.config.PlaywrightWebDriverOptions;
import io.github.kmariusz.playwrightwebdriver.util.JavaScriptUtils;
import io.github.kmariusz.playwrightwebdriver.util.SelectorUtils;
import lombok.Getter;
import org.openqa.selenium.Alert;
import org.openqa.selenium.By;
import org.openqa.selenium.Cookie;
import org.openqa.selenium.Dimension;
import org.openqa.selenium.NoSuchFrameException;
import org.openqa.selenium.OutputType;
import org.openqa.selenium.Point;
import org.openqa.selenium.WebDriver;
import org.openqa.selenium.WebDriverException;
import org.openqa.selenium.WebElement;
import org.openqa.selenium.WindowType;
import org.openqa.selenium.bidi.BiDi;
import org.openqa.selenium.bidi.HasBiDi;
import org.openqa.selenium.logging.LogEntries;
import org.openqa.selenium.logging.LogEntry;
import org.openqa.selenium.logging.Logs;
import org.openqa.selenium.remote.RemoteWebDriver;

import java.net.URI;
import java.net.URISyntaxException;
import java.net.URL;
import java.time.Duration;
import java.util.ArrayList;
import java.util.Date;
import java.util.HashSet;
import java.util.LinkedHashMap;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.Set;
import java.util.UUID;
import java.util.logging.Level;
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
 * WebDriver interfaces. For these cases, use getter methods to access the native Playwright objects like Playwright,
 * Browser, and Page directly.
 */
public class PlaywrightWebDriver extends RemoteWebDriver implements HasBiDi {
    /**
     * The Playwright instance used for browser automation.
     */
    @Getter
    private final Playwright playwright;

    /**
     * The Browser instance representing the automated browser.
     */
    @Getter
    private final Browser browser;

    /**
     * The BrowserContext instance representing an isolated browser session.
     */
    @Getter
    private final BrowserContext context;

    /**
     * A map of window handles to their corresponding Page instances.
     * This allows tracking of multiple windows/tabs opened during the session.
     */
    private final Map<String, Page> windowHandles = new LinkedHashMap<>();

    /**
     * A map to store console messages from all pages.
     * This is used to capture and retrieve console logs through the Selenium logging API.
     */
    private final Map<String, List<LogEntry>> consoleMessages = new LinkedHashMap<>();

    /**
     * The Page instance representing a single tab or window within the browser.
     */
    @Getter
    private Page page;

    /**
     * The current frame in focus for WebDriver operations.
     * This helps track which frame should be used for element finding and other operations.
     */
    @Getter
    private Frame frame;

    /**
     * The timeout duration for script execution.
     * This is used to limit how long scripts can run before being forcibly terminated.
     */
    private Duration scriptTimeout = Duration.ofSeconds(30);

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
     */
    public PlaywrightWebDriver(PlaywrightWebDriverOptions options) {
        if (options == null) {
            options = new PlaywrightWebDriverOptions();
        }

        this.playwright = Playwright.create(options.getCreateOptions());
        this.browser = createBrowser(options);
        this.context = browser.newContext(options.getContextOptions());
        setPage(context.newPage());
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
     * Sets the current page for WebDriver operations and brings it to the front.
     *
     * @param page the Playwright Page to set as current
     * @return this WebDriver instance for method chaining
     */
    private WebDriver setPage(Page page) {
        this.page = page;
        page.bringToFront();
        setPageOnConsoleMessage();
        return setFrame(page.mainFrame());
    }

    /**
     * Configures console message listeners for all open pages and cleans up listeners for closed pages.
     * This method ensures that console messages from all pages are captured and stored for retrieval
     * through the Selenium logging API.
     */
    private void setPageOnConsoleMessage() {
        updateWindowHandles();
        this.consoleMessages.entrySet().removeIf(entry -> !windowHandles.containsKey(entry.getKey()));

        for (Map.Entry<String, Page> entry : this.windowHandles.entrySet()) {
            String handle = entry.getKey();
            if (!this.consoleMessages.containsKey(handle)) {
                Page page = entry.getValue();
                page.onConsoleMessage(consoleMessage -> {
                    List<LogEntry> consoleMessages = this.consoleMessages.getOrDefault(handle, new ArrayList<>());
                    consoleMessages.add(mapConsoleMessage(consoleMessage));
                    this.consoleMessages.put(handle, consoleMessages);
                });
            }
        }
    }

    /**
     * Maps a Playwright ConsoleMessage to a Selenium LogEntry.
     * This method converts Playwright-specific console messages to Selenium's logging format,
     * handling the conversion of message type to appropriate log levels.
     *
     * @param consoleMessage the Playwright console message to map
     * @return a Selenium LogEntry containing the mapped message information
     */
    private LogEntry mapConsoleMessage(ConsoleMessage consoleMessage) {
        Level level = Level.INFO;
        try {
            level = Level.parse(consoleMessage.type().toUpperCase());
        } catch (IllegalArgumentException ignored) {
        }
        return new LogEntry(level, System.currentTimeMillis(), consoleMessage.text());
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
     * Finds all elements within the current frame that match the given selector.
     *
     * @param by The Selenium By selector
     * @return a list of WebElements matching the given selector
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
     * Finds the first element within the current frame that matches the given selector.
     *
     * @param by The Selenium By selector
     * @return the first WebElement matching the given selector
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
        if (page != null) {
            String handle = getWindowHandle();
            page.close();
            windowHandles.remove(handle);
            if (windowHandles.isEmpty()) {
                quit();
            } else {
                setPage(context.pages().get(0));
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
     * Updates the internal map of window handles to Playwright Page objects.
     * This method ensures that the driver keeps track of all open pages/windows,
     * removing closed ones and adding newly opened ones to the window handles map.
     * Each page is assigned a unique UUID as its window handle.
     */
    private void updateWindowHandles() {
        // Interact with Playwright to refresh pages list
        page.title();
        List<Page> pages = context.pages();
        windowHandles.entrySet().removeIf(entry -> !pages.contains(entry.getValue()));
        pages.forEach(p -> {
                    if (!windowHandles.containsValue(p)) {
                        windowHandles.put(UUID.randomUUID().toString(), p);
                    }
                }
        );
    }

    /**
     * Gets the set of window handles available to the driver.
     *
     * @return a set of window handle identifiers for all open windows/tabs
     */
    @Override
    public Set<String> getWindowHandles() {
        updateWindowHandles();
        return new LinkedHashSet<>(windowHandles.keySet());
    }

    /**
     * Gets the handle of the current window.
     *
     * @return the window handle identifier for the currently active window
     */
    @Override
    public String getWindowHandle() {
        updateWindowHandles();
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
        return JavaScriptUtils.executeScript(page, script, scriptTimeout, args);
    }

    /**
     * Executes JavaScript code in the context of the current page.
     *
     * @param script the JavaScript code to execute
     * @param args   the arguments to pass to the script
     * @return the value returned by the script, or null if the script returns undefined
     */
    @Override
    public Object executeAsyncScript(String script, Object... args) {
        return JavaScriptUtils.executeAsyncScript(page, script, scriptTimeout, args);
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
     * Returns an empty {@link Optional} for BiDi, as Playwright does not yet support the WebDriver BiDi protocol.
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
                    .filter(frame -> nameOrId.equals(frame.name()))
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
                PlaywrightWebElement element = PlaywrightWebElement.from(frameElement);
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
                return setPage(windowHandles.get(nameOrHandle));
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
            setPage(context.newPage());
            return window(getWindowHandle());
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
            return new PlaywrightAlert();
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
        /**
         * Navigates back in the browser history.
         * This method simulates clicking the browser's back button.
         * Waits for the page to finish loading after navigation.
         */
        @Override
        public void back() {
            page.goBack();
            page.waitForLoadState();
        }

        /**
         * Navigates forward in the browser history.
         * This method simulates clicking the browser's forward button.
         * Waits for the page to finish loading after navigation.
         */
        @Override
        public void forward() {
            page.goForward();
            page.waitForLoadState();
        }

        /**
         * Navigates to the specified URL as a string.
         * Waits for the page to finish loading and for the URL to match the target.
         *
         * @param url the URL to navigate to
         */
        @Override
        public void to(String url) {
            page.navigate(url);
            page.waitForLoadState();
            page.waitForURL(url);
        }

        /**
         * Navigates to the specified URL as a {@link URL} object.
         * Waits for the page to finish loading and for the URL to match the target.
         *
         * @param url the URL to navigate to
         */
        @Override
        public void to(URL url) {
            page.navigate(url.toString());
            page.waitForLoadState();
            page.waitForURL(url.toString());
        }

        /**
         * Reloads the current page.
         * Waits for the page to finish loading after reload.
         */
        @Override
        public void refresh() {
            page.reload();
            page.waitForLoadState();
        }
    }

    /**
     * Implementation of WebDriver.Options for Playwright.
     * This class handles browser-level options like cookies, timeouts, and window management.
     */
    private class PlaywrightOptions implements Options {
        /**
         * Adds a cookie to the current browser context.
         *
         * @param cookie the Selenium {@link Cookie} to add
         */
        @Override
        public void addCookie(Cookie cookie) {
            com.microsoft.playwright.options.Cookie playwrightCookie =
                    new com.microsoft.playwright.options.Cookie(cookie.getName(), cookie.getValue())
                            .setDomain(cookie.getDomain())
                            .setPath(cookie.getPath())
                            .setSecure(cookie.isSecure())
                            .setHttpOnly(cookie.isHttpOnly())
                            .setSameSite(convertSameSitePolicy(cookie.getSameSite()));
            if (cookie.getExpiry() != null) {
                playwrightCookie
                        .setExpires(cookie.getExpiry().getTime() / 1000.0);
            }
            if (playwrightCookie.domain == null) {
                String urlString = PlaywrightWebDriver.this.getCurrentUrl();
                URI uri;
                try {
                    uri = new URI(urlString);
                } catch (URISyntaxException e) {
                    throw new RuntimeException(e);
                }
                String host = uri.getHost();
                InternetDomainName internetDomainName = InternetDomainName.from(host).topPrivateDomain();
                String domainName = internetDomainName.toString();
                playwrightCookie.setDomain(domainName);
            }
            context.addCookies(List.of(playwrightCookie));
        }

        /**
         * Deletes a cookie by its name.
         *
         * @param name the name of the cookie to delete
         */
        @Override
        public void deleteCookieNamed(String name) {
            context.clearCookies(
                    new BrowserContext.ClearCookiesOptions().setName(name)
            );
        }

        /**
         * Deletes the specified cookie.
         *
         * @param cookie the Selenium {@link Cookie} to delete
         */
        @Override
        public void deleteCookie(Cookie cookie) {
            deleteCookieNamed(cookie.getName());
        }

        /**
         * Deletes all cookies in the current browser context.
         */
        @Override
        public void deleteAllCookies() {
            context.clearCookies();
        }

        /**
         * Retrieves all cookies from the current browser context.
         *
         * @return a set of Selenium {@link Cookie} objects
         */
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

        /**
         * Retrieves a cookie by its name.
         *
         * @param name the name of the cookie to retrieve
         * @return the Selenium {@link Cookie} with the specified name, or null if not found
         */
        @Override
        public Cookie getCookieNamed(String name) {
            return getCookies().stream()
                    .filter(cookie -> name.equals(cookie.getName()))
                    .findFirst()
                    .orElse(null);
        }

        /**
         * Returns the {@link Timeouts} interface for managing timeouts.
         *
         * @return a new instance of {@link PlaywrightTimeouts}
         */
        @Override
        public Timeouts timeouts() {
            return new PlaywrightTimeouts();
        }

        /**
         * Returns the options for managing the current window.
         *
         * @return a {@link Window} object for controlling the browser window
         */
        @Override
        public Window window() {
            return new PlaywrightWindow();
        }

        /**
         * Returns the interface for managing driver logs.
         * <p>
         * This implementation provides access to console messages captured during
         * test execution.
         *
         * @return a {@link Logs} object to access browser console logs
         */
        @Override
        public Logs logs() {
            return new PlaywrightLogs();
        }

        /**
         * Converts a Selenium SameSite cookie policy string to the corresponding Playwright SameSiteAttribute.
         *
         * @param sameSite the Selenium SameSite policy string (lax, strict, none, etc.)
         * @return the corresponding Playwright SameSiteAttribute, or null if the policy is not recognized
         */
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
     * Implementation of WebDriver.Window for Playwright.
     */
    private class PlaywrightWindow implements Window {

        /**
         * Returns the size of the current browser window's viewport.
         *
         * @return a Dimension object containing the width and height of the viewport
         */
        @Override
        public Dimension getSize() {
            com.microsoft.playwright.options.ViewportSize size = page.viewportSize();
            return new Dimension(size.width, size.height);
        }

        /**
         * Sets the size of the current browser window's viewport.
         *
         * @param targetSize the desired size of the viewport as a Dimension object
         * @throws IllegalArgumentException if the width or height is not positive
         */
        @Override
        public void setSize(Dimension targetSize) {
            if (targetSize.getWidth() <= 0 || targetSize.getHeight() <= 0) {
                throw new IllegalArgumentException("Window size must be positive: " + targetSize);
            }
            page.setViewportSize(targetSize.getWidth(), targetSize.getHeight());
        }

        /**
         * Returns the position of the current browser window.
         * <p>
         * Not supported in Playwright. This method will always throw an UnsupportedOperationException.
         *
         * @return never returns normally
         * @throws UnsupportedOperationException always thrown, as getting window position is not supported
         */
        @Override
        public Point getPosition() {
            throw new UnsupportedOperationException("Getting window position is not supported in Playwright.");
        }

        /**
         * Sets the position of the current browser window.
         * <p>
         * Not supported in Playwright. This method will always throw an UnsupportedOperationException.
         *
         * @param targetPosition the desired position of the window
         * @throws UnsupportedOperationException always thrown, as setting window position is not supported
         */
        @Override
        public void setPosition(Point targetPosition) {
            throw new UnsupportedOperationException("Setting window position is not supported in Playwright.");
        }

        /**
         * Maximizes the current browser window.
         * <p>
         * Not supported in Playwright. This method will always throw an UnsupportedOperationException.
         *
         * @throws UnsupportedOperationException always thrown, as maximizing window is not supported
         */
        @Override
        public void maximize() {
            throw new UnsupportedOperationException("Maximizing window is not supported in Playwright.");
        }

        /**
         * Minimizes the current browser window.
         * <p>
         * Not supported in Playwright. This method will always throw an UnsupportedOperationException.
         *
         * @throws UnsupportedOperationException always thrown, as minimizing window is not supported
         */
        @Override
        public void minimize() {
            throw new UnsupportedOperationException("Minimizing window is not supported in Playwright.");
        }

        /**
         * Sets the current browser window to fullscreen mode.
         * <p>
         * Not supported in Playwright. This method will always throw an UnsupportedOperationException.
         *
         * @throws UnsupportedOperationException always thrown, as fullscreen mode is not supported
         */
        @Override
        public void fullscreen() {
            throw new UnsupportedOperationException("Fullscreen mode is not supported in Playwright.");
        }
    }

    /**
     * Implementation of WebDriver.Logs for Playwright.
     * Provides access to browser console logs captured during test execution.
     */
    private class PlaywrightLogs implements Logs {
        /**
         * Gets all console messages captured for the current window/page.
         *
         * @return a list of LogEntry objects representing console messages
         */
        private List<LogEntry> getMessages() {
            setPageOnConsoleMessage();
            return consoleMessages.getOrDefault(getWindowHandle(), new ArrayList<>());
        }

        /**
         * Currently, this implementation returns all console messages regardless of the
         * requested log type.
         *
         * @param logType the type of logs to retrieve
         * @return a LogEntries object containing all console messages
         */
        @Override
        public LogEntries get(String logType) {
            return new LogEntries(getMessages());
        }

        /**
         * Returns a set of log levels from captured console messages, represented as strings.
         *
         * @return a set of available log types
         */
        @Override
        public Set<String> getAvailableLogTypes() {
            List<LogEntry> messages = getMessages();
            return messages.stream()
                    .map(LogEntry::getLevel)
                    .map(Level::toString)
                    .collect(Collectors.toSet());
        }
    }

    /**
     * Implementation of WebDriver.Timeouts for Playwright.
     */
    private class PlaywrightTimeouts implements Timeouts {
        /**
         * Sets the timeout for implicitly waiting for elements to appear.
         * This timeout applies to all element searches performed by the driver.
         *
         * @param duration the timeout duration for implicit waits
         * @return this Timeouts instance for method chaining
         */
        @Override
        public Timeouts implicitlyWait(Duration duration) {
            context.setDefaultTimeout(duration.toMillis());
            for (Page p : context.pages()) {
                p.setDefaultTimeout(duration.toMillis());
            }
            return this;
        }

        /**
         * Sets the timeout for page load operations.
         * This timeout applies to navigation and loading of resources in the current page.
         *
         * @param duration the timeout duration for page load operations
         * @return this Timeouts instance for method chaining
         */
        @Override
        public Timeouts pageLoadTimeout(Duration duration) {
            context.setDefaultNavigationTimeout(duration.toMillis());
            for (Page p : context.pages()) {
                p.setDefaultNavigationTimeout(duration.toMillis());
            }
            return this;
        }

        /**
         * Sets the script timeout for executing JavaScript in the current page.
         * This timeout applies to all scripts executed via executeScript or executeAsyncScript.
         *
         * @param duration the timeout duration for script execution
         * @return this Timeouts instance for method chaining
         */
        @Override
        public Timeouts scriptTimeout(Duration duration) {
            scriptTimeout = duration;
            return this;
        }
    }

    /**
     * Implementation of WebDriver.Alert for Playwright.
     */
    private class PlaywrightAlert implements Alert {
        /**
         * Dismisses the currently displayed dialog.
         */
        @Override
        public void dismiss() {
            // Dismisses the currently displayed dialog.
            page.onDialog(Dialog::dismiss);
        }

        /**
         * Accepts the currently displayed dialog.
         */
        @Override
        public void accept() {
            page.onDialog(Dialog::accept);
        }

        /**
         * Retrieves the message text from the currently displayed dialog.
         *
         * @return the message text of the dialog
         */
        @Override
        public String getText() {
            List<String> messages = new ArrayList<>();
            page.onDialog(dialog -> messages.add(dialog.message()));
            return messages.get(0);
        }

        /**
         * Sends keys to the currently displayed prompt dialog.
         *
         * @param keysToSend the text to enter into the prompt dialog
         */
        @Override
        public void sendKeys(String keysToSend) {
            page.onDialog(dialog -> dialog.accept(keysToSend));
        }
    }
}