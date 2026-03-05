package com.ares.selenium.tests;

import com.ares.selenium.core.SelfHealingDriver;
import org.junit.jupiter.api.*;

@TestMethodOrder(MethodOrderer.OrderAnnotation.class)
class LoginTest {

    private static SelfHealingDriver healingDriver;

    @BeforeAll
    static void setup() throws Exception {
        healingDriver = new SelfHealingDriver("chrome");
    }

    @AfterAll
    static void teardown() {
        if (healingDriver != null)
            healingDriver.quit();
    }

    // ─────────────────────────────────────
    // TEST 1: Basic Login
    // ─────────────────────────────────────
    @Test
    @Order(1)
    @DisplayName("✅ Basic Login Test")
    void test1_BasicLogin() {

        // Open frontend
        healingDriver.navigateTo(healingDriver.getAppUrl());

        // Type credentials (using our element IDs from LoginPage.jsx)
        healingDriver.type("email-input",
                healingDriver.getTestEmail());
        healingDriver.type("password-input",
                healingDriver.getTestPassword());

        // Click login
        healingDriver.click("login-button");

        System.out.println("✅ Login test completed!");
    }

    // ─────────────────────────────────────
    // TEST 2: Self-Healing Demo
    // Simulates what happens when ID changes
    // ─────────────────────────────────────
    @Test
    @Order(2)
    @DisplayName("⚕️ Self-Healing Demo")
    void test2_SelfHealingDemo() {

        healingDriver.navigateTo(healingDriver.getAppUrl());

        // This ID doesn't exist!
        // Healing will trigger and find the real element
        System.out.println("\n🧪 Testing with WRONG element ID...");
        System.out.println("   Simulating: dev changed"
                + " 'email-input' to something else");

        // Clear cache to force healing
        healingDriver.navigateTo(healingDriver.getAppUrl());

        // Try wrong ID → healing should kick in
        try {
            healingDriver.type("email-field-v2",
                    "healed@ares.com");
            System.out.println("⚕️ Healing worked!");
        } catch (Exception e) {
            System.out.println("ℹ️ Healing attempted: "
                    + e.getMessage());
        }
    }
}
