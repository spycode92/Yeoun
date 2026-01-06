/**
 * clientList.js (AG Grid v31+ 버전 안정화 - 수정본)
 */

let gridApi = null;

document.addEventListener("DOMContentLoaded", () => {

    /* 메시지 출력 */
    const holder = document.getElementById("clientMsgHolder");
    if (holder?.dataset.msg) {
        alert(holder.dataset.msg);
    }

    /* 검색 버튼 (기존 submit 방지) */
    const btnSearch = document.getElementById("btnSearch");
    if (btnSearch) {
        btnSearch.addEventListener("click", e => {
            e.preventDefault();
            loadClientList();
        });
    }	
	/* 🔥 초기화 버튼 */
	const btnReset = document.getElementById("btnReset");
	if (btnReset) {
	    btnReset.addEventListener("click", () => {

	        const keywordInput =
	            document.querySelector("input[name='keyword']");
	        const itemKeywordInput =
	            document.querySelector("input[name='itemKeyword']");

	        if (keywordInput) keywordInput.value = "";
	        if (itemKeywordInput) itemKeywordInput.value = "";

	        console.log("🔄 검색 조건 초기화");

	        loadClientList();
	    });
	}


    /* 엔터 검색 */
    document.getElementById("keyword")?.addEventListener("keydown", e => {
        if (e.key === "Enter") {
            e.preventDefault();
            loadClientList();
        }
    });

    /* 그리드 생성 */
    initClientGrid();
    
   
});


/* ==========================================================
   1. AG Grid v31 그리드 초기화
========================================================== */

function initClientGrid() {

    const columnDefs = [
		{
		    headerName: "유형",
		    field: "clientType",
		    width: 110,
		    valueFormatter: p => {
		        if (!p.value) return "";
		        return p.value === "CUSTOMER" ? "거래처" : 
		               p.value === "SUPPLIER" ? "협력사" : p.value;
		    }
        },
       
        { headerName: "거래처명", field: "clientName", flex: 1 },
        { headerName: "사업자번호", field: "businessNo", width: 150 },
        { headerName: "대표자명", field: "ceoName", width: 140 },
        { headerName: "담당자", field: "managerName", width: 140 },
        { headerName: "연락처", field: "managerTel", width: 150 },
		{
		    headerName: "상태",
		    field: "statusCode",
		    width: 120,
		    valueFormatter: p => {
		        if (!p.value) return "";
		        return p.value === "ACTIVE" ? "활성" :
		               p.value === "INACTIVE" ? "비활성" : p.value;
		    }
		},

		// ✅ 상세 버튼 추가
		    {
		        headerName: "상세",
		        width: 110,
		        cellRenderer: (params) => {
		            return `
		                <button class="btn btn-sm btn-primary"
		                        onclick="goDetail('${params.data.clientId}')">
		                    상세
		                </button>
		            `;
            },
            cellStyle: { textAlign: "center" }
        }
    ];

    const gridOptions = {
        columnDefs,
        defaultColDef: { 
            resizable: true, 
            sortable: true, 
            filter: false 
        },
        pagination: true,
        paginationPageSize: 20,
        rowHeight: 38,
        animateRows: true,
        
        /* 로딩 오버레이 설정 (v32+) */
        loading: true,
        overlayLoadingTemplate: '<span class="ag-overlay-loading-center">데이터를 불러오는 중...</span>',
        overlayNoRowsTemplate: '<span class="ag-overlay-no-rows-center">조회된 데이터가 없습니다.</span>',

        /* Grid Ready 이벤트 */
        onGridReady: params => {
            gridApi = params.api;
            console.log("✅ Grid Ready");
            
            /* 초기 데이터가 있으면 로드 */
            if (window.initialClientList && Array.isArray(window.initialClientList)) {
                console.log("📊 초기 데이터 로드:", window.initialClientList.length);
                params.api.setGridOption("rowData", window.initialClientList);
                params.api.setGridOption("loading", false);
            }
            /* 초기 데이터 없으면 자동으로 로딩 상태 유지 */
        }
    };

    const gridDiv = document.getElementById("clientGrid");
    if (gridDiv) {
        agGrid.createGrid(gridDiv, gridOptions);
    } else {
        console.error("❌ clientGrid 요소를 찾을 수 없습니다.");
    }
}



/* ==========================================================
   2. 검색 및 목록 조회
========================================================== */
function loadClientList() {

    if (!gridApi) {
        console.error("❌ Grid API가 초기화되지 않았습니다.");
        return;
    }

    const keyword =
        document.querySelector("input[name='keyword']")?.value.trim() ?? "";

    const itemKeyword =
        document.querySelector("input[name='itemKeyword']")?.value.trim() ?? "";

    const type = window.currentType ?? "CUSTOMER";

    console.log("🔍 검색 조건:", { keyword, itemKeyword, type });

    gridApi.setGridOption("loading", true);

    const params = new URLSearchParams({
        keyword,
        type
    });

    // 🔥 SUPPLIER일 때만 itemKeyword 포함
    if (type === "SUPPLIER" && itemKeyword) {
        params.append("itemKeyword", itemKeyword);
    }

    fetch(apiUrl(`sales/client/data?${params.toString()}`))
        .then(res => {
            if (!res.ok) {
                throw new Error(`HTTP error! status: ${res.status}`);
            }
            return res.json();
        })
        .then(list => {
            gridApi.setGridOption("loading", false);
            gridApi.setGridOption("rowData", list ?? []);
        })
        .catch(err => {
            console.error("❌ 데이터 로드 실패:", err);
            alert("거래처 목록을 불러오는데 실패했습니다.\n" + err.message);
            gridApi.setGridOption("loading", false);
            gridApi.setGridOption("rowData", []);
        });
}


/* ==========================================================
   3. 상세조회
========================================================== */

function showClientDetail(clientId) {

    fetch(apiUrl(`sales/client/${clientId}`))
        .then(res => {
            if (!res.ok) {
                throw new Error(`HTTP error! status: ${res.status}`);
            }
            return res.json();
        })
        .then(d => {

            // 상세 모달 값 세팅
            const set = (id, v) => {
                const el = document.getElementById(id);
                if (el) el.textContent = v ?? "";
            };

            set("d-clientName", d.clientName);
            set("d-clientType", d.clientType);
            set("d-businessNo", d.businessNo);
            set("d-ceoName", d.ceoName);

            set("d-postCode", d.postCode);
            set("d-addr", d.addr);
            set("d-addrDetail", d.addrDetail);

            set("d-managerName", d.managerName);
            set("d-managerDept", d.managerDept);
            set("d-managerTel", d.managerTel);
            set("d-managerEmail", d.managerEmail);

            set("d-bankName", d.bankName);
            set("d-accountNumber", d.accountNumber);
            set("d-accountName", d.accountName);

            new bootstrap.Modal(document.getElementById("clientDetailModal")).show();
        })
        .catch(err => {
            console.error("❌ 상세 조회 실패:", err);
            alert("상세 조회 오류가 발생했습니다.\n" + err.message);
        });
}

/* ==========================================================
   화면 이동
========================================================== */
function goDetail(clientId) {
    location.href = `/sales/client/${clientId}`;
}
