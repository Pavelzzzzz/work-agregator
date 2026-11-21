package com.vacancyscout.ingestion;

import org.springframework.web.reactive.function.client.ExchangeStrategies;

/** WebClient settings shared by the ingestion clients. */
final class IngestionWebClientSupport {

  // rabota.by serves ~740 KB of uncompressed HTML for a vacancy page; the
  // default 256 KB codec limit makes bodyToMono(String) fail before parsing.
  private static final int MAX_BODY_BYTES = 2 * 1024 * 1024;

  private IngestionWebClientSupport() {}

  static ExchangeStrategies exchangeStrategies() {
    return ExchangeStrategies.builder()
        .codecs(codecs -> codecs.defaultCodecs().maxInMemorySize(MAX_BODY_BYTES))
        .build();
  }
}
