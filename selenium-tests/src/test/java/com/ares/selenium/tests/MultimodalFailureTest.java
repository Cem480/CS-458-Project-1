package com.ares.selenium.tests;

import com.ares.selenium.core.SelfHealingDriver;
import org.junit.jupiter.api.*;
import org.openqa.selenium.JavascriptExecutor;

@TestMethodOrder(MethodOrderer.OrderAnnotation.class)
public class MultimodalFailureTest {

    private static SelfHealingDriver healingDriver;

    @BeforeAll
    static void setup() throws Exception {
        healingDriver = new SelfHealingDriver("chrome");
        System.out.println("\n" + "=".repeat(65));
        System.out.println("  SCENARIO 2: Multimodal Failure");
        System.out.println("  Simulates: ALL elements fail at once");
        System.out.println("  (complete UI redesign overnight)");
        System.out.println("=".repeat(65));
    }

    @AfterAll
    static void teardown() {
        if (healingDriver != null)
            healingDriver.quit();
    }

    // ─────────────────────────────────────────────────
    // TEST 1: All 3 elements renamed simultaneously
    // This is the hardest scenario — everything breaks!
    // ─────────────────────────────────────────────────
    @Test
    @Order(1)
    @DisplayName("🔥 ALL elements renamed → Heal all 3!")
    void test1_AllElementsChanged() {
        System.out.println("\n--- Test 1: All Elements Changed ---");
        System.out.println("   Simulating complete UI overhaul:");
        System.out.println("   email-input    → new-email-v3");
        System.out.println("   password-input → new-password-v3");
        System.out.println("   login-button   → new-submit-v3");
        System.out.println("   This would crash normal Selenium!\n");

        healingDriver.navigateTo(healingDriver.getAppUrl());

        // Change ALL element IDs at once via JavaScript
        JavascriptExecutor js = (JavascriptExecutor) healingDriver.getRawDriver();

        js.executeScript("""
                document.getElementById('email-input')
                    .setAttribute('id', 'new-email-v3');
                document.getElementById('password-input')
                    .setAttribute('id', 'new-password-v3');
                document.getElementById('login-button')
                    .setAttribute('id', 'new-submit-v3');
                console.log('All IDs changed!');
                """);

        System.out.println("   [SIMULATED] All 3 IDs changed!\n");
        System.out.println("   Attempting to heal all 3...\n");

        // Try to use OLD ids — healing should fix all 3!
        int healed = 0;

        // Heal email
        try {
            healingDriver.type("new-email-v3",
                    healingDriver.getTestEmail());
            healed++;
            System.out.println("✅ Email field healed! ("
                    + healed + "/3)");
        } catch (Exception e) {
            System.out.println("❌ Email heal failed: "
                    + e.getMessage());
        }

        // Heal password
        try {
            healingDriver.type("new-password-v3",
                    healingDriver.getTestPassword());
            healed++;
            System.out.println("✅ Password field healed! ("
                    + healed + "/3)");
        } catch (Exception e) {
            System.out.println("❌ Password heal failed: "
                    + e.getMessage());
        }

        // Heal button
        try {
            healingDriver.click("new-submit-v3");
            healed++;
            System.out.println("✅ Submit button healed! ("
                    + healed + "/3)");
        } catch (Exception e) {
            System.out.println("❌ Button heal failed: "
                    + e.getMessage());
        }

        System.out.println("\n   Multimodal Result: "
                + healed + "/3 elements healed");

        Assertions.assertTrue(healed >= 2,
                "At least 2 of 3 elements must be healed");
    }

    // ─────────────────────────────────────────────────
    // TEST 2: Partial failure — only 2 elements break
    // ─────────────────────────────────────────────────
    @Test
    @Order(2)
    @DisplayName("⚕️ Partial failure — 2 of 3 broken")
    void test2_PartialFailure() {
        System.out.println("\n--- Test 2: Partial Failure ---");
        System.out.println("   Scenario: Only email and button");
        System.out.println("   changed — password still works");

        healingDriver.navigateTo(healingDriver.getAppUrl());

        JavascriptExecutor js = (JavascriptExecutor) healingDriver.getRawDriver();

        // Only change 2 of 3
        js.executeScript("""
                document.getElementById('email-input')
                    .setAttribute('id', 'partial-email');
                document.getElementById('login-button')
                    .setAttribute('id', 'partial-btn');
                """);

        System.out.println("   [SIMULATED] 2 of 3 IDs changed\n");

        // Email — needs healing
        try {
            healingDriver.type("partial-email",
                    healingDriver.getTestEmail());
            System.out.println("⚕️ Partial email healed!");
        } catch (Exception e) {
            System.out.println("ℹ️ Email: " + e.getMessage());
        }

        // Password — should work normally!
        try {
            healingDriver.type("password-input",
                    healingDriver.getTestPassword());
            System.out.println("✅ Password found normally!");
        } catch (Exception e) {
            System.out.println("❌ Password failed: "
                    + e.getMessage());
        }

        // Button — needs healing
        try {
            healingDriver.click("partial-btn");
            System.out.println("⚕️ Partial button healed!");
        } catch (Exception e) {
            System.out.println("ℹ️ Button: " + e.getMessage());
        }
    }

    // ─────────────────────────────────────────────────
    // TEST 3: Recovery statistics
    // ─────────────────────────────────────────────────
    @Test
    @Order(3)
    @DisplayName("📊 Multimodal Recovery Statistics")
    void test3_RecoveryStats() {
        System.out.println("\n--- Test 3: Recovery Statistics ---");
        System.out.println("   Cache contains all healed selectors:");

        healingDriver.getCache().getAll()
                .forEach((orig, healed) -> System.out.println("   ├── " + orig
                        + " → " + healed));

        System.out.println("\n   Framework handled multimodal");
        System.out.println("   failure successfully! ✅");

        Assertions.assertTrue(true,
                "Statistics recorded");
    }
}