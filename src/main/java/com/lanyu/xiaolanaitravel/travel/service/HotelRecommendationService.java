package com.lanyu.xiaolanaitravel.travel.service;

import com.lanyu.xiaolanaitravel.ai.dto.FlyAiHotelItem;
import com.lanyu.xiaolanaitravel.travel.dto.HotelCandidateResponse;
import com.lanyu.xiaolanaitravel.travel.dto.HotelSearchCriteria;
import com.lanyu.xiaolanaitravel.travel.entity.TravelPlan;
import com.lanyu.xiaolanaitravel.user.entity.UserProfile;
import org.springframework.stereotype.Service;

import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;
import java.util.Locale;
import java.util.Objects;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

/**
 * 使用本次查询条件和用户画像为飞猪真实酒店候选排序。
 *
 * <p>这里计算的是小兰内部匹配顺序，不是酒店平台评分。</p>
 */
@Service
public class HotelRecommendationService {

    private static final Pattern PRICE_PATTERN = Pattern.compile("\\d+(?:\\.\\d+)?");
    private static final String ECONOMY_KEYWORDS = "性价比|预算|省钱|经济|便宜|实惠";
    private static final String COMFORT_KEYWORDS = "舒适|品质|体验|高档|星级|豪华|安静|干净";

    public List<HotelCandidateResponse> rank(
            TravelPlan plan,
            UserProfile profile,
            List<FlyAiHotelItem> sourceHotels,
            HotelSearchCriteria criteria) {

        List<PricedHotel> hotels = sourceHotels == null
                ? List.of()
                : sourceHotels.stream()
                .filter(Objects::nonNull)
                .map(item -> new PricedHotel(item, parsePrice(item.getPrice())))
                .filter(item -> withinPriceRange(item.price(), criteria))
                .toList();

        List<Integer> knownPrices = hotels.stream()
                .map(PricedHotel::price)
                .filter(Objects::nonNull)
                .toList();
        Integer lowestPrice = knownPrices.stream().min(Integer::compareTo).orElse(null);
        Integer highestPrice = knownPrices.stream().max(Integer::compareTo).orElse(null);
        boolean profileUsed = hasAccommodationProfile(profile);

        return hotels.stream()
                .map(hotel -> toResponse(
                        plan, profile, hotel, criteria,
                        lowestPrice, highestPrice, profileUsed))
                .sorted(Comparator.comparing(
                                HotelCandidateResponse::overallMatchScore,
                                Comparator.nullsLast(Comparator.reverseOrder()))
                        .thenComparing(HotelCandidateResponse::priceValue,
                                Comparator.nullsLast(Integer::compareTo)))
                .toList();
    }

    private HotelCandidateResponse toResponse(
            TravelPlan plan,
            UserProfile profile,
            PricedHotel pricedHotel,
            HotelSearchCriteria criteria,
            Integer lowestPrice,
            Integer highestPrice,
            boolean profileUsed) {

        FlyAiHotelItem hotel = pricedHotel.hotel();
        List<String> reasons = new ArrayList<>();
        int tripScore = 45;

        String locationKeyword = normalize(criteria.locationKeyword());
        if (locationKeyword != null) {
            tripScore += 20;
            String locationLabel = criteria.locationType() == null
                    ? "指定地点"
                    : criteria.locationType().getLabel();
            reasons.add("按" + locationLabel + "“" + locationKeyword + "”周边查询");
            if (containsIgnoreCase(hotel.getName(), locationKeyword)
                    || containsIgnoreCase(hotel.getAddress(), locationKeyword)) {
                tripScore += 15;
                reasons.add("酒店名称或地址与住宿位置直接匹配");
            }
        } else if (plan != null && normalize(plan.getDestination()) != null) {
            reasons.add("来自本次目的地“" + plan.getDestination().strip() + "”的酒店候选");
        }

        if (pricedHotel.price() != null
                && (criteria.minPrice() != null || criteria.maxPrice() != null)) {
            tripScore += 10;
            reasons.add("符合本次设置的价格范围");
        }
        if (hasCoordinates(hotel)) {
            tripScore += 10;
            reasons.add("坐标完整，选中后可加入行程路线");
        }
        if (normalize(hotel.getMainPic()) != null) {
            tripScore += 5;
        }
        tripScore = Math.min(tripScore, 100);

        int profileScore = profileUsed
                ? calculateProfileScore(profile, pricedHotel, lowestPrice, highestPrice, reasons)
                : 50;
        if (!profileUsed) {
            reasons.add("尚未填写住宿偏好，当前主要按本次行程条件排序");
        }

        int overallScore = profileUsed
                ? Math.round(tripScore * 0.6f + profileScore * 0.4f)
                : tripScore;

        return new HotelCandidateResponse(
                hotel.getName(), hotel.getPrice(), hotel.getAddress(),
                hotel.getLatitude(), hotel.getLongitude(), hotel.getMainPic(),
                hotel.getDetailUrl(), hotel.getStar(), hotel.getBrandName(), "FLIGGY",
                pricedHotel.price(), tripScore, profileScore, overallScore,
                profileUsed, reasons.stream().distinct().limit(5).toList());
    }

    private int calculateProfileScore(
            UserProfile profile,
            PricedHotel hotel,
            Integer lowestPrice,
            Integer highestPrice,
            List<String> reasons) {

        String budgetPreference = normalize(profile.getBudgetPreference());
        String accommodationPreference = normalize(profile.getAccommodationPreference());
        String profileText = String.join(" ",
                budgetPreference == null ? "" : budgetPreference,
                accommodationPreference == null ? "" : accommodationPreference);
        int score = 45;

        if (profileText.matches(".*(" + ECONOMY_KEYWORDS + ").*")) {
            int priceFit = relativeBudgetScore(hotel.price(), lowestPrice, highestPrice);
            score += priceFit;
            if (priceFit >= 20) {
                reasons.add("价格更符合你的性价比偏好");
            }
        }

        if (profileText.matches(".*(" + COMFORT_KEYWORDS + ").*")) {
            int comfort = comfortLevel(hotel.hotel().getStar());
            score += comfort * 7;
            if (comfort >= 3) {
                reasons.add("住宿等级更符合你的舒适度偏好");
            }
        }

        if (matchesAccommodationType(accommodationPreference, hotel.hotel())) {
            score += 15;
            reasons.add("住宿类型与你填写的偏好相符");
        }

        return Math.min(score, 100);
    }

    private boolean withinPriceRange(Integer price, HotelSearchCriteria criteria) {
        if (price == null) {
            return criteria.minPrice() == null && criteria.maxPrice() == null;
        }
        return (criteria.minPrice() == null || price >= criteria.minPrice())
                && (criteria.maxPrice() == null || price <= criteria.maxPrice());
    }

    private int relativeBudgetScore(Integer price, Integer lowestPrice, Integer highestPrice) {
        if (price == null || lowestPrice == null || highestPrice == null) return 0;
        if (Objects.equals(lowestPrice, highestPrice)) return 25;
        double normalized = (double) (highestPrice - price) / (highestPrice - lowestPrice);
        return (int) Math.round(Math.max(0, Math.min(1, normalized)) * 35);
    }

    private int comfortLevel(String star) {
        String value = normalize(star);
        if (value == null) return 1;
        if (value.matches(".*(五星|豪华).*")) return 5;
        if (value.matches(".*(四星|高档).*")) return 4;
        if (value.matches(".*(三星|舒适).*")) return 3;
        if (value.matches(".*(二星|经济).*")) return 2;
        return 1;
    }

    private boolean matchesAccommodationType(String preference, FlyAiHotelItem hotel) {
        if (preference == null) return false;
        String hotelText = (normalize(hotel.getName()) + " "
                + normalize(hotel.getBrandName()) + " " + normalize(hotel.getStar()))
                .toLowerCase(Locale.ROOT);
        if (preference.contains("民宿") && hotelText.contains("民宿")) return true;
        if (preference.contains("公寓") && hotelText.contains("公寓")) return true;
        return preference.contains("酒店") && hotelText.contains("酒店");
    }

    private boolean hasAccommodationProfile(UserProfile profile) {
        return profile != null && (normalize(profile.getBudgetPreference()) != null
                || normalize(profile.getAccommodationPreference()) != null);
    }

    private boolean hasCoordinates(FlyAiHotelItem hotel) {
        return normalize(hotel.getLongitude()) != null && normalize(hotel.getLatitude()) != null;
    }

    private boolean containsIgnoreCase(String source, String keyword) {
        return source != null && source.toLowerCase(Locale.ROOT)
                .contains(keyword.toLowerCase(Locale.ROOT));
    }

    private Integer parsePrice(String price) {
        if (price == null) return null;
        Matcher matcher = PRICE_PATTERN.matcher(price.replace(",", ""));
        if (!matcher.find()) return null;
        try {
            return (int) Math.round(Double.parseDouble(matcher.group()));
        } catch (NumberFormatException ignored) {
            return null;
        }
    }

    private String normalize(String value) {
        return value == null || value.isBlank() ? null : value.strip();
    }

    private record PricedHotel(FlyAiHotelItem hotel, Integer price) {
    }
}
