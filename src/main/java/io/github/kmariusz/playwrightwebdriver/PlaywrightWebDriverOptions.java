package io.github.kmariusz.playwrightwebdriver;

import lombok.*;
import lombok.experimental.Accessors;

/**
 * Configuration options for PlaywrightWebDriver.
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor(access = AccessLevel.PRIVATE)
@Accessors(chain = true)
public class PlaywrightWebDriverOptions {
    @Builder.Default
    private BrowserTypes browserType = BrowserTypes.CHROMIUM;
    @Builder.Default
    private boolean headless = true;
    @Builder.Default
    private int windowWidth = 1280;
    @Builder.Default
    private int windowHeight = 800;
    @Builder.Default
    private boolean ignoreHTTPSErrors = false;
    @Builder.Default
    private int slowMo = 0;
}
