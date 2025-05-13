package id.nusashop.gui;

import id.nusashop.NusaShop;
import id.nusashop.models.Category;
import id.nusashop.models.ShopItem;
import id.nusashop.utils.ItemBuilder;
import id.nusashop.utils.Messages;
import id.nusashop.utils.ShopUtils;

import org.bukkit.Bukkit;
import org.bukkit.ChatColor;
import org.bukkit.Material;
import org.bukkit.Sound;
import org.bukkit.entity.Player;
import org.bukkit.inventory.Inventory;
import org.bukkit.inventory.InventoryHolder;
import org.bukkit.inventory.ItemFlag;
import org.bukkit.inventory.ItemStack;

import java.text.DecimalFormat;
import java.util.ArrayList;
import java.util.List;

/**
 * GUI untuk konfirmasi transaksi dengan pilihan jumlah yang dapat dikustomisasi
 */
public class ConfirmationGUI implements InventoryHolder {
    private final NusaShop plugin;
    private final Player player;
    private final ShopItem item;
    private int amount;
    private final Category category;
    private final boolean isBuying;
    private final Inventory inventory;
    
    private static final DecimalFormat PRICE_FORMAT = new DecimalFormat("#,##0.00");
    
    public ConfirmationGUI(NusaShop plugin, Player player, ShopItem item, int amount, Category category, boolean isBuying) {
        this.plugin = plugin;
        this.player = player;
        this.item = item;
        this.amount = amount;
        this.category = category;
        this.isBuying = isBuying;
        
        String title = ChatColor.translateAlternateColorCodes('&', 
                isBuying ? "&8Konfirmasi Pembelian" : "&8Konfirmasi Penjualan");
        this.inventory = Bukkit.createInventory(this, 5 * 9, title);
        
        setupInventory();
    }
    
    private void setupInventory() {
        // Background dengan warna sesuai jenis transaksi
        Material backgroundMaterial = isBuying ? Material.GREEN_STAINED_GLASS_PANE : Material.ORANGE_STAINED_GLASS_PANE;
        ItemStack backgroundItem = new ItemBuilder(backgroundMaterial)
            .name(" ")
            .build();
        
        // Material batas hitam
        ItemStack borderItem = new ItemBuilder(Material.BLACK_STAINED_GLASS_PANE)
            .name(" ")
            .build();
        
        // Isi background
        for (int i = 0; i < inventory.getSize(); i++) {
            // Batas hitam di pinggir
            if (i < 9 || i >= inventory.getSize() - 9 || i % 9 == 0 || i % 9 == 8) {
                inventory.setItem(i, borderItem);
            } else {
                inventory.setItem(i, backgroundItem);
            }
        }
        
        // Item yang ditransaksikan
        Material material;
        try {
            material = Material.valueOf(item.getMaterial().toUpperCase());
        } catch (IllegalArgumentException e) {
            material = Material.BARRIER;
        }
        
        // Hitung harga per item
        double pricePerUnit = isBuying ? item.getBuyPrice() / item.getAmount() : item.getSellPrice() / item.getAmount();
        double totalPrice = pricePerUnit * amount;
        
        // Item informasi transaksi
        List<String> itemLore = new ArrayList<>();
        itemLore.add(ChatColor.GRAY + "Item: " + ChatColor.WHITE + 
                  ChatColor.translateAlternateColorCodes('&', item.getName()));
        itemLore.add(ChatColor.GRAY + "Jumlah: " + ChatColor.WHITE + amount);
        itemLore.add(ChatColor.GRAY + "Harga per item: " + ChatColor.WHITE + PRICE_FORMAT.format(pricePerUnit) + " coins");
        
        String priceLabel = isBuying ? "Total Pembayaran" : "Total Penerimaan";
        ChatColor priceColor = isBuying ? ChatColor.RED : ChatColor.GREEN;
        
        itemLore.add(ChatColor.GRAY + priceLabel + ": " + priceColor + PRICE_FORMAT.format(totalPrice) + " coins");
        
        if (isBuying) {
            double balance = plugin.getEconomy().getBalance(player);
            String balanceStatus = balance >= totalPrice ? "cukup" : "tidak cukup";
            ChatColor balanceColor = balance >= totalPrice ? ChatColor.GREEN : ChatColor.RED;
            
            itemLore.add("");
            itemLore.add(ChatColor.GRAY + "Saldo Anda: " + ChatColor.WHITE + PRICE_FORMAT.format(balance) + 
                       " " + balanceColor + "(" + balanceStatus + ")");
            
            // Tambahkan informasi berapa banyak yang dapat dibeli dengan saldo saat ini
            int maxAffordable = (int)(balance / pricePerUnit);
            if (maxAffordable > 0 && maxAffordable < amount) {
                itemLore.add(ChatColor.YELLOW + "Saldo Anda cukup untuk " + maxAffordable + " item");
            }
        } else {
            // Untuk penjualan, cek apakah player punya item yang cukup
            try {
                Material itemMaterial = Material.valueOf(item.getMaterial());
                int playerItemCount = ShopUtils.countItems(player, itemMaterial);
                String itemStatus = playerItemCount >= amount ? "cukup" : "tidak cukup";
                ChatColor itemColor = playerItemCount >= amount ? ChatColor.GREEN : ChatColor.RED;
                
                itemLore.add("");
                itemLore.add(ChatColor.GRAY + "Item Anda: " + ChatColor.WHITE + playerItemCount + 
                           " " + itemColor + "(" + itemStatus + ")");
                
                // Tampilkan informasi maksimum yang dapat dijual
                if (playerItemCount > 0 && playerItemCount < amount) {
                    itemLore.add(ChatColor.YELLOW + "Anda hanya memiliki " + playerItemCount + " item");
                }
            } catch (IllegalArgumentException ignored) {
                itemLore.add(ChatColor.RED + "Item tidak valid!");
            }
        }
        
        // Item informasi transaksi di tengah atas
        ItemStack transactionItem = new ItemBuilder(material)
            .name(ChatColor.translateAlternateColorCodes('&', 
                 (isBuying ? "&b&lPembelian " : "&6&lPenjualan ") + amount + "x " + item.getName()))
            .lore(itemLore)
            .amount(Math.min(amount, 64)) // Max stack size is 64
            .flag(ItemFlag.HIDE_ATTRIBUTES)
            .build();
        
        inventory.setItem(13, transactionItem);
        
        // --- PILIHAN JUMLAH ---
        // Header untuk pilihan jumlah
        ItemStack headerItem = new ItemBuilder(Material.KNOWLEDGE_BOOK)
            .name(ChatColor.YELLOW + "Atur Jumlah " + (isBuying ? "Pembelian" : "Penjualan"))
            .lore(
                ChatColor.GRAY + "Gunakan tombol di bawah untuk",
                ChatColor.GRAY + "mengubah jumlah " + (isBuying ? "pembelian" : "penjualan")
            )
            .build();
        inventory.setItem(4, headerItem);
        
        // --- TOMBOL PENGUBAH JUMLAH ---
        // Baris tombol decrement (-64, -32, -1)
        addQuantityButton(19, -64, Material.RED_CONCRETE);
        addQuantityButton(20, -32, Material.RED_TERRACOTTA);
        addQuantityButton(21, -1, Material.PINK_CONCRETE);
        
        // Item penampil jumlah saat ini
        ItemStack currentAmount = new ItemBuilder(Material.HOPPER)
            .name(ChatColor.GOLD + "Jumlah Saat Ini: " + ChatColor.WHITE + amount)
            .lore(
                ChatColor.GRAY + "Total harga: " + 
                (isBuying ? ChatColor.RED : ChatColor.GREEN) + PRICE_FORMAT.format(totalPrice)
            )
            .build();
        inventory.setItem(22, currentAmount);
        
        // Baris tombol increment (+1, +32, +64)
        addQuantityButton(23, 1, Material.LIME_CONCRETE);
        addQuantityButton(24, 32, Material.GREEN_TERRACOTTA);
        addQuantityButton(25, 64, Material.GREEN_CONCRETE);
        
        // Tombol preset untuk jumlah umum
        addPresetButton(30, 64, "1 Stack");
        addPresetButton(31, 320, "5 Stacks");
        addPresetButton(32, 1728, "27 Stacks (Satu Chest)");
        
        // Opsi Jumlah Kustom
        ItemStack customItem = new ItemBuilder(Material.NAME_TAG)
            .name(ChatColor.GOLD + "Jumlah Kustom")
            .lore(
                ChatColor.GRAY + "Klik untuk memasukkan",
                ChatColor.GRAY + "jumlah spesifik"
            )
            .build();
        inventory.setItem(28, customItem);
        
        // Tombol konfirmasi (HIJAU)
        String confirmMessage;
        if (isBuying) {
            confirmMessage = Messages.get("shop.confirm-buy")
                .replace("%amount%", String.valueOf(amount))
                .replace("%item%", ChatColor.stripColor(ChatColor.translateAlternateColorCodes('&', item.getName())))
                .replace("%price%", PRICE_FORMAT.format(totalPrice));
        } else {
            confirmMessage = Messages.get("shop.confirm-sell")
                .replace("%amount%", String.valueOf(amount))
                .replace("%item%", ChatColor.stripColor(ChatColor.translateAlternateColorCodes('&', item.getName())))
                .replace("%price%", PRICE_FORMAT.format(totalPrice));
        }
        
        ItemStack confirmButton = new ItemBuilder(Material.LIME_CONCRETE)
            .name(ChatColor.GREEN + "" + ChatColor.BOLD + "✓ KONFIRMASI")
            .lore(
                ChatColor.GRAY + "Klik untuk " + (isBuying ? "membeli" : "menjual") + " item ini",
                "",
                ChatColor.YELLOW + confirmMessage
            )
            .build();
        
        inventory.setItem(37, confirmButton);
        inventory.setItem(38, confirmButton);
        
        // Tombol batal (MERAH)
        ItemStack cancelButton = new ItemBuilder(Material.RED_CONCRETE)
            .name(ChatColor.RED + "" + ChatColor.BOLD + "✗ BATAL")
            .lore(
                ChatColor.GRAY + "Klik untuk membatalkan",
                ChatColor.GRAY + "transaksi ini"
            )
            .build();
        
        inventory.setItem(42, cancelButton);
        inventory.setItem(43, cancelButton);
    }
    
    /**
     * Menambahkan tombol pengubah jumlah
     * @param slot Slot untuk menempatkan tombol
     * @param change Perubahan jumlah (positif untuk tambah, negatif untuk kurang)
     * @param material Material tombol
     */
    private void addQuantityButton(int slot, int change, Material material) {
        String prefix = change > 0 ? "+" : "";
        ChatColor color = change > 0 ? ChatColor.GREEN : ChatColor.RED;
        
        ItemStack button = new ItemBuilder(material)
            .name(color + "" + prefix + change)
            .lore(
                ChatColor.GRAY + "Klik untuk " + (change > 0 ? "menambah" : "mengurangi") + 
                " jumlah sebanyak " + Math.abs(change),
                "",
                ChatColor.YELLOW + "Shift+Klik untuk " + (change > 0 ? "menambah" : "mengurangi") + 
                " jumlah sebanyak " + Math.abs(change) * 5
            )
            .amount(Math.min(Math.abs(change), 64))
            .build();
        
        inventory.setItem(slot, button);
    }
    
    /**
     * Menambahkan tombol preset jumlah
     * @param slot Slot untuk menempatkan tombol
     * @param presetAmount Jumlah preset
     * @param label Label tombol
     */
    private void addPresetButton(int slot, int presetAmount, String label) {
        Material material = item.getMaterial() != null ? 
            Material.valueOf(item.getMaterial().toUpperCase()) : Material.STONE;
        
        double pricePerUnit = isBuying ? item.getBuyPrice() / item.getAmount() : item.getSellPrice() / item.getAmount();
        double totalPrice = pricePerUnit * presetAmount;
        
        // Highlight button if this is the current amount
        ChatColor titleColor = (presetAmount == amount) ? ChatColor.GREEN : ChatColor.YELLOW;
        String activeIndicator = (presetAmount == amount) ? " " + ChatColor.GREEN + "✓" : "";
        
        ItemStack button = new ItemBuilder(material)
            .name(titleColor + label + activeIndicator)
            .lore(
                ChatColor.GRAY + "Jumlah: " + ChatColor.WHITE + presetAmount + " item",
                "",
                ChatColor.GRAY + "Total: " + (isBuying ? ChatColor.RED : ChatColor.GREEN) + 
                PRICE_FORMAT.format(totalPrice) + " coins",
                "",
                ChatColor.YELLOW + "Klik untuk memilih"
            )
            .amount(Math.min(64, presetAmount))
            .build();
        
        inventory.setItem(slot, button);
    }
    
    /**
     * Update GUI setelah jumlah berubah
     */
    public void updateAmount(int newAmount) {
        // Ensure amount is not negative
        this.amount = Math.max(1, newAmount);
        setupInventory();  // Rebuild the inventory with new amount
        player.updateInventory();
        
        // Play sound effect untuk feedback
        player.playSound(player.getLocation(), Sound.UI_BUTTON_CLICK, 0.5f, 1.0f);
    }
    
    /**
     * Menerapkan perubahan jumlah dengan batas minimum dan maksimum
     * @param change Perubahan jumlah (positif atau negatif)
     * @param multiplier Pengali perubahan (untuk shift-click)
     */
    public void changeAmount(int change, int multiplier) {
        int newAmount = amount + (change * multiplier);
        
        // Minimum 1 item
        newAmount = Math.max(1, newAmount);
        
        // Batasi jumlah maksimum untuk penjualan
        if (!isBuying) {
            try {
                Material itemMaterial = Material.valueOf(item.getMaterial());
                int playerItemCount = ShopUtils.countItems(player, itemMaterial);
                newAmount = Math.min(newAmount, playerItemCount);
            } catch (IllegalArgumentException ignored) {}
        }
        
        updateAmount(newAmount);
    }
    
    /**
     * Meminta jumlah kustom lewat chat
     */
    public void requestCustomAmount() {
        player.closeInventory();
        
        // Tunggu sedikit sebelum menampilkan pesan agar inventory benar-benar tertutup
        Bukkit.getScheduler().runTaskLater(plugin, () -> {
            // Gunakan requestInput, bukan waitForInput
            plugin.getChatInputManager().requestInput(
                player,
                "Masukkan jumlah " + (isBuying ? "pembelian" : "penjualan") + " yang diinginkan:",
                input -> {
                    try {
                        // Jika input adalah 'batal', batalkan operasi
                        if (input.equalsIgnoreCase("batal")) {
                            player.sendMessage(ChatColor.RED + "Operasi dibatalkan.");
                            return;
                        }
                        
                        int newAmount = Integer.parseInt(input);
                        if (newAmount <= 0) {
                            player.sendMessage(ChatColor.RED + "Jumlah harus lebih dari 0!");
                            return;
                        }
                        
                        // Batasi jumlah maksimum untuk penjualan
                        if (!isBuying) {
                            try {
                                Material itemMaterial = Material.valueOf(item.getMaterial());
                                int playerItemCount = ShopUtils.countItems(player, itemMaterial);
                                if (newAmount > playerItemCount) {
                                    player.sendMessage(ChatColor.YELLOW + "Anda hanya memiliki " + 
                                                     playerItemCount + " item. Jumlah disesuaikan.");
                                    newAmount = playerItemCount;
                                }
                            } catch (IllegalArgumentException ignored) {}
                        }
                        
                        // Reopen with new amount
                        amount = newAmount;
                        open();
                    } catch (NumberFormatException e) {
                        player.sendMessage(ChatColor.RED + "Masukkan angka yang valid!");
                    }
                },
                60 // timeout dalam detik
            );
        }, 2L);
    }
    
    public void open() {
        player.openInventory(inventory);
    }
    
    /**
     * Memproses transaksi setelah konfirmasi
     */
    public void processTransaction() {
        if (isBuying) {
            ShopUtils.buyItem(plugin, player, item, amount);
        } else {
            if (amount == ShopUtils.countItems(player, Material.valueOf(item.getMaterial().toUpperCase()))) {
                ShopUtils.sellAllItems(plugin, player, item);
            } else {
                ShopUtils.sellItem(plugin, player, item, amount);
            }
        }
    }
    
    /**
     * Membuka kembali menu sebelumnya
     */
    public void openPreviousMenu() {
        // Mode detail sebelumnya
        new ItemDetailsGUI(plugin, player, category, item).open();
    }
    
    /**
     * Getter untuk item
     */
    public ShopItem getItem() {
        return item;
    }
    
    @Override
    public Inventory getInventory() {
        return inventory;
    }
}