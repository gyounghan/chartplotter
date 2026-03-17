# 경로(Route) 관리 가이드

차트플로터 앱의 경로 관련 코드 구조, 로직, 유지보수 시 참고사항을 정리한 문서입니다.

---

## 1. 개요

경로는 사용자가 지도에 순차적으로 찍은 점들을 연결한 선으로, 저장·편집·삭제·항해 연결이 가능합니다.

| 개념 | 설명 |
|------|------|
| **Route** | 저장된 경로 (이름, 포인트 리스트, 생성/수정 시각) |
| **RoutePoint** | 경로 내 한 개의 점 (위도, 경도, 순서, 선택적 이름) |
| **편집 모드** | 경로 생성 또는 기존 경로 수정 시 지도에 점 추가/삭제/순서 변경 |
| **항해 연결** | 저장된 경로를 선택하여 항해 경로(waypoints + 목적지)로 설정 |

---

## 2. 아키텍처

```
┌─────────────────────────────────────────────────────────────────┐
│  Presentation Layer                                             │
│  ┌──────────────────┐  ┌──────────────────┐  ┌──────────────┐ │
│  │ ChartOnlyScreen   │  │ MenuPanel        │  │ MainViewModel│ │
│  │ (경로 편집 UI)     │  │ (경로 메뉴/목록)  │  │ (항해 연결)   │ │
│  └────────┬─────────┘  └────────┬─────────┘  └──────┬───────┘ │
│           │                     │                    │         │
│           └─────────────────────┼────────────────────┘         │
│                                 ▼                              │
│  ┌──────────────────────────────────────────────────────────────┐
│  │ RouteViewModel (경로 CRUD, 편집 상태)                          │
│  └──────────────────────────────┬───────────────────────────────┘
└─────────────────────────────────┼───────────────────────────────┘
                                  │
┌─────────────────────────────────┼───────────────────────────────┐
│  Domain Layer                   ▼                                │
│  ┌──────────────────┐  ┌─────────────────────────────────────┐│
│  │ RouteUseCase     │  │ ConnectRouteToNavigationUseCase       ││
│  │ (CRUD, 순서변경)  │  │ (경로 → 항해 경로 변환)               ││
│  └────────┬─────────┘  └───────────────────────────────────────┘│
│           │                                                      │
│  ┌────────┴─────────┐  ┌─────────────────────────────────────────┐│
│  │ UpdateNavigation│  │ MainViewModel.setRouteAsNavigation()     ││
│  │ RouteUseCase    │  │ (항해 경로 지도 표시)                     ││
│  └─────────────────┘  └─────────────────────────────────────────┘│
└───────────────────────────────────────────────────────────────────┘
                                  │
┌─────────────────────────────────┼───────────────────────────────┐
│  Data Layer                     ▼                                │
│  ┌──────────────────┐  ┌──────────────────┐  ┌────────────────┐│
│  │ RouteRepository  │  │ RouteRepositoryImpl│  │ LocalDataSource ││
│  │ (인터페이스)      │  │ (CRUD 구현)        │  │ (SharedPrefs)  ││
│  └──────────────────┘  └──────────────────┘  └────────────────┘│
└───────────────────────────────────────────────────────────────────┘
                                  │
┌─────────────────────────────────┼───────────────────────────────┐
│  Map 렌더링                     ▼                                │
│  ┌──────────────────────────────────────────────────────────────┐│
│  │ PMTilesLoader (addRouteLine, addRoutePoints, removeRouteLine)││
│  └──────────────────────────────────────────────────────────────┘│
└───────────────────────────────────────────────────────────────────┘
```

---

## 3. 파일 구조

| 경로 | 역할 |
|------|------|
| `data/models/Route.kt` | Route, RoutePoint 데이터 모델 |
| `domain/repositories/RouteRepository.kt` | Route 저장소 인터페이스 |
| `data/repositories/RouteRepositoryImpl.kt` | CRUD 구현 |
| `data/datasources/LocalDataSource.kt` | SharedPreferences 저장 (route_prefs) |
| `domain/usecases/RouteUseCase.kt` | 경로 CRUD, 포인트 추가/삭제/순서 변경 |
| `domain/usecases/ConnectRouteToNavigationUseCase.kt` | 경로 → 항해 경로 변환 (위치 기반) |
| `domain/usecases/UpdateNavigationRouteUseCase.kt` | 항해 경로 지도 표시 |
| `presentation/viewmodel/RouteViewModel.kt` | 경로 UI 상태, 편집 로직 |
| `presentation/viewmodel/MainViewModel.kt` | 항해 연결(setRouteAsNavigation, currentNavigationRoute) |
| `presentation/modules/chart/ChartOnlyScreen.kt` | 경로 편집 배너, 다이얼로그, 지도 클릭 처리 |
| `presentation/modules/chart/components/MenuPanel.kt` | 경로 메뉴, 목록, 항해 연결 UI |
| `PMTilesLoader.kt` | 경로 선/점 지도 레이어 추가·제거 |

---

## 4. 데이터 모델

### Route

```kotlin
data class Route(
    val id: String,           // UUID
    val name: String,
    val points: List<RoutePoint>,
    val createdAt: Long,
    val updatedAt: Long
)
```

### RoutePoint

```kotlin
data class RoutePoint(
    val latitude: Double,
    val longitude: Double,
    val order: Int,           // 0부터 시작, 순차적
    val name: String = ""
)
```

### 저장소

- SharedPreferences: `route_prefs`, 키 `"routes"`
- JSON 배열 형식: `Route[]` 직렬화

---

## 5. 주요 로직

### 5.1 경로 생성

1. 메뉴 → 경로 → **경로 생성** → 경로 생성 설명 다이얼로그
2. **생성** 버튼 → 편집 모드 진입 (`isEditingRoute = true`, `editingRoutePoints = []`)
3. 지도 클릭 → `addPointToEditingRoute(lat, lng)` 호출
4. **완료** 버튼 (점 2개 이상) → 경로 이름 입력 다이얼로그
5. 이름 입력 후 **생성** → `createRoute(name, editingRoutePoints)` → 저장 및 편집 모드 종료

### 5.2 경로 수정

1. 메뉴 → 경로 → 목록에서 **편집** 아이콘
2. `selectRoute(route)`, `setEditingRoute(true)`, `setEditingRoutePoints(route.points)` 호출
3. 점 추가·삭제·순서 변경·위치 이동 가능
4. **완료** → 경로 이름 수정 다이얼로그 → **수정** → `updateRoute(updatedRoute)`

### 5.3 경로 편집 시 지도 클릭 처리

`ChartOnlyScreen`에서 `routeUiState.isEditingRoute`일 때:

| 상황 | 동작 |
|------|------|
| `movingPointOrder != null` | 클릭 위치로 해당 점 위치 이동 (`updatePointInEditingRoute`) |
| 기존 점 클릭 | 해당 점 편집 다이얼로그 표시 |
| 빈 공간 클릭 | 새 점 추가 (`addPointToEditingRoute`) |

### 5.4 경로 → 항해 연결 (ConnectRouteToNavigationUseCase)

현재 위치 기준으로 경로를 waypoints + 목적지로 변환합니다.

**경로 안(50m 이내):**

- 가장 가까운 선분 찾기 → 해당 선분 끝점(다음 점)부터 연결

**경로 밖:**

1. 현재 위치 → 첫 번째 점 선분이 경로 선분과 교차하는지 확인
2. **교차 O**: 교차점 이후 점들을 waypoint로 포함
3. **교차 X**: 첫 번째 점부터 연결

반환: `Pair<List<SavedPoint>, SavedPoint>` (waypoints, destination)

---

## 6. 지도 렌더링 (PMTilesLoader)

| 함수 | 용도 |
|------|------|
| `addRouteLine(map, routeId, points, color)` | 경로 선 표시 (2개 이상 점 필요) |
| `addRoutePoints(map, routeId, points)` | 경로 점 마커(파란 원) 표시 |
| `removeRouteLine(map, routeId)` | 경로 선·점 레이어 제거 |

레이어/소스 ID 규칙:

- 선: `route_line_layer_{routeId}`, `route_line_source_{routeId}`
- 점: `route_points_layer_{routeId}`, `route_points_source_{routeId}`

편집 중 경로는 `routeId = "editing_route"` 사용.

---

## 7. UI 상태 (RouteUiState)

```kotlin
data class RouteUiState(
    val selectedRoute: Route? = null,      // 수정 중인 경로 (null이면 신규 생성)
    val isEditingRoute: Boolean = false,
    val editingRoutePoints: List<RoutePoint> = emptyList(),
    val movingPointOrder: Int? = null,    // 위치 이동 중인 점의 order
    val showRouteCreateDialog: Boolean = false
)
```

---

## 8. 경로 표시 토글

`SystemSettings.routeVisible`로 저장된 경로 표시 여부를 제어합니다.

- `SettingsViewModel.updateRouteVisible(visible)` 호출
- `systemSettings.routeVisible == true`일 때만 `PMTilesLoader.addRouteLine/addRoutePoints` 호출
- 편집 중에는 `routeVisible`과 관계없이 항상 표시

---

## 9. 유지보수 시 참고사항

### 9.1 새 기능 추가

- **경로 CRUD**: `RouteUseCase` 확장 후 `RouteViewModel`에 노출
- **경로→항해 로직**: `ConnectRouteToNavigationUseCase` 수정
- **지도 표시**: `PMTilesLoader`에 새 함수 추가

### 9.2 경로 저장 형식 변경

- `LocalDataSource.saveRoutes()`, `loadRoutes()` 수정
- 마이그레이션 필요 시 `loadRoutes()`에서 버전/형식 분기 처리

### 9.3 RoutePoint 스키마 변경

- `RoutePoint`에 필드 추가 시 `LocalDataSource`, `RouteRepositoryImpl` 직렬화/역직렬화 수정
- `RoutePoint.copy()` 사용處 확인 (순서 변경, 위치 이동 등)

### 9.4 디버깅 로그

- `[RouteViewModel]`, `[ConnectRouteToNavigation]`, `[PMTilesLoader]` 태그로 로그 출력
- 경로 연결 실패 시 `ConnectRouteToNavigationUseCase.execute()` 예외 처리 확인

### 9.5 주의사항

- `map.getStyle { }` 비동기 콜백 내에서 `addRouteLine` 등 호출 → 스타일 로드 완료 후 실행 보장
- `removeRouteLine` 시 layer/source 존재 여부 확인 후 제거
- `order` 필드는 0부터 연속적이어야 함 (삭제/추가 시 재정렬 필요)

---

## 10. 문자열 리소스

| 키 | 용도 |
|-----|------|
| `route_create` | 경로 생성 |
| `route_display` | 경로 표시 토글 |
| `route_list` | 경로 목록 |
| `no_routes_saved` | 저장된 경로 없음 |
| `points_count` | 포인트 개수 (`"N개"`) |
| `edit` | 편집 |
| `nav_start_from_route` | 경로로 항해 시작 |

`res/values/strings.xml` 및 다국어 파일 참조.
