package com.example.cloudfour.userservice.domain.region.util;

import java.util.List;
import java.util.regex.Pattern;

public final class RegionParser {

    private static final List<String> SI_SUFFIX =
            List.of("특별시","광역시","특별자치시","특별자치도","시","도");
    private static final List<String> GU_SUFFIX = List.of("구","군","시");
    private static final List<String> DONG_SUFFIX = List.of("동","읍","면","리");

    private static final Pattern APT_BLOCK = Pattern.compile("^\\d+(동|호)$");

    private RegionParser() {}

    public record RegionParts(String si, String gu, String dong) {}

    public static RegionParts parseOrThrow(String fullAddress) {
        if (fullAddress == null || fullAddress.isBlank()) {
            throw new RegionParseException("주소가 비어있습니다.");
        }

        String noParen = fullAddress.replaceAll("\\([^)]*\\)", " ");
        String normalized = noParen.replaceAll("[,·]", " ")
                .trim()
                .replaceAll("\\s+", " ");
        String[] tokens = normalized.split(" ");

        String si = null, gu = null, dong = null;

        for (String raw : tokens) {
            String token = raw.trim();
            if (token.isEmpty()) continue;
            if (APT_BLOCK.matcher(token).matches()) continue;

            boolean isRoadName = token.endsWith("로") || token.endsWith("길");

            if (si == null && endsWithAny(token, SI_SUFFIX)) {
                si = token;
                continue;
            }

            if (endsWithAny(token, GU_SUFFIX)) {
                if (!token.equals(si)) {
                    gu = token;
                }
                continue;
            }

            if (!isRoadName && dong == null && endsWithAny(token, DONG_SUFFIX)) {
                if (token.matches(".*[가-힣].*")) {
                    dong = token;
                    break;
                }
            }
        }

        if (si != null && gu == null && si.contains("세종")) {
            gu = "세종시";
        }

        if (si == null || gu == null || dong == null) {
            throw new RegionParseException("시/구/동 식별 실패: " + normalized);
        }
        return new RegionParts(si, gu, dong);
    }

    private static boolean endsWithAny(String token, List<String> suffixes) {
        for (String s : suffixes) if (token.endsWith(s)) return true;
        return false;
    }

    public static class RegionParseException extends RuntimeException {
        public RegionParseException(String msg) { super(msg); }
    }
}
