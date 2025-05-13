package id.nusashop.gui;

import id.nusashop.NusaShop;
import id.nusashop.models.Category;
import id.nusashop.models.ShopItem;
import id.nusashop.utils.ItemBuilder;
import id.nusashop.utils.ShopUtils;
import id.nusashop.managers.StatisticsManager;

import org.bukkit.Bukkit;
import org.bukkit.ChatColor;
import org.bukkit.Material;
import org.bukkit.enchantments.Enchantment;
import org.bukkit.entity.Player;
import org.bukkit.inventory.Inventory;
import org.bukkit.inventory.InventoryHolder;
import org.bukkit.inventory.ItemFlag;
import org.bukkit.inventory.ItemStack;

import java.util.ArrayList;
import java.util.List;

/**
 * GUI untuk detail item dan transaksi
 */
public class ItemDetailsGUI implements InventoryHolder {
    private final NusaShop plugin;
    private final Player player;
    private final Category category;
    private final ShopItem item;
    private final Inventory inventory;
    
    public ItemDetailsGUI(NusaShop plugin, Player player, Category category, ShopItem item) {
        this.plugin = plugin;
        this.player = player;
        this.category = category;
        this.item = item;
        
        String title = ChatColor.translateAlternateColorCodes('&', "&8Detail " + item.getName().replace("&", ""));
        this.inventory = Bukkit.createInventory(this, 5 * 9, title);
        
        setupInventory();
    }
    
    private void setupInventory() {
        // Background dasar
        ItemStack backgroundItem = new ItemBuilder(Material.LIGHT_BLUE_STAINED_GLASS_PANE)
            .name(" ")
            .build();
        
        for (int i = 0; i < inventory.getSize(); i++) {
            inventory.setItem(i, backgroundItem);
        }
        
        // Background bagian dalam
        ItemStack innerBackground = new ItemBuilder(Material.WHITE_STAINED_GLASS_PANE)
            .name(" ")
            .build();
        
        for (int row = 1; row < 4; row++) {
            for (int col = 1; col < 8; col++) {
                inventory.setItem(row * 9 + col, innerBackground);
            }
        }
        
        // Item display utama di tengah
        Material material;
        try {
            material = Material.valueOf(item.getMaterial().toUpperCase());
        } catch (IllegalArgumentException e) {
            material = Material.BARRIER;
        }
        
        ItemStack displayItem = new ItemBuilder(material)
            .name(ChatColor.translateAlternateColorCodes('&', item.getName()))
            .amount(item.getAmount())
            .flag(ItemFlag.HIDE_ATTRIBUTES)
            .flag(ItemFlag.HIDE_ENCHANTS)
            .enchant(Enchantment.UNBREAKING, 1)
            .build();
        
        inventory.setItem(13, displayItem);
        
        // Informasi item (kiri)
        List<String> infoLore = new ArrayList<>();
        
        infoLore.add(ChatColor.GRAY + "Kategori: " + ChatColor.GOLD + 
                     ChatColor.translateAlternateColorCodes('&', category.getName()));
        
        infoLore.add(ChatColor.GRAY + "Material: " + ChatColor.WHITE + item.getMaterial());
        infoLore.add(ChatColor.GRAY + "Jumlah: " + ChatColor.WHITE + item.getAmount() + " item/stack");
        
        if (plugin.getStatisticsManager().getItemBuyCount(item.getId()) > 0) {
            infoLore.add("");
            infoLore.add(ChatColor.GRAY + "Dibeli: " + ChatColor.WHITE + 
                         plugin.getStatisticsManager().getItemBuyCount(item.getId()) + " kali");
        }
        
        if (plugin.getStatisticsManager().getItemSellCount(item.getId()) > 0) {
            infoLore.add(ChatColor.GRAY + "Dijual: " + ChatColor.WHITE + 
                         plugin.getStatisticsManager().getItemSellCount(item.getId()) + " kali");
        }
        
        ItemStack infoItem = new ItemBuilder(Material.BOOK)
            .name(ChatColor.YELLOW + "Informasi Item")
            .lore(infoLore)
            .build();
        
        inventory.setItem(11, infoItem);
        
        // Harga beli (jika tersedia)
        if (item.canBuy()) {
            List<String> buyLore = new ArrayList<>();
            String buyFormat = String.format("%.2f", item.getBuyPrice());
            
            buyLore.add(ChatColor.GRAY + "Harga per unit: " + ChatColor.GREEN + 
                        buyFormat + " coin" + (item.getBuyPrice() != 1 ? "s" : ""));
            
            buyLore.add("");
            buyLore.add(ChatColor.GRAY + "Saldo Anda: " + ChatColor.GREEN + 
                        String.format("%.2f", plugin.getEconomy().getBalance(player)));
            
            buyLore.add("");
            buyLore.add(ChatColor.YELLOW + "Klik untuk membeli " + item.getAmount() + "x item");
            
            ItemStack buyItem = new ItemBuilder(Material.EMERALD)
                .name(ChatColor.GREEN + "Beli Item")
                .lore(buyLore)
                .build();
            
            inventory.setItem(29, buyItem);
            
            // Tombol beli stack
            ItemStack buyStackItem = new ItemBuilder(Material.EMERALD_BLOCK)
                .name(ChatColor.GREEN + "Beli 64x Item")
                .lore(
                    ChatColor.GRAY + "Harga total: " + ChatColor.GREEN + 
                    String.format("%.2f", item.getBuyPrice() * 64 / item.getAmount()) + " coins",
                    "",
                    ChatColor.YELLOW + "Klik untuk membeli 64x " + 
                    ChatColor.stripColor(ChatColor.translateAlternateColorCodes('&', item.getName()))
                )
                .build();
            
            inventory.setItem(30, buyStackItem);
            
            // Tombol beli jumlah kustom
            ItemStack customBuyButton = new ItemBuilder(Material.COMPASS)
                .name(ChatColor.GREEN + "Beli Jumlah Kustom")
                .lore(
                    ChatColor.GRAY + "Klik untuk menentukan jumlah",
                    ChatColor.GRAY + "yang ingin dibeli",
                    "",
                    ChatColor.YELLOW + "Harga per unit: " + ChatColor.WHITE + 
                        String.format("%.2f", item.getBuyPrice() / item.getAmount())
                )
                .build();
            
            inventory.setItem(31, customBuyButton);
        }
        
        // Harga jual (jika tersedia)
        if (item.canSell()) {
            List<String> sellLore = new ArrayList<>();
            String sellFormat = String.format("%.2f", item.getSellPrice());
            
            sellLore.add(ChatColor.GRAY + "Harga per unit: " + ChatColor.GOLD + 
                         sellFormat + " coin" + (item.getSellPrice() != 1 ? "s" : ""));
            
            // Cek berapa banyak item yang dimiliki player
            try {
                Material itemMaterial = Material.valueOf(item.getMaterial());
                int playerItemCount = ShopUtils.countItems(player, itemMaterial);
                sellLore.add("");
                sellLore.add(ChatColor.GRAY + "Anda memiliki: " + ChatColor.WHITE + 
                             playerItemCount + "x " + itemMaterial.name());
            } catch (IllegalArgumentException ignored) {}
            
            // Info harga jual (jika ada)
            double basePrice = item.getSellPrice();
            double eventMultiplier = 1.0;
            
            // Cek apakah ada event bonus
            if (plugin.getEventManager().hasActiveEvent()) {
                eventMultiplier = plugin.getEventManager().getSellPriceMultiplier(item, category);
            }
            
            if (eventMultiplier > 1.0) {
                // Harga dengan bonus
                double boostedPrice = basePrice * eventMultiplier;
                int bonusPercent = (int)((eventMultiplier - 1.0) * 100);
                
                // Tampilkan harga asli dan harga dengan bonus
                sellLore.add(ChatColor.GOLD + "Harga Jual: " + ChatColor.YELLOW + 
                        StatisticsManager.formatPrice(basePrice) + ChatColor.GREEN + 
                        " → " + ChatColor.GREEN + StatisticsManager.formatPrice(boostedPrice) + 
                        ChatColor.YELLOW + " (+" + bonusPercent + "%)");
            } else {
                // Tampilkan harga normal
                sellLore.add(ChatColor.GOLD + "Harga Jual: " + ChatColor.YELLOW + 
                        StatisticsManager.formatPrice(basePrice));
            }
            
            sellLore.add("");
            sellLore.add(ChatColor.YELLOW + "Klik untuk menjual " + item.getAmount() + "x item");
            
            ItemStack sellItem = new ItemBuilder(Material.GOLD_INGOT)
                .name(ChatColor.GOLD + "Jual Item")
                .lore(sellLore)
                .build();
            
            inventory.setItem(33, sellItem);
            
            // Tombol jual semua
            ItemStack sellAllItem = new ItemBuilder(Material.GOLD_BLOCK)
                .name(ChatColor.GOLD + "Jual Semua Item")
                .lore(
                    ChatColor.GRAY + "Menjual semua item sejenis",
                    ChatColor.GRAY + "yang ada di inventory Anda.",
                    "",
                    ChatColor.YELLOW + "Klik untuk menjual semua " + 
                    ChatColor.stripColor(ChatColor.translateAlternateColorCodes('&', item.getName()))
                )
                .build();
            
            inventory.setItem(32, sellAllItem);
            
            // Tombol jual jumlah kustom
            ItemStack customSellButton = new ItemBuilder(Material.COMPASS)
                .name(ChatColor.GOLD + "Jual Jumlah Kustom")
                .lore(
                    ChatColor.GRAY + "Klik untuk menentukan jumlah",
                    ChatColor.GRAY + "yang ingin dijual",
                    "",
                    ChatColor.YELLOW + "Harga per unit: " + ChatColor.WHITE + 
                        String.format("%.2f", item.getSellPrice() / item.getAmount())
                )
                .build();
            
            inventory.setItem(34, customSellButton);
        }
        
        // Tombol kembali
        ItemStack backButton = new ItemBuilder(Material.OAK_DOOR)
            .name(ChatColor.RED + "« Kembali")
            .lore(ChatColor.GRAY + "Kembali ke kategori")
            .build();
        
        inventory.setItem(4 * 9 + 4, backButton);
    }
    
    public void open() {
        player.openInventory(inventory);
    }
    
    public ShopItem getItem() {
        return item;
    }
    
    public Category getCategory() {
        return category;
    }
    
    @Override
    public Inventory getInventory() {
        return inventory;
    }
}