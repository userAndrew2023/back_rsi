package ru.rsiscan.rsiscranner.service;


import java.util.List;
import java.util.Map;

public interface RsiService {

    List<Map<String, Object>> getAll();

    Map<String, Object> getBySymbol(String symbol) throws Exception;
}
