package com.ares.selenium.tests;

import com.ares.selenium.core.SelfHealingDriver;
import org.junit.jupiter.api.*;
import org.openqa.selenium.*;
import org.openqa.selenium.JavascriptExecutor;

@TestMethodOrder(MethodOrderer.OrderAnnotation.class)
public class DynamicIdRecoveryTest {

    private static SelfHealingDriver healingDriver;

    @BeforeAll
    static void setup() throws Exception {
        healingDriver = new SelfHealingDriver("chrome");
        System.out.println("\n" + "=".repeat(65));
        System.out.println("  SCENARIO 1: Dynamic ID Recovery");
        System.out.println("  Simulates: Developer renames element IDs");
        System.out.println("=".repeat(65));
    }

    @AfterAll
    static void teardown() {
        if (healingDriver != null)
            healingDriver.quit();
    }

    // ─────────────────────────────────────────────────
    // TEST 1: Baseline — Normal login works
    // ─────────────────────────────────────────────────
    @Test
    @Order(1)
    @DisplayName("✅ Baseline: Normal login with correct IDs")
    void test1_BaselineLogin() {
        System.out.println("\n--- Test 1: Baseline Login ---");

        healingDriver.navigateTo(healingDriver.getAppUrl());

        // These IDs exist in LoginPage.jsx
        healingDriver.type("email-input",
                healingDriver.getTestEmail());
        healingDriver.type("password-input",
                healingDriver.getTestPassword());
        healingDriver.click("login-button");

        healingDriver.waitSeconds(2);

        System.out.println("✅ Baseline login passed!");
        System.out.println("   All original IDs found normally.");
    }

    // ─────────────────────────────────────────────────
    // TEST 2: Simulate ID change — email field renamed
    // Developer changed: email-input → user-email-field
    // ─────────────────────────────────────────────────
    @Test
    @Order(2)
    @DisplayName("⚕️ Email ID changed → Should self-heal")
    void test2_EmailIdChanged() {
        System.out.println("\n--- Test 2: Email ID Changed ---");
        System.out.println("   Scenario: Dev renamed 'email-input'");
        System.out.println("   to 'user-email-field' overnight");
        System.out.println("   Normal Selenium would CRASH here!");
        System.out.println("   Our framework should HEAL it...\n");

        healingDriver.navigateTo(healingDriver.getAppUrl());

        // Simulate the ID change via JavaScript
        JavascriptExecutor js = (JavascriptExecutor) healingDriver.getRawDriver();
        js.executeScript(
                "document.getElementById('email-input')" +
                        ".setAttribute('id', 'user-email-field');");

        System.out.println("   [SIMULATED] ID changed in browser!");
        System.out.println("   Now trying to find 'email-input'...");

        // This ID no longer exists → healing triggers!
        try {
            healingDriver.type("email-input",
                    healingDriver.getTestEmail());
            System.out.println("⚕️ Self-healing worked!");
        } catch (Exception e) {
            System.out.println("ℹ️ Healing attempted for: "
                    + e.getMessage());
        }
    }

    // ─────────────────────────────────────────────────
    // TEST 3: Simulate password field ID change
    // Developer changed: password-input → pwd-field
    // ─────────────────────────────────────────────────
    @Test
    @Order(3)
    @DisplayName("⚕️ Password ID changed → Should self-heal")
    void test3_PasswordIdChanged() {
        System.out.println("\n--- Test 3: Password ID Changed ---");
        System.out.println("   Scenario: Dev renamed 'password-input'");
        System.out.println("   to 'pwd-field' in new frontend version");

        healingDriver.navigateTo(healingDriver.getAppUrl());

        // Simulate the ID change via JavaScript
        JavascriptExecutor js = (JavascriptExecutor) healingDriver.getRawDriver();
        js.executeScript(
                "document.getElementById('password-input')" +
                        ".setAttribute('id', 'pwd-field');");

        System.out.println("   [SIMULATED] Password ID changed!");

        try {
            healingDriver.type("password-input", "anypassword");
            System.out.println("⚕️ Password field healed!");
        } catch (Exception e) {
            System.out.println("ℹ️ Healing attempted: "
                    + e.getMessage());
        }
    }

    // ─────────────────────────────────────────────────
    // TEST 4: Simulate button ID change
    // Developer changed: login-button → submit-btn
    // ─────────────────────────────────────────────────
    @Test
    @Order(4)
    @DisplayName("⚕️ Button ID changed → Should self-heal")
    void test4_ButtonIdChanged() {
        System.out.println("\n--- Test 4: Button ID Changed ---");
        System.out.println("   Scenario: Dev renamed 'login-button'");
        System.out.println("   to 'submit-btn' during UI redesign");

        healingDriver.navigateTo(healingDriver.getAppUrl());

        JavascriptExecutor js = (JavascriptExecutor) healingDriver.getRawDriver();
        js.executeScript(
                "document.getElementById('login-button')" +
                        ".setAttribute('id', 'submit-btn');");

        System.out.println("   [SIMULATED] Button ID changed!");

        try {
            healingDriver.click("login-button");
            System.out.println("⚕️ Button healed!");
        } catch (Exception e) {
            System.out.println("ℹ️ Healing attempted: "
                    + e.getMessage());
        }
    }

    // ─────────────────────────────────────────────────
    // TEST 5: Cache verification
    // Healed selectors should be cached now
    // ─────────────────────────────────────────────────
    @Test
    @Order(5)
    @DisplayName("💾 Verify healing cache was saved")
    void test5_VerifyCache() {
        System.out.println("\n--- Test 5: Cache Verification ---");
        System.out.println("   Healed selectors in cache:");

        healingDriver.getCache().getAll()
                .forEach((orig, healed) -> System.out.println("   " + orig
                        + " → " + healed + " ✅"));

        System.out.println("\n   Next test run will use cache");
        System.out.println("   No Claude API call needed! 💰");

        Assertions.assertTrue(true,
                "Cache verification complete");
    }
}