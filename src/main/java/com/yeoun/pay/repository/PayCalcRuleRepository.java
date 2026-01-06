package com.yeoun.pay.repository;

import com.yeoun.pay.entity.PayCalcRule;
import com.yeoun.pay.enums.TargetType;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.time.LocalDate;
import java.util.List;

public interface PayCalcRuleRepository extends JpaRepository<PayCalcRule, Long> {

    /* =========================================================
       🔹 리스트 조회
    ========================================================= */

    /** 항목별 목록 (우선순위 ASC) */
    List<PayCalcRule> findByItem_ItemCodeOrderByPriorityAsc(String itemCode);

    /** 항목 + 대상 조건 목록 (우선순위 ASC) */
    List<PayCalcRule> findByItem_ItemCodeAndTargetTypeAndTargetCodeOrderByPriorityAsc(
            String itemCode,
            TargetType targetType,
            String targetCode
    );

    /** 전체 목록 정렬 */
    List<PayCalcRule> findAllByOrderByPriorityAsc();


    /* =========================================================
       🔹 기간 중복 체크
    ========================================================= */

    @Query("""
        SELECT r
          FROM PayCalcRule r
         WHERE r.item.itemCode = :itemCode
           AND r.targetType = :targetType
           AND (:targetCode = '' OR r.targetCode = :targetCode)
        """)
    List<PayCalcRule> findForOverlapCheck(
            @Param("itemCode") String itemCode,
            @Param("targetType") TargetType targetType,
            @Param("targetCode") String targetCode
    );



    /* =========================================================
       🔹 활성 규칙 조회 (Native)
    ========================================================= */

    @Query(value = """
        SELECT *
          FROM PAY_CALC_RULE R
         WHERE R.ITEM_CODE = :itemCode
           AND :asOf BETWEEN R.START_DATE AND NVL(R.END_DATE, :asOf)
         ORDER BY R.PRIORITY NULLS LAST, R.RULE_ID
        """, nativeQuery = true)
    List<PayCalcRule> findActiveByItemAndDate(
            @Param("itemCode") String itemCode,
            @Param("asOf") LocalDate asOf
    );


    @Query(value = """
        SELECT *
          FROM PAY_CALC_RULE R
         WHERE R.STATUS = 'ACTIVE'
           AND :asOf BETWEEN R.START_DATE AND NVL(R.END_DATE, :asOf)
         ORDER BY R.PRIORITY NULLS LAST, R.RULE_ID
        """, nativeQuery = true)
    List<PayCalcRule> findActiveRules(@Param("asOf") LocalDate asOf);



    /* =========================================================
       🔹 우선순위 중복 체크 (등록/수정)
    ========================================================= */

    /** 등록용: itemCode + priority 존재 여부 */
    boolean existsByItem_ItemCodeAndPriority(String itemCode, Integer priority);


    /** 수정용: 자기 자신 제외하고 동일한 priority 존재 여부 */
    @Query("""
        SELECT COUNT(r) > 0
          FROM PayCalcRule r
         WHERE r.item.itemCode = :itemCode
           AND r.priority = :priority
           AND r.ruleId <> :ruleId
        """)
    boolean existsByItemCodeAndPriorityExceptSelf(
            @Param("itemCode") String itemCode,
            @Param("priority") Integer priority,
            @Param("ruleId") Long ruleId
    );

}
