package id.nusashop.commands;

import id.nusashop.NusaShop;
import org.bukkit.command.Command;
import org.bukkit.command.CommandSender;
import org.bukkit.command.TabCompleter;
import org.bukkit.entity.Player;
import org.bukkit.util.StringUtil;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.stream.Collectors;

/**
 * Tab completer untuk command blackmarket
 */
public class BlackmarketTabCompleter implements TabCompleter {
    
    private final NusaShop plugin;
    
    // Daftar subcommands yang tersedia
    private static final List<String> SUBCOMMANDS = Arrays.asList(
        "info"
    );
    
    public BlackmarketTabCompleter(NusaShop plugin) {
        this.plugin = plugin;
    }
    
    @Override
    public List<String> onTabComplete(CommandSender sender, Command command, String alias, String[] args) {
        List<String> completions = new ArrayList<>();
        
        // Hanya player yang bisa menggunakan command ini
        if (!(sender instanceof Player player)) {
            return completions;
        }
        
        // Cek permission
        if (!player.hasPermission("nusashop.blackmarket")) {
            return completions;
        }
        
        // Jika args.length == 1, sediakan subcommands
        if (args.length == 1) {
            StringUtil.copyPartialMatches(args[0], SUBCOMMANDS, completions);
        }
        
        // Sort completions
        return completions.stream().sorted().collect(Collectors.toList());
    }
}