// lotTrace.js
// ------------------------------------------------------------
// LOT 추적 트리(좌) + 상세(우) 제어

// 전역 상태
const TraceState = {
  mode: "PRODUCT",
  treeEl: null,
  detailEl: null
};

document.addEventListener("DOMContentLoaded", () => {
  // 0) mode 읽기
  const pageEl = document.getElementById("tracePage");
  TraceState.mode = pageEl?.dataset.mode || "PRODUCT";

  // 1) 주요 DOM 캐싱
  TraceState.treeEl = document.getElementById("lotTree");
  TraceState.detailEl = document.getElementById("lotDetail");

  if (!TraceState.treeEl || !TraceState.detailEl) return;

  // 2) 트리 클릭 이벤트(이벤트 위임)
  TraceState.treeEl.addEventListener("click", onTreeClick);
  
  // 3) 검색 폼 즉시 검색
  initLotTraceSearchForm();
});

/** ============================================================
 *  이벤트 핸들러
 *  ============================================================ */
function onTreeClick(e) {
  const treeEl = TraceState.treeEl;

  const rootLink = e.target.closest(".lot-root-link");
  const groupToggle = e.target.closest(".lot-group-toggle");
  const childLink = e.target.closest(".tree-child-link");

  // 1) ROOT LOT 클릭
  if (rootLink) {
    e.preventDefault();

    const lotNo = rootLink.dataset.lotNo;
    toggleRootChildren(treeEl, rootLink);

    if (TraceState.mode === "PRODUCT") loadProductRootDetail(lotNo);
    else loadMaterialRootDetail(lotNo);

    return;
  }

  // 2) 그룹 토글 클릭(공정/자재/사용완제품 등)
  if (groupToggle) {
    e.preventDefault();

    const lotNo = groupToggle.dataset.lotNo;
    const group = groupToggle.dataset.group;

    const li = groupToggle.closest("li");
    const children = li?.querySelector(`.group-children[data-group="${group}"]`);

    if (children) children.classList.toggle("d-none");

    // 최초 1회만 로딩
    if (children && !children.dataset.loaded) {
      if (TraceState.mode === "PRODUCT") {
        if (group === "process") loadProcessList(children, lotNo);
        if (group === "material") loadMaterialList(children, lotNo);
      } else {
        // MATERIAL 모드: usedProducts 그룹만 로딩(너가 만든 구조 기준)
        if (group === "usedProducts") loadUsedProductList(children, lotNo);
      }
      children.dataset.loaded = "true";
    }

    return;
  }

  // 3) 하위 항목 클릭(공정/자재/사용완제품)
  if (childLink) {
    e.preventDefault();

    // active 표시
    treeEl.querySelectorAll(".tree-item-active")
      .forEach(el => el.classList.remove("tree-item-active"));
    childLink.classList.add("tree-item-active");

    // ---- MATERIAL 모드 전용: 사용된 완제품 클릭 ----
    if (TraceState.mode === "MATERIAL") {
      const productLotNo = childLink.dataset.productLotNo;
      if (productLotNo) {
        loadProductRootDetail(productLotNo);
      }
      // MATERIAL 모드에서는 공정상세/자재상세 클릭은 기본적으로 막아둠(혼선 방지)
      return;
    }

    // ---- PRODUCT 모드 전용: 공정 상세 ----
    const orderId = childLink.dataset.orderId;
    const stepSeq = childLink.dataset.stepSeq;
    if (orderId && stepSeq) {
      loadProcessDetail(orderId, stepSeq);
      return;
    }

    // ---- PRODUCT 모드 전용: 자재 상세 ----
    const outputLotNo = childLink.dataset.outputLotNo;
    const inputLotNo = childLink.dataset.inputLotNo;
    if (outputLotNo && inputLotNo) {
      loadMaterialDetail(outputLotNo, inputLotNo);
      return;
    }
  }
}

/** ============================================================
 *  UI 토글/패널 제어
 *  ============================================================ */
function toggleRootChildren(treeEl, rootLink) {
  const li = rootLink.closest("li.list-group-item");
  const children = li?.querySelector(".root-children");
  if (!children) return;

  // 다른 ROOT 펼침 닫기
  treeEl.querySelectorAll(".root-children").forEach(box => {
    if (box !== children) box.classList.add("d-none");
  });

  // 현재 ROOT 토글
  children.classList.toggle("d-none");
}

/** 공정/자재 패널 show/hide를 한 군데서 통제 */
function showPanel(which) {
  const proc = document.getElementById("processDetailPanel");
  const mat = document.getElementById("materialDetailPanel");

  if (proc) proc.style.display = (which === "process") ? "block" : "none";
  if (mat) mat.style.display = (which === "material") ? "block" : "none";
}

/** ============================================================
 *  fetch 헬퍼 (응답 처리 통일)
 *  ============================================================ */
async function fetchText(url) {
  const res = await fetch(url);
  if (!res.ok) throw new Error(`HTTP ${res.status} for ${url}`);
  return res.text();
}

async function fetchJson(url) {
  const res = await fetch(url);
  if (!res.ok) throw new Error(`HTTP ${res.status} for ${url}`);
  return res.json();
}

/** ============================================================
 *  ROOT 상세 로딩
 *  ============================================================ */
async function loadProductRootDetail(lotNo) {
  if (!lotNo) return;

  const detailEl = TraceState.detailEl;
  detailEl.innerHTML = "<div class='text-muted'>로딩 중...</div>";

  try {
    const html = await fetchText(`/lot/trace/detail?lotNo=${encodeURIComponent(lotNo)}`);
    detailEl.innerHTML = html;
    // ROOT 상세 로딩 시 패널은 기본 숨김(선택한 공정/자재 없으니까)
    showPanel(null);
  } catch (e) {
    console.error(e);
    detailEl.innerHTML = "<div class='text-danger'>완제품 LOT 상세 로딩 실패</div>";
  }
}

function loadMaterialRootDetail(lotNo) {
  if (!lotNo) return;
  
  const detailEl = TraceState.detailEl;

  detailEl.innerHTML = "<div class='text-muted'>로딩 중...</div>";

  fetch(`/lot/trace/material-root-detail?lotNo=${encodeURIComponent(lotNo)}`)
    .then(res => {
      // 404/500이면 여기서 잡아서 화면에 표시
      if (!res.ok) throw new Error("HTTP " + res.status);
      return res.text();
    })
    .then(html => {
      detailEl.innerHTML = html;
    })
    .catch(err => {
      console.error("material-root-detail fetch failed:", err);
      detailEl.innerHTML =
        "<div class='text-danger'>자재 LOT 상세 로딩 실패</div>";
    });
}


/** ============================================================
 *  그룹 목록 로딩 (좌측 트리)
 *  ============================================================ */
async function loadProcessList(container, lotNo) {
  if (!container) return;

  try {
    const list = await fetchJson(`/lot/trace/process-list?lotNo=${encodeURIComponent(lotNo)}`);
    container.innerHTML = "";

    if (!list || list.length === 0) {
      container.innerHTML = "<li class='small text-muted'>(공정 정보 없음)</li>";
      return;
    }

    list.forEach(p => {
      const li = document.createElement("li");
      li.className = "process-item";

      li.innerHTML = `
        <a href="#" class="tree-child-link small"
           data-order-id="${p.orderId}"
           data-step-seq="${p.stepSeq}">
          ${p.processName} / ${p.processId}
          <span class="text-muted"> (${p.status})</span>
        </a>
      `;
      container.appendChild(li);
    });
  } catch (e) {
    console.error(e);
    container.innerHTML = "<li class='small text-danger'>(공정 목록 로딩 실패)</li>";
  }
}

async function loadMaterialList(container, lotNo) {
  if (!container) return;

  try {
    const list = await fetchJson(`/lot/trace/material-list?lotNo=${encodeURIComponent(lotNo)}`);
    container.innerHTML = "";

    if (!list || list.length === 0) {
      container.innerHTML = "<li class='small text-muted'>(자재 정보 없음)</li>";
      return;
    }

    list.forEach(m => {
      const li = document.createElement("li");
      li.className = "material-item";

      li.innerHTML = `
        <a href="#" class="tree-child-link small"
           data-output-lot-no="${lotNo}"
           data-input-lot-no="${m.lotNo}">
          ${m.displayName}
          <span class="text-muted"> (${m.usedQty}${m.unit ? " " + m.unit : ""})</span>
        </a>
      `;
      container.appendChild(li);
    });
  } catch (e) {
    console.error(e);
    container.innerHTML = "<li class='small text-danger'>(자재 목록 로딩 실패)</li>";
  }
}

async function loadUsedProductList(container, inputLotNo) {
  if (!container) return;

  try {
    const list = await fetchJson(`/lot/trace/material/used-products?inputLotNo=${encodeURIComponent(inputLotNo)}`);
    container.innerHTML = "";

    if (!list || list.length === 0) {
      container.innerHTML = "<li class='small text-muted'>(사용된 완제품 LOT 없음)</li>";
      return;
    }

    list.forEach(p => {
      const li = document.createElement("li");
      li.className = "product-item";

      li.innerHTML = `
        <a href="#" class="tree-child-link small"
           data-product-lot-no="${p.lotNo}">
          ${p.lotNo} / ${p.displayName ?? "-"}
          <span class="text-muted"> (${p.usedQty ?? "-"}${p.unit ? " " + p.unit : ""})</span>
          ${p.status ? `<span class="text-muted"> / ${p.status}</span>` : ""}
        </a>
      `;
      container.appendChild(li);
    });
  } catch (e) {
    console.error(e);
    container.innerHTML = "<li class='small text-danger'>(완제품 목록 로딩 실패)</li>";
  }
}

/** ============================================================
 *  상세 로딩 (우측 패널)
 *  ============================================================ */
async function loadProcessDetail(orderId, stepSeq) {
  try {
    const detail = await fetchJson(
      `/lot/trace/process-detail?orderId=${encodeURIComponent(orderId)}&stepSeq=${encodeURIComponent(stepSeq)}`
    );
    renderProcessDetail(detail);
  } catch (e) {
    console.error(e);
    showPanel("process");
    const panel = document.getElementById("processDetailPanel");
    if (panel) panel.innerHTML = `<div class="text-danger small">공정 상세 로딩 실패</div>`;
  }
}

function renderProcessDetail(detail) {
  showPanel("process");

  document.getElementById("pd-processName").innerText = detail.processName || "-";
  document.getElementById("pd-processId").innerText = detail.processId || "-";
  document.getElementById("pd-status").innerText = detail.status || "-";
  document.getElementById("pd-deptName").innerText = detail.deptName || "-";

  document.getElementById("pd-workerId").innerText = detail.workerId || "-";
  document.getElementById("pd-workerName").innerText = detail.workerName || "-";
  document.getElementById("pd-lineId").innerText = detail.lineId || "-";

  document.getElementById("pd-startTime").innerText = formatDateTimeMin(detail.startTime);
  document.getElementById("pd-endTime").innerText = formatDateTimeMin(detail.endTime);

  document.getElementById("pd-goodQty").innerText = detail.goodQty ?? "-";
  document.getElementById("pd-defectQty").innerText = detail.defectQty ?? "-";
  document.getElementById("pd-defectRate").innerText =
    (detail.defectRate != null) ? detail.defectRate.toFixed(1) + "%" : "-";

  // QC 버튼
  const qcActions = document.getElementById("pd-qcActions");
  if (qcActions) {
    qcActions.innerHTML = "";

    if (detail.processId === "PRC-QC") {
      const btn = document.createElement("button");
      btn.type = "button";
      btn.className = "btn btn-sm btn-outline-primary";
      btn.innerText = "QC 결과 보기";

      if (!detail.qcResultId) {
        btn.disabled = true;
        btn.title = "QC 결과가 아직 등록되지 않았습니다.";
      } else {
        btn.addEventListener("click", () => openQcViewModal(detail.qcResultId));
      }
      qcActions.appendChild(btn);
    }
  }

  // 설비 목록
  const eqBox = document.getElementById("pd-equipments");
  if (eqBox) {
    eqBox.innerHTML = "";
    const eqList = detail.equipments || [];

    if (eqList.length === 0) {
      eqBox.innerHTML = `<div class="text-muted small">설비 정보 없음 (수기 검사)</div>`;
      return;
    }

    eqList.forEach(eq => {
      const row = document.createElement("div");
      row.className = "p-2 border rounded bg-white";

      const status = eq.status || "-";
      const badgeClass =
        status === "가동" ? "bg-success" :
        status === "정지" ? "bg-secondary" :
        status === "고장" ? "bg-danger" :
        status === "점검" ? "bg-warning text-dark" :
        "bg-light text-dark";

      row.innerHTML = `
        <div class="d-flex justify-content-between align-items-center">
          <div class="fw-semibold fs-6">${eq.equipName || "-"}</div>
          <span class="badge ${badgeClass}">${status}</span>
        </div>
        <div class="small text-muted mt-1">
          <span class="me-2">${eq.equipCode || "-"}</span>
          <span>${eq.koName || "-"}</span>
        </div>
        <div class="small text-muted">${eq.stdName || ""}</div>
      `;
      eqBox.appendChild(row);
    });
  }
}

async function loadMaterialDetail(outputLotNo, inputLotNo) {
  try {
    const detail = await fetchJson(
      `/lot/trace/material-detail?outputLotNo=${encodeURIComponent(outputLotNo)}&inputLotNo=${encodeURIComponent(inputLotNo)}`
    );
    renderMaterialDetail(detail);
  } catch (e) {
    console.error(e);
    alert("자재 상세 조회 중 오류가 발생했습니다.");
  }
}

function setText(id, value) {
  const el = document.getElementById(id);
  if (!el) return;                 
  el.innerText = (value ?? "-");
}

function renderMaterialDetail(d) {
  showPanel("material");

  // ===== LOT/자재 기본 =====
  setText("md-matName", d.matName);
  setText("md-matId", d.matId);
  setText("md-matType", d.matType);
  setText("md-matUnit", d.matUnit);

  setText("md-lotNo", d.lotNo);
  setText("md-lotType", d.lotType);
  setText("md-lotStatus", d.lotStatus);
  setText("md-lotCreatedDate", formatDateTimeSec(d.lotCreatedDate));

  // ===== 자재 관리 =====
  setText("md-manufactureDate", formatDate(d.manufactureDate));
  setText("md-expirationDate", formatDate(d.expirationDate));

  // ===== 재고 배지 =====
  const badge = document.getElementById("md-stockBadge");
  if (badge) {
    const amount = d.ivAmount; // 서버에서 ivAmount 주는 구조 유지
    if (amount == null || amount === 0) {
      badge.className = "badge bg-secondary";
      badge.innerText = "소진 완료";
    } else {
      badge.className = "badge bg-success";
      badge.innerText = "재고 보유";
    }
  }

  // ===== 투입 정보(완제품 기준) =====
  setText("md-usedQty", d.usedQty);
  setText("md-usedUnit", d.matUnit ? d.matUnit : "");
}


/** ============================================================
 *  날짜 포맷
 *  ============================================================ */
function formatDateTimeSec(dt) {
  if (!dt) return "-";
  return dt.replace("T", " ").split(".")[0];
}
function formatDateTimeMin(dt) {
  if (!dt) return "-";
  const s = dt.replace("T", " ").split(".")[0];
  return s.substring(0, 16);
}
function formatDate(dt) {
  if (!dt) return "-";
  return dt.split("T")[0].split(" ")[0];
}

/** ============================================================
 *  LOT 검색 폼: 즉시 검색(자동 submit)
 *  ============================================================ */
function initLotTraceSearchForm() {
  const form = document.getElementById("lotTraceSearchForm");
  if (!form) return;

  const mode = TraceState.mode || "PRODUCT"; // ✅ 여기 변경

  const keywordEl = document.getElementById("searchKeyword");
  const statusEl  = (mode === "PRODUCT") ? document.getElementById("pStatus") : document.getElementById("mStatus");
  const typeEl    = (mode === "PRODUCT") ? document.getElementById("pType")   : document.getElementById("mType");
  const btnSearch = document.getElementById("btnSearch");

  const submit = () => {
    // setPageToFirst(form); // 필요하면 활성화
    form.requestSubmit ? form.requestSubmit(btnSearch || undefined) : form.submit();
  };

  [statusEl, typeEl].forEach(el => {
    if (!el) return;
    el.addEventListener("change", submit);
  });

  if (keywordEl) {
    keywordEl.addEventListener("keydown", (e) => {
      if (e.key === "Enter") {
        e.preventDefault();
        submit();
      }
    });
  }
}