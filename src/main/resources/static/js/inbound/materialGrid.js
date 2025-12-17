const grid = new tui.Grid({
	el: document.getElementById("materialGrid"),
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
			header: "회사명",
			name: "clientName",
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
grid.on("click", (ev) => {
	const { columnName, rowKey } = ev;
	
	if (columnName === "btn") {
		const row = grid.getRow(rowKey);
		// 입고 상세 페이지로 이동
		location.href = `/inventory/inbound/mat/${row.inboundId}`
	}
});

const startDateInput = document.querySelector("#startDate");
const endDateInput = document.querySelector("#endDate");

// 날짜 포맷 함수
function formatDate(isoDate) {
	if (!isoDate) return "";
	
	return isoDate.split("T")[0]; // YYYY-MM-dd 형식
}

// 원재료 정보 불러오기
async function loadMaterialInbound(startDate, endDate, searchType, keyword) {
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
		
		// 원재료정보만필터
		data = data.filter(row => row.inboundType === "MAT_IB");
		
		// 상태값이 영어로 들어오는 것을 한글로 변환해서 기존 data에 덮어씌움
		data = data.map(item => ({
			...item,
			inboundStatus: statusMap[item.inboundStatus] || item.inboundStatus
		}));
		
		grid.resetData(data);
		
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
		startDate = sessionStorage.getItem("material_startDate");
		endDate = sessionStorage.getItem("material_endDate");
	}
	
	if (!startDate || !endDate) {
		const today = new Date();
	    const year  = today.getFullYear();
	    const month = today.getMonth() + 1;
	    const day   = today.getDate();
		
		startDate = `${year}-${String(month).padStart(2, "0")}-01`;
		endDate   = `${year}-${String(month).padStart(2, "0")}-${String(day).padStart(2, "0")}`;
		
		sessionStorage.removeItem("material_startDate");
		sessionStorage.removeItem("material_endDate");
	}
	
	startDateInput.value = startDate;
	endDateInput.value   = endDate;
	
	await loadMaterialInbound(startDate, endDate, "all", "");

	//스피너  off
	hideSpinner();
});

// 검색
document.querySelector("#searchbtn").addEventListener("click", async () => {
	const startDate = startDateInput.value;
	const endDate = endDateInput.value;
	const keyword = document.querySelector("#materialKeyword").value;
	const searchType = document.querySelector("select[name='searchType']").value;
	
	if (!startDate || !endDate) {
		alert("조회할 기간을 선택해주세요!");
		return;
	}
	
	await loadMaterialInbound(startDate, endDate, searchType, keyword);
});

// 시작날짜 클릭 시 데이터 조회
document.querySelector("#startDate").addEventListener("input", async () => {
	const startDate = startDateInput.value;
	const endDate = endDateInput.value;
	const keyword = document.querySelector("#materialKeyword").value;
	const searchType = document.querySelector("select[name='searchType']").value;
	
	sessionStorage.setItem("material_startDate", startDateInput.value);
	sessionStorage.setItem("material_endDate", endDateInput.value);
	
	await loadMaterialInbound(startDate, endDate, searchType, keyword);
});

// 종료날짜 클릭 시 데이터 조회
document.querySelector("#endDate").addEventListener("input", async () => {
	const startDate = startDateInput.value;
	const endDate = endDateInput.value;
	const keyword = document.querySelector("#materialKeyword").value;
	const searchType = document.querySelector("select[name='searchType']").value;
	
	sessionStorage.setItem("material_startDate", startDateInput.value);
	sessionStorage.setItem("material_endDate", endDateInput.value);
	
	await loadMaterialInbound(startDate, endDate, searchType, keyword);
});
