param(
    [int]$Port = 8000
)

$ErrorActionPreference = 'Stop'
$Root = [System.IO.Path]::GetFullPath((Join-Path $PSScriptRoot '..'))
$Prefix = "http://127.0.0.1:$Port/"

function Get-ContentType([string]$Path) {
    switch ([System.IO.Path]::GetExtension($Path).ToLowerInvariant()) {
        '.html' { 'text/html; charset=utf-8' }
        '.htm'  { 'text/html; charset=utf-8' }
        '.js'   { 'application/javascript; charset=utf-8' }
        '.css'  { 'text/css; charset=utf-8' }
        '.json' { 'application/json; charset=utf-8' }
        '.xml'  { 'application/xml; charset=utf-8' }
        '.svg'  { 'image/svg+xml' }
        '.png'  { 'image/png' }
        '.jpg'  { 'image/jpeg' }
        '.jpeg' { 'image/jpeg' }
        '.gif'  { 'image/gif' }
        '.ico'  { 'image/x-icon' }
        '.woff' { 'font/woff' }
        '.woff2'{ 'font/woff2' }
        '.ttf'  { 'font/ttf' }
        default { 'application/octet-stream' }
    }
}

$Listener = New-Object System.Net.HttpListener
$Listener.Prefixes.Add($Prefix)

try {
    $Listener.Start()
} catch {
    Write-Host ''
    Write-Host "R4-Server konnte auf Port $Port nicht gestartet werden." -ForegroundColor Red
    Write-Host $_.Exception.Message -ForegroundColor Red
    Write-Host ''
    Write-Host 'Falls bereits ein R4-Server laeuft, kannst du im Browser diese Adresse oeffnen:'
    Write-Host "  ${Prefix}r4.html"
    Read-Host 'Enter zum Beenden'
    exit 1
}

Write-Host 'Blockly@rduino R4 - lokaler Testserver' -ForegroundColor Cyan
Write-Host "Ordner: $Root"
Write-Host "Adresse: ${Prefix}r4.html"
Write-Host ''
Write-Host 'Dieses Fenster offen lassen. Zum Beenden Strg+C druecken.' -ForegroundColor Yellow

Start-Process "${Prefix}r4.html"

try {
    while ($Listener.IsListening) {
        $Context = $Listener.GetContext()
        $RequestPath = [System.Uri]::UnescapeDataString($Context.Request.Url.AbsolutePath.TrimStart('/'))
        if ([string]::IsNullOrWhiteSpace($RequestPath)) {
            $RequestPath = 'r4.html'
        }

        $RelativePath = $RequestPath.Replace('/', [System.IO.Path]::DirectorySeparatorChar)
        $Candidate = [System.IO.Path]::GetFullPath((Join-Path $Root $RelativePath))

        # Prevent ../ path traversal outside the repository folder.
        if (-not $Candidate.StartsWith($Root, [System.StringComparison]::OrdinalIgnoreCase)) {
            $Context.Response.StatusCode = 403
            $Context.Response.Close()
            continue
        }

        if (Test-Path $Candidate -PathType Container) {
            $Candidate = Join-Path $Candidate 'index.html'
        }

        if (-not (Test-Path $Candidate -PathType Leaf)) {
            $Context.Response.StatusCode = 404
            $Bytes = [System.Text.Encoding]::UTF8.GetBytes('404 - Datei nicht gefunden')
            $Context.Response.ContentType = 'text/plain; charset=utf-8'
            $Context.Response.ContentLength64 = $Bytes.Length
            $Context.Response.OutputStream.Write($Bytes, 0, $Bytes.Length)
            $Context.Response.Close()
            continue
        }

        $Bytes = [System.IO.File]::ReadAllBytes($Candidate)
        $Context.Response.StatusCode = 200
        $Context.Response.ContentType = Get-ContentType $Candidate
        $Context.Response.ContentLength64 = $Bytes.Length
        $Context.Response.OutputStream.Write($Bytes, 0, $Bytes.Length)
        $Context.Response.Close()
    }
} finally {
    if ($Listener.IsListening) {
        $Listener.Stop()
    }
    $Listener.Close()
}
