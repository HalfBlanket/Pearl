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

public class PremadeKitEditGui {

    private static final TextColor KIT_COLOR = TextColor.fromHexString("#3799DD");
    private static final TextColor SAVE_COLOR = TextColor.fromHexString("#18FA00");
    private static final TextColor RESET_COLOR = TextColor.fromHexString("#FA1B00");

    private static final int HELMET_SLOT = 0;
    private static final int CHESTPLATE_SLOT = 1;
    private static final int LEGGINGS_SLOT = 2;
    private static final int BOOTS_SLOT = 3;
    private static final int SEPARATOR_1 = 4;
    private static final int OFFHAND_SLOT = 5;
    private static final int SEPARATOR_2 = 6;
    private static final int SEPARATOR_3 = 7;
    private static final int SEPARATOR_4 = 8;

    public static final int IMPORT_SLOT = 47;
    public static final int SAVE_SLOT = 49;
    public static final int RESET_SLOT = 51;

    private final Pearl plugin;
    private final Map<UUID, String> playerKitName = new HashMap<>();

    public PremadeKitEditGui(Pearl plugin) {
        this.plugin = plugin;
    }

    public Component getGuiTitle(String kitName) {
        return Component.text("Edit: " + kitName)
                .color(NamedTextColor.LIGHT_PURPLE)
                .decoration(TextDecoration.BOLD, true);
    }

    public boolean isPremadeEditGui(Component title) {
        for (String kitName : PremadeKitsGui.PREMADE_KIT_DISPLAY_NAMES.values()) {
            if (title.equals(getGuiTitle(kitName))) {
                return true;
            }
        }
        return false;
    }

    public void open(Player player, String kitId, String displayName) {
        playerKitName.put(player.getUniqueId(), kitId);

        Inventory inventory = Bukkit.createInventory(null, 54, getGuiTitle(displayName));

        ItemStack[] saved = plugin.getPremadeKitStorage().loadKit(kitId);

        // Armor
        inventory.setItem(HELMET_SLOT, saved[HELMET_SLOT]);
        inventory.setItem(CHESTPLATE_SLOT, saved[CHESTPLATE_SLOT]);
        inventory.setItem(LEGGINGS_SLOT, saved[LEGGINGS_SLOT]);
        inventory.setItem(BOOTS_SLOT, saved[BOOTS_SLOT]);

        // Separators
        inventory.setItem(SEPARATOR_1, createGlassPane());
        inventory.setItem(OFFHAND_SLOT, saved[OFFHAND_SLOT]);
        inventory.setItem(SEPARATOR_2, createGlassPane());
        inventory.setItem(SEPARATOR_3, createGlassPane());
        inventory.setItem(SEPARATOR_4, createGlassPane());

        // Inventory slots
        for (int i = 9; i < 45; i++) {
            if (saved[i] != null) {
                inventory.setItem(i, saved[i]);
            }
        }

        setBottomRow(inventory);

        player.openInventory(inventory);
    }

    public void importFromPlayer(Player player, Inventory guiInventory) {
        guiInventory.setItem(HELMET_SLOT, player.getInventory().getHelmet());
        guiInventory.setItem(CHESTPLATE_SLOT, player.getInventory().getChestplate());
        guiInventory.setItem(LEGGINGS_SLOT, player.getInventory().getLeggings());
        guiInventory.setItem(BOOTS_SLOT, player.getInventory().getBoots());

        ItemStack offhand = player.getInventory().getItemInOffHand();
        if (offhand.getType() != Material.AIR) {
            guiInventory.setItem(OFFHAND_SLOT, offhand);
        } else {
            guiInventory.setItem(OFFHAND_SLOT, null);
        }

        for (int i = 9; i < 36; i++) {
            guiInventory.setItem(i, player.getInventory().getItem(i));
        }

        for (int i = 0; i < 9; i++) {
            guiInventory.setItem(36 + i, player.getInventory().getItem(i));
        }
    }

    public void saveFromInventory(Player player, Inventory inventory) {
        String kitName = getPlayerKitName(player);

        ItemStack[] items = new ItemStack[45];

        items[0] = inventory.getItem(HELMET_SLOT);
        items[1] = inventory.getItem(CHESTPLATE_SLOT);
        items[2] = inventory.getItem(LEGGINGS_SLOT);
        items[3] = inventory.getItem(BOOTS_SLOT);
        items[5] = inventory.getItem(OFFHAND_SLOT);

        for (int i = 9; i < 45; i++) {
            items[i] = inventory.getItem(i);
        }

        plugin.getPremadeKitStorage().saveKit(kitName, items);
    }

    public void resetInventory(Inventory inventory) {
        inventory.setItem(HELMET_SLOT, null);
        inventory.setItem(CHESTPLATE_SLOT, null);
        inventory.setItem(LEGGINGS_SLOT, null);
        inventory.setItem(BOOTS_SLOT, null);
        inventory.setItem(OFFHAND_SLOT, null);

        for (int i = 9; i < 45; i++) {
            inventory.setItem(i, null);
        }
    }

    public String getPlayerKitName(Player player) {
        return playerKitName.getOrDefault(player.getUniqueId(), "eval");
    }

    public void removePlayer(Player player) {
        playerKitName.remove(player.getUniqueId());
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