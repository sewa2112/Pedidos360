# ===========================
# STAGE 1: Build con Node.js
# ===========================
FROM node:22-alpine AS builder

WORKDIR /app

# Copiar package.json primero para caché de capas
COPY package.json package-lock.json ./

# Instalar dependencias
RUN npm ci --ignore-scripts

# Copiar el código fuente
COPY . .

# Build de producción
RUN npm run build -- --configuration production

# ===========================
# STAGE 2: Servidor NGINX con HTTPS
# ===========================
FROM nginx:1.27-alpine

# Instalar openssl y generar certificado SSL autofirmado para HTTPS
RUN apk add --no-cache openssl && \
    mkdir -p /etc/ssl/certs && \
    openssl req -x509 -nodes -days 365 -newkey rsa:2048 \
    -keyout /etc/ssl/certs/selfsigned.key \
    -out /etc/ssl/certs/selfsigned.crt \
    -subj "/C=CL/ST=RM/L=Santiago/O=Pedidos360/CN=pedidos360"

# Eliminar configuración por defecto de nginx
RUN rm /etc/nginx/conf.d/default.conf

# Copiar la configuración personalizada con soporte SSL
COPY nginx.conf /etc/nginx/conf.d/default.conf

# Copiar los archivos del build de Angular
COPY --from=builder /app/dist/pedidos360-frontend/browser /usr/share/nginx/html

EXPOSE 80 443

CMD ["nginx", "-g", "daemon off;"]
