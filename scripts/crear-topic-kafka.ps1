# Script para crear y verificar el topic pedidos-stream en Apache Kafka
Write-Host "Consultando topics existentes en Kafka..." -ForegroundColor Cyan
docker exec pedidos360-kafka /opt/kafka/bin/kafka-topics.sh --bootstrap-server localhost:9092 --list

Write-Host "`nCreando topic pedidos-stream con 3 particiones y factor de replicacion 1..." -ForegroundColor Cyan
docker exec pedidos360-kafka /opt/kafka/bin/kafka-topics.sh --create --if-not-exists --topic pedidos-stream --partitions 3 --replication-factor 1 --bootstrap-server localhost:9092

Write-Host "`nCreando topic opcional pedidos360-pedidos con 1 particion..." -ForegroundColor Cyan
docker exec pedidos360-kafka /opt/kafka/bin/kafka-topics.sh --create --if-not-exists --topic pedidos360-pedidos --partitions 1 --replication-factor 1 --bootstrap-server localhost:9092

Write-Host "`nVerificando lista actualizada de topics:" -ForegroundColor Green
docker exec pedidos360-kafka /opt/kafka/bin/kafka-topics.sh --bootstrap-server localhost:9092 --list

Write-Host "`nDetalle del topic pedidos-stream:" -ForegroundColor Green
docker exec pedidos360-kafka /opt/kafka/bin/kafka-topics.sh --bootstrap-server localhost:9092 --describe --topic pedidos-stream
