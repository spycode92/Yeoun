package com.yeoun.pay.entity;

import jakarta.persistence.Index; 
import jakarta.persistence.*;
import jakarta.persistence.ForeignKey;
import jakarta.persistence.Table;
import jakarta.validation.constraints.Digits;
import lombok.*;
import org.hibernate.annotations.*;

import com.yeoun.pay.enums.ActiveStatus;
import com.yeoun.pay.enums.CalcMethod;
import com.yeoun.pay.enums.RuleType;
import com.yeoun.pay.enums.TargetType;

import java.math.BigDecimal;
import java.time.LocalDate;

@Entity
@Table(
 name = "PAY_CALC_RULE",
 indexes = {
     @Index(name = "IX_PAY_CALC_RULE_ITEM", columnList = "ITEM_CODE"),
     @Index(name = "IX_PAY_CALC_RULE_STATUS", columnList = "STATUS")
 }

)
@Getter @Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
@DynamicInsert // DB DEFAULT 사용
public class PayCalcRule {

 @Id
 @GeneratedValue(strategy = GenerationType.IDENTITY) // Oracle 12c+ IDENTITY
 @Column(name = "RULE_ID", precision = 18, nullable = false)
 @Comment("규칙ID (PK)")
 private Long ruleId;
  
 @ManyToOne(fetch = FetchType.LAZY, optional = false)
 @JoinColumn(name = "ITEM_CODE",
         nullable = false,
         foreignKey = @ForeignKey(name = "FK_PAY_CALC_RULE__PAY_ITEM_MST"))
 @Comment("적용될 급여 항목 코드 (FK: PAY_ITEM_MST.ITEM_CODE)")
 private PayItemMst item; // ITEM_CODE

 @Enumerated(EnumType.STRING)
 @Column(name = "RULE_TYPE", length = 10, nullable = false)
 @Comment("계산 타입: FIXED / RATE / FORMULA / EXTERNAL")
 private RuleType ruleType;

 @Column(name = "START_DATE")
 @Comment("적용 시작일")
 private LocalDate startDate;

 @Column(name = "END_DATE")
 @Comment("적용 종료일 (NULL=무기한)")
 private LocalDate endDate;

 @Enumerated(EnumType.STRING)
 @Column(name = "TARGET_TYPE", length = 10)
 @Comment("대상 구분: EMP/DEPT/GRADE")
 private TargetType targetType;

 @Column(name = "TARGET_CODE", length = 30)
 @Comment("대상 식별코드 (예: M2-1, D100, 사번 등)")
 private String targetCode;

 @Digits(integer = 15, fraction = 6)
 @Column(name = "VALUE_NUM", precision = 15, scale = 6)
 @Comment("정의/비율/금액 값(수치). RULE_TYPE에 따라 의미 다름")
 private BigDecimal valueNum;

 @Lob
 @Column(name = "CALC_FORMULA")
 @Comment("계산 공식 (예: UNUSED_DAYS*DAILY_WAGE)")
 private String calcFormula;

 @Column(name = "PRIORITY", precision = 4)
 @ColumnDefault("100")
 @Comment("낮을수록 우선 (기본=100)")
 private Integer priority;

 @Enumerated(EnumType.STRING)
 @Column(name = "STATUS", length = 12, nullable = false)
 @ColumnDefault("'ACTIVE'")
 @Comment("사용 상태: ACTIVE/INACTIVE")
 private ActiveStatus status;
 
 @Column(name = "REMARK")
 private String remark;


//급여 계산용
public BigDecimal getAmount() {
  return this.valueNum;
}

public BigDecimal getRate() {
  return this.valueNum;
}

public String getExpr() {
  return this.calcFormula;
}


@Transient
private String itemCode;   // 화면에서 넘어오는 ITEM_CODE 임시 저장

public String getItemCode() { return itemCode; }
public void setItemCode(String itemCode) { this.itemCode = itemCode; }

 
}
