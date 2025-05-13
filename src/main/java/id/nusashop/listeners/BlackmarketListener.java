package id.nusashop.listeners;

import id.nusashop.NusaShop;
import id.nusashop.gui.BlackmarketGUI;
import id.nusashop.models.BlackmarketItem;
import id.nusashop.utils.Animations;
import id.nusashop.utils.ItemBuilder;
import id.nusashop.managers.BlackmarketManager;

import net.milkbowl.vault.economy.Economy;
import net.milkbowl.vault.economy.EconomyResponse;

import org.bukkit.boss.BossBar;
import org.bukkit.ChatColor;
import org.bukkit.Material;
import org.bukkit.Sound;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.Listener;
import org.bukkit.event.inventory.InventoryClickEvent;
import org.bukkit.event.player.PlayerJoinEvent;
import org.bukkit.inventory.ItemStack;

import java.text.DecimalFormat;
import java.time.LocalTime;
import java.time.format.DateTimeFormatter;
import java.util.List;

/**
 * Listener untuk GUI blackmarket
 */
public class BlackmarketListener implements Listener {
    private final NusaShop plugin;
    private static final DecimalFormat PRICE_FORMAT = new DecimalFormat("#,##0.00");
    
    public BlackmarketListener(NusaShop plugin) {
        this.plugin = plugin;
    }
    
    @EventHandler
    public void onInventoryClick(InventoryClickEvent event) {
        if (!(event.getWhoClicked() instanceof Player player)) return;
        
        // Cek apakah inventory adalah BlackmarketGUI
        if (event.getInventory().getHolder() instanceof BlackmarketGUI blackmarketGUI) {
            event.setCancelled(true);
            
            if (event.getCurrentItem() == null || event.getCurrentItem().getType() == Material.AIR) return;
            
            // Tombol exit
            if (event.getRawSlot() == event.getInventory().getSize() - 5 && 
                event.getCurrentItem().getType() == Material.BARRIER) {
                player.closeInventory();
                player.playSound(player.getLocation(), Sound.UI_BUTTON_CLICK, 0.5f, 1.0f);
                return;
            }
            
            // Cek apakah slot berisi item blackmarket
            BlackmarketItem blackmarketItem = blackmarketGUI.getItemAt(event.getRawSlot());
            if (blackmarketItem != null) {
                // Proses pembelian item
                processPurchase(player, blackmarketItem);
                
                // Update GUI setelah transaksi
                new BlackmarketGUI(plugin, player).open();
                
                // Simpan data stok
                plugin.getBlackmarketManager().saveStockData();
            }
        }
    }
    
    /**
     * Memproses pembelian item blackmarket
     * @param player Player yang membeli
     * @param item Item yang dibeli
     */
    private void processPurchase(Player player, BlackmarketItem item) {
        // Cek stok
        if (item.getCurrentStock() <= 0) {
            player.sendMessage(ChatColor.RED + "Maaf, stok item ini telah habis.");
            player.playSound(player.getLocation(), Sound.ENTITY_VILLAGER_NO, 1.0f, 1.0f);
            return;
        }
        
        // Cek permission
        if (item.getPermission() != null && !player.hasPermission(item.getPermission())) {
            player.sendMessage(ChatColor.RED + "Kamu tidak memiliki izin untuk membeli item ini.");
            player.playSound(player.getLocation(), Sound.ENTITY_VILLAGER_NO, 1.0f, 1.0f);
            return;
        }
        
        // Cek harga
        double price = item.getBuyPrice();
        if (price < 0) {
            player.sendMessage(ChatColor.RED + "Item ini tidak dijual.");
            player.playSound(player.getLocation(), Sound.ENTITY_VILLAGER_NO, 1.0f, 1.0f);
            return;
        }
        
        // Cek saldo player
        Economy economy = plugin.getEconomy();
        if (economy.getBalance(player) < price) {
            player.sendMessage(ChatColor.RED + "Kamu tidak memiliki cukup uang untuk membeli item ini.");
            player.playSound(player.getLocation(), Sound.ENTITY_VILLAGER_NO, 1.0f, 1.0f);
            return;
        }
        
        // Cek inventory space
        if (!hasInventorySpace(player, item.getActualItem())) {
            player.sendMessage(ChatColor.RED + "Inventory kamu penuh!");
            player.playSound(player.getLocation(), Sound.ENTITY_VILLAGER_NO, 1.0f, 1.0f);
            return;
        }
        
        // Transaksi
        EconomyResponse response = economy.withdrawPlayer(player, price);
        if (response.transactionSuccess()) {
            // Jika ini adalah item command, eksekusi command
            if (item.isCommandItem()) {
                for (String cmd : item.getCommands()) {
                    // Ganti placeholder dengan nama player
                    String processedCmd = cmd.replace("{player}", player.getName());
                    plugin.getServer().dispatchCommand(plugin.getServer().getConsoleSender(), processedCmd);
                    plugin.getLogger().info("Executing command for " + player.getName() + ": " + processedCmd);
                }
            } else {
                // Berikan item seperti biasa untuk item non-command
                player.getInventory().addItem(item.getActualItem());
            }
            
            // Kurangi stok
            item.decreaseStock();
            
            // Notifikasi
            String message = ChatColor.GREEN + "Berhasil membeli " + ChatColor.AQUA + 
                    item.getName() + ChatColor.GREEN + " seharga " + ChatColor.GOLD + 
                    PRICE_FORMAT.format(price) + " coins";
            
            Animations.playTransactionCompleteAnimation(plugin, player, message);
            player.playSound(player.getLocation(), Sound.ENTITY_EXPERIENCE_ORB_PICKUP, 0.5f, 1.0f);
            
            // Log transaksi
            plugin.getLogger().info("[Blackmarket] " + player.getName() + " membeli " + 
                    item.getId() + " seharga " + price + " coins. Stok tersisa: " + item.getCurrentStock());
        } else {
            player.sendMessage(ChatColor.RED + "Transaksi gagal: " + response.errorMessage);
        }
    }
    
    /**
     * Cek apakah inventory pemain memiliki ruang kosong
     * @param player Player
     * @param item Item yang akan ditambahkan
     * @return true jika ada space
     */
    private boolean hasInventorySpace(Player player, ItemStack item) {
        // Cek apakah ada slot kosong
        for (ItemStack slot : player.getInventory().getStorageContents()) {
            if (slot == null || slot.getType() == Material.AIR) {
                return true;
            }
            
            // Cek stacking
            if (slot.isSimilar(item) && 
                slot.getAmount() + item.getAmount() <= slot.getMaxStackSize()) {
                return true;
            }
        }
        
        return false;
    }
    
    /**
     * Tambahkan informasi status ke lore item
     * @param lore List lore yang akan ditambahkan informasi
     */
    private void addStatusInfo(List<String> lore) {
        boolean isOpen = plugin.getBlackmarketManager().isOpen();
        
        lore.add("");
        lore.add(ChatColor.GRAY + "Status: " + 
                (isOpen ? ChatColor.GREEN + "BUKA" : ChatColor.RED + "TUTUP"));
        
        lore.add(ChatColor.GRAY + "Jam Operasional: " + 
                ChatColor.YELLOW + plugin.getBlackmarketManager().getOpenTimeFormatted() + 
                ChatColor.GRAY + " - " + 
                ChatColor.YELLOW + plugin.getBlackmarketManager().getCloseTimeFormatted());
    }
    
    /**
     * Handle player join untuk notifikasi
     */
    @EventHandler
    public void onPlayerJoin(PlayerJoinEvent event) {
        Player player = event.getPlayer();
        BlackmarketManager manager = plugin.getBlackmarketManager();
        
        // Jika Blackmarket sedang buka, tambahkan player ke BossBar
        if (manager.isOpen()) {
            // Tambahkan ke BossBar jika belum ada
            BossBar bar = manager.getBlackmarketBar();
            if (bar != null && bar.isVisible() && !bar.getPlayers().contains(player)) {
                bar.addPlayer(player);
            }
            
            // Jika player adalah VIP, beri pesan kustom
            if (player.hasPermission("nusashop.blackmarket.vip")) {
                player.sendMessage(ChatColor.DARK_GRAY + "[" + ChatColor.RED + "Blackmarket" + ChatColor.DARK_GRAY + "] " + 
                    ChatColor.GOLD + "Pasar Gelap sedang buka! " + ChatColor.GREEN + "Sebagai VIP, kamu mendapat akses prioritas!");
            } else {
                player.sendMessage(ChatColor.DARK_GRAY + "[" + ChatColor.RED + "Blackmarket" + ChatColor.DARK_GRAY + "] " + 
                    ChatColor.GOLD + "Pasar Gelap sedang buka! Ketik /blackmarket untuk mengaksesnya.");
            }
        }
    }
}