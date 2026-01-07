package tech.kayys.wayang.billing.model;

import java.util.List;
import java.util.Map;


/**
 * Time series components
 */
public record TimeSeriesComponents(
    List<Double> trend,
    Map<Integer, Double> seasonal,
    List<Double> residuals,
    double rSquared
) {}
