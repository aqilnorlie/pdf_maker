package org.example;

import java.io.IOException;

public class Main {
    public static void main(String[] args) throws IOException {

        Engine engine = new Engine();
//        engine.load_file("input.html");
        engine.renderPdf(engine.load_file("D:\\openHTMLtoPDF\\my_own_develop_pdf\\src\\main\\java\\org\\example\\input.html"), "latest1.pdf");
    }
}