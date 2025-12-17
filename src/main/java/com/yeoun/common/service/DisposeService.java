package com.yeoun.common.service;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.List;

import org.springframework.stereotype.Service;

import com.yeoun.common.dto.DisposeDTO;
import com.yeoun.common.dto.DisposeListDTO;
import com.yeoun.common.entity.Dispose;
import com.yeoun.common.mapper.DisposeMapper;
import com.yeoun.common.repository.DisposeRepository;

import jakarta.transaction.Transactional;
import lombok.RequiredArgsConstructor;

@Service
@RequiredArgsConstructor
public class DisposeService {
	private final DisposeRepository disposeRepository;
	private final DisposeMapper disposeMapper;

	// 폐기 등록
	@Transactional
	public void registDispose(DisposeDTO disposeDTO) {
		Dispose dispose = disposeDTO.toEntity();
		
		disposeRepository.save(dispose);
	}

	public List<DisposeListDTO> getDisposeList(DisposeListDTO requestBody) {
        LocalDateTime startDateTime = null;
        LocalDateTime endDateTime = null;
        String workType = null;
        String searchKeyword = null;

        if (requestBody.getStartDate() != null) {
            startDateTime = requestBody.getStartDate().atStartOfDay();
        }
        if (requestBody.getEndDate() != null) {
            endDateTime = requestBody.getEndDate().atTime(23, 59, 59);
        }
        if (requestBody.getWorkType() != null && !requestBody.getWorkType().isBlank()) {
            workType = requestBody.getWorkType();
        }
        if (requestBody.getSearchKeyword() != null && !requestBody.getSearchKeyword().isBlank()) {
            searchKeyword = "%" + requestBody.getSearchKeyword().trim().toLowerCase() + "%";
        }
	    
        return disposeMapper.searchDisposeList(startDateTime, endDateTime, workType, searchKeyword);
    }
}
