# Задача:

Необходимо реализовать на языке Java (можно использовать 17
версию) класс для работы с API Честного знака. Класс должен быть
thread-safe и поддерживать ограничение на количество запросов к
API. Ограничение указывается в конструкторе в виде количества
запросов в определенный интервал времени. Например:
public CrptApi(TimeUnit timeUnit, int requestLimit)
timeUnit – указывает промежуток времени – секунда, минута и пр.
requestLimit – положительное значение, которое определяет
максимальное количество запросов в этом промежутке времени.
При превышении лимита запрос вызов должен блокироваться,
чтобы не превысить максимальное количество запросов к API и
продолжить выполнение, без выбрасывания исключения, когда
ограничение на количество вызов API не будет превышено в
результате этого вызова. В любой ситуации превышать лимит на
количество запросов запрещено для метода.

Реализовать нужно единственный метод – Создание документа для
ввода в оборот товара, произведенного в РФ. Документ и подпись
должны передаваться в метод в виде Java объекта и строки
соответственно.

Вызывается по HTTPS метод POST следующий URL: https://ismp.crpt.ru/api/v3/lk/documents/create

В теле запроса передается в формате JSON документ: {"description":
{ "participantInn": "string" }, "doc_id": "string", "doc_status": "string",
"doc_type": "LP_INTRODUCE_GOODS", 109 "importRequest": true,
"owner_inn": "string", "participant_inn": "string", "producer_inn":
"string", "production_date": "2020-01-23", "production_type": "string",
"products": [ { "certificate_document": "string",
"certificate_document_date": "2020-01-23",
"certificate_document_number": "string", "owner_inn": "string",
"producer_inn": "string", "production_date": "2020-01-23",
"tnved_code": "string", "uit_code": "string", "uitu_code": "string" } ],
"reg_date": "2020-01-23", "reg_number": "string"}

При реализации можно использовать библиотеки HTTP клиента,
JSON сериализации. Реализация должна быть максимально
удобной для последующего расширения функционала.
Решение должно быть оформлено в виде одного файла
CrptApi.java. Все дополнительные классы, которые используются
должны быть внутренними.
Можно прислать ссылку на файл в GitHub.
В задании необходимо просто сделать вызов указанного метода,
реальный API не должен интересовать.

# Решение:
Так как постановка задачи выдалась достаточно абстрактной, я реализовал 2 разных класса, 
где метод синхронизации потоков "syncThr" одинаковый, а метод создания документа "createDoc" сделан по-разному.
Также в задании сказано вызвать готовый метод, поэтому, в каждом из классов реализован метод main, 
с симуляцией работы настоящей программы. 

# 1 Решение: CrptApiV1.
Данный класс принимает в метод документ и подпись, который сериализуется в JSON, 
после чего его отправляет на данный в задании адрес с помощью post-запроса. К сожалению в задании не было документации,
по тому, что надо делать с подписью и что из себя представляет данный сервис, поэтому post-запрос на данный адрес не пускает
по причине недостаточной аутентификации. Однако все равно можно посмотреть результаты работы, вызвав метод main.

# 2 Решение: CrptApiV2.
Работа данного класса диамитральнопротивоположная работе первого решения. Данный класс принимает post запрос и 
из его тела, которое передается в JSON формате(тело такое, как указано в задании), формирует объект, который можно
удобно изменять, получает из query подпись, на основе чего создается новый файл, который записывается в папку data.
Также для демонстрации работы метода, создается Map, который хранит пары ключ-подпись, значение-объект.

# Дополнительно:
Использовал такие фреймворки как: SpringBoot, JacksonJson, Lombok, Apache HttpClient.