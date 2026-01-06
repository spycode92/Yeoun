// 사원 등록 화면 전용 JS (emp.js)

/**
 * 조직/직무 관련 JS
 * - 상위부서(topDept) 선택 시 하위부서(deptId) 자동 필터링
 * - 부서 선택 유지 및 복원 로직 포함
 */
let allDept = [];

function initOrgSelects() {
	
	const pageMode = document.body.dataset.mode;
	
	const topSel = document.getElementById('topDept');	// 상위부서(본부) 셀렉트
	const deptSel = document.getElementById('deptId');	// 하위부서 셀렉트
	const posSel = document.getElementById('posCode');	// 직급 셀렉트
	
	if (!topSel || !deptSel || !posSel) return;
	
	const allDept = Array.from(
		deptSel.querySelectorAll('option[data-parent]')
	);
	
	// 부서 셀렉트 초기화 함수
	function resetDept(phText, disabled = true) {
		// 기존 옵션 전부 제거
		deptSel.innerText = '';
		
		// placeholder용 option 생성
		const ph = document.createElement('option');
		ph.value = '';
		ph.textContent = phText;	// "먼저 본부를 선택하세요" 문구
		ph.disabled = true;
		ph.selected = true;
		
		deptSel.appendChild(ph);
		deptSel.disabled = disabled;	// true면 비활성화, false면 선택 가능
	}
	
	// 직급 셀렉트 관리
	function updatePosState(deptId) {
		// 직급 셀렉트에서 value=""인 옵션(placeholder)을 찾고,
		// 없다면 맨 첫 옵션을 placeholder로 간주
		const firstOpt = posSel.querySelector('option[value=""]') || posSel.options[0];
		
		if (!deptId) {
			// 부서가 선택되지 않은 상태
			if (firstOpt) firstOpt.textContent = '먼저 부서를 선택하세요';
			posSel.value = '';			// 선택값 초기화
			posSel.disabled = true;		// 직급 비활성화
		} else {
			// 부서가 정상적으로 선택된 상태
			if (firstOpt) firstOpt.textContent = '직급을 선택하세요';
			posSel.disabled = false;	// 직급 활성화
		}
	}
	
	// 선택된 본부(topId)에 따라 하위 부서 목록 필터링
	function filterDeptByTop(topId) {
		// 기존에 선택되어 있던 부서 id (필터 후 복원용)
		const prevDept = deptSel.value;
		
		// 상위부서가 선택되지 않은 경우 
		if (!topId) {
			resetDept('먼저 본부를 선택하세요', true);
			updatePosState(null);
			return
		}
		
		// 상위부서가 선택된 경우: 부서 셀렉트 초기화 후 활성화
		resetDept('부서를 선택하세요', false);
		
		// allDept 중에서 data-parent 속성이 topId인 것만 골라서
		// deptSel에 option으로 다시 추가
		allDept
			.filter(o => o.getAttribute('data-parent') === topId)
			.forEach(o => deptSel.appendChild(o.cloneNode(true)));
		
		// 필터링 후에도 예전에 선택한 부서가 새 목록에 존재하면
		// 그 값 유지 (수정 화면에서 유용)				
		if([...deptSel.options].some(o => o.value === prevDept)) {
			deptSel.value = prevDept;
			updatePosState(prevDept);
		} else {
			// 기존 선택 부서가 없어진 경우: 직급 비활성화
			updatePosState(null);
		}
	}
	
	// 페이지 처음 로드될 때, 현재 선택된 부서를 기준으로 상태 맞추기
	// (등록/수정 화면 공통 처리)
	const selectedDeptOpt = deptSel.querySelector('option:checked[value]');
	
	if (selectedDeptOpt) {
		// 선택된 부서 옵션이 있을 때
		const parentDeptId = selectedDeptOpt.getAttribute('data-parent');
		
		if (parentDeptId) {
			// 해당 부서가 어느 상위부서에 속하는지 기준으로 topDept 셀렉트 값 먼저 세팅
			topSel.value = parentDeptId;
			// 상위부서 기준으로 하위 부서 목록 생성
			filterDeptByTop(parentDeptId);
			// 그 후에 현재 부서 다시 선택
	        deptSel.value = selectedDeptOpt.value;
			// 부서가 있으므로 직급 셀렉트 활성화
	        updatePosState(selectedDeptOpt.value);
		} else {
			// 부모 정보가 없으면, 현재 상위부서 값 기준으로 필터링
			filterDeptByTop(topSel.value);
			updatePosState(deptSel.value);
		}
	} else {
		// 선택된 부서가 없는 경우 (신규 등록 화면)
		filterDeptByTop(topSel.value);	// 상위부서가 기본 선택되어 있다면 그걸로 필터링
		updatePosState(deptSel.value);  // 부서가 없으면 직급 비활성화
	}
	
	// 수정 모드에서는 조직/직급은 보여주기만하고 수정은 막음
	if (pageMode === 'edit') {
		topSel.disabled  = true;
		deptSel.disabled = true;
		posSel.disabled  = true;
		return;
	}
	
	// 등록 모드일 때만 change 이벤트 바인딩
	// 상위부서가 바뀌면 그에 맞춰 하위부서 필터링
	topSel.addEventListener('change', e => filterDeptByTop(e.target.value));
	// 부서가 바뀌면 직급 셀렉트 활성/비활성 갱신
	deptSel.addEventListener('change', e => updatePosState(e.target.value));
}

document.addEventListener('DOMContentLoaded', initOrgSelects);

 
/**
 * 주소검색 API (다음 우편번호)
 */
window.btnSearchZip = function() {
	if (!window.daum || !daum.Postcode) {
		alert('우편번호 스크립트가 아직 로드되지 않았습니다. 잠시 후 다시 시도하세요.');
		return;
	}
	
	// 다음 우편번호 팝업
	new daum.Postcode({
		oncomplete: function(data) {
			var addr = '';				// 최종 주소 문자열
			var extraRoadAddr = '';		// 참고 항목(동, 건물명 등)
			
			if (data.userSelectedType === 'R') {
				// 도로명 주소를 선택했을 때
				addr = data.roadAddress;
				
				// 법정동명(bname)이 있고, 동/로/가 로 끝나면 참고항목에 추가
				if (data.bname !== '' && /[동|로|가]$/g.test(data.bname)) {
					extraRoadAddr += data.bname;
				}
				// 아파트 동 건물명이 있고 공동주택이면 참고항목에 추가
				if (data.buildingName !== '' && data.apartment === 'Y') {
	            	extraRoadAddr += (extraRoadAddr !== '' ? ', ' + data.buildingName : data.buildingName);
	            }
				// 참고항목이 있으면 괄호로 감싸서 뒤에 붙임
	         	if (extraRoadAddr !== '') {
	            	addr += ' (' + extraRoadAddr + ')';
	          	}
			} else {
				// 지번 주소 선택 시
				addr = data.jibunAddress;
			}
			
			// 값 세팅
	        document.getElementById('zipcode').value = data.zonecode; // 우편번호
	        document.getElementById('address').value = addr;          // 기본주소
	        document.getElementById('addressDetail').focus();         // 상세주소로 커서 이동
		}
	}).open();
};


/**
 * 연락처 자동 하이픈
 */
document.addEventListener("DOMContentLoaded", () => {
	const phoneInput = document.getElementById("phone");
	if (!phoneInput) return;
	
	phoneInput.addEventListener("input", () => {
		// 1) 숫자만 추출 (숫자가 아닌 문자 모두 제거)
		let value = phoneInput.value.replace(/[^0-9]/g, "");
		
		// 2) 최대 11자리까지만 유지
	    value = value.substring(0, 11);
		
		// 3) 길이에 따라 하이픈 넣어서 다시 세팅
		if (value.length < 4) {
			// 3자리 이하 -> 그냥 숫자만
			phoneInput.value = value;
		} else if (value.length < 8) {
			// 4~7자리 -> 3자리 - 나머지
			phoneInput.value = `${value.slice(0, 3)}-${value.slice(3)}`;
		} else {
			// 8~11자리 -> 3자리 - 4자리 - 나머지
			phoneInput.value = `${value.slice(0, 3)}-${value.slice(3, 7)}-${value.slice(7)}`;
		}
	});
});


/**
 * 주민등록번호
 */
document.addEventListener("DOMContentLoaded", () => {
	const el = document.getElementById("rrn");
	if (!el) return;
	
	el.addEventListener("input", () => {
		// 1) 숫자만 남기기
		let v = el.value.replace(/\D/g, "");
		// 2) 최대 13자리까지만 허용 (하이픈 제외)
		v = v.slice(0, 13); 
		// 3) 7번째 자리 이후부터는 자동으로 하이픈 삽입
		if (v.length > 6) v = v.slice(0, 6) + "-" + v.slice(6);
		// 4) input 값에 다시 반영 -> 항상 "######-#######" 형태 유지
		el.value = v;
	});
});


/**
 * 이름 = 예금주
 */
document.addEventListener('DOMContentLoaded', function () {
	const nameInput  = document.getElementById('empName');		// 이름 입력칸
	const holderInput = document.getElementById('holder'); 		// 예금주 입력칸
	const holderView  = document.getElementById('holderView');  // 예금주 읽기 전용
	
	// 이름 input이 있고, 예금주 둘 중 하나라도 있을 때만 동장
	if (nameInput && (holderInput || holderView)) {
		const syncHolder = () => {
			const v = nameInput.value.trim();	// 공백 제거한 이름 값
			if (holderInput) holderInput.value = v;
	        if (holderView)  holderView.value  = v;
		};
		
		// 처음 로딩할 때 한 번
	    syncHolder();
	    // 이름 타이핑할 때마다 따라가게
	    nameInput.addEventListener('input', syncHolder);
	}
});


/**
 * 프로필 사진 미리보기
 */
document.addEventListener("DOMContentLoaded", () => {
	const fileInput = document.getElementById('photoFile');
	const preview   = document.getElementById('photoPreview');
	
	if (!fileInput || !preview) return;   // 다른 화면일 때 가드
	
	fileInput.addEventListener('change', (event) => {
		const file = event.target.files[0];	  // 첫 번째 선택 파일
		if (!file) return; 					  // 파일 선택 취소 시

		const reader = new FileReader();	  // 파일 내용을 읽어오는 객체
		
		// 파일을 다 읽으면 실행될 콜백
		reader.onload = (e) => {
			preview.src = e.target.result;    // 미리보기 이미지 교체
		};
		reader.readAsDataURL(file);
	});
});






