/**
 * 
 *  채팅방 생성 공통 로직
 *  
 *  1) 1:1 채팅 : 첫 메세지가 전송되는 순간
 *  2) 그룹채팅 : 생성을 누르는 순간 (firstMessage, msgType 제외)
 *  
 *  받아오는 값 :: 방에 속한 멤버, 그룹채팅 여부, 이름, 첫 전송 메시지 정보
 *  return 값   :: roomId
 * 
 */


async function createRoom ({
	members,			// 방에 속한 멤버
	groupYn,			// 그룹채팅 여부
	groupName,			// 채팅방 이름
	firstMessage,		// 첫번째 전송 메시지
	msgType,			// 첫번째 전송 메시지의 타입
	csrfHeader,
	csrfToken
}) {

	  // 새 방: 방 생성 + 첫 메시지 저장
	  const bodyData = {
	    members: members,		
	    groupYn: groupYn,
	    groupName: groupName,		
	    firstMessage: firstMessage,			
	    msgType: msgType		
	  };
	
	  try {
		  
	    const res = await fetch(apiUrl(`messenger/chat`), {
			  method: 'POST',
			  headers: {
				  		'Content-Type': 'application/json',
				  		[csrfHeader]: csrfToken
			      	   },
			  body: JSON.stringify(bodyData)
			  });
	    
	    const data = await res.json();
	    return data;
	    
	  } 
	  
	  catch (error) {
	    console.error('방 생성 실패:', error);
	    return null;
	  }
  
}