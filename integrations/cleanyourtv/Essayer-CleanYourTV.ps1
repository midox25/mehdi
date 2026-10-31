param(
    [string]$TV = '10.114.7.230:5555',
    [string]$AdbPath = "$env:USERPROFILE\TV-tools\platform-tools\adb.exe"
)
. (Join-Path $PSScriptRoot 'Support.ps1')
$apk = Join-Path $PSScriptRoot 'CleanYourTV.apk'
if (!(Test-Path $apk) -or !(Test-Path "$apk.sha256")) { throw 'APK ou empreinte introuvable : gardez le ZIP extrait complet.' }
$expected = ((Get-Content "$apk.sha256" -Raw).Trim() -split '\s+')[0]
if ((Get-FileHash $apk -Algorithm SHA256).Hash -ne $expected) { throw 'Empreinte APK incorrecte : arret.' }
Initialize-CytSession 'essai-cyt'
Save-CytBackup
if ($script:CytPackages -contains 'package:com.gathin.tvhome') { throw 'CleanYourTV est deja installe : aucune mise a jour automatique de ses donnees.' }
Save-CytScreenshot 'avant'
$undo = Join-Path $script:CytSession 'ANNULER-ESSAI.ps1'
@(
    '$ErrorActionPreference = "Stop"',
    ('$adb = ' + (Quote-Cyt $AdbPath)),
    ('$tv = ' + (Quote-Cyt $TV)),
    ('& $adb -s $tv shell cmd package set-home-activity --user 0 ' + (Quote-Cyt $script:CytPreviousHome)),
    'if ($LASTEXITCODE -ne 0) { throw "Accueil precedent non restaure : arret." }',
    '& $adb -s $tv shell pm disable-user --user 0 com.gathin.tvhome',
    '& $adb -s $tv shell input keyevent KEYCODE_HOME'
) | Set-Content $undo -Encoding UTF8
$choose = Join-Path $script:CytSession 'CHOISIR-CET-ACCUEIL.ps1'
@(
    '$ErrorActionPreference = "Stop"',
    ('$adb = ' + (Quote-Cyt $AdbPath)),
    ('$tv = ' + (Quote-Cyt $TV)),
    '& $adb -s $tv shell cmd package set-home-activity --user 0 com.gathin.tvhome/.MainActivity',
    'if ($LASTEXITCODE -ne 0) { throw "Changement accueil refuse." }',
    '& $adb -s $tv shell input keyevent KEYCODE_HOME'
) | Set-Content $choose -Encoding UTF8
$result = Invoke-CytAdb -Arguments @('install',$apk) -Afficher
if (($result -join "`n") -notmatch '(?m)^Success\s*$') { throw 'Installation non confirmee.' }
$start = Invoke-CytAdb -Arguments @('shell','am','start','-W','-n','com.gathin.tvhome/.MainActivity') -Afficher
if (($start -join "`n") -notmatch 'Status:\s*ok') { throw "Demarrage non confirme. Annulation : $undo" }
Start-Sleep -Seconds 2
Save-CytScreenshot 'apres'
Invoke-CytAdb -Arguments @('shell','dumpsys','window') | Set-Content (Join-Path $script:CytSession 'fenetres-prive.txt')
Write-Host 'ESSAI OUVERT. Aucun lanceur desactive, aucun changement de Home, aucun nettoyage effectue.' -ForegroundColor Green
Write-Host "Annulation : $undo"
Write-Host "Apres verification sur la TV seulement, choix de Home : $choose"
Write-Host 'Envoyez la capture apres.png ou une photo de la TV. Gardez les sauvegardes privees.'
