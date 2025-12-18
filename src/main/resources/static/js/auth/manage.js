document.addEventListener('DOMContentLoaded', function () {

  // ================== 1) Grid 생성 ==================
  const grid = new tui.Grid({
    el: document.getElementById('empGrid'),
    scrollY: true,
    bodyHeight: 500,
    rowHeaders: ['rowNum'],
    columns: [
      { 
		header: '사번', 
		name: 'empId', 
		width: 110, 
		align: 'center' 
	  },
      { 
		header: '이름', 
		name: 'empName', 
		width: 90, 
		align: 'center' 
	  },
      { 
		header: '부서', 
		name: 'deptName', 
		minWidth: 120 
	  },
      { 
		header: '직급', 
		name: 'posName', 
		width: 80, 
		align: 'center' 
	  }
    ],
    data: {
      api: {
        readData: {
          url: '/auth/manage/data',
          method: 'GET'
        }
      }
    }
  });

  // ================== 2) 검색 폼 연동 ==================
  const searchForm = document.getElementById('empSearchForm');

  searchForm.addEventListener('submit', function (e) {
    e.preventDefault();

    const keyword = searchForm.querySelector('input[name="keyword"]').value.trim();
    const deptId  = searchForm.querySelector('select[name="deptId"]').value;

    // 1페이지부터 다시 조회
    grid.readData(1, { keyword, deptId }, true);
  });

  // 첫 로딩
  grid.readData(1, {}, true);

  // ================== 3) 행 클릭 시 오른쪽 패널에 반영 ==================
  const selEmpIdEl   = document.getElementById('sel-empId');
  const selEmpNameEl = document.getElementById('sel-empName');
  const selDeptPosEl = document.getElementById('sel-deptPos');
  const formEmpId    = document.getElementById('formEmpId');
  const btnSaveRole  = document.getElementById('btnSaveRole');
  const roleCheckboxes = document.querySelectorAll('.role-checkbox');

  grid.on('click', function (ev) {
    const rowKey = ev.rowKey;
    if (rowKey == null) return;

    const row = grid.getRow(rowKey);
    if (!row) return;

    const empId   = row.empId;
    const empName = row.empName;
    const dept    = row.deptName || '';
    const pos     = row.posName || '';

    selEmpIdEl.textContent   = empId;
    selEmpNameEl.textContent = empName;
    selDeptPosEl.textContent = dept && pos ? `${dept} / ${pos}` : (dept || pos || '—');
    formEmpId.value          = empId;

    btnSaveRole.disabled = false;

    // 사원별 역할 Ajax로 가져오기
    fetch('/auth/manage/' + empId + '/roles')
      .then(res => res.json())
      .then(roleCodes => {
        roleCheckboxes.forEach(cb => {
          cb.checked = roleCodes.includes(cb.value);
        });
      })
      .catch(err => {
        console.error('권한 조회 실패', err);
        alert('권한 정보를 불러오는 중 오류가 발생했습니다.');
      });
  });

    // ================== 4) 권한 저장: confirm -> (SYS_ADMIN 추가 confirm) -> 비번 모달 -> verify -> submit ==================
    const roleForm = document.getElementById('roleAssignForm');

    const adminPwModalEl = document.getElementById('adminPwModal');
    const adminPwModal = adminPwModalEl ? new bootstrap.Modal(adminPwModalEl) : null;
	
	// 저장 확인 모달
	const saveConfirmModalEl = document.getElementById('saveConfirmModal');
	const saveConfirmModal = saveConfirmModalEl ? new bootstrap.Modal(saveConfirmModalEl) : null;

	const confirmEmpText   = document.getElementById('confirmEmpText');
	const confirmRoleBadges = document.getElementById('confirmRoleBadges');
	const confirmRoleCount = document.getElementById('confirmRoleCount');
	const confirmSysWarn   = document.getElementById('confirmSysWarn');
	const confirmNoRole    = document.getElementById('confirmNoRole');
	const confirmSysAgreeWrap = document.getElementById('confirmSysAgreeWrap');
	const confirmSysAgree = document.getElementById('confirmSysAgree');

	const btnOpenPwModal = document.getElementById('btnOpenPwModal');

    const adminPwInput = document.getElementById('adminPassword');
    const adminPwError = document.getElementById('adminPwError');
    const btnVerifyAdminPw = document.getElementById('btnVerifyAdminPw');

	btnSaveRole.addEventListener('click', function () {

	  // 사원 선택 확인
	  const empId = formEmpId.value;
	  const empName = (document.getElementById('sel-empName')?.textContent || '').trim();
	  const empIdText = (document.getElementById('sel-empId')?.textContent || '').trim();

	  if (!empId) {
	    alert('사원을 선택하세요.');
	    return;
	  }

	  // 선택된 권한들
	  const checked = Array.from(document.querySelectorAll('.role-checkbox:checked'));
	  const roles = checked.map(cb => {
	    const label = document.querySelector(`label[for="${cb.id}"]`);
	    return label ? label.innerText.replace(/\s+/g, ' ').trim() : cb.value;
	  });

	  // 저장 확인 모달이 없으면 fallback
	  if (!saveConfirmModal) {
	    if (!confirm('선택한 권한으로 저장하시겠습니까?')) return;
	    adminPwInput.value = '';
	    adminPwError.classList.add('d-none');
	    adminPwModal.show();
	    return;
	  }

	  // 모달 내용 채우기
	  confirmEmpText.textContent = `${empName} (${empIdText})`;
	  confirmRoleBadges.innerHTML = '';
	  confirmRoleCount.textContent = roles.length;

	  const hasSysAdmin = checked.some(cb => cb.value === 'ROLE_SYS_ADMIN');
	  confirmSysWarn.classList.toggle('d-none', !hasSysAdmin);
	  
	  // SYS_ADMIN 동의 체크박스 강제
	  if (hasSysAdmin) {
	    confirmSysAgreeWrap?.classList.remove('d-none');
	    if (confirmSysAgree) {
	      confirmSysAgree.checked = false;
	      btnOpenPwModal.disabled = true;

	      confirmSysAgree.onchange = () => {
	        btnOpenPwModal.disabled = !confirmSysAgree.checked;
	      };
	    }
	  } else {
	    confirmSysAgreeWrap?.classList.add('d-none');
	    btnOpenPwModal.disabled = false;

	    if (confirmSysAgree) {
	      confirmSysAgree.checked = false;
	      confirmSysAgree.onchange = null;
	    }
	  }


	  if (roles.length === 0) {
	    confirmNoRole.classList.remove('d-none');
	  } else {
	    confirmNoRole.classList.add('d-none');

	    roles.forEach(text => {
	      const span = document.createElement('span');
	      span.className = 'badge rounded-pill bg-light text-dark border';
	      span.textContent = text;
	      confirmRoleBadges.appendChild(span);
	    });
	  }

	  saveConfirmModal.show();
	});

	// 비밀번호 입력하는 모달로 이동
	btnOpenPwModal?.addEventListener('click', function () {
		
		// 방어: SYS_ADMIN 포함인데 동의 안 했으면 막기
		const hasSysAdminNow = Array.from(document.querySelectorAll('.role-checkbox:checked'))
		  .some(cb => cb.value === 'ROLE_SYS_ADMIN');

		if (hasSysAdminNow && confirmSysAgree && !confirmSysAgree.checked) {
		  alert('SYS_ADMIN 부여 동의 체크가 필요합니다.');
		  return;
		}
		
	  saveConfirmModal.hide();

	  adminPwInput.value = '';
	  adminPwError.classList.add('d-none');
	  adminPwModal.show();
	});

	// 비밀번호 입력 모달
    btnVerifyAdminPw?.addEventListener('click', function () {
      const pw = (adminPwInput.value || '').trim();
      if (!pw) {
        adminPwError.textContent = '비밀번호를 입력하세요.';
        adminPwError.classList.remove('d-none');
        return;
      }

      // 비밀번호 재확인 API 호출
      fetch('/auth/manage/verify', {
        method: 'POST',
        headers: {
          'Content-Type': 'application/x-www-form-urlencoded',
          ...(typeof csrfHeader !== 'undefined' && typeof csrfToken !== 'undefined'
              ? { [csrfHeader]: csrfToken }
              : {})
        },
        body: new URLSearchParams({ password: pw })
      })
      .then(res => res.json())
      .then(data => {
        if (!data || !data.success) {
          adminPwError.textContent = data?.message || '비밀번호 확인 실패';
          adminPwError.classList.remove('d-none');
          return;
        }

        adminPwModal.hide();
        roleForm.submit(); // 검증 성공 시에만 저장 요청
      })
      .catch(err => {
        console.error('verify error', err);
        adminPwError.textContent = '비밀번호 확인 중 오류가 발생했습니다.';
        adminPwError.classList.remove('d-none');
      });
    });

  });

  // 비밀번호 보기/숨기기 (manage 페이지에 없으면 추가)
  function togglePassword(inputId, iconEl) {
    const input = document.getElementById(inputId);
    if (!input) return;

    if (input.type === 'password') {
      input.type = 'text';
      iconEl.classList.remove('bx-hide');
      iconEl.classList.add('bx-show');
    } else {
      input.type = 'password';
      iconEl.classList.remove('bx-show');
      iconEl.classList.add('bx-hide');
    }
  }