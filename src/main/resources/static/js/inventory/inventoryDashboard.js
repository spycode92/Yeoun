// 전역변수
let inventoryInfo; // 창고정보
let inventorySafetyStockInfo; // 안전재고 정보
let todayInboundData; // 입고정보
let todayOutboundData; // 입고정보
let chartData; // 가공한차트데이터
let orderData; //작업지시서데이터
let ivOrderCheckData;

let outboundNOrderGrid; // 작업지시서그리드객체
let outboundShipmentGrid; // 출하지시서 그리드 객체

let viewMode = 'month'; // 차트 뷰모드
let trendChart; // 차트객체

let safetyStockGrid; // 안전재고그리드객체
let expireDisposalGrid; // 유통기한관리그리드객체

let islistOn = false;

const today = new Date();

// 스피너 보이기 끄기
function showSpinner() {
	document.getElementById('loading-overlay').style.display = 'flex';
}
 function hideSpinner() {
	document.getElementById('loading-overlay').style.display = 'none';
}

document.addEventListener('DOMContentLoaded', async function () {
	//스피너 on
	showSpinner();
	
	// 재고정보 
	inventoryInfo = await fetchNotNormalInventoryData();
//	console.log("@@@@@@",inventoryInfo);
	// 안전재고 수량정보
	inventorySafetyStockInfo = await fetchInventorySafetyStockData();
	// 오늘 입고 정보
	todayInboundData = await fetchTodayInboundData();
	// 오늘 출고 정보
	todayOutboundData = await fetchTodayOutboundData();
	// 차트데이터
	const RawChartData = await fetchIvHistoryData();
	// 차트데이터 가공
	chartData = normalizeIvHistory(RawChartData);
	// 작업지시서 데이터
	orderData = await fetchOrderListData();
	// 발주체크 데이터
	ivOrderCheckData = await fetchIvOrderCheckData();
//	console.log(ivOrderCheckData,'@@@@@@@@@@@@@@@@@@@@@@@@@@@@@@');

		
	// 차트타입 버튼 active 설정
	document.querySelectorAll('.chart-type').forEach(btn => {
	    btn.addEventListener('click', onChartTypeClick);
	});

// -----------------------------------------------------------------------
// 상단 카드 
	// 금일 입고 예정양
	const todayInboundTotalEl = document.getElementById('todayInboundTotal');
	todayInboundTotalEl.innerHTML = todayInboundData.length;
	// 금일입고 처리완료 수 조회
	let IbCompleteCnt = 0;
	todayInboundData.forEach(inbound => {
		if(inbound.inboundStatus ==='COMPLETED') IbCompleteCnt++;
	});
	// 금일입고처리완료수 보이기 
	const todayInboundCompleteEl = document.getElementById('todayInboundComplete');
	todayInboundCompleteEl.innerHTML = `<i class='bx bx-up-arrow-alt'></i>처리 : ${IbCompleteCnt}`;
	
	// 금일 출고 예정양
	const todayOutboundTotalEl = document.getElementById('todayOutboundTotal');
	todayOutboundTotalEl.innerHTML = todayOutboundData.length;
	// 금일 출고 처리 완료 수 조회
	let ObCompleteCnt = 0;
	todayOutboundData.forEach(outbound => {
		if(outbound.status === 'COMPLETED') ObCompleteCnt++;
	})
	// 금일 출고 처리완료수 보이기
	const todayOutboundCompleteEl = document.getElementById('todayOutboundComplete');
	todayOutboundCompleteEl.innerHTML = `<i class='bx bx-down-arrow-alt'></i> 처리 : ${ObCompleteCnt}`

	// 출고등록 필요한 작업지시서 표시
	await renderOrderGrid();
	// 출고등록 필요한 출하지시서 표시\
	await renderShipmentGrid();
	// 하나라도 있으면 div 표시
	// 그리드 보이는 카드
	const cardEl = document.getElementById('outboundNOrderCard');
	if(islistOn) {
		cardEl.style.display = 'block';
//		console.log(islistOn,"!!!!!!!!!!!!!!");
	} else {
		cardEl.style.display = 'none';
//		console.log(islistOn,"@@@@@@@@@@@");
	}

		
	//발주 필요 수량 표시
	let lowStockCnt = 0;
	ivOrderCheckData.forEach(stock => {
		// 출고 예정 수량 : 생산계획수량 - 작업지시(출고완)수량
		const EXPECT_OBPLAN_QTY = stock.productPlanQty - stock.outboundPlanQty;
		// 예상 재고 수량(재고수량 - 예상출고수량) + 예상입고수량
		const EXPECT_IV_QTY = stock.expectIvQty + stock.expectIbQty;
		// 최종 재고 수량 : 예상재고수량 + 예상입고수량 - (생산계획수량 - 작업지시수량)
		const FINAL_IV_QTY = EXPECT_IV_QTY - EXPECT_OBPLAN_QTY;
		const SAFETY_QTY = stock.safetyQty
		
		if(FINAL_IV_QTY < SAFETY_QTY) lowStockCnt++;
	})
//	inventorySafetyStockInfo.forEach(stock => {
//		// 안전재고수량보다 예상재고량(재고량 - 출고예정량)이 작을경우
//		if(stock.safetyStockQty > stock.expectIvQty + stock.expectIbAmount) lowStockCnt++;
//	})
	const orderEl = document.getElementById('orderCnt');
	orderEl.innerText = `${lowStockCnt} / ${ivOrderCheckData.length}`;
	
	const orderStatusEl = document.getElementById('orderStatus');
	if (lowStockCnt > 0) {
	    orderStatusEl.className = 'text-warning fw-semibold';
	    orderStatusEl.innerHTML = '발주 필요';
	} else {
	    orderStatusEl.className = 'text-success', 'fw-semibold'; 
	    orderStatusEl.innerHTML = '정상';
	}
	// 발주필요 목록 표시
	// 안전재고 미달목록표시
	await renderNeedOrderStockGrid();
	
	//유통기한 임박 수량 표시
	let expireCnt = 0;
	let disposalCnt = 0;
	inventoryInfo.forEach(iv => {
		if(iv.ivStatus === 'DISPOSAL_WAIT') expireCnt++;
		if(iv.ivStatus === 'EXPIRED') disposalCnt++;
	})
	
	const expireEl = document.getElementById('expireCnt');
	expireEl.innerHTML = `임박 : ${expireCnt}`
	const disposalEl = document.getElementById('disposalCnt');
	disposalEl.innerHTML = `폐기 : ${disposalCnt}`
	
	// 유통기한관리 목록 표시
	await renderExpireDisposalGrid();
	
// -------------------------------------------------------------------------------
// 입출고 차트 데이터 입력
	// 차트옵션설정, 차트생성
	trendChart = new ApexCharts(document.querySelector("#trendChart"), trendOptions);
	trendChart.render();
	
	const aggregated = aggregateByMode(chartData, viewMode);
	const { labels, series } = buildChartSeries(aggregated);
	
	trendChart.updateOptions({
	    labels: labels,
	    xaxis: { type: 'category' },
	    series: series
	}, false, true);
	
	
	//스피너  off
	hideSpinner();	
});

// ------------------------------------
// 차트옵션
// 2. Trend Chart (Bar + Line)
const trendOptions = {
    series: [{
        name: '입고',
        type: 'column',
		data: []
    }, {
        name: '출고',
        type: 'area',
		data: []
    }, {
        name: '폐기',
        type: 'line',
		data: []
    }],
    chart: {
        height: 350,
        type: 'line',
        stacked: false,
    },
    stroke: {
        width: [0, 2, 5],
        curve: 'smooth'
    },
    plotOptions: {
        bar: {
            columnWidth: '50%'
        }
    },
    fill: {
        opacity: [0.85, 0.25, 1],
        gradient: {
            inverseColors: false,
            shade: 'light',
            type: "vertical",
            opacityFrom: 0.85,
            opacityTo: 0.55,
            stops: [0, 100, 100, 100]
        }
    },
    markers: {
        size: 0
    },
    yaxis: {
        title: {
            text: '수량',
        },
        min: 0
    },
    tooltip: {
        shared: true,
        intersect: false,
        y: {
            formatter: function (y) {
                if (typeof y !== "undefined") {
                    return y.toFixed(0) + " 건";
                }
                return y;

            }
        }
    }
};

// -------------------------------------------------------------
// 재고내역데이터를 차트에 사용할 수있는 데이터로 가공

// 재고내역이 없는 날자 생성
function buildDateList(startDate, endDate) {
	const dates = [];
	const d = new Date(startDate);
	const end = new Date(endDate);
	// 시작일이 종료일이 될때까지 날자 입력('YYYY-MM-DD')
	while (d <= end) {
		dates.push(d.toISOString().slice(0, 10));
		d.setDate(d.getDate() + 1);
	}
	return dates;
}

// 재고내역 데이터를 없는날자에 0을넣어서 생성(rawData : 재고데이터fetchIvHistoryData결과)
function normalizeIvHistory(rawData) {
	// 워크타입 설정
	const WORK_TYPES = ['INBOUND', 'OUTBOUND', 'DISPOSE'];
	// buildDateList를 통해 오늘부터 1년전까지 날자리스트 생성
	const dateList = buildDateList(rawData.startDate, rawData.endDate);
	
	// rowData를 가공해서 맵으로 저장할 객체 생성
	const map = new Map();
	// rowData의 날자_워크타입을 키로 데이터저장
	rawData.data.forEach(row => {
	    const key = `${row.createdDate}_${row.workType}`;
	    map.set(key, row);
	});
	
	// 재고내역 데이터를 가공하여 리턴할 객체 생성
	const result = [];
	
	// 일자리스트 객체별 반복
	dateList.forEach(date => {
		// 일자별 워크타입 반복
	    WORK_TYPES.forEach(type => {
			// 키 : 날자_워크타입
	        const key = `${date}_${type}`;
			// 위에서 저장한 날자별 워크타입데이터 지정
	        const found = map.get(key);
			// 리턴객체에 데이터 저장
	        result.push({
	            createdDate: date,
	            workType: type,
	            sumCurrent: found ? found.sumCurrent : 0,
	            sumPrev: found ? found.sumPrev : 0,
	        });
	    });
	});

	return result;
}

// 뷰모드에 따른 그룹키설정
function getGroupKey(dateStr, mode) {
    const d = new Date(dateStr); // 'YYYY-MM-DD'
	//일별차트는 가공없이 리턴
    if (mode === 'day') { 
        return dateStr;
    }
	
	// 월별차트는 YYYY-MM 형태로 가공후 리턴
    if (mode === 'month') { 
        const y = d.getFullYear();
        const m = String(d.getMonth() + 1).padStart(2, '0');
        return `${y}-${m}`;
    }
	
	// 주별차트는 YY MMdd-MMdd로 설정
	if (mode === 'week') {
	    const base = new Date(dateStr);
	    const day = base.getDay(); // 0(일)~6(토)

	    // 월요일 시작 기준
	    const diffToMonday = (day === 0 ? -6 : 1 - day);
	    // 입력받은 날짜의 주 시작일 설정
		const weekStart = new Date(base);
	    weekStart.setDate(base.getDate() + diffToMonday);
		// 입력받은 날짜의 주 마지막일 설정
	    const weekEnd = new Date(weekStart);
	    weekEnd.setDate(weekStart.getDate() + 6);
		
		// 리턴할 정보로 가공
	    const y2 = String(weekStart.getFullYear()).slice(2); // '25'
		
	    const startMonth = String(weekStart.getMonth() + 1).padStart(2, '0');
	    const startDay   = String(weekStart.getDate()).padStart(2, '0');

	    const endMonth = String(weekEnd.getMonth() + 1).padStart(2, '0');
	    const endDay   = String(weekEnd.getDate()).padStart(2, '0');

	    return `${y2} ${startMonth}${startDay}-${endMonth}${endDay}`;
	}
}

// 재집계 함수
function aggregateByMode(chartData, mode) {
    const groups = new Map(); // key: groupKey + '_' + workType

    chartData.forEach(row => {
		// chartData의 각 날을 차트모드에 맞게 변형후 그룹키로 설정
        const groupKey = getGroupKey(row.createdDate, mode);
		// 각 날자별 입고,출고,폐기 구분하여 키 설정
        const key = `${groupKey}_${row.workType}`;
		// groups에 해당날자의 입고,출고,폐기 키가 존재하지않으면 키설정
        if (!groups.has(key)) {
            groups.set(key, {
                groupKey, // 날자
                workType: row.workType, // 입고,출고,폐기
                sumCurrent: 0, // 현재수량합계 = 0으로 설정
                sumPrev: 0 // 이전수량 합계 = 0으로 설정
            });
        }
		// 0으로 설정했던 수량합계에 값 넣어주기
        const g = groups.get(key);
        g.sumCurrent += row.sumCurrent;
        g.sumPrev    += row.sumPrev;
    });

    return Array.from(groups.values());
}

// 재집계함수를 토대로 차트에 넣어줄 데이터 생성함수
// aggregatedData : aggregateByMode(chartData, mode)
function buildChartSeries(aggregatedData) {
	// 라벨설정 : 재집계함수의 groupKey = mode별로 가공된 날짜데이터
    const labels = [...new Set(aggregatedData.map(row => row.groupKey))];
	// 날짜데이터 정렬
    labels.sort();
	
	// 재집계함수를 필요한 데이터로 가공하기 위한 맵 객체
    const map = new Map();
	
    aggregatedData.forEach(row => {
		// 키설정 : 날짜_워크타입
        const key = `${row.groupKey}_${row.workType}`;
		// 입고,출고,폐기 수량을 저장할 value
        let value = 0;
		
        if (row.workType === 'INBOUND') {
			// 입고의경우 현재수량 - 이전수량
            value = row.sumCurrent - row.sumPrev;
        } else if (row.workType === 'OUTBOUND' || row.workType === 'DISPOSE') {
			// 출고, 폐기의경우 이전수량 - 현재수량 
            value = row.sumPrev - row.sumCurrent;
        }
        map.set(key, value);
    });
	// 차트에 입력할 입고,출고,폐기 데이터리스트 객체
    const inboundData = [];
    const outboundData = [];
    const disposeData = [];
	
	// 각 날짜에 해당 입고,출고,폐기 데이터를 입력 
    labels.forEach(key => {
        inboundData.push(map.get(`${key}_INBOUND`)  ?? 0);
        outboundData.push(map.get(`${key}_OUTBOUND`) ?? 0);
        disposeData.push(map.get(`${key}_DISPOSE`)  ?? 0);
    });
	
	// 차트에 필요한 정보(그래프 이름, 타입, 데이터) series에 저장
    const series = [
        { name: '입고', type: 'column', data: inboundData },
        { name: '출고', type: 'area',   data: outboundData },
        { name: '폐기', type: 'line',   data: disposeData }
    ];

    return { labels, series };
}

// ------------------------------------------------------------------
// 차트 타입 변경 함수
function onChartTypeClick(event) {
	// 선택된 버튼
    const clicked = event.currentTarget;
    const mode = clicked.id === 'btnMonth'
        ? 'month'
        : clicked.id === 'btnWeek'
        ? 'week'
        : 'day';

    viewMode = mode;

    //모든 chart-type 버튼에서 active 제거
    document.querySelectorAll('.chart-type').forEach(btn => {
        btn.classList.remove('active');
    });

    //클릭된 버튼에만 active 추가
    clicked.classList.add('active');
	
	// 재고내역 데이터를 비어있는 날자를 채워서 재가공
	const aggregated = aggregateByMode(chartData, viewMode);
	// 차트를 그리기위한 날짜,옵션,데이터 설정
	const { labels, series } = buildChartSeries(aggregated);
	
	// 차트 옵션,데이터 업데이트
	trendChart.updateOptions({
	    labels: labels,
	    xaxis: { type: 'category' },
	    series: series
	}, false, true);
}

// -----------------------------------------------------------------------
// 출고지시 안된 작업지시서 그리드
async function renderOrderGrid() {
	// 출고지시안된 작업지시서, 출하지시서 체크 함수 
	islistOn = false;
	// 출고등록안한 작업지시서데이터	
	const outboundNList = orderData.filter(order => {
		return order.outboundYn === 'N';
	});
	
	// 그리드 보이는 카드
	if(outboundNList.length === 0) {
		return;
	}
	
	islistOn = true;	
	
	const gridEl = document.getElementById('outboundNOrderGrid');
	const Grid = tui.Grid;
	
	if(outboundNOrderGrid) {
		outboundNOrderGrid.distroy();
	}
	
	gridLangSet(Grid)
	
	// 미등록 출고 재고 그리드
	    outboundNOrderGrid = new Grid({
	        el: gridEl,
	//        scrollX: false,
	//        scrollY: true,
	        bodyHeight: 120,
	        rowHeaders: ['rowNum'],
			pageOptions: {
			    useClient: true, 
			    perPage: 5 
			},
			columnOptions: {
				resizable: true
			},
	        columns: [
	            { header: '작업지시서',   name: 'orderId', minWidth: 160 },
	            { header: '생산 품목', name: 'productName', minWidth: 110, align: 'center' },
	            { header: '작업 시작시간',     name: 'planStartDate', minWidth: 80,  align: 'center',
				  formatter: function({ value }) {
					  if (!value) return '';
					  return value.replace('T', ' ');
	      		  }	
				},
				{ header: '출고등록',      name: "btn", width: 100, align: "center",
				  formatter: (cellInfo) => "<button type='button' class='btn btn-outline-info btn-sm' data-row='${cellInfo.rowKey}' >출고등록</button>"
				}
	        ],
	        data: outboundNList
	    });
		
		// 출고등록 버튼 동작
		outboundNOrderGrid	.on("click", async (event) => {
			if(event.columnName == "btn") {
				const target = event.nativeEvent.target;
				if (target && target.tagName === "BUTTON") {
					
					const rowData = outboundNOrderGrid.getRow(event.rowKey);
//					console.log(rowData);

					//모달 열기
					const modalEl = document.getElementById("matObModal");
					const bsModal = new bootstrap.Modal(modalEl);
					bsModal.show();
//					console.log(rowData);
//					// 모달세팅
					await initOutboundModalByRow(rowData);
				}
			}
		});
}

async function initOutboundModalByRow(rowData) {
	// 초기화
	matObWorkOrderSelect.innerHTML = `<option value="">작업지시서를 선택하세요</option>`;
	matObManagerName.value = "";
	matObProductName.value = "";
	matObDueDate.value = "";
	matObBomTbody.innerHTML = "";
	matObWorkId.value = "";
	matObManagerId.value = "";
	outboundDate = null;
	
	// 작업리스트 목록
	workOrderList = await loadOrderList();
	
	if (!workOrderList || workOrderList.length === 0) {
	    alert("작업지시 데이터를 찾을 수 없습니다.");
	    return;
	}
	
	// 셀렉트 옵션 구성
	workOrderList.forEach(el => {
	    const opt = document.createElement("option");
	    opt.value = el.orderId;
	    opt.textContent = `${el.orderId} - ${el.productName}`;
	    opt.dataset.productId = el.productId;
	    matObWorkOrderSelect.appendChild(opt);
	});
	
	// 그리드에서 선택한 작업지시서 선택상태로변경
	matObWorkOrderSelect.value = rowData.orderId;
	
	
	// change 이벤트 강제 발생
	const changeEvent = new Event("change");
	matObWorkOrderSelect.dispatchEvent(changeEvent);
	
}

// -------------------------------------------------------------------
// 출고지시 내려지지않은 출하지시서 그리드
async function renderShipmentGrid() {
	const shipmentData = await loadShipmentList();
//	const filteredData = shipmentData.filter(shipment => {
//		shipment.
//	})
	// 그리드 보이는 카드
	if(shipmentData.length === 0) {
		return;
	}
	islistOn = true;

	const gridEl = document.getElementById('outboundShipmentGrid');
	const Grid = tui.Grid;

	if (outboundShipmentGrid) outboundShipmentGrid.destroy();

	gridLangSet(Grid);
	
	outboundShipmentGrid = new Grid({
	   el: gridEl,
	   bodyHeight: 160,
	   rowHeaders: ['rowNum'],
	   pageOptions: { useClient: true, perPage: 5 },
	   columnOptions: {
	   	resizable: true
	   },
	   columns: [
		{ header: '출하지시서', name: 'shipmentId', minWidth: 140 },
		{ header: '거래처',     name: 'clientName', minWidth: 120 },
		{ header: '출고예정일', name: 'startDate',  minWidth: 120, align: 'center',
			formatter: ({ value }) => value ? value.replace('T', ' ') : ''
		},
		{
			header: '출고등록',
			name: 'btn',
			width: 100,
			align: 'center',
			formatter: ({ rowKey }) =>
			`<button type="button" class="btn btn btn-outline-info btn-sm" data-row="${rowKey}">출고등록</button>`
		}
		],
		data: shipmentData   // 여기서 /api/shipment/list 결과 사용
	 });
	 
	outboundShipmentGrid.on('click', async (event) => {
		if (event.columnName !== 'btn') return;
		const target = event.nativeEvent.target;
		if (!target || target.tagName !== 'BUTTON') return;
		
		const rowData = outboundShipmentGrid.getRow(event.rowKey);
		if (!rowData) return;
		
		const modalEl = document.getElementById('prdObModal');
		const bsModal = new bootstrap.Modal(modalEl);
		bsModal.show();
		
		await initShipmentModalByRow(rowData);
	});
}

async function initShipmentModalByRow(rowData) {
	const shipmentSelect  = document.querySelector("#shipmentSelect");
	const shipmentIdInput = document.querySelector("#shipmentId");
	const processByName   = document.querySelector("#processByName");
	const processByEmpId  = document.querySelector(".processByEmpId");
	const shopClientName  = document.querySelector("#shopClientName");
	const expectDate      = document.querySelector("#expectDate");
	
	// 초기화
	shipmentSelect.innerHTML = `<option value="">출하지시서를 선택하세요</option>`;
	shipmentIdInput.value = '';
	processByName.value = '';
	processByEmpId.value = '';
	shopClientName.value = '';
	expectDate.value = '';
	shipTbody.innerHTML = '';
	prdOutboundDate = null;
	
	// 혹시 shipmentList가 아직 안 채워져 있으면 한 번만 불러오기
	if (!shipmentList || shipmentList.length === 0) {
	  shipmentList = await loadShipmentList();
	}
	
	 // 셀렉트 옵션 구성
	shipmentList.forEach(el => {
	    const opt = document.createElement("option");
	    opt.value = el.shipmentId;
	    opt.textContent = el.shipmentId;
	    shipmentSelect.appendChild(opt);
	});

	// 그리드에서 선택한 출하지시서 선택
	shipmentSelect.value = rowData.shipmentId;

	// 실제 데이터 찾기
	const shipOrder = shipmentList.find(el => String(el.shipmentId) === String(rowData.shipmentId));
	if (!shipOrder) {
		alert("출하지시서 데이터를 찾을 수 없습니다.");
		return;
	}

	shipmentIdInput.value = shipOrder.shipmentId;
	processByName.value   = shipOrder.createdName;
	processByEmpId.value  = shipOrder.createdId;
	shopClientName.value  = shipOrder.clientName;
	expectDate.value      = shipOrder.startDate?.split("T")[0] || "";

	prdOutboundDate = shipOrder.startDate;

	// change 이벤트 강제 발생
	const changeEvent = new Event("change");
	shipmentSelect.dispatchEvent(changeEvent);
//	renderProductList(shipOrder.items);
}

// ------------------------------------------------------------------
// 안전재고 그리드 함수

// 안전재고미달데이터 필터
function getLowStockRows() {
    return inventorySafetyStockInfo.filter(stock => {
        // 예상재고량 = ivQty - planOutQty (지금 구조 기준)
        const expectIvQty = stock.ivQty - stock.planOutQty;
        return stock.safetyStockQty > expectIvQty + stock.expectIbAmount;
    });
}

function getNeedOrderStocks() {
	return ivOrderCheckData.filter(stock => {
		// 출고 예정 수량 : 생산계획수량 - 작업지시(출고완)수량
		const EXPECT_OBPLAN_QTY = stock.productPlanQty - stock.outboundPlanQty;
		// 예상 재고 수량(재고수량 - 예상출고수량) + 예상입고수량
		const EXPECT_IV_QTY = stock.expectIvQty + stock.expectIbQty;
		// 최종 재고 수량 : 예상재고수량 + 예상입고수량 - (생산계획수량 - 작업지시수량)
		const FINAL_IV_QTY = EXPECT_IV_QTY - EXPECT_OBPLAN_QTY;
		const SAFETY_QTY = stock.safetyQty

		return FINAL_IV_QTY < SAFETY_QTY; 
	})
}

// 안전재고 미달목록 그리드 그리기
async function renderNeedOrderStockGrid() {
	// 안전재고 미달 데이터 
//    const lowStocks = getLowStockRows();
	// 발주필요 재고 데이터
	const needOrderStocks = getNeedOrderStocks();
	
//	console.log(needOrderStocks,"!!!!!!!!!!!!!!!!!!@#")
	
	
	// 그리드 보이는 카드
    const cardEl = document.getElementById('safetyStockCard');

    if (needOrderStocks.length === 0) {
        // 미달 없으면 카드 숨김
        cardEl.style.display = 'none';
        return;
    }

    // 미달 있으면 카드 표시
    cardEl.style.display = 'block';

    const gridEl = document.getElementById('safetyStockGrid');
	const Grid = tui.Grid;
    // 기존 그리드가 있으면 destroy 후 다시 생성
    if (safetyStockGrid) {
        safetyStockGrid.destroy();
    }
	
	gridLangSet(Grid)
	
	// 안전재고 그리드
    safetyStockGrid = new Grid({
        el: gridEl,
//        scrollX: false,
//        scrollY: true,
        bodyHeight: 220,
        rowHeaders: ['rowNum'],
		pageOptions: {
		    useClient: true, 
		    perPage: 5 
		},
		columnOptions: {
			resizable: true
		},
        columns: [
            { header: '품목명',   name: 'itemName', minWidth: 160 },
//            { header: '품목코드', name: 'itemId', width: 110, align: 'center' },
            { header: '재고',     name: 'expectIvQty', minWidth: 80,  align: 'center' },
            { header: '입고예정',     name: 'expectIbQty', minWidth: 80,  align: 'center' },
            { header: '생산계획',   name: 'productPlanQty', minWidth: 90, align: 'right', 
			  formatter: ({value}) => value.toLocaleString() },
            { header: '생산계획(출고완)', name: 'outboundPlanQty', minWidth: 90, align: 'right', 
			  formatter: ({value}) => value.toLocaleString() },
            { header: '안전재고수량', name: 'safetyQty', minWidth: 90, align: 'right', 
              formatter: ({value}) => value.toLocaleString() },
            { header: '필요 재고', name: '', minWidth: 90, align: 'right', 
			  formatter: ({row}) => {
				// 출고 예정 수량 : 생산계획수량 - 작업지시(출고완)수량
				const EXPECT_OBPLAN_QTY = row.productPlanQty - row.outboundPlanQty;
				// 예상 재고 수량(재고수량 - 예상출고수량) + 예상입고수량
				const EXPECT_IV_QTY = row.expectIvQty + row.expectIbQty;
				// 최종 재고 수량 : 예상재고수량 + 예상입고수량 - (생산계획수량 - 작업지시수량)
				const FINAL_IV_QTY = EXPECT_IV_QTY - EXPECT_OBPLAN_QTY;
				const SAFETY_QTY = row.safetyQty;
				
				return SAFETY_QTY - FINAL_IV_QTY;
				}
		    },
			{ header: '단위',   name: 'itemUnit', minWidth: 50 },
			{ header: '발주',      name: "btn", width: 100, align: "center",
			  formatter: (cellInfo) => "<button type='button' class='btn-detail btn btn-outline-info btn-sm' data-row='${cellInfo.rowKey}' >발주</button>"
			}
        ],
        data: needOrderStocks
    });
	
	// 발주 버튼 동작
	safetyStockGrid	.on("click", async (event) => {
		if(event.columnName == "btn") {
			const target = event.nativeEvent.target;
			if (target && target.tagName === "BUTTON") {
				
				const rowData = safetyStockGrid.getRow(event.rowKey);
				
//				console.log(rowData,"###################");

				//모달 열기
				const modalEl = document.getElementById("modalCenter");
				const bsModal = new bootstrap.Modal(modalEl);
				bsModal.show();
				
				// 모달세팅
				await initPurchaseModalByRow(rowData);
			}
		}
	});
}
// 발주 모달 초기화, 선택한 안전재고 발주
async function initPurchaseModalByRow(rowData) {
    // 모달 초기화
    clientInput.value = "";             
    hiddenId.value = "";               
    document.querySelector("#managerName").value = "";
    itemSelect.innerHTML = "";
    itemSelect.disabled = true;
    orderTableBody.innerHTML = "";
    dueDateInput.value = "";

    // 공급업체 전체 데이터 가져오기
    const data = await supplierList();
//	console.log("!@#!@#!@#!@#", data);
    if (!data) return;

    // 이 품목을 공급하는 거래처 찾기
    // supplierItemList 안에 materialId가 rowData.itemId인 곳
    const targetMaterialId = rowData.itemId;

	let supplier = null;
	let targetItem = null;
	
	// 거래처, 거래처품목에서 발주선택한 안전재고 찾기
	for (const client of data) {
	    const found = client.supplierItemList?.find(
	        item => String(item.materialId) === String(targetMaterialId)
	    );
	    if (found) {
//			console.log("found", found);
	        supplier = client;
	        targetItem = found;
	        break;
	    }
	}
	
	// 품목의 거래처가 없을때
	if (!supplier || !targetItem) {
	    alert("해당 품목을 공급하는 거래처가 없습니다.");
	    return;
	}
	
	// 거래처 선택 
	selectClient(supplier, data);

	// itemId로 옵션 선택
	const targetItemId = targetItem.itemId;
	
	// 거래처선택후 채워진 공급처 옵션에서 해당품목 찾기
	const optionToSelect = Array.from(itemSelect.options).find(
		opt => Number(opt.value) === Number(targetItemId)
	);
//	console.log("optionToSelect : ", optionToSelect);
	if (!optionToSelect) {
		alert("선택된 거래처에 이 품목이 없습니다.");
		return;
	}

	itemSelect.value = optionToSelect.value;

	// change 이벤트 실행
	const changeEvent = new Event("change");
	itemSelect.dispatchEvent(changeEvent);
	
	// 발주필요 수량주문수량에 입력
	// 발주필요수량
	const inventoryNeedOrderQty = Math.max(0, 
//	const needOrderQty = Math.max(0, 
		rowData.safetyQty - 
		(rowData.expectIvQty + rowData.expectIbQty - (rowData.productPlanQty - rowData.outboundPlanQty))
	);
	
	// 발주수량으로 변환
	needOrderQty = Math.ceil(convertFromBaseUnit(inventoryNeedOrderQty, targetItem.unit));
//	console.log(needOrderQty);
	
	// orderTableBody(purchase_regist.js에선언되어있음)의 마지막추가된 row정보
	const lastRow = orderTableBody.lastElementChild;
	if (!lastRow) return;
	// 발주수량 입력 인풋엘리먼트
	const qtyInput = lastRow.querySelector("input[name=orderAmount]");
	if (!qtyInput) return;
	// 최소수량, 단위, 단가 정보(input에 설정되어있는값가져오기)
	const minOrder = parseInt(qtyInput.dataset.min, 10);
	const unit     = parseInt(qtyInput.dataset.orderUnit, 10);
	const unitPrice    = parseInt(qtyInput.dataset.price, 10);
	
	// 발주필요수량을 최소주문수량, 주문단위규칙에 맞게 변형하기위해 저장
	let qty = needOrderQty;
	// 최소주문수량 체크
	if(qty < minOrder) qty = minOrder;
	// 주문단위 설정 올림(발주필요수량 / 주문단위) x 주문단위
	if(qty % unit !== 0) qty = Math.ceil(qty / unit) * unit;
	// 입력
	qty = Math.round(qty);
	qtyInput.value = qty;
	
	// 금액 계산 후 입력
	const supplyPrice = qty * unitPrice;
	const tax = Math.round(supplyPrice * 0.1);
	const total = supplyPrice + tax;
	
	lastRow.querySelector(".supplyPrice").textContent = supplyPrice.toLocaleString();
	lastRow.querySelector(".taxPrice").textContent    = tax.toLocaleString();
	lastRow.querySelector(".totalPrice").textContent  = total.toLocaleString();
	
	
}

// ----------------------------------------------------------------------
// 유통기한 목록 리스트
// 임박, 폐기 재고 데이터필터함수
function getExpireAndDisposalRows() {
    return inventoryInfo.filter(iv =>  iv.ivStatus === 'DISPOSAL_WAIT' || iv.ivStatus === 'EXPIRED');
}

async function renderExpireDisposalGrid() {
    // 임박 + 폐기 같이 필터
    const rows = getExpireAndDisposalRows();

    const cardEl = document.getElementById('expireDisposalCard');
    const gridEl = document.getElementById('expireDisposalGrid');
	
	const Grid = tui.Grid;
	gridLangSet(Grid);
	
    if (rows.length === 0) {
        cardEl.style.display = 'none';
        return;
    }

    cardEl.style.display = 'block';

    if (expireDisposalGrid) {
        expireDisposalGrid.destroy();
    }

    expireDisposalGrid = new Grid({
        el: gridEl,
        scrollX: false,
        scrollY: true,
        bodyHeight: 220,
        rowHeaders: ['rowNum'],
		pageOptions: {
		    useClient: true,
		    perPage: 5
		},
		columnOptions: {
			resizable: true
		},
        columns: [
//            { header: '품목코드', name: 'itemId', width: 110, align: 'center' },
            { header: '품목명',   name: 'prodName', minWidth: 160 },
            { header: '유형',     name: 'itemType', width: 80,  align: 'center',
			  formatter: ({ value }) => {
				switch (value) {
					case 'RAW':  return '원자재';
					case 'SUB':  return '부자재';
					case 'FG':   return '상품';
					case 'PKG':  return '포장재';
					default:     return value ?? '';
				}
			  }
			},
            { header: 'LOT번호',  name: 'lotNo', minWidth: 220 },
            { header: '재고수량', name: 'ivAmount', width: 90, align: 'right',
              formatter: ({ value }) => value.toLocaleString() },
            { header: '출고예정', name: 'expectObAmount', width: 90, align: 'right',
              formatter: ({ value }) => (value ?? 0).toLocaleString() },
            { header: '유통기한', name: 'expirationDate', minWidth: 140, align: 'center',
			  formatter: ({ value }) => {
				if (!value) return '';
				    const date = new Date(value);
				    const year = date.getFullYear();
				    const month = String(date.getMonth() + 1).padStart(2, '0');
				    const day = String(date.getDate()).padStart(2, '0');
				    
				    return `${year}-${month}-${day}`;
			  }
			},
            { header: '상태',     name: 'ivStatus', width: 90, align: 'center',
			  formatter: ({ value }) => {
				switch(value) {
					case 'NORMAL'          : return '정상';
					case 'EXPIRED'         : return '만료';
					case 'DISPOSAL_WAIT': return '임박';
					default:     return value ?? '';
			    }
			  }
			},
			{
				header: '상세',      name: "btn", width: 100, align: "center",
				formatter: (cellInfo) => "<button type='button' class='btn-detail btn btn-outline-info btn-sm' data-row='${cellInfo.rowKey}' >상세</button>"
			}
        ],
        data: rows
    });
	
	expireDisposalGrid.on("click", (event) => {
		if(event.columnName == "btn") {
			const target = event.nativeEvent.target;
			if (target && target.tagName === "BUTTON") {
				
				const rowData = expireDisposalGrid.getRow(event.rowKey);
//				console.log(rowData);
				// 같은 LOT, 같은 상품(itemId)만 필터
				const sameLotList = inventoryInfo.filter(item =>
					item.lotNo === rowData.lotNo &&
					item.itemId === rowData.itemId &&
					// 현재 행은 제외
					item.ivId !== rowData.ivId
				);

				openDetailModal(rowData, sameLotList);
			}
		}
	});

}


// ----------------------------------------------------------------------
// 데이터 정보 가져오기

// 재고정보 가져오기
async function fetchNotNormalInventoryData() {
	const response = 
		await fetch('/api/inventories/expiration', {
			method: 'POST',
			headers: {
				[csrfHeader]: csrfToken,
				'Content-Type': 'application/json'
			}
		});
//	console.log(response);
	if (!response.ok) {
		throw new Error('재고데이터를 가져올 수 없습니다.')
	}
	return await response.json();
}

// 안전재고/재고 비교 정보 데이터
async function fetchInventorySafetyStockData() {
	const response = 
		await fetch('/api/inventories/inventorySafetyStockCheckInfo', {
			method: 'GET',
			headers: {
				[csrfHeader]: csrfToken,
				'Content-Type': 'application/json'
			}
		});
//	console.log(response);
	if (!response.ok) {
		throw new Error('재고와 안전재고 비교 데이터를 가져올 수 없습니다.')
	}
	return await response.json();
}

// 오늘 입고정보 조회함수
async function fetchTodayInboundData() {
	
	const startDate = today.toISOString().slice(0, 10);
	const endDate = today.toISOString().slice(0, 10);
	 
	const MATERIAL_INBOUND_LIST = 
		`/inventory/inbound/materialList/data` +
		`?startDate=${startDate}` +
		`&endDate=${endDate}` + 
		`&searchType=` +
		`&keyword=`
			
	const response = 
		await fetch(MATERIAL_INBOUND_LIST, {
			method: 'GET',
			headers: {
				[csrfHeader]: csrfToken,
				'Content-Type': 'application/json'
			}
		});

	if (!response.ok) {
		throw new Error('입고 정보를 조회할 수 없습니다.')
	}
	
	return await response.json();
}

// 오늘 출고정보 조회함수
async function fetchTodayOutboundData() {
	
	const startDate = today.toISOString().slice(0, 10);
	const endDate = today.toISOString().slice(0, 10);
//	 console.log(startDate, endDate);
	const MATERIAL_OUTBOUND_LIST = 
		`/inventory/outbound/list/data` +
		`?startDate=${startDate}` +
		`&endDate=${endDate}` +
		`&keyword=`;
			
	const response = 
		await fetch(MATERIAL_OUTBOUND_LIST, {
			method: 'GET',
			headers: {
				[csrfHeader]: csrfToken,
				'Content-Type': 'application/json'
			}
		});

	if (!response.ok) {
		throw new Error('출고 정보를 조회할 수 없습니다.')
	}
	
	return await response.json();
}

// 재고내역 조회
async function fetchIvHistoryData() {
	const response = await fetch('/api/inventories/ivHistoryGroup', {
		method: 'GET',
		headers: {
			[csrfHeader]: csrfToken,
			'Content-Type': 'application/json'
		}
	});
	
	if (!response.ok) {
		throw new Error('입출고 추이 데이터를 조회할 수 없습니다.')
	}
	
	return await response.json();
}

// 작업지시서 리스트 가져오기
// 작업지시 정보 가져오기
async function fetchOrderListData() {
		const response = await fetch("/api/inventories/orderData", {
			method: "GET",
			headers: {
				[csrfHeader]: csrfToken,
				'Content-Type': 'application/json'
			}
		});
		
		if (!response.ok) {
			throw new Error("작업지시서 데이터 로드 실패!");
		}
		
		return await response.json();
}


// 발주 필요 수량 체크
async function fetchIvOrderCheckData() {
	const response = await fetch("/api/inventories/inventoryOrderCheck", {
		method: "GET",
		headers: {
			[csrfHeader]: csrfToken,
			'Content-Type': 'application/json'
		}
	});
	
	if(!response.ok) {
		throw new Error("발주체크 데이터 로드 실패")
	}
	
	return await response.json();
}

// 

// --------------------------------------------------------------
// 그리드 랭기지 설정
function gridLangSet(grid) {
	// 1. 그리드 한글 언어셋 설정 (필터 및 각종 텍스트 한글화)
	grid.setLanguage('ko', {
	    display: {
	        noData: '데이터가 없습니다.',
	        loadingData: '데이터를 불러오는 중입니다.',
	        resizeHandleGuide: '마우스 드래그를 통해 너비를 조정할 수 있습니다.',
	    },
	    net: {
	        confirmCreate: '생성하시겠습니까?',
	        confirmUpdate: '수정하시겠습니까?',
	        confirmDelete: '삭제하시겠습니까?',
	        confirmModify: '저장하시겠습니까?',
	        noDataToCreate: '생성할 데이터가 없습니다.',
	        noDataToUpdate: '수정할 데이터가 없습니다.',
	        noDataToDelete: '삭제할 데이터가 없습니다.',
	        noDataToModify: '수정할 데이터가 없습니다.',
	        failResponse: '데이터 요청 중에 에러가 발생하였습니다.'
	    },
	    filter: {
	        // 문자열 필터 옵션
	        contains: '포함',
	        eq: '일치',
	        ne: '불일치',
	        start: '시작 문자',
	        end: '끝 문자',
	        
	        // 날짜/숫자 필터 옵션
	        after: '이후',
	        afterEq: '이후 (포함)',
	        before: '이전',
	        beforeEq: '이전 (포함)',

	        // 버튼 및 기타
	        apply: '적용',
	        clear: '초기화',
	        selectAll: '전체 선택'
	    }
	});
}







