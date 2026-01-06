/**
	일정게시판 JavaScript 
**/

//const csrfToken = document.querySelector('meta[name="_csrf_token"]')?.content;
//const csrfHeaderName = document.querySelector('meta[name="_csrf_headerName"]')?.content;

const currentUserId = document.getElementById('currentUserId')?.value;
const currentUserName = document.getElementById('currentUserName')?.value;

let picker_list = null;

document.addEventListener('DOMContentLoaded', function() {
	
	//일정목록 데이트피커 객체 생성
	picker_list = tui.DatePicker.createRangePicker({
		language: 'ko',
	    startpicker: {
	        date: today,
	        input: '#startpicker-input-list',
	        container: '#startpicker-container-list'
	    },
	    endpicker: {
			language: 'ko',
	        date: nextYear,
	        input: '#endpicker-input-list',
	        container: '#endpicker-container-list'
	    },
	    format: 'YYYY-MM-dd'
	});
	
	// ---------------------------------------------------------------
	// 초기 TUI 그리드 불러오기
	getScheduleData();
	
	// Search버튼 눌러 그리드 불러오기
	const searchForm = document.getElementById('schedule-search-form');
	searchForm.addEventListener('submit', function async (event) {
		event.preventDefault();
		getScheduleData();
	});
	
	// 시작날자, 끝날자 받아서 data불러오기
	function getScheduleData(){
		const startDate = picker_list.getStartDate();
		const endDate = picker_list.getEndDate();
		
		const params = new URLSearchParams({
			startDate: formatLocalDateTime(startDate)
			, endDate: formatLocalDateTime(endDate)
		});
				
		fetch(apiUrl(`api/schedules?${params.toString()}`), {method: 'GET'})
		.then(response => {
			if (!response.ok) throw new Error(response.text());
			return response.json();  //JSON 파싱
		})
		.then(data => { // response가 ok일때
//			console.log(data);
			initGrid(data);
		}).catch(error => {
			console.error('에러', error)
			alert("데이터 조회 실패");
		});
	}
	
	// ---------------------------------------------------------------
	// 일정등록 버튼이벤트
	document.getElementById('add-schedule').addEventListener('click', () => {
		openScheduleModal('add');
	})
	
	
	
	
	
	
		
});// DOM로드 끝

let grid = null;
// 그리드 불러오기 함수
function initGrid(data) {
	const Pagination = tui.Pagination;
	
	if(!grid){
		createGrid();
	} else {
		grid.destroy();
		createGrid();
	}
	
	// 1. 그리드 한글 언어셋 설정 (필터 및 각종 텍스트 한글화)
	tui.Grid.setLanguage('ko', {
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
	
	function createGrid() {
		grid = new tui.Grid({
			el: document.getElementById("grid"),
			editable: true,
			columns: [
				{
					header: '일정제목',
					name: 'scheduleTitle',
					sortable: true,
					width: 150,
					filter: 'text'
				},
				{
					header: '종류',
					name: 'scheduleType',
					sortable: true,
					width: 70,
					filter: 'select'
					, formatter: (row) => {
						const typeMap = {
							'share': '공유'
							, 'company': '회사'
							, 'private': '개인'
						}
						
						return typeMap[row.value] || row.value;
					}
				},
				{
					header: '일정시작',
					name: 'scheduleStart',
					sortable: true,
					width:130,
					filter: {
						type: 'date', options: {format: 'yyyy.MM.dd'}
					}
				},
				{
					header: '일정마감',
					name: 'scheduleFinish',
					sortable: true,
					width:130,
					filter: {
						type: 'date', options: {format: 'yyyy.MM.dd'}
					}
				},
				{
					header: '작성자'
					, name: 'empName'
					, sortable: true
					, filter: 'text'
					, width: 80
				},
				{
					header: '내용',
					name: 'scheduleContent',
					sortable: true,
					filter: 'text'
				},
				{
					header: '상세'
					, name: "btn"
					, width: 100 // 너비 설정
					, align: "center"
					, formatter: (cellInfo) => "<button type='button' class='btn-detail btn-primary btn-sm' data-row='${cellInfo.rowKey}' >상세</button>"
				}
			],
			rowHeaders: ['rowNum'],
			pageOptions: {
				useClient: true,
				perPage: 5
			},
			columnOptions: {
			    resizable: true
			}
		});
		grid.resetData(data);
		grid.sort('scheduleStart', true); // true: 오름차순 false: 내림차순
	}
	
	
	// 상세보기 버튼 이벤트
	grid.on("click", (event) => {
		if(event.columnName == "btn") {
			const target = event.nativeEvent.target;
			if (target && target.tagName === "BUTTON") {
				
				const rowData = grid.getRow(event.rowKey);
				const scheduleId = rowData.scheduleId;
				
				fetch(apiUrl(`api/schedules/${scheduleId}`), {method: 'GET'})
				.then(response => {
					if (!response.ok) throw new Error(response.text());
					return response.json();  //JSON 파싱
				})
				.then(data => { // response가 ok일때
					openScheduleModal("edit", data);
				}).catch(error => {
					console.error('에러', error)
					alert("데이터 조회 실패");
				});
			}
		}
	});
}
































