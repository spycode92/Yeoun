const clientInput = document.querySelector("#clientSearch"); // 거래처명 입력하는 input 태그
const clientResultBox = document.querySelector("#clientResult"); // 거래처명 검색하고 나오는 리스트 ul 태그
const itemSelect = document.querySelector("#itemSelect"); // 발주 품목 선택할 select 태그
const dueDateInput = document.querySelector("#dueDate"); // 입고예정일 날짜 input 태그
const orderTableBody = document.querySelector("#orderTable tbody"); // 선택한 품목이 등록될 table 태그
const hiddenId = document.querySelector("#clientId");

// 공급업체 데이터 가져오기
const supplierList = async () => {
	try {
		const res = await fetch("/purchase/supplier/data", {method: "GET"});
		
		if (!res.ok) {
			throw new Error("데이터 로드 실패!");
		}
		
		const data = await res.json();
		
		return data;
	} catch (error) {
		console.error(error);
	}
}

// foucs 했을 때 동작할 이벤트
clientInput.addEventListener("focus", async () => {
	const data = await supplierList();
	
	// 전체 리스트를 보여주기 때문에 data만 넣음
	renderClientList(data, data);
});


// 거래처 검색 이벤트 (검색어 입력했을 때)
clientInput.addEventListener("input", async function() {
	resetClientSelection();
	
	// input에 입력한 검색어
	const keyword = clientInput.value.trim().toLowerCase();
	
	// 거래처 데이터 가져오기
	const data = await supplierList();
	
	// 검색어가 없으면 전체 리스트를 보여주고 있으면 검색된 리스트만 보여주기
	const filtered = (keyword.length === 0)
		? data
		: data.filter(item => item.clientName.toLowerCase().includes(keyword));
	
	
	renderClientList(filtered, data);
});

// 리스트 렌더링 공통 사용 함수
function renderClientList(filtered, data) {
	// 기존 내용 초기화
	clientResultBox.innerHTML = "";
	
	filtered.forEach(item => {
		const li = document.createElement("li");
		li.className = "list-group-item list-group-item-action";
		li.textContent = item.clientName;
		
		// 클릭 시 업체 선택
		li.addEventListener("click", () => {
			selectClient(item, data);
			
			// 다른 업체 선택 시 초기화 시킴
			orderTableBody.innerHTML = "";
		});
		
		// 검색 결과 보여주는 곳에 검색된 결과 추가
		clientResultBox.appendChild(li);
	});
	
	// 선택 완료되면 리스트 화면 안보이기
	clientResultBox.classList.remove("d-none");
}

// 거래처 이름 클릭 후 해당 거래처의 품목 보여주는 로직
function selectClient(item, data) {
	clientInput.value = item.clientName;
	hiddenId.value = item.clientId;
	clientResultBox.classList.add("d-none");
	
	// 선택된 거래처 객체 찾기
	const selectedClient = data.find(client => client.clientId === item.clientId);
	
	// 담당자 이름 설정
	document.querySelector("#managerName").value = selectedClient.managerName;
	
	// 공급품목 중 공급가능여부가 Y인 것만 표시
	const availableItems = selectedClient.supplierItemList.filter(item => item.supplyAvaliable === "Y");
	
	// 기존 내용 초기화
	itemSelect.innerHTML = "";
	
	// 기본 안내 옵션
	const defaultOption = document.createElement("option");
	defaultOption.value = "";
	defaultOption.textContent = "품목을 선택하세요";
	defaultOption.selected = true; // 기본으로 선택
	itemSelect.appendChild(defaultOption);
	
	// 공급 가능한 품목이 없는 경우
	if (availableItems.length === 0) {
		const option = document.createElement("option");
		
		option.value = "";
		option.textContent = "선택할 품목 없음";
		option.disabled = true;
		itemSelect.appendChild(option);
		itemSelect.disabled = true; 
		return;
	}
	
	// 공급 가능한 품목이 있는 경우 목록 나옴
	availableItems.forEach(item => {
		const option = document.createElement("option");
		
		option.value = item.itemId;
		option.textContent = item.matName;
		option.dataset.minOrderQty = item.minOrderQty;
		option.dataset.unitPrice  = item.unitPrice;
		option.dataset.orderUnit  = item.orderUnit;
		option.dataset.unit  = item.unit;
		option.dataset.leadDays   = item.leadDays;
		option.dataset.materialId   = item.materialId;
		
		itemSelect.appendChild(option);
	});
	
	itemSelect.disabled = false;
}


// 품목 선택 후 테이블 추가 및 납기일 계산 
itemSelect.addEventListener("change", () => {
	const option = itemSelect.options[itemSelect.selectedIndex];
	// 값이 없으면 리턴
	if (!option.value) return;
	
	// 중복 품목 체크 로직
	let isDuplicate = false;
	const existingRow = orderTableBody.querySelectorAll("tr");
	const selectedItemId = option.value;
	const orderUnit = parseInt(option.dataset.orderUnit) || 1; // 주문단위
	
	existingRow.forEach(row => {
		 const itemIdInput = row.querySelector("input[name=itemId]");
		 
		 if (itemIdInput && itemIdInput.value === selectedItemId) {
			// 이미 존재하는 품목이면 수량만 증가
			const qtyInput = row.querySelector(".orderQty");
			let currentQty = parseInt(qtyInput.value);
			
			// 주문 단위만큼 증가
			currentQty += orderUnit;
			qtyInput.value = currentQty;
			
			// change 이벤트 강제 발생시켜 가격 재계산 로직 실행
			qtyInput.dispatchEvent(new Event("change", { bubbles: true }));
			
			isDuplicate = true;
		 }
	});
	
	// 중복이면 함수 종료(새 행을 추가 하지 않음)
	if (isDuplicate) {
		itemSelect.value = "";
		return;
	}
	
	
	// 선택한 품목의 리드타임 적용(오늘 날짜 + 리드타임)
	const leadDays = parseInt(option.dataset.leadDays, 10);
	
	const now = new Date();
	const minDate = new Date(now.getTime() - (now.getTimezoneOffset() * 60000))
	                .toISOString()
	                .split("T")[0];
	
	// 오늘 날짜 이전 선택 불가
	dueDateInput.min = minDate;
	
	const targetDate  = new Date();
	targetDate .setDate(targetDate .getDate() + leadDays);
	
	const yyyy = targetDate .getFullYear();
	const mm = String(targetDate .getMonth() + 1).padStart(2, "0");
	const dd = String(targetDate .getDate()).padStart(2, "0");
	
	dueDateInput.value = `${yyyy}-${mm}-${dd}`;
	
	const itemName = option.textContent;
	const minOrder = parseInt(option.dataset.minOrderQty);
	const unitPrice = parseInt(option.dataset.unitPrice);
	
	const supplyPrice = minOrder * unitPrice;
	const tax = Math.round(supplyPrice * 0.1);
	const total = supplyPrice + tax;
	
	// 테이블에 추가할 row 작성
	const row = `
		<tr>
			<td>${itemName}</td>
			<td>${minOrder}</td>
			<td>${option.dataset.unit}</td>
			<td>
				<input 
					type="number" 
					class="form-control orderQty" 
					value="${minOrder}" 
					min="${minOrder}" 
					step="${option.dataset.orderUnit}"
					data-min="${minOrder}"
					data-order-unit="${option.dataset.orderUnit}"
					data-price="${unitPrice}"
					name="orderAmount"
					/>
				<input type="hidden" name="itemId" value="${option.value}">
				<input type="hidden" name="vat" value="${tax}">
				<input type="hidden" name="unitPrice" value="${unitPrice}">
				<input type="hidden" name="unit" value="${option.dataset.unit}">
			</td>
			<td>${unitPrice.toLocaleString()}</td>
			<td class="supplyPrice">${supplyPrice.toLocaleString()}</td>
			<td class="taxPrice">${tax.toLocaleString()}</td>
			<td class="totalPrice">${total.toLocaleString()}</td>
			<td><button class="btn btn-primary btn-sm btnDelete">삭제</button></td>
		</tr>
	`;
	
	// table에 row 추가
	orderTableBody.insertAdjacentHTML("beforeend", row);
	
	// 삭제 버튼 기능
	const newestRow = orderTableBody.lastElementChild;
	newestRow.querySelector(".btnDelete").addEventListener("click", () => {
		newestRow.remove();
	});
});

// 주문수량 변경 이벤트
orderTableBody.addEventListener("change", (e) => {
	 if (!e.target.classList.contains("orderQty")) return;
	 
	 const qtyInput = e.target;
	 const minOrder = parseInt(qtyInput.dataset.min);
	 const unit = parseInt(qtyInput.dataset.orderUnit);
	 const price = parseInt(qtyInput.dataset.price);
	 
	 let qty = parseInt(qtyInput.value);

	 	 // 최소 주문수량 체크
	 if (qty < minOrder) {
	     qty = minOrder;
	     qtyInput.value = qty;
	 }
	 
	 // 주문 단위 체크
	 if (qty % unit !== 0) {
		qty = Math.ceil(qty / unit) * unit;
		
		if (qty < minOrder) {
			qty = minOrder;
		}
		
		qtyInput.value = qty;
	 }
	 
	 // 공급가액, 부가세, 총 금액 계산
	 const supply = qty * price;
	 const tax = Math.round(supply * 0.1);
	 const total = supply + tax;
	 
	 // DOM 업데이트
	 const tr = qtyInput.closest("tr");;
	 tr.querySelector(".supplyPrice").textContent = supply.toLocaleString();
	 tr.querySelector(".taxPrice").textContent = tax.toLocaleString();
	 tr.querySelector(".totalPrice").textContent = total.toLocaleString();
});

// 거래처 선택 해제용 함수
function resetClientSelection() {
	hiddenId.value = "";
	
	document.querySelector("#managerName").value = "";
	
	itemSelect.innerHTML = "";
	itemSelect.disabled = true;
	
	const defaultOption = document.createElement("option");
	
	defaultOption.value = "";
	defaultOption.textContent = "거래처를 먼저 선택하세요.";
	defaultOption.selected = true;
	itemSelect.appendChild(defaultOption);
	
	// 발주 테이블 초기화
	orderTableBody.innerHTML = "";
}

// ----------------------------------------------------------
// 발주 등록
const submitOrder = async () => {
	const clientId = hiddenId.value;
	const dueDate = dueDateInput.value;
	
	// 선택한 품목들을 담을 변수
	const items = [];
	
	// 선택된 품목들을 items에 추가
	document.querySelectorAll("#orderTable tbody tr").forEach(tr => {
		items.push({
			itemId: tr.querySelector("input[name=itemId]").value,
			orderAmount: tr.querySelector("input[name=orderAmount]").value,
			unitPrice: tr.querySelector("input[name=unitPrice]").value,
		});
	});
	
	if (clientId.trim().length === 0) {
		alert("거래처를 선택해주세요.");
		return;
	}
	
	if (items.length <= 0) {
		alert("품목을 선택해주세요.");
		return;
	}
	
	if (dueDate.value === null || dueDate.value === "") {
		alert("예상도착일을 입력해주세요.");
		return;
	}
	
	// body에 담아서 보낼 내용
	const payload = {
		clientId,
		dueDate,
		supplierItemList: items
	};
	
	const res = await fetch("/purchase/purchaseOrder", {
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
	
	alert("발주가 완료되었습니다." || result.message);
	
	setTimeout(() => {
		location.reload();
	}, 300);
}






