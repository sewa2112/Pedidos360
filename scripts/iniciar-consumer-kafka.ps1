# Script para abrir el Consumer de consola leyendo desde el inicio (--from-beginning)
Write-Host "Iniciando Kafka Console Consumer en el topic pedidos-stream (--from-beginning)..." -ForegroundColor Cyan
Write-Host "Para salir presione Ctrl + C." -ForegroundColor Yellow
docker exec -it pedidos360-kafka /opt/kafka/bin/kafka-console-consumer.sh --bootstrap-server localhost:9092 --topic pedidos-stream --from-beginning
