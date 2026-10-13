param([ValidateSet('run', 'offline', 'test', 'package', 'scenebuilder')][string]$Action = 'run')
$ErrorActionPreference = 'Stop'
$taskRoot = Split-Path -Parent $PSScriptRoot
Set-Location -LiteralPath $taskRoot

if ($Action -eq 'scenebuilder') {
    $taskSceneBuilder = @(
        (Join-Path $env:LOCALAPPDATA 'SceneBuilder\SceneBuilder.exe'),
        'C:\Program Files\SceneBuilder\SceneBuilder.exe'
    ) | Where-Object { Test-Path -LiteralPath $_ } | Select-Object -First 1
    if (-not $taskSceneBuilder) { throw 'SceneBuilder was not found. Open src/main/resources/com/badminton/view/main.fxml manually.' }
    $taskFxml = Join-Path $taskRoot 'src\main\resources\com\badminton\view\main.fxml'
    Start-Process -FilePath $taskSceneBuilder -ArgumentList ('"' + $taskFxml + '"')
    exit 0
}

$taskJavaCandidates = @()
if ($env:JAVA_HOME) { $taskJavaCandidates += $env:JAVA_HOME }
$taskJdks = Join-Path $env:USERPROFILE '.jdks'
if (Test-Path -LiteralPath $taskJdks) {
    $taskJavaCandidates += Get-ChildItem -LiteralPath $taskJdks -Directory |
        Where-Object { $_.Name -match '17' } | Sort-Object Name -Descending | ForEach-Object { $_.FullName }
    $taskJavaCandidates += Get-ChildItem -LiteralPath $taskJdks -Directory |
        Sort-Object Name -Descending | ForEach-Object { $_.FullName }
}
$taskJavaHome = $taskJavaCandidates | Where-Object { Test-Path -LiteralPath (Join-Path $_ 'bin\javac.exe') } | Select-Object -First 1
if ($taskJavaHome) { $env:JAVA_HOME = $taskJavaHome }
if (-not $taskJavaHome -and -not (Get-Command javac -ErrorAction SilentlyContinue)) { throw 'Install JDK 17 or newer, then set JAVA_HOME.' }

switch ($Action) {
    'offline' {
        $taskJar = Join-Path $taskRoot 'target\badminton-booking-1.0.0.jar'
        if (-not (Test-Path -LiteralPath $taskJar)) { throw 'Packaged JAR was not found. Run project.ps1 -Action package first, or use run.cmd.' }
        $taskJava = if ($taskJavaHome) { Join-Path $taskJavaHome 'bin\java.exe' } else { 'java' }
        & $taskJava '-jar' $taskJar
        exit $LASTEXITCODE
    }
    'test' { & '.\mvnw.cmd' -B -ntp test; exit $LASTEXITCODE }
    'package' {
        & '.\mvnw.cmd' -B -ntp package dependency:copy-dependencies '-DincludeScope=runtime' '-DoutputDirectory=target/lib'
        exit $LASTEXITCODE
    }
    default { & '.\mvnw.cmd' -ntp javafx:run; exit $LASTEXITCODE }
}
