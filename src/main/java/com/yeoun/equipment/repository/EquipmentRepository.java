package com.yeoun.equipment.repository;

import com.yeoun.equipment.entity.Equipment;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;

public interface EquipmentRepository extends JpaRepository<Equipment, String> {

    // Y혹은 N인것만 찾기
    List<Equipment> findByUseYn(String useYn);
}
