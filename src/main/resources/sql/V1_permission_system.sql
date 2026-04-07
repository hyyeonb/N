-- ============================================================
-- Phase 1: 권한 시스템 DB 변경
-- ============================================================

-- 1-1. 테이블 이름 변경 (네이밍 규칙 통일: R_*_T)
RENAME TABLE `user` TO `R_USER_T`;
RENAME TABLE `social_account` TO `R_SOCIAL_ACCOUNT_T`;
RENAME TABLE `login_history` TO `R_LOGIN_HISTORY_T`;

-- 1-2. R_USER_T 컬럼 추가
ALTER TABLE R_USER_T
  ADD COLUMN STATUS VARCHAR(20) NOT NULL DEFAULT 'ACTIVE' AFTER UPDATED_AT,
  ADD COLUMN ALL_GROUP_VIEW TINYINT(1) NOT NULL DEFAULT 1 AFTER STATUS,
  ADD COLUMN REVIEWED_AT DATETIME NULL AFTER ALL_GROUP_VIEW;

-- 1-3. 신규 테이블 생성
CREATE TABLE R_PAGE_T (
  PAGE_CODE VARCHAR(30) NOT NULL PRIMARY KEY,
  PAGE_NAME VARCHAR(100) NOT NULL,
  PAGE_GROUP VARCHAR(50) NOT NULL,
  SORT_ORDER INT NOT NULL DEFAULT 0,
  PAGE_PATH VARCHAR(100) NULL,
  CREATE_AT DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4;

CREATE TABLE R_USER_PAGE_ACCESS_T (
  USER_ID BIGINT NOT NULL,
  PAGE_CODE VARCHAR(30) NOT NULL,
  CAN_VIEW TINYINT(1) NOT NULL DEFAULT 1,
  CAN_EDIT TINYINT(1) NOT NULL DEFAULT 0,
  MODIFY_USER_ID BIGINT NULL,
  MODIFY_AT DATETIME NULL,
  PRIMARY KEY (USER_ID, PAGE_CODE),
  FOREIGN KEY (USER_ID) REFERENCES R_USER_T(USER_ID) ON DELETE CASCADE,
  FOREIGN KEY (PAGE_CODE) REFERENCES R_PAGE_T(PAGE_CODE) ON DELETE CASCADE
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4;

CREATE TABLE R_USER_GROUP_ACCESS_T (
  USER_ID BIGINT NOT NULL,
  GROUP_TYPE VARCHAR(10) NOT NULL,
  GROUP_ID BIGINT NOT NULL,
  CAN_VIEW TINYINT(1) NOT NULL DEFAULT 1,
  CAN_EDIT TINYINT(1) NOT NULL DEFAULT 0,
  MODIFY_USER_ID BIGINT NULL,
  MODIFY_AT DATETIME NULL,
  PRIMARY KEY (USER_ID, GROUP_TYPE, GROUP_ID),
  FOREIGN KEY (USER_ID) REFERENCES R_USER_T(USER_ID) ON DELETE CASCADE
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4;

-- 1-4. 시드 데이터
INSERT INTO R_PAGE_T (PAGE_CODE, PAGE_NAME, PAGE_GROUP, SORT_ORDER, PAGE_PATH) VALUES
('dashboard','통합 대시보드','dashboard',1,'/dashboard'),
('topology','토폴로지','dashboard',2,'/topology'),
('user_topology','사용자 토폴로지','dashboard',3,'/user-topology'),
('watch_realtime','실시간 성능감시','perf',1,'/watch/realtime'),
('perf_stats','성능 통계','perf',2,'/perf/stats'),
('fault_realtime','실시간 장애감시','fault',1,'/fault/realtime'),
('fault_history','장애이력','fault',2,'/fault/history'),
('fault_stats','장애통계','fault',3,'/fault/stats'),
('group_mgmt','그룹 관리','mgmt',1,'/mgmt/groups'),
('asset_mgmt','자산 관리','mgmt',2,'/mgmt/assets'),
('asset_config','자산 Config 관리','mgmt',3,'/mgmt/asset-config'),
('new_asset_mgmt','신규 자산 관리','mgmt',4,'/mgmt/new-assets'),
('model_mgmt','모델 관리','mgmt',5,'/mgmt/models'),
('ssh_sessions','SSH 접속 이력','mgmt',6,'/mgmt/ssh-sessions'),
('traceroute','Traceroute','tools',1,'/tools/traceroute'),
('board_files','자료실','board',1,'/board/files'),
('board_notices','공지사항','board',2,'/board/notices'),
('system_admin','시스템 관리','system',1,'/settings/admin');

-- 기존 사용자에게 전체 페이지 VIEW=1, EDIT=0 부여
INSERT INTO R_USER_PAGE_ACCESS_T (USER_ID, PAGE_CODE, CAN_VIEW, CAN_EDIT)
SELECT u.USER_ID, p.PAGE_CODE, 1, 0 FROM R_USER_T u CROSS JOIN R_PAGE_T p;

-- 최초 Admin (USER_ID=18, dev3-hyunbin) 전체 EDIT 권한
UPDATE R_USER_PAGE_ACCESS_T SET CAN_EDIT=1, MODIFY_AT=NOW() WHERE USER_ID=18;
UPDATE R_USER_T SET REVIEWED_AT=NOW() WHERE USER_ID=18;
