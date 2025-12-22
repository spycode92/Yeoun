const grid = new tui.Grid({
	el: document.getElementById("grid"),
	columns: [
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
			header: "사유",
			name : "reason",
		},

	],
	bodyHeight: 500,
	columnOptions: {
		resizable: true
	},
	pageOptions: { 
		useClient: true,
		perPage: 10 
	},
	
});

// 데이터 가져오기
async function loadOutworkList(startDate, endDate) {
	const OUTWORK_LIST = `/attendance/outwork/data?startDate=${startDate}&endDate=${endDate}`;
	try {
		const res = await fetch(OUTWORK_LIST, {method: "GET"});
		
		if (!res.ok) {
			throw new Error("데이터 로드 실패!");
		}
		
		let data = await res.json();
		
		
		grid.resetData(data);
		grid.sort('accessDate', true); 
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
	
	await loadOutworkList(startDate, endDate);
});

// 날짜 조회 후 데이터 가져오기
document.querySelector("#searchbtn").addEventListener("click", async () => {
	const startDate = document.querySelector("#startDate").value;
	const endDate = document.querySelector("#endDate").value;
	
	if (!startDate || !endDate) {
		alert("조회할 기간을 선택해주세요!");
		return;
	}
	
	await loadOutworkList(startDate, endDate);
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
