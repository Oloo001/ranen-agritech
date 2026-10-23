package com.ranen.ranen;

import org.springframework.context.annotation.Profile;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@Profile("dev")
public class AdminController {

    private final AdviceJob adviceJob;

    public AdminController(AdviceJob adviceJob) {
        this.adviceJob = adviceJob;
    }

    @GetMapping("/admin/run-advice")
    public String runAdvice() {
        return "Sent " + adviceJob.run() + " message(s)";
    }
}
