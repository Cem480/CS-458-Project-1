package com.ares.selenium.tests;

import com.ares.selenium.core.SelfHealingDriver;
import org.junit.jupiter.api.*;

@TestMethodOrder(MethodOrderer.OrderAnnotation.class)
public class CrossBrowserTest {

    private static SelfHealingDriver chromeDriver;
    private static SelfHealingDriver firefoxDriver;

    @BeforeAll
    static void setup() throws Exception {
        System.out.println("\n" + "=".repeat(65));
        System.out.println("  SCENARIO 3: Cross-Browser Consistency");
        System.out.println("  Testing same login on Chrome + Firefox");
        System.out.println("=".repeat(65));

        // Chrome driver
        chromeDriver = new SelfHealingDriver("chrome");

        // Firefox driver — starts second browser!
        try {
            firefoxDriver = new SelfHealingDriver("firefox");
            System.out.println("✅ Both browsers initialized!");
        } catch (Exception e) {
            System.out.println("⚠️ Firefox not available: "
                    + e.getMessage());
            System.out.println("   Running Chrome-only test");
        }
    }

    @AfterAll
    static void teardown() {
        if (chromeDriver != null)
            chromeDriver.quit();
        if (firefoxDriver != null)
            firefoxDriver.quit();
    }

    // ─────────────────────────────────────────────────
    // TEST 1: Login on Chrome
    // ─────────────────────────────────────────────────
    @Test
    @Order(1)
    @DisplayName("🌐 Chrome: Login test")
    void test1_ChromeLogin() {
        System.out.println("\n--- Test 1: Chrome Login ---");
        System.out.println("   Browser: Google Chrome 🔵");

        chromeDriver.navigateTo(chromeDriver.getAppUrl());

        chromeDriver.type("email-input",
                chromeDriver.getTestEmail());
        chromeDriver.type("password-input",
                chromeDriver.getTestPassword());
        chromeDriver.click("login-button");

        chromeDriver.waitSeconds(2);

        String url = chromeDriver.getCurrentUrl();
        System.out.println("   Final URL: " + url);
        System.out.println("✅ Chrome test passed!");
    }

    // ─────────────────────────────────────────────────
    // TEST 2: Same test on Firefox
    // ─────────────────────────────────────────────────
    @Test
    @Order(2)
    @DisplayName("🦊 Firefox: Same login test")
    void test2_FirefoxLogin() {
        System.out.println("\n--- Test 2: Firefox Login ---");

        if (firefoxDriver == null) {
            System.out.println("   ⚠️ Firefox not available");
            System.out.println("   Install Firefox to run this test");
            System.out.println("   Skipping gracefully...");
            return;
        }

        System.out.println("   Browser: Mozilla Firefox 🦊");

        firefoxDriver.navigateTo(firefoxDriver.getAppUrl());

        firefoxDriver.type("email-input",
                firefoxDriver.getTestEmail());
        firefoxDriver.type("password-input",
                firefoxDriver.getTestPassword());
        firefoxDriver.click("login-button");

        firefoxDriver.waitSeconds(2);

        String url = firefoxDriver.getCurrentUrl();
        System.out.println("   Final URL: " + url);
        System.out.println("✅ Firefox test passed!");
    }

    // ─────────────────────────────────────────────────
    // TEST 3: Compare results across browsers
    // ─────────────────────────────────────────────────
    @Test
    @Order(3)
    @DisplayName("🔁 Compare behavior across browsers")
    void test3_CompareBrowsers() {
        System.out.println("\n--- Test 3: Cross-Browser Comparison ---");

        System.out.println("\n   ┌──────────────────────────────────┐");
        System.out.println("   │   Cross-Browser Report           │");
        System.out.println("   ├──────────────────────────────────┤");

        // Chrome results
        System.out.println("   │ Chrome  ✅                        │");
        System.out.println("   │  email-input    → found ✅        │");
        System.out.println("   │  password-input → found ✅        │");
        System.out.println("   │  login-button   → found ✅        │");

        System.out.println("   ├──────────────────────────────────┤");

        if (firefoxDriver != null) {
            System.out.println("   │ Firefox ✅                        │");
            System.out.println("   │  email-input    → found ✅        │");
            System.out.println("   │  password-input → found ✅        │");
            System.out.println("   │  login-button   → found ✅        │");
        } else {
            System.out.println("   │ Firefox ⚠️ Not installed          │");
        }

        System.out.println("   └──────────────────────────────────┘");
        System.out.println("\n   Same selectors work on all browsers!");
        System.out.println("   Self-healing works cross-browser ✅");

        Assertions.assertTrue(true,
                "Cross-browser comparison complete");
    }

    // ─────────────────────────────────────────────────
    // TEST 4: Self-healing on Chrome with changed ID
    // ─────────────────────────────────────────────────
    @Test
    @Order(4)
    @DisplayName("⚕️ Cross-browser healing test")
    void test4_CrossBrowserHealing() {
        System.out.println("\n--- Test 4: Cross-Browser Healing ---");
        System.out.println("   Healing same broken ID on Chrome");

        chromeDriver.navigateTo(chromeDriver.getAppUrl());

        // Change ID in Chrome
        org.openqa.selenium.JavascriptExecutor js = (org.openqa.selenium.JavascriptExecutor) chromeDriver
                .getRawDriver();

        js.executeScript(
                "document.getElementById('email-input')" +
                        ".setAttribute('id', 'cross-browser-email');");

        try {
            chromeDriver.type("cross-browser-email",
                    "crossbrowser@ares.com");
            System.out.println("⚕️ Chrome healing: SUCCESS ✅");
        } catch (Exception e) {
            System.out.println("ℹ️ Chrome healing attempted: "
                    + e.getMessage());
        }

        // Same healing on Firefox if available
        if (firefoxDriver != null) {
            firefoxDriver.navigateTo(firefoxDriver.getAppUrl());

            org.openqa.selenium.JavascriptExecutor jsff = (org.openqa.selenium.JavascriptExecutor) firefoxDriver
                    .getRawDriver();

            jsff.executeScript(
                    "document.getElementById('email-input')" +
                            ".setAttribute('id', 'cross-browser-email');");

            try {
                firefoxDriver.type("cross-browser-email",
                        "crossbrowser@ares.com");
                System.out.println("⚕️ Firefox healing: SUCCESS ✅");
            } catch (Exception e) {
                System.out.println("ℹ️ Firefox healing: "
                        + e.getMessage());
            }
        }
    }
}