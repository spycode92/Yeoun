// ===============================
// 급여 이력 조회 JS (AG Grid)
// ===============================

let gridApi = null;
let currentPage = 0;
const pageSize = 20;

// ===============================
// DOM 준비되면 초기 실행
// ===============================
document.addEventListener("DOMContentLoaded", () => {

    initGrid();
    initEvents();
    loadPayHistory(0);
});


// ===============================
// AG Grid 초기화
// ===============================
function initGrid() {

	const columnDefs = [
	    { 
	        headerName: "지급월", field: "payYymm",  width: 120,
	        valueFormatter: p => formatYymm(p.value),
	        cellStyle: { textAlign: "center", fontWeight: "600" }
	    },
	    { headerName: "사번", field: "empId", width: 120 },
	    { headerName: "이름", field: "empName", width: 120 },
	    { headerName: "부서", field: "deptName", width: 120 },
        {
            headerName: "총지급액",
            field: "totAmt",
            width: 120,
            valueFormatter: p => numberFormat(p.value),
            cellStyle: { textAlign: "right" }
        },
		
		{
			            headerName: "기본급",
			            field: "baseAmt",
			            width: 120,
			            valueFormatter: p => numberFormat(p.value),
			            cellStyle: { textAlign: "right" }
		        },
				{
						            headerName: "수당합계",
						            field: "alwAmt",
						            width: 120,
						            valueFormatter: p => numberFormat(p.value),
						            cellStyle: { textAlign: "right" }
						        },
								{
										            headerName: "공제액",
										            field: "dedAmt",
										            width: 120,
										            valueFormatter: p => numberFormat(p.value),
										            cellStyle: { textAlign: "right" }
										        },
        {
            headerName: "실수령액",
            field: "netAmt",
            width: 120,
            valueFormatter: p => numberFormat(p.value),
            cellStyle: { textAlign: "right", color: "#0d6efd", fontWeight: "600" }
        },
       
		{
		    headerName: "상세",
		    width: 120,
		    cellRenderer: p => {
		        return `<button class="btn btn-sm btn-outline-primary" 
		                      onclick="openDetailModal('${p.data.payYymm}', '${p.data.empId}')">
		                   상세보기
		                </button>`;
		    },
		    cellStyle: { textAlign: "center" }
		},
		{
		           headerName: "상태",
		           field: "calcStatus",
		           width: 120,
		           cellRenderer: p => statusBadge(p.value),
		           cellStyle: { textAlign: "center" }
		       }

    ];

	const gridOptions = {
	    columnDefs,
	    rowData: [],
	    pagination: true,              // ← ★ 페이지네이션 활성화
	    paginationPageSize: 20,        // ← ★ 1페이지당 20줄
	    paginationPageSizeSelector: [10, 20, 50, 100],  // 선택 변경 가능 (옵션)
	};


    const gridDiv = document.querySelector("#payslipGrid");
    if (!gridDiv) {
        console.error("❌ #payslipGrid 요소를 찾을 수 없습니다.");
        return;
    }

    gridApi = agGrid.createGrid(gridDiv, gridOptions);
}



// ===============================
// 이벤트 설정
// ===============================
function initEvents() {

    const btnSearch = document.getElementById("btnSearch");
    if (btnSearch) {
        btnSearch.addEventListener("click", () => loadPayHistory(0));
    }

    const btnReset = document.getElementById("btnReset");
    if (btnReset) {
        btnReset.addEventListener("click", () => {
            const ids = ["keyword", "deptName", "year", "month"];
            ids.forEach(id => {
                const el = document.getElementById(id);
                if (el) el.value = "";
            });
            loadPayHistory(0);
        });
    }

    const modeSelect = document.getElementById("mode");
    const deptName = document.getElementById("deptName");

    if (modeSelect && deptName) {
        modeSelect.addEventListener("change", (e) => {
            deptName.style.display = e.target.value === "dept" ? "block" : "none";
        });
    }
}



// ===============================
// 급여 이력 조회 API
// ===============================
function loadPayHistory(page) {

    currentPage = page;

	const params = new URLSearchParams({
	    page,
	    size: pageSize,
	    mode: document.getElementById("mode")?.value ?? "",
	    keyword: document.getElementById("keyword")?.value ?? "",
	    empName: document.getElementById("keyword")?.value ?? "",
	    deptName: document.getElementById("deptName")?.value ?? "",
	    year: document.getElementById("year")?.value ?? "",
	    month: document.getElementById("month")?.value ?? ""
	});


    fetch(apiUrl(`pay/history/search?${params.toString()}`))
        .then(res => res.json())
        .then(data => {
            if (!gridApi) {
                console.error("❌ Grid API가 없습니다. initGrid() 확인 필요");
                return;
            }
            // 🔥 AG Grid v31 방식
            gridApi.setGridOption('rowData', data);
           
        })
        .catch(err => {
            console.error("급여 이력 조회 오류:", err);
            alert("급여 이력 조회에 실패했습니다.");
        });
}


// ===============================
// 유틸 함수
// ===============================
function numberFormat(x) {
    if (!x) return "";
    return x.toString().replace(/\B(?=(\d{3})+(?!\d))/g, ",");
}

function statusBadge(s) {
    if (!s) return "";

    let color = "bg-secondary";
    let label = "미확인";   // 기본값

    switch (s) {
        case "SIMULATED":
            color = "bg-secondary";
            label = "가계산";
            break;
        case "CALCULATED":
            color = "bg-primary";
            label = "계산완료";
            break;
        case "CONFIRMED":
            color = "bg-success";
            label = "확정";
            break;
        case "ERROR":
            color = "bg-danger";
            label = "오류";
            break;
        default:
            label = s;
    }

    return `<span class="badge ${color}">${label}</span>`;
}



// ===============================
// 지급월 포맷 (YYYYMM → YYYY-MM)
// ===============================
function formatYymm(yymm) {
    if (!yymm || yymm.length !== 6) return yymm;
    const yyyy = yymm.substring(0, 4);
    const mm = yymm.substring(4, 6);
    return `${yyyy}-${mm}`;
}

// ===============================
// 급여 상세모달
// ===============================
async function openDetailModal(payYymm, empId) {

    try {
        // 🔥 상세 데이터 조회 API 호출
        const res = await fetch(apiUrl(`pay/history/detail?payYymm=${payYymm}&empId=${empId}`));
        const data = await res.json();

        // ===========================
        // 1) 기본정보 표시
        // ===========================
        document.getElementById("d-payYymm").innerText = formatYymm(data.payYymm);
        document.getElementById("d-empId").innerText = data.empId;
        document.getElementById("d-empName").innerText = data.empName;
        document.getElementById("d-deptName").innerText = data.deptName;
		document.getElementById("d-posName").innerText = data.posName;
		document.getElementById("confirmUser").textContent = data.confirmUser ?? "-";		
		if (data.confirmDate) {
		    const d = new Date(data.confirmDate);
		    const formatted =
		        d.getFullYear() + "-" +
		        String(d.getMonth()+1).padStart(2,"0") + "-" +
		        String(d.getDate()).padStart(2,"0") + " " +
		        String(d.getHours()).padStart(2,"0") + ":" +
		        String(d.getMinutes()).padStart(2,"0") + ":" +
		        String(d.getSeconds()).padStart(2,"0");

		    document.getElementById("confirmDate").textContent = formatted;
		} else {
		    document.getElementById("confirmDate").textContent = "-";
		}


        document.getElementById("d-baseAmt").innerText = numberFormat(data.baseAmt) + " 원";
        document.getElementById("d-alwAmt").innerText  = numberFormat(data.alwAmt) + " 원";
        document.getElementById("d-dedAmt").innerText  = numberFormat(data.dedAmt) + " 원";
        document.getElementById("d-netAmt").innerText  = numberFormat(data.netAmt) + " 원";
        document.getElementById("d-totAmt").innerText  = numberFormat(data.totAmt) + " 원";

        // ===========================
        // 2) 지급항목 테이블 표시
        // ===========================
        renderItemTable("payItemsBody", data.payItems);

        // ===========================
        // 3) 공제항목 테이블 표시
        // ===========================
        renderItemTable("dedItemsBody", data.dedItems);

        // ===========================
        // 4) 모달 열기
        // ===========================
        new bootstrap.Modal(document.getElementById("detailModal")).show();

    } catch (err) {
        console.error("상세조회 오류:", err);
        alert("상세 조회 중 오류가 발생했습니다.");
    }
}


// ===============================
// 지급 / 공제 테이블 채우기 함수
// ===============================
function renderItemTable(target, list) {
    const el = document.getElementById(target);
    el.innerHTML = "";

    (list ?? []).forEach(it => {
        el.innerHTML += `
            <tr>
                <td>${it.itemName}</td>
                <td class="text-end">${numberFormat(it.amount)}</td>
            </tr>
        `;
    });
}


// ===============================
// 연도/월 자동생성
// ===============================

document.addEventListener("DOMContentLoaded", () => {

    // 현재 연도
    const thisYear = new Date().getFullYear();
    const yearSelect = document.getElementById("year");

    // 올해, 작년만 넣기
    yearSelect.innerHTML = `
        <option value="">연도</option>
        <option value="${thisYear}">${thisYear}</option>
        <option value="${thisYear - 1}">${thisYear - 1}</option>
    `;

    // 월 1~12 자동 생성
    const monthSelect = document.getElementById("month");
    let monthHtml = `<option value="">월</option>`;
    for (let i = 1; i <= 12; i++) {
        const v = i.toString().padStart(2, "0");
        monthHtml += `<option value="${v}">${i}월</option>`;
    }
    monthSelect.innerHTML = monthHtml;

});