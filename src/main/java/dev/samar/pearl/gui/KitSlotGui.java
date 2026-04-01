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

import java.util.HashMap;
import java.util.Map;
import java.util.UUID;

public class KitSlotGui {

    private static final TextColor KIT_COLOR = TextColor.fromHexString("#3799DD");
    private static final TextColor SAVE_COLOR = TextColor.fromHexString("#18FA00");
    private static final TextColor RESET_COLOR = TextColor.fromHexString("#FA1B00");

    // Hardcoded GUI slots
    private static final int HELMET_SLOT = 0;
    private static final int CHESTPLATE_SLOT = 1;
    private static final int LEGGINGS_SLOT = 2;
    private static final int BOOTS_SLOT = 3;
    private static final int SEPARATOR_1 = 4;
    private static final int OFFHAND_SLOT = 5;
    private static final int SEPARATOR_2 = 6;
    private static final int SEPARATOR_3 = 7;
    private static final int SEPARATOR_4 = 8;

    // Bottom row
    private static final int IMPORT_SLOT = 47;
    private static final int SAVE_SLOT = 49;
    private static final int RESET_SLOT = 51;

    private final Pearl plugin;
    private final Map<UUID, Integer> playerKitNumber = new HashMap<>();

    public KitSlotGui(Pearl plugin) {
        this.plugin = plugin;
    }

    public Component getGuiTitle(int kitNumber) {
        return Component.text("Kit " + kitNumber)
                .color(NamedTextColor.LIGHT_PURPLE)
                .decoration(TextDecoration.BOLD, true);
    }

    public boolean isKitSlotGui(Component title) {
        for (int i = 1; i <= 7; i++) {
            if (title.equals(getGuiTitle(i))) {
                return true;
            }
        }
        return false;
    }

    public void open(Player player, int kitNumber) {
        playerKitNumber.put(player.getUniqueId(), kitNumber);

        Inventory inventory = Bukkit.createInventory(null, 54, getGuiTitle(kitNumber));

        // Load saved kit
        ItemStack[] saved = plugin.getKitSlotStorage().loadKit(player, kitNumber);

        // Armor slots (0-3)
        inventory.setItem(HELMET_SLOT, saved[HELMET_SLOT]);
        inventory.setItem(CHESTPLATE_SLOT, saved[CHESTPLATE_SLOT]);
        inventory.setItem(LEGGINGS_SLOT, saved[LEGGINGS_SLOT]);
        inventory.setItem(BOOTS_SLOT, saved[BOOTS_SLOT]);

        // Separator
        inventory.setItem(SEPARATOR_1, createGlassPane());

        // Offhand
        inventory.setItem(OFFHAND_SLOT, saved[OFFHAND_SLOT]);

        // Separators
        inventory.setItem(SEPARATOR_2, createGlassPane());
        inventory.setItem(SEPARATOR_3, createGlassPane());
        inventory.setItem(SEPARATOR_4, createGlassPane());

        // Inventory slots (9-44) mapped from saved data
        for (int i = 9; i < 45; i++) {
            if (saved[i] != null) {
                inventory.setItem(i, saved[i]);
            }
        }

        // Bottom row
        setBottomRow(inventory);

        player.openInventory(inventory);
    }

    public void importFromPlayer(Player player, Inventory guiInventory) {
        // Armor
        guiInventory.setItem(HELMET_SLOT, player.getInventory().getHelmet());
        guiInventory.setItem(CHESTPLATE_SLOT, player.getInventory().getChestplate());
        guiInventory.setItem(LEGGINGS_SLOT, player.getInventory().getLeggings());
        guiInventory.setItem(BOOTS_SLOT, player.getInventory().getBoots());

        // Offhand
        ItemStack offhand = player.getInventory().getItemInOffHand();
        if (offhand.getType() != Material.AIR) {
            guiInventory.setItem(OFFHAND_SLOT, offhand);
        } else {
            guiInventory.setItem(OFFHAND_SLOT, null);
        }

        // Main inventory (player slots 9-35) -> GUI slots 9-35
        for (int i = 9; i < 36; i++) {
            guiInventory.setItem(i, player.getInventory().getItem(i));
        }

        // Hotbar (player slots 0-8) -> GUI slots 36-44
        for (int i = 0; i < 9; i++) {
            guiInventory.setItem(36 + i, player.getInventory().getItem(i));
        }
    }

    public void saveFromInventory(Player player, Inventory inventory) {
        int kitNumber = getPlayerKitNumber(player);

        ItemStack[] items = new ItemStack[45];

        // Armor (GUI 0-3)
        items[0] = inventory.getItem(HELMET_SLOT);
        items[1] = inventory.getItem(CHESTPLATE_SLOT);
        items[2] = inventory.getItem(LEGGINGS_SLOT);
        items[3] = inventory.getItem(BOOTS_SLOT);

        // Offhand (GUI 5)
        items[5] = inventory.getItem(OFFHAND_SLOT);

        // Inventory items (GUI 9-44)
        for (int i = 9; i < 45; i++) {
            items[i] = inventory.getItem(i);
        }

        plugin.getKitSlotStorage().saveKit(player, kitNumber, items);
    }

    public void resetInventory(Inventory inventory) {
        // Clear armor slots
        inventory.setItem(HELMET_SLOT, null);
        inventory.setItem(CHESTPLATE_SLOT, null);
        inventory.setItem(LEGGINGS_SLOT, null);
        inventory.setItem(BOOTS_SLOT, null);

        // Clear offhand
        inventory.setItem(OFFHAND_SLOT, null);

        // Clear inventory slots (9-44)
        for (int i = 9; i < 45; i++) {
            inventory.setItem(i, null);
        }

        // Keep hardcoded items intact
        // Separators stay: slots 4, 6, 7, 8
        // Bottom row stays: slots 45-53
    }

    public int getPlayerKitNumber(Player player) {
        return playerKitNumber.getOrDefault(player.getUniqueId(), 1);
    }

    public void removePlayer(Player player) {
        playerKitNumber.remove(player.getUniqueId());
    }

    private void setBottomRow(Inventory inventory) {
        inventory.setItem(45, createGlassPane());
        inventory.setItem(46, createGlassPane());
        inventory.setItem(47, createItem(Material.CHEST, 1, "Import Inventory", KIT_COLOR));
        inventory.setItem(48, createGlassPane());
        inventory.setItem(49, createItem(Material.LIME_DYE, 1, "Save", SAVE_COLOR));
        inventory.setItem(50, createGlassPane());
        inventory.setItem(51, createItem(Material.ANVIL, 1, "Reset", RESET_COLOR));
        inventory.setItem(52, createGlassPane());
        inventory.setItem(53, createGlassPane());
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
}