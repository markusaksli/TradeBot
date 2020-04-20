package experimental;

import collection.PriceBean;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Paths;
import java.util.List;

public class CompareData {
    public static void main(String[] args) throws IOException {
        List<String> live = Files.readAllLines(Paths.get("agg_log.txt"));
        List<String> collected = Files.readAllLines(Paths.get("backtest_log.txt"));

        for (int i = 0; i < collected.size(); i++) {
            if (!collected.get(i).equals(live.get(i))) {
                System.out.println(collected.get(i) + "   " + live.get(i));
            }
        }


        int offset = 0;
        for (int i = 0; i < collected.size(); i++) {
            PriceBean liveBean = PriceBean.of(live.get(i));
            PriceBean collectedBean = PriceBean.of(collected.get(i + offset));
            if (liveBean.getPrice() != collectedBean.getPrice()) {
                System.out.println(i + "   " + liveBean.getPrice() + "   " + collectedBean.getPrice());
                offset++;
            }
        }
    }
}
