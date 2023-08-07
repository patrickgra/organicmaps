#include "indexer/drawing_rule_def.hpp"

#include <algorithm>
#include <iterator>

namespace drule
{
using namespace std;

namespace
{
struct less_key
{
  bool operator() (drule::Key const & r1, drule::Key const & r2) const
  {
    // assume that unique algo leaves the first element (with max priority), others - go away
    if (r1.m_type == r2.m_type)
    {
      if (r1.m_hatching != r2.m_hatching)
        return r1.m_hatching;
      return (r1.m_priority > r2.m_priority);
    }
    else
      return (r1.m_type < r2.m_type);
   }
};

struct equal_key
{
  bool operator() (drule::Key const & r1, drule::Key const & r2) const
  {
    // many line rules - is ok, other rules - one is enough
    if (r1.m_type == drule::line)
      return (r1 == r2);
    else
    {
      if (r1.m_type == r2.m_type)
      {
        // Keep several area styles if bigger one (r1) is hatching and second is not.
        if (r1.m_type == drule::area && r1.m_hatching && !r2.m_hatching)
          return false;

        return true;
      }
      return false;
    }
  }
};
}  // namespace

void MakeUnique(KeysT & keys)
{
  sort(keys.begin(), keys.end(), less_key());
  keys.resize(distance(keys.begin(), unique(keys.begin(), keys.end(), equal_key())));
}
}
