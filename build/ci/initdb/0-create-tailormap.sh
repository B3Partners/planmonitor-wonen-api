#!/usr/bin/env bash
# Copyright (C) 2024 Provincie Zeeland
#
# SPDX-License-Identifier: MIT
set -e

psql -U "$POSTGRES_USER" -d "$POSTGRES_DB" -c 'CREATE EXTENSION postgis;'
psql -U "$POSTGRES_USER" -a -c "CREATE ROLE tailormap LOGIN PASSWORD 'tailormap' SUPERUSER CREATEDB;"
psql -U "$POSTGRES_USER" -c 'CREATE DATABASE tailormap OWNER tailormap;'



