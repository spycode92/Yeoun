/**
 * tui ui 수정스타일변경과 영어표기를 따로 만든 클래스
 */
// 영어 한글 표기 CODE_MAP
const CODE_MAP = {
	//제품유형
    'FINISHED_GOODS': '완제품',
    'SEMI_FINISHED_GOODS': '반제품',
	
	//상태코드
    'Y': '활성',
    'N': '비활성',
    'PRD_ACTIVE': '활성',
    'PRD_INACTIVE': '비활성',
    'PRD_DISCONTINUED': '단종',
    'PRD_SEASONAL': '시즌상품',
    'PRD_OUT_OF_STOCK': '재고없음',
    'PRD_IN_STOCK': '재고있음',
	
	//원재료 유형
    'RAW': '원재료',
    'SUB': '부자재',
    'PKG': '포장재',
    'WIP': '공정중', // 또는 '재공품' (Work-in-Process)
    'FIN': '완제품', // 또는 '생산품' (Finished Goods)
    'BOX': '박스',
    'UNIT_BOX': '박스',
	
	//향수 유형
	'LIQUID': '고체향수', 
	'SOLID': '액체향수',
	
	//안전재고 - 정책방식
	'FIXED_QTY':'고정 계산방식',
	'DAYS_COVER': '일수기반',
	
	//품질항목기준 - 대상구분
	'FINISHED_QC':'완제품'
};

class StatusModifiedRenderer {
    constructor(props) {
        const el = document.createElement('div');
        el.className = 'tui-grid-cell-content-renderer'; 
        this.el = el;
        this.grid = props.grid; 
        
        this.render(props);
    }
	
	static getKoreanText(englishValue) {
        // 분리된 PRD_STATUS_MAP을 참조합니다.
        return CODE_MAP[englishValue] || englishValue; 
    }

    getElement() {
        return this.el;
    }

    formatStatusBadge(status) {
        const map = {
            "Y": { text: "활성", cls: "status-badge bg-primary" },
            "N": { text: "비활성", cls: "status-badge bg-warning" },
        };
        const item = map[status] || { text: status, cls: "status-badge" };
        
        // TOAST UI Grid의 html 유틸리티를 사용하여 안전하게 HTML 문자열 반환
        return `<span class="${item.cls}" style="padding: 4px 8px; border-radius: 4px; color: white; font-weight: bold;">${item.text}<span style="font-size:10px;opacity:0.6;pa">▼</span></span> `;
    }
	
    render(props) {
        const value = props.value;
        const rowKey = props.rowKey;
        const isSelect = props.columnInfo.renderer?.options?.isSelect;

        const koreanText = StatusModifiedRenderer.getKoreanText(value);

        // 신규 행 여부
        let isCreated = false;
        let isUpdated = false;

        if (this.grid) {
            const { createdRows, updatedRows } = this.grid.getModifiedRows();
            isCreated = createdRows.some(r => String(r.rowKey) === String(rowKey));
            isUpdated = updatedRows.some(r => String(r.rowKey) === String(rowKey));
        }

        const hasValue = value !== null && value !== undefined && value !== '';
        const displayText = (!hasValue && isCreated) ? '' : koreanText;

        let contentHTML = '';
        // UI
        if (isSelect) {
            contentHTML = `
            <div style="
                width:100%;
                height:100%;
                padding:0px 10px;
                box-sizing:border-box;
                display:flex;
                justify-content:space-between;
                align-items:center;
                background:transparent;
                cursor:pointer;
            ">
                <span>${displayText}</span>
                <span style="font-size:10px;opacity:0.6;">▼</span>
            </div>
            `;
        } else if (value === 'Y' || value === 'N') {
            // ⭐ 2. Y/N 값일 때: 배지 HTML 할당
            contentHTML = this.formatStatusBadge(value);
            
        } else {
            contentHTML = koreanText;
        }

        this.el.innerHTML = contentHTML;

        // 🎨 색상 조건 수정
        const shouldHighlight = isUpdated || (isCreated && hasValue);
        
        const highlightClassName = 'modified-cell-highlight';

        if (shouldHighlight) {
            // 하이라이트 클래스 추가
            this.el.classList.add(highlightClassName);
            
            // ⭐ 이전에 인라인으로 설정했던 색상 코드는 CSS 클래스 내부로 이동
            // this.el.style.backgroundColor = '#c3f2ffff'; 
            // this.el.style.color = '#007aff';
            
        } else {
            // 하이라이트 클래스 제거 (원래 스타일로 복원)
            this.el.classList.remove(highlightClassName);
            
            // this.el.style.backgroundColor = '';
            // this.el.style.color = '';
        }
        


        
 
    }

}

class NumberOnlyEditor {
  constructor(props) {
    const el = document.createElement('input');
    el.type = 'text'; // type을 text로 두어야 input 이벤트 제어가 용이합니다.
    this.el = el;

    // 초기 값 설정
    this.el.value = props.value;

    // 입력 이벤트를 감지하여 숫자만 남기는 함수 바인딩
    this.el.addEventListener('input', this.handleInput.bind(this));
  }

  getElement() {
    return this.el;
  }

  getValue() {
    // 최종적으로 저장될 때도 숫자가 아닌 문자는 제거하고 반환합니다.
    return this.el.value.replace(/[^0-9]/g, ''); 
  }
  
  // 입력 시마다 실행되어 숫자(0-9)가 아닌 문자를 제거합니다.
  handleInput() {
    this.el.value = this.el.value.replace(/[^0-9]/g, ''); 
  }
}


