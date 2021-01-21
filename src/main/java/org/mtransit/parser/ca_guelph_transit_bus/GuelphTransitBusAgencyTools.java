package org.mtransit.parser.ca_guelph_transit_bus;

import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;
import org.mtransit.parser.CleanUtils;
import org.mtransit.parser.DefaultAgencyTools;
import org.mtransit.parser.MTLog;
import org.mtransit.parser.Pair;
import org.mtransit.parser.SplitUtils;
import org.mtransit.parser.SplitUtils.RouteTripSpec;
import org.mtransit.parser.StringUtils;
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

import java.util.ArrayList;
import java.util.Arrays;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Locale;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import static org.mtransit.parser.StringUtils.EMPTY;

// http://data.open.guelph.ca/
// http://data.open.guelph.ca/dataset/guelph-transit-gtfs-data
// http://data.open.guelph.ca/datafiles/guelph-transit/guelph_transit_gtfs.zip
// OTHER: http://guelph.ca/uploads/google/google_transit.zip
public class GuelphTransitBusAgencyTools extends DefaultAgencyTools {

	public static void main(@Nullable String[] args) {
		if (args == null || args.length == 0) {
			args = new String[3];
			args[0] = "input/gtfs.zip";
			args[1] = "../../mtransitapps/ca-guelph-transit-bus-android/res/raw/";
			args[2] = ""; // files-prefix
		}
		new GuelphTransitBusAgencyTools().start(args);
	}

	@Nullable
	private HashSet<Integer> serviceIdInts;

	@Override
	public void start(@NotNull String[] args) {
		MTLog.log("Generating Guelph Transit bus data...");
		long start = System.currentTimeMillis();
		this.serviceIdInts = extractUsefulServiceIdInts(args, this, true);
		super.start(args);
		MTLog.log("Generating Guelph Transit bus data... DONE in %s.", Utils.getPrettyDuration(System.currentTimeMillis() - start));
	}

	@Override
	public boolean excludingAll() {
		return this.serviceIdInts != null && this.serviceIdInts.isEmpty();
	}

	@Override
	public boolean excludeCalendar(@NotNull GCalendar gCalendar) {
		if (this.serviceIdInts != null) {
			return excludeUselessCalendarInt(gCalendar, this.serviceIdInts);
		}
		return super.excludeCalendar(gCalendar);
	}

	@Override
	public boolean excludeCalendarDate(@NotNull GCalendarDate gCalendarDates) {
		if (this.serviceIdInts != null) {
			return excludeUselessCalendarDateInt(gCalendarDates, this.serviceIdInts);
		}
		return super.excludeCalendarDate(gCalendarDates);
	}

	@Override
	public boolean excludeTrip(@NotNull GTrip gTrip) {
		if (this.serviceIdInts != null) {
			return excludeUselessTripInt(gTrip, this.serviceIdInts);
		}
		return super.excludeTrip(gTrip);
	}

	@NotNull
	@Override
	public Integer getAgencyRouteType() {
		return MAgency.ROUTE_TYPE_BUS;
	}

	private static final Pattern DIGITS = Pattern.compile("[\\d]+");

	private static final String COMMUNITY_BUS_RSN = "Com";
	private static final long COMMUNITY_BUS_RID = 9_998L;

	private static final String U = "U";

	private static final long RID_ENDS_WITH_U = 21_000L;

	@Override
	public long getRouteId(@NotNull GRoute gRoute) {
		if (COMMUNITY_BUS_RSN.equals(gRoute.getRouteShortName())) {
			return COMMUNITY_BUS_RID;
		}
		String routeShortName = gRoute.getRouteShortName();
		if (routeShortName.length() > 0 && Utils.isDigitsOnly(routeShortName)) {
			return Long.parseLong(routeShortName); // using route short name as route ID
		}
		Matcher matcher = DIGITS.matcher(routeShortName);
		if (matcher.find()) {
			int digits = Integer.parseInt(matcher.group());
			if (routeShortName.endsWith(U)) {
				return RID_ENDS_WITH_U + digits;
			}
		}
		throw new MTLog.Fatal("Can't find route ID for '%s' (%s)!", routeShortName, gRoute);
	}

	private static final Pattern ALL_WHITESPACES = Pattern.compile("\\s+", Pattern.CASE_INSENSITIVE);

	@Nullable
	@Override
	public String getRouteShortName(@NotNull GRoute gRoute) {
		String routeShortName = gRoute.getRouteShortName();
		routeShortName = ALL_WHITESPACES.matcher(routeShortName).replaceAll(EMPTY);
		return routeShortName;
	}

	private static final Pattern STARTS_WITH_ROUTE_RSN = Pattern.compile("(route[\\d]*[A-Z]*[\\-]?[\\s]*)", Pattern.CASE_INSENSITIVE);

	@NotNull
	@Override
	public String getRouteLongName(@NotNull GRoute gRoute) {
		String routeLongName = gRoute.getRouteLongName();
		routeLongName = STARTS_WITH_ROUTE_RSN.matcher(routeLongName).replaceAll(EMPTY);
		if (StringUtils.isEmpty(routeLongName)) {
			throw new MTLog.Fatal("getRouteLongName() > Unexpected route short name '%s' (%s)!", gRoute.getRouteShortName(), gRoute);
		}
		return CleanUtils.cleanLabel(routeLongName);
	}

	private static final String AGENCY_COLOR = "00A6E5"; // BLUE

	@NotNull
	@Override
	public String getAgencyColor() {
		return AGENCY_COLOR;
	}

	@SuppressWarnings("DuplicateBranchesInSwitch")
	@Nullable
	@Override
	public String getRouteColor(@NotNull GRoute gRoute) {
		if (StringUtils.isEmpty(gRoute.getRouteShortName())) {
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
			throw new MTLog.Fatal("getRouteColor() > Unexpected route color for '%s'!", gRoute);
		}
		return super.getRouteColor(gRoute);
	}

	private static final String UNIVERSITY_CENTER = "University Ctr";
	private static final String INDUSTRIAL_SHORT = "Ind";
	private static final String GUELPH_CENTRAL_STATION = "Guelph Central Sta";
	private static final String STONE_ROAD_MALL = "Stone Road Mall";

	private static final HashMap<Long, RouteTripSpec> ALL_ROUTE_TRIPS2;

	static {
		HashMap<Long, RouteTripSpec> map2 = new HashMap<>();
		//noinspection deprecation
		map2.put(1L, new RouteTripSpec(1L, //
				MDirectionType.EAST.intValue(), MTrip.HEADSIGN_TYPE_STRING, UNIVERSITY_CENTER, //
				MDirectionType.WEST.intValue(), MTrip.HEADSIGN_TYPE_STRING, "Edinburgh @ Laurelwood") //
				.addTripSort(MDirectionType.EAST.intValue(), //
						Arrays.asList( //
								"112", // Edinburgh at Laurelwood northbound
								"356", // ==
								"5844", // University Centre South Loop Platform 2
								"5845" // University Centre South Loop Platform 4
						)) //
				.addTripSort(MDirectionType.WEST.intValue(), //
						Arrays.asList( //
								"5845", // University Centre South Loop Platform 4
								"106", // ++
								"112" // Edinburgh at Laurelwood northbound
						)) //
				.compileBothTripSort());
		//noinspection deprecation
		map2.put(2L, new RouteTripSpec(2L, //
				MDirectionType.EAST.intValue(), MTrip.HEADSIGN_TYPE_STRING, UNIVERSITY_CENTER, //
				MDirectionType.WEST.intValue(), MTrip.HEADSIGN_TYPE_STRING, "Edinburgh @ Ironwood") //
				.addTripSort(MDirectionType.EAST.intValue(), //
						Arrays.asList( //
								"162", // Edinburgh at Ironwood southbound
								"168", // ++
								"5834" // University Centre North Loop Platform 10
						)) //
				.addTripSort(MDirectionType.WEST.intValue(), //
						Arrays.asList( //
								"5847", // University Centre North Loop Platform 11
								"157", // ++
								"162" // Edinburgh at Ironwood southbound
						)) //
				.compileBothTripSort());
		//noinspection deprecation
		map2.put(3L, new RouteTripSpec(3L, //
				MDirectionType.NORTH.intValue(), MTrip.HEADSIGN_TYPE_STRING, "Woodlawn @ Edinburgh", //
				MDirectionType.SOUTH.intValue(), MTrip.HEADSIGN_TYPE_STRING, GUELPH_CENTRAL_STATION) //
				.addTripSort(MDirectionType.NORTH.intValue(), //
						Arrays.asList( //
								"5841", // Guelph Central Station Platform 21
								"305", // ++
								"231" // Woodlawn at Edinburgh eastbound
						)) //
				.addTripSort(MDirectionType.SOUTH.intValue(), //
						Arrays.asList( //
								"231", // Woodlawn at Edinburgh eastbound
								"389", // Westmount at Kimberley southbound
								"392", // ++
								"1130", // ==
								"5837", "5841" // Guelph Central Station
						)) //
				.compileBothTripSort());
		//noinspection deprecation
		map2.put(4L, new RouteTripSpec(4L, //
				MDirectionType.EAST.intValue(), MTrip.HEADSIGN_TYPE_STRING, "Watson @ Guelph Transit", //
				MDirectionType.WEST.intValue(), MTrip.HEADSIGN_TYPE_STRING, GUELPH_CENTRAL_STATION) //
				.addTripSort(MDirectionType.EAST.intValue(), //
						Arrays.asList( //
								"5849", // Guelph Central Station Platform 2
								"360", //
								"416" // Watson at Guelph Transit southbound
						)) //
				.addTripSort(MDirectionType.WEST.intValue(), //
						Arrays.asList( //
								"416", // Watson at Guelph Transit southbound
								"422", // ++
								"5850" // Guelph Central Station Platform 2
						)) //
				.compileBothTripSort());
		//noinspection deprecation
		map2.put(5L, new RouteTripSpec(5L, //
				MDirectionType.NORTH.intValue(), MTrip.HEADSIGN_TYPE_STRING, UNIVERSITY_CENTER, //
				MDirectionType.SOUTH.intValue(), MTrip.HEADSIGN_TYPE_STRING, "Frederick @ Waterford") //
				.addTripSort(MDirectionType.NORTH.intValue(), //
						Arrays.asList( //
								"520", // Frederick at Waterford westbound
								"528", // Gordon at Lowes northbound
								"168", // ==
								"169", // !=
								"5844", // != University Centre South Loop Platform 2
								"5845" // != University Centre South Loop Platform 4
						)) //
				.addTripSort(MDirectionType.SOUTH.intValue(), //
						Arrays.asList( //
								"5844", // University Centre South Loop Platform 2
								"5915", // ++
								"520" // Frederick at Waterford westbound
						)) //
				.compileBothTripSort());
		//noinspection deprecation
		map2.put(6L, new RouteTripSpec(6L, //
				MDirectionType.EAST.intValue(), MTrip.HEADSIGN_TYPE_STRING, UNIVERSITY_CENTER, //
				MDirectionType.WEST.intValue(), MTrip.HEADSIGN_TYPE_STRING, "Ironwood @ Kortright") //
				.addTripSort(MDirectionType.EAST.intValue(), //
						Arrays.asList( //
								"615", // Ironwood at Kortright northbound
								"621", // ++
								"5843" // University Centre South Loop
						)) //
				.addTripSort(MDirectionType.WEST.intValue(), //
						Arrays.asList( //
								"5831", // University Centre South Loop
								"608", // ++
								"612", // Ironwood at Woodborough southbound
								"615" // Ironwood at Kortright northbound
						)) //
				.compileBothTripSort());
		//noinspection deprecation
		map2.put(7L, new RouteTripSpec(7L, //
				MDirectionType.EAST.intValue(), MTrip.HEADSIGN_TYPE_STRING, "Ptarmigan @ Downey", //
				MDirectionType.WEST.intValue(), MTrip.HEADSIGN_TYPE_STRING, UNIVERSITY_CENTER) //
				.addTripSort(MDirectionType.EAST.intValue(), //
						Arrays.asList( //
								"5843", // University Centre South Loop
								"706", // ++
								"709", // Downey at Woodland Glen westbound
								"713" // Ptarmigan at Downey eastbound
						)) //
				.addTripSort(MDirectionType.WEST.intValue(), //
						Arrays.asList( //
								"713", // Ptarmigan at Downey eastbound
								"717", // Kortright at Ironwood eastbound
								"5831" // University Centre South Loop
						)) //
				.compileBothTripSort());
		//noinspection deprecation
		map2.put(8L, new RouteTripSpec(8L, //
				MDirectionType.NORTH.intValue(), MTrip.HEADSIGN_TYPE_STRING, GUELPH_CENTRAL_STATION, //
				MDirectionType.SOUTH.intValue(), MTrip.HEADSIGN_TYPE_STRING, STONE_ROAD_MALL) //
				.addTripSort(MDirectionType.NORTH.intValue(), //
						Arrays.asList( //
								"813", // Stone Road Mall
								"819", // ++
								"5849" // Guelph Central Station
						)) //
				.addTripSort(MDirectionType.SOUTH.intValue(), //
						Arrays.asList( //
								"5850", // Guelph Central Station
								"808", // ++
								"813" // Stone Road Mall
						)) //
				.compileBothTripSort());
		//noinspection deprecation
		map2.put(9L, new RouteTripSpec(9L, //
				MDirectionType.EAST.intValue(), MTrip.HEADSIGN_TYPE_STRING, GUELPH_CENTRAL_STATION, //
				MDirectionType.WEST.intValue(), MTrip.HEADSIGN_TYPE_STRING, "Elmira @ West Acres") //
				.addTripSort(MDirectionType.EAST.intValue(), //
						Arrays.asList( //
								"213", // Elmira at West Acres northbound
								"919", // ++
								"6067", // ==
								"5833", // Guelph Central Station Platform 1
								"5852" // Guelph Central Station Platform 7
						)) //
				.addTripSort(MDirectionType.WEST.intValue(), //
						Arrays.asList( //
								"5839", // Guelph Central Station Platform 8
								"904", // ++
								"213" // Elmira at West Acres northbound
						)) //
				.compileBothTripSort());
		//noinspection deprecation
		map2.put(10L, new RouteTripSpec(10L, //
				MDirectionType.EAST.intValue(), MTrip.HEADSIGN_TYPE_STRING, GUELPH_CENTRAL_STATION, //
				MDirectionType.WEST.intValue(), MTrip.HEADSIGN_TYPE_STRING, "Imperial @ Ferman") //
				.addTripSort(MDirectionType.EAST.intValue(), //
						Arrays.asList( //
								"1015", // Imperial at Ferman southbound
								"1022", // ++
								"5860" // Guelph Central Station Platform 22
						)) //
				.addTripSort(MDirectionType.WEST.intValue(), //
						Arrays.asList( //
								"5858", // Guelph Central Station Platform 19
								"1008", // ++
								"1015" // Imperial at Ferman southbound
						)) //
				.compileBothTripSort());
		//noinspection deprecation
		map2.put(11L, new RouteTripSpec(11L, //
				MDirectionType.EAST.intValue(), MTrip.HEADSIGN_TYPE_STRING, GUELPH_CENTRAL_STATION, //
				MDirectionType.WEST.intValue(), MTrip.HEADSIGN_TYPE_STRING, "Silvercreek @ Greengate") //
				.addTripSort(MDirectionType.EAST.intValue(), //
						Arrays.asList( //
								"2035", // Silvercreek at Greengate southbound
								"1122", // Paisley at Yorkshire eastbound
								"5859" // Guelph Central Station Platform 20
						)) //
				.addTripSort(MDirectionType.WEST.intValue(), //
						Arrays.asList( //
								"5851", // Guelph Central Station Platform 4
								"1105", // ++
								"2035" // Silvercreek at Greengate southbound
						)) //
				.compileBothTripSort());
		//noinspection deprecation
		map2.put(12L, new RouteTripSpec(12L, //
				MDirectionType.NORTH.intValue(), MTrip.HEADSIGN_TYPE_STRING, "Woodlawn @ Victoria", //
				MDirectionType.SOUTH.intValue(), MTrip.HEADSIGN_TYPE_STRING, GUELPH_CENTRAL_STATION) //
				.addTripSort(MDirectionType.NORTH.intValue(), //
						Arrays.asList( //
								"5860", // Guelph Central Station Platform 22
								"1207", // ++
								"1214" // Woodlawn at Victoria westbound
						)) //
				.addTripSort(MDirectionType.SOUTH.intValue(), //
						Arrays.asList( //
								"1214", // Woodlawn at Victoria westbound
								"1221", // ++
								"5858" // Guelph Central Station Platform 19
						)) //
				.compileBothTripSort());
		//noinspection deprecation
		map2.put(13L, new RouteTripSpec(13L, //
				MDirectionType.EAST.intValue(), MTrip.HEADSIGN_TYPE_STRING, "Eastview @ Starwood", //
				MDirectionType.WEST.intValue(), MTrip.HEADSIGN_TYPE_STRING, GUELPH_CENTRAL_STATION) //
				.addTripSort(MDirectionType.EAST.intValue(), //
						Arrays.asList( //
								"5837", // Guelph Central Station Platform 5
								"1313", // ++
								"370" // Eastview at Starwood westbound
						)) //
				.addTripSort(MDirectionType.WEST.intValue(), //
						Arrays.asList( //
								"370", // Eastview at Starwood westbound
								"1324", // Eramosa at Orchard westbound
								"1130", // ==
								"5837", // Guelph Central Station Platform 5
								"5841" // Guelph Central Station Platform 21
						)) //
				.compileBothTripSort());
		//noinspection deprecation
		map2.put(14L, new RouteTripSpec(14L, //
				MDirectionType.EAST.intValue(), MTrip.HEADSIGN_TYPE_STRING, "Watson @ Fleming", //
				MDirectionType.WEST.intValue(), MTrip.HEADSIGN_TYPE_STRING, GUELPH_CENTRAL_STATION) //
				.addTripSort(MDirectionType.EAST.intValue(), //
						Arrays.asList( //
								"5859", // Guelph Central Station Platform 20
								"1406", // ++
								"332" // Watson at Fleming southbound
						)) //
				.addTripSort(MDirectionType.WEST.intValue(), //
						Arrays.asList( //
								"332", // Watson at Fleming southbound
								"1418", // ++
								"5851" // Guelph Central Station Platform 4
						)) //
				.compileBothTripSort());
		//noinspection deprecation
		map2.put(15L, new RouteTripSpec(15L, //
				MDirectionType.EAST.intValue(), MTrip.HEADSIGN_TYPE_STRING, UNIVERSITY_CENTER, //
				MDirectionType.WEST.intValue(), MTrip.HEADSIGN_TYPE_STRING, "College @ Flanders") //
				.addTripSort(MDirectionType.EAST.intValue(), //
						Arrays.asList( //
								"1508", // College at Flanders northbound
								"118", // College at Lynnwood eastbound
								"5847" // University Centre North Loop Platform 11
						)) //
				.addTripSort(MDirectionType.WEST.intValue(), //
						Arrays.asList( //
								"5834", // University Centre North Loop Platform 10
								"207", // ++
								"1508" // College at Flanders northbound
						)) //
				.compileBothTripSort());
		//noinspection deprecation
		map2.put(16L, new RouteTripSpec(16L, //
				MDirectionType.EAST.intValue(), MTrip.HEADSIGN_TYPE_STRING, "Southgate", //
				MDirectionType.WEST.intValue(), MTrip.HEADSIGN_TYPE_STRING, "Clair @ Gordon") //
				.addTripSort(MDirectionType.EAST.intValue(), //
						Arrays.asList( //
								"6058", // Clair at Gordon westbound
								"1619", // Laird at Clair westbound
								"1621", // ++
								"1624" // 485 Southgate southbound
						)) //
				.addTripSort(MDirectionType.WEST.intValue(), //
						Arrays.asList( //
								"1624", // 485 Southgate southbound
								"1627", // ++
								"1631", // == Clair at Laird eastbound
								"6058", // != Clair at Gordon westbound =>
								"6006", // !=
								"6101" // Poppy at Gordon westbound =>
						)) //
				.compileBothTripSort());
		//noinspection deprecation
		map2.put(17L, new RouteTripSpec(17L, // splitted
				MDirectionType.NORTH.intValue(), MTrip.HEADSIGN_TYPE_STRING, "Woodlawn Smart Ctrs", //
				MDirectionType.SOUTH.intValue(), MTrip.HEADSIGN_TYPE_STRING, UNIVERSITY_CENTER) //
				.addTripSort(MDirectionType.NORTH.intValue(), //
						Arrays.asList( //
								"5836", // University Centre North Loop Platform 7
								"1501", // ++
								"5942", // Speedvale at Conestoga College eastbound (Imperial @ Willow)
								"5917" // Woodlawn Smart Centres southbound =>
						)) //
				.addTripSort(MDirectionType.SOUTH.intValue(), //
						Arrays.asList( //
								"5917", // Woodlawn Smart Centres southbound <=
								"223", // Royal at ARC Industries northbound
								"320", // Inverness at Wilton northbound
								"321", // Victoria at Norma southbound
								"333", // Watson at Starwood southbound
								"339", // ++
								"5836" // University Centre North Loop Platform 7
						)) //
				.compileBothTripSort());
		//noinspection deprecation
		map2.put(18L, new RouteTripSpec(18L, //
				MDirectionType.NORTH.intValue(), MTrip.HEADSIGN_TYPE_STRING, "Eastview @ Victoria", //
				MDirectionType.SOUTH.intValue(), MTrip.HEADSIGN_TYPE_STRING, UNIVERSITY_CENTER) //
				.addTripSort(MDirectionType.NORTH.intValue(), //
						Arrays.asList( //
								"5840", // University Centre South Loop Platform 6
								"366", // Watson at Fleming northbound
								"372" // Eastview at Victoria westbound
						)) //
				.addTripSort(MDirectionType.SOUTH.intValue(), //
						Arrays.asList( //
								"372", // Eastview at Victoria westbound
								"378", // ++ Victoria at Norma northbound
								"379", // Inverness at Wilton southbound
								"278", // Elmira at West End Community Centre southbound
								"6047", // Stone Road Mall Platform 2
								"1520", // ==
								"5840" // != University Centre South Loop Platform 6
						)) //
				.compileBothTripSort());
		//noinspection deprecation
		map2.put(20L, new RouteTripSpec(20L, //
				MDirectionType.EAST.intValue(), MTrip.HEADSIGN_TYPE_STRING, GUELPH_CENTRAL_STATION, //
				MDirectionType.WEST.intValue(), MTrip.HEADSIGN_TYPE_STRING, "Imperial @ Galaxy Cinema") //
				.addTripSort(MDirectionType.EAST.intValue(), //
						Arrays.asList( //
								"2028", // Imperial at Galaxy Cinema northbound
								"1130", // ==
								"5833", "5839" // Guelph Central Station
						)) //
				.addTripSort(MDirectionType.WEST.intValue(), //
						Arrays.asList( //
								"5833", // Guelph Central Station Platform 1
								"5863", // ++
								"2028" // Imperial at Galaxy Cinema northbound
						)) //
				.compileBothTripSort());
		//noinspection deprecation
		map2.put(40L, new RouteTripSpec(40L, //
				MDirectionType.NORTH.intValue(), MTrip.HEADSIGN_TYPE_STRING, GUELPH_CENTRAL_STATION, //
				MDirectionType.SOUTH.intValue(), MTrip.HEADSIGN_TYPE_STRING, STONE_ROAD_MALL) //
				.addTripSort(MDirectionType.NORTH.intValue(), //
						Arrays.asList( //
								"6047", "6047", // Stone Road Mall
								"5850", "5850" // Guelph Central Station
						)) //
				.addTripSort(MDirectionType.SOUTH.intValue(), //
						Arrays.asList( //
								"5850", // Guelph Central Station
								"156", // College at Centennial westbound
								"6047" // Stone Road Mall
						)) //
				.compileBothTripSort());
		//noinspection deprecation
		map2.put(50L + RID_ENDS_WITH_U, new RouteTripSpec(50L + RID_ENDS_WITH_U, // 50U
				MDirectionType.EAST.intValue(), MTrip.HEADSIGN_TYPE_STRING, UNIVERSITY_CENTER, //
				MDirectionType.WEST.intValue(), MTrip.HEADSIGN_TYPE_STRING, "Stone @ Edinburgh") //
				.addTripSort(MDirectionType.EAST.intValue(), //
						Arrays.asList( //
								"207", // Stone at Edinburgh westbound
								"114", // Stone at Scottsdale westbound
								"1516", // ++
								"5846" // University Centre North Loop Platform 8
						)) //
				.addTripSort(MDirectionType.WEST.intValue(), //
						Arrays.asList( //
								"5846", // University Centre North Loop Platform 8
								"1501", // ++
								"207" // Stone at Edinburgh westbound
						)) //
				.compileBothTripSort());
		//noinspection deprecation
		map2.put(51L + RID_ENDS_WITH_U, new RouteTripSpec(51L + RID_ENDS_WITH_U, // 51U
				MDirectionType.EAST.intValue(), MTrip.HEADSIGN_TYPE_STRING, UNIVERSITY_CENTER, //
				MDirectionType.WEST.intValue(), MTrip.HEADSIGN_TYPE_STRING, "Janefield @ Mason") //
				.addTripSort(MDirectionType.EAST.intValue(), //
						Arrays.asList( //
								"116", // Janefield at Mason northbound
								"1520", // ==
								"5845", "5847" // University Centre South Loop
						)) //
				.addTripSort(MDirectionType.WEST.intValue(), //
						Arrays.asList( //
								"5845", // University Centre South Loop
								"115", // Janefield at Poppy northbound
								"116" // Janefield at Mason northbound
						)) //
				.compileBothTripSort());
		//noinspection deprecation
		map2.put(52L + RID_ENDS_WITH_U, new RouteTripSpec(52L + RID_ENDS_WITH_U, // 52U
				MDirectionType.NORTH.intValue(), MTrip.HEADSIGN_TYPE_STRING, UNIVERSITY_CENTER, //
				MDirectionType.SOUTH.intValue(), MTrip.HEADSIGN_TYPE_STRING, "Edinburgh @ Rickson") //
				.addTripSort(MDirectionType.NORTH.intValue(), //
						Arrays.asList( //
								"166", // Edinburgh at Rickson eastbound
								"167", // Edinburgh at Carrington eastbound
								"171", // ++
								"5845" // University Centre South Loop Platform 4
						)) //
				.addTripSort(MDirectionType.SOUTH.intValue(), //
						Arrays.asList( //
								"5847", // University Centre North Loop Platform 11
								"703,", // ++
								"166" // Edinburgh at Rickson eastbound
						)) //
				.compileBothTripSort());
		//noinspection deprecation
		map2.put(56L + RID_ENDS_WITH_U, new RouteTripSpec(56L + RID_ENDS_WITH_U, // 56U
				MDirectionType.NORTH.intValue(), MTrip.HEADSIGN_TYPE_STRING, UNIVERSITY_CENTER, //
				MDirectionType.SOUTH.intValue(), MTrip.HEADSIGN_TYPE_STRING, "Goodwin @ Samuel") //
				.addTripSort(MDirectionType.NORTH.intValue(), //
						Arrays.asList( //
								"6014", // Goodwin at Samuel eastbound
								"5605", // Colonial at Lambeth Way northbound
								"169", // ++
								"5842" // University Centre South Loop Platform 0
						)) //
				.addTripSort(MDirectionType.SOUTH.intValue(), //
						Arrays.asList( //
								"5842", // University Centre South Loop Platform 0
								"104", // ++
								"6014" // Goodwin at Samuel eastbound
						)) //
				.compileBothTripSort());
		//noinspection deprecation
		map2.put(57L + RID_ENDS_WITH_U, new RouteTripSpec(57L + RID_ENDS_WITH_U, // 57U
				MDirectionType.EAST.intValue(), MTrip.HEADSIGN_TYPE_STRING, UNIVERSITY_CENTER, //
				MDirectionType.WEST.intValue(), MTrip.HEADSIGN_TYPE_STRING, "Ironwood @ Reid") //
				.addTripSort(MDirectionType.EAST.intValue(), //
						Arrays.asList( //
								"5706", // Ironwood at Reid westbound
								"5709", // ++
								"5846" // University Centre North Loop Platform 8
						)) //
				.addTripSort(MDirectionType.WEST.intValue(), //
						Arrays.asList( //
								"5838", // University Centre North Loop Platform 9
								"1503", // ++
								"5706" // Ironwood at Reid westbound
						)) //
				.compileBothTripSort());
		//noinspection deprecation
		map2.put(58L + RID_ENDS_WITH_U, new RouteTripSpec(58L + RID_ENDS_WITH_U, // 58U
				MDirectionType.EAST.intValue(), MTrip.HEADSIGN_TYPE_STRING, UNIVERSITY_CENTER, //
				MDirectionType.WEST.intValue(), MTrip.HEADSIGN_TYPE_STRING, "Edinburgh @ Ironwood") //
				.addTripSort(MDirectionType.EAST.intValue(), //
						Arrays.asList( //
								"162", // Edinburgh at Ironwood southbound
								"721", // ++
								"5838" // University Centre North Loop Platform 9
						)) //
				.addTripSort(MDirectionType.WEST.intValue(), //
						Arrays.asList( //
								"5846", // University Centre North Loop Platform 8
								"1501", // ++
								"162" // Edinburgh at Ironwood southbound
						)) //
				.compileBothTripSort());
		ALL_ROUTE_TRIPS2 = map2;
	}

	@Override
	public int compareEarly(long routeId, @NotNull List<MTripStop> list1, @NotNull List<MTripStop> list2, @NotNull MTripStop ts1, @NotNull MTripStop ts2, @NotNull GStop ts1GStop, @NotNull GStop ts2GStop) {
		if (ALL_ROUTE_TRIPS2.containsKey(routeId)) {
			return ALL_ROUTE_TRIPS2.get(routeId).compare(routeId, list1, list2, ts1, ts2, ts1GStop, ts2GStop, this);
		}
		return super.compareEarly(routeId, list1, list2, ts1, ts2, ts1GStop, ts2GStop);
	}

	@NotNull
	@Override
	public ArrayList<MTrip> splitTrip(@NotNull MRoute mRoute, @Nullable GTrip gTrip, @NotNull GSpec gtfs) {
		if (ALL_ROUTE_TRIPS2.containsKey(mRoute.getId())) {
			return ALL_ROUTE_TRIPS2.get(mRoute.getId()).getAllTrips();
		}
		return super.splitTrip(mRoute, gTrip, gtfs);
	}

	@NotNull
	@Override
	public Pair<Long[], Integer[]> splitTripStop(@NotNull MRoute mRoute, @NotNull GTrip gTrip, @NotNull GTripStop gTripStop, @NotNull ArrayList<MTrip> splitTrips, @NotNull GSpec routeGTFS) {
		if (ALL_ROUTE_TRIPS2.containsKey(mRoute.getId())) {
			return SplitUtils.splitTripStop(mRoute, gTrip, gTripStop, routeGTFS, ALL_ROUTE_TRIPS2.get(mRoute.getId()), this);
		}
		return super.splitTripStop(mRoute, gTrip, gTripStop, splitTrips, routeGTFS);
	}

	private static final String DASH = "-";

	@Override
	public void setTripHeadsign(@NotNull MRoute mRoute, @NotNull MTrip mTrip, @NotNull GTrip gTrip, @NotNull GSpec gtfs) {
		if (ALL_ROUTE_TRIPS2.containsKey(mRoute.getId())) {
			return; // split
		}
		final String tripHeadsignLC = gTrip.getTripHeadsign().toLowerCase(Locale.ENGLISH);
		if (tripHeadsignLC.contains("northbound") //
				|| tripHeadsignLC.contains("north loop")) {
			mTrip.setHeadsignString("North", gTrip.getDirectionId());
			return;
		} else if (tripHeadsignLC.contains("southbound") //
				|| tripHeadsignLC.contains("south loop")) {
			mTrip.setHeadsignString("South", gTrip.getDirectionId());
			return;
		}
		mTrip.setHeadsignString(
				cleanTripHeadsign(gTrip.getTripHeadsign()),
				gTrip.getDirectionIdOrDefault()
		);
	}

	@Override
	public boolean directionFinderEnabled() {
		return false; // DISABLED because provided direction_id USELESS
	}

	private static final Pattern STARTS_WITH_RSN = Pattern.compile("(^[\\d]+ )", Pattern.CASE_INSENSITIVE);

	private static final Pattern INDUSTRIAL_ = Pattern.compile("((^|\\W)(industrial)(\\W|$))", Pattern.CASE_INSENSITIVE);
	private static final String INDUSTRIAL_REPLACEMENT = "$2" + INDUSTRIAL_SHORT + "$4";

	@NotNull
	@Override
	public String cleanTripHeadsign(@NotNull String tripHeadsign) {
		tripHeadsign = CleanUtils.keepToAndRemoveVia(tripHeadsign);
		tripHeadsign = STARTS_WITH_RSN.matcher(tripHeadsign).replaceAll(EMPTY);
		tripHeadsign = CleanUtils.CLEAN_AT.matcher(tripHeadsign).replaceAll(CleanUtils.CLEAN_AT_REPLACEMENT);
		tripHeadsign = INDUSTRIAL_.matcher(tripHeadsign).replaceAll(INDUSTRIAL_REPLACEMENT);
		tripHeadsign = CleanUtils.cleanNumbers(tripHeadsign);
		tripHeadsign = CleanUtils.cleanStreetTypes(tripHeadsign);
		return CleanUtils.cleanLabel(tripHeadsign);
	}

	@Override
	public boolean mergeHeadsign(@NotNull MTrip mTrip, @NotNull MTrip mTripToMerge) {
		List<String> headsignsValues = Arrays.asList(mTrip.getHeadsignValue(), mTripToMerge.getHeadsignValue());
		if (mTrip.getRouteId() == 1L) {
			if (Arrays.asList( //
					"Edinburgh @ Laurelwood", //
					"Edinburgh College" //
			).containsAll(headsignsValues)) {
				mTrip.setHeadsignString("Edinburgh College", mTrip.getHeadsignId());
				return true;
			}
		} else if (mTrip.getRouteId() == 2L) {
			if (Arrays.asList( //
					"Edinburgh @ Ironwood", //
					"College Edinburgh" //
			).containsAll(headsignsValues)) {
				mTrip.setHeadsignString("College Edinburgh", mTrip.getHeadsignId());
				return true;
			}
		} else if (mTrip.getRouteId() == 5L) {
			if (Arrays.asList( //
					"Frederick @ Waterford", //
					UNIVERSITY_CENTER, //
					"Goodwin" //
			).containsAll(headsignsValues)) {
				mTrip.setHeadsignString("Goodwin", mTrip.getHeadsignId());
				return true;
			}
		} else if (mTrip.getRouteId() == 6L) {
			if (Arrays.asList( //
					"Ironwood @ Kortright", //
					UNIVERSITY_CENTER, //
					"Harvard Ironwood" //
			).containsAll(headsignsValues)) {
				mTrip.setHeadsignString("Harvard Ironwood", mTrip.getHeadsignId());
				return true;
			}
		} else if (mTrip.getRouteId() == 7L) {
			if (Arrays.asList( //
					"Ptarmigan @ Downey", //
					"Kortright Downey" //
			).containsAll(headsignsValues)) {
				mTrip.setHeadsignString("Kortright Downey", mTrip.getHeadsignId());
				return true;
			}
		} else if (mTrip.getRouteId() == 15L) {
			if (Arrays.asList( //
					"College @ Flanders", //
					UNIVERSITY_CENTER, //
					"University College" //
			).containsAll(headsignsValues)) {
				mTrip.setHeadsignString("University College", mTrip.getHeadsignId());
				return true;
			}
		} else if (mTrip.getRouteId() == 16L) {
			if (Arrays.asList( //
					"Clair @ Gordon", //
					"Southgate" //
			).containsAll(headsignsValues)) {
				mTrip.setHeadsignString("Southgate", mTrip.getHeadsignId());
				return true;
			}
		} else if (mTrip.getRouteId() == 17L) {
			if (Arrays.asList( //
					"Imperial @ Willow", //
					"Woodlawn Watson" //
			).containsAll(headsignsValues)) {
				mTrip.setHeadsignString("Woodlawn Watson", mTrip.getHeadsignId());
				return true;
			}
		} else if (mTrip.getRouteId() == 18L) {
			if (Arrays.asList( //
					"Eastview @ Victoria", //
					"Watson Woodlawn" //
			).containsAll(headsignsValues)) {
				mTrip.setHeadsignString("Watson Woodlawn", mTrip.getHeadsignId());
				return true;
			}
		} else if (mTrip.getRouteId() == 20L) {
			if (Arrays.asList( //
					"Imperial @ Galaxy Cinema", //
					GUELPH_CENTRAL_STATION, //
					"Northwest " + INDUSTRIAL_SHORT //
			).containsAll(headsignsValues)) {
				mTrip.setHeadsignString("Northwest " + INDUSTRIAL_SHORT, mTrip.getHeadsignId());
				return true;
			}
		} else if (mTrip.getRouteId() == 59L + RID_ENDS_WITH_U) { // 59U
			if (Arrays.asList( //
					"Gordon @ Vaughan", //
					"Clairfields" //
			).containsAll(headsignsValues)) {
				mTrip.setHeadsignString("Clairfields", mTrip.getHeadsignId());
				return true;
			}
		} else if (mTrip.getRouteId() == 99L) {
			if (Arrays.asList( //
					EMPTY, //
					"North" //
			).containsAll(headsignsValues)) {
				mTrip.setHeadsignString("North", mTrip.getHeadsignId());
				return true;
			}
		}
		throw new MTLog.Fatal("Unexpected trips to merge %s and %s.", mTrip, mTripToMerge);
	}

	private static final Pattern CLEAN_DEPART_ARRIVE = Pattern.compile("( (arrival|depart)$)", Pattern.CASE_INSENSITIVE);

	private static final Pattern PLATFORM_ = CleanUtils.cleanWords("platform");
	private static final String PLATFORM_REPLACEMENT_ = CleanUtils.cleanWordsReplacement("P");

	@NotNull
	@Override
	public String cleanStopName(@NotNull String gStopName) {
		gStopName = CleanUtils.CLEAN_AT.matcher(gStopName).replaceAll(CleanUtils.CLEAN_AT_REPLACEMENT);
		gStopName = CLEAN_DEPART_ARRIVE.matcher(gStopName).replaceAll(EMPTY);
		gStopName = PLATFORM_.matcher(gStopName).replaceAll(PLATFORM_REPLACEMENT_);
		gStopName = CleanUtils.cleanBounds(gStopName);
		gStopName = CleanUtils.cleanStreetTypes(gStopName);
		gStopName = CleanUtils.cleanNumbers(gStopName);
		return CleanUtils.cleanLabel(gStopName);
	}

	private static final String UNDERSCORE = "_";

	@Override
	public int getStopId(@NotNull GStop gStop) {
		//noinspection deprecation
		final String stopId = gStop.getStopId();
		if (stopId.equals("Route5A-0549_Victoria Road South at Macalister Boulevard southbound")) {
			return 619;
		}
		if (gStop.getStopCode() != null && gStop.getStopCode().length() > 0 && Utils.isDigitsOnly(gStop.getStopCode())) {
			return Integer.parseInt(gStop.getStopCode());
		}
		int indexOfDASH = stopId.indexOf(DASH);
		int indexOfUNDERSCORE = stopId.indexOf(UNDERSCORE, indexOfDASH);
		if (indexOfDASH >= 0 && indexOfUNDERSCORE >= 0) {
			return Integer.parseInt(stopId.substring(indexOfDASH + 1, indexOfUNDERSCORE));
		}
		throw new MTLog.Fatal("Error while getting stop ID for %s!", gStop);
	}
}
