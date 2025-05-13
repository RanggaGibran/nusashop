package id.nusashop.commands;

import id.nusashop.NusaShop;
import id.nusashop.gui.BlackmarketGUI;
import id.nusashop.utils.Messages;
import id.nusashop.managers.BlackmarketManager;

import net.milkbowl.vault.economy.Economy;
import net.milkbowl.vault.economy.EconomyResponse;

import org.bukkit.ChatColor;
import org.bukkit.Sound;
import org.bukkit.command.Command;
import org.bukkit.command.CommandExecutor;
import org.bukkit.command.CommandSender;
import org.bukkit.entity.Player;

import java.util.Calendar;

/**
 * Command handler untuk akses ke blackmarket
 */
public class BlackmarketCommand implements CommandExecutor {
    private final NusaShop plugin;
    
    public BlackmarketCommand(NusaShop plugin) {
        this.plugin = plugin;
    }
    
    @Override
    public boolean onCommand(CommandSender sender, Command command, String label, String[] args) {
        // Hanya player yang bisa menggunakan perintah ini
        if (!(sender instanceof Player player)) {
            sender.sendMessage(Messages.get("general.player-only"));
            return true;
        }
        
        // Cek permission
        if (!player.hasPermission("nusashop.blackmarket")) {
            player.sendMessage(ChatColor.RED + "Kamu tidak memiliki izin untuk mengakses pasar gelap.");
            return true;
        }
        
        // Cek apakah blackmarket sedang buka
        BlackmarketManager manager = plugin.getBlackmarketManager();
        if (!manager.isOpen()) {
            // Cek apakah pemain adalah VIP dan sedang dalam waktu VIP pre-opening
            if (player.hasPermission("nusashop.blackmarket.vip") && manager.isInVipPreOpeningTime()) {
                // VIP dapat akses lebih awal
                player.sendMessage(ChatColor.GREEN + "Sebagai VIP, kamu mendapat akses lebih awal ke Pasar Gelap!");
            } else {
                player.sendMessage(ChatColor.RED + "Pasar gelap sedang tutup. Buka pada pukul " + 
                    manager.getOpenTimeFormatted() + " - " + 
                    manager.getCloseTimeFormatted());
                return true;
            }
        }
        
        // Cek argumen
        if (args.length > 0 && args[0].equalsIgnoreCase("info")) {
            sendBlackmarketInfo(player);
            return true;
        }
        
        // Cek apakah pemain memiliki cukup uang untuk biaya masuk
        double entryFee = manager.getEntryFee();
        
        // Kurangi biaya masuk untuk VIP
        if (player.hasPermission("nusashop.blackmarket.vip")) {
            entryFee *= 0.5; // VIP hanya bayar 50%
        }
        
        if (entryFee > 0) {
            Economy economy = plugin.getEconomy();
            
            // Periksa saldo pemain
            if (economy.getBalance(player) < entryFee) {
                player.sendMessage(ChatColor.RED + "Kamu memerlukan " + ChatColor.GOLD + 
                        String.format("%.0f", entryFee) + " coins" + ChatColor.RED + " untuk masuk ke pasar gelap.");
                return true;
            }
            
            // Bayar biaya masuk
            EconomyResponse response = economy.withdrawPlayer(player, entryFee);
            if (!response.transactionSuccess()) {
                player.sendMessage(ChatColor.RED + "Gagal membayar biaya masuk: " + response.errorMessage);
                return true;
            }
            
            player.sendMessage(ChatColor.GREEN + "Kamu membayar " + ChatColor.GOLD + 
                    String.format("%.0f", entryFee) + " coins" + ChatColor.GREEN + " untuk masuk ke pasar gelap.");
        }
        
        // Buka GUI blackmarket
        new BlackmarketGUI(plugin, player).open();
        
        // Play ambience sound for immersion
        player.playSound(player.getLocation(), Sound.BLOCK_ENCHANTMENT_TABLE_USE, 0.5f, 0.5f);
        
        return true;
    }
    
    /**
     * Mengirimkan informasi tentang blackmarket ke pemain
     * @param player Player yang menerima informasi
     */
    private void sendBlackmarketInfo(Player player) {
        player.sendMessage(ChatColor.DARK_GRAY + "=".repeat(40));
        player.sendMessage(ChatColor.RED + "                Pasar Gelap");
        player.sendMessage(ChatColor.DARK_GRAY + "=".repeat(40));
        player.sendMessage(ChatColor.GRAY + "Pasar gelap adalah tempat jual beli barang langka dan");
        player.sendMessage(ChatColor.GRAY + "eksklusif dengan stok terbatas.");
        player.sendMessage("");
        player.sendMessage(ChatColor.YELLOW + "Jam Buka: " + ChatColor.WHITE + 
                plugin.getBlackmarketManager().getOpenTimeFormatted() + " - " + 
                plugin.getBlackmarketManager().getCloseTimeFormatted());
        player.sendMessage(ChatColor.YELLOW + "Biaya Masuk: " + ChatColor.GREEN + 
                String.format("%.0f", plugin.getBlackmarketManager().getEntryFee()) + " coins");
        
        // Tambahkan info rotasi
        if (plugin.getBlackmarketManager().isRotationEnabled()) {
            player.sendMessage("");
            player.sendMessage(ChatColor.GOLD + "⚠ Item Rotasi: " + ChatColor.YELLOW + "Sebagian item hanya");
            player.sendMessage(ChatColor.YELLOW + "tersedia pada hari/minggu tertentu.");
            
            // Tambahkan hari saat ini untuk referensi
            Calendar cal = Calendar.getInstance();
            String dayOfWeek = plugin.getBlackmarketManager().getDayOfWeek(cal.get(Calendar.DAY_OF_WEEK));
            int weekOfMonth = cal.get(Calendar.WEEK_OF_MONTH);
            
            player.sendMessage(ChatColor.YELLOW + "Hari ini: " + ChatColor.WHITE + dayOfWeek + 
                               ChatColor.YELLOW + ", Minggu ke-" + ChatColor.WHITE + weekOfMonth);
        }
        
        player.sendMessage("");
        player.sendMessage(ChatColor.GOLD + "Gunakan " + ChatColor.WHITE + "/blackmarket" + 
                ChatColor.GOLD + " untuk masuk ke pasar gelap.");
        player.sendMessage(ChatColor.DARK_GRAY + "=".repeat(40));
    }
}