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

/**
 * Results in a 409 response when another planregistratie already has the same plan name. Plan names are unique for all
 * gemeentes. The errors property has the same format as for {@link PlanregistratieValidationException}.
 */
public class PlanNaamInUseException extends ErrorResponseException {
  public PlanNaamInUseException(String planNaam) {
    super(HttpStatus.CONFLICT, createProblemDetail(planNaam), null);
  }

  private static ProblemDetail createProblemDetail(String planNaam) {
    String message = "Er bestaat al een andere planregistratie met de naam \"%s\"".formatted(planNaam);
    ProblemDetail problemDetail = ProblemDetail.forStatusAndDetail(HttpStatus.CONFLICT, message);
    problemDetail.setTitle("Plannaam is al in gebruik");
    problemDetail.setProperty("errors", List.of(new ValidationError("planNaam", message)));
    return problemDetail;
  }
}
