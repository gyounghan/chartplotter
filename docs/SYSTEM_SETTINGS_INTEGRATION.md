# SystemSetting → ChartPlotter 설정 연동 문서

ChartPlotter 앱이 SystemSetting 앱의 설정을 어떻게 확인(읽기)하는지 설명합니다.

---

## 1. 개요

ChartPlotter는 **Android ContentProvider**를 통해 SystemSetting 앱의 설정을 조회합니다.  
SystemSetting이 **공급자(Provider)**, ChartPlotter가 **소비자(Consumer)** 역할을 합니다.

---

## 2. 아키텍처

```
┌─────────────────────────────┐         ┌─────────────────────────────┐
│      SystemSetting 앱        │         │      ChartPlotter 앱         │
│                             │         │                             │
│  [SharedPreferences]        │         │  [SystemSettingsReader]      │
│  system_settings            │         │       │                     │
│       │                     │         │       │ query()              │
│       ▼                     │         │       ▼                     │
│  [SystemSettingsManager]    │         │  contentResolver.query()   │
│  loadSettings()             │         │       │                     │
│  saveSettings() → notifyChange()      │       │                     │
│       │                     │         │       │                     │
│       ▼                     │  query  │       ▼                     │
│  [SystemSettingsProvider] ◄─┼─────────┼── Cursor (key, value)      │
│  ContentProvider            │         │       │                     │
│  query() → MatrixCursor     │         │       ▼                     │
│                             │         │  [SystemSettings] data      │
│                             │         │       │                     │
│                             │         │       ▼                     │
│                             │         │  [SettingsViewModel]        │
│                             │         │  systemSettings 상태        │
│                             │         │       │                     │
│                             │         │       ▼                     │
│                             │         │  [SettingsScreen 등 UI]    │
└─────────────────────────────┘         └─────────────────────────────┘
```

---

## 3. SystemSetting 쪽 구현

### 3.1 ContentProvider 등록

**파일**: `SystemSetting/app/src/main/AndroidManifest.xml`

```xml
<provider
    android:name=".provider.SystemSettingsProvider"
    android:authorities="com.kumhomarine.systemsetting.provider"
    android:exported="true" />
```

- `android:exported="true"`: 다른 앱에서 접근 가능
- Authority: `com.kumhomarine.systemsetting.provider`

### 3.2 SystemSettingsProvider

**파일**: `SystemSetting/.../provider/SystemSettingsProvider.kt`

- **역할**: SharedPreferences에 저장된 설정을 Cursor 형태로 제공
- **URI**: `content://com.kumhomarine.systemsetting.provider/settings`
- **query()**: `settingsManager.loadSettings()`로 `SystemSettings` 로드 → 각 필드를 `(key, value)` 행으로 `MatrixCursor`에 추가 → 반환

**Cursor 스키마**:
- 컬럼: `key` (String), `value` (String)
- 행 예: `("language", "한국어")`, `("mmsi", ""), ("record_length", "60")` 등

**읽기 전용**: insert/update/delete는 미구현(또는 0 반환)

### 3.3 SystemSettingsManager

**파일**: `SystemSetting/.../data/SystemSettingsManager.kt`

- **저장소**: `SharedPreferences` (`system_settings`)
- **saveSettings() 종료 시**:
  ```kotlin
  context.contentResolver.notifyChange(
      Uri.parse("content://com.kumhomarine.systemsetting.provider/settings"),
      null
  )
  ```
  → 설정 변경 시 ContentResolver에 알림 → ChartPlotter의 ContentObserver가 동작

---

## 4. ChartPlotter 쪽 구현

### 4.1 ContentProvider 접근 권한

**파일**: `ChartPlotter/app/src/main/AndroidManifest.xml`

```xml
<queries>
    <package android:name="com.kumhomarine.systemsetting" />
</queries>
```

- Android 11(API 30) 이상에서 다른 앱 패키지 접근 시 필요

### 4.2 SystemSettingsReader

**파일**: `ChartPlotter/.../data/SystemSettingsReader.kt`

**역할**: ContentProvider를 query하여 `SystemSettings` 객체로 변환

**핵심 로직**:

```kotlin
private val CONTENT_URI = Uri.parse("content://com.kumhomarine.systemsetting.provider/settings")

fun loadSettings(): SystemSettings {
    val cursor = context.contentResolver.query(CONTENT_URI, null, null, null, null)
    // cursor가 null이거나 예외 시 → SystemSettings() 기본값 반환
    
    // Cursor에서 key, value 추출 → settingsMap에 저장
    while (cursor.moveToNext()) {
        settingsMap[key] = value
    }
    
    // getString/getFloat/getInt/getBoolean 등으로 파싱
    // parseJsonMap: advanced_features, alert_settings 등 JSON 필드
    
    return SystemSettings(
        language = getString("language", "한국어"),
        mmsi = getString("mmsi", ""),
        recordLength = getInt("record_length", 60),
        // ... 기타 필드
    )
}
```

**키 매핑 예시**:

| SharedPreferences 키 | SystemSettings 필드 |
|---------------------|---------------------|
| language | language |
| vessel_length | vesselLength |
| mmsi | mmsi |
| record_length | recordLength |
| (제거됨: boat_3d_enabled 등) | (ChartPlotter 로컬 ChartSettingsRepository 사용) |

**기본값**: 키가 없거나 파싱 실패 시 두 번째 인자(기본값) 사용

### 4.3 SettingsViewModel

**파일**: `ChartPlotter/.../presentation/viewmodel/SettingsViewModel.kt`

- **생성 시**: `SystemSettingsReader` + `ChartSettingsRepository` 주입
- **loadSystemSettings()**:
  - `SystemSettingsReader.loadSettings()`로 시스템 설정 로드
  - `ChartSettingsRepository.loadChartSettings()`로 지도/선박(차트) 설정 로드
  - 두 결과를 merge하여 `systemSettings` 상태로 갱신
- **reloadSystemSettings()**: 동일 로직으로 재로드 (ContentObserver에서 호출)

### 4.4 MainActivity – ContentObserver

**파일**: `ChartPlotter/.../MainActivity.kt`

- **ContentObserver 등록**:
  ```kotlin
  contentResolver.registerContentObserver(
      Uri.parse("content://com.kumhomarine.systemsetting.provider/settings"),
      true,
      settingsContentObserver
  )
  ```
- **onChange()** 시:
  - `systemSettingsReader.loadSettings()` 호출
  - **언어 변경** → `recreate()`로 앱 재시작
  - **그 외 설정 변경** → `settingsViewModelRef?.reloadSystemSettings()` 호출

---

## 5. 데이터 흐름 요약

| 단계 | 앱 | 코드/클래스 | 동작 |
|------|-----|-------------|------|
| 1 | SystemSetting | SystemSettingsManager.saveSettings() | SharedPreferences 저장 후 `notifyChange()` |
| 2 | SystemSetting | SystemSettingsProvider | query 시 SharedPreferences → Cursor 반환 |
| 3 | ChartPlotter | SystemSettingsReader.loadSettings() | `contentResolver.query()` → Cursor 파싱 → SystemSettings |
| 4 | ChartPlotter | SettingsViewModel | SystemSettings + ChartSettings merge → UI 상태 반영 |
| 5 | ChartPlotter | ContentObserver.onChange() | 설정 변경 시 `reloadSystemSettings()` 호출 |

---

## 6. 참고 사항

- **지도/선박(차트) 설정**: ChartPlotter 전용 설정으로 분리됨. `ChartSettingsRepository` 로컬 저장, merge 시 ChartPlotter 값 우선
- **SystemSetting 미설치**: `SystemSettingsReader`가 예외 처리 후 `SystemSettings()` 기본값 반환
- **Provider 미등록**: `MainActivity`에서 `resolveContentProvider` 확인 후 Observer 등록 생략
