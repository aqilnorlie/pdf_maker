package org.example;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

public class Table {

    List<List<String>> rows;
    List<List<String>> formatRows;
    List<List<Map<String, String>>> rowStyles;
    float cellWidth = 150;
    float cellHeight = 20;
    boolean isBold;



    Table() {
        this.rows = new ArrayList<>();
        this.formatRows = new ArrayList<>();
        this.rowStyles = new ArrayList<>();
        this.isBold = false;
    }

    void addRow(List<String> row,  List<Map<String, String>> styles) {
        rows.add(row);
        rowStyles.add(styles);
    }
    void addRowFormat(List<String> row) {
        formatRows.add(row);
    }


    public List<List<String>> filterTable(List<List<String>> table) {
        return table.stream()
                .map(row -> row.stream()
                        .filter(data -> !data.equals("th") && !data.equals("td"))
                        .collect(Collectors.toList()))
                .collect(Collectors.toList());
    }

    public boolean isHeaderRow(List<String> row) {
        return row.contains("th"); // If the row contains "th", it's a header row
    }

}
