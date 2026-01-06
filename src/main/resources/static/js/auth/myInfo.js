// myInfo.js
// 메인 상단바 프로필 

/* 
 * 로그아웃 여부
 */
document.querySelectorAll('.logout-link').forEach(link => {
	link.addEventListener('click', function (e) {
		e.preventDefault();
		if (confirm('로그아웃 하시겠습니까?')) {
			document.getElementById('hiddenLogoutForm').submit();
		}
	});
});

/**
 * 정보 수정 버튼
 */
document.getElementById('editBtn')?.addEventListener('click', function () {
	if (window.detailMode === 'self') {
		window.location.href = '/my/info/edit';
	}
});
























