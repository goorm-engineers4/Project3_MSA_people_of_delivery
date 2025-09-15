package com.example.cloudfour.aiservice.constants;

import java.util.HashMap;
import java.util.Map;

public class WeatherConstants {
    public static final Map<Integer, String> WEATHER_DESC_KO_MAP = new HashMap<>();
    
    static {
        WEATHER_DESC_KO_MAP.put(200, "비를 동반한 천둥구름");
        WEATHER_DESC_KO_MAP.put(201, "가벼운 비를 동반한 천둥구름");
        WEATHER_DESC_KO_MAP.put(202, "폭우를 동반한 천둥구름");
        WEATHER_DESC_KO_MAP.put(210, "약한 천둥구름");
        WEATHER_DESC_KO_MAP.put(211, "천둥구름");
        WEATHER_DESC_KO_MAP.put(212, "강한 천둥구름");
        WEATHER_DESC_KO_MAP.put(221, "불규칙적 천둥구름");
        WEATHER_DESC_KO_MAP.put(230, "약한 연무를 동반한 천둥구름");
        WEATHER_DESC_KO_MAP.put(231, "연무를 동반한 천둥구름");
        WEATHER_DESC_KO_MAP.put(232, "강한 안개비를 동반한 천둥구름");
        
        WEATHER_DESC_KO_MAP.put(300, "가벼운 안개비");
        WEATHER_DESC_KO_MAP.put(301, "안개비");
        WEATHER_DESC_KO_MAP.put(302, "강한 안개비");
        WEATHER_DESC_KO_MAP.put(310, "가벼운 적은비");
        WEATHER_DESC_KO_MAP.put(311, "적은비");
        WEATHER_DESC_KO_MAP.put(312, "강한 적은비");
        WEATHER_DESC_KO_MAP.put(313, "소나기와 안개비");
        WEATHER_DESC_KO_MAP.put(314, "강한 소나기와 안개비");
        WEATHER_DESC_KO_MAP.put(321, "소나기");
        
        WEATHER_DESC_KO_MAP.put(500, "약한 비");
        WEATHER_DESC_KO_MAP.put(501, "중간 비");
        WEATHER_DESC_KO_MAP.put(502, "강한 비");
        WEATHER_DESC_KO_MAP.put(503, "매우 강한 비");
        WEATHER_DESC_KO_MAP.put(504, "극심한 비");
        WEATHER_DESC_KO_MAP.put(511, "우박");
        WEATHER_DESC_KO_MAP.put(520, "약한 소나기 비");
        WEATHER_DESC_KO_MAP.put(521, "소나기 비");
        WEATHER_DESC_KO_MAP.put(522, "강한 소나기 비");
        WEATHER_DESC_KO_MAP.put(531, "불규칙적 소나기 비");
        
        WEATHER_DESC_KO_MAP.put(600, "가벼운 눈");
        WEATHER_DESC_KO_MAP.put(601, "눈");
        WEATHER_DESC_KO_MAP.put(602, "강한 눈");
        WEATHER_DESC_KO_MAP.put(611, "진눈깨비");
        WEATHER_DESC_KO_MAP.put(612, "소나기 진눈깨비");
        WEATHER_DESC_KO_MAP.put(615, "약한 비와 눈");
        WEATHER_DESC_KO_MAP.put(616, "비와 눈");
        WEATHER_DESC_KO_MAP.put(620, "약한 소나기 눈");
        WEATHER_DESC_KO_MAP.put(621, "소나기 눈");
        WEATHER_DESC_KO_MAP.put(622, "강한 소나기 눈");
        
        WEATHER_DESC_KO_MAP.put(701, "박무");
        WEATHER_DESC_KO_MAP.put(711, "연기");
        WEATHER_DESC_KO_MAP.put(721, "연무");
        WEATHER_DESC_KO_MAP.put(731, "모래 먼지");
        WEATHER_DESC_KO_MAP.put(741, "안개");
        WEATHER_DESC_KO_MAP.put(751, "모래");
        WEATHER_DESC_KO_MAP.put(761, "먼지");
        WEATHER_DESC_KO_MAP.put(762, "화산재");
        WEATHER_DESC_KO_MAP.put(771, "돌풍");
        WEATHER_DESC_KO_MAP.put(781, "토네이도");
        
        WEATHER_DESC_KO_MAP.put(800, "구름 한 점 없는 맑은 하늘");
        WEATHER_DESC_KO_MAP.put(801, "약간의 구름이 낀 하늘");
        WEATHER_DESC_KO_MAP.put(802, "드문드문 구름이 낀 하늘");
        WEATHER_DESC_KO_MAP.put(803, "구름이 거의 없는 하늘");
        WEATHER_DESC_KO_MAP.put(804, "구름으로 뒤덮인 흐린 하늘");
        
        WEATHER_DESC_KO_MAP.put(900, "토네이도");
        WEATHER_DESC_KO_MAP.put(901, "태풍");
        WEATHER_DESC_KO_MAP.put(902, "허리케인");
        WEATHER_DESC_KO_MAP.put(903, "한랭");
        WEATHER_DESC_KO_MAP.put(904, "고온");
        WEATHER_DESC_KO_MAP.put(905, "바람부는");
        WEATHER_DESC_KO_MAP.put(906, "우박");
        WEATHER_DESC_KO_MAP.put(951, "바람이 거의 없는");
        WEATHER_DESC_KO_MAP.put(952, "약한 바람");
        WEATHER_DESC_KO_MAP.put(953, "부드러운 바람");
        WEATHER_DESC_KO_MAP.put(954, "중간 세기 바람");
        WEATHER_DESC_KO_MAP.put(955, "신선한 바람");
        WEATHER_DESC_KO_MAP.put(956, "센 바람");
        WEATHER_DESC_KO_MAP.put(957, "돌풍에 가까운 센 바람");
        WEATHER_DESC_KO_MAP.put(958, "돌풍");
        WEATHER_DESC_KO_MAP.put(959, "심각한 돌풍");
        WEATHER_DESC_KO_MAP.put(960, "폭풍");
        WEATHER_DESC_KO_MAP.put(961, "강한 폭풍");
        WEATHER_DESC_KO_MAP.put(962, "허리케인");
    }
    
    public static String getWeatherDescKo(Integer code) {
        return code != null && WEATHER_DESC_KO_MAP.containsKey(code) 
            ? WEATHER_DESC_KO_MAP.get(code) 
            : "알 수 없음";
    }

    private WeatherConstants() {
        throw new AssertionError("Utility class should not be instantiated");
    }
}