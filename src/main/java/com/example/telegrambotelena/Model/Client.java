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
    String lastArrivalDate;
    String passportNumber;
    String residenceCard;
    String residenceCardExpireDate;
    public Client(BotStage botStage) {
        this.botStage = botStage;
    }

}
