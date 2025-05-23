# Playwright WebDriver

A WebDriver implementation using Microsoft Playwright for Java, providing a Selenium WebDriver-compatible API with
Playwright's powerful automation capabilities.

## Features

- Implements Selenium WebDriver interface using Playwright
- Supports Chrome, Firefox, and WebKit browsers
- Headless and headed modes
- Automatic browser management
- Screenshot support
- Asynchronous JavaScript execution
- Window and tab management

## Requirements

- Java 11 or higher
- Playwright browsers installed (automatically handled by Playwright)

## Installation

### Maven

Add the following dependency to your `pom.xml`.
Replace `LATEST_VERSION` with the latest release version
from [GitHub Releases](https://github.com/KMariusz/PlaywrightWebDriver/releases):

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
from [GitHub Releases](https://github.com/KMariusz/PlaywrightWebDriver/releases):

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

## Best Practices

1. **Page Object Model**: Use the Page Object pattern to organize your tests and make them more maintainable.
2. **Explicit Waits**: Always use explicit waits instead of thread sleeps.
3. **Resource Management**: Always call `driver.quit()` in a `finally` block to ensure resources are properly released.
4. **Selectors**: Prefer using stable selectors like `data-testid` attributes for element location.
5. **Configuration**: Use `PlaywrightWebDriverOptions` to configure browser settings instead of hardcoding them.

## Contributing

Contributions are welcome! Please feel free to submit a Pull Request.

## License

This project is licensed under the MIT License - see the [LICENSE](LICENSE) file for details.
