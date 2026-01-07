package tech.kayys.wayang.billing.dto;

public 
record SsoAuthenticationResult(
    boolean success,
    String userId,
    String email,
    String sessionToken
) {}