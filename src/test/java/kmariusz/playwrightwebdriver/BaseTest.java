package kmariusz.playwrightwebdriver;

import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.openqa.selenium.WebDriver;

import lombok.Getter;

@Getter
public class BaseTest {
    protected WebDriver driver;
    
    @BeforeEach
    public void setUp() {
        PlaywrightWebDriverOptions options = new PlaywrightWebDriverOptions();
        options.setHeadless(false);
        options.setWindowWidth(1280);
        options.setWindowHeight(800);        
        driver = new PlaywrightWebDriver(options);
    }

    @AfterEach
    public void tearDown() {
        if (driver != null) {
            driver.quit();
        }
    }
}
