package id.nusashop.models;

/**
 * Model untuk item shop
 */
public class ShopItem {
    private final String id;
    private String name;
    private String material;
    private double buyPrice;  // Harga untuk membeli item (player membayar), -1 jika tidak bisa dibeli
    private double sellPrice; // Harga untuk menjual item (player menerima), -1 jika tidak bisa dijual
    private int amount;
    
    public ShopItem(String id, String name, String material, double buyPrice, double sellPrice, int amount) {
        this.id = id;
        this.name = name;
        this.material = material;
        this.buyPrice = buyPrice;
        this.sellPrice = sellPrice;
        this.amount = amount;
    }
    
    public String getId() {
        return id;
    }
    
    public String getName() {
        return name;
    }
    
    public void setName(String name) {
        this.name = name;
    }
    
    public String getMaterial() {
        return material;
    }
    
    public void setMaterial(String material) {
        this.material = material;
    }
    
    public double getBuyPrice() {
        return buyPrice;
    }
    
    public void setBuyPrice(double buyPrice) {
        this.buyPrice = buyPrice;
    }
    
    public double getSellPrice() {
        return sellPrice;
    }
    
    public void setSellPrice(double sellPrice) {
        this.sellPrice = sellPrice;
    }
    
    public int getAmount() {
        return amount;
    }
    
    public void setAmount(int amount) {
        this.amount = amount;
    }
    
    public boolean canBuy() {
        return buyPrice >= 0;
    }
    
    public boolean canSell() {
        return sellPrice >= 0;
    }
}