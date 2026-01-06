// supplierDetail.js (AG Grid - 인라인 편집 + step 설정)

// BOM 단위 → 허용 공급단위 매핑
const unitMap = {
    "KG": ["KG", "G"],
    "G":  ["kg", "g"],
    "ML": ["ml", "L"],
    "L":  ["L", "ML"],
    "BOX": ["BOX"],
    "EA": ["EA"]
};


let itemGridApi = null;
const clientId = window.clientId ?? null;

/* =======================================================
   페이지 로딩 시 실행
======================================================= */
document.addEventListener("DOMContentLoaded", () => {
    console.log("clientId =", clientId);
    console.log("initialItemList =", window.initialItemList);

    initItemGrid();
    loadItemGrid();
});

/* =======================================================
   1) AG-GRID — 협력사 취급 품목 목록 (인라인 편집)
======================================================= */
function initItemGrid() {
    const columnDefs = [
          {
            headerName: "자재구분",
            field: "matType",
            width: 110,
            editable: false,
            cellRenderer: p => {
                const v = p.value;
                if (v === "RAW") return "원재료";
                if (v === "SUB") return "부자재";
                if (v === "PKG") return "포장재";
                return "";
            }
        },

        {
            headerName: "자재코드",
            width: 130,
            editable: false,
			cellClass: "text-center",
            valueGetter: p => p.data.materialId || p.data.matId || ""
        },

        {
            headerName: "품명",
            flex: 1,
            minWidth: 120,
            editable: false,
            valueGetter: p => p.data.materialName || p.data.matName || ""
        },

        {
            headerName: "BOM단위",
            width: 90,
            editable: false,
            valueGetter: p => p.data.matUnit ?? ""
        },

        // 🔥 편집 가능: 공급단위
		{
		    headerName: "공급단위",
		    field: "unit",
		    width: 100,

		    // ✅ matUnit 있는 경우만 편집 가능
		    editable: params => {
		        const matUnit = params.data.matUnit?.toUpperCase();
		        return !!unitMap[matUnit];
		    },

		    cellEditor: 'agSelectCellEditor',

		    // ✅ 행(row)별로 선택지 동적 변경
		    cellEditorParams: params => {
		        const matUnit = params.data.matUnit?.toUpperCase();
		        return {
		            values: unitMap[matUnit] || []
		        };
		    },

		    // ✅ 잘못된 단위 선택 시 저장 차단
		    valueSetter: params => {
		        const matUnit = params.data.matUnit?.toUpperCase();
		        const allowedUnits = unitMap[matUnit] || [];

		        if (!allowedUnits.includes(params.newValue)) {
		            alert(`❌ ${matUnit} 기준에서는 [${allowedUnits.join(", ")}] 단위만 선택 가능합니다.`);
		            return false; // ❌ 저장 안 됨
		        }

		        params.data.unit = params.newValue;
		        return true;
		    }
		},

		// 🔥 편집 가능: 발주단위 (1 단위 증가)
		        {
		            headerName: "발주단위",
		            field: "orderUnit",
		            width: 110,
		            editable: true,
		            cellEditor: 'agNumberCellEditor',
		            cellEditorParams: {
		                min: 1,
		                step: 1,
		                precision: 0
		            },
		            valueSetter: params => {
		                const newValue = params.newValue;
		                const numValue = Number(newValue);
		                
		                if (newValue === null || newValue === undefined || newValue === '' || 
		                    isNaN(numValue) || numValue <= 0) {
		                    alert('발주단위는 1 이상의 숫자를 입력해야 합니다.');
		                    return false;
		                }
		                params.data.orderUnit = numValue;
		                return true;
		            },
		            valueFormatter: p => p.value ? Number(p.value).toLocaleString() : ''
		        },

		// 🔥 편집 가능: MOQ (10 단위 증가)
		        {
		            headerName: "MOQ",
		            width: 100,
		            editable: true,
		            cellEditor: 'agNumberCellEditor',
		            cellEditorParams: {
		                min: 10,
		                step: 10,
		                precision: 0
		            },
		            valueGetter: p => p.data.moq ?? p.data.minOrderQty ?? "",
		            valueSetter: params => {
		                const newValue = params.newValue;
		                const numValue = Number(newValue);
		                
		                if (newValue === null || newValue === undefined || newValue === '' || 
		                    isNaN(numValue) || numValue <= 0) {
		                    alert('MOQ는 1 이상의 숫자를 입력해야 합니다.');
		                    return false;
		                }
		                params.data.moq = numValue;
		                params.data.minOrderQty = numValue;
		                return true;
		            },
		            valueFormatter: p => p.value ? Number(p.value).toLocaleString() : ''
		        },


        // 🔥 편집 가능: 납기일 (1 단위 증가)
        {
            headerName: "납기일",
            field: "leadDays",
            width: 100,
            editable: true,
            cellEditor: 'agNumberCellEditor',
            cellEditorParams: {
                min: 0,
                step: 1,
                precision: 0
            }
        },

		// 🔥 편집 가능: 단가 (10원 단위 증가, 최소 10원)
		      {
		          headerName: "단가",
		          field: "unitPrice",
		          width: 110,
		          editable: true,
		          cellEditor: 'agNumberCellEditor',
		          cellEditorParams: {
		              min: 10,
		              step: 10,
		              precision: 0
		          },
		          valueSetter: params => {
		              const newValue = params.newValue;
		              const numValue = Number(newValue);
		              
		              // null, undefined, 빈값, 숫자가 아님, 0 이하 체크
		              if (newValue === null || newValue === undefined || newValue === '' || 
		                  isNaN(numValue) || numValue < 10) {
		                  alert('단가는 10원 이상의 값을 입력해야 합니다.');
		                  return false;
		              }
		              
		              // 10원 단위로 반올림
		              const roundedValue = Math.round(numValue / 10) * 10;
		              params.data.unitPrice = roundedValue;
		              
		              if (roundedValue !== numValue) {
		                  setTimeout(() => {
		                      alert(`단가는 10원 단위로 입력됩니다. ${roundedValue.toLocaleString()}원으로 저장됩니다.`);
		                  }, 100);
		              }
		              
		              return true;
		          },
		          valueFormatter: p => p.value ? Number(p.value).toLocaleString() + '원' : ''
		      },

			  {
			      headerName: "공급",
			      field: "supplyAvailable",
			      width: 90,
			      editable: true,
			      cellEditor: 'agSelectCellEditor',
			      cellEditorParams: {
			          values: ['Y', 'N']
			      },

			      // ✅ 셀에 보여줄 값
			      valueFormatter: p => {
			          if (p.value === 'Y') return '가능';
			          if (p.value === 'N') return '불가';
			          return '';
			      },

			      // ✅ 셀렉트에서 선택 후 다시 Y/N으로 변환
			      valueParser: p => {
			          if (p.newValue === '가능') return 'Y';
			          if (p.newValue === '불가') return 'N';
			          return p.newValue;
			      }
			  }

    ];

    const gridOptions = {
        columnDefs,
        rowData: [],
        defaultColDef: {
            sortable: true,
            filter: false,
            resizable: true
        },
        pagination: true,
        paginationPageSize: 20,
        
        // 🔥 셀 편집 완료 시 자동 저장
        onCellValueChanged: (event) => {
            console.log('셀 값 변경:', event);
            saveItemChanges(event.data);
        },
        
        // 🔥 편집 모드 스타일
        getRowStyle: params => {
            return { cursor: 'pointer' };
        }
    };

    const gridDiv = document.getElementById("supplierItemGrid");
    if (!gridDiv) {
        console.error("supplierItemGrid 요소를 찾을 수 없습니다.");
        return;
    }

    itemGridApi = agGrid.createGrid(gridDiv, gridOptions);
}

/* =======================================================
   2) 초기 데이터 로드
======================================================= */
function loadItemGrid() {
    if (!itemGridApi) return;

    const data = window.initialItemList || [];
    console.log("loadItemGrid data =", data);

    itemGridApi.setGridOption("rowData", data);
}

/* =======================================================
   3) 🔥 셀 편집 시 자동 저장
======================================================= */
function saveItemChanges(item) {
    // CSRF TOKEN 처리
    const csrfToken = document.querySelector('meta[name="_csrf_token"]')?.content || "";
    const csrfHeader = document.querySelector('meta[name="_csrf_headerName"]')?.content || "";

    const headers = { "Content-Type": "application/json" };
    if (csrfToken && csrfHeader) {
        headers[csrfHeader] = csrfToken;
    }

    // 저장할 데이터 구성
    const payload = {
        unitPrice: item.unitPrice,
        moq: item.moq || item.minOrderQty,
        unit: item.unit,
        orderUnit: item.orderUnit,
        leadDays: item.leadDays,
        supplyAvailable: item.supplyAvailable
    };

    // API 호출
    fetch(apiUrl(`sales/client/${clientId}/items/${item.itemId}/update`), {
        method: "PUT",
        headers: headers,
        body: JSON.stringify(payload)
    })
    .then(res => {
        if (!res.ok) throw new Error("저장 실패");
        return res.text();
    })
    .then(() => {
        console.log("저장 완료:", item.itemId);
        showToast("저장되었습니다.");
    })
    .catch(err => {
        alert("저장 오류: " + err.message);
        // 실패 시 그리드 새로고침
        loadItemGrid();
    });
}

/* =======================================================
   4) 토스트 메시지
======================================================= */
function showToast(message) {
    const toast = document.createElement('div');
    toast.className = 'position-fixed top-0 end-0 p-3';
    toast.style.zIndex = '9999';
    toast.innerHTML = `
        <div class="toast show" role="alert">
            <div class="toast-body bg-success text-white rounded">
                ${message}
            </div>
        </div>
    `;
    document.body.appendChild(toast);
    
    setTimeout(() => {
        toast.remove();
    }, 2000);
}

// ===========================
// 메시지 표시
// ===========================
document.addEventListener("DOMContentLoaded", () => {
    const holder = document.getElementById("msgHolder");
    if (holder && holder.dataset.msg) {
        alert(holder.dataset.msg);
    }

    // 탭 URL 파라미터 처리
    const url = new URL(window.location.href);
    const tab = url.searchParams.get("tab");

    if (tab === "item") {
        document.querySelector("#supplierTabs .nav-link.active")?.classList.remove("active");
        document.querySelector("#basicTab").classList.remove("show", "active");
        document.querySelector("a[href='#itemTab']").classList.add("active");
        document.querySelector("#itemTab").classList.add("show", "active");
    }
});

// ===========================
// 수정 모드 ON
// ===========================
function enableEdit() {
    document.querySelectorAll('.view-mode').forEach(e => e.classList.add('d-none'));
    document.querySelectorAll('.edit-mode').forEach(e => e.classList.remove('d-none'));

    document.getElementById('btnEdit').classList.add('d-none');
    document.getElementById('btnSave').classList.remove('d-none');
    document.getElementById('btnCancel').classList.remove('d-none');
}

// ===========================
// 수정 취소 → 새로고침
// ===========================
function cancelEdit() {
    location.reload();
}

// ===========================
// 다음 주소 검색
// ===========================
function searchAddress() {
    new daum.Postcode({
        oncomplete: function(data) {
            const road = data.roadAddress;
            const jibun = data.jibunAddress;
            const addr = road ? road : jibun;

            document.getElementById('addr').value = addr;

            if (document.getElementById('postCode')) {
                document.getElementById('postCode').value = data.zonecode;
            }

            document.getElementById('addrDetail').focus();
        }
    }).open();
}

// ===========================
// 저장
// ===========================
function saveClient() {
    const client = {
        clientId:        getValue("clientId"),
        ceoName:         getValue("ceoName"),
        managerName:     getValue("managerName"),
        managerDept:     getValue("managerDept"),
        managerTel:      getValue("managerTel"),
        managerEmail:    getValue("managerEmail"),
        addr:            getValue("addr"),
        addrDetail:      getValue("addrDetail"),
        postCode:        getValue("postCode"),
        accountNumber:   getValue("accountNumber"),
        accountName:     getValue("accountName"),
        bankName:        getValue("bankName"),
        statusCode:      getValue("statusCode")
    };

    const csrfToken  = document.querySelector('meta[name="_csrf_token"]')?.content || "";
    const csrfHeader = document.querySelector('meta[name="_csrf_headerName"]')?.content || "";

    const headers = { "Content-Type": "application/json" };
    if (csrfToken && csrfHeader) {
        headers[csrfHeader] = csrfToken;
    }

    fetch(apiUrl(`sales/client/update`), {
        method: "POST",
        headers: headers,
        body: JSON.stringify(client)
    })
    .then(res => {
        if (!res.ok) throw new Error("저장 실패");
        return res.text();
    })
    .then(() => {
        alert("저장되었습니다.");
        location.reload();
    })
    .catch(err => alert("오류 발생: " + err.message));
}

// ===========================
// 공통 input getter
// ===========================
function getValue(id) {
    const el = document.getElementById(id);
    return el ? el.value : "";
}