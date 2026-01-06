package com.yeoun.common.wrapper;

import com.yeoun.common.util.FileUtil.FileUploadHelpper;

public class FileAttachWrapper implements FileUploadHelpper {
    
    // 파일이 참조하는 문서의 ID (결재 문서 ID)
    private final Long refId;
    
    // 파일이 참조하는 테이블명 (예: APPROVAL_DOC)
    private final String refTable; 
    
    /**
     * 생성자: 참조 ID와 테이블명을 주입받습니다.
     * @param refId 참조 ID
     * @param refTable 참조 테이블명
     */
    public FileAttachWrapper(Long refId, String refTable) {
        this.refId = refId;
        this.refTable = refTable;
    }

    // ⭐ FileUploadHelpper 인터페이스 구현부 ⭐

    /**
     * FileUtil이 파일 메타데이터에 기록할 테이블명을 반환합니다.
     */
    @Override
    public String getTargetTable() {
        return refTable;
    }

    /**
     * FileUtil이 파일 메타데이터에 기록할 참조 ID(PK)를 반환합니다.
     */
    @Override
    public Long getTargetTableId() {
        // Long 타입의 ID를 Object로 반환
        return refId;
    }
    
    /* * 주의: FileUploadHelpper에 List<MultipartFile> getFiles()와 같은 다른 메서드가 
     * 정의되어 있다면, 해당 메서드들도 여기에 반드시 구현해야 컴파일 오류가 발생하지 않습니다.
     */
}
