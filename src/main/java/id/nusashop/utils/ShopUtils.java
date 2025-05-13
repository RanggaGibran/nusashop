package id.nusashop.utils;

import id.nusashop.NusaShop;
import id.nusashop.models.Category;
import id.nusashop.models.ShopItem;
import id.nusashop.managers.StatisticsManager;
import id.nusashop.utils.Animations;

import org.bukkit.ChatColor;
import org.bukkit.Material;
import org.bukkit.Sound;
import org.bukkit.entity.Player;
import org.bukkit.inventory.ItemStack;
import org.bukkit.inventory.PlayerInventory;
import org.bukkit.plugin.java.JavaPlugin;
import org.bukkit.plugin.RegisteredServiceProvider;

import org.bukkit.event.player.PlayerInteractEvent;
import org.bukkit.event.EventHandler;

import net.milkbowl.vault.economy.Economy;
import net.milkbowl.vault.economy.EconomyResponse;

/**
 * Utilitas untuk transaksi shop
 */
public class ShopUtils {
    
    /**
     * Membeli item dari shop
     * @param plugin plugin instance
     * @param player player yang membeli
     * @param shopItem item yang dibeli
     * @param amount jumlah yang dibeli
     * @return true jika pembelian berhasil
     */
    public static boolean buyItem(NusaShop plugin, Player player, ShopItem shopItem, int amount) {
        // Cek apakah item bisa dibeli
        if (!shopItem.canBuy()) {
            playFailSound(plugin, player);
            Messages.send(player, "shop.cannot-buy");
            return false;
        }
        
        // Hitung harga sesuai jumlah
        double pricePerUnit = shopItem.getBuyPrice() / shopItem.getAmount();
        double totalPrice = pricePerUnit * amount;
        
        // Cek saldo player
        Economy economy = plugin.getEconomy();
        if (economy.getBalance(player) < totalPrice) {
            playFailSound(plugin, player);
            String[] placeholders = {"%price%", "%balance%"};
            String[] values = {String.format("%.2f", totalPrice), 
                              String.format("%.2f", economy.getBalance(player))};
            Messages.send(player, "shop.buy-failed", placeholders, values);
            return false;
        }
        
        // Buat item yang akan diberikan
        try {
            Material material = Material.valueOf(shopItem.getMaterial().toUpperCase());
            
            // Check if we have inventory space first
            int maxStackSize = material.getMaxStackSize();
            int requiredSlots = (int) Math.ceil((double) amount / maxStackSize);
            int freeSlots = 0;
            
            // Count free slots and slots with same material that can fit more items
            for (ItemStack slot : player.getInventory().getStorageContents()) {
                if (slot == null || slot.getType() == Material.AIR) {
                    freeSlots++;
                } else if (slot.getType() == material && slot.getAmount() < maxStackSize) {
                    // This slot can fit more of our material
                    freeSlots++;
                }
            }
            
            if (freeSlots < requiredSlots) {
                playFailSound(plugin, player);
                Messages.send(player, "shop.inventory-full");
                return false;
            }
            
            // Calculate how many full stacks and remaining items
            int fullStacks = amount / maxStackSize;
            int remainder = amount % maxStackSize;
            
            // Now we know we have enough space, do the economy transaction first
            EconomyResponse response = economy.withdrawPlayer(player, totalPrice);
            
            if (response.transactionSuccess()) {
                // Give the items in proper stacks
                for (int i = 0; i < fullStacks; i++) {
                    ItemStack stack = new ItemStack(material, maxStackSize);
                    player.getInventory().addItem(stack);
                }
                
                // Add remainder items if any
                if (remainder > 0) {
                    ItemStack remainderStack = new ItemStack(material, remainder);
                    player.getInventory().addItem(remainderStack);
                }
                
                // Catat transaksi untuk statistik
                // Cari kategori dari item ini
                for (Category category : plugin.getShopManager().getCategories()) {
                    if (category.getItems().stream().anyMatch(item -> item.getId().equals(shopItem.getId()))) {
                        plugin.getStatisticsManager().recordBuyTransaction(player, shopItem, category, amount, totalPrice);
                        break;
                    }
                }
                
                String message = Messages.get("shop.buy-success")
                    .replace("%amount%", String.valueOf(amount))
                    .replace("%item%", shopItem.getName())
                    .replace("%price%", String.format("%.2f", totalPrice));
                
                // Gunakan versi animasi yang tidak menggunakan particle
                Animations.playTransactionCompleteAnimation(plugin, player, message);
                return true;
            } else {
                playFailSound(plugin, player);
                Messages.send(player, "shop.transaction-error");
                return false;
            }
            
        } catch (IllegalArgumentException e) {
            playFailSound(plugin, player);
            player.sendMessage(ChatColor.RED + "Error: Item tidak valid");
            return false;
        }
    }
    
    /**
     * Menjual item ke shop
     * @param plugin plugin instance
     * @param player player yang menjual
     * @param shopItem item yang dijual
     * @param amount jumlah yang dijual
     * @return true jika penjualan berhasil
     */
    public static boolean sellItem(NusaShop plugin, Player player, ShopItem shopItem, int amount) {
        // Validasi item dapat dijual
        if (!shopItem.canSell()) {
            Messages.send(player, "shop.sell-not-allowed");
            return false;
        }
        
        Material itemMaterial = Material.valueOf(shopItem.getMaterial().toUpperCase());
        
        // Validasi pemain memiliki item
        if (!hasItems(player, itemMaterial, amount)) {
            Messages.send(player, "shop.sell-failed");
            return false;
        }
        
        // Cari kategori dari item untuk multiplier event
        Category itemCategory = null;
        for (Category category : plugin.getShopManager().getCategories()) {
            if (category.getItems().stream().anyMatch(item -> item.getId().equals(shopItem.getId()))) {
                itemCategory = category;
                break;
            }
        }
        
        // Hitung harga dasar
        double basePrice = shopItem.getSellPrice() * amount / shopItem.getAmount();
        double eventMultiplier = 1.0;
        String eventBonusText = "";
        
        // Terapkan multiplier event jika kategori ditemukan
        if (itemCategory != null && plugin.getEventManager() != null && plugin.getEventManager().hasActiveEvent()) {
            eventMultiplier = plugin.getEventManager().getSellPriceMultiplier(shopItem, itemCategory);
            
            // Jika ada bonus dari event
            if (eventMultiplier > 1.0) {
                int bonusPercent = (int)((eventMultiplier - 1.0) * 100);
                eventBonusText = ChatColor.YELLOW + " (+" + bonusPercent + "% event bonus)";
            }
        }
        
        // Hitung harga final dengan multiplier
        double finalPrice = basePrice * eventMultiplier;
        
        // Hapus item dari inventory
        removeItems(player, itemMaterial, amount);
        
        // Berikan uang ke pemain
        plugin.getEconomy().depositPlayer(player, finalPrice);
        
        // Catat transaksi di statistik
        if (itemCategory != null) {
            plugin.getStatisticsManager().recordSellTransaction(player, shopItem, itemCategory, amount, finalPrice);
        }
        
        // Tampilkan pesan sukses
        String message = Messages.get("shop.sell-success")
            .replace("%amount%", String.valueOf(amount))
            .replace("%item%", shopItem.getName())
            .replace("%price%", StatisticsManager.formatPrice(finalPrice));
        
        // Tambahkan informasi bonus event jika ada
        if (!eventBonusText.isEmpty()) {
            message += eventBonusText;
        }
        
        // Gunakan animasi untuk konsistensi dengan sellAllItems
        Animations.playTransactionCompleteAnimation(plugin, player, message);
        
        // Putar sound sukses
        playSuccessSound(plugin, player);
        
        return true;
    }
    
    /**
     * Menjual semua item sejenis di inventory
     * @param plugin plugin instance
     * @param player player yang menjual
     * @param shopItem item yang dijual
     * @return true jika penjualan berhasil
     */
    public static boolean sellAllItems(NusaShop plugin, Player player, ShopItem shopItem) {
        // Cek apakah item bisa dijual
        if (!shopItem.canSell()) {
            playFailSound(plugin, player);
            Messages.send(player, "shop.cannot-sell");
            return false;
        }
        
        try {
            Material material = Material.valueOf(shopItem.getMaterial().toUpperCase());
            int totalAmount = countItems(player, material);
            
            if (totalAmount <= 0) {
                playFailSound(plugin, player);
                Messages.send(player, "shop.sell-failed");
                return false;
            }
            
            // Cari kategori untuk multiplier event
            Category itemCategory = null;
            for (Category category : plugin.getShopManager().getCategories()) {
                if (category.getItems().stream().anyMatch(item -> item.getId().equals(shopItem.getId()))) {
                    itemCategory = category;
                    break;
                }
            }
            
            // Hitung harga dasar
            double pricePerUnit = shopItem.getSellPrice() / shopItem.getAmount();
            double basePrice = pricePerUnit * totalAmount;
            double eventMultiplier = 1.0;
            String eventBonusText = "";
            
            // Terapkan multiplier event jika kategori ditemukan
            if (itemCategory != null && plugin.getEventManager() != null && plugin.getEventManager().hasActiveEvent()) {
                eventMultiplier = plugin.getEventManager().getSellPriceMultiplier(shopItem, itemCategory);
                
                // Jika ada bonus dari event
                if (eventMultiplier > 1.0) {
                    int bonusPercent = (int)((eventMultiplier - 1.0) * 100);
                    eventBonusText = ChatColor.YELLOW + " (+" + bonusPercent + "% event bonus)";
                }
            }
            
            // Hitung harga final dengan multiplier
            double totalPrice = basePrice * eventMultiplier;
            
            // Transaksi berhasil, ambil item dan berikan uang
            removeItems(player, material, totalAmount);
            Economy economy = plugin.getEconomy();
            EconomyResponse response = economy.depositPlayer(player, totalPrice);
            
            if (response.transactionSuccess()) {
                // Catat transaksi untuk statistik
                if (itemCategory != null) {
                    plugin.getStatisticsManager().recordSellTransaction(player, shopItem, itemCategory, totalAmount, totalPrice);
                }
                
                String message = Messages.get("shop.sell-success")
                    .replace("%amount%", String.valueOf(totalAmount))
                    .replace("%item%", shopItem.getName())
                    .replace("%price%", String.format("%.2f", totalPrice));
                
                // Tambahkan informasi bonus event jika ada
                if (!eventBonusText.isEmpty()) {
                    message += eventBonusText;
                }
                
                // Gunakan versi animasi yang tidak menggunakan particle
                Animations.playTransactionCompleteAnimation(plugin, player, message);
                return true;
            } else {
                // Kembalikan item jika transaksi gagal
                player.getInventory().addItem(new ItemStack(material, totalAmount));
                playFailSound(plugin, player);
                Messages.send(player, "shop.transaction-error");
                return false;
            }
            
        } catch (IllegalArgumentException e) {
            playFailSound(plugin, player);
            player.sendMessage(ChatColor.RED + "Error: Item tidak valid");
            return false;
        }
    }
    
    /**
     * Cek apakah inventory memiliki cukup ruang untuk menampung item
     * @param player player
     * @param item item yang akan ditambahkan
     * @return true jika cukup ruang
     */
    private static boolean hasSpace(Player player, ItemStack item) {
        PlayerInventory inventory = player.getInventory();
        int free = 0;
        
        for (ItemStack slot : inventory.getStorageContents()) {
            if (slot == null || slot.getType() == Material.AIR) {
                free += item.getMaxStackSize();
            } else if (slot.isSimilar(item)) {
                free += Math.max(0, slot.getMaxStackSize() - slot.getAmount());
            }
            
            if (free >= item.getAmount()) {
                return true;
            }
        }
        
        return false;
    }
    
    /**
     * Cek apakah player memiliki item tertentu dalam jumlah tertentu
     * @param player player
     * @param material jenis item
     * @param amount jumlah yang dibutuhkan
     * @return true jika memiliki item yang cukup
     */
    private static boolean hasItems(Player player, Material material, int amount) {
        return countItems(player, material) >= amount;
    }
    
    /**
     * Menghitung jumlah item tertentu di inventory player
     * @param player player
     * @param material jenis item
     * @return jumlah total item
     */
    public static int countItems(Player player, Material material) {
        PlayerInventory inventory = player.getInventory();
        int count = 0;
        
        for (ItemStack item : inventory.getContents()) {
            if (item != null && item.getType() == material) {
                count += item.getAmount();
            }
        }
        
        return count;
    }
    
    /**
     * Menghapus sejumlah item dari inventory player
     * @param player player
     * @param material jenis item
     * @param amount jumlah yang akan dihapus
     */
    private static void removeItems(Player player, Material material, int amount) {
        PlayerInventory inventory = player.getInventory();
        int remaining = amount;
        
        for (int i = 0; i < inventory.getSize(); i++) {
            ItemStack item = inventory.getItem(i);
            
            if (item != null && item.getType() == material) {
                if (item.getAmount() <= remaining) {
                    remaining -= item.getAmount();
                    inventory.setItem(i, null);
                } else {
                    item.setAmount(item.getAmount() - remaining);
                    remaining = 0;
                }
                
                if (remaining <= 0) {
                    break;
                }
            }
        }
    }
    
    /**
     * Memainkan suara sukses
     * @param plugin plugin instance
     * @param player player
     */
    private static void playSuccessSound(NusaShop plugin, Player player) {
        try {
            String soundName = plugin.getConfigManager().getConfig().getString("shop.success-sound", "ENTITY_PLAYER_LEVELUP");
            Sound sound = Sound.valueOf(soundName);
            player.playSound(player.getLocation(), sound, 1.0f, 1.0f);
        } catch (Exception ignored) {
            player.playSound(player.getLocation(), Sound.ENTITY_PLAYER_LEVELUP, 1.0f, 1.0f);
        }
    }
    
    /**
     * Memainkan suara gagal
     * @param plugin plugin instance
     * @param player player
     */
    private static void playFailSound(NusaShop plugin, Player player) {
        try {
            String soundName = plugin.getConfigManager().getConfig().getString("shop.fail-sound", "ENTITY_VILLAGER_NO");
            Sound sound = Sound.valueOf(soundName);
            player.playSound(player.getLocation(), sound, 1.0f, 1.0f);
        } catch (Exception ignored) {
            player.playSound(player.getLocation(), Sound.ENTITY_VILLAGER_NO, 1.0f, 1.0f);
        }
    }
}