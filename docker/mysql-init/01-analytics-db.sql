-- 학습 분석(EP-08) 전용 데이터베이스 생성 (최초 컨테이너 기동 시 1회 실행)
-- 운영 DB(student_management)와 동일 MySQL 인스턴스의 별도 데이터베이스.
-- 스타 스키마 테이블은 백엔드의 분석 datasource(ddl-auto)가 자동 생성한다.
CREATE DATABASE IF NOT EXISTS student_analytics
  CHARACTER SET utf8mb4 COLLATE utf8mb4_unicode_ci;

-- appuser 가 분석 DB에 접근할 수 있도록 권한 부여
GRANT ALL PRIVILEGES ON student_analytics.* TO 'appuser'@'%';
FLUSH PRIVILEGES;
