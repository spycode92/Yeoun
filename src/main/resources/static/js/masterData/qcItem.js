window.onload = function () {	
	qcItemGridAllSearch();//품질항목기준
}


const Grid = tui.Grid;

//g-grid1 품질항목기준
const grid1 = new Grid({
	  el: document.getElementById('qcItemGrid'), 
      rowHeaders: ['rowNum','checkbox'],
	  columns: [

		{header: 'QC항목ID' ,name: 'qcItemId' ,align: 'center'}
		,{header: '항목명' ,name: 'itemName' ,align: 'center'}
		,{header: '대상구분' ,name: 'targetType' ,align: 'center',width: 110,filter: "select"
			,renderer:{ type: StatusModifiedRenderer}
		}
		,{header: '단위' ,name: 'unit' ,align: 'center'
			,renderer:{ type: StatusModifiedRenderer}
		}
		,{header: '기준 텍스트' ,name: 'stdText' ,align: 'center',width: 230}
		,{header: 'MIN' ,name: 'minValue' ,align: 'center'}
        ,{header: 'MAX' ,name: 'maxValue' ,align: 'center'}
		,{header: '사용여부' ,name: 'useYn' ,align: 'center'
			,renderer:{ type: StatusModifiedRenderer}
		}  
		,{header: '정렬순서' ,name: 'sortOrder' ,align: 'center',hidden: true}
		,{header: '생성자id' ,name: 'createdId' ,align: 'center',hidden: true} 
		,{header: '생성자이름' ,name: 'createdByName' ,align: 'center',hidden: true}
		,{header: '생성일시' ,name: 'createdDate' ,align: 'center',hidden: true}  
		,{header: '수정자id' ,name: 'updatedId' ,align: 'center',hidden: true}  
		,{header: '수정자이름' ,name: 'updatedByName' ,align: 'center',hidden: true}
		,{header: '수정일시' ,name: 'updatedDate' ,align: 'center',hidden: true}
		,{
			header: '상세보기', name: 'view_details', align: 'center', width: 100
			, formatter: (rowInfo) => {
				return `<button type='button' class='btn btn-primary btn-sm' data-row-key='${rowInfo.row.rowKey}'>상세</button>`;
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

//qcitem  품질항목관리 조회
function qcItemGridAllSearch(){

	const params = {
		qcItemId: document.getElementById("qcItemId").value ?? "",
	};
	const queryString = new URLSearchParams(params).toString();
	fetch(apiUrl(`masterData/qc_item/list?${queryString}`), {
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

grid1.on("click", async (ev) => {

	const target = ev.nativeEvent.target;
	// const targetElement = ev.nativeEvent.target; 이 줄이 빠진 경우
	if (ev.targetType === 'cell' && target.tagName === 'BUTTON') {
		console.log('Button in cell clicked, rowKey:', ev.rowKey);
		
		const rowData = grid1.getRow(ev.rowKey);
		console.log('rowData data:', rowData);
		
		// 예: 모달 열기, 상세 정보 표시 등		
		$('#qcItem-modal').modal('show');
		document.getElementById('qcmodalTilte').innerText= 'QC 항목 상세';
		document.getElementById('modalQcItemId').value = rowData.qcItemId;//QC 항목 ID
		document.getElementById('itemName').value = rowData.itemName;//항목명
		document.getElementById('targetType').value = rowData.targetType;//대상구분
		document.getElementById('unit').value = rowData.unit;//단위
		document.getElementById('stdText').value = rowData.stdText;//기준텍스트
		document.getElementById('minValue').value = rowData.minValue;//최소값
		document.getElementById('maxValue').value = rowData.maxValue;//최대값
		document.getElementById('sortOrder').value = rowData.sortOrder;//정렬순서
		document.getElementById('useYn').value = rowData.useYn;//사용여부
		document.getElementById('createdId').value = rowData.createdByName;//생성자
		document.getElementById('createdDate').value = rowData.createdDate;//생성일시
		document.getElementById('updatedId').value = rowData.updatedByName;//수정자
		document.getElementById('updatedDate').value = rowData.updatedDate;//수정일시
		
		document.getElementById('qcItemId').readOnly = true;
		document.getElementById('userAndDate').style.display = 'flex';

		qcItemGridAllSearch();
	}

});

// 모달 내 폼을 가로채서 AJAX로 전송하여 서버 에러 메시지를 alert로 표시
const qcItemForm = document.querySelector('#qcItem-modal form');
if (qcItemForm) {
    qcItemForm.addEventListener('submit', function (ev) {
        ev.preventDefault();
        const form = ev.target;
           // mode 값을 기존 input에 설정 (form에 name="mode"인 input이 있다면)
        const modeValue = document.getElementById('qcmodalTilte').innerText == 'QC 항목 등록' ? 'new' : 'modify';
        let modeInput = form.querySelector('input[name="mode"]');
        
        if (modeInput) {
            modeInput.value = modeValue;
        } else {
            // mode input이 없으면 새로 생성
            modeInput = document.createElement('input');
            modeInput.type = 'hidden';
            modeInput.name = 'mode';
            modeInput.value = modeValue;
            form.appendChild(modeInput);
        }
        
        const formData = new FormData(form);
        const params = new URLSearchParams(formData);
        console.log('최종 params:', params.toString());

        fetch(apiUrl(`${form.action}`), {
            method: form.method || 'POST',
            credentials: 'same-origin',
            headers: {
                [csrfHeader]: csrfToken,
                'Content-Type': 'application/x-www-form-urlencoded',
                'X-Requested-With': 'XMLHttpRequest'
            },
            body: params.toString()
        })
        .then(res => {
            if (!res.ok) return res.text().then(t => { throw new Error(`HTTP ${res.status}: ${t || res.statusText}`); });
            return res.text();
        })
        .then(result => {
            console.log('서버 응답:', result);
            
            // 오류 응답 처리
            if (result && result.toLowerCase().startsWith('error')) {
                const errorMsg = result.replace(/^error:\s*/i, '').trim();
                alert(errorMsg);
                return;
            }
            
            // 성공 응답 처리
            if (result && result.toLowerCase().includes('success')) {
                alert('저장되었습니다.');
                // 성공 시 모달 닫고 그리드 갱신
                document.querySelector('#qcItem-modal .modal-footer [data-bs-dismiss="modal"]').click();
                qcModalreset();
                qcItemGridAllSearch();
            } else {
                // 예상치 못한 응답도 일단 표시
                alert(result || '알 수 없는 오류가 발생했습니다.');
            }
        })
        .catch(err => {
            console.error('QC 저장 오류', err);
            alert(err.message || '저장 중 오류가 발생했습니다.');
        });
    });
}
// 항목 등록
const qcItemRegistBtn = document.getElementById('qcItemRegistBtn');
qcItemRegistBtn.addEventListener("click", function() {
	document.getElementById('qcmodalTilte').innerText= 'QC 항목 등록';
	qcModalreset();
	document.getElementById('modalQcItemId').value = 'QC-';
	document.getElementById('qcItemId').readOnly = false;
	document.getElementById('userAndDate').style.display ='none';//생성자
	
});

function qcModalreset() {
	document.getElementById('modalQcItemId').value = '';//QC 항목 ID
	document.getElementById('itemName').value = '';//항목명
	document.getElementById('targetType').value = '';//대상구분
	document.getElementById('unit').value = '';//단위
	document.getElementById('stdText').value = '';//기준텍스트
	document.getElementById('minValue').value = '';//최소값
	document.getElementById('maxValue').value = '';//최대값
	document.getElementById('sortOrder').value = '';//정렬순서
	document.getElementById('useYn').value = '';//사용여부
	document.getElementById('createdId').value = '';//생성자
	document.getElementById('createdDate').value = '';//생성일시
	document.getElementById('updatedId').value = '';//수정자
	document.getElementById('updatedDate').value = '';//수정일시
	qcItemGridAllSearch();//공정코드 관리 그리드 조회
}


// 품질항목관리 삭제
const deleteQcRowBtn = document.getElementById('deleteQcRowBtn');
deleteQcRowBtn.addEventListener('click', async function() {

	// 체크된 rowKey들 수집
	let rowKeysToDelete = [];
	try {
		if (typeof grid1.getCheckedRowKeys === 'function') {
			rowKeysToDelete = grid1.getCheckedRowKeys() || [];
		} else if (typeof grid1.getCheckedRows === 'function') {
			const checkedRows = grid1.getCheckedRows() || [];
			rowKeysToDelete = checkedRows.map(r => r && (r.rowKey || r.qcItemId)).filter(Boolean);
		}
	} catch (e) {
		console.warn('체크된 행 조회 실패', e);
	}
	if (!Array.isArray(rowKeysToDelete) || rowKeysToDelete.length === 0) {
		alert('삭제할 행을 선택(체크)해주세요.');
		return;
	}
	// 간결한 방식으로 각 rowKey로부터 qcItemId(또는 식별 가능한 ID)를 수집
	const getAllData = () => (typeof grid1.getData === 'function' ? grid1.getData() : (grid1.data || []));
	const qcItemIds = rowKeysToDelete.map(key => {
		try {
			const row = (typeof grid1.getRow === 'function' && grid1.getRow(key)) ||
				getAllData().find(d => d && (String(d.rowKey) === String(key) || String(d.qcItemId) === String(key)));
			return row && row.qcItemId ? String(row.qcItemId) : String(key);
		}
		catch (e) {
			console.warn('삭제 ID 수집 중 오류', e);
			return String(key);
		}
	}).filter(Boolean);

	if (!confirm('선택한 항목을 삭제하시겠습니까?')) return;
	fetch(apiUrl(`masterData/qcItem/delete`), {
		method: 'POST',
		credentials: 'same-origin',
		headers: {
			[csrfHeader]: csrfToken,
			'Content-Type': 'application/json'
		},
		body: JSON.stringify(qcItemIds)
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
		qcItemGridAllSearch();
	})
	.catch(err => {
		console.error('삭제 중 오류', err);
		try { alert('삭제 중 오류가 발생했습니다. ' + (err && err.message ? err.message : '')); } catch (e) {}
	});
});






