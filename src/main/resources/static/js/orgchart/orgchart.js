document.addEventListener('DOMContentLoaded', function () {

  // 1. 서버에서 조직도 데이터 가져오기
  fetch(apiUrl(`api/org/tree`))
    .then(res => res.json())
    .then(data => {
	  
	  // flat → Treant 트리 구조 변환
      const nodeStructure = buildTreantTree(data); 

      const chartConfig = {
        chart: {
          container: "#orgCard",
          nodeAlign: "BOTTOM",
          connectors: { type: "step" },
          node: { HTMLclass: "node-style" }
        },
        nodeStructure: nodeStructure   
      };

      new Treant(chartConfig);
    })
    .catch(err => {
      console.error('조직도 데이터 로딩 실패', err);
    });
});
  

function buildTreantTree(nodes) {

  const deptInfoMap = {};   // deptId → { deptId, parentDeptId, deptName }
  const empListMap  = {};   // deptId → [ {empName, posName}, ... ]
  const deptNodeMap = {};   // deptId → Treant node
  const roots = [];

  // 1) 부서 정보, 직원 리스트 그룹핑
  nodes.forEach(n => {
    if (!deptInfoMap[n.deptId]) {
      deptInfoMap[n.deptId] = {
        deptId: n.deptId,
        parentDeptId: n.parentDeptId,
        deptName: n.deptName
      };
    }

    if (!empListMap[n.deptId]) {
      empListMap[n.deptId] = [];
    }
    if (n.empName) {
      empListMap[n.deptId].push({
        empName: n.empName,
        posName: n.posName || ''
      });
    }
  });

  // 2) 부서별 카드 HTML 만들기
  Object.values(deptInfoMap).forEach(info => {
    const empList = empListMap[info.deptId] || [];

    const empRowsHtml = empList.map(e => `
      <div class="emp-row">
        <span class="emp-name">${e.empName}</span>
        <span class="emp-sep">|</span>
        <span class="emp-pos">${e.posName}</span>
      </div>
    `).join('');
	
	const themeClass =
	  info.deptId === 'DEP999' ? 'theme-representative' :
	  info.deptId === 'DEP000' || info.parentDeptId === 'DEP000' ? 'theme-erp' :
	  info.deptId === 'DEP100' || info.parentDeptId === 'DEP100' ? 'theme-mes' :
	  '';

    const cardHtml = `
      <div class="dept-card">
        <div class="dept-header ${themeClass}">${info.deptName}</div>
        <div class="dept-body">
          ${empRowsHtml || `<div class="emp-row emp-empty">직원 없음</div>`}
        </div>
      </div>
    `;

    // Treant 노드 정의 (text 대신 innerHTML 사용)
    deptNodeMap[info.deptId] = {
      innerHTML: cardHtml,
      HTMLclass: "dept-node",
      children: []
    };
  });

  // 3) 부서 간 부모-자식 트리 연결
  Object.values(deptInfoMap).forEach(info => {
    const node = deptNodeMap[info.deptId];

    if (!info.parentDeptId) {
      // 최상위 부서
      roots.push(node);
    } else {
      const parent = deptNodeMap[info.parentDeptId];
      if (parent) {
        parent.children.push(node);
      }
    }
  });

  // 4) 루트가 하나면 그대로, 여러 개면 가짜 루트
  if (roots.length === 1) {
    return roots[0];
  }

  return {
    innerHTML: `<div class="dept-card root-card"><div class="dept-header">조직도</div></div>`,
    HTMLclass: "dept-node root-node",
    children: roots
  };
}
