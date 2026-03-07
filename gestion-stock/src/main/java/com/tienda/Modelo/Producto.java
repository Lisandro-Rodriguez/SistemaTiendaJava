package com.tienda.Modelo;

public class Producto {
    private String codigoBarras;
    private String nombre;
    private String tipo;    // NUEVO
    private String marca;   // NUEVO
    private double precioCosto;
    private int stock;
    private double margenGanancia;
    private double precioVenta;

    // Constructor actualizado
    public Producto(String codigoBarras, String nombre, String tipo, String marca, double precioCosto, int stock, double margenGanancia) {
        this.codigoBarras = codigoBarras;
        this.nombre = nombre;
        this.tipo = tipo;
        this.marca = marca;
        this.precioCosto = precioCosto;
        this.stock = stock;
        this.margenGanancia = margenGanancia;
        this.precioVenta = precioCosto + (precioCosto * (margenGanancia / 100));
    }

    // Getters
    public String getCodigoBarras() { return codigoBarras; }
    public String getNombre() { return nombre; }
    public String getTipo() { return tipo; }
    public String getMarca() { return marca; }
    public double getPrecioCosto() { return precioCosto; }
    public int getStock() { return stock; }
    public double getMargenGanancia() { return margenGanancia; }
    public double getPrecioVenta() { return precioVenta; }

    // Setters básicos
    public void setStock(int stock) { this.stock = stock; }
}