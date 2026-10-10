package com.bms.backend.common;

/** The JSON the API sends when something goes wrong: {"message": "..."} */
public record ApiError(String message) {
}
