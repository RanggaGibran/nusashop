package id.nusashop.listeners;

import id.nusashop.NusaShop;
import org.bukkit.event.EventHandler;
import org.bukkit.event.EventPriority;
import org.bukkit.event.Listener;
import org.bukkit.event.player.AsyncPlayerChatEvent;
import org.bukkit.event.player.PlayerQuitEvent;

/**
 * Listener untuk chat input
 */
public class ChatInputListener implements Listener {
    private final NusaShop plugin;
    
    public ChatInputListener(NusaShop plugin) {
        this.plugin = plugin;
    }
    
    @EventHandler(priority = EventPriority.LOW)
    public void onPlayerChat(AsyncPlayerChatEvent event) {
        // Cek apakah ada input yang ditunggu
        if (plugin.getChatInputManager().processInput(event.getPlayer(), event.getMessage())) {
            // Batalkan event chat jika ini adalah input yang ditunggu
            event.setCancelled(true);
        }
    }
    
    @EventHandler
    public void onPlayerQuit(PlayerQuitEvent event) {
        // Jika dibutuhkan, bisa menambahkan kode untuk membersihkan input tertunda
    }
}