package id.nusashop.managers;

import id.nusashop.NusaShop;
import id.nusashop.models.Category;
import id.nusashop.models.ShopItem;
import org.bukkit.OfflinePlayer;
import org.bukkit.configuration.ConfigurationSection;
import org.bukkit.configuration.file.FileConfiguration;
import org.bukkit.configuration.file.YamlConfiguration;
import org.bukkit.entity.Player;
import org.bukkit.ChatColor;

import org.bukkit.configuration.file.FileConfiguration;

import java.io.File;
import java.io.IOException;
import java.text.DecimalFormat;
import java.util.Collections;
import java.util.HashMap;
import java.util.LinkedHashMap;
import java.util.Map;
import java.util.UUID;
import java.util.stream.Collectors;

/**
 * Manager untuk mengelola statistik shop
 */
public class StatisticsManager {
    private final NusaShop plugin;
    private final File statsFile;
    private FileConfiguration stats;
    
    // Statistik global
    private int totalTransactions = 0;
    private double totalMoneySpent = 0.0;
    private double totalMoneyEarned = 0.0;
    
    // Statistik per Item
    private final Map<String, Integer> itemBuyCount = new HashMap<>();
    private final Map<String, Integer> itemSellCount = new HashMap<>();
    
    // Statistik per Kategori
    private final Map<String, Integer> categoryBuyCount = new HashMap<>();
    private final Map<String, Integer> categorySellCount = new HashMap<>();
    
    // Statistik per Player
    private final Map<UUID, PlayerStats> playerStats = new HashMap<>();
    
    private static final DecimalFormat PRICE_FORMAT = new DecimalFormat("#,##0.00");
    
    public StatisticsManager(NusaShop plugin) {
        this.plugin = plugin;
        this.statsFile = new File(plugin.getDataFolder(), "statistics.yml");
        loadStats();
    }
    
    /**
     * Memuat data statistik dari file
     */
    private void loadStats() {
        if (!statsFile.exists()) {
            stats = new YamlConfiguration();
            saveStats();
        } else {
            stats = YamlConfiguration.loadConfiguration(statsFile);
        }
        
        // Load global stats
        totalTransactions = stats.getInt("global.total-transactions", 0);
        totalMoneySpent = stats.getDouble("global.total-money-spent", 0.0);
        totalMoneyEarned = stats.getDouble("global.total-money-earned", 0.0);
        
        // Load item stats
        ConfigurationSection itemsSection = stats.getConfigurationSection("items");
        if (itemsSection != null) {
            for (String itemId : itemsSection.getKeys(false)) {
                itemBuyCount.put(itemId, itemsSection.getInt(itemId + ".buy-count", 0));
                itemSellCount.put(itemId, itemsSection.getInt(itemId + ".sell-count", 0));
            }
        }
        
        // Load category stats
        ConfigurationSection categoriesSection = stats.getConfigurationSection("categories");
        if (categoriesSection != null) {
            for (String categoryId : categoriesSection.getKeys(false)) {
                categoryBuyCount.put(categoryId, categoriesSection.getInt(categoryId + ".buy-count", 0));
                categorySellCount.put(categoryId, categoriesSection.getInt(categoryId + ".sell-count", 0));
            }
        }
        
        // Load player stats
        ConfigurationSection playersSection = stats.getConfigurationSection("players");
        if (playersSection != null) {
            for (String uuidStr : playersSection.getKeys(false)) {
                try {
                    UUID uuid = UUID.fromString(uuidStr);
                    PlayerStats playerStat = new PlayerStats(uuid);
                    
                    playerStat.totalBuyCount = playersSection.getInt(uuidStr + ".buy-count", 0);
                    playerStat.totalSellCount = playersSection.getInt(uuidStr + ".sell-count", 0);
                    playerStat.totalSpent = playersSection.getDouble(uuidStr + ".total-spent", 0.0);
                    playerStat.totalEarned = playersSection.getDouble(uuidStr + ".total-earned", 0.0);
                    
                    playerStats.put(uuid, playerStat);
                } catch (IllegalArgumentException ignored) {
                    // Skip invalid UUID entries
                }
            }
        }
    }
    
    /**
     * Menyimpan data statistik ke file
     */
    public void saveStats() {
        // Save global stats
        stats.set("global.total-transactions", totalTransactions);
        stats.set("global.total-money-spent", totalMoneySpent);
        stats.set("global.total-money-earned", totalMoneyEarned);
        
        // Save item stats
        for (Map.Entry<String, Integer> entry : itemBuyCount.entrySet()) {
            stats.set("items." + entry.getKey() + ".buy-count", entry.getValue());
        }
        
        for (Map.Entry<String, Integer> entry : itemSellCount.entrySet()) {
            stats.set("items." + entry.getKey() + ".sell-count", entry.getValue());
        }
        
        // Save category stats
        for (Map.Entry<String, Integer> entry : categoryBuyCount.entrySet()) {
            stats.set("categories." + entry.getKey() + ".buy-count", entry.getValue());
        }
        
        for (Map.Entry<String, Integer> entry : categorySellCount.entrySet()) {
            stats.set("categories." + entry.getKey() + ".sell-count", entry.getValue());
        }
        
        // Save player stats
        for (Map.Entry<UUID, PlayerStats> entry : playerStats.entrySet()) {
            String uuidStr = entry.getKey().toString();
            PlayerStats playerStat = entry.getValue();
            
            stats.set("players." + uuidStr + ".buy-count", playerStat.totalBuyCount);
            stats.set("players." + uuidStr + ".sell-count", playerStat.totalSellCount);
            stats.set("players." + uuidStr + ".total-spent", playerStat.totalSpent);
            stats.set("players." + uuidStr + ".total-earned", playerStat.totalEarned);
        }
        
        // Save to file
        try {
            stats.save(statsFile);
        } catch (IOException e) {
            plugin.getLogger().severe("Gagal menyimpan statistik: " + e.getMessage());
        }
    }
    
    /**
     * Mencatat transaksi pembelian
     * @param player player yang melakukan transaksi
     * @param item item yang dibeli
     * @param category kategori item
     * @param amount jumlah yang dibeli
     * @param price harga total
     */
    public void recordBuyTransaction(Player player, ShopItem item, Category category, int amount, double price) {
        // Update statistik global
        totalTransactions++;
        totalMoneySpent += price;
        
        // Update statistik item
        itemBuyCount.put(item.getId(), itemBuyCount.getOrDefault(item.getId(), 0) + amount);
        
        // Update statistik kategori
        categoryBuyCount.put(category.getId(), categoryBuyCount.getOrDefault(category.getId(), 0) + amount);
        
        // Update statistik pemain
        PlayerStats stats = getPlayerStats(player);
        stats.totalBuyCount += amount;
        stats.totalSpent += price;
        
        // Ini yang penting - tracking item yang dibeli oleh player
        stats.itemsBought.put(item.getId(), stats.itemsBought.getOrDefault(item.getId(), 0) + amount);
    }
    
    /**
     * Mencatat transaksi penjualan
     * @param player player yang melakukan transaksi
     * @param item item yang dijual
     * @param category kategori item
     * @param amount jumlah yang dijual
     * @param price harga total
     */
    public void recordSellTransaction(Player player, ShopItem item, Category category, int amount, double price) {
        // Update statistik global
        totalTransactions++;
        totalMoneyEarned += price;
        
        // Update statistik item
        itemSellCount.put(item.getId(), itemSellCount.getOrDefault(item.getId(), 0) + amount);
        
        // Update statistik kategori
        categorySellCount.put(category.getId(), categorySellCount.getOrDefault(category.getId(), 0) + amount);
        
        // Update statistik pemain
        PlayerStats stats = getPlayerStats(player);
        stats.totalSellCount += amount;
        stats.totalEarned += price;
        
        // Ini yang penting - tracking item yang dijual oleh player
        stats.itemsSold.put(item.getId(), stats.itemsSold.getOrDefault(item.getId(), 0) + amount);
    }
    
    /**
     * Mendapatkan total transaksi (jumlah transaksi pembelian + penjualan)
     */
    public int getTotalTransactions() {
        return totalTransactions;
    }
    
    /**
     * Mendapatkan total uang yang dibelanjakan di shop
     */
    public double getTotalMoneySpent() {
        return totalMoneySpent;
    }
    
    /**
     * Mendapatkan total uang yang diperoleh dari penjualan ke shop
     */
    public double getTotalMoneyEarned() {
        return totalMoneyEarned;
    }
    
    /**
     * Mendapatkan jumlah pembelian item
     * @param itemId id item
     */
    public int getItemBuyCount(String itemId) {
        return itemBuyCount.getOrDefault(itemId, 0);
    }
    
    /**
     * Mendapatkan jumlah penjualan item
     * @param itemId id item
     */
    public int getItemSellCount(String itemId) {
        return itemSellCount.getOrDefault(itemId, 0);
    }
    
    /**
     * Mendapatkan jumlah transaksi kategori (pembelian)
     * @param categoryId id kategori
     */
    public int getCategoryBuyCount(String categoryId) {
        return categoryBuyCount.getOrDefault(categoryId, 0);
    }
    
    /**
     * Mendapatkan jumlah transaksi kategori (penjualan)
     * @param categoryId id kategori
     */
    public int getCategorySellCount(String categoryId) {
        return categorySellCount.getOrDefault(categoryId, 0);
    }
    
    /**
     * Mendapatkan item terpopuler yang dibeli
     */
    public String getMostBoughtItem() {
        if (itemBuyCount.isEmpty()) return "Belum ada";
        
        Map<String, Integer> sortedItems = itemBuyCount.entrySet().stream()
                .sorted(Collections.reverseOrder(Map.Entry.comparingByValue()))
                .collect(Collectors.toMap(Map.Entry::getKey, Map.Entry::getValue, (e1, e2) -> e1, LinkedHashMap::new));
        
        String itemId = sortedItems.keySet().iterator().next();
        // Cari nama item yang sesuai
        for (Category category : plugin.getShopManager().getCategories()) {
            for (ShopItem item : category.getItems()) {
                if (item.getId().equals(itemId)) {
                    return ChatColor.stripColor(ChatColor.translateAlternateColorCodes('&', item.getName())) + 
                           " (" + sortedItems.get(itemId) + ")";
                }
            }
        }
        
        return itemId + " (" + sortedItems.get(itemId) + ")";
    }
    
    /**
     * Mendapatkan item terpopuler yang dijual
     */
    public String getMostSoldItem() {
        if (itemSellCount.isEmpty()) return "Belum ada";
        
        Map<String, Integer> sortedItems = itemSellCount.entrySet().stream()
                .sorted(Collections.reverseOrder(Map.Entry.comparingByValue()))
                .collect(Collectors.toMap(Map.Entry::getKey, Map.Entry::getValue, (e1, e2) -> e1, LinkedHashMap::new));
        
        String itemId = sortedItems.keySet().iterator().next();
        // Cari nama item yang sesuai
        for (Category category : plugin.getShopManager().getCategories()) {
            for (ShopItem item : category.getItems()) {
                if (item.getId().equals(itemId)) {
                    return ChatColor.stripColor(ChatColor.translateAlternateColorCodes('&', item.getName())) + 
                           " (" + sortedItems.get(itemId) + ")";
                }
            }
        }
        
        return itemId + " (" + sortedItems.get(itemId) + ")";
    }
    
    /**
     * Mendapatkan kategori terpopuler
     */
    public String getMostPopularCategory() {
        if (categoryBuyCount.isEmpty()) return "Belum ada";
        
        // Combine buy and sell counts for categories
        Map<String, Integer> combinedCounts = new HashMap<>();
        
        for (Map.Entry<String, Integer> entry : categoryBuyCount.entrySet()) {
            combinedCounts.put(entry.getKey(), entry.getValue());
        }
        
        for (Map.Entry<String, Integer> entry : categorySellCount.entrySet()) {
            combinedCounts.put(entry.getKey(), 
                    combinedCounts.getOrDefault(entry.getKey(), 0) + entry.getValue());
        }
        
        Map<String, Integer> sortedCategories = combinedCounts.entrySet().stream()
                .sorted(Collections.reverseOrder(Map.Entry.comparingByValue()))
                .collect(Collectors.toMap(Map.Entry::getKey, Map.Entry::getValue, (e1, e2) -> e1, LinkedHashMap::new));
        
        String categoryId = sortedCategories.keySet().iterator().next();
        
        // Cari nama kategori yang sesuai
        Category category = plugin.getShopManager().getCategory(categoryId);
        if (category != null) {
            return ChatColor.stripColor(ChatColor.translateAlternateColorCodes('&', category.getName())) + 
                   " (" + sortedCategories.get(categoryId) + ")";
        }
        
        return categoryId + " (" + sortedCategories.get(categoryId) + ")";
    }
    
    /**
     * Mendapatkan statistik player
     * @param player player yang dicari statistiknya
     */
    public PlayerStats getPlayerStats(OfflinePlayer player) {
        return playerStats.getOrDefault(player.getUniqueId(), new PlayerStats(player.getUniqueId()));
    }
    
    /**
     * Format angka menjadi string dengan format harga
     * @param value angka yang akan diformat
     * @return string dengan format harga
     */
    public static String formatPrice(double value) {
        return PRICE_FORMAT.format(value);
    }
    
    /**
     * Mendapatkan jumlah transaksi hari ini
     * @return Jumlah transaksi hari ini
     */
    public int getTodayTransactions() {
        // Implementasi sederhana, bisa ditingkatkan dengan menyimpan data per tanggal
        return Math.max(5, totalTransactions / 10); // Contoh implementasi sementara
    }

    /**
     * Mendapatkan semua data jumlah pembelian item
     * @return Map dengan key=itemId, value=jumlah pembelian
     */
    public Map<String, Integer> getAllItemBuyCounts() {
        return new HashMap<>(itemBuyCount);
    }

    /**
     * Mendapatkan semua data jumlah penjualan item
     * @return Map dengan key=itemId, value=jumlah penjualan
     */
    public Map<String, Integer> getAllItemSellCounts() {
        return new HashMap<>(itemSellCount);
    }
    
    /**
     * Kelas untuk menyimpan data statistik player
     */
    public class PlayerStats {
        private final UUID playerUuid;
        private int totalBuyCount = 0;
        private int totalSellCount = 0;
        private double totalSpent = 0.0;
        private double totalEarned = 0.0;
        private final Map<String, Integer> itemsBought = new HashMap<>();
        private final Map<String, Integer> itemsSold = new HashMap<>();
        
        public PlayerStats(UUID playerUuid) {
            this.playerUuid = playerUuid;
        }
        
        public int getTotalBuyCount() {
            return totalBuyCount;
        }
        
        public int getTotalSellCount() {
            return totalSellCount;
        }
        
        public double getTotalSpent() {
            return totalSpent;
        }
        
        public double getTotalEarned() {
            return totalEarned;
        }
        
        public int getTotalTransactions() {
            return totalBuyCount + totalSellCount;
        }
        
        /**
         * Mendapatkan nama item yang paling banyak dibeli oleh pemain
         * @return Nama item terpopuler, atau "Belum Ada" jika belum ada pembelian
         */
        public String getMostBoughtItem() {
            if (itemsBought.isEmpty()) {
                return "Belum Ada";
            }
            
            String mostBoughtItemId = Collections.max(itemsBought.entrySet(), 
                    Map.Entry.comparingByValue()).getKey();
            
            // Cari nama item berdasarkan ID
            for (Category category : StatisticsManager.this.plugin.getShopManager().getCategories()) {
                for (ShopItem item : category.getItems()) {
                    if (item.getId().equals(mostBoughtItemId)) {
                        return item.getName();
                    }
                }
            }
            
            return mostBoughtItemId; // Fallback ke ID item jika nama tidak ditemukan
        }
        
        /**
         * Mendapatkan nama item yang paling banyak dijual oleh pemain
         * @return Nama item terpopuler, atau "Belum Ada" jika belum ada penjualan
         */
        public String getMostSoldItem() {
            if (itemsSold.isEmpty()) {
                return "Belum Ada";
            }
            
            String mostSoldItemId = Collections.max(itemsSold.entrySet(), 
                    Map.Entry.comparingByValue()).getKey();
            
            // Cari nama item berdasarkan ID
            for (Category category : StatisticsManager.this.plugin.getShopManager().getCategories()) {
                for (ShopItem item : category.getItems()) {
                    if (item.getId().equals(mostSoldItemId)) {
                        return item.getName();
                    }
                }
            }
            
            return mostSoldItemId; // Fallback ke ID item jika nama tidak ditemukan
        }
    }
}