# Nexora ERP — Classic ASP 레거시 ERP를 Spring Boot로 옮긴 기록

[![CI](https://github.com/jihye-lake/ERP-crm/actions/workflows/ci.yml/badge.svg)](https://github.com/jihye-lake/ERP-crm/actions/workflows/ci.yml)

사내에서 쓰던 **프로젝트 매출 / 직접비 관리 ERP**(Classic ASP + VBScript + MSSQL)를
Spring Boot 3 + PostgreSQL로 다시 만든 프로젝트입니다.

기능을 그대로 베끼기보다는, 원래 시스템을 쓰면서 불편했거나 위험했던 부분
(문자열로 이어붙인 SQL, 소스에 박혀 있는 비밀번호, 135개짜리 반복 컬럼 등)을 정리하는 데 집중했습니다.
회사명·클라이언트·담당자·계정은 전부 가상 데이터로 바꿨고 실제 데이터는 없습니다.

- 개발 과정 / 삽질 기록 : [docs/DEVLOG.md](docs/DEVLOG.md)

<br>

## 실행 방법

### 1) Docker (가장 간단)

```bash
docker compose up -d --build
```

http://localhost:8080 접속 → `admin` / `admin1234`

### 2) IntelliJ에서 DB 없이 바로 보기 (H2)

`ErpCrmApplication` 실행 설정 → Program arguments에 `--spring.profiles.active=h2`

메모리 DB라 앱을 끄면 데이터가 초기화됩니다.

### 3) 로컬에 설치한 PostgreSQL 사용

`setup_db.sql`로 DB와 계정(`erp_crm` / `erp_user`)을 만든 다음 그냥 실행하면 됩니다.
테이블 생성과 데모 데이터는 Flyway가 넣어줍니다. 접속 정보는 `DB_URL`, `DB_USERNAME`, `DB_PASSWORD` 환경변수로 바꿀 수 있습니다.

| 계정 | 비밀번호 | 역할 | 할 수 있는 것 |
|---|---|---|---|
| admin | admin1234 | 관리자(팀장) | 전체 + 프로젝트 추가, 매출 수정, 삭제, 엑셀 업로드, 변경 이력 |
| editor | editor1234 | 운영 담당자 | 조회 + 프로젝트 추가, 직접비 입력, 통계 |
| partner | partner1234 | 파트너사 | 제한된 목록 조회만 |

<br>

## 기술 스택

Java 17, Spring Boot 3.2, Spring Security 6, Spring Data JPA, Thymeleaf, PostgreSQL 16, Flyway, Apache POI,
JUnit 5 / Mockito / Testcontainers, Docker Compose, GitHub Actions

<br>

## 원래 시스템에서 문제였던 것들

1. **SQL을 문자열로 이어붙여서 만듦** — 검색어가 그대로 쿼리에 들어가서 SQL Injection에 노출. 페이징도 `TOP n ... NOT IN (TOP m ...)` 방식이라 뒤 페이지일수록 비효율적
2. **로그인** — 특정 IP는 비밀번호 없이 자동 로그인, 나머지는 소스코드에 평문으로 적힌 비밀번호와 비교
3. **직접비 테이블이 컬럼 135개** — 비용 항목 27개 × (단가, 투여량, 일자, 금액, 비고) 5개를 전부 컬럼으로 펼쳐둔 구조. 항목 하나 추가하려면 테이블 변경 + ASP 파일 여러 개 수정
4. **권한 체크가 페이지마다 if문** — 빠진 페이지가 있어도 알 방법이 없음
5. **엑셀 업로드에서 문제 있는 행을 그냥 버림** — 일부 행이 빠져도 아무 메시지가 없어서, 몇 건이 누락됐는지 알 수 없음
6. **누가 매출을 바꿨는지 기록이 없음**
7. IIS 전용 컴포넌트(DEXT.FileUpload), Jet OLEDB에 묶여 있어서 서버 이전이 어려움

<br>

## 어떻게 바꿨는지

| 원래 | 변경 후 |
|---|---|
| 문자열 SQL + `TOP/NOT IN` 페이징 | 파라미터 바인딩 + Spring Data `Pageable` |
| IP 자동 로그인 / 평문 비밀번호 | Spring Security 폼 로그인 + BCrypt, 역할(ADMIN/EDITOR/VIEWER) 기반 권한 |
| 페이지별 if문 권한 체크 | `@PreAuthorize`로 메서드 단위 선언 + 역할별 접근 테스트로 검증 |
| 직접비 135개 컬럼 | `direct_cost_item` 자식 테이블 (프로젝트당 최대 27행), 항목은 enum으로 관리 |
| 프로젝트코드에서 매번 `substring`으로 연/월 추출 | `year_code` generated column + `month_code` 컬럼 + 인덱스 |
| 엑셀 업로드 시 문제 행 무시 | 행별 검증 → 하나라도 문제 있으면 전체 취소하고 행 번호/사유 표시 |
| 변경 이력 없음 | `project_audit_log` 테이블 + 관리자용 변경 이력 화면 |
| DEXT.FileUpload + Jet OLEDB | MultipartFile + Apache POI (.xls / .xlsx) |
| CSRF 보호 없음 | 폼은 Thymeleaf 자동 토큰, AJAX는 공통 헤더로 전송 |

### 직접비 테이블

```
[원래] ERP_direct_cost
  IDX, 항목1_단가, 항목1_투여량, 항목1_일자, 항목1_금액, 항목1_비고,
       항목2_단가, ... (27개 항목 × 5 = 135개 컬럼)

[변경] project 1 ── N direct_cost_item
  direct_cost_item(id, project_id, category_code, unit_price, quantity, days, amount, note)
  UNIQUE(project_id, category_code)
```

<br>

## 설계하면서 고민했던 부분

**직접비를 정규화하면 집계 쿼리가 복잡해지는 문제**
원래 구조는 한 행에 다 있어서 합계가 `항목1_금액 + 항목2_금액 + ...` 한 줄이었는데, 정규화하면
목록 화면에서 프로젝트마다 직접비 합계를 따로 구해야 합니다. 엔티티로 가져와서 자바에서 더하면 프로젝트 수만큼
쿼리가 나가기 때문에(N+1), 목록 조회는 네이티브 쿼리 + 서브쿼리로 한 번에 가져오고 결과는
인터페이스 프로젝션(`ProjectListRow`)으로 받도록 했습니다.

**연/월을 generated column으로 만든 이유**
검색이 거의 항상 "몇 년 몇 월 프로젝트"인데, 원래는 `SUBSTRING(프로젝트코드, 2, 2)` 같은 조건이라 인덱스를 못 탔습니다.
처음에는 코드 규칙(P + 연도2 + 월2 + 순번)에서 연/월을 둘 다 DB 파생 컬럼으로 만들고 인덱스를 걸었습니다.

**프로젝트코드 형식 변경 (V4)**
이후 프로젝트코드를 `연도2-종류약어-클라이언트약어-순번2` 형식(예: `26-DSH-BOT-01` = 26년 / 대시보드구축 / 블루오션테크 / 1번째 건)으로 바꿨습니다.
새 코드에는 월이 없어서 `month_code`는 일반 컬럼으로 바꿔 기존 값을 보존하고, 엑셀 업로드에서는 '월' 열로 따로 받습니다.
`year_code`는 새 코드 앞 2자리에서 계속 파생합니다. 기존 데이터는 같은 (연도, 종류, 클라이언트) 안에서 시간순으로 순번을 다시 매겼고,
형식이 깨지면 검색에 안 잡히기 때문에 엑셀 업로드 검증과 DB `CHECK` / `UNIQUE` 제약을 같이 걸었습니다.

| 종류 | 약어 | | 클라이언트 | 약어 |
|---|---|---|---|---|
| 대시보드구축 | DSH | | 블루오션테크 | BOT |
| 플랫폼구축 | PLT | | 파인트리컨설팅 | PTC |
| 데이터분석 | ANL | | 선샤인헬스케어 | SSH |
| 컨설팅 | CON | | 선라인마케팅 | SLM |
| | | | 메트로데이터랩 | MDL |
| | | | 그린필드전자 | GFE |
| | | | 노스브릿지시스템즈 | NBS |
| | | | 클리어웨이브통신 | CWT |

**Kcode 제거 + 프로젝트 번호(No) 추가 (V5)**
더 이상 쓰지 않는 Kcode(파트너 정산 코드)와, Kcode 입력란 노출 여부만 판단하던 `requires_partner_code` 컬럼을 화면/DB에서 모두 뺐습니다.
대신 `no` 컬럼을 추가했습니다. 1, 2, 3 ... 순서대로 붙지만 **중간 프로젝트가 삭제돼도 번호를 당기지 않는 영구 번호**입니다
(1·2·3·4 중 3을 지우면 1·2·4로 남고, 다음 등록은 5번). PostgreSQL `GENERATED ALWAYS AS IDENTITY`라서
한 번 나간 번호는 재사용되지 않고, 애플리케이션이나 수동 SQL로 지정·변경할 수도 없습니다.
기존 데이터는 시간순(연도 → 월 → 등록시각 → 프로젝트코드)으로 1번부터 매겼고, 목록 검색창에서 No로도 찾을 수 있습니다.
엑셀 업로드 양식에서도 Kcode 열이 빠졌습니다(예전 양식을 올리면 바로 안내).

**화면에서 프로젝트 직접 추가 (V6)**
대시보드 목록 우측 `+` 버튼(관리자·운영 담당자만)으로 한 행을 추가해 제목 / 종류 / 담당자 / 클라이언트 / 매출 / 직접비를 입력합니다.
프로젝트코드는 서버가 `연도-종류약어-클라이언트약어-순번`으로 자동 생성합니다(같은 조합의 가장 큰 순번 + 1, 연/월은 화면에서 보고 있던 값).
클라이언트 약어가 필요해서 V4의 CASE 문에만 있던 매핑을 `client` 테이블로 옮겼고, 클라이언트는 이 목록에서만 고를 수 있습니다.
담당자는 기존 프로젝트의 담당자 중에서 검색하거나, 처음 등록하는 사람이면 입력한 이름을 그대로 쓸 수 있습니다.
직접비는 27개 항목 중 `9-1. 기타1`에 넣어 두므로, 나중에 직접비 입력 화면에서 항목별로 나누면 됩니다.

**AJAX 응답을 "Y"/"N" 텍스트로 그대로 둔 것**
REST 스타일 JSON으로 바꾸는 게 더 깔끔하긴 한데, 이번 범위는 서버 이전이라 화면 JS는 최소한으로만 고치고 싶었습니다.
기존 응답 형식을 그대로 맞추고, 대신 실패 시 서버 로그는 남기도록 했습니다.

**엑셀 업로드: 부분 반영 vs 전체 취소**
문제 있는 행만 빼고 넣는 방식도 생각했는데, 그러면 엑셀을 고쳐서 다시 올렸을 때 앞에서 들어간 행이 중복이 됩니다.
정산 데이터라 "애매하게 반쯤 들어간 상태"가 더 위험하다고 봐서 전체 취소 + 오류 목록을 보여주는 쪽을 택했습니다.

**감사 로그를 같은 트랜잭션에 묶은 것**
로그 저장은 `Propagation.MANDATORY`로 해서 반드시 본 작업 트랜잭션 안에서만 저장되게 했습니다.
매출 수정이 롤백됐는데 로그만 남거나, 반대로 수정은 됐는데 로그가 빠지는 경우를 없애려고요.
그리고 `project_id`에 FK를 안 걸었는데, 프로젝트를 삭제해도 "누가 지웠는지"는 남아야 해서입니다.

**H2와 PostgreSQL**
로컬에서 DB 없이 띄워볼 수 있게 H2 프로필을 남겨뒀는데, 실제로 H2에서는 되는데 PostgreSQL에서 안 되는 경우
(또는 그 반대)를 몇 번 겪었습니다 ([DEVLOG](docs/DEVLOG.md) 참고). 그래서 쿼리 검증 테스트는 H2가 아니라
Testcontainers로 실제 PostgreSQL을 띄워서 돌립니다.

<br>

## 테스트

```bash
./mvnw test
```

| 테스트 | 내용 |
|---|---|
| `DirectCostServiceTest` | 직접비 금액 계산 규칙 (원래 화면 JS의 계산 방식과 동일한지) |
| `ExcelProjectImportServiceTest` | 엑셀 정상 업로드 / 빈 행 / 코드 형식·중복·금액 오류 시 전체 취소 / 예전(Kcode) 양식 안내 |
| `ProjectCreateServiceTest` | 프로젝트 추가: 코드 자동 순번, 직접비·이력 저장, 입력값 오류, 담당자/클라이언트 검색 |
| `ProjectNoH2Test` | No 시간순 부여, 삭제돼도 번호 유지·재사용 안 함, No 검색 |
| `SecurityAccessTest` | 역할별 화면·기능 접근 권한, CSRF 토큰 없을 때 거부 |
| `AuditLogIntegrationTest` | 변경 이력 저장, 값 변화 없으면 미기록, 삭제 후에도 이력 유지, 트랜잭션 밖 호출 차단 |
| `PostgresIntegrationTest` | Flyway 마이그레이션(V4 코드 변환, V5 No/Kcode 제거), generated column, No 영구성·수정 불가, 검색/통계 네이티브 쿼리 (Testcontainers) |

`PostgresIntegrationTest`는 Docker가 필요해서 Docker가 없는 PC에서는 자동으로 skip 되고, GitHub Actions에서는 항상 실행됩니다.

<br>

## 프로젝트 구조

```
src/main/java/com/nexora/erp
  config/       Spring Security 설정, 로그인 후 역할별 이동
  domain/       엔티티 (Project, DirectCostItem, ProjectAuditLog, ...)
  repository/   JPA 리포지토리, 목록/통계용 네이티브 쿼리 + 프로젝션
  service/      검색·합계, 직접비 계산, 엑셀 업로드, 변경 이력
  web/          컨트롤러, 폼 DTO
src/main/resources
  db/migration/ Flyway 스크립트 (V1 스키마, V2 데모 데이터, V3 변경 이력, V4 코드 형식, V5 No 추가·Kcode 제거, V6 클라이언트 마스터)
  schema-h2.sql H2 프로필 전용 스키마
  templates/    Thymeleaf 화면
```

### 원래 파일 ↔ 이번 코드

| 원래 ASP | 이번 코드 |
|---|---|
| Login.asp, Login_process.asp | `SecurityConfig`, `AdminUserDetailsService`, `LoginSuccessHandler` |
| index.asp (관리자/담당자 대시보드) | `DashboardController#fullDashboard` |
| monitor.asp (파트너사용 조회) | `DashboardController#monitorDashboard` |
| DirectCost/index.asp, Check.asp, DirectCost_Pro.asp | `DirectCostController`, `DirectCostService` |
| Project_Upload.asp | `ExcelProjectImportService` |
| Project_Delete.asp, Update_Cost.asp | `ProjectAdminController` (Update_Kcode.asp는 V5에서 Kcode와 함께 제거) |
| extend_session.asp | `SessionController` |
| (새로 추가) 통계 화면 | `StatisticsController` — 월별/연도별/클라이언트별/항목별 차트 (Chart.js) |
| (새로 추가) 변경 이력 | `AuditLogController` |

<br>

## 아직 남은 것

- [ ] AJAX 응답을 Y/N 텍스트에서 JSON + HTTP 상태코드로 정리 (화면 JS 같이 수정 필요)
- [ ] 직접비 항목(27개)을 enum이 아니라 테이블로 빼서 관리자 화면에서 추가/수정
- [ ] 비밀번호 변경, 로그인 실패 횟수 제한
- [ ] 변경 이력 엑셀 다운로드

<br>

## 참고

- 데이터: 회사(Nexora), 클라이언트 8곳, 담당자, 프로젝트 36건 / 직접비 88건 모두 가상입니다. 데모 계정 비밀번호는 예시용입니다.
- 원래 시스템에 같이 있던, 이 ERP와 관계없는 부가 모듈(`LIB/` 하위)은 옮기지 않았습니다.
- 개발하면서 AI 코딩 도구(Claude Code)를 함께 사용했습니다. 주로 실행 오류 원인 추적, 테스트 코드 초안, Docker/CI 설정, 문서 정리에 활용했고,
  나온 결과는 직접 실행하고 테스트를 돌려보면서 확인한 뒤 반영했습니다.
