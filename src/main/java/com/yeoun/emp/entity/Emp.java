package com.yeoun.emp.entity;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;

import org.springframework.data.annotation.CreatedDate;
import org.springframework.data.jpa.domain.support.AuditingEntityListener;

import com.yeoun.auth.entity.Role;
import com.yeoun.common.util.FileUtil.FileUploadHelpper;
import com.yeoun.leave.entity.AnnualLeave;

import jakarta.persistence.CascadeType;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.EntityListeners;
import jakarta.persistence.FetchType;
import jakarta.persistence.Id;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.ManyToOne;
import jakarta.persistence.OneToMany;
import jakarta.persistence.OneToOne;
import jakarta.persistence.Table;
import lombok.Getter;
import lombok.Setter;

@Entity(name = "Emp")
@Table(name = "EMP")
@Getter
@Setter
@EntityListeners(AuditingEntityListener.class)
public class Emp implements FileUploadHelpper { 

	// 사원번호 (로그인 ID)
	@Id
	@Column(name = "EMP_ID", nullable = false, length = 7)
	private String empId;		
	
	// 비밀번호(BCrypt)
	@Column(name = "EMP_PWD", nullable = false, length = 200)
	private String empPwd;		
	
	// 비밀번호 변경 필요 여부
	@Column(name = "PWD_CHANGE_REQ", length = 1)
	private String pwdChangeReq = "N";
	
	// 이름
	@Column(name = "EMP_NAME", nullable = false, length = 50)
	private String empName;		
	
	// 성별 (CHAR(1))
	@Column(name = "GENDER", length = 1)
	private String gender;		
	
	// 주민등록번호 (민감정보, 보관시 암호화/마스킹 고려)
	@Column(name = "RRN", length = 14)
	private String rrn;			
	
	// 연락처
	@Column(name = "MOBILE", length = 20)
	private String mobile;		
	
	// 이메일
	@Column(name = "EMAIL", length = 100)
	private String email;		
	
	// 우편번호
	@Column(name = "POST_CODE", length = 10)
	private String postCode; 	
	
	// 기본주소
	@Column(name = "ADDRESS1", length = 200)
	private String address1; 	
	
	// 상세주소
	@Column(name = "ADDRESS2", length = 200)
	private String address2; 	
	
	// 입사일 (DATE -> LocalDate)
	@Column(name = "HIRE_DATE")
	private LocalDate hireDate;	
	
	// 퇴사일
	@Column(name = "RETIRE_DATE")
	private LocalDate retireDate;
	
    // 부서ID (FK: DEPT.DEPT_ID)
    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "DEPT_ID", nullable = false)
    private Dept dept;
  
    // 직급코드 (FK: POSITION.POS_CODE)
    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "POS_CODE", nullable = false, referencedColumnName = "POS_CODE")
    private Position position;
	
	// 재직 상태
	@Column(name = "STATUS", nullable = false, length = 10)
	private String status;		
	
	// 사진파일ID (사진 파일 FK)
	@Column(name = "PHOTO_FILE_ID")
	private Long photoFileId;	
	
	// 등록 일시
	@CreatedDate
	@Column(name = "CREATED_DATE", updatable = false)
	private LocalDateTime createdDate;	// 등록 일시
	
	// 최근 로그인 시각 (로그인 성공 시 서비스에서 수동 업데이트)
	@Column(name = "LAST_LOGIN")
	private LocalDateTime lastLogin;
	
	// ---------------------------------------------------------------
	// 사원은 역할(ROLE)과 다대다(N:M) 관계
	// => 따라서, 완충 작용을 담당할 EmpRole 엔티티를 정의함
	// => Emp 와 EmpRole 은 1:N 관계
	// => @OneToMany 어노테이션으로 1:N 관계에서 1 에 해당하는 엔티티 입장에서의 관계 지정
	// 1) mappedBy = "member" : 현재 엔티티가 연관관계의 주인이 아니므로, 상대방의 필드 지정하여 해당 필드 기준으로 매핑 수행
	// 2) cascade = CascadeType.ALL : 부모 엔티티가 저장/삭제 될 경우 자식 엔티티도 저장/삭제
	// 3) orphanRemoval = true : 부모 엔티티와 연관관계가 끊어진 자식엔티티(고아객체) 자동으로 삭제
	@OneToMany(mappedBy = "emp", cascade = CascadeType.ALL, orphanRemoval = true)
	private List<EmpRole> empRoles = new ArrayList<>(); // 1명의 사원이 여러개의 권한을 가질 수 있으므로 EmpRole 의 List 를 사용
	
	// 사용자 권한을 추가하는 addRole() 메서드 정의
	public void addRole(Role role) {
		// EmpRole 인스턴스 생성 시 생성자 파라미터로 현재 엔티티(Emp)와 역할 엔티티(Role) 전달
		EmpRole empRole = new EmpRole(this, role);
		// 사용자 권한 목록 객체(List<EmpRole>)에 1개의 권한이 저장된 EmpRole 엔티티 추가
		empRoles.add(empRole);
	}
	
	// ---------------------------------------------------------------
	@Override
	public String getTargetTable() {
		return "EMP";
	}

	@Override
	public Long getTargetTableId() {
		return Long.valueOf(empId);
	}
	
	
	
	
}
