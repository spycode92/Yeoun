// processDone.js (완료 처리 탭 전용)
let doneGrid = null;

document.addEventListener("DOMContentLoaded", () => {
  const gridEl = document.getElementById("doneGrid");
  if (!gridEl) return;

  doneGrid = new tui.Grid({
    el: gridEl,
    bodyHeight: 400,
    rowHeaders: ["rowNum"],
    scrollX: true,
    scrollY: true,
    columnOptions: { resizable: true },
    pageOptions: { useClient: true, perPage: 10 },
    columns: [
      { 
		header: "작업지시번호", 
		name: "orderId", 
		width: 150 
	  },
      { 
		header: "라인", 
		name: "lineName" 
	  },
      { 
		header: "제품명", 
		name: "prdName" 
	  },
      { 
		header: "계획수량", 
		name: "planQty" 
	  },
      { 
		header: "양품", 
		name: "goodQty" 
	  },
      { 
		header: "불량", 
		name: "defectQty" 
	  },
      {
        header: "처리결과",
        name: "status",
        align: "center",
        formatter: ({ value }) => {
          if (value === "COMPLETED") return "<span class='badge bg-success'>완료</span>";
          if (value === "SCRAPPED")  return "<span class='badge bg-danger'>폐기</span>";
          return value || "-";
        }
      },
	  { 
		header: "완료일시", 
		name: "doneTime", 
		width: 160,
		formatter: ({ value }) =>
		    value ? value.replace("T", " ").substring(0, 16) : "-"
	  },
      { 
		header: "경과시간", 
		name: "elapsedTime" 
	  },
      {
        header: " ",
        name: "btn",
        width: 90,
        align: "center",
        formatter: () => "<button type='button' class='btn btn-outline-info btn-sm'>상세</button>"
      }
    ]
  });
  
  // 엔터 submit 방지 + 엔터 검색
  document.getElementById("doneSearchForm")
    ?.addEventListener("submit", (e) => {
      e.preventDefault();
      loadDoneGrid();
    });
  
  // 검색 버튼
  document.getElementById("btnSearchDone")?.addEventListener("click", (e) => {
    e.currentTarget.blur();       // 버튼 눌림색 제거
    loadDoneGrid();
  });
  
  // 처리결과 select 바뀌면 바로 검색
  document.getElementById("doneStatus")?.addEventListener("change", () => {
    loadDoneGrid();
  });

  // 초기화 버튼
  document.getElementById("btnResetDone")?.addEventListener("click", (e) => {
	e.currentTarget.blur(); 
	
    const dateEl = document.getElementById("doneDate");
    const kwEl   = document.getElementById("doneKeyword");
    const stEl   = document.getElementById("doneStatus");

    if (dateEl) dateEl.value = "";
    if (kwEl)   kwEl.value = "";
    if (stEl)   stEl.value = "";
	
    loadDoneGrid(); // 초기화 후 재조회
  });
  
  loadDoneGrid();

  doneGrid.on("click", (ev) => {
	if (ev.columnName !== "btn") return;

   	const row = doneGrid.getRow(ev.rowKey);
   	if (!row || !row.orderId) return;

   	openDetailModal(row.orderId);
  });
});

function loadDoneGrid() {
  const workDate = document.getElementById("doneDate")?.value || "";
  const searchKeyword = document.getElementById("doneKeyword")?.value || "";
  const status   = document.getElementById("doneStatus")?.value || "";

  const params = new URLSearchParams();
  if (workDate) params.append("workDate", workDate);
  if (searchKeyword) params.append("searchKeyword", searchKeyword);
  if (status)   params.append("doneStatus", status);

  fetch("/process/status/done/data?" + params.toString())
    .then(res => {
      if (!res.ok) throw new Error("HTTP " + res.status);
      return res.json();
    })
    .then(data => doneGrid?.resetData(data))
    .catch(err => {
      console.error(err);
      alert("완료 데이터를 불러오는 중 오류가 발생했습니다.");
    });
}
