
//===============================
//  소켓 연결 후 방 구독
//===============================

connectWebSocket(() => {
    subscribeEvent();
});

//===============================
//  구독
//===============================

function subscribeEvent() {

    // STOMP 연결 전이면
    if (!connected) {
        console.warn("STOMP가 아직 연결되지않았습니다. 구독 불가.");
        return;
    }

    // 1) 메시지 수신 구독
    stompClient.subscribe(`/user/queue/messenger`, (message) => {
        receiveNewMessage(JSON.parse(message.body));
    });
	
	// 2) 페이지별 알림, 개인 알림 구독
    const section = getSectionPath();
    autoSubscribeByPath(section);
    subscribePersonalAlarm();
	
}


//===============================
//	메시지 수신 이벤트
//===============================
const chatBadge = document.querySelector("#badge_chat");
function receiveNewMessage(req){
	chatBadge.style.display = "flex";
}


//===============================
// 클릭 시 사라지는 이벤트 공통으로 빼기
//===============================
function hideElement(el) {
  if (!el) return;
  el.style.display = "none";
}

chatBadge.addEventListener("click", () => {
  console.log("클릭");
  hideElement(badge);
});

// ====================================================================

// -----------------------------------------------------------
// 현재 URL의 상위주소 받아오기
function getSectionPath() {
    const path  = window.location.pathname;
    const parts = path.split("/").filter(Boolean);
    if (parts.length === 0) return null;
    return parts[0];
}

//  URL의 상위주소 받아 공통 알림 채널 구독
function autoSubscribeByPath(sectionPath) {
    if (!sectionPath) return;

    stompClient.subscribe(`/alarm/${sectionPath}`, message => {
//        const payload = JSON.parse(message.body);
        const payload = message.body
        showRefreshBadge(payload);  // 섹션 공통 새로고침 뱃지 표시 함수
    });
}


function showRefreshBadge(payload) {
    // 레이아웃에있는 공통 뱃지 요소
    const badge = document.getElementById("refresh-badge");
    if (!badge) {
        console.warn("inventory-refresh-badge 요소가 없습니다.");
        return;
    }
	// 뱃지에 웹소켓으로 부터 받은 메세지를 표현할 스판요소
	const textSpan = badge.querySelector(".badge-text");
	if (textSpan) {
	    textSpan.textContent = payload || "데이터가 변경되었습니다. 새로고침 해주세요.";
	}
	// 뱃지보이기
    badge.style.display = "inline-flex";
	
	// 뱃지에 data-bound가 없을경우만이벤트 등록
	if (!badge.dataset.bound) {
		const refreshBtn = badge.querySelector(".badge-refresh-btn");
		const closeBtn   = badge.querySelector(".badge-close-btn");

		if (refreshBtn) {
			refreshBtn.addEventListener("click", () => {
				location.reload();
			});
	  	}
	
		if (closeBtn) {
			closeBtn.addEventListener("click", () => {
				badge.style.display = "none";
			});
		}
		// 뱃지에 data-bound = true 생성(이벤트중복생성방지)
	    badge.dataset.bound = "true";
	}
}

// 개인 알림이 도착했을 때 종에 표시, 드랍다운 알림에 new 표시
function subscribePersonalAlarm() {
	// '/alarm/${Username}' 주소로 개인 구독 주소 설정
    stompClient.subscribe("/user/alarm", message => {
//        const text = message.body;     
        showNewAlarm();
		showNewAlarmAtDropdown();
    });
}

// 헤더 종모양에 뱃지표시
function showNewAlarm() {
    const badgeBell = document.getElementById("badge_bell");
    if (!badgeBell) return;

//    badgeBell.textContent = "●";
    badgeBell.style.display = "inline-flex";

    // 클릭 시 숨기고, 알림 목록 열기까지 묶고 싶으면
    if (!badgeBell.dataset.bound) {
        const bell = document.getElementById("bell");
        if (bell) {
            bell.addEventListener("click", () => {
                badgeBell.style.display = "none";
            });
        }
        badgeBell.dataset.bound = "true";
    }
}
// 종클릭시 나오는 드랍다운에 new 표시
function showNewAlarmAtDropdown() {
	const alarmNewBadge = document.querySelector(".alarm-new-badge");
	alarmNewBadge.style.display = "inline";
}


// 읽지않은 알림 데이터 가져오기
async function getAlarmReadStatus() {
    try {
        const response = await fetch(`/alarm/status`, {
            method: "GET",
            headers: {
                [csrfHeader]: csrfToken,
                'Content-Type': 'application/json'
            }
        });
        
        if (!response.ok) {
            throw new Error("알림 데이터 로드 실패");
        }
        
        const alarmData = await response.json();
        console.log("알림 데이터:", alarmData);
        
        return alarmData;
        
    } catch (error) {
        console.error("알림 조회 오류:", error);
        alert("알림 목록을 불러오지 못했습니다.");
    }
}
// 페이지 로드시 안읽은 알림이 있는경우 알림뱃지 설정
(async () => {
	const alarmData = await getAlarmReadStatus();
	console.log('init alarmData:', alarmData, alarmData.length);
    if(alarmData.length != 0) {
		showNewAlarm();
		showNewAlarmAtDropdown();
	}
})();
// ----------------------------------------------------------------------
