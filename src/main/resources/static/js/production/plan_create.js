let suggestGridApi = null;
let orderItemGridApi = null;

// 🔥 최종 선택된 모든 수주 목록
let finalSelectedOrders = [];

/* ========================================================
   INIT
======================================================== */
document.addEventListener("DOMContentLoaded", () => {
    initSuggestGrid();
    initOrderItemGrid();

    document.getElementById("btnLoadSuggested").addEventListener("click", loadSuggestList);
    document.getElementById("btnCreatePlan").addEventListener("click", createProductionPlan);

    // 상세 모달 → 선택완료
    document.getElementById("btnSelectOrders")
        .addEventListener("click", saveSelectedOrdersFromModal);
});


/* ========================================================
   1) 추천 생산 목록 GRID
======================================================== */
function initSuggestGrid() {

    const columnDefs = [
        { headerName: "선택", checkboxSelection: true, width: 60 },
        { headerName: "제품명", field: "prdName", width: 150 },
        { headerName: "총 주문수량", field: "totalOrderQty", width: 120 },
        { headerName: "현재 재고", field: "currentStock", width: 120 },
        { headerName: "부족수량", field: "shortageQty", width: 120 },
        { headerName: "수주건수", field: "orderCount", width: 100 },
        {
            headerName: "가장 빠른 납기",
            field: "earliestDeliveryDate",
            width: 140,
            cellRenderer: p => p.value ? p.value : "-"
        },
        {
            headerName: "원자재 재고",
            field: "bomStatus",
            width: 120,
            cellRenderer: p => {
                if (!p.value) return "-";
                return p.value === "부족"
                    ? "<span style='color:red;'>❌ 부족</span>"
                    : "<span style='color:green;'>✔ 가능</span>";
            }
        },
        {
            headerName: "생산 필요",
            field: "needProduction",
            width: 120,
            cellRenderer: params =>
                params.value === "YES"
                    ? `<span style="color:#d9534f; font-weight:bold;">YES</span>`
                    : `<span style="color:#5cb85c;">NO</span>`
        },
        {
            headerName: "상세",
            width: 100,
			flex: 1,   
            cellRenderer: params => {
                return `
                    <button class="btn btn-sm btn-secondary"
                            onclick='showOrderItems("${params.data.prdId}")'>
                        보기
                    </button>`;
            }
        }
    ];

	suggestGridApi = agGrid.createGrid(
	    document.getElementById("suggestGrid"),
	    {
	        columnDefs,

	        rowSelection: "multiple",
	        suppressRowClickSelection: true,
	        rowData: [],
	        localeText: { noRowsToShow: "생산목록 조회 중입니다" },

	        /* ⭐⭐⭐ 이 부분 추가 ⭐⭐⭐ */
	        defaultColDef: {
	            headerClass: "ag-center-header",
	            cellClass: "ag-center-cell",
	            sortable: false,
	            resizable: true
	        }
	    }
	);

    // ⭐ 추천 목록에서 체크가 변경될 때 동작
    suggestGridApi.addEventListener("selectionChanged", onSuggestProductSelected);
}


/* ========================================================
   2) 추천 목록에서 제품 선택 → 해당 제품 수주 자동 추가
======================================================== */
function onSuggestProductSelected() {
    finalSelectedOrders = []; // 매번 리셋 (중복방지)

    const selectedProducts = suggestGridApi.getSelectedRows();

    selectedProducts.forEach(prod => {
        fetch(apiUrl(`production/order-items/${prod.prdId}`))
            .then(res => res.json())
            .then(items => {
                items.forEach(oi => {
                    finalSelectedOrders.push({
                        orderItemId: oi.orderItemId,
                        qty: oi.orderQty,
                        prdId: prod.prdId
                    });
                });
            });
    });
}


/* ========================================================
   3) 추천 목록 조회
======================================================== */
function loadSuggestList() {
    const group = document.getElementById("productGroup").value;

    fetch(apiUrl(`production/suggest?group=${group}`))
        .then(res => res.json())
        .then(data => {
            suggestGridApi.setGridOption("rowData", data);
        });
}


/* ========================================================
   4) 수주 상세 GRID
======================================================== */
function initOrderItemGrid() {

    const orderDetailColumnDefs = [
        { headerName: "선택", checkboxSelection: true, headerCheckboxSelection: true, width: 60 },
        { headerName: "수주번호", field: "orderId", width: 150 },
        { headerName: "거래처명", field: "clientName", width: 150 },
        { headerName: "제품명", field: "prdName", width: 150 },
        { headerName: "주문수량", field: "orderQty", width: 120 },
        { headerName: "내부 담당자", field: "empName", width: 150 },
        { headerName: "납기일", field: "deliveryDate", width: 150 },
        { headerName: "담당자명", field: "managerName", width: 150 },
        { headerName: "연락처", field: "managerTel", width: 150 },
        { headerName: "이메일", field: "managerEmail", width: 200 }
    ];

    orderItemGridApi = agGrid.createGrid(
        document.getElementById("orderItemGrid"),
        {
            columnDefs: orderDetailColumnDefs,
            rowSelection: "multiple",
            suppressRowClickSelection: true,
            rowData: [],
            defaultColDef: { sortable: true, filter: true, resizable: true }
        }
    );
}


/* ========================================================
   5) 상세모달 열기
======================================================== */
function showOrderItems(prdId) {

    fetch(apiUrl(`production/order-items/${prdId}`))
        .then(res => res.json())
        .then(data => {

            orderItemGridApi.setGridOption("rowData", data);

            // 선택복원: 이미 finalSelectedOrders에 있는 수주는 체크
            setTimeout(() => {
                const toSelect = finalSelectedOrders
                    .filter(i => i.prdId === prdId)
                    .map(i => i.orderItemId);

                orderItemGridApi.forEachNode(node => {
                    if (toSelect.includes(node.data.orderItemId)) {
                        node.setSelected(true);
                    }
                });
            }, 100);

            const modal = new bootstrap.Modal(document.getElementById("orderItemModal"));
            modal.show();
        });
}


/* ========================================================
   6) 상세모달 → 선택 완료
======================================================== */
function saveSelectedOrdersFromModal() {

    const rows = orderItemGridApi.getSelectedRows();
    const prdId = rows.length > 0 ? rows[0].prdId : null;

    // 기존 동일 제품 수주 제거
    finalSelectedOrders = finalSelectedOrders.filter(i => i.prdId !== prdId);

    // 새로 선택한 수주만 추가
    rows.forEach(r => {
        finalSelectedOrders.push({
            orderItemId: r.orderItemId,
            qty: r.orderQty,
            prdId: prdId
        });
    });

    const modal = bootstrap.Modal.getInstance(document.getElementById("orderItemModal"));
    modal.hide();

    alert("✔ 선택된 수주가 반영되었습니다.");
	createProductionPlan();
}


/* ========================================================
   7) 생산계획 생성
======================================================== */
function createProductionPlan() {

    if (finalSelectedOrders.length === 0) {
        alert("📌 선택된 수주가 없습니다.");
        return;
    }

    const memo = document.getElementById("planMemo")?.value || "";

    const payload = {
        memo,
        items: finalSelectedOrders
    };

    fetch(apiUrl(`production/create/submit`), {
        method: "POST",
        headers: {
            "Content-Type": "application/json",
            "X-CSRF-TOKEN": csrfToken
        },
        body: JSON.stringify(payload)
    })
        .then(res => res.json())
        .then(data => {
            if (data.success) {
                alert("🎉 생산계획 생성 완료!");
                location.href = "/production/plan";
            } else {
                alert("❌ 실패: " + data.message);
            }
        });
}


/* ========================================================
   8) 조회 결과 영역 표시
======================================================== */
document.getElementById("btnLoadSuggested").addEventListener("click", () => {
    document.getElementById("placeholderMessage").style.display = "none";
    document.getElementById("resultSection").style.display = "block";
});
