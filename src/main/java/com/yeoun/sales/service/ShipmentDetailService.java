package com.yeoun.sales.service;

import com.yeoun.sales.dto.ShipmentDetailDTO;
import com.yeoun.sales.dto.ShipmentDetailItemDTO;
import com.yeoun.sales.repository.ShipmentDetailQueryRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;

import java.util.List;

@Service
@RequiredArgsConstructor
public class ShipmentDetailService {

    private final ShipmentDetailQueryRepository detailRepo;

    public ShipmentDetailDTO getDetail(String orderId) {

        // 헤더 조회
        Object[] h = detailRepo.findHeader(orderId);

        ShipmentDetailDTO dto = ShipmentDetailDTO.builder()
                .orderId((String) h[0])
                .clientName((String) h[1])
                .dueDate((String) h[2])
                .status((String) h[3])
                .build();

        // 품목 리스트 조회
        List<ShipmentDetailItemDTO> items = detailRepo.findItems(orderId);
        dto.setItems(items);

        return dto;
    }
}
