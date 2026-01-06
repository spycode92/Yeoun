//결재.js
//변수는 v- ,함수는 f-,그리드는 g- 주석 
// 현재 로그인한 사용자 EMP_ID
const LOGIN_USER_ID = document.getElementById('currentUserId').value;
const LOGIN_USER_NAME = document.getElementById('currentUserName').value;
// 현재 열린 문서의 approvalId
let approvalId;
// 현재 열린 문서의 결재권자(approval) 
let currentApprover;
// 현재 열린 문서 상태 (예: '반려', '1차대기', '완료' 등)
let currentDocStatus = null;
// 모달의 결재확인 버튼

// 결제확인 버튼
const approvalCheckBtn = document.getElementById('approvalCheckBtn');
// 반려 버튼
const approvalCompanionBtn = document.getElementById('approvalCompanionBtn');

// ========================================================
// v- 결재권한자
let elemApproverIdNum = null;//결재권한자 count 중요! 꼬이면안됨
// ========================================================

// f- 결재확인 버튼 눌렀을때 동작할 함수
approvalCheckBtn.addEventListener('click', () => {
	patchApproval("accept");
});

// f- 반려버튼 눌렀을때 동작할 함수
approvalCompanionBtn.addEventListener('click', () => {
	patchApproval("deny")
});

// f- null-safe 날짜 변환 함수
function toDateStr(value) {
	if (!value) return '';              // null, undefined, '' 전부 빈 문자열 처리
	return String(value).split('T')[0]; // 혹시 문자열 아니어도 방어
}


// f- 현재 로그인한 사용자와 결재권자 비교
function checkApprover() {
	if (currentApprover != LOGIN_USER_ID) {
		alert("승인 또는 반려권한이 없습니다.");
		return true;
	}
}

// v- 결재 시 첨부된 도장 이미지 (Base64)
let approvalStampImage = null;
// approvalStampImage가 반려 도장인지 여부
let approvalStampIsReject = false;

// 기본 반려 도장 이미지 경로(프로젝트의 static 폴더에 이미지 파일을 두세요)
// 필요하면 서버에 업로드된 이미지 경로로 변경 가능합니다.
// Spring Boot의 정적 리소스는 기본적으로 classpath:/static 아래가 루트 URL이 됩니다.
// 정적 파일을 `src/main/resources/static/img/reject_stamp.png`에 두었다면 접근 URL은 `/img/reject_stamp.png` 입니다.
let DEFAULT_REJECT_STAMP_URL = apiUrl(`img/reject_stamp.png`);

// URL 또는 파일을 DataURL(Base64)로 변환 (필요 시 사용)
async function urlToDataURL(url) {
	try {
		const resp = await fetch(url, { credentials: 'same-origin' });
		if (!resp.ok) throw new Error('이미지 로드 실패:' + resp.status);
		const blob = await resp.blob();
		return await new Promise((resolve, reject) => {
			const reader = new FileReader();
			reader.onloadend = () => resolve(reader.result);
			reader.onerror = reject;
			reader.readAsDataURL(blob);
		});
	} catch (e) {
		console.error('urlToDataURL error', e);
		return null;
	}
}

// f- 저장된 도장 이미지 불러오기 함수
async function loadApprovalStamps(approvalId) {
	try {
		const response = await fetch(apiUrl(`api/approvals/stamps/${approvalId}`));
		if (!response.ok) {
			console.debug('도장 이미지 조회 실패');
			return {};
		}
		const stampImages = await response.json();
		console.debug('불러온 도장 이미지:', stampImages);
		return stampImages; // { "1": "/files/download/123", "2": "/files/download/124", ... }
	} catch (error) {
		console.error('도장 이미지 로드 오류:', error);
		return {};
	}
}

// f- 결재권자별 도장 이미지 표시 함수
async function displayStampsForApprovers(approvalId) {
	const stampImages = await loadApprovalStamps(approvalId);

	// 결재권자 div들을 순회하며 도장 이미지 표시
	const approverDivs = document.querySelectorAll('#approver > div');
	approverDivs.forEach((div, index) => {
		// HTML div 순서가 아닌 실제 결재 순서를 찾아야 함
		// 일단 HTML div 순서를 기본값으로 사용
		const displayOrder = (index + 1).toString(); // 화면 표시 순서: 1, 2, 3
		
		// p 태그에서 empId 추출
		const pTag = div.querySelector('p');
		let empId = null;
		if (pTag && pTag.textContent) {
			const match = pTag.textContent.match(/\((\d+)\)/);
			if (match) {
				empId = match[1];
			}
		}
		
		// 화면 순서 기준 또는 empId 기준으로 도장 찾기
		let stampUrl = stampImages[displayOrder]; // 우선 화면 순서로 찾기
		if (!stampUrl && empId) {
			stampUrl = stampImages['emp_' + empId]; // empId로 찾기
		}

		// 디버그: 어떤 값들이 왔는지 확인
		console.debug(`[도장 디버그] displayOrder=${displayOrder}, empId=${empId}, stampUrl=`, stampUrl, ` approvalStampImage present=`, !!approvalStampImage, ` approvalStampIsReject=`, approvalStampIsReject, ` currentDocStatus=`, currentDocStatus, ` pText=`, pTag ? pTag.textContent : 'N/A');

		// 우선순위 변경 (요청사항): 문서 상태가 '반려'일 때는
		// - 해당 결재권자(=반려를 실행한 사용자)의 자리에는 항상 반려 도장을 표시합니다.
		//   세션에 반려 도장(approvalStampImage && approvalStampIsReject)이 있으면 우선 사용,
		//   없으면 DEFAULT_REJECT_STAMP_URL 사용.
		// - 다른 결재권자의 경우 기존 정책(서버 도장 우선) 유지.
		let finalStamp = null;
		const isThisApproverCurrentUser = pTag && pTag.textContent && (pTag.textContent.includes(LOGIN_USER_NAME) || pTag.textContent.includes(`(${LOGIN_USER_ID})`));

		if (currentDocStatus === '반려' && isThisApproverCurrentUser) {
			if (approvalStampImage && approvalStampIsReject) {
				finalStamp = approvalStampImage;
			} else {
				finalStamp = DEFAULT_REJECT_STAMP_URL;
			}
		} else if (stampUrl) {
			// 일반적인 경우: 서버(DB) 도장이 있으면 우선 사용
			finalStamp = stampUrl;
		} else if (currentDocStatus === '반려' && !isThisApproverCurrentUser) {
			// 문서가 반려인데 현재 결재권자가 반려 실행자가 아니라면
			// 서버 도장이 없으면 '(인)' 표시는 유지 (기존 동작)
			finalStamp = null;
		} else {
			// 작성/완료 등 반려가 아닐 때는 DB 도장이 없으면 '(인)'
			finalStamp = null;
		}

		if (pTag && finalStamp) {
			updateStampPreview(finalStamp, pTag);
		}
	});
}

// f- 도장 미리보기 업데이트 함수 (수정: pTag를 파라미터로 받도록)
function updateStampPreview(imageUrl, targetPTag = null) {
	// targetPTag가 없으면 기본 선택자 사용
	const approverPTag = targetPTag || document.querySelector('#approver > div p');

	// 결재란의 p 태그를 찾지 못하면 종료
	if (!approverPTag) return;

	// 기존의 도장 스탬프 컨테이너(.approver-stamp)를 찾아 제거 (재할당을 위해)
	const existingStamp = approverPTag.querySelector('.approver-stamp');
	if (existingStamp) {
		existingStamp.remove();
	}

	// 새 도장 컨테이너 생성 (인쇄 로직과 동일하게)
	const stampDiv = document.createElement('div');
	stampDiv.className = 'approver-stamp';
	stampDiv.style.cssText = 'width: 60px; height: 60px; display: flex; align-items: center; justify-content: center; margin: 10px auto;';

	if (imageUrl) {
		const stampImg = document.createElement('img');
		// 시각적 스타일
		stampImg.style.cssText = 'max-width: 100%; max-height: 100%; object-fit: contain;';
		// 안전하게 CORS 허용 시도
		try { stampImg.crossOrigin = 'Anonymous'; } catch (e) {}
		stampImg.src = imageUrl;

		// 이미지 로드 실패 시 urlToDataURL로 재시도하여 Base64로 교체
		stampImg.addEventListener('error', async (ev) => {
			console.warn('도장 이미지 로드 실패, DataURL로 재시도:', imageUrl);
			const data = await urlToDataURL(imageUrl);
			if (data) {
				stampImg.src = data;
			} else {
				console.error('도장 DataURL 변환 실패:', imageUrl);
			}
		});

		stampDiv.appendChild(stampImg);
	} else {
		// 이미지 없는 경우 (인) 텍스트 표시 로직
		stampDiv.textContent = '(인)';
		stampDiv.style.cssText += 'border: 1px dotted black; border-radius: 50%;';
	}

	// p 태그의 가장 아래쪽에 삽입
	approverPTag.appendChild(stampDiv);
}

// f- 결재 패치 보내기 함수
async function patchApproval(btn) {
	// 현재 로그인한 사용자와 결재권자 비교
	if (checkApprover()) return;
	let msg = "";
	btn == 'accept' ? msg = "승인하시겠습니까?" : msg = "반려하시겠습니까?"


	// 결재권한자와 사용자가 동일인물일 때
	if (confirm(msg)) {
		let stampToSave = null; // 서버에 보낼 도장 데이터 변수 초기화

		if (btn === 'accept') {
			// 승인 시 도장 이미지 선택
			const stampBase64 = await selectStampImage();
				if (stampBase64) {
				approvalStampImage = stampBase64;
					approvalStampIsReject = false;
				stampToSave = stampBase64;

				// 현재 결재권자의 위치를 찾아서 도장 미리보기 표시
				const approverDivs = document.querySelectorAll('#approver > div');
				approverDivs.forEach((div, index) => {
					const pTag = div.querySelector('p');
					if (pTag && pTag.textContent.includes(LOGIN_USER_NAME)) {
						updateStampPreview(approvalStampImage, pTag);
					}
				});
			} else {
				alert("도장이 선택되지 않아 승인을 취소합니다.");
				return;
			}
		} else {
				// 반려 시 자동으로 반려 도장 생성 (PNG 파일을 읽어 Base64로 변환)
				const rejectStamp = await createRejectStamp();
				approvalStampImage = rejectStamp;
				approvalStampIsReject = true;
				stampToSave = rejectStamp;

			// 현재 결재권자의 위치를 찾아서 반려 도장 미리보기 표시
			const approverDivs = document.querySelectorAll('#approver > div');
			approverDivs.forEach((div, index) => {
				const pTag = div.querySelector('p');
				if (pTag && pTag.textContent.includes(LOGIN_USER_NAME)) {
					updateStampPreview(approvalStampImage, pTag);
				}
			});
		}

		// 도장 데이터(stampToSave)를 전달
		sendApprovalRequest(btn, stampToSave);
	}
}

// f- 반려 도장 자동 생성 함수
// 이제 서버의 PNG 파일을 읽어 Base64(DataURL)로 변환하여 반환합니다.
// 실패할 경우 기존 Canvas 기반 생성(기본 동작)을 사용합니다.
async function createRejectStamp() {
	try {
		// 이미 DEFAULT_REJECT_STAMP_URL이 data:로 시작하면 그대로 반환
		if (DEFAULT_REJECT_STAMP_URL && DEFAULT_REJECT_STAMP_URL.startsWith('data:')) {
			return DEFAULT_REJECT_STAMP_URL;
		}
		// 서버에 있는 PNG 파일을 fetch하여 DataURL로 변환
		const data = await urlToDataURL(DEFAULT_REJECT_STAMP_URL);
		if (data) return data;
		console.warn('기본 반려 도장 PNG를 DataURL로 변환하지 못했습니다. Canvas로 대체 생성합니다.');
	} catch (e) {
		console.error('createRejectStamp fetch error', e);
	}

}
/// f- 도장 이미지 선택 함수 (PNG만 허용)
function selectStampImage() {
	return new Promise((resolve) => {
		const input = document.createElement('input');
		input.type = 'file';
		input.accept = 'image/png';


		input.onchange = (e) => {
			const file = e.target.files[0];

			if (!file) {
				resolve(null);
				return;
			}

			const reader = new FileReader();
			reader.onload = (event) => {
				// 성공적으로 Base64 문자열 획득 후 resolve
				resolve(event.target.result);
			};
			reader.onerror = () => {
				resolve(null);
			};
			reader.readAsDataURL(file);
		};
		input.click();
	});
}

// f- 결재 요청 전송 함수
function sendApprovalRequest(btn, stampBase64) {
	// 요청 본문(Body)에 포함할 데이터 객체 생성
	const requestBody = {
		// stampBase64는 승인/반려 모두 전달 가능하도록 포함합니다.
		stampImage: stampBase64 || null,
		// 필요한 경우, 결재 상태나 기타 데이터를 추가합니다.
		action: btn
	};
	fetch(apiUrl(`api/approvals/${approvalId}`, {
		method: 'PATCH'
		, headers: {
			[csrfHeader]: csrfToken
			, 'Content-Type': 'application/json'
		}
		, body: JSON.stringify(requestBody)
	})
		.then(response => {
			if (!response.ok) return response.json().then(err => { throw new Error(err.result); });
			return response.json();
		})
		.then(data => {
			alert(data.result);
			// 서버 저장이 완료되면 화면에 세션 도장을 표시 (반려 포함)
			if (stampBase64) {
				try {
					displaySessionStampOnUI(stampBase64, btn);
				} catch (e) { console.error('세션 도장 표시 실패', e); }
			}
			// 사용자에게 잠깐 보여준 뒤 페이지 새로고침하여 상태 반영
			setTimeout(() => { location.reload(); }, 800);

		}).catch(error => {
			console.error('에러', error)
			alert("결재 승인 실패: " + error.message);
		});
}

// UI에 세션 도장을 표시하는 헬퍼: 원본 결재칸 및 모달에 도장 추가하고 'rejected' 클래스 적용
function displaySessionStampOnUI(stampBase64, action) {
	if (!stampBase64) return;
	// 원본에서 현재 결재권자 위치 찾기 (currentApprover는 서버에서 오는 값)
	const approverPs = Array.from(document.querySelectorAll('#approver > div p'));
	let targetP = approverPs.find(p => p.textContent && (p.textContent.includes(LOGIN_USER_NAME) || p.textContent.includes(`(${LOGIN_USER_ID})`)));
	if (!targetP) targetP = approverPs[0];
	if (targetP) {
		updateStampPreview(stampBase64, targetP);
		// '반려' 도장은 상태가 반려이거나 action이 'deny'일 때만 rejected 클래스 적용
		if (action === 'deny' || currentDocStatus === '반려') {
			const div = targetP.closest('div'); if (div) try { div.classList.add('rejected'); } catch (e) {}
		}
	}
	// modal에도 반영
	const modal = document.getElementById('modal-doc');
	if (modal) {
		const modalP = Array.from(modal.querySelectorAll('#approver > div p')).find(p => p.textContent && (p.textContent.includes(LOGIN_USER_NAME) || p.textContent.includes(`(${LOGIN_USER_ID})`)));
		if (modalP) {
			updateStampPreview(stampBase64, modalP);
			if (action === 'deny' || currentDocStatus === '반려') {
				const mdiv = modalP.closest('div'); if (mdiv) try { mdiv.classList.add('rejected'); } catch (e) {}
			}
		}
	}
}

// f- 결제상세보기 => 결제권자 정보 불러오기함수
async function getApproverList(approvalId) {
	try {
		const response = await fetch(`/api/approvals/approvers/${approvalId}`, { method: 'GET' });

		if (!response.ok) {
			const errorData = await response.json();
			throw new Error(errorData.result);
		}
		const data = await response.json();
		return data;
	} catch (error) {
		alert("결재권자 목록을 불러올 수 없습니다!");
		return null;
	}
}

// f- 그리드 상세에서 결재자 목록을 로드하고 UI에 표시하는 함수
async function loadAndDisplayApprovers(approvalId) {
	const approverList = await getApproverList(approvalId);

	let sortedList;
	if (approverList.length > 0) {
		// 원본 배열을 복사한 후 정렬 (원본 보존)
		sortedList = [...approverList].sort((a, b) => {
			return Number(a.orderApprovers) - Number(b.orderApprovers);
		});
		console.debug("approverList (정렬 후)---------------->", approverList);
		console.debug("sortedList---->", sortedList);
		
		// selectBox의 모든 아이템을 매핑 객체로 만들기 (한 번만 실행)
		const empMapping = {};
		const selectBoxItems = selectBox.getItems();
		selectBoxItems.forEach(item => {
			empMapping[item.value] = item.label;
		});

		window.count = 0;
		approverDiv.innerHTML = "";

		//넣어줄때문제가 생김
		for (const approver of sortedList) {
			console.log('approver:', approver);
			console.log('looking for empId:', approver.empId);
			const empLabel = empMapping[approver.empId];
			console.log('found empLabel:', empLabel);
			if (empLabel) {
				print("detail", empLabel);
			} else {
				console.warn('empLabel not found for empId:', approver.empId);
			}
		}

		// 도장 이미지 불러오기 및 표시
		await displayStampsForApprovers(approvalId);
	}
}

//grid - 1.결재사항 - 진행해야할 결재만 - 결재권한자만 볼수있음
//grid - 2.전체결재 - 나와관련된 모든 결재문서
//grid - 3.내 결재목록 - 내가 기안한 문서
//grid - 4.결재대기 - 나와관련된 모든 결재대기
//grid - 5.결재완료 - 나와 관련된 결재완료한 문서
window.onload = function () {
	AllGridSearch();//조회버튼
	empData();
}

let approverDiv = document.querySelector('#approver');

let itemData;
	let selectBox = null;
	let modalOpenedFromGrid = false;


// f- selectbox - 인사정보 불러오기
async function empData() {
	try {
		const response = await fetch("/approval/empList");
		const data = await response.json();
		itemData = [];
		let obj = {};
		//console.log(data);
		data.map((item, index) => {
			obj["value"] = item[0]; //사번
			obj["label"] = item[1] + "(" + item[0] + ")"; //이름(사번)
			itemData.push(obj);
			obj = {};
		});
			selectBox;
		//셀렉트박스 - 토스트유아이
		selectBox = new tui.SelectBox('#select-box', {
			data: itemData
			,
			theme: {
				'common.border': '1px solid #e4e4e4',
				'common.color': '#353535',
				'common.background': '#fcfcfc',
				'common.width': '400px',
				'common.height': '45px',
				'common.border-radius': '5px',

				'common.disabled.background': '#eceef1',
				'common.disabled.color': '#708093',

				'dropdown.maxHeight': '300px',

				'itemGroup.items.paddingLeft': '15px',

				'itemGroup.label.color': 'black',
				}
		});

		//셀렉트박스 닫힐때
		selectBox.on('close', (ev) => {
			let selectlabel = selectBox.getSelectedItem().label;
			let approverEmpId = selectBox.getSelectedItem().value;

			if (selectlabel != null && approverArr.length < 3) {//셀렉트 라벨선택시 3번까지만셈
				print(ev.type, selectlabel);
				approverArr.push({
					empId: approverEmpId
					, approverOrder: window.count
					, delegateStatus: 'N' //여기서 전결상태도 불러오자
					, originalEmpId: approverEmpId // 초기 사번 저장
				});
				console.debug("approverArr:", approverArr);
			}

		});
		//const modal = document.getElementById('approval-modal');
		//그리드 1클릭시 상세버튼
		grid1.on("click", async (ev) => {

			const target = ev.nativeEvent.target;
			// const targetElement = ev.nativeEvent.target; 이 줄이 빠진 경우
			if (ev.targetType === 'cell' && target.tagName === 'BUTTON') {

				const rowData = grid1.getRow(ev.rowKey);
				// mark modal opened from grid so approvalNo clicks are ignored
				modalOpenedFromGrid = true;
				$('#approval-modal').modal('show');

				document.getElementById('saveBtn').style.display = "none";//approvalCompanionBtn//approvalCheckBtn
				document.getElementById('attachmentBtn').style.display = "none";//첨부파일
				document.getElementById('downloadArea').style.display = "block";//다운로드
				document.getElementById('approvalCompanionBtn').style.display = "inline-block";//반려
				document.getElementById('approvalCheckBtn').style.display = "inline-block";//결재확인
				// 문서 열릴때 approvalId에 현재 열린 문서id 저장
				approvalId = rowData.approval_id;
				// 현재 문서 상태 저장
				currentDocStatus = rowData.doc_status || rowData.status || null;
				getApprovalDocFileData(approvalId);
				// 문서 열릴때 현재 결재권자(approval) 저장
				currentApprover = rowData.approver;
				console.debug("rowData", rowData);//DraftingHidden
				document.getElementById('Drafting').innerText = rowData.approval_title;
				document.getElementById('DraftingHidden').value = rowData.approval_title;
				//document.getElementById('Drafting').value = rowData.approval_title;
				document.getElementById('today-date').innerText = toDateStr(rowData.created_date);//결재 작성날짜 = 결재시작일
				document.getElementById('approval-title').value = rowData.approval_title;
				//양식종류 form-menu
				document.getElementById('approver-name').value = rowData.emp_id;//결재자명
				document.getElementById('form-menu').value = rowData.form_type;//양식종류
				//const createdDate = rowData.created_date;
				document.getElementById('create-date').value = toDateStr(rowData.created_date);//결재시작일 =결재 작성날짜 
				document.getElementById('finish-date').value = toDateStr(rowData.finish_date);//결재완료날짜
				//휴가 연차신청서 
				document.getElementById('start-date').value = toDateStr(rowData.start_date); //휴가시작날짜
				document.getElementById('end-date').value = toDateStr(rowData.end_date); //휴가종료날짜
				//document.getElementById('leave-radio').value = rowData.leave_type;// 연차유형 라디오- 없앳음 -휴가종류로 들어감
				document.getElementById('leave-type').value = rowData.leave_type;//휴가종류
				console.debug("rowData.to_dept_id", rowData.to_dept_id);
				document.getElementById('position').value = rowData.to_pos_code;
				document.getElementById('to-dept-id').value = rowData.to_dept_id;//발령부서
				document.getElementById('expnd-type').value = rowData.expnd_type;//지출종류EXPND_TYPE
				//document.getElementById('approver').value = rowData.approver;//결재권한자
				//상세버튼 클릭시 디폴트 결재권한자 div 생기게하는 로직
				await loadAndDisplayApprovers(approvalId);
				//document.getElementById('approver').innerText = rowData.approver;//전결자
				document.getElementById('reason-write').value = rowData.reason;//결재사유내용
				updateCharCount(); // 글자 수 카운터 업데이트
				selectBox.disable();
				// 상세버튼 양식종류에 따른 form 보이기/숨기기
				formChange(rowData.form_type);
				formDisable();
			}
		});

		grid2.on("click", async (ev) => {

			const target = ev.nativeEvent.target;
			if (ev.targetType === 'cell' && target.tagName === 'BUTTON') {
				const rowData = grid2.getRow(ev.rowKey);
				modalOpenedFromGrid = true;
				$('#approval-modal').modal('show');

				document.getElementById('saveBtn').style.display = "none";
				document.getElementById('attachmentBtn').style.display = "none";//첨부파일
				document.getElementById('downloadArea').style.display = "block";//다운로드
				document.getElementById('approvalCompanionBtn').style.display = "inline-block";//반려
				document.getElementById('approvalCheckBtn').style.display = "inline-block";//결재확인

				// 문서 열릴때 approvalId에 현재 열린 문서id 저장
				approvalId = rowData.approval_id;
				// 현재 문서 상태 저장
				currentDocStatus = rowData.doc_status || rowData.status || null;
				getApprovalDocFileData(approvalId);
				// 문서 열릴때 현재 결재권자(approval) 저장
				currentApprover = rowData.approver;

				document.getElementById('Drafting').innerText = rowData.approval_title;
				document.getElementById('DraftingHidden').value = rowData.approval_title;
				document.getElementById('today-date').innerText = toDateStr(rowData.created_date);//결재 작성날짜 = 결재시작일
				document.getElementById('approval-title').value = rowData.approval_title;
				document.getElementById('form-menu').value = rowData.form_type;//양식종류//양식종류form-menu
				document.getElementById('approver-name').value = rowData.emp_id;//결재자명
				document.getElementById('create-date').value = toDateStr(rowData.created_date);//결재시작일 =결재 작성날짜 
				document.getElementById('finish-date').value = toDateStr(rowData.finish_date);//결재완료날짜
				//휴가 연차신청서 
				document.getElementById('start-date').value = toDateStr(rowData.start_date); //휴가시작날짜
				document.getElementById('end-date').value = toDateStr(rowData.end_date); //휴가종료날짜
				//document.getElementById('leave-radio').value = rowData.leave_type;// 연차유형 라디오- 없앳음 -휴가종류로 들어감
				document.getElementById('leave-type').value = rowData.leave_type;//휴가종류
				document.getElementById('position').value = rowData.to_pos_code;//변경직급
				document.getElementById('to-dept-id').value = rowData.to_dept_id;//발령부서
				document.getElementById('expnd-type').value = rowData.expnd_type;//지출종류EXPND_TYPE
				//document.getElementById('approver').value = rowData.approver;//결재권한자

				// 결재자 목록 로드 및 표시
				await loadAndDisplayApprovers(approvalId);
				//document.getElementById('approver').innerText = rowData.approver;//전결자
				document.getElementById('reason-write').value = rowData.reason;//결재사유내용
				updateCharCount(); // 글자 수 카운터 업데이트
				selectBox.disable();
				formChange(rowData.form_type);
				formDisable();
			}
		});


		grid3.on("click", async (ev) => {

			const target = ev.nativeEvent.target;
			if (ev.targetType === 'cell' && target.tagName === 'BUTTON') {
				const rowData = grid3.getRow(ev.rowKey);
					modalOpenedFromGrid = true;
					$('#approval-modal').modal('show');

				document.getElementById('saveBtn').style.display = "none";
				document.getElementById('attachmentBtn').style.display = "none";//첨부파일
				document.getElementById('downloadArea').style.display = "block";//다운로드
				document.getElementById('approvalCompanionBtn').style.display = "inline-block";//반려
				document.getElementById('approvalCheckBtn').style.display = "inline-block";//결재확인

				// 문서 열릴때 approvalId에 현재 열린 문서id 저장
				approvalId = rowData.approval_id;
				// 현재 문서 상태 저장
				currentDocStatus = rowData.doc_status || rowData.status || null;
				getApprovalDocFileData(approvalId);
				// 문서 열릴때 현재 결재권자(approval) 저장
				currentApprover = rowData.approver;

				document.getElementById('Drafting').innerText = rowData.approval_title;
				document.getElementById('DraftingHidden').value = rowData.approval_title;
				document.getElementById('today-date').innerText = toDateStr(rowData.created_date);//결재 작성날짜 = 결재시작일
				document.getElementById('approval-title').value = rowData.approval_title;
				document.getElementById('form-menu').value = rowData.form_type;//양식종류//양식종류form-menu
				document.getElementById('approver-name').value = rowData.emp_id;//결재자명
				console.debug("rowData.created_date", toDateStr(rowData.created_date));
				const createdDate = rowData.created_date;
				document.getElementById('create-date').value = toDateStr(rowData.created_date);//결재시작일 =결재 작성날짜 
				document.getElementById('finish-date').value = toDateStr(rowData.finish_date);//결재완료날짜
				//휴가 연차신청서 
				document.getElementById('start-date').value = toDateStr(rowData.start_date); //휴가시작날짜
				document.getElementById('end-date').value = toDateStr(rowData.end_date); //휴가종료날짜
				//document.getElementById('leave-radio').value = rowData.leave_type;// 연차유형 라디오- 없앳음 -휴가종류로 들어감
				document.getElementById('leave-type').value = rowData.leave_type;//휴가종류
				document.getElementById('position').value = rowData.to_pos_code; //변경직급
				document.getElementById('to-dept-id').value = rowData.to_dept_id;//발령부서
				document.getElementById('expnd-type').value = rowData.expnd_type;//지출종류EXPND_TYPE
				//document.getElementById('approver').value = rowData.approver;//결재권한자
				await loadAndDisplayApprovers(approvalId);
				//document.getElementById('approver').innerText = rowData.approver;//전결자
				document.getElementById('reason-write').value = rowData.reason;//결재사유내용
				updateCharCount(); // 글자 수 카운터 업데이트
				selectBox.disable();
				formChange(rowData.form_type);
				formDisable();
			}
		});

		grid4.on("click", async (ev) => {

			const target = ev.nativeEvent.target;
			if (ev.targetType === 'cell' && target.tagName === 'BUTTON') {
				const rowData = grid4.getRow(ev.rowKey);
				modalOpenedFromGrid = true;
				$('#approval-modal').modal('show');

				document.getElementById('saveBtn').style.display = "none";//등록버튼
				document.getElementById('attachmentBtn').style.display = "none";//첨부파일
				document.getElementById('downloadArea').style.display = "block";//다운로드
				document.getElementById('approvalCompanionBtn').style.display = "inline-block";//반려버튼
				document.getElementById('approvalCheckBtn').style.display = "inline-block";//결재확인버튼

				// 문서 열릴때 approvalId에 현재 열린 문서id 저장
				approvalId = rowData.approval_id;
				// 현재 문서 상태 저장
				currentDocStatus = rowData.doc_status || rowData.status || null;
				getApprovalDocFileData(approvalId);
				// 문서 열릴때 현재 결재권자(approval) 저장
				currentApprover = rowData.approver;

				document.getElementById('Drafting').innerText = rowData.approval_title;
				document.getElementById('DraftingHidden').value = rowData.approval_title;
				document.getElementById('today-date').innerText = toDateStr(rowData.created_date);//결재 작성날짜 = 결재시작일
				document.getElementById('approval-title').value = rowData.approval_title;
				document.getElementById('form-menu').value = rowData.form_type;//양식종류//양식종류form-menu
				document.getElementById('approver-name').value = rowData.emp_id;//결재자명

				const createdDate = rowData.created_date;
				document.getElementById('create-date').value = toDateStr(rowData.created_date);//결재시작일 =결재 작성날짜 
				document.getElementById('finish-date').value = toDateStr(rowData.finish_date);//결재완료날짜
				//휴가 연차신청서 
				document.getElementById('start-date').value = toDateStr(rowData.start_date); //휴가시작날짜
				document.getElementById('end-date').value = toDateStr(rowData.end_date); //휴가종료날짜
				//document.getElementById('leave-radio').value = rowData.leave_type;// 연차유형 라디오- 없앳음 -휴가종류로 들어감
				document.getElementById('leave-type').value = rowData.leave_type;//휴가종류
				document.getElementById('position').value = rowData.to_pos_code; //변경직급
				document.getElementById('to-dept-id').value = rowData.to_dept_id;//발령부서
				document.getElementById('expnd-type').value = rowData.expnd_type;//지출종류EXPND_TYPE
				//document.getElementById('approver').value = rowData.approver;//결재권한자
				await loadAndDisplayApprovers(approvalId);
				//document.getElementById('approver').innerText = rowData.approver;//전결자
				document.getElementById('reason-write').value = rowData.reason;//결재사유내용
				updateCharCount(); // 글자 수 카운터 업데이트
				selectBox.disable();
				formDisable();
			}
		});

		grid5.on("click", async (ev) => {

			const target = ev.nativeEvent.target;
			if (ev.targetType === 'cell' && target.tagName === 'BUTTON') {
				const rowData = grid5.getRow(ev.rowKey);
				$('#approval-modal').modal('show');

				document.getElementById('saveBtn').style.display = "none";
				document.getElementById('attachmentBtn').style.display = "none";//첨부파일
				document.getElementById('downloadArea').style.display = "block";//다운로드
				document.getElementById('approvalCompanionBtn').style.display = "inline-block";//반려
				document.getElementById('approvalCheckBtn').style.display = "inline-block";//결재확인
				// 문서 열릴때 approvalId에 현재 열린 문서id 저장
				approvalId = rowData.approval_id;
				// 현재 문서 상태 저장
				currentDocStatus = rowData.doc_status || rowData.status || null;
				getApprovalDocFileData(approvalId);
				// 문서 열릴때 현재 결재권자(approval) 저장
				currentApprover = rowData.approver;

				document.getElementById('Drafting').innerText = rowData.approval_title;
				document.getElementById('DraftingHidden').value = rowData.approval_title;
				document.getElementById('today-date').innerText = rowData.created_date.split('T')[0];//결재 작성날짜 = 결재시작일
				document.getElementById('approval-title').value = rowData.approval_title;
				document.getElementById('form-menu').value = rowData.form_type;//양식종류//양식종류form-menu
				document.getElementById('approver-name').value = rowData.emp_id;//결재자명

				const createdDate = rowData.created_date;
				document.getElementById('create-date').value = toDateStr(rowData.created_date);//결재시작일 =결재 작성날짜 
				document.getElementById('finish-date').value = toDateStr(rowData.finish_date);//결재완료날짜
				//휴가 연차신청서 
				document.getElementById('start-date').value = toDateStr(rowData.start_date); //휴가시작날짜
				document.getElementById('end-date').value = toDateStr(rowData.end_date); //휴가종료날짜
				//document.getElementById('leave-radio').value = rowData.leave_type;// 연차유형 라디오- 없앳음 -휴가종류로 들어감
				document.getElementById('leave-type').value = rowData.leave_type;//휴가종류
				document.getElementById('position').value = rowData.to_pos_code;//변경직급
				document.getElementById('to-dept-id').value = rowData.to_dept_id;//발령부서
				document.getElementById('expnd-type').value = rowData.expnd_type;//지출종류EXPND_TYPE
				//document.getElementById('approver').value = rowData.approver;//결재권한자
				await loadAndDisplayApprovers(approvalId);
				//document.getElementById('approver').innerText = rowData.approver;//전결자
				document.getElementById('reason-write').value = rowData.reason;//결재사유내용
				updateCharCount(); // 글자 수 카운터 업데이트
				selectBox.disable();
				formChange(rowData.form_type);
				formDisable();
			}
		});

		return itemData;
	} catch (error) {
		console.error('Error fetching data:', error);
	}
}

// f- 결재양식에따른 form 활성화/비활성화 함수
function formChange(formType) {
	if (formType == '지출결의서') {//attachmentBtn
		document.getElementById('expndTypeForm').style.display = 'flex';//지출종류
		document.getElementById('leavePeriodForm').style.display = 'none';// 휴가기간
		document.getElementById('leaveTypeForm').style.display = 'none';//휴가종류	
		document.getElementById('positionForm').style.display = 'none';//직급
		document.getElementById('toDeptForm').style.display = 'none'; //발령부서
		document.getElementById('expnd-type').required = true;
		document.getElementById('start-date').required = false;
		document.getElementById('end-date').required = false;
		document.getElementById('leave-type').required = false;//leave-type
		document.getElementById('position').required = false;
		document.getElementById('to-dept-id').required = false;

	} else if (formType == '연차신청서') {
		document.getElementById('expndTypeForm').style.display = 'none';//지출종류
		document.getElementById('leavePeriodForm').style.display = 'flex';// 휴가기간
		document.getElementById('leaveTypeForm').style.display = 'flex';//휴가종류	
		document.getElementById('positionForm').style.display = 'none';//직급
		document.getElementById('toDeptForm').style.display = 'none'; //발령부서
		document.getElementById('start-date').required = true;
		document.getElementById('end-date').required = true;
		document.getElementById('leave-type').required = true;//leave-type
		document.getElementById('position').required = false;
		document.getElementById('to-dept-id').required = false;
		document.getElementById('expnd-type').required = false;

	} else if (formType == '반차신청서') {
		document.getElementById('expndTypeForm').style.display = 'none';//지출종류
		document.getElementById('leavePeriodForm').style.display = 'flex';// 휴가기간
		document.getElementById('leaveTypeForm').style.display = 'flex';//휴가종류	
		document.getElementById('positionForm').style.display = 'none';//직급
		document.getElementById('toDeptForm').style.display = 'none'; //발령부서
		document.getElementById('start-date').required = true;
		document.getElementById('end-date').required = true;
		document.getElementById('leave-type').required = true;//leave-type
		document.getElementById('position').required = false;
		document.getElementById('to-dept-id').required = false;
		document.getElementById('expnd-type').required = false;
	} else if (formType == '인사발령신청서') {
		document.getElementById('expndTypeForm').style.display = 'none';//지출종류
		document.getElementById('leavePeriodForm').style.display = 'none';// 휴가기간
		document.getElementById('leaveTypeForm').style.display = 'none';//휴가종류	
		document.getElementById('positionForm').style.display = 'flex';//직급
		document.getElementById('toDeptForm').style.display = 'flex'; //발령부
		document.getElementById('start-date').required = false;
		document.getElementById('end-date').required = false;
		document.getElementById('leave-type').required = false;//leave-type
		document.getElementById('position').required = true;
		document.getElementById('to-dept-id').required = true;
		document.getElementById('expnd-type').required = false;

	} else if (formType == '자유양식결재서') {
		document.getElementById('expndTypeForm').style.display = 'none';//지출종류
		document.getElementById('leavePeriodForm').style.display = 'none';// 휴가기간
		document.getElementById('leaveTypeForm').style.display = 'none';//휴가종류	
		document.getElementById('positionForm').style.display = 'none';//직급
		document.getElementById('toDeptForm').style.display = 'none'; //발령부서
		document.getElementById('start-date').required = false;
		document.getElementById('end-date').required = false;
		document.getElementById('leave-type').required = false;//leave-type
		document.getElementById('position').required = false;
		document.getElementById('to-dept-id').required = false;
		document.getElementById('expnd-type').required = false;

	}
}
// f- 그리드 클릭시 상세보기 document.getElementById('myInput').disabled = true;
function formDisable() {
	document.getElementById('approval-title').disabled = true;
	document.getElementById('approver-name').disabled = true;
	document.getElementById('form-menu').disabled = true;
	document.getElementById('today-date').disabled = true;
	document.getElementById('Drafting').disabled = true;
	document.getElementById('DraftingHidden').disabled = true;
	document.getElementById('create-date').disabled = true;
	document.getElementById('finish-date').disabled = true;
	document.getElementById('start-date').disabled = true;
	document.getElementById('end-date').disabled = true;
	document.getElementById('leave-type').disabled = true;
	document.getElementById('position').disabled = true;
	document.getElementById('to-dept-id').disabled = true;
	document.getElementById('expnd-type').disabled = true;
	document.getElementById('reason-write').disabled = true;
	
	// 결재권한자 변경 div가 열려있다면 닫기
	if (jeongyeoljaDiv && jeongyeoljaDiv.style.display !== 'none') {
		jeongyeoljaDiv.style.display = 'none';
	}
	
	// 상세보기일 때 결재권한자 안내 문구 숨기기
	const approverInfo = document.getElementById('approverInfo');
	if (approverInfo) {
		approverInfo.style.display = 'none';
	}

}
//f- 기안서작성 클릭시 활성화 시켜주는 함수
function formEnable() {
	document.getElementById('approval-title').disabled = false;
	document.getElementById('approver-name').disabled = false;
	document.getElementById('form-menu').disabled = false;
	document.getElementById('today-date').disabled = false;
	document.getElementById('Drafting').disabled = false;
	document.getElementById('DraftingHidden').disabled = false;
	document.getElementById('create-date').disabled = false;
	document.getElementById('finish-date').disabled = false;
	document.getElementById('start-date').disabled = false;
	document.getElementById('end-date').disabled = false;
	document.getElementById('leave-type').disabled = false;
	document.getElementById('position').disabled = false; // 직급 필드 추가
	document.getElementById('to-dept-id').disabled = false;
	document.getElementById('expnd-type').disabled = false;
	document.getElementById('reason-write').disabled = false;
	// 작성일 때 결재권한자 안내 문구 보이기
	const approverInfo = document.getElementById('approverInfo');
	if (approverInfo) {
		approverInfo.style.display = 'block';
	}
}

//f- 글자 수 업데이트 함수 (전역 함수로 정의)
function updateCharCount() {
	const reasonTextarea = document.getElementById('reason-write');
	const charCount = document.getElementById('reason-char-count');
	
	if (reasonTextarea && charCount) {
		const currentLength = reasonTextarea.value.length;
		charCount.textContent = `${currentLength}/3000자`;
		
		// 3000자 초과 시 경고 스타일 적용
		if (currentLength > 2950) {
			charCount.style.color = currentLength >= 3000 ? 'red' : 'orange';
		} else {
			charCount.style.color = '#6c757d'; // 기본 Bootstrap muted 색상
		}
	}
}

//f- 모달 첨부파일
document.addEventListener('DOMContentLoaded', function () {

	const attachBtn = document.getElementById('attachmentBtn');
	const fileInput = document.getElementById('realFileInput');
	const listContainer = document.getElementById('fileListContainer');

	attachBtn.addEventListener('click', () => fileInput.click());
	fileInput.addEventListener('change', updateFileListDisplay);

	// 사유내용 글자 수 체크 기능 추가
	const reasonTextarea = document.getElementById('reason-write');
	const charCount = document.getElementById('reason-char-count');
	
	if (reasonTextarea && charCount) {
		// 여러 이벤트에 리스너 등록
		reasonTextarea.addEventListener('input', updateCharCount);
		reasonTextarea.addEventListener('keyup', updateCharCount);
		reasonTextarea.addEventListener('paste', function() {
			// 붙여넣기 후 약간의 지연을 두고 카운트 업데이트
			setTimeout(updateCharCount, 10);
		});
		
		// 초기 로드 시에도 카운트 표시
		updateCharCount();
	}

	function resetAttachments() {
		fileInput.value = ''; // input[type=file]의 파일 목록을 초기화
		updateFileListDisplay(); // 화면 목록 갱신 (목록을 비우고 "선택된 파일 없음" 표시)
	}
	// 파일 목록을 화면에 갱신하는 함수
	function updateFileListDisplay() {
		listContainer.innerHTML = '';
		const files = fileInput.files;
		// '선택된 파일 없음' 문구 표시/숨김
		//fileNameDisp.style.display = files.length > 0 ? 'none' : 'block';

		Array.from(files).forEach((file, index) => {
			const item = document.createElement('div');
			item.style.cssText = 'border-radius: 15px; display: flex; align-items: center; margin: 5px;';

			// 미리보기/아이콘 영역 생성
			const preview = createPreviewElement(file);
			item.appendChild(preview);
			// 파일 정보 영역 생성
			const info = document.createElement('div');

			// 파일 이름 (innerText 사용)
			const nameSpan = document.createElement('span');
			nameSpan.innerText = file.name;
			info.appendChild(nameSpan);
			// 삭제 버튼 생성 (innerText 사용 및 이벤트 연결)
			const deleteBtn = document.createElement('button');
			deleteBtn.innerText = '×';
			deleteBtn.type = 'button';

			deleteBtn.style.cssText = 'border: none; background: transparent; padding: 0; font-size: 18px; cursor: pointer;';
			deleteBtn.onclick = () => removeFile(index);
			info.appendChild(deleteBtn);
			item.appendChild(info);
			listContainer.appendChild(item);
		});
	}
	// 파일 유형에 따른 미리보기/아이콘 요소 생성
	function createPreviewElement(file) {
		const previewArea = document.createElement('div');
		previewArea.style.cssText = 'width: 50px; height: 50px; border: none; overflow: hidden; display: flex; justify-content: center; align-items: center;';
		if (file.type.startsWith('image/')) {
			const reader = new FileReader();
			reader.onload = (e) => {
				const img = document.createElement('img');
				img.src = e.target.result;
				img.style.cssText = 'width: 100%; height: 100%; object-fit: cover;';
				previewArea.appendChild(img);

			};
			reader.readAsDataURL(file);
		} else if (file.type === 'application/pdf') {
			previewArea.innerHTML = '<span style="font-size: 30px;">📄</span>';
		} else {
			previewArea.innerHTML = '<span style="font-size: 30px;">📎</span>';
		}
		return previewArea;
	}
	// 파일 삭제 로직 (DataTransfer 사용)
	function removeFile(indexToRemove) {
		const dt = new DataTransfer();
		const files = fileInput.files;
		for (let i = 0; i < files.length; i++) {
			if (i !== indexToRemove) {
				dt.items.add(files[i]);
			}
		}

		fileInput.files = dt.files;
		updateFileListDisplay();
	}

	window.resetAttachments = resetAttachments;

	const printButton = document.getElementById('printBtn');

	if (printButton) {
		printButton.addEventListener('click', async () => {
			const modalDoc = document.getElementById('modal-doc');
			if (!modalDoc) return;

			// 1. 인쇄를 위해 모달 내용을 복사합니다. (원본 폼 보호)
			const printElement = modalDoc.cloneNode(true);

			// 인쇄용 문서에서 <span>~</span> 요소들 제거
			const tildeSpans = printElement.querySelectorAll('span');
			tildeSpans.forEach(span => {
				if (span.textContent.trim() === '~') {
					span.remove();
				}
			});

			// A. 결재완료기간 (createdDate ~ finishDate) 처리
			const createdDate = document.getElementById('create-date')?.value || ' - ';
			const finishDate = document.getElementById('finish-date')?.value || ' - ';
			const completeDateDiv = printElement.querySelector('#create-date');

			if (completeDateDiv) {
				// 인쇄 시 두 칸을 대체할 통합 텍스트 노드 생성
				const combinedDateSpan = document.createElement('span');
				combinedDateSpan.textContent = `${createdDate} ~ ${finishDate}`;
				combinedDateSpan.style.padding = '3px 5px';
				combinedDateSpan.style.display = 'inline-block';
				combinedDateSpan.style.minWidth = '350px'; // 충분한 너비 확보

				// 기존의 <div class="row">를 통합된 <span>으로 대체
				completeDateDiv.parentNode.replaceChild(combinedDateSpan, completeDateDiv);
			}

			// B. 휴가기간 (startDate ~ endDate) 처리
			const startDate = document.getElementById('start-date')?.value || ' - ';
			const endDate = document.getElementById('end-date')?.value || ' - ';
			const leavePeriodDiv = printElement.querySelector('#start-date');

			if (leavePeriodDiv) {
				const combinedLeaveSpan = document.createElement('span');
				combinedLeaveSpan.textContent = `${startDate} ~ ${endDate}`;
				combinedLeaveSpan.style.padding = '3px 5px';
				combinedLeaveSpan.style.display = 'inline-block';
				combinedLeaveSpan.style.minWidth = '350px';

				leavePeriodDiv.parentNode.replaceChild(combinedLeaveSpan, leavePeriodDiv);
			}

			// ===================================================
			// 2. 불필요한 UI 요소 및 입력 필드를 정리하고 값으로 대체합니다.
			// ===================================================

			// 2.1. 입력 필드 (select, textarea, input[type=text] 등)를 값으로 대체
			// 참고: type="date" input은 위에서 이미 처리했으므로 이 루프에서는 대체되지 않습니다.
			printElement.querySelectorAll('input, select, textarea').forEach(input => {
				let displayValue = '';

				// 숨겨진 필드 및 날짜 필드 건너뛰기
				if (input.type === 'hidden' || input.type === 'date') {
					input.remove();
					return;
				}

				if (input.tagName === 'SELECT') {
					const originalSelect = document.getElementById(input.id);

					if (originalSelect && originalSelect.selectedIndex >= 0) {
						displayValue = originalSelect.options[originalSelect.selectedIndex].text;
					} else {
						displayValue = ' - ';
					}

				} else if (input.tagName === 'TEXTAREA' || input.tagName === 'INPUT') {
					displayValue = input.value || ' - ';
				}

				// 값만 표시하는 <span> 태그 생성 및 대체
				const displayNode = document.createElement('span');
				displayNode.textContent = displayValue;
				displayNode.style.display = 'inline-block';
				displayNode.style.minWidth = '200px';
				displayNode.style.paddingLeft = '5px';

				// 원본 입력 필드를 displayNode로 대체
				input.parentNode.replaceChild(displayNode, input);
			});

			// 2.2. 인쇄 시 불필요한 UI/버튼 영역 제거 (기존 로직 유지)
			printElement.querySelector('.btn-close')?.remove();
			//printElement.querySelector('.modal-footer')?.remove();
			printElement.querySelector('#attachmentBtn')?.remove();
			printElement.querySelector('#realFileInput')?.remove();
			printElement.querySelector('#select-box')?.remove();
			printElement.querySelector('#jeongyeolja')?.remove();
			printElement.querySelector('#approverInfo')?.remove();
			printElement.querySelector('#approvalCompanionBtn')?.remove();
			printElement.querySelector('#approvalCheckBtn')?.remove();
			printElement.querySelector('#printBtn')?.remove();
			printElement.querySelector('#saveBtn')?.remove();
			printElement.querySelector('#modalCloseBtn')?.remove();
			printElement.querySelector('#attachmentBtn')?.remove();
			printElement.querySelector('#realFileInput')?.remove();
			printElement.querySelector('#approver_close_1')?.remove();
			printElement.querySelector('#approver_close_2')?.remove();
			printElement.querySelector('#approver_close_3')?.remove();

			// hr 태그 제거 (구분선 제거)
			printElement.querySelectorAll('hr').forEach(hr => hr.remove());

			// 버튼이 있는 modal-footer 제거 (바닥에 "-" 문자 방지)
			printElement.querySelectorAll('.modal-footer').forEach(footer => {
				// 첨부파일 영역(downloadArea)이 있는 footer는 유지, 나머지는 제거
				if (!footer.querySelector('#downloadArea')) {
					footer.remove();
				} else {
					// downloadArea가 있는 footer에서도 버튼과 input 제거
					footer.querySelectorAll('button, input').forEach(el => el.remove());
				}
			});

			// 모든 빈 텍스트 노드 정리 (혹시 남아있는 "-" 등 제거)
			const walker = document.createTreeWalker(printElement, NodeFilter.SHOW_TEXT, null, false);
			const textNodesToClean = [];
			while (walker.nextNode()) {
				const node = walker.currentNode;
				if (node.textContent.trim() === '-' || node.textContent.trim() === '–' || node.textContent.trim() === '—') {
					textNodesToClean.push(node);
				}
			}
			textNodesToClean.forEach(node => node.remove());

			// 인쇄용: 결재권자 p태그 안에 도장 표시
			// 먼저 기존 도장 제거 (화면에서 복사된 도장 제거)
			printElement.querySelectorAll('.approver-stamp').forEach(stamp => stamp.remove());

			// 저장된 도장 이미지 불러오기
			const stampImages = await loadApprovalStamps(approvalId);
			console.debug('[인쇄] 불러온 도장 이미지:', stampImages);

			printElement.querySelectorAll('#approver > div p').forEach((pTag, index) => {
				const order = (index + 1).toString(); // 1, 2, 3
				const stampUrl = stampImages[order];
				console.debug(`[인쇄] ${order}차 결재권자 도장 URL:`, stampUrl);
				console.debug(`[인쇄-디버그] order=${order}, approvalStampImage present=`, !!approvalStampImage, ` approvalStampIsReject=`, approvalStampIsReject, ` currentDocStatus=`, currentDocStatus);

				const stampDiv = document.createElement('div');
				stampDiv.className = 'approver-stamp';
				stampDiv.style.cssText = 'width: 60px; height: 60px; display: flex; align-items: center; justify-content: center; margin: 10px auto;';

					// 도장 선택 규칙 (인쇄용, 개선):
					// - 문서가 '반려'일 때는 반려를 실행한 결재권자 자리(현재 로그인 사용자 포함)에는
					//   세션의 반려 도장(approvalStampImage && approvalStampIsReject)을 우선 표시하고, 없으면
					//   DEFAULT_REJECT_STAMP_URL을 표시합니다.
					// - 그 외 자리(다른 결재권자)는 기존 정책을 따릅니다: 서버(DB) 도장이 있으면 우선 사용.
					let finalStamp = null;
					const isThisApproverCurrentUserForPrint = pTag && pTag.textContent && (pTag.textContent.includes(LOGIN_USER_NAME) || pTag.textContent.includes(`(${LOGIN_USER_ID})`));

					if (currentDocStatus === '반려' && isThisApproverCurrentUserForPrint) {
						if (approvalStampImage && approvalStampIsReject) {
							finalStamp = approvalStampImage;
						} else {
							finalStamp = DEFAULT_REJECT_STAMP_URL;
						}
					} else if (stampUrl) {
						finalStamp = stampUrl;
					} else {
						finalStamp = null; // 비반려 문서 또는 서버 도장이 없을 때 '(인)'
					}

				if (finalStamp) {
					const stampImg = document.createElement('img');
					stampImg.style.cssText = 'max-width: 100%; max-height: 100%; object-fit: contain;';
					try { stampImg.crossOrigin = 'Anonymous'; } catch (e) {}
					stampImg.src = finalStamp;

					stampImg.addEventListener('error', async () => {
						console.warn('[인쇄] 도장 로드 실패, DataURL로 재시도:', finalStamp);
						const data = await urlToDataURL(finalStamp);
						if (data) {
							stampImg.src = data;
						} else {
							console.error('[인쇄] 도장 DataURL 변환 실패:', finalStamp);
						}
					});

					stampDiv.appendChild(stampImg);
					console.debug(`[인쇄] ${order}차 도장 이미지 추가됨 (final)`);
				} else {
					// 그래도 없으면 (인) 텍스트 표시
					stampDiv.textContent = '(인)';
					stampDiv.style.cssText += 'border: 1px dotted black; border-radius: 50%;';
					console.debug(`[인쇄] ${order}차 도장 없음 - (인) 표시`);
				}

				pTag.insertBefore(stampDiv, pTag.lastChild);
			});

			// 💡 2단계: printElement 내에 .approver-stamp 요소가 있는지 확인
			const stampElements = printElement.querySelectorAll('.approver-stamp');
			console.debug(`[DOM Check] 삽입된 .approver-stamp 개수: ${stampElements.length}`);

			if (stampElements.length > 0) {
				const hasImage = stampElements[0].querySelector('img');
				console.debug(`[DOM Check] 첫 번째 도장 요소에 img 태그 존재 여부: ${!!hasImage}`);
			}

			// 3. 인쇄용 첨부파일을 printElement 내에 이미지 포함으로 채웁니다.
			const printDownloadArea = printElement.querySelector('#downloadArea');
			if (printDownloadArea) {
				printDownloadArea.innerHTML = '파일 목록을 불러오는 중...';
				try {
					const resp = await fetch(`/approval/file/${approvalId}`);
					if (resp.ok) {
						const files = await resp.json();
						printDownloadArea.innerHTML = '';
						// 동기적으로 처리하여 Base64 변환을 순차적으로 수행
						for (const file of files) {
							const fileId = file.fileId;
							const fileName = file.originFileName || file.fileName;
							if (!fileId || !fileName) continue;
							if (isImageFile(fileName)) {
								// 인증이 필요할 수 있으므로 fetch 후 Blob -> DataURL 변환
								const dataUrl = await fetchImageAsDataURL(fileId);
								if (dataUrl) {
									printDownloadArea.appendChild(createFileLink(fileId, fileName, true, dataUrl));
									continue;
								}
								// 실패하면 일반 링크로 대체
							}
							printDownloadArea.appendChild(createFileLink(fileId, fileName, true));
						}
					} else {
						printDownloadArea.textContent = '첨부파일을 불러올 수 없습니다.';
					}
				} catch (e) {
					console.error('인쇄용 첨부파일 로드 실패:', e);
					printDownloadArea.textContent = '첨부파일을 불러올 수 없습니다.';
				}
			}

			// 4. 숨긴 iframe을 만들어 인쇄 내용을 삽입합니다. (새창 차단/팝업 이슈 회피)
			let printHTML = `
			    <html>
			    <head>
			        <title>기안서 인쇄</title>
					<!-- external css links removed: use app styles -->
			        <style>
			            /* --- 기존 기안서 필드 스타일 --- */
			            @page { margin: 2cm; }
			            body { font-family: 'Malgun Gothic', sans-serif; }
			            .modal-content { width: 800px; margin: 20px auto; padding: 30px;}
			            .modal-header { padding-bottom: 10px; margin-bottom: 20px; }
			            .modal-header h3 { font-size: 24px; text-align: center; }
						.modal-footer {
							border: none !important;
						}
			            h5 { 
			                display: flex; 
			                align-items: baseline; 
			                margin-bottom: 15px; 
			                border-bottom: 1px dashed #ccc; 
			                padding-bottom: 5px;
			            }
			            h5 label { font-weight: bold; width: 200px; flex-shrink: 0; }
			            .d-flex p { margin-left: 10px; }
			            /* 입력 필드 대체 <span>의 테두리 제거 */
			            h5 span { border: none; padding: 3px 5px; border-radius: 3px; }
					
			            /* =================================================== */
			            /* ⭐️ 첨부파일 목록 인쇄 CSS 추가 ⭐️ */
			            /* =================================================== */
					
			            /* 1. 컨테이너 스타일 */
			            #fileListContainer {
			                display: flex !important; /* Flex 레이아웃 유지 */
			                flex-wrap: wrap; /* 여러 줄로 표시 */
			                margin-top: 20px;
			                padding: 10px 0;
			            }
					
			            /* 2. 개별 파일 아이템 스타일 */
			            .file-preview-item {
			                display: flex;
			                flex-direction: column;
			                align-items: center;
			                margin-right: 15px;
			                padding: 5px;
			                border: none; /* 파일 구분을 위한 연한 테두리 제거 */
					
			                /* 중요: 내용이 페이지를 넘어갈 때 잘리지 않도록 함 */
			                page-break-inside: avoid; 
			                box-sizing: border-box;
			                max-width: 100px; /* JS에서 설정한 너비를 존중 */
			            }
					
			            /* 3. 이미지 자체 스타일 */
			            .file-preview-item img {
			                display: block !important; /* 이미지가 확실히 보이도록 강제 */
			                /* JS에서 설정된 width: 80px, height: 80px은 유지됩니다. */
			            }
					
			            /* 4. PDF 등 이미지가 아닌 파일 아이콘 스타일 */
			                /* JS에서 확장자 텍스트를 담는 div */
			                background-color: #f8f8f8 !important; /* 흰색 배경 유지 */
			            }
						
						/* 5. 인쇄시 모달 푸터 테두리 제거 (첨부파일 구분선 제거) */
						.modal-footer {
							border: none !important;
						}
						
						/* 6. 인쇄시 결재권자 테두리 추가 */
						#approver > div {
							border: 1px dotted black !important;
							border-radius: 5px !important;
							padding: 5px !important;
							margin: 5px !important;
							font-size: 10px !important;
							text-align: center !important;
							width: 190px !important;
							height: 280px !important;
						}
						
						/* 7. 도장 이미지가 인쇄 시 숨겨지지 않도록 강제 설정 */
						.approver-stamp {
						    /* 기존 스타일 유지 */
						    display: flex !important; 
						}
						.approver-stamp img {
						    display: block !important; 
						    /* Base64 이미지 로딩 실패 시 표시되지 않도록 크기를 명시적으로 지정 */
						    width: 100% !important; 
						    height: 100% !important; 
						    object-fit: contain !important;
						}
					
			        </style>
			    </head>
			    <body>
			        <div id="print-area">
			            ${printElement.innerHTML}
			        </div>
			    </body>
			    </html>
			`;

			// 숨긴 iframe 생성 및 쓰기
			const iframe = document.createElement('iframe');
			iframe.style.position = 'fixed';
			iframe.style.right = '0';
			iframe.style.bottom = '0';
			iframe.style.width = '0';
			iframe.style.height = '0';
			iframe.style.border = '0';
			iframe.style.visibility = 'hidden';
			iframe.id = 'print-iframe-' + Date.now();
			document.body.appendChild(iframe);

			const iframeWindow = iframe.contentWindow;
			const iframeDoc = iframeWindow.document;
			iframeDoc.open();
			iframeDoc.write(printHTML);
			iframeDoc.close();

			const cleanup = () => { try { document.body.removeChild(iframe); } catch (e) { } };
			try { iframeWindow.addEventListener('afterprint', cleanup, { once: true }); } catch (e) { }
			// 안전용: afterprint 미지원 브라우저 대비 타임아웃
			setTimeout(cleanup, 5000);

			// 모든 이미지가 로드될 때까지 기다린 후 인쇄
			iframeWindow.addEventListener('load', () => {
				const images = iframeDoc.querySelectorAll('img');
				console.debug('[인쇄] 총 이미지 개수:', images.length);

				if (images.length === 0) {
					// 이미지가 없으면 바로 인쇄
					iframeWindow.focus();
					iframeWindow.print();
					return;
				}

				let loadedCount = 0;
				const checkAllLoaded = () => {
					loadedCount++;
						console.debug(`[인쇄] 이미지 로드 진행: ${loadedCount}/${images.length}`);
						if (loadedCount === images.length) {
							console.debug('[인쇄] 모든 이미지 로드 완료, 인쇄 시작');
						setTimeout(() => {
							iframeWindow.focus();
							iframeWindow.print();
						}, 100);
					}
				};

				images.forEach((img, index) => {
					if (img.complete) {
						console.debug(`[인쇄] 이미지 ${index + 1} 이미 로드됨`);
						checkAllLoaded();
					} else {
						img.addEventListener('load', () => {
							console.log(`[인쇄] 이미지 ${index + 1} 로드 완료`);
							checkAllLoaded();
						});
						img.addEventListener('error', () => {
							console.error(`[인쇄] 이미지 ${index + 1} 로드 실패:`, img.src);
							checkAllLoaded(); // 실패해도 카운트
						});
					}
				});
			});
		});
	}
});

function getBase64Image(imgEl) {
	return new Promise((resolve) => {
		// 이미 blob: URL이 아니면 그대로 반환 (불필요한 변환 방지)
		if (!imgEl.src.startsWith('blob:')) {
			resolve(imgEl.src);
			return;
		}
		const canvas = document.createElement('canvas');
		const ctx = canvas.getContext('2d');

		const img = new Image();
		img.onload = function () {
			canvas.width = img.naturalWidth;
			canvas.height = img.naturalHeight;
			ctx.drawImage(img, 0, 0);

			// toDataURL로 Base64 문자열 생성 (이미지 형식 지정)
			const dataURL = canvas.toDataURL('image/png');
			resolve(dataURL);
		};
		img.onerror = function () {
			console.error("Image loading failed for Base64 conversion.");
			resolve(''); // 로드 실패 시 빈 문자열 반환
		};
		// 중요: 로컬 파일이라도 crossOrigin 설정 권장
		img.crossOrigin = 'Anonymous';
		img.src = imgEl.src;
	});
}

// 서버에서 이미지 파일을 가져와 DataURL로 변환 (인증 포함 가능)
async function fetchImageAsDataURL(fileId) {
	try {
		const resp = await fetch(apiUrl(`files/download/${fileId}`), { credentials: 'include' });
		if (!resp.ok) throw new Error(`이미지 응답 상태: ${resp.status}`);
		const blob = await resp.blob();
		return await new Promise((resolve, reject) => {
			const reader = new FileReader();
			reader.onloadend = () => resolve(reader.result);
			reader.onerror = reject;
			reader.readAsDataURL(blob);
		});
	} catch (e) {
		console.error('fetchImageAsDataURL error', e);
		return null;
	}
}

// 파일 링크 생성 헬퍼 함수 downloadArea영역에생성되는 a태그
// 이미지 파일 확인
const isImageFile = (fileName) => /\.(jpg|jpeg|png|gif|webp|bmp)$/i.test(fileName);

// 파일 항목 생성 (인쇄 모드에서만 이미지 표시)
const createFileLink = (fileId, fileName, isPrintMode = false, srcOverride = null) => {
	const container = document.createElement('div');
	container.style.margin = '10px 0';
	container.style.padding = '8px';
	if (!isPrintMode) {
		container.style.border = '1px solid #ddd';
	}
	container.style.borderRadius = '4px';
	container.style.backgroundColor = '#f9f9f9';

	if (isPrintMode && isImageFile(fileName)) {
		// 인쇄 모드: 이미지 미리보기
		const img = document.createElement('img');
		img.src = srcOverride || `/files/download/${fileId}`;
		img.alt = fileName;
		Object.assign(img.style, {
			maxWidth: '100%',
			maxHeight: '300px',
			display: 'block',
			marginBottom: '8px',
			borderRadius: '4px'
		});

		const fileName_span = document.createElement('span');
		fileName_span.textContent = `📄 ${fileName}`;
		Object.assign(fileName_span.style, {
			display: 'block',
			fontSize: '12px',
			color: '#666',
			paddingRight: '5px',
			wordBreak: 'break-all'
		});

		container.appendChild(img);
		container.appendChild(fileName_span);
	} else {
		// 일반 모드: 다운로드 링크만
		const link = document.createElement('a');
		link.href = `/files/download/${fileId}`;
		link.download = fileName;
		link.textContent = `📎 ${fileName}`;
		Object.assign(link.style, {
			display: 'block',
			color: '#007bff',
			textDecoration: 'none',
			cursor: 'pointer'
		});
		container.appendChild(link);
	}

	return container;
};

// 결재 문서 첨부파일 로드 및 렌더링
async function loadAndRenderFiles(docId) {
	const container = document.getElementById('downloadArea');
	if (!container) return console.error('다운로드 영역을 찾을 수 없습니다.');

	container.innerHTML = '파일 목록을 불러오는 중...';

	try {
		const response = await fetch(`/approval/file/${docId}`);
		if (!response.ok) throw new Error(`상태: ${response.status}`);

		const files = await response.json();
		container.innerHTML = '';

		if (!files.length) {
			container.textContent = '첨부된 파일이 없습니다.';
			return;
		}

		files.forEach(file => {
			const fileId = file.fileId;
			const fileName = file.originFileName || file.fileName;
			if (fileId && fileName) container.appendChild(createFileLink(fileId, fileName));
		});

	} catch (error) {
		console.error('첨부파일 로드 실패:', error);
		container.innerHTML = `⚠️ 파일을 불러올 수 없습니다. (${error.message})`;
	}
}

// 결재 문서 파일 데이터 로드
const getApprovalDocFileData = (approvalId) => loadAndRenderFiles(approvalId);


//f- 등록버튼,폼 결재권한자 데이터 말아서 보내는 함수
document.getElementById('modal-doc').addEventListener('submit', async function (event) {
	// 폼의 기본 제출 동작 방지
	event.preventDefault();

	// FormData 객체를 사용하여 폼 데이터 수집
	const formData = new FormData(this);

	//결재권한자가 한명이라면 또는두명이라면 순번을 앞당김
	console.log("approverArr---->",approverArr);
	//-----------------------------------------------------
	//approverArr 배열에 approverOrder가 1이나 2가 없을경우 approverOrder를 앞당기고 또는 2가 비었을경우 3을 2로 앞당겨서
	//approverArr 배열에 저장해야함
	//-----------------------------------------------------
	
	//결재문서
	if (approverArr.length != 0) { //결재권한자가 있으면
		formData.append('docStatus', '1차대기');//문서상태
		formData.append('docApprover', approverArr[0].empId);//결재권한자//1차 empId
	}

	if (approverArr.length === 0) {
		console.log("결재자 배열이 비어있습니다.");
		alert("결재 권한자 없습니다.  선택해주세요");
		return;
	}
	//결재권한자 3명까지
	if (approverArr.length > 0) {

		//결재권한자 사번,오더순서,열람여부,전결상태
		if (approverArr[0] !== undefined)
			formData.append('approverEmpIdOVD1', approverArr[0].empId + ","
				+ approverArr[0].approverOrder + "," + "Y" + "," + approverArr[0].delegateStatus); //결재권한자 아이디 3게
		if (approverArr[1] !== undefined)
			formData.append('approverEmpIdOVD2', approverArr[1].empId + ","
				+ approverArr[1].approverOrder + "," + "N" + "," + approverArr[1].delegateStatus);
		if (approverArr[2] !== undefined)
			formData.append('approverEmpIdOVD3', approverArr[2].empId + ","
				+ approverArr[2].approverOrder + "," + "N" + "," + approverArr[2].delegateStatus);
		//formData.append('approvalStatus', false);//권한자상태 필요없음
	}


	// FormData를 일반 JavaScript 객체로 변환
	//const dataObject = Object.fromEntries(formData.entries());

	await fetch(apiUrl(`approval/approval_doc`), {
		method: 'POST',
		headers: {
			[csrfHeader]: csrfToken
		},
		body: formData // 요청 본문에 JSON 데이터 포함
	})
		.then(response => {
			// 1. 응답이 성공적인지 확인
			if (!response.ok) {	
					throw new Error('서버 응답이 실패했습니다. 상태 코드: ' + response.status);	
			}else{
				// 모달 닫기
				const modalElement = document.getElementById('approval-modal');
				const modalInstance = bootstrap.Modal.getInstance(modalElement);
				modalInstance.hide();
				const myTabBtn = document.querySelector('button[data-bs-target="#navs-my-tab"]');
				const tab = bootstrap.Tab.getOrCreateInstance(myTabBtn);
				tab.show();
			}
			// 2. 응답 본문을 JSON으로 파싱
			return response.json(); 
		})
		.then(result => {	
    		// 3. 서버에서 보낸 JSON 객체의 'message' 필드에 접근
			const successMessage = result.message;	 
		
			// 4. 받아온 메시지 사용 (예: 사용자에게 알림)
			if (successMessage) {
				alert(successMessage); 
			
			} else {
				console.error("서버 응답에 message 필드가 없습니다.");
			}})
});

//f- 결재권한자변경(전결자) 라디오버튼에 관련된 함수
document.addEventListener("change", function (event) {

	if (!event.target.matches('input[name="radioJeongyeolja"]')) return;

	const selectBoxElement = document.getElementById('delegetedApprover');
	const selectedEmpName = selectBoxElement.options[selectBoxElement.selectedIndex].text;
	const targetDiv = document.getElementById(`approver_${elemApproverIdNum}`);
	const selectedValue = event.target.value;
	console.log("선택된 이름", selectedEmpName);
	console.log("선택된 전결 상태:", selectedValue);
	console.log("클릭될때", event.target.clicked);
	//console.log("targetDiv");
	if (selectedValue === 'N') {// 결재권한자변경상태가 없음일때

		approverArr.forEach(approver => {

			if (targetDiv) {
				targetDiv.querySelectorAll('span').forEach(span => span.remove());
			}
			approver.delegateStatus = 'N';
			approver.empId = approver.originalEmpId;  // 원래 사번 복구

			console.log(`결재권한자 ${approver.approverOrder} delegateStatus = N`);
		});
		document.getElementById('delegetedApprover').style.display = "none";
		document.getElementById(`approvalBtn_${elemApproverIdNum}`).style.display = "none";
	} else {
		document.getElementById('delegetedApprover').style.display = "block";
		document.getElementById(`approvalBtn_${elemApproverIdNum}`).style.display = "block";
	}
});

//f- 결재권한자 변경/전결 적용 함수
function applyDelegateChange(button) {

	console.log("적용 버튼이 클릭되었습니다.");
	const count = Number(button.dataset.count); // 버튼 자체의 data-count 사용
	console.log();

	//전결에 필요한 로직추가 approverArr 배열에 delegateStatus 값 변경
	// 라디오 버튼값 가져오기
	const radioJeongyeolja = document.getElementsByName('radioJeongyeolja');
	const targetDiv = document.getElementById(`approver_${elemApproverIdNum}`);

	let selectedValue;
	for (const radio of radioJeongyeolja) {
		// console.log("radio value:", radio.value, "checked:", radio.checked);
		if (radio.checked) {
			selectedValue = radio.value;
			break;
		}
	}

	//alert(count + "번 결재권한자를 전결자로 지정\n부모 div id: " + id);
	// console.log("적용 버튼 클릭 div id:", id);
	// console.log("적용 버튼 클릭 div count:", count);
	// console.log("선택된 전결 상태:", selectedValue);

	// console.log("이전의 approverArr:", approverArr);

	// toastui selectbox에서 선택된 사번 가져오기#select-box
	const selectBoxElement = document.getElementById('delegetedApprover');
	const selectedEmpId = selectBoxElement.value;//선택된 사번
	const selectedEmpName = selectBoxElement.options[selectBoxElement.selectedIndex].text;

	if (selectedValue == 'N') {

		approverArr.forEach(approver => {

			if (targetDiv) {
				targetDiv.querySelectorAll('span').forEach(span => span.remove());
			}
			approver.delegateStatus = 'N';
			approver.empId = approver.originalEmpId;  // 원래 사번 복구

			console.log(`결재권한자 ${approver.approverOrder} delegateStatus = N`);
		});
	}
	if (targetDiv) {
		// 새로운 전결자 표시
		if (selectedValue != 'N') {
			targetDiv.querySelectorAll('span').forEach(span => span.remove());
			targetDiv.innerHTML += `<span style="color:blue;"> ${selectedValue} <br> ${selectedEmpName} </span>`;
		}
	}
	approverArr.forEach((value, key) => {
		console.log("비교 중인 approverOrder:", value.approverOrder, "==", count);
		if (value.approverOrder === count && selectedValue != 'N') {
			// 선택된 전결 상태에 따라 delegateStatus 값 설정
			value.empId = selectedEmpId;//셀렉트 박스 값을 가져와서 넣어야함
			value.delegateStatus = selectedValue;//전결상태 변경

			console.log("매핑된 결재권한자:", value);
			console.log(`결재권한자 순서 ${count}의 전결상태가 ${selectedValue}로 변경되었습니다.`);

		}
	});
	console.log("Updated approverArr:", approverArr);
}

const Grid = tui.Grid;

// g- 결재사항
const grid1 = new Grid({
	el: document.getElementById('approvalGrid'),
	rowHeaders: ['rowNum'],
	columns: [

		{ header: '순번', name: 'row_no', align: 'center', hidden: true }
		, { header: '문서id', name: 'approval_id', align: 'center', hidden: true }
		, { header: '문서제목', name: 'approval_title', align: 'center', width: 370 }
		, { header: '양식', name: 'form_type', align: 'center', width: 136, filter: "select" }
		, { header: '사원번호', name: 'emp_id', align: 'center' }
		, { header: '기안자', name: 'emp_name', align: 'center', filter: "select"}
		, { header: '직급코드', name: 'pos_code', align: 'center', hidden: true }
		, { header: '직급', name: 'pos_name', align: 'center', width: 51 }
		, { header: '부서코드', name: 'dept_id', align: 'center', hidden: true }
		, { header: '부서명', name: 'dept_name', align: 'center' }
		, { header: '결재권한자id', name: 'approver', align: 'center', hidden: true }
		, { header: '결재권한자', name: 'approver_name', align: 'center' }
		, { header: '생성일', name: 'created_date', align: 'center' }
		, { header: '결재완료일자', name: 'finish_date', align: 'center' , width: 110, sortable: true
		}//상태가 대기이면서 3일전 날짜일때만 상태에 시계아이콘
		, { header: '휴가시작일자', name: 'start_date', align: 'center', hidden: true }
		, { header: '휴가종료일자', name: 'end_date', align: 'center', hidden: true }
		, { header: '연차유형', name: 'leave_type', align: 'center', hidden: true }
		, { header: '변경직급', name: 'to_pos_code', align: 'center', hidden: true }
		, { header: '발령부서', name: 'to_dept_id', align: 'center', hidden: true }
		, { header: '지출종류', name: 'expnd_type', align: 'center', hidden: true }
		, { header: '결재사유내용', name: 'reason', align: 'center', hidden: true }
		, { header: '상태', name: 'doc_status', align: 'center', filter: "select"
			,renderer:{ type: StatusBadgeRenderer}
		}
		, {
			header: '상세보기', name: 'view_details', align: 'center', width: 100
			, formatter: (rowInfo) => {
				return `<button type='button' class='btn btn-primary btn-sm' data-row-key='${rowInfo.row.rowKey}'>상세</button>`;
			}
		}
	],
	data: []
	, bodyHeight: 500 // 그리드 본문의 높이를 픽셀 단위로 지정. 스크롤이 생김.
	, height: 100
	, columnOptions: {
		resizable: true
	}
	, pageOptions: {
		useClient: true,
		perPage: 10
	}
});


// g- 전체결재
const grid2 = new Grid({
	el: document.getElementById('allApprovalGrid'), // 전체결재
	rowHeaders: ['rowNum'],
	columns: [

		{ header: '순번', name: 'row_no', align: 'center', hidden: true }
		, { header: '문서id', name: 'approval_id', align: 'center', hidden: true }
		, { header: '문서제목', name: 'approval_title', align: 'center', width: 370 }
		, { header: '양식', name: 'form_type', align: 'center', width: 136, filter: "select" }
		, { header: '사원번호', name: 'emp_id', align: 'center' }
		, { header: '기안자', name: 'emp_name', align: 'center' ,filter: "select"}
		, { header: '직급코드', name: 'pos_code', align: 'center', hidden: true }
		, { header: '직급', name: 'pos_name', align: 'center', width: 51 }
		, { header: '부서코드', name: 'dept_id', align: 'center', hidden: true }
		, { header: '부서명', name: 'dept_name', align: 'center' }
		, { header: '결재권한자id', name: 'approver', align: 'center', hidden: true }
		, { header: '결재권한자', name: 'approver_name', align: 'center' }
		, { header: '생성일', name: 'created_date', align: 'center' }
		, { header: '결재완료일자', name: 'finish_date', align: 'center' , width: 110,sortable: true,}
		, { header: '휴가시작일자', name: 'start_date', align: 'center', hidden: true }
		, { header: '휴가종료일자', name: 'end_date', align: 'center', hidden: true }
		, { header: '연차유형', name: 'leave_type', align: 'center', hidden: true }
		, { header: '변경직급', name: 'to_pos_code', align: 'center', hidden: true }
		, { header: '발령부서', name: 'to_dept_id', align: 'center', hidden: true }
		, { header: '지출종류', name: 'expnd_type', align: 'center', hidden: true }
		, { header: '결재사유내용', name: 'reason', align: 'center', hidden: true }
		, { header: '상태', name: 'doc_status', align: 'center' ,filter: "select"
			,renderer:{ type: StatusBadgeRenderer}
		}
		, {
			header: '상세보기', name: 'view_details', align: 'center'
			, formatter: (rowInfo) => {
				return `<button type='button' class='btn btn-primary btn-sm' data-row-key='${rowInfo.row.rowKey}'>상세</button>`;
			}
		}
	],
	data: []
	, bodyHeight: 500
	, columnOptions: {
		resizable: true
	}
	, pageOptions: {
		useClient: true,
		perPage: 10
	}
});
//g- 내결재목록
const grid3 = new Grid({
	el: document.getElementById('myApprovalGrid'), // 내 결재목록
	rowHeaders: ['rowNum'],
	columns: [

		{ header: '순번', name: 'row_no', align: 'center', hidden: true }
		, { header: '문서id', name: 'approval_id', align: 'center', hidden: true }
		, { header: '문서제목', name: 'approval_title', align: 'center', width: 370 }
		, { header: '양식', name: 'form_type', align: 'center', width: 136, filter: "select" }
		, { header: '사원번호', name: 'emp_id', align: 'center' }
		, { header: '기안자', name: 'emp_name', align: 'center' ,filter: "select" }
		, { header: '직급코드', name: 'pos_code', align: 'center', hidden: true }
		, { header: '직급', name: 'pos_name', align: 'center', width: 51 }
		, { header: '부서코드', name: 'dept_id', align: 'center', hidden: true }
		, { header: '부서명', name: 'dept_name', align: 'center' }
		, { header: '결재권한자id', name: 'approver', align: 'center', hidden: true }
		, { header: '결재권한자', name: 'approver_name', align: 'center' }
		, { header: '생성일', name: 'created_date', align: 'center' }
		, { header: '결재완료일자', name: 'finish_date', align: 'center' , width: 110,sortable: true,}
		, { header: '휴가시작일자', name: 'start_date', align: 'center', hidden: true }
		, { header: '휴가종료일자', name: 'end_date', align: 'center', hidden: true }
		, { header: '연차유형', name: 'leave_type', align: 'center', hidden: true }
		, { header: '변경직급', name: 'to_pos_code', align: 'center', hidden: true }
		, { header: '발령부서', name: 'to_dept_id', align: 'center', hidden: true }
		, { header: '지출종류', name: 'expnd_type', align: 'center', hidden: true }
		, { header: '결재사유내용', name: 'reason', align: 'center', hidden: true }
		, { header: '상태', name: 'doc_status', align: 'center' ,filter: "select"
			,renderer:{ type: StatusBadgeRenderer}
		}
		, {
			header: '상세보기', name: 'view_details', align: 'center'
			, formatter: function (rowInfo) {
				return `<button type='button' class='btn btn-primary btn-sm' data-row-key='${rowInfo.row.rowKey}'>상세</button>`;
			}
		}
	],
	data: []
	, bodyHeight: 500
	, columnOptions: {
		resizable: true
	}
	, pageOptions: {
		useClient: true,
		perPage: 10
	}
});
//g- 결재대기
const grid4 = new Grid({
	el: document.getElementById('waitingApprovalGrid'), //결재대기
	rowHeaders: ['rowNum'],
	columns: [

		{ header: '순번', name: 'row_no', align: 'center', hidden: true }
		, { header: '문서id', name: 'approval_id', align: 'center', hidden: true }
		, { header: '문서제목', name: 'approval_title', align: 'center', width: 370 }
		, { header: '양식', name: 'form_type', align: 'center', width: 136, filter: "select" }
		, { header: '사원번호', name: 'emp_id', align: 'center' }
		, { header: '기안자', name: 'emp_name', align: 'center' ,filter: "select"}
		, { header: '직급코드', name: 'pos_code', align: 'center', hidden: true }
		, { header: '직급', name: 'pos_name', align: 'center', width: 51 }
		, { header: '부서코드', name: 'dept_id', align: 'center', hidden: true }
		, { header: '부서명', name: 'dept_name', align: 'center' }
		, { header: '결재권한자id', name: 'approver', align: 'center', hidden: true }
		, { header: '결재권한자', name: 'approver_name', align: 'center' }
		, { header: '생성일', name: 'created_date', align: 'center' }
		, { header: '결재완료일자', name: 'finish_date', align: 'center' , width: 110,sortable: true}
		, { header: '휴가시작일자', name: 'start_date', align: 'center', hidden: true }
		, { header: '휴가종료일자', name: 'end_date', align: 'center', hidden: true }
		, { header: '연차유형', name: 'leave_type', align: 'center', hidden: true }
		, { header: '변경직급', name: 'to_pos_code', align: 'center', hidden: true }
		, { header: '발령부서', name: 'to_dept_id', align: 'center', hidden: true }
		, { header: '지출종류', name: 'expnd_type', align: 'center', hidden: true }
		, { header: '결재사유내용', name: 'reason', align: 'center', hidden: true }
		, { header: '상태', name: 'doc_status', align: 'center' ,filter: "select"
			,renderer:{ type: StatusBadgeRenderer}
		}
		, {
			header: '상세보기', name: 'view_details', align: 'center'
			, formatter: function (rowInfo) {
				return `<button type='button' class='btn btn-primary btn-sm' data-row-key='${rowInfo.row.rowKey}'>상세</button>`;
			}
		}
	],
	data: []
	, bodyHeight: 500
	, columnOptions: {
		resizable: true
	}
	, pageOptions: {
		useClient: true,
		perPage: 10
	}
});
//g- 결재완료
const grid5 = new Grid({
	el: document.getElementById('doneApprovalGrid'), //결재완료
	rowHeaders: ['rowNum'],
	columns: [

		{ header: '순번', name: 'row_no', align: 'center', hidden: true }
		, { header: '문서id', name: 'approval_id', align: 'center', hidden: true }
		, { header: '문서제목', name: 'approval_title', align: 'center', width: 370 }
		, { header: '양식', name: 'form_type', align: 'center', width: 136, filter: "select" }
		, { header: '사원번호', name: 'emp_id', align: 'center' }
		, { header: '기안자', name: 'emp_name', align: 'center' }
		, { header: '직급코드', name: 'pos_code', align: 'center', hidden: true }
		, { header: '직급', name: 'pos_name', align: 'center', width: 51 }
		, { header: '부서코드', name: 'dept_id', align: 'center', hidden: true }
		, { header: '부서명', name: 'dept_name', align: 'center' }
		, { header: '결재권한자id', name: 'approver', align: 'center', hidden: true }
		, { header: '결재권한자', name: 'approver_name', align: 'center' }
		, { header: '생성일', name: 'created_date', align: 'center' }
		, { header: '결재완료일자', name: 'finish_date', align: 'center' , width: 110,sortable: true}
		, { header: '휴가시작일자', name: 'start_date', align: 'center', hidden: true }
		, { header: '휴가종료일자', name: 'end_date', align: 'center', hidden: true }
		, { header: '연차유형', name: 'leave_type', align: 'center', hidden: true }
		, { header: '변경직급', name: 'to_pos_code', align: 'center', hidden: true }
		, { header: '발령부서', name: 'to_dept_id', align: 'center', hidden: true }
		, { header: '지출종류', name: 'expnd_type', align: 'center', hidden: true }
		, { header: '결재사유내용', name: 'reason', align: 'center', hidden: true }
		, { header: '상태', name: 'doc_status', align: 'center' , filter: "select"
			,renderer:{ type: StatusBadgeRenderer}
		}
		, {
			header: '상세보기', name: 'view_details', align: 'center'
			, formatter: function (rowInfo) {
				return `<button type='button' class='btn btn-primary btn-sm' data-row-key='${rowInfo.row.rowKey}'>상세</button>`;
			}
		}
	],
	data: []
	, bodyHeight: 500
	, columnOptions: {
		resizable: true
	}
	, pageOptions: {
		useClient: true,
		perPage: 10
	}
});


// -----------------------------------------
// 탭클릭시 그리드를 다시 그려주는 로직
// -----------------------------------------
document.querySelectorAll('button[data-bs-toggle="tab"]').forEach(tab => {
	tab.addEventListener('shown.bs.tab', function (e) {
		const targetId = e.target.getAttribute('data-bs-target');

		if (targetId === '#nav-approval-tab') {//결재사항
			grid1.refreshLayout();
		} else if (targetId === '#navs-all-tab') {//전체결재
			grid2.refreshLayout();
		} else if (targetId === '#navs-my-tab') {//내결재목록
			grid3.refreshLayout();
		} else if (targetId === '#navs-waiting-tab') {//결재대기
			grid4.refreshLayout();
		} else if (targetId === '#navs-done-tab') {//결재완료
			grid5.refreshLayout();
		}
		AllGridSearch();
	});
});
// -----------------------------------------
// -----------------------------------------
// -----------------------------------------


Grid.applyTheme('clean'); // Call API of static method
//f- 날짜,기안자,문서양식 조회 불러오는 함수
function AllGridSearch() {
	const params = {

		createDate: document.getElementById("searchStartDate").value ?? "",
		finishDate: document.getElementById("searchEndDate").value ?? "",
		empName: document.getElementById("searchEmpIdAndformType").value ?? "",
		approvalTitle: document.getElementById("searchEmpIdAndformType").value ?? ""
	};

	fetch(apiUrl(`approval/searchAllGrids`), {
		method: 'POST',
		headers: {
			[csrfHeader]: csrfToken,
			'Content-Type': 'application/json'
		},
		body: JSON.stringify(params)
	})
		.then(res => {
			if (!res.ok) {
				throw new Error(`HTTP error! status: ${res.status}`);
			}
			return res.json();
		})
		.then(data => {
			grid1.resetData(data.grid1Data);
			grid2.resetData(data.grid2Data);
			grid3.resetData(data.grid3Data);
			grid4.resetData(data.grid4Data);
			grid5.resetData(data.grid5Data);
			console.log("검색데이터:", data);
		})
		.catch(err => {
			console.error("조회오류", err);
			grid1.resetData([]);
			grid2.resetData([]);
			grid3.resetData([]);
			grid4.resetData([]);
			grid5.resetData([]);
		});
	console.log("params:", params);

}
const searchBtn = document.getElementById("searchBtn");
if (searchBtn) {
	searchBtn.addEventListener("click", (ev) => {

	});

}

// 서버에서 받아온 default 결재권자 담을 변수
let formList = [];
// 선택한 양식을 담을 변수
let selectedForm = null;

// f- default 결재권자 가져오는 함수
async function defalutapprover() {
	const res = await fetch("/api/approvals/defaultApprover", { method: "GET" });

	if (!res.ok) {
		throw new Error("데이터 로드 실패!");
	}

	formList = await res.json();
}

//모달창 코드
//f- 기안서 셀렉트 박스 변경시 모달창에 텍스트 변경함수
function draftValFn(ev) {
	let draft_doc = ev.value;

	document.getElementById('saveBtn').style.display = "block";//등록
	document.getElementById('attachmentBtn').style.display = 'block';//첨부파일
	document.getElementById('downloadArea').style.display = "none";//다운로드
	//document.getElementById('DraftingHidden').value = draft_doc;

	// html에서 th:data-formname="${item.formName}" 값을 가져와서 이름으로 사용
	const formName = ev.selectedOptions[0].dataset.formname;
	console.log("ev.selectedOptions[0].dataset.formname", ev.selectedOptions[0].dataset.formname);

	document.getElementById('DraftingHidden').value = formName;
	document.getElementById('Drafting').innerText = formName;
	// 선택한 결재 양식과 서버에서 받아온 데이터 중 일치하는 값 찾기
	selectedForm = formList.find(item => item.formName === draft_doc);
	console.log("draft_doc", draft_doc);
	//양식종류에따라 보여지는 화면이 다름
	document.getElementById('approvalCompanionBtn').style.display = "none";//반려
	document.getElementById('approvalCheckBtn').style.display = "none";//결재확인	
	formChange(draft_doc);

	formReset();
	defaultPrint();
}

//f- 양식 모달 리셋함수
function formReset(ev) {

	// Null 체크 추가
	const draftingElement = document.getElementById('Drafting');
	if (draftingElement) { // draftingElement가 null인지 체크
		draftingElement.innerText = selectedForm.formName;
	}

	// Null 체크 추가
	const draftingHiddenElement = document.getElementById("DraftingHidden");
	if (draftingHiddenElement) { // draftingHiddenElement가 null인지 체크
		draftingHiddenElement.value = selectedForm.formName; // 양식종류 숨은값
	}

	//document.getElementById("DraftingHidden").value = selectedForm.formName;//양식종류 숨은값
	//document.getElementById('Drafting').innerText = selectedForm.formName;
	//document.getElementById("DraftingHidden").value = selectedForm.formName;//양식종류 숨은값

	document.getElementById("approval-title").value = "";//문서제목
	//document.getElementById("approver-name").value ="";//결재자명 - 로그인정보에서 불러옴
	document.getElementById("create-date").value = null;//문서 생성일자
	document.getElementById("finish-date").value = null;//결재완료기간
	document.getElementById("start-date").value = null;//휴가신청서 시작날짜
	document.getElementById("end-date").value = null;//휴가신청서 종료날짜
	document.getElementById("leave-type").selectedIndex = 0;//휴가종류
	document.getElementById("to-dept-id").selectedIndex = 0;//발령부서
	document.getElementById("expnd-type").selectedIndex = 0;//지출종류
	document.getElementById("reason-write").value = "";//사유내용
	
	// 사유내용 글자 수 카운터 초기화
	const charCount = document.getElementById('reason-char-count');
	if (charCount) {
		charCount.textContent = '0/3000자';
		charCount.style.color = '#6c757d'; // 기본 색상으로 복원
	}
	//selectBox.resetItems();
	//selectBox.setItems(itemData);

	//const originalSelect = document.getElementById('select-box');
	//originalSelect.value = '';
	//selectBox.select(null);
}

let today = new Date();
let year = today.getFullYear(); // 년도
let month = today.getMonth() + 1;  // 월
let date = today.getDate();  // 날짜
let day = today.getDay();  // 요일

const formattedDate = `${year}년 ${month}월 ${date}일`;
document.getElementById("today-date").textContent = formattedDate;


let jeongyeoljaDiv = document.querySelector('#jeongyeolja');
let jeongyeoljaContent = document.querySelector("#jeongyeolja-content");
let approverDivClose = document.getElementById("approverDiv-close");

this.count = 0; //결재권한자 label count
let defalutapproverArr = ["d-이사랑", "d-미미미누", "d-김경란"];
let approverArr = [];//결재권한자 배열 
let writeBtn = document.getElementById("writeBtn");

//모달이 닫힐떄 첨부파일 리셋
const approvalModal = document.getElementById('approval-modal');
approvalModal.addEventListener('hidden.bs.modal', function (event) {
	resetAttachments();
	// modal이 닫힐 때 grid에서 연 플래그 초기화
	modalOpenedFromGrid = false;
});

//f- 기안서작성 모달이 열리기전에 이벤트를 감지
$('#approval-modal').on('show.bs.modal', function (e) {
	// e.relatedTarget이 null/undefined이면 .dataset 접근을 멈추고 actionType에 undefined 할당
	let actionType = e.relatedTarget?.dataset?.action;

	// actionType이 유효할 때만 로직을 실행합니다.
	if (!selectedForm && actionType === 'create') {
		e.preventDefault();
		alert("양식을 선택해주세요.");
	} else {
		console.log(" 모달 열기 진행");
	}
});

//f- 작성 버튼 클릭 시 실행되는 함수
function defaultPrint() {
	// 모달을 닫고 다시 작성 버튼을 클릭하면 이전 데이터가 남아있어서 초기화 진행
	approverDiv.innerHTML = "";
		formReset();
		formEnable();
		if (selectBox && typeof selectBox.enable === 'function') selectBox.enable();
	window.count = 0;
	approverArr = [];//form-type-defalut
	document.getElementById('form-menu').value = document.getElementById('form-type-defalut').value;//양식종류

	if(document.getElementById('form-type-defalut').value === '인사발령신청서'){
		document.getElementById('approval-title').placeholder = '[승진 또는 전보]만 가능합니다, 제목을입력해주세요!';
	}

	// 작성 모드용 버튼 설정
	document.getElementById('saveBtn').style.display = "block";//등록 버튼 보이기
	document.getElementById('attachmentBtn').style.display = 'block';//첨부파일 버튼 보이기
	document.getElementById('downloadArea').style.display = "none";//다운로드 영역 숨기기
	document.getElementById('approvalCompanionBtn').style.display = "none";//반려 버튼 숨기기
	document.getElementById('approvalCheckBtn').style.display = "none";//결재확인 버튼 숨기기
	
	// 오늘 날짜를 create-date, finish-date 필드에 기본값으로 설정
	const today = new Date();
	const isoFormattedDate = today.toISOString().split('T')[0]; // YYYY-MM-DD 형식
	
	// today-date 요소에 오늘 날짜 설정 (읽기 전용 표시용)
	const todayDateElement = document.getElementById('today-date');
	if (todayDateElement) {
		todayDateElement.textContent = isoFormattedDate;
		console.log('defaultPrint - today-date 설정됨:', isoFormattedDate);
	}
	
	const createDateInput = document.getElementById('create-date');
	if (createDateInput) {
		createDateInput.value = isoFormattedDate;
		console.log('defaultPrint - create-date 설정됨:', isoFormattedDate);
	}
	// selectedForm 값이 없을 경우 에러가 생길 수 있어서 에러 처리
	//<option selected>기안서</option> 해당구문 없앨시에 마지막인덱스로됨
	if (!selectedForm) {
		console.log('모달을 열 수 없습니다.');
		return;
	}

	defalutapproverArr = []; //디폴트 결재권한자 초기화
	for (let i = 1; i <= 3; i++) {
		// selectedForm의 approver1, approver2, approver3을 가져오기 위해서 템플릿 문자열 사용
		const approver = selectedForm[`approver${i}`] + " " + selectedForm[`approver${i}Name`];

		// 결재권자가 없으면 화면에 출력되지 않도록 처리
		if (selectedForm[`approver${i}`] == null) {
			break;
		}

		if (approver) {//디폴트 결재권한자 라벨이 null이 아닐때
			defalutapproverArr.push(approver);
			console.log("defalutapproverArr", defalutapproverArr);
		}

		console.log("추출된 기본 결재자:", defalutapproverArr);

	}
	// 4. 기본 결재 라인 설정 (this.count가 0일 때만 실행)
	// 이 로직은 결재 라인에 아무도 없을 때만 기본값을 넣어주기 위한 로직입니다.
	if (window.count === 0) {

		defalutapproverArr.forEach(approver => {

			const approverParts = approver.split(" ");
			const approverEmpId = approverParts[0];
			print("defalut", approver);

			approverArr.push({
				empId: approverEmpId,
				approverOrder: window.count,
				delegateStatus: 'N',
				originalEmpId: approverEmpId
			});
		});
		console.log("approverArr 실행 후:", approverArr);
	}

}

defalutapprover();

// 화살표를 동적으로 관리하는 함수
function updateArrows() {
	// 기존 화살표들 모두 제거
	const existingArrows = approverDiv.querySelectorAll('.bi-caret-right-fill');
	existingArrows.forEach(arrow => arrow.remove());
	
	// 카드 개수 확인 (btn 클래스를 가진 div들)
	const cards = approverDiv.querySelectorAll('.btn');
	
	// 카드가 2개 이상일 때만 화살표 추가
	if (cards.length >= 2) {
		for (let i = 0; i < cards.length - 1; i++) {
			const arrow = document.createElement('i');
			arrow.className = 'bi bi-caret-right-fill';
			arrow.style.marginTop = '95px';
			
			// 현재 카드 다음에 화살표 삽입
			cards[i].insertAdjacentElement('afterend', arrow);
		}
	}
}

//f- 결재권한자 div 버튼 생성 함수
function print(type, text) {

	if (this.count < 3) {
		const idx = this.count + 1;
		
		// type이 "detail"일 때는 닫기 버튼을 표시하지 않음 (상세 보기용)
		const closeButton = type === "detail" ? '' : '<a id="approver_close_' + idx + '" onclick="approverDivclose(this,' + "'" + type + "'" + ',' + idx + ')" style="float:right;margin-right: 8px;">&times;</a>';
		
		// const cardHtml = '<div class="btn"'
		// 	+ 'style="background-color: #E7E7FF;width:250px;height:200px; margin:5px; padding: 5px 0px 0px 0px;">'
		// 	+ closeButton
		// 	+ '<p id="approver_' + idx + '" onclick="approvalNo(' + idx + ',' + "'" + text + "'" + ')" style="margin-top:30px;height: 129px;font-size:22px;">' + idx + '차 결재권한자 ' + '<br>' + text + '<br>' + '</p>'
		// 	+ '</div>';

		const cardHtml = '<div class="btn" '
            + 'style="background-color: #E7E7FF; width:250px; height:200px; margin:5px; padding: 5px 0px 0px 0px; '
            + 'border-radius: 10px; border: 1px solid #D1D1F5; cursor: pointer; ' // 기본 테두리
            + 'box-shadow: 0 4px 6px rgba(0,0,0,0.1); transition: transform 0.1s, box-shadow 0.1s; display: inline-block; vertical-align: top;" ' // 입체감 추가
            + 'onmouseover="this.style.backgroundColor=\'#DADAFF\';" ' // 마우스 올리면 살짝 진해짐
            + 'onmouseout="this.style.backgroundColor=\'#E7E7FF\';" '
            + 'onmousedown="this.style.transform=\'translateY(2px)\'; this.style.boxShadow=\'0 2px 3px rgba(0,0,0,0.15)\';" ' // 클릭 시 아래로 눌림
            + 'onmouseup="this.style.transform=\'translateY(0px)\'; this.style.boxShadow=\'0 4px 6px rgba(0,0,0,0.1)\';">' // 떼면 복귀
            + closeButton
            + '<p id="approver_' + idx + '" onclick="approvalNo(' + idx + ',' + "'" + text + "'" + ')" '
            + 'style="margin-top:30px; height: 129px; font-size:22px;">' // 기존 시퀀스(마진, 높이) 유지
            + idx + '차 결재권한자 ' + '<br>' + text + '<br>' 
            + '</p>'
            + '</div>';

		approverDiv.innerHTML += cardHtml;
		this.count = idx;
		
		// 카드 추가 후 화살표 업데이트
		updateArrows();
	}
}


//f- 결재권한자 버튼 클릭시 결재권한자변경 div 태그 생성//전결자
function approvalNo(count, text) {
	// 그리드에서 모달을 열었을 때는 approvalNo 동작을 무시합니다.
	if (modalOpenedFromGrid) return;
	
	// 상세보기 상태인지 확인 (reason-write가 disabled된 경우)
	const reasonTextarea = document.getElementById('reason-write');
	if (reasonTextarea && reasonTextarea.disabled) {
		// 상세보기 모드에서는 열려있는 결재권한자 변경 div를 닫음
		if (jeongyeoljaDiv && jeongyeoljaDiv.style.display !== 'none') {
			jeongyeoljaDiv.style.display = 'none';
		}
		return;
	}
	
	elemApproverIdNum = count;
	let type = "change";
	if (jeongyeoljaDiv) {
		// div 초기화
		jeongyeoljaDiv.innerHTML = `
	            <button type="button" onClick="approverDivclose(this, '${type}', ${count})" class="btn-close" style="float:right;margin-right: 8px;"></button>
	            <h5>${count}차 결재권한자 : ${text} 변경</h5>
	            ${jeongyeoljaContent.innerHTML}
	            <button id="approvalBtn_${count}" 
	                    type="button" class="btn btn-primary" 
	                    data-count="${count}" 
	                    onclick="applyDelegateChange(this)"
						style="display:none;margin-left:5px;background-color: #788393; color:#e9ecef;">
	                결재권한자로 지정
	            </button>
	        `;
		jeongyeoljaDiv.style.display = 'block';
	}
}
//f- 결재권한자,결재권한자변경(전결자) 닫기버튼
function approverDivclose(buttonDiv, type, count) {
	const divElement = buttonDiv.parentNode; // 버튼의 부모인 div를 찾음
	console.log("type", type);

	const approverCard = buttonDiv.closest('.btn-success');

	if (approverCard) {
		// 화살표 아이콘 (다음 형제 요소) 찾기
		// 삭제할 카드의 바로 다음에 있는 요소(화살표 <i> 태그)를 찾습니다.
		const arrowIcon = approverCard.nextElementSibling;

		// 부모 요소 (approverDiv)를 찾습니다.
		const parentDiv = approverCard.parentElement;
		// 카드 삭제
		parentDiv.removeChild(approverCard);

		// 화살표 아이콘 삭제 (안전하게 확인 후 삭제)
		// 다음 형제 요소가 null이 아니며, 실제로 화살표 아이콘 클래스를 가지고 있을 때만 삭제합니다.
		if (arrowIcon && arrowIcon.classList.contains('bi-caret-right-fill')) {
			parentDiv.removeChild(arrowIcon);
		}

	}

	jeongyeoljaDiv.style.display = 'none';
	//defalut 태그 닫기 버튼시 
	if (buttonDiv.parentElement.id === "" || type === "defalut") {//결재권한자
		divElement.remove(); //자신의 div 제거

		if (divElement.innerText !== null) { //defalut 태그가 있을때
			approverArr = approverArr.filter((ev) => ev.approverOrder !== count);
		}
		approverArr = approverArr.filter((ev) => ev !== count);
		this.count = count - 1; //제거 라벨 카운트 원상복기
		
		// 카드 삭제 후 화살표 업데이트
		updateArrows();
	}
	if (type === "close") { //전결자 변경 닫기버튼시
		divElement.remove(); //자신의 div 제거
		//전결자 변경시 결재권한자 배열에서 해당 결재권한자 제거
		approverArr = approverArr.filter((ev) => ev.approverOrder !== count);
		
		// 카드 삭제 후 화살표 업데이트
		updateArrows();
	}
	if (approverArr.length === 0) {
		this.count = 0;
	}
}

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
