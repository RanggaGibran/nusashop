package id.nusashop.commands;

import id.nusashop.NusaShop;
import id.nusashop.gui.admin.AdminShopGUI;
import id.nusashop.models.Category;
import id.nusashop.models.ShopItem;
import id.nusashop.utils.Messages;
import id.nusashop.models.BlackmarketItem;
import id.nusashop.managers.BlackmarketManager;

import org.bukkit.ChatColor;
import org.bukkit.command.Command;
import org.bukkit.command.CommandExecutor;
import org.bukkit.command.CommandSender;
import org.bukkit.entity.Player;

import java.util.List;
import java.util.Map;

/**
 * Command handler untuk perintah admin
 */
public class AdminCommand implements CommandExecutor {
    private final NusaShop plugin;
    
    public AdminCommand(NusaShop plugin) {
        this.plugin = plugin;
    }
    
    @Override
    public boolean onCommand(CommandSender sender, Command command, String label, String[] args) {
        // Hanya player dengan izin yang bisa menggunakan perintah ini
        if (!(sender instanceof Player player)) {
            sender.sendMessage(Messages.get("general.player-only"));
            return true;
        }
        
        if (!player.hasPermission("nusashop.admin")) {
            Messages.send(player, "general.no-permission");
            return true;
        }
        
        if (args.length == 0) {
            // Buka GUI admin
            new AdminShopGUI(plugin, player).open();
            return true;
        }
        
        switch (args[0].toLowerCase()) {
            case "reload" -> {
                // Use the comprehensive reload method
                plugin.reloadPlugin();
                Messages.send(player, "admin.reload");
            }
            case "help" -> {
                sendHelp(player);
            }
            case "checkevent" -> {
                if (plugin.getEventManager().hasActiveEvent()) {
                    player.sendMessage(ChatColor.GREEN + "Event aktif: " + 
                            plugin.getEventManager().getEventName());
                    player.sendMessage(ChatColor.GREEN + "Deskripsi: " + 
                            plugin.getEventManager().getEventDescription());
                    player.sendMessage(ChatColor.GREEN + "Berakhir dalam: " + 
                            plugin.getEventManager().getRemainingTimeFormatted());
                    player.sendMessage(ChatColor.GREEN + "Multiplier harga jual global: " + 
                            ChatColor.YELLOW + plugin.getEventManager().getSellMultiplier());
                    
                    // Tampilkan multiplier kategori
                    if (!plugin.getEventManager().getCategorySellMultipliers().isEmpty()) {
                        player.sendMessage(ChatColor.GREEN + "Multiplier per kategori:");
                        for (Map.Entry<String, Double> entry : plugin.getEventManager().getCategorySellMultipliers().entrySet()) {
                            player.sendMessage(ChatColor.YELLOW + " - " + entry.getKey() + ": " + entry.getValue());
                        }
                    }
                    
                    // Tampilkan multiplier item
                    if (!plugin.getEventManager().getItemSellMultipliers().isEmpty()) {
                        player.sendMessage(ChatColor.GREEN + "Multiplier per item:");
                        for (Map.Entry<String, Double> entry : plugin.getEventManager().getItemSellMultipliers().entrySet()) {
                            player.sendMessage(ChatColor.YELLOW + " - " + entry.getKey() + ": " + entry.getValue());
                        }
                    }
                } else {
                    player.sendMessage(ChatColor.YELLOW + "Tidak ada event yang aktif saat ini.");
                }
                return true;
            }
            case "testprice" -> {
                if (args.length < 3) {
                    player.sendMessage(ChatColor.RED + "Gunakan: /shopadmin testprice <item-id> <amount>");
                    return true;
                }
                
                String itemId = args[1];
                int amount;
                try {
                    amount = Integer.parseInt(args[2]);
                } catch (NumberFormatException e) {
                    player.sendMessage(ChatColor.RED + "Jumlah harus berupa angka");
                    return true;
                }
                
                // Cari item dan kategori
                ShopItem targetItem = null;
                Category targetCategory = null;
                
                for (Category category : plugin.getShopManager().getCategories()) {
                    for (ShopItem item : category.getItems()) {
                        if (item.getId().equalsIgnoreCase(itemId)) {
                            targetItem = item;
                            targetCategory = category;
                            break;
                        }
                    }
                    if (targetItem != null) break;
                }
                
                if (targetItem == null) {
                    player.sendMessage(ChatColor.RED + "Item dengan ID " + itemId + " tidak ditemukan");
                    return true;
                }
                
                // Hitung harga jual dasar
                double basePrice = targetItem.getSellPrice() * amount / targetItem.getAmount();
                
                // Dapatkan multiplier event
                double eventMultiplier = 1.0;
                if (plugin.getEventManager().hasActiveEvent()) {
                    eventMultiplier = plugin.getEventManager().getSellPriceMultiplier(targetItem, targetCategory);
                }
                
                // Hitung harga final
                double finalPrice = basePrice * eventMultiplier;
                
                // Tampilkan info
                player.sendMessage(ChatColor.GREEN + "=== Test Price: " + targetItem.getName() + " ===");
                player.sendMessage(ChatColor.GREEN + "Item ID: " + itemId);
                player.sendMessage(ChatColor.GREEN + "Category: " + targetCategory.getId());
                player.sendMessage(ChatColor.GREEN + "Amount: " + amount);
                player.sendMessage(ChatColor.GREEN + "Base Price: " + basePrice);
                player.sendMessage(ChatColor.GREEN + "Event Multiplier: " + eventMultiplier + 
                        (eventMultiplier > 1.0 ? " (+" + (int)((eventMultiplier - 1.0) * 100) + "%)" : ""));
                player.sendMessage(ChatColor.GREEN + "Final Price: " + finalPrice);
                return true;
            }
            case "prices" -> {
                if (args.length < 3) {
                    player.sendMessage(ChatColor.RED + "Penggunaan: /shopadmin prices adjust <multiplier> [kategori]");
                    return true;
                }
                
                if (!args[1].equalsIgnoreCase("adjust")) {
                    player.sendMessage(ChatColor.RED + "Penggunaan: /shopadmin prices adjust <multiplier> [kategori]");
                    return true;
                }
                
                double multiplier;
                try {
                    multiplier = Double.parseDouble(args[2]);
                    if (multiplier <= 0) {
                        player.sendMessage(ChatColor.RED + "Multiplier harus lebih dari 0.");
                        return true;
                    }
                } catch (NumberFormatException e) {
                    player.sendMessage(ChatColor.RED + "Multiplier harus berupa angka.");
                    return true;
                }
                
                // Kategori spesifik atau semua kategori
                String targetCategory = null;
                if (args.length >= 4) {
                    targetCategory = args[3];
                    
                    // Verifikasi kategori ada
                    if (plugin.getShopManager().getCategory(targetCategory) == null) {
                        player.sendMessage(ChatColor.RED + "Kategori '" + targetCategory + "' tidak ditemukan.");
                        return true;
                    }
                }
                
                // Lakukan penyesuaian harga
                int itemsAdjusted = adjustPrices(multiplier, targetCategory);
                
                // Simpan perubahan
                plugin.getShopManager().saveAll();
                
                // Reload shop data
                plugin.getShopManager().reloadShops();
                
                // Tampilkan pesan sukses
                if (targetCategory != null) {
                    player.sendMessage(ChatColor.GREEN + "Berhasil menyesuaikan " + itemsAdjusted + " item di kategori '" 
                            + targetCategory + "' dengan multiplier " + multiplier);
                } else {
                    player.sendMessage(ChatColor.GREEN + "Berhasil menyesuaikan " + itemsAdjusted 
                            + " item di semua kategori dengan multiplier " + multiplier);
                }
                return true;
            }
            case "blackmarket" -> {
                if (args.length < 2) {
                    player.sendMessage(ChatColor.RED + "Penggunaan: /shopadmin blackmarket <open|close|reset|rotation>");
                    return true;
                }
                
                switch (args[1].toLowerCase()) {
                    case "open" -> {
                        plugin.getBlackmarketManager().setForcedOpen(true);
                        player.sendMessage(ChatColor.GREEN + "Blackmarket dipaksa buka. Stok tidak akan direset.");
                        plugin.getBlackmarketManager().broadcastOpenMessage();
                    }
                    case "close" -> {
                        plugin.getBlackmarketManager().setForcedOpen(false);
                        player.sendMessage(ChatColor.YELLOW + "Blackmarket kembali ke mode jadwal normal.");
                        
                        // Cek apakah blackmarket tetap buka berdasarkan jadwal
                        if (!plugin.getBlackmarketManager().isOpen()) {
                            plugin.getBlackmarketManager().broadcastCloseMessage();
                        } else {
                            player.sendMessage(ChatColor.YELLOW + "Blackmarket masih buka karena masih dalam jam operasional.");
                        }
                    }
                    case "reset" -> {
                        // Reset stok blackmarket
                        for (BlackmarketItem item : plugin.getBlackmarketManager().getAllItems()) {
                            item.setCurrentStock(item.getMaxStock());
                        }
                        plugin.getBlackmarketManager().saveStockData();
                        player.sendMessage(ChatColor.GREEN + "Stok blackmarket telah direset.");
                    }
                    case "rotation" -> {
                        if (args.length < 3) {
                            sendRotationInfo(player);
                            return true;
                        }
                        
                        switch (args[2].toLowerCase()) {
                            case "info" -> sendRotationInfo(player);
                            case "toggle" -> {
                                boolean current = plugin.getBlackmarketManager().isRotationEnabled();
                                player.sendMessage(ChatColor.YELLOW + "Sistem rotasi saat ini: " + 
                                        (current ? ChatColor.GREEN + "AKTIF" : ChatColor.RED + "NONAKTIF"));
                                player.sendMessage(ChatColor.YELLOW + "Gunakan konfigurasi untuk mengubah status.");
                            }
                            default -> player.sendMessage(ChatColor.RED + "Opsi tidak valid. Gunakan: info, toggle");
                        }
                    }
                    default -> {
                        player.sendMessage(ChatColor.RED + "Opsi tidak valid. Gunakan: open, close, reset, atau rotation");
                    }
                }
                
                return true;
            }
            default -> sendHelp(player);
        }
        
        return true;
    }
    
    /**
     * Melakukan penyesuaian harga pada item shop
     * 
     * @param multiplier Faktor pengali untuk harga
     * @param categoryId Kategori tertentu atau null untuk semua kategori
     * @return Jumlah item yang diubah
     */
    private int adjustPrices(double multiplier, String categoryId) {
        int itemsAdjusted = 0;
        
        if (categoryId != null) {
            // Adjust prices in specific category
            Category category = plugin.getShopManager().getCategory(categoryId);
            if (category != null) {
                itemsAdjusted = adjustCategoryPrices(category, multiplier);
            }
        } else {
            // Adjust prices in all categories
            List<Category> categories = plugin.getShopManager().getCategories();
            for (Category category : categories) {
                itemsAdjusted += adjustCategoryPrices(category, multiplier);
            }
        }
        
        return itemsAdjusted;
    }
    
    /**
     * Melakukan penyesuaian harga pada item dalam kategori tertentu
     * 
     * @param category Kategori target
     * @param multiplier Faktor pengali untuk harga
     * @return Jumlah item yang diubah
     */
    private int adjustCategoryPrices(Category category, double multiplier) {
        int count = 0;
        
        for (ShopItem item : category.getItems()) {
            // Adjust buy price if available
            if (item.canBuy()) {
                double newBuyPrice = item.getBuyPrice() * multiplier;
                item.setBuyPrice(newBuyPrice);
            }
            
            // Adjust sell price if available
            if (item.canSell()) {
                double newSellPrice = item.getSellPrice() * multiplier;
                item.setSellPrice(newSellPrice);
            }
            
            count++;
        }
        
        return count;
    }
    
    private void sendHelp(Player player) {
        player.sendMessage("§6=== §eNusaShop Admin Commands §6===");
        player.sendMessage("§e/shopadmin §7- Buka menu admin");
        player.sendMessage("§e/shopadmin reload §7- Reload konfigurasi plugin");
        player.sendMessage("§e/shopadmin help §7- Menampilkan bantuan");
        player.sendMessage("§e/shopadmin checkevent §7- Periksa event yang sedang aktif");
        player.sendMessage("§e/shopadmin testprice <item-id> <amount> §7- Uji harga jual item");
        player.sendMessage("§e/shopadmin prices adjust <multiplier> [kategori] §7- Sesuaikan harga shop");
        player.sendMessage("§e/shopadmin blackmarket open §7- Paksa buka blackmarket");
        player.sendMessage("§e/shopadmin blackmarket close §7- Kembalikan ke jadwal normal");
        player.sendMessage("§e/shopadmin blackmarket reset §7- Reset stok blackmarket");
        player.sendMessage("§e/shopadmin blackmarket rotation info §7- Informasi rotasi blackmarket");
        player.sendMessage("§e/shopadmin blackmarket rotation toggle §7- Toggle status rotasi blackmarket");
    }
    
    /**
     * Menampilkan informasi rotasi ke admin
     * @param player Admin yang melihat info
     */
    private void sendRotationInfo(Player player) {
        BlackmarketManager manager = plugin.getBlackmarketManager();
        
        player.sendMessage(ChatColor.GOLD + "=== " + ChatColor.YELLOW + "Informasi Rotasi Blackmarket" + ChatColor.GOLD + " ===");
        player.sendMessage(ChatColor.YELLOW + "Status Rotasi: " + 
                (manager.isRotationEnabled() ? ChatColor.GREEN + "AKTIF" : ChatColor.RED + "NONAKTIF"));
        player.sendMessage(ChatColor.YELLOW + "Jadwal: " + ChatColor.WHITE + manager.getRotationSchedule());
        player.sendMessage("");
        
        player.sendMessage(ChatColor.YELLOW + "Hari ini: " + ChatColor.WHITE + manager.getCurrentDay());
        player.sendMessage(ChatColor.YELLOW + "Minggu ke: " + ChatColor.WHITE + manager.getCurrentWeek());
        
        player.sendMessage("");
        player.sendMessage(ChatColor.YELLOW + "Item yang tersedia hari ini:");
        
        // Hitung berapa item yang tersedia dari total
        int available = 0;
        int total = manager.getAllItems().size();
        
        for (BlackmarketItem item : manager.getAllItems()) {
            if (manager.isItemAvailableToday(item)) {
                available++;
                
                String rotationInfo = item.isRotatingItem() ? 
                        ChatColor.LIGHT_PURPLE + " [ROTASI]" : "";
                
                player.sendMessage(ChatColor.WHITE + "- " + 
                        ChatColor.AQUA + item.getName() + 
                        ChatColor.YELLOW + " (Stok: " + item.getCurrentStock() + ")" +
                        rotationInfo);
            }
        }
        
        player.sendMessage("");
        player.sendMessage(ChatColor.YELLOW + "Total item tersedia: " + 
                ChatColor.WHITE + available + "/" + total);
    }
}