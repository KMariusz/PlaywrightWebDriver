package io.github.kmariusz.playwrightwebdriver.util;

import java.util.Map;

import org.openqa.selenium.WrapsElement;

import com.microsoft.playwright.Locator;
import com.microsoft.playwright.Page;

import io.github.kmariusz.playwrightwebdriver.PlaywrightWebElement;

public class JavaScriptUtils {
    
    public static Object executeScript(Page page, String script, Object... args) {
        if (script.startsWith("return ")) {
            script = script.substring(7);
        }

        if (args == null || args.length == 0) {
            return page.evaluate(script);
        }

        Map<Integer, PlaywrightWebElement> playwrightWebElements = getPlaywrightWebElements(args);
        
        if (!playwrightWebElements.isEmpty()) {
            Object[] newArgs = replacePlaywrightWebElements(args, playwrightWebElements);
            
            if (playwrightWebElements.size() == 1) {
                int index = playwrightWebElements.keySet().iterator().next();
                PlaywrightWebElement element = playwrightWebElements.get(index);
                Locator locator = element.locator();
                
                if (args.length == 1) {
                    script = "node => " + script.replace("arguments[" + index + "]", "node");
                    return locator.evaluate(script);
                }
                
                script = "(node, arguments) => " + script.replace("arguments[" + index + "]", "node");
                return locator.evaluate(script, newArgs);
            }
        }
        
        script = "(arguments) => " + script;
        return page.evaluate(script, args);
    }

    public static Object executeAsyncScript(Page page, String script, Object... args) {
        return executeScript(page, script, args);
    }

    public static boolean isPlaywrightWebElement(Object arg) {
        return arg instanceof PlaywrightWebElement || 
               (arg instanceof WrapsElement && 
                ((WrapsElement) arg).getWrappedElement() instanceof PlaywrightWebElement);
    }

    public static Map<Integer, PlaywrightWebElement> getPlaywrightWebElements(Object[] args) {
        Map<Integer, PlaywrightWebElement> playwrightWebElements = new java.util.HashMap<>();
        
        for (int i = 0; i < args.length; i++) {
            if (isPlaywrightWebElement(args[i])) {
                PlaywrightWebElement element = args[i] instanceof WrapsElement ? 
                    (PlaywrightWebElement)((WrapsElement) args[i]).getWrappedElement() : 
                    (PlaywrightWebElement)args[i];
                    
                playwrightWebElements.put(i, element);
            }
        }
        
        return playwrightWebElements;
    }

    public static Object[] replacePlaywrightWebElements(Object[] args, Map<Integer, PlaywrightWebElement> playwrightWebElements) {
        Object[] newArgs = new Object[args.length];
        
        for (int i = 0; i < args.length; i++) {
            newArgs[i] = playwrightWebElements.containsKey(i) ? null : args[i];
        }
        
        return newArgs;
    }
}