package com.yeoun.sales.service;

import com.yeoun.sales.entity.Client;
import com.yeoun.sales.repository.ClientRepository;

import jakarta.transaction.Transactional;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;

import java.time.LocalDateTime;
import java.util.List;

@Service
@RequiredArgsConstructor
public class ClientService {

    private final ClientRepository clientRepository;

    /** 전체 목록 */
    public List<Client> findAll() {
        return clientRepository.findAll();
    }

    /** 검색 */
    public List<Client> search(String name, String type) {
        if ((name == null || name.isBlank()) && (type == null || type.isBlank()))
            return clientRepository.findAll();
        return clientRepository.search(name, type);
    }

    /** 상세조회 */
    public Client get(String clientId) {
        return clientRepository.findById(clientId).orElse(null);
    }

    /** ===============================
     *   신규 등록 (필수값+중복검증 포함)
     * =============================== */
    public Client create(Client client) {

        /* 1. 필수값 검증 */
        if (client.getClientType() == null || client.getClientType().isBlank())
            throw new IllegalArgumentException("유형은 필수입니다.");

        if (client.getClientName() == null || client.getClientName().isBlank())
            throw new IllegalArgumentException("거래처명은 필수입니다.");

        if (client.getBusinessNo() == null || client.getBusinessNo().isBlank())
            throw new IllegalArgumentException("사업자번호는 필수입니다.");


        /* ⭐⭐ 2. 사업자번호 숫자만 남기기 */
        String cleanBizNo = client.getBusinessNo().replaceAll("[^0-9]", "");

        if (clientRepository.existsBizNoClean(cleanBizNo)) {
            throw new IllegalArgumentException("이미 등록된 사업자번호입니다.");
        }

        /* 4. 거래처 ID 자동 생성 */
        client.setClientId(generateClientId(client.getClientType()));

        /* 5. 기본 상태 */
        client.setStatusCode("ACTIVE");

        /* 6. 생성일시 */
        client.setCreatedAt(LocalDateTime.now());
        client.setCreatedBy("SYSTEM");

        /* 기본값 보정 */
        if (client.getFaxNumber() == null) client.setFaxNumber("");
        if (client.getManagerTel() == null) client.setManagerTel("");
        if (client.getAccountName() == null) client.setAccountName("");

        return clientRepository.save(client);
    }


    /** ===========================
     *   수정
     * =========================== */
    public Client update(String clientId, Client updateForm) {

        Client origin = clientRepository.findById(clientId)
                .orElseThrow(() -> new IllegalArgumentException("거래처 없음"));

        // 필드 갱신
        origin.setClientName(updateForm.getClientName());
        origin.setClientType(updateForm.getClientType());
        origin.setBusinessNo(updateForm.getBusinessNo());
        origin.setCeoName(updateForm.getCeoName());
        origin.setManagerName(updateForm.getManagerName());
        origin.setManagerDept(updateForm.getManagerDept());
        origin.setManagerTel(updateForm.getManagerTel());
        origin.setManagerEmail(updateForm.getManagerEmail());
        origin.setPostCode(updateForm.getPostCode());
        origin.setAddr(updateForm.getAddr());
        origin.setAddrDetail(updateForm.getAddrDetail());
        origin.setFaxNumber(updateForm.getFaxNumber());
        origin.setBankName(updateForm.getBankName());
        origin.setAccountName(updateForm.getAccountName());
        origin.setAccountNumber(updateForm.getAccountNumber());
        origin.setStatusCode(updateForm.getStatusCode());

        // 수정일/수정자
        origin.setUpdatedAt(LocalDateTime.now());
        origin.setUpdatedBy("SYSTEM");

        return clientRepository.save(origin);
    }


    /* ===========================
        ID 생성 규칙
        CUS20251202-0001
        VEN20251202-0001
       ===========================*/
    public String generateClientId(String type) {

        String prefix = type.equals("SUPPLIER") ? "VEN" : "CUS";  
        String date = java.time.LocalDate.now().toString().replace("-", "");
        String pattern = prefix + date + "-%";

        /* 오늘 날짜의 최대 seq 조회 */
        String maxId = clientRepository.findMaxClientId(pattern);

        int nextSeq = 1;

        if (maxId != null) {
            String seqStr = maxId.substring(maxId.lastIndexOf("-") + 1); // 0001
            nextSeq = Integer.parseInt(seqStr) + 1;
        }

        String seqStr = String.format("%04d", nextSeq);

        return prefix + date + "-" + seqStr;
    }

    /* 사업자번호 중복 체크 API용 */
    
    private String cleanBizNo(String no) {
        if (no == null) return null;
        return no.replaceAll("[^0-9]", "");  // 숫자만 남기기
    }
    
    public boolean existsByBusinessNoClean(String businessNo) {
        return clientRepository.existsBizNoClean(businessNo);
    }
    
    
 
 // 고객사 정보 수정
    @Transactional
    public void update(Client req) {
        Client c = clientRepository.findById(req.getClientId()).orElseThrow();
        
        c.setCeoName(req.getCeoName());
        c.setManagerName(req.getManagerName());
        c.setManagerDept(req.getManagerDept());
        c.setManagerTel(req.getManagerTel());
        c.setManagerEmail(req.getManagerEmail());
        c.setAddr(req.getAddr());
        c.setAddrDetail(req.getAddrDetail());
        
        c.setPostCode(req.getPostCode());   

        c.setBankName(req.getBankName());         
        c.setAccountNumber(req.getAccountNumber());
        c.setAccountName(req.getAccountName());

        c.setStatusCode(req.getStatusCode());
    }




}
