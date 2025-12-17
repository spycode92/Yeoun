const grid = new tui.Grid({
	el: document.getElementById("grid"),
	columns: [
		{
			header: "사원이름",
			name : "empName",
			sortable: true
		},
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
			header: "날짜",
			name : "workDate",
			sortable: true
		},
		{
			header: "출근시간",
			name : "workIn"
		},
		{
			header: "퇴근시간",
			name : "workOut"
		},
		{
			header: "상태",
			name : "statusCode",
			filter: "select"
		},
		{
			header: "총근무시간",
			name : "workDuration",
			formatter: ({value}) => {
				if (value === null) return "";
				
				// data에서 받아온 값 중에서 workDuration를 60으로 나눠서 시간 구하기
				const hours = Math.floor(value / 60);
				// 60으로 나머지를 구해서 분 구하기
				const minutes = value % 60;
				
				return `${hours}시간${minutes}분`;
			}
		},
		{
			header: "수정",
			name : "btn",
			formatter: (rowInfo) => {
				return  `<button class="btn btn-primary btn-sm" data-id="${rowInfo.row.id}">수정</button>`
			}
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

// 버튼 이벤트 감지
grid.on("click", (ev) => {
	const { columnName, rowKey } = ev;
	
	if (columnName === "btn") {
		const row = grid.getRow(rowKey);
		// 해당 행의 id 값 가져오기
		const attendanceId = row.attendanceId;
		// attendance.js에 만들어둔 함수 사용
		openModalAttendance("edit", attendanceId);
	}
})

// 데이터 가져오기
async function loadAttendanceList(startDate, endDate) {
	const ATTENDANCE_ADMIN_LIST = `/attendance/list/data?startDate=${startDate}&endDate=${endDate}`;
	try {
		const res = await fetch(ATTENDANCE_ADMIN_LIST, {method: "GET"});
		
		if (!res.ok) {
			throw new Error("데이터 로드 실패!");
		}
		
		let data = await res.json();
		
		// 데이터가 없을 경우 빈배열 반환
		if (!data || data.length === 0) {
			grid.resetData([]);
		}
		
		const statusMap = {
			WORKIN: "출근",
			LATE: "지각",
			OFF: "휴무",
			OUTWORK: "외근"
		};
		
		// 상태값이 영어로 들어오는 것을 한글로 변환해서 기존 data에 덮어씌움
		data = data.map(item => ({
			...item,
			statusCode: statusMap[item.statusCode] || item.statusCode
		}));
		
		grid.resetData(data);
		
	} catch(error) {
		console.error(error);
		alert("데이터를 불러오는 중 오류가 발생했습니다.");	
	}
}

document.addEventListener("DOMContentLoaded", async () => {
	// 세션에 저장된 날짜
	const savedStart = sessionStorage.getItem("startDate");
	const savedEnd   = sessionStorage.getItem("endDate");
	
	let startDate;
	let endDate;
	
	if (savedStart && savedEnd) {// 날짜 변경이 있을 경우 저장된 날짜로 가져오기
		startDate = savedStart;
		endDate = savedEnd;
	} else {
		// 오늘 날짜 구하기
		const today = new Date();
		const year = today.getFullYear();
		const month = today.getMonth() + 1;
		const day = today.getDate();
		
		// 이번 달 1일과 오늘 날짜 계산
		startDate = `${year}-${String(month).padStart(2, "0")}-01`;
		endDate = `${year}-${String(month).padStart(2, "0")}-${String(day).padStart(2, "0")}`;
	}
	
	// 날짜 input 기본값 설정
	document.querySelector("#startDate").value = startDate;
	document.querySelector("#endDate").value = endDate;
	
	await loadAttendanceList(startDate, endDate);
});

// 날짜 조회 후 데이터 가져오기
document.querySelector("#searchbtn").addEventListener("click", async () => {
	const startDate = document.querySelector("#startDate").value;
	const endDate = document.querySelector("#endDate").value;
	
	sessionStorage.setItem("tartDate", startDate.value);
	sessionStorage.setItem("endDate", endDate.value);
	
	if (!startDate || !endDate) {
		alert("조회할 기간을 선택해주세요!");
		return;
	}
	
	await loadAttendanceList(startDate, endDate);
});