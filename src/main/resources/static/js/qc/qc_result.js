// qc_result.js

let qcResultGrid = null;
let qcViewModal = null;

document.addEventListener("DOMContentLoaded", () => {
	
	// 모달 초기화
	const modalEl = document.getElementById("qcViewModal");
	qcViewModal = new bootstrap.Modal(modalEl);
	
	// 그리드 초기화
	const gridEl = document.getElementById("qcResultGrid");
	
	qcResultGrid = new tui.Grid({
		el: gridEl,
		rowHeaders: ["rowNum"],
		scrollX: true,
		scrollY: true,
		columnOptions: {
			resizable: true
	    },
		pageOptions: {
			useClient: true,         
			perPage: 10            
		},
		columns: [
			{
				header: 'QC 결과ID',
				name: 'qcResultId'
			},
			{
				header: '작업지시번호',
				name: 'orderId'
			},
			{
				header: '제품코드',
				name: 'prdId'
			},
			{
				header: '제품명',
				name: 'prdName'
			},
			{
				header: '검사일자',
				name: 'inspectionDate'
			},
			{
				header: '상태',
				name: 'overallResult',
				formatter: ({ value }) => {
				    switch (value) {
				      case "PASS":
				        return "합격";
				      case "FAIL":
				        return "불합격";
				      default:
				        return value || "-";
				    }
			    }
			},
			{
				header: '불합격사유',
				name: 'failReason'
			},
			{
			  	header: " ",
			  	name: "btn",
			 	width: 90,
			  	align: "center",
			  	formatter: () =>
			    	"<button type='button' class='btn btn-info btn-sm'>상세</button>"
			}
		]
	});
	loadQcResultGrid();
	
	qcResultGrid.on('click', (ev) => {
        if (ev.columnName !== "btn") return;

		const row = qcResultGrid.getRow(ev.rowKey);
	    if (!row || !row.qcResultId) return;
		
        openQcViewModal(row.qcResultId);
    });
});

// 목록 조회
function loadQcResultGrid() {
    fetch("/qc/result/data")
        .then(res => res.json())
        .then(data => {
            qcResultGrid.resetData(data);
        });
}

// 모달을 열면서 데이터 넣는 함수
function openQcViewModal(qcResultId) {

  fetch(`/qc/result/${qcResultId}`)
    .then(res => res.json())
    .then(data => {

      // ===== 타이틀 영역 =====
      document.getElementById("qcViewTitleOrder").innerText        = data.orderId;
      document.getElementById("qcViewTitleProductName").innerText  = data.productName || "";
      document.getElementById("qcViewTitleProductCode").innerText  = data.productCode || "";

      // ===== 기본 작업 정보 =====
      document.getElementById("qcViewOrderIdText").innerText = data.orderId;

      // 제품명 (코드 포함)
      document.getElementById("qcViewProductText").innerText =
        `${data.productName || ""} (${data.productCode || ""})`;

      document.getElementById("qcViewPlanQtyText").innerText =
        (data.planQty ?? "") + " EA";

      document.getElementById("qcViewLotNoText").innerText =
        data.lotNo || "-";

      // ===== 검사/수량 정보 =====
	  document.getElementById("qcViewInspectionDateText").innerText =
	    data.inspectionDate || "";

	  document.getElementById("qcViewInspectorNameText").innerText =
	    data.inspectorName || "";

	  document.getElementById("qcViewInspectionQtyText").innerText =
	    data.inspectionQty ?? "";

	  document.getElementById("qcViewGoodQtyText").innerText =
	    data.goodQty ?? "";

	  document.getElementById("qcViewDefectQtyText").innerText =
	    data.defectQty ?? "";

		// ===== 전체 판정 / 사유 / 비고 =====
		const overall = data.overallResult || "-";
		const overallBadge = document.getElementById("qcViewOverallResultBadge");

		// 텍스트 세팅
		overallBadge.textContent = overall;

		// 클래스 초기화 후 상태별 색 입히기
		overallBadge.className = "badge w-100 py-2 fs-6 text-center";

		if (overall === "PASS") {
		  overallBadge.classList.add("bg-success");
		} else if (overall === "FAIL") {
		  overallBadge.classList.add("bg-danger");
		} else {
		  overallBadge.classList.add("bg-secondary");
		}

		// 불합격 사유 / 비고
		const failReasonEl      = document.getElementById("qcViewFailReason");
		const failReasonWrapper = failReasonEl.closest(".col-md-5");
		const remarkEl          = document.getElementById("qcViewRemark");
		const remarkWrapper     = remarkEl.closest(".col-md-4");

		// 값 세팅
		failReasonEl.value = data.failReason || "";
		remarkEl.value     = data.remark || "";

		// PASS면 불합격 사유 영역 숨기기
		if (overall === "FAIL") {
		  failReasonWrapper.style.display = "";
		} else {
		  failReasonWrapper.style.display = "none";
		}

		// 비고가 없으면 비고 영역 숨기기
		if (data.remark && data.remark.trim() !== "") {
		  remarkWrapper.style.display = "";
		} else {
		  remarkWrapper.style.display = "none";
		}


      // ===== 디테일 리스트 =====
      const tbody = document.getElementById("qcViewDetailTbody");
      tbody.innerHTML = "";

      (data.details || []).forEach(row => {
        const badge =
          row.result === "PASS"
            ? "<span class='badge bg-success'>PASS</span>"
            : row.result === "FAIL"
              ? "<span class='badge bg-danger'>FAIL</span>"
              : "";

        const tr = `
          <tr>
            <td>${row.itemName || ""}</td>
            <td>${row.unit || ""}</td>
            <td>${row.stdText || ""}</td>
            <td>${row.measureValue || ""}</td>
            <td>${badge}</td>
            <td>${row.remark || ""}</td>
          </tr>
        `;

        tbody.insertAdjacentHTML("beforeend", tr);
      });

      qcViewModal.show();
    })
    .catch(() => alert("QC 결과 조회 중 오류가 발생했습니다."));
}
