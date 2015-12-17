package owh.sqlldr;

import owh.OwhHistory;
import rst.check;
import rst.db.sqlldr.cols.SqlLdrColNum;
import rst.osm.graph.OsmNode;

public final class OwhLonLat
{

	//          Fields
	// ============================================================================================================================================

	private final boolean lastVisible;

	private final OwhCol lon, lat;

	//          Constructor
	// ============================================================================================================================================

	public OwhLonLat (final OwhSqlLdr owhldr, final boolean regular, final String prefix)
	{
		this.lastVisible = !regular;
		final Boolean version_ = (regular ? true : null);

		this.lon = new OwhCol(owhldr,version_,new SqlLdrColNum(owhldr.ldr,prefix+"lon"))
		{
			public void set (final OsmNode node) { set(node.lon); }

			public final void set (final OwhHistory hist, final int i) { set(hist.get(i)); }
		};

		this.lat = new OwhCol(owhldr,version_,new SqlLdrColNum(owhldr.ldr,prefix+"lat"))
		{
			public void set (final OsmNode node) { set(node.lat); }

			public final void set (final OwhHistory hist, final int i) { set(hist.get(i)); }
		};
	}

	//          Methods
	// ============================================================================================================================================

	public final void set (final OwhHistory history)
	{
		check.that(lastVisible);
		if (history.lastVisibleLocation!=null)
		{
			lon.set(history.lastVisibleLocation);
			lat.set(history.lastVisibleLocation);
		}
		else
		{
			lon.setNull();
			lat.setNull();
		}
	}

	// ============================================================================================================================================

}
