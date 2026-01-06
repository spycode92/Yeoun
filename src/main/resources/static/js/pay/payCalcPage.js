console.log("payCalcPage.js loaded");

const slips = window.PAY_CALC_DATA.slips;
const currentYyyymm = window.PAY_CALC_DATA.yyyymm;

// ===============================
// 1. AG Grid 설정
// ===============================

const columnDefs = [
    { headerName: "사번", field: "empId", width: 120 },
    { headerName: "이름", field: "empName", width: 140 },
    { headerName: "부서", field: "deptName", width: 160 },
    { headerName: "기본급", field: "baseAmt", width: 120, valueFormatter: p => numberFormatRaw(p.value), cellClass: "text-end" },
    { headerName: "수당합계", field: "alwAmt", width: 120, valueFormatter: p => numberFormatRaw(p.value), cellClass: "text-end" },
    { headerName: "총지급액", field: "totAmt", width: 120, valueFormatter: p => numberFormatRaw(p.value), cellClass: "text-end" },
    { headerName: "공제액", field: "dedAmt", width: 120, valueFormatter: p => numberFormatRaw(p.value), cellClass: "text-end" },
    {
        headerName: "실수령액",
        field: "netAmt",
        width: 140,
        valueFormatter: p => numberFormatRaw(p.value),
        cellStyle: { color: "#0d6efd", fontWeight: '600', textAlign: "right" }
    },
    {
        headerName: "상태",
        field: "calcStatus",
        width: 130,
        cellRenderer: p => renderStatusBadge(p.value),
        cellStyle: { textAlign: "center" }
    },
    {
        headerName: "상세",
        width: 100,
        cellRenderer: p =>
            `<button class="btn btn-sm btn-outline-primary" onclick="openDetail('${p.data.empId}')">상세</button>`,
        cellStyle: { textAlign: "center" }
    }
];

const gridOptions = {
    columnDefs,
    rowData: slips,
    defaultColDef: {
        resizable: true,
        sortable: true,
        filter: true
    },
    pagination: true,
    paginationPageSize: 10,
    rowHeight: 38
};

document.addEventListener("DOMContentLoaded", () => {
    new agGrid.Grid(document.getElementById("payslipGrid"), gridOptions);

    // 월 선택 이벤트
    const monthSel = document.getElementById("calc_month");
    if (monthSel) {
        monthSel.addEventListener("change", e => {
            location.href = "/pay/calc?yyyymm=" + e.target.value;
        });
    }

    refreshStatus(currentYyyymm);
});

// ===============================
// 2. 상세보기 (모달)
// ===============================

async function openDetail(empId) {
    const mm = document.getElementById("calc_month").value;

    try {
        const res = await fetch(apiUrl(`pay/calc/detail?yyyymm=${mm}&empId=${empId}`));
        const data = await res.json();

        document.getElementById("d-empId").innerText = data.empId ?? "";
        document.getElementById("d-empName").innerText = data.empName ?? "";
        document.getElementById("d-deptName").innerText = data.deptName ?? "";

        document.getElementById("d-baseAmt").innerText = numberFormat(data.baseAmt);
        document.getElementById("d-alwAmt").innerText = numberFormat(data.alwAmt);
        document.getElementById("d-dedAmt").innerText = numberFormat(data.dedAmt);
        document.getElementById("d-netAmt").innerText = numberFormat(data.netAmt);

        const payBody = document.getElementById("payItemsBody");
        const dedBody = document.getElementById("dedItemsBody");
        payBody.innerHTML = "";
        dedBody.innerHTML = "";

        let payCnt = 0, dedCnt = 0;

        (data.payItems || []).forEach(it => {
            payBody.innerHTML += `
                <tr><td>${it.itemName}</td><td class="text-end">${numberFormat(it.amount)}</td></tr>`;
            payCnt++;
        });
        (data.dedItems || []).forEach(it => {
            dedBody.innerHTML += `
                <tr><td>${it.itemName}</td><td class="text-end">${numberFormat(it.amount)}</td></tr>`;
            dedCnt++;
        });

        document.getElementById("payCount").innerText = `${payCnt}건`;
        document.getElementById("dedCount").innerText = `${dedCnt}건`;

        new bootstrap.Modal(document.getElementById("detailModal")).show();

    } catch (e) {
        alert("상세 조회 실패: " + e);
    }
}

// ===============================
// 3. 상태 박스 갱신
// ===============================

async function refreshStatus(mm) {
    try {
        const res = await fetch(apiUrl(`pay/calc/status?yyyymm=${mm}`));
        if (!res.ok) return;

        const s = await res.json();
        const fmt = n => (n ?? 0).toLocaleString();

        document.getElementById("statMonth").innerText = mm;

        const statusMap = {
            CONFIRMED: "확정 완료",
            CALCULATED: "계산 완료",
            SIMULATED: "가계산",
            READY: "미계산"
        };

        document.getElementById("statCalc").innerText =
            statusMap[s.calcStatus] ?? "미계산";

        document.getElementById("statCount").innerText = `건수 ${s.count}`;
        document.getElementById("statTot").innerText = fmt(s.totalAmt);
        document.getElementById("statDed").innerText = fmt(s.dedAmt);
        document.getElementById("statNet").innerText = fmt(s.netAmt);

        const box = document.getElementById("statusBox");
        box.classList.remove("alert-success", "alert-warning");
        box.classList.add(
            s.calcStatus === "CONFIRMED" ? "alert-success" : "alert-warning"
        );

    } catch (e) {
        console.warn("status refresh failed", e);
    }
}

// ===============================
// 4. 공통 함수들
// ===============================

function numberFormatRaw(n) {
    if (n == null) return "";
    return n.toString().replace(/\B(?=(\d{3})+(?!\d))/g, ",");
}

function numberFormat(n) {
    if (n == null) return "0 원";
    return Number(n).toLocaleString() + " 원";
}

function renderStatusBadge(s) {
    const cls = {
        CONFIRMED: "bg-success",
        CALCULATED: "bg-info text-dark",
        SIMULATED: "bg-warning text-dark",
        READY: "bg-secondary"
    }[s] || "bg-secondary";

    return `<span class="badge ${cls}">${s}</span>`;
}
