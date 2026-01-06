package com.yeoun.messenger.dto;

import com.yeoun.common.dto.FileAttachDTO;
import lombok.Builder;
import lombok.Getter;
import lombok.Setter;
import lombok.ToString;

import java.util.List;

@Getter
@Setter
@Builder
@ToString
public class MessageEventDTO {
    private Long msgId;                 // 메시지 ID
    private Long roomId;                // 방 ID
    private String senderId;            // 보낸사람 ID
    private String senderName;          // 보낸사람 이름
    private Integer senderProfile;      // 프로필 이미지 번호
    private String msgContent;          // 메시지 내용
    private String msgType;             // 메시지 타입 (ex. TEXT/IMAGE/SYSTEM)
    private int fileCount;              // 파일 갯수
    private List<FileAttachDTO> files;  // 파일 구성 정보
    private String preview;             // 미리보기에 표시될 내용
    private String sentTime;            // yyyy-MM-dd HH:mm
}
