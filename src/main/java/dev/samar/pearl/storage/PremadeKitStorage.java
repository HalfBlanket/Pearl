package dev.samar.pearl.storage;

import dev.samar.pearl.Pearl;
import org.bukkit.inventory.ItemStack;
import org.bukkit.util.io.BukkitObjectInputStream;
import org.bukkit.util.io.BukkitObjectOutputStream;

import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.sql.*;
import java.util.Base64;
import java.util.logging.Level;

public class PremadeKitStorage {

    private final Pearl plugin;
    private final DatabaseProvider databaseProvider;

    public PremadeKitStorage(Pearl plugin, DatabaseProvider databaseProvider) {
        this.plugin = plugin;
        this.databaseProvider = databaseProvider;
    }

    public void initialize() {
        if (!databaseProvider.isInitialized()) {
            plugin.getLogger().severe("Cannot initialize premade kits table - database provider not initialized!");
            return;
        }

        try (Connection connection = databaseProvider.getConnection();
             Statement statement = connection.createStatement()) {

            if (databaseProvider.isMySQL()) {
                statement.executeUpdate(
                        "CREATE TABLE IF NOT EXISTS premade_kits (" +
                                "kit_name VARCHAR(64) NOT NULL, " +
                                "slot INT NOT NULL, " +
                                "item MEDIUMTEXT NOT NULL, " +
                                "PRIMARY KEY (kit_name, slot)" +
                                ") ENGINE=InnoDB DEFAULT CHARSET=utf8mb4"
                );
            } else {
                statement.executeUpdate(
                        "CREATE TABLE IF NOT EXISTS premade_kits (" +
                                "kit_name TEXT NOT NULL, " +
                                "slot INTEGER NOT NULL, " +
                                "item TEXT NOT NULL, " +
                                "PRIMARY KEY (kit_name, slot)" +
                                ")"
                );
            }

            plugin.getLogger().info("Premade kits table initialized successfully.");
        } catch (SQLException e) {
            plugin.getLogger().log(Level.SEVERE, "Failed to initialize premade kits table!", e);
        }
    }

    public void saveKit(String kitName, ItemStack[] items) {
        if (!databaseProvider.isInitialized()) return;

        try (Connection connection = databaseProvider.getConnection()) {
            connection.setAutoCommit(false);

            try (PreparedStatement delete = connection.prepareStatement(
                    "DELETE FROM premade_kits WHERE kit_name = ?")) {
                delete.setString(1, kitName);
                delete.executeUpdate();
            }

            try (PreparedStatement insert = connection.prepareStatement(
                    "INSERT INTO premade_kits (kit_name, slot, item) VALUES (?, ?, ?)")) {

                for (int i = 0; i < items.length; i++) {
                    if (items[i] != null) {
                        String encoded = encodeItem(items[i]);
                        if (encoded != null) {
                            insert.setString(1, kitName);
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
            plugin.getLogger().log(Level.SEVERE, "Failed to save premade kit: " + kitName, e);
        }
    }

    public ItemStack[] loadKit(String kitName) {
        ItemStack[] items = new ItemStack[45];

        if (!databaseProvider.isInitialized()) return items;

        try (Connection connection = databaseProvider.getConnection();
             PreparedStatement statement = connection.prepareStatement(
                     "SELECT slot, item FROM premade_kits WHERE kit_name = ?")) {

            statement.setString(1, kitName);
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
            plugin.getLogger().log(Level.SEVERE, "Failed to load premade kit: " + kitName, e);
        }

        return items;
    }

    public void resetKit(String kitName) {
        if (!databaseProvider.isInitialized()) return;

        try (Connection connection = databaseProvider.getConnection();
             PreparedStatement delete = connection.prepareStatement(
                     "DELETE FROM premade_kits WHERE kit_name = ?")) {
            delete.setString(1, kitName);
            delete.executeUpdate();
        } catch (SQLException e) {
            plugin.getLogger().log(Level.SEVERE, "Failed to reset premade kit: " + kitName, e);
        }
    }

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