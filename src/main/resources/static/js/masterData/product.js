// 전역변수 설정
let activeType = 'PROD' // 현재 그리드 타입 설정
let mainGrid = null;
let newRowKeys = []; 


// 문서시작
document.addEventListener('DOMContentLoaded',async () => {
	initTabEvent();
	initMainGrid();
	await loadActiveGrid();
	initButtons();
})


// 탭 액티브 토글
function initTabEvent() {
    const tabs = document.querySelectorAll('.nav-link[data-grid-type]');

    tabs.forEach(tab => {
        tab.addEventListener('click', (e) => {
            e.preventDefault();

            // 기존 active 제거
            tabs.forEach(t => t.classList.remove('active'));

            // 현재 클릭한 탭에 active 추가
            tab.classList.add('active');

            // 타입 전환 (나중에 grid 스위칭에서 사용)
            const type = tab.getAttribute('data-grid-type'); // 'PROD' / 'MAT'
            if (type) {
                activeType = type;// 전역 상태
                updateTitle(); // 제목 변경
                loadActiveGrid(); // 그리드 갈아끼우기
            }
        });
    });
}

// 그리드 제목변경
function updateTitle() {
    const titleEl = document.getElementById('gridTitle');
    if (!titleEl) return;
    titleEl.textContent = activeType === 'PROD' ? '완제품' : '원재료';
}

// 그리드 초기화
function initMainGrid() {
    const Grid = tui.Grid;

    mainGrid = new Grid({
        el: document.getElementById('grid'),
        rowHeaders: [
            { type: 'rowNum', header: 'No.' },
            { type: 'checkbox' }
        ],
        // 컬럼은 비워두고 type별로 setColumns에서 설정
        columns: [],
        data: [],
        bodyHeight: 500,
        columnOptions: { resizable: true },
        pageOptions: { useClient: true, perPage: 20 }
    });
}

// 상품컬럼 세팅
function setProdColumns() {
    mainGrid.setColumns([
        {
            header: '품번',
            name: 'prdId',
            align: 'center',
            editor: 'text',
            width: 100,
			validation: { 
			    required: true,  // 필수 입력
			    unique: true     // 컬럼 내 유일성
			},
            renderer: { type: StatusModifiedRenderer }
        },
        {
            header: '품목명',
            name: 'itemName',
            align: 'center',
            width: 100,
			validation: { 
				required: true,  // 필수 입력
			},
            filter: 'select',
            renderer: { type: StatusModifiedRenderer },
            editor: {
                type: 'select',
                options: {
                    listItems: [
						{ text: '고체향수', value: 'SOLID' },   // 고체 = SOLID
						{ text: '액체향수', value: 'LIQUID' }  // 액체 = LIQUID
                    ]
                }
            }
        },
        {
            header: '제품명',
            name: 'prdName',
            align: 'center',
			minWidth: 200,
			validation: { 
				required: true,  // 필수 입력
			},
            editor: 'text',
            renderer: { type: StatusModifiedRenderer }
        },
        {
            header: '제품유형',
            name: 'prdCat',
            align: 'center',
            minWidth: 100,
			validation: { 
				required: true,  // 필수 입력
			},
            filter: 'select',
            renderer: { type: StatusModifiedRenderer },
            editor: {
                type: 'select',
                options: {
                    listItems: [
                        { text: '완제품', value: 'FINISHED_GOODS' },
                        { text: '반제품', value: 'SEMI_FINISHED_GOODS' }
                    ]
                }
            }
        },
        {
            header: '단위',
            name: 'prdUnit',
            align: 'center',
			validation: { 
				required: true,  // 필수 입력
			},
            renderer: { type: StatusModifiedRenderer },
            editor: {
                type: 'select',
                options: {
                    listItems: [
                        { text: 'g', value: 'g' },
                        { text: 'ml', value: 'ml' },
                        { text: 'EA', value: 'EA' }
                    ]
                }
            }
        },
        {
            header: '단가',
            name: 'unitPrice',
            align: 'center',
            editor: 'text',
			validation: { 
				required: true,  // 필수 입력
				dataType:'number',
				min: 0
			},
            renderer: { type: StatusModifiedRenderer }
        },
        {
            header: '상태',
            name: 'prdStatus',
            align: 'center',
            renderer: { type: StatusModifiedRenderer },
			validation: { 
				required: true,  // 필수 입력
			},
            editor: {
                type: 'select',
                options: {
                    listItems: [
                        { text: '활성', value: 'ACTIVE' },
                        { text: '비활성', value: 'INACTIVE' },
                    ]
                }
            }
        },
        {
            header: '유효일자',
            name: 'effectiveDate',
            align: 'center',
            editor: 'text',
			validation: { 
				required: true,  // 필수 입력
				dataType:'number',
				min: 0
			},
            renderer: { type: StatusModifiedRenderer }
        },
        {
            header: '제품상세설명',
            name: 'prdSpec',
            align: 'center',
            editor: 'text',
            width: 370,
            renderer: { type: StatusModifiedRenderer }
        },
//        { header: '생성자ID', name: 'createdId', align: 'center', renderer: { type: StatusModifiedRenderer } },
        { header: '생성일시', name: 'createdDate', align: 'center', renderer: { type: StatusModifiedRenderer } },
//        { header: '수정자ID', name: 'updatedId', align: 'center', renderer: { type: StatusModifiedRenderer } },
        { header: '수정일시', name: 'updatedDate', align: 'center', renderer: { type: StatusModifiedRenderer } }
    ]);
}

// 원자재 컬럼 세팅
function setMatColumns() {
    mainGrid.setColumns([
        {
            header: '원재료ID',
            name: 'matId',
            align: 'center',
            editor: 'text',
			validation: { 
			    required: true,  // 필수 입력
			    unique: true     // 컬럼 내 유일성
			},
            renderer: { type: StatusModifiedRenderer }
        },
        {
            header: '원재료 품목명',
            name: 'matName',
            align: 'center',
            editor: 'text',			
			validation: { 
			    required: true,  // 필수 입력
			},
            renderer: { type: StatusModifiedRenderer }
        },
        {
            header: '원재료 유형',
            name: 'matType',
            align: 'center',
			validation: { 
			    required: true,  // 필수 입력
			},
            filter: 'select',
            renderer: { type: StatusModifiedRenderer },
            editor: {
                type: 'select',
                options: {
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
        },
        {
            header: '단위',
            name: 'matUnit',
            align: 'center',
			validation: { 
			    required: true,  // 필수 입력
			},
            filter: 'select',
            renderer: { type: StatusModifiedRenderer },
            editor: {
                type: 'select',
                options: {
                    listItems: [
                        { text: 'g', value: 'g' },
                        { text: 'ml', value: 'ml' },
                        { text: 'EA', value: 'EA' },
                        { text: 'BOX', value: 'Box' }
                    ]
                }
            }
        },
        {
            header: '유효일자',
            name: 'effectiveDate',
            align: 'center',
//			validation: { 
//				dataType:'number',
//				min: 0
//			},
            editor: 'text',
            renderer: { type: StatusModifiedRenderer }
        },
        {
            header: '상세설명(원재료)',
            name: 'matDesc',
            align: 'center',
            editor: 'text',
            width: 280,
            renderer: { type: StatusModifiedRenderer }
        },
//        { header: '생성자ID', name: 'createdId', align: 'center', renderer: { type: StatusModifiedRenderer } },
        { header: '생성일자', name: 'createdDate', align: 'center', renderer: { type: StatusModifiedRenderer } },
//        { header: '수정자ID', name: 'updatedId', align: 'center', renderer: { type: StatusModifiedRenderer } },
        { header: '수정일시', name: 'updatedDate', align: 'center', renderer: { type: StatusModifiedRenderer } },
        {
            header: '활성',
            name: 'useYn',
            align: 'center',
			validation: { 
			    required: true,  // 필수 입력
			},
            filter: 'select',
            renderer: { type: StatusModifiedRenderer },
            editor: {
                type: 'select',
                options: {
                    listItems: [
                        { text: '활성', value: 'Y' },
                        { text: '비활성', value: 'N' },
                    ]
                }
            }
        }
    ]);
}

// 활성중인 그리드 로드
async function loadActiveGrid() {
	newRowKeys = [];
	
    if (activeType === 'PROD') {
        setProdColumns();
        await getProdMstData().then(data => {
			mainGrid.resetData(data || [])
			// 기존행 PK 전부 잠금
			mainGrid.getData().forEach(row => {
			    mainGrid.disableCell(row.rowKey, 'prdId');
			});
		});
    } else {
        setMatColumns();
        await getMatMstData().then(data => {
			console.log("matData : ", data);
			mainGrid.resetData(data || [])
			// 기존행 PK 전부 잠금
			mainGrid.getData().forEach(row => {
			    mainGrid.disableCell(row.rowKey, 'matId');
			});
		});
    }
}

// 완제품 마스터 조회
async function getProdMstData() {
    return await fetch('/masterData/product/list', {
        method: 'GET',
        headers: {
            [csrfHeader]: csrfToken,
            'Content-Type': 'application/json'
        }
    })
    .then(res => {
        if (!res.ok) {
            throw new Error(`HTTP error! status: ${res.status}`);
        }

        const contentType = res.headers.get('content-type') || '';
        if (!contentType.includes('application/json')) {
            // 204나 빈 응답 방어
            if (res.status === 204) {
                return [];
            }
            return res.text().then(text => {
                throw new Error(`Expected JSON but received: ${text.substring(0, 100)}...`);
            });
        }
        return res.json();
    })
    .catch(err => {
        console.error('완제품 조회 오류', err);
        return [];
    });
}

// 원자재 마스터 조회
async function getMatMstData() {
    return await fetch('/material/list', {
        method: 'GET',
        headers: {
            [csrfHeader]: csrfToken,
            'Content-Type': 'application/json'
        }
    })
    .then(res => {
        if (!res.ok) {
            throw new Error(`HTTP error! status: ${res.status}`);
        }

        const contentType = res.headers.get('content-type') || '';
        if (!contentType.includes('application/json')) {
            if (res.status === 204) {
                return [];
            }
            return res.text().then(text => {
                throw new Error(`Expected JSON but received: ${text.substring(0, 100)}...`);
            });
        }

        return res.json();
    })
    .catch(err => {
        console.error('원재료 조회 오류', err);
        return [];
    });
}

// 그리드 추가 삭제 저장 버튼 이벤트설정
function initButtons() {
    const addBtn = document.getElementById('addRowBtn');
    const saveBtn = document.getElementById('saveRowBtn');
    const deleteBtn = document.getElementById('deleteRowBtn');

    addBtn.addEventListener('click', onAddRow);
    saveBtn.addEventListener('click', onSaveRows);
    deleteBtn.addEventListener('click', onDeleteRows);
}

// Pk 수정가능상태 업데이트
function updatePkEditableState() {
	const pkColumn = activeType === 'PROD' ? 'prdId' : 'matId'
	const rows = mainGrid.getData();
	
	// newRowKey에 해당하는 row는 수정가능, 아닌키는 수정불가능
	rows.forEach(row => {
		if(newRowKeys.includes(row.rowKey)) {
			mainGrid.enableCell(row.rowKey, pkColumn);
		} else {
			mainGrid.disableCell(row.rowKey, pkColumn)
		}
	});
}

// 추가 버튼을 눌러 그리드 행추가
function onAddRow() {
    // 맨 위에 빈 행 추가 (focus 옵션으로 커서도 이동)
    mainGrid.prependRow({}, { focus: true });
	
	// 방금 추가된 행의 rowKey 구하기
	const newRow = mainGrid.getRowAt(0);   // [web:12]
	if (!newRow) return;
	
	newRowKeys.push(newRow.rowKey);
	
	updatePkEditableState();
}

// 선택한 그리드 행 삭제
function onDeleteRows() {
    const checkedRowKeys = mainGrid.getCheckedRowKeys();
    if (!checkedRowKeys || checkedRowKeys.length === 0) {
        alert('삭제할 행을 선택(체크)해주세요.');
        return;
    }

	if (!confirm('선택한 행을 삭제/비활성화 하시겠습니까?')) {
	    return;
	}
	
	const pkColumn = activeType === 'PROD' ? 'prdId' : 'matId';
	let deleteCount = 0;
	let inactiveCount = 0;
	
    // UI에서만 삭제 (DB 반영은 저장 시에 한 번에 처리)
    checkedRowKeys
        .sort((a, b) => b - a)  // 아래 행부터 지우면 안전 [web:11]
        .forEach(rowKey => {
            const row = mainGrid.getRow(rowKey);
			if (!row) return;

			// newRowKeys에 포함된 rowKey 완전 삭제
			if (newRowKeys.includes(rowKey)) {
				mainGrid.removeRow(rowKey);
				deleteCount++;
			
				// 배열에서 해당 rowKey 제거
				newRowKeys = newRowKeys.filter(k => k !== rowKey);
				return;
			}
			
			// 그외
			if (activeType === 'PROD') {
				// 완제품: 상태 컬럼을 INACTIVE로
				mainGrid.setValue(rowKey, 'prdStatus', 'INACTIVE');
			} else {
				// 원재료: useYn을 N으로
				mainGrid.setValue(rowKey, 'useYn', 'N');
			}
		
			inactiveCount++;
	});
		
	alert(`${deleteCount}건 삭제, ${inactiveCount}건 비활성화 처리되었습니다.`);
	// pk 수정가능상태설정
	updatePkEditableState();
}

// 변경사항 저장(추가, 수정, 삭제)
function onSaveRows() {
	// 편집중인셀 커밋
	mainGrid.finishEditing();
	// 유효성검사시행
	const invalidRows = mainGrid.validate();
	
	if (invalidRows.length > 0) {
//	    alert(`${invalidRows.length}개 행의 유효성 검사를 수정해주세요.`);
	    alert(`수정및 추가된 행에서 잘못된 입력값이 발견되었습니다.`);
	    // 첫 번째 오류 셀에 포커스
	    const firstError = invalidRows[0];
	    mainGrid.focus(firstError[0], firstError[1]);
	    return;
	}
	
	// 수정된 로우 정보
    const modified = mainGrid.getModifiedRows(); 
    const { createdRows, updatedRows, deletedRows } = modified;
	
	// 추가,수정,삭제된 로우가 없을때 
    if (
        (!createdRows || createdRows.length === 0) &&
        (!updatedRows || updatedRows.length === 0) &&
        (!deletedRows || deletedRows.length === 0)
    ) {
        alert('수정된 내용이 없습니다.');
        return;
    }
	
	// 활성그리드에 따라 url적용
    const url = activeType === 'PROD'
        ? '/masterData/product/save'
        : '/material/save';

    fetch(url, {
        method: 'POST',
        credentials: 'same-origin',
        headers: {
            [csrfHeader]: csrfToken,
            'Content-Type': 'application/json'
        },
        body: JSON.stringify(modified)
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
        // 응답 구조에 맞게 성공 판정
        const okTexts = ['success', 'ok', 'true'];
        let isSuccess = false;

        if (typeof parsed === 'string') {
            isSuccess = okTexts.includes(parsed.trim().toLowerCase());
        } else if (parsed) {
            const status = (parsed.status || '').toString().toLowerCase();
            const msg = (parsed.message || '').toString().toLowerCase();
            isSuccess =
                status === 'success' ||
                okTexts.includes(msg) ||
                msg.includes('success');
        }

        if (!isSuccess) {
            throw new Error('Unexpected response: ' + JSON.stringify(parsed));
        }

        alert('저장이 완료되었습니다.');
        // 타입에 맞게 다시 조회
        loadActiveGrid();
    })
    .catch(err => {
        console.error('저장 중 오류', err);
        alert('저장 중 오류가 발생했습니다: ' + (err.message || ''));
    });
}









