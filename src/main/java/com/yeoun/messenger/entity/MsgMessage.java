package com.yeoun.messenger.entity;

import com.yeoun.common.util.FileUtil.FileUploadHelpper;
import com.yeoun.emp.entity.Emp;
import jakarta.persistence.*;
import lombok.Builder;
import lombok.Getter;
import lombok.Setter;
import org.springframework.data.annotation.CreatedDate;
import org.springframework.data.jpa.domain.support.AuditingEntityListener;

import java.time.LocalDateTime;

@Entity
@Table(name = "MSG_MESSAGE")
@Getter
@Setter
@EntityListeners(AuditingEntityListener.class)
public class MsgMessage implements FileUploadHelpper {

    // 메시지 ID
    @Id
    @GeneratedValue(strategy = GenerationType.SEQUENCE, generator = "MSG_SEQ")
    @SequenceGenerator(
            name = "MSG_SEQ",
            sequenceName = "MSG_SEQ",
            allocationSize = 1
    )
    private long msgId;

    // 채팅방 ID
    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "ROOM_ID", nullable = false)
    private MsgRoom roomId;

    // 보낸사람 ID
    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "SENDER_ID", nullable = false)
    private Emp senderId;

    // 메시지 내용
    @Lob
    @Column
    private String msgContent;

    // 메시지 타입 (EX: TEXT, PHOTO)
    @Column(nullable = false, length = 20)
    private String msgType;

    // 파일 ID => 나중에 fk
    @Column(length = 255)
    private String fileId;

    // 미리보기 url		===============> 컬럼 삭제
    //@Column(length = 500)
    //private String thumbUrl;

    // 메시지 전송시간
    @CreatedDate
    @Column(nullable = false)
    private LocalDateTime sentDate;
    
    // 비고
    @Column
    private String remark;
    
    // 기본 생성자 추가
    protected MsgMessage() {}

	@Override
	public String getTargetTable() {
		return "MSG_MESSAGE";
	}

	@Override
	public Long getTargetTableId() {
		return msgId;
	}

    @Builder
    public MsgMessage(MsgRoom room, Emp sender, String msgContent, String msgType) {
        this.roomId = room;
        this.senderId = sender;
        this.msgContent = msgContent;
        this.msgType = msgType;
    }



}



