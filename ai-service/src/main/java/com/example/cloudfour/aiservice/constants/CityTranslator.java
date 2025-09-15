package com.example.cloudfour.aiservice.constants;

import java.util.HashMap;
import java.util.Map;

public class CityTranslator {
    
    private static final Map<String, String> KOREAN_TO_ENGLISH_CITY_MAP = new HashMap<>();
    
    static {
        KOREAN_TO_ENGLISH_CITY_MAP.put("서울", "seoul");
        KOREAN_TO_ENGLISH_CITY_MAP.put("부산", "busan");
        KOREAN_TO_ENGLISH_CITY_MAP.put("대구", "daegu");
        KOREAN_TO_ENGLISH_CITY_MAP.put("인천", "incheon");
        KOREAN_TO_ENGLISH_CITY_MAP.put("광주", "gwangju");
        KOREAN_TO_ENGLISH_CITY_MAP.put("대전", "daejeon");
        KOREAN_TO_ENGLISH_CITY_MAP.put("울산", "ulsan");
        KOREAN_TO_ENGLISH_CITY_MAP.put("세종", "sejong");

        KOREAN_TO_ENGLISH_CITY_MAP.put("수원", "suwon");
        KOREAN_TO_ENGLISH_CITY_MAP.put("성남", "seongnam");
        KOREAN_TO_ENGLISH_CITY_MAP.put("고양", "goyang");
        KOREAN_TO_ENGLISH_CITY_MAP.put("용인", "yongin");
        KOREAN_TO_ENGLISH_CITY_MAP.put("부천", "bucheon");
        KOREAN_TO_ENGLISH_CITY_MAP.put("안산", "ansan");
        KOREAN_TO_ENGLISH_CITY_MAP.put("안양", "anyang");
        KOREAN_TO_ENGLISH_CITY_MAP.put("남양주", "namyangju");
        KOREAN_TO_ENGLISH_CITY_MAP.put("화성", "hwaseong");
        KOREAN_TO_ENGLISH_CITY_MAP.put("평택", "pyeongtaek");
        KOREAN_TO_ENGLISH_CITY_MAP.put("의정부", "uijeongbu");
        KOREAN_TO_ENGLISH_CITY_MAP.put("시흥", "siheung");
        KOREAN_TO_ENGLISH_CITY_MAP.put("파주", "paju");
        KOREAN_TO_ENGLISH_CITY_MAP.put("광명", "gwangmyeong");
        KOREAN_TO_ENGLISH_CITY_MAP.put("김포", "gimpo");
        KOREAN_TO_ENGLISH_CITY_MAP.put("군포", "gunpo");
        KOREAN_TO_ENGLISH_CITY_MAP.put("하남", "hanam");
        KOREAN_TO_ENGLISH_CITY_MAP.put("오산", "osan");
        KOREAN_TO_ENGLISH_CITY_MAP.put("이천", "icheon");
        KOREAN_TO_ENGLISH_CITY_MAP.put("양주", "yangju");
        KOREAN_TO_ENGLISH_CITY_MAP.put("구리", "guri");
        KOREAN_TO_ENGLISH_CITY_MAP.put("안성", "anseong");
        KOREAN_TO_ENGLISH_CITY_MAP.put("포천", "pocheon");
        KOREAN_TO_ENGLISH_CITY_MAP.put("의왕", "uiwang");
        KOREAN_TO_ENGLISH_CITY_MAP.put("양평", "yangpyeong");
        KOREAN_TO_ENGLISH_CITY_MAP.put("동두천", "dongducheon");
        KOREAN_TO_ENGLISH_CITY_MAP.put("과천", "gwacheon");
        KOREAN_TO_ENGLISH_CITY_MAP.put("가평", "gapyeong");
        KOREAN_TO_ENGLISH_CITY_MAP.put("연천", "yeoncheon");

        KOREAN_TO_ENGLISH_CITY_MAP.put("춘천", "chuncheon");
        KOREAN_TO_ENGLISH_CITY_MAP.put("원주", "wonju");
        KOREAN_TO_ENGLISH_CITY_MAP.put("강릉", "gangneung");
        KOREAN_TO_ENGLISH_CITY_MAP.put("동해", "donghae");
        KOREAN_TO_ENGLISH_CITY_MAP.put("태백", "taebaek");
        KOREAN_TO_ENGLISH_CITY_MAP.put("속초", "sokcho");
        KOREAN_TO_ENGLISH_CITY_MAP.put("삼척", "samcheok");

        KOREAN_TO_ENGLISH_CITY_MAP.put("청주", "cheongju");
        KOREAN_TO_ENGLISH_CITY_MAP.put("충주", "chungju");
        KOREAN_TO_ENGLISH_CITY_MAP.put("제천", "jecheon");

        KOREAN_TO_ENGLISH_CITY_MAP.put("천안", "cheonan");
        KOREAN_TO_ENGLISH_CITY_MAP.put("공주", "gongju");
        KOREAN_TO_ENGLISH_CITY_MAP.put("보령", "boryeong");
        KOREAN_TO_ENGLISH_CITY_MAP.put("아산", "asan");
        KOREAN_TO_ENGLISH_CITY_MAP.put("서산", "seosan");
        KOREAN_TO_ENGLISH_CITY_MAP.put("논산", "nonsan");
        KOREAN_TO_ENGLISH_CITY_MAP.put("계룡", "gyeryong");
        KOREAN_TO_ENGLISH_CITY_MAP.put("당진", "dangjin");

        KOREAN_TO_ENGLISH_CITY_MAP.put("전주", "jeonju");
        KOREAN_TO_ENGLISH_CITY_MAP.put("군산", "gunsan");
        KOREAN_TO_ENGLISH_CITY_MAP.put("익산", "iksan");
        KOREAN_TO_ENGLISH_CITY_MAP.put("정읍", "jeongeup");
        KOREAN_TO_ENGLISH_CITY_MAP.put("남원", "namwon");
        KOREAN_TO_ENGLISH_CITY_MAP.put("김제", "gimje");

        KOREAN_TO_ENGLISH_CITY_MAP.put("목포", "mokpo");
        KOREAN_TO_ENGLISH_CITY_MAP.put("여수", "yeosu");
        KOREAN_TO_ENGLISH_CITY_MAP.put("순천", "suncheon");
        KOREAN_TO_ENGLISH_CITY_MAP.put("나주", "naju");
        KOREAN_TO_ENGLISH_CITY_MAP.put("광양", "gwangyang");

        KOREAN_TO_ENGLISH_CITY_MAP.put("포항", "pohang");
        KOREAN_TO_ENGLISH_CITY_MAP.put("경주", "gyeongju");
        KOREAN_TO_ENGLISH_CITY_MAP.put("김천", "gimcheon");
        KOREAN_TO_ENGLISH_CITY_MAP.put("안동", "andong");
        KOREAN_TO_ENGLISH_CITY_MAP.put("구미", "gumi");
        KOREAN_TO_ENGLISH_CITY_MAP.put("영주", "yeongju");
        KOREAN_TO_ENGLISH_CITY_MAP.put("영천", "yeongcheon");
        KOREAN_TO_ENGLISH_CITY_MAP.put("상주", "sangju");
        KOREAN_TO_ENGLISH_CITY_MAP.put("문경", "mungyeong");
        KOREAN_TO_ENGLISH_CITY_MAP.put("경산", "gyeongsan");

        KOREAN_TO_ENGLISH_CITY_MAP.put("창원", "changwon");
        KOREAN_TO_ENGLISH_CITY_MAP.put("진주", "jinju");
        KOREAN_TO_ENGLISH_CITY_MAP.put("통영", "tongyeong");
        KOREAN_TO_ENGLISH_CITY_MAP.put("사천", "sacheon");
        KOREAN_TO_ENGLISH_CITY_MAP.put("김해", "gimhae");
        KOREAN_TO_ENGLISH_CITY_MAP.put("밀양", "miryang");
        KOREAN_TO_ENGLISH_CITY_MAP.put("거제", "geoje");
        KOREAN_TO_ENGLISH_CITY_MAP.put("양산", "yangsan");

        KOREAN_TO_ENGLISH_CITY_MAP.put("제주", "jeju");
        KOREAN_TO_ENGLISH_CITY_MAP.put("서귀포", "seogwipo");

        KOREAN_TO_ENGLISH_CITY_MAP.put("도쿄", "tokyo");
        KOREAN_TO_ENGLISH_CITY_MAP.put("오사카", "osaka");
        KOREAN_TO_ENGLISH_CITY_MAP.put("교토", "kyoto");
        KOREAN_TO_ENGLISH_CITY_MAP.put("베이징", "beijing");
        KOREAN_TO_ENGLISH_CITY_MAP.put("상하이", "shanghai");
        KOREAN_TO_ENGLISH_CITY_MAP.put("뉴욕", "new york");
        KOREAN_TO_ENGLISH_CITY_MAP.put("런던", "london");
        KOREAN_TO_ENGLISH_CITY_MAP.put("파리", "paris");
        KOREAN_TO_ENGLISH_CITY_MAP.put("로마", "rome");
        KOREAN_TO_ENGLISH_CITY_MAP.put("시드니", "sydney");
        KOREAN_TO_ENGLISH_CITY_MAP.put("방콕", "bangkok");
        KOREAN_TO_ENGLISH_CITY_MAP.put("싱가포르", "singapore");
        KOREAN_TO_ENGLISH_CITY_MAP.put("홍콩", "hong kong");
        KOREAN_TO_ENGLISH_CITY_MAP.put("타이페이", "taipei");
    }

    public static String translateToEnglish(String koreanCity) {
        if (koreanCity == null || koreanCity.trim().isEmpty()) {
            return koreanCity;
        }
        
        String trimmedCity = koreanCity.trim().toLowerCase();

        if (containsKorean(trimmedCity)) {
            String englishCity = KOREAN_TO_ENGLISH_CITY_MAP.get(trimmedCity);
            if (englishCity != null) {
                return englishCity;
            }
        }

        return trimmedCity;
    }

    private static boolean containsKorean(String text) {
        if (text == null) {
            return false;
        }
        
        for (char c : text.toCharArray()) {
            if (Character.UnicodeBlock.of(c) == Character.UnicodeBlock.HANGUL_SYLLABLES ||
                Character.UnicodeBlock.of(c) == Character.UnicodeBlock.HANGUL_JAMO ||
                Character.UnicodeBlock.of(c) == Character.UnicodeBlock.HANGUL_COMPATIBILITY_JAMO) {
                return true;
            }
        }
        return false;
    }

    public static boolean isSupportedCity(String city) {
        if (city == null || city.trim().isEmpty()) {
            return false;
        }
        
        String trimmedCity = city.trim().toLowerCase();

        if (KOREAN_TO_ENGLISH_CITY_MAP.containsKey(trimmedCity)) {
            return true;
        }

        return KOREAN_TO_ENGLISH_CITY_MAP.containsValue(trimmedCity);
    }
}
