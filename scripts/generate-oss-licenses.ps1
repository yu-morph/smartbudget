param(
    [string]$JavaHome = "C:\Program Files\Android\Android Studio\jbr"
)

$ErrorActionPreference = "Stop"
$env:JAVA_HOME = $JavaHome

& "$PSScriptRoot\..\gradlew.bat" :app:generateOpenSourceLicenses
if ($LASTEXITCODE -ne 0) {
    throw "오픈소스 라이선스 문서 생성에 실패했습니다."
}

Write-Host "런타임 종속성의 오픈소스 라이선스 문서를 생성했습니다."
