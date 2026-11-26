$ErrorActionPreference = "Stop"

$root = $PSScriptRoot
Set-Location $root

# Cargar variables de entorno desde .env
$envFile = Join-Path $root ".env"

if (Test-Path $envFile) {
    Get-Content $envFile | ForEach-Object {
        if ($_ -match '^\s*([^#=\s]+)\s*=(.*)$') {
            $nombre = $matches[1]
            $valor = $matches[2].Trim().Trim('"').Trim("'")

            [Environment]::SetEnvironmentVariable(
                $nombre,
                $valor,
                "Process"
            )
        }
    }
}

# Validar configuracion JWT
if ([string]::IsNullOrWhiteSpace($env:JWT_SECRET)) {
    throw "JWT_SECRET no configurado"
}

# Configuracion PostgreSQL
if ([string]::IsNullOrWhiteSpace($env:SPRING_DATASOURCE_PASSWORD)) {
    if ([string]::IsNullOrWhiteSpace($env:POSTGRES_PASSWORD)) {
        throw "SPRING_DATASOURCE_PASSWORD o POSTGRES_PASSWORD no configurado"
    }

    $env:SPRING_DATASOURCE_PASSWORD = $env:POSTGRES_PASSWORD
}

# Configuracion SUNAT
if (-not $env:SUNAT_BETA_ENABLED) {
    $env:SUNAT_BETA_ENABLED = "false"
}

if (-not $env:MS_CLIENTE_URL) {
    $env:MS_CLIENTE_URL = "http://localhost:8083"
}

# Configuracion de descubrimiento de microservicios
if (-not $env:EUREKA_ENABLED) {
    $env:EUREKA_ENABLED = "false"
}

if (-not $env:EUREKA_URL) {
    $env:EUREKA_URL = "http://localhost:8761/eureka/"
}

# Iniciar PostgreSQL
Write-Host "[INICIANDO] PostgreSQL" -ForegroundColor Cyan

docker start maranatha-postgres | Out-Null

if ($LASTEXITCODE -ne 0) {
    throw "Error al iniciar PostgreSQL"
}

# Esperar hasta que PostgreSQL este disponible
$postgresListo = $false

for ($i = 0; $i -lt 30; $i++) {
    docker exec maranatha-postgres pg_isready -U postgres | Out-Null

    if ($LASTEXITCODE -eq 0) {
        $postgresListo = $true
        break
    }

    Start-Sleep -Seconds 2
}

if (-not $postgresListo) {
    throw "PostgreSQL no esta disponible"
}

Write-Host "[ACTIVO] PostgreSQL" -ForegroundColor Green

# Microservicios de Maranatha Store
$modules = @(
    "ms-auth",
    "ms-producto",
    "ms-cliente",
    "ms-ventas",
    "ms-facturacion",
    "api-gateway"
)

# Iniciar microservicios en ventanas independientes
foreach ($module in $modules) {

    Write-Host "[INICIANDO] $module" -ForegroundColor Cyan

    $command = "Set-Location '$root'; .\mvnw.cmd -pl $module spring-boot:run"

    Start-Process powershell -ArgumentList '-NoExit','-Command',$command

    Start-Sleep -Seconds 2
}

# Resumen de arranque
Write-Host ""
Write-Host "========================================" -ForegroundColor Green
Write-Host "       MARANATHA STORE - BACKEND" -ForegroundColor Green
Write-Host "========================================" -ForegroundColor Green
Write-Host ""
Write-Host "Solicitudes de arranque finalizadas."
Write-Host "PostgreSQL: maranatha-postgres"
Write-Host "Gateway: http://localhost:8080"
Write-Host "Swagger: http://localhost:8080/swagger-ui.html"
Write-Host "SUNAT Beta: $env:SUNAT_BETA_ENABLED"
Write-Host ""
Write-Host "========================================" -ForegroundColor Green