package id.nusashop.commands;

import id.nusashop.NusaShop;
import id.nusashop.models.SellWand;
import id.nusashop.utils.Messages;

import org.bukkit.Bukkit;
import org.bukkit.ChatColor;
import org.bukkit.command.Command;
import org.bukkit.command.CommandExecutor;
import org.bukkit.command.CommandSender;
import org.bukkit.command.TabCompleter;
import org.bukkit.entity.Player;
import org.bukkit.inventory.ItemStack;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.List;

/**
 * Command untuk membuat dan memberikan sell wand
 */
public class SellWandCommand implements CommandExecutor, TabCompleter {
    private final NusaShop plugin;
    private final SellWand sellWand;
    
    public SellWandCommand(NusaShop plugin) {
        this.plugin = plugin;
        this.sellWand = new SellWand(plugin);
    }
    
    @Override
    public boolean onCommand(CommandSender sender, Command command, String label, String[] args) {
        if (!sender.hasPermission("nusashop.sellwand.give")) {
            if (sender instanceof Player) {
                Messages.send((Player) sender, "sellwand.no-permission");
            } else {
                sender.sendMessage(ChatColor.RED + "You do not have permission to use this command.");
            }
            return true;
        }
        
        if (args.length < 1) {
            sender.sendMessage(ChatColor.RED + "Penggunaan: /" + label + " <player> [uses]");
            return true;
        }
        
        // Dapatkan target player
        Player target = Bukkit.getPlayer(args[0]);
        if (target == null) {
            sender.sendMessage(ChatColor.RED + "Player tidak ditemukan: " + args[0]);
            return true;
        }
        
        // Dapatkan jumlah uses
        int uses = 10; // Default
        if (args.length >= 2) {
            try {
                uses = Integer.parseInt(args[1]);
                if (uses <= 0) {
                    sender.sendMessage(ChatColor.RED + "Jumlah penggunaan harus lebih dari 0!");
                    return true;
                }
            } catch (NumberFormatException e) {
                sender.sendMessage(ChatColor.RED + "Jumlah penggunaan harus berupa angka: " + args[1]);
                return true;
            }
        }
        
        // Buat sell wand dan berikan ke player
        ItemStack wandItem = sellWand.createWand(uses);
        target.getInventory().addItem(wandItem);
        
        sender.sendMessage(ChatColor.GREEN + "Memberikan Tongkat Penjual dengan " + 
            ChatColor.YELLOW + uses + ChatColor.GREEN + " penggunaan kepada " + 
            ChatColor.YELLOW + target.getName());
        
        target.sendMessage(ChatColor.GREEN + "Anda menerima " + 
            ChatColor.GOLD + "Tongkat Penjual" + ChatColor.GREEN + " dengan " + 
            ChatColor.YELLOW + uses + ChatColor.GREEN + " penggunaan.");
        
        return true;
    }
    
    @Override
    public List<String> onTabComplete(CommandSender sender, Command command, String alias, String[] args) {
        if (!sender.hasPermission("nusashop.sellwand.give")) {
            return Collections.emptyList();
        }
        
        if (args.length == 1) {
            // Autocomplete player names
            List<String> playerNames = new ArrayList<>();
            for (Player player : Bukkit.getOnlinePlayers()) {
                playerNames.add(player.getName());
            }
            return playerNames;
        } else if (args.length == 2) {
            // Suggest common uses values
            return Arrays.asList("5", "10", "25", "50", "100");
        }
        
        return Collections.emptyList();
    }
}