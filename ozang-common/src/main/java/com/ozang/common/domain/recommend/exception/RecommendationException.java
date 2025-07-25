package com.ozang.common.domain.recommend.exception;

import com.ozang.common.exception.ErrorDetails;
import com.ozang.common.exception.GlobalException;

public class RecommendationException extends GlobalException  {

  public RecommendationException(String exceptionName, String message,
      ErrorDetails details) {
    super(exceptionName, message, details);
  }
}
