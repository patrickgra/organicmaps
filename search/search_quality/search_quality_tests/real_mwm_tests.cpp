#include "testing/testing.hpp"

#include "search/search_tests_support/helpers.hpp"

#include "geometry/distance_on_sphere.hpp"

namespace real_mwm_tests
{

class MwmTestsFixture : public search::tests_support::SearchTest
{
public:
  // Pass LDEBUG to verbose logs for debugging.
  MwmTestsFixture() : search::tests_support::SearchTest(LINFO) {}

  // Default top POIs count to check types or distances.
  static size_t constexpr kTopPoiResultsCount = 5;
  // Feature's centers table is created with low coordinates precision for better compression,
  // so distance-to-pivot is not precise and real meters distance may differ.
  static double constexpr kDistanceEpsilon = 5;
  static double constexpr kDefaultViewportRadiusM = 10000;
  static double constexpr kLoadMwmRadiusM = 200000;

  void SetViewportAndLoadMaps(ms::LatLon const & center, double radiusM = kDefaultViewportRadiusM)
  {
    RegisterLocalMapsInViewport(mercator::MetersToXY(center.m_lon, center.m_lat, kLoadMwmRadiusM));

    SetViewport(center, radiusM);
  }

  using ResultsT = std::vector<search::Result>;

  class Range
  {
    ResultsT const & m_v;
    size_t m_beg, m_end;

  public:
    explicit Range(ResultsT const & v) : m_v(v), m_beg(0), m_end(kTopPoiResultsCount) {}
    Range(ResultsT const & v, size_t beg, size_t end = kTopPoiResultsCount) : m_v(v), m_beg(beg), m_end(end) {}

    size_t size() const { return m_end - m_beg; }
    auto begin() const { return m_v.begin() + m_beg; }
    auto end() const { return m_v.begin() + m_end; }
    auto const & operator[](size_t i) const { return *(begin() + i); }
  };

  /// @return First (minimal) distance in meters.
  static double SortedByDistance(Range const & results, ms::LatLon const & center)
  {
    double const firstDist = ms::DistanceOnEarth(center, mercator::ToLatLon(results[0].GetFeatureCenter()));

    double prevDist = firstDist;
    for (size_t i = 1; i < results.size(); ++i)
    {
      double const dist = ms::DistanceOnEarth(center, mercator::ToLatLon(results[i].GetFeatureCenter()));
      TEST_LESS(prevDist, dist + kDistanceEpsilon, (results[i-1], results[i]));
      prevDist = dist;
    }

    return firstDist;
  }

  static std::vector<uint32_t> GetClassifTypes(std::vector<base::StringIL> const & arr)
  {
    std::vector<uint32_t> res;
    res.reserve(arr.size());

    Classificator const & c = classif();
    for (auto const & e : arr)
      res.push_back(c.GetTypeByPath(e));
    return res;
  }

  static void EqualClassifType(Range const & results, std::vector<uint32_t> const & types)
  {
    for (auto const & r : results)
    {
      auto const it = std::find_if(types.begin(), types.end(), [type = r.GetFeatureType()](uint32_t inType)
      {
        uint32_t t = type;
        ftype::TruncValue(t, ftype::GetLevel(inType));
        return t == inType;
      });

      TEST(it != types.end(), (r));
    }
  }

  static void NameStartsWith(Range const & results, base::StringIL const & prefixes)
  {
    for (auto const & r : results)
    {
      auto const it = std::find_if(prefixes.begin(), prefixes.end(), [name = r.GetString()](char const * prefix)
      {
        return strings::StartsWith(name, prefix);
      });

      TEST(it != prefixes.end(), (r));
    }
  }

  /// @param[in] rect { min_lon, min_lat, max_lon, max_lat }
  static void CenterInRect(Range const & results, m2::RectD const & rect)
  {
    for (auto const & r : results)
    {
      auto const ll = mercator::ToLatLon(r.GetFeatureCenter());
      TEST(rect.IsPointInside({ll.m_lon, ll.m_lat}), (r));
    }
  }
};

// https://github.com/organicmaps/organicmaps/issues/3026
UNIT_CLASS_TEST(MwmTestsFixture, Berlin_Rewe)
{
  // Berlin
  ms::LatLon const center(52.5170365, 13.3888599);
  SetViewportAndLoadMaps(center);

  auto request = MakeRequest("rewe");
  auto const & results = request->Results();
  TEST_GREATER(results.size(), kTopPoiResultsCount, ());

  TEST_EQUAL(results[0].GetFeatureType(), classif().GetTypeByPath({"amenity", "fast_food"}), ());

  Range const range(results, 1);
  EqualClassifType(range, GetClassifTypes({{"shop"}}));
  double const dist = SortedByDistance(range, center);
  TEST_LESS(dist, 1000, ());
}

// https://github.com/organicmaps/organicmaps/issues/1376
UNIT_CLASS_TEST(MwmTestsFixture, Madrid_Carrefour)
{
  // Madrid
  ms::LatLon const center(40.41048, -3.69773);
  SetViewportAndLoadMaps(center);

  auto request = MakeRequest("carrefour");
  auto const & results = request->Results();
  TEST_GREATER(results.size(), 10, ());

  /// @todo 'Carrefour' city in Haiti :)
  TEST_EQUAL(results[0].GetFeatureType(), classif().GetTypeByPath({"place", "city", "capital", "3"}), ());

  Range const range(results, 1, 10);
  EqualClassifType(range, GetClassifTypes({{"shop"}}));
  double const dist = SortedByDistance(range, center);
  TEST_LESS(dist, 500, ());
}

// https://github.com/organicmaps/organicmaps/issues/2530
UNIT_CLASS_TEST(MwmTestsFixture, Nicosia_Jumbo)
{
  // Nicosia
  ms::LatLon const center(35.16915, 33.36141);
  SetViewportAndLoadMaps(center);

  auto request = MakeRequest("jumb");
  auto const & results = request->Results();
  TEST_GREATER(results.size(), kTopPoiResultsCount, ());

  Range const range(results);
  EqualClassifType(range, GetClassifTypes({{"shop"}, {"amenity", "parking"}}));
  double const dist = SortedByDistance(range, center);
  TEST_LESS(dist, 5000, ());
}

// https://github.com/organicmaps/organicmaps/issues/2470
UNIT_CLASS_TEST(MwmTestsFixture, Aarhus_Netto)
{
  // Aarhus
  ms::LatLon const center(56.14958, 10.20394);
  SetViewportAndLoadMaps(center);

  auto request = MakeRequest("netto");
  auto const & results = request->Results();
  TEST_GREATER(results.size(), kTopPoiResultsCount, ());

  Range const range(results);
  EqualClassifType(range, GetClassifTypes({{"shop"}}));
  double const dist = SortedByDistance(range, center);
  TEST_LESS(dist, 500, ());
}

// https://github.com/organicmaps/organicmaps/issues/2133
UNIT_CLASS_TEST(MwmTestsFixture, NY_Subway)
{
  // New York
  ms::LatLon const center(40.7355019, -73.9948155);
  SetViewportAndLoadMaps(center);

  // Interesting case, because Manhattan has high density of:
  // - "Subway" fast food
  // - railway-subway category;
  // - bus stops with name ".. subway ..";
  // + Some noname cities LIKE("Subway", 1 error) in the World.
  auto request = MakeRequest("subway");
  auto const & results = request->Results();
  TEST_GREATER(results.size(), kTopPoiResultsCount, ());

  Range const range(results, 0, 3);
  EqualClassifType(range, GetClassifTypes({{"amenity", "fast_food"}}));
  double const dist = SortedByDistance(range, center);
  TEST_LESS(dist, 1000, ());
}

// https://github.com/organicmaps/organicmaps/issues/1997
UNIT_CLASS_TEST(MwmTestsFixture, London_Asda)
{
  // London
  ms::LatLon const center(51.50295, 0.00325);
  SetViewportAndLoadMaps(center);

  auto request = MakeRequest("asda");
  auto const & results = request->Results();
  TEST_GREATER(results.size(), kTopPoiResultsCount, ());

  /// @todo 3 only because cafe is better than fuel, despite fuel is closer.
  Range const range(results, 0, 3);
  EqualClassifType(range, GetClassifTypes({{"shop"}, {"amenity"}}));
  double const dist = SortedByDistance(range, center);
  TEST_LESS(dist, 2000, ());
}

// https://github.com/organicmaps/organicmaps/issues/3103
UNIT_CLASS_TEST(MwmTestsFixture, Lyon_Aldi)
{
  // Lyon
  ms::LatLon const center(45.7578137, 4.8320114);
  SetViewportAndLoadMaps(center);

  auto request = MakeRequest("aldi");
  auto const & results = request->Results();
  TEST_GREATER(results.size(), kTopPoiResultsCount, ());

  Range const range(results);
  EqualClassifType(range, GetClassifTypes({{"shop", "supermarket"}}));
  double const dist = SortedByDistance(range, center);
  TEST_LESS(dist, 4000, ());
}

// https://github.com/organicmaps/organicmaps/issues/1262
UNIT_CLASS_TEST(MwmTestsFixture, NY_BarnesNoble)
{
  // New York
  ms::LatLon const center(40.7355019, -73.9948155);
  SetViewportAndLoadMaps(center);

  auto request = MakeRequest("barne's & noble");
  auto const & results = request->Results();
  TEST_GREATER(results.size(), 10, ());

  TEST_EQUAL(results[0].GetFeatureType(), classif().GetTypeByPath({"amenity", "cafe"}), ());

  Range const range(results, 1);
  EqualClassifType(range, GetClassifTypes({{"shop", "books"}}));
  double const dist = SortedByDistance(range, center);
  TEST_LESS(dist, 2000, ());
}

// https://github.com/organicmaps/organicmaps/issues/2470
UNIT_CLASS_TEST(MwmTestsFixture, Hamburg_Park)
{
  // Hamburg
  ms::LatLon const center(53.5503410, 10.0006540);
  SetViewportAndLoadMaps(center);

  auto request = MakeRequest("Heide-Park");
  auto const & results = request->Results();
  TEST_GREATER(results.size(), kTopPoiResultsCount, ());

  Range const range(results, 0, 3);
  EqualClassifType(range, GetClassifTypes({{"tourism"}, {"amenity", "fast_food"}, {"highway", "bus_stop"}}));
  NameStartsWith(range, {"Heide Park", "Heide-Park"});
  double const dist = SortedByDistance(range, center);
  TEST_LESS(dist, 100000, ());
}

// https://github.com/organicmaps/organicmaps/issues/1560
UNIT_CLASS_TEST(MwmTestsFixture, Barcelona_Carrers)
{
  // Barcelona
  ms::LatLon const center(41.3828939, 2.177432);
  SetViewportAndLoadMaps(center);

  {
    auto request = MakeRequest("carrer de napols");
    auto const & results = request->Results();
    TEST_GREATER(results.size(), kTopPoiResultsCount, ());

    Range const range(results, 0, 4);
    EqualClassifType(range, GetClassifTypes({{"highway"}}));
    CenterInRect(range, {2.1651583, 41.3899995, 2.1863021, 41.4060494});
  }

  {
    auto request = MakeRequest("carrer de les planes sabadell");
    auto const & results = request->Results();
    TEST_GREATER(results.size(), kTopPoiResultsCount, ());

    Range const range(results, 0, 1);
    EqualClassifType(range, GetClassifTypes({{"highway"}}));
    CenterInRect(range, {2.1078314, 41.5437515, 2.1106129, 41.5438819});
  }
}

} // namespace real_mwm_tests
