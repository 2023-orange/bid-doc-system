package com.example.biddoc.common.util;

import com.example.biddoc.common.exception.BusinessException;
import com.example.biddoc.common.exception.ErrorCode;

public class AssertUtil {

    public static void notNull(Object obj, ErrorCode code) {
        if (obj == null) {
            throw new BusinessException(code);
        }
    }

    public static void isTrue(boolean condition, ErrorCode code) {
        if (!condition) {
            throw new BusinessException(code);
        }
    }
}
