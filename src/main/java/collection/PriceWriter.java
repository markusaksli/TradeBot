package collection;

import java.io.*;
import java.nio.file.Files;
import java.nio.file.Paths;
import java.util.List;

public class PriceWriter implements Closeable {

    private final DataOutputStream stream;

    public PriceWriter(String file) throws FileNotFoundException {
        this.stream = new DataOutputStream(new BufferedOutputStream(new FileOutputStream(file)));
    }

    public void writeBean(PriceBean bean) throws IOException {
        stream.writeLong(bean.getTimestamp());
        stream.writeDouble(bean.getPrice());
        stream.writeBoolean(bean.isClosing());
    }

    @Override
    public void close() throws IOException {
        stream.close();
    }

    public static void main(String[] args) {
        try (PriceWriter pw = new PriceWriter("backtesting\\BTCUSDT_2019.04.01-2019.07.01.dat")) {
            List<String> lines = Files.readAllLines(Paths.get("backtesting\\BTCUSDT_2019.04.01-2019.07.01.txt"));
            for (int i = 1; i < lines.size(); i++) {
                String[] arr = lines.get(i).split(";");
                pw.writeBean(new PriceBean(Long.parseLong(arr[0]), Double.parseDouble(arr[1]), arr[2].equals("1")));
            }
        } catch (IOException e) {
            e.printStackTrace();
        }
    }
}
