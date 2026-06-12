param(
    [string]$RemoteHost = "your-server.example.com",
    [int]$SshPort = 22,
    [string]$RemoteUser = "root",
    [int]$LocalPort = 18080,
    [int]$RemotePort = 8000
)

$ErrorActionPreference = "Stop"

Write-Host "Opening SSH tunnel: localhost:$LocalPort -> ${RemoteUser}@${RemoteHost}:$RemotePort"
ssh -N -L "${LocalPort}:127.0.0.1:${RemotePort}" -p $SshPort "${RemoteUser}@${RemoteHost}"

