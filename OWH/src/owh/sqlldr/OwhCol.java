package owh.sqlldr;

import owh.OwhHistory;
import rst.check;
import rst.api.UOE;
import rst.db.sqlldr.SqlLdrCol;
import rst.osm.graph.OsmNode;

public abstract class OwhCol
{

	//          Fields
	// ============================================================================================================================================

	public final SqlLdrCol col;

	public final boolean aggTagCol;

	public final String name;

	//          Constructors
	// ============================================================================================================================================

	public OwhCol (final OwhSqlLdr owhldr, final Boolean version, final boolean aggTagCol, final SqlLdrCol col)
	{
		this.col       = check.nn(col).nullable();
		this.aggTagCol = aggTagCol;
		this.name      = col.name;

		owhldr.add(col);
		if (aggTagCol) check.that(version.booleanValue());
		if (version!=null) owhldr.add(this,version);
		check.that(name.equals(name.trim().toLowerCase()));
	}

	// --------------------------------------------------------------------------------------------------------------------------------------------

	public OwhCol (final OwhSqlLdr owhldr, final Boolean version, final SqlLdrCol col) { this(owhldr,version,false,col); }

	//          Methods
	// ============================================================================================================================================

	public abstract void set (final OwhHistory history, final int index);

	// --------------------------------------------------------------------------------------------------------------------------------------------

	public void set (final OsmNode node) { throw UOE.uo.e(); }
	public void set (final OwhHistory history) { throw UOE.uo.e(); }

	// --------------------------------------------------------------------------------------------------------------------------------------------

	public final void setNull () { col.set(null); }

	public final void set (final String value) { col.set(value); }

	public final void set01 (final boolean value) { set(value ? 1 : 0); }

	// --------------------------------------------------------------------------------------------------------------------------------------------

	public final void set (final int value) { col.set(value); }

	public final void set (final long value) { col.set(value); }

	public final void set (final double value) { col.set(value); }

	// --------------------------------------------------------------------------------------------------------------------------------------------

	public final void set (final Integer value) { if (value!=null) col.set(value.intValue()); else col.set(null); }

	public final void set (final Long value) { if (value!=null) col.set(value.longValue()); else col.set(null); }

	public final void set (final Double value) { if (value!=null) col.set(value.doubleValue()); else col.set(null); }

	// ============================================================================================================================================

}
