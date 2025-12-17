----------------------------------------------------------------
-- 1. 순번 관리용 임시 테이블 (Global Temporary Table)
-- 2. ID/순번 생성 로직을 위한 트리거
-- 3. 테이블 간 연동 및 데이터 초기화 트리거
-- 4. 함수 (Function), 프로시저 (Procedure),뷰 (View) 등
-- 데이터베이스의 자동화된 비즈니스 로직을 정의하는 모든 PL/SQL 및 의존성 객체를 담는 파일
----------------------------------------------------------------




-- BOM_HDR 테이블의 BOM_HDR_ID 자동 생성 및 관리
-- 캐싱의 의미
-- "캐싱(Caching)"은 컴퓨터 과학 및 데이터베이스 분야에서 데이터를 임시적으로 저장하여
-- 더 빠르게 접근할 수 있도록 하는 기술을 의미합니다.

-- -------------------------------------------------------------
-- 1. 의존성 객체: TEMP_BOM_HDR_SEQ (Global Temporary Table)
--    - ORA-04091 회피를 위한 순번 캐싱 테이블
-- -------------------------------------------------------------
-- **(중요)** 기존에 존재할 경우 오류를 피하기 위해 먼저 DROP 합니다.
DROP TABLE TEMP_BOM_HDR_SEQ; 
-- BOM_HDR 테이블의 순번 데이터를 임시적으로 안전하게 저장하여, 트리거가 실행되는 동안 BOM_HDR을 직접 조회하지 않도록 합니다.
CREATE GLOBAL TEMPORARY TABLE TEMP_BOM_HDR_SEQ ( --
    BOM_ID VARCHAR2(50),
    NEXT_SEQ NUMBER
) ON COMMIT DELETE ROWS;
/

-- -------------------------------------------------------------
-- 2. 핵심 기능 트리거 A: TRG_BOM_HDR_SEQ_CACHE (AFTER STATEMENT)
--    - INSERT 문 실행 완료 후, BOM_HDR의 현재 최대 순번을 TEMP_BOM_HDR_SEQ에 캐싱합니다.
-- -------------------------------------------------------------
-- BOM_HDR에 새로운 데이터가 모두 삽입된 후, 딱 한 번 실행됩니다
CREATE OR REPLACE TRIGGER TRG_BOM_HDR_SEQ_CACHE
AFTER INSERT ON BOM_HDR
BEGIN
    DELETE FROM TEMP_BOM_HDR_SEQ;
    
    INSERT INTO TEMP_BOM_HDR_SEQ (BOM_ID, NEXT_SEQ)
    SELECT 
        BOM_ID, 
        NVL(MAX(TO_NUMBER(SUBSTR(BOM_HDR_ID, INSTR(BOM_HDR_ID, '-H') + 2))), 0) + 1
    FROM BOM_HDR
    GROUP BY BOM_ID;
END;
/

-- -------------------------------------------------------------
-- 3. 핵심 기능 트리거 B: TRG_BOM_HDR_ID_FINAL (BEFORE EACH ROW)
--    - BOM_HDR 행 삽입 직전에 TEMP 테이블에서 순번을 가져와 ID를 생성합니다.
-- -------------------------------------------------------------
-- BOM_HDR에 데이터가 삽입되는 각 행마다 실행됩니다.
CREATE OR REPLACE TRIGGER TRG_BOM_HDR_ID_FINAL
BEFORE INSERT ON BOM_HDR
FOR EACH ROW
DECLARE
    v_next_seq NUMBER := 1;
BEGIN
    -- [1] 순번 조회 및 업데이트 (TEMP 테이블)
    SELECT NEXT_SEQ INTO v_next_seq
    FROM TEMP_BOM_HDR_SEQ
    WHERE BOM_ID = :NEW.BOM_ID;

    UPDATE TEMP_BOM_HDR_SEQ
    SET NEXT_SEQ = NEXT_SEQ + 1
    WHERE BOM_ID = :NEW.BOM_ID;

    -- [2] BOM_HDR_ID 값 생성 및 할당
    :NEW.BOM_ID := UPPER(:NEW.BOM_ID); 
    :NEW.BOM_HDR_ID := :NEW.BOM_ID || '-H' || LPAD(v_next_seq, 3, '0');
    
EXCEPTION
    WHEN NO_DATA_FOUND THEN
        -- 초기값 설정 (순번 1)
        :NEW.BOM_ID := UPPER(:NEW.BOM_ID); 
        :NEW.BOM_HDR_ID := :NEW.BOM_ID || '-H' || LPAD(1, 3, '0');
        
        -- 임시 테이블에 순번 2를 INSERT (다음 행을 대비)
        INSERT INTO TEMP_BOM_HDR_SEQ (BOM_ID, NEXT_SEQ) 
        VALUES (:NEW.BOM_ID, 2);
END;
/

-- -------------------------------------------------------------
-- 4. 연동 트리거: TRG_BOM_MST_HDR_INSERT (AFTER INSERT on BOM_MST)
--    - BOM_MST에 INSERT 발생 시, BOM_HDR에 새 레코드를 생성하여 
--      위의 2단계 트리거(TRG_BOM_HDR_ID_FINAL)가 자동 호출되도록 유발합니다.
-- -------------------------------------------------------------
-- BOM_MST 가 insert 될 때마다 실행 그리고 BOM_HDR 에도 기본값으로 insert 수행
CREATE OR REPLACE TRIGGER TRG_BOM_MST_HDR_INSERT
AFTER INSERT ON BOM_MST
FOR EACH ROW
DECLARE
    -- BOM_HDR 초기값 정의
    v_hdr_name VARCHAR2(100) := :NEW.BOM_ID || '표준 헤더';
    v_hdr_type VARCHAR2(10) := 'STD';
    v_use_yn CHAR(1) := 'Y';
BEGIN
    INSERT INTO BOM_HDR (
        BOM_ID, 
        BOM_HDR_NAME, 
        BOM_HDR_TYPE, 
        USE_YN, 
        BOM_HDR_DATE 
        -- BOM_HDR_ID는 자동 생성되므로 생략
    )
    VALUES (
        :NEW.BOM_ID,
        v_hdr_name,
        v_hdr_type,
        v_use_yn,
        SYSDATE
    );
END;
/