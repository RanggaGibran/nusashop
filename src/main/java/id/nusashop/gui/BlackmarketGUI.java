package id.nusashop.gui;

import id.nusashop.NusaShop;
import id.nusashop.models.BlackmarketItem;
import id.nusashop.utils.ItemBuilder;
import id.nusashop.utils.Messages;

import org.bukkit.Bukkit;
import org.bukkit.ChatColor;
import org.bukkit.Material;
import org.bukkit.entity.Player;
import org.bukkit.inventory.Inventory;
import org.bukkit.inventory.InventoryHolder;
import org.bukkit.inventory.ItemStack;
import org.bukkit.inventory.meta.ItemMeta;

import java.text.DecimalFormat;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

/**
 * GUI untuk blackmarket
 */
public class BlackmarketGUI implements InventoryHolder {
    private final NusaShop plugin;
    private final Player player;
    private final Inventory inventory;
    private final Map<Integer, BlackmarketItem> slotMappings = new HashMap<>();
    
    private static final DecimalFormat PRICE_FORMAT = new DecimalFormat("#,##0.00");
    
    public BlackmarketGUI(NusaShop plugin, Player player) {
        this.plugin = plugin;
        this.player = player;
        
        String title = plugin.getBlackmarketManager().getGuiTitle();
        int rows = plugin.getBlackmarketManager().getGuiRows();
        this.inventory = Bukkit.createInventory(this, rows * 9, title);
        
        setupInventory();
    }
    
    private void setupInventory() {
        // Bersihkan inventory
        inventory.clear();
        slotMappings.clear();
        
        // Background
        Material backgroundMat;
        try {
            backgroundMat = Material.valueOf(plugin.getBlackmarketManager().getBackgroundMaterial());
        } catch (IllegalArgumentException e) {
            backgroundMat = Material.BLACK_STAINED_GLASS_PANE;
        }
        
        ItemStack backgroundItem = new ItemBuilder(backgroundMat)
            .name(" ")
            .build();
        
        Material borderMat;
        try {
            borderMat = Material.valueOf(plugin.getBlackmarketManager().getBorderMaterial());
        } catch (IllegalArgumentException e) {
            borderMat = Material.GRAY_STAINED_GLASS_PANE;
        }
        
        ItemStack borderItem = new ItemBuilder(borderMat)
            .name(" ")
            .build();
        
        // Isi background
        for (int i = 0; i < inventory.getSize(); i++) {
            inventory.setItem(i, backgroundItem);
        }
        
        // Border atas dan bawah
        int rowSize = 9;
        int rows = inventory.getSize() / rowSize;
        
        for (int i = 0; i < rowSize; i++) {
            // Top row
            inventory.setItem(i, borderItem);
            // Bottom row
            inventory.setItem((rows - 1) * rowSize + i, borderItem);
        }
        
        // Border kiri dan kanan
        for (int i = 1; i < rows - 1; i++) {
            // Left border
            inventory.setItem(i * rowSize, borderItem);
            // Right border
            inventory.setItem(i * rowSize + 8, borderItem);
        }
        
        // Header - Informasi Blackmarket
        ItemStack infoItem = new ItemBuilder(Material.ENDER_EYE)
            .name(ChatColor.RED + "» " + ChatColor.GOLD + "Pasar Gelap " + ChatColor.RED + "«")
            .lore(
                ChatColor.GRAY + "Hanya menjual barang langka dan eksklusif",
                ChatColor.GRAY + "dengan stok terbatas.",
                "",
                ChatColor.RED + "⚠ " + ChatColor.YELLOW + "Stok tidak selalu direset",
                ChatColor.RED + "⚠ " + ChatColor.YELLOW + "Harga lebih mahal dari toko biasa",
                ChatColor.RED + "⚠ " + ChatColor.YELLOW + "Sebagian item tidak dapat dijual kembali",
                ChatColor.RED + "⚠ " + ChatColor.YELLOW + "Beberapa item hanya tersedia pada hari tertentu"
            )
            .build();
            
        inventory.setItem(4, infoItem);
        
        // Tambahkan item-item blackmarket yang tersedia hari ini
        int slot = 10;
        for (BlackmarketItem blackmarketItem : plugin.getBlackmarketManager().getAvailableItems()) {
            // Lompati slot border
            if (slot % 9 == 0) {
                slot++;
            }
            if (slot % 9 == 8) {
                slot += 2;
            }
            
            // Jika sudah mencapai baris terakhir, stop
            if (slot >= (rows - 1) * 9) {
                break;
            }
            
            // Buat item untuk ditampilkan di GUI
            ItemStack itemDisplay = blackmarketItem.createItemStack();
            ItemMeta meta = itemDisplay.getItemMeta();
            
            if (meta != null) {
                List<String> lore = meta.getLore();
                if (lore == null) {
                    lore = new ArrayList<>();
                }
                
                // Tambahkan info harga dan stok
                lore.add("");
                
                // Harga beli
                if (blackmarketItem.canBuy()) {
                    lore.add(ChatColor.YELLOW + "Harga: " + ChatColor.GREEN + PRICE_FORMAT.format(blackmarketItem.getBuyPrice()) + " coins");
                } else {
                    lore.add(ChatColor.YELLOW + "Harga: " + ChatColor.RED + "Tidak tersedia");
                }
                
                // Stok
                lore.add(ChatColor.YELLOW + "Stok Tersisa: " + ChatColor.WHITE + blackmarketItem.getCurrentStock());
                
                // Tambahkan info rotasi jika ini adalah item rotasi
                if (blackmarketItem.isRotatingItem()) {
                    lore.add("");
                    lore.add(ChatColor.LIGHT_PURPLE + "⚠ " + ChatColor.YELLOW + "Item Rotasi Terbatas!");
                    
                    if (blackmarketItem.getRotationType().equals("daily")) {
                        List<String> days = plugin.getBlackmarketManager().getRotationDays(blackmarketItem.getRotationGroup());
                        String daysString = String.join(", ", days);
                        lore.add(ChatColor.YELLOW + "Tersedia pada: " + ChatColor.WHITE + daysString);
                    } else if (blackmarketItem.getRotationType().equals("weekly")) {
                        lore.add(ChatColor.YELLOW + "Tersedia pada minggu ke-" + ChatColor.WHITE + blackmarketItem.getRotationWeek());
                    }
                }
                
                // Tambahkan indikator untuk item command
                if (blackmarketItem.isCommandItem()) {
                    lore.add("");
                    lore.add(ChatColor.LIGHT_PURPLE + "✧ " + ChatColor.YELLOW + "Item Aktivasi Command");
                    lore.add(ChatColor.GRAY + "Aktif segera setelah pembelian");
                }
                
                // Izin khusus jika ada
                if (blackmarketItem.getPermission() != null) {
                    boolean hasPermission = plugin.getBlackmarketManager().hasPermissionFor(player, blackmarketItem);
                    lore.add(ChatColor.YELLOW + "Status: " + 
                            (hasPermission ? ChatColor.GREEN + "Tersedia" : ChatColor.RED + "Memerlukan izin khusus"));
                }
                
                // Instruksi
                lore.add("");
                if (blackmarketItem.canBuy() && blackmarketItem.getCurrentStock() > 0) {
                    lore.add(ChatColor.GREEN + "Klik untuk membeli");
                } else {
                    lore.add(ChatColor.RED + "Tidak tersedia untuk dibeli");
                }
                
                meta.setLore(lore);
                itemDisplay.setItemMeta(meta);
            }
            
            inventory.setItem(slot, itemDisplay);
            slotMappings.put(slot, blackmarketItem);
            
            slot++;
        }
        
        // Tombol Exit
        ItemStack exitButton = new ItemBuilder(Material.BARRIER)
            .name(ChatColor.RED + "Keluar")
            .lore(ChatColor.GRAY + "Klik untuk keluar dari pasar gelap")
            .build();
            
        inventory.setItem(inventory.getSize() - 5, exitButton);
    }
    
    public void open() {
        player.openInventory(inventory);
    }
    
    public BlackmarketItem getItemAt(int slot) {
        return slotMappings.get(slot);
    }
    
    @Override
    public Inventory getInventory() {
        return inventory;
    }
}