package com.fourthread.ozang.domain.dm.entity;

public enum DirectMessageURI {
  SEND("/sub/direct-messages_");

  DirectMessageURI(String uri) {
    this.uri = uri;
  }
  public String getUri() {
    return uri;
  }

  final String uri;
}
