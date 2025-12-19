// dashboard.js

/**
 * 생산관리 대시보드 JS
 * - 생산 현황 추적 차트 (계획 vs 완료 수량)
 * - 월/주/일 토글
 */

// -------------------------------
// KPI 카드 클릭
// -------------------------------
document.addEventListener("DOMContentLoaded", () => {
	document.querySelectorAll(".kpi-card[data-link]").forEach(card => {
		// 마우스 클릭
		card.addEventListener("click", (e) => {
			// 카드 안에 있는 a 태그 클릭이면 a가 이동하게 두기
	        if (e.target.closest("a")) return;

	        const url = card.getAttribute("data-link");
	        if (url) window.location.href = url;
		});
		
		// 키보드 Enter/Space (role=button, tabindex=0 쓰는 경우 필수)
	    card.addEventListener("keydown", (e) => {
			if (e.key === "Enter" || e.key === " ") {
				e.preventDefault();
		        const url = card.getAttribute("data-link");
		        if (url) window.location.href = url;
			}
		});
	});
});


// -------------------------------
// 즉시 조치 리스트 툴팁
// -------------------------------
document.addEventListener("DOMContentLoaded", function() {
	const tooltipTriggerList = [].slice.call (
		document.querySelectorAll('[data-bs-toggle="tooltip"]')
	);
	tooltipTriggerList.forEach(function (tooltipTriggerEl) {
		new bootstrap.Tooltip(tooltipTriggerEl);
	});
});


// -------------------------------
// 분 -> 사람이 읽기 쉬운 시간 포맷
// -------------------------------
function formatMinutes(min) {
  if (min == null || min <= 0) return "0분";

  if (min < 60) {
    return min + "분";
  }

  const h = Math.floor(min / 60);
  const m = min % 60;

  return m === 0 ? `${h}시간` : `${h}시간 ${m}분`;
}

// -------------------------------
// 마지막 업데이트 시간 표시
// -------------------------------
function updateLastUpdatedAt(date = new Date()) {
  const el = document.getElementById("lastUpdatedAt");
  if (!el) return;

  const hh = String(date.getHours()).padStart(2, "0");
  const mm = String(date.getMinutes()).padStart(2, "0");
  const ss = String(date.getSeconds()).padStart(2, "0");

  el.textContent = `${hh}:${mm}`;
}

// -------------------------------
// 라인 히트맵: 초기 포맷 + 실시간 증가(1분마다)
// -------------------------------
document.addEventListener("DOMContentLoaded", () => {

  // 1) 초기 1회: "1164분" -> "19시간 24분"
  document.querySelectorAll(".rack .big-mid").forEach(el => {
    const stay = Number(el.dataset.stay);
    const active = Number(el.dataset.active);

    const current = (el.textContent || "").trim();
    if (current.includes("QC") || current.includes("대기") || current.includes("시작대기")) return;

    if (active > 0 && !Number.isNaN(stay)) {
      el.textContent = formatMinutes(stay);
    }
  });

  // 2) 1분 tick
  function tickHeatmapMinutes() {
    document.querySelectorAll(".rack .big-mid").forEach(el => {
      const active = Number(el.dataset.active);
      if (!(active > 0)) return;

      let stay = Number(el.dataset.stay);

      // QC대기 n분 같은 케이스를 대비한 파싱
      if (Number.isNaN(stay)) {
        const m = (el.textContent || "").match(/(\d+)\s*분/);
        stay = m ? Number(m[1]) : NaN;
      }
      if (Number.isNaN(stay)) return;

      stay += 1;
      el.dataset.stay = String(stay);

      const current = (el.textContent || "").trim();
      if (current.startsWith("QC대기")) {
        el.textContent = `QC대기 ${stay}분`;
      } else {
        el.textContent = formatMinutes(stay);
      }
    });

    updateLastUpdatedAt(); // tick 1회마다 갱신
  }

  updateLastUpdatedAt();                 // 최초 1회
  setInterval(tickHeatmapMinutes, 60_000);
});
