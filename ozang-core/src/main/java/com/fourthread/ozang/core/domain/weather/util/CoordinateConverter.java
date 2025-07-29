package com.fourthread.ozang.core.domain.weather.util;

import com.fourthread.ozang.core.domain.weather.entity.GridCoordinate;
import org.springframework.stereotype.Component;

@Component
public class CoordinateConverter {

    // 기상청 공식 격자 변환 파라미터 (람베르트 정각원추도법)
    private static final double RE = 6371.00877; // 지구 반경 (km)
    private static final double GRID = 5.0; // 격자 간격 (km)
    private static final double SLAT1 = 30.0; // 투영 위도1 (degree)
    private static final double SLAT2 = 60.0; // 투영 위도2 (degree)
    private static final double OLON = 126.0; // 기준점 경도 (degree)
    private static final double OLAT = 38.0; // 기준점 위도 (degree)
    private static final double XO = 43.0; // 기준점 X (GRID)
    private static final double YO = 136.0; // 기준점 Y (GRID)

    private final ProjectionParameters projParams;

    public CoordinateConverter() {
        this.projParams = calculateProjectionParameters();
    }

    /**
     * 투영 변환에 필요한 공통 파라미터들을 계산하여 캐싱
     */
    private ProjectionParameters calculateProjectionParameters() {
        final double DEGRAD = Math.PI / 180.0;
        final double RADDEG = 180.0 / Math.PI;

        double re = RE / GRID;
        double slat1 = SLAT1 * DEGRAD;
        double slat2 = SLAT2 * DEGRAD;
        double olon = OLON * DEGRAD;
        double olat = OLAT * DEGRAD;

        // Lambert 투영 공통 파라미터 계산
        double sn = Math.tan(Math.PI * 0.25 + slat2 * 0.5) / Math.tan(Math.PI * 0.25 + slat1 * 0.5);
        sn = Math.log(Math.cos(slat1) / Math.cos(slat2)) / Math.log(sn);

        double sf = Math.tan(Math.PI * 0.25 + slat1 * 0.5);
        sf = Math.pow(sf, sn) * Math.cos(slat1) / sn;

        double ro = Math.tan(Math.PI * 0.25 + olat * 0.5);
        ro = re * sf / Math.pow(ro, sn);

        return new ProjectionParameters(DEGRAD, RADDEG, re, slat1, slat2, olon, olat, sn, sf, ro);
    }

    /**
     * 위·경도를 격자 좌표로 변환
     */
    public GridCoordinate convertToGrid(double lat, double lon) {
        double ra = Math.tan(Math.PI * 0.25 + lat * projParams.degrad * 0.5);
        ra = projParams.re * projParams.sf / Math.pow(ra, projParams.sn);

        double theta = lon * projParams.degrad - projParams.olon;
        if (theta > Math.PI) theta -= 2.0 * Math.PI;
        if (theta < -Math.PI) theta += 2.0 * Math.PI;
        theta *= projParams.sn;

        int x = (int) Math.floor(ra * Math.sin(theta) + XO + 0.5);
        int y = (int) Math.floor(projParams.ro - ra * Math.cos(theta) + YO + 0.5);

        return new GridCoordinate(x, y);
    }

    /**
     * 격자 좌표를 위·경도로 변환
     */
    public double[] convertGridToLatLon(GridCoordinate grid) {
        double xn = grid.getX() - XO;
        double yn = projParams.ro - grid.getY() + YO;

        double ra = Math.hypot(xn, yn);
        if (projParams.sn < 0.0) ra = -ra;

        double alat = Math.pow(projParams.re * projParams.sf / ra, 1.0 / projParams.sn);
        alat = 2.0 * Math.atan(alat) - Math.PI * 0.5;

        double theta = calculateTheta(xn, yn);
        double alon = theta / projParams.sn + projParams.olon;

        double lat = alat * projParams.raddeg;
        double lon = alon * projParams.raddeg;

        return new double[]{lat, lon};
    }

    /**
     * 각도 계산 로직 분리
     */
    private double calculateTheta(double xn, double yn) {
        if (Math.abs(xn) <= 1e-7) {
            return 0.0;
        } else if (Math.abs(yn) <= 1e-7) {
            return Math.PI * 0.5 * (xn < 0.0 ? -1 : 1);
        } else {
            return Math.atan2(xn, yn);
        }
    }

    /**
     * 파라미터를 담는 내부 클래스
     */
    private static class ProjectionParameters {
        final double degrad, raddeg, re, slat1, slat2, olon, olat, sn, sf, ro;

        ProjectionParameters(double degrad, double raddeg, double re, double slat1,
            double slat2, double olon, double olat, double sn, double sf, double ro) {
            this.degrad = degrad;
            this.raddeg = raddeg;
            this.re = re;
            this.slat1 = slat1;
            this.slat2 = slat2;
            this.olon = olon;
            this.olat = olat;
            this.sn = sn;
            this.sf = sf;
            this.ro = ro;
        }
    }
}
