package com.fooddelivery.user;

import com.fooddelivery.auth.AuthUser;
import com.fooddelivery.common.ApiResponse;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;
import java.util.List;
import org.springframework.http.HttpStatus;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.ResponseStatus;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.transaction.annotation.Transactional;
import jakarta.validation.Valid;

@RestController
@RequestMapping("/api/v1/addresses")
public class AddressController {
    private final AddressRepository addresses;

    public AddressController(AddressRepository addresses) {
        this.addresses = addresses;
    }

    public record AddressRequest(
        @Size(max = 50) String label,
        @NotBlank @Size(max = 200) String street,
        @Size(max = 100) String district,
        @Size(max = 100) String city,
        Boolean isDefault) {}

    public record AddressResponse(Long id, String label, String street, String district, String city, boolean isDefault) {
        static AddressResponse from(Address a) {
            return new AddressResponse(a.getId(), a.getLabel(), a.getStreet(), a.getDistrict(), a.getCity(), a.isDefault());
        }
    }

    @GetMapping
    public ApiResponse<List<AddressResponse>> list(@AuthenticationPrincipal AuthUser user) {
        return ApiResponse.ok(addresses.findByUserId(user.id()).stream().map(AddressResponse::from).toList());
    }

    @PostMapping
    @ResponseStatus(HttpStatus.CREATED)
    @Transactional
    public ApiResponse<AddressResponse> create(@AuthenticationPrincipal AuthUser user,
                                               @Valid @RequestBody AddressRequest req) {
        boolean def = req.isDefault() != null && req.isDefault();
        if (def) {
            // Chỉ 1 địa chỉ mặc định: hạ các địa chỉ cũ xuống
            addresses.findByUserId(user.id()).forEach(a -> a.setDefault(false));
        }
        Address a = new Address(user.id(), req.label(), req.street().trim(), req.district(), req.city(), def);
        return ApiResponse.ok(AddressResponse.from(addresses.save(a)));
    }

    @DeleteMapping("/{id}")
    @ResponseStatus(HttpStatus.NO_CONTENT)
    public void delete(@AuthenticationPrincipal AuthUser user, @PathVariable Long id) {
        Address a = addresses.findByIdAndUserId(id, user.id())
            .orElseThrow(() -> new com.fooddelivery.common.exception.ResourceNotFoundException("Address", id));
        addresses.delete(a);
    }
}
