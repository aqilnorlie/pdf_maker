package org.example;


import com.helger.css.ECSSVersion;
import com.helger.css.decl.*;
import com.helger.css.reader.CSSReader;

import com.helger.css.reader.CSSReaderDeclarationList;
import org.apache.pdfbox.pdmodel.PDDocument;
import org.apache.pdfbox.pdmodel.PDPage;
import org.apache.pdfbox.pdmodel.PDPageContentStream;
import org.apache.pdfbox.pdmodel.common.PDRectangle;
import org.apache.pdfbox.pdmodel.font.PDType1Font;
import org.apache.pdfbox.pdmodel.font.Standard14Fonts;
import org.apache.pdfbox.pdmodel.graphics.image.PDImageXObject;
import org.jsoup.Jsoup;
import org.jsoup.nodes.Document;
import org.jsoup.nodes.Element;
import org.jsoup.select.Elements;

import java.io.File;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Paths;
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
//        System.out.println((handleCSS(file_path)));  // handle notation like @page to make it horizontal or portrait
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

        //Loop content HTML
        for(Element data : content){

            String tag = data.tagName().toLowerCase();
            String text = data.text().trim(); // To remove whitespace
            LayoutElement layout = new LayoutElement(tag, text);  // add into LayoutElement class


            if (tag.equals("table")) {
                Table table = new Table();
                Elements rows = data.select("tr");
                for (Element row : rows) {
                    List<String> rowData = new ArrayList<>();
                    List<Map<String, String>> rowStyles = new ArrayList<>();

                    Elements cells = row.select("th, td");
                    for (Element cell : cells) {

                        rowData.add(cell.tagName().trim());
                        rowData.add(cell.text().trim());

                        Map<String, String> styles = new HashMap<>();
                        if (globalStyles.containsKey(cell.tagName())) {
                            styles.putAll(globalStyles.get(tag));  // Apply global styles
                        }
                        if (cell.hasAttr("style")) {
                            styles.putAll(parseCss(cell.attr("style"))); // Apply inline styles
                        }
                        rowStyles.add(styles);

                        System.out.println(rowData);
                    }
                    table.addRow(rowData, rowStyles);
                    table.addRowFormat(rowData);


                }
                System.out.println("Table Row : " + table.rows);
                System.out.println("Format array : " + table.formatRows);
                System.out.println("Format array : " + table.rowStyles);
                elementObj.add(table);
            }

            if(tag.equals("img")){

                String src = data.attr("src");
                if(!src.isEmpty()){
                    elementObj.add(new Image((src)));
                }
            }

            if(globalStyles.containsKey("page")){
                System.out.println("@@");
                Page page = new Page();
                page.setFormat();
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

        try(PDDocument document = new PDDocument()){

            PDPage page = new PDPage(PDRectangle.A4);
            document.addPage(page);
            PDPageContentStream contentStream = new PDPageContentStream(document, page);

            float yPosition = page.getMediaBox().getHeight() - 50;
            float margin = 50;
            float pageWidth = page.getMediaBox().getWidth();


            for(Object element : elementsObj){  //loop list

                if (element instanceof Page landscape) {

                    PDRectangle pageSize = landscape.landscape
                            ? new PDRectangle(PDRectangle.A4.getHeight(), PDRectangle.A4.getWidth()) // Landscape
                            : PDRectangle.A4; // Portrait

                    if (contentStream != null) {
                        contentStream.close(); // Close previous stream before creating a new page
                    }

                    page = new PDPage(pageSize);
                    document.addPage(page);
                    contentStream = new PDPageContentStream(document, page);

                    // Update Page Dimensions
                    pageWidth = pageSize.getWidth();
                    yPosition = pageSize.getHeight() - 50;

                    continue;
                }
                if (page == null) {
                    page = new PDPage(PDRectangle.A4); // Default to portrait if no Page object appears first
                    document.addPage(page);
                    contentStream = new PDPageContentStream(document, page);
                    pageWidth = page.getMediaBox().getWidth();
                    yPosition = page.getMediaBox().getHeight() - 50;
                }


                // check in the list of object LayoutElement
                if(element instanceof LayoutElement layoutElement){

                    PDType1Font font;

                    //Setup font based on property
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

                    //set the font
                    contentStream.setFont(font, layoutElement.fontSize);

                    switch (layoutElement.color.toLowerCase()) {
                        case "red": contentStream.setNonStrokingColor(1f, 0f, 0f); break;
                        case "green": contentStream.setNonStrokingColor(0f, 1f, 0f); break;
                        case "blue": contentStream.setNonStrokingColor(0f, 0f, 1f); break;
                        case "black": contentStream.setNonStrokingColor(0f, 0f, 0f); break;
                        default:
                            if (layoutElement.color.startsWith("#") && layoutElement.color.length() == 7) {
                                int r = (int) (Integer.parseInt(layoutElement.color.substring(1, 3), 16) / 255f);
                                int g = (int) (Integer.parseInt(layoutElement.color.substring(3, 5), 16) / 255f);
                                int b = (int) (Integer.parseInt(layoutElement.color.substring(5, 7), 16) / 255f);
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

                    //Start write the HTML into PDf
                    contentStream.beginText();
                    contentStream.newLineAtOffset(xOffset, yPosition); //handle x and y axis where y is line
                    System.out.println("START");
                    System.out.println(layoutElement.text);
                    contentStream.showText(layoutElement.text);
                    System.out.println("End");
                    contentStream.endText();

                    yPosition -= (layoutElement.fontSize + 20); // based on the font set new line

                    if (yPosition < 50) {
                        contentStream.close(); // Close the current content stream
                        PDPage newPage = new PDPage();
                        document.addPage(newPage);
                        contentStream = new PDPageContentStream(document, newPage);
                        yPosition = newPage.getMediaBox().getHeight() - 50; // Reset yPosition for new page
                    }

                }

                // check in the list of object Image
                if (element instanceof Image img){

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
                        page = new PDPage();
                        document.addPage(page);
                        contentStream = new PDPageContentStream(document, page);
                        yPosition = page.getMediaBox().getHeight() - 50;
                    }

                    //Insert image into pdf
                    contentStream.drawImage(pdImage, margin, yPosition - imgHeight, imgWidth, imgHeight);
                    yPosition -= imgHeight + 10;

                }

                // check in the list of object Table
                 if (element instanceof Table table) {

                     //count row
                    int rowCount = table.rows.size();

                     // Add this to remove th and td, if not it will count extra column
                    table.rows = table.filterTable(table.rows);

                    int colCount = table.rows.isEmpty() ? 0 : table.rows.getFirst().size();

                    if (rowCount == 0 || colCount == 0) continue;

                    float tableHeight = rowCount * table.cellHeight;
                    float tableWidth = colCount * table.cellWidth;

                    if (yPosition - tableHeight < 50) {
                        contentStream.close();
                        page = new PDPage();
                        document.addPage(page);
                        contentStream = new PDPageContentStream(document, page);
                        yPosition = page.getMediaBox().getHeight() - 50;
                    }

                    float startX = margin;
                    float startY = yPosition;
                    for (int i = 0; i <= rowCount; i++) {
                        contentStream.moveTo(startX, startY - i * table.cellHeight);
                        contentStream.lineTo(startX + tableWidth, startY - i * table.cellHeight);
                        contentStream.stroke();
                    }
                    for (int j = 0; j <= colCount; j++) {
                        contentStream.moveTo(startX + j * table.cellWidth, startY);
                        contentStream.lineTo(startX + j * table.cellWidth, startY - tableHeight);
                        contentStream.stroke();
                    }

                    for (int i = 0; i < rowCount; i++) {

                        List<String> originalRow = table.formatRows.get(i);
                        boolean isHeader = table.isHeaderRow(originalRow);  // to check 'th'
                        List<String> row = table.rows.get(i);

                        PDType1Font font = isHeader ? new PDType1Font(Standard14Fonts.FontName.HELVETICA_BOLD)
                                : new PDType1Font(Standard14Fonts.FontName.HELVETICA);

                        contentStream.setFont(font, 12);
                        contentStream.setNonStrokingColor(0f, 0f, 0f);

                        System.out.println("DATA CHECK : " + row);
                        for (int j = 0; j < row.size(); j++) {

                            contentStream.beginText();
                            contentStream.newLineAtOffset(startX + j * table.cellWidth + 2, startY - (i + 1) * table.cellHeight + 5);
                            contentStream.showText(row.get(j));
                            contentStream.endText();
                        }
                    }

                    yPosition -= tableHeight + 10;
                }

                if (yPosition < 50) {
                    contentStream.close();
                    page = new PDPage();
                    document.addPage(page);
                    contentStream = new PDPageContentStream(document, page);
                    yPosition = page.getMediaBox().getHeight() - 50;
                }
            }

            contentStream.close();
            document.save(outputFile);

        }

    }

    public void renderPdf2(List<Object> elementsObj, String outputFile) throws IOException {
        try (PDDocument document = new PDDocument()) {
            PDPage page = null;  // No page created initially
            PDPageContentStream contentStream = null;
            float yPosition = 0;
            float margin = 50;
            float pageWidth = 0;

            for (Object element : elementsObj) {

                if (element instanceof Page landscape) {
                    PDRectangle pageSize = landscape.landscape
                            ? new PDRectangle(PDRectangle.A4.getHeight(), PDRectangle.A4.getWidth()) // Landscape
                            : PDRectangle.A4; // Portrait

                    if (contentStream != null) {
                        contentStream.close();
                    }

                    page = new PDPage(pageSize);
                    document.addPage(page);
                    contentStream = new PDPageContentStream(document, page);

                    pageWidth = pageSize.getWidth();
                    yPosition = pageSize.getHeight() - 50;

                    continue; // Move to next element
                }

                // Ensure a page exists before adding content
                if (page == null) {
                    page = new PDPage(PDRectangle.A4); // Default to portrait if no Page object appears first
                    document.addPage(page);
                    contentStream = new PDPageContentStream(document, page);
                    pageWidth = page.getMediaBox().getWidth();
                    yPosition = page.getMediaBox().getHeight() - 50;
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
                                int r = (int) (Integer.parseInt(layoutElement.color.substring(1, 3), 16) / 255f);
                                int g = (int) (Integer.parseInt(layoutElement.color.substring(3, 5), 16) / 255f);
                                int b = (int) (Integer.parseInt(layoutElement.color.substring(5, 7), 16) / 255f);
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
                        page = new PDPage();
                        document.addPage(page);
                        contentStream = new PDPageContentStream(document, page);
                        yPosition = page.getMediaBox().getHeight() - 50;
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
                        page = new PDPage();
                        document.addPage(page);
                        contentStream = new PDPageContentStream(document, page);
                        yPosition = page.getMediaBox().getHeight() - 50;
                    }

                    contentStream.drawImage(pdImage, margin, yPosition - imgHeight, imgWidth, imgHeight);
                    yPosition -= imgHeight + 10;
                }

                if (element instanceof Table table) {

                    //count row
                    int rowCount = table.rows.size();

                    // Add this to remove th and td, if not it will count extra column
                    table.rows = table.filterTable(table.rows);

                    int colCount = table.rows.isEmpty() ? 0 : table.rows.getFirst().size();

                    if (rowCount == 0 || colCount == 0) continue;

                    float tableHeight = rowCount * table.cellHeight;
                    float tableWidth = colCount * table.cellWidth;

                    if (yPosition - tableHeight < 50) {
                        contentStream.close();
                        page = new PDPage();
                        document.addPage(page);
                        contentStream = new PDPageContentStream(document, page);
                        yPosition = page.getMediaBox().getHeight() - 50;
                    }

                    float startX = margin;
                    float startY = yPosition;
                    for (int i = 0; i <= rowCount; i++) {
                        contentStream.moveTo(startX, startY - i * table.cellHeight);
                        contentStream.lineTo(startX + tableWidth, startY - i * table.cellHeight);
                        contentStream.stroke();
                    }
                    for (int j = 0; j <= colCount; j++) {
                        contentStream.moveTo(startX + j * table.cellWidth, startY);
                        contentStream.lineTo(startX + j * table.cellWidth, startY - tableHeight);
                        contentStream.stroke();
                    }

                    for (int i = 0; i < rowCount; i++) {

                        List<String> originalRow = table.formatRows.get(i);
                        boolean isHeader = table.isHeaderRow(originalRow);  // to check 'th'
                        List<String> row = table.rows.get(i);

                        PDType1Font font = isHeader ? new PDType1Font(Standard14Fonts.FontName.HELVETICA_BOLD)
                                : new PDType1Font(Standard14Fonts.FontName.HELVETICA);

                        contentStream.setFont(font, 12);
                        contentStream.setNonStrokingColor(0f, 0f, 0f);

                        System.out.println("DATA CHECK : " + row);
                        for (int j = 0; j < row.size(); j++) {

                            contentStream.beginText();
                            contentStream.newLineAtOffset(startX + j * table.cellWidth + 2, startY - (i + 1) * table.cellHeight + 5);
                            contentStream.showText(row.get(j));
                            contentStream.endText();
                        }
                    }

                    yPosition -= tableHeight + 10;
                }

                if (yPosition < 50) {
                    contentStream.close();
                    page = new PDPage();
                    document.addPage(page);
                    contentStream = new PDPageContentStream(document, page);
                    yPosition = page.getMediaBox().getHeight() - 50;
                }
            }

            if (contentStream != null) {
                contentStream.close();
            }

            document.save(outputFile);
        }
    }
}
