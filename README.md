[![Maven Central](https://img.shields.io/maven-central/v/io.github.kmariusz/playwrightwebdriver)](https://central.sonatype.com/artifact/io.github.kmariusz/playwrightwebdriver)
[![License: MIT](https://img.shields.io/badge/License-MIT-yellow.svg)](https://opensource.org/licenses/MIT)


# Playwright WebDriver

A WebDriver implementation using Microsoft Playwright for Java, providing a Selenium WebDriver-compatible API with
Playwright's powerful automation capabilities.

## Table of Contents
- [Features](#features)
- [Limitations](#limitations)
- [Requirements](#requirements)
- [Installation](#installation)
- [Usage](#usage)
- [Configuration Options](#configuration-options)
- [License](#license)
- [Support](#support)

## Features

PlaywrightWebDriver was created to quickly and easily replace Selenium WebDriver with Playwright execution engine.

It supports most of the common Selenium operations on page objects and allows you to see how Selenium project tests would behave in Playwright.

It is not comparable to full project migration from Selenium to Playwright. It is highly recommended to do so to fully utilize the potential of Playwright framework.

## Limitations

Selenium and Playwright differ in some aspects so much that it is not possible to support all possible use cases.<br>
Some of such cases are listed below.

### Alerts Handling

Main difference in alerts handling between Selenium and Playwright is timing model. In Selenium, it is synchronous - interaction is done after alert appears.<br>
In Playwright, it is asynchronous - handler is pre-registered before dialog appears.<br>
It means that such cases will not work properly by simple WebDriver replacement.

### Window Management

In Selenium there are methods which allow to resize and move browser window, such as `driver.manage().window().maximize()` or `driver.manage().window().setPosition()`.<br>
In Playwright, there is no such functionality. Browser window cannot be resized or moved on runtime.<br>
This means that such methods will not work properly by simple WebDriver replacement.<br>
Only methods `setSize()` and `getSize()` are supported by PlaywrightWebDriver, but they will not change the browser window size, only the Playwright page viewport size.

### Logs

Playwright does not support browser logs in the same way as Selenium does.<br>
In Selenium, you can retrieve different types of logs (browser, driver, performance) using `driver.manage().logs()`.<br>
Playwright does not have a direct equivalent for this functionality.<br>
Playwright use [ConsoleMessages](https://playwright.dev/java/docs/api/class-consolemessage).
Those messages are mapped in PlaywrightWebDriver as a list of `org.openqa.selenium.logging.LogEntry` objects, but it does not support other types of logs.<br>

### JavaScript Code Execution

JavaScript code in Selenium executed using [JavascriptExecutor](https://www.selenium.dev/selenium/docs/api/java/org/openqa/selenium/JavascriptExecutor.html) behaves differently than in Playwright's [evaluation](https://playwright.dev/docs/evaluating).

The current implementation has been tested with various examples, but it may still not work correctly with some complex scripts.<br>
Please report any [issues](https://github.com/KMariusz/PlaywrightWebDriver/issues) you encounter.

### WebDriver BiDi

Playwright does not support WebDriver BiDi protocol. It is a work in progress and will be implemented in the future.
Current status can be tracked [here](https://github.com/microsoft/playwright/issues/32577)

### Issues

There may be reported [issues](https://github.com/KMariusz/PlaywrightWebDriver/issues) that are still not resolved. Check them out.

## Requirements

- Java 11 or higher
- Compatible with Selenium WebDriver 4.x

## Installation

Snippets with the latest version of the PlaywrightWebDriver dependency can be found in [Maven Central](https://central.sonatype.com/artifact/io.github.kmariusz/playwrightwebdriver)

## Usage

### Basic Example

```java
import io.github.kmariusz.playwrightwebdriver.PlaywrightWebDriver;
import io.github.kmariusz.playwrightwebdriver.config.PlaywrightWebDriverOptions;
import io.github.kmariusz.playwrightwebdriver.config.BrowserTypes;
import org.openqa.selenium.By;
import org.openqa.selenium.WebElement;
import org.openqa.selenium.OutputType;
import org.openqa.selenium.TakesScreenshot;
import java.io.File;

public class ExampleTest {
    public static void main(String[] args) {
        // Create a new instance with default options (headless Chrome)
        PlaywrightWebDriver driver = new PlaywrightWebDriver();

        try {
            // Navigate to a website
            driver.get("https://example.com");

            // Find an element and interact with it
            WebElement element = driver.findElement(By.id("someId"));
            element.click();

            // Take a screenshot
            File screenshot = ((TakesScreenshot) driver).getScreenshotAs(OutputType.FILE);

            // Execute JavaScript
            driver.executeScript("alert('Hello from Playwright WebDriver!');");

        } finally {
            // Always close the driver to release resources
            driver.quit();
        }
    }
}
```

## Configuration Options

You can customize the WebDriver behavior using `PlaywrightWebDriverOptions`. This class provides a builder pattern to configure various aspects of browser behavior:

```java
import io.github.kmariusz.playwrightwebdriver.PlaywrightWebDriver;
import io.github.kmariusz.playwrightwebdriver.config.PlaywrightWebDriverOptions;
import io.github.kmariusz.playwrightwebdriver.config.BrowserTypes;
import com.microsoft.playwright.Browser;
import com.microsoft.playwright.BrowserType;
import com.microsoft.playwright.Playwright;

// Create options using the builder pattern
PlaywrightWebDriverOptions options = PlaywrightWebDriverOptions.builder()
        .browserType(BrowserTypes.FIREFOX)  // Choose browser type: CHROMIUM (default), FIREFOX, or WEBKIT
        .build();

// Or use the fluent API with setter methods
PlaywrightWebDriverOptions options = new PlaywrightWebDriverOptions()
        .setBrowserType(BrowserTypes.FIREFOX)
        .setLaunchOptions(new BrowserType.LaunchOptions()
            .setHeadless(false)
            .setSlowMo(100))
        .setContextOptions(new Browser.NewContextOptions()
            .setViewportSize(1920, 1080)
            .setIgnoreHTTPSErrors(true));

PlaywrightWebDriver driver = new PlaywrightWebDriver(options);
```

## License

This project is licensed under the MIT License - see the [LICENSE](LICENSE) file for details.

## Support

For issues, feature requests, or questions, please [create an issue](https://github.com/KMariusz/PlaywrightWebDriver/issues) on GitHub.
