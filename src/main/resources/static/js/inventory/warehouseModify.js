let locationCache = [];

const zoneAddToggle = document.getElementById("zoneAddToggle");
const zoneSelectBox = document.getElementById("zoneSelectBox");
const zoneAddBox = document.getElementById("zoneAddBox");
const zoneInput = document.getElementById("zoneInput");
const zoneCancel = document.getElementById("zoneCancel");
const zoneSelect = document.getElementById("zoneSelect");

const rackAddToggle = document.getElementById("rackAddToggle");
const rackSelectBox = document.getElementById("rackSelectBox");
const rackAddBox = document.getElementById("rackAddBox");
const rackInput = document.getElementById("rackInput");
const rackCancel = document.getElementById("rackCancel");
const rackSelect = document.getElementById("rackSelect");

document.addEventListener("DOMContentLoaded", () => {
	const modalEl = document.getElementById("registWarehouseModal");
	
    if (modalEl) {
        bsWarehouseModal = new bootstrap.Modal(modalEl);
    }

	// 창고등록 div 변수에 담음
    const openBtn = document.getElementById("openWarehouseModal");
	
    if (openBtn) {
        openBtn.addEventListener("click", async () => { // 창고등록 클릭 이벤트
            resetWarehouseModal();
			await loadLocationCache();
			renderZoneOptions(); 
            bsWarehouseModal.show();
        });
    }
});

// 모달 초기화
function resetWarehouseModal() {
    zoneSelectBox.classList.remove("d-none");
    zoneAddBox.classList.add("d-none");
    rackSelectBox.classList.remove("d-none");
    rackAddBox.classList.add("d-none");

    rowStart.value = 1;
    rowEnd.value = 1;
    colStart.value = 1;
    colEnd.value = 1;

    previewBox.innerHTML = "";
}

// 창고 데이터 가져오기
async function loadLocationCache() {
	locationCache = await getLocationData();
}

// zone 목록 추출
function getZonesFromLocationData() {
	const zoneSet = new Set();
	
	locationCache.forEach(d => zoneSet.add(d.zone));
	
	return Array.from(zoneSet).sort();
}

// zone 옵션 넣기 
function renderZoneOptions() {
	zoneSelect.innerHTML = `<option disabled selected>Zone 선택</option>`;
	
	getZonesFromLocationData().forEach(zone => {
		const option = document.createElement("option");
        option.value = zone;
        option.textContent = zone;
        zoneSelect.appendChild(option);
	});
}

// zone 변경 이벤트
zoneSelect.addEventListener("change", (e) => {
	const zone = e.target.value;
	
	rackSelect.innerHTML = `<option disabled selected>Rack 선택</option>`;
	
	getRackByZone(zone).forEach(rack => {
		const option = document.createElement("option");
		option.value = rack;
       option.textContent = rack;
       rackSelect.appendChild(option);
	});
	
	rackSelect.disabled = false;
	rackAddToggle.disabled = false;
});

// zone 값을 받아서 rack 목록 필터링 
function getRackByZone(zone) { 
	const rackSet = new Set(); 
	
	locationCache 
		.filter(d => d.zone === zone) 
		.forEach(d => rackSet.add(d.rack)); 
		
	return Array.from(rackSet).sort(); 
}

// zone 신규 버튼 클릭 이벤트
zoneAddToggle.onclick = () => {
	zoneSelectBox.classList.add("d-none");
	zoneAddBox.classList.remove("d-none");
	zoneInput.focus();
}

// zone 취소 버튼 클릭 이벤트
zoneCancel.onclick = () => {
	zoneAddBox.classList.add("d-none");
	zoneSelectBox.classList.remove("d-none");
	zoneInput.value = "";
}

// 신규 zone 값 가져오기
function getSelectedZone() {
	if (!zoneAddBox.classList.contains("d-none")) {
		return zoneInput.value.trim();
	}
	
	return zoneSelect.value;
}

// rack 신규 버튼 클릭 이벤트
rackAddToggle.onclick = () => {
	rackSelectBox.classList.add("d-none");
	rackAddBox.classList.remove("d-none");
	rackInput.focus();
}

// rack 취소 버튼 클릭 이벤트
rackCancel.onclick = () => {
	rackAddBox.classList.add("d-none");
	rackSelectBox.classList.remove("d-none");
	rackInput.value = "";
}

// 신규 rack 값 가져오기
function getSelectedRack() {
	if (!rackAddBox.classList.contains("d-none")) {
		return rackInput.value.trim();
	}
	
	return rackSelect.value;
}

// 기존 location 중복 검사
function getExistingLocationSet(zone, rack) {
	return new Set(
		locationCache
			.filter(d => d.zone === zone && d.rack === rack)
			.map(d => `${d.rackRow}-${d.rackCol}`)
	);
}

function buildNewLocations(zone, rack, rs, re, cs, ce) {
	 const existingSet = getExistingLocationSet(zone, rack);
	 
	 const newList = [];
	 const duplicated = [];
	 
	 for (let r = rs; r <= re; r++) {
		const rowChar = String.fromCharCode(64 + r); // 1 -> A로 변환
		
		for (let c = cs; c <= ce; c++) {
			const key = `${rowChar}-${normalizeCol(c)}`;
			
			if (existingSet.has(key)) {
				duplicated.push(key);
			} else {
				newList.push({
					zone, 
					rack,
					rackRow: rowChar,
					rackCol: c
				});
			}
		}
	 } 
	 
	 return { newList, duplicated };
}


// 등록 버튼 클릭 이벤트
document.getElementById("registerBtn").addEventListener("click", async () => {
	const zone = getSelectedZone();
	const rack = normalizeRack(resolveRackValue());
	
	if (!zone || !rack) {
		alert("Zone과 Rack을 입력 또는 선택하세요.");
		return;
	}
	
	const rs = Number(document.getElementById("rowStart").value);
	const re = Number(document.getElementById("rowEnd").value);
	const cs = Number(document.getElementById("colStart").value);
	const ce = Number(document.getElementById("colEnd").value);
	
	const { newList, duplicated } = buildNewLocations(zone, rack, rs, re, cs, ce);
	
	if (newList.length === 0) {
		alert("모든 위치가 이미 존재합니다.");
		return;
	}
	
	if (duplicated.length > 0) {
		if (!confirm(`이미 존재하는 위치 ${duplicated.length}개를 제외하고 ${newList.length}개를 등록할까요?`)) {
			return;
		}
	}
	
	const payload = {
		zone,
		rack,
		rowStart: rs,
		rowEnd: re,
		colStart: cs,
		colEnd: ce
	}
	
	try { 
		const res = await fetch("/api/inventories/locations/add", {
			method: "POST",
			headers : {
				[csrfHeader]: csrfToken,
				"Content-Type": "application/json"
			},
			body: JSON.stringify(payload)
		});
		
		if (!res.ok) {
			throw new Error("서버 오류");
		}

		alert("창고 등록 완료");

		await loadLocationCache();
		initDashboard();
		bsWarehouseModal.hide();
	} catch (error) {
		console.error(error);
		alert("등록 중 오류가 발생했습니다.");
	}
});

function resolveRackValue() {
	// 신규 zone인 경우 01부터 시작
	if (!zoneAddBox.classList.contains("d-none")) {
		return "01";
	}

	// 기존 zone인 경우 기존 로직
	const rack = getSelectedRack();
	return normalizeRack(rack);
}

function normalizeCol(col) {
    return col.toString().padStart(2, "0");
}

function normalizeRack(rack) {
    return rack.toString().padStart(2, "0");
}







