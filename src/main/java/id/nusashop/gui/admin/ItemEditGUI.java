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

import java.text.DecimalFormat;

/**
 * GUI untuk mengedit item shop
 */
public class ItemEditGUI implements InventoryHolder {
    private final NusaShop plugin;
    private final Player player;
    private final Inventory inventory;
    private final ShopItem item;
    private final Category category;
    private final boolean isNew;
    
    private static final DecimalFormat PRICE_FORMAT = new DecimalFormat("#,##0.00");
    
    /**
     * Membuat GUI edit item
     * @param plugin Instance plugin
     * @param player Player yang mengedit
     * @param category Kategori item
     * @param item Item yang diedit (null jika membuat baru)
     */
    public ItemEditGUI(NusaShop plugin, Player player, Category category, ShopItem item) {
        this.plugin = plugin;
        this.player = player;
        this.category = category;
        this.item = item;
        this.isNew = item == null;
        
        String title = ChatColor.translateAlternateColorCodes('&', 
                isNew ? "&8Tambah Item" : "&8Edit Item");
        this.inventory = Bukkit.createInventory(this, 4 * 9, title);
        
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
        
        // ID Item
        ItemStack idItem = new ItemBuilder(Material.NAME_TAG)
            .name(ChatColor.GOLD + "ID Item")
            .lore(
                ChatColor.GRAY + "ID Saat Ini: " + (isNew ? "belum diatur" : item.getId()),
                "",
                ChatColor.YELLOW + "Klik untuk mengubah ID"
            )
            .build();
        inventory.setItem(10, idItem);
        
        // Nama Item
        ItemStack nameItem = new ItemBuilder(Material.PAPER)
            .name(ChatColor.GOLD + "Nama Item")
            .lore(
                ChatColor.GRAY + "Nama Saat Ini: " + (isNew ? "belum diatur" : item.getName()),
                "",
                ChatColor.YELLOW + "Klik untuk mengubah nama"
            )
            .build();
        inventory.setItem(11, nameItem);
        
        // Material Item
        Material material = Material.STONE;
        if (!isNew) {
            try {
                material = Material.valueOf(item.getMaterial().toUpperCase());
            } catch (IllegalArgumentException ignored) {}
        }
        
        ItemStack materialItem = new ItemBuilder(material)
            .name(ChatColor.GOLD + "Material Item")
            .lore(
                ChatColor.GRAY + "Material Saat Ini: " + (isNew ? "STONE" : item.getMaterial()),
                "",
                ChatColor.YELLOW + "Klik untuk mengubah material"
            )
            .build();
        inventory.setItem(12, materialItem);
        
        // Harga Beli
        ItemStack buyPriceItem = new ItemBuilder(Material.EMERALD)
            .name(ChatColor.GOLD + "Harga Beli")
            .lore(
                ChatColor.GRAY + "Harga Beli Saat Ini: " + 
                    (isNew ? "0.00" : (item.canBuy() ? PRICE_FORMAT.format(item.getBuyPrice()) : "Tidak dapat dibeli")),
                "",
                ChatColor.YELLOW + "Klik Kiri: " + ChatColor.GRAY + "Set harga beli",
                ChatColor.YELLOW + "Klik Kanan: " + ChatColor.GRAY + "Nonaktifkan pembelian"
            )
            .build();
        inventory.setItem(14, buyPriceItem);
        
        // Harga Jual
        ItemStack sellPriceItem = new ItemBuilder(Material.GOLD_INGOT)
            .name(ChatColor.GOLD + "Harga Jual")
            .lore(
                ChatColor.GRAY + "Harga Jual Saat Ini: " + 
                    (isNew ? "0.00" : (item.canSell() ? PRICE_FORMAT.format(item.getSellPrice()) : "Tidak dapat dijual")),
                "",
                ChatColor.YELLOW + "Klik Kiri: " + ChatColor.GRAY + "Set harga jual",
                ChatColor.YELLOW + "Klik Kanan: " + ChatColor.GRAY + "Nonaktifkan penjualan"
            )
            .build();
        inventory.setItem(15, sellPriceItem);
        
        // Jumlah Item
        ItemStack amountItem = new ItemBuilder(Material.HOPPER)
            .name(ChatColor.GOLD + "Jumlah Item")
            .lore(
                ChatColor.GRAY + "Jumlah Saat Ini: " + (isNew ? "1" : item.getAmount()),
                "",
                ChatColor.YELLOW + "Klik untuk mengubah jumlah"
            )
            .build();
        inventory.setItem(16, amountItem);
        
        // Tombol Simpan
        ItemStack saveItem = new ItemBuilder(Material.EMERALD_BLOCK)
            .name(ChatColor.GREEN + "Simpan Perubahan")
            .lore(
                ChatColor.GRAY + "Klik untuk menyimpan item"
            )
            .build();
        inventory.setItem(31, saveItem);
        
        // Tombol Kembali
        ItemStack backItem = new ItemBuilder(Material.ARROW)
            .name(ChatColor.RED + "« Kembali")
            .lore(
                ChatColor.GRAY + "Kembali tanpa menyimpan"
            )
            .build();
        inventory.setItem(27, backItem);
    }
    
    public void open() {
        player.openInventory(inventory);
    }
    
    @Override
    public Inventory getInventory() {
        return inventory;
    }
    
    /**
     * Apakah item baru
     */
    public boolean isNew() {
        return isNew;
    }
    
    /**
     * Mendapatkan item yang diedit
     */
    public ShopItem getItem() {
        return item;
    }
    
    /**
     * Mendapatkan kategori item
     */
    public Category getCategory() {
        return category;
    }
}