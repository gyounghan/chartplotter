# PMTiles 읽기 및 지도 렌더링 아키텍처

전자해도에서 PMTiles 파일을 읽고 MapLibre 지도에 그리는 전반적인 흐름을 설명합니다.

---

## 1. 개요

| 구성요소 | 역할 |
|---------|------|
| **PMTilesConfig** | 개별 PMTiles 파일의 렌더 설정 (레이어 타입, 색상, 텍스트 필드 등) |
| **PMTilesManager** | 설정 로딩, 파일 경로 관리 (외부/내부 저장소) |
| **PMTilesLoader** | PMTiles 파일 로드 → MapLibre Style/Source/Layer에 적용 |
| **ChartPlotterMap** | 지도 UI, `PMTilesLoader.loadPMTiles()` 호출 |

---

## 2. PMTilesConfig — 무엇을 읽는가

`PMTilesConfig`는 **하나의 PMTiles 파일**을 어떻게 렌더할지 정의하는 데이터 클래스입니다.

### 2.1 주요 필드

| 필드 | 설명 | 예시 |
|-----|------|------|
| `fileName` | PMTiles 파일명 | `lineTiles.pmtiles`, `p_soundg_1.pmtiles` |
| `sourceName` | MapLibre 소스 ID | `lineTiles-source`, `depthTiles1-source` |
| `sourceLayer` | PMTiles 내부 레이어명 | `line_map`, `soundg`, `sbdare` |
| `layerType` | 렌더 타입 | `LINE`, `AREA`, `TEXT`, `SYMBOL` |
| `colorMapping` | 속성값 → 색상 매핑 (LINE/AREA) | `98 → 빨강`, `96 → 파랑` |
| `hasTextLayer` | 텍스트 표시 여부 | `true` |
| `textField` | 텍스트로 표시할 속성명 | `ELEVATION`, `VALUE`, `NATSUR_Nat` |
| `stripNumericPrefixFromTextField` | "숫자 지질" → 지질만 표시 | `true` (sbdare용) |
| `isDynamicSymbol` | ICON 속성에 따른 동적 아이콘 | `true` |
| `iconMapping` | ICON 값 → drawable 리소스명 | `"lights" → "lights"` |
| `iconSize` | 아이콘 크기 배율 | `1.0f`, `1.2f` |
| `minZoom` | 최소 줌 레벨 | `15f` (깊이 등) |

### 2.2 LayerType별 용도

| LayerType | 용도 | 예시 |
|-----------|------|------|
| **LINE** | 선형 객체 (해안선, 경계선 등) | `lineTiles.pmtiles` |
| **AREA** | 면적 객체 (해역, 구역 등) | `areaMapTiles.pmtiles`, `a_fishfarm.pmtiles` |
| **TEXT** | 텍스트 라벨 (수심, 지명 등) | `p_soundg_*.pmtiles` (ELEVATION), `name_level.pmtiles` (VALUE) |
| **SYMBOL** | 아이콘 심볼 (등대, 부표, 암초 등) | `p_lights.pmtiles`, `p_wrecks.pmtiles` |

### 2.3 색상 매핑 (LINE/AREA)

- **BFR_COLOR / COLOR / LAYER**: 면 채우기 색상 결정
- **BDR_COLOR**: 선(테두리) 색상 결정
- `PMTilesManager.getBdrColorMapping()` — S-57 표준 색상 코드 → Android Color 매핑

---

## 3. PMTilesManager — 설정 출처

### 3.1 로딩 우선순위

1. **외부 저장소**  
   - 경로: `getExternalFilesDir()/charts/`  
   - PMTiles: `charts/pmtiles/*.pmtiles`  
   - 설정: `charts/pmtiles_config.json`

2. **내부 assets (fallback)**  
   - PMTiles: `assets/pmtiles/*.pmtiles`  
   - 설정: `PMTilesManager.pmtilesConfigs` (하드코딩)

### 3.2 설정 로딩 흐름

```
PMTilesManager.loadConfigs(context)
  → loadExternalConfig() 시도
  → 없으면 pmtilesConfigs (하드코딩) 사용
```

### 3.3 파일명 기반 자동 설정

외부 JSON에 없는 파일은 **파일명 규칙**으로 설정 생성:

| 접두사 | layerType | sourceLayer 추출 |
|--------|-----------|------------------|
| `l_` | LINE | `l_` 제거 |
| `a_` | AREA | `a_` 제거 |
| `p_` | TEXT | `p_` 제거, `_`로 분리 후 첫 부분 (예: `p_soundg_1` → `soundg`) |
| `line*`, `area*` | LINE/AREA | `line_map`, `area_map` |

`PMTilesManager.createDefaultConfigFromFileName(fileName)` 에서 처리합니다.

### 3.4 외부 디렉터리 구조

```
{getExternalFilesDir()}/charts/
├── pmtiles_config.json    # PMTiles 설정 (선택)
├── pmtiles/               # PMTiles 파일
│   ├── lineTiles.pmtiles
│   ├── areaMapTiles.pmtiles
│   ├── p_soundg_1.pmtiles
│   └── ...
└── icons/                 # 커스텀 아이콘 (png, jpg, bmp)
    ├── lights.png
    ├── lights_red.png
    └── ...
```

---

## 4. PMTilesLoader — 어떻게 로드·적용하는가

### 4.1 전체 흐름

```
PMTilesLoader.loadPMTiles(context, map, fontSize)
  ├─ PMTilesManager.ensureExternalDirectories()
  ├─ 외부 설정 없으면 exportDefaultConfigToExternal() (참고용)
  ├─ PMTilesManager.loadPMTilesSource()
  │   ├─ hasExternalPMTiles() → 외부 파일 사용
  │   └─ 없으면 assets 사용
  │
  ├─ [외부] loadFromExternal()
  │   ├─ loadConfigs() → (설정 리스트, 외부설정 여부)
  │   ├─ 외부 PMTiles 파일과 설정 매칭 (파일명 기준)
  │   └─ applyPMTilesToMap()
  │
  └─ [내부 assets] loadPMTilesFromAssets()
      ├─ getPMTilesFilesFromAssets()
      ├─ copyPmtilesFromAssets() → filesDir/pmtiles/
      ├─ findConfigByFileName() → 설정 매칭
      └─ applyPMTilesToMap()
```

### 4.2 applyPMTilesToMap — 지도에 그리기

1. **Style 초기화**
   - 빈 스타일 JSON (background만) 로드
   - `map.setStyle(Style.Builder().fromJson(...))` 호출

2. **각 PMTiles → VectorSource 추가**
   - `pmtiles://file://{파일경로}` 형식 URL
   - `VectorSource(config.sourceName, pmtilesUrl)`
   - `style.addSource(source)`

3. **각 Config에 따라 레이어 추가**
   - `config.layerType`에 따라 분기:

| layerType | 호출 함수 | 생성 레이어 |
|-----------|-----------|-------------|
| LINE | `addLineLayer()` | `{sourceName}-lines` |
| AREA | `addAreaLayer()` | `{sourceName}-areas`, `{sourceName}-lines` |
| TEXT | `addTextLayer()` | `{sourceName}-labels` |
| SYMBOL | `addSymbolLayer()` 또는 `addDynamicSymbolLayer()` | `{sourceName}-symbols` 또는 `{sourceName}-dynamic-symbols` |

### 4.3 레이어별 렌더링 방식

#### LINE (선)

- `LineLayer`, `sourceLayer` = config.sourceLayer
- 색상: `BDR_COLOR` → `createBorderColorExpression()`
- 두께: `WIDTH` / `BFR_WIDTH` (없으면 1.0)

#### AREA (면)

1. **FillLayer** (`{sourceName}-areas`)
   - 채우기 색상: `BFR_COLOR`/`COLOR`/`LAYER` → `createFillColorExpression()`
   - `fillOpacity`: 0.6

2. **LineLayer** (`{sourceName}-lines`)
   - 테두리 색상: `BDR_COLOR` → `createBorderColorExpression()`
   - `lineWidth`: 0.2

#### TEXT (텍스트)

- `SymbolLayer`에 `textField` 설정
- 표현식: `get(config.textField)` (stripNumericPrefix 옵션 시 `createStripNumericPrefixExpression()`)
- 필터 예:
  - 수심(depth): `ELEVATION > 0` (숫자만)
  - 그 외: `textField != ""`
- 폰트 크기: `fontSize / 14f`로 스케일 (시스템 설정 반영)

#### SYMBOL (심볼)

1. **정적 심볼** (`addSymbolLayer`)
   - `textField` 값으로 단일 아이콘 사용
   - `loadIconBitmap()`: 외부 `icons/` → drawable fallback

2. **동적 심볼** (`addDynamicSymbolLayer`)
   - 피처의 `ICON` 속성값에 따라 다른 아이콘
   - `iconMapping`: `"ICON값" → "drawable 리소스명"`
   - `match(get("ICON"), ...)` 표현식으로 아이콘 선택
   - `loadIconBitmapWithTransparency()`: BMP 시 흰색→투명 처리

### 4.4 아이콘 로딩 우선순위

1. **외부**: `charts/icons/{iconName}.{png|jpg|bmp}`
2. **내부**: `context.resources.getIdentifier(iconName, "drawable", packageName)`

---

## 5. 지도에 그리는 순서 (레이어 스택)

MapLibre는 **나중에 추가된 레이어가 위에** 그려집니다. PMTilesLoader는 config 리스트 순서대로 레이어를 추가하므로, `pmtiles_config.json` / 하드코딩 config의 **순서가 렌더 순서**를 결정합니다.

예상되는 순서 (일반적):

1. background
2. AREA (면) — 먼저 추가
3. LINE (선)
4. TEXT (수심, 지명 등)
5. SYMBOL (등대, 부표, 암초 등)
6. 이후 ChartOnlyScreen 등에서 **오버레이** 추가:
   - 항적선 (`addTrackLine`)
   - 경로선 (`addRouteLine`)
   - 목적지 마커 (`addDestinationMarkers`)
   - 항해 마커 (`addNavigationMarker`)
   - 코스업 선 (`addCourseLine`)
   - 위경도선 (`addGridLines`)
   - 거리원 (`updateDistanceCircle`)
   - 해딩연장선 (`addHeadingLine`)

---

## 6. ChartPlotterMap과의 연동

### 6.1 진입점

```kotlin
// ChartPlotterMap.kt
mapView.getMapAsync { map ->
    PMTilesLoader.loadPMTiles(context, map, fontSize)
    onMapReady(map)
}
```

### 6.2 재로딩 트리거

- **폰트 크기 변경 시**: `LaunchedEffect(fontSize)` 에서 `loadPMTiles` 재호출
  - 텍스트 레이어 크기가 시스템 설정에 맞게 다시 적용됨

---

## 7. PMTilesLoader 오버레이 API 요약

지도 타일 위에 사용자 데이터를 그리기 위한 API:

| 함수 | 용도 |
|------|------|
| `addDestinationMarkers` | 목적지 마커 |
| `addNavigationRoute` | 항해 경로선 |
| `addNavigationMarker` | 항해 마커 (현재 목적지) |
| `addTrackLine` | 항적선 |
| `addRouteLine` / `addRoutePoints` | 경로선·경유점 |
| `addCourseLine` | 코스업 선 (현재→목표) |
| `addGridLines` | 위경도선 |
| `updateDistanceCircle` | 거리원 |
| `addHeadingLine` | 해딩연장선 |
| `addMapHiddenOverlay` | 지도 감춤 오버레이 |
| `remove*` 계열 | 각 오버레이 제거 |

이 오버레이들은 `GeoJsonSource` + `SymbolLayer` / `LineLayer` 등으로 구성되며, PMTiles 기본 스타일 위에 추가됩니다.

---

## 8. 요약 다이어그램

```
[PMTiles 파일] (.pmtiles)
       │
       ▼
[PMTilesManager] ─┬─ loadPMTilesSource() → 외부 charts/pmtiles/ 또는 assets/pmtiles/
                  ├─ loadConfigs() → pmtiles_config.json 또는 하드코딩
                  └─ findConfigByFileName() / createDefaultConfigFromFileName()
       │
       ▼
[PMTilesConfig] (fileName, sourceLayer, layerType, colorMapping, textField, iconMapping, ...)
       │
       ▼
[PMTilesLoader.applyPMTilesToMap()]
       │
       ├─ VectorSource (pmtiles://file://...)
       │
       └─ layerType별 레이어 추가
              ├─ LINE  → LineLayer (BDR_COLOR, WIDTH)
              ├─ AREA  → FillLayer + LineLayer (BFR_COLOR, BDR_COLOR)
              ├─ TEXT  → SymbolLayer (textField, stripNumericPrefix)
              └─ SYMBOL → SymbolLayer (iconMapping, ICON match)
       │
       ▼
[MapLibre Style] (sources + layers)
       │
       ▼
[ChartPlotterMap] + 오버레이 (항적, 경로, 마커 등)
       │
       ▼
[화면에 렌더링]
```

---

## 9. 참고 파일

| 파일 | 역할 |
|------|------|
| `PMTilesConfig.kt` | PMTilesConfig, PMTilesConfigFile, LayerType, PMTilesManager |
| `PMTilesLoader.kt` | PMTiles 로드, 레이어 생성, 오버레이 API |
| `ChartPlotterMap.kt` | MapView, loadPMTiles 호출 |
| `ChartOnlyScreen.kt` | addTrackLine, addRouteLine 등 오버레이 호출 |
