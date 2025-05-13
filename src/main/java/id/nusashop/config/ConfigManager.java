package id.nusashop.config;

import id.nusashop.NusaShop;
import org.bukkit.configuration.file.FileConfiguration;
import org.bukkit.configuration.file.YamlConfiguration;

import java.io.File;
import java.io.IOException;

/**
 * Manager untuk mengelola file konfigurasi plugin
 */
public class ConfigManager {
    private final NusaShop plugin;
    
    private FileConfiguration config;
    private File configFile;
    
    private FileConfiguration messages;
    private File messagesFile;
    
    private FileConfiguration shops;
    private File shopsFile;
    
    public ConfigManager(NusaShop plugin) {
        this.plugin = plugin;
    }
    
    /**
     * Metode untuk mengkopi default resource jika file tidak ada
     * @param resourceName nama resource yang akan dicopy
     * @return File yang dihasilkan
     */
    private File copyDefault(String resourceName) {
        File file = new File(plugin.getDataFolder(), resourceName);
        if (!file.exists()) {
            plugin.saveResource(resourceName, false);
        }
        return file;
    }
    
    /**
     * Memuat semua konfigurasi
     */
    public void loadConfigs() {
        // Load konfigurasi utama
        configFile = copyDefault("config.yml");
        config = YamlConfiguration.loadConfiguration(configFile);
        
        // Load file pesan
        messagesFile = copyDefault("messages.yml");
        messages = YamlConfiguration.loadConfiguration(messagesFile);
        
        // Load file shops
        shopsFile = copyDefault("shops.yml");
        shops = YamlConfiguration.loadConfiguration(shopsFile);
    }
    
    /**
     * Mendapatkan konfigurasi utama
     */
    public FileConfiguration getConfig() {
        return config;
    }
    
    /**
     * Mendapatkan konfigurasi pesan
     */
    public FileConfiguration getMessages() {
        return messages;
    }
    
    /**
     * Mendapatkan konfigurasi shops
     */
    public FileConfiguration getShops() {
        return shops;
    }
    
    /**
     * Menyimpan konfigurasi utama
     */
    public void saveConfig() {
        try {
            config.save(configFile);
        } catch (IOException e) {
            plugin.getLogger().severe("Gagal menyimpan config.yml: " + e.getMessage());
        }
    }
    
    /**
     * Menyimpan konfigurasi pesan
     */
    public void saveMessages() {
        try {
            messages.save(messagesFile);
        } catch (IOException e) {
            plugin.getLogger().severe("Gagal menyimpan messages.yml: " + e.getMessage());
        }
    }
    
    /**
     * Menyimpan konfigurasi shops
     */
    public void saveShops() {
        try {
            shops.save(shopsFile);
        } catch (IOException e) {
            plugin.getLogger().severe("Gagal menyimpan shops.yml: " + e.getMessage());
        }
    }
    
    /**
     * Reload semua konfigurasi
     */
    public void reloadConfigs() {
        config = YamlConfiguration.loadConfiguration(configFile);
        messages = YamlConfiguration.loadConfiguration(messagesFile);
        shops = YamlConfiguration.loadConfiguration(shopsFile);
    }
}