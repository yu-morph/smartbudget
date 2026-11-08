# Azure App Service F1 배포 안내

## 확정 사항과 현재 상태

2026-09-18 결정: **Azure App Service의 Linux Web App을 Free F1 App Service Plan에 배포한다.** FastAPI 백엔드, 테스트 HTML, 변환된 거래 JSON을 하나의 앱에 포함한다.

이 문서는 배포 계획과 실행 시 확인할 절차입니다. 아직 Azure 리소스를 생성하거나 실제 배포를 검증하지 않았습니다. 구독, 리전, 최종 리소스 이름, Python 런타임 버전, 공개 URL은 배포 시 확정합니다. GitHub 자동 배포도 아직 구성하지 않았습니다.

## 리소스 구성

| 리소스 | 용도 | 이름 예시(미생성) |
| --- | --- | --- |
| Resource Group | 과제용 리소스 묶음 | `rg-smartbudget-mock` |
| App Service Plan | Linux 실행 자원, SKU F1 | `asp-smartbudget-mock-f1` |
| Web App | FastAPI와 테스트 화면 실행 | `smartbudget-mock-api-고유접미사` |

DB, Storage Account, Static Web Apps, Container Registry, VM은 필요하지 않습니다. 데이터 JSON을 배포 파일에 포함하고 읽기 전용으로 사용합니다. 컨테이너 이미지 대신 Python 코드 배포를 사용합니다.

하나의 HTTPS 기본 도메인에서 `/`는 테스트 화면, `/transactions`는 API, `/docs`는 API 문서를 제공합니다. 화면이 상대 경로로 API를 호출하므로 배포 도메인을 HTML에 하드코딩하지 않습니다.

## F1 제약과 비용 확인

- Linux F1은 공유 컴퓨팅을 사용하며 공식 가격표 기준 하루 60 CPU분, 메모리 1GB, 저장 공간 1GB입니다. CPU분은 접속 가능한 경과시간 60분이라는 뜻이 아닙니다.
- Free 계층은 개발·테스트용입니다. 지속적인 서비스 가용성이나 발표 순간의 응답 속도를 보장하지 않습니다.
- F1에서는 Always On을 사용할 수 없습니다. 유휴 후 앱이 내려가면 다음 접속에서 다시 시작하느라 지연될 수 있습니다.
- 플랜이 무료여도 별도로 생성한 유료 리소스나 모니터링 서비스까지 무료라는 뜻은 아닙니다. 추가 리소스는 필요성과 비용을 확인한 뒤 생성합니다.
- 구독과 리전에서 Linux F1을 만들 수 있는지 생성 화면에서 확인합니다. F1을 선택할 수 없으면 중단하고 리전·구독 조건을 확인합니다. B1 등 유료 SKU로 임의 변경하지 않습니다.

근거: [Linux App Service 가격표](https://azure.microsoft.com/en-us/pricing/details/app-service/linux/), [App Service 플랜](https://learn.microsoft.com/en-us/azure/app-service/overview-hosting-plans), [앱 설정과 Always On](https://learn.microsoft.com/en-us/azure/app-service/configure-common). 확인 기준은 2026-09-18이며 배포 시 포털의 실제 제공 조건을 다시 확인합니다.

## 배포 전 로컬 확인

```bash
.venv/bin/pytest -v
git diff --check
.venv/bin/uvicorn app:app --host 127.0.0.1 --port 8000
```

이미 같은 포트에 서버가 실행 중이면 새 서버를 띄우지 않습니다. `/`, `/docs`, `/transactions`를 확인하고 종료할 때는 `Ctrl+C`를 사용합니다.

배포 파일에는 최소한 다음이 있어야 합니다.

```text
app.py
index.html
data/processed/transactions.json
의존성 설치 파일
```

`.venv`, `.git`, 캐시, 로컬 자격증명은 배포하지 않습니다. 원본 CSV와 변환 스크립트는 서버 실행 자체에는 필요하지 않습니다. Azure에서 매 요청마다 데이터 변환기를 실행하지 않습니다.

### 의존성 설치 준비

현재 저장소는 `pyproject.toml`로 의존성을 관리합니다. Azure 공식 문서에서는 잠금 파일 없는 `pyproject.toml`을 Poetry로 처리할 수 있다고 설명하므로, 로컬 pip 설치 성공만으로 Azure 빌드 성공을 가정하지 않습니다.

저장소 루트의 `requirements.txt`가 Azure 빌드에서 pip 설치에 사용하는 최소 실행 의존성입니다. 버전 범위는 현재 `pyproject.toml`과 일치하며 의존성 변경 시 함께 맞춰야 합니다.

```text
fastapi>=0.115,<1
uvicorn>=0.30,<1
```

테스트용 `pytest`, `httpx`는 실행 서버에 필요하지 않습니다. Python 런타임은 프로젝트 요구 사항인 3.11 이상 중 App Service에서 제공되는 버전을 선택하고 해당 버전으로 테스트합니다. 로컬 `.venv`를 Azure에 복사하지 않습니다.

근거: [Python 앱의 빌드 및 의존성 설치](https://learn.microsoft.com/en-us/azure/app-service/configure-language-python).

## Azure Portal 설정 순서

1. 사용할 Azure 구독을 선택하고 과제 전용 Resource Group을 생성합니다.
2. App Services에서 Web App 만들기를 선택합니다.
3. 게시 방식은 **코드**, 운영체제는 **Linux**, 런타임은 지원되는 **Python 3.11 이상**으로 지정합니다.
4. 해당 구독에서 **Free F1**을 지원하는 리전을 고릅니다. App Service Plan도 F1인지 최종 생성 화면에서 확인합니다.
5. 앱 이름은 사용 가능한 고유 이름으로 정합니다. 실제 접속 주소는 생성 후 개요의 기본 도메인을 복사합니다.
6. 유료 DB, 별도 저장소, 불필요한 모니터링 리소스를 함께 생성하지 않습니다.
7. Web App 설정에서 HTTPS Only를 켭니다.
8. 시작 명령을 아래와 같이 지정합니다.

```bash
python -m uvicorn app:app --host 0.0.0.0 --port 8000
```

`app:app`은 루트 `app.py`의 FastAPI 객체 `app`을 뜻합니다. 이미 의존성에 있는 Uvicorn을 사용하며 단일 프로세스로 실행합니다. `--reload`는 사용하지 않습니다.

빌드 자동화를 사용하는 배포에서는 앱 설정 `SCM_DO_BUILD_DURING_DEPLOYMENT=true`를 확인합니다. 빌드가 의존성을 설치한 뒤 시작 명령이 실행되어야 합니다. 런타임의 앱 위치는 플랫폼이 관리하므로 `/home/site/wwwroot`를 코드에 하드코딩하지 않습니다.

## 코드 전달

이번 초기 배포는 Azure CLI의 `az webapp deploy --type zip`을 사용했습니다. 실행에 필요한 `app.py`, `index.html`, `requirements.txt`, `data/processed/transactions.json`만 ZIP에 포함했습니다. 배포는 원격 앱의 기존 내용을 교체할 수 있으므로 대상 이름과 구독을 확인합니다.

이 방법과 별개로 GitHub Actions 자동 배포도 가능하지만 현재는 미구성입니다. 사용할 경우 `yu-morph/smartbudget-mock-api`의 실제 배포 브랜치를 정하고 변경 사항을 커밋·푸시해야 합니다. 로컬의 미커밋 파일은 GitHub 기반 배포에 포함되지 않습니다. 자동 배포를 원한다는 별도 결정 없이 워크플로를 추가하지 않습니다.

참고: [Python 앱 배포 공식 빠른 시작](https://learn.microsoft.com/en-us/azure/app-service/quickstart-python). 공식 예시의 B1을 그대로 따라 하지 말고 이 프로젝트의 확정 SKU인 F1을 유지합니다.

## 배포 후 확인표

배포 작업의 성공 표시만으로 완료로 간주하지 않고 실제 공개 주소에서 확인합니다.

- [ ] App Service Plan의 SKU가 F1이다.
- [ ] HTTPS `/`에서 테스트 화면이 열린다.
- [ ] `/transactions`가 `200`이며 현재 데이터 기준 `count`가 873이다.
- [ ] `/docs`에서 API 문서가 열린다.
- [ ] 날짜 `2026-12-31` 조회 결과가 해당 날짜로만 구성된다.
- [ ] 기간 `2026-01-01`~`2026-12-31`, `food`, `EXPENSE` 복합 조회가 된다.
- [ ] 존재하지 않는 설명 검색은 `200`과 빈 목록을 반환한다.
- [ ] 시작일이 종료일보다 늦으면 `400`이다.
- [ ] 브라우저에서 달력·드롭다운·초기화가 동작한다.
- [ ] 공개 URL, 리전, Python 버전, 배포한 커밋과 확인일을 아래 기록에 남긴다.

## 문제 해결

| 증상 | 확인할 내용 |
| --- | --- |
| 기본 환영 페이지만 보임 | 코드가 올바른 앱에 배포됐는지, 시작 명령이 저장됐는지 확인 |
| Application Error 또는 5xx | 배포 로그와 Log stream에서 시작 오류 확인 |
| fastapi/uvicorn 모듈 없음 | 의존성 파일의 루트 위치, 빌드 자동화, 설치 로그 확인 |
| JSON 파일을 찾지 못함 | `data/processed/transactions.json`이 배포에 포함됐는지 확인 |
| HTML 화면만 실패 | `index.html`이 `app.py`와 같은 폴더에 있는지 확인 |
| 오랜만에 접속할 때 느림 | F1 유휴 후 재시작 가능성 확인, 잠시 후 재시도 |
| 쿼터 관련 오류 | 포털의 사용량·쿼터 상태 확인, 유료 업그레이드는 별도 결정 |
| 시작일만 조회했는데 결과가 다름 | 서버의 오늘 기준임을 확인하고 종료일도 명시 |
| 다른 웹 앱에서만 호출 실패 | 브라우저 콘솔의 CORS 오류 확인, 허용 출처 설정 필요 |

발표 전 실제 URL로 미리 접속해 화면과 조회를 확인하고, 네트워크·F1 제약에 대비해 로컬 실행 방법도 준비합니다. F1은 안정적인 상시 운영용 서비스로 설명하지 않습니다.

## 실제 배포 기록

| 항목 | 상태 |
| --- | --- |
| 배포 플랫폼 / SKU | Azure App Service Linux / Free F1 확정 |
| 배포 상태 | 2026-09-18 배포·공개 확인 완료 |
| 구독 / 리전 | Visual Studio Enterprise 구독 / Korea Central |
| 리소스 그룹 / 플랜 / 앱 이름 | `smartbudget` / `smartbudget-f1` / `smartbudget-mock-api-yoon-20260918` |
| Python 런타임 | 3.14 |
| 공개 URL | <https://smartbudget-mock-api-yoon-20260918.azurewebsites.net/> |
| HTTPS 전용 | 적용됨 |
| 실제 확인 | `/` 200, `/transactions` 873건, 복합 필터 30건, 잘못된 기간 400, `/docs` 200 |
| 배포 커밋 / 확인일 | `083eab4` 기반 ZIP / 2026-09-18 |

과제 종료 후 정리할 때는 과제 전용 리소스인지 확인한 후 삭제합니다. 공유 Resource Group을 통째로 삭제하면 다른 서비스도 삭제되므로 피합니다.
