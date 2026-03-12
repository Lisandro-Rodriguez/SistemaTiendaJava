package com.tienda.util;

import com.itextpdf.text.*;
import com.itextpdf.text.pdf.*;
import com.tienda.db.VentaDAO;
import org.apache.poi.ss.usermodel.*;
import org.apache.poi.ss.usermodel.Font;
import org.apache.poi.xssf.usermodel.*;

import java.awt.Desktop;
import java.io.*;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.List;
import java.util.prefs.Preferences;

/**
 * Exporta resumen del día/período a PDF y/o Excel.
 */
public class ExportadorReporte {

    private static String getNombreNegocio() {
        return Preferences.userRoot().node("tienda").get("nombre_negocio", "MI KIOSCO");
    }

    // ─── EXPORTAR PDF ───────────────────────────────────────────────────────────

    public static File exportarResumenPDF(String periodo) {
        try {
            File folder = new File("reportes");
            if (!folder.exists()) folder.mkdirs();

            String ts = LocalDateTime.now().format(DateTimeFormatter.ofPattern("yyyy-MM-dd_HH-mm"));
            String ruta = "reportes/Reporte_" + periodo + "_" + ts + ".pdf";

            Document doc = new Document(PageSize.A4);
            doc.setMargins(50, 50, 50, 50);
            PdfWriter.getInstance(doc, new FileOutputStream(ruta));
            doc.open();

            // Fuentes
            com.itextpdf.text.Font fTitulo  = new com.itextpdf.text.Font(com.itextpdf.text.Font.FontFamily.HELVETICA, 20, com.itextpdf.text.Font.BOLD, new BaseColor(30, 90, 160));
            com.itextpdf.text.Font fSubtit  = new com.itextpdf.text.Font(com.itextpdf.text.Font.FontFamily.HELVETICA, 13, com.itextpdf.text.Font.BOLD);
            com.itextpdf.text.Font fNormal  = new com.itextpdf.text.Font(com.itextpdf.text.Font.FontFamily.HELVETICA, 10);
            com.itextpdf.text.Font fBold    = new com.itextpdf.text.Font(com.itextpdf.text.Font.FontFamily.HELVETICA, 10, com.itextpdf.text.Font.BOLD);
            com.itextpdf.text.Font fHeader  = new com.itextpdf.text.Font(com.itextpdf.text.Font.FontFamily.HELVETICA, 9, com.itextpdf.text.Font.BOLD, BaseColor.WHITE);
            com.itextpdf.text.Font fCell    = new com.itextpdf.text.Font(com.itextpdf.text.Font.FontFamily.HELVETICA, 8);

            // Título
            Paragraph pTitulo = new Paragraph(getNombreNegocio() + " — Reporte de Ventas", fTitulo);
            pTitulo.setAlignment(Element.ALIGN_CENTER);
            pTitulo.setSpacingAfter(4);
            doc.add(pTitulo);

            String labelPeriodo = switch (periodo) {
                case "hoy"    -> "Hoy (" + LocalDateTime.now().format(DateTimeFormatter.ofPattern("dd/MM/yyyy")) + ")";
                case "semana" -> "Últimos 7 días";
                case "mes"    -> "Últimos 30 días";
                default       -> "Histórico completo";
            };
            Paragraph pPeriodo = new Paragraph("Período: " + labelPeriodo, fNormal);
            pPeriodo.setAlignment(Element.ALIGN_CENTER);
            pPeriodo.setSpacingAfter(2);
            doc.add(pPeriodo);

            Paragraph pGen = new Paragraph("Generado: " + LocalDateTime.now().format(DateTimeFormatter.ofPattern("dd/MM/yyyy HH:mm")), fNormal);
            pGen.setAlignment(Element.ALIGN_CENTER);
            pGen.setSpacingAfter(16);
            doc.add(pGen);

            // Resumen ejecutivo
            double[] datos = VentaDAO.obtenerVentasYGananciaPeriodo(periodo);
            double ventas   = datos[0];
            double ganancia = datos[1];
            double costo    = ventas - ganancia;
            double margen   = ventas > 0 ? (ganancia / ventas * 100) : 0;

            doc.add(new Paragraph("RESUMEN EJECUTIVO", fSubtit));
            doc.add(new Chunk(new com.itextpdf.text.pdf.draw.LineSeparator()));
            doc.add(Chunk.NEWLINE);

            PdfPTable tResumen = new PdfPTable(2);
            tResumen.setWidthPercentage(60);
            tResumen.setHorizontalAlignment(Element.ALIGN_LEFT);
            tResumen.setWidths(new float[]{2f, 1.5f});
            tResumen.setSpacingAfter(16);

            agregarFilaResumen(tResumen, "💰 Total Ventas",    "$" + fmt(ventas),   fBold, fBold);
            agregarFilaResumen(tResumen, "📦 Costo Mercadería","$" + fmt(costo),    fNormal, fNormal);
            agregarFilaResumen(tResumen, "📈 Ganancia Bruta",  "$" + fmt(ganancia), fBold, fBold);
            agregarFilaResumen(tResumen, "% Margen",           String.format("%.1f%%", margen), fNormal, fNormal);
            doc.add(tResumen);

            // Detalle por día (si no es "hoy")
            if (!periodo.equals("hoy")) {
                doc.add(new Paragraph("VENTAS POR DÍA", fSubtit));
                doc.add(new Chunk(new com.itextpdf.text.pdf.draw.LineSeparator()));
                doc.add(Chunk.NEWLINE);

                int diasFiltro = periodo.equals("semana") ? 7 : periodo.equals("mes") ? 30 : 365;
                List<String[]> porDia = VentaDAO.obtenerVentasPorDia(diasFiltro);

                PdfPTable tDia = new PdfPTable(3);
                tDia.setWidthPercentage(70);
                tDia.setHorizontalAlignment(Element.ALIGN_LEFT);
                tDia.setWidths(new float[]{1.5f, 1.2f, 1.2f});
                tDia.setSpacingAfter(16);

                for (String cab : new String[]{"Fecha", "Ventas", "Ganancia"}) {
                    PdfPCell c = new PdfPCell(new Phrase(cab, fHeader));
                    c.setBackgroundColor(new BaseColor(30, 90, 160));
                    c.setPadding(5); tDia.addCell(c);
                }
                for (String[] fila : porDia) {
                    tDia.addCell(new PdfPCell(new Phrase(fila[0], fCell)));
                    PdfPCell cv = new PdfPCell(new Phrase("$" + fila[1], fCell)); cv.setHorizontalAlignment(Element.ALIGN_RIGHT); tDia.addCell(cv);
                    PdfPCell cg = new PdfPCell(new Phrase("$" + fila[2], fCell)); cg.setHorizontalAlignment(Element.ALIGN_RIGHT); tDia.addCell(cg);
                }
                doc.add(tDia);
            }

            // Listado de ventas
            doc.add(new Paragraph("DETALLE DE VENTAS", fSubtit));
            doc.add(new Chunk(new com.itextpdf.text.pdf.draw.LineSeparator()));
            doc.add(Chunk.NEWLINE);

            List<String[]> ventas_lista = VentaDAO.obtenerHistorialParaExportar(periodo);

            PdfPTable tVentas = new PdfPTable(6);
            tVentas.setWidthPercentage(100);
            tVentas.setWidths(new float[]{0.5f, 1.4f, 1f, 0.8f, 0.8f, 0.8f});

            for (String cab : new String[]{"#", "Fecha", "Cliente", "Venta", "Ganancia", "Método"}) {
                PdfPCell c = new PdfPCell(new Phrase(cab, fHeader));
                c.setBackgroundColor(new BaseColor(30, 90, 160));
                c.setPadding(4); tVentas.addCell(c);
            }

            boolean alt = false;
            for (String[] v : ventas_lista) {
                BaseColor bg = alt ? new BaseColor(245, 248, 255) : BaseColor.WHITE;
                for (int i : new int[]{0, 1, 2, 4, 6, 7}) {
                    String txt = switch (i) {
                        case 4 -> "$" + v[4];
                        case 6 -> "$" + v[6];
                        default -> v[i];
                    };
                    PdfPCell c = new PdfPCell(new Phrase(txt, fCell));
                    c.setBackgroundColor(bg);
                    c.setPadding(3);
                    tVentas.addCell(c);
                }
                alt = !alt;
            }
            doc.add(tVentas);

            doc.close();
            File archivo = new File(ruta);
            abrirArchivo(archivo);
            return archivo;

        } catch (Exception e) {
            System.err.println("Error exportar PDF: " + e.getMessage());
            return null;
        }
    }

    // ─── EXPORTAR EXCEL ─────────────────────────────────────────────────────────

    public static File exportarResumenExcel(String periodo) {
        try {
            File folder = new File("reportes");
            if (!folder.exists()) folder.mkdirs();

            String ts = LocalDateTime.now().format(DateTimeFormatter.ofPattern("yyyy-MM-dd_HH-mm"));
            String ruta = "reportes/Reporte_" + periodo + "_" + ts + ".xlsx";

            XSSFWorkbook wb = new XSSFWorkbook();

            // ── Hoja 1: Resumen ──────────────────────────────────────
            XSSFSheet hResumen = wb.createSheet("Resumen");
            CellStyle estTitulo = estiloTitulo(wb);
            CellStyle estHeader = estiloHeader(wb);
            CellStyle estMonto  = estiloMonto(wb);
            CellStyle estNormal = estiloNormal(wb);

            int r = 0;
            Row rTit = hResumen.createRow(r++);
            crearCelda(rTit, 0, getNombreNegocio() + " — Reporte de Ventas", estTitulo);
            hResumen.addMergedRegion(new org.apache.poi.ss.util.CellRangeAddress(0, 0, 0, 3));

            r++;
            double[] datos = VentaDAO.obtenerVentasYGananciaPeriodo(periodo);
            double ventas   = datos[0];
            double ganancia = datos[1];
            double costo    = ventas - ganancia;
            double margen   = ventas > 0 ? (ganancia / ventas * 100) : 0;

            String[][] resumen = {
                {"Total Ventas",     fmt(ventas)},
                {"Costo Mercadería", fmt(costo)},
                {"Ganancia Bruta",   fmt(ganancia)},
                {"Margen %",         String.format("%.1f%%", margen)}
            };
            for (String[] fila : resumen) {
                Row row = hResumen.createRow(r++);
                crearCelda(row, 0, fila[0], estHeader);
                crearCelda(row, 1, fila[1], estMonto);
            }

            hResumen.setColumnWidth(0, 6000);
            hResumen.setColumnWidth(1, 4000);

            // ── Hoja 2: Ventas por día ───────────────────────────────
            if (!periodo.equals("hoy")) {
                XSSFSheet hDias = wb.createSheet("Por Día");
                Row hd = hDias.createRow(0);
                for (int i = 0; i < 3; i++) {
                    String[] hdrs = {"Fecha", "Ventas ($)", "Ganancia ($)"};
                    crearCelda(hd, i, hdrs[i], estHeader);
                }
                int diasFiltro = periodo.equals("semana") ? 7 : periodo.equals("mes") ? 30 : 365;
                List<String[]> porDia = VentaDAO.obtenerVentasPorDia(diasFiltro);
                int rd = 1;
                for (String[] fila : porDia) {
                    Row row = hDias.createRow(rd++);
                    crearCelda(row, 0, fila[0], estNormal);
                    crearCelda(row, 1, fila[1], estMonto);
                    crearCelda(row, 2, fila[2], estMonto);
                }
                for (int i = 0; i < 3; i++) hDias.autoSizeColumn(i);
            }

            // ── Hoja 3: Detalle de ventas ────────────────────────────
            XSSFSheet hDetalle = wb.createSheet("Ventas");
            Row hdr = hDetalle.createRow(0);
            String[] cols = {"#", "Fecha", "Cliente", "Detalle", "Venta ($)", "Costo ($)", "Ganancia ($)", "Método", "Cajero"};
            for (int i = 0; i < cols.length; i++) crearCelda(hdr, i, cols[i], estHeader);

            List<String[]> ventas_lista = VentaDAO.obtenerHistorialParaExportar(periodo);
            int rv = 1;
            for (String[] v : ventas_lista) {
                Row row = hDetalle.createRow(rv++);
                for (int i = 0; i < v.length; i++) {
                    CellStyle st = (i == 4 || i == 5 || i == 6) ? estMonto : estNormal;
                    crearCelda(row, i, v[i], st);
                }
            }
            for (int i = 0; i < cols.length; i++) hDetalle.autoSizeColumn(i);

            try (FileOutputStream fos = new FileOutputStream(ruta)) { wb.write(fos); }
            wb.close();

            File archivo = new File(ruta);
            abrirArchivo(archivo);
            return archivo;

        } catch (Exception e) {
            System.err.println("Error exportar Excel: " + e.getMessage());
            return null;
        }
    }

    // ─── Helpers ────────────────────────────────────────────────────────────────

    private static void agregarFilaResumen(PdfPTable t, String label, String valor,
                                           com.itextpdf.text.Font fL, com.itextpdf.text.Font fV) {
        PdfPCell c1 = new PdfPCell(new Phrase(label, fL)); c1.setBorder(Rectangle.NO_BORDER); c1.setPadding(4);
        PdfPCell c2 = new PdfPCell(new Phrase(valor, fV)); c2.setBorder(Rectangle.NO_BORDER); c2.setPadding(4);
        c2.setHorizontalAlignment(Element.ALIGN_RIGHT);
        t.addCell(c1); t.addCell(c2);
    }

    private static String fmt(double v) { return String.format("%.2f", v); }

    private static void crearCelda(Row row, int col, String val, CellStyle st) {
        Cell c = row.createCell(col);
        c.setCellValue(val);
        if (st != null) c.setCellStyle(st);
    }

    private static CellStyle estiloTitulo(Workbook wb) {
        CellStyle s = wb.createCellStyle();
        Font f = wb.createFont(); f.setBold(true); f.setFontHeightInPoints((short)14);
        s.setFont(f); return s;
    }
    private static CellStyle estiloHeader(Workbook wb) {
        CellStyle s = wb.createCellStyle();
        Font f = wb.createFont(); f.setBold(true);
        s.setFont(f);
        s.setFillForegroundColor(IndexedColors.LIGHT_CORNFLOWER_BLUE.getIndex());
        s.setFillPattern(FillPatternType.SOLID_FOREGROUND);
        return s;
    }
    private static CellStyle estiloMonto(Workbook wb) {
        CellStyle s = wb.createCellStyle();
        s.setAlignment(HorizontalAlignment.RIGHT);
        return s;
    }
    private static CellStyle estiloNormal(Workbook wb) { return wb.createCellStyle(); }

    private static void abrirArchivo(File f) {
        if (!f.exists()) return;
        try { new ProcessBuilder("rundll32", "url.dll,FileProtocolHandler", f.getAbsolutePath()).start(); }
        catch (Exception e) {
            try { if (Desktop.isDesktopSupported()) Desktop.getDesktop().open(f); }
            catch (Exception ex) { System.err.println("No se pudo abrir: " + ex.getMessage()); }
        }
    }
}
