$ErrorActionPreference = 'Stop'
Set-Location (Join-Path $PSScriptRoot '../..')
Write-Host 'ViaForge Testlabor - 48 lokale Vanilla-Server' -ForegroundColor Cyan
Write-Host 'Auswahl: z.B. 26.2 oder 1.12.2,1.13.2 oder regression / legacy / modern'
Write-Host 'Alle 48 gleichzeitig benoetigen mehr Arbeitsspeicher als dieser PC hat.'
while ($true) {
    Write-Host ''
    $action = Read-Host 'start / stop / status / build / verify / op / kit / find / adressen / ende'
    if ($action -eq 'ende') { break }
    if ($action -eq 'adressen') {
        Start-Process (Join-Path (Get-Location) 'run/test-servers/index.html')
        continue
    }
    if ($action -notin @('start','stop','status','build','verify','op','kit','find')) { continue }
    $defaultGroup = if ($action -eq 'start') { 'regression' } else { 'all' }
    $selection = Read-Host "Version(en) oder Gruppe; leer = $defaultGroup"
    if ([string]::IsNullOrWhiteSpace($selection)) { $selection = $defaultGroup }
    $extra = @()
    if ($action -in @('op','kit')) { $extra = @((Read-Host 'Dein Minecraft-Spielername')) }
    if ($action -eq 'find') { $extra = @((Read-Host 'Block, Item oder Mob suchen (englische ID, z.B. shulker)')) }
    & py -3 (Join-Path $PSScriptRoot 'lab.py') $action $selection @extra
}
