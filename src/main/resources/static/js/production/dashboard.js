// production/dashboard.js

/**
 * 생산관리 대시보드 JS
 * - 생산 현황 추적 차트 (계획 vs 완료 수량)
 * - 월/주/일 토글
 */

let chart = null;

// -------------------------------
// 서버 응답(trend DTO) → ApexCharts 옵션
// -------------------------------
function toChartOptions(trend) {
	const labels = trend?.labels || [];
	const planned = trend?.planned || [];
  	const completed = trend?.completed || [];

  	const prettyLabels = labels.map(l => {
		if (!l) return "-";
		if (trend.range === "day" && l.includes("-")) {
			const [, m, d] = l.split("-");
			return `${m}/${d}`;
		}
		if (trend.range === "month" && l.includes("-")) {
	      	const [y, m] = l.split("-");
	      	return `${y.slice(2)}/${m}`;
	    }
		if (trend.range === "week" && l.includes("-")) {
	      	const [, w] = l.split("-");
	      	return `${w}주`;
	    }
		return l;
	});
	
	return {
		chart: {
			type: "bar",
			height: 320,
			toolbar: {
				show: true
			}
		},
		plotOptions: {
			bar: {
				borderRadius: 6, 
				columnWidth: "45%"
			}
		},
		dataLabels: { 
			enabled: false 
		},
	    grid: { 
			strokeDashArray: 4 
		},
	    xaxis: { 
			categories: prettyLabels 
		},
	    yaxis: { 
			title: { 
				text: "수량 (EA)" 
			} 
		},
	    legend: { 
			position: "bottom" 
		},
	    series: [
			{
				name: "계획",
				data: planned
			},
			{
				name: "완료", 
				data: completed
			}
	    ],
		tooltip: {
			shared: true,
			intersect: false,
			y: {
				formatter: (val, ctx) => {
					const i = ctx.dataPointIndex;
					const planned = ctx.w.globals.series[0]?.[i] ?? 0;
			        const completed = ctx.w.globals.series[1]?.[i] ?? 0;
					
					// planned가 0이면 %는 표시 안 함
			        if (planned > 0) {
						const rate = Math.round((completed / planned) * 100);
				        // 현재 값 + 완료율 같이 보여주기
				        return `${val} (완료율 ${rate}%)`;
					}
					return `${val}`;
				}
			}
		}
	};
}

// -------------------------------
// 차트 초기 생성
// -------------------------------
function initChart(trend) {
	const options = toChartOptions(trend);
	chart = new ApexCharts(document.querySelector("#prodChart"), options);
    chart.render();
}

// -------------------------------
// 차트 갱신
// -------------------------------
function updateChart(trend) {
	if (!chart) return initChart(trend);
	
	const options = toChartOptions(trend);
	chart.updateOptions({
		xaxis: options.xaxis,
		yaxis: options.yaxis
	}, false, true);
	
	chart.updateSeries(options.series, true);
}

// -------------------------------
// 서버에서 데이터 조회
// -------------------------------
async function fetchTrend(range) {
	const base = (window.ctx || "").replace(/\/$/, ""); 
    const res = await fetch(`${base}/production/dashboard/trend?range=${encodeURIComponent(range)}`);
    if (!res.ok) throw new Error(`trend API error: ${res.status}`);
    return await res.json();
}

// -------------------------------
// 버튼 active 처리
// -------------------------------
function setActiveRangeBtn(activeBtn) {
	document.querySelectorAll("[data-range]").forEach(b => {
		b.classList.remove("btn-primary");
		b.classList.add("btn-outline-primary");
	});
	activeBtn.classList.remove("btn-outline-primary");
    activeBtn.classList.add("btn-primary");
}


// -------------------------------
// 초기화
// -------------------------------
document.addEventListener("DOMContentLoaded", async () => {
	
	// 1) 최초 렌더
	if (window.initialTrend && window.initialTrend.labels) {
		initChart(window.initialTrend);
	} else {
		const trend = await fetchTrend("day");
		initChart(trend);
	}
	
	// 2) range 버튼
	document.querySelectorAll("[data-range]").forEach(btn => {
		btn.addEventListener("click", async () => {
			try {
				setActiveRangeBtn(btn);
				
				const range = btn.getAttribute("data-range");
		        document.querySelectorAll("[data-range]").forEach(b => b.disabled = true);
			
				const trend = await fetchTrend(range);
		        updateChart(trend);
				
			} catch (e) {
				console.error(e);
		        alert("차트 데이터를 불러오지 못했습니다.");
			} finally {
				document.querySelectorAll("[data-range]").forEach(b => b.disabled = false);
			}
		});
	});
});