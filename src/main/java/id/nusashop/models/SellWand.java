package id.nusashop.models;

import id.nusashop.utils.ItemBuilder;
import org.bukkit.ChatColor;
import org.bukkit.Material;
import org.bukkit.NamespacedKey;
import org.bukkit.enchantments.Enchantment;
import org.bukkit.inventory.ItemFlag;
import org.bukkit.inventory.ItemStack;
import org.bukkit.inventory.meta.ItemMeta;
import org.bukkit.persistence.PersistentDataContainer;
import org.bukkit.persistence.PersistentDataType;
import org.bukkit.plugin.java.JavaPlugin;

import java.util.ArrayList;
import java.util.List;

/**
 * Class model untuk Sell Wand
 */
public class SellWand {
    private static final String SELL_WAND_KEY = "sell_wand_uses";
    private final JavaPlugin plugin;
    
    public SellWand(JavaPlugin plugin) {
        this.plugin = plugin;
    }
    
    /**
     * Membuat item sell wand baru dengan jumlah penggunaan tertentu
     *
     * @param uses Jumlah penggunaan maksimal
     * @return ItemStack sell wand
     */
    public ItemStack createWand(int uses) {
        ItemStack wand = new ItemBuilder(Material.BLAZE_ROD)
            .name(ChatColor.GOLD + "" + ChatColor.BOLD + "Tongkat Penjual")
            .lore(
                ChatColor.GRAY + "Klik kanan pada chest untuk",
                ChatColor.GRAY + "menjual semua item di dalamnya",
                "",
                ChatColor.YELLOW + "Penggunaan tersisa: " + ChatColor.WHITE + uses
            )
            .enchant(Enchantment.LUCK_OF_THE_SEA, 1)
            .flag(ItemFlag.HIDE_ENCHANTS)
            .build();
            
        ItemMeta meta = wand.getItemMeta();
        if (meta != null) {
            // Simpan uses ke dalam persistent data
            NamespacedKey key = new NamespacedKey(plugin, SELL_WAND_KEY);
            meta.getPersistentDataContainer().set(key, PersistentDataType.INTEGER, uses);
            wand.setItemMeta(meta);
        }
        
        return wand;
    }
    
    /**
     * Mengecek apakah item adalah sell wand
     *
     * @param item Item yang akan dicek
     * @return true jika item adalah sell wand
     */
    public boolean isSellWand(ItemStack item) {
        if (item == null || item.getType() != Material.BLAZE_ROD) {
            return false;
        }
        
        ItemMeta meta = item.getItemMeta();
        if (meta == null) return false;
        
        PersistentDataContainer container = meta.getPersistentDataContainer();
        NamespacedKey key = new NamespacedKey(plugin, SELL_WAND_KEY);
        
        return container.has(key, PersistentDataType.INTEGER);
    }
    
    /**
     * Mendapatkan jumlah penggunaan yang tersisa pada sell wand
     *
     * @param item Sell wand item
     * @return Jumlah penggunaan tersisa, atau 0 jika bukan sell wand
     */
    public int getRemainingUses(ItemStack item) {
        if (!isSellWand(item)) return 0;
        
        ItemMeta meta = item.getItemMeta();
        if (meta == null) return 0;
        
        PersistentDataContainer container = meta.getPersistentDataContainer();
        NamespacedKey key = new NamespacedKey(plugin, SELL_WAND_KEY);
        
        return container.getOrDefault(key, PersistentDataType.INTEGER, 0);
    }
    
    /**
     * Mengurangi jumlah penggunaan sell wand
     *
     * @param item Sell wand item
     * @return true jika berhasil mengurangi uses, false jika uses habis
     */
    public boolean useWand(ItemStack item) {
        if (!isSellWand(item)) return false;
        
        ItemMeta meta = item.getItemMeta();
        if (meta == null) return false;
        
        PersistentDataContainer container = meta.getPersistentDataContainer();
        NamespacedKey key = new NamespacedKey(plugin, SELL_WAND_KEY);
        
        int uses = container.getOrDefault(key, PersistentDataType.INTEGER, 0);
        if (uses <= 0) return false;
        
        // Kurangi penggunaan
        uses--;
        container.set(key, PersistentDataType.INTEGER, uses);
        
        // Update lore
        List<String> lore = meta.hasLore() ? meta.getLore() : new ArrayList<>();
        if (lore != null) {
            for (int i = 0; i < lore.size(); i++) {
                if (lore.get(i).contains("Penggunaan tersisa:")) {
                    lore.set(i, ChatColor.YELLOW + "Penggunaan tersisa: " + ChatColor.WHITE + uses);
                    break;
                }
            }
            meta.setLore(lore);
        }
        
        item.setItemMeta(meta);
        return uses > 0;
    }
}