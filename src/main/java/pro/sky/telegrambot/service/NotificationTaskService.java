package pro.sky.telegrambot.service;

import org.springframework.stereotype.Service;
import pro.sky.telegrambot.entity.NotificationTask;
import pro.sky.telegrambot.repository.NotificationTaskRepository;

import java.time.LocalDateTime;
import java.util.Collection;

@Service
public class NotificationTaskService {
    private final NotificationTaskRepository notificationTaskRepository;

    public NotificationTaskService(NotificationTaskRepository notificationTaskRepository) {
        this.notificationTaskRepository = notificationTaskRepository;
    }

    public void createNotificationTask(NotificationTask notificationTask) {
        if (notificationTask == null) {
            throw new IllegalArgumentException("Argument is null");
        }
        notificationTaskRepository.save(notificationTask);
    }

    public Collection<NotificationTask> getNotificationTask(LocalDateTime localDateTime) {
        return notificationTaskRepository.findNotificationTasksByDate(localDateTime);
    }
}
