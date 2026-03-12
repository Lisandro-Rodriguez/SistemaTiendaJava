package com.tienda.vista;

import com.tienda.db.ProductoDAO;
import com.tienda.db.VentaDAO;
import com.tienda.util.ExportadorReporte;
import javax.swing.*;
import javax.swing.border.EmptyBorder;
import javax.swing.table.*;
import java.awt.*;

public class PanelDashboard extends JPanel {

    private final JTabbedPane pestanas;
    private VentanaProducto ventanaPrincipal;

    public PanelDashboard(JTabbedPane pestanas, VentanaProducto ventana) {
        this.pestanas = pestanas;
        this.ventanaPrincipal = ventana;
        setLayout(new BorderLayout(0, 0));
        setBorder(new EmptyBorder(0, 0, 0, 0));
        construir();
    }

    public PanelDashboard(JTabbedPane pestanas, TableRowSorter<DefaultTableModel> sorter) {
        this.pestanas = pestanas;
        this.ventanaPrincipal = null;
        setLayout(new BorderLayout(0, 0));
        construir();
    }

    private void construir() {
        removeAll();
        // Pestañas internas: Resumen + Reportes
        JTabbedPane tabs = new JTabbedPane();
        tabs.addTab("📊 Resumen", construirPanelResumen());
        tabs.addTab("📤 Exportar Reportes", construirPanelReportes());
        add(tabs, BorderLayout.CENTER);
        revalidate(); repaint();
    }

    // ── Panel resumen (tarjetas) ─────────────────────────────────────────────

    private JPanel construirPanelResumen() {
        JPanel panel = new JPanel(new BorderLayout(20, 20));
        panel.setBorder(new EmptyBorder(30, 40, 30, 40));
        panel.setOpaque(false);

        JLabel lblTitulo = new JLabel("📊 Resumen del Negocio", SwingConstants.CENTER);
        lblTitulo.setFont(new Font("SansSerif", Font.BOLD, 26));
        lblTitulo.setForeground(new Color(30, 90, 160));
        lblTitulo.setBorder(new EmptyBorder(0, 0, 10, 0));
        panel.add(lblTitulo, BorderLayout.NORTH);

        // Fila superior: 3 tarjetas de inventario/ventas
        JPanel filaSuperior = new JPanel(new GridLayout(1, 3, 20, 0));
        filaSuperior.setOpaque(false);

        filaSuperior.add(crearTarjeta("💰 Ventas Históricas",
                "$" + fmt(VentaDAO.obtenerTotalVentasHistorico()),
                "Total ingresos registrados",
                new Color(39, 174, 96), new Color(220, 255, 235), null));

        filaSuperior.add(crearTarjeta("📦 Productos en Stock",
                String.valueOf(ProductoDAO.obtenerTotalProductosRegistrados()),
                "Total productos cargados",
                new Color(30, 90, 160), new Color(220, 235, 255), null));

        int bajoStock = ProductoDAO.obtenerProductosBajoStock(5);
        Runnable accionStock = bajoStock > 0 ? () -> {
            for (int i = 0; i < pestanas.getTabCount(); i++) {
                if (pestanas.getTitleAt(i).contains("Inventario")) { pestanas.setSelectedIndex(i); break; }
            }
            if (ventanaPrincipal != null) {
                TableRowSorter<DefaultTableModel> s = ventanaPrincipal.getSorterInventario();
                if (s != null) s.setRowFilter(RowFilter.numberFilter(RowFilter.ComparisonType.BEFORE, 6, 6));
            }
        } : null;

        filaSuperior.add(crearTarjeta("⚠️ Alertas Stock Bajo",
                String.valueOf(bajoStock),
                bajoStock > 0 ? "Clic para ver cuáles son" : "Todo el inventario OK",
                new Color(220, 53, 69), new Color(255, 235, 238), accionStock));

        // Fila inferior: tarjetas de ganancias del período
        JPanel filaInferior = new JPanel(new GridLayout(1, 3, 20, 0));
        filaInferior.setOpaque(false);

        double[] hoy    = VentaDAO.obtenerVentasYGananciaPeriodo("hoy");
        double[] semana = VentaDAO.obtenerVentasYGananciaPeriodo("semana");
        double[] mes    = VentaDAO.obtenerVentasYGananciaPeriodo("mes");

        filaInferior.add(crearTarjetaGanancia("📅 Ganancia Hoy",     hoy[1],    hoy[0]));
        filaInferior.add(crearTarjetaGanancia("📆 Ganancia Semana",  semana[1], semana[0]));
        filaInferior.add(crearTarjetaGanancia("🗓️ Ganancia 30 días", mes[1],    mes[0]));

        JPanel centro = new JPanel(new GridLayout(2, 1, 0, 20));
        centro.setOpaque(false);
        centro.add(filaSuperior);
        centro.add(filaInferior);
        panel.add(centro, BorderLayout.CENTER);

        JButton btnRefrescar = new JButton("↻  Actualizar");
        btnRefrescar.setFont(new Font("SansSerif", Font.BOLD, 13));
        btnRefrescar.setBackground(new Color(30, 90, 160));
        btnRefrescar.setForeground(Color.WHITE);
        btnRefrescar.setFocusPainted(false);
        btnRefrescar.setBorderPainted(false);
        btnRefrescar.setCursor(Cursor.getPredefinedCursor(Cursor.HAND_CURSOR));
        btnRefrescar.setPreferredSize(new Dimension(200, 40));
        btnRefrescar.addActionListener(e -> construir());

        JPanel sur = new JPanel(); sur.setOpaque(false); sur.add(btnRefrescar);
        panel.add(sur, BorderLayout.SOUTH);

        return panel;
    }

    // ── Panel de exportación ────────────────────────────────────────────────

    private JPanel construirPanelReportes() {
        JPanel panel = new JPanel(new BorderLayout(20, 20));
        panel.setBorder(new EmptyBorder(30, 50, 30, 50));

        JLabel lbl = new JLabel("📤 Exportar Reportes de Ventas", SwingConstants.CENTER);
        lbl.setFont(new Font("SansSerif", Font.BOLD, 22));
        lbl.setForeground(new Color(30, 90, 160));
        panel.add(lbl, BorderLayout.NORTH);

        JPanel centro = new JPanel(new GridLayout(4, 1, 0, 16));
        centro.setBorder(new EmptyBorder(20, 60, 20, 60));

        String[] periodos = {"hoy", "semana", "mes", "historico"};
        String[] labels   = {"Hoy", "Últimos 7 días", "Últimos 30 días", "Histórico completo"};

        for (int i = 0; i < periodos.length; i++) {
            final String periodo = periodos[i];
            final String label   = labels[i];

            JPanel fila = new JPanel(new FlowLayout(FlowLayout.CENTER, 16, 0));
            fila.setOpaque(false);

            JLabel lblP = new JLabel(label);
            lblP.setFont(new Font("SansSerif", Font.BOLD, 14));
            lblP.setPreferredSize(new Dimension(180, 36));

            JButton btnPDF   = botonExportar("📄 PDF",   new Color(192, 57, 43));
            JButton btnExcel = botonExportar("📊 Excel", new Color(39, 174, 96));

            btnPDF.addActionListener(e -> {
                btnPDF.setText("Generando...");
                btnPDF.setEnabled(false);
                SwingWorker<Void, Void> w = new SwingWorker<>() {
                    @Override protected Void doInBackground() {
                        ExportadorReporte.exportarResumenPDF(periodo); return null;
                    }
                    @Override protected void done() {
                        btnPDF.setText("📄 PDF"); btnPDF.setEnabled(true);
                    }
                };
                w.execute();
            });

            btnExcel.addActionListener(e -> {
                btnExcel.setText("Generando...");
                btnExcel.setEnabled(false);
                SwingWorker<Void, Void> w = new SwingWorker<>() {
                    @Override protected Void doInBackground() {
                        ExportadorReporte.exportarResumenExcel(periodo); return null;
                    }
                    @Override protected void done() {
                        btnExcel.setText("📊 Excel"); btnExcel.setEnabled(true);
                    }
                };
                w.execute();
            });

            fila.add(lblP); fila.add(btnPDF); fila.add(btnExcel);
            centro.add(fila);
        }

        JLabel nota = new JLabel("Los archivos se guardan en la carpeta 'reportes/' del proyecto.", SwingConstants.CENTER);
        nota.setFont(new Font("SansSerif", Font.ITALIC, 11));
        nota.setForeground(new Color(120, 120, 120));

        panel.add(centro, BorderLayout.CENTER);
        panel.add(nota, BorderLayout.SOUTH);
        return panel;
    }

    // ── Helpers de tarjetas ─────────────────────────────────────────────────

    private JPanel crearTarjeta(String titulo, String valor, String subtitulo,
                                 Color colorBorde, Color colorFondo, Runnable onClick) {
        JPanel card = new JPanel(new GridLayout(3, 1, 0, 6));
        card.setBackground(colorFondo);
        card.setBorder(BorderFactory.createCompoundBorder(
                BorderFactory.createLineBorder(colorBorde, 3, true),
                new EmptyBorder(20, 20, 20, 20)));
        if (onClick != null) card.setCursor(Cursor.getPredefinedCursor(Cursor.HAND_CURSOR));

        JLabel lblTit = new JLabel(titulo, SwingConstants.CENTER);
        lblTit.setFont(new Font("SansSerif", Font.BOLD, 14));
        lblTit.setForeground(colorBorde.darker());

        JLabel lblVal = new JLabel(valor, SwingConstants.CENTER);
        lblVal.setFont(new Font("SansSerif", Font.BOLD, 38));
        lblVal.setForeground(colorBorde);

        JLabel lblSub = new JLabel(subtitulo, SwingConstants.CENTER);
        lblSub.setFont(new Font("SansSerif", Font.PLAIN, 11));
        lblSub.setForeground(new Color(100, 110, 130));

        card.add(lblTit); card.add(lblVal); card.add(lblSub);

        if (onClick != null) {
            card.addMouseListener(new java.awt.event.MouseAdapter() {
                @Override public void mouseClicked(java.awt.event.MouseEvent e) { onClick.run(); }
                @Override public void mouseEntered(java.awt.event.MouseEvent e) { card.setBackground(colorFondo.darker()); }
                @Override public void mouseExited(java.awt.event.MouseEvent e)  { card.setBackground(colorFondo); }
            });
        }
        return card;
    }

    private JPanel crearTarjetaGanancia(String titulo, double ganancia, double ventas) {
        double margen = ventas > 0 ? (ganancia / ventas * 100) : 0;
        boolean positiva = ganancia >= 0;
        Color colorBorde = positiva ? new Color(155, 89, 182) : new Color(220, 53, 69);
        Color colorFondo = positiva ? new Color(245, 235, 255) : new Color(255, 235, 238);
        String sub = String.format("Margen: %.1f%% sobre $%s en ventas", margen, fmt(ventas));
        return crearTarjeta(titulo, "$" + fmt(ganancia), sub, colorBorde, colorFondo, null);
    }

    private JButton botonExportar(String texto, Color color) {
        JButton btn = new JButton(texto);
        btn.setFont(new Font("SansSerif", Font.BOLD, 13));
        btn.setBackground(color);
        btn.setForeground(Color.WHITE);
        btn.setFocusPainted(false);
        btn.setBorderPainted(false);
        btn.setCursor(Cursor.getPredefinedCursor(Cursor.HAND_CURSOR));
        btn.setPreferredSize(new Dimension(130, 36));
        return btn;
    }

    private static String fmt(double v) { return String.format("%.2f", v); }
}
