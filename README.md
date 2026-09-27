# Pedidos360 - Sistema de Gestion de Pedidos Cloud Native

Este proyecto es una solucion integral para la gestion y creacion de ordenes de trabajo y pedidos, disenada siguiendo principios de arquitectura Cloud Native, seguridad multi-capa y despliegue contenerizado en la nube.

---

## Arquitectura General

El sistema esta desacoplado en capas independientes para garantizar alta disponibilidad, seguridad y escalabilidad:

```text
[ Cliente / Navegador ]
         │
         ├── (1) Autenticacion OAuth2 / PKCE ──> [ Microsoft Entra ID (Azure AD) ]
         │                                                    │
         │  (Token JWT con scope 'OT.Create')                 │
         ▼                                                    ▼
[ Frontend Angular (Nginx / HTTPS en AWS EC2) ]
         │
         ├── (2) Peticion con Bearer Token
         ▼
[ AWS API Gateway (HTTP API) ]
   └── (3) Valida JWT con Entra ID (Emisor + Audiencia)
         │
         ├── (4) Peticion autorizada
         ▼
[ Backend Spring Boot (Docker en AWS EC2) ]
   └── (5) Resource Server: valida token + @PreAuthorize("hasAuthority('SCOPE_OT.Create')")
         │
         ├── (6) Persistencia JPA / Hibernate
         ▼
[ Base de Datos AWS RDS (MySQL 8) ]
```

---

## 1. Autenticacion con Angular y Microsoft Entra ID (MSAL)

La autenticacion del usuario final se implemento utilizando las librerias oficiales de Microsoft:
- **@azure/msal-browser**: Gestiona el flujo Authorization Code Flow con PKCE directamente en el navegador.
- **@azure/msal-angular**: Integra el ciclo de vida de MSAL con el framework de Angular.

### Componentes clave implementados:
1. **MsalGuard**: Protege la ruta `/dashboard`. Si un usuario intenta ingresar sin autenticarse, es redirigido automaticamente al login oficial de Microsoft Entra ID.
2. **MsalInterceptor**: Monitorea las llamadas salientes hacia los endpoints de pedidos (`/pedidos`). Cuando detecta una peticion a un recurso protegido, obtiene el Access Token silenciosamente y lo inyecta en la cabecera HTTP:
   ```http
   Authorization: Bearer eyJ0eXAiOiJKV1Qi...
   ```
3. **Ambitos (Scopes)**: El frontend solicita especificamente el permiso de negocio `api://<BACKEND_CLIENT_ID>/OT.Create`.

---

## 2. Seguridad en Dos Capas (API Gateway + Backend Resource Server)

Para garantizar la seguridad del backend (enfoque Zero Trust), el token es validado en dos etapas independientes:

### Capa 1: AWS API Gateway (API Manager)
- Cuenta con un **JWT Authorizer** integrado con Microsoft Entra ID.
- Revisa que el emisor (`iss: https://sts.windows.net/.../`) y la audiencia (`aud: api://...`) sean validos.
- Si el cliente envia una peticion sin token o con un token manipulado/expirado, API Gateway responde con `401 Unauthorized` de inmediato, protegiendo al backend de trafico ilegitimo.

### Capa 2: Backend Spring Boot (OAuth2 Resource Server)
- Utiliza **Spring Cloud Azure** y **Spring Security OAuth2 Resource Server**.
- Valida la firma del token y deserializa los claims.
- **Control de Acceso Basado en Roles y Scopes**: El endpoint para crear pedidos esta protegido con:
  ```java
  @PostMapping
  @PreAuthorize("hasAuthority('SCOPE_OT.Create')")
  public Pedido crearPedido(@RequestBody Pedido pedido) { ... }
  ```
  Esto asegura que solo usuarios con el permiso explicito otorgado en Entra ID puedan registrar ordenes.
- **Configuracion CORS Global**: Implementada mediante `CorsConfigurationSource` para admitir solicitudes cross-origin desde la interfaz web de produccion y desarrollo.

---

## 3. Contenerizacion y Despliegue en AWS EC2

Toda la aplicacion esta orquestada mediante **Docker Compose**:

| Servicio | Tecnologia | Puerto | Descripcion |
|---|---|---|---|
| **Frontend** | Angular + Nginx Alpine | `80` (HTTP) / `443` (HTTPS) | Multi-stage build con Node 22. Genera certificados SSL para operar bajo HTTPS seguro y sirve la SPA. |
| **Backend** | Spring Boot 4 + Java 21 | `8085` | Multi-stage build con Maven 3.9 y Eclipse Temurin JRE 21. Conectado a la base de datos externa. |
| **Base de Datos** | AWS RDS MySQL | `3306` | Base de datos relacional administrada en AWS con pool HikariCP. |

### Infraestructura AWS:
- **Instancia EC2**: Ubuntu Linux `t3.micro`.
- **IP Elastica**: Direccion publica estatica asociada a la instancia para garantizar persistencia de red.
- **Security Groups**: Puertos abiertos estrictamente para `SSH (22)`, `HTTP (80)`, `HTTPS (443)` y `Backend (8085)`.

---

## 4. Como ejecutar el proyecto

### En local:
```bash
# Backend (Spring Boot en puerto 8085)
cd pedidos360-backend
./mvnw spring-boot:run

# Frontend (Angular en puerto 4200)
cd pedidos360-frontend
npm install
npm start
```

### En produccion (AWS EC2):
```bash
# Clonar y configurar variables de entorno
cp .env.example .env

# Levantar todos los contenedores
sudo docker compose up -d --build
```

---

*Y eso profe, disculpe el atraso =)*
