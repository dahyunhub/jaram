package com.jaram.common.exception;

/**
 * 비식별화 복원 실패(원문에 [[CHILD_n]] 센티넬 잔존 등). NFR-1 가드.
 */
public class DeidentificationException extends BusinessException {

    public DeidentificationException() {
        super(ErrorCode.DEIDENTIFICATION_RESTORE_FAILED);
    }

    public DeidentificationException(String message) {
        super(ErrorCode.DEIDENTIFICATION_RESTORE_FAILED, message);
    }
}
