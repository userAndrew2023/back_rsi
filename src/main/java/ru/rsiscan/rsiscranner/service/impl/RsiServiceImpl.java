package ru.rsiscan.rsiscranner.service.impl;

import models.CandleStick;
import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;
import org.springframework.stereotype.Service;
import ru.rsiscan.rsiscranner.service.RsiService;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.net.URL;
import java.nio.charset.StandardCharsets;
import java.util.*;

@Service
public class RsiServiceImpl implements RsiService {

    private static final List<String> crypto = List.of(
            "BTC", "ADA", "LTC"
    );

    @Override
    public List<Map<String, Object>> getAll() {
        List<Map<String, Object>> mapList = new ArrayList<>();
        for (String symbol : crypto) {
            new Thread(() -> {
                try {
                    mapList.add(getBySymbol(symbol + "USDT"));
                } catch (Exception e) {
                    throw new RuntimeException(e);
                }
            }).start();
        }
        while (true) {
            if (mapList.size() == crypto.size()) {
                break;
            }
        }
        return mapList;
    }

    @Override
    public Map<String, Object> getBySymbol(String symbol) throws Exception {
        JSONArray[] arr = {null, null, null, null};
        new Thread(() -> {
            try {
                arr[0] = readJsonArrayFromUrl("https://api.binance.com/api/v3/klines?symbol=" + symbol + "&interval=15m");
            } catch (IOException | JSONException e) {
                throw new RuntimeException(e);
            }
        }).start();
        JSONArray m15 = arr[0];
        new Thread(() -> {
            try {
                arr[1] =  readJsonArrayFromUrl("https://api.binance.com/api/v3/klines?symbol=" + symbol + "&interval=1h");
            } catch (IOException | JSONException e) {
                throw new RuntimeException(e);
            }
        }).start();
        JSONArray h1 = arr[1];
        new Thread(() -> {
            try {
                arr[2] = readJsonArrayFromUrl("https://api.binance.com/api/v3/klines?symbol=" + symbol + "&interval=4h");
            } catch (IOException | JSONException e) {
                throw new RuntimeException(e);
            }
        }).start();
        JSONArray h4 = arr[2];
        new Thread(() -> {
            try {
                arr[3] = readJsonArrayFromUrl("https://api.binance.com/api/v3/klines?symbol=" + symbol + "&interval=1d");
            } catch (IOException | JSONException e) {
                throw new RuntimeException(e);
            }
        }).start();
        JSONArray d1 = arr[3];
        while (true) {
            System.out.println("Цикл идёт");
            if (arr[0] != null && arr[1] != null && arr[2] != null && arr[3] != null) {
                System.out.println("Цикл прошёл");
                m15 = arr[0];
                h1 = arr[1];
                h4 = arr[2];
                d1 = arr[3];
                break;
            }
        }
        JSONObject jsonObject = readJsonFromUrl("https://api.binance.com/api/v3/ticker?symbol=" + symbol);
        List<CandleStick> candleSticks15m = new ArrayList<>();
        for (int i = 0; i < m15.length(); i++) {
            JSONArray candleArray = (JSONArray) m15.get(i);
            candleSticks15m.add(new CandleStick(Float.parseFloat(candleArray.get(4).toString())));
        }
        List<CandleStick> candleSticks1h = new ArrayList<>();
        for (int i = 0; i < h1.length(); i++) {
            JSONArray candleArray = (JSONArray) h1.get(i);
            candleSticks1h.add(new CandleStick(Float.parseFloat(candleArray.get(4).toString())));
        }
        List<CandleStick> candleSticks4h = new ArrayList<>();
        for (int i = 0; i < h4.length(); i++) {
            JSONArray candleArray = (JSONArray) h4.get(i);
            candleSticks4h.add(new CandleStick(Float.parseFloat(candleArray.get(4).toString())));
        }
        List<CandleStick> candleSticks1d = new ArrayList<>();
        for (int i = 0; i < d1.length(); i++) {
            JSONArray candleArray = (JSONArray) d1.get(i);
            candleSticks1d.add(new CandleStick(Float.parseFloat(candleArray.get(4).toString())));
        }
        Map<String, Object> returnMap = new HashMap<>();
        returnMap.put("pair", symbol);
        returnMap.put("price", jsonObject.get("lastPrice").toString());
        returnMap.put("volume", jsonObject.get("volume").toString());

        Map<String, String> rsi = new HashMap<>();
        rsi.put("15m", calculate(candleSticks15m));
        rsi.put("1h", calculate(candleSticks1h));
        rsi.put("4h", calculate(candleSticks4h));
        rsi.put("1d", calculate(candleSticks1d));

        returnMap.put("rsi", rsi);
        return returnMap;
    }

    private static String readAll(BufferedReader rd) throws IOException {
        StringBuilder sb = new StringBuilder();
        int cp;
        while ((cp = rd.read()) != -1) {
            sb.append((char) cp);
        }
        return sb.toString();
    }

    public static JSONArray readJsonArrayFromUrl(String url) throws IOException, JSONException {
        try (InputStream is = new URL(url).openStream()) {
            BufferedReader rd = new BufferedReader(new InputStreamReader(is, StandardCharsets.UTF_8));
            String jsonText = readAll(rd);
            rd.close();
            is.close();
            return new JSONArray(jsonText);
        }
    }

    public static JSONObject readJsonFromUrl(String url) throws IOException, JSONException {
        try (InputStream is = new URL(url).openStream()) {
            BufferedReader rd = new BufferedReader(new InputStreamReader(is, StandardCharsets.UTF_8));
            String jsonText = readAll(rd);
            rd.close();
            is.close();
            return new JSONObject(jsonText);
        }
    }

    public static String calculate(List<CandleStick> data) throws Exception {
        int periodLength = 14;
        int lastBar = data.size() - 1;
        int firstBar = lastBar - periodLength + 1;
        if (firstBar < 0) {
            String msg = "Quote history length " + data.size() + " is insufficient to calculate the indicator.";
            throw new Exception(msg);
        }

        float aveGain = 0, aveLoss = 0;
        for (int bar = firstBar + 1; bar <= lastBar; bar++) {
            float change = data.get(bar).getClose() - data.get(bar - 1).getClose();
            if (change >= 0) {
                aveGain += change;
            } else {
                aveLoss += change;
            }
        }

        float rs = aveGain / Math.abs(aveLoss);
        float result = 100 - 100 / (1 + rs);
        return Float.toString(result);
    }
}
