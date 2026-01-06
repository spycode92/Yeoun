package com.yeoun.messenger.dto;

import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;
import lombok.ToString;

@Getter
@Setter
@NoArgsConstructor
@ToString
public class MsgSendRequest {
    private Long roomId;
    private String senderId;
    private String msgContent;
    private String msgType;
}
