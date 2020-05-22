package collection;

import trading.Formatter;

import java.io.*;

public class PriceReader implements Closeable {

    private final DataInputStream stream;

    public PriceReader(String file) throws FileNotFoundException {
        this.stream = new DataInputStream(new BufferedInputStream(new FileInputStream(file)));
    }

    public PriceBean readPrice() {
        try {
            return new PriceBean(stream.readLong(), stream.readDouble(), stream.readBoolean());
        } catch (IOException e) {
            return null;
        }
    }

    @Override
    public void close() throws IOException {
        stream.close();
    }

    public static void main(String[] args) {
        try (PriceReader pr = new PriceReader("backtesting\\BTCUSDT_2020.01.01-2020.04.01.dat")) {
            PriceBean bean = pr.readPrice();
            while (bean != null) {
                if (bean.isClosing()) System.out.println(Formatter.formatDate(bean.getTimestamp()));
                bean = pr.readPrice();
            }
        } catch (IOException e) {
            e.printStackTrace();
        }
    }
}
