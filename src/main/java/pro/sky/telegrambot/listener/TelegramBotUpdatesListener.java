package pro.sky.telegrambot.listener;

import com.pengrad.telegrambot.TelegramBot;
import com.pengrad.telegrambot.UpdatesListener;
import com.pengrad.telegrambot.model.Message;
import com.pengrad.telegrambot.model.Update;
import com.pengrad.telegrambot.request.SendMessage;
import com.pengrad.telegrambot.response.SendResponse;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Service;
import pro.sky.telegrambot.entity.NotificationTask;
import pro.sky.telegrambot.service.NotificationTaskService;

import javax.annotation.PostConstruct;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.time.temporal.ChronoUnit;
import java.util.List;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

@Service
public class TelegramBotUpdatesListener implements UpdatesListener {

    private final Logger logger = LoggerFactory.getLogger(TelegramBotUpdatesListener.class);
    private static final Pattern PATTERN = Pattern.compile("([0-9.:\\s]{16})(\\s)([\\W+]+)");

    private final NotificationTaskService notificationTaskService;
    private final TelegramBot telegramBot;


    public TelegramBotUpdatesListener(NotificationTaskService notificationTaskService, TelegramBot telegramBot) {
        this.notificationTaskService = notificationTaskService;
        this.telegramBot = telegramBot;
    }



    @PostConstruct
    public void init() {
        telegramBot.setUpdatesListener(this);
    }

    @Override
    public int process(List<Update> updates) {
        updates.forEach(update -> {
            logger.info("Processing update: {}", update);
            String text = update.message().text();
            Long chatId = update.message().chat().id();
            if (text.equals("/start")) {
                SendMessage message = new SendMessage(chatId, "Привет!");
                SendResponse response = telegramBot.execute(message);
            }

            createNewDBRecord(updates);

            notificationTaskOnScheduledTime();
            });
        return UpdatesListener.CONFIRMED_UPDATES_ALL;
    }

    private void createNewDBRecord(List<Update> updates) {
        updates
                .stream()
                .map(Update::message)
                .filter(message -> (checkMessagePattern(message.text())))
                .forEach(message -> notificationTaskService.createNotificationTask(createNotificationTaskFromMessageText(message)));
    }

    @Scheduled(cron = "0 0/1 * * * *")
    private void notificationTaskOnScheduledTime() {
        if(!notificationTaskService
                .getNotificationTask(LocalDateTime.now().truncatedTo(ChronoUnit.MINUTES)).isEmpty()) {
            notificationTaskService
                    .getNotificationTask(LocalDateTime.now().truncatedTo(ChronoUnit.MINUTES))
                    .forEach(notificationTask -> {
                        SendMessage message = new SendMessage(notificationTask.getChatId(), notificationTask.getMessage());
                        SendResponse response = telegramBot.execute(message);
                    });
        }
    }

    private boolean checkMessagePattern(String text) {
        Matcher matcher = PATTERN.matcher(text);
        return matcher.matches();
    }

    private NotificationTask createNotificationTaskFromMessageText(Message message) {
        String text = message.text();
        Matcher matcher = PATTERN.matcher(text);
        String date = null;
        String task = null;

        if (matcher.matches()) {
            date = matcher.group(1);
            task = matcher.group(3);
        }

        LocalDateTime localDateTime = LocalDateTime.parse(date, DateTimeFormatter.ofPattern("dd.MM.yyyy HH:mm"));
        return new NotificationTask(message.chat().id(), task, localDateTime);
    }


}
