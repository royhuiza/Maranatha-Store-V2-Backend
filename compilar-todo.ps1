$ErrorActionPreference = 'Stop'
Set-Location $PSScriptRoot
.\mvnw.cmd clean package -DskipTests
Write-Host "Backend compilado correctamente." -ForegroundColor Green
