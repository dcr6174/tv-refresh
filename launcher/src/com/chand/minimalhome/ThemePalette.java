package com.chand.minimalhome;

final class ThemePalette {
    static final String[] NAMES = {"Auto", "Ocean Blue", "Emerald", "Violet", "Crimson", "Amber", "Aurora"};
    static final int[] COLORS = {0xFF167DDF, 0xFF167DDF, 0xFF00A572, 0xFF883EE8, 0xFFCF244B, 0xFFDC8118, 0xFF24B9A4};
    static final String[] FESTIVALS = {"Dussehra", "Deepavali", "Sankranti", "Bhogi", "Kanuma", "Ugadi", "Bathukamma", "Navaratri", "Navaratri & Bathukamma", "Bonalu", "Vinayaka Chavithi", "Sri Rama Navami", "Shivaratri", "Holi", "Varalakshmi", "Rakhi Pournami", "Krishnashtami", "Hanuman Jayanti", "Akshaya Tritiya", "Atla Tadde", "Nagula Chavithi", "Karthika Pournami"};
    static String normalize(String name) {
        for (String candidate : NAMES) if (candidate.equals(name)) return name;
        for (String candidate : FESTIVALS) if (candidate.equals(name)) return name;
        if ("Warm Dusk".equals(name)) return "Amber";
        return "Ocean Blue";
    }
    static String resolve(String name) {
        name = normalize(name);
        if (!"Auto".equals(name)) return name;
        String festival = FestivalCalendar.today();
        if (!festival.isEmpty()) return festival;
        return "Ocean Blue";
    }
    static boolean festive(String name) {
        String resolved = resolve(name);
        for (String candidate : FESTIVALS) if (candidate.equals(resolved)) return true;
        return false;
    }
    static int color(String name) {
        name = resolve(name);
        for (int i = 0; i < NAMES.length; i++) if (NAMES[i].equals(name)) return COLORS[i];
        if (name.contains("Bathukamma") || name.equals("Holi")) return 0xFFDC4BA7;
        if (name.equals("Ugadi") || name.equals("Nagula Chavithi")) return 0xFF20AF7A;
        if (name.equals("Shivaratri") || name.equals("Krishnashtami")) return 0xFF6B70DF;
        if (name.equals("Deepavali") || name.equals("Karthika Pournami")) return 0xFFE8A333;
        if (name.equals("Sankranti") || name.equals("Kanuma")) return 0xFFEE8A35;
        return 0xFFDF7140;
    }
    static int highlight(String name) {
        int c = color(name);
        return android.graphics.Color.rgb((android.graphics.Color.red(c) + 255) / 2,
                (android.graphics.Color.green(c) + 255) / 2, (android.graphics.Color.blue(c) + 255) / 2);
    }
}
