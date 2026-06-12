package com.bank.util;

import java.io.IOException;
import java.io.InputStream;
import java.sql.Connection;
import java.sql.DriverManager;
import java.sql.SQLException;
import java.util.Properties;

public final class DBUtil {
    private static final Properties PROPERTIES = new Properties();

    static {
        try (InputStream inputStream = DBUtil.class.getClassLoader().getResourceAsStream("db.properties")) {
            if (inputStream != null) {
                PROPERTIES.load(inputStream);
            }
            Class.forName(getProperty("db.driver", "com.mysql.cj.jdbc.Driver"));
        } catch (IOException e) {
            throw new ExceptionInInitializerError(e);
        } catch (ClassNotFoundException e) {
            throw new ExceptionInInitializerError(e);
        }
    }

    private DBUtil() {
    }

    public static Connection getConnection() throws SQLException {
        String url = getProperty("db.url", "jdbc:mysql://localhost:3306/ibank");
        String username = getProperty("db.username", "root");
        String password = getProperty("db.password", "");
        return DriverManager.getConnection(url, username, password);
    }

    private static String getProperty(String key, String defaultValue) {
        String envKey = "IBANK_" + key.substring("db.".length()).toUpperCase().replace('.', '_');
        String envValue = System.getenv(envKey);
        if (envValue != null && envValue.trim().length() > 0) {
            return envValue.trim();
        }
        return PROPERTIES.getProperty(key, defaultValue);
    }
}
