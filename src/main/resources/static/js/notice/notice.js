/**
	공지사항 JavaScript 
**/
//const csrfToken = document.querySelector('meta[name="_csrf_token"]')?.content;
//const csrfHeaderName = document.querySelector('meta[name="_csrf_headerName"]')?.content;
console.log("#@@@@@@@@@@@@@@@@@@csrfToken : ", csrfToken);
console.log("@@@@@@@@@@@@@@@@@@@csrfHeader : ", csrfHeader);
const currentUserId = document.getElementById('currentUserId')?.value;
const currentUserName = document.getElementById('currentUserName')?.value;

const orderKey = getUrlParameter('orderKey') || 'updatedDate';
const orderMethod = getUrlParameter('orderMethod') || 'asc';
const activeTh = document.querySelector(`th[data-key="${orderKey}"]`);

//돔로드시작
document.addEventListener('DOMContentLoaded', function() {
	updateSortUI(activeTh, orderKey, orderMethod);
	if (!orderKey) return;
		
}); // DOM로드 끝

// 보기버튼 눌렀을때 모달열기함수
document.querySelectorAll('button[data-notice-id]').forEach(btn => {
	btn.addEventListener('click', (event) => {
		const noticeId = btn.dataset.noticeId;
		selectedNoticeId = noticeId;
		const modalEl = document.getElementById('show-notice');
		new bootstrap.Modal(modalEl).show();
	})
});

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
//	updateSortUI(thElement, key, newOrderMethod);
	
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
//	document.querySelectorAll('th .sort-icon').forEach(icon => {
//		icon.className = 'sort-icon fa-solid fa-sort';
//	});
	const selected = document.querySelector('th .selected');
	if(selected) {
		const th = selected.closest('th');
		if(th) th.setAttribute('aria-sort', 'none');
	}
	
	
	// 클릭된 헤더 상태 변경
	if(!activeTh) return;
	const icon = activeTh.querySelector('.sort-icon');
	if(!icon) return;
	
	if(activeKey == activeTh.getAttribute('data-key')) {
		// asc와 desc 에 따라 아이콘 갱신
		const isAsc = newOrderMethod === 'asc';
		icon.className = isAsc
			? 'sort-icon fa-solid fa-sort-up selected'
			: 'sort-icon fa-solid fa-sort-down selected';
//		activeTh.setAttribute('aria-sort', isAsc ? 'ascending' : 'descending');
	} else {
		// 정렬 대상이 아니면 중립
		icon.className = 'sort-icon fa-solid fa-sort';
		activeTh.setAttribute('aria-sort', 'none');
	}
}

















