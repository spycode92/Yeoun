//전역변수
let detailRowData; // 재고상세 데이터
let currentIvid; // 재고id
let currentIvQty; // 현재 재고수량
let expectOutboundQty; // 출고예정수량
let currentLoc; // 현재위치
let canUseQty; // 이동가능수량

// 1) 모달 필드 초기화
function resetDetailModal() {
	//전역변수값 초기화
	detailRowData = null;
	currentIvid = null;
	currentIvQty = null;
	expectOutboundQty = null;
	currentLoc = null;
	canUseQty = null;
	
	document.getElementById('detailIvId').value = '';
	document.getElementById('detailLotNo').value = '';
	document.getElementById('detailCategory').value = '';
	document.getElementById('detailProdName').value = '';
	document.getElementById('detailAmount').value = '';
	document.getElementById('detailExpectObAmount').value = '';
	document.getElementById('detailLocation').value = '';
	document.getElementById('detailExpiry').value = '';
	document.getElementById('sameLotTableBody').innerHTML = '';
}


// 2) rowData를 받아서 모달에 바인딩 + 모달 오픈
function openDetailModal(rowData, sameLotList = []) {
	resetDetailModal();
	// 전역변수 값 지정
	detailRowData = rowData;
	
	currentIvid = rowData.ivId;
	currentIvQty = rowData.ivAmount;
	expectOutboundQty = rowData.expectObAmount;
	currentLoc = rowData.locationId; 
	canUseQty = rowData.ivAmount - rowData.expectObAmount;
	
	// 기본 정보 세팅
	document.getElementById('detailIvId').value = rowData.ivId || '';
	document.getElementById('detailLotNo').value = rowData.lotNo || '';
	// 카테고리: itemType → 한글 매핑
	document.getElementById('detailCategory').value = (function(type) {
		switch (type) {
			case 'RAW':  return '원자재';
			case 'SUB':  return '부자재';
			case 'FG':   return '완제품';
			default:     return type || '';
		}
	})(rowData.itemType);

	document.getElementById('detailProdName').value = rowData.prodName || '';
	document.getElementById('detailAmount').value = rowData.ivAmount ?? '';
	document.getElementById('detailExpectObAmount').value = rowData.expectObAmount ?? '';

	// 위치
	const loc = [rowData.zone, rowData.rack, rowData.rackRow, rowData.rackCol]
		.filter(v => v)
		.join('-');
	document.getElementById('detailLocation').value = loc;
	
	const expiryInput = document.getElementById('detailExpiry');
	// 유통기한 (시:분까지만)
	if (rowData.expirationDate) {
		expiryInput.value = rowData.expirationDate.substring(0, 16);
		// 보이게 (다시 검색했을 때를 대비해)
		const expiryCol = expiryInput.closest('.col');
		if (expiryCol) expiryCol.style.display = '';
	} else {
		const expiryCol = expiryInput.closest('.col');
		if (expiryCol) expiryCol.style.display = 'none';
	}

	// 동일 LOT 다른 위치 테이블 채우기 (필요시)
	const tbody = document.getElementById('sameLotTableBody');
	const section = document.getElementById('detailSameLotSection');
	// 초기화
	tbody.innerHTML = '';
	// 동일 lot넘버의 상품이 다른위치에 존재하지 않을경우
	if(!sameLotList || sameLotList.length == 0) {
		section.style.display = 'none'; // 숨김
	} else {
		section.style.display = 'block'; // 보임
	}
	
	sameLotList.forEach(item => {
		const tr = document.createElement('tr');
		tr.innerHTML = `
			<td>${item.zone ?? ''}</td>
			<td>${item.rack ?? ''}</td>
			<td>${item.rackRow ?? ''}</td>
			<td>${item.rackCol ?? ''}</td>
			<td>${item.ivAmount ?? ''}</td>
			<td>${item.expectObAmount ?? ''}</td>
		`;
		tbody.appendChild(tr);
	});

	// 부트스트랩 모달 오픈
	const modalEl = document.getElementById('detailModal');
	const bsModal = bootstrap.Modal.getOrCreateInstance(modalEl);
	bsModal.show();
}

//수량조절 버튼 클릭 이벤트
const detailBtnAdjustQty = document.getElementById('adjustBtn');
detailBtnAdjustQty.addEventListener('click', () => {
	openAdjustQtyModal(detailRowData);
});

//재고이동 버튼 클릭 이벤트
const detailBtnMove = document.getElementById('detailBtnMove');
detailBtnMove.addEventListener('click', () => {
	openMoveModal(detailRowData);	
});

//폐기 버튼 클릭 이벤트
const detailBtnDisposal = document.getElementById('detailBtnDisposal');
detailBtnDisposal.addEventListener('click', () => {
	openDisposalModal(detailRowData);
});



























