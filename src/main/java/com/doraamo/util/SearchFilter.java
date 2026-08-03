package com.doraamo.util;

import java.util.Locale;

/** Case-insensitive substring match used by the Coordinator filter box. */
public final class SearchFilter {

    private SearchFilter() {
    }

    public static boolean matches(String query, String... candidates) {
        if (query == null || query.trim().isEmpty()) {
            return true;
        }
        String q = query.trim().toLowerCase(Locale.ROOT).replace(" ", "");
        for (String raw : candidates) {
            if (raw == null || raw.isEmpty()) {
                continue;
            }
            String c = raw.toLowerCase(Locale.ROOT).replace(" ", "");
            if (c.contains(q)) {
                return true;
            }
        }
        return false;
    }
}
