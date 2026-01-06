/**
	일정 모달 JavaScript 
**/

let picker = null;
let isProgrammaticChange = false; //날자 세팅
let checkedUpEmpList;
// 일정등록 데이트피커 객체 생성
function createRangePicker() {
	picker = tui.DatePicker.createRangePicker({
		language: 'ko',
	    startpicker: {
	        date: today,
	        input: '#startpicker-input',
	        container: '#startpicker-container'
	    },
	    endpicker: {
	        date: nextDay,
	        input: '#endpicker-input',
	        container: '#endpicker-container'
	    },
	    format: 'YYYY-MM-dd HH:mm',
		timepicker: {
            layoutType: 'tab',
            inputType: 'spinbox',
			showMeridiem: false
        }
	});
	
	picker.on('change:start', () => {
		if (isProgrammaticChange) return;
		validateRangeWithAllday();
	});

	// end 날짜 변경 시 검증
	picker.on('change:end', () => {
		if (isProgrammaticChange) return;
		validateRangeWithAllday();
	});
}

// 종일 일정이 아닌경우 start, end 같은 날로
function syncEndDateToStartDate() {
	const startDate = picker.getStartDate();   // Date 객체 또는 null
	const endDate   = picker.getEndDate();     // Date 객체 또는 null

  // start 가 아직 없으면 아무것도 못 하니까 그냥 종료
	if (!(startDate instanceof Date)) {
		return;
	}

	let newEnd;

	if (endDate instanceof Date) {
		// 기존 end 의 시간은 유지하고 날짜(년월일)만 start 기준으로 맞추기
		newEnd = new Date(endDate);
		newEnd.setFullYear(
			startDate.getFullYear(),
			startDate.getMonth(),
			startDate.getDate()
	    );
	} else {
	    // end 가 null 인 경우: start 를 그대로 복사해서 end 로 사용
	    newEnd = new Date(startDate);
	}

	picker.setEndDate(newEnd);
}

// 두 Date가 같은 '날짜(년/월/일)'인지 확인
function isSameDay(d1, d2) {
	// 둘 중 하나라도 값이 없으면 비교 불가 → false 리턴
	if (!(d1 instanceof Date) || !(d2 instanceof Date)) {
		return false;
	}

	return d1.getFullYear() === d2.getFullYear() &&
			d1.getMonth()    === d2.getMonth() &&
			d1.getDate()     === d2.getDate();
}

// allday 상태를 보고 start/end 검증하는 함수
function validateRangeWithAllday() {
	
	const alldayCheck = document.getElementById('all-day-checkbox');
	const isAllDay = alldayCheck.checked;
	const startDate = picker.getStartDate();
	const endDate   = picker.getEndDate();

	if (!isAllDay) { // 종일 아닐 때만 체크

	    if (!isSameDay(startDate, endDate)) {
			// end 를 start 날짜로 맞추기 (시간까지 통일하려면 new Date(startDate) 써도 됨)
			const fixedEnd = new Date(endDate);
			fixedEnd.setFullYear(
				startDate.getFullYear(),
			 	startDate.getMonth(),
				startDate.getDate()
			);
		
			picker.setEndDate(fixedEnd);
		
			alert('종일 일정이 아닐 때는 시작일과 종료일이 같은 날이어야 합니다.\n종료일을 시작일로 변경했습니다.');
		}
	} else {
//		const newStart = new Date(startDate);
//		const newEnd   = new Date(endDate);
//	
//		newStart.setHours(0, 0, 0, 0); 
//		newEnd.setHours(23, 59, 59, 999); 
//	
//		picker.setStartDate(newStart);
//		picker.setEndDate(newEnd);
	}
}

document.addEventListener('DOMContentLoaded', function() {
	
	createRangePicker();
	
	const alldayCheck = document.getElementById('all-day-checkbox');

	// 종일 체크박스 변경 이벤트
	alldayCheck.addEventListener('change', function() {
		const alldayYN = document.getElementById('all-day-checkbox-value');
		alldayCheck.checked ? alldayYN.value = "Y" : alldayYN.value = "N";
		
		if (!alldayCheck.checked) {
			syncEndDateToStartDate();
		}
	});
	
	//일정등록 모달 등록, 수정버튼 이벤트
	const addScheduleForm = document.getElementById('add-schedule-form')
	const addScheduleBtn = document.getElementById('add-schedule-btn');
	
	addScheduleForm.addEventListener('submit', async function(event) {
		event.preventDefault(); // 기본제출 막기

		// 일정 등록 일때 구분
		if(addScheduleBtn.value == 'add') {
			// 일정등록폼 지정
			const form = event.target;
			// 일정등록 데이터 생성
			const scheduleWithRepeatDTO = {
				scheduleDTO: {
					scheduleTitle: form.scheduleTitle.value
					, createdUser: form.createdUser.value
					, scheduleType: form.scheduleType.value
					, scheduleStart: form.scheduleStart.value
					, scheduleFinish: form.scheduleFinish.value
					, alldayYN: form.alldayYN.value
					, recurrenceType: form.recurrenceType.value
					, scheduleContent: form.scheduleContent.value
				},
				repeatScheduleDTO: null,
				sharedEmpList:[]
			};
			// 반복일정 정보 있을 경우에만 반복일정 값 세팅
			if(form.recurrenceType && form.recurrenceType.value !== 'none') {
				const byDayArr = Array.from(form.querySelectorAll('input[name="byDay"]:checked'))
				    .map(el => el.value); 
				scheduleWithRepeatDTO.repeatScheduleDTO = {
					recurrenceType: form.recurrenceType.value
					, interval: parseInt(form.interval?.value) || 1
					, byDay: byDayArr.length ? byDayArr : null
					, monthDay: form.monthDay?.value ? parseInt(form.monthDay.value) : null
					, yearMonth: form.yearMonth?.value ? parseInt(form.yearMonth.value) : null
					, yearDay: form.yearDay?.value ? parseInt(form.yearDay.value) : null
					, recurrenceEndType: form.recurrenceEndType?.value || null
					, recurrenceUntil: form.recurrenceUntil?.value || null
				}
			}
			
			// 공유일정일경우 공유자명단 데이터
			if(form.scheduleType.value === 'share' && checkedUpEmpList && checkedUpEmpList.length > 0) {
				scheduleWithRepeatDTO.sharedEmpList = checkedUpEmpList;
			}
			
			// 폼데이터 지정
//			const formData = new FormData(addScheduleForm);
//			if (!addScheduleForm) {
//			    alert('add-schedule-form 폼 엘리먼트가 존재하지 않습니다!');
//			    return;
//			}
			// 등록일때는 scheduleId의 name값 제거 [jpa에 id가 null이어야 자동생성가능]
//			addScheduleForm.scheduleId.removeAttribute('name');
			// scheduleType이 share일 때공유자명단을 formData에 추가
			const scheduleType = document.getElementById('schedule-type');
			if (scheduleType.value === 'share') {
				if (!checkedUpEmpList || checkedUpEmpList.length === 0) {
					alert('공유자 명단을 1명 이상 선택해야 합니다.');
					return;
				} 
//				else {
//					formData.append('sharedEmpList', JSON.stringify(checkedUpEmpList));
//				}
			}
			
			fetch('/main/schedule', {
				method: 'POST'
				, headers: {
<<<<<<< HEAD
					[csrfHeaderName]: csrfToken
					, 'Content-Type': 'application/json' 
=======
					[csrfHeader]: csrfToken
>>>>>>> refs/remotes/origin/develop_update
				}
				, body: JSON.stringify(scheduleWithRepeatDTO)
			})
			.then(response => {
				if (!response.ok) {
					throw new Error(response.msg);
				}
				return response.json();  //JSON 파싱
			})
			.then(response => { // response가 ok일때
				alert(response.msg);
				location.reload();
			}).catch(error => {
				alert("제목, 시작,종료 일시, 내용은 필수입력 사항입니다.");
			});
			
		} else if(addScheduleBtn.value == 'edit') {
			
			if(confirm("수정하시겠습니까?")){
				// 폼데이터 지정
				const formData = new FormData(addScheduleForm);
				if (!addScheduleForm) {
				    alert('add-schedule-form 폼 엘리먼트가 존재하지 않습니다!');
				    return;
				}
				// 수정일때는 scheduleId 포함해서 보내기
				addScheduleForm.scheduleId.setAttribute('name', 'scheduleId');
				// scheduleType이 share일 때공유자명단을 formData에 추가
				const scheduleType = document.getElementById('schedule-type');
				if (scheduleType.value === 'share') {
					if (!checkedUpEmpList || checkedUpEmpList.length === 0) {
						alert('공유자 명단을 1명 이상 선택해야 합니다.');
						return;
					} else {
						formData.append('sharedEmpList', JSON.stringify(checkedUpEmpList));
					}
				}
				
				fetch('/main/schedule', {
					method: 'PATCH'
					, headers: {
						[csrfHeader]: csrfToken
					}
					, body: formData
				})
				.then(response => {
					if (!response.ok) {
						throw new Error(response.msg);
					}
					return response.json();  //JSON 파싱
				})
				.then(response => { // response가 ok일때
					alert(response.msg);
					location.reload();
				}).catch(error => {
					alert("수정에 실패하였습니다.");
				});
			}
		}
	}); // 일정등록, 수정함수 끝
	
	// 일정조회 - 삭제버튼 이벤트
	document.getElementById('delete-schedule-btn').addEventListener('click', function () {
		if(confirm("삭제하시겠습니까 ?")) {
			//삭제요청보내기
			fetch('/main/schedule', {
				method: 'DELETE'
				, headers: {
					[csrfHeader]: csrfToken
				}
				, body: new FormData(addScheduleForm)
			})
			.then(response => {
				if (!response.ok) {
					throw new Error(response.msg);
				}
				return response.json();  //JSON 파싱
			})
			.then(response => { // response가 ok일때
				alert(response.msg);
				location.reload();
			}).catch(error => {
				alert("삭제에 실패하였습니다.");
			});
		}
	}); //일정조회 - 삭제 버튼 이벤트 끝
		
});// DOM로드 끝


// ----------------------------------------------------------
// 일정 타입셀렉트박스 변경이벤트 추가
document.getElementById('schedule-type').addEventListener('change', (event) => {
	const shareEl = document.getElementById('shareField');
	// 선택된 공유자 목록,명단 초기화
	setSharers([]);
	
	if(event.target.value == 'share'){
		shareEl.style.display = 'block';
	} else {
		shareEl.style.display = 'none';
	}
});
// 모달열기함수
async function openScheduleModal(mode, data = null) {
	const modal = new bootstrap.Modal(document.getElementById('add-schedule-modal'));
	const form = document.getElementById('add-schedule-form');
	const modalTitle = document.getElementById('modalCenterTitle');
	const deleteBtn = document.getElementById('delete-schedule-btn');
	const submitBtn = document.getElementById('add-schedule-btn');
	const select = document.getElementById('schedule-type');
	const createdUserName = document.getElementById('createdUserName');
	const alldayCheckbox = document.getElementById('all-day-checkbox');
	const organizeInput = document.getElementById('schedule-sharer');
	const organizeBtn = document.getElementById('select-sharer-btn');
	const shareEl = document.getElementById('shareField');
//	const createdUser = document.getElementById('schedule-writer')
//	const startpickerInput = document.getElementById('startpicker-input');
//	const endpickerInput = document.getElementById('endpicker-input');
//	const schedueId = document.getElementById('schedule-id');
	
	const sp = picker.getStartpicker(); 
	const ep = picker.getEndpicker();
	
	// -------------------------------------------------------------------
	// -------------------------------------------------------------------
	// 모달이 열릴때 add, edit 구분
	if(mode === 'add') {
//		form.reset(); // 폼 입력값 초기화
		deleteBtn.disabled = false;
		submitBtn.disabled = false;
		submitBtn.classList.remove('d-none');
		//폼요소 입력가능하게 변경
		Array.from(form.elements).forEach(el => {
		    if (el.tagName === 'INPUT' || el.tagName === 'TEXTAREA') {
		        el.readOnly = false;
		    } 
			if (el.tagName === 'SELECT' || el.type === 'checkbox') {
		        el.disabled = false;
		    }
			createdUserName.readOnly = true;
		});
		
		modalTitle.textContent = '일정등록';
		deleteBtn.classList.add('d-none');
	    submitBtn.textContent = '등록';
		submitBtn.value ='add';
		
		form.scheduleId.value = '';
		form.scheduleTitle.value = '';
		createdUserName.value = currentUserName || '';
		form.createdUser.value = currentUserId || '';
		form.scheduleContent.value = '';
		
		sp.enable && sp.enable(); //시작날자 선택가능
		ep.enable && ep.enable(); //종료날자 선택가능
		
		// 날짜 초기값
		isProgrammaticChange = true;
		picker.setStartDate(new Date(today));
		picker.setEndDate(new Date(nextDay));
		isProgrammaticChange = false;
		
		// 등록모달은 개인으로 기본값설정
		select.value = 'share';
		shareEl.style.display = "block";
		
		// 종일 체크 해제
		alldayCheckbox.checked = true
		form.alldayYN.value = "Y";
		
		organizeInput.disabled = true;
		organizeInput.value = '';
		organizeBtn.disabled = false;

	} else if (mode === 'edit' && data) {
		modalTitle.textContent = '일정조회';
		deleteBtn.classList.remove('d-none');
		submitBtn.classList.remove('d-none');
	    submitBtn.textContent = '수정';
		submitBtn.value ='edit';
		form.scheduleId.value = data.scheduleId || '';
		form.scheduleTitle.value = data.scheduleTitle || '';
		createdUserName.value = data.empName || '';
		form.createdUser.value = data.createdUser || '';
		
		//셀렉트박스 데이터값 설정 후 체인지 이벤트 실행
		select.value = data.scheduleType;
		select.dispatchEvent(new Event('change'));
		
		if(data.scheduleType == 'share') {
			// 만약 셀렉트 scheduleType이 share라면 해당 공유자 명단 불러오기
			const oldSharers = await checkSharers(data.scheduleType, data.scheduleId);
			// 등록할때 쓰이는 checkedUpEmpList에 값 저장
			checkedUpEmpList = oldSharers.map(item => ({
									empId: item.empId
									,empName: item.empName
								}));
			// 일정조회 모달의 공유자 인풋에 보여주기
			setSharers(oldSharers);
		}
		
		// 날짜 초기값
		isProgrammaticChange = true;
		picker.setStartDate(data.scheduleStart ? new Date(data.scheduleStart) : today);
		picker.setEndDate(data.scheduleFinish ? new Date(data.scheduleFinish): nextDay);
		isProgrammaticChange = false;
		
		// 종일 체크
		alldayCheckbox.checked = data.alldayYN === 'Y'; 
		form.alldayYN.value = data.alldayYN; // hidden value
		form.scheduleContent.value = data.scheduleContent || '';
		
		if (data.createdUser !== currentUserId) {
		    // 권한 없음: 조직선택, 삭제, 수정 버튼 비활성화
			organizeBtn.disabled = true;
		    deleteBtn.disabled = true;
			deleteBtn.classList.add('d-none');
		    submitBtn.disabled = true;
			submitBtn.classList.add('d-none');
			// 데이트피커 비활성화
			sp.enable && sp.disable();
			ep.enable && ep.disable();
			
		    // 폼 전체의 인풋/셀렉트/체크박스 등을 읽기 전용으로 만들기
		    Array.from(form.elements).forEach(el => {
		        if (el.tagName === 'INPUT' || el.tagName === 'TEXTAREA') {
		            el.readOnly = true;
		        }
				if (el.tagName === 'SELECT' || el.type === 'checkbox') {
		            el.disabled = true;
		        }
		    });
		} else {
		    // 권한 있는 사용자에게는 모든 기능 활성화
			organizeBtn.disabled = false;
		    deleteBtn.disabled = false;
		    submitBtn.disabled = false;
			
			//데이트피커 비활성화
			sp.enable && sp.enable();
			ep.enable && ep.enable();
			
		    Array.from(form.elements).forEach(el => {
		        if (el.tagName === 'INPUT' || el.tagName === 'TEXTAREA') {
		            el.readOnly = false;
		        } 
				if (el.tagName === 'SELECT' || el.type === 'checkbox') {
		            el.disabled = false;
		        }
				createdUserName.readOnly = true;
		    });
		}
	}
	
	modal.show();
} // 모달열기 함수 끝

var today = new Date();
var nextDay = new Date();
var nextYear = new Date(today);

nextYear.setFullYear(nextYear.getFullYear() + 1);
nextDay.setDate(nextDay.getDate() + 1);

function pad(n) {
    return n < 10 ? '0' + n : n;
}

function formatDateTime(date) {
	var year   = date.getFullYear();
	const month = (date.getMonth() + 1).toString().padStart(2, '0');
  	const day = date.getDate().toString().padStart(2, '0');
	var hour   = pad(date.getHours());
	var minute = pad(date.getMinutes());

	var formatted = year + '-' + month + '-' + day + ' ' + hour + ':' + minute;
	
	return formatted;
}

function formatLocalDateTime(date) {
	var year   = date.getFullYear();
	const month = (date.getMonth() + 1).toString().padStart(2, '0');
  	const day = date.getDate().toString().padStart(2, '0');
	var hour   = pad(date.getHours());
	var minute = pad(date.getMinutes());
	var second = pad(date.getSeconds());

	var formatted = year + '-' + month + '-' + day + 'T' + hour + ':' + minute + ':' + second;
	
	return formatted;
}

function formatDate(date) {
	const year = date.getFullYear();
	const month = (date.getMonth() + 1).toString().padStart(2, '0');
	const day = date.getDate().toString().padStart(2, '0');
	return `${year}-${month}-${day}`;
}

// ------------------------------------------------------------------
// 조직도 모달 함수

//모달열기
function openOrgModal() {
    const modal = new bootstrap.Modal(document.getElementById('organization-modal'));
    modal.show();

    // 모달 열린 후 Grid layout refresh
    setTimeout(() => {
        if (treeGrid) treeGrid.refreshLayout && treeGrid.refreshLayout();
    }, 200);
}

// 모달닫기
function closeOrgModal() {
    const modalEl = document.getElementById('organization-modal');
    const modal = bootstrap.Modal.getInstance(modalEl);
    modal.hide();
}

let toastTreeData = null;
// 조직도 불러오기
async function getOrganizationChart() {
	await fetch(`/api/schedules/organizationChart`, {method: 'GET'})
	.then(response => {
		if (!response.ok) throw new Error(response.text());
		return response.json();  //JSON 파싱
	}).then(async data => {
		toastTreeData = await buildDeptTree(data.data);
		await renderOrgGrid();
	}).catch(error => {
		console.error('에러', error)
		alert("조직 데이터 조회 실패");
	});
}

// 트리그리드 형태로 변환
async function buildDeptTree(flatList) {
    const deptMap = {};
    flatList.forEach(item => {
        // 부서 노드 준비
        if (!deptMap[item.DEPT_ID]) {
            deptMap[item.DEPT_ID] = {
                name: item.DEPT_NAME,
                deptId: item.DEPT_ID,
                type: 'department',
                parentId: item.PARENT_ID ?? null,
                children: []
            };
        }
        // 직원 노드는 부서 children에 추가
        if (item.EMP_ID) {
            deptMap[item.DEPT_ID].children.push({
                name: item.EMP_NAME,
                empId: item.EMP_ID,
                type: 'employee'
            });
        }
    });

    // 계층 트리화
    const treeRoot = [];
    Object.values(deptMap).forEach(dept => {
        if (!dept.parentId || !deptMap[dept.parentId]) {
            treeRoot.push(dept);   // 최상위 부서
        } else {
            deptMap[dept.parentId].children.push(dept);  // 하위부서로 연결
        }
    });
	
	const toastTreeData = convertTreeNodes(treeRoot);

    return toastTreeData;
}

// 트리 노드 변환
function convertTreeNodes(nodes) {
    return nodes.map(node => {
        const newNode = { ...node };
        if (Array.isArray(newNode.children) && newNode.children.length > 0) {
            newNode._children = convertTreeNodes(newNode.children);
        }
        delete newNode.children;
        newNode._attributes = { expanded: true }; // 펼침 초기화
        return newNode;
    });
}

let treeGrid = null;

// 조직도그리드 그리기
async function renderOrgGrid() {
	// 그리드 언어 설정
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
	// 조직도 열때마다 초기화
    if (treeGrid) {
		treeGrid.destroy();
		treeGrid = null;
	}
//	console.log("토스트트리데이터 : ",toastTreeData);
    treeGrid = new tui.Grid({
        el: document.getElementById('organizationChartGrid'),
        data: toastTreeData, // 트리화 데이터
        rowHeaders: ['checkbox'],
        bodyHeight: 'auto',
		filter: true,
        treeColumnOptions: {
            name: 'name',
            useCascadingCheckbox: true
        },
        columns: [
            { header: '이름'
			, name: 'name'
			, treeColumn: true
			, align: 'left'
			, filter: 'text'	
			, formatter: function({row}) {
				if(row.empId != null){
					return `${row.name}(${row.empId})`
				} else {
					return `${row.name}(${row.deptId})`
				}
			}
			 }
        ]
    });
		
}

document.getElementById('select-sharer-btn').addEventListener('click', function(){
    openOrgModal();
    getOrganizationChart();
});


document.getElementById('select-org-btn').addEventListener('click', function(){
	// 체크된 직원 정보 가져오기
    checkedUpEmpList = getCheckedEmpId();
	// 체크된직원 input 처리
	setSharers(checkedUpEmpList);
	
    closeOrgModal();
});

// 조직도에서 선택된 직원 input값 처리, 초기화
function setSharers(checkedUpEmpList) {
	// 명단 보여줄 input
	const sharerInput = document.getElementById('schedule-sharer');
	sharerInput.disabled = true;
	// 텍스트 명단 초기화 & 세팅
	if(checkedUpEmpList.length === 0) {
		sharerInput.value = '';
	} else {
		const checkedNames = checkedUpEmpList.map(item => `${item.empName} (${item.empId})`);
		sharerInput.value = checkedNames.join(', ');
	}
	
}



function getCheckedEmpId() {
    let checked = [];
    const rows = treeGrid.getData();
    rows.forEach((rowData) => {
        // 직원만
        if(rowData._attributes && rowData._attributes.checked && rowData.empId != null) {
            checked.push({
                empId: rowData.empId,
                empName: rowData.name
            });
        }
    });
    return checked;
}

async function checkSharers(scheduleType, scheduleId) {
	// 스케줄타입이 공유가 아니면 리턴
	if(scheduleType != 'share') return;
	try {
		const response = await fetch(apiUrl(`/api/schedules/sharerList/${scheduleId}`), {method: 'GET'});
		if(!response.ok) throw new Error(await response.text());
		
		const data = await response.json();
//		console.log(data);
		return data;
	} catch(error) {
		console.error('에러', error)
		alert("공유자 목록 조회 실패");
	}
}

// ----------------------------------------------------------
// 반복일정 관련 js 만들기
function onRecurrenceTypeChange() {
    const type = document.getElementById('recurrenceType').value;
    document.getElementById('weekday-options').style.display = (type === 'weekly') ? 'block' : 'none';
    document.getElementById('monthly-options').style.display = (type === 'monthly') ? 'block' : 'none';
    document.getElementById('yearly-options').style.display = (type === 'yearly') ? 'block' : 'none';
}

function onRecurrenceEndTypeChange() {
    const type = document.getElementById('recurrenceEndType').value;
    document.getElementById('recurrence-until').style.display = (type === 'until') ? 'block' : 'none';
}

window.onload = function() {
    onRecurrenceTypeChange();
    onRecurrenceEndTypeChange();
};















