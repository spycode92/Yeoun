package com.yeoun.common.service;

import org.springframework.stereotype.Service;

import com.yeoun.common.dto.DisposeDTO;
import com.yeoun.common.entity.Dispose;
import com.yeoun.common.repository.DisposeRepository;

import jakarta.transaction.Transactional;
import lombok.RequiredArgsConstructor;

@Service
@RequiredArgsConstructor
public class DisposeService {
	private final DisposeRepository disposeRepository;

	// 폐기 등록
	@Transactional
	public void registDispose(DisposeDTO disposeDTO) {
		Dispose dispose = disposeDTO.toEntity();
		
		disposeRepository.save(dispose);
	}
}
