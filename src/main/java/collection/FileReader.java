package collection;

import java.io.File;
import java.io.FileNotFoundException;
import java.util.ArrayList;
import java.util.List;
import java.util.Scanner;

public class FileReader {

    public List<Double> readPrices (/*String fileName */) throws FileNotFoundException {
        List<Double> prices = new ArrayList<>();
        File file = new File("BTCUSDT_January2020.txt");
        Scanner sc = new Scanner(file);
        while (sc.hasNextLine()){
            String line = sc.nextLine();
            String[] linearray = line.strip().split(",");
            if (linearray[6].equals("sell")) {
                prices.add(Double.parseDouble(linearray[3]));
            }
        }
        return prices;
    }

}
