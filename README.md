# Pedidos360 - Sistema de Gestion de Pedidos Cloud Native con Mensajeria Asincrona

Hola profe. En este repositorio presentamos la solucion completa de Pedidos360, disenada siguiendo principios de arquitectura Cloud Native, microservicios desacoplados, seguridad perimetral Zero Trust con IDaaS empresarial, y un sistema de mensajeria asincrona con RabbitMQ que incluye politicas de retencion, Dead Letter Queue (DLQ), clusterizacion y administracion programatica en tiempo real.

---

## Arquitectura General del Sistema

El flujo del sistema combina la persistencia transaccional en base de datos cloud con el procesamiento asincrono de eventos desacoplados:

```text
[ Cliente / Navegador Angular ]
         |
         +--> (1) Login OIDC con PKCE ----> [ Microsoft Entra ID (Azure AD) ]
         |                                                 |
         |    (Bearer Token JWT con scopes)                |
         v                                                 v
[ AWS API Gateway (HTTP API) ]
    +--> (2) Valida JWT en el borde (Issuer, Audience, Firma asimetrica)
         |
         +--> (3) Peticion autorizada
         v
[ Microservicio Principal: pedidos360-backend ]
    +--> (4) Valida JWT internamente con Spring Security (@PreAuthorize)
    +--> (5) Guarda en BD relacional ----> [ AWS RDS (MySQL 8) ]
    +--> (6) Emite evento asincrono  ----> [ RabbitMQ Broker (orders.exchange) ]
                                                   |
                     +-----------------------------+-----------------------------+
                     |                                                           |
                     v                                                           v
   [ Microservicio Procesador y DLQ ]                          [ Microservicio Administrador ]
       (order-processor-dlq)                                       (rabbitmq-avanzado)
   - Escucha en orders.queue                                   - API REST de gestion dinamica
   - ACK manual en caso de exito                               - Control de listeners (pause/resume)
   - NACK hacia orders.dlx en fallas                           - Soporte de cluster multi-nodo
   - Monitoreo en orders.dlq (cuarentena)                      - Validacion estricta con DTOs
```

---

## 1. Seguridad e Identidad en la Nube (IDaaS)

### Autenticacion en el Frontend con Microsoft Entra ID y MSAL
Para no almacenar credenciales ni contraseñas en nuestra base de datos, delegamos la autenticacion a un proveedor IDaaS empresarial utilizando las librerias oficiales `@azure/msal-browser` y `@azure/msal-angular`:
- **Authorization Code Flow con PKCE**: Flujo oficial recomendado para aplicaciones SPA (Single Page Applications) que elimina el uso de secretos expuestos en el cliente.
- **MsalGuard**: Protege las rutas internas como `/dashboard`. Si un usuario intenta ingresar sin haberse identificado, el guard lo redirige a la pantalla oficial de inicio de sesion corporativa de Microsoft.
- **MsalInterceptor**: Intercepta de manera automatica cada peticion saliente hacia los endpoints de la API (`/pedidos`) y le adjunta el token JWT valido en la cabecera HTTP:
  ```http
  Authorization: Bearer eyJ0eXAiOiJKV1Qi...
  ```
- **Ambitos (Scopes)**: El frontend solicita especificamente los scopes estandar de identidad `openid`, `profile` y el permiso de negocio delegado `api://<BACKEND_CLIENT_ID>/access_as_user` (u `OT.Create`).

### Validacion en Dos Capas (Defense in Depth)
1. **Capa Externa (AWS API Gateway - API Manager)**: Dispone de un JWT Authorizer conectado al emisor de nuestro Tenant (`https://sts.windows.net/ac6019c7-78df-4d97-9d85-291001476eb0/`) y a la audiencia de nuestro backend. Peticiones sin token o con firmas manipuladas son descartadas de inmediato en el borde con codigo `401 Unauthorized` sin consumir computo en el backend.
2. **Capa Interna (Backend Spring Boot - Resource Server)**: Utiliza `spring-boot-starter-oauth2-resource-server` para validar la firma digital y los reclamos del token antes de ejecutar metodos del controlador, garantizando que nadie pueda evadir las reglas de acceso.

---

## 2. Mensajeria Asincrona y Desacoplamiento con RabbitMQ

Uno de los requerimientos clave fue incorporar mensajeria asincrona sin poner en riesgo la logica de negocio existente.

### Emision Desacoplada de Eventos (pedidos360-backend)
Cuando un cliente genera una orden desde el frontend o mediante llamada REST:
1. `PedidoService` guarda la entidad en la base de datos AWS RDS MySQL como se venia haciendo habitualmente.
2. Una vez confirmado el guardado, delega en `OrderEventPublisher` la publicacion del evento en RabbitMQ:
   - **Exchange**: `orders.exchange` (Direct).
   - **Routing Key**: `order.created`.
   - **Payload**: Mensaje estructurado con los datos del pedido y timestamp.
3. **Mecanismo de Resiliencia**: El envio a RabbitMQ esta aislado mediante un bloque seguro con logging de advertencia. Si el servidor de mensajeria estuviera temporalmente caido o reiniciandose, el guardado del pedido en la base de datos relacional no se revierte ni se interrumpe, manteniendo la continuidad operativa del sistema.

---

## 3. Manejo de Errores, Confirmaciones y Dead Letter Queue (order-processor-dlq)

Para asegurar que ningun pedido o mensaje quede en el limbo ante fallos de procesamiento, implementamos el patron de **Dead Letter Exchange (DLX)** y **Dead Letter Queue (DLQ)**.

### Configuracion de Colas y Politicas de Retencion
En `RabbitMQConfig` se definieron de forma centralizada:
- **orders.queue**: Cola principal durable. Tiene un tiempo de vida para los mensajes (`x-message-ttl: 30000` ms) y las directivas de desvio automatico:
  - `x-dead-letter-exchange: orders.dlx`
  - `x-dead-letter-routing-key: order.dead`
- **orders.dlq**: Cola de mensajes muertos con politica de retencion extendida (`x-message-ttl: 86400000` ms, es decir 24 horas) para almacenar aquellos mensajes que no pudieron completarse, permitiendo auditoria, analisis forense y reprocesamiento posterior.

### Confirmaciones Explicitas (ACK / NACK)
El consumidor `OrderConsumer` opera con confirmaciones manuales (`acknowledge-mode: manual`):
- **Exito**: Si el procesamiento concluye adecuadamente, se ejecuta `channel.basicAck(deliveryTag, false)`.
- **Falla Controlada**: Ante una excepcion o error no recuperable, se invoca `channel.basicNack(deliveryTag, false, false)` con `requeue = false`. Esto evita ciclos infinitos de reintentos sobre la cola principal y deriva el mensaje directamente al DLX.
- **Auditoria de Cuarentena**: Un listener conectado a `orders.dlq` captura los mensajes que cayeron en cuarentena e imprime trazas estructuradas para su monitoreo.

---

## 4. Microservicio Administrador y Cluster (rabbitmq-avanzado)

Este microservicio se encarga de la administracion programatica del broker y provee tolerancia a fallos.

### Cluster de Alta Disponibilidad con Docker Compose
Se configuro un cluster de dos nodos (`rabbitmq-1` y `rabbitmq-2`) interconectados en una red bridge compartiendo la misma cookie de Erlang (`secret_cookie_value`) y sincronizando colas y exchanges mediante classic peer discovery:
- **Nodo 1**: Puerto AMQP `5672` / Consola Web `15672`.
- **Nodo 2**: Puerto AMQP `5673` / Consola Web `15673`.
- **Sincronizacion**: `queue_master_locator = min-masters`.
- **Comprobacion de salud**: Ejecutable via `docker exec rabbitmq-1 rabbitmqctl cluster_status`.

### Gestion Programatica con RabbitAdmin
Toda la creacion y mantenimiento de recursos se realiza mediante el servicio `RabbitResourceManager`, abstrayendo a los controladores de la libreria nativa de RabbitMQ:
- Creacion y eliminacion de colas y exchanges en caliente.
- Declaracion dinamica de bindings y routing keys.
- Purgado de colas acumuladas.
- Consulta de metricas en tiempo real (conteo de mensajes encolados y consumidores concurrentes activos).

### Validacion Estricta de Entradas con DTOs
Para asegurar la robustez de los endpoints del administrador:
- Se implementaron DTOs validados (`CreateQueueDTO`, `CreateExchangeDTO`, `CreateBindingDTO`) con anotaciones `@NotBlank`, `@Size(min = 3, max = 50)` y expresiones regulares con `@Pattern`.
- Un `GlobalExceptionHandler` captura violaciones de validacion y responde con codigo HTTP 400 Bad Request formateado de manera legible si se intentan crear nombres vacios o con caracteres invalidos.

### Control Dinamico de Listeners (Pausa y Reanudacion)
A traves de `ListenerManagementService` y su controlador REST, es posible detener (`POST /api/listeners/{id}/pause`) o reanudar (`POST /api/listeners/{id}/resume`) cualquier consumidor en tiempo real sin reiniciar la aplicacion ni desconectar las conexiones maestras del broker.

---

## 5. Tabla de Microservicios y Puertos del Ecosistema

| Componente | Directorio | Tecnologia | Puertos | Responsabilidad |
|---|---|---|---|---|
| **Frontend SPA** | `pedidos360-frontend` | Angular 21 + Nginx Alpine | `80` (HTTP) / `443` (HTTPS) | Interfaz web de usuario, flujo OIDC con MSAL y consumo seguro de APIs. |
| **Backend Principal** | `pedidos360-backend` | Spring Boot 4 + Java 21 | `8085` | Logica de negocio, persistencia en AWS RDS MySQL y publicacion de eventos. |
| **Procesador y DLQ** | `order-processor-dlq` | Spring Boot 3 + Spring AMQP | `8080` | Consumo concurrente, manual ACK/NACK, envio a DLX y logs de cuarentena. |
| **Administrador RabbitMQ** | `rabbitmq-avanzado` | Spring Boot 3 + Spring AMQP | `8080` | Endpoints REST para administrar colas/exchanges, control de listeners y cluster. |
| **Broker de Mensajeria** | Contenedor Docker | RabbitMQ 3.12 Management | `5672` (AMQP) / `15672` (Web UI) | Broker de eventos y colas con interfaz grafica de administracion. |
| **Base de Datos Cloud** | AWS RDS Gestionada | MySQL 8.0 | `3306` | Persistencia relacional de clientes, restaurantes, items y pedidos. |

---

## 6. Despliegue en AWS EC2 y Verificacion en Vivo

### Despliegue en Produccion (EC2)
En la instancia EC2 (`52.22.210.39`), toda la solucion se orquesta con Docker Compose:

```bash
# 1. Comprobar memoria y Swap en la maquina
free -h

# 2. Levantar la suite completa en segundo plano
sudo docker compose up -d --build

# 3. Comprobar que los tres servicios esten en estado Up
sudo docker compose ps
```

### Comprobacion de RabbitMQ desde la Consola de EC2
Para inspeccionar el motor de mensajeria directamente en el contenedor:

```bash
# Ver estado de colas
sudo docker exec pedidos360-rabbitmq rabbitmqadmin -u admin -p admin123 list queues

# Publicar un mensaje manual de prueba en el Exchange
sudo docker exec pedidos360-rabbitmq rabbitmqadmin -u admin -p admin123 publish exchange=orders.exchange routing_key=order.created payload='{"pedidoId":99,"cliente":"Prueba Docente"}'

# Leer y confirmar el mensaje desde la cola
sudo docker exec pedidos360-rabbitmq rabbitmqadmin -u admin -p admin123 get queue=orders.queue ackmode=ack_requeue_false
```

### Accesos Directos
- **Frontend SPA en Produccion**: `https://52.22.210.39`
- **Consola Grafica de RabbitMQ**: `http://52.22.210.39:15672` (Usuario: `admin` / Password: `admin123`)
- **AWS API Gateway**: `https://0i74gnrx68.execute-api.us-east-1.amazonaws.com/pedidos`

---

## 7. Resumen de Cumplimiento de la Rubrica (Evaluacion Parcial 3)

1. **Configuracion centralizada (12%)**: Constantes de colas, exchanges y routing keys definidas de manera ordenada en clases `@Configuration`.
2. **Beans de mensajeria explicitos (13%)**: Declaracion formal de beans para `Queue`, `DirectExchange`, `FanoutExchange` (DLX) y `Binding`.
3. **Separacion de arquitectura y negocio (10%)**: `PedidoService` no contiene codigo de infraestructura ni configuracion de RabbitMQ; todo esta aislado en `OrderEventPublisher` y clases de configuracion.
4. **Consumidores funcionales (15%)**: Consumidores organizados con `@RabbitListener` y container factories con concurrencia configurada.
5. **Manejo de confirmaciones y DLQ (20%)**: Uso de `basicAck` y `basicNack(requeue = false)` con redireccion hacia el Dead Letter Exchange y cola de cuarentena documentada.
6. **Endpoints REST de administracion (13%)**: Rutas bien estructuradas bajo `/api/rabbitmq/*` y `/api/listeners/*`.
7. **Servicio dedicado (10%)**: Encapsulacion completa de `RabbitAdmin` en `RabbitResourceManager`.
8. **Validacion de parametros (7%)**: Uso de DTOs con Bean Validation (`@NotBlank`, `@Size`, `@Pattern`) y manejo global de excepciones con codigo HTTP 400 descriptivo.
