# SmartBudget 앱 아이콘 원본

- `smartbudget-icon.svg`: 편집 가능한 벡터 원본.
- `smartbudget-icon.png`: 고해상도 래스터 원본.
- `smartbudget-icon-round.svg`: 원형 레거시 아이콘을 생성하기 위한 벡터 원본.

Android 런처 아이콘은 이 원본을 바탕으로 `app/src/main/res` 아래에 구성한다. 일반 아이콘과 원형 아이콘은 화면 밀도별 PNG로 제공하고, adaptive icon은 배경·전경·단색 레이어를 분리해 시스템 마스크와 테마 아이콘을 지원한다.

핵심 콘텐츠는 Android adaptive icon 안전 영역 안에서 원형·스쿼클 마스크에 잘리지 않도록 원본 캔버스의 50% 크기로 배치한다. 화면 밀도별 PNG는 고해상도 벡터 렌더링 결과를 안티앨리어싱이 적용된 고품질 보간으로 축소해 생성한다.
