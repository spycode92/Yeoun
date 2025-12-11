//전역변수

// 폐기 모달 초기화
function resetDisposalModal() {
	document.getElementById('ivQtyDisposal').value = '';
	document.getElementById('exPectObQtyDisposal').value = '';
	document.getElementById('canDisposalAmount').value = '';
    // 수량, 사유 초기화
    document.getElementById('disposalQty').value = 1;
    document.getElementById('disposalReason').value = '';

}

// 폐기 모달 열기
function openDisposalModal(rowData) {
	resetDisposalModal();
	
	document.getElementById('ivQtyDisposal').value = rowData.ivAmount;;
	document.getElementById('exPectObQtyDisposal').value = rowData.expectObAmount;;
	document.getElementById('canDisposalAmount').value = canUseQty;
	
	const modalEl = document.getElementById('disposalModal');
	const modal = bootstrap.Modal.getOrCreateInstance(modalEl);
	modal.show();
}

// 폐기물량 유효성 검사
const disposalQtyInput = document.getElementById('disposalQty');

disposalQtyInput.addEventListener('input', () => {
	let val = Number(disposalQtyInput.value);
	
	// 숫자가 아니거나 1보다 작을때 1로 변경
	if(isNaN(val) || val < 1) {
		disposalQtyInput.value = 1;
		return;
	}
	// 폐기가능 수량보다 더큰 값일때는 폐기가능수량으로 설정
	if(val > canUseQty) {
		disposalQtyInput.value = canUseQty;
		return;
	}
});

//폐기처리 버튼 클릭 이벤트
const disposalBtn = document.getElementById('disposalBtn');
disposalBtn.addEventListener('click', async () => {
	//폐기수량
	const disposalQty = disposalQtyInput.value;
	if(disposalQty < 1) {
		alert('폐기 수량은 1보다 작을 수 없습니다.')
	}
	if(disposalQty > canUseQty){
		alert('폐기 수량은 이동가능 수량보다 클수없습니다.')
	}
	
	const disposeQty = disposalQtyInput.value
	const disposeReason = document.getElementById('disposalReason').value;
	
	// 재고이동 요청
	const response = 
		await fetch(`/api/inventories/${currentIvid}/dispose`, {
			method: 'POST',
			headers: {
				[csrfHeader]: csrfToken,
				'Content-Type': 'application/json'
			},
			body: JSON.stringify({
				reason: disposeReason,
				disposeAmount: disposeQty
			})
		});
	//	console.log(response);
	if (!response.ok) {
		throw new Error('폐기에 실패하였습니다.')
	}
	alert("폐기 완료");
	window.location.reload();
});
