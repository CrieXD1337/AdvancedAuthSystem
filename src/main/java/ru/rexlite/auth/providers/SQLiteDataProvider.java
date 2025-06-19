package ru.rexlite.auth.providers;

import cn.nukkit.utils.TextFormat;
import ru.rexlite.auth.MainAuth;
import ru.rexlite.auth.managers.ConfigManager;

import java.sql.Connection;
import java.sql.DriverManager;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.sql.Statement;

public class SQLiteDataProvider implements DataProvider {

    private Connection connection;
    private final MainAuth plugin;
    private final ConfigManager configManager;
    private final String url;

    public SQLiteDataProvider(MainAuth plugin, ConfigManager configManager) {
        this.plugin = plugin;
        this.configManager = configManager;
        this.url = "jdbc:sqlite:" + plugin.getDataFolder() + "/auth.db";
        try {
            Class.forName("org.sqlite.JDBC");
            this.connection = DriverManager.getConnection(this.url);
            this.createTable();
            plugin.getLogger().info(TextFormat.GREEN + "[SQLiteDataProvider] The connection to the SQLite database was established successfully.");
        } catch (Exception e) {
            plugin.getLogger().error("[SQLiteDataProvider] Failed to connect to database: " + e.getMessage(), e);
            throw new RuntimeException("Database initialization error", e);
        }
    }

    private void createTable() {
        try (Statement st = this.getConnection().createStatement()) {
            st.execute("CREATE TABLE IF NOT EXISTS users (nickname VARCHAR(32) PRIMARY KEY, password VARCHAR(255) NOT NULL, ip VARCHAR(45), client_id VARCHAR(100), email VARCHAR(100))");
            plugin.getLogger().info("[SQLiteDataProvider] The users table was created successfully or already exists.");
        } catch (SQLException e) {
            plugin.getLogger().error("[SQLiteDataProvider] Error creating table: " + e.getMessage(), e);
        }
    }

    private void checkConnection() throws SQLException {
        if (this.connection == null || this.connection.isClosed()) {
            this.connection = DriverManager.getConnection(this.url);
            plugin.getLogger().info("[SQLiteDataProvider] The connection to the SQLite database has been restored.");
        }
    }

    public Connection getConnection() throws SQLException {
        checkConnection();
        return this.connection;
    }

    @Override
    public boolean isRegistered(String nick) {
        try (Connection conn = this.getConnection();
             PreparedStatement ps = conn.prepareStatement("SELECT 1 FROM users WHERE nickname = ?")) {
            ps.setString(1, nick);
            ResultSet rs = ps.executeQuery();
            return rs.next();
        } catch (SQLException e) {
            plugin.getLogger().error("[SQLiteDataProvider] Registration verification error for nickname " + nick + ": " + e.getMessage(), e);
            return false;
        }
    }

    @Override
    public boolean isPasswordCorrect(String nick, String hash) {
        try (Connection conn = this.getConnection();
             PreparedStatement ps = conn.prepareStatement("SELECT password FROM users WHERE nickname = ?")) {
            ps.setString(1, nick);
            ResultSet rs = ps.executeQuery();
            return rs.next() && rs.getString("password").equals(hash);
        } catch (SQLException e) {
            plugin.getLogger().error("[SQLiteDataProvider] Password verification error for nickname " + nick + ": " + e.getMessage(), e);
            return false;
        }
    }

    @Override
    public void register(String nick, String hash, String ip, String clientId, String email) {
        try (Connection conn = this.getConnection();
             PreparedStatement ps = conn.prepareStatement("INSERT INTO users (nickname, password, ip, client_id, email) VALUES (?, ?, ?, ?, ?)")) {
            ps.setString(1, nick);
            ps.setString(2, hash);
            ps.setString(3, ip);
            ps.setString(4, clientId);
            ps.setString(5, email);
            ps.executeUpdate();
            plugin.getLogger().info("[SQLiteDataProvider] User " + nick + " successfully registered.");
        } catch (SQLException e) {
            plugin.getLogger().error("[SQLiteDataProvider] User registration error " + nick + ": " + e.getMessage(), e);
        }
    }

    @Override
    public void updatePassword(String nick, String newHash) {
        try (Connection conn = this.getConnection();
             PreparedStatement ps = conn.prepareStatement("UPDATE users SET password = ? WHERE nickname = ?")) {
            ps.setString(1, newHash);
            ps.setString(2, nick);
            ps.executeUpdate();
            plugin.getLogger().info("[SQLiteDataProvider] Password for user " + nick + " was successfully updated.");
        } catch (SQLException e) {
            plugin.getLogger().error("[SQLiteDataProvider] Error updating password for nickname " + nick + ": " + e.getMessage(), e);
        }
    }

    @Override
    public String getClientId(String nick) {
        try (Connection conn = this.getConnection();
             PreparedStatement ps = conn.prepareStatement("SELECT client_id FROM users WHERE nickname = ?")) {
            ps.setString(1, nick);
            ResultSet rs = ps.executeQuery();
            return rs.next() ? rs.getString("client_id") : null;
        } catch (SQLException e) {
            plugin.getLogger().error("[SQLiteDataProvider] Error client_id getter for name " + nick + ": " + e.getMessage(), e);
            return null;
        }
    }

    @Override
    public void updateClientId(String nick, String clientId) {
        try (Connection conn = this.getConnection();
             PreparedStatement ps = conn.prepareStatement("UPDATE users SET client_id = ? WHERE nickname = ?")) {
            ps.setString(1, clientId);
            ps.setString(2, nick);
            ps.executeUpdate();
            plugin.getLogger().info("[SQLiteDataProvider] Client_id for " + nick + " successfully updated");
        } catch (SQLException e) {
            plugin.getLogger().error("[SQLiteDataProvider] Client_id update error for player " + nick + ": " + e.getMessage(), e);
        }
    }

    @Override
    public void close() {
        if (this.connection != null) {
            try {
                this.connection.close();
                plugin.getLogger().info("[SQLiteDataProvider] Connection with database closed.");
            } catch (SQLException e) {
                plugin.getLogger().error("[SQLiteDataProvider] Connection close error: " + e.getMessage(), e);
            }
        }
    }
}
