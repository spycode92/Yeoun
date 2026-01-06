package com.yeoun.common.entity;

import java.time.LocalDate;

import org.springframework.data.annotation.CreatedDate;
import org.springframework.data.jpa.domain.support.AuditingEntityListener;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.EntityListeners;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.SequenceGenerator;
import jakarta.persistence.Table;
import lombok.Getter;
import lombok.Setter;

@Entity
@Table(name = "FILE_ATTACH")
@SequenceGenerator(
		name = "FILE_ATTACH_SEQ_GENERATOR",
		sequenceName = "FILE_ATTACH_SEQ", 
		initialValue = 1,
		allocationSize = 1
)
@Getter
@Setter
@EntityListeners(AuditingEntityListener.class)
public class FileAttach {
	
	// 파일ID
	@Id
	@GeneratedValue(strategy = GenerationType.SEQUENCE, generator = "FILE_ATTACH_SEQ_GENERATOR")
	@Column(name = "FILE_ID")
	private Long fileId;
	
	// 참조테이블
	@Column(name = "REF_TABLE", length = 30, nullable = false)
	private String refTable;
	
	// 참조ID
	@Column(name = "REF_ID", nullable = false)
	private Long refId;
	
	// 카테고리
	@Column(name = "CATEGORY", length = 255)
	private String category;
	
	// 저장 파일명
	@Column(name = "FILE_NAME", length = 255, nullable = false)
	private String fileName;
	
	// 원본 파일명
	 @Column(name = "ORIGIN_FILE_NAME", length = 255, nullable = false)
	private String originFileName;
	
	// 파일 경로
	@Column(name = "FILE_PATH", length = 400, nullable = false)
	private String filePath;
	
	// 파일 크기
	@Column(name = "FILE_SIZE", nullable = false)
	private Long fileSize;
	
	// 생성일자
	@CreatedDate
	@Column(name = "CREATED_DATE", updatable = false)
	private LocalDate createdDate;

}
