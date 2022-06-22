#import "MWMSearch.h"
#import "MWMFrameworkListener.h"
#import "MWMFrameworkObservers.h"
#import "SwiftBridge.h"

#include <CoreApi/Framework.h>

#include "platform/network_policy.hpp"

namespace {
using Observer = id<MWMSearchObserver>;
using Observers = NSHashTable<Observer>;
} // namespace

@interface MWMSearch () <MWMFrameworkDrapeObserver>

@property(nonatomic) NSUInteger suggestionsCount;
@property(nonatomic) BOOL searchOnMap;

@property(nonatomic) BOOL textChanged;

@property(nonatomic) Observers *observers;

@property(nonatomic) NSUInteger lastSearchTimestamp;

@property(nonatomic) MWMSearchIndex *itemsIndex;

@property(nonatomic) NSInteger searchCount;

@property(copy, nonatomic) NSString *lastQuery;

@end

@implementation MWMSearch {
  search::EverywhereSearchParams m_everywhereParams;
  search::ViewportSearchParams m_viewportParams;
  search::Results m_everywhereResults;
  search::Results m_viewportResults;
  std::vector<search::ProductInfo> m_productInfo;
}

#pragma mark - Instance

+ (MWMSearch *)manager {
  static MWMSearch *manager;
  static dispatch_once_t onceToken;
  dispatch_once(&onceToken, ^{
    manager = [[self alloc] initManager];
  });
  return manager;
}

- (instancetype)initManager {
  self = [super init];
  if (self) {
    _observers = [Observers weakObjectsHashTable];
    [MWMFrameworkListener addObserver:self];
  }
  return self;
}

- (void)searchEverywhere {
  self.lastSearchTimestamp += 1;
  NSUInteger const timestamp = self.lastSearchTimestamp;
  m_everywhereParams.m_onResults = [self, timestamp](search::Results const &results,
                                                     std::vector<search::ProductInfo> const &productInfo) {
    if (timestamp == self.lastSearchTimestamp) {
      self->m_everywhereResults = results;
      self->m_productInfo = productInfo;
      self.suggestionsCount = results.GetSuggestsCount();

      [self onSearchResultsUpdated];
    }

    if (results.IsEndMarker())
      self.searchCount -= 1;
  };

  GetFramework().GetSearchAPI().SearchEverywhere(m_everywhereParams);
  self.searchCount += 1;
}

- (void)searchInViewport {
  m_viewportParams.m_onStarted = [self] { self.searchCount += 1; };
  m_viewportParams.m_onCompleted = [self](search::Results const &results) {
    if (!results.IsEndMarker())
      return;
    if (!results.IsEndedCancelled())
      self->m_viewportResults = results;
    self.searchCount -= 1;
  };

  GetFramework().GetSearchAPI().SearchInViewport(m_viewportParams);
}

- (void)update {
  [self reset];
  if (m_everywhereParams.m_query.empty())
    return;

  if (IPAD) {
    [self searchInViewport];
    [self searchEverywhere];
  } else {
    if (self.searchOnMap)
      [self searchInViewport];
    else
      [self searchEverywhere];
  }
}

#pragma mark - Add/Remove Observers

+ (void)addObserver:(id<MWMSearchObserver>)observer {
  [[MWMSearch manager].observers addObject:observer];
}

+ (void)removeObserver:(id<MWMSearchObserver>)observer {
  [[MWMSearch manager].observers removeObject:observer];
}

#pragma mark - Methods

+ (void)saveQuery:(NSString *)query forInputLocale:(NSString *)inputLocale {
  if (!query || query.length == 0)
    return;

  std::string const locale = (!inputLocale || inputLocale.length == 0)
                               ? [MWMSearch manager]->m_everywhereParams.m_inputLocale
                               : inputLocale.UTF8String;
  std::string const text = query.precomposedStringWithCompatibilityMapping.UTF8String;
  GetFramework().GetSearchAPI().SaveSearchQuery(make_pair(locale, text));
}

+ (void)searchQuery:(NSString *)query forInputLocale:(NSString *)inputLocale withCategory:(BOOL)isCategory {
  if (!query)
    return;

  MWMSearch *manager = [MWMSearch manager];
  if (inputLocale.length != 0) {
    std::string const locale = inputLocale.UTF8String;
    manager->m_everywhereParams.m_inputLocale = locale;
    manager->m_viewportParams.m_inputLocale = locale;
  }

  manager.lastQuery = query.precomposedStringWithCompatibilityMapping;
  std::string const text = manager.lastQuery.UTF8String;
  manager->m_everywhereParams.m_query = text;
  manager->m_viewportParams.m_query = text;
  manager.textChanged = YES;

  manager->m_everywhereParams.m_isCategory = manager->m_viewportParams.m_isCategory = (isCategory == YES);

  [manager update];
}

+ (void)showResult:(search::Result const &)result {
  GetFramework().ShowSearchResult(result);
}
+ (search::Result const &)resultWithContainerIndex:(NSUInteger)index {
  return [MWMSearch manager]->m_everywhereResults[index];
}

+ (search::ProductInfo const &)productInfoWithContainerIndex:(NSUInteger)index {
  return [MWMSearch manager]->m_productInfo[index];
}

+ (BOOL)isFeatureAt:(NSUInteger)index in:(std::vector<FeatureID> const &)array {
  auto const &result = [self resultWithContainerIndex:index];
  if (result.GetResultType() != search::Result::Type::Feature)
    return NO;
  auto const &resultFeatureID = result.GetFeatureID();
  return std::binary_search(array.begin(), array.end(), resultFeatureID);
}

+ (MWMSearchItemType)resultTypeWithRow:(NSUInteger)row {
  auto itemsIndex = [MWMSearch manager].itemsIndex;
  return [itemsIndex resultTypeWithRow:row];
}

+ (NSUInteger)containerIndexWithRow:(NSUInteger)row {
  auto itemsIndex = [MWMSearch manager].itemsIndex;
  return [itemsIndex resultContainerIndexWithRow:row];
}

- (void)reset {
  self.lastSearchTimestamp += 1;
  GetFramework().GetSearchAPI().CancelAllSearches();

  m_everywhereResults.Clear();
  m_viewportResults.Clear();

  [self onSearchResultsUpdated];
}

+ (void)clear {
  auto manager = [MWMSearch manager];
  manager->m_everywhereParams.m_query.clear();
  manager->m_viewportParams.m_query.clear();
  manager.suggestionsCount = 0;
  [manager reset];
}

+ (void)setSearchOnMap:(BOOL)searchOnMap {
  if (IPAD)
    return;
  MWMSearch *manager = [MWMSearch manager];
  if (manager.searchOnMap == searchOnMap)
    return;
  manager.searchOnMap = searchOnMap;
  if (searchOnMap && ![MWMRouter isRoutingActive])
    GetFramework().ShowSearchResults(manager->m_everywhereResults);
  [manager update];
}

+ (NSUInteger)suggestionsCount {
  return [MWMSearch manager].suggestionsCount;
}
+ (NSUInteger)resultsCount {
  return [MWMSearch manager].itemsIndex.count;
}

- (void)updateItemsIndexWithBannerReload:(BOOL)reloadBanner {
  auto const resultsCount = self->m_everywhereResults.GetCount();
  auto const itemsIndex = [[MWMSearchIndex alloc] initWithSuggestionsCount:self.suggestionsCount
                                                              resultsCount:resultsCount];
  [itemsIndex build];
  self.itemsIndex = itemsIndex;
}

#pragma mark - Notifications

- (void)onSearchStarted {
  for (Observer observer in self.observers) {
    if ([observer respondsToSelector:@selector(onSearchStarted)])
      [observer onSearchStarted];
  }
}

- (void)onSearchCompleted {
  [self updateItemsIndexWithBannerReload:YES];
  for (Observer observer in self.observers) {
    if ([observer respondsToSelector:@selector(onSearchCompleted)])
      [observer onSearchCompleted];
  }
}

- (void)onSearchResultsUpdated {
  [self updateItemsIndexWithBannerReload:NO];
  for (Observer observer in self.observers) {
    if ([observer respondsToSelector:@selector(onSearchResultsUpdated)])
      [observer onSearchResultsUpdated];
  }
}

#pragma mark - MWMFrameworkDrapeObserver

- (void)processViewportChangedEvent {
  if (!GetFramework().GetSearchAPI().IsViewportSearchActive())
    return;
  if (IPAD)
    [self searchEverywhere];
}

#pragma mark - Properties

- (void)setSearchCount:(NSInteger)searchCount {
  NSAssert((searchCount >= 0) && ((_searchCount == searchCount - 1) || (_searchCount == searchCount + 1)),
           @"Invalid search count update");
  if (_searchCount == 0)
    [self onSearchStarted];
  else if (searchCount == 0)
    [self onSearchCompleted];
  _searchCount = searchCount;
}

@end
