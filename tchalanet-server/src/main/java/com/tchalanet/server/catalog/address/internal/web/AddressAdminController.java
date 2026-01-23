package com.tchalanet.server.catalog.address.internal.web;

import com.tchalanet.server.catalog.address.api.AddressCatalog;
import com.tchalanet.server.catalog.address.api.AddressSearchCriteria;
import com.tchalanet.server.catalog.address.api.AddressView;
import com.tchalanet.server.catalog.address.internal.write.AddressAdminService;
import com.tchalanet.server.catalog.address.internal.web.model.CreateAddressRequest;
import com.tchalanet.server.catalog.address.internal.web.model.UpdateAddressRequest;
import com.tchalanet.server.common.error.ProblemRest;
import com.tchalanet.server.common.types.id.AddressId;
import com.tchalanet.server.common.web.api.ApiResponse;
import com.tchalanet.server.common.web.paging.TchPage;
import com.tchalanet.server.common.web.paging.TchPageRequest;
import com.tchalanet.server.common.web.paging.TchPaging;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import lombok.RequiredArgsConstructor;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/platform/addresses")
@RequiredArgsConstructor
@PreAuthorize("hasAuthority('ADMIN')")
@Tag(name = "Platform • Addresses")
public class AddressAdminController {

    private final AddressAdminService admin;
    private final AddressCatalog catalog;

    @Operation(summary = "List active addresses (platform)")
    @GetMapping("/active")
    public ApiResponse<List<AddressView>> listActive() {
        return ApiResponse.success(catalog.listActive());
    }

    @Operation(summary = "Search addresses (paginated)")
    @GetMapping("/search")
    public ApiResponse<TchPage<AddressView>> search(
        AddressSearchCriteria criteria, @TchPaging TchPageRequest pageReq) {
        return ApiResponse.success(catalog.search(criteria, pageReq));
    }

    @Operation(summary = "Get address by id (platform)")
    @GetMapping("/{id}")
    public ApiResponse<AddressView> getById(@PathVariable String id) {
        var addressId = AddressId.parse(id);
        var view =
            catalog
                .findById(addressId)
                .orElseThrow(() -> ProblemRest.notFound("address_not_found" + id));
        return ApiResponse.success(view);
    }

    @Operation(summary = "Create address (platform)")
    @PostMapping
    public ApiResponse<AddressView> create(@RequestBody CreateAddressRequest request) {
        var created = admin.create(request);
        return ApiResponse.created(created); // mapping should be internal-safe
    }

    @Operation(summary = "Update address (platform)")
    @PutMapping("/{id}")
    public ApiResponse<AddressView> update(
        @PathVariable String id, @RequestBody UpdateAddressRequest request) {
        var addressId = AddressId.parse(id);
        var updated = admin.update(addressId, request);
        return ApiResponse.success(updated);
    }

    @Operation(summary = "Soft-delete address (platform)")
    @DeleteMapping("/{id}")
    public void delete(@PathVariable String id) {
        var addressId = AddressId.parse(id);
        admin.softDelete(addressId);
    }

    @Operation(
        summary =
            "Deduplicate addresses by postalCode, line1, city, country (keeps one, soft-deletes duplicates)")
    @PostMapping("/dedup")
    public ApiResponse<List<AddressView>> dedupByFields(
        @RequestParam String postalCode,
        @RequestParam String line1,
        @RequestParam String city,
        @RequestParam String country) {

        var kept =
            admin.dedupByFields(postalCode, line1, city, country);
        return ApiResponse.success(kept);
    }
}
