package id.nusashop.gui;

import id.nusashop.NusaShop;
import id.nusashop.models.Category;
import id.nusashop.models.ShopItem;
import id.nusashop.utils.Messages;
import id.nusashop.utils.ShopUtils;

import org.bukkit.Bukkit;
import org.bukkit.ChatColor;
import org.bukkit.Material;
import org.bukkit.Sound;
import org.bukkit.entity.Player;
import org.bukkit.inventory.Inventory;
import org.bukkit.inventory.InventoryHolder;
import org.bukkit.inventory.ItemStack;

import java.text.DecimalFormat;
import java.util.HashMap;
import java.util.Map;

/**
 * GUI untuk menjual item dengan menempatkan item di inventory
 */
public class SellGUI implements InventoryHolder {
    private final NusaShop plugin;
    private final Player player;
    private final Inventory inventory;
    
    private static final DecimalFormat PRICE_FORMAT = new DecimalFormat("#,##0.00");
    
    public SellGUI(NusaShop plugin, Player player) {
        this.plugin = plugin;
        this.player = player;
        this.inventory = Bukkit.createInventory(this, 6 * 9, ChatColor.DARK_GREEN + "" + ChatColor.BOLD + "Letakkan Item untuk Dijual");
    }
    
    /**
     * Proses penjualan item saat GUI ditutup
     */
    public void processSale() {
        boolean anySold = false;
        double totalEarnings = 0;
        
        // Proses setiap item di inventory
        for (ItemStack item : inventory.getContents()) {
            if (item == null) continue;
            
            // Cari item shop yang cocok
            ShopItem shopItem = findShopItem(item);
            
            if (shopItem != null && shopItem.canSell()) {
                // Hitung jumlah unit penuh yang dimiliki
                int sellAmount = item.getAmount();
                
                // Cari kategori untuk menerapkan event multiplier
                Category category = findCategory(shopItem);
                
                // Hitung harga dasar per item
                double pricePerUnit = shopItem.getSellPrice() / shopItem.getAmount();
                double totalPrice = pricePerUnit * sellAmount;
                
                // Terapkan multiplier event jika ada
                double eventMultiplier = 1.0;
                if (plugin.getEventManager().hasActiveEvent()) {
                    eventMultiplier = plugin.getEventManager().getSellPriceMultiplier(shopItem, category);
                    totalPrice *= eventMultiplier;
                }
                
                // Berikan uang ke pemain
                plugin.getEconomy().depositPlayer(player, totalPrice);
                
                // Update statistik
                if (category != null) {
                    plugin.getStatisticsManager().recordSellTransaction(player, shopItem, category, sellAmount, totalPrice);
                }
                
                // Akumulasi total pendapatan
                totalEarnings += totalPrice;
                anySold = true;
                
                
                // Tambahkan pesan bonus untuk event jika ada
                if (eventMultiplier > 1.0) {
                    int bonusPercent = (int)((eventMultiplier - 1.0) * 100);
                    player.sendMessage(ChatColor.GREEN + "+" + bonusPercent + "% bonus dari event!");
                }
            } else {
                // Kembalikan item ke pemain jika tidak bisa dijual
                returnItemToPlayer(item);
            }
        }
        
        if (anySold) {
            // Putar sound effect sukses
            player.playSound(player.getLocation(), Sound.ENTITY_PLAYER_LEVELUP, 0.5f, 1.0f);
            
            // Kirim total pendapatan jika beberapa item dijual
            if (totalEarnings > 0) {
                player.sendMessage(ChatColor.GREEN + "Total pendapatan: " + 
                                ChatColor.GOLD + PRICE_FORMAT.format(totalEarnings) + " coins");
            }
        } else {
            player.sendMessage(ChatColor.YELLOW + "Tidak ada item yang terjual.");
        }
    }
    
    /**
     * Kembalikan item ke inventory pemain atau jatuhkan di kaki mereka jika inventory penuh
     */
    private void returnItemToPlayer(ItemStack item) {
        HashMap<Integer, ItemStack> leftover = player.getInventory().addItem(item);
        
        // Jatuhkan item yang tidak muat di inventory pemain
        for (ItemStack drop : leftover.values()) {
            player.getWorld().dropItemNaturally(player.getLocation(), drop);
            player.sendMessage(ChatColor.YELLOW + "Item yang tidak dapat dijual telah dikembalikan ke inventory Anda atau dijatuhkan di sekitar Anda.");
        }
    }
    
    /**
     * Temukan item shop yang cocok dengan item yang diberikan
     */
    private ShopItem findShopItem(ItemStack item) {
        for (Category category : plugin.getShopManager().getCategories()) {
            for (ShopItem shopItem : category.getItems()) {
                try {
                    // Periksa apakah material cocok
                    Material shopMaterial = Material.valueOf(shopItem.getMaterial().toUpperCase());
                    if (item.getType() == shopMaterial) {
                        return shopItem;
                    }
                } catch (IllegalArgumentException ignored) {
                    // Material tidak valid, lewati
                }
            }
        }
        return null;
    }
    
    /**
     * Temukan kategori yang berisi item shop
     */
    private Category findCategory(ShopItem shopItem) {
        for (Category category : plugin.getShopManager().getCategories()) {
            if (category.getItems().contains(shopItem)) {
                return category;
            }
        }
        return null;
    }
    
    /**
     * Buka GUI
     */
    public void open() {
        player.openInventory(inventory);
    }
    
    @Override
    public Inventory getInventory() {
        return inventory;
    }
}