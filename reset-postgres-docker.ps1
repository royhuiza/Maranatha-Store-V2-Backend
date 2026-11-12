Set-Location $PSScriptRoot
docker compose down -v
docker compose up -d maranatha-postgres
Write-Host 'PostgreSQL reiniciado con las cuatro bases limpias.' -ForegroundColor Green
