package com.ares.selenium.core;

import com.fasterxml.jackson.databind.ObjectMapper;
import java.io.File;
import java.util.HashMap;
import java.util.Map;

public class SelectorCache {

    private final String cachePath;
    private Map<String, String> cache;
    private final ObjectMapper objectMapper = new ObjectMapper();

    public SelectorCache(String cachePath) {
        this.cachePath = cachePath;
        this.cache = loadCache();
    }

    // ─────────────────────────────────────
    // Get healed selector from cache
    // ─────────────────────────────────────
    public String get(String originalSelector) {
        return cache.get(originalSelector);
    }

    // ─────────────────────────────────────
    // Save healed selector to cache
    // ─────────────────────────────────────
    public void put(String originalSelector, String healedSelector) {
        cache.put(originalSelector, healedSelector);
        saveCache();
        System.out.println("💾 Cached: " + originalSelector
                + " → " + healedSelector);
    }

    public boolean contains(String selector) {
        return cache.containsKey(selector);
    }

    public void clear() {
        cache.clear();
        saveCache();
    }

    // ─────────────────────────────────────
    // Load from JSON file
    // ─────────────────────────────────────
    @SuppressWarnings("unchecked")
    private Map<String, String> loadCache() {
        try {
            File file = new File(cachePath);
            if (file.exists()) {
                System.out.println("📂 Loading selector cache from: "
                        + cachePath);
                return objectMapper.readValue(file, Map.class);
            }
        } catch (Exception e) {
            System.out.println("⚠️ Could not load cache: "
                    + e.getMessage());
        }
        return new HashMap<>();
    }

    // ─────────────────────────────────────
    // Save to JSON file
    // ─────────────────────────────────────
    private void saveCache() {
        try {
            objectMapper.writerWithDefaultPrettyPrinter()
                    .writeValue(new File(cachePath), cache);
        } catch (Exception e) {
            System.out.println("⚠️ Could not save cache: "
                    + e.getMessage());
        }
    }

    public Map<String, String> getAll() {
        return new HashMap<>(cache);
    }
}