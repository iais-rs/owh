package owh;

import java.util.*;

import rst.check;
import rst.iterate.ArrayIterator;
import rst.osm.graph.*;

public final class OwhHistory implements Iterable<OsmNode>
{

	//          Fields
	// ============================================================================================================================================

	private final OsmNode[] nodes;

	public final OsmNode firstVisible, lastVisible, lastVisibleLocation;

	public final boolean hasWheelchair, hasWheelmapPoi, hasOtherPoi;

	//          Constructor
	// ============================================================================================================================================

	public OwhHistory (final OsmHistory<OsmNode> history, final Set<OsmTag> wheelmapPoiFilter, final Set<OsmTag> otherPoiFilter)
	{
		nodes = new OsmNode[history.size()];

		int i = 0, lastVersion = 0;
		OsmNode firstVisible_ = null, lastVisible_ = null, lastVisibleLocation_ = null;
		boolean hasWheelchair_ = false, hasWheelmapPoi_ = false, hasOtherPoi_ = false;

		for (final OsmNode node : history)
		{
			check.that(node.version>lastVersion);
			lastVersion = node.version;

			nodes[i++] = node;
			if (node.isVisible())
			{
				if (firstVisible_==null) firstVisible_ = node;
				lastVisible_ = node;
				if (node.lon!=null && node.lat!=null) lastVisibleLocation_ = node;
			}

			if (!hasWheelchair_  && node.hasKey("wheelchair")     ) hasWheelchair_  = true;
			if (!hasWheelmapPoi_ && node.hasTag(wheelmapPoiFilter)) hasWheelmapPoi_ = true;
			if (!hasOtherPoi_    && node.hasTag(otherPoiFilter)   ) hasOtherPoi_    = true;
		}

		firstVisible        = firstVisible_;
		lastVisible         = lastVisible_;
		lastVisibleLocation = lastVisibleLocation_;
		hasWheelchair       = hasWheelchair_;
		hasWheelmapPoi      = hasWheelmapPoi_;
		hasOtherPoi         = hasOtherPoi_;
	}

	//          Methods
	// ============================================================================================================================================

	public final boolean isVisible (final int index) { return nodes[index].isVisible(); }

	public final boolean isLastVersionVisible () { return nodes[nodes.length-1].isVisible(); }

	public final boolean isLastVisible (final int index) { return nodes[index]==lastVisible; }

	public final boolean isLastVisibleLocation (final int index) { return nodes[index]==lastVisibleLocation; }

	// --------------------------------------------------------------------------------------------------------------------------------------------

	public final boolean isLastInChangeset (final int index)
	{
		final int next = index+1;
		return next>=nodes.length || (nodes[next].changeset.id!=nodes[index].changeset.id);
	}

	// --------------------------------------------------------------------------------------------------------------------------------------------

	public final boolean isLastVisInChangeset (final int index)
	{
		final OsmNode node = nodes[index];
		if (!node.isVisible()) return false;
		for (int i=index+1; i<nodes.length; i++)
		{
			final OsmNode cur = nodes[i];
			if (cur.changeset.id==node.changeset.id)
			{
				if (cur.isVisible()) return false;
			}
			else i = nodes.length;
		}
		return true;
	}

	// --------------------------------------------------------------------------------------------------------------------------------------------

	public final String getWheelchairValid (final int index)
	{
		final OsmNode node = nodes[index];
		final OsmTag wheelchairTag = node.getTag("wheelchair");
		if (wheelchairTag==null) return null;
		else
		{
			final String value = wheelchairTag.value.toLowerCase();
			return (value.equals("yes") || value.equals("no") || value.equals("limited") ? value : null);
		}
	}

	// --------------------------------------------------------------------------------------------------------------------------------------------

	public final boolean hasValidWheelchair (final int index) { return getWheelchairValid(index)!=null; }

	// --------------------------------------------------------------------------------------------------------------------------------------------

	/** hier werden auch die nicht-last-visible-in-changeset verwendet!!! */
	public final boolean isValidWheelchairChange (final int index)
	{
		if (nodes[index]==firstVisible) return true;
		else if (!nodes[index].isVisible()) return false;
		else
		{
			String wc = null;
			for (int i=0; i<index; i++)
				if (nodes[i].isVisible()) wc = getWheelchairValid(i);

			final String wcIdx = getWheelchairValid(index);
			if (wc==null && wcIdx==null) return false;
			else if ((wc==null) != (wcIdx==null)) return true;
			else return !wc.equalsIgnoreCase(wcIdx);
		}
	}

	// --------------------------------------------------------------------------------------------------------------------------------------------

	/** duration in seconds iff isValidWheelchairChange(index); otherwise null */
	public final Long getValidWheelchairChangeDuration (final int index)
	{
		if (!isValidWheelchairChange(index)) return null;
		if (nodes[index]==lastVisible) return null;
		else
		{
			final String wc = getWheelchairValid(index);
			for (int i=index+1; i<nodes.length; i++)
			{
				final OsmNode node = nodes[i];
				if (node.isVisible())
				{
					final String wcNode = getWheelchairValid(i);
					if (wc==null && wcNode==null); // do nothing / continue
					else if ( ((wc==null) != (wcNode==null)) || !wcNode.equalsIgnoreCase(wc))
						return (nodes[i].changeset.timeMillisUtc-nodes[index].changeset.timeMillisUtc)/1000;
				}
			}
			return null; // if valid till the end, then return null
		}
	}

	// --------------------------------------------------------------------------------------------------------------------------------------------

	public final boolean hasKey (final int index, final String key) { return nodes[index].hasKey(key); }

	public final boolean hasTag (final int index, final Set<OsmTag> tags) { return nodes[index].hasTag(tags); }

	// --------------------------------------------------------------------------------------------------------------------------------------------

	public final int size () { return nodes.length; }

	public final OsmNode get (final int index) { return nodes[index]; }

	public final OsmNode getIfExists (final int index) { return index<nodes.length ? nodes[index] : null; }

	public final Iterator<OsmNode> iterator () { return new ArrayIterator<OsmNode>(nodes); }

	// ============================================================================================================================================

}
