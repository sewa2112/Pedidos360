# Script para abrir el Producer de consola de Kafka
Write-Host "Iniciando Kafka Console Producer en el topic pedidos-stream..." -ForegroundColor Cyan
Write-Host "Escriba un mensaje y presione Enter para enviarlo."
Write-Host "Para salir presione Ctrl + C." -ForegroundColor Yellow
docker exec -it pedidos360-kafka /opt/kafka/bin/kafka-console-producer.sh --bootstrap-server localhost:9092 --topic pedidos-stream
