package id.nusashop.managers;

import id.nusashop.NusaShop;

import org.bukkit.ChatColor;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.EventPriority;
import org.bukkit.event.Listener;
import org.bukkit.event.player.AsyncPlayerChatEvent;
import org.bukkit.event.player.PlayerQuitEvent;

import java.util.HashMap;
import java.util.Map;
import java.util.UUID;
import java.util.function.Consumer;

/**
 * Manager untuk menangani input chat dari player
 */
public class ChatInputManager implements Listener {
    private final NusaShop plugin;
    private final Map<UUID, PendingInput> pendingInputs = new HashMap<>();
    
    public ChatInputManager(NusaShop plugin) {
        this.plugin = plugin;
        plugin.getServer().getPluginManager().registerEvents(this, plugin);
        plugin.getLogger().info("ChatInputManager registered!");
    }
    
    /**
     * Request input dari player
     * 
     * @param player Player yang diminta input
     * @param prompt Pesan prompt untuk ditampilkan
     * @param callback Callback yang akan dipanggil dengan input player
     * @param timeoutSeconds Waktu timeout dalam detik
     */
    public void requestInput(Player player, String prompt, Consumer<String> callback, int timeoutSeconds) {
        UUID playerId = player.getUniqueId();
        
        // Log untuk debug
        plugin.getLogger().info("Requesting input from " + player.getName());
        
        // Batalkan input sebelumnya jika ada
        if (pendingInputs.containsKey(playerId)) {
            pendingInputs.get(playerId).cancel();
        }
        
        // Buat input baru
        PendingInput input = new PendingInput(player, callback);
        pendingInputs.put(playerId, input);
        
        // Tampilkan prompt dengan delay kecil agar terbaca setelah inventory tertutup
        plugin.getServer().getScheduler().runTaskLater(plugin, () -> {
            player.sendMessage("");
            player.sendMessage(ChatColor.YELLOW + "» " + ChatColor.GOLD + prompt);
            player.sendMessage(ChatColor.GRAY + "Ketik 'batal' untuk membatalkan");
        }, 2L);
        
        // Set timeout
        if (timeoutSeconds > 0) {
            plugin.getServer().getScheduler().runTaskLater(plugin, () -> {
                if (pendingInputs.containsKey(playerId) && pendingInputs.get(playerId) == input) {
                    pendingInputs.remove(playerId);
                    player.sendMessage(ChatColor.RED + "Waktu habis. Input dibatalkan.");
                }
            }, timeoutSeconds * 20L);
        }
    }
    
    /**
     * Handle chat input
     */
    @EventHandler(priority = EventPriority.LOWEST)
    public void onPlayerChat(AsyncPlayerChatEvent event) {
        Player player = event.getPlayer();
        UUID playerId = player.getUniqueId();
        
        if (pendingInputs.containsKey(playerId)) {
            // Log untuk debug
            plugin.getLogger().info("Received chat input from " + player.getName() + ": " + event.getMessage());
            
            event.setCancelled(true);
            String input = event.getMessage();
            
            // Hapus input dari map
            PendingInput pendingInput = pendingInputs.remove(playerId);
            
            // Execute callback on main thread
            plugin.getServer().getScheduler().runTask(plugin, () -> {
                try {
                    pendingInput.processInput(input);
                } catch (Exception e) {
                    player.sendMessage(ChatColor.RED + "Terjadi kesalahan saat memproses input.");
                    plugin.getLogger().severe("Error processing chat input: " + e.getMessage());
                    e.printStackTrace();
                }
            });
        }
    }
    
    /**
     * Clean up when player quits
     */
    @EventHandler
    public void onPlayerQuit(PlayerQuitEvent event) {
        pendingInputs.remove(event.getPlayer().getUniqueId());
    }
    
    /**
     * Class to represent a pending input
     */
    private static class PendingInput {
        private final Player player;
        private final Consumer<String> callback;
        private boolean cancelled = false;
        
        public PendingInput(Player player, Consumer<String> callback) {
            this.player = player;
            this.callback = callback;
        }
        
        public void processInput(String input) {
            if (!cancelled) {
                callback.accept(input);
            }
        }
        
        public void cancel() {
            cancelled = true;
        }
    }
}