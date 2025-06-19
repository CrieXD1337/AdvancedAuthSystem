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

public class MySqlDataProvider implements DataProvider {

    private Connection connection;
    private final MainAuth plugin;
    private final ConfigManager configManager;
    private final String url;
    private final String user;
    private final String pass;

    public MySqlDataProvider(MainAuth plugin, ConfigManager configManager) {
        this.plugin = plugin;
        this.configManager = configManager;
        this.url = "jdbc:mysql://" + configManager.getConfig().getString("mysql.host") + ":" +
                configManager.getConfig().getInt("mysql.port") + "/" +
                configManager.getConfig().getString("mysql.database") + "?useSSL=false&serverTimezone=UTC";
        this.user = configManager.getConfig().getString("mysql.user");
        this.pass = configManager.getConfig().getString("mysql.password");
        try {
            Class.forName("com.mysql.jdbc.Driver");
            this.connection = DriverManager.getConnection(this.url, this.user, this.pass);
            this.createTable();
            plugin.getLogger().info(TextFormat.GREEN + "[MySqlDataProvider] The connection to the database was established successfully.");
        } catch (Exception e) {
            plugin.getLogger().error("[MySqlDataProvider] Failed to connect to database: " + e.getMessage(), e);
            throw new RuntimeException("Database initialization error", e);
        }
    }

    private void createTable() {
        try (Statement st = this.getConnection().createStatement()) {
            st.execute("CREATE TABLE IF NOT EXISTS users (nickname VARCHAR(32) PRIMARY KEY, password VARCHAR(255) NOT NULL, ip VARCHAR(45), client_id VARCHAR(100), email VARCHAR(100))");
            plugin.getLogger().info("[MySqlDataProvider] The users table was created successfully or already exists.");
        } catch (SQLException e) {
            plugin.getLogger().error("[MySqlDataProvider] Error creating table: " + e.getMessage(), e);
        }
    }

    private void checkConnection() throws SQLException {
        if (this.connection == null || this.connection.isClosed()) {
            this.connection = DriverManager.getConnection(this.url, this.user, this.pass);
            plugin.getLogger().info("[MySqlDataProvider] The connection to the database has been restored.");
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
            plugin.getLogger().error("[MySqlDataProvider] Registration verification error for nickname " + nick + ": " + e.getMessage(), e);
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
            plugin.getLogger().error("[MySqlDataProvider] Password verification error for nickname " + nick + ": " + e.getMessage(), e);
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
            plugin.getLogger().info("[MySqlDataProvider] User " + nick + " successfully registered.");
        } catch (SQLException e) {
            plugin.getLogger().error("[MySqlDataProvider] User registration error " + nick + ": " + e.getMessage(), e);
        }
    }

    @Override
    public void updatePassword(String nick, String newHash) {
        try (Connection conn = this.getConnection();
             PreparedStatement ps = conn.prepareStatement("UPDATE users SET password = ? WHERE nickname = ?")) {
            ps.setString(1, newHash);
            ps.setString(2, nick);
            ps.executeUpdate();
            plugin.getLogger().info("[MySqlDataProvider] Password for user " + nick + " was successfully updated.");
        } catch (SQLException e) {
            plugin.getLogger().error("[MySqlDataProvider] Error updating password for nickname " + nick + ": " + e.getMessage(), e);
        }
    }

    // emal - soon

    @Override
    public String getClientId(String nick) {
        try (Connection conn = this.getConnection();
             PreparedStatement ps = conn.prepareStatement("SELECT client_id FROM users WHERE nickname = ?")) {
            ps.setString(1, nick);
            ResultSet rs = ps.executeQuery();
            return rs.next() ? rs.getString("client_id") : null;
        } catch (SQLException e) {
            plugin.getLogger().error("[MySqlDataProvider] Error client_id getter for name " + nick + ": " + e.getMessage(), e);
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
            plugin.getLogger().info("[MySqlDataProvider] Client_id for " + nick + " successfully updated");
        } catch (SQLException e) {
            plugin.getLogger().error("[MySqlDataProvider] Client_id update error for player " + nick + ": " + e.getMessage(), e);
        }
    }

    @Override
    public void close() {
        if (this.connection != null) {
            try {
                this.connection.close();
                plugin.getLogger().info("[MySqlDataProvider] Connection with database closed.");
            } catch (SQLException e) {
                plugin.getLogger().error("[MySqlDataProvider] Connection close error: " + e.getMessage(), e);
            }
        }
    }
}