package ru.rsiscan.rsiscranner.controller;

import lombok.AllArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;
import ru.rsiscan.rsiscranner.service.RsiService;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;

@RestController
@RequestMapping("/rsi")
@AllArgsConstructor
public class RsiRestController {

    private final RsiService rsiService;

    @GetMapping("/")
    public ResponseEntity<List<Map<String, Object>>> getAll() {
        return new ResponseEntity<>(rsiService.getAll(), HttpStatus.OK);
    }

    @GetMapping("/{symbol}")
    public ResponseEntity<Map<String, Object>> getBySymbol(@PathVariable String symbol) throws Exception {
        return new ResponseEntity<>(rsiService.getBySymbol(symbol), HttpStatus.OK);
    }
}
