package core.commands.crowns;

import core.commands.abstracts.LeaderboardCommand;
import core.commands.utils.CommandCategory;
import core.parsers.NoOpParser;
import core.parsers.Parser;
import core.parsers.params.CommandParameters;
import dao.ChuuService;
import dao.entities.LbEntry;

import java.util.List;

public class UniqueAlbumLeaderboardCommand extends LeaderboardCommand<CommandParameters> {
    public UniqueAlbumLeaderboardCommand(ChuuService dao) {
        super(dao);
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
    public String getEntryName(CommandParameters params) {
        return "unique albums";
    }

    @Override
    public String getDescription() {
        return ("Unique album leaderboard in guild");
    }

    @Override
    public List<String> getAliases() {
        return List.of("uniquealbumlb", "uniquealblb");
    }

    @Override
    public List<LbEntry> getList(CommandParameters parameters) {
        return db.getUniqueAlbumLeaderboard(parameters.getE().getGuild().getIdLong());
    }

    @Override
    public String getName() {
        return "Unique albums leaderboard";
    }

}
