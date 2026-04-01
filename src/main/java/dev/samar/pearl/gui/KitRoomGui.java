package dev.samar.pearl.gui;

import dev.samar.pearl.Pearl;
import dev.samar.pearl.util.FoliaUtil;
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
import org.bukkit.inventory.meta.PotionMeta;
import org.bukkit.potion.PotionType;

import java.util.HashMap;
import java.util.HashSet;
import java.util.Map;
import java.util.Set;
import java.util.UUID;

public class KitRoomGui {

    private static final TextColor KIT_COLOR = TextColor.fromHexString("#3799DD");
    private static final TextColor SAVE_COLOR = TextColor.fromHexString("#18FA00");
    private static final TextColor ACTIVE_COLOR = TextColor.fromHexString("#FFFF00");

    public static final String DEFAULT_CATEGORY = "crystal_pvp";

    private final Pearl plugin;
    private final Component normalTitle;
    private final Component adminTitle;

    private final Map<UUID, String> playerCategory = new HashMap<>();
    private final Set<UUID> switching = new HashSet<>();

    public KitRoomGui(Pearl plugin) {
        this.plugin = plugin;

        this.normalTitle = Component.text("Kit Room")
                .color(NamedTextColor.LIGHT_PURPLE)
                .decoration(TextDecoration.BOLD, true);

        this.adminTitle = Component.text("Kit Room (Edit)")
                .color(NamedTextColor.LIGHT_PURPLE)
                .decoration(TextDecoration.BOLD, true);
    }

    public void openNormal(Player player) {
        openNormal(player, DEFAULT_CATEGORY);
    }

    public void openNormal(Player player, String category) {
        switching.add(player.getUniqueId());
        playerCategory.put(player.getUniqueId(), category);

        Inventory inventory = Bukkit.createInventory(null, 54, normalTitle);

        ItemStack[] saved = plugin.getKitRoomStorage().loadItems(category);
        for (int i = 0; i < 45; i++) {
            if (saved[i] != null) {
                inventory.setItem(i, saved[i]);
            }
        }

        setBottomRow(inventory, false, category);

        player.openInventory(inventory);
        switching.remove(player.getUniqueId());
    }

    public void openAdmin(Player player) {
        openAdmin(player, DEFAULT_CATEGORY);
    }

    public void openAdmin(Player player, String category) {
        switching.add(player.getUniqueId());
        playerCategory.put(player.getUniqueId(), category);

        Inventory inventory = Bukkit.createInventory(null, 54, adminTitle);

        ItemStack[] saved = plugin.getKitRoomStorage().loadItems(category);
        for (int i = 0; i < 45; i++) {
            if (saved[i] != null) {
                inventory.setItem(i, saved[i]);
            }
        }

        setBottomRow(inventory, true, category);

        player.openInventory(inventory);
        switching.remove(player.getUniqueId());
    }

    public void switchCategory(Player player, Inventory currentInventory, String newCategory, boolean admin) {
        String currentCategory = getPlayerCategory(player);
        saveFromInventory(currentCategory, currentInventory);

        playerCategory.put(player.getUniqueId(), newCategory);

        ItemStack[] saved = plugin.getKitRoomStorage().loadItems(newCategory);

        for (int i = 0; i < 45; i++) {
            currentInventory.setItem(i, saved[i]);
        }

        setBottomRow(currentInventory, admin, newCategory);
    }

    public void saveFromInventory(Player player, Inventory inventory) {
        String category = getPlayerCategory(player);
        saveFromInventory(category, inventory);
    }

    public void saveFromInventory(String category, Inventory inventory) {
        ItemStack[] items = new ItemStack[45];
        for (int i = 0; i < 45; i++) {
            items[i] = inventory.getItem(i);
        }

        plugin.getKitRoomStorage().saveItems(category, items);
    }

    public String getPlayerCategory(Player player) {
        return playerCategory.getOrDefault(player.getUniqueId(), DEFAULT_CATEGORY);
    }

    public boolean isSwitching(Player player) {
        return switching.contains(player.getUniqueId());
    }

    public void removePlayer(Player player) {
        playerCategory.remove(player.getUniqueId());
        switching.remove(player.getUniqueId());
    }

    private void setBottomRow(Inventory inventory, boolean admin, String activeCategory) {
        inventory.setItem(45, createCategoryItem(Material.BARREL, "Refill", "refill", activeCategory));
        inventory.setItem(46, createGlassPane());
        inventory.setItem(47, createCategoryItem(Material.NETHERITE_CHESTPLATE, "Crystal PvP", "crystal_pvp", activeCategory));
        inventory.setItem(48, createCategoryItem(Material.GOLDEN_APPLE, "Consumables", "consumables", activeCategory));
        inventory.setItem(49, createCategoryItem(Material.CROSSBOW, "Arrows", "arrows", activeCategory));
        inventory.setItem(50, createPotionCategoryItem("Potions", "potions", activeCategory));
        inventory.setItem(51, createCategoryItem(Material.RECOVERY_COMPASS, "Miscellaneous", "miscellaneous", activeCategory));
        inventory.setItem(52, createGlassPane());

        if (admin) {
            inventory.setItem(53, createItem(Material.LIME_DYE, 1, "Save", SAVE_COLOR));
        } else {
            inventory.setItem(53, createItem(Material.TOTEM_OF_UNDYING, 1, "Fill totem", KIT_COLOR));
        }
    }

    private ItemStack createCategoryItem(Material material, String name, String category, String activeCategory) {
        ItemStack item = new ItemStack(material, 1);
        ItemMeta meta = item.getItemMeta();

        TextColor color = category.equals(activeCategory) ? ACTIVE_COLOR : KIT_COLOR;

        meta.displayName(Component.text(name)
                .color(color)
                .decoration(TextDecoration.ITALIC, false));

        if (category.equals(activeCategory)) {
            meta.setEnchantmentGlintOverride(true);
        }

        item.setItemMeta(meta);
        return item;
    }

    private ItemStack createPotionCategoryItem(String name, String category, String activeCategory) {
        ItemStack item = new ItemStack(Material.POTION, 1);
        PotionMeta meta = (PotionMeta) item.getItemMeta();

        meta.setBasePotionType(PotionType.WATER);

        TextColor color = category.equals(activeCategory) ? ACTIVE_COLOR : KIT_COLOR;

        meta.displayName(Component.text(name)
                .color(color)
                .decoration(TextDecoration.ITALIC, false));

        if (category.equals(activeCategory)) {
            meta.setEnchantmentGlintOverride(true);
        }

        item.setItemMeta(meta);
        return item;
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

    public Component getNormalTitle() {
        return normalTitle;
    }

    public Component getAdminTitle() {
        return adminTitle;
    }
}