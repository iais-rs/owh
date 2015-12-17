package owh.sqlldr;

import java.util.*;

import owh.OwhHistory;
import rst.check;
import rst.db.sqlldr.cols.SqlLdrColVar;
import rst.osm.graph.OsmNode;

public final class OwhTag extends OwhCol
{

	//          Fields
	// ============================================================================================================================================

	private static final int length = 1000;

	private final String key;

	private final List<String> aliases = new ArrayList<String>(10);

	//          Constructor
	// ============================================================================================================================================

	public OwhTag (final OwhSqlLdr owhldr, final boolean version, final boolean aggTagCol, final String key)
	{
		super(owhldr,version,aggTagCol,new SqlLdrColVar(owhldr.ldr,key.replace(':','_'),length).setFormat("CHAR("+length+")"));
		this.key = check.nn(key);
	}

	//          Methods
	// ============================================================================================================================================

	public final void set (final OwhHistory history, final int i)
	{
		final OsmNode node = history.get(i);
		String value = node.getTagValue(key);
		for (int j=0; value==null && j<aliases.size(); j++) value = node.getTagValue(aliases.get(j));
		set(value);
	}

	// --------------------------------------------------------------------------------------------------------------------------------------------

	public final OwhTag addAlias (final String alias)
	{
		aliases.add(alias);
		return this;
	}

	// ============================================================================================================================================

}
