package com.laundry.util;

import java.io.BufferedWriter;
import java.io.File;
import java.io.FileWriter;
import java.io.IOException;
import java.nio.charset.StandardCharsets;
import javax.swing.JTable;
import javax.swing.table.TableModel;

/**
 * Utility to export JTable data or custom arrays to standard CSV files.
 */
public class CsvExporter {

    public static void exportTableToCsv(JTable table, File file) throws IOException {
        TableModel model = table.getModel();
        try (BufferedWriter bw = new BufferedWriter(new FileWriter(file, StandardCharsets.UTF_8))) {
            // Write Headers
            for (int col = 0; col < model.getColumnCount(); col++) {
                String header = escapeCsv(model.getColumnName(col));
                bw.write(header);
                if (col < model.getColumnCount() - 1) {
                    bw.write(",");
                }
            }
            bw.newLine();

            // Write Data Rows
            for (int row = 0; row < model.getRowCount(); row++) {
                for (int col = 0; col < model.getColumnCount(); col++) {
                    Object val = model.getValueAt(row, col);
                    String text = val != null ? val.toString() : "";
                    bw.write(escapeCsv(text));
                    if (col < model.getColumnCount() - 1) {
                        bw.write(",");
                    }
                }
                bw.newLine();
            }
        }
    }

    private static String escapeCsv(String text) {
        if (text == null) return "\"\"";
        String escaped = text.replace("\"", "\"\"");
        if (escaped.contains(",") || escaped.contains("\n") || escaped.contains("\"") || escaped.contains("\r")) {
            return "\"" + escaped + "\"";
        }
        return escaped;
    }
}
