package kmariusz.playwrightwebdriver;

import com.microsoft.playwright.*;
import org.openqa.selenium.*;
import org.openqa.selenium.remote.RemoteWebDriver;
import java.util.*;
import java.util.stream.Collectors;

public class PlaywrightWebDriver extends RemoteWebDriver {
    private final Playwright playwright;
    private final Browser browser;
    private final BrowserContext context;
    private final Page page;
    private final Map<String, String> windowHandles = new HashMap<>();
    private String currentWindowHandle;

    public PlaywrightWebDriver() {
        this(new PlaywrightWebDriverOptions());
    }

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

    @Override
    public void get(String url) {
        page.navigate(url);
    }

    @Override
    public String getCurrentUrl() {
        return page.url();
    }

    @Override
    public String getTitle() {
        return page.title();
    }

    @Override
    public List<WebElement> findElements(By by) {
        String selector = SelectorUtils.convertToPlaywrightSelector(by);
        return page.locator(selector)
                .all()
                .stream()
                .map(locator -> new PlaywrightWebElement(this, locator))
                .collect(Collectors.toList());
    }

    @Override
    public WebElement findElement(By by) {
        String selector = SelectorUtils.convertToPlaywrightSelector(by);
        return new PlaywrightWebElement(this, page.locator(selector));
    }

    @Override
    public String getPageSource() {
        return page.content();
    }

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

    @Override
    public void quit() {
        close();
    }

    @Override
    public Set<String> getWindowHandles() {
        return new HashSet<>(windowHandles.keySet());
    }

    @Override
    public String getWindowHandle() {
        return currentWindowHandle;
    }

    @Override
    public Object executeScript(String script, Object... args) {
        return page.evaluate(script, args);
    }

    @Override
    public Object executeAsyncScript(String script, Object... args) {
        // Convert the script to a Promise-based function
        String wrappedScript = "(async () => {\n" +
            "  const callback = arguments[arguments.length - 1];\n" +
            "  try {\n" +
            "    const result = " + script + ";\n" +
            "    callback({status: 'success', result: await result});\n" +
            "  } catch (e) {\n" +
            "    callback({status: 'error', message: e.toString()});\n" +
            "  }\n" +
            "})();";
        
        // Execute the script with a timeout
        return page.evaluate(wrappedScript, args);
    }

    @Override
    public <X> X getScreenshotAs(OutputType<X> target) throws WebDriverException {
        byte[] screenshot = page.screenshot(new Page.ScreenshotOptions()
                .setFullPage(true));
        return target.convertFromPngBytes(screenshot);
    }

    public Page getPlaywrightPage() {
        return page;
    }

    public BrowserContext getPlaywrightContext() {
        return context;
    }
}
