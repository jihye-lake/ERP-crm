-- ============================================================================
-- H2 전용: V4__project_code_format.sql 의 데이터 변환 부분
-- V2 시드(옛 코드 P26110001 형식)가 들어간 뒤에 실행된다.
-- 매핑 규칙은 V4와 반드시 같아야 함!
-- ============================================================================

-- 1) 옛 코드에서 월 보존
UPDATE project SET month_code = SUBSTRING(project_code FROM 4 FOR 2) WHERE project_code LIKE 'P%';

-- 2) 기존 코드 → 새 코드 매핑
CREATE TABLE project_code_map AS
SELECT id,
       old_code,
       yy || '-' || type_abbr || '-' || client_abbr || '-' ||
       LPAD(CAST(ROW_NUMBER() OVER (PARTITION BY yy, type_abbr, client_abbr ORDER BY old_code) AS VARCHAR), 2, '0') AS new_code
FROM (
    SELECT id,
           project_code AS old_code,
           SUBSTRING(project_code FROM 2 FOR 2) AS yy,
           CASE project_type
               WHEN '플랫폼구축'   THEN 'PLT'
               WHEN '데이터분석'   THEN 'ANL'
               WHEN '대시보드구축' THEN 'DSH'
               WHEN '컨설팅'       THEN 'CON'
               ELSE 'ETC'
           END AS type_abbr,
           CASE client_name
               WHEN '블루오션테크'       THEN 'BOT'
               WHEN '파인트리컨설팅'     THEN 'PTC'
               WHEN '선샤인헬스케어'     THEN 'SSH'
               WHEN '선라인마케팅'       THEN 'SLM'
               WHEN '메트로데이터랩'     THEN 'MDL'
               WHEN '그린필드전자'       THEN 'GFE'
               WHEN '노스브릿지시스템즈' THEN 'NBS'
               WHEN '클리어웨이브통신'   THEN 'CWT'
               ELSE 'ETC'
           END AS client_abbr
    FROM project
    WHERE project_code LIKE 'P%'
) src;

UPDATE project
SET project_code = (SELECT m.new_code FROM project_code_map m WHERE m.id = project.id)
WHERE id IN (SELECT id FROM project_code_map);

DROP TABLE project_code_map;

-- 3) 제약
ALTER TABLE project ALTER COLUMN month_code SET NOT NULL;
ALTER TABLE project ADD CONSTRAINT ck_project_month_code CHECK (REGEXP_LIKE(month_code, '^(0[1-9]|1[0-2])$'));
ALTER TABLE project ADD CONSTRAINT ck_project_code_format
    CHECK (REGEXP_LIKE(project_code, '^[0-9]{2}-[A-Z]{2,3}-[A-Z]{3}-[0-9]{2}$'));
ALTER TABLE project ADD CONSTRAINT uq_project_code UNIQUE (project_code);
