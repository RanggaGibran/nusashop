package id.nusashop.utils;

import id.nusashop.NusaShop;

import org.bukkit.ChatColor;
import org.bukkit.configuration.file.FileConfiguration;
import org.bukkit.entity.Player;

/**
 * Utilitas untuk pesan-pesan plugin
 */
public class Messages {
    // Sistem messages
    public static final String PLUGIN_ENABLED = "NusaShop berhasil diaktifkan!";
    public static final String PLUGIN_DISABLED = "NusaShop dinonaktifkan!";
    public static final String ECONOMY_NOT_FOUND = "Vault Economy tidak ditemukan! Plugin dinonaktifkan.";
    
    /**
     * Mendapatkan pesan dari file messages.yml
     * @param path path ke pesan dalam file konfigurasi
     * @return pesan yang sudah diformat dengan warna
     */
    public static String get(String path) {
        FileConfiguration messages = NusaShop.getInstance().getConfigManager().getMessages();
        String message = messages.getString(path);
        if (message == null) {
            return ChatColor.RED + "Pesan tidak ditemukan: " + path;
        }
        return ChatColor.translateAlternateColorCodes('&', message);
    }
    
    /**
     * Mengirim pesan ke pemain
     * @param player pemain yang akan menerima pesan
     * @param path path ke pesan dalam file konfigurasi
     */
    public static void send(Player player, String path) {
        player.sendMessage(get(path));
    }
    
    /**
     * Mengirim pesan ke pemain dengan placeholder
     * @param player pemain yang akan menerima pesan
     * @param path path ke pesan dalam file konfigurasi
     * @param placeholders placeholder yang akan diganti (format: %placeholder%)
     * @param values nilai untuk menggantikan placeholder
     */
    public static void send(Player player, String path, String[] placeholders, String[] values) {
        String message = get(path);
        for (int i = 0; i < placeholders.length; i++) {
            message = message.replace(placeholders[i], values[i]);
        }
        player.sendMessage(message);
    }
}