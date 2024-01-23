package cprtApi;

import com.fasterxml.jackson.databind.ObjectMapper;
import lombok.Getter;
import lombok.Setter;
import lombok.ToString;
import org.apache.http.HttpEntity;
import org.apache.http.client.methods.CloseableHttpResponse;
import org.apache.http.client.methods.HttpPost;
import org.apache.http.entity.ContentType;
import org.apache.http.entity.StringEntity;
import org.apache.http.impl.client.CloseableHttpClient;
import org.apache.http.impl.client.HttpClients;
import org.apache.http.util.EntityUtils;
import java.io.IOException;
import java.time.Duration;
import java.time.Instant;
import java.util.List;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.locks.Lock;
import java.util.concurrent.locks.ReentrantLock;

public class CrptApiV1 {

    private static final String URI_FOR_REQUEST = "https://ismp.crpt.ru/api/v3/lk/documents/create";
    private final long period;
    private final int reqCount;
    private int innerCount;
    private static Instant time;
    private static final Lock lock = new ReentrantLock();

    public CrptApiV1(TimeUnit timeUnit, int reqCount) {
        this.period = timeUnit.toMillis(1);
        this.reqCount = reqCount;
    }

    public void syncThr(Doc doc, String signature) {
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
        // Создание нового клиента для запроса.
    CloseableHttpClient httpClient = HttpClients.createDefault();
        // Сериализация документа в JSON.
    ObjectMapper mapper = new ObjectMapper();
    String newJson =  mapper.writerWithDefaultPrettyPrinter().writeValueAsString(doc);
        // Установка URL, хедера и тела пост-запроса.
    HttpPost post = new HttpPost(URI_FOR_REQUEST);
    post.setHeader("Content-Type", "application/json");
    HttpEntity httpEntity = new StringEntity(newJson, ContentType.APPLICATION_JSON);
    post.setEntity(httpEntity);
            // Отправка запроса и получение ответа
        CloseableHttpResponse response = httpClient.execute(post);
        // Чтение ответа
    HttpEntity responseEntity = response.getEntity();
        String responseJson =  EntityUtils.toString(responseEntity);
        System.out.println(responseJson); //
            response.close();
            httpClient.close();
    }

    public static void main(String[] args) throws IOException {
        CrptApiV1 crptApiV1 = new CrptApiV1(TimeUnit.MINUTES, 3);
        ObjectMapper mapper = new ObjectMapper();
        // На основе представленного в docx файле JSON-тела, были созданы подходящие для сериализации модели данных.
        Doc doc = mapper.readValue(GetJson.js, Doc.class);
        String signature = "Signature";
        // Создание 4 потоков, для демонстрации работы thread-safe и ограничения на количество запросов.
        for (int i = 0; i < 4; i ++) {
            new Thread(() -> {
                crptApiV1.syncThr(doc, signature);
            }).start();
        }
    }

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
