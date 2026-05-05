package edu.touro.las.mcon364.streams.ds;

import org.junit.jupiter.api.Test;

import java.util.*;
import java.util.stream.Collectors;

import static org.junit.jupiter.api.Assertions.*;

public class WeatherDataScienceExerciseTest {
    // ---------- parseRow tests ----------

    @Test
    void parseRow_validRow_returnsRecord() {
        String row = "ST01,New York,2025-01-01,3.5,65,0.0";

        Optional<WeatherDataScienceExercise.WeatherRecord> result =
                WeatherDataScienceExercise.parseRow(row);

        assertTrue(result.isPresent());
        WeatherDataScienceExercise.WeatherRecord r = result.get();

        assertEquals("ST01", r.stationId());
        assertEquals("New York", r.city());
        assertEquals("2025-01-01", r.date());
        assertEquals(3.5, r.temperatureC(), 0.0001);
        assertEquals(65, r.humidity());
        assertEquals(0.0, r.precipitationMm(), 0.0001);
    }

    @Test
    void parseRow_tooFewColumns_returnsEmpty() {
        String row = "ST01,New York,2025-01-01";

        Optional<WeatherDataScienceExercise.WeatherRecord> result =
                WeatherDataScienceExercise.parseRow(row);

        assertTrue(result.isEmpty());
    }

    @Test
    void parseRow_missingTemperature_returnsEmpty() {
        String row = "ST01,New York,2025-01-01,,65,0.0";

        Optional<WeatherDataScienceExercise.WeatherRecord> result =
                WeatherDataScienceExercise.parseRow(row);

        assertTrue(result.isEmpty());
    }

    @Test
    void parseRow_nonNumericTemperature_returnsEmpty() {
        String row = "ST01,New York,2025-01-01,abc,65,0.0";

        Optional<WeatherDataScienceExercise.WeatherRecord> result =
                WeatherDataScienceExercise.parseRow(row);

        assertTrue(result.isEmpty());
    }


    // ---------- isValid tests ----------

    private WeatherDataScienceExercise.WeatherRecord record(
            double temp, int humidity, double precip
    ) {
        return new WeatherDataScienceExercise.WeatherRecord(
                "ST01", "City", "2025-01-01", temp, humidity, precip
        );
    }

    @Test
    void isValid_temperatureUpperBoundary_valid() {
        assertTrue(WeatherDataScienceExercise.isValid(record(60, 50, 0)));
    }

    @Test
    void isValid_temperatureLowerBoundary_valid() {
        assertTrue(WeatherDataScienceExercise.isValid(record(-60, 50, 0)));
    }

    @Test
    void isValid_temperatureTooHigh_invalid() {
        assertFalse(WeatherDataScienceExercise.isValid(record(61, 50, 0)));
    }

    @Test
    void isValid_temperatureTooLow_invalid() {
        assertFalse(WeatherDataScienceExercise.isValid(record(-61, 50, 0)));
    }

    @Test
    void isValid_humidityLowerBoundary_valid() {
        assertTrue(WeatherDataScienceExercise.isValid(record(20, 0, 0)));
    }

    @Test
    void isValid_humidityUpperBoundary_valid() {
        assertTrue(WeatherDataScienceExercise.isValid(record(20, 100, 0)));
    }

    @Test
    void isValid_humidityTooLow_invalid() {
        assertFalse(WeatherDataScienceExercise.isValid(record(20, -1, 0)));
    }

    @Test
    void isValid_humidityTooHigh_invalid() {
        assertFalse(WeatherDataScienceExercise.isValid(record(20, 101, 0)));
    }

    @Test
    void isValid_negativePrecipitation_invalid() {
        assertFalse(WeatherDataScienceExercise.isValid(record(20, 50, -1)));
    }

    @Test
    void isValid_zeroPrecipitation_valid() {
        assertTrue(WeatherDataScienceExercise.isValid(record(20, 50, 0)));
    }


    // ---------- Integration tests ----------

    private List<WeatherDataScienceExercise.WeatherRecord> getCleanedSample() {
        List<String> rows = List.of(
                "stationId,city,date,temperatureC,humidity,precipitationMm",
                "ST01,New York,2025-01-01,3.5,65,0.0",
                "ST02,Boston,2025-01-01,-2.0,58,0.0",
                "ST03,Chicago,2025-01-01,-5.1,72,2.3",
                "ST04,Miami,2025-01-01,24.8,80,0.0",
                "BADROW",
                "ST05,Denver,2025-01-01,1.2,150,0.0"
        );

        return rows.stream()
                .skip(1)
                .map(WeatherDataScienceExercise::parseRow)
                .flatMap(Optional::stream)
                .filter(WeatherDataScienceExercise::isValid)
                .toList();
    }

    @Test
    void integration_cleanedList_notEmpty() {
        List<WeatherDataScienceExercise.WeatherRecord> cleaned = getCleanedSample();
        assertFalse(cleaned.isEmpty());
    }

    @Test
    void integration_allRecordsAreValid() {
        List<WeatherDataScienceExercise.WeatherRecord> cleaned = getCleanedSample();

        assertTrue(cleaned.stream()
                .allMatch(WeatherDataScienceExercise::isValid));
    }

    @Test
    void integration_highestAverageTemperatureCity_notNullOrEmpty() {
        List<WeatherDataScienceExercise.WeatherRecord> cleaned = getCleanedSample();

        String hottestCity = cleaned.stream()
                .collect(Collectors.groupingBy(
                        WeatherDataScienceExercise.WeatherRecord::city,
                        Collectors.averagingDouble(
                                WeatherDataScienceExercise.WeatherRecord::temperatureC
                        )
                ))
                .entrySet().stream()
                .max(Map.Entry.comparingByValue())
                .map(Map.Entry::getKey)
                .orElse(null);

        assertNotNull(hottestCity);
        assertFalse(hottestCity.isEmpty());
    }

    @Test
    void integration_wettestDay_precipitationNonNegative() {
        List<WeatherDataScienceExercise.WeatherRecord> cleaned = getCleanedSample();

        WeatherDataScienceExercise.WeatherRecord wettest =
                cleaned.stream()
                        .max(Comparator.comparingDouble(
                                WeatherDataScienceExercise.WeatherRecord::precipitationMm
                        ))
                        .orElse(null);

        assertNotNull(wettest);
        assertTrue(wettest.precipitationMm() >= 0);
    }
}
