package dev.samar.pearl.gui;

import dev.samar.pearl.Pearl;
import dev.samar.pearl.command.KitCommand;
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

import java.util.Map;

public class PremadeKitsGui {

    private static final TextColor KIT_COLOR = TextColor.fromHexString("#3799DD");

    public static final Map<Integer, String> PREMADE_KIT_SLOTS = Map.of(
            11, "eval",
            13, "drain",
            15, "netherite_pot"
    );

    public static final Map<Integer, String> PREMADE_KIT_DISPLAY_NAMES = Map.of(
            11, "Eval",
            13, "Drain",
            15, "Netherite Pot"
    );

    private final Pearl plugin;
    private final Component guiTitle;

    public PremadeKitsGui(Pearl plugin) {
        this.plugin = plugin;

        this.guiTitle = Component.text("Premade Kits")
                .color(NamedTextColor.LIGHT_PURPLE)
                .decoration(TextDecoration.BOLD, true);
    }

    public void open(Player player) {
        Inventory inventory = Bukkit.createInventory(null, 27, guiTitle);

        inventory.setItem(11, createItem(Material.END_CRYSTAL, 1, "Eval"));
        inventory.setItem(13, createItem(Material.CROSSBOW, 1, "Drain"));
        inventory.setItem(15, createItem(Material.NETHERITE_SWORD, 1, "Netherite Pot"));

        player.openInventory(inventory);
    }

    public void loadPremadeKit(Player player, String kitName) {
        ItemStack[] saved = plugin.getPremadeKitStorage().loadKit(kitName);

        boolean isEmpty = true;
        for (ItemStack item : saved) {
            if (item != null) {
                isEmpty = false;
                break;
            }
        }

        if (isEmpty) {
            player.sendActionBar(Component.text("This premade kit is empty!")
                    .color(NamedTextColor.RED));
            return;
        }

        // Clear everything first
        player.getInventory().clear();
        player.getInventory().setArmorContents(new ItemStack[4]);
        player.getInventory().setItemInOffHand(new ItemStack(Material.AIR));

        // Set armor as a batch using setArmorContents for reliable refresh
        ItemStack[] armorContents = new ItemStack[4];
        armorContents[3] = saved[0] != null ? saved[0].clone() : null; // Helmet = index 3
        armorContents[2] = saved[1] != null ? saved[1].clone() : null; // Chestplate = index 2
        armorContents[1] = saved[2] != null ? saved[2].clone() : null; // Leggings = index 1
        armorContents[0] = saved[3] != null ? saved[3].clone() : null; // Boots = index 0
        player.getInventory().setArmorContents(armorContents);

        // Set offhand
        player.getInventory().setItemInOffHand(saved[5] != null ? saved[5].clone() : new ItemStack(Material.AIR));

        // Set main inventory (slots 9-35)
        for (int i = 9; i < 36; i++) {
            if (saved[i] != null) {
                player.getInventory().setItem(i, saved[i].clone());
            }
        }

        // Set hotbar (saved slots 36-44 -> player slots 0-8)
        for (int i = 36; i < 45; i++) {
            if (saved[i] != null) {
                player.getInventory().setItem(i - 36, saved[i].clone());
            }
        }

        player.updateInventory();
        player.closeInventory();

        // Schedule a delayed inventory refresh to ensure armor renders on client
        FoliaUtil.runOnEntityDelayed(plugin, player, () -> {
            if (player.isOnline()) {
                player.updateInventory();
            }
        }, 1L);

        // Track for regear
        KitCommand.setLastLoadedKit(player, "premade:" + kitName);

        player.sendActionBar(Component.text("Premade kit loaded!")
                .color(TextColor.fromHexString("#3799DD")));
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

    public Component getGuiTitle() {
        return guiTitle;
    }
}