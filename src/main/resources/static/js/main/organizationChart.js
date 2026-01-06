const organizationChartGridEl = document.getElementById("organizationChartGrid");
var treeData
const myDeptId = 'DEP002';

document.addEventListener('DOMContentLoaded', async function () {
	getOrganizationChart();
});

// 최근 공지사항 목록 데이터조회
async function getOrganizationChart() {
	await fetch(apiUrl(`api/schedules/organizationChart`), {method: 'GET'})
	.then(response => {
		if (!response.ok) throw new Error(response.text());
		return response.json();  //JSON 파싱
	}).then(async data => {
		treeData = await buildDeptTree(data.data);
		await initOrganizationChart(treeData);
	}).catch(error => {
		console.error('에러', error)
		alert("공지 데이터 조회 실패");
	});
}

async function buildDeptTree(flatList) {
    const deptMap = {};
    flatList.forEach(item => {
        // 부서 노드 준비
        if (!deptMap[item.DEPT_ID]) {
            deptMap[item.DEPT_ID] = {
                name: item.DEPT_NAME,
                deptId: item.DEPT_ID,
                type: 'department',
                parentId: item.PARENT_ID ?? null,
                children: []
            };
        }
        // 직원 노드는 부서 children에 추가
        if (item.EMP_ID) {
            deptMap[item.DEPT_ID].children.push({
                name: item.EMP_NAME,
                empId: item.EMP_ID,
                type: 'employee'
            });
        }
    });

    // 계층 트리화
    const treeRoot = [];
    Object.values(deptMap).forEach(dept => {
        if (!dept.parentId || !deptMap[dept.parentId]) {
            treeRoot.push(dept);   // 최상위 부서
        } else {
            deptMap[dept.parentId].children.push(dept);  // 하위부서로 연결
        }
    });
	
//	function convertTreeNodes(nodes) {
//	    return nodes.map(node => {
//	        const newNode = { ...node };
//	        if (Array.isArray(newNode.children) && newNode.children.length > 0) {
//	            newNode._children = convertTreeNodes(newNode.children);
//	        }
//	        delete newNode.children;
//	        newNode._attributes = { expanded: true }; // 펼침 초기화
//	        return newNode;
//	    });
//	}
	
	const toastTreeData = convertTreeNodes(treeRoot, myDeptId);

    return toastTreeData;
}

// 공지그리드 생성변수
let treeGrid = null;

// 공지그리드 그리기 함수
async function initOrganizationChart(data) {
	console.log("트리데이터 : ", treeData);
	treeGrid = new tui.Grid({
	    el: organizationChartGridEl,
	    data: data,
	    rowHeaders: ['checkbox'],
	    bodyHeight: 300,
	    treeColumnOptions: {
	        name: 'name',
	        useCascadingCheckbox: true,
//	        onlyLeafCheckbox: false
	    },
	    columns: [
	        {
	            header: '이름',
	            name: 'name',
	            treeColumn: true,
				align: 'left',
	            width: 200
	        },
			{
				header: '사번',
				name: 'empId',
				align: 'center'
			}
	    ]
	});
	
	//트리 펼침
	treeGrid.on('expand', ev => {
	  const { rowKey } = ev;
	  const descendantRows = treeGrid.getDescendantRows(rowKey);

	  console.log('rowKey: ' + rowKey);
	  console.log('descendantRows: ' + descendantRows);

	  if (!descendantRows.length) {
	    treeGrid.appendRow(
	      {
	        name: 'dynamic loading data',
	        _children: [
	          {
	            name: 'leaf row'
	          },
	          {
	            name: 'internal row',
	            _children: []
	          }
	        ]
	      },
	      { parentRowKey: rowKey }
	    );
	  }
	});

	treeGrid.on('collapse', ev => {
	  const { rowKey } = ev;
	  const descendantRows = treeGrid.getDescendantRows(rowKey);

	  console.log('rowKey: ' + rowKey);
	  console.log('descendantRows: ' + descendantRows);
	});
	
	// 확인버튼
	const checkedUpEmpBtn = document.getElementById('checkedUpEmpBtn');
	
	checkedUpEmpBtn.addEventListener('click', function() {
		console.log("확인버튼눌림");
		const checkedUpEmpList = getCheckedEmpId();
		const checkedEmpEl = document.getElementById("checkedEmp");
		
		checkedEmpEl.innerHTML = '';
		
		if(checkedUpEmpList.length > 0) {
			
			checkedUpEmpList.forEach((emp) => {
				let div = document.createElement("div");
				let span = document.createElement("span");
				const empName = emp.empName;
				const empId = emp.empId;
				span.textContent = empName + ' (' + empId + ')'
				div.appendChild(span);
				checkedEmpEl.append(div);
			})
			
		}
		
	})
	
	// 체크한 명단 가져오기
	function getCheckedEmpId() {
		let checked = [];
		const rows = treeGrid.getData();
//		console.log("treeData : ", rows);
		rows.forEach((rowData) => {
			console.log("rowDAta : ", rowData);
			if(rowData._attributes.checked && rowData.empId != null) {
				checked.push({
					empId: rowData.empId
					, empName: rowData.name
				});
			}
		})
		
//		console.log("반복끝 checked : ", checked);
		return checked;
	}
	
	// 명단 div에 뿌리기
	
}

function convertTreeNodes(nodes, myDeptId) {
    return nodes.map(node => {
        const newNode = { ...node };

        // 최상위 부서(대표)는 펼쳐놓고,
        // 하위 부서들 중 내가 속한 부서만 펼침 -> expanded: true
        // 나머지는 접힘 -> expanded: false
        if (newNode.parentId === null) {
            // 최상위 부서(대표)는 무조건 펼침
            newNode._attributes = { expanded: true };
        } else if (newNode.deptId === myDeptId) {
            // 내가 속한 부서만 펼침
            newNode._attributes = { expanded: true };
        } else {
            // 그 외는 접힘
            newNode._attributes = { expanded: false };
        }

        if (Array.isArray(newNode.children) && newNode.children.length > 0) {
            newNode._children = convertTreeNodes(newNode.children, myDeptId);
        }
        delete newNode.children;
        return newNode;
    });
}