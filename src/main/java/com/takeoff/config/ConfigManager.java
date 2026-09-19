package com.takeoff.config;

import java.io.IOException;
import java.io.InputStream;
import java.util.Properties;

/**
 * Loads src/test/resources/config.properties once and exposes typed getters.
 * Every key can be overridden by an environment variable of the same name in
 * UPPER_SNAKE_CASE, so CI/CD or a secret manager can inject real values
 * without touching the committed properties file.
 */
public final class ConfigManager {

    private static final Properties PROPERTIES = load();

    private ConfigManager() {
    }

    private static Properties load() {
        Properties properties = new Properties();
        try (InputStream in = ConfigManager.class.getClassLoader().getResourceAsStream("config.properties")) {
            if (in == null) {
                throw new IllegalStateException("config.properties not found on classpath");
            }
            properties.load(in);
        } catch (IOException e) {
            throw new IllegalStateException("Failed to load config.properties", e);
        }
        return properties;
    }

    private static String get(String key) {
        return get(key, key.toUpperCase().replace('.', '_'));
    }

    /**
     * @param envKey explicit environment variable name to check first. Used for
     *               values (credentials especially) where the auto-derived name
     *               (e.g. "PASSWORD") would be too generic/collision-prone as a
     *               real environment variable.
     */
    private static String get(String key, String envKey) {
        String envValue = System.getenv(envKey);
        if (envValue != null && !envValue.isBlank()) {
            return envValue;
        }
        String systemPropertyValue = System.getProperty(key);
        if (systemPropertyValue != null && !systemPropertyValue.isBlank()) {
            return systemPropertyValue;
        }
        return PROPERTIES.getProperty(key);
    }

    public static String b2bUrl() {
        return get("b2b.url", "B2B_URL");
    }

    public static String adminUrl() {
        return get("admin.url", "ADMIN_URL");
    }

    public static String email() {
        return get("email", "TEST_EMAIL");
    }

    public static String password() {
        return get("password", "TEST_PASSWORD");
    }

    public static String adminEmail() {
        return get("admin.email", "ADMIN_EMAIL");
    }

    public static String adminPassword() {
        return get("admin.password", "ADMIN_PASSWORD");
    }

    public static String adminPin() {
        return get("admin.pin", "ADMIN_PIN");
    }

    public static String browser() {
        return get("browser");
    }

    public static boolean headless() {
        return Boolean.parseBoolean(get("headless"));
    }

    public static double slowMoMs() {
        return Double.parseDouble(get("slowmo.ms"));
    }

    public static double defaultTimeoutMs() {
        return Double.parseDouble(get("default.timeout.ms"));
    }

    public static int viewportWidth() {
        return Integer.parseInt(get("viewport.width"));
    }

    public static int viewportHeight() {
        return Integer.parseInt(get("viewport.height"));
    }
}
