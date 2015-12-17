package owh;

import rst.*;

import java.util.Arrays;

public final class NodeDatabaseEntry
{

	//          Fields
	// ====================================================================================================================================================================================

	private final int maxsize = 1000*1000;

	private final long[] nodeids;
	private final float[] lons, lats;

	private int size = 0, index = -1;
	private long minNodeid = -1, maxNodeid = -1;

	//          Constructor
	// ====================================================================================================================================================================================

	public NodeDatabaseEntry ()
	{
		nodeids = new long [maxsize];
		lons    = new float[maxsize];
		lats    = new float[maxsize];
	}

	//          Methods
	// ====================================================================================================================================================================================

	public final boolean isFull () { return size==maxsize; }
	public final int getSize () { return size; }

	public final boolean contains (final long nodeid) { return minNodeid <= nodeid && nodeid <= maxNodeid; }

	public final long getMinNodeid () { return minNodeid; }
	public final long getMaxNodeid () { return maxNodeid; }

	// ------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------

	public final void set (final long nodeid, final float lon, final float lat)
	{
		check.lower(nodeid,maxNodeid);

		if (size>0)
		{
			index = size-1;
			final long lastNodeid = nodeids[index];

			if (lastNodeid!=nodeid)
			{
				maxNodeid = nodeid;
				index = size;
				size++;
			}
		}
		else
		{
			check.lower(nodeid,1);
			minNodeid = maxNodeid = nodeid;
			size = 1;
			index = 0;
		}

		nodeids[index] = nodeid;
		lons   [index] = lon;
		lats   [index] = lat;
	}

	// ------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------

	public final float getLon (final long nodeid)
	{
		check.that(find(nodeid));
		return lons[index];
	}

	// ------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------

	public final float getLat (final long nodeid)
	{
		check.that(find(nodeid));
		return lats[index];
	}

	// ------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------

	public final boolean find (final long nodeid)
	{
		check.bounds(nodeid,minNodeid,maxNodeid);
		if (index<0 || index>=size) index = 0;
		if (nodeids[index]==nodeid) return true;

		final int low, high;
		if (nodeids[index]<nodeid)
		{
			low = index+1;
			high = size;
		}
		else
		{
			low = 0;
			high = index;
		}

		index = Arrays.binarySearch(nodeids,low,high,nodeid);
		if (index>=0 && nodeids[index]==nodeid) return true;
		else
		{
			String message = null;
			for (int i=0; i<size; i++)
			{
				if (nodeids[i]==nodeid)
				{
					if (message==null) message = "";
					message += "\nfound @ "+i;
				}
				if (i>0) check.lower_(nodeids[i],nodeids[i-1]);
			}
			if (message==null)
			{
				System.out.println("!!!!!!!!!! nodeid "+nodeid+" not found in DatabaseEntry min "+minNodeid+" max "+maxNodeid);
				return false;
			}
			else
			{
				message = message.trim();
				System.out.println(message);
				time.sleep(1000);
				throw check.re(nodeid);
			}
		}
	}

	// ====================================================================================================================================================================================

}
