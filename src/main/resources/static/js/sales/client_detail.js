document.addEventListener("DOMContentLoaded", () => {
    const holder = document.getElementById("msgHolder");
    if (holder && holder.dataset.msg) {
        alert(holder.dataset.msg);
    }
});

/* ===========================
   수정 모드 ON
=========================== */
function enableEdit() {
    document.querySelectorAll('.view-mode').forEach(e => e.classList.add('d-none'));
    document.querySelectorAll('.edit-mode').forEach(e => e.classList.remove('d-none'));

    document.getElementById('btnEdit').classList.add('d-none');
    document.getElementById('btnSave').classList.remove('d-none');
    document.getElementById('btnCancel').classList.remove('d-none');
}

/* ===========================
   수정 취소 → 새로고침
=========================== */
function cancelEdit() {
    location.reload();
}

/* ===========================
   다음 주소 검색
=========================== */
function searchAddress() {
    new daum.Postcode({
        oncomplete: function(data) {
            const road = data.roadAddress;
            const jibun = data.jibunAddress;

            const addr = road ? road : jibun;

            // 🔥 주소 입력
            document.getElementById('addr').value = addr;

            // 🔥 우편번호 입력
            if (document.getElementById('postCode')) {
                document.getElementById('postCode').value = data.zonecode;
            }

            // 상세주소로 포커스 이동
            document.getElementById('addrDetail').focus();
        }
    }).open();
}

/* ===========================
   저장
=========================== */
function saveClient() {

    const client = {
        clientId:        getValue("clientId"),           
        ceoName:         getValue("ceoName"),
        managerName:     getValue("managerName"),
        managerDept:     getValue("managerDept"),
        managerTel:      getValue("managerTel"),
        managerEmail:    getValue("managerEmail"),
		bizType:         getValue("bizType"),
		bizItem:         getValue("bizItem"),
        addr:            getValue("addr"),
        addrDetail:      getValue("addrDetail"),

        // 🔥 추가된 우편번호
        postCode:        getValue("postCode"),

        // 계좌 정보
        accountNumber:   getValue("accountNumber"),
        accountName:     getValue("accountName"),
        bankName:        getValue("bankName"),

        // 상태
        statusCode:      getValue("statusCode")
    };

    // ===== CSRF TOKEN 처리 =====
    const csrfToken  = document.querySelector('meta[name="_csrf_token"]')?.content || "";
    const csrfHeader = document.querySelector('meta[name="_csrf_headerName"]')?.content || "";

    const headers = { "Content-Type": "application/json" };
    if (csrfToken && csrfHeader) {
        headers[csrfHeader] = csrfToken;
    }

    fetch(apiUrl(`sales/client/update`), {
        method: "POST",
        headers: headers,
        body: JSON.stringify(client)
    })
    .then(res => {
        if (!res.ok) throw new Error("저장 실패");
        return res.text();
    })
    .then(msg => {
        alert("저장되었습니다.");
        location.reload();
    })
    .catch(err => alert("오류 발생: " + err.message));
}

/* ===========================
   공통 input getter
=========================== */
function getValue(id) {
    const el = document.getElementById(id);
    return el ? el.value : "";
}
