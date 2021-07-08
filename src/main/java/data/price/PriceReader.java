package data.price;

import java.io.*;
import java.nio.file.Path;

public class PriceReader implements Closeable {

    private final DataInputStream stream;
    private Path path;
    private String coin;
    private String fiat;
    private long startTime;
    private long endTime;

    public String getCoin() {
        return coin;
    }

    public String getFiat() {
        return fiat;
    }

    public long getStartTime() {
        return startTime;
    }

    public long getEndTime() {
        return endTime;
    }

    public String getPair() {
        return coin + fiat;
    }

    public Path getPath() {
        return path;
    }

    public PriceReader(String file) throws IOException {
        this.stream = new DataInputStream(new BufferedInputStream(new FileInputStream(file)));
        this.path = Path.of(file);
        this.coin = stream.readUTF();
        this.fiat = stream.readUTF();
        this.startTime = stream.readLong();
        this.endTime = stream.readLong();
    }

    public PriceBean readPrice() throws IOException {
        return new PriceBean(stream.readLong(), stream.readDouble(), stream.readBoolean());
    }

    @Override
    public void close() throws IOException {
        stream.close();
    }
}
