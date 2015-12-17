package owh;

import rst.check;
import rst.textio.ProgressPrinter;

public final class NodeDatabase
{

	//          Fields
	// ====================================================================================================================================================================================

	private final NodeDatabaseEntry[] entries = new NodeDatabaseEntry[10*1000];

	private int last = 0, index = 0;

	//          Constructor
	// ====================================================================================================================================================================================

	public NodeDatabase (final int initCreate)
	{
		check.bounds(initCreate,1,5000);
		final ProgressPrinter progress = ProgressPrinter.create(initCreate).setPrefix("init NodeDatabaseEntry's").print();
		for (int i=0; i<initCreate; i++)
		{
			entries[i] = new NodeDatabaseEntry();
			progress.incProgress();
		}
	}

	//          Methods
	// ====================================================================================================================================================================================

	public final void set (final long nodeid, final float lon, final float lat)
	{
		if (entries[last].contains(nodeid)) entries[last].set(nodeid,lon,lat);
		else
		{
			if (entries[last].isFull())
			{
				last++;
				if (entries[last]==null) entries[last] = new NodeDatabaseEntry();
				else check.value(entries[last].getSize(),0);
			}
			entries[last].set(nodeid,lon,lat);
		}
	}

	// ------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------

	public final float getLon (final long nodeid)
	{
		check.that(find(nodeid));
		return entries[index].getLon(nodeid);
	}

	// ------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------

	public final float getLat (final long nodeid)
	{
		check.that(find(nodeid));
		return entries[index].getLat(nodeid);
	}

	// ------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------

	public final boolean find (final long nodeid)
	{
		int low, high;

		if (index<0||index>last) index = 0;
		if (entries[index].contains(nodeid)) return entries[index].find(nodeid);
		else if (nodeid < entries[index].getMinNodeid())
		{
			low = 0;
			high = index-1;
		}
		else
		{
			low  = index+1;
			high = last+1;
		}

		while (low <= high)
		{
			index = (low + high) >>> 1;

			if (entries[index].contains(nodeid)) return entries[index].find(nodeid);
			else if (nodeid < entries[index].getMinNodeid()) high = index-1;
			else low = index+1;
		}

		System.out.println("!!!!!!!!!! nodeid "+nodeid+" not found in Database");
		int container = -1;
		for (int i=0; i<=last; i++)
		{
			if (entries[i].contains(nodeid))
			{
				check.value(container,-1);
				container = i;
			}
			if (i>0) check.lower_(entries[i].getMinNodeid(),entries[i-1].getMaxNodeid());
		}
		if (container>=0) check.not(entries[container].find(nodeid),nodeid);
		return false;
	}

	// ====================================================================================================================================================================================

}
