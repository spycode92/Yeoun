// production/dashboard.js

/**
 * 생산관리 대시보드 JS
 * - 생산 현황 추적 차트 (계획 vs 완료 수량)
 * - 월/주/일 토글
 */

let chart = null;
// production/dashboard.js

let orderChart = null;
let itemTrendChart = null;

// =========================
// 0) 유틸: 숫자 올림
// =========================
function niceCeil(n) {
    if (!n || n <= 0) return 5;
    const padded = Math.ceil(n * 1.1);
    const digits = Math.floor(Math.log10(padded));
    const base = Math.pow(10, digits);
    const steps = [1, 2, 5, 10];
    for (const s of steps) {
        const candidate = s * base;
        if (candidate >= padded) return candidate;
    }
    return 10 * base;
}

// =========================
// 1) 날짜 유틸
// =========================
function pad2(v) { return String(v).padStart(2, '0'); }

function fmtDayKey(d) {
    return `${d.getFullYear()}-${pad2(d.getMonth() + 1)}-${pad2(d.getDate())}`;
}

function fmtMonthKey(d) {
    return `${d.getFullYear()}-${pad2(d.getMonth() + 1)}`;
}

// =========================
// 2) ISO 주차 유틸
// =========================
function isoWeekKey(date) {
    const d = new Date(Date.UTC(date.getFullYear(), date.getMonth(), date.getDate()));
    const dayNum = d.getUTCDay() || 7;
    d.setUTCDate(d.getUTCDate() + 4 - dayNum);
    const yearStart = new Date(Date.UTC(d.getUTCFullYear(), 0, 1));
    const weekNo = Math.ceil((((d - yearStart) / 86400000) + 1) / 7);
    const weekYear = d.getUTCFullYear();
    return `${weekYear}-${pad2(weekNo)}`;
}

function isoWeekStart(date) {
    const d = new Date(date.getFullYear(), date.getMonth(), date.getDate());
    const day = d.getDay();
    const diffToMon = (day === 0 ? -6 : 1) - day;
    d.setDate(d.getDate() + diffToMon);
    d.setHours(0, 0, 0, 0);
    return d;
}

function weekLabelFromDate(date) {
    const mon = isoWeekStart(date);
    const month = mon.getMonth() + 1;
    const weekOfMonth = Math.floor((mon.getDate() - 1) / 7) + 1;
    return `${month}월 ${weekOfMonth}주차`;
}

// =========================
// 3) 기간 키 생성: 월4 / 주4 / 일5
// =========================
function lastNDayKeys(n) {
    const keys = [];
    const today = new Date();
    today.setHours(0, 0, 0, 0);

    for (let i = n - 1; i >= 0; i--) {
        const d = new Date(today);
        d.setDate(d.getDate() - i);
        keys.push({ key: fmtDayKey(d), label: `${pad2(d.getMonth() + 1)}/${pad2(d.getDate())}` });
    }
    return keys;
}

function lastNMonthKeys(n) {
    const keys = [];
    const now = new Date();
    now.setDate(1);
    now.setHours(0, 0, 0, 0);

    for (let i = n - 1; i >= 0; i--) {
        const d = new Date(now);
        d.setMonth(d.getMonth() - i);
        keys.push({ key: fmtMonthKey(d), label: `${d.getFullYear()}.${pad2(d.getMonth() + 1)}` });
    }
    return keys;
}

function lastNWeekKeys(n) {
    const keys = [];
    const today = new Date();
    today.setHours(0, 0, 0, 0);

    const thisMon = isoWeekStart(today);

    for (let i = n - 1; i >= 0; i--) {
        const d = new Date(thisMon);
        d.setDate(d.getDate() - (i * 7));
        keys.push({ key: isoWeekKey(d), label: weekLabelFromDate(d) });
    }
    return keys;
}

// =========================
// 4) 서버 데이터
// =========================
function normalizeSeries(range, rawData) {
    let frame;
    if (range === 'MONTH') frame = lastNMonthKeys(4);
    else if (range === 'WEEK') frame = lastNWeekKeys(4);
    else frame = lastNDayKeys(5);

    const map = new Map();
    (rawData || []).forEach(r => {
        map.set(r.period, {
            plan: Number(r.planCnt || 0),
            order: Number(r.orderCnt || 0)
        });
    });

    const categories = frame.map(x => x.label);
    const planSeries = frame.map(x => map.get(x.key)?.plan ?? 0);
    const orderSeries = frame.map(x => map.get(x.key)?.order ?? 0);
    const yMax = niceCeil(Math.max(...planSeries, ...orderSeries, 0));

    return { categories, planSeries, orderSeries, yMax };
}

// =========================
// 5) 데이터 로딩
// =========================
async function loadTrendData(range) {
    const response = await fetch(`/production/orderChart/data?range=${range}`);
    if (!response.ok) return [];
    return await response.json();
}

// =========================
// 6) range 적용
// =========================
let currentRange = 'WEEK';

async function applyRange(range) {
    currentRange = range;
    setRange(range);

    // 차트가 아직 없으면 리턴
    if (!orderChart) return;

    const raw = await loadTrendData(range);
    const { categories, planSeries, orderSeries, yMax } = normalizeSeries(range, raw);

    await orderChart.updateOptions({
        xaxis: { categories },
        yaxis: { min: 0, max: yMax }
    }, false, true);

    await orderChart.updateSeries([
        { data: planSeries },
        { data: orderSeries }
    ], true);
}

// =========================
// 7) 최초 렌더
// =========================
document.addEventListener("DOMContentLoaded", async () => {
    renderItemQtyChart();

    const raw = await loadTrendData(currentRange);
    const init = normalizeSeries(currentRange, raw);

    const options = {
        chart: { height: 320, type: 'line', toolbar: { show: false }, fontFamily: 'Pretendard, sans-serif' },
        series: [
            { name: '생산계획', type: 'column', data: init.planSeries },
            { name: '작업지시', type: 'line', data: init.orderSeries }
        ],
        stroke: { width: [0, 3], curve: 'smooth' },
        plotOptions: { bar: { columnWidth: '45%', borderRadius: 4 } },
        markers: { size: 4, strokeWidth: 2 },
        colors: ['#c7c9f7', '#6a5acd'],
        xaxis: { categories: init.categories },
        yaxis: { min: 0, max: init.yMax },
        legend: {
            fontSize: '13px',
            fontWeight: 600,
            markers: {radius: 6},
            position: 'top',
            horizontalAlign: 'right'
        },
        tooltip: {
            shared: true,
            intersect: false,
            style: { fontSize: '13px' }
        },
        grid: { borderColor: '#eaeaea', strokeDashArray: 4 }
    };

    const el = document.querySelector("#orderChart");
    if (!el) {
        console.error("#orderChart element not found");
        return;
    }

    orderChart = new ApexCharts(el, options);
    await orderChart.render();

    // 버튼 연결
    document.getElementById('btnDay')?.addEventListener('click', () => applyRange('DAY'));
    document.getElementById('btnWeek')?.addEventListener('click', () => applyRange('WEEK'));
    document.getElementById('btnMonth')?.addEventListener('click', () => applyRange('MONTH'));
});

function setRange(range) {
    const map = { DAY: 'btnDay', WEEK: 'btnWeek', MONTH: 'btnMonth' };

    // 1) 모든 btn-check 해제
    document.querySelectorAll('input.btn-check[name="range"]').forEach(el => {
        el.checked = false;
    });

    // 2) 선택한 것만 체크
    const id = map[range];
    const target = document.getElementById(id);
    if (target) target.checked = true;

    // 3) (선택) label active 강제 정리 — Bootstrap이 알아서 하지만 안전빵
    document.querySelectorAll('label[for]').forEach(lb => lb.classList.remove('active'));
    const label = document.querySelector(`label[for="${id}"]`);
    if (label) label.classList.add('active');
}

// =========================
// 품목별 계획 vs 작업지시 수량 차트
// =========================

async function loadItemQtyData() {
    const res = await fetch('/production/itemOrderChart/data');
    if (!res.ok) {
        console.error('품목별 수량 데이터 로딩 실패');
        return [];
    }
    return await res.json();
}

async function renderItemQtyChart() {
    const raw = await loadItemQtyData();

    const itemMeta = raw.map(r => ({
        itemId: r.itemId,
        itemName: r.itemName
    }));

    const categories = raw.map(r => r.itemName);
    const planSeries = raw.map(r => Number(r.planCnt || 0));
    const orderSeries = raw.map(r => Number(r.orderCnt || 0));

    const yMax = niceCeil(Math.max(...planSeries, ...orderSeries, 0));

    const options = {
        chart: {
            type: 'bar',
            height: 320,
            toolbar: { show: false },
            fontFamily: 'Pretendard, sans-serif',
            events: {
                dataPointSelection: function (event, chartContext, config) {
                    const idx = config.dataPointIndex;
                    if (idx >= 0) onSelectItem(itemMeta[idx]);
                },
                click: function (event, chartContext, config) {
                    if (config.dataPointIndex === -1 && config.globals?.categoryLabels) {
                        const idx = config.globals.categoryLabels.indexOf(
                            event.target.innerText
                        );
                        if (idx >= 0) onSelectItem(itemMeta[idx]);
                    }
                }
            }
        },
        series: [
            { name: '생산계획 수량', data: planSeries },
            { name: '작업지시 수량', data: orderSeries }
        ],
        plotOptions: {
            bar: {
                columnWidth: '40%',
                borderRadius: 4
            }
        },
        colors: ['#c7c9f7', '#6a5acd'],
        xaxis: {
            categories,
            labels: {
                rotate: -20,
                style: {
                    fontSize: '13px',
                    fontWeight: 500,
                    colors: '#6b7280'
                },
                formatter: function (value, index) {
                    return value;
                }
            }
        },
        yaxis: {
            min: 0,
            max: yMax,
            style: {
                fontSize: '13px',
                fontWeight: 500,
                colors: '#6b7280'
            },
        },
        tooltip: {
            shared: true,
            intersect: false,
            style: { fontSize: '13px' }
        },
        legend: {
            fontSize: '13px',
            fontWeight: 600,
            markers: {radius: 6},
            position: 'top',
            horizontalAlign: 'right'
        },
        grid: {
            borderColor: '#eaeaea',
            strokeDashArray: 4
        }
    };

    const chart = new ApexCharts(
        document.querySelector('#itemOrderChart'),
        options
    );

    chart.render();
}

function onSelectItem(item) {
    console.log('선택 품목:', item);

    // 1) 제목 표시
    document.getElementById('selectedItemTitle').innerText =
        `선택 품목: ${item.itemName}`;

    // 2) 하단 섹션 표시
    document.getElementById('itemTrendSection').classList.remove('d-none');

    // 최초 1회만 차트 생성
    if (!itemTrendChart) {
        const options = {
            chart: {
                type: 'line',
                height: 280,
                toolbar: { show: false },
                fontFamily: 'Pretendard, sans-serif'
            },
            series: [
                { name: '생산계획', data: [] },
                { name: '작업지시', data: [] }
            ],
            stroke: { width: [0, 3], curve: 'smooth' },
            colors: ['#c7c9f7', '#6a5acd'],
            xaxis: {
                categories: [],
                style: {
                    fontSize: '13px',
                    fontWeight: 500,
                    colors: '#6b7280'
                }
            },
            yaxis: {
                min: 0,
                style: {
                    fontSize: '13px',
                    fontWeight: 500,
                    colors: '#6b7280'
                }
            },
            tooltip: {
                shared: true,
                style: { fontSize: '13px' }
            },
            grid: { borderColor: '#eaeaea', strokeDashArray: 4 }
        };

        itemTrendChart = new ApexCharts(
            document.querySelector('#itemTrendChart'),
            options
        );
        itemTrendChart.render();
    }

    // 3) 기간별 품목 추이 로딩
    loadItemTrend(item.itemId, 'DAY');
}

let selectedItemId = null;

async function loadItemTrend(itemId, range = 'DAY') {
    selectedItemId = itemId;

    const response = await fetch(
        `/production/itemChart/data?itemId=${itemId}&range=${range}`
    );

    if (!response.ok) {
        console.error('품목 기간별 추이 로딩 실패');
        return;
    }

    const raw = await response.json();

    //  normalizeSeries 그대로 재활용
    const { categories, planSeries, orderSeries, yMax } =
        normalizeSeries(range, raw);

    await itemTrendChart.updateOptions({
        xaxis: {
            categories,
            style: {
                fontSize: '13px',
                fontWeight: 500,
                colors: '#6b7280'
            }
        },
        yaxis: {
            min: 0,
            max: yMax,
            style: {
                fontSize: '13px',
                fontWeight: 500,
                colors: '#6b7280'
            }
        }
    }, false, true);

    await itemTrendChart.updateSeries([
        { name: '생산계획', type: 'column', data: planSeries },
        { name: '작업지시', type: 'line', data: orderSeries }
    ], true);
}

document.addEventListener("DOMContentLoaded", () => {
    document.getElementById('btnItemDay')?.addEventListener('click', () => {
        if (selectedItemId) loadItemTrend(selectedItemId, 'DAY');
    });

    document.getElementById('btnItemWeek')?.addEventListener('click', () => {
        if (selectedItemId) loadItemTrend(selectedItemId, 'WEEK');
    });

    document.getElementById('btnItemMonth')?.addEventListener('click', () => {
        if (selectedItemId) loadItemTrend(selectedItemId, 'MONTH');
    });
});




