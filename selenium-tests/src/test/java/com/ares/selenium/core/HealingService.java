package com.ares.selenium.core;

import com.fasterxml.jackson.databind.ObjectMapper;
import okhttp3.*;

import java.util.List;
import java.util.Map;

public class HealingService {

    private final String apiKey;
    private final String model;
    private final OkHttpClient httpClient = new OkHttpClient.Builder()
            .callTimeout(java.time.Duration.ofSeconds(30))
            .build();
    private final ObjectMapper objectMapper = new ObjectMapper();

    public HealingService(String apiKey, String model) {
        this.apiKey = apiKey;
        this.model = model;
    }

    // ─────────────────────────────────────────
    // MAIN: Ask Claude to find new selector
    // ─────────────────────────────────────────
    public HealingResult healSelector(String originalSelector,
            String pageHtml,
            String pageUrl) {
        System.out.println("\n🔧 HEALING: Cannot find '"
                + originalSelector + "'");
        System.out.println("   Asking Claude for help...");

        try {
            String prompt = buildPrompt(
                    originalSelector, pageHtml, pageUrl);

            String systemPrompt = """
                    You are a Selenium test healing assistant.
                    When given an HTML page and a missing element selector,
                    find the best matching element and return ONLY valid JSON.
                    No explanation, no markdown, just JSON.
                    Use double quotes inside JSON values, never single quotes.

                    Response format:
                    {
                      "selector": "new-selector-value",
                      "type": "id" or "name" or "css" or "xpath",
                      "confidence": 0-100,
                      "reason": "why this selector matches"
                    }

                    Rules for selector values:
                    - For type "id": just the id value, e.g. "email-input"
                    - For type "name": just the name value, e.g. "email"
                    - For type "css": valid CSS, e.g. "input[type=email]"
                    - For type "xpath": valid xpath, e.g. "//input[@type='email']"
                    """;

            // Build request body
            Map<String, Object> requestBody = Map.of(
                    "model", model,
                    "max_tokens", 256,
                    "system", systemPrompt,
                    "messages", List.of(
                            Map.of("role", "user", "content", prompt)));

            String json = objectMapper
                    .writeValueAsString(requestBody);

            // Call Claude API
            Request request = new Request.Builder()
                    .url("https://api.anthropic.com/v1/messages")
                    .header("x-api-key", apiKey)
                    .header("anthropic-version", "2023-06-01")
                    .header("Content-Type", "application/json")
                    .post(RequestBody.create(json,
                            MediaType.get("application/json")))
                    .build();

            try (Response response = httpClient
                    .newCall(request).execute()) {

                String responseBody = response.body().string();
                System.out.println("   Claude raw response: "
                        + responseBody);
                return parseResponse(responseBody,
                        originalSelector);
            }

        } catch (Exception e) {
            System.out.println("❌ Claude API error: "
                    + e.getMessage());
            return HealingResult.failed(originalSelector);
        }
    }

    // ─────────────────────────────────────────
    // Build the prompt for Claude
    // ─────────────────────────────────────────
    private String buildPrompt(String originalSelector,
            String pageHtml,
            String pageUrl) {
        // Trim HTML to avoid token limits
        String trimmedHtml = pageHtml.length() > 3000
                ? pageHtml.substring(0, 3000) + "..."
                : pageHtml;

        return String.format("""
                I am running a Selenium test on this page: %s

                I cannot find this element by ID: '%s'

                Here is the current page HTML:
                %s

                Please find the correct selector for this element.
                The element is likely a form input field or button.

                IMPORTANT:
                - Return ONLY the JSON response, nothing else
                - No markdown, no code blocks, no explanation
                - Use double quotes in CSS selectors, not single quotes
                - For CSS type, use format like: input[type="email"]
                """,
                pageUrl, originalSelector, trimmedHtml);
    }

    // ─────────────────────────────────────────
    // Parse Claude response
    // ─────────────────────────────────────────
    private HealingResult parseResponse(String rawResponse,
            String original) {
        try {
            Map responseMap = objectMapper
                    .readValue(rawResponse, Map.class);
            List contentList = (List) responseMap.get("content");
            Map firstContent = (Map) contentList.get(0);
            String text = (String) firstContent.get("text");

            // Clean up any markdown formatting
            text = text.trim()
                    .replaceAll("```json", "")
                    .replaceAll("```", "")
                    .trim();

            System.out.println("   Parsed text: " + text);

            Map result = objectMapper.readValue(text, Map.class);

            String selector = (String) result.get("selector");
            String type = (String) result.get("type");
            int confidence = 0;

            // Handle confidence being int or double
            Object confObj = result.get("confidence");
            if (confObj instanceof Integer) {
                confidence = (Integer) confObj;
            } else if (confObj instanceof Double) {
                confidence = ((Double) confObj).intValue();
            }

            String reason = (String) result.get("reason");

            // ── Clean up the selector ──────────────
            // Remove any surrounding quotes
            selector = selector.trim()
                    .replaceAll("^['\"]|['\"]$", "");

            // Fix single quotes in CSS to double quotes
            if ("css".equalsIgnoreCase(type)) {
                selector = selector.replace("'", "\"");
            }

            // Normalize type to lowercase
            type = type.toLowerCase().trim();

            System.out.println("✅ Claude found: "
                    + type + "='" + selector + "'"
                    + " (" + confidence + "% confidence)");
            System.out.println("   Reason: " + reason);

            return new HealingResult(
                    true, original, selector, type,
                    confidence, reason);

        } catch (Exception e) {
            System.out.println("❌ Parse error: "
                    + e.getMessage());
            return HealingResult.failed(original);
        }
    }

    // ─────────────────────────────────────────
    // Result object
    // ─────────────────────────────────────────
    public record HealingResult(
            boolean success,
            String originalSelector,
            String newSelector,
            String selectorType,
            int confidence,
            String reason) {
        public static HealingResult failed(String original) {
            return new HealingResult(
                    false, original, null, null, 0,
                    "Healing failed");
        }
    }
}