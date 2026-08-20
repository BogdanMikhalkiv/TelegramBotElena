package com.example.telegrambotelena;

import com.example.telegrambotelena.BotStageEnum.BotStage;
import com.example.telegrambotelena.GoogleAPIConfig.GoogleSheetsLiveTest;
import com.example.telegrambotelena.Model.Client;
import lombok.SneakyThrows;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;
import org.telegram.telegrambots.client.okhttp.OkHttpTelegramClient;
import org.telegram.telegrambots.longpolling.util.LongPollingSingleThreadUpdateConsumer;
import org.telegram.telegrambots.meta.api.methods.send.SendMessage;
import org.telegram.telegrambots.meta.api.objects.CallbackQuery;
import org.telegram.telegrambots.meta.api.objects.Update;
import org.telegram.telegrambots.meta.api.objects.replykeyboard.InlineKeyboardMarkup;
import org.telegram.telegrambots.meta.api.objects.replykeyboard.ReplyKeyboardMarkup;
import org.telegram.telegrambots.meta.api.objects.replykeyboard.buttons.InlineKeyboardButton;
import org.telegram.telegrambots.meta.api.objects.replykeyboard.buttons.InlineKeyboardRow;
import org.telegram.telegrambots.meta.api.objects.replykeyboard.buttons.KeyboardButton;
import org.telegram.telegrambots.meta.api.objects.replykeyboard.buttons.KeyboardRow;
import org.telegram.telegrambots.meta.generics.TelegramClient;

import java.io.IOException;
import java.security.GeneralSecurityException;
import java.time.LocalDate;
import java.time.format.DateTimeParseException;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

@Component
public class UpdateConsumer  implements LongPollingSingleThreadUpdateConsumer {
    private final TelegramClient telegramClient;
    // private Client client;
    private boolean questionnaireEditMode = false;
    private final Map<Long,Client> clientMap = new HashMap<>();
    private final Pattern VALID_EMAIL_ADDRESS_REGEX =
            Pattern.compile("^[A-Z0-9._%+-]+@[A-Z0-9.-]+\\.[A-Z]{2,6}$", Pattern.CASE_INSENSITIVE);
    private final Pattern ONLY_LATIN_LETTERS_OR_SKIP_REGEX = Pattern.compile("^([a-zA-Z]+|-)$");
    private final Pattern HEIGHT_REGEX = Pattern.compile("^\\d{1,3}$");
    private final Pattern PESEL_REGEX = Pattern.compile("^\\d{11}$");
    private final Pattern ALL_LETTERS_REGEX = Pattern.compile("^\\p{L}+$");
    private final Pattern POSTCODE_REGEX = Pattern.compile("^[0-9]{2}-[0-9]{3}$");
    private final Pattern PHONE_NUMBER_REGEX = Pattern.compile("^(\\+48)?\\d{9}$");
    private final Pattern POLISH_TEXT_PATTERN = Pattern.compile("^[a-zA-ZąćęłńóśźżĄĆĘŁŃÓŚŹŻ\\s-]+$");

    @Value("${bot.token}")
    private  String botToken;
//    private UserSessionHandler userSessionHandler;

    public UpdateConsumer(@Value("${bot.token}") String botToken , Client client) {
        this.telegramClient = new OkHttpTelegramClient(botToken);
    }

    @SneakyThrows
    @Override
    public void consume(Update update) {
        System.out.println(getChatID(update));
        Long chatId = getChatID(update);


        if (chatId == null){
            return;
        }

         Client client = clientMap.computeIfAbsent(
                chatId,
                k -> Client.builder().botStage(BotStage.IDLE).build());

        if (update.hasMessage()) {
            if (update.getMessage().getText().equals("/start")) {
                sendMainMenu(getChatID(update));
            } else {
               // sendMsg(update.getMessage().getChatId(), "привет , я тебя не понимаю" );
                if (client.getBotStage() != BotStage.IDLE) {
                    questionnaireFormMethod(chatId, client, update, questionnaireEditMode);
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

        if (!textMsg.equals("Введите Вашу электронную почту:") && !textMsg.equals("Анкета успешно заполнена!")){
            var prevBtn = InlineKeyboardButton.builder()
                    .text("Назад")
                    .callbackData("back")
                    .build();


            List<InlineKeyboardRow> inlineKeyboardRows =
                    List.of(new InlineKeyboardRow(prevBtn));

            InlineKeyboardMarkup inlineKeyboardMarkup = new InlineKeyboardMarkup(inlineKeyboardRows);
            sendMessage.setReplyMarkup(inlineKeyboardMarkup);
        }


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

    @SneakyThrows
    private void handleCallbackQuery(CallbackQuery callbackQuery, Update update, Client client) {
        var data = callbackQuery.getData();
        var chatID = callbackQuery.getFrom().getId();
        var user = callbackQuery.getFrom();




        switch (data) {
            case "questionnaire":
                client.setBotStage(BotStage.WAITING_EMAIL);
                SendMessage sendMessage = SendMessage.builder()
                        .text("Введите Вашу электронную почту:")
                        .chatId(chatID)
                        .build();
                startButton(sendMessage);
                telegramClient.execute(sendMessage);
                break;
            case "married":
                client.setMaritalStatus("Żonaty/Mężatka");
                client.setBotStage(BotStage.WAITING_CITY_OF_BIRTH);
                sendMsg(chatID, "Введите ваш город рождения:");
                break;
            case "unmarried":
                client.setMaritalStatus("Kawaler/Panna");
                client.setBotStage(BotStage.WAITING_CITY_OF_BIRTH);
                sendMsg(chatID, "Введите ваш город рождения:");
                break;
            case "divorced":
                client.setMaritalStatus("Rozwodnik");
                client.setBotStage(BotStage.WAITING_CITY_OF_BIRTH);
                sendMsg(chatID, "Введите ваш город рождения:");
                break;
            case "widow":
                client.setMaritalStatus("Wdowiec/wdowa");
                client.setBotStage(BotStage.WAITING_CITY_OF_BIRTH);
                sendMsg(chatID, "Введите ваш город рождения:");
                break;

            case "back":
                client.setBotStage(client.getBotStage().prev());
                sendStageText(chatID, client.getBotStage(),client);
                break;

            case "correct":
                sendMsg(chatID,"Анкета успешно заполнена!");
                client.setBotStage(BotStage.IDLE);
                questionnaireEditMode = false;
                writeDataToGoogleSheet(client);
                break;

            case "edit":
                questionnaireEditMode = true;
                //client.setBotStage(BotStage.VERIFICATION);
                editMessage("Пожалуйста, выберите НИЖЕ кнопку для изменения значения",chatID);
                break;

            case "edit_email":
                client.setBotStage(BotStage.WAITING_EMAIL);
                sendStageText(chatID, client.getBotStage(),client);
                break;

            case "edit_name":
                client.setBotStage(BotStage.WAITING_NAME);
                sendStageText(chatID, client.getBotStage(),client);
                break;

            case "edit_surnameCurrent":
                client.setBotStage(BotStage.WAITING_SURNAME);
                sendStageText(chatID, client.getBotStage(),client);
                break;

            case "edit_surnamePrevious":
                client.setBotStage(BotStage.WAITING_SURNAME_PREVIOUS);
                sendStageText(chatID, client.getBotStage(),client);
                break;

            case "edit_surnameMaiden":
                client.setBotStage(BotStage.WAITING_SURNAME_MAIDEN);
                sendStageText(chatID, client.getBotStage(),client);
                break;

            case "edit_dateOfBirth":
                client.setBotStage(BotStage.WAITING_DATE_OF_BIRTH);
                sendStageText(chatID, client.getBotStage(),client);
                break;

            case "edit_fathersName":
                client.setBotStage(BotStage.WAITING_FATHERS_NAME);
                sendStageText(chatID, client.getBotStage(),client);
                break;

            case "edit_mothersName":
                client.setBotStage(BotStage.WAITING_MOTHERS_NAME);
                sendStageText(chatID, client.getBotStage(),client);
                break;

            case "edit_mothersSurnameMaiden":
                client.setBotStage(BotStage.WAITING_MOTHERS_SURNAME_MAIDEN);
                sendStageText(chatID, client.getBotStage(),client);
                break;

            case "edit_maritalStatus":
                client.setBotStage(BotStage.WAITING_MARITAL_STATUS);
                sendStageText(chatID, client.getBotStage(),client);
                break;

            case "edit_cityOfBirth":
                client.setBotStage(BotStage.WAITING_CITY_OF_BIRTH);
                sendStageText(chatID, client.getBotStage(),client);
                break;

            case "edit_nationality":
                client.setBotStage(BotStage.WAITING_NATIONALITY);
                sendStageText(chatID, client.getBotStage(),client);
                break;

            case "edit_citizenship":
                client.setBotStage(BotStage.WAITING_CITIZENSHIP);
                sendStageText(chatID, client.getBotStage(),client);
                break;

            case "edit_countryOfBirth":
                client.setBotStage(BotStage.WAITING_COUNTRY_OF_BIRTH);
                sendStageText(chatID, client.getBotStage(),client);
                break;

            case "edit_height":
                client.setBotStage(BotStage.WAITING_HEIGHT);
                sendStageText(chatID, client.getBotStage(),client);
                break;

            case "edit_PESEL":
                client.setBotStage(BotStage.WAITING_PESEL);
                sendStageText(chatID, client.getBotStage(),client);
                break;

            case "edit_education":
                client.setBotStage(BotStage.WAITING_EDUCATION);
                sendStageText(chatID, client.getBotStage(),client);
                break;

            case "edit_eyeColour":
                client.setBotStage(BotStage.WAITING_EYE_COLOUR);
                sendStageText(chatID, client.getBotStage(),client);
                break;

            case "edit_cityOfResidence":
                client.setBotStage(BotStage.WAITING_CITY_OF_RESIDENCE);
                sendStageText(chatID, client.getBotStage(),client);
                break;

            case "edit_streetOfResidence":
                client.setBotStage(BotStage.WAITING_STREET_OF_RESIDENCE);
                sendStageText(chatID, client.getBotStage(),client);
                break;

            case "edit_houseNumber":
                client.setBotStage(BotStage.WAITING_HOUSE_NUMBER);
                sendStageText(chatID, client.getBotStage(),client);
                break;

            case "edit_flatNumber":
                client.setBotStage(BotStage.WAITING_FLAT_NUMBER);
                sendStageText(chatID, client.getBotStage(),client);
                break;

            case "edit_postcode":
                client.setBotStage(BotStage.WAITING_POSTCODE);
                sendStageText(chatID, client.getBotStage(),client);
                break;

            case "edit_telephone":
                client.setBotStage(BotStage.WAITING_TELEPHONE);
                sendStageText(chatID, client.getBotStage(),client);
                break;

            case "edit_lastArrivalDate":
                client.setBotStage(BotStage.WAITING_LAST_ARRIVAL_DATE);
                sendStageText(chatID, client.getBotStage(),client);
                break;

            case "edit_passportNumber":
                client.setBotStage(BotStage.WAITING_PASSPORT_NUMBER);
                sendStageText(chatID, client.getBotStage(),client);
                break;

            case "edit_residenceCard":
                client.setBotStage(BotStage.WAITING_RESIDENCE_CARD);
                sendStageText(chatID, client.getBotStage(),client);
                break;

            case "edit_residenceCardExpireDate":
                client.setBotStage(BotStage.WAITING_RESIDENCE_CARD_EXPIRE_DATE);
                sendStageText(chatID, client.getBotStage(),client);
                break;


        }

    }

    public void sendStageText(Long chatId, BotStage stage, Client client) {
        String text = switch (stage) {
            case IDLE -> null;
            case WAITING_EMAIL -> "Введите Вашу электронную почту:";
            case WAITING_NAME -> "Введите ваше имя в латинице:";
            case WAITING_SURNAME -> "Введите вашу текущую фамилию в латинице:";
            case WAITING_SURNAME_PREVIOUS -> "Введите вашу предыдущую фамилию в латинице (если не было предыдущей фамилии,  просто прочерк '-') :";
            case WAITING_SURNAME_MAIDEN -> "Введите вашу девичью фамилию в латинице (если не было, просто прочерк '-'):";
            case WAITING_DATE_OF_BIRTH -> "Введите вашу дату рождения (в формате ГГГГ-ММ-ДД):";
            case WAITING_FATHERS_NAME-> "Введите имя вашего отца в латинице:";
            case WAITING_MOTHERS_NAME -> "Введите имя вашей матери в латинице:";
            case WAITING_MOTHERS_SURNAME_MAIDEN -> "Введите девичью фамилию вашей матери в латинице:";
            case WAITING_MARITAL_STATUS -> "Укажите ваше семейное положение: ";
            case WAITING_CITY_OF_BIRTH -> "Введите ваш город рождения:";
            case WAITING_NATIONALITY -> "Укажите вашу национальность:";
            case WAITING_CITIZENSHIP -> "Укажите ваше гражданство:";
            case WAITING_COUNTRY_OF_BIRTH -> "Укажите страну вашего рождения:";
            case WAITING_HEIGHT -> "Укажите ваш рост (в сантиметрах, например 180):";
            case WAITING_PESEL -> "Введите ваш номер PESEL (если есть, иначе '-'):";
            case WAITING_EDUCATION -> "Укажите ваше образование (например: высшее, среднее):";
            case WAITING_EYE_COLOUR -> "Укажите цвет ваших глаз:";
            case WAITING_CITY_OF_RESIDENCE -> "Введите город Вашего текущего проживания по польски:";
            case WAITING_STREET_OF_RESIDENCE -> "Введите улицу проживания по польски:";
            case WAITING_HOUSE_NUMBER -> "Введите номер дома:";
            case WAITING_FLAT_NUMBER -> "Введите номер квартиры (если нет, введите '-'):";
            case WAITING_POSTCODE -> "Введите ваш почтовый индекс:";
            case WAITING_TELEPHONE -> "Введите ваш контактный номер телефона:";
            case WAITING_LAST_ARRIVAL_DATE -> "Введите дату вашего последнего въезда в Польшу (ГГГГ-ММ-ДД):";
            case WAITING_PASSPORT_NUMBER -> "Введите серию и номер вашего паспорта:";
            case WAITING_RESIDENCE_CARD -> "Укажите тип вашей карты побыту (например: сталый/часовый , либо '-'):";
            case WAITING_RESIDENCE_CARD_EXPIRE_DATE -> "Введите дату окончания карты побыту ГГГГ-ММ-ДД (если есть, иначе '-'):";
            case VERIFICATION -> "Спасибо! Анкета успешно изменена.\n" +
                    " Проверьте ваши данные: \n" +
                    client +
                    "\n Если все верно , нажмите на кнопку ниже  - ВСЕ ВЕРНО" +
                    ", если ещё хотите что то изменить, нажмите на кнопку -  ИЗМЕНИТЬ ДАННЫЕ ⬇️ ";
        };
        if (stage.equals(BotStage.VERIFICATION)){
            verificationMessage(text, chatId);
        } else {
            sendMsg(chatId, text);
        }
    }

    private boolean isValidEmailFormat(String emailStr) {
        Matcher matcher = VALID_EMAIL_ADDRESS_REGEX.matcher(emailStr);
        return matcher.matches();
    }

    private boolean isValidPolishLetters(String text) {
        Matcher matcher = POLISH_TEXT_PATTERN.matcher(text);
        return matcher.matches();
    }

    private boolean isValidOnlyLettersInText(String text) {
        Matcher matcher = ONLY_LATIN_LETTERS_OR_SKIP_REGEX.matcher(text);
        return matcher.matches();
    }

    private boolean isValidHeight(String text) {
        Matcher matcher = HEIGHT_REGEX.matcher(text);
        return matcher.matches();
    }

    private boolean isValidPESEL(String text) {
        Matcher matcher = PESEL_REGEX.matcher(text);
        return matcher.matches();
    }

    private  boolean isValideAllLettersUnicode(String text) {
        Matcher matcher = ALL_LETTERS_REGEX.matcher(text);
        return matcher.matches();
    }

    private boolean isValidePostcode(String text) {
        Matcher matcher = POSTCODE_REGEX.matcher(text);
        return matcher.matches();
    }

    private boolean isValidPhoneNumber(String text) {
        Matcher matcher = PHONE_NUMBER_REGEX.matcher(text);
        return matcher.matches();
    }

    private void moveToNextStage(Boolean isEditMode, BotStage stage, Client currentClient,  Long chatID, String text) {
        if (isEditMode){
            currentClient.setBotStage(BotStage.VERIFICATION);
            sendStageText(chatID, currentClient.getBotStage(),currentClient);
        } else {
            if (!stage.equals(BotStage.WAITING_MARITAL_STATUS)){
                sendMsg(chatID, text);
            } else {
                maritalStatus(chatID);
            }
            currentClient.setBotStage(stage);
        }
    }


    @SneakyThrows
    private void questionnaireFormMethod(Long chatID, Client client, Update update, boolean questionnaireEditMode) {
        String text = update.getMessage().getText();
        switch (client.getBotStage()) {



            case WAITING_EMAIL:

                if (!isValidEmailFormat(text)) {
                    sendMsg(chatID, "Неправильный формат эл. почты! Внимательно проверьте!");
                    System.out.println(client.getBotStage());
                } else {
                    client.setEmail(text);
//                    if (questionnaireEditMode){
//                        client.setBotStage(BotStage.VERIFICATION);
//                        sendStageText(chatID, client.getBotStage());
//                    } else {
//                        client.setBotStage(BotStage.WAITING_NAME);
//                        sendMsg(chatID, "Введите ваше имя в латинице:");
//                    }
                    moveToNextStage(
                            questionnaireEditMode,
                            BotStage.WAITING_NAME,
                            client,
                            chatID,
                            "Введите ваше имя в латинице:"
                    );
                }
                break;

            case WAITING_NAME:

                if (!isValidOnlyLettersInText(text)) {
                    sendMsg(chatID,"Вводимые данные должны содержать исключительно латинские буквы ! Проверьте внимательно!");
                } else {
                    client.setName(text);
//                    sendMsg(chatID, "Введите вашу текущую фамилию в латинице:");
                    
                    moveToNextStage(
                            questionnaireEditMode,
                            BotStage.WAITING_SURNAME,
                            client,
                            chatID,
                            "Введите вашу текущую фамилию в латинице:"
                    );
                }
                break;

            case WAITING_SURNAME:
                if (!isValidOnlyLettersInText(text)) {
                    sendMsg(chatID,"Вводимые данные должны содержать исключительно латинские буквы! Проверьте внимательно!");
                } else {
                    client.setSurnameCurrent(text);
                    moveToNextStage(
                            questionnaireEditMode,
                            BotStage.WAITING_SURNAME_PREVIOUS,
                            client,
                            chatID,
                            "Введите вашу предыдущую фамилию в латинице (если не было предыдущей фамилии,  просто прочерк '-') :"
                    );
//                    client.setBotStage(BotStage.WAITING_SURNAME_PREVIOUS);
//                    sendMsg(chatID, "Введите вашу предыдущую фамилию в латинице (если не было предыдущей фамилии,  просто прочерк '-') :");
                }
                break;

            case WAITING_SURNAME_PREVIOUS:
                if (!isValidOnlyLettersInText(text)) {
                    sendMsg(chatID,"Вводимые данные должны содержать исключительно латинские буквы! Проверьте внимательно!");
                } else {
                    client.setSurnamePrevious(text);
//                    client.setBotStage(BotStage.WAITING_SURNAME_MAIDEN);
//                    sendMsg(chatID, "Введите вашу девичью фамилию в латинице (если не было, просто прочерк '-'):");
                    moveToNextStage(
                            questionnaireEditMode,
                            BotStage.WAITING_SURNAME_MAIDEN,
                            client,
                            chatID,
                            "Введите вашу девичью фамилию в латинице (если не было, просто прочерк '-'):"
                    );
                }

                break;

            case WAITING_SURNAME_MAIDEN:
                if (!isValidOnlyLettersInText(text)) {
                    sendMsg(chatID,"Вводимые данные должны содержать исключительно латинские буквы! Проверьте внимательно!");
                } else {
                    client.setSurnameMaiden(text);
//                    client.setBotStage(BotStage.WAITING_DATE_OF_BIRTH);
//                    sendMsg(chatID, "Введите вашу дату рождения (в формате ГГГГ-ММ-ДД):");
                    moveToNextStage(
                            questionnaireEditMode,
                            BotStage.WAITING_DATE_OF_BIRTH,
                            client,
                            chatID,
                            "Введите вашу дату рождения (в формате ГГГГ-ММ-ДД):"
                    );
                }
                break;

            case WAITING_DATE_OF_BIRTH:
                try {
                    LocalDate date = LocalDate.parse(text);

                    if (date.isAfter(LocalDate.now())) {
                        sendMsg(chatID, "Дата рождения не может быть в будущем! Введите вашу дату рождения (в формате ГГГГ-ММ-ДД):");
                    } else {
                        client.setDateOfBirth(date);
//                        client.setBotStage(BotStage.WAITING_FATHERS_NAME);
//                        sendMsg(chatID, "Введите имя вашего отца в латинице:");
                        moveToNextStage(
                                questionnaireEditMode,
                                BotStage.WAITING_FATHERS_NAME,
                                client,
                                chatID,
                                "Введите имя вашего отца в латинице:"
                        );
                    }

                } catch (DateTimeParseException e) {
                    sendMsg(chatID, "Неправильный формат даты! Пожалуйста, введите её строго в формате ГГГГ-ММ-ДД (например: 1995-12-25):");
                }
                break;

            case WAITING_FATHERS_NAME:
                if (!isValidOnlyLettersInText(text)) {
                    sendMsg(chatID,"Вводимые данные должны содержать исключительно латинские буквы! Проверьте внимательно!");
                } else {
                    client.setFathersName(text);
//                    client.setBotStage(BotStage.WAITING_MOTHERS_NAME);
//                    sendMsg(chatID, "Введите имя вашей матери в латинице:");
                    moveToNextStage(
                            questionnaireEditMode,
                            BotStage.WAITING_MOTHERS_NAME,
                            client,
                            chatID,
                            "Введите имя вашей матери в латинице:"
                    );
                }
                break;

            case WAITING_MOTHERS_NAME:
                if (!isValidOnlyLettersInText(text)) {
                    sendMsg(chatID,"Вводимые данные должны содержать исключительно латинские буквы! Проверьте внимательно!");
                } else {
                    client.setMothersName(text);
//                    client.setBotStage(BotStage.WAITING_MOTHERS_SURNAME_MAIDEN);
//                    sendMsg(chatID, "Введите девичью фамилию вашей матери в латинице:");
                    moveToNextStage(
                            questionnaireEditMode,
                            BotStage.WAITING_MOTHERS_SURNAME_MAIDEN,
                            client,
                            chatID,
                            "Введите девичью фамилию вашей матери в латинице:"
                    );
                }
                break;

            case WAITING_MOTHERS_SURNAME_MAIDEN:
                if (!isValidOnlyLettersInText(text)) {
                    sendMsg(chatID,"Вводимые данные должны содержать исключительно латинские буквы! Проверьте внимательно!");
                } else {
                    client.setMothersSurnameMaiden(text);
//                    client.setBotStage(BotStage.WAITING_MARITAL_STATUS);
                    //maritalStatus(chatID);
                    moveToNextStage(
                            questionnaireEditMode,
                            BotStage.WAITING_MARITAL_STATUS,
                            client,
                            chatID,
                            "-"
                    );

                }
                break;

            //добавить кнопки с выбором семейного положения!
            case WAITING_MARITAL_STATUS:
                //client.setMaritalStatus(text);
//                client.setBotStage(BotStage.WAITING_CITY_OF_BIRTH);
//                sendMsg(chatID, "Введите ваш город рождения:");
//                moveToNextStage(
//                        questionnaireEditMode,
//                        BotStage.WAITING_CITY_OF_BIRTH,
//                        client,
//                        chatID,
//                        "Введите ваш город рождения:"
//                );
                break;

            case WAITING_CITY_OF_BIRTH:
                if (!isValideAllLettersUnicode(text)){
                    sendMsg(chatID,"Вводимые данные должны соджержать ИСКЛЮЧИТЕЛЬНО буквы! Проверьте внимательнее!");
                } else {
                    client.setCityOfBirth(text);
//                    client.setBotStage(BotStage.WAITING_NATIONALITY);
//                    sendMsg(chatID, "Укажите вашу национальность:");
                    moveToNextStage(
                            questionnaireEditMode,
                            BotStage.WAITING_NATIONALITY,
                            client,
                            chatID,
                            "Укажите вашу национальность:"
                    );
                }
                break;

            case WAITING_NATIONALITY:
                if (!isValideAllLettersUnicode(text)){
                    sendMsg(chatID,"Вводимые данные должны соджержать ИСКЛЮЧИТЕЛЬНО буквы! Проверьте внимательнее!");
                } else {
                    client.setNationality(text);
//                    client.setBotStage(BotStage.WAITING_CITIZENSHIP);
//                    sendMsg(chatID, "Укажите ваше гражданство:");
                    moveToNextStage(
                            questionnaireEditMode,
                            BotStage.WAITING_CITIZENSHIP,
                            client,
                            chatID,
                            "Укажите ваше гражданство:"
                    );
                }
                break;

            case WAITING_CITIZENSHIP:
                if (!isValideAllLettersUnicode(text)){
                    sendMsg(chatID,"Вводимые данные должны соджержать ИСКЛЮЧИТЕЛЬНО буквы! Проверьте внимательнее!");
                } else {
                    client.setCitizenship(text);
//                    client.setBotStage(BotStage.WAITING_COUNTRY_OF_BIRTH);
//                    sendMsg(chatID, "Укажите страну вашего рождения:");
                    moveToNextStage(
                            questionnaireEditMode,
                            BotStage.WAITING_COUNTRY_OF_BIRTH,
                            client,
                            chatID,
                            "Укажите страну вашего рождения:"
                    );
                }
                break;

            case WAITING_COUNTRY_OF_BIRTH:
                if (!isValideAllLettersUnicode(text)){
                    sendMsg(chatID,"Вводимые данные должны соджержать ИСКЛЮЧИТЕЛЬНО буквы! Проверьте внимательнее!");
                } else {
                    client.setCountryOfBirth(text);
//                    client.setBotStage(BotStage.WAITING_HEIGHT);
//                    sendMsg(chatID, "Укажите ваш рост (в сантиметрах, например 180):");
                    moveToNextStage(
                            questionnaireEditMode,
                            BotStage.WAITING_HEIGHT,
                            client,
                            chatID,
                            "Укажите ваш рост (в сантиметрах, например 180):"
                    );
                }
                break;

            case WAITING_HEIGHT:
                if (!isValidHeight(text)){
                    sendMsg(chatID, "Рост должен быть ИСКЛЮЧИТЕЛЬНО в числовом формате, например : 180 или 167");
                } else {
                    client.setHeight(text);
//                    client.setBotStage(BotStage.WAITING_PESEL);
//                    sendMsg(chatID, "Введите ваш номер PESEL (если есть, иначе '-'):");
                    moveToNextStage(
                            questionnaireEditMode,
                            BotStage.WAITING_PESEL,
                            client,
                            chatID,
                            "Введите ваш номер PESEL (если есть, иначе '-'):"
                    );
                }
                break;


            case WAITING_PESEL:

                if (!isValidPESEL(text)){
                    sendMsg(chatID, "PESEL должны быть ИСКЛЮЧИТЕЛЬНО в числовом виде и минимум 11 цифр! ");
                } else {
                    client.setPESEL(text);
//                    client.setBotStage(BotStage.WAITING_EDUCATION);
//                    sendMsg(chatID, "Укажите ваше образование (например: высшее, среднее):");
                    moveToNextStage(
                            questionnaireEditMode,
                            BotStage.WAITING_EDUCATION,
                            client,
                            chatID,
                            "Укажите ваше образование (например: высшее, среднее):"
                    );
                }
                break;

            //ДОБАВИТЬ КНОПКИ С ВЫБОРОМ ОБРАЗОВАНИЯ
            case WAITING_EDUCATION:
                client.setEducation(text);
//                client.setBotStage(BotStage.WAITING_EYE_COLOUR);
//                sendMsg(chatID, "Укажите цвет ваших глаз:");
                moveToNextStage(
                        questionnaireEditMode,
                        BotStage.WAITING_EYE_COLOUR,
                        client,
                        chatID,
                        "Укажите цвет ваших глаз:"
                );
                break;

            case WAITING_EYE_COLOUR:
                if (!isValideAllLettersUnicode(text)){
                     sendMsg(chatID,"Вводимые данные должны соджержать ИСКЛЮЧИТЕЛЬНО буквы! Проверьте внимательнее!");
                } else {
                    client.setEyeColour(text);
//                    client.setBotStage(BotStage.WAITING_CITY_OF_RESIDENCE);
//                    sendMsg(chatID, "Введите город вашего текущего проживания по польски:");
                    moveToNextStage(
                            questionnaireEditMode,
                            BotStage.WAITING_CITY_OF_RESIDENCE,
                            client,
                            chatID,
                            "Введите город вашего текущего проживания в Польше:"
                    );
                }
                break;

            case WAITING_CITY_OF_RESIDENCE:
                if (!isValidPolishLetters(text)){
                    sendMsg(chatID,"Вводимые данные должны быть ИСКЛЮЧИТЕЛЬНО на польском языке, без цифр и специальных знаков! Проверьте пожалуйста внимательнее!");
                } else {
                    client.setCityOfResidence(text);
//                    client.setBotStage(BotStage.WAITING_STREET_OF_RESIDENCE);
//                    sendMsg(chatID, "Введите улицу проживания по польски:");
                    moveToNextStage(
                            questionnaireEditMode,
                            BotStage.WAITING_STREET_OF_RESIDENCE,
                            client,
                            chatID,
                            "Введите улицу проживания в Польше:"
                    );
                }
                break;

            case WAITING_STREET_OF_RESIDENCE:
                if (!isValideAllLettersUnicode(text)){
                    sendMsg(chatID,"Вводимые данные должны соджержать ИСКЛЮЧИТЕЛЬНО буквы! Проверьте внимательнее!");
                } else {
                    client.setStreetOfResidence(text);
//                    client.setBotStage(BotStage.WAITING_HOUSE_NUMBER);
//                    sendMsg(chatID, "Введите номер дома:");
                    moveToNextStage(
                            questionnaireEditMode,
                            BotStage.WAITING_HOUSE_NUMBER,
                            client,
                            chatID,
                            "Введите номер дома:"
                    );
                }
                break;

            case WAITING_HOUSE_NUMBER:
                client.setHouseNumber(text);
//                client.setBotStage(BotStage.WAITING_FLAT_NUMBER);
//                sendMsg(chatID, "Введите номер квартиры (если нет, введите '-'):");
                moveToNextStage(
                        questionnaireEditMode,
                        BotStage.WAITING_FLAT_NUMBER,
                        client,
                        chatID,
                        "Введите номер квартиры (если нет, введите '-'):"
                );
                break;

            case WAITING_FLAT_NUMBER:
                client.setFlatNumber(text);
//                client.setBotStage(BotStage.WAITING_POSTCODE);
//                sendMsg(chatID, "Введите ваш почтовый индекс:");
                moveToNextStage(
                        questionnaireEditMode,
                        BotStage.WAITING_POSTCODE,
                        client,
                        chatID,
                        "Введите Ваш почтовый индекс:"
                );
                break;

            case WAITING_POSTCODE:
                if (!isValidePostcode(text)) {
                    sendMsg(chatID,"ВводимыЙ формат почтового индекса ДОЛЖЕН состоят из 5 цифр в формате: ХХ-ХХХ ");
                } else {
                    client.setPostcode(text);
//                    client.setBotStage(BotStage.WAITING_TELEPHONE);
//                    sendMsg(chatID, "Введите ваш контактный номер телефона:");
                    moveToNextStage(
                            questionnaireEditMode,
                            BotStage.WAITING_TELEPHONE,
                            client,
                            chatID,
                            "Введите Ваш контактный номер телефона:"
                    );
                }
                break;

            case WAITING_TELEPHONE:
                if (!isValidPhoneNumber(text)) {
                    sendMsg(chatID,"Номер телефона должен быть в следующих форматах: +48ХХХХХХХХХ или просто из 9 цифр");
                } else {
                    client.setTelephone(text);
//                    client.setBotStage(BotStage.WAITING_LAST_ARRIVAL_DATE);
//                    sendMsg(chatID, "Введите дату вашего последнего въезда в Польшу (ГГГГ-ММ-ДД):");
                    moveToNextStage(
                            questionnaireEditMode,
                            BotStage.WAITING_LAST_ARRIVAL_DATE,
                            client,
                            chatID,
                            "Введите дату Вашего последнего въезда в Польшу (ГГГГ-ММ-ДД):"
                    );
                }
                break;

            case WAITING_LAST_ARRIVAL_DATE:
                try {
                    LocalDate LAST_ARRIVAL_DATE = LocalDate.parse(text);

                    client.setLastArrivalDate(LAST_ARRIVAL_DATE);
//                    client.setBotStage(BotStage.WAITING_PASSPORT_NUMBER);
//                    sendMsg(chatID, "Введите серию и номер вашего паспорта:");
                    moveToNextStage(
                            questionnaireEditMode,
                            BotStage.WAITING_PASSPORT_NUMBER,
                            client,
                            chatID,
                            "Введите серию и номер Вашего паспорта:"
                    );
                }catch (DateTimeParseException e) {
                    sendMsg(chatID, "Неправильный формат даты! Пожалуйста, введите её строго в формате ГГГГ-ММ-ДД (например - 1995-12-25):");
                }


                break;

            case WAITING_PASSPORT_NUMBER:
                client.setPassportNumber(text);
//                client.setBotStage(BotStage.WAITING_RESIDENCE_CARD);
//                sendMsg(chatID, "Укажите тип вашей карты побыту (например: сталый/часовый , либо '-'):");
                moveToNextStage(
                        questionnaireEditMode,
                        BotStage.WAITING_RESIDENCE_CARD,
                        client,
                        chatID,
                        "Укажите тип Вашей карты побыту (например: сталый/часовый , если нету карты побыту впишите прочерк '-'):"
                );
                break;

            case WAITING_RESIDENCE_CARD:

                client.setResidenceCard(text);

                if (text.equals("-")) {
                    client.setResidenceCardExpireDate("-");
                    client.setBotStage(BotStage.VERIFICATION);
                    //sendMsg(chatID, "Спасибо! Анкета успешно заполнена.\n Проверьте ваши данные\n" + client);
                    //writeDataToGoogleSheet(client);
                    verificationMessage("Спасибо! Анкета успешно заполнена.\n" +
                            " Проверьте ваши данные: \n" +
                            client +
                            "\n Если все верно , нажмите на кнопку ниже  - ВСЕ ВЕРНО" +
                            ", если хотите что то изменить нажмите на кнопку -  ИЗМЕНИТЬ ДАННЫЕ ⬇️ ", chatID
                    );
                } else {
//                    client.setBotStage(BotStage.WAITING_RESIDENCE_CARD_EXPIRE_DATE);
//                    sendMsg(chatID, "Введите дату окончания карты побыту ГГГГ-ММ-ДД (если есть, иначе '-'):");
                    moveToNextStage(
                            questionnaireEditMode,
                            BotStage.WAITING_RESIDENCE_CARD_EXPIRE_DATE,
                            client,
                            chatID,
                            "Введите дату окончания карты побыту ГГГГ-ММ-ДД:"
                    );
                }
                break;

            case WAITING_RESIDENCE_CARD_EXPIRE_DATE:

                try {
                    LocalDate RESIDENCE_CARD_EXPIRE_DATE = LocalDate.parse(text);

                    client.setResidenceCardExpireDate(RESIDENCE_CARD_EXPIRE_DATE.toString());
                    client.setBotStage(BotStage.VERIFICATION);
                    //client.setBotStage(BotStage.IDLE);

                    //sendMsg(chatID, "Спасибо! Анкета успешно заполнена.\n Проверьте ваши данные\n" + client + "\n Если все верно , нажмите на кнопку ниже ⬇️ ");
                    //writeDataToGoogleSheet(client);

                    verificationMessage("Спасибо! Анкета успешно заполнена.\n" +
                                            " Проверьте ваши данные: \n" +
                                            client +
                                            "\n Если все верно , нажмите на кнопку ниже  - ВСЕ ВЕРНО" +
                                            ", если хотите что то изменить нажмите на кнопку -  ИЗМЕНИТЬ ДАННЫЕ ⬇️ ", chatID
                    );
                } catch (DateTimeParseException e) {
                    sendMsg(chatID, "Неправильный формат даты! Пожалуйста, введите её строго в формате ГГГГ-ММ-ДД (например: 1995-12-25):");
                }

                break;

            case VERIFICATION:


                break;

        }

    }

    @SneakyThrows
    private void verificationMessage(String msg, Long chatId) {
        SendMessage message = SendMessage.builder()
                .text(msg)
                .chatId(chatId)
                .build();

        var buttonCorrect = InlineKeyboardButton.builder()
                .text("Все верно!")
                .callbackData("correct")
                .build();
        var buttonEdit = InlineKeyboardButton.builder()
                .text("Изменить данные")
                .callbackData("edit")
                .build();

        List<InlineKeyboardRow> inlineKeyboardRows = List.of(new InlineKeyboardRow(buttonCorrect),
                                                                new InlineKeyboardRow(buttonEdit)
        );
        InlineKeyboardMarkup inlineKeyboardMarkup = new InlineKeyboardMarkup(inlineKeyboardRows);

        message.setReplyMarkup(inlineKeyboardMarkup);

        telegramClient.execute(message);
    }
    @SneakyThrows
    private void editMessage(String msg, Long chatId) {
        SendMessage message = SendMessage.builder()
                .text(msg)
                .chatId(chatId)
                .build();


        var btnEmail = InlineKeyboardButton.builder()
                .text("📧 Email")
                .callbackData("edit_email")
                .build();

        var btnName = InlineKeyboardButton.builder()
                .text("👤 Имя")
                .callbackData("edit_name")
                .build();

        var btnSurnameCurrent = InlineKeyboardButton.builder()
                .text("👤 Текущая фамилия")
                .callbackData("edit_surnameCurrent")
                .build();

        var btnSurnamePrevious = InlineKeyboardButton.builder()
                .text("👤 Предыдущая фамилия")
                .callbackData("edit_surnamePrevious")
                .build();

        var btnSurnameMaiden = InlineKeyboardButton.builder()
                .text("👤 Девичья фамилия")
                .callbackData("edit_surnameMaiden")
                .build();

        var btnDateOfBirth = InlineKeyboardButton.builder()
                .text("📅 Дата рождения")
                .callbackData("edit_dateOfBirth")
                .build();


        var btnFathersName = InlineKeyboardButton.builder()
                .text("👨 Имя отца")
                .callbackData("edit_fathersName")
                .build();

        var btnMothersName = InlineKeyboardButton.builder()
                .text("👩 Имя матери")
                .callbackData("edit_mothersName")
                .build();

        var btnMothersSurnameMaiden = InlineKeyboardButton.builder()
                .text("👩 Девичья фамилия матери")
                .callbackData("edit_mothersSurnameMaiden")
                .build();


        var btnMaritalStatus = InlineKeyboardButton.builder()
                .text("💍 Семейное положение")
                .callbackData("edit_maritalStatus")
                .build();

        var btnCityOfBirth = InlineKeyboardButton.builder()
                .text("🏙 Город рождения")
                .callbackData("edit_cityOfBirth")
                .build();

        var btnNationality = InlineKeyboardButton.builder()
                .text("🌍 Национальность")
                .callbackData("edit_nationality")
                .build();

        var btnCitizenship = InlineKeyboardButton.builder()
                .text("🏛 Гражданство")
                .callbackData("edit_citizenship")
                .build();

        var btnCountryOfBirth = InlineKeyboardButton.builder()
                .text("🗺 Страна рождения")
                .callbackData("edit_countryOfBirth")
                .build();

        var btnHeight = InlineKeyboardButton.builder()
                .text("📏 Рост")
                .callbackData("edit_height")
                .build();

        var btnPESEL = InlineKeyboardButton.builder()
                .text("🔢 PESEL")
                .callbackData("edit_PESEL")
                .build();

        var btnEducation = InlineKeyboardButton.builder()
                .text("🎓 Образование")
                .callbackData("edit_education")
                .build();

        var btnEyeColour = InlineKeyboardButton.builder()
                .text("👁 Цвет глаз")
                .callbackData("edit_eyeColour")
                .build();


        var btnCityOfResidence = InlineKeyboardButton.builder()
                .text("🏙 Город проживания")
                .callbackData("edit_cityOfResidence")
                .build();

        var btnStreetOfResidence = InlineKeyboardButton.builder()
                .text("🛣 Улица проживания")
                .callbackData("edit_streetOfResidence")
                .build();

        var btnHouseNumber = InlineKeyboardButton.builder()
                .text("🏠 Номер дома")
                .callbackData("edit_houseNumber")
                .build();

        var btnFlatNumber = InlineKeyboardButton.builder()
                .text("🚪 Номер квартиры")
                .callbackData("edit_flatNumber")
                .build();

        var btnPostcode = InlineKeyboardButton.builder()
                .text("📮 Почтовый индекс")
                .callbackData("edit_postcode")
                .build();

        var btnTelephone = InlineKeyboardButton.builder()
                .text("📱 Телефон")
                .callbackData("edit_telephone")
                .build();


        var btnLastArrivalDate = InlineKeyboardButton.builder()
                .text("✈️ Дата последнего въезда в Польшу")
                .callbackData("edit_lastArrivalDate")
                .build();

        var btnPassportNumber = InlineKeyboardButton.builder()
                .text("📕 Номер паспорта")
                .callbackData("edit_passportNumber")
                .build();

        var btnResidenceCard = InlineKeyboardButton.builder()
                .text("💳 Карта побыту")
                .callbackData("edit_residenceCard")
                .build();

        var btnResidenceCardExpireDate = InlineKeyboardButton.builder()
                .text("⏳ Срок карты побыту")
                .callbackData("edit_residenceCardExpireDate")
                .build();

        List<InlineKeyboardRow> inlineKeyboardRows = List.of(
                new InlineKeyboardRow(btnEmail, btnTelephone),
                new InlineKeyboardRow(btnName, btnSurnameCurrent),
                new InlineKeyboardRow(btnSurnamePrevious, btnSurnameMaiden),
                new InlineKeyboardRow(btnDateOfBirth, btnHeight),
                new InlineKeyboardRow(btnFathersName, btnMothersName),
                new InlineKeyboardRow(btnMothersSurnameMaiden, btnMaritalStatus),
                new InlineKeyboardRow(btnCityOfBirth, btnCountryOfBirth),
                new InlineKeyboardRow(btnNationality, btnCitizenship),
                new InlineKeyboardRow(btnPESEL, btnEducation),
                new InlineKeyboardRow(btnEyeColour, btnPostcode),
                new InlineKeyboardRow(btnCityOfResidence, btnStreetOfResidence),
                new InlineKeyboardRow(btnHouseNumber, btnFlatNumber),
                new InlineKeyboardRow(btnLastArrivalDate, btnPassportNumber),
                new InlineKeyboardRow(btnResidenceCard, btnResidenceCardExpireDate)
        );

        InlineKeyboardMarkup inlineKeyboardMarkup = new InlineKeyboardMarkup(inlineKeyboardRows);

        message.setReplyMarkup(inlineKeyboardMarkup);

        telegramClient.execute(message);
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

    private void startButton(SendMessage sendMessage) {
        // Create a keyboard

        // Create a list of keyboard rows
        List<KeyboardRow> keyboard = new ArrayList<>();
        // First keyboard row
        KeyboardRow keyboardFirstRow = new KeyboardRow();
        // Add buttons to the first keyboard row
        keyboardFirstRow.add(new KeyboardButton("/start"));
        // Add all of the keyboard rows to the list
        keyboard.add(keyboardFirstRow);
        ReplyKeyboardMarkup replyKeyboardMarkup = ReplyKeyboardMarkup.builder()
                .keyboard(keyboard)
                .selective(true)
                .resizeKeyboard(true)
                .oneTimeKeyboard(true)
                .build();
        sendMessage.setReplyMarkup(replyKeyboardMarkup);

        // and assign this list to our keyboard
        replyKeyboardMarkup.setKeyboard(keyboard);
    }

    private void commands(Long chatId) {

    }
    @SneakyThrows
    private void maritalStatus(Long chatId) {
        System.out.println("dfdfdfdfkgfgonbodfgnbodfgnbfg");
        SendMessage message = SendMessage.builder()
                .text("Укажите ваше семейное положение: ")
                .chatId(chatId)
                .build();

        var button1 = InlineKeyboardButton.builder()
                .text("Женат/Замужем")
                .callbackData("married")
                .build();
        var button2 = InlineKeyboardButton.builder()
                .text("Холост/Незамужем")
                .callbackData("unmarried")
                .build();
        var button3 = InlineKeyboardButton.builder()
                .text("Разведен/Разведена")
                .callbackData("divorced")
                .build();
        var button4 = InlineKeyboardButton.builder()
                .text("Вдовец/Вдова")
                .callbackData("widow")
                .build();

        List<InlineKeyboardRow> inlineKeyboardRows =
                List.of(new InlineKeyboardRow(button1),
                        new InlineKeyboardRow(button2),
                        new InlineKeyboardRow(button3),
                        new InlineKeyboardRow(button4)
                );

        InlineKeyboardMarkup inlineKeyboardMarkup = new InlineKeyboardMarkup(inlineKeyboardRows);

        message.setReplyMarkup(inlineKeyboardMarkup);

        telegramClient.execute(message);
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
