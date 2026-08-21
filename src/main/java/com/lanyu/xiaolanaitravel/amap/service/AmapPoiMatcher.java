package com.lanyu.xiaolanaitravel.amap.service;

import com.lanyu.xiaolanaitravel.amap.dto.AmapPoiItem;
import org.springframework.stereotype.Component;

import java.math.BigDecimal;
import java.util.List;
import java.util.Locale;
import java.util.Optional;
import java.util.stream.Stream;

/** 根据地点名称和旅行目的地，从高德候选结果中选择可信 POI。 */
@Component
public class AmapPoiMatcher {

    public Optional<AmapPoiItem> findBest(
            String placeName,
            String destination,
            List<AmapPoiItem> candidates) {

        if (candidates == null || candidates.isEmpty()) {
            return Optional.empty();
        }

        String normalizedPlaceName = normalizeName(placeName);
        List<AmapPoiItem> usableCandidates = candidates.stream()
                .filter(this::hasValidCoordinate)
                .filter(candidate -> belongsToDestination(candidate, destination))
                .toList();

        Optional<AmapPoiItem> exactMatch = usableCandidates.stream()
                .filter(candidate -> normalizeName(candidate.getName())
                        .equals(normalizedPlaceName))
                .findFirst();
        if (exactMatch.isPresent()) {
            return exactMatch;
        }

        Optional<AmapPoiItem> containsMatch = usableCandidates.stream()
                .filter(candidate -> {
                    String candidateName = normalizeName(candidate.getName());
                    return candidateName.contains(normalizedPlaceName)
                            || normalizedPlaceName.contains(candidateName);
                })
                .findFirst();
        return containsMatch.isPresent()
                ? containsMatch
                : usableCandidates.stream().findFirst();
    }

    private boolean belongsToDestination(AmapPoiItem poi, String destination) {
        String expected = normalizeAdministrativeName(destination);
        if (expected.isEmpty()) {
            return false;
        }

        return Stream.of(
                        poi.getCityname(),
                        poi.getPname(),
                        poi.getAdname(),
                        poi.getAddress())
                .map(this::normalizeAdministrativeName)
                .filter(value -> !value.isEmpty())
                .anyMatch(value -> value.equals(expected)
                        || value.contains(expected)
                        || expected.contains(value));
    }

    private boolean hasValidCoordinate(AmapPoiItem poi) {
        if (poi == null || poi.getLocation() == null) {
            return false;
        }
        try {
            String[] parts = poi.getLocation().split(",", -1);
            if (parts.length != 2) {
                return false;
            }
            BigDecimal longitude = new BigDecimal(parts[0].strip());
            BigDecimal latitude = new BigDecimal(parts[1].strip());
            return longitude.compareTo(BigDecimal.valueOf(-180)) >= 0
                    && longitude.compareTo(BigDecimal.valueOf(180)) <= 0
                    && latitude.compareTo(BigDecimal.valueOf(-90)) >= 0
                    && latitude.compareTo(BigDecimal.valueOf(90)) <= 0;
        } catch (NumberFormatException exception) {
            return false;
        }
    }

    private String normalizeName(String value) {
        if (value == null) {
            return "";
        }
        return value.strip()
                .replaceAll("[\\s·•（）()—-]", "")
                .toLowerCase(Locale.ROOT);
    }

    private String normalizeAdministrativeName(String value) {
        if (value == null) {
            return "";
        }
        return value.strip()
                .replaceAll("\\s+", "")
                .replaceFirst("(特别行政区|自治州|自治区|地区|市|盟)$", "")
                .toLowerCase(Locale.ROOT);
    }
}
