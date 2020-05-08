package Modes;

import trading.Account;

import java.util.Scanner;

public final class Live {
    private static Account account;

    public Live() {
        init();
    }

    public static Account getAccount() {
        return account;
    }


    private static void init() {
        Scanner sc = new Scanner(System.in);
        String apiKey;
        String apiSecret;
        while (true) {
            System.out.println("Enter your API Key: ");
            apiKey = sc.nextLine();
            if (apiKey.length() == 64) {
                System.out.println("Enter your Secret Key: ");
                apiSecret = sc.nextLine();
                if (apiSecret.length() == 64) {
                    break;
                } else System.out.println("Secret API is incorrect, enter again.");
            } else System.out.println("Incorrect API, enter again.");
        }
        account = new Account(apiKey, apiSecret);
        System.out.println(account.getMakerComission() + " Maker commission.");
        System.out.println(account.getBuyerComission() + " Buyer commission");
        System.out.println(account.getTakerComission() + " Taker comission");
    }
}
