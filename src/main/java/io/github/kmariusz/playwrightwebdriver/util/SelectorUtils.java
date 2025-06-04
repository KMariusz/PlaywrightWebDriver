package io.github.kmariusz.playwrightwebdriver.util;

import lombok.experimental.UtilityClass;
import org.openqa.selenium.By;

/**
 * Utility class for converting Selenium By selectors to Playwright selector strings.
 */
@UtilityClass
public class SelectorUtils {
    /**
     * Converts a Selenium By selector to a Playwright selector string.
     *
     * @param by The Selenium By selector to convert
     * @return The corresponding Playwright selector string
     * @throws IllegalArgumentException if the By selector is null or unsupported
     */
    public static String convertToPlaywrightSelector(By by) {
        if (by == null) {
            throw new IllegalArgumentException("By selector cannot be null");
        }

        String byString = by.toString();
        try {
            if (byString.startsWith("By.id: ")) {
                String id = byString.substring(7);
                return "#" + escapeCssSelector(id);
            } else if (byString.startsWith("By.className: ")) {
                String className = byString.substring(14);
                return "." + escapeCssSelector(className);
            } else if (byString.startsWith("By.cssSelector: ")) {
                return byString.substring(16);
            } else if (byString.startsWith("By.xpath: ")) {
                return byString.substring(9);
            } else if (byString.startsWith("By.linkText: ")) {
                String text = byString.substring(12);
                return String.format("text='%s'", escapeTextSelector(text));
            } else if (byString.startsWith("By.partialLinkText: ")) {
                String text = byString.substring(19);
                return String.format("text=*%s", escapeTextSelector(text));
            } else if (byString.startsWith("By.name: ")) {
                String name = byString.substring(9);
                return String.format("[name='%s']", escapeAttributeValue(name));
            } else if (byString.startsWith("By.tagName: ")) {
                return byString.substring(12);
            }
        } catch (StringIndexOutOfBoundsException e) {
            throw new IllegalArgumentException("Malformed By selector format: " + byString, e);
        } catch (Exception e) {
            throw new IllegalArgumentException("Error processing selector: " + byString, e);
        }

        throw new IllegalArgumentException("Unsupported By selector type: " + byString);
    }

    /**
     * Escapes special characters in CSS selectors.
     * <p>
     * Ensures characters with special meaning in CSS selectors are properly escaped
     * so they're interpreted as literal characters. Special characters include:
     * [ ] ^ $ * . | ? + { } = ! < > : ( ) -
     * </p>
     *
     * @param selector The CSS selector string to escape
     * @return The escaped selector string
     */
    private static String escapeCssSelector(String selector) {
        return selector.replaceAll("([\\[\\]^$*.|?+{}=!<>:()\\-])", "\\\\$1");
    }

    /**
     * Escapes text content for use in Playwright text selectors.
     * <p>
     * Handles escaping of single quotes by doubling them as per Playwright's text selector syntax.
     * For example, "user's profile" becomes "user''s profile".
     * </p>
     *
     * @param text The text content to escape
     * @return The escaped text string
     */
    private static String escapeTextSelector(String text) {
        return text.replace("'", "''");
    }

    /**
     * Escapes attribute values for use in Playwright attribute selectors.
     * <p>
     * Handles escaping of single quotes by doubling them as per Playwright's attribute selector syntax.
     * For example, "John's data" becomes "John''s data" when used in attribute values.
     * </p>
     *
     * @param value The attribute value to escape
     * @return The escaped attribute value
     */
    private static String escapeAttributeValue(String value) {
        return value.replace("'", "''");
    }
}
