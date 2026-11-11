#!/usr/bin/env bash
set -euo pipefail

# 1. Validar que existe .env antes de levantar la suite
if [ ! -f ".env" ]; then
  echo "ERROR: No se encontro .env en la raiz. Copie .env.example a .env y complete las credenciales."
  exit 1
fi

set -a
source .env
set +a

MODULES=("pedidos360-backend" "local-service" "order-processor-dlq" "rabbitmq-avanzado" "logging-system")

for mod in "${MODULES[@]}"; do
  echo "=== Ejecutando pruebas en $mod ==="
  (cd "$mod" && ./mvnw test)
done

echo "=== Verificando endpoints de integracion en ejecucion (si Docker Compose esta activo) ==="
curl -fsS http://localhost:8080/api/listeners >/dev/null && echo "OK rabbitmq-avanzado (8080)"
curl -fsS http://localhost:8081/api/orders/status >/dev/null && echo "OK order-processor-dlq orders (8081)"
curl -fsS http://localhost:8081/api/dlq/status >/dev/null && echo "OK order-processor-dlq dlq (8081)"
curl -fsS http://localhost:8082/api/locales >/dev/null && echo "OK local-service (8082)"
curl -fsS http://localhost:8083/api/logs/status >/dev/null && echo "OK logging-system (8083)"
echo "Verificacion completa."
