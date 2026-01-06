console.log("✔ plan_list.js 로드됨!");

let planGridApi = null;

document.addEventListener("DOMContentLoaded", () => {
    initPlanGrid();
    loadPlanList();

    // 검색 버튼
    document.getElementById("btnSearch")?.addEventListener("click", () => {
        const keyword = document.getElementById("keyword")?.value || "";
        applySearchFilter(keyword);
    });

    // 엔터 검색
    document.getElementById("keyword")?.addEventListener("keydown", e => {
        if (e.key === "Enter") {
            e.preventDefault();
            applySearchFilter(e.target.value);
        }
    });
});

/* =====================================================
   GRID 초기화
===================================================== */
function initPlanGrid() {

    const columnDefs = [
        { headerName: "계획번호", field: "planId", width: 200 },
        //{ headerName: "생성일", field: "createdAt", width: 200 },
		
		{
		    headerName: "생성일",
		    field: "createdAt",   // 실제 필드명
		    width: 150,
		    valueFormatter: params => {
		        if (!params.value) return "";
		        return params.value.substring(0, 10); // YYYY-MM-DD
		    }
		},

        { headerName: "제품명", field: "itemName", width: 180 },
        { headerName: "총수량", field: "totalQty", width: 160 },

        /* =====================
           상태 뱃지
        ===================== */
        {
            headerName: "상태",
            field: "status",
            width: 140,
            cellRenderer: params => {
                const map = {
                    PLANNING:         { text: "검토대기",   color: "secondary" },
                    MATERIAL_PENDING: { text: "자재확보중", color: "warning" },
                    IN_PROGRESS:      { text: "생산중",     color: "primary" },
                    DONE:             { text: "생산완료",   color: "success" },
                    CANCELLED:        { text: "취소",       color: "danger" }
                };

                const item = map[params.value] || {
                    text: params.value,
                    color: "dark"
                };

                return `
                    <span class="badge bg-${item.color}" style="font-size:13px;">
                        ${item.text}
                    </span>
                `;
            }
        },      

        /* =====================
           🔥 취소 버튼 (검토대기만)
        ===================== */
        {
            headerName: "취소",
            width: 90,
            cellRenderer: params => {
                if (params.data.status !== "PLANNING") return "";

                return `
                    <button class="btn btn-sm btn-outline-danger"
                            onclick="cancelPlan('${params.data.planId}')">
                        취소
                    </button>
                `;
            }
        },
		/* =====================
		          메모 (툴팁)
		       ===================== */
		       {
		           headerName: "메모",
		           field: "memo",
		           width: 250,
		           tooltipField: "memo",
		           cellRenderer: params => {
		               if (!params.value) return "-";

		               const maxLength = 20;
		               const text = params.value;
		               const display = text.length > maxLength
		                   ? text.substring(0, maxLength) + "..."
		                   : text;

		               return `<span title="${text}" style="cursor:pointer;">${display}</span>`;
		           }
		       },

		       /* =====================
		          상세 버튼
		       ===================== */
		       {
		           headerName: "상세",
		           width: 90,
		           cellRenderer: params => `
		               <button class="btn btn-sm btn-primary"
		                       onclick="openPlanDetail('${params.data.planId}')">
		                   보기
		               </button>
		           `
		       }
    ];

    const gridOptions = {
        columnDefs,
        rowSelection: "single",

        pagination: true,
        paginationPageSize: 20,
        paginationPageSizeSelector: [10, 20, 50, 100],

        defaultColDef: {
            sortable: true,
            resizable: true,
            filter: false,
			cellClass: "ag-center-cell",      // ⭐ 값 가운데
			headerClass: "ag-center-header"   // ⭐ 제목 가운데
        }
    };

    planGridApi = agGrid.createGrid(
        document.getElementById("planGrid"),
        gridOptions
    );
}

/* =====================================================
   목록 조회
===================================================== */
function loadPlanList() {
    fetch(apiUrl(`production/list`))
        .then(res => res.json())
        .then(data => {
            if (!planGridApi) return;
            planGridApi.setGridOption("rowData", data);
        })
        .catch(err => console.error("❌ 생산계획 목록 조회 실패:", err));
}

/* =====================================================
   검색 필터
===================================================== */
function applySearchFilter(keyword) {
    if (!planGridApi) return;
    planGridApi.setGridOption("quickFilterText", keyword);
}

/* =====================================================
   🔥 생산계획 취소
===================================================== */
function cancelPlan(planId) {

    if (!confirm("해당 생산계획을 취소하시겠습니까?")) return;

    fetch(apiUrl(`production/plan/${planId}/cancel`), {
        method: "POST",
        headers: {
            "X-CSRF-TOKEN": csrfToken
        }
    })
    .then(res => res.json())
    .then(data => {
        if (!data.success) {
            throw new Error(data.message || "취소 실패");
        }

        alert(data.message || "생산계획이 취소되었습니다.");
        loadPlanList(); // 🔄 즉시 갱신
    })
    .catch(err => {
        alert(err.message);
        console.error("❌ 생산계획 취소 오류:", err);
    });
}
