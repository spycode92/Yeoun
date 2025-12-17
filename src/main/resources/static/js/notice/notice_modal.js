/**
	공지사항모달 JavaScript 
**/
let selectedNoticeId = null;
// 공지사항 조회 모달설정
const showNoticeModal = document.getElementById('show-notice');
const showNoticeForm = document.getElementById("notice-form-read");
const fixedCheck = document.getElementById('fixed-check') // 체크박스
const deleteNoticeBtn = document.getElementById('notice-delete');
const modifyNoticeBtn = document.getElementById('notice-modify');


//돔로드시작
document.addEventListener('DOMContentLoaded', function() {
	
	// 공지사항 조회모달 열기 이벤트
	showNoticeModal.addEventListener('show.bs.modal', async function(event){
		await getNoticeFileData(selectedNoticeId);
		await getNoticeData(selectedNoticeId);		
	});
	
	//공지사항 조회 - 수정버튼
	showNoticeForm.addEventListener('submit', function(event) {
		event.preventDefault(); //기본제출 막기
		
		fetch('/notice/' + selectedNoticeId, {
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
		
		fetch('/notice/' + selectedNoticeId, {
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
		document.getElementById("fileFieldWrite").innerHTML = "";// 파일첨부영역초기화
		createNoticeForm.reset();
	});
	// 공지사항 등록 버튼 함수
	createNoticeForm.addEventListener('submit', function(event) {
		event.preventDefault(); //기본제출 막기
		
		fetch('/notice', {
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
function NoticeDetailFormatDate(date) {
	let inputDate = date instanceof Date ? date : new Date(date);

    if (isNaN(inputDate.getTime())) {
		console.log("실패");
        // 파싱 실패 시 fallback (원본 값 그대로 쓰거나, 빈 문자열 등)
        return '';
    }
	
    const yyyy = inputDate.getFullYear();
    const MM = String(inputDate.getMonth() + 1).padStart(2, '0');
    const dd = String(inputDate.getDate()).padStart(2, '0');
    const HH = String(inputDate.getHours()).padStart(2, '0');
    const mm = String(inputDate.getMinutes()).padStart(2, '0');
    return `${yyyy}-${MM}-${dd} ${HH}:${mm}`;
}



// 조회할 공지 데이터 불러오기
async function getNoticeData(noticeId) {
	await fetch('/api/notices/' + noticeId)
	.then(response => { // response가 200이 아닐때
		if (!response.ok) throw new Error('공지사항을 불러올 수 없습니다.');
    	
		return response.json();
	})
	.then(data => {
//		console.log(data,"데이어어어어");
		inputReadData(data);
	})
	.catch(error => console.error('Error:', error));
}

// 공지조회모달 데이터 입력함수
async function inputReadData(data){
	// 날짜 문자열을 Date 객체로 변환
//	console.log(data.createdDate);
	const createdDate = new Date(data.createdDate);
//	console.log("craetedDate : ", createdDate);
	const updatedDate = new Date(data.updatedDate);
//	console.log("updatedDate : ", updatedDate);
	const createdUser = data.createdUser;
	const createdUserName = data.empName;
	const deptName = data.deptName;
	document.getElementById("notice-files-read").value = "";// 파일첨부영역초기화
	document.getElementById("fileFieldRead").innerHTML = "";// 파일첨부영역초기화
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
	document.getElementById('notice-createdDate-read').textContent = NoticeDetailFormatDate(createdDate);
//	console.log("formatDate(createdDate) : ", formatDate(createdDate));
	document.getElementById('notice-updatedDate-read').textContent = NoticeDetailFormatDate(updatedDate);
//	console.log("formatDate(updatedDate) : ", formatDate(updatedDate));
	document.getElementById('notice-content-read').textContent = data.noticeContent;
	document.getElementById('notice-modify').add = data.noticeContent;
	
	await initReadModal(data.createdUser);
	
}

// 조회할 공지 파일 데이터 가져오기
async function getNoticeFileData(noticeId) {
	await fetch('/api/notices/file/' + noticeId)
	.then(response => { // response가 200이 아닐때
		if (!response.ok) throw new Error('공지사항을 불러올 수 없습니다.');
    	
		return response.json();
	})
	.then(data => {
//		console.log(data,"데이터어어어어");
		inputReadFileData(data);
	})
	.catch(error => console.error('Error:', error));
}

// 공지 파일 데이터 입력함수
function inputReadFileData(fileData) {
	//등록된 파일 데이터 입력
//	console.log(fileData)
	// 등록된파일위치 초기화
	document.getElementById("notice-attached-file").innerHTML = "";
	
//	let files = Array.from(fileData);
	// 이미지 파일 갯수만큼 반복문 수행
	fileData.forEach(function(file, index) {
		// 미리보기 3-1) 미리보기 HTML 요소 생성
		const fileList = document.createElement('div');
		const iconEl = document.createElement('i');
		const nameSpan = document.createElement('span');
		const downloadEl = document.createElement('a');
		const deleteEl = document.createElement('a');
		const downloadImg = document.createElement('img');
		const deleteImg   = document.createElement('img');
		
		fileList.classList.add('attach-file');
		iconEl.classList.add('fa-regular', 'fa-file', 'file-icon');
		nameSpan.textContent = file.originFileName + "  ";
		
		fileList.appendChild(iconEl);
		fileList.appendChild(nameSpan);

		downloadEl.href = `/files/download/${file.fileId}`;
		downloadEl.classList.add('file-download-link');
		downloadEl.title = '다운로드';
		
		downloadImg.src = '/img/download-icon.png';
		downloadImg.alt = '다운로드';
		downloadImg.classList.add('file-download-icon', 'img-btn');
		
		downloadEl.appendChild(downloadImg);
		fileList.appendChild(downloadEl);
		
		// 4) 수정 권한이 있을때만 삭제 아이콘 설정
		if(hasRole('ROLE_NOTICE_WRITER')) {
			deleteEl.href = '#';                                
			deleteEl.classList.add('file-delete-link');
			deleteEl.title = '삭제';
			deleteEl.dataset.fileId = file.fileId;             
			
			deleteImg.src = '/img/delete-icon.png';
			deleteImg.alt = '삭제';
			deleteImg.classList.add('file-delete-icon', 'img-btn');
	
			deleteEl.appendChild(deleteImg);
			fileList.appendChild(deleteEl);
		}
							
		// 미리보기 3-2) 미리보기 영역에 요소 추가
//		console.log(fileList);
		document.getElementById("notice-attached-file").append(fileList);
		
		deleteEl.addEventListener('click', function(event) {
			event.preventDefault();
			
			deleteFile(deleteEl);

		});
	});
	
	initUploadArea();
}
// 파일 삭제 함수
async function deleteFile(elem) {
	if(!confirm("파일을 삭제하시겠습니까?")) {
		return;
	}

	// 하이퍼링크 요소 객체(elem)에 포함된 데이터셋 id 값(data-id) 가져오기
	const fileId = elem.dataset.fileId;
	
	await $.ajax({
		// RESTful API 형식으로 주소를 지정할 경우 삭제는 DELETE 방식의 메서드 활용하며, URL 뒤에 삭제할 번호를 경로 변수 형태로 포함
		url: "/files/" + fileId,
		type: "delete",
		// data 속성에 변수값 전달 시 속성명과 변수명이 동일하면 하나만 기술해도 됨
		dataType: "json",
		// AJAX 요청 전 먼저 CSRF 값을 서버측으로 전송
		beforeSend: function(xhr) {
			xhr.setRequestHeader(csrfHeader, csrfToken);
		},
		success: async function(response) {
			if(response.result) {
				const parent = elem.parentElement; // 자바스크립트
//				console.log(parent);
				parent.remove();
				// 삭제된 파일 지우고 파일업로드창 생성
				initUploadArea();
			}
		},
		error: function() {
			alert("삭제 요청 실패!");
		}
	});
}

// 권한 배열에서 특정권한이 존재하는지 확인
function hasRole(role) {
	if(!userRoles || !Array.isArray(userRoles)) {
		return false;
	}
	
	return userRoles.includes(role);
}

// 공지조회 모달 열때 공지쓰기 권한이 있는지 판별
async function initReadModal(createdUser) {
	
	if(hasRole('ROLE_NOTICE_WRITER')) { // 권한이있을때
		Array.from(showNoticeForm.elements).forEach(el => {
			if(el.tagName === 'INPUT' || el.tagName === 'TEXTAREA') {
				el.readOnly = false;
			}
			if(el.tagName === 'SELECT' || el.type === 'checkbox' || el.type === 'file'){
				el.disabled = false;
			}
		});
		deleteNoticeBtn.disabled = false;
		modifyNoticeBtn.disabled = false;
		deleteNoticeBtn.style.display = 'block';
		modifyNoticeBtn.style.disabled = 'block';
	} else { //권한이 없을때 수정,삭제 불가능
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
		deleteNoticeBtn.style.display = 'none';
		modifyNoticeBtn.style.display = 'none';
		
	}
}

// 공지등록 첨부파일 엘리먼트
const writeFileInput = document.getElementById('notice-files-write');
const readFileInput = document.getElementById('notice-files-read');
const MAX_FILE_COUNT = 5;

// 공지등록 첨부파일 변화시 동작이벤트
writeFileInput.addEventListener('change', function(event) {
	let files = Array.from(event.target.files);

	if(files.length > MAX_FILE_COUNT) { // 파일 갯수가 5개보다 많을 경우
		alert("최대 첨부 가능한 갯수 : " + MAX_FILE_COUNT);
		files = files.slice(0, MAX_FILE_COUNT);
	}
	// 파일등록 영역 초기화
	document.getElementById("fileFieldWrite").innerHTML = "";
	
	// 기존 파일 선택 영역을 교체할 새로운 파일 목록 객체 변경에 사용되는 DataTransfer 객체 생성
	let newFiles = new DataTransfer();
	
	// 파일 갯수만큼 반복문 수행
	files.forEach(function(file, index) {
		// 현재 반복중인 파일 1개를 새로운 DataTransfer 객체에 추가
		newFiles.items.add(file);
		// 미리보기 3-1) 미리보기 HTML 요소 생성
		const fileList = document.createElement('div');
		const iconEl = document.createElement('i');
		const nameSpan = document.createElement('span');
		
		fileList.classList.add('file');
		iconEl.classList.add('fa-regular', 'fa-file', 'file-icon');
		nameSpan.textContent = file.name;

		fileList.appendChild(iconEl);
		fileList.appendChild(nameSpan);
							
		// 미리보기 3-2) 미리보기 영역에 요소 추가
//		console.log(fileList);
		document.getElementById("fileFieldWrite").append(fileList);
	});
	// --------------------------------------------------
	// 파일 선택 요소 객체의 files 속성에 접근하여 새로운 파일 목록 객체로 업데이트
	writeFileInput.files = newFiles.files;
});

// 공지사항 조회 파일관리, 첨부
let existingFileCount; // 현재까지 업로드된 파일 갯수

//파일업로드창 초기화
async function initUploadArea() {
	//현재까지 업로드된 첨부파일 갯수 가져오기(클래스 선택자 itemImg 와 oldFile 이 함께 붙은 요소의 갯수)
	existingFileCount = $(".attach-file").length;
	
//	console.log("initUploadArea : ", existingFileCount);

	// 현재까지 업로드 된 첨부파일 갯수가 최대 첨부 가능 갯수와 같으면 첨부파일 요소 숨김처리, 아니면 표시
	if(existingFileCount == MAX_FILE_COUNT) {
		$("#uploadArea").hide(); // 숨김
	} else {
		$("#uploadArea").show(); // 숨김
	}
}


// 공지조회모달 첨부파일 변화시 동작이벤트
readFileInput.addEventListener('change', function(event) {
	let files = Array.from(event.target.files);
//	console.log(files, "Asdfasdfszdf");
	
	const totalFileCount = files.length + existingFileCount;
//	console.log("!!!!!!!!!!!!!", existingFileCount);
//	console.log(totalFileCount);
	if(totalFileCount > MAX_FILE_COUNT) { // 파일 갯수가 5개보다 많을 경우
		let availableFileCount = MAX_FILE_COUNT - existingFileCount;
		alert("최대 첨부 가능한 갯수 : " + availableFileCount);
		
		files = files.slice(0, availableFileCount);
	}
	// 파일등록 영역 초기화
	document.getElementById("fileFieldRead").innerHTML = "";
	
	// 기존 파일 선택 영역을 교체할 새로운 파일 목록 객체 변경에 사용되는 DataTransfer 객체 생성
	let newFiles = new DataTransfer();
	
	// 파일 갯수만큼 반복문 수행
	files.forEach(function(file, index) {
		// 현재 반복중인 파일 1개를 새로운 DataTransfer 객체에 추가
		newFiles.items.add(file);
		// 미리보기 3-1) 미리보기 HTML 요소 생성
		const fileList = document.createElement('div');
		const iconEl = document.createElement('i');
		const nameSpan = document.createElement('span');
		
		fileList.classList.add('added_file');
		iconEl.classList.add('fa-regular', 'fa-file', 'file-icon');
		nameSpan.textContent = file.name;

		fileList.appendChild(iconEl);
		fileList.appendChild(nameSpan);
							
		// 미리보기 3-2) 미리보기 영역에 요소 추가
//		console.log(fileList);
		document.getElementById("fileFieldRead").append(fileList);
	});
	// --------------------------------------------------
	// 파일 선택 요소 객체의 files 속성에 접근하여 새로운 파일 목록 객체로 업데이트
	readFileInput.files = newFiles.files;
});










