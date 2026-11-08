# GitHub Actions 운영 안내

## dev 브랜치 검증

`dev` 브랜치에 커밋이 추가되면 `dev 테스트 및 빌드` 워크플로가 실행된다. 이 워크플로는 자동 포맷, 로컬 단위 테스트, ktlint, detekt를 수행하고 자동 포맷이 커밋에 반영됐는지 확인한 뒤 디버그 APK를 빌드한다. 빌드된 디버그 APK는 워크플로 아티팩트로 14일간 보관한다.

## 릴리즈 빌드

GitHub 릴리즈가 게시되면 `릴리즈 앱 빌드 및 첨부` 워크플로가 릴리즈의 태그가 가리키는 커밋을 체크아웃한다. 태그는 `v1.0.0` 형식이어야 한다. 앱의 `versionName`은 `1.0.0`, `versionCode`는 주 버전, 부 버전, 패치 버전으로 계산한 정수가 된다.

빌드된 APK와 AAB에는 `arm64-v8a`, `armeabi-v7a` ABI가 포함된다. 워크플로는 서명된 릴리즈 APK와 AAB를 생성해 워크플로 아티팩트로 30일간 보관하고, APK만 해당 GitHub 릴리즈의 첨부 파일로 업로드한다.

## 필요한 GitHub Actions secret

저장소의 Settings, Secrets and variables, Actions에서 다음 repository secret을 등록한다.

- `ANDROID_KEYSTORE_BASE64`: JKS 또는 keystore 파일을 Base64로 인코딩한 값.
- `ANDROID_KEYSTORE_PASSWORD`: 키 저장소 비밀번호.
- `ANDROID_KEY_ALIAS`: 서명 키 별칭.
- `ANDROID_KEY_PASSWORD`: 서명 키 비밀번호.

서명 키는 저장소에 커밋하지 않는다. 동일한 앱의 업데이트를 배포하려면 모든 릴리즈에서 같은 서명 키를 안전하게 보관하고 사용해야 한다.
