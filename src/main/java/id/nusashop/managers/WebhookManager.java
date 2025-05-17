package id.nusashop.managers;

import id.nusashop.NusaShop;
import id.nusashop.models.Category;
import id.nusashop.models.ShopItem;
import org.bukkit.ChatColor;
import org.bukkit.configuration.ConfigurationSection;

import javax.net.ssl.HttpsURLConnection;
import java.awt.Color;
import java.io.IOException;
import java.io.OutputStream;
import java.net.URL;
import java.nio.charset.StandardCharsets;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.concurrent.CompletableFuture;
import java.util.logging.Level;

/**
 * Manager untuk mengelola Discord webhook
 */
public class WebhookManager {
    private final NusaShop plugin;
    private String eventWebhookUrl;
    private String blackmarketWebhookUrl;
    private boolean enabled;
    
    public WebhookManager(NusaShop plugin) {
        this.plugin = plugin;
        loadConfig();
    }
    
    /**
     * Memuat konfigurasi webhook dari config
     */
    public void loadConfig() {
        enabled = plugin.getConfigManager().getConfig().getBoolean("discord.enabled", false);
        eventWebhookUrl = plugin.getConfigManager().getConfig().getString("discord.event-webhook-url", "");
        blackmarketWebhookUrl = plugin.getConfigManager().getConfig().getString("discord.blackmarket-webhook-url", "");
    }
    
    /**
     * Mengirim notifikasi event ke Discord
     * 
     * @param eventName Nama event
     * @param description Deskripsi event
     * @param startTime Waktu mulai event
     * @param endTime Waktu berakhir event
     * @param sellMultiplier Pengali harga jual
     * @param categorySellMultipliers Pengali harga jual per kategori
     */
    public void sendEventNotification(String eventName, String description, LocalDateTime startTime, 
                                    LocalDateTime endTime, double sellMultiplier, 
                                    Map<String, Double> categorySellMultipliers) {
        if (!enabled || eventWebhookUrl.isEmpty()) return;
        
        // Format waktu
        DateTimeFormatter formatter = DateTimeFormatter.ofPattern("dd MMM yyyy, HH:mm");
        String formattedStartTime = startTime.format(formatter);
        String formattedEndTime = endTime.format(formatter);
        
        // Buat JSON untuk webhook
        StringBuilder json = new StringBuilder();
        json.append("{");
        json.append("\"embeds\": [{");
        
        // Embed title and description
        json.append("\"title\":\"").append(escapeJson(eventName)).append("\",");
        json.append("\"description\":\"").append(escapeJson(description)).append("\",");
        json.append("\"color\":").append(convertColor(Color.ORANGE)).append(",");
        
        // Event details
        json.append("\"fields\":[");
        
        // Duration field
        json.append("{");
        json.append("\"name\":\"⏱️ Durasi\",");
        json.append("\"value\":\"Mulai: ").append(escapeJson(formattedStartTime)).
                append("\\nBerakhir: ").append(escapeJson(formattedEndTime)).append("\",");
        json.append("\"inline\":true");
        json.append("},");
        
        // Multiplier field
        json.append("{");
        json.append("\"name\":\"💰 Bonus Penjualan\",");
        json.append("\"value\":\"");
        if (sellMultiplier > 1.0) {
            int bonus = (int)((sellMultiplier - 1.0) * 100);
            json.append("Global: +").append(bonus).append("%\\n");
        } else {
            json.append("Global: Normal\\n");
        }
        
        // Add category-specific bonuses
        if (!categorySellMultipliers.isEmpty()) {
            for (Map.Entry<String, Double> entry : categorySellMultipliers.entrySet()) {
                if (entry.getValue() > 1.0) {
                    Category category = plugin.getShopManager().getCategory(entry.getKey());
                    String categoryName = category != null ? 
                        ChatColor.stripColor(ChatColor.translateAlternateColorCodes('&', category.getName())) : 
                        entry.getKey();
                    
                    int bonus = (int)((entry.getValue() - 1.0) * 100);
                    json.append(escapeJson(categoryName)).append(": +").append(bonus).append("%\\n");
                }
            }
        }
        
        json.append("\",");
        json.append("\"inline\":true");
        json.append("}");
        
        // Close fields array
        json.append("],");
        
        // Footer
        json.append("\"footer\":{");
        json.append("\"text\":\"NusaShop Event System\"");
        json.append("}");
        
        // Close embed and root objects
        json.append("}]}");
        
        // Send webhook asynchronously
        sendWebhookAsync(eventWebhookUrl, json.toString());
    }
    
    /**
     * Mengirim notifikasi blackmarket ke Discord
     * 
     * @param isOpening true jika buka, false jika tutup
     * @param openTime Waktu buka (jika isOpening=true)
     * @param closeTime Waktu tutup (jika isOpening=true)
     * @param featuredItems Item unggulan untuk ditampilkan (max 3-5 item)
     */
    public void sendBlackmarketNotification(boolean isOpening, String openTime, String closeTime,
                                          List<Map<String, Object>> featuredItems) {
        if (!enabled || blackmarketWebhookUrl.isEmpty()) return;
        
        // Buat JSON untuk webhook
        StringBuilder json = new StringBuilder();
        json.append("{");
        json.append("\"embeds\": [{");
        
        // Embed title and description
        String title = isOpening ? "🏴‍☠️ Pasar Gelap Telah Dibuka!" : "🏴‍☠️ Pasar Gelap Telah Ditutup!";
        String description = isOpening ? 
            "Pasar gelap sekarang terbuka! Kunjungi untuk mendapatkan barang-barang langka dan eksklusif." : 
            "Pasar gelap telah ditutup. Datang lagi besok untuk kesempatan mendapatkan barang langka dan ekslusif.";
            
        json.append("\"title\":\"").append(title).append("\",");
        json.append("\"description\":\"").append(escapeJson(description)).append("\",");
        json.append("\"color\":").append(convertColor(new Color(75, 0, 130))).append(","); // Deep Purple
        
        if (isOpening) {
            // Add timing information
            json.append("\"fields\":[");
            json.append("{");
            json.append("\"name\":\"⏱️ Jadwal\",");
            json.append("\"value\":\"Buka: ").append(escapeJson(openTime)).
                    append("\\nTutup: ").append(escapeJson(closeTime)).append("\",");
            json.append("\"inline\":false");
            json.append("}");
            
            // Add featured items if available
            if (featuredItems != null && !featuredItems.isEmpty()) {
                json.append(",{");
                json.append("\"name\":\"✨ Barang Unggulan\",");
                json.append("\"value\":\"");
                
                int itemCount = 0;
                for (Map<String, Object> item : featuredItems) {
                    if (itemCount >= 5) break; // Show max 5 items
                    
                    String name = (String) item.get("name");
                    double price = (Double) item.get("price");
                    
                    json.append("• **").append(escapeJson(name)).append("** — ");
                    json.append("💰 ").append(String.format("%.2f", price)).append(" coins\\n");
                    
                    itemCount++;
                }
                
                json.append("\",");
                json.append("\"inline\":false");
                json.append("}");
            }
            
            // Add instruction
            json.append(",{");
            json.append("\"name\":\"📋 Cara Mengakses\",");
            json.append("\"value\":\"Ketik `/blackmarket` di server untuk mengakses pasar gelap!\",");
            json.append("\"inline\":false");
            json.append("}");
            
            // Close fields array
            json.append("]");
        }
        
        // Footer
        json.append(",\"footer\":{");
        json.append("\"text\":\"NusaShop Blackmarket System\"");
        json.append("}");
        
        // Close embed and root objects
        json.append("}]}");
        
        // Send webhook asynchronously
        sendWebhookAsync(blackmarketWebhookUrl, json.toString());
    }
    
    /**
     * Mengirim notifikasi blackmarket closing warning ke Discord
     * 
     * @param minutesRemaining Menit tersisa sebelum blackmarket tutup
     */
    public void sendBlackmarketClosingWarning(int minutesRemaining) {
        if (!enabled || blackmarketWebhookUrl.isEmpty()) return;
        
        // Buat JSON untuk webhook
        StringBuilder json = new StringBuilder();
        json.append("{");
        json.append("\"embeds\": [{");
        
        // Embed title and description
        json.append("\"title\":\"⚠️ Pasar Gelap Akan Segera Tutup!\",");
        json.append("\"description\":\"Pasar gelap akan tutup dalam **").append(minutesRemaining)
                .append(" menit**! Segeralah berkunjung sebelum barang yang tersedia menghilang.\",");
        json.append("\"color\":").append(convertColor(Color.ORANGE)).append(",");
        
        // Instruction
        json.append("\"fields\":[{");
        json.append("\"name\":\"📋 Cara Mengakses\",");
        json.append("\"value\":\"Ketik `/blackmarket` di server untuk mengakses pasar gelap!\",");
        json.append("\"inline\":false");
        json.append("}]");
        
        // Footer
        json.append(",\"footer\":{");
        json.append("\"text\":\"NusaShop Blackmarket System\"");
        json.append("}");
        
        // Close embed and root objects
        json.append("}]}");
        
        // Send webhook asynchronously
        sendWebhookAsync(blackmarketWebhookUrl, json.toString());
    }
    
    /**
     * Mengirim webhook secara asynchronous
     * 
     * @param webhookUrl URL webhook Discord
     * @param jsonBody Body JSON untuk webhook
     */
    private void sendWebhookAsync(String webhookUrl, String jsonBody) {
        CompletableFuture.runAsync(() -> {
            try {
                URL url = new URL(webhookUrl);
                HttpsURLConnection connection = (HttpsURLConnection) url.openConnection();
                connection.setRequestMethod("POST");
                connection.setRequestProperty("Content-Type", "application/json");
                connection.setRequestProperty("User-Agent", "NusaShop Discord Webhook");
                connection.setDoOutput(true);
                
                try (OutputStream outputStream = connection.getOutputStream()) {
                    outputStream.write(jsonBody.getBytes(StandardCharsets.UTF_8));
                }
                
                int responseCode = connection.getResponseCode();
                if (responseCode != 204) {
                    plugin.getLogger().warning("Failed to send Discord webhook: HTTP " + responseCode);
                }
            } catch (IOException e) {
                plugin.getLogger().log(Level.WARNING, "Error sending Discord webhook", e);
            }
        });
    }
    
    /**
     * Escape special characters in JSON strings
     * 
     * @param input Input string
     * @return Escaped string
     */
    private String escapeJson(String input) {
        if (input == null) return "";
        
        return input.replace("\\", "\\\\")
                   .replace("\"", "\\\"")
                   .replace("\n", "\\n")
                   .replace("\r", "\\r")
                   .replace("\t", "\\t");
    }
    
    /**
     * Convert Java Color to Discord color integer
     * 
     * @param color Java Color object
     * @return Discord color integer
     */
    private int convertColor(Color color) {
        return (color.getRed() << 16) + (color.getGreen() << 8) + color.getBlue();
    }
}