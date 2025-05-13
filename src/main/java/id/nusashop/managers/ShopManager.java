package id.nusashop.managers;

import id.nusashop.NusaShop;
import id.nusashop.models.Category;
import id.nusashop.models.ShopItem;

import org.bukkit.configuration.ConfigurationSection;
import org.bukkit.configuration.file.FileConfiguration;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

/**
 * Manager untuk mengelola shop dan kategori
 */
public class ShopManager {
    private final NusaShop plugin;
    private final Map<String, Category> categories;
    
    public ShopManager(NusaShop plugin) {
        this.plugin = plugin;
        this.categories = new HashMap<>();
        
        loadCategories();
    }
    
    /**
     * Memuat kategori dari konfigurasi
     */
    private void loadCategories() {
        FileConfiguration shopsConfig = plugin.getConfigManager().getShops();
        ConfigurationSection categoriesSection = shopsConfig.getConfigurationSection("categories");
        if (categoriesSection != null) {
            for (String categoryId : categoriesSection.getKeys(false)) {
                String name = categoriesSection.getString(categoryId + ".name");
                String iconMaterial = categoriesSection.getString(categoryId + ".icon");
                String description = categoriesSection.getString(categoryId + ".description", "");
                
                Category category = new Category(categoryId, name, iconMaterial, description);
                
                // Load items di kategori
                ConfigurationSection itemsSection = categoriesSection.getConfigurationSection(categoryId + ".items");
                if (itemsSection != null) {
                    for (String itemId : itemsSection.getKeys(false)) {
                        String itemName = itemsSection.getString(itemId + ".name");
                        String itemMaterial = itemsSection.getString(itemId + ".material");
                        double buyPrice = itemsSection.getDouble(itemId + ".buy-price", -1);
                        double sellPrice = itemsSection.getDouble(itemId + ".sell-price", -1);
                        int amount = itemsSection.getInt(itemId + ".amount", 1);
                        
                        ShopItem item = new ShopItem(itemId, itemName, itemMaterial, buyPrice, sellPrice, amount);
                        category.addItem(item);
                    }
                }
                
                categories.put(categoryId, category);
                plugin.getLogger().info("Memuat kategori: " + name + " dengan " + 
                        (category.getItems() != null ? category.getItems().size() : 0) + " item");
            }
        }
    }
    
    /**
     * Mendapatkan semua kategori
     * @return list kategori
     */
    public List<Category> getCategories() {
        return new ArrayList<>(categories.values());
    }
    
    /**
     * Mendapatkan kategori berdasarkan ID
     * @param id ID kategori
     * @return kategori atau null jika tidak ditemukan
     */
    public Category getCategory(String id) {
        return categories.get(id);
    }
    
    /**
     * Menyimpan semua data
     */
    public void saveAll() {
        // Dapatkan konfigurasi shops
        FileConfiguration shopsConfig = plugin.getConfigManager().getShops();
        
        // Hapus semua data kategori yang ada, lalu buat yang baru
        shopsConfig.set("categories", null);
        ConfigurationSection categoriesSection = shopsConfig.createSection("categories");
        
        for (Category category : categories.values()) {
            ConfigurationSection categorySection = categoriesSection.createSection(category.getId());
            categorySection.set("name", category.getName());
            categorySection.set("icon", category.getIconMaterial());
            categorySection.set("description", category.getDescription());
            
            ConfigurationSection itemsSection = categorySection.createSection("items");
            for (ShopItem item : category.getItems()) {
                ConfigurationSection itemSection = itemsSection.createSection(item.getId());
                itemSection.set("name", item.getName());
                itemSection.set("material", item.getMaterial());
                itemSection.set("buy-price", item.getBuyPrice());
                itemSection.set("sell-price", item.getSellPrice());
                itemSection.set("amount", item.getAmount());
            }
        }
        
        // Simpan konfigurasi ke file
        plugin.getConfigManager().saveShops();
        plugin.getLogger().info("Menyimpan data shop dengan " + categories.size() + " kategori.");
    }
    
    /**
     * Membuat kategori baru
     * @param id ID kategori baru
     * @param name Nama kategori
     * @param iconMaterial Material icon
     * @param description Deskripsi kategori
     * @return Kategori baru yang dibuat
     */
    public Category createCategory(String id, String name, String iconMaterial, String description) {
        if (categories.containsKey(id)) {
            return null; // ID sudah ada
        }
        
        Category category = new Category(id, name, iconMaterial, description);
        categories.put(id, category);
        saveAll();
        return category;
    }
    
    /**
     * Menghapus kategori
     * @param id ID kategori yang akan dihapus
     * @return true jika berhasil dihapus
     */
    public boolean removeCategory(String id) {
        if (categories.containsKey(id)) {
            categories.remove(id);
            saveAll();
            return true;
        }
        return false;
    }
    
    /**
     * Menyimpan perubahan kategori
     * @param category Kategori yang diubah
     */
    public void saveCategory(Category category) {
        categories.put(category.getId(), category);
        saveAll();
    }
    
    /**
     * Membuat item baru dalam kategori
     * @param category Kategori item
     * @param id ID item baru
     * @param name Nama item
     * @param material Material item
     * @param buyPrice Harga beli
     * @param sellPrice Harga jual
     * @param amount Jumlah item
     * @return Item baru yang dibuat
     */
    public ShopItem createItem(Category category, String id, String name, String material, 
                             double buyPrice, double sellPrice, int amount) {
        if (category.getItem(id) != null) {
            return null; // ID sudah ada
        }
        
        ShopItem item = new ShopItem(id, name, material, buyPrice, sellPrice, amount);
        category.addItem(item);
        saveAll();
        return item;
    }
    
    /**
     * Menyimpan perubahan item
     * @param category Kategori item
     * @param item Item yang diubah
     */
    public void saveItem(Category category, ShopItem item) {
        // Cek apakah item sudah ada
        ShopItem existingItem = category.getItem(item.getId());
        if (existingItem != null) {
            // Update item (hapus yang lama dan tambahkan yang baru)
            category.removeItem(existingItem);
        }
        
        category.addItem(item);
        saveAll();
    }
    
    /**
     * Reload shop data from configuration
     */
    public void reloadShops() {
        // Clear current categories
        categories.clear();
        
        // Load categories again from the fresh config
        loadCategories();
        
        plugin.getLogger().info("Shop data reloaded successfully with " + categories.size() + " categories.");
    }
}