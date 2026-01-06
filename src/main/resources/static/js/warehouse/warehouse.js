const API_BASE = apiUrl(`/api/warehouse`);
const INVENTORY_API = apiUrl('/api/inventories');

let warehouseData = [];
let currentZone = null;
let currentRack = null;
let selectedLocation = null;
let stockList = [];
let locationInfo = [];

// 재고 이동 관련 전역 변수
let currentIvid = null;
let currentIvQty = 0;
let expectOutboundQty = 0;
let currentLoc = null;
let canUseQty = 0;

// Modal 인스턴스
let bsStockModal = null;
let bsMoveModal = null;
let bsLocationStatusModal = null;
let targetZoneForNewRack = null;

// =============================================
// 초기화
document.addEventListener("DOMContentLoaded", () => {
    // Modal 인스턴스 초기화
    const stockModalEl = document.getElementById("stockInfoModal");
    if (stockModalEl) {
        bsStockModal = new bootstrap.Modal(stockModalEl);
    }
    
    const moveModalEl = document.getElementById("moveModal");
    if (moveModalEl) {
        bsMoveModal = new bootstrap.Modal(moveModalEl);
    }
    
    const locationStatusModalEl = document.getElementById("locationStatusModal");
    if (locationStatusModalEl) {
        bsLocationStatusModal = new bootstrap.Modal(locationStatusModalEl);
    }
    
    loadWarehouseData();
    initEventHandlers();
});

// 창고 데이터 불러오기
async function loadWarehouseData() {
    try {
        const response = await fetch(API_BASE + "/locations");
        const locations = await response.json();
		locationInfo = locations;
        warehouseData = transformLocationsToHierarchy(locations);
        renderWarehouseList();
    } catch (error) {
        console.error('창고 데이터 로드 실패:', error);
        alert('창고 데이터를 불러오는데 실패했습니다.');
    }
}

// 데이터 구조 변환
function transformLocationsToHierarchy(locations) {
    const zones = {};
    
    locations.forEach(loc => {
        if (!zones[loc.zone]) {
            zones[loc.zone] = {
                id: loc.zone,
                name: `${loc.zone}구역`,
                racks: {}
            };
        }
        
        if (!zones[loc.zone].racks[loc.rack]) {
            zones[loc.zone].racks[loc.rack] = {
                id: loc.rack,
                name: `Rack ${loc.rack}`,
                zone: loc.zone,
                locations: []
            };
        }
        
        zones[loc.zone].racks[loc.rack].locations.push({
            locationId: loc.locationId,
            row: loc.rackRow,
            col: loc.rackCol,
            useYn: loc.useYn,
            stockCount: loc.stockCount || 0
        });
    });
    
    return Object.values(zones).map(zone => ({
        ...zone,
        racks: Object.values(zone.racks).sort((a, b) => a.id.localeCompare(b.id))
    })).sort((a, b) => a.id.localeCompare(b.id));
} 

// 창고 목록 렌더링
function renderWarehouseList() {
    const accordion = document.getElementById("warehouseAccordion");
    accordion.innerHTML = "";
    
    warehouseData.forEach((zone, zoneIndex) => {
        const collapseId = `collapse-${zone.id}`;
        
        const zoneHtml = `
            <div class="accordion-item">
                <h2 class="accordion-header">
                    <button class="accordion-button ${zoneIndex === 0 ? '' : 'collapsed'}" type="button" 
                            data-bs-toggle="collapse" data-bs-target="#${collapseId}">
                        <strong>Zone ${zone.id}</strong>
                    </button>
                </h2>
                <div id="${collapseId}" class="accordion-collapse collapse ${zoneIndex === 0 ? 'show' : ''}" 
                     data-bs-parent="#warehouseAccordion">
                    <div class="accordion-body p-0">
                        <div>
                            ${renderRackList(zone)}
                        </div>
                    </div>
                </div>
            </div>
        `;
        accordion.insertAdjacentHTML('beforeend', zoneHtml);
    });
}

// rack 렌더링
function renderRackList(zone) {
    let html = '';
    zone.racks.forEach(rack => {
        html += `
            <a href="javascript:void(0)" class="list-group-item  rack-list-item" 
               data-zone-id="${zone.id}" data-rack-id="${rack.id}" onclick="selectRack('${zone.id}', '${rack.id}')">
                <div class="d-flex justify-content-between align-items-center">
                    <div>
                        Rack ${rack.id}
                    </div>
                </div>
            </a>
        `;
    });
    return html;
}

// rack 선택
function selectRack(zoneId, rackId) {
    const zone = warehouseData.find(zone => zone.id === zoneId);
    const rack = zone?.racks.find(rack => rack.id === rackId);
    
    if (!rack) return; // 수정됨
    
    currentZone = zone;
    currentRack = rack;
    
    document.querySelectorAll(".rack-list-item").forEach(item => {
        item.classList.remove("active");
    });
    
    const selectedItem = document.querySelector(`.rack-list-item[data-zone-id="${zoneId}"][data-rack-id="${rackId}"]`);
	
    if (selectedItem) {
		console.log(selectedItem);
        selectedItem.classList.add("active");
    }
    
    document.getElementById("noSelectionPlaceholder").style.display = "none";
    document.getElementById("rackDetailContent").style.display = "block";
    
    updateRackDetail();
}

// rack 상세 정보 업데이트
function updateRackDetail() {
    if (!currentRack || !currentZone) return;
    
    document.getElementById("rackPathDisplay").innerHTML = 
        `Zone ${currentZone.id} > Rack ${currentRack.id}`;
	
    const totalCount = currentRack.locations.length;
    const totalStockCount = currentRack.locations.reduce((sum, loc) => sum + loc.stockCount, 0);
    const disabledCount = currentRack.locations.filter(l => l.useYn === 'N').length;

    document.getElementById("infoTotal").textContent = totalCount;
    document.getElementById("infoStock").textContent = totalStockCount;
    document.getElementById("infoDisabled").textContent = disabledCount;

    renderLocationGrid();
}

// 그리드 렌더링 (CSS 클래스명 수정)
function renderLocationGrid() {
    if (!currentRack) return;
    
    const grid = document.getElementById("locationGrid");
    grid.innerHTML = "";
    
    const rows = [...new Set(currentRack.locations.map(location => location.row))].sort();
    const cols = [...new Set(currentRack.locations.map(location => location.col))].sort();
    
    if (rows.length === 0 || cols.length === 0) {
        grid.innerHTML = '<p class="text-muted">위치 데이터가 없습니다.</p>';
        return;
    }
    
    grid.style.gridTemplateColumns = `repeat(${cols.length}, 1fr)`;
     
    rows.forEach(row => {
        cols.forEach(col => {
            const location = currentRack.locations.find(location => location.row === row && location.col === col);
            
            if (location) {
                const cell = document.createElement("div");
                cell.className = "location-cell"; 
                cell.dataset.locationId = location.locationId;
                
                let cellHtml = `<span>${row}-${col}</span>`;
                
                if (location.stockCount > 0) {
                    cellHtml += `<span class="stock-badge">${location.stockCount}</span>`;
                }
                
                cell.innerHTML = cellHtml;
                
                if (location.useYn === "N") {
                    cell.classList.add("disabled");
                }
                
                // 좌클릭 (재고 조회)
                cell.addEventListener("click", () => {
                    openStockModal(location);
                });
                
                grid.appendChild(cell);
            }
        });
    });
}

// Location 상태 변경 모달
function openLocationStatusModal(location) {
    selectedLocation = location;
    
    document.getElementById("locationStatusTitle").textContent = 
        `Zone ${currentZone.id} - Rack ${currentRack.id} - ${location.row}${location.col}`;
    document.getElementById("locationStatusSelect").value = location.useYn;
    
    bsLocationStatusModal.show();
}

// Location 상태 저장
async function saveLocationStatus() {
    if (!selectedLocation) return;
    
    const newStatus = document.getElementById("locationStatusSelect").value;
    
    if (newStatus === 'N' && selectedLocation.stockCount > 0) {
        alert('재고가 있는 위치는 비활성화할 수 없습니다.');
        return;
    }
    
    try {
        const response = await fetch(`${API_BASE}/locations/${selectedLocation.locationId}`, {
            method: 'PUT',
            headers: { 'Content-Type': 'application/json' },
            body: JSON.stringify({ useYn: newStatus })
        });
        
        if (!response.ok) throw new Error('상태 변경 실패');
        
        selectedLocation.useYn = newStatus;
        alert('창고 상태가 변경되었습니다.');
        
        bsLocationStatusModal.hide();
        updateRackDetail();
        await loadWarehouseData();
    } catch (error) {
        console.error('창고 상태 변경 실패:', error);
        alert('창고 상태 변경에 실패했습니다.');
    }
}

// 재고 상세 모달
async function openStockModal(location) {
    selectedLocation = location;
    
    document.getElementById("stockLocationInfo").textContent = 
        `Zone ${currentZone.id} - Rack ${currentRack.id} - ${location.row} - ${location.col}`;
    
    // 위치 상태 select 설정
    const locationStatusInModal = document.getElementById("locationStatusInStockModal");
    if (locationStatusInModal) {
        locationStatusInModal.value = location.useYn;
        
        // 상태 변경 이벤트 리스너
        locationStatusInModal.onchange = function() {
            checkLocationStatusChange(location, this.value);
        };
    }
    
    bsStockModal.show();
    
    try {
        const response = await fetch(`${INVENTORY_API}/${location.locationId}`);
        stockList = await response.json();
        
        const tbody = document.getElementById("modalTBody");
        tbody.innerHTML = "";
        
        const statusMap = {
            NORMAL: "정상",
            EXPIRED: "만료",
            DISPOSAL_WAIT: "임박"
        };
        
        if (stockList.length === 0) {
            tbody.innerHTML = '<tr><td colspan="5" class="text-center py-3 text-muted">데이터가 없습니다.</td></tr>';
            // 재고 없으면 비활성화 가능
            if (locationStatusInModal) {
                locationStatusInModal.disabled = false;
            }
        } else {
            tbody.innerHTML = stockList.map((item, index) => `
                <tr>
                    <td>${item.itemId}</td>
                    <td class="text-start fw-semibold">${item.prodName}</td>
                    <td><span class="badge bg-label-info text-dark">${item.ivAmount}</span></td>
                    <td>${statusMap[item.status] || item.status}</td>
                    <td>
                        <button class="btn btn-sm btn-primary move-stock-btn" data-index="${index}">
                            이동
                        </button>
                    </td>
                </tr>
            `).join("");
            
            const buttons = tbody.querySelectorAll(".move-stock-btn");
            buttons.forEach(btn => {
                btn.addEventListener("click", function() {
                    const index = this.getAttribute("data-index");
                    const item = stockList[index];
                    
                    currentIvid = item.ivId;
                    currentIvQty = item.ivAmount;
                    expectOutboundQty = item.expectObAmount;
                    currentLoc = item.locationId;
                    canUseQty = item.ivAmount - item.expectObAmount;
                    
                    showMoveStockModal(item);
                });
            });
            
            // 재고가 있으면 비활성화 불가능하도록 체크
            checkLocationStatusChange(location, locationStatusInModal.value);
        }
        
        updateLocationCellColor(location.locationId, stockList.length);
    } catch (error) {
        console.error('재고 조회 실패:', error);
        document.getElementById('modalTableBody').innerHTML = 
            '<tr><td colspan="5" class="text-center text-danger">오류 발생</td></tr>';
    }
}

// 위치 상태 변경 체크
function checkLocationStatusChange(location, newStatus) {
    const locationStatusSelect = document.getElementById("locationStatusInStockModal");
    
    // 재고가 있는데 비활성화하려는 경우
    if (newStatus === 'N' && stockList.length > 0) {
        // 원래 상태로 되돌림
        if (locationStatusSelect) {
            locationStatusSelect.value = location.useYn;
        }
        alert('재고가 있는 위치는 비활성화할 수 없습니다.\n먼저 재고를 다른 위치로 이동하세요.');
    } 
}

// 위치 상태 저장 (재고 목록 모달에서)
async function saveLocationStatusFromStockModal() {
    if (!selectedLocation) return;
    
    const newStatus = document.getElementById("locationStatusInStockModal").value;
    
    // 재고가 있는데 비활성화하려는 경우
    if (newStatus === 'N' && stockList.length > 0) {
        alert('재고가 있는 위치는 비활성화할 수 없습니다.');
        return;
    }
    
    // 상태가 변경되지 않았으면
    if (newStatus === selectedLocation.useYn) {
        alert('변경된 내용이 없습니다.');
        return;
    }
	
    try {
        const response = await fetch(`${API_BASE}/locations/${selectedLocation.locationId}`, {
            method: 'POST',
			headers: {
				[csrfHeader]: csrfToken,
				'Content-Type': 'application/json'
			},
            body: JSON.stringify({ useYn: newStatus })
        });
        
        if (!response.ok) {
			const errorData = await response.json();
			throw new Error(errorData.message || '상태 변경 실패'); 
		}
        
        // 로컬 데이터 업데이트
        selectedLocation.useYn = newStatus;
        
        // 그리드 셀 업데이트
        const cells = document.querySelectorAll(".location-cell");
        cells.forEach(cell => {
            if (cell.dataset.locationId === selectedLocation.locationId) {
                if (newStatus === 'N') {
                    cell.classList.add('disabled');
                } else {
                    cell.classList.remove('disabled');
                }
            }
        });
        
        alert("위치 상태가 변경되었습니다.");
        
        // 정보 요약 업데이트
        updateRackDetail();
        
        // 전체 데이터 새로고침
        await loadWarehouseData();
    } catch (error) {
        console.error('위치 상태 변경 실패:', error);
        alert(error.message || '상태 변경 중 오류가 발생했습니다.');
    }
}

// 재고 목록 모달에서 위치 상태 저장 버튼
document.getElementById('btnSaveLocationStatusInStockModal').addEventListener('click', saveLocationStatusFromStockModal);

// 재고 이동 모달 호출
function showMoveStockModal(item) {
    if (typeof window.openMoveModal === "function") {
        try {
            window.openMoveModal(item);
        } catch (error) {
            console.error('재고 이동 모달 열기 실패:', error);
            alert('재고 이동 기능에 오류가 발생했습니다.');
        }
    } else {
        console.error('openMoveModal 함수를 찾을 수 없습니다.');
        alert('재고 이동 기능을 불러올 수 없습니다. inventoryMove.js를 확인하세요.');
    }
}

// 이벤트 핸들러에 추가
function initEventHandlers() {
    const btnRefresh = document.getElementById('btnRefresh');
    if (btnRefresh) {
        btnRefresh.addEventListener('click', loadWarehouseData);
    }
    
    const addRackForm = document.getElementById('addRackForm');
    if (addRackForm) {
        addRackForm.addEventListener('submit', submitAddRack);
    }
    
    const btnSaveLocationStatus = document.getElementById('btnSaveLocationStatus');
    if (btnSaveLocationStatus) {
        btnSaveLocationStatus.addEventListener('click', saveLocationStatus);
    }
    

}

// location cell 색상 업데이트 (오타 수정: celled -> cellId)
function updateLocationCellColor(locationId, count) {
    const cells = document.querySelectorAll(".location-cell");
    cells.forEach(cell => {
        // 수정됨
        if (cell.dataset.locationId === locationId) {
            const oldBadge = cell.querySelector(".stock-badge");
            if (oldBadge) oldBadge.remove();
            
            if (count > 0) {
                const badge = document.createElement("span");
                badge.className = "stock-badge";
                badge.textContent = count;
                cell.appendChild(badge);
            }
            
            const location = currentRack.locations.find(l => l.locationId === locationId);
            if (location) {
                location.stockCount = count;
            }
        }
    });
    
    updateRackDetail();
}

// 재고 이동 모달 
function showMoveStockModal(item) {
    if (typeof window.openMoveModal === "function") {
        window.openMoveModal(item);
    } else {
        console.error('openMoveModal 함수를 찾을 수 없습니다.');
    }
}

// 이벤트 핸들러
function initEventHandlers() {
    const btnRefresh = document.getElementById('btnRefresh');
    if (btnRefresh) {
        btnRefresh.addEventListener('click', loadWarehouseData);
    }
    
    const addRackForm = document.getElementById('addRackForm');
    if (addRackForm) {
        addRackForm.addEventListener('submit', submitAddRack);
    }
    
    const btnSaveLocationStatus = document.getElementById('btnSaveLocationStatus');
    if (btnSaveLocationStatus) {
        btnSaveLocationStatus.addEventListener('click', saveLocationStatus);
    }
}
