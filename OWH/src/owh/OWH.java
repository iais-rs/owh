package owh;

import java.sql.Connection;
import java.util.ArrayList;
import java.util.Set;
import java.util.TreeSet;

import org.xml.sax.Attributes;
import org.xml.sax.SAXException;

import owh.sqlldr.OwhSqlLdr;
import rst.check;
import rst.db.ConnectionData;
import rst.db.ConnectionFactory;
import rst.db.ResultSet2;
import rst.db.sql;
import rst.db.rs2cols.BooleanColumn;
import rst.db.rs2cols.StringColumn;
import rst.misc.CarpetTokenizer;
import rst.osm.graph.OsmChangeset;
import rst.osm.graph.OsmElementWithHist;
import rst.osm.graph.OsmHistory;
import rst.osm.graph.OsmNode;
import rst.osm.graph.OsmTag;
import rst.osm.graph.OsmWay;
import rst.textio.BufferedReader2;
import rst.textio.Param;
import rst.textio.ParamUser;
import rst.xml.XmlAnalyzer;
import rst.xml.XmlDoneException;
import rst.xml.XmlHandler;

public final class OWH extends ParamUser implements XmlHandler
{

	//          Fields
	// ====================================================================================================================================================================================

	private final NodeDatabase nodeDatabase;

	private final Set<OsmTag> wheelmapPoiFilter = new TreeSet<OsmTag>(), otherPoiFilter = new TreeSet<OsmTag>();

	private final OwhSqlLdr ldr;

	private final long maxnodes;

	private final boolean stopOnWays;

	// ------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------

	private OsmHistory<OsmNode> xmlNodeHistory = null;
	private OsmHistory<OsmWay> xmlWayHistory = null;
	private OsmNode xmlNode = null;
	private OsmWay xmlWay = null;

	private long nodeCounter = 0, wayCounter = 0;

	//          Constructor
	// ====================================================================================================================================================================================

	public OWH (final Param param)
	{
		super(param);
		stopOnWays = param.getBoolean("stop.on.ways","true","false");
		nodeDatabase = (stopOnWays ? null : new NodeDatabase(param.getInt("node.database.init.create")));
		maxnodes = param.getLong("maxnodes");

		loadPoiFilters();
		ldr = new OwhSqlLdr(this);

		parseXmlFile();
		
		ldr.close();
	}

	//          Methods
	// ====================================================================================================================================================================================

	private final boolean haveToHandle (final OsmHistory<? extends OsmElementWithHist> history)
	{
		for (final OsmElementWithHist elem : history)
		{
			if (elem.hasKey("wheelchair")
					|| elem.hasTag(wheelmapPoiFilter)
					|| elem.hasTag(otherPoiFilter)
			)
				return true;
		}
		return false;
	}

	// ------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------

	public final void handleNodeHistory (final OsmHistory<OsmNode> history)
	{
		if (haveToHandle(history))
		{
			final OwhHistory owh = new OwhHistory(history,wheelmapPoiFilter,otherPoiFilter);
			ldr.write(owh);
		}
	}

	// ------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------

	public final void handleWayHistory (final OsmHistory<OsmWay> wayHistory, final NodeDatabase nodeDatabase)
	{
		if (haveToHandle(wayHistory))
		{
			OsmWay last = null;
			for (final OsmWay way : wayHistory) if (way.isVisible()) last = way;
			if (last==null) for (final OsmWay way : wayHistory) if (!way.isVisible()) last = way;
			if (last==null) return; // dann halt nicht

			double
				minLon = Double.POSITIVE_INFINITY, maxLon = Double.NEGATIVE_INFINITY,
				minLat = Double.POSITIVE_INFINITY, maxLat = Double.NEGATIVE_INFINITY;

			for (final long nodeid : last.nodeids())
			{
				if (nodeDatabase.find(nodeid))
				{
					final double lon = nodeDatabase.getLon(nodeid);
					final double lat = nodeDatabase.getLat(nodeid);

					if (lon<minLon) minLon = lon;
					if (lon>maxLon) maxLon = lon;
					if (lat<minLat) minLat = lat;
					if (lat>maxLat) maxLat = lat;
				}
			}

			if (Double.isInfinite(minLon)) return; // dann halt auch nicht
			else
			{
				final double lon = (minLon+maxLon)/2;
				final double lat = (minLat+maxLat)/2;

				final ArrayList<OsmNode> list = new ArrayList<OsmNode>(wayHistory.size());

				for (final OsmWay way : wayHistory)
				{
					final OsmNode node = new OsmNode(-way.id,way.version,way.changeset,lon,lat);
					node.setVisible(way.isVisible());
					for (final OsmTag tag : way.tags()) node.addTag(tag);
					list.add(node);
				}

				final OsmHistory<OsmNode> nodeHistory = new OsmHistory<OsmNode>(list.get(0));
				for (int i=1; i<list.size(); i++) nodeHistory.add(list.get(i));

				handleNodeHistory(nodeHistory);
			}
		}
	}

	// ------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------

	private final void parseXmlFile ()
	{
		final String sourcedir = check.dir(param.get("source.dir.xml"),true);
		final String osmfile   = check.nn(param.get("source.file.xml"));
		new XmlAnalyzer(sourcedir,osmfile,100*1000*1000).setHandler(this).run().print(sourcedir,osmfile+".stats",true);
	}

	// ------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------

	public final void startElement (final String typ, final String path, final int depth, final Attributes attributes) throws SAXException
	{
		if (path.equals("osm.node"))
		{
			nodeCounter++;
			if (maxnodes>0 && nodeCounter>maxnodes) throw new XmlDoneException();

			final long   csid   = Long.parseLong(attributes.getValue("changeset"));
			final String cstime = attributes.getValue("timestamp");
			final String csuser = attributes.getValue("user");

			final String csuid_ = attributes.getValue("uid");
			final Long csuid = (csuid_!=null ? Long.parseLong(csuid_) : null);

			final OsmChangeset cs = new OsmChangeset(csid,cstime,csuser,csuid);

			final long nid   = Long.parseLong(attributes.getValue("id"));
			final int  nvers = Integer.parseInt(attributes.getValue("version"));

			Double nlon = null, nlat = null;
			final String nlon_ = attributes.getValue("lon");
			final String nlat_ = attributes.getValue("lat");
			if (nlon_!=null) nlon = Double.parseDouble(nlon_);
			if (nlat_!=null) nlat = Double.parseDouble(nlat_);

			if (nodeDatabase!=null && nlon!=null && nlat!=null) nodeDatabase.set(nid,nlon.floatValue(),nlat.floatValue());

			xmlNode = new OsmNode(nid,nvers,cs,nlon,nlat);

			final String visible = attributes.getValue("visible");
			if (visible!=null)
			{
				if(visible.equals("true")) xmlNode.setVisible(true);
				else
				{
					check.value(visible,"false");
					xmlNode.setVisible(false);
				}
			}

			if (xmlNodeHistory==null || xmlNodeHistory.id!=xmlNode.id)
			{
				if (xmlNodeHistory!=null) handleNodeHistory(xmlNodeHistory);
				xmlNodeHistory = new OsmHistory<OsmNode>(xmlNode);
			}
			else xmlNodeHistory.add(xmlNode);
		}
		else if (path.equals("osm.node.tag"))
		{
			final String k = attributes.getValue("k").replace("#","+");
			final String v = attributes.getValue("v").replace("#","+");
			xmlNode.addTag(OsmTag.get(k,v));
		}
		else if (typ.equals("way"))
		{
			if (wayCounter==0)
			{
				if (xmlNodeHistory!=null) handleNodeHistory(xmlNodeHistory);
				xmlNodeHistory = null;

				ldr.waysHaveStarted();

				if (stopOnWays) throw new XmlDoneException();
			}
			wayCounter++;
			xmlWay = null;

			final long   csid   = Long.parseLong(attributes.getValue("changeset"));
			final String cstime = attributes.getValue("timestamp");
			final String csuser = attributes.getValue("user");

			final String csuid_ = attributes.getValue("uid");
			final Long csuid = (csuid_!=null ? Long.parseLong(csuid_) : null);

			final OsmChangeset cs = new OsmChangeset(csid,cstime,csuser,csuid);

			final long nid   = Long.parseLong(attributes.getValue("id"));
			final int  nvers = Integer.parseInt(attributes.getValue("version"));

			xmlWay = new OsmWay(nid,nvers,cs);

			final String visible = attributes.getValue("visible");
			if (visible!=null)
			{
				if(visible.equals("true")) xmlWay.setVisible(true);
				else
				{
					check.value(visible,"false");
					xmlWay.setVisible(false);
				}
			}

			if (xmlWayHistory==null || xmlWayHistory.id!=xmlWay.id)
			{
				if (xmlWayHistory!=null) handleWayHistory(xmlWayHistory,nodeDatabase);
				xmlWayHistory = new OsmHistory<OsmWay>(xmlWay);
			}
			else xmlWayHistory.add(xmlWay);
		}
		else if (path.equals("osm.way.nd"))
		{
			final long nodeid = Long.parseLong(attributes.getValue("ref"));
			xmlWay.addNodeid(nodeid);
		}
		else if (path.equals("osm.way.tag"))
		{
			final String k = attributes.getValue("k").replace("#","+");
			final String v = attributes.getValue("v").replace("#","+");
			xmlWay.addTag(OsmTag.get(k,v));
		}
		else if (typ.equals("relation"))
		{
			if (xmlNodeHistory!=null) handleNodeHistory(xmlNodeHistory);
			xmlNodeHistory = null;

			if (xmlWayHistory!=null) handleWayHistory(xmlWayHistory,nodeDatabase);
			xmlWayHistory = null;

			throw new XmlDoneException();
		}
	}

	// ------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------

	public final void endElement (final String typ, final String path, final int depth, final String characters) throws SAXException {}

	// ------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------

	private final void loadPoiFilters ()
	{
		final Set<String> poiKeys = new TreeSet<String>();
		final String source = check.value(param.get("poi.filter.source"),"database","file");

		if (source.equals("database"))
		{
			final ConnectionData cdata = ConnectionData.get(param);
			final Connection conn = ConnectionFactory.oracleConnection(cdata);
			final ResultSet2 rs = new ResultSet2(conn,true,"select key, value, wheelmap_tag from "+param.get("poi.filter.table"));

			final StringColumn  key      = new StringColumn (rs,"key");
			final StringColumn  value    = new StringColumn (rs,"value");
			final BooleanColumn wheelmap = new BooleanColumn(rs,"wheelmap_tag","1","0");

			while (rs.next())
			{
				final OsmTag tag = OsmTag.get(key,value);
				poiKeys.add(tag.key);

				final boolean isWheelmap = wheelmap.get();
				check.that((isWheelmap ? wheelmapPoiFilter : otherPoiFilter).add(tag));
			}

			sql.close(conn);
		}
		else
		{
			for (final String line : new BufferedReader2(param.get("poi.filter.file")))
			{
				final CarpetTokenizer tok = new CarpetTokenizer(line);
				final String key = tok.next();
				final String value = tok.next();
				final int wheelmap = tok.nextInt();
				check.bounds(wheelmap,0,1);
				final boolean isWheelmap= (wheelmap==1);

				final OsmTag tag = OsmTag.get(key,value);
				poiKeys.add(tag.key);
				check.that((isWheelmap ? wheelmapPoiFilter : otherPoiFilter).add(tag));
			}
		}

		System.out.println();
		System.out.println("num wheelmap poi-tags: "+wheelmapPoiFilter.size());
		System.out.println("num other poi-tags: "+otherPoiFilter.size());
		System.out.println("num poi-keys: "+poiKeys.size());
	}

	// ------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------

	public final Set<OsmTag> getWheelmapPoiFilter () { return wheelmapPoiFilter; }
	public final Set<OsmTag> getOtherPoiFilter    () { return otherPoiFilter; }

	// ====================================================================================================================================================================================

}
