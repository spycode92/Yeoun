package com.yeoun.messenger.dto;

import com.yeoun.common.dto.FileAttachDTO;
import com.yeoun.messenger.entity.MsgMessage;
import lombok.AllArgsConstructor;
import lombok.Getter;

import java.util.List;

@Getter
@AllArgsConstructor
public class MessageSaveResult {
    private MsgMessage message;
    private List<FileAttachDTO> files;
}
