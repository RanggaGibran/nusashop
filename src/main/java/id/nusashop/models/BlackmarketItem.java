package id.nusashop.models;

import org.bukkit.Material;
import org.bukkit.enchantments.Enchantment;
import org.bukkit.inventory.ItemFlag;
import org.bukkit.inventory.ItemStack;
import org.bukkit.inventory.meta.ItemMeta;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

/**
 * Model untuk item di blackmarket
 */
public class BlackmarketItem {
    private final String id;
    private String name;
    private String material;
    private List<String> lore = new ArrayList<>();
    private double buyPrice;
    private double sellPrice;
    private int amount;
    private int maxStock;
    private int currentStock;
    private Map<Enchantment, Integer> enchantments = new HashMap<>();
    private boolean resetOnRestart;
    private String permission;
    
    // Properti rotasi
    private boolean isRotatingItem = false;
    private String rotationType = ""; // daily, weekly, monthly
    private String rotationGroup = "";
    private int rotationWeek = 0;
    
    // Properti untuk item command
    private boolean isCommandItem = false;
    private List<String> commands = new ArrayList<>();
    
    /**
     * Membuat BlackmarketItem baru
     * @param id ID item
     * @param name Display name
     * @param material Material item
     * @param lore Deskripsi item
     * @param buyPrice Harga pembelian
     * @param sellPrice Harga penjualan (-1 jika tidak bisa dijual)
     * @param amount Jumlah per stack
     * @param maxStock Stok maksimal
     * @param resetOnRestart Apakah stok direset saat server restart
     */
    public BlackmarketItem(String id, String name, String material, List<String> lore, 
                         double buyPrice, double sellPrice, int amount, int maxStock, 
                         boolean resetOnRestart) {
        this.id = id;
        this.name = name;
        this.material = material;
        if (lore != null) {
            this.lore = lore;
        }
        this.buyPrice = buyPrice;
        this.sellPrice = sellPrice;
        this.amount = amount;
        this.maxStock = maxStock;
        this.currentStock = maxStock; // Set stok awal = maksimal
        this.resetOnRestart = resetOnRestart;
    }
    
    /**
     * Membuat ItemStack dari BlackmarketItem
     * @return ItemStack untuk GUI
     */
    public ItemStack createItemStack() {
        Material mat;
        try {
            mat = Material.valueOf(material.toUpperCase());
        } catch (IllegalArgumentException e) {
            mat = Material.BARRIER;
        }
        
        ItemStack item = new ItemStack(mat, amount);
        ItemMeta meta = item.getItemMeta();
        
        if (meta != null) {
            // Set nama
            meta.setDisplayName(name);
            
            // Set lore
            meta.setLore(lore);
            
            // Sembunyikan atribut item
            meta.addItemFlags(ItemFlag.HIDE_ATTRIBUTES);
            meta.addItemFlags(ItemFlag.HIDE_ENCHANTS);
            meta.addItemFlags(ItemFlag.HIDE_ADDITIONAL_TOOLTIP);
            
            // Terapkan enchantments
            for (Map.Entry<Enchantment, Integer> entry : enchantments.entrySet()) {
                meta.addEnchant(entry.getKey(), entry.getValue(), true);
            }
            
            item.setItemMeta(meta);
        }
        
        return item;
    }
    
    /**
     * Mendapatkan ItemStack sebenarnya untuk diberikan ke pemain
     * @return ItemStack untuk pemain
     */
    public ItemStack getActualItem() {
        Material mat;
        try {
            mat = Material.valueOf(material.toUpperCase());
        } catch (IllegalArgumentException e) {
            mat = Material.BARRIER;
        }
        
        ItemStack item = new ItemStack(mat, amount);
        ItemMeta meta = item.getItemMeta();
        
        if (meta != null) {
            // Set nama
            meta.setDisplayName(name);
            
            // Set lore
            meta.setLore(lore);
            
            // Terapkan enchantments
            for (Map.Entry<Enchantment, Integer> entry : enchantments.entrySet()) {
                meta.addEnchant(entry.getKey(), entry.getValue(), true);
            }
            
            item.setItemMeta(meta);
        }
        
        return item;
    }
    
    // Getters dan Setters
    public String getId() { return id; }
    
    public String getName() { return name; }
    
    public String getMaterial() { return material; }
    
    public List<String> getLore() { return lore; }
    
    public double getBuyPrice() { return buyPrice; }
    
    public double getSellPrice() { return sellPrice; }
    
    public int getAmount() { return amount; }
    
    public int getMaxStock() { return maxStock; }
    
    public int getCurrentStock() { return currentStock; }
    
    public void setCurrentStock(int currentStock) { this.currentStock = currentStock; }
    
    public void decreaseStock() {
        if (currentStock > 0) {
            currentStock--;
        }
    }
    
    public boolean canBuy() { return buyPrice >= 0 && currentStock > 0; }
    
    public boolean canSell() { return sellPrice >= 0; }
    
    public void addEnchantment(Enchantment enchantment, int level) {
        enchantments.put(enchantment, level);
    }
    
    public Map<Enchantment, Integer> getEnchantments() { return enchantments; }
    
    public boolean isResetOnRestart() { return resetOnRestart; }
    
    public String getPermission() { return permission; }
    
    public void setPermission(String permission) { this.permission = permission; }
    
    // Getter dan setter untuk properti rotasi
    public boolean isRotatingItem() {
        return isRotatingItem;
    }
    
    public void setRotatingItem(boolean rotatingItem) {
        isRotatingItem = rotatingItem;
    }
    
    public String getRotationType() {
        return rotationType;
    }
    
    public void setRotationType(String rotationType) {
        this.rotationType = rotationType;
    }
    
    public String getRotationGroup() {
        return rotationGroup;
    }
    
    public void setRotationGroup(String rotationGroup) {
        this.rotationGroup = rotationGroup;
    }
    
    public int getRotationWeek() {
        return rotationWeek;
    }
    
    public void setRotationWeek(int rotationWeek) {
        this.rotationWeek = rotationWeek;
    }
    
    // Getter dan setter untuk properti command
    public boolean isCommandItem() {
        return isCommandItem;
    }
    
    public void setCommandItem(boolean commandItem) {
        isCommandItem = commandItem;
    }
    
    public List<String> getCommands() {
        return commands;
    }
    
    public void setCommands(List<String> commands) {
        this.commands = commands;
    }
    
    public void addCommand(String command) {
        commands.add(command);
    }
}