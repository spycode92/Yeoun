package com.yeoun.common.repository;


import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import com.yeoun.common.entity.Dispose;

@Repository
public interface DisposeRepository extends JpaRepository<Dispose, Long> {
	
}
