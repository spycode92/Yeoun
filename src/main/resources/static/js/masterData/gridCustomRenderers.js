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
	
	//bom 그룹 타입 유형
	'STD':	'정규/양산 BOM',
	'ALT':	'대체 BOM',
	'SMP':	'샘플/시험 BOM',
	'DEV':	'개발/연구 BOM',
	'RWRK':	'재작업 BOM',
	
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
        return CODE_MAP[englishValue] || englishValue; 
    }

    getElement() {
        return this.el;
    }

    // ⭐ 수정: props를 인자로 받아 editor 유무를 확인합니다.
    formatStatusBadge(status, hasEditor) {
        const map = {
            "Y": { text: "활성", cls: "status-badge bg-primary" },
            "N": { text: "비활성", cls: "status-badge bg-warning" },
        };
        const item = map[status] || { text: status, cls: "status-badge" };
        
        // editor 설정이 있을 때만 화살표 추가
        const arrow = hasEditor ? `<span style="font-size:8px;opacity:0.6;padding-left:3px;">▼</span>` : '';
        
        return `<span class="${item.cls}" style="padding: 4px 8px; border-radius: 4px; color: white;">${item.text}${arrow}</span> `;
    }
	
    render(props) {
        const value = props.value;
        const rowKey = props.rowKey;
        const columnInfo = props.columnInfo;
        
        // ⭐ 1. Editor 설정 여부 확인 (핵심!)
        const hasEditor = !!columnInfo.editor; 
        const isSelect = props.columnInfo.renderer?.options?.isSelect;

        const koreanText = StatusModifiedRenderer.getKoreanText(value);

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
        
        if (isSelect) {
            // ⭐ 2. isSelect 모드일 때도 editor가 있을 때만 화살표 렌더링
            const arrow = hasEditor ? `<span style="font-size:10px;opacity:0.6;">▼</span>` : '';
            contentHTML = `
            <div style="width:100%; height:100%; padding:0px 10px; box-sizing:border-box; display:flex; justify-content:space-between; align-items:center; background:transparent; cursor:pointer;">
                <span>${displayText}</span>
                ${arrow}
            </div>
            `;
        } else if (value === 'Y' || value === 'N') {
            // ⭐ 3. 배지 함수에 editor 여부 전달
            contentHTML = this.formatStatusBadge(value, hasEditor);
        } else {
            contentHTML = koreanText;
        }

        this.el.innerHTML = contentHTML;

        // 하이라이트 로직 (기존과 동일)
        const shouldHighlight = isUpdated || (isCreated && hasValue);
        const highlightClassName = 'modified-cell-highlight';

        if (shouldHighlight) {
            this.el.classList.add(highlightClassName);
        } else {
            this.el.classList.remove(highlightClassName);
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


