/** ============================================================
 *  신규 거래처 등록 화면 전용 JS
 *  - 사업자번호 자동 포맷팅
 *  - 사업자번호 자리수 검증
 *  - 사업자번호 중복확인
 *  - 담당자 전화번호 자동 하이픈
 *  - 카카오 주소 검색
 * ============================================================ */

// ===============================
// 1) 사업자번호 자동 포맷팅 + 자리수 검증
// ===============================
document.addEventListener("DOMContentLoaded", function () {

    const bizInput = document.getElementById("businessNo");
    const telInput = document.getElementById("managerTel");
    const bizMsg = document.getElementById("bizMsg");

    /* ---------------------------
       사업자번호 입력 이벤트
    --------------------------- */
    if (bizInput) {
        bizInput.addEventListener("input", function (e) {
            let val = e.target.value.replace(/[^0-9]/g, ""); // 숫자만 허용

            // 하이픈 자동 삽입 (XXX-XX-XXXXX)
            if (val.length > 3 && val.length <= 5) {
                val = val.slice(0, 3) + "-" + val.slice(3);
            } else if (val.length > 5) {
                val =
                    val.slice(0, 3) +
                    "-" +
                    val.slice(3, 5) +
                    "-" +
                    val.slice(5, 10);
            }

            e.target.value = val;

            // 자리수 검증 (10자리)
            const onlyNum = val.replace(/-/g, "");
            if (onlyNum.length === 10) {
                bizMsg.textContent = "사업자번호 형식이 올바릅니다.";
                bizMsg.className = "text-primary";
            } else {
                bizMsg.textContent = "사업자번호는 숫자 10자리여야 합니다.";
                bizMsg.className = "text-danger";
            }
        });
    }

    /* ---------------------------
       전화번호 자동 하이픈
    --------------------------- */
    if (telInput) {
        telInput.addEventListener("input", function (e) {
            let val = e.target.value.replace(/[^0-9]/g, "");

            if (val.length < 4) {
                e.target.value = val;
            } else if (val.length < 8) {
                e.target.value = val.slice(0, 3) + "-" + val.slice(3);
            } else {
                e.target.value =
                    val.slice(0, 3) +
                    "-" +
                    val.slice(3, 7) +
                    "-" +
                    val.slice(7, 11);
            }
        });
    }
});

// ===============================
// 2) 사업자번호 중복확인
// ===============================
function checkBusinessNo() {
    const bizMsg = document.getElementById("bizMsg");
    const num = document.getElementById("businessNo").value.replace(/-/g, "");

    if (num.length !== 10) {
        bizMsg.innerText = "사업자번호는 10자리여야 합니다.";
        bizMsg.className = "text-danger";
        return;
    }

    fetch(apiUrl(`sales/client/check-business?businessNo=`) + num)
        .then(res => res.json())
        .then(valid => {
            if (valid) {
                bizMsg.innerHTML = "사용 가능한 사업자번호입니다.";
                bizMsg.className = "text-primary";
            } else {
                bizMsg.innerHTML = "이미 등록된 사업자번호입니다.";
                bizMsg.className = "text-danger";
            }
        });
}


// ===============================
// 3) 카카오 주소 API
// ===============================
function findAddress() {
    new daum.Postcode({
        oncomplete: function (data) {
            document.getElementById("postcode").value = data.zonecode;
            document.getElementById("address").value = data.address;
        }
    }).open();
}

// 전역에서 쓸 수 있게 export (필요시)
window.findAddress = findAddress;
window.checkBusinessNo = checkBusinessNo;

// =====================================
// 4) 등록 버튼 눌렀을 때 최종 검증
// =====================================
document.addEventListener("DOMContentLoaded", () => {
    const form = document.getElementById("clientForm");
    const bizInput = document.getElementById("businessNo");

    form.addEventListener("submit", function (e) {
		
		// ⭐ 계좌번호 자릿수 검증 추가
		  if (!validateAccountNo()) {
		      e.preventDefault();
		      return;
		  }

	     const bizNo = bizInput.value.replace(/-/g, "");

        // 길이검증
        if (bizNo.length !== 10) {
            e.preventDefault();
            alert("사업자번호는 숫자 10자리여야 합니다.");
            return;
        }

        // 🔥 등록 전에 중복 체크를 다시 요청
        fetch(apiUrl(`sales/client/check-business?businessNo=${bizNo}`))
            .then(res => res.json())
            .then(valid => {

                if (!valid) {
                    // 사용 불가 → 이미 등록됨
                    e.preventDefault();
                    alert("이미 등록된 사업자번호입니다.");
                } 
                else {
                    form.submit(); // 통과 → 실제 제출
                }
            });

        e.preventDefault();  // 비동기 처리 완료될 때까지 기본 제출 막기
    });
});


// ===============================
// 5) 계좌번호 자릿수 검증 (간단)
// ===============================
function validateAccountNo() {
    const accountInput = document.getElementById("accountNumber");

    if (!accountInput) return true; // 화면에 없으면 통과

    const val = accountInput.value.replace(/[^0-9]/g, "");
    accountInput.value = val; // 숫자만 유지

    if (val.length < 8 || val.length > 20) {
        alert("계좌번호는 숫자 8~20자리여야 합니다.");
        accountInput.focus();
        return false;
    }
    return true;
}

