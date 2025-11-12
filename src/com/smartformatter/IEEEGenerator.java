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

    private static class Block {
        String type;
        String text;
        Block(String t, String s) { type = t; text = s; }
    }

    public static void generateFromText(String input, String outPath, boolean twoColumn) throws Exception {

        String[] lines = input.replace("\r", "").split("\n");
        int i = 0;

        // ---- Title ----
        String title = lines[i++].trim();
        while (i < lines.length && lines[i].trim().isEmpty()) i++;

        // ---- Author(s) ----
        String authors = lines[i++].trim() + "\n" + lines[i++].trim() + "\n" + lines[i++].trim();
        while (i < lines.length && lines[i].trim().isEmpty()) i++;

        // ---- Abstract ----
        if (lines[i].trim().equalsIgnoreCase("Abstract")) i++;
        StringBuilder abs = new StringBuilder();
        while (i < lines.length && !lines[i].toLowerCase().startsWith("keywords"))
            abs.append(lines[i++].trim()).append(" ");
        String abstractText = abs.toString().trim();
        while (i < lines.length && lines[i].trim().isEmpty()) i++;

        // ---- Keywords ----
        String keywords = lines[i++].trim();
        while (i < lines.length && lines[i].trim().isEmpty()) i++;

        // ---- Parse Remaining Blocks ----
        List<Block> blocks = new ArrayList<>();
        for (; i < lines.length; i++) {
            String s = lines[i].trim();
            if (s.isEmpty()) continue;

            if (s.matches("^\\d+\\..*"))
                blocks.add(new Block("SECTION", s));
            else if (s.startsWith("!"))
                blocks.add(new Block("IMAGE", s.substring(1).trim()));
            else if (s.startsWith("|TABLE|"))
                blocks.add(new Block("TABLE", readTable(lines, i)));
            else if (s.startsWith("$") && s.endsWith("$"))
                blocks.add(new Block("EQUATION", s.substring(1, s.length() - 1).trim()));
            else
                blocks.add(new Block("PARA", s));
        }

        // ---- PDF Setup ----
        Document doc = new Document(PageSize.A4, 56, 56, 70, 70);
        PdfWriter writer = PdfWriter.getInstance(doc, new FileOutputStream(outPath));
        doc.open();

        BaseFont base = BaseFont.createFont(BaseFont.TIMES_ROMAN, BaseFont.WINANSI, BaseFont.EMBEDDED);
        Font titleFont = new Font(base, 14, Font.BOLD);
        Font authorFont = new Font(base, 12, Font.NORMAL);
        Font absHeadFont = new Font(base, 12, Font.BOLD);
        Font absBodyFont = new Font(base, 10, Font.ITALIC);
        Font keyFont = new Font(base, 10, Font.NORMAL);
        Font secFont = new Font(base, 12, Font.BOLD);
        Font paraFont = new Font(base, 10, Font.NORMAL);
        Font capFont = new Font(base, 9, Font.ITALIC);
        Font eqFont = new Font(base, 14, Font.BOLD);

        // ---- Header ----
        Paragraph t = new Paragraph(title, titleFont);
        t.setAlignment(Element.ALIGN_CENTER);
        doc.add(t);
        doc.add(Chunk.NEWLINE);

        Paragraph a = new Paragraph(authors, authorFont);
        a.setAlignment(Element.ALIGN_CENTER);
        doc.add(a);
        doc.add(Chunk.NEWLINE);

        Paragraph absh = new Paragraph("Abstract", absHeadFont);
        absh.setAlignment(Element.ALIGN_CENTER);
        doc.add(absh);

        Paragraph absb = new Paragraph(abstractText, absBodyFont);
        absb.setAlignment(Element.ALIGN_JUSTIFIED);
        absb.setSpacingAfter(12);
        doc.add(absb);

        Paragraph kw = new Paragraph(keywords, keyFont);
        kw.setSpacingAfter(14);
        doc.add(kw);

        // ---- Columns ----
        PdfContentByte cb = writer.getDirectContent();
        ColumnText ct = new ColumnText(cb);
        float gap = 20f;
        float colW = (doc.right() - doc.left() - gap) / 2f;
        float startY = writer.getVerticalPosition(true);

        Rectangle[] cols = {
                new Rectangle(doc.left(), doc.bottom(), doc.left() + colW, startY),
                new Rectangle(doc.left() + colW + gap, doc.bottom(), doc.right(), startY)
        };

        int col = 0;
        setCol(ct, cols[col]);
        int figCount = 1;

        // ---- Add Content ----
        for (Block b : blocks) {
            switch (b.type) {
                case "SECTION":
                    Paragraph sH = new Paragraph(b.text, secFont);
                    sH.setSpacingBefore(10);
                    sH.setSpacingAfter(4);
                    ct.addElement(sH);
                    break;

                case "PARA":
                    Paragraph p = new Paragraph(b.text, paraFont);
                    p.setAlignment(Element.ALIGN_JUSTIFIED);
                    p.setFirstLineIndent(12f);
                    ct.addElement(p);
                    break;

                case "EQUATION":
                    Paragraph eq = new Paragraph(b.text, eqFont);
                    eq.setAlignment(Element.ALIGN_CENTER);
                    eq.setSpacingBefore(10);
                    eq.setSpacingAfter(10);
                    ct.addElement(eq);
                    break;

                case "IMAGE":
                    try {
                        String[] parts = b.text.split("\\|", 2);
                        String path = parts[0].trim();
                        String caption = (parts.length > 1) ? parts[1].trim() : "";

                        byte[] imgBytes = Files.readAllBytes(new File(path).toPath());
                        Image img = Image.getInstance(imgBytes);
                        img.scaleToFit(colW - 8, 260);

                        Paragraph imgWrap = new Paragraph();
                        imgWrap.setAlignment(Element.ALIGN_CENTER);
                        imgWrap.add(new Chunk(img, 0, 0));
                        imgWrap.setSpacingBefore(6);
                        imgWrap.setSpacingAfter(2);
                        ct.addElement(imgWrap);

                        Paragraph cp = new Paragraph("Figure " + figCount++ + ". " + caption, capFont);
                        cp.setAlignment(Element.ALIGN_CENTER);
                        cp.setSpacingAfter(10);
                        ct.addElement(cp);
                    } catch (Exception ex) {
                        ct.addElement(new Paragraph("[Image Error: " + ex.getMessage() + "]", capFont));
                    }
                    break;

                case "TABLE":
                    PdfPTable table = makeSmartTable(b.text, colW);
                    ct.addElement(table);
                    break;
            }

            while (true) {
                int status = ct.go();
                if ((status & ColumnText.NO_MORE_COLUMN) != 0) {
                    col = (col + 1) % 2;
                    if (col == 0) {
                        doc.newPage();
                        cols[0] = new Rectangle(doc.left(), doc.bottom(), doc.left() + colW, doc.top());
                        cols[1] = new Rectangle(doc.left() + colW + gap, doc.bottom(), doc.right(), doc.top());
                    }
                    setCol(ct, cols[col]);
                } else break;
            }
        }

        ct.go();
        addPageNumbers(writer);
        doc.close();
    }

    private static void setCol(ColumnText ct, Rectangle r) {
        ct.setSimpleColumn(r.getLeft(), r.getBottom(), r.getRight(), r.getTop());
    }

    private static String readTable(String[] lines, int start) {
        StringBuilder sb = new StringBuilder();
        for (int j = start + 1; j < lines.length; j++) {
            if (lines[j].trim().equalsIgnoreCase("|ENDTABLE|")) break;
            sb.append(lines[j]).append("\n");
        }
        return sb.toString();
    }

    private static PdfPTable makeSmartTable(String text, float colW) {
        String[] lines = text.split("\n");
        if (lines.length == 0) return new PdfPTable(1);

        String[] headers = lines[0].split(",");
        int totalCols = headers.length;
        List<PdfPTable> tables = new ArrayList<>();

        int startCol = 0;
        while (startCol < totalCols) {
            int endCol = Math.min(startCol + 5, totalCols);
            int colCount = endCol - startCol;

            PdfPTable table = new PdfPTable(colCount);
            table.setWidthPercentage(100);
            table.setSpacingBefore(8);
            table.setSpacingAfter(6);

            for (int c = startCol; c < endCol; c++) {
                PdfPCell cell = new PdfPCell(new Phrase(headers[c].trim(), new Font(Font.HELVETICA, 9, Font.BOLD)));
                cell.setHorizontalAlignment(Element.ALIGN_CENTER);
                cell.setBackgroundColor(new Color(230, 230, 230));
                cell.setPadding(4);
                table.addCell(cell);
            }

            for (int i = 1; i < lines.length; i++) {
                String[] vals = lines[i].split(",");
                for (int c = startCol; c < endCol; c++) {
                    String val = c < vals.length ? vals[c].trim() : "";
                    PdfPCell cell = new PdfPCell(new Phrase(val, new Font(Font.HELVETICA, 9, Font.NORMAL)));
                    cell.setHorizontalAlignment(Element.ALIGN_CENTER);
                    cell.setPadding(4);
                    table.addCell(cell);
                }
            }

            tables.add(table);
            startCol += 5;
        }

        PdfPTable mainTable;
        if (tables.size() == 1) {
            mainTable = tables.get(0);
        } else {
            mainTable = new PdfPTable(1);
            mainTable.setWidthPercentage(100);
            for (PdfPTable t : tables) {
                PdfPCell wrapper = new PdfPCell(t);
                wrapper.setBorder(Rectangle.NO_BORDER);
                wrapper.setPaddingBottom(8);
                mainTable.addCell(wrapper);
            }
        }
        return mainTable;
    }

    private static void addPageNumbers(PdfWriter writer) {
        PdfContentByte cb = writer.getDirectContent();
        Font f = new Font(Font.HELVETICA, 8, Font.NORMAL);
        Phrase p = new Phrase(String.valueOf(writer.getPageNumber()), f);
        ColumnText.showTextAligned(cb, Element.ALIGN_CENTER, p,
                (writer.getPageSize().getWidth()) / 2, 25, 0);
    }
}
