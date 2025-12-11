# 도장 이미지 통합 가이드

## 구현 완료 사항

### 1. 백엔드 (✅ 완료)

#### FileAttachRepository.java
```java
// 참조테이블, 참조테이블id, 카테고리로 파일조회 (도장 이미지 조회용)
List<FileAttach> findByRefTableAndRefIdAndCategory(String refTable, Long refId, String category);
```

#### ApprovalDocService.java
```java
// 결재 문서의 도장 이미지 조회 (결재 순서별로 Map 반환)
public Map<String, String> getApprovalStampImages(Long approvalId) {
    Map<String, String> stampImages = new HashMap<>();
    
    // APPROVER_STAMP 테이블에서 해당 결재 문서의 모든 도장 이미지 조회
    List<FileAttach> stampFiles = fileAttachRepository.findByRefTableAndRefId("APPROVER_STAMP", approvalId);
    
    for (FileAttach file : stampFiles) {
        // category에 결재 순서가 "1_STAMP", "2_STAMP" 형식으로 저장되어 있음
        String category = file.getCategory();
        if (category != null && category.endsWith("_STAMP")) {
            String order = category.replace("_STAMP", ""); // "1", "2", "3" 추출
            stampImages.put(order, "/files/download/" + file.getFileId());
        }
    }
    
    return stampImages;
}
```

#### ApprovalRestController.java
```java
// 결재 문서의 도장 이미지 조회
@GetMapping("/stamps/{approvalId}")
public ResponseEntity<Map<String, String>> getApprovalStamps(@PathVariable("approvalId") Long approvalId) {
    Map<String, String> stampImages = approvalDocService.getApprovalStampImages(approvalId);
    return ResponseEntity.ok(stampImages);
}
```

### 2. 프론트엔드 (✅ 함수 추가 완료, 🔧 호출 부분 수동 추가 필요)

#### approvalDoc.js에 추가된 함수들:

```javascript
// f- 저장된 도장 이미지 불러오기 함수
async function loadApprovalStamps(approvalId) {
	try {
		const response = await fetch(`/api/approvals/stamps/${approvalId}`);
		if (!response.ok) {
			console.log('도장 이미지 조회 실패');
			return {};
		}
		const stampImages = await response.json();
		console.log('불러온 도장 이미지:', stampImages);
		return stampImages; // { "1": "/files/download/123", "2": "/files/download/124", ... }
	} catch (error) {
		console.error('도장 이미지 로드 오류:', error);
		return {};
	}
}

// f- 결재권자별 도장 이미지 표시 함수
async function displayStampsForApprovers(approvalId) {
	const stampImages = await loadApprovalStamps(approvalId);
	
	// 결재권자 div들을 순회하며 도장 이미지 표시
	const approverDivs = document.querySelectorAll('#approver > div');
	approverDivs.forEach((div, index) => {
		const order = (index + 1).toString(); // 1, 2, 3
		const stampUrl = stampImages[order];
		
		if (stampUrl) {
			// p 태그 찾기
			const pTag = div.querySelector('p');
			if (pTag) {
				updateStampPreview(stampUrl, pTag);
			}
		}
	});
}

// f- 도장 미리보기 업데이트 함수 (수정됨)
function updateStampPreview(imageUrl, targetPTag = null) {
	// targetPTag가 없으면 기본 선택자 사용
	const approverPTag = targetPTag || document.querySelector('#approver > div p');
	
	// 결재란의 p 태그를 찾지 못하면 종료
	if (!approverPTag) return;

	// 기존의 도장 스탬프 컨테이너(.approver-stamp)를 찾아 제거 (재할당을 위해)
	const existingStamp = approverPTag.querySelector('.approver-stamp');
	if (existingStamp) {
		existingStamp.remove();
	}

	// 새 도장 컨테이너 생성
	const stampDiv = document.createElement('div');
	stampDiv.className = 'approver-stamp';
	stampDiv.style.cssText = 'width: 60px; height: 60px; display: flex; align-items: center; justify-content: center; margin: 10px auto;';

	if (imageUrl) {
		const stampImg = document.createElement('img');
		stampImg.src = imageUrl;
		stampImg.style.cssText = 'max-width: 100%; max-height: 100%; object-fit: contain;';
		stampDiv.appendChild(stampImg);
	} else {
		// 이미지 없는 경우 (인) 텍스트 표시
		stampDiv.textContent = '(인)';
		stampDiv.style.cssText += 'border: 1px dotted black; border-radius: 50%;';
	}

	// p 태그의 가장 아래쪽에 삽입
	approverPTag.appendChild(stampDiv);
}
```

## 🔧 수동으로 추가해야 할 부분

`approvalDoc.js` 파일에서 **grid1, grid2, grid3, grid4, grid5의 클릭 이벤트 핸들러**에 다음 코드를 추가해주세요:

### 추가 위치
결재권자 목록을 표시한 후 (print 함수 호출 후), 다음 코드를 추가:

```javascript
// 도장 이미지 불러오기 및 표시
await displayStampsForApprovers(approvalId);
```

### 예시 (grid1):
```javascript
grid1.on("click", async (ev) => {
    const target = ev.nativeEvent.target;
    if (ev.targetType === 'cell' && target.tagName === 'BUTTON') {
        const rowData = grid1.getRow(ev.rowKey);
        $('#approval-modal').modal('show');
        
        // ... 기존 코드 ...
        
        approvalId = rowData.approval_id;
        getApprovalDocFileData(approvalId);
        currentApprover = rowData.approver;
        
        // ... 기존 코드 ...
        
        const approverList = await getApproverList(approvalId);
        
        if (approverList.length > 0) {
            sortedList = approverList.sort((a, b) => {
                return Number(a.orderApprovers) - Number(b.orderApprovers);
            });
            
            window.count = 0;
            approverDiv.innerHTML = "";
            
            for (const approver of sortedList) {
                selectBox.select(approver.empId);
                print("default", selectBox.getSelectedItem()?.label);
            }
            
            // ✅ 여기에 추가!
            await displayStampsForApprovers(approvalId);
        }
        
        // ... 나머지 코드 ...
    }
});
```

### 추가해야 할 위치 (총 5곳)
1. **grid1.on("click", ...)** - 약 346번째 줄 근처
2. **grid2.on("click", ...)** - 약 407번째 줄 근처  
3. **grid3.on("click", ...)** - 약 472번째 줄 근처
4. **grid4.on("click", ...)** - 약 536번째 줄 근처
5. **grid5.on("click", ...)** - 약 598번째 줄 근처

각 그리드의 클릭 이벤트에서 결재권자 목록을 표시하는 `for` 루프 바로 다음에 추가하면 됩니다.

## 작동 방식

1. 사용자가 결재 문서 상세보기 버튼 클릭
2. 결재권자 목록 조회 및 표시
3. **`displayStampsForApprovers(approvalId)` 호출**
4. 서버에서 저장된 도장 이미지 조회 (`/api/approvals/stamps/{approvalId}`)
5. 각 결재권자별로 도장 이미지 표시
6. 도장이 있으면 이미지 표시, 없으면 "(인)" 텍스트 표시

## 테스트 방법

1. 결재 문서를 승인할 때 도장 이미지 업로드
2. 문서 상세보기를 열었을 때 해당 결재권자의 도장이 표시되는지 확인
3. 콘솔에서 `불러온 도장 이미지:` 로그 확인

## 주의사항

- 도장 이미지는 `APPROVER_STAMP` 테이블에 저장됩니다
- 카테고리는 `{결재순서}_STAMP` 형식입니다 (예: "1_STAMP", "2_STAMP")
- 이미지 URL은 `/files/download/{fileId}` 형식으로 반환됩니다
