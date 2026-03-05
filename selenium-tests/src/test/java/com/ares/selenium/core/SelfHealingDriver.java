package com.ares.selenium.core;

import org.openqa.selenium.*;
import org.openqa.selenium.chrome.ChromeDriver;
import org.openqa.selenium.chrome.ChromeOptions;
import org.openqa.selenium.firefox.FirefoxDriver;
import org.openqa.selenium.support.ui.WebDriverWait;
import org.openqa.selenium.support.ui.ExpectedConditions;
import io.github.bonigarcia.wdm.WebDriverManager;

import java.io.FileInputStream;
import java.time.Duration;
import java.util.Properties;

public class SelfHealingDriver {

    private WebDriver driver;
    private WebDriverWait wait;
    private final HealingService healingService;
    private final SelectorCache selectorCache;
    private final Properties config;

    // Healing statistics
    private int totalFinds = 0;
    private int healingCount = 0;
    private int failureCount = 0;

    public SelfHealingDriver(String browser) throws Exception {

        // Load config
        config = new Properties();
        config.load(new FileInputStream(
                "src/test/resources/config.properties"));

        // Initialize healing components
        this.selectorCache = new SelectorCache(
                config.getProperty("healing.cache.path",
                        "healing-cache.json"));

        this.healingService = new HealingService(
                config.getProperty("anthropic.api.key"),
                config.getProperty("anthropic.api.model"));

        // Initialize browser
        setupBrowser(browser);

        int timeout = Integer.parseInt(
                config.getProperty("timeout.seconds", "10"));
        this.wait = new WebDriverWait(
                driver, Duration.ofSeconds(timeout));

        System.out.println("🚀 SelfHealingDriver initialized"
                + " with " + browser);
    }

    // ─────────────────────────────────────────
    // CORE: Find element with self-healing
    // ─────────────────────────────────────────
    public WebElement findElement(String elementId) {
        totalFinds++;

        // Step 1: Check healing cache first
        if (selectorCache.contains(elementId)) {
            String cached = selectorCache.get(elementId);
            System.out.println("💾 Using cached: "
                    + elementId + " → " + cached);
            try {
                return wait.until(ExpectedConditions
                        .presenceOfElementLocated(
                                By.id(cached)));
            } catch (Exception e) {
                System.out.println("⚠️ Cached selector failed,"
                        + " re-healing...");
                selectorCache.clear();
            }
        }

        // Step 2: Try original selector
        try {
            WebElement element = wait.until(
                    ExpectedConditions.presenceOfElementLocated(
                            By.id(elementId)));
            System.out.println("✅ Found: #" + elementId);
            return element;

        } catch (TimeoutException e) {

            // Step 3: Original failed → HEAL!
            System.out.println("\n⚕️ HEALING TRIGGERED for: "
                    + elementId);
            healingCount++;

            return healElement(elementId);
        }
    }

    // ─────────────────────────────────────────
    // Healing Process
    // ─────────────────────────────────────────
    private WebElement healElement(String originalId) {
        try {
            // Get current page info
            String pageHtml = driver.getPageSource();
            String pageUrl = driver.getCurrentUrl();

            // Ask Claude
            HealingService.HealingResult result = healingService.healSelector(
                    originalId, pageHtml, pageUrl);

            if (!result.success()) {
                failureCount++;
                throw new RuntimeException(
                        "❌ Healing failed for: " + originalId);
            }

            // Try multiple strategies to apply healed selector
            WebElement element = tryMultipleSelectors(result);

            // Save to cache for next time
            selectorCache.put(originalId, result.newSelector());

            System.out.println("⚕️ HEALED: " + originalId
                    + " → " + result.selectorType()
                    + "='" + result.newSelector() + "'");

            return element;

        } catch (Exception e) {
            failureCount++;
            throw new RuntimeException(
                    "❌ Cannot heal element: " + originalId, e);
        }
    }

    // ─────────────────────────────────────────
    // Try multiple selector strategies
    // ─────────────────────────────────────────
    private WebElement tryMultipleSelectors(
            HealingService.HealingResult result) {

        String selector = result.newSelector();
        String type = result.selectorType();

        System.out.println("   Trying strategy 1: "
                + type + "='" + selector + "'");

        // Strategy 1: Use what Claude suggested
        try {
            return findByType(type, selector);
        } catch (Exception e) {
            System.out.println("   ⚠️ Strategy 1 failed: "
                    + e.getMessage());
        }

        // Strategy 2: Try as CSS selector directly
        System.out.println("   Trying strategy 2: css");
        try {
            return wait.until(ExpectedConditions
                    .presenceOfElementLocated(
                            By.cssSelector(selector)));
        } catch (Exception e) {
            System.out.println("   ⚠️ Strategy 2 failed");
        }

        // Strategy 3: Try by name attribute
        System.out.println("   Trying strategy 3: name");
        try {
            return wait.until(ExpectedConditions
                    .presenceOfElementLocated(
                            By.name(selector)));
        } catch (Exception e) {
            System.out.println("   ⚠️ Strategy 3 failed");
        }

        // Strategy 4: Try by XPath with id
        System.out.println("   Trying strategy 4: xpath id");
        try {
            return wait.until(ExpectedConditions
                    .presenceOfElementLocated(
                            By.xpath("//*[@id='" + selector + "']")));
        } catch (Exception e) {
            System.out.println("   ⚠️ Strategy 4 failed");
        }

        // Strategy 5: Try by XPath with type=email
        System.out.println("   Trying strategy 5: xpath type");
        try {
            return wait.until(ExpectedConditions
                    .presenceOfElementLocated(
                            By.xpath("//input[@type='email']")));
        } catch (Exception e) {
            System.out.println("   ⚠️ Strategy 5 failed");
        }

        throw new RuntimeException(
                "All healing strategies failed for: " + selector);
    }

    // ─────────────────────────────────────────
    // Find by different selector types
    // ─────────────────────────────────────────
    private WebElement findByType(String type, String selector) {
        By by = switch (type.toLowerCase()) {
            case "id" -> By.id(selector);
            case "name" -> By.name(selector);
            case "css" -> By.cssSelector(selector);
            case "xpath" -> By.xpath(selector);
            default -> By.id(selector);
        };

        return wait.until(
                ExpectedConditions.presenceOfElementLocated(by));
    }

    // ─────────────────────────────────────────
    // Convenience Methods
    // ─────────────────────────────────────────
    public void navigateTo(String url) {
        driver.get(url);
        System.out.println("🌐 Navigated to: " + url);
    }

    public void type(String elementId, String text) {
        WebElement element = findElement(elementId);
        element.clear();
        element.sendKeys(text);
        System.out.println("⌨️  Typed '" + text
                + "' into: " + elementId);
    }

    public void click(String elementId) {
        findElement(elementId).click();
        System.out.println("🖱️  Clicked: " + elementId);
    }

    public String getText(String elementId) {
        return findElement(elementId).getText();
    }

    public boolean isElementPresent(String elementId) {
        try {
            wait.until(ExpectedConditions
                    .presenceOfElementLocated(By.id(elementId)));
            return true;
        } catch (Exception e) {
            return false;
        }
    }

    public void waitSeconds(int seconds) {
        try {
            Thread.sleep(seconds * 1000L);
        } catch (InterruptedException e) {
            Thread.currentThread().interrupt();
        }
    }

    public String getCurrentUrl() {
        return driver.getCurrentUrl();
    }

    public String getPageTitle() {
        return driver.getTitle();
    }

    public String getAppUrl() {
        return config.getProperty("app.url",
                "http://localhost:3000");
    }

    public String getTestEmail() {
        return config.getProperty("test.email");
    }

    public String getTestPassword() {
        return config.getProperty("test.password");
    }

    public WebDriver getRawDriver() {
        return driver;
    }

    public SelectorCache getCache() {
        return selectorCache;
    }

    // ─────────────────────────────────────────
    // Print Healing Statistics
    // ─────────────────────────────────────────
    public void printStats() {
        System.out.println("\n" + "=".repeat(50));
        System.out.println("  HEALING STATISTICS");
        System.out.println("=".repeat(50));
        System.out.println("  Total finds:    " + totalFinds);
        System.out.println("  Healed:         " + healingCount);
        System.out.println("  Failed:         " + failureCount);
        System.out.printf("  Health rate:    %.1f%%%n",
                totalFinds > 0
                        ? (totalFinds - failureCount)
                                * 100.0 / totalFinds
                        : 100.0);

        // Print cache contents
        if (!selectorCache.getAll().isEmpty()) {
            System.out.println("\n  Healed Selectors Cache:");
            selectorCache.getAll().forEach((orig, healed) -> System.out.println("  " + orig + " → " + healed));
        }
        System.out.println("=".repeat(50));
    }

    public void quit() {
        printStats();
        if (driver != null)
            driver.quit();
    }

    // ─────────────────────────────────────────
    // Browser Setup
    // ─────────────────────────────────────────
    private void setupBrowser(String browser) {
        boolean headless = Boolean.parseBoolean(
                config.getProperty("headless", "false"));

        switch (browser.toLowerCase()) {
            case "firefox" -> {
                WebDriverManager.firefoxdriver().setup();
                driver = new FirefoxDriver();
            }
            default -> {
                WebDriverManager.chromedriver().setup();
                ChromeOptions options = new ChromeOptions();
                if (headless) {
                    options.addArguments("--headless");
                    options.addArguments("--no-sandbox");
                }
                options.addArguments("--window-size=1920,1080");
                options.addArguments("--disable-search-engine-choice-screen");
                options.addArguments("--log-level=3"); // reduce noise
                driver = new ChromeDriver(options);
            }
        }
    }
}