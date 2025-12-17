document.getElementById("completeReInboundBtn").addEventListener("click", async () => {
	const items = [];
	const rows = document.querySelectorAll("tbody tr");
	
	// 품목들의 데이터를 반복문을 통해서 items 배열 안에 담음
	for (const row of rows) {
		const index = row.querySelector(".moveZone").dataset.index;
		
		// 입고수량
		const inboundAmount = Number(row.querySelector(`.inboundAmount[data-index="${index}"]`).value);
		// 폐기 수량
		const disposeAmount = Number(row.querySelector(`.disposeAmount[data-index="${index}"]`).textContent);
		// 입고 요청 수량
		const requestAmount = Number(row.querySelector(`.requestAmount[data-index="${index}"]`).dataset.qty);
		// 발주 단위
		const unit = row.querySelector(`.unit[data-index="${index}"]`).innerText;
		
		// 단위 변환된 입고 및 폐기 수량
		const converedrequestAmount = parseInt(convertFromBaseUnit(requestAmount, unit));
		const converedInboundAmount = parseInt(convertToBaseUnit(inboundAmount, unit));
		const converedDisposeAmount = parseInt(convertToBaseUnit(disposeAmount, unit));
		
		const locationId = getLocationIdByPosition(index);
		
		// 창고위치 입력 확인
		if (!locationId) {
			alert("창고 위치를 지정하지 않은 품목이 있습니다.\n모든 품목의 위치를 지정해주세요.");
			return;
		}
		
		// 입고 수량 입력 확인
		if (inboundAmount < 0) {
			alert("입고 수량을 입력해주세요.");
			return;
		}
		
		if (converedrequestAmount !== (inboundAmount + disposeAmount)) {
			alert("입고 수량 또는 폐기 수량을 입력해주세요.");
			return;
		}
		
		const itemName = row.querySelector("td").getAttribute("data-name");
		const itemId = row.querySelector(".itemId").value;
		const itemType = row.querySelector(".itemType").value;
		const inboundItemId = row.querySelector(".inboundItemId").value;
		
		// 로트넘버 추가
		const lotNo = row.querySelector(".prodLotNo").value;
		
		items.push({
			inboundAmount: converedInboundAmount,
			disposeAmount: converedDisposeAmount,
			locationId,
			inboundItemId,
			itemId,
			itemType,
			itemName,
			lotNo
		});
	}

	const inboundId = document.querySelector("#inboundId").value;
	
	// 스피너 시작
	showSpinner();
	
	try {
		const res = await fetch("/inventory/inbound/re/complete", {
			method: "POST",
			headers: {
				"Content-Type": "application/json",
				[csrfHeader]: csrfToken
			},
			body: JSON.stringify({
				inboundId,
				type: "RE",
				items
			})
		});
		
		if (!res.ok) {
			alert("입고 등록을 실패했습니다.");
			return;
		}
		
		const data = await res.json();
		
		if (data.success) {
			alert("입고가 완료되었습니다.");
			setTimeout(() => {
				window.location.href = "/inventory/inbound/reInboundList";
			}, 10); 
		}
	} catch (error) {
		console.error(error);
		alert("요청 처리 중 오류가 발생했습니다." || error.message);
	} finally {
		//스피너  off
		hideSpinner(); 
	}
});