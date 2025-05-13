package id.nusashop.commands;

import id.nusashop.NusaShop;
import id.nusashop.gui.MainShopGUI;
import id.nusashop.utils.Messages;

import org.bukkit.command.Command;
import org.bukkit.command.CommandExecutor;
import org.bukkit.command.CommandSender;
import org.bukkit.entity.Player;

/**
 * Command handler untuk perintah nusashop/shop/toko
 */
public class ShopCommand implements CommandExecutor {
    private final NusaShop plugin;
    
    public ShopCommand(NusaShop plugin) {
        this.plugin = plugin;
    }
    
    @Override
    public boolean onCommand(CommandSender sender, Command command, String label, String[] args) {
        // Hanya player yang bisa menggunakan perintah ini
        if (!(sender instanceof Player player)) {
            sender.sendMessage(Messages.get("general.player-only"));
            return true;
        }
        
        // Buka GUI shop utama
        new MainShopGUI(plugin, player).open();
        return true;
    }
}