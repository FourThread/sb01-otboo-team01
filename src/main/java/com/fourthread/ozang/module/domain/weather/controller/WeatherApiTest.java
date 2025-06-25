package com.fourthread.ozang.module.domain.weather.controller;

import java.io.InputStreamReader;
import java.net.HttpURLConnection;
import java.net.URL;
import java.net.URLEncoder;
import java.io.BufferedReader;
import java.io.IOException;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;


public class WeatherApiTest {
    public static void main(String[] args) throws IOException {
        // =============== 실제 API 키로 교체하세요 ===============

        String serviceKey = "%2F5geoBa8qF1S5p%2BUnKj6SZ9OS3uUHmZCjUiVZvFNmqW0dI8oNkeQquxUGbtYq%2BZtu6vULvbxSX%2FeDpAvnHJFRg%3D%3D";


        // 현재 시간 기준으로 API 시간 계산
        LocalDateTime now = LocalDateTime.now();
        int hour = now.getHour();

        // API 제공 시간에 맞춰 조정 (02, 05, 08, 11, 14, 17, 20, 23시)
        int[] apiTimes = {2, 5, 8, 11, 14, 17, 20, 23};
        int baseHour = 23; // 기본값

        for (int time : apiTimes) {
            if (hour >= time) {
                baseHour = time;
            }
        }

        LocalDateTime baseDateTime = now.withHour(baseHour).withMinute(0).withSecond(0).withNano(0);
        if (hour < 2) {
            baseDateTime = now.minusDays(1).withHour(23).withMinute(0).withSecond(0).withNano(0);
        }

        String baseDate = baseDateTime.format(DateTimeFormatter.ofPattern("yyyyMMdd"));
        String baseTime = baseDateTime.format(DateTimeFormatter.ofPattern("HHmm"));

        System.out.println("🕐 사용할 기준시간: " + baseDate + " " + baseTime);

        // 서울 좌표 (x=60, y=127)
        testApi(serviceKey, baseDate, baseTime, "60", "127", "서울");

        System.out.println("\n" + "=".repeat(50) + "\n");

        // 부산 좌표 (x=98, y=76)
        testApi(serviceKey, baseDate, baseTime, "98", "76", "부산");
    }

    private static void testApi(String serviceKey, String baseDate, String baseTime,
        String nx, String ny, String location) throws IOException {

        StringBuilder urlBuilder = new StringBuilder("http://apis.data.go.kr/1360000/VilageFcstInfoService_2.0/getVilageFcst");
        urlBuilder.append("?" + URLEncoder.encode("serviceKey","UTF-8") + "=" + serviceKey);
        urlBuilder.append("&" + URLEncoder.encode("pageNo","UTF-8") + "=" + URLEncoder.encode("1", "UTF-8"));
        urlBuilder.append("&" + URLEncoder.encode("numOfRows","UTF-8") + "=" + URLEncoder.encode("100", "UTF-8"));
        urlBuilder.append("&" + URLEncoder.encode("dataType","UTF-8") + "=" + URLEncoder.encode("XML", "UTF-8"));
        urlBuilder.append("&" + URLEncoder.encode("base_date","UTF-8") + "=" + URLEncoder.encode(baseDate, "UTF-8"));
        urlBuilder.append("&" + URLEncoder.encode("base_time","UTF-8") + "=" + URLEncoder.encode(baseTime, "UTF-8"));
        urlBuilder.append("&" + URLEncoder.encode("nx","UTF-8") + "=" + URLEncoder.encode(nx, "UTF-8"));
        urlBuilder.append("&" + URLEncoder.encode("ny","UTF-8") + "=" + URLEncoder.encode(ny, "UTF-8"));

        System.out.println("🌍 " + location + " 날씨 API 테스트");
        System.out.println("📍 좌표: x=" + nx + ", y=" + ny);
        System.out.println("🔗 요청 URL: " + urlBuilder.toString());

        URL url = new URL(urlBuilder.toString());
        HttpURLConnection conn = (HttpURLConnection) url.openConnection();
        conn.setRequestMethod("GET");
        conn.setRequestProperty("Content-type", "application/json");

        System.out.println("📡 Response code: " + conn.getResponseCode());

        BufferedReader rd;
        if(conn.getResponseCode() >= 200 && conn.getResponseCode() <= 300) {
            rd = new BufferedReader(new InputStreamReader(conn.getInputStream()));
        } else {
            rd = new BufferedReader(new InputStreamReader(conn.getErrorStream()));
        }

        StringBuilder sb = new StringBuilder();
        String line;
        while ((line = rd.readLine()) != null) {
            sb.append(line);
        }
        rd.close();
        conn.disconnect();

        String response = sb.toString();
        System.out.println("📄 응답 길이: " + response.length() + " characters");

        // 응답 분석
        if (response.contains("<resultCode>00</resultCode>")) {
            System.out.println("✅ API 호출 성공!");

            // 간단한 데이터 추출
            if (response.contains("<category>TMP</category>")) {
                System.out.println("🌡️ 온도 데이터 포함됨");
            }
            if (response.contains("<category>SKY</category>")) {
                System.out.println("☁️ 하늘상태 데이터 포함됨");
            }
            if (response.contains("<category>PTY</category>")) {
                System.out.println("🌧️ 강수형태 데이터 포함됨");
            }

        } else if (response.contains("<resultCode>")) {
            System.out.println("API 오류 응답");
            // 에러 코드 추출
            String errorCode = extractBetween(response, "<resultCode>", "</resultCode>");
            String errorMsg = extractBetween(response, "<resultMsg>", "</resultMsg>");
            System.out.println("오류 코드: " + errorCode);
            System.out.println("오류 메시지: " + errorMsg);
        } else {
            System.out.println("예상치 못한 응답 형식");
        }

        // 처음 500자만 출력
        System.out.println("🔍 응답 미리보기:");
        System.out.println(response.length() > 500 ? response.substring(0, 500) + "..." : response);
    }

    private static String extractBetween(String text, String start, String end) {
        int startIndex = text.indexOf(start);
        if (startIndex == -1) return "N/A";
        startIndex += start.length();

        int endIndex = text.indexOf(end, startIndex);
        if (endIndex == -1) return "N/A";

        return text.substring(startIndex, endIndex);
    }
}
