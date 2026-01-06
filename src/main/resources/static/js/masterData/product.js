
window.onload = function () {	
	productGridAllSearch();//완제품 그리드 조회
	materialGridAllSearch();//원재료 그리드 조회
	prdItemNameList();//완제품 품목명(향수타입) 드롭다운
	prdItemTypeList();//완제품 제품유형 드롭다운
	prdUnitList();//완제품 단위 드롭다운
	prdStatusList();//완제품 제품상태 드롭다운
	matTypeList();//원재료 원재료유형 드롭다운
	matUnitList();//원재료 단위 드롭다운
}

document.querySelectorAll('button[data-bs-toggle="tab"]').forEach(tab => {
    tab.addEventListener('shown.bs.tab', function (e) {
        const targetId = e.target.getAttribute('data-bs-target');

        if (targetId === '#navs-product-tab') {//완제품탭
            grid1.refreshLayout();
			productGridAllSearch();
        } else if (targetId === '#navs-material-tab') {//원재료 탭
            grid2.refreshLayout();
			materialGridAllSearch();
        }
    });
});


let perfumeListItems = []; //완제품 품목명(향수타입) 드롭다운
let itemTypeListItems = [];//완제품 제품유형 드롭다운
let unitListItems = [];//완제품 단위 드롭다운
let statusListItems = [];//완제품 제품상태 드롭다운

let matTypeListItems = [];//원재료 원재료유형 드롭다운
let matUnitListItems = [];//원재료 단위 드롭다운

// effectiveDate 허용범위 (개월 단위)
const EFFECTIVE_DATE_MIN = 0;
const EFFECTIVE_DATE_MAX = 120; // 예: 최대 120개월(10년)




const Grid = tui.Grid;

//g-grid1 완제품(상위품번)
const grid1 = new Grid({
	  el: document.getElementById('productGrid'),
	  language: 'ko', // 한글 설정
      rowHeaders: [
			{ type: 'rowNum', header: 'No.'},
			{ type: 'checkbox'}
		],
	  columns: [
		{header: '품번' ,name: 'prdId' ,align: 'center',editor: 'text',width: 100
			,renderer:{ type: StatusModifiedRenderer}	
		}
		,{header: '품목명' ,name: 'itemName' ,align: 'center',editor: 'text',filter: "select",width: 100
			,renderer:{ type: StatusModifiedRenderer
				,options: {
					isSelect: true   // ⭐ 이걸로 구분
				}
			}
			,editor: {
				type: 'select', // 드롭다운 사용
				options: {
					listItems: perfumeListItems
				}
			}
		}
		,{header: '제품명' ,name: 'prdName' ,align: 'center',editor: 'text'
			,renderer:{ type: StatusModifiedRenderer}
		}
		,{header: '제품유형' ,name: 'prdCat' ,align: 'center',filter: "select",width: 100
			,renderer:{ type: StatusModifiedRenderer
				,options: {
					isSelect: true   // ⭐ 이걸로 구분
				}
			}
			,editor: {
				type: 'select', // 드롭다운 사용
				options: {
					listItems: itemTypeListItems
				}
			}
		}
		,{header: '단위' ,name: 'prdUnit' ,align: 'center',width:80
			,renderer:{ type: StatusModifiedRenderer
				,options: {
					isSelect: true   // ⭐ 이걸로 구분
				}
			}
			,editor: {
				type: 'select', // 드롭다운 사용
				options: {
					listItems: unitListItems
				}
			}
		}
		,{header: '단가' ,name: 'unitPrice' ,align: 'center'
			,renderer:{ type: StatusModifiedRenderer}
				,editor: {
					type: NumberOnlyEditor, // ⬅️ 클래스 이름 직접 사용
				options: {
				maxLength: 10
				}
			}
		}
        ,{header: '상태' ,name: 'prdStatus' ,align: 'center',hidden:true
			,renderer:{ type: StatusModifiedRenderer
				,options: {
					isSelect: true   // ⭐ 이걸로 구분
				}
			}
			,editor: {
				type: 'select', // 드롭다운 사용
				options: {
					// value는 실제 데이터 값, text는 사용자에게 보이는 값
					listItems: statusListItems
				}
			}
		}
		,{header: '유효일자(개월)' ,name: 'effectiveDate' ,align: 'center'
			,renderer:{ type: StatusModifiedRenderer}
			,editor: {
            	type: NumberOnlyEditor, // ⬅️ 클래스 이름 직접 사용
            	options: {
					maxLength: 10
				},
			}
		}
        ,{header: '제품상세설명' ,name: 'prdSpec' ,align: 'center',editor: 'text',width: 370
			,renderer:{ type: StatusModifiedRenderer}
		}
        ,{header: '생성자ID' ,name: 'createdId' ,align: 'center',hidden:true}
		,{header: '생성자이름' ,name: 'createdByName' ,align: 'center',hidden:true}
        ,{header: '생성일자' ,name: 'createdDate' ,align: 'center',hidden:true}
        ,{header: '수정자ID' ,name: 'updatedId' ,align: 'center',hidden:true}
		,{header: '수정자이름' ,name: 'updatedByName' ,align: 'center',hidden:true}
        ,{header: '수정일자' ,name: 'updatedDate' ,align: 'center',hidden:true}           
		,{header: '사용여부' ,name: 'useYn' ,align: 'center',filter: "select",width:83
			,renderer:{ type: StatusModifiedRenderer
				,options: {
					isSelect: false   // ⭐ 이걸로 구분
				}
			}
			,editor: {
				type: 'select', // 드롭다운 사용
				options: {
					// value는 실제 데이터 값, text는 사용자에게 보이는 값
					listItems: [
						{value: 'Y', text: '활성'},
						{value: 'N', text: '비활성'}
					]
				}
			}
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
			{ type: 'checkbox'}
		],
	    columns: [
		    {header: '원재료ID' ,name: 'matId' ,align: 'center',editor: 'text'
				,renderer:{ type: StatusModifiedRenderer}	
			}
		    ,{header: '원재료 품목명' ,name: 'matName' ,align: 'center',editor: 'text'
				,renderer:{ type: StatusModifiedRenderer}	
			}
		    ,{header: '원재료 유형' ,name: 'matType' ,align: 'center',editor: 'text',filter: "select",width: 102
				,renderer:{ type: StatusModifiedRenderer
					,options: {
					isSelect: true   
					}
				}	
				,editor: {
					type: 'select', // 드롭다운 사용
					options: {
						listItems: matTypeListItems
					}
				}
			}
		    ,{header: '단위' ,name: 'matUnit' ,align: 'center',editor: 'text',filter: "select",width:70
				,renderer:{ type: StatusModifiedRenderer
					,options: {
					isSelect: true 
					}
				}	
				,editor: {
					type: 'select', // 드롭다운 사용
					options: {
						// value는 실제 데이터 값, text는 사용자에게 보이는 값
						listItems: matUnitListItems
					}
				}
			}
	        ,{header: '유효일자(개월)' ,name: 'effectiveDate' ,align: 'center',width: 102
				,renderer:{ type: StatusModifiedRenderer}	
				,editor: {
            		type: NumberOnlyEditor 
					,options: {
						maxLength: 10
					}
				}
			}
	        ,{header: '상세설명(원재료)' ,name: 'matDesc' ,align: 'center',editor: 'text',width: 370
				,renderer:{ type: StatusModifiedRenderer}	
			}
	        ,{header: '생성자ID' ,name: 'createdId' ,align: 'center',hidden:true}
			,{header: '생성자이름' ,name: 'createdByName' ,align: 'center',hidden:true}
	        ,{header: '생성일자' ,name: 'createdDate' ,align: 'center',hidden:true}
	        ,{header: '수정자ID' ,name: 'updatedId' ,align: 'center',hidden:true}
			,{header: '수정자이름' ,name: 'updatedByName' ,align: 'center',hidden:true}
	        ,{header: '수정일시' ,name: 'updatedDate' ,align: 'center',hidden:true}
			,{header: '사용여부' ,name: 'useYn' ,align: 'center',filter: "select",width:83
				,renderer:{ type: StatusModifiedRenderer
					,options: {
						isSelect: false   // ⭐ 이걸로 구분
					}
				}
				,editor: {
					type: 'select', // 드롭다운 사용
					options: {
						// value는 실제 데이터 값, text는 사용자에게 보이는 값
						listItems: [
							{value: 'Y', text: '활성'},
							{value: 'N', text: '비활성'}
						]
					}
				}
			}          
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
	        

			let isNewRow = false;
			try {
				const modified = (typeof grid1.getModifiedRows === 'function') ? (grid1.getModifiedRows() || {}) : {};
				const createdRows = Array.isArray(modified.createdRows) ? modified.createdRows : [];
				isNewRow = createdRows.some(r => r && String(r.rowKey) === String(rowKey));
			} catch (e) {
				// 실패 시 기존 fallback 사용
				isNewRow = !prdIdValue;
			}
	        if (!isNewRow) {
	            ev.stop(); // 편집 모드 진입 차단
	            alert('기존 품번은 수정할 수 없습니다.'); 
	        }
	    }
});

grid2.on('beforeChange', (ev) => {
    const { rowKey, columnName } = ev.changes[0]; // 변경된 데이터 목록 (배열)
	if (columnName === 'matId') {
	        // 💡 핵심 수정: rowKey 대신, 현재 행의 'matId' 값을 가져옵니다.
	        const matIdValue = grid2.getValue(rowKey, 'matId');
	        
			let isNewRow = false;
			try {
				const modified = (typeof grid2.getModifiedRows === 'function') ? (grid2.getModifiedRows() || {}) : {};
				const createdRows = Array.isArray(modified.createdRows) ? modified.createdRows : [];
				isNewRow = createdRows.some(r => r && String(r.rowKey) === String(rowKey));
			} catch (e) {
				// 실패 시 기존 fallback 사용
				isNewRow = !matIdValue;
			}

	        // 기존 행일 경우 (isNewRow가 false, 즉 matIdValue 있는 경우)
	        if (!isNewRow) {
	            ev.stop(); // 편집 모드 진입 차단
	            alert('기존 원재료ID는 수정할 수 없습니다.'); 
	        }
	    }
});


function productGridAllSearch() {

	const params = {
		prdId: document.getElementById("prdId").value ?? "",
		prdName: document.getElementById("prdName").value ?? ""	
	};
	
	const queryString = new URLSearchParams(params).toString();
	fetch(apiUrl(`masterData/product/list?${queryString}`), {
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
			const camelCaseData = transformKeys(data);
			console.log("camelCaseData",camelCaseData);
			grid1.resetData(camelCaseData);
		})
		.catch(err => {
			console.error("조회오류", err);
			//grid1.resetData([]);
		
		});

}

if (!window.isAdmin) {
    grid1.disable();
	grid2.disable();
    console.log("관리자 권한 없음: 그리드 비활성화");
}

function materialGridAllSearch() {

	const params = {
		matId: document.getElementById("matId").value ?? "",
		matName: document.getElementById("matName").value ?? ""
	};
	
	const queryString = new URLSearchParams(params).toString();
	fetch(apiUrl(`material/list?${queryString}`), {
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
			
			console.log("검색데이터2:", data);
			const camelCaseData = transformKeys(data);
			console.log("camelCaseData",camelCaseData);
			grid2.resetData(camelCaseData);
		})
		.catch(err => {
			console.error("조회오류", err);
			grid2.resetData([]);
		
		});

}

const toCamelCase = (snakeCaseString) => {
  if (!snakeCaseString || typeof snakeCaseString !== 'string') {
    return snakeCaseString;
  }

  // 1. 소문자로 변환
  // 2. 언더스코어(_)를 기준으로 문자열을 분리
  // 3. reduce를 사용하여 카멜 케이스로 조합
  return snakeCaseString.toLowerCase().split('_').reduce((acc, part) => {
    // 첫 번째 파트는 그대로 사용 (created)
    if (acc === '') {
      return part;
    }
    // 두 번째 파트부터는 첫 글자를 대문자로 변환 후 뒤에 붙임 (ByName)
    return acc + part.charAt(0).toUpperCase() + part.slice(1);
  }, '');
};

const transformKeys = (data) => {
  if (Array.isArray(data)) {
    // 배열이면 배열의 모든 요소에 대해 재귀 호출
    return data.map(transformKeys);
  }

  if (data !== null && typeof data === 'object') {
    // 객체이면 키를 순회하며 변환
    const newObject = {};
    for (const key in data) {
      if (Object.prototype.hasOwnProperty.call(data, key)) {
        const newKey = toCamelCase(key);
        // 값도 객체나 배열일 수 있으므로 재귀적으로 처리
        newObject[newKey] = transformKeys(data[key]);
      }
    }
    return newObject;
  }

  // 객체나 배열이 아니면 값 그대로 반환 (문자열, 숫자, null 등)
  return data;
};

//완제품 품목명(향수타입) 드롭다운
function prdItemNameList() {
	fetch(apiUrl(`masterData/product/prdItemNameList`), {
		method: 'GET',
		headers: {
			[csrfHeader]: csrfToken,
			'Content-Type': 'application/json'
		},
	})
	.then(res => {
		if (!res.ok) throw new Error(`HTTP error! status: ${res.status}`);
		return res.json();
	})
	.then(data => {
		console.log("품목명(향수타입) 드롭다운 데이터:", data);
	
		data.forEach(item => {
			perfumeListItems.push({
				value: item.VALUE, 
				text: item.TEXT   
			});
		});
		console.log("perfumeListItems:", perfumeListItems);
		// Dropdown editor의 listItems 업데이트
		
	})
	.catch(err => {
		console.error('품목명(향수타입) 드롭다운 조회 오류', err);
	});

}

//완제품 제품유형 드롭다운 조회
function prdItemTypeList() {
	fetch(apiUrl(`masterData/product/prdItemTypeList`), {
		method: 'GET',
		headers: {
			[csrfHeader]: csrfToken,
			'Content-Type': 'application/json'
		},
	})
	.then(res => {
		if (!res.ok) throw new Error(`HTTP error! status: ${res.status}`);
		return res.json();
	})
	.then(data => {
		console.log("제품유형 드롭다운 데이터:", data);
		data.forEach(item => {
			itemTypeListItems.push({
				value: item.VALUE,
				text: item.TEXT
			});
		});
		console.log("itemTypeListItems:", itemTypeListItems);
		// Dropdown editor의 listItems 업데이트
	})

}
//완제품 단위 드롭다운 조회
function prdUnitList() {
	fetch(apiUrl(`masterData/product/prdUnitList`), {
		method: 'GET',
		headers: {
			[csrfHeader]: csrfToken,
			'Content-Type': 'application/json'
		},
	})
	.then(res => {
		if (!res.ok) throw new Error(`HTTP error! status: ${res.status}`);
		return res.json();
	})
	.then(data => {
		console.log("단위 드롭다운 데이터:", data);
		data.forEach(item => {
			unitListItems.push({
				value: item.VALUE,
				text: item.TEXT
			});
		});
		console.log("unitListItems:", unitListItems);
		// Dropdown editor의 listItems 업데이트
	})
}

//완제품 제품상태 드롭다운
function prdStatusList() {
	fetch(apiUrl(`masterData/product/prdStatusList`), {
		method: 'GET',
		headers: {
			[csrfHeader]: csrfToken,
			'Content-Type': 'application/json'
		},
	})
	.then(res => {
		if (!res.ok) throw new Error(`HTTP error! status: ${res.status}`);
		return res.json();
	})
	.then(data => {
		console.log("제품상태 드롭다운 데이터:", data);
		data.forEach(item => {
			statusListItems.push({
				value: item.VALUE,
				text: item.TEXT
			});
		});
		console.log("statusListItems:", statusListItems);
		// Dropdown editor의 listItems 업데이트
	})
}

//원재료 원재료 유형 드롭다운
function matTypeList() {
	fetch(apiUrl(`material/matTypeList`), {
		method: 'GET',
		headers: {
			[csrfHeader]: csrfToken,
			'Content-Type': 'application/json'
		},
	})
	.then(res => {
		if (!res.ok) throw new Error(`HTTP error! status: ${res.status}`);
		return res.json();
	})
	.then(data => {
		console.log("원재료 원재료유형 드롭다운 데이터:", data);
		data.forEach(item => {
			matTypeListItems.push({
				value: item.VALUE,
				text: item.TEXT
			});
		});
		console.log("matTypeListItems:", matTypeListItems);
		// Dropdown editor의 listItems 업데이트
	})
}

//원재료 단위 드롭다운
function matUnitList() {
	fetch(apiUrl(`material/matUnitList`), {
		method: 'GET',
		headers: {
			[csrfHeader]: csrfToken,
			'Content-Type': 'application/json'
		},
	})
	.then(res => {
		if (!res.ok) throw new Error(`HTTP error! status: ${res.status}`);
		return res.json();
	})
	.then(data => {
		console.log("원재료 원재료 단위 드롭다운 데이터:", data);
		data.forEach(item => {
			matUnitListItems.push({
				value: item.VALUE,
				text: item.TEXT
			});
		});
		console.log("matUnitListItems:", matUnitListItems);
		// Dropdown editor의 listItems 업데이트
	})
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

	if (updatedRows.length === 0 && createdRows.length === 0) {
		alert('수정된 내용이 없습니다.');
		return;
	}

	// 저장 전 필수값 검사: prdId는 반드시 필요합니다 (신규 행도 먼저 품번 입력 필요)
	const requiredFields = ['prdId', 'itemName', 'prdName'];
	const fieldLabels = { prdId: '품번', itemName: '품목명', prdName: '제품명' };
	const rowsToCheck = [...createdRows, ...updatedRows];
	const gd = (typeof grid1.getData === 'function') ? grid1.getData() : (grid1.data || []);
	// rowsToCheck를 순회하여 필수값이 누락된 행을 수집합니다.
	// - 빈 행은 무시
	// - 필수 필드(requiredFields)를 검사하여 비어있으면 missing에 기록
	// - 그리드 원본 데이터(gd)에서 해당 행의 No.(index)를 찾아 id로 사용
	// - id가 숫자형이면 No. 표시에 사용될 수 있도록 저장
	const invalidRows = rowsToCheck.reduce((acc, row, idx) => {
		// 행이 없으면 건너뜀
		if (!row) return acc;
		// 각 필수 필드가 비어있는지 확인 (null/undefined 또는 공백 문자열)
		const missing = requiredFields.filter(f => !String(row[f] ?? '').trim());
		// 누락된 필드가 없으면 통과
		if (!missing.length) return acc;
		// 화면 그리드 데이터에서 해당 행의 No. 위치를 찾음(일치하는 prdId 또는 rowKey 기준)
		const fi = gd.findIndex(d => d && ((row.prdId && String(d.prdId) === String(row.prdId)) || (row.rowKey && String(d.rowKey) === String(row.rowKey))));
		// 찾은 인덱스가 있으면 1-based 번호를 id로, 없으면 prdId/rowKey 또는 임시 '#idx' 사용
		acc.push({ id: fi >= 0 ? String(fi + 1) : (row.prdId || row.rowKey || `#${idx + 1}`), missing });
		return acc;
	}, []);

	if (invalidRows.length > 0) {
		// 숫자형 id를 가진 항목을 기준으로 오름차순 정렬(그리드의 No. 순서)
		const sorted = invalidRows.slice().sort((a, b) => {
			const na = Number(a.id);
			const nb = Number(b.id);
			if (!isNaN(na) && !isNaN(nb)) return na - nb;
			return String(a.id).localeCompare(String(b.id));
		});

		const lines = sorted.map(r => {
			console.log("누락된 r:", r);
			const missNames = r.missing.map(m => fieldLabels[m] || m).join(', ');
			const displayId = (!isNaN(Number(r.id))) ? `No. ${Number(r.id)}` : r.id;
			return `${displayId} (누락: ${missNames})`;
		});
		alert('다음 행에 필수값이 비어 있어 저장할 수 없습니다.\n특히 신규 행은 먼저 품번을 입력해 주세요.\n' + lines.join('\n'));
		return;
	}

	// 단가, 유효기간 범위 검사 — 유효하지 않으면 즉시 저장 중단
	for (let idx = 0; idx < rowsToCheck.length; idx++) {
		const row = rowsToCheck[idx];
		if (!row) continue;
		if (row.effectiveDate != null && String(row.effectiveDate).trim() !== '') {
			const ed = Number(row.effectiveDate);
			if (isNaN(ed) || ed < EFFECTIVE_DATE_MIN || ed > EFFECTIVE_DATE_MAX) {
				alert('유효일자(개월)는 ' + EFFECTIVE_DATE_MIN + ' ~ ' + EFFECTIVE_DATE_MAX + ' 범위 내의 값이어야 합니다. (행: ' + (idx+1) + ')');
				return;
			}
		}

		if (row.unitPrice != null && String(row.unitPrice).trim() !== '') {
			const up = Number(row.unitPrice);
			if (isNaN(up) || up < 0 || up > 1000000000) {
				alert('단가는 0 ~ 1,000,000,000 범위 내의 값이어야 합니다. (행: ' + (idx+1) + ')');
				return;
			}
		}
	}

	fetch(apiUrl(`masterData/product/save`), {
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
			return res.text().then(t => { throw new Error(`HTTP ${res.status}: ${t || res.statusText}`); });
		}
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

		if (!isSuccess(parsed)) {
			try { alert(parsed); } catch (e) {}
			return;
		}
		productGridAllSearch();
	})
	.catch(err => {
		console.error('저장 오류', err && err.message ? err.message : err);
		try { alert(err && err.message ? err.message : err); } catch (e) {}
	});
});

//원재료 row 저장: POST JSON형식으로 서버에 요청
const saveMaterialRowBtn = document.getElementById('saveMaterialRowBtn');
saveMaterialRowBtn.addEventListener('click', function() {
	const modifiedData = (typeof grid2.getModifiedRows === 'function') ? (grid2.getModifiedRows() || {}) : {};
	const updatedRows = Array.isArray(modifiedData.updatedRows) ? modifiedData.updatedRows : [];
	let createdRows = Array.isArray(modifiedData.createdRows) ? modifiedData.createdRows : [];

	if (updatedRows.length === 0 && createdRows.length === 0) {
		alert('수정된 내용이 없습니다.');
		return;
	}
	// 1) createdRows에서 '완전 빈 행' 제거
	const isRowEmpty = (row) => {
		if (!row) return true;
		const vals = Object.values(row || {});
		if (vals.length === 0) return true;
		return vals.every(v => v === null || v === undefined || (typeof v === 'string' && v.toString().trim() === ''));
	};
	try { createdRows = createdRows.filter(r => !isRowEmpty(r)); } catch (e) { /* ignore */ }

	const rowsToCheck = [...createdRows, ...updatedRows];
	const gd = (typeof grid2.getData === 'function') ? grid2.getData() : (grid2.data || []);
	// matId와 matName 둘 다 필수 검사(완전 빈 행은 무시)
	const missingRows = rowsToCheck.reduce((acc, row, i) => {
		try {
			if (!row) return acc;
			// 행이 완전히 비어있으면 무시
			if (!Object.values(row || {}).some(v => v != null && String(v).trim() !== '')) return acc;
			const missing = [];
			if (!(row?.matId != null && String(row.matId).trim() !== '')) missing.push('matId');
			if (!(row?.matName != null && String(row.matName).trim() !== '')) missing.push('matName');
			if (missing.length) {
				const f = gd.findIndex(d => d && ((row.matId && String(d.matId) === String(row.matId)) || (row.rowKey && String(d.rowKey) === String(row.rowKey))));
				acc.push({ id: f >= 0 ? String(f + 1) : (row?.rowKey || `#${i+1}`), missing });
			}
			return acc;
		} catch {
			acc.push({ id: `#${i+1}`, missing: ['matId','matName'] });
			return acc;
		}
	}, []);
	if (missingRows.length) {
		// 정렬 및 메시지 생성
		missingRows.sort((a,b) => { const A = Number(a.id), B = Number(b.id); return (!isNaN(A) && !isNaN(B)) ? A - B : String(a.id).localeCompare(String(b.id)); });
		const labels = { matId: '원재료ID(matId)', matName: '원재료명(matName)' };
		const lines = missingRows.map(r => {
			const displayId = Number.isFinite(Number(r.id)) ? `No. ${Number(r.id)}` : r.id;
			const missNames = r.missing.map(k => labels[k] || k).join(', ');
			return `${displayId} (누락: ${missNames})`;
		});
		alert('다음 행에 필수값이 비어 있어 저장할 수 없습니다.\n' + lines.join('\n'));
		return;
	}
	// 유효기간 범위 검사 — 유효하지 않으면 즉시 저장 중단
	for (let idx = 0; idx < rowsToCheck.length; idx++) {
		const row = rowsToCheck[idx];
		if (!row) continue;
		if (row.effectiveDate != null && String(row.effectiveDate).trim() !== '') {
			const ed = Number(row.effectiveDate);
			if (isNaN(ed) || ed < EFFECTIVE_DATE_MIN || ed > EFFECTIVE_DATE_MAX) {
				alert('유효일자(개월)는 ' + EFFECTIVE_DATE_MIN + ' ~ ' + EFFECTIVE_DATE_MAX + ' 범위 내의 값이어야 합니다. (행: ' + (idx+1) + ')');
				return;
			}
		}
	}

	fetch(apiUrl(`material/save`), {
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
			return res.text().then(t => { throw new Error(`HTTP ${res.status}: ${t || res.statusText}`); });
		}
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

		if (!isSuccess(parsed)) {
			try { alert(parsed); } catch (e) {}
			return;
		}
		console.log('저장 성공:', parsed);
		materialGridAllSearch();
	})
	.catch(err => {
		console.error('저장 오류', err.message);
		alert('오류: ' + err.message);
		
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
				// // prdId가 있는 항목이 포함된 경우에만 삭제 확인창 표시
				// if (!confirm('서버에서 실제로 삭제할 항목이 포함되어 있습니다. 선택한 항목을 삭제하시겠습니까?')) return;
				// fetch('/masterData/product/delete', {
				// 	method: 'POST',
				// 	credentials: 'same-origin',
				// 	headers: {
				// 		[csrfHeader]: csrfToken,
				// 		'Content-Type': 'application/json'
				// 	},
				// 	body: JSON.stringify(serverPrdIds)
				// })
				// .then(res => {
					
				// 	const ct = (res.headers.get('content-type') || '').toLowerCase();
				// 	if (ct.includes('application/json')) return res.json();
				// 	return res.text();
				// })
				// .then(parsed => {
					
				// 	// 3. 성공 상태 확인
				//     if (parsed && parsed.status === 'success') {
				//         alert('✅ 삭제가 성공적으로 완료되었습니다.');
				//         productGridAllSearch();
				//         return;
				//     }

				//     // 4. 기타 예상치 못한 응답
				//     throw new Error('삭제 실패: 알 수 없는 응답 형식');
				
				// 	// 서버 삭제 성공 시 그리드 재조회
				// 	productGridAllSearch();
				// })
				// .catch(err => {
				// 	console.error('삭제 중 오류', err);
				// 	try { alert('삭제 중 오류가 발생했습니다. ' + (err && err.message ? err.message : '')); } catch (e) {}
				// });
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


	// 체크된 rowKey들 수집
	let rowKeysToDelete = [];
	try {
		if (typeof grid2.getCheckedRowKeys === 'function') {
			rowKeysToDelete = grid2.getCheckedRowKeys() || [];
		} else if (typeof grid2.getCheckedRows === 'function') {
			const checkedRows = grid2.getCheckedRows() || [];
			rowKeysToDelete = checkedRows.map(r => r && (r.rowKey || r.matId)).filter(Boolean);
		}else  {
			// 그리드 빈행 제거
			console.log('체크된 행 키:', rowKeysToDelete);

			rowKeysToDelete.forEach((key, i) => {
				grid2.deleteRow(rowKeysToDelete[i]);
			});

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
		} catch (e) {
			console.warn('삭제 ID 수집 중 오류', e);
			return String(key);
		}
	}).filter(Boolean);

		// 구분: 빈 행(또는 matId가 없는 행)은 화면에서만 삭제하고, matId가 있는 행만 서버에 삭제 요청
		try {
			const getAllData = () => (typeof grid2.getData === 'function' ? grid2.getData() : (grid2.data || []));
			const data = getAllData();
			// 그리드의 수정 정보에서 생성된(신규) 행들을 조회하여, 신규행은 UI에서만 삭제하도록 처리
			const modified = (typeof grid2.getModifiedRows === 'function') ? (grid2.getModifiedRows() || {}) : {};
			const createdRows = Array.isArray(modified.createdRows) ? modified.createdRows : [];
			const uiOnlyKeys = []; // 화면에서만 제거할 rowKey
			const serverMatIds = []; // 서버에 삭제 요청할 matId 목록
			for (const key of rowKeysToDelete) {
				// 우선 해당 키가 생성된(신규) 행인지 확인
				const isCreated = createdRows.some(r => r && (String(r.rowKey) === String(key) || String(r.matId) === String(key)));
				if (isCreated) {
					uiOnlyKeys.push(key);
					continue;
				}
				let row = null;
				if (typeof grid2.getRow === 'function') row = grid2.getRow(key);
				if (!row) row = data.find(d => d && (String(d.rowKey) === String(key) || String(d.matId) === String(key)));
				// 빈 행 판단: 모든 필드가 비어있거나 matId가 없으면 UI에서만 삭제
				const vals = row ? Object.values(row) : [];
				const allEmpty = !row || vals.length === 0 || vals.every(v => v === null || v === undefined || (typeof v === 'string' && v.trim() === ''));
				if (allEmpty || !row || !row.matId) {
					uiOnlyKeys.push(key);
				} else {
					serverMatIds.push(String(row.matId));
				}
			}

			// UI에서만 제거할 행들 삭제
			let removedUi = 0;
			if (uiOnlyKeys.length > 0) {
				for (const k of uiOnlyKeys) {
					try {
						if (typeof grid2.removeRow === 'function') { grid2.removeRow(k); removedUi++; continue; }
						if (typeof grid2.deleteRow === 'function') { grid2.deleteRow(k); removedUi++; continue; }
						const newData = data.filter(r => !(r && (String(r.rowKey) === String(k) || String(r.matId) === String(k))));
						grid2.resetData(newData);
						removedUi++;
					} catch (e) { console.warn('UI 전용 행 삭제 실패', k, e); }
				}
			}

			// 서버에 삭제 요청 보낼 matId가 있으면 기존 로직 수행
			if (serverMatIds.length > 0) {
				// if (!confirm('선택한 항목을 삭제하시겠습니까?')) return;
				// fetch('/material/delete', {
				// 	method: 'POST',
				// 	credentials: 'same-origin',
				// 	headers: {
				// 		[csrfHeader]: csrfToken,
				// 		'Content-Type': 'application/json'
				// 	},
				// 	body: JSON.stringify(matIds)
				// })
				// .then(res => {
				// 	if (!res.ok) throw new Error(`HTTP error! status: ${res.status}`);
				// 	const ct = (res.headers.get('content-type') || '').toLowerCase();
				// 	if (ct.includes('application/json')) return res.json();
				// 	return res.text();
				// }
				// )
				// .then(parsed => {
				// 	console.log('삭제 응답:', parsed);
				// 	const okTexts = ['success','ok','true'];
				// 	if (typeof parsed === 'string') {
				// 		if (!okTexts.includes(parsed.trim().toLowerCase())) throw new Error('Unexpected response: ' + parsed);
				// 	}
				// 	else if (!(parsed && (parsed.status === 'success' || okTexts.includes((parsed.message||'').toString().toLowerCase())))) {
				// 		throw new Error('삭제 실패: ' + JSON.stringify(parsed));
				// 	}
				// 	// 서버 삭제 성공 시 그리드 재조회
				// 	materialGridAllSearch();
				// })
				// .catch(err => {
				// 	console.error('삭제 중 오류', err);
				// 	try { alert('삭제 중 오류가 발생했습니다. ' + (err && err.message ? err.message : '')); } catch (e) {}
				// });
			} else {
				if (removedUi > 0) alert('추가한 행을 화면에서만 삭제했습니다. (DB에는 반영되지 않음)');
			}
		} catch (e) {
			console.error('삭제 처리 중 오류', e);
			try { alert('삭제 처리 중 오류가 발생했습니다. ' + (e && e.message ? e.message : '')); } catch (err) {}
		}
});



