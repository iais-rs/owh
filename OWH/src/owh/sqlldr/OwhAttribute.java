package owh.sqlldr;

import owh.OwhHistory;
import rst.db.sqlldr.SqlLdrCol;
import rst.osm.graph.OsmNode;

public abstract class OwhAttribute extends OwhCol
{

	//          Constructor
	// ============================================================================================================================================

	public OwhAttribute (final OwhSqlLdr owhldr, final boolean version, final SqlLdrCol col) { super(owhldr,version,col); }

	//          Methods
	// ============================================================================================================================================

	public abstract void set (final OsmNode node);

	// --------------------------------------------------------------------------------------------------------------------------------------------

	public final void set (final OwhHistory history, final int index) { set(history.get(index)); }

	// ============================================================================================================================================

}
