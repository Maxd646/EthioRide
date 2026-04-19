package com.ethioride.shared.utils;

import java.util.Locale;
import java.util.ResourceBundle;

/**
 * Simple i18n helper — English only.
 */
public class I18n {
    private static ResourceBundle bundle;

    public static void load(String baseName) {
        bundle = ResourceBundle.getBundle(baseName, Locale.ENGLISH);
    }

    public static String get(String key) {
        if (bundle == null) return key;
        try {
            return bundle.getString(key);
        } catch (Exception e) {
            return key;
        }
    }
}
