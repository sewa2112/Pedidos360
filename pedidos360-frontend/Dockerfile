# ===========================
# Servidor NGINX con HTTPS (Servicio de archivos estaticos precompilados)
# ===========================
FROM nginx:1.27-alpine

# Instalar openssl y generar certificado SSL autofirmado para HTTPS
RUN apk add --no-cache openssl && \
    mkdir -p /etc/ssl/certs && \
    openssl req -x509 -nodes -days 365 -newkey rsa:2048 \
    -keyout /etc/ssl/certs/selfsigned.key \
    -out /etc/ssl/certs/selfsigned.crt \
    -subj "/C=CL/ST=RM/L=Santiago/O=Pedidos360/CN=pedidos360"

# Eliminar configuracion por defecto de nginx
RUN rm /etc/nginx/conf.d/default.conf

# Copiar la configuracion personalizada con soporte SSL
COPY nginx.conf /etc/nginx/conf.d/default.conf

# Copiar los archivos estaticos precompilados de Angular
COPY dist/pedidos360-frontend/browser /usr/share/nginx/html

EXPOSE 80 443

CMD ["nginx", "-g", "daemon off;"]
