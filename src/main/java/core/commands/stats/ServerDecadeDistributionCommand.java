package core.commands.stats;

import core.commands.Context;
import core.commands.abstracts.ConcurrentCommand;
import core.commands.utils.ChuuEmbedBuilder;
import core.commands.utils.CommandCategory;
import core.commands.utils.CommandUtil;
import core.otherlisteners.Reactionary;
import core.parsers.NoOpParser;
import core.parsers.Parser;
import core.parsers.params.CommandParameters;
import dao.ServiceView;

import javax.annotation.Nonnull;
import java.util.Comparator;
import java.util.List;
import java.util.Map;

public class ServerDecadeDistributionCommand extends ConcurrentCommand<CommandParameters> {


    public ServerDecadeDistributionCommand(ServiceView dao) {
        super(dao);
        respondInPrivate = true;

    }

    @Override
    protected CommandCategory initCategory() {
        return CommandCategory.SERVER_STATS;
    }

    @Override
    public Parser<CommandParameters> initParser() {
        return NoOpParser.INSTANCE;

    }

    @Override
    public String getDescription() {
        return "Which decade are your albums from?";
    }

    @Override
    public List<String> getAliases() {
        return List.of("serverdecades", "serverdecade", "sdecade", "sdecades");
    }

    @Override
    public String slashName() {
        return "decades";
    }

    @Override
    public String getName() {
        return "Server album decades";
    }


    @Override
    protected void onCommand(Context e, @Nonnull CommandParameters params) {
        var counts = db.getGuildDecades(e.getGuild().getIdLong());
        List<String> lines = counts.entrySet().stream().sorted(Map.Entry.comparingByValue((Comparator.reverseOrder()))).map(t ->
                ". **%ds**: %d %s%n".formatted(CommandUtil.getDecade(t.getKey().getValue()), t.getValue(), CommandUtil.singlePlural(t.getValue(), "album", "albums"))
        ).toList();

        StringBuilder a = new StringBuilder();
        for (int i = 0; i < lines.size() && i < 10; i++) {
            String s = lines.get(i);
            a.append(i + 1).append(s);
        }

        var embedBuilder = new ChuuEmbedBuilder(e)
                .setDescription(a)
                .setAuthor(String.format("%s's years", e.getGuild().getName()), null, e.getGuild().getIconUrl())
                .setFooter("%s has albums from %d different %s".formatted(CommandUtil.stripEscapedMarkdown(e.getGuild().getName()), counts.size(), CommandUtil.singlePlural(counts.size(), "decade", "decades")), null);

        e.sendMessage(embedBuilder.build()).queue(m ->
                new Reactionary<>(lines, m, 10, embedBuilder));
    }


}
