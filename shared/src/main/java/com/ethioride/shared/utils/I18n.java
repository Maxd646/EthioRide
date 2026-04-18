package com.ethioride.shared.utils;

import java.util.Locale;
import java.util.ResourceBundle;

/**
 * Internationalization helper. Supports English and Amharic.
 */
public class I18n {
    private static ResourceBundle bundle;
    private static Locale currentLocale = Locale.ENGLISH;

    public static void setLocale(Locale locale) {
        currentLocale = locale;
        bundle = null; // reset cache
    }

    public static void load(String baseName) {
        bundle = ResourceBundle.getBundle(baseName, currentLocale);
    }

    public static String get(String key) {
        if (bundle == null) return key;
        try {
            return bundle.getString(key);
        } catch (Exception e) {
            return key;
        }
    }

    public static Locale getCurrentLocale() {
        return currentLocale;
    }
}
