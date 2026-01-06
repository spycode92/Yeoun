package com.yeoun.notice.service;

import java.io.IOException;
import java.util.List;

import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Sort;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.Authentication;
import org.springframework.stereotype.Service;
import org.springframework.web.multipart.MultipartFile;

import com.yeoun.common.dto.FileAttachDTO;
import com.yeoun.common.entity.FileAttach;
import com.yeoun.common.repository.FileAttachRepository;
import com.yeoun.common.service.FileAttachService;
import com.yeoun.common.util.FileUtil;
import com.yeoun.emp.entity.Emp;
import com.yeoun.emp.repository.EmpRepository;
import com.yeoun.notice.dto.NoticeDTO;
import com.yeoun.notice.entity.Notice;
import com.yeoun.notice.repository.NoticeRepository;

import groovy.util.logging.Log4j2;
import jakarta.persistence.EntityNotFoundException;
import jakarta.transaction.Transactional;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;

@Service
@RequiredArgsConstructor
@Log4j2
public class NoticeService {
	private final NoticeRepository noticeRepository;
	private final FileAttachRepository fileAttachRepository;
	private final FileAttachService fileAttachService;
	private final EmpRepository empRepository;
	
	private final FileUtil fileUtil;
	
	//공지리스트 불러오기
	public Page<NoticeDTO> getNotice(int page, int size, String searchKeyword, String orderKey, String orderMethod) {
		// 동적정렬 오름차순, 내림차순 결정
		Sort.Direction direction = "asc".equalsIgnoreCase(orderMethod) ? Sort.Direction.ASC : Sort.Direction.DESC;
		// 동적정렬 객체생성 (orderkey, orderMethod)
		Sort.Order dynamicOrder = new Sort.Order(direction, orderKey);
		
		// 기본 정렬 기준(고정여부, 수정일 내림차순) + 동적정렬기준 해서 정렬객체 생성
	    Sort sort = Sort.by(Sort.Order.desc("noticeYN"), dynamicOrder);
		
		// 페이징과 정렬을 포함하는 PageRequest 생성
		PageRequest pageRequest = PageRequest.of(page, size, sort);
		
		Page<Notice> noticePage;
		
		// 검색어가 존재하면 제목,내용에 포함된 공지사항 조회
	    if (searchKeyword != null && !searchKeyword.isEmpty()) {
	    	noticePage = noticeRepository.searchNotice(searchKeyword, pageRequest);
	    } else { // 존재하지 않을 시 전체 공지사항 조회
	    	noticePage = noticeRepository.findByDeleteYN("N", pageRequest);
	    }
	    
//    	System.out.println(noticePage);

	    return noticePage.map(NoticeDTO::fromEntity);
	}
	
	// 공지상세 조회하기
	public NoticeDTO getOneNotice(Long noticeId) {
		Notice notice = noticeRepository.findById(noticeId).orElse(null);
		
		return NoticeDTO.fromEntity(notice);
	}
	
	//공지사항 등록하기
	@Transactional
	public void createNotice(NoticeDTO noticeDTO, List<MultipartFile> noticeFiles) throws IOException {
		
		// 공지사항 DB등록
		if(noticeDTO.getNoticeYN() == null) {
			noticeDTO.setNoticeYN("N");
		}
//		System.out.println(noticeFiles);
		Notice notice = noticeDTO.toEntity();
		noticeRepository.save(notice);
		
		// ===========================================
		// 파일업로드 시작
		List<FileAttach> fileList = fileUtil.uploadFile(notice, noticeFiles).stream()
										.map(FileAttachDTO::toEntity)
										.toList();
		
		// 파일 DB 저장
		fileAttachRepository.saveAll(fileList);
		
	}
	
	//공지사항 수정하기
	@Transactional
	public void modifyNotice(@Valid NoticeDTO noticeDTO, List<MultipartFile> noticeFiles, String empId) throws IOException {
		Long noticeId = noticeDTO.getNoticeId();
		Notice notice = noticeRepository.findById(noticeId).orElseThrow(() -> new EntityNotFoundException("공지사항이 존재하지 않습니다."));
		Emp emp = empRepository.findById(empId).orElseThrow(() -> new EntityNotFoundException("존재하지않는 사원입니다"));
		
		notice.setUpdateEmp(emp);
		notice.setNoticeTitle(noticeDTO.getNoticeTitle()); //변경된 제목 적용
		notice.setNoticeContent(noticeDTO.getNoticeContent()); //변경된 내용 적용
		
		if(noticeDTO.getNoticeYN() == null) {
			noticeDTO.setNoticeYN("N"); // 고정여부 값설정
		}
		
		notice.setNoticeYN(noticeDTO.getNoticeYN()); // 고정여부 변경값 적용
		// 트랜잭션이 끝날때 변경사항 자동 적용(더티체킹)
		
		// ===========================================
		// 파일업로드 시작
		List<FileAttach> fileList = fileUtil.uploadFile(notice, noticeFiles).stream()
										.map(FileAttachDTO::toEntity)
										.toList();
		
		// 파일 DB 저장
		fileAttachRepository.saveAll(fileList);
	}
	
	// 공지사항 삭제하기
	@Transactional
	public void deleteNotice(Long noticeId, String empId) {
		Notice notice = noticeRepository.findById(noticeId).orElseThrow(() -> new EntityNotFoundException("공지사항이 존재하지 않습니다."));
		Emp emp = empRepository.findById(empId).orElseThrow(() -> new EntityNotFoundException("존재하지않는 사원입니다"));
		notice.setUpdateEmp(emp);
		notice.setDeleteYN("Y");
		
		// ======================================
		// 파일 삭제
		// noticeId, NOTICE 활용하여 파일리스트 객체 가져오기
		List<FileAttach> fileList = fileAttachRepository.findByRefTableAndRefId("NOTICE", noticeId);
		
		fileAttachService.removeFiles(fileList);
	}
	
	// 공지사항 파일 가져오기
	public List<FileAttachDTO> getNoticeFiles(Long noticeId) {
		List<FileAttach> fileList = fileAttachRepository.findByRefTableAndRefId("NOTICE", noticeId);
		
		return fileList.stream().map(FileAttachDTO::fromEntity).toList(); 
	}
	
	
	// 최근 공지사항 목록 불러오기 (메인페이지)
	public Page<NoticeDTO> getLastNotice(int page, int size) {
		// 기본 정렬 기준(고정여부, 수정일 내림차순) + 동적정렬기준 해서 정렬객체 생성
	    Sort sort = Sort.by(Sort.Order.desc("updatedDate"));
		
		// 페이징과 정렬을 포함하는 PageRequest 생성
		PageRequest pageRequest = PageRequest.of(page, size, sort);
		
		Page<Notice> noticePage;

    	noticePage = noticeRepository.findByDeleteYN("N", pageRequest);

	    return noticePage.map(NoticeDTO::fromEntity);
	}
	
	


}
