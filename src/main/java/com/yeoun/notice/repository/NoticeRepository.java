package com.yeoun.notice.repository;

import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import com.yeoun.notice.entity.Notice;

@Repository
public interface NoticeRepository extends JpaRepository<Notice, Long> {
	// 검색어가있을때 리스트 불러오기
	@Query("""
			SELECT n FROM Notice n
			WHERE n.deleteYN = 'N'
			AND (
				:searchKeyword is null
				or :searchKeyword = '' 
				or n.noticeTitle like concat('%', :searchKeyword, '%')
				or n.noticeContent like concat('%', :searchKeyword, '%')
			)
			""")
	Page<Notice> searchNotice(@Param("searchKeyword")String searchKeyword, Pageable pageable);
	
	// 검색어가 없을때 리스트불러오기
	Page<Notice> findByDeleteYN(String yn, Pageable pageable);
	

}
