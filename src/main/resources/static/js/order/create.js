
// ====================================
  // 유효성 검증 (프론트)
  // ====================================
  function validateModalForm() {
      let valid = true;

      function showError(fieldName, condition) {
          const el = document.querySelector(`[name="${fieldName}"]`)
		  			|| document.getElementById(fieldName);
          const errBox = document.querySelector(`[data-error-for="${fieldName}"]`);

          if (!el || !errBox) return;

          if (condition) {
              el.classList.add("is-invalid");
              errBox.style.display = "block";
              valid = false;
          } else {
              el.classList.remove("is-invalid");
              errBox.style.display = "none";
          }
      }

      // ===========================
      // 1) 기본 필드
      // ===========================
      ["planId", "routeId", "prdId", "lineId"].forEach(name => {
    	  showError(name, !document.querySelector(`[name="${name}"]`)?.value);
      });

      const qty = document.querySelector(`[name="planQty"]`).value;
      showError("planQty", !qty || qty <= 0);

	  // ===========================
	  // 2) 날짜/시간 검증
	  // ===========================
      const date  = document.getElementById("planDate")?.value;
      const start = document.getElementById("startTime")?.value;
      const end	  = document.getElementById("endTime")?.value;
      
      // a) 날짜 미선택
      showError("planDate", !date);
	  
	  // a-1) 오늘 이전 날짜 금지
	  if (date) {
		const selected = new Date(date + "T00:00:00");
		const today = new Date();
		today.setHours(0, 0, 0, 0);
		showError("planDate", selected < today);
	  }
      
      // b) 시간 단독 입력 방지 (날짜가 없는 경우 에러)
      if (!date && (start || end)) {
    	  showError("planDate", true);
      }
      
      // c) 날짜가 있으면 시간 입력 필수
      showError("startTime", date && !start);
      showError("endTime",   date && !end);

      // d) 시간 순서 검증 (모두 있는 경우)
      if (date && start && end) {
          const [sh, sm] = start.split(":").map(Number);
          const [eh, em] = end.split(":").map(Number);
          
          const startMin = sh * 60 + sm;
          const endMin	 = eh * 60 + em;
          
          if (endMin <= startMin) {
        	  showError("planEndDate", true);
          }
      }

	  // ===========================
	  // 3) 작업자 검증 (필수 입력 + 중복 금지)
	  // ===========================
	  const workerNames = ["prcBld", "prcFlt", "prcFil", "prcCap", "prcQc", "prcLbl"];
	  
	  // a) 필수 입력
      workerNames.forEach(name => {
    	  showError(name, !document.querySelector(`[name="${name}"]`)?.value);
      });
	  
	  // b) 중복 방지
	  const workerValues = workerNames
	  	.map(n => document.querySelector(`[name="${n}"]`)?.value || "")
		.filter(v => v);
		
	  if (workerValues.length > 0) {
		const counts = workerValues.reduce((acc, v) => {
			acc[v] = (acc[v] || 0) + 1;
			return acc;
		}, {});
	  
		workerNames.forEach(name => {
			const v = document.querySelector(`[name="${name}"]`)?.value;
			if (v && counts[v] > 1){
				showError(name, true);
			}
		});
	}


      return valid;
  }


  // ======================================================
  // 작업지시 등록 모달
  // ======================================================

  function debounce(fn, delay = 300) {
	  let timer = null;
	  return function (...args) {
		  clearTimeout(timer);
		  timer = setTimeout(() => fn.apply(this, args), delay);
	  };
  }

  // 등록 모달 열기
  document.getElementById("btnCreate")?.addEventListener("click", () => {
    clearFormFields();
    const modal = new bootstrap.Modal(document.getElementById("workCreateModal"));
    modal.show();

	resetValidationUI();

	// 상태UI도 함께 초기화
	updateValidationUI({ valid: false, message: "", lineAvailable: true, workerAvailable: true });
  });
  
  let maxQty = null;

  // 생산계획서 정보 불러오기
  document.getElementById("planId").addEventListener("change", function() {
	    const option  = this.selectedOptions[0];
		if (!option) return;
		
	    const product = option.dataset.name;
	    const total   = option.dataset.total;
        const remain  = option.dataset.remain;
	    
	    maxQty = remain;	// 수량 저장
	    referTime = calcRecommendedHours(total);	// 소요시간 계산

	    document.getElementById("planDetail").innerText = `${product} (${remain} / ${total}개)`;
	    document.getElementById("referTime").innerText = referTime;
	    if (remain < 336) {
	    	document.getElementById("planQty").value = remain;
	    }
	    
	    const matchedOption = selectOptionByText("prdName", product);
	    let prdId;

	    if (matchedOption) {
	        prdId = matchedOption.value;
	        document.getElementById("routeHeader").value = `RT-${prdId}`;
	    }
	    
/*	    fetch(`/order/check?prdId=${prdId}&planQty=${remain}`)
	    .then(res => res.json())
	    .then(list => renderStockPanel(list));*/
	});

  // 품목 자동 선택
  function selectOptionByText(selectId, text) {
	    const select = document.getElementById(selectId);
		if (!select) return null;
		
	    for (let option of select.options) {
	        if (option.text.trim() === text.trim()) {
	            option.selected = true;
	            return option;
	        }
	    }
	    return null;
	}
  
  // 계획 수량 제한
  document.getElementById("planQty").addEventListener("input", function () {
    if (maxQty && Number(this.value) > maxQty) {
        alert(`이 생산계획의 최대 수량은 ${maxQty}개입니다.`);
        this.value = maxQty;
    }
  });
  
  // 예상 소요 시간 계산
  function calcRecommendedHours(remain) {
	   const hours = remain / 42;
	   return "약 " + Math.ceil(remain / 42) + "시간(" + Math.round(hours * 10) / 10 + "시간) 소요 예상";
  }
  
  // ====================================
  // 서버 유효성 검증 (공통 호출부로 정리)
  // ====================================

  // 유효성 검증용 페이로드
  function collectValidatePayload() {
	  const date	= document.getElementById("planDate")?.value;
	  const start	= document.getElementById("startTime")?.value;
	  const end		= document.getElementById("endTime")?.value;

	  return {
		  lineId: document.querySelector('[name="lineId"]').value,
		  startTime: (date && start) ? `${date}T${start}`  : null,
		  endTime:   (date && end)   ? `${date}T${end}`    : null,
		  workers: [
			  document.querySelector('[name="prcBld"]').value,
			  document.querySelector('[name="prcFlt"]').value,
			  document.querySelector('[name="prcFil"]').value,
			  document.querySelector('[name="prcCap"]').value,
			  document.querySelector('[name="prcLbl"]').value
		  ].filter(v => v)	// null 제거
	  };
  }
  
  // 서버 유효성 검증 호출
  async function validateBeforeSubmit() {
    const payload = collectValidatePayload();

    const res = await fetch("/order/validate", {
  	  method: "POST",
  	  headers: {
  		  "Content-Type": "application/json",
  		  [csrfHeader]: csrfToken
  	  },
  	  body: JSON.stringify(payload)
    });

    return await res.json();
  }


  // change 될 때 유효성 검증
  const watchFields = [
  "planDate",
  "startTime",
  "endTime",
  "lineId",
  "prcBld",
  "prcFlt",
  "prcFil",
  "prcCap",
  "prcQc",
  "prcLbl",

  // 있으면 걸리고, 없으면 스킵
  "line",
  "blendSelect",
  "filterSelect",
  "fillSelect",
  "cappingSelect",
  "labelPackSelect"
  ];

  watchFields.forEach(id => {
    const el = document.getElementById(id) || document.querySelector(`[name="${id}"]`);
    if (!el) return;
    el.addEventListener("change", debounce(runValidation, 300));
  });

  async function runValidation() {
    const result = await validateBeforeSubmit();
    updateValidationUI(result);
  }

  
  // ====================================
  // 폼 전송(등록 처리)
  // ====================================
  document.getElementById("btnCreateSubmit").addEventListener("click", async () => {

	  // 1) 프론트 유효성 검증
      if (!validateModalForm()) return;

	  // 2) hidden Input 세팅	  
      const date = document.getElementById("planDate").value;
      const start = document.getElementById("startTime").value;
      const end = document.getElementById("endTime").value;

      if (date && start) {
          document.getElementById("planStartDate").value = `${date}T${start}`;
      }

      if (date && end) {
          document.getElementById("planEndDate").value = `${date}T${end}`;
      }
	  
	  // 3) 서버 유효성 검증
	  const validation = await validateBeforeSubmit();

	  if (!validation.valid) {
	    alert(validation.message);

	    if (validation.suggestedLines) {
	  	  showSuggestedLines(validation.suggestedLines);
	    }

	    if (validation.suggestedWorkers) {
	  	  showSuggestedWorkers(validation.suggestedWorkers);
	    }

	    return;
	  }

	  // 4) 등록 payload
      const payload = {
          planId: document.querySelector('[name="planId"]').value,
          prdId:  document.querySelector('[name="prdId"]').value,
          lineId: document.querySelector('[name="lineId"]').value,
          routeId: document.querySelector('[name="routeId"]').value,
          planQty: document.querySelector('[name="planQty"]').value,
          planStartDate: document.getElementById("planStartDate").value,
          planEndDate:   document.getElementById("planEndDate").value,
          prcBld: document.querySelector('[name="prcBld"]').value,
          prcFlt: document.querySelector('[name="prcFlt"]').value,
          prcFil: document.querySelector('[name="prcFil"]').value,
          prcCap: document.querySelector('[name="prcCap"]').value,
		  prcQc:  document.querySelector('[name="prcQc"]').value,
          prcLbl: document.querySelector('[name="prcLbl"]').value,
          remark: document.querySelector('[name="remark"]').value
      };


      const res = await fetch("/order/create", {
          method: "POST",
          headers: {
              "Content-Type": "application/json",
              [csrfHeader]: csrfToken
          },
          body: JSON.stringify(payload)
      });

      if (res.ok) {
          alert("등록이 완료되었습니다.");
          bootstrap.Modal.getInstance(document.getElementById("workCreateModal")).hide();
          loadWorkOrderGrid();  // 그리드 갱신
      } else {
          alert("등록 실패: 서버 오류");
      }
  });

  
  // ====================================
  // 추천 UI
  // ====================================
  
  // line 추천
  function showSuggestedLines(lines) {
	const box = document.getElementById("lineSuggestBox");
	if (!box) return;
	
	box.innerHTML = "";

	lines.forEach(lineId => {
		const btn = document.createElement("button");
		btn.className = "btn btn-sm btn-outline-primary me-1";
		btn.innerText = lineId;
		btn.onclick = () => {
			document.querySelector('[name="lineId"]').value = lineId;
			runValidation();
		};
		box.appendChild(btn);
	});
  }

  // 작업자 추천
  function showSuggestedWorkers(workers) {
	  const box = document.getElementById("workerSuggestBox");
	  if (!box) return;
	  
	  box.innerHTML = "";

	  workers.forEach(workerId => {
		 const btn = document.createElement("button");
		 btn.className = "btn btn-sm btn-outline-primary me-1";
		 btn.innerText = workerId;
		 btn.onclick = () => {
			 const targetField = findEmptyWorkerField();

			 if (!targetField) {
				 alert("이미 모든 작업자 슬롯이 채워져 있습니다.");
				 return;
			 }

			 document.querySelector(`[name="${targetField}"]`).value = workerId;
			 runValidation();
		 };

		 box.appendChild(btn);
	  });
  }

  // 작업자 필드
  const workerFields = [
	  "prcBld", "prcFlt", "prcFil", "prcCap", "prcLbl"
  ];

  // 비어있는 슬롯 찾기
  function findEmptyWorkerField() {
	  return workerFields.find(name => {
		  const el = document.querySelector(`[name="${name}"]`);
		  return el && !el.value;
	  });
  }

  // 유효성검사 결과 UI
  function updateValidationUI(result) {

	  const msgBox = document.getElementById("validationMessage");
	  const submitBtn = document.getElementById("btnCreateSubmit");

	  // 없으면 종료
	  if (!result) return;
	  
	  // 초기화
	  msgBox.classList.remove("text-danger", "text-success");

	  if (result.valid) {
		  msgBox.innerText = "";
		  msgBox.classList.add('text-success');
		  submitBtn.disabled = false;
		  return;
	  }

	  // 유효하지 않은 경우
	  submitBtn.disabled = true;
	  msgBox.innerText = result.message || "유효성 검증 실패";
	  msgBox.classList.add('text-danger');

	  // 라인 추천
	  if (!result.lineAvailable && result.suggestedLines) {
		  showSuggestedLines(result.suggestedLines);
	  }

	  // 작업자 추천
	  if (!result.workerAvailable && result.suggestedWorkers) {
		  showSuggestedWorkers(result.suggestedWorkers);
	  }
  }
  
  
  // ====================================
  // 유효성 검사 초기화
  // ===================================
  function resetValidationUI() {
    const msgBox = document.getElementById("validationMessage");
    const lineBox = document.getElementById("lineSuggestBox");
    const workerBox = document.getElementById("workerSuggestBox");
    const submitBtn = document.getElementById("btnCreateSubmit");

    if (!msgBox || !lineBox || !workerBox || !submitBtn) return;

    msgBox.innerText = "";
    msgBox.classList.remove("text-danger", "text-success");

    lineBox.innerHTML = "";
    workerBox.innerHTML = "";

    submitBtn.disabled = true; // 제출 불가
  }


  
  // ====================================
  // 값 초기화
  // ====================================
  
  function clearFormFields(){

      // select, input 들 직접 초기화
      document.querySelector('select[name="planId"]').value = "";
      document.querySelector('select[name="prdId"]').value  = "";
      document.querySelector('select[name="lineId"]').value = "";
      document.querySelector('input[name="routeId"]').value = "";
      document.querySelector('input[name="planQty"]').value = "";
      document.getElementById("planDate").value = "";
      document.getElementById("startTime").value = "";
      document.getElementById("endTime").value = "";

      // 작업자들
      document.querySelector('select[name="prcBld"]').value = "";
      document.querySelector('select[name="prcFlt"]').value = "";
      document.querySelector('select[name="prcFil"]').value = "";
      document.querySelector('select[name="prcCap"]').value = "";
      document.querySelector('select[name="prcLbl"]').value = "";

      // 표시용 텍스트 초기화
      document.getElementById("planDetail").innerText = "생산 계획을 선택해주세요.";
      document.getElementById("referTime").innerText  = "생산 계획을 선택해주세요.";
  }