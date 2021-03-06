package cm.aptoide.pt;

import android.annotation.SuppressLint;
import android.app.Application;
import android.app.UiModeManager;
import android.content.Context;
import android.content.Intent;
import android.content.SharedPreferences;
import android.content.pm.PackageInfo;
import android.content.pm.PackageManager;
import android.content.res.Configuration;
import android.content.res.XmlResourceParser;
import android.os.Bundle;
import android.os.Environment;
import android.preference.PreferenceManager;
import android.util.Log;
import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.multidex.MultiDex;
import androidx.work.WorkManager;
import cm.aptoide.accountmanager.AdultContent;
import cm.aptoide.accountmanager.AptoideAccountManager;
import cm.aptoide.analytics.AnalyticsManager;
import cm.aptoide.analytics.implementation.navigation.NavigationTracker;
import cm.aptoide.pt.abtesting.AppsNameExperimentManager;
import cm.aptoide.pt.account.AdultContentAnalytics;
import cm.aptoide.pt.account.MatureBodyInterceptorV7;
import cm.aptoide.pt.ads.AdsRepository;
import cm.aptoide.pt.ads.AdsUserPropertyManager;
import cm.aptoide.pt.analytics.FirstLaunchAnalytics;
import cm.aptoide.pt.crashreports.ConsoleLogger;
import cm.aptoide.pt.crashreports.CrashReport;
import cm.aptoide.pt.database.RoomInstalledPersistence;
import cm.aptoide.pt.database.RoomNotificationPersistence;
import cm.aptoide.pt.database.room.AptoideDatabase;
import cm.aptoide.pt.database.room.RoomInstalled;
import cm.aptoide.pt.dataprovider.WebService;
import cm.aptoide.pt.dataprovider.cache.L2Cache;
import cm.aptoide.pt.dataprovider.interfaces.TokenInvalidator;
import cm.aptoide.pt.dataprovider.ws.BodyInterceptor;
import cm.aptoide.pt.dataprovider.ws.v2.aptwords.AdsApplicationVersionCodeProvider;
import cm.aptoide.pt.dataprovider.ws.v7.BaseBody;
import cm.aptoide.pt.dataprovider.ws.v7.BaseRequestWithStore;
import cm.aptoide.pt.dataprovider.ws.v7.store.GetStoreMetaRequest;
import cm.aptoide.pt.download.OemidProvider;
import cm.aptoide.pt.downloadmanager.AptoideDownloadManager;
import cm.aptoide.pt.file.CacheHelper;
import cm.aptoide.pt.file.FileManager;
import cm.aptoide.pt.install.InstallManager;
import cm.aptoide.pt.install.PackageRepository;
import cm.aptoide.pt.install.installer.RootInstallationRetryHandler;
import cm.aptoide.pt.leak.LeakTool;
import cm.aptoide.pt.link.AptoideInstallParser;
import cm.aptoide.pt.logger.Logger;
import cm.aptoide.pt.navigator.Result;
import cm.aptoide.pt.networking.AuthenticationPersistence;
import cm.aptoide.pt.networking.IdsRepository;
import cm.aptoide.pt.networking.Pnp1AuthorizationInterceptor;
import cm.aptoide.pt.notification.NotificationAnalytics;
import cm.aptoide.pt.notification.NotificationCenter;
import cm.aptoide.pt.notification.NotificationInfo;
import cm.aptoide.pt.notification.NotificationPolicyFactory;
import cm.aptoide.pt.notification.NotificationProvider;
import cm.aptoide.pt.notification.NotificationService;
import cm.aptoide.pt.notification.NotificationSyncScheduler;
import cm.aptoide.pt.notification.NotificationsCleaner;
import cm.aptoide.pt.notification.SystemNotificationShower;
import cm.aptoide.pt.notification.UpdatesNotificationManager;
import cm.aptoide.pt.notification.UpdatesNotificationWorkerFactory;
import cm.aptoide.pt.notification.sync.NotificationSyncFactory;
import cm.aptoide.pt.notification.sync.NotificationSyncManager;
import cm.aptoide.pt.preferences.AptoideMd5Manager;
import cm.aptoide.pt.preferences.PRNGFixes;
import cm.aptoide.pt.preferences.Preferences;
import cm.aptoide.pt.preferences.secure.SecurePreferences;
import cm.aptoide.pt.preferences.secure.SecurePreferencesImplementation;
import cm.aptoide.pt.preferences.toolbox.ToolboxManager;
import cm.aptoide.pt.presenter.View;
import cm.aptoide.pt.root.RootAvailabilityManager;
import cm.aptoide.pt.search.suggestions.SearchSuggestionManager;
import cm.aptoide.pt.search.suggestions.TrendingManager;
import cm.aptoide.pt.store.StoreCredentialsProvider;
import cm.aptoide.pt.store.StoreUtilsProxy;
import cm.aptoide.pt.sync.SyncScheduler;
import cm.aptoide.pt.sync.alarm.SyncStorage;
import cm.aptoide.pt.themes.NewFeature;
import cm.aptoide.pt.themes.NewFeatureManager;
import cm.aptoide.pt.themes.ThemeAnalytics;
import cm.aptoide.pt.updates.UpdateRepository;
import cm.aptoide.pt.util.PreferencesXmlParser;
import cm.aptoide.pt.utils.AptoideUtils;
import cm.aptoide.pt.utils.FileUtils;
import cm.aptoide.pt.utils.q.QManager;
import cm.aptoide.pt.view.ActivityModule;
import cm.aptoide.pt.view.ActivityProvider;
import cm.aptoide.pt.view.BaseActivity;
import cm.aptoide.pt.view.BaseFragment;
import cm.aptoide.pt.view.FragmentModule;
import cm.aptoide.pt.view.FragmentProvider;
import cm.aptoide.pt.view.MainActivity;
import cm.aptoide.pt.view.configuration.implementation.VanillaActivityProvider;
import cm.aptoide.pt.view.configuration.implementation.VanillaFragmentProvider;
import cm.aptoide.pt.view.recycler.DisplayableWidgetMapping;
import com.flurry.android.FlurryAgent;
import com.jakewharton.rxrelay.BehaviorRelay;
import com.jakewharton.rxrelay.PublishRelay;
import com.mopub.common.MoPub;
import com.mopub.common.SdkConfiguration;
import com.mopub.common.logging.MoPubLog;
import com.mopub.mobileads.GooglePlayServicesAdapterConfiguration;
import com.mopub.nativeads.AppLovinBaseAdapterConfiguration;
import com.mopub.nativeads.AppnextBaseAdapterConfiguration;
import com.mopub.nativeads.InMobiBaseAdapterConfiguration;
import com.mopub.nativeads.InneractiveAdapterConfiguration;
import io.rakam.api.Rakam;
import io.rakam.api.RakamClient;
import io.sentry.Sentry;
import io.sentry.android.AndroidSentryClientFactory;
import java.io.IOException;
import java.net.MalformedURLException;
import java.net.URL;
import java.util.ArrayList;
import java.util.Calendar;
import java.util.Collections;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.TimeZone;
import java.util.concurrent.TimeUnit;
import javax.inject.Inject;
import javax.inject.Named;
import okhttp3.OkHttpClient;
import org.xmlpull.v1.XmlPullParserException;
import rx.Completable;
import rx.Observable;
import rx.Single;
import rx.android.schedulers.AndroidSchedulers;
import rx.functions.Action1;
import rx.schedulers.Schedulers;

import static cm.aptoide.pt.preferences.managed.ManagedKeys.CAMPAIGN_SOCIAL_NOTIFICATIONS_PREFERENCE_VIEW_KEY;

public abstract class AptoideApplication extends Application {

  static final String CACHE_FILE_NAME = "aptoide.wscache";
  private static final String TAG = AptoideApplication.class.getName();
  private static FragmentProvider fragmentProvider;
  private static ActivityProvider activityProvider;
  private static DisplayableWidgetMapping displayableWidgetMapping;
  @Inject AptoideDatabase aptoideDatabase;
  @Inject RoomNotificationPersistence notificationPersistence;
  @Inject RoomInstalledPersistence roomInstalledPersistence;
  @Inject @Named("base-rakam-host") String rakamBaseHost;
  @Inject AptoideDownloadManager aptoideDownloadManager;
  @Inject UpdateRepository updateRepository;
  @Inject CacheHelper cacheHelper;
  @Inject AptoideAccountManager accountManager;
  @Inject Preferences preferences;
  @Inject @Named("secure") cm.aptoide.pt.preferences.SecurePreferences securePreferences;
  @Inject AdultContent adultContent;
  @Inject IdsRepository idsRepository;
  @Inject @Named("default") OkHttpClient defaultClient;
  @Inject RootAvailabilityManager rootAvailabilityManager;
  @Inject AuthenticationPersistence authenticationPersistence;
  @Inject SyncScheduler alarmSyncScheduler;
  @Inject @Named("mature-pool-v7") BodyInterceptor<BaseBody> bodyInterceptorPoolV7;
  @Inject @Named("web-v7") BodyInterceptor<BaseBody> bodyInterceptorWebV7;
  @Inject @Named("defaultInterceptorV3") BodyInterceptor<cm.aptoide.pt.dataprovider.ws.v3.BaseBody>
      bodyInterceptorV3;
  @Inject L2Cache httpClientCache;
  @Inject QManager qManager;
  @Inject TokenInvalidator tokenInvalidator;
  @Inject PackageRepository packageRepository;
  @Inject AdsApplicationVersionCodeProvider applicationVersionCodeProvider;
  @Inject AdsRepository adsRepository;
  @Inject SyncStorage syncStorage;
  @Inject NavigationTracker navigationTracker;
  @Inject NewFeature newFeature;
  @Inject NewFeatureManager newFeatureManager;
  @Inject ThemeAnalytics themeAnalytics;
  @Inject @Named("mature-pool-v7") BodyInterceptor<BaseBody> accountSettingsBodyInterceptorPoolV7;
  @Inject StoreCredentialsProvider storeCredentials;
  @Inject StoreUtilsProxy storeUtilsProxy;
  @Inject TrendingManager trendingManager;
  @Inject AdultContentAnalytics adultContentAnalytics;
  @Inject NotificationAnalytics notificationAnalytics;
  @Inject SearchSuggestionManager searchSuggestionManager;
  @Inject AnalyticsManager analyticsManager;
  @Inject FirstLaunchAnalytics firstLaunchAnalytics;
  @Inject InvalidRefreshTokenLogoutManager invalidRefreshTokenLogoutManager;
  @Inject RootInstallationRetryHandler rootInstallationRetryHandler;
  @Inject AptoideShortcutManager shortcutManager;
  @Inject SettingsManager settingsManager;
  @Inject InstallManager installManager;
  @Inject @Named("default-followed-stores") List<String> defaultFollowedStores;
  @Inject AdsUserPropertyManager adsUserPropertyManager;
  @Inject OemidProvider oemidProvider;
  @Inject AptoideMd5Manager aptoideMd5Manager;
  @Inject AppsNameExperimentManager appsNameExperimentManager;
  @Inject UpdatesNotificationWorkerFactory updatesNotificationWorkerFactory;
  @Inject UpdatesNotificationManager updatesNotificationManager;
  private LeakTool leakTool;
  private NotificationCenter notificationCenter;
  private FileManager fileManager;
  private NotificationProvider notificationProvider;
  private BehaviorRelay<Map<Integer, Result>> fragmentResultRelay;
  private Map<Integer, Result> fragmentResultMap;
  private BodyInterceptor<BaseBody> accountSettingsBodyInterceptorWebV7;
  private ApplicationComponent applicationComponent;
  private PublishRelay<NotificationInfo> notificationsPublishRelay;
  private NotificationsCleaner notificationsCleaner;
  private NotificationSyncScheduler notificationSyncScheduler;
  private AptoideApplicationAnalytics aptoideApplicationAnalytics;

  public static FragmentProvider getFragmentProvider() {
    return fragmentProvider;
  }

  public static ActivityProvider getActivityProvider() {
    return activityProvider;
  }

  public static DisplayableWidgetMapping getDisplayableWidgetMapping() {
    return displayableWidgetMapping;
  }

  public LeakTool getLeakTool() {
    if (leakTool == null) {
      leakTool = new LeakTool();
    }
    return leakTool;
  }

  @Override public void onCreate() {

    getApplicationComponent().inject(this);
    CrashReport.getInstance()
        .addLogger(new ConsoleLogger());
    Logger.setDBG(ToolboxManager.isDebug(getDefaultSharedPreferences()) || BuildConfig.DEBUG);

    Single.fromCallable(() -> aptoideMd5Manager.calculateMd5Sum())
        .subscribeOn(Schedulers.io())
        .observeOn(AndroidSchedulers.mainThread())
        .subscribe(__ -> {
        }, error -> CrashReport.getInstance()
            .log(error));

    try {
      PRNGFixes.apply();
    } catch (Exception e) {
      CrashReport.getInstance()
          .log(e);
    }

    //
    // call super
    //
    super.onCreate();

    initializeMoPub();

    //
    // execute custom Application onCreate code with time metric
    //

    long initialTimestamp = System.currentTimeMillis();

    getLeakTool().setup(this);

    //
    // hack to set the debug flag active in case of Debug
    //

    fragmentProvider = createFragmentProvider();
    activityProvider = createActivityProvider();
    displayableWidgetMapping = createDisplayableWidgetMapping();

    //
    // do not erase this code. it is useful to figure out when someone forgot to attach an error handler when subscribing and the app
    // is crashing in Rx without a proper stack trace
    //
    //if (BuildConfig.DEBUG) {
    //  RxJavaPlugins.getInstance().registerObservableExecutionHook(new RxJavaStackTracer());
    //}
    analyticsManager.setup();
    UiModeManager uiModeManager = (UiModeManager) getSystemService(Context.UI_MODE_SERVICE);
    aptoideApplicationAnalytics = new AptoideApplicationAnalytics(analyticsManager);

    androidx.work.Configuration configuration =
        new androidx.work.Configuration.Builder().setWorkerFactory(updatesNotificationWorkerFactory)
            .setMinimumLoggingLevel(android.util.Log.DEBUG)
            .build();
    WorkManager.initialize(this, configuration);
    //
    // async app initialization
    // beware! this code could be executed at the same time the first activity is
    // visible
    //
    /**
     * There's not test at the moment
     * TODO change this class in order to accept that there's no test
     * AN-1838
     */
    generateAptoideUuid().andThen(initializeRakamSdk())
        .andThen(initializeSentry())
        .andThen(setUpUpdatesNotification())
        .andThen(setUpAdsUserProperty())
        .andThen(checkAdsUserProperty())
        .andThen(sendAptoideApplicationStartAnalytics(
            uiModeManager.getCurrentModeType() == Configuration.UI_MODE_TYPE_TELEVISION))
        .andThen(setUpFirstRunAnalytics())
        .andThen(setUpAppsNameAbTest())
        .observeOn(Schedulers.computation())
        .andThen(prepareApp(AptoideApplication.this.getAccountManager()).onErrorComplete(err -> {
          // in case we have an error preparing the app, log that error and continue
          CrashReport.getInstance()
              .log(err);
          return true;
        }))
        .andThen(discoverAndSaveInstalledApps())
        .subscribe(() -> { /* do nothing */}, error -> CrashReport.getInstance()
            .log(error));

    initializeFlurry(this, BuildConfig.FLURRY_KEY);

    clearFileCache();

    startNotificationCenter();
    startNotificationCleaner();

    rootAvailabilityManager.isRootAvailable()
        .doOnSuccess(isRootAvailable -> {
          if (isRootAvailable) {
            rootInstallationRetryHandler.start();
          }
        })
        .subscribe(__ -> {
        }, throwable -> throwable.printStackTrace());

    accountManager.accountStatus()
        .map(account -> account.isLoggedIn())
        .distinctUntilChanged()
        .subscribe(isLoggedIn -> aptoideApplicationAnalytics.updateDimension(isLoggedIn));

    long totalExecutionTime = System.currentTimeMillis() - initialTimestamp;
    Logger.getInstance()
        .v(TAG, String.format("onCreate took %d millis.", totalExecutionTime));
    invalidRefreshTokenLogoutManager.start();

    installManager.start();
  }

  private Completable setUpUpdatesNotification() {
    return updatesNotificationManager.setUpNotification();
  }

  private Completable setUpAppsNameAbTest() {
    if (SecurePreferences.isAppsAbTest(
        SecurePreferencesImplementation.getInstance(getApplicationContext(),
            getDefaultSharedPreferences()))) {
      return setUpAbTest().doOnCompleted(() -> SecurePreferences.setAppsAbTest(false,
          SecurePreferencesImplementation.getInstance(getApplicationContext(),
              getDefaultSharedPreferences())))
          .subscribeOn(Schedulers.newThread());
    } else {
      return Completable.complete();
    }
  }

  private Completable setUpAbTest() {
    return appsNameExperimentManager.setUpExperiment();
  }

  private Completable checkAdsUserProperty() {
    return Completable.fromAction(() -> adsUserPropertyManager.start());
  }

  private Completable setUpAdsUserProperty() {
    return idsRepository.getUniqueIdentifier()
        .flatMapCompletable(id -> adsUserPropertyManager.setUp(id))
        .doOnCompleted(() -> Rakam.getInstance()
            .enableForegroundTracking(this));
  }

  private Completable setUpFirstRunAnalytics() {
    return sendAppStartToAnalytics().doOnCompleted(() -> SecurePreferences.setFirstRun(false,
        SecurePreferencesImplementation.getInstance(getApplicationContext(),
            getDefaultSharedPreferences())));
  }

  private Completable initializeSentry() {
    return Completable.fromAction(
        () -> Sentry.init(BuildConfig.SENTRY_DSN_KEY, new AndroidSentryClientFactory(this)));
  }

  private void initializeRakam() {
    RakamClient instance = Rakam.getInstance();

    try {
      instance.initialize(this, new URL(rakamBaseHost), BuildConfig.RAKAM_API_KEY);
    } catch (MalformedURLException e) {
      Logger.getInstance()
          .e(TAG, "error: ", e);
    }
    instance.setDeviceId(idsRepository.getAndroidId());
    instance.trackSessionEvents(true);
    instance.setLogLevel(Log.VERBOSE);
    instance.setEventUploadPeriodMillis(1);
  }

  public void initializeMoPub() {
    SdkConfiguration sdkConfiguration = new SdkConfiguration.Builder(
        BuildConfig.MOPUB_BANNER_50_HOME_PLACEMENT_ID).withAdditionalNetwork(
        AppLovinBaseAdapterConfiguration.class.toString())
        .withMediatedNetworkConfiguration(AppLovinBaseAdapterConfiguration.class.toString(),
            getMediatedNetworkConfigurationBaseMap(BuildConfig.MOPUB_BANNER_50_HOME_PLACEMENT_ID))
        .withAdditionalNetwork(InMobiBaseAdapterConfiguration.class.getName())
        .withMediatedNetworkConfiguration(InMobiBaseAdapterConfiguration.class.toString(),
            getMediatedNetworkConfigurationBaseMap(BuildConfig.MOPUB_BANNER_50_HOME_PLACEMENT_ID))
        .withAdditionalNetwork(InneractiveAdapterConfiguration.class.getName())
        .withMediatedNetworkConfiguration(InneractiveAdapterConfiguration.class.getName(),
            getMediatedNetworkConfigurationWithAppIdMap(
                BuildConfig.MOPUB_BANNER_50_HOME_PLACEMENT_ID,
                BuildConfig.MOPUB_FYBER_APPLICATION_ID))
        .withAdditionalNetwork(AppnextBaseAdapterConfiguration.class.toString())
        .withMediatedNetworkConfiguration(AppnextBaseAdapterConfiguration.class.toString(),
            getMediatedNetworkConfigurationBaseMap(
                BuildConfig.MOPUB_BANNER_50_EXCLUSIVE_PLACEMENT_ID))
        .withAdditionalNetwork(GooglePlayServicesAdapterConfiguration.class.getName())
        .withMediatedNetworkConfiguration(GooglePlayServicesAdapterConfiguration.class.getName(),
            getAdMobAdsPreferencesMap())
        .withLogLevel(MoPubLog.LogLevel.DEBUG)
        .build();

    MoPub.initializeSdk(this, sdkConfiguration, null);
  }

  @NonNull private Map<String, String> getMediatedNetworkConfigurationBaseMap(
      String mediatedNetworkPlacementId) {
    Map<String, String> mediationNetworkConfiguration = new HashMap<>();
    mediationNetworkConfiguration.put("Placement_Id", mediatedNetworkPlacementId);
    return mediationNetworkConfiguration;
  }

  @NonNull private Map<String, String> getMediatedNetworkConfigurationWithAppIdMap(
      String mediatedNetworkPlacementId, String appId) {
    Map<String, String> mediationNetworkConfiguration =
        getMediatedNetworkConfigurationBaseMap(mediatedNetworkPlacementId);
    mediationNetworkConfiguration.put("appID", appId);
    return mediationNetworkConfiguration;
  }

  private Map<String, String> getAdMobAdsPreferencesMap() {
    HashMap<String, String> result = new HashMap<>();
    result.put("npa", "1");
    return result;
  }

  public ApplicationComponent getApplicationComponent() {
    if (applicationComponent == null) {
      applicationComponent = DaggerApplicationComponent.builder()
          .applicationModule(new ApplicationModule(this))
          .flavourApplicationModule(new FlavourApplicationModule(this))
          .build();
    }
    return applicationComponent;
  }

  /**
   * <p>Needs to be here, to be mocked for tests. Should be on BaseActivity if there were no
   * tests</p>
   *
   * @return Returns a new Activity Module for the Activity Component
   */
  public ActivityModule getActivityModule(BaseActivity activity, Intent intent,
      NotificationSyncScheduler notificationSyncScheduler, View view, boolean firstCreated,
      String fileProviderAuthority) {

    return new ActivityModule(activity, intent, notificationSyncScheduler, view, firstCreated,
        fileProviderAuthority);
  }

  /**
   * Needs to be here, to be mocked for tests. Should be on BaseFragment if there were no tests
   *
   * @return Returns a new Fragment Module for the Fragment Component
   */
  public FragmentModule getFragmentModule(BaseFragment baseFragment, Bundle savedInstanceState,
      Bundle arguments, boolean createStoreUserPrivacyEnabled, String packageName) {
    return new FragmentModule(baseFragment, savedInstanceState, arguments,
        createStoreUserPrivacyEnabled, packageName);
  }

  @Override protected void attachBaseContext(Context base) {
    super.attachBaseContext(base);
    MultiDex.install(this);
  }

  public TokenInvalidator getTokenInvalidator() {
    return tokenInvalidator;
  }

  private void startNotificationCenter() {
    getPreferences().getBoolean(CAMPAIGN_SOCIAL_NOTIFICATIONS_PREFERENCE_VIEW_KEY, true)
        .first()
        .subscribe(enabled -> getNotificationSyncScheduler().setEnabled(enabled),
            throwable -> CrashReport.getInstance()
                .log(throwable));

    getNotificationCenter().setup();
  }

  private void startNotificationCleaner() {
    getNotificationCleaner().setup();
  }

  private NotificationsCleaner getNotificationCleaner() {
    if (notificationsCleaner == null) {
      notificationsCleaner = new NotificationsCleaner(notificationPersistence,
          Calendar.getInstance(TimeZone.getTimeZone("UTC")), accountManager,
          getNotificationProvider(), CrashReport.getInstance());
    }
    return notificationsCleaner;
  }

  public String getCachePath() {
    return Environment.getExternalStorageDirectory()
        .getAbsolutePath() + "/.aptoide/";
  }

  public String getFeedbackEmail() {
    return "support@aptoide.com";
  }

  public String getAccountType() {
    return BuildConfig.APPLICATION_ID;
  }

  public String getExtraId() {
    return null;
  }

  public boolean isCreateStoreUserPrivacyEnabled() {
    return true;
  }

  @NonNull protected abstract SystemNotificationShower getSystemNotificationShower();

  public PublishRelay<NotificationInfo> getNotificationsPublishRelay() {
    if (notificationsPublishRelay == null) {
      notificationsPublishRelay = PublishRelay.create();
    }
    return notificationsPublishRelay;
  }

  public NotificationCenter getNotificationCenter() {
    if (notificationCenter == null) {
      final NotificationProvider notificationProvider = getNotificationProvider();
      notificationCenter =
          new NotificationCenter(notificationProvider, getNotificationSyncScheduler(),
              new NotificationPolicyFactory(notificationProvider),
              new NotificationAnalytics(new AptoideInstallParser(), analyticsManager,
                  navigationTracker));
    }
    return notificationCenter;
  }

  public NotificationProvider getNotificationProvider() {
    if (notificationProvider == null) {
      notificationProvider = new NotificationProvider(
          new RoomNotificationPersistence(aptoideDatabase.notificationDao()), Schedulers.io());
    }
    return notificationProvider;
  }

  public NotificationSyncScheduler getNotificationSyncScheduler() {
    if (notificationSyncScheduler == null) {
      notificationSyncScheduler = new NotificationSyncManager(getAlarmSyncScheduler(), true,
          new NotificationSyncFactory(new NotificationService(BuildConfig.APPLICATION_ID,
              new OkHttpClient.Builder().readTimeout(45, TimeUnit.SECONDS)
                  .writeTimeout(45, TimeUnit.SECONDS)
                  .addInterceptor(new Pnp1AuthorizationInterceptor(getAuthenticationPersistence(),
                      getTokenInvalidator()))
                  .build(), WebService.getDefaultConverter(), getIdsRepository(),
              BuildConfig.VERSION_NAME, getExtraId(), getDefaultSharedPreferences(), getResources(),
              getAccountManager()), getNotificationProvider()));
    }
    return notificationSyncScheduler;
  }

  public SharedPreferences getDefaultSharedPreferences() {
    return PreferenceManager.getDefaultSharedPreferences(this);
  }

  public OkHttpClient getDefaultClient() {
    return defaultClient;
  }

  public AptoideDownloadManager getDownloadManager() {
    return aptoideDownloadManager;
  }

  public InstallManager getInstallManager() {
    return installManager;
  }

  public QManager getQManager() {
    return qManager;
  }

  public AptoideAccountManager getAccountManager() {
    return accountManager;
  }

  public AuthenticationPersistence getAuthenticationPersistence() {
    return authenticationPersistence;
  }

  public Preferences getPreferences() {
    return preferences;
  }

  public PackageRepository getPackageRepository() {
    return packageRepository;
  }

  private void clearFileCache() {
    getFileManager().purgeCache()
        .subscribe(cleanedSize -> Logger.getInstance()
                .d(TAG, "cleaned size: " + AptoideUtils.StringU.formatBytes(cleanedSize, false)),
            err -> CrashReport.getInstance()
                .log(err));
  }

  public FileManager getFileManager() {
    if (fileManager == null) {
      fileManager = new FileManager(cacheHelper, new FileUtils(), new String[] {
          getApplicationContext().getCacheDir().getPath(), getCachePath()
      }, aptoideDownloadManager, httpClientCache);
    }
    return fileManager;
  }

  private void initializeFlurry(Context context, String flurryKey) {
    new FlurryAgent.Builder().withLogEnabled(false)
        .build(context, flurryKey);
  }

  private Completable sendAptoideApplicationStartAnalytics(boolean isTv) {
    return Completable.fromAction(() -> {
      aptoideApplicationAnalytics.setPackageDimension(getPackageName());
      aptoideApplicationAnalytics.setVersionCodeDimension(getVersionCode());
      aptoideApplicationAnalytics.sendIsTvEvent(isTv);
    });
  }

  private Completable sendAppStartToAnalytics() {
    return firstLaunchAnalytics.sendAppStart(this,
        SecurePreferencesImplementation.getInstance(getApplicationContext(),
            getDefaultSharedPreferences()), idsRepository);
  }

  protected DisplayableWidgetMapping createDisplayableWidgetMapping() {
    return DisplayableWidgetMapping.getInstance();
  }

  private Completable generateAptoideUuid() {
    return Completable.fromAction(() -> idsRepository.getUniqueIdentifier());
  }

  private Completable initializeRakamSdk() {
    return Completable.fromAction(() -> initializeRakam())
        .subscribeOn(Schedulers.newThread());
  }

  private Completable prepareApp(AptoideAccountManager accountManager) {
    if (SecurePreferences.isFirstRun(
        SecurePreferencesImplementation.getInstance(getApplicationContext(),
            getDefaultSharedPreferences()))) {

      setSharedPreferencesValues();

      return setupFirstRun().andThen(rootAvailabilityManager.updateRootAvailability())
          .andThen(Completable.merge(accountManager.updateAccount(), createShortcut()));
    }

    return Completable.complete();
  }

  private void setSharedPreferencesValues() {
    PreferencesXmlParser preferencesXmlParser = new PreferencesXmlParser();

    XmlResourceParser parser = getResources().getXml(R.xml.settings);
    try {
      List<String[]> parsedPrefsList = preferencesXmlParser.parse(parser);
      for (String[] keyValue : parsedPrefsList) {
        getDefaultSharedPreferences().edit()
            .putBoolean(keyValue[0], Boolean.valueOf(keyValue[1]))
            .apply();
      }
    } catch (IOException | XmlPullParserException e) {
      e.printStackTrace();
    }
  }

  // todo re-factor all this code to proper Rx
  private Completable setupFirstRun() {
    return Completable.defer(() -> generateAptoideUuid().andThen(
        setDefaultFollowedStores(storeCredentials, storeUtilsProxy).andThen(refreshUpdates())
            .doOnError(err -> CrashReport.getInstance()
                .log(err))));
  }

  private Completable setDefaultFollowedStores(StoreCredentialsProvider storeCredentials,
      StoreUtilsProxy proxy) {

    return Observable.from(defaultFollowedStores)
        .flatMapCompletable(followedStoreName -> {
          BaseRequestWithStore.StoreCredentials defaultStoreCredentials =
              storeCredentials.get(followedStoreName);

          return proxy.addDefaultStore(
              GetStoreMetaRequest.of(defaultStoreCredentials, accountSettingsBodyInterceptorPoolV7,
                  defaultClient, WebService.getDefaultConverter(), tokenInvalidator,
                  getDefaultSharedPreferences()), accountManager, defaultStoreCredentials);
        })
        .toCompletable();
  }

  /**
   * BaseBodyInterceptor for v7 ws calls with CDN = pool configuration
   */
  public BodyInterceptor<BaseBody> getBodyInterceptorPoolV7() {
    return bodyInterceptorPoolV7;
  }

  public BodyInterceptor<BaseBody> getAccountSettingsBodyInterceptorPoolV7() {
    return accountSettingsBodyInterceptorPoolV7;
  }

  public BodyInterceptor<BaseBody> getAccountSettingsBodyInterceptorWebV7() {
    if (accountSettingsBodyInterceptorWebV7 == null) {
      accountSettingsBodyInterceptorWebV7 =
          new MatureBodyInterceptorV7(bodyInterceptorWebV7, adultContent);
    }
    return accountSettingsBodyInterceptorWebV7;
  }

  public BodyInterceptor<cm.aptoide.pt.dataprovider.ws.v3.BaseBody> getBodyInterceptorV3() {
    return bodyInterceptorV3;
  }

  protected String getAptoidePackage() {
    return BuildConfig.APPLICATION_ID;
  }

  public Completable createShortcut() {
    return Completable.defer(() -> {
      if (shortcutManager.shouldCreateShortcut()) {
        createAppShortcut();
      }
      return null;
    });
  }

  private Completable discoverAndSaveInstalledApps() {
    return Observable.fromCallable(() -> {
      // get the installed apps
      List<PackageInfo> installedApps =
          AptoideUtils.SystemU.getAllInstalledApps(getPackageManager());
      Logger.getInstance()
          .v(TAG, "Found " + installedApps.size() + " user installed apps.");

      // Installed apps are inserted in database based on their firstInstallTime. Older comes first.
      Collections.sort(installedApps,
          (lhs, rhs) -> (int) ((lhs.firstInstallTime - rhs.firstInstallTime) / 1000));

      // return sorted installed apps
      return installedApps;
    })  // transform installation package into Installed table entry and save all the data
        .flatMapIterable(list -> list)
        .map(packageInfo -> new RoomInstalled(packageInfo, getPackageManager()))
        .toList()
        .flatMap(appsInstalled -> roomInstalledPersistence.getAll()
            .first()
            .map(installedFromDatabase -> combineLists(appsInstalled, installedFromDatabase,
                installed -> installed.setStatus(RoomInstalled.STATUS_UNINSTALLED))))
        .flatMapCompletable(list -> roomInstalledPersistence.replaceAllBy(list))
        .toCompletable();
  }

  public <T> List<T> combineLists(List<T> list1, List<T> list2, @Nullable Action1<T> transformer) {
    List<T> toReturn = new ArrayList<>(list1.size() + list2.size());
    toReturn.addAll(list1);
    for (T item : list2) {
      if (!toReturn.contains(item)) {
        if (transformer != null) {
          transformer.call(item);
        }
        toReturn.add(item);
      }
    }

    return toReturn;
  }

  private Completable refreshUpdates() {
    return updateRepository.sync(true, false);
  }

  private void createAppShortcut() {
    Intent shortcutIntent = new Intent(this, MainActivity.class);
    shortcutIntent.setAction(Intent.ACTION_MAIN);
    Intent intent = new Intent();
    intent.putExtra(Intent.EXTRA_SHORTCUT_INTENT, shortcutIntent);
    intent.putExtra(Intent.EXTRA_SHORTCUT_NAME, getResources().getString(R.string.app_name));
    intent.putExtra(Intent.EXTRA_SHORTCUT_ICON_RESOURCE,
        Intent.ShortcutIconResource.fromContext(getApplicationContext(), R.mipmap.ic_launcher));
    intent.setAction("com.android.launcher.action.INSTALL_SHORTCUT");
    getApplicationContext().sendBroadcast(intent);
  }

  public RootAvailabilityManager getRootAvailabilityManager() {
    return rootAvailabilityManager;
  }

  public AdsApplicationVersionCodeProvider getVersionCodeProvider() {
    return applicationVersionCodeProvider;
  }

  public AdsRepository getAdsRepository() {
    return adsRepository;
  }

  public SyncStorage getSyncStorage() {
    return syncStorage;
  }

  public BehaviorRelay<Map<Integer, Result>> getFragmentResultRelay() {
    if (fragmentResultRelay == null) {
      fragmentResultRelay = BehaviorRelay.create();
    }
    return fragmentResultRelay;
  }

  @SuppressLint("UseSparseArrays") public Map<Integer, Result> getFragmentResultMap() {
    if (fragmentResultMap == null) {
      fragmentResultMap = new HashMap<>();
    }
    return fragmentResultMap;
  }

  public NavigationTracker getNavigationTracker() {
    return navigationTracker;
  }

  public NewFeatureManager getNewFeatureManager() {
    return newFeatureManager;
  }

  public NewFeature getNewFeature() {
    return newFeature;
  }

  public ThemeAnalytics getThemeAnalytics() {
    return themeAnalytics;
  }

  public FragmentProvider createFragmentProvider() {
    return new VanillaFragmentProvider();
  }

  public ActivityProvider createActivityProvider() {
    return new VanillaActivityProvider();
  }

  public String getVersionCode() {
    String version = "NaN";
    try {
      PackageInfo pInfo = getPackageManager().getPackageInfo(getPackageName(), 0);
      version = String.valueOf(pInfo.versionCode);
    } catch (PackageManager.NameNotFoundException e) {

    }
    return version;
  }

  public String getPartnerId() {
    return oemidProvider.getOemid();
  }

  public SyncScheduler getAlarmSyncScheduler() {
    return alarmSyncScheduler;
  }

  public TrendingManager getTrendingManager() {
    return trendingManager;
  }

  public NotificationAnalytics getNotificationAnalytics() {
    return notificationAnalytics;
  }

  public IdsRepository getIdsRepository() {
    return idsRepository;
  }

  public SearchSuggestionManager getSearchSuggestionManager() {
    return searchSuggestionManager;
  }

  public AnalyticsManager getAnalyticsManager() {
    return analyticsManager;
  }

  public AdultContentAnalytics getAdultContentAnalytics() {
    return adultContentAnalytics;
  }

  public SettingsManager getSettingsManager() {
    return settingsManager;
  }

  public StoreCredentialsProvider getStoreCredentials() {
    return storeCredentials;
  }
}

