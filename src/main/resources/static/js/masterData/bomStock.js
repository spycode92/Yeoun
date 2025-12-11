window.onload = function () {	
	bomGridAllSearch();// bom그리드 조회
	safetyStockGridAllSearch();//안전재고 그리드 조회
}

const modalElement = document.getElementById('safetyStockModal');//안전재고 모달
modalElement.addEventListener('shown.bs.modal', function () {
    grid2.refreshLayout();
});

class StatusModifiedRenderer {
    constructor(props) {
        const el = document.createElement('div');
        el.className = 'tui-grid-cell-content-renderer'; 
        this.el = el;
        this.grid = props.grid; 
        
        this.render(props);
    }

    getElement() {
        return this.el;
    }

    render(props) {
        const value = props.value;
        const rowKey = props.rowKey; 
        
        this.el.textContent = value; 

        // 💡 수정되거나 추가된 행 상태 확인 로직
        let isUpdatedOrCreated = false;
        
        if (this.grid) {
            const modifiedRows = this.grid.getModifiedRows();
            
            // 1. 수정된 행(updatedRows) 목록에서 현재 rowKey 확인
            const isUpdated = modifiedRows.updatedRows.some(row => String(row.rowKey) === String(rowKey));
            
            // 2. 새로 추가된 행(createdRows) 목록에서 현재 rowKey 확인
            const isCreated = modifiedRows.createdRows.some(row => String(row.rowKey) === String(rowKey));
            
            // 두 상태 중 하나라도 true이면 스타일 적용
            isUpdatedOrCreated = isUpdated || isCreated;
        }
        
        // 🎨 인라인 스타일 적용
        if (isUpdatedOrCreated) {
            // 수정되거나 추가된 행에 적용될 스타일
            this.el.style.backgroundColor = '#c3f2ffff'; 
            this.el.style.color = '#000000';         
            this.el.style.fontWeight = 'bold';
        } else {
            // 조건 불충족 시 스타일 초기화
            this.el.style.backgroundColor = '';
            this.el.style.color = '';
            this.el.style.fontWeight = '';
        }
    }
}


const Grid = tui.Grid;
//g-grid1 bom그리드
const grid1 = new Grid({
	  el: document.getElementById('bomGrid'), 
	  data: [],
      rowHeaders: ['rowNum','checkbox'],
	  columns: [

	    {header: 'BOMId' ,name: 'bomId' ,align: 'center',editor: 'text',filter: "select"
			,renderer:{ type: StatusModifiedRenderer}	
		}
		,{header: '완제품 id' ,name: 'prdId' ,align: 'center',editor: 'text',filter: "select"
			,renderer:{ type: StatusModifiedRenderer}	
		}
		,{header: '원재료 id' ,name: 'matId' ,align: 'center',editor: 'text',width: 230
			,renderer:{ type: StatusModifiedRenderer}	
		}
		,{header: '원재료 사용량' ,name: 'matQty' ,align: 'center',editor: 'text'
			,renderer:{ type: StatusModifiedRenderer}	
		}
		,{header: '단위' ,name: 'matUnit' ,align: 'center',editor: 'text',filter: "select"
			,renderer:{ type: StatusModifiedRenderer}
			,editor: {
				type: 'select', // 드롭다운 사용
				options: {
					// value는 실제 데이터 값, text는 사용자에게 보이는 값
					listItems: [
						{ text: 'g', value: 'g' },
						{ text: 'ml', value: 'ml' },
						{ text: 'EA', value: 'EA' }
					]
				}
			}	
		}
		,{header: '순서' ,name: 'bomSeqNo' ,align: 'center',editor: 'text'
			,renderer:{ type: StatusModifiedRenderer}	
		}
		,{header: '생성자ID' ,name: 'createdId' ,align: 'center'}
		,{header: '생성일자' ,name: 'createdDate' ,align: 'center'}
		,{header: '수정자ID' ,name: 'updatedId' ,align: 'center'}
		,{header: '수정일시' ,name: 'updatedDate' ,align: 'center'}           
	  ]
	  ,bodyHeight: 500 // 그리드 본문의 높이를 픽셀 단위로 지정. 스크롤이 생김.
	  ,height:100
	  ,columnOptions: {
    		resizable: true
  	  }
	  ,pageOptions: {
    		useClient: true,
    		perPage: 20
  	  }
	});
	

//g-grid2 안전재고 그리드
const grid2 = new Grid({
		  el: document.getElementById('safetyStockGrid'), 
		  data: [],
	      rowHeaders: ['rowNum','checkbox'],
		  columns: [

		    {header: '품목코드' ,name: 'itemId' ,align: 'center',editor: 'text'
				,renderer:{ type: StatusModifiedRenderer}	
			}
			,{header: '품목종류' ,name: 'itemType' ,align: 'center',editor: 'text'
				,renderer:{ type: StatusModifiedRenderer}	

			}
			,{header: '품목명' ,name: 'itemName' ,align: 'center',editor: 'text',width: 230
				,renderer:{ type: StatusModifiedRenderer}	
			}
			,{header: '용량' ,name: 'volume' ,align: 'center',editor: 'text',filter: "select"
				,renderer:{ type: StatusModifiedRenderer}
			}
			,{header: '단위' ,name: 'itemUnit' ,align: 'center'
				,renderer:{ type: StatusModifiedRenderer}
				,editor: {
					type: 'select', // 드롭다운 사용
					options: {
						listItems: [
							{ text: 'g', value: 'g' },
							{ text: 'ml', value: 'ml' },
							{ text: 'EA', value: 'EA' }
						]
					}
				}

			}
			,{header: '정책방식' ,name: 'policyType' ,align: 'center'
				,renderer:{ type: StatusModifiedRenderer}
				,editor: {
					type: 'select', // 드롭다운 사용
					options: {
						listItems: [
							{ text: '고정 계산방식', value: 'FIXED_QTY' },
							{ text: '일수기반', value: 'DAYS COVER' },
						]
					}
				}
			}
			,{header: '정책일수' ,name: 'policyDays' ,align: 'center',editor: 'text'
				,renderer:{ type: StatusModifiedRenderer}
			}
	        ,{header: '일별 수량' ,name: 'safetyStockQtyDaily' ,align: 'center',editor: 'text'
				,renderer:{ type: StatusModifiedRenderer}
			}
			,{header: '총 수량' ,name: 'safetyStockQty' ,align: 'center',editor: 'text'
				,renderer:{ type: StatusModifiedRenderer}
			}
			,{header: '상태' ,name: 'status' ,align: 'center',editor: 'text'
				,renderer:{ type: StatusModifiedRenderer}
				,editor: {
					type: 'select', // 드롭다운 사용
					options: {
						// value는 실제 데이터 값, text는 사용자에게 보이는 값
						listItems: [
							{ text: 'ACTIVE', value: 'ACTIVE' },//활성
							{ text: 'INACTIVE', value: 'INACTIVE' },//비활성
							{ text: 'DISCONTINUED', value: 'DISCONTINUED' },//단종
							{ text: 'SEASONAL', value: 'SEASONAL' },//시즌상품
							{ text: 'OUT_OF_STOCK', value: 'OUT_OF_STOCK' }//단종
						]
					}
				}
			}
			,{header: '비고' ,name: 'remark' ,align: 'center',editor: 'text'
				,renderer:{ type: StatusModifiedRenderer}
			}           
		  ]
		  ,bodyHeight: 500 // 그리드 본문의 높이를 픽셀 단위로 지정. 스크롤이 생김.
		  ,height:100
		  ,columnOptions: {
	    		resizable: true
	  	  }
		  ,pageOptions: {
	    		useClient: true,
	    		perPage: 20
	  	  }
});


grid1.on('beforeChange', (ev) => {
    const { rowKey, columnName } = ev.changes[0]; // 변경된 데이터 목록 (배열)
	if (columnName === 'prdId' || columnName === 'matId') {
	        // 💡 핵심 수정: rowKey 대신, 현재 행의 'prdId' 값을 가져옵니다.
	        const prdIdValue = grid1.getValue(rowKey, 'prdId');
			const matIdValue = grid1.getValue(rowKey, 'matId');
	        
	        // prdId 값이 비어있거나 null, undefined인 경우를 '새 행'으로 간주합니다.
	        const isNewRow = !prdIdValue || !matIdValue; 

	        console.log("prdId 값:", prdIdValue,"matId 값:",matIdValue, " | isNewRow:", isNewRow);

	        // 기존 행일 경우 (isNewRow가 false, 즉 prdIdValue가 있는 경우)
	        if (!isNewRow) {
	            ev.stop(); // 편집 모드 진입 차단
	            alert('기존 완제품 Id,원재료Id는 수정할 수 없습니다.  삭제후 새로추가(등록) 해주세요!'); 
	        }
	    }
});


grid2.on('beforeChange', (ev) => {
    const { rowKey, columnName } = ev.changes[0]; // 변경된 데이터 목록 (배열)
	if (columnName === 'itemId') {
	        // 💡 핵심 수정: rowKey 대신, 현재 행의 'prdId' 값을 가져옵니다.
	        const itemIdValue = grid2.getValue(rowKey, 'itemId');
	        
	        // itemId 값이 비어있거나 null, undefined인 경우를 '새 행'으로 간주합니다.
	        const isNewRow = !itemIdValue; 

	        console.log("itemId 값:", itemIdValue," | isNewRow:", isNewRow);

	        // 기존 행일 경우 (isNewRow가 false, 즉 itemIdValue가 있는 경우)
	        if (!isNewRow) {
	            ev.stop(); // 편집 모드 진입 차단
	            alert('기존 품목코드는 수정할 수 없습니다.  삭제후 새로추가(등록) 해주세요!'); 
	        }
	    }
});

//bom그리드 전체조회
function bomGridAllSearch() {

	fetch('/bom/list', {
		method: 'GET',
		headers: {
			[csrfHeader]: csrfToken,
			'Content-Type': 'application/json'
		},
		
	})
	.then(res => {
	    if (!res.ok) {
	        throw new Error(`HTTP error! status: ${res.status}`);
	    }
	    
	    // 💡 추가된 로직: 응답 본문이 비어 있는지 확인
	    const contentType = res.headers.get("content-type");
	    if (!contentType || !contentType.includes("application/json")) {
	        // Content-Type이 JSON이 아니거나, 200 OK인데 본문이 비어있다면 (Empty)
	        if (res.status === 204 || res.headers.get("Content-Length") === "0") {
	             return []; // 빈 배열 반환하여 grid 오류 방지
	        }
	        // JSON이 아닌 다른 데이터(HTML 오류 등)가 있다면 텍스트로 읽어 오류 발생
	        return res.text().then(text => {
	            throw new Error(`Expected JSON but received: ${text.substring(0, 100)}...`);
	        });
	    }

	    return res.json(); // 유효한 JSON일 때만 파싱 시도
	})
		.then(data => {
			
			console.log("검색데이터:", data);
			grid1.resetData(data);
		})
		.catch(err => {
			console.error("조회오류", err);
			grid1.resetData([]);
		
		});

}
//안전재고 그리드 전체조회
function safetyStockGridAllSearch() {

	fetch('/safetyStock/list', {
		method: 'GET',
		headers: {
			[csrfHeader]: csrfToken,
			'Content-Type': 'application/json'
		},
		
	})
	.then(res => {
	    if (!res.ok) {
	        throw new Error(`HTTP error! status: ${res.status}`);
	    }
	    
	    // 💡 추가된 로직: 응답 본문이 비어 있는지 확인
	    const contentType = res.headers.get("content-type");
	    if (!contentType || !contentType.includes("application/json")) {
	        // Content-Type이 JSON이 아니거나, 200 OK인데 본문이 비어있다면 (Empty)
	        if (res.status === 204 || res.headers.get("Content-Length") === "0") {
	             return []; // 빈 배열 반환하여 grid 오류 방지
	        }
	        // JSON이 아닌 다른 데이터(HTML 오류 등)가 있다면 텍스트로 읽어 오류 발생
	        return res.text().then(text => {
	            throw new Error(`Expected JSON but received: ${text.substring(0, 100)}...`);
	        });
	    }

	    return res.json(); // 유효한 JSON일 때만 파싱 시도
	})
		.then(data => {
			
			console.log("검색데이터2:", data);
			grid2.resetData(data);	
		})
		.catch(err => {
			console.error("조회오류", err);
			grid2.resetData([]);
		
		});

}

//bom row 추가
const addBomRowBtn = document.getElementById('addBomRowBtn');
addBomRowBtn.addEventListener('click', function() {
	grid1.prependRow();
});

//안전재고 row 추가
const addSafetyStockRowBtn = document.getElementById('addSafetyStockRowBtn');
addSafetyStockRowBtn.addEventListener('click', function() {
	grid2.prependRow();
});

//bom row 저장
const saveBomRowBtn = document.getElementById('saveBomRowBtn');
saveBomRowBtn.addEventListener('click', function() {
	const modifiedData = (typeof grid1.getModifiedRows === 'function') ? (grid1.getModifiedRows() || {}) : {};
	const updatedRows = Array.isArray(modifiedData.updatedRows) ? modifiedData.updatedRows : [];
	let createdRows = Array.isArray(modifiedData.createdRows) ? modifiedData.createdRows : [];

	// 새로 추가된 행 중 모든 필드가 비어있는(빈 행) 경우 그리드에서 제거하고 서버 전송 대상에서 제외
	const isRowEmpty = (row) => {
		if (!row) return true;
		const vals = Object.values(row);
		if (vals.length === 0) return true;
		return vals.every(v => v === null || v === undefined || (typeof v === 'string' && v.trim() === ''));
	};
	const emptyCreated = createdRows.filter(isRowEmpty);
	if (emptyCreated.length > 0) {
		emptyCreated.forEach(r => {
			try {
				const key = r && (r.rowKey || r.prdId);
				if (key && typeof grid1.removeRow === 'function') {
					grid1.removeRow(key);
				} else if (key && typeof grid1.deleteRow === 'function') {
					grid1.deleteRow(key);
				}
			} catch (e) {
				console.warn('빈 행 삭제 실패', e);
			}
		});
		// 서버로 보낼 createdRows에서 빈 행 제외
		createdRows = createdRows.filter(r => !isRowEmpty(r));
		// 반영: modifiedData 객체에도 반영해 전송값 일관성 유지
		try { modifiedData.createdRows = createdRows; } catch (e) {}
	}
	
	if (updatedRows.length === 0 && createdRows.length === 0) {
		alert('수정된 내용이 없습니다.');
		return;
	}

	// 누락 방지 검사 및 보정: bomId,prdId, matId는 필수 값	
	const created = Array.isArray(modifiedData.createdRows) ? modifiedData.createdRows : [];
	const updated = Array.isArray(modifiedData.updatedRows) ? modifiedData.updatedRows : [];
	const problems = [];
	const ensureBomId = (row) => {
		if (!row) return;
		const bomId = (row.bomId || '').toString().trim();
		const prdId = (row.prdId || '').toString().trim();
		const matId = (row.matId || '').toString().trim();
		if (!bomId) problems.push({row, msg: 'bomId 누락'});
		if (!prdId) problems.push({row, msg: 'prdId 누락'});
		if (!matId) problems.push({row, msg: 'matId 누락'});
		
	};

	created.forEach(ensureBomId);
	updated.forEach(ensureBomId);

	// 추가 필수값: matQty (null이면 DB 제약으로 실패하므로 클라이언트에서 선검증)
	const ensureMatQty = (row) => {
		if (!row) return;
		const v = row.matQty;
		if (v === null || v === undefined || (typeof v === 'string' && v.trim() === '')) {
			problems.push({row, msg: 'matQty 누락'});
			return;
		}
		// 숫자 검사
		const num = Number(v);
		if (Number.isNaN(num)) {
			problems.push({row, msg: 'matQty 숫자 형식 아님'});
		}
	};

	created.forEach(ensureMatQty);
	updated.forEach(ensureMatQty);

	if (problems.length > 0) {
		const msgs = problems.slice(0,10).map(p => {
			const id = (p.row && (p.row.bomId || p.row.rowKey || p.row.prdId || p.row.matId)) || '#';
			return `${id} -> ${p.msg}`;
		}).join('\n');
		alert('다음 행에 필수값이 누락되어 저장할 수 없습니다.\n' + msgs + '\n(품번(prdId)과 원재료(matId)을 입력해주세요.)');
		return;
	}
	console.log('sending /bom/save payload:', modifiedData);
	console.log('csrf header:', csrfHeader, csrfToken);
	fetch('/bom/save', {
		method: 'POST',
		credentials: 'same-origin',
		headers: {
			[csrfHeader]: csrfToken,
			'Content-Type': 'application/json'
		},
		body: JSON.stringify(modifiedData)
	})
	.then(res => {
		if (!res.ok) {
			throw new Error(`HTTP error! status: ${res.status}`);
		}
		const ct = (res.headers.get('content-type') || '').toLowerCase();
		if (ct.includes('application/json')) return res.json();
		return res.text();
	})
	.then(parsed => {
		// 서버가 plain text 또는 JSON을 보낼 수 있도록 유연히 처리
		console.log("저장응답데이터:", parsed);
		const okTexts = ['success','ok','true'];
		let success = false;
		if (typeof parsed === 'string') {
			success = okTexts.includes(parsed.trim().toLowerCase()) || parsed.trim().toLowerCase().startsWith('success');
		} else if (parsed && typeof parsed === 'object') {
			const status = (parsed.status || parsed.result || '').toString().toLowerCase();
			const message = (parsed.message || '').toString().toLowerCase();
			success = status === 'success' || okTexts.includes(message) || message.includes('success');
		}
		if (!success) throw new Error('Unexpected response: ' + JSON.stringify(parsed));
		alert("저장이 완료되었습니다.");
		bomGridAllSearch();//저장후 전체조회
	})
	.catch(err => {
		console.error("저장오류", err);
		alert("저장 중 오류가 발생했습니다.");
	});
});

//안전재고 row 저장
const saveSafetyStockRowBtn = document.getElementById('saveSafetyStockRowBtn');
saveSafetyStockRowBtn.addEventListener('click', function() {
	console.log("안전재고 저장버튼 클릭");
	const modifiedData = (typeof grid2.getModifiedRows === 'function') ? (grid2.getModifiedRows() || {}) : {};
	const updatedRows = Array.isArray(modifiedData.updatedRows) ? modifiedData.updatedRows : [];
	let createdRows = Array.isArray(modifiedData.createdRows) ? modifiedData.createdRows : [];

	// 빈 행 검사/제거 (새로 추가된 행 중 모든 필드가 빈 경우 화면에서만 제거)
	const isRowEmpty = (row) => {
		if (!row) return true;
		const vals = Object.values(row);
		if (vals.length === 0) return true;
		return vals.every(v => v === null || v === undefined || (typeof v === 'string' && v.trim() === ''));
	};
	const emptyCreated = createdRows.filter(isRowEmpty);
	if (emptyCreated.length > 0) {
		emptyCreated.forEach(r => {
			try {
				const key = r && (r.rowKey || r.itemId);
				if (key && typeof grid2.removeRow === 'function') {
					grid2.removeRow(key);
				} else if (key && typeof grid2.deleteRow === 'function') {
					grid2.deleteRow(key);
				}
			} catch (e) {
				console.warn('빈 행 삭제 실패', e);
			}
		});
		createdRows = createdRows.filter(r => !isRowEmpty(r));
		try { modifiedData.createdRows = createdRows; } catch (e) {}
	}

	if (updatedRows.length === 0 && createdRows.length === 0) {
		alert('수정된 내용이 없습니다.');
		return;
	}

	// 필수값 검사: itemId와 safetyStockQty(총수량) 필수
	const created = Array.isArray(modifiedData.createdRows) ? modifiedData.createdRows : [];
	const updated = Array.isArray(modifiedData.updatedRows) ? modifiedData.updatedRows : [];
	const problems = [];
	const ensureItemId = (row) => {
		if (!row) return;
		const itemId = (row.itemId || '').toString().trim();
		if (!itemId) problems.push({row, msg: 'itemId 누락'});
	};
	const ensureSafetyQty = (row) => {
		if (!row) return;
		const v = row.safetyStockQty;
		if (v === null || v === undefined || (typeof v === 'string' && v.trim() === '')) {
			problems.push({row, msg: 'safetyStockQty 누락'});
			return;
		}
		const num = Number(v);
		if (Number.isNaN(num)) problems.push({row, msg: 'safetyStockQty 숫자 형식 아님'});
	};
	// 선택적 숫자 필드 검사
	const ensureOptionalNumber = (row, field, label) => {
		if (!row) return;
		const v = row[field];
		if (v === null || v === undefined || v === '') return;
		const num = Number(v);
		if (Number.isNaN(num)) problems.push({row, msg: `${label} 숫자 형식 아님`});
	};

	created.forEach(ensureItemId);
	updated.forEach(ensureItemId);
	created.forEach(ensureSafetyQty);
	updated.forEach(ensureSafetyQty);
	created.forEach(r => ensureOptionalNumber(r, 'policyDays', 'policyDays'));
	updated.forEach(r => ensureOptionalNumber(r, 'policyDays', 'policyDays'));
	created.forEach(r => ensureOptionalNumber(r, 'safetyStockQtyDaily', 'safetyStockQtyDaily'));
	updated.forEach(r => ensureOptionalNumber(r, 'safetyStockQtyDaily', 'safetyStockQtyDaily'));

	if (problems.length > 0) {
		const msgs = problems.slice(0,10).map(p => {
			const id = (p.row && (p.row.itemId || p.row.rowKey)) || '#';
			return `${id} -> ${p.msg}`;
		}).join('\n');
		alert('다음 행에 필수값이 누락되어 저장할 수 없습니다.\n' + msgs + '\n(itemId와 총수량(safetyStockQty)을 확인해주세요.)');
		return;
	}

	console.log('sending /safetyStock/save payload:', modifiedData);
	console.log('csrf header:', csrfHeader, csrfToken);
	fetch('/safetyStock/save', {
		method: 'POST',
		credentials: 'same-origin',
		headers: {
			[csrfHeader]: csrfToken,
			'Content-Type': 'application/json'
		},
		body: JSON.stringify(modifiedData)
	})
	.then(res => {
		if (!res.ok) {
			throw new Error(`HTTP error! status: ${res.status}`);
		}
		const ct = (res.headers.get('content-type') || '').toLowerCase();
		if (ct.includes('application/json')) return res.json();
		return res.text();
	})
	.then(parsed => {
		console.log("저장응답데이터:", parsed);
		const okTexts = ['success','ok','true'];
		let success = false;
		if (typeof parsed === 'string') {
			const p = parsed.trim().toLowerCase();
			// 기존 판단: success/ok 포함 또는 startsWith 'success'/'ok'
			if (okTexts.includes(p) || p.startsWith('success') || p.startsWith('ok')) {
				success = true;
			}
			// 서버가 "Created ... rows: N" 또는 "created" 같은 텍스트를 반환하는 경우 허용
			else if (p.startsWith('created') || p.includes('created') || p.includes('rows')) {
				success = true;
			}
			// 단순 숫자 응답(예: "1") -> 0 이상은 성공으로 간주
			else if (/^\d+$/.test(p)) {
				try { success = Number(p) >= 0; } catch (e) { success = false; }
			}
		} else if (parsed && typeof parsed === 'object') {
			const status = (parsed.status || parsed.result || '').toString().toLowerCase();
			const message = (parsed.message || '').toString().toLowerCase();
			success = status === 'success' || okTexts.includes(message) || message.includes('success') || message.includes('created');
		}
		if (!success) throw new Error('Unexpected response: ' + JSON.stringify(parsed));
		alert("저장이 완료되었습니다.");
		safetyStockGridAllSearch();//저장후 전체조회
	})
	.catch(err => {
		console.error("저장오류", err);
		alert("저장 중 오류가 발생했습니다.");
	});
});

//bom그리드 선택삭제
const deleteBomRowBtn = document.getElementById('deleteBomRowBtn');
deleteBomRowBtn.addEventListener('click', async function() {
	console.log('BOM 그리드 삭제 버튼 클릭');

	// 체크된 rowKey들 수집
	let rowKeysToDelete = [];
	try {
		if (typeof grid1.getCheckedRowKeys === 'function') {
			rowKeysToDelete = grid1.getCheckedRowKeys() || [];
		} else if (typeof grid1.getCheckedRows === 'function') {
			const checkedRows = grid1.getCheckedRows() || [];
			rowKeysToDelete = checkedRows.map(r => r && (r.rowKey || r.prdId)).filter(Boolean);
		}else  {
			// 그리드 빈행 제거
			console.log('체크된 행 키:', rowKeysToDelete);

			rowKeysToDelete.forEach((key, i) => {
				grid1.deleteRow(rowKeysToDelete[i]);
			});

		}
		
	} catch (e) {
		console.warn('체크된 행 조회 실패', e);
	}

	if (!Array.isArray(rowKeysToDelete) || rowKeysToDelete.length === 0) {
		alert('삭제할 행을 선택(체크)해주세요.');
		return;
	}


		// 구분: 빈 행(또는 BomId가 없는 행)은 화면에서만 삭제하고, BomId가 있는 행만 서버에 삭제 요청
		try {
			const getAllData = () => (typeof grid1.getData === 'function' ? grid1.getData() : (grid1.data || []));
			const data = getAllData();
			// 그리드의 수정 정보에서 생성된(신규) 행들을 조회하여, 신규행은 UI에서만 삭제하도록 처리
			const modified = (typeof grid1.getModifiedRows === 'function') ? (grid1.getModifiedRows() || {}) : {};
			const createdRows = Array.isArray(modified.createdRows) ? modified.createdRows : [];
				const uiOnlyKeys = []; // 화면에서만 제거할 rowKey
				const serverPairs = []; // 서버에 삭제 요청할 {prdId, matId} 쌍 목록
			for (const key of rowKeysToDelete) {
				// 우선 해당 키가 생성된(신규) 행인지 확인
				const isCreated = createdRows.some(r => r && (String(r.rowKey) === String(key) || String(r.bomId) === String(key)));
				if (isCreated) {
					uiOnlyKeys.push(key);
					continue;
				}
				let row = null;
				if (typeof grid1.getRow === 'function') row = grid1.getRow(key);
				if (!row) row = data.find(d => d && (String(d.rowKey) === String(key) || String(d.prdId) === String(key)));
				// 빈 행 판단: 모든 필드가 비어있거나 prdId가 없으면 UI에서만 삭제
				const vals = row ? Object.values(row) : [];
				const allEmpty = !row || vals.length === 0 || vals.every(v => v === null || v === undefined || (typeof v === 'string' && v.trim() === ''));
                	if (allEmpty || !row || !row.prdId || !row.matId) {
                    	uiOnlyKeys.push(key);
                	} else {
                    	serverPairs.push({ prdId: String(row.prdId), matId: String(row.matId) });
                	}
			}

			// UI에서만 제거할 행들 삭제
			let removedUi = 0;
			if (uiOnlyKeys.length > 0) {
				for (const k of uiOnlyKeys) {
					try {
						if (typeof grid1.removeRow === 'function') { grid1.removeRow(k); removedUi++; continue; }
						if (typeof grid1.deleteRow === 'function') { grid1.deleteRow(k); removedUi++; continue; }
						const newData = data.filter(r => !(r && (String(r.rowKey) === String(k) || String(r.bomId) === String(k))));
						grid1.resetData(newData);
						removedUi++;
					} catch (e) { console.warn('UI 전용 행 삭제 실패', k, e); }
				}
			}

			// 서버에 삭제 요청 보낼 prdId+matId 쌍이 있으면 삭제 요청 수행
			if (serverPairs.length > 0) {
				if (!confirm('서버에서 실제로 삭제할 항목이 포함되어 있습니다. 선택한 항목을 삭제하시겠습니까?')) return;
				fetch('/bom/delete', {
					method: 'POST',
					credentials: 'same-origin',
					headers: {
						[csrfHeader]: csrfToken,
						'Content-Type': 'application/json'
					},
					body: JSON.stringify(serverPairs)
				})
				.then(res => {
					if (!res.ok) throw new Error(`HTTP error! status: ${res.status}`);
					const ct = (res.headers.get('content-type') || '').toLowerCase();
					if (ct.includes('application/json')) return res.json();
					return res.text();
				})
				.then(parsed => {
					console.log('삭제 응답:', parsed);
					const okTexts = ['success','ok','true'];
					let success = false;
					if (typeof parsed === 'string') {
						const p = parsed.trim().toLowerCase();
						success = okTexts.includes(p) || p.startsWith('success') || p.startsWith('ok');
					} else if (parsed && typeof parsed === 'object') {
						const status = (parsed.status || parsed.result || '').toString().toLowerCase();
						const message = (parsed.message || '').toString().toLowerCase();
						success = status === 'success' || okTexts.includes(message) || message.includes('success');
					}
					if (!success) throw new Error('Unexpected response: ' + JSON.stringify(parsed));
					// 서버 삭제 성공 시 그리드 재조회
					bomGridAllSearch();
				})
				.catch(err => {
					console.error('삭제 중 오류', err);
					try { alert('삭제 중 오류가 발생했습니다. ' + (err && err.message ? err.message : '')); } catch (e) {}
				});
			} else {
				if (removedUi > 0) alert('추가한 행을 화면에서만 삭제했습니다. (DB에는 반영되지 않음)');
			}
		} catch (e) {
			console.error('삭제 처리 중 오류', e);
			try { alert('삭제 처리 중 오류가 발생했습니다. ' + (e && e.message ? e.message : '')); } catch (err) {}
		}
	
});

//안전재고 그리드 선택삭제
const deleteSafetyStockRowBtn = document.getElementById('deleteSafetyStockRowBtn');
deleteSafetyStockRowBtn.addEventListener('click', async function() {
	console.log('안전재고 그리드 삭제 버튼 클릭');
	let checkedRows = [];
	try {
		if (typeof grid2.getCheckedRowKeys === 'function') {
			const keys = grid2.getCheckedRowKeys() || [];
			// map keys to rows
			checkedRows = (keys || []).map(k => {
				try { return (typeof grid2.getRow === 'function') ? grid2.getRow(k) : null; } catch(e) { return null; }
			}).filter(Boolean);
		} else if (typeof grid2.getCheckedRows === 'function') {
			checkedRows = grid2.getCheckedRows() || [];
		}
	} catch (e) { console.warn('체크된 행 조회 실패', e); }

	if (!Array.isArray(checkedRows) || checkedRows.length === 0) {
		alert('삭제할 행을 선택해주세요.');
		return;
	}

	if (!confirm(`${checkedRows.length}개의 행을 삭제하시겠습니까?`)) return;

	// 구분: UI 전용(신규/빈) 행은 화면에서만 제거, DB에 있는 행은 itemId 수집하여 서버에 요청
	const modified = (typeof grid2.getModifiedRows === 'function') ? (grid2.getModifiedRows() || {}) : {};
	const createdRows = Array.isArray(modified.createdRows) ? modified.createdRows : [];
	const uiOnlyKeys = [];
	const serverItemIds = [];

	// helper to find rowKey or itemId
	for (const row of checkedRows) {
		try {
			const key = row && (row.rowKey || row.itemId);
			const isCreated = createdRows.some(r => r && (String(r.rowKey) === String(key) || String(r.itemId) === String(key)));
			const allEmpty = !row || Object.values(row).length === 0 || Object.values(row).every(v => v === null || v === undefined || (typeof v === 'string' && v.trim() === ''));
			if (isCreated || allEmpty || !row.itemId) {
				uiOnlyKeys.push(key || (row && row.rowKey));
			} else {
				serverItemIds.push(String(row.itemId));
			}
		} catch (e) { console.warn('체크 행 처리 실패', e); }
	}

	// 먼저 UI 전용 행 삭제
	let removedUi = 0;
	if (uiOnlyKeys.length > 0) {
		for (const k of uiOnlyKeys) {
			try {
				if (k && typeof grid2.removeRow === 'function') { grid2.removeRow(k); removedUi++; continue; }
				if (k && typeof grid2.deleteRow === 'function') { grid2.deleteRow(k); removedUi++; continue; }
				// fallback: reset data excluding keys
				const data = (typeof grid2.getData === 'function' ? grid2.getData() : (grid2.data || []));
				const newData = data.filter(r => !(r && (String(r.rowKey) === String(k) || String(r.itemId) === String(k))));
				grid2.resetData(newData);
				removedUi++;
			} catch (e) { console.warn('UI 전용 행 삭제 실패', k, e); }
		}
	}

	// 서버 삭제 처리
	if (serverItemIds.length > 0) {
		if (!confirm('서버에서 실제로 삭제할 항목이 포함되어 있습니다. 선택한 항목을 삭제하시겠습니까?')) return;
		try {
			console.log('sending /safetyStock/delete payload:', serverItemIds);
			console.log('csrf header:', csrfHeader, csrfToken);
			const res = await fetch('/safetyStock/delete', {
				method: 'POST',
				credentials: 'same-origin',
				headers: {
					[csrfHeader]: csrfToken,
					'Content-Type': 'application/json'
				},
				body: JSON.stringify(serverItemIds)
			});
			if (!res.ok) throw new Error(`HTTP error! status: ${res.status}`);
			const ct = (res.headers.get('content-type') || '').toLowerCase();
			const parsed = ct.includes('application/json') ? await res.json() : await res.text();
			console.log('삭제 응답:', parsed);
			// 유연한 성공 판단
			let success = false;
			if (typeof parsed === 'string') {
				const p = parsed.trim().toLowerCase();
				success = p === 'ok' || p === 'success' || p.startsWith('deleted') || p.includes('deleted') || /^\d+$/.test(p);
			} else if (parsed && typeof parsed === 'object') {
				const status = (parsed.status || parsed.result || '').toString().toLowerCase();
				const message = (parsed.message || '').toString().toLowerCase();
				success = status === 'success' || message.includes('deleted') || message.includes('success');
			}
			if (!success) throw new Error('Unexpected response: ' + JSON.stringify(parsed));
			// 서버 삭제 성공 시 재조회
			safetyStockGridAllSearch();
		} catch (err) {
			console.error('삭제 중 오류', err);
			try { alert('삭제 중 오류가 발생했습니다. ' + (err && err.message ? err.message : '')); } catch (e) {}
		}
	} else {
		if (removedUi > 0) alert('추가한 행을 화면에서만 삭제했습니다. (DB에는 반영되지 않음)');
	}
});


