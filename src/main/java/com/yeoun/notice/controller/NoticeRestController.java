package com.yeoun.notice.controller;

import java.util.List;

import org.springframework.data.domain.Page;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import com.yeoun.common.dto.FileAttachDTO;
import com.yeoun.notice.dto.NoticeDTO;
import com.yeoun.notice.service.NoticeService;

import lombok.RequiredArgsConstructor;

@RequiredArgsConstructor
@RestController
@RequestMapping("/api/notices")
public class NoticeRestController {
	
    private final NoticeService noticeService;
    // 공지사항 조회
    @GetMapping("/{noticeId}")
    public ResponseEntity<NoticeDTO> getNotice(@PathVariable("noticeId")Long noticeId) {
        NoticeDTO noticeDTO = noticeService.getOneNotice(noticeId);
        
        if (noticeDTO == null) { //찾는 공지사항이 없을때
            return ResponseEntity.notFound().build();
        }
        
        return ResponseEntity.ok(noticeDTO);
    }
    
    //최근 공지사항 조회
    @GetMapping("/last-notice")
    public ResponseEntity<List<NoticeDTO>> getLastNotice(){
    	Page<NoticeDTO> noticePage = noticeService.getLastNotice(0, 5);
    	
        if (noticePage.getContent() == null) { //찾는 공지사항이 없을때
            return ResponseEntity.notFound().build();
        }
        
        return ResponseEntity.ok(noticePage.getContent());
    }
    
    // 공지조회시 파일데이터 조회
    @GetMapping("/file/{noticeId}")
    public ResponseEntity<List<FileAttachDTO>> getNoticeFile(@PathVariable("noticeId")Long noticeId){
    	List<FileAttachDTO> fileDTOList = noticeService.getNoticeFiles(noticeId);
        
        return ResponseEntity.ok(fileDTOList);
    }
}
