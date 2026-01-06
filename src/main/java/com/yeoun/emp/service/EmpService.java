package com.yeoun.emp.service;

import java.io.IOException;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.HashSet;
import java.util.List;
import java.util.Set;
import java.util.concurrent.ThreadLocalRandom;
import java.util.stream.Collectors;

import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.security.core.userdetails.UsernameNotFoundException;
import org.springframework.security.crypto.bcrypt.BCryptPasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import com.yeoun.auth.entity.Role;
import com.yeoun.auth.repository.RoleRepository;
import com.yeoun.common.dto.FileAttachDTO;
import com.yeoun.common.entity.FileAttach;
import com.yeoun.common.repository.FileAttachRepository;
import com.yeoun.common.util.FileUtil;
import com.yeoun.emp.dto.EmpDTO;
import com.yeoun.emp.dto.EmpDetailDTO;
import com.yeoun.emp.dto.EmpListDTO;
import com.yeoun.emp.entity.Dept;
import com.yeoun.emp.entity.Emp;
import com.yeoun.emp.entity.EmpBank;
import com.yeoun.emp.entity.EmpRole;
import com.yeoun.emp.entity.Position;
import com.yeoun.emp.repository.DeptRepository;
import com.yeoun.emp.repository.EmpBankRepository;
import com.yeoun.emp.repository.EmpRepository;
import com.yeoun.emp.repository.EmpRoleRepository;
import com.yeoun.emp.repository.PositionRepository;
import com.yeoun.leave.service.LeaveService;
import com.yeoun.messenger.entity.MsgStatus;
import com.yeoun.messenger.repository.MsgStatusRepository;

import jakarta.persistence.EntityNotFoundException;
import lombok.RequiredArgsConstructor;
import lombok.extern.log4j.Log4j2;

@Service
@Log4j2
@Transactional
@RequiredArgsConstructor
public class EmpService {
	
	private final EmpRepository empRepository;
	private final RoleRepository roleRepository;
	private final EmpRoleRepository empRoleRepository;
	private final DeptRepository deptRepository;
	private final PositionRepository positionRepository;
	private final EmpBankRepository empBankRepository;
	private final FileUtil fileUtil;
	private final FileAttachRepository fileAttachRepository;
	private final LeaveService leaveService;
	private final MsgStatusRepository msgStatusRepository;
	private final BCryptPasswordEncoder encoder;
	
	// ----------------------------------------------------------------------------
	// ========== 권한 관리 ========== 
	// 자동 관리 대상 권한 목록
	private static final Set<String> AUTO_ROLE_SET = Set.of(
		"ROLE_HR_ADMIN",
		"ROLE_DEPT_MANAGER",
		"ROLE_SYS_ADMIN",
		"ROLE_NOTICE_WRITER"
	);
	
	// 0-1. 부서/직급에 따라 권한 자동 부여
	private void addRoleIfNotExists(Emp emp, String roleCode) {
		
		// 현재 DB 기준으로 이 사원이 가진 권한 코드
		List<String> currentCodes = empRoleRepository.findRoleCodesByEmpId(emp.getEmpId());
		if (currentCodes.contains(roleCode)) {
	        return; // 이미 있으면 패스
	    }
		
		Role role = roleRepository.findByRoleCode(roleCode)
				.orElseThrow(() -> new IllegalStateException("역할 없음: " + roleCode));
		
		EmpRole empRole = new EmpRole();
		empRole.setEmp(emp);
		empRole.setRole(role);
		
		empRoleRepository.save(empRole);
	}
	
	// 발령/부서이동 등으로 부서/직급 바뀐 뒤, 권한 재계산할 때 쓰는 공개 메서드
	public void syncRolesByDeptAndPos(Emp emp) {
	    assignDefaultRoles(emp);   // 내부에서 자동 관리 대상 권한 정리
	}
	
	// 부서/직급 기준으로 자동권한 삭제 및 추가
	private void assignDefaultRoles(Emp emp) {
		
		String deptId = emp.getDept().getDeptId();
		String posName = emp.getPosition().getPosName();
		
		// 1) 현재 부서/직급 기준으로 필요한 자동 권한 계산
	    Set<String> requiredRoles = new HashSet<>();
		
		// 인사부
	    if ("DEP005".equals(deptId)) {
	        requiredRoles.add("ROLE_HR_ADMIN");
	    }
		
		// 부장
	    if (posName != null && posName.contains("부장")) {
	        requiredRoles.add("ROLE_DEPT_MANAGER");
	    }
		
		// 대표/이사
	    if ("대표".equals(posName)) {
	        requiredRoles.add("ROLE_SYS_ADMIN");
	        requiredRoles.add("ROLE_NOTICE_WRITER");
	    } else if (posName != null && posName.contains("이사")) {
	        // ERP / MES 이사
	        requiredRoles.add("ROLE_NOTICE_WRITER");

	        // ERP본부 및 MES본부: HR_ADMIN 추가
	        if ("DEP000".equals(deptId) || "DEP100".equals(deptId)) {
	            requiredRoles.add("ROLE_HR_ADMIN");
	        }
	    }
	    
	    // 2) DB에서 현재 권한 목록 조회
	    List<EmpRole> empRoles = empRoleRepository.findByEmp_EmpId(emp.getEmpId());
	    Set<String> currentCodes = empRoles.stream()
	            .map(er -> er.getRole().getRoleCode())
	            .collect(Collectors.toSet());

	    // 3) 자동 관리 대상 중, 이제는 필요 없는 권한 삭제
	    for (EmpRole er : empRoles) {
	        String code = er.getRole().getRoleCode();
	        if (AUTO_ROLE_SET.contains(code) && !requiredRoles.contains(code)) {
	            empRoleRepository.delete(er);
	        }
	    }

	    // 4) 필요한데 아직 없는 권한은 추가
	    for (String code : requiredRoles) {
	        if (!currentCodes.contains(code)) {
	            addRoleIfNotExists(emp, code);
	        }
	    }
	}
	
	// 퇴직 시 해당 사원의 모든 권한 삭제
	public void removeAllRoles(Emp emp) {
	    empRoleRepository.deleteByEmp_EmpId(emp.getEmpId());
	}

	
	// ========== 사원 등록 ========== 
	// 사원번호 생성 로직 (입사일 yyMM + 난수 3자리)
	private String generateEmpId(LocalDate hireDate, int maxRetry) {
		
		// 사원번호 yyMM
		// 입사일이 null이 아니면 그 날짜, null이면 오늘 날짜 사용
		LocalDate base = (hireDate != null) ? hireDate : LocalDate.now();
		String datePart = base.format(DateTimeFormatter.ofPattern("yyMM"));
		
		// 난수 3자리
		for (int i = 0; i < maxRetry; i++) {
			String randomPart = String.format("%03d", ThreadLocalRandom.current().nextInt(1000));
			
			// 최종 사번 조합
			String candidate = datePart + randomPart;
			// 이미 존재하는 사번인지 DB 확인
			boolean exists = empRepository.existsByEmpId(candidate);
			if (!exists) return candidate;
		}
		throw new IllegalStateException("사번 생성 충돌 : 재시도 초과");
	}
	
	// 1. 사원 신규 등록
	@Transactional
	public void registEmp(EmpDTO empDTO) {
		
		// 주민번호, 이메일, 연락처 중복 검사
		String rrn = empDTO.getRrn();
		String email = empDTO.getEmail();
		String mobile = empDTO.getMobile();
		
		if (rrn != null && !rrn.isBlank() && empRepository.existsByRrn(rrn)) {
			throw new IllegalStateException("이미 등록된 주민등록번호입니다.");
		}
		if (email != null && !email.isBlank() && empRepository.existsByEmail(email)) {
			throw new IllegalStateException("이미 사용 중인 이메일입니다.");
		}
		if (mobile != null && !mobile.isBlank() && empRepository.existsByMobile(mobile)) {
			throw new IllegalStateException("이미 사용 중인 연락처입니다.");
		}
		
		// 1) 사번 자동 생성 (충돌 방지 재시도)
		String empId = generateEmpId(empDTO.getHireDate(), 3);
		
		// 2) 존재하는 부서/직급 연결 - NOT NULL 보장
		Dept dept = deptRepository.findById(empDTO.getDeptId())
				.orElseThrow(() -> new IllegalArgumentException("부서 없음: " + empDTO.getDeptId()));
		Position position = positionRepository.findById(empDTO.getPosCode())
				.orElseThrow(() -> new IllegalArgumentException("직급 없음: " + empDTO.getPosCode()));
		
		// 3) DTO -> Enitity
		Emp emp = empDTO.toEntity();
		
		// 4) 서비스에서 세팅해야 하는 값들
		emp.setEmpId(empId);						// 자동 사번
		emp.setEmpPwd(encoder.encode("1234"));		// 초기 비밀번호
		emp.setHireDate(empDTO.getHireDate() != null ? empDTO.getHireDate() : LocalDate.now());
		emp.setStatus("ACTIVE");
		emp.setDept(dept);
		emp.setPosition(position);
		
		// 5) EMP 저장
		Emp savedEmp = empRepository.saveAndFlush(emp);
		
		// 6) 사원 사진 파일 업로드
		if (empDTO.getPhotoFile() != null && !empDTO.getPhotoFile().isEmpty()) {
			try {
				List<FileAttachDTO> uploadedList =
						fileUtil.uploadFile(savedEmp, List.of(empDTO.getPhotoFile()));
				
				// FILE_ATTACH 엔티티로 변환 후 저장
				List<FileAttach> attachEntities = uploadedList.stream()
						.map(FileAttachDTO::toEntity)
						.toList();
				
				fileAttachRepository.saveAll(attachEntities);
				
				// 첫 번째 파일의 FILE_ID 를 EMP.photoFileId에 연결
				Long photoFileId = attachEntities.get(0).getFileId();
				savedEmp.setPhotoFileId(photoFileId);
				
			} catch (IOException ioException) {
				throw new RuntimeException("사원 사진 업로드 중 오류가 발생했습니다.", ioException);
			}
		}
		
		// 7) 급여정보(EMP_BANK) 저장
		EmpBank bank = new EmpBank();
		bank.setEmpId(savedEmp.getEmpId());
		bank.setBankCode(empDTO.getBankCode());
		bank.setAccountNo(empDTO.getAccountNo());
		bank.setHolder(savedEmp.getEmpName());

		EmpBank savedBank = empBankRepository.saveAndFlush(bank);
		
		// 7-1) 통장 사본 파일 업로드
		if (empDTO.getBankbookFile() != null && !empDTO.getBankbookFile().isEmpty()) {
			try {
				List<FileAttachDTO> uploadedList =
						fileUtil.uploadFile(savedBank, List.of(empDTO.getBankbookFile()));
				
				List<FileAttach> attachEntities = uploadedList.stream()
						.map(FileAttachDTO::toEntity)
						.toList();
				
				fileAttachRepository.saveAll(attachEntities);
				
				Long fileId = attachEntities.get(0).getFileId();
				savedBank.setFileId(fileId);
				
			} catch (IOException ioException) {
				throw new RuntimeException("통장 사본 업로드 중 오류가 발생했습니다.", ioException);
			}
		}
		
		// 8) 역할 자동 부여
		assignDefaultRoles(savedEmp);
		
		// 9) 메신저 상태(MSG_STATUS) 저장
		MsgStatus msgStatus = new MsgStatus();
		
		msgStatus.setEmpId(empId);
		msgStatus.setAvlbStat("ONLINE");
		msgStatus.setAvlbUpdated(LocalDateTime.now());
		msgStatus.setAutoWorkStat("IN");
		msgStatus.setWorkStatUpdated(LocalDateTime.now());
		msgStatus.setWorkStatSource("AUTO");
		msgStatus.setOnlineYn("N");
	    int randomImg = ThreadLocalRandom.current().nextInt(1, 6);
	    msgStatus.setMsgProfile(randomImg);
	    
	    msgStatusRepository.save(msgStatus);
	    
	    // 10) 연차 생성
	    leaveService.createAnnualLeaveForEmp(empId);
	    
	    log.info("EMP 등록 완료: empId={}, dept={}, pos={}",
	            emp.getEmpId(), dept.getDeptName(), position.getPosName());
		
	}
	
	// ========== 활성화 부서/직급 ========== 
	// 활성화된 부서 목록 조회
	public List<Dept> getDeptList() {
		return deptRepository.findActive();
	}
	
	// 활성화된 직급 목록 조회
	public List<Position> getPositionList() {
		return positionRepository.findActive();
	}
	
	// ========== 사원 목록 조회 ========== 
	// 1. 사원 현황 목록
	public List<EmpListDTO> getEmpList(String keyword, String deptId) {
		
		// 공백 정리
		if (keyword != null) {
			keyword = keyword.trim();
		}
		if (deptId != null && deptId.isBlank()) {
            deptId = null;
        }
		
		// Pageable.unpaged() 사용해서 기존 searchEmpList 재활용
        Page<EmpListDTO> page =
                empRepository.searchEmpList(keyword, deptId, Pageable.unpaged());
		
		return page.getContent();
	}
	
	// 2. 인사 발령에서 사원 목록
	public List<EmpListDTO> getEmpListForHrAction(String deptId, String posCode, String status, String keyword) {
		
		if (keyword != null) {
	        keyword = keyword.trim();
	        if (keyword.isBlank()) keyword = null;
	    }
	    if (deptId != null && deptId.isBlank()) deptId = null;
	    if (posCode != null && posCode.isBlank()) posCode = null;
	    
	    return empRepository.searchEmpForHrAction(deptId, posCode, status, keyword);
	    
	}
	
	// ========== 사원 정보 조회 ==========
	// 1. 사원 정보 조회
	@Transactional(readOnly = true)
	public EmpDTO getEmp(String empId) {
		// EmpRepository - findByempId() 메서드 호출하여 사원 정보 조회
		Emp emp = empRepository.findByEmpId(empId)
				.orElseThrow(() -> new UsernameNotFoundException(empId + " 에 해당하는 사원이 없습니다!"));
		for(EmpRole er : emp.getEmpRoles()) {
			String code = (er.getRole() != null) ? er.getRole().getRoleCode() : "NULL";
	        log.info(">>> Role: {}", code);
		}
		// --------------------------------------------------------------------------
		// Emp 엔티티 -> EmpDTO 객체로 변환하여 리턴
		return EmpDTO.fromEntity(emp);
	}
	
	// 2. 사원 정보 상세 조회
	public EmpDetailDTO getEmpDetail(String empId) {
		
		Emp emp = empRepository.findById(empId)
	            .orElseThrow(() -> new EntityNotFoundException("사원 없음: " + empId));

		String address = buildAddress(emp);
	    String rrnMasked = maskRrn(emp.getRrn());
	    String bankInfo = buildBankInfo(emp);   // "국민은행 (123456-12-123456)"
	    String photoPath = buildPhotoPath(emp); // null일 수도 있음


	    return new EmpDetailDTO(
	            emp.getEmpId(),
	            emp.getEmpName(),
	            emp.getDept().getDeptName(),
	            emp.getPosition().getPosName(),
	            emp.getGender(),
	            emp.getHireDate() != null ? emp.getHireDate().toString() : "",
	            emp.getMobile(),
	            emp.getEmail(),
	            buildAddress(emp),
	            rrnMasked,
	            bankInfo,
	            photoPath
	    );
	}
	
	// 2-1. 상세 주소 없는 경우
	private String buildAddress(Emp emp) {
	    String addr1 = emp.getAddress1();
	    String addr2 = emp.getAddress2();

	    if (addr2 == null) addr2 = "";

	    return (addr1 + " " + addr2).trim();
	}
	
	// 2-2. 주민번호 마스킹
	public String maskRrn(String rrn) {
		if (rrn == null || rrn.isBlank()) {
			return "";
		}
		
		// 숫자만 추출
		String digits = rrn.replaceAll("\\D", "");
		if (digits.length() < 7) {
			return rrn;
		}
		
		String front = digits.substring(0, 6);	// 생년월일 6자리
		String mid = digits.substring(6, 7);	// 성별 1자리
		
		return front + "-" + mid + "******";
	}
	
	// 2-3. 계좌번호 마스킹
	private String maskAccount(String account) {
	    if (account == null || account.isBlank()) {
	        return "";
	    }

	    // 1) 숫자만 추출
	    String digits = account.replaceAll("\\D", "");
	    if (digits.length() < 4) {
	        // 너무 짧으면 그냥 원본 리턴하거나 전부 마스킹
	        return account;
	    }

	    int len = digits.length();
	    // 앞은 전부 * 로, 뒤 4자리만 살리기
	    String maskedDigits = "*".repeat(len - 4) + digits.substring(len - 4);

	    // 2) 원본 문자열 구조(하이픈 등)를 유지하면서 숫자만 치환
	    StringBuilder result = new StringBuilder();
	    int idx = 0;

	    for (char c : account.toCharArray()) {
	        if (Character.isDigit(c)) {
	            result.append(maskedDigits.charAt(idx++));
	        } else {
	            result.append(c); // -, 공백 등은 그대로
	        }
	    }

	    return result.toString();
	}
	
	// 2-4. 급여통장 문자열 조합
	private String buildBankInfo(Emp emp) {

	    return empBankRepository.findTopByEmpIdOrderByCreatedDateDesc(emp.getEmpId())
	            .map(bank -> {
	            	String bankName = bank.getBank().getCodeName();  		// 은행명
	            	String account  = maskAccount(bank.getAccountNo());     // 계좌번호
	            	String holder  = bank.getHolder();     

	            	// 두 줄 구조로 리턴
	                return bankName + " (" + account + ")\n예금주: " + holder;
	            })
	            .orElse("");
	}
	
	// 2-5. 프로필 사진 경로
	private String buildPhotoPath(Emp emp) {
	    Long photoFileId = emp.getPhotoFileId(); // Long 타입

	    if (photoFileId == null) {
	        return null; // 사진 없음 → JS에서 기본 이미지 처리
	    }
	    return "/files/download/" + photoFileId;
	}
	
	// ========== 사원 정보 수정 ==========
	@Transactional(readOnly = true)
	public EmpDTO getEmpForEdit(String empId) {
		
	    Emp emp = empRepository.findById(empId)
	        .orElseThrow(() -> new EntityNotFoundException("사원 없음: " + empId));
	    
	    EmpDTO empDTO = new EmpDTO();
	    
	    // 사원 인사 정보 
	    empDTO.setEmpId(emp.getEmpId());
	    empDTO.setEmpName(emp.getEmpName());
	    empDTO.setGender(emp.getGender());
	    empDTO.setRrn(emp.getRrn());
	    empDTO.setHireDate(emp.getHireDate());
	    empDTO.setEmail(emp.getEmail());
	    empDTO.setMobile(emp.getMobile());
	    empDTO.setPostCode(emp.getPostCode());
	    empDTO.setAddress1(emp.getAddress1());
	    empDTO.setAddress2(emp.getAddress2());
	    empDTO.setDeptId(emp.getDept().getDeptId());
	    empDTO.setPosCode(emp.getPosition().getPosCode());
	    empDTO.setRrnMasked(maskRrn(emp.getRrn()));
	    empDTO.setPhotoFileId(emp.getPhotoFileId());
	    
	    // 사원 급여정보
	    empBankRepository.findByEmpId(empId).ifPresent(bank -> {
	    	empDTO.setBankCode(bank.getBankCode());
	    	empDTO.setAccountNo(bank.getAccountNo());
	    	empDTO.setHolder(bank.getHolder());
	    	empDTO.setFileId(bank.getFileId());
    	});
	    
	    return empDTO;
	}
	
	@Transactional
	public void updateEmp(EmpDTO empDTO) {
		
		Emp emp = empRepository.findById(empDTO.getEmpId())
		            .orElseThrow(() -> new IllegalArgumentException("사원 없음"));
		
		// 2) 중복 체크 (자기 자신 제외)
	    if (empRepository.existsByEmailAndEmpIdNot(empDTO.getEmail(), empDTO.getEmpId())) {
	        throw new IllegalStateException("이미 사용 중인 이메일입니다.");
	    }

	    if (empRepository.existsByMobileAndEmpIdNot(empDTO.getMobile(), empDTO.getEmpId())) {
	        throw new IllegalStateException("이미 사용 중인 연락처입니다.");
	    }

	    // 변경 가능한 필드만 업데이트
	    emp.setEmpName(empDTO.getEmpName());
	    emp.setMobile(empDTO.getMobile());
	    emp.setEmail(empDTO.getEmail());
	    emp.setPostCode(empDTO.getPostCode());
	    emp.setAddress1(empDTO.getAddress1());
	    emp.setAddress2(empDTO.getAddress2());
		    
		// ============================
	    // 1) 프로필 사진 수정 처리
	    // ============================
	    if (empDTO.getPhotoFile() != null && !empDTO.getPhotoFile().isEmpty()) {
	        try {
	            // 새 파일 업로드
	            List<FileAttachDTO> uploadedList =
	                    fileUtil.uploadFile(emp, List.of(empDTO.getPhotoFile()));

	            List<FileAttach> attachEntities = uploadedList.stream()
	                    .map(FileAttachDTO::toEntity)
	                    .toList();

	            fileAttachRepository.saveAll(attachEntities);

	            // 새 파일의 ID로 교체
	            Long newFileId = attachEntities.get(0).getFileId();
	            emp.setPhotoFileId(newFileId);

	        } catch (IOException e) {
	            throw new RuntimeException("사원 사진 업로드 중 오류가 발생했습니다.", e);
	        }
	    }
	    
	    // ============================
	    // 2) 급여계좌 + 통장 사본 수정
	    // ============================
	    if (empDTO.getBankCode() != null && empDTO.getAccountNo() != null) {
	        EmpBank empBank = empBankRepository.findByEmpId(emp.getEmpId())
	                .orElseGet(() -> {
	                    EmpBank bank = new EmpBank();
	                    bank.setEmpId(emp.getEmpId());
	                    return bank;
	                });

	        empBank.setBankCode(empDTO.getBankCode());
	        empBank.setAccountNo(empDTO.getAccountNo());
	        empBank.setHolder(emp.getEmpName());

	        // --- 통장 사본 파일 수정 ---
	        if (empDTO.getBankbookFile() != null && !empDTO.getBankbookFile().isEmpty()) {
	            try {
	                List<FileAttachDTO> uploadedList =
	                        fileUtil.uploadFile(empBank, List.of(empDTO.getBankbookFile()));

	                List<FileAttach> attachEntities = uploadedList.stream()
	                        .map(FileAttachDTO::toEntity)
	                        .toList();

	                fileAttachRepository.saveAll(attachEntities);

	                Long newBankFileId = attachEntities.get(0).getFileId();
	                empBank.setFileId(newBankFileId);

	            } catch (IOException e) {
	                throw new RuntimeException("통장 사본 업로드 중 오류가 발생했습니다.", e);
	            }
	        } else {
	            // 새 파일 안 올리면 기존 fileId 유지
	            empBank.setFileId(empDTO.getFileId());
	        }

	        empBankRepository.save(empBank);
	    }
	}
	
	
	// ========== 비밀번호 변경 ==========
	// 일반 사원
	public void changePassword(String empId, String newPassword) {
        Emp emp = empRepository.findById(empId)
                .orElseThrow(() -> new EntityNotFoundException("사원 없음: " + empId));

        String encoded = encoder.encode(newPassword);
        emp.setEmpPwd(encoded);
        
        emp.setPwdChangeReq("N");
    }
	
	// 관리자 전용 - 비밀번호를 초기값(1234)로 재설정하는 기능
	public void resetPwdToDefaultByAdmin(String empId) {
		Emp emp = empRepository.findById(empId)
                .orElseThrow(() -> new EntityNotFoundException("사원 없음: " + empId));
		
		// 초기 비밀번호 (평문)
		String defaultPwd = "1234";
		
		// 암호화 저장
		String encoded = encoder.encode(defaultPwd);
		emp.setEmpPwd(encoded);
		
		// 다음 로그인 때 비밀번호 변경 강제
		emp.setPwdChangeReq("Y");
		
		log.info("[관리자 비밀번호 초기화] empId={}, 초기값=1234, pwdChangeReq=Y", empId);
	}
	
	@Transactional(readOnly = true)
	public Emp getEmpEntity(String empId) {
	    return empRepository.findById(empId)
	            .orElseThrow(() -> new EntityNotFoundException("사원 없음: " + empId));
	}
	
	

} // EmpService 끝
