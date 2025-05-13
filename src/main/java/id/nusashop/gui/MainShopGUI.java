package id.nusashop.gui;

import id.nusashop.NusaShop;
import id.nusashop.models.Category;
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
 * GUI utama untuk menampilkan kategori shop
 */
public class MainShopGUI implements InventoryHolder {
    private final NusaShop plugin;
    private final Player player;
    private final Inventory inventory;
    private final Map<Integer, Category> categorySlotMap = new HashMap<>();
    
    // Untuk pagination jika kategori banyak
    private int page = 0;
    private static final int CATEGORIES_PER_PAGE = 21; // 3 baris x 7 kolom (tidak termasuk border)
    
    public MainShopGUI(NusaShop plugin, Player player) {
        this.plugin = plugin;
        this.player = player;
        
        // Buat inventory dengan ukuran yang tepat
        String title = ChatColor.translateAlternateColorCodes('&', 
                plugin.getConfigManager().getConfig().getString("gui.main-title", "&#4a89dc&lNusa&b&lShop"));
        
        // Selalu gunakan 6 baris untuk main menu agar lebih lebar
        this.inventory = Bukkit.createInventory(this, 6 * 9, title);
        
        setupInventory();
    }
    
    private void setupInventory() {
        // Bersihkan inventory
        inventory.clear();
        categorySlotMap.clear();
        
        List<Category> categories = plugin.getShopManager().getCategories();
        int totalPages = calculateTotalPages(categories);
        
        // Batas halaman
        if (page >= totalPages) page = totalPages - 1;
        if (page < 0) page = 0;
        
        // Isi background dengan gradient yang lebih menarik
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
            inventory.setItem(i * 9, middleBorderItem);
            inventory.setItem(i * 9 + 8, middleBorderItem);
        }
        
        // Header kustom dengan desain yang lebih menarik
        ItemStack headerItem = new ItemBuilder(Material.NETHER_STAR)
            .name(ChatColor.AQUA + "" + ChatColor.BOLD + "✦ " + TextStyle.toSmallCaps("nusashop") + " ✦")
            .lore(
                ChatColor.GRAY + "⦿ " + TextStyle.toSmallCaps("select a category to browse"),
                ChatColor.GRAY + "⦿ " + TextStyle.toSmallCaps("available items for purchase")
            )
            .addFlags(ItemFlag.HIDE_ATTRIBUTES)
            .addFlags(ItemFlag.HIDE_ENCHANTS)
            .enchant(Enchantment.UNBREAKING, 1)
            .glow(true)
            .build();
        
        inventory.setItem(4, headerItem);
        
        // Event item - dengan desain yang lebih menarik
        if (plugin.getEventManager().hasActiveEvent()) {
            List<String> eventLore = new ArrayList<>();
            eventLore.add(ChatColor.GRAY + plugin.getEventManager().getEventDescription());
            eventLore.add("");
            eventLore.add(ChatColor.YELLOW + "⏱ " + TextStyle.toSmallCaps("ends in") + ": " + 
                    ChatColor.WHITE + plugin.getEventManager().getRemainingTimeFormatted());
            eventLore.add("");
            eventLore.add(ChatColor.LIGHT_PURPLE + "✧ " + TextStyle.toSmallCaps("special discounts available!"));
            
            ItemStack eventItem = new ItemBuilder(Material.ENDER_EYE)
                .name(ChatColor.LIGHT_PURPLE + "" + ChatColor.BOLD + "⚡ " + 
                     plugin.getEventManager().getEventName())
                .lore(eventLore)
                .enchant(Enchantment.UNBREAKING, 1)
                .addFlags(ItemFlag.HIDE_ENCHANTS)
                .glow(true)
                .build();
            
            inventory.setItem(8, eventItem);
        }
        
        // Statistik Shop dengan layout yang lebih bersih
        if (plugin.getServer().getPluginManager().isPluginEnabled("PlaceholderAPI")) {
            ItemStack statsItem = new ItemBuilder(Material.BOOK)
                .name(ChatColor.GOLD + "" + ChatColor.BOLD + "✯ " + TextStyle.toSmallCaps("shop statistics") + " ✯")
                .lore(
                    ChatColor.GRAY + "⦿ " + TextStyle.toSmallCaps("transactions") + ": " + ChatColor.WHITE + 
                        plugin.getStatisticsManager().getTotalTransactions(),
                    ChatColor.GRAY + "⦿ " + TextStyle.toSmallCaps("popular item") + ": " + ChatColor.WHITE + 
                        plugin.getStatisticsManager().getMostBoughtItem(),
                    "",
                    ChatColor.YELLOW + "» " + TextStyle.toSmallCaps("updated regularly")
                )
                .glow(true)
                .build();
            
            inventory.setItem(5 * 9 + 8, statsItem);
        }
        
        // "Jual Item" button di pojok kiri bawah
        ItemStack sellItem = new ItemBuilder(Material.GOLD_INGOT)
            .name(ChatColor.GOLD + "" + ChatColor.BOLD + "$ " + TextStyle.toSmallCaps("sell items") + " $")
            .lore(
                ChatColor.GRAY + "⦿ " + TextStyle.toSmallCaps("quickly sell items from"),
                ChatColor.GRAY + "  " + TextStyle.toSmallCaps("your inventory"),
                "",
                ChatColor.YELLOW + "» " + TextStyle.toSmallCaps("click to open sell menu")
            )
            .build();
            
        inventory.setItem(5 * 9, sellItem);
        
        // Hitung kategori yang perlu ditampilkan
        int startIndex = page * CATEGORIES_PER_PAGE;
        int endIndex = Math.min(startIndex + CATEGORIES_PER_PAGE, categories.size());
        
        // Tampilkan kategori dalam grid yang lebih lebar (3 baris x 7 kolom)
        int slot = 10; // Mulai dari baris kedua, kolom kedua
        for (int i = startIndex; i < endIndex; i++) {
            Category category = categories.get(i);
            
            // Lewati slot batas
            if (slot % 9 == 0) slot++; // Skip kolom kiri
            if (slot % 9 == 8) slot += 2; // Skip kolom kanan dan lanjut ke baris berikutnya
            
            if (slot >= 5 * 9) break; // Jangan melewati batas bawah
            
            ItemStack categoryItem = createCategoryItem(category);
            inventory.setItem(slot, categoryItem);
            categorySlotMap.put(slot, category);
            slot++;
        }
        
        // Tombol navigasi halaman dengan desain yang lebih menarik
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
    }
    
    private ItemStack createCategoryItem(Category category) {
        Material material;
        try {
            material = Material.valueOf(category.getIconMaterial().toUpperCase());
        } catch (IllegalArgumentException e) {
            material = Material.CHEST; // Default jika material tidak valid
        }
        
        String categoryName = ChatColor.translateAlternateColorCodes('&', category.getName());
        String categoryNameFormatted = ChatColor.GOLD + "" + ChatColor.BOLD + categoryName;
        
        // Buat item dengan efek berkilau dan layout yang lebih bersih
        return new ItemBuilder(material)
            .name(categoryNameFormatted)
            .lore(getCategoryDescription(category))
            .addFlags(ItemFlag.HIDE_ATTRIBUTES)
            .addFlags(ItemFlag.HIDE_ENCHANTS)
            .enchant(Enchantment.LUCK_OF_THE_SEA, 1)
            .glow(true)
            .build();
    }
    
    private List<String> getCategoryDescription(Category category) {
        List<String> lore = new ArrayList<>();
        
        // Tambahkan deskripsi kategori jika ada
        if (!category.getDescription().isEmpty()) {
            for (String line : category.getDescription().split("\n")) {
                lore.add(ChatColor.GRAY + ChatColor.translateAlternateColorCodes('&', line));
            }
        }
        
        // Informasi jumlah item dengan layout yang lebih bersih
        lore.add("");
        lore.add(ChatColor.GRAY + "⦿ " + TextStyle.toSmallCaps("items") + ": " + 
                 ChatColor.WHITE + category.getItems().size());
        
        // Statistik kategori jika tersedia
        if (plugin.getStatisticsManager().getCategoryBuyCount(category.getId()) > 0) {
            int buyCount = plugin.getStatisticsManager().getCategoryBuyCount(category.getId());
            lore.add(ChatColor.GRAY + "⦿ " + TextStyle.toSmallCaps("purchases") + ": " + 
                     ChatColor.WHITE + buyCount);
        }
        
        lore.add("");
        lore.add(ChatColor.YELLOW + "➤ " + ChatColor.WHITE + TextStyle.toSmallCaps("click to browse items"));
        
        return lore;
    }
    
    /**
     * Menghitung total halaman berdasarkan jumlah kategori
     */
    private int calculateTotalPages(List<Category> categories) {
        return Math.max(1, (int) Math.ceil(categories.size() / (double) CATEGORIES_PER_PAGE));
    }
    
    /**
     * Beralih ke halaman berikutnya
     */
    public void nextPage() {
        int totalPages = calculateTotalPages(plugin.getShopManager().getCategories());
        
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
     * Mendapatkan kategori pada slot tertentu
     */
    public Category getCategoryAt(int slot) {
        return categorySlotMap.get(slot);
    }
    
    /**
     * Membuka GUI
     */
    public void open() {
        player.openInventory(inventory);
    }
    
    /**
     * Mendapatkan jumlah total halaman
     */
    public int getTotalPages() {
        return calculateTotalPages(plugin.getShopManager().getCategories());
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