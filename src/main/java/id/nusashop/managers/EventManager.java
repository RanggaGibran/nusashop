package id.nusashop.managers;

import id.nusashop.NusaShop;
import id.nusashop.models.Category;
import id.nusashop.models.ShopItem;
import id.nusashop.utils.Messages;

import org.bukkit.Bukkit;
import org.bukkit.ChatColor;
import org.bukkit.configuration.ConfigurationSection;
import org.bukkit.entity.Player;
import org.bukkit.scheduler.BukkitRunnable;
import org.bukkit.scheduler.BukkitTask;

import java.time.LocalDateTime;
import java.time.ZoneId;
import java.time.format.DateTimeFormatter;
import java.time.format.DateTimeParseException;
import java.time.temporal.ChronoUnit;
import java.util.ArrayList;
import java.util.Date;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.concurrent.TimeUnit;

/**
 * Manager untuk mengelola event spesial shop
 */
public class EventManager {
    private final NusaShop plugin;
    
    // Data event aktif
    private boolean hasActiveEvent = false;
    private String eventName = "";
    private String eventDescription = "";
    private LocalDateTime startTime;
    private LocalDateTime endTime;
    private double sellMultiplier = 1.0;
    private double buyMultiplier = 1.0;
    
    // Multiplier khusus per kategori dan item
    private final Map<String, Double> categorySellMultipliers = new HashMap<>();
    private final Map<String, Double> categoryBuyMultipliers = new HashMap<>();
    private final Map<String, Double> itemSellMultipliers = new HashMap<>();
    private final Map<String, Double> itemBuyMultipliers = new HashMap<>();
    
    // Task untuk pengumuman berkala
    private BukkitTask announcementTask;
    
    // Task untuk pengecekan event
    private BukkitTask eventCheckTask;
    private String lastEventName = "";
    
    // Formatter untuk waktu
    private static final DateTimeFormatter DATE_FORMATTER = 
            DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm:ss");
    
    private static final DateTimeFormatter DISPLAY_FORMATTER = 
            DateTimeFormatter.ofPattern("dd MMMM yyyy, HH:mm");
    
    public EventManager(NusaShop plugin) {
        this.plugin = plugin;
        loadEvents();
        startEventCheckTask();
    }
    
    /**
     * Memuat event dari konfigurasi
     */
    public void loadEvents() {
        // Reset data
        hasActiveEvent = false;
        sellMultiplier = 1.0;
        buyMultiplier = 1.0;
        categorySellMultipliers.clear();
        categoryBuyMultipliers.clear();
        itemSellMultipliers.clear();
        itemBuyMultipliers.clear();
        
        // Batalkan task pengumuman jika ada
        if (announcementTask != null) {
            announcementTask.cancel();
            announcementTask = null;
        }
        
        // Cek apakah sistem event diaktifkan
        if (!plugin.getConfigManager().getConfig().getBoolean("events.enabled", true)) {
            return;
        }
        
        // Cek event aktif
        ConfigurationSection currentEventConfig = 
                plugin.getConfigManager().getConfig().getConfigurationSection("events.current-event");
        
        if (currentEventConfig != null) {
            try {
                startTime = LocalDateTime.parse(
                        currentEventConfig.getString("start-time", "2025-01-01 00:00:00"), 
                        DATE_FORMATTER);
                
                endTime = LocalDateTime.parse(
                        currentEventConfig.getString("end-time", "2025-01-02 00:00:00"), 
                        DATE_FORMATTER);
                
                LocalDateTime now = LocalDateTime.now();
                
                if (now.isAfter(startTime) && now.isBefore(endTime)) {
                    // Event sedang aktif
                    hasActiveEvent = true;
                    eventName = ChatColor.translateAlternateColorCodes('&', 
                            currentEventConfig.getString("name", "&cEvent Spesial"));
                    
                    eventDescription = ChatColor.translateAlternateColorCodes('&', 
                            currentEventConfig.getString("description", "&eDeskripsi event"));
                    
                    sellMultiplier = currentEventConfig.getDouble("sell-multiplier", 1.0);
                    buyMultiplier = currentEventConfig.getDouble("buy-multiplier", 1.0);
                    
                    // Load multiplier per kategori
                    ConfigurationSection categorySection = 
                            currentEventConfig.getConfigurationSection("category-multipliers");
                    
                    if (categorySection != null) {
                        for (String categoryId : categorySection.getKeys(false)) {
                            double multiplier = categorySection.getDouble(categoryId, 1.0);
                            categorySellMultipliers.put(categoryId, multiplier);
                        }
                    }
                    
                    // Load multiplier per item
                    ConfigurationSection itemSection = 
                            currentEventConfig.getConfigurationSection("item-multipliers");
                    
                    if (itemSection != null) {
                        for (String itemId : itemSection.getKeys(false)) {
                            double multiplier = itemSection.getDouble(itemId, 1.0);
                            itemSellMultipliers.put(itemId, multiplier);
                        }
                    }
                    
                    plugin.getLogger().info("Event aktif: " + eventName);
                    plugin.getLogger().info("Berlangsung hingga: " + endTime.format(DISPLAY_FORMATTER));
                    
                    // Only send Discord notification if this is a new event
                    if (!eventName.equals(lastEventName)) {
                        lastEventName = eventName;
                        plugin.getWebhookManager().sendEventNotification(
                            ChatColor.stripColor(eventName),
                            ChatColor.stripColor(eventDescription),
                            startTime,
                            endTime,
                            sellMultiplier,
                            categorySellMultipliers
                        );
                    }
                }
            } catch (DateTimeParseException e) {
                plugin.getLogger().warning("Format tanggal event salah: " + e.getMessage());
            }
        }
        
        // Cek upcoming events jika tidak ada event aktif
        if (!hasActiveEvent) {
            ConfigurationSection upcomingEvents = 
                    plugin.getConfigManager().getConfig().getConfigurationSection("events.upcoming-events");
            
            if (upcomingEvents != null) {
                for (String key : upcomingEvents.getKeys(false)) {
                    ConfigurationSection eventConfig = upcomingEvents.getConfigurationSection(key);
                    if (eventConfig != null) {
                        try {
                            LocalDateTime eventStart = LocalDateTime.parse(
                                    eventConfig.getString("start-time"), DATE_FORMATTER);
                            LocalDateTime eventEnd = LocalDateTime.parse(
                                    eventConfig.getString("end-time"), DATE_FORMATTER);
                            
                            LocalDateTime now = LocalDateTime.now();
                            
                            if (now.isAfter(eventStart) && now.isBefore(eventEnd)) {
                                // Event ini sudah aktif
                                hasActiveEvent = true;
                                eventName = ChatColor.translateAlternateColorCodes('&', 
                                        eventConfig.getString("name", "&cEvent Spesial"));
                                
                                eventDescription = ChatColor.translateAlternateColorCodes('&', 
                                        eventConfig.getString("description", "&eDeskripsi event"));
                                
                                sellMultiplier = eventConfig.getDouble("sell-multiplier", 1.0);
                                buyMultiplier = eventConfig.getDouble("buy-multiplier", 1.0);
                                
                                // Load multiplier per kategori dan item jika ada
                                // ...
                                
                                plugin.getLogger().info("Event aktif dari jadwal upcoming: " + eventName);
                                break;
                            }
                        } catch (DateTimeParseException e) {
                            plugin.getLogger().warning("Format tanggal event salah: " + e.getMessage());
                        }
                    }
                }
            }
        }

        scheduleAnnouncements();
    }
    
    /**
     * Menjadwalkan pengumuman berkala tentang event aktif
     */
    private void scheduleAnnouncements() {
        if (!hasActiveEvent) return;
        
        int interval = plugin.getConfigManager().getConfig().getInt("events.current-event.announcement-interval", 30);
        
        announcementTask = new BukkitRunnable() {
            @Override
            public void run() {
                if (!hasActiveEvent) {
                    this.cancel();
                    return;
                }
                
                // Hitung sisa waktu
                LocalDateTime now = LocalDateTime.now();
                long hoursRemaining = now.until(endTime, ChronoUnit.HOURS);
                
                // Broadcast event info
                Bukkit.broadcastMessage("");
                Bukkit.broadcastMessage(ChatColor.GOLD + "▬▬▬▬▬▬▬▬▬▬▬▬▬▬▬▬▬▬▬▬▬▬▬▬▬▬▬▬▬▬▬▬▬▬▬▬▬▬▬▬▬▬▬▬▬▬▬");
                Bukkit.broadcastMessage(ChatColor.YELLOW + "      " + eventName);
                Bukkit.broadcastMessage(ChatColor.YELLOW + "      " + eventDescription);
                Bukkit.broadcastMessage(ChatColor.YELLOW + "      Berakhir dalam: " + 
                        ChatColor.WHITE + hoursRemaining + " jam");
                Bukkit.broadcastMessage(ChatColor.YELLOW + "      Ketik " + 
                        ChatColor.GREEN + "/shop" + ChatColor.YELLOW + " untuk belanja sekarang!");
                Bukkit.broadcastMessage(ChatColor.GOLD + "▬▬▬▬▬▬▬▬▬▬▬▬▬▬▬▬▬▬▬▬▬▬▬▬▬▬▬▬▬▬▬▬▬▬▬▬▬▬▬▬▬▬▬▬▬▬▬");
                Bukkit.broadcastMessage("");
            }
        }.runTaskTimer(plugin, 20 * 60, 20 * 60 * interval); // Interval dalam menit
    }
    
    /**
     * Start a task to periodically check for event changes
     */
    public void startEventCheckTask() {
        // Cancel any existing task
        if (eventCheckTask != null) {
            eventCheckTask.cancel();
        }
        
        // Check for event changes every minute
        eventCheckTask = new BukkitRunnable() {
            @Override
            public void run() {
                checkEventChanges();
            }
        }.runTaskTimer(plugin, 20 * 60, 20 * 60); // Check every minute
    }
    
    /**
     * Check if event status has changed and handle accordingly
     */
    private void checkEventChanges() {
        LocalDateTime now = LocalDateTime.now();
        boolean wasActive = hasActiveEvent;
        String previousEventName = eventName;
        
        // Check if the current active event has ended
        if (hasActiveEvent && now.isAfter(endTime)) {
            hasActiveEvent = false;
            plugin.getLogger().info("Event ended: " + eventName);
            
            // Reset multipliers
            sellMultiplier = 1.0;
            buyMultiplier = 1.0;
            categorySellMultipliers.clear();
            categoryBuyMultipliers.clear();
            itemSellMultipliers.clear();
            itemBuyMultipliers.clear();
            
            // Cancel announcement task
            if (announcementTask != null) {
                announcementTask.cancel();
                announcementTask = null;
            }
        }
        
        // If no active event, check upcoming events
        if (!hasActiveEvent) {
            ConfigurationSection upcomingEvents = 
                    plugin.getConfigManager().getConfig().getConfigurationSection("events.upcoming-events");
            
            if (upcomingEvents != null) {
                for (String key : upcomingEvents.getKeys(false)) {
                    ConfigurationSection eventConfig = upcomingEvents.getConfigurationSection(key);
                    if (eventConfig != null) {
                        try {
                            LocalDateTime eventStart = LocalDateTime.parse(
                                    eventConfig.getString("start-time"), DATE_FORMATTER);
                            LocalDateTime eventEnd = LocalDateTime.parse(
                                    eventConfig.getString("end-time"), DATE_FORMATTER);
                            
                            // Check if this event should be active now
                            if (now.isAfter(eventStart) && now.isBefore(eventEnd)) {
                                // Event should be active
                                hasActiveEvent = true;
                                eventName = ChatColor.translateAlternateColorCodes('&', 
                                        eventConfig.getString("name", "&cEvent Spesial"));
                                
                                eventDescription = ChatColor.translateAlternateColorCodes('&', 
                                        eventConfig.getString("description", "&eDeskripsi event"));
                                
                                sellMultiplier = eventConfig.getDouble("sell-multiplier", 1.0);
                                buyMultiplier = eventConfig.getDouble("buy-multiplier", 1.0);
                                startTime = eventStart;
                                endTime = eventEnd;
                                
                                // Load category multipliers
                                ConfigurationSection categorySection = 
                                        eventConfig.getConfigurationSection("category-multipliers");
                                
                                if (categorySection != null) {
                                    for (String categoryId : categorySection.getKeys(false)) {
                                        double multiplier = categorySection.getDouble(categoryId, 1.0);
                                        categorySellMultipliers.put(categoryId, multiplier);
                                    }
                                }
                                
                                plugin.getLogger().info("New event started: " + eventName);
                                break; // Stop after finding the first active event
                            }
                        } catch (DateTimeParseException e) {
                            plugin.getLogger().warning("Format tanggal event salah: " + e.getMessage());
                        }
                    }
                }
            }
        }
        
        // Handle event status changes
        if (!previousEventName.equals(eventName)) {
            // Track this as the last event name to prevent duplicate webhooks
            lastEventName = eventName;
            
            if (hasActiveEvent) {
                // Send Discord notification for new event
                plugin.getWebhookManager().sendEventNotification(
                    ChatColor.stripColor(eventName),
                    ChatColor.stripColor(eventDescription),
                    startTime,
                    endTime,
                    sellMultiplier,
                    categorySellMultipliers
                );
                
                // Schedule announcements
                scheduleAnnouncements();
                
                // Announce event start
                Bukkit.broadcastMessage("");
                Bukkit.broadcastMessage(ChatColor.GOLD + "▬▬▬▬▬▬▬▬▬▬▬▬▬▬▬▬▬▬▬▬▬▬▬▬▬▬▬▬▬▬▬▬▬▬▬▬▬▬▬▬▬▬▬▬▬▬▬");
                Bukkit.broadcastMessage(ChatColor.GREEN + "" + ChatColor.BOLD + "EVENT DIMULAI: " + eventName);
                Bukkit.broadcastMessage(ChatColor.YELLOW + eventDescription);
                Bukkit.broadcastMessage(ChatColor.WHITE + "Berakhir: " + endTime.format(DISPLAY_FORMATTER));
                Bukkit.broadcastMessage(ChatColor.GOLD + "▬▬▬▬▬▬▬▬▬▬▬▬▬▬▬▬▬▬▬▬▬▬▬▬▬▬▬▬▬▬▬▬▬▬▬▬▬▬▬▬▬▬▬▬▬▬▬");
                Bukkit.broadcastMessage("");
            } else if (!previousEventName.isEmpty()) {
                // Announce event end
                Bukkit.broadcastMessage("");
                Bukkit.broadcastMessage(ChatColor.GOLD + "▬▬▬▬▬▬▬▬▬▬▬▬▬▬▬▬▬▬▬▬▬▬▬▬▬▬▬▬▬▬▬▬▬▬▬▬▬▬▬▬▬▬▬▬▬▬▬");
                Bukkit.broadcastMessage(ChatColor.RED + "" + ChatColor.BOLD + "EVENT BERAKHIR: " + previousEventName);
                Bukkit.broadcastMessage(ChatColor.YELLOW + "Semua harga telah kembali normal.");
                Bukkit.broadcastMessage(ChatColor.GOLD + "▬▬▬▬▬▬▬▬▬▬▬▬▬▬▬▬▬▬▬▬▬▬▬▬▬▬▬▬▬▬▬▬▬▬▬▬▬▬▬▬▬▬▬▬▬▬▬");
                Bukkit.broadcastMessage("");
            }
        }
    }
    
    /**
     * Mendapatkan multiplier harga jual berdasarkan item dan kategori
     * @param item Item yang dijual
     * @param category Kategori dari item
     * @return Multiplier untuk harga jual (1.0 = normal)
     */
    public double getSellPriceMultiplier(ShopItem item, Category category) {
        if (!hasActiveEvent) {
            return 1.0;
        }
        
        // Cek multiplier per item (prioritas tertinggi)
        if (itemSellMultipliers.containsKey(item.getId())) {
            return itemSellMultipliers.get(item.getId());
        }
        
        // Cek multiplier per kategori
        if (category != null && categorySellMultipliers.containsKey(category.getId())) {
            return categorySellMultipliers.get(category.getId());
        }
        
        // Gunakan multiplier default event
        return sellMultiplier;
    }
    
    /**
     * Mendapatkan multiplier harga beli berdasarkan item dan kategori
     * @param item Item yang dibeli
     * @param category Kategori dari item
     * @return Multiplier untuk harga beli (1.0 = normal)
     */
    public double getBuyPriceMultiplier(ShopItem item, Category category) {
        if (!hasActiveEvent) return 1.0;
        
        // Cek multiplier per item (prioritas tertinggi)
        if (itemBuyMultipliers.containsKey(item.getId())) {
            return itemBuyMultipliers.get(item.getId());
        }
        
        // Cek multiplier per kategori
        if (categoryBuyMultipliers.containsKey(category.getId())) {
            return categoryBuyMultipliers.get(category.getId());
        }
        
        // Gunakan multiplier default event
        return buyMultiplier;
    }
    
    /**
     * Mendapatkan multiplier harga jual global
     * @return Multiplier harga jual default
     */
    public double getSellMultiplier() {
        return sellMultiplier;
    }

    /**
     * Mendapatkan map multiplier harga jual per kategori
     * @return Map kategori dan multiplier
     */
    public Map<String, Double> getCategorySellMultipliers() {
        return new HashMap<>(categorySellMultipliers);
    }

    /**
     * Mendapatkan map multiplier harga jual per item
     * @return Map item dan multiplier
     */
    public Map<String, Double> getItemSellMultipliers() {
        return new HashMap<>(itemSellMultipliers);
    }
    
    /**
     * Cek apakah ada event aktif
     * @return true jika ada event aktif
     */
    public boolean hasActiveEvent() {
        return hasActiveEvent;
    }
    
    /**
     * Mendapatkan nama event aktif
     * @return nama event, atau string kosong jika tidak ada event aktif
     */
    public String getEventName() {
        return eventName;
    }
    
    /**
     * Mendapatkan deskripsi event aktif
     * @return deskripsi event, atau string kosong jika tidak ada event aktif
     */
    public String getEventDescription() {
        return eventDescription;
    }
    
    /**
     * Mendapatkan waktu mulai event
     * @return waktu mulai event
     */
    public LocalDateTime getStartTime() {
        return startTime;
    }
    
    /**
     * Mendapatkan waktu berakhir event
     * @return waktu berakhir event
     */
    public LocalDateTime getEndTime() {
        return endTime;
    }
    
    /**
     * Mendapatkan waktu berakhir event dalam format yang user friendly
     * @return String waktu berakhir event
     */
    public String getFormattedEndTime() {
        return endTime != null ? endTime.format(DISPLAY_FORMATTER) : "";
    }
    
    /**
     * Mendapatkan sisa waktu event dalam format yang user friendly
     * @return String sisa waktu event
     */
    public String getRemainingTimeFormatted() {
        long millisLeft = endTime.atZone(ZoneId.systemDefault()).toInstant().toEpochMilli() - System.currentTimeMillis();
        if (millisLeft <= 0) {
            // Event sudah selesai, matikan event & return string yang tepat
            this.hasActiveEvent = false;
            return "Berakhir";
        }
        long seconds = millisLeft / 1000;
        long hours = seconds / 3600;
        long minutes = (seconds % 3600) / 60;
        long secs = seconds % 60;
        if (hours > 0) return hours + " Jam " + minutes + " Menit";
        if (minutes > 0) return minutes + " Menit";
        return secs + " Detik";
    }
}