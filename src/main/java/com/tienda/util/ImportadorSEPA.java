package com.tienda.util;

import java.io.BufferedReader;
import java.io.FileInputStream;
import java.io.IOException;
import java.io.InputStreamReader;
import java.nio.charset.StandardCharsets;
import java.util.Arrays;
import java.util.HashSet;
import java.util.List;
import java.util.Set;

import com.tienda.db.CatalogoDAO;
import com.tienda.db.ProductoDAO;
import com.tienda.Modelo.Producto;

public class ImportadorSEPA {

    private static final List<String> PALABRAS_CLAVE = Arrays.asList(
        "coca", "pepsi", "sprite", "fanta", "7up", "manaos", "cunnington",
        "agua", "jugo", "yogur", "leche", "mantec", "queso", "fiambre",
        "gallet", "alfajor", "chocol", "caramel", "chicle", "caramelo",
        "papa", "snack", "maiz", "cereal", "arroz", "fideos", "harina",
        "aceite", "azucar", "sal", "vinagre", "mayones", "ketchup",
        "cerveza", "vino", "fernet", "whisky", "vodka", "gin",
        "cigarr", "tabaco",
        "jabon", "shampoo", "desodor", "dentifric", "papel", "servillet",
        "detergente", "lavandina", "suaviz",
        "pila", "fosforo", "encendedor",
        "cafe", "te ", "mate", "yerba", "tostado"
    );

    public static class ResultadoImportacion {
        public int importados = 0;
        public int duplicados = 0;
        public int errores = 0;
    }

    /**
     * Importa al CATÁLOGO (tabla separada), no al inventario activo.
     * Los productos del catálogo solo sirven para autocompletar al registrar.
     */
    public static ResultadoImportacion importar(String rutaArchivo, double margenGanancia) {
        ResultadoImportacion resultado = new ResultadoImportacion();
        Set<String> codigosVistos = new HashSet<>();

        try (BufferedReader br = new BufferedReader(
                new InputStreamReader(new FileInputStream(rutaArchivo), StandardCharsets.UTF_8))) {

            String linea = br.readLine();
            if (linea == null) return resultado;

            String[] headers = linea.split("\\|");
            int idxEan   = indexOf(headers, "id_producto");
            int idxDesc  = indexOf(headers, "productos_descripcion");
            int idxMarca = indexOf(headers, "productos_marca");
            int idxPrecio = indexOf(headers, "productos_precio_lista");

            if (idxEan == -1 || idxDesc == -1) {
                System.err.println("Formato SEPA no reconocido.");
                return resultado;
            }

            while ((linea = br.readLine()) != null) {
                try {
                    String[] cols = linea.split("\\|");
                    if (cols.length <= idxDesc) continue;

                    String ean  = cols[idxEan].trim();
                    String desc = cols[idxDesc].trim();
                    String marca = idxMarca < cols.length ? cols[idxMarca].trim() : "-";
                    String precioStr = idxPrecio < cols.length ? cols[idxPrecio].trim() : "0";

                    if (ean.isEmpty() || ean.equals("0") || ean.length() < 7) continue;
                    if (codigosVistos.contains(ean)) { resultado.duplicados++; continue; }
                    codigosVistos.add(ean);

                    if (!esRelevante(desc)) continue;

                    double precio = 0;
                    try { precio = Double.parseDouble(precioStr.replace(",", ".")); } catch (Exception e) {}

                    String tipo = inferirTipo(desc);

                    // Guardar en catálogo (no en inventario)
                    CatalogoDAO.registrarEnCatalogo(ean, capitalizar(desc), capitalizar(marca), tipo, precio);
                    resultado.importados++;

                } catch (Exception e) {
                    resultado.errores++;
                }
            }

        } catch (IOException e) {
            System.err.println("Error leyendo archivo: " + e.getMessage());
        }

        return resultado;
    }

    /**
     * Importar directamente al inventario activo (con stock y margen).
     * Para cuando el usuario explícitamente quiere agregar todo al stock.
     */
    public static ResultadoImportacion importarAlInventario(String rutaArchivo, double margenGanancia) {
        ResultadoImportacion resultado = new ResultadoImportacion();
        Set<String> codigosVistos = new HashSet<>();

        try (BufferedReader br = new BufferedReader(
                new InputStreamReader(new FileInputStream(rutaArchivo), StandardCharsets.UTF_8))) {

            String linea = br.readLine();
            if (linea == null) return resultado;

            String[] headers = linea.split("\\|");
            int idxEan    = indexOf(headers, "id_producto");
            int idxDesc   = indexOf(headers, "productos_descripcion");
            int idxMarca  = indexOf(headers, "productos_marca");
            int idxPrecio = indexOf(headers, "productos_precio_lista");

            if (idxEan == -1 || idxDesc == -1) return resultado;

            while ((linea = br.readLine()) != null) {
                try {
                    String[] cols = linea.split("\\|");
                    if (cols.length <= idxDesc) continue;

                    String ean   = cols[idxEan].trim();
                    String desc  = cols[idxDesc].trim();
                    String marca = idxMarca < cols.length ? cols[idxMarca].trim() : "-";
                    String precioStr = idxPrecio < cols.length ? cols[idxPrecio].trim() : "0";

                    if (ean.isEmpty() || ean.equals("0") || ean.length() < 7) continue;
                    if (codigosVistos.contains(ean)) { resultado.duplicados++; continue; }
                    codigosVistos.add(ean);
                    if (ProductoDAO.buscarPorCodigo(ean) != null) { resultado.duplicados++; continue; }
                    if (!esRelevante(desc)) continue;

                    double precio = 0;
                    try { precio = Double.parseDouble(precioStr.replace(",", ".")); } catch (Exception e) {}

                    ProductoDAO.registrarProducto(new Producto(
                            ean, capitalizar(desc), inferirTipo(desc),
                            capitalizar(marca), precio, 0, margenGanancia));
                    resultado.importados++;

                } catch (Exception e) {
                    resultado.errores++;
                }
            }
        } catch (IOException e) {
            System.err.println("Error: " + e.getMessage());
        }
        return resultado;
    }

    private static boolean esRelevante(String descripcion) {
        String d = descripcion.toLowerCase();
        for (String p : PALABRAS_CLAVE) if (d.contains(p)) return true;
        return false;
    }

    private static String inferirTipo(String descripcion) {
        String d = descripcion.toLowerCase();
        if (d.contains("coca") || d.contains("pepsi") || d.contains("agua") ||
            d.contains("jugo") || d.contains("cerveza") || d.contains("vino") ||
            d.contains("fernet") || d.contains("sprite") || d.contains("manaos")) return "Bebida";
        if (d.contains("alfajor") || d.contains("chocol") || d.contains("caramel") ||
            d.contains("gallet") || d.contains("chicle") || d.contains("snack")) return "Golosina/Snack";
        if (d.contains("leche") || d.contains("yogur") || d.contains("queso")) return "Lacteo";
        if (d.contains("jabon") || d.contains("shampoo") || d.contains("detergente") ||
            d.contains("papel") || d.contains("lavandina")) return "Limpieza/Higiene";
        if (d.contains("cafe") || d.contains("yerba") || d.contains("mate")) return "Infusion";
        if (d.contains("arroz") || d.contains("fideos") || d.contains("harina") ||
            d.contains("aceite") || d.contains("azucar")) return "Almacen";
        if (d.contains("cigarr") || d.contains("tabaco")) return "Cigarrillo";
        return "General";
    }

    private static String capitalizar(String texto) {
        if (texto == null || texto.isEmpty()) return texto;
        return Character.toUpperCase(texto.charAt(0)) + texto.substring(1).toLowerCase();
    }

    private static int indexOf(String[] array, String valor) {
        for (int i = 0; i < array.length; i++)
            if (array[i].trim().equalsIgnoreCase(valor)) return i;
        return -1;
    }
}
