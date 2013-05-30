package no.runsafe.warpdrive.summoningstone;

import no.runsafe.framework.event.entity.IEntityPortalEnterEvent;
import no.runsafe.framework.event.player.IPlayerJoinEvent;
import no.runsafe.framework.event.player.IPlayerPortalEvent;
import no.runsafe.framework.event.player.IPlayerRightClickBlock;
import no.runsafe.framework.server.RunsafeLocation;
import no.runsafe.framework.server.RunsafeWorld;
import no.runsafe.framework.server.block.RunsafeBlock;
import no.runsafe.framework.server.entity.PassiveEntity;
import no.runsafe.framework.server.entity.RunsafeEntity;
import no.runsafe.framework.server.entity.RunsafeItem;
import no.runsafe.framework.server.event.entity.RunsafeEntityPortalEnterEvent;
import no.runsafe.framework.server.event.player.RunsafePlayerJoinEvent;
import no.runsafe.framework.server.event.player.RunsafePlayerPortalEvent;
import no.runsafe.framework.server.item.RunsafeItemStack;
import no.runsafe.framework.server.item.meta.RunsafeBookMeta;
import no.runsafe.framework.server.item.meta.RunsafeItemMeta;
import no.runsafe.framework.server.player.RunsafePlayer;
import org.bukkit.Effect;
import org.bukkit.Material;

public class EventHandler implements IPlayerPortalEvent, IEntityPortalEnterEvent, IPlayerRightClickBlock, IPlayerJoinEvent
{
	public EventHandler(SummoningEngine engine, SummoningStoneRepository repository)
	{
		this.engine = engine;
		this.repository = repository;
	}

	@Override
	public void OnPlayerPortalEvent(RunsafePlayerPortalEvent event)
	{
		RunsafeLocation from = event.getFrom();

		if (from != null)
		{
			int stoneID = this.engine.getStoneAtLocation(from);
			if (stoneID > -1) event.setCancelled(true);
		}
	}

	@Override
	public void OnEntityPortalEnter(RunsafeEntityPortalEnterEvent event)
	{
		if (event.getBlock().getTypeId() == Material.ENDER_PORTAL.getId())
		{
			RunsafeLocation location = event.getLocation();
			int stoneID = this.engine.getStoneAtLocation(location);

			if (stoneID > -1)
			{
				RunsafeEntity entity = event.getEntity();
				if (entity.getEntityType() == PassiveEntity.DroppedItem)
				{
					RunsafeItemStack item = ((RunsafeItem) entity).getItemStack();
					if (item.getItemId() == Material.WRITTEN_BOOK.getId())
					{
						RunsafeItemMeta meta = item.getItemMeta();

						if (meta != null)
						{
							if (meta instanceof RunsafeBookMeta)
							{
								RunsafeWorld world = location.getWorld();
								this.engine.registerPendingSummon(((RunsafeBookMeta) meta).getAuthor(), stoneID);
								world.playEffect(location, Effect.GHAST_SHRIEK, 0);
								world.createExplosion(location.getX() + 0.5, location.getY(), location.getZ() + 0.5, 0, false, false);
							}
						}
					}
				}
				entity.remove();
			}
		}
	}

	@Override
	public boolean OnPlayerRightClick(RunsafePlayer runsafePlayer, RunsafeItemStack itemStack, RunsafeBlock runsafeBlock)
	{
		if (itemStack.getType() == Material.FLINT_AND_STEEL && runsafeBlock.getTypeId() == Material.EMERALD_BLOCK.getId())
		{
			RunsafeLocation stoneLocation = runsafeBlock.getLocation();
			if (SummoningStone.isSummoningStone(stoneLocation))
			{
				int stoneID = this.repository.addSummoningStone(stoneLocation);
				SummoningStone summoningStone = new SummoningStone(stoneLocation);
				summoningStone.activate();
				summoningStone.setTimerID(this.engine.registerExpireTimer(stoneID));

				this.engine.registerStone(stoneID, summoningStone);
				return false;
			}
		}
		else if (itemStack.getType() == Material.EYE_OF_ENDER && runsafeBlock.getTypeId() == Material.ENDER_PORTAL_FRAME.getId())
		{
			if (this.engine.isRitualWorld(runsafePlayer.getWorld()))
			{
				if (this.engine.playerHasPendingSummon(runsafePlayer))
					this.engine.acceptPlayerSummon(runsafePlayer);
				else
					runsafePlayer.sendColouredMessage("&cYou have no pending summons to accept.");
				return false;
			}
		}

		return true;
	}

	@Override
	public void OnPlayerJoinEvent(RunsafePlayerJoinEvent event)
	{
		RunsafePlayer player = event.getPlayer();
		if (this.engine.playerHasPendingSummon(player))
			player.sendColouredMessage("&3You have a pending summon, head to the ritual stone to accept.");
	}

	private SummoningEngine engine;
	private SummoningStoneRepository repository;
}
