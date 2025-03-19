package org.example;

import org.jsoup.parser.Tag;

import java.util.Map;

public class LayoutElement {
    String tag;
    String text;
    float fontSize;
    boolean isBold;
    boolean isItalic;
    String color;
    String textAlign;

    LayoutElement(String tag, String text) {
        this.tag = tag;
        this.text = text;
        this.fontSize = 12;
        this.isBold = false;
        this.isItalic = false;
        this.color = "black";
        this.textAlign = "left";
    }

    @Override
    public String toString() {
        return "Tag : " + this.tag + " text : " + this.text + " Bold : " + this.isBold + ". size : " + this.fontSize;
    }

    public void applyFormat(String tag){

        switch (tag) {
            case "h1":
                this.fontSize = 24;
                this.isBold = true;
                break;
            case "h2":
                this.fontSize = 20;
                this.isBold = true;
                break;
            case "h3":
                this.fontSize = 16;
                this.isBold = true;
                break;
            case "h4":
                this.fontSize = 14;
                this.isBold = true;
                break;
            case "h5":
                this.fontSize = 12;
                this.isBold = true;
                break;
            case "h6":
                this.fontSize = 10;
                this.isBold = true;
                break;
            case "b":
            case "strong":
                this.isBold = true;
                break;
            case "i":
            case "em":
                this.isItalic = true;
                break;
            case "li":
                this.text = "• " + text;
                break;
            case "dt":
                this.isBold = true;
                break;
            case "dd":
                this.text = "    " + text;
                break;
            case "blockquote":
                this.text = "  " + text;
                this.isItalic = true;
                break;
            case "code":
            case "pre":
                if (this.fontSize == 12) this.fontSize = 10;
                break;
        }
    }

    void applyStyles(Map<String, String> styles) {
        for (Map.Entry<String, String> style : styles.entrySet()) {
            switch (style.getKey().toLowerCase()) {
                case "font-size":
                    this.fontSize = Float.parseFloat(style.getValue().replace("px", "").replace("pt", ""));
                    break;
                case "font-weight":
                    this.isBold = style.getValue().equalsIgnoreCase("bold") || style.getValue().equals("700");
                    break;
                case "font-style":
                    this.isItalic = style.getValue().equalsIgnoreCase("italic");
                    break;
                case "color":
                    this.color = style.getValue();
                    break;
                case "text-align":
                    this.textAlign = style.getValue().toLowerCase();
                    break;
            }
        }

    }
}

