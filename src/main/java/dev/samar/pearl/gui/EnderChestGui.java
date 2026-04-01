package dev.samar.pearl.gui;

import dev.samar.pearl.Pearl;
import net.kyori.adventure.text.Component;
import net.kyori.adventure.text.format.TextColor;
import net.kyori.adventure.text.format.TextDecoration;
import net.kyori.adventure.text.format.NamedTextColor;
import org.bukkit.Bukkit;
import org.bukkit.Material;
import org.bukkit.entity.Player;
import org.bukkit.inventory.Inventory;
import org.bukkit.inventory.ItemStack;
import org.bukkit.inventory.meta.ItemMeta;

public class EnderChestGui {

    private static final TextColor KIT_COLOR = TextColor.fromHexString("#3799DD");
    private static final TextColor SAVE_COLOR = TextColor.fromHexString("#18FA00");
    private static final TextColor RESET_COLOR = TextColor.fromHexString("#FA1B00");

    private final Pearl plugin;
    private final Component guiTitle;

    public EnderChestGui(Pearl plugin) {
        this.plugin = plugin;

        this.guiTitle = Component.text("Ender Chest")
                .color(NamedTextColor.LIGHT_PURPLE)
                .decoration(TextDecoration.BOLD, true);
    }

    public void open(Player player) {
        Inventory inventory = Bukkit.createInventory(null, 36, guiTitle);

        // Load saved ender chest from database
        ItemStack[] saved = plugin.getKitRoomStorage().loadEnderChest(player);
        for (int i = 0; i < 27; i++) {
            if (saved[i] != null) {
                inventory.setItem(i, saved[i]);
            }
        }

        setBottomRow(inventory);

        player.openInventory(inventory);
    }

    public void saveFromInventory(Player player, Inventory inventory) {
        ItemStack[] items = new ItemStack[27];
        for (int i = 0; i < 27; i++) {
            items[i] = inventory.getItem(i);
        }

        // Save to database
        plugin.getKitRoomStorage().saveEnderChest(player, items);

        // Sync to vanilla ender chest
        syncToVanillaEnderChest(player, items);
    }

    public void importFromEnderChest(Player player, Inventory guiInventory) {
        Inventory enderChest = player.getEnderChest();

        for (int i = 0; i < 27; i++) {
            guiInventory.setItem(i, enderChest.getItem(i));
        }
    }

    public void resetInventory(Inventory inventory) {
        for (int i = 0; i < 27; i++) {
            inventory.setItem(i, null);
        }
    }

    public void resetAndSync(Player player, Inventory guiInventory) {
        // Clear GUI
        resetInventory(guiInventory);

        // Clear database
        plugin.getKitRoomStorage().resetEnderChest(player);

        // Clear vanilla ender chest
        player.getEnderChest().clear();
    }

    private void syncToVanillaEnderChest(Player player, ItemStack[] items) {
        Inventory enderChest = player.getEnderChest();
        enderChest.clear();

        for (int i = 0; i < 27; i++) {
            if (items[i] != null) {
                enderChest.setItem(i, items[i].clone());
            }
        }
    }

    private void setBottomRow(Inventory inventory) {
        inventory.setItem(27, createGlassPane());
        inventory.setItem(28, createGlassPane());
        inventory.setItem(29, createItem(Material.ENDER_CHEST, 1, "Import From Enderchest", KIT_COLOR));
        inventory.setItem(30, createGlassPane());
        inventory.setItem(31, createItem(Material.LIME_DYE, 1, "Save", SAVE_COLOR));
        inventory.setItem(32, createGlassPane());
        inventory.setItem(33, createItem(Material.ANVIL, 1, "Reset", RESET_COLOR));
        inventory.setItem(34, createGlassPane());
        inventory.setItem(35, createGlassPane());
    }

    private ItemStack createItem(Material material, int amount, String name, TextColor color) {
        ItemStack item = new ItemStack(material, amount);
        ItemMeta meta = item.getItemMeta();

        meta.displayName(Component.text(name)
                .color(color)
                .decoration(TextDecoration.ITALIC, false));

        item.setItemMeta(meta);
        return item;
    }

    private ItemStack createGlassPane() {
        ItemStack item = new ItemStack(Material.GRAY_STAINED_GLASS_PANE, 1);
        ItemMeta meta = item.getItemMeta();

        meta.displayName(Component.empty());

        item.setItemMeta(meta);
        return item;
    }

    public Component getGuiTitle() {
        return guiTitle;
    }
}