const grid = new tui.Grid({
	el: document.getElementById("grid"),
	columns: [
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
async function loadAttendanceList(startDate, endDate) {
	const ATTENDANCE_LIST = `/attendance/my/data?startDate=${startDate}&endDate=${endDate}`;
	try {
		const res = await fetch(ATTENDANCE_LIST, {method: "GET"});
		
		if (!res.ok) {
			throw new Error("데이터 로드 실패!");
		}
		
		let data = await res.json();
		
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
	
	sessionStorage.setItem("startDate", startDate.value);
	sessionStorage.setItem("endDate", endDate.value);
	
	if (!startDate || !endDate) {
		alert("조회할 기간을 선택해주세요!");
		return;
	}
	
	await loadAttendanceList(startDate, endDate);
});

// -----------------------------------------------------
// 외근 등록 유효성 검사
const form = document.querySelector("#outworkForm");
const dateInput = document.querySelector("#inTime");
const typeSelect = document.querySelector("select[name='accessType']");
const outTimeInput = document.querySelector("#outTime");
const reasonTextarea = document.querySelector("#reason");

form.addEventListener("submit", (event) => {
	// 근무날짜 미선택 시 전송 안됨
	if (!dateInput.value) {
		event.preventDefault();
		dateInput.classList.add("is-invalid");
	}
	
	// 근무유형 미선택 시 전송 안됨
	if (!typeSelect.value) {
		event.preventDefault();
		typeSelect.classList.add("is-invalid");
	}
	
	// 외근 시작 시간 미입력 했을 경우 전송 안됨
	if (!outTimeInput.value) {
		event.preventDefault();
		outTimeInput.classList.add("is-invalid");
	}
	
	// 외근 사유 2글자 미만일 경우 전송 안됨
	if (reasonTextarea.value.trim().length < 2) {
		event.preventDefault();
		reasonTextarea.classList.add("is-invalid");
	}
});

// 근무 날짜 입력 또는 선택 시 에러 문구 삭제
dateInput.addEventListener("change", () => {
	if (dateInput.value) {
		dateInput.classList.remove("is-invalid");
	}
});

// 근무유형을 선택했을 경우 에러 문구 삭제
typeSelect.addEventListener("change", () => {
	if (typeSelect.value) {
		typeSelect.classList.remove("is-invalid");
	}
});

// 외근 사유 2글자 이상 입력 시 에러 문구 삭제
reasonTextarea.addEventListener("input", () => {
	if (reasonTextarea.value.trim().length >= 2) {
		reasonTextarea.classList.remove("is-invalid");
	}
});

// 외근 시작 시간 입력 시 에러 문구 삭제됨
outTimeInput.addEventListener("input", () => {
	if (outTimeInput.value) {
		outTimeInput.classList.remove("is-invalid");
	}
});
