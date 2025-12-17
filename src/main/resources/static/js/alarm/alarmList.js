// 전역변수
let alarmGrid;
const startDate = document.getElementById('startDate');
const endDate = document.getElementById('endDate');
const today = new Date();


function formatDateToYMD(date) {
    const year  = date.getFullYear();
    const month = String(date.getMonth() + 1).padStart(2, '0'); // 0~11 → 1~12
    const day   = String(date.getDate()).padStart(2, '0');
    return `${year}-${month}-${day}`;
}

// 오늘 날짜 기준으로 endDate와 startDate 설정
function setDateRange() {
    const today = new Date();
    const todayStr = formatDateToYMD(today);   // 로컬 기준 오늘 날짜
	
    // endDate 초기값 오늘 날짜, max설정 
    endDate.value = today.toISOString().split('T')[0];
	endDate.max = todayStr;
	
    // startDate 초기값 30일 전 날짜
    const thirtyDaysAgo = new Date(today);
    thirtyDaysAgo.setDate(today.getDate() - 30);
    startDate.value = formatDateToYMD(thirtyDaysAgo);
}

// endDate 변경 시 startDate 최대값 업데이트
endDate.addEventListener('change', function() {
    if (startDate.value > endDate.value) {
        startDate.value = '';
    }
    startDate.max = endDate.value;
});

// startDate 변경 시 endDate 최소값 업데이트
startDate.addEventListener('change', function() {
    endDate.min = startDate.value;
});

// 문서로드 
document.addEventListener('DOMContentLoaded', async () => {
	// 날짜범위 지정
	setDateRange();
	const alarmData = await getAlarmData();
	// 알림그리드생성
	initAlarmGrid();
	alarmGrid.resetData(alarmData);
	// 조회한 알림에대해 읽음처리
	updateAlarmData();
	// 헤더 종에 새 알림 표시 제거
	const badgeBell = document.getElementById("badge_bell");
	badgeBell.style.display = "none";
	//조회버튼 클릭시 새로조회
	document.getElementById('searchbtn').addEventListener('click', getAlarmData);
})

// 알림데이터조회
async function getAlarmData() {
	const params = new URLSearchParams({
	    startDate: startDate.value || '',
	    endDate: endDate.value || ''
	});
	
    try {
        const response = await fetch(`/alarm/list?${params}`, {
            method: "GET",
            headers: {
                [csrfHeader]: csrfToken,
                'Content-Type': 'application/json'
            }
        });
        
        if (!response.ok) {
            throw new Error("알림 데이터 로드 실패");
        }
        
        const alarmData = await response.json();
        console.log("알림 데이터:", alarmData);
        
        // 그리드에 데이터 적용
        if (alarmGrid) {
            alarmGrid.resetData(alarmData);  // 또는 render()
        }
        
        return alarmData;
        
    } catch (error) {
        console.error("알림 조회 오류:", error);
        alert("알림 목록을 불러오지 못했습니다.");
        return [];
    }
}

// 알림데이터조회
async function updateAlarmData() {
	const params = new URLSearchParams({
	    startDate: startDate.value || '',
	    endDate: endDate.value || '',
		alarmStatus: "Y"
	});
	
    try {
        const response = await fetch(`/alarm/list?${params}`, {
            method: "POST",
            headers: {
                [csrfHeader]: csrfToken,
                'Content-Type': 'application/json'
            }
        });
        
        if (!response.ok) {
            throw new Error("알림 데이터 상태변경 실패");
        }
        
        const alarmStatusData = await response.json();
        console.log("알림 데이터 상태변경 완료:", alarmStatusData);
        
    } catch (error) {
        console.error("알림 상태변경 오류:", error);
        alert("알림 상태를 변경 할 수 없습니다.");
    }
}

// 그리드 초기화
function initAlarmGrid() {
    const Grid = tui.Grid;

    alarmGrid = new Grid({
        el: document.getElementById('alarmGrid'),
        bodyHeight: 400,
        rowHeight: 40,
        scrollX: false,
        scrollY: true,
		rowHeaders: ['rowNum'],
		pageOptions: {
		    useClient: true,
		    perPage: 5
		},
        columns: [
//            { header: 'ID', name: 'alarmId', width: 70, align: 'center' },
            { header: '메시지', name: 'alarmMessage', minWidth: 300,
			  formatter: ({ value, row }) => {
				    const status = row.alarmStatus; // 숨긴 컬럼 값 사용
				    if (status === 'N') {
				        return `
		                    <span style="display:inline-flex;align-items:center;justify-content:center;gap:6px;width:100%;text-align:center;">
				                <span>${value ?? ''}</span>
				                <span style="color:red;font-weight:bold;">NEW</span>
				            </span>
				        `;
				    }
				    return value ?? '';
			  }	
			},
            { header: '상태', name: 'alarmStatus', width: 80, align: 'center', hidden: true },
			{
			    header: '이동',
			    name: 'alarmLink',
			    width: 100,
			    align: 'center',
			    renderer: {
			        type: class LinkButtonRenderer {
			            constructor(props) {
			                const el = document.createElement('button');
			                el.type = 'button';
			                el.className = 'btn btn-sm btn-outline-primary';
			                el.textContent = '이동';
			                el.addEventListener('click', (ev) => {
			                    ev.stopPropagation();
			                    const link = props.value;
			                    if (link) {
			                        window.location.href = link;
			                    }
			                });
			                this.el = el;
			            }
			            getElement() {
			                return this.el;
			            }
			            render(props) {
			                this.el.disabled = !props.value;
			            }
			        }
			    }
			},
            {
                header: '등록일시',
                name: 'createdDate',
                width: 180,
                formatter: ({ value }) => {
                    if (!value) return '';
                    // "2025-12-09T22:03:12.005569" → "2025-12-09 22:03:12"
                    const [date, time] = value.split('T');
                    return `${date} ${time.substring(0, 8)}`;
                }
            }
        ]
    });
}

// --------------------------------------------------------------
// 그리드 랭기지 설정
function gridLangSet(grid) {
	// 1. 그리드 한글 언어셋 설정 (필터 및 각종 텍스트 한글화)
	grid.setLanguage('ko', {
	    display: {
	        noData: '데이터가 없습니다.',
	        loadingData: '데이터를 불러오는 중입니다.',
	        resizeHandleGuide: '마우스 드래그를 통해 너비를 조정할 수 있습니다.',
	    },
	    net: {
	        confirmCreate: '생성하시겠습니까?',
	        confirmUpdate: '수정하시겠습니까?',
	        confirmDelete: '삭제하시겠습니까?',
	        confirmModify: '저장하시겠습니까?',
	        noDataToCreate: '생성할 데이터가 없습니다.',
	        noDataToUpdate: '수정할 데이터가 없습니다.',
	        noDataToDelete: '삭제할 데이터가 없습니다.',
	        noDataToModify: '수정할 데이터가 없습니다.',
	        failResponse: '데이터 요청 중에 에러가 발생하였습니다.'
	    },
	    filter: {
	        // 문자열 필터 옵션
	        contains: '포함',
	        eq: '일치',
	        ne: '불일치',
	        start: '시작 문자',
	        end: '끝 문자',
	        
	        // 날짜/숫자 필터 옵션
	        after: '이후',
	        afterEq: '이후 (포함)',
	        before: '이전',
	        beforeEq: '이전 (포함)',

	        // 버튼 및 기타
	        apply: '적용',
	        clear: '초기화',
	        selectAll: '전체 선택'
	    }
	});
}

