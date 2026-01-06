// hrList.js
// 인사 발령 목록 그리드 + 검색/페이징 (Toast Grid 클라이언트 페이징)

let hrGrid = null;
const pageSize = 10;

document.addEventListener('DOMContentLoaded', () => {

  // 1) 그리드 먼저 세팅
  initHrGrid();

  // 2) 검색 버튼
  const btnSearch = document.getElementById('btnSearch');
  if (btnSearch) {
    btnSearch.addEventListener('click', () => loadHrActionList());
  }

  // 3) 발령 구분 선택 시 자동 검색
  const actionTypeSelect = document.getElementById('actionType');
  if (actionTypeSelect) {
    actionTypeSelect.addEventListener('change', () => loadHrActionList());
  }

  // 4) 키워드 엔터 시 검색
  const keywordInput = document.getElementById('keyword');
  if (keywordInput) {
    keywordInput.addEventListener('keydown', (e) => {
      if (e.key === 'Enter') {
        e.preventDefault();
        loadHrActionList();
      }
    });
  }
  
  // 5) 날짜 범위 제약
  const startDateInput = document.getElementById('startDate');
  const endDateInput   = document.getElementById('endDate');
  
  if (startDateInput && endDateInput) {
	
	// 시작일 변경 시, 종료일 최소값 설정 및 잘못된 값 보정
	startDateInput.addEventListener('change', () => {
		if (!startDateInput.value) {
			endDateInput.min = '';
			return;
		}
		endDateInput.min = startDateInput.value;
		
		if (endDateInput.value && endDateInput.value < startDateInput.value) {
			endDateInput.value = startDateInput.value;
		}
	});
	
	// 종료일 변경 시, 시작일 최대값 설정 + 잘못된 값 보정
	endDateInput .addEventListener('change', () => {
		if (!endDateInput.value) {
			startDateInput.max = '';
			return;
		}
		startDateInput.max = endDateInput.value;
		
		if (startDateInput.value && startDateInput.value > endDateInput.value) {
			startDateInput.value = endDateInput.value;
		}
	});
  }

  // 6) 초기 로딩
  loadHrActionList();
});

// ===============================
// 1. 발령 목록 가져오기 (전체 + 클라이언트 페이징)
// ===============================
function loadHrActionList() {
  const keyword   = document.getElementById('keyword')?.value || '';
  const actionType = document.getElementById('actionType')?.value || '';
  const startDate  = document.getElementById('startDate')?.value || '';
  const endDate    = document.getElementById('endDate')?.value || '';
  
  if (startDate && endDate && endDate < startDate) {
      alert('종료일은 시작일보다 빠를 수 없습니다.');
      return;
  }

  const params = new URLSearchParams({
    keyword,
    actionType,
    startDate,
    endDate
  });

  fetch(apiUrl(`api/hr/actions?`) + params.toString())
    .then(res => {
      console.log('응답 상태:', res.status, res);
      if (!res.ok) {
        throw new Error('HTTP 오류 상태 코드: ' + res.status);
      }
      return res.json();
    })
    .then(data => {
      console.log('받은 데이터:', data);  // ← 여기 찍히는 게 바로 Array([...])

      // 컨트롤러가 List<HrActionDTO>를 리턴하므로 data는 배열 자체
      const rows = Array.isArray(data) ? data : [];
      hrGrid.resetData(rows);
    })
    .catch(err => {
      console.error('발령 목록 조회 에러:', err);
      alert('인사 발령 목록 불러오기 실패');
    });
}

// ===============================
// 2. Toast Grid 생성 함수
// ===============================
function initHrGrid() {

  if (hrGrid) {
    hrGrid.destroy();
  }

  hrGrid = new tui.Grid({
    el: document.getElementById('grid'),
    rowHeaders: ['rowNum'],
    scrollX: true,
    scrollY: true,
    columnOptions: {
      resizable: true
    },
    pageOptions: {
      useClient: true,   // 클라이언트 페이징 사용
      perPage: pageSize  // 한 페이지당 10건
    },
    columns: [
      { 
        header: '사원번호',
        name: 'empId',
        align: 'center'
      },
      { 
        header: '성명',
        name: 'empName',
        align: 'center'
      },
      { 
        header: '발령구분',
        name: 'actionTypeName',  
        align: 'center'
      },
      { 
        header: '발령일자',
        name: 'effectiveDate',  
        align: 'center'
      },
      { 
        header: '부서(이전)',
        name: 'fromDeptName',
        align: 'center'
      },
      { 
        header: '부서(이후)',
        name: 'toDeptName',
        align: 'center'
      },
      { 
        header: '직급(이전)',
        name: 'fromPosName',
        align: 'center'
      },
      { 
        header: '직급(이후)',
        name: 'toPosName',
        align: 'center'
      }
      // { 
      //   header: '결재 상태',
      //   name: 'status',
      //   align: 'center'
      // }
    ]
  });
}
