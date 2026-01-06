package com.yeoun.messenger.dto;

import groovy.transform.ToString;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

@Getter
@Setter
@NoArgsConstructor
@ToString
public class MsgReadRequest {
	private String ReaderId;
	private Long RoomId;
	private Long MsgId;
	private String groupYn;
}
