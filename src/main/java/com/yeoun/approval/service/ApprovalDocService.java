package com.yeoun.approval.service;

import java.io.IOException;
import java.time.LocalDate;
import java.time.format.DateTimeFormatter;
import java.util.Arrays;
import java.util.Collections;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.util.StringUtils;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.multipart.MultipartFile;

import com.yeoun.approval.dto.ApprovalDocDTO;
import com.yeoun.approval.dto.ApprovalDocGridDTO;
import com.yeoun.approval.dto.ApprovalFormDTO;
import com.yeoun.approval.dto.ApproverDTO;
import com.yeoun.approval.entity.ApprovalForm;
import com.yeoun.approval.entity.Approver;
import com.yeoun.approval.entity.ApproverId;
import com.yeoun.approval.mapper.ApprovalDocMapper;
import com.yeoun.approval.mapper.ApprovalFormMapper;
import com.yeoun.approval.entity.ApprovalDoc;
import com.yeoun.approval.repository.ApprovalDocRepository;
import com.yeoun.approval.repository.ApproverRepository;
import com.yeoun.common.dto.FileAttachDTO;
import com.yeoun.common.entity.FileAttach;
import com.yeoun.common.repository.FileAttachRepository;
import com.yeoun.common.util.FileUtil;
import com.yeoun.common.wrapper.FileAttachWrapper;
import com.yeoun.common.service.FileAttachService;
import com.yeoun.emp.entity.Dept;
import com.yeoun.emp.entity.Emp;
import com.yeoun.emp.entity.Position;
import com.yeoun.emp.repository.EmpRepository;
import com.yeoun.hr.entity.HrAction;
import com.yeoun.hr.repository.HrActionRepository;
import com.yeoun.leave.service.LeaveService;

import jakarta.persistence.EntityNotFoundException;
import lombok.RequiredArgsConstructor;
import lombok.extern.log4j.Log4j2;

@Service
@RequiredArgsConstructor
@Log4j2
@Transactional
public class ApprovalDocService {

	private final ApprovalDocRepository approvalDocRepository;
	private final ApproverRepository approverRepository;
	private final FileAttachRepository fileAttachRepository;
	private final HrActionRepository hrActionRepository;
	private final ApprovalFormMapper approvalFormMapper;
	private final ApprovalDocMapper approvalDocMapper;
	private final LeaveService leaveService;
	private final FileUtil fileUtil;
	private final FileAttachService fileAttachService;
	private final EmpRepository empRepository;
	private static final DateTimeFormatter FORMATTER = DateTimeFormatter.ofPattern("yyyy-MM-dd");

	// 기안자 명 불러오기
	@Transactional(readOnly = true)
	public List<Emp> getEmp() {
		return approvalDocRepository.findAllMember();
	}

	// 기안자명 불러오기
	@Transactional(readOnly = true)
	public List<Object[]> getEmp2() {
		return approvalDocRepository.findAllMember2();
	}

	// 직급정보 불러오기
	@Transactional(readOnly = true)
	public List<Position> getPosition() {
		return approvalDocRepository.findPosition();
	}

	// 결재문서 파일 가져오기
	public List<FileAttachDTO> getApprovalDocFiles(Long approvalId) {
		List<FileAttach> fileList = fileAttachRepository.findByRefTableAndRefId("APPROVAL_DOC", approvalId);

		return fileList.stream().map(FileAttachDTO::fromEntity).toList();
	}

	// 검색 조회
	@Transactional(readOnly = true)
	public Map<String, List<ApprovalDocGridDTO>> getAllGridsData(String empId, Map<String, Object> searchParams) {
		log.info("searchParams 맵 전체 확인: {}", searchParams);
		searchParams.put("empId", empId);

		String createDate = (String) searchParams.get("createDate");
		String finishDate = (String) searchParams.get("finishDate");
		String empName = (String) searchParams.get("empName");
		String approvalTitle = (String) searchParams.get("approvalTitle");

		// 날짜 필터 xml 스타일(createDate -> createDateFilter)
		if (createDate != null && !createDate.trim().isEmpty()) {
			searchParams.put("createDateFilter", createDate);
		}
		if (finishDate != null && !finishDate.trim().isEmpty()) {
			searchParams.put("finishDateFilter", finishDate);
		}

		// 기안자및 문서양식 필터 통합 xml 스타일
		String unifiedFilterValue = null;
		if (empName != null && !empName.trim().isEmpty()) {
			unifiedFilterValue = empName;
		} else if (approvalTitle != null && !approvalTitle.trim().isEmpty()) {
			unifiedFilterValue = approvalTitle;
		}

		if (unifiedFilterValue != null) {
			searchParams.put("empNameTitleFilter", unifiedFilterValue);
		}

		Map<String, List<ApprovalDocGridDTO>> results = new HashMap<>();
		results.put("grid1Data", approvalDocMapper.searchApprovalItems(searchParams));// 결재사항
		results.put("grid2Data", approvalDocMapper.searchAllApproval(searchParams));// 전체결재
		results.put("grid3Data", approvalDocMapper.searchMyApprovalList(searchParams));// 내결재목록
		results.put("grid4Data", approvalDocMapper.searchPendingApproval(searchParams));// 결재대기
		results.put("grid5Data", approvalDocMapper.searchCompletedApproval(searchParams));// 결재완료

		return results;
	}

	// 결재 문서 등록
	public void saveApprovalDoc(String empId, Map<String, String> doc,
			@RequestParam(required = false) MultipartFile[] files) {

		log.info(">>>>>>>>>>>>>>>>>> approvalDoc : {}", doc);
		ApprovalDoc approvalDoc = new ApprovalDoc();

		// 날짜 파싱
		LocalDate createdDate = parseDateSafely(doc.get("createdDate"));
		LocalDate finishDate = parseDateSafely(doc.get("finishDate"));
		LocalDate startDate = parseDateSafely(doc.get("startDate"));
		LocalDate endDate = parseDateSafely(doc.get("endDate"));

		// ApprovalDoc 엔티티에 값 설정
		approvalDoc.setApprovalId(null);
		approvalDoc.setApprovalTitle(doc.get("approvalTitle"));
		approvalDoc.setEmpId(empId);
		approvalDoc.setCreatedDate(createdDate);
		approvalDoc.setFinishDate(finishDate);
		approvalDoc.setDocStatus(doc.get("docStatus"));
		approvalDoc.setFormType(doc.get("drafting"));
		approvalDoc.setApprover(doc.get("docApprover"));
		approvalDoc.setStartDate(startDate);
		approvalDoc.setEndDate(endDate);
		approvalDoc.setLeaveType(doc.get("leaveType"));
		approvalDoc.setToPosCode(doc.get("position"));
		approvalDoc.setToDeptId(doc.get("toDeptId"));
		approvalDoc.setExpndType(doc.get("expndType"));
		approvalDoc.setReason(doc.get("reason"));

		// 2. 문서 저장 및 생성된 ID 획득
		approvalDocRepository.save(approvalDoc);
		Long generatedApprovalId = approvalDoc.getApprovalId();

		// 3. 결재선 처리 (기존 로직 유지)
		String[] approverKeys = { "approverEmpIdOVD1", "approverEmpIdOVD2", "approverEmpIdOVD3" };
		for (String key : approverKeys) {
			processApprover(generatedApprovalId, doc, key);
		}

		// 4. 파일 업로드 및 DB 저장 로직 완성
		if (files != null && files.length > 0) {

			// 4-1. Array -> List 변환
			List<MultipartFile> fileList = Arrays.asList(files);

			// 4-2. FileAttachWrapper 객체 생성 (FileUploadHelpper 계약 이행)
			FileAttachWrapper wrapper = new FileAttachWrapper(
					generatedApprovalId,
					"APPROVAL_DOC");

			try {
				// 4-3. FileUtil 호출 (물리적 저장 및 DTO 목록 획득)
				List<FileAttachDTO> fileDtos = fileUtil.uploadFile(wrapper, fileList);

				// 4-4. DTO -> Entity 변환 및 추가 정보 설정
				List<FileAttach> finalFileList = fileDtos.stream()
						.map(dto -> dto.toEntity()) // DTO를 Entity로 변환
						.peek(fileAttach -> {
						})
						.toList();

				fileAttachRepository.saveAll(finalFileList);

			} catch (IOException e) {
				log.error("파일 업로드 중 오류 발생", e);
				// 파일 저장 실패 시 트랜잭션 롤백 유도
				throw new RuntimeException("공통 파일 처리 중 오류가 발생했습니다.", e);
			}
		}

		// 5. 인사발령신청서인 경우 HrAction 자동 생성
		if ("인사발령신청서".equals(doc.get("drafting"))) {
			createHrActionForApproval(empId, approvalDoc, doc);
		}
	}

	// 인사발령신청서용 HrAction 생성 메서드
	private void createHrActionForApproval(String empId, ApprovalDoc approvalDoc, Map<String, String> doc) {
		HrAction hrAction = new HrAction();

		// 결재문서 ID 설정
		hrAction.setApprovalId(approvalDoc.getApprovalId());

		// 발령 대상자 (기안자)
		Emp targetEmp = empRepository.findById(empId)
                .orElseThrow(() -> new IllegalArgumentException("사원을 찾을 수 없습니다. empId=" + empId));

		targetEmp.setEmpId(empId);
		hrAction.setEmp(targetEmp);

		// 등록자 (기안자)
		Emp createdUser = new Emp();
		createdUser.setEmpId(empId);
		hrAction.setCreatedUser(createdUser);

		// 발령유형 - 직위변경으로 기본 설정
		if(targetEmp.getDept().getDeptId().equals(doc.get("toDeptId"))) {
			hrAction.setActionType("PROMOTION");// 승진:직급상승
			log.info("[승진] ---> PROMOTION");
		}else{
			hrAction.setActionType("TRANSFER");//  전보:동일직급에서 다른부서로
			log.info("[전보] ---> TRANSFER");
		}
		//발령 효력 시작일 (휴직 시작일 / 복직일 / 퇴직일 공통 사용)
		LocalDate effectiveDate = parseDateSafely(doc.get("finishDate"));
		hrAction.setEffectiveDate(effectiveDate);
		//이전 직급 부서 설정
		//DEPT_ID
		//POS_CODE
        hrAction.setFromDept(targetEmp.getDept());			// 이전 부서
        hrAction.setFromPosition(targetEmp.getPosition());	// 이전 직급
		hrAction.setAppliedYn("N");// 발령은 처음 생성될 때 EMP에 미적용
		hrAction.setAppliedDate(null);     // 적용일자 없음

		
		// 이후 직급/부서 설정
		if (doc.get("position") != null && !doc.get("position").isEmpty()) {
			Position toPosition = new Position();
			toPosition.setPosCode(doc.get("position"));
			hrAction.setToPosition(toPosition);
		}

		if (doc.get("toDeptId") != null && !doc.get("toDeptId").isEmpty()) {
			Dept toDept = new Dept();
			toDept.setDeptId(doc.get("toDeptId"));
			hrAction.setToDept(toDept);
		}

		// 발령사유
		hrAction.setActionReason(doc.get("reason"));

		// 상태 설정
		hrAction.setStatus("대기");
		hrAction.setAppliedYn("N");

		hrActionRepository.save(hrAction);
		log.info("HrAction 생성 완료: approvalId={}", approvalDoc.getApprovalId());
	}

	// 기안서 양식종류
	@Transactional(readOnly = true)
	public List<ApprovalForm> getFormTypes(String deptId) {
		return approvalDocRepository.findAllFormTypes(deptId);
	}

	// 부서목록조회
	@Transactional(readOnly = true)
	public List<Dept> getDept() {
		return approvalDocRepository.findAllDepartments();
	}

	// 결재 승인 메서드
	@Transactional
	public void updateApproval(Long approvalId, String empId, String btn, String stampImageBase64) {

		// 1. 전달받은 문서ID를 사용하여 문서 엔티티 가져오기
		ApprovalDoc approvalDoc = approvalDocRepository.findById(approvalId)
				.orElseThrow(() -> new EntityNotFoundException("존재하지 않는 결재문서 입니다."));

		// 2. 반려(deny/reject) 버튼 처리 로직: 클라이언트에서 전송한 도장(Base64)이 있으면 저장
		if ("deny".equals(btn) || "reject".equals(btn)) {
			// 전달된 도장 이미지가 있으면 FileAttach로 저장
			if (stampImageBase64 != null && !stampImageBase64.isEmpty()) {
				// 현재 결재권자 정보(approver) 를 찾아 category/originFileName을 결정
				// approverRepository에서 현재 approvalId와 empId에 해당하는 Approver를 찾음
				List<Approver> approverListForStamp = approverRepository.findByApprovalId(approvalId);
				Approver currentApproverForStamp = approverListForStamp.stream()
						.filter(a -> a.getEmpId() != null && a.getEmpId().equals(empId))
						.findFirst().orElse(null);

				String category = null;
				if (currentApproverForStamp != null) {
					category = currentApproverForStamp.getOrderApprovers() + "_STAMP";
				}

				FileAttachDTO stampFileDTO = FileAttachDTO.builder()
						.refTable("APPROVER_STAMP")
						.refId(approvalId)
						.category(category)
						.originFileName(empId + "_stamp")
						.build();

				// FileAttachService를 통해 이미지 저장
				fileAttachService.saveStampImage(stampImageBase64, stampFileDTO);
			}

			// 문서 상태를 "반려"로 변경하고 저장
			approvalDoc.setDocStatus("반려");
			approvalDocRepository.save(approvalDoc);
			return;
		}

		// 3. 결재 승인 버튼을 눌렀을 때 ("accept"인 경우)
		if ("accept".equals(btn)) {

			// 3-1. 현재 로그인 사용자(empId)가 문서의 현재 결재 대기자(approver)인지 **1차 검증**
			if (!empId.equals(approvalDoc.getApprover())) {
				throw new IllegalArgumentException("현재 결재 대기자가 아닙니다.");
			}

			// 3-2. 해당 문서의 전체 승인권자 목록 가져오기 (총 순서 확인 및 현재 결재권자 찾기용)
			List<Approver> approverList = approverRepository.findByApprovalId(approvalId);

			// 3-3. 현재 로그인 사용자와 일치하는 Approver 객체 찾기
			Approver currentApprover = approverList.stream()
					.filter(approver -> empId.equals(approver.getEmpId()))
					.findFirst()
					.orElseThrow(() -> new EntityNotFoundException("해당 문서의 결재선에 등록되지 않은 사용자입니다."));

			// 현재 순서와 총 결재선 길이
			int currentOrder = Integer.parseInt(currentApprover.getOrderApprovers());
			int totalApproverCount = approverList.size();

			// 1. 도장 이미지를 FileAttach 테이블에 저장하고 ID를 받습니다.
			FileAttachDTO stampFileDTO = FileAttachDTO.builder()
					.refTable("APPROVER_STAMP") // 파일 연결 테이블 지정
					.refId(approvalId) // 결재 문서 ID와 연결
					.category(currentApprover.getOrderApprovers() + "_STAMP") // 결재 순서를 카테고리에 저장
					.originFileName(empId + "_stamp")
					.build();

			// 2. FileAttachService를 호출하여 이미지 저장 및 DB 기록
			fileAttachService.saveStampImage(stampImageBase64, stampFileDTO);

			currentApprover.setApprovalStatus(true); // boolean 필드를 true로 설정 (승인 완료)
			currentApprover.setViewing("Y"); // (선택 사항: 승인 후에도 열람 가능하도록 유지)
			// 결재 일시 및 도장 이미지 저장은 현재 엔티티 구조상 불가능하므로 생략됨.

			// 3-4. 최종 결재권자인지 확인 (현재 순서 == 총 인원 수)
			if (currentOrder == totalApproverCount) {

				approvalDoc.setDocStatus("완료");
				handleAfterFinalApproval(approvalDoc);

				// 연차신청서 처리
				if ("연차신청서".equals(approvalDoc.getFormType()) || "반차신청서".equals(approvalDoc.getFormType())) {
					leaveService.createAnnualLeave(approvalDoc.getApprovalId());
				}

			} else {

				// B. 다음 결재권자에게 이관 처리
				Long nextApproverOrder = (long) currentOrder + 1;

				// 3-5. 다음 순서의 결재권자 엔티티 조회 (기존 Repository 메서드 활용)
				Approver nextApprover = approverRepository.findByApprovalIdAndOrderApprovers(
						approvalId, nextApproverOrder.toString());

				if (nextApprover != null) {
					// 다음 결재권자의 VIEWING을 Y로 변경
					nextApprover.setViewing("Y");

					// approvalDoc의 approver을 다음 결재권자의 EmpId로 변경
					approvalDoc.setApprover(nextApprover.getEmpId());

					// approvalDoc의 status 변경 (예: "2차 대기")
					approvalDoc.setDocStatus(nextApprover.getOrderApprovers() + "차 대기");
				} else {
					// 다음 결재자가 없는 경우 (최종 승인으로 처리)
					approvalDoc.setDocStatus("완료");
					handleAfterFinalApproval(approvalDoc);
					
					// 연차신청서 처리
					if ("연차신청서".equals(approvalDoc.getFormType())) {
						leaveService.createAnnualLeave(approvalDoc.getApprovalId());
					}
				}
			}
		}
	}

	// 기본 결재권자 가져오기
	public List<ApprovalFormDTO> getDefaultApproverList(String empId) {

		List<ApprovalFormDTO> list = approvalFormMapper.findDefaultApproverList(empId);

		return list;
	}

	// ------------------------------------------------------------------------------
	// 인사 발령 등록 폼을 통한 신청 (승인 후처리)
	private void handleAfterFinalApproval(ApprovalDoc approvalDoc) {

		// 1) 문서가 인사발령 문서인지 확인
		if (!"인사발령신청서".equals(approvalDoc.getFormType())) {
			return; // 다른 양식이면 종료
		}

		// 2) 인사발령 서비스에 해당 결재문서의 발령을 적용
		Long approvalId = approvalDoc.getApprovalId();
		log.info("approvalId 인사발령신청서 id -----> {}", approvalId);
		HrAction hrAction = hrActionRepository.findByApprovalId(approvalId)
				.orElseThrow(() -> new EntityNotFoundException(
						// 예외 메시지 구체화
						"HR 데이터 누락: 결재 최종 승인 후처리용 인사발령(HrAction)을 찾을 수 없습니다. approvalId=" + approvalId));

		// 3) 발령 상태만 '승인완료'로 변경 (EMP 적용 금지)
		hrAction.setStatus("승인완료");

		// 4) 적용여부는 그대로 'N'
		hrAction.setAppliedYn("N");

		// 5) appliedDate NULL
		hrAction.setAppliedDate(null);


	}

	// 결제문서 조회시 결제권한자 목록 불러오기
	public List<ApproverDTO> getApproverDTOList(Long approvalId) {
		return approverRepository.findByApprovalId(approvalId).stream().map(ApproverDTO::fromEntity).toList();
	}

	// 결재 문서의 도장 이미지 조회 (결재 순서별로 Map 반환)
	public Map<String, String> getApprovalStampImages(Long approvalId) {
		Map<String, String> stampImages = new HashMap<>();

		// 1. 결재권자 목록 조회
		List<Approver> approverList = approverRepository.findByApprovalId(approvalId);

		// 2. APPROVER_STAMP 테이블에서 해당 결재 문서의 모든 도장 이미지 조회
		List<FileAttach> stampFiles = fileAttachRepository.findByRefTableAndRefId("APPROVER_STAMP", approvalId);

		// 3. 결재권자 정보와 도장 이미지 매칭 (order와 empId 모두 기준으로)
		for (Approver approver : approverList) {
			String order = approver.getOrderApprovers(); // "1", "2", "3"
			String empId = approver.getEmpId();

			// 해당 결재 순서의 도장 찾기 (category 또는 originFileName으로 매칭)
			// 동일 결재자에 대해 여러 파일이 있을 수 있으므로 fileId가 가장 큰(최신) 항목을 선택
			FileAttach bestFile = null;
			Long bestFileId = null;
			for (FileAttach file : stampFiles) {
				String category = file.getCategory();
				String originFileName = file.getOriginFileName();

				boolean matchByCategory = category != null && category.equals(order + "_STAMP");
				boolean matchByEmpId = originFileName != null && originFileName.startsWith(empId + "_");

				if (matchByCategory || matchByEmpId) {
					Long fid = file.getFileId();
					if (fid == null) continue;
					if (bestFile == null || bestFileId == null || fid > bestFileId) {
						bestFile = file;
						bestFileId = fid;
					}
				}
			}
			if (bestFile != null && bestFileId != null) {
				// order와 empId 둘 다로 매핑 (JavaScript에서 어느 쪽으로든 찾을 수 있도록)
				String downloadUrl = "/files/download/" + bestFileId;
				stampImages.put(order, downloadUrl);        // 순서 기준
				stampImages.put("emp_" + empId, downloadUrl); // empId 기준
			}
		}

		log.info("결재 문서 {} 의 도장 이미지 조회 결과: {}", approvalId, stampImages);
		return stampImages;
	}

	// -------------------------------------------------------------------------------
	// 메인페이지 내가 결제할 결제문서, 내가올린 결제 문서 불러오기
	public Page<ApprovalDocDTO> getSummaryApproval(String empId) {

		PageRequest pageRequest = PageRequest.of(0, 5);
		Page<ApprovalDoc> approvalDOCPage = approvalDocRepository.getSummaryApprovalPage(empId, pageRequest);

		return approvalDOCPage.map(ApprovalDocDTO::fromEntity);
	}

	// 문자열을 LocalDate로 안전하게 파싱하는 헬퍼 메서드
	private LocalDate parseDateSafely(String dateString) {
		if (StringUtils.hasText(dateString)) {
			try {
				return LocalDate.parse(dateString, FORMATTER);
			} catch (Exception e) {
				System.err.println(dateString + " 날짜 파싱 오류 발생: " + e.getMessage());
			}
		}
		return null;
	}

	private void processApprover(Long approvalId, Map<String, String> doc, String docKey) {
		String approverString = doc.get(docKey);

		// NullPointerException 방지를 위해 StringUtils.hasText로 확인
		if (StringUtils.hasText(approverString)) {
			String[] parts = approverString.split(","); // 예: ["2506864", "1", "Y"]

			// parts 배열의 길이가 3 이상인지 확인
			if (parts.length >= 3) {
				Approver approver = new Approver();

				approver.setApprovalId(approvalId); // 생성된 문서 ID 설정
				approver.setEmpId(parts[0]); // 사번
				approver.setOrderApprovers(parts[1]); // 순서
				approver.setViewing(parts[2]); // 열람 권한
				approver.setDelegateStatus(parts[3]); // 결재권한자변경여부//전결자 상태

				// 기타 필드 설정
				approver.setApprovalStatus(false); // 기본 상태: 미결재

				// Approver 엔티티 저장
				approverRepository.save(approver);

			} else {
				log.warn("결재자 데이터 형식이 올바르지 않습니다 ({}): {}", docKey, approverString);
			}
		}
	}
}
