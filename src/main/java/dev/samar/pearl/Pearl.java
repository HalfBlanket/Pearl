package dev.samar.pearl;

import dev.samar.pearl.command.KitCommand;
import dev.samar.pearl.gui.*;
import dev.samar.pearl.listener.GuiListener;
import dev.samar.pearl.storage.*;
import org.bukkit.plugin.java.JavaPlugin;

public final class Pearl extends JavaPlugin {
    private static Pearl instance;

    private KitGui kitGui;
    private KitRoomGui kitRoomGui;
    private EnderChestGui enderChestGui;
    private KitSlotGui kitSlotGui;
    private PremadeKitsGui premadeKitsGui;
    private PremadeKitEditGui premadeKitEditGui;

    private DatabaseProvider databaseProvider;
    private KitRoomStorage kitRoomStorage;
    private KitSlotStorage kitSlotStorage;
    private PremadeKitStorage premadeKitStorage;

    @Override
    public void onEnable() {
        instance = this;
        saveDefaultConfig();

        // Initialize the shared database provider
        this.databaseProvider = new DatabaseProvider(this);
        databaseProvider.initialize();

        if (!databaseProvider.isInitialized()) {
            getLogger().severe("====================================");
            getLogger().severe("DATABASE FAILED TO INITIALIZE!");
            getLogger().severe("Pearl will not function correctly.");
            getLogger().severe("Check your config.yml database settings.");
            getLogger().severe("====================================");
        }

        // Initialize storages with the shared provider
        this.kitRoomStorage = new KitRoomStorage(this, databaseProvider);
        kitRoomStorage.initialize();

        this.kitSlotStorage = new KitSlotStorage(this, databaseProvider);
        kitSlotStorage.initialize();

        this.premadeKitStorage = new PremadeKitStorage(this, databaseProvider);
        premadeKitStorage.initialize();

        // Initialize GUIs
        this.kitGui = new KitGui(this);
        this.kitRoomGui = new KitRoomGui(this);
        this.enderChestGui = new EnderChestGui(this);
        this.kitSlotGui = new KitSlotGui(this);
        this.premadeKitsGui = new PremadeKitsGui(this);
        this.premadeKitEditGui = new PremadeKitEditGui(this);

        // Register commands and listeners
        new KitCommand(this).register();
        getServer().getPluginManager().registerEvents(new GuiListener(this), this);

        getLogger().info("Pearl enabled with " + databaseProvider.getStorageType().toUpperCase() + " storage.");
    }

    @Override
    public void onDisable() {
        if (databaseProvider != null) databaseProvider.close();
    }

    public static Pearl getInstance() { return instance; }
    public KitGui getKitGui() { return kitGui; }
    public KitRoomGui getKitRoomGui() { return kitRoomGui; }
    public EnderChestGui getEnderChestGui() { return enderChestGui; }
    public KitSlotGui getKitSlotGui() { return kitSlotGui; }
    public PremadeKitsGui getPremadeKitsGui() { return premadeKitsGui; }
    public PremadeKitEditGui getPremadeKitEditGui() { return premadeKitEditGui; }
    public KitRoomStorage getKitRoomStorage() { return kitRoomStorage; }
    public KitSlotStorage getKitSlotStorage() { return kitSlotStorage; }
    public PremadeKitStorage getPremadeKitStorage() { return premadeKitStorage; }
    public DatabaseProvider getDatabaseProvider() { return databaseProvider; }
}