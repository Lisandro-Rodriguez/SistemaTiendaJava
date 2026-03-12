package com.tienda.util;

import java.io.InputStreamReader;
import java.net.HttpURLConnection;
import java.net.URI;

import com.google.gson.JsonObject;
import com.google.gson.JsonParser;

public class BuscadorProducto {

    public static class DatosProducto {
        public String nombre;
        public String marca;
        public String tipo;

        public boolean encontrado() {
            return nombre != null && !nombre.isEmpty();
        }
    }

    public static DatosProducto buscarPorCodigo(String codigoBarras) {
        DatosProducto datos = new DatosProducto();
        try {
            String urlStr = "https://world.openfoodfacts.org/api/v0/product/" + codigoBarras + ".json";
            HttpURLConnection conn = (HttpURLConnection) URI.create(urlStr).toURL().openConnection();
            conn.setRequestMethod("GET");
            conn.setConnectTimeout(4000); // 4 segundos máximo
            conn.setReadTimeout(4000);
            conn.setRequestProperty("User-Agent", "SistemaTiendaJava/1.0");

            if (conn.getResponseCode() != 200) return datos;

            JsonObject json = JsonParser.parseReader(
                    new InputStreamReader(conn.getInputStream())).getAsJsonObject();

            if (json.get("status").getAsInt() != 1) return datos;

            JsonObject producto = json.getAsJsonObject("product");

            // Nombre
            if (producto.has("product_name_es") && !producto.get("product_name_es").getAsString().isEmpty())
                datos.nombre = capitalizar(producto.get("product_name_es").getAsString());
            else if (producto.has("product_name") && !producto.get("product_name").getAsString().isEmpty())
                datos.nombre = capitalizar(producto.get("product_name").getAsString());

            // Marca
            if (producto.has("brands") && !producto.get("brands").getAsString().isEmpty())
                datos.marca = capitalizar(producto.get("brands").getAsString().split(",")[0].trim());

            // Tipo/Categoría
            if (producto.has("categories") && !producto.get("categories").getAsString().isEmpty()) {
                String cat = producto.get("categories").getAsString();
                // Tomamos la última categoría que suele ser la más específica
                String[] partes = cat.split(",");
                String ultima = partes[partes.length - 1].trim();
                // Limpiamos prefijos como "en:", "es:"
                if (ultima.contains(":")) ultima = ultima.split(":")[1].trim();
                datos.tipo = capitalizar(ultima);
            }

        } catch (Exception e) {
            System.err.println("No se pudo consultar la API: " + e.getMessage());
        }
        return datos;
    }

    private static String capitalizar(String texto) {
        if (texto == null || texto.isEmpty()) return texto;
        return Character.toUpperCase(texto.charAt(0)) + texto.substring(1).toLowerCase();
    }
}