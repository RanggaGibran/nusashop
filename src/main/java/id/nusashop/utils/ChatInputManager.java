package id.nusashop.utils;

import id.nusashop.NusaShop;

import org.bukkit.ChatColor;
import org.bukkit.entity.Player;
import org.bukkit.scheduler.BukkitRunnable;

import java.util.HashMap;
import java.util.Map;
import java.util.UUID;
import java.util.function.Consumer;

/**
 * Manager untuk input chat dari pemain
 */
public class ChatInputManager {
    private final NusaShop plugin;
    private final Map<UUID, InputRequest> pendingInputs = new HashMap<>();
    
    public ChatInputManager(NusaShop plugin) {
        this.plugin = plugin;
    }
    
    /**
     * Request input dari pemain melalui chat
     * @param player Pemain
     * @param prompt Pesan prompt
     * @param callback Callback setelah input diterima
     * @param timeoutSeconds Timeout dalam detik
     */
    public void requestInput(Player player, String prompt, Consumer<String> callback, int timeoutSeconds) {
        UUID playerId = player.getUniqueId();
        
        // Bersihkan input lama jika ada
        pendingInputs.remove(playerId);
        
        // Tampilkan prompt
        player.sendMessage("");
        player.sendMessage(ChatColor.YELLOW + "» " + ChatColor.WHITE + prompt);
        player.sendMessage(ChatColor.GRAY + "Ketik 'batal' untuk membatalkan");
        
        // Simpan request
        pendingInputs.put(playerId, new InputRequest(callback, System.currentTimeMillis() + (timeoutSeconds * 1000)));
        
        // Atur timeout
        new BukkitRunnable() {
            @Override
            public void run() {
                // Cek apakah input sudah diproses
                InputRequest request = pendingInputs.get(playerId);
                if (request != null && System.currentTimeMillis() > request.expireTime) {
                    pendingInputs.remove(playerId);
                    player.sendMessage(ChatColor.RED + "Waktu input habis!");
                }
            }
        }.runTaskLater(plugin, timeoutSeconds * 20L);
    }
    
    /**
     * Memproses input chat dari pemain
     * @param player Pemain
     * @param message Pesan chat
     * @return true jika input diproses, false jika bukan input yang diminta
     */
    public boolean processInput(Player player, String message) {
        UUID playerId = player.getUniqueId();
        InputRequest request = pendingInputs.get(playerId);
        
        if (request != null) {
            pendingInputs.remove(playerId);
            
            // PENTING: Jalankan callback di thread utama/sync
            new BukkitRunnable() {
                @Override
                public void run() {
                    request.callback.accept(message);
                }
            }.runTask(plugin);
            
            return true;
        }
        
        return false;
    }
    
    /**
     * Class untuk menyimpan request input
     */
    private static class InputRequest {
        private final Consumer<String> callback;
        private final long expireTime;
        
        public InputRequest(Consumer<String> callback, long expireTime) {
            this.callback = callback;
            this.expireTime = expireTime;
        }
    }
}