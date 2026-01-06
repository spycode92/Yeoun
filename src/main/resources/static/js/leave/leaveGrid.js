const grid = new tui.Grid({
	el: document.getElementById("grid"),
	language: 'ko',
	columns: [
		{
			header: "순서",
			name : "rowKey",
			formatter: ({ row }) => row.rowKey + 1,
			sortable: true,
		},
		{
			header: "연차유형",
			name : "leaveType",
			filter: "select"
		},
		{
			header: "휴가시작일",
			name : "startDate",
			sortable: true,
			filter: {
				type: 'date', options: {format: 'yyyy-MM-dd'}
			}
		},
		{
			header: "휴가종료일",
			name : "endDate",
			sortable: true,
			filter: {
				type: 'date', options: {format: 'yyyy-MM-dd'}
			}
		},
		{
			header: "사용일수",
			name : "usedDays"
		},
		{
			header: "사유",
			name : "reason"
		},
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

// 데이터 가져오기
async function loadLeaveList(startDate, endDate) {
	const LEAVE_LIST = apiUrl(`leave/my/data?startDate=${startDate}&endDate=${endDate}`);
	try {
		const res = await fetch(LEAVE_LIST, {method: "GET"});
		
		if (!res.ok) {
			throw new Error("데이터 로드 실패!");
		}
		
		let data = await res.json();
		
		// 연차유형 변환
		const leaveTypeMap = {
			ANNUAL: "연차",
			HALF: "반차",
			SICK: "병가",
		};
		
		// 상태값이 영어로 들어오는 것을 한글로 변환해서 기존 data에 덮어씌움
		data = data.map(item => ({
			...item,
			leaveType: leaveTypeMap[item.leaveType] || item.leaveType,
		}));
		
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
	
	// 이번 달 1일과 말일 계산
	const startDate = `${year}-${String(month).padStart(2, "0")}-01`;
	const endDate = new Date(year, month, 0).toISOString().split("T")[0];
	
	await loadLeaveList(startDate, endDate);
});
