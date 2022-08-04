#include "search/search_tests_support/helpers.hpp"

#include "storage/country_info_getter.hpp"

#include "indexer/scales.hpp"

#include "geometry/mercator.hpp"
#include "geometry/rect2d.hpp"

namespace search
{
namespace tests_support
{
using namespace std;

SearchTest::SearchTest(base::LogLevel logLevel /* = base::LDEBUG */)
  : m_scopedLog(logLevel)
  , m_engine(m_dataSource, make_unique<storage::CountryInfoGetterForTesting>(), {})
{
  SetViewport(mercator::Bounds::FullRect());
}

void SearchTest::SetViewport(ms::LatLon const & ll, double radiusM)
{
  SetViewport(mercator::MetersToXY(ll.m_lon, ll.m_lat, radiusM));
}

void SearchTest::RegisterCountry(string const & name, m2::RectD const & rect)
{
  auto & infoGetter =
      static_cast<storage::CountryInfoGetterForTesting &>(m_engine.GetCountryInfoGetter());
  infoGetter.AddCountry(storage::CountryDef(name, rect));
}

bool SearchTest::CategoryMatch(std::string const & query, Rules const & rules, string const & locale)
{
  TestSearchRequest request(m_engine, query, locale, Mode::Everywhere, m_viewport);
  request.SetCategorial();

  request.Run();
  return MatchResults(m_dataSource, rules, request.Results());
}

bool SearchTest::ResultsMatch(std::string const & query, Rules const & rules,
                              std::string const & locale /* = "en" */,
                              Mode mode /* = Mode::Everywhere */)
{
  TestSearchRequest request(m_engine, query, locale, mode, m_viewport);
  request.Run();
  return MatchResults(m_dataSource, rules, request.Results());
}

bool SearchTest::ResultsMatch(vector<search::Result> const & results, Rules const & rules)
{
  return MatchResults(m_dataSource, rules, results);
}

bool SearchTest::ResultsMatch(SearchParams const & params, Rules const & rules)
{
  TestSearchRequest request(m_engine, params);
  request.Run();
  return ResultsMatch(request.Results(), rules);
}

bool SearchTest::IsResultMatches(search::Result const & result, Rule const & rule)
{
  return tests_support::ResultMatches(m_dataSource, rule, result);
}

bool SearchTest::AlternativeMatch(string const & query, vector<Rules> const & rulesList)
{
  TestSearchRequest request(m_engine, query, "en", Mode::Everywhere, m_viewport);
  request.Run();
  return tests_support::AlternativeMatch(m_dataSource, rulesList, request.Results());
}

size_t SearchTest::GetResultsNumber(string const & query, string const & locale)
{
  TestSearchRequest request(m_engine, query, locale, Mode::Everywhere, m_viewport);
  request.Run();
  return request.Results().size();
}

unique_ptr<TestSearchRequest> SearchTest::MakeRequest(SearchParams const & params)
{
  auto request = make_unique<TestSearchRequest>(m_engine, params);
  request->Run();
  return request;
}

unique_ptr<TestSearchRequest> SearchTest::MakeRequest(
    string const & query, string const & locale /* = "en" */)
{
  SearchParams params;
  params.m_query = query;
  params.m_inputLocale = locale;
  params.m_viewport = m_viewport;
  params.m_mode = Mode::Everywhere;
  params.m_needAddress = true;
  params.m_suggestsEnabled = false;
  params.m_streetSearchRadiusM = TestSearchRequest::kDefaultTestStreetSearchRadiusM;
  params.m_villageSearchRadiusM = TestSearchRequest::kDefaultTestVillageSearchRadiusM;

  auto request = make_unique<TestSearchRequest>(m_engine, params);
  request->Run();
  return request;
}

size_t SearchTest::CountFeatures(m2::RectD const & rect)
{
  size_t count = 0;
  auto counter = [&count](const FeatureType & /* ft */) { ++count; };
  m_dataSource.ForEachInRect(counter, rect, scales::GetUpperScale());
  return count;
}

// static
void SearchTest::OnMwmBuilt(MwmInfo const & info)
{
  switch (info.GetType())
  {
  case MwmInfo::COUNTRY: RegisterCountry(info.GetCountryName(), info.m_bordersRect); break;
  case MwmInfo::WORLD: m_engine.LoadCitiesBoundaries(); break;
  case MwmInfo::COASTS: break;
  }
}

} // namespace tests_supoort
} // namespace search
