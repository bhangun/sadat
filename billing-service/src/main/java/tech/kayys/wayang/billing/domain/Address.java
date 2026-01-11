package tech.kayys.wayang.billing.domain;

import jakarta.persistence.Embeddable;

/**
 * Address embeddable
 */
@Embeddable
public class Address {
    public String line1;
    public String line2;
    public String city;
    public String state;
    public String postalCode;
    public String country;
}

