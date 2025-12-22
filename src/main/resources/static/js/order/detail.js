// ======================================================
  // 작업지시 상세 모달
  // ======================================================
  
	let orderId;
  	let outboundYn;
	  
  function openDetailModal(workId) {
    console.log("Detail:", workId);
    const modal = new bootstrap.Modal(document.getElementById("workDetailModal"));

    // 1) 기본 정보 로드
    fetch(`/order/detail/${workId}`)
            .then(res => res.json())
            .then(data => {
                console.log("정보:::: " , data);
                orderId = data.orderId;
                outboundYn = data.outboundYn;

                  document.getElementById("detailOrderId").innerText = data.orderId;
                  document.getElementById("detailPrdName").innerText = data.prdName;
                  document.getElementById("detailPlanQty").innerText = data.planQty;
                  document.getElementById("detailPlanDate").innerText = data.planDate;
                  document.getElementById("detailPlanTime").innerText = data.planTime;
                  document.getElementById("detailLineName").innerText = data.lineName;
                  document.getElementById("detailRouteId").innerText = data.routeId;
                  document.getElementById("detailRemark").value = data.remark;
                  
                  const dataStatus = data.status;
                  const statusBtn = document.getElementById("btnReleased");
                  const cancelBtn = document.getElementById("btnCanceled");
                  const statusText = document.getElementById("detailStatus");
                  
                  if (dataStatus === "CREATED"){
                	  statusBtn.innerText = '확정';
                	  statusBtn.classList.add('btn-success');
                	  if (statusBtn.classList.contains('btn-info')){
                		  statusBtn.classList.remove('btn-info');
                	  }               	  
                	  statusText.innerText = '준비';
                	  statusBtn.disabled = false; // 확정버튼 활성화
                	  cancelBtn.disabled = false; // 취소버튼 활성화
                  } else {
                	  statusBtn.innerText = '확정됨';
                	  statusBtn.classList.add('btn-info');
                	  if (statusBtn.classList.contains('btn-success')){
                		  statusBtn.classList.remove('btn-success');
                	  }
                	  switch (dataStatus){
                	  	case "RELEASED":
                	  		statusText.innerText = '확정';
                	  		break;
                	  	case "IN_PROGRESS":
                	  		statusText.innerText = '진행';
                	  		break;
                	  	case "COMPLETED":
                	  		statusText.innerText = '완료';
                	  		break;
                	  	case "CANCELED":
                	  		statusText.innerText = '취소';
                	  		break;
                	  	default:
                	  		statusText.innerText = '미정';
                	  }
                	  statusBtn.disabled = true; // 확정버튼 비활성화
                	  cancelBtn.disabled = true; // 취소버튼 비활성화
                	  
                  }
                  

                  // 작업자 렌더링
                  data.infos.forEach(info => {
				    const select = document.getElementById(info.processId);  
				
				    if (!select) return;
				
				    // workerId가 null이면 선택값 없음
				    if (info.workerId) {
				        select.value = info.workerId;
				    } else {
				        select.value = ""; // '선택'
				    }
				    
				    if (dataStatus === "CREATED"){
				    	select.disabled = false;
				    } else {
				    	select.disabled = true;
				    }
				});


                  // 진행률 처리
                  updateProcessUI(data.infos);
            });

      modal.show();
  }
  
  // 상태 설정
  function updateProcessUI(workInfoList) {
	  
	const dots = document.querySelectorAll(".process-line .dot");
    const processLine = document.querySelector(".process-line");

    // 1) 점 상태 초기화
    dots.forEach(dot => dot.className = "dot");

    // 2) 점 색상 매핑
    workInfoList.forEach((step, i) => {
        const dot = dots[i];
        if (!dot) return;

        switch (step.status) {
            case "DONE":
                dot.classList.add("done");
                break;
            case "IN_PROGRESS":
                dot.classList.add("current");
                break;
            default:
                dot.classList.add("future");
        }
    });

    // 3) 진행률: COMPLETED 개수로 퍼센트 계산
    const completedCount = workInfoList.filter(s => s.status === "COMPLETED").length;
    const totalCount = workInfoList.length;
    const progressPercent = (completedCount / totalCount) * 100;

    // 4) CSS 변수로 선 색상 조절
    processLine.style.setProperty("--progress", progressPercent + "%");

}
  
  // 확정
  document.getElementById("btnReleased").addEventListener("click", async () => {
	  console.log("orderid...............", orderId);
	  updateStatus(orderId, "RELEASED", "작업지시가 확정되었습니다.");
  });
  
  // 취소
  document.getElementById("btnCanceled").addEventListener("click", async() => {
	  
	  if (outboundYn?.trim() === 'Y') {
		  alert("출고가 완료된 작업지시는 취소가 불가능합니다.");
	  	  return;
	  }
	  if (!confirm("정말 취소하시겠습니까?")) return;
	  updateStatus(orderId, "CANCELED", "작업지시가 취소되었습니다.");
  });
  
  // 작업지시서 상태변경 공통 함수
  async function updateStatus (orderId, status, alertContent){
	  return fetch (`/order/status/${orderId}?status=${status}`, {
		  method: "PATCH",
		  headers: {
		      "Content-Type": "application/json",
		      [csrfHeader]: csrfToken
		  }
	  })
	  .then(res => {
		  if (!res.ok) throw new Error ("서버 오류 : " + res.status);
		  return res.text();
	  })
      .then(() => {
    	 alert(alertContent);
    	 
   		// 모달 닫기
   	    const modal = bootstrap.Modal.getInstance(document.getElementById("workDetailModal"));
   	    modal.hide();
   	    
   	    // 목록 새로고침
   	    loadWorkOrderGrid(); 
     })
     .catch(err => console.error("오류 발생:", err));
  }

  // 수정
  document.getElementById("btnModify").addEventListener("click", async () => {

	  const prcBld = document.getElementById("PRC-BLD").value;
	  const prcFlt = document.getElementById("PRC-FLT").value;
	  const prcFil = document.getElementById("PRC-FIL").value;
	  const prcCap = document.getElementById("PRC-CAP").value;
	  const prcQc  = document.getElementById("PRC-QC").value;
	  const prcLbl = document.getElementById("PRC-LBL").value;
	  const remark = document.getElementById("detailRemark").value;
	  
	  const payload = {
		"PRC-BLD": prcBld,
		"PRC-FLT": prcFlt,
		"PRC-FIL": prcFil,
		"PRC-CAP": prcCap,
	    "PRC-QC" : prcQc,
		"PRC-LBL": prcLbl,
		"remark": remark
	  };

	  fetch (`/order/modify/${orderId}`, {
		  method: "PATCH",
		  headers: {
			  "Content-Type": "application/json",
			  [csrfHeader]: csrfToken
		  },
		  body: JSON.stringify(payload)
	  })
		.then(() => {
		  alert("수정 완료");
	  })
	    .catch(err => console.error ("오류 발생", err));
  });
  
  	// 시작입력 보정
	const startTime = document.getElementById("startTime");
	const endTime 	= document.getElementById("endTime");
	 
	function validateTimes(){
		const start = startTime.value;
		const end	= endTime.value;
	
	
		if (!start || !end) 
			return;
		
		
		// 근무시간 범위 설정
		if (start < "09:00" || start > "18:00") {
			alert ("시작 시간은 09:00~18:00 사이여야 합니다.");
			startTime.value = "";
			return;
		}
		
		if (end < "09:00" || end > "18:00") {
			alert ("종료 시간은 09:00~18:00 사이여야 합니다.");
			endTime.value = "";
			return;
		}
		
		// 30분 단위로 가능
		const startMin = Number(start.split(":")[1]);
		const endMin   = Number(end.split(":")[1]);
		 
		if (startMin % 30 !== 0 || endMin % 30 !== 0) {
			alert("시간은 30분 단위로만 선택할 수 있습니다.");
			startTime.value = "";
			endTime.value	= "";
			return;
		}
		
		// 시작시간 < 종료시간
		if (start >= end) {
			alert("종료 시간은 시작 시간 이후여야 합니다.");
			endTime.value = "";
			return;
		}
	}
	
	// 이벤트 연결
	startTime.addEventListener("change", () => {
		// 시작 시간이 바뀌면 최솟값 갱신
		if (startTime.value) {
			endTime.min = startTime.value;
		}
		validateTimes();
	});
	
	endTime.addEventListener("change", validateTimes);
	