package org.mtransit.parser.ca_guelph_transit_bus;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import org.apache.commons.lang3.StringUtils;
import org.mtransit.parser.CleanUtils;
import org.mtransit.parser.DefaultAgencyTools;
import org.mtransit.parser.Pair;
import org.mtransit.parser.SplitUtils;
import org.mtransit.parser.SplitUtils.RouteTripSpec;
import org.mtransit.parser.Utils;
import org.mtransit.parser.gtfs.data.GCalendar;
import org.mtransit.parser.gtfs.data.GCalendarDate;
import org.mtransit.parser.gtfs.data.GRoute;
import org.mtransit.parser.gtfs.data.GSpec;
import org.mtransit.parser.gtfs.data.GStop;
import org.mtransit.parser.gtfs.data.GTrip;
import org.mtransit.parser.gtfs.data.GTripStop;
import org.mtransit.parser.mt.data.MAgency;
import org.mtransit.parser.mt.data.MDirectionType;
import org.mtransit.parser.mt.data.MRoute;
import org.mtransit.parser.mt.data.MTrip;
import org.mtransit.parser.mt.data.MTripStop;

// http://data.open.guelph.ca/
// http://data.open.guelph.ca/dataset/guelph-transit-gtfs-data
// http://data.open.guelph.ca/datafiles/guelph-transit/guelph_transit_gtfs.zip
// http://guelph.ca/uploads/google/google_transit.zip
public class GuelphTransitBusAgencyTools extends DefaultAgencyTools {

	public static void main(String[] args) {
		if (args == null || args.length == 0) {
			args = new String[3];
			args[0] = "input/gtfs.zip";
			args[1] = "../../mtransitapps/ca-guelph-transit-bus-android/res/raw/";
			args[2] = ""; // files-prefix
		}
		new GuelphTransitBusAgencyTools().start(args);
	}

	private HashSet<String> serviceIds;

	@Override
	public void start(String[] args) {
		System.out.printf("\nGenerating Guelph Transit bus data...");
		long start = System.currentTimeMillis();
		this.serviceIds = extractUsefulServiceIds(args, this);
		super.start(args);
		System.out.printf("\nGenerating Guelph Transit bus data... DONE in %s.\n", Utils.getPrettyDuration(System.currentTimeMillis() - start));
	}

	@Override
	public boolean excludingAll() {
		return this.serviceIds != null && this.serviceIds.isEmpty();
	}

	@Override
	public boolean excludeCalendar(GCalendar gCalendar) {
		if (this.serviceIds != null) {
			return excludeUselessCalendar(gCalendar, this.serviceIds);
		}
		return super.excludeCalendar(gCalendar);
	}

	@Override
	public boolean excludeCalendarDate(GCalendarDate gCalendarDates) {
		if (this.serviceIds != null) {
			return excludeUselessCalendarDate(gCalendarDates, this.serviceIds);
		}
		return super.excludeCalendarDate(gCalendarDates);
	}

	@Override
	public boolean excludeTrip(GTrip gTrip) {
		if (this.serviceIds != null) {
			return excludeUselessTrip(gTrip, this.serviceIds);
		}
		return super.excludeTrip(gTrip);
	}

	@Override
	public boolean excludeRoute(GRoute gRoute) {
		return super.excludeRoute(gRoute);
	}

	@Override
	public Integer getAgencyRouteType() {
		return MAgency.ROUTE_TYPE_BUS;
	}

	private static final Pattern DIGITS = Pattern.compile("[\\d]+");

	private static final String COMMUNITY_BUS_RSN = "Com";
	private static final long COMMUNITY_BUS_RID = 9998L;

	private static final String U = "U";

	private static final long RID_ENDS_WITH_U = 21000L;

	@Override
	public long getRouteId(GRoute gRoute) {
		if (COMMUNITY_BUS_RSN.equals(gRoute.getRouteShortName())) {
			return COMMUNITY_BUS_RID;
		}
		String routeShortName = gRoute.getRouteShortName();
		if (routeShortName != null && routeShortName.length() > 0 && Utils.isDigitsOnly(routeShortName)) {
			return Long.parseLong(routeShortName); // using route short name as route ID
		}
		Matcher matcher = DIGITS.matcher(routeShortName);
		if (matcher.find()) {
			int digits = Integer.parseInt(matcher.group());
			if (routeShortName.endsWith(U)) {
				return RID_ENDS_WITH_U + digits;
			}
		}
		if (isGoodEnoughAccepted()) {
			if (routeShortName.startsWith("Zone ") //
					|| routeShortName.startsWith("NYE ") //
			) {
				return Long.parseLong(gRoute.getRouteId());
			}
		}
		System.out.printf("\nCan't find route ID for '%s' (%s)!\n", routeShortName, gRoute);
		System.exit(-1);
		return -1l;
	}

	private static final Pattern ALL_WHITESPACES = Pattern.compile("\\s+", Pattern.CASE_INSENSITIVE);

	@Override
	public String getRouteShortName(GRoute gRoute) {
		String routeShortName = gRoute.getRouteShortName();
		routeShortName = ALL_WHITESPACES.matcher(routeShortName).replaceAll(StringUtils.EMPTY);
		return routeShortName;
	}

	private static final Pattern STARTS_WITH_ROUTE_RSN = Pattern.compile("(route[\\d]*[A-Z]*[\\-]?[\\s]*)", Pattern.CASE_INSENSITIVE);

	@Override
	public String getRouteLongName(GRoute gRoute) {
		String routeLongName = gRoute.getRouteLongName();
		routeLongName = STARTS_WITH_ROUTE_RSN.matcher(routeLongName).replaceAll(StringUtils.EMPTY);
		if (StringUtils.isEmpty(routeLongName)) {
			System.out.printf("\ngetRouteLongName() > Unexpected route short name '%s' (%s)!\n", gRoute.getRouteShortName(), gRoute);
			System.exit(-1);
			return null;
		}
		return CleanUtils.cleanLabel(routeLongName);
	}

	private static final String AGENCY_COLOR = "00A6E5"; // BLUE

	@Override
	public String getAgencyColor() {
		return AGENCY_COLOR;
	}

	@Override
	public String getRouteColor(GRoute gRoute) {
		if (COMMUNITY_BUS_RSN.equals(gRoute.getRouteShortName())) {
			return "D14625";
		}
		if (gRoute.getRouteShortName().startsWith("Zone ") //
				|| gRoute.getRouteShortName().startsWith("NYE ")) {
			return "ED1C24";
		}
		Matcher matcher = DIGITS.matcher(gRoute.getRouteShortName());
		if (matcher.find()) {
			int rsn = Integer.parseInt(matcher.group());
			switch (rsn) {
			// @formatter:off
			case 1: return "EC008C";
			case 2: return "EC008C";
			case 3: return "91469B";
			case 4: return "1988B7";
			case 5: return "921B1E";
			case 6: return "ED1C24";
			case 7: return "682C91";
			case 8: return "0082B1";
			case 9: return "5C7AAE";
			case 10: return "A54686";
			case 11: return "5C7AAE";
			case 12: return "008290";
			case 13: return "811167";
			case 14: return "485E88";
			case 15: return "8F7140";
			case 16: return "29712A";
			case 17: return "CB640A";
			case 18: return "CB640A";
			case 20: return "556940";
			case 40: return "005689";
			case 41: return "405D18";
			case 50: return "A54686"; // 50 U
			case 51: return "405D18"; // 51 U
			case 52: return "485E88"; // 52 U
			case 56: return "ED1C24"; // 56 U
			case 57: return "5C7AAE"; // 57 U
			case 58: return "91469b"; // 58 U
			case 99: return "4F832E ";
			// @formatter:on
			}
		}
		System.out.printf("\ngetRouteColor() > Unexpected route color for '%s'!\n", gRoute);
		System.exit(-1);
		return null;
	}

	private static HashMap<Long, RouteTripSpec> ALL_ROUTE_TRIPS2;
	static {
		HashMap<Long, RouteTripSpec> map2 = new HashMap<Long, RouteTripSpec>();
		map2.put(1L, new RouteTripSpec(1L, //
				MDirectionType.EAST.intValue(), MTrip.HEADSIGN_TYPE_DIRECTION, MDirectionType.EAST.getId(), //
				MDirectionType.WEST.intValue(), MTrip.HEADSIGN_TYPE_DIRECTION, MDirectionType.WEST.getId()) //
				.addTripSort(MDirectionType.EAST.intValue(), //
						Arrays.asList(new String[] { //
						"112", // Edinburgh at Laurelwood northbound
								"356", // ==
								"5844", // University Centre South Loop Platform 2
								"5845", // University Centre South Loop Platform 4
						})) //
				.addTripSort(MDirectionType.WEST.intValue(), //
						Arrays.asList(new String[] { //
						"5845", // University Centre South Loop Platform 4
								"106", // ++
								"112", // Edinburgh at Laurelwood northbound
						})) //
				.compileBothTripSort());
		map2.put(2L, new RouteTripSpec(2L, //
				MDirectionType.EAST.intValue(), MTrip.HEADSIGN_TYPE_DIRECTION, MDirectionType.EAST.getId(), //
				MDirectionType.WEST.intValue(), MTrip.HEADSIGN_TYPE_DIRECTION, MDirectionType.WEST.getId()) //
				.addTripSort(MDirectionType.EAST.intValue(), //
						Arrays.asList(new String[] { //
						"162", // Edinburgh at Ironwood southbound
								"168", // ++
								"5834", // University Centre North Loop Platform 10
						})) //
				.addTripSort(MDirectionType.WEST.intValue(), //
						Arrays.asList(new String[] { //
						"5847", // University Centre North Loop Platform 11
								"157", // ++
								"162", // Edinburgh at Ironwood southbound
						})) //
				.compileBothTripSort());
		map2.put(3L, new RouteTripSpec(3L, //
				MDirectionType.NORTH.intValue(), MTrip.HEADSIGN_TYPE_DIRECTION, MDirectionType.NORTH.getId(), //
				MDirectionType.SOUTH.intValue(), MTrip.HEADSIGN_TYPE_DIRECTION, MDirectionType.SOUTH.getId()) //
				.addTripSort(MDirectionType.NORTH.intValue(), //
						Arrays.asList(new String[] { //
						"5841", // Guelph Central Station Platform 21
								"305", // ++
								"231", // Woodlawn at Edinburgh eastbound
						})) //
				.addTripSort(MDirectionType.SOUTH.intValue(), //
						Arrays.asList(new String[] { //
						"231", // Woodlawn at Edinburgh eastbound
								"389", // Westmount at Kimberley southbound
								"392", // ++
								"1130", // ==
								"5837", // Guelph Central Station Platform 5
								"5841", // Guelph Central Station Platform 21
						})) //
				.compileBothTripSort());
		map2.put(4L, new RouteTripSpec(4L, //
				MDirectionType.EAST.intValue(), MTrip.HEADSIGN_TYPE_DIRECTION, MDirectionType.EAST.getId(), //
				MDirectionType.WEST.intValue(), MTrip.HEADSIGN_TYPE_DIRECTION, MDirectionType.WEST.getId()) //
				.addTripSort(MDirectionType.EAST.intValue(), //
						Arrays.asList(new String[] { //
						"5849", // Guelph Central Station Platform 2
								"408", // ++
								"416", // Watson at Guelph Transit southbound
						})) //
				.addTripSort(MDirectionType.WEST.intValue(), //
						Arrays.asList(new String[] { //
						"416", // Watson at Guelph Transit southbound
								"423", // ++
								"5850", // Guelph Central Station Platform 2
						})) //
				.compileBothTripSort());
		map2.put(5L, new RouteTripSpec(5L, //
				MDirectionType.NORTH.intValue(), MTrip.HEADSIGN_TYPE_DIRECTION, MDirectionType.NORTH.getId(), //
				MDirectionType.SOUTH.intValue(), MTrip.HEADSIGN_TYPE_DIRECTION, MDirectionType.SOUTH.getId()) //
				.addTripSort(MDirectionType.NORTH.intValue(), //
						Arrays.asList(new String[] { //
						"520", // Frederick at Waterford westbound
								"528", // Gordon at Lowes northbound
								"168", // ==
								"169", // !=
								"5844", // != University Centre South Loop Platform 2
								"5845", // != University Centre South Loop Platform 4
						})) //
				.addTripSort(MDirectionType.SOUTH.intValue(), //
						Arrays.asList(new String[] { //
						"5844", // University Centre South Loop Platform 2
								"5915", // ++
								"520", // Frederick at Waterford westbound
						})) //
				.compileBothTripSort());
		map2.put(6L, new RouteTripSpec(6L, //
				MDirectionType.EAST.intValue(), MTrip.HEADSIGN_TYPE_DIRECTION, MDirectionType.EAST.getId(), //
				MDirectionType.WEST.intValue(), MTrip.HEADSIGN_TYPE_DIRECTION, MDirectionType.WEST.getId()) //
				.addTripSort(MDirectionType.EAST.intValue(), //
						Arrays.asList(new String[] { //
						"615", // Ironwood at Kortright northbound
								"621", // ++
								"5843", // University Centre South Loop Platform 1
						})) //
				.addTripSort(MDirectionType.WEST.intValue(), //
						Arrays.asList(new String[] { //
						"5831", // University Centre South Loop Platform 3
								"608", // ++
								"615", // Ironwood at Kortright northbound
						})) //
				.compileBothTripSort());
		map2.put(7L, new RouteTripSpec(7L, //
				MDirectionType.EAST.intValue(), MTrip.HEADSIGN_TYPE_DIRECTION, MDirectionType.EAST.getId(), //
				MDirectionType.WEST.intValue(), MTrip.HEADSIGN_TYPE_DIRECTION, MDirectionType.WEST.getId()) //
				.addTripSort(MDirectionType.EAST.intValue(), //
						Arrays.asList(new String[] { //
						"5843", // University Centre South Loop Platform 1
								"706", // ++
								"709", // Downey at Woodland Glen westbound
								"713", // Ptarmigan at Downey eastbound
						})) //
				.addTripSort(MDirectionType.WEST.intValue(), //
						Arrays.asList(new String[] { //
						"713", // Ptarmigan at Downey eastbound
								"717", // Kortright at Ironwood eastbound
								"5831", // University Centre South Loop Platform 3
						})) //
				.compileBothTripSort());
		map2.put(8L, new RouteTripSpec(8L, //
				MDirectionType.NORTH.intValue(), MTrip.HEADSIGN_TYPE_DIRECTION, MDirectionType.NORTH.getId(), //
				MDirectionType.SOUTH.intValue(), MTrip.HEADSIGN_TYPE_DIRECTION, MDirectionType.SOUTH.getId()) //
				.addTripSort(MDirectionType.NORTH.intValue(), //
						Arrays.asList(new String[] { //
						"6047", // Stone Road Mall Platform 2
								"819", // ++
								"5849", // Guelph Central Station Platform 2
						})) //
				.addTripSort(MDirectionType.SOUTH.intValue(), //
						Arrays.asList(new String[] { //
						"5850", // Guelph Central Station Platform 3
								"808", // ++
								"6047", // Stone Road Mall Platform 2
						})) //
				.compileBothTripSort());
		map2.put(9L, new RouteTripSpec(9L, //
				MDirectionType.EAST.intValue(), MTrip.HEADSIGN_TYPE_DIRECTION, MDirectionType.EAST.getId(), //
				MDirectionType.WEST.intValue(), MTrip.HEADSIGN_TYPE_DIRECTION, MDirectionType.WEST.getId()) //
				.addTripSort(MDirectionType.EAST.intValue(), //
						Arrays.asList(new String[] { //
						"213", // Elmira at West Acres northbound
								"919", // ++
								"923", // ==
								"5833", // Guelph Central Station Platform 1
								"5852", // Guelph Central Station Platform 7
						})) //
				.addTripSort(MDirectionType.WEST.intValue(), //
						Arrays.asList(new String[] { //
						"5853", // Guelph Central Station Platform 8
								"904", // ++
								"213", // Elmira at West Acres northbound
						})) //
				.compileBothTripSort());
		map2.put(10L, new RouteTripSpec(10L, //
				MDirectionType.EAST.intValue(), MTrip.HEADSIGN_TYPE_DIRECTION, MDirectionType.EAST.getId(), //
				MDirectionType.WEST.intValue(), MTrip.HEADSIGN_TYPE_DIRECTION, MDirectionType.WEST.getId()) //
				.addTripSort(MDirectionType.EAST.intValue(), //
						Arrays.asList(new String[] { //
						"1015", // Imperial at Ferman southbound
								"1022", // ++
								"5860", // Guelph Central Station Platform 22
						})) //
				.addTripSort(MDirectionType.WEST.intValue(), //
						Arrays.asList(new String[] { //
						"5858", // Guelph Central Station Platform 19
								"1008", // ++
								"1015", // Imperial at Ferman southbound
						})) //
				.compileBothTripSort());
		map2.put(11L, new RouteTripSpec(11L, //
				MDirectionType.EAST.intValue(), MTrip.HEADSIGN_TYPE_DIRECTION, MDirectionType.EAST.getId(), //
				MDirectionType.WEST.intValue(), MTrip.HEADSIGN_TYPE_DIRECTION, MDirectionType.WEST.getId()) //
				.addTripSort(MDirectionType.EAST.intValue(), //
						Arrays.asList(new String[] { //
						"2035", // Silvercreek at Greengate southbound
								"1122", // Paisley at Yorkshire eastbound
								"5859", // Guelph Central Station Platform 20
						})) //
				.addTripSort(MDirectionType.WEST.intValue(), //
						Arrays.asList(new String[] { //
						"5851", // Guelph Central Station Platform 4
								"1105", // ++
								"2035", // Silvercreek at Greengate southbound
						})) //
				.compileBothTripSort());
		map2.put(12L, new RouteTripSpec(12L, //
				MDirectionType.NORTH.intValue(), MTrip.HEADSIGN_TYPE_DIRECTION, MDirectionType.NORTH.getId(), //
				MDirectionType.SOUTH.intValue(), MTrip.HEADSIGN_TYPE_DIRECTION, MDirectionType.SOUTH.getId()) //
				.addTripSort(MDirectionType.NORTH.intValue(), //
						Arrays.asList(new String[] { //
						"5860", // Guelph Central Station Platform 22
								"1207", // ++
								"1214", // Woodlawn at Victoria westbound
						})) //
				.addTripSort(MDirectionType.SOUTH.intValue(), //
						Arrays.asList(new String[] { //
						"1214", // Woodlawn at Victoria westbound
								"1221", // ++
								"5858", // Guelph Central Station Platform 19
						})) //
				.compileBothTripSort());
		map2.put(13L, new RouteTripSpec(13L, //
				MDirectionType.EAST.intValue(), MTrip.HEADSIGN_TYPE_DIRECTION, MDirectionType.EAST.getId(), //
				MDirectionType.WEST.intValue(), MTrip.HEADSIGN_TYPE_DIRECTION, MDirectionType.WEST.getId()) //
				.addTripSort(MDirectionType.EAST.intValue(), //
						Arrays.asList(new String[] { //
						"5837", // Guelph Central Station Platform 5
								"1313", // ++
								"370", // Eastview at Starwood westbound
						})) //
				.addTripSort(MDirectionType.WEST.intValue(), //
						Arrays.asList(new String[] { //
						"370", // Eastview at Starwood westbound
								"1324", // Eramosa at Orchard westbound
								"1130", // ==
								"5837", // Guelph Central Station Platform 5
								"5841", // Guelph Central Station Platform 21
						})) //
				.compileBothTripSort());
		map2.put(14l, new RouteTripSpec(14l, //
				MDirectionType.EAST.intValue(), MTrip.HEADSIGN_TYPE_DIRECTION, MDirectionType.EAST.getId(), //
				MDirectionType.WEST.intValue(), MTrip.HEADSIGN_TYPE_DIRECTION, MDirectionType.WEST.getId()) //
				.addTripSort(MDirectionType.EAST.intValue(), //
						Arrays.asList(new String[] { //
						"5859", // Guelph Central Station Platform 20
								"1406", // ++
								"332", // Watson at Fleming southbound
						})) //
				.addTripSort(MDirectionType.WEST.intValue(), //
						Arrays.asList(new String[] { //
						"332", // Watson at Fleming southbound
								"1418", // ++
								"5851", // Guelph Central Station Platform 4
						})) //
				.compileBothTripSort());
		map2.put(15L, new RouteTripSpec(15L, //
				MDirectionType.EAST.intValue(), MTrip.HEADSIGN_TYPE_DIRECTION, MDirectionType.EAST.getId(), //
				MDirectionType.WEST.intValue(), MTrip.HEADSIGN_TYPE_DIRECTION, MDirectionType.WEST.getId()) //
				.addTripSort(MDirectionType.EAST.intValue(), //
						Arrays.asList(new String[] { //
						"1508", // College at Flanders northbound
								"118", // College at Lynnwood eastbound
								"5847", // University Centre North Loop Platform 11
						})) //
				.addTripSort(MDirectionType.WEST.intValue(), //
						Arrays.asList(new String[] { //
						"5834", // University Centre North Loop Platform 10
								"207", // ++
								"1508", // College at Flanders northbound
						})) //
				.compileBothTripSort());
		map2.put(16L, new RouteTripSpec(16L, //
				MDirectionType.EAST.intValue(), MTrip.HEADSIGN_TYPE_DIRECTION, MDirectionType.EAST.getId(), //
				MDirectionType.WEST.intValue(), MTrip.HEADSIGN_TYPE_DIRECTION, MDirectionType.WEST.getId()) //
				.addTripSort(MDirectionType.EAST.intValue(), //
						Arrays.asList(new String[] { //
						"6058", // Clair at Gordon westbound
								"1619", // Laird at Clair westbound
								"1621", // ++
								"1624", // 485 Southgate southbound
						})) //
				.addTripSort(MDirectionType.WEST.intValue(), //
						Arrays.asList(new String[] { //
						"1624", // 485 Southgate southbound
								"1627", // ++
								"1631", // Clair at Laird eastbound
								"6058", // Clair at Gordon westbound
						})) //
				.compileBothTripSort());
		map2.put(17L, new RouteTripSpec(17L, //
				MDirectionType.NORTH.intValue(), MTrip.HEADSIGN_TYPE_DIRECTION, MDirectionType.NORTH.getId(), //
				MDirectionType.SOUTH.intValue(), MTrip.HEADSIGN_TYPE_DIRECTION, MDirectionType.SOUTH.getId()) //
				.addTripSort(MDirectionType.NORTH.intValue(), //
						Arrays.asList(new String[] { //
						"5836", // University Centre North Loop Platform 7
								"1501", // _++
								"219", // Imperial at Willow northbound
								"223", // Royal at ARC Industries northbound
								"320", // Inverness at Wilton northbound
								"321", // Victoria at Norma southbound
						})) //
				.addTripSort(MDirectionType.SOUTH.intValue(), //
						Arrays.asList(new String[] { //
						"321", // Victoria at Norma southbound
								"333", // Watson at Starwood southbound
								"338", // ++
								"339", // ++
								"340", // ++
								"5836", // University Centre North Loop Platform 7
								"5848", // University Centre North Loop Platform 12
						})) //
				.compileBothTripSort());
		map2.put(18L, new RouteTripSpec(18L, //
				MDirectionType.NORTH.intValue(), MTrip.HEADSIGN_TYPE_DIRECTION, MDirectionType.NORTH.getId(), //
				MDirectionType.SOUTH.intValue(), MTrip.HEADSIGN_TYPE_DIRECTION, MDirectionType.SOUTH.getId()) //
				.addTripSort(MDirectionType.NORTH.intValue(), //
						Arrays.asList(new String[] { //
						"5840", // University Centre South Loop Platform 6
								"366", // Watson at Fleming northbound
								"372", // Eastview at Victoria westbound
								"378", // ++ Victoria at Norma northbound
								"379", // Inverness at Wilton southbound
						})) //
				.addTripSort(MDirectionType.SOUTH.intValue(), //
						Arrays.asList(new String[] { //
						"379", // Inverness at Wilton southbound
								"278", // Elmira at West End Community Centre southbound
								"6047", // Stone Road Mall Platform 2
								"1520", // ==
								"5840", // != University Centre South Loop Platform 6
								"5848", // != University Centre North Loop Platform 12
						})) //
				.compileBothTripSort());
		map2.put(20L, new RouteTripSpec(20L, //
				MDirectionType.EAST.intValue(), MTrip.HEADSIGN_TYPE_DIRECTION, MDirectionType.EAST.getId(), //
				MDirectionType.WEST.intValue(), MTrip.HEADSIGN_TYPE_DIRECTION, MDirectionType.WEST.getId()) //
				.addTripSort(MDirectionType.EAST.intValue(), //
						Arrays.asList(new String[] { //
						"2028", // Imperial at Galaxy Cinema northbound
								"1130", // ==
								"5833", // Guelph Central Station Platform 1
								"5853", // Guelph Central Station Platform 8
						})) //
				.addTripSort(MDirectionType.WEST.intValue(), //
						Arrays.asList(new String[] { //
						"5833", // Guelph Central Station Platform 1
								"5863", // ++
								"2028", // Imperial at Galaxy Cinema northbound
						})) //
				.compileBothTripSort());
		map2.put(40L, new RouteTripSpec(40L, //
				MDirectionType.NORTH.intValue(), MTrip.HEADSIGN_TYPE_DIRECTION, MDirectionType.NORTH.getId(), //
				MDirectionType.SOUTH.intValue(), MTrip.HEADSIGN_TYPE_DIRECTION, MDirectionType.SOUTH.getId()) //
				.addTripSort(MDirectionType.NORTH.intValue(), //
						Arrays.asList(new String[] { //
						"6047", "6047", // Stone Road Mall Platform 2
								"5850", "5850", // Guelph Central Station Platform 3
						})) //
				.addTripSort(MDirectionType.SOUTH.intValue(), //
						Arrays.asList(new String[] { //
						"5850", // Guelph Central Station Platform 3
								"156", // College at Centennial westbound
								"6047", // Stone Road Mall Platform 2
						})) //
				.compileBothTripSort());
		map2.put(41L, new RouteTripSpec(41L, //
				MDirectionType.NORTH.intValue(), MTrip.HEADSIGN_TYPE_DIRECTION, MDirectionType.NORTH.getId(), // Downtown
				MDirectionType.SOUTH.intValue(), MTrip.HEADSIGN_TYPE_DIRECTION, MDirectionType.SOUTH.getId()) // University
				.addTripSort(MDirectionType.NORTH.intValue(), //
						Arrays.asList(new String[] { //
						"5848", // University Centre North Loop Platform 12
								"292", // ++
								"544", // ++
								"5839", // Guelph Central Station Platform 6
						})) //
				.addTripSort(MDirectionType.SOUTH.intValue(), //
						Arrays.asList(new String[] { //
						"5839", // Guelph Central Station Platform 6
								"501", // ++
								"356", // ==
								"5840", // University Centre South Loop Platform 6
								"5836", // University Centre North Loop Platform 7
								"5848", // University Centre North Loop Platform 12
						})) //
				.compileBothTripSort());
		map2.put(50L + RID_ENDS_WITH_U, new RouteTripSpec(50L + RID_ENDS_WITH_U, // 50U
				MDirectionType.EAST.intValue(), MTrip.HEADSIGN_TYPE_DIRECTION, MDirectionType.EAST.getId(), // University
				MDirectionType.WEST.intValue(), MTrip.HEADSIGN_TYPE_DIRECTION, MDirectionType.WEST.getId()) // Stone
				.addTripSort(MDirectionType.EAST.intValue(), //
						Arrays.asList(new String[] { //
						// "207", // Stone at Edinburgh westbound
								"114", // Stone at Scottsdale westbound
								"1516", // ++
								"5846", // University Centre North Loop Platform 8
						})) //
				.addTripSort(MDirectionType.WEST.intValue(), //
						Arrays.asList(new String[] { //
						"5846", // University Centre North Loop Platform 8
								"1501", // ++
								"207", // Stone at Edinburgh westbound
								"114", // Stone at Scottsdale westbound
						})) //
				.compileBothTripSort());
		map2.put(51L + RID_ENDS_WITH_U, new RouteTripSpec(51L + RID_ENDS_WITH_U, // 51U
				MDirectionType.EAST.intValue(), MTrip.HEADSIGN_TYPE_DIRECTION, MDirectionType.EAST.getId(), // University
				MDirectionType.WEST.intValue(), MTrip.HEADSIGN_TYPE_DIRECTION, MDirectionType.WEST.getId()) // Janefield
				.addTripSort(MDirectionType.EAST.intValue(), //
						Arrays.asList(new String[] { //
						"116", // Janefield at Mason northbound
								"1520", // ==
								"5845", // University Centre South Loop Platform 4
								"5847", // University Centre North Loop Platform 11
						})) //
				.addTripSort(MDirectionType.WEST.intValue(), //
						Arrays.asList(new String[] { //
						"5845", // University Centre South Loop Platform 4
								"115", // Janefield at Poppy northbound
								"116", // Janefield at Mason northbound
						})) //
				.compileBothTripSort());
		map2.put(52L + RID_ENDS_WITH_U, new RouteTripSpec(52L + RID_ENDS_WITH_U, // 52U
				MDirectionType.NORTH.intValue(), MTrip.HEADSIGN_TYPE_DIRECTION, MDirectionType.NORTH.getId(), // University
				MDirectionType.SOUTH.intValue(), MTrip.HEADSIGN_TYPE_DIRECTION, MDirectionType.SOUTH.getId()) // Kortright / Edinburgh
				.addTripSort(MDirectionType.NORTH.intValue(), //
						Arrays.asList(new String[] { //
						"166", // Edinburgh at Rickson eastbound
								"167", // Edinburgh at Carrington eastbound
								"171", // ++
								"5845", // University Centre South Loop Platform 4
						})) //
				.addTripSort(MDirectionType.SOUTH.intValue(), //
						Arrays.asList(new String[] { //
						"5847", // University Centre North Loop Platform 11
								"703,", // ++
								"166", // Edinburgh at Rickson eastbound
						})) //
				.compileBothTripSort());
		map2.put(56L + RID_ENDS_WITH_U, new RouteTripSpec(56L + RID_ENDS_WITH_U, // 56U
				MDirectionType.NORTH.intValue(), MTrip.HEADSIGN_TYPE_DIRECTION, MDirectionType.NORTH.getId(), // University
				MDirectionType.SOUTH.intValue(), MTrip.HEADSIGN_TYPE_DIRECTION, MDirectionType.SOUTH.getId()) // Colonial
				.addTripSort(MDirectionType.NORTH.intValue(), //
						Arrays.asList(new String[] { //
						"6014", // Goodwin at Samuel eastbound
								"5605", // Colonial at Lambeth Way northbound
								"169", // ++
								"5842", // University Centre South Loop Platform 0
						})) //
				.addTripSort(MDirectionType.SOUTH.intValue(), //
						Arrays.asList(new String[] { //
						"5842", // University Centre South Loop Platform 0
								"104", // ++
								"6014", // Goodwin at Samuel eastbound
						})) //
				.compileBothTripSort());
		map2.put(57L + RID_ENDS_WITH_U, new RouteTripSpec(57L + RID_ENDS_WITH_U, // 57U
				MDirectionType.EAST.intValue(), MTrip.HEADSIGN_TYPE_DIRECTION, MDirectionType.EAST.getId(), // University
				MDirectionType.WEST.intValue(), MTrip.HEADSIGN_TYPE_DIRECTION, MDirectionType.WEST.getId()) // Ironwood
				.addTripSort(MDirectionType.EAST.intValue(), //
						Arrays.asList(new String[] { //
						"5706", // Ironwood at Reid westbound
								"5709", // ++
								"5846", // University Centre North Loop Platform 8
						})) //
				.addTripSort(MDirectionType.WEST.intValue(), //
						Arrays.asList(new String[] { //
						"5838", // University Centre North Loop Platform 9
								"1503", // ++
								"5706", // Ironwood at Reid westbound
						})) //
				.compileBothTripSort());
		map2.put(58L + RID_ENDS_WITH_U, new RouteTripSpec(58L + RID_ENDS_WITH_U, // 58U
				MDirectionType.EAST.intValue(), MTrip.HEADSIGN_TYPE_DIRECTION, MDirectionType.EAST.getId(), // University
				MDirectionType.WEST.intValue(), MTrip.HEADSIGN_TYPE_DIRECTION, MDirectionType.WEST.getId()) // Edinburgh
				.addTripSort(MDirectionType.EAST.intValue(), //
						Arrays.asList(new String[] { //
						"162", // Edinburgh at Ironwood southbound
								"721", // ++
								"5838", // University Centre North Loop Platform 9
						})) //
				.addTripSort(MDirectionType.WEST.intValue(), //
						Arrays.asList(new String[] { //
						"5846", // University Centre North Loop Platform 8
								"1501", // ++
								"162", // Edinburgh at Ironwood southbound
						})) //
				.compileBothTripSort());
		map2.put(99L, new RouteTripSpec(99L, //
				MDirectionType.NORTH.intValue(), MTrip.HEADSIGN_TYPE_DIRECTION, MDirectionType.NORTH.getId(), //
				MDirectionType.SOUTH.intValue(), MTrip.HEADSIGN_TYPE_DIRECTION, MDirectionType.SOUTH.getId()) //
				.addTripSort(MDirectionType.NORTH.intValue(), //
						Arrays.asList(new String[] { //
						"6058", // Clair at Gordon westbound
								"1634", // ++ Gosling Gardens at Gosling Garden Park northbound
								"5848", // University Centre North Loop Platform 12
								"544", // == !=
								"6067", // == <>
								"5853", // != Guelph Central Station Platform 8 => END
								"5852", // != <> Guelph Central Station Platform 7 >> SOUTH
								"5857", // != Guelph Central Station Platform 18 >> CONTINUE
								"6068", // <>
								"1101", // !=
								"6055", // Woodlawn Smart Centres Platform 1
						})) //
				.addTripSort(MDirectionType.SOUTH.intValue(), //
						Arrays.asList(new String[] { //
						"6055", // Woodlawn Smart Centres Platform 1
								"233", // ++ Woodlawn at Woolwich eastbound
								"6094", // !=
								"6067", // <>
								"5852", // <> Guelph Central Station Platform 7
								"6068", // <>
								"501", // !=
								"1602", // Gordon at Water southbound
								"5832", // University Centre South Loop Platform 5
								"1615", // ++ Gordon at Clairfields southbound
								"6058", // Clair at Gordon westbound
						})) //
				.compileBothTripSort());
		ALL_ROUTE_TRIPS2 = map2;
	}

	@Override
	public int compareEarly(long routeId, List<MTripStop> list1, List<MTripStop> list2, MTripStop ts1, MTripStop ts2, GStop ts1GStop, GStop ts2GStop) {
		if (ALL_ROUTE_TRIPS2.containsKey(routeId)) {
			return ALL_ROUTE_TRIPS2.get(routeId).compare(routeId, list1, list2, ts1, ts2, ts1GStop, ts2GStop, this);
		}
		return super.compareEarly(routeId, list1, list2, ts1, ts2, ts1GStop, ts2GStop);
	}

	@Override
	public ArrayList<MTrip> splitTrip(MRoute mRoute, GTrip gTrip, GSpec gtfs) {
		if (ALL_ROUTE_TRIPS2.containsKey(mRoute.getId())) {
			return ALL_ROUTE_TRIPS2.get(mRoute.getId()).getAllTrips();
		}
		return super.splitTrip(mRoute, gTrip, gtfs);
	}

	@Override
	public Pair<Long[], Integer[]> splitTripStop(MRoute mRoute, GTrip gTrip, GTripStop gTripStop, ArrayList<MTrip> splitTrips, GSpec routeGTFS) {
		if (ALL_ROUTE_TRIPS2.containsKey(mRoute.getId())) {
			return SplitUtils.splitTripStop(mRoute, gTrip, gTripStop, routeGTFS, ALL_ROUTE_TRIPS2.get(mRoute.getId()), this);
		}
		return super.splitTripStop(mRoute, gTrip, gTripStop, splitTrips, routeGTFS);
	}

	private static final String DASH = "-";

	@Override
	public void setTripHeadsign(MRoute mRoute, MTrip mTrip, GTrip gTrip, GSpec gtfs) {
		if (ALL_ROUTE_TRIPS2.containsKey(mRoute.getId())) {
			return; // split
		}
		if (mRoute.getId() == COMMUNITY_BUS_RID) {
			if (gTrip.getDirectionId() == 0 && gTrip.getTripHeadsign().equals("Community Bus North Loop")) {
				mTrip.setHeadsignDirection(MDirectionType.NORTH);
				return;
			} else if (gTrip.getDirectionId() == 1 && gTrip.getTripHeadsign().equals("Community Bus South Loop")) {
				mTrip.setHeadsignDirection(MDirectionType.SOUTH);
				return;
			}
		}
		if (isGoodEnoughAccepted()) {
			if (StringUtils.isEmpty(gTrip.getTripHeadsign())) {
				if (mRoute.getId() == 972L) { // Zone 1
					mTrip.setHeadsignString("WHanKor", gTrip.getDirectionId());
					return;
				} else if (mRoute.getId() == 973L) { // Zone 2
					mTrip.setHeadsignString("VicClr", gTrip.getDirectionId());
					return;
				} else if (mRoute.getId() == 974L) { // Zone 3
					mTrip.setHeadsignString("North West", gTrip.getDirectionId());
					return;
				} else if (mRoute.getId() == 975L) { // Zone 4
					mTrip.setHeadsignString("North Central", gTrip.getDirectionId());
					return;
				} else if (mRoute.getId() == 976L) { // Zone 5
					mTrip.setHeadsignString("North East", gTrip.getDirectionId());
					return;
				} else if (mRoute.getId() == 977L) { // Zone 6
					mTrip.setHeadsignString("West Central", gTrip.getDirectionId());
					return;
				}
			}
			if (mRoute.getId() == 978L) { // NYE 99
				mTrip.setHeadsignString(gTrip.getTripHeadsign(), gTrip.getDirectionId());
				return;
			}
		}
		System.out.printf("\nUnexpected trip headsign for %s!\n", gTrip);
		System.exit(-1);
		return;
	}

	@Override
	public String cleanTripHeadsign(String tripHeadsign) {
		tripHeadsign = CleanUtils.cleanStreetTypes(tripHeadsign);
		return CleanUtils.cleanLabel(tripHeadsign);
	}

	private static final Pattern CLEAN_DEPART_ARRIVE = Pattern.compile("( (arrival|depart)$)", Pattern.CASE_INSENSITIVE);

	@Override
	public String cleanStopName(String gStopName) {
		gStopName = CleanUtils.removePoints(gStopName);
		gStopName = CleanUtils.CLEAN_AT.matcher(gStopName).replaceAll(CleanUtils.CLEAN_AT_REPLACEMENT);
		gStopName = CLEAN_DEPART_ARRIVE.matcher(gStopName).replaceAll(StringUtils.EMPTY);
		gStopName = CleanUtils.cleanNumbers(gStopName);
		gStopName = CleanUtils.cleanStreetTypes(gStopName);
		return CleanUtils.cleanLabel(gStopName);
	}

	private static final String UNDERSCORE = "_";

	@Override
	public int getStopId(GStop gStop) {
		if (gStop.getStopId().equals("Route5A-0549_Victoria Road South at Macalister Boulevard southbound")) {
			return 619;
		}
		if (gStop.getStopCode() != null && gStop.getStopCode().length() > 0 && Utils.isDigitsOnly(gStop.getStopCode())) {
			return Integer.valueOf(gStop.getStopCode());
		}
		int indexOfDASH = gStop.getStopId().indexOf(DASH);
		int indexOfUNDERSCORE = gStop.getStopId().indexOf(UNDERSCORE, indexOfDASH);
		if (indexOfDASH >= 0 && indexOfUNDERSCORE >= 0) {
			return Integer.valueOf(gStop.getStopId().substring(indexOfDASH + 1, indexOfUNDERSCORE));
		}
		System.out.printf("\nError while getting stop ID for %s!\n", gStop);
		System.exit(-1);
		return -1;
	}
}
