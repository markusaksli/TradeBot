package trading;

import ch.qos.logback.classic.sift.AppenderFactoryUsingJoran;
import collection.PriceBean;

import java.io.File;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.text.DecimalFormat;
import java.text.NumberFormat;
import java.text.SimpleDateFormat;
import java.time.Duration;
import java.time.LocalDateTime;
import java.time.ZoneOffset;
import java.time.format.DateTimeFormatter;
import java.time.temporal.TemporalAccessor;
import java.util.ArrayList;
import java.util.Date;
import java.util.List;
import java.util.stream.Collectors;
import java.util.stream.IntStream;

public class Formatter {
    private static final SimpleDateFormat SIMPLE_FORMATTER = new SimpleDateFormat("yyyy/MM/dd HH:mm:ss");
    private static final SimpleDateFormat ONLY_DATE_FORMATTER = new SimpleDateFormat("yyyy.MM.dd");
    private static final DateTimeFormatter DATE_TIME_FORMATTER = DateTimeFormatter.ofPattern("yyyy/MM/dd HH:mm:ss");
    private static final NumberFormat PERCENT_FORMAT = new DecimalFormat("0.000%");

    public static String formatDate(LocalDateTime date) {
        return DATE_TIME_FORMATTER.format(date);
    }

    public static String formatDate(long timestamp) {
        return SIMPLE_FORMATTER.format(new Date(timestamp));
    }

    public static String formatOnlyDate(long timestamp) {
        return ONLY_DATE_FORMATTER.format(new Date(timestamp));
    }

    public static String formatPercent(double percentage) {
        return PERCENT_FORMAT.format(percentage);
    }

    public static String formatDecimal(double decimal) {
        int zeroes = 0;
        String s = String.format("%.12f", decimal).replaceAll("[,.]", "");
        for (char c : s.toCharArray()) {
            if (c == '0') {
                zeroes++;
            } else if (c != '-') {
                break;
            }
        }
        NumberFormat decimalFormat = new DecimalFormat("0." + "0".repeat(3 + zeroes));
        return decimalFormat.format(decimal);
    }

    public static String formatDuration(Duration duration) {
        long seconds = duration.getSeconds();
        long absSeconds = Math.abs(seconds);
        String positive = String.format(
                "%d:%02d:%02d",
                absSeconds / 3600,
                (absSeconds % 3600) / 60,
                absSeconds % 60);
        return seconds < 0 ? "-" + positive : positive;
    }

    public static List<PriceBean> formatData(String path) throws IOException {
        List<String> lines = Files.readAllLines(Path.of(path));
        return IntStream.range(1, lines.size()).mapToObj(lines::get).map(PriceBean::of).collect(Collectors.toList());
    }

    public static List<Double> formatChart(List<PriceBean> beans) {
        return beans.stream().filter(PriceBean::isClose).map(PriceBean::getPrice).collect(Collectors.toList());
    }
}
