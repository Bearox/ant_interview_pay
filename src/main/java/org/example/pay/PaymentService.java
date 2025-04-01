package org.example.pay;

import org.example.result.ConsultResult;

public interface PaymentService {
    ConsultResult isEnabled();
    PaymentTypeEnum getPaymentType();
}
