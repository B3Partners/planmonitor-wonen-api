/*
 * Copyright (C) 2026 Provincie Zeeland
 *
 * SPDX-License-Identifier: MIT
 */

package nl.b3p.planmonitorwonen.api.validation;

/**
 * A validation error for a field of the request body.
 *
 * @param field the field, for example "plantype" or "aantallen.woningType.Eengezins.gepland"
 * @param message a message for the user, in Dutch
 */
public record ValidationError(String field, String message) {}
