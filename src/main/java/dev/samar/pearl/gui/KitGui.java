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

public class KitGui {

    private static final TextColor KIT_COLOR = TextColor.fromHexString("#3799DD");

    private final Pearl plugin;
    private final String title;
    private final int rows;
    private final Component guiTitle;

    public KitGui(Pearl plugin) {
        this.plugin = plugin;
        this.title = plugin.getConfig().getString("gui.title", "Pearl Kits");
        this.rows = plugin.getConfig().getInt("gui.rows", 4);
        this.guiTitle = Component.text(title)
                .color(NamedTextColor.LIGHT_PURPLE)
                .decoration(TextDecoration.BOLD, true);
    }

    public void open(Player player) {
        int size = rows * 9;

        Inventory inventory = Bukkit.createInventory(null, size, guiTitle);

        for (int i = 0; i < 7; i++) {
            inventory.setItem(10 + i, createItem(Material.CHEST, i + 1, "ᴋɪᴛ " + (i + 1)));
        }

        inventory.setItem(27, createGlassPane());
        inventory.setItem(28, createGlassPane());
        inventory.setItem(29, createItem(Material.ENDER_CHEST, 1, "ᴇɴᴅᴇʀᴄʜᴇѕᴛ"));
        inventory.setItem(30, createGlassPane());
        inventory.setItem(31, createItem(Material.LECTERN, 1, "ᴋɪᴛ ʀᴏᴏᴍ"));
        inventory.setItem(32, createGlassPane());
        inventory.setItem(33, createItem(Material.DIAMOND_NAUTILUS_ARMOR, 1, "ᴘʀᴇᴍᴀᴅᴇ ᴋɪᴛѕ"));
        inventory.setItem(34, createGlassPane());
        inventory.setItem(35, createGlassPane());

        player.openInventory(inventory);
    }

    public Component getGuiTitle() {
        return guiTitle;
    }

    private ItemStack createItem(Material material, int amount, String name) {
        ItemStack item = new ItemStack(material, amount);
        ItemMeta meta = item.getItemMeta();

        meta.displayName(Component.text(name)
                .color(KIT_COLOR)
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