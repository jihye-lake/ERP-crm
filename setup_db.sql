-- ============================================================================
-- PostgreSQL을 Docker 없이 네이티브로 설치한 경우 사용하는 셋업 스크립트.
-- pgAdmin의 Query Tool에서 아래 순서대로 실행하세요.
-- ============================================================================

-- STEP 1: postgres 슈퍼유저로 접속한 상태에서 실행 (기본 postgres DB에 접속)
CREATE DATABASE erp_crm;
CREATE USER erp_user WITH PASSWORD 'erp_password';
GRANT ALL PRIVILEGES ON DATABASE erp_crm TO erp_user;

-- STEP 2: 이제 상단 DB 선택 콤보박스(또는 새 연결)에서 "erp_crm" DB로 전환한 뒤 아래를 실행
GRANT ALL ON SCHEMA public TO erp_user;
ALTER DATABASE erp_crm OWNER TO erp_user;

-- 이후 애플리케이션을 처음 실행하면 schema.sql / data.sql이 자동으로 실행되어
-- 테이블 생성과 데모 데이터 입력이 이루어집니다 (application.yml 참고).
