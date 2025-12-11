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

// 페이지 로딩될 때 select 채우기
document.addEventListener("DOMContentLoaded", async () => {
	// 창고 데이터 가져오기
	locationInfo = await getLocationInfo();
	
	const inboundStatus = document.getElementById("inboundStatus").value;
	
	// 입고 완료의 경우 수량입력, 창고선택 및 입고완료 버튼 숨김 처리
	if (inboundStatus === "COMPLETED") {
		disableAllInputs();
		hideCompleteButton();
	}
	
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
});

// ------------------------------------------------------
// 입고완료 처리 
// 상태가 입고완료면 input과 select 비활성화
function disableAllInputs() {
	// 입고수량 input 비활성화
	document.querySelectorAll(".inboundAmount").forEach(input => {
		input.disabled = true;
	});
	
	// select 박스 비활성화
	document.querySelectorAll(".moveZone, .moveRack, .moveRow, .moveColumn").forEach(select => {
		select.disabled = true;
	});
}

function hideCompleteButton() {
	const btn = document.getElementById("completeInboundBtn");
	
	if (btn) {
		btn.style.display = "none";
	}
}

document.querySelectorAll(".inboundAmount").forEach(input => {
	input.addEventListener("input", () => {
		const inboundStatus  = document.getElementById("inboundStatus").value;
		
		// 완료 상태이면 입력 무시
		if (inboundStatus === "COMPLETED") return;
		
		const index = input.dataset.index;
		
		const requestSpan = document.querySelector(`.requestAmount[data-index="${index}"]`); 
		const disposeSpan = document.querySelector(`.disposeAmount[data-index="${index}"]`);
		
		
		const requestAmount = Number(requestSpan.textContent || 0);
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
	rows.forEach(row => {
		const index = row.querySelector(".moveZone").dataset.index;
		
		const inboundAmount = Number(row.querySelector(`.inboundAmount[data-index="${index}"]`).value);
		const disposeAmount = Number(row.querySelector(`.disposeAmount[data-index="${index}"]`).textContent);
		
		const locationId = getLocationIdByPosition(index);
		
		const itemName = row.querySelector("td").getAttribute("data-name");
		const itemId = row.querySelector(".itemId").value;
		const itemType = row.querySelector(".itemType").value;
		const inboundItemId = row.querySelector(".inboundItemId").value;
		// 로트넘버 추가
		const lotNo = row.querySelector(".prodLotNo").value;
		
		items.push({
			inboundAmount,
			disposeAmount,
			locationId,
			inboundItemId,
			itemId,
			itemType,
			itemName,
			lotNo
		});
	});
	
	const inboundId = document.querySelector("#inboundId").value;
	
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
	console.log(id);
	const url = `/purchase/detail/${id}`;
	
	window.open(url, '_blank', 'width=1200,height=900,scrollbars=yes');
}

function openDetailOrderWindow(id) {
	console.log(id);
	const url = `/inventory/inbound/detail/prodWin/${id}`;
	
	window.open(url, '_blank', 'width=1320,height=610,scrollbars=yes');
}