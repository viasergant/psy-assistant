package com.psyassistant.careplans.rest;

import com.psyassistant.careplans.service.CarePlanService;
import java.util.List;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

/**
 * Lightweight config endpoint exposing care plan configuration to authenticated clients.
 */
@RestController
@RequestMapping("/api/v1/config")
public class CarePlanConfigController {

    private final CarePlanService carePlanService;

    public CarePlanConfigController(final CarePlanService carePlanService) {
        this.carePlanService = carePlanService;
    }

    /**
     * Returns the list of configured intervention types.
     *
     * <p>GET /api/v1/config/care-plan-intervention-types
     *
     * @return list of intervention type codes
     */
    @GetMapping("/care-plan-intervention-types")
    @PreAuthorize("isAuthenticated()")
    public ResponseEntity<List<String>> getInterventionTypes() {
        return ResponseEntity.ok(carePlanService.getInterventionTypes());
    }
}
