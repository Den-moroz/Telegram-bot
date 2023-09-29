package com.example.telegram_bot.service;

import com.example.telegram_bot.config.TelegramBotConfig;
import com.example.telegram_bot.model.Ads;
import com.example.telegram_bot.model.User;
import com.example.telegram_bot.repository.AdsRepository;
import com.example.telegram_bot.repository.UserRepository;
import com.vdurmont.emoji.EmojiParser;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;
import org.telegram.telegrambots.bots.TelegramLongPollingBot;
import org.telegram.telegrambots.meta.api.methods.commands.SetMyCommands;
import org.telegram.telegrambots.meta.api.methods.send.SendMessage;
import org.telegram.telegrambots.meta.api.methods.updatingmessages.EditMessageText;
import org.telegram.telegrambots.meta.api.objects.Chat;
import org.telegram.telegrambots.meta.api.objects.Message;
import org.telegram.telegrambots.meta.api.objects.Update;
import org.telegram.telegrambots.meta.api.objects.commands.BotCommand;
import org.telegram.telegrambots.meta.api.objects.commands.scope.BotCommandScopeDefault;
import org.telegram.telegrambots.meta.api.objects.replykeyboard.InlineKeyboardMarkup;
import org.telegram.telegrambots.meta.api.objects.replykeyboard.ReplyKeyboardMarkup;
import org.telegram.telegrambots.meta.api.objects.replykeyboard.buttons.InlineKeyboardButton;
import org.telegram.telegrambots.meta.api.objects.replykeyboard.buttons.KeyboardRow;
import org.telegram.telegrambots.meta.exceptions.TelegramApiException;
import java.sql.Timestamp;
import java.util.ArrayList;
import java.util.List;

@Component
public class TelegramBotService extends TelegramLongPollingBot {
    private static final String HELP_TEXT = "This bot created for whether application. "
            + "You can execute any command from the main menu. "
            + "Type /start to start communication with bot";
    private static final String ID_FOR_NO_BUTTON = "NO_BUTTON";
    private static final String ID_FOR_YES_BUTTON = "YES_BUTTON";
    private static final String BASIC_ERROR = "Something went wrong when trying to execute message";
    private final TelegramBotConfig config;
    private final UserRepository userRepository;
    private final AdsRepository adsRepository;

    public TelegramBotService(TelegramBotConfig config, UserRepository userRepository, AdsRepository adsRepository) {
        this.config = config;
        this.userRepository = userRepository;
        this.adsRepository = adsRepository;
        List<BotCommand> commands = new ArrayList<>();
        commands.add(new BotCommand("/start", "Get a start message"));
        commands.add(new BotCommand("/mydata", "Get your data stored"));
        commands.add(new BotCommand("/deletedata", "Delete my data"));
        commands.add(new BotCommand("/help", "Info how to use this bot"));
        commands.add(new BotCommand("/settings", "Set your preferences"));
        try {
            execute(new SetMyCommands(commands, new BotCommandScopeDefault(), null));
        } catch (TelegramApiException e) {
            System.out.println("Something wen't wrong when try to add a list of commands" + e);
        }
    }

    @Override
    public String getBotUsername() {
        return config.getBotName();
    }

    @Override
    public String getBotToken() {
        return config.getToken();
    }

    @Override
    public void onUpdateReceived(Update update) {
        if (update.hasMessage() && update.getMessage().hasText()) {
            String message = update.getMessage().getText();
            long chatId = update.getMessage().getChatId();

            if (message.contains("/send") && config.getAdminId() == chatId) {
                String textToSend = EmojiParser.parseToUnicode(message.substring(message.indexOf(" ")));
                sendMessageForAll(textToSend, userRepository.findAll());
            } else {
                switch (message) {
                    case "/start":
                        registerUser(update.getMessage());
                        sendMessage(chatId, update.getMessage().getChat().getFirstName());
                        break;
                    case "/help":
                        prepareAndSendMessage(chatId, HELP_TEXT);
                        break;
                    case "/register":
                        register(chatId);
                        break;
                    default:
                        prepareAndSendMessage(chatId, "Sorry, command was not found");
                }
            }
        } else if (update.hasCallbackQuery()) {
            long messageId = update.getCallbackQuery().getMessage().getMessageId();
            long chatId = update.getCallbackQuery().getMessage().getChatId();
            String data = update.getCallbackQuery().getData();

            if (data.equals(ID_FOR_YES_BUTTON)) {
                String text = "You pressed yes button";
                executeEditMessageText(chatId, messageId, text);
            } else if (data.equals(ID_FOR_NO_BUTTON)) {
                String text = "You pressed no button";
                executeEditMessageText(chatId, messageId, text);
            }
        }
    }

    @Scheduled(cron = "0 * * * * *")
    private void sendAds() {
        var ads = adsRepository.findAll();
        var users = userRepository.findAll();
        for (Ads ad : ads) {
            sendMessageForAll(ad.getAd(), users);
        }

    }
    private void registerUser(Message message) {
        if (userRepository.findById(message.getChatId()).isEmpty()) {
            long chatId = message.getChatId();
            Chat chat = message.getChat();

            User newUser = new User();
            newUser.setChatId(chatId);
            newUser.setFirstName(chat.getFirstName());
            newUser.setLastName(chat.getLastName());
            newUser.setUserName(chat.getUserName());
            newUser.setRegisteredAt(new Timestamp(System.currentTimeMillis()));
            userRepository.save(newUser);
        }
    }

    private void sendMessage(long chatId, String firstName) {
        startCommandReceived(chatId, firstName);
        SendMessage message = new SendMessage();
        message.setChatId(chatId);

        ReplyKeyboardMarkup keyboardMarkup = new ReplyKeyboardMarkup();
        List<KeyboardRow> keyboardRows = new ArrayList<>();

        KeyboardRow keyboardRow = new KeyboardRow();
        keyboardRow.add("weather");
        keyboardRow.add("Get random joke)");

        keyboardRows.add(keyboardRow);

        keyboardRow = new KeyboardRow();
        keyboardRow.add("register");
        keyboardRow.add("check my data");
        keyboardRow.add("delete my data");

        keyboardRows.add(keyboardRow);
        keyboardMarkup.setKeyboard(keyboardRows);
        message.setReplyMarkup(keyboardMarkup);
        executeMessage(message);
    }

    private void startCommandReceived(long chatId, String firstName) {
        String message = EmojiParser.parseToUnicode("Hi " + firstName + " , this is whether telegram bot, enjoy)" + ":tada:");
        prepareAndSendMessage(chatId, message);
    }

    private void register(long chatId) {
        SendMessage message = new SendMessage();
        message.setChatId(chatId);
        message.setText("Do you really want to register?");

        InlineKeyboardMarkup inlineKeyboardMarkup = new InlineKeyboardMarkup();
        List<List<InlineKeyboardButton>> inlineRows = new ArrayList<>();
        List<InlineKeyboardButton> rowInLine = new ArrayList<>();
        InlineKeyboardButton yesButton = new InlineKeyboardButton();
        yesButton.setText("Yes");
        yesButton.setCallbackData(ID_FOR_YES_BUTTON);

        InlineKeyboardButton noButton = new InlineKeyboardButton();
        noButton.setText("No");
        noButton.setCallbackData(ID_FOR_NO_BUTTON);

        rowInLine.add(yesButton);
        rowInLine.add(noButton);

        inlineRows.add(rowInLine);
        inlineKeyboardMarkup.setKeyboard(inlineRows);
        message.setReplyMarkup(inlineKeyboardMarkup);

        executeMessage(message);
    }

    private void executeMessage(SendMessage message) {
        try {
            execute(message);
        } catch (TelegramApiException e) {
            System.out.println(BASIC_ERROR + e);
        }
    }

    private void executeEditMessageText(long chatId, long messageId, String text) {
        EditMessageText message = new EditMessageText();
        message.setChatId(chatId);
        message.setText(text);
        message.setMessageId((int) messageId);

        try {
            execute(message);
        } catch (TelegramApiException e) {
            System.out.println(BASIC_ERROR + e);
        }
    }

    private void prepareAndSendMessage(long chatId, String textToSend) {
        SendMessage message = new SendMessage();
        message.setChatId(chatId);
        message.setText(textToSend);
        executeMessage(message);
    }

    private void sendMessageForAll(String textToSend, List<User> users) {
        for (User user : users) {
            prepareAndSendMessage(user.getChatId(), textToSend);
        }
    }
}
