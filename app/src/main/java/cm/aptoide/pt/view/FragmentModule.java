package cm.aptoide.pt.view;

import android.content.SharedPreferences;
import android.content.res.Resources;
import android.os.Bundle;
import android.view.WindowManager;
import androidx.fragment.app.Fragment;
import cm.aptoide.accountmanager.AptoideAccountManager;
import cm.aptoide.analytics.AnalyticsManager;
import cm.aptoide.analytics.implementation.navigation.NavigationTracker;
import cm.aptoide.pt.R;
import cm.aptoide.pt.account.AccountAnalytics;
import cm.aptoide.pt.account.ErrorsMapper;
import cm.aptoide.pt.account.view.AccountErrorMapper;
import cm.aptoide.pt.account.view.AccountNavigator;
import cm.aptoide.pt.account.view.ImagePickerNavigator;
import cm.aptoide.pt.account.view.ImagePickerPresenter;
import cm.aptoide.pt.account.view.ImagePickerView;
import cm.aptoide.pt.account.view.ImageValidator;
import cm.aptoide.pt.account.view.PhotoFileGenerator;
import cm.aptoide.pt.account.view.UriToPathResolver;
import cm.aptoide.pt.account.view.store.ManageStoreErrorMapper;
import cm.aptoide.pt.account.view.store.ManageStoreNavigator;
import cm.aptoide.pt.account.view.store.ManageStorePresenter;
import cm.aptoide.pt.account.view.store.ManageStoreView;
import cm.aptoide.pt.account.view.store.StoreManager;
import cm.aptoide.pt.account.view.user.CreateUserErrorMapper;
import cm.aptoide.pt.account.view.user.ManageUserNavigator;
import cm.aptoide.pt.account.view.user.ManageUserPresenter;
import cm.aptoide.pt.account.view.user.ManageUserView;
import cm.aptoide.pt.actions.PermissionManager;
import cm.aptoide.pt.actions.PermissionService;
import cm.aptoide.pt.ads.AdsRepository;
import cm.aptoide.pt.ads.MoPubAdsManager;
import cm.aptoide.pt.app.AdsManager;
import cm.aptoide.pt.app.AppCoinsManager;
import cm.aptoide.pt.app.AppNavigator;
import cm.aptoide.pt.app.AppViewAnalytics;
import cm.aptoide.pt.app.AppViewManager;
import cm.aptoide.pt.app.AppViewModelManager;
import cm.aptoide.pt.app.CampaignAnalytics;
import cm.aptoide.pt.app.DownloadStateParser;
import cm.aptoide.pt.app.FlagManager;
import cm.aptoide.pt.app.FlagService;
import cm.aptoide.pt.app.ReviewsManager;
import cm.aptoide.pt.app.migration.AppcMigrationManager;
import cm.aptoide.pt.app.view.AppCoinsInfoView;
import cm.aptoide.pt.app.view.AppViewFragment;
import cm.aptoide.pt.app.view.AppViewFragment.BundleKeys;
import cm.aptoide.pt.app.view.AppViewNavigator;
import cm.aptoide.pt.app.view.AppViewPresenter;
import cm.aptoide.pt.app.view.AppViewView;
import cm.aptoide.pt.app.view.MoreBundleManager;
import cm.aptoide.pt.app.view.MoreBundlePresenter;
import cm.aptoide.pt.app.view.MoreBundleView;
import cm.aptoide.pt.blacklist.BlacklistManager;
import cm.aptoide.pt.bottomNavigation.BottomNavigationMapper;
import cm.aptoide.pt.crashreports.CrashReport;
import cm.aptoide.pt.dataprovider.WebService;
import cm.aptoide.pt.dataprovider.interfaces.TokenInvalidator;
import cm.aptoide.pt.dataprovider.model.v7.Type;
import cm.aptoide.pt.dataprovider.ws.BodyInterceptor;
import cm.aptoide.pt.dataprovider.ws.v7.BaseBody;
import cm.aptoide.pt.download.DownloadAnalytics;
import cm.aptoide.pt.download.DownloadFactory;
import cm.aptoide.pt.editorial.CardId;
import cm.aptoide.pt.editorial.EditorialAnalytics;
import cm.aptoide.pt.editorial.EditorialFragment;
import cm.aptoide.pt.editorial.EditorialManager;
import cm.aptoide.pt.editorial.EditorialNavigator;
import cm.aptoide.pt.editorial.EditorialPresenter;
import cm.aptoide.pt.editorial.EditorialRepository;
import cm.aptoide.pt.editorial.EditorialService;
import cm.aptoide.pt.editorial.EditorialView;
import cm.aptoide.pt.editorial.Slug;
import cm.aptoide.pt.editorialList.EditorialListAnalytics;
import cm.aptoide.pt.editorialList.EditorialListManager;
import cm.aptoide.pt.editorialList.EditorialListNavigator;
import cm.aptoide.pt.editorialList.EditorialListPresenter;
import cm.aptoide.pt.editorialList.EditorialListRepository;
import cm.aptoide.pt.editorialList.EditorialListService;
import cm.aptoide.pt.editorialList.EditorialListView;
import cm.aptoide.pt.home.AptoideBottomNavigator;
import cm.aptoide.pt.home.ChipManager;
import cm.aptoide.pt.home.Home;
import cm.aptoide.pt.home.HomeAnalytics;
import cm.aptoide.pt.home.HomeContainerNavigator;
import cm.aptoide.pt.home.HomeContainerPresenter;
import cm.aptoide.pt.home.HomeContainerView;
import cm.aptoide.pt.home.HomeNavigator;
import cm.aptoide.pt.home.HomePresenter;
import cm.aptoide.pt.home.HomeView;
import cm.aptoide.pt.home.apps.AppMapper;
import cm.aptoide.pt.home.apps.AppsFragmentView;
import cm.aptoide.pt.home.apps.AppsManager;
import cm.aptoide.pt.home.apps.AppsNavigator;
import cm.aptoide.pt.home.apps.AppsPresenter;
import cm.aptoide.pt.home.apps.SeeMoreAppcFragment;
import cm.aptoide.pt.home.apps.SeeMoreAppcManager;
import cm.aptoide.pt.home.apps.SeeMoreAppcNavigator;
import cm.aptoide.pt.home.apps.SeeMoreAppcPresenter;
import cm.aptoide.pt.home.apps.UpdatesManager;
import cm.aptoide.pt.home.bundles.BundlesRepository;
import cm.aptoide.pt.home.bundles.ads.AdMapper;
import cm.aptoide.pt.home.bundles.ads.banner.BannerRepository;
import cm.aptoide.pt.home.more.appcoins.EarnAppcListConfiguration;
import cm.aptoide.pt.home.more.appcoins.EarnAppcListFragment;
import cm.aptoide.pt.home.more.appcoins.EarnAppcListManager;
import cm.aptoide.pt.home.more.appcoins.EarnAppcListPresenter;
import cm.aptoide.pt.home.more.apps.ListAppsConfiguration;
import cm.aptoide.pt.home.more.apps.ListAppsMoreFragment;
import cm.aptoide.pt.home.more.apps.ListAppsMoreManager;
import cm.aptoide.pt.home.more.apps.ListAppsMorePresenter;
import cm.aptoide.pt.home.more.apps.ListAppsMoreRepository;
import cm.aptoide.pt.install.InstallAnalytics;
import cm.aptoide.pt.install.InstallManager;
import cm.aptoide.pt.navigator.ActivityNavigator;
import cm.aptoide.pt.navigator.FragmentNavigator;
import cm.aptoide.pt.navigator.FragmentResultNavigator;
import cm.aptoide.pt.navigator.Result;
import cm.aptoide.pt.networking.image.ImageLoader;
import cm.aptoide.pt.notification.AppcPromotionNotificationStringProvider;
import cm.aptoide.pt.notification.NotificationAnalytics;
import cm.aptoide.pt.notification.sync.LocalNotificationSyncManager;
import cm.aptoide.pt.permission.AccountPermissionProvider;
import cm.aptoide.pt.presenter.LoginSignUpCredentialsView;
import cm.aptoide.pt.presenter.LoginSignupCredentialsFlavorPresenter;
import cm.aptoide.pt.promotions.ClaimPromotionDialogPresenter;
import cm.aptoide.pt.promotions.ClaimPromotionDialogView;
import cm.aptoide.pt.promotions.ClaimPromotionsManager;
import cm.aptoide.pt.promotions.ClaimPromotionsNavigator;
import cm.aptoide.pt.promotions.PromotionViewAppMapper;
import cm.aptoide.pt.promotions.PromotionsAnalytics;
import cm.aptoide.pt.promotions.PromotionsManager;
import cm.aptoide.pt.promotions.PromotionsNavigator;
import cm.aptoide.pt.promotions.PromotionsPreferencesManager;
import cm.aptoide.pt.promotions.PromotionsPresenter;
import cm.aptoide.pt.promotions.PromotionsView;
import cm.aptoide.pt.reactions.ReactionsManager;
import cm.aptoide.pt.repository.request.RewardAppCoinsAppsRepository;
import cm.aptoide.pt.search.SearchManager;
import cm.aptoide.pt.search.SearchNavigator;
import cm.aptoide.pt.search.analytics.SearchAnalytics;
import cm.aptoide.pt.search.suggestions.SearchSuggestionManager;
import cm.aptoide.pt.search.suggestions.TrendingManager;
import cm.aptoide.pt.search.view.SearchResultPresenter;
import cm.aptoide.pt.search.view.SearchResultView;
import cm.aptoide.pt.store.StoreUtilsProxy;
import cm.aptoide.pt.store.view.StoreTabGridRecyclerFragment.BundleCons;
import cm.aptoide.pt.store.view.my.MyStoresNavigator;
import cm.aptoide.pt.store.view.my.MyStoresPresenter;
import cm.aptoide.pt.store.view.my.MyStoresView;
import cm.aptoide.pt.updates.UpdatesAnalytics;
import cm.aptoide.pt.view.app.AppCenter;
import cm.aptoide.pt.view.wizard.WizardPresenter;
import cm.aptoide.pt.view.wizard.WizardView;
import cm.aptoide.pt.wallet.WalletAppProvider;
import cm.aptoide.pt.wallet.WalletInstallManager;
import com.jakewharton.rxrelay.BehaviorRelay;
import dagger.Module;
import dagger.Provides;
import java.util.Arrays;
import java.util.Map;
import javax.inject.Named;
import okhttp3.OkHttpClient;
import org.parceler.Parcels;
import rx.android.schedulers.AndroidSchedulers;
import rx.schedulers.Schedulers;
import rx.subscriptions.CompositeSubscription;

@Module public class FragmentModule {

  private final Fragment fragment;
  private final Bundle savedInstance;
  private final Bundle arguments;
  private final boolean isCreateStoreUserPrivacyEnabled;
  private final String packageName;

  public FragmentModule(Fragment fragment, Bundle savedInstance, Bundle arguments, boolean isCreateStoreUserPrivacyEnabled, String packageName) {
    this.fragment = fragment;
    this.savedInstance = savedInstance;
    this.arguments = arguments;
    this.isCreateStoreUserPrivacyEnabled = isCreateStoreUserPrivacyEnabled;
    this.packageName = packageName;
  }

  @FragmentScope @Provides LoginSignupCredentialsFlavorPresenter provideLoginSignUpPresenter(
      AptoideAccountManager accountManager, AccountNavigator accountNavigator, AccountErrorMapper errorMapper, AccountAnalytics accountAnalytics) {
    return new LoginSignupCredentialsFlavorPresenter((LoginSignUpCredentialsView) fragment,
        accountManager, CrashReport.getInstance(), arguments.getBoolean("dismiss_to_navigate_to_main_view"),
        arguments.getBoolean("clean_back_stack"), accountNavigator, Arrays.asList("email", "user_friends"), Arrays.asList("email"), errorMapper,
        accountAnalytics);
  }

  @FragmentScope @Provides @Named("home-fragment-navigator") FragmentNavigator provideHomeFragmentNavigator(Map<Integer, Result> fragmentResultMap,
      BehaviorRelay<Map<Integer, Result>> fragmentResultRelay) {
    return new FragmentResultNavigator(fragment.getChildFragmentManager(), R.id.main_home_container_content, android.R.anim.fade_in, android.R.anim.fade_out,
        fragmentResultMap, fragmentResultRelay);
  }

  @FragmentScope @Provides ImagePickerPresenter provideImagePickerPresenter(
      AccountPermissionProvider accountPermissionProvider, PhotoFileGenerator photoFileGenerator,
      ImageValidator imageValidator, UriToPathResolver uriToPathResolver, ImagePickerNavigator imagePickerNavigator) {
    return new ImagePickerPresenter((ImagePickerView) fragment, CrashReport.getInstance(),
        accountPermissionProvider, photoFileGenerator, imageValidator, AndroidSchedulers.mainThread(), uriToPathResolver, imagePickerNavigator,
        fragment.getActivity()
            .getContentResolver(), ImageLoader.with(fragment.getContext()));
  }

  @FragmentScope @Provides ManageStorePresenter provideManageStorePresenter(UriToPathResolver uriToPathResolver, ManageStoreNavigator manageStoreNavigator,
      ManageStoreErrorMapper manageStoreErrorMapper, AptoideAccountManager accountManager,
      AccountAnalytics accountAnalytics) {
    return new ManageStorePresenter((ManageStoreView) fragment, CrashReport.getInstance(),
        uriToPathResolver, packageName, manageStoreNavigator, arguments.getBoolean("go_to_home", true), manageStoreErrorMapper, accountManager,
        arguments.getInt(FragmentNavigator.REQUEST_CODE_EXTRA), accountAnalytics);
  }

  @FragmentScope @Provides ManageUserPresenter provideManageUserPresenter(AptoideAccountManager accountManager, CreateUserErrorMapper errorMapper,
      ManageUserNavigator manageUserNavigator, UriToPathResolver uriToPathResolver, AccountAnalytics accountAnalytics) {
    return new ManageUserPresenter((ManageUserView) fragment, CrashReport.getInstance(),
        accountManager, errorMapper, manageUserNavigator, arguments.getBoolean("is_edit", false),
        uriToPathResolver, isCreateStoreUserPrivacyEnabled, savedInstance == null, accountAnalytics);
  }

  @FragmentScope @Provides ImageValidator provideImageValidator() {
    return new ImageValidator(ImageLoader.with(fragment.getContext()), Schedulers.computation());
  }

  @FragmentScope @Provides CreateUserErrorMapper provideCreateUserErrorMapper(AccountErrorMapper accountErrorMapper) {
    return new CreateUserErrorMapper(fragment.getContext(), accountErrorMapper, fragment.getResources());
  }

  @FragmentScope @Provides AccountErrorMapper provideAccountErrorMapper() {
    return new AccountErrorMapper(fragment.getContext(), new ErrorsMapper());
  }

  @FragmentScope @Provides ManageStoreErrorMapper provideManageStoreErrorMapper() {
    return new ManageStoreErrorMapper(fragment.getResources(), new ErrorsMapper());
  }

  @FragmentScope @Provides SearchResultPresenter provideSearchResultPresenter(SearchAnalytics searchAnalytics, SearchNavigator searchNavigator, SearchManager searchManager,
      TrendingManager trendingManager, SearchSuggestionManager searchSuggestionManager,
      BottomNavigationMapper bottomNavigationMapper) {
    return new SearchResultPresenter((SearchResultView) fragment, searchAnalytics, searchNavigator,
        CrashReport.getInstance(), AndroidSchedulers.mainThread(), searchManager, trendingManager,
        searchSuggestionManager, (AptoideBottomNavigator) fragment.getActivity(),
        bottomNavigationMapper, Schedulers.io());
  }

  @FragmentScope @Provides HomePresenter providesHomePresenter(Home home, HomeNavigator homeNavigator, AdMapper adMapper, AptoideAccountManager aptoideAccountManager,
      HomeAnalytics homeAnalytics) {
    return new HomePresenter((HomeView) fragment, home, AndroidSchedulers.mainThread(), CrashReport.getInstance(), homeNavigator, adMapper, homeAnalytics);
  }

  @FragmentScope @Provides HomeNavigator providesHomeNavigator(
      @Named("main-fragment-navigator") FragmentNavigator fragmentNavigator, BottomNavigationMapper bottomNavigationMapper, AppNavigator appNavigator,
      @Named("aptoide-theme") String theme, AccountNavigator accountNavigator) {
    return new HomeNavigator(fragmentNavigator, (AptoideBottomNavigator) fragment.getActivity(),
        bottomNavigationMapper, appNavigator, ((ActivityNavigator) fragment.getActivity()), theme,
        accountNavigator);
  }

  @FragmentScope @Provides HomeContainerNavigator providesHomeContainerNavigator(
      @Named("home-fragment-navigator") FragmentNavigator childFragmentNavigator) {
    return new HomeContainerNavigator(childFragmentNavigator);
  }

  @FragmentScope @Provides Home providesHome(BundlesRepository bundlesRepository, PromotionsManager promotionsManager,
      PromotionsPreferencesManager promotionsPreferencesManager, BannerRepository bannerRepository,
      MoPubAdsManager moPubAdsManager, BlacklistManager blacklistManager,
      @Named("homePromotionsId") String promotionsType, ReactionsManager reactionsManager) {
    return new Home(bundlesRepository, promotionsManager, bannerRepository, moPubAdsManager,
        promotionsPreferencesManager, blacklistManager, promotionsType, reactionsManager);
  }

  @FragmentScope @Provides MyStoresPresenter providesMyStorePresenter(AptoideAccountManager aptoideAccountManager, MyStoresNavigator navigator) {
    return new MyStoresPresenter((MyStoresView) fragment, AndroidSchedulers.mainThread(),
        aptoideAccountManager, navigator);
  }

  @FragmentScope @Provides MyStoresNavigator providesMyStoreNavigator(
      @Named("main-fragment-navigator") FragmentNavigator fragmentNavigator, BottomNavigationMapper bottomNavigationMapper) {
    return new MyStoresNavigator(fragmentNavigator, (AptoideBottomNavigator) fragment.getActivity(),
        bottomNavigationMapper);
  }

  @FragmentScope @Provides HomeAnalytics providesHomeAnalytics(AnalyticsManager analyticsManager,
      NavigationTracker navigationTracker) {
    return new HomeAnalytics(navigationTracker, analyticsManager);
  }

  @FragmentScope @Provides AppsNavigator providesAppsNavigator(
      @Named("main-fragment-navigator") FragmentNavigator fragmentNavigator, BottomNavigationMapper bottomNavigationMapper, AppNavigator appNavigator) {
    return new AppsNavigator(fragmentNavigator, (AptoideBottomNavigator) fragment.getActivity(),
        bottomNavigationMapper, appNavigator);
  }

  @FragmentScope @Provides FlagManager providesFlagManager(FlagService flagService) {
    return new FlagManager(flagService);
  }

  @FragmentScope @Provides FlagService providesFlagService(@Named("defaultInterceptorV3")
      BodyInterceptor<cm.aptoide.pt.dataprovider.ws.v3.BaseBody> bodyInterceptorV3,
      @Named("default") OkHttpClient okHttpClient, TokenInvalidator tokenInvalidator,
      @Named("default") SharedPreferences sharedPreferences) {
    return new FlagService(bodyInterceptorV3, okHttpClient, tokenInvalidator, sharedPreferences);
  }

  @FragmentScope @Provides AppViewManager providesAppViewManager(AppViewModelManager appViewModelManager, InstallManager installManager,
      DownloadFactory downloadFactory, AppCenter appCenter, ReviewsManager reviewsManager,
      AdsManager adsManager, FlagManager flagManager, StoreUtilsProxy storeUtilsProxy,
      AptoideAccountManager aptoideAccountManager, DownloadStateParser downloadStateParser,
      AppViewAnalytics appViewAnalytics, NotificationAnalytics notificationAnalytics,
      InstallAnalytics installAnalytics, Resources resources, WindowManager windowManager,
      @Named("marketName") String marketName, AppCoinsManager appCoinsManager, MoPubAdsManager moPubAdsManager, PromotionsManager promotionsManager,
      AppcMigrationManager appcMigrationManager, LocalNotificationSyncManager localNotificationSyncManager,
      AppcPromotionNotificationStringProvider appcPromotionNotificationStringProvider) {
    return new AppViewManager(appViewModelManager, installManager, downloadFactory, appCenter,
        reviewsManager, adsManager, flagManager, storeUtilsProxy, aptoideAccountManager,
        moPubAdsManager, downloadStateParser, appViewAnalytics, notificationAnalytics,
        installAnalytics, (Type.APPS_GROUP.getPerLineCount(resources, windowManager) * 6),
        Schedulers.io(), marketName, appCoinsManager, promotionsManager, appcMigrationManager,
        localNotificationSyncManager, appcPromotionNotificationStringProvider);
  }

  @FragmentScope @Provides AppViewModelManager providesAppViewModelManager(AppViewConfiguration appViewConfiguration, StoreManager storeManager,
      @Named("marketName") String marketName, AppCenter appCenter, DownloadStateParser downloadStateParser, InstallManager installManager,
      AppcMigrationManager appcMigrationManager, AppCoinsManager appCoinsManager) {
    return new AppViewModelManager(appViewConfiguration, storeManager, marketName, appCenter,
        downloadStateParser, installManager, appcMigrationManager, appCoinsManager);
  }

  @FragmentScope @Provides AppViewPresenter providesAppViewPresenter(AccountNavigator accountNavigator, AppViewAnalytics analytics,
      CampaignAnalytics campaignAnalytics, AppViewNavigator appViewNavigator, AppViewManager appViewManager, AptoideAccountManager accountManager, CrashReport crashReport,
      PromotionsNavigator promotionsNavigator) {
    return new AppViewPresenter((AppViewView) fragment, accountNavigator, analytics,
        campaignAnalytics, appViewNavigator, appViewManager, accountManager, AndroidSchedulers.mainThread(), crashReport, new PermissionManager(),
        ((PermissionService) fragment.getContext()), promotionsNavigator);
  }

  @FragmentScope @Provides AppViewConfiguration providesAppViewConfiguration() {
    return new AppViewConfiguration(arguments.getLong(BundleKeys.APP_ID.name(), -1), arguments.getString(BundleKeys.PACKAGE_NAME.name(), null),
        arguments.getString(BundleKeys.STORE_NAME.name(), null), arguments.getString(BundleKeys.STORE_THEME.name(), ""),
        Parcels.unwrap(arguments.getParcelable(BundleKeys.MINIMAL_AD.name())), ((AppViewFragment.OpenType) arguments.getSerializable(BundleKeys.SHOULD_INSTALL.name())),
        arguments.getString(BundleKeys.MD5.name(), ""), arguments.getString(BundleKeys.UNAME.name(), ""),
        arguments.getDouble(BundleKeys.APPC.name(), -1), arguments.getString(BundleKeys.EDITORS_CHOICE_POSITION.name(), ""),
        arguments.getString(BundleKeys.ORIGIN_TAG.name(), ""), arguments.getString(BundleKeys.DOWNLOAD_CONVERSION_URL.name(), ""));
  }

  @FragmentScope @Provides MoreBundlePresenter providesGetStoreWidgetsPresenter(MoreBundleManager moreBundleManager, CrashReport crashReport, HomeNavigator homeNavigator,
      AdMapper adMapper, BundleEvent bundleEvent, HomeAnalytics homeAnalytics, ChipManager chipManager) {
    return new MoreBundlePresenter((MoreBundleView) fragment, moreBundleManager, AndroidSchedulers.mainThread(), crashReport, homeNavigator, adMapper, bundleEvent,
        homeAnalytics, chipManager);
  }

  @FragmentScope @Provides MoreBundleManager providesGetStoreManager(BundlesRepository bundlesRepository) {
    return new MoreBundleManager(bundlesRepository);
  }

  @FragmentScope @Provides BundleEvent providesBundleEvent() {
    return new BundleEvent(arguments.getString(BundleCons.TITLE), arguments.getString(BundleCons.ACTION));
  }

  @FragmentScope @Provides WizardPresenter providesWizardPresenter(AptoideAccountManager aptoideAccountManager, CrashReport crashReport,
      AccountAnalytics accountAnalytics) {
    return new WizardPresenter((WizardView) fragment, aptoideAccountManager, crashReport,
        accountAnalytics);
  }

  @FragmentScope @Provides AppCoinsInfoPresenter providesAppCoinsInfoPresenter(AppCoinsInfoNavigator appCoinsInfoNavigator, InstallManager installManager,
      CrashReport crashReport) {
    return new AppCoinsInfoPresenter((AppCoinsInfoView) fragment, appCoinsInfoNavigator,
        installManager, crashReport, AppCoinsInfoNavigator.APPC_WALLET_PACKAGE_NAME,
        AndroidSchedulers.mainThread());
  }

  @FragmentScope @Provides EditorialManager providesEditorialManager(EditorialRepository editorialRepository, InstallManager installManager,
      DownloadFactory downloadFactory, DownloadStateParser downloadStateParser,
      NotificationAnalytics notificationAnalytics, InstallAnalytics installAnalytics,
      EditorialAnalytics editorialAnalytics, ReactionsManager reactionsManager, MoPubAdsManager moPubAdsManager) {
    return new EditorialManager(editorialRepository, getEditorialConfiguration(), installManager,
        downloadFactory, downloadStateParser, notificationAnalytics, installAnalytics,
        editorialAnalytics, reactionsManager, moPubAdsManager);
  }

  private EditorialConfiguration getEditorialConfiguration() {
    String source = arguments.getString(EditorialFragment.CARD_ID, "");
    if (source.equals("")) {
      source = arguments.getString(EditorialFragment.SLUG, "");
      return new EditorialConfiguration(new Slug(source));
    }
    return new EditorialConfiguration(new CardId(source));
  }

  @FragmentScope @Provides EditorialRepository providesEditorialRepository(EditorialService editorialService) {
    return new EditorialRepository(editorialService);
  }

  @FragmentScope @Provides EditorialPresenter providesEditorialPresenter(EditorialManager editorialManager, CrashReport crashReport,
      EditorialAnalytics editorialAnalytics, EditorialNavigator editorialNavigator) {
    return new EditorialPresenter((EditorialView) fragment, editorialManager, AndroidSchedulers.mainThread(), crashReport, new PermissionManager(),
        ((PermissionService) fragment.getContext()), editorialAnalytics, editorialNavigator);
  }

  @FragmentScope @Provides PromotionsPresenter providesPromotionsPresenter(PromotionsManager promotionsManager, PromotionsAnalytics promotionsAnalytics,
      PromotionsNavigator promotionsNavigator, @Named("homePromotionsId") String promotionsType) {
    return new PromotionsPresenter((PromotionsView) fragment, promotionsManager, new PermissionManager(), ((PermissionService) fragment.getContext()),
        AndroidSchedulers.mainThread(), promotionsAnalytics, promotionsNavigator, promotionsType);
  }

  @FragmentScope @Provides PromotionViewAppMapper providesPromotionViewAppMapper(DownloadStateParser downloadStateParser) {
    return new PromotionViewAppMapper(downloadStateParser);
  }

  @FragmentScope @Provides ClaimPromotionDialogPresenter providesClaimPromotionDialogPresenter(
      ClaimPromotionsManager claimPromotionsManager, PromotionsAnalytics promotionsAnalytics,
      ClaimPromotionsNavigator navigator) {
    return new ClaimPromotionDialogPresenter((ClaimPromotionDialogView) fragment, new CompositeSubscription(), AndroidSchedulers.mainThread(), claimPromotionsManager,
        promotionsAnalytics, navigator);
  }

  @FragmentScope @Provides ClaimPromotionsManager providesClaimPromotionsManager(PromotionsManager promotionsManager) {
    return new ClaimPromotionsManager(promotionsManager, arguments.getString("package_name", "default"),
        arguments.getString("promotion_id", "default"));
  }

  @FragmentScope @Provides EditorialListPresenter providesEditorialListPresenter(
      EditorialListManager editorialListManager, AptoideAccountManager aptoideAccountManager,
      EditorialListNavigator editorialListNavigator, EditorialListAnalytics editorialListAnalytics) {
    return new EditorialListPresenter((EditorialListView) fragment, editorialListManager,
        aptoideAccountManager, editorialListNavigator, editorialListAnalytics, CrashReport.getInstance(), AndroidSchedulers.mainThread());
  }

  @FragmentScope @Provides EditorialListManager providesEditorialListManager(EditorialListRepository editorialListRepository, ReactionsManager reactionsManager) {
    return new EditorialListManager(editorialListRepository, reactionsManager);
  }

  @FragmentScope @Provides EditorialListRepository providesEditorialListRepository(
      EditorialListService editorialListService) {
    return new EditorialListRepository(editorialListService);
  }

  @FragmentScope @Provides EditorialListService providesEditorialService(
      @Named("mature-pool-v7") BodyInterceptor<BaseBody> bodyInterceptorPoolV7,
      @Named("default") OkHttpClient okHttpClient, TokenInvalidator tokenInvalidator,
      @Named("default") SharedPreferences sharedPreferences) {
    return new EditorialListService(bodyInterceptorPoolV7, okHttpClient, tokenInvalidator,
        WebService.getDefaultConverter(), sharedPreferences, 10);
  }

  @FragmentScope @Provides EditorialListNavigator providesEditorialListNavigator(
      @Named("main-fragment-navigator") FragmentNavigator fragmentNavigator, AccountNavigator accountNavigator) {
    return new EditorialListNavigator(fragmentNavigator, accountNavigator);
  }

  @FragmentScope @Provides EditorialListAnalytics editorialListAnalytics(NavigationTracker navigationTracker, AnalyticsManager analyticsManager) {
    return new EditorialListAnalytics(navigationTracker, analyticsManager);
  }

  @FragmentScope @Provides EditorialAnalytics providesEditorialAnalytics(DownloadAnalytics downloadAnalytics, AnalyticsManager analyticsManager,
      NavigationTracker navigationTracker) {
    return new EditorialAnalytics(downloadAnalytics, analyticsManager, navigationTracker, arguments.getBoolean("fromHome"));
  }

  @FragmentScope @Provides HomeContainerPresenter providesHomeContainerPresenter(CrashReport crashReport, AptoideAccountManager accountManager,
      HomeContainerNavigator homeContainerNavigator, HomeNavigator homeNavigator, HomeAnalytics homeAnalytics, Home home, ChipManager chipManager) {
    return new HomeContainerPresenter((HomeContainerView) fragment, AndroidSchedulers.mainThread(),
        crashReport, accountManager, homeContainerNavigator, homeNavigator, homeAnalytics, home,
        chipManager);
  }

  @FragmentScope @Provides AppMapper providesAppMapper() {
    return new AppMapper();
  }

  @FragmentScope @Provides AppsManager providesAppsManager(UpdatesManager updatesManager,
      InstallManager installManager, AppMapper appMapper, DownloadAnalytics downloadAnalytics,
      InstallAnalytics installAnalytics, UpdatesAnalytics updatesAnalytics, DownloadFactory downloadFactory, MoPubAdsManager moPubAdsManager,
      PromotionsManager promotionsManager) {
    return new AppsManager(updatesManager, installManager, appMapper, downloadAnalytics,
        installAnalytics, updatesAnalytics, fragment.getContext()
        .getPackageManager(), fragment.getContext(), downloadFactory, moPubAdsManager,
        promotionsManager);
  }

  @FragmentScope @Provides AppsPresenter providesAppsPresenter(AppsManager appsManager,
      AptoideAccountManager aptoideAccountManager, AppsNavigator appsNavigator) {
    return new AppsPresenter(((AppsFragmentView) fragment), appsManager, AndroidSchedulers.mainThread(), Schedulers.io(), CrashReport.getInstance(),
        new PermissionManager(), ((PermissionService) fragment.getContext()), aptoideAccountManager,
        appsNavigator);
  }

  @FragmentScope @Provides SeeMoreAppcManager providesSeeMoreManager(UpdatesManager updatesManager,
      InstallManager installManager, AppMapper appMapper, DownloadAnalytics downloadAnalytics,
      InstallAnalytics installAnalytics, DownloadFactory downloadFactory, PromotionsManager promotionsManager) {
    return new SeeMoreAppcManager(updatesManager, installManager, appMapper, downloadFactory,
        downloadAnalytics, installAnalytics, promotionsManager);
  }

  @FragmentScope @Provides SeeMoreAppcPresenter providesSeeMoreAppcPresenter(SeeMoreAppcManager seeMoreAppcManager, SeeMoreAppcNavigator seeMoreAppcNavigator) {
    return new SeeMoreAppcPresenter(((SeeMoreAppcFragment) fragment), AndroidSchedulers.mainThread(), Schedulers.io(), CrashReport.getInstance(),
        new PermissionManager(), ((PermissionService) fragment.getContext()), seeMoreAppcManager,
        seeMoreAppcNavigator);
  }

  @FragmentScope @Provides AppcPromotionNotificationStringProvider providesAppcPromotionNotificationStringProvider() {
    return new AppcPromotionNotificationStringProvider(fragment.getContext()
        .getString(R.string.promo_update2appc_claim_notification_title), fragment.getContext()
        .getString(R.string.promo_update2appc_claim_notification_body));
  }

  @FragmentScope @Provides EarnAppcListPresenter provideEarnAppCoinsListPresenter(CrashReport crashReport, RewardAppCoinsAppsRepository rewardAppCoinsAppsRepository,
      AnalyticsManager analyticsManager, AppNavigator appNavigator, EarnAppcListConfiguration earnAppcListConfiguration,
      EarnAppcListManager earnAppcListManager) {
    return new EarnAppcListPresenter((EarnAppcListFragment) fragment, AndroidSchedulers.mainThread(), crashReport, rewardAppCoinsAppsRepository, analyticsManager,
        appNavigator, earnAppcListConfiguration, earnAppcListManager, new PermissionManager(), ((PermissionService) fragment.getContext()));
  }

  @FragmentScope @Provides EarnAppcListManager provideEarnAppcListManager(WalletAppProvider walletAppProvider, WalletInstallManager walletInstallManager) {
    return new EarnAppcListManager(walletAppProvider, walletInstallManager);
  }

  @FragmentScope @Provides EarnAppcListConfiguration providesListAppsConfiguration() {
    return new EarnAppcListConfiguration(arguments.getString(BundleCons.TITLE), arguments.getString(BundleCons.TAG));
  }

  @FragmentScope @Provides ListAppsConfiguration providesListAppsMoreConfiguration() {
    return new ListAppsConfiguration(fragment.getArguments()
        .getString(BundleCons.TITLE), arguments.getString(BundleCons.TAG),
        arguments.getString(BundleCons.ACTION), arguments.getString(BundleCons.NAME));
  }

  @FragmentScope @Provides ListAppsMorePresenter providesListAppsMorePresenter(
      CrashReport crashReport, AppNavigator appNavigator,
      @Named("default") SharedPreferences sharedPreferences,
      ListAppsConfiguration listAppsConfiguration, ListAppsMoreManager listAppsMoreManager) {
    return new ListAppsMorePresenter((ListAppsMoreFragment) fragment,
        AndroidSchedulers.mainThread(), crashReport, appNavigator, sharedPreferences,
        listAppsConfiguration, listAppsMoreManager);
  }

  @FragmentScope @Provides ListAppsMoreManager providesListAppsMoreManager(
      ListAppsMoreRepository listAppsMoreRepository, AdsRepository adsRepository) {
    return new ListAppsMoreManager(listAppsMoreRepository, adsRepository);
  }
}