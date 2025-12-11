console.log("✔ plan_list.js 로드됨!");

let planGridApi = null;

document.addEventListener("DOMContentLoaded", () => {
    initPlanGrid();
    loadPlanList();

    // 검색 버튼 이벤트
    document.getElementById("btnSearch")?.addEventListener("click", () => {
        const keyword = document.getElementById("keyword")?.value || "";
        applySearchFilter(keyword);
    });

    // 엔터 검색
    document.getElementById("keyword")?.addEventListener("keydown", e => {
        if (e.key === "Enter") {
            e.preventDefault();
            const keyword = e.target.value;
            applySearchFilter(keyword);
        }
    });
});

function initPlanGrid() {

    const columnDefs = [
        { headerName: "계획ID", field: "planId", width: 200, sortable: true, filter: true },
        { headerName: "작성일", field: "createdAt", width: 200, sortable: true, filter: true },
        { headerName: "제품명", field: "itemName", width: 180, sortable: true, filter: true },
        { headerName: "총수량", field: "totalQty", width: 160, sortable: true, filter: true },

        {
            headerName: "상태",
            field: "status",
            sortable: true,
            filter: true,
            cellRenderer: params => {
                const map = {
                    PLANNING:         { text: "검토대기",     color: "secondary" },
                    MATERIAL_PENDING: { text: "자재확보중",   color: "warning" },
                    IN_PROGRESS:      { text: "생산중",       color: "primary" },
                    DONE:             { text: "생산완료",     color: "success" }
                };

                const item = map[params.value] || { text: params.value, color: "dark" };

                return `<span class="badge bg-${item.color}" style="font-size:13px;">
                            ${item.text}
                        </span>`;
            },
            width: 140
        },
		{
		    headerName: "메모",
		    field: "memo",
		    width: 250,
		    tooltipField: "memo",
		    cellRenderer: params => {
		        if (!params.value) return "-";

		        const text = params.value;
		        const maxLength = 20; // 🔥 원하는 표시 글자수 조절 가능

		        // 20자 이상이면 … 처리
		        const display = text.length > maxLength 
		            ? text.substring(0, maxLength) + "..." 
		            : text;

		        return `
		            <span title="${params.value}" style="cursor:pointer;">
		                ${display}
		            </span>
		        `;
		    }
		},


        {
            headerName: "상세",
            width: 100,
            cellRenderer: params =>
                `<button class="btn btn-sm btn-primary"
                          onclick="openPlanDetail('${params.data.planId}')">보기</button>`
        }
    ];

    const gridOptions = {
        columnDefs,
        rowSelection: "single",

        // ✅ 페이지네이션 추가
        pagination: true,
        paginationPageSize: 20,
        paginationPageSizeSelector: [10, 20, 50, 100],

        // AG Grid 최신 v31 방식
        defaultColDef: {
            sortable: true,
            filter: true,
            resizable: true,
        }
    };

    planGridApi = agGrid.createGrid(document.getElementById("planGrid"), gridOptions);
}

function loadPlanList() {
    fetch("/production/list")
        .then(res => res.json())
        .then(data => {
            console.log("📌 서버에서 받아온 데이터:", data);

            if (!planGridApi) {
                console.error("📌 planGridApi가 아직 준비되지 않았습니다.");
                return;
            }
            planGridApi.setGridOption("rowData", data);
        })
        .catch(err => console.error("📌 목록 조회 에러:", err));
}



/* =====================================
      🔍 그리드 검색 기능
===================================== */
function applySearchFilter(keyword) {

    if (!planGridApi) return;

    planGridApi.setGridOption("quickFilterText", keyword);

    console.log("🔍 검색어 필터 적용:", keyword);
}
