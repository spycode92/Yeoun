// 전역 변수 및 설정
let bsRackModal = null; // Rack 상세 모달 인스턴스
let bsStockModal = null; // 재고 상세 모달 인스턴스
let bsMoveModal = null;  // 재고 이동 모달 인스턴스

// 디자인 상수
const CONFIG = {
    // Konva 카드 스타일
    cardBg: "#ffffff",
    cardStroke: "#dee2e6", // 부트스트랩 border color와 유사
    cardShadow: { color: 'black', blur: 10, offset: { x: 2, y: 4 }, opacity: 0.05 },
    
    // Zone 헤더 스타일
    headerBg: "#343a40", // 부트스트랩 bg-dark
    headerText: "#ffffff",
    
    // Rack 버튼 스타일
    rackBtnFill: "#f8f9fa", // 부트스트랩 bg-light
    rackBtnStroke: "#ced4da",
    rackBtnHover: "#e9ecef",
    rackBtnText: "#495057",
    
    // 레이아웃 설정
    padding: 20,
    headerHeight: 40,
    rackBtnSize: 70, // 버튼 크기
    rackGap: 15      // 버튼 사이 간격
};

// 초기화 (DOMContentLoaded)
document.addEventListener("DOMContentLoaded", async () => {
    // 부트스트랩 모달 초기화
    const rackEl = document.getElementById('rackDetailModal');
    if (rackEl) bsRackModal = new bootstrap.Modal(rackEl);

    const stockEl = document.getElementById('stockModal');
    if (stockEl) bsStockModal = new bootstrap.Modal(stockEl);

    const moveEl = document.getElementById('moveStockModal');
    if (moveEl) bsMoveModal = new bootstrap.Modal(moveEl);

    // 대시보드(Konva) 실행
    await initDashboard();
});

// Konva 대시보드 로직
let locationInfo = [];

async function initDashboard() {
    const containerBlock = document.getElementById('container');
    const stageWidth = containerBlock.offsetWidth || 200;
    const stageHeight = 200; // 대시보드 높이 설정

    const stage = new Konva.Stage({
        container: "container",
        width: stageWidth,
        height: stageHeight,
        draggable: true // 화면이 좁을 때 드래그 허용
    });

    const layer = new Konva.Layer();
    stage.add(layer);

    // 데이터 가져오기
    const rawData = await getLocationData();
	locationInfo = rawData;

    if (!rawData || rawData.length === 0) {
        // 데이터 없을 때 안내 문구
        const noDataText = new Konva.Text({
            x: 50, y: 50, text: "표시할 창고 데이터가 없습니다.", fontSize: 20, fill: "#adb5bd"
        });
        layer.add(noDataText);
        return;
    }

    // 데이터 그룹화 (Zone -> Rack)
    const zones = {};
    rawData.forEach(d => {
        if (!zones[d.zone]) zones[d.zone] = [];
        
        const colNum = parseInt(d.rackCol, 10);
        const rowNum = d.rackRow.toUpperCase().charCodeAt(0) - 64; // A=1, B=2
        
        zones[d.zone].push({ ...d, colIdx: colNum, rowIdx: rowNum });
    });

    // 카드 배치 시작
    let currentX = 20;
    const startY = 30;
    const cardGap = 30; // 카드 사이 간격

    Object.keys(zones).sort().forEach(zoneName => {
        const zoneData = zones[zoneName];
        
        // Zone 카드 그리기 함수 호출
        const result = drawZoneCard(currentX, startY, zoneName, zoneData);
        
        layer.add(result.group);
        currentX += result.width + cardGap; // 다음 카드 위치 계산
    });

    layer.draw();
}

// Zone 카드 그리기 (Rack 버튼 포함)
function drawZoneCard(startX, startY, zoneName, zoneData) {
    const group = new Konva.Group({ x: startX, y: startY });

    // Rack 별로 데이터 묶기
    const racks = {};
    zoneData.forEach(d => {
        if (!racks[d.rack]) racks[d.rack] = [];
        racks[d.rack].push(d);
    });
    const rackKeys = Object.keys(racks).sort();

    // 카드 크기 계산
    const contentWidth = (rackKeys.length * CONFIG.rackBtnSize) + ((rackKeys.length - 1) * CONFIG.rackGap);
    // 최소 너비 160px 보장
    const cardWidth = Math.max(contentWidth + (CONFIG.padding * 2), 160); 
    const cardHeight = CONFIG.headerHeight + CONFIG.padding + CONFIG.rackBtnSize + CONFIG.padding;

    // A. 배경 박스
    const cardBg = new Konva.Rect({
        width: cardWidth, height: cardHeight,
        fill: CONFIG.cardBg, stroke: CONFIG.cardStroke, strokeWidth: 1,
        cornerRadius: 6,
        shadowColor: CONFIG.cardShadow.color, shadowBlur: CONFIG.cardShadow.blur,
        shadowOffset: CONFIG.cardShadow.offset, shadowOpacity: CONFIG.cardShadow.opacity
    });

    // B. 헤더
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

    // C. Rack 버튼 배치
    // 카드 중앙 정렬을 위한 오프셋 계산
    const startBtnX = (cardWidth - contentWidth) / 2;

    rackKeys.forEach((rackName, index) => {
        const btnX = startBtnX + (index * (CONFIG.rackBtnSize + CONFIG.rackGap));
        const btnY = CONFIG.headerHeight + CONFIG.padding;

        const btnGroup = new Konva.Group({ x: btnX, y: btnY });

        // 버튼 모양
        const rect = new Konva.Rect({
            width: CONFIG.rackBtnSize, height: CONFIG.rackBtnSize,
            fill: CONFIG.rackBtnFill, stroke: CONFIG.rackBtnStroke, strokeWidth: 1,
            cornerRadius: 4,
            shadowColor: 'black', shadowBlur: 2, shadowOpacity: 0.05
        });

        // Rack 이름 텍스트
        const text = new Konva.Text({
            x: 0, y: 25, width: CONFIG.rackBtnSize,
            text: `Rack\n${rackName}`,
            fontSize: 13, fontStyle: 'bold', fill: CONFIG.rackBtnText, align: 'center',
            lineHeight: 1.2
        });

        // 셀 개수 뱃지 (우측 상단)
        const countText = new Konva.Text({
            x: CONFIG.rackBtnSize - 25, y: 5,
            text: `${racks[rackName].length}`,
            fontSize: 10, fill: '#adb5bd'
        });

        btnGroup.add(rect);
        btnGroup.add(text);
        btnGroup.add(countText);

        // 이벤트 리스너
        btnGroup.on('mouseenter', () => {
            document.body.style.cursor = 'pointer';
            rect.fill(CONFIG.rackBtnHover);
            rect.stroke('#0d6efd'); // Primary color border
        });
        btnGroup.on('mouseleave', () => {
            document.body.style.cursor = 'default';
            rect.fill(CONFIG.rackBtnFill);
            rect.stroke(CONFIG.rackBtnStroke);
        });
        
        // ★ 클릭 시: Rack 상세 모달(부트스트랩) 열기
        btnGroup.on('click', () => {
            openRackDetailModal(zoneName, rackName, racks[rackName]);
        });

        group.add(btnGroup);
    });

    return { group, width: cardWidth };
}

// Rack 상세 (그리드) 모달 열기
function openRackDetailModal(zone, rack, cells) {
    const badge = document.getElementById('rackInfoBadge'); // 추가된 배지
    const container = document.getElementById('rackGridContainer');
    
    badge.innerText = `ZONE ${zone} > Rack ${rack}`;
    
    container.innerHTML = '';
    container.className = ''; // 기존 클래스 초기화

    const maxRow = Math.max(...cells.map(c => c.rowIdx));
    const maxCol = Math.max(...cells.map(c => c.colIdx));
    
    // CSS Grid 적용
    Object.assign(container.style, {
        display: 'grid',
        gridTemplateColumns: `repeat(${maxCol}, 45px)`,
        gridTemplateRows: `repeat(${maxRow}, 45px)`,
        gap: '8px',
        justifyContent: 'center',
        padding: '10px'
    });

    cells.forEach(cell => {
        const div = document.createElement('div');
        div.className = 'grid-cell';
        
        // 재고 수량 확인 로직 (예: cell.qty > 0)
        div.classList.add('has-stock'); 
        
        div.innerText = `${cell.rackRow}-${cell.rackCol}`;
        
        div.style.gridRow = (maxRow - cell.rowIdx) + 1;
        div.style.gridColumn = cell.colIdx;

        div.onclick = () => openStockModal(cell);
        container.appendChild(div);
    });

    bsRackModal.show();
}

let stockList;

// 2. 재고 상세 (리스트) 모달 열기
async function openStockModal(cellData) {
    // input 태그에 값 넣기 (기존 span 방식에서 변경)
	const locationId = cellData.locationId;
    document.getElementById('modalLocationId').value = locationId;
    document.getElementById('modalZoneRack').value = `${cellData.zone}구역 / Rack ${cellData.rack} / ${cellData.rackRow}-${cellData.rackCol}`;

    const tbody = document.getElementById('modalTableBody');
    
    bsStockModal.show();

    try {        
		stockList = await getLocationInventory(locationId);
		
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
	        // 1. HTML 생성 (data-index만 심어둠)
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
	
	        // 2. 이벤트 리스너 등록
	        const buttons = tbody.querySelectorAll('.move-stock-btn');
	        buttons.forEach(btn => {
	            btn.addEventListener('click', function() {
	                const index = this.getAttribute('data-index');
	                const item = stockList[index]; // 클릭한 행의 정확한 데이터 가져오기
	                
	                // 클릭하는 순간에 전역 변수 업데이트
	                currentIvid = item.ivId;
	                currentIvQty = item.ivAmount;
	                expectOutboundQty = item.expectObAmount;
	                currentLoc = item.locationId; 
	                canUseQty = item.ivAmount - item.expectObAmount;
	                
	                console.log("선택된 데이터로 변수 업데이트 완료:", canUseQty);
	                
	                // 팀원 함수 호출 (필요하다면 객체도 넘김)
	                openMoveModal(item);
	            });
	        });
	    }
    } catch (e) {
		console.log(e)
        tbody.innerHTML = '<tr><td colspan="5" class="text-center text-danger">오류 발생</td></tr>';
    }
}

// 창고 데이터 가져오기
async function getLocationData() {
    try {
        const response = await fetch('/api/inventories/locations');
		
        if (!response.ok) {
			throw new Error("서버 통신 실패");
		}
		
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
		
		if (!response.ok) {
			throw new Error("서버 통신 실패");
		}
		
		return await response.json();
	} catch (error) {
		console.error(error);
		return [];
	}
}
