#pragma once

#include <cstddef>

namespace search
{

// Performance/quality sensitive settings. They are recommended, but not mandatory.
// Radius is in meters from one of the predefined pivots:
// - viewport center
// - user's position
// - matched city center
struct RecommendedFilteringParams
{
  /// @name When reading and matching features "along" the street.
  /// @{
  // Streets search radius, can be ignored if streets count in area is less than m_maxStreetsCount.
  double m_streetSearchRadiusM = 80000;
  // Max number of street cadidates. Streets count can be greater, if they are all inside m_streetSearchRadiusM area.
  size_t m_maxStreetsCount = 100;
  /// @}

  // Villages search radius.
  double m_villageSearchRadiusM = 200000;
};

} // namespace search
