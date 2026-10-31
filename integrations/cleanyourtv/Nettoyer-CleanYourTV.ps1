param(
    [string]$TV = '10.114.7.230:5555',
    [string]$AdbPath = "$env:USERPROFILE\TV-tools\platform-tools\adb.exe"
)
. (Join-Path $PSScriptRoot 'Support.ps1')
$list = Join-Path $PSScriptRoot 'nettoyage-limite.txt'
$approved = @('com.google.android.feedback','com.miui.tv.analytics','com.xiaomi.statistic','fusion.android.tv.demo')
$requested = @(Get-Content $list | ForEach-Object { ($_ -replace '#.*$','').Trim() } | Where-Object { $_ })
if ($requested.Count -ne 4 -or @(Compare-Object ($approved | Sort-Object) ($requested | Sort-Object)).Count) {
    throw 'Liste modifiee : arret. Ne pas appliquer la liste complete du modele.'
}
$upstream = Join-Path $PSScriptRoot 'upstream'
$python = Join-Path $upstream 'tools\venv\Scripts\python.exe'
if (!(Test-Path $python)) {
    $launcher = Get-Command py -ErrorAction SilentlyContinue
    $runner = Get-Command python -ErrorAction SilentlyContinue
    $venv = Join-Path $upstream 'tools\venv'
    if ($launcher) { & $launcher.Source -3 -m venv $venv }
    elseif ($runner -and $runner.Source -notmatch 'WindowsApps') { & $runner.Source -m venv $venv }
    else { throw 'Python 3 est necessaire au nettoyage officiel. Installez-le depuis python.org/downloads/windows. Aucun changement sur la TV.' }
    if ($LASTEXITCODE -ne 0 -or !(Test-Path $python)) { throw 'Python 3/venv indisponible : aucune modification TV.' }
}
& $python -m pip install --require-virtualenv -r (Join-Path $upstream 'tools\requirements.txt')
if ($LASTEXITCODE -ne 0) { throw 'Dependances Python non installees : aucune modification TV.' }
Initialize-CytSession 'nettoyage-cyt'
Save-CytBackup
Save-CytScreenshot 'avant-nettoyage'
$env:TV_ADB_BINARY = $AdbPath
$env:TV_HOST = $TV
$restore = Join-Path $upstream 'tools\restore.ps1'
$undo = Join-Path $script:CytSession 'ANNULER-NETTOYAGE.ps1'
@(
    ('$env:TV_ADB_BINARY = ' + (Quote-Cyt $AdbPath)),
    ('$env:TV_HOST = ' + (Quote-Cyt $TV)),
    ('$adb = ' + (Quote-Cyt $AdbPath)),
    ('& ' + (Quote-Cyt $restore) + ' -List ' + (Quote-Cyt $list) + ' -TvHost ' + (Quote-Cyt $TV) + ' -Stock ' + (Quote-Cyt ($script:CytPreviousHome.Split('/')[0]))),
    'if ($LASTEXITCODE -ne 0) { throw "Restauration officielle non confirmee : arret." }',
    ('& $adb -s ' + (Quote-Cyt $TV) + ' shell cmd package set-home-activity --user 0 ' + (Quote-Cyt $script:CytPreviousHome))
) | Set-Content $undo -Encoding UTF8
Write-Host "Annulation officielle preparee : $undo"
$monitor = Start-Process -FilePath $AdbPath -ArgumentList @('-s',$TV,'logcat','-v','time','-T','1') -WindowStyle Hidden -PassThru -RedirectStandardOutput (Join-Path $script:CytSession 'logcat-nettoyage.txt') -RedirectStandardError (Join-Path $script:CytSession 'logcat-nettoyage.err')
try {
    & (Join-Path $upstream 'tools\disable.ps1') -List $list -TvHost $TV | ForEach-Object {
        $line = '[{0}] {1}' -f (Get-Date -Format 'yyyy-MM-dd HH:mm:ss'), $_
        Write-Host $line
        Add-Content (Join-Path $script:CytSession 'nettoyage.txt') $line
    }
    $cleanExit = $LASTEXITCODE
} finally {
    if (!$monitor.HasExited) { Stop-Process -Id $monitor.Id -ErrorAction SilentlyContinue }
}
if ($cleanExit -ne 0) { throw "Nettoyage non confirme. Conservez le resultat et $undo" }
$disabled = @(Invoke-CytAdb -Arguments @('shell','pm','list','packages','-d','--user','0'))
$disabled | Set-Content (Join-Path $script:CytSession 'packages-desactives-apres.txt')
foreach ($pkg in $approved) {
    if ($script:CytPackages -contains "package:$pkg" -and $disabled -notcontains "package:$pkg") { throw "Desactivation non confirmee pour $pkg : ne pas relancer en boucle." }
}
Invoke-CytAdb -Arguments @('shell','sync') | Out-Null
Save-CytScreenshot 'apres-nettoyage'
Write-Host 'NETTOYAGE LIMITE VERIFIE. Les services TV, Wi-Fi, lanceurs et mises a jour sont conserves.' -ForegroundColor Green
Write-Host "Annulation : $undo"
