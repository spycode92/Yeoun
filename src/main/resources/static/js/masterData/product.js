
window.onload = function () {	
	productGridAllSearch();//완제품 그리드 조회
	materialGridAllSearch();//원재료 그리드 조회

}

document.querySelectorAll('button[data-bs-toggle="tab"]').forEach(tab => {
    tab.addEventListener('shown.bs.tab', function (e) {
        const targetId = e.target.getAttribute('data-bs-target');

        if (targetId === '#navs-product-tab') {//완제품탭
            grid1.refreshLayout();
        } else if (targetId === '#navs-material-tab') {//원재료 탭
            grid2.refreshLayout();
        }
    });
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

//g-grid1 완제품(상위품번)
const grid1 = new Grid({
	  el: document.getElementById('productGrid'),
      rowHeaders: [
			{ type: 'rowNum', header: 'No.'},
			{ type: 'checkbox' } 
		],
	  columns: [
		{header: '품번' ,name: 'prdId' ,align: 'center',editor: 'text',width: 100
		
			,renderer:{ type: StatusModifiedRenderer}	
		}
		,{header: '품목명' ,name: 'itemName' ,align: 'center',editor: 'text',filter: "select",width: 100
			,renderer:{ type: StatusModifiedRenderer}
		}
		,{header: '제품명' ,name: 'prdName' ,align: 'center',editor: 'text'
			,renderer:{ type: StatusModifiedRenderer}
		}
		,{header: '제품유형' ,name: 'prdCat' ,align: 'center',filter: "select",width: 100
			,renderer:{ type: StatusModifiedRenderer}
			,editor: {
				type: 'select', // 드롭다운 사용
				options: {
					// value는 실제 데이터 값, text는 사용자에게 보이는 값
					listItems: [
						{ text: '완제품', value: '완제품' },
						{ text: '반제품', value: '반제품' }
					]
				}
			}
		}
		,{header: '단위' ,name: 'prdUnit' ,align: 'center'
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
		,{header: '단가' ,name: 'unitPrice' ,align: 'center',editor: 'text'
			,renderer:{ type: StatusModifiedRenderer}
		}
        ,{header: '상태' ,name: 'prdStatus' ,align: 'center'
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
		,{header: '유효일자' ,name: 'effectiveDate' ,align: 'center',editor: 'text'
			,renderer:{ type: StatusModifiedRenderer}
		}
        ,{header: '제품상세설명' ,name: 'prdSpec' ,align: 'center',editor: 'text',width: 370
			,renderer:{ type: StatusModifiedRenderer}
		}
        ,{header: '생성자ID' ,name: 'createdId' ,align: 'center'
			,renderer:{ type: StatusModifiedRenderer}
		}
        ,{header: '생성일자' ,name: 'createdDate' ,align: 'center'
			,renderer:{ type: StatusModifiedRenderer}
		}
        ,{header: '수정자ID' ,name: 'updatedId' ,align: 'center'
			,renderer:{ type: StatusModifiedRenderer}
		}
        ,{header: '수정일시' ,name: 'updatedDate' ,align: 'center'
			,renderer:{ type: StatusModifiedRenderer}
		}           
	  ]
	  ,data: []
	  ,bodyHeight: 500 // 그리드 본문의 높이를 픽셀 단위로 지정. 스크롤이 생김.
	  ,height:100
	  ,columnOptions: {
    		resizable: true
  	  }
	  ,pageOptions: {
			useClient: true
			,perPage: 20
		}
});

//g-grid2 원재료(하위품번)
const grid2 = new Grid({
	    el: document.getElementById('materialGrid'),
          rowHeaders: [
			{ type: 'rowNum', header: 'No.'},
			{ type: 'checkbox' } 
		],
	    columns: [
		    {header: '원재료ID' ,name: 'matId' ,align: 'center',editor: 'text'
				,renderer:{ type: StatusModifiedRenderer}	
			}
		    ,{header: '원재료 품목명' ,name: 'matName' ,align: 'center',editor: 'text'
				,renderer:{ type: StatusModifiedRenderer}	
			}
		    ,{header: '원재료 유형' ,name: 'matType' ,align: 'center',editor: 'text',filter: "select"
				,renderer:{ type: StatusModifiedRenderer}	
				,editor: {
					type: 'select', // 드롭다운 사용
					options: {
						// value는 실제 데이터 값, text는 사용자에게 보이는 값
						listItems: [
							{ text: '원재료', value: 'RAW' },
							{ text: '부자재', value: 'SUB' },
							{ text: '포장재', value: 'PKG' },
							{ text: '공정중', value: 'WIP' },
							{ text: '생산품', value: 'FIN' },
							{ text: '박스', value: 'BOX' }
						]
					}
				}
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
	        ,{header: '유효일자' ,name: 'effectiveDate' ,align: 'center',editor: 'text'
				,renderer:{ type: StatusModifiedRenderer}	
			}
	        ,{header: '상세설명(원재료)' ,name: 'matDesc' ,align: 'center',editor: 'text',width: 280
				,renderer:{ type: StatusModifiedRenderer}	
			}
	        ,{header: '생성자ID' ,name: 'createdId' ,align: 'center'}
	        ,{header: '생성일자' ,name: 'createdDate' ,align: 'center'}
	        ,{header: '수정자ID' ,name: 'updatedId' ,align: 'center'}
	        ,{header: '수정일시' ,name: 'updatedDate' ,align: 'center'}           
	    ],
	    data: []
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
	if (columnName === 'prdId') {
	        // 💡 핵심 수정: rowKey 대신, 현재 행의 'prdId' 값을 가져옵니다.
	        const prdIdValue = grid1.getValue(rowKey, 'prdId');
	        
	        // prdId 값이 비어있거나 null, undefined인 경우를 '새 행'으로 간주합니다.
	        const isNewRow = !prdIdValue; 

	        console.log("prdId 값:", prdIdValue, " | isNewRow:", isNewRow);

	        // 기존 행일 경우 (isNewRow가 false, 즉 prdIdValue가 있는 경우)
	        if (!isNewRow) {
	            ev.stop(); // 편집 모드 진입 차단
	            alert('기존 품번은 수정할 수 없습니다. 삭제후 새로추가(등록) 해주세요!'); 
	        }
	    }
});

grid2.on('beforeChange', (ev) => {
    const { rowKey, columnName } = ev.changes[0]; // 변경된 데이터 목록 (배열)
	if (columnName === 'matId') {
	        // 💡 핵심 수정: rowKey 대신, 현재 행의 'prdId' 값을 가져옵니다.
	        const matIdValue = grid2.getValue(rowKey, 'matId');
	        
	        // prdId 값이 비어있거나 null, undefined인 경우를 '새 행'으로 간주합니다.
	        const isNewRow = !matIdValue; 

	        console.log("matId 값:", matIdValue, " | isNewRow:", isNewRow);

	        // 기존 행일 경우 (isNewRow가 false, 즉 prdIdValue가 있는 경우)
	        if (!isNewRow) {
	            ev.stop(); // 편집 모드 진입 차단
	            alert('기존 원재료ID는 수정할 수 없습니다. 삭제후 새로추가(등록) 해주세요!'); 
	        }
	    }
});




function productGridAllSearch() {

	fetch('/masterData/product/list', {
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
			//grid1.resetData([]);
		
		});

}

function materialGridAllSearch() {

	fetch('/material/list', {
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

	    return res.json(); // 유효한 JSON일 때만 파싱 시도
	})
		.then(data => {
			
			console.log("검색데이터:", data);
			grid2.resetData(data);
		})
		.catch(err => {
			console.error("조회오류", err);
			grid2.resetData([]);
		
		});

}

//완제품 row 추가
const addProductRowBtn = document.getElementById('addProductRowBtn');
addProductRowBtn.addEventListener('click', function() {
   grid1.prependRow();
});
//원재료 row 추가
const addMaterialRowBtn = document.getElementById('addMaterialRowBtn');
addMaterialRowBtn.addEventListener('click', function() {
   grid2.prependRow();
});

//완제품 row 저장: POST JSON형식으로 서버에 요청
const saveProductRowBtn = document.getElementById('saveProductRowBtn');
saveProductRowBtn.addEventListener('click', function() {

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

	// 저장 전 필수값 검사: prdId는 반드시 필요합니다 (신규 행도 먼저 품번 입력 필요)
	const requiredFields = ['prdId', 'itemName', 'prdName'];
	const fieldLabels = { prdId: '품번', itemName: '품목명', prdName: '제품명' };
	const rowsToCheck = [...createdRows, ...updatedRows];
	const invalidRows = rowsToCheck.map((row, idx) => {
		const missing = requiredFields.filter(f => {
			try {
				const v = row[f];
				return v === null || v === undefined || (typeof v === 'string' && v.trim() === '');
			} catch (e) {
				return true;
			}
		});
		if (missing.length > 0) {
			const id = row.prdId || row.rowKey || ('#' + (idx + 1));
			return { id, missing };
		}
		return null;
	}).filter(Boolean);

	if (invalidRows.length > 0) {
		const lines = invalidRows.map(r => {
			const missNames = r.missing.map(m => fieldLabels[m] || m).join(', ');
			return `${r.id} (누락: ${missNames})`;
		});
		alert('다음 행에 필수값이 비어 있어 저장할 수 없습니다.\n특히 신규 행은 먼저 품번을 입력해 주세요.\n' + lines.join('\n'));
		return;
	}

	fetch('/masterData/product/save', {
		method: 'POST',
		credentials: 'same-origin',
		headers: {
			[csrfHeader]: csrfToken,
			'Content-Type': 'application/json'
		},
		body: JSON.stringify(modifiedData)
	})
	.then(res => {
		if (!res.ok) throw new Error(`HTTP error! status: ${res.status}`);
		const ct = (res.headers.get('content-type') || '').toLowerCase();
		if (ct.includes('application/json')) return res.json();
		return res.text();
	})
	.then(parsed => {
		const okTexts = ['success', 'ok', 'true'];
		const isSuccess = (p) => {
			if (typeof p === 'string') return okTexts.includes(p.trim().toLowerCase());
			if (!p) return false;
			const status = (p.status || p.result || '').toString().toLowerCase();
			const message = (p.message || '').toString().toLowerCase();
			return status === 'success' || okTexts.includes(message) || message.includes('success');
		};

		if (!isSuccess(parsed)) throw new Error('Unexpected response: ' + JSON.stringify(parsed));
		console.log('저장 성공:', parsed);
		productGridAllSearch();
	})
	.catch(err => {
		console.error('저장 오류', err);
		try { alert('저장 중 오류가 발생했습니다. ' + (err && err.message ? err.message : '')); } catch (e) {}
	});
});

//원재료 row 저장: POST JSON형식으로 서버에 요청
const saveMaterialRowBtn = document.getElementById('saveMaterialRowBtn');
saveMaterialRowBtn.addEventListener('click', function() {
	const modifiedData = (typeof grid2.getModifiedRows === 'function') ? (grid2.getModifiedRows() || {}) : {};
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
				const key = r && (r.rowKey || r.matId);
				if (key && typeof grid2.removeRow === 'function') {
					grid2.removeRow(key);
				} else if (key && typeof grid2.deleteRow === 'function') {
					grid2.deleteRow(key);
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
	fetch('/material/save', {
		method: 'POST',
		credentials: 'same-origin',
		headers: {
			[csrfHeader]: csrfToken,
			'Content-Type': 'application/json'
		},
		body: JSON.stringify(modifiedData)
	})
	.then(res => {
		if (!res.ok) throw new Error(`HTTP error! status: ${res.status}`);
		const ct = (res.headers.get('content-type') || '').toLowerCase();
		if (ct.includes('application/json')) return res.json();
		return res.text();
	})
	.then(parsed => {
		const okTexts = ['success', 'ok', 'true'];
		const isSuccess = (p) => {
			if (typeof p === 'string') return okTexts.includes(p.trim().toLowerCase());
			if (!p) return false;	
			const status = (p.status || p.result || '').toString().toLowerCase();
			const message = (p.message || '').toString().toLowerCase();
			return status === 'success' || okTexts.includes(message) || message.includes('success');
		};
		if (!isSuccess(parsed)) throw new Error('Unexpected response: ' + JSON.stringify(parsed));
		console.log('저장 성공:', parsed);
		materialGridAllSearch();
	})
	.catch(err => {
		console.error('저장 오류', err);
		try { alert('저장 중 오류가 발생했습니다. ' + (err && err.message ? err.message : '')); } catch (e) {}
	});
});


//완제품row 삭제: POST JSON형식으로 서버에 요청
const deleteProductRowBtn = document.getElementById('deleteProductRowBtn');
deleteProductRowBtn.addEventListener('click', async function() {

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

	// 간결한 방식으로 각 rowKey로부터 prdId(또는 식별 가능한 ID)를 수집
	const getAllData = () => (typeof grid1.getData === 'function' ? grid1.getData() : (grid1.data || []));
	const prdIds = rowKeysToDelete.map(key => {
		try {
			const row = (typeof grid1.getRow === 'function' && grid1.getRow(key)) ||
				getAllData().find(d => d && (String(d.rowKey) === String(key) || String(d.prdId) === String(key)));
			return row && row.prdId ? String(row.prdId) : String(key);
		} catch (e) {
			console.warn('삭제 ID 수집 중 오류', e);
			return String(key);
		}
	}).filter(Boolean);

		// 구분: 빈 행(또는 prdId가 없는 행)은 화면에서만 삭제하고, prdId가 있는 행만 서버에 삭제 요청
		try {
			const getAllData = () => (typeof grid1.getData === 'function' ? grid1.getData() : (grid1.data || []));
			const data = getAllData();
			// 그리드의 수정 정보에서 생성된(신규) 행들을 조회하여, 신규행은 UI에서만 삭제하도록 처리
			const modified = (typeof grid1.getModifiedRows === 'function') ? (grid1.getModifiedRows() || {}) : {};
			const createdRows = Array.isArray(modified.createdRows) ? modified.createdRows : [];
			const uiOnlyKeys = []; // 화면에서만 제거할 rowKey
			const serverPrdIds = []; // 서버에 삭제 요청할 prdId 목록
			for (const key of rowKeysToDelete) {
				// 우선 해당 키가 생성된(신규) 행인지 확인
				const isCreated = createdRows.some(r => r && (String(r.rowKey) === String(key) || String(r.prdId) === String(key)));
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
				if (allEmpty || !row || !row.prdId) {
					uiOnlyKeys.push(key);
				} else {
					serverPrdIds.push(String(row.prdId));
				}
			}

			// UI에서만 제거할 행들 삭제
			let removedUi = 0;
			if (uiOnlyKeys.length > 0) {
				for (const k of uiOnlyKeys) {
					try {
						if (typeof grid1.removeRow === 'function') { grid1.removeRow(k); removedUi++; continue; }
						if (typeof grid1.deleteRow === 'function') { grid1.deleteRow(k); removedUi++; continue; }
						const newData = data.filter(r => !(r && (String(r.rowKey) === String(k) || String(r.prdId) === String(k))));
						grid1.resetData(newData);
						removedUi++;
					} catch (e) { console.warn('UI 전용 행 삭제 실패', k, e); }
				}
			}

			// 서버에 삭제 요청 보낼 prdId가 있으면 기존 로직 수행
			if (serverPrdIds.length > 0) {
				// prdId가 있는 항목이 포함된 경우에만 삭제 확인창 표시
				if (!confirm('서버에서 실제로 삭제할 항목이 포함되어 있습니다. 선택한 항목을 삭제하시겠습니까?')) return;
				fetch('/masterData/product/delete', {
					method: 'POST',
					credentials: 'same-origin',
					headers: {
						[csrfHeader]: csrfToken,
						'Content-Type': 'application/json'
					},
					body: JSON.stringify(serverPrdIds)
				})
				.then(res => {
					
					const ct = (res.headers.get('content-type') || '').toLowerCase();
					if (ct.includes('application/json')) return res.json();
					return res.text();
				})
				.then(parsed => {
					
					// 1. HTTP 500 에러 본문인 경우 (parsed.status가 500인 경우)
				    if (parsed && parsed.status === 500) {
				        
				        // ORA-02292 오류가 포함된 500 에러인지 확인 (message 필드에서 직접 확인)
				        const errorMessage = parsed.message || parsed.trace;
				        
				        if (errorMessage && errorMessage.includes("ORA-02292")) {
				            // ✅ 요청하신 메시지를 사용자에게 표시
				            alert("⚠️ 삭제 실패: 연관된 데이터가 존재합니다. 먼저 관련(자식) 데이터를 삭제하세요.");
				            return; // 여기서 로직 종료
				        }
				        
				        // ORA-02292가 아닌 다른 500 에러인 경우
				        throw new Error(`서버 내부 오류: ${parsed.error} (${parsed.message ? parsed.message.substring(0, 50) + '...' : ''})`);
				    }
					// 3. 성공 상태 확인
				    if (parsed && parsed.status === 'success') {
				        alert('✅ 삭제가 성공적으로 완료되었습니다.');
				        productGridAllSearch();
				        return;
				    }

				    // 4. 기타 예상치 못한 응답
				    throw new Error('삭제 실패: 알 수 없는 응답 형식');
				
					// 서버 삭제 성공 시 그리드 재조회
					productGridAllSearch();
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

//원재료row 삭제: POST JSON형식으로 서버에 요청
const deleteMaterialRowBtn = document.getElementById('deleteMaterialRowBtn');
deleteMaterialRowBtn.addEventListener('click', async function() {
	console

	// 체크된 rowKey들 수집
	let rowKeysToDelete = [];
	try {
		if (typeof grid2.getCheckedRowKeys === 'function') {
			rowKeysToDelete = grid2.getCheckedRowKeys() || [];
		} else if (typeof grid2.getCheckedRows === 'function') {
			const checkedRows = grid2.getCheckedRows() || [];
			rowKeysToDelete = checkedRows.map(r => r && (r.rowKey || r.matId)).filter(Boolean);
		}
	} catch (e) {
		console.warn('체크된 행 조회 실패', e);
	}
	if (!Array.isArray(rowKeysToDelete) || rowKeysToDelete.length === 0) {
		alert('삭제할 행을 선택(체크)해주세요.');
		return;
	}
	// 간결한 방식으로 각 rowKey로부터 matId(또는 식별 가능한 ID)를 수집
	const getAllData = () => (typeof grid2.getData === 'function' ? grid2.getData() : (grid2.data || []));
	const matIds = rowKeysToDelete.map(key => {
		try {
			const row = (typeof grid2.getRow === 'function' && grid2.getRow(key)) ||
				getAllData().find(d => d && (String(d.rowKey) === String(key) || String(d.matId) === String(key)));
			return row && row.matId ? String(row.matId) : String(key);
		}
		catch (e) {
			console.warn('삭제 ID 수집 중 오류', e);
			return String(key);
		}
	}).filter(Boolean);

	if (!confirm('선택한 항목을 삭제하시겠습니까?')) return;
	fetch('/material/delete', {
		method: 'POST',
		credentials: 'same-origin',
		headers: {
			[csrfHeader]: csrfToken,
			'Content-Type': 'application/json'
		},
		body: JSON.stringify(matIds)
	})
	.then(res => {
		if (!res.ok) throw new Error(`HTTP error! status: ${res.status}`);
		const ct = (res.headers.get('content-type') || '').toLowerCase();
		if (ct.includes('application/json')) return res.json();
		return res.text();
	}
	)
	.then(parsed => {
		console.log('삭제 응답:', parsed);
		const okTexts = ['success','ok','true'];
		if (typeof parsed === 'string') {
			if (!okTexts.includes(parsed.trim().toLowerCase())) throw new Error('Unexpected response: ' + parsed);
		}
		else if (!(parsed && (parsed.status === 'success' || okTexts.includes((parsed.message||'').toString().toLowerCase())))) {
			throw new Error('삭제 실패: ' + JSON.stringify(parsed));
		}
		// 서버 삭제 성공 시 그리드 재조회
		materialGridAllSearch();
	})
	.catch(err => {
		console.error('삭제 중 오류', err);
		try { alert('삭제 중 오류가 발생했습니다. ' + (err && err.message ? err.message : '')); } catch (e) {}
	});
});



