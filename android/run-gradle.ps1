param(
    [Parameter(ValueFromRemainingArguments = $true)]
    [string[]] $GradleArgs
)

$gradleBat = Get-ChildItem "$env:USERPROFILE\.gradle\wrapper\dists" -Recurse -Filter gradle.bat |
    Sort-Object FullName -Descending |
    Select-Object -First 1

if (-not $gradleBat) {
    Write-Error "Local Gradle distribution was not found. Open the project in Android Studio and sync it, or install Gradle."
    exit 1
}

& $gradleBat.FullName "-Dorg.gradle.problems.report=false" @GradleArgs
exit $LASTEXITCODE
