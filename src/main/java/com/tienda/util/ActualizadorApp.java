package com.tienda.util;

import javax.swing.*;
import java.awt.*;
import java.io.*;
import java.net.*;
import java.nio.file.*;

/**
 * Verifica si hay una versión nueva en GitHub Releases y notifica al usuario.
 * Repositorio: https://github.com/TU_USUARIO/SistemaTiendaJava
 * Para publicar una actualización: crear un Release en GitHub con tag "v1.1.0"
 * y subir el JAR como asset.
 */
public class ActualizadorApp {

    // ─── CONFIGURAR ESTO CON TU USUARIO DE GITHUB ────────────────────────────
    private static final String GITHUB_USER = "Lisandro-Rodriguez";
    private static final String GITHUB_REPO = "SistemaTiendaJava";
    // ─────────────────────────────────────────────────────────────────────────

    public static final String VERSION_ACTUAL = "1.0.0";

    private static final String API_URL =
        "https://api.github.com/repos/" + GITHUB_USER + "/" + GITHUB_REPO + "/releases/latest";

    /**
     * Verifica actualizaciones en segundo plano.
     * Si hay versión nueva muestra un diálogo no bloqueante.
     */
    public static void verificarEnSegundoPlano(java.awt.Window parent) {
        new Thread(() -> {
            try {
                String[] resultado = obtenerUltimaVersion();
                if (resultado == null) return;

                String versionRemota = resultado[0];
                String urlDescarga   = resultado[1];
                String urlRelease    = resultado[2];

                if (esVersionMayor(versionRemota, VERSION_ACTUAL)) {
                    SwingUtilities.invokeLater(() ->
                        mostrarDialogoActualizacion(parent, versionRemota, urlDescarga, urlRelease));
                }
            } catch (Exception e) {
                // Silencioso — no molestar al usuario si no hay internet
            }
        }, "actualizador").start();
    }

    private static String[] obtenerUltimaVersion() throws Exception {
        HttpURLConnection conn = (HttpURLConnection)
            URI.create(API_URL).toURL().openConnection();
        conn.setRequestProperty("Accept", "application/vnd.github+json");
        conn.setConnectTimeout(5000);
        conn.setReadTimeout(5000);

        if (conn.getResponseCode() != 200) return null;

        StringBuilder sb = new StringBuilder();
        try (BufferedReader br = new BufferedReader(
                new InputStreamReader(conn.getInputStream()))) {
            String line;
            while ((line = br.readLine()) != null) sb.append(line);
        }

        String json = sb.toString();

        // Parseo simple sin dependencias extra
        String version   = extraerCampo(json, "tag_name");
        String urlRelease = extraerCampo(json, "html_url");

        // Buscar primer asset .jar
        String urlDescarga = urlRelease; // fallback a la página del release
        int assetsIdx = json.indexOf("\"browser_download_url\"");
        if (assetsIdx != -1) {
            int start = json.indexOf("\"", assetsIdx + 23) + 1;
            int end   = json.indexOf("\"", start);
            if (start > 0 && end > start) urlDescarga = json.substring(start, end);
        }

        if (version == null) return null;
        // Normalizar: "v1.1.0" → "1.1.0"
        if (version.startsWith("v")) version = version.substring(1);

        return new String[]{version, urlDescarga, urlRelease};
    }

    private static void mostrarDialogoActualizacion(java.awt.Window parent,
            String versionNueva, String urlDescarga, String urlRelease) {

        JDialog dlg = new JDialog((Frame) null, "Nueva version disponible", false);
        dlg.setLayout(new BorderLayout(16, 16));
        dlg.getRootPane().setBorder(BorderFactory.createEmptyBorder(20, 24, 16, 24));

        JLabel lblTitulo = new JLabel("Nueva version disponible: v" + versionNueva);
        lblTitulo.setFont(new Font("SansSerif", Font.BOLD, 15));

        JLabel lblActual = new JLabel("Version actual: v" + VERSION_ACTUAL);
        lblActual.setFont(new Font("SansSerif", Font.PLAIN, 12));
        lblActual.setForeground(new Color(100, 100, 100));

        JPanel panelTexto = new JPanel(new GridLayout(2, 1, 0, 4));
        panelTexto.setOpaque(false);
        panelTexto.add(lblTitulo);
        panelTexto.add(lblActual);

        JPanel panelBotones = new JPanel(new FlowLayout(FlowLayout.RIGHT, 8, 0));
        panelBotones.setOpaque(false);

        JButton btnDescargar = new JButton("Descargar actualizacion");
        btnDescargar.setBackground(new Color(30, 90, 160));
        btnDescargar.setForeground(Color.WHITE);
        btnDescargar.setFocusPainted(false);
        btnDescargar.setBorderPainted(false);
        btnDescargar.addActionListener(e -> {
            try {
                Desktop.getDesktop().browse(URI.create(urlRelease));
            } catch (Exception ex) {
                JOptionPane.showMessageDialog(dlg,
                    "Abre este link en tu navegador:\n" + urlRelease);
            }
            dlg.dispose();
        });

        JButton btnLuego = new JButton("Ahora no");
        btnLuego.addActionListener(e -> dlg.dispose());

        panelBotones.add(btnLuego);
        panelBotones.add(btnDescargar);

        dlg.add(panelTexto, BorderLayout.CENTER);
        dlg.add(panelBotones, BorderLayout.SOUTH);
        dlg.pack();
        dlg.setMinimumSize(new Dimension(380, 130));
        if (parent != null) dlg.setLocationRelativeTo(parent);
        else dlg.setLocationRelativeTo(null);
        dlg.setVisible(true);
    }

    // ── Helpers ──────────────────────────────────────────────────────────────

    private static String extraerCampo(String json, String campo) {
        String key = "\"" + campo + "\"";
        int idx = json.indexOf(key);
        if (idx == -1) return null;
        int start = json.indexOf("\"", idx + key.length() + 1) + 1;
        int end   = json.indexOf("\"", start);
        if (start <= 0 || end <= start) return null;
        return json.substring(start, end);
    }

    /** Compara versiones semánticas: "1.1.0" > "1.0.0" → true */
    static boolean esVersionMayor(String remota, String actual) {
        try {
            int[] r = parsear(remota);
            int[] a = parsear(actual);
            for (int i = 0; i < 3; i++) {
                if (r[i] > a[i]) return true;
                if (r[i] < a[i]) return false;
            }
        } catch (Exception e) { /* ignorar */ }
        return false;
    }

    private static int[] parsear(String v) {
        String[] p = v.split("\\.");
        int[] r = new int[3];
        for (int i = 0; i < Math.min(3, p.length); i++)
            r[i] = Integer.parseInt(p[i].trim());
        return r;
    }
}
