package org.example.pay;

import org.example.result.ConstResult;
import org.example.result.ConsultResult;

/**
 * 余额
 */
public class PaymentBalanceImp implements PaymentService {

    @Override
    public ConsultResult isEnabled() {
        System.out.println("Balance isEnabled Run");
        return ConstResult.successResult;
    }


    @Override
    public PaymentTypeEnum getPaymentType() {
        return PaymentTypeEnum.Balance;
    }
}
