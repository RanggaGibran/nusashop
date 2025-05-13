package id.nusashop.gui.admin;

import id.nusashop.NusaShop;
import id.nusashop.models.Category;
import id.nusashop.utils.ItemBuilder;

import org.bukkit.Bukkit;
import org.bukkit.ChatColor;
import org.bukkit.Material;
import org.bukkit.entity.Player;
import org.bukkit.inventory.Inventory;
import org.bukkit.inventory.InventoryHolder;
import org.bukkit.inventory.ItemStack;

import java.util.HashMap;
import java.util.List;
import java.util.Map;

/**
 * GUI untuk mengelola kategori shop
 */
public class CategoryManageGUI implements InventoryHolder {
    private final NusaShop plugin;
    private final Player player;
    private final Inventory inventory;
    private final Map<Integer, Category> slotMap = new HashMap<>();
    
    public CategoryManageGUI(NusaShop plugin, Player player) {
        this.plugin = plugin;
        this.player = player;
        
        String title = ChatColor.translateAlternateColorCodes('&', "&8Kelola Kategori");
        
        // Buat inventory dengan ukuran yang tepat
        List<Category> categories = plugin.getShopManager().getCategories();
        int rows = Math.min(6, (int) Math.ceil(categories.size() / 7.0) + 1); // +1 untuk tombol tambah dan kembali
        this.inventory = Bukkit.createInventory(this, rows * 9, title);
        
        setupInventory();
    }
    
    private void setupInventory() {
        List<Category> categories = plugin.getShopManager().getCategories();
        
        // Header: dekorasi
        for (int i = 0; i < 9; i++) {
            inventory.setItem(i, new ItemBuilder(Material.BLACK_STAINED_GLASS_PANE).name(" ").build());
        }
        
        // Tampilkan semua kategori
        int slot = 9;
        for (Category category : categories) {
            Material iconMaterial;
            try {
                iconMaterial = Material.valueOf(category.getIconMaterial().toUpperCase());
            } catch (IllegalArgumentException e) {
                iconMaterial = Material.CHEST;
            }
            
            ItemStack item = new ItemBuilder(iconMaterial)
                .name(ChatColor.translateAlternateColorCodes('&', category.getName()))
                .lore(
                    ChatColor.GRAY + "ID: " + category.getId(),
                    ChatColor.GRAY + "Icon: " + category.getIconMaterial(),
                    ChatColor.GRAY + "Items: " + category.getItems().size(),
                    "",
                    ChatColor.YELLOW + "Klik Kiri: " + ChatColor.GRAY + "Edit kategori",
                    ChatColor.YELLOW + "Klik Kanan: " + ChatColor.GRAY + "Hapus kategori",
                    ChatColor.YELLOW + "Shift + Klik Kiri: " + ChatColor.GRAY + "Lihat item"
                )
                .build();
            
            inventory.setItem(slot, item);
            slotMap.put(slot, category);
            slot++;
            
            // Lewati slot di pinggir
            if (slot % 9 == 8) {
                slot += 2;
            }
        }
        
        // Tempatkan tombol tambah kategori di bawah
        int totalRows = inventory.getSize() / 9;
        inventory.setItem((totalRows - 1) * 9 + 2, new ItemBuilder(Material.LIME_CONCRETE)
            .name(ChatColor.GREEN + "Tambah Kategori Baru")
            .lore(ChatColor.GRAY + "Klik untuk membuat kategori baru")
            .build()
        );
        
        // Tombol kembali
        inventory.setItem((totalRows - 1) * 9 + 6, new ItemBuilder(Material.ARROW)
            .name(ChatColor.RED + "« Kembali")
            .lore(ChatColor.GRAY + "Kembali ke menu admin")
            .build()
        );
    }
    
    public void open() {
        player.openInventory(inventory);
    }
    
    @Override
    public Inventory getInventory() {
        return inventory;
    }
    
    /**
     * Mendapatkan kategori di slot tertentu
     */
    public Category getCategoryAt(int slot) {
        return slotMap.get(slot);
    }
}