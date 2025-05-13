package id.nusashop.api;

import id.nusashop.NusaShop;
import id.nusashop.models.Category;
import id.nusashop.models.ShopItem;
import id.nusashop.utils.ShopUtils;

import org.bukkit.Material;
import org.bukkit.entity.Player;
import org.bukkit.inventory.ItemStack;
import org.bukkit.inventory.Inventory;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

/**
 * API untuk NusaShop yang dapat digunakan oleh plugin lain
 */
public class NusaShopAPI {
    private static NusaShopAPI instance;
    private final NusaShop plugin;
    
    private NusaShopAPI(NusaShop plugin) {
        this.plugin = plugin;
    }
    
    /**
     * Mendapatkan instance API
     * @return NusaShopAPI instance
     */
    public static NusaShopAPI getInstance() {
        if (instance == null) {
            instance = new NusaShopAPI(NusaShop.getInstance());
        }
        return instance;
    }
    
    /**
     * Menjual item dari inventory player
     * 
     * @param player Player yang menjual
     * @param material Material item yang dijual
     * @param amount Jumlah item yang dijual
     * @return TransactionResult yang berisi hasil transaksi
     */
    public TransactionResult sellItem(Player player, Material material, int amount) {
        ShopItem shopItem = findShopItemByMaterial(material);
        if (shopItem == null) {
            return new TransactionResult(false, 0, "Item tidak dapat dijual di shop");
        }
        
        // Cek jumlah item yang dimiliki player
        int available = ShopUtils.countItems(player, material);
        if (available < amount) {
            amount = available; // Batasi jumlah yang dijual
        }
        
        if (amount <= 0) {
            return new TransactionResult(false, 0, "Tidak ada item yang dapat dijual");
        }
        
        // Lakukan penjualan
        Category category = findCategoryForItem(shopItem);
        boolean success = ShopUtils.sellItem(plugin, player, shopItem, amount);
        
        if (success) {
            // Hitung harga yang diberikan
            double pricePerUnit = shopItem.getSellPrice() / shopItem.getAmount();
            double basePrice = pricePerUnit * amount;
            double eventMultiplier = 1.0;
            
            if (plugin.getEventManager().hasActiveEvent() && category != null) {
                eventMultiplier = plugin.getEventManager().getSellPriceMultiplier(shopItem, category);
            }
            
            double finalPrice = basePrice * eventMultiplier;
            
            return new TransactionResult(true, finalPrice, "Berhasil menjual " + amount + " " + material.name());
        } else {
            return new TransactionResult(false, 0, "Gagal menjual item");
        }
    }
    
    /**
     * Menjual semua item dari sebuah inventory
     * 
     * @param player Player yang akan menerima uang hasil penjualan
     * @param inventory Inventory yang berisi item yang akan dijual
     * @return TransactionSummary berisi hasil semua penjualan
     */
    public TransactionSummary sellAllFromInventory(Player player, Inventory inventory) {
        Map<Material, Integer> itemCounts = new HashMap<>();
        List<ItemSold> itemsSold = new ArrayList<>();
        double totalEarned = 0;
        int totalItemsSold = 0;
        
        // Hitung semua item di inventory
        for (ItemStack item : inventory.getContents()) {
            if (item != null && !item.getType().isAir()) {
                Material material = item.getType();
                itemCounts.put(material, itemCounts.getOrDefault(material, 0) + item.getAmount());
            }
        }
        
        // Proses penjualan untuk setiap jenis item
        for (Map.Entry<Material, Integer> entry : itemCounts.entrySet()) {
            Material material = entry.getKey();
            int amount = entry.getValue();
            
            // Cari item shop yang sesuai
            ShopItem shopItem = findShopItemByMaterial(material);
            if (shopItem != null && shopItem.canSell()) {
                // Hitung harga
                Category category = findCategoryForItem(shopItem);
                double pricePerUnit = shopItem.getSellPrice() / shopItem.getAmount();
                double basePrice = pricePerUnit * amount;
                double eventMultiplier = 1.0;
                
                if (plugin.getEventManager().hasActiveEvent() && category != null) {
                    eventMultiplier = plugin.getEventManager().getSellPriceMultiplier(shopItem, category);
                }
                
                double finalPrice = basePrice * eventMultiplier;
                
                // Hapus item dari inventory
                inventory.removeItem(new ItemStack(material, amount));
                
                // Berikan uang ke player
                plugin.getEconomy().depositPlayer(player, finalPrice);
                
                // Catat transaksi di statistik
                if (category != null) {
                    plugin.getStatisticsManager().recordSellTransaction(player, shopItem, category, amount, finalPrice);
                }
                
                // Catat hasil penjualan
                ItemSold sold = new ItemSold(material, amount, finalPrice);
                itemsSold.add(sold);
                totalEarned += finalPrice;
                totalItemsSold += amount;
            }
        }
        
        return new TransactionSummary(totalEarned, totalItemsSold, itemsSold);
    }
    
    /**
     * Mencari ShopItem berdasarkan Material
     * 
     * @param material Material yang dicari
     * @return ShopItem yang cocok atau null jika tidak ditemukan
     */
    public ShopItem findShopItemByMaterial(Material material) {
        String materialName = material.name();
        
        for (Category category : plugin.getShopManager().getCategories()) {
            for (ShopItem item : category.getItems()) {
                if (item.getMaterial().equalsIgnoreCase(materialName) && item.canSell()) {
                    return item;
                }
            }
        }
        
        return null;
    }
    
    /**
     * Mencari kategori untuk item tertentu
     * 
     * @param shopItem Item yang dicari kategorinya
     * @return Category atau null jika tidak ditemukan
     */
    private Category findCategoryForItem(ShopItem shopItem) {
        for (Category category : plugin.getShopManager().getCategories()) {
            if (category.getItems().contains(shopItem)) {
                return category;
            }
        }
        return null;
    }
    
    /**
     * Mendapatkan harga jual untuk material tertentu
     * 
     * @param material Material yang ingin dicek harganya
     * @return Harga jual per item atau -1 jika tidak dapat dijual
     */
    public double getSellPrice(Material material) {
        ShopItem item = findShopItemByMaterial(material);
        if (item == null || !item.canSell()) {
            return -1;
        }
        
        return item.getSellPrice() / item.getAmount();
    }
    
    /**
     * Class yang merepresentasikan hasil transaksi
     */
    public static class TransactionResult {
        private final boolean success;
        private final double amount;
        private final String message;
        
        public TransactionResult(boolean success, double amount, String message) {
            this.success = success;
            this.amount = amount;
            this.message = message;
        }
        
        public boolean isSuccess() {
            return success;
        }
        
        public double getAmount() {
            return amount;
        }
        
        public String getMessage() {
            return message;
        }
    }
    
    /**
     * Class yang menyimpan informasi tentang item yang dijual
     */
    public static class ItemSold {
        private final Material material;
        private final int amount;
        private final double price;
        
        public ItemSold(Material material, int amount, double price) {
            this.material = material;
            this.amount = amount;
            this.price = price;
        }
        
        public Material getMaterial() {
            return material;
        }
        
        public int getAmount() {
            return amount;
        }
        
        public double getPrice() {
            return price;
        }
    }
    
    /**
     * Class yang merepresentasikan ringkasan transaksi penjualan
     */
    public static class TransactionSummary {
        private final double totalEarned;
        private final int totalItemsSold;
        private final List<ItemSold> itemsSold;
        
        public TransactionSummary(double totalEarned, int totalItemsSold, List<ItemSold> itemsSold) {
            this.totalEarned = totalEarned;
            this.totalItemsSold = totalItemsSold;
            this.itemsSold = itemsSold;
        }
        
        public double getTotalEarned() {
            return totalEarned;
        }
        
        public int getTotalItemsSold() {
            return totalItemsSold;
        }
        
        public List<ItemSold> getItemsSold() {
            return itemsSold;
        }
        
        public boolean hasItemsSold() {
            return !itemsSold.isEmpty();
        }
    }
}