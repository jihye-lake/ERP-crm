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
| admin | admin1234 | 관리자(팀장) | 전체 + 매출/Kcode 수정, 삭제, 엑셀 업로드, 변경 이력 |
| editor | editor1234 | 운영 담당자 | 조회 + 직접비 입력, 통계 |
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
| 프로젝트코드에서 매번 `substring`으로 연/월 추출 | `year_code`, `month_code` generated column + 인덱스 |
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
코드 규칙(P + 연도2 + 월2 + 순번)이 바뀔 일이 없어서 DB에서 파생 컬럼으로 만들고 인덱스를 걸었습니다.
대신 이 형식이 깨지면 검색에 안 잡히기 때문에 엑셀 업로드에서 코드 형식을 꼭 검증하도록 했습니다.

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
| `ExcelProjectImportServiceTest` | 엑셀 정상 업로드 / 빈 행 / 코드 형식·중복·금액 오류 시 전체 취소 |
| `SecurityAccessTest` | 역할별 화면·기능 접근 권한, CSRF 토큰 없을 때 거부 |
| `AuditLogIntegrationTest` | 변경 이력 저장, 값 변화 없으면 미기록, 삭제 후에도 이력 유지, 트랜잭션 밖 호출 차단 |
| `PostgresIntegrationTest` | Flyway 마이그레이션, generated column, 검색/통계 네이티브 쿼리 (Testcontainers) |

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
  db/migration/ Flyway 스크립트 (V1 스키마, V2 데모 데이터, V3 변경 이력)
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
| Project_Delete.asp, Update_Cost.asp, Update_Kcode.asp | `ProjectAdminController` |
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
