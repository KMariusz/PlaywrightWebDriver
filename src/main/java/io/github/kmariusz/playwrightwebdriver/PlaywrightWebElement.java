package io.github.kmariusz.playwrightwebdriver;

import com.microsoft.playwright.Locator;
import lombok.Getter;
import lombok.RequiredArgsConstructor;
import lombok.experimental.Accessors;
import org.openqa.selenium.*;
import org.openqa.selenium.interactions.Coordinates;
import org.openqa.selenium.remote.RemoteWebElement;

import java.util.List;
import java.util.UUID;
import java.util.stream.Collectors;

@Getter
@RequiredArgsConstructor
@Accessors(fluent = true)
public class PlaywrightWebElement extends RemoteWebElement {
    private final PlaywrightWebDriver driver;
    private final Locator locator;
    private final String id = UUID.randomUUID().toString();

    @Override
    public void click() {
        locator.click();
    }

    @Override
    public void submit() {
        locator.evaluate("element => element.form?.submit()");
    }

    @Override
    public void sendKeys(CharSequence... keysToSend) {
        locator.fill(keysToSend[0].toString());
    }

    @Override
    public void clear() {
        locator.fill("");
    }

    @Override
    public String getTagName() {
        return locator.evaluate("element => element.tagName").toString().toLowerCase();
    }

    @Override
    public String getAttribute(String name) {
        return locator.getAttribute(name);
    }

    @Override
    public boolean isSelected() {
        return locator.isChecked();
    }

    @Override
    public boolean isEnabled() {
        return !locator.isDisabled();
    }

    @Override
    public String getText() {
        return locator.textContent();
    }

    @Override
    public List<WebElement> findElements(By by) {
        String selector = SelectorUtils.convertToPlaywrightSelector(by);
        return locator.locator(selector)
                .all()
                .stream()
                .map(l -> new PlaywrightWebElement(driver, l))
                .collect(Collectors.toList());
    }

    @Override
    public WebElement findElement(By by) {
        String selector = SelectorUtils.convertToPlaywrightSelector(by);
        return new PlaywrightWebElement(driver, locator.locator(selector));
    }

    @Override
    public boolean isDisplayed() {
        return locator.isVisible();
    }

    @Override
    public Point getLocation() {
        var rect = locator.boundingBox();
        if (rect == null) {
            return new Point(0, 0);
        }
        return new Point((int) rect.x, (int) rect.y);
    }

    @Override
    public Dimension getSize() {
        var rect = locator.boundingBox();
        if (rect == null) {
            return new Dimension(0, 0);
        }
        return new Dimension((int) rect.width, (int) rect.height);
    }

    @Override
    public Rectangle getRect() {
        var rect = locator.boundingBox();
        if (rect == null) {
            return new Rectangle(0, 0, 0, 0);
        }
        return new Rectangle(
                (int) rect.x,
                (int) rect.y,
                (int) rect.width,
                (int) rect.height
        );
    }

    @Override
    public String getCssValue(String propertyName) {
        return locator.evaluate("element => getComputedStyle(element)." + propertyName).toString();
    }

    @Override
    public <X> X getScreenshotAs(OutputType<X> target) throws WebDriverException {
        byte[] screenshot = locator.screenshot();
        return target.convertFromPngBytes(screenshot);
    }

    @Override
    public Coordinates getCoordinates() {
        final Rectangle rect = getRect();
        return new Coordinates() {
            @Override
            public Point onScreen() {
                return new Point(rect.x, rect.y);
            }

            @Override
            public Point inViewPort() {
                return new Point(rect.x, rect.y);
            }

            @Override
            public Point onPage() {
                return new Point(rect.x, rect.y);
            }

            @Override
            public Object getAuxiliary() {
                return locator;
            }
        };
    }

    // This method is not part of the WebElement interface
    // but is required by RemoteWebElement in newer Selenium versions
    @Override
    public void setParent(org.openqa.selenium.remote.RemoteWebDriver parent) {
        super.setParent(parent);
    }

    @Override
    public String toString() {
        return String.format("PlaywrightWebElement: %s", locator);
    }
}
