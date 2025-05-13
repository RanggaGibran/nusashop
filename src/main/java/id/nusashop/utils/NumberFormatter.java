package id.nusashop.utils;

/**
 * Utility class untuk format angka
 */
public class NumberFormatter {
    
    /**
     * Format angka menjadi format ringkas (1K, 10K, 1jt, dst)
     * @param number Angka yang akan diformat
     * @return String hasil format
     */
    public static String formatCompact(double number) {
        if (number < 1000) {
            return String.format("%.1f", number).replace(".0", "");
        }
        
        if (number < 10000) { // 1K-9.9K
            return String.format("%.1fK", number / 1000).replace(".0K", "K");
        }
        
        if (number < 1000000) { // 10K-999K
            return String.format("%dK", (int)(number / 1000));
        }
        
        if (number < 10000000) { // 1jt-9.9jt
            return String.format("%.1fjt", number / 1000000).replace(".0jt", "jt");
        }
        
        if (number < 1000000000) { // 10jt-999jt
            return String.format("%djt", (int)(number / 1000000));
        }
        
        if (number < 10000000000L) { // 1M-9.9M
            return String.format("%.1fM", number / 1000000000).replace(".0M", "M");
        }
        
        return String.format("%dM", (int)(number / 1000000000)); // 10M+
    }
    
    /**
     * Format harga menjadi format ringkas dengan simbol mata uang
     * @param price Harga yang akan diformat
     * @param currencySymbol Simbol mata uang
     * @return String hasil format
     */
    public static String formatCompactPrice(double price, String currencySymbol) {
        return currencySymbol + formatCompact(price);
    }
}
