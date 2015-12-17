package owh.sqlldr;

import java.util.*;

import owh.OWH;
import owh.OwhHistory;
import rst.*;
import rst.db.sqlldr.*;
import rst.db.sqlldr.cols.*;
import rst.enums.*;
import rst.osm.graph.*;
import rst.textio.ParamUser;

public final class OwhSqlLdr extends ParamUser
{

	//          Fields
	// ====================================================================================================================================================================================

	private final Set<OsmTag> wheelmapPoiFilter, otherPoiFilter;

	private final SqlLdrCol historySeq, realNode, wheelmapPoiTags, otherPoiTags, allPoiTags;

	private final Map<String,SqlLdrCol> sqlLdrCols = new TreeMap<String,SqlLdrCol>();

	private final List<OwhFlag> flags = new ArrayList<OwhFlag>(64);

	private final List<OwhCol> versionCols = new ArrayList<OwhCol>(64), historyCols = new ArrayList<OwhCol>(64);

	private final StringBuilder wheelmapTagsBuilder = new StringBuilder(1024), otherPoiTagsBuilder = new StringBuilder(1024), allPoiTagsBuilder = new StringBuilder(1024);

	protected final SqlLdr ldr;

	private final OwhLonLat lastVisibleLonLat;

	private final boolean addGeom = false;

	private final SqlLdrColGeo geom;

	//          Constructor
	// ====================================================================================================================================================================================

	public OwhSqlLdr (final OWH owh)
	{
		super(owh);

		wheelmapPoiFilter = owh.getWheelmapPoiFilter();
		otherPoiFilter    = owh.getOtherPoiFilter();

		ldr = new SqlLdr(param,"target.table","target.dir"); //.noTableStats();

		// ------------------------------------------------------------

		new OwhAttribute(this,true,new SqlLdrColNum(ldr,"nodeid" )) { public final void set (final OsmNode node) { set(node.id); } };
		new OwhAttribute(this,true,new SqlLdrColNum(ldr,"version")) { public final void set (final OsmNode node) { set(node.version); } };

		add(historySeq = new SqlLdrSeqCol(ldr,"history_seq"));
		add(realNode   = new SqlLdrColNum(ldr,"real_node"));
		realNode.set(1);

		new OwhAttribute(this,true,new SqlLdrColNum(ldr,"changeset")) { public final void set (final OsmNode node) { set(node.changeset.id); } };
		new OwhAttribute(this,true,new SqlLdrCol(ldr,"timestamp","DATE").setFormat(" date \"yyyy-mm-dd hh24:mi:ss\" ")) {
																		public final void set (final OsmNode node) { set(node.changeset.getSqlTimeString()); } };
		new OwhAttribute(this,true,new SqlLdrColNum(ldr,"uid_"     )) { public final void set (final OsmNode node) { set(node.changeset.uid); } };
		new OwhAttribute(this,true,new SqlLdrColVar(ldr,"user_",1000).setFormat("CHAR(1000)")) {
																		public final void set (final OsmNode node) { set(node.changeset.user); } };

		// ------------------------------------------------------------

		new OwhFlag(this,true,"visible",                 "visible")       { public final boolean is (final OwhHistory hist, final int i) { return hist.isVisible(i); } };
		new OwhFlag(this,true,"last_version_visible",    "last_vers_vis") { public final boolean is (final OwhHistory hist, final int i) { return hist.isLastVersionVisible(); } };
		new OwhFlag(this,true,"last_visible",            "last_vis")      { public final boolean is (final OwhHistory hist, final int i) { return hist.isLastVisible(i); } };
		new OwhFlag(this,true,"last_vis_in_changeset",   "last_vis_cs")   { public final boolean is (final OwhHistory hist, final int i) { return hist.isLastVisInChangeset(i); } };

		// ------------------------------------------------------------

		new OwhFlag(this,false,"wheelchair_node",      "wc_node")      { public final boolean is (final OwhHistory hist, final int i) { return hist.hasKey(i,"wheelchair"); } };
		new OwhFlag(this,false,"valid_wheelchair_node","val_wc_node")  { public final boolean is (final OwhHistory hist, final int i) { return hist.getWheelchairValid(i)!=null; } };
		new OwhFlag(this,false,"wheelmap_poi_node",    "wmappoi_node") { public final boolean is (final OwhHistory hist, final int i) { return hist.hasTag(i,wheelmapPoiFilter); } };
		new OwhFlag(this,false,"other_poi_node",       "oth_poi_node") { public final boolean is (final OwhHistory hist, final int i) { return hist.hasTag(i,otherPoiFilter); } };

		// ------------------------------------------------------------

		new OwhFlag(this,true,"wheelchair_version",      "wc_vers")      { public final boolean is (final OwhHistory hist, final int i) { return hist.hasKey(i,"wheelchair"); } };
		new OwhFlag(this,true,"valid_wheelchair_version","val_wc_vers")  { public final boolean is (final OwhHistory hist, final int i) { return hist.getWheelchairValid(i)!=null; } };
		new OwhFlag(this,true,"wheelmap_poi_version",    "wmappoi_vers") { public final boolean is (final OwhHistory hist, final int i) { return hist.hasTag(i,wheelmapPoiFilter); } };
		new OwhFlag(this,true,"other_poi_version",       "oth_poi_vers") { public final boolean is (final OwhHistory hist, final int i) { return hist.hasTag(i,otherPoiFilter); } };

		// ------------------------------------------------------------

		new OwhTag(this,true,false,"wheelchair");
		new OwhCol(this,true,new SqlLdrColVar(ldr,"wheelchair_valid",7)) { public void set (final OwhHistory hist, final int i) { set(hist.getWheelchairValid(i)); } };

		new OwhFlag(this,true,"wc_valid_status_change","wcv_stat_change") { public final boolean is (final OwhHistory hist, final int i) { return hist.isValidWheelchairChange(i); } };

		new OwhCol(this,true,new SqlLdrColNum(ldr,"wc_valid_status_duration")) { public final void set (final OwhHistory hist, final int i)
		{
			final Long duration = hist.getValidWheelchairChangeDuration(i);
			if (duration==null) col.setNull();
			else col.set(duration.longValue());
		} };

		// ------------------------------------------------------------

		add(wheelmapPoiTags = new SqlLdrColVar(ldr,"wheelmap_poi_tags",1000).nullable());
		add(otherPoiTags    = new SqlLdrColVar(ldr,"other_poi_tags",1000).nullable());
		add(allPoiTags      = new SqlLdrColVar(ldr,false,"all_poi_tags",1000).nullable());

		// ------------------------------------------------------------

		new OwhTag(this,true,false,"name");

		new OwhTag(this,true,false,"toilets:wheelchair")
			.addAlias("toilet:wheelchair").addAlias("wheelchair_toilet").addAlias("wheelchair_toilets")
			.addAlias("wheelchair:toilets").addAlias("wheelchair:toilet");

		for (final String key : new String[]{
				"wheelchair:description","wheelchair:description:en",
				"capacity","capacity:disabled",
				"wheelchair:entrance_width","wheelchair:step_height","wheelchair:places","ramp:wheelchair"})
		{
			new OwhTag(this,true,false,key);
		}

		for (final String key : new String[]{
				"shop", "amenity", "public_transport",
				"highway", "railway", "aerialway", "aeroway",
				"natural", "building", "office",
				"tourism", "leisure", "historic", "sport"})
		{
			new OwhTag(this,true,true,key);
		}

		// ------------------------------------------------------------

		new OwhLonLat(this,true,"");
		lastVisibleLonLat = new OwhLonLat(this,false,"last_visible_");

		if (addGeom)
		{
			geom = new SqlLdrColGeo(ldr,"last_visible_geom",CoordSys.WGS84,GeoType.POINT,false);
			geom.nullable();
		}
		else geom = null;

		// ------------------------------------------------------------

		/*

Date date = new Date();
DateFormat df = new SimpleDateFormat("yyyy-MM-dd HH:mm:ss");
df.setTimeZone(...chose timezone...);

System.out.println("Date and time in Madrid: " + df.format(date));
		 */

//		final Object time_offset_de = check.nn(null); // timezoneDE = TimeZone.getTimeZone("Europe/Berlin");
//		final Object time_offset_gb = check.nn(null); // timezoneGB = TimeZone.getTimeZone("Europe/London");
//		final Object time_offset_es = check.nn(null); // timezoneES = TimeZone.getTimeZone("Europe/Madrid");

		// ------------------------------------------------------------

		addConstIndxCom();
		ldr.addAdditionalAlterCommands("",
			"grant select on "+ldr.tablename+" to public with grant option;",
			"drop synonym owh;","create synonym owh for "+ldr.tablename+";");
		ldr.init();
	}

	//          Methods
	// ====================================================================================================================================================================================

	public final void waysHaveStarted ()
	{
		realNode.set(0);
	}

	// ------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------

	public final void add (final SqlLdrCol col)
	{
		check.that(col.name.equals(col.name.trim().toLowerCase()));
		check.isnull(sqlLdrCols.put(col.name,col));
	}

	// ------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------

	public final void add (final OwhCol col, final boolean version) { (version ? versionCols : historyCols).add(col); }

	public final void addFlag (final OwhFlag flag) { flags.add(flag); }

	// ------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------

	public final void write (final OwhHistory history)
	{
		for (final OwhCol col : historyCols) col.set(history);
		lastVisibleLonLat.set(history);

		if (addGeom)
		{
			final OsmNode loc = history.lastVisibleLocation;
			if (loc!=null) geom.setPoint(loc.lon.doubleValue(),loc.lat.doubleValue());
			else geom.setNull();
		}

		historySeq.reset();
		for (int i=0; i<history.size(); i++)
		{
			final OsmNode node = history.get(i);

			string.clear(wheelmapTagsBuilder);
			string.clear(otherPoiTagsBuilder);
			string.clear(allPoiTagsBuilder);

			for (final OwhCol col : versionCols)
			{
				col.set(history,i);
				if (col.aggTagCol)
				{
					final OsmTag tag = node.getTag(col.name);
					if (tag!=null)
					{
						int counter = 0;

						if (wheelmapPoiFilter.contains(tag))
						{
							append(wheelmapTagsBuilder,tag);
							append(allPoiTagsBuilder,tag);
							counter++;
						}

						if (otherPoiFilter.contains(tag))
						{
							append(otherPoiTagsBuilder,tag);
							append(allPoiTagsBuilder,tag);
							counter++;
						}

						check.bounds(counter,0,1);
					}
				}
			}

			set(wheelmapPoiTags,wheelmapTagsBuilder);
			set(otherPoiTags,otherPoiTagsBuilder);
			set(allPoiTags,allPoiTagsBuilder);

			historySeq.next();
			ldr.writeDataLine();
		}
	}

	// ------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------

	/** constraints, indizes and comments */
	private final void addConstIndxCom ()
	{
		final int prll = 4, createPrll = 4;

		new SqlLdrIdx(ldr,get("nodeid"),get("version")).pk().compress(1).prll(prll).createPrll(createPrll);

		for (final String name : new String[]{"name","user_","uid_","changeset","wheelchair"})
			new SqlLdrIdx(ldr,get(name)).suffix(name.replace("_","")).compress(1).prll(prll).createPrll(createPrll);

		new SqlLdrIdx(ldr,get("wheelmap_poi_tags")).suffix("wmap_poi_tags").compress(1).prll(prll).createPrll(createPrll);
		new SqlLdrIdx(ldr,get("other_poi_tags")).suffix("oth_poi_tags").compress(1).prll(prll).createPrll(createPrll);

		new SqlLdrIdx(ldr,get("wheelchair_valid")).suffix("wc_valid").bidx().prll(prll).createPrll(createPrll);

		for (final OwhFlag flag : flags)
			new SqlLdrIdx(ldr,flag.col).suffix(flag.idxSuffix).bidx().prll(prll).createPrll(createPrll);

		for (final String name : new String[]{"last_visible_lon","last_visible_lat"})
			new SqlLdrIdx(ldr,get(name)).suffix(name.replace("last_visible","fl_lst_vis"))
				.bidx().fkt("floor(",")").prll(prll).createPrll(createPrll);


		get("nodeid")                   .setComment("[att] id of the OSM element");
		get("version")                  .setComment("[att] version of the OSM element");
		get("history_seq")              .setComment("[att] sequence within OWH, starts with 1, no gaps");
		get("changeset")                .setComment("[att] id of the changeset");
		get("timestamp")                .setComment("[att] timestamp of the OSM element");
		get("uid_")                     .setComment("[att] id of the user who edited this version");
		get("user_")                    .setComment("[att] name of the user who edited this version");
		get("visible")                  .setComment("[att] 1 iff version is visible, 0 iff version is not visible");
		get("last_version_visible")     .setComment("[flag-node] indicates if the last version of the node is visible");
		get("last_visible")             .setComment("[flag-vers] indicates if this version of the node is the last visible revision");
		//get("last_in_changeset")        .setComment("[flag-vers] indicates if this version of the node is the last revision within the corresponding changeset");
		get("last_vis_in_changeset")    .setComment("[flag-vers] indicates if this version of the node is the last visible revision within the corresponding changeset");
		get("wheelchair_node")          .setComment("[flag-node] indicates if any version of the node has a wheelchair-tag");
		get("valid_wheelchair_node")    .setComment("[flag-node] indicates if any version of the node has a valid wheelchair-tag [lower(wheelchair) in ('yes','no','limited')]");
		get("wheelmap_poi_node")        .setComment("[flag-node] indicates if any version of the node falls within the WheelMap-POI-filter");
		get("other_poi_node")           .setComment("[flag-node] indicates if any version of the node falls within the rest of OWH-poi-filter");
		get("wheelchair_version")       .setComment("[flag-vers] indicates if this version has a wheelchair-tag");
		get("valid_wheelchair_version") .setComment("[flag-vers] indicates if this version has a valid wheelchair-tag [lower(wheelchair) in ('yes','no','limited')]");
		get("wheelmap_poi_version")     .setComment("[flag-vers] indicates if this version falls within the WheelMap-POI-filter");
		get("other_poi_version")        .setComment("[flag-vers] indicates if this version falls within the rest of OWH-poi-filter");
		get("wheelchair")               .setComment("[tag] value of wheelchair-tag of the version");
		get("wheelchair_valid")         .setComment("[tag] if lower(wheelchair) in ('yes','no','limited') then lower(wheelchair) else NULL");
		get("wc_valid_status_change")   .setComment("[flag-vers] indicates if there is a change in wheelchair_valid on this version; but only on visible nodes");
		get("wc_valid_status_duration") .setComment("[value-vers] iff wc_valid_status_change=1 then duration of the wheelchair_valid in seconds (NULL for last one); otherwise NULL");
		get("wheelmap_poi_tags")        .setComment("[tag-agg] concatenation of all tags that fall within the Wheelmap-POI-filter");
		get("other_poi_tags")           .setComment("[tag-agg] concatenation of all tags that fall within the rest of OWH-poi-filter");
		get("name")                     .setComment("[tag] value of name-tag of the version");

		get("toilets_wheelchair")       .setComment("[tag] value of toilets:wheelchair-tag of the version");
		get("wheelchair_description")   .setComment("[tag] value of wheelchair:description-tag of the version");
		get("wheelchair_description_en").setComment("[tag] value of wheelchair:description:en-tag of the version");
		get("capacity")                 .setComment("[tag] value of capacity-tag of the version");
		get("capacity_disabled")        .setComment("[tag] value of capacity:disabled-tag of the version");

		get("shop")                     .setComment("[tag] value of shop-tag of the version");
		get("amenity")                  .setComment("[tag] value of amenity-tag of the version");
		get("public_transport")         .setComment("[tag] value of public_transport-tag of the version");
		get("highway")                  .setComment("[tag] value of highway-tag of the version");
		get("railway")                  .setComment("[tag] value of railway-tag of the version");
		get("aerialway")                .setComment("[tag] value of aerialway-tag of the version");
		get("aeroway")                  .setComment("[tag] value of aeroway-tag of the version");
		get("natural")                  .setComment("[tag] value of natural-tag of the version");
		get("building")                 .setComment("[tag] value of building-tag of the version");
		get("office")                   .setComment("[tag] value of office-tag of the version");
		get("tourism")                  .setComment("[tag] value of tourism-tag of the version");
		get("leisure")                  .setComment("[tag] value of leisure-tag of the version");
		get("historic")                 .setComment("[tag] value of historic-tag of the version");
		get("sport")                    .setComment("[tag] value of sport-tag of the version");
		get("lon")                      .setComment("[att] longitude (wgs84) of the version");
		get("lat")                      .setComment("[att] latitude (wgs84) of the version");
		get("last_visible_lon")         .setComment("[att-node] longitude (wgs84) of the last visible version of the node");
		get("last_visible_lat")         .setComment("[att-node] latitude (wgs84) of the last visible version of the node");
	}

	// ------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------

	private final void append (final StringBuilder builder, final OsmTag tag)
	{
		if (builder.length()>0) builder.append(", ");
		builder.append(tag.key);
		builder.append("=");
		builder.append(tag.value);
	}

	// ------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------

	private final SqlLdrCol get (final String name) { return check.nn(sqlLdrCols.get(name)); }

	// ------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------

	private final void set (final SqlLdrCol col, final StringBuilder builder)
	{
		String s = builder.toString();
		if (s!=null) s = s.trim();
		if (s.length()==0) s = null;

		if (s==null) col.setNull();
		else col.set(s);
	}

	// ------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------

	public final void close () { ldr.close(); }

	// ====================================================================================================================================================================================

}
