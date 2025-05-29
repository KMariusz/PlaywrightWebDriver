package io.github.kmariusz.playwrightwebdriver;

import com.microsoft.playwright.Locator;
import com.microsoft.playwright.Page;
import org.openqa.selenium.WrapsElement;

public class JavaScriptUtils {
    public static Object executeScript(Page page, String script, Object... args) {
        script = script.replace("return ", "");
        if (args == null || args.length == 0) {
            return page.evaluate(script);
        }
        if (args.length == 1 && (WrapsElement.class.isAssignableFrom(args[0].getClass()) || args[0] instanceof PlaywrightWebElement)) {
            Object arg0 = args[0];
            PlaywrightWebElement element;
            if (WrapsElement.class.isAssignableFrom(arg0.getClass()))
                element = (PlaywrightWebElement) ((WrapsElement) arg0).getWrappedElement();
            else
                element = (PlaywrightWebElement) arg0;
            Locator locator = element.locator();
            script = "node => " + script.replace("arguments[0]", "node");
            return locator.evaluate(script);
        }
        return page.evaluate(script, args);
    }

    public static Object executeAsyncScript(Page page, String script, Object... args) {
        return executeScript(page, script, args);
    }
}