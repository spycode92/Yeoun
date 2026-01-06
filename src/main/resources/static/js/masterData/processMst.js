window.onload = function () {	
	productRouteSearch();//제품별 공정라우트 그리드 조회
	processCodeGridAllSearch();//공정코드 관리 그리드 조회
}

//탭 전환시 그리드 레이아웃 갱신
document.querySelectorAll('button[data-bs-toggle="tab"]').forEach(tab => {
    tab.addEventListener('shown.bs.tab', function (e) {
        const targetId = e.target.getAttribute('data-bs-target');
        if (targetId === '#navs-process-tab') {//제품별 공정라우트 탭
            grid1.refreshLayout();
			productRouteSearch();
        } else if (targetId === '#navs-processCode-tab') {//공정코드 관리 탭
            grid2.refreshLayout();
			processCodeGridAllSearch();
        }
    });
});

// 라우트단계 공정코드 조회 모달 그리드 레이아웃갱신
const routeModalElement = document.getElementById('route-modal');//신규라우트 모달
routeModalElement.addEventListener('shown.bs.modal', function () {
    grid3.refreshLayout();
});

const processLookupModalElement = document.getElementById('processLookup-modal');//공정코드 조회 모달
processLookupModalElement.addEventListener('shown.bs.modal', function () {
	grid4.refreshLayout();
});

const Grid = tui.Grid;

// g- grid1 제품별 공정 라우트 그리드
const grid1 = new Grid({
	  el: document.getElementById('processGrid'), 
      rowHeaders: ['rowNum','checkbox'],
	  columns: [

		{header: '라우트ID' ,name: 'routeId' ,align: 'center'}
		,{header: '제품코드' ,name: 'prdId' ,align: 'center'}
		,{header: '라우트명' ,name: 'routeName' ,align: 'center',width: 150,filter: "select"}
		,{header: '설명' ,name: 'description' ,align: 'center',width: 370}
		,{header: '사용여부' ,name: 'useYn' ,align: 'center',width: 90,hidden: true}  
		,{header: '생성자id' ,name: 'createdId' ,align: 'center',hidden: true}
		,{header: '생성자이름' ,name: 'createdByName' ,align: 'center'}  
		,{header: '생성일시' ,name: 'createdDate' ,align: 'center'}  
		,{header: '수정자id' ,name: 'updatedId' ,align: 'center',hidden: true} 
		,{header: '수정자이름' ,name: 'updatedByName' ,align: 'center'} 
		,{header: '수정일시' ,name: 'updatedDate' ,align: 'center'} 
		, {
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
    		perPage: 10
  	  }
	});
	
// g- grid2 공정코드 관리(PROCESS_MASTER 조회)
const grid2 = new Grid({
	    el: document.getElementById('processCodeGrid'),
        rowHeaders: ['rowNum','checkbox'],
	    columns: [
	    {header: '공정ID' ,name: 'processId' ,align: 'center',editor: 'text'
			,renderer:{ type: StatusModifiedRenderer}
		}
	    ,{header: '공정명' ,name: 'processName' ,align: 'center',editor: 'text' ,width: 120,filter: "select"
			,renderer:{ type: StatusModifiedRenderer
				,options: {
					isSelect: false   // ⭐ 이걸로 구분
				}
			}
		}
	    ,{header: '공정유형' ,name: 'processType' ,align: 'center',editor: 'text'
			,renderer:{ type: StatusModifiedRenderer}
		}
	    ,{header: '설명' ,name: 'description' ,align: 'center',editor: 'text' ,width: 370
			,renderer:{ type: StatusModifiedRenderer}
		}
		,{header: '공정순서' ,name: 'stepNo' ,align: 'center'
			,renderer:{ type: StatusModifiedRenderer}
			,editor: {
				type: NumberOnlyEditor, // ⬅️ 클래스 이름 직접 사용
				options: {
				maxLength: 10
				}
			}
		}
		,{header: '생성자id' ,name: 'createdId' ,align: 'center',hidden:true}
		,{header: '생성자이름' ,name: 'createdByName' ,align: 'center'}  
		,{header: '생성일시' ,name: 'createdDate' ,align: 'center'}
		,{header: '수정자id' ,name: 'updatedId' ,align: 'center',hidden:true}
		,{header: '수정자이름' ,name: 'updatedByName' ,align: 'center'} 
		,{header: '수정일시' ,name: 'updatedDate' ,align: 'center'}
		,{header: '사용여부' ,name: 'useYn' ,align: 'center'
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
	    	perPage: 10
        }
});

//g-grid3 신규라우트 모달 그리드 - 공정단계
const grid3 = new Grid({
	    el: document.getElementById('processStepGrid'),
        rowHeaders: ['rowNum','checkbox'],
	    columns: [
	    {header: '라우트단계ID' ,name: 'routeStepId' ,align: 'center', editor: 'text'
			,renderer:{ type: StatusModifiedRenderer}
		}
	    ,{header: '라우트ID' ,name: 'routeId' ,align: 'center', editor: 'text',filter: "select"
			,renderer:{ type: StatusModifiedRenderer}
		}
	    ,{header: '순번' ,name: 'stepSeq' ,align: 'center', editor: 'text',filter: "select"
			,renderer:{ type: StatusModifiedRenderer}
			,editor: {
				type: NumberOnlyEditor 
				,options: {
					maxLength: 10
				}
			}
		}
		,{header: '공정ID' ,name: 'processId' ,align: 'center', editor: 'text',filter: "select"
			,renderer:{ type: StatusModifiedRenderer}
		}
        ,{header: 'QC 여부' ,name: 'qcPointYn' ,align: 'center'
			,renderer:{ type: StatusModifiedRenderer
				,options: {
					isSelect: true
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
	    ,{header: '비고' ,name: 'remark' ,align: 'center', editor: 'text'
			,renderer:{ type: StatusModifiedRenderer}
		}
		,{header: '생성자id' ,name: 'createdId' ,align: 'center', hidden:true}
		,{header: '생성자이름' ,name: 'createdByName' ,align: 'center'}	
		,{header: '생성일시' ,name: 'createdDate' ,align: 'center'}	
		,{header: '수정자id' ,name: 'updatedId' ,align: 'center',hidden:true}	
		,{header: '수정자이름' ,name: 'updatedByName' ,align: 'center'} 
		,{header: '수정일시' ,name: 'updatedDate' ,align: 'center'}                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                              
	    ],
	    data: []
	    ,bodyHeight: 200 // 그리드 본문의 높이를 픽셀 단위로 지정. 스크롤이 생김.
	    ,height:100
	    ,columnOptions: {
	    	resizable: true
        }
	    ,pageOptions: {
	    	useClient: true,
	    	perPage: 10
        }
});


//g- grid2 = grid4 신규라우트 모달 그리드 - 공정코드조회 모달(PROCESS_MASTER 조회)
const grid4 = new Grid({
	    el: document.getElementById('routeStepCodeGrid'),
        rowHeaders: ['rowNum'],
	    columns: [
	    {header: '공정ID' ,name: 'processId' ,align: 'center'}
	    ,{header: '공정명' ,name: 'processName' ,align: 'center'}
	    ,{header: '공정유형' ,name: 'processType' ,align: 'center'}
	    ,{header: '설명' ,name: 'description' ,align: 'center',width: 315}
		,{header: '공정순서' ,name: 'stepNo' ,align: 'center'
			,renderer:{ type: StatusModifiedRenderer}
		}
		,{header: '사용여부' ,name: 'useYn' ,align: 'center'
			,renderer:{ type: StatusModifiedRenderer
				,options: {
					isSelect: false   // ⭐ 이걸로 구분
				}
			}
		}
		,{header: '생성자id' ,name: 'createdId' ,align: 'center',hidden: true}
		,{header: '생성자이름' ,name: 'createdByName' ,align: 'center',hidden: true}  
		,{header: '생성일시' ,name: 'createdDate' ,align: 'center',hidden: true}
		,{header: '수정자id' ,name: 'updatedId' ,align: 'center',hidden: true}
		,{header: '수정일시' ,name: 'updatedDate' ,align: 'center',hidden: true}
	    ],
	    data: []
	    ,bodyHeight: 200 // 그리드 본문의 높이를 픽셀 단위로 지정. 스크롤이 생김.
	    ,height:100
	    ,columnOptions: {
	    	resizable: true
        }
});


const PROCESS_CODE_TO_TYPE_MAP = {
    '블렌딩': 'MIX',         
    '여과': 'FILTER',   
    '충전': 'FILL',       
    '캡/펌프': 'CAPPING', 
    'QC 검사': 'QC',         
    '라벨링': 'PACK'        
};
//공정코드 관리 그리드 수정시 기존 공정ID수정 불가
grid2.on('beforeChange', (ev) => {
	const { rowKey, columnName, value } = ev.changes[0]; // 변경된 데이터 목록 (배열)
	if (columnName === 'processId') {
		// 기존 로직은 processId 값 유무로 신규행 판별했으나,
		// prependRow 등으로 기본값이 채워지면 신규행이더라도 수정이 막히는 문제가 있어
		// grid의 modifiedRows().createdRows 목록에 rowKey가 존재하는지로 판단합니다.
		const processIdValue = grid2.getValue(rowKey, 'processId');
		let isNewRow = false;
		try {
			const modified = (typeof grid2.getModifiedRows === 'function') ? (grid2.getModifiedRows() || {}) : {};
			const createdRows = Array.isArray(modified.createdRows) ? modified.createdRows : [];
			isNewRow = createdRows.some(r => r && String(r.rowKey) === String(rowKey));
		} catch (e) {
			// 실패 시 기존 fallback 사용
			isNewRow = !processIdValue;
		}

		console.log("processId 값:", processIdValue, " | isNewRow:", isNewRow);

		if (!isNewRow) {
			ev.stop(); // 편집 모드 진입 차단
			alert('기존 공정ID는 수정할 수 없습니다. 삭제후 새로추가(등록) 해주세요!'); 
		}
	}
});

grid2.on('afterChange', (ev) => {
	const { rowKey, columnName, value } = ev.changes[0]; 
	
	if(columnName === 'processName'){
		const processNameValue = value;
		
		const newProcessType = PROCESS_CODE_TO_TYPE_MAP[processNameValue];
		if(newProcessType){
			grid2.setValue(rowKey, 'processType', newProcessType, false); // 마지막 false는 이벤트 발생 방지
		}
		
	}
	
});

//공정단계에서 더블클릭시 모달열리게
grid3.on('dblclick', function(ev) {
    // 1. 클릭한 위치가 셀(cell)인지 확인
    // 2. 더블클릭한 컬럼 이름이 'matId'인지 확인
    if (ev.targetType === 'cell' && ev.columnName === 'processId') {
        
        const rowData = grid3.getRow(ev.rowKey);
        
        if (rowData) {
            const myModal = new bootstrap.Modal(document.getElementById('processLookup-modal'));
            myModal.show();
          
        }
    }
});


//BOM정보 원재료 id-> 원재료 조회 클릭시 row 더블클릭시 값이 들어감 
grid4.on('dblclick', function(ev) {
    if (ev.targetType !== 'cell' && ev.targetType !== 'rowHeader') {
        return; 
    }

    let rowData = grid4.getRow(ev.rowKey); 
    let focusInfo = grid3.getFocusedCell();

    if (focusInfo && focusInfo.rowKey !== null) {
        grid3.finishEditing(focusInfo.rowKey, 'processId');
        // 데이터 입력 실행
        grid3.setValue(focusInfo.rowKey, 'processId', rowData.processId);
        console.log("입력 완료:", focusInfo.rowKey, rowData.processId);

        // 4. 모달 닫기
        let closeBtn = document.querySelector('#processLookup-modal .modal-footer [data-bs-dismiss="modal"]');
        if (closeBtn) closeBtn.click();
        
    } else {
        alert("데이터를 입력할 행을 먼저 선택해주세요.");
    }
});


let processLookupModal; // 공정코드 조회 모달
document.addEventListener("DOMContentLoaded", () => {
  processLookupModal = new bootstrap.Modal(document.getElementById("processLookup-modal"));
});

//신규라우트 모달 오픈
function openRouteModalForCreate(){
	routeModalreset();
	document.getElementById('userAndDate').style.display = 'none';
	document.getElementById('routeModalTitle').innerText ='신규 라우트 등록';
	document.getElementById('modalProcessprdId').disabled = false;
}

// 신규라우트 -->  공정코드 조회 2번째 모달
function openProcessLookupModal() {
    processLookupModal.show();
}

  
//제품별 공정라우트 그리드 조회
function productRouteSearch(){
	
	const params = {
		prdId: document.getElementById("processprdId").value ?? "",
		routeName: document.getElementById("routeName").value ?? "",		
	};
	
	const queryString = new URLSearchParams(params).toString();
	fetch(apiUrl(`masterData/process/list?${queryString}`), {
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
			console.log("검색데이터 grid1:", data);
			
			const camelCaseData = transformKeys(data);
			console.log("camelCaseData",camelCaseData);
			grid1.resetData(camelCaseData);
		})
		.catch(err => {
			console.error("조회오류", err);
			grid1.resetData([]);
		
		});
	
}

//공정코드 관리 그리드 조회
function processCodeGridAllSearch() {
	
	const params = {
		processId: document.getElementById("processId").value ?? "",
		processName: document.getElementById("processName").value ?? "",		
	};
	const queryString = new URLSearchParams(params).toString();
	
	fetch(apiUrl(`masterData/processCode/list?${queryString}`), {
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
			return res.json();
		})
		.then(data => {
			console.log("검색데이터 grid2:", data);
			const camelCaseData = transformKeys(data);
			console.log("camelCaseData",camelCaseData);
			grid2.resetData(camelCaseData);
			grid4.resetData(camelCaseData);//신규라우트 모달 그리드 - 공정코드조회 모달
			//공정코드 라우트 step id 자동생성 데이터 넣어주기
			processDataList = camelCaseData.map((item, index) => {
				let currentStep = item.stepNo || '';
				let displayStep = String(currentStep).padStart(2, '0');
				return {
					// 데이터에 stepNo가 있으면 그대로 쓰고, 없으면 순번(index+1)을 2자리 문자열로 만듭니다.
					stepNo: displayStep,
					processId: item.processId || '',
					processName: item.processName || ''
				};
			});
		})
		.catch(err => {	
			console.error("조회오류", err);
			grid2.resetData([]);
		});
}

//grid3 신규라우트 모달 그리드 - 공정단계 조회
function processStepSearch(routeId) {
	
	fetch(apiUrl(`masterData/processStep/list?routeId=${routeId}`), {
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
			return res.json();
		})
		.then(data => {
			console.log("검색데이터 grid3:", data);
			const camelCaseData = transformKeys(data);
			console.log("camelCaseData",camelCaseData);
			grid3.resetData(camelCaseData);
		})
		.catch(err => {	
			console.error("조회오류", err);
			grid3.resetData([]);
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

//제품별 공정라우트 그리드 - 상세보기 버튼 클릭 이벤트
grid1.on("click", async (ev) => {

	const target = ev.nativeEvent.target;
	// const targetElement = ev.nativeEvent.target; 이 줄이 빠진 경우
	if (ev.targetType === 'cell' && target.tagName === 'BUTTON') {
		console.log('Button in cell clicked, rowKey:', ev.rowKey);
		
		const rowData = grid1.getRow(ev.rowKey);
		console.log('Row data:', rowData);
		console.log('라우트ID:', rowData.routeId);
		
		// 예: 모달 열기, 상세 정보 표시 등		
		$('#route-modal').modal('show');
		document.getElementById('routeModalTitle').innerText = '라우트 상세';
		document.getElementById('modalRouteId').value = rowData.routeId;//라우트 ID
		document.getElementById('modalProcessprdId').value = rowData.prdId;//제품코드
		document.getElementById('modalRouteName').value = rowData.routeName;//라우트명
		document.getElementById('modalRouteUseYn').value = rowData.useYn;//사용여부
		document.getElementById('modalRouteRemark').value = rowData.description;//비고
		document.getElementById('modalRouteCreatedId').value = rowData.createdByName;//생성자
		document.getElementById('modalRouteCreatedDate').value = rowData.createdDate;//생성일시
		document.getElementById('modalRouteUpdatedId').value = rowData.updatedByName;//수정자
		document.getElementById('modalRouteUpdatedDate').value = rowData.updatedDate;//수정일시
		
		document.getElementById('userAndDate').style.display = 'flex';
		document.getElementById('modalProcessprdId').disabled = true;
		processStepSearch(rowData.routeId);//신규라우트 모달 그리드 - 공정단계 조회
	}

});
//공정단계에서 MATID를 더블클릭하면 품목코드 창열리게



//라우트 모달 셀렉트박스 값선택시 자동으로 routeId생성
document.getElementById('modalProcessprdId').addEventListener('change', function() {
	const prdId = this.value;
	const generatedRouteId = `RT-${prdId}`; // 예: RT-제품코드-타임스탬프
	document.getElementById('modalRouteId').value = generatedRouteId;
});


//라우트모달 리셋
function routeModalreset() {
	document.getElementById('modalRouteId').value = '';//라우트 ID
	document.getElementById('modalProcessprdId').value = '';//제품코드
	document.getElementById('modalRouteName').value = '';//라우트명
	document.getElementById('modalRouteUseYn').value = 'Y';//사용여부
	document.getElementById('modalRouteRemark').value = '';//비고
	document.getElementById('modalRouteCreatedId').value = '';//생성자
	document.getElementById('modalRouteCreatedDate').value = '';//생성일시
	document.getElementById('modalRouteUpdatedId').value = '';//수정자
	document.getElementById('modalRouteUpdatedDate').value = '';//수정일시
	grid3.resetData([]);//신규라우트 모달 그리드 - 공정단계 조회 초기화
	processCodeGridAllSearch();//공정코드 관리 그리드 조회
}

//공정코드 관리 그리드 추가버튼
const addProcessCodeRowBtn = document.getElementById('addProcessCodeRowBtn');
addProcessCodeRowBtn.addEventListener('click', function(event) {
	grid2.prependRow({
        processId: 'PRC-',             // 고정 기본값
    }, { focus: true });
    
    //새로 추가된 행의 공정ID 기본값 설정
});

//공정코드를 fatch로 불러와서 붙이면좋을듯
let processDataList = [
	//예시
    { stepNo: "01", processId: "PRC-BLD", processName: "블렌딩" },
    { stepNo: "02", processId: "PRC-FLT", processName: "여과" },
    { stepNo: "03", processId: "PRC-FIL", processName: "충전" },
    { stepNo: "04", processId: "PRC-CAP", processName: "캡/펌프 조립" },
    { stepNo: "05", processId: "PRC-QC", processName: "QC 검사" },
    { stepNo: "06", processId: "PRC-LBL", processName: "라벨링/포장" }
];

//라우트id가 있으면 자동으로 routeId 삽입

//라우트모달 공정단계 단계추가
function addRouteStepRow(){
	grid3.appendRow();
	//공정단계를 추가 하면 자동으로 생성되는 routestpeId
	// 새로 생성된 행 목록
	const newRows = grid3.getModifiedRows().createdRows; 
	
	if (newRows.length > 0) {
	    const prdId = document.getElementById('modalProcessprdId').value;
		const routeId = document.getElementById('modalRouteId').value;
		
	    newRows.forEach((item) =>{
			if(!item.routeId && prdId){
				grid3.setValue(item.rowKey,'routeId',routeId);
			}
			
		});
	} else {
	    console.log("새로운 행이 없습니다.");
	}
}
//공정단계 - 공정 id 가 추가되면 라우트 단계id가 자동으로들어간다.
grid3.on('afterChange', (ev) => {
    const { rowKey, columnName,value } = ev.changes[0]; // 변경된 데이터 목록 (배열)
	if (columnName === 'processId') {
	        // 💡 핵심 수정: rowKey 대신, 현재 행의 'processId' 값을 가져옵니다.
			const prdId = document.getElementById('modalProcessprdId').value;
	        const processIdValue = grid3.getValue(rowKey, 'processId');
			
			// 제품 ID (prdId)가 없으면 RouteStepId를 만들 수 없으므로 중단
            if (!prdId) {
                console.error("제품 ID(prdId)가 설정되지 않았습니다.");
                return;
            }
			
			// 변경된 processId (value)를 사용하여 processDataList에서 해당 StepNo/Name 찾기
			const selectedProcess = processDataList.find(item => item.processId === value);
			
			if(selectedProcess){
				const stepNo = selectedProcess.stepNo;
				const generatedRouteStepId = `RS-${prdId}-${stepNo}`;
				grid3.setValue(rowKey,'routeStepId',generatedRouteStepId);
				console.log(`RowKey: ${rowKey} | RouteStepId 생성 완료: ${generatedRouteStepId}`);

			}else{
				console.warn(`일치하는 processId (${value})를 processDataList에서 찾을 수 없습니다.`);
			}
			

	    }
});


//공정코드관리 그리드 저장
const saveProcessCodeRowBtn = document.getElementById('saveProcessCodeRowBtn');
saveProcessCodeRowBtn.addEventListener('click', function() {
		
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
		alert('공정코드 그리드 내용이 없습니다. 계속 진행하시겠습니까?');
		return;
		
	}
	fetch(apiUrl(`masterData/processCode/save`), {
		method: 'POST',
		credentials: 'same-origin',
		headers: {
			[csrfHeader]: csrfToken,
			'Content-Type': 'application/json'
		},
		body: JSON.stringify(modifiedData)
	})
	.then(async res => {
	    if (!res.ok) {
	        throw new Error(`HTTP error! status: ${res.status}`);
	    }
	    // 응답 Content-Type 확인: JSON이면 파싱, 아니면 텍스트로 읽음
	    const contentType = res.headers.get('content-type') || '';
	    if (contentType.includes('application/json')) {
	        const data = await res.json();
			return ({ type: 'json', data });
	    }
	    const text = await res.text();
		return ({ type: 'text', data: text });
	})
	.then(resp => {
	    if (!resp) return;
	    if (resp.type === 'json') {
	        console.log('저장결과(JSON):', resp.data);
	        // 서버에서 JSON 형태로 상태를 보내는 경우 추가 처리 가능
	        alert('저장 완료');
	    } else {
	        const text = String(resp.data || '').trim();
	        console.log('저장결과(텍스트):', text);
	        if (text === 'success') {
	            alert('저장 완료');
				processCodeGridAllSearch();
				
	        } else if (text === 'no-data') {
	            alert('서버: 전송한 데이터가 없습니다. 내용을 확인하세요.');
	        } else if (text.startsWith('error')) {
	            alert('저장 중 오류: ' + text);
	        } else {
	            // 미확인 텍스트 응답
	            alert('저장 완료 (서버 응답: ' + text.substring(0, 200) + ')');
	        }
	    }
	})
	.catch(err => {
		console.error('저장오류', err);
		alert('저장 중 오류가 발생했습니다. 콘솔 로그를 확인하세요.');
	});
	
});

// 라우트모달 공정단계 저장
const saveRouteBtn = document.getElementById('saveRouteBtn');
saveRouteBtn.addEventListener('click', function() {
	
	const modifiedData = (typeof grid3.getModifiedRows === 'function') ? (grid3.getModifiedRows() || {}) : {};
	const updatedRows = Array.isArray(modifiedData.updatedRows) ? modifiedData.updatedRows : [];
	let createdRows = Array.isArray(modifiedData.createdRows) ? modifiedData.createdRows : [];
	
	const routeNewData = {
		routeId: document.getElementById('modalRouteId').value ?? "",
		prdId: document.getElementById('modalProcessprdId').value ?? "",
		routeName: document.getElementById('modalRouteName').value ?? "",
		useYn: document.getElementById('modalRouteUseYn').value ?? "",
		description: document.getElementById('modalRouteRemark').value ?? ""
	};
	//console.log('라우트저장데이터:', routeNewData);
	// 생성된 공정단계의 라우트ID가 라우트정보의 라우트ID와 일치하는지 확인
	createdRows.forEach(row => {
		if(row.routeId != routeNewData.routeId){
			alert( '라우트정보의 라우트ID와 생성된 공정단계의 라우트ID가 일치하지 않습니다. 라우트ID를 확인해주세요.');
		 	return;
		}
	});

	// 누락된 입력 항목들을 하나로 모아 사용자에게 알림
	const missing = [];
	if (!routeNewData.prdId || String(routeNewData.prdId).trim() === '') missing.push('제품코드');
	if (!routeNewData.routeName || String(routeNewData.routeName).trim() === '') missing.push('라우트명');
	if (missing.length > 0) {
		alert(missing.join(' 및 ') + '을(를) 입력해주세요.');
		return;
	}

	// routeInfo에 mode 추가
	if(document.getElementById('routeModalTitle').textContent === '신규 라우트 등록'){
		routeNewData.mode = 'new';
	}else{
		routeNewData.mode = 'modify';
	}
	
	// 모든 유효성 검사 완료 후 routeInfo 설정
	modifiedData.routeInfo = routeNewData;
	

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
				if (key && typeof grid3.removeRow === 'function') {
					grid3.removeRow(key);
				} else if (key && typeof grid3.deleteRow === 'function') {
					grid3.deleteRow(key);
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

	// 안전한 로깅: modifiedData는 객체(예: {createdRows, updatedRows, routeInfo})일 수 있습니다.
	// 모든 경우에 대해 stepSeq 유효성 검사 (배열/객체 모두 커버)
	let invalid = false;
	const validateRows = (rows) => {
		if (!Array.isArray(rows) || invalid) return;
		for (const r of rows) {
			const seq = Number(r?.stepSeq);
			console.log('check stepSeq', seq);
			if (!Number.isNaN(seq) && seq > 99999) {
				alert('순번은 99999 이하로 지정해주세요.');
				invalid = true;
				return;
			}
		}
	};

	if (Array.isArray(modifiedData)) {
		for (const item of modifiedData) {
			console.log(item);
			validateRows(item.createdRows);
			if (invalid) break;
			validateRows(item.updatedRows);
			if (invalid) break;
		}
	} else if (modifiedData && typeof modifiedData === 'object') {
		// 객체 형태의 modifiedData도 createdRows/updatedRows 검사
		validateRows(modifiedData.createdRows);
		if (!invalid) validateRows(modifiedData.updatedRows);
		Object.entries(modifiedData).forEach(([k, v]) => console.log(k, v));
	} else {
		console.log(modifiedData);
	}

	if (invalid) return; // 검사 실패 시 저장 중단
		
	if (updatedRows.length === 0 && createdRows.length === 0) {
		if(confirm('공정단계 그리드 수정내용이 없습니다. 계속 진행하시겠습니까?') === false) {
			return;
		}
	}
	
	createdRows.forEach(row => {
	    if (row.routeStepId === null || row.routeStepId === undefined || row.routeStepId.trim() === '') {
	        // routeStepId가 없는 신규 행에 대해 생성 로직 재실행 (안전망)
			alert("라우트단계ID를 생성할 수 없습니다.직접 지정해주세요 예시) RS-제품코드-번호");
			return; // 저장 취소
	    }
	});

	console.log('수정된 데이터:', modifiedData);
	fetch(apiUrl(`masterData/process/save`), {
		method: 'POST',
		credentials: 'same-origin',
		headers: {
			[csrfHeader]: csrfToken,
			'Content-Type': 'application/json'
		},
		body: JSON.stringify(modifiedData)
	})
	.then(async res => {
	    if (!res.ok) {
	        throw new Error(`HTTP error! status: ${res.status}`);
	    }
	    // 응답 Content-Type 확인: JSON이면 파싱, 아니면 텍스트로 읽음
	    const contentType = res.headers.get('content-type') || '';
	    if (contentType.includes('application/json')) {
	        const data = await res.json();
			return ({ type: 'json', data });
	    }
	    const text = await res.text();
		return ({ type: 'text', data: text });
	})
	.then(resp => {
	    if (!resp) return;
	    if (resp.type === 'json') {
	        console.log('저장결과(JSON):', resp.data);
	        // 서버에서 JSON 형태로 상태를 보내는 경우 추가 처리 가능
	        alert('저장 완료');
			//공정단계그리드조회
			
			processStepSearch(document.getElementById('modalRouteId').value);
	    } else {
	        const text = String(resp.data || '').trim();
	        console.log('저장결과(텍스트):', text);
	        if (text === 'success') {
	            alert('저장 완료');
				//공정단계그리드조회
				processStepSearch(document.getElementById('modalRouteId').value);
	        } else if (text === 'no-data') {
	            alert('서버: 전송한 데이터가 없습니다. 내용을 확인하세요.');
	        } else if (text.startsWith('error')) {
	            alert('저장 중 오류: ' + text);
	        } else {
	            // 미확인 텍스트 응답
	            alert('저장 완료 (서버 응답: ' + text.substring(0, 200) + ')');
	        }
	    }
	})
	.catch(err => {
		console.error('저장오류', err);
		alert('저장 중 오류가 발생했습니다. 컬럼을 확인해주세요.');
	});
});

//제품별 공정라우트 그리드 수정(삭제) useYn='N' 처리
const modifyProcessRowBtn = document.getElementById('modifyProcessRowBtn');
modifyProcessRowBtn.addEventListener('click', async function() {
	const checkedRows = grid1.getCheckedRows();
	if (checkedRows.length === 0) {
		alert('삭제할 라우트를 선택해주세요.');
		return;
	}
	if (!confirm(`${checkedRows.length}개의 라우트를 삭제하시겠습니까?`)) {
		return;
	}
	try {
		const response = await fetch(apiUrl(`masterData/process/modify`), {
			method: 'POST',
			credentials: 'same-origin',
			headers: {
				[csrfHeader]: csrfToken,
				'Content-Type': 'application/json'
			},
			body: JSON.stringify({ routes: checkedRows })
		});
		if (!response.ok) {
			throw new Error(`HTTP error! status: ${response.status}`);
		}
		const resultText = await response.text();
		if (resultText === 'success') {
			alert('삭제 완료');
			productRouteSearch();//제품별 공정라우트 그리드 조회
		} else {
			alert('삭제 실패: ' + resultText);
		}
	} catch (error) {
		console.error('삭제오류', error);
		alert('삭제 중 오류가 발생했습니다. 콘솔 로그를 확인하세요.');
	}
});

// 공정코드 관리 수정(삭제) useYn='N' 처리
const modifyProcessCodeRowBtn = document.getElementById('modifyProcessCodeRowBtn');
//완제품row 삭제: POST JSON형식으로 서버에 요청
modifyProcessCodeRowBtn.addEventListener('click', async function() {

	// 체크된 rowKey들 수집
	let rowKeysToDelete = [];
	try {
		if (typeof grid2.getCheckedRowKeys === 'function') {
			rowKeysToDelete = grid2.getCheckedRowKeys() || [];
		} else if (typeof grid2.getCheckedRows === 'function') {
			const checkedRows = grid2.getCheckedRows() || [];
			rowKeysToDelete = checkedRows.map(r => r && (r.rowKey || r.processId)).filter(Boolean);
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

	// 간결한 방식으로 각 rowKey로부터 prdId(또는 식별 가능한 ID)를 수집
	const getAllData = () => (typeof grid2.getData === 'function' ? grid2.getData() : (grid2.data || []));

		// 구분: 빈 행(또는 prdId가 없는 행)은 화면에서만 삭제하고, prdId가 있는 행만 서버에 삭제 요청
		try {
			const getAllData = () => (typeof grid2.getData === 'function' ? grid2.getData() : (grid2.data || []));
			const data = getAllData();
			// 그리드의 수정 정보에서 생성된(신규) 행들을 조회하여, 신규행은 UI에서만 삭제하도록 처리
			const modified = (typeof grid2.getModifiedRows === 'function') ? (grid2.getModifiedRows() || {}) : {};
			const createdRows = Array.isArray(modified.createdRows) ? modified.createdRows : [];
			const uiOnlyKeys = []; // 화면에서만 제거할 rowKey
			const serverProcessIds = []; // 서버에 삭제 요청할 processId 목록
			for (const key of rowKeysToDelete) {
				// 우선 해당 키가 생성된(신규) 행인지 확인
				const isCreated = createdRows.some(r => r && (String(r.rowKey) === String(key) || String(r.processId) === String(key)));
				if (isCreated) {
					uiOnlyKeys.push(key);
					continue;
				}
				let row = null;
				if (typeof grid2.getRow === 'function') row = grid2.getRow(key);
				if (!row) row = data.find(d => d && (String(d.rowKey) === String(key) || String(d.processId) === String(key)));
				// 빈 행 판단: 모든 필드가 비어있거나 processId가 없으면 UI에서만 삭제
				const vals = row ? Object.values(row) : [];
				const allEmpty = !row || vals.length === 0 || vals.every(v => v === null || v === undefined || (typeof v === 'string' && v.trim() === ''));
				if (allEmpty || !row || !row.processId) {
					uiOnlyKeys.push(key);
				} else {
					serverProcessIds.push(String(row.processId));
				}
			}

			// UI에서만 제거할 행들 삭제
			let removedUi = 0;
			if (uiOnlyKeys.length > 0) {
				for (const k of uiOnlyKeys) {
					try {
						if (typeof grid2.removeRow === 'function') { grid2.removeRow(k); removedUi++; continue; }
						if (typeof grid2.deleteRow === 'function') { grid2.deleteRow(k); removedUi++; continue; }
						const newData = data.filter(r => !(r && (String(r.rowKey) === String(k) || String(r.processId) === String(k))));
						grid2.resetData(newData);
						removedUi++;
					} catch (e) { console.warn('UI 전용 행 삭제 실패', k, e); }
				}
			}

			// 서버에 삭제 요청 보낼 processId가 있으면 기존 로직 수행
			if (serverProcessIds.length > 0) {
				// processId가 있는 항목이 포함된 경우에만 삭제 확인창 표시
				/*if (!confirm('서버에서 실제로 삭제할 항목이 포함되어 있습니다. 선택한 항목을 삭제하시겠습니까?')) return;
				fetch('/masterData/processCode/modify', {
					method: 'POST',
					credentials: 'same-origin',
					headers: {
						[csrfHeader]: csrfToken,
						'Content-Type': 'application/json'
					},
					// 서버는 RequestBody로 Map<String,Object>를 기대하므로
					// 배열 자체가 아닌 { processCodes: [...] } 형태로 보냅니다.
					body: JSON.stringify({ processCodes: serverProcessIds })
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
					if (typeof parsed === 'string') {
						if (!okTexts.includes(parsed.trim().toLowerCase())) throw new Error('Unexpected response: ' + parsed);
					} else if (!(parsed && (parsed.status === 'success' || okTexts.includes((parsed.message||'').toString().toLowerCase())))) {
						throw new Error('삭제 실패: ' + JSON.stringify(parsed));
					}
					// 서버 삭제 성공 시 그리드 재조회
					processCodeGridAllSearch();
				})
				.catch(err => {
					console.error('삭제 중 오류', err);
					try { alert('삭제 중 오류가 발생했습니다. ' + (err && err.message ? err.message : '')); } catch (e) {}
				});*/
			} else {
				if (removedUi > 0) alert('추가한 행을 화면에서만 삭제했습니다. (DB에는 반영되지 않음)');
			}
		} catch (e) {
			console.error('삭제 처리 중 오류', e);
			try { alert('삭제 처리 중 오류가 발생했습니다. ' + (e && e.message ? e.message : '')); } catch (err) {}
		}
	
});

//라우트 조회 상세 - 공정단계 그리드에서 단계삭제
const deleteRouteStepRowBtn = document.getElementById('deleteRouteStepRowBtn');
deleteRouteStepRowBtn.addEventListener('click', async function() {
	// 체크된 rowKey들 수집
	let rowKeysToDelete = [];
	try {
		if (typeof grid3.getCheckedRowKeys === 'function') {
			rowKeysToDelete = grid3.getCheckedRowKeys() || [];
		} else if (typeof grid3.getCheckedRows === 'function') {
			const checkedRows = grid3.getCheckedRows() || [];
			rowKeysToDelete = checkedRows.map(r => r && (r.rowKey || r.routeStepId)).filter(Boolean);
		}
	} catch (e) {
		console.warn('체크된 행 조회 실패', e);
	}
	if (rowKeysToDelete.length === 0) {
		alert('삭제할 공정단계를 선택(체크)해주세요.');
		return;
	}
	if (!confirm(`${rowKeysToDelete.length}개의 공정단계를 삭제하시겠습니까?`)) {
		return;
	}
	// 간결한 방식으로 각 rowKey로부터 routeStepId(또는 식별 가능한 ID)를 수집
	const getAllData = () => (typeof grid3.getData === 'function' ? grid3.getData() : (grid3.data || []));
	const data = getAllData();

	// 신규로 추가된(저장되지 않은) 행은 grid3.getModifiedRows().createdRows에 존재할 수 있음
	const modified = (typeof grid3.getModifiedRows === 'function') ? (grid3.getModifiedRows() || {}) : {};
	const createdRows = Array.isArray(modified.createdRows) ? modified.createdRows : [];

	const uiOnlyKeys = [];
	const routeStepIdsToDelete = [];

	for (const key of rowKeysToDelete) {
		// 먼저 생성된(저장되지 않은) 행인지 확인
		const isCreated = createdRows.some(r => r && (String(r.rowKey) === String(key) || String(r.routeStepId) === String(key)));
		if (isCreated) {
			uiOnlyKeys.push(key);
			continue;
		}

		let row = null;
		if (typeof grid3.getRow === 'function') row = grid3.getRow(key);
		if (!row) row = data.find(d => d && (String(d.rowKey) === String(key) || String(d.routeStepId) === String(key)));
		if (row && row.routeStepId) {
			routeStepIdsToDelete.push(String(row.routeStepId));
		} else {
			// 식별 불가 항목은 UI에서만 제거 시도
			uiOnlyKeys.push(key);
		}
	}

	// UI 전용 키들부터 제거
	if (uiOnlyKeys.length > 0) {
		for (const k of uiOnlyKeys) {
			try {
				if (typeof grid3.removeRow === 'function') { grid3.removeRow(k); continue; }
				if (typeof grid3.deleteRow === 'function') { grid3.deleteRow(k); continue; }
				const newData = data.filter(r => !(r && (String(r.rowKey) === String(k) || String(r.routeStepId) === String(k))));
				grid3.resetData(newData);
			} catch (e) { console.warn('UI 전용 행 삭제 실패', k, e); }
		}
	}

	// 서버 삭제 대상이 없으면 여기서 종료
	if (routeStepIdsToDelete.length === 0) {
		if (uiOnlyKeys.length > 0) {
			alert('추가한 행을 화면에서만 삭제했습니다. (DB에는 반영되지 않음)');
		} else {
			alert('삭제할 공정단계를 정확히 선택해주세요.');
		}
		return;
	}

	try {
		if (!confirm(`${routeStepIdsToDelete.length}개의 공정단계를 삭제하시겠습니까?`)) return;
		const response = await fetch(apiUrl(`masterData/processStep/delete`), {
			method: 'POST',
			credentials: 'same-origin',
			headers: {
				[csrfHeader]: csrfToken,
				'Content-Type': 'application/json'
			},
			body: JSON.stringify(routeStepIdsToDelete)
		});
		if (!response.ok) {
			throw new Error(`HTTP error! status: ${response.status}`);
		}
		const resultText = await response.text();
		if (resultText === 'success') {
			alert('삭제 완료');
			const routeId = document.getElementById('modalRouteId').value;//라우트 ID
			processStepSearch(routeId);//신규라우트 모달 그리드 - 공정단계 조회
		} else {
			alert('삭제 실패: ' + resultText);
		}
	} catch (error) {
		console.error('삭제오류', error);
		alert('삭제 중 오류가 발생했습니다. 콘솔 로그를 확인하세요.');
	}
});

//모달 움직이게 하기
const modalHeader = document.querySelector(".modal-header");
const modalDialog = document.querySelector(".modal-dialog");
let isDragging = false;
let mouseOffset = { x: 0, y: 0 };
let dialogOffset = { left: 0, right: 0 };

modalHeader.addEventListener("mousedown", function (event) {
	isDragging = true;
	mouseOffset = { x: event.clientX, y: event.clientY };
	dialogOffset = {
		left: modalDialog.style.left === '' ? 0 : Number(modalDialog.style.left.replace('px', '')),
		right: modalDialog.style.top === '' ? 0 : Number(modalDialog.style.top.replace('px', ''))
	}
});

document.addEventListener("mousemove", function (event) {
	if (!isDragging) {
		return;
	}
	let newX = event.clientX - mouseOffset.x;
	let newY = event.clientY - mouseOffset.y;

	modalDialog.style.left = `${dialogOffset.left + newX}px`
	modalDialog.style.top = `${dialogOffset.right + newY}px`
});

document.addEventListener("mouseup", function () {
	isDragging = false;
});