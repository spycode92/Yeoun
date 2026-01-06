package com.yeoun.messenger.dto;

import com.yeoun.emp.entity.Emp;
import com.yeoun.messenger.entity.MsgMessage;
import com.yeoun.messenger.entity.MsgRelation;
import com.yeoun.messenger.entity.MsgRoom;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;
import lombok.ToString;

import java.time.LocalDateTime;

import org.modelmapper.ModelMapper;

@Getter
@Setter
@NoArgsConstructor
@ToString
public class MsgRelationDTO {

    private long relationId;            // 관계 고유 ID
    private MsgRoom roomId;             // 채팅방 ID
    private Emp empId;                  // 참여자 ID
    private MsgMessage lastReadId;      // 마지막 조회 메시지 ID
    private String pinnedYn;            // 채팅방 고정 여부
    private String participantYn;       // 채팅방 참여 상태
    private LocalDateTime joinDate;     // 마지막 입장 일시
    private LocalDateTime activeDate;   // 마지막 확인 시간
    private LocalDateTime pinnedDate;   // 채팅방 고정 시간
    private String remark;              // 비고
    
    // ===========================================================
  	// DTO <-> Entity 변환 메서드 구현
  		
  	private static ModelMapper modelMapper = new ModelMapper();
  	
  	public MsgRelation toEntity(MsgRoom roomId, Emp empId) {

  		MsgRelation msgRelation = new MsgRelation();
 		msgRelation.setRoomId(roomId);
 		msgRelation.setEmpId(empId);
 		
 		return msgRelation;
 	}
 	
 	public static MsgRelationDTO fromEntity(MsgRelation msgRelation) {
 		return modelMapper.map(msgRelation, MsgRelationDTO.class);
 	}
 	


}
