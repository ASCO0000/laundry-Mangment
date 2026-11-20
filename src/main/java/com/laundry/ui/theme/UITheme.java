package com.laundry.ui.theme;

import com.formdev.flatlaf.FlatLightLaf;
import java.awt.Color;
import java.awt.Font;
import java.util.Enumeration;
import java.util.logging.Level;
import java.util.logging.Logger;
import javax.swing.BorderFactory;
import javax.swing.UIDefaults;
import javax.swing.UIManager;

/**
 * Initializes FlatLaf Look and Feel with the application's blue-and-white theme.
 */
public class UITheme {

    private static final Logger LOGGER = Logger.getLogger(UITheme.class.getName());

    public static void setup() {
        try {
            // Apply FlatLaf Light
            FlatLightLaf.setup();

            // Override global UI properties for high-polish modern aesthetics
            UIManager.put("Component.focusWidth", 1);
            UIManager.put("Component.innerFocusWidth", 0);
            UIManager.put("Button.arc", 8);
            UIManager.put("Component.arc", 8);
            UIManager.put("TextComponent.arc", 8);
            UIManager.put("ScrollBar.showButtons", false);
            UIManager.put("ScrollBar.thumbArc", 999);
            UIManager.put("ScrollBar.thumbInsets", new java.awt.Insets(2, 2, 2, 2));
            UIManager.put("Table.selectionBackground", new Color(0xDB, 0xEA, 0xFE));
            UIManager.put("Table.selectionForeground", UIConstants.PRIMARY_NAVY);
            UIManager.put("Table.alternateRowColor", new Color(0xF8, 0xFA, 0xFC));
            UIManager.put("TableHeader.background", UIConstants.BG_LIGHT);
            UIManager.put("TableHeader.foreground", UIConstants.TEXT_PRIMARY);
            UIManager.put("TableHeader.separatorColor", UIConstants.CARD_BORDER);

            setGlobalFont(new Font("Segoe UI", Font.PLAIN, 13));
        } catch (Throwable t) {
            LOGGER.log(Level.INFO, "FlatLaf setup skipped or unavailable, using System Look and Feel.", t);
            try {
                UIManager.setLookAndFeel(UIManager.getSystemLookAndFeelClassName());
            } catch (Exception ignored) {}
        }
    }

    private static void setGlobalFont(Font font) {
        UIDefaults defaults = UIManager.getDefaults();
        Enumeration<Object> keys = defaults.keys();
        while (keys.hasMoreElements()) {
            Object key = keys.nextElement();
            Object value = defaults.get(key);
            if (value instanceof Font) {
                defaults.put(key, font);
            }
        }
    }
}
