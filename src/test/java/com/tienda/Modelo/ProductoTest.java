package com.tienda.Modelo;

import static org.junit.jupiter.api.Assertions.assertEquals;
import org.junit.jupiter.api.Test;

public class ProductoTest {

    @Test
    public void testPrecioVentaCalculadoCorrectamente() {
        Producto p = new Producto("123", "Coca Cola", "Bebida", "Coca", 100.0, 10, 50.0);
        assertEquals(150.0, p.getPrecioVenta(), 0.01);
    }

    @Test
    public void testPrecioVentaSinMargen() {
        Producto p = new Producto("456", "Agua", "Bebida", "Ser", 100.0, 5, 0.0);
        assertEquals(100.0, p.getPrecioVenta(), 0.01);
    }

    @Test
    public void testStockSetter() {
        Producto p = new Producto("789", "Pan", "Alimento", "Bimbo", 50.0, 20, 30.0);
        p.setStock(15);
        assertEquals(15, p.getStock());
    }

    @Test
    public void testGetters() {
        Producto p = new Producto("001", "Leche", "Lacteo", "LaSerenisima", 200.0, 8, 25.0);
        assertEquals("001", p.getCodigoBarras());
        assertEquals("Leche", p.getNombre());
        assertEquals("Lacteo", p.getTipo());
        assertEquals("LaSerenisima", p.getMarca());
        assertEquals(200.0, p.getPrecioCosto(), 0.01);
        assertEquals(8, p.getStock());
        assertEquals(25.0, p.getMargenGanancia(), 0.01);
    }
}