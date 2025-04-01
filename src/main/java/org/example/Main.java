package org.example;

import com.google.gson.Gson;
import org.example.circuit_breaker.MockCircuitBreak;
import org.example.metric.MockMetric;
import org.example.pay.PaymentTypeEnum;
import org.example.rate_limit.MockRateLimiter;

import java.util.List;

//TIP 要<b>运行</b>代码，请按 <shortcut actionId="Run"/> 或
// 点击装订区域中的 <icon src="AllIcons.Actions.Execute"/> 图标。
public class Main {
    public static void main(String[] args) {
        PaymentRemoteService service = new PaymentRemoteService(new MockCircuitBreak(), new MockMetric(), new MockRateLimiter());
//        ConsultResult enabled = service.isEnabled(PaymentTypeEnum.Balance.getKey());
//        System.out.println(enabled.ToString());

        List<PaymentTypeEnum> validPaymentList = service.getValidPaymentList(new PaymentQueryOpt());
        Gson gson = new Gson();
        String json = gson.toJson(validPaymentList);
        System.out.println(json);

        PaymentRemoteService.close();
    }
}