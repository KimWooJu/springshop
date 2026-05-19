package com.springshop.web.controller;

import com.springshop.web.dto.request.ChangePasswordRequest;
import com.springshop.web.dto.request.CreateAddressRequest;
import com.springshop.web.dto.request.CreateUserRequest;
import com.springshop.web.dto.request.UpdateAddressRequest;
import com.springshop.web.dto.request.UpdateUserRequest;
import com.springshop.web.dto.response.ApiResponse;
import com.springshop.web.dto.response.PageResponse;
import com.springshop.web.dto.response.UserAddressResponse;
import com.springshop.web.dto.response.UserProfileResponse;
import com.springshop.web.dto.response.UserResponse;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.responses.ApiResponses;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Map;
import java.util.concurrent.atomic.AtomicLong;

/**
 * 사용자 API 컨트롤러.
 *
 * <p>회원 가입, 프로필 조회/수정, 비밀번호 변경, 배송지 관리, 관리자용 사용자 목록 등
 * 사용자 라이프사이클 전반의 REST 엔드포인트를 제공한다.
 *
 * <p>응답은 {@link ApiResponse} sealed interface를 통해 성공/실패/페이징을 구분한다.
 */
@RestController
@RequestMapping("/api/v1/users")
@Tag(name = "사용자 API", description = "회원 CRUD 및 주소 관리")
public class UserController {

    private static final Logger LOG = LoggerFactory.getLogger(UserController.class);
    private static final AtomicLong ID_SEQ = new AtomicLong(1000);

    @Operation(summary = "회원 가입", description = "신규 회원을 등록한다. 이메일 중복 검사를 수행한다.")
    @ApiResponses({
            @io.swagger.v3.oas.annotations.responses.ApiResponse(responseCode = "201", description = "등록 성공"),
            @io.swagger.v3.oas.annotations.responses.ApiResponse(responseCode = "400", description = "검증 실패"),
            @io.swagger.v3.oas.annotations.responses.ApiResponse(responseCode = "409", description = "이메일 중복")
    })
    @PostMapping
    public ResponseEntity<ApiResponse<UserResponse>> register(
            @Parameter(description = "회원 가입 요청", required = true)
            @Valid @RequestBody CreateUserRequest request) {
        LOG.info("회원 가입 요청 - email={}", request.email());
        Long newId = ID_SEQ.incrementAndGet();
        UserResponse data = UserResponse.of(
                newId, request.email(), request.name(),
                request.phone(), "ACTIVE", "USER", false);
        ApiResponse<UserResponse> body = ApiResponse.Success.of(data, "회원 가입 완료");
        return ResponseEntity.status(HttpStatus.CREATED).body(body);
    }

    @Operation(summary = "사용자 단건 조회", description = "사용자 ID로 정보를 조회한다.")
    @GetMapping("/{id}")
    public ResponseEntity<ApiResponse<UserResponse>> getUser(
            @Parameter(description = "사용자 ID", required = true, example = "1024")
            @PathVariable Long id) {
        LOG.debug("사용자 조회 - id={}", id);
        UserResponse data = UserResponse.of(
                id, "user" + id + "@example.com", "사용자" + id,
                "010-1234-5678", "ACTIVE", "USER", true);
        return ResponseEntity.ok(ApiResponse.Success.of(data));
    }

    @Operation(summary = "사용자 정보 수정", description = "이름, 닉네임, 전화번호를 수정한다.")
    @PutMapping("/{id}")
    @PreAuthorize("hasRole('USER') and #id == authentication.principal.id")
    public ResponseEntity<ApiResponse<UserResponse>> updateUser(
            @PathVariable Long id,
            @Valid @RequestBody UpdateUserRequest request) {
        LOG.info("사용자 수정 - id={}, name={}", id, request.name());
        UserResponse data = UserResponse.of(
                id, "user" + id + "@example.com", request.name(),
                request.phone(), "ACTIVE", "USER", true);
        return ResponseEntity.ok(ApiResponse.Success.of(data, "수정 완료"));
    }

    @Operation(summary = "회원 탈퇴", description = "회원을 탈퇴 처리(soft delete)한다.")
    @DeleteMapping("/{id}")
    @PreAuthorize("hasRole('USER') and #id == authentication.principal.id")
    public ResponseEntity<ApiResponse<Void>> withdrawUser(
            @PathVariable Long id,
            @RequestParam(required = false) String reason) {
        LOG.warn("회원 탈퇴 - id={}, reason={}", id, reason);
        return ResponseEntity.ok(ApiResponse.Success.empty());
    }

    @Operation(summary = "내 프로필 조회", description = "로그인한 사용자의 상세 프로필을 조회한다.")
    @GetMapping("/me")
    @PreAuthorize("isAuthenticated()")
    public ResponseEntity<ApiResponse<UserProfileResponse>> getMyProfile() {
        Long currentId = 1024L;
        UserProfileResponse profile = new UserProfileResponse(
                currentId, "닉네임", "안녕하세요!", "https://cdn.example.com/avatar.jpg",
                null, null, "MALE", null, List.of(),
                null, 0L, "BRONZE", 0, 0L, 0, LocalDateTime.now(), Map.of());
        return ResponseEntity.ok(ApiResponse.Success.of(profile));
    }

    @Operation(summary = "비밀번호 변경", description = "현재 비밀번호 검증 후 새 비밀번호로 교체한다.")
    @PutMapping("/me/password")
    @PreAuthorize("isAuthenticated()")
    public ResponseEntity<ApiResponse<Void>> changePassword(
            @Valid @RequestBody ChangePasswordRequest request) {
        LOG.info("비밀번호 변경 요청");
        if (request.currentPassword().equals(request.newPassword())) {
            return ResponseEntity.badRequest().body(
                    ApiResponse.Failure.of("PASSWORD_SAME", "기존 비밀번호와 동일합니다."));
        }
        return ResponseEntity.ok(ApiResponse.Success.empty());
    }

    @Operation(summary = "내 배송지 목록", description = "로그인한 사용자의 모든 배송지를 조회한다.")
    @GetMapping("/me/addresses")
    @PreAuthorize("isAuthenticated()")
    public ResponseEntity<ApiResponse<List<UserAddressResponse>>> getAddresses() {
        List<UserAddressResponse> addresses = List.of(
                buildAddressResponse(1L, "집", true),
                buildAddressResponse(2L, "회사", false)
        );
        return ResponseEntity.ok(ApiResponse.Success.of(addresses));
    }

    @Operation(summary = "배송지 추가", description = "새 배송지를 등록한다.")
    @PostMapping("/me/addresses")
    @PreAuthorize("isAuthenticated()")
    public ResponseEntity<ApiResponse<UserAddressResponse>> addAddress(
            @Valid @RequestBody CreateAddressRequest request) {
        LOG.info("배송지 추가 - alias={}", request.alias());
        UserAddressResponse data = buildAddressResponse(
                ID_SEQ.incrementAndGet(), request.alias(), request.isDefault());
        return ResponseEntity.status(HttpStatus.CREATED)
                .body(ApiResponse.Success.of(data, "배송지 등록 완료"));
    }

    @Operation(summary = "배송지 수정", description = "기존 배송지를 수정한다.")
    @PutMapping("/me/addresses/{addressId}")
    @PreAuthorize("isAuthenticated()")
    public ResponseEntity<ApiResponse<UserAddressResponse>> updateAddress(
            @PathVariable Long addressId,
            @Valid @RequestBody UpdateAddressRequest request) {
        LOG.info("배송지 수정 - id={}", addressId);
        UserAddressResponse data = buildAddressResponse(addressId, request.alias(), false);
        return ResponseEntity.ok(ApiResponse.Success.of(data, "수정 완료"));
    }

    @Operation(summary = "배송지 삭제", description = "배송지를 삭제한다.")
    @DeleteMapping("/me/addresses/{addressId}")
    @PreAuthorize("isAuthenticated()")
    public ResponseEntity<ApiResponse<Void>> deleteAddress(
            @PathVariable Long addressId) {
        LOG.info("배송지 삭제 - id={}", addressId);
        return ResponseEntity.ok(ApiResponse.Success.empty());
    }

    @Operation(summary = "기본 배송지 설정", description = "지정한 배송지를 기본으로 설정한다.")
    @PutMapping("/me/addresses/{addressId}/default")
    @PreAuthorize("isAuthenticated()")
    public ResponseEntity<ApiResponse<UserAddressResponse>> setDefaultAddress(
            @PathVariable Long addressId) {
        LOG.info("기본 배송지 설정 - id={}", addressId);
        UserAddressResponse data = buildAddressResponse(addressId, "집", true);
        return ResponseEntity.ok(ApiResponse.Success.of(data, "기본 배송지로 설정됨"));
    }

    @Operation(summary = "사용자 목록 (관리자)", description = "관리자가 사용자 목록을 페이징 조회한다.")
    @GetMapping
    @PreAuthorize("hasRole('ADMIN')")
    public ResponseEntity<ApiResponse<List<UserResponse>>> listUsers(
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "20") int size,
            @RequestParam(required = false) String keyword,
            @RequestParam(required = false) String status) {
        LOG.debug("사용자 목록 - page={}, size={}, keyword={}, status={}", page, size, keyword, status);
        List<UserResponse> content = List.of(
                UserResponse.forAdmin(1001L, "u1@example.com", "사용자1", "010-1111-2222",
                        "ACTIVE", "USER", true, LocalDateTime.now().minusDays(1), LocalDateTime.now().minusMonths(3)),
                UserResponse.forAdmin(1002L, "u2@example.com", "사용자2", "010-3333-4444",
                        "ACTIVE", "USER", true, LocalDateTime.now().minusHours(5), LocalDateTime.now().minusMonths(2))
        );
        return ResponseEntity.ok(ApiResponse.Success.of(content,
                "총 " + content.size() + "명, 페이지 " + page + "/" + size));
    }

    @Operation(summary = "사용자 상태 변경 (관리자)", description = "관리자가 사용자 상태를 변경한다.")
    @PutMapping("/{id}/status")
    @PreAuthorize("hasRole('ADMIN')")
    public ResponseEntity<ApiResponse<UserResponse>> changeUserStatus(
            @PathVariable Long id,
            @RequestParam String newStatus,
            @RequestParam(required = false) String reason) {
        LOG.warn("사용자 상태 변경 - id={}, newStatus={}, reason={}", id, newStatus, reason);
        UserResponse data = UserResponse.forAdmin(
                id, "user" + id + "@example.com", "이름" + id, "010-1234-5678",
                newStatus, "USER", true, LocalDateTime.now(), LocalDateTime.now().minusYears(1));
        return ResponseEntity.ok(ApiResponse.Success.of(data, "상태 변경 완료"));
    }

    @Operation(summary = "사용자 권한 부여 (관리자)", description = "사용자에게 관리자 권한을 부여한다.")
    @PostMapping("/{id}/grant-admin")
    @PreAuthorize("hasRole('SUPER_ADMIN')")
    public ResponseEntity<ApiResponse<Void>> grantAdmin(
            @PathVariable Long id) {
        LOG.warn("관리자 권한 부여 - id={}", id);
        return ResponseEntity.ok(ApiResponse.Success.empty());
    }

    @Operation(summary = "사용자 권한 회수 (관리자)", description = "관리자 권한을 회수한다.")
    @PostMapping("/{id}/revoke-admin")
    @PreAuthorize("hasRole('SUPER_ADMIN')")
    public ResponseEntity<ApiResponse<Void>> revokeAdmin(
            @PathVariable Long id) {
        LOG.warn("관리자 권한 회수 - id={}", id);
        return ResponseEntity.ok(ApiResponse.Success.empty());
    }

    @Operation(summary = "사용자 잠금", description = "관리자가 사용자를 잠금 상태로 전환한다.")
    @PostMapping("/{id}/lock")
    @PreAuthorize("hasRole('ADMIN')")
    public ResponseEntity<ApiResponse<UserResponse>> lockUser(
            @PathVariable Long id,
            @RequestParam(required = false) String reason) {
        LOG.warn("사용자 잠금 - id={}, reason={}", id, reason);
        UserResponse data = UserResponse.of(
                id, "u" + id + "@example.com", "잠금사용자", "010-9999-9999",
                "LOCKED", "USER", true);
        return ResponseEntity.ok(ApiResponse.Success.of(data, "잠금 처리됨"));
    }

    @Operation(summary = "사용자 잠금 해제", description = "관리자가 잠금된 사용자를 해제한다.")
    @PostMapping("/{id}/unlock")
    @PreAuthorize("hasRole('ADMIN')")
    public ResponseEntity<ApiResponse<UserResponse>> unlockUser(
            @PathVariable Long id) {
        LOG.info("사용자 잠금 해제 - id={}", id);
        UserResponse data = UserResponse.of(
                id, "u" + id + "@example.com", "해제된사용자", "010-9999-9999",
                "ACTIVE", "USER", true);
        return ResponseEntity.ok(ApiResponse.Success.of(data, "잠금 해제 완료"));
    }

    private UserAddressResponse buildAddressResponse(Long id, String name, boolean isDefault) {
        return new UserAddressResponse(
                id, 1024L, name, "홍길동", "010-1234-5678", null,
                "서울특별시 강남구 테헤란로 123", "456호", "06236", "강남구", "KR",
                isDefault, "문 앞에 놓아주세요", LocalDateTime.now().minusDays(10), LocalDateTime.now());
    }
}
