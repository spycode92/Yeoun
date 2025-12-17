/* ============================
    CSRF 토큰 공통 처리
 ============================ */
 function getCsrf() {
   const tokenMeta = document.querySelector('meta[name="_csrf_token"]');
   const headerMeta = document.querySelector('meta[name="_csrf_headerName"]');

   if (!tokenMeta || !headerMeta) {
     alert("CSRF 메타 태그를 찾을 수 없습니다.");
     throw new Error("CSRF meta missing");
   }

   return {
     token: tokenMeta.content,
     header: headerMeta.content
   };
 }
 /* ============================
	  상태값 한글처리
	============================ */
 
 document.addEventListener("DOMContentLoaded", () => {
    const statusEl = document.getElementById("orderStatusText");
    if (!statusEl) return;

    const statusMap = {
      REQUEST: "요청",
      RECEIVED: "수주접수",
      CONFIRMED: "수주확정",
      PLANNED: "생산계획",
      SHIPPED: "출하완료",
      CANCEL: "취소"
    };

    const rawStatus = statusEl.innerText.trim();
    statusEl.innerText = statusMap[rawStatus] ?? rawStatus;
  });

 /* ============================
    입금 확인
 ============================ */
 function confirmPayment(btn) {
   const orderId = btn.dataset.orderId;
   if (!orderId) {
     alert("orderId가 없습니다.");
     return;
   }

   if (!confirm("입금 확인 처리하시겠습니까?")) return;

   const csrf = getCsrf();

   fetch(`/sales/orders/${orderId}/confirm`, {
     method: "POST",
     headers: {
       [csrf.header]: csrf.token
     }
   })
   .then(res => {
     if (!res.ok) throw new Error("입금확인 처리 실패");
     return res.text();
   })
   .then(() => {
     alert("입금 확인 처리되었습니다.");
     location.reload();
   })
   .catch(err => {
     console.error(err);
     alert(err.message);
   });
 }

 /* ============================
    수주 취소
 ============================ */
 function cancelOrder(btn) {
   const orderId = btn.dataset.orderId;
   if (!orderId) {
     alert("orderId가 없습니다.");
     return;
   }

   if (!confirm("수주를 취소하시겠습니까?")) return;

   const csrf = getCsrf();

   fetch(`/sales/orders/${orderId}/cancel`, {
     method: "POST",
     headers: {
       [csrf.header]: csrf.token
     }
   })
   .then(res => {
     if (!res.ok) throw new Error("취소 처리 실패");
     return res.text();
   })
   .then(() => {
     alert("수주가 취소되었습니다.");
     location.reload();
   })
   .catch(err => {
     console.error(err);
     alert(err.message);
   });
 }