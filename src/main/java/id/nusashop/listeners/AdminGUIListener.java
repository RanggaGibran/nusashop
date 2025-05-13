package id.nusashop.listeners;

import id.nusashop.NusaShop;
import id.nusashop.gui.admin.*;
import id.nusashop.models.Category;
import id.nusashop.models.ShopItem;
import id.nusashop.utils.Messages;

import org.bukkit.ChatColor;
import org.bukkit.Material;
import org.bukkit.Sound;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.Listener;
import org.bukkit.event.inventory.InventoryClickEvent;
import org.bukkit.inventory.ItemStack;

/**
 * Listener untuk GUI admin shop
 */
public class AdminGUIListener implements Listener {
    private final NusaShop plugin;
    
    public AdminGUIListener(NusaShop plugin) {
        this.plugin = plugin;
    }
    
    @EventHandler
    public void onInventoryClick(InventoryClickEvent event) {
        if (!(event.getWhoClicked() instanceof Player player)) return;
        if (!player.hasPermission("nusashop.admin")) return;
        
        // Cek apakah ini adalah inventory GUI admin
        if (event.getInventory().getHolder() instanceof AdminShopGUI) {
            event.setCancelled(true);
            
            if (event.getCurrentItem() == null || event.getCurrentItem().getType() == Material.AIR) return;
            
            int slot = event.getRawSlot();
            
            // Kelola kategori
            if (slot == 11 && event.getCurrentItem().getType() == Material.BOOKSHELF) {
                new CategoryManageGUI(plugin, player).open();
                playClickSound(player);
                return;
            }
            
            // Kelola item
            if (slot == 13 && event.getCurrentItem().getType() == Material.CHEST) {
                // Disini perlu daftar kategori dulu
                player.sendMessage(ChatColor.YELLOW + "Pilih kategori terlebih dahulu:");
                
                int i = 1;
                for (Category category : plugin.getShopManager().getCategories()) {
                    player.sendMessage(ChatColor.GOLD + "" + i + ". " + 
                            ChatColor.translateAlternateColorCodes('&', category.getName()));
                    i++;
                }
                
                player.sendMessage(ChatColor.YELLOW + "Ketik nomor kategori:");
                plugin.getChatInputManager().requestInput(player, "Masukkan nomor kategori:", input -> {
                    try {
                        int index = Integer.parseInt(input) - 1;
                        if (index >= 0 && index < plugin.getShopManager().getCategories().size()) {
                            Category selected = plugin.getShopManager().getCategories().get(index);
                            new ItemManageGUI(plugin, player, selected).open();
                        } else {
                            player.sendMessage(ChatColor.RED + "Kategori tidak ditemukan!");
                        }
                    } catch (NumberFormatException e) {
                        player.sendMessage(ChatColor.RED + "Masukkan angka yang valid!");
                    }
                }, 30);
                return;
            }
            
            // Reload plugin
            if (slot == 15 && event.getCurrentItem().getType() == Material.COMPARATOR) {
                plugin.getConfigManager().reloadConfigs();
                Messages.send(player, "admin.reload");
                player.closeInventory();
                return;
            }
        }
        
        // Category Management GUI
        else if (event.getInventory().getHolder() instanceof CategoryManageGUI categoryGUI) {
            event.setCancelled(true);
            
            if (event.getCurrentItem() == null || event.getCurrentItem().getType() == Material.AIR) return;
            
            int slot = event.getRawSlot();
            int totalRows = event.getInventory().getSize() / 9;
            
            // Tombol tambah kategori
            if (slot == (totalRows - 1) * 9 + 2 && event.getCurrentItem().getType() == Material.LIME_CONCRETE) {
                new CategoryEditGUI(plugin, player, null).open();
                playClickSound(player);
                return;
            }
            
            // Tombol kembali
            if (slot == (totalRows - 1) * 9 + 6 && event.getCurrentItem().getType() == Material.ARROW) {
                new AdminShopGUI(plugin, player).open();
                playClickSound(player);
                return;
            }
            
            // Klik pada kategori
            Category category = categoryGUI.getCategoryAt(slot);
            if (category != null) {
                if (event.isLeftClick() && !event.isShiftClick()) {
                    // Edit kategori
                    new CategoryEditGUI(plugin, player, category).open();
                    playClickSound(player);
                } else if (event.isRightClick()) {
                    // Hapus kategori - minta konfirmasi
                    player.sendMessage(ChatColor.YELLOW + "Apakah Anda yakin ingin menghapus kategori " + 
                            ChatColor.RED + ChatColor.translateAlternateColorCodes('&', category.getName()) + 
                            ChatColor.YELLOW + "? Ketik 'ya' untuk konfirmasi:");
                    
                    plugin.getChatInputManager().requestInput(player, "Ketik 'ya' untuk menghapus:", input -> {
                        if (input.equalsIgnoreCase("ya")) {
                            plugin.getShopManager().removeCategory(category.getId());
                            player.sendMessage(ChatColor.GREEN + "Kategori berhasil dihapus!");
                            new CategoryManageGUI(plugin, player).open();
                        } else {
                            player.sendMessage(ChatColor.RED + "Penghapusan dibatalkan.");
                            new CategoryManageGUI(plugin, player).open();
                        }
                    }, 30);
                    player.closeInventory();
                } else if (event.isShiftClick() && event.isLeftClick()) {
                    // Lihat item dalam kategori
                    new ItemManageGUI(plugin, player, category).open();
                    playClickSound(player);
                }
            }
        }
        
        // Item Management GUI
        else if (event.getInventory().getHolder() instanceof ItemManageGUI itemGUI) {
            event.setCancelled(true);
            
            if (event.getCurrentItem() == null || event.getCurrentItem().getType() == Material.AIR) return;
            
            int slot = event.getRawSlot();
            int bottomRow = 5 * 9;
            
            // Navigasi halaman
            if (slot == bottomRow + 3 && event.getCurrentItem().getType() == Material.ARROW) {
                itemGUI.previousPage();
                playClickSound(player);
                return;
            }
            
            if (slot == bottomRow + 5 && event.getCurrentItem().getType() == Material.ARROW) {
                itemGUI.nextPage();
                playClickSound(player);
                return;
            }
            
            // Tombol tambah item
            if (slot == bottomRow + 1 && event.getCurrentItem().getType() == Material.LIME_CONCRETE) {
                new ItemEditGUI(plugin, player, itemGUI.getCategory(), null).open();
                playClickSound(player);
                return;
            }
            
            // Tombol kembali
            if (slot == bottomRow + 7 && event.getCurrentItem().getType() == Material.OAK_DOOR) {
                new CategoryManageGUI(plugin, player).open();
                playClickSound(player);
                return;
            }
            
            // Klik pada item
            ShopItem item = itemGUI.getItemAt(slot);
            if (item != null) {
                if (event.isLeftClick()) {
                    // Edit item
                    new ItemEditGUI(plugin, player, itemGUI.getCategory(), item).open();
                    playClickSound(player);
                } else if (event.isRightClick()) {
                    // Hapus item - minta konfirmasi
                    player.sendMessage(ChatColor.YELLOW + "Apakah Anda yakin ingin menghapus item " + 
                            ChatColor.RED + ChatColor.translateAlternateColorCodes('&', item.getName()) + 
                            ChatColor.YELLOW + "? Ketik 'ya' untuk konfirmasi:");
                    
                    plugin.getChatInputManager().requestInput(player, "Ketik 'ya' untuk menghapus:", input -> {
                        if (input.equalsIgnoreCase("ya")) {
                            itemGUI.getCategory().removeItem(item);
                            player.sendMessage(ChatColor.GREEN + "Item berhasil dihapus!");
                            new ItemManageGUI(plugin, player, itemGUI.getCategory()).open();
                        } else {
                            player.sendMessage(ChatColor.RED + "Penghapusan dibatalkan.");
                            new ItemManageGUI(plugin, player, itemGUI.getCategory()).open();
                        }
                    }, 30);
                    player.closeInventory();
                }
            }
        }
        
        // Category Edit GUI
        else if (event.getInventory().getHolder() instanceof CategoryEditGUI categoryEditGUI) {
            event.setCancelled(true);
            
            if (event.getCurrentItem() == null || event.getCurrentItem().getType() == Material.AIR) return;
            
            int slot = event.getRawSlot();
            
            // Tombol kembali
            if (slot == 18 && event.getCurrentItem().getType() == Material.ARROW) {
                new CategoryManageGUI(plugin, player).open();
                playClickSound(player);
                return;
            }
            
            // Tombol simpan
            if (slot == 22 && event.getCurrentItem().getType() == Material.EMERALD_BLOCK) {
                // Proses simpan kategori
                if (categoryEditGUI.isNew()) {
                    player.sendMessage(ChatColor.RED + "Silakan isi semua informasi kategori terlebih dahulu!");
                    return;
                }
                
                plugin.getShopManager().saveCategory(categoryEditGUI.getCategory());
                player.sendMessage(ChatColor.GREEN + "Kategori berhasil disimpan!");
                new CategoryManageGUI(plugin, player).open();
                return;
            }
            
            // TODO: Implementasi untuk editing kategori - ID, nama, dsb
            // Akan dibuat dengan sistem chat input. Untuk implementasi yang lebih singkat, 
            // kami akan berfokus pada fitur utama terlebih dahulu.
        }
        
        // Item Edit GUI
        else if (event.getInventory().getHolder() instanceof ItemEditGUI itemEditGUI) {
            event.setCancelled(true);
            
            if (event.getCurrentItem() == null || event.getCurrentItem().getType() == Material.AIR) return;
            
            int slot = event.getRawSlot();
            
            // Tombol kembali
            if (slot == 27 && event.getCurrentItem().getType() == Material.ARROW) {
                new ItemManageGUI(plugin, player, itemEditGUI.getCategory()).open();
                playClickSound(player);
                return;
            }
            
            // Tombol simpan
            if (slot == 31 && event.getCurrentItem().getType() == Material.EMERALD_BLOCK) {
                // Proses simpan item
                if (itemEditGUI.isNew()) {
                    player.sendMessage(ChatColor.RED + "Silakan isi semua informasi item terlebih dahulu!");
                    return;
                }
                
                plugin.getShopManager().saveItem(itemEditGUI.getCategory(), itemEditGUI.getItem());
                player.sendMessage(ChatColor.GREEN + "Item berhasil disimpan!");
                new ItemManageGUI(plugin, player, itemEditGUI.getCategory()).open();
                return;
            }
            
            // TODO: Implementasi untuk editing item - ID, nama, material, harga, dsb
            // Akan dibuat dengan sistem chat input. Untuk implementasi yang lebih singkat, 
            // kami akan berfokus pada fitur utama terlebih dahulu.
        }
    }
    
    private void playClickSound(Player player) {
        player.playSound(player.getLocation(), Sound.UI_BUTTON_CLICK, 0.5f, 1.0f);
    }
}