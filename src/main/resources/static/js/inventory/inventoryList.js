// 재고조회 js
// 전역변수
let inventoryGrid; // 그리드 객체 변수
let inventoryData = []; // 그리드로 그려지는 데이터 저장
let locationInfo = [];

function showSpinner() {
	document.getElementById('loading-overlay').style.display = 'flex';
}
function hideSpinner() {
	document.getElementById('loading-overlay').style.display = 'none';
}

// 문서 로딩 후 시작
document.addEventListener('DOMContentLoaded', async function () {
	
	// 창고정보 저장
	locationInfo = await getLocationInfo();
	inputLocationInfo(locationInfo);

	initGrid();
	//최초로딩
	const firstSearchData = getSearchData();
	const firstData = await fetchInventoryData(firstSearchData);
	
	// 받아온 데이터로 그리드 생성
	inventoryGrid.resetData(firstData);
	inventoryGrid.sort('ibDate', true);
	// 그리드생성한 재고데이터를 저장
	inventoryData = firstData;	
	hideSpinner();
});

// 검색 데이터 설정(검색, 상세검색 입력값으로 requestBody생성)
async function getSearchData() {
	function addDefaultTime(dateStr, timeStr = "00:00:00") {
		if(!dateStr) return '';
		// 시간이 없을 경우 00:00:00 추가
		if(dateStr.length === 10) {
			return `${dateStr} ${timeStr}`;
		}
		//시간이 있으면 다시반환
		return dateStr;
	}
	
	return {
		lotNo: document.getElementById('searchLotNo').value.trim(),
		prodName: document.getElementById('searchProdName').value.trim(),
		itemType: document.getElementById('searchCategory')?
			document.getElementById('searchCategory').value:'',
		zone: document.getElementById('searchZone')?
			document.getElementById('searchZone').value:'',
		rack: document.getElementById('searchRack')?
			document.getElementById('searchRack').value:'',
		status: document.getElementById('searchStatus')?
			document.getElementById('searchStatus').value:'',
		ibDate: document.getElementById('searchDate')?
			addDefaultTime(document.getElementById('searchDate').value,"00:00:00"):'',
		expirationDate: document.getElementById('searchExpireDate')?
			addDefaultTime(document.getElementById('searchExpireDate').value,"00:00:00"):''
	};
}

// 검색데이터에 기반하여 재고 데이터 정보 가져오기
async function fetchInventoryData(searchData) {
	const response = 
		await fetch('/api/inventories', {
			method: 'POST',
			headers: {
				[csrfHeader]: csrfToken,
				'Content-Type': 'application/json'
			},
			body: JSON.stringify(searchData)
			
		});
//	console.log(response);
	if (!response.ok) {
		throw new Error('재고데이터를 가져올 수 없습니다.')
	}
	return await response.json();
} 

// 검색버튼 이벤트함수
const btnSearch = document.getElementById('btnSearch');

btnSearch.addEventListener('click', async () => {
	event.preventDefault(); // 폼제출 막기
	
	showSpinner();
	
	const searchData = await getSearchData();
	const gridData = await fetchInventoryData(searchData)
	// 받아온 데이터로 그리드 생성
	inventoryGrid.resetData(gridData);
	// 그리드생성한 재고데이터를 저장
	inventoryData = gridData;	
	
	hideSpinner();

});

// 그리드 설정
function initGrid() {
	const inventoryGridEl = document.getElementById('inventoryGrid');
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
	
	
	inventoryGrid = new Grid({
		el: inventoryGridEl,
		bodyHeight: 'auto',
//		rowHeaders:['rowNum'],
		pageOptions: {
		    useClient: true,  // 클라이언트 사이드 페이징
		    perPage: 20       // 페이지당 20개 행
		},	
		columns: [
		  { header: 'LOT 번호',  name: 'lotNo',    minWidth: 220 },
		  { header: '상품명',    name: 'prodName', minWidth: 180 },
		  { header: '재고량',    name: 'ivAmount', width: 80, align: 'right' },
		  { header: '출고예정',    name: 'expectObAmount', width: 80, align: 'right' },
		  {
		    header: '위치', name: 'location', minWidth: 80,
		    formatter: ({ row }) => {
		      const z  = row.zone  || '';
		      const r  = row.rack  || '';
		      const rr = row.rackRow || '';
		      const rc = row.rackCol || '';
		      
		      return [z, r, rr, rc].filter(v => v).join('-'); // 예: "A-01-B-01"
		    }
		  },
		  { header: 'Zone',      name: 'zone',     minWidth: 60, hidden: true },
		  { header: 'Rack',      name: 'rack',     minWidth: 60, hidden: true },
		  { header: 'Row',       name: 'rackRow',  minWidth: 60, hidden: true },
		  { header: 'Col',       name: 'rackCol',  minWidth: 60, hidden: true },
		  { header: '입고일',    name: 'ibDate',   minWidth: 120, 
			formatter: ({ value }) => value ? value.substring(0, 16) : ''
		  },
		  { header: '유통기한',  name: 'expirationDate', minWidth: 120, 
			formatter: ({ value }) => value ? value.substring(0, 10) : '없음'
		  },
		  { header: '상태',      name: 'ivStatus', width: 80, 
			formatter: ({ value }) => {
				switch(value) {
					case 'NORMAL'          : return '정상';
					case 'EXPIRED'         : return '만료';
					case 'DISPOSAL_WAIT': return '임박';
					default:     return value ?? '';
			    }
			}
		  },
		  {
		  	header: '상세',      name: "btn", width: 100, align: "center",
		  	formatter: (cellInfo) => "<button type='button' class='btn-detail btn-primary btn-sm' data-row='${cellInfo.rowKey}' >상세</button>"
		  }
		]
	});
	// 상세보기 버튼 이벤트
	inventoryGrid.on("click", (event) => {
		if(event.columnName == "btn") {
			const target = event.nativeEvent.target;
			if (target && target.tagName === "BUTTON") {
				
				const rowData = inventoryGrid.getRow(event.rowKey);
				
				// 같은 LOT, 같은 상품(itemId)만 필터
				const sameLotList = inventoryData.filter(item =>
					item.lotNo === rowData.lotNo &&
					item.itemId === rowData.itemId &&
					// 현재 행은 제외
					item.ivId !== rowData.ivId
				);

				openDetailModal(rowData, sameLotList);
			}
		}
	});
}


// 창고 ZONE, RACK, ROW, COL 가져오기
async function getLocationInfo() {
	const response = 
		await fetch('/api/inventories/locations', {
			method: 'GET',
			headers: {
				[csrfHeader]: csrfToken,
				'Content-Type': 'application/json'
			}
		});
		
		console.log(response);
		if (!response.ok) {
			throw new Error('창고정보를 가져올 수 없습니다.')
		}
		return await response.json();
}

// 창고 정보의 유니크값 뽑아내기
function getUniqueValues(list, key) {
    return [...new Set(list.map(item => item[key]))]; 
}
// 셀렉트박스 채우기 함수
function fillSelect(selectEl, values) {
	selectEl.length = 1; // 첫번째 option만 남김
	
	values.forEach(v => {
		const opt = document.createElement('option');
		opt.value = v;
		opt.textContent = v;
		selectEl.appendChild(opt);
	})
}

//창고 정보 셀렉트박스에 집어넣기
function inputLocationInfo(locationInfo) {
	const zones = getUniqueValues(locationInfo, 'zone');
	const racks  = getUniqueValues(locationInfo, 'rack');
	const zoneSelect = document.getElementById('searchZone')
	const rackSelect = document.getElementById('searchRack')
	
	fillSelect(zoneSelect, zones);
	fillSelect(rackSelect, racks);
}

// 상세검색버튼
document.getElementById('btnToggleAdvanced').addEventListener('click', function () {
    const advancedArea = document.getElementById('advancedSearch');
    const icon = this.querySelector('i');

    if (advancedArea.style.display === 'none') {
        advancedArea.style.display = 'block';
        icon.classList.replace('bx-chevron-down', 'bx-chevron-up');
    } else {
        advancedArea.style.display = 'none';
        icon.classList.replace('bx-chevron-up', 'bx-chevron-down');
    }
});