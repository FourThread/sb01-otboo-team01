package com.fourthread.ozang.module.domain.feed.exception;

import com.fourthread.ozang.module.common.exception.ErrorDetails;
import com.fourthread.ozang.module.common.exception.GlobalException;

public class FeedNotFoundException extends GlobalException {

  public FeedNotFoundException(String exceptionName, String message,
      ErrorDetails details) {
    super(exceptionName, message, details);
  }
}
