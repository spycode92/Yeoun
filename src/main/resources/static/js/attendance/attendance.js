// 출/퇴근 버튼 클릭 시 사원번호를 전달해서 출/퇴근 기록 요청
async function attendance(empId) {
	const PROCESS_ATTENDANCE = apiUrl(`attendance/toggle/${empId}`);
	const response = await fetch(PROCESS_ATTENDANCE, { 
		method: "POST",
		headers: {
			[csrfHeader]: csrfToken, 
			"Content-Type": "application/json"
		}
	});
	const data = await response.json();
	
	return data; // {success: true, status: status} 로 반환
}

const handleAttendanceToggle = async (empId) => {
	const result =  await attendance(empId);
	const attendanceBtn = document.querySelector("#attendance");
	
	let msg = "";
	
	// attendance함수의 반환값이 data의 status로 알림 변경
	if (result.status === "WORKIN") {
		attendanceBtn.innerText = "퇴근";
		msg = "출근했습니다.";
	} else if (result.status === "WORK_OUT") {
		attendanceBtn.innerText = "출근";
		msg = "퇴근했습니다.";
	} else if (result.status === "LATE") {
		msg = "지각입니다.";
	} else if (result.status === "IN") {
		msg = "복귀합니다.";
	} else {
		msg = "외출입니다.";
	}
	
	// 버튼 활성화/비활성화 적용
	if (result.buttonEnabled === false) {
		attendanceBtn.disabled = true;
	} else {
		attendanceBtn.disabled = false;
	}
	
	alert(msg);
	
	setTimeout(() => {
	    if (result.status === "WORKIN" || result.status === "WORK_OUT" || result.status === "LATE") {
	        location.reload();
	    }
	}, 10); 
	
}

// 사원번호로 사원 조회
const searchEmp = async () => {
	const empId = document.querySelector("#nameWithTitle").value;
	const empName = document.querySelector("#empName");
	
	const SEARCH_EMP = apiUrl(`attendance/search?empId=${empId}`);
	
	try {
		const response = await fetch(SEARCH_EMP, {method: "GET"});
		
		if (!response.ok) {
			const errorData = await response.json;
			alert(errorData.message || "사원 정보를 찾을 수 없습니다.");
			return;
		}		
		
		const data = await response.json();

		if (data) {
			empName.value = data.empName;
		}
	} catch (error) {
		console.error("사원 조회 중 오류 : " , error);
		alert("사원 조회 중 오류가 발생했습니다.");
	}
}

let currentMode = "regist";
let currentAttendanceId = null;
// 수정 모달에서 기존 시간과 변경된 시간 비교하기 위한 변수
let originalInTime = null;

// 출퇴근 수기 등록 및 수정 모달
const openModalAttendance = async (mode, attendanceId = null) => {
	const modalTitle = document.querySelector("#modalCenterTitle");
	const saveBtn = document.querySelector("#saveBtn");
	const searchBtn = document.querySelector("#searchBtn");
	const searchEmpId = document.querySelector("#nameWithTitle");
	const modalElement = document.querySelector("#modalCenter");
	const modalInstance = new bootstrap.Modal(modalElement);
	
	currentMode = mode; // 현재 모드 저장
	currentAttendanceId = attendanceId; // 수정 모드일 경우 id가 들어와서 저장
	
	const ATTENDANCE_DETAIL_URL = apiUrl(`attendance/${attendanceId}`);
	
	if (mode === "edit" && attendanceId) { 	// 수정 버튼 클릭 시 동작
		modalTitle.textContent = "출/퇴근 수정";
		saveBtn.textContent = "수정";
		
		// 수정 모달에서는 사원번호 조회 기능 비활성화
		searchEmpId.disabled = true
		searchBtn.disabled = true;
		
		// 선택한 데이터 불러오기
		const response = await fetch(ATTENDANCE_DETAIL_URL);
		const data = await response.json();
		
		document.querySelector("#nameWithTitle").value = data.empId;
		
		// 사원번호로 이름 조회 로직
		await searchEmp();
		
		if (data.workIn != null) {
			originalInTime = data.workIn.slice(0, 5);
			document.querySelector("#inTime").value = originalInTime;
		} else {
			originalInTime = null;
		}
		
		if (data.workOut != null) {
			document.querySelector("#endTime").value = data.workOut.slice(0, 5);
		}
		
		document.querySelector("select[name='statusCode']").value = data.statusCode;
	} else { // 등록 모드
		modalTitle.textContent = "출/퇴근 등록";
		saveBtn.textContent = "등록";
		originalInTime = null;
		
		// 등록 모달에서는 사원번호 조회 활성화
		searchEmpId.disabled = false;
		searchBtn.disabled = false;
		
		resetModal(); // 모달 초기화
	}
	modalInstance.show();
}

// 등록 및 수정 공용 함수 
const saveAttendance = async () => {
	const empId = document.querySelector("#nameWithTitle").value;
	const workIn = document.querySelector("#inTime").value;
	const workOut = document.querySelector("#endTime").value;
	const statusCode = document.querySelector("select[name='statusCode']").value;
	
	const url = currentMode === "edit" ? apiUrl(`attendance/${currentAttendanceId}`) : "/attendance";
	const method = currentMode === "edit" ? "PATCH" : "POST";
	
	// 근무정책의 출퇴근 데이터 가져오기
	const data = await loadWorkPolicy();
	
	// 근무정책에 따른 출퇴근 시간 저장하는 변수
	const MIN_TIME = data.startTime;
	const MAX_TIME = data.endTime;
	
	// 범위 체크 상수를 사용하여 Date 객체로 변환
	const min = new Date(`2000-01-01T${MIN_TIME}`);
	const max = new Date(`2000-01-01T${MAX_TIME}`);
	
	// 출근 시간을 입력하지 않았을 때
	if (!workIn) {
		alert("출근 시간은 필수 입력입니다.");
		return;
	}
	
	if (!statusCode) {
		alert("근태 상태는 필수 선택입니다.");
		return;
	}
	
	const inDate = new Date(`2000-01-01T${workIn}`);
	
	// 수정모드에서 시간 변경되었을 때 동작
	if (originalInTime !== null && workIn !== originalInTime) {
		
		if (inDate < min || inDate > max) {
			alert(`출근시간은 ${MIN_TIME} ~ ${MAX_TIME} 사이여야 합니다.`);
			return;
		}
	}
	
	// 퇴근 시간이 있을 때 출근 시간과 비교
	if (workOut) {
		const outDate = new Date(`2000-01-01T${workOut}`);
		
		if (outDate < inDate) {
			alert("퇴근시간은 출근시간 이후여야 합니다.");
			return;
		}
		
		if (outDate < min || outDate > max) {
			alert(`퇴근시간은 ${MIN_TIME} ~ ${MAX_TIME} 사이여야 합니다.`);
			return;
		}
	}
	
	try {
		const response = await fetch(url, {
			method,
			headers: {
				[csrfHeader]: csrfToken, 
				"Content-Type": "application/json"
			},
			body: JSON.stringify({empId, workIn, workOut, statusCode}),
		});
		
		if (!response.ok) {
			const errorData = await response.json();
			throw new Error(errorData.message || "요청 처리 중 오류가 발생했습니다.");
		}
		
		// 정상 응답일 경우
		const result = await response.json();
		alert(result.message || "정상적으로 처리되었습니다.");
	
		// fetch 응답 전 리로드 되지 않도록 약 0.3초 지연
		setTimeout(() => {
			location.reload();
		}, 300);
	} catch (error) {
		console.error("에러 : " + error);
		alert(error.message || "서버와 통신 중 오류가 발생했습니다.");
	}
}

// 모달 초기화
function resetModal() {
	document.querySelector("#nameWithTitle").value = "";
	document.querySelector("#empName").value = "";
	document.querySelector("#inTime").value = "";
	document.querySelector("#endTime").value = "";
	document.querySelector("select[name='statusCode']").value = "";
}

// 근무정책에서 출근시간과 퇴근시간 정보 가져오기
async function loadWorkPolicy() {
	const res = await fetch(apiUrl(`attendance/policy/data`));
	const data = await res.json();
	
	return data;
}