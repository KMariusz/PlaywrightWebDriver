package io.github.kmariusz.playwrightwebdriver.util;

import com.microsoft.playwright.Locator;
import com.microsoft.playwright.Page;
import io.github.kmariusz.playwrightwebdriver.PlaywrightWebElement;
import lombok.experimental.UtilityClass;

import java.time.Duration;
import java.util.Map;
import java.util.stream.Collectors;
import java.util.stream.IntStream;

/**
 * Utility class for executing JavaScript code in the context of Playwright's Page or Locator.
 * <p>
 * This class provides static methods to evaluate JavaScript code, handling the conversion of Selenium WebElement
 * arguments (specifically {@link io.github.kmariusz.playwrightwebdriver.PlaywrightWebElement}) to Playwright node handles.
 * <br>
 * <b>Note:</b> All script execution is synchronous. There is no true asynchronous execution; the {@code executeAsyncScript}
 * method is an alias for {@code executeScript} and does not provide asynchronous behavior.
 * </p>
 */
@UtilityClass
public class JavaScriptUtils {

    /**
     * Executes JavaScript code in the context of the given Playwright Page.
     * <p>
     * If any argument is a {@link io.github.kmariusz.playwrightwebdriver.PlaywrightWebElement} (or wraps one),
     * it is converted to a Playwright node handle and passed to the script. If only one such element is present,
     * the script is executed in the context of that node. Otherwise, all arguments are passed as an array.
     * </p>
     *
     * @param page          the Playwright Page to execute the script in
     * @param script        the JavaScript code to execute
     * @param scriptTimeout the timeout for the script execution
     * @param args          arguments to pass to the script; may include PlaywrightWebElement or WrapsElement
     * @return the result of the script execution
     */
    public static Object executeScript(Page page, String script, Duration scriptTimeout, Object... args) {
        Locator dummyLocator = page.locator("body");
        Locator.EvaluateOptions options = new Locator.EvaluateOptions()
                .setTimeout(scriptTimeout.toMillis());

        if (args == null || args.length == 0) {
            return dummyLocator.evaluate("() => {" + script + "}", null, options);
        }

        Map<Integer, PlaywrightWebElement> playwrightWebElements = getPlaywrightWebElements(args);
        if (playwrightWebElements.isEmpty()) {
            script = "(arguments) => {" + script + "}";
            return dummyLocator.evaluate(script, args, options);
        }

        Object[] newArgs = replacePlaywrightWebElements(args, playwrightWebElements);

        if (playwrightWebElements.size() == 1) {
            int index = playwrightWebElements.keySet().iterator().next();
            PlaywrightWebElement element = playwrightWebElements.get(index);
            Locator locator = element.locator();
            script = "(node, arguments) => {" + script.replace("arguments[" + index + "]", "node") + "}";
            return locator
                    .evaluate(script, newArgs, options);
        }

        script = "(arguments) => {" + script + "}";
        return page.evaluate(script, newArgs);
    }

    /**
     * Executes JavaScript code in the context of the given Playwright Page.
     * <p>
     * This method is an alias for {@link #executeScript(Page, String, Duration, Object...)} and does not provide asynchronous execution.
     * All script execution is synchronous.
     * </p>
     *
     * @param page          the Playwright Page to execute the script in
     * @param script        the JavaScript code to execute (synchronously)
     * @param scriptTimeout the timeout for the script execution
     * @param args          arguments to pass to the script
     * @return the result of the script execution
     */
    public static Object executeAsyncScript(Page page, String script, Duration scriptTimeout, Object... args) {
        return executeScript(page, script, scriptTimeout, args);
    }

    /**
     * Extracts all PlaywrightWebElement instances from the argument array, mapping their indices.
     *
     * @param args the argument array
     * @return a map of argument indices to PlaywrightWebElement instances
     */
    private static Map<Integer, PlaywrightWebElement> getPlaywrightWebElements(Object[] args) {
        return IntStream.range(0, args.length)
                .filter(i -> PlaywrightWebElement.instanceOf(args[i]))
                .boxed()
                .collect(Collectors.toMap(
                        i -> i,
                        i -> PlaywrightWebElement.from(args[i])
                ));
    }

    /**
     * Replaces PlaywrightWebElement arguments with Playwright node handles for script execution.
     *
     * @param args                  the original argument array
     * @param playwrightWebElements map of indices to PlaywrightWebElement instances
     * @return a new argument array with PlaywrightWebElements replaced by node handles
     */
    private static Object[] replacePlaywrightWebElements(Object[] args, Map<Integer, PlaywrightWebElement> playwrightWebElements) {
        return IntStream.range(0, args.length)
                .mapToObj(i -> playwrightWebElements.containsKey(i) ? mapPlaywrightWebElement(playwrightWebElements.get(i)) : args[i])
                .toArray();
    }

    /**
     * Maps a PlaywrightWebElement to a Playwright node handle for use in script evaluation.
     *
     * @param playwrightWebElement the PlaywrightWebElement to map
     * @return a Playwright node handle
     */
    private static Object mapPlaywrightWebElement(PlaywrightWebElement playwrightWebElement) {
        return playwrightWebElement.locator().evaluateHandle("(node) => node");
    }
}
