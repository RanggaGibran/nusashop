package id.nusashop.models;

import java.util.ArrayList;
import java.util.List;

/**
 * Model untuk kategori shop
 */
public class Category {
    private final String id;
    private String name;
    private String iconMaterial;
    private String description;
    private final List<ShopItem> items;
    
    public Category(String id, String name, String iconMaterial, String description) {
        this.id = id;
        this.name = name;
        this.iconMaterial = iconMaterial;
        this.description = description;
        this.items = new ArrayList<>();
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
    
    public String getIconMaterial() {
        return iconMaterial;
    }
    
    public void setIconMaterial(String iconMaterial) {
        this.iconMaterial = iconMaterial;
    }
    
    public String getDescription() {
        return description;
    }
    
    public void setDescription(String description) {
        this.description = description;
    }
    
    public List<ShopItem> getItems() {
        return items;
    }
    
    public void addItem(ShopItem item) {
        items.add(item);
    }
    
    public void removeItem(ShopItem item) {
        items.remove(item);
    }
    
    public ShopItem getItem(String itemId) {
        for (ShopItem item : items) {
            if (item.getId().equals(itemId)) {
                return item;
            }
        }
        return null;
    }
}