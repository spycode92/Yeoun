// ===============================
// 출하 상세 모달 전용 JS
// ===============================

let shipmentDetailModal = null;

// -------------------------------
// 1) 초기화 함수 (페이지 로드시 실행)
// -------------------------------
document.addEventListener("DOMContentLoaded", () => {

    const modalEl = document.getElementById("shipmentDetailModal");

    if (modalEl) {
        shipmentDetailModal = new bootstrap.Modal(modalEl);
    } else {
        console.error("❌ shipmentDetailModal 요소를 찾을 수 없습니다.");
    }
});


// -------------------------------
// 2) 상세 모달 오픈 함수 (외부에서 호출)
// -------------------------------
function openShipmentDetail(orderId) {

    if (!shipmentDetailModal) {
        alert("상세 모달이 준비되지 않았습니다.");
        return;
    }

    // 모달 내부 초기화
    document.getElementById("detailOrderId").textContent = orderId;
    document.getElementById("detailClientName").textContent = "-";
    document.getElementById("detailDueDate").textContent = "-";
    document.getElementById("detailStatusBadge").innerHTML = "";
    document.querySelector("#detailItemTable tbody").innerHTML = `
        <tr><td colspan="4" class="text-center text-muted py-3">로딩 중...</td></tr>
    `;

    // API 호출
    fetch(`/sales/shipment/detail?orderId=${encodeURIComponent(orderId)}`)
        .then(res => {
            if (!res.ok) throw new Error("상세 조회 실패");
            return res.json();
        })
        .then(detail => renderShipmentDetail(detail))
        .catch(err => {
            console.error("❌ 상세 조회 오류:", err);
            alert("상세 정보를 불러오는 중 문제가 발생했습니다.");
        });
}


// -------------------------------
// 3) 모달 렌더링 함수
// -------------------------------
function renderShipmentDetail(detail) {

    // 헤더 정보 렌더링
    document.getElementById("detailClientName").textContent = detail.clientName || "-";
    document.getElementById("detailDueDate").textContent = detail.dueDate || "-";
    document.getElementById("detailStatusBadge").innerHTML = renderShipmentStatusBadge(detail.status);

    // 품목 리스트 렌더링
    const tbody = document.querySelector("#detailItemTable tbody");
    tbody.innerHTML = "";

    if (!detail.items || detail.items.length === 0) {
        tbody.innerHTML = `
            <tr><td colspan="4" class="text-center py-3">품목 정보가 없습니다.</td></tr>
        `;
    } else {
        detail.items.forEach(item => {
            const tr = document.createElement("tr");
            tr.innerHTML = `
                <td>${item.prdName}</td>
                <td class="text-end">${item.orderQty}</td>
                <td class="text-end">${item.stockQty}</td>
                <td class="text-center">
                    ${item.reservable
                        ? '<span class="badge bg-success">가능</span>'
                        : '<span class="badge bg-secondary">불가</span>'}
                </td>
            `;
            tbody.appendChild(tr);
        });
    }

    shipmentDetailModal.show();
}


// -------------------------------
// 4) 상태 뱃지 렌더링
// -------------------------------
function renderShipmentStatusBadge(status) {
    switch (status) {
        case "SHIPPED":
            return '<span class="badge bg-success">출하완료</span>';
        case "RESERVED":
            return '<span class="badge bg-primary">예약</span>';
        case "LACK":
            return '<span class="badge bg-danger">부족</span>';
        default:
            return '<span class="badge bg-secondary">대기</span>';
    }
}
