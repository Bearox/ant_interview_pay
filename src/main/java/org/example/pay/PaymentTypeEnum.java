package org.example.pay;

import lombok.AllArgsConstructor;
import lombok.Getter;

import java.util.HashMap;
import java.util.Map;

@AllArgsConstructor
@Getter
public enum PaymentTypeEnum {
    NotDefine("NotDefine", "未定义"),
    RedPocket("RedPocket", "红包"),
    Balance("Balance", "余额");
    private final String key;
    private final String name;

    private static final Map<String, PaymentTypeEnum> enumMap = new HashMap<>();
    static {
        for (PaymentTypeEnum paymentTypeEnum : PaymentTypeEnum.values()) {
            enumMap.put(paymentTypeEnum.getKey(), paymentTypeEnum);
        }
    }

    public static PaymentTypeEnum fromKey(String key) {
        return enumMap.getOrDefault(key, NotDefine);
    }
}
