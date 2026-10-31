param(
    [string]$TV = '10.114.7.230:5555',
    [string]$AdbPath = "$env:USERPROFILE\TV-tools\platform-tools\adb.exe",
    [string]$Photo = ''
)
$ErrorActionPreference = 'Stop'
$apk = Join-Path $PSScriptRoot 'BarcaHome.apk'
$hashFile = "$apk.sha256"
if (!(Test-Path $AdbPath) -or !(Test-Path $apk) -or !(Test-Path $hashFile)) {
    throw 'Gardez le script, APK et SHA256 dans le meme dossier. Verifiez le chemin ADB.'
}
$expected = ((Get-Content $hashFile -Raw).Trim() -split '\s+')[0]
if ((Get-FileHash $apk -Algorithm SHA256).Hash -ne $expected) {
    throw 'Empreinte APK incorrecte : arret.'
}
$session = Join-Path $PSScriptRoot ("sauvegarde-" + (Get-Date -Format yyyyMMdd-HHmmss))
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
$state = Invoke-TVCommand -Arguments @('get-state')
if (($state -join '').Trim() -ne 'device') { throw 'TV non autorisee.' }
$packages = Invoke-TVCommand -Arguments @('shell','pm','list','packages')
$packages | Set-Content (Join-Path $session 'packages-avant.txt')
if ($packages -contains 'package:com.mehdi.barcahome') {
    throw 'Barca Home existe deja : aucune mise a jour automatique pour proteger ses donnees.'
}
$previousHomeOutput = Invoke-TVCommand -Arguments @('shell','cmd','package','resolve-activity','--brief','-a','android.intent.action.MAIN','-c','android.intent.category.HOME')
$previousHomeOutput | Set-Content (Join-Path $session 'accueil-avant.txt')
$component = @($previousHomeOutput | Where-Object { $_ -match '^[a-zA-Z0-9_.]+/[a-zA-Z0-9_.$]+$' })
if ($component.Count -ne 1 -or $component[0] -match 'ResolverActivity|ChooserActivity|com.mehdi.barcahome') {
    throw 'Accueil initial ambigu : envoyez accueil-avant.txt avant installation.'
}
$undo = Join-Path $session 'ANNULER.ps1'
@(
    '$ErrorActionPreference = "Stop"',
    "`$adb = '$($AdbPath.Replace("'", "''"))'",
    "`$tv = '$TV'",
    "& `$adb -s `$tv shell cmd package set-home-activity --user 0 '$($component[0])'",
    'if ($LASTEXITCODE -ne 0) { throw "Restauration accueil echouee : arret avant desinstallation." }',
    '& $adb -s $tv uninstall com.mehdi.barcahome',
    '& $adb -s $tv shell input keyevent KEYCODE_HOME'
) | Set-Content $undo
$result = Invoke-TVCommand -Arguments @('install',$apk)
if (($result -join "`n") -notmatch '(?m)^Success\s*$') { throw 'Installation non confirmee.' }
if ($Photo) {
    if (!(Test-Path -LiteralPath $Photo)) { throw 'Photo introuvable. Application installee, fond non transfere.' }
    Invoke-TVCommand -Arguments @('shell','mkdir','-p','/sdcard/Android/data/com.mehdi.barcahome/files') | Out-Null
    Invoke-TVCommand -Arguments @('push',$Photo,'/sdcard/Android/data/com.mehdi.barcahome/files/background.jpg') | Out-Null
}
Invoke-TVCommand -Arguments @('shell','am','start','-n','com.mehdi.barcahome/.HomeActivity') | Out-Null
Write-Host "Application ouverte pour essai. Accueil Xiaomi conserve. Annulation : $undo" -ForegroundColor Green
Write-Host 'Verifiez les tuiles YouTube, Live TV et les reglages avec votre telecommande avant tout changement par defaut.'
