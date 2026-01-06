package com.yeoun.emp.entity;

import com.yeoun.auth.entity.Role;

import jakarta.persistence.Entity;
import jakarta.persistence.FetchType;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.ManyToOne;
import jakarta.persistence.SequenceGenerator;
import jakarta.persistence.Table;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

// 사원 권한을 관리하는 엔티티
// => Emp 엔티티와 Role 엔티티는 다대다(N:M) 관계이므로 중간 완충 역할을 수행
@Entity
@Table(name = "EMP_ROLE")
@SequenceGenerator(
		name = "EMP_ROLE_SEQ_GENERATOR",
		sequenceName = "EMP_ROLE_SEQ", 
		initialValue = 1,
		allocationSize = 1
)
@Getter
@Setter
@NoArgsConstructor
public class EmpRole {
	
	@Id @GeneratedValue(strategy = GenerationType.SEQUENCE, generator = "EMP_ROLE_SEQ_GENERATOR")
	private Long id;
	
	// 사원(EMP) 테이블과 연관관계 설정
	@ManyToOne(fetch = FetchType.LAZY)
	@JoinColumn(name = "EMP_ID", nullable = false) // EMP 테이블에 연결할 컬럼을 EMP_ROLE 테이블의 EMP_ID 컬럼으로 지정 (FK 설정) 
	private Emp emp;
	
	// 역할(ROLE) 테이블과 연관관계 설정
	@ManyToOne(fetch = FetchType.LAZY)
	@JoinColumn(name = "ROLE_CODE", nullable = false) // ROLE 테이블에 연결할 컬럼을 EMP_ROLE 테이블의 ROLE_CODE 컬럼으로 지정 (FK 설정) 
	private Role role;

	// id를 제외한 Emp, Role 엔티티를 전달받아 객체를 초기화하는 생성자 정의
	public EmpRole(Emp emp, Role role) {
		this.emp = emp;
		this.role = role;
	}
	

}

