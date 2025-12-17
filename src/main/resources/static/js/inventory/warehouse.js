// 전역 변수 및 설정
let bsRackModal = null; // Rack 상세 모달 인스턴스
let bsStockModal = null; // 재고 상세 모달 인스턴스
let bsMoveModal = null;  // 재고 이동 모달 인스턴스
let layer = null;        // Konva Layer
let stockList = [];      // 재고 목록 데이터

// 디자인 상수
const CONFIG = {
    cardBg: "#ffffff",
    cardStroke: "#dee2e6",
    cardShadow: { color: 'black', blur: 10, offset: { x: 2, y: 4 }, opacity: 0.05 },
	
	// zone 헤더 스타일
    headerBg: "#343a40",
    headerText: "#ffffff",
	
	// rack 버튼 스타일
    rackBtnFill: "#f8f9fa",
    rackBtnStroke: "#ced4da",
    rackBtnHover: "#e9ecef",
    rackBtnText: "#495057",
	
	// 레이아웃 설정
    padding: 20,
    headerHeight: 40,
    rackBtnSize: 70,
    rackGap: 15
};

// 초기화
document.addEventListener("DOMContentLoaded", async () => {
    const rackEl = document.getElementById('rackDetailModal');
	
    if (rackEl) {
		bsRackModal = new bootstrap.Modal(rackEl);
	}

    const stockEl = document.getElementById('stockModal');
	
    if (stockEl) {
		bsStockModal = new bootstrap.Modal(stockEl);
	}

    const moveEl = document.getElementById('moveStockModal');
	
    if (moveEl) {
		bsMoveModal = new bootstrap.Modal(moveEl);
	}

    await initDashboard();
});

let locationInfo = [];

// Konva 대시보드 로직
// 대시보드 초기화 및 렌더링 함수
async function initDashboard() {
	// 컨테이너 크기 측정
    const containerBlock = document.getElementById('container');
    const stageWidth = containerBlock.offsetWidth || 200;
    const stageHeight = 200;
   
	// Konva Stage 생성(캔버스)
    const stage = new Konva.Stage({
        container: "container",
        width: stageWidth,
        height: stageHeight,
        draggable: true // 화면 드래그 이동 가능
    });

	// layer 생성 및 추가
    layer = new Konva.Layer();
    stage.add(layer);

	// 창고 위치 데이터 가져오익
    const rawData = await getLocationData();
	locationInfo = rawData;

	// 데이터가 없을 경우 처리
    if (!rawData || rawData.length === 0) {
        const noDataText = new Konva.Text({
            x: 50, y: 50, text: "표시할 창고 데이터가 없습니다.", fontSize: 20, fill: "#adb5bd"
        });
		
        layer.add(noDataText);
		
        return;
    }

	// 데이터 구조화(zone별 그룹핑)
    const zones = {};
    rawData.forEach(d => {
        if (!zones[d.zone]) zones[d.zone] = [];
		
		// 행/열 인덱스 계산(A -> 1, B -> 2)
        const colNum = parseInt(d.rackCol, 10);
        const rowNum = d.rackRow.toUpperCase().charCodeAt(0) - 64; 
		
        zones[d.zone].push({ ...d, colIdx: colNum, rowIdx: rowNum });
    });

	// zone 카드 배치 시작점
    let currentX = 20;
    const startY = 30;
    const cardGap = 30;

	// 각 zone별로 카드 생성 및 레이어에 추가
    Object.keys(zones).sort().forEach(zoneName => {
        const zoneData = zones[zoneName];
		// zone 카드 그리기(rack 버튼 포함)
        const result = drawZoneCard(currentX, startY, zoneName, zoneData);
		
        layer.add(result.group);
		
        currentX += result.width + cardGap; // 다음 카드 위치 계산
    });

    layer.draw();
	
	// 초기 로딩 시 화면 크기 설정
	fitStageIntoParentContainer(stage, layer);
	
	// 화면 크기 감지해서 실시간으로 크기 변함
	window.addEventListener('resize', () => fitStageIntoParentContainer(stage, layer));
}

function fitStageIntoParentContainer(stage, layer) {
    const container = document.getElementById('container');
	
    if (!container) return;

    // 현재 컨테이너(화면) 너비
    const containerWidth = container.offsetWidth;

    // 실제 그림(콘텐츠)의 전체 너비 측정
    // getClientRect: 현재 레이어에 그려진 모든 도형을 감싸는 사각형
    const contentRect = layer.getClientRect({ relativeTo: layer });
    const contentWidth = contentRect.width + 40; // 여백 40px 추가

    // 배율(Scale) 계산
    // 화면이 그림보다 작으면 축소, 크면 1배율(원본)
    let scale = containerWidth / contentWidth;
    if (scale > 1) scale = 1; // 너무 커지는 것 방지 (최대 1배)

    // Stage 크기 및 배율 적용
    stage.width(containerWidth);
    // 높이도 배율에 맞춰 줄여줌 (비율 유지)
    stage.height(200 * scale); 
    
    stage.scale({ x: scale, y: scale });
    stage.batchDraw(); // 다시 그리기
}

// 개별 zone 카드와 내부 rack 버튼을 그리는 함수
function drawZoneCard(startX, startY, zoneName, zoneData) {
    const group = new Konva.Group({ 
		x: startX, 
		y: startY 
	});
	
	// 해당 zone 내부의 rack 데이터 그룹핑
    const racks = {};
	
    zoneData.forEach(d => {
        if (!racks[d.rack]) {
			racks[d.rack] = [];
		}
		
        racks[d.rack].push(d);
    });
	
    const rackKeys = Object.keys(racks).sort();

	// 카드 크기 계산
    const contentWidth = (rackKeys.length * CONFIG.rackBtnSize) + ((rackKeys.length - 1) * CONFIG.rackGap);
    const cardWidth = Math.max(contentWidth + (CONFIG.padding * 2), 160); 
    const cardHeight = CONFIG.headerHeight + CONFIG.padding + CONFIG.rackBtnSize + CONFIG.padding;
	
	// zone 삭제 버튼
//	const delBtnGroup = new Konva.Group({
//		x: cardWidth - 30,
//		y: 5
//	});
	
	// 버튼 배경
//	const delBg = new Konva.Rect({
//	    width: 25, height: 25, fill: 'transparent' 
//	});
//	
	// 삭제 텍스트(X)
//	const delText = new Konva.Text({
//	    text: '✕', fontSize: 18, fill: '#adb5bd', // 평소엔 연한 회색
//	    align: 'center', verticalAlign: 'middle', padding: 3
//	});
	
//	delBtnGroup.add(delBg);
//	delBtnGroup.add(delText);
	
	// 호버 효과
//	delBtnGroup.on('mouseenter', () => {
//	    document.body.style.cursor = 'pointer';
//	    delText.fill('#ff6b6b'); // 빨간색 강조
//	});
//	
//	delBtnGroup.on('mouseleave', () => {
//	    document.body.style.cursor = 'default';
//	    delText.fill('#adb5bd');
//	});
//	
	// 구역 삭제 시도
//	delBtnGroup.on('click', (e) => {
//	    e.cancelBubble = true; // 버블링 방지
//	    deleteZone(zoneName); // 삭제 함수 호출
//	});

	// 카드 배경
    const cardBg = new Konva.Rect({
        width: cardWidth, height: cardHeight,
        fill: CONFIG.cardBg, stroke: CONFIG.cardStroke, strokeWidth: 1,
        cornerRadius: 6,
        shadowColor: CONFIG.cardShadow.color, shadowBlur: CONFIG.cardShadow.blur,
        shadowOffset: CONFIG.cardShadow.offset, shadowOpacity: CONFIG.cardShadow.opacity
    });

	// 카드 헤더(zone 이름)
    const headerBg = new Konva.Rect({
        width: cardWidth, height: CONFIG.headerHeight,
        fill: CONFIG.headerBg, cornerRadius: [6, 6, 0, 0]
    });
	
	
    const headerText = new Konva.Text({
        x: 0, y: 12, width: cardWidth,
        text: `ZONE ${zoneName}`,
        fontSize: 16, fontStyle: 'bold', fill: CONFIG.headerText, align: 'center'
    });

    group.add(cardBg);
    group.add(headerBg);
    group.add(headerText);
//	group.add(delBtnGroup); // 버튼 추가
	
	// rack 버튼 배치
    const startBtnX = (cardWidth - contentWidth) / 2;

    rackKeys.forEach((rackName, index) => {
        const btnX = startBtnX + (index * (CONFIG.rackBtnSize + CONFIG.rackGap));
        const btnY = CONFIG.headerHeight + CONFIG.padding;
        const btnGroup = new Konva.Group({ x: btnX, y: btnY });
		
//		const rackDelBtn = new Konva.Text({
//			x: CONFIG.rackBtnSize - 15, y: 2,
//			text: '×', fontSize: 14, fill: '#fa5252',
//			visible: false
//		});
		
		// 버튼 배경
        const rect = new Konva.Rect({
            width: CONFIG.rackBtnSize, height: CONFIG.rackBtnSize,
            fill: CONFIG.rackBtnFill, stroke: CONFIG.rackBtnStroke, strokeWidth: 1,
            cornerRadius: 4,
            shadowColor: 'black', shadowBlur: 2, shadowOpacity: 0.05
        });

		// rack 이름
        const text = new Konva.Text({
            x: 0, y: 25, width: CONFIG.rackBtnSize,
            text: `Rack\n${rackName}`,
            fontSize: 13, fontStyle: 'bold', fill: CONFIG.rackBtnText, align: 'center',
            lineHeight: 1.2
        });

        btnGroup.add(rect);
        btnGroup.add(text);
//        btnGroup.add(rackDelBtn);

		// 마우스 오버 시 효과
        btnGroup.on('mouseenter', () => {
            document.body.style.cursor = 'pointer';
            rect.fill(CONFIG.rackBtnHover);
            rect.stroke('#0d6efd');
//			rackDelBtn.show();
        });
		
        btnGroup.on('mouseleave', () => {
            document.body.style.cursor = 'default';
            rect.fill(CONFIG.rackBtnFill);
            rect.stroke(CONFIG.rackBtnStroke);
//			rackDelBtn.hide();
        });
        
		// 클릭 시 상세 모달 열기
        btnGroup.on('click', () => {
            openRackDetailModal(zoneName, rackName, racks[rackName]);
        });
		
		// 삭제버튼 클릭 이벤트
//		rackDelBtn.on("click", (e) => {
//			e.cancelBubble = true;
//			deleteRack(zoneName, rackName);
//		});

        group.add(btnGroup);
    });

    return { group, width: cardWidth };
}

// rack 상세 모달 열기
async function openRackDetailModal(zone, rack, cells) {
    const badge = document.getElementById('rackInfoBadge');
    const container = document.getElementById('rackGridContainer');
    
	// 모달 헤더 정보 업데이트
    badge.innerText = `ZONE ${zone} > Rack ${rack}`;
    container.innerHTML = '';
    container.className = ''; 

    // 모달 먼저 표시 (로딩 느낌)
    bsRackModal.show();

	// 그리드 크기 계산
    const maxRow = Math.max(...cells.map(c => c.rowIdx));
    const maxCol = Math.max(...cells.map(c => c.colIdx));
    
	// 스타일 적용
    Object.assign(container.style, {
        display: 'grid',
        gridTemplateColumns: `repeat(${maxCol}, 45px)`,
        gridTemplateRows: `repeat(${maxRow}, 45px)`,
        gap: '8px',
        justifyContent: 'center',
        padding: '10px'
    });

    // 빈 셀(Loading 상태) 먼저 그리기
    cells.forEach(cell => {
        const div = document.createElement('div');
        div.className = 'grid-cell';
        div.id = `cell-${cell.locationId}`; 
        
        // 로딩 중임을 표시 (연한 회색 등)
        div.style.backgroundColor = '#f1f3f5'; 
        div.style.color = '#adb5bd';

        div.innerText = `${cell.rackRow}-${cell.rackCol}`;
		
		// grid 위치 지정 (Y축은 아래에서 위로 쌓이도록 계산)
//        div.style.gridRow = (maxRow - cell.rowIdx) + 1;
		div.style.gridRow = cell.rowIdx;
        div.style.gridColumn = cell.colIdx;

		// 클릭 시 재고 목록 열기 
        div.onclick = () => openStockModal(cell);
		
        container.appendChild(div);
    });

    // 비동기로 재고 조회 후 색상 입히기
    try {
        // 모든 셀의 재고를 병렬로 조회
        const promises = cells.map(async (cell) => {
            const inventory = await getLocationInventory(cell.locationId);
            return { locationId: cell.locationId, count: inventory.length };
        });

        // 결과 대기
        const results = await Promise.all(promises);

        // 결과에 따라 색상 업데이트
        results.forEach(res => {
            updateHtmlCellColor(res.locationId, res.count);
        });

    } catch (error) {
        console.error("재고 조회 중 오류:", error);
    }
}

// 재고 상세 (리스트) 모달 열기
async function openStockModal(cellData) {
	const locationId = cellData.locationId;
	
	// 모달 내부 정보 입력
    document.getElementById('modalLocationId').value = locationId;
    document.getElementById('stockModalTitle').innerText = `선택위치 : ${cellData.zone}-${cellData.rack}-${cellData.rackRow}-${cellData.rackCol}`;

    const tbody = document.getElementById('modalTableBody');
    
    bsStockModal.show();

    try {      
		// 해당 위치 재고 목록 가져오기  
		stockList = await getLocationInventory(locationId);
		
        //색상 업데이트
		updateHtmlCellColor(locationId, stockList.length);
		
		// 상태값 한글 매핑
		const statusMap = {
			NORMAL: "정상", 
			EXPIRED: "만료", 
			DISPOSAL_WAIT: "임박"
		}
		
		stockList = stockList.map(item => ({
			...item,
			status: statusMap[item.status] || item.status
		}));
		
        if(stockList.length === 0) {
            tbody.innerHTML = '<tr><td colspan="5" class="text-center py-3 text-muted">데이터가 없습니다.</td></tr>';
		} else {
	        tbody.innerHTML = stockList.map((item, index) => `
	            <tr>
	                <td>${item.itemId}</td>
	                <td class="text-start fw-semibold">${item.prodName}</td>
	                <td><span class="badge bg-label-info text-dark">${item.ivAmount}</span></td>
	                <td>${item.status}</td>
	                <td>
	                    <button class="btn btn-sm btn-primary move-stock-btn" data-index="${index}">
	                        이동
	                    </button>
	                </td>
	            </tr>
	        `).join('');
	
			// 이동 버튼 클릭 로직
	        const buttons = tbody.querySelectorAll('.move-stock-btn');
	        buttons.forEach(btn => {
	            btn.addEventListener('click', function() {
	                const index = this.getAttribute('data-index');
	                const item = stockList[index]; 
	                
					// 클릭하는 순간에 전역 변수 업데이트
					currentIvid = item.ivId;
					currentIvQty = item.ivAmount;
					expectOutboundQty = item.expectObAmount;
					currentLoc = item.locationId; 
					canUseQty = item.ivAmount - item.expectObAmount;
	                
					// 재고이동 모달
	                openMoveModal(item);
	            });
	        });
	    }
    } catch (error) {
		console.log(error)
        tbody.innerHTML = '<tr><td colspan="5" class="text-center text-danger">오류 발생</td></tr>';
    }
}

// cell 색상 변경 로직
function updateHtmlCellColor(locationId, count) {
	const targetCell = document.getElementById("cell-" + locationId);
	
	if (targetCell) {
        // 스타일 초기화
        targetCell.style.backgroundColor = '';
        targetCell.classList.remove('has-stock');
        targetCell.style.color = '';
        targetCell.style.border = '';

		let newColor;
		let textColor = 'black';
        let borderColor = '#dee2e6';

		if (count === 0) {
			newColor = "#ffffff"; // 빈 셀 (흰색)
            borderColor = '#e9ecef';
		} 
        else if (count <= 3) {
            // 파란색 계열
			newColor = '#90caf9'; 
            targetCell.classList.add('has-stock');
            borderColor = '#1e88e5';
		} 
        else {
            // 붉은색 계열 (4개 이상)
			newColor = '#ff6b6b';
            targetCell.classList.add('has-stock');
            textColor = 'white'; // 배경이 진하므로 글자는 흰색
            borderColor = '#c92a2a';
		}
		
        // 스타일 적용
        targetCell.style.backgroundColor = newColor;
        targetCell.style.color = textColor;
        targetCell.style.border = `1px solid ${borderColor}`;
	}
}

// 창고 데이터 가져오기
async function getLocationData() {
    try {
        const response = await fetch('/api/inventories/locations');
        if (!response.ok) throw new Error("서버 통신 실패");
        return await response.json();
    } catch (error) {
        console.error(error);
        return [];
    }
}

// 창고 위치에 해당하는 재고 가져오기
async function getLocationInventory(locationId) {
	try {
		const response = await fetch(`/api/inventories/${locationId}`);
		if (!response.ok) throw new Error("서버 통신 실패");
		return await response.json();
	} catch (error) {
		console.error(error);
		return [];
	}
}
