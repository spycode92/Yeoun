let hasTouchedDeliveryDate = false;

document.addEventListener("DOMContentLoaded", () => {

  /* ============================================
     ✅ DOM 참조
  ============================================ */
  const form = document.getElementById("orderForm");
  const btnSave = document.getElementById("btnSaveOrder");

  const orderDateInput = document.getElementById("orderDate");
  const deliveryDateInput = document.getElementById("deliveryDate");
  const deliveryErr = document.getElementById("deliveryDateError");

  const addItemBtn = document.getElementById("addItemBtn");
  const itemBody = document.getElementById("itemBody");

  // 담당자 정보
  const managerNameInput  = document.getElementById("clientManager");
  const managerTelInput   = document.getElementById("clientManagerTel");
  const managerEmailInput = document.getElementById("clientManagerEmail");

  const nameErr  = document.getElementById("managerNameError");
  const telErr   = document.getElementById("managerTelError");
  const emailErr = document.getElementById("managerEmailError");

  /* 정규식 */
  const NAME_REGEX  = /^[가-힣a-zA-Z\s]{2,}$/;
  const TEL_REGEX   = /^01[0-9]-\d{3,4}-\d{4}$/;
  // 🔥 이메일 검증 강화: 한글 불가, 영문/숫자/특수문자만 허용
  const EMAIL_REGEX = /^[a-zA-Z0-9._%+-]+@[a-zA-Z0-9.-]+\.[a-zA-Z]{2,}$/;

  /* ============================================
     ✅ 공통 유틸
  ============================================ */
  function setSaveEnabled(enabled) {
    if (!btnSave) return;
    btnSave.disabled = !enabled;
  }

  function showDeliveryError(msg) {
    if (!deliveryErr) return;
    if (!msg) {
      deliveryErr.classList.add("d-none");
      deliveryErr.textContent = "";
    } else {
      deliveryErr.classList.remove("d-none");
      deliveryErr.textContent = msg;
    }
  }

  function showError(el, msg) {
    if (!el) return;
    if (!msg) {
      el.classList.add("d-none");
      el.textContent = "";
    } else {
      el.classList.remove("d-none");
      el.textContent = msg;
    }
  }

  // ✅ 영업일 더하기(주말 제외)
  function addBusinessDays(startDate, days) {
    const date = new Date(startDate);
    let added = 0;
    while (added < days) {
      date.setDate(date.getDate() + 1);
      const day = date.getDay();
      if (day !== 0 && day !== 6) added++;
    }
    return date;
  }

  /* ============================================
     ✅ 납기일 검증
  ============================================ */
  function validateDeliveryDate(showErrorMsg = false) {

    if (!deliveryDateInput.value) {
      if (showErrorMsg && hasTouchedDeliveryDate) {
        showDeliveryError("납기일자를 선택하세요.");
      } else {
        showDeliveryError("");
      }
      return false;
    }

    const orderDate = new Date(orderDateInput.value);
    const deliveryDate = new Date(deliveryDateInput.value);

    const day = deliveryDate.getDay();
    if (day === 0 || day === 6) {
      if (showErrorMsg) {
        showDeliveryError("납기일은 평일만 선택 가능합니다.");
      }
      return false;
    }

    const minDate = addBusinessDays(orderDate, 5);
    minDate.setHours(0,0,0,0);
    deliveryDate.setHours(0,0,0,0);

    if (deliveryDate < minDate) {
      if (showErrorMsg) {
        showDeliveryError("납기일은 평일 기준 최소 5영업일 이후여야 합니다.");
      }
      return false;
    }

    showDeliveryError("");
    return true;
  }

  /* ============================================
     ✅ 제품(품목) 필수 입력 검증
  ============================================ */
  function validateOrderItems() {
    if (!itemBody) return false;

    const rows = itemBody.querySelectorAll("tr");
    if (rows.length === 0) {
      alert("수주 품목을 최소 1개 이상 추가해주세요.");
      return false;
    }

    for (let i = 0; i < rows.length; i++) {
      const row = rows[i];
      const prdSelect = row.querySelector(".prd-select");
      const qtyInput = row.querySelector(".qty-input");

      const prdId = prdSelect?.value?.trim();
      const qty = parseInt(qtyInput?.value || "0", 10);

      if (!prdId) {
        alert(`품목 ${i + 1}번째 줄에 제품을 선택해주세요.`);
        prdSelect?.focus();
        return false;
      }

      if (!qty || qty <= 0) {
        alert(`품목 ${i + 1}번째 줄 수량은 1 이상이어야 합니다.`);
        qtyInput?.focus();
        return false;
      }
    }

    return true;
  }

  /* ============================================
     ✅ 담당자 정보 검증
  ============================================ */
  function validateManagerInfo(showErrorMsg = false) {
    let valid = true;

    // 담당자명
    if (!managerNameInput?.value.trim()) {
      if (showErrorMsg) showError(nameErr, "담당자명을 입력하세요.");
      valid = false;
    } else if (!NAME_REGEX.test(managerNameInput.value.trim())) {
      if (showErrorMsg) showError(nameErr, "담당자명은 한글/영문 2자 이상입니다.");
      valid = false;
    } else {
      if (showErrorMsg) showError(nameErr, "");
    }

    // 연락처
    if (!managerTelInput?.value.trim()) {
      if (showErrorMsg) showError(telErr, "연락처를 입력하세요.");
      valid = false;
    } else if (!TEL_REGEX.test(managerTelInput.value.trim())) {
      if (showErrorMsg) showError(telErr, "연락처 형식은 010-1234-5678 입니다.");
      valid = false;
    } else {
      if (showErrorMsg) showError(telErr, "");
    }

    // 이메일
    if (!managerEmailInput?.value.trim()) {
      if (showErrorMsg) showError(emailErr, "이메일을 입력하세요.");
      valid = false;
    } else if (!EMAIL_REGEX.test(managerEmailInput.value.trim())) {
      if (showErrorMsg) showError(emailErr, "이메일 형식이 올바르지 않습니다.");
      valid = false;
    } else {
      if (showErrorMsg) showError(emailErr, "");
    }

    return valid;
  }

  /* ============================================
     ✅ 주소 검증
  ============================================ */
  function validateAddress(showAlert = false) {
    const postcode = document.getElementById("clientPostcode")?.value?.trim();
    const addr = document.getElementById("clientAddr")?.value?.trim();

    if (!postcode || !addr) {
      if (showAlert) {
        alert("배송 주소를 입력해주세요.");
      }
      return false;
    }
    return true;
  }

  /* ============================================
     ✅ 저장버튼 활성화 조건 통합
  ============================================ */
  function refreshSaveButtonState() {
    const okDelivery = validateDeliveryDate(false);
    
    const okItems = (() => {
      if (!itemBody) return false;
      const rows = itemBody.querySelectorAll("tr");
      if (rows.length === 0) return false;

      for (const row of rows) {
        const prdId = row.querySelector(".prd-select")?.value?.trim();
        const qty = parseInt(row.querySelector(".qty-input")?.value || "0", 10);
        if (!prdId) return false;
        if (!qty || qty <= 0) return false;
      }
      return true;
    })();

    const okManager = validateManagerInfo(false);
    const okAddress = validateAddress(false);

    setSaveEnabled(okDelivery && okItems && okManager && okAddress);
  }

  /* ============================================
     ✅ 초기 날짜 세팅
  ============================================ */
  if (orderDateInput && deliveryDateInput) {
    const today = new Date();
    const todayStr = today.toISOString().split("T")[0];

    orderDateInput.value = todayStr;
    orderDateInput.min = todayStr;
    orderDateInput.readOnly = true;

    const minDeliveryDate = addBusinessDays(today, 5);
    deliveryDateInput.min = minDeliveryDate.toISOString().split("T")[0];

    deliveryDateInput.addEventListener("input", refreshSaveButtonState);
    deliveryDateInput.addEventListener("change", refreshSaveButtonState);

    setSaveEnabled(false);
    showDeliveryError("");
  }

  /* ============================================
     ✅ 담당자 정보 실시간 검증
  ============================================ */
  if (managerNameInput) {
    managerNameInput.addEventListener("input", () => {
      const val = managerNameInput.value.trim();
      if (val && !NAME_REGEX.test(val)) {
        showError(nameErr, "담당자명은 한글/영문 2자 이상입니다.");
      } else {
        showError(nameErr, "");
      }
      refreshSaveButtonState();
    });
  }

  if (managerTelInput) {
    // 🔥 전화번호 자동 포맷팅 (010-1234-5678 또는 010-123-5678)
    managerTelInput.addEventListener("input", (e) => {
      let value = e.target.value.replace(/[^0-9]/g, ""); // 숫자만 추출
      
      // 최대 11자리까지만 입력 가능
      if (value.length > 11) {
        value = value.slice(0, 11);
      }
      
      // 자동 하이픈 추가
      if (value.length <= 3) {
        e.target.value = value;
      } else if (value.length <= 6) {
        e.target.value = value.slice(0, 3) + "-" + value.slice(3);
      } else if (value.length <= 10) {
        e.target.value = value.slice(0, 3) + "-" + value.slice(3, 6) + "-" + value.slice(6);
      } else {
        e.target.value = value.slice(0, 3) + "-" + value.slice(3, 7) + "-" + value.slice(7);
      }

      const formatted = e.target.value;
      if (formatted && !TEL_REGEX.test(formatted)) {
        showError(telErr, "연락처 형식은 010-1234-5678 입니다.");
      } else {
        showError(telErr, "");
      }
      refreshSaveButtonState();
    });

    // 🔥 붙여넣기 시에도 동일하게 처리
    managerTelInput.addEventListener("paste", (e) => {
      e.preventDefault();
      const pastedText = (e.clipboardData || window.clipboardData).getData("text");
      const numbers = pastedText.replace(/[^0-9]/g, "");
      
      if (numbers.length > 11) {
        managerTelInput.value = numbers.slice(0, 11);
      } else {
        managerTelInput.value = numbers;
      }
      
      // input 이벤트 트리거
      managerTelInput.dispatchEvent(new Event("input"));
    });
  }

  if (managerEmailInput) {
      // 🔥 이메일 입력 시 한글 입력 방지 및 실시간 검증
      managerEmailInput.addEventListener("input", (e) => {
        // 한글 및 공백 제거
        let value = e.target.value.replace(/[ㄱ-ㅎ|ㅏ-ㅣ|가-힣\s]/g, "");
        e.target.value = value;

        const val = value.trim();
        
        if (val && !EMAIL_REGEX.test(val)) {
          showError(emailErr, "이메일 형식이 올바르지 않습니다.");
        } else {
          showError(emailErr, "");
        }
        
        refreshSaveButtonState();
      });

      // 🔥 한글 입력 자체를 막기 (compositionstart/end 이벤트)
      let isComposing = false;
      
      managerEmailInput.addEventListener("compositionstart", () => {
        isComposing = true;
      });
      
      managerEmailInput.addEventListener("compositionend", (e) => {
        isComposing = false;
        // 한글이 입력되었다면 제거
        const value = e.target.value.replace(/[ㄱ-ㅎ|ㅏ-ㅣ|가-힣\s]/g, "");
        e.target.value = value;
        e.target.dispatchEvent(new Event("input"));
      });
    }

  /* ============================================
     ✅ submit 최종 방어
  ============================================ */
  if (form) {
    form.addEventListener("submit", (e) => {
      hasTouchedDeliveryDate = true;

      // 1. 납기일 검증
      if (!validateDeliveryDate(true)) {
        e.preventDefault();
        e.stopPropagation();
        return;
      }

      // 2. 제품 필수 검증
      if (!validateOrderItems()) {
        e.preventDefault();
        e.stopPropagation();
        return;
      }

      // 3. 담당자 정보 검증
      if (!validateManagerInfo(true)) {
        e.preventDefault();
        e.stopPropagation();
        alert("담당자 정보를 올바르게 입력해주세요.");
        return;
      }

      // 4. 주소 검증
      if (!validateAddress(true)) {
        e.preventDefault();
        e.stopPropagation();
        return;
      }
    });
  }

  /* ============================================
     1) 제품 목록 추가 버튼
  ============================================ */
  if (addItemBtn) {
    addItemBtn.addEventListener("click", () => {

      if (!itemBody) return;

      const index = itemBody.querySelectorAll("tr").length;

      const row = document.createElement("tr");
      row.innerHTML = `
        <td>
          <select class="form-select prd-select"
                  name="items[${index}][prdId]" required>
            <option value="">-- 선택 --</option>
            ${productList.map(p =>
              `<option value="${p.prdId}"
                       data-price="${p.unitPrice}"
                       data-minqty="${p.minQty}"
                       data-unit="${p.prdUnit}">
                ${p.prdName}
              </option>`).join("")}
          </select>
        </td>

        <td>
          <input type="number" class="form-control price-input"
                 name="items[${index}][unitPrice]" readonly>
        </td>

        <td>
          <input type="number" class="form-control minqty-input"
                 name="items[${index}][minQty]" readonly>
        </td>

        <td>
          <input type="text" class="form-control unit-input"
                 name="items[${index}][unit]" readonly>
        </td>

        <td>
          <input type="number" class="form-control qty-input"
                 name="items[${index}][qty]" required>
        </td>

        <td>
          <input type="number" class="form-control amount-input"
                 name="items[${index}][amount]" readonly>
        </td>

        <td class="text-center">
          <button type="button" class="btn btn-sm btn-danger delBtn">X</button>
        </td>
      `;

      itemBody.appendChild(row);

      const prdSelect = row.querySelector(".prd-select");
      const priceInput = row.querySelector(".price-input");
      const minQtyInput = row.querySelector(".minqty-input");
      const unitInput = row.querySelector(".unit-input");
      const qtyInput = row.querySelector(".qty-input");
      const amountInput = row.querySelector(".amount-input");

      // 삭제
      row.querySelector(".delBtn").addEventListener("click", () => {
        row.remove();
        refreshSaveButtonState();
      });

      // 제품 선택 → 자동 입력
      prdSelect.addEventListener("change", () => {
        const opt = prdSelect.selectedOptions[0];

        const unitPrice = parseInt(opt.dataset.price) || 0;
        const minQty = parseInt(opt.dataset.minqty) || 0;
        const unit = opt.dataset.unit ?? "";

        priceInput.value = unitPrice;
        minQtyInput.value = minQty;
        unitInput.value = unit;

        let qty = parseInt(qtyInput.value) || 0;
        if (qty < minQty) qty = minQty;
        if (qty % 10 !== 0) qty = Math.ceil(qty / 10) * 10;

        qtyInput.value = qty;
        amountInput.value = qty * unitPrice;

        refreshSaveButtonState();
      });

      // 수량 입력
      qtyInput.addEventListener("input", () => {
        let qty = parseInt(qtyInput.value) || 0;
        const minQty = parseInt(minQtyInput.value) || 0;

        if (qty < minQty) qty = minQty;
        if (qty % 10 !== 0) qty = Math.ceil(qty / 10) * 10;

        qtyInput.value = qty;
        amountInput.value = qty * (parseInt(priceInput.value) || 0);

        refreshSaveButtonState();
      });

      refreshSaveButtonState();
    });
  }

  /* ============================================
     2) 거래처 자동완성 검색
  ============================================ */
  const clientSearch = document.getElementById("clientSearch");
  const autoList = document.getElementById("clientAutoList");

  function resetClientInfo() {
    document.getElementById("clientInfoBox")?.classList.add("d-none");

    const fields = [
      "clientCeo", "clientManager", "clientManagerTel",
      "clientManagerEmail", "clientBizNo",
      "clientPostcode", "clientAddr", "clientAddrDetail"
    ];

    fields.forEach(id => {
      const el = document.getElementById(id);
      if (el) el.value = "";
    });

    refreshSaveButtonState();
  }

  if (clientSearch && autoList) {
    clientSearch.addEventListener("input", () => {
      const keyword = clientSearch.value.trim();

      resetClientInfo();

      if (keyword.length < 1) {
        autoList.innerHTML = "";
        autoList.classList.add("d-none");
        return;
      }

      fetch(apiUrl(`sales/orders/search-customer?keyword=${encodeURIComponent(keyword)}`))
        .then(r => r.json())
        .then(list => {
          if (!list || list.length === 0) {
            autoList.innerHTML = "";
            autoList.classList.add("d-none");
            return;
          }

          autoList.innerHTML = list.map(c => `
            <button type="button"
                    class="list-group-item list-group-item-action auto-item"
                    data-client-id="${c.clientId}"
                    data-client-name="${c.clientName}">
              ${c.clientName}
            </button>
          `).join("");

          autoList.classList.remove("d-none");
        })
        .catch(err => console.error("검색 오류", err));
    });

    // 외부 클릭 시 자동완성 닫기
    document.addEventListener("click", (e) => {
      if (!e.target.closest("#clientAutoList") && e.target.id !== "clientSearch") {
        autoList.classList.add("d-none");
      }
    });
  }

  /* 거래처 선택시 detail 자동 세팅 */
  document.addEventListener("click", (e) => {
    if (!e.target.classList.contains("auto-item")) return;

    const clientId = e.target.dataset.clientId;
    const clientName = e.target.dataset.clientName;

    document.getElementById("clientSearch").value = clientName;
    document.getElementById("clientId").value = clientId;

    autoList.classList.add("d-none");

    fetch(apiUrl(`sales/client/detail/${clientId}`))
      .then(res => res.json())
      .then(data => {
        document.getElementById("clientInfoBox")?.classList.remove("d-none");

        document.getElementById("clientCeo").value = data.ceoName ?? "";
        document.getElementById("clientManager").value = data.managerName ?? "";
        document.getElementById("clientManagerTel").value = data.managerTel ?? "";
        document.getElementById("clientManagerEmail").value = data.managerEmail ?? "";
        document.getElementById("clientBizNo").value = data.businessNo ?? "";

        document.getElementById("clientPostcode").value =
          data.postCode ?? data.dPostcode ?? data.postcode ?? data.zonecode ?? "";

        document.getElementById("clientAddr").value = data.addr ?? "";
        document.getElementById("clientAddrDetail").value = data.addrDetail ?? "";

        refreshSaveButtonState();
      })
      .catch(err => console.error("거래처 정보 조회 오류", err));
  });

  /* ============================================
     3) 카카오 주소 검색
  ============================================ */
  const addrSearchBtn = document.getElementById("addrSearchBtn");
  if (addrSearchBtn) {
    addrSearchBtn.addEventListener("click", () => {
      new daum.Postcode({
        oncomplete: function (data) {
          const addr = data.roadAddress ? data.roadAddress : data.jibunAddress;

          document.getElementById("clientPostcode").value = data.zonecode;
          document.getElementById("clientAddr").value = addr;

          document.getElementById("clientAddrDetail").value = "";
          document.getElementById("clientAddrDetail").focus();

          refreshSaveButtonState();
        }
      }).open();
    });
  }

  // ✅ 최초 상태 갱신
  refreshSaveButtonState();
});