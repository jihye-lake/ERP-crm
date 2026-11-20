-- ============================================================================
-- 클라이언트 마스터
-- 화면에서 프로젝트를 직접 추가할 때 프로젝트코드(26-DSH-BOT-01)의 클라이언트 약어를
-- 자동으로 만들어야 해서, V4 마이그레이션 CASE 문에만 있던 "클라이언트명 → 약어" 매핑을 테이블로 옮김.
-- (화면의 클라이언트 검색도 이 테이블을 사용)
--
-- H2에서도 이 파일을 그대로 실행하므로 PostgreSQL 전용 문법(~ 정규식 등)은 쓰지 말 것!
-- ============================================================================

CREATE TABLE client (
    id    BIGSERIAL    PRIMARY KEY,
    name  VARCHAR(200) NOT NULL,
    abbr  VARCHAR(3)   NOT NULL,
    CONSTRAINT uq_client_name UNIQUE (name),
    CONSTRAINT uq_client_abbr UNIQUE (abbr),
    CONSTRAINT ck_client_abbr CHECK (char_length(abbr) = 3 AND abbr = upper(abbr))
);

INSERT INTO client (name, abbr) VALUES
('블루오션테크',       'BOT'),
('파인트리컨설팅',     'PTC'),
('선샤인헬스케어',     'SSH'),
('선라인마케팅',       'SLM'),
('메트로데이터랩',     'MDL'),
('그린필드전자',       'GFE'),
('노스브릿지시스템즈', 'NBS'),
('클리어웨이브통신',   'CWT');
