package io.github.kmariusz.playwrightwebdriver;

import com.microsoft.playwright.Locator;
import io.github.kmariusz.playwrightwebdriver.util.SelectorUtils;
import lombok.Getter;
import lombok.RequiredArgsConstructor;
import lombok.experimental.Accessors;
import org.openqa.selenium.By;
import org.openqa.selenium.Dimension;
import org.openqa.selenium.OutputType;
import org.openqa.selenium.Point;
import org.openqa.selenium.Rectangle;
import org.openqa.selenium.SearchContext;
import org.openqa.selenium.TakesScreenshot;
import org.openqa.selenium.WebDriver;
import org.openqa.selenium.WebDriverException;
import org.openqa.selenium.WebElement;
import org.openqa.selenium.WrapsDriver;
import org.openqa.selenium.WrapsElement;
import org.openqa.selenium.interactions.Coordinates;
import org.openqa.selenium.interactions.Locatable;
import org.openqa.selenium.remote.RemoteWebElement;

import java.util.List;
import java.util.UUID;
import java.util.stream.Collectors;

/**
 * Implementation of Selenium's WebElement interface using Playwright.
 * This class bridges Selenium's WebDriver API with Playwright's automation capabilities,
 * allowing Playwright to be used with existing Selenium-based test frameworks.
 */
@Getter
@RequiredArgsConstructor
@Accessors(fluent = true)
public class PlaywrightWebElement extends RemoteWebElement implements WebElement, Locatable, TakesScreenshot, WrapsDriver {
    /**
     * The Playwright WebDriver instance that created this element
     */
    private final PlaywrightWebDriver driver;

    /**
     * The Playwright Locator that represents this element
     */
    private final Locator locator;

    /**
     * Unique identifier for this element
     */
    private final String playwrightElementId = UUID.randomUUID().toString();

    /**
     * Checks if the argument is a PlaywrightWebElement or wraps one.
     *
     * @param arg the argument to check
     * @return true if the argument is or wraps a PlaywrightWebElement, false otherwise
     */
    public static boolean instanceOf(Object arg) {
        return arg instanceof PlaywrightWebElement ||
                (arg instanceof WrapsElement &&
                        ((WrapsElement) arg).getWrappedElement() instanceof PlaywrightWebElement);
    }

    /**
     * Returns the unique identifier for this element.
     *
     * @return A UUID string that uniquely identifies this element instance
     */
    @Override
    public String getId() {
        return playwrightElementId;
    }

    /**
     * Clicks the element.
     */
    @Override
    public void click() {
        locator.click();
    }

    /**
     * Submits a form if this element is within a form.
     * This will find the enclosing form element and trigger its submit action.
     * If the element is not within a form, this method will have no effect.
     */
    @Override
    public void submit() {
        locator.evaluate("element => element.form?.submit()");
    }

    /**
     * Simulates typing into the element.
     * This method will first clear any existing content before typing the new text.
     *
     * @param keysToSend Character sequence(s) to send to the element
     */
    @Override
    public void sendKeys(CharSequence... keysToSend) {
        StringBuilder textToType = new StringBuilder();
        for (CharSequence keys : keysToSend) {
            textToType.append(keys);
        }
        locator.fill(textToType.toString());
    }

    /**
     * Clears the content of this element.
     */
    @Override
    public void clear() {
        locator.fill("");
    }

    /**
     * Gets the tag name of this element.
     *
     * @return The tag name in lowercase
     */
    @Override
    public String getTagName() {
        return locator.evaluate("element => element.tagName").toString().toLowerCase();
    }

    /**
     * Gets the value of the specified attribute of this element.
     *
     * @param name The name of the attribute
     * @return The attribute's value or null if not present
     */
    @Override
    public String getAttribute(String name) {
        return locator.getAttribute(name);
    }

    /**
     * Determines if this element is selected or checked.
     *
     * @return true if the element is selected/checked, false otherwise
     */
    @Override
    public boolean isSelected() {
        return locator.isChecked();
    }

    /**
     * Determines whether this element is enabled or not.
     *
     * @return true if the element is enabled, false otherwise
     */
    @Override
    public boolean isEnabled() {
        return !locator.isDisabled();
    }

    /**
     * Gets the visible text of this element.
     *
     * @return The visible text content
     */
    @Override
    public String getText() {
        return locator.textContent();
    }

    /**
     * Finds all elements within the current context using the given mechanism.
     *
     * @param by The locating mechanism to use
     * @return A list of all WebElements found, or an empty list if nothing matches
     */
    @Override
    public List<WebElement> findElements(By by) {
        String selector = SelectorUtils.convertToPlaywrightSelector(by);
        return locator.locator(selector)
                .all()
                .stream()
                .map(l -> new PlaywrightWebElement(driver, l))
                .collect(Collectors.toList());
    }

    /**
     * Finds the first element within the current context using the given mechanism.
     *
     * @param by The locating mechanism to use
     * @return The first matching element
     */
    @Override
    public WebElement findElement(By by) {
        String selector = SelectorUtils.convertToPlaywrightSelector(by);
        return new PlaywrightWebElement(driver, locator.locator(selector));
    }

    /**
     * Returns the shadow DOM root of this element if the element has an open shadow root.
     * This allows querying elements in the shadow DOM of this element.
     *
     * @return A SearchContext representing the shadow root that can be used to find elements within the shadow DOM
     */
    @Override
    public SearchContext getShadowRoot() {
        return this;
    }

    /**
     * Determines if this element is displayed or not.
     *
     * @return true if the element is displayed, false otherwise
     */
    @Override
    public boolean isDisplayed() {
        return locator.isVisible();
    }

    /**
     * Gets the location of the element in the renderable canvas.
     *
     * @return The point containing the coordinates of the upper-left corner of the element
     */
    @Override
    public Point getLocation() {
        var rect = locator.boundingBox();
        if (rect == null) {
            return new Point(0, 0);
        }
        return new Point((int) rect.x, (int) rect.y);
    }

    /**
     * Gets the size of the element on screen.
     *
     * @return The dimension containing the width and height of the element
     */
    @Override
    public Dimension getSize() {
        var rect = locator.boundingBox();
        if (rect == null) {
            return new Dimension(0, 0);
        }
        return new Dimension((int) rect.width, (int) rect.height);
    }

    /**
     * Gets the position and size of the rendered element.
     *
     * @return A rectangle containing the size and position of the element
     */
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

    /**
     * Gets the computed value of a CSS property.
     *
     * @param propertyName The name of the CSS property
     * @return The value of the property
     */
    @Override
    public String getCssValue(String propertyName) {
        return locator.evaluate("element => getComputedStyle(element)." + propertyName).toString();
    }

    /**
     * Takes a screenshot of this element.
     *
     * @param <X>    Return type of the screenshot output
     * @param target Target output type
     * @return The screenshot as the specified output type
     * @throws WebDriverException When the operation fails
     */
    @Override
    public <X> X getScreenshotAs(OutputType<X> target) throws WebDriverException {
        byte[] screenshot = locator.screenshot();
        return target.convertFromPngBytes(screenshot);
    }

    /**
     * Gets the coordinates of the element.
     *
     * @return A coordinates object containing various coordinate systems for this element
     */
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

    /**
     * Sets the parent driver for this element.
     * This method is not part of the WebElement interface but is required by
     * RemoteWebElement in newer Selenium versions.
     *
     * @param parent The parent RemoteWebDriver instance
     */
    @Override
    public void setParent(org.openqa.selenium.remote.RemoteWebDriver parent) {
        super.setParent(parent);
    }

    /**
     * Returns a string representation of this element.
     *
     * @return A string containing the element description
     */
    @Override
    public String toString() {
        return String.format("PlaywrightWebElement: %s", locator);
    }

    /**
     * Gets the WebDriver instance that controls this element.
     *
     * @return The WebDriver instance associated with this element
     */
    @Override
    public WebDriver getWrappedDriver() {
        return driver;
    }
}
