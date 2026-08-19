package com.example.telegrambotelena.Model;

import com.example.telegrambotelena.BotStageEnum.BotStage;
import jakarta.persistence.Entity;
import lombok.*;
import org.springframework.stereotype.Component;

import java.time.LocalDate;

@Builder
@Component
@Data
@AllArgsConstructor
@NoArgsConstructor
public class Client {
    BotStage botStage;
    String email;
    String name;
    String surnameCurrent;
    String surnamePrevious;
    String surnameMaiden;
    LocalDate dateOfBirth;
    String fathersName;
    String mothersName;
    String mothersSurnameMaiden;
    String maritalStatus;
    String cityOfBirth;
    String nationality;
    String citizenship;
    String countryOfBirth;
    String height;
    String PESEL;
    String education;
    String eyeColour;
    String cityOfResidence;
    String streetOfResidence;
    String houseNumber;
    String flatNumber;
    String postcode;
    String telephone;
    LocalDate lastArrivalDate;
    String passportNumber;
    String residenceCard;
    String residenceCardExpireDate;
    public Client(BotStage botStage) {
        this.botStage = botStage;
    }

    @Override
    public String toString() {
        return """
    АНКЕТА КЛИЕНТА
    ----------------------------------------
    ЛИЧНЫЕ ДАННЫЕ:
    - Имя: %s
    - Текущая фамилия: %s
    - Предыдущая фамилия: %s
    - Девичья фамилия: %s
    - Дата рождения: %s
    - Семейное положение: %s
    - Рост: %s см
    - Цвет глаз: %s
    - Образование: %s
    - Email: %s
    
    ПРОИСХОЖДЕНИЕ:
    - Гражданство: %s
    - Национальность: %s
    - Страна рождения: %s
    - Город рождения: %s
    
    РОДИТЕЛИ:
    - Имя отца: %s
    - Имя матери: %s
    - Девичья фамилия матери: %s
    
    АДРЕС ПРОЖИВАНИЯ:
    - Индекс: %s
    - Город: %s
    - Улица: %s
    - Дом: %s %s
    
    КОНТАКТЫ И СВЯЗЬ:
    - Телефон: %s
    
    ДОКУМЕНТЫ И СТАТУС:
    - Номер PESEL: %s
    - Номер паспорта: %s
    - Карта побыту: %s
    - Срок карты побыту: %s
    - Дата последнего въезда: %s
    ----------------------------------------
    """.formatted(
                name != null ? name : "Не указано",
                surnameCurrent != null ? surnameCurrent : "Не указана",
                surnamePrevious != null ? surnamePrevious : "Нет",
                surnameMaiden != null ? surnameMaiden : "Нет",
                dateOfBirth != null ? dateOfBirth.toString() : "Не указана",
                maritalStatus != null ? maritalStatus : "Не указано",
                height != null ? height : "Не указан",
                eyeColour != null ? eyeColour : "Не указан",
                education != null ? education : "Не указано",
                email != null ? email : "Не указан",
                citizenship != null ? citizenship : "Не указано",
                nationality != null ? nationality : "Не указана",
                countryOfBirth != null ? countryOfBirth : "Не указана",
                cityOfBirth != null ? cityOfBirth : "Не указан",
                fathersName != null ? fathersName : "Не указано",
                mothersName != null ? mothersName : "Не указано",
                mothersSurnameMaiden != null ? mothersSurnameMaiden : "Не указана",
                postcode != null ? postcode : "Не указан",
                cityOfResidence != null ? cityOfResidence : "Не указан",
                streetOfResidence != null ? streetOfResidence : "Не указана",
                houseNumber != null ? houseNumber : "Не указан",
                flatNumber != null && !flatNumber.isEmpty() ? ", кв. " + flatNumber : "",
                telephone != null ? telephone : "Не указан",
                PESEL != null ? PESEL : "Не указан",
                passportNumber != null ? passportNumber : "Не указан",
                residenceCard != null ? residenceCard : "Не указана",
                residenceCardExpireDate != null ? residenceCardExpireDate : "Не указан",
                lastArrivalDate != null ? lastArrivalDate.toString() : "Не указана"
        );
    }

}
