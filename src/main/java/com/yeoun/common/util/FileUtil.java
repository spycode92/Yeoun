package com.yeoun.common.util;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.time.LocalDate;
import java.time.format.DateTimeFormatter;
import java.util.ArrayList;
import java.util.List;
import java.util.UUID;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;
import org.springframework.web.multipart.MultipartFile;

import com.yeoun.common.dto.FileAttachDTO;
import com.yeoun.common.entity.FileAttach;

import lombok.extern.log4j.Log4j2;


@Component
@Log4j2
public class FileUtil {
	// 파일 업로드에 사용할 경로를 properties 파일에서 가져오기
	// => 변수 선언부에 @Value("${프로퍼티속성명}") 형태로 선언
	@Value("${file.uploadBaseLocation}")
	private String uploadBaseLocation;
	
	// ======================================================================
	// 파일업로드 인터페이스
	public interface FileUploadHelpper{
		String getTargetTable();
		Long getTargetTableId();
	}
	// ======================================================================
	// ======================================================================
	// 파일 업로드 후 List<FileAttachDTO> 반환
	public <T extends FileUploadHelpper> List<FileAttachDTO> uploadFile(T entity,List<MultipartFile> files) throws IOException {
		// ItemImg 엔티티 목록을 저장할 List<ItemImg> 객체 생성
		List<FileAttach> FileList = new ArrayList<>();
		
		// [ 파일 저장될 디렉토리 생성 ]
		LocalDate today = LocalDate.now(); 
		DateTimeFormatter dtf = DateTimeFormatter.ofPattern("yyyy/MM/dd"); 
		String subDir = today.format(dtf);
		
		// 파일 저장 경로에 대한 Path 객체 생성하고 해당 경로를 실제 서버상에 생성
		Path uploadDir = Paths.get(uploadBaseLocation, subDir).toAbsolutePath().normalize();
		
		// 생성된 Path 객체에 해당하는 디렉토리가 실제 서버상에 존재하지 않을 경우 새로 생성
		if(!Files.exists(uploadDir)) {
			Files.createDirectories(uploadDir); // 하위 경로를 포함한 경로 상의 모든 디렉토리 생성
		}
		// =======================================================================================
		// 업로드한 파일정보를 저장할 객체 생성
		List<FileAttachDTO> fileList = new ArrayList<FileAttachDTO>();
		// FileUploadHelpper를 상속한 엔티티의 getFiles()메서드의 리턴값 MultipartFile[]배열값 반복하여 파일 업로드
		for(MultipartFile mFile : files) {
			if(!mFile.isEmpty()) { // 파일이 존재할 때만 업로드 실행
				String originalFileName = mFile.getOriginalFilename(); //파일의 원본이름
				
				String uuid = UUID.randomUUID().toString();
				String realFileName = uuid + "_" + originalFileName; // 실제 저장되는 파일이름
				
				Path destinationPath = uploadDir.resolve(realFileName); //resolve메서드를 통해 경로 + 파일이름
				
				mFile.transferTo(destinationPath); //transferTo(Path) 메서드로 파일업로드
				
				// 파일DB 저장을위해 fileDTO에 파일정보 저장
				FileAttachDTO fileDTO = FileAttachDTO.builder()
										.refTable(entity.getTargetTable())
										.refId(Long.parseLong(entity.getTargetTableId().toString()))
										.category(mFile.getContentType())
										.fileName(realFileName)
										.originFileName(originalFileName)
										.filePath(subDir)
										.fileSize(mFile.getSize())
										.build();
				//fileList에 fileDTO 정보 추가
				fileList.add(fileDTO);
			}
		}
		// 업로드된 파일리스트 반환
		return fileList;
	}
	
	// ======================================================================
	// 실제 서버상에 업로드 된 파일 제거(단일 파일 삭제)
	public void deleteFile(FileAttachDTO fileAttachDTO) {
		// 파일기본경로 + 저장경로 + 저장된 파일명 결합
		Path path = Paths.get(uploadBaseLocation, fileAttachDTO.getFilePath())
						.resolve(fileAttachDTO.getFileName())
						.normalize();
		// Files 클래스의 deleteIfExists() 메서드 호출하여 해당 파일이 서버상에 존재할 경우 삭제 처리
		try {
			Files.deleteIfExists(path);
		} catch (IOException e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		}
	}

	// 실제 서버상에 업로드 된 파일 제거(다중 파일 삭제)
	public void deleteFiles(List<FileAttachDTO> fileAttachDTOList) {
		for(FileAttachDTO fileAttachDTO : fileAttachDTOList) {
			deleteFile(fileAttachDTO);
		}
	}

	// Base64 문자열을 파일로 저장 (Base64 디코딩 포함)
	public FileAttachDTO saveFileFromBase64(String base64Image, String originalFileName, String category,
			String refTable, Long refId) throws IOException {
		if (base64Image == null || base64Image.isEmpty()) {
			return null;
		}

		// Base64 헤더 제거 (data:image/png;base64,...)
		String[] parts = base64Image.split(",");
		String imageString = parts.length > 1 ? parts[1] : parts[0];

		byte[] fileData = java.util.Base64.getDecoder().decode(imageString);

		return saveFileFromBytes(fileData, originalFileName, category, refTable, refId);
	}

	// Base64 문자열(또는 byte[])을 파일로 저장
	public FileAttachDTO saveFileFromBytes(byte[] fileData, String originalFileName, String category, String refTable,
			Long refId) throws IOException {
		if (uploadBaseLocation == null || uploadBaseLocation.trim().isEmpty()) {
			throw new IOException("파일 업로드 경로(uploadBaseLocation)가 설정되지 않았습니다.");
		}

		// [ 파일 저장될 디렉토리 생성 ]
		LocalDate today = LocalDate.now();
		DateTimeFormatter dtf = DateTimeFormatter.ofPattern("yyyy/MM/dd");
		String subDir = today.format(dtf);

		Path uploadDir = Paths.get(uploadBaseLocation, subDir).toAbsolutePath().normalize();

		if (!Files.exists(uploadDir)) {
			Files.createDirectories(uploadDir);
		}

		String uuid = UUID.randomUUID().toString();
		String realFileName = uuid + "_" + originalFileName;

		Path destinationPath = uploadDir.resolve(realFileName);

		Files.write(destinationPath, fileData);

		return FileAttachDTO.builder()
				.refTable(refTable)
				.refId(refId)
				.category(category)
				.fileName(realFileName)
				.originFileName(originalFileName)
				.filePath(subDir)
				.fileSize((long) fileData.length)
				.build();
	}
}
