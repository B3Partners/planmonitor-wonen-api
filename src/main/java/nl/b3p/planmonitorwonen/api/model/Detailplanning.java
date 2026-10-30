/*
 * Copyright (C) 2024 Provincie Zeeland
 *
 * SPDX-License-Identifier: MIT
 */

package nl.b3p.planmonitorwonen.api.model;

public record Detailplanning(String id, String plancategorieId, Integer jaartal, Integer aantalGepland) {}
