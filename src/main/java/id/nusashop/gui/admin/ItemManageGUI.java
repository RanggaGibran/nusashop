package id.nusashop.gui.admin;

import id.nusashop.NusaShop;
import id.nusashop.models.Category;
import id.nusashop.models.ShopItem;
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
 * GUI untuk mengelola item dalam kategori
 */
public class ItemManageGUI implements InventoryHolder {
    private final NusaShop plugin;
    private final Player player;
    private final Inventory inventory;
    private final Category category;
    private final Map<Integer, ShopItem> slotMap = new HashMap<>();
    
    private int page = 0;
    private static final int ITEMS_PER_PAGE = 36;
    
    public ItemManageGUI(NusaShop plugin, Player player, Category category) {
        this.plugin = plugin;
        this.player = player;
        this.category = category;
        
        String title = ChatColor.translateAlternateColorCodes('&', 
                "&8Item: " + ChatColor.stripColor(ChatColor.translateAlternateColorCodes('&', category.getName())));
        this.inventory = Bukkit.createInventory(this, 6 * 9, title);
        
        setupInventory();
    }
    
    private void setupInventory() {
        // Bersihkan inventory
        inventory.clear();
        slotMap.clear();
        
        // Tampilkan item di kategori ini
        List<ShopItem> items = category.getItems();
        int totalPages = (int) Math.ceil(items.size() / (double) ITEMS_PER_PAGE);
        
        int startIndex = page * ITEMS_PER_PAGE;
        int endIndex = Math.min(startIndex + ITEMS_PER_PAGE, items.size());
        
        int slot = 0;
        for (int i = startIndex; i < endIndex; i++) {
            ShopItem item = items.get(i);
            
            Material material;
            try {
                material = Material.valueOf(item.getMaterial().toUpperCase());
            } catch (IllegalArgumentException e) {
                material = Material.BARRIER;
            }
            
            ItemStack displayItem = new ItemBuilder(material)
                .name(ChatColor.translateAlternateColorCodes('&', item.getName()))
                .amount(item.getAmount())
                .lore(
                    ChatColor.GRAY + "ID: " + item.getId(),
                    ChatColor.GRAY + "Material: " + item.getMaterial(),
                    ChatColor.GREEN + "Harga Beli: " + (item.canBuy() ? item.getBuyPrice() : "Tidak dapat dibeli"),
                    ChatColor.GOLD + "Harga Jual: " + (item.canSell() ? item.getSellPrice() : "Tidak dapat dijual"),
                    "",
                    ChatColor.YELLOW + "Klik Kiri: " + ChatColor.GRAY + "Edit item",
                    ChatColor.YELLOW + "Klik Kanan: " + ChatColor.GRAY + "Hapus item"
                )
                .build();
            
            inventory.setItem(slot, displayItem);
            slotMap.put(slot, item);
            slot++;
        }
        
        // Item untuk navigasi di baris bawah
        int bottomRow = 5 * 9;
        
        // Tombol halaman sebelumnya
        if (page > 0) {
            inventory.setItem(bottomRow + 3, new ItemBuilder(Material.ARROW)
                .name(ChatColor.YELLOW + "« Halaman Sebelumnya")
                .build()
            );
        }
        
        // Info halaman
        inventory.setItem(bottomRow + 4, new ItemBuilder(Material.PAPER)
            .name(ChatColor.GOLD + "Halaman " + (page + 1) + "/" + Math.max(1, totalPages))
            .build()
        );
        
        // Tombol halaman selanjutnya
        if (page < totalPages - 1) {
            inventory.setItem(bottomRow + 5, new ItemBuilder(Material.ARROW)
                .name(ChatColor.YELLOW + "Halaman Selanjutnya »")
                .build()
            );
        }
        
        // Tombol tambah item baru
        inventory.setItem(bottomRow + 1, new ItemBuilder(Material.LIME_CONCRETE)
            .name(ChatColor.GREEN + "Tambah Item Baru")
            .lore(ChatColor.GRAY + "Klik untuk membuat item baru")
            .build()
        );
        
        // Tombol kembali
        inventory.setItem(bottomRow + 7, new ItemBuilder(Material.OAK_DOOR)
            .name(ChatColor.RED + "« Kembali")
            .lore(ChatColor.GRAY + "Kembali ke menu kategori")
            .build()
        );
    }
    
    /**
     * Membuka halaman GUI
     */
    public void open() {
        player.openInventory(inventory);
    }
    
    /**
     * Pindah ke halaman berikutnya
     */
    public void nextPage() {
        int totalItems = category.getItems().size();
        int totalPages = (int) Math.ceil(totalItems / (double) ITEMS_PER_PAGE);
        
        if (page < totalPages - 1) {
            page++;
            setupInventory();
        }
    }
    
    /**
     * Pindah ke halaman sebelumnya
     */
    public void previousPage() {
        if (page > 0) {
            page--;
            setupInventory();
        }
    }
    
    @Override
    public Inventory getInventory() {
        return inventory;
    }
    
    /**
     * Mendapatkan kategori yang sedang dilihat
     */
    public Category getCategory() {
        return category;
    }
    
    /**
     * Mendapatkan item pada slot tertentu
     */
    public ShopItem getItemAt(int slot) {
        return slotMap.get(slot);
    }
}