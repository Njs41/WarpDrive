package no.runsafe.warpdrive.commands;

import no.runsafe.framework.api.IConfiguration;
import no.runsafe.framework.api.IScheduler;
import no.runsafe.framework.api.command.argument.IArgumentList;
import no.runsafe.framework.api.command.argument.RequiredArgument;
import no.runsafe.framework.api.command.player.PlayerAsyncCommand;
import no.runsafe.framework.api.event.plugin.IConfigurationChanged;
import no.runsafe.framework.api.log.IConsole;
import no.runsafe.framework.api.player.IPlayer;
import no.runsafe.warpdrive.database.WarpRepository;

import java.util.List;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

public class SetHome extends PlayerAsyncCommand implements IConfigurationChanged
{
	public SetHome(IScheduler scheduler, WarpRepository repository, IConsole output)
	{
		super(
			"sethome", "Saves your current location as a home", "runsafe.home.set", scheduler,
			new RequiredArgument("name")
		);
		warpRepository = repository;
		console = output;
	}

	@Override
	public void OnConfigurationChanged(IConfiguration configuration)
	{
		Map<String, String> section = configuration.getConfigValuesAsMap("private.max");
		console.logInformation("Loading configuration..");
		console.logInformation("private.max:");
		for (String key : section.keySet())
			console.logInformation("  %s: %s", key, section.get(key));
		privateWarpLimit.clear();
		for (String key : section.keySet())
			privateWarpLimit.put(key, Integer.valueOf(section.get(key)));
	}

	@Override
	public String OnAsyncExecute(IPlayer player, IArgumentList parameters)
	{
		List<String> homes = warpRepository.GetPrivateList(player.getName());
		String name = parameters.get("name").toLowerCase();
		if (!homes.contains(name))
		{
			int limit = 0;
			for (String group : player.getGroups())
			{
				if (privateWarpLimit.containsKey(group))
					limit = Math.max(limit, privateWarpLimit.get(group));
			}
			if (limit == 0)
				limit = privateWarpLimit.get("default");
			if (homes.size() >= limit)
				return String.format("You are only allowed %d homes on this server.", limit);
		}

		warpRepository.Persist(player.getName(), name, false, player.getLocation());
		return String.format("Current location saved as the home %s.", name);
	}

	private final WarpRepository warpRepository;
	private final ConcurrentHashMap<String, Integer> privateWarpLimit = new ConcurrentHashMap<String, Integer>();
	private final IConsole console;
}
