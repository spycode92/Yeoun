package com.yeoun.emp.repository;

import java.util.List;

import org.springframework.data.jpa.repository.Query;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import com.yeoun.emp.entity.Dept;

@Repository
public interface DeptRepository extends JpaRepository<Dept, String> {
	
	// 활성화된 부서 목록 조회
	// => USE_YN = 'Y'인 부서 / 부서명 오름차순 정렬
	@Query("SELECT d FROM Dept d"
			+ " WHERE d.useYn = 'Y'"
			+ " ORDER BY d.deptName ASC")
	List<Dept> findActive();
	
	// 1) 루트(대표) 밑의 본부들만 조회  -> ERP본부, MES본부 (USE_YN = 'Y')
    List<Dept> findByParentDeptIdAndUseYn(String parentDeptId, String useYn);

    // 2) 실제 부서들(본부 제외) 전체 조회
    //    조건: parentDeptId IS NOT NULL AND parentDeptId <> :parentDeptId AND useYn = :useYn
    List<Dept> findByParentDeptIdIsNotNullAndParentDeptIdNotAndUseYn(String parentDeptId, String useYn);

}
