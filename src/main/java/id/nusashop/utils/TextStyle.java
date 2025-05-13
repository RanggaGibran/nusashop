package id.nusashop.utils;

/**
 * Utilitas untuk styling text dalam GUI
 */
public class TextStyle {
    
    /**
     * Mengonversi teks biasa ke small caps
     * 
     * @param text Teks yang akan dikonversi
     * @return Teks dalam format small caps
     */
    public static String toSmallCaps(String text) {
        if (text == null) return "";
        
        StringBuilder result = new StringBuilder();
        for (char c : text.toCharArray()) {
            if (Character.isLetter(c) && Character.isUpperCase(c)) {
                result.append(c);  // Keep uppercase as is
            } else if (Character.isLetter(c)) {
                // Convert lowercase to small caps
                switch (c) {
                    case 'a': result.append('ᴀ'); break;
                    case 'b': result.append('ʙ'); break;
                    case 'c': result.append('ᴄ'); break;
                    case 'd': result.append('ᴅ'); break;
                    case 'e': result.append('ᴇ'); break;
                    case 'f': result.append('ғ'); break;
                    case 'g': result.append('ɢ'); break;
                    case 'h': result.append('ʜ'); break;
                    case 'i': result.append('ɪ'); break;
                    case 'j': result.append('ᴊ'); break;
                    case 'k': result.append('ᴋ'); break;
                    case 'l': result.append('ʟ'); break;
                    case 'm': result.append('ᴍ'); break;
                    case 'n': result.append('ɴ'); break;
                    case 'o': result.append('ᴏ'); break;
                    case 'p': result.append('ᴘ'); break;
                    case 'q': result.append('ǫ'); break;
                    case 'r': result.append('ʀ'); break;
                    case 's': result.append('s'); break;
                    case 't': result.append('ᴛ'); break;
                    case 'u': result.append('ᴜ'); break;
                    case 'v': result.append('ᴠ'); break;
                    case 'w': result.append('ᴡ'); break;
                    case 'x': result.append('x'); break;
                    case 'y': result.append('ʏ'); break;
                    case 'z': result.append('ᴢ'); break;
                    default: result.append(c);
                }
            } else {
                result.append(c);  // Non-letters remain unchanged
            }
        }
        
        return result.toString();
    }
}