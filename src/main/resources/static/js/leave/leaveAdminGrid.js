const LEAVE_API_BASE = apiUrl(`leave`)

const grid = new tui.Grid({
	el: document.getElementById("grid"),
	columns: [
		{
			header: "부서",
			name : "deptName",
			sortable: true,
			filter: { type: 'text', showApplyBtn: true, showClearBtn: true }
		},
		{
			header: "직급",
			name : "posName",
			filter: { type: 'text', showApplyBtn: true, showClearBtn: true }
		},
		{
			header: "사원번호",
			name : "empId",
			sortable: true,
		},
		{
			header: "이름",
			name : "empName",
			sortable: true,
		},
		{
			header: "총연차",
			name : "totalDays"
		},
		{
			header: "사용연차",
			name : "usedDays"
		},
		{
			header: "잔여연차",
			name : "remainDays",
		},
		{
			header: "수정",
			name : "btn",
			formatter: (rowInfo) => {
				return  `<button class="btn btn-primary btn-sm" data-id="${rowInfo.row.id}">수정</button>`
			}
		},
	],
	columnOptions: {
		resizable: true
	},
	bodyHeight: 500,	
	pageOptions: { 
		useClient: true,
		perPage: 10 
	}
});

// 데이터 가져오기
async function loadLeaveList(empId = null) {
	// 사원번호 검색 여부에 따라 쿼리파라미터 다르게 보냄
	const LEAVE_LIST = empId
	    ? apiUrl(`leave/list/data?empId=${empId}`)
	    : apiUrl(`leave/list/data`);
		
	try {
		const res = await fetch(LEAVE_LIST, {method: "GET"});
		
		if (!res.ok) {
			throw new Error("데이터 로드 실패!");
		}
		
		let data = await res.json();
		
		grid.resetData(data);
	} catch(error) {
		console.error(error);
		alert("데이터를 불러오는 중 오류가 발생했습니다.");	
	}
}

// 사원번호 조회 버튼 클릭 이벤트
document.querySelector("#searchBtn").addEventListener("click", () => {
	// 입력한 사원 번호
	let empId = document.querySelector("#searchId").value.trim();
	
	if (empId) {
		loadLeaveList(empId);
	} else {
		loadLeaveList();
	}
})

let currentLeaveId = null;

let modalInstance = null;

// 모달 관련
const openModal = async (leaveId) => {
	const modalElement = document.querySelector("#modalCenter");
	modalInstance = new bootstrap.Modal(modalElement);
	
	currentLeaveId = leaveId;
	
	// leaveId로 개인의 연차 조회
	const url = `${LEAVE_API_BASE}/${leaveId}`;
	
	const response = await fetch(url);
	const data = await response.json();
	
	// 현재 보유 연차에 값 넣어주기
	document.querySelector("#currentLeave").value = data.totalDays;
		
	modalInstance.show();
}

// 수정 버튼 클릭 이벤트(리스트에서 보이는 수정 버튼)
grid.on("click", (ev) => {	
	const { columnName, rowKey } = ev;

	resetModal();
	
	if (columnName === "btn") {
		const row = grid.getRow(rowKey);
		
		openModal(row.id);
	}
});

document.querySelector("#modifyBtn").addEventListener("click", async () => {
	if (!currentLeaveId) {
		alert("연차 정보가 없습니다.");
		return;
	}
	
	const changeType = getChangeType();
	const currentLeave = document.querySelector("#currentLeave").value;
	const changeDays = document.querySelector("#changeDays").value;
	const reason = document.querySelector("#reason").value;
	
	// 총연차가 0일 때 마이너스가 되지 않도록 처리
	if (changeType === "decrease") {
		if (currentLeave - changeDays < 0) {
			alert("차감할 수 있는 연차가 부족합니다.");
			return;
		}
	}
	
	// 사유를 입력하지 않았을 경우 전송되지 않도록 처리
	if (reason.trim().length === 0) {
		alert("사유는 필수 입력입니다.");
		return;
	}
	
	const url = `${LEAVE_API_BASE}/${currentLeaveId}`;
	const response = await fetch(url, {
		method: "POST",
		headers : {
			[csrfHeader]: csrfToken,
			"Content-Type": "application/json"
		},
		body: JSON.stringify({changeType, changeDays, reason})
	});
	
	if (!response.ok) {
		const errorData = await response.json();
		throw new Error(errorData.message || "요청 처리 중 오류가 발생했습니다.");
	}
	
	// 정상 응답일 경우
	const result = response.json();
	alert(result.message || "정상적으로 처리되었습니다.");
	
	modalInstance.hide(); // 모달 닫기
	resetModal(); // 모달 초기화
	 
	loadLeaveList();
});

// 모달 초기화
function resetModal() {
	document.querySelector("#changeDays").value = "";
	document.querySelector("#reason").value = "";
	document.querySelectorAll('input[name="changeType"]').forEach(radio => {
	  radio.checked = false;
	});
}

// 라디오 버튼 선택 값 반환
function getChangeType() {
	const selected = document.querySelector('input[name="changeType"]:checked');
	return selected ? selected.id : null;
}


// 페이지 로드 시 전체 조회
loadLeaveList();