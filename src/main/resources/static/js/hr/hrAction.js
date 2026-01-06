// 인사 발령 JS

let empGrid = null;
//let originalEmpList = [];

document.addEventListener('DOMContentLoaded', function() {
	
	// 페이지 로드되면 사원 목록 불러오기
	loadEmpListForHrAction();
	
	// 결재자 셀렉트박스 옵션 로드
	initApproverView();
	
	// 결재선 박스 기본 숨김
	setApprovalLineVisible(false);
	
	// 검색 이벤트
	const btnSearch = document.getElementById('btnSearchEmp');
	if (btnSearch) {
		btnSearch.addEventListener('click', () => onSearchEmp());
	}
	
	// 부서 선택 바뀌면 바로 필터링
	const selDept = document.getElementById('searchDept');
	if (selDept) {
		selDept.addEventListener('change', () => onSearchEmp());
	}

	// 직급 선택 바뀌면 바로 필터링
	const selPos = document.getElementById('searchPos');
	if (selPos) {
		selPos.addEventListener('change', () => onSearchEmp());
	}
	
	// 재직상태 선택 바뀌면 바로 필터링
	const selStatus = document.getElementById('searchStatus');
	if (selStatus) {
		selStatus.addEventListener('change', () => onSearchEmp());
	}

	// 이름/사번 입력 후 엔터 누르면 검색
	const inputKeyword = document.getElementById('searchKeyword');
	if (inputKeyword) {
		inputKeyword.addEventListener('input', () => {
			onSearchEmp();
		});
	}
	
	// 발령 등록 버튼 이벤트 - DOMContentLoaded에서 바로 등록
	const btnSubmit = document.getElementById('btnSubmit');
	if (btnSubmit) {
		btnSubmit.addEventListener('click', handleSubmitAction);
	}
	
	// 발령 구분 변경 시 부서/직급 막기
	const selActionType = document.querySelector("select[name='actionType']");
    if (selActionType) {
        selActionType.addEventListener('change', handleActionTypeChange);
    }
	
	// 페이지 로딩 시 초기상태도 통일
    handleActionTypeChange();
});

// 0. 검색 실행 함수
function onSearchEmp() {
	loadEmpListForHrAction();
}

// 1. 사원 목록 불러오기 
function loadEmpListForHrAction() {

	const dept = document.getElementById('searchDept')?.value || '';
	const pos = document.getElementById('searchPos')?.value || '';
	const status  = document.getElementById('searchStatus')?.value || '';
	const keyword = document.getElementById('searchKeyword')?.value.trim() || '';

	const params = new URLSearchParams();
	if (dept) params.append('deptId', dept);
	if (pos) params.append('posCode', pos);
	if (status)  params.append('status', status);
	if (keyword) params.append('keyword', keyword);

	const url = apiUrl(`api/hr/employees`) + (params.toString() ? ('?' + params.toString()) : '');

	fetch(url)
		.then(res => {
			if (!res.ok) {
				throw new Error('사원 목록 요청 실패');
			}
			return res.json();
		})
		.then(data => {
			// 처음에는 그리드 생성, 이후에는 데이터만 교체
			if (!empGrid) {
				buildEmpGrid(data);
			} else {
				empGrid.resetData(data);
			}
		})
		.catch(err => {
			console.error('사원 목록 로드 실패:', err);
			alert('사원 목록을 불러오는데 실패했습니다.');
		});
}

// 2. 테이블에 사원 목록 렌더링
function buildEmpGrid(rows) {
	empGrid = new tui.Grid({
		el: document.getElementById('empGrid'),
		data: rows,
		scrollX: false,
		scrollY: true,
		bodyHeight: 500,
		rowHeaders: ['rowNum'],
		columnOptions: {
		  resizable: true
		},
		columns: [
			{ 
				header: '사번', 
				name: 'empId', 
				align: 'center' 
			},
			{ 
				header: '이름', 
				name: 'empName', 
				align: 'center' 
			},
			{ 
				header: '부서', 
				name: 'deptName', 
				align: 'center'
			},
			{ 
				header: '직급', 
				name: 'posName',
				align: 'center'
			},
			{
				header: '상태',
				name: 'statusName',
				align: 'center'
			},
			{ 
				header: '연락처', 
				name: 'mobile', 
				align: 'center'
			}
		]
	});

	// 행 클릭 → 오른쪽 정보 세팅
	empGrid.on('click', ev => {
		const row = empGrid.getRow(ev.rowKey);
		if (!row) return;

		document.getElementById('sel-emp-id').textContent = row.empId;
		document.getElementById('sel-emp-name').textContent = row.empName;
		document.getElementById('sel-emp-dept').textContent = row.deptName;
		document.getElementById('sel-emp-pos').textContent = row.posName;

		document.getElementById('empId').value = row.empId;
		
		loadApproverOptions(row.empId);
	});
}

// 3. 발령 등록 처리 함수
function handleSubmitAction(e) {
	e.preventDefault(); // 폼 기본 제출 막기
	
	const actionType = document.querySelector("select[name='actionType']").value;
	
	// DTO 구성
	const dto = {
		empId: document.getElementById("empId").value,
		actionType: actionType,
		effectiveDate: document.querySelector("input[name='effectiveDate']").value,
		leaveEndDate: document.querySelector("input[name='leaveEndDate']").value,
		toDeptId: document.querySelector("select[name='toDeptId']").value,
		toPosCode: document.querySelector("select[name='toPosCode']").value,
		actionReason: document.querySelector("textarea[name='actionReason']").value,
	};
	
	// 필수값 체크
	if (!dto.empId) {
		alert("사원을 선택하세요!");
		return;
	}
	if (!dto.actionType) {
		alert("발령 구분을 선택하세요!");
		return;
	}
	if (!dto.effectiveDate) {
		alert("발령 효력일을 입력하세요!");
		return;
	}

	// 휴직일 때는 종료 예정일 필수
	if (actionType === 'LEAVE_ACT' && !dto.leaveEndDate) {
		alert("휴직 종료 예정일을 입력하세요!")
		return;
	}	
	
	
	// 퇴직이 아닐 때만 부서/직급 필수
    if (actionType !== 'RETIRE_ACT' && actionType !== 'LEAVE_ACT' && actionType !== 'RETURN_ACT') {
      if (!dto.toDeptId) {
        alert("부서를 선택하세요!");
        return;
      }
      if (!dto.toPosCode) {
        alert("직급을 선택하세요!");
        return;
      }
    }
	
	
	// REST API POST
	fetch(apiUrl(`api/hr/actions`), {
		method: "POST",
		headers: {
			"Content-Type": "application/json",
			[csrfHeader]: csrfToken
		},
		body: JSON.stringify(dto)
	})
	.then(response => {
	    if (!response.ok) {
	        throw new Error("서버 오류 발생");
	    }
	    return response.text(); 
	})
	.then(result => {
	    alert("발령 등록이 완료되었습니다!");
	    window.location.href = "/hr/actions";  
	})
	.catch(err => {
	    console.error("발령 등록 실패:", err);
	    alert("발령 등록 중 오류가 발생했습니다.");
	});
}

// 결재선 초기화 (처음 로딩 시)
function initApproverView() {
  const a1 = document.getElementById("appr1");
  const a2 = document.getElementById("appr2");
  const a3 = document.getElementById("appr3");
  if (a1) a1.textContent = "-";
  if (a2) a2.textContent = "-";
  if (a3) a3.textContent = "-";
}

// 4. 결재자 정보 로드 (발령 대상자 empId 기반)
function loadApproverOptions(empId) {
  // 아직 사원 선택 안 했으면 비우기만
  if (!empId) {
    initApproverView();
	setApprovalLineVisible(false);
    return;
  }

  fetch(apiUrl(`api/hr/approvers?formName=인사발령신청서&empId=${encodeURIComponent(empId)}`))
    .then(res => {
      if (!res.ok) {
        throw new Error("결재자 API 호출 실패");
      }
      return res.json();
    })
    .then(list => {
      const a1 = document.getElementById("appr1");
      const a2 = document.getElementById("appr2");
      const a3 = document.getElementById("appr3");

      initApproverView(); // 먼저 초기화

      // list[0] = 1차, list[1] = 2차, list[2] = 3차
      if (list[0] && a1) a1.textContent = `${list[0].empName} (${list[0].deptName})`;
      if (list[1] && a2) a2.textContent = `${list[1].empName} (${list[1].deptName})`;
      if (list[2] && a3) a3.textContent = `${list[2].empName} (${list[2].deptName})`;
	  
	  setApprovalLineVisible(true);
    })
    .catch(err => {
      console.error("결재자 로드 실패:", err);
      // 에러 난 경우도 깔끔하게 표시
      const a1 = document.getElementById("appr1");
      if (a1) a1.textContent = "결재선 로드 실패";
	  
	  setApprovalLineVisible(false);
    });
}

// 5. 발령타입
function handleActionTypeChange() {
    const actionType = document.querySelector("select[name='actionType']").value;
    const deptSelect = document.querySelector("select[name='toDeptId']");
    const posSelect = document.querySelector("select[name='toPosCode']");
	const leaveEndInput = document.querySelector("input[name='leaveEndDate']");

    if (!deptSelect || !posSelect || !leaveEndInput) return;

	// 퇴직 및 휴직일 때 부서/직급 비활성화
    if (actionType === 'RETIRE_ACT' || actionType === 'LEAVE_ACT' || actionType === 'RETURN_ACT') {
        deptSelect.disabled = true;
        posSelect.disabled = true;
        deptSelect.value = '';
        posSelect.value = '';
    } else {
        // 다른 발령일 때 되돌리기
        deptSelect.disabled = false;
        posSelect.disabled = false;
    }
	
	// 휴직일 때만 휴직 종료 예정일 활성화
	if (actionType === 'LEAVE_ACT') {
		leaveEndInput.disabled = false;
	} else {
		leaveEndInput.disabled = true;
		leaveEndInput.value = "";
	}
	
}

// 6. 결재선 박스 show/hide
function setApprovalLineVisible(show) {
	const box = document.getElementById('approvalLineBox');
	if (!box) return;
	
	box.style.display = show ? 'block' : 'none';
}

