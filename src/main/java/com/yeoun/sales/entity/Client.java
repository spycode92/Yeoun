package com.yeoun.sales.entity;

import jakarta.persistence.*;
import lombok.*;
import org.hibernate.annotations.Comment;

import java.time.LocalDateTime;

@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
@Entity
@Table(name = "CLIENT")
public class Client {

    // 1) 거래처 ID (PK)
    @Id
    @Column(name = "CLIENT_ID", length = 20, nullable = false)
    @Comment("시스템에서 거래처를 구분하기 위한 고유 ID (CUS/SUP + YYYYMMDD + 4자리 Seq)")
    private String clientId;

    // 2) 거래처명
    @Column(name = "CLIENT_NAME", length = 30, nullable = false)
    @Comment("거래처(회사) 명칭")
    private String clientName;

    // 3) 거래처구분
    @Column(name = "CLIENT_TYPE", length = 20, nullable = false)
    @Comment("거래처의 구분 정보 (CUSTOMER / SUPPLIER)")
    private String clientType;

    // 4) 사업자등록번호
    @Column(name = "BUSINESS_NO", length = 20)
    @Comment("사업자 번호")
    private String businessNo;

    // 5) 대표자명
    @Column(name = "CEO_NAME", length = 50)
    @Comment("거래처 대표자명")
    private String ceoName;
    
    // 업태
    @Column(name = "BIZ_TYPE")
    @Comment("업태")
    private String bizType;   

    // 업종
    @Column(name = "BIZ_ITEM")
    @Comment("업종")
    private String bizItem;   

    // 6) 담당자명
    @Column(name = "MANAGER_NAME", length = 50)
    @Comment("우리 회사와 거래되는 담당자 이름")
    private String managerName;

    // 7) 담당자부서
    @Column(name = "MANAGER_DEPT", length = 50)
    @Comment("담당자(구매자)의 부서명")
    private String managerDept;

    // 8) 담당자연락처
    @Column(name = "MANAGER_TEL", length = 20)
    @Comment("담당자 연락처")
    private String managerTel;

    // 9) 담당자 이메일
    @Column(name = "MANAGER_EMAIL", length = 100)
    @Comment("담당자 이메일 주소")
    private String managerEmail;

    // 10) 우편번호
    @Column(name = "POST_CODE", length = 10)
    @Comment("거래처 우편번호")
    private String postCode;

    // 11) 기본주소
    @Column(name = "ADDR", length = 100)
    @Comment("거래처 기본 주소")
    private String addr;

    // 12) 상세주소
    @Column(name = "ADDR_DETAIL", length = 100)
    @Comment("거래처 상세 주소")
    private String addrDetail;

    // 13) FAX 번호
    @Column(name = "FAX_NUMBER", length = 30)
    @Comment("거래처 FAX 번호")
    private String faxNumber;

    // 14) 은행명
    @Column(name = "BANK_NAME", length = 20)
    @Comment("거래처 계좌 은행명")
    private String bankName;

    // 15) 예금주
    @Column(name = "ACCOUNT_NAME", length = 20)
    @Comment("거래처 계좌 예금주")
    private String accountName;

    // 16) 계좌번호
    @Column(name = "ACCOUNT_NUMBER", length = 30)
    @Comment("거래처 계좌 번호")
    private String accountNumber;

    // 17) 상태코드 (ACTIVE / INACTIVE)
    @Column(name = "STATUS_CODE", length = 10, nullable = false)
    @Comment("거래처 상태 코드 (ACTIVE/INACTIVE)")
    private String statusCode;

    // 18) 등록일시
    @Column(name = "CREATED_AT")
    @Comment("거래처 정보 최초 등록 일시")
    private LocalDateTime createdAt;

    // 19) 생성자 ID (EMP FK)
    @Column(name = "CREATED_BY", length = 20)
    @Comment("거래처 정보를 최초 등록한 사용자 ID")
    private String createdBy;

    // 20) 수정일시
    @Column(name = "UPDATED_AT")
    @Comment("거래처 정보 최종 수정 일시")
    private LocalDateTime updatedAt;

    // 21) 수정자 ID (EMP FK)
    @Column(name = "UPDATED_BY", length = 20)
    @Comment("마지막으로 거래처 정보를 수정한 사용자 ID")
    private String updatedBy;

    // =============================
    // 기본값 자동 세팅
    // =============================
    @PrePersist
    public void prePersist() {
        if (this.statusCode == null) this.statusCode = "ACTIVE";
        if (this.createdAt == null) this.createdAt = LocalDateTime.now();
    }

    @PreUpdate
    public void preUpdate() {
        this.updatedAt = LocalDateTime.now();
    }




}
