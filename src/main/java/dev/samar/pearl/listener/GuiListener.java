package dev.samar.pearl.listener;

import dev.samar.pearl.Pearl;
import dev.samar.pearl.command.KitCommand;
import dev.samar.pearl.gui.PremadeKitsGui;
import dev.samar.pearl.util.FoliaUtil;
import net.kyori.adventure.text.Component;
import net.kyori.adventure.text.format.NamedTextColor;
import net.kyori.adventure.text.format.TextColor;
import org.bukkit.Material;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.Listener;
import org.bukkit.event.inventory.ClickType;
import org.bukkit.event.inventory.InventoryClickEvent;
import org.bukkit.event.inventory.InventoryCloseEvent;
import org.bukkit.event.inventory.InventoryDragEvent;
import org.bukkit.event.player.PlayerQuitEvent;
import org.bukkit.inventory.ItemStack;

import java.util.Map;
import java.util.Set;

public class GuiListener implements Listener {

    private final Pearl plugin;

    private static final Map<Integer, String> CATEGORY_SLOTS = Map.of(
            47, "crystal_pvp",
            48, "consumables",
            49, "arrows",
            50, "potions",
            51, "miscellaneous"
    );

    private static final int REFILL_SLOT = 45;
    private static final int FILL_TOTEM_SLOT = 53;
    private static final int SAVE_SLOT = 53;

    private static final int EC_IMPORT_SLOT = 29;
    private static final int EC_SAVE_SLOT = 31;
    private static final int EC_RESET_SLOT = 33;

    private static final int KS_IMPORT_SLOT = 47;
    private static final int KS_SAVE_SLOT = 49;
    private static final int KS_RESET_SLOT = 51;

    private static final Set<Integer> KS_LOCKED_SLOTS = Set.of(4, 6, 7, 8, 45, 46, 47, 48, 49, 50, 51, 52, 53);

    private static final Map<Integer, Integer> KIT_CHEST_SLOTS = Map.of(
            10, 1,
            11, 2,
            12, 3,
            13, 4,
            14, 5,
            15, 6,
            16, 7
    );

    private static final int PE_IMPORT_SLOT = 47;
    private static final int PE_SAVE_SLOT = 49;
    private static final int PE_RESET_SLOT = 51;

    private static final Set<Integer> PE_LOCKED_SLOTS = Set.of(4, 6, 7, 8, 45, 46, 47, 48, 49, 50, 51, 52, 53);

    private static final Component SAVED_MESSAGE = Component.text("ⓘ Saved")
            .color(TextColor.fromHexString("#18FA00"));

    public GuiListener(Pearl plugin) {
        this.plugin = plugin;
    }

    @EventHandler
    public void onInventoryClick(InventoryClickEvent event) {
        if (!(event.getWhoClicked() instanceof Player player)) return;

        Component title = event.getView().title();

        // Main Kit GUI
        if (title.equals(plugin.getKitGui().getGuiTitle())) {
            event.setCancelled(true);

            if (event.getCurrentItem() == null) return;

            if (event.getSlot() == 31) {
                if (event.getClick() == ClickType.RIGHT && player.hasPermission("pearl.admin")) {
                    FoliaUtil.runOnEntity(plugin, player, () -> plugin.getKitRoomGui().openAdmin(player));
                } else if (event.getClick() == ClickType.LEFT) {
                    FoliaUtil.runOnEntity(plugin, player, () -> {
                        plugin.getKitRoomGui().openNormal(player);
                        KitCommand.broadcastKitRoom(player);
                    });
                }
            }

            if (event.getSlot() == 29) {
                FoliaUtil.runOnEntity(plugin, player, () -> plugin.getEnderChestGui().open(player));
            }

            if (event.getSlot() == 33) {
                FoliaUtil.runOnEntity(plugin, player, () -> plugin.getPremadeKitsGui().open(player));
            }

            if (KIT_CHEST_SLOTS.containsKey(event.getSlot())) {
                int kitNumber = KIT_CHEST_SLOTS.get(event.getSlot());
                FoliaUtil.runOnEntity(plugin, player, () -> plugin.getKitSlotGui().open(player, kitNumber));
            }

            return;
        }

        // Kit Room Normal GUI
        if (title.equals(plugin.getKitRoomGui().getNormalTitle())) {

            if (event.getRawSlot() >= event.getView().getTopInventory().getSize()) {
                return;
            }

            if (event.getSlot() >= 45 && event.getSlot() <= 53) {
                event.setCancelled(true);

                if (event.getCurrentItem() == null) return;

                if (event.getSlot() == REFILL_SLOT) {
                    String category = plugin.getKitRoomGui().getPlayerCategory(player);
                    ItemStack[] saved = plugin.getKitRoomStorage().loadItems(category);

                    for (int i = 0; i < 45; i++) {
                        event.getView().getTopInventory().setItem(i, saved[i]);
                    }

                    player.sendActionBar(Component.text("Kit room refilled!")
                            .color(NamedTextColor.GREEN));
                    return;
                }

                if (event.getSlot() == FILL_TOTEM_SLOT) {
                    int filled = 0;
                    ItemStack totem = new ItemStack(Material.TOTEM_OF_UNDYING, 1);

                    for (int i = 0; i < 36; i++) {
                        ItemStack slot = player.getInventory().getItem(i);
                        if (slot == null || slot.getType() == Material.AIR) {
                            player.getInventory().setItem(i, totem.clone());
                            filled++;
                        }
                    }

                    ItemStack offhand = player.getInventory().getItemInOffHand();
                    if (offhand.getType() == Material.AIR) {
                        player.getInventory().setItemInOffHand(totem.clone());
                        filled++;
                    }

                    if (filled > 0) {
                        player.sendActionBar(Component.text("Filled " + filled + " slots with totems!")
                                .color(NamedTextColor.GREEN));
                    } else {
                        player.sendActionBar(Component.text("No empty slots to fill!")
                                .color(NamedTextColor.RED));
                    }
                    return;
                }

                if (CATEGORY_SLOTS.containsKey(event.getSlot())) {
                    String category = CATEGORY_SLOTS.get(event.getSlot());
                    String current = plugin.getKitRoomGui().getPlayerCategory(player);

                    if (!category.equals(current)) {
                        if (event.getCursor() != null && !event.getCursor().getType().isAir()) {
                            player.getInventory().addItem(event.getCursor().clone());
                            event.getView().setCursor(null);
                        }
                        FoliaUtil.runOnEntity(plugin, player, () -> plugin.getKitRoomGui().openNormal(player, category));
                    }
                }
                return;
            }

            return;
        }

        // Kit Room Admin GUI
        if (title.equals(plugin.getKitRoomGui().getAdminTitle())) {
            if (event.getRawSlot() >= 45 && event.getRawSlot() <= 53) {
                event.setCancelled(true);

                if (event.getCurrentItem() == null) return;

                if (event.getSlot() == SAVE_SLOT) {
                    plugin.getKitRoomGui().saveFromInventory(player, event.getInventory());

                    player.sendActionBar(SAVED_MESSAGE);
                    return;
                }

                if (event.getSlot() == REFILL_SLOT) {
                    String category = plugin.getKitRoomGui().getPlayerCategory(player);
                    ItemStack[] saved = plugin.getKitRoomStorage().loadItems(category);

                    for (int i = 0; i < 45; i++) {
                        event.getView().getTopInventory().setItem(i, saved[i]);
                    }

                    player.sendActionBar(Component.text("Kit room refilled!")
                            .color(NamedTextColor.GREEN));
                    return;
                }

                if (CATEGORY_SLOTS.containsKey(event.getSlot())) {
                    String category = CATEGORY_SLOTS.get(event.getSlot());
                    String current = plugin.getKitRoomGui().getPlayerCategory(player);

                    if (!category.equals(current)) {
                        plugin.getKitRoomGui().switchCategory(player, event.getInventory(), category, true);

                        player.sendActionBar(SAVED_MESSAGE);
                    }
                }
                return;
            }
        }

        // Ender Chest GUI
        if (title.equals(plugin.getEnderChestGui().getGuiTitle())) {

            if (event.getRawSlot() >= event.getView().getTopInventory().getSize()) {
                return;
            }

            if (event.getSlot() >= 27 && event.getSlot() <= 35) {
                event.setCancelled(true);

                if (event.getCurrentItem() == null) return;

                if (event.getSlot() == EC_IMPORT_SLOT) {
                    plugin.getEnderChestGui().importFromEnderChest(player, event.getView().getTopInventory());

                    player.sendActionBar(Component.text("Imported from ender chest!")
                            .color(NamedTextColor.GREEN));
                    return;
                }

                if (event.getSlot() == EC_SAVE_SLOT) {
                    plugin.getEnderChestGui().saveFromInventory(player, event.getView().getTopInventory());

                    player.sendActionBar(SAVED_MESSAGE);
                    return;
                }

                if (event.getSlot() == EC_RESET_SLOT) {
                    plugin.getEnderChestGui().resetAndSync(player, event.getView().getTopInventory());

                    player.sendActionBar(Component.text("Ender chest reset!")
                            .color(NamedTextColor.RED));
                    return;
                }

                return;
            }
        }

        // Kit Slot GUI (Kit 1-7)
        if (plugin.getKitSlotGui().isKitSlotGui(title)) {

            if (event.getRawSlot() >= event.getView().getTopInventory().getSize()) {
                return;
            }

            if (KS_LOCKED_SLOTS.contains(event.getSlot())) {
                event.setCancelled(true);

                if (event.getCurrentItem() == null) return;

                if (event.getSlot() == KS_IMPORT_SLOT) {
                    plugin.getKitSlotGui().importFromPlayer(player, event.getView().getTopInventory());

                    player.sendActionBar(Component.text("Inventory imported!")
                            .color(NamedTextColor.GREEN));
                    return;
                }

                if (event.getSlot() == KS_SAVE_SLOT) {
                    plugin.getKitSlotGui().saveFromInventory(player, event.getView().getTopInventory());

                    player.sendActionBar(SAVED_MESSAGE);
                    return;
                }

                if (event.getSlot() == KS_RESET_SLOT) {
                    int kitNumber = plugin.getKitSlotGui().getPlayerKitNumber(player);
                    plugin.getKitSlotGui().resetInventory(event.getView().getTopInventory());
                    plugin.getKitSlotStorage().resetKit(player, kitNumber);

                    player.sendActionBar(Component.text("Kit reset!")
                            .color(NamedTextColor.RED));
                    return;
                }

                return;
            }
        }

        // Premade Kits GUI
        if (title.equals(plugin.getPremadeKitsGui().getGuiTitle())) {
            event.setCancelled(true);

            if (event.getCurrentItem() == null) return;

            if (PremadeKitsGui.PREMADE_KIT_SLOTS.containsKey(event.getSlot())) {
                String kitId = PremadeKitsGui.PREMADE_KIT_SLOTS.get(event.getSlot());
                String displayName = PremadeKitsGui.PREMADE_KIT_DISPLAY_NAMES.get(event.getSlot());

                if (event.getClick() == ClickType.RIGHT && player.hasPermission("pearl.admin")) {
                    FoliaUtil.runOnEntity(plugin, player, () -> plugin.getPremadeKitEditGui().open(player, kitId, displayName));
                    return;
                }

                if (event.getClick() == ClickType.LEFT) {
                    FoliaUtil.runOnEntity(plugin, player, () -> {
                        plugin.getPremadeKitsGui().loadPremadeKit(player, kitId);
                        KitCommand.broadcastPremadeKit(player);
                    });
                }
            }

            return;
        }

        // Premade Kit Edit GUI (Admin)
        if (plugin.getPremadeKitEditGui().isPremadeEditGui(title)) {

            if (event.getRawSlot() >= event.getView().getTopInventory().getSize()) {
                return;
            }

            if (PE_LOCKED_SLOTS.contains(event.getSlot())) {
                event.setCancelled(true);

                if (event.getCurrentItem() == null) return;

                if (event.getSlot() == PE_IMPORT_SLOT) {
                    plugin.getPremadeKitEditGui().importFromPlayer(player, event.getView().getTopInventory());

                    player.sendActionBar(Component.text("Inventory imported!")
                            .color(NamedTextColor.GREEN));
                    return;
                }

                if (event.getSlot() == PE_SAVE_SLOT) {
                    plugin.getPremadeKitEditGui().saveFromInventory(player, event.getView().getTopInventory());

                    player.sendActionBar(SAVED_MESSAGE);
                    return;
                }

                if (event.getSlot() == PE_RESET_SLOT) {
                    String kitName = plugin.getPremadeKitEditGui().getPlayerKitName(player);
                    plugin.getPremadeKitEditGui().resetInventory(event.getView().getTopInventory());
                    plugin.getPremadeKitStorage().resetKit(kitName);

                    player.sendActionBar(Component.text("Premade kit reset!")
                            .color(NamedTextColor.RED));
                    return;
                }

                return;
            }
        }
    }

    @EventHandler
    public void onInventoryDrag(InventoryDragEvent event) {
        if (!(event.getWhoClicked() instanceof Player)) return;

        Component title = event.getView().title();

        if (title.equals(plugin.getKitGui().getGuiTitle())) {
            event.setCancelled(true);
            return;
        }

        if (title.equals(plugin.getKitRoomGui().getNormalTitle())) {
            for (int slot : event.getRawSlots()) {
                if (slot < event.getView().getTopInventory().getSize()) {
                    event.setCancelled(true);
                    return;
                }
            }
            return;
        }

        if (title.equals(plugin.getKitRoomGui().getAdminTitle())) {
            for (int slot : event.getRawSlots()) {
                if (slot >= 45 && slot <= 53) {
                    event.setCancelled(true);
                    return;
                }
            }
        }

        if (title.equals(plugin.getEnderChestGui().getGuiTitle())) {
            for (int slot : event.getRawSlots()) {
                if (slot >= 27 && slot <= 35) {
                    event.setCancelled(true);
                    return;
                }
            }
        }

        if (plugin.getKitSlotGui().isKitSlotGui(title)) {
            for (int slot : event.getRawSlots()) {
                if (KS_LOCKED_SLOTS.contains(slot)) {
                    event.setCancelled(true);
                    return;
                }
            }
        }

        if (title.equals(plugin.getPremadeKitsGui().getGuiTitle())) {
            event.setCancelled(true);
        }

        if (plugin.getPremadeKitEditGui().isPremadeEditGui(title)) {
            for (int slot : event.getRawSlots()) {
                if (PE_LOCKED_SLOTS.contains(slot)) {
                    event.setCancelled(true);
                    return;
                }
            }
        }
    }

    @EventHandler
    public void onInventoryClose(InventoryCloseEvent event) {
        if (!(event.getPlayer() instanceof Player player)) return;

        if (plugin.getKitRoomGui().isSwitching(player)) {
            return;
        }

        Component title = event.getView().title();

        if (title.equals(plugin.getKitRoomGui().getNormalTitle())) {
            plugin.getKitRoomGui().removePlayer(player);
        }

        if (title.equals(plugin.getKitRoomGui().getAdminTitle())) {
            plugin.getKitRoomGui().saveFromInventory(player, event.getInventory());
            plugin.getKitRoomGui().removePlayer(player);

            player.sendActionBar(SAVED_MESSAGE);
        }

        if (title.equals(plugin.getEnderChestGui().getGuiTitle())) {
            plugin.getEnderChestGui().saveFromInventory(player, event.getInventory());

            player.sendActionBar(SAVED_MESSAGE);
        }

        if (plugin.getKitSlotGui().isKitSlotGui(title)) {
            plugin.getKitSlotGui().saveFromInventory(player, event.getView().getTopInventory());
            plugin.getKitSlotGui().removePlayer(player);

            player.sendActionBar(SAVED_MESSAGE);
        }

        if (plugin.getPremadeKitEditGui().isPremadeEditGui(title)) {
            plugin.getPremadeKitEditGui().saveFromInventory(player, event.getInventory());
            plugin.getPremadeKitEditGui().removePlayer(player);

            player.sendActionBar(SAVED_MESSAGE);
        }
    }

    @EventHandler
    public void onPlayerQuit(PlayerQuitEvent event) {
        Player player = event.getPlayer();
        KitCommand.removePlayer(player);
        plugin.getKitRoomGui().removePlayer(player);
        plugin.getKitSlotGui().removePlayer(player);
        plugin.getPremadeKitEditGui().removePlayer(player);
    }
}