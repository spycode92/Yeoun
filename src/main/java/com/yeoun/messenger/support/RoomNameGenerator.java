package com.yeoun.messenger.support;

import com.yeoun.messenger.dto.RoomMemberDTO;
import com.yeoun.messenger.entity.MsgRoom;
import com.yeoun.messenger.repository.MsgRoomRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Component;

import java.util.List;

@Component
@RequiredArgsConstructor
public class RoomNameGenerator {

    private final MsgRoomRepository msgRoomRepository;

    // ========================================================
    // 렌더링할 방 이름 정하기
    public String create(Long roomId, String name, List<RoomMemberDTO> members) {

        MsgRoom room = msgRoomRepository.findById(roomId).get();
        String groupName = room.getGroupName();
        String groupYn = room.getGroupYn();

        // 방 이름이 있는 경우
        if (groupName != null && !groupName.isBlank()) {
            return groupName;
        }

        // 방 이름이 없고, 그룹채팅(Y)인 경우
        else if ("Y".equals(groupYn)) {
            return members.get(0).getEmpName() + "의 그룹채팅(" + members.toArray().length + ")";
        }

        // 방 이름 없고, 1:1인 경우
        RoomMemberDTO opponent = members.stream()
                .filter(m -> !m.getEmpId().equals(name))
                .findFirst()
                .orElse(null);

        if (opponent != null) {
            return opponent.getEmpName() + " " + opponent.getPosition();
        }

        return "알 수 없음";
    }

}
