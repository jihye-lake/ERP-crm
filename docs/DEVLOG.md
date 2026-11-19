# 개발 기록

작업하면서 막혔던 것, 바꾼 이유 등을 날짜별로 적어둔 메모.

---

## 2026-11-13 — 실행이 안 되는 문제 정리

IntelliJ에서 실행하자마자 `Application run failed`로 꺼짐. 하나 고치면 다음 게 나와서 순서대로 정리.

### 1. PostgreSQL 접속 실패 — `erp_user` 계정이 없었음

```
HikariPool-1 - Exception during pool initialization.
org.postgresql.util.PSQLException: ?????????: ????? "erp_user"?? password ?????? ??????????
```

메시지가 `?????`로 깨져서 처음엔 비밀번호 문제인 줄 알았는데, PostgreSQL 서버 로그
(`C:\Program Files\PostgreSQL\18\data\log\`)를 열어보니 진짜 원인은 이거였음.

```
치명적오류:  사용자 "erp_user"의 password 인증을 실패했습니다
상세정보:  "erp_user" 롤 없음
```

PostgreSQL은 설치만 하고 `setup_db.sql`을 안 돌린 상태였음. 스크립트 실행 후 해결.

> 메시지가 깨지는 이유: 한국어 로케일로 설치하면 서버가 CP949로 에러를 보내는데,
> 접속(인증) 단계는 클라이언트 인코딩을 맞추기 전이라 그대로 깨져서 들어옴.
> 앞으로 비슷한 거 나오면 서버 로그부터 볼 것. (`postgresql.conf`의 `lc_messages = 'C'`로 바꾸면 영어로 나옴)

### 2. H2 프로필도 실패 — 스키마 문법 차이

DB 없이 띄워보려고 h2 프로필로 돌렸더니 이번엔 테이블 생성에서 실패.

```
JdbcSQLSyntaxErrorException: Syntax error in SQL statement
"CREATE TABLE project ( id UUID PRIMARY KEY [*]DEFAULT gen_random_uuid(), ..."
```

`schema.sql`이 PostgreSQL 기준이라 H2(PostgreSQL 호환 모드여도)가 못 읽는 부분이 있었음.

| PostgreSQL | H2 |
|---|---|
| `gen_random_uuid()` | `RANDOM_UUID()` |
| `GENERATED ALWAYS AS (...) STORED` | `GENERATED ALWAYS AS (...)` |
| `PRIMARY KEY DEFAULT ...` | `DEFAULT ... PRIMARY KEY` (순서) |

→ H2 전용 `schema-h2.sql`을 따로 두고 h2 프로필에서만 쓰게 함.
스키마 바뀌면 두 군데 다 고쳐야 하는 게 단점이라, 쿼리 검증은 결국 Testcontainers로 하기로 함 (아래).

### 3. 로그인 후 대시보드 500 에러

```
Exception evaluating SpringEL expression: "row.id" (template: "dashboard/list" - line 136)
Caused by: java.lang.UnsupportedOperationException:
  Cannot project byte[] to java.util.UUID; Target type is not an interface and no matching Converter found
```

목록 조회가 네이티브 쿼리 + 인터페이스 프로젝션인데, 네이티브 쿼리라 Hibernate가 UUID 컬럼을
엔티티 타입 정보 없이 JDBC 값 그대로 넘겨줌. H2에서는 이게 `byte[]`로 와서 `UUID getId()`로 변환을 못 함.

`id`는 화면에서 `data-id`랑 URL에만 쓰이니까 그냥 문자열로 받음.

```java
// ProjectRepository
"select cast(p.id as varchar) as id, ..."

// ProjectListRow
String getId();   // UUID → String
```

### 4. (예방) data.sql 한글 깨짐

Java 17은 윈도우에서 기본 문자셋이 MS949 (UTF-8 기본은 Java 18부터).
`spring.sql.init.encoding`을 안 주면 UTF-8로 저장된 시드 데이터의 한글이 깨져서 들어갈 수 있어서 명시함.

### 확인

h2 프로필로 띄워서 admin / editor / partner 각각 로그인 → 대시보드, 모니터, 통계, 직접비 입력·저장,
매출/Kcode 수정, 세션 연장까지 확인. partner는 권한 없는 화면에서 403 나오는 것도 확인.

---

## 2026-11-13 — Docker, CI, Maven Wrapper

다른 PC에서 받아서 바로 실행이 안 되는 게 제일 문제라고 생각해서 먼저 처리.

- **Maven Wrapper 추가** — IntelliJ 내장 Maven만 있는 환경이라 터미널에서 `mvn`이 안 됐음. `./mvnw`로 통일
- **Dockerfile** — 멀티스테이지(JDK로 빌드 → JRE로 실행), 의존성 레이어 분리, root 아닌 계정으로 실행
- **docker-compose** — 기존엔 postgres만 있었는데 app 추가. `depends_on` + `healthcheck`로 DB 준비된 다음에 앱이 뜨게 함
- **DB 접속 정보 환경변수화** — `DB_URL`, `DB_USERNAME`, `DB_PASSWORD` (기본값은 로컬 개발용)
- **GitHub Actions** — push/PR 때 `./mvnw verify` + 도커 이미지 빌드
- **.gitattributes** — 윈도우에서 커밋하면 `mvnw`가 CRLF가 될 수 있어서 LF 고정. Dockerfile에서도 한 번 더 `sed`로 정리

---

## 2026-11-13 — schema.sql → Flyway

기존엔 `spring.sql.init.mode: always`라 **실행할 때마다 테이블을 DROP하고 다시 만들었음.**
데모용이라 넘어갔던 건데, 입력한 데이터가 재시작하면 다 날아가니 사실상 실제로는 못 쓰는 구조.

- `schema.sql` → `db/migration/V1__init_schema.sql`
- `data.sql` → `db/migration/V2__demo_seed.sql`
- 새 테이블은 `V3__project_audit_log.sql`로 추가
- `git mv`로 옮겨서 이력은 유지

h2 프로필은 마이그레이션 SQL이 PostgreSQL 문법이라 Flyway를 끄고, `schema-h2.sql` + V2 시드를 그대로 씀.

> 참고: Spring Boot 3.2가 관리하는 Flyway 버전(9.x)은 PostgreSQL 16까지 공식 지원. 로컬에 깔린 건 18이라
> 경고는 나올 수 있음. Docker / CI는 16으로 맞춰둠.

---

## 2026-11-13 — 변경 이력(감사 로그)

매출, Kcode, 직접비를 누가 언제 바꿨는지 남는 곳이 없었음.

- 대상: 매출 수정 / Kcode 수정 / 직접비 저장 / 프로젝트 삭제 / 엑셀 업로드
- 이전 값, 이후 값, 작업자, 시각 저장
- 직접비는 27개 항목을 하나하나 남기면 너무 많아서 **저장 단위로 합계 변화만** 기록
- 값이 그대로면 기록 안 함

고민했던 것

- **트랜잭션**: `AuditLogService.record()`를 `Propagation.MANDATORY`로 둠. 본 작업이 롤백되면 로그도 같이 롤백되고,
  트랜잭션 없이 로그만 따로 남기려고 하면 예외가 나게. (테스트로 확인)
- **FK 안 걸기**: `project_id`에 FK를 걸면 프로젝트 삭제할 때 이력도 같이 지워야 함. 삭제 이력이 제일 중요한데 그게 사라지면 의미가 없어서 FK 없이 프로젝트코드도 같이 저장
- **이력은 수정 불가**: 엔티티에 setter 없음, 컬럼 전부 `updatable = false`

관리자만 볼 수 있는 `/audit-logs` 화면 추가 (프로젝트코드 검색, 페이징).

---

## 2026-11-13 — CSRF 다시 켜기

원래 ASP 화면의 AJAX(매출 수정, Kcode 수정, 삭제)를 그대로 쓰려고 꺼뒀었음. 다시 켜면서 손댄 곳:

- 폼: `action` → `th:action`으로 바꾸면 Thymeleaf가 `_csrf` hidden 필드를 알아서 넣어줌 (로그인, 로그아웃, 엑셀 업로드)
- AJAX: 공통 레이아웃 `<head>`에 토큰을 `<meta>`로 넣고 `$.ajaxSetup`으로 모든 요청 헤더에 붙임
  → 화면별 JS는 안 건드려도 됨
- H2 콘솔만 예외 처리 (로컬 확인용)

헷갈렸던 것: 테스트에서 토큰 없이 POST 하면 403이 아니라 `/login?expired=1`로 리다이렉트됨.
세션이 아예 없는 상태에서 토큰이 없으면 Spring Security가 "세션 만료"로 보고 `invalidSessionUrl`로 보내기 때문.
실제 브라우저(세션 있음 + 토큰 틀림)는 403이 맞음. 테스트를 두 경우로 나눠서 작성.

---

## 2026-11-13 — 엑셀 업로드 검증

기존엔 프로젝트코드가 비어 있는 행을 **아무 말 없이 건너뛰고**, 금액을 못 읽으면 **0원으로 넣었음.**
업로드 결과도 화면에 안 나왔음 (redirect 파라미터로 건수만 넘기고 화면에서 안 씀).

바꾼 것

- 행별 검증: 코드 비어 있음 / 코드 형식(P + 연2 + 월2 + 순번, 월은 01~12) / 파일 안 중복 / DB 중복 / 제목 없음 / 금액 숫자 아님 / 음수
- **문제 행이 하나라도 있으면 전체 저장 안 함** + 엑셀 기준 행 번호와 사유를 표로 보여줌
- 엑셀이 아닌 파일 올리면 안내 메시지
- DB 중복 체크는 행마다 조회하지 않고 `IN` 한 번으로
- 금액에 `￦`, `,`, `원` 섞여 있어도 허용

부분 저장이 아니라 전체 취소로 한 이유: 반쯤 들어간 상태에서 엑셀 고쳐서 다시 올리면 앞에 들어간 행이 중복됨.

---

## 2026-11-13 — 테스트

전부 다 짜기보다 깨지면 문제가 큰 곳 위주로.

| 테스트 | 확인하는 것 |
|---|---|
| `DirectCostServiceTest` | 금액 = 단가 × 투여량 × 일자, 단 0/빈칸은 곱셈에서 빠짐 (원래 화면 JS 규칙) |
| `ExcelProjectImportServiceTest` | 정상 / 빈 행 무시 / 오류 행 있으면 전체 취소 / DB 중복 |
| `SecurityAccessTest` | 역할 × 화면 접근 표, 관리자 전용 기능, CSRF |
| `AuditLogIntegrationTest` | 이력 저장 내용, 같은 값이면 미기록, 삭제 후 이력 유지, MANDATORY |
| `PostgresIntegrationTest` | Testcontainers로 실제 PostgreSQL — Flyway, generated column, 검색/통계 쿼리 |

- 엑셀 테스트는 테스트 안에서 POI로 xlsx를 직접 만들어서 넣음 (파일을 리포에 안 두려고)
- 계정 관련 테스트는 `@WithUserDetails`로 시드 계정을 그대로 사용
- PostgreSQL 테스트는 Docker 없으면 skip (`disabledWithoutDocker = true`), CI에서는 실행

삽질: 테스트 클래스마다 스프링 컨텍스트가 따로 뜨는데 H2 인메모리 DB(`DB_CLOSE_DELAY=-1`)는 같은 걸 재사용해서
두 번째 컨텍스트에서 "테이블이 이미 있음"으로 실패. `schema-h2.sql` 맨 위에 `DROP TABLE IF EXISTS` 넣어서 해결.

로컬 결과: 44개 중 38개 통과, 6개 skip (PostgreSQL 테스트 — 로컬에 Docker 없음)

---

## 남은 것 / 확인 필요

- [ ] 로컬 PostgreSQL(18)로 Flyway 적용 실제 확인
- [ ] CI에서 PostgreSQL 통합 테스트 통과 확인 (첫 push 후)
- [ ] `docker compose up --build` 실제 실행 확인 (Docker 설치 후)
- [ ] README 스크린샷 추가
