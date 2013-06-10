package no.runsafe.warpdrive.summoningstone;

import no.runsafe.framework.database.IDatabase;
import no.runsafe.framework.database.Repository;
import no.runsafe.framework.database.Row;
import no.runsafe.framework.database.Set;
import no.runsafe.framework.server.RunsafeLocation;
import no.runsafe.framework.server.RunsafeServer;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;

public class SummoningStoneRepository extends Repository
{
	public SummoningStoneRepository(IDatabase database)
	{
		this.database = database;
	}

	public String getTableName()
	{
		return "summoningStones";
	}

	public List<RunsafeLocation> getStoneList()
	{
		List<RunsafeLocation> stones = new ArrayList<RunsafeLocation>();

		Set data = this.database.Query("SELECT ID, world, x, y, z FROM summoningStones");

		if (data != null)
		{
			for (Row node : data)
			{
				stones.add(new RunsafeLocation(
						RunsafeServer.Instance.getWorld(node.String("world")),
						node.Double("x"),
						node.Double("y"),
						node.Double("z")
				));
			}
		}

		return stones;
	}

	public void wipeStoneList()
	{
		this.database.Execute("DELETE FROM summoningStones");
	}

	public void deleteSummoningStone(int ID)
	{
		this.database.Execute("DELETE FROM summoningStones WHERE ID = ?", ID);
	}

	public int addSummoningStone(RunsafeLocation location)
	{
		this.database.Execute(
				"INSERT INTO summoningStones (world, x, y, z) VALUES(?, ?, ?, ?)",
				location.getWorld().getName(),
				location.getX(),
				location.getY(),
				location.getZ()
		);

		Row data = this.database.QueryRow("SELECT LAST_INSERT_ID() AS ID FROM summoningStones");
		if (data != null)
			return data.Integer("ID");

		return 0;
	}

	@Override
	public HashMap<Integer, List<String>> getSchemaUpdateQueries()
	{
		HashMap<Integer, List<String>> queries = new HashMap<Integer, List<String>>();
		ArrayList<String> sql = new ArrayList<String>();
		sql.add(
				"CREATE TABLE `summoningStones` (" +
						"`ID` int(10) NOT NULL AUTO_INCREMENT," +
						"`world` VARCHAR(255) NOT NULL," +
						"`x` DOUBLE NOT NULL," +
						"`y` DOUBLE NOT NULL," +
						"`z` DOUBLE NOT NULL," +
						"PRIMARY KEY (`ID`)" +
				")"
		);
		queries.put(1, sql);
		return queries;
	}

	private IDatabase database;
}
