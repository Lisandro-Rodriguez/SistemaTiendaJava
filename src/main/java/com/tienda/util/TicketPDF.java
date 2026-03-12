package com.tienda.util;

import java.awt.Desktop;
import java.io.File;
import java.io.FileOutputStream;
import java.util.List;
import java.util.prefs.Preferences;

import com.itextpdf.text.BaseColor;
import com.itextpdf.text.Chunk;
import com.itextpdf.text.Document;
import com.itextpdf.text.Element;
import com.itextpdf.text.Font;
import com.itextpdf.text.Paragraph;
import com.itextpdf.text.Phrase;
import com.itextpdf.text.Rectangle;
import com.itextpdf.text.pdf.PdfPCell;
import com.itextpdf.text.pdf.PdfPTable;
import com.itextpdf.text.pdf.PdfWriter;
import com.itextpdf.text.pdf.draw.LineSeparator;

public class TicketPDF {

    // Datos del negocio — se leen desde preferencias (configurables desde la app)
    private static String getNombreNegocio() {
        return Preferences.userRoot().node("tienda").get("nombre_negocio", "MI KIOSCO");
    }
    private static String getDireccion() {
        return Preferences.userRoot().node("tienda").get("direccion", "");
    }
    private static String getCuit() {
        return Preferences.userRoot().node("tienda").get("cuit", "");
    }
    private static String getTelefono() {
        return Preferences.userRoot().node("tienda").get("telefono", "");
    }
    private static String getMensaje() {
        return Preferences.userRoot().node("tienda").get("mensaje_ticket", "Gracias por su compra!");
    }

    public static void generarTicket(String nroTicket, String fecha, String cliente, String cajero,
                                     List<String[]> productos, double total, String metodo) {
        generarTicket(nroTicket, fecha, cliente, cajero, productos, total, metodo, 0, false);
    }

    public static void generarTicket(String nroTicket, String fecha, String cliente, String cajero,
                                     List<String[]> productos, double total, String metodo,
                                     double vuelto, boolean esFiado) {
        try {
            File folder = new File("tickets");
            if (!folder.exists()) folder.mkdirs();

            String rutaDestino = "tickets/Ticket_" + nroTicket + ".pdf";

            // Ticket 80mm de ancho (226pt), altura dinámica según productos
            float alturaBase = 420;
            float alturaPorProducto = 18;
            float altura = alturaBase + (productos.size() * alturaPorProducto);
            Document doc = new Document(new Rectangle(226, altura));
            doc.setMargins(12, 12, 12, 12);

            PdfWriter writer = PdfWriter.getInstance(doc, new FileOutputStream(rutaDestino));
            doc.open();

            // ─── Fuentes ───────────────────────────────────────────
            Font fBold14  = new Font(Font.FontFamily.COURIER, 11, Font.BOLD);
            Font fBold10  = new Font(Font.FontFamily.COURIER, 8, Font.BOLD);
            Font fNormal8 = new Font(Font.FontFamily.COURIER, 7, Font.NORMAL);
            Font fSmall   = new Font(Font.FontFamily.COURIER, 6, Font.NORMAL);
            Font fTotal   = new Font(Font.FontFamily.COURIER, 10, Font.BOLD);
            BaseColor gris = new BaseColor(120, 120, 120);

            // ─── Encabezado ─────────────────────────────────────────
            Paragraph titulo = new Paragraph(getNombreNegocio(), fBold14);
            titulo.setAlignment(Element.ALIGN_CENTER);
            titulo.setSpacingAfter(2);
            doc.add(titulo);

            String dir = getDireccion();
            if (!dir.isBlank()) {
                Paragraph pDir = new Paragraph(dir, fSmall);
                pDir.setAlignment(Element.ALIGN_CENTER);
                doc.add(pDir);
            }
            String cuit = getCuit();
            if (!cuit.isBlank()) {
                Paragraph pCuit = new Paragraph("CUIT: " + cuit, fSmall);
                pCuit.setAlignment(Element.ALIGN_CENTER);
                doc.add(pCuit);
            }
            String tel = getTelefono();
            if (!tel.isBlank()) {
                Paragraph pTel = new Paragraph("Tel: " + tel, fSmall);
                pTel.setAlignment(Element.ALIGN_CENTER);
                doc.add(pTel);
            }

            doc.add(new Chunk(new LineSeparator(0.5f, 100, BaseColor.BLACK, Element.ALIGN_CENTER, -2)));

            // ─── Datos del comprobante ──────────────────────────────
            String tipoComp = esFiado ? "COMPROBANTE FIADO" : "TICKET DE VENTA";
            Paragraph pTipo = new Paragraph(tipoComp, fBold10);
            pTipo.setAlignment(Element.ALIGN_CENTER);
            pTipo.setSpacingBefore(3);
            pTipo.setSpacingAfter(3);
            doc.add(pTipo);

            PdfPTable tblInfo = new PdfPTable(2);
            tblInfo.setWidthPercentage(100);
            tblInfo.setWidths(new float[]{1f, 1.6f});
            tblInfo.getDefaultCell().setBorder(Rectangle.NO_BORDER);

            agregarCeldaInfo(tblInfo, "Ticket N°:", nroTicket, fNormal8);
            agregarCeldaInfo(tblInfo, "Fecha:", fecha, fNormal8);
            agregarCeldaInfo(tblInfo, "Cajero:", cajero, fNormal8);
            if (!cliente.equals("-") && !cliente.isBlank())
                agregarCeldaInfo(tblInfo, "Cliente:", cliente, fNormal8);
            agregarCeldaInfo(tblInfo, "Pago:", metodo, fNormal8);
            doc.add(tblInfo);

            doc.add(new Chunk(new LineSeparator(0.5f, 100, BaseColor.BLACK, Element.ALIGN_CENTER, -2)));

            // ─── Encabezado tabla productos ─────────────────────────
            Paragraph pEncCols = new Paragraph("CANT  DESCRIPCION             P.UNIT   SUBTOT", fSmall);
            pEncCols.setSpacingBefore(3);
            doc.add(pEncCols);
            doc.add(new Chunk(new LineSeparator(0.3f, 100, gris, Element.ALIGN_CENTER, -1)));

            // ─── Productos ──────────────────────────────────────────
            for (String[] p : productos) {
                // p[0]=codigo, p[1]=nombre, p[2]=tipo, p[3]=cantidad, p[4]=subtotal, p[5]=precio_unitario
                int cant = 1;
                double subtotal = 0, precioUnit = 0;
                try { cant = Integer.parseInt(p[3]); } catch (Exception e) {}
                try { subtotal = Double.parseDouble(p[4].replace("$","").replace(",",".")); } catch (Exception e) {}
                try {
                    if (p.length > 5) precioUnit = Double.parseDouble(p[5].replace("$","").replace(",","."));
                    else precioUnit = subtotal / Math.max(1, cant);
                } catch (Exception e) { precioUnit = subtotal / Math.max(1, cant); }

                String nombre = p[1].length() > 22 ? p[1].substring(0, 22) : p[1];
                String linea = String.format("%-4d  %-22s %6.2f %7.2f", cant, nombre, precioUnit, subtotal);
                doc.add(new Paragraph(linea, fNormal8));
            }

            doc.add(new Chunk(new LineSeparator(0.5f, 100, BaseColor.BLACK, Element.ALIGN_CENTER, -2)));

            // ─── Totales ────────────────────────────────────────────
            PdfPTable tblTotal = new PdfPTable(2);
            tblTotal.setWidthPercentage(100);
            tblTotal.setWidths(new float[]{1.5f, 1f});
            tblTotal.getDefaultCell().setBorder(Rectangle.NO_BORDER);

            PdfPCell cLblTotal = new PdfPCell(new Phrase("TOTAL:", fTotal));
            cLblTotal.setBorder(Rectangle.NO_BORDER);
            cLblTotal.setHorizontalAlignment(Element.ALIGN_LEFT);
            PdfPCell cValTotal = new PdfPCell(new Phrase("$" + String.format("%.2f", total), fTotal));
            cValTotal.setBorder(Rectangle.NO_BORDER);
            cValTotal.setHorizontalAlignment(Element.ALIGN_RIGHT);
            tblTotal.addCell(cLblTotal);
            tblTotal.addCell(cValTotal);
            doc.add(tblTotal);

            if (!esFiado && vuelto > 0) {
                Paragraph pVuelto = new Paragraph("Vuelto: $" + String.format("%.2f", vuelto), fNormal8);
                pVuelto.setAlignment(Element.ALIGN_RIGHT);
                doc.add(pVuelto);
            }
            if (esFiado) {
                doc.add(new Chunk(new LineSeparator(0.5f, 100, BaseColor.BLACK, Element.ALIGN_CENTER, -2)));
                Paragraph pFiado = new Paragraph("** COMPRA A CUENTA CORRIENTE **", fBold10);
                pFiado.setAlignment(Element.ALIGN_CENTER);
                doc.add(pFiado);
            }

            doc.add(new Chunk(new LineSeparator(0.5f, 100, BaseColor.BLACK, Element.ALIGN_CENTER, -2)));

            Paragraph gracias = new Paragraph("\n" + getMensaje(), fNormal8);
            gracias.setAlignment(Element.ALIGN_CENTER);
            doc.add(gracias);

            Paragraph nota = new Paragraph("No válido como factura fiscal", fSmall);
            nota.setAlignment(Element.ALIGN_CENTER);
            nota.getFont().setColor(gris);
            doc.add(nota);

            doc.close();
            abrirPDF(new File(rutaDestino));

        } catch (Exception e) {
            System.err.println("Error generando ticket PDF: " + e.getMessage());
        }
    }

    private static void agregarCeldaInfo(PdfPTable t, String label, String valor, Font f) {
        PdfPCell c1 = new PdfPCell(new Phrase(label, f));
        c1.setBorder(Rectangle.NO_BORDER);
        PdfPCell c2 = new PdfPCell(new Phrase(valor, f));
        c2.setBorder(Rectangle.NO_BORDER);
        t.addCell(c1); t.addCell(c2);
    }

    private static void abrirPDF(File archivo) {
        if (!archivo.exists()) return;
        try {
            new ProcessBuilder("rundll32", "url.dll,FileProtocolHandler", archivo.getAbsolutePath()).start();
        } catch (Exception e) {
            try { if (Desktop.isDesktopSupported()) Desktop.getDesktop().open(archivo); }
            catch (Exception ex) { System.err.println("No se pudo abrir el PDF: " + ex.getMessage()); }
        }
    }
}