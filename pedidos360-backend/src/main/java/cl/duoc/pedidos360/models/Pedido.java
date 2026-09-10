package cl.duoc.pedidos360.models;
import jakarta.persistence.*;


@Entity
@Table(name = "pedidos")
public class Pedido {

@Id
@GeneratedValue(strategy = GenerationType.IDENTITY)
private Integer id;
private String nombre_cliente;
private String restaurante;
private String productos;
private String direccion;
private Integer monto_Total;
private String estado;
private String fecha;
    
public Integer getId() { return id; }

public String getCliente() { return nombre_cliente; }
    public void setCliente(String nombre_cliente) { this.nombre_cliente = nombre_cliente; }

    public String getRestaurante() { return restaurante; }
    public void setRestaurante(String restaurante) { this.restaurante = restaurante; }

    public String getItems() { return productos; }
    public void setItems(String productos) { this.productos = productos; }

    public String getDireccion() { return direccion; }
    public void setDireccion(String direccion) { this.direccion = direccion; }

    public Integer getMontoTotal() { return monto_Total; }
    public void setMontoTotal(Integer monto_Total) { this.monto_Total = monto_Total; }

    public String getEstado() { return estado; }
    public void setEstado(String estado) { this.estado = estado; }

    public String getFecha() { return fecha; }
    public void setFecha(String fecha) { this.fecha = fecha; }
}
