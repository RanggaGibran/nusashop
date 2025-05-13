package id.nusashop.listeners;

import id.nusashop.NusaShop;
import id.nusashop.gui.*;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.EventPriority;
import org.bukkit.event.Listener;
import org.bukkit.event.inventory.*;
import org.bukkit.inventory.InventoryHolder;

/**
 * Listener untuk mencegah player mengambil item dari GUI
 */
public class GuiProtectionListener implements Listener {

    private final NusaShop plugin;
    
    public GuiProtectionListener(NusaShop plugin) {
        this.plugin = plugin;
    }
    
    /**
     * Handler untuk InventoryClickEvent dengan prioritas tertinggi
     * Memastikan bahwa item-item dalam GUI tidak bisa diambil/dipindahkan
     */
    @EventHandler(priority = EventPriority.HIGHEST)
    public void onInventoryClick(InventoryClickEvent event) {
        if (!(event.getWhoClicked() instanceof Player)) return;
        
        InventoryHolder holder = event.getInventory().getHolder();
        
        // Cek apakah inventory adalah salah satu GUI dari plugin
        if (holder instanceof MainShopGUI ||
            holder instanceof CategoryShopGUI ||
            holder instanceof ItemDetailsGUI ||
            holder instanceof ConfirmationGUI ||
            holder instanceof SellGUI) {
            
            // Cek aksi berbahaya yang bisa mengakibatkan item diambil
            switch (event.getAction()) {
                case MOVE_TO_OTHER_INVENTORY:
                case COLLECT_TO_CURSOR:
                case HOTBAR_MOVE_AND_READD:
                case HOTBAR_SWAP:
                case CLONE_STACK:
                case PICKUP_ALL:
                case PICKUP_HALF:
                case PICKUP_SOME:
                case PICKUP_ONE:
                case PLACE_ALL:
                case PLACE_SOME:
                case PLACE_ONE:
                case SWAP_WITH_CURSOR:
                    event.setCancelled(true);
                    break;
                default:
                    // Untuk aksi lainnya, tetap cancel jika container adalah GUI
                    if (event.getClickedInventory() != null && 
                        event.getClickedInventory().getHolder() instanceof MainShopGUI ||
                        event.getClickedInventory().getHolder() instanceof CategoryShopGUI ||
                        event.getClickedInventory().getHolder() instanceof ItemDetailsGUI ||
                        event.getClickedInventory().getHolder() instanceof ConfirmationGUI ||
                        event.getClickedInventory().getHolder() instanceof SellGUI) {
                        
                        event.setCancelled(true);
                    }
                    break;
            }
        }
    }
    
    /**
     * Handler untuk InventoryDragEvent
     * Mencegah player drag item dalam GUI
     */
    @EventHandler(priority = EventPriority.HIGHEST)
    public void onInventoryDrag(InventoryDragEvent event) {
        if (!(event.getWhoClicked() instanceof Player)) return;
        
        InventoryHolder holder = event.getInventory().getHolder();
        
        // Cancel event jika inventory adalah GUI plugin
        if (holder instanceof MainShopGUI ||
            holder instanceof CategoryShopGUI ||
            holder instanceof ItemDetailsGUI ||
            holder instanceof ConfirmationGUI ||
            holder instanceof SellGUI) {
            
            // Cek apakah drag mempengaruhi slot di GUI
            boolean affectsGUI = false;
            int topSize = event.getView().getTopInventory().getSize();
            
            for (int slot : event.getRawSlots()) {
                if (slot < topSize) {
                    affectsGUI = true;
                    break;
                }
            }
            
            if (affectsGUI) {
                event.setCancelled(true);
            }
        }
    }
    
    /**
     * Handler untuk InventoryCreativeEvent
     * Mencegah player dalam mode creative mengambil item dari GUI
     */
    @EventHandler(priority = EventPriority.HIGHEST)
    public void onCreativeInventoryClick(InventoryCreativeEvent event) {
        if (!(event.getWhoClicked() instanceof Player)) return;
        
        InventoryHolder holder = event.getInventory().getHolder();
        
        // Cancel event jika inventory adalah GUI plugin
        if (holder instanceof MainShopGUI ||
            holder instanceof CategoryShopGUI ||
            holder instanceof ItemDetailsGUI ||
            holder instanceof ConfirmationGUI ||
            holder instanceof SellGUI) {
            
            event.setCancelled(true);
        }
    }
}