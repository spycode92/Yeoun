/**
	공지사항 JavaScript 
**/
//const csrfToken = document.querySelector('meta[name="_csrf_token"]')?.content;
//const csrfHeaderName = document.querySelector('meta[name="_csrf_headerName"]')?.content;

const currentUserId = document.getElementById('currentUserId')?.value;
const currentUserName = document.getElementById('currentUserName')?.value;

//돔로드시작
document.addEventListener('DOMContentLoaded', function() {
	let selectedNoticeId = null;
	// 공지사항 조회 모달설정
	const showNoticeModal = document.getElementById('show-notice');
	const showNoticeForm = document.getElementById("notice-form-read");
	const fixedCheck = document.getElementById('fixed-check') // 체크박스
	const deleteNoticeBtn = document.getElementById('notice-delete');
	const modifyNoticeBtn = document.getElementById('notice-modify');
	
	const orderKey = getUrlParameter('orderKey') || 'updatedDate';
	const orderMethod = getUrlParameter('orderMethod') || 'asc';
	if (!orderKey) return;
	
	const activeTh = document.querySelector(`th[data-key="${orderKey}"]`);
	
	updateSortUI(activeTh, orderKey, orderMethod);
	
	// 공지사항 조회모달 열기 이벤트
	showNoticeModal.addEventListener('show.bs.modal', function(event){
		const button = event.relatedTarget;
		const noticeId = button.getAttribute('data-notice-id');
		
		selectedNoticeId = noticeId;
		
		fetch(apiUrl(`api/notices/${noticeId}`))
			.then(response => { // response가 200이 아닐때
				if (!response.ok) throw new Error('공지사항을 불러올 수 없습니다.');
		    	
				return response.json();
			})
			.then(data => {
				// 날짜 문자열을 Date 객체로 변환
				const createdDate = new Date(data.createdDate);
				const updatedDate = new Date(data.updatedDate);
				const createdUser = data.createdUser;
				const createdUserName = data.empName;
				const deptName = data.deptName;
				
				document.getElementById('notice-id-read').value = data.noticeId;
				document.getElementById('notice-createdUser-read').value = createdUser;
				document.getElementById('notice-title-read').value = data.noticeTitle;
				if(data.noticeYN === 'Y') {
					fixedCheck.checked = true;
					fixedCheck.value = "Y";
				} else {
					fixedCheck.checked = false;
					fixedCheck.value = "N";
				}
				document.getElementById('notice-writer-read').textContent = `(${deptName})${createdUserName}`
				document.getElementById('notice-createdDate-read').textContent = formatDate(createdDate);
				document.getElementById('notice-updatedDate-read').textContent = formatDate(updatedDate);
				document.getElementById('notice-content-read').textContent = data.noticeContent;
				document.getElementById('notice-modify').add = data.noticeContent;
				
				initReadModal(data.createdUser);
			})
			.catch(error => console.error('Error:', error));
	});
	
	// 공지조회 모달 열때 글쓴이와 접속자 동일인물 판별
	function initReadModal(createdUser) {
		if(currentUserId == createdUser) { // 로그인직원과 글쓴이가 동일인물
			Array.from(showNoticeForm.elements).forEach(el => {
				if(el.tagName === 'INPUT' || el.tagName === 'TEXTAREA') {
					el.readOnly = false;
				}
				if(el.tagName === 'SELECT' || el.tagName === 'checkbox' || el.type === 'file'){
					el.disabled = false;
				}
			});
			deleteNoticeBtn.disabled = false;
			modifyNoticeBtn.disabled = false;
		} else if(currentUserId != createdUser) { //일치하지 않을떄 수정,삭제 불가능
			Array.from(showNoticeForm.elements).forEach(el => {	
				if(el.tagName === 'INPUT' || el.tagName === 'TEXTAREA') {
					el.readOnly = true;
				}
				if(el.tagName === 'SELECT' || el.type === 'checkbox' || el.type === 'file'){
					el.disabled = true;
				}
			});
			deleteNoticeBtn.disabled = true;
			modifyNoticeBtn.disabled = true;
		}
	}
	
	
	//공지사항 조회 - 수정버튼
	showNoticeForm.addEventListener('submit', function(event) {
		event.preventDefault(); //기본제출 막기
		
		fetch(apiUrl(`notices/${selectedNoticeId}`), {
			method: 'PATCH'
			, headers: {
				[csrfHeader]: csrfToken
			}
			, body: new FormData(showNoticeForm)
		})
		.then(response => {
			if (!response.ok) throw new Error('수정에 실패했습니다.');
			return response.json();  // REST컨트롤러 아니므로 JSON 파싱
		})
		.then(response => { // response가 ok일때
			alert(response.msg);
			location.reload();
		}).catch(error => {
			console.error('에러', error)
			alert("제목, 내용은 필수입력 사항입니다.");
		});
	});
	
	//공지사항 조회 - 삭제버튼
	deleteNoticeBtn.addEventListener('click', function(event) {
		event.preventDefault(); //기본제출 막기
		
		alert("정말 삭제하시겠습니까?");
		
		fetch(apiUrl(`notices/${selectedNoticeId}`), {
			method: 'DELETE'
			, headers: {
				[csrfHeader]: csrfToken
			}			
		})
		.then(response => {
			if (!response.ok) throw new Error('삭제에 실패했습니다.');
			return response.json();  // REST컨트롤러 아니므로 JSON 파싱
		})
		.then(response => { // response가 ok일때
			alert(response.msg);
			location.reload();
		}).catch(error => {
			console.error('에러', error)
			alert("제목, 내용은 필수입력 사항입니다.");
		});
	});
	
	
	
	// 공지사항 등록 모달설정
	const createNoticeModal = document.getElementById('create-notice');
	const createNoticeForm = document.getElementById("notice-write-form");
	// 공지사항 등록 모달 열릴때 폼 초기화
	createNoticeModal.addEventListener('show.bs.modal', function(event){
		createNoticeForm.reset();
	});
	// 공지사항 등록 버튼 함수
	createNoticeForm.addEventListener('submit', function(event) {
		event.preventDefault(); //기본제출 막기
		
		fetch(apiUrl(`notices`), {
			method: 'POST'
			, headers: {
				[csrfHeader]: csrfToken
			}
			, body: new FormData(createNoticeForm)
		})
		.then(response => {
			if (!response.ok) throw new Error('등록에 실패했습니다.');
			return response.json();  //JSON 파싱
		})
		.then(response => { // response가 ok일때
			alert(response.msg);
			location.reload();
		}).catch(error => {
			console.error('에러', error)
			alert("제목, 내용은 필수입력 사항입니다.");
		});
	});
		
}); // DOM로드 끝

//공지사항 수정 체크박스 값 변경
function toggleValue(checkbox){
	if (checkbox.checked) {
		checkbox.value = "Y";
	} else {
		checkbox.value = "N"
	}
}

// 포맷 함수 (예: yyyy-MM-dd HH:mm)
function formatDate(date) {
    const yyyy = date.getFullYear();
    const MM = String(date.getMonth() + 1).padStart(2, '0');
    const dd = String(date.getDate()).padStart(2, '0');
    const HH = String(date.getHours()).padStart(2, '0');
    const mm = String(date.getMinutes()).padStart(2, '0');
    return `${yyyy}-${MM}-${dd} ${HH}:${mm}`;
}

//파라미터값받아오기
function getUrlParameter(name) {
    const urlParams = new URLSearchParams(window.location.search);
    return urlParams.get(name);
}

// 정렬버튼 클릭 함수
function allineTable(thElement) {
	// 지금 누른 orderKey 받아오기	
    const key = thElement.getAttribute('data-key');
	
	//현재 orderKey, orderMethod 
    let currentOrderKey = getUrlParameter('orderKey');
	let currentOrderMethod = getUrlParameter('orderMethod') || 'asc';
	// 새 orderMethod
    let newOrderMethod = 'asc';
    // 지금 누른 오더키가 원래의 오더키 일때 
	if (currentOrderKey === key) {
		// 새 오더메서드 방법 변경
        newOrderMethod = (currentOrderMethod === 'asc') ? 'desc' : 'asc';
    }
	
	//아이콘 변경
	updateSortUI(thElement, key, newOrderMethod);
	
	//현재 검색 타입, 검색어 존재하면 유지 없으면 널스트링
    const searchKeyword = getUrlParameter('searchKeyword') || '';
    const page = getUrlParameter('page') || '';
	
	//요청할 url 조합
	let url = `${window.location.pathname}?orderKey=${key}&orderMethod=${newOrderMethod}`;
    	if(searchKeyword) url += `&searchKeyword=${encodeURIComponent(searchKeyword)}`;
    	if(page) url += `&page=${encodeURIComponent(page)}`;
	
	// 이동요청
	window.location.href = url;
}

// 정렬버튼 아이콘 변경
function updateSortUI(activeTh, activeKey, newOrderMethod) {
	// 전체 초기화
	document.querySelectorAll('th .sort-icon').forEach(icon => {
		icon.className = 'sort-icon fa-solid fa-sort';
		const th = icon.closest('th');
		if(th) th.setAttribute('aria-sort', 'none');
	});
	
	// 클릭된 헤더 상태 변경
	if(!activeTh) return;
	const icon = activeTh.querySelector('.sort-icon');
	if(!icon) return;
	
	if(activeKey == activeTh.getAttribute('data-key')) {
		// asc와 desc 에 따라 아이콘 갱신
		const isAsc = newOrderMethod === 'asc';
		icon.className = isAsc
			? 'sort-icon fa-solid fa-sort-up'
			: 'sort-icon fa-solid fa-sort-down';
//		activeTh.setAttribute('aria-sort', isAsc ? 'ascending' : 'descending');
	} else {
		// 정렬 대상이 아니면 중립
		icon.className = 'sort-icon fa-solid fa-sort';
		activeTh.setAttribute('aria-sort', 'none');
	}
}

















