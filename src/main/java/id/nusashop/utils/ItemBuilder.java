package id.nusashop.utils;

import org.bukkit.Material;
import org.bukkit.enchantments.Enchantment;
import org.bukkit.inventory.ItemFlag;
import org.bukkit.inventory.ItemStack;
import org.bukkit.inventory.meta.ItemMeta;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;

/**
 * Utility class untuk mempermudah pembuatan ItemStack
 */
public class ItemBuilder {
    private final ItemStack itemStack;
    private final ItemMeta itemMeta;
    private final List<String> lore = new ArrayList<>();
    
    public ItemBuilder(Material material) {
        this.itemStack = new ItemStack(material);
        this.itemMeta = itemStack.getItemMeta();
    }
    
    public ItemBuilder(ItemStack itemStack) {
        this.itemStack = itemStack.clone();
        this.itemMeta = this.itemStack.getItemMeta();
        
        if (itemMeta != null && itemMeta.hasLore() && itemMeta.getLore() != null) {
            lore.addAll(itemMeta.getLore());
        }
    }
    
    public ItemBuilder amount(int amount) {
        itemStack.setAmount(amount);
        return this;
    }
    
    public ItemBuilder name(String name) {
        if (itemMeta != null) {
            itemMeta.setDisplayName(name);
        }
        return this;
    }
    
    public ItemBuilder lore(String line) {
        lore.add(line);
        return this;
    }
    
    public ItemBuilder lore(String... lines) {
        lore.addAll(Arrays.asList(lines));
        return this;
    }
    
    public ItemBuilder lore(List<String> lines) {
        lore.addAll(lines);
        return this;
    }
    
    public ItemBuilder clearLore() {
        lore.clear();
        return this;
    }
    
    public ItemBuilder enchant(Enchantment enchantment, int level) {
        itemMeta.addEnchant(enchantment, level, true);
        return this;
    }
    
    public ItemBuilder hideEnchants() {
        itemMeta.addItemFlags(ItemFlag.HIDE_ENCHANTS);
        return this;
    }
    
    public ItemBuilder hideAttributes() {
        itemMeta.addItemFlags(ItemFlag.HIDE_ATTRIBUTES);
        return this;
    }
    
    public ItemBuilder addFlags(ItemFlag... flags) {
        itemMeta.addItemFlags(flags);
        return this;
    }
    
    /**
     * Menambahkan ItemFlag tunggal
     * @param flag ItemFlag yang ingin ditambahkan
     * @return ItemBuilder
     */
    public ItemBuilder flag(ItemFlag flag) {
        itemMeta.addItemFlags(flag);
        return this;
    }
    
    /**
     * Menambahkan efek bercahaya pada item
     * 
     * @param glow true untuk menambahkan efek glow
     * @return ItemBuilder instance
     */
    public ItemBuilder glow(boolean glow) {
        if (glow) {
            enchant(Enchantment.LUCK_OF_THE_SEA, 1);
            flag(ItemFlag.HIDE_ENCHANTS);
        }
        return this;
    }
    
    public ItemStack build() {
        if (itemMeta != null) {
            if (!lore.isEmpty()) {
                itemMeta.setLore(lore);
            }
            itemStack.setItemMeta(itemMeta);
        }
        return itemStack;
    }
}