param(
    [int]$Port = 8000
)

$ErrorActionPreference = 'Stop'
$Root = [System.IO.Path]::GetFullPath((Join-Path $PSScriptRoot '..'))
$BaseUrl = "http://127.0.0.1:$Port/"

function Get-ContentType([string]$Path) {
    switch ([System.IO.Path]::GetExtension($Path).ToLowerInvariant()) {
        '.html'  { return 'text/html; charset=utf-8' }
        '.htm'   { return 'text/html; charset=utf-8' }
        '.js'    { return 'application/javascript; charset=utf-8' }
        '.css'   { return 'text/css; charset=utf-8' }
        '.json'  { return 'application/json; charset=utf-8' }
        '.xml'   { return 'application/xml; charset=utf-8' }
        '.svg'   { return 'image/svg+xml' }
        '.png'   { return 'image/png' }
        '.jpg'   { return 'image/jpeg' }
        '.jpeg'  { return 'image/jpeg' }
        '.gif'   { return 'image/gif' }
        '.ico'   { return 'image/x-icon' }
        '.woff'  { return 'font/woff' }
        '.woff2' { return 'font/woff2' }
        '.ttf'   { return 'font/ttf' }
        default  { return 'application/octet-stream' }
    }
}

function Send-Response($Stream, [int]$StatusCode, [string]$StatusText, [string]$ContentType, [byte[]]$Body) {
    $Header = "HTTP/1.1 $StatusCode $StatusText`r`n" +
              "Content-Type: $ContentType`r`n" +
              "Content-Length: $($Body.Length)`r`n" +
              "Cache-Control: no-store`r`n" +
              "Connection: close`r`n`r`n"
    $HeaderBytes = [System.Text.Encoding]::ASCII.GetBytes($Header)
    $Stream.Write($HeaderBytes, 0, $HeaderBytes.Length)
    if ($Body.Length -gt 0) {
        $Stream.Write($Body, 0, $Body.Length)
    }
    $Stream.Flush()
}

$Server = [System.Net.Sockets.TcpListener]::new([System.Net.IPAddress]::Loopback, $Port)

try {
    $Server.Start()
} catch {
    Write-Host ''
    Write-Host "R4-Server konnte auf Port $Port nicht gestartet werden." -ForegroundColor Red
    Write-Host $_.Exception.Message -ForegroundColor Red
    Write-Host ''
    Write-Host 'Falls bereits ein R4-Server laeuft, oeffne im Browser:'
    Write-Host "  ${BaseUrl}r4.html"
    Read-Host 'Enter zum Beenden'
    exit 1
}

Write-Host 'Blockly@rduino R4 - lokaler Testserver' -ForegroundColor Cyan
Write-Host "Ordner: $Root"
Write-Host "Adresse: ${BaseUrl}r4.html"
Write-Host ''
Write-Host 'Dieses Fenster offen lassen. Zum Beenden Strg+C druecken.' -ForegroundColor Yellow

Start-Process "${BaseUrl}r4.html"

try {
    while ($true) {
        $Client = $Server.AcceptTcpClient()
        $Reader = $null
        $Stream = $null
        try {
            $Stream = $Client.GetStream()
            $Reader = New-Object System.IO.StreamReader($Stream, [System.Text.Encoding]::ASCII, $false, 1024, $true)
            $RequestLine = $Reader.ReadLine()

            if ([string]::IsNullOrWhiteSpace($RequestLine)) {
                continue
            }

            # Consume the remaining HTTP request headers.
            while ($true) {
                $Line = $Reader.ReadLine()
                if ([string]::IsNullOrEmpty($Line)) { break }
            }

            $Parts = $RequestLine.Split(' ')
            if ($Parts.Length -lt 2 -or $Parts[0] -ne 'GET') {
                $Body = [System.Text.Encoding]::UTF8.GetBytes('405 - Nur GET wird unterstuetzt')
                Send-Response $Stream 405 'Method Not Allowed' 'text/plain; charset=utf-8' $Body
                continue
            }

            $RequestTarget = $Parts[1].Split('?')[0]
            $RequestPath = [System.Uri]::UnescapeDataString($RequestTarget.TrimStart('/'))
            if ([string]::IsNullOrWhiteSpace($RequestPath)) {
                $RequestPath = 'r4.html'
            }

            $RelativePath = $RequestPath.Replace('/', [System.IO.Path]::DirectorySeparatorChar)
            $Candidate = [System.IO.Path]::GetFullPath((Join-Path $Root $RelativePath))

            # Prevent ../ path traversal outside the repository folder.
            if (-not $Candidate.StartsWith($Root, [System.StringComparison]::OrdinalIgnoreCase)) {
                $Body = [System.Text.Encoding]::UTF8.GetBytes('403 - Zugriff verweigert')
                Send-Response $Stream 403 'Forbidden' 'text/plain; charset=utf-8' $Body
                continue
            }

            if (Test-Path $Candidate -PathType Container) {
                $Candidate = Join-Path $Candidate 'index.html'
            }

            if (-not (Test-Path $Candidate -PathType Leaf)) {
                $Body = [System.Text.Encoding]::UTF8.GetBytes('404 - Datei nicht gefunden')
                Send-Response $Stream 404 'Not Found' 'text/plain; charset=utf-8' $Body
                continue
            }

            $Body = [System.IO.File]::ReadAllBytes($Candidate)
            Send-Response $Stream 200 'OK' (Get-ContentType $Candidate) $Body
        } catch {
            Write-Host "Anfragefehler: $($_.Exception.Message)" -ForegroundColor DarkYellow
        } finally {
            if ($Reader) { $Reader.Dispose() }
            if ($Stream) { $Stream.Dispose() }
            $Client.Close()
        }
    }
} finally {
    $Server.Stop()
}
