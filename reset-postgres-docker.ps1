Set-Location $PSScriptRoot
docker compose down -v
docker compose up -d postgres
Write-Host 'PostgreSQL reiniciado con las cuatro bases limpias.' -ForegroundColor Green
