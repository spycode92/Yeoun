package com.yeoun.hr.service;

import java.time.LocalDate;
import java.util.Comparator;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.stereotype.Service;

import com.yeoun.approval.entity.ApprovalDoc;
import com.yeoun.approval.entity.ApprovalForm;
import com.yeoun.approval.entity.Approver;
import com.yeoun.approval.repository.ApprovalDocRepository;
import com.yeoun.approval.repository.ApprovalFormRepository;
import com.yeoun.approval.repository.ApproverRepository;
import com.yeoun.auth.dto.LoginDTO;
import com.yeoun.common.entity.CommonCode;
import com.yeoun.common.service.CommonCodeService;
import com.yeoun.emp.entity.Dept;
import com.yeoun.emp.entity.Emp;
import com.yeoun.emp.entity.Position;
import com.yeoun.emp.repository.DeptRepository;
import com.yeoun.emp.repository.EmpRepository;
import com.yeoun.emp.repository.PositionRepository;
import com.yeoun.emp.service.EmpService;
import com.yeoun.hr.dto.HrActionDTO;
import com.yeoun.hr.dto.HrActionRequestDTO;
import com.yeoun.hr.entity.HrAction;
import com.yeoun.hr.repository.HrActionRepository;

import jakarta.transaction.Transactional;
import lombok.RequiredArgsConstructor;
import lombok.extern.log4j.Log4j2;

@Service
@RequiredArgsConstructor
@Log4j2
public class HrActionService {

	private final EmpService empService;
    private final HrActionRepository hrActionRepository;
    private final EmpRepository empRepository;
    private final DeptRepository deptRepository;
    private final PositionRepository positionRepository;
    private final CommonCodeService commonCodeService;
    
    private final ApprovalDocRepository approvalDocRepository;
    private final ApprovalFormRepository approvalFormRepository;
    private final ApproverRepository approverRepository;
    
    // ----------------------------------------------------------------------------
    // 발령 코드에 맞는 이름
    private String getActionTypeName(String actionType) {

        if (actionType == null || actionType.isBlank()) {
            return "인사발령";
        }

        return switch (actionType) {
            case "PROMOTION"  -> "승진";
            case "TRANSFER"   -> "전보";
            case "RETIRE_ACT" -> "퇴직";
            case "LEAVE_ACT"  -> "휴직";
            case "RETURN_ACT" -> "복직";
            default           -> "인사발령";
        };
    }
    
    /**
     *  1. 인사 발령 등록 및 전자결재 생성
     *   - 인사 발령 신청 폼에서 넘어온 데이터를 받아서 HR_ACTION 발령 레코드 생성
     *   - APPROVAL_DOC 전자결재 문서 생성
     *   - APPROVER 결재선 (1차, 2차, 3차) 생성
     *   - 발령과 결재문서 연결 => approvalId 세팅 => 발령ID (actionId) 리턴 
     */
    @Transactional
    public Long createAction(HrActionRequestDTO hrActionRequestDTO) {

    	// ======================================
        // 1. 로그인 사용자(신청자) 정보 가져오기
        // ======================================
        Authentication auth = SecurityContextHolder.getContext().getAuthentication();
        LoginDTO loginUser = (LoginDTO) auth.getPrincipal();
        String loginEmpId = loginUser.getEmpId();
        
        Emp creator = empRepository.findById(loginEmpId)
                .orElseThrow(() -> new IllegalArgumentException("등록자 사원을 찾을 수 없습니다. empId=" + loginEmpId));

        // ======================================
        // 2. 발령 대상 사원 / 부서 / 직급 조회
        // ======================================
        // 1) 발령 타입 및 발령 대상
        String actionType = hrActionRequestDTO.getActionType();
        
        Emp emp = empRepository.findById(hrActionRequestDTO.getEmpId())
                .orElseThrow(() -> new IllegalArgumentException("사원을 찾을 수 없습니다. empId=" + hrActionRequestDTO.getEmpId()));

        // 2) 부서 및 직급
        Dept toDept = null;
        Position toPos = null;
        
        // 퇴직/휴직/복직이 아닐 때만 발령 후 부서 및 직급을 조회
        if (!"RETIRE_ACT".equals(actionType) && !"LEAVE_ACT".equals(actionType) && !"RETURN_ACT".equals(actionType)) {
            toDept = deptRepository.findById(hrActionRequestDTO.getToDeptId())
                    .orElseThrow(() -> new IllegalArgumentException("부서를 찾을 수 없습니다. deptId=" + hrActionRequestDTO.getToDeptId()));

            toPos = positionRepository.findById(hrActionRequestDTO.getToPosCode())
                    .orElseThrow(() -> new IllegalArgumentException("직급을 찾을 수 없습니다. posCode=" + hrActionRequestDTO.getToPosCode()));
        }
        
        // ==========================
        // 3. HR_ACTION 엔티티 생성
        // ==========================
        // 1) 기본 필드 세팅 (DTO -> 엔티티)
        HrAction action = hrActionRequestDTO.toEntity();	

        // 2) 관계 필드 세팅
        action.setEmp(emp);							// 발령 대상 사원
        action.setFromDept(emp.getDept());			// 이전 부서
        action.setFromPosition(emp.getPosition());	// 이전 직급
        
        // 퇴직/휴직/복직 아닐 때만 toDept / toPos 세팅
        if (!"RETIRE_ACT".equals(actionType) && !"LEAVE_ACT".equals(actionType) && !"RETURN_ACT".equals(actionType)) {
            action.setToDept(toDept);
            action.setToPosition(toPos);
        }

        action.setCreatedUser(creator);  // 발령을 신청한 사람 (로그인 사원)
        action.setStatus("대기");	  	 // 발령 상태 기본값 (요청 상태)
        action.setAppliedYn("N");     	 // 발령은 처음 생성될 때 EMP에 미적용
        action.setAppliedDate(null);     // 적용일자 없음

        // 3) 발령(HR_ACTION) 저장
        HrAction saved = hrActionRepository.save(action);
        
        // ==========================
        // 4. 전자결재 문서(approval_doc) 생성
        // ==========================
        // 1) 문서 제목 자동 생성
        String actionTypeName = getActionTypeName(actionType);
        
        String title;
        
        // 퇴직/휴직/복직일 경우
        if ("RETIRE_ACT".equals(actionType) || "LEAVE_ACT".equals(actionType) || "RETURN_ACT".equals(actionType)) {
        	title = String.format("[인사발령/%s] %s (%s %s)",
        			actionTypeName,
        			emp.getEmpName(),
        			emp.getDept().getDeptName(),
        			emp.getPosition().getPosName());
        } else {
        	// 그 외 경우
        	title = String.format(
    			"[인사발령/%s] %s %s %s → %s %s",
    			actionTypeName,                      // 승진, 전보 등
    			emp.getDept().getDeptName(),		 // 인사부
    			emp.getPosition().getPosName(),      // 대리
    			emp.getEmpName(),                    // 홍길동
    			toDept.getDeptName(),                // 영업부
    			toPos.getPosName()                   // 과장
			);
        }
        
        // ==========================
        // 4. 결재선(approver) 생성
        // ==========================
        // 4-1. 신청자 소속 부서 기준으로 approval_form 가져오기
        String formName = "인사발령신청서";
        String deptIdForForm = emp.getDept().getDeptId(); 
        
        ApprovalForm form = approvalFormRepository
                .findByFormNameAndDeptId(formName, deptIdForForm)
                .orElseThrow(() -> new IllegalStateException(
                        "해당 부서의 인사발령 결재양식이 없습니다. deptId=" + deptIdForForm));
        
        // 3-2. approval_doc 엔티티 생성
        ApprovalDoc approvalDoc = new ApprovalDoc();
        approvalDoc.setApprovalTitle(title);							// 문서제목
        approvalDoc.setEmpId(creator.getEmpId());						// 사원번호
        approvalDoc.setCreatedDate(LocalDate.now());					// 생성일자
        approvalDoc.setDocStatus("1차대기");							// 문서상태
        approvalDoc.setFormType("인사발령신청서");						// 양식종류
        // 퇴직일 때는 현재 부서 or null 등 선택
        if (!"RETIRE_ACT".equals(actionType) && toDept != null) {
            approvalDoc.setToDeptId(toDept.getDeptId());
        } else {
            // 그냥 현재 소속부서를 넣고 싶으면:
            approvalDoc.setToDeptId(emp.getDept().getDeptId());
            // 아니면 컬럼이 nullable이면 setToDeptId(null) 도 가능
        }
        approvalDoc.setReason(hrActionRequestDTO.getActionReason());	// 발령사유
        
        // 3-3. 현재 결재자(1차 결재자) 세팅
        if (form.getApprover1() != null) {
            approvalDoc.setApprover(form.getApprover1());
        }
        
        // 3-4. 전자결재 문서 저장
        ApprovalDoc savedDoc = approvalDocRepository.save(approvalDoc);
        Long approvalId = savedDoc.getApprovalId();

        int order = 1;
        
        // ==========================
        // 4-2. 결재선(Approver) 엔티티들 생성
        // ==========================
        
        // APPROVER_1 (1차 결재자)
        if (form.getApprover1() != null) {
            Approver line1 = new Approver();
            line1.setEmpId(form.getApprover1());				// 결재자 사번
            line1.setApprovalId(approvalId);					// 결재문서 ID
            line1.setApprovalStatus(false);      				// 승인 여부 (false = 대기)
            line1.setOrderApprovers(String.valueOf(order++));	// 결재 순서: 1
            line1.setDelegateStatus(null);  					// 대결자 여부
            line1.setViewing("Y");								// 결재문서 목록에 보이도록
            approverRepository.save(line1);
        }

        // APPROVER_2
        if (form.getApprover2() != null) {
            Approver line2 = new Approver();
            line2.setEmpId(form.getApprover2());
            line2.setApprovalId(approvalId);
            line2.setApprovalStatus(false);
            line2.setOrderApprovers(String.valueOf(order++));
            line2.setDelegateStatus(null);
            line2.setViewing(null);								// 아직 열람 대상 아님
            approverRepository.save(line2);
        }

        // APPROVER_3
        if (form.getApprover3() != null) {
            Approver line3 = new Approver();
            line3.setEmpId(form.getApprover3());
            line3.setApprovalId(approvalId);
            line3.setApprovalStatus(false);
            line3.setOrderApprovers(String.valueOf(order++));
            line3.setDelegateStatus(null);
            line3.setViewing(null);
            approverRepository.save(line3);
        }

        // ==========================
        // 5. HR_ACTION에 approvalId 연결
        // ==========================
        saved.setApprovalId(approvalId);      // 발령(HR_ACTION) ← 결재문서 ID 매핑

        return saved.getActionId();
    }
    
	// ==========================
    // 2. 인사 발령 목록
    // ==========================
    // 인사 발령 자동 적용
    @Transactional
	public void applyScheduledHrActions() {
    	
    	LocalDate today = LocalDate.now();
    	log.info("[발령자동적용] {} 기준 발령 적용 시작", today);
    	
    	// 1. 승인 완료 + 미적용 + 효력일 도달한 발령 리스트 조회
    	List<HrAction> targetList =
    			hrActionRepository.findByStatusAndAppliedYnAndEffectiveDateLessThanEqual(
    				"승인완료",		// 최종 승인 상태
    				"N", 			// 아직 적용 안 됨
    				today			// 효력일이 오늘 이전/오늘
				);
    	
    	if(targetList.isEmpty()) {
    		log.info("[발령자동적용] 적용할 발령 없음");
    		return;
    	}
    	
    	log.info("[발령자동적용] 총 {}건 적용 예정", targetList.size());
    	
    	// 2. 각 발령을 실제 EMP 정보에 반영
    	for (HrAction action : targetList) {
    		
    		Emp emp = action.getEmp();	// 발령 대상 사원
    		
    		// 발령 유형에 따라 분기
    		switch (action.getActionType()) {
    			case "PROMOTION":
    				if (action.getToPosition() != null) {
    					emp.setPosition(action.getToPosition());
    					log.info("[발령적용] 승진 적용 - 사번:{} / 직급:{}",
                                emp.getEmpId(),
                                action.getToPosition().getPosName());
				}
    				
    			// 승진 후 권한 싱크
    			empService.syncRolesByDeptAndPos(emp);
    			break;
    			
    			case "TRANSFER":
    				if (action.getToDept() != null) {
    					emp.setDept(action.getToDept());
    					log.info("[발령적용] 전보 적용 - 사번:{} / 부서:{}",
                                emp.getEmpId(),
                                action.getToDept().getDeptName());
    				}
    				if(action.getToPosition() != null) {
    					emp.setPosition(action.getToPosition());
    					log.info("[발령적용] 전보 직급 변경 - 사번:{} / 직급:{}",
                                emp.getEmpId(),
                                action.getToPosition().getPosName());
    				}
    				
    				// 전보 후 권한 싱크
        			empService.syncRolesByDeptAndPos(emp);
    				break;
    				
    			case "RETIRE_ACT":
    				emp.setStatus("RETIRE");
    				emp.setRetireDate(action.getEffectiveDate());
    				log.info("[발령적용] 퇴직 적용 - 사번:{} / 퇴사일:{}",
    	                     emp.getEmpId(), action.getEffectiveDate());
    				
    				// 퇴직 시 권한 전부 삭제
    				empService.removeAllRoles(emp);
    				
    	            break;
    	            
    			case "LEAVE_ACT":
    				emp.setStatus("LEAVE");
    				break;
    				
    			case "RETURN_ACT":
    				emp.setStatus("ACTIVE");
    				break;
    				
				default:
					log.warn("[발령적용] 미지원 발령타입: {} (ACTION_ID={})",
                            action.getActionType(), action.getActionId());
                    continue;
    		}
    		
    		// 3) HR_ACTION 상태 업데이트 (적용여부 / 적용일자 / 상태)
            action.setAppliedYn("Y");          // 이제 적용 완료
            action.setAppliedDate(today);      // 실제 반영된 날짜 기록
            action.setStatus("적용완료");
    		
    	}
    	
    	log.info("[발령자동적용] 발령 {}건 적용 완료", targetList.size());
		
	}

	// ==========================
    // 3. 인사 발령 목록
    // ==========================
    public List<HrActionDTO> getHrActionList(String keyword, String actionType, String startDate, String endDate) {

		// 0) 검색값 정리
		if (keyword != null) keyword = keyword.trim();
		if (actionType != null && actionType.isBlank()) actionType = null;
		
		LocalDate start = null;
		LocalDate end = null;
		if (startDate != null && !startDate.isBlank()) {
			start = LocalDate.parse(startDate);   // "yyyy-MM-dd" 형식 가정
		}
		if (endDate != null && !endDate.isBlank()) {
			end = LocalDate.parse(endDate);
		}
		
		// 1) 공통코드 Map
		Map<String, String> actionTypeMap = commonCodeService.getHrActionTypeList()
				.stream()
				.collect(Collectors.toMap(
						 CommonCode::getCodeId,   
						 CommonCode::getCodeName  
				));
		
		// 2) 조건 걸어서 엔티티 조회
		List<HrAction> actionList =
				hrActionRepository.searchHrActions(keyword, actionType, start, end);
		
		// 3) 날짜 정렬(내림차순)
	    actionList.sort(
	            Comparator.comparing(HrAction::getEffectiveDate, Comparator.nullsLast(Comparator.naturalOrder()))
	                      .reversed()
	    );
		
	    // 4) DTO 변환해서 List 로 반환
	    return actionList.stream()
	            .map(a -> {
	                String typeCode = a.getActionType();
	                String typeName = actionTypeMap.getOrDefault(typeCode, typeCode);

	                return new HrActionDTO(
	                        a.getActionId(),
	                        a.getEmp().getEmpId(),
	                        a.getEmp().getEmpName(),
	                        typeCode,
	                        typeName,
	                        a.getEffectiveDate(),
	                        a.getLeaveEndDate(),
	                        a.getFromDept() != null ? a.getFromDept().getDeptName() : null,
	                        a.getToDept() != null ? a.getToDept().getDeptName() : null,
	                        a.getFromPosition() != null ? a.getFromPosition().getPosName() : null,
	                        a.getToPosition() != null ? a.getToPosition().getPosName() : null,
	                        a.getStatus(),
	                        a.getCreatedDate()
	                );
	            })
	            .collect(Collectors.toList());
	}


        
        
        
        
        
        
        
        
        
}
