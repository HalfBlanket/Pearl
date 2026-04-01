package dev.samar.pearl.storage;

import dev.samar.pearl.Pearl;
import com.zaxxer.hikari.HikariConfig;
import com.zaxxer.hikari.HikariDataSource;

import java.io.File;
import java.sql.Connection;
import java.sql.SQLException;
import java.util.logging.Level;

public class DatabaseProvider {

    private final Pearl plugin;
    private final String storageType;
    private HikariDataSource dataSource;
    private boolean initialized = false;

    public DatabaseProvider(Pearl plugin) {
        this.plugin = plugin;
        this.storageType = plugin.getConfig().getString("storage", "sqlite").trim().toLowerCase();
    }

    public void initialize() {
        try {
            HikariConfig config = new HikariConfig();
            config.setPoolName("Pearl-DB-Pool");

            if (storageType.equals("mysql")) {
                String host = plugin.getConfig().getString("mysql.host", "localhost");
                int port = plugin.getConfig().getInt("mysql.port", 3306);
                String database = plugin.getConfig().getString("mysql.database", "pearl");
                String username = plugin.getConfig().getString("mysql.username", "root");
                String password = plugin.getConfig().getString("mysql.password", "");
                int poolSize = plugin.getConfig().getInt("mysql.pool-size", 10);
                boolean useSSL = plugin.getConfig().getBoolean("mysql.use-ssl", false);

                // Build JDBC URL with connection properties in the URL itself
                String jdbcUrl = "jdbc:mysql://" + host + ":" + port + "/" + database
                        + "?useSSL=" + useSSL
                        + "&allowPublicKeyRetrieval=true"
                        + "&characterEncoding=UTF-8"
                        + "&useUnicode=true"
                        + "&autoReconnect=true";

                config.setDriverClassName("com.mysql.cj.jdbc.Driver");
                config.setJdbcUrl(jdbcUrl);
                config.setUsername(username);
                config.setPassword(password);
                config.setMaximumPoolSize(poolSize);
                config.setMinimumIdle(2);
                config.setConnectionTimeout(30000);
                config.setIdleTimeout(600000);
                config.setMaxLifetime(1800000);
                config.setLeakDetectionThreshold(60000);

                // Performance properties only (no encoding here)
                config.addDataSourceProperty("cachePrepStmts", "true");
                config.addDataSourceProperty("prepStmtCacheSize", "250");
                config.addDataSourceProperty("prepStmtCacheSqlLimit", "2048");
                config.addDataSourceProperty("useServerPrepStmts", "true");

                plugin.getLogger().info("Connecting to MySQL: " + host + ":" + port + "/" + database);

            } else {
                // SQLite
                File dbFile = new File(plugin.getDataFolder(), "pearl.db");
                plugin.getDataFolder().mkdirs();

                config.setDriverClassName("org.sqlite.JDBC");
                config.setJdbcUrl("jdbc:sqlite:" + dbFile.getAbsolutePath());
                config.setMaximumPoolSize(1);
                config.setMinimumIdle(1);
                config.setConnectionTimeout(30000);
                config.setIdleTimeout(0);
                config.setMaxLifetime(0);
                config.setConnectionTestQuery("SELECT 1");

                plugin.getLogger().info("Using SQLite storage: " + dbFile.getAbsolutePath());
            }

            dataSource = new HikariDataSource(config);

            // Verify connection works
            try (Connection conn = dataSource.getConnection()) {
                if (conn == null || conn.isClosed()) {
                    throw new SQLException("Test connection failed - connection is null or closed!");
                }
                plugin.getLogger().info("Database connection pool established successfully. Storage: " + storageType.toUpperCase());
                initialized = true;
            }

        } catch (Exception e) {
            plugin.getLogger().log(Level.SEVERE, "====================================");
            plugin.getLogger().log(Level.SEVERE, "FAILED TO INITIALIZE DATABASE!");
            plugin.getLogger().log(Level.SEVERE, "Storage type: " + storageType);
            plugin.getLogger().log(Level.SEVERE, "Error: " + e.getMessage());
            plugin.getLogger().log(Level.SEVERE, "====================================");
            plugin.getLogger().log(Level.SEVERE, "Full stack trace:", e);

            if (dataSource != null) {
                try {
                    dataSource.close();
                } catch (Exception ignored) {}
                dataSource = null;
            }
            initialized = false;
        }
    }

    public Connection getConnection() throws SQLException {
        if (!initialized || dataSource == null || dataSource.isClosed()) {
            throw new SQLException("DataSource is not initialized or has been closed! " +
                    "Check server startup logs for database connection errors. Storage type: " + storageType);
        }
        return dataSource.getConnection();
    }

    public boolean isMySQL() {
        return storageType.equals("mysql");
    }

    public boolean isInitialized() {
        return initialized;
    }

    public String getStorageType() {
        return storageType;
    }

    public void close() {
        if (dataSource != null && !dataSource.isClosed()) {
            dataSource.close();
            plugin.getLogger().info("Database connection pool closed.");
        }
        initialized = false;
    }
}