//전역변수

// 수량 조절 모달 초기화
function resetAdjustQtyModal() {
    // 기본 조절 유형은 증가로
    document.getElementById('adjustInc').checked = true;
    document.getElementById('adjustDec').checked = false;

    // 수량, 사유 초기화
    document.getElementById('adjustQty').value = 1;
    document.getElementById('adjustReason').value = '';
}

// 수량 조절 모달 열기
function openAdjustQtyModal(rowData) {
	resetAdjustQtyModal();
	
	const modalEl = document.getElementById('adjustModal');
	const modal = bootstrap.Modal.getOrCreateInstance(modalEl);
	modal.show();
}

// 변경후 재고수량이 출고예정 수량보다 낮아질 수 없음을 체크하는함수
function validateAdjustQty() {
	const qtyVal = Number(document.getElementById('adjustQty').value);
	const adjustType = document.querySelector('input[name="adjustType"]:checked').value;
	
	// 변경할 수량에 숫자가아니거나 1이하의 수가 들어가면 실행
	if(Number.isNaN(qtyVal) || qtyVal < 1) {
		return {
			valid: false,
			message: '변경 수량은 1 이상이어야 합니다.'
		}
	}
	
	// 증가일 때는 재고/출고예정 조건 체크안함
	if(adjustType !== 'DEC') {
		return {valid: true};
	}
	
	// 변경후 재고수량, 출고예정수량
	const newQty = Number(currentIvQty) - qtyVal;
	const expected = Number(expectOutboundQty) || 0;
	
	if(newQty < expected) {
		return {
			valid: false,
			message: `변경 후 재고(${newQty})가 출고예정 수량(${expected})보다 작을 수 없습니다.`
		}
	}
	
	return { valid: true };
}

// 수량조절 음수 입력 불가설정
const qtyInput = document.getElementById('adjustQty');

qtyInput.addEventListener('keydown', (e) => {
	if (e.key === '-' || e.key === '+') {
		e.preventDefault();
	}
});

qtyInput.addEventListener('input', () => {
	const val = Number(qtyInput.value);
	
	if (Number.isNaN(val) || val < 1) {
	    qtyInput.value = 1;
	}
	
	const result = validateAdjustQty();
	if (!result.valid) {
	    alert(result.message);
		qtyInput.value = 1;
	    return;
	}
	
});

// 라디오버튼변경시 변경값 체크
const adjustTypeRadios = document.querySelectorAll('input[name="adjustType"]');

adjustTypeRadios.forEach(radio => {
	radio.addEventListener('change', () => {
		const result = validateAdjustQty();
		if (!result.valid) {
		    alert(result.message);
			qtyInput.value = 1;
		    return;
		}
	});
});

// 저장버튼 클릭시 들어갈 데이터
function getAdjustData() {
	return {
//		ivId: currentIvid,
		adjustType: document.querySelector('input[name="adjustType"]:checked').value,
		adjustQty: Number(document.getElementById('adjustQty').value || 0),
		reason: document.getElementById('adjustReason').value.trim() || ''
	}
}

// 저장버튼 클릭이벤트
const saveBtn = document.getElementById('adjustSave');
saveBtn.addEventListener('click', async () => {
	
	if (Number(document.getElementById('adjustQty').value) < 1) {
		alert("변경 수량은 1보다 커야합니다.")
	}
	
	const adjustData = getAdjustData();
	
	const response = 
	await fetch(`/api/inventories/${currentIvid}/adjustQty`, {
		method: 'POST',
		headers: {
			[csrfHeader]: csrfToken,
			'Content-Type': 'application/json'
		},
		body: JSON.stringify(adjustData)
	});
	
	if (!response.ok) {
		alert("수량조절에 실패하였습니다.")
	    throw new Error('수량 조절에 실패했습니다.');
	}
	
	alert("수량 조절 완료");
	window.location.reload();
});