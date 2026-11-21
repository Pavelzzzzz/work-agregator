package com.vacancyscout.config;

import org.springframework.web.reactive.function.client.ExchangeStrategies;

/** Shared WebClient settings for outbound HTTP calls. */
public final class WebClientSupport {

  // Some vacancy pages serve ~740 KB of uncompressed HTML; the default 256 KB
  // codec limit makes bodyToMono(String) fail before the body can be parsed.
  private static final int MAX_BODY_BYTES = 2 * 1024 * 1024;

  private WebClientSupport() {}

  public static ExchangeStrategies exchangeStrategies() {
    return ExchangeStrategies.builder()
        .codecs(codecs -> codecs.defaultCodecs().maxInMemorySize(MAX_BODY_BYTES))
        .build();
  }
}
