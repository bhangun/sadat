package tech.kayys.wayang.organization.domain;

import jakarta.persistence.Embeddable;

@Embeddable
public class Address {

    public String street;
    public String city;
    public String state;
    public String postalCode;
    public String country;

    public Address() {
    }

    public Address(String street, String city, String state, String postalCode, String country) {
        this.street = street;
        this.city = city;
        this.state = state;
        this.postalCode = postalCode;
        this.country = country;
    }
}