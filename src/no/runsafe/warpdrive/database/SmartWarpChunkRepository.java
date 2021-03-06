package no.runsafe.warpdrive.database;

import no.runsafe.framework.api.ILocation;
import no.runsafe.framework.api.IWorld;
import no.runsafe.framework.api.database.IDatabase;
import no.runsafe.framework.api.database.ISchemaUpdate;
import no.runsafe.framework.api.database.Repository;
import no.runsafe.framework.api.database.SchemaUpdate;

public class SmartWarpChunkRepository extends Repository
{
	@Override
	public String getTableName()
	{
		return "smartwarp_targets";
	}

	@Override
	public ISchemaUpdate getSchemaUpdateQueries()
	{
		ISchemaUpdate update = new SchemaUpdate();

		update.addQueries(
			"CREATE TABLE smartwarp_targets (" +
				"`world` varchar(255) NOT NULL," +
				"`x` int NOT NULL," +
				"`y` int NOT NULL," +
				"`z` int NOT NULL," +
				"`safe` bit NOT NULL," +
				"`cave` bit NOT NULL," +
				"PRIMARY KEY(`world`,`x`,`y`,`z`)" +
			")"
		);

		return update;
	}

	public void clear(IWorld world)
	{
		database.execute("DELETE FROM smartwarp_targets WHERE world=?", world.getName());
	}

	public ILocation getTarget(IWorld world, boolean cave)
	{
		return database.queryLocation(
			"SELECT world, x, y, z FROM smartwarp_targets WHERE world=? AND safe=true AND cave=? ORDER BY RAND() LIMIT 1",
			world.getName(), cave
		);
	}

	public void setUnsafe(ILocation candidate)
	{
		database.update(
			"UPDATE smartwarp_targets SET safe=false WHERE world=? AND x=? AND y=? AND z=?",
			candidate.getWorld().getName(),
			candidate.getBlockX(), candidate.getBlockY(), candidate.getBlockZ()
		);
	}

	public void saveTarget(ILocation target, boolean safe, boolean cave)
	{
		database.update(
			"INSERT INTO smartwarp_targets (world, x, y, z, safe, cave) VALUES (?, ?, ?, ?, ?, ?)" +
				" ON DUPLICATE KEY UPDATE safe=VALUES(safe), cave=VALUES(cave)",
			target.getWorld().getName(),
			target.getBlockX(), target.getBlockY(), target.getBlockZ(),
			safe, cave
		);
	}
}
