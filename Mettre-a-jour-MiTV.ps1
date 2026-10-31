param(
    [string]$TV = '10.114.7.230:5555',
    [string]$AdbPath = "$env:USERPROFILE\TV-tools\platform-tools\adb.exe"
)
$ErrorActionPreference = 'Stop'
$apk = Join-Path $PSScriptRoot 'BarcaHome.apk'
if (!(Test-Path $AdbPath) -or !(Test-Path $apk) -or !(Test-Path "$apk.sha256")) {
    throw 'ADB, APK ou empreinte introuvable. Gardez les fichiers du ZIP ensemble.'
}
$expected = ((Get-Content "$apk.sha256" -Raw).Trim() -split '\s+')[0]
if ((Get-FileHash $apk -Algorithm SHA256).Hash -ne $expected) { throw 'Empreinte APK incorrecte.' }
$session = Join-Path $PSScriptRoot ('sauvegarde-mitv-' + (Get-Date -Format yyyyMMdd-HHmmss-fff))
New-Item -ItemType Directory $session | Out-Null
$script:step = 0
function Invoke-TVCommand {
    param([string[]]$Arguments)
    $script:step++
    $line = '[{0}] adb -s {1} {2}' -f (Get-Date -Format 'yyyy-MM-dd HH:mm:ss'), $TV, ($Arguments -join ' ')
    Write-Host $line -ForegroundColor Cyan
    Add-Content (Join-Path $session 'commandes.txt') $line
    $log = Start-Process -FilePath $AdbPath -ArgumentList @('-s',$TV,'logcat','-v','time','-T','1') -WindowStyle Hidden -PassThru -RedirectStandardOutput (Join-Path $session "logcat-$script:step.txt") -RedirectStandardError (Join-Path $session "logcat-$script:step.err")
    try {
        $result = & $AdbPath -s $TV @Arguments
        if ($LASTEXITCODE -ne 0) { throw 'Commande echouee. Arret : conservez ce resultat.' }
        $result | ForEach-Object { Write-Host $_ }
        return $result
    } finally {
        if (!$log.HasExited) { Stop-Process -Id $log.Id -ErrorAction SilentlyContinue }
        Get-Content (Join-Path $session "logcat-$script:step.txt") -Tail 3 -ErrorAction SilentlyContinue | ForEach-Object { Write-Host "[logcat] $_" -ForegroundColor DarkGray }
    }
}

function Quote-PSLiteral([string]$Value) { return "'" + $Value.Replace("'", "''") + "'" }
$deviceState = ((Invoke-TVCommand -Arguments @('get-state')) -join '').Trim()
if ($deviceState -ne 'device') { throw 'TV non autorisee : arret.' }
$installedPaths = @(Invoke-TVCommand -Arguments @('shell','pm','path','com.mehdi.barcahome'))
if ($installedPaths.Count -ne 1 -or $installedPaths[0] -notmatch '^package:(/data/app/[^\s]+\.apk)$') {
    throw 'Installation precedente introuvable ou multiple : arret.'
}
$oldApkPath = $Matches[1]
$oldApkBackup = Join-Path $session 'BarcaHome-avant.apk'
Invoke-TVCommand -Arguments @('pull',$oldApkPath,$oldApkBackup) | Out-Null
if (!(Test-Path $oldApkBackup) -or (Get-Item $oldApkBackup).Length -eq 0) {
    throw 'Sauvegarde APK incomplete : arret.'
}
(Get-FileHash $oldApkBackup -Algorithm SHA256).Hash | Set-Content (Join-Path $session 'apk-avant.sha256')
$undo = Join-Path $session 'ANNULER-MISE-A-JOUR.ps1'
@(
    '$ErrorActionPreference = "Stop"',
    ('$adb = ' + (Quote-PSLiteral $AdbPath)),
    ('$tv = ' + (Quote-PSLiteral $TV)),
    ('& $adb -s $tv install -r ' + (Quote-PSLiteral $oldApkBackup)),
    'if ($LASTEXITCODE -ne 0) { throw "Restauration APK echouee : arret." }',
    '& $adb -s $tv shell am force-stop com.mehdi.barcahome',
    '& $adb -s $tv shell am start -n com.mehdi.barcahome/.HomeActivity'
) | Set-Content $undo -Encoding UTF8
$result = Invoke-TVCommand -Arguments @('install','-r',$apk)
if (($result -join "`n") -notmatch '(?m)^Success\s*$') { throw 'Mise a jour non confirmee. Sauvegarde conservee.' }
Invoke-TVCommand -Arguments @('shell','am','force-stop','com.mehdi.barcahome') | Out-Null
Invoke-TVCommand -Arguments @('shell','am','start','-n','com.mehdi.barcahome/.HomeActivity') | Out-Null
Write-Host "Mi TV Home 1.3 ouvert. Photo et accueil par defaut conserves. Annulation : $undo" -ForegroundColor Green
