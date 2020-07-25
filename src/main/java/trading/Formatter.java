package trading;

import java.text.DecimalFormat;
import java.text.NumberFormat;
import java.text.SimpleDateFormat;
import java.time.Duration;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.LocalTime;
import java.time.format.DateTimeFormatter;
import java.time.format.DateTimeParseException;
import java.util.Date;

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

    public static SimpleDateFormat getSimpleFormatter() {
        return SIMPLE_FORMATTER;
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

    public static boolean isValidDateFormat(String format, String value) {
        LocalDateTime ldt;
        DateTimeFormatter fomatter = DateTimeFormatter.ofPattern(format);

        try {
            ldt = LocalDateTime.parse(value, fomatter);
            String result = ldt.format(fomatter);
            return result.equals(value);
        } catch (DateTimeParseException e) {
            try {
                LocalDate ld = LocalDate.parse(value, fomatter);
                String result = ld.format(fomatter);
                return result.equals(value);
            } catch (DateTimeParseException exp) {
                try {
                    LocalTime lt = LocalTime.parse(value, fomatter);
                    String result = lt.format(fomatter);
                    return result.equals(value);
                } catch (DateTimeParseException e2) {
                    // Debugging purposes
                    //e2.printStackTrace();
                }
            }
        }

        return false;
    }
}
