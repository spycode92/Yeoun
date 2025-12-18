const grid = new tui.Grid({
	el: document.getElementById("grid"),
	columns: [
		{
			header: "부서명",
			name : "deptName",
			sortable: true,
			filter: {
				type: "text",
				showApplyBtn: true, 
				showClearBtn: true
			}
		},
		{
			header: "직급",
			name : "posName",
			sortable: true,
			filter: {
				type: "text",
				showApplyBtn: true, 
				showClearBtn: true
			}
		},
		{
			header: "사원이름",
			name : "empName",
			sortable: true
		},
		{
			header: "날짜",
			name : "accessDate",
			sortable: true
		},
		{
			header: "외출시간",
			name : "outTime"
		},
		{
			header: "복귀시간",
			name : "returnTime"
		},
		{
			header: "상태",
			name : "accessType"
		}
	],
	bodyHeight: 500,
	columnOptions: {
		resizable: true
	},
	pageOptions: { 
		useClient: true,
		perPage: 10 
	}
});

grid.sort('accessDate', true);

// 데이터 가져오기
async function loadAttendanceList(startDate, endDate) {
	const ACCESS_LOG_LIST = `/attendance/accessList/data?startDate=${startDate}&endDate=${endDate}`;
	try {
		const res = await fetch(ACCESS_LOG_LIST, {method: "GET"});
		
		if (res.status === 403) {
			alert("접근 권한이 없습니다.");
			location.href = "/main";
			return;
		}
		
		if (!res.ok) {
			throw new Error("데이터 로드 실패!");
		}
		
		let data = await res.json();
		
		const statusMap = {
			OUT: "외출",
			IN: "복귀",
			OUTWORK: "외근"
		}
		
		data = data.map(item => ({
			...item,
			accessType: statusMap[item.accessType] || item.accessType
		}));
		
		// 데이터가 없을 경우 빈배열 반환
		if (!data || data.length === 0) {
			grid.resetData([]);
		}
		
		grid.resetData(data);
	} catch(error) {
		console.error(error);
		alert("데이터를 불러오는 중 오류가 발생했습니다.");	
	}
}

document.addEventListener("DOMContentLoaded", async () => {
	// 오늘 날짜 구하기
	const today = new Date();
	const year = today.getFullYear();
	const month = today.getMonth() + 1;
	const day = today.getDate();
	
	// 이번 달 1일과 오늘 날짜 계산
	const startDate = `${year}-${String(month).padStart(2, "0")}-01`;
	const endDate = `${year}-${String(month).padStart(2, "0")}-${String(day).padStart(2, "0")}`;
	
	// 날짜 input 기본값 설정
	document.querySelector("#startDate").value = startDate;
	document.querySelector("#endDate").value = endDate;
	
	await loadAttendanceList(startDate, endDate);
});

// 날짜 조회 후 데이터 가져오기
document.querySelector("#searchbtn").addEventListener("click", async () => {
	const startDate = document.querySelector("#startDate").value;
	const endDate = document.querySelector("#endDate").value;
	
	if (!startDate || !endDate) {
		alert("조회할 기간을 선택해주세요!");
		return;
	}
	
	await loadAttendanceList(startDate, endDate);
});