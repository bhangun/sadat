package tech.kayys.wayang.schema.acp;

import java.util.Arrays;
import java.util.List;

import tech.kayys.wayang.schema.governance.SLA;
import tech.kayys.wayang.schema.governance.Trust;

public class CommercePolicy {
    private String pricingModel;
    private Double price;
    private String currency = "USD";
    private Integer quota;
    private String settlement;
    private SLA sla;
    private Trust trust;

    public String getPricingModel() {
        return pricingModel;
    }

    public void setPricingModel(String pricingModel) {
        List<String> validModels = Arrays.asList("per_call", "per_token", "subscription", "outcome_based");
        if (!validModels.contains(pricingModel)) {
            throw new IllegalArgumentException("Invalid pricing model: " + pricingModel);
        }
        this.pricingModel = pricingModel;
    }

    public Double getPrice() {
        return price;
    }

    public void setPrice(Double price) {
        if (price != null && price < 0) {
            throw new IllegalArgumentException("Price cannot be negative");
        }
        this.price = price;
    }

    public String getCurrency() {
        return currency;
    }

    public void setCurrency(String currency) {
        this.currency = currency;
    }

    public Integer getQuota() {
        return quota;
    }

    public void setQuota(Integer quota) {
        if (quota != null && quota < 0) {
            throw new IllegalArgumentException("Quota cannot be negative");
        }
        this.quota = quota;
    }

    public String getSettlement() {
        return settlement;
    }

    public void setSettlement(String settlement) {
        List<String> validSettlements = Arrays.asList("prepaid", "postpaid", "escrow");
        if (!validSettlements.contains(settlement)) {
            throw new IllegalArgumentException("Invalid settlement: " + settlement);
        }
        this.settlement = settlement;
    }

    public SLA getSla() {
        return sla;
    }

    public void setSla(SLA sla) {
        this.sla = sla;
    }

    public Trust getTrust() {
        return trust;
    }

    public void setTrust(Trust trust) {
        this.trust = trust;
    }
}
