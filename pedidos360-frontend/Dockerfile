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
# STAGE 2: Servidor NGINX
# ===========================
FROM nginx:1.27-alpine

# Eliminar configuración por defecto de nginx
RUN rm /etc/nginx/conf.d/default.conf

# Copiar la configuración personalizada
COPY nginx.conf /etc/nginx/conf.d/default.conf

# Copiar los archivos del build de Angular (salida en dist/pedidos360-frontend/browser)
COPY --from=builder /app/dist/pedidos360-frontend/browser /usr/share/nginx/html

EXPOSE 80

CMD ["nginx", "-g", "daemon off;"]

