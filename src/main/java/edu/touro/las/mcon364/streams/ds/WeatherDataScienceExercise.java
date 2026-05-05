package edu.touro.las.mcon364.streams.ds;

import java.io.*;
import java.nio.charset.StandardCharsets;
import java.nio.file.NoSuchFileException;
import java.util.*;
import java.util.stream.*;


public class WeatherDataScienceExercise {

    record WeatherRecord(
            String stationId,
            String city,
            String date,
            double temperatureC,
            int humidity,
            double precipitationMm
    ) {}

    public static void main(String[] args) throws Exception {
        List<String> rows = readCsvRows("noaa_weather_sample_200_rows.csv");

        List<WeatherRecord> cleaned = rows.stream()
                .skip(1) // skip header
                .map(WeatherDataScienceExercise::parseRow)
                .flatMap(Optional::stream)
                .filter(WeatherDataScienceExercise::isValid)
                .toList();

        System.out.println("Total raw rows (excluding header): " + (rows.size() - 1));
        System.out.println("Total cleaned rows: " + cleaned.size());

        // TODO 1:
        // Count how many valid weather records remain after cleaning.
        long validCount = cleaned.stream().count();
        System.out.println("Valid records: " + validCount);

        // TODO 2:
        // Compute the average temperature across all valid rows.
        double avgTemp = cleaned.stream()
                .mapToDouble(WeatherRecord::temperatureC)
                .average()
                .orElse(0.0);
        System.out.println("Average temperature: " + avgTemp);

        // TODO 3:
        // Find the city with the highest average temperature.
        String hottestCity = cleaned.stream()
                .collect(Collectors.groupingBy(
                        WeatherRecord::city,
                        Collectors.averagingDouble(WeatherRecord::temperatureC)
                ))
                .entrySet().stream()
                .max(Map.Entry.comparingByValue())
                .map(Map.Entry::getKey)
                .orElse(null);

        System.out.println("City with highest avg temp: " + hottestCity);


        // TODO 4:
        // Group records by city.
        Map<String, List<WeatherRecord>> byCity = cleaned.stream()
                .collect(Collectors.groupingBy(WeatherRecord::city));

        System.out.println("Grouped by city: " + byCity);

        // TODO 5:
        // Compute average precipitation by city.
        Map<String, Double> avgPrecByCity = cleaned.stream()
                .collect(Collectors.groupingBy(
                        WeatherRecord::city,
                        Collectors.averagingDouble(WeatherRecord::precipitationMm)
                ));

        System.out.println("Average precipitation by city: " + avgPrecByCity);

        // TODO 6:
        // Partition rows into freezing days (temperature <= 0)
        // and non-freezing days (temperature > 0).
        Map<Boolean, List<WeatherRecord>> partionedByTemp = cleaned.stream()
                .collect(Collectors.partitioningBy(
                        r -> r.temperatureC() <= 0
                ));
        List<WeatherRecord> freezingDays = partionedByTemp.get(true);
        List<WeatherRecord> nonFreezingDays = partionedByTemp.get(false);

        System.out.println("Freezing days: " + freezingDays.size());
        System.out.println("Non-freezing days: " + nonFreezingDays.size());

        // TODO 7:
        // Create a Set<String> of all distinct cities.
        Set<String> distinctCities = cleaned.stream()
                .map(WeatherRecord::city)
                .collect(Collectors.toSet());

        System.out.println("Distinct cities: " + distinctCities);

        // TODO 8:
        // Find the wettest single day.
        WeatherRecord wettest = cleaned.stream()
                .max(Comparator.comparingDouble(WeatherRecord::precipitationMm))
                .orElse(null);

        if (wettest != null) {
            System.out.println(
                    "Wettest day: " +
                            wettest.city() + " on " +
                            wettest.date() +
                            " with " +
                            wettest.precipitationMm() + " mm"
            );

            // TODO 9:
            // Create a Map<String, Double> from city to average humidity.
            Map<String, Double> avgHumidity = cleaned.stream()
                    .collect(Collectors.groupingBy(WeatherRecord::city,
                            Collectors.averagingDouble(WeatherRecord::humidity)
                    ));

            System.out.println("Average humidity by city: " + avgHumidity);

            // TODO 10:
            // Produce a list of formatted strings like:
            // "Miami on 2025-01-02: 25.1C, humidity 82%"
            List<String> formatted = cleaned.stream()
                    .map(r -> r.city() + " on " + r.date() + ": "
                            + r.temperatureC() + "C, humidity "
                            + r.humidity() + "%")
                    .toList();

            formatted.forEach(System.out::println);

            // TODO 11 (optional):
            // Build a Map<String, CityWeatherSummary> for all cities.
            Map<String, CityWeatherSummary> summaryByCity = cleaned.stream()
                    .collect(Collectors.groupingBy(
                            WeatherRecord::city,
                            Collectors.collectingAndThen(
                                    Collectors.toList(),
                                    list -> new CityWeatherSummary(
                                            list.get(0).city(),
                                            list.size(),
                                            list.stream()
                                                    .mapToDouble(WeatherRecord::temperatureC)
                                                    .average()
                                                    .orElse(0.0),
                                            list.stream()
                                                    .mapToDouble(WeatherRecord::precipitationMm)
                                                    .average()
                                                    .orElse(0.0),
                                            list.stream()
                                                    .mapToDouble(WeatherRecord::temperatureC)
                                                    .max()
                                                    .orElse(0.0)
                                    )
                            )
                    ));

            summaryByCity.forEach((city, summary) ->
                    System.out.println(city + " → " + summary)
            );
            // Put your code below these comments or refactor into helper methods.
        }
    }

    static Optional<WeatherRecord> parseRow(String row) {
        // TODO:
        // 1. Split the row by commas
        // 2. Reject malformed rows
        // 3. Reject rows with missing temperature
        // 4. Parse numeric values safely
        // 5. Return Optional.empty() if parsing fails

        try {
            String[] parts =  row.split(",");

            if(parts.length != 6) {
                return Optional.empty();
            }

            String stationId = parts[0].trim();
            String city = parts[1].trim();
            String date = parts[2].trim();
            String temperatureC = parts[3].trim();
            String humidity = parts[4].trim();
            String precipitationMm = parts[5].trim();

            if (temperatureC == null || temperatureC.isBlank()) {
                return Optional.empty();
            }

            double temperatureDouble = Double.parseDouble(temperatureC);
            int humidityInt = Integer.parseInt(humidity);
            double precipitationMmDouble = Double.parseDouble(precipitationMm);

            WeatherRecord record = new WeatherRecord(
                    stationId,
                    city,
                    date,
                    temperatureDouble,
                    humidityInt,
                    precipitationMmDouble
            );

            return Optional.of(record);
        } catch (NumberFormatException e) {
            return Optional.empty();
        }
    }

    static boolean isValid(WeatherRecord r) {
        // TODO:
        // Keep only rows where:
        // - temperature is between -60 and 60
        // - humidity is between 0 and 100
        // - precipitation is >= 0
        return r.temperatureC >= -60  && r.temperatureC <= 60
                && r.humidity >= 0 && r.humidity <= 100
                && r.precipitationMm >= 0;
    }

    record CityWeatherSummary(
            String city,
            long dayCount,
            double avgTemp,
            double avgPrecipitation,
            double maxTemp
    ) {}

    private static List<String> readCsvRows(String fileName) throws IOException {
        InputStream in = WeatherDataScienceExercise.class.getResourceAsStream(fileName);
        if (in == null) {
            throw new NoSuchFileException("Classpath resource not found: " + fileName);
        }
        try (BufferedReader reader = new BufferedReader(new InputStreamReader(in, StandardCharsets.UTF_8))) {
            return reader.lines().toList();
        }
    }
}
