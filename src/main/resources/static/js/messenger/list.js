/**
 *  messenger/list 파일의 js
 */

// ==========================
// DOM 요소 참조 / 전역 상태
// ==========================
const tabFriends		= document.getElementById('tab-friends'); // 친구목록 탭
const tabChats 		= document.getElementById('tab-chats'); // 대화목록 탭
const friendsPanel 	= document.getElementById('friends-panel'); // 친구패널
const chatsPanel 		= document.getElementById('chats-panel'); // 대화패널
const headerTitle 		= document.getElementById('header-title'); // 헤더(친구목록-대화목록 텍스트 전환)
const groupButton		= document.getElementById('group-button'); // 그룹채팅 시작 버튼

const searchInput		= document.querySelector('.chat-search input'); // 검색창
const searchButton		= document.querySelector('.chat-search span'); // 검색버튼

const statusIndicator	= document.getElementById('status-indicator');	// 내 상태
const statusText		= document.getElementById('status-text'); // 내 상태 텍스트
const workStatus		= document.getElementById('work-status'); // 수동 근무 상태


//현재 모드: 'friend' / 'chat'
let currentMode = 'friend';

//===============================
//  소켓 연결 후 방 구독
//===============================

connectWebSocket(() => {
	subscribeEvent();
	//sendRead();	// 읽음 이벤트 전송
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
	
	// 1) 친구 상태 변경 구독
	stompClient.subscribe(`/topic/status/change`, (message) => {
	    changeStatus(JSON.parse(message.body));
	});
	
	// 2) 메시지 수신 구독
	stompClient.subscribe(`/user/queue/messenger`, (message) => {
	    receiveNewMessage(JSON.parse(message.body));
	});
}

// ==========================
// 채팅 목록 AJAX 불러오기
// ==========================
async function loadChatList() {

    try {
        const res = await fetch("/messenger/list/chat", {
            method: "POST",
            headers: {
                "Accept": "application/json",
                [csrfHeader] : csrfToken
            }
        });

        if (!res.ok) {
            console.error("채팅 목록 로딩 실패", res.status);
            return;
        }

        const list = await res.json();

        const panel = document.querySelector("#chats-panel");
        panel.innerHTML = ""; // 기존꺼 지움

        if (list.length === 0) {
            panel.innerHTML = `<p class="text-muted p-3">대화 내역이 없습니다.</p>`;
            return;
        }

        list.forEach(room => {
            const item = document.createElement("div");
            item.className = "chat-item";
            item.dataset.id = room.roomId;

            item.innerHTML = `
        <img class="rounded-circle" src="/img/msg_img_${room.profileImg}.png">

        <div class="chat-center">
          <p class="chat-title">${room.groupName ?? ''}</p>
          <p class="chat-last">${room.previewMessage ?? ''}</p>
        </div>

        <div class="chat-right">
          <span class="chat-time">${room.previewTime ?? ''}</span>
          ${
                room.unreadCount > 0
                    ? `<span class="badge-unread">${room.unreadCount}</span>`
                    : ""
            }
        </div>
      `;

            // ==========================
            // 더블클릭으로 창 열기
            // ==========================
            item.addEventListener("dblclick", () => {
                window.open(
                    `/messenger/room/${room.roomId}`,
                    "_blank",
                    "width=500,height=700,resizable=no,scrollbars=no"
                );
            });

            panel.appendChild(item);
        });

    } catch (err) {
        console.error("채팅 목록 불러오기 에러:", err);
    }
}


// ==========================
// 상태 실시간 변화 구독
// ==========================
function changeStatus(req){
	
	console.log("changeStatus 진입............... ", req);
	
	// 해당 친구 DOM 찾기
	const item = document.querySelector(`.friend-item [data-id="${req.empId}"]`);
	console.log("changeStatus 안의 item.......... ", item);
	if (!item) return;

	const dot = item.closest(".friend-item").querySelector(".status-dot");
	console.log("changeStatus 안의 dot.......... ", dot);
	if (!dot) return;

	// 1) dot 색상 변경
	dot.classList.remove("online", "offline", "away", "busy");
	
	switch (req.avlbStat) {
	    case "ONLINE":  dot.classList.add("online");  break;
	    case "AWAY":    dot.classList.add("away");    break;
	    case "BUSY":    dot.classList.add("busy");    break;
	    case "OFFLINE": dot.classList.add("offline"); break;
	}

	// 2) 상태 변경 => 왜인지는 모르겠지만 ITEM 자체가 status-msg라서 필요없어진 코드!
	//const msg = item.getElementsByClassName("status-msg");
	//if (!msg) return;

	// 백엔드에서 보내는 workStat → UI 문구 매핑
	const workMap = {
	    WORKING: "근무 중",
	    MEETING: "회의 중",
	    CALL: "통화 중",
	    FOCUS: "집중 업무 중",
	    LUNCH: "식사 중",
	    OUTING: "외출 중",
	    FIELDWORK: "외근 중",
	    WFH: "재택근무 중",
	    VACATION: "휴가 중"
	};

	console.log("req.workStat::::::::::", req.workStat);
	// workStat이 null이면 "-"
	item.textContent = workMap[req.workStat] || "-";
}

// ==========================
// 상태 실시간 변화 구독
// ==========================
function receiveNewMessage(req){

    console.log("tabChats : ", tabChats);

	if (currentMode == 'friend'){
        tabChats.classList.add('has-alert');
	} else if (currentMode == 'chat'){
        tabChats.classList.remove('has-alert');

        // 1) 기존에 있던 같은 roomId 방 제거
        const existing = chatsPanel.querySelector(`.chat-item[data-id="${String(req.roomId)}"]`);
        if (existing){
            existing.remove();
        }

        // 2) 새 방 생성
        const item = document.createElement('div');
        item.classList.add('chat-item');
        item.dataset.id = req.roomId;

        item.innerHTML = `
            <img class="rounded-circle"
                 src="/img/msg_img_${req.profileImg}.png">
            
            <div class="chat-center">
                <p class="mb-0 fw-bold">${req.groupName}</p>
                <small class="text-muted">${req.preview}</small>
            </div>
            
            <div class="chat-right">
                <span class="chat-time">${req.sentTime}</span>
                ${req.unreadCount > 0
                    ? `<span class="badge-unread">${req.unreadCount}</span>`
                    : ''
                }
            </div>
        `;

        // 3) 최상단 추가
        chatsPanel.prepend(item);

	}

}


// ==========================
// 각 친구의 상태
// ==========================
document.querySelectorAll('.friend-item').forEach(item => {
  const dot = item.querySelector('.status-dot'); // 친구 상태를 나타내는 점
  const status = dot.dataset.status; // friend.status 값 ("ONLINE","OFFLINE","AWAY","BUSY")

  switch (status) {
    case "ONLINE":  // 온라인
      dot.classList.add("online");
      break;

    case "OFFLINE": // 오프라인
      dot.classList.add("offline");
      break;

    case "AWAY":    // 자리비움
      dot.classList.add("away");
      break;

    case "BUSY":    // 다른 용무중
      dot.classList.add("busy");
      break;

    default:		// 디폴트 = 오프라인
      dot.classList.add("offline");
  }
});

//==========================
// 대화창 검색 동작 후 대화창 렌더링
//==========================
function renderRoomList(rooms) {

    document.querySelectorAll('#chats-panel .chat-item').forEach(e => e.remove());

    const chatList = document.querySelector('.chat-list');
    const emptyChat = document.querySelector('.empty-chat');

    if (!chatList) {
        console.error("chat-list 컨테이너가 존재하지 않습니다.");
        return;
    }

    // 기존 내용 제거
    chatList.innerHTML = '';

    // 1) 검색 결과가 없을 경우 → emptyChat 보여주기
    if (!rooms || rooms.length === 0) {
        if (emptyChat) emptyChat.style.display = 'block';
        return;
    } else {
        if (emptyChat) emptyChat.style.display = 'none';
    }

    // 2) 검색 결과 렌더링
    rooms.forEach(room => {

        const item = document.createElement('div');
        item.classList.add('chat-item');
        item.dataset.id = room.roomId;

        item.innerHTML = `
            <img class="rounded-circle"
                 src="/img/msg_img_${room.profileImg}.png">

            <div class="chat-center">
                <p class="chat-title">${room.groupName}</p>
                <p class="chat-last">${room.previewMessage}</p>
            </div>

            <div class="chat-right">
                <span class="chat-time">${room.previewTime}</span>

                ${
                    room.unreadCount && room.unreadCount > 0
                        ? `<span class="badge-unread">${room.unreadCount}</span>`
                        : ''
                }
            </div>
        `;

        chatList.appendChild(item);
    });
}



// ==========================
// 검색 로직
// ==========================
	
// 친구 목록 필터링 (이름 및 부서)
function filterFriends()  {
    const keyword = searchInput.value.trim().toLowerCase();
    const items = document.querySelectorAll('.friend-item');

    items.forEach(item => {
      const name = item.querySelector('p').textContent.toLowerCase();
      const dept = item.querySelector('small').textContent.toLowerCase();
      
      const visible = name.includes(keyword) || dept.includes(keyword);
      item.style.display = visible ? 'flex' : 'none';
  });
}

// 대화 목록 필터링 (대화 상세내용)
function filterChats() {
    const keyword = searchInput.value.trim();
    if (!keyword) return;
    
    fetch(`/messenger/rooms/search?keyword=${encodeURIComponent(keyword)}`)
    .then(response => response.json())
    .then(rooms => {
    	renderRoomList(rooms);
    })
    .catch(err => console.error(err));

}


// 입력 변화 시: 친구 모드일 때만 실시간 필터링
searchInput.addEventListener('input', () => {
	if (currentMode === 'friend'){
		filterFriends();
	}
});

// 키보드로 검색 실행: 대화 모드에서만 Enter로 검색
searchInput.addEventListener('keydown', (event) => {
	if (currentMode === 'chat' && event.key === 'Enter') {
		filterChats();
	}
});

// 돋보기 클릭: 대화 모드일 때만 검색
searchButton.addEventListener('click', () => {
	if (currentMode === 'chat') {
 		filterChats();
 	}
 });

// ==========================
// 탭 전환
// ==========================

function activeFriendsTab() {
	currentMode = 'friend';
	
	tabFriends.classList.add('active');
	tabChats.classList.remove('active');
	
	friendsPanel.classList.remove('d-none');
	chatsPanel.classList.add('d-none');
	
	headerTitle.textContent = '친구 목록';
	groupButton.style.display = 'none';
	
	// 검색창 초기화
	searchInput.value = '';
	searchInput.placeholder = '이름/부서로 검색';
	
	// 친구가 다시 보이도록 함
	document.querySelectorAll('.friend-item').forEach(item => {
		item.style.display = 'flex';
	});
}

function activeChatsTab() {
	currentMode = 'chat';
	
	tabChats.classList.add('active');
	tabFriends.classList.remove('active');
	
	chatsPanel.classList.remove('d-none');
	friendsPanel.classList.add('d-none');
	
	headerTitle.textContent = '대화 목록';
	groupButton.style.display = 'block';
	
	// 검색창 초기화
	searchInput.value = '';
	searchInput.placeholder = '대화 상대/내용으로 검색';
	
	// 대화가 다시 보이도록 함
	document.querySelectorAll('.chat-item').forEach(item => {
		item.style.display = 'flex';
	});

    // ========= 추가 ==========
    loadChatList();

}
	
// 친구 탭 클릭
tabFriends.addEventListener('click', () => {
  activeFriendsTab();
  console.log("친구목록!!!");
});

// 대화 탭 클릭
tabChats.addEventListener('click', () => {
  activeChatsTab();
  console.log("대화목록!!!");
});


// ==========================
// 더블클릭 시 채팅창 팝업
// ==========================
document.querySelectorAll('.friend-item').forEach(item => {
  item.addEventListener('dblclick', () => {
    const targetId = item.dataset.id;
    window.open(
      'target/' + targetId,
      '_blank',
      'width=500,height=700,resizable=no,scrollbars=no');
  });
});

document.querySelectorAll('.chat-item').forEach(item => {
	item.addEventListener('dblclick', () => {
		const roomId = item.dataset.id;
		window.open(
			'room/' + roomId,
			'_blank',
			'width=500,height=700,resizable=no,scrollbars=no');
	});
});


// ==========================
// 상태 토글
// ==========================

let statuses = [
    { color: '#4CAF50', text: '온라인' },
    { color: '#FFC107', text: '자리비움' },
    { color: '#F44336', text: '다른 용무중' }
];

let current = 0;
let manuallySet = false;  // 수동 상태 변경 여부


// ==========================
// 서버로 상태 전송 함수
// ==========================
async function sendStatusToServer(presence, reason) {
	
	console.log("sendStatusToServer 진입!!!!!");
	console.log("presence... : " , presence);
	console.log("reason... : ", reason);
	
    try {
        const res = await fetch('/messenger/status', {
            method: 'PATCH',
            headers: {
                'Content-Type': 'application/json',
                [csrfHeader]: csrfToken
            },
            body: JSON.stringify({
                avlbStat: presence,     // ONLINE / AWAY / BUSY / OFFLINE
                workStat: reason        // MEETING / LUNCH / WORKING / etc
            })
        });

        const text = await res.text();
        console.log("상태 전송 완료... text:", text);

    } catch (err) {
        console.error("상태 전송 실패:", err);
    }
}

// =====================
// 수동 상태 변경
// =====================
statusIndicator.addEventListener('click', () => {
    manuallySet = true; // 자동 변경 잠시 차단

    current = (current + 1) % statuses.length;
    statusIndicator.style.backgroundColor = statuses[current].color;
    statusText.textContent = statuses[current].text;

    // 상태 매핑: 텍스트 → 코드
    let presenceCode = '';

    switch (statuses[current].text) {
        case '온라인':
            presenceCode = 'ONLINE';
            break;
        case '자리비움':
            presenceCode = 'AWAY';
            break;
        case '다른 용무중':
            presenceCode = 'BUSY';
            break;
        default:
            presenceCode = 'ONLINE';
    }

// 서버로 전송
    sendStatusToServer(presenceCode, null);

});



// =====================
// 자동 자리비움
// =====================
let idleTime = 0;
let autoAwayTime = 30 * 1000; // 30초

setInterval(() => {
    idleTime += 1000;

    // 수동 상태일 때는 자동 자리비움 X
    if (manuallySet) return;

    if (idleTime >= autoAwayTime) {
        statusIndicator.style.backgroundColor = '#FFC107';
        statusText.textContent = '자리비움';
    }
}, 1000);


// =====================
// 사용자 활동 감지 → 자동 ONLINE
// =====================
['mousemove', 'keydown', 'click', 'scroll'].forEach(evt => {
    document.addEventListener(evt, () => {
        idleTime = 0;

        // 수동 상태면 자동복귀 금지
        if (manuallySet) return;

        statusIndicator.style.backgroundColor = '#4CAF50';
        statusText.textContent = '온라인';
    });
});


// ==========================
// 업무 사유(workStatus) 선택 시 → 1차 상태 자동 변경
// ==========================
workStatus.addEventListener('change', () => {
	
	console.log("workStatus change.........");
	
    const value = workStatus.value;
	
	console.log("workStatus.value..........", value);

    // 업무 사유 선택은 수동 변경으로 취급 → 자동 자리비움 잠시 중지
    manuallySet = true;

    // 상태 매핑
    let newStatus = { color: '', text: '' };

    switch (value) {
        case 'MEETING':         // 회의 중
        case 'CALL':            // 통화 중
        case 'FOCUS':           // 집중 업무 중
            newStatus = { color: '#F44336', text: '다른 용무중' }; // BUSY
            break;

        case 'LUNCH':           // 식사 중
        case 'OUTING':          // 외출 중
        case 'FIELDWORK':       // 외근 중
            newStatus = { color: '#FFC107', text: '자리비움' }; // AWAY
            break;

        case 'VACATION':        // 휴가 중
            newStatus = { color: '#9E9E9E', text: '오프라인' }; // OFFLINE
            break;

        case 'WORKING':         // 근무 중
        case 'WFH':             // 재택근무 중
            newStatus = { color: '#4CAF50', text: '온라인' }; // ONLINE
            break;

        default:
            newStatus = { color: '#4CAF50', text: '온라인' };
    }

    // UI 적용
    statusIndicator.style.backgroundColor = newStatus.color;
    statusText.textContent = newStatus.text;
	
	console.log("newStatus.color............", newStatus.color);
	console.log("newStatus.text.............", newStatus.text);

    // ==========================
    // 업무 사유 선택 시 서버로 전송
    // ==========================
    const reasonMap = {
        'MEETING': 'MEETING',
        'CALL': 'CALL',
        'FOCUS': 'FOCUS',
        'LUNCH': 'LUNCH',
        'OUTING': 'OUTING',
        'FIELDWORK': 'FIELDWORK',
        'VACATION': 'VACATION',
        'WORKING': 'WORKING',
        'WFH': 'WFH'
    };

    const presenceMap = {
        'MEETING': 'BUSY',
        'CALL': 'BUSY',
        'FOCUS': 'BUSY',
        'LUNCH': 'AWAY',
        'OUTING': 'AWAY',
        'FIELDWORK': 'AWAY',
        'VACATION': 'OFFLINE',
        'WORKING': 'ONLINE',
        'WFH': 'ONLINE'
    };
    const reason = reasonMap[value];
    const presence = presenceMap[value];
	
	console.log("reasonMap[value].............", reasonMap);
	console.log("presenceMap[value].............", presenceMap);

    // 서버 전송
    sendStatusToServer(presence, reason);

});



// ==========================
// 즐겨찾기 핸들러
// ==========================
document.addEventListener("click", (event) => {
  if (!event.target.classList.contains("star-btn")) return;
  
  event.preventDefault();   // GET 방지
  event.stopPropagation();  // 버블링 방지
  
  const starBtn = event.target;
  const friendItem = event.target.closest(".friend-item");
  const container = document.querySelector("#friends-panel");
  
  const wasActive = starBtn.classList.contains("active");	// 토글 전 상태

  // 1. 별 토글 
  starBtn.classList.toggle("active");
  starBtn.classList.toggle("bi-star");
  starBtn.classList.toggle("bi-star-fill");
  
  const isNowActive = starBtn.classList.contains("active");	// 토글 후 상태
  
  // 2. 위치 이동 로직
  
	//==========================
	// 즐겨찾기 ON -> 맨 위로 이동
	//==========================
  if (!wasActive && isNowActive) {
	container.prepend(friendItem);
  } 
 
	//==========================
	// 즐겨찾기 OFF
	//==========================
  else if (wasActive && !isNowActive) {
	  const allItems = Array.from(container.querySelectorAll(".friend-item"));
	  const normalFriends = allItems.filter(item => 
	  	!item.querySelector(".star-btn").classList.contains("active")
	  );

	  normalFriends.sort((a, b) => {
		  const nameA = a.querySelector("p").textContent.trim();
		  const nameB = b.querySelector("p").textContent.trim();
		  return nameA.localeCompare(nameB, "ko");
	  });
	  
	  normalFriends.forEach(item => container.appendChild(item));
  }

  // 3. 서버에 즐겨찾기 상태 전송
  
  const id = starBtn.dataset.id;
  fetch(`/messenger/favorite/${id}`, {
    method: "PATCH",
    headers: {
    	[csrfHeader] : csrfToken
    },
    credentials: "include"
  })
  .then(res => res.text())
  .then(msg => {
      console.log("서버 응답", msg);
  })
  .catch(err => {
      console.error("즐겨찾기 오류:", err);
      // 실패 시 UI 롤백 가능
      starBtn.classList.toggle("active");
      starBtn.classList.toggle("bi-star");
      starBtn.classList.toggle("bi-star-fill");
  });

});


// ==========================
// 초기 진입 시 : 친구 탭 활성화
// ==========================
activeFriendsTab();



//==========================
// 모달 열기
//==========================
document.querySelector('.create-group-btn').addEventListener('click', () => {
 const modal = new bootstrap.Modal(document.getElementById('group-modal'));
 modal.show();
});

//========================================
// 선택 멤버 박스
//========================================
const selectedBox = document.querySelector('.selected-list');
const modalEl = document.getElementById('group-modal');

//========================================
// 1) 멤버 클릭 이벤트 등록 (한 번만 등록)
//========================================
document.querySelectorAll('#group-modal .member-item').forEach(item => {
 item.addEventListener('click', () => {
     item.classList.toggle('selected');  // 선택/해제
     updateSelectedBox();
 });
});
//========================================
// 2) 모달 열리기 "직전" 초기화 (show.bs.modal)
//========================================
modalEl.addEventListener('show.bs.modal', () => {

 // 이전 선택 UI 제거 (한 줄)
 selectedBox.innerHTML = '';

 // 모든 선택 해제
 document.querySelectorAll('#group-modal .member-item')
     .forEach(item => item.classList.remove('selected'));
});

//========================================
// 3) 선택된 멤버 박스 업데이트
//========================================
function updateSelectedBox() {
 const selectedItems = [...document.querySelectorAll('#group-modal .member-item.selected')];
 selectedBox.innerHTML = '';

 selectedItems.forEach(item => {
     const id = item.dataset.id;
     const name = item.querySelector('.name').textContent;

     const tag = document.createElement('div');
     tag.className = 'selected-tag';
     tag.innerHTML = `${name} <i class="bi bi-x-lg" data-id="${id}"></i>`;
     selectedBox.appendChild(tag);
 });
}

//========================================
// 4) X 버튼 클릭 → 선택 해제
//========================================
selectedBox.addEventListener('click', (e) => {
 if (!e.target.classList.contains('bi-x-lg')) return;

 const id = e.target.dataset.id;
 const original = document.querySelector(`#group-modal .member-item[data-id="${id}"]`);

 if (original) original.classList.remove('selected');

 updateSelectedBox();
});

//========================================
//5) 그룹 채팅방 생성
//========================================

document.getElementById('create-group-btn')
.addEventListener('click', async () => {

  // 1) 선택된 멤버 수집
  const members = [...document.querySelectorAll('#group-modal .member-item.selected')]
                    .map(item => item.dataset.id);

  if (members.length < 2) {
    alert("두 명 이상 선택해야 그룹채팅을 만들 수 있어요!");
    return;
  }

  // 2) 그룹명 (없으면 null)
  const groupNameInput = document.getElementById('group-name');
  const groupName = groupNameInput ? groupNameInput.value.trim() : null;

  // 3) createRoom 호출
  const data = await createRoom({
    members: members,
    groupYn: 'Y',              // 그룹 채팅
    groupName: groupName,      // 입력값 또는 null
    firstMessage: null,        // 그룹은 firstMessage 없음
    msgType: null,             // 그룹은 msgType 없음
    csrfHeader: csrfHeader,
    csrfToken: csrfToken
  });

  console.log(data);
  console.log("type!!!!!!!", typeof data);
  console.log("roomId :: ", data.roomId);
  console.log("groupName :: ", data.groupName);
  console.log("groupYn :: ", data.groupYn);

  if (!data.roomId) {
    alert("그룹 채팅방 생성에 실패했습니다.");
    return;
  }

  // 4) 모달 닫기
  const modal = bootstrap.Modal.getInstance(document.getElementById('group-modal'));
  modal.hide();

  // 5) 생성된 그룹 채팅방 오픈
  window.open(
    '/messenger/room/' + data.roomId + '?groupYn=' + data.groupYn,
    '_blank',
    'width=500,height=700,resizable=no,scrollbars=no'
  );
});

// ==========================
// 채팅방 클릭 → 해당 unread 제거
// ==========================
chatsPanel.addEventListener("dblclick", (event) => {

    // 가장 가까운 chat-item 찾기
    const item = event.target.closest(".chat-item");
    if (!item) return;

    // 채팅방 안의 unread badge 찾기
    const badge = item.querySelector(".badge-unread");
    if (badge) {
        badge.remove();
    }

});





