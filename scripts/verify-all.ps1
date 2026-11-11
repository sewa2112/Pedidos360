# Script de verificacion integral de todos los microservicios Java (Windows PowerShell)
$ErrorActionPreference = "Stop"

$modules = @(
    "pedidos360-backend",
    "local-service",
    "order-processor-dlq",
    "rabbitmq-avanzado",
    "logging-system"
)

foreach ($mod in $modules) {
    Write-Host "=== Ejecutando suite de pruebas en $mod ===" -ForegroundColor Cyan
    Push-Location $mod
    & .\mvnw.cmd test
    if ($LASTEXITCODE -ne 0) {
        Pop-Location
        Write-Error "Fallo en las pruebas de $mod"
        exit $LASTEXITCODE
    }
    Pop-Location
}

Write-Host "TODAS LAS PRUEBAS DE LOS 5 MICROSERVICIOS FINALIZARON EXITOSAMENTE." -ForegroundColor Green
