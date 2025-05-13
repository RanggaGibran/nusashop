package id.nusashop.commands;

import id.nusashop.NusaShop;
import id.nusashop.managers.StatisticsManager;
import id.nusashop.managers.StatisticsManager.PlayerStats;
import id.nusashop.utils.Messages;

import org.bukkit.Bukkit;
import org.bukkit.ChatColor;
import org.bukkit.command.Command;
import org.bukkit.command.CommandExecutor;
import org.bukkit.command.CommandSender;
import org.bukkit.entity.Player;

/**
 * Command untuk melihat statistik shop
 */
public class StatsCommand implements CommandExecutor {
    private final NusaShop plugin;
    
    public StatsCommand(NusaShop plugin) {
        this.plugin = plugin;
    }
    
    @Override
    public boolean onCommand(CommandSender sender, Command command, String label, String[] args) {
        // Command dapat digunakan oleh player dan console
        if (args.length == 0) {
            showGlobalStats(sender);
            return true;
        }
        
        if (args.length == 1 && args[0].equalsIgnoreCase("help")) {
            showHelp(sender);
            return true;
        }
        
        if (args.length == 1 && args[0].equalsIgnoreCase("global")) {
            showGlobalStats(sender);
            return true;
        }
        
        if ((args.length == 1 || args.length == 2) && args[0].equalsIgnoreCase("player")) {
            Player targetPlayer;
            
            // Jika ada argumen kedua, cari player dengan nama itu
            if (args.length == 2) {
                targetPlayer = Bukkit.getPlayer(args[1]);
                if (targetPlayer == null) {
                    sender.sendMessage(ChatColor.RED + "Player tidak ditemukan: " + args[1]);
                    return true;
                }
            } else {
                // Jika tidak ada argumen kedua dan perintah dari player, gunakan player itu
                if (!(sender instanceof Player)) {
                    sender.sendMessage(ChatColor.RED + "Harap tentukan nama player: /shopstats player <nama>");
                    return true;
                }
                
                targetPlayer = (Player) sender;
            }
            
            showPlayerStats(sender, targetPlayer);
            return true;
        }
        
        // Argumen tidak valid, tampilkan bantuan
        showHelp(sender);
        return true;
    }
    
    /**
     * Menampilkan statistik global
     */
    private void showGlobalStats(CommandSender sender) {
        StatisticsManager statsManager = plugin.getStatisticsManager();
        
        sender.sendMessage(ChatColor.GOLD + "=== " + ChatColor.YELLOW + "NusaShop - Statistik Global" + ChatColor.GOLD + " ===");
        sender.sendMessage(ChatColor.YELLOW + "Total Transaksi: " + ChatColor.WHITE + statsManager.getTotalTransactions());
        sender.sendMessage(ChatColor.YELLOW + "Total Uang Dibelanjakan: " + ChatColor.GREEN + 
                "Rp" + StatisticsManager.formatPrice(statsManager.getTotalMoneySpent()));
        sender.sendMessage(ChatColor.YELLOW + "Total Uang Diperoleh: " + ChatColor.GREEN + 
                "Rp" + StatisticsManager.formatPrice(statsManager.getTotalMoneyEarned()));
        sender.sendMessage("");
        sender.sendMessage(ChatColor.YELLOW + "Item Terpopuler (Pembelian): " + ChatColor.WHITE + statsManager.getMostBoughtItem());
        sender.sendMessage(ChatColor.YELLOW + "Item Terpopuler (Penjualan): " + ChatColor.WHITE + statsManager.getMostSoldItem());
        sender.sendMessage(ChatColor.YELLOW + "Kategori Terpopuler: " + ChatColor.WHITE + statsManager.getMostPopularCategory());
    }
    
    /**
     * Menampilkan statistik player
     */
    private void showPlayerStats(CommandSender sender, Player targetPlayer) {
        StatisticsManager statsManager = plugin.getStatisticsManager();
        PlayerStats playerStats = statsManager.getPlayerStats(targetPlayer);
        
        sender.sendMessage(ChatColor.GOLD + "=== " + ChatColor.YELLOW + "NusaShop - Statistik " + 
                targetPlayer.getName() + ChatColor.GOLD + " ===");
        sender.sendMessage(ChatColor.YELLOW + "Total Transaksi: " + ChatColor.WHITE + playerStats.getTotalTransactions());
        sender.sendMessage(ChatColor.YELLOW + "Item Dibeli: " + ChatColor.WHITE + playerStats.getTotalBuyCount() + " unit");
        sender.sendMessage(ChatColor.YELLOW + "Item Dijual: " + ChatColor.WHITE + playerStats.getTotalSellCount() + " unit");
        sender.sendMessage("");
        sender.sendMessage(ChatColor.YELLOW + "Total Belanja: " + ChatColor.GREEN + 
                "Rp" + StatisticsManager.formatPrice(playerStats.getTotalSpent()));
        sender.sendMessage(ChatColor.YELLOW + "Total Pendapatan: " + ChatColor.GREEN + 
                "Rp" + StatisticsManager.formatPrice(playerStats.getTotalEarned()));
    }
    
    /**
     * Menampilkan bantuan
     */
    private void showHelp(CommandSender sender) {
        sender.sendMessage(ChatColor.GOLD + "=== " + ChatColor.YELLOW + "NusaShop - Bantuan Statistik" + ChatColor.GOLD + " ===");
        sender.sendMessage(ChatColor.YELLOW + "/shopstats " + ChatColor.GRAY + "- Menampilkan statistik global");
        sender.sendMessage(ChatColor.YELLOW + "/shopstats global " + ChatColor.GRAY + "- Menampilkan statistik global");
        sender.sendMessage(ChatColor.YELLOW + "/shopstats player " + ChatColor.GRAY + "- Menampilkan statistik player Anda");
        sender.sendMessage(ChatColor.YELLOW + "/shopstats player <nama> " + ChatColor.GRAY + "- Menampilkan statistik player tertentu");
        sender.sendMessage(ChatColor.YELLOW + "/shopstats help " + ChatColor.GRAY + "- Menampilkan bantuan ini");
    }
}