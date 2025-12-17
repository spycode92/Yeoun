const reMaterialGrid = new tui.Grid({
	el: document.getElementById("reMaterialGrid"),
	bodyHeight: 500,
	rowHeaders: ['rowNum'],
	pageOptions: {
	    useClient: true,  // 클라이언트 사이드 페이징
	    perPage: 20       // 페이지당 20개 행
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
reMaterialGrid.on("click", (ev) => {
	const { columnName, rowKey } = ev;
	
	if (columnName === "btn") {
		const row = reMaterialGrid.getRow(rowKey);
		// 입고 상세 페이지로 이동
		location.href = `/inventory/inbound/re/${row.inboundId}`
	}
});

const reStartDateInput = document.querySelector("#reStartDate");
const reEndDateInput = document.querySelector("#reEndDate");

// 날짜 포맷 함수
function formatDate(isoDate) {
	if (!isoDate) return "";
	
	return isoDate.split("T")[0]; // YYYY-MM-dd 형식
}

// 원재료 정보 불러오기
async function loadReMaterialInbound(startDate, endDate, searchType, keyword) {
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
			grid.resetData([]);
		}
		
		const statusMap = {
			PENDING_ARRIVAL : "입고대기",
			INSPECTED: "검수완료",
			COMPLETED: "입고완료"
		}
		
		// 재입고 정보만 필터
		data = data.filter(row => row.inboundType == "RE_IB");
		
		// 상태값이 영어로 들어오는 것을 한글로 변환해서 기존 data에 덮어씌움
		data = data.map(item => ({
			...item,
			inboundStatus: statusMap[item.inboundStatus] || item.inboundStatus
		}));
		
		reMaterialGrid.resetData(data);
		
	} catch (error) {
		console.error(error);
	}
}

document.addEventListener("DOMContentLoaded", async () => {
	//스피너 on
	showSpinner();
});

// 페이지 뒤로가기에만 지정한 날짜 적용되게 하는 로직
window.addEventListener("pageshow", async (e) => {
	const isBackForward = e.persisted || performance.getEntriesByType("navigation")[0].type ===  "back_forward";
	
	let startDate;
	let endDate;
	
	if (isBackForward) {
		// 뒤로 가기 했을 경우 이전 조회 날짜 유지
		startDate = sessionStorage.getItem("rematerial_startDate");
		endDate = sessionStorage.getItem("rematerial_endDate");
	}
	
	if (!startDate || !endDate) {
		const today = new Date();
	    const year  = today.getFullYear();
	    const month = today.getMonth() + 1;
	    const day   = today.getDate();
		
		startDate = `${year}-${String(month).padStart(2, "0")}-01`;
		endDate   = `${year}-${String(month).padStart(2, "0")}-${String(day).padStart(2, "0")}`;
		
		sessionStorage.removeItem("rematerial_startDate");
		sessionStorage.removeItem("rematerial_endDate");
	}
	
	reStartDateInput.value = startDate;
	reEndDateInput.value = endDate;
	
	await loadReMaterialInbound(startDate, endDate, "all", "");

	//스피너  off
	hideSpinner();
});

// 검색
document.querySelector("#reSearchbtn").addEventListener("click", async () => {
	const startDate = reStartDateInput.value;
	const endDate = reEndDateInput.value;
	const keyword = document.querySelector("#reMaterialKeyword").value;
	const searchType = document.querySelector("select[name='searchType']").value;
	
	if (!startDate || !endDate) {
		alert("조회할 기간을 선택해주세요!");
		return;
	}
	
	await loadReMaterialInbound(startDate, endDate, searchType, keyword);
});

// 시작날짜 클릭 시 데이터 조회
document.querySelector("#reStartDate").addEventListener("input", async () => {
	const startDate = reStartDateInput.value;
	const endDate = reEndDateInput.value;
	const keyword = document.querySelector("#reMaterialKeyword").value;
	const searchType = document.querySelector("select[name='reSearchType']").value;
	
	sessionStorage.setItem("rematerial_startDate", reStartDateInput.value);
	sessionStorage.setItem("rematerial_endDate", reEndDateInput.value);
	
	await loadReMaterialInbound(startDate, endDate, searchType, keyword);
});

// 종료날짜 클릭 시 데이터 조회
document.querySelector("#reEndDate").addEventListener("input", async () => {
	const startDate = reStartDateInput.value;
	const endDate = reEndDateInput.value;
	const keyword = document.querySelector("#reMaterialKeyword").value;
	const searchType = document.querySelector("select[name='reSearchType']").value;
	
	sessionStorage.setItem("rematerial_startDate", reStartDateInput.value);
	sessionStorage.setItem("rematerial_endDate", reEndDateInput.value);
	
	await loadReMaterialInbound(startDate, endDate, searchType, keyword);
});
