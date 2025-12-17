// qc_regist.js

let qcRegistGrid = null;
let qcRegModal = null;
let qcSaved = false;        // 저장 성공 여부
let currentOrderId = null;  // 모달이 열려있는 orderId (cancel 때 필요)
let qcDirty = false;		// 입력 변경 여부
let allRegistRows = []; 	// PENDING 전체 캐시 (필터용)

document.addEventListener("DOMContentLoaded", () => {
	
	// 모달 초기화
    const modalEl = document.getElementById("qcRegModal");
    qcRegModal = new bootstrap.Modal(modalEl);
	
	// 모달이 '저장 없이' 닫히면 QC 취소 처리
	modalEl.addEventListener("hidden.bs.modal", () => {
	  if (qcSaved) {           // 저장한 뒤 닫힌 거면 cancel 안 함
	    currentOrderId = null;
	    return;
	  }
	  if (currentOrderId) {   // 저장 없이 닫힘 -> cancel 호출
	    cancelQc(currentOrderId);
	    currentOrderId = null;
	  }
	});
	
	// 닫히기 직전: 입력이 있을 때만 confirm으로 닫힘을 막음
	modalEl.addEventListener("hide.bs.modal", (e) => {
	  if (qcSaved) return;        // 저장으로 닫히는 경우는 경고 X
	  if (!qcDirty) return;       // 입력 변경 없으면 경고 X

	  const ok = confirm("입력한 내용이 저장되지 않았습니다.\n정말 닫을까요?");
	  if (!ok) {
	    e.preventDefault();       // 여기서 닫힘 취소
	  }
	});

	document.getElementById("qcRegForm")?.addEventListener("input", () => {
	  qcDirty = true;
	});
	document.getElementById("qcRegForm")?.addEventListener("change", () => {
	  qcDirty = true;
	});
	
	// 그리드 초기화
	const gridEl = document.getElementById("qcRegistGrid");
	
	qcRegistGrid = new tui.Grid({
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
				header: 'QC_RESULT_ID',
				name: 'qcResultId',
				hidden: true
			},
			{
				header: '작업지시번호',
				name: 'orderId'
			},
			{
				header: '제품명',
				name: 'prdName'
			},
			{
				header: '지시수량',
				name: 'planQty'
			},
			{
			  header: '라인',
			  name: 'lineName'
			},
			{
			  header: '대기시간',
			  name: 'qcCreatedAt',
			  formatter: ({ value }) => {
			    if (!value) return '-';
			    const dt = new Date(value);
			    const diffMin = Math.floor((Date.now() - dt.getTime()) / 60000);
			    if (diffMin < 60) return diffMin + '분';
			    const h = Math.floor(diffMin / 60);
			    const m = diffMin % 60;
			    return `${h}시간 ${m}분`;
			  }
			},
			{
			  	header: " ",
			  	name: "btn",
			 	width: 90,
			  	align: "center",
			  	formatter: () =>
			    	"<button type='button' class='btn btn-info btn-sm'>검사 시작</button>"
			}
		]
	});
	loadQcRegistGrid();
	
	qcRegistGrid.on('click', (ev) => {
        if (ev.columnName !== "btn") return;

		const row = qcRegistGrid.getRow(ev.rowKey);
	    if (!row || !row.orderId) return;
		
        startQcAndOpenModal(row); 
    });
	
	function startQcAndOpenModal(rowData) {

	  const orderId = rowData.orderId;

	  // CSRF
	  const csrfTokenMeta  = document.querySelector('meta[name="_csrf_token"]');
	  const csrfHeaderMeta = document.querySelector('meta[name="_csrf_headerName"]');
	  const csrfToken      = csrfTokenMeta ? csrfTokenMeta.content : null;
	  const csrfHeaderName = csrfHeaderMeta ? csrfHeaderMeta.content : null;

	  fetch(`/qc/start?orderId=${encodeURIComponent(orderId)}`, {
	    method: "POST",
	    headers: {
	      ...(csrfToken && csrfHeaderName ? { [csrfHeaderName]: csrfToken } : {})
	    }
	  })
	  .then(res => {
	    if (!res.ok) throw new Error("HTTP " + res.status);
	    return res.json();
	  })
	  .then(data => {
	    if (!data.success) {
	      alert(data.message || "QC 시작 처리 실패");
	      return;
	    }
		
		qcSaved = false; 
		qcDirty = false; 
		currentOrderId = rowData.orderId;
		
	    openQcRegModal(rowData);

	    loadQcRegistGrid();
	  })
	  .catch(err => {
	    console.error(err);
	    alert("QC 시작 처리 중 오류가 발생했습니다.");
	  });
	}
	
	// 전체 판정 변경 시 FAIL 사유 활성/비활성
	const overallResultSelect = document.getElementById("overallResult");
	if (overallResultSelect) {
	  overallResultSelect.addEventListener("change", updateFailReasonState);
	  updateFailReasonState(); // 초기 1회
	}
	
	// qc 등록 저장 버튼 클릭 이벤트
	const btnSave = document.getElementById("btnQcSave");
	if (btnSave) {
		btnSave.addEventListener("click", onClickSaveQcResult);
	}
	
	// 필터 select 변경 이벤트
	document.getElementById("orderFilter")?.addEventListener("change", () => {
	  // 작업지시 선택하면 제품도 자동으로 맞춰주는 UX
	  syncProductByOrder();
	  applyRegistFilter();
	});

	document.getElementById("productFilter")?.addEventListener("change", () => {
	  applyRegistFilter();
	});

});

//FAIL 사유 활성/비활성 제어 함수
function updateFailReasonState() {
  const overallResultSelect = document.getElementById("overallResult");
  const failReasonTextarea  = document.getElementById("failReason");

  if (!overallResultSelect || !failReasonTextarea) return;

  const val = overallResultSelect.value;

  if (val === "FAIL") {
    failReasonTextarea.removeAttribute("readonly");
  } else {
    // PASS 또는 미선택: 값 지우고 읽기 전용
    failReasonTextarea.value = "";
    failReasonTextarea.setAttribute("readonly", "readonly");
  }
}

// 목록 조회
function loadQcRegistGrid() {
    fetch("/qc/regist/data")
        .then(res => res.json())
        .then(data => {
            allRegistRows = Array.isArray(data) ? data : [];
			
			// 셀렉트 옵션 채우기(처음 1회만 / 혹은 데이터 바뀔 때마다 갱신)
			fillRegistFilters(allRegistRows);
			
			// 현재 선택값 기준으로 필터 적용해서 그리드 갱신
			applyRegistFilter();
        });
}

// QC 등록 모달을 열면서 데이터 넣는 함수
function openQcRegModal(rowData) {

	// 모달 제목
	document.getElementById("qcModalTitleOrder").innerText = rowData.orderId;
	document.getElementById("qcModalTitleProductName").innerText = rowData.prdName;
	document.getElementById("qcModalTitleProductCode").innerText = `(${rowData.prdId})`;

	// 모달 상단
    document.getElementById("qcOrderIdText").innerText = rowData.orderId;
    document.getElementById("qcProductText").innerText = rowData.prdName;
    document.getElementById("qcPlanQtyText").innerText = rowData.planQty + " EA";
	document.getElementById("qcLotNoText").innerText = rowData.lotNo || "-";

    // hidden
	document.getElementById("qcResultId").value = rowData.qcResultId;
    document.getElementById("orderId").value = rowData.orderId;
	document.getElementById("qcPlanQty").value  = rowData.planQty;
	
	// 폼/테이블 초기화
	document.getElementById("qcDetailTbody").innerHTML = "";
	document.getElementById("inspectionDate").value = new Date().toISOString().substring(0, 10);
	document.getElementById("overallResult").value = "";
	document.getElementById("failReason").value = "";
	
	// FAIL 사유 필드 비활성화 + 회색 처리
	const overallResultSelect = document.getElementById("overallResult");
	const failReasonTextarea = document.getElementById("failReason");
	if (overallResultSelect && failReasonTextarea) {
	  overallResultSelect.value = "";
	  failReasonTextarea.value = "";
	  failReasonTextarea.setAttribute("readonly", "readonly"); // ✅ readonly
	}
	
	// 수량/비고 초기화
	const goodInput = document.getElementById("qcGoodQty");
	const defectInput = document.getElementById("qcDefectQty");
	const remarkInput = document.getElementById("qcRemark");

	if (goodInput)   goodInput.value = "";
	if (defectInput) defectInput.value = "";
	if (remarkInput) remarkInput.value = "";

	// 상세행 조회해서 tbody 채우기
	loadQcDetailRows(rowData.qcResultId);

	// 모달 열기
	qcRegModal.show();
	qcDirty = false; 
	
	// 모달 열릴 때 FAIL 사유 상태 동기화
	updateFailReasonState();
}

// QC 항목 상세 리스트 가져오기
function loadQcDetailRows(qcResultId) {

  fetch(`/qc/${qcResultId}/details`)
    .then((res) => {
      if (!res.ok) {
        throw new Error("HTTP " + res.status);
      }
      return res.json();
    })
    .then((data) => {
      console.log("QC 상세행:", data);
      renderQcDetailTable(data);
    })
    .catch((err) => {
      console.error("QC 상세 로딩 오류", err);
      alert("QC 상세 정보를 불러오는 중 오류가 발생했습니다.");
    });
}

// QC 항목별 입력 라인 생성
function renderQcDetailTable(detailList) {
  const tbody = document.getElementById("qcDetailTbody");
  tbody.innerHTML = "";

  detailList.forEach((row, idx) => {
    const tr = document.createElement("tr");

	tr.innerHTML = `
	  <td>
	    ${row.itemName ?? ""}
	    <input type="hidden" name="details[${idx}].qcResultDtlId" value="${row.qcResultDtlId}">
	    <input type="hidden" name="details[${idx}].qcItemId" value="${row.qcItemId}">
	  </td>
	  <td>${row.unit ?? ""}</td>
	  <td>${row.stdText ?? ""}</td>
	  <td>
	    <input type="text"
	           class="form-control form-control-sm"
	           name="details[${idx}].measureValue"
			   value="${row.measureValue ?? ""}"
               data-min="${row.minValue ?? ""}"
               data-max="${row.maxValue ?? ""}">
	  </td>
	  <td>
	    <select class="form-select form-select-sm"
	            name="details[${idx}].result">
	      <option value="PASS" ${row.result === "PASS" ? "selected" : ""}>합격</option>
	      <option value="FAIL" ${row.result === "FAIL" ? "selected" : ""}>불합격</option>
	    </select>
	  </td>
	  <td>
	    <input type="text"
	           class="form-control form-control-sm"
	           name="details[${idx}].remark"
	           value="${row.remark ?? ""}">
	  </td>
	  <td class="text-center">
      	<input type="file"
               class="form-control form-control-sm qc-file-input"
               data-dtl-id="${row.qcResultDtlId}"
               multiple>
      </td>
	`;

    tbody.appendChild(tr);
  });
}

// 상세 테이블 값
function collectDetailRowsFromTable() {
  const trs = document.querySelectorAll("#qcDetailTbody tr");
  const detailRows = [];

  trs.forEach((tr) => {
    const dtlInput    = tr.querySelector('input[name$=".qcResultDtlId"]');
    const itemInput   = tr.querySelector('input[name$=".qcItemId"]');
    const measureInput= tr.querySelector('input[name$=".measureValue"]');
    const resultSelect= tr.querySelector('select[name$=".result"]');
    const remarkInput = tr.querySelector('input[name$=".remark"]');

    const qcResultDtlId = dtlInput    ? dtlInput.value    : null;
    const qcItemId      = itemInput   ? itemInput.value   : null;
    const measureValue  = measureInput? measureInput.value: "";
    const result        = resultSelect? resultSelect.value: "";
    const remark        = remarkInput ? remarkInput.value : "";

    detailRows.push({
      qcResultDtlId,
      qcItemId,
      measureValue,
      result,
      remark
    });
  });

  // 측정값이 비어 있는 행이 하나라도 있는지 체크
  const emptyIndex = detailRows.findIndex(row =>
    !row.measureValue || row.measureValue.trim() === ""
  );

  if (emptyIndex !== -1) {
    // 어떤 행이 비었는지 포커스까지 주기
    const trs = document.querySelectorAll("#qcDetailTbody tr");
    const targetInput = trs[emptyIndex].querySelector('input[name$=".measureValue"]');
    if (targetInput) {
      targetInput.focus();
    }
    return null;  // ->  onClickSaveQcResult() 에서 알럿 뜸
  }

  return detailRows;
}

// 저장 버튼
function onClickSaveQcResult() {
    const qcResultId = document.getElementById("qcResultId").value;
    if (!qcResultId) {
      alert("QC 결과 ID가 없습니다.");
      return;
    }

    // 1) 디테일 행 수집
    const detailRows = collectDetailRowsFromTable();
    if (!detailRows) {
      alert("모든 QC 항목의 측정값을 입력해주세요.");
      return;
    }
    if (detailRows.length === 0) {
      alert("저장할 QC 항목이 없습니다.");
      return;
    }
  
  	// 2) 헤더 영역 값 읽기
    const goodQtyVal   = document.getElementById("qcGoodQty")?.value;
    const defectQtyVal = document.getElementById("qcDefectQty")?.value;
    const remark       = document.getElementById("qcRemark")?.value || "";

	const overallResultEl = document.getElementById("overallResult");
	const failReasonEl    = document.getElementById("failReason");

	const overallResult = overallResultEl ? overallResultEl.value : "";
	const failReason    = failReasonEl ? failReasonEl.value.trim() : "";
	
	const inspectionDate = document.getElementById("inspectionDate")?.value; // "2025-12-14"
	const inspectorId    = document.getElementById("inspectorId")?.value;   // 사번

	// 전체 판정 필수
	if (!overallResult) {
	  alert("전체 판정을 선택해주세요.");
	  overallResultEl?.focus();
	  return;
	}

	// FAIL인데 불합격 사유가 없으면 막기
	if (overallResult === "FAIL" && failReason === "") {
	  alert("전체 판정이 FAIL인 경우, 불합격 사유를 입력해주세요.");
	  failReasonEl?.removeAttribute("readonly");
	  failReasonEl?.focus();
	  return;
	}

	// 양품/불량 수량 필수
	if (goodQtyVal === "" || defectQtyVal === "") {
	  alert("양품 수량과 불량 수량을 모두 입력해주세요.");
	  if (goodQtyVal === "") {
	    document.getElementById("qcGoodQty")?.focus();
	  } else {
	    document.getElementById("qcDefectQty")?.focus();
	  }
	  return;
	}

	const goodQty   = Number(goodQtyVal);
	const defectQty = Number(defectQtyVal);

	if (isNaN(goodQty) || isNaN(defectQty)) {
	  alert("양품/불량 수량은 숫자만 입력 가능합니다.");
	  return;
	}
	if (goodQty < 0 || defectQty < 0) {
	  alert("양품/불량 수량은 0 이상이어야 합니다.");
	  return;
	}
	
	// good + defect = planQty 체크
	const planQtyVal = document.getElementById("qcPlanQty")?.value;
	const planQty = planQtyVal ? Number(planQtyVal) : null;

	if (planQty !== null && !isNaN(planQty)) {
	  if (goodQty + defectQty !== planQty) {
	    alert("양품 수량 + 불량 수량이 지시수량과 일치하지 않습니다.");
	    // 필요하면 여기서 return 빼고 경고만 띄우게 바꿀 수도 있어
	    return;
	  }
	}
	
	const hasDetailFail = detailRows.some(r => (r.result || "").toUpperCase() === "FAIL");

	if (overallResult === "PASS" && hasDetailFail) {
	  if (!confirm("상세 항목에 FAIL이 포함되어 있습니다.\n그래도 전체 판정을 PASS로 저장할까요?")) {
	    return;
	  }
	}
	
    // 3) 서버로 보낼 payload
    const payload = {
      qcResultId: Number(qcResultId),
	  inspectionDate: inspectionDate,
	  inspectorId: inspectorId,
	  overallResult: overallResult,
      goodQty: goodQty,
      defectQty: defectQty,
      failReason: failReason,
      remark: remark,
      detailRows: detailRows
    };
	
	const overallLabel = overallResult === "PASS" ? "합격"
	                   : overallResult === "FAIL" ? "불합격"
	                   : overallResult === "PENDING" ? "검사대기" : overallResult;
	
	// 저장 전 최종 확인
    let confirmMsg = `다음 내용으로 QC 결과를 저장하시겠습니까?\n\n`
                   + `ㆍ전체 판정 : ${overallLabel}\n`
                   + `ㆍ양품 수량 : ${goodQty}\n`
                   + `ㆍ불량 수량 : ${defectQty}\n`;

    if (overallResult === "FAIL" && failReason) {
      confirmMsg += `ㆍ불합격 사유 : ${failReason}\n`;
    }
    confirmMsg += `\n저장 후에는 수정이 어려울 수 있습니다.`;

    if (!confirm(confirmMsg)) {
      // 사용자가 "취소" 누르면 저장 중단
      return;
    }

    // 4) CSRF 토큰
    const csrfTokenMeta  = document.querySelector('meta[name="_csrf_token"]');
    const csrfHeaderMeta = document.querySelector('meta[name="_csrf_headerName"]');

    const csrfToken     = csrfTokenMeta ? csrfTokenMeta.content : null;
    const csrfHeaderName = csrfHeaderMeta ? csrfHeaderMeta.content : null;

    // 5) fetch 호출 
    fetch(`/qc/${qcResultId}/save`, {
      method: "POST",
      headers: {
        "Content-Type": "application/json",
        ...(csrfToken && csrfHeaderName ? { [csrfHeaderName]: csrfToken } : {})
      },
      body: JSON.stringify(payload)
    })
      .then((res) => {
        if (!res.ok) {
          throw new Error("HTTP " + res.status);
        }
        return res.json();
      })
      .then((data) => {
        console.log("QC 저장 결과:", data);

        if (data.success) {
          alert(data.message || "QC 검사 결과가 저장되었습니다.");
		  
		  qcSaved = true;
		  qcDirty = false;
          // 모달 닫고 목록 새로고침
          qcRegModal.hide();
          loadQcRegistGrid();
        } else {
          alert(data.message || "QC 저장 중 오류가 발생했습니다.");
        }
      })
      .catch((err) => {
        console.error("QC 저장 오류:", err);
        alert("QC 저장 중 오류가 발생했습니다.");
      });
  }
  
  // 측정값 입력 시 자동 PASS/FAIL 판정
  const qcDetailTbody = document.getElementById("qcDetailTbody");

  if (qcDetailTbody) {
    qcDetailTbody.addEventListener("input", (e) => {
      // 측정값 input이 아닐 경우 무시
      const input = e.target;
      if (!input.matches('input[name$=".measureValue"]')) return;

      const row = input.closest("tr");
      if (!row) return;

      const select = row.querySelector('select[name$=".result"]');
      if (!select) return;

      const raw = input.value;
      const minAttr = input.dataset.min;
      const maxAttr = input.dataset.max;

      // min/max 둘 다 없으면 자동판정 대상 아님
      if (!minAttr && !maxAttr) {
        return;
      }

      if (!raw) {
        return;
      }

      const v = Number(raw);
      if (Number.isNaN(v)) {
        // 숫자 아니면 FAIL으로
        select.value = "FAIL";
        return;
      }

      let pass = true;

      if (minAttr && !Number.isNaN(Number(minAttr)) && v < Number(minAttr)) {
        pass = false;
      }
      if (maxAttr && !Number.isNaN(Number(maxAttr)) && v > Number(maxAttr)) {
        pass = false;
      }

      select.value = pass ? "PASS" : "FAIL";
    });
  }

  // 파일첨부
  function uploadQcDetailFiles(qcResultDtlId, files) {

    const formData = new FormData();
    // 백엔드에서 @RequestParam("qcDetailFiles") 로 받을 값
    for (let i = 0; i < files.length; i++) {
      formData.append("qcDetailFiles", files[i]);
    }
	
	const csrfTokenMeta  = document.querySelector('meta[name="_csrf_token"]');
	const csrfHeaderMeta = document.querySelector('meta[name="_csrf_headerName"]');

	const csrfToken      = csrfTokenMeta ? csrfTokenMeta.content : null;
	const csrfHeaderName = csrfHeaderMeta ? csrfHeaderMeta.content : null;

    fetch(`/qc/detail/${qcResultDtlId}/files`, {
      method: "POST",
	  headers: {
	      ...(csrfToken && csrfHeaderName ? { [csrfHeaderName]: csrfToken } : {})
    	},
      body: formData
    })
      .then(res => {
        if (!res.ok) {
          throw new Error("HTTP " + res.status);
        }
        return res.json();
      })
//      .then(data => {
//        alert(data.msg || "첨부파일이 업로드되었습니다.");
//      })
      .catch(err => {
        console.error("파일 업로드 오류:", err);
        alert("첨부파일 업로드 중 오류가 발생했습니다.");
      });
  }
  
  // 파일 선택 시 자동 업로드
  document.addEventListener("change", (e) => {
    const input = e.target;
    if (!input.matches(".qc-file-input")) return;   

    const qcResultDtlId = input.dataset.dtlId;
    const files = input.files;

    if (!qcResultDtlId || !files || files.length === 0) {
      return;
    }

    uploadQcDetailFiles(qcResultDtlId, files);  
  });

  // 검사 시작 후 저장 아닌 취소 눌렀을 경우
  function cancelQc(orderId) {
    // CSRF
    const csrfTokenMeta  = document.querySelector('meta[name="_csrf_token"]');
    const csrfHeaderMeta = document.querySelector('meta[name="_csrf_headerName"]');
    const csrfToken      = csrfTokenMeta ? csrfTokenMeta.content : null;
    const csrfHeaderName = csrfHeaderMeta ? csrfHeaderMeta.content : null;

    fetch(`/qc/cancel?orderId=${encodeURIComponent(orderId)}`, {
      method: "POST",
      headers: {
        ...(csrfToken && csrfHeaderName ? { [csrfHeaderName]: csrfToken } : {})
      }
    })
    .then(res => {
      if (!res.ok) throw new Error("HTTP " + res.status);
      return res.json();
    })
    .then(data => {
      if (!data.success) {
        console.warn("QC cancel 실패:", data.message);
      }
      loadQcRegistGrid();
    })
    .catch(err => {
      console.error("QC cancel 오류:", err);
    });
  }

  // -------------------------------------------------------------------------
  // 셀렉트 옵션 채우기
  function fillRegistFilters(rows) {
    const orderSel = document.getElementById("orderFilter");
    const prodSel  = document.getElementById("productFilter");
    if (!orderSel || !prodSel) return;

    const prevOrder = orderSel.value || "ALL";
    const prevProd  = prodSel.value || "ALL";

    // 작업지시(중복 제거)
    const orderIds = [...new Set(rows.map(r => r.orderId).filter(Boolean))];

    orderSel.innerHTML =
      `<option value="ALL">전체 작업지시</option>` +
      orderIds.map(id => `<option value="${id}">${id}</option>`).join("");

    // 제품(중복 제거: prdId 기준)
    const prodMap = new Map();
    rows.forEach(r => {
      if (r.prdId && !prodMap.has(r.prdId)) {
        prodMap.set(r.prdId, { prdId: r.prdId, prdName: r.prdName || "" });
      }
    });

    prodSel.innerHTML =
      `<option value="ALL">전체 제품</option>` +
      [...prodMap.values()]
        .map(p => `<option value="${p.prdId}">${p.prdName} (${p.prdId})</option>`)
        .join("");

    // 기존 선택 유지(없으면 ALL)
    orderSel.value = orderIds.includes(prevOrder) ? prevOrder : "ALL";
    prodSel.value  = prodMap.has(prevProd) ? prevProd : "ALL";
  }
  
  // 필터 적용해서 그리드 갱신
  function applyRegistFilter() {
    const orderSel = document.getElementById("orderFilter");
    const prodSel  = document.getElementById("productFilter");
    if (!orderSel || !prodSel) {
      qcRegistGrid.resetData(allRegistRows);
      return;
    }

    const orderId = orderSel.value;
    const prdId   = prodSel.value;

    const filtered = allRegistRows.filter(r => {
      const okOrder = (orderId === "ALL") || (r.orderId === orderId);
      const okProd  = (prdId === "ALL")   || (r.prdId === prdId);
      return okOrder && okProd;
    });

    qcRegistGrid.resetData(filtered);
  }

  function syncProductByOrder() {
    const orderSel = document.getElementById("orderFilter");
    const prodSel  = document.getElementById("productFilter");
    if (!orderSel || !prodSel) return;

    const orderId = orderSel.value;
    if (orderId === "ALL") return;

    // 선택된 작업지시의 제품을 찾아서 제품 select를 자동으로 맞춤
    const row = allRegistRows.find(r => r.orderId === orderId);
    if (row && row.prdId) {
      prodSel.value = row.prdId;
    }
  }
  
  // 필터 초기화 버튼
  document.getElementById("btnResetFilter")?.addEventListener("click", (e) => {
    const orderSel = document.getElementById("orderFilter");
    const prodSel  = document.getElementById("productFilter");

    if (orderSel) orderSel.value = "ALL";
    if (prodSel)  prodSel.value  = "ALL";

    applyRegistFilter();

    e.target.blur();
  });


