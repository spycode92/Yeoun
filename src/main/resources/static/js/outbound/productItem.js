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

// 페이지 로딩될 때 lcationId를 사용자가 보기 쉬운 형태로 변환
document.addEventListener("DOMContentLoaded", async () => {
	locationInfo = await getLocationInfo();
	
	document.querySelectorAll(".locationId").forEach(td => {
		const rawId = td.textContent.trim();
		const loc = locationInfo.find(l => String(l.locationId) === rawId);
		
		if (loc) {
			td.textContent = `${loc.zone}-${loc.rack}-${loc.rackRow}-${loc.rackCol}`;
		}
	});
	
});

// 출고 등록 버튼 이벤트
document.getElementById("completePrdOutboundBtn").addEventListener("click", async () => {
	const items = [];
	
	const rows = document.querySelectorAll("tbody tr");
	
	// 품목들의 데이터를 반복문을 통해서 items 배열 안에 담음
	rows.forEach((row, index) => {
		const prdId = row.querySelector(".prdId")?.textContent.trim();
		const lotNo = row.querySelector(".lotNo")?.textContent.trim();
		const outboundQty = Number(row.querySelector(`.outboundQty[data-index="${index}"]`)?.textContent.trim());
		const ivId = row.querySelector(".ivId")?.value;
		
		
		items.push({
			prdId,
			lotNo,
			outboundQty,
			ivId
		})
	})
	
	const outboundId = document.querySelector("#outboundId").value;
	const shipmentId = document.querySelector("#shipmentId").value;
	
	const res = await fetch("/inventory/outbound/prd/complete", {
		method: "POST",
		headers: {
			"Content-Type": "application/json",
			[csrfHeader]: csrfToken
		},
		body: JSON.stringify({ 
			outboundId,
			shipmentId,
			type: "FG",
			items
		})
	});
	
	if (!res.ok) {
		alert("출고 등록을 실패했습니다.");
		return;
	}
	
	const data = await res.json();
	
	if (data.success) {
		alert("출고가 완료되었습니다.");
		setTimeout(() => {
			window.location.href = "/inventory/outbound/list";
		}, 10); 
	}
	
});