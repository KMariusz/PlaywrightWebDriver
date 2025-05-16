package io.github.kmariusz.playwrightwebdriver;

import org.openqa.selenium.By;

public class SelectorUtils {
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
            } else {
                throw new IllegalArgumentException("Unsupported By selector: " + byString);
            }
        } catch (Exception e) {
            throw new IllegalArgumentException("Failed to convert By selector: " + byString, e);
        }
    }

    private static String escapeCssSelector(String selector) {
        // Escape special CSS selector characters
        return selector.replaceAll("([\\[\\]^$*.|?+{}=!<>:()\\-])", "\\\\$1");
    }

    private static String escapeTextSelector(String text) {
        // Escape single quotes in text selectors by doubling them
        return text.replace("'", "''");
    }

    private static String escapeAttributeValue(String value) {
        // Escape single quotes in attribute values by doubling them
        return value.replace("'", "''");
    }
}
