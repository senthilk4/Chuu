/*
 * Copyright 2015-2016 Austin Keener
 * <p>
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 * <p>
 * http://www.apache.org/licenses/LICENSE-2.0
 * <p>
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */
package core.commands.config;

import core.apis.lyrics.TextSplitter;
import core.commands.Context;
import core.commands.abstracts.ConcurrentCommand;
import core.commands.abstracts.MyCommand;
import core.commands.utils.ChuuEmbedBuilder;
import core.commands.utils.CommandCategory;
import core.parsers.HelpParser;
import core.parsers.Parser;
import core.parsers.params.WordParameter;
import core.parsers.utils.OptionalEntity;
import dao.ServiceView;
import net.dv8tion.jda.api.EmbedBuilder;
import net.dv8tion.jda.api.MessageBuilder;
import net.dv8tion.jda.api.entities.Message;
import net.dv8tion.jda.api.entities.MessageChannel;
import net.dv8tion.jda.api.entities.MessageEmbed;
import net.dv8tion.jda.api.requests.RestAction;

import javax.annotation.Nonnull;
import java.util.*;
import java.util.stream.Collectors;

public class HelpCommand extends ConcurrentCommand<WordParameter> {
    private static final String NO_NAME = "No name provided for this command. Sorry!";
    private static final String NO_DESCRIPTION = "No description has been provided for this command. Sorry!";
    private static final String NO_USAGE = "No usage instructions have been provided for this command. Sorry!";

    private final TreeMap<CommandCategory, SortedSet<MyCommand<?>>> categoryMap;
    private final Comparator<MyCommand<?>> myCommandComparator = (c1, c2) -> {
        String s = c1.getAliases().get(0);
        String s1 = c2.getAliases().get(0);
        return s.compareToIgnoreCase(s1);
    };


    public HelpCommand(ServiceView dao) {
        super(dao);
        categoryMap = new TreeMap<>(Comparator.comparingInt(CommandCategory::getOrder));
        registerCommand(this);
    }


    @Override
    protected CommandCategory initCategory() {
        return CommandCategory.STARTING;
    }

    @Override
    public Parser<WordParameter> initParser() {
        return new HelpParser(new OptionalEntity("all", "DM you a list of all the commands with an explanation"));
    }

    public MyCommand<?> registerCommand(MyCommand<?> command) {
        SortedSet<MyCommand<?>> myCommands = categoryMap.get(command.getCategory());
        if (myCommands == null) {
            TreeSet<MyCommand<?>> set = new TreeSet<>(myCommandComparator);
            set.add(command);
            categoryMap.put(command.getCategory(), set);
        } else {
            myCommands.add(command);
        }
        return command;
    }

    @Override
    public String getDescription() {
        return "Command that helps to use all other commands!";
    }

    @Override
    public String getUsageInstructions() {
        return
                """
                        help   **OR**  help *<command>*
                        help - returns the list of commands along with a simple description of each.
                        help <command> - returns the name, description, aliases and usage information of a command.
                           - This can use the aliases of a command as input as well.
                        Example: !help chart""";
    }

    @Override
    public List<String> getAliases() {
        return List.of("help", "h");
    }

    @Override
    public String getName() {
        return "Help Command";
    }

    @Override
    protected void onCommand(Context e, @Nonnull WordParameter params) {
        Character prefix = e.getPrefix();
        if (params.hasOptional("all")) {

            e.sendMessage(new MessageBuilder()
                            .append(e.getAuthor())
                            .append(": Help information was sent as a private message.")
                            .build(), e.getAuthor())
                    .flatMap(z -> e.getAuthor().openPrivateChannel())
                    .queue(privateChannel -> sendPrivate(privateChannel, e));
            return;
        }
        if (params.getWord() == null) {
            sendEmbed(e);
            return;

        }
        doSend(params.getWord(), e, prefix);
    }

    public void sendPrivate(MessageChannel channel, Context e) {
        Character prefix = e.getPrefix();
        StringBuilder s = new StringBuilder();
        List<RestAction<Message>> messageActions = new ArrayList<>();
        s.append("A lot of commands accept different time frames which are the following:\n")
                .append(" d: Day \n")
                .append(" w: Week \n")
                .append(" m: Month \n")
                .append(" q: quarter \n")
                .append(" s: semester \n")
                .append(" y: year \n")
                .append(" a: alltime \n")
                .append("\n")
                .append("You can use ").append(prefix).append(getAliases().get(0))
                .append(" + other command to get a exact description of what a command accepts\n")
                .append("\n")
                .append("The following commands are supported by the bot\n");

        for (Map.Entry<CommandCategory, SortedSet<MyCommand<?>>> a : categoryMap.entrySet()) {
            CommandCategory key = a.getKey();
            if (key == CommandCategory.MUSIC || key == CommandCategory.SCROBBLING) {
                continue;
            }
            Collection<MyCommand<?>> commandList = a.getValue();
            s.append("\n__**").append(key.toString().replaceAll("_", " ")).append(":**__ _").append(key.getDescription()).append("_\n");

            for (MyCommand<?> c : commandList) {
                if (s.length() > 1800) {
                    messageActions.add(channel.sendMessage(new MessageBuilder()
                            .append(s.toString())
                            .build()));
                    s = new StringBuilder();
                }
                String description = c.getDescription();
                description = (description == null || description.isEmpty()) ? NO_DESCRIPTION : description;

                s.append("**").append(prefix).append(c.getAliases().get(0)).append("** - ");
                s.append(description).append("\n");
            }
        }

        messageActions.add(channel.sendMessage(new MessageBuilder()
                .append(s.toString())
                .build()));

        RestAction.allOf(messageActions).queue();
    }

    public void sendEmbed(Context e) {
        EmbedBuilder embedBuilder = new ChuuEmbedBuilder(e);
        Character correspondingPrefix = e.getPrefix();
        for (Map.Entry<CommandCategory, SortedSet<MyCommand<?>>> a : categoryMap.entrySet()) {
            StringBuilder s = new StringBuilder();
            CommandCategory key = a.getKey();
            Collection<MyCommand<?>> commandList = a.getValue();
            s.append("\n__**").append(key.toString().replaceAll("_", " ")).append(":**__ _").append(key.getDescription()).append("_\n");
            String line = commandList.stream().map(x -> "*" + correspondingPrefix + x.getAliases().get(0) + "*").collect(Collectors.joining(", "));
            embedBuilder.addField(new MessageEmbed.Field(s.toString(), line, false));
        }
        embedBuilder.setFooter(correspondingPrefix + "help \"command\" for the explanation of one command.\n" + correspondingPrefix + "help --all for the whole help message")
                .setTitle("Commands");
        e.sendMessage(embedBuilder.build()).queue();
    }

    private void doSend(String command, Context e, Character prefix) {
        List<MyCommand<?>> values = categoryMap.values().stream().flatMap(Collection::stream).toList();
        for (MyCommand<?> c : values) {
            if (c.getAliases().contains(command.toLowerCase())) {
                String name = c.getName();
                String description = c.getDescription();
                String usageInstructions = c.getUsageInstructions();

                name = (name == null || name.isEmpty()) ? NO_NAME : name;
                description = (description == null || description.isEmpty()) ? NO_DESCRIPTION : description;
                usageInstructions = (usageInstructions == null || usageInstructions
                        .isEmpty()) ? NO_USAGE : usageInstructions;
                boolean resend = false;
                String realUsageInstructions = usageInstructions;
                String remainingUsageInstructions = null;
                List<String> pagees = TextSplitter.split(realUsageInstructions, 1600);
                e.sendMessage("**Name:** " + name + "\n" +
                              "**Description:** " + description + "\n" +
                              "**Aliases:** " + prefix +
                              String.join(", " + prefix, c.getAliases()) + "\n" +
                              "**Usage:** " +
                              prefix + pagees.get(0)).queue(x -> {
                    for (int i = 1; i < pagees.size(); i++) {
                        e.sendMessage(EmbedBuilder.ZERO_WIDTH_SPACE + pagees.get(i)).queue();
                    }
                });
                return;
            }
        }
        e.sendMessage("The provided command '**" + command +
                      "**' does not exist. Use " + prefix + "help to list all commands.").queue();
    }


}
