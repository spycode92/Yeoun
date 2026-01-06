package com.yeoun.approval.mapper;

import java.util.List;
import java.util.Map;

import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Param;
import org.springframework.web.bind.annotation.RequestParam;

import com.yeoun.approval.dto.ApprovalDocDTO;
import com.yeoun.approval.dto.ApprovalDocGridDTO;
import com.yeoun.approval.dto.ApprovalFormDTO;

@Mapper
public interface ApprovalDocMapper {
	//그리드1. 결재사항 검색 - 진행해야할 결재만
	List<ApprovalDocGridDTO> searchApprovalItems(Map<String, Object> params);
	//그리드2. 전체결재 검색
	List<ApprovalDocGridDTO> searchAllApproval(Map<String, Object> params);
	//그리드3. 내결재목록 검색
	List<ApprovalDocGridDTO> searchMyApprovalList(Map<String, Object> params);
	//그리드4. 결재대기 검색
	List<ApprovalDocGridDTO> searchPendingApproval(Map<String, Object> params);
	//그리드5. 결재완료 검색
	List<ApprovalDocGridDTO> searchCompletedApproval(Map<String, Object> params);
}
