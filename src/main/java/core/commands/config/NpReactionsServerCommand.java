package core.commands.config;

import core.commands.abstracts.ConcurrentCommand;
import core.commands.utils.CommandCategory;
import core.commands.utils.ReactValidation;
import core.parsers.EmojeParser;
import core.parsers.OptionalEntity;
import core.parsers.Parser;
import core.parsers.params.EmotiParameters;
import dao.ChuuService;
import net.dv8tion.jda.api.Permission;
import net.dv8tion.jda.api.events.message.MessageReceivedEvent;

import javax.validation.constraints.NotNull;
import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.atomic.AtomicLong;
import java.util.stream.Collectors;

public class NpReactionsServerCommand extends ConcurrentCommand<EmotiParameters> {
    public NpReactionsServerCommand(ChuuService dao) {
        super(dao);
    }

    @Override
    protected CommandCategory initCategory() {
        return CommandCategory.CONFIGURATION;
    }

    @Override
    public Parser<EmotiParameters> initParser() {
        EmojeParser emojeParser = new EmojeParser();
        emojeParser.addOptional(new OptionalEntity("check", "check the current reactions"));
        return emojeParser;
    }

    @Override
    public String getDescription() {
        return "Sets reactions for the whole server on your nps";
    }

    @Override
    public List<String> getAliases() {
        return List.of("serverreactions", "sreacts", "sreact", "serverreacts");
    }

    @Override
    public String getName() {
        return "Server reacts";
    }

    @Override
    protected void onCommand(MessageReceivedEvent e, @NotNull EmotiParameters params) {
        if (params.hasOptional("check")) {
            List<String> serverReactions = getService().getServerReactions(e.getGuild().getIdLong());
            if (serverReactions.isEmpty()) {
                sendMessageQueue(e, "Don't have any reaction set");
                return;
            }
            List<String> displaying = serverReactions.stream().map(EmotiParameters.Emotable::toDisplay).collect(Collectors.toList());

            sendMessageQueue(e, "Have this reactions: " + String.join(" ", displaying));
            return;

        }
        if (e.getMember() == null || !e.getMember().hasPermission(Permission.MESSAGE_MANAGE)) {
            sendMessageQueue(e, "Only server mods can use this command");
            return;
        }
        if (params.getEmotis().isEmpty()) {
            sendMessageQueue(e, "Clearing reactions");
            getService().insertServerReactions(e.getGuild().getIdLong(), new ArrayList<>());
            return;
        }

        AtomicLong messageId = new AtomicLong();
        if (!e.getGuild().getSelfMember().hasPermission(Permission.MESSAGE_ADD_REACTION)) {
            sendMessageQueue(e, "I dont have permissions to add reactions!");
            return;
        }

        if (params.hasEmotes()) {
            sendMessage(e, "Checking permissions...").
                    flatMap(ReactValidation.sender.apply(messageId, params.getEmotes()))
                    .flatMap(aVoid -> e.getChannel().retrieveMessageById(messageId.get()))
                    .queue(ReactValidation.getMessageConsumer(e, params, (emotes) -> {
                        List<String> content = params.getEmotis().stream().filter(emotable -> {
                            if (emotable instanceof EmotiParameters.CustomEmote emote) {
                                return !emotes.contains(emote.entity());
                            }
                            return true;
                        }).map(EmotiParameters.Emotable::getContent).collect(Collectors.toList());
                        if (content.isEmpty()) {
                            sendMessageQueue(e, "Didn't have any reaction to add :(");
                        } else {
                            sendMessageQueue(e, "Will set the following reactions: " + content.stream().map(EmotiParameters.Emotable::toDisplay).collect(Collectors.joining(" ")));
                            getService().insertServerReactions(e.getGuild().getIdLong(), content);
                        }
                    }));
        } else {
            if (!params.hasEmojis()) {
                String collect = params.getEmojis().stream().map(EmotiParameters.Emotable::toDisplay).collect(Collectors.joining(" "));
                sendMessageQueue(e, "Will set the following reactions: " + collect);
                getService().insertServerReactions(e.getGuild().getIdLong(), params.getEmojis());
            }
        }
    }
}