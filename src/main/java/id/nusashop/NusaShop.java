package id.nusashop;

import id.nusashop.commands.AdminCommand;
import id.nusashop.commands.AdminTabCompleter;
import id.nusashop.commands.ShopCommand;
import id.nusashop.commands.StatsCommand;
import id.nusashop.commands.SellGuiCommand;
import id.nusashop.commands.BlackmarketCommand;
import id.nusashop.commands.BlackmarketTabCompleter;
import id.nusashop.commands.SellWandCommand;
import id.nusashop.config.ConfigManager;
import id.nusashop.listeners.AdminGUIListener;
import id.nusashop.listeners.ChatInputListener;
import id.nusashop.listeners.InventoryListener;
import id.nusashop.listeners.BlackmarketListener;
import id.nusashop.listeners.SellWandListener;
import id.nusashop.managers.ShopManager;
import id.nusashop.managers.StatisticsManager;
import id.nusashop.managers.BlackmarketManager;
import id.nusashop.placeholders.ShopExpansion;
import id.nusashop.utils.ChatInputManager;
import id.nusashop.utils.Messages;
import id.nusashop.managers.EventManager;

import net.milkbowl.vault.economy.Economy;

import org.bukkit.plugin.RegisteredServiceProvider;
import org.bukkit.plugin.java.JavaPlugin;
import org.bukkit.command.TabCompleter;

import java.util.logging.Logger;

/**
 * Plugin NusaShop - Sistem toko untuk server NusaTown Skyblock
 */
public class NusaShop extends JavaPlugin {
    private static NusaShop instance;
    private static final Logger LOGGER = Logger.getLogger("NusaShop");
    
    private Economy economy;
    private ConfigManager configManager;
    private ShopManager shopManager;
    private ChatInputManager chatInputManager;
    private StatisticsManager statisticsManager;
    private EventManager eventManager;
    private BlackmarketManager blackmarketManager;

    @Override
    public void onEnable() {
        instance = this;
        
        // Setup configuration
        configManager = new ConfigManager(this);
        configManager.loadConfigs();
        
        // Setup economy via Vault
        if (!setupEconomy()) {
            LOGGER.severe("Vault tidak ditemukan! Harap install Vault dan plugin economy.");
            getServer().getPluginManager().disablePlugin(this);
            return;
        }
        
        // Setup managers
        shopManager = new ShopManager(this);
        
        // Inisialisasi ChatInputManager
        chatInputManager = new ChatInputManager(this);
        
        statisticsManager = new StatisticsManager(this);
        
        // Inisialisasi EventManager
        eventManager = new EventManager(this);
        getLogger().info("Event Manager initialized");
        
        // Inisialisasi BlackmarketManager
        blackmarketManager = new BlackmarketManager(this);
        getLogger().info("Blackmarket Manager initialized");
        
        // Register commands dan tab completers
        getCommand("nusashop").setExecutor(new ShopCommand(this));
        getCommand("shopadmin").setExecutor(new AdminCommand(this));
        getCommand("shopadmin").setTabCompleter(new AdminTabCompleter(this));
        getCommand("shopstats").setExecutor(new StatsCommand(this));
        getCommand("sellgui").setExecutor(new SellGuiCommand(this));
        getCommand("blackmarket").setExecutor(new BlackmarketCommand(this));
        getCommand("blackmarket").setTabCompleter(new BlackmarketTabCompleter(this));
        getCommand("sellwand").setExecutor(new SellWandCommand(this));
        getCommand("sellwand").setTabCompleter((TabCompleter) getCommand("sellwand").getExecutor());
        
        // Register event listeners
        getServer().getPluginManager().registerEvents(new InventoryListener(this), this);
        getServer().getPluginManager().registerEvents(new AdminGUIListener(this), this);
        getServer().getPluginManager().registerEvents(new ChatInputListener(this), this);
        getServer().getPluginManager().registerEvents(new BlackmarketListener(this), this);
        getServer().getPluginManager().registerEvents(new SellWandListener(this), this);
        
        // Register PlaceholderAPI expansion if available
        if (getServer().getPluginManager().getPlugin("PlaceholderAPI") != null) {
            new ShopExpansion(this).register();
            LOGGER.info("PlaceholderAPI support enabled!");
        } else {
            LOGGER.info("PlaceholderAPI not found, placeholders will not work!");
        }
        
        LOGGER.info(Messages.PLUGIN_ENABLED);
    }

    @Override
    public void onDisable() {
        // Simpan data statistik
        if (statisticsManager != null) {
            statisticsManager.saveStats();
        }
        
        // Cleanup BlackmarketManager resources
        if (blackmarketManager != null) {
            blackmarketManager.saveStockData();
            blackmarketManager.cleanup();
        }
        
        LOGGER.info(Messages.PLUGIN_DISABLED);
    }
    
    /**
     * Setup Vault economy
     * @return true jika berhasil, false jika gagal
     */
    private boolean setupEconomy() {
        if (getServer().getPluginManager().getPlugin("Vault") == null) {
            return false;
        }
        RegisteredServiceProvider<Economy> rsp = getServer().getServicesManager().getRegistration(Economy.class);
        if (rsp == null) {
            return false;
        }
        economy = rsp.getProvider();
        return economy != null;
    }
    
    /**
     * Mendapatkan instance dari plugin
     * @return instance plugin
     */
    public static NusaShop getInstance() {
        return instance;
    }
    
    /**
     * Mendapatkan Vault Economy
     * @return Economy instance
     */
    public Economy getEconomy() {
        return economy;
    }
    
    /**
     * Mendapatkan ConfigManager
     * @return ConfigManager instance
     */
    public ConfigManager getConfigManager() {
        return configManager;
    }
    
    /**
     * Mendapatkan ShopManager
     * @return ShopManager instance
     */
    public ShopManager getShopManager() {
        return shopManager;
    }
    
    /**
     * Mendapatkan ChatInputManager
     * @return ChatInputManager instance
     */
    public ChatInputManager getChatInputManager() {
        return chatInputManager;
    }
    
    /**
     * Mendapatkan StatisticsManager
     * @return StatisticsManager instance
     */
    public StatisticsManager getStatisticsManager() {
        return statisticsManager;
    }
    
    /**
     * Mendapatkan instance EventManager
     * @return EventManager
     */
    public EventManager getEventManager() {
        return eventManager;
    }
    
    /**
     * Mendapatkan instance BlackmarketManager
     * @return BlackmarketManager instance
     */
    public BlackmarketManager getBlackmarketManager() {
        return blackmarketManager;
    }
    
    /**
     * Reload all plugin configurations and data
     */
    public void reloadPlugin() {
        // Reload all configuration files
        configManager.reloadConfigs();
        
        // Reload shop data
        shopManager.reloadShops();
        
        // Reload events
        eventManager.loadEvents();
        
        // Reload blackmarket
        blackmarketManager.reload();
        
        getLogger().info("NusaShop plugin reloaded successfully!");
    }
    
    /**
     * Mendapatkan API untuk NusaShop
     * @return NusaShopAPI instance
     */
    public id.nusashop.api.NusaShopAPI getAPI() {
        return id.nusashop.api.NusaShopAPI.getInstance();
    }
}