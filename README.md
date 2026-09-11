# Nexora ERP — Java 재구현 (Spring Boot)

기존 Classic ASP(VBScript) + MSSQL로 만들어진 사내 프로젝트 매출/직접비 관리 ERP를
Spring Boot + PostgreSQL로 재구현한 프로젝트입니다. **핵심 기능 위주로 대표성 있게 재구현**한
버전이며, 회사명·클라이언트명·계정 등은 전부 가상 데이터로 대체했습니다. 실제 데이터는
포함되어 있지 않습니다.

> ⚠️ **중요**: 이 코드는 Maven Central 접근이 막힌 네트워크 환경에서 작성되어, **Spring
> Boot 의존성을 받아 `mvn compile`로 끝까지 빌드해서 검증하지는 못했습니다.** 다만
> `schema.sql`/`data.sql`(통계 화면용으로 확장된 36건의 프로젝트 + 88건의 직접비 포함)은
> 실제 로컬 PostgreSQL 16에 직접 적용해 정상적으로 테이블 생성과 데모 데이터 입력이 되는
> 것을 확인했고, 대시보드/검색/통계 화면에 쓰이는 모든 네이티브 쿼리(월별·연도별·클라이언트별·
> 직접비 항목별 집계 포함)도 같은 DB에 대해 직접 실행해 계산 결과가 시드 데이터와 정확히
> 일치하는 것까지 확인했습니다. 그 외 Java 코드는 여러 차례 꼼꼼히 검토했지만, IntelliJ에서
> 처음 실행하면 오탈자나 의존성 관련 오류가 나올 수 있습니다. 에러 메시지를 그대로 알려주시면
> 바로 고쳐드리겠습니다.

## 1. 무엇을 재구현했는가

| 원본 ASP 파일 | 재구현 위치 | 비고 |
|---|---|---|
| Login.asp, Login_process.asp | `SecurityConfig`, `AdminUserDetailsService`, `AuthController` | IP 화이트리스트 자동 로그인 + 소스코드 평문 비밀번호 하드코딩 → 계정 테이블 + BCrypt 해시 검증으로 완전 교체 |
| index.asp (팀장/담당자 전용 대시보드) | `DashboardController#fullDashboard`, `dashboard/list.html` | 문자열을 이어붙여 조립하던 SQL(TOP/NOT IN 페이징) → 파라미터 바인딩 + Spring Data `Pageable`로 교체 |
| monitor.asp (전체 로그인 사용자용 제한된 조회) | `DashboardController#monitorDashboard`, `dashboard/monitor.html` | 동일 도메인/서비스 재사용, 뷰만 분리 |
| DirectCost/index.asp, Check.asp, DirectCost_Pro.asp | `DirectCostController`, `DirectCostService`, `direct-cost/*.html` | 27개 항목 × 5필드(135개 컬럼) 반복 컬럼 구조 → `direct_cost_item` 자식 테이블로 정규화 |
| Project_Upload.asp (엑셀 일괄 업로드) | `ExcelProjectImportService` | IIS 전용 컴포넌트(DEXT.FileUpload) + ADO Jet OLEDB → Spring MultipartFile + Apache POI로 교체 |
| Project_Delete.asp, Update_Cost.asp, Update_Kcode.asp | `ProjectAdminController` | 기존 프런트엔드 AJAX와의 호환을 위해 "Y"/"N" 텍스트 응답 계약은 그대로 유지 |
| extend_session.asp | `SessionController` | 세션 연장 |
| *(원본에 없던 신규 화면)* | `StatisticsController`, `StatisticsService`, `statistics/view.html` | 월별/연도별/클라이언트별/직접비 항목별 매출·비용을 Chart.js 그래프로 보여주는 통계 화면. 대시보드 상단 "통계 보기" 버튼으로 이동 (자세한 내용은 아래 "7. 통계 화면" 참고) |

재구현하지 않은 것: 없음에 가깝지만, 원본에 섞여 있던 이 ERP 기능과 무관한 별도 서브시스템(`LIB/` 하위의 부가 모듈 파일들)은 재구현 대상에서 제외했습니다.

### 보안 관련 주요 변경 사항 (원본 대비)

- 특정 IP 주소를 화이트리스트에 올려 자동 로그인시키던 로직을 **완전히 제거**했습니다.
- 소스코드에 평문으로 하드코딩되어 있던 계정/비밀번호를 **제거**하고, DB 계정 + BCrypt 해시 기반 로그인으로 교체했습니다.
- 실제 회사명, 클라이언트명, 담당자명, IP 등은 전부 **가상의 이름으로 대체**했습니다 (아래 "가상 데이터 안내" 참고).

## 2. 로컬 실행에 필요한 프로그램

- **JDK 17 이상**
- **PostgreSQL 15+** (네이티브 설치, Docker 없이 진행하는 경우 기준)
  1. [postgresql.org](https://www.postgresql.org/download/windows/) 에서 Windows용 설치파일 다운로드 후 설치
     - 설치 중 `postgres` 슈퍼유저 비밀번호를 물어봅니다 → 기억해두기
     - 포트는 기본값 5432 그대로 사용
  2. pgAdmin 실행 → 서버 접속 → 상단 메뉴 **Tools > Query Tool**
  3. 프로젝트에 포함된 `setup_db.sql` 파일을 열어서 안내된 순서(STEP 1 → DB 전환 → STEP 2)대로 실행
     - 이 스크립트가 `erp_crm` DB와 `erp_user` 계정을 만들어 줍니다 (`application.yml`에 이미 이 값으로 설정되어 있어 별도 수정 불필요)
  4. (참고) Docker를 쓸 수 있다면 이 과정 대신 프로젝트 루트에서 `docker compose up -d` 한 줄로 대체 가능합니다.
- 기존 IIS/Tomcat은 필요 없습니다. Spring Boot는 내장 톰캣을 포함하고 있어서 IntelliJ 실행 버튼만으로 뜹니다.

## 3. 실행 방법 (IntelliJ)

1. `java-migration` 폴더를 IntelliJ에서 **Open** (Maven 프로젝트로 자동 인식, `pom.xml` 기준 의존성을 인터넷에서 자동 다운로드합니다)
2. 위 2번 항목대로 PostgreSQL에 `erp_crm` DB와 `erp_user` 계정이 준비되어 있는지 확인
3. `ErpCrmApplication`의 `main()` 옆 초록색 실행 버튼 클릭
   - 최초 실행 시 `schema.sql` → `data.sql`이 자동 실행되어 테이블 생성 + 가상 데모 데이터가 들어갑니다.
   - 콘솔에 빨간 에러 없이 `Started ErpCrmApplication` 로그가 뜨면 성공
4. 브라우저에서 `http://localhost:8080` 접속 → 로그인 화면으로 자동 이동
   - 데모 계정: `admin` / `admin1234` (관리자), `editor` / `editor1234` (운영 담당자), `partner` / `partner1234` (파트너사)
5. DB 없이 그냥 눌러서 확인만 해보고 싶다면 실행 설정(Run Configuration)의 Program arguments에 아래를 추가:
   ```
   --spring.profiles.active=h2
   ```
   (H2 인메모리 DB로 뜨며, 앱을 끄면 데이터가 사라집니다. `schema.sql`이 PostgreSQL 문법 기준(UUID 기본값, generated column 등)이라 H2 호환 모드에서 100% 동작을 보장하지 못합니다 — 안 되면 postgres 프로필을 권장합니다.)

## 4. 프로젝트 구조

```
src/main/java/com/nexora/erp/
  domain/        JPA 엔티티 (Project, DirectCostItem, CostCategory, AdminAccount, AdminRole)
  repository/    Spring Data JPA 리포지토리 (검색/합계 native query 포함)
  service/       비즈니스 로직 (검색, 직접비 계산, 엑셀 업로드, 로그인 사용자 조회)
  web/           컨트롤러 + 폼 DTO
  config/        Spring Security 설정, 로그인 성공 후 역할별 리다이렉트
src/main/resources/
  schema.sql     정규화된 테이블 DDL (PostgreSQL)
  data.sql       가상 데모 시드 데이터 (계정 3개 + 프로젝트 36건, 2025~2026년 전체 월에 분포)
  templates/     Thymeleaf 화면 (로그인 / 대시보드 / 모니터 / 직접비 입력·조회 / 통계)
  static/css/    사이드바 + 카드 기반의 모던 대시보드 UI로 재구성한 스타일시트 (원본 ASP 룩앤필은 유지하지 않음)
```

## 5. 가상 데이터 안내

이 저장소에는 실제 회사 정보가 전혀 포함되어 있지 않습니다.

- 시스템/기관명: "Nexora" (완전 가상, 디지털 프로덕트/데이터 컨설팅 에이전시 콘셉트)
- 클라이언트명: 블루오션테크, 그린필드전자, 노스브릿지시스템즈 등 8곳 (전부 가상)
- 프로젝트 유형: 플랫폼 구축, 데이터분석, 대시보드 구축, 컨설팅 등 (전부 가상 프로젝트)
- 담당자명: 김도윤, 박서연, 이하늘, 최지안 등 (전부 가상)
- 데모 계정 비밀번호는 `admin1234` 등 예시용이며, 실제 운영 환경에 배포할 경우 반드시 변경하세요.
- 프로젝트 36건(2025년 12건 + 2026년 24건, 12개월 전체에 분포)과 직접비 항목 88건을 시드해,
  대시보드에서 어떤 연/월을 검색해도(예: 2026년 08월) 실제 값이 나오고, 통계 화면의 모든 그래프에도
  값이 채워지도록 했습니다.

## 6. 통계 화면 (`/statistics`)

대시보드(관리자/담당자 전용) 상단의 **"통계 보기"** 버튼으로 이동할 수 있는 신규 화면입니다.
원본 ASP 사이트에는 없던 화면이지만, 이미 쌓여 있는 매출/직접비 데이터를 여러 각도에서 한눈에
볼 수 있도록 추가했습니다. 차트는 [Chart.js](https://www.chartjs.org/)를 CDN으로 불러와 그립니다.

- 연간 목표 대비 누계 실적 게이지 (목표 금액은 `application.yml`의 `erp.annual-target-amount`에서 조정)
- 클라이언트별 / 프로젝트 종류별 매출 비중 도넛 차트
- 월별 매출·직접비 막대 그래프
- 직접비 항목(카테고리)별 비중 가로 막대 그래프
- 연도별 매출·직접비 추이 막대 그래프
- 화면 우측 상단에서 연도를 바꿔가며 조회 가능

매출·클라이언트 상세 정보가 노출되므로 대시보드 전체 화면과 동일하게 팀장(ADMIN)/담당자(EDITOR)만
접근할 수 있고, 파트너사(VIEWER) 계정에는 버튼 자체가 보이지 않습니다.

## 7. 다음에 이어서 할 만한 작업

- [ ] CSRF 보호 활성화 (현재는 원본과의 AJAX 호환을 위해 비활성화)
- [ ] 프로젝트/직접비 변경 이력(감사 로그) 테이블 추가
- [ ] 역할별 메뉴 접근을 `@PreAuthorize` 기반 테스트 코드로 검증
- [ ] Flyway 등 스키마 마이그레이션 도구 도입 (현재는 `schema.sql` 재실행 방식)
- [ ] 엑셀 업로드 실패 행에 대한 상세 오류 리포트 (현재는 프로젝트코드 없는 행만 조용히 skip)
