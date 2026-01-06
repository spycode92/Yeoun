package com.yeoun.emp.dto;

import java.time.LocalDate;
import java.util.List;
import java.util.stream.Collector;

import org.hibernate.validator.constraints.Length;
import org.modelmapper.ModelMapper;
import org.springframework.format.annotation.DateTimeFormat;
import org.springframework.web.multipart.MultipartFile;

import com.yeoun.emp.entity.Emp;

import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.PastOrPresent;
import jakarta.validation.constraints.Pattern;
import lombok.Getter;
import lombok.Setter;
import lombok.ToString;

@Getter
@Setter
@ToString
public class EmpDTO {
	
	// === 검증 그룹 인터페이스 ===
    public interface Regist {}  // 등록용
    public interface Edit {}  // 수정용
	
	// ========================
	// 기본정보
	// ========================
	
	private String empId;
	
	// 이름
	@NotBlank(message = "이름은 필수 입력값입니다.", groups = {Regist.class, Edit.class})
	@Length(min = 2, max = 20, message = "이름은 2 ~ 20자리 입니다.", groups = {Regist.class, Edit.class})
	private String empName;  			
	
	// 성별
	@NotBlank(message = "성별을 선택해주세요.", groups = Regist.class)
    @Pattern(regexp = "M|F", message = "성별은 M 또는 F만 가능합니다.", groups = Regist.class)
	private String gender;  
    
    // 주민번호 
	@NotBlank(message = "주민등록번호는 필수 입력입니다.", groups = Regist.class)
    @Pattern(
      regexp = "^(\\d{2}(0[1-9]|1[0-2])(0[1-9]|[12]\\d|3[01]))-[1-4]\\d{6}$",
      message = "주민등록번호 형식은 000000-0000000 입니다.", groups = Regist.class
    )
    private String rrn;
	
	private String rrnMasked;
    
	// 입사일: 필수 + 과거/오늘
    @NotNull(message = "입사일은 필수입니다.", groups = Regist.class)
    @PastOrPresent(message = "입사일은 오늘 또는 과거 날짜만 가능합니다.", groups = Regist.class)
    @DateTimeFormat(pattern = "yyyy-MM-dd")
	private LocalDate hireDate;  
    
    // 사진파일ID (사진 파일 FK)	
    private Long photoFileId;  
    
	// ========================
	// 연락/계정
	// ========================

    // 이메일
    @NotBlank(message = "이메일은 필수 입력값입니다.", groups = {Regist.class, Edit.class})
    @Email(message = "이메일 형식에 맞게 입력해 주세요.", groups = {Regist.class, Edit.class})
    private String email;  		
    
    // 연락처
    @NotBlank(message = "연락처는 필수입니다.", groups = {Regist.class, Edit.class})
    @Pattern(regexp = "^010-\\d{4}-\\d{4}$", message = "연락처 형식은 010-0000-0000 입니다.", groups = {Regist.class, Edit.class})
    private String mobile; 	
	
    // 주소 (우편번호 / 기본주소 + 상세주소)
	@NotBlank(message = "우편번호는 필수 입력값입니다.", groups = {Regist.class, Edit.class})
	private String postCode; 
	
	@NotBlank(message = "기본 주소는 필수 입력값입니다.", groups = {Regist.class, Edit.class})
	private String address1;  	
	private String address2;   	
	
	// ========================
	// 조직/직무
	// ========================
	
	// 부서 
	@NotBlank(message = "부서를 선택해주세요.", groups = Regist.class)
    private String deptId;
	
	// 직급
	@NotBlank(message = "직급을 선택해주세요.", groups = Regist.class)
    private String posCode;
	
    // 재직 상태		
	private String status; 

	// ========================
	// 급여통장 정보
	// ========================

	// 은행 코드 (예: BANK_004)
	@NotBlank(message = "은행을 선택해주세요.", groups = {Regist.class, Edit.class})
	private String bankCode;  

	// 계좌번호
	@NotBlank(message = "계좌번호를 입력해주세요.", groups = {Regist.class, Edit.class})
	@Pattern(regexp = "^[0-9\\-]{6,20}$", message = "계좌번호는 숫자와 '-'만 사용하여 6~20자 내로 입력해주세요.", 
			 groups = {Regist.class, Edit.class})
	private String accountNo;  

	// 예금주명
	private String holder; 

	// 통장 사본 파일 ID 
	private Long fileId; 
	
	// ========================
	// 업로드용 파일 
	// ========================
	// 사원 사진 
	private MultipartFile photoFile;
	
	// 통장 사본
	private MultipartFile bankbookFile;
	
	// -----------------------------------------------------------------------
	private static ModelMapper modelMapper = new ModelMapper();
	
	
	// ModelMapper 객체의 map() 메서드를 활용하여 객체 변환 수행
	// 1) EmpDTO -> Emp(엔티티) 타입으로 변환하는 toEntity() 메서드 정의
	public Emp toEntity() {
		return modelMapper.map(this, Emp.class);
	}
	
	// 2) Entity -> DTO 로 변환하는 fromEntity() 메서드 정의
	public static EmpDTO fromEntity(Emp emp) {
		return modelMapper.map(emp, EmpDTO.class);
	}

	
}
