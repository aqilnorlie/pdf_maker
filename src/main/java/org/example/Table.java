package org.example;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

public class Table {

    List<List<String>> rows;
    List<List<String>> formatRows;
    Map<String, Map<String, String>> stylesGlobal;
    float cellWidth = 100;
    float cellHeight = 30;
    String color;
    String backgroundColor;
    String margin;
    String width;
//    Map<String, String > rootStyles;


    private final Map<String, String> thStyles = new HashMap<>();
    private final Map<String, String> tdStyles = new HashMap<>();

    Table() {
        this.rows = new ArrayList<>();
        this.formatRows = new ArrayList<>();
        this.stylesGlobal = new HashMap<>();
        this.backgroundColor = "white";
        thStyles.put("color", "black");
        thStyles.put("background-color", "white");
        thStyles.put("font-weight", "normal");
        tdStyles.put("color", "black");
        tdStyles.put("background-color", "white");
        tdStyles.put("font-weight", "normal");

    }

    public void addRow(List<String> row) {
        rows.add(row);
    }

    public void addStyle(Map<String, Map<String, String>> styles) {
        stylesGlobal.putAll(styles);
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

    public void applyCSS() {

        for (Map.Entry<String, Map<String, String>> entry : stylesGlobal.entrySet()) {
            String outerKey = entry.getKey(); // "td" or "th"
            Map<String, String> innerMap = entry.getValue();

            System.out.println("Tag: " + outerKey);

            for (Map.Entry<String, String> innerEntry : innerMap.entrySet()) {
                String property = innerEntry.getKey().toLowerCase();
                String value = innerEntry.getValue();
                System.out.println("  " + property + " : " + value);

                if (outerKey.equalsIgnoreCase("th")) {
                    switch (property) {
                        case "background-color":
                            thStyles.put("background-color", value);
                            break;
                        case "color":
                            thStyles.put("color", value);
                            break;
                        case "font-weight":
                            thStyles.put("font-weight", value);
                            break;
                    }
                } else if (outerKey.equalsIgnoreCase("td")) {
                    switch (property) {
                        case "background-color":
                            tdStyles.put("background-color", value);
                            break;
                        case "color":
                            tdStyles.put("color", value);
                            break;
                        case "font-weight":
                            tdStyles.put("font-weight", value);
                            break;
                    }
                }
            }
        }
    }

    // Use to get the value of style according the MAP
    public String getThColor() {
        return thStyles.get("color");
    }

    public String getThBackgroundColor() {
        return thStyles.get("background-color");
    }

    public String getThFontWeight() {
        return thStyles.get("font-weight");
    }

    public String getTdColor() {
        return tdStyles.get("color");
    }

    public String getTdBackgroundColor() {
        return tdStyles.get("background-color");
    }

    public String getTdFontWeight() {
        return tdStyles.get("font-weight");
    }

    public void setRootStyles(Map<String, String> rootStyles){

        System.out.println("inside margin : " + rootStyles);
        for(String key : rootStyles.keySet()){
            if(key.equalsIgnoreCase("margin")){
                System.out.println("MARGIN TABLE : " + rootStyles.get(key));
                String marginValue = rootStyles.get(key);
                this.margin = marginValue.replace("%", "");

            }else if(key.equalsIgnoreCase("width")){
                this.width = rootStyles.get(key).replace("%", "");
            }
        }

    }

//    public void setRootStylesMap(Map<String, String> tableStyle){
//
//        for(String key : tableStyle.keySet()){
//            switch (key){
//                case "margin":
//                    System.out.println("MARGIN TABLE ROOT: " + tableStyle.get(key));
//                    rootStyles.put("margin", tableStyle.get(key).replace("%", ""));
//                    this.margin = tableStyle.get(key).replace("%", "");
//                    break;
//                case "width":
//                    rootStyles.put("width", tableStyle.get(key).replace("%", ""));
//                    break;
//                case "border":
//                    System.out.println("border TABLE ROOT: " + tableStyle.get(key));
//                    rootStyles.put("border", tableStyle.get(key).replace("px", ""));
//                    break;
//            }
//
//        }
//
//        System.out.println("ROOT MAP : " + rootStyles);
//    }


}
