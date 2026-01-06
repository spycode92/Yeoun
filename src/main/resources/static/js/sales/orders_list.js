// ================================
// 상태값 → 뱃지 변환 함수
// ================================
const statusBadge = (value) => {
    const map = {
        "REQUEST":  { text: "수주요청", color: "primary" },
        "RECEIVED": { text: "접수완료", color: "success" },
        "CONFIRMED": { text: "수주확정", color: "warning" },
        "SHIPPED": { text: "출하완료", color: "secondary" },
        "CANCEL":   { text: "취소", color: "secondary" }
    };

    const v = map[value] ?? { text: value ?? "-", color: "secondary" };

    return `
        <span class="badge bg-${v.color}" style="font-size:12px;">
            ${v.text}
        </span>
    `;
};

// URL 파라미터에서 status 자동 인식
const urlParams = new URLSearchParams(location.search);
let currentStatus = urlParams.get("status") ?? "";

document.addEventListener("DOMContentLoaded", () => {

    /* ================================
       날짜 조건 제어 (🔥 추가)
    ================================ */
    const startDateInput = document.getElementById("startDate");
    const endDateInput   = document.getElementById("endDate");

    if (startDateInput && endDateInput) {
        startDateInput.addEventListener("change", () => {
            const startDate = startDateInput.value;

            if (startDate) {
                // 종료일은 시작일 이후만 선택 가능
                endDateInput.min = startDate;

                // 이미 선택된 종료일이 시작일보다 빠르면 초기화
                if (endDateInput.value && endDateInput.value < startDate) {
                    endDateInput.value = "";
                }
            }
        });
    }
	// 🔥 엔터 키로 검색
	document.getElementById("keyword")?.addEventListener("keydown", (e) => {
	    if (e.key === "Enter") {
	        e.preventDefault(); // form submit 방지
	        loadGrid();
	    }
	});


    /* ================================
       AG-Grid 설정
    ================================ */
    const gridOptions = {
        columnDefs: [
            { headerName: "수주번호", field: "orderId", width: 200 },
            { headerName: "거래처명", field: "clientName", width: 200 },
            { headerName: "수주일자", field: "orderDate", width: 200 },
            { headerName: "납기일자", field: "deliveryDate", width: 200 },
            {
                headerName: "상태",
                field: "orderStatus",
                width: 120,
                cellRenderer: params => statusBadge(params.value)
            },
            { headerName: "담당자", field: "managerName", width: 150 },
            { headerName: "메모", field: "memo", flex: 1 },
            {
                headerName: "상세",
                width: 100,
                cellRenderer: params => `
                    <button class="btn btn-outline-primary btn-sm"
                            onclick="location.href='/sales/orders/${params.data.orderId}'">
                        상세
                    </button>
                `
            }
        ],
        rowHeight: 42,
        pagination: true,
        paginationPageSize: 20,
        paginationPageSizeSelector: [10, 20, 50, 100],
    };

    const gridApi = agGrid.createGrid(
        document.getElementById("orderGrid"),
        gridOptions
    );

    function loadGrid() {
        const params = {
            status: currentStatus,
            startDate: startDateInput.value,
            endDate: endDateInput.value,
            keyword: document.getElementById("keyword").value
        };

        fetch(apiUrl(`sales/orders/list?`) + new URLSearchParams(params))
            .then(r => r.json())
            .then(data => gridApi.setGridOption("rowData", data));
    }

    // 초기 로딩
    loadGrid();

    // 검색
    document.getElementById("btnSearch").addEventListener("click", loadGrid);

    // 초기화
    document.getElementById("btnReset").addEventListener("click", () => {
        startDateInput.value = "";
        endDateInput.value = "";
        endDateInput.min = "";
        document.getElementById("keyword").value = "";
        currentStatus = "";
        loadGrid();
    });

    // 상태 탭 클릭
    document.querySelectorAll(".tab-btn").forEach(btn => {
        btn.addEventListener("click", () => {
            document.querySelector(".tab-btn.active")?.classList.remove("active");
            btn.classList.add("active");

            currentStatus = btn.dataset.status;
            loadGrid();
        });
    });
});
