# Script para abrir el Consumer en vivo (tiempo real)
Write-Host "Iniciando Kafka Console Consumer en tiempo real en pedidos-stream..." -ForegroundColor Cyan
Write-Host "Esperando nuevos mensajes que sean publicados por el Producer..."
Write-Host "Para salir presione Ctrl + C." -ForegroundColor Yellow
docker exec -it pedidos360-kafka /opt/kafka/bin/kafka-console-consumer.sh --bootstrap-server localhost:9092 --topic pedidos-stream
