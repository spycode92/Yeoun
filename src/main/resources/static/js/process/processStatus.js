// processStatus.js
// 공정 현황 목록 + 상세 모달 + 공정 단계 시작/종료/메모 저장

let processGrid = null; // 전역 그리드 변수

// -------------------------------
// 페이지 로드 시 실행
// -------------------------------
document.addEventListener("DOMContentLoaded", () => {

  const gridEl = document.getElementById("processGrid");

  if (!gridEl) {
    console.error("processGrid 요소를 찾을 수 없습니다.");
    return;
  }

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
        header: "제품코드",
        name: "prdId",
        align: "center"
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
        header: "양품수량",
        name: "goodQty",
        align: "right"
      },
	  {
	    header: "진행률",
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
        header: "현재공정",
        name: "currentProcess",
        align: "center"
      },
	  {
	    header: "상태",
	    name: "status",
	    align: "center",
	    formatter: ({ value }) => {
	      switch (value) {
	        case "IN_PROGRESS":
	          return "진행중";
	        case "RELEASED":
	          return "확정";
	        case "DONE":
	          return "완료";
	        case "CANCELLED":
	          return "취소";
	        default:
	          return value || "-";
	      }
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
          "<button type='button' class='btn btn-info btn-sm'>상세</button>"
      }
    ]
  });

  // 2) 검색 버튼 클릭 시 조회
  const btnSearch = document.getElementById("btnSearchProcess");
  if (btnSearch) {
    btnSearch.addEventListener("click", () => {
      loadProcessGrid();
    });
  }

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
    .then((res) => {
      if (!res.ok) {
        throw new Error("HTTP " + res.status);
      }
      return res.json();
    })
    .then((data) => {
      console.log("공정현황 목록:", data);
      if (processGrid) {
        processGrid.resetData(data);
      }
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
    <div class="col-md-3">
      <div class="text-muted">품번</div>
      <div class="fw-semibold">${summary.prdId}</div>
    </div>
    <div class="col-md-3">
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
    }

    // 버튼
    let workBtnHtml = "";

    if (step.processId === "PRC-QC") {
      // QC 공정
      if (step.status === "DONE") {
        workBtnHtml = '<span class="text-muted">완료</span>';
      } else if (step.canStart) {
        workBtnHtml = `
          <button type="button"
                  class="btn btn-outline-info btn-sm btn-qc-regist"
                  data-order-id="${summary.orderId}">
            QC 등록
          </button>
        `;
      } else {
        workBtnHtml = '<span class="text-muted">대기</span>';
      }
    } else {
      // 일반 공정
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
                  data-step-seq="${step.stepSeq}">
            종료
          </button>
        `;
      }
      if (!step.canStart && !step.canFinish) {
        if (step.status === "DONE") {
          workBtnHtml = '<span class="text-muted">완료</span>';
        } else if (step.status === "READY") {
          workBtnHtml = '<span class="text-muted">대기</span>';
        } else {
          workBtnHtml = "-";
        }
      }
    }

    const memoInputHtml = `
      <input type="text"
             class="form-control form-control-sm step-memo-input"
             data-order-id="${summary.orderId}"
             data-step-seq="${step.stepSeq}"
             value="${step.memo ? step.memo : ""}">
    `;

    tr.innerHTML = `
      <td>${step.stepSeq}</td>
      <td>${step.processId}</td>
      <td>
        <span class="badge rounded-pill bg-label-primary">
          ${step.processName}
        </span>
      </td>
      <td>${statusBadge}</td>
      <td>${formatDateTime(step.startTime)}</td>
      <td>${formatDateTime(step.endTime)}</td>
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
  fetch(`/process/status/detail/${orderId}`)
    .then((res) => {
      if (!res.ok) throw new Error("HTTP " + res.status);
      return res.json();
    })
    .then((data) => {
      console.log("공정 상세 데이터:", data);

      // ★ 공통 렌더 함수 사용
      renderProcessDetail(data);

      // 모달 띄우기
      const modalEl = document.getElementById("detailModal");
      const modal   = new bootstrap.Modal(modalEl);
      modal.show();
    })
    .catch((err) => {
      console.error("공정 상세 조회 오류", err);
      alert("공정 상세 정보를 불러오는 중 오류가 발생했습니다.");
    });
}


// -------------------------------
// 공정 단계 시작/종료/메모 이벤트
// -------------------------------
// 시작/종료/QC등록 버튼 공통 이벤트 (이벤트 위임)
document.addEventListener("click", (e) => {
  if (e.target.classList.contains("btn-step-start")) {
    const btn     = e.target;
    const orderId = btn.dataset.orderId;
    const stepSeq = btn.dataset.stepSeq;

    handleStartStep(orderId, stepSeq);
  }

  if (e.target.classList.contains("btn-step-finish")) {
    const btn     = e.target;
    const orderId = btn.dataset.orderId;
    const stepSeq = btn.dataset.stepSeq;

    handleFinishStep(orderId, stepSeq);
  }

  // QC 등록 버튼
  if (e.target.classList.contains("btn-qc-regist")) {
    const btn     = e.target;
    const orderId = btn.dataset.orderId; // 지금은 안 쓰지만, 나중에 파라미터로 넘길 수 있음

    // 그냥 QC 등록 목록으로 이동
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
  if (!confirm('해당 공정을 시작 처리하시겠습니까?')) {
    return;
  }

  const headers = { 'Content-Type': 'application/json' };
  if (typeof csrfHeader !== 'undefined' && typeof csrfToken !== 'undefined') {
    headers[csrfHeader] = csrfToken;
  }

  fetch('/process/status/step/start', {
    method: 'POST',
    headers: headers,
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
      if (detail) {
        renderProcessDetail(detail);
      }
    })
    .catch(err => {
      console.error('공정 시작 처리 오류', err);
      alert('공정 시작 처리 중 오류가 발생했습니다.');
    });
}

// -------------------------------
// 공정 종료
// -------------------------------
function handleFinishStep(orderId, stepSeq) {
  if (!confirm('해당 공정을 종료 처리하시겠습니까?')) {
    return;
  }

  const headers = { 'Content-Type': 'application/json' };
  if (typeof csrfHeader !== 'undefined' && typeof csrfToken !== 'undefined') {
    headers[csrfHeader] = csrfToken;
  }

  fetch('/process/status/step/finish', {
    method: 'POST',
    headers: headers,
    body: JSON.stringify({ orderId, stepSeq })
  })
    .then(res => res.json())
    .then(result => {
      if (!result.success) {
        alert(result.message || '공정 종료 처리 중 오류가 발생했습니다.');
        return;
      }

      alert(result.message || '공정을 종료 처리했습니다.');

      const detail = result.detail;
      if (detail) {
        renderProcessDetail(detail);
      }
    })
    .catch(err => {
      console.error('공정 종료 처리 오류', err);
      alert('공정 종료 처리 중 오류가 발생했습니다.');
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
  }

  // 버튼
  let workBtnHtml = "";

  // QC 공정일 때
  if (updatedStep.processId === "PRC-QC") {

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
      workBtnHtml = '<span class="text-muted">대기</span>';
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
                data-step-seq="${updatedStep.stepSeq}">
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
    <td>${workBtnHtml}</td>
    <td>${memoInputHtml}</td>
  `;
}

// -------------------------------
// 날짜 포맷
// -------------------------------
function formatDateTime(dt) {
  if (!dt) return "-";
  return dt.replace("T", " ").split(".")[0];
}
