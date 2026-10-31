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
Add-Type -AssemblyName System.Windows.Forms
$picker = New-Object System.Windows.Forms.OpenFileDialog
$picker.Title = 'Choisissez votre photo de Barcelone (JPG ou PNG)'
$picker.Filter = 'Images|*.jpg;*.jpeg;*.png'
$picker.CheckFileExists = $true
try {
    if ($picker.ShowDialog() -ne [System.Windows.Forms.DialogResult]::OK) {
        Write-Host 'Annule : aucune modification.'
        return
    }
    $selectedPhoto = $picker.FileName
} finally { $picker.Dispose() }
if ((Get-Item -LiteralPath $selectedPhoto).Length -gt 30MB) { throw 'Choisissez une image de moins de 30 Mo.' }
$session = Join-Path $PSScriptRoot ('sauvegarde-barcelona-' + (Get-Date -Format yyyyMMdd-HHmmss))
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
Invoke-TVCommand -Arguments @('get-state') | Out-Null
$installedPaths = @(Invoke-TVCommand -Arguments @('shell','pm','path','com.mehdi.barcahome'))
if ($installedPaths.Count -ne 1 -or $installedPaths[0] -notmatch '^package:(/data/app/[^\s]+\.apk)$') {
    throw 'Installation precedente introuvable ou multiple. Arret avant modification.'
}
$oldApkPath = $Matches[1]
$oldApkBackup = Join-Path $session 'BarcaHome-avant.apk'
Invoke-TVCommand -Arguments @('pull',$oldApkPath,$oldApkBackup) | Out-Null
if (!(Test-Path $oldApkBackup) -or (Get-Item $oldApkBackup).Length -eq 0) {
    throw 'Sauvegarde APK incomplete : arret.'
}
(Get-FileHash $oldApkBackup -Algorithm SHA256).Hash | Set-Content (Join-Path $session 'apk-avant.sha256')
$photoRemote = '/sdcard/Android/data/com.mehdi.barcahome/files/background.jpg'
$oldPhotoBackup = Join-Path $session 'fond-avant.jpg'
$photoPresence = ((Invoke-TVCommand -Arguments @('shell',"if [ -f $photoRemote ]; then echo PRESENT; else echo ABSENT; fi")) -join '').Trim()
if ($photoPresence -notin @('PRESENT','ABSENT')) { throw 'Etat du fond precedent inconnu : arret.' }
if ($photoPresence -eq 'PRESENT') {
    Invoke-TVCommand -Arguments @('pull',$photoRemote,$oldPhotoBackup) | Out-Null
}
$undo = Join-Path $session 'ANNULER-MISE-A-JOUR.ps1'
$undoLines = @(
    '$ErrorActionPreference = "Stop"',
    ('$adb = ' + (Quote-PSLiteral $AdbPath)),
    ('$tv = ' + (Quote-PSLiteral $TV)),
    ('& $adb -s $tv install -r ' + (Quote-PSLiteral $oldApkBackup)),
    'if ($LASTEXITCODE -ne 0) { throw "Restauration APK echouee : arret." }',
    '& $adb -s $tv shell am force-stop com.mehdi.barcahome'
)
if ($photoPresence -eq 'PRESENT') {
    $undoLines += ('& $adb -s $tv push ' + (Quote-PSLiteral $oldPhotoBackup) + ' ' + (Quote-PSLiteral $photoRemote))
} else {
    $undoLines += ('& $adb -s $tv shell rm -f ' + (Quote-PSLiteral $photoRemote))
}
$undoLines += 'if ($LASTEXITCODE -ne 0) { throw "Restauration fond echouee." }'
$undoLines += '& $adb -s $tv shell am start -n com.mehdi.barcahome/.HomeActivity'
$undoLines | Set-Content $undo -Encoding UTF8
$result = Invoke-TVCommand -Arguments @('install','-r',$apk)
if (($result -join "`n") -notmatch '(?m)^Success\s*$') { throw 'Mise a jour non confirmee. Sauvegarde conservee.' }
Invoke-TVCommand -Arguments @('shell','mkdir','-p','/sdcard/Android/data/com.mehdi.barcahome/files') | Out-Null
Invoke-TVCommand -Arguments @('push',$selectedPhoto,$photoRemote) | Out-Null
Invoke-TVCommand -Arguments @('shell','touch',$photoRemote) | Out-Null
Invoke-TVCommand -Arguments @('shell','am','force-stop','com.mehdi.barcahome') | Out-Null
Invoke-TVCommand -Arguments @('shell','am','start','-n','com.mehdi.barcahome/.HomeActivity') | Out-Null
Write-Host "Barcelona 1.1 ouvert. Accueil par defaut inchange. Annulation : $undo" -ForegroundColor Green
