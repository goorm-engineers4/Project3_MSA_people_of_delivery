package com.example.cloudfour.aiservice.client;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;
import org.springframework.web.reactive.function.client.WebClient;
import reactor.core.publisher.Mono;

import java.util.Map;
import lombok.Builder;

@Slf4j
@Component
@RequiredArgsConstructor
public class WeatherClient {
    
    private final WebClient webClient;
    
    @Value("${weather.api.key}")
    private String apiKey;
    
    @Value("${weather.api.url}")
    private String apiUrl;
    
    public Mono<WeatherData> getCurrentWeather(String city) {
        String url = apiUrl + "?q=" + city + "&appid=" + apiKey + "&units=metric&lang=kr";
        
        log.info("날씨 API 호출: {}", city);
        
        return webClient.get()
                .uri(url)
                .retrieve()
                .bodyToMono(Map.class)
                .map(this::parseWeatherData)
                .doOnSuccess(data -> log.info("날씨 API 응답 성공: {} - {}", city, data.getWeather()))
                .doOnError(error -> log.error("날씨 API 호출 실패: {} - {}", city, error.getMessage()));
    }
    
    @SuppressWarnings("unchecked")
    private WeatherData parseWeatherData(Map<String, Object> response) {
        try {
            Map<String, Object> main = (Map<String, Object>) response.get("main");
            Map<String, Object> weather = (Map<String, Object>) ((java.util.List<?>) response.get("weather")).get(0);
            Map<String, Object> wind = response.get("wind") != null ? (Map<String, Object>) response.get("wind") : null;
            Map<String, Object> rain = response.get("rain") != null ? (Map<String, Object>) response.get("rain") : null;
            Double temp = main.get("temp") != null ? Double.parseDouble(main.get("temp").toString()) : null;
            Double feelsLike = main.get("feels_like") != null ? Double.parseDouble(main.get("feels_like").toString()) : null;
            Integer humidity = main.get("humidity") != null ? Integer.parseInt(main.get("humidity").toString()) : null;
            Double windSpeed = wind != null && wind.get("speed") != null ? Double.parseDouble(wind.get("speed").toString()) : null;
            Double rain1h = rain != null && rain.get("1h") != null ? Double.parseDouble(rain.get("1h").toString()) : null;
            Integer code = weather.get("id") != null ? Integer.parseInt(weather.get("id").toString()) : null;
            return WeatherData.builder()
                    .temperature(temp != null ? String.valueOf(temp) : null)
                    .humidity(humidity != null ? String.valueOf(humidity) : null)
                    .weather((String) weather.get("description"))
                    .code(code)
                    .feelsLike(feelsLike)
                    .windSpeed(windSpeed)
                    .rain1h(rain1h)
                    .build();
        } catch (Exception e) {
            log.error("날씨 데이터 파싱 실패: {}", e.getMessage());
            return WeatherData.builder()
                    .weather("알 수 없음")
                    .temperature("0")
                    .humidity("0")
                    .code(null)
                    .feelsLike(null)
                    .windSpeed(null)
                    .rain1h(null)
                    .build();
        }
    }
    
    @Builder
    public static class WeatherData {
        private String weather;
        private String temperature;
        private String humidity;
        private Integer code;
        private Double feelsLike;
        private Double windSpeed;
        private Double rain1h;

        public WeatherData() {}

        public WeatherData(String weather, String temperature, String humidity, Integer code, 
                          Double feelsLike, Double windSpeed, Double rain1h) {
            this.weather = weather;
            this.temperature = temperature;
            this.humidity = humidity;
            this.code = code;
            this.feelsLike = feelsLike;
            this.windSpeed = windSpeed;
            this.rain1h = rain1h;
        }

        public String getWeather() {
            return weather;
        }

        public String getTemperature() {
            return temperature;
        }

        public String getHumidity() {
            return humidity;
        }

        public Integer getCode() {
            return code;
        }

        public Double getFeelsLike() {
            return feelsLike;
        }

        public Double getWindSpeed() {
            return windSpeed;
        }

        public Double getRain1h() {
            return rain1h;
        }

        public void setWeather(String weather) {
            this.weather = weather;
        }

        public void setTemperature(String temperature) {
            this.temperature = temperature;
        }

        public void setHumidity(String humidity) {
            this.humidity = humidity;
        }

        public void setCode(Integer code) {
            this.code = code;
        }

        public void setFeelsLike(Double feelsLike) {
            this.feelsLike = feelsLike;
        }

        public void setWindSpeed(Double windSpeed) {
            this.windSpeed = windSpeed;
        }

        public void setRain1h(Double rain1h) {
            this.rain1h = rain1h;
        }
    }
}
