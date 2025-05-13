package id.nusashop.commands;

import id.nusashop.NusaShop;
import id.nusashop.models.Category;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.stream.Collectors;

import org.bukkit.command.Command;
import org.bukkit.command.CommandSender;
import org.bukkit.command.TabCompleter;
import org.bukkit.util.StringUtil;

/**
 * Tab completer untuk perintah admin
 */
public class AdminTabCompleter implements TabCompleter {
    
    private final NusaShop plugin;
    
    private static final List<String> COMMANDS = Arrays.asList(
            "reload", "help", "checkevent", "testprice", "prices", "blackmarket"
    );
    
    private static final List<String> PRICE_SUBCOMMANDS = Arrays.asList(
            "adjust"
    );
    
    private static final List<String> BLACKMARKET_SUBCOMMANDS = Arrays.asList(
            "open", "close", "reset", "rotation"
    );
    
    private static final List<String> ROTATION_SUBCOMMANDS = Arrays.asList(
            "info", "toggle"
    );
    
    private static final List<String> PRICE_EXAMPLE_MULTIPLIERS = Arrays.asList(
            "0.5", "0.8", "1.0", "1.2", "1.5", "2.0"
    );
    
    public AdminTabCompleter(NusaShop plugin) {
        this.plugin = plugin;
    }
    
    @Override
    public List<String> onTabComplete(CommandSender sender, Command command, String alias, String[] args) {
        List<String> completions = new ArrayList<>();
        
        if (args.length == 1) {
            // Main commands
            StringUtil.copyPartialMatches(args[0], COMMANDS, completions);
        } else if (args.length == 2) {
            // Subcommands based on first argument
            if (args[0].equalsIgnoreCase("blackmarket")) {
                StringUtil.copyPartialMatches(args[1], BLACKMARKET_SUBCOMMANDS, completions);
            } else if (args[0].equalsIgnoreCase("prices")) {
                StringUtil.copyPartialMatches(args[1], PRICE_SUBCOMMANDS, completions);
            } else if (args[0].equalsIgnoreCase("testprice")) {
                // Provide item IDs
                completions.addAll(getAllItemIds());
            }
        } else if (args.length == 3) {
            if (args[0].equalsIgnoreCase("blackmarket") && args[1].equalsIgnoreCase("rotation")) {
                // Rotation subcommands
                StringUtil.copyPartialMatches(args[2], ROTATION_SUBCOMMANDS, completions);
            } else if (args[0].equalsIgnoreCase("prices") && args[1].equalsIgnoreCase("adjust")) {
                // Multiplier examples
                StringUtil.copyPartialMatches(args[2], PRICE_EXAMPLE_MULTIPLIERS, completions);
            } else if (args[0].equalsIgnoreCase("testprice")) {
                // Suggest amounts for testprice
                completions.addAll(Arrays.asList("1", "8", "16", "32", "64"));
            }
        } else if (args.length == 4) {
            if (args[0].equalsIgnoreCase("prices") && args[1].equalsIgnoreCase("adjust")) {
                // Category IDs
                List<String> categoryIds = getCategoryIds();
                StringUtil.copyPartialMatches(args[3], categoryIds, completions);
            }
        }
        
        return completions.stream().sorted().collect(Collectors.toList());
    }
    
    /**
     * Mendapatkan semua ID kategori dari ShopManager
     * @return List ID kategori
     */
    private List<String> getCategoryIds() {
        return plugin.getShopManager().getCategories().stream()
                .map(Category::getId)
                .collect(Collectors.toList());
    }
    
    /**
     * Mendapatkan semua ID item dari semua kategori
     * @return List ID item
     */
    private List<String> getAllItemIds() {
        List<String> itemIds = new ArrayList<>();
        
        for (Category category : plugin.getShopManager().getCategories()) {
            category.getItems().forEach(item -> itemIds.add(item.getId()));
        }
        
        return itemIds;
    }
}