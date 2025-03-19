package org.example;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

public class Table {

    List<List<String>> rows;
    Map<String, List<String>> details = new HashMap<>();
    float cellWidth = 150;
    float cellHeight = 20;
    boolean isBold;


    Table() {
        this.rows = new ArrayList<>();
        this.isBold = false;
    }

    public void addMap(String tagName, List<String> row){
        details.put(tagName, row);
    }
    void addRow(List<String> row) {
        rows.add(row);
    }


    public List<String> updateRow(List<String> row){

        for(String r : row){
            if(r.contains("th") || r.contains("td")){
                row.removeIf(data -> data.contains("th") || data.contains("td"));
            }
            return row;
        }

    }
}
