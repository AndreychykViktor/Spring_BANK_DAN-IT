package com.example.hm1.service.chat;

import com.example.hm1.dao.AccountRepo;
import com.example.hm1.dao.ChatMessageRepository;
import com.example.hm1.dao.ChatThreadRepository;
import com.example.hm1.dao.CustomerRepo;
import com.example.hm1.dao.UserRepository;
import com.example.hm1.dto.chat.ChatMessageRequest;
import com.example.hm1.dto.chat.ChatMessageResponse;
import com.example.hm1.dto.chat.ChatThreadResponse;
import com.example.hm1.entity.Account;
import com.example.hm1.entity.ChatMessage;
import com.example.hm1.entity.ChatThread;
import com.example.hm1.entity.Currency;
import com.example.hm1.entity.Customer;
import com.example.hm1.entity.Transaction;
import com.example.hm1.entity.User;
import com.example.hm1.service.ExchangeRateService;
import com.example.hm1.service.NotificationService;
import com.example.hm1.service.TransactionService;
import jakarta.persistence.EntityNotFoundException;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.time.LocalDateTime;
import java.util.Comparator;
import java.util.List;
import java.util.Optional;
import java.util.Set;
import java.util.stream.Collectors;

@Service
@Transactional
public class ChatServiceImpl implements ChatService {

    private final ChatThreadRepository threadRepository;
    private final ChatMessageRepository messageRepository;
    private final UserRepository userRepository;
    private final CustomerRepo customerRepo;
    private final AccountRepo accountRepo;
    private final ExchangeRateService exchangeRateService;
    private final TransactionService transactionService;
    private final NotificationService notificationService;
    private final ChatCommandParser commandParser = new ChatCommandParser();

    public ChatServiceImpl(ChatThreadRepository threadRepository,
                           ChatMessageRepository messageRepository,
                           UserRepository userRepository,
                           CustomerRepo customerRepo,
                           AccountRepo accountRepo,
                           ExchangeRateService exchangeRateService,
                           TransactionService transactionService,
                           NotificationService notificationService) {
        this.threadRepository = threadRepository;
        this.messageRepository = messageRepository;
        this.userRepository = userRepository;
        this.customerRepo = customerRepo;
        this.accountRepo = accountRepo;
        this.exchangeRateService = exchangeRateService;
        this.transactionService = transactionService;
        this.notificationService = notificationService;
    }

    @Override
    public ChatThreadResponse getOrCreateThread(User currentUser, User otherUser) {
        ChatThread thread = threadRepository.findThreadBetween(currentUser, otherUser)
                .orElseGet(() -> createThread(currentUser, otherUser));
        return ChatThreadResponse.from(thread);
    }

    @Override
    @Transactional(readOnly = true)
    public List<ChatThreadResponse> getThreadsForUser(User user) {
        return threadRepository.findAllByParticipant(user)
                .stream()
                .map(ChatThreadResponse::from)
                .collect(Collectors.toList());
    }

    @Override
    @Transactional(readOnly = true)
    public List<ChatMessageResponse> getMessagesForThread(Long threadId, User currentUser) {
        ChatThread thread = threadRepository.findById(threadId)
                .orElseThrow(() -> new EntityNotFoundException("Chat thread not found: " + threadId));
        validateParticipation(thread, currentUser);
        return messageRepository.findByThreadOrderByCreatedAtAsc(thread)
                .stream()
                .map(ChatMessageResponse::from)
                .collect(Collectors.toList());
    }

    @Override
    public ChatMessageResponse sendMessage(ChatMessageRequest request) {
        User sender = userRepository.findById(request.getSenderUserId())
                .orElseThrow(() -> new EntityNotFoundException("Sender not found: " + request.getSenderUserId()));

        ChatThread thread = resolveThread(request, sender);
        validateParticipation(thread, sender);

        ChatMessage message = new ChatMessage();
        message.setThread(thread);
        message.setSender(sender);
        message.setContent(request.getContent());
        message.setMessageType(ChatMessage.MessageType.TEXT);

        Optional<ChatCommandParser.CashCommand> cashCommand = commandParser.parse(request.getContent());
        if (cashCommand.isPresent()) {
            message.setMessageType(ChatMessage.MessageType.TRANSFER);
            message.setTransferAmount(cashCommand.get().getAmount());
            message.setTransferCurrency(cashCommand.get().getCurrency());
            message.setTransferStatus(ChatMessage.TransferStatus.PENDING);
        }

        message = messageRepository.save(message);

        if (message.getMessageType() == ChatMessage.MessageType.TRANSFER) {
            processTransfer(thread, message, sender, cashCommand.get());
        }

        thread.setUpdatedAt(LocalDateTime.now());
        threadRepository.save(thread);

        return ChatMessageResponse.from(message);
    }

    private ChatThread createThread(User currentUser, User otherUser) {
        ChatThread thread = new ChatThread(Set.of(currentUser, otherUser));
        return threadRepository.save(thread);
    }

    private ChatThread resolveThread(ChatMessageRequest request, User sender) {
        if (request.getThreadId() != null) {
            return threadRepository.findById(request.getThreadId())
                    .orElseThrow(() -> new EntityNotFoundException("Chat thread not found: " + request.getThreadId()));
        }

        if (request.getRecipientUserId() == null) {
            throw new IllegalArgumentException("Either threadId or recipientUserId must be provided");
        }

        User recipient = userRepository.findById(request.getRecipientUserId())
                .orElseThrow(() -> new EntityNotFoundException("Recipient not found: " + request.getRecipientUserId()));

        return threadRepository.findThreadBetween(sender, recipient)
                .orElseGet(() -> createThread(sender, recipient));
    }

    private void validateParticipation(ChatThread thread, User user) {
        boolean participant = thread.getParticipants()
                .stream()
                .anyMatch(p -> p.getId().equals(user.getId()));

        if (!participant) {
            throw new IllegalStateException("User is not a participant of the thread");
        }
    }

    private void processTransfer(ChatThread thread,
                                 ChatMessage message,
                                 User sender,
                                 ChatCommandParser.CashCommand command) {
        User recipient = thread.getParticipants().stream()
                .filter(user -> !user.getId().equals(sender.getId()))
                .findFirst()
                .orElseThrow(() -> new IllegalStateException("Unable to resolve recipient for transfer"));

        Currency commandCurrency = toCurrency(command.getCurrency());
        BigDecimal originalAmount = command.getAmount().setScale(2, RoundingMode.HALF_UP);

        Optional<Account> senderAccountOpt = findAccountForUser(sender, commandCurrency);
        if (senderAccountOpt.isEmpty()) {
            markTransferFailure(message, "У відправника немає рахунку в валюті " + commandCurrency.name());
            return;
        }

        Optional<Account> recipientAccountOpt = findAccountForUser(recipient, commandCurrency);
        if (recipientAccountOpt.isEmpty()) {
            markTransferFailure(message, "У отримувача немає рахунку в валюті " + commandCurrency.name());
            return;
        }

        Account senderAccount = senderAccountOpt.get();
        Account recipientAccount = recipientAccountOpt.get();

        try {
            BigDecimal debitAmount = convertAmount(originalAmount, commandCurrency, senderAccount.getCurrency())
                    .setScale(2, RoundingMode.HALF_UP);
            BigDecimal creditAmount = convertAmount(originalAmount, commandCurrency, recipientAccount.getCurrency())
                    .setScale(2, RoundingMode.HALF_UP);

            BigDecimal senderBalance = BigDecimal.valueOf(senderAccount.getBalance()).setScale(2, RoundingMode.HALF_UP);
            if (senderBalance.compareTo(debitAmount) < 0) {
                markTransferFailure(message, "Недостатньо коштів");
                return;
            }

            BigDecimal newSenderBalance = senderBalance.subtract(debitAmount).setScale(2, RoundingMode.HALF_UP);
            BigDecimal newRecipientBalance = BigDecimal.valueOf(recipientAccount.getBalance())
                    .setScale(2, RoundingMode.HALF_UP)
                    .add(creditAmount)
                    .setScale(2, RoundingMode.HALF_UP);

            senderAccount.setBalance(newSenderBalance.doubleValue());
            recipientAccount.setBalance(newRecipientBalance.doubleValue());
            accountRepo.save(senderAccount);
            accountRepo.save(recipientAccount);

            String summary = String.format(
                    "Chat transfer %s %.2f (debited %.2f %s, credited %.2f %s)",
                    commandCurrency.name(),
                    originalAmount,
                    debitAmount,
                    senderAccount.getCurrency().name(),
                    creditAmount,
                    recipientAccount.getCurrency().name()
            );

            Transaction senderTransaction = transactionService.createTransaction(
                    Transaction.TransactionType.TRANSFER,
                    debitAmount,
                    summary,
                    senderAccount,
                    recipientAccount,
                    senderAccount.getCustomer()
            );

            transactionService.createTransaction(
                    Transaction.TransactionType.TRANSFER,
                    creditAmount,
                    summary,
                    senderAccount,
                    recipientAccount,
                    recipientAccount.getCustomer()
            );

            message.setTransferAmount(originalAmount);
            message.setTransferCurrency(commandCurrency.name());
            message.setRelatedTransaction(senderTransaction);
            message.setTransferStatus(ChatMessage.TransferStatus.SUCCESS);
            messageRepository.save(message);

            notificationService.sendAccountUpdateNotification(
                    senderAccount.getNumber(),
                    "CHAT_TRANSFER_OUT",
                    debitAmount.doubleValue(),
                    senderAccount.getBalance()
            );
            notificationService.sendAccountUpdateNotification(
                    recipientAccount.getNumber(),
                    "CHAT_TRANSFER_IN",
                    creditAmount.doubleValue(),
                    recipientAccount.getBalance()
            );
        } catch (Exception ex) {
            markTransferFailure(message, ex.getMessage() != null ? ex.getMessage() : "Помилка переказу");
        }
    }

    private Optional<Account> findAccountForUser(User user, Currency preferredCurrency) {
        Customer customer = customerRepo.findByUserId(user.getId())
                .orElseThrow(() -> new IllegalStateException("Customer not found for user " + user.getId()));
        List<Account> accounts = accountRepo.findByCustomerId(customer.getId());
        if (accounts.isEmpty()) {
            return Optional.empty();
        }

        return accounts.stream()
                .filter(account -> account.getCurrency() == preferredCurrency)
                .min(Comparator.comparing(Account::getId));
    }

    private BigDecimal convertAmount(BigDecimal amount, Currency from, Currency to) {
        if (from == to) {
            return amount;
        }
        return exchangeRateService.convertAmount(amount, from, to);
    }

    private void markTransferFailure(ChatMessage message, String reason) {
        message.setTransferStatus(ChatMessage.TransferStatus.FAILED);
        if (reason != null && !reason.isBlank()) {
            message.setContent(message.getContent() + "\n(" + reason + ")");
        }
        messageRepository.save(message);
    }

    private Currency toCurrency(String code) {
        try {
            return Currency.valueOf(code.toUpperCase());
        } catch (IllegalArgumentException ex) {
            throw new IllegalArgumentException("Unsupported currency code: " + code);
        }
    }
}

