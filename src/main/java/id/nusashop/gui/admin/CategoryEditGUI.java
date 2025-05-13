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

/**
 * GUI untuk mengedit kategori shop
 */
public class CategoryEditGUI implements InventoryHolder {
    private final NusaShop plugin;
    private final Player player;
    private final Inventory inventory;
    private final Category category;
    private final boolean isNew;
    
    /**
     * Membuat GUI edit kategori
     * @param plugin Instance plugin
     * @param player Player yang mengedit
     * @param category Kategori yang diedit (null jika membuat baru)
     */
    public CategoryEditGUI(NusaShop plugin, Player player, Category category) {
        this.plugin = plugin;
        this.player = player;
        this.category = category;
        this.isNew = category == null;
        
        String title = ChatColor.translateAlternateColorCodes('&', 
                isNew ? "&8Tambah Kategori" : "&8Edit Kategori");
        this.inventory = Bukkit.createInventory(this, 3 * 9, title);
        
        setupInventory();
    }
    
    private void setupInventory() {
        // Isi background dengan glass pane
        ItemStack backgroundItem = new ItemBuilder(Material.GRAY_STAINED_GLASS_PANE)
            .name(" ")
            .build();
            
        for (int i = 0; i < inventory.getSize(); i++) {
            inventory.setItem(i, backgroundItem);
        }
        
        // ID Kategori
        ItemStack idItem = new ItemBuilder(Material.NAME_TAG)
            .name(ChatColor.GOLD + "ID Kategori")
            .lore(
                ChatColor.GRAY + "ID Saat Ini: " + (isNew ? "belum diatur" : category.getId()),
                "",
                ChatColor.YELLOW + "Klik untuk mengubah ID"
            )
            .build();
        inventory.setItem(10, idItem);
        
        // Nama Kategori
        ItemStack nameItem = new ItemBuilder(Material.PAPER)
            .name(ChatColor.GOLD + "Nama Kategori")
            .lore(
                ChatColor.GRAY + "Nama Saat Ini: " + (isNew ? "belum diatur" : category.getName()),
                "",
                ChatColor.YELLOW + "Klik untuk mengubah nama"
            )
            .build();
        inventory.setItem(12, nameItem);
        
        // Icon Kategori
        Material iconMaterial = Material.CHEST;
        if (!isNew) {
            try {
                iconMaterial = Material.valueOf(category.getIconMaterial().toUpperCase());
            } catch (IllegalArgumentException ignored) {}
        }
        
        ItemStack iconItem = new ItemBuilder(iconMaterial)
            .name(ChatColor.GOLD + "Icon Kategori")
            .lore(
                ChatColor.GRAY + "Icon Saat Ini: " + (isNew ? "CHEST" : category.getIconMaterial()),
                "",
                ChatColor.YELLOW + "Klik untuk mengubah icon"
            )
            .build();
        inventory.setItem(14, iconItem);
        
        // Deskripsi Kategori
        ItemStack descItem = new ItemBuilder(Material.BOOK)
            .name(ChatColor.GOLD + "Deskripsi Kategori")
            .lore(
                ChatColor.GRAY + "Deskripsi Saat Ini: " + (isNew ? "belum diatur" : category.getDescription()),
                "",
                ChatColor.YELLOW + "Klik untuk mengubah deskripsi"
            )
            .build();
        inventory.setItem(16, iconItem);
        
        // Tombol Simpan
        ItemStack saveItem = new ItemBuilder(Material.EMERALD_BLOCK)
            .name(ChatColor.GREEN + "Simpan Perubahan")
            .lore(
                ChatColor.GRAY + "Klik untuk menyimpan kategori"
            )
            .build();
        inventory.setItem(22, saveItem);
        
        // Tombol Kembali
        ItemStack backItem = new ItemBuilder(Material.ARROW)
            .name(ChatColor.RED + "« Kembali")
            .lore(
                ChatColor.GRAY + "Kembali tanpa menyimpan"
            )
            .build();
        inventory.setItem(18, backItem);
    }
    
    public void open() {
        player.openInventory(inventory);
    }
    
    @Override
    public Inventory getInventory() {
        return inventory;
    }
    
    /**
     * Apakah kategori baru
     */
    public boolean isNew() {
        return isNew;
    }
    
    /**
     * Mendapatkan kategori yang diedit
     */
    public Category getCategory() {
        return category;
    }
}