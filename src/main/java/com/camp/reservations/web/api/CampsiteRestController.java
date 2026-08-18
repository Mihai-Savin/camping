package com.camp.reservations.web.api;

import com.camp.reservations.domain.Campsite;
import com.camp.reservations.dto.CampsiteRequest;
import com.camp.reservations.dto.CampsiteResponse;
import com.camp.reservations.security.OwnerPrincipal;
import com.camp.reservations.service.CampsiteService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/api/campsites")
@RequiredArgsConstructor
public class CampsiteRestController {

    private final CampsiteService campsiteService;

    @GetMapping
    public List<CampsiteResponse> list(@RequestParam(defaultValue = "false") boolean includeInactive) {
        List<Campsite> campsites = includeInactive ? campsiteService.findAll() : campsiteService.findAllActive();
        return campsites.stream().map(CampsiteResponse::from).toList();
    }

    @GetMapping("/{id}")
    public CampsiteResponse get(@PathVariable Long id) {
        return CampsiteResponse.from(campsiteService.findById(id));
    }

    @PostMapping
    @ResponseStatus(HttpStatus.CREATED)
    public CampsiteResponse create(@Valid @RequestBody CampsiteRequest request,
                                    @AuthenticationPrincipal OwnerPrincipal principal) {
        return CampsiteResponse.from(campsiteService.create(request, principal.getOwner()));
    }

    @PutMapping("/{id}")
    public CampsiteResponse update(@PathVariable Long id, @Valid @RequestBody CampsiteRequest request,
                                    @AuthenticationPrincipal OwnerPrincipal principal) {
        return CampsiteResponse.from(campsiteService.update(id, request, principal.getOwner()));
    }

    @DeleteMapping("/{id}")
    @ResponseStatus(HttpStatus.NO_CONTENT)
    public void delete(@PathVariable Long id, @AuthenticationPrincipal OwnerPrincipal principal) {
        campsiteService.delete(id, principal.getOwner());
    }
}
