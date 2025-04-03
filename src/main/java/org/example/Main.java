package org.example;

import java.io.IOException;

public class Main {
    public static void main(String[] args) throws IOException {

        Engine engine = new Engine();
        engine.renderPdf(engine.load_file("C:\\Users\\aqiln\\Desktop\\inglab\\openHTMLtoPDF\\my_own_develop_pdf-modification\\src\\main\\java\\org\\example\\input.html"),  "latest24new2.pdf");
    }
}