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
		
		if (!response.ok) {
			throw new Error('창고정보를 가져올 수 없습니다.')
		}
		return await response.json();
}

// 창고위치 저장할 변수
let locationInfo = [];

// 문자열 숫자 자연 정렬
function sortNumericStrings(arr) {
	return arr.slice().sort((a, b) => 
		a.localeCompare(b, "en", { numeric: true }));
}

// select 채우기
function fillSelect(selectEl, values) {
	selectEl.innerHTML = `<option value="">선택</option>`;
	
	values.forEach(v => {
		const opt = document.createElement('option');
		opt.value = v;
		opt.textContent = v;
		selectEl.appendChild(opt);
	});
}

// 같은 index에 있는 select 한번에 찾기
function getRowSelects(index) {
	return {
		zone: document.querySelector(`.moveZone[data-index="${index}"]`), 
		rack: document.querySelector(`.moveRack[data-index="${index}"]`),
		row: document.querySelector(`.moveRow[data-index="${index}"]`),
		col: document.querySelector(`.moveColumn[data-index="${index}"]`)
	}
}


// zone 선택 시 rack 목록 채우기
function applyZoneSelection(index) {
	const { zone, rack, row, col } = getRowSelects(index);
	const selectedZone = zone.value;
	
	// select 초기화
	fillSelect(rack, []);
	fillSelect(row, []);
	fillSelect(col, []);
	
	if (!selectedZone) return;
	
	const list = locationInfo.filter(location => location.zone === selectedZone);
	const racks = sortNumericStrings([...new Set(list.map(location => location.rack))]);
	
	fillSelect(rack, racks);
}

// rack 선택 시 row와 col 목록 채우기
function applyRackSelection(index) {
	const { zone, rack, row, col } = getRowSelects(index);
	const selectedZone = zone.value;
	const selectedRack = rack.value;
	
	// 리셋
	fillSelect(row, []);
	fillSelect(col, []);
	
	 if (!selectedZone || !selectedRack) return;
	 
	 // 선택한 zone과 rack에 해당하는 위치 저장
	 const list = locationInfo.filter(
		locatoin => locatoin.zone === selectedZone && locatoin.rack === selectedRack
	 );
	 
	 // 중복 제거한 목록 추출
	 const rows = sortNumericStrings([...new Set(list.map(location => location.rackRow))]);
	 const cols = sortNumericStrings([...new Set(list.map(l => l.rackCol))]);
	 
	 fillSelect(row, rows);
	 fillSelect(col, cols);
}

// 페이지 로딩될 때 select 및 수량 정보 채우기
document.addEventListener("DOMContentLoaded", async () => {
	//스피너  off
	hideSpinner();
	
	// 창고 데이터 가져오기
	locationInfo = await getLocationInfo();
	
	document.querySelectorAll(".locationId").forEach(td => {
		const rawId = td.textContent.trim();
		const loc = locationInfo.find(l => String(l.locationId) === rawId);
		
		if (loc) {
			td.textContent = `${loc.zone}-${loc.rack}-${loc.rackRow}-${loc.rackCol}`;
		}
	})
	
	// 모든 row의 zone select 채우기
	const zones = [... new Set(locationInfo.map(location => location.zone))];
	const sortedZones = sortNumericStrings(zones);
	
	document.querySelectorAll(".moveZone").forEach(select => {
		fillSelect(select, sortedZones);
	});
	
	// zone 변경 시 rack 변경
	document.querySelectorAll(".moveZone").forEach(select => {
		const index = select.dataset.index;
		select.addEventListener("change", () => {
			applyZoneSelection(index);
		});
	});
	
	// rack 변경 시 row, col 업데이트
	document.querySelectorAll(".moveRack").forEach(select => {
		const index = select.dataset.index;
		
		select.addEventListener("change", () => {
			applyRackSelection(index);
		});
	});
	
	// ========================
	// 단위에 따른 수량 변환
	// 변환 대상에 해당하는 요소를 찾기	
	const tartgetElements = document.querySelectorAll(".convert-qty");
	
	tartgetElements.forEach(td => {
		// 요청 수량 
		const baseQty = parseFloat(td.dataset.qty);
		// 단위
		const targetUnit = td.dataset.unit;
		// DB에 저장된 수량을 단위에 맞게 변환(util.js에서 함수 가져옴)
		const convertedValue = convertFromBaseUnit(baseQty, targetUnit);
		
		// 변환한 값을 화면에 보여주기
		td.innerText = convertedValue.toLocaleString();
	});
	
	// -----------------------------------
	const inboundInputs = document.querySelectorAll(".inboundAmount");
	
	if (inboundInputs.length > 0) {
		inboundInputs.forEach(input => {
			
			input.addEventListener("change", function() {
				const currentIndex = this.getAttribute('data-index');
				const currentVal = Number(this.value);
				
				// 같은 index를 가진 요청 수량 찾기
				const requestSpan = document.querySelector(`.requestAmount[data-index="${currentIndex}"]`);
				
				let maxAmount = 0;
				// span 태그 안에 콤마가 있을 수 있기 때문에 제거 후 숫자로 변환
				if (requestSpan) {
					const textVal = requestSpan.innerText.replace(/,/g, '');
					maxAmount = Number(textVal);
				}
				
				// 음수 체크
				if (currentVal < 0) {
					alert("입고 수량은 0보다 작을 수 없습니다.")
					this.value = 0;
					return;
				}
				
				// 요청 수량보다 초과하는지 체크
				if (maxAmount > 0 && currentVal > maxAmount) {
					alert(`입고 수량은 요청 수량(${maxAmount.toLocaleString()})을 초과할 수 없습니다.`);
					this.value = maxAmount; 
				}
			});
		})
	}
});

// ------------------------------------------------------
document.querySelectorAll(".inboundAmount").forEach(input => {
	input.addEventListener("input", () => {
		const inboundStatus  = document.getElementById("inboundStatus").value;
		
		// 완료 상태이면 입력 무시
		if (inboundStatus === "COMPLETED") return;
		
		const index = input.dataset.index;
		
		const requestSpan = document.querySelector(`.requestAmount[data-index="${index}"]`); 
		const disposeSpan = document.querySelector(`.disposeAmount[data-index="${index}"]`);
		
		// 발주 단위
		const unit = document.querySelector(`.unit[data-index="${index}"]`).innerText;
		
		const requestAmount = Number(convertFromBaseUnit(requestSpan.dataset.qty, unit) || 0);
		const inboundAmount = Number(input.value || 0);
		
		
		// 요청수량보다 커지지 못하게 제한
		if (inboundAmount > requestAmount) {
			alert("입고 수량은 발주 수량이상으로 입력할 수 없습니다.");
			inboundAmount = requestAmount;
			input.value = requestAmount;
		}
		
		// 음수 기호 입력 막음
		input.addEventListener("keydown", (e) => {
			if (e.key === "-" || e.key === "e") {
				e.preventDefault();
			}
		});
		
		if (inboundAmount < 0) {
			input.value = 0;
			return;
		}
		
		// 불량수량 계산
		const dispose = requestAmount - inboundAmount;
		
		disposeSpan.textContent = dispose;
	});
});

document.getElementById("completeInboundBtn").addEventListener("click", async () => {
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
		const res = await fetch("/inventory/inbound/mat/complete", {
			method: "POST",
			headers: {
				"Content-Type": "application/json",
				[csrfHeader]: csrfToken
			},
			body: JSON.stringify({
				inboundId,
				type: "MAT",
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
				window.location.href = "/inventory/inbound";
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

// 현재 location값 기준으로 locationId를 찾아 반환하는함수
function getLocationIdByPosition(index) {
	const { zone, rack, row, col } = getRowSelects(index);
	
	const selectedZone = zone.value;
	const selectedRack = rack.value;
	const selectedRow = row.value;
	const selectedCol = col.value;
	
	if (!selectedZone || !selectedRack || !selectedRow || !selectedCol) {
		return null;
	}
	
	const target = locationInfo.find(loc => 
		loc.zone === selectedZone &&
		loc.rack === selectedRack &&
		loc.rackRow  === selectedRow &&
		loc.rackCol === selectedCol
	);
	
	if (!target) {
		console.error("해당 위치의 LOCATION_ID를 찾을 수 없음");
		return null;
	}
	
	return target.locationId;
}

// ---------------------------------------
// 발주 상세 모달 열기
function openDetailWindow(id) {
	const url = `/purchase/detail/${id}`;
	
	window.open(url, '_blank', 'width=1200,height=900,scrollbars=yes');
}

// 작업직시서 상세 모달 열기
function openDetailOrderWindow(id) {
	const url = `/inventory/inbound/detail/prodWin/${id}`;
	
	window.open(url, '_blank', 'width=1320,height=610,scrollbars=yes');
}