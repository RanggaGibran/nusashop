package id.nusashop.listeners;

import id.nusashop.NusaShop;
import id.nusashop.gui.CategoryShopGUI;
import id.nusashop.gui.ConfirmationGUI;
import id.nusashop.gui.ItemDetailsGUI;
import id.nusashop.gui.MainShopGUI;
import id.nusashop.gui.SellGUI;
import id.nusashop.models.Category;
import id.nusashop.models.ShopItem;
import id.nusashop.utils.Messages;
import id.nusashop.utils.ShopUtils;

import org.bukkit.ChatColor;
import org.bukkit.Material;
import org.bukkit.Sound;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.Listener;
import org.bukkit.event.inventory.ClickType;
import org.bukkit.event.inventory.InventoryClickEvent;
import org.bukkit.event.inventory.InventoryCloseEvent;
import org.bukkit.inventory.InventoryHolder;
import org.bukkit.inventory.ItemStack;

/**
 * Listener untuk inventory events (termasuk GUI shop)
 */
public class InventoryListener implements Listener {
    private final NusaShop plugin;
    
    public InventoryListener(NusaShop plugin) {
        this.plugin = plugin;
    }
    
    @EventHandler
    public void onInventoryClick(InventoryClickEvent event) {
        if (!(event.getWhoClicked() instanceof Player player)) return;
        
        InventoryHolder holder = event.getInventory().getHolder();
        
        // Main shop menu (daftar kategori)
        if (holder instanceof MainShopGUI mainGUI) {
            event.setCancelled(true);
            
            if (event.getCurrentItem() == null || event.getCurrentItem().getType() == Material.AIR) return;
            
            // Cek apakah klik pada slot yang valid
            int slot = event.getRawSlot();
            if (slot < 0 || slot >= event.getInventory().getSize()) return;
            
            // Tombol navigasi halaman pada main menu
            int bottomRow = 5 * 9;
            if (slot == bottomRow + 3 && event.getCurrentItem().getType() == Material.ARROW) {
                mainGUI.previousPage();
                playClickSound(player);
                return;
            }
            
            if (slot == bottomRow + 5 && event.getCurrentItem().getType() == Material.ARROW) {
                mainGUI.nextPage();
                playClickSound(player);
                return;
            }
            
            // Coba temukan kategori berdasarkan slot
            Category category = mainGUI.getCategoryAt(slot);
            if (category != null) {
                new CategoryShopGUI(plugin, player, category).open();
                playClickSound(player);
            }
        }
        
        // Category shop menu (daftar item dalam kategori)
        else if (holder instanceof CategoryShopGUI categoryGUI) {
            event.setCancelled(true);
            
            if (event.getCurrentItem() == null || event.getCurrentItem().getType() == Material.AIR) return;
            
            int slot = event.getRawSlot();
            if (slot < 0 || slot >= event.getInventory().getSize()) return;
            
            // Tombol navigasi halaman
            int bottomRow = 5 * 9;
            if (slot == bottomRow + 3 && event.getCurrentItem().getType() == Material.ARROW) {
                categoryGUI.previousPage();
                playClickSound(player);
                return;
            }
            
            if (slot == bottomRow + 5 && event.getCurrentItem().getType() == Material.ARROW) {
                categoryGUI.nextPage();
                playClickSound(player);
                return;
            }
            
            // Tombol kembali
            if (slot == bottomRow && event.getCurrentItem().getType() == Material.OAK_DOOR) {
                new MainShopGUI(plugin, player).open();
                playClickSound(player);
                return;
            }
            
            // Klik pada item
            ShopItem item = categoryGUI.getItemAt(slot);
            if (item != null) {
                handleItemClick(player, categoryGUI.getCategory(), item, event.getClick());
            }
        }
        
        // Item details menu
        else if (holder instanceof ItemDetailsGUI detailsGUI) {
            event.setCancelled(true);
            
            if (event.getCurrentItem() == null || event.getCurrentItem().getType() == Material.AIR) return;
            
            int slot = event.getRawSlot();
            if (slot < 0 || slot >= event.getInventory().getSize()) return;
            
            // Tombol kembali
            if (slot == 4 * 9 + 4 && event.getCurrentItem().getType() == Material.OAK_DOOR) {
                new CategoryShopGUI(plugin, player, detailsGUI.getCategory()).open();
                playClickSound(player);
                return;
            }
            
            // Tombol beli
            if (slot == 29 && detailsGUI.getItem().canBuy()) {
                boolean needConfirm = !plugin.getConfigManager().getConfig().getBoolean("shop.buy-without-confirm", false);
                
                if (needConfirm) {
                    // Buka GUI konfirmasi pembelian
                    new ConfirmationGUI(plugin, player, detailsGUI.getItem(), 
                            detailsGUI.getItem().getAmount(), detailsGUI.getCategory(), true).open();
                } else {
                    ShopUtils.buyItem(plugin, player, detailsGUI.getItem(), detailsGUI.getItem().getAmount());
                }
                return;
            }
            
            // Tombol beli stack
            if (slot == 30 && detailsGUI.getItem().canBuy()) {
                boolean needConfirm = !plugin.getConfigManager().getConfig().getBoolean("shop.buy-without-confirm", false);
                
                if (needConfirm) {
                    // Buka GUI konfirmasi pembelian stack
                    new ConfirmationGUI(plugin, player, detailsGUI.getItem(), 
                            64, detailsGUI.getCategory(), true).open();
                } else {
                    ShopUtils.buyItem(plugin, player, detailsGUI.getItem(), 64);
                }
                return;
            }
            
            // Tombol beli jumlah kustom
            if (slot == 31 && event.getCurrentItem().getType() == Material.COMPASS && detailsGUI.getItem().canBuy()) {
                Material itemMaterial = Material.valueOf(detailsGUI.getItem().getMaterial().toUpperCase());
                ShopItem shopItem = detailsGUI.getItem();
                Category shopCategory = detailsGUI.getCategory();
                
                // Tutup inventory saat ini
                player.closeInventory();
                
                // Request input jumlah via chat
                player.sendMessage(ChatColor.GREEN + "Masukkan jumlah " + 
                    ChatColor.translateAlternateColorCodes('&', shopItem.getName()) + 
                    ChatColor.GREEN + " yang ingin dibeli:");
                
                plugin.getChatInputManager().requestInput(player, "Masukkan jumlah (atau 'batal' untuk membatalkan):", input -> {
                    try {
                        // Cek apakah input adalah "batal"
                        if (input.equalsIgnoreCase("batal")) {
                            player.sendMessage(ChatColor.YELLOW + "Pembelian dibatalkan.");
                            new ItemDetailsGUI(plugin, player, shopCategory, shopItem).open();
                            return;
                        }
                        
                        // Parse input jumlah
                        int amount = Integer.parseInt(input);
                        
                        // Validasi jumlah
                        if (amount <= 0) {
                            player.sendMessage(ChatColor.RED + "Jumlah harus lebih dari 0.");
                            new ItemDetailsGUI(plugin, player, shopCategory, shopItem).open();
                            return;
                        }
                        
                        // Konfirmasi atau langsung proses tergantung konfigurasi
                        boolean needConfirm = !plugin.getConfigManager().getConfig().getBoolean("shop.buy-without-confirm", false);
                        
                        if (needConfirm) {
                            // Buka GUI konfirmasi pembelian dengan jumlah kustom
                            new ConfirmationGUI(plugin, player, shopItem, amount, shopCategory, true).open();
                        } else {
                            // Langsung proses pembelian
                            ShopUtils.buyItem(plugin, player, shopItem, amount);
                            // Buka kembali detail item GUI
                            new ItemDetailsGUI(plugin, player, shopCategory, shopItem).open();
                        }
                    } catch (NumberFormatException e) {
                        player.sendMessage(ChatColor.RED + "Masukkan angka yang valid.");
                        new ItemDetailsGUI(plugin, player, shopCategory, shopItem).open();
                    }
                }, 30); // Timeout 30 detik
                
                return;
            }
            
            // Tombol jual
            if (slot == 33 && detailsGUI.getItem().canSell()) {
                boolean needConfirm = !plugin.getConfigManager().getConfig().getBoolean("shop.sell-without-confirm", false);
                
                if (needConfirm) {
                    // Buka GUI konfirmasi penjualan
                    new ConfirmationGUI(plugin, player, detailsGUI.getItem(), 
                            detailsGUI.getItem().getAmount(), detailsGUI.getCategory(), false).open();
                } else {
                    ShopUtils.sellItem(plugin, player, detailsGUI.getItem(), detailsGUI.getItem().getAmount());
                }
                return;
            }
            
            // Tombol jual semua
            if (slot == 32 && detailsGUI.getItem().canSell()) {
                boolean needConfirm = !plugin.getConfigManager().getConfig().getBoolean("shop.sell-without-confirm", false);
                
                if (needConfirm) {
                    // Untuk jual semua, kita perlu menghitung jumlahnya dulu
                    Material material = Material.valueOf(detailsGUI.getItem().getMaterial().toUpperCase());
                    int totalAmount = ShopUtils.countItems(player, material);
                    
                    if (totalAmount > 0) {
                        // Buka GUI konfirmasi penjualan semua
                        new ConfirmationGUI(plugin, player, detailsGUI.getItem(), 
                                totalAmount, detailsGUI.getCategory(), false).open();
                    } else {
                        Messages.send(player, "shop.sell-failed");
                    }
                } else {
                    ShopUtils.sellAllItems(plugin, player, detailsGUI.getItem());
                }
                return;
            }
            
            // Tombol jual jumlah kustom
            if (slot == 34 && event.getCurrentItem().getType() == Material.COMPASS && detailsGUI.getItem().canSell()) {
                Material itemMaterial = Material.valueOf(detailsGUI.getItem().getMaterial().toUpperCase());
                ShopItem shopItem = detailsGUI.getItem();
                Category shopCategory = detailsGUI.getCategory();
                
                // Hitung berapa banyak yang dimiliki pemain
                int playerHas = ShopUtils.countItems(player, itemMaterial);
                
                if (playerHas <= 0) {
                    Messages.send(player, "shop.sell-failed");
                    return;
                }
                
                // Tutup inventory saat ini
                player.closeInventory();
                
                // Request input jumlah via chat dengan prompt yang lebih jelas
                String itemName = ChatColor.translateAlternateColorCodes('&', shopItem.getName());
                plugin.getChatInputManager().requestInput(player, 
                    "Masukkan jumlah " + itemName + " yang ingin dijual (maksimal " + playerHas + ")",
                    input -> {
                        try {
                            // Cek apakah input adalah "batal"
                            if (input.equalsIgnoreCase("batal")) {
                                player.sendMessage(ChatColor.YELLOW + "Penjualan dibatalkan.");
                                new ItemDetailsGUI(plugin, player, shopCategory, shopItem).open();
                                return;
                            }
                            
                            // Parse input jumlah
                            int amount = Integer.parseInt(input);
                            
                            // Validasi jumlah
                            if (amount <= 0) {
                                player.sendMessage(ChatColor.RED + "Jumlah harus lebih dari 0.");
                                new ItemDetailsGUI(plugin, player, shopCategory, shopItem).open();
                                return;
                            }
                            
                            // Validasi bahwa pemain memiliki cukup item
                            if (amount > playerHas) {
                                player.sendMessage(ChatColor.RED + "Anda hanya memiliki " + playerHas + " item.");
                                new ItemDetailsGUI(plugin, player, shopCategory, shopItem).open();
                                return;
                            }
                            
                            // Konfirmasi atau langsung proses tergantung konfigurasi
                            boolean needConfirm = !plugin.getConfigManager().getConfig().getBoolean("shop.sell-without-confirm", false);
                            
                            if (needConfirm) {
                                // Buka GUI konfirmasi penjualan dengan jumlah kustom
                                new ConfirmationGUI(plugin, player, shopItem, amount, shopCategory, false).open();
                            } else {
                                // Langsung proses penjualan
                                ShopUtils.sellItem(plugin, player, shopItem, amount);
                                // Buka kembali detail item GUI
                                new ItemDetailsGUI(plugin, player, shopCategory, shopItem).open();
                            }
                        } catch (NumberFormatException e) {
                            player.sendMessage(ChatColor.RED + "Masukkan angka yang valid.");
                            new ItemDetailsGUI(plugin, player, shopCategory, shopItem).open();
                        }
                    }, 30); // Timeout 30 detik
                
                return;
            }
        }
        
        // Konfirmasi menu
        else if (holder instanceof ConfirmationGUI confirmGUI) {
            event.setCancelled(true);
            
            if (event.getRawSlot() >= event.getInventory().getSize()) return;
            
            int slot = event.getRawSlot();
            ItemStack clickedItem = event.getCurrentItem();
            
            if (clickedItem == null || clickedItem.getType() == Material.AIR) return;
            
            // Konfirmasi transaksi
            if ((slot == 37 || slot == 38) && clickedItem.getType() == Material.LIME_CONCRETE) {
                confirmGUI.processTransaction();
                player.closeInventory();
                return;
            }
            
            // Batal
            if ((slot == 42 || slot == 43) && clickedItem.getType() == Material.RED_CONCRETE) {
                confirmGUI.openPreviousMenu();
                return;
            }
            
            // Tombol pengurang jumlah (- buttons)
            if (slot >= 19 && slot <= 21) { // Changed from slot <= 22 to slot <= 21 since we removed -10
                int change = 0;
                switch (slot) {
                    case 19: change = -64; break;
                    case 20: change = -32; break;
                    case 21: change = -1; break;  // moved from slot 22 to 21
                }
                
                if (change != 0) {
                    // Jika shift-click, kalikan dengan 5
                    int multiplier = event.isShiftClick() ? 5 : 1;
                    confirmGUI.changeAmount(change, multiplier);
                    return;
                }
            }
            
            // Tombol penambah jumlah (+ buttons)
            if (slot >= 23 && slot <= 25) {
                int change = 0;
                switch (slot) {
                    case 23: change = 1; break;
                    case 24: change = 32; break;
                    case 25: change = 64; break;
                }
                
                if (change != 0) {
                    // Jika shift-click, kalikan dengan 5
                    int multiplier = event.isShiftClick() ? 5 : 1;
                    confirmGUI.changeAmount(change, multiplier);
                    return;
                }
            }
            
            // Tombol preset jumlah
            if (slot == 30) {
                confirmGUI.updateAmount(64); // 1 Stack
                return;
            }
            if (slot == 31) {
                confirmGUI.updateAmount(320); // 5 Stacks
                return; 
            }
            if (slot == 32) {
                confirmGUI.updateAmount(1728); // 27 Stacks
                return;
            }
            
            // Jumlah kustom
            if (slot == 28 && clickedItem.getType() == Material.NAME_TAG) {
                confirmGUI.requestCustomAmount();
                return;
            }
        }
        
        // SellGUI - memungkinkan interaksi normal
        else if (holder instanceof SellGUI) {
            // Tidak perlu membatalkan event
            // Pemain dapat berinteraksi dengan SellGUI seperti chest normal
            return;
        }
    }
    
    @EventHandler
    public void onInventoryClose(InventoryCloseEvent event) {
        if (!(event.getPlayer() instanceof Player)) return;
        
        InventoryHolder holder = event.getInventory().getHolder();
        
        // Proses penjualan saat SellGUI ditutup
        if (holder instanceof SellGUI sellGUI) {
            sellGUI.processSale();
        }
    }
    
    private void handleItemClick(Player player, Category category, ShopItem item, ClickType clickType) {
        // Konfigurasi: apakah memerlukan konfirmasi untuk beli/jual
        boolean buyWithoutConfirm = plugin.getConfigManager().getConfig().getBoolean("shop.buy-without-confirm", false);
        boolean sellWithoutConfirm = plugin.getConfigManager().getConfig().getBoolean("shop.sell-without-confirm", false);
        
        // Detail item (shift + klik)
        if (clickType.isShiftClick()) {
            new ItemDetailsGUI(plugin, player, category, item).open();
            playClickSound(player);
        }
        // Beli (klik kiri)
        else if (clickType.isLeftClick() && item.canBuy()) {
            if (buyWithoutConfirm) {
                ShopUtils.buyItem(plugin, player, item, item.getAmount());
            } else {
                // Buka GUI konfirmasi
                new ConfirmationGUI(plugin, player, item, item.getAmount(), category, true).open();
            }
        } 
        // Jual (klik kanan)
        else if (clickType.isRightClick() && item.canSell()) {
            if (sellWithoutConfirm) {
                ShopUtils.sellItem(plugin, player, item, item.getAmount());
            } else {
                // Buka GUI konfirmasi
                new ConfirmationGUI(plugin, player, item, item.getAmount(), category, false).open();
            }
        }
    }
    
    private void playClickSound(Player player) {
        player.playSound(player.getLocation(), Sound.UI_BUTTON_CLICK, 0.5f, 1.0f);
    }
}