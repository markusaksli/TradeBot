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
import si.mazi.rescu.ClientConfig;


public class Account {
    public static void main(String[] args) throws IOException, InterruptedException {
        //Trying to get info with xchange api
        Exchange bitmex = ExchangeFactory.INSTANCE.createExchange(BitmexExchange.class.getName());

        ExchangeSpecification bitmexSpec = bitmex.getExchangeSpecification();
        bitmexSpec.setSslUri("https://testnet.bitmex.com");
        bitmexSpec.setApiKey("ZoKlEc3zTsR0L_KLbFQCthKc");
        bitmexSpec.setSecretKey("JCC_KV8_U4T8ZCyEZIceRwzTKTosMj02jcsQqYPGXUB9BkgU");
        bitmex.applySpecification(bitmexSpec);

        AccountService accountService = bitmex.getAccountService();
        System.out.println(accountService.getAccountInfo().toString());

        //Trying to get info with bitmex api and requests.
        //https://testnet.bitmex.com/api/v1

    }
}
