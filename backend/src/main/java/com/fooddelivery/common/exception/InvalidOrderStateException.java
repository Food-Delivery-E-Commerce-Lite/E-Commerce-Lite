package com.fooddelivery.common.exception;

import org.springframework.http.HttpStatus;

public class InvalidOrderStateException extends BusinessException {
    public InvalidOrderStateException(Object from, Object to) {
        super("INVALID_ORDER_STATE",
              "Không thể chuyển từ " + from + " sang " + to, HttpStatus.CONFLICT);
    }
}
