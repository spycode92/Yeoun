const productGrid = new tui.Grid({
	el: document.getElementById("productGrid"),
	bodyHeight: 500,
	rowHeaders: ['rowNum'],
	pageOptions: {
	    useClient: true,  // 클라이언트 사이드 페이징
	    perPage: 20       // 페이지당 20개 행
	},	
	columnOptions: {
		resizable: true
	},
	columns: [
		{
			header: "출고번호",
			name: "outboundId",
		},
		{
			header: "거래처명",
			name: "clientName",
		},
		{
			header: "출고예정일",
			name: "startDate",
			sortable: true,
			formatter: ({value}) => formatDate(value)
		},
		{
			header: "출고일",
			name: "outboundDate",
			sortable: true,
			formatter: ({value}) => formatDate(value)
		},
		{
			header: "상태",
			name: "status",
			filter: "select"
		},
		{
			header: " ",
			name: "btn",
			formatter: (rowInfo) => {
				return `<button class="btn btn-outline-info btn-sm" data-id="${rowInfo.row.id}">상세</button>`
			}
		}
	]
});

// 상세 버튼 클릭 동작
productGrid.on("click", (ev) => {
	const { columnName, rowKey } = ev;
	
	if (columnName === "btn") {
		const row = productGrid.getRow(rowKey);
		// 입고 상세 페이지로 이동
		location.href = `/inventory/outbound/prd/${row.outboundId}`
	}
});

const prdStartDateInput = document.querySelector("#prdStartDate");
const prdEndDateInput = document.querySelector("#prdEndDate");

// 날짜 포맷 함수
function formatDate(isoDate) {
	if (!isoDate) return "";
	
	return isoDate.split("T")[0]; // YYYY-MM-dd 형식
}

// 출고 정보 불러오기
async function loadProductOutbound(startDate, endDate, keyword) {
	const MATERIAL_OUTBOUND_LIST = 
		`/inventory/outbound/list/data` +
		`?startDate=${startDate}` +
		`&endDate=${endDate}` +
		`&keyword=${keyword}`;
			
	try {
		const res = await fetch(MATERIAL_OUTBOUND_LIST, {method: "GET"});
		
		if (!res.ok) {
			throw new Error("데이터 로드 실패!");
		}
		
		const data = await res.json();
		
		let prdOutboundList  = data.filter(item => item.shipmentId != null);
		
		// 데이터가 없을 경우 빈배열 반환
		if (!prdOutboundList  || prdOutboundList .length === 0) {
			productGrid.resetData([]);
		}
		
		const statusMap = {
			WAITING : "출고대기",
			COMPLETED: "출고완료",
			CANCELED: "출고취소"
		}
		
		// 상태값이 영어로 들어오는 것을 한글로 변환해서 기존 data에 덮어씌움
		prdOutboundList  = prdOutboundList .map(item => ({
			...item,
			status: statusMap[item.status] || item.status
		}));
		
		productGrid.resetData(prdOutboundList );
		
	} catch (error) {
		console.error(error);
	}
}

// 페이지 뒤로가기에만 지정한 날짜 적용되게 하는 로직
window.addEventListener("pageshow", async (e) => {
	const isBackForward = e.persisted || performance.getEntriesByType("navigation")[0].type ===  "back_forward";
	
	let startDate;
	let endDate;
	
	if (isBackForward) {
		// 뒤로 가기 했을 경우 이전 조회 날짜 유지
		startDate = sessionStorage.getItem("product_startDate");
		endDate = sessionStorage.getItem("product_endDate");
	}
	
	if (!startDate || !endDate) {
		const today = new Date();
	    const year  = today.getFullYear();
	    const month = today.getMonth() + 1;
	    const day   = today.getDate();
		
		startDate = `${year}-${String(month).padStart(2, "0")}-01`;
		endDate   = `${year}-${String(month).padStart(2, "0")}-${String(day).padStart(2, "0")}`;
		
		sessionStorage.removeItem("product_startDate");
		sessionStorage.removeItem("product_endDate");
	}
	
	prdStartDateInput.value = startDate;
	prdEndDateInput.value   = endDate;
	
	await loadProductOutbound(startDate, endDate, null);

	//스피너  off
	hideSpinner();
});

// 검색
document.querySelector("#search").addEventListener("click", async () => {
	const startDate = prdStartDateInput.value;
	const endDate = prdEndDateInput.value;
	const keyword = document.querySelector("#productKeyword").value;
	
	if (!startDate || !endDate) {
		alert("조회할 기간을 선택해주세요!");
		return;
	}
	
	await loadProductOutbound(startDate, endDate, keyword);
});

// 시작날짜 클릭 시 데이터 조회
document.querySelector("#prdStartDate").addEventListener("input", async () => {
	const startDate = prdStartDateInput.value;
	const endDate = prdEndDateInput.value;
	const keyword = document.querySelector("#productKeyword").value;
	
	sessionStorage.setItem("product_startDate", prdStartDateInput.value);
	sessionStorage.setItem("product_endDate", prdEndDateInput.value);
	
	await loadProductOutbound(startDate, endDate, keyword);
});

// 종료날짜 클릭 시 데이터 조회
document.querySelector("#prdEndDate").addEventListener("input", async () => {
	const startDate = prdStartDateInput.value;
	const endDate = prdEndDateInput.value;
	const keyword = document.querySelector("#productKeyword").value;
	
	sessionStorage.setItem("product_startDate", prdStartDateInput.value);
	sessionStorage.setItem("product_endDate", prdEndDateInput.value);
	
	await loadProductOutbound(startDate, endDate, keyword);
});


// =============================================================
// 출고 등록 로직
const shipmentSelect = document.querySelector("#shipmentSelect");
const processByName = document.querySelector("#processByName");
const processByEmpId = document.querySelector(".processById");
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
		processByEmpId.value = el.createdId;
		
		shipmentSelect.appendChild(opt);
	});
	
	shipmentSelect.addEventListener("change", async () => {
		const shipId = shipmentSelect.value;
		
		if (!shipId) return;
		
		// 선택한 출하지시서 리스트에서 찾기
		const shipOrder = shipmentList.find(el => el.shipmentId === shipId);
		
		if (!shipOrder) {
			alert("출하지시서 데이터를 찾을 수 없습니다.");
			return;
		}
		
		// 선택한 출하지시서에 따른 담당자, 거래처명, 출고일 정보 입력
		processByName.value = shipOrder.createdName;
		shopClientName.value = shipOrder.clientName;
		
		// 오늘 날짜 구하기(한국 시간)
		const now = new Date();
		const today = new Date(now.getTime() - (now.getTimezoneOffset() * 60000))
				.toISOString()
				.split("T")[0];
				
		// 오늘 이전 날자 선택 불가
		expectDate.min = today;
		
		// startDate가 없으면 오늘 날짜를 기본값으로 하거나 0 처리
		expectDate.value = shipOrder.startDate?.split("T")[0] || today;
		
		prdOutboundDate = shipOrder.startDate;
		
		// 선택한 출하지시서의 품목 리스트 렌더링
		renderProductList(shipOrder.items);
		
	});
});

// 출고 버튼 클릭(모달 초기화)
document.querySelector("#prdRegistBtn").addEventListener("click", () => {
	restPrdModal();
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
					<input type="number" class="form-control outboundQty" min="0">
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
	
	// 출고 품목들 items 추가
	for (const row of rows) {
		const shipmentQty = Number(row.querySelector("td[name=shipmentQty]").dataset.qty);
		// 출고 수량
		const outboundQty = Number(row.querySelector(".outboundQty").value);
		
		// 출고 수량과 요청 수량 비교
		if (outboundQty > shipmentQty) {
			alert("출고 수량이 요청 수량보다 많습니다.");
			return;
		}
		
		// 출고 수량과 요청 수량 비교
		if (shipmentQty > outboundQty) {
			alert("출고 수량이 요청 수량보다 적습니다.");
			return;
		}
		
		items.push({
			prdId: row.querySelector("input[name=prdId]").value,
			outboundQty: row.querySelector(".outboundQty").value
		});
		
	}
	
	if (shipmentId.value === "" || shipmentId.value === null) {
		alert("출하지시서를 선택해주세요.");
		return;
	}
	
	if (items.length <= 0) {
		alert("등록할 물품이 없습니다.");
		return;
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
		location.href = "/inventory/outbound/productList";
	}, 300);
}

// 모달 초기화
function restPrdModal() {
	shipmentSelect.innerHTML = '<option value="">출하지시서를 선택하세요</option>';
	processByName.vlaeu = "";
	shopClientName.value = "";
	expectDate.value = "";
	shipTbody.innerHTML = "";
}