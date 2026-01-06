package com.yeoun.emp.service;

import java.util.List;

import org.springframework.stereotype.Service;

import com.yeoun.emp.entity.Dept;
import com.yeoun.emp.repository.DeptRepository;

import lombok.RequiredArgsConstructor;

@Service
@RequiredArgsConstructor
public class DeptService {
	
	private final DeptRepository deptRepository;
	
	// 본부 리스트 (ERP / MES 등)
    public List<Dept> getTopDeptList() {
        return deptRepository.findByParentDeptIdAndUseYn("DEP999", "Y");
    }

    // 하위 부서 리스트 (개발부, 생산부, 인사부 등)
    public List<Dept> getSubDeptList() {
        return deptRepository.findByParentDeptIdIsNotNullAndParentDeptIdNotAndUseYn("DEP999", "Y");
    }

}
