package com.faisal.controller;

import com.faisal.dto.request.CreateUserRequest;
import com.faisal.dto.request.PageRequest;
import com.faisal.dto.response.ApiResponse;
import com.faisal.dto.response.UserResponse;
import com.faisal.service.UserService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.security.SecurityRequirement;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Sort;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/users")
@RequiredArgsConstructor
@Tag(name = "Users", description = "Users management endpoints")
public class UserController {

    private final UserService userService;

    @PostMapping
    @ResponseStatus(HttpStatus.CREATED)
    @PreAuthorize("hasRole('ADMIN')")
    @SecurityRequirement(name = "bearerAuth")
    @Operation(
            summary = "Create user",
            description = "Creates a new user account. ADMIN only (as currently configured).",
            operationId = "createUser"
    )
    public ResponseEntity<ApiResponse<UserResponse>> createUser(
            @Parameter(description = "User creation payload (name, email, role).")
            @Valid @RequestBody CreateUserRequest request
    ) {
        UserResponse response = userService.createUser(request);
        return ResponseEntity.ok(new ApiResponse<>(true, response));
    }

    @GetMapping("/{id}")
    @ResponseStatus(HttpStatus.OK)
    @PreAuthorize("hasRole('ADMIN')")
    @SecurityRequirement(name = "bearerAuth")
    @Operation(
            summary = "Get user by id",
            description = "Returns user details by id. ADMIN only.",
            operationId = "getUserById"
    )
    public ResponseEntity<ApiResponse<UserResponse>> getUser(
            @Parameter(description = "User id.", example = "1")
            @PathVariable Long id
    ) {
        UserResponse response = userService.getUser(id);
        return ResponseEntity.ok(new ApiResponse<>(true, response));
    }


    @GetMapping
    @ResponseStatus(HttpStatus.OK)
    @PreAuthorize("hasRole('ADMIN')")
    @SecurityRequirement(name = "bearerAuth")
    @Operation(
            summary = "List users",
            description = "Returns a paginated list of users. ADMIN only.",
            operationId = "listUsers"
    )
    public ResponseEntity<ApiResponse<Page<UserResponse>>> listUsers(
            @RequestParam(defaultValue = "0", name = "page") int page,
            @RequestParam(defaultValue = "10", name = "size") int size,
            @RequestParam(defaultValue = "id", name = "sort_by") String sortBy,
            @RequestParam(defaultValue = "ASC", name = "direction") String direction
    ) {
        Pageable pageable = org.springframework.data.domain.PageRequest.of(
                page,
                size,
                Sort.by(Sort.Direction.fromString(direction.toUpperCase()), sortBy)
        );
        Page<UserResponse> response = userService.list(pageable);
        return ResponseEntity.ok(new ApiResponse<>(true, response));
    }

    @PutMapping("/{id}")
    @ResponseStatus(HttpStatus.CREATED)
    @PreAuthorize("hasRole('ADMIN')")
    @SecurityRequirement(name = "bearerAuth")
    @Operation(
            summary = "Update user",
            description = "Updates a user by id. ADMIN only.",
            operationId = "updateUser"
    )
    public ResponseEntity<ApiResponse<UserResponse>> updateUser(
            @Parameter(description = "User id to update.", example = "1")
            @PathVariable Long id,

            @Parameter(description = "User update payload (name, email, role).")
            @RequestBody CreateUserRequest request
    ) {
        UserResponse response = userService.update(id, request);
        return ResponseEntity.ok(new ApiResponse<>(true, response));
    }

    @DeleteMapping("/{id}")
    @ResponseStatus(HttpStatus.OK)
    @PreAuthorize("hasRole('ADMIN')")
    @SecurityRequirement(name = "bearerAuth")
    @Operation(
            summary = "Delete user",
            description = "Deletes a user by id. ADMIN only.",
            operationId = "deleteUser"
    )
    public ResponseEntity<ApiResponse<String>> deleteUser(
            @Parameter(description = "User id to delete.", example = "1")
            @PathVariable Long id
    ) {
        userService.delete(id);
        return ResponseEntity.ok(new ApiResponse<>(true, "User deleted successfully"));
    }
}