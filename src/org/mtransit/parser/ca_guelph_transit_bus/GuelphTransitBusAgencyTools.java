package org.mtransit.parser.ca_guelph_transit_bus;

import java.util.HashSet;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import org.apache.commons.lang3.StringUtils;
import org.mtransit.parser.DefaultAgencyTools;
import org.mtransit.parser.Utils;
import org.mtransit.parser.gtfs.data.GCalendar;
import org.mtransit.parser.gtfs.data.GCalendarDate;
import org.mtransit.parser.gtfs.data.GRoute;
import org.mtransit.parser.gtfs.data.GSpec;
import org.mtransit.parser.gtfs.data.GStop;
import org.mtransit.parser.gtfs.data.GTrip;
import org.mtransit.parser.mt.data.MAgency;
import org.mtransit.parser.mt.data.MRoute;
import org.mtransit.parser.CleanUtils;
import org.mtransit.parser.mt.data.MTrip;

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

	private static final String A = "A";
	private static final String B = "B";

	@Override
	public long getRouteId(GRoute gRoute) {
		if (ROUTE_GORDON_CORRIDOR.equals(gRoute.getRouteId())) {
			return 9999l;
		}
		String routeShortName = gRoute.getRouteShortName();
		if (routeShortName != null && routeShortName.length() > 0 && Utils.isDigitsOnly(routeShortName)) {
			return Integer.valueOf(routeShortName); // using stop code as stop ID
		}
		Matcher matcher = DIGITS.matcher(routeShortName);
		if (!matcher.find()) {
			System.out.printf("\nCan't find route ID digits for %s (%s)!\n", routeShortName, gRoute);
			System.exit(-1);
			return -1l;
		}
		int digits = Integer.parseInt(matcher.group());
		if (routeShortName.endsWith(A)) {
			return 1000l + digits;
		} else if (routeShortName.endsWith(B)) {
			return 2000l + digits;
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

	private static final String DASH = "-";

	@Override
	public void setTripHeadsign(MRoute mRoute, MTrip mTrip, GTrip gTrip, GSpec gtfs) {
		String gTripHeadsign = gTrip.getTripHeadsign();
		int indexOfDASH = gTripHeadsign.indexOf(DASH);
		if (indexOfDASH >= 0) {
			gTripHeadsign = gTripHeadsign.substring(indexOfDASH + 1);
		}
		int directionId = gTrip.getDirectionId() == null ? 0 : gTrip.getDirectionId();
		if (mRoute.getId() == 12l) {
			mTrip.setHeadsignString(GENERAL_HOSPITAL, directionId);
			return;
		} else if (mRoute.getId() == 16l) {
			mTrip.setHeadsignString(SOUTHGATE, directionId);
			return;
		} else if (mRoute.getId() == 50l) {
			mTrip.setHeadsignString(STONE_ROAD_EXPRESS, directionId);
			return;
		} else if (mRoute.getId() == 56l) {
			mTrip.setHeadsignString(VICTORIA_EXPRESS, directionId);
			return;
		} else if (mRoute.getId() == 57l) {
			mTrip.setHeadsignString(HARVARD_EXPRESS, directionId);
			return;
		} else if (mRoute.getId() == 58l) {
			mTrip.setHeadsignString(EDINBURGH_EXPRESS, directionId);
			return;
		}
		mTrip.setHeadsignString(cleanTripHeadsign(gTripHeadsign), directionId);
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
