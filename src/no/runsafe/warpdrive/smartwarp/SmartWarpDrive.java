package no.runsafe.warpdrive.smartwarp;

import no.runsafe.framework.api.*;
import no.runsafe.framework.api.entity.IEntity;
import no.runsafe.framework.api.event.player.IPlayerDamageEvent;
import no.runsafe.framework.api.event.plugin.IConfigurationChanged;
import no.runsafe.framework.api.event.plugin.IPluginDisabled;
import no.runsafe.framework.api.log.IConsole;
import no.runsafe.framework.api.player.IPlayer;
import no.runsafe.framework.minecraft.event.entity.RunsafeEntityDamageEvent;
import no.runsafe.framework.timer.ForegroundWorker;
import no.runsafe.warpdrive.Engine;
import no.runsafe.warpdrive.database.SmartWarpChunkRepository;

import java.util.ArrayList;
import java.util.List;

public class SmartWarpDrive extends ForegroundWorker<IEntity, ILocation>
	implements IPlayerDamageEvent, IConfigurationChanged, IPluginDisabled
{
	public SmartWarpDrive(IScheduler scheduler, SmartWarpChunkRepository smartWarpChunks, Engine engine, IConsole console)
	{
		super(scheduler);
		this.scheduler = scheduler;
		this.smartWarpChunks = smartWarpChunks;
		this.engine = engine;
		this.console = console;
		setInterval(10);
	}

	public void EngageSurface(IEntity entity, IWorld target, boolean lock)
	{
		if (lockedSurfaceLocation != null)
		{
			if (skyFall)
				fallen.add(entity);
			Push(entity, lockedSurfaceLocation);
			return;
		}
		ILocation candidate = new ChunkScanner(scheduler, smartWarpChunks, target, false, engine).find();
		if (candidate == null)
			return;
		if (skyFall)
		{
			fallen.add(entity);
			candidate.setY(300);
			candidate.setPitch(90);
		}
		if (lock)
		{
			lockedSurfaceLocation = candidate;
			scheduler.startSyncTask(this::unlockSurface, 20);
		}
		Push(entity, candidate);
	}

	public void EngageCave(IEntity entity, IWorld target, boolean lock)
	{
		if (lockedCaveLocation != null)
		{
			Push(entity, lockedCaveLocation);
			return;
		}
		ILocation candidate = new ChunkScanner(scheduler, smartWarpChunks, target, true, engine).find();
		if (candidate == null)
			return;
		if (lock)
		{
			lockedCaveLocation = candidate;
			scheduler.startSyncTask(this::unlockCave, 20);
		}
		Push(entity, candidate);
	}

	@Override
	public void process(IEntity entity, ILocation target)
	{
		if (!entity.teleport(target) && fallen.contains(entity))
			fallen.remove(entity);
	}

	@Override
	public void OnConfigurationChanged(IConfiguration configuration)
	{
		skyFall = configuration.getConfigValueAsBoolean("smart.skyfall");
	}

	@Override
	public void OnPlayerDamage(IPlayer player, RunsafeEntityDamageEvent event)
	{
		if (event.getCause() == RunsafeEntityDamageEvent.RunsafeDamageCause.FALL)
		{
			if (fallen.contains(player))
			{
				event.cancel();
				fallen.remove(player);
			}
		}
	}

	@Override
	public void OnPluginDisabled()
	{
		if (!fallen.isEmpty())
			console.logInformation("Teleporting %d falling players due to plugin shutdown.", fallen.size());
		for (IEntity entity : fallen)
		{
			if (entity != null)
				entity.teleport(entity.getLocation().findTop());
		}
	}

	private void unlockCave()
	{
		lockedCaveLocation = null;
	}

	private void unlockSurface()
	{
		lockedSurfaceLocation = null;
	}

	private ILocation lockedCaveLocation;
	private ILocation lockedSurfaceLocation;
	private boolean skyFall = false;
	private final List<IEntity> fallen = new ArrayList<>(0);
	private final IScheduler scheduler;
	private final SmartWarpChunkRepository smartWarpChunks;
	private final Engine engine;
	private final IConsole console;
}
