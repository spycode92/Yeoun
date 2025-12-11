// supplierDetail.js  (조회 전용)

let itemGridApi = null;
const clientId = window.clientId ?? null;

/* =======================================================
   페이지 로딩 시 실행
======================================================= */
document.addEventListener("DOMContentLoaded", () => {
    // 콘솔에서 데이터 확인 (초기 디버깅용)
    console.log("clientId =", clientId);
    console.log("initialItemList =", window.initialItemList);

    initItemGrid();
    loadItemGrid();
});

/* =======================================================
   1) AG-GRID — 협력사 취급 품목 목록
======================================================= */
function initItemGrid() {

    
	const columnDefs = [
	    { headerName: "품목ID", field: "itemId", width: 100 },

	    {
	        headerName: "카테고리",
	        field: "matType",
	        width: 110,
	        cellRenderer: p => {
	            const v = p.value;
	            if (v === "RAW") return "원재료";
	            if (v === "SUB") return "부자재";
	            if (v === "PKG") return "포장재";
	            return "";
	        }
	    },

	    // 자재코드
	    {
	        headerName: "자재코드",
	        width: 130,
	        valueGetter: p => p.data.materialId || p.data.matId || ""
	    },

	    // 품명
	    {
	        headerName: "품명",
	        flex: 1,
	        minWidth: 160,
	        valueGetter: p => p.data.materialName || p.data.matName || ""
	    },

		// 자재 기본 단위 (제품단위)
		   {
		       headerName: "BOM단위",
		       width: 90,
		       valueGetter: p => p.data.matUnit ?? ""
		   },

		   // 협력사 발주 지정 단위
		   {
		       headerName: "공급단위",
		       width: 100,
		       valueGetter: p => p.data.unit ?? ""
		   },


	    // 발주 단위 (ORDER_UNIT)
	    {
	        headerName: "발주단위",
	        width: 110,
	        valueGetter: p => p.data.orderUnit ?? ""
	    },

	    // MOQ
	    {
	        headerName: "MOQ",
	        width: 100,
	        valueGetter: p => p.data.moq ?? p.data.minOrderQty ?? ""
	    },

	    // 리드타임(납기일)
	    {
	        headerName: "납기일",
	        width: 100,
	        valueGetter: p => p.data.leadDays ?? ""
	    },

	    // 단가
	    {
	        headerName: "단가",
	        field: "unitPrice",
	        width: 110,
	        cellRenderer: p => {
	            if (!p.value) return "";
	            return Number(p.value).toLocaleString();
	        }
	    },

	    // 공급 여부
	    {
	        headerName: "공급",
	        field: "supplyAvailable",
	        width: 90,
	        cellRenderer: p => (p.value === "Y" ? "가능" : "불가")
	    }
	];


    const gridOptions = {
        columnDefs,
        rowData: [],
        defaultColDef: {
            sortable: true,
            filter: true,
            resizable: true
        },
        pagination: true,
        paginationPageSize: 20
    };

    const gridDiv = document.getElementById("supplierItemGrid");
    if (!gridDiv) {
        console.error("supplierItemGrid 요소를 찾을 수 없습니다.");
        return;
    }

    itemGridApi = agGrid.createGrid(gridDiv, gridOptions);
}

/* =======================================================
   2) 초기 데이터 로드
======================================================= */
function loadItemGrid() {
    if (!itemGridApi) return;

    const data = window.initialItemList || [];
    console.log("loadItemGrid data =", data);

    itemGridApi.setGridOption("rowData", data);
}
