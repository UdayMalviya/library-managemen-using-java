$ErrorActionPreference = "Stop"

# Require a full JDK because the script compiles source files before running the app.
$javac = Get-Command javac -ErrorAction SilentlyContinue
if (-not $javac) {
    Write-Host "javac was not found. Install a JDK (not just a JRE) and run this script again."
    exit 1
}

$projectRoot = $PSScriptRoot
$sourceRoot = Join-Path $projectRoot "src"
$outputRoot = Join-Path $projectRoot "out"

New-Item -ItemType Directory -Force -Path $outputRoot | Out-Null

$sourceFiles = Get-ChildItem -Path $sourceRoot -Recurse -Filter *.java | ForEach-Object { $_.FullName }
if (-not $sourceFiles) {
    Write-Host "No Java source files were found under $sourceRoot."
    exit 1
}

# Build into a separate output folder so source files stay untouched.
& $javac.Source -d $outputRoot $sourceFiles
if ($LASTEXITCODE -ne 0) {
    exit $LASTEXITCODE
}

java -cp $outputRoot librarymanagement.Main
exit $LASTEXITCODE
