// 공공데이터 포털 uri, APi
const uri = "https://apis.data.go.kr/B090041/openapi/service/SpcdeInfoService/getHoliDeInfo?Servicekey";
const myApiKey = "3bb524dc5656794ff51462c21245e81ffd44e902f5c3220a4d89b540465280e9";

// 로그인한 사원의 사번, 이름
const currentUserId = document.getElementById('currentUserId')?.value;
const currentUserName = document.getElementById('currentUserName')?.value;

// csrfTocken
//const csrfToken = document.querySelector('meta[name="_csrf_token"]')?.content;
//const csrfHeaderName = document.querySelector('meta[name="_csrf_headerName"]')?.content;

// 연차 툴팁
const tooltip = document.getElementById('leave_tooltip');

// 캘린더의 날자와 연동할 데이트피커 객체
let dateController = null;

let calendar = null; // calendar 객체 선언

let holidayData = null; // 휴일데이터 저장
let monthlyScheduleData = null; //달별 스케줄 데이터
let monthlyLeaveData = null;

let calendarYear = null; // 현재 날짜 년 저장
let calendarMonth = null; // 현재 날짜 월 저장
let scheduleData = null; // 일정 데이터 저장
let currentDate = null; // 현재날짜 저장

let calendarType = 'month'; //현재 달력 타입 지정

// 캘린더 월 이동 버튼
const prevMonthBtn = document.getElementById("prevMonth");
const nextMonthBtn = document.getElementById("nextMonth");

// 캘린더 엘리먼트 지정
const calendarEl = document.getElementById('calendar');

// ----------------------------------
// 결재관련 전역변수, 함수 
// 현재 열린 문서의 approvalId
let approvalId;
// 현재 열린 문서의 결재권자(approval) 
let currentApprover;
// 결제확인 버튼
const approvalCheckBtn = document.getElementById('approvalCheckBtn');
// 반려 버튼
const approvalCompanionBtn = document.getElementById('approvalCompanionBtn');

// 결재확인 버튼 눌렀을때 동작할 함수
approvalCheckBtn.addEventListener('click', () => {
	patchApproval("accept");
});

// 반려버튼 눌렀을때 동작할 함수
approvalCompanionBtn.addEventListener('click', () => {
	patchApproval("deny")		
});

// 결재 패치 보내기 함수
function patchApproval(btn) {
	// 현재 로그인한 사용자와 결재권자 비교
	if(checkApprover()) return;
	let msg = "";
	btn == 'accept' ? msg = "승인하시겠습니까?" : msg = "반려하시겠습니까?"
	 
	
	// 결재권한자와 사용자가 동일인물일 때
	if(confirm(msg)) {
		//결재 확인 동작함수
		fetch(`/api/approvals/${approvalId}?btn=${btn}` , {
			method: 'PATCH'
			, headers: {
				[csrfHeader]: csrfToken
			}
		})
		.then(response => {
			if (!response.ok) return response.json().then(err => { throw new Error(err.result); });
			return response.json();
		})
		.then(data => {
			alert(data.result);
			// 결제승인완료시 새로고침
			location.reload();
			
		}).catch(error => {
			console.error('에러', error)
			alert("결재 승인 실패!!");
		});
	 }
}

// 현재 로그인한 사용자와 결재권자 비교
function checkApprover() {
	if(currentApprover != currentUserId) {
		alert("승인 또는 반려권한이 없습니다."); 
		return true;
	}
}

// null-safe 날짜 변환 함수
function toDateStr(value) {
  if (!value) return '';              // null, undefined, '' 전부 빈 문자열 처리
  return String(value).split('T')[0]; // 혹시 문자열 아니어도 방어

}

//결제상세보기 => 결제권자 정보 불러오기함수
async function getApproverList(approvalId) {
	try {
		const response = await fetch(`/api/approvals/approvers/${approvalId}`, {method: 'GET'});
		
		if(!response.ok) {
			const errorData = await response.json();
			throw new Error(errorData.result);
		}
		const data = await response.json();
		return data;
	} catch(error) {
		alert("결재권자 목록을 불러올 수 없습니다!");
		return null;
	}
}

async function print(type, text) {
	// 결재권한자변경 div 버튼 생성
//	console.log(window.count, "@@@@@@@@@@@@");
	if(this.count < 3){
//	console.log(this.count, "this.count:!!!!!!!!!!!!")
		this.count++;
		approverDiv.innerHTML +='<div class="btn btn-success approvers"'
	  						+'style="width:200px;height:200px; margin:5px; padding: 5px 0px 0px 0px;">'
	//  						+'<p onclick="approverDivclose(this,' + "'"+ type + "'"+ ','+ count +')" style="float:right;margin-right: 8px;">&times;</p>'
	//  						+'<p onclick="approvalNo('+ (this.count)+','+ "'"+ text + "'" +')" style="margin-top:50px;height: 129px;">'+(this.count) + '차 결재권한자 : (직급)' + text + ' 변경</p>'
	  						+'<p style="float:right;margin-right: 8px;">&times;</p>'
	  						+'<p style="margin-top:50px;height: 129px;">'+(this.count) + '차 결재권한자 : (직급)' + text + ' 변경</p>'
							+'</div>';
	}

} 
// -----------------------------------------------------------------
//---------------------------------------------------------
// 캘린더위 버튼, 날짜 함수
//---------------------------------------------------------

// 캘린더 중앙의 날자 클릭했을때 생성될 데이트피커 생성함수, 체인지이벤트
function createDatePicker() {
	dateController = new tui.DatePicker('#tui-datepicker',
	    {
	    language: 'ko',
	    date: new Date(),
		showAlways: false,
		autoClose: true,
		openers: true,
		calendar: {
			showToday : true,
		},
		showToday: true,
		showJumpButtons: true,
		type: 'date'
	});
	
	// datepicker날짜 선택시 캐린더 날짜 변경
	dateController.on('change', function() {
		var selectedDate = dateController.getDate();
		var formattedDate = formatDateToYYYYMMDD(selectedDate);
		calendar.setDate(formattedDate);
		updateCurrentDate();
	});
}

// 데이트피커의 날을 캘린더 형식에 맞게 변경
function formatDateToYYYYMMDD(date) {
	var year = date.getFullYear();
	var month = String(date.getMonth() + 1).padStart(2, '0');
	var day = String(date.getDate()).padStart(2, '0');
	return year + '-' + month + '-' + day;
}


// 달력 월 변경 버튼 함수
prevMonthBtn.addEventListener('click', function() {
	calendar.prev();
	updateCurrentDate();
});

nextMonthBtn.addEventListener('click', function() {
	calendar.next();
	updateCurrentDate();
});

//일정등록버튼 함수(일정등록모달 열기)
document.getElementById('open-add-schedule-modal-btn').addEventListener('click', function() {
	openScheduleModal('add');
});

// 캘린더 상단 체크박스 클릭하여 캘린더 일정 필터
function checkFilter() {
	if(tooltip.style.display == 'block') {
		tooltip.style.display = 'none';
	}
	
	const companyFilter = document.getElementById('filter-company')
	const departmentFilter = document.getElementById('filter-department')
	const personalFilter = document.getElementById('filter-personal')
	const leaveFilter = document.getElementById('filter-leaves')
	
	companyFilter.checked ? calendar.setCalendarVisibility('company', true) : calendar.setCalendarVisibility('company', false);
	departmentFilter.checked ? calendar.setCalendarVisibility('share', true) : calendar.setCalendarVisibility('share', false);
	personalFilter.checked ? calendar.setCalendarVisibility('private', true) : calendar.setCalendarVisibility('private', false);
	leaveFilter.checked ? calendar.setCalendarVisibility('leave', true) : calendar.setCalendarVisibility('leave', false);
}

// 버튼 함수 지정(list, month, week, day 버튼 화면전환)
function changeCalendarType(type) {
	if(tooltip.style.display == 'block') {
		tooltip.style.display = 'none';
	}
	calendar.changeView(type, true);
	calendarType = type;
	
}
// 캘린더 타입 버튼 active표시함수
const buttons = document.querySelectorAll('button[data-view]');
buttons.forEach(btn => {
	btn.addEventListener('click', function() {
		// 이전에 선택된 버튼이 있다면 
		const prevActive = document.querySelector('button[data-view].active');
		// active클래스 제거
		if(prevActive) prevActive.classList.remove('active');
//		console.log("this : " , this)
		this.classList.add('active');	
	});
	
});

// MONTH, WEEK, DAY, LIST 버튼 이벤트리스너
document.getElementById('type-month').addEventListener('click', function() {
	// 만약 이미 달력이 없으면 생성
	if (!calendar) initCalendar();
    changeCalendarType('month');
	updateCurrentDate();
//	checkCalendarType();
});

document.getElementById('type-week').addEventListener('click', function() {
	// 만약 이미 달력이 없으면 생성
	if (!calendar) initCalendar();
    changeCalendarType('week');	
	updateCurrentDate();
//	checkCalendarType();
});

document.getElementById('type-day').addEventListener('click', function() {
	// 만약 이미 달력이 없으면 생성
	if (!calendar) initCalendar();
    changeCalendarType('day');
	updateCurrentDate();
//	checkCalendarType();
});

// list 버튼 클릭시 페이지 이동
document.getElementById('type-list').addEventListener('click', function() {
	location.href = "/main/schedule"
});

// ------캘린더 위 버튼관련 함수 끝



// ---------------------------------
// 캘린더 관련 함수 
// ---------------------------------

// 캘린더 생성함수
function initCalendar() {
	showCalendarLoading();
	calendarEl.innerHTML = ""; // 기존 내용 제거
	// 만약 이미 달력이 있으면 제거
	if (calendar) {
	    calendar.destroy();
	    calendar = null;
	}
	
	// 캘린더 객체 생성
	calendar = new tui.Calendar(calendarEl, {
	    defaultView: 'month',
		template: {
		    milestone(schedule) {
		        if (schedule.calendarId == 'leave' && schedule.raw && Array.isArray(schedule.raw.leaves)) {
					const namesArr = schedule.raw.leaves.map(leave => leave.emp_name);
					const firstName = namesArr[0] || '';
					const leaveCount = schedule.raw.leaves.length;
					
					if(schedule.raw.leaves.length > 1) {
			            return `<span style="font-size:10px; color:#1e7e34;">
			                👤${firstName} 외 ${leaveCount - 1}명
			            </span>`;
					} else if(schedule.raw.leaves.length = 1) {
						return `<span style="font-size:13px; color:#1e7e34;">
			                👤${firstName}
			            </span>`;
					}
		        }
				return schedule.title;
		    }
		},
	    useCreationPopup: false,
	    useDetailPopup: true,
		isReadOnly: false,
		useDetailPopup: false,
		month: {
	        visibleEventCount: 4  //월타입 달력에 보여줄 스케줄의 최대 개수
	    },
		week: {
		  // 시간 09:00~18:00만
		  hourStart: 9,
		  hourEnd: 18, 
		  taskView: ['milestone'],  
		  scheduleView: ['allday', 'time']
		},
		day: {
		  hourStart: 9,
		  hourEnd: 18,
		  taskView: ['milestone'],
		  scheduleView: ['allday', 'time']
		},
		calendars: [
		    {
		        id: 'holiday',
		        name: '공휴일',
		        color: '#fff',
		        backgroundColor: '#fdebe8',
		        borderColor: '#e74c3c'
		    },
			{
                id: 'private',
                name: '개인',
                color: '#000',
                backgroundColor: '#ffbb3b',
                dragBackgroundColor: 'rgba(255,187,59,0.6)',
                borderColor: '#111111',
                isDraggable: false,
                isResizable: false
            },
            {
                id: 'share',
                name: '부서',
                color: '#fff',
                backgroundColor: '#00a9ff',
                dragBackgroundColor: 'rgba(0,169,255,0.6)',
                borderColor: '#111111',
                isDraggable: false,
                isResizable: false
            },
            {
                id: 'company',
                name: '회사',
                color: '#fff',
                backgroundColor: '#ff5583',
                dragBackgroundColor: 'rgba(255,85,131,0.6)',
                borderColor: '#111111',
                isDraggable: false,
                isResizable: false
            },
			{
			    id: 'leave',
			    name: '연차',
			    color: '#333',
			    backgroundColor: '#b7f3c4',           // 초록 계열 예시
			    dragBackgroundColor: 'rgba(40,167,69,0.6)',
			    borderColor: '#1e7e34',
			    isDraggable: false,
			    isResizable: false
			},
			{
			    id: 'leave',
			    name: '연차',
			    color: '#333',
			    backgroundColor: '#b7f3c4',
			    borderColor: '#1e7e34',
			    isDraggable: false,
			    isResizable: false
			}
		]
	});
	// 이전에 선택된 날짜가 있으면 설정
	if(currentDate) calendar.setDate(currentDate);
	
	updateCurrentDate(); // 캘린더의 날자로 현재 날자 업데이트
	
	// 캘린더 일자 클릭 이벤트 설정
	calendar.on('selectDateTime', (event) => {
		if(tooltip.style.display == 'block') {
			tooltip.style.display = 'none';
			calendar.clearGridSelections();
			return;
		}
		
		//일정등록모달 열기
		openAddScheduleModal(event);
		// 달력 선택 색 초기화
		calendar.clearGridSelections();
	});

	// 캘린더에 등록된 일정 클릭시 이벤트
	calendar.on('clickEvent', async (eventInfo) => {
		const event = eventInfo.event;
		const nativeEvent = eventInfo.nativeEvent; // 마우스이벤트
		const tooltip = document.getElementById('leave_tooltip');
		
		// 연차 툴팁이 열려있다면 닫기
		if(tooltip.style.display == 'block') { 
			tooltip.style.display = 'none';
			return;
		}
		
		if (event.calendarId === 'holiday') { // 선택한 일정이 휴일일때
			alert("휴일입니다.");
			
		} else if (event.calendarId === 'leave') { // 선택한 일정이 연차일때
			// 일정이 없다면 [] 반환
			const leaves = event.raw?.leaves || [];
			
			//일정툴팁에 들어갈 html 작성
			let html = '';
			html += `<div class="leave-tooltip__title">${event.title}</div>`; 
//			if (leaves.length === 0) { // 연차일정이 없을때
//				html += `<div>연차 인원이 없습니다.</div>`;
//			} else {
			// 연차 일정이 있을때
			html += '<ul class="leave-tooltip__list">';
			const leaveTypeMap = {
			    ANNUAL: '연차',
			    SICK: '병가',
			    HALF: '반차',
			};
			leaves.forEach((l) => {
				const leaveTypeKor = leaveTypeMap[l.leaveType] || '기타';
		    	html += `<li>${l.emp_name}(${leaveTypeKor}) - ${l.startDate} ~ ${l.endDate} / ${l.usedDays}일</li>`;
			});
			html += '</ul>';
//			}
			//툴팁에 생성한 내용 추가
			tooltip.innerHTML = html;
			
			// 컨테이너 기준 절대좌표 계산
		    const rect = calendarEl.getBoundingClientRect();
			// 툴팁 위치 지정
			tooltip.style.position = 'absolute';
		    tooltip.style.left = (nativeEvent.clientX ) + 'px';
		    tooltip.style.top = (nativeEvent.clientY) + 'px';
		    calendarEl.appendChild(tooltip);
			// 툴팁 보이기
		    tooltip.style.display = 'block';
//			setTimeout(() => {tooltipCheck()}, 3000);	
		
		} else { // 선택한 일정이 연차도, 휴일도 아닐때 = 일정관리에 등록된 일정일 때
			
			const scheduleId = event.id;
			showCalendarLoading();
			// 등록된 일정정보 조회
			fetch(`/api/schedules/${scheduleId}`, {method: 'GET'})
			.then(response => {
				if (!response.ok) throw new Error(response.text());
				return response.json();  //JSON 파싱
			})
			.then(async data => { // response가 ok일때
				// 조회한 일정정보와 함께 일정조회모달 열기
				await openScheduleModal("edit", data);
				
			}).then(async () => {
				hideCalendarLoading()
			})
			.catch(error => {
				console.error('에러', error)
				alert("데이터 조회 실패");
			});
			
		}
	});
	
	hideCalendarLoading();

}

// 휴일정보 공공데이터 포털에서 받아와서 캘린더 반영
async function yearHoliday(year){
	await fetch(`${uri}=${myApiKey}&solYear=${year}&numOfRows=30&pageNo=1&_type=json`)
		.then(response => {
			if (!response.ok) throw new Error('Network response was not ok.');
			return response.json();
		})
		.then(data => {
			const beforeConvert = data.response.body.items.item;
			holidayData = convertHolidayDataToSchedules(beforeConvert);
		})
		.catch(console.error);
}

// 공휴일 데이터를 캘린더에 넣을수 있게 변환
function convertHolidayDataToSchedules(holidayData) {
    return holidayData
        .filter(item => item.isHoliday === 'Y')
        .map((item, idx) => ({
            id: String("holiday" + idx + 1 ),
            calendarId: 'holiday',
            title: item.dateName,
            category: 'milestone',
            isAllDay: true,
//            isHoliday: true,
			isReadOnly: true,
            start: formatDate(item.locdate), //'2025-01-28' 형식 
            end: formatDate(item.locdate), //'2025-01-28' 형식
			color: "#e74c3c",         // 텍스트 색 (빨강 예시)
		    backgroundColor: "#fdebe8", // 배경색 (연한 빨강)
			borderColor: "#e74c3c"    // 테두리색 (빨강)
        }));
}
// 받아온 공휴일 데이터의 날자를 캘린더에 넣을 날자에 맞게 변환
function formatDate(locdate) {
    const str = locdate.toString();
    return `${str.slice(0,4)}-${str.slice(4,6)}-${str.slice(6,8)}`;
}

//해당월의 달력일정 불러오기
async function loadMonthSchedule() {
	showCalendarLoading();
	// 현재 바뀐 날짜 정보에서 그해의 월초, 월말 날자 설정
	const loadDate = calendar.getDate();
	const startDate = new Date(
		loadDate.getFullYear(),
		loadDate.getMonth(),
		1,
		0, 0, 0, 0 
	); 
	const endDate = new Date(
		loadDate.getFullYear(),
		loadDate.getMonth() + 1,
		0,
		23, 59, 59, 999
	);
	// 그해의 월초, 월말 날자 params에저장
	const params = new URLSearchParams({
		startDate: formatLocalDateTime(startDate)
		, endDate: formatLocalDateTime(endDate)
	});
	
	if(tooltip.style.display == 'block') {
		tooltip.style.display = 'none';
		return;
	}
	
	// 해당 월초~월말 정보를 가지고 스케줄데이터 가져오기
	await getScheduleData(params); // 그달의 스케줄 가져오기
	await getLeaveData(params); // 그달의 연차 데이터 가져오기
	
	// 스케줄러 초기화
	await calendar.clear();
	// 저장된 그해의 휴일데이터 입력
	await calendar.createEvents(holidayData);
	// 저장된 그해의 휴일데이터 입력
	await calendar.createEvents(monthlyLeaveData);
	// 저장된 그달의 일정데이터 입력
	await calendar.createEvents(monthlyScheduleData);
	checkFilter();
	hideCalendarLoading();
	
}

// 현재 달력이 선택한 월의 일정 정보 불러오기
async function getScheduleData(params) {
	await fetch(`/api/schedules?${params.toString()}`, {method: 'GET'})
	.then(response => {
		if (!response.ok) throw new Error(response.text());
		return response.json();  //JSON 파싱
	})
	.then(data => { // response가 ok일때
		// 조회한 월단위 일정을 캘린더 데이터로 변환
		monthlyScheduleData = convertScheduleDataToSchedules(data);
	}).catch(error => {
		console.error('에러', error)
		alert("일정 데이터 조회 실패");
	});
}
// 현재 달력이 선택한 월의 일정정보 캘린더에 맞게 변환
function convertScheduleDataToSchedules(monthScheduleData) {
	return monthScheduleData.map(item => {
		const isAllday = item.alldayYN == "Y";
		return {
			id: String(item.scheduleId),
			calendarId: item.scheduleType,
			title: item.scheduleTitle,
			body: item.scheduleContent || "",
			start: item.scheduleStart.replace(" ", "T"),
			end: item.scheduleFinish.replace(" ", "T"),
			category: isAllday ? "allday" : "time",
			isAllday
//			raw: { ...item } // 기타등등 넣을정보
		};
	});
}

// 현재 달력이 선택한 월의 연차 정보 불러오기
async function getLeaveData(params) {
	await fetch(`/api/schedules/leaves?${params.toString()}`, {method: 'GET'})
	.then(response => {
		if (!response.ok) throw new Error(response.text());
		return response.json();  //JSON 파싱
	})
	.then(data => { // response가 ok일때
		// 연차데이터 날짜별로 그룹화
		const dateLeaveMap = groupLeavesByDate(data);
		// 스케줄에 넣을 데이터로 변환
		monthlyLeaveData = convertGroupedLeavesToSchedules(dateLeaveMap);
	}).catch(error => {
		console.error('에러', error)
		alert("연차 데이터 조회 실패");
	});
}

//연차정보를 날짜별로 그룹핑
function groupLeavesByDate(leaves) {
	
    const result = {};

    leaves.forEach(item => {
        const start = new Date(item.startDate);
        const end = new Date(item.endDate);

        let current = new Date(start);
        while (current <= end) {
            // YYYY-MM-DD 형태로 포맷
            const yyyyMMdd = current.toISOString().slice(0, 10);

            if (!result[yyyyMMdd]) {
                result[yyyyMMdd] = [];
            }
            result[yyyyMMdd].push(item);

            // 다음 날로 증가
            current.setDate(current.getDate() + 1);
        }
    });
    return result;
}

// 날짜별 그룹화 연차 정보를 캘린더 데이터에 맞게 변환
function convertGroupedLeavesToSchedules(dateLeaveMap) {
    return Object.entries(dateLeaveMap).map(([date, leaves]) => {
        // 당일 연차자 이름만 모아서 표시
        const names = leaves.map(leave => leave.emp_name);
        const title = `휴무: ${names.join(', ')} (${leaves.length}명)`;
        return {
            id: `leave-summary-${date}`,
            calendarId: 'leave',
            title: title,
            category: 'milestone',
            isAllDay: true,
            isReadOnly: true,
            start: date,
            end: date,
            color: '#333',
            backgroundColor: '#b7f3c4',
            borderColor: '#1e7e34',
            raw: {leaves}
        };
    });
}

// 빈 날자 클릭시 일정등록할 일정 등록 커스텀모달 등록
function openAddScheduleModal(data) {
	//모달열기
	openScheduleModal('add');
	// data로받아서 등록모달 날짜 지정하기
	var start = new Date(data.start);
	var end = new Date(start)
	
	end.setDate(start.getDate() + 1);
	isProgrammaticChange = true;
	picker.setStartDate(start ? new Date(start) : today);
	picker.setEndDate(end ? new Date(end): nextDay);
	isProgrammaticChange = false;
}
// -------------------------------------------------------------
// 캘린더 관련 함수 끝

//===============================================================
// DOM LOAD
document.addEventListener('DOMContentLoaded', async function () {
	
	createDatePicker(); // 데이트피커 생성
	initCalendar(); //달력 생성

	// datepicker날짜 선택시 캐린더 날짜 변경
	dateController.on('change', function() {
		var selectedDate = dateController.getDate();
		var formattedDate = formatDateToYYYYMMDD(selectedDate);
		calendar.setDate(formattedDate);
		updateCurrentDate();
	});
	
	// 캘린더 날짜 클릭하여 데이트피커열기
	const calendarDateEl = document.getElementById('calendar-date');
	
	calendarDateEl.addEventListener('click', function() {
		dateController.open();
	});
	
	await getLastNoticeList();
	await getApprovalList();
	
	document.querySelectorAll('input.calendar-filter').forEach((checkbox) => {
		checkbox.addEventListener('change', (event) => {
			checkFilter();
		});
	});
	
});// DOM로드 끝

// --------------------------------------------------------------
// 공지사항, 결제문서 목록 불러오기, 이벤트 함수

// 최근 공지사항 목록 데이터조회
async function getLastNoticeList() {
	await fetch(`/api/notices/last-notice`, {method: 'GET'})
	.then(response => {
		if (!response.ok) throw new Error(response.text());
		return response.json();  //JSON 파싱
	}).then(data => {
//		console.log(data, "공지데이터");
		initNoticeGrid(data);
	}).catch(error => {
		console.error('에러', error)
		alert("공지 데이터 조회 실패");
	});
}


// 공지그리드 생성변수
let noticeGrid = null;

// 공지그리드 그리기 함수
async function initNoticeGrid(data) {
	const Pagination = tui.Pagination;
	
	noticeGrid = new tui.Grid({
	    el: document.getElementById("noticeGrid"),
	    editable: true,
	    columns: [
	        {
	            header: '제목',
	            name: 'noticeTitle',
	            align: "left",
	            formatter: function({ row }) {
	                const title = row.noticeTitle || "";
	                let dateHtml = "";

	                if (row.updatedDate) {
	                    const date = new Date(row.updatedDate);
	                    if (!isNaN(date)) {
	                        const mm = String(date.getMonth() + 1).padStart(2, '0');
	                        const dd = String(date.getDate()).padStart(2, '0');
	                        const hh = String(date.getHours()).padStart(2, '0');
	                        const min = String(date.getMinutes()).padStart(2, '0');
	                        
	                        // 날짜 텍스트 생성
	                        const dateText = `(${mm}-${dd} ${hh}:${min})`;
	                        
	                        // 날짜 부분에만 적용할 스타일 (글자 작게, 줄바꿈 방지 등)
	                        dateHtml = `<span style="font-size: 11px; color: #888; margin-left: 10px; flex-shrink: 0;">${dateText}</span>`;
	                    }
	                }

	                // Flexbox를 사용하여 제목(왼쪽)과 날짜(오른쪽) 배치
	                return `
	                    <div style="display: flex; justify-content: space-between; align-items: center; width: 100%;">
	                        <span style="overflow: hidden; text-overflow: ellipsis; white-space: nowrap;">${title}</span>
	                        ${dateHtml}
	                    </div>
	                `;
	            },
	            className: 'combined-text'
	        }
	    ]
	});
	// 그리드 데이터 추가
//	console.log(data);
	noticeGrid.resetData(data);
//	console.log(noticeGrid.gridEl, "노티스그리드");
	const rows = noticeGrid.getData();
	
	rows.forEach(row => {
		if(row.noticeYN == 'Y'){
			noticeGrid.addCellClassName(row.rowKey, 'noticeTitle', 'notice-cell');
		}
	});	
	
	
	// 상세보기 버튼 이벤트
	await noticeGrid.on("click", (event) => {
//		console.log(event);
		const rowData = noticeGrid.getRow(event.rowKey);
		const noticeId = rowData.noticeId;
		
		selectedNoticeId = noticeId;
		const modalEl = document.getElementById('show-notice');
		new bootstrap.Modal(modalEl).show();
	});
}

// 최근 결제 문서 목록 데이터
async function getApprovalList() {
	await fetch(`/api/approvals`, {method: 'GET'})
	.then(response => {
		if (!response.ok) throw new Error(response.text());
		return response.json();  //JSON 파싱
	}).then(data => {
//		console.log(data, "결제문서데이터");
		initApprovalGrid(data);
	}).catch(error => {
		console.error('에러', error)
		alert("결제문서 데이터 조회 실패");
	});
}


let approvalGrid = null;
let selectedApprovalId = null;
let approverDiv = document.querySelector('#approver');
// 결제그리드 그리기 함수
async function initApprovalGrid(data) {
	const Pagination = tui.Pagination;
//	console.log(data, "공지데이터");
	approvalGrid = new tui.Grid({
		el: document.getElementById("approvalGrid"),
		editable: true,
		columns: [
			{
				header: '제목'
				, name: 'approvalTitle'
				, align: "left"
			},
			{
				header: '상태'
				, name: 'docStatus'
				, align: 'left'
				, width: 80
			}
		]
	});
	approvalGrid.resetData(data);
	
	const response = await fetch("/approval/empList");
	const selectData = await response.json();
	let itemData  = [];
	let obj ={};
	selectData.map((item,index)=>{
		obj["value"] = item[0]; //사번
		obj["label"] = (index+1) +" : "+item[1]+"("+item[0]+")"; //이름(사번)
		itemData.push(obj);
		obj = {};
	});
	
	//셀렉트박스 - 토스트유아이
	let selectBox = new tui.SelectBox('#select-box', {
	  data: itemData
	});
	//셀렉트박스 닫힐때
	selectBox.on('close',(ev)=>{
		let selectlabel = selectBox.getSelectedItem().label;
		let approverEmpId = selectBox.getSelectedItem().value;
		if(selectlabel != null && approverArr.length < 3){//셀렉트 라벨선택시 3번까지만셈
			print(ev.type, selectlabel);
			approverArr.push({
				empId: approverEmpId
				, approverOrder: this.count 
				, delegateStatus : false //여기서 전결상태도 불러오자
			});
		}
		
	});
	
	// 결재문서 상세보기 이벤트
	await approvalGrid.on("click", async (event) => {
//		console.log(event);
		const rowData = approvalGrid.getRow(event.rowKey);
		if(!rowData) {
			return;
		}
		selectedApprovalId = approvalId;
//		console.log("rowData : ", rowData);
		
//		alert("선택된 approvalId : " + approvalId);
//		const modalEl = document.getElementById('show-notice');
//		new bootstrap.Modal(modalEl).show();
		$('#approval-modal').modal('show');
		//formReset();
		document.getElementById('saveBtn').style.display = "none";
		// 문서 열릴때 approvalId에 현재 열린 문서id 저장
		approvalId = rowData.approvalId;
		// 문서 열릴때 현재 결재권자(approval) 저장
		currentApprover = rowData.approver;
		const approvalForm = document.getElementById('modal-doc');
		
		
		Array.from(approvalForm.elements).forEach(el => {
			if(el.tagName === 'INPUT' || el.tagName === 'TEXTAREA') {
				el.readOnly = true;
			}
			if(el.tagName === 'SELECT' || el.type === 'CHECKBOX') {
				el.disabled = true;
			}
		});
		
		// 모든 폼 display 초기화
		document.getElementById('leavePeriodForm').style.display = 'none';
		document.getElementById('leaveTypeForm').style.display = 'none';
		document.getElementById('expndTypeForm').style.display = 'none';
		document.getElementById('toDeptForm').style.display = 'none';
		
		// formType별 display 제어
		if (rowData.formType === '연차신청서') {
		  document.getElementById('leavePeriodForm').style.display = 'block';
		  document.getElementById('start-date').value = rowData.startDate;
		  document.getElementById('end-date').value = rowData.endDate;
		  document.getElementById('leaveTypeForm').style.display = 'block';
		  document.getElementById('leave-type').value = rowData.leaveType;
		} else if (rowData.formType === '지출결의서') {
		  document.getElementById('expndTypeForm').style.display = 'block';
		  document.getElementById('expnd-type').value = rowData.expndType;
		} else if (rowData.formType === '인사발령신청서') {
		  document.getElementById('toDeptForm').style.display = 'block';
		  document.getElementById('to-dept-id').value = rowData.toDeptId;
		}
//		console.log(rowData.empId);
		document.getElementById('Drafting').innerHTML = rowData.formType;
//		console.log(rowData.approvaTitle);
		document.getElementById('today-date').innerText = toDateStr(rowData.createdDate) ;//결재 작성날짜 = 결재시작일
		document.getElementById('approval-title').value = rowData.approvalTitle;
		//양식종류 form-menu
//		document.getElementById('approver-name').value  = rowData.empId;//결재자명
		
		//const createdDate = rowData.created_date;
		document.getElementById('create-date').value = toDateStr(rowData.createdDate);//결재시작일 =결재 작성날짜 
		document.getElementById('finish-date').value = toDateStr(rowData.finishDate);//결재완료날짜
		//휴가 연차신청서 
		document.getElementById('start-date').value = toDateStr(rowData.startDate); //휴가시작날짜
		document.getElementById('end-date').value = toDateStr(rowData.endDate); //휴가종료날짜
		//document.getElementById('leave-radio').value = rowData.leave_type;// 연차유형 라디오- 없앳음 -휴가종류로 들어감
		document.getElementById('leave-type').value = rowData.leaveType;//휴가종류
		
//		console.log("rowData.to_dept_id",rowData.to_deptId);
		document.getElementById('to-dept-id').value = rowData.toDeptId;//발령부서,디비잘못넣음
		document.getElementById('expnd-type').value = rowData.expndType;//지출종류EXPND_TYPE
		//document.getElementById('approver').value = rowData.approver;//결재권한자
		const approverList = await getApproverList(approvalId);
		selectBox.enable();
		let sortedList; 

		approverDiv.innerHTML = "";
		if(approverList.length > 0) {
			
			sortedList = approverList.sort((a, b) => {
				return Number(a.orderApprovers) - Number(b.orderApprovers);
			});
			
			window.count = 0;
								
			
			for (const approver of sortedList) {
				selectBox.select(approver.empId);
				print("default", selectBox.getSelectedItem().label);
			}
			
		}
		
		const approverBtns = document.querySelectorAll('.btn.approvers');
		
		approverBtns.forEach(btn => {
			btn.classList.add('disabled');
			btn.onclick = null; // 클릭 이벤트 해제
		});
		//document.getElementById('approver').innerText = rowData.approver;//전결자
		document.getElementById('reason-write').value = rowData.reason;//결재사유내용
		selectBox.disable();
			
	});
	// 결재 문서 모달 열기 끝
	// ----------------------------------------------------------------------------
}
// ------------------------------------------------------------------
// 공지사항, 결제문서 목록 불러와 그리드 그리기 끝
	
// 정리선
// ----------------------------------------------------------------------------------

// 달력, 데이트피커에 현재 날짜 업데이트 함수
async function updateCurrentDate() {
	showCalendarLoading();
	// 현재날짜 표시 할 위치 지정
	const currentDateEl = document.getElementById('calendar-date');
    // 현재날짜 저장
	currentDate = calendar.getDate();
	if (dateController) {
	    // change 이벤트 다시 안 터지게 silent 옵션 true
	    dateController.setDate(currentDate.d.d, true);
	}
	
    const year = currentDate.getFullYear();
    const month = currentDate.getMonth() + 1;

	// 스케줄러 위 중앙에 현재 날짜 년월 표시
	currentDateEl.textContent = `${year}년 ${month}월`;
//    const day = currentDate.getDate();
    
	//현재 해의 공휴일정보 받아오기
	if(!calendarYear) { //처음 캘린더 생성
		await yearHoliday(year);
	} else if(calendarYear != year) { // 선택된 년도가 바뀔때
		await yearHoliday(year);
	}
	
	// 기존의 년월과 현재 업데이트하는 년월이 다를경우
	// 그달의 스케줄 정보 불러오기
	if(calendarYear != year || calendarMonth != month) { 
		await loadMonthSchedule();
	}
	
	// 바뀐 년월 정보 저장
	calendarYear = year;
	calendarMonth = month;
	hideCalendarLoading(); 
}
	
// 스피너 보이기 끄기
function showCalendarLoading() {
	document.getElementById('calendar-loading-overlay').style.display = 'flex';
}
function hideCalendarLoading() {
	document.getElementById('calendar-loading-overlay').style.display = 'none';
}






