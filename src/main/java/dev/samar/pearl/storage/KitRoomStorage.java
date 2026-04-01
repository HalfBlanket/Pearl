package dev.samar.pearl.storage;

import dev.samar.pearl.Pearl;
import org.bukkit.entity.Player;
import org.bukkit.inventory.ItemStack;
import org.bukkit.util.io.BukkitObjectInputStream;
import org.bukkit.util.io.BukkitObjectOutputStream;

import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.sql.*;
import java.util.Base64;
import java.util.logging.Level;

public class KitRoomStorage {

    private final Pearl plugin;
    private final DatabaseProvider databaseProvider;

    public KitRoomStorage(Pearl plugin, DatabaseProvider databaseProvider) {
        this.plugin = plugin;
        this.databaseProvider = databaseProvider;
    }

    public void initialize() {
        if (!databaseProvider.isInitialized()) {
            plugin.getLogger().severe("Cannot initialize kit room tables - database provider not initialized!");
            return;
        }

        try (Connection connection = databaseProvider.getConnection();
             Statement statement = connection.createStatement()) {

            if (databaseProvider.isMySQL()) {
                statement.executeUpdate(
                        "CREATE TABLE IF NOT EXISTS kit_room_items (" +
                                "category VARCHAR(64) NOT NULL, " +
                                "slot INT NOT NULL, " +
                                "item MEDIUMTEXT NOT NULL, " +
                                "PRIMARY KEY (category, slot)" +
                                ") ENGINE=InnoDB DEFAULT CHARSET=utf8mb4"
                );

                statement.executeUpdate(
                        "CREATE TABLE IF NOT EXISTS ender_chest (" +
                                "uuid VARCHAR(36) NOT NULL, " +
                                "slot INT NOT NULL, " +
                                "item MEDIUMTEXT NOT NULL, " +
                                "PRIMARY KEY (uuid, slot)" +
                                ") ENGINE=InnoDB DEFAULT CHARSET=utf8mb4"
                );
            } else {
                statement.executeUpdate(
                        "CREATE TABLE IF NOT EXISTS kit_room_items (" +
                                "category TEXT NOT NULL, " +
                                "slot INTEGER NOT NULL, " +
                                "item TEXT NOT NULL, " +
                                "PRIMARY KEY (category, slot)" +
                                ")"
                );

                statement.executeUpdate(
                        "CREATE TABLE IF NOT EXISTS ender_chest (" +
                                "uuid TEXT NOT NULL, " +
                                "slot INTEGER NOT NULL, " +
                                "item TEXT NOT NULL, " +
                                "PRIMARY KEY (uuid, slot)" +
                                ")"
                );
            }

            plugin.getLogger().info("Kit room tables initialized successfully.");
        } catch (SQLException e) {
            plugin.getLogger().log(Level.SEVERE, "Failed to initialize kit room tables!", e);
        }
    }

    // ============ Kit Room Items ============

    public void saveItems(String category, ItemStack[] items) {
        if (!databaseProvider.isInitialized()) return;

        try (Connection connection = databaseProvider.getConnection()) {
            connection.setAutoCommit(false);

            try (PreparedStatement delete = connection.prepareStatement(
                    "DELETE FROM kit_room_items WHERE category = ?")) {
                delete.setString(1, category);
                delete.executeUpdate();
            }

            try (PreparedStatement insert = connection.prepareStatement(
                    "INSERT INTO kit_room_items (category, slot, item) VALUES (?, ?, ?)")) {

                for (int i = 0; i < 45; i++) {
                    if (items[i] != null) {
                        String encoded = encodeItem(items[i]);
                        if (encoded != null) {
                            insert.setString(1, category);
                            insert.setInt(2, i);
                            insert.setString(3, encoded);
                            insert.addBatch();
                        }
                    }
                }

                insert.executeBatch();
            }

            connection.commit();
            plugin.getLogger().info("Saved kit room category: " + category);
        } catch (SQLException e) {
            plugin.getLogger().log(Level.SEVERE, "Failed to save kit room items for category: " + category, e);
        }
    }

    public ItemStack[] loadItems(String category) {
        ItemStack[] items = new ItemStack[45];

        if (!databaseProvider.isInitialized()) return items;

        try (Connection connection = databaseProvider.getConnection();
             PreparedStatement statement = connection.prepareStatement(
                     "SELECT slot, item FROM kit_room_items WHERE category = ?")) {

            statement.setString(1, category);
            ResultSet rs = statement.executeQuery();

            while (rs.next()) {
                int slot = rs.getInt("slot");
                String encoded = rs.getString("item");

                if (slot >= 0 && slot < 45) {
                    ItemStack item = decodeItem(encoded);
                    if (item != null) {
                        items[slot] = item;
                    }
                }
            }

        } catch (SQLException e) {
            plugin.getLogger().log(Level.SEVERE, "Failed to load kit room items for category: " + category, e);
        }

        return items;
    }

    // ============ Ender Chest ============

    public void saveEnderChest(Player player, ItemStack[] items) {
        if (!databaseProvider.isInitialized()) return;

        String uuid = player.getUniqueId().toString();

        try (Connection connection = databaseProvider.getConnection()) {
            connection.setAutoCommit(false);

            try (PreparedStatement delete = connection.prepareStatement(
                    "DELETE FROM ender_chest WHERE uuid = ?")) {
                delete.setString(1, uuid);
                delete.executeUpdate();
            }

            try (PreparedStatement insert = connection.prepareStatement(
                    "INSERT INTO ender_chest (uuid, slot, item) VALUES (?, ?, ?)")) {

                for (int i = 0; i < 27; i++) {
                    if (items[i] != null) {
                        String encoded = encodeItem(items[i]);
                        if (encoded != null) {
                            insert.setString(1, uuid);
                            insert.setInt(2, i);
                            insert.setString(3, encoded);
                            insert.addBatch();
                        }
                    }
                }

                insert.executeBatch();
            }

            connection.commit();
        } catch (SQLException e) {
            plugin.getLogger().log(Level.SEVERE, "Failed to save ender chest for player: " + player.getName(), e);
        }
    }

    public ItemStack[] loadEnderChest(Player player) {
        ItemStack[] items = new ItemStack[27];

        if (!databaseProvider.isInitialized()) return items;

        String uuid = player.getUniqueId().toString();

        try (Connection connection = databaseProvider.getConnection();
             PreparedStatement statement = connection.prepareStatement(
                     "SELECT slot, item FROM ender_chest WHERE uuid = ?")) {

            statement.setString(1, uuid);
            ResultSet rs = statement.executeQuery();

            while (rs.next()) {
                int slot = rs.getInt("slot");
                String encoded = rs.getString("item");

                if (slot >= 0 && slot < 27) {
                    ItemStack item = decodeItem(encoded);
                    if (item != null) {
                        items[slot] = item;
                    }
                }
            }

        } catch (SQLException e) {
            plugin.getLogger().log(Level.SEVERE, "Failed to load ender chest for player: " + player.getName(), e);
        }

        return items;
    }

    public void resetEnderChest(Player player) {
        if (!databaseProvider.isInitialized()) return;

        String uuid = player.getUniqueId().toString();

        try (Connection connection = databaseProvider.getConnection();
             PreparedStatement delete = connection.prepareStatement(
                     "DELETE FROM ender_chest WHERE uuid = ?")) {
            delete.setString(1, uuid);
            delete.executeUpdate();
        } catch (SQLException e) {
            plugin.getLogger().log(Level.SEVERE, "Failed to reset ender chest for player: " + player.getName(), e);
        }
    }

    // ============ Encoding ============

    private String encodeItem(ItemStack item) {
        try (ByteArrayOutputStream outputStream = new ByteArrayOutputStream();
             BukkitObjectOutputStream dataOutput = new BukkitObjectOutputStream(outputStream)) {

            dataOutput.writeObject(item);
            return Base64.getEncoder().encodeToString(outputStream.toByteArray());

        } catch (IOException e) {
            plugin.getLogger().log(Level.WARNING, "Failed to encode item!", e);
            return null;
        }
    }

    private ItemStack decodeItem(String encoded) {
        try (ByteArrayInputStream inputStream = new ByteArrayInputStream(Base64.getDecoder().decode(encoded));
             BukkitObjectInputStream dataInput = new BukkitObjectInputStream(inputStream)) {

            return (ItemStack) dataInput.readObject();

        } catch (IOException | ClassNotFoundException e) {
            plugin.getLogger().log(Level.WARNING, "Failed to decode item!", e);
            return null;
        }
    }
}