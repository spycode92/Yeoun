/* =========================================================
   상태 그룹 정의
========================================================= */
const STATUS_GROUP_MAP = {
    RESERVED_GROUP: ["RESERVED", "PENDING"]
};


let gridApi = null;

/* =========================================================
   1) 페이지 로드 후 GRID 초기화 + 목록 조회
========================================================= */
document.addEventListener("DOMContentLoaded", () => {
	
	/* ================================
	     📅 날짜 조건 제어 (추가)
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


    initGrid();
    loadShipmentList("ALL");

    // 검색 버튼
    document.getElementById("btnSearch")?.addEventListener("click", () => {
        loadShipmentList(getSelectedStatus());
    });
	
	//엔터키로 검색
	const keywordInput = document.getElementById("keyword");
	   if (keywordInput) {
	       keywordInput.addEventListener("keydown", (e) => {
	           if (e.key === "Enter") {
	               e.preventDefault();
	               loadShipmentList(getSelectedStatus());
	           }
	       });
	   }

	// 초기화 버튼
	   document.getElementById("btnReset")?.addEventListener("click", () => {
	       startDateInput.value = "";
	       endDateInput.value = "";
	       endDateInput.min = ""; // ⭐ 중요
	       document.getElementById("keyword").value = "";
	       loadShipmentList("ALL");
	   });

    // 탭 클릭 이벤트
    document.querySelectorAll("#shipmentTabs .nav-link")?.forEach(tab => {
        tab.addEventListener("click", (e) => {
            e.preventDefault();

            const target = e.currentTarget;

            document.querySelector("#shipmentTabs .active")?.classList.remove("active");
            target.classList.add("active");

			const statusKey = target.dataset.status;

			// 그룹이면 배열, 아니면 단일값
			const statusList = STATUS_GROUP_MAP[statusKey] ?? statusKey;

			loadShipmentList(statusList);

        });
    });

});


/* =========================================================
   2) GRID 초기화 (Community 버전)
========================================================= */
function initGrid() {

    const columnDefs = [
      //  { headerName: "선택", checkboxSelection: true, width: 60 },

        // ⭐ rowGroup 제거 - Community 버전에서는 사용 불가
        { 
            headerName: "수주번호", 
            field: "orderId", 
            width: 150
        },

        { headerName: "거래처명", field: "clientName", width: 150 },
        { headerName: "제품명",   field: "prdName", width: 150 },
        { headerName: "수주수량", field: "orderQty", width: 150 },
        { headerName: "현재재고", field: "stockQty", width: 150 },
        { headerName: "납기요청일", field: "dueDate", width: 150 },

        // 상태 컬럼
        {
            headerName: "출하상태",
            field: "status",
            width: 150,
            cellRenderer: params => renderStatusBadge(params.value)
        },

        /* =========================================================
           ⭐ 예약 버튼 컬럼 - 수주번호별 첫 행에만 표시
           ⭐ reservableGroup 필드를 서버에서 받아서 사용
        ========================================================= */
		{
		    headerName: "예약",
		    width: 120,
		    cellRenderer: params => {

		        if (!params.data) return "";

		        const { orderId, status, reservableGroup } = params.data;

		        /* ===============================
		           1️⃣ 예약취소 버튼 (RESERVED)
		        =============================== */
		        if (status === "RESERVED" || status === "PENDING")  {
		            return `
		                <button class="btn btn-sm btn-outline-secondary"
		                        onclick="cancelShipment('${orderId}')">
		                    예약취소
		                </button>
		            `;
		        }

		        /* ===============================
		           2️⃣ PENDING / SHIPPED → 아무것도 표시 안 함
		        =============================== */
		        if (status === "SHIPPED") {
		            return "";
		        }

		        /* ===============================
		           3️⃣ WAITING / LACK → 예약 / 예약불가
		        =============================== */

		        // 현재 화면 row 기준
		        const visibleRows = [];
		        params.api.forEachNodeAfterFilterAndSort(node => {
		            if (node.data) visibleRows.push(node.data);
		        });

		        const sameOrderRows = visibleRows.filter(r => r.orderId === orderId);
		        if (sameOrderRows.length === 0) return "";

		        const firstRow = sameOrderRows[0];
		        const isFirstRow =
		            params.data.prdName === firstRow.prdName &&
		            params.data.orderQty === firstRow.orderQty &&
		            params.data.clientName === firstRow.clientName;

		        if (!isFirstRow) return "";

		        if (reservableGroup === true) {
		            return `
		                <button class="btn btn-sm btn-outline-danger"
		                        onclick="reserveShipment('${orderId}')">
		                    예약
		                </button>
		            `;
		        }

		        return `<span class="text-muted">예약불가</span>`;
		    }
		},


		{
		    headerName: "상세",
		    width: 100,
		    cellRenderer: params => {
		        if (!params.data) return "";
		        return `
		            <button class="btn btn-outline-primary btn-sm"
		                    onclick="openShipmentDetail('${params.data.orderId}')">
		                상세
		            </button>
		        `;
		    }
		}

    ];


    gridApi = agGrid.createGrid(
        document.getElementById("shipmentGrid"),
        {
            columnDefs,
            rowSelection: "multiple",
            suppressRowClickSelection: true,
            pagination: true,
            paginationPageSize: 20,
            paginationPageSizeSelector: [10, 20, 50, 100],
            
            // ⭐ Community 버전에서는 groupDisplayType 제거
            // groupDisplayType: "groupRows",
            // autoGroupColumnDef 제거
        }
    );
}


/* =========================================================
   3) 상태 뱃지 표시
========================================================= */
function renderStatusBadge(status) {
    switch (status) {
        case "RESERVED":
            return `<span class="badge bg-primary">예약</span>`;
        case "LACK":
            return `<span class="badge bg-danger">부족</span>`;
        case "SHIPPED":
            return `<span class="badge bg-success">출하완료</span>`;
		case "PENDING":
			return `<span class="badge bg-primary">출고준비</span>`;
        default:
            return `<span class="badge bg-secondary">대기</span>`;
    }
}


/* =========================================================
   4) 현재 선택된 탭 상태값 읽기
========================================================= */
function getSelectedStatus() {
    const active = document.querySelector("#shipmentTabs .active");
    return active ? active.dataset.status : "ALL";
}


/* =========================================================
   5) 출하 목록 조회
========================================================= */
function loadShipmentList(status) {

    const param = {
        status,
        startDate: document.getElementById("startDate").value,
        endDate: document.getElementById("endDate").value,
        keyword: document.getElementById("keyword").value
    };

    const csrfToken  = document.querySelector('meta[name="_csrf_token"]')?.content || "";
    const csrfHeader = document.querySelector('meta[name="_csrf_headerName"]')?.content || "";

    const headers = { "Content-Type": "application/json" };
    if (csrfToken && csrfHeader) {
        headers[csrfHeader] = csrfToken;
    }

    fetch("/sales/shipment/list", {
        method: "POST",
        headers,
        body: JSON.stringify(param)
    })
    .then(res => res.json())
    .then(data => {
        // ⭐ 수주번호별로 정렬 (같은 주문끼리 모으기)
        const sortedData = data.sort((a, b) => {
            if (a.orderId !== b.orderId) {
                return a.orderId.localeCompare(b.orderId);
            }
            // 같은 주문 내에서는 제품명으로 정렬
            return a.prdName.localeCompare(b.prdName);
        });
        
        gridApi.setGridOption("rowData", sortedData);
    })
    .catch(err => {
        console.error("❌ 목록 조회 오류:", err);
        alert("출하 목록 조회 중 오류 발생");
    });
}


/* =========================================================
   6) 출하 예약 처리
========================================================= */
function reserveShipment(orderId) {

    if (!confirm("해당 주문을 출하 예약하시겠습니까?")) return;

    const csrfToken  = document.querySelector('meta[name="_csrf_token"]')?.content || "";
    const csrfHeader = document.querySelector('meta[name="_csrf_headerName"]')?.content || "";

    const headers = {};
    if (csrfHeader && csrfToken) headers[csrfHeader] = csrfToken;

    fetch(`/sales/shipment/reserve?orderId=${orderId}`, {
        method: "POST",
        headers
    })
    .then(res => res.json())
    .then(data => {
        if (data.success) {
            alert("✔ 출하 예약이 완료되었습니다.");
            loadShipmentList(getSelectedStatus());
        } else {
            alert("❌ 실패: " + data.message);
        }
    })
    .catch(err => {
        console.error("❌ 예약 오류:", err);
        alert("예약 처리 중 오류 발생");
    });
}


/* =========================================================
   7) 상세 보기
========================================================= */
function openDetail(orderId) {
    alert("상세 페이지 준비 중: " + orderId);
}

/* =========================================================
   8) 예약 취소
========================================================= */

function cancelShipment(orderId) {

    if (!confirm("출하 예약을 취소하시겠습니까?")) return;

    const csrfToken  = document.querySelector('meta[name="_csrf_token"]')?.content || "";
    const csrfHeader = document.querySelector('meta[name="_csrf_headerName"]')?.content || "";

    const headers = {};
    if (csrfHeader && csrfToken) headers[csrfHeader] = csrfToken;

    fetch(`/sales/shipment/cancel?orderId=${orderId}`, {
        method: "POST",
        headers
    })
    .then(res => res.json())
    .then(data => {
        if (data.success) {
            alert("✔ 예약이 취소되었습니다.");
            loadShipmentList(getSelectedStatus());
        } else {
            alert("❌ 취소 실패: " + data.message);
        }
    })
    .catch(err => {
        console.error("예약취소 오류", err);
        alert("예약취소 중 오류 발생");
    });
}
