package com.yeoun.order.support;

import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Component;

@Component
@RequiredArgsConstructor
public class ColorCodeGenerator {

    // =======================================================
    // 컬러코드 자동 생성
    public String generate(String workId) {
        int hash = workId.hashCode();

        int h = Math.floorMod(hash, 360);
        int s = 30 + Math.floorMod(hash >>> 3, 25);
        int l = 65 + Math.floorMod(hash >>> 5, 15);

        double sat = s / 100.0;
        double lig = l / 100.0;

        double c = (1 - Math.abs(2 * lig - 1)) * sat;
        double x = c * (1 - Math.abs((h / 60.0) % 2 - 1));
        double m = lig - c / 2.0;

        double r = 0, g = 0, b = 0;

        if (h < 60) { r = c; g = x; }
        else if (h < 120) { r = x; g = c; }
        else if (h < 180) { g = c; b = x; }
        else if (h < 240) { g = x; b = c; }
        else if (h < 300) { r = x; b = c; }
        else { r = c; b = x; }

        int R = (int) Math.round((r + m) * 255);
        int G = (int) Math.round((g + m) * 255);
        int B = (int) Math.round((b + m) * 255);

        return String.format("#%02X%02X%02X", R, G, B);
    }
}
