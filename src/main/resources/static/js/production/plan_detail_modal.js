// /js/production/plan_detail_modal.js

let modalPlanItemGridApi;
let modalOrderItemGridApi;
let orderItemMap = {};

/* ===============================
   생산계획 상세 모달 호출 함수
================================= */
function openPlanDetail(planId) {

    fetch(apiUrl(`production/plan/detail/${planId}`))
        .then(res => res.json())
        .then(data => {
            console.log("받은 데이터:", data);

            // ============================
            // 🔥 상태 한글 변환 매핑
            // ============================
            const statusMap = {
                PLANNING: "검토대기",
                MATERIAL_PENDING: "자재확보중",
                IN_PROGRESS: "생산중",
                DONE: "생산완료",
				CANCELLED:"취소"
            };

            // 전역에 저장
            orderItemMap = data.orderItemMap || {};

            // ============================
            // 🔥 기본 정보 표시
            // ============================
            document.getElementById("d_planId").innerText = data.planId;
			document.getElementById("d_createdAt").innerText =			    
			data.createdAt
			        ?.replace("T", " ")
			        ?.split(".")[0] ?? "";
			document.getElementById("d_createdBy").innerText =
				data.createdByName ?? "";
		


            // ■ 상태 한글 변환 적용
			document.getElementById("d_status").innerHTML =
			    `<span class="badge bg-info">${statusMap[data.status]}</span>`;


            // ■ 메모
            document.getElementById("d_memo").innerText = data.memo || "-";

            // ============================
            // 🔥 납기일 제거 → 설정하지 않음
            // ============================
            // document.getElementById("d_dueDate").innerText = data.dueDate;
            // (HTML에서 d_dueDate 블록도 제거하거나 숨기면 됨)

            // ============================
            // 🔥 PLAN_ITEM 그리드 적용
            // ============================
            modalPlanItemGridApi.setGridOption("rowData", data.planItems);

            // ============================
            // 🔥 첫 제품의 수주 자동 로드
            // ============================
            if (data.planItems && data.planItems.length > 0) {
                const firstPrdId = data.planItems[0].prdId;
                const firstOrders = orderItemMap[firstPrdId] || [];
                modalOrderItemGridApi.setGridOption("rowData", firstOrders);
            } else {
                modalOrderItemGridApi.setGridOption("rowData", []);
            }

            // ============================
            // 🔥 모달 열기
            // ============================
            const modal = new bootstrap.Modal(document.getElementById("planDetailModal"));
            modal.show();
        })
        .catch(err => console.error("에러:", err));
}

window.openPlanDetail = openPlanDetail;


/* ===============================
   AG GRID 초기화
================================= */
document.addEventListener('DOMContentLoaded', function() {
    
    // 생산 아이템 그리드
    modalPlanItemGridApi = agGrid.createGrid(
        document.getElementById("modalPlanItemGrid"),
        {
            columnDefs: [
                { field: "prdId", headerName: "제품ID", width: 200 },
                { field: "prdName", headerName: "제품명", width: 200 },
                { field: "planQty", headerName: "계획수량", width: 200 }
                             
            ],
			domLayout: "autoHeight",
            //rowSelection: { mode: 'singleRow' },
            onRowClicked: function(event) {
                const prdId = event.data.prdId;
                const orders = orderItemMap[prdId] || [];
                
                console.log("클릭한 제품:", prdId);
                console.log("해당 제품의 수주:", orders);
                
                // ✅ 완전히 새로운 데이터로 교체
                modalOrderItemGridApi.setGridOption('rowData', orders);
            }
        }
    );

    // 수주 그리드
    modalOrderItemGridApi = agGrid.createGrid(
        document.getElementById("modalOrderItemGrid"),
        {
            columnDefs: [
                { field: "orderId", headerName: "수주번호", width: 200 },
                { field: "clientName", headerName: "거래처", width: 150 },
                { field: "orderQty", headerName: "주문수량", width: 120 },
                { field: "orderDate", headerName: "수주일자", width: 140 },
                { field: "deliveryDate", headerName: "납기일", width: 140 }
            ]
        }
    );
    
    console.log("그리드 초기화 완료");
});