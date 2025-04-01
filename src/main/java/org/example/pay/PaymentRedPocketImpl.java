package org.example.pay;

import org.example.result.ConstResult;
import org.example.result.ConsultResult;

public class PaymentRedPocketImpl implements PaymentService {
    @Override
    public ConsultResult isEnabled() {
        System.out.println("RedPocket isEnabled Run");
        try {
            Thread.sleep(2000);
        } catch (InterruptedException e) {
            System.out.println(e.getMessage());
        }
        return ConstResult.successResult;
    }


    @Override
    public PaymentTypeEnum getPaymentType() {
        return PaymentTypeEnum.RedPocket;
    }
}
