package com.example.telegrambotelena;

import com.example.telegrambotelena.BotStageEnum.BotStage;
import com.example.telegrambotelena.GoogleAPIConfig.GoogleSheetsLiveTest;
import com.example.telegrambotelena.Model.Client;
import lombok.AllArgsConstructor;
import lombok.SneakyThrows;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;
import org.telegram.telegrambots.client.okhttp.OkHttpTelegramClient;
import org.telegram.telegrambots.longpolling.util.LongPollingSingleThreadUpdateConsumer;
import org.telegram.telegrambots.meta.api.methods.send.SendMessage;
import org.telegram.telegrambots.meta.api.objects.CallbackQuery;
import org.telegram.telegrambots.meta.api.objects.Update;
import org.telegram.telegrambots.meta.api.objects.User;
import org.telegram.telegrambots.meta.api.objects.replykeyboard.InlineKeyboardMarkup;
import org.telegram.telegrambots.meta.api.objects.replykeyboard.buttons.InlineKeyboardButton;
import org.telegram.telegrambots.meta.api.objects.replykeyboard.buttons.InlineKeyboardRow;
import org.telegram.telegrambots.meta.generics.TelegramClient;

import java.io.IOException;
import java.security.GeneralSecurityException;
import java.time.LocalDate;
import java.time.format.DateTimeParseException;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

@Component
public class UpdateConsumer  implements LongPollingSingleThreadUpdateConsumer {
    private final TelegramClient telegramClient;
    private Client client;
    private boolean questionnaireMode = false;
    private final Map<Long,Client> clientMap = new HashMap<>();

    @Value("${bot.token}")
    private  String botToken;
//    private UserSessionHandler userSessionHandler;

    public UpdateConsumer(@Value("${bot.token}") String botToken , Client client) {
        this.telegramClient = new OkHttpTelegramClient(botToken);
        this.client = client;
    }

    @SneakyThrows
    @Override
    public void consume(Update update) {
        System.out.println(getChatID(update));
        Long chatId = getChatID(update);
        // System.out.println(update.getMessage().getFrom().getUserName() + " " + update.getMessage().getFrom().getFirstName());

        if (chatId == null){
            return;
        }

         client = clientMap.computeIfAbsent(
                getChatID(update),
                k -> Client.builder().botStage(BotStage.IDLE).build());

        if (update.hasMessage()) {
            if (update.getMessage().getText().equals("/start")) {
                sendMainMenu(getChatID(update));
            } else {
               // sendMsg(update.getMessage().getChatId(), "привет , я тебя не понимаю" );
                if (client.getBotStage() != BotStage.IDLE) {
                    questionnaireFormMethod(chatId, client, update);
                }
            }
        } else if (update.hasCallbackQuery()) {
            handleCallbackQuery(update.getCallbackQuery(), update, client);
        }

    }
    @SneakyThrows
    public void sendMsg(Long chatID, String textMsg) {
        SendMessage sendMessage = SendMessage.builder()
                .text(textMsg)
                .chatId(chatID)
                .build();

        telegramClient.execute(sendMessage);
    }

    public Long getChatID(Update update) {
        if (update.hasMessage()) {
            return update.getMessage().getChatId();
        } else if (update.hasCallbackQuery()) {
            return update.getCallbackQuery().getMessage().getChatId();
        }
        return null;
    }

    private void handleCallbackQuery(CallbackQuery callbackQuery, Update update, Client client) {
        var data = callbackQuery.getData();
        var chatID = callbackQuery.getFrom().getId();
        var user = callbackQuery.getFrom();

        switch (data) {
            case "questionnaire":
                client.setBotStage(BotStage.WAITING_EMAIL);
                sendMsg(chatID,"Введите Вашу электронную почту:");
        }
    }

    private void questionnaireFormMethod(Long chatID, Client client, Update update) {
        String text = update.getMessage().getText();
        switch (client.getBotStage()) {

            case WAITING_EMAIL:
                client.setEmail(text);
                client.setBotStage(BotStage.WAITING_NAME);
                sendMsg(chatID, "Введите ваше имя:");
                break;

            case WAITING_NAME:
                client.setName(text);
                client.setBotStage(BotStage.WAITING_SURNAME);
                sendMsg(chatID, "Введите вашу текущую фамилию:");
                break;

            case WAITING_SURNAME:
                client.setSurnameCurrent(text);
                client.setBotStage(BotStage.WAITING_SURNAME_PREVIOUS);
                sendMsg(chatID, "Введите вашу предыдущую фамилию (если меняли, иначе '-') :");
                break;

            case WAITING_SURNAME_PREVIOUS:
                client.setSurnamePrevious(text);
                client.setBotStage(BotStage.WAITING_SURNAME_MAIDEN);
                sendMsg(chatID, "Введите вашу девичью фамилию (если актуально, иначе '-'):");
                break;

            case WAITING_SURNAME_MAIDEN:
                client.setSurnameMaiden(text);
                client.setBotStage(BotStage.WAITING_DATE_OF_BIRTH);
                sendMsg(chatID, "Введите вашу дату рождения (в формате ГГГГ-ММ-ДД):");
                break;

            case WAITING_DATE_OF_BIRTH:
                try {
                    LocalDate date = LocalDate.parse(text);

                    if (date.isAfter(LocalDate.now())) {
                        sendMsg(chatID, "Дата рождения не может быть в будущем! Введите вашу дату рождения (в формате ГГГГ-ММ-ДД):");
                    } else {
                        client.setDateOfBirth(date); // или date, если в классе Client тип поля LocalDate
                        client.setBotStage(BotStage.WAITING_FATHERS_NAME);
                        sendMsg(chatID, "Введите имя вашего отца:");
                    }

                } catch (DateTimeParseException e) {
                    sendMsg(chatID, "Неправильный формат даты! Пожалуйста, введите её строго в формате ГГГГ-ММ-ДД (например: 1995-12-25):");
                }
                break;

            case WAITING_FATHERS_NAME:
                client.setFathersName(text);
                client.setBotStage(BotStage.WAITING_MOTHERS_NAME);
                sendMsg(chatID, "Введите имя вашей матери:");
                break;

            case WAITING_MOTHERS_NAME:
                client.setMothersName(text);
                client.setBotStage(BotStage.WAITING_MOTHERS_SURNAME_MAIDEN);
                sendMsg(chatID, "Введите девичью фамилию вашей матери:");
                break;

            case WAITING_MOTHERS_SURNAME_MAIDEN:
                client.setMothersSurnameMaiden(text);
                client.setBotStage(BotStage.WAITING_MARITAL_STATUS);
                sendMsg(chatID, "Укажите ваше семейное положение (например: женат/замужем, холост/незамужем):");
                break;

            case WAITING_MARITAL_STATUS:
                client.setMaritalStatus(text);
                client.setBotStage(BotStage.WAITING_CITY_OF_BIRTH);
                sendMsg(chatID, "Введите ваш город рождения:");
                break;

            case WAITING_CITY_OF_BIRTH:
                client.setCityOfBirth(text);
                client.setBotStage(BotStage.WAITING_NATIONALITY);
                sendMsg(chatID, "Укажите вашу национальность:");
                break;

            case WAITING_NATIONALITY:
                client.setNationality(text);
                client.setBotStage(BotStage.WAITING_CITIZENSHIP);
                sendMsg(chatID, "Укажите ваше гражданство:");
                break;

            case WAITING_CITIZENSHIP:
                client.setCitizenship(text);
                client.setBotStage(BotStage.WAITING_COUNTRY_OF_BIRTH);
                sendMsg(chatID, "Укажите страну вашего рождения:");
                break;

            case WAITING_COUNTRY_OF_BIRTH:
                client.setCountryOfBirth(text);
                client.setBotStage(BotStage.WAITING_HEIGHT);
                sendMsg(chatID, "Укажите ваш рост (в сантиметрах):");
                break;

            case WAITING_HEIGHT:
                client.setHeight(text);
                client.setBotStage(BotStage.WAITING_PESEL);
                sendMsg(chatID, "Введите ваш номер PESEL (если есть, иначе '-'):");
                break;

            case WAITING_PESEL:
                client.setPESEL(text);
                client.setBotStage(BotStage.WAITING_EDUCATION);
                sendMsg(chatID, "Укажите ваше образование (например: высшее, среднее):");
                break;

            case WAITING_EDUCATION:
                client.setEducation(text);
                client.setBotStage(BotStage.WAITING_EYE_COLOUR);
                sendMsg(chatID, "Укажите цвет ваших глаз:");
                break;

            case WAITING_EYE_COLOUR:
                client.setEyeColour(text);
                client.setBotStage(BotStage.WAITING_CITY_OF_RESIDENCE);
                sendMsg(chatID, "Введите город вашего текущего проживания:");
                break;

            case WAITING_CITY_OF_RESIDENCE:
                client.setCityOfResidence(text);
                client.setBotStage(BotStage.WAITING_STREET_OF_RESIDENCE);
                sendMsg(chatID, "Введите улицу проживания:");
                break;

            case WAITING_STREET_OF_RESIDENCE:
                client.setStreetOfResidence(text);
                client.setBotStage(BotStage.WAITING_HOUSE_NUMBER);
                sendMsg(chatID, "Введите номер дома:");
                break;

            case WAITING_HOUSE_NUMBER:
                client.setHouseNumber(text);
                client.setBotStage(BotStage.WAITING_FLAT_NUMBER);
                sendMsg(chatID, "Введите номер квартиры (если нет, введите '-'):");
                break;

            case WAITING_FLAT_NUMBER:
                client.setFlatNumber(text);
                client.setBotStage(BotStage.WAITING_POSTCODE);
                sendMsg(chatID, "Введите ваш почтовый индекс:");
                break;

            case WAITING_POSTCODE:
                client.setPostcode(text);
                client.setBotStage(BotStage.WAITING_TELEPHONE);
                sendMsg(chatID, "Введите ваш контактный номер телефона:");
                break;

            case WAITING_TELEPHONE:
                client.setTelephone(text);
                client.setBotStage(BotStage.WAITING_LAST_ARRIVAL_DATE);
                sendMsg(chatID, "Введите дату вашего последнего въезда в страну (ГГГГ-ММ-ДД):");
                break;

            case WAITING_LAST_ARRIVAL_DATE:
                try {
                    LocalDate LAST_ARRIVAL_DATE = LocalDate.parse(text);

                    client.setLastArrivalDate(text);
                    client.setBotStage(BotStage.WAITING_PASSPORT_NUMBER);
                    sendMsg(chatID, "Введите серию и номер вашего паспорта:");
                }catch (DateTimeParseException e) {
                    sendMsg(chatID, "Неправильный формат даты! Пожалуйста, введите её строго в формате ГГГГ-ММ-ДД (например: 1995-12-25):");
                }


                break;

            case WAITING_PASSPORT_NUMBER:
                client.setPassportNumber(text);
                client.setBotStage(BotStage.WAITING_RESIDENCE_CARD);
                sendMsg(chatID, "Укажите тип вашей карты побыту (например: сталый/часовый , либо '-'):");
                break;

            case WAITING_RESIDENCE_CARD:

                client.setResidenceCard(text);

                if (text.equals("-")) {
                    client.setResidenceCardExpireDate("-");
                    client.setBotStage(BotStage.IDLE);
                    sendMsg(chatID, "Спасибо! Анкета успешно заполнена.");
                    writeDataToGoogleSheet(client);
                } else {
                    client.setBotStage(BotStage.WAITING_RESIDENCE_CARD_EXPIRE_DATE);
                    sendMsg(chatID, "Введите дату окончания карты побыту ГГГГ-ММ-ДД (если есть, иначе '-'):");
                }
                break;

            case WAITING_RESIDENCE_CARD_EXPIRE_DATE:

                try {
                    LocalDate RESIDENCE_CARD_EXPIRE_DATE = LocalDate.parse(text);

                    client.setResidenceCardExpireDate(text);
                    client.setBotStage(BotStage.IDLE);
                    sendMsg(chatID, "Спасибо! Анкета успешно заполнена.");
                    writeDataToGoogleSheet(client);
                } catch (DateTimeParseException e) {
                    sendMsg(chatID, "Неправильный формат даты! Пожалуйста, введите её строго в формате ГГГГ-ММ-ДД (например: 1995-12-25):");
                }

                break;



        }

    }

    private void writeDataToGoogleSheet(Client client) {
        try {
            GoogleSheetsLiveTest test = new GoogleSheetsLiveTest();
            test.setup();
            test.writeData(client);
            System.out.println("--- ТЕСТ: Данные успешно отправлены в таблицу! ---");
        } catch (GeneralSecurityException e) {
            throw new RuntimeException(e);
        } catch (IOException e) {
            throw new RuntimeException(e);
        }
    }


    @SneakyThrows
    private void sendMainMenu(Long chatId) {
        SendMessage message = SendMessage.builder()
                .text("Добро пожаловать в ТГ Бот Елены, выберите следующие действия: ")
                .chatId(chatId)
                .build();

        var button1 = InlineKeyboardButton.builder()
                .text("Заполнить анкету")
                .callbackData("questionnaire")
                .build();


        List<InlineKeyboardRow> inlineKeyboardRows = List.of(new InlineKeyboardRow( button1));

        InlineKeyboardMarkup inlineKeyboardMarkup = new InlineKeyboardMarkup(inlineKeyboardRows);

        message.setReplyMarkup(inlineKeyboardMarkup);

        telegramClient.execute(message);

        //sendMsg(chatId, message.getText());
    }
}
