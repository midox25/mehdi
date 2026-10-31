$ErrorActionPreference = 'Stop'
function Initialize-CytSession([string]$Kind) {
    if (!(Test-Path -LiteralPath $AdbPath)) { throw 'ADB introuvable : verifiez le chemin des platform-tools.' }
    $script:CytStamp = Get-Date -Format yyyyMMdd-HHmmss-fff
    $script:CytSession = Join-Path $PSScriptRoot ("sauvegarde-$Kind-$script:CytStamp")
    New-Item -ItemType Directory -Path $script:CytSession | Out-Null
    $script:CytStep = 0
    $state = ((Invoke-CytAdb -Arguments @('get-state')) -join '').Trim()
    if ($state -ne 'device') { throw 'TV non autorisee : arret.' }
    $model = ((Invoke-CytAdb -Arguments @('shell','getprop','ro.product.model')) -join '').Trim()
    if ($model -notmatch '^MiTV[-_]MOSR4$') { throw "Modele inattendu ($model) : aucune modification." }
    $sdk = ((Invoke-CytAdb -Arguments @('shell','getprop','ro.build.version.sdk')) -join '').Trim()
    if ($sdk -notmatch '^\d+$' -or [int]$sdk -lt 26) { throw 'Version Android incompatible : aucune modification.' }
    Write-Host "Modele : $model ; SDK : $sdk" -ForegroundColor Green
}
function Invoke-CytAdb {
    param([string[]]$Arguments, [switch]$Afficher)
    $script:CytStep++
    $line = '[{0}] adb -s {1} {2}' -f (Get-Date -Format 'yyyy-MM-dd HH:mm:ss'), $TV, ($Arguments -join ' ')
    Write-Host $line -ForegroundColor Cyan
    Add-Content (Join-Path $script:CytSession 'commandes.txt') $line
    $log = Start-Process -FilePath $AdbPath -ArgumentList @('-s',$TV,'logcat','-v','time','-T','1') -WindowStyle Hidden -PassThru -RedirectStandardOutput (Join-Path $script:CytSession "logcat-$script:CytStep.txt") -RedirectStandardError (Join-Path $script:CytSession "logcat-$script:CytStep.err")
    try {
        $result = & $AdbPath -s $TV @Arguments
        if ($LASTEXITCODE -ne 0) { throw 'Commande ADB echouee : arret, ne relancez pas en boucle.' }
        if ($Afficher) { $result | ForEach-Object { Write-Host $_ } }
        return $result
    } finally {
        if (!$log.HasExited) { Stop-Process -Id $log.Id -ErrorAction SilentlyContinue }
    }
}
function Save-CytBackup {
    $script:CytPackages = @(Invoke-CytAdb -Arguments @('shell','pm','list','packages','--user','0'))
    $script:CytPackages | Set-Content (Join-Path $script:CytSession 'packages.txt')
    Invoke-CytAdb -Arguments @('shell','pm','list','packages','-d','--user','0') | Set-Content (Join-Path $script:CytSession 'packages-desactives.txt')
    Invoke-CytAdb -Arguments @('shell','getprop') | Set-Content (Join-Path $script:CytSession 'getprop-prive.txt')
    foreach ($zone in @('global','secure','system')) {
        Invoke-CytAdb -Arguments @('shell','settings','list',$zone) | Set-Content (Join-Path $script:CytSession "settings-$zone-prive.txt")
    }
    $answer = @(Invoke-CytAdb -Arguments @('shell','cmd','package','resolve-activity','--brief','-a','android.intent.action.MAIN','-c','android.intent.category.HOME'))
    $answer | Set-Content (Join-Path $script:CytSession 'accueil-avant.txt')
    $components = @($answer | Where-Object { $_ -match '^[A-Za-z0-9_.]+/[A-Za-z0-9_.$]+$' })
    if ($components.Count -ne 1 -or $components[0] -match 'ResolverActivity|RecoveryActivity|FallbackHome') {
        throw 'Accueil actuel ambigu : arret avant modification.'
    }
    $script:CytPreviousHome = $components[0]
    foreach ($pkg in @('com.google.android.feedback','com.miui.tv.analytics','com.xiaomi.statistic','fusion.android.tv.demo')) {
        Invoke-CytAdb -Arguments @('shell','dumpsys','package',$pkg) | Set-Content (Join-Path $script:CytSession "$pkg-avant.txt")
    }
}
function Save-CytScreenshot([string]$Name) {
    $remote = "/sdcard/cyt-$script:CytStamp-$Name.png"
    Invoke-CytAdb -Arguments @('shell','screencap','-p',$remote) | Out-Null
    $file = Join-Path $script:CytSession "$Name.png"
    Invoke-CytAdb -Arguments @('pull',$remote,$file) | Out-Null
    Invoke-CytAdb -Arguments @('shell','rm','-f',$remote) | Out-Null
    if (!(Test-Path $file) -or (Get-Item $file).Length -lt 8) { throw 'Capture vide : utilisez une photo de la TV avant de poursuivre.' }
    $bytes = [System.IO.File]::ReadAllBytes($file)
    if ($bytes.Length -lt 20 -or [Convert]::ToBase64String($bytes,0,8) -ne 'iVBORw0KGgo=' -or
        [Text.Encoding]::ASCII.GetString($bytes,$bytes.Length-8,4) -ne 'IEND') { throw 'Capture PNG incomplete : arret.' }
    Write-Host "Capture : $file"
}
function Quote-Cyt([string]$Value) { return "'" + $Value.Replace("'", "''") + "'" }
