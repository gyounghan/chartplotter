# SystemSetting ↔ ChartPlotter 설정 동기화 플랜

## 개요

SystemSetting 앱에서 변경한 **지도 설정**과 **선박 설정**이 ChartPlotter에 실시간 반영되도록 하는 연동 플랜입니다.

---

## 1. 현황 분석

### 1.1 아키텍처

```
[SystemSetting 앱]                    [ChartPlotter 앱]
      │                                      │
      │  SharedPreferences 저장              │  SystemSettingsReader
      │         │                            │  (ContentProvider query)
      │         ▼                            │         │
      │  SystemSettingsManager               │         ▼
      │         │                            │  SettingsViewModel
      │  contentResolver.notifyChange() ─────┼──► ContentObserver (MainActivity)
      │         │                            │         │
      │                                      │  언어 변경 시에만 recreate
      │                                      │  (지도/선박 설정 변경 시 처리 없음)
```

### 1.2 현재 동작

| 구분 | 내용 |
|------|------|
| 설정 저장 | SystemSetting → SharedPreferences, `notifyChange()` 호출 |
| 설정 읽기 | ChartPlotter → ContentProvider query (SystemSettingsReader) |
| 변경 감지 | MainActivity ContentObserver 등록 (언어 변경 시에만 `recreate`) |
| **문제** | 지도/선박 설정 변경 시 ChartPlotter의 SettingsViewModel이 재로드되지 않음 |

### 1.3 데이터 타입 불일치

| 설정 | SystemSetting | ChartPlotter | 비고 |
|------|---------------|--------------|------|
| `aisCourseExtension` | `Int` (분) | `Float` (미터) | **단위·타입 불일치** - 통일 필요 |

---

## 2. 지도 설정 (MapSettings) 연동 플랜

### 2.1 설정 목록 및 ChartPlotter 적용 대상

| 설정 키 | 설명 | ChartPlotter 적용 위치 | 적용 방식 | 우선순위 |
|---------|------|------------------------|-----------|----------|
| `boat3DEnabled` | 3D 보트 선택 | LocationManager (선박 아이콘) | 2D/3D 아이콘 전환 | P2 |
| `distanceCircleRadius` | 거리원 반경(m) | PMTilesLoader / CircleOverlay | 거리원 반경 업데이트 | P2 |
| `headingLineEnabled` | 해딩연장 ON/OFF | LocationManager (Heading Line) | 연장선 표시/숨김 | P2 |
| `courseLineEnabled` | 코스 연장 ON/OFF | LocationManager (Course Line) | 연장선 표시/숨김 | P2 |
| `extensionLength` | 연장선 길이(m) | LocationManager | 연장선 길이 적용 | P2 |
| `gridLineEnabled` | 위경도선 ON/OFF | PMTilesLoader / GridOverlay | 격자선 표시/숨김 | P2 |
| `destinationVisible` | 목적지 표시 | PMTilesLoader (목적지 마커) | 목적지 마커 표시/숨김 | P2 |
| `routeVisible` | 경로 표시 | ChartOnlyScreen, PMTilesLoader | 경로선 표시/숨김 | **P1 (구현됨)** |
| `trackVisible` | 항적 표시 | ChartOnlyScreen (updateTrackDisplay) | 항적선 전역 표시/숨김 | **P1** |
| `mapHidden` | 지도 감춤 | ChartPlotterMap | 배경 지도 레이어 표시/숨김 | P2 |

### 2.2 적용 상세

#### P1: routeVisible (구현됨)
- **위치**: `ChartOnlyScreen.kt` - `LaunchedEffect(systemSettings.routeVisible, ...)`
- **동작**: `routeVisible == false` 시 모든 경로선/점 제거
- **확인**: 설정 변경 시 `systemSettings`가 갱신되어야 반영됨

#### P1: trackVisible (미구현)
- **위치**: `ChartOnlyScreen.kt` - `updateTrackDisplay()`, `updateCurrentTrackDisplay()`
- **적용**: `systemSettings.trackVisible == false` 일 때 항적 전부 제거
- **코드 예시**:
  ```kotlin
  if (!systemSettings.trackVisible) {
      PMTilesLoader.removeAllTracks(map)
      return
  }
  ```

#### P2: 기타 지도 설정
- `boat3DEnabled`, `distanceCircleRadius`, `headingLineEnabled`, `courseLineEnabled`, `extensionLength`, `gridLineEnabled`, `destinationVisible`, `mapHidden`
- LocationManager, PMTilesLoader, ChartPlotterMap 등에 해당 파라미터 전달 후 조건부 렌더링

---

## 3. 선박 설정 (VesselSettings) 연동 플랜

### 3.1 설정 목록 및 ChartPlotter 적용 대상

| 설정 키 | 설명 | ChartPlotter 적용 위치 | 적용 방식 | 우선순위 |
|---------|------|------------------------|-----------|----------|
| `mmsi` | MMSI 번호 | AISOverlay, AIS 알림 | 자선 MMSI 식별, 알림 발송 시 참조 | P1 |
| `aisCourseExtension` | AIS 코스 연장 | AISOverlay | 타선 코스 연장선 길이 | **P1** (단위 통일 필요) |
| `vesselTrackingSettings` | 관심 선박/위험물표 | AISOverlay | 표시할 선박 유형 필터링 | P2 |
| `recordLength` | 기록 길이(분) | TrackViewModel / VoyageRecorder | 항적 기록 최대 길이 제한 | P2 |

### 3.2 적용 상세

#### mmsi
- 자선(own ship) MMSI로 사용. AIS 알림, 관심 선박 판별 등에 활용
- 적용: AISOverlay, AISAlertNotifier 등에 `systemSettings.mmsi` 전달

#### aisCourseExtension (단위 통일 필요)
- **현재**: SystemSetting = 분(Int), ChartPlotter = 미터(Float)
- **권장**: 둘 다 **미터(Float)** 로 통일
  - SystemSetting UI: "코스 연장 (m)" 으로 변경
  - ChartPlotter: 그대로 Float (미터) 사용

#### vesselTrackingSettings
- 키: "관심 선박", "위험물표" 등
- AISOverlay에서 표시할 선박 유형 필터링에 사용

---

## 4. 설정 변경 감지 및 반영

### 4.1 핵심 수정: SettingsViewModel 갱신

**문제**: SystemSetting 저장 시 `notifyChange()` 호출 → MainActivity ContentObserver 수신  
→ 현재는 **언어 변경 시에만** `recreate()` 수행, 지도/선박 설정은 무시

**해결**: ContentObserver에서 **모든 설정 변경**에 대해 `SettingsViewModel.reloadSystemSettings()` 호출

### 4.2 구현 방법

#### 옵션 A: MainActivity에서 SettingsViewModel 참조 전달 (권장)

```kotlin
// MainActivity.kt
private var settingsViewModelRef: SettingsViewModel? = null

settingsContentObserver = object : ContentObserver(Handler(Looper.getMainLooper())) {
    override fun onChange(selfChange: Boolean) {
        super.onChange(selfChange)
        runOnUiThread {
            try {
                val newSettings = systemSettingsReader.loadSettings()
                if (newSettings.language != currentLanguage) {
                    recreate()
                } else {
                    // 언어 외 설정 변경 → SettingsViewModel 재로드
                    settingsViewModelRef?.reloadSystemSettings()
                }
            } catch (e: Exception) { ... }
        }
    }
}

// setContent 내부
settingsViewModelRef = settingsViewModel
```

#### 옵션 B: Application-scoped Flow

- Application에 `MutableSharedFlow<Unit>` 생성
- SystemSetting 저장 시 해당 Flow에 emit (별도 브로드캐스트 필요)
- SettingsViewModel에서 collect 및 `reloadSystemSettings()` 호출

→ 옵션 A가 기존 notifyChange 구조를 활용하므로 단순함.

### 4.3 적용 순서

1. **Phase 1**: 설정 변경 시 ChartPlotter 반영
   - MainActivity ContentObserver 확장
   - `settingsViewModelRef?.reloadSystemSettings()` 호출

2. **Phase 2**: 미사용 지도/선박 설정 적용
   - `trackVisible` → updateTrackDisplay/updateCurrentTrackDisplay
   - `aisCourseExtension` 단위 통일
   - 기타 P2 설정 (선택 구현)

---

## 5. aisCourseExtension 단위 통일

| 앱 | 현재 | 변경 후 |
|----|------|---------|
| SystemSetting | Int (분) | Float (미터) |
| ChartPlotter | Float (미터) | Float (미터) (유지) |

- SystemSetting `SystemSettings.kt`: `aisCourseExtension: Int` → `Float`
- SystemSetting `SystemSettingsManager.kt`: `putInt` → `putFloat`, `getInt` → `getFloat`
- SystemSetting `VesselSettingsContent`: "분" → "m" (미터)
- SystemSettingsProvider: Int → Float 저장/반환

---

## 6. 작업 체크리스트

### Phase 1: 설정 변경 시 ChartPlotter 반영
- [x] MainActivity에 `settingsViewModelRef` 추가
- [x] ContentObserver에서 언어 변경 외에 `reloadSystemSettings()` 호출
- [x] setContent에서 `settingsViewModelRef` 할당

### Phase 2: trackVisible 적용
- [x] `updateTrackDisplay()`에 `systemSettings.trackVisible` 체크 추가
- [x] `updateCurrentTrackDisplay()`에 `systemSettings.trackVisible` 체크 추가
- [x] LaunchedEffect에 `systemSettings.trackVisible` 의존성 추가

### Phase 3: aisCourseExtension 단위 통일
- [x] SystemSetting: Int → Float 변경
- [x] SystemSettingsProvider: Int → Float (cursor는 settings.aisCourseExtension 자동 변환)
- [x] ChartPlotter Reader: Float 유지 (이미 Float)

### Phase 4 (선택): 기타 지도/선박 설정
- [ ] boat3DEnabled
- [ ] distanceCircleRadius
- [ ] headingLineEnabled / courseLineEnabled / extensionLength
- [ ] gridLineEnabled
- [ ] destinationVisible
- [ ] mapHidden
- [ ] vesselTrackingSettings
- [ ] recordLength

---

## 7. 참고 파일

| 파일 | 역할 |
|------|------|
| `SystemSetting/.../SystemSettingsManager.kt` | 설정 저장, notifyChange |
| `SystemSetting/.../SystemSettingsProvider.kt` | ContentProvider (읽기) |
| `ChartPlotter/.../SystemSettingsReader.kt` | ContentProvider에서 읽기 |
| `ChartPlotter/.../SettingsViewModel.kt` | systemSettings 상태, reloadSystemSettings() |
| `ChartPlotter/.../MainActivity.kt` | ContentObserver 등록 |
| `ChartPlotter/.../ChartOnlyScreen.kt` | routeVisible, trackVisible 적용 |
