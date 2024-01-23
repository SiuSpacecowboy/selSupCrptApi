package cprtApi;

import com.fasterxml.jackson.databind.ObjectMapper;
import lombok.Getter;
import lombok.Setter;
import lombok.ToString;
import org.apache.http.HttpEntity;
import org.apache.http.client.methods.HttpPost;
import org.apache.http.entity.ContentType;
import org.apache.http.entity.StringEntity;
import org.apache.http.impl.client.CloseableHttpClient;
import org.apache.http.impl.client.HttpClients;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.context.ConfigurableApplicationContext;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Controller;
import org.springframework.web.bind.annotation.*;

import java.io.File;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Paths;
import java.time.Duration;
import java.time.Instant;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.locks.Lock;
import java.util.concurrent.locks.ReentrantLock;

@Controller
@RequestMapping(value = "api/v2/crptApi")
@SpringBootApplication
public class CrptApiV2 {

    private static final String URI_FOR_REQUEST = "http://localhost:8080/api/v2/crptApi";
    private final long period;
    private final int reqCount;
    private int innerCount;
    private static Instant time;
    private static long inc;
    private static final Lock lock = new ReentrantLock();
    private static final Map<String, Doc> rep = new HashMap<>();

    @Autowired
    public CrptApiV2(TimeUnit timeUnit, int reqCount) {
        this.period = timeUnit.toMillis(1);
        this.reqCount = reqCount;
    }

    @PostMapping
    @ResponseStatus(HttpStatus.OK)
    public void syncThr(@RequestBody Doc doc, @RequestParam(name = "signature") String signature) {
        lock.lock(); // Для синхронизации использую Lock.
        try {
            innerCount++;
            Instant thrEntTime = Instant.now();
            if (innerCount == 1) { // Установка начала интервала, основываясь на самом первом запросе.
                time = thrEntTime;
            } else if (Duration.between(time, thrEntTime).toMillis() >= period) { // Проверка на истечение времени интервала и обнуление счетчика при true.
                innerCount = 1;
                time = thrEntTime;
            } else if (innerCount > reqCount) { // Проверка на превышение лимита запросов, при true, засыпает до истечения текущего временного интервала.
                innerCount = 1;
                long diff = period - Duration.between(time, thrEntTime).toMillis();
                if (diff > 0) {
                    System.out.println("Выполнение остановлено до следующего доступного интервала, оставшееся время: "
                            + TimeUnit.SECONDS.convert(diff, TimeUnit.MILLISECONDS) + " секунд.");
                    Thread.sleep(diff);
                }
                time = Instant.now(); // Установка нового времени начала нового интервала.
            }
            createDoc(doc, signature); // Выполнение логики.
        } catch (InterruptedException | IOException e) {
            throw new RuntimeException(e);
        } finally {
            lock.unlock();
        }
    }

    private void createDoc(Doc doc, String signature) throws IOException {
        // Так как мы получаем запрос в виде объекта, можем удобно редактировать его структуру по необходимости.
        ObjectMapper map = new ObjectMapper();
        String r = map.writerWithDefaultPrettyPrinter().writeValueAsString(doc); // Преобразование конечного объекта в строку.
        inc++;
        Files.write(Paths.get("data/res" + inc + ".txt"), r.getBytes()); // Создание текстового документа
        rep.put(signature, doc); // Так как в тз было указано реализовать все в одном классе, а файлы записываются в отдельную папку.
//     Для наглядности работы программы, я решил создать Map для хранения подписи, как уникального ключа, и содержания как объекта.
        System.out.println(rep.size()); // Выводится размер репозитория.
    }

    public static void main(String[] args) throws InterruptedException, IOException {
        ConfigurableApplicationContext ctx = SpringApplication.run(CrptApiV2.class, args);
        Thread.sleep(1000);
        // Создание 4 потоков, для демонстрации работы thread-safe и ограничения на количество запросов.
        for (int i = 0; i < 4; i++) {
            CloseableHttpClient httpClient = HttpClients.createDefault();
            HttpPost post = new HttpPost(URI_FOR_REQUEST + "?signature=sign" + i);
            post.setHeader("Content-Type", "application/json");
            HttpEntity httpEntity = new StringEntity(GetJson.js, ContentType.APPLICATION_JSON);
            post.setEntity(httpEntity);
            httpClient.execute(post);
            httpClient.close();
        }
        ctx.close();
    }

    // В конфигурации настраиваются значения для интервала и количества запросов в этот интервал
    @Configuration
    static class SpringConfig {
        @Bean
        public TimeUnit timeUnit() {
            return TimeUnit.MINUTES;
        }

        @Bean
        public int reqCount() {
            return 3;
        }
    }

    // 3 Класса из JSON запроса, для формирования объекта
    @Getter
    @Setter
    @ToString
    static class Doc {
        private Description description;
        private String doc_id;
        private String doc_status;
        private String doc_type;
        private boolean importRequest;
        private String owner_inn;
        private String participant_inn;
        private String producer_inn;
        private String production_date;
        private String production_type;
        private List<Product> products;
        private String reg_date;
        private String reg_number;
    }
    @Getter
    @Setter
    @ToString
    static class Description {
        private String participantInn;
    }
    @Getter
    @Setter
    @ToString
    static class Product {
        private String certificate_document;
        private String certificate_document_date;
        private String certificate_document_number;
        private String owner_inn;
        private String producer_inn;
        private String production_date;
        private String tnved_code;
        private String uit_code;
        private String uitu_code;
    }

    // Класс для формирования документа
    static class GetJson {
        private static  final String js = "{\"description\":\n" +
                "{ \"participantInn\": \"string\" }, \"doc_id\": \"string\", \"doc_status\": \"string\",\n" +
                "  \"doc_type\": \"LP_INTRODUCE_GOODS\", \"importRequest\": true,\n" +
                "  \"owner_inn\": \"string\", \"participant_inn\": \"string\", \"producer_inn\":\n" +
                "\"string\", \"production_date\": \"2020-01-23\", \"production_type\": \"string\",\n" +
                "  \"products\": [ { \"certificate_document\": \"string\",\n" +
                "    \"certificate_document_date\": \"2020-01-23\",\n" +
                "    \"certificate_document_number\": \"string\", \"owner_inn\": \"string\",\n" +
                "    \"producer_inn\": \"string\", \"production_date\": \"2020-01-23\",\n" +
                "    \"tnved_code\": \"string\", \"uit_code\": \"string\", \"uitu_code\": \"string\" } ],\n" +
                "  \"reg_date\": \"2020-01-23\", \"reg_number\": \"string\"}";
    }
}
