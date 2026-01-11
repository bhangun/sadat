package tech.kayys.wayang.billing.model;

import java.math.BigDecimal;
import java.time.YearMonth;

public /**
 * Monthly revenue data point
 */
record MonthlyRevenue(YearMonth month, BigDecimal revenue) {
    public double value() {
        return revenue.doubleValue();
    }
}