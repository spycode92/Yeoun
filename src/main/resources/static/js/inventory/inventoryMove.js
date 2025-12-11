//전역변수

// 재고 이동 모달 초기화 함수
function resetMoveModal() {
    const moveZone = document.getElementById('moveZone');
    const moveRack = document.getElementById('moveRack');
    const moveRow = document.getElementById('moveRow');
    const moveColumn = document.getElementById('moveColumn');
	
    const moveQty = document.getElementById('moveQty');
	const ivQtyMove = document.getElementById('ivQtyMove');
	const exPectObQtyMove = document.getElementById('exPectObQtyMove');
    const canMoveAmount = document.getElementById('canMoveAmount');
	
	const zones = sortNumericStrings(getUniqueValuesMove(locationInfo, 'zone'));
	fillSelectMove(moveZone, zones);
    // 모든 입력값 초기화
	moveZone.value = ''
    moveRack.value = '';     // Rack 초기화
    moveRow.value = '';      // Row 초기화
    moveColumn.value = '';   // Column 초기화
    moveQty.value = '0';     // 수량 0으로 초기화
	ivQtyMove.value = '';
	exPectObQtyMove.value = '';
	canMoveAmount.value = '';
	
	// zones가 있으면 0번째로 기본 세팅 + 하위 셀렉트 자동 세팅
	if (zones.length > 0) {
	    moveZone.value = zones[0];
	    applyZoneSelection(zones[0]);
	}
}

// 재고이동 모달 열기
function openMoveModal(rowData) {
	resetMoveModal();
	
	document.getElementById('ivQtyMove').value = rowData.ivAmount;
	document.getElementById('exPectObQtyMove').value = rowData.expectObAmount;
	// 이동가능 수량 전역변수 지정
	document.getElementById('canMoveAmount').value = canUseQty; 
	
	// 이동물량 제한 (1~이동가능수량)
	const moveQty = document.getElementById('moveQty');
	moveQty.value = 1;
	moveQty.min   = 1;
	moveQty.max   = canUseQty; // 
	
	const modalEl = document.getElementById('moveModal');
	const bsModal = bootstrap.Modal.getOrCreateInstance(modalEl);
	bsModal.show();
}


// 창고 정보의 유니크값 뽑아내기
function getUniqueValuesMove(list, key) {
    return [...new Set(list.map(item => item[key]))]; 
}
// 셀렉트박스 채우기 함수
function fillSelectMove(selectEl, values) {
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
    const moveRack   = document.getElementById('moveRack');
    const moveRow    = document.getElementById('moveRow');
    const moveColumn = document.getElementById('moveColumn');

    if (!selectedZone) {
        fillSelectMove(moveRack, []);
        fillSelectMove(moveRow, []);
        fillSelectMove(moveColumn, []);
        return;
    }

    const byZone = locationInfo.filter(loc => loc.zone === selectedZone);
    const racks  = sortNumericStrings(getUniqueValuesMove(byZone, 'rack'));

    fillSelectMove(moveRack, racks);
    fillSelectMove(moveRow, []);
    fillSelectMove(moveColumn, []);

    if (racks.length > 0) {
        const firstRack = racks[0];
        document.getElementById('moveRack').value = firstRack;
        applyRackSelection(selectedZone, firstRack);
    }
}

// zone + rack 기준으로 row, col 세팅
function applyRackSelection(selectedZone, selectedRack) {
    const moveRow    = document.getElementById('moveRow');
    const moveColumn = document.getElementById('moveColumn');

    if (!selectedZone || !selectedRack) {
        fillSelectMove(moveRow, []);
        fillSelectMove(moveColumn, []);
        return;
    }

    const byZoneRack = locationInfo.filter(
        loc => loc.zone === selectedZone && loc.rack === selectedRack
    );
	
	// 숫자 문자열 오름차순 정렬 (01,02,03)
	const rows = sortNumericStrings(getUniqueValuesMove(byZoneRack, 'rackRow'));
	const cols = sortNumericStrings(getUniqueValuesMove(byZoneRack, 'rackCol'));
	
    fillSelectMove(moveRow, rows);
    fillSelectMove(moveColumn, cols);
}


// zone 선택시 rack 값 채우기
document.getElementById('moveZone').addEventListener('change', () => {
	const moveZone   = document.getElementById('moveZone');
	const selectedZone = moveZone.value;
	
	applyZoneSelection(selectedZone);	
});

// Rack 선택시 row, col값 채우기
document.getElementById('moveRack').addEventListener('change', () => {
	const moveZone   = document.getElementById('moveZone');
	const moveRack   = document.getElementById('moveRack');
	
	const selectedZone = moveZone.value;
	const selectedRack = moveRack.value;

	applyRackSelection(selectedZone, selectedRack);
});


// 현재 location값 기준으로 locationId를 찾아 반환하는함수
function getLocationIdByPosition() {
    const zone   = document.getElementById('moveZone').value;
    const rack   = document.getElementById('moveRack').value;
    const row    = document.getElementById('moveRow').value;
    const column = document.getElementById('moveColumn').value;
	
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

    // 현재 위치와 선택한 위치가 동일한 경우
    if (currentLoc && String(currentLoc) === String(targetLoc.locationId)) {
        alert('같은 장소로 이동할 수 없습니다.');
        return null;
    }

    return targetLoc.locationId;
}

// 이동물량 유효성 검사
const moveQtyInput = document.getElementById('moveQty');

moveQtyInput.addEventListener('input', () => {
	let val = Number(moveQtyInput.value);
	
	// 숫자가 아니거나 1보다 작을때 1로 변경
	if(isNaN(val) || val < 1) {
		moveQtyInput.value = 1;
		return;
	}
	// 이동수량보다 더큰 값일때 이동가능수량으로 지정
	if(val > canUseQty) {
		moveQtyInput.value = canUseQty;
		return;
	}
});

// 이동버튼 클릭시 이벤트
const btnMove = document.getElementById('moveBtnMove');

btnMove.addEventListener('click', async () => {
	// 로케이션id 가져오기
	const moveLocationId = getLocationIdByPosition();
	// 유효성검사 
	if(moveLocationId == null) return;
	
	//이동수량
	const moveQty = document.getElementById('moveQty').value;
	if(moveQty < 1) {
		alert('이동 수량은 1보다 작을 수 없습니다.')
	}
	if(moveQty > canUseQty){
		alert('이동 수량은 이동가능 수량보다 클수없습니다.')
	}
	
	// 재고이동 요청
	const response = 
		await fetch(`/api/inventories/${currentIvid}/move`, {
			method: 'POST',
			headers: {
				[csrfHeader]: csrfToken,
				'Content-Type': 'application/json'
			},
			body: JSON.stringify({
				moveLocationId,
				moveAmount: moveQty
			})
		});
		
	if (!response.ok) {
		throw new Error('재고이동에 실패하였습니다.')
	}
	alert("재고 이동 완료");
	window.location.reload();
	
});















