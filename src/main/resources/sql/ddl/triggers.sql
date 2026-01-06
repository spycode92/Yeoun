----------------------------------------------------------------
-- 1. 순번 관리용 임시 테이블 (Global Temporary Table)
-- 2. ID/순번 생성 로직을 위한 트리거
-- 3. 테이블 간 연동 및 데이터 초기화 트리거
-- 4. 함수 (Function), 프로시저 (Procedure),뷰 (View) 등
-- 데이터베이스의 자동화된 비즈니스 로직을 정의하는 모든 PL/SQL 및 의존성 객체를 담는 파일
----------------------------------------------------------------



-- 확인 및 검증용 쿼리
-- 내가 만든 모든  프로시저와 트리거
SELECT 
    OBJECT_NAME AS "이름", 
    OBJECT_TYPE AS "유형", 
    TIMESTAMP   AS "생성시간", 
    STATUS      AS "상태"
FROM USER_OBJECTS
WHERE OBJECT_TYPE IN ('PROCEDURE', 'TRIGGER')
ORDER BY OBJECT_TYPE, OBJECT_NAME;

  
-- 특정 프로시저 쿼리
SELECT text
FROM user_source
WHERE name = 'SP_SYNC_BOM_STATUS' -- OBJECT_NAME
ORDER BY line;

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
    v_count NUMBER;
    v_hdr_name VARCHAR2(100) := :NEW.BOM_ID || '표준 헤더';
    v_hdr_type VARCHAR2(10) := 'STD';
    v_use_yn CHAR(1) := 'Y';
BEGIN
    -- 1. 이미 해당 BOM_ID가 헤더 테이블에 있는지 확인
    SELECT COUNT(*)
      INTO v_count
      FROM BOM_HDR
     WHERE BOM_ID = :NEW.BOM_ID;

    -- 2. 존재하지 않을 때만 INSERT 실행
    IF v_count = 0 THEN
        INSERT INTO BOM_HDR (
            BOM_ID, 
            BOM_HDR_NAME, 
            BOM_HDR_TYPE, 
            USE_YN, 
            BOM_HDR_DATE 
        )
        VALUES (
            :NEW.BOM_ID,
            v_hdr_name,
            v_hdr_type,
            v_use_yn,
            SYSDATE
        );
    END IF;
END;
/

-- mat use_yn 프로시저
-- 자재(N) -> BOM_MST(N) -> BOM_HDR(N) -> BOM_HDR_ID 그룹(N)
-- 자재(모두 Y) -> BOM_MST(Y) ->BOM_HDR(Y) -> BOM_HDR_ID 그룹(Y)

CREATE OR REPLACE PROCEDURE SP_SYNC_BOM_STATUS (
    p_bom_id IN VARCHAR2, 
    p_use_yn IN VARCHAR2
)
IS
    V_BOM_HDR_ID    VARCHAR2(50);
    V_HDR_N_COUNT   NUMBER;
    V_OTHER_N_COUNT NUMBER;
    PRAGMA AUTONOMOUS_TRANSACTION; 
BEGIN
    -- [1단계] N으로 바뀔 때는 즉시 처리
    IF p_use_yn = 'N' THEN
        UPDATE BOM_HDR
        SET USE_YN = 'N',
            UPDATED_DATE = SYSDATE,
            UPDATED_ID = 'SYS_SYN'
        WHERE BOM_ID = p_bom_id AND USE_YN != 'N'
        RETURNING BOM_HDR_ID INTO V_BOM_HDR_ID;

    -- [2단계] Y로 복구될 때
    ELSIF p_use_yn = 'Y' THEN
        -- [수정 핵심] 
        -- 현재 트리거를 일으킨 데이터(세션 내 수정분)는 무시하고, 
        -- 이미 DB에 'N'으로 박혀있는 '다른' 자재들이 있는지 확인합니다.
        SELECT COUNT(*) 
        INTO V_OTHER_N_COUNT 
        FROM BOM_MST
        WHERE BOM_ID = p_bom_id 
          AND USE_YN = 'N'
          AND ROWNUM <= 1;

        -- 만약 V_OTHER_N_COUNT가 1이라면, 그 1개가 '나 자신'일 가능성이 큽니다.
        -- 자율 트랜잭션은 나의 'Y' 변경을 못 보고 여전히 'N'으로 보기 때문입니다.
        -- 따라서 'N'인 개수가 1개 이하(즉, 나를 제외하면 0개)라면 'Y'로 업데이트합니다.
        IF V_OTHER_N_COUNT <= 1 THEN 
             UPDATE BOM_HDR
             SET USE_YN = 'Y',
                 UPDATED_DATE = SYSDATE,
                 UPDATED_ID = 'SYS_SYN'
             WHERE BOM_ID = p_bom_id AND USE_YN != 'Y'
             RETURNING BOM_HDR_ID INTO V_BOM_HDR_ID;
        END IF;
    END IF;

    -- [3단계] 제품 그룹 전파
    -- V_BOM_HDR_ID가 NULL이 아닐 때만 실행 (상태 변화가 있을 때만)
    IF V_BOM_HDR_ID IS NOT NULL THEN
        -- 여기도 마찬가지로 자율 트랜잭션의 한계가 있을 수 있으므로 
        -- 결과가 즉시 반영되지 않으면 'N'의 개수를 세는 로직을 주의해야 합니다.
        SELECT COUNT(*) INTO V_HDR_N_COUNT 
        FROM BOM_HDR 
        WHERE BOM_HDR_ID = V_BOM_HDR_ID AND USE_YN = 'N';

        IF V_HDR_N_COUNT > 0 THEN
            UPDATE BOM_HDR SET USE_YN = 'N', UPDATED_DATE = SYSDATE, UPDATED_ID = 'SYS_SYN'
            WHERE BOM_HDR_ID = V_BOM_HDR_ID AND USE_YN != 'N';
        ELSE
            UPDATE BOM_HDR SET USE_YN = 'Y', UPDATED_DATE = SYSDATE, UPDATED_ID = 'SYS_SYN'
            WHERE BOM_HDR_ID = V_BOM_HDR_ID AND USE_YN != 'Y';
        END IF;
    END IF;

    COMMIT; 
EXCEPTION
    WHEN OTHERS THEN
        ROLLBACK;
        RAISE;
END;
/

-- 1.BOM_MST에서 USE_YN 을 변경 했을때 사용하는 트리거 
-- 2.BOM_MST -> MATERIAL_MST (영향없음)
CREATE OR REPLACE TRIGGER TRG_CALL_SYNC_MST
AFTER UPDATE OF USE_YN ON BOM_MST
FOR EACH ROW
BEGIN
    -- 반드시 파라미터 2개(BOM_ID, USE_YN)만 넘겨야 합니다.
    SP_SYNC_BOM_STATUS(:NEW.BOM_ID, :NEW.USE_YN);
END;
/

-- 1.MATERIAL_MST에서 USE_YN을 변경했을때 사용하는 트리거 
CREATE OR REPLACE TRIGGER TRG_SYNC_MAT_TO_BOM_MST
AFTER UPDATE OF USE_YN ON MATERIAL_MST -- 자재 마스터의 사용여부가 바뀔 때
FOR EACH ROW
BEGIN
    -- 자재 마스터의 MAT_ID와 동일한 모든 BOM 상세 내역을 업데이트
    UPDATE BOM_MST
    SET USE_YN = :NEW.USE_YN,
        UPDATED_DATE = SYSDATE,
        UPDATED_ID = 'SYS_MAT'
    WHERE MAT_ID = :NEW.MAT_ID;
END;
/
