package id.nusashop.utils;

import id.nusashop.NusaShop;

import org.bukkit.entity.Player;
import org.bukkit.scheduler.BukkitRunnable;

/**
 * Utilitas untuk animasi dan efek visual
 */
public class Animations {

    /**
     * Metode kosong untuk menggantikan efek particle
     * Sudah tidak menggunakan particle untuk mengurangi lag
     * 
     * @param plugin Plugin instance
     * @param player Player yang melakukan transaksi
     * @param isBuying True jika pembelian, false jika penjualan
     */
    public static void playTransactionEffect(NusaShop plugin, Player player, boolean isBuying) {
        // Metode dibiarkan kosong untuk menghindari efek particle yang menyebabkan lag
    }
    
    /**
     * Animasi penyelesaian transaksi
     * 
     * @param plugin Plugin instance
     * @param player Player yang melakukan transaksi
     * @param message Pesan untuk ditampilkan setelah animasi
     */
    public static void playTransactionCompleteAnimation(NusaShop plugin, Player player, String message) {
        new BukkitRunnable() {
            @Override
            public void run() {
                player.sendTitle("§a✓", "§7Transaksi Selesai", 5, 20, 10);
                player.sendMessage(message);
                // Tidak perlu memanggil playTransactionEffect lagi
                // untuk menghindari lag dari particle
            }
        }.runTask(plugin);
    }
}