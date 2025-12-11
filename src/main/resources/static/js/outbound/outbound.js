const workOrderSelect  = document.querySelector("#workOrderSelect");
const managerName = document.querySelector("#managerName");
const productName = document.querySelector("#productName");
const dueDate = document.querySelector("#dueDate");
const bomTbody = document.querySelector("#bomTbody");
const workId = document.querySelector("#workId");
const managerId = document.querySelector("#managerId");

// 작업지시서 전역변수로 저장
let workOrderList = [];
let outboundDate;

// 작업지시 정보 가져오기
async function loadOrderList() {
	try {
		const res = await fetch("/order/orderList/data", {method: "GET"});
		
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

// 작업지시 선택 이벤트
workOrderSelect.addEventListener("focus", async () => {
	workOrderList = await loadOrderList();
	
	if (workOrderList.length === 0) {
		return;
	}
	
	workOrderSelect.innerHTML = `<option value="">작업지시서를 선택하세요</option>`;
	
	// 옵션 추가
	workOrderList.forEach(el => {
		const opt = document.createElement("option");
		opt.value = el.orderId;
		opt.textContent = `${el.orderId} - ${el.productName}`;
		opt.dataset.productId = el.productId;
		workId.value = el.orderId;
		managerId.value = el.createdId;
		
		workOrderSelect.appendChild(opt);
	});
});

// 작업지시 선택 시 상세 정보 입력
workOrderSelect.addEventListener("change", async () => {
	const workId = workOrderSelect.value;
	
	if (!workId) return;
	
	// 선택한 작업지시서 리스트에서 찾기
	const workOrder = workOrderList.find(el => el.orderId === workId); 
	
	if (!workOrder) {
		alert("작업지시 데이터를 찾을 수 없습니다.");
		return;
	}
	
	// 선택한 작업지시에 따른 담당자, 제품명, 출고일 정보 입력
	managerName.value = workOrder.createdUserName;
	productName.value = workOrder.productName + "|" + workOrder.planQty;
	dueDate.value = workOrder.planStartDate?.split("T")[0] || "0";
	
	outboundDate = workOrder.planStartDate;
	
	// BOM 조회
	loadBomList(workOrder.productId, workOrder.planQty);
});

// BOM 조회
async function loadBomList(productId, planQty) {
	try {
		const res = await fetch(`/bom/list/data/${productId}`);
		const bomList = await res.json();
		
		// bom 목록 초기화
		bomTbody.innerHTML = "";

		// 반복문을 사용해서 필요한 원재료 품목 tbody에 넣기		
		for (let bom of bomList) {
			// 필요수량 구하기 (bom에 등록된 원재료 사용량 * 작업지시서 수량)
			const needQty = bom.matQty * planQty;
			
			const stockRes = await fetch(`/api/inventories/stock/${bom.matId}`);
			const stock = await stockRes.json();
			
			const row = `
				<tr>
					<td >${bom.matId}</td>
					<td>${bom.matName}</td>
					<td>${bom.matUnit}</td>
					<td>${needQty}</td>
					<td>${stock.stock}</td>
					<td>
						<input type="number" class="form-control outboundQty" min="0">
						<input type="hidden" name="matId" value="${bom.matId}"/>
					</td>
				</tr>
			`;
			
			bomTbody.insertAdjacentHTML("beforeend", row);
		}
		
	} catch(error) {
		console.error(error);
		alert("BOM을 조회할 수 없습니다.");
	}
}

// 출고 등록
const submitOutbound = async () => {
	// 출고 품목을 담을 변수
	const items = [];
	
	// 출고 품목들을 items에 추가
	document.querySelectorAll("#bomTbody tr").forEach(tr => {
		items.push({
			matId : tr.querySelector("input[name=matId]").value,
			outboundQty: tr.querySelector(".outboundQty").value
		});
	});
	
	// body에 담아서 보낼 내용
	const payload = {
		workOrderId: workId.value,
		createdId: managerId.value,
		startDate: outboundDate,
		type: "MAT",
		items
	};
	
	const res = await fetch("/inventory/outbound/mat/regist", {
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

