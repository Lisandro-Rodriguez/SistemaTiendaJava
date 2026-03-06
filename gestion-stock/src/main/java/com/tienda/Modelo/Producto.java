package com.tienda.Modelo;

public class Producto {
    private String codigoBarras;
    private String nombre;
    private double precioCosto;
    private double precioVenta;
    private int stock;
    private double porcentajeGanancia;

    

    public Producto(String codigoBarras, String nombre, double precioCosto, int stock, double porcentajeGanancia){
        this.codigoBarras = codigoBarras;
        this.nombre = nombre;
        this.precioCosto = precioCosto;
        this.stock = stock;
        this.porcentajeGanancia = porcentajeGanancia;

        this.precioVenta= calcularPrecioVenta(precioCosto,porcentajeGanancia);
    }

    private double calcularPrecioVenta(double costo, double porcentaje){
        return costo + (costo * (porcentaje / 100));
    }

    public void setPrecioVenta(double p) { this.precioVenta = p; };

    public String getCodigoBarras(){
        return codigoBarras;
    }
    public String getNombre(){
        return nombre;
    }
    public double getPrecioCosto(){
        return precioCosto;
    }
    public double getPrecioVenta(){
        return precioVenta;
    }
    public int getStock(){
        return stock;
    }
    public double getPorcentajeGanancia(){
        return porcentajeGanancia;
    }
}
