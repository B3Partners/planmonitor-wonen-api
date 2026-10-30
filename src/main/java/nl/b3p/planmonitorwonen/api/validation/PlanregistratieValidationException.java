/*
 * Copyright (C) 2026 Provincie Zeeland
 *
 * SPDX-License-Identifier: MIT
 */

package nl.b3p.planmonitorwonen.api.validation;

import java.util.List;
import org.springframework.http.HttpStatus;
import org.springframework.http.ProblemDetail;
import org.springframework.web.ErrorResponseException;

/** Results in a 400 response with a problem detail listing all validation errors in the "errors" property. */
public class PlanregistratieValidationException extends ErrorResponseException {
  private final transient List<ValidationError> errors;

  public PlanregistratieValidationException(List<ValidationError> errors) {
    super(HttpStatus.BAD_REQUEST, createProblemDetail(errors), null);
    this.errors = List.copyOf(errors);
  }

  private static ProblemDetail createProblemDetail(List<ValidationError> errors) {
    ProblemDetail problemDetail = ProblemDetail.forStatusAndDetail(
        HttpStatus.BAD_REQUEST,
        errors.size() == 1
            ? "De planregistratie bevat 1 fout"
            : "De planregistratie bevat %d fouten".formatted(errors.size()));
    problemDetail.setTitle("Ongeldige planregistratie");
    problemDetail.setProperty("errors", errors);
    return problemDetail;
  }

  public List<ValidationError> getErrors() {
    return errors;
  }
}
