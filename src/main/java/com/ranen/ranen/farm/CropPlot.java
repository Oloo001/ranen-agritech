package com.ranen.ranen.farm;

import java.time.LocalDate;

public record CropPlot(int plotId, String crop, LocalDate plantedDate) {
}
