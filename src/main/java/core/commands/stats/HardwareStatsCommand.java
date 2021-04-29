package core.commands.stats;

import core.commands.abstracts.ConcurrentCommand;
import core.commands.utils.CommandCategory;
import core.parsers.NoOpParser;
import core.parsers.Parser;
import core.parsers.params.CommandParameters;
import core.services.ColorService;
import dao.BotStats;
import dao.ChuuService;
import net.dv8tion.jda.api.EmbedBuilder;
import net.dv8tion.jda.api.events.message.MessageReceivedEvent;

import javax.validation.constraints.NotNull;
import java.text.DecimalFormat;
import java.util.List;

public class HardwareStatsCommand extends ConcurrentCommand<CommandParameters> {
    public HardwareStatsCommand(ChuuService dao) {
        super(dao);
    }

    @Override
    protected CommandCategory initCategory() {
        return CommandCategory.BOT_STATS;
    }

    @Override
    public Parser<CommandParameters> initParser() {
        return NoOpParser.INSTANCE;
    }

    @Override
    public String getDescription() {
        return "Stats about the bot internals";
    }

    @Override
    public List<String> getAliases() {
        return List.of("botstats");
    }

    @Override
    public String getName() {
        return "Bot Stats";
    }

    @Override
    protected void onCommand(MessageReceivedEvent e, @NotNull CommandParameters params) {
        BotStats botStats = db.getBotStats();
        int shardTotal = e.getJDA().getShardInfo().getShardTotal();
        int mb = 1024 * 1024;
        Runtime instance = Runtime.getRuntime();
        long l = (instance.totalMemory() - instance.freeMemory()) / mb;
        EmbedBuilder embedBuilder = new EmbedBuilder()
                .setTitle(e.getJDA().getSelfUser().getName() + "'s stats")
                .setColor(ColorService.computeColor(e))
                .addField("**Registered users:**", "**" + botStats.userCount() + "**", true)
                .addField("**Number of Servers:**", "**" + botStats.guildCount() + "**", true)
                .addField("**Total setted users:**", "**" + botStats.setCount() + "**", true)
                .addField("**Artist Count:**", "**" + botStats.artistCount() + "**", true)
                .addField("**Album Count:**", "**" + botStats.albumCount() + "**", true)
                .addField("**Scrobble Count:**", "**" + botStats.scrobbledCount() + "**", true)
                .addField("**RYM Rating Count:**", "**" + botStats.rymCount() + "**", true)
                .addField("**Average RYM Rating:**", "**" + new DecimalFormat("#0.##").format(botStats.rymAvg() / 2f) + "**", true)
                .addField("**Recommendations Given:**", "**" + botStats.recCount() + "**", true)
                .addField("**Count of Random Urls:**", "**" + botStats.randomCount() + "**", true)
                .addField("**Total Artist Images:**", "**" + botStats.imageCount() + "**", true)
                .addField("**Number of Votes casted:**", "**" + botStats.voteCount() + "**", true)
                .addField("**Memory usage:**", "**" + l + " MB**", true)
                .addField("**Total Number of api requests:**", "**" + botStats.apiCount() + "**", true)
                .addField("**Shards Count:**", "**" + (shardTotal) + "**", true)
                .setThumbnail(e.getJDA().getSelfUser().getAvatarUrl());
        e.getChannel().sendMessage(embedBuilder.build()).queue();


    }

}
