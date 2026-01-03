package tech.kayys.silat.sdk.client;

import java.time.Duration;
import java.util.Map;

public record SilatClientConfig(String endpoint,String tenantId,String apiKey,TransportType transport,Duration timeout,Map<String,String>headers){}
