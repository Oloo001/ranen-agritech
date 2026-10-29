package com.ranen.ranen.channels;

import com.ranen.ranen.calendar.CalendarService;
import com.ranen.ranen.farm.CropPlot;
import com.ranen.ranen.farm.PlotStore;
import com.ranen.ranen.common.PhoneNumbers;
import com.ranen.ranen.identity.FarmerRepository;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import java.time.LocalDate;
import java.time.ZoneId;
import java.time.temporal.ChronoUnit;
import java.util.List;

@RestController
public class UssdController {

    private static final ZoneId NAIROBI = ZoneId.of("Africa/Nairobi");

    private final PlotStore plotStore;
    private final CalendarService calendarService;
    private final FarmerRepository farmerRepository;

    public UssdController(PlotStore plotStore, CalendarService calendarService,
                          FarmerRepository farmerRepository) {
        this.plotStore = plotStore;
        this.calendarService = calendarService;
        this.farmerRepository = farmerRepository;
    }

    private static String fit(String reply) {
        return reply.length() <= 180 ? reply : reply.substring(0, 177) + "...";
    }

    @PostMapping(value = "/ussd", produces = "text/plain")
    public String handle(@RequestParam(name = "phoneNumber", defaultValue = "") String rawPhoneNumber,
                         @RequestParam(defaultValue = "") String text) {

        String phoneNumber = PhoneNumbers.normalize(rawPhoneNumber).orElse(null);
        if (phoneNumber == null) {
            return "END Sorry, this service works with Kenyan mobile numbers only.";
        }

        // ... the rest of the method stays exactly as it is

        // "1*2*1" becomes ["1", "2", "1"]; empty text becomes an empty array
        String[] steps = text.isEmpty() ? new String[0] : text.split("\\*");

        if (steps.length == 0) {
            return "CON Habari " + phoneNumber + "!\nRanen AgriTech\n"
                    + "1. Register new plot\n2. My crop calendar\n3. Find agrovets\n4. Language/Lugha"
                    + "\n5. Log harvest";
        }

        String reply = switch (steps[0]) {
            case "1" -> registerPlot(phoneNumber, steps);
            case "2" -> showCalendar(phoneNumber, steps);
            case "3" -> "END Agrovet directory coming soon.";
            case "4" -> chooseLanguage(phoneNumber, steps);
            case "5" -> logHarvest(phoneNumber, steps);
            default -> "END Invalid choice.";
        };

        System.out.println("USSD reply length: " + reply.length());
        return fit(reply);
    }

    private String registerPlot(String phone, String[] steps) {
        // steps[0] = "1" (register), steps[1] = crop, steps[2] = when planted
        if (steps.length == 1) {
            return "CON Which crop?\n1. Cassava\n2. Sweet potato";
        }
        if (!steps[1].equals("1") && !steps[1].equals("2")) {
            return "END Invalid choice.";
        }
        if (steps.length == 2) {
            return "CON When did you plant?\n1. Today\n2. About 1 week ago\n3. About 2 weeks ago";
        }

        int daysAgo = switch (steps[2]) {
            case "1" -> 0;
            case "2" -> 7;
            case "3" -> 14;
            default -> -1;
        };
        if (daysAgo < 0) {
            return "END Invalid choice.";
        }

        String crop = steps[1].equals("1") ? "Cassava" : "Sweet potato";
        LocalDate planted = LocalDate.now(NAIROBI).minusDays(daysAgo);
        plotStore.addPlot(phone, crop, planted);
        farmerRepository.registerConsent(phone);

        return "END " + crop + " plot registered. You will get SMS advice.";
    }

    private String showCalendar(String phone, String[] steps) {
        List<CropPlot> plots = plotStore.getPlots(phone);
        if (plots.isEmpty()) {
            return "END You have no plots yet. Dial again and choose 1.";
        }
        if (plots.size() == 1) {
            return plotDetails(plots.get(0));
        }

        // Several plots: first ask which one
        if (steps.length == 1) {
            StringBuilder sb = new StringBuilder("CON Which plot?\n");
            for (int i = 0; i < plots.size(); i++) {
                sb.append(i + 1).append(". ").append(plots.get(i).crop()).append("\n");
            }
            return sb.toString();
        }

        // steps[1] is the number they picked
        int choice;
        try {
            choice = Integer.parseInt(steps[1]);
        } catch (NumberFormatException e) {
            return "END Invalid choice.";
        }
        if (choice < 1 || choice > plots.size()) {
            return "END Invalid choice.";
        }
        return plotDetails(plots.get(choice - 1));
    }

    private String chooseLanguage(String phone, String[] steps) {
        if (steps.length == 1) {
            return "CON Choose language / Chagua lugha\n1. English\n2. Kiswahili\n3. Dholuo";
        }
        String lang = switch (steps[1]) {
            case "1" -> "en";
            case "2" -> "sw";
            case "3" -> "luo";
            default -> null;
        };
        if (lang == null) {
            return "END Invalid choice.";
        }
        farmerRepository.setLanguage(phone, lang);
        return "END Saved. / Imehifadhiwa.";
    }

    private String plotDetails(CropPlot plot) {
        long dap = ChronoUnit.DAYS.between(plot.plantedDate(), LocalDate.now(NAIROBI));

        String current = calendarService.currentMilestone(plot.crop(), dap)
                .map(m -> "Task: " + m.name() + "\n" + m.advice())
                .orElse("No task right now.");
        String next = calendarService.nextMilestone(plot.crop(), dap)
                .map(m -> "\nNext: " + m.name() + " in " + (m.dapStart() - dap) + " days")
                .orElse("");

        return "END " + plot.crop() + " (day " + dap + ")\n" + current + next;
    }

    private String logHarvest(String phone, String[] steps) {
        List<CropPlot> plots = plotStore.getPlots(phone);
        if (plots.isEmpty()) {
            return "END You have no active plots.";
        }
        if (steps.length == 1) {
            StringBuilder sb = new StringBuilder("CON Which plot did you harvest?\n");
            for (int i = 0; i < plots.size(); i++) {
                sb.append(i + 1).append(". ").append(plots.get(i).crop()).append("\n");
            }
            return sb.toString();
        }

        int choice;
        try {
            choice = Integer.parseInt(steps[1]);
        } catch (NumberFormatException e) {
            return "END Invalid choice.";
        }
        if (choice < 1 || choice > plots.size()) {
            return "END Invalid choice.";
        }
        if (steps.length == 2) {
            return "CON Enter harvest in kg (numbers only):";
        }

        int kg;
        try {
            kg = Integer.parseInt(steps[2].trim());
        } catch (NumberFormatException e) {
            return "END Invalid amount.";
        }
        if (kg < 1 || kg > 100000) {
            return "END Invalid amount.";
        }

        CropPlot plot = plots.get(choice - 1);

        // Step 3: show what we understood and ask before saving anything.
        if (steps.length == 3) {
            return "CON Record " + kg + " kg of " + plot.crop() + "?\n1. Confirm\n2. Cancel";
        }

        // Step 4: the farmer's answer.
        return switch (steps[3]) {
            case "1" -> plotStore.recordHarvest(plot.plotId(), phone, kg)
                    ? "END Harvest of " + kg + " kg recorded. Thank you!"
                    : "END Could not record this harvest. Please try again.";
            case "2" -> "END Cancelled. Nothing was saved.";
            default -> "END Invalid choice.";
        };
    }
}