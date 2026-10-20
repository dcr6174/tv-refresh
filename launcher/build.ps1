param([string]$Jdk, [string]$BuildTools, [string]$AndroidJar, [string]$Python = 'python', [string]$Aapt2)
$ErrorActionPreference = 'Stop'
if (!$Jdk -or !$BuildTools -or !$AndroidJar) { throw 'Pass -Jdk <JDK 17 folder> -BuildTools <Android build-tools 34 folder> -AndroidJar <platform 34 android.jar>' }
$Jdk = (Resolve-Path $Jdk).Path
$BuildTools = (Resolve-Path $BuildTools).Path
$AndroidJar = (Resolve-Path $AndroidJar).Path
if (!$Aapt2) { $Aapt2 = "$BuildTools/aapt2.exe" } else { $Aapt2 = (Resolve-Path $Aapt2).Path }
$env:JAVA_HOME = $Jdk
Push-Location $PSScriptRoot
try {
  New-Item -ItemType Directory -Force build/gen,build/classes,build/dex | Out-Null
  & $Aapt2 compile --dir res -o build/res.zip
  if ($LASTEXITCODE -and $LASTEXITCODE -ne -1073741819) { throw 'Resource compilation failed' }
  & $Python repair_archive.py build/res.zip
  if ($LASTEXITCODE) { throw 'Resource archive validation failed' }
  & $Aapt2 link -o build/base.apk -I $AndroidJar -A assets --manifest AndroidManifest.xml --java build/gen --min-sdk-version 28 --target-sdk-version 28 build/res.zip
  if ($LASTEXITCODE -and $LASTEXITCODE -ne -1073741819) { throw 'Resource linking failed' }
  & $Python repair_archive.py build/base.apk
  if ($LASTEXITCODE) { throw 'APK resource archive validation failed' }
  $sources = @(Get-ChildItem src,build/gen -Filter *.java -Recurse | ForEach-Object FullName)
  & "$Jdk/bin/javac.exe" -encoding UTF-8 --release 11 -classpath $AndroidJar -d build/classes @sources
  if ($LASTEXITCODE) { throw 'Java compilation failed' }
  $classes = @(Get-ChildItem build/classes -Filter *.class -Recurse | ForEach-Object FullName)
  & "$Jdk/bin/java.exe" -cp "$BuildTools/lib/d8.jar" com.android.tools.r8.D8 --min-api 28 --lib $AndroidJar --output build/dex @classes
  if ($LASTEXITCODE) { throw 'DEX compilation failed' }
  Copy-Item build/base.apk build/unsigned.apk -Force
  & "$Jdk/bin/jar.exe" uf build/unsigned.apk -C build/dex classes.dex
  if ($LASTEXITCODE) { throw 'APK packaging failed' }
  & $Python repair_archive.py --align build/unsigned.apk build/aligned.apk
  if ($LASTEXITCODE) { throw 'APK alignment failed' }
  if (!(Test-Path signing.keystore)) {
    & "$Jdk/bin/keytool.exe" -genkeypair -keystore signing.keystore -storepass android -keypass android -alias minimalhome -keyalg RSA -keysize 2048 -validity 10000 -dname 'CN=TV Refresh'
    if ($LASTEXITCODE) { throw 'Signing key generation failed' }
  }
  & "$Jdk/bin/java.exe" -jar "$BuildTools/lib/apksigner.jar" sign --ks signing.keystore --ks-pass pass:android --out TVRefresh.apk build/aligned.apk
  if ($LASTEXITCODE) { throw 'APK signing failed' }
  & "$Jdk/bin/java.exe" -jar "$BuildTools/lib/apksigner.jar" verify --verbose TVRefresh.apk
  if ($LASTEXITCODE) { throw 'APK signature verification failed' }
  Get-FileHash TVRefresh.apk -Algorithm SHA256
} finally { Pop-Location }
