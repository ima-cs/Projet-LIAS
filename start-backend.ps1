param([switch]$Tests)
$ErrorActionPreference = 'Stop'
Set-Location (Join-Path $PSScriptRoot 'lias-backend')
if (-not $env:LIAS_DB_PASSWORD) { throw 'Définir LIAS_DB_PASSWORD avec le mot de passe PostgreSQL du rôle lias.' }
if (-not $env:LIAS_JWT_SECRET) {
    $jwtBytes = New-Object byte[] 64
    $random = [System.Security.Cryptography.RandomNumberGenerator]::Create()
    $random.GetBytes($jwtBytes)
    $random.Dispose()
    $env:LIAS_JWT_SECRET = [Convert]::ToBase64String($jwtBytes)
}
if ($Tests) { & mvn.cmd clean test; if ($LASTEXITCODE -ne 0) { exit $LASTEXITCODE } }
& mvn.cmd spring-boot:run '-Dspring-boot.run.profiles=demo'
exit $LASTEXITCODE
