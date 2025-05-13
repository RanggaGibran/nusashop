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

import java.util.Arrays;
import java.util.List;

/**
 * GUI admin utama untuk mengelola shop
 */
public class AdminShopGUI implements InventoryHolder {
    private final NusaShop plugin;
    private final Player player;
    private final Inventory inventory;
    
    public AdminShopGUI(NusaShop plugin, Player player) {
        this.plugin = plugin;
        this.player = player;
        
        String title = ChatColor.translateAlternateColorCodes('&', "&8Admin Shop");
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
        
        // Kelola Kategori
        ItemStack categoryItem = new ItemBuilder(Material.BOOKSHELF)
            .name(ChatColor.GOLD + "Kelola Kategori")
            .lore(
                ChatColor.GRAY + "Tambah, edit, dan hapus",
                ChatColor.GRAY + "kategori shop"
            )
            .build();
        inventory.setItem(11, categoryItem);
        
        // Kelola Item
        ItemStack itemsItem = new ItemBuilder(Material.CHEST)
            .name(ChatColor.GOLD + "Kelola Item")
            .lore(
                ChatColor.GRAY + "Tambah, edit, dan hapus",
                ChatColor.GRAY + "item di dalam shop"
            )
            .build();
        inventory.setItem(13, itemsItem);
        
        // Reload Plugin
        ItemStack reloadItem = new ItemBuilder(Material.COMPARATOR)
            .name(ChatColor.RED + "Reload Plugin")
            .lore(
                ChatColor.GRAY + "Muat ulang konfigurasi plugin"
            )
            .build();
        inventory.setItem(15, reloadItem);
    }
    
    public void open() {
        player.openInventory(inventory);
    }
    
    @Override
    public Inventory getInventory() {
        return inventory;
    }
}