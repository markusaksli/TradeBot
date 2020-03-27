import com.fasterxml.jackson.databind.DeserializationFeature;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.knowm.xchange.ExchangeFactory;
import org.knowm.xchange.ExchangeSpecification;
import org.knowm.xchange.bitmex.BitmexExchange;
import org.knowm.xchange.currency.Currency;
import org.knowm.xchange.dto.account.AccountInfo;
import org.knowm.xchange.service.account.AccountService;
import java.io.IOException;
import java.io.InputStream;
import java.math.BigDecimal;
import java.net.URI;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.util.Base64;
import com.fasterxml.jackson.databind.DeserializationFeature;
import com.fasterxml.jackson.databind.ObjectMapper;
import java.io.IOException;
import java.io.InputStream;
import java.net.URI;
import java.net.http.HttpResponse;
import org.knowm.xchange.Exchange;
import org.knowm.xchange.bitmex.dto.account.BitmexAccount;


public class Account {
    public static void main(String[] args) throws IOException, InterruptedException {
        //Trying to get info with xchange api
        /*Exchange bitmex = ExchangeFactory.INSTANCE.createExchange(BitmexExchange.class.getName());

        ExchangeSpecification bitmexSpec = bitmex.getExchangeSpecification();
        bitmexSpec.setHost("testnet.bitmex.com");
        bitmexSpec.setSslUri("https://testnet.bitmex.com");
        bitmexSpec.setApiKey("ZoKlEc3zTsR0L_KLbFQCthKc");
        bitmexSpec.setSecretKey("JCC_KV8_U4T8ZCyEZIceRwzTKTosMj02jcsQqYPGXUB9BkgU");
        bitmex.applySpecification(bitmexSpec);

        AccountService accountService = bitmex.getAccountService();
        System.out.println(accountService.getAccountInfo().getWallet().getBalance(Currency.XBT));*/

        //Trying to get info with bitmex api and requests.
        //https://testnet.bitmex.com/api/v1
        ObjectMapper mapper = new ObjectMapper();

        mapper.configure(DeserializationFeature.FAIL_ON_UNKNOWN_PROPERTIES, false);

        var client = HttpClient.newHttpClient();
        String API_KEY= "ZoKlEc3zTsR0L_KLbFQCthKc";
        var request = HttpRequest.newBuilder()
                .uri(URI.create("https://testnet.bitmex.com/api/v1/user/wallet"))
                .header("User-Agent", "Mozilla/5.0")
                .header("x-ratelimit-limit", "60")
                .header("x-ratelimit-remaining", "58")
                .header("x-ratelimit-reset", "1489791662")
                .header("Authorization", new String(API_KEY.getBytes()))
                .GET()
                .build();



        HttpResponse<InputStream> response = client.send(request, HttpResponse.BodyHandlers.ofInputStream());
        Wallet_Data walletData = mapper.readValue(response.body(), Wallet_Data.class);
        System.out.println(walletData);

    }

    /**
     * Is supposed to get info on account.
     * @throws IOException
     */
    public static void AccountInfo() throws IOException {
        InputStream is =
                Account.class.getResourceAsStream(
                        "/org/knowm/xchange/bitmex/dto/account/");

        ObjectMapper mapper = new ObjectMapper();
        BitmexAccount bitmexAccount = mapper.readValue(is, BitmexAccount.class);
        System.out.println(bitmexAccount);


    }
}
