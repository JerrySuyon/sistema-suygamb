package com.bisuteria.sistema_suygamb.model;

import jakarta.persistence.*;
@Entity
@Table(name = "productos")
public class Producto {
	@Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Integer id;
	
	@Column(nullable = false)
    private Boolean activo = true;
	
    private String codigo;
    private String nombre;
    private int  stock;
    private Integer cantidadCaja;
    private Integer cajas;
    
    private Double precioCompra;
    private Double precioUnidad;
    private Double precioMayor;

    private String categoria;

    // GETTERS Y SETTERS

    public Integer getId() {
        return id;
    }

    public void setId(Integer id) {
        this.id = id;
    }

    public String getCodigo() {
        return codigo;
    }

    public void setCodigo(String codigo) {
        this.codigo = codigo;
    }

    public String getNombre() {
        return nombre;
    }

    public void setNombre(String nombre) {
        this.nombre = nombre;
    }

    public Integer getStock() {
        return stock;
    }

    public void setStock(Integer stock) {
        this.stock = stock;
    }

    public Double getPrecioUnidad() {
        return precioUnidad;
    }

    public void setPrecioUnidad(Double precioUnidad) {
        this.precioUnidad = precioUnidad;
    }

    public Double getPrecioMayor() {
        return precioMayor;
    }

    public void setPrecioMayor(Double precioMayor) {
        this.precioMayor = precioMayor;
    }

    public String getCategoria() {
        return categoria;
    }

    public void setCategoria(String categoria) {
        this.categoria = categoria;
    }
    public Double getPrecioCompra() {
        return precioCompra;
    }

    public void setPrecioCompra(Double precioCompra) {
        this.precioCompra = precioCompra;
    }
    public Boolean getActivo() {
        return activo;
    }

    public void setActivo(Boolean activo) {
        this.activo = activo;
    }
    
    public Integer getCantidadCaja() {
        return cantidadCaja;
    }

    public void setCantidadCaja(Integer cantidadCaja) {
        this.cantidadCaja = cantidadCaja;
    }
    
    public Integer getCajas() {
        return cajas;
    }

    public void setCajas(Integer cajas) {
        this.cajas = cajas;
    }
    public Integer getCajasCalculadas() {

        if(cantidadCaja == null || cantidadCaja == 0){
            return 0;
        }

        return stock / cantidadCaja;
    }

}
