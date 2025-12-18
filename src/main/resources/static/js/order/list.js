document.addEventListener("DOMContentLoaded", () => {

  const gridEl = document.getElementById("workOrderGrid");
  if (!gridEl) {
    console.error("workOrderGrid 요소 없음!");
    return;
  }

  // 1) Grid 생성
  workOrderGrid = new tui.Grid ({
    el: gridEl,
    bodyHeight: 450,
    rowHeaders: ["rowNum"],
    scrollX: false,
    scrollY: true,
    pageOptions: {
      useClient: true,
      perPage: 10
    },
    columnOptions: {resizable: true},
    columns: [
      {header: "작업지시번호", name: "orderId", width: 160, align: "center"},
      {header: "품번", name: "prdId", align: "center"},
      {header: "품목군", name: "prdName", align: "left"},
      {header: "계획수량", name: "planQty", align: "right"},
      {
        header: "시작시간",
        name: "startDate",
        align: "center",
        formatter: ({ value }) => formatDate(value)
      },
      {
        header: "종료시간",
        name: "endDate",
        align: "center",
        formatter: ({ value }) => formatDate(value)
      },
      {
        header: "상태",
        name: "status",
        align: "center",
        formatter: ({ value }) => formatStatusBadge(value)
      },
      {header: "출고여부", name: "outboundYn", align: "center"},
      {
        header: "상세",
        name: "btn",
        width: 90,
        align: "center",
        formatter: () => "<button class='btn btn-primary btn-sm grid-detail'>상세</button>"
      }
    ]
  });

  // 2) 검색 (검색버튼 클릭, 엔터키, 셀렉트박스 체인지)
  
  document.getElementById("btnSearchOrder").addEventListener("click", () => {
      loadWorkOrderGrid();
  });
  
  document.getElementById("searchKeyword").addEventListener("keydown", (event) => {
		if (event.key === "Enter") {
			loadWorkOrderGrid();
		}
  })
  
  document.getElementById("searchStatus").addEventListener("change", () => {
	loadWorkOrderGrid();
  });

  // 3) 페이지 로드 시 전체 조회
  loadWorkOrderGrid();

  // 4) 상세 버튼 클릭 이벤트
  workOrderGrid.on("click", (event) => {
    if (event.columnName !== "btn") return;
    const row = workOrderGrid.getRow(event.rowKey);
    openDetailModal(row.orderId);
  });

});

// ======================================================
// 작업지시 목록 조회
// ======================================================
function loadWorkOrderGrid(){
    const raw = document.getElementById("searchKeyword").value;

    const keyword = raw
        .trim()                 // 양쪽 공백 제거
        .replace(/\s+/g, " ")   // 여러 칸 띄어쓰기 → 하나로
        .toLowerCase();         // 소문자 통일
    const status  = document.getElementById("searchStatus").value  || "";
    const startDateFrom   = document.getElementById("startDateFrom").value || "";

  const query = new URLSearchParams({
    keyword,
    status,
  startDateFrom
  });

  fetch("/order/list/data?" + query.toString())
          .then(res => {
            if (!res.ok) throw new Error("HTTP " + res.status);
            return res.json();
          })
          .then(data => {
            console.log("작업지시 목록 : ", data);
            workOrderGrid.resetData(data);
          })
          .catch(err => {
            console.error("작업지시 목록 조회 오류", err);
            alert("조회 중 오류 발생");
          });

}