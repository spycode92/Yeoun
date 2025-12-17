const Grid = tui.Grid;
const productGrid = new Grid({
	el: document.getElementById("productGrid"),
	bodyHeight: 500,
	rowHeaders: ['rowNum'],
	pageOptions: {
	    useClient: true,  // 클라이언트 사이드 페이징
	    perPage: 20       // 페이지당 10개 행
	},	
	columnOptions: {
		resizable: true
	},
	columns: [
		{
			header: "입고번호",
			name: "inboundId",
		},
		{
			header: "작업지시서",
			name: "prodId",
		},
		{
			header: "담당자",
			name: "materialEmpName",
		},
		{
			header: "입고예정일",
			name: "expectArrivalDate",
			sortable: true,
			formatter: ({value}) => formatDate(value)
		},
		{
			header: "상태",
			name: "inboundStatus",
			filter: "select"
		},
		{
			header: " ",
			name: "btn",
			formatter: (rowInfo) => {
				return `<button class="btn btn-outline-info btn-sm" data-id="${rowInfo.row.id}">상세</button>`
			}
		}
	]
});

// 상세 버튼 클릭 동작
productGrid.on("click", (ev) => {
	const { columnName, rowKey } = ev;
	
	if (columnName === "btn") {
		const row = productGrid.getRow(rowKey);
		// 입고 상세 페이지로 이동
		location.href = `/inventory/inbound/prd/${row.inboundId}`
	}
});

const prdStartDateInput = document.querySelector("#prdStartDate");
const prdEndDateInput = document.querySelector("#prdEndDate");


// 원재료 정보 불러오기
async function loadProductInbound(startDate, endDate, keyword, searchType) {
	const MATERIAL_INBOUND_LIST = 
		`/inventory/inbound/materialList/data` +
		`?startDate=${startDate}` +
		`&endDate=${endDate}` +
		`&searchType=${searchType}` +
		`&keyword=${keyword}`
			
	try {
		const res = await fetch(MATERIAL_INBOUND_LIST, {method: "GET"});
		
		if (!res.ok) {
			throw new Error("데이터 로드 실패!");
		}
		
		let data = await res.json();
		
		// 데이터가 없을 경우 빈배열 반환
		if (!data || data.length === 0) {
			productGrid.resetData([]);
		}
		
		const statusMap = {
			PENDING_ARRIVAL : "입고대기",
			INSPECTED: "검수완료",
			COMPLETED: "입고완료"
		}
		// 완제품 정보만필터
		data = await data.filter(row => row.inboundType === "PRD_IB");

		
		// 상태값이 영어로 들어오는 것을 한글로 변환해서 기존 data에 덮어씌움
		data = await data.map(item => ({
			...item,
			inboundStatus: statusMap[item.inboundStatus] || item.inboundStatus
		}));
//		console.log(data);
		await productGrid.resetData(data);
		
	} catch (error) {
		console.error(error);
	}
}

// 페이지 뒤로가기에만 지정한 날짜 적용되게 하는 로직
window.addEventListener("pageshow", async (e) => {
	const isBackForward = e.persisted || performance.getEntriesByType("navigation")[0].type ===  "back_forward";
	
	let startDate;
	let endDate;
	
	if (isBackForward) {
		// 뒤로 가기 했을 경우 이전 조회 날짜 유지
		startDate = sessionStorage.getItem("product_startDate");
		endDate = sessionStorage.getItem("product_endDate");
	}
	
	if (!startDate || !endDate) {
		const today = new Date();
	    const year  = today.getFullYear();
	    const month = today.getMonth() + 1;
	    const day   = today.getDate();
		
		startDate = `${year}-${String(month).padStart(2, "0")}-01`;
		endDate   = `${year}-${String(month).padStart(2, "0")}-${String(day).padStart(2, "0")}`;
		
		sessionStorage.removeItem("product_startDate");
		sessionStorage.removeItem("product_endDate");
	}
	
	prdStartDateInput.value = startDate;
	prdEndDateInput.value = endDate;
	
	await loadProductInbound(startDate, endDate, "", "");

	//스피너  off
	hideSpinner();
});

// 검색
document.querySelector("#prdBtnSearch").addEventListener("click", async () => {
	const startDate = prdStartDateInput.value;
	const endDate = prdEndDateInput.value;
	const keyword = document.querySelector("#prdKeyword").value;
	const searchType = document.querySelector("select[name='prdSearchType']").value;

	
	if (!startDate || !endDate) {
		alert("조회할 기간을 선택해주세요!");
		return;
	}
	
	await loadProductInbound(startDate, endDate, keyword, searchType);
});

// 시작날짜 클릭 시 데이터 조회
document.querySelector("#prdStartDate").addEventListener("input", async () => {
	const startDate = prdStartDateInput.value;
	const endDate = prdEndDateInput.value;
	const keyword = document.querySelector("#prdKeyword").value;
	const searchType = document.querySelector("select[name='prdSearchType']").value;
	
	sessionStorage.setItem("product_startDate", prdStartDateInput.value);
	sessionStorage.setItem("product_endDate", prdEndDateInput.value);

	
	await loadProductInbound(startDate, endDate, keyword, searchType);
});

// 종료날짜 클릭 시 데이터 조회
document.querySelector("#prdEndDate").addEventListener("input", async () => {
	const startDate = prdStartDateInput.value;
	const endDate = prdEndDateInput.value;
	const keyword = document.querySelector("#prdKeyword").value;
	const searchType = document.querySelector("select[name='prdSearchType']").value;
	
	sessionStorage.setItem("product_startDate", prdStartDateInput.value);
	sessionStorage.setItem("product_endDate", prdEndDateInput.value);

	
	await loadProductInbound(startDate, endDate, keyword, searchType);
});
