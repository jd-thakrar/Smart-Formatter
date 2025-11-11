package com.smartformatter;

import com.lowagie.text.*;
import com.lowagie.text.pdf.*;
import java.awt.Color;
import java.io.File;
import java.io.FileOutputStream;
import java.nio.file.Files;
import java.util.ArrayList;
import java.util.List;

public class IEEEGenerator {

    // ---------- Data structure ----------
    private static class Block {
        String type;
        String text;
        Block(String t, String s) { type = t; text = s; }
    }

    // ---------- Page Number Footer ----------
    private static class PageNumberEvent extends PdfPageEventHelper {
        private final Font footerFont;

        public PageNumberEvent() throws Exception {
            BaseFont bf = BaseFont.createFont(BaseFont.TIMES_ROMAN, BaseFont.WINANSI, BaseFont.EMBEDDED);
            footerFont = new Font(bf, 9, Font.NORMAL, new Color(100, 100, 100));
        }

        @Override
        public void onEndPage(PdfWriter writer, Document document) {
            PdfContentByte cb = writer.getDirectContent();
            Phrase footer = new Phrase(String.valueOf(writer.getPageNumber()), footerFont);
            // 🔧 Moved lower for better placement
            ColumnText.showTextAligned(cb, Element.ALIGN_CENTER, footer,
                    (document.right() + document.left()) / 2,
                    document.bottom() - 30, 0);
        }
    }

    // ---------- Generator ----------
    public static void generateFromText(String input, String outPath, boolean twoColumn) throws Exception {
        String[] lines = input.replace("\r", "").split("\n");
        int i = 0;

        String title = "";
        StringBuilder author = new StringBuilder();
        StringBuilder abs = new StringBuilder();
        String keywords = "";

        // ---- Parse title ----
        if (i < lines.length) title = lines[i++].trim();
        while (i < lines.length && lines[i].trim().isEmpty()) i++;

        // ---- Parse authors ----
        while (i < lines.length && !lines[i].trim().equalsIgnoreCase("Abstract")) {
            author.append(lines[i++].trim()).append("\n");
            if (author.toString().toLowerCase().contains("email")) break;
        }
        while (i < lines.length && lines[i].trim().isEmpty()) i++;

        // ---- Parse abstract ----
        if (i < lines.length && lines[i].trim().equalsIgnoreCase("Abstract")) i++;
        while (i < lines.length && !lines[i].toLowerCase().startsWith("keywords")) {
            abs.append(lines[i++].trim()).append(" ");
        }

        // ---- Parse keywords ----
        if (i < lines.length && lines[i].toLowerCase().startsWith("keywords"))
            keywords = lines[i++].trim();

        // ---- Parse remaining content ----
        List<Block> blocks = new ArrayList<>();
        for (; i < lines.length; i++) {
            String s = lines[i].trim();
            if (s.isEmpty()) continue;

            if (s.matches("^\\d+\\..*")) blocks.add(new Block("SECTION", s));
            else if (s.startsWith("!")) blocks.add(new Block("IMAGE", s.substring(1, s.length() - 1).trim()));
            else if (s.startsWith("|TABLE|")) {
                StringBuilder tb = new StringBuilder();
                tb.append(s).append("\n");
                i++;
                while (i < lines.length && !lines[i].contains("|ENDTABLE|"))
                    tb.append(lines[i++]).append("\n");
                if (i < lines.length) tb.append(lines[i]).append("\n");
                blocks.add(new Block("TABLE", tb.toString()));
            } else blocks.add(new Block("PARA", s));
        }

        // ---- PDF setup ----
        Document doc = new Document(PageSize.A4, 56, 56, 70, 70);
        PdfWriter writer = PdfWriter.getInstance(doc, new FileOutputStream(outPath));
        writer.setPageEvent(new PageNumberEvent());
        doc.open();

        // ---- Fonts ----
        BaseFont bf = BaseFont.createFont(BaseFont.TIMES_ROMAN, BaseFont.WINANSI, BaseFont.EMBEDDED);
        Font titleFont = new Font(bf, 14, Font.BOLD);
        Font authorFont = new Font(bf, 12, Font.NORMAL);
        Font absHeadFont = new Font(bf, 12, Font.BOLD);
        Font absBodyFont = new Font(bf, 10, Font.ITALIC);
        Font keyFont = new Font(bf, 10, Font.NORMAL);
        Font secFont = new Font(bf, 12, Font.BOLD);
        Font paraFont = new Font(bf, 10, Font.NORMAL);
        Font hFont = new Font(bf, 9, Font.BOLD);
        Font cFont = new Font(bf, 9, Font.NORMAL);
        Font capFont = new Font(bf, 9, Font.ITALIC);

        // ---- Header ----
        Paragraph pTitle = new Paragraph(title, titleFont);
        pTitle.setAlignment(Element.ALIGN_CENTER);
        doc.add(pTitle);
        doc.add(Chunk.NEWLINE);

        Paragraph pAuth = new Paragraph(author.toString().trim(), authorFont);
        pAuth.setAlignment(Element.ALIGN_CENTER);
        doc.add(pAuth);
        doc.add(Chunk.NEWLINE);

        Paragraph pAbsH = new Paragraph("Abstract", absHeadFont);
        pAbsH.setAlignment(Element.ALIGN_CENTER);
        doc.add(pAbsH);

        Paragraph pAbsB = new Paragraph(abs.toString().trim(), absBodyFont);
        pAbsB.setAlignment(Element.ALIGN_JUSTIFIED);
        pAbsB.setSpacingAfter(10);
        doc.add(pAbsB);

        Paragraph pKey = new Paragraph(keywords, keyFont);
        pKey.setSpacingAfter(14);
        doc.add(pKey);

        // ---- Columns ----
        PdfContentByte cb = writer.getDirectContent();
        ColumnText ct = new ColumnText(cb);
        float gap = 20f;
        float colW = (doc.right() - doc.left() - (twoColumn ? gap : 0)) / (twoColumn ? 2f : 1f);
        float startY = writer.getVerticalPosition(true);

        Rectangle[] cols = {
                new Rectangle(doc.left(), doc.bottom(), doc.left() + colW, startY),
                new Rectangle(doc.left() + colW + gap, doc.bottom(), doc.right(), startY)
        };

        int col = 0;
        setColumn(ct, cols[col]);

        // ---- Render ----
        for (Block b : blocks) {
            switch (b.type) {
                case "SECTION":
                    Paragraph sHead = new Paragraph(b.text, secFont);
                    sHead.setSpacingBefore(8);
                    sHead.setSpacingAfter(4);
                    ct.addElement(sHead);
                    break;

                case "PARA":
                    Paragraph para = new Paragraph(b.text, paraFont);
                    para.setAlignment(Element.ALIGN_JUSTIFIED);
                    para.setFirstLineIndent(12);
                    para.setSpacingBefore(0);
                    para.setSpacingAfter(6);
                    ct.addElement(para);
                    break;

                case "IMAGE":
                    addImage(ct, b.text, colW, capFont);
                    break;

                case "TABLE":
                    ct.go(); // flush column before adding table
                    List<PdfPTable> parts = makeColumnFriendlyTables(b.text, hFont, cFont);
                    for (PdfPTable t : parts) {
                        t.setSpacingBefore(6f);
                        t.setSpacingAfter(6f);
                        ct.addElement(t);
                        ct.go();
                    }
                    break;
            }

            // ---- Manage column flow ----
            while (true) {
                int status = ct.go();
                if ((status & ColumnText.NO_MORE_COLUMN) != 0) {
                    if (twoColumn) {
                        col = (col + 1) % 2;
                        if (col == 0) {
                            doc.newPage();
                            // ✅ full height for new pages
                            cols[0] = new Rectangle(doc.left(), doc.bottom(), doc.left() + colW, doc.top());
                            cols[1] = new Rectangle(doc.left() + colW + gap, doc.bottom(), doc.right(), doc.top());
                        }
                        setColumn(ct, cols[col]);
                    } else {
                        // ✅ single-column fix: reset full height
                        doc.newPage();
                        setColumn(ct, new Rectangle(doc.left(), doc.bottom(), doc.right(), doc.top()));
                    }
                } else break;
            }
        }

        ct.go();
        doc.close();
    }

    // ---------- Table Utilities ----------
    private static List<PdfPTable> makeColumnFriendlyTables(String raw, Font hFont, Font cFont) throws Exception {
        String[] lines = raw.split("\n");
        List<String[]> rows = new ArrayList<>();
        for (String l : lines) {
            String s = l.trim();
            if (s.isEmpty() || s.contains("|TABLE|") || s.contains("|ENDTABLE|")) continue;
            rows.add(s.split("\\s*,\\s*"));
        }

        List<PdfPTable> parts = new ArrayList<>();
        if (rows.isEmpty()) return parts;
        int totalCols = rows.get(0).length;
        int chunk = 5; // split after 5 columns

        for (int c = 0; c < totalCols; c += chunk) {
            int end = Math.min(c + chunk, totalCols);
            List<String[]> sub = new ArrayList<>();
            for (String[] row : rows) {
                String[] slice = new String[end - c];
                System.arraycopy(row, c, slice, 0, end - c);
                sub.add(slice);
            }
            PdfPTable t = buildTable(sub, hFont, cFont);
            parts.add(t);
        }
        return parts;
    }

    private static PdfPTable buildTable(List<String[]> rows, Font hFont, Font cFont) {
        int n = rows.get(0).length;
        PdfPTable t = new PdfPTable(n);
        t.setWidthPercentage(95);
        boolean header = true;
        for (String[] row : rows) {
            for (String c : row) {
                PdfPCell cell = new PdfPCell(new Phrase(c, header ? hFont : cFont));
                cell.setHorizontalAlignment(Element.ALIGN_CENTER);
                cell.setVerticalAlignment(Element.ALIGN_MIDDLE);
                cell.setPadding(4);
                if (header) cell.setBackgroundColor(new Color(230, 230, 230));
                t.addCell(cell);
            }
            header = false;
        }
        return t;
    }

    // ---------- Image Utility ----------
    private static void addImage(ColumnText ct, String path, float colW, Font capFont) {
        try {
            File f = new File(path);
            byte[] bytes = Files.readAllBytes(f.toPath());
            Image img = Image.getInstance(bytes);
            img.scaleToFit(colW - 10, 220);
            img.setAlignment(Element.ALIGN_CENTER);
            Paragraph imgP = new Paragraph();
            imgP.add(new Chunk(img, 0, 0));
            imgP.setAlignment(Element.ALIGN_CENTER);
            imgP.setSpacingBefore(6);
            imgP.setSpacingAfter(6);
            ct.addElement(imgP);
        } catch (Exception e) {
            ct.addElement(new Paragraph("[Image missing: " + path + "]", capFont));
        }
    }

    // ---------- Column Utility ----------
    private static void setColumn(ColumnText ct, Rectangle r) {
        ct.setSimpleColumn(r.getLeft(), r.getBottom(), r.getRight(), r.getTop());
    }
}
