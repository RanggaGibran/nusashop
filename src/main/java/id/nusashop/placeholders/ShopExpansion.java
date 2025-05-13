package id.nusashop.placeholders;

import id.nusashop.NusaShop;
import id.nusashop.managers.StatisticsManager;
import id.nusashop.managers.StatisticsManager.PlayerStats;
import id.nusashop.models.Category;
import id.nusashop.models.ShopItem;
import id.nusashop.utils.NumberFormatter;

import org.bukkit.OfflinePlayer;

import me.clip.placeholderapi.expansion.PlaceholderExpansion;

import java.util.ArrayList;
import java.util.Collections;
import java.util.Comparator;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Map.Entry;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;

/**
 * Ekspansi PlaceholderAPI untuk NusaShop
 */
public class ShopExpansion extends PlaceholderExpansion {
    private final NusaShop plugin;
    
    public ShopExpansion(NusaShop plugin) {
        this.plugin = plugin;
    }
    
    @Override
    public String getIdentifier() {
        return "nusashop";
    }
    
    @Override
    public String getAuthor() {
        return "NusaTown Dev";
    }
    
    @Override
    public String getVersion() {
        return plugin.getDescription().getVersion();
    }
    
    @Override
    public boolean persist() {
        // Ini memastikan kita tidak perlu register ulang saat reload
        return true;
    }
    
    @Override
    public String onRequest(OfflinePlayer player, String identifier) {
        StatisticsManager statsManager = plugin.getStatisticsManager();
        
        // Global placeholders dengan format compact
        if (identifier.equals("total_transactions")) {
            return NumberFormatter.formatCompact(statsManager.getTotalTransactions());
        }
        
        if (identifier.equals("total_money_spent")) {
            double amount = statsManager.getTotalMoneySpent();
            String currencySymbol = plugin.getConfigManager().getConfig().getString("economy.symbol", "$");
            return NumberFormatter.formatCompactPrice(amount, currencySymbol);
        }
        
        if (identifier.equals("total_money_earned")) {
            double amount = statsManager.getTotalMoneyEarned();
            String currencySymbol = plugin.getConfigManager().getConfig().getString("economy.symbol", "$");
            return NumberFormatter.formatCompactPrice(amount, currencySymbol);
        }
        
        if (identifier.equals("most_bought_item")) {
            return statsManager.getMostBoughtItem();
        }
        
        if (identifier.equals("most_sold_item")) {
            return statsManager.getMostSoldItem();
        }
        
        if (identifier.equals("most_popular_category")) {
            return statsManager.getMostPopularCategory();
        }
        
        // Server time placeholder
        if (identifier.equals("server_time")) {
            LocalDateTime now = LocalDateTime.now();
            return now.format(DateTimeFormatter.ofPattern("HH:mm:ss"));
        }
        
        // Placeholder untuk transaksi hari ini dengan format compact
        if (identifier.equals("today_transactions")) {
            return NumberFormatter.formatCompact(statsManager.getTodayTransactions());
        }
        
        // Placeholder untuk item populer dengan ranking (top 5)
        if (identifier.startsWith("popular_item_")) {
            try {
                int rank = Integer.parseInt(identifier.substring(13));
                if (rank > 0 && rank <= 5) {
                    List<Entry<String, Integer>> popularItems = getTopItems(statsManager);
                    if (popularItems.size() >= rank) {
                        String itemId = popularItems.get(rank - 1).getKey();
                        ShopItem item = findItemById(itemId);
                        return item != null ? item.getName() : "Item #" + rank;
                    }
                }
                return "-";
            } catch (NumberFormatException e) {
                return "Format Error";
            }
        }
        
        // Format compact untuk jumlah item populer
        if (identifier.startsWith("popular_item_") && identifier.endsWith("_count")) {
            try {
                // Contoh: popular_item_1_count
                int rank = Integer.parseInt(identifier.substring(13, identifier.length() - 6));
                if (rank > 0 && rank <= 5) {
                    List<Entry<String, Integer>> popularItems = getTopItems(statsManager);
                    if (popularItems.size() >= rank) {
                        int count = popularItems.get(rank - 1).getValue();
                        return NumberFormatter.formatCompact(count);
                    }
                }
                return "0";
            } catch (NumberFormatException | IndexOutOfBoundsException e) {
                return "0";
            }
        }
        
        // Placeholder untuk kategori
        if (identifier.startsWith("category_item_count_")) {
            String categoryId = identifier.substring("category_item_count_".length());
            Category category = plugin.getShopManager().getCategory(categoryId);
            if (category != null) {
                return String.valueOf(category.getItems().size());
            }
            return "0";
        }
        
        // Placeholder dengan format compact untuk kategori
        if (identifier.startsWith("category_buy_count_")) {
            String categoryId = identifier.substring("category_buy_count_".length());
            int count = statsManager.getCategoryBuyCount(categoryId);
            return NumberFormatter.formatCompact(count);
        }
        
        // Placeholder untuk harga dan stok item (jika menggunakan sistem stok)
        if (identifier.startsWith("item_buy_price_") || 
            identifier.startsWith("item_sell_price_")) {
            
            String prefix = identifier.startsWith("item_buy_price_") ? "item_buy_price_" : "item_sell_price_";
            String itemId = identifier.substring(prefix.length());
            ShopItem item = findItemById(itemId);
            
            if (item != null) {
                if (prefix.equals("item_buy_price_")) {
                    return StatisticsManager.formatPrice(item.getBuyPrice());
                } else {
                    return StatisticsManager.formatPrice(item.getSellPrice());
                }
            }
            return "0.00";
        }
        
        // Placeholder untuk nama item berdasarkan ID 
        if (identifier.startsWith("item_name_")) {
            String itemId = identifier.substring("item_name_".length());
            ShopItem item = findItemById(itemId);
            return item != null ? item.getName() : "Unknown Item";
        }
        
        // Placeholder untuk event diskon (jika ada sistem diskon)
        if (identifier.equals("event_end_date")) {
            // Jika ada sistem diskon, return tanggal berakhirnya
            // Sementara kita return tanggal default
            return "30/04/2025";
        }
        
        if (identifier.startsWith("discount_item_")) {
            try {
                int index = Integer.parseInt(identifier.substring("discount_item_".length()));
                // Implementasikan jika ada sistem diskon
                // Sementara kita return placeholder data
                switch (index) {
                    case 1: return "Diamond Pickaxe";
                    case 2: return "Golden Apple";
                    case 3: return "Elytra";
                    default: return "Unknown Item";
                }
            } catch (NumberFormatException e) {
                return "Error";
            }
        }
        
        if (identifier.startsWith("discount_percent_")) {
            try {
                int index = Integer.parseInt(identifier.substring("discount_percent_".length()));
                // Implementasikan jika ada sistem diskon
                // Sementara kita return placeholder data
                switch (index) {
                    case 1: return "20";
                    case 2: return "15";
                    case 3: return "30";
                    default: return "0";
                }
            } catch (NumberFormatException e) {
                return "0";
            }
        }
        
        // Tambahkan placeholder untuk event

        // Placeholders untuk event
        if (identifier.equals("has_active_event")) {
            return plugin.getEventManager().hasActiveEvent() ? "true" : "false";
        }

        if (identifier.equals("event_name")) {
            return plugin.getEventManager().hasActiveEvent() ? 
                    plugin.getEventManager().getEventName() : "Tidak ada event";
        }

        if (identifier.equals("event_description")) {
            return plugin.getEventManager().hasActiveEvent() ? 
                    plugin.getEventManager().getEventDescription() : "";
        }

        if (identifier.equals("event_end_time")) {
            return plugin.getEventManager().hasActiveEvent() ? 
                    plugin.getEventManager().getFormattedEndTime() : "";
        }

        if (identifier.equals("event_remaining_time")) {
            return plugin.getEventManager().hasActiveEvent() ? 
                    plugin.getEventManager().getRemainingTimeFormatted() : "";
        }
        
        // Format non-compact (original) placeholders
        if (identifier.equals("raw_total_transactions")) {
            return String.valueOf(statsManager.getTotalTransactions());
        }

        if (identifier.equals("raw_total_money_spent")) {
            return StatisticsManager.formatPrice(statsManager.getTotalMoneySpent());
        }

        if (identifier.equals("raw_total_money_earned")) {
            return StatisticsManager.formatPrice(statsManager.getTotalMoneyEarned());
        }

        // Format compact (dengan suffix _formatted) placeholders
        if (identifier.equals("total_transactions_formatted")) {
            return NumberFormatter.formatCompact(statsManager.getTotalTransactions());
        }

        if (identifier.equals("total_money_spent_formatted")) {
            double amount = statsManager.getTotalMoneySpent();
            String currencySymbol = plugin.getConfigManager().getConfig().getString("economy.symbol", "$");
            return NumberFormatter.formatCompactPrice(amount, currencySymbol);
        }
        
        // Player-specific placeholders dengan format compact
        if (player != null) {
            PlayerStats playerStats = statsManager.getPlayerStats(player);
            
            if (identifier.equals("player_buy_count")) {
                return NumberFormatter.formatCompact(playerStats.getTotalBuyCount());
            }
            
            if (identifier.equals("player_sell_count")) {
                return NumberFormatter.formatCompact(playerStats.getTotalSellCount());
            }
            
            if (identifier.equals("player_total_spent")) {
                double amount = playerStats.getTotalSpent();
                String currencySymbol = plugin.getConfigManager().getConfig().getString("economy.symbol", "$");
                return NumberFormatter.formatCompactPrice(amount, currencySymbol);
            }
            
            if (identifier.equals("player_total_earned")) {
                double amount = playerStats.getTotalEarned();
                String currencySymbol = plugin.getConfigManager().getConfig().getString("economy.symbol", "$");
                return NumberFormatter.formatCompactPrice(amount, currencySymbol);
            }
            
            if (identifier.equals("player_total_transactions")) {
                return NumberFormatter.formatCompact(playerStats.getTotalTransactions());
            }
            
            // Placeholder untuk item yang paling sering dibeli oleh player
            if (identifier.equals("player_most_bought_item")) {
                return playerStats.getMostBoughtItem();
            }
        }
        
        return null; // Placeholder yang tidak dikenal
    }
    
    /**
     * Mendapatkan daftar item terpopuler berdasarkan jumlah pembelian
     * @param statsManager Manager statistik
     * @return List item terurut berdasarkan popularitas
     */
    private List<Map.Entry<String, Integer>> getTopItems(StatisticsManager statsManager) {
        Map<String, Integer> itemBuyCount = statsManager.getAllItemBuyCounts();
        List<Map.Entry<String, Integer>> sortedItems = new ArrayList<>(itemBuyCount.entrySet());
        
        // Urutkan berdasarkan jumlah pembelian (dari tertinggi ke terendah)
        Collections.sort(sortedItems, (e1, e2) -> e2.getValue().compareTo(e1.getValue()));
        
        return sortedItems;
    }
    
    /**
     * Mencari item shop berdasarkan ID
     * @param itemId ID item yang dicari
     * @return ShopItem atau null jika tidak ditemukan
     */
    private ShopItem findItemById(String itemId) {
        for (Category category : plugin.getShopManager().getCategories()) {
            for (ShopItem item : category.getItems()) {
                if (item.getId().equals(itemId)) {
                    return item;
                }
            }
        }
        return null;
    }
}