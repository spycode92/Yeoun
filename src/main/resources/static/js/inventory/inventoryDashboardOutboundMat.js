const matObWorkOrderSelect  = document.querySelector("#matObWorkOrderSelect");
const matObManagerName = document.querySelector("#matObManagerName");
const matObProductName = document.querySelector("#matObProductName");
const matObDueDate = document.querySelector("#matObDueDate");
const matObBomTbody = document.querySelector("#matObBomTbody");
const matObWorkId = document.querySelector("#matObWorkId");
const matObManagerId = document.querySelector("#matObManagerId");

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
matObWorkOrderSelect.addEventListener("focus", async () => {
	workOrderList = await loadOrderList();
	
	if (workOrderList.length === 0) {
		return;
	}
	
	matObWorkOrderSelect.innerHTML = `<option value="">작업지시서를 선택하세요</option>`;
	
	// 옵션 추가
	await workOrderList.forEach(el => {
		const opt = document.createElement("option");
		opt.value = el.orderId;
		opt.textContent = `${el.orderId} - ${el.productName}`;
		opt.dataset.productId = el.productId;
		opt.dataset.managerId = el.productId;
		
		matObWorkOrderSelect.appendChild(opt);
	});
});

// 작업지시 선택 시 상세 정보 입력
matObWorkOrderSelect.addEventListener("change", async () => {
	matObWorkId.value = matObWorkOrderSelect.value;
	let createdData = workOrderList.find(el => {
		return el.orderId === matObWorkId.value;
	})
	
	matObManagerId.value = createdData.createdId;
	const obWorkId = matObWorkId.value;
	if (!obWorkId) return;
	
	// 선택한 작업지시서 리스트에서 찾기
	const workOrder = workOrderList.find(el => el.orderId === obWorkId); 
	
	if (!workOrder) {
		alert("작업지시 데이터를 찾을 수 없습니다.");
		return;
	}
	
	// 선택한 작업지시에 따른 담당자, 제품명, 출고일 정보 입력
	matObManagerName.value = workOrder.createdUserName;
	matObProductName.value = workOrder.productName + "|" + workOrder.planQty;
	
	// 오늘 날짜 구하기(한국 시간)
	const now = new Date();
	const today = new Date(now.getTime() - (now.getTimezoneOffset() * 60000))
			.toISOString()
			.split("T")[0];
			
	matObDueDate.min = today;
	
	matObDueDate.value = workOrder.planStartDate?.split("T")[0] || today;
	
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
		matObBomTbody.innerHTML = "";

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
					<td data-qty="${needQty}" name="needQty">${needQty}</td>
					<td data-stock="${stock.stock}" name="stock">${stock.stock}</td>
					<td>
						<input type="number" class="form-control outboundQty" min="0">
						<input type="hidden" name="matId" value="${bom.matId}"/>
					</td>
				</tr>
			`;
			
			matObBomTbody.insertAdjacentHTML("beforeend", row);
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
	const rows = document.querySelectorAll("#matObBomTbody tr");
	
	// 유효성 검사 플래그
	let isValid = true;
	let errorMessage = "";
	
	// 출고 품목들을 items에 추가
	for(const row of rows) {
		const needQty = Number(row.querySelector("td[name=needQty]").dataset.qty);
		// 출고 수량
		const outboundQty = Number(row.querySelector(".outboundQty").value);
		// 재고수량
		const stock = Number(row.querySelector("td[name=stock]").dataset.stock);
		// 출고 수량과 재고 수량 비교
		if (outboundQty > stock) {
			isValid = false;
			errorMessage = "출고 수량이 재고 수량보다 많습니다.";
			break;
		} 

		// 출고 수량과 요청 수량 비교
		if (needQty > outboundQty) {
			isValid = false;
			errorMessage = "출고 수량이 요청 수량보다 적습니다.";
			break;
		}
		
		// 출고 수량과 요청 수량 비교
		if (needQty < outboundQty) {
			isValid = false;
			errorMessage = "출고 수량이 요청 수량보다 많습니다.";
			break;
		}

		items.push({
			matId : row.querySelector("input[name=matId]").value,
			outboundQty
		});
	}
	
	// 검사 실패 시 종료
	if (!isValid) {
		alert(errorMessage);
		return;
	}
	
	// 스피너 시작
	showSpinner();
	
	try {
		// body에 담아서 보낼 내용
		const payload = {
			workOrderId: matObWorkId.value,
			createdId: matObManagerId.value,
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
			throw new Error("서버 오류가 발생했습니다.");
		}
		
		const result = await res.json();
		
		alert("출고 등록이 완료되었습니다." || result.message);
		
		setTimeout(() => {
			location.reload();
		}, 300);
		
	} catch (error) {
		console.error(error);
		alert("요청 처리 중 오류가 발생했습니다." || error.message);
	} finally {
		//스피너  off
		hideSpinner(); 
	}
}

