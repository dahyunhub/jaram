package com.ondo.common.exception;

/**
 * 외부 LLM 분석 관련 예외(연결 실패·타임아웃·출력 검증 실패 등). 메시지엔 메모 보존 고지가 포함된다(FR-4).
 */
public class AiAnalysisException extends BusinessException {

    public AiAnalysisException(ErrorCode errorCode) {
        super(errorCode);
    }

    public AiAnalysisException(ErrorCode errorCode, String message) {
        super(errorCode, message);
    }

    public AiAnalysisException(ErrorCode errorCode, Throwable cause) {
        super(errorCode, cause);
    }
}
