// =============================================================
// 출고 등록 로직
const shipmentSelect = document.querySelector("#shipmentSelect");
const processByName = document.querySelector("#processByName");
const processByEmpId = document.querySelector(".processByEmpId");
const shopClientName = document.querySelector("#shopClientName");
const expectDate = document.querySelector("#expectDate");
const shipTbody = document.querySelector("#shipTbody");
const shipmentId = document.querySelector("#shipmentId");

// 출하지시서 데이터 저장
let shipmentList = [];
let prdOutboundDate;

// 출하지시서 정보 가져오기
async function loadShipmentList() {
	try {
		
		// 출하지시서 데이터 가져오는 API 작성하기
		const res = await fetch("/api/shipment/list");
		
		if (!res.ok) {
			throw new Error("데이터 로드 실패!");
		}
		
		const data = await res.json();
		
		return data;
		
	} catch (error) {
		console.error(error);
		return [];
	}
	
}

// 출하지시서 선택 이벤트
shipmentSelect.addEventListener("focus", async () => {
	shipmentList = await loadShipmentList();
	
	if (shipmentList.length === 0) {
		return;
	}
	
	shipmentSelect.innerHTML = `<option value="">출하지시서를 선택하세요.</option>`; 
	
	// 출하지시서 목록 만들기
	shipmentList.forEach(el => {
		const opt = document.createElement("option");
		opt.value = el.shipmentId;
		opt.textContent = el.shipmentId;
		
		shipmentId.value = el.shipmentId;
		
		shipmentSelect.appendChild(opt);
	});
	
});

shipmentSelect.addEventListener("change", async () => {
	console.log("change 실행")
	const shipId = shipmentSelect.value;
	
	if (!shipId) return;
	
	const changedShipOrder = shipmentList.find(el => {
		return el.shipmentId === shipId
	})

	// 체인지이벤트 발생시 선택된 shipId 의 createdId 입력
	processByEmpId.value = changedShipOrder.createdId;
	
	// 선택한 출하지시서 리스트에서 찾기
	const shipOrder = shipmentList.find(el => el.shipmentId === shipId);
	
	if (!shipOrder) {
		alert("출하지시서 데이터를 찾을 수 없습니다.");
		return;
	}
	
	// 선택한 출하지시서에 따른 담당자, 거래처명, 출고일 정보 입력
	shipmentId.value = shipId;
	processByName.value = shipOrder.createdName;
	shopClientName.value = shipOrder.clientName;
	expectDate.value = shipOrder.startDate?.split("T")[0] || "0";
	prdOutboundDate = shipOrder.startDate;
	
	// 선택한 출하지시서의 품목 리스트 렌더링
	renderProductList(shipOrder.items);
	
});

// 출하지시서에 해당하는 품목 렌더링
function renderProductList(items) {
	// 출고 품목 초기화
	shipTbody.innerHTML = "";
	
	// 반복문 사용해서 필요한 상품 tbody에 넣기
	for (let prd of items) {
		// 필요수량
		const row = `
			<tr>
				<td>${prd.prdId}</td>
				<td>${prd.prdName}</td>
				<td data-qty="${prd.shipmentQty}" name="shipmentQty">${prd.shipmentQty}</td>
				<td data-stock="${prd.orderqQty}" name="stock">${prd.orderqQty}</td>
				<td>
					<input type="number" class="form-control outboundQty" min="0" value="${prd.shipmentQty}">
					<input type="hidden" name="prdId" value="${prd.prdId}"/>
				</td>
			</tr>
		`;
		
		shipTbody.insertAdjacentHTML("beforeend", row);
	}
}

// 출고 등록 버튼 로직
const submitPrdOutbound = async () => {
	// 출고 품목을 담을 변수
	const items = [];
	const rows = document.querySelectorAll("#shipTbody tr");
	
	for (const row of rows) {
		// 납품 수량
		const shipmentQty = Number(row.querySelector("td[name=shipmentQty]").dataset.qty);
		// 출고 수량
		const outboundQty = Number(row.querySelector(".outboundQty").value);
		// 재고 수량
		const stock = Number(row.querySelector("td[name=stock]").dataset.stock);
		
		// 출고 수량과 재고 수량 비교
		if (outboundQty > stock) {
			alert("출고 수량이 재고 수량보다 많습니다.");
			return;
		}

		// 출고 수량과 요청 수량 비교
		if (shipmentQty > outboundQty) {
			alert("출고 수량이 요청 수량보다 적습니다.");
			return;
		}
		
		// 출고 수량과 요청 수량 비교
		if (shipmentQty < outboundQty) {
			alert("출고 수량이 요청 수량보다 많습니다.");
			return;
		}
		
		items.push({
			prdId: row.querySelector("input[name=prdId]").value,
			outboundQty
		});
		
	}

	
	// body에 담아서 보낼 내용
	const payload = {
		shipmentId : shipmentId.value,
		startDate: prdOutboundDate,
		createdId: processByEmpId.value,
		type: "FG",
		items
	};
	
	const res = await fetch("/inventory/outbound/fg/regist", {
		method: "POST",
		headers: {
			[csrfHeader]: csrfToken,
			"Content-Type": "application/json"
		},
		body: JSON.stringify(payload)
	});
	
	if (!res.ok) {
		console.error("요청 처리 중 오류가 발생했습니다.");
		return;
	}
	
	const result = await res.json();
	
	alert("출고 등록이 완료되었습니다." || result.message);
	
	setTimeout(() => {
		location.reload();
	}, 300);
}