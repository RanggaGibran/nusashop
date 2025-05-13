package id.nusashop.gui;

import id.nusashop.NusaShop;
import id.nusashop.models.Category;
import id.nusashop.models.ShopItem;
import id.nusashop.utils.ItemBuilder;
import id.nusashop.utils.TextStyle;

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
import java.util.HashMap;
import java.util.List;
import java.util.Map;

/**
 * GUI untuk menampilkan item dalam kategori tertentu
 */
public class CategoryShopGUI implements InventoryHolder {
    private final NusaShop plugin;
    private final Player player;
    private final Category category;
    private final Inventory inventory;
    
    private final Map<Integer, ShopItem> slotMappings = new HashMap<>();
    private int page = 0;
    private static final int ITEMS_PER_PAGE = 28; // 4 rows of 7 items (excluding border)
    
    public CategoryShopGUI(NusaShop plugin, Player player, Category category) {
        this.plugin = plugin;
        this.player = player;
        this.category = category;
        
        // Buat inventory dengan ukuran yang tepat untuk kategori ini
        String title = ChatColor.translateAlternateColorCodes('&', 
                "&8" + category.getName().replace("&", ""));
        
        this.inventory = Bukkit.createInventory(this, 6 * 9, title);
        
        setupInventory();
    }
    
    private void setupInventory() {
        // Bersihkan inventory
        inventory.clear();
        slotMappings.clear();
        
        // Item background untuk dekorasi - Gunakan gradient warna
        ItemStack topBorderItem = new ItemBuilder(Material.LIGHT_BLUE_STAINED_GLASS_PANE)
            .name(" ")
            .build();
        
        ItemStack middleBorderItem = new ItemBuilder(Material.BLUE_STAINED_GLASS_PANE)
            .name(" ")
            .build();
        
        ItemStack bottomBorderItem = new ItemBuilder(Material.PURPLE_STAINED_GLASS_PANE)
            .name(" ")
            .build();
        
        // Border atas dengan gradient
        for (int i = 0; i < 9; i++) {
            inventory.setItem(i, topBorderItem);
        }
        
        // Border bawah dengan gradient berbeda
        for (int i = 0; i < 9; i++) {
            inventory.setItem(5 * 9 + i, bottomBorderItem);
        }
        
        // Border kiri dan kanan
        for (int i = 1; i < 5; i++) {
            inventory.setItem(i * 9, middleBorderItem); // Kolom kiri
            inventory.setItem(i * 9 + 8, middleBorderItem); // Kolom kanan
        }
        
        // Header item - ikon kategori dengan design lebih menarik
        Material categoryMaterial;
        try {
            categoryMaterial = Material.valueOf(category.getIconMaterial().toUpperCase());
        } catch (IllegalArgumentException e) {
            categoryMaterial = Material.CHEST;
        }
        
        // Gunakan small caps untuk nama kategori
        String categoryName = ChatColor.translateAlternateColorCodes('&', category.getName());
        String smallCapsName = TextStyle.toSmallCaps(ChatColor.stripColor(categoryName));
        
        // Header kategori dengan design yang ditingkatkan
        ItemStack categoryItem = new ItemBuilder(categoryMaterial)
            .name(ChatColor.GOLD + "" + ChatColor.BOLD + "« " + smallCapsName + " »")
            .lore(
                ChatColor.GRAY + "✦ " + TextStyle.toSmallCaps("items") + ": " + 
                ChatColor.YELLOW + category.getItems().size(),
                "",
                ChatColor.GRAY + TextStyle.toSmallCaps("browse through the available items")
            )
            .addFlags(ItemFlag.HIDE_ATTRIBUTES)
            .addFlags(ItemFlag.HIDE_ENCHANTS)
            .enchant(Enchantment.LUCK_OF_THE_SEA, 1)
            .glow(true)
            .build();
        
        inventory.setItem(4, categoryItem);
        
        // Tampilkan item sesuai halaman
        List<ShopItem> items = category.getItems();
        int totalPages = calculateTotalPages(items);
        
        // Batas halaman
        if (page >= totalPages) page = totalPages - 1;
        if (page < 0) page = 0;
        
        int startIndex = page * ITEMS_PER_PAGE;
        int endIndex = Math.min(startIndex + ITEMS_PER_PAGE, items.size());
        
        int slot = 10; // Start dari baris kedua, kolom kedua
        for (int i = startIndex; i < endIndex; i++) {
            // Lewati slot batas
            if (slot % 9 == 0) slot++; // Skip kolom kiri
            if (slot % 9 == 8) slot += 2; // Skip kolom kanan dan lanjut ke baris berikutnya
            
            if (slot >= 5 * 9) break; // Batas bawah sebelum baris navigasi
            
            ShopItem item = items.get(i);
            ItemStack displayItem = createItemDisplay(item);
            
            inventory.setItem(slot, displayItem);
            slotMappings.put(slot, item);
            slot++;
        }
        
        // Tombol navigasi halaman dengan desain yang ditingkatkan
        if (totalPages > 1) {
            // Tombol halaman sebelumnya - hanya jika tidak di halaman pertama
            if (page > 0) {
                ItemStack prevItem = new ItemBuilder(Material.ARROW)
                    .name(ChatColor.YELLOW + "« " + TextStyle.toSmallCaps("previous page"))
                    .lore(
                        ChatColor.GRAY + TextStyle.toSmallCaps("page") + " " + page + " " + 
                        TextStyle.toSmallCaps("of") + " " + totalPages
                    )
                    .build();
                
                inventory.setItem(5 * 9 + 3, prevItem);
            }
            
            // Info halaman dengan desain yang lebih baik
            ItemStack pageInfo = new ItemBuilder(Material.MAP)
                .name(
                    ChatColor.GOLD + TextStyle.toSmallCaps("page") + " " + 
                    ChatColor.WHITE + (page + 1) + ChatColor.GOLD + "/" + ChatColor.WHITE + totalPages
                )
                .glow(true)
                .addFlags(ItemFlag.HIDE_ENCHANTS)
                .build();
            
            inventory.setItem(5 * 9 + 4, pageInfo);
            
            // Tombol halaman selanjutnya - hanya jika tidak di halaman terakhir
            if (page < totalPages - 1) {
                ItemStack nextItem = new ItemBuilder(Material.ARROW)
                    .name(ChatColor.YELLOW + TextStyle.toSmallCaps("next page") + " »")
                    .lore(
                        ChatColor.GRAY + TextStyle.toSmallCaps("page") + " " + (page + 2) + " " + 
                        TextStyle.toSmallCaps("of") + " " + totalPages
                    )
                    .build();
                
                inventory.setItem(5 * 9 + 5, nextItem);
            }
        }
        
        // Tombol kembali dengan desain yang lebih menarik
        ItemStack backButton = new ItemBuilder(Material.OAK_DOOR)
            .name(ChatColor.RED + "« " + TextStyle.toSmallCaps("back to menu"))
            .lore(ChatColor.GRAY + TextStyle.toSmallCaps("return to main shop"))
            .build();
        
        inventory.setItem(5 * 9, backButton);
    }
    
    private ItemStack createItemDisplay(ShopItem item) {
        Material material;
        try {
            material = Material.valueOf(item.getMaterial().toUpperCase());
        } catch (IllegalArgumentException e) {
            material = Material.BARRIER; // Default jika material tidak valid
        }
        
        // Gunakan small caps untuk tampilan item
        String itemName = ChatColor.translateAlternateColorCodes('&', item.getName());
        
        ItemBuilder builder = new ItemBuilder(material)
            .name(itemName)
            .amount(item.getAmount());
        
        List<String> lore = new ArrayList<>();
        
        // Informasi harga dengan format yang lebih menarik
        if (item.canBuy()) {
            String buyFormat = String.format("%.2f", item.getBuyPrice());
            lore.add(ChatColor.GREEN + "✦ " + TextStyle.toSmallCaps("buy") + ": " + 
                    plugin.getConfigManager().getConfig().getString("economy.format", "&e%amount% Coins")
                        .replace("%amount%", buyFormat)
                        .replace("&", "§"));
        }
        
        if (item.canSell()) {
            // Harga dasar per unit
            double basePrice = item.getSellPrice();
            double eventMultiplier = 1.0;
            
            // Cek apakah ada event bonus
            if (plugin.getEventManager().hasActiveEvent()) {
                eventMultiplier = plugin.getEventManager().getSellPriceMultiplier(item, category);
            }
            
            String sellFormat = String.format("%.2f", basePrice);
            
            // Tampilkan harga dasar
            String sellText = ChatColor.GOLD + "✦ " + TextStyle.toSmallCaps("sell") + ": " + 
                    plugin.getConfigManager().getConfig().getString("economy.format", "&e%amount% Coins")
                        .replace("%amount%", sellFormat)
                        .replace("&", "§");
            
            // Tambahkan informasi bonus jika ada
            if (eventMultiplier > 1.0) {
                int bonusPercent = (int)((eventMultiplier - 1.0) * 100);
                double boostedPrice = basePrice * eventMultiplier;
                String boostedFormat = String.format("%.2f", boostedPrice);
                
                sellText += ChatColor.YELLOW + " → " + 
                        plugin.getConfigManager().getConfig().getString("economy.format", "&e%amount% Coins")
                            .replace("%amount%", boostedFormat)
                            .replace("&", "§") +
                        ChatColor.GREEN + " (+" + bonusPercent + "%)";
            }
            
            lore.add(sellText);
        }
        
        // Statistik item dengan design yang lebih baik
        int buyCount = plugin.getStatisticsManager().getItemBuyCount(item.getId());
        int sellCount = plugin.getStatisticsManager().getItemSellCount(item.getId());
        
        if (buyCount > 0 || sellCount > 0) {
            lore.add("");
            if (buyCount > 0) {
                lore.add(ChatColor.GRAY + "⦿ " + TextStyle.toSmallCaps("bought") + ": " + 
                         ChatColor.WHITE + buyCount + " " + TextStyle.toSmallCaps("times"));
            }
            if (sellCount > 0) {
                lore.add(ChatColor.GRAY + "⦿ " + TextStyle.toSmallCaps("sold") + ": " + 
                         ChatColor.WHITE + sellCount + " " + TextStyle.toSmallCaps("times"));
            }
        }
        
        // Instruksi dengan design yang lebih jelas
        lore.add("");
        if (item.canBuy()) {
            lore.add(ChatColor.YELLOW + "➤ " + ChatColor.WHITE + TextStyle.toSmallCaps("left click to buy"));
        }
        if (item.canSell()) {
            lore.add(ChatColor.YELLOW + "➤ " + ChatColor.WHITE + TextStyle.toSmallCaps("right click to sell"));
        }
        lore.add(ChatColor.YELLOW + "➤ " + ChatColor.WHITE + TextStyle.toSmallCaps("shift+click for details"));
        
        builder.lore(lore);
        
        return builder.build();
    }
    
    /**
     * Menghitung total halaman berdasarkan jumlah item
     * @param items List item yang akan ditampilkan
     * @return Jumlah halaman yang dibutuhkan
     */
    private int calculateTotalPages(List<ShopItem> items) {
        return Math.max(1, (int) Math.ceil(items.size() / (double) ITEMS_PER_PAGE));
    }
    
    /**
     * Beralih ke halaman berikutnya
     */
    public void nextPage() {
        int totalPages = calculateTotalPages(category.getItems());
        
        if (page < totalPages - 1) {
            page++;
            setupInventory();
        }
    }
    
    /**
     * Beralih ke halaman sebelumnya
     */
    public void previousPage() {
        if (page > 0) {
            page--;
            setupInventory();
        }
    }
    
    /**
     * Membuka GUI
     */
    public void open() {
        player.openInventory(inventory);
    }
    
    /**
     * Mendapatkan kategori yang ditampilkan
     */
    public Category getCategory() {
        return category;
    }
    
    /**
     * Mendapatkan item pada slot tertentu
     */
    public ShopItem getItemAt(int slot) {
        return slotMappings.get(slot);
    }
    
    /**
     * Mendapatkan jumlah total halaman
     */
    public int getTotalPages() {
        return calculateTotalPages(category.getItems());
    }
    
    /**
     * Mendapatkan halaman saat ini
     */
    public int getCurrentPage() {
        return page;
    }
    
    @Override
    public Inventory getInventory() {
        return inventory;
    }
}