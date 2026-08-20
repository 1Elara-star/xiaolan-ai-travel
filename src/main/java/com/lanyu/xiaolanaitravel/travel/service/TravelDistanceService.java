package com.lanyu.xiaolanaitravel.travel.service;

import org.springframework.stereotype.Service;

import java.math.BigDecimal;

/**
 * 旅行地点距离计算服务。
 *
 * 当前只负责根据两组经纬度，
 * 使用 Haversine 公式计算两点之间的球面直线距离。
 *
 * 注意：
 * 这里得到的不是实际道路距离，
 * 只能作为用户选择交通方式时的参考。
 *
 * 实际步行 / 驾车 / 公交 / 骑行距离和耗时，
 * 后续仍然由高德路线服务提供。
 */
@Service
public class TravelDistanceService {

    /**
     * 地球平均半径，单位：米。
     */
    private static final double EARTH_RADIUS_METERS =
            6_371_000D;

    /**
     * 计算两个经纬度坐标之间的直线距离。
     *
     * @return 距离，单位：米
     */
    public int calculateStraightLineDistanceMeters(
            BigDecimal originLongitude,
            BigDecimal originLatitude,
            BigDecimal destinationLongitude,
            BigDecimal destinationLatitude) {

        validateCoordinate(
                originLongitude,
                originLatitude,
                "起点"
        );

        validateCoordinate(
                destinationLongitude,
                destinationLatitude,
                "终点"
        );

        double originLatRadians =
                Math.toRadians(
                        originLatitude.doubleValue()
                );

        double destinationLatRadians =
                Math.toRadians(
                        destinationLatitude.doubleValue()
                );

        double latitudeDifference =
                Math.toRadians(
                        destinationLatitude.doubleValue()
                                - originLatitude.doubleValue()
                );

        double longitudeDifference =
                Math.toRadians(
                        destinationLongitude.doubleValue()
                                - originLongitude.doubleValue()
                );

        /*
         * Haversine 公式。
         *
         * 先根据经纬度差计算球面上的夹角，
         * 再乘地球半径得到两点之间的球面直线距离。
         */
        double a =
                Math.sin(latitudeDifference / 2)
                        * Math.sin(latitudeDifference / 2)
                        + Math.cos(originLatRadians)
                        * Math.cos(destinationLatRadians)
                        * Math.sin(longitudeDifference / 2)
                        * Math.sin(longitudeDifference / 2);

        double c =
                2 * Math.atan2(
                        Math.sqrt(a),
                        Math.sqrt(1 - a)
                );

        double distanceMeters =
                EARTH_RADIUS_METERS * c;

        /*
         * 最终统一四舍五入成整数米。
         */
        return (int) Math.round(distanceMeters);
    }

    /**
     * 校验经纬度是否合法。
     */
    private void validateCoordinate(
            BigDecimal longitude,
            BigDecimal latitude,
            String pointName) {

        if (longitude == null
                || latitude == null) {

            throw new IllegalArgumentException(
                    pointName + "缺少经纬度"
            );
        }

        if (longitude.compareTo(
                BigDecimal.valueOf(-180)) < 0
                || longitude.compareTo(
                BigDecimal.valueOf(180)) > 0) {

            throw new IllegalArgumentException(
                    pointName + "经度超出有效范围"
            );
        }

        if (latitude.compareTo(
                BigDecimal.valueOf(-90)) < 0
                || latitude.compareTo(
                BigDecimal.valueOf(90)) > 0) {

            throw new IllegalArgumentException(
                    pointName + "纬度超出有效范围"
            );
        }
    }
}