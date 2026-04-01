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

public class KitSlotStorage {

    private final Pearl plugin;
    private final DatabaseProvider databaseProvider;

    public KitSlotStorage(Pearl plugin, DatabaseProvider databaseProvider) {
        this.plugin = plugin;
        this.databaseProvider = databaseProvider;
    }

    public void initialize() {
        if (!databaseProvider.isInitialized()) {
            plugin.getLogger().severe("Cannot initialize kit slots table - database provider not initialized!");
            return;
        }

        try (Connection connection = databaseProvider.getConnection();
             Statement statement = connection.createStatement()) {

            if (databaseProvider.isMySQL()) {
                statement.executeUpdate(
                        "CREATE TABLE IF NOT EXISTS kit_slots (" +
                                "uuid VARCHAR(36) NOT NULL, " +
                                "kit_number INT NOT NULL, " +
                                "slot INT NOT NULL, " +
                                "item MEDIUMTEXT NOT NULL, " +
                                "PRIMARY KEY (uuid, kit_number, slot)" +
                                ") ENGINE=InnoDB DEFAULT CHARSET=utf8mb4"
                );
            } else {
                statement.executeUpdate(
                        "CREATE TABLE IF NOT EXISTS kit_slots (" +
                                "uuid TEXT NOT NULL, " +
                                "kit_number INTEGER NOT NULL, " +
                                "slot INTEGER NOT NULL, " +
                                "item TEXT NOT NULL, " +
                                "PRIMARY KEY (uuid, kit_number, slot)" +
                                ")"
                );
            }

            plugin.getLogger().info("Kit slots table initialized successfully.");
        } catch (SQLException e) {
            plugin.getLogger().log(Level.SEVERE, "Failed to initialize kit slots table!", e);
        }
    }

    public void saveKit(Player player, int kitNumber, ItemStack[] items) {
        if (!databaseProvider.isInitialized()) return;

        String uuid = player.getUniqueId().toString();

        try (Connection connection = databaseProvider.getConnection()) {
            connection.setAutoCommit(false);

            try (PreparedStatement delete = connection.prepareStatement(
                    "DELETE FROM kit_slots WHERE uuid = ? AND kit_number = ?")) {
                delete.setString(1, uuid);
                delete.setInt(2, kitNumber);
                delete.executeUpdate();
            }

            try (PreparedStatement insert = connection.prepareStatement(
                    "INSERT INTO kit_slots (uuid, kit_number, slot, item) VALUES (?, ?, ?, ?)")) {

                for (int i = 0; i < items.length; i++) {
                    if (items[i] != null) {
                        String encoded = encodeItem(items[i]);
                        if (encoded != null) {
                            insert.setString(1, uuid);
                            insert.setInt(2, kitNumber);
                            insert.setInt(3, i);
                            insert.setString(4, encoded);
                            insert.addBatch();
                        }
                    }
                }

                insert.executeBatch();
            }

            connection.commit();
        } catch (SQLException e) {
            plugin.getLogger().log(Level.SEVERE, "Failed to save kit " + kitNumber + " for player: " + player.getName(), e);
        }
    }

    public ItemStack[] loadKit(Player player, int kitNumber) {
        ItemStack[] items = new ItemStack[45];

        if (!databaseProvider.isInitialized()) return items;

        String uuid = player.getUniqueId().toString();

        try (Connection connection = databaseProvider.getConnection();
             PreparedStatement statement = connection.prepareStatement(
                     "SELECT slot, item FROM kit_slots WHERE uuid = ? AND kit_number = ?")) {

            statement.setString(1, uuid);
            statement.setInt(2, kitNumber);
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
            plugin.getLogger().log(Level.SEVERE, "Failed to load kit " + kitNumber + " for player: " + player.getName(), e);
        }

        return items;
    }

    public void resetKit(Player player, int kitNumber) {
        if (!databaseProvider.isInitialized()) return;

        String uuid = player.getUniqueId().toString();

        try (Connection connection = databaseProvider.getConnection();
             PreparedStatement delete = connection.prepareStatement(
                     "DELETE FROM kit_slots WHERE uuid = ? AND kit_number = ?")) {
            delete.setString(1, uuid);
            delete.setInt(2, kitNumber);
            delete.executeUpdate();
        } catch (SQLException e) {
            plugin.getLogger().log(Level.SEVERE, "Failed to reset kit " + kitNumber + " for player: " + player.getName(), e);
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