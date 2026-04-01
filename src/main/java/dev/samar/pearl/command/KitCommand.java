package dev.samar.pearl.command;

import com.mojang.brigadier.Command;
import com.mojang.brigadier.tree.LiteralCommandNode;
import dev.samar.pearl.Pearl;
import dev.samar.pearl.util.FoliaUtil;
import io.papermc.paper.command.brigadier.CommandSourceStack;
import io.papermc.paper.command.brigadier.Commands;
import io.papermc.paper.plugin.lifecycle.event.types.LifecycleEvents;
import net.kyori.adventure.text.Component;
import net.kyori.adventure.text.format.NamedTextColor;
import net.kyori.adventure.text.format.TextColor;
import org.bukkit.Bukkit;
import org.bukkit.Material;
import org.bukkit.attribute.Attribute;
import org.bukkit.entity.Player;
import org.bukkit.inventory.ItemStack;
import org.bukkit.inventory.meta.Damageable;
import org.bukkit.potion.PotionEffect;

import java.util.*;

@SuppressWarnings("UnstableApiUsage")
public class KitCommand {

    private final Pearl plugin;

    private static final Map<UUID, String> lastLoadedKit = new HashMap<>();

    private static final Set<Material> NO_RESTOCK = Set.of(
            Material.RESPAWN_ANCHOR,
            Material.TOTEM_OF_UNDYING,
            Material.ELYTRA,
            Material.FIREWORK_ROCKET,
            Material.LEATHER_HELMET, Material.LEATHER_CHESTPLATE, Material.LEATHER_LEGGINGS, Material.LEATHER_BOOTS,
            Material.CHAINMAIL_HELMET, Material.CHAINMAIL_CHESTPLATE, Material.CHAINMAIL_LEGGINGS, Material.CHAINMAIL_BOOTS,
            Material.IRON_HELMET, Material.IRON_CHESTPLATE, Material.IRON_LEGGINGS, Material.IRON_BOOTS,
            Material.GOLDEN_HELMET, Material.GOLDEN_CHESTPLATE, Material.GOLDEN_LEGGINGS, Material.GOLDEN_BOOTS,
            Material.DIAMOND_HELMET, Material.DIAMOND_CHESTPLATE, Material.DIAMOND_LEGGINGS, Material.DIAMOND_BOOTS,
            Material.NETHERITE_HELMET, Material.NETHERITE_CHESTPLATE, Material.NETHERITE_LEGGINGS, Material.NETHERITE_BOOTS,
            Material.TURTLE_HELMET
    );

    public KitCommand(Pearl plugin) {
        this.plugin = plugin;
    }

    public void register() {
        plugin.getLifecycleManager().registerEventHandler(LifecycleEvents.COMMANDS, event -> {
            final Commands commands = event.registrar();

            // /kit or /k
            LiteralCommandNode<CommandSourceStack> kitNode = Commands.literal("kit")
                    .executes(ctx -> {
                        if (ctx.getSource().getSender() instanceof Player player) {
                            FoliaUtil.runOnEntity(plugin, player, () -> plugin.getKitGui().open(player));
                        } else {
                            ctx.getSource().getSender().sendMessage("§cOnly players can use this command!");
                        }
                        return Command.SINGLE_SUCCESS;
                    })
                    .build();

            commands.register(kitNode, "Open the kits menu", List.of("k"));

            // /k1 through /k7
            for (int i = 1; i <= 7; i++) {
                final int kitNumber = i;

                LiteralCommandNode<CommandSourceStack> kitLoadNode = Commands.literal("k" + kitNumber)
                        .executes(ctx -> {
                            if (ctx.getSource().getSender() instanceof Player player) {
                                FoliaUtil.runOnEntity(plugin, player, () -> loadKit(player, kitNumber));
                            } else {
                                ctx.getSource().getSender().sendMessage("§cOnly players can use this command!");
                            }
                            return Command.SINGLE_SUCCESS;
                        })
                        .build();

                commands.register(kitLoadNode, "Load kit " + kitNumber);
            }

            // /regear
            LiteralCommandNode<CommandSourceStack> regearNode = Commands.literal("regear")
                    .executes(ctx -> {
                        if (ctx.getSource().getSender() instanceof Player player) {
                            FoliaUtil.runOnEntity(plugin, player, () -> regear(player));
                        } else {
                            ctx.getSource().getSender().sendMessage("§cOnly players can use this command!");
                        }
                        return Command.SINGLE_SUCCESS;
                    })
                    .build();

            commands.register(regearNode, "Restock consumables from last loaded kit");

            // /heal
            LiteralCommandNode<CommandSourceStack> healNode = Commands.literal("heal")
                    .executes(ctx -> {
                        if (ctx.getSource().getSender() instanceof Player player) {
                            FoliaUtil.runOnEntity(plugin, player, () -> heal(player));
                        } else {
                            ctx.getSource().getSender().sendMessage("§cOnly players can use this command!");
                        }
                        return Command.SINGLE_SUCCESS;
                    })
                    .build();

            commands.register(healNode, "Heal and restore saturation");

            // /repair
            LiteralCommandNode<CommandSourceStack> repairNode = Commands.literal("repair")
                    .executes(ctx -> {
                        if (ctx.getSource().getSender() instanceof Player player) {
                            FoliaUtil.runOnEntity(plugin, player, () -> repair(player));
                        } else {
                            ctx.getSource().getSender().sendMessage("§cOnly players can use this command!");
                        }
                        return Command.SINGLE_SUCCESS;
                    })
                    .build();

            commands.register(repairNode, "Repair all armor and tools");
        });
    }

    private void loadKit(Player player, int kitNumber) {
        ItemStack[] saved = plugin.getKitSlotStorage().loadKit(player, kitNumber);

        boolean isEmpty = true;
        for (ItemStack item : saved) {
            if (item != null) {
                isEmpty = false;
                break;
            }
        }

        if (isEmpty) {
            player.sendActionBar(Component.text("Kit " + kitNumber + " is empty!")
                    .color(NamedTextColor.RED));
            return;
        }

        // Clear everything first
        player.getInventory().clear();
        player.getInventory().setArmorContents(new ItemStack[4]);
        player.getInventory().setItemInOffHand(new ItemStack(Material.AIR));

        ItemStack[] armorContents = new ItemStack[4];
        armorContents[3] = saved[0] != null ? saved[0].clone() : null; // Helmet
        armorContents[2] = saved[1] != null ? saved[1].clone() : null; // Chestplate
        armorContents[1] = saved[2] != null ? saved[2].clone() : null; // Leggings
        armorContents[0] = saved[3] != null ? saved[3].clone() : null; // Boots
        player.getInventory().setArmorContents(armorContents);

        player.getInventory().setItemInOffHand(saved[5] != null ? saved[5].clone() : new ItemStack(Material.AIR));

        for (int i = 9; i < 36; i++) {
            if (saved[i] != null) {
                player.getInventory().setItem(i, saved[i].clone());
            }
        }

        for (int i = 36; i < 45; i++) {
            if (saved[i] != null) {
                player.getInventory().setItem(i - 36, saved[i].clone());
            }
        }

        // Load ender chest
        ItemStack[] enderItems = plugin.getKitRoomStorage().loadEnderChest(player);
        player.getEnderChest().clear();
        for (int i = 0; i < 27; i++) {
            if (enderItems[i] != null) {
                player.getEnderChest().setItem(i, enderItems[i].clone());
            }
        }

        player.updateInventory();

        FoliaUtil.runOnEntityDelayed(plugin, player, () -> {
            if (player.isOnline()) {
                player.updateInventory();
            }
        }, 1L);

        lastLoadedKit.put(player.getUniqueId(), "kit:" + kitNumber);

        player.sendActionBar(Component.text("Kit " + kitNumber + " loaded!")
                .color(TextColor.fromHexString("#3799DD")));

        broadcastKitLoad(player);
    }

    private void regear(Player player) {
        String lastKit = lastLoadedKit.get(player.getUniqueId());

        if (lastKit == null) {
            player.sendActionBar(Component.text("No kit loaded to regear from!")
                    .color(NamedTextColor.RED));
            return;
        }

        ItemStack[] saved;

        if (lastKit.startsWith("kit:")) {
            int kitNumber = Integer.parseInt(lastKit.substring(4));
            saved = plugin.getKitSlotStorage().loadKit(player, kitNumber);
        } else if (lastKit.startsWith("premade:")) {
            String kitName = lastKit.substring(8);
            saved = plugin.getPremadeKitStorage().loadKit(kitName);
        } else {
            player.sendActionBar(Component.text("No kit loaded to regear from!")
                    .color(NamedTextColor.RED));
            return;
        }

        int restocked = 0;

        for (int i = 9; i < 36; i++) {
            if (saved[i] != null && shouldRestock(saved[i])) {
                ItemStack current = player.getInventory().getItem(i);
                if (current == null || current.getType() == Material.AIR) {
                    player.getInventory().setItem(i, saved[i].clone());
                    restocked++;
                } else if (current.isSimilar(saved[i]) && current.getAmount() < saved[i].getAmount()) {
                    current.setAmount(saved[i].getAmount());
                    restocked++;
                }
            }
        }

        for (int i = 36; i < 45; i++) {
            if (saved[i] != null && shouldRestock(saved[i])) {
                int playerSlot = i - 36;
                ItemStack current = player.getInventory().getItem(playerSlot);
                if (current == null || current.getType() == Material.AIR) {
                    player.getInventory().setItem(playerSlot, saved[i].clone());
                    restocked++;
                } else if (current.isSimilar(saved[i]) && current.getAmount() < saved[i].getAmount()) {
                    current.setAmount(saved[i].getAmount());
                    restocked++;
                }
            }
        }

        if (saved[5] != null && shouldRestock(saved[5])) {
            ItemStack currentOffhand = player.getInventory().getItemInOffHand();
            if (currentOffhand.getType() == Material.AIR) {
                player.getInventory().setItemInOffHand(saved[5].clone());
                restocked++;
            } else if (currentOffhand.isSimilar(saved[5]) && currentOffhand.getAmount() < saved[5].getAmount()) {
                currentOffhand.setAmount(saved[5].getAmount());
                restocked++;
            }
        }

        player.updateInventory();

        if (restocked > 0) {
            player.sendActionBar(Component.text("Regeared " + restocked + " items!")
                    .color(NamedTextColor.GREEN));
            broadcastRegear(player);
        } else {
            player.sendActionBar(Component.text("Nothing to restock!")
                    .color(NamedTextColor.YELLOW));
        }
    }

    private void heal(Player player) {
        double maxHealth = player.getAttribute(Attribute.MAX_HEALTH).getValue();
        player.setHealth(maxHealth);

        player.setFoodLevel(20);
        player.setSaturation(20f);
        player.setExhaustion(0f);

        for (PotionEffect effect : player.getActivePotionEffects()) {
            player.removePotionEffect(effect.getType());
        }

        player.setFireTicks(0);

        player.sendActionBar(Component.text("Healed!")
                .color(NamedTextColor.GREEN));

        broadcastHeal(player);
    }

    private void repair(Player player) {
        int repaired = 0;

        for (int i = 0; i < 36; i++) {
            ItemStack item = player.getInventory().getItem(i);
            if (item != null && item.getType().getMaxDurability() > 0 && item.getItemMeta() instanceof Damageable damageable) {
                if (damageable.getDamage() > 0) {
                    damageable.setDamage(0);
                    item.setItemMeta(damageable);
                    repaired++;
                }
            }
        }

        ItemStack[] armor = player.getInventory().getArmorContents();
        for (ItemStack item : armor) {
            if (item != null && item.getType().getMaxDurability() > 0 && item.getItemMeta() instanceof Damageable damageable) {
                if (damageable.getDamage() > 0) {
                    damageable.setDamage(0);
                    item.setItemMeta(damageable);
                    repaired++;
                }
            }
        }
        player.getInventory().setArmorContents(armor);

        ItemStack offhand = player.getInventory().getItemInOffHand();
        if (offhand.getType().getMaxDurability() > 0 && offhand.getItemMeta() instanceof Damageable damageable) {
            if (damageable.getDamage() > 0) {
                damageable.setDamage(0);
                offhand.setItemMeta(damageable);
                player.getInventory().setItemInOffHand(offhand);
                repaired++;
            }
        }

        player.updateInventory();

        if (repaired > 0) {
            player.sendActionBar(Component.text("Repaired " + repaired + " items!")
                    .color(NamedTextColor.GREEN));
            broadcastRepair(player);
        } else {
            player.sendActionBar(Component.text("Nothing to repair!")
                    .color(NamedTextColor.YELLOW));
        }
    }

    private boolean shouldRestock(ItemStack item) {
        if (item == null) return false;
        return !NO_RESTOCK.contains(item.getType());
    }

    public static void setLastLoadedKit(Player player, String kit) {
        lastLoadedKit.put(player.getUniqueId(), kit);
    }

    public static void removePlayer(Player player) {
        lastLoadedKit.remove(player.getUniqueId());
    }

    public static void broadcastKitLoad(Player player) {
        Component message = Component.text("🏹 " + player.getName() + " loaded a kit")
                .color(NamedTextColor.GRAY);

        Bukkit.getServer().sendMessage(message);
    }

    public static void broadcastKitRoom(Player player) {
        Component message = Component.text("🏹 " + player.getName() + " opened kit room")
                .color(NamedTextColor.GRAY);

        Bukkit.getServer().sendMessage(message);
    }

    public static void broadcastPremadeKit(Player player) {
        Component message = Component.text("🏹 " + player.getName() + " loaded a premade kit")
                .color(NamedTextColor.GRAY);

        Bukkit.getServer().sendMessage(message);
    }

    public static void broadcastRegear(Player player) {
        Component message = Component.text("🏹 " + player.getName() + " regeared")
                .color(NamedTextColor.GRAY);

        Bukkit.getServer().sendMessage(message);
    }

    public static void broadcastHeal(Player player) {
        Component message = Component.text("❤ ")
                .color(TextColor.fromHexString("#FA1B00"))
                .append(Component.text(player.getName() + " healed")
                        .color(NamedTextColor.GRAY));

        Bukkit.getServer().sendMessage(message);
    }

    public static void broadcastRepair(Player player) {
        Component message = Component.text("🏹 " + player.getName() + " repaired")
                .color(NamedTextColor.GRAY);

        Bukkit.getServer().sendMessage(message);
    }
}