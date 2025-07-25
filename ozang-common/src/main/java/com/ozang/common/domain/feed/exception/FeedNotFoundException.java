package com.ozang.common.domain.feed.exception;

import com.ozang.common.exception.ErrorDetails;
import com.ozang.common.exception.GlobalException;

public class FeedNotFoundException extends GlobalException {

  public FeedNotFoundException(String exceptionName, String message, ErrorDetails details) {
    super(exceptionName, message, details);
  }
}
