package no.runsafe.warpdrive.commands;

import no.runsafe.framework.api.IScheduler;
import no.runsafe.framework.api.command.argument.IArgumentList;
import no.runsafe.framework.api.command.argument.ITabComplete;
import no.runsafe.framework.api.command.argument.OptionalArgument;
import no.runsafe.framework.api.player.IPlayer;
import no.runsafe.warpdrive.Engine;
import no.runsafe.warpdrive.database.WarpRepository;
import org.apache.commons.lang.StringUtils;

import java.util.List;

public class Home extends PlayerTeleportCommand
{
	public Home(WarpRepository repository, IScheduler scheduler, Engine engine)
	{
		super("home", "Teleports you to a home location", "runsafe.home.use", scheduler, engine, new HomeArgument(repository));
		warpRepository = repository;
	}

	public static class HomeArgument extends OptionalArgument implements ITabComplete
	{
		public HomeArgument(WarpRepository warpRepository)
		{
			super("home");
			this.warpRepository = warpRepository;
		}

		@Override
		public List<String> getAlternatives(IPlayer player, String arg)
		{
			return warpRepository.GetPrivateList(player.getName());
		}

		private final WarpRepository warpRepository;
	}

	@Override
	public PlayerTeleportCommand.PlayerTeleport OnAsyncExecute(IPlayer player, IArgumentList params)
	{
		PlayerTeleport target = new PlayerTeleport();
		target.player = player;
		String home;
		if (params.get("home") == null)
		{
			List<String> homes = warpRepository.GetPrivateList(player.getName());
			if (homes.isEmpty())
			{
				target.message = "You do not have any homes set.";
				return target;
			}
			if (homes.size() == 1)
				home = homes.get(0);
			else
			{
				target.message = String.format("Homes: %s", StringUtils.join(homes, ", "));
				return target;
			}
		}
		else
			home = params.get("home");
		target.location = warpRepository.GetPrivate(player.getName(), home);
		if (target.location == null)
			target.message = String.format("You do not have a home named %s.", home);
		else
			target.force = true;
		return target;
	}

	private final WarpRepository warpRepository;
}
