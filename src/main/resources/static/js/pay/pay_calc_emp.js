
/* ===============================
    사원 기본 정보 로드
================================ */
document.getElementById("btnLoadEmpInfo")?.addEventListener("click", loadEmpInfo);
document.getElementById("btnSimulateOne")?.addEventListener("click", simulateOne);
document.getElementById("btnConfirmOne")?.addEventListener("click", confirmOne);

function loadEmpInfo() {
    const empId = document.getElementById("emp_select").value;
    const yyyymm = document.getElementById("emp_calc_month").value;

    if (!empId) return alert("사원을 선택하세요.");

    fetch(apiUrl(`pay/calc/emp/info?empId=${empId}&yyyymm=${yyyymm}`))
        .then(res => res.json())
        .then(data => {
            if (!data) {
                alert("사원 정보를 불러올 수 없습니다.");
                return;
            }

            // 기본정보 세팅
            document.getElementById("e-empId").innerText = data.empId ?? "";
            document.getElementById("e-empName").innerText = data.empName ?? "";
            document.getElementById("e-deptName").innerText = data.deptName ?? "";
            document.getElementById("e-posName").innerText = data.posName ?? "";

            // 상태 배지 업데이트
            updateCalcStatusBadge(data.calcStatus);

            // UI 표시
            document.getElementById("empInfoBox").style.display = "block";
            document.getElementById("btnSimulateOne").disabled = false;
            document.getElementById("btnConfirmOne").disabled = false;

            // URL 파라미터 유지
            history.replaceState({}, "", `/pay/calc/emp?yyyymm=${yyyymm}&empId=${empId}`);
        });
}

/* ===============================
    상태 배지 업데이트 함수
================================ */
function updateCalcStatusBadge(status) {
    const box = document.getElementById("empStatusBox");
    const text = document.getElementById("empStatusText");

    box.style.display = "block";

    switch (status) {
        case "SIMULATED":
            box.className = "alert alert-warning d-flex justify-content-between align-items-center";
            text.className = "badge bg-warning text-dark";
            text.innerText = "가계산";
            break;

        case "CALCULATED":
            box.className = "alert alert-primary d-flex justify-content-between align-items-center";
            text.className = "badge bg-primary";
            text.innerText = "계산완료";
            break;

        case "CONFIRMED":
            box.className = "alert alert-success d-flex justify-content-between align-items-center";
            text.className = "badge bg-success";
            text.innerText = "확정완료";
            break;

        default:
            box.className = "alert alert-secondary d-flex justify-content-between align-items-center";
            text.className = "badge bg-secondary";
            text.innerText = "미계산";
    }
}

/* ===============================
    단일 사원 가계산
================================ */
function simulateOne() {
    const empId = document.getElementById("emp_select").value;
    const yyyymm = document.getElementById("emp_calc_month").value;

    if (!empId) return alert("사원을 선택하세요.");

    fetch(apiUrl(`pay/calc/emp/simulate`), {
        method: "POST",
        headers: { 
            'Content-Type': 'application/x-www-form-urlencoded',
            [csrfHeader]: csrfToken
        },
        body: new URLSearchParams({ empId, yyyymm })
    })
    .then(res => res.json())
	.then(data => {
	    if (!data.success) {
	        alert(data.message);
	        return;
	    }

	    renderResult(data.data);
	    loadEmpInfo();
	});

}

/* ===============================
    단일 사원 확정
================================ */
function confirmOne() {
    const empId = document.getElementById("emp_select").value;
    const yyyymm = document.getElementById("emp_calc_month").value;

    if (!empId) return alert("사원을 선택하세요.");

    fetch(apiUrl(`pay/calc/emp/confirm`), {
        method: "POST",
        headers: { 
            'Content-Type': 'application/x-www-form-urlencoded',
            [csrfHeader]: csrfToken
        },
        body: new URLSearchParams({ empId, yyyymm })
    })
    .then(res => res.json())
	.then(data => {
	    if (!data.success) {
	        alert(data.message);
	        return;
	    }

	    renderResult(data.data);
	    loadEmpInfo();
	});

}

/* ===============================
    결과 표시
================================ */
function renderResult(data) {

    document.getElementById("empResultBox").style.display = "block";

    // 결과창 상태 표현도 동일하게 세팅
    updateCalcStatusBadge(data.calcStatus);

    document.getElementById("r-baseAmt").innerText = numberFormat(data.baseAmt) + " 원";
    document.getElementById("r-alwAmt").innerText = numberFormat(data.alwAmt) + " 원";
    document.getElementById("r-dedAmt").innerText = numberFormat(data.dedAmt) + " 원";
    document.getElementById("r-netAmt").innerText = numberFormat(data.netAmt) + " 원";

    // 지급 항목
    let payHtml = "";
    (data.payItems ?? []).forEach(i => {
        payHtml += `<tr><td>${i.itemName}</td><td class="text-end">${numberFormat(i.amount)}</td></tr>`;
    });
    document.getElementById("r-payItems").innerHTML = payHtml;

    // 공제 항목
    let dedHtml = "";
    (data.dedItems ?? []).forEach(i => {
        dedHtml += `<tr><td>${i.itemName}</td><td class="text-end">${numberFormat(i.amount)}</td></tr>`;
    });
    document.getElementById("r-dedItems").innerHTML = dedHtml;
}

/* 숫자 포맷 */
function numberFormat(x) {
    return x?.toLocaleString() ?? "0";
}


/* ===========================
   사원 검색 자동완성
=========================== */
const empKeyword = document.getElementById("emp_keyword");
const empSelect = document.getElementById("emp_select");

empKeyword?.addEventListener("input", function () {
    const keyword = empKeyword.value.trim();
    if (!keyword) {
        empSelect.innerHTML = `<option value="">-- 사원 선택 --</option>`;
        return;
    }

    fetch(apiUrl(`pay/calc/searchEmployee?keyword=`) + encodeURIComponent(keyword))
        .then(res => res.json())
        .then(list => {

            empSelect.innerHTML = `<option value="">-- 사원 선택 --</option>`;

            list.forEach(emp => {
                const opt = document.createElement("option");
                opt.value = emp.empId;
                opt.textContent = `${emp.empId} | ${emp.empName}`;
                empSelect.appendChild(opt);
            });
        });
});
/* ===========================
            초기화 
=========================== */

document.getElementById("btnResetEmp").addEventListener("click", () => {

    // 입력값 초기화
    document.getElementById("emp_keyword").value = "";
    document.getElementById("emp_select").value = "";
	
	// 계산월 초기화 (첫 번째 옵션으로 돌리기)
	    const monthSelect = document.getElementById("emp_calc_month");
	    if (monthSelect) {
	        monthSelect.selectedIndex = 0;   // 첫 번째 항목 선택
	    }

    // 사원 정보 숨기기
    document.getElementById("empInfoBox").style.display = "none";
    document.getElementById("empResultBox").style.display = "none";

    // 버튼 비활성화
    document.getElementById("btnSimulateOne").disabled = true;
    document.getElementById("btnConfirmOne").disabled = true;

    // 상태 박스 숨기기
    const statusBox = document.getElementById("empStatusBox");
    if (statusBox) statusBox.style.display = "none";
});

