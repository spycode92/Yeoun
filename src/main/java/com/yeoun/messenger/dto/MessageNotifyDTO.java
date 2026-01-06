package com.yeoun.messenger.dto;

import lombok.Builder;
import lombok.Getter;
import lombok.Setter;
import lombok.ToString;

@Getter
@Setter
@Builder
@ToString
public class MessageNotifyDTO {
	private Long roomId;				// 메시지 ID
	private String groupName;			// 그룹채팅방일 시 이름
	private String preview;				// 미리보기 메시지 (밖에서 보일 메시지)
	private String senderId;			// 보낸사람 ID (혹시 몰라서)
	private String senderName;			// 보낸사람 이름 (토스트 알림에 사용 예정)
	private String sentTime;			// 메시지 보낸 시간
	private Integer unreadCount;		// 읽지 않은 메시지 갯수
	private Integer profileImg;			// 프로필이미지 번호
}
