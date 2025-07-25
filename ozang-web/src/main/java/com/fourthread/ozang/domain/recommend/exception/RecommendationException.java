package com.fourthread.ozang.domain.recommend.exception;

import com.fourthread.ozang.common.exception.ErrorDetails;
import com.fourthread.ozang.common.exception.GlobalException;

public class RecommendationException extends GlobalException  {

  public RecommendationException(String exceptionName, String message,
      ErrorDetails details) {
    super(exceptionName, message, details);
  }
}
