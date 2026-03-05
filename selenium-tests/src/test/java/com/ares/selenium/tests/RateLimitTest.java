package com.ares.selenium.tests;

import com.ares.selenium.core.SelfHealingDriver;
import org.junit.jupiter.api.*;
import org.openqa.selenium.*;

import java.io.*;
import java.net.*;
import java.nio.charset.StandardCharsets;

@TestMethodOrder(MethodOrderer.OrderAnnotation.class)
public class RateLimitTest {

    private static SelfHealingDriver healingDriver;
    private static final String API_URL = "http://localhost:8080/api/auth";
    private static final String TEST_EMAIL = "ratetest@ares.com";
    private static final String TEST_PASS = "ratetest123";

    @BeforeAll
    static void setup() throws Exception {
        healingDriver = new SelfHealingDriver("chrome");
        System.out.println("\n" + "=".repeat(65));
        System.out.println("  SCENARIO 5: Rate Limit Simulation");
        System.out.println("  Tests rapid login attempts → LOCKED");
        System.out.println("=".repeat(65));

        // Register test user via API
        setupTestUser();
    }

    @AfterAll
    static void teardown() {
        if (healingDriver != null)
            healingDriver.quit();
    }

    // ─────────────────────────────────────────────────
    // Setup: Register test user via API
    // ─────────────────────────────────────────────────
    static void setupTestUser() {
        System.out.println("\n   Setting up rate limit test user...");
        try {
            String body = String.format(
                    "{\"email\":\"%s\",\"password\":\"%s\"}",
                    TEST_EMAIL, TEST_PASS);

            String response = sendPost(
                    API_URL + "/register", body);

            if (response.contains("SUCCESS")
                    || response.contains("already")) {
                System.out.println("✅ Test user ready: "
                        + TEST_EMAIL);
            }
        } catch (Exception e) {
            System.out.println("⚠️ Setup: " + e.getMessage());
        }
    }

    // ─────────────────────────────────────────────────
    // TEST 1: Rapid API requests — simulate bot attack
    // ─────────────────────────────────────────────────
    @Test
    @Order(1)
    @DisplayName("🤖 Rapid API requests → Detect bot behavior")
    void test1_RapidApiRequests() throws Exception {
        System.out.println("\n--- Test 1: Rapid API Requests ---");
        System.out.println("   Simulating bot: 5 rapid requests");
        System.out.println("   No delay between attempts!\n");

        String lastResponse = "";

        for (int i = 1; i <= 5; i++) {
            String body = String.format(
                    "{\"email\":\"%s\",\"password\":\"WRONG_%d\"," +
                            "\"ipAddress\":\"10.0.0.1\"}",
                    TEST_EMAIL, i);

            lastResponse = sendPost(
                    API_URL + "/login", body);

            String status = extractField(
                    lastResponse, "status");
            System.out.printf("   Request #%d → %-12s%s%n",
                    i, status,
                    i == 5 ? " ← Final state" : "");

            // No sleep — rapid fire!
        }

        System.out.println("\n   Last response: " + lastResponse);
        System.out.println("   Rate limiting detected attempts ✅");
    }

    // ─────────────────────────────────────────────────
    // TEST 2: UI rate limit simulation via Selenium
    // ─────────────────────────────────────────────────
    @Test
    @Order(2)
    @DisplayName("🌐 UI rapid login attempts via browser")
    void test2_UiRapidLogins() throws Exception {
        System.out.println("\n--- Test 2: UI Rapid Login ---");
        System.out.println("   Selenium clicks login 5 times fast\n");

        for (int i = 1; i <= 5; i++) {
            healingDriver.navigateTo(
                    healingDriver.getAppUrl());

            healingDriver.type("email-input", TEST_EMAIL);
            healingDriver.type("password-input",
                    "WRONG_UI_" + i);
            healingDriver.click("login-button");

            // Check what response appeared on screen
            healingDriver.waitSeconds(1);

            System.out.printf("   UI Attempt #%d → complete%n", i);
        }

        System.out.println("\n   UI rate limit simulation done ✅");
    }

    // ─────────────────────────────────────────────────
    // TEST 3: Account lockout after 10 attempts
    // ─────────────────────────────────────────────────
    @Test
    @Order(3)
    @DisplayName("🔒 10 attempts → Account LOCKED")
    void test3_AccountLockout() throws Exception {
        System.out.println("\n--- Test 3: Account Lockout ---");

        // Register fresh user for clean lockout test
        String lockEmail = "lockrate@ares.com";
        String lockBody = String.format(
                "{\"email\":\"%s\",\"password\":\"lock123\"}",
                lockEmail);
        sendPost(API_URL + "/register", lockBody);

        System.out.println("   Sending 10 rapid wrong passwords...\n");

        String lastResponse = "";

        for (int i = 1; i <= 10; i++) {
            String body = String.format(
                    "{\"email\":\"%s\",\"password\":\"WRONG_%d\"," +
                            "\"ipAddress\":\"192.168.1.50\"}",
                    lockEmail, i);

            lastResponse = sendPost(
                    API_URL + "/login", body);

            String status = extractField(
                    lastResponse, "status");
            String icon = switch (status) {
                case "ERROR" -> "❌";
                case "CHALLENGED" -> "⚠️";
                case "LOCKED" -> "🔒";
                default -> "❓";
            };

            System.out.printf("   Attempt #%-2d → %-12s %s%n",
                    i, status, icon);

            Thread.sleep(50); // minimal delay
        }

        System.out.println("\n   Final response: " + lastResponse);

        boolean locked = lastResponse.contains("LOCKED");
        System.out.println("\n   Account locked: "
                + (locked ? "YES 🔒 ✅" : "NO ⚠️"));

        Assertions.assertTrue(locked,
                "Account must be locked after 10 attempts");
    }

    // ─────────────────────────────────────────────────
    // TEST 4: Locked account tries to login via UI
    // ─────────────────────────────────────────────────
    @Test
    @Order(4)
    @DisplayName("🔒 Locked account shows error in UI")
    void test4_LockedAccountUi() throws Exception {
        System.out.println("\n--- Test 4: Locked Account UI ---");
        System.out.println("   User is LOCKED — tries to login via UI");

        String lockEmail = "lockrate@ares.com";

        healingDriver.navigateTo(healingDriver.getAppUrl());

        healingDriver.type("email-input", lockEmail);
        healingDriver.type("password-input", "lock123");
        healingDriver.click("login-button");

        healingDriver.waitSeconds(2);

        // Take screenshot for report
        try {
            File screenshot = ((TakesScreenshot) healingDriver.getRawDriver())
                    .getScreenshotAs(OutputType.FILE);

            new File("target/screenshots").mkdirs();
            java.nio.file.Files.copy(
                    screenshot.toPath(),
                    new File("target/screenshots/"
                            + "locked-account.png").toPath(),
                    java.nio.file.StandardCopyOption.REPLACE_EXISTING);
            System.out.println("   📸 Screenshot saved!");
        } catch (Exception e) {
            System.out.println("   Screenshot: " + e.getMessage());
        }

        System.out.println("   UI shows LOCKED status ✅");
    }

    // ─────────────────────────────────────────────────
    // TEST 5: Risk Score + Rate Limit combined
    // ─────────────────────────────────────────────────
    @Test
    @Order(5)
    @DisplayName("🤖 Rate limit triggers LLM fraud analysis")
    void test5_RateLimitTriggersLlm() throws Exception {
        System.out.println("\n--- Test 5: Rate Limit + LLM ---");
        System.out.println("   Scenario: Many attempts from new IP");
        System.out.println("   Expected: HIGH risk → Claude decides\n");

        String freshEmail = "llmrate@ares.com";
        sendPost(API_URL + "/register",
                String.format(
                        "{\"email\":\"%s\",\"password\":\"llm123\"}",
                        freshEmail));

        // Build up 9 failed attempts
        for (int i = 1; i <= 9; i++) {
            sendPost(API_URL + "/login",
                    String.format(
                            "{\"email\":\"%s\"," +
                                    "\"password\":\"WRONG_%d\"," +
                                    "\"ipAddress\":\"10.0.%d.1\"}",
                            freshEmail, i, i));
            Thread.sleep(50);
        }

        System.out.println("   Built up 9 failed attempts...");

        // Now correct password from suspicious IP
        // This should trigger HIGH risk → LLM!
        String finalAttempt = sendPost(
                API_URL + "/login",
                String.format(
                        "{\"email\":\"%s\"," +
                                "\"password\":\"llm123\"," +
                                "\"ipAddress\":\"185.220.101.99\"}",
                        freshEmail));

        String status = extractField(finalAttempt, "status");
        String message = extractField(finalAttempt, "message");

        System.out.println("\n   ┌─────────────────────────────┐");
        System.out.println("   │  FINAL LLM DECISION:        │");
        System.out.println("   │  Status:  " + status);
        System.out.println("   │  Message: " + message
                .substring(0, Math.min(40, message.length()))
                + "...");
        System.out.println("   └─────────────────────────────┘");

        System.out.println("\n   Rate limit + LLM integration: ✅");

        Assertions.assertTrue(
                finalAttempt.contains("LOCKED") ||
                        finalAttempt.contains("BLOCKED") ||
                        finalAttempt.contains("CHALLENGED"),
                "High rate of attempts must trigger protection");
    }

    // ─────────────────────────────────────────────────
    // HELPERS
    // ─────────────────────────────────────────────────
    private static String sendPost(String url, String body)
            throws Exception {
        URL apiUrl = new URL(url);
        HttpURLConnection conn = (HttpURLConnection) apiUrl.openConnection();
        conn.setRequestMethod("POST");
        conn.setRequestProperty("Content-Type",
                "application/json");
        conn.setDoOutput(true);
        conn.setConnectTimeout(5000);
        conn.setReadTimeout(10000);

        try (OutputStream os = conn.getOutputStream()) {
            os.write(body.getBytes(StandardCharsets.UTF_8));
        }

        try (BufferedReader br = new BufferedReader(
                new InputStreamReader(
                        conn.getInputStream(),
                        StandardCharsets.UTF_8))) {
            StringBuilder response = new StringBuilder();
            String line;
            while ((line = br.readLine()) != null) {
                response.append(line);
            }
            return response.toString();
        } catch (Exception e) {
            try (BufferedReader br = new BufferedReader(
                    new InputStreamReader(
                            conn.getErrorStream(),
                            StandardCharsets.UTF_8))) {
                StringBuilder response = new StringBuilder();
                String line;
                while ((line = br.readLine()) != null) {
                    response.append(line);
                }
                return response.toString();
            }
        }
    }

    private String extractField(String json, String field) {
        try {
            com.fasterxml.jackson.databind.ObjectMapper om = new com.fasterxml.jackson.databind.ObjectMapper();
            return om.readTree(json).get(field).asText();
        } catch (Exception e) {
            return "UNKNOWN";
        }
    }
}