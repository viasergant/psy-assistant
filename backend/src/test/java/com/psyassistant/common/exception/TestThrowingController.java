package com.psyassistant.common.exception;

import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

/**
 * Minimal controller used only in tests to simulate an unhandled exception.
 */
@RestController
@RequestMapping("/test")
class TestThrowingController {

    /**
     * Always throws a {@link RuntimeException} to exercise the global exception handler.
     *
     * @return never returns normally
     */
    @GetMapping("/throw")
    String throwException() {
        throw new RuntimeException("simulated error");
    }
}
