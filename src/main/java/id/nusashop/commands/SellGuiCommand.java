package id.nusashop.commands;

import id.nusashop.NusaShop;
import id.nusashop.gui.SellGUI;
import id.nusashop.utils.Messages;

import org.bukkit.command.Command;
import org.bukkit.command.CommandExecutor;
import org.bukkit.command.CommandSender;
import org.bukkit.entity.Player;

/**
 * Command untuk membuka GUI penjualan cepat
 */
public class SellGuiCommand implements CommandExecutor {
    private final NusaShop plugin;
    
    public SellGuiCommand(NusaShop plugin) {
        this.plugin = plugin;
    }
    
    @Override
    public boolean onCommand(CommandSender sender, Command command, String label, String[] args) {
        if (!(sender instanceof Player)) {
            sender.sendMessage(Messages.get("general.player-only"));
            return true;
        }
        
        Player player = (Player) sender;
        
        
        // Cek permission
        if (!player.hasPermission("nusashop.sell")) {
            Messages.send(player, "general.no-permission");
            return true;
        }
        
        // Buka GUI penjualan
        new SellGUI(plugin, player).open();
        return true;
    }
}