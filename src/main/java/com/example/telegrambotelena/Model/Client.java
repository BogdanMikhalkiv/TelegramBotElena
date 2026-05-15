package com.example.telegrambotelena.Model;

import com.example.telegrambotelena.BotStageEnum.BotStage;
import jakarta.persistence.Entity;
import lombok.*;
import org.springframework.stereotype.Component;

@Builder
@Component
@Data
@AllArgsConstructor
@NoArgsConstructor
@ToString
public class Client {
    BotStage botStage;
    String name;
    String surname;

    public Client(BotStage botStage) {
        this.botStage = botStage;
    }

}
