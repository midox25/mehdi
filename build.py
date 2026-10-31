#!/usr/bin/env python3
"""Build with official Android SDK tools; no Gradle or third-party libraries."""
import os, pathlib, subprocess, zipfile, hashlib
ROOT = pathlib.Path(__file__).resolve().parent
SDK = pathlib.Path(os.environ.get('ANDROID_HOME', '/workspace/android-sdk'))
PLATFORM = SDK / 'platforms' / 'android-35' / 'android.jar'
choices = [p for p in (SDK / 'build-tools').iterdir() if (p / 'aapt2').exists()]
TOOLS = sorted(choices)[-1]
OUT = ROOT / 'build'
OUT.mkdir(exist_ok=True)
(OUT / 'classes').mkdir(exist_ok=True)
def run(*args):
    subprocess.run([str(x) for x in args], cwd=ROOT, check=True)
run(TOOLS/'aapt2','compile','--dir',ROOT/'res','-o',OUT/'resources.zip')
run(TOOLS/'aapt2','link','-o',OUT/'base.apk','--manifest',ROOT/'AndroidManifest.xml','-I',PLATFORM,'-R',OUT/'resources.zip','--auto-add-overlay')
run('javac','-encoding','UTF-8','--release','8','-classpath',PLATFORM,'-d',OUT/'classes',*ROOT.glob('src/**/*.java'))
run(TOOLS/'d8','--lib',PLATFORM,'--min-api','26','--output',OUT,*sorted((OUT/'classes').rglob('*.class')))
with zipfile.ZipFile(OUT/'base.apk','a') as apk:
    apk.write(OUT/'classes.dex','classes.dex')
run(TOOLS/'zipalign','-f','4',OUT/'base.apk',OUT/'aligned.apk')
keys = ROOT.parent / '.barca-home-signing'
keys.mkdir(exist_ok=True, mode=0o700)
key = keys/'development.p12'
if not key.exists():
    run('keytool','-genkeypair','-keystore',key,'-storepass','android','-keypass','android','-alias','development','-dname','CN=Barca Home Development','-keyalg','RSA','-keysize','2048','-validity','3650','-noprompt')
    key.chmod(0o600)
apk = OUT/'BarcaHome.apk'
run(TOOLS/'apksigner','sign','--ks',key,'--ks-key-alias','development','--ks-pass','pass:android','--out',apk,OUT/'aligned.apk')
run(TOOLS/'apksigner','verify','--verbose',apk)
run(TOOLS/'zipalign','-c','4',apk)
digest = hashlib.sha256(apk.read_bytes()).hexdigest()
(OUT/'BarcaHome.apk.sha256').write_text(digest+'  BarcaHome.apk\n')
print('APK:',apk,'\nSHA256:',digest)
