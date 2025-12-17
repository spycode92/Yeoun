// 재고조회 js
// 전역변수
let inventoryHistoryGrid; // 그리드 객체 변수
let inventoryHistoryData = []; // 그리드로 그려지는 데이터 저장

function showSpinner() {
	document.getElementById('loading-overlay').style.display = 'flex';
}
function hideSpinner() {
	document.getElementById('loading-overlay').style.display = 'none';
}
// 문서 로딩 후 시작
document.addEventListener('DOMContentLoaded', async function () {
	
	showSpinner();
	// 기본 날짜 세팅
	const startDateInput = document.getElementById('startDate');
	const endDateInput = document.getElementById('endDate');
	
	const today = new Date();
	const weekAgo = new Date();
	weekAgo.setDate(today.getDate() - 7);

	const formatDate = (d) => {
		const y = d.getFullYear();
		const m = String(d.getMonth() + 1).padStart(2, '0');
		const da = String(d.getDate()).padStart(2, '0');
		return `${y}-${m}-${da}`;
	};

	startDateInput.value = formatDate(weekAgo);
	endDateInput.value = formatDate(today);
	
	initGrid();

	try {
		// 3) 최초 로딩: 현재 화면 검색조건으로 서버 조회
		inventoryHistoryData = await fetchInventoryHistoryData();
		inventoryHistoryGrid.resetData(inventoryHistoryData);
		inventoryHistoryGrid.sort('createdDate', false);
	} catch (e) {
		console.error(e);
		alert('재고내역 조회 중 오류가 발생했습니다.');
	} finally {
		hideSpinner();
	}
	
	// 검색버튼 이벤트함수
	const btnSearch = document.getElementById('btnSearchHistory');

	btnSearch.addEventListener('click', async (event) => {
		event.preventDefault(); // 폼제출 막기
		
		showSpinner();
		
		try {
		  inventoryHistoryData = await fetchInventoryHistoryData();
		  inventoryHistoryGrid.resetData(inventoryHistoryData);
		  inventoryHistoryGrid.sort('createdDate', false);
		} catch (e) {
		  console.error(e);
		  alert('재고내역 검색 중 오류가 발생했습니다.');
		} finally {
		  hideSpinner();
		}
	});
	hideSpinner();
});


// 검색조건 생성 함수
function buildSearchData() {
	const startDate = document.getElementById('startDate').value;          // "yyyy-MM-dd"
	const endDate   = document.getElementById('endDate').value;
  	const workType  = document.getElementById('searchHistoryType').value;  // 기존 select
	const keyword   = document.getElementById('searchKeyword').value.trim();

	return {
		startDate: startDate || null,       // 서버 DTO: LocalDate startDate
		endDate: endDate || null,           // 서버 DTO: LocalDate endDate
		workType: workType || null,         // 서버 DTO: String workType
		searchKeyword: keyword || null      // 서버 DTO: String searchKeyword
	};
}

// 재고내역 데이터 정보 가져오기
async function fetchInventoryHistoryData() {
	const searchData = buildSearchData();
	
	const response = 
		await fetch('/api/inventories/historys', {
			method: 'POST',
			headers: {
				[csrfHeader]: csrfToken,
				'Content-Type': 'application/json'
			},
			body: JSON.stringify(searchData)	
		});
//	console.log(response);
	if (!response.ok) {
		throw new Error('재고내역 데이터를 가져올 수 없습니다.')
	}
	return await response.json();
} 


// 그리드 설정
function initGrid() {
	const inventoryHistoryGridEl = document.getElementById('historyGrid');
	const Grid = tui.Grid;
	// 1. 그리드 한글 언어셋 설정 (필터 및 각종 텍스트 한글화)
	Grid.setLanguage('ko', {
	    display: {
	        noData: '데이터가 없습니다.',
	        loadingData: '데이터를 불러오는 중입니다.',
	        resizeHandleGuide: '마우스 드래그를 통해 너비를 조정할 수 있습니다.',
	    },
	    net: {
	        confirmCreate: '생성하시겠습니까?',
	        confirmUpdate: '수정하시겠습니까?',
	        confirmDelete: '삭제하시겠습니까?',
	        confirmModify: '저장하시겠습니까?',
	        noDataToCreate: '생성할 데이터가 없습니다.',
	        noDataToUpdate: '수정할 데이터가 없습니다.',
	        noDataToDelete: '삭제할 데이터가 없습니다.',
	        noDataToModify: '수정할 데이터가 없습니다.',
	        failResponse: '데이터 요청 중에 에러가 발생하였습니다.'
	    },
	    filter: {
	        // 문자열 필터 옵션
	        contains: '포함',
	        eq: '일치',
	        ne: '불일치',
	        start: '시작 문자',
	        end: '끝 문자',
	        
	        // 날짜/숫자 필터 옵션
	        after: '이후',
	        afterEq: '이후 (포함)',
	        before: '이전',
	        beforeEq: '이전 (포함)',

	        // 버튼 및 기타
	        apply: '적용',
	        clear: '초기화',
	        selectAll: '전체 선택'
	    }
	});
	inventoryHistoryGrid = new Grid({
		el: inventoryHistoryGridEl,
		bodyHeight: 'auto',
		pageOptions: {
		    useClient: true,
		    perPage: 20
		},
		useClientSort: true,
		columnOptions: {
			resizable: true
		},
		columns: [
//       		{ 
//		        header: '내역', name: 'ivHistoryId', width: 80, align: 'center',
//		        formatter: ({ value }) => value || '-'
//			},
            { 
                header: 'LOT 번호', name: 'lotNo', minWidth: 160, align: 'center', filter: 'text'
            },
            { 
                header: '상품명', name: 'itemName', minWidth: 140, filter: 'text'
            },
            { 
                header: '작업유형', name: 'workType', width: 100, align: 'center',
				filter: 'select',
                formatter: ({ value }) => {
                    const types = {
                        'INBOUND': '입고',
                        'OUTBOUND': '출고', 
                        'MOVE': '이동',
                        'DISPOSE': '폐기',
                        'INC': '증가',
                        'DEC': '감소'
                    };
                    return types[value] || value || '-';
                }
            },
            { 
                header: '작업자', name: 'empName', minWidth: 140, filter: 'select'
            },
            { 
                header: '이전위치', name: 'prevLocationName', width: 100, align: 'center',
                formatter: ({ value }) => value || '-'
            },
            { 
                header: '현재위치', name: 'currentLocationName', width: 100, align: 'center'
            },
            { 
                header: '이동수량', name: 'moveAmount', width: 90, align: 'right',
                formatter: ({ value }) => value !== null && value !== undefined ? value.toLocaleString() : '-'
            },
			{
			    header: '변동수량', name: 'changeAmount', width: 100, align: 'right',
			    formatter: ({ row }) => {
			        const prev = row.prevAmount !== null && row.prevAmount !== undefined ? row.prevAmount : 0;
			        const curr = row.currentAmount !== null && row.currentAmount !== undefined ? row.currentAmount : 0;
			        const change = Math.abs(prev - curr);
			        return change > 0 ? change.toLocaleString() : '-';
			    }
			},
            { 
                header: '작업일시', name: 'createdDate', minWidth: 140,
				sortable:true,
				sortingType: 'desc',      // 기본 정렬 방향
				sortComparator: (a, b) => {
				  // a, b 는 문자열: "2025-12-04T08:46:50.830733"
				  const da = new Date(a);
				  const db = new Date(b);
				  return da - db;         // 오름차순 기준 (Grid에서 descending 옵션으로 뒤집음)
				},
				formatter: ({ value }) => {
				    if (!value) return '-';
				    const date = new Date(value);
				    const y = date.getFullYear().toString().slice(-2);
				    const m = (date.getMonth() + 1).toString().padStart(2, '0');
				    const d = date.getDate().toString().padStart(2, '0');
				    const hh = date.getHours().toString().padStart(2, '0');
				    const mm = date.getMinutes().toString().padStart(2, '0');
				    const ss = date.getSeconds().toString().padStart(2, '0');
				    return `${y}-${m}-${d} ${hh}:${mm}:${ss}`;
				}
            },
            {
                header: '사유', name: 'reason', minWidth: 120,
                formatter: ({ value }) => value || '-'
            }
        ]
    });
}

const startDateInput = document.getElementById('startDate');
const endDateInput = document.getElementById('endDate');

startDateInput.addEventListener('change', () => {
    const start = new Date(startDateInput.value);
    const end = new Date(endDateInput.value);

    // startDate가 endDate 이후일 때 endDate를 startDate + 1일로 조정
    if (startDateInput.value && endDateInput.value && start >= end) {
        const newEnd = new Date(start);
        newEnd.setDate(newEnd.getDate() + 1);
        endDateInput.value = newEnd.toISOString().substring(0,10); // yyyy-mm-dd
    }
});

endDateInput.addEventListener('change', () => {
    const start = new Date(startDateInput.value);
    const end = new Date(endDateInput.value);

    // endDate가 startDate 이전일 때 startDate를 endDate - 1일로 조정
    if (startDateInput.value && endDateInput.value && end <= start) {
        const newStart = new Date(end);
        newStart.setDate(newStart.getDate() - 1);
        startDateInput.value = newStart.toISOString().substring(0,10); // yyyy-mm-dd
    }
});


