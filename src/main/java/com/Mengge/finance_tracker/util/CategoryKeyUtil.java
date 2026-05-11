package com.Mengge.finance_tracker.util;

import java.util.Locale;

public final class CategoryKeyUtil {
    private CategoryKeyUtil() {}

    public static String normalizeKey(String category) {
        return category.trim().toLowerCase(Locale.ROOT);
    }
}
