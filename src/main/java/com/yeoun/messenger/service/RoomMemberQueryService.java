package com.yeoun.messenger.service;

import com.yeoun.emp.entity.Dept;
import com.yeoun.emp.entity.Emp;
import com.yeoun.emp.entity.Position;
import com.yeoun.emp.repository.DeptRepository;
import com.yeoun.emp.repository.EmpRepository;
import com.yeoun.emp.repository.PositionRepository;
import com.yeoun.messenger.dto.RoomMemberDTO;
import com.yeoun.messenger.entity.MsgRelation;
import com.yeoun.messenger.entity.MsgStatus;
import com.yeoun.messenger.repository.MsgRelationRepository;
import com.yeoun.messenger.repository.MsgStatusRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.log4j.Log4j2;
import org.springframework.stereotype.Service;

import java.util.List;

@Service
@RequiredArgsConstructor
@Log4j2
public class RoomMemberQueryService {

    private final MsgRelationRepository msgRelationRepository;
    private final EmpRepository empRepository;
    private final PositionRepository positionRepository;
    private final DeptRepository deptRepository;
    private final MsgStatusRepository msgStatusRepository;

    // ========================================================
    // 소속 멤버 가져오기
    public List<RoomMemberDTO> getMembers(Long roomId){
        List<MsgRelation> relationList = msgRelationRepository.findByRoomId_RoomId(roomId);
        List<RoomMemberDTO> memberList = relationList.stream()
                .map(relation -> buildRoomMember(relation.getEmpId().getEmpId()))
                .toList();

        return memberList;
    }

    // ========================================================
    // 방 내 인원 정보 조회
    public RoomMemberDTO buildRoomMember (String empId) {
        Emp emp = empRepository.findById(empId)
                .orElseThrow(() -> new RuntimeException("사용자 없음"));

        MsgStatus msgStatus = msgStatusRepository.findById(empId)
                .orElseThrow(() -> new RuntimeException("사용자 프로필 없음"));

        String posName = positionRepository.findById(emp.getPosition().getPosCode())
                .map(Position::getPosName).orElse("미정");

        String deptName = deptRepository.findById(emp.getDept().getDeptId())
                .map(Dept::getDeptName).orElse("미정");

        return RoomMemberDTO.of(emp, msgStatus, posName, deptName);
    }
}
