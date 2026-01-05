package com.faisal.dto;

import com.faisal.enums.Role;

public record AuthUser (Long userId, Role role, String email) {
}