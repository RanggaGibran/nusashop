package id.nusashop.managers;

import id.nusashop.NusaShop;
import id.nusashop.models.BlackmarketItem;
import org.bukkit.Bukkit;
import org.bukkit.ChatColor;
import org.bukkit.Sound;
import org.bukkit.boss.BarColor;
import org.bukkit.boss.BarStyle;
import org.bukkit.boss.BossBar;
import org.bukkit.configuration.ConfigurationSection;
import org.bukkit.configuration.file.FileConfiguration;
import org.bukkit.configuration.file.YamlConfiguration;
import org.bukkit.enchantments.Enchantment;
import org.bukkit.entity.Player;
import org.bukkit.permissions.Permission;
import org.bukkit.scheduler.BukkitRunnable;
import org.bukkit.scheduler.BukkitTask;

import java.io.File;
import java.io.IOException;
import java.time.LocalTime;
import java.time.format.DateTimeFormatter;
import java.time.temporal.ChronoUnit;
import java.util.ArrayList;
import java.util.Calendar;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

/**
 * Manager untuk mengelola fitur Blackmarket
 */
public class BlackmarketManager {
    private final NusaShop plugin;
    private final Map<String, BlackmarketItem> items = new HashMap<>();
    private File blackmarketFile;
    private FileConfiguration blackmarketConfig;
    private File stockFile;
    private FileConfiguration stockConfig;
    
    // Status blackmarket
    private boolean forcedOpen = false;
    private boolean isCurrentlyOpen = false;
    
    // Pengaturan umum
    private LocalTime openTime;
    private LocalTime closeTime;
    private double entryFee;
    private int closingWarningTime;
    private String openMessage;
    private String closeMessage;
    
    // Pengaturan GUI
    private String guiTitle;
    private int guiRows;
    private String backgroundMaterial;
    private String borderMaterial;
    
    // Pengaturan notifikasi
    private String vipPreOpenMessage;
    private int vipPreOpenMinutes;
    private BossBar blackmarketBar;
    
    // Jadwal tugas
    private BukkitTask openTask;
    private BukkitTask closeTask;
    private BukkitTask warningTask;
    
    // Format waktu
    private static final DateTimeFormatter TIME_FORMATTER = DateTimeFormatter.ofPattern("HH:mm");
    
    // Pengaturan rotasi
    private boolean rotationEnabled = false;
    private String rotationSchedule; // daily atau weekly
    private Map<String, List<String>> dailyGroups = new HashMap<>();
    private Map<String, List<Integer>> weeklyGroups = new HashMap<>();

    /**
     * Constructor BlackmarketManager
     * @param plugin Instance dari main plugin
     */
    public BlackmarketManager(NusaShop plugin) {
        this.plugin = plugin;
        loadConfig();
        loadStockData();
        setupSchedules();
    }
    
    /**
     * Memuat konfigurasi dari file blackmarket.yml
     */
    private void loadConfig() {
        // Pastikan file blackmarket.yml ada
        blackmarketFile = new File(plugin.getDataFolder(), "blackmarket.yml");
        if (!blackmarketFile.exists()) {
            plugin.saveResource("blackmarket.yml", false);
        }
        
        blackmarketConfig = YamlConfiguration.loadConfiguration(blackmarketFile);
        
        // Muat pengaturan umum
        try {
            openTime = LocalTime.parse(blackmarketConfig.getString("general.open-time", "20:00"), TIME_FORMATTER);
            closeTime = LocalTime.parse(blackmarketConfig.getString("general.close-time", "24:00"), TIME_FORMATTER);
            
            // Handle kasus khusus untuk 24:00 karena LocalTime tidak mendukung 24:00
            if (blackmarketConfig.getString("general.close-time", "").equals("24:00")) {
                closeTime = LocalTime.of(0, 0); // 00:00 sama dengan 24:00
            }
        } catch (Exception e) {
            plugin.getLogger().warning("Format waktu di blackmarket.yml tidak valid! Menggunakan default: 20:00-24:00");
            openTime = LocalTime.of(20, 0);
            closeTime = LocalTime.of(0, 0); // 00:00
        }
        
        entryFee = blackmarketConfig.getDouble("general.entry-fee", 5000.0);
        closingWarningTime = blackmarketConfig.getInt("general.closing-warning-time", 15);
        openMessage = ChatColor.translateAlternateColorCodes('&', blackmarketConfig.getString("general.open-message", 
                "&8[&c&lBlackmarket&8] &7Pasar gelap telah &a&lDIBUKA&7! Ketik &e/blackmarket &7untuk mengakses."));
        closeMessage = ChatColor.translateAlternateColorCodes('&', blackmarketConfig.getString("general.close-message", 
                "&8[&c&lBlackmarket&8] &7Pasar gelap telah &c&lDITUTUP&7! Datang lagi besok."));
        
        // Muat pengaturan GUI
        guiTitle = ChatColor.translateAlternateColorCodes('&', blackmarketConfig.getString("gui.title", "&8» &c&lPasar Gelap"));
        guiRows = Math.min(6, Math.max(3, blackmarketConfig.getInt("gui.rows", 6)));
        backgroundMaterial = blackmarketConfig.getString("gui.background-material", "BLACK_STAINED_GLASS_PANE");
        borderMaterial = blackmarketConfig.getString("gui.border-material", "GRAY_STAINED_GLASS_PANE");
        
        // Muat konfigurasi rotasi
        loadRotationConfig();
        
        // Muat pengaturan notifikasi
        vipPreOpenMessage = ChatColor.translateAlternateColorCodes('&', 
                blackmarketConfig.getString("notifications.vip-pre-open-message", 
                "&8[&c&lBlackmarket VIP&8] &7Pasar Gelap akan dibuka dalam {minutes} menit khusus untuk VIP!"));
        vipPreOpenMinutes = blackmarketConfig.getInt("notifications.vip-pre-open-minutes", 15);
        
        // Inisialisasi BossBar
        blackmarketBar = Bukkit.createBossBar(
            ChatColor.RED + "⚠ Pasar Gelap: " + ChatColor.GOLD + "Buka",
            BarColor.RED,
            BarStyle.SOLID
        );
        blackmarketBar.setVisible(false);
        
        // Muat item
        ConfigurationSection itemsSection = blackmarketConfig.getConfigurationSection("items");
        if (itemsSection != null) {
            for (String itemId : itemsSection.getKeys(false)) {
                ConfigurationSection itemSection = itemsSection.getConfigurationSection(itemId);
                if (itemSection != null) {
                    String name = ChatColor.translateAlternateColorCodes('&', itemSection.getString("name", itemId));
                    String material = itemSection.getString("material", "BARRIER");
                    
                    // Muat lore dengan warna
                    List<String> rawLore = itemSection.getStringList("lore");
                    List<String> lore = new ArrayList<>();
                    for (String line : rawLore) {
                        lore.add(ChatColor.translateAlternateColorCodes('&', line));
                    }
                    
                    double buyPrice = itemSection.getDouble("buy-price", -1);
                    double sellPrice = itemSection.getDouble("sell-price", -1);
                    int amount = itemSection.getInt("amount", 1);
                    int stock = itemSection.getInt("stock", 1);
                    boolean resetOnRestart = itemSection.getBoolean("reset-on-restart", true);
                    
                    BlackmarketItem blackmarketItem = new BlackmarketItem(
                        itemId, name, material, lore, buyPrice, sellPrice, amount, stock, resetOnRestart
                    );
                    
                    // Muat command jika ada
                    if (itemSection.contains("commands")) {
                        blackmarketItem.setCommandItem(true);
                        List<String> commands = itemSection.getStringList("commands");
                        blackmarketItem.setCommands(commands);
                        plugin.getLogger().info("Loaded command item: " + itemId + " with " + commands.size() + " commands");
                    }
                    
                    // Tambahkan permission jika ada
                    String permission = itemSection.getString("permission", null);
                    if (permission != null && !permission.isEmpty()) {
                        blackmarketItem.setPermission(permission);
                    }
                    
                    // Tambahkan enchantments jika ada
                    ConfigurationSection enchantSection = itemSection.getConfigurationSection("enchantments");
                    if (enchantSection != null) {
                        for (String enchantName : enchantSection.getKeys(false)) {
                            try {
                                Enchantment enchantment = Enchantment.getByName(enchantName);
                                if (enchantment != null) {
                                    int level = enchantSection.getInt(enchantName, 1);
                                    blackmarketItem.addEnchantment(enchantment, level);
                                }
                            } catch (Exception e) {
                                plugin.getLogger().warning("Enchantment tidak valid: " + enchantName);
                            }
                        }
                    }
                    
                    // Tambahkan konfigurasi rotasi jika ada
                    ConfigurationSection rotationSection = itemSection.getConfigurationSection("rotation");
                    if (rotationSection != null) {
                        blackmarketItem.setRotatingItem(true);
                        blackmarketItem.setRotationType(rotationSection.getString("type", "daily"));
                        
                        // Muat properti berdasarkan tipe rotasi
                        if (blackmarketItem.getRotationType().equals("daily")) {
                            blackmarketItem.setRotationGroup(rotationSection.getString("group", ""));
                        } else if (blackmarketItem.getRotationType().equals("weekly")) {
                            blackmarketItem.setRotationWeek(rotationSection.getInt("week", 1));
                        }
                    }
                    
                    items.put(itemId, blackmarketItem);
                    plugin.getLogger().info("Loaded blackmarket item: " + itemId);
                }
            }
        }
    }
    
    /**
     * Memuat konfigurasi rotasi dari file blackmarket.yml
     */
    private void loadRotationConfig() {
        rotationEnabled = blackmarketConfig.getBoolean("rotation.enabled", false);
        rotationSchedule = blackmarketConfig.getString("rotation.schedule", "daily");
        
        // Reset maps
        dailyGroups.clear();
        weeklyGroups.clear();

        // Muat grup rotasi harian
        ConfigurationSection dailySection = blackmarketConfig.getConfigurationSection("rotation.daily-groups");
        if (dailySection != null) {
            for (String groupName : dailySection.getKeys(false)) {
                List<String> days = dailySection.getStringList(groupName);
                dailyGroups.put(groupName, days);
            }
        }

        // Muat grup rotasi mingguan
        ConfigurationSection weeklySection = blackmarketConfig.getConfigurationSection("rotation.weekly-groups");
        if (weeklySection != null) {
            for (String groupName : weeklySection.getKeys(false)) {
                List<Integer> weeks = weeklySection.getIntegerList(groupName);
                weeklyGroups.put(groupName, weeks);
            }
        }
    }
    
    /**
     * Memuat data stok dari file terpisah
     */
    private void loadStockData() {
        stockFile = new File(plugin.getDataFolder(), "blackmarket_stock.yml");
        if (!stockFile.exists()) {
            try {
                stockFile.createNewFile();
            } catch (IOException e) {
                plugin.getLogger().severe("Gagal membuat file blackmarket_stock.yml: " + e.getMessage());
                return;
            }
        }
        
        stockConfig = YamlConfiguration.loadConfiguration(stockFile);
        
        for (String itemId : items.keySet()) {
            BlackmarketItem item = items.get(itemId);
            if (item.isResetOnRestart()) {
                // Jika reset pada restart, gunakan stok maksimum
                item.setCurrentStock(item.getMaxStock());
            } else {
                // Jika tidak, muat dari config
                int stock = stockConfig.getInt("stock." + itemId, item.getMaxStock());
                item.setCurrentStock(stock);
            }
        }
    }
    
    /**
     * Menyimpan data stok ke file
     */
    public void saveStockData() {
        if (stockConfig == null) return;
        
        for (String itemId : items.keySet()) {
            BlackmarketItem item = items.get(itemId);
            if (!item.isResetOnRestart()) {
                stockConfig.set("stock." + itemId, item.getCurrentStock());
            }
        }
        
        try {
            stockConfig.save(stockFile);
        } catch (IOException e) {
            plugin.getLogger().severe("Gagal menyimpan file blackmarket_stock.yml: " + e.getMessage());
        }
    }
    
    /**
     * Setup jadwal buka dan tutup blackmarket
     */
    private void setupSchedules() {
        // Batalkan tugas sebelumnya
        if (openTask != null) {
            openTask.cancel();
            openTask = null;
        }
        
        if (closeTask != null) {
            closeTask.cancel();
            closeTask = null;
        }
        
        if (warningTask != null) {
            warningTask.cancel();
            warningTask = null;
        }
        
        LocalTime now = LocalTime.now();
        
        // Hitung waktu hingga pembukaan dan penutupan berikutnya
        long secondsUntilOpen = calculateSecondsUntil(now, openTime);
        long secondsUntilClose = calculateSecondsUntil(now, closeTime);
        
        // CRITICAL FIX: Enforce minimum delay to prevent rapid cycling
        final long MIN_DELAY_SECONDS = 30; // Minimal 30 detik antara operasi
        secondsUntilOpen = Math.max(MIN_DELAY_SECONDS, secondsUntilOpen);
        secondsUntilClose = Math.max(MIN_DELAY_SECONDS, secondsUntilClose);
        
        // Determine if blackmarket is currently open based on time range
        boolean shouldBeOpen = isTimeInRange(now);
        
        // Only set the state once - prevent changing based on force open
        if (!forcedOpen) {
            isCurrentlyOpen = shouldBeOpen;
        }
        
        // Only schedule what's needed based on current state
        if (!isCurrentlyOpen && !forcedOpen) {
            // Schedule opening
            plugin.getLogger().info("Scheduling blackmarket to open in " + secondsUntilOpen + " seconds");
            scheduleOpen(secondsUntilOpen);
            
            // Schedule VIP notification if needed
            long secondsUntilVipNotification = secondsUntilOpen - (vipPreOpenMinutes * 60);
            if (secondsUntilVipNotification > MIN_DELAY_SECONDS) {
                // Add VIP notification schedule here if needed
            }
        } else {
            // Schedule closing if market is open
            plugin.getLogger().info("Scheduling blackmarket to close in " + secondsUntilClose + " seconds");
            scheduleClose(secondsUntilClose);
            
            // Schedule warning notification
            long secondsUntilWarning = secondsUntilClose - (closingWarningTime * 60);
            if (secondsUntilWarning > MIN_DELAY_SECONDS) {
                scheduleWarning(secondsUntilWarning);
            }
        }
        
        plugin.getLogger().info("Blackmarket scheduling setup completed. " +
                "Currently " + (isCurrentlyOpen ? "OPEN" : "CLOSED"));
    }

    /**
     * Format waktu dalam detik menjadi format jam:menit:detik
     * @param seconds Waktu dalam detik
     * @return String format waktu
     */
    private String formatTime(long seconds) {
        long hours = seconds / 3600;
        long minutes = (seconds % 3600) / 60;
        long secs = seconds % 60;
        
        if (hours > 0) {
            return String.format("%d jam, %d menit, %d detik", hours, minutes, secs);
        } else if (minutes > 0) {
            return String.format("%d menit, %d detik", minutes, secs);
        } else {
            return String.format("%d detik", secs);
        }
    }
    
    /**
     * Menghitung berapa detik dari waktu saat ini hingga waktu target
     * @param from Waktu asal
     * @param to Waktu target
     * @return Jumlah detik hingga waktu target, atau detik hingga waktu target di hari berikutnya jika target sudah lewat
     */
    private long calculateSecondsUntil(LocalTime from, LocalTime to) {
        // CRITICAL FIX: Handle exact time matching
        if (from.equals(to)) {
            // If current time exactly matches target time, schedule for tomorrow instead (add 24 hours)
            return 24 * 60 * 60;
        }
        
        long secondsBetween = from.until(to, ChronoUnit.SECONDS);
        
        // Jika waktu target sudah lewat, hitung untuk hari berikutnya
        if (secondsBetween < 0) {
            secondsBetween += 24 * 60 * 60; // Tambahkan detik dalam 1 hari
        }
        
        return secondsBetween;
    }
    
    /**
     * Jadwalkan tugas untuk membuka blackmarket
     * @param secondsUntilOpen Detik hingga blackmarket buka
     */
    private void scheduleOpen(long secondsUntilOpen) {
        // CRITICAL FIX: Check if we already have the correct state to avoid redundant operations
        if (isCurrentlyOpen) {
            plugin.getLogger().info("Blackmarket already open, skipping open schedule");
            return;
        }
        
        // Make sure we have a valid delay
        if (secondsUntilOpen < 1) {
            secondsUntilOpen = 30; // Default to 30 seconds if calculation gives invalid result
            plugin.getLogger().warning("Invalid delay for blackmarket open, using default: " + secondsUntilOpen + "s");
        }
        
        openTask = new BukkitRunnable() {
            @Override
            public void run() {
                // Safety check - only proceed if market is not already open
                if (isCurrentlyOpen) {
                    plugin.getLogger().info("Blackmarket already open when task executed, ignoring");
                    return;
                }
                
                // Set state first to prevent race conditions
                isCurrentlyOpen = true;
                
                // Reset stok untuk item yang perlu direset saat buka
                for (BlackmarketItem item : items.values()) {
                    if (item.isResetOnRestart()) {
                        item.setCurrentStock(item.getMaxStock());
                    }
                }
                
                // Kirim pengumuman
                broadcastOpenMessage();
                
                // Jadwalkan penutupan dengan safety measures
                long secondsUntilClose;
                if (closeTime.isBefore(openTime)) {
                    // Tutup besok
                    secondsUntilClose = openTime.until(LocalTime.MIDNIGHT, ChronoUnit.SECONDS) + 
                                      LocalTime.MIDNIGHT.until(closeTime, ChronoUnit.SECONDS);
                } else {
                    // Tutup hari ini
                    secondsUntilClose = openTime.until(closeTime, ChronoUnit.SECONDS);
                }
                
                // CRITICAL FIX: Ensure minimum delay
                secondsUntilClose = Math.max(30, secondsUntilClose);
                
                // Jadwalkan penutupan
                scheduleClose(secondsUntilClose);
                
                // Jadwalkan peringatan sebelum tutup
                long secondsUntilWarning = secondsUntilClose - (closingWarningTime * 60);
                if (secondsUntilWarning > 0) {
                    scheduleWarning(secondsUntilWarning);
                }
                
                // Log
                plugin.getLogger().info("Blackmarket telah dibuka! Akan tutup dalam " + formatTime(secondsUntilClose));
            }
        }.runTaskLater(plugin, secondsUntilOpen * 20); // Konversi detik ke ticks
    }
    
    /**
     * Jadwalkan tugas untuk menutup blackmarket
     * @param secondsUntilClose Detik hingga blackmarket tutup
     */
    private void scheduleClose(long secondsUntilClose) {
        // CRITICAL FIX: Check if we already have the correct state to avoid redundant operations
        if (!isCurrentlyOpen && !forcedOpen) {
            plugin.getLogger().info("Blackmarket already closed, skipping close schedule");
            return;
        }
        
        // Make sure we have a valid delay
        if (secondsUntilClose < 1) {
            secondsUntilClose = 30; // Default to 30 seconds if calculation gives invalid result
            plugin.getLogger().warning("Invalid delay for blackmarket close, using default: " + secondsUntilClose + "s");
        }
        
        closeTask = new BukkitRunnable() {
            @Override
            public void run() {
                // Safety check - only proceed if market is open
                if (!isCurrentlyOpen && !forcedOpen) {
                    plugin.getLogger().info("Blackmarket already closed when task executed, ignoring");
                    return;
                }
                
                // Set state first to prevent race conditions
                isCurrentlyOpen = false;
                forcedOpen = false;
                
                // Kirim pengumuman
                broadcastCloseMessage();
                
                // Jadwalkan pembukaan berikutnya dengan safety measures
                long nextOpenDelay;
                LocalTime now = LocalTime.now();
                
                if (now.isBefore(openTime)) {
                    // Buka hari ini jika belum waktunya
                    nextOpenDelay = ChronoUnit.SECONDS.between(now, openTime);
                } else {
                    // Buka besok jika sudah lewat waktu hari ini
                    nextOpenDelay = ChronoUnit.SECONDS.between(now, openTime) + (24 * 60 * 60);
                }
                
                // CRITICAL FIX: Ensure minimum delay
                nextOpenDelay = Math.max(30, nextOpenDelay);
                
                // Schedule next opening
                scheduleOpen(nextOpenDelay);
                
                // Log
                plugin.getLogger().info("Blackmarket telah ditutup! Akan buka kembali dalam " + formatTime(nextOpenDelay));
            }
        }.runTaskLater(plugin, secondsUntilClose * 20); // Konversi detik ke ticks
    }
    
    /**
     * Jadwalkan tugas untuk memperingatkan sebelum blackmarket tutup
     * @param secondsUntilWarning Detik hingga peringatan
     */
    private void scheduleWarning(long secondsUntilWarning) {
        warningTask = new BukkitRunnable() {
            @Override
            public void run() {
                // Kirim peringatan
                broadcastWarningMessage();
                
                // Log
                plugin.getLogger().info("Peringatan: Blackmarket akan tutup dalam " + closingWarningTime + " menit!");
            }
        }.runTaskLater(plugin, secondsUntilWarning * 20); // Konversi detik ke ticks
    }
    
    /**
     * Jadwalkan notifikasi untuk pemain VIP
     * @param secondsUntilVipNotification Detik hingga notifikasi VIP
     */
    private void scheduleVipNotification(long secondsUntilVipNotification) {
        if (secondsUntilVipNotification <= 0) {
            return;
        }
        
        plugin.getLogger().info("Scheduling VIP notification in " + formatTime(secondsUntilVipNotification));
        
        BukkitTask vipTask = new BukkitRunnable() {
            @Override
            public void run() {
                sendVipNotification();
            }
        }.runTaskLater(plugin, secondsUntilVipNotification * 20L);
    }
    
    /**
     * Kirim notifikasi khusus untuk pemain VIP
     */
    private void sendVipNotification() {
        String message = vipPreOpenMessage.replace("{minutes}", String.valueOf(vipPreOpenMinutes));
        
        for (Player player : Bukkit.getOnlinePlayers()) {
            if (player.hasPermission("nusashop.blackmarket.vip")) {
                player.sendMessage(message);
                player.playSound(player.getLocation(), Sound.BLOCK_NOTE_BLOCK_CHIME, 1.0f, 1.0f);
            }
        }
        
        plugin.getLogger().info("Sent VIP notification for Blackmarket pre-opening");
    }
    
    /**
     * Kirim pesan pembukaan ke semua player dengan titel dan efek suara
     */
    public void broadcastOpenMessage() {
        // Kirim pesan chat
        String formattedMessage = ChatColor.translateAlternateColorCodes('&', openMessage);
        Bukkit.broadcastMessage(formattedMessage);
        
        // Tampilkan BossBar
        blackmarketBar.setVisible(true);
        for (Player player : Bukkit.getOnlinePlayers()) {
            blackmarketBar.addPlayer(player);
        }
        
        // Schedule BossBar removal after 2 minutes
        new BukkitRunnable() {
            @Override
            public void run() {
                blackmarketBar.setVisible(false);
                blackmarketBar.removeAll();
            }
        }.runTaskLater(plugin, 2400L); // 2 minutes = 2400 ticks
        
        // Kirim title dan sound effects untuk semua player
        for (Player player : Bukkit.getOnlinePlayers()) {
            // Title animation
            player.sendTitle(
                ChatColor.RED + "⚠ BLACKMARKET " + ChatColor.GREEN + "BUKA ⚠",
                ChatColor.GOLD + "Ketik " + ChatColor.WHITE + "/blackmarket" + ChatColor.GOLD + " untuk mengakses",
                10, 70, 20
            );
            
            // Play sound
            player.playSound(player.getLocation(), Sound.ENTITY_EXPERIENCE_ORB_PICKUP, 1.0f, 0.5f);
            
            // Schedule a delayed second sound for better effect
            new BukkitRunnable() {
                @Override
                public void run() {
                    player.playSound(player.getLocation(), Sound.BLOCK_BELL_USE, 0.7f, 1.0f);
                }
            }.runTaskLater(plugin, 10L);
        }
        
        plugin.getLogger().info("Blackmarket opened and notifications sent");
    }
    
    /**
     * Kirim pesan penutupan ke semua player dengan titel dan efek suara
     */
    public void broadcastCloseMessage() {
        // Kirim pesan chat
        String formattedMessage = ChatColor.translateAlternateColorCodes('&', closeMessage);
        Bukkit.broadcastMessage(formattedMessage);
        
        // Hide BossBar if visible
        if (blackmarketBar.isVisible()) {
            blackmarketBar.setVisible(false);
            blackmarketBar.removeAll();
        }
        
        // Kirim title dan sound effects untuk semua player
        for (Player player : Bukkit.getOnlinePlayers()) {
            player.sendTitle(
                ChatColor.RED + "⚠ BLACKMARKET TUTUP ⚠",
                ChatColor.GRAY + "Datang kembali besok",
                10, 70, 20
            );
            
            // Play closed sound
            player.playSound(player.getLocation(), Sound.BLOCK_CHEST_CLOSE, 0.7f, 1.0f);
        }
        
        plugin.getLogger().info("Blackmarket closed and notifications sent");
    }
    
    /**
     * Kirim pesan peringatan sebelum tutup ke semua player
     */
    private void broadcastWarningMessage() {
        String formattedMessage = ChatColor.DARK_GRAY + "[" + ChatColor.RED + "Blackmarket" + ChatColor.DARK_GRAY + "] " + 
                              ChatColor.YELLOW + "Perhatian! Pasar Gelap akan tutup dalam " + 
                              ChatColor.RED + closingWarningTime + ChatColor.YELLOW + " menit!";
        Bukkit.broadcastMessage(formattedMessage);
        
        // Tampilkan countdown di BossBar
        blackmarketBar.setTitle(ChatColor.RED + "⚠ Pasar Gelap: " + ChatColor.GOLD + "Tutup dalam " + closingWarningTime + " menit");
        blackmarketBar.setColor(BarColor.YELLOW);
        blackmarketBar.setVisible(true);
        
        for (Player player : Bukkit.getOnlinePlayers()) {
            // Add player to BossBar if not already added
            if (!blackmarketBar.getPlayers().contains(player)) {
                blackmarketBar.addPlayer(player);
            }
            
            // Play warning sound
            player.playSound(player.getLocation(), Sound.BLOCK_NOTE_BLOCK_BASS, 1.0f, 0.5f);
        }
        
        plugin.getLogger().info("Blackmarket closing warning sent");
    }
    
    /**
     * Metode untuk membersihkan BossBar saat plugin dimatikan
     */
    public void cleanup() {
        if (blackmarketBar != null) {
            blackmarketBar.setVisible(false);
            blackmarketBar.removeAll();
        }
    }
    
    /**
     * Batalkan semua tugas terjadwal
     */
    private void cancelTasks() {
        if (openTask != null && !openTask.isCancelled()) {
            openTask.cancel();
            openTask = null;
        }
        
        if (closeTask != null && !closeTask.isCancelled()) {
            closeTask.cancel();
            closeTask = null;
        }
        
        if (warningTask != null && !warningTask.isCancelled()) {
            warningTask.cancel();
            warningTask = null;
        }
    }
    
    /**
     * Memeriksa apakah waktu saat ini berada dalam rentang buka
     * @param time Waktu yang diperiksa
     * @return true jika waktu dalam rentang buka
     */
    private boolean isTimeInRange(LocalTime time) {
        // Jika blackmarket dipaksa buka melalui admin, selalu kembalikan true
        if (forcedOpen) {
            return true;
        }
        
        if (closeTime.isBefore(openTime)) {
            // Kasus dimana blackmarket buka sampai lewat tengah malam
            // Contoh: buka 20:00, tutup 02:00
            return time.equals(openTime) || time.isAfter(openTime) || 
                   time.isBefore(closeTime) || time.equals(closeTime);
        } else {
            // Kasus normal dimana blackmarket buka dan tutup di hari yang sama
            // Contoh: buka 10:00, tutup 22:00
            return (time.equals(openTime) || time.isAfter(openTime)) && 
                   (time.isBefore(closeTime));
        }
    }

    /**
     * Memeriksa apakah blackmarket sedang buka
     * @return true jika blackmarket buka
     */
    public boolean isOpen() {
        return isCurrentlyOpen || forcedOpen;
    }
    
    /**
     * Memaksa blackmarket buka melalui admin command
     * @param open true untuk memaksa buka, false untuk kembali ke jadwal normal
     */
    public void setForcedOpen(boolean open) {
        this.forcedOpen = open;
    }
    
    /**
     * Mengecek apakah player memiliki izin untuk membeli item tertentu
     * @param player Player yang akan dicek
     * @param item Item yang akan dibeli
     * @return true jika player memiliki izin
     */
    public boolean hasPermissionFor(Player player, BlackmarketItem item) {
        return item.getPermission() == null || player.hasPermission(item.getPermission());
    }
    
    /**
     * Mendapatkan waktu buka dalam format yang mudah dibaca
     * @return String waktu buka (format HH:mm)
     */
    public String getOpenTimeFormatted() {
        return openTime.format(TIME_FORMATTER);
    }
    
    /**
     * Mendapatkan waktu tutup dalam format yang mudah dibaca
     * @return String waktu tutup (format HH:mm)
     */
    public String getCloseTimeFormatted() {
        if (closeTime.equals(LocalTime.MIDNIGHT)) {
            return "24:00";
        }
        return closeTime.format(TIME_FORMATTER);
    }
    
    /**
     * Reload blackmarket data dan jadwal
     */
    public void reload() {
        // Batalkan jadwal yang ada
        cancelTasks();
        
        // Simpan data stok sebelum reload
        saveStockData();
        
        // Muat ulang konfigurasi dan stok
        loadConfig();
        loadStockData();
        
        // Setup ulang jadwal
        setupSchedules();
    }
    
    /**
     * Metode cleanup saat plugin dinonaktifkan
     */
    public void shutdown() {
        // Batalkan semua tugas
        cancelTasks();
        
        // Simpan data stok
        saveStockData();
        
        // Bersihkan BossBar
        cleanup();
    }
    
    /**
     * Memeriksa apakah item tersedia hari ini berdasarkan rotasi
     * @param item Item yang diperiksa
     * @return true jika item tersedia hari ini
     */
    public boolean isItemAvailableToday(BlackmarketItem item) {
        if (!rotationEnabled || !item.isRotatingItem()) {
            return true; // Item non-rotasi atau rotasi dinonaktifkan
        }
        
        Calendar cal = Calendar.getInstance();
        
        // Rotasi harian berdasarkan hari dalam seminggu
        if (item.getRotationType().equals("daily")) {
            String dayOfWeek = getDayOfWeek(cal.get(Calendar.DAY_OF_WEEK));
            String group = item.getRotationGroup();
            
            // Jika grup tidak dikonfigurasi, selalu tersedia
            if (!dailyGroups.containsKey(group)) {
                return true;
            }
            
            // Cek apakah hari ini termasuk dalam grup rotasi
            return dailyGroups.get(group).contains(dayOfWeek);
        }
        
        // Rotasi mingguan berdasarkan minggu dalam sebulan
        if (item.getRotationType().equals("weekly")) {
            int weekOfMonth = cal.get(Calendar.WEEK_OF_MONTH);
            int week = item.getRotationWeek();
            
            return weekOfMonth == week;
        }
        
        // Default: tersedia
        return true;
    }


    /**
     * Mendapatkan semua item yang tersedia hari ini
     * @return List item yang tersedia berdasarkan rotasi
     */
    public List<BlackmarketItem> getAvailableItems() {
        if (!rotationEnabled) {
            return new ArrayList<>(items.values()); // Semua item tersedia jika rotasi dinonaktifkan
        }
        
        List<BlackmarketItem> availableItems = new ArrayList<>();
        
        for (BlackmarketItem item : items.values()) {
            if (isItemAvailableToday(item)) {
                availableItems.add(item);
            }
        }
        
        return availableItems;
    }

    /**
     * Mendapatkan daftar hari untuk suatu grup rotasi
     * @param group Nama grup rotasi
     * @return List hari dalam grup
     */
    public List<String> getRotationDays(String group) {
        return dailyGroups.getOrDefault(group, new ArrayList<>());
    }

    /**
     * Mendapatkan daftar minggu untuk suatu grup rotasi
     * @param group Nama grup rotasi
     * @return List minggu dalam grup
     */
    public List<Integer> getRotationWeeks(String group) {
        return weeklyGroups.getOrDefault(group, new ArrayList<>());
    }

    /**
     * Cek apakah sistem rotasi aktif
     * @return true jika rotasi aktif
     */
    public boolean isRotationEnabled() {
        return rotationEnabled;
    }

    /**
     * Mendapatkan jadwal rotasi saat ini
     * @return daily atau weekly
     */
    public String getRotationSchedule() {
        return rotationSchedule;
    }

    /**
     * Mendapatkan nama hari dari konstanta Calendar (metode publik)
     * @param dayOfWeek Konstanta hari dari Calendar
     * @return Nama hari dalam bahasa Inggris
     */
    public String getDayOfWeek(int dayOfWeek) {
        // Implementasi yang sama dengan metode private
        switch (dayOfWeek) {
            case Calendar.MONDAY: return "Monday";
            case Calendar.TUESDAY: return "Tuesday";
            case Calendar.WEDNESDAY: return "Wednesday";
            case Calendar.THURSDAY: return "Thursday";
            case Calendar.FRIDAY: return "Friday";
            case Calendar.SATURDAY: return "Saturday";
            case Calendar.SUNDAY: return "Sunday";
            default: return "Unknown";
        }
    }

    /**
     * Mendapatkan hari ini dalam bentuk string
     * @return Nama hari ini
     */
    public String getCurrentDay() {
        Calendar cal = Calendar.getInstance();
        return getDayOfWeek(cal.get(Calendar.DAY_OF_WEEK));
    }

    /**
     * Mendapatkan minggu saat ini dalam bulan
     * @return Nomor minggu (1-5)
     */
    public int getCurrentWeek() {
        Calendar cal = Calendar.getInstance();
        return cal.get(Calendar.WEEK_OF_MONTH);
    }

    /**
     * Mendapatkan bossbar blackmarket
     * @return BossBar instance
     */
    public BossBar getBlackmarketBar() {
        return blackmarketBar;
    }

    /**
     * Cek apakah sekarang adalah waktu pre-opening untuk VIP
     * @return true jika sekarang adalah waktu VIP pre-opening
     */
    public boolean isInVipPreOpeningTime() {
        if (isCurrentlyOpen || forcedOpen) {
            return false; // Sudah buka untuk semua orang
        }
        
        LocalTime now = LocalTime.now();
        
        // Hitung waktu hingga pembukaan berikutnya
        long secondsUntilOpen = calculateSecondsUntil(now, openTime);
        
        // Jika waktu hingga buka kurang dari periode pre-open VIP, 
        // maka sedang dalam waktu pre-opening
        return secondsUntilOpen <= (vipPreOpenMinutes * 60) && secondsUntilOpen > 0;
    }

    // Getters
    public List<BlackmarketItem> getAllItems() {
        return new ArrayList<>(items.values());
    }
    
    public BlackmarketItem getItem(String id) {
        return items.get(id);
    }
    
    public double getEntryFee() {
        return entryFee;
    }
    
    public String getGuiTitle() {
        return guiTitle;
    }
    
    public int getGuiRows() {
        return guiRows;
    }
    
    public String getBackgroundMaterial() {
        return backgroundMaterial;
    }
    
    public String getBorderMaterial() {
        return borderMaterial;
    }
}