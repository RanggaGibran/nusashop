package id.nusashop.listeners;

import id.nusashop.NusaShop;
import id.nusashop.api.NusaShopAPI;
import id.nusashop.models.SellWand;
import id.nusashop.utils.Animations;
import id.nusashop.utils.Messages;

import org.bukkit.ChatColor;
import org.bukkit.Material;
import org.bukkit.Sound;
import org.bukkit.block.Block;
import org.bukkit.block.Chest;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.Listener;
import org.bukkit.event.block.Action;
import org.bukkit.event.player.PlayerInteractEvent;
import org.bukkit.inventory.Inventory;
import org.bukkit.inventory.ItemStack;

/**
 * Listener untuk mendeteksi penggunaan Sell Wand pada chest
 */
public class SellWandListener implements Listener {
    private final NusaShop plugin;
    private final SellWand sellWand;
    
    public SellWandListener(NusaShop plugin) {
        this.plugin = plugin;
        this.sellWand = new SellWand(plugin);
    }
    
    @EventHandler
    public void onPlayerInteract(PlayerInteractEvent event) {
        Player player = event.getPlayer();
        ItemStack item = event.getItem();
        
        // Cek apakah player klik kanan dengan sell wand
        if (event.getAction() != Action.RIGHT_CLICK_BLOCK || item == null || !sellWand.isSellWand(item)) {
            return;
        }
        
        Block clickedBlock = event.getClickedBlock();
        if (clickedBlock == null || !(clickedBlock.getState() instanceof Chest)) {
            return;
        }
        
        event.setCancelled(true); // Cegah chest terbuka
        
        // Cek permission
        if (!player.hasPermission("nusashop.sellwand.use")) {
            Messages.send(player, "sellwand.no-permission");
            return;
        }
        
        // Proses penjualan isi chest
        Chest chest = (Chest) clickedBlock.getState();
        Inventory chestInventory = chest.getInventory();
        
        // Gunakan API untuk menjual semua item dari inventory
        NusaShopAPI.TransactionSummary summary = NusaShopAPI.getInstance().sellAllFromInventory(player, chestInventory);
        
        if (summary.hasItemsSold()) {
            // Berhasil menjual, kurangi penggunaan wand
            boolean hasUsesLeft = sellWand.useWand(item);
            
            // Animasi sukses
            String message = ChatColor.GREEN + "Berhasil menjual " + 
                ChatColor.YELLOW + summary.getTotalItemsSold() + ChatColor.GREEN + " item seharga " + 
                ChatColor.GOLD + String.format("%.2f", summary.getTotalEarned()) + ChatColor.GREEN + " coins";
            
            Animations.playTransactionCompleteAnimation(plugin, player, message);
            player.playSound(player.getLocation(), Sound.ENTITY_PLAYER_LEVELUP, 0.5f, 1.0f);
            
            // Notifikasi uses tersisa
            int remainingUses = sellWand.getRemainingUses(item);
            if (remainingUses > 0) {
                player.sendMessage(ChatColor.YELLOW + "Penggunaan tongkat tersisa: " + 
                    ChatColor.WHITE + remainingUses);
            } else if (!hasUsesLeft) {
                player.sendMessage(ChatColor.RED + "Tongkat penjual telah kehabisan penggunaan!");
                player.getInventory().setItemInMainHand(new ItemStack(Material.AIR));
                player.playSound(player.getLocation(), Sound.ENTITY_ITEM_BREAK, 1.0f, 0.5f);
            }
        } else {
            // Tidak ada yang terjual
            player.sendMessage(ChatColor.YELLOW + "Tidak ada item yang dapat dijual dalam chest ini.");
            player.playSound(player.getLocation(), Sound.ENTITY_VILLAGER_NO, 0.5f, 1.0f);
        }
    }
}