-- ============================================================================
-- 1) Kcode 제거
--    Kcode(파트너 정산 코드)를 더 이상 쓰지 않으므로 컬럼/인덱스를 삭제한다.
--    requires_partner_code 는 "Kcode 입력란을 보여줄지" 판단만 하던 플래그라 같이 삭제.
--    (과거 변경 이력의 KCODE_UPDATE 행은 기록 보존을 위해 그대로 둔다)
--
-- 2) No(프로젝트 번호) 추가
--    1, 2, 3, 4 ... 순서대로 붙는 영구 번호. 중간에 삭제돼도 번호를 당기지 않는다.
--    (3번을 지우면 1, 2, 4 로 남고, 다음 등록은 5번)
--    → IDENTITY(시퀀스)는 한 번 나간 번호를 다시 쓰지 않으므로 이 성질을 DB가 보장한다.
--    GENERATED ALWAYS 라서 애플리케이션/수동 SQL로 번호를 지정하거나 바꿀 수 없다.
--
-- 기존 행 번호는 시간순(연도 → 월 → 등록시각 → 프로젝트코드)으로 1부터 매긴다.
-- H2용 같은 처리는 schema-h2.sql / schema-h2-v5-data.sql 에 있음. 여기를 고치면 거기도 같이 맞출 것!
-- ============================================================================

-- 1) Kcode 제거
DROP INDEX IF EXISTS idx_project_kcode;
ALTER TABLE project DROP COLUMN kcode;
ALTER TABLE project DROP COLUMN requires_partner_code;

-- 2) No 추가: 기존 행 번호 부여 → IDENTITY 전환 → 시퀀스를 다음 번호로
ALTER TABLE project ADD COLUMN no BIGINT;

UPDATE project
SET no = numbered.rn
FROM (
    SELECT id, row_number() OVER (ORDER BY year_code, month_code, created_at, project_code) AS rn
    FROM project
) numbered
WHERE project.id = numbered.id;

ALTER TABLE project ALTER COLUMN no SET NOT NULL;
ALTER TABLE project ALTER COLUMN no ADD GENERATED ALWAYS AS IDENTITY;
SELECT setval(pg_get_serial_sequence('project', 'no'), (SELECT coalesce(max(no), 0) + 1 FROM project), false);

ALTER TABLE project ADD CONSTRAINT uq_project_no UNIQUE (no);
