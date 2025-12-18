// processStatus.js
// 공정 현황 목록 + 상세 모달 + 공정 단계 시작/종료/메모 저장

let processGrid = null; // 전역 그리드 변수

// -------------------------------
// 페이지 로드 시 실행
// -------------------------------
document.addEventListener("DOMContentLoaded", () => {

  const gridEl = document.getElementById("processGrid");

  if (!gridEl) return;
  
  if (!window.tui || !tui.Grid) {
    console.error("Toast UI Grid 스크립트가 로드되지 않았습니다.");
    return;
  }

  // 1) 그리드 생성
  processGrid = new tui.Grid({
    el: gridEl,
    bodyHeight: 400,
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
        header: "작업지시번호",
        name: "orderId",
        align: "center",
        width: 150
      },
      {
        header: "라인",
        name: "lineName",
        align: "center",
        width: 130
      },
      {
        header: "제품명",
        name: "prdName",
        align: "center"
      },
      {
        header: "계획수량",
        name: "planQty",
        align: "right"
      },
	  {
	    header: "계획기간",
	    name: "planPeriod",
	    align: "center",
	    width: 200,
	    formatter: ({ row }) => formatPlanPeriod(row.planStartDate, row.planEndDate)
	  },
	  {
	    header: "상태",
	    name: "status",
	    align: "center",
		formatter: ({ value }) => {
		  switch (value) {
		    case "IN_PROGRESS":
		      return `<span class="badge bg-primary">진행중</span>`;
		    case "RELEASED":
		      return `<span class="badge bg-secondary">대기</span>`;
		    default:
		      return `<span class="badge bg-light text-dark">-</span>`;
		  }
		}
	  },
      {
        header: "현재공정",
        name: "currentProcess",
        align: "center"
      },
	  {
	    header: "공정 진행도",
	    name: "progressRate",
	    align: "center",
	    formatter: ({ value }) => {
	      const percent = value ?? 0;
	      return `
	        <div style="width:100%; background:#eee; height:12px; border-radius:8px; overflow:hidden;">
	          <div style="
	            width:${percent}%; 
	            height:100%; 
	            background:#00c8a2;
	            transition: width .4s;
	          "></div>
	        </div>
	        <div style="font-size:11px; margin-top:2px;">${percent}%</div>
	      `;
	    }
	  },
      {
        header: "경과시간",
        name: "elapsedTime",
        align: "center"
      },
      {
        header: " ",
        name: "btn",
        width: 90,
        align: "center",
        formatter: () =>
          "<button type='button' class='btn btn-outline-info btn-sm'>상세</button>"
      }
    ]
  });

  // 2) 검색/초기화 이벤트 바인딩
  const btnSearch = document.getElementById("btnSearchProcess");
  const btnReset  = document.getElementById("btnResetProcess");
  
  // 셀렉트는 변경 즉시 검색 (현재공정/상태)
  const selProcess = document.getElementById("searchProcess");
  const selStatus  = document.getElementById("searchHStatus");

  // 디바운스(연속 변경 시 호출 난사 방지)
  const debounce = (fn, delay = 250) => {
    let t = null;
    return (...args) => {
      clearTimeout(t);
      t = setTimeout(() => fn(...args), delay);
    };
  };
  
  const autoSearch = debounce(() => loadProcessGrid(), 250);

  selProcess?.addEventListener("change", autoSearch);
  selStatus?.addEventListener("change", autoSearch);
  document.getElementById("workDate")?.addEventListener("change", autoSearch);

  btnSearch?.addEventListener("click", (e) => {
    e.preventDefault();
    e.currentTarget.blur();
    loadProcessGrid();
  });
  
  // 검색어 엔터로 검색
  document.getElementById("searchKeyword")?.addEventListener("keydown", (e) => {
    if (e.key === "Enter") {
      e.preventDefault();
      loadProcessGrid();
    }
  });

  btnReset?.addEventListener("click", (e) => {
    e.preventDefault();
    e.currentTarget.blur();

    const workDate      = document.getElementById("workDate");
    const searchProcess = document.getElementById("searchProcess");
    const searchHStatus = document.getElementById("searchHStatus");
    const searchKeyword = document.getElementById("searchKeyword");

    if (workDate) workDate.value = "";
    if (searchProcess) searchProcess.value = "";
    if (searchHStatus) searchHStatus.value = "";
    if (searchKeyword) searchKeyword.value = "";

    loadProcessGrid();
  });

  // 3) 페이지 처음 들어올 때 전체 목록 조회
  loadProcessGrid();

  // 4) 그리드 내 '상세' 버튼 클릭 이벤트
  processGrid.on("click", (ev) => {
    if (ev.columnName !== "btn") return;

    const row = processGrid.getRow(ev.rowKey);
    if (!row || !row.orderId) return;

    openDetailModal(row.orderId);
  });
});

// -------------------------------
// 공정현황 목록 조회
// -------------------------------
function loadProcessGrid() {
  const workDate      = document.getElementById("workDate")?.value || "";
  const searchProcess = document.getElementById("searchProcess")?.value || "";
  const searchHStatus = document.getElementById("searchHStatus")?.value || "";
  const searchKeyword = document.getElementById("searchKeyword")?.value || "";
  
  const params = new URLSearchParams();

  if (workDate)      params.append("workDate", workDate);             // LocalDate workDate
  if (searchProcess) params.append("searchProcess", searchProcess);   // String processName
  if (searchHStatus) params.append("searchHStatus", searchHStatus);   // String status
  if (searchKeyword) params.append("searchKeyword", searchKeyword);   // String keyword

  fetch("/process/status/data?" + params.toString())
    .then(async (res) => {
      if (!res.ok) {
        const text = await res.text();
        console.error("HTTP", res.status, "response:", text);
        throw new Error("HTTP " + res.status);
      }
      return res.json();
    })
    .then((data) => {
      console.log("공정현황 목록:", data);
      processGrid?.resetData(data);
    })
    .catch((err) => {
      console.error("공정현황 데이터 로딩 중 오류", err);
      alert("공정현황 데이터를 불러오는 중 오류가 발생했습니다.");
    });
}

// 상세 요약 + 단계 테이블 렌더링 공통 함수
function renderProcessDetail(detail) {
  const summary = detail.wop;
  const steps   = detail.steps || [];

  // 1) 상단 요약
  const summaryEl = document.getElementById("summaryGrid");
  summaryEl.innerHTML = `
    <div class="col-md-3">
      <div class="text-muted">작업지시번호</div>
      <div class="fw-semibold">${summary.orderId}</div>
    </div>
    <div class="col-md-3">
      <div class="text-muted">제품명</div>
      <div class="fw-semibold">${summary.prdName}</div>
    </div>
    <div class="col-md-2">
      <div class="text-muted">품번</div>
      <div class="fw-semibold">${summary.prdId}</div>
    </div>
    <div class="col-md-2">
      <div class="text-muted">라인</div>
      <div class="fw-semibold">${summary.lineName}</div>
    </div>
    <div class="col-md-2">
      <div class="text-muted">계획수량</div>
      <div class="fw-semibold">${summary.planQty}</div>
    </div>
  `;

  // 2) 공정 단계 테이블
  const tbody = document.querySelector("#stepTable tbody");
  tbody.innerHTML = "";

  if (steps.length === 0) {
    tbody.innerHTML = `
      <tr>
        <td colspan="10" class="text-center text-muted py-4">
          공정 단계 정보가 없습니다.
        </td>
      </tr>
    `;
    return;
  }

  steps.forEach((step) => {
    const tr = document.createElement("tr");

    // 상태 뱃지
	let statusBadge = "-";
	if (step.status === "READY") {
	  statusBadge = `<span class="badge bg-label-secondary">대기</span>`;
	} else if (step.status === "IN_PROGRESS") {
	  statusBadge = `<span class="badge bg-label-warning text-dark">진행중</span>`;
	} else if (step.status === "DONE") {
	  statusBadge = `<span class="badge bg-label-success">완료</span>`;
	} else if (step.status === "QC_PENDING") {
	  statusBadge = `<span class="badge bg-label-info text-dark">QC 대기</span>`;
  	} else if (step.status === "SKIPPED") {
      statusBadge = `<span class="badge bg-secondary">생략</span>`;
    }
	
	// 예상/지연 표시
    const expectedText =
      (step.expectedMinutes != null && step.expectedMinutes > 0)
        ? `${step.expectedMinutes}분`
        : "";

    const delayedHtml =
      (step.delayed === true)
        ? `<span class="badge bg-danger">지연</span>`
        : `<span class="text-muted"></span>`;

	let workBtnHtml = "";

	if (step.status === "SKIPPED") {
	  workBtnHtml = `<span class="text-muted">생략</span>`;
	} else if (step.processId === "PRC-QC") {
	  if (step.status === "DONE") {
	    workBtnHtml = `<span class="text-muted">완료</span>`;
	  } else if (step.status === "QC_PENDING") {
	    workBtnHtml = `
	      <button type="button"
	              class="btn btn-outline-info btn-sm btn-qc-regist"
	              data-order-id="${summary.orderId}">
	        QC 등록
	      </button>
	    `;
	  } else {
	    workBtnHtml = `<span class="text-muted">대기</span>`;
	  }
	} else {
	  if (step.canStart) {
	    workBtnHtml += `
	      <button type="button"
	              class="btn btn-primary btn-sm btn-step-start"
	              data-order-id="${summary.orderId}"
	              data-step-seq="${step.stepSeq}">
	        시작
	      </button>
	    `;
	  }
	  if (step.canFinish) {
	    workBtnHtml += `
	      <button type="button"
	              class="btn btn-success btn-sm btn-step-finish ms-1"
	              data-order-id="${summary.orderId}"
	              data-step-seq="${step.stepSeq}"
	              data-standard-qty="${step.standardQty ?? ''}"
	              data-plan-qty="${step.planQty}">
	        종료
	      </button>
	    `;
	  }

	  if (!step.canStart && !step.canFinish) {
	    if (step.status === "DONE")      workBtnHtml = `<span class="text-muted">완료</span>`;
	    else if (step.status === "READY") workBtnHtml = `<span class="text-muted">대기</span>`;
	    else if (step.status === "QC_PENDING") workBtnHtml = `<span class="text-muted">QC 대기</span>`; 
	    else workBtnHtml = "-";
	  }
	}


    const memoInputHtml = `
      <input type="text"
             class="form-control form-control-sm step-memo-input"
             data-order-id="${summary.orderId}"
             data-step-seq="${step.stepSeq}"
             value="${step.memo ? step.memo : ""}">
    `;
	
	const goodQtyText   = (step.goodQty   ?? "") === "" ? "-" : step.goodQty;
	const defectQtyText = (step.defectQty ?? "") === "" ? "-" : step.defectQty;

    tr.innerHTML = `
      <td>${step.stepSeq}</td>
      <td>
        <span class="badge rounded-pill bg-label-primary">
          ${step.processName}
        </span>
      </td>
      <td>${statusBadge}</td>
      <td>${formatDateTime(step.startTime)}</td>
      <td>${formatDateTime(step.endTime)}</td>
	  <td class="text-end">${expectedText}</td>
      <td class="text-center">${delayedHtml}</td>
	  <td class="text-end">${goodQtyText}</td>
	  <td class="text-end">${defectQtyText}</td>
      <td>${workBtnHtml}</td>
      <td>${memoInputHtml}</td>
    `;

    tbody.appendChild(tr);
  });
}


// -------------------------------
// 상세 모달 열기
// -------------------------------
function openDetailModal(orderId) {
  const modalEl = document.getElementById("detailModal");
  const modal   = bootstrap.Modal.getOrCreateInstance(modalEl);

  // 일단 모달부터 띄우고(overlay DOM 보장)
  modal.show();

  // 이전 내용 대충 비우기(선택)
  document.getElementById("summaryGrid").innerHTML = "";
  document.querySelector("#stepTable tbody").innerHTML =
    `<tr><td colspan="11" class="text-center text-muted py-4">로딩 중...</td></tr>`;

  showDetailOverlay("상세 불러오는 중...");

  fetch(`/process/status/detail/${orderId}`)
    .then((res) => {
      if (!res.ok) throw new Error("HTTP " + res.status);
      return res.json();
    })
    .then((data) => {
      renderProcessDetail(data);
    })
    .catch((err) => {
      console.error("공정 상세 조회 오류", err);
      alert("공정 상세 정보를 불러오는 중 오류가 발생했습니다.");
      modal.hide();
    })
    .finally(() => {
      hideDetailOverlay();
    });
}


// -------------------------------
// 공정 단계 시작/종료/메모 이벤트
// -------------------------------
// 시작/종료/QC등록 버튼 공통 이벤트 (이벤트 위임)
document.addEventListener("click", (e) => {

  const startBtn  = e.target.closest(".btn-step-start");
  const finishBtn = e.target.closest(".btn-step-finish");
  const qcBtn     = e.target.closest(".btn-qc-regist");

  if (startBtn) {
    const orderId = startBtn.dataset.orderId;
    const stepSeq = startBtn.dataset.stepSeq;
    handleStartStep(orderId, stepSeq);
    return;
  }

  if (finishBtn) {
    const orderId  = finishBtn.dataset.orderId;
    const stepSeq  = finishBtn.dataset.stepSeq;
    const planQty  = finishBtn.dataset.planQty || "";
    const standard = finishBtn.dataset.standardQty || "";

    // hidden input 세팅
    document.getElementById("finish-orderId").value  = orderId;
    document.getElementById("finish-stepSeq").value  = stepSeq;
    document.getElementById("finish-goodQty").value  = "";
    document.getElementById("finish-defectQty").value = "";

    // 계획 수량 표시
    const planInput = document.getElementById("finish-planQty");
    if (planInput) planInput.value = planQty ? planQty + " EA" : "-";

    // 기준값 라벨/도움말/값 세팅
    const labelEl  = document.getElementById("finish-standardLabel");
    const helpEl   = document.getElementById("finish-standardHelp");
    const stdInput = document.getElementById("finish-standardQty");

    const stepNum = parseInt(stepSeq, 10);

    if (!isNaN(stepNum) && stepNum <= 2) {
      if (labelEl) labelEl.textContent = "기준 배합량 (표준)";
      if (helpEl)  helpEl.textContent  = "BOM과 계획수량을 기준으로 계산된 이론 배합량입니다.";
      stdInput.value = standard ? standard + " ml" : "-";
    } else {
      if (labelEl) labelEl.textContent = "기준 완제품 수량 (이론)";
      if (helpEl)  helpEl.textContent  = "계획수량 기준으로 표시되는 표준 기준값(이론)입니다. (손실 반영 X)";
      stdInput.value = standard ? standard + " EA" : "-";
    }

    // 모달 오픈
    const modal = new bootstrap.Modal(document.getElementById("finishModal"));
    modal.show();
    return;
  }

  if (qcBtn) {
    window.location.href = "/qc/regist";
  }
});


// 메모 change 시 저장
document.addEventListener("change", (e) => {
  if (e.target.classList.contains("step-memo-input")) {
    const input   = e.target;
    const orderId = input.dataset.orderId;
    const stepSeq = input.dataset.stepSeq;
    const memo    = input.value;
    handleSaveStepMemo(orderId, stepSeq, memo, input);
  }
});

// -------------------------------
// 공정 시작
// -------------------------------
function handleStartStep(orderId, stepSeq) {
  if (!confirm('해당 공정을 시작 처리하시겠습니까?')) return;

  showDetailOverlay("공정 시작 처리 중...");

  const headers = { 'Content-Type': 'application/json' };
  if (typeof csrfHeader !== 'undefined' && typeof csrfToken !== 'undefined') {
    headers[csrfHeader] = csrfToken;
  }

  fetch('/process/status/step/start', {
    method: 'POST',
    headers,
    body: JSON.stringify({ orderId, stepSeq })
  })
    .then(res => res.json())
    .then(result => {
      if (!result.success) {
        alert(result.message || '공정 시작 처리 중 오류가 발생했습니다.');
        return;
      }

      alert(result.message || '공정을 시작 처리했습니다.');

      // ★ detail 전체로 모달 다시 렌더링
      const detail = result.detail;
      if (detail) renderProcessDetail(detail);
    })
    .catch(err => {
      console.error('공정 시작 처리 오류', err);
      alert('공정 시작 처리 중 오류가 발생했습니다.');
    })
    .finally(() => {
      hideDetailOverlay();
    });
}


// -------------------------------
// 공정 종료
// -------------------------------
const btnFinishSubmit = document.getElementById("btnFinishSubmit");
if (btnFinishSubmit) {
	btnFinishSubmit.addEventListener("click", () => {

	    const orderId  = document.getElementById("finish-orderId").value;
	    const stepSeq  = document.getElementById("finish-stepSeq").value;
	    const goodQty  = document.getElementById("finish-goodQty").value;
	    const defectQty = document.getElementById("finish-defectQty").value;

	    if (goodQty === "" || defectQty === "") {
	        alert("양품/불량 수량을 모두 입력해주세요.");
	        return;
	    }
    	// API 호출
    	finishStepWithQty(orderId, stepSeq, goodQty, defectQty);
	});
}

function finishStepWithQty(orderId, stepSeq, goodQty, defectQty) {
  
  const headers = { 'Content-Type': 'application/json' };
  if (typeof csrfHeader !== 'undefined') headers[csrfHeader] = csrfToken;

  fetch('/process/status/step/finish', {
    method: 'POST',
    headers: headers,
    body: JSON.stringify({
      orderId,
      stepSeq,
      goodQty,
      defectQty
    })
  })
    .then(res => res.json())
    .then(result => {
      if (!result.success) {
        alert(result.message || '공정 종료 처리 중 오류');
        return;
      }

      alert("공정이 종료되었습니다.");
      
      // 모달 닫기
      const modal = bootstrap.Modal.getInstance(document.getElementById("finishModal"));
      modal.hide();

      const detail = result.detail;
      if (detail) renderProcessDetail(detail);
    })
    .catch(err => {
      console.error(err);
      alert("공정 종료 처리 중 오류가 발생했습니다.");
    });
}

// -------------------------------
// 메모 저장
// -------------------------------
function handleSaveStepMemo(orderId, stepSeq, memo, inputEl) {
  const headers = {
    'Content-Type': 'application/json'
  };
  if (typeof csrfHeader !== 'undefined' && typeof csrfToken !== 'undefined') {
    headers[csrfHeader] = csrfToken;
  }

  fetch('/process/status/step/memo', {
    method: 'POST',
    headers: headers,
    body: JSON.stringify({ orderId, stepSeq, memo })
  })
    .then(res => res.json())
    .then(result => {
      if (!result.success) {
        alert(result.message || '메모 저장 중 오류가 발생했습니다.');
        return;
      }
      console.log('메모 저장 완료');
    })
    .catch(err => {
      console.error('메모 저장 오류', err);
      alert('메모 저장 중 오류가 발생했습니다.');
    });
}

// -------------------------------
// 모달 안에서 해당 row 갱신
// -------------------------------
function updateStepRowInModal(updatedStep) {
  const tbody = document.querySelector("#stepTable tbody");
  if (!tbody) return;

  const rows = Array.from(tbody.querySelectorAll("tr"));
  const targetRow = rows.find((tr) => {
    const seqCell = tr.querySelector("td:first-child");
    return seqCell && seqCell.textContent.trim() === String(updatedStep.stepSeq);
  });

  if (!targetRow) return;

  // 상태 뱃지
  let statusBadge = "-";
  if (updatedStep.status === "READY") {
    statusBadge = `<span class="badge bg-label-secondary">대기</span>`;
  } else if (updatedStep.status === "IN_PROGRESS") {
    statusBadge = `<span class="badge bg-label-warning text-dark">진행중</span>`; 
  } else if (updatedStep.status === "DONE") {
    statusBadge = `<span class="badge bg-label-success">완료</span>`;
  } else if (updatedStep.status === "SKIPPED") {
	statusBadge = `<span class="badge bg-secondary">생략</span>`;
  }
  
  // 예상/지연 표시
  const expectedText =
    (updatedStep.expectedMinutes != null && updatedStep.expectedMinutes > 0)
      ? `${updatedStep.expectedMinutes}분`
      : "-";

  const delayedHtml =
    (updatedStep.delayed === true)
      ? `<span class="badge bg-danger">지연</span>`
      : `<span class="text-muted">-</span>`;

  // 버튼
  let workBtnHtml = "";

  if (updatedStep.status === "SKIPPED") {
    workBtnHtml = `<span class="text-muted">생략</span>`;
  // QC 공정일 때
  } else if (updatedStep.processId === "PRC-QC") {

    if (updatedStep.status === "DONE") {
      workBtnHtml = '<span class="text-muted">완료</span>';

    } else if (updatedStep.canStart) {
      workBtnHtml = `
        <button type="button"
                class="btn btn-outline-info btn-sm btn-qc-regist"
                data-order-id="${updatedStep.orderId}">
          QC 등록
        </button>
      `;
    } else {
      workBtnHtml = '<span class="text-muted">등록 전</span>';
    }

  } else {
    // 나머지 공정은 기존 로직 유지
    if (updatedStep.canStart) {
      workBtnHtml += `
        <button type="button"
                class="btn btn-primary btn-sm btn-step-start"
                data-order-id="${updatedStep.orderId}"
                data-step-seq="${updatedStep.stepSeq}">
          시작
        </button>
      `;
    }
    if (updatedStep.canFinish) {
      workBtnHtml += `
        <button type="button"
                class="btn btn-success btn-sm btn-step-finish ms-1"
                data-order-id="${updatedStep.orderId}"
                data-step-seq="${updatedStep.stepSeq}"
				data-standard-qty="${updatedStep.standardQty ?? ''}"
				data-plan-qty="${updatedStep.planQty}">
          종료
        </button>
      `;
    }
	if (!updatedStep.canStart && !updatedStep.canFinish) {
	  if (updatedStep.status === 'DONE') {
	    workBtnHtml = '<span class="text-muted">완료</span>';
	  } else if (updatedStep.status === 'READY') {
	    workBtnHtml = '<span class="text-muted">대기</span>';
	  } else {
	    workBtnHtml = '-';
	  }
	}
  }

  const memoInputHtml = `
    <input type="text"
           class="form-control form-control-sm step-memo-input"
           data-order-id="${updatedStep.orderId}"
           data-step-seq="${updatedStep.stepSeq}"
           value="${updatedStep.memo ? updatedStep.memo : ""}">
  `;

  const goodQtyText   = (updatedStep.goodQty   ?? "") === "" ? "-" : updatedStep.goodQty;
  const defectQtyText = (updatedStep.defectQty ?? "") === "" ? "-" : updatedStep.defectQty;

  targetRow.innerHTML = `
    <td>${updatedStep.stepSeq}</td>
    <td>${updatedStep.processId}</td>
    <td>
      <span class="badge rounded-pill bg-label-primary">
        ${updatedStep.processName}
      </span>
    </td>
    <td>${statusBadge}</td>
    <td>${formatDateTime(updatedStep.startTime)}</td>
    <td>${formatDateTime(updatedStep.endTime)}</td>
	<td class="text-end">${expectedText}</td>
    <td class="text-center">${delayedHtml}</td>
	<td class="text-end">${goodQtyText}</td>
    <td class="text-end">${defectQtyText}</td>
    <td>${workBtnHtml}</td>
    <td>${memoInputHtml}</td>
  `;
}


// ===============================================================================
// 모달 닫힘 버그
// ESC 우선순위: finishModal이 떠있으면 finish부터 닫고, detail로 안 넘어가게 막기
document.addEventListener("keydown", (e) => {
  if (e.key !== "Escape") return;

  const finishEl = document.getElementById("finishModal");
  if (!finishEl) return;

  // finish 모달이 실제로 열려있는지 체크
  if (finishEl.classList.contains("show")) {
    e.preventDefault();
    e.stopPropagation();

    const inst = bootstrap.Modal.getInstance(finishEl)
      || bootstrap.Modal.getOrCreateInstance(finishEl);

    inst.hide();
  }
}, true); 

const detailEl = document.getElementById("detailModal");
const finishEl = document.getElementById("finishModal");

// finish 닫힌 뒤 detail 상태 복구
if (finishEl && detailEl) {
  finishEl.addEventListener("hidden.bs.modal", () => {
    const detailShown = detailEl.classList.contains("show");

    // 1) detail이 열려있는데 body modal-open이 풀려버린 경우 복구
    if (detailShown) {
      document.body.classList.add("modal-open");

      // 2) backdrop 정리: detail이 열려있으면 backdrop은 "1개"만 남겨야 함
      const backs = Array.from(document.querySelectorAll(".modal-backdrop"));
      if (backs.length > 1) {
        // 마지막(가장 위) 하나만 남기고 제거
        backs.slice(0, backs.length - 1).forEach(b => b.remove());
      }

      // 3) 포커스 detail로 복구 (ESC/키보드 이벤트 정상화)
      detailEl.focus();
    } else {
      // 4) 혹시 둘 다 닫혔는데 backdrop만 남으면 전체 리셋(화면 멈춤 방지)
      normalizeModalState();
    }
  });

  // 어떤 모달이든 닫힌 후 "남은 backdrop / modal-open" 정리
  function normalizeModalState() {
    const anyModalShown = document.querySelector(".modal.show");
    if (!anyModalShown) {
      document.querySelectorAll(".modal-backdrop").forEach(b => b.remove());
      document.body.classList.remove("modal-open");
      document.body.style.removeProperty("padding-right");
    }
  }
  document.addEventListener("hidden.bs.modal", normalizeModalState);
}

// 페이지 오버레이
function showDetailOverlay(msg = "처리 중...") {
  const el = document.getElementById("detailLoadingOverlay");
  if (!el) return;
  const txt = el.querySelector(".text-muted");
  if (txt) txt.textContent = msg;
  el.classList.remove("d-none");
}

function hideDetailOverlay() {
  const el = document.getElementById("detailLoadingOverlay");
  if (!el) return;
  el.classList.add("d-none");
}

// -------------------------------
// 날짜 포맷
// -------------------------------
function formatDateTime(dt) {
  if (!dt) return "-";
  return dt.replace("T", " ").substring(0, 16);
}

function formatPlanPeriod(start, end) {
  if (!start || !end) return "-";

  const s = start.replace("T", " ").substring(5, 16);
  const e = end.replace("T", " ").substring(5, 16);

  // 같은 날이면 종료 날짜 제거
  if (s.substring(0,5) === e.substring(0,5)) {
    return `${s} ~ ${e.substring(6)}`;
  }
  return `${s} ~ ${e}`;
}