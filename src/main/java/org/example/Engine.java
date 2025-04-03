package org.example;


import org.apache.pdfbox.pdmodel.PDDocument;
import org.apache.pdfbox.pdmodel.PDPage;
import org.apache.pdfbox.pdmodel.PDPageContentStream;
import org.apache.pdfbox.pdmodel.common.PDRectangle;
import org.apache.pdfbox.pdmodel.font.PDType1Font;
import org.apache.pdfbox.pdmodel.font.Standard14Fonts;
import org.apache.pdfbox.pdmodel.graphics.color.PDColor;
import org.apache.pdfbox.pdmodel.graphics.color.PDDeviceRGB;
import org.apache.pdfbox.pdmodel.graphics.image.PDImageXObject;
import org.jsoup.Jsoup;
import org.jsoup.nodes.Document;
import org.jsoup.nodes.Element;
import org.jsoup.select.Elements;

import java.io.File;
import java.io.IOException;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

public class Engine {

    public Map<String, String> parseCss(String css) {
        Map<String, String> styles = new HashMap<>();
        if (css == null || css.trim().isEmpty()) return styles;

        Pattern pattern = Pattern.compile("([^:]+):\\s*([^;]+);?");
        Matcher matcher = pattern.matcher(css.trim());
        while (matcher.find()) {
            styles.put(matcher.group(1).trim(), matcher.group(2).trim());
        }
        return styles;
    }

    public List<Object> load_file (String file_path) throws IOException {

        List<Object> elementObj = new ArrayList<>();
        File myObj = new File(file_path);
        Document doc = Jsoup.parse(myObj, "UTF-8");

        //Need this to handle global CSS
        Map<String, Map<String, String>> globalStyles = new HashMap<>();
        Elements styleTags = doc.select("style");

        for (Element styleTag : styleTags) {
            String cssContent = styleTag.html();
//            System.out.println("CSS CONTENT : " + cssContent);
            Pattern pattern = Pattern.compile("(\\w+)\\s*\\{([^}]+)\\}");
            Matcher matcher = pattern.matcher(cssContent);

            while (matcher.find()) {
                String tag = matcher.group(1).toLowerCase();
                Map<String, String> styles = parseCss(matcher.group(2));
                globalStyles.put(tag, styles);
                System.out.println("global styles : " + globalStyles);
            }
        }

        // Get all element in HTML
        Elements content = doc.getAllElements();
        System.out.println("GLOBAL STYlE : " + globalStyles);
        //Loop content HTML
        for(Element data : content){

            String tag = data.tagName().toLowerCase();
            String text = data.text().trim(); // To remove whitespace
            LayoutElement layout = new LayoutElement(tag, text);  // add into LayoutElement class

            Table table = new Table();

            if (tag.equals("table")) {
                System.out.println("tag ## : " + tag);
                if (globalStyles.containsKey(tag)){
                    System.out.println("<WHAT DATA?> : " + globalStyles.get(tag));
                    table.setRootStyles(globalStyles.get(tag));

                }

                Map<String, Map<String, String>> rowStylesMapGlobal = new HashMap<>();
                Elements rows = data.select("tr");

                for (Element row : rows) {
                    List<String> rowData = new ArrayList<>();
                    Elements cells = row.select("th, td");
                    for (Element cell : cells) {

                        rowData.add(cell.tagName().trim());
                        System.out.println("NEW @@ : " + cell.tagName());
                        rowData.add(cell.text().trim());
                        System.out.println("WHY : " + rowData);
//

                        if (globalStyles.containsKey(cell.tagName())) {

                            //This is set global CSS for table
                            if(!rowStylesMapGlobal.containsKey(cell.tagName())){
                                rowStylesMapGlobal.put(cell.tagName(), globalStyles.get(cell.tagName()));

                            }
                        }

                        //this is for inline style table
                        if (cell.hasAttr("style")) {
//                            styles.putAll(parseCss(cell.attr("style"))); // Apply inline styles
//                            rowStylesMapGlobal.put(cell.tagName(), parseCss(cell.attr("style")));
                        }

                    }
                    table.addRow(rowData);
                    table.addRowFormat(rowData);
                }
                System.out.println("@@@@@@@@@  :" + rowStylesMapGlobal);
                table.addStyle(rowStylesMapGlobal); //test
                elementObj.add(table);

            }

            if(tag.equals("img")){
                String src = data.attr("src");
                if(!src.isEmpty()){
                    elementObj.add(new Image((src)));
                }
            }

            if(globalStyles.containsKey("page")){
                System.out.println("@@PAGE");
                String detail = globalStyles.get("page").toString();

                Page page = new Page();
                page.setFormat(detail);
                elementObj.add(page);
                globalStyles.remove("page");
            }

            //Ignore tag with 'html', 'body', 'head', 'root' , 'title' and 'meta'
            if(!tag.equals("html") && !tag.equals("body") && !tag.equals("head") && !tag.equals("#root") &&
                    !tag.equals("title") && !tag.equals("meta") && !tag.equals("table") && !tag.equals("tr")
                    && !tag.equals("th") && !tag.equals("td") && !tag.equals("tbody")){

                //if inside globalStyles have tag except above tag
                if (globalStyles.containsKey(tag) ) {
                    layout.applyStyles(globalStyles.get(tag));
                }

                layout.applyFormat(tag); //add format based on tag
                elementObj.add(layout); // add layout object into list
            }
        }

        return elementObj;
    }

    public void renderPdf(List<Object> elementsObj, String outputFile) throws IOException {
        try (PDDocument document = new PDDocument()) {
            PDPage page = null;  // No page created initially
            PDPageContentStream contentStream = null;
            float yPosition = 0;
            float margin = 50; // Left/right margin
            float topMargin = 50;
            float pageWidth = 0;
            PDRectangle pageSize = PDRectangle.A4;

            for (Object element : elementsObj) {
                // Handle Page object to set custom size
                if (element instanceof Page landscape) {
                    pageSize = landscape.landscape
                            ? new PDRectangle(PDRectangle.A4.getHeight(), PDRectangle.A4.getWidth()) // Landscape
                            : PDRectangle.A4;

                    if (contentStream != null) {
                        contentStream.close();
                    }

                    page = new PDPage(pageSize);
                    document.addPage(page);
                    contentStream = new PDPageContentStream(document, page);

                    pageWidth = pageSize.getWidth();
                    yPosition = pageSize.getHeight() - topMargin; // Apply top margin

                    continue; // Move to next element
                }

                // Ensure a page exists before adding content
                if (page == null) {
                    page = new PDPage(pageSize); // Use tracked pageSize
                    document.addPage(page);
                    contentStream = new PDPageContentStream(document, page);
                    pageWidth = page.getMediaBox().getWidth();
                    yPosition = page.getMediaBox().getHeight() - topMargin; // Apply top margin
                }

                if (element instanceof LayoutElement layoutElement) {
                    PDType1Font font;

                    if (layoutElement.isBold && layoutElement.isItalic) {
                        font = new PDType1Font(Standard14Fonts.FontName.HELVETICA_BOLD_OBLIQUE);
                    } else if (layoutElement.isBold) {
                        font = new PDType1Font(Standard14Fonts.FontName.HELVETICA_BOLD);
                    } else if (layoutElement.isItalic) {
                        font = new PDType1Font(Standard14Fonts.FontName.HELVETICA_OBLIQUE);
                    } else if (layoutElement.tag.equals("code") || layoutElement.tag.equals("pre")) {
                        font = new PDType1Font(Standard14Fonts.FontName.COURIER);
                    } else {
                        font = new PDType1Font(Standard14Fonts.FontName.HELVETICA);
                    }

                    contentStream.setFont(font, layoutElement.fontSize);

                    switch (layoutElement.color.toLowerCase()) {
                        case "red": contentStream.setNonStrokingColor(1f, 0f, 0f); break;
                        case "green": contentStream.setNonStrokingColor(0f, 1f, 0f); break;
                        case "blue": contentStream.setNonStrokingColor(0f, 0f, 1f); break;
                        case "black": contentStream.setNonStrokingColor(0f, 0f, 0f); break;
                        default:
                            if (layoutElement.color.startsWith("#") && layoutElement.color.length() == 7) {
                                float r = Integer.parseInt(layoutElement.color.substring(1, 3), 16) / 255f;
                                float g = Integer.parseInt(layoutElement.color.substring(3, 5), 16) / 255f;
                                float b = Integer.parseInt(layoutElement.color.substring(5, 7), 16) / 255f;
                                contentStream.setNonStrokingColor(r, g, b);
                            }
                            break;
                    }

                    float textWidth = font.getStringWidth(layoutElement.text) / 1000 * layoutElement.fontSize;
                    float xOffset = margin;
                    if (layoutElement.textAlign.equals("center")) {
                        xOffset = (pageWidth - textWidth) / 2;
                    } else if (layoutElement.textAlign.equals("right")) {
                        xOffset = pageWidth - textWidth - margin;
                    }

                    contentStream.beginText();
                    contentStream.newLineAtOffset(xOffset, yPosition);
                    contentStream.showText(layoutElement.text);
                    contentStream.endText();

                    yPosition -= (layoutElement.fontSize + 20);

                    if (yPosition < 50) {
                        contentStream.close();
                        page = new PDPage(pageSize); // Use tracked pageSize
                        document.addPage(page);
                        contentStream = new PDPageContentStream(document, page);
                        pageWidth = page.getMediaBox().getWidth();
                        yPosition = page.getMediaBox().getHeight() - topMargin; // Consistent top margin
                    }
                }

                if (element instanceof Image img) {
                    PDImageXObject pdImage = PDImageXObject.createFromFile(img.src, document);
                    float imgWidth = pdImage.getWidth();
                    float imgHeight = pdImage.getHeight();

                    if (imgWidth > img.width || imgHeight > img.height) {
                        float scale = Math.min(img.width / imgWidth, img.height / imgHeight);
                        imgWidth *= scale;
                        imgHeight *= scale;
                    }

                    if (yPosition - imgHeight < 50) {
                        contentStream.close();
                        page = new PDPage(pageSize); // Use tracked pageSize
                        document.addPage(page);
                        contentStream = new PDPageContentStream(document, page);
                        pageWidth = page.getMediaBox().getWidth();
                        yPosition = page.getMediaBox().getHeight() - topMargin; // Consistent top margin
                    }

                    contentStream.drawImage(pdImage, margin, yPosition - imgHeight, imgWidth, imgHeight);
                    yPosition -= imgHeight + 10;
                }

                if (element instanceof Table table) {
                    // Count row
                    int rowCount = table.rows.size();

                    // Apply style table
                    table.applyCSS();

                    // Add this to remove th and td, if not it will count extra column
                    table.rows = table.filterTable(table.rows);

                    int colCount = table.rows.isEmpty() ? 0 : table.rows.getFirst().size();

                    if (rowCount == 0 || colCount == 0) continue;

                    // START : ADJUST MARGIN
                    float tableHeight = rowCount * table.cellHeight;
                    float tableWidth = colCount * table.cellWidth;

                    float startX = 0;
                    float startY = yPosition;

                    if (table.margin == null) {
                        startX = margin; // Left margin
                    } else {
                        float marginPercentage = Float.parseFloat(table.margin) / 100f;
                        startX = pageWidth * marginPercentage;
                    }

                    // END : ADJUST MARGIN

                    float currentY = startY;
                    int rowsDrawn = 0;

                    // Draw table row-by-row with splitting
                    for (int i = 0; i < rowCount; i++) {
                        float nextYPosition = currentY - ((i - rowsDrawn) + 1) * table.cellHeight;

                        // Check for page break
                        if (nextYPosition < 50 && i < rowCount - 1) {
                            // Draw vertical lines up to the bottom of the current page
                            float pageBottomY = currentY - (i - rowsDrawn) * table.cellHeight; // Bottom of last row
                            for (int j = 0; j <= colCount; j++) {
                                float xLine = startX + j * table.cellWidth;
                                contentStream.moveTo(xLine, currentY);
                                contentStream.lineTo(xLine, pageBottomY > 50 ? pageBottomY : 50); // Stop at row bottom or margin
                                contentStream.stroke();
                            }

                            contentStream.close();
                            page = new PDPage(pageSize); // Use tracked pageSize
                            document.addPage(page);
                            contentStream = new PDPageContentStream(document, page);
                            pageWidth = page.getMediaBox().getWidth();
                            currentY = page.getMediaBox().getHeight() - topMargin; // Consistent top margin
                            nextYPosition = currentY - table.cellHeight; // Position of first row on new page
                            rowsDrawn = i; // Update rows drawn on previous page
                        }

                        List<String> originalRow = table.formatRows.get(i);
                        boolean isHeader = table.isHeaderRow(originalRow);
                        List<String> row = table.rows.get(i);

                        PDType1Font font = isHeader ? new PDType1Font(Standard14Fonts.FontName.HELVETICA_BOLD)
                                : new PDType1Font(Standard14Fonts.FontName.HELVETICA);
                        contentStream.setFont(font, 12); // Set font for this row

                        // Draw background
                        float rowTopY = currentY - (i - rowsDrawn) * table.cellHeight; // Top of current row
                        if (originalRow.contains("th")) {
                            switch (table.getThBackgroundColor().toLowerCase()) {
                                case "blue": contentStream.setNonStrokingColor(0f, 0f, 1f); break;
                                case "yellow": contentStream.setNonStrokingColor(1f, 1f, 0f); break;
                                case "red": contentStream.setNonStrokingColor(1f, 0f, 0f); break;
                                case "green": contentStream.setNonStrokingColor(0f, 1f, 0f); break;
                                case "black": contentStream.setNonStrokingColor(0f, 0f, 0f); break;
                                case "white": contentStream.setNonStrokingColor(1f, 1f, 1f); break;
                                case "gray": contentStream.setNonStrokingColor(0.5f, 0.5f, 0.5f); break;
                                case "purple": contentStream.setNonStrokingColor(0.5f, 0f, 0.5f); break;
                                case "orange": contentStream.setNonStrokingColor(1f, 0.65f, 0f); break;
                                case "pink": contentStream.setNonStrokingColor(1f, 0.75f, 0.8f); break;
                                default:
                                    String bgColor = table.getThBackgroundColor();
                                    if (bgColor.startsWith("#") && bgColor.length() == 7) {
                                        float r = Integer.parseInt(bgColor.substring(1, 3), 16) / 255f;
                                        float g = Integer.parseInt(bgColor.substring(3, 5), 16) / 255f;
                                        float b = Integer.parseInt(bgColor.substring(5, 7), 16) / 255f;
                                        contentStream.setNonStrokingColor(r, g, b);
                                    } else {
                                        contentStream.setNonStrokingColor(1f, 1f, 1f);
                                    }
                                    break;
                            }
                            contentStream.addRect(startX, nextYPosition, tableWidth, table.cellHeight);
                            contentStream.fill();
                        } else if (originalRow.contains("td")) {
                            switch (table.getTdBackgroundColor().toLowerCase()) {
                                case "blue": contentStream.setNonStrokingColor(0f, 0f, 1f); break;
                                case "yellow": contentStream.setNonStrokingColor(1f, 1f, 0f); break;
                                case "red": contentStream.setNonStrokingColor(1f, 0f, 0f); break;
                                case "green": contentStream.setNonStrokingColor(0f, 1f, 0f); break;
                                case "black": contentStream.setNonStrokingColor(0f, 0f, 0f); break;
                                case "white": contentStream.setNonStrokingColor(1f, 1f, 1f); break;
                                case "gray": contentStream.setNonStrokingColor(0.5f, 0.5f, 0.5f); break;
                                case "purple": contentStream.setNonStrokingColor(0.5f, 0f, 0.5f); break;
                                case "orange": contentStream.setNonStrokingColor(1f, 0.65f, 0f); break;
                                case "pink": contentStream.setNonStrokingColor(1f, 0.75f, 0.8f); break;
                                default:
                                    String bgColor = table.getTdBackgroundColor();
                                    if (bgColor.startsWith("#") && bgColor.length() == 7) {
                                        float r = Integer.parseInt(bgColor.substring(1, 3), 16) / 255f;
                                        float g = Integer.parseInt(bgColor.substring(3, 5), 16) / 255f;
                                        float b = Integer.parseInt(bgColor.substring(5, 7), 16) / 255f;
                                        contentStream.setNonStrokingColor(r, g, b);
                                    } else {
                                        contentStream.setNonStrokingColor(1f, 1f, 1f);
                                    }
                                    break;
                            }
                            contentStream.addRect(startX, nextYPosition, tableWidth, table.cellHeight);
                            contentStream.fill();
                        }

                        // Set text color
                        if (originalRow.contains("th")) {
                            switch (table.getThColor().toLowerCase()) {
                                case "red": contentStream.setNonStrokingColor(1f, 0f, 0f); break;
                                case "green": contentStream.setNonStrokingColor(0f, 1f, 0f); break;
                                case "blue": contentStream.setNonStrokingColor(0f, 0f, 1f); break;
                                case "black": contentStream.setNonStrokingColor(0f, 0f, 0f); break;
                                case "yellow": contentStream.setNonStrokingColor(1f, 1f, 0f); break;
                                default:
                                    if (table.color.startsWith("#") && table.color.length() == 7) {
                                        float r = Integer.parseInt(table.color.substring(1, 3), 16) / 255f;
                                        float g = Integer.parseInt(table.color.substring(3, 5), 16) / 255f;
                                        float b = Integer.parseInt(table.color.substring(5, 7), 16) / 255f;
                                        contentStream.setNonStrokingColor(r, g, b);
                                    }
                                    break;
                            }
                        } else if (originalRow.contains("td")) {
                            switch (table.getTdColor().toLowerCase()) {
                                case "red": contentStream.setNonStrokingColor(1f, 0f, 0f); break;
                                case "green": contentStream.setNonStrokingColor(0f, 1f, 0f); break;
                                case "blue": contentStream.setNonStrokingColor(0f, 0f, 1f); break;
                                case "black": contentStream.setNonStrokingColor(0f, 0f, 0f); break;
                                case "yellow": contentStream.setNonStrokingColor(1f, 1f, 0f); break;
                                default:
                                    if (table.color.startsWith("#") && table.color.length() == 7) {
                                        float r = Integer.parseInt(table.color.substring(1, 3), 16) / 255f;
                                        float g = Integer.parseInt(table.color.substring(3, 5), 16) / 255f;
                                        float b = Integer.parseInt(table.color.substring(5, 7), 16) / 255f;
                                        contentStream.setNonStrokingColor(r, g, b);
                                    }
                                    break;
                            }
                        }

                        // Draw horizontal grid line for this row
                        float yLine = currentY - (i - rowsDrawn) * table.cellHeight; // Top of current row
                        contentStream.moveTo(startX, yLine);
                        contentStream.lineTo(startX + tableWidth, yLine);
                        contentStream.stroke();

                        // Draw vertical grid lines up to this row
                        float yBottom = nextYPosition; // Bottom of current row
                        for (int j = 0; j <= colCount; j++) {
                            float xLine = startX + j * table.cellWidth;
                            contentStream.moveTo(xLine, currentY); // Start at top of page or section
                            contentStream.lineTo(xLine, yBottom > 50 ? yBottom : 50); // Extend to current row bottom or page bottom
                            contentStream.stroke();
                        }

                        // Draw text
                        for (int j = 0; j < row.size(); j++) {
                            contentStream.beginText();
                            contentStream.newLineAtOffset(startX + j * table.cellWidth + 2, nextYPosition + 5);
                            contentStream.showText(row.get(j));
                            contentStream.endText();
                        }

                        // Draw final bottom line after last row
                        if (i == rowCount - 1) {
                            float finalBottomY = currentY - (rowCount - rowsDrawn) * table.cellHeight;
                            contentStream.moveTo(startX, finalBottomY);
                            contentStream.lineTo(startX + tableWidth, finalBottomY);
                            contentStream.stroke();

                            // Finalize vertical lines to the table bottom
                            for (int j = 0; j <= colCount; j++) {
                                float xLine = startX + j * table.cellWidth;
                                contentStream.moveTo(xLine, currentY);
                                contentStream.lineTo(xLine, finalBottomY > 50 ? finalBottomY : 50);
                                contentStream.stroke();
                            }
                        }
                    }

                    yPosition = currentY - (rowCount - rowsDrawn) * table.cellHeight - 10; // Final position
                }

                if (yPosition < 50) {
                    contentStream.close();
                    page = new PDPage(pageSize); // Use tracked pageSize
                    document.addPage(page);
                    contentStream = new PDPageContentStream(document, page);
                    pageWidth = page.getMediaBox().getWidth();
                    yPosition = page.getMediaBox().getHeight() - topMargin;
                }
            }

            if (contentStream != null) {
                contentStream.close();
            }
            document.save(outputFile);
        }
    }
}
