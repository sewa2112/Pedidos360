package com.pedidos360.local.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Pattern;
import jakarta.validation.constraints.Size;

public class CreateLocalDTO {

    @NotBlank(message = "El nombre del restaurante/local no puede estar vacio")
    @Size(min = 3, max = 100, message = "El nombre debe tener entre 3 y 100 caracteres")
    private String nombre;

    @NotBlank(message = "La direccion no puede estar vacia")
    @Size(min = 5, max = 200, message = "La direccion debe tener entre 5 y 200 caracteres")
    private String direccion;

    @Pattern(regexp = "^[+0-9\\s-]{6,20}$", message = "El telefono solo puede contener numeros, espacios o signo +")
    private String telefono;

    @NotBlank(message = "La categoria no puede estar vacia")
    @Size(min = 3, max = 50, message = "La categoria debe tener entre 3 y 50 caracteres")
    private String categoria;

    private boolean activo = true;

    public CreateLocalDTO() {
    }

    public CreateLocalDTO(String nombre, String direccion, String telefono, String categoria, boolean activo) {
        this.nombre = nombre;
        this.direccion = direccion;
        this.telefono = telefono;
        this.categoria = categoria;
        this.activo = activo;
    }

    public String getNombre() {
        return nombre;
    }

    public void setNombre(String nombre) {
        this.nombre = nombre;
    }

    public String getDireccion() {
        return direccion;
    }

    public void setDireccion(String direccion) {
        this.direccion = direccion;
    }

    public String getTelefono() {
        return telefono;
    }

    public void setTelefono(String telefono) {
        this.telefono = telefono;
    }

    public String getCategoria() {
        return categoria;
    }

    public void setCategoria(String categoria) {
        this.categoria = categoria;
    }

    public boolean isActivo() {
        return activo;
    }

    public void setActivo(boolean activo) {
        this.activo = activo;
    }
}
