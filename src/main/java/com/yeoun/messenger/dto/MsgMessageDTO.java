package com.yeoun.messenger.dto;

import com.yeoun.common.dto.FileAttachDTO;
import com.yeoun.emp.entity.Emp;
import com.yeoun.messenger.entity.MsgMessage;
import com.yeoun.messenger.entity.MsgRoom;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;
import lombok.ToString;
import org.modelmapper.ModelMapper;

import java.time.LocalDateTime;
import java.util.List;

@Getter
@Setter
@NoArgsConstructor
@ToString
public class MsgMessageDTO {

    private Long msgId;             // 메시지 ID
    private Long roomId;            // 채팅방 ID
    private String senderId;        // 보낸사람 ID
    private String msgContent;      // 메시지 내용
    private String msgType;         // 메시지 타입
    private String fileId;          // 파일 ID
    private LocalDateTime sentDate; // 메시지 전송시간
    private String remark;          // 비고

    private String senderName;      // 보낸사람 이름 (추가필드)
    private Integer senderProfile;  // 보낸사람 프로필 (추가필드)
    private String sentDateFormatted;	// 전송시간 포맷 (추가필드)

    private List<FileAttachDTO> files; // 추가필드

    // ===========================================================
    // DTO <-> Entity 변환 메서드 구현

    private static ModelMapper modelMapper = new ModelMapper();

    public MsgMessage toEntity(MsgRoom room, Emp sender) {
        MsgMessage entity = modelMapper.map(this, MsgMessage.class);

        // ID 기반 엔티티는 수동으로 매핑해야 한다
        entity.setRoomId(room);
        entity.setSenderId(sender);

        return entity;
    }

    public static MsgMessageDTO fromEntity(MsgMessage msg, List<FileAttachDTO> files) {
        MsgMessageDTO dto = new MsgMessageDTO();

        dto.setMsgId(msg.getMsgId());
        dto.setRoomId(msg.getRoomId().getRoomId());
        dto.setSenderId(msg.getSenderId().getEmpId());
        dto.setSenderName(msg.getSenderId().getEmpName());
        dto.setSentDate(msg.getSentDate());
        dto.setMsgType(msg.getMsgType());
        dto.setMsgContent(msg.getMsgContent());

        dto.setFiles(files);

        return dto;
    }


}
