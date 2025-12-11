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
		,{header: '대상구분' ,name: 'targetType' ,align: 'center',width: 110,filter: "select"}
		,{header: '단위' ,name: 'unit' ,align: 'center'}
		,{header: '기준 텍스트' ,name: 'stdText' ,align: 'center',width: 230}
		,{header: 'MIN' ,name: 'minValue' ,align: 'center'}
        ,{header: 'MAX' ,name: 'maxValue' ,align: 'center'}
		,{header: '사용' ,name: 'useYn' ,align: 'center'}//,hidden: true  
		,{header: '정렬순서' ,name: 'sortOrder' ,align: 'center'}
		,{header: '생성자' ,name: 'createdId' ,align: 'center'}  
		,{header: '생성자' ,name: 'createdDate' ,align: 'center'}  
		,{header: '수정자' ,name: 'updatedId' ,align: 'center'}  
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
	
//qcitem  품질항목관리 조회
function qcItemGridAllSearch(){
	fetch('/masterData/qc_item/list', {
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
	fetch('/masterData/qcItem/delete', {
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






