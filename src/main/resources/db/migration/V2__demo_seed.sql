-- ============================================================================
-- Nexora ERP - 데모 시드 데이터
-- 전부 가상의 인물/회사/프로젝트입니다. 실제 데이터가 아닙니다.
-- 2025년 6건 x 2 + 2026년 12개월 x 2건 = 총 36개 프로젝트, 통계 화면(/statistics)의
-- 월별/연도별/클라이언트별/항목별 차트가 실제로 값을 갖도록 1년 전체에 걸쳐 시드했습니다.
-- ============================================================================

-- 데모 계정 (비밀번호는 BCrypt 해시. 평문은 README 참고)
INSERT INTO admin_account (username, password_hash, display_name, role) VALUES
('admin',   '$2b$10$DN1wNctp/HucRc64H4zhe.7kq4T2WECwX.3a0P5Vl/l3zEq6tDSiy', '관리자',      'ADMIN'),
('editor',  '$2b$10$btIoA7XnSectvufNmfYVeeVhDDqB3H3zZVTpqRRO5fdk/cb4bfAKa', '운영 담당자',  'EDITOR'),
('partner', '$2b$10$6BpASfdjRBPdnVeFHEqNg.hMO1agDg045xQUmaYqWgOkxQ4zDXsBG', '파트너사',    'VIEWER');

-- 데모 프로젝트 (가상 데이터, 2025~2026년 전체 월에 걸쳐 분포)
INSERT INTO project (project_code, kcode, title, project_type, manager, client_name, client_manager, revenue_amount, requires_partner_code) VALUES
('P25010001', NULL, '스마트홈 기기 사용성 온라인 플랫폼 구축', '플랫폼구축', '김도윤', '파인트리컨설팅', '장하윤', 7100000, true),
('P25010002', NULL, '모바일 뱅킹 앱 UX 개선 온라인 플랫폼 구축', '플랫폼구축', '박서연', '선샤인헬스케어', '강태현', 6690000, false),
('P25030001', 'K25-55', '리테일 매장 방문 경험 데이터분석', '데이터분석', '박서연', '선라인마케팅', '최유진', 8640000, true),
('P25030002', 'K25-47', 'OTT 서비스 이용 행태 데이터분석', '데이터분석', '이하늘', '선라인마케팅', '최유진', 4160000, true),
('P25050001', NULL, '전기차 충전 인프라 만족도 대시보드구축', '대시보드구축', '이하늘', '메트로데이터랩', '서지우', 6510000, false),
('P25050002', NULL, '프리미엄 가전 구매 여정 워크숍', '컨설팅', '이하늘', '선라인마케팅', '최유진', 8440000, false),
('P25070001', NULL, '친환경 포장재 인식 온라인 플랫폼 구축', '플랫폼구축', '박서연', '메트로데이터랩', '서지우', 6560000, false),
('P25070002', NULL, '금융 앱 온보딩 경험 개선 컨설팅', '컨설팅', '최지안', '메트로데이터랩', '서지우', 6670000, false),
('P25090001', NULL, '건강기능식품 구매 트렌드 대시보드구축', '대시보드구축', '최지안', '그린필드전자', '한소율', 9450000, false),
('P25090002', NULL, '반려동물 용품 구매 채널 데이터분석', '데이터분석', '이하늘', '그린필드전자', '한소율', 7920000, false),
('P25110001', NULL, '중고거래 플랫폼 신뢰도 데이터분석', '데이터분석', '박서연', '노스브릿지시스템즈', '오지훈', 9270000, false),
('P25110002', NULL, 'B2B SaaS 도입 의사결정 대시보드구축', '대시보드구축', '김도윤', '선라인마케팅', '최유진', 12510000, false),
('P26010001', NULL, '제로웨이스트 소비 행태 대시보드구축', '대시보드구축', '최지안', '노스브릿지시스템즈', '오지훈', 8440000, false),
('P26010002', NULL, '차세대 뷰티 디바이스 컨셉 워크숍', '컨설팅', '이하늘', '클리어웨이브통신', '윤아름', 12680000, false),
('P26020001', NULL, '원격의료 서비스 인식 대시보드구축', '대시보드구축', '김도윤', '그린필드전자', '한소율', 15870000, false),
('P26020002', NULL, '구독 서비스 이탈 원인 분석 컨설팅', '컨설팅', '박서연', '노스브릿지시스템즈', '오지훈', 16130000, false),
('P26030001', NULL, '음원 스트리밍 서비스 만족도 온라인 플랫폼 구축', '플랫폼구축', '김도윤', '선샤인헬스케어', '강태현', 9940000, false),
('P26030002', NULL, '구독형 커머스 이용 경험 온라인 플랫폼 구축', '플랫폼구축', '박서연', '선라인마케팅', '최유진', 7190000, false),
('P26040001', NULL, '웨어러블 기기 구매 의향 온라인 플랫폼 구축', '플랫폼구축', '김도윤', '블루오션테크', '정민준', 5560000, false),
('P26040002', NULL, '간편결제 서비스 이용 대시보드구축', '대시보드구축', '최지안', '블루오션테크', '정민준', 10550000, true),
('P26050001', NULL, '펫테크 제품 구매 태도 대시보드구축', '대시보드구축', '박서연', '파인트리컨설팅', '장하윤', 7650000, false),
('P26050002', 'K26-40', '재택근무 도구 만족도 대시보드구축', '대시보드구축', '김도윤', '그린필드전자', '한소율', 18280000, true),
('P26060001', NULL, '골프웨어 브랜드 선호도 데이터분석', '데이터분석', '이하늘', '선라인마케팅', '최유진', 15230000, false),
('P26060002', NULL, 'OTT 광고 수용도 온라인 플랫폼 구축', '플랫폼구축', '김도윤', '클리어웨이브통신', '윤아름', 14420000, false),
('P26070001', NULL, '홈트레이닝 앱 이용 행태 온라인 플랫폼 구축', '플랫폼구축', '박서연', '메트로데이터랩', '서지우', 8780000, true),
('P26070002', NULL, '스마트홈 기기 사용성 온라인 플랫폼 구축', '플랫폼구축', '박서연', '파인트리컨설팅', '장하윤', 5930000, false),
('P26080001', 'K26-45', '캠핑용품 구매 채널 데이터분석', '데이터분석', '최지안', '파인트리컨설팅', '장하윤', 18190000, true),
('P26080002', NULL, '프리미엄 가전 구매 여정 워크숍', '컨설팅', '최지안', '블루오션테크', '정민준', 5870000, true),
('P26090001', 'K26-23', '홈카페 트렌드 데이터분석', '데이터분석', '이하늘', '블루오션테크', '정민준', 10000000, true),
('P26090002', NULL, '전기차 충전 인프라 만족도 대시보드구축', '대시보드구축', '최지안', '블루오션테크', '정민준', 7000000, false),
('P26100001', 'K26-35', '건강기능식품 구매 트렌드 대시보드구축', '대시보드구축', '김도윤', '선샤인헬스케어', '강태현', 18630000, true),
('P26100002', 'K26-45', '모바일 뱅킹 앱 UX 개선 온라인 플랫폼 구축', '플랫폼구축', '박서연', '선샤인헬스케어', '강태현', 9500000, true),
('P26110001', NULL, 'B2B SaaS 도입 의사결정 대시보드구축', '대시보드구축', '이하늘', '블루오션테크', '정민준', 5810000, false),
('P26110002', NULL, '제로웨이스트 소비 행태 대시보드구축', '대시보드구축', '이하늘', '블루오션테크', '정민준', 15890000, false),
('P26120001', NULL, '친환경 포장재 인식 온라인 플랫폼 구축', '플랫폼구축', '이하늘', '메트로데이터랩', '서지우', 15120000, false),
('P26120002', NULL, '친환경차 보조금 인지도 데이터분석', '데이터분석', '박서연', '선라인마케팅', '최유진', 11050000, false);

-- 데모 직접비 내역 (모든 프로젝트에 2~3건씩 시드하여 통계 화면의 항목별 비중이 실제로 나타나도록 구성)
INSERT INTO direct_cost_item (project_id, category_code, unit_price, quantity, days, amount, note)
SELECT id, 'TRANS', 227000, 1, 1, 227000, '출장 교통/숙박비' FROM project WHERE project_code = 'P25010001';

INSERT INTO direct_cost_item (project_id, category_code, unit_price, quantity, days, amount, note)
SELECT id, 'ETC_1', 435000, 1, 1, 435000, '기타 진행비' FROM project WHERE project_code = 'P25010001';

INSERT INTO direct_cost_item (project_id, category_code, unit_price, quantity, days, amount, note)
SELECT id, 'GIVES_1', 2553000, 1, 1, 2553000, '고객 사은품' FROM project WHERE project_code = 'P25010001';

INSERT INTO direct_cost_item (project_id, category_code, unit_price, quantity, days, amount, note)
SELECT id, 'OUTSOURCE_1', 469000, 1, 1, 469000, '개발 외주' FROM project WHERE project_code = 'P25010002';

INSERT INTO direct_cost_item (project_id, category_code, unit_price, quantity, days, amount, note)
SELECT id, 'INSURANCE', 1761000, 1, 1, 1761000, '출장/행사 보험료' FROM project WHERE project_code = 'P25010002';

INSERT INTO direct_cost_item (project_id, category_code, unit_price, quantity, days, amount, note)
SELECT id, 'PARTNER_COST_OUT', 412000, 1, 1, 412000, '추가 파트너 정산비' FROM project WHERE project_code = 'P25030001';

INSERT INTO direct_cost_item (project_id, category_code, unit_price, quantity, days, amount, note)
SELECT id, 'OUTSOURCE_1', 2096000, 1, 1, 2096000, '개발 외주' FROM project WHERE project_code = 'P25030001';

INSERT INTO direct_cost_item (project_id, category_code, unit_price, quantity, days, amount, note)
SELECT id, 'ETC_1', 1848000, 1, 1, 1848000, '기타 진행비' FROM project WHERE project_code = 'P25030001';

INSERT INTO direct_cost_item (project_id, category_code, unit_price, quantity, days, amount, note)
SELECT id, 'OUTSOURCE_2', 995000, 1, 1, 995000, '데이터 정제 외주' FROM project WHERE project_code = 'P25030002';

INSERT INTO direct_cost_item (project_id, category_code, unit_price, quantity, days, amount, note)
SELECT id, 'INSURANCE', 1273000, 1, 1, 1273000, '출장/행사 보험료' FROM project WHERE project_code = 'P25030002';

INSERT INTO direct_cost_item (project_id, category_code, unit_price, quantity, days, amount, note)
SELECT id, 'TRANS', 791000, 1, 1, 791000, '출장 교통/숙박비' FROM project WHERE project_code = 'P25050001';

INSERT INTO direct_cost_item (project_id, category_code, unit_price, quantity, days, amount, note)
SELECT id, 'MEALS', 2026000, 1, 1, 2026000, '투입 인력 식대' FROM project WHERE project_code = 'P25050001';

INSERT INTO direct_cost_item (project_id, category_code, unit_price, quantity, days, amount, note)
SELECT id, 'OUTSOURCE_2', 179000, 1, 1, 179000, '데이터 정제 외주' FROM project WHERE project_code = 'P25050001';

INSERT INTO direct_cost_item (project_id, category_code, unit_price, quantity, days, amount, note)
SELECT id, 'OUTSOURCE_1', 3991000, 1, 1, 3991000, '개발 외주' FROM project WHERE project_code = 'P25050002';

INSERT INTO direct_cost_item (project_id, category_code, unit_price, quantity, days, amount, note)
SELECT id, 'BOOKS', 83000, 1, 1, 83000, '산출물 인쇄비' FROM project WHERE project_code = 'P25050002';

INSERT INTO direct_cost_item (project_id, category_code, unit_price, quantity, days, amount, note)
SELECT id, 'GIVES_1', 1299000, 1, 1, 1299000, '고객 사은품' FROM project WHERE project_code = 'P25070001';

INSERT INTO direct_cost_item (project_id, category_code, unit_price, quantity, days, amount, note)
SELECT id, 'ETC_1', 96000, 1, 1, 96000, '기타 진행비' FROM project WHERE project_code = 'P25070001';

INSERT INTO direct_cost_item (project_id, category_code, unit_price, quantity, days, amount, note)
SELECT id, 'BOOKS', 1022000, 1, 1, 1022000, '산출물 인쇄비' FROM project WHERE project_code = 'P25070001';

INSERT INTO direct_cost_item (project_id, category_code, unit_price, quantity, days, amount, note)
SELECT id, 'PARTNER_COST_OUT', 1007000, 1, 1, 1007000, '추가 파트너 정산비' FROM project WHERE project_code = 'P25070002';

INSERT INTO direct_cost_item (project_id, category_code, unit_price, quantity, days, amount, note)
SELECT id, 'MEALS', 936000, 1, 1, 936000, '투입 인력 식대' FROM project WHERE project_code = 'P25070002';

INSERT INTO direct_cost_item (project_id, category_code, unit_price, quantity, days, amount, note)
SELECT id, 'ETC_1', 1383000, 1, 1, 1383000, '기타 진행비' FROM project WHERE project_code = 'P25090001';

INSERT INTO direct_cost_item (project_id, category_code, unit_price, quantity, days, amount, note)
SELECT id, 'PARTNER_COST_OUT', 1660000, 1, 1, 1660000, '추가 파트너 정산비' FROM project WHERE project_code = 'P25090001';

INSERT INTO direct_cost_item (project_id, category_code, unit_price, quantity, days, amount, note)
SELECT id, 'GIVES_1', 2141000, 1, 1, 2141000, '고객 사은품' FROM project WHERE project_code = 'P25090001';

INSERT INTO direct_cost_item (project_id, category_code, unit_price, quantity, days, amount, note)
SELECT id, 'MEALS', 919000, 1, 1, 919000, '투입 인력 식대' FROM project WHERE project_code = 'P25090002';

INSERT INTO direct_cost_item (project_id, category_code, unit_price, quantity, days, amount, note)
SELECT id, 'GIVES_1', 828000, 1, 1, 828000, '고객 사은품' FROM project WHERE project_code = 'P25090002';

INSERT INTO direct_cost_item (project_id, category_code, unit_price, quantity, days, amount, note)
SELECT id, 'PARTNER_COST_OUT', 1310000, 1, 1, 1310000, '추가 파트너 정산비' FROM project WHERE project_code = 'P25090002';

INSERT INTO direct_cost_item (project_id, category_code, unit_price, quantity, days, amount, note)
SELECT id, 'PARTNER_COST_OUT', 2030000, 1, 1, 2030000, '추가 파트너 정산비' FROM project WHERE project_code = 'P25110001';

INSERT INTO direct_cost_item (project_id, category_code, unit_price, quantity, days, amount, note)
SELECT id, 'INSURANCE', 458000, 1, 1, 458000, '출장/행사 보험료' FROM project WHERE project_code = 'P25110001';

INSERT INTO direct_cost_item (project_id, category_code, unit_price, quantity, days, amount, note)
SELECT id, 'BOOKS', 1328000, 1, 1, 1328000, '산출물 인쇄비' FROM project WHERE project_code = 'P25110001';

INSERT INTO direct_cost_item (project_id, category_code, unit_price, quantity, days, amount, note)
SELECT id, 'GIVES_1', 2877000, 1, 1, 2877000, '고객 사은품' FROM project WHERE project_code = 'P25110002';

INSERT INTO direct_cost_item (project_id, category_code, unit_price, quantity, days, amount, note)
SELECT id, 'MEALS', 571000, 1, 1, 571000, '투입 인력 식대' FROM project WHERE project_code = 'P25110002';

INSERT INTO direct_cost_item (project_id, category_code, unit_price, quantity, days, amount, note)
SELECT id, 'TRANS', 2964000, 1, 1, 2964000, '출장 교통/숙박비' FROM project WHERE project_code = 'P26010001';

INSERT INTO direct_cost_item (project_id, category_code, unit_price, quantity, days, amount, note)
SELECT id, 'BOOKS', 681000, 1, 1, 681000, '산출물 인쇄비' FROM project WHERE project_code = 'P26010001';

INSERT INTO direct_cost_item (project_id, category_code, unit_price, quantity, days, amount, note)
SELECT id, 'TRANS', 2363000, 1, 1, 2363000, '출장 교통/숙박비' FROM project WHERE project_code = 'P26010002';

INSERT INTO direct_cost_item (project_id, category_code, unit_price, quantity, days, amount, note)
SELECT id, 'PERSONS_1', 1749000, 1, 1, 1749000, '투입 인력 인건비' FROM project WHERE project_code = 'P26010002';

INSERT INTO direct_cost_item (project_id, category_code, unit_price, quantity, days, amount, note)
SELECT id, 'TRANS', 416000, 1, 1, 416000, '출장 교통/숙박비' FROM project WHERE project_code = 'P26020001';

INSERT INTO direct_cost_item (project_id, category_code, unit_price, quantity, days, amount, note)
SELECT id, 'BOOKS', 4640000, 1, 1, 4640000, '산출물 인쇄비' FROM project WHERE project_code = 'P26020001';

INSERT INTO direct_cost_item (project_id, category_code, unit_price, quantity, days, amount, note)
SELECT id, 'OUTSOURCE_1', 3491000, 1, 1, 3491000, '개발 외주' FROM project WHERE project_code = 'P26020002';

INSERT INTO direct_cost_item (project_id, category_code, unit_price, quantity, days, amount, note)
SELECT id, 'TRANS', 3304000, 1, 1, 3304000, '출장 교통/숙박비' FROM project WHERE project_code = 'P26020002';

INSERT INTO direct_cost_item (project_id, category_code, unit_price, quantity, days, amount, note)
SELECT id, 'PARTNER_COST_COMP', 321000, 1, 1, 321000, '완료 건 파트너 정산비' FROM project WHERE project_code = 'P26030001';

INSERT INTO direct_cost_item (project_id, category_code, unit_price, quantity, days, amount, note)
SELECT id, 'OUTSOURCE_1', 3513000, 1, 1, 3513000, '개발 외주' FROM project WHERE project_code = 'P26030001';

INSERT INTO direct_cost_item (project_id, category_code, unit_price, quantity, days, amount, note)
SELECT id, 'INSURANCE', 43000, 1, 1, 43000, '출장/행사 보험료' FROM project WHERE project_code = 'P26030001';

INSERT INTO direct_cost_item (project_id, category_code, unit_price, quantity, days, amount, note)
SELECT id, 'PARTNER_COST_OUT', 1883000, 1, 1, 1883000, '추가 파트너 정산비' FROM project WHERE project_code = 'P26030002';

INSERT INTO direct_cost_item (project_id, category_code, unit_price, quantity, days, amount, note)
SELECT id, 'OUTSOURCE_2', 216000, 1, 1, 216000, '데이터 정제 외주' FROM project WHERE project_code = 'P26030002';

INSERT INTO direct_cost_item (project_id, category_code, unit_price, quantity, days, amount, note)
SELECT id, 'OUTSOURCE_1', 2368000, 1, 1, 2368000, '개발 외주' FROM project WHERE project_code = 'P26040001';

INSERT INTO direct_cost_item (project_id, category_code, unit_price, quantity, days, amount, note)
SELECT id, 'PARTNER_COST_COMP', 436000, 1, 1, 436000, '완료 건 파트너 정산비' FROM project WHERE project_code = 'P26040001';

INSERT INTO direct_cost_item (project_id, category_code, unit_price, quantity, days, amount, note)
SELECT id, 'GIVES_1', 3888000, 1, 1, 3888000, '고객 사은품' FROM project WHERE project_code = 'P26040002';

INSERT INTO direct_cost_item (project_id, category_code, unit_price, quantity, days, amount, note)
SELECT id, 'OUTSOURCE_2', 189000, 1, 1, 189000, '데이터 정제 외주' FROM project WHERE project_code = 'P26040002';

INSERT INTO direct_cost_item (project_id, category_code, unit_price, quantity, days, amount, note)
SELECT id, 'OUTSOURCE_2', 518000, 1, 1, 518000, '데이터 정제 외주' FROM project WHERE project_code = 'P26050001';

INSERT INTO direct_cost_item (project_id, category_code, unit_price, quantity, days, amount, note)
SELECT id, 'GIVES_1', 111000, 1, 1, 111000, '고객 사은품' FROM project WHERE project_code = 'P26050001';

INSERT INTO direct_cost_item (project_id, category_code, unit_price, quantity, days, amount, note)
SELECT id, 'PERSONS_1', 2611000, 1, 1, 2611000, '투입 인력 인건비' FROM project WHERE project_code = 'P26050001';

INSERT INTO direct_cost_item (project_id, category_code, unit_price, quantity, days, amount, note)
SELECT id, 'OUTSOURCE_1', 2696000, 1, 1, 2696000, '개발 외주' FROM project WHERE project_code = 'P26050002';

INSERT INTO direct_cost_item (project_id, category_code, unit_price, quantity, days, amount, note)
SELECT id, 'PERSONS_1', 3815000, 1, 1, 3815000, '투입 인력 인건비' FROM project WHERE project_code = 'P26050002';

INSERT INTO direct_cost_item (project_id, category_code, unit_price, quantity, days, amount, note)
SELECT id, 'GIVES_1', 272000, 1, 1, 272000, '고객 사은품' FROM project WHERE project_code = 'P26050002';

INSERT INTO direct_cost_item (project_id, category_code, unit_price, quantity, days, amount, note)
SELECT id, 'OUTSOURCE_2', 993000, 1, 1, 993000, '데이터 정제 외주' FROM project WHERE project_code = 'P26060001';

INSERT INTO direct_cost_item (project_id, category_code, unit_price, quantity, days, amount, note)
SELECT id, 'INSURANCE', 3904000, 1, 1, 3904000, '출장/행사 보험료' FROM project WHERE project_code = 'P26060001';

INSERT INTO direct_cost_item (project_id, category_code, unit_price, quantity, days, amount, note)
SELECT id, 'MEALS', 7446000, 1, 1, 7446000, '투입 인력 식대' FROM project WHERE project_code = 'P26060002';

INSERT INTO direct_cost_item (project_id, category_code, unit_price, quantity, days, amount, note)
SELECT id, 'INSURANCE', 467000, 1, 1, 467000, '출장/행사 보험료' FROM project WHERE project_code = 'P26060002';

INSERT INTO direct_cost_item (project_id, category_code, unit_price, quantity, days, amount, note)
SELECT id, 'ETC_1', 2176000, 1, 1, 2176000, '기타 진행비' FROM project WHERE project_code = 'P26070001';

INSERT INTO direct_cost_item (project_id, category_code, unit_price, quantity, days, amount, note)
SELECT id, 'GIVES_1', 1144000, 1, 1, 1144000, '고객 사은품' FROM project WHERE project_code = 'P26070001';

INSERT INTO direct_cost_item (project_id, category_code, unit_price, quantity, days, amount, note)
SELECT id, 'BOOKS', 727000, 1, 1, 727000, '산출물 인쇄비' FROM project WHERE project_code = 'P26070001';

INSERT INTO direct_cost_item (project_id, category_code, unit_price, quantity, days, amount, note)
SELECT id, 'INSURANCE', 1262000, 1, 1, 1262000, '출장/행사 보험료' FROM project WHERE project_code = 'P26070002';

INSERT INTO direct_cost_item (project_id, category_code, unit_price, quantity, days, amount, note)
SELECT id, 'TRANS', 1540000, 1, 1, 1540000, '출장 교통/숙박비' FROM project WHERE project_code = 'P26070002';

INSERT INTO direct_cost_item (project_id, category_code, unit_price, quantity, days, amount, note)
SELECT id, 'BOOKS', 3956000, 1, 1, 3956000, '산출물 인쇄비' FROM project WHERE project_code = 'P26080001';

INSERT INTO direct_cost_item (project_id, category_code, unit_price, quantity, days, amount, note)
SELECT id, 'MEALS', 832000, 1, 1, 832000, '투입 인력 식대' FROM project WHERE project_code = 'P26080001';

INSERT INTO direct_cost_item (project_id, category_code, unit_price, quantity, days, amount, note)
SELECT id, 'OUTSOURCE_1', 199000, 1, 1, 199000, '개발 외주' FROM project WHERE project_code = 'P26080002';

INSERT INTO direct_cost_item (project_id, category_code, unit_price, quantity, days, amount, note)
SELECT id, 'MEALS', 1844000, 1, 1, 1844000, '투입 인력 식대' FROM project WHERE project_code = 'P26080002';

INSERT INTO direct_cost_item (project_id, category_code, unit_price, quantity, days, amount, note)
SELECT id, 'PARTNER_COST_COMP', 1014000, 1, 1, 1014000, '완료 건 파트너 정산비' FROM project WHERE project_code = 'P26080002';

INSERT INTO direct_cost_item (project_id, category_code, unit_price, quantity, days, amount, note)
SELECT id, 'MEALS', 1636000, 1, 1, 1636000, '투입 인력 식대' FROM project WHERE project_code = 'P26090001';

INSERT INTO direct_cost_item (project_id, category_code, unit_price, quantity, days, amount, note)
SELECT id, 'TRANS', 863000, 1, 1, 863000, '출장 교통/숙박비' FROM project WHERE project_code = 'P26090001';

INSERT INTO direct_cost_item (project_id, category_code, unit_price, quantity, days, amount, note)
SELECT id, 'ETC_1', 1061000, 1, 1, 1061000, '기타 진행비' FROM project WHERE project_code = 'P26090001';

INSERT INTO direct_cost_item (project_id, category_code, unit_price, quantity, days, amount, note)
SELECT id, 'TRANS', 2272000, 1, 1, 2272000, '출장 교통/숙박비' FROM project WHERE project_code = 'P26090002';

INSERT INTO direct_cost_item (project_id, category_code, unit_price, quantity, days, amount, note)
SELECT id, 'BOOKS', 175000, 1, 1, 175000, '산출물 인쇄비' FROM project WHERE project_code = 'P26090002';

INSERT INTO direct_cost_item (project_id, category_code, unit_price, quantity, days, amount, note)
SELECT id, 'TRANS', 3929000, 1, 1, 3929000, '출장 교통/숙박비' FROM project WHERE project_code = 'P26100001';

INSERT INTO direct_cost_item (project_id, category_code, unit_price, quantity, days, amount, note)
SELECT id, 'ETC_1', 5291000, 1, 1, 5291000, '기타 진행비' FROM project WHERE project_code = 'P26100001';

INSERT INTO direct_cost_item (project_id, category_code, unit_price, quantity, days, amount, note)
SELECT id, 'PARTNER_COST_COMP', 1375000, 1, 1, 1375000, '완료 건 파트너 정산비' FROM project WHERE project_code = 'P26100002';

INSERT INTO direct_cost_item (project_id, category_code, unit_price, quantity, days, amount, note)
SELECT id, 'PARTNER_COST_OUT', 469000, 1, 1, 469000, '추가 파트너 정산비' FROM project WHERE project_code = 'P26100002';

INSERT INTO direct_cost_item (project_id, category_code, unit_price, quantity, days, amount, note)
SELECT id, 'BOOKS', 1530000, 1, 1, 1530000, '산출물 인쇄비' FROM project WHERE project_code = 'P26100002';

INSERT INTO direct_cost_item (project_id, category_code, unit_price, quantity, days, amount, note)
SELECT id, 'PARTNER_COST_OUT', 911000, 1, 1, 911000, '추가 파트너 정산비' FROM project WHERE project_code = 'P26110001';

INSERT INTO direct_cost_item (project_id, category_code, unit_price, quantity, days, amount, note)
SELECT id, 'OUTSOURCE_1', 1143000, 1, 1, 1143000, '개발 외주' FROM project WHERE project_code = 'P26110001';

INSERT INTO direct_cost_item (project_id, category_code, unit_price, quantity, days, amount, note)
SELECT id, 'TRANS', 4712000, 1, 1, 4712000, '출장 교통/숙박비' FROM project WHERE project_code = 'P26110002';

INSERT INTO direct_cost_item (project_id, category_code, unit_price, quantity, days, amount, note)
SELECT id, 'INSURANCE', 344000, 1, 1, 344000, '출장/행사 보험료' FROM project WHERE project_code = 'P26110002';

INSERT INTO direct_cost_item (project_id, category_code, unit_price, quantity, days, amount, note)
SELECT id, 'OUTSOURCE_1', 1394000, 1, 1, 1394000, '개발 외주' FROM project WHERE project_code = 'P26110002';

INSERT INTO direct_cost_item (project_id, category_code, unit_price, quantity, days, amount, note)
SELECT id, 'INSURANCE', 5357000, 1, 1, 5357000, '출장/행사 보험료' FROM project WHERE project_code = 'P26120001';

INSERT INTO direct_cost_item (project_id, category_code, unit_price, quantity, days, amount, note)
SELECT id, 'OUTSOURCE_1', 1687000, 1, 1, 1687000, '개발 외주' FROM project WHERE project_code = 'P26120001';

INSERT INTO direct_cost_item (project_id, category_code, unit_price, quantity, days, amount, note)
SELECT id, 'GIVES_1', 1339000, 1, 1, 1339000, '고객 사은품' FROM project WHERE project_code = 'P26120002';

INSERT INTO direct_cost_item (project_id, category_code, unit_price, quantity, days, amount, note)
SELECT id, 'PARTNER_COST_COMP', 1400000, 1, 1, 1400000, '완료 건 파트너 정산비' FROM project WHERE project_code = 'P26120002';

INSERT INTO direct_cost_item (project_id, category_code, unit_price, quantity, days, amount, note)
SELECT id, 'BOOKS', 1279000, 1, 1, 1279000, '산출물 인쇄비' FROM project WHERE project_code = 'P26120002';
