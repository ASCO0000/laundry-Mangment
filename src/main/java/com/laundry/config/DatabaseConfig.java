package com.laundry.config;

import java.io.InputStream;
import java.util.Properties;
import java.util.logging.Level;
import java.util.logging.Logger;

/**
 * Manages database connection properties with environment overrides
 * and configurable defaults.
 */
public class DatabaseConfig {

    private static final Logger LOGGER = Logger.getLogger(DatabaseConfig.class.getName());
    private static final Properties properties = new Properties();
    private static boolean isH2Active = false;

    static {
        loadProperties();
    }

    private static void loadProperties() {
        try (InputStream in = DatabaseConfig.class.getResourceAsStream("/db.properties")) {
            if (in != null) {
                properties.load(in);
            } else {
                LOGGER.info("No db.properties found on classpath, using built-in defaults.");
                setDefaultProperties();
            }
        } catch (Exception e) {
            LOGGER.log(Level.WARNING, "Failed to load db.properties, falling back to defaults", e);
            setDefaultProperties();
        }
    }

    private static void setDefaultProperties() {
        properties.setProperty("db.driver", "com.mysql.cj.jdbc.Driver");
        properties.setProperty("db.url", "jdbc:mysql://localhost:3306/laundry_management?createDatabaseIfNotExist=true&useSSL=false&allowPublicKeyRetrieval=true&serverTimezone=UTC");
        properties.setProperty("db.username", "root");
        properties.setProperty("db.password", "root");
        properties.setProperty("db.auto.init", "true");
        properties.setProperty("db.fallback.h2", "true");
        properties.setProperty("db.h2.url", "jdbc:h2:./data/laundry_db;MODE=MySQL;DATABASE_TO_LOWER=TRUE;CASE_INSENSITIVE_IDENTIFIERS=TRUE");
        properties.setProperty("db.h2.driver", "org.h2.Driver");
        properties.setProperty("db.h2.username", "sa");
        properties.setProperty("db.h2.password", "");
    }

    public static String getUrl() {
        if (isH2Active) {
            return properties.getProperty("db.h2.url", "jdbc:h2:mem:laundry_db;MODE=MySQL;DATABASE_TO_LOWER=TRUE");
        }
        String envUrl = System.getenv("LAUNDRY_DB_URL");
        if (envUrl != null && !envUrl.isEmpty()) return envUrl;
        return properties.getProperty("db.url");
    }

    public static String getUsername() {
        if (isH2Active) {
            return properties.getProperty("db.h2.username", "sa");
        }
        String envUser = System.getenv("LAUNDRY_DB_USER");
        if (envUser != null) return envUser;
        return properties.getProperty("db.username", "root");
    }

    public static String getPassword() {
        if (isH2Active) {
            return properties.getProperty("db.h2.password", "");
        }
        String envPass = System.getenv("LAUNDRY_DB_PASSWORD");
        if (envPass != null) return envPass;
        return properties.getProperty("db.password", "");
    }

    public static String getDriver() {
        if (isH2Active) {
            return properties.getProperty("db.h2.driver", "org.h2.Driver");
        }
        return properties.getProperty("db.driver", "com.mysql.cj.jdbc.Driver");
    }

    public static boolean isAutoInit() {
        return Boolean.parseBoolean(properties.getProperty("db.auto.init", "true"));
    }

    public static boolean isFallbackH2Allowed() {
        return Boolean.parseBoolean(properties.getProperty("db.fallback.h2", "true"));
    }

    public static void activateH2Fallback() {
        isH2Active = true;
        LOGGER.info("Switched active database target to embedded H2 (MySQL Mode).");
    }

    public static boolean isH2Active() {
        return isH2Active;
    }
}
