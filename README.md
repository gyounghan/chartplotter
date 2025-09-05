# Chart Plotter - 간단한 지도 앱

OpenStreetMap을 사용하여 지도를 표시하는 간단한 차트 플로터 앱입니다.

## 기능

- OpenStreetMap을 사용한 지도 표시
- 한국 근해 기본 위치 설정 (위도 35.0, 경도 128.0)
- Jetpack Compose를 사용한 현대적인 UI
- OSMDroid를 사용한 고성능 지도 렌더링

## 설치 및 설정

### 1. 앱 빌드 및 실행

```bash
./gradlew assembleDebug
```

### 2. 앱 실행

빌드된 APK를 Android 기기에 설치하고 실행하세요.

## 사용법

1. 앱을 실행하면 한국 근해(위도 35.0, 경도 128.0)가 기본 위치로 설정됩니다
2. 지도를 터치하여 확대/축소하고 이동할 수 있습니다
3. OpenStreetMap 데이터를 사용하여 실제 지형과 해안선을 확인할 수 있습니다

## 기술 스택

- **언어**: Kotlin
- **UI 프레임워크**: Jetpack Compose
- **지도 라이브러리**: OSMDroid (OpenStreetMap)
- **최소 SDK**: API 33 (Android 13)
- **타겟 SDK**: API 34 (Android 14)
- **컴파일 SDK**: API 35

## 파일 구조

```
app/src/main/
├── java/com/marineplay/chartplotter/
│   ├── MainActivity.kt           # 메인 액티비티 (지도 표시)
│   └── MBTilesManager.kt        # MBTiles 파일 관리 (향후 확장용)
├── assets/
│   └── mbtiles/                 # MBTiles 파일들 (향후 추가 예정)
└── AndroidManifest.xml          # 앱 매니페스트
```

## 향후 확장 계획

이 앱은 향후 다음과 같은 기능을 추가할 예정입니다:

1. **MBTiles 지원**: 해상도, 항해도, 해저지형 MBTiles 파일 로드
2. **레이어 관리**: 다양한 지도 레이어 추가/제거
3. **위치 추적**: GPS를 사용한 현재 위치 표시
4. **경로 계획**: 항해 경로 계획 및 표시
5. **데이터 내보내기**: 지도 데이터 및 경로 내보내기

## 주의사항

- 인터넷 연결이 필요합니다 (OpenStreetMap 타일 다운로드용)
- 외부 저장소 쓰기 권한이 필요합니다 (OSMDroid 캐시용)

## 문제 해결

### 지도가 로드되지 않는 경우

1. 인터넷 연결 상태 확인
2. 앱 권한 설정 확인 (외부 저장소 쓰기 권한)
3. 로그캣에서 OSMDroid 관련 오류 메시지 확인

### 앱이 느리게 실행되는 경우

1. 기기 메모리 상태 확인
2. OSMDroid 캐시 정리 (앱 데이터 삭제)
3. 지도 확대/축소 레벨 조정

## 라이선스

이 프로젝트는 MIT 라이선스 하에 배포됩니다.

## 기여하기

버그 리포트, 기능 제안, 코드 기여를 환영합니다. GitHub Issues나 Pull Request를 통해 참여해주세요.
