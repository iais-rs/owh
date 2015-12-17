package owh.sqlldr;

import owh.OwhHistory;
import rst.check;
import rst.db.sqlldr.SqlLdrCol;

public abstract class OwhFlag extends OwhCol
{

	//          Fields
	// ============================================================================================================================================

	public final String idxSuffix;

	//          Constructor
	// ============================================================================================================================================

	public OwhFlag (final OwhSqlLdr owhldr, final boolean version, final String name, final String idxSuffix)
	{
		super(owhldr,version,new SqlLdrCol(owhldr.ldr,name,"NUMBER(1)"));
		this.idxSuffix = check.nn(idxSuffix);
		owhldr.addFlag(this);
	}

	//			Methods
	// ============================================================================================================================================

	protected abstract boolean is (final OwhHistory history, final int index);

	// --------------------------------------------------------------------------------------------------------------------------------------------

	public final void set (final OwhHistory history, final int index) { set01(is(history,index)); }

	// --------------------------------------------------------------------------------------------------------------------------------------------

	public final void set (final OwhHistory history)
	{
		boolean value = false;
		for (int index=0; index<history.size(); index++)
			if (is(history,index))
			{
				value = true;
				break;
			}
		set01(value);
	}

	// ============================================================================================================================================

}
