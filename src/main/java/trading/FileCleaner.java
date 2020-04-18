package trading;

import java.io.*;
import java.nio.charset.StandardCharsets;

/**
 * Purpose of this class is to create files with the lines that
 * only have the "sell" keyword inside.
 * The less data useless data we have, the better.
 * It has been used to create the .txt files in Backtesting and will remain with the same functionality
 */
public class FileCleaner {
    public static void main(String[] args) {
        //fileCleaner("/LINK_august.csv", "LINK_aug.txt");
    }

    private static void fileCleaner(String fileFrom, String fileInto) {
        InputStream inputStream = FileCleaner.class.getResourceAsStream(fileFrom);
        InputStreamReader streamReader = new InputStreamReader(inputStream, StandardCharsets.UTF_8);
        try (FileWriter fileWriter = new FileWriter("./src/main/resources/Backtesting/" + fileInto);
             BufferedReader reader = new BufferedReader(streamReader)) {
            fileWriter.append("unix,date,symbol,price,amount,dollar_amount,type,trans_id\n");
            for (String line; (line = reader.readLine()) != null; ) {
                if (line.contains("sell")) {
                    fileWriter.append(line + "\n");
                }
            }
            fileWriter.flush();

        } catch (IOException e) {
            e.printStackTrace();
        }
    }
}
