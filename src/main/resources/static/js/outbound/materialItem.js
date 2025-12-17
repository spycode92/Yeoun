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
	//스피너  off
	hideSpinner(); 
	
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
document.getElementById("completeOutboundBtn").addEventListener("click", async () => {
	const items = [];
	const rows = document.querySelectorAll("tbody tr");
	
	// 품목들의 데이터를 반복문을 통해서 items 배열 안에 담음
	rows.forEach((row, index) => {
		const matId = row.querySelector(".matId")?.textContent.trim();
		const lotNo = row.querySelector(".lotNo")?.textContent.trim();
		const outboundQty = Number(row.querySelector(`.outboundQty[data-index="${index}"]`)?.textContent.trim());
		const ivId = row.querySelector(".ivId")?.value;
		
		items.push({
			matId,
			lotNo,
			outboundQty,
			ivId
		})
	})
	
	
	const outboundId = document.querySelector("#outboundId").value;
	const workOrderId = document.querySelector("#workOrderId").value;
	
	// 스피너 시작
	showSpinner();
	
	try {
		const res = await fetch("/inventory/outbound/mat/complete", {
			method: "POST",
			headers: {
				"Content-Type": "application/json",
				[csrfHeader]: csrfToken
			},
			body: JSON.stringify({ 
				outboundId,
				workOrderId,
				type: "MAT",
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
				window.location.href = "/inventory/outbound";
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

// 작업직시서 상세 모달 열기
function openDetailOrderWindow(id) {
	console.log(id);
	const url = `/inventory/outbound/detail/prodWin/${id}`;
	
	window.open(url, '_blank', 'width=1320,height=610,scrollbars=yes');
}