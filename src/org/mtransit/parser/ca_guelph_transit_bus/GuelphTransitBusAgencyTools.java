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

// http://open.guelph.ca/dataset/guelph-transit-gtfs-data/
// http://guelph.ca/uploads/google/google_transit.zip
// http://openguelph.wpengine.com/wp-content/uploads/2014/02/google_transit.zip
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
	public Integer getAgencyRouteType() {
		return MAgency.ROUTE_TYPE_BUS;
	}

	private static final Pattern DIGITS = Pattern.compile("[\\d]+");

	private static final String ROUTE_GORDON_CORRIDOR = "Route Gordon Corridor";
	private static final String GORDON_CORRIDOR = "Gordon Corridor";
	private static final String GORDON_CORRIDOR_RSN = "GC";
	private static final long GORDON_CORRIDOR_RID = 9999l;

	private static final String A = "A";
	private static final String B = "B";

	private static final long RID_STARTS_WITH_A = 1000l;
	private static final long RID_STARTS_WITH_B = 2000l;

	@Override
	public long getRouteId(GRoute gRoute) {
		if (ROUTE_GORDON_CORRIDOR.equals(gRoute.getRouteId())) {
			return GORDON_CORRIDOR_RID;
		}
		String routeShortName = gRoute.getRouteShortName();
		if (routeShortName != null && routeShortName.length() > 0 && Utils.isDigitsOnly(routeShortName)) {
			return Long.parseLong(routeShortName); // using route short name as route ID
		}
		Matcher matcher = DIGITS.matcher(routeShortName);
		if (!matcher.find()) {
			System.out.printf("\nCan't find route ID digits for %s (%s)!\n", routeShortName, gRoute);
			System.exit(-1);
			return -1l;
		}
		int digits = Integer.parseInt(matcher.group());
		if (routeShortName.endsWith(A)) {
			return RID_STARTS_WITH_A + digits;
		} else if (routeShortName.endsWith(B)) {
			return RID_STARTS_WITH_B + digits;
		} else {
			System.out.printf("\nCan't find route ID for %s (%s)!\n", routeShortName, gRoute);
			System.exit(-1);
			return -1l;
		}
	}

	@Override
	public String getRouteShortName(GRoute gRoute) {
		if (ROUTE_GORDON_CORRIDOR.equals(gRoute.getRouteId())) {
			return GORDON_CORRIDOR_RSN;
		}
		return super.getRouteShortName(gRoute);
	}

	private static final String COLLEGE_EDINBURGH_CLOCKWISE = "College Edinburgh (Clockwise)";
	private static final String COLLEGE_EDINBURGH_COUNTER_CLOCKWISE = "College Edinburgh (Counter Clockwise)";
	private static final String WEST_LOOP_CLOCKWISE = "West Loop (Clockwise)";
	private static final String WEST_LOOP_COUNTER_CLOCKWISE = "West Loop (Counter Clockwise)";
	private static final String EAST_LOOP_CLOCKWISE = "East Loop (Clockwise)";
	private static final String EAST_LOOP_COUNTER_CLOCKWISE = "East Loop (Counter Clockwise)";
	private static final String YORK = "York";
	private static final String GORDON = "Gordon";
	private static final String HARVARD_IRONWOOD = "Harvard Ironwood";
	private static final String KORTRIGHT_DOWNEY = "Kortright Downey";
	private static final String STONE_ROAD_MALL = "Stone Road Mall";
	private static final String WATERLOO = "Waterloo";
	private static final String WILLOW_WEST = "Willow West";
	private static final String IMPERIAL = "Imperial";
	private static final String VICTORIA_ROAD_RECREATION_CENTRE = "Victoria Road Recreation Centre";
	private static final String GRANGE = "Grange";
	private static final String UNIVERSITY_COLLEGE = "University College";
	private static final String NORTHWEST_INDUSTRIAL = "Northwest Industrial";
	private static final String EDINBURGH_EXPRESS = "Edinburgh Express";
	private static final String HARVARD_EXPRESS = "Harvard Express";
	private static final String VICTORIA_EXPRESS = "Victoria Express";
	private static final String STONE_ROAD_EXPRESS = "Stone Road Express";
	private static final String SOUTHGATE = "Southgate";
	private static final String GENERAL_HOSPITAL = "General Hospital";

	private static final String RSN_58 = "58";
	private static final String RSN_57 = "57";
	private static final String RSN_56 = "56";
	private static final String RSN_50 = "50";
	private static final String RSN_20 = "20";
	private static final String RSN_16 = "16";
	private static final String RSN_15 = "15";
	private static final String RSN_14 = "14";
	private static final String RSN_13 = "13";
	private static final String RSN_12 = "12";
	private static final String RSN_11 = "11";
	private static final String RSN_10 = "10";
	private static final String RSN_9 = "9";
	private static final String RSN_8 = "8";
	private static final String RSN_7 = "7";
	private static final String RSN_6 = "6";
	private static final String RSN_5 = "5";
	private static final String RSN_4 = "4";
	private static final String RSN_3B = "3B";
	private static final String RSN_3A = "3A";
	private static final String RSN_2B = "2B";
	private static final String RSN_2A = "2A";
	private static final String RSN_1B = "1B";
	private static final String RSN_1A = "1A";

	private static final Pattern STARTS_WITH_ROUTE_RSN = Pattern.compile("(route[\\d]*[A-Z]*[\\-]?[\\s]*)", Pattern.CASE_INSENSITIVE);

	@Override
	public String getRouteLongName(GRoute gRoute) {
		String routeLongName = gRoute.getRouteLongName();
		routeLongName = STARTS_WITH_ROUTE_RSN.matcher(routeLongName).replaceAll(StringUtils.EMPTY);
		if (StringUtils.isEmpty(routeLongName)) {
			if (ROUTE_GORDON_CORRIDOR.equals(gRoute.getRouteId())) {
				return GORDON_CORRIDOR;
			}
			// @formatter:off
			if (RSN_1A.equalsIgnoreCase(gRoute.getRouteShortName())) { return COLLEGE_EDINBURGH_CLOCKWISE;	}
			if (RSN_1B.equalsIgnoreCase(gRoute.getRouteShortName())) { return COLLEGE_EDINBURGH_COUNTER_CLOCKWISE;	}
			if (RSN_2A.equalsIgnoreCase(gRoute.getRouteShortName())) { return WEST_LOOP_CLOCKWISE;	}
			if (RSN_2B.equalsIgnoreCase(gRoute.getRouteShortName())) { return WEST_LOOP_COUNTER_CLOCKWISE;	}
			if (RSN_3A.equalsIgnoreCase(gRoute.getRouteShortName())) { return EAST_LOOP_CLOCKWISE;	}
			if (RSN_3B.equalsIgnoreCase(gRoute.getRouteShortName())) { return EAST_LOOP_COUNTER_CLOCKWISE;	}
			if (RSN_4.equalsIgnoreCase(gRoute.getRouteShortName())) { return YORK; }
			if (RSN_5.equalsIgnoreCase(gRoute.getRouteShortName())) { return GORDON; }
			if (RSN_6.equalsIgnoreCase(gRoute.getRouteShortName())) { return HARVARD_IRONWOOD; }
			if (RSN_7.equalsIgnoreCase(gRoute.getRouteShortName())) { return KORTRIGHT_DOWNEY; }
			if (RSN_8.equalsIgnoreCase(gRoute.getRouteShortName())) { return STONE_ROAD_MALL; }
			if (RSN_9.equalsIgnoreCase(gRoute.getRouteShortName())) { return WATERLOO; }
			if (RSN_10.equalsIgnoreCase(gRoute.getRouteShortName())) { return IMPERIAL; }
			if (RSN_11.equalsIgnoreCase(gRoute.getRouteShortName())) { return WILLOW_WEST; }
			if (RSN_12.equalsIgnoreCase(gRoute.getRouteShortName())) { return GENERAL_HOSPITAL; }
			if (RSN_13.equalsIgnoreCase(gRoute.getRouteShortName())) { return VICTORIA_ROAD_RECREATION_CENTRE; }
			if (RSN_14.equalsIgnoreCase(gRoute.getRouteShortName())) { return GRANGE; }
			if (RSN_15.equalsIgnoreCase(gRoute.getRouteShortName())) { return UNIVERSITY_COLLEGE; }
			if (RSN_16.equalsIgnoreCase(gRoute.getRouteShortName())) { return SOUTHGATE; }
			if (RSN_20.equalsIgnoreCase(gRoute.getRouteShortName())) { return NORTHWEST_INDUSTRIAL; }
			if (RSN_50.equalsIgnoreCase(gRoute.getRouteShortName())) { return STONE_ROAD_EXPRESS; }
			if (RSN_56.equalsIgnoreCase(gRoute.getRouteShortName())) { return VICTORIA_EXPRESS; }
			if (RSN_57.equalsIgnoreCase(gRoute.getRouteShortName())) { return HARVARD_EXPRESS; }
			if (RSN_58.equalsIgnoreCase(gRoute.getRouteShortName())) { return EDINBURGH_EXPRESS; }
			// @formatter:on
			System.out.printf("\ngetRouteLongName() > Unexpected route short name '%s' (%s)!\n", gRoute.getRouteShortName(), gRoute);
			System.exit(-1);
			return null;
		} else {
			return CleanUtils.cleanLabel(routeLongName);
		}
	}

	private static final String AGENCY_COLOR = "00A6E5"; // BLUE

	@Override
	public String getAgencyColor() {
		return AGENCY_COLOR;
	}

	private static final String COLOR_F57215 = "F57215";
	private static final String COLOR_3D3A9B = "3D3A9B";
	private static final String COLOR_0A904B = "0A904B";
	private static final String COLOR_A52D84 = "A52D84";
	private static final String COLOR_90191E = "90191E";
	private static final String COLOR_6BA630 = "6BA630";
	private static final String COLOR_5F2490 = "5F2490";
	private static final String COLOR_F9A720 = "F9A720";
	private static final String COLOR_EC008C = "EC008C";
	private static final String COLOR_5A74B4 = "5A74B4";
	private static final String COLOR_007C8E = "007C8E";
	private static final String COLOR_475683 = "475683";
	private static final String COLOR_F2E827 = "F2E827";
	private static final String COLOR_9E7C3E = "9E7C3E";
	private static final String COLOR_4B6639 = "4B6639";
	private static final String COLOR_00ADEF = "00ADEF";
	private static final String COLOR_8D319D = "8D319D";
	private static final String COLOR_ED1C24 = "ED1C24";
	private static final String COLOR_66C530 = "66C530";

	@Override
	public String getRouteColor(GRoute gRoute) {
		if (ROUTE_GORDON_CORRIDOR.equals(gRoute.getRouteId())) {
			return null;
		}
		// @formatter:off
		if (RSN_1A.equalsIgnoreCase(gRoute.getRouteShortName())) { return COLOR_F57215; }
		if (RSN_1B.equalsIgnoreCase(gRoute.getRouteShortName())) { return COLOR_F57215; }
		if (RSN_2A.equalsIgnoreCase(gRoute.getRouteShortName())) { return COLOR_3D3A9B; }
		if (RSN_2B.equalsIgnoreCase(gRoute.getRouteShortName())) { return COLOR_3D3A9B; }
		if (RSN_3A.equalsIgnoreCase(gRoute.getRouteShortName())) { return COLOR_0A904B; }
		if (RSN_3B.equalsIgnoreCase(gRoute.getRouteShortName())) { return COLOR_0A904B; }
		if (RSN_4.equalsIgnoreCase(gRoute.getRouteShortName())) { return COLOR_A52D84;	}
		if (RSN_5.equalsIgnoreCase(gRoute.getRouteShortName())) { return COLOR_90191E; }
		if (RSN_6.equalsIgnoreCase(gRoute.getRouteShortName())) { return COLOR_6BA630; }
		if (RSN_7.equalsIgnoreCase(gRoute.getRouteShortName())) { return COLOR_5F2490; }
		if (RSN_8.equalsIgnoreCase(gRoute.getRouteShortName())) { return COLOR_00ADEF; }
		if (RSN_9.equalsIgnoreCase(gRoute.getRouteShortName())) { return COLOR_F9A720; }
		if (RSN_10.equalsIgnoreCase(gRoute.getRouteShortName())) { return COLOR_EC008C; }
		if (RSN_11.equalsIgnoreCase(gRoute.getRouteShortName())) { return COLOR_5A74B4; }
		if (RSN_12.equalsIgnoreCase(gRoute.getRouteShortName())) { return COLOR_007C8E; }
		if (RSN_13.equalsIgnoreCase(gRoute.getRouteShortName())) { return COLOR_ED1C24; }
		if (RSN_14.equalsIgnoreCase(gRoute.getRouteShortName())) { return COLOR_475683; }
		if (RSN_15.equalsIgnoreCase(gRoute.getRouteShortName())) { return COLOR_F2E827; }
		if (RSN_16.equalsIgnoreCase(gRoute.getRouteShortName())) { return COLOR_9E7C3E; }
		if (RSN_20.equalsIgnoreCase(gRoute.getRouteShortName())) { return COLOR_4B6639; }
		if (RSN_50.equalsIgnoreCase(gRoute.getRouteShortName())) { return COLOR_00ADEF; }
		if (RSN_56.equalsIgnoreCase(gRoute.getRouteShortName())) { return COLOR_8D319D; }
		if (RSN_57.equalsIgnoreCase(gRoute.getRouteShortName())) { return COLOR_ED1C24; }
		if (RSN_58.equalsIgnoreCase(gRoute.getRouteShortName())) { return COLOR_66C530; }
		// @formatter:on
		System.out.printf("\ngetRouteColor() > Unexpected route short name '%s' (%s)!\n", gRoute.getRouteShortName(), gRoute);
		System.exit(-1);
		return null;
	}

	private static HashMap<Long, RouteTripSpec> ALL_ROUTE_TRIPS2;
	static {
		HashMap<Long, RouteTripSpec> map2 = new HashMap<Long, RouteTripSpec>();
		map2.put(GORDON_CORRIDOR_RID, new RouteTripSpec(GORDON_CORRIDOR_RID, //
				MDirectionType.NORTH.intValue(), MTrip.HEADSIGN_TYPE_DIRECTION, MDirectionType.NORTH.getId(), //
				MDirectionType.SOUTH.intValue(), MTrip.HEADSIGN_TYPE_DIRECTION, MDirectionType.SOUTH.getId()) //
				.addTripSort(MDirectionType.NORTH.intValue(), //
						Arrays.asList(new String[] { //
						"Route Gordon Corridor-11_Gosling Gardens at Clair Rd. W.", //
								"Route Gordon Corridor-18_Gordon St. at Valley Rd.", //
								"Route Gordon Corridor-24_Gordon St. at Monticello Cres.", //
								"Route Gordon Corridor-25_Gordon St. at South Ring Rd. E.", //
								"Route Gordon Corridor-25_University Centre" })) //
				.addTripSort(MDirectionType.SOUTH.intValue(), //
						Arrays.asList(new String[] { //
						"Route Gordon Corridor-0_University Dep", //
								"Route Gordon Corridor-5_1155 Gordon St.", //
								"Route Gordon Corridor-11_Gosling Gardens at Clair Rd. W." })) //
				.compileBothTripSort());
		map2.put(1l + RID_STARTS_WITH_A, new RouteTripSpec(1l + RID_STARTS_WITH_A, // 1A
				MDirectionType.EAST.intValue(), MTrip.HEADSIGN_TYPE_DIRECTION, MDirectionType.EAST.getId(), //
				MDirectionType.WEST.intValue(), MTrip.HEADSIGN_TYPE_DIRECTION, MDirectionType.WEST.getId()) //
				.addTripSort(MDirectionType.EAST.intValue(), //
						Arrays.asList(new String[] { //
						"Route1A-0115_181 Janefield Ave.", //
								"Route1A-0119_50 College Ave. W.", //
								"Route1A-0123_UC South Loop Plat2" })) //
				.addTripSort(MDirectionType.WEST.intValue(), //
						Arrays.asList(new String[] { //
						"Route1A-0100_UC South Loop Plat3", //
								"Route1A-0112_Edinburgh Rd. S. at Laurelwood Crt.", //
								"Route1A-0115_181 Janefield Ave." })) //
				.compileBothTripSort());
		map2.put(1l + RID_STARTS_WITH_B, new RouteTripSpec(1l + RID_STARTS_WITH_B, // 1B
				MDirectionType.EAST.intValue(), MTrip.HEADSIGN_TYPE_DIRECTION, MDirectionType.EAST.getId(), //
				MDirectionType.WEST.intValue(), MTrip.HEADSIGN_TYPE_DIRECTION, MDirectionType.WEST.getId()) //
				.addTripSort(MDirectionType.EAST.intValue(), //
						Arrays.asList(new String[] { //
						"Route1B-0162_583 Edinburgh Rd. S.", //
								"Route1B-0168_Gordon St. at Valley Rd.", //
								"Route1B-0175_University Centre" })) //
				.addTripSort(MDirectionType.WEST.intValue(), //
						Arrays.asList(new String[] { //
						"Route1B-0150_University Centre",//
								"Route1B-0157_129 Janefield Ave.", //
								"Route1B-0162_583 Edinburgh Rd. S." })) //
				.compileBothTripSort());
		map2.put(2l + RID_STARTS_WITH_A, new RouteTripSpec(2l + RID_STARTS_WITH_A, // 2A
				MDirectionType.EAST.intValue(), MTrip.HEADSIGN_TYPE_DIRECTION, MDirectionType.EAST.getId(), //
				MDirectionType.WEST.intValue(), MTrip.HEADSIGN_TYPE_DIRECTION, MDirectionType.WEST.getId()) //
				.addTripSort(MDirectionType.EAST.intValue(), //
						Arrays.asList(new String[] { //
						"2A-0216_Elmira Rd. at Zehrs Depart", //
								"2A-0237_Woolwich St. at Tiffany St.", //
								"2A-0241_Guelph Central Station" })) //
				.addTripSort(MDirectionType.WEST.intValue(), //
						Arrays.asList(new String[] { //
						"2A-0200_Guelph Central Station", //
								"2A-0203_Gordon St. at Forbes Ave.", //
								"2A-0206_University Centre Arrive", //
								"2A-0216_Elmira Rd. at Zehrs Depart" })) //
				.compileBothTripSort());
		map2.put(2l + RID_STARTS_WITH_B, new RouteTripSpec(2l + RID_STARTS_WITH_B, // 2B
				MDirectionType.EAST.intValue(), MTrip.HEADSIGN_TYPE_DIRECTION, MDirectionType.EAST.getId(), //
				MDirectionType.WEST.intValue(), MTrip.HEADSIGN_TYPE_DIRECTION, MDirectionType.WEST.getId()) //
				.addTripSort(MDirectionType.EAST.intValue(), //
						Arrays.asList(new String[] { //
						"Route2B-0279_Elmira Rd.at Paisley Depart", //
								"Route2B-0291_University Centre Arrive", //
								"Route2B-0298_Guelph Central Station" })) //
				.addTripSort(MDirectionType.WEST.intValue(), //
						Arrays.asList(new String[] { //
						"Route2B-0250_Guelph Central Station", //
								"Route2B-0259_Woolwich St. at Evergreen", //
								"Route2B-0268_Woodlawn Rd. W. at Royal Rd.", //
								"Route2B-0279_Elmira Rd.at Paisley Depart" })) //
				.compileBothTripSort());
		map2.put(3l + RID_STARTS_WITH_A, new RouteTripSpec(3l + RID_STARTS_WITH_A, // 3A
				MDirectionType.EAST.intValue(), MTrip.HEADSIGN_TYPE_DIRECTION, MDirectionType.EAST.getId(), //
				MDirectionType.WEST.intValue(), MTrip.HEADSIGN_TYPE_DIRECTION, MDirectionType.WEST.getId()) //
				.addTripSort(MDirectionType.EAST.intValue(), //
						Arrays.asList(new String[] { //
						"Route3A-0300_GCS East Plat5", //
								"Route3A-0306_Yorkshire St. N. at London Rd. W.", //
								"Route3A-0313_Nicklin Rd. at Woodlawn Rd. Depart", //
								"Route3A-0327_Eastview Rd. at Glenburnie Dr. Depart", //
								"Route3A-0333_Watson Pkwy N. at Library" })) //
				.addTripSort(MDirectionType.WEST.intValue(), //
						Arrays.asList(new String[] { //
						"Route3A-0333_Watson Pkwy N. at Library", //
								"Route3A-0341_UC North Plat9 Arrive", //
								"Route3A-0349_GCS East Plat5" })) //
				.compileBothTripSort());
		map2.put(3l + RID_STARTS_WITH_B, new RouteTripSpec(3l + RID_STARTS_WITH_B, // 3B
				MDirectionType.EAST.intValue(), MTrip.HEADSIGN_TYPE_DIRECTION, MDirectionType.EAST.getId(), //
				MDirectionType.WEST.intValue(), MTrip.HEADSIGN_TYPE_DIRECTION, MDirectionType.WEST.getId()) //
				.addTripSort(MDirectionType.EAST.intValue(), //
						Arrays.asList(new String[] { //
						"Route3B-0350_GCS Mid Plat6", //
								"Route3B-0353_Gordon St. at Water St.", //
								"Route3B-0357_UC South Plat6 Arr", //
								"Route3B-0365_Watson Pkwy. N. opposite Library" })) //
				.addTripSort(MDirectionType.WEST.intValue(), //
						Arrays.asList(new String[] { //
						"Route3B-0365_Watson Pkwy. N. opposite Library", //
								"Route3B-0372_Eastview Rd. at Victoria Rd.", //
								"Route3B-0373_211 Victoria Rd. N.", //
								"Route3B-0378_Victoria Rd. N. opposite Trillium Waldorf", //
								"Route3B-0393_Westmount Rd. at London Rd. W.", //
								"Route3B-0399_GCS Mid Plat6" })) //
				.compileBothTripSort());
		map2.put(4l, new RouteTripSpec(4l, //
				MDirectionType.EAST.intValue(), MTrip.HEADSIGN_TYPE_DIRECTION, MDirectionType.EAST.getId(), //
				MDirectionType.WEST.intValue(), MTrip.HEADSIGN_TYPE_DIRECTION, MDirectionType.WEST.getId()) //
				.addTripSort(MDirectionType.EAST.intValue(), //
						Arrays.asList(new String[] { //
						"Route4-400_Guelph Central Station", //
								"Route4-408_535 York Rd. at Tim Hortons", //
								"Route4-416_Watson Rd. S. at Guelph Transit Centre" })) //
				.addTripSort(MDirectionType.WEST.intValue(), //
						Arrays.asList(new String[] { //
						"Route4-417_Watson Pkwy. S. at Dunlop Dr.", //
								"Route4-423_York Rd. at Beaumont Cres.", //
								"Route4-430_Guelph Central Station" })) //
				.compileBothTripSort());
		map2.put(5l, new RouteTripSpec(5l, //
				MDirectionType.NORTH.intValue(), MTrip.HEADSIGN_TYPE_DIRECTION, MDirectionType.NORTH.getId(), //
				MDirectionType.SOUTH.intValue(), MTrip.HEADSIGN_TYPE_DIRECTION, MDirectionType.SOUTH.getId()) //
				.addTripSort(MDirectionType.NORTH.intValue(), //
						Arrays.asList(new String[] { //
						"Route5-0520_Frederick Dr. at Waterford Dr.", //
								"Route5-0528_Gordon St. at Lowes Rd.", //
								"Route5-0538_University Centre Arrive", //
								"Route5-0507_University Centre", //
								"Route5-0546_Guelph Central Station" })) //
				.addTripSort(MDirectionType.SOUTH.intValue(), //
						Arrays.asList(new String[] { //
						"Route5-0500_Guelph Central Station", //
								"Route5-0503_Gordon St. at Water St.", //
								"Route5-0507_University Centre Arrive", //
								"Route5-0520_Frederick Dr. at Waterford Dr." })) //
				.compileBothTripSort());
		map2.put(6l, new RouteTripSpec(6l, //
				MDirectionType.EAST.intValue(), MTrip.HEADSIGN_TYPE_DIRECTION, MDirectionType.EAST.getId(), //
				MDirectionType.WEST.intValue(), MTrip.HEADSIGN_TYPE_DIRECTION, MDirectionType.WEST.getId()) //
				.addTripSort(MDirectionType.EAST.intValue(), //
						Arrays.asList(new String[] { //
						"Route6-0613_Ironwood Rd. at Hilldale Cres.", //
								"Route6-0618_692 Scottsdale Dr.", //
								"Route6-0622_Youngman Dr. at Harvard Rd.",//
								"Route6-0627_UC South Loop Plat3" })) //
				.addTripSort(MDirectionType.WEST.intValue(), //
						Arrays.asList(new String[] { //
						"Route6-0600_UC South Loop Plat2",//
								"Route6-0606_Edinburgh Rd. at Laurelwood Crt.", //
								"Route6-0613_Ironwood Rd. at Hilldale Cres." })) //
				.compileBothTripSort());
		map2.put(7l, new RouteTripSpec(7l, //
				MDirectionType.EAST.intValue(), MTrip.HEADSIGN_TYPE_DIRECTION, MDirectionType.EAST.getId(), //
				MDirectionType.WEST.intValue(), MTrip.HEADSIGN_TYPE_DIRECTION, MDirectionType.WEST.getId()) //
				.addTripSort(MDirectionType.EAST.intValue(), //
						Arrays.asList(new String[] { //
						"Route7-0713_Ptarmigan Dr. at Downey Rd.", //
								"Route7-0719_Kortright Rd. at Edinburgh Rd. S.", //
								"Route7-0725_UC South Loop Plat5" })) //
				.addTripSort(MDirectionType.WEST.intValue(), //
						Arrays.asList(new String[] { //
						"Route7-0700_UC South Loop Plat1", //
								"Route7-0709_Downey Rd. at Woodland Glen Dr.", //
								"Route7-0713_Ptarmigan Dr. at Downey Rd." })) //
				.compileBothTripSort());
		map2.put(8l, new RouteTripSpec(8l, //
				MDirectionType.NORTH.intValue(), MTrip.HEADSIGN_TYPE_DIRECTION, MDirectionType.NORTH.getId(), //
				MDirectionType.SOUTH.intValue(), MTrip.HEADSIGN_TYPE_DIRECTION, MDirectionType.SOUTH.getId()) //
				.addTripSort(MDirectionType.NORTH.intValue(), //
						Arrays.asList(new String[] { //
						"Route8-0812_Janefield Ave. at Scottsdale Dr.", //
								"Route8-0820_Edinburgh Rd. S. at Municipal St.", //
								"Route8-0827_Guelph Central Station" })) //
				.addTripSort(MDirectionType.SOUTH.intValue(), //
						Arrays.asList(new String[] { //
						"Route8-0800_Guelph Central Station", //
								"Route8-0806_Maple St. at Forest St.", //
								"Route8-0812_Janefield Ave. at Scottsdale Dr." })) //
				.compileBothTripSort());
		map2.put(9l, new RouteTripSpec(9l, //
				MDirectionType.EAST.intValue(), MTrip.HEADSIGN_TYPE_DIRECTION, MDirectionType.EAST.getId(), //
				MDirectionType.WEST.intValue(), MTrip.HEADSIGN_TYPE_DIRECTION, MDirectionType.WEST.getId()) //
				.addTripSort(MDirectionType.EAST.intValue(), //
						Arrays.asList(new String[] { //
						"Route9-0915_Imperial Rd. at West Acres Dr.", //
								"Route9-0920_180 Waterloo Ave.", //
								"Route9-0924_Guelph Central Station" })) //
				.addTripSort(MDirectionType.WEST.intValue(), //
						Arrays.asList(new String[] { //
						"Route9-0900_Guelph Central Station", //
								"Route9-0912_Paisley Rd. at Zehrs", //
								"Route9-0915_Imperial Rd. at West Acres Dr." })) //
				.compileBothTripSort());
		map2.put(10l, new RouteTripSpec(10l, //
				MDirectionType.EAST.intValue(), MTrip.HEADSIGN_TYPE_DIRECTION, MDirectionType.EAST.getId(), //
				MDirectionType.WEST.intValue(), MTrip.HEADSIGN_TYPE_DIRECTION, MDirectionType.WEST.getId()) //
				.addTripSort(MDirectionType.EAST.intValue(), //
						Arrays.asList(new String[] { //
						"Route10-1019_Paisley Rd. at West Hill Estates", //
								"Route10-1024_Paisley St. at Yorkshire St. N.", //
								"Route10-1028_Guelph Central Station" })) //
				.addTripSort(MDirectionType.WEST.intValue(), //
						Arrays.asList(new String[] { //
						"Route10-1000_Guelph Central Station", //
								"Route10-1009_Paisley Rd. at Heath Rd.", //
								"Route10-1019_Paisley Rd. at West Hill Estates" })) //
				.compileBothTripSort());
		map2.put(11l, new RouteTripSpec(11l, //
				MDirectionType.EAST.intValue(), MTrip.HEADSIGN_TYPE_DIRECTION, MDirectionType.EAST.getId(), //
				MDirectionType.WEST.intValue(), MTrip.HEADSIGN_TYPE_DIRECTION, MDirectionType.WEST.getId()) //
				.addTripSort(MDirectionType.EAST.intValue(), //
						Arrays.asList(new String[] { //
						"Route11-1118_158 Willow Rd.", //
								"Route11-1122_Paisley St. at Yorkshire St. N.", //
								"Route11-1125_Guelph Central Station" })) //
				.addTripSort(MDirectionType.WEST.intValue(), //
						Arrays.asList(new String[] { //
						"Route11-1100_Guelph Central Station", //
								"Route11-1109_Dawson Rd. at Shelldale Cres.", //
								"Route11-1118_158 Willow Rd." })) //
				.compileBothTripSort());
		map2.put(12l, new RouteTripSpec(12l, //
				MDirectionType.NORTH.intValue(), MTrip.HEADSIGN_TYPE_DIRECTION, MDirectionType.NORTH.getId(), //
				MDirectionType.SOUTH.intValue(), MTrip.HEADSIGN_TYPE_DIRECTION, MDirectionType.SOUTH.getId()) //
				.addTripSort(MDirectionType.NORTH.intValue(), //
						Arrays.asList(new String[] { //
						"Route12-1200_Guelph Central Station", //
								"Route12-1207_Emma St. at Metcalfe St.", //
								"Route12-1215_Windsor St. at Kingsley Crt." })) //
				.addTripSort(MDirectionType.SOUTH.intValue(), //
						Arrays.asList(new String[] { //
						"Route12-1215_Windsor St. at Kingsley Crt.", //
								"Route12-1222_Delhi St. at Emma St.", //
								"Route12-1228_Guelph Central Station" })) //
				.compileBothTripSort());
		map2.put(13l, new RouteTripSpec(13l, //
				MDirectionType.EAST.intValue(), MTrip.HEADSIGN_TYPE_DIRECTION, MDirectionType.EAST.getId(), //
				MDirectionType.WEST.intValue(), MTrip.HEADSIGN_TYPE_DIRECTION, MDirectionType.WEST.getId()) //
				.addTripSort(MDirectionType.EAST.intValue(), //
						Arrays.asList(new String[] { //
						"Route13-1300_Guelph Central Station", //
								"Route13-1311_Cassino Ave. at William St.", //
								"Route13-1323_Victoria Rd. N. at Greenview St." })) //
				.addTripSort(MDirectionType.WEST.intValue(), //
						Arrays.asList(new String[] { //
						"Route13-1323_Victoria Rd. N. at Greenview St.", //
								"Route13-1327_Eramosa Rd. at Metcalfe St.", //
								"Route13-1330_Guelph Central Station" })) //
				.compileBothTripSort());
		map2.put(14l, new RouteTripSpec(14l, //
				MDirectionType.EAST.intValue(), MTrip.HEADSIGN_TYPE_DIRECTION, MDirectionType.EAST.getId(), //
				MDirectionType.WEST.intValue(), MTrip.HEADSIGN_TYPE_DIRECTION, MDirectionType.WEST.getId()) //
				.addTripSort(MDirectionType.EAST.intValue(), //
						Arrays.asList(new String[] { //
						"Route14-1400_Guelph Central Station", //
								"Route14-1406_Grange Rd. at Victoria Rd. N.", //
								"Route14-1412_Watson Pkwy. N. at Fleming Rd." })) //
				.addTripSort(MDirectionType.WEST.intValue(), //
						Arrays.asList(new String[] { //
						"Route14-1412_Watson Pkwy. N. at Fleming Rd.", //
								"Route14-1419_Victoria Rd. N. at Grange Rd.", //
								"Route14-1425_Guelph Central Station" })) //
				.compileBothTripSort());
		map2.put(15l, new RouteTripSpec(15l, //
				MDirectionType.EAST.intValue(), MTrip.HEADSIGN_TYPE_DIRECTION, MDirectionType.EAST.getId(), //
				MDirectionType.WEST.intValue(), MTrip.HEADSIGN_TYPE_DIRECTION, MDirectionType.WEST.getId()) //
				.addTripSort(MDirectionType.EAST.intValue(), //
						Arrays.asList(new String[] { //
						"Route15-1508_College Ave. W. at Flanders Rd.", //
								"Route15-1515_Stone Road Mall", //
								"Route15-1521_University Centre" })) //
				.addTripSort(MDirectionType.WEST.intValue(), //
						Arrays.asList(new String[] { //
						"Route15-1500_University Centre", //
								"Route15-1504_Stone Rd. W. Edinburgh Rd. S.", //
								"Route15-1508_College Ave. W. at Flanders Rd." })) //
				.compileBothTripSort());
		map2.put(16l, new RouteTripSpec(16l, //
				MDirectionType.NORTH.intValue(), MTrip.HEADSIGN_TYPE_DIRECTION, MDirectionType.NORTH.getId(), //
				MDirectionType.SOUTH.intValue(), MTrip.HEADSIGN_TYPE_DIRECTION, MDirectionType.SOUTH.getId()) //
				.addTripSort(MDirectionType.NORTH.intValue(), //
						Arrays.asList(new String[] { //
						"Route16-1631_300 Clair (Water Tower)", //
								"Route16-1646_Gordon St. at Monticello Cres.", //
								"Route16-1654_Guelph Central Station" })) //
				.addTripSort(MDirectionType.SOUTH.intValue(), //
						Arrays.asList(new String[] { //
						"Route16-1600_Guelph Central Station", //
								"Route16-1605_Gordon St. at College Ave.", //
								"Route16-1611_Gordon St. at Edinburgh Rd. S.", //
								"Route16-1631_300 Clair (Water Tower)" })) //
				.compileBothTripSort());
		map2.put(20l, new RouteTripSpec(20l, //
				MDirectionType.EAST.intValue(), MTrip.HEADSIGN_TYPE_DIRECTION, MDirectionType.EAST.getId(), //
				MDirectionType.WEST.intValue(), MTrip.HEADSIGN_TYPE_DIRECTION, MDirectionType.WEST.getId()) //
				.addTripSort(MDirectionType.EAST.intValue(), //
						Arrays.asList(new String[] { //
						"Route20-2016_Across from 498 Governors Rd.", //
								"Route20-2033_Silvercreek Pkwy N. at Campbell Rd.", //
								"Route20-2040_26 Willow Rd.", //
								"Route20-2046_Guelph Central Station" })) //
				.addTripSort(MDirectionType.WEST.intValue(), //
						Arrays.asList(new String[] { //
						"Route20-2000_Guelph Central Station", //
								"Route20-2005_Willow West Mall", //
								"Route20-2010_Willow Rd. at Flaherty Dr.", //
								"Route20-2016_Across from 498 Governors Rd." })) //
				.compileBothTripSort());
		map2.put(50l, new RouteTripSpec(50l, //
				MDirectionType.EAST.intValue(), MTrip.HEADSIGN_TYPE_DIRECTION, MDirectionType.EAST.getId(), //
				MDirectionType.WEST.intValue(), MTrip.HEADSIGN_TYPE_DIRECTION, MDirectionType.WEST.getId()) //
				.addTripSort(MDirectionType.EAST.intValue(), //
						Arrays.asList(new String[] { //
						"Route50-5004_Stone Rd. W. Edinburgh Rd. S.", //
								"Route50-5008_Scottsdale Dr. at Wilsonview Ave.", //
								"Route50-5013_University Centre" })) //
				.addTripSort(MDirectionType.WEST.intValue(), //
						Arrays.asList(new String[] { //
						"Route50-5000_University Dep", //
								"Route50-5002_Stone Rd. W. at Fire Hall", //
								"Route50-5004_Stone Rd. W. Edinburgh Rd. S." })) //
				.compileBothTripSort());
		map2.put(56l, new RouteTripSpec(56l, //
				MDirectionType.NORTH.intValue(), MTrip.HEADSIGN_TYPE_DIRECTION, MDirectionType.NORTH.getId(), //
				MDirectionType.SOUTH.intValue(), MTrip.HEADSIGN_TYPE_DIRECTION, MDirectionType.SOUTH.getId()) //
				.addTripSort(MDirectionType.NORTH.intValue(), //
						Arrays.asList(new String[] { //
						"Route56-5604_Frederick Dr. at Waterford Dr.", //
								"Route56-5610_Gordon St. at Landsdown Dr.", //
								"Route56-5616_University Arr" })) //
				.addTripSort(MDirectionType.SOUTH.intValue(), //
						Arrays.asList(new String[] { //
						"Route56-5600_University Dep", //
								"Route56-5602_1035 Victoria", //
								"Route56-5604_Frederick Dr. at Waterford Dr." })) //
				.compileBothTripSort());
		map2.put(57l, new RouteTripSpec(57l, //
				MDirectionType.EAST.intValue(), MTrip.HEADSIGN_TYPE_DIRECTION, MDirectionType.EAST.getId(), //
				MDirectionType.WEST.intValue(), MTrip.HEADSIGN_TYPE_DIRECTION, MDirectionType.WEST.getId()) //
				.addTripSort(MDirectionType.EAST.intValue(), //
						Arrays.asList(new String[] { //
						"Route57-5709_Scottsdale @ Stone", //
								"Route57-5712_252 Stone Rd. W.", //
								"Route57-5714_University Arr" })) //
				.addTripSort(MDirectionType.WEST.intValue(), //
						Arrays.asList(new String[] { //
						"Route57-5700_University Dep", //
								"Route57-5705_Harvard Rd. at Youngman Dr.", //
								"Route57-5709_Scottsdale @ Stone" })) //
				.compileBothTripSort());
		map2.put(58l, new RouteTripSpec(58l, //
				MDirectionType.EAST.intValue(), MTrip.HEADSIGN_TYPE_DIRECTION, MDirectionType.EAST.getId(), //
				MDirectionType.WEST.intValue(), MTrip.HEADSIGN_TYPE_DIRECTION, MDirectionType.WEST.getId()) //
				.addTripSort(MDirectionType.EAST.intValue(), //
						Arrays.asList(new String[] { //
						"Route58-5804_583 Edinburgh Rd. S.", //
								"Route58-5808_Kortright Rd. opposite Yewholme Dr.", //
								"Route58-5812_University Centre" })) //
				.addTripSort(MDirectionType.WEST.intValue(), //
						Arrays.asList(new String[] { //
						"Route58-5800_University Dep", //
								"Route58-5802_Stone Rd. W. at Fire Hall", //
								"Route58-5804_583 Edinburgh Rd. S." })) //
				.compileBothTripSort());
		ALL_ROUTE_TRIPS2 = map2;
	}

	@Override
	public int compareEarly(long routeId, List<MTripStop> list1, List<MTripStop> list2, MTripStop ts1, MTripStop ts2, GStop ts1GStop, GStop ts2GStop) {
		if (ALL_ROUTE_TRIPS2.containsKey(routeId)) {
			return ALL_ROUTE_TRIPS2.get(routeId).compare(routeId, list1, list2, ts1, ts2, ts1GStop, ts2GStop);
		}
		return super.compareEarly(routeId, list1, list2, ts1, ts2, ts1GStop, ts2GStop);
	}

	@Override
	public ArrayList<MTrip> splitTrip(MRoute mRoute, GTrip gTrip, GSpec gtfs) {
		if (ALL_ROUTE_TRIPS2.containsKey(mRoute.getId())) {
			return ALL_ROUTE_TRIPS2.get(mRoute.getId()).getAllTrips();
		}
		System.out.printf("\fUnexpected split trip (unexpected route ID: %s) %s", mRoute.getId(), gTrip);
		System.exit(-1);
		return null;
	}

	@Override
	public Pair<Long[], Integer[]> splitTripStop(MRoute mRoute, GTrip gTrip, GTripStop gTripStop, ArrayList<MTrip> splitTrips, GSpec routeGTFS) {
		if (ALL_ROUTE_TRIPS2.containsKey(mRoute.getId())) {
			return SplitUtils.splitTripStop(mRoute, gTrip, gTripStop, routeGTFS, ALL_ROUTE_TRIPS2.get(mRoute.getId()));
		}
		return super.splitTripStop(mRoute, gTrip, gTripStop, splitTrips, routeGTFS);
	}

	private static final String DASH = "-";

	@Override
	public void setTripHeadsign(MRoute mRoute, MTrip mTrip, GTrip gTrip, GSpec gtfs) {
		if (ALL_ROUTE_TRIPS2.containsKey(mRoute.getId())) {
			return; // split
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
