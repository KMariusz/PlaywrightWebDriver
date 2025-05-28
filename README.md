[![Maven Central](https://img.shields.io/maven-central/v/io.github.kmariusz/playwrightwebdriver)](https://central.sonatype.com/artifact/io.github.kmariusz/playwrightwebdriver)
[![License: MIT](https://img.shields.io/badge/License-MIT-yellow.svg)](https://opensource.org/licenses/MIT)


# Playwright WebDriver

A WebDriver implementation using Microsoft Playwright for Java, providing a Selenium WebDriver-compatible API with
Playwright's powerful automation capabilities.

## Table of Contents
- [Features](#features)
- [Known Bugs](#known-bugs)
- [Requirements](#requirements)
- [Installation](#installation)
- [Usage](#usage)
- [Configuration Options](#configuration-options)
- [License](#license)

## Features

PlaywrightWebDriver was created to quickly and easily replace Selenium WebDriver with Playwright execution engine.

It supports most of the common Selenium operations on page objects and allows you to see how Selenium project tests would behave in Playwright.

It is not comparable to full project migration from Selenium to Playwright. It is highly recommended to do so to fully utilize the potential of Playwright framework.

- Implements Selenium WebDriver interface using Playwright
- Supports Chrome, Firefox, and WebKit browsers
- Headless and headed modes
- Automatic browser management
- Screenshot support
- Asynchronous JavaScript execution

## Known Bugs

### JavaScript Code Execution

JavaScript code in Selenium executed using [JavascriptExecutor](https://www.selenium.dev/selenium/docs/api/java/org/openqa/selenium/JavascriptExecutor.html) behaves differently than in Playwright's [evaluation](https://playwright.dev/docs/evaluating).

The current implementation may not work properly with all types of JavaScript execution.

### Issues

There may be reported [issues](https://github.com/KMariusz/PlaywrightWebDriver/issues) that are still not resolved. Check them out.

## Requirements

- Java 11 or higher
- Playwright browsers installed (automatically handled by Playwright)

## Installation

### Maven

Add the following dependency to your `pom.xml`.
Replace `LATEST_VERSION` with the latest release version
from [Maven Central](https://central.sonatype.com/artifact/io.github.kmariusz/playwrightwebdriver):

```xml
<dependency>
    <groupId>io.github.kmariusz</groupId>
    <artifactId>playwrightwebdriver</artifactId>
    <version>LATEST_VERSION</version>
</dependency>
```

### Gradle

Add the following to your `build.gradle`.
Replace `LATEST_VERSION` with the latest release version
from [Maven Central](https://central.sonatype.com/artifact/io.github.kmariusz/playwrightwebdriver):

```groovy
dependencies {
    implementation 'io.github.kmariusz:playwrightwebdriver:LATEST_VERSION'
}
```

## Usage

### Basic Example

```java
import io.github.kmariusz.playwrightwebdriver.PlaywrightWebDriver;
import io.github.kmariusz.playwrightwebdriver.PlaywrightWebDriverOptions;
import io.github.kmariusz.playwrightwebdriver.BrowserTypes;
import org.openqa.selenium.By;
import org.openqa.selenium.WebElement;

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

You can customize the WebDriver behavior using `PlaywrightWebDriverOptions`:

```java
PlaywrightWebDriverOptions options = PlaywrightWebDriverOptions.builder()
        .browserType(BrowserTypes.FIREFOX)  // Use Firefox
        .headless(false)                    // Run in headed mode
        .windowWidth(1920)                  // Set window width
        .windowHeight(1080)                 // Set window height
        .ignoreHTTPSErrors(true)            // Ignore HTTPS errors
        .slowMo(100)                        // Slow down execution by 100ms
        .build();

PlaywrightWebDriver driver = new PlaywrightWebDriver(options);
```

## License

This project is licensed under the MIT License - see the [LICENSE](LICENSE) file for details.
