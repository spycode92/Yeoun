/**
 * [ 웹소켓 설정 ]
 *
 * Thymeleaf에서 받을 값
 * window.roomId
 * window.targetEmpId
 * window.senderId
 * window.csrfToken
 * window.csrfHeader
 *
 * HTML 요소 :
 * #chat-body, #message, #send, #fileInput, #attachment-preview, #groupYn
 *
 */

// ===============================
//  전역 변수
// ===============================

let roomId      = window.roomId      ?? null; // 현재 방 ID
let senderId    = window.senderId    ?? null; // 로그인 사용자 ID
let targetEmpId = window.targetEmpId ?? null; // 1:1 새 방 생성 대상
let groupYn     = document.getElementById("groupYn")?.value ?? "N";

console.log("groupYn:", groupYn);

if (!senderId) {
	console.warn("senderId가 설정되지 않았습니다. chat.html에서 window.senderId 설정 필요");
}

// 파일 첨부 관련
const fileInput         = document.getElementById("fileInput");
const attachmentPreview = document.getElementById("attachment-preview");
const messageTextarea   = document.getElementById("message");
const dropZone          = document.getElementById("chat-body");

let selectedFiles = [];   // 현재 선택된 파일 목록

//===============================
//  소켓 연결 후 방 구독
//===============================

connectWebSocket(() => {
	if (roomId) {
		subscribeRoom(roomId);
		sendRead();	// 읽음 이벤트 전송
	}
});

window.addEventListener("focus", sendRead);	// 방이 포커스 될 때도 읽음 이벤트 전송

//===============================
//  방 구독
//===============================

function subscribeRoom(roomId) {

	// STOMP 연결 전이면
	if (!connected) {
		console.warn("STOMP가 아직 연결되지않았습니다. 구독 불가.");
		return;
	}

	// 방 ID가 없으면 (새 방이거나 오류)
	if (!roomId) {
		console.warn("roomId가 없습니다. 구독 불가.");
		return;
	}

	// 1) 메시지 수신 구독
	stompClient.subscribe(`/topic/chat/room/${roomId}`, (message) => {
	    renderMyMessage(JSON.parse(message.body));
	});

	// 2) 읽음 이벤트 수신 구독
	stompClient.subscribe(`/topic/chat/room/${roomId}/read`, (message) => {
		readMessage(JSON.parse(message.body));
	});

	// 3) 방 퇴장 이벤트 수신 구독
	stompClient.subscribe(`/topic/chat/room/${roomId}/leave`, (message) => {
		showUserLeft(JSON.parse(message.body));
	});
	
	console.log("방 구독 완료! : ", roomId);
}

// ========================================
// 파일 타입 판별 (이미지 / 텍스트 / 기타)
// ========================================

function detectFileType(mime, name) {
	if (mime?.startsWith("image/")) return "IMAGE";
	if (mime === "text/plain" || name?.toLowerCase().endsWith(".txt")) return "TEXT";
	return "OTHER";
}

// ========================================
// [ 메시지 렌더링 ]
//
// MessageEventDTO 기반:
//  - msgId, roomId, senderId, senderName, senderProfile
//  - msgType (TEXT/FILE/SYSTEM)
//  - msgContent
//  - fileCount
//  - files (FileAttachDTO 리스트)
//  - preview
//  - sentTime (yyyy-MM-dd 오전/오후 HH:mm)
//
// ========================================

function renderMyMessage(eventDTO) {
	const chatBody = document.getElementById("chat-body");
	if (!chatBody) return;

	const isMe = eventDTO.senderId === senderId ||
				eventDTO.senderId === null ||
				eventDTO.senderId === undefined;
	const time = formatTimeFull();

	// SYSTEM 메시지 따로 처리 분기점 ===================================
	if (eventDTO.msgType === "SYSTEM") {
		const row = document.createElement("div");
		row.className = "msg-row system";
		row.innerHTML = `
			<div class="system-text">
				${eventDTO.msgContent ?? ""}
			</div>
		`;
		chatBody.appendChild(row);
		chatBody.scrollTop = chatBody.scrollHeight;
		return;
	}
	// ==============================================================

	const row = document.createElement("div");
	row.className = "msg-row " + (isMe ? "right" : "left");
	row.setAttribute("data-msg-id", eventDTO.msgId);

	let bubbleHtml = "";


	// 파일 메시지
	if (eventDTO.msgType === "FILE" && eventDTO.files?.length > 0) {
		bubbleHtml += `<div class="file-bundle">`;

		eventDTO.files.forEach(file => {
			const type = detectFileType(file.category, file.originFileName);

			if (type === "IMAGE") {
				// 이미지 파일: 미리보기 + 다운로드 링크
				bubbleHtml += `
                    <div class="file-item image" data-file-id="${file.fileId}">
						<a href="/files/download/${file.fileId}" target="_blank">
							<img src="/uploads/${file.filePath}/${file.fileName}" class="file-thumb">
						</a>
						<span class="file-name">${file.originFileName}</span>
						<a class="file-download" href="/files/download/${file.fileId}">
							다운로드
						</a>
					</div>
                `;
			} else if (type === "TEXT") {
				bubbleHtml += `
				  <div class="file-item text" data-file-id="${file.fileId}">
					<i class="bi bi-file-text"></i>
					<span>${file.originFileName}</span>
					<a class="file-download" href="/files/download/${file.fileId}">
						다운로드
					</a>
				  </div>
				`;
			} else {
				bubbleHtml += `
				  <div class="file-item other" data-file-id="${file.fileId}">
					<i class="bi bi-file-earmark"></i>
					<span>${file.originFileName}</span>
					<a class="file-download" href="/files/download/${file.fileId}">
						다운로드
					</a>
				  </div>
				`;
			}
		});

		bubbleHtml += `</div>`;
	}

	// 텍스트 메시지
	if (eventDTO.msgType === "TEXT") {
		const content = eventDTO.msgContent ?? eventDTO.preview ?? "";
		if (isMe) {
			bubbleHtml += `<div class="msg-bubble msg-right">${content}</div>`;
		} else {
			bubbleHtml += `<div class="msg-bubble msg-left">${content}</div>`;
		}
	}

	if (isMe) {
		// 오른쪽 (내 메시지)
		row.innerHTML = `
			<div class="msg-meta">
				<span>${time}</span>
				<i class="bi bi-check2 read-check"></i>
			</div>
			${bubbleHtml}
		`;
	} else {
		// 왼쪽 (상대 메시지)
		const profileImgIndex = eventDTO.senderProfile ?? 1;

		row.innerHTML = `
			<img src="/img/msg_img_${profileImgIndex}.png" class="profile-img">
		
			<div class="msg-main">
				<div class="sender-name">${eventDTO.senderName ?? ""}</div>
				${bubbleHtml}
			</div>

			<div class="msg-time msg-time-left">${time}</div>
		`;
	}

	chatBody.appendChild(row);
	chatBody.scrollTop = chatBody.scrollHeight;

	if (!bubbleHtml) {
		console.warn("빈 메시지 렌더링 방지", eventDTO);
		return;
	}

}

// ===============================
//  파일 첨부: 타입 판별 (미리보기용)
// ===============================

function getFileTypeForPreview(file) {
	if (file.type.startsWith("image/")) return "image";
	if (file.type === "text/plain" || file.name.toLowerCase().endsWith(".txt")) return "text";
	return "other";
}

// ===============================
//  파일 미리보기 렌더링
// ===============================

function renderAttachmentPreview() {
	if (!attachmentPreview) return;

	attachmentPreview.innerHTML = "";

	if (!selectedFiles.length) {
		attachmentPreview.style.display = "none";
		return;
	}

	attachmentPreview.style.display = "flex";

	selectedFiles.forEach((file, index) => {
		const type = getFileTypeForPreview(file);

		const chip = document.createElement("div");
		chip.className = "preview-chip";
		chip.dataset.index = index;

		const thumb = document.createElement("div");
		thumb.className = "thumb";

		if (type === "image") {
			const img = document.createElement("img");
			img.src = URL.createObjectURL(file);
			img.onload = () => URL.revokeObjectURL(img.src);
			thumb.appendChild(img);
		} else if (type === "text") {
			const icon = document.createElement("i");
			icon.className = "bi bi-file-text";
			thumb.appendChild(icon);
		} else {
			const icon = document.createElement("i");
			icon.className = "bi bi-file-earmark";
			thumb.appendChild(icon);
		}

		const nameSpan = document.createElement("span");
		nameSpan.className = "name";
		nameSpan.textContent = file.name;

		const removeBtn = document.createElement("button");
		removeBtn.className = "remove";
		removeBtn.type = "button";
		removeBtn.innerHTML = "&times;";

		chip.appendChild(thumb);
		chip.appendChild(nameSpan);
		chip.appendChild(removeBtn);

		attachmentPreview.appendChild(chip);
	});
}

// ===============================
//  file input 변경 시
// ===============================

if (fileInput) {
	fileInput.addEventListener("change", (e) => {
		const files = Array.from(e.target.files || []);

		files.forEach(file => {
			const type = getFileTypeForPreview(file);

			if (type === "image") {
				// 이미지 파일은 여러 개 허용
				selectedFiles.push(file);
			} else {
				// 텍스트/기타 파일은 한 개만 허용
				selectedFiles = selectedFiles.filter(existing => {
					const existingType = getFileTypeForPreview(existing);
					return existingType === "image";
				});
				selectedFiles.push(file);
			}
		});

		// input.files 재구성
		const dt = new DataTransfer();
		selectedFiles.forEach(f => dt.items.add(f));
		fileInput.files = dt.files;

		renderAttachmentPreview();
	});
}

// ===============================
//  미리보기의 X 버튼으로 첨부 제거
// ===============================

if (attachmentPreview) {
	attachmentPreview.addEventListener("click", (e) => {
		if (!e.target.classList.contains("remove")) return;

		const chip  = e.target.closest(".preview-chip");
		const index = Number(chip.dataset.index);

		selectedFiles.splice(index, 1);

		const dt = new DataTransfer();
		selectedFiles.forEach(f => dt.items.add(f));
		if (fileInput) fileInput.files = dt.files;

		renderAttachmentPreview();
	});
}

// ===============================
//  드래그 앤 드롭 파일 첨부
// ===============================

if (dropZone) {
	["dragenter", "dragover", "dragleave", "drop"].forEach(eventName => {
		dropZone.addEventListener(eventName, (e) => {
			e.preventDefault();
			e.stopPropagation();
		});
	});

	dropZone.addEventListener("dragover", () => {
		dropZone.classList.add("drag-active");
	});

	dropZone.addEventListener("dragleave", () => {
		dropZone.classList.remove("drag-active");
	});

	dropZone.addEventListener("drop", (e) => {
		dropZone.classList.remove("drag-active");

		const droppedFiles = Array.from(e.dataTransfer.files || []);

		droppedFiles.forEach(file => {
			const type = getFileTypeForPreview(file);

			if (type === "image") {
				selectedFiles.push(file);
			} else {
				selectedFiles = selectedFiles.filter(existing => {
					const existingType = getFileTypeForPreview(existing);
					return existingType === "image";
				});
				selectedFiles.push(file);
			}
		});

		const dt = new DataTransfer();
		selectedFiles.forEach(f => dt.items.add(f));
		if (fileInput) fileInput.files = dt.files;

		renderAttachmentPreview();
	});
}

// ===============================
//  파일이 포함된 메시지 전송 (기존 방)
//   → /messenger/chat/{roomId} 로 multipart
//   → 서버: saveMessage + broadcastMessage
//   → 클라이언트: STOMP 이벤트(renderMessage)로만 렌더
// ===============================

async function sendMessageWithFiles() {

	if (!roomId) {
		return;
	}

	const text     = messageTextarea?.value.trim() ?? "";
	const hasFiles = selectedFiles.length > 0;

	if (!text && !hasFiles) return;

	const messageDTO = {
		msgContent: text,
		msgType: hasFiles ? "FILE" : "TEXT",
		roomId: roomId,
		senderId: senderId
	};

	const formData = new FormData();
	formData.append(
		"message",
		new Blob([JSON.stringify(messageDTO)], { type: "application/json" })
	);

	if (hasFiles) {
		selectedFiles.forEach(file => {
			formData.append("files", file);
		});
	}

	try {
		const res = await fetch(`/messenger/chat/${roomId}`, {
			method: "POST",
			headers: {
				[csrfHeader]: csrfToken
			},
			body: formData
		});

		if (!res.ok) {
			console.error("전송 실패", res.status);
			alert("파일/메시지 전송에 실패했습니다.");
			return;
		}

		// 전송 성공 후 입력/첨부 초기화
		if (messageTextarea) messageTextarea.value = "";
		selectedFiles = [];
		if (fileInput) fileInput.value = "";
		renderAttachmentPreview();

		// 실제 메시지 렌더링은 STOMP 이벤트(renderMessage)가 담당

	} catch (e) {
		console.error("전송 오류:", e);
	}
}

// ===============================
//  텍스트/파일 통합 메시지 전송
// ===============================

async function sendChatMessage() {
	if (!messageTextarea) return;

	const text     = messageTextarea.value.trim();
	const hasFiles = selectedFiles.length > 0;

	// 새 방인 경우
	if (!roomId) {
		if (!targetEmpId) {
			console.error("targetEmpId가 없습니다. 신규 방 생성 불가.");
			return;
		}

		if (hasFiles) {
			alert("일단 텍스트로 방을 열고 파일을 보내주세요.");
			return;
		}

		if (!text) return;

		const res = await fetch("/messenger/chat", {
			method: "POST",
			headers: {
				"Content-Type": "application/json",
				[csrfHeader]: csrfToken
			},
			body: JSON.stringify({
				members: [targetEmpId, senderId],
				createdUser: senderId,
				groupYn: "N",
				firstMessage: text,
				msgType: "TEXT"
			})
		});

		const data = await res.json();
		roomId = data.roomId;
		console.log("새 방 생성 data : ", data);

		subscribeRoom(roomId);

		// 첫 메시지 직접 렌더
		renderMyMessage({
			msgId: data.firstMsgId ?? null,
			roomId: roomId,
			senderId: senderId,
			senderName: "나",
			msgType: "TEXT",
			msgContent: text,
			sentTime: formatTimeFull()
		});

		messageTextarea.value = "";
		return;
	}

	// 기존 방인 경우
	if (hasFiles) {
		// 파일이 하나라도 있으면 multipart 경로 사용
		await sendMessageWithFiles();
		return;
	}

	// 파일 없이 텍스트만 있을 때 → STOMP로 전송
	if (!text) return;

	const payload = {
		roomId: roomId,
		senderId: senderId,
		msgContent: text,
		msgType: "TEXT"
	};

	// 서버에 텍스트만 STOMP 전송
	stompClient.send("/app/chat/send", {}, JSON.stringify(payload));

	// 메시지 렌더링은 STOMP broadcast → renderMessage 가 처리
	messageTextarea.value = "";
}

//===============================
//  이벤트 바인딩
//===============================

document.getElementById("send")?.addEventListener("click", sendChatMessage);

document.getElementById("message")?.addEventListener("keydown", function (e) {
	if (e.key === "Enter" && !e.shiftKey) {
		e.preventDefault();
		sendChatMessage();
	}
});

//===============================
//  읽음 전송
//===============================

function sendRead() {
	if (!stompClient || !connected || !roomId || !senderId) return;

	const payload = {
		roomId: roomId,
		readerId: senderId,
		groupYn: groupYn
	};

	stompClient.send("/app/chat/read", {}, JSON.stringify(payload));
}

//===============================
//  읽음 수신 처리
//===============================

function readMessage(dto) {

	if (dto.readerId === senderId) return;

	const rows = document.querySelectorAll('.msg-row.right .read-check');

	rows.forEach(icon => {
		icon.classList.remove("bi-check2");
		icon.classList.add("bi-check2-all");
		icon.style.color = "#6a5acd";
	});
}

//===============================
//  방 퇴장 전송
//===============================

function leaveRoom() {
	if (!stompClient || !connected || !roomId) return;

	const payload = {
		roomId: roomId,
		empId: senderId
	};

	stompClient.send("/app/room/leave", {}, JSON.stringify(payload));
}

//===============================
//  퇴장 메시지 렌더링
//===============================

function showUserLeft(dto) {
	if (groupYn === "N") return;

	const chatBody = document.getElementById("chat-body");
	if (!chatBody) return;

	const row = document.createElement("div");
	row.className = "msg-row system";
	const name = dto.empName || "알 수 없음";

	row.innerHTML = `
		<div class="system-text">
			${name} 님이 방을 나갔습니다.
		</div>
	`;

	chatBody.appendChild(row);
	chatBody.scrollTop = chatBody.scrollHeight;
}

// 초기화시 미리보기 숨김
document.addEventListener("DOMContentLoaded", () => {
	selectedFiles = [];
	renderAttachmentPreview();
});

// 맨 아래에서 페이지 시작
document.addEventListener("DOMContentLoaded", () => {
	selectedFiles = [];
	renderAttachmentPreview();

	const chatBody = document.getElementById("chat-body");
	if (chatBody) {
		setTimeout(() => {
			chatBody.scrollTop = chatBody.scrollHeight;
		}, 50);
	}
});
