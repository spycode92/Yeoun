// ===============================
// 출하 상세 모달 전용 JS (FIXED v3)
// ===============================

let shipmentDetailModal = null;

document.addEventListener("DOMContentLoaded", () => {
    const modalEl = document.getElementById("shipmentDetailModal");
    if (modalEl) {
        shipmentDetailModal = new bootstrap.Modal(modalEl);
    }
});

// -------------------------------
// 상세 모달 오픈
// -------------------------------
function openShipmentDetail(orderId, shipmentId, status) {

    if (!shipmentDetailModal) return;

    document.getElementById("detailOrderId").textContent = orderId;
    document.getElementById("detailClientName").textContent = "-";
    document.getElementById("detailDueDate").textContent = "-";
    document.getElementById("detailOutboundDate").textContent = "-";
    document.getElementById("detailProcessBy").textContent = "-";
    document.getElementById("detailStatusBadge").innerHTML =
        renderShipmentStatusBadge(status);

    const normalTbody = document.querySelector("#detailItemTable tbody");
    const completedTbody = document.getElementById("completedShipmentTbody");

    normalTbody.innerHTML =
        `<tr><td colspan="3" class="text-center text-muted">로딩 중...</td></tr>`;
    completedTbody.innerHTML = "";

    let url = `/sales/shipment/detail?orderId=${encodeURIComponent(orderId)}`;
    if (status === "SHIPPED" && shipmentId) {
        url += `&shipmentId=${encodeURIComponent(shipmentId)}`;
    }

    fetch(url)
        .then(res => res.json())
        .then(detail => renderShipmentDetail(detail))
        .catch(err => {
            console.error("❌ 상세 조회 오류:", err);
            alert("상세 정보를 불러오는 중 문제가 발생했습니다.");
        });
}

// -------------------------------
// 모달 렌더링
// -------------------------------
function renderShipmentDetail(detail) {

    console.log("📦 [DETAIL RAW]", detail);
    console.log("📦 status =", detail.status);
    console.log("📦 items =", detail.items);

    const isCompleted = detail.status === "SHIPPED";

    // 공통 헤더
    document.getElementById("detailClientName").textContent = detail.clientName || "-";
    document.getElementById("detailDueDate").textContent = detail.dueDate || "-";
    document.getElementById("detailStatusBadge").innerHTML =
        renderShipmentStatusBadge(detail.status);

    // 🔥 테이블/영역 토글 (이게 핵심)
    document.getElementById("completedShipmentInfo")
        .classList.toggle("d-none", !isCompleted);

    document.getElementById("completedShipmentTable")
        .classList.toggle("d-none", !isCompleted);

    document.getElementById("detailItemTable")
        .classList.toggle("d-none", isCompleted);

    // 🔥 "품목정보" 텍스트 토글 (출하완료 시 숨김)
    const itemInfoLabel = document.getElementById("itemInfoLabel");
    if (itemInfoLabel) {
        itemInfoLabel.classList.toggle("d-none", isCompleted);
    }

    const normalTbody = document.querySelector("#detailItemTable tbody");
    const completedTbody = document.getElementById("completedShipmentTbody");

    normalTbody.innerHTML = "";
    completedTbody.innerHTML = "";

    // =========================
    // 출하완료 → LOT 이력
    // =========================
    if (isCompleted) {
        console.log("🚚 출하완료 분기 진입");
		
		// 🔥 [추가된 부분] 운송장번호
		   document.getElementById("detailTrackingNumber").textContent =
		       detail.trackingNumber ?? "-";
        
        // 🔥 수정: items 대신 completedItems 사용
        const shipmentItems = detail.completedItems || [];
        console.log("🚚 completedItems length =", shipmentItems.length);

        // 🔥 출하일 포맷: yyyy-MM-dd HH:mm (초 제거)
        if (detail.outboundDate) {
            const formatted = detail.outboundDate.replace("T", " ").substring(0, 16);
            document.getElementById("detailOutboundDate").textContent = formatted;
        } else {
            document.getElementById("detailOutboundDate").textContent = "-";
        }

        document.getElementById("detailProcessBy").textContent =
            detail.processBy || "-";

        if (shipmentItems.length === 0) {
            console.warn("⚠️ 출하 이력 배열 비어있음");
            completedTbody.innerHTML = `
                <tr><td colspan="4" class="text-center">출하 이력이 없습니다.</td></tr>
            `;
        } else {
            shipmentItems.forEach(item => {
                const tr = document.createElement("tr");
                // 🔥 출하일 포맷: yyyy-MM-dd HH:mm (초 제거)
                const outboundDateFormatted = item.outboundDate 
                    ? item.outboundDate.replace("T", " ").substring(0, 16)
                    : "-";
                
                tr.innerHTML = `
                    <td>${item.prdName ?? "-"}</td>
                    <td>${item.lotNo ?? "-"}</td>
                    <td class="text-end">${item.outboundAmount ?? 0}</td>
                    <td class="text-center">${outboundDateFormatted}</td>
                `;
                completedTbody.appendChild(tr);
            });
        }
    }

    // =========================
    // 출하 전 → 수주 기준
    // =========================
    else {
        if (!detail.items || detail.items.length === 0) {
            normalTbody.innerHTML = `
                <tr><td colspan="3" class="text-center">품목 정보가 없습니다.</td></tr>
            `;
        } else {
            detail.items.forEach(item => {
                const tr = document.createElement("tr");
                tr.innerHTML = `
                    <td>${item.prdName}</td>
                    <td class="text-end">${item.orderQty}</td>
                    <td class="text-end">${item.stockQty}</td>
                `;
                normalTbody.appendChild(tr);
            });
        }
    }

    shipmentDetailModal.show();
}

// -------------------------------
// 상태 뱃지
// -------------------------------
function renderShipmentStatusBadge(status) {
    switch (status) {
        case "RESERVED": return `<span class="badge bg-primary">예약</span>`;
        case "LACK":     return `<span class="badge bg-danger">부족</span>`;
        case "SHIPPED":  return `<span class="badge bg-success">출하완료</span>`;
        case "PENDING":  return `<span class="badge bg-secondary">출고준비</span>`;
        default:         return `<span class="badge bg-primary">대기</span>`;
    }
}