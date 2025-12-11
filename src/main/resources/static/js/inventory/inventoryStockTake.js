// 전역변수
let locationInfo; //로케이션 정보
let stockTakeGrid;//그리드
let isReviewMode = true; // 실사 입력모드 구분 false: 실사입력, true: 확인

// 문서로드
document.addEventListener('DOMContentLoaded', async function () {
	const zone = document.getElementById('zone');
	// 창고정보 저장
	locationInfo = await getLocationInfo();
//	console.log(locationInfo);

	// 로케이션에서 존정보만 추출하여 오름차순정렬
	const zones = sortNumericStrings(getUniqueValues(locationInfo, 'zone'));
	// 존 셀렉트박스 채우기
	fillSelect(zone, zones);
	
	// zones가 있으면 0번째로 기본 세팅 + 하위 셀렉트 자동 세팅
	if (zones.length > 0) {
	    zone.value = zones[0];
	    applyZoneSelection(zones[0]);
	}
	
	// 그리드 초기화
	initStockTakeGrid();
	
	// 입력완료 버튼 이벤트
	const btnComplete = document.getElementById('btnCompleteInput');
	if (btnComplete) {
	    btnComplete.addEventListener('click', onCompleteInput);
	}
	// 일괄반영 버튼 이벤트
	const btnApplySel = document.getElementById('btnApplySelected');
	if (btnApplySel) {
	    btnApplySel.addEventListener('click', () => {
			const rowKeys = stockTakeGrid.getCheckedRowKeys();
			if (!rowKeys || rowKeys.length === 0) {
			    alert('반영할 행을 체크해주세요.');
			    return;
			}
			applyMultipleRows(rowKeys);
		});
	}
});

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
	selectEl.length = 0; 
	
	values.forEach(v => {
		const opt = document.createElement('option');
		opt.value = v;
		opt.textContent = v;
		selectEl.appendChild(opt);
	})
}

// 숫자 형태 문자열(01,02,10)을 올바르게 오름차순 정렬
function sortNumericStrings(arr) {
    return arr.slice().sort((a, b) => a.localeCompare(b, 'en', { numeric: true }));
}

// zone 기준으로 rack, row, col 세팅
function applyZoneSelection(selectedZone) {
    const rack   = document.getElementById('rack');
    const row    = document.getElementById('row');
    const column = document.getElementById('column');
	
	// 선택된 zone이 없으면 빈배열추가
    if (!selectedZone) {
        fillSelect(rack, []);
        fillSelect(row, []);
        fillSelect(column, []);
        return;
    }
	
	// 로케이션 정보에서 선택된 존으로 데이터 필터
    const byZone = locationInfo.filter(loc => loc.zone === selectedZone);
	// 선택된 존으로 필터된 데이터에서 중복제거한 랙 값 추출후 오름차순 정렬
    const racks  = sortNumericStrings(getUniqueValues(byZone, 'rack'));
	
	// 랙 셀렉트박스 채우기
    fillSelect(rack, racks);
	
    fillSelect(row, []);
    fillSelect(column, []);
	
	// 랙이 존재하면 첫번째 선택후 로우,컬럼 채우기함수 실행
    if (racks.length > 0) {
        const firstRack = racks[0];
        document.getElementById('rack').value = firstRack;
        applyRackSelection(selectedZone, firstRack);
    }
}

// zone + rack 기준으로 row, col 세팅
function applyRackSelection(selectedZone, selectedRack) {
    const row    = document.getElementById('row');
    const column = document.getElementById('column');
	
	// 선택된 zone이나 rack이 없으면 row, column 비우기
    if (!selectedZone || !selectedRack) {
        fillSelect(row, []);
        fillSelect(column, []);
        return;
    }
	
	// locationh정보에서 선택한 존, 랙의 정보 필터
    const byZoneRack = locationInfo.filter(
        loc => loc.zone === selectedZone && loc.rack === selectedRack
    );
	
	// 선택된 존,랙으로 필터된 데이터에서 유니크한 rows, cols 추출후 오름차순정렬
	const rows = sortNumericStrings(getUniqueValues(byZoneRack, 'rackRow'));
	const cols = sortNumericStrings(getUniqueValues(byZoneRack, 'rackCol'));
	
	// 로우 컬럼 셀렉트박스 채우기
    fillSelect(row, rows);
    fillSelect(column, cols);
}


// zone 선택시 rack 값 채우기
document.getElementById('zone').addEventListener('change', () => {
	const zone   = document.getElementById('zone');
	const selectedZone = zone.value;
	
	applyZoneSelection(selectedZone);	
});

// Rack 선택시 row, col값 채우기
document.getElementById('rack').addEventListener('change', () => {
	const zone   = document.getElementById('zone');
	const rack   = document.getElementById('rack');
	
	const selectedZone = zone.value;
	const selectedRack = rack.value;

	applyRackSelection(selectedZone, selectedRack);
});


// 현재 location값 기준으로 locationId를 찾아 반환하는함수
function getLocationIdByPosition() {
    const zone   = document.getElementById('zone').value;
    const rack   = document.getElementById('rack').value;
    const row    = document.getElementById('row').value;
    const column = document.getElementById('column').value;
	
	// 하나라도 값이 없을 경우
    if (!zone || !rack || !row || !column) {
        return null;
    }
	
	// 선택한 zone,rack,row,col의 로케이션 정보 선택
    const targetLoc = locationInfo.find(loc =>
        loc.zone    === zone &&
        loc.rack    === rack &&
        loc.rackRow === row &&
        loc.rackCol === column
    );
	
	//선택한 값의 로케이션정보가 없을경우
    if (!targetLoc) {
        return null;
    }

    return targetLoc.locationId;
}

//조회버튼 동작함수
const btnSelect = document.getElementById('btnSelect');

btnSelect.addEventListener('click', async () => {
	// locationId
	const locationId = getLocationIdByPosition();
	// 존,랙,로우,컬럼값저장
	const zone = document.getElementById('zone').value;
	const rack = document.getElementById('rack').value;
	const row = document.getElementById('row').value;
	const column = document.getElementById('column').value;
	// 선택한 로케이션 이름설정
	const selectLocation = `${zone}-${rack}-${row}-${column}`
	// 로케이션이름을 보여줄 위치
	const head = document.getElementById('stockTakeHead');
	
	// 선택한 로케이션의 재고목록 저장
	const rawList  = await getLocationInventory(locationId);
	
	// 재고가 존재하지 않을때
	if(rawList.length < 1) {
		alert("해당위치에 재고가 없습니다!");
		return;
	}
	
	// 재고가 존재할 때
	// 어느 위치의 실사를 하는지 실사위치 표시
	head.innerHTML= `실사 위치 : ${selectLocation}`;
	
	// 실사도중 조회를 눌렀을 경우 한번더 확인 
	if(!isReviewMode) {
		const ok = confirm('이전에 실사 확인을 완료한 데이터가 있습니다. 새로 조회하시겠습니까?');
       if (!ok) return;
	}
	
	// 그리드 초기화
	if (stockTakeGrid) {
	    stockTakeGrid.enable();     // 모든 row + checkbox 활성화[web:5]
	    stockTakeGrid.uncheckAll(); // 체크 상태도 초기화
	}
	
	// 실사용 필드(count/diff) 초기화
	const locationIvInfo = rawList.map(row => ({
		...row,
	    countQty: '',
	    diffQty: null
	}));
	
	loadStockTakeData(locationIvInfo);
});

// 조회시 리뷰모드로 적용, 데이터로 그리드 새로그리기
function loadStockTakeData(locationIvInfo) {
    isReviewMode = false;   // 조회하면 항상 입력모드로 리셋
    stockTakeGrid.resetData(locationIvInfo);
	
	// 재고수량, 출고예정수량, 차이 컬럼 숨기고, 실사수량은 다시 editable
	const cols = stockTakeGrid.getColumns().map(col => {
	    if (['ivAmount', 'expectObAmount', 'diffQty'].includes(col.name)) {
	        return { ...col, hidden: true };
	    }
	    if (col.name === 'countQty') {
	        return { ...col, editor: { type: 'text' } };
	    }
	    return col;
	});
	// 컬럼 설정 적용
	stockTakeGrid.setColumns(cols);
}

// locationId로 해당위치의 재고리스트 정보 가져오기
async function getLocationInventory(locationId) {
	const response = 
		await fetch(`/api/inventories/${locationId}`, {
			method: 'GET',
			headers: {
				[csrfHeader]: csrfToken,
				'Content-Type': 'application/json'
			}
		});
		if (!response.ok) {
			throw new Error('해당위치의 재고정보를 가져올 수 없습니다.')
		}
		return await response.json();
}

// 그리드 초기화 함수
function initStockTakeGrid() {
    const stockGridEl = document.getElementById('stockTakeGrid');
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
	
    stockTakeGrid = new Grid({
        el: stockGridEl,
        bodyHeight: 'auto',
 		rowHeaders: ['rowNum', 'checkbox'],
        pageOptions: {
            useClient: true,
            perPage: 20
        },
        columns: [
            // 기본 식별 정보
            { header: 'LOT 번호',   name: 'lotNo',    minWidth: 180 },
            { header: '상품명',     name: 'prodName', minWidth: 180 },

            // 시스템 재고
            { header: '재고량',     name: 'ivAmount', width: 80, align: 'right', hidden: true },
            { header: '출고예정',   name: 'expectObAmount', width: 80, align: 'right', hidden: true },

            // 실사수량 입력
            {
                header: '실사수량',
                name: 'countQty',
                width: 100,
                align: 'right',
                editor: { type: 'text' },
				validation: {
				    required: true,    // 빈값 금지
				    dataType: 'number',// 숫자만 허용
				    min: 0             // 0 이상
				}
				
            },
			{
			    header: '차이(실사-장부)',
			    name: 'diffQty',
			    width: 110,
			    align: 'right',
			    hidden: true
			},
			{
			    header: '반영',
			    name: 'btn',
			    width: 80,
			    align: 'center',
			    formatter: (cellInfo) => {
			        return `<button type="button" class="btn-row-apply btn-primary btn-sm" data-row="${cellInfo.rowKey}">반영</button>`;
			    }
			},
            { header: 'ivId',       name: 'ivId', hidden: true },
            { header: '품목코드',   name: 'itemId', hidden: true },
            { header: '로케이션ID', name: 'locationId', hidden: true }
        ]
    });
	// 단건 반영버튼 함수
	stockTakeGrid.on('click', (ev) => {
	    if (ev.columnName === 'btn') {
	        const target = ev.nativeEvent.target;
	        if (target && target.tagName === 'BUTTON') {
	            onRowApply(ev.rowKey);
	        }
	    }
	});
}

// 실사 입력완료동작함수
function onCompleteInput() {
    const data = stockTakeGrid.getData();
	
    if (data.length === 0) {
        alert('실사 대상 데이터가 없습니다.');
        return;
    }
	
    // 실사수량 미입력 체크
    const hasInput = data.some(row => row.countQty !== '' && row.countQty != null);
    if (!hasInput) {
        alert('최소 한 개 이상의 실사 수량을 입력해주세요.');
        return;
    }
	
	// 유효성 안맞는 셀 목록 반환
	const errors = stockTakeGrid.validate();
	if (errors.length > 0) {
	    alert('실사 수량에 숫자만 입력 가능하며, 0 이상이어야 합니다.');
	    return;
	}

    // 실재고 - 전산재고 계산
    data.forEach(row => {
        const book  = Number(row.ivAmount || 0);
        const count = Number(row.countQty || 0);
        row.diffQty = count - book;
    });
	
	// 컬럼 설정세팅
    const cols = stockTakeGrid.getColumns().map(col => {
		// 재고량/출고예정량/차이 보이기
        if (['ivAmount', 'expectObAmount', 'diffQty'].includes(col.name)) {
            return { ...col, hidden: false };
        }
		// 실사수량 수정 막기
        if (col.name === 'countQty') {
            const { editor, ...rest } = col; // editor 제거
            return rest;
        }
        return col;
    });
	
	// 설정한 컬럼값 세팅
    stockTakeGrid.setColumns(cols);
	// 입력한 실제고값, 계산한 차이 데이터 그리드 입력
    stockTakeGrid.resetData(data);

    isReviewMode = true;
}

// 유효성 검사 및 API 반영 함수
async function applyStockTakeRow(rowKey) {
    const row = stockTakeGrid.getRow(rowKey);
	
    if (!isReviewMode) {
        alert('먼저 입력완료를 눌러 실사 수량과 장부 수량의 차이를 확인해주세요.');
        return false;  // 처리 안됨
    }

    if (!row) return false;

	// 실사 수량이 출고예정 수량보다 작은지 검사
	if((row.countQty || 0) < (row.expectObAmount || 0)) {
		alert("실사 수량이 출고예정 수량보다 적어 변경할 수 없습니다.")
		return false;
	}
	
    const req = buildAdjustRequestFromRow(row);
    if (!req) {
        alert('차이가 0인 행은 반영할 필요가 없습니다.');
        return false;
    }
	// 단건 수량 조정 실행
    const response = await fetch(`/api/inventories/${req.ivId}/adjustQty`, {
        method: 'POST',
        headers: { [csrfHeader]: csrfToken, 'Content-Type': 'application/json' },
        body: JSON.stringify(req)
    });

    if (!response.ok) {
        alert('실사 반영 중 오류가 발생했습니다.');
        return false;
    }

    // 반영 후 상태 업데이트
    await updateRowAfterApply(rowKey);

    return true;  // 성공 처리됨
}

// 반영 후 상태 업데이트 함수
async function updateRowAfterApply(rowKey) {
    const row = stockTakeGrid.getRow(rowKey);
    if (!row) return;
	// 실사수량
    const stockTakeQty = row.countQty || 0;
	// 재고수량을 실사수량으로 변경
	await stockTakeGrid.setValue(rowKey, 'ivAmount', stockTakeQty);
	// 차이를 0으로 변경
    await stockTakeGrid.setValue(rowKey, 'diffQty', 0);
	// 체크박스 체크해제 
    await stockTakeGrid.uncheck(rowKey);
	
	// 버튼 반영완료후 disabled로 비활성화
    const btnCol = stockTakeGrid.getColumn('btn');
    if (btnCol) {
		await stockTakeGrid.setValue(
		    rowKey,
		    'btn',
		    '<button type="button" class="btn btn-secondary btn-sm" disabled>반영완료</button>'
		);
    }
	
	// 행, 체크박스 비활성화
    await stockTakeGrid.disableRow(rowKey, true);
}

// 단건 반영 이벤트 핸들러
async function onRowApply(rowKey) {
    await applyStockTakeRow(rowKey);
}

// 일괄 반영 함수
async function applyMultipleRows(rowKeys) {
	let success = 0;
	let fail = 0;
	
	//단건 반영 재사용
	for (const rowKey of rowKeys) {
	    const ok = await applyStockTakeRow(rowKey);
	    if (ok) success++;
	    else fail++;
	}

	alert(`실사 반영 완료\n성공: ${success}건, 실패: ${fail}건`);
}

// 재고수량 조절의 DTO를 재사용하기위한 data빌드함수
function buildAdjustRequestFromRow(row) {
	//차이가 없으면 조정 불필요
    const diff = Number(row.diffQty || 0);
    if (diff === 0) {
        return null; 
    }
	
    const adjustType = diff > 0 ? 'INC' : 'DEC';
    const adjustQty  = Math.abs(diff);

    return {
        ivId: row.ivId,
        adjustType: adjustType,
        adjustQty: adjustQty,
        reason: 'STOCK_TAKE'
    };
}