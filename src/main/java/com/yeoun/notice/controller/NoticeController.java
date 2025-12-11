package com.yeoun.notice.controller;

import java.util.HashMap;
import java.util.List;
import java.util.Map;

import org.springframework.data.domain.Page;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.Authentication;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.validation.BindingResult;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.ModelAttribute;
import org.springframework.web.bind.annotation.PatchMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.multipart.MultipartFile;

import com.yeoun.common.util.CommonUtil;
import com.yeoun.notice.dto.NoticeDTO;
import com.yeoun.notice.service.NoticeService;

import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.PostMapping;



@Controller
@RequiredArgsConstructor
@RequestMapping("/notice")
public class NoticeController {
	private final NoticeService noticeService;
	
	// 공지사항 목록 조회
	@GetMapping("")
	public String notices(Model model,
			@RequestParam(defaultValue = "0", name = "page")int page,
		    @RequestParam(defaultValue = "10", name = "size")int size,
		    @RequestParam(defaultValue = "", name = "searchKeyword")String searchKeyword,
		    @RequestParam(defaultValue = "updatedDate", name = "orderKey")String orderKey,
		    @RequestParam(defaultValue = "desc", name = "orderMethod")String orderMethod) {
		
		Page<NoticeDTO> noticePage = noticeService.getNotice(page, size, searchKeyword, orderKey, orderMethod);
		
	    model.addAttribute("noticeDTOList", noticePage.getContent());
	    model.addAttribute("currentPage", noticePage.getNumber()); // 현재 페이지
	    model.addAttribute("totalPages", noticePage.getTotalPages()); // 
	    model.addAttribute("searchKeyword", searchKeyword);
	    model.addAttribute("orderKey", orderKey);
	    model.addAttribute("orderMethod", orderMethod);
	    
	    System.out.println("노티스페이지" + noticePage.getContent());
	    System.out.println("totalPages" + noticePage.getTotalPages());
		return "/notice/notice";
	}
	
	//공지사항 등록 로직
	@PostMapping("")
	public ResponseEntity<Map<String, String>> notices(@ModelAttribute("noticeDTO") @Valid NoticeDTO noticeDTO, 
			BindingResult bindingResult, Authentication authentication
			, @RequestParam("noticeFiles") List<MultipartFile> noticeFiles) {
		
		Map<String, String> msg = new HashMap<>();
		// 서버에서 권한비교 한번더 해주기
		if(!CommonUtil.hasRole(authentication, "ROLE_NOTICE_WRITER")) {
			msg.put("msg", "권한이 없습니다!");
			return ResponseEntity.status(HttpStatus.FORBIDDEN).body(msg);
		}

//		System.out.println("noticeDTO : " + noticeDTO);
		if(bindingResult.hasErrors()) {
			msg.put("msg", "공지사항 등록에 실패했습니다");
			return ResponseEntity.status(HttpStatus.BAD_REQUEST).body(msg);
		}
		noticeDTO.setCreatedUser(authentication.getName());
		try {
			// 공지 등록 수행
			noticeService.createNotice(noticeDTO, noticeFiles);
			msg.put("msg", "공지사항이 등록되었습니다.");
			return ResponseEntity.ok(msg);
		
		} catch (Exception e) {
			msg.put("msg", "공지사항 등록에 실패했습니다 :" + e.getMessage());
			return ResponseEntity.status(HttpStatus.BAD_REQUEST).body(msg);
		}
	}
	
	//공지사항 수정 로직
	@PatchMapping("/{noticeId}")
	public ResponseEntity<Map<String, String>> notices( @PathVariable("noticeId")Long noticeId,
			@ModelAttribute("noticeDTO") @Valid NoticeDTO noticeDTO, BindingResult bindingResult
			, @RequestParam("noticeFiles") List<MultipartFile> noticeFiles, Authentication authentication) {
	
		Map<String, String> msg = new HashMap<>();
		// 서버에서 권한비교 한번더 해주기
		if(!CommonUtil.hasRole(authentication, "ROLE_NOTICE_WRITER")) {
			msg.put("msg", "권한이 없습니다!");
			return ResponseEntity.status(HttpStatus.FORBIDDEN).body(msg);
		}
		
		String empId = authentication.getName();
		
		if(bindingResult.hasErrors()) {
			msg.put("msg", "공지사항 수정에 실패했습니다");
			return ResponseEntity.status(HttpStatus.BAD_REQUEST).body(msg);
		}
		
		try {
			// 공지 수정 수행
			noticeService.modifyNotice(noticeDTO, noticeFiles, empId);
			msg.put("msg", "공지사항이 수정되었습니다.");
			return ResponseEntity.ok(msg);
			
		} catch (Exception e) {
			msg.put("msg", "공지사항 수정에 실패했습니다 :" + e.getMessage());
			return ResponseEntity.status(HttpStatus.BAD_REQUEST).body(msg);
		}
	}
	
	//공지사항 삭제 로직
	@DeleteMapping("/{noticeId}")
	public ResponseEntity<Map<String, String>> notices( @PathVariable("noticeId")Long noticeId
			, Authentication authentication) {
		Map<String, String> msg = new HashMap<>();
		// 서버에서 권한비교 한번더 해주기
		if(!CommonUtil.hasRole(authentication, "ROLE_NOTICE_WRITER")) {
			msg.put("msg", "권한이 없습니다!");
			return ResponseEntity.status(HttpStatus.FORBIDDEN).body(msg);
		}
		String empId = authentication.getName();
		
		try {
			// 공지 삭제 수행
			noticeService.deleteNotice(noticeId, empId);
			msg.put("msg", "공지사항이 삭제되었습니다.");
			return ResponseEntity.ok(msg);
			
		} catch (Exception e) {
			System.out.println(e);
			msg.put("msg", "공지사항 삭제에 실패했습니다 :" + e.getMessage());
			return ResponseEntity.status(HttpStatus.BAD_REQUEST).body(msg);
		}
	}
}























