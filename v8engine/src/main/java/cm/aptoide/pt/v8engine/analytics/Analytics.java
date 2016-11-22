package cm.aptoide.pt.v8engine.analytics;

import android.content.Intent;
import android.content.SharedPreferences;
import android.content.pm.PackageManager;
import android.os.Bundle;
import android.preference.PreferenceManager;
import android.text.TextUtils;
import cm.aptoide.accountmanager.AptoideAccountManager;
import cm.aptoide.accountmanager.Constants;
import cm.aptoide.pt.dataprovider.DataProvider;
import cm.aptoide.pt.dataprovider.repository.IdsRepositoryImpl;
import cm.aptoide.pt.logger.Logger;
import cm.aptoide.pt.model.v7.GetAppMeta;
import cm.aptoide.pt.preferences.secure.SecurePreferencesImplementation;
import cm.aptoide.pt.v8engine.BuildConfig;
import cm.aptoide.pt.v8engine.V8Engine;
import com.crashlytics.android.answers.Answers;
import com.crashlytics.android.answers.CustomEvent;
import com.facebook.FacebookSdk;
import com.facebook.appevents.AppEventsLogger;
import com.flurry.android.FlurryAgent;
import com.localytics.android.Localytics;
import java.io.IOException;
import java.io.InputStream;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.HashMap;
import java.util.Map;
import java.util.Random;
import java.util.zip.ZipFile;

import static cm.aptoide.pt.v8engine.analytics.Analytics.AppViewViewedFrom.containsUnwantedValues;
import static cm.aptoide.pt.v8engine.analytics.Analytics.Lifecycle.Application.facebookLogger;

/**
 * Created by neuro on 07-05-2015.f
 * v8 integration by jdandrade on 25-07-2016
 */
public class Analytics {

  // Constantes globais a todos os eventos.
  public static final String ACTION = "Action";
  private static final String TAG = Analytics.class.getSimpleName();
  private static final boolean ACTIVATE_FLURRY = BuildConfig.FLURRY_CONFIGURED;
  private static final int ALL = Integer.MAX_VALUE;
  private static final int LOCALYTICS = 1 << 0;
  private static final int FLURRY = 1 << 1;
  private static final int FABRIC = 1 << 2;
  private static final String[] unwantedValuesList = {
      "ads-highlighted", "apps-group-trending", "apps-group-local-top-apps",
      "timeline-your-friends-installs", "apps-group-latest-applications",
      "apps-group-top-apps-in-this-store", "apps-group-aptoide-publishers",
      "stores-group-top-stores", "stores-group-featured-stores", "reviews-group-reviews",
      "apps-group-top-games", "apps-group-top-stores", "apps-group-featured-stores",
      "apps-group-editors-choice"
  };
  private static boolean ACTIVATE_LOCALYTICS = BuildConfig.LOCALYTICS_CONFIGURED;
  private static boolean isFirstSession;

  public static boolean checkBuildVariant() {
    return BuildConfig.BUILD_TYPE.contains("release") && BuildConfig.FLAVOR.contains("dev");
  }

  /**
   * Verifica se as flags fornecidas constam em accepted.
   *
   * @param flag flags fornecidas
   * @param accepted flags aceitáveis
   * @return true caso as flags fornecidas constem em accepted.
   */
  private static boolean checkAcceptability(int flag, int accepted) {
    if (accepted == LOCALYTICS && !ACTIVATE_LOCALYTICS) {
      Logger.d(TAG, "Localytics Disabled ");
      return false;
    } else if (accepted == FLURRY && !ACTIVATE_FLURRY) {
      Logger.d(TAG, "Flurry Disabled");
      return false;
    } else {
      return (flag & accepted) == accepted;
    }
  }

  private static void track(String event, String key, String attr, int flags) {

    try {
      if (!ACTIVATE_LOCALYTICS && !ACTIVATE_FLURRY) return;

      HashMap stringObjectHashMap = new HashMap<>();

      stringObjectHashMap.put(key, attr);

      track(event, stringObjectHashMap, flags);

      Logger.d(TAG, "Event: " + event + ", Key: " + key + ", attr: " + attr);
    } catch (Exception e) {
      Logger.d(TAG, e.getStackTrace().toString());
    }
  }

  private static void track(String event, HashMap map, int flags) {
    try {
      if (!ACTIVATE_LOCALYTICS && !ACTIVATE_FLURRY) {
        return;
      }
      if (checkAcceptability(flags, LOCALYTICS)) {
        Localytics.tagEvent(event, map);
        Logger.d(TAG, "Localytics Event: " + event + ", Map: " + map);
      }

      if (checkAcceptability(flags, FLURRY)) {
        FlurryAgent.logEvent(event, map);
        Logger.d(TAG, "Flurry Event: " + event + ", Map: " + map);
      }
    } catch (Exception e) {
      Logger.d(TAG, e.getStackTrace().toString());
    }
  }

  private static void logFacebookEvents(String eventName, Bundle parameters) {
    if (BuildConfig.BUILD_TYPE.equals("debug")) {
      return;
    }
    facebookLogger.logEvent(eventName, parameters);
  }

  private static void logFabricEvent(String event, Map<String, String> map, int flags) {
    if (checkAcceptability(flags, FABRIC)) {
      CustomEvent customEvent = new CustomEvent(event);
      for (Map.Entry<String, String> entry : map.entrySet()) {
        customEvent.putCustomAttribute(entry.getKey(), entry.getValue());
      }
      Answers.getInstance().logCustom(customEvent);
      Logger.d(TAG, "Fabric Event: " + event + ", Map: " + map);
    }
  }

  private static void track(String event, int flags) {

    try {
      if (!ACTIVATE_LOCALYTICS && !ACTIVATE_FLURRY) return;

      if (checkAcceptability(flags, LOCALYTICS)) Localytics.tagEvent(event);

      if (checkAcceptability(flags, FLURRY)) FlurryAgent.logEvent(event);

      Logger.d(TAG, "Event: " + event);
    } catch (Exception e) {
      e.printStackTrace();
    }
  }

  public static class Lifecycle {

    public static class Application {

      static AppEventsLogger facebookLogger;

      public static void onCreate(android.app.Application application) {

        SharedPreferences sPref =
            PreferenceManager.getDefaultSharedPreferences(application.getBaseContext());
        ACTIVATE_LOCALYTICS =
            ACTIVATE_LOCALYTICS && (sPref.getBoolean(Constants.IS_LOCALYTICS_ENABLE_KEY, false));
        isFirstSession = sPref.getBoolean(Constants.IS_LOCALYTICS_FIRST_SESSION, false);
        if (!ACTIVATE_LOCALYTICS && !isFirstSession) {
          return;
        }

        // Integrate Localytics
        Localytics.autoIntegrate(application);
        setupDimensions();

        //Integrate FacebookSDK
        FacebookSdk.sdkInitialize(application);
        AppEventsLogger.activateApp(application);
        facebookLogger = AppEventsLogger.newLogger(application);

        Logger.d(TAG, "Localytics session configured");
      }

      private static void setupDimensions() {
        if (!checkForUTMFileInMetaINF()) {
          Dimensions.setUTMDimensionsToUnknown();
        }

        if (isFirstSession && !ACTIVATE_LOCALYTICS) {
          Dimensions.setSamplingTypeDimension("90% sampling");
        } else {
          Dimensions.setSamplingTypeDimension("Full-tracking");
        }
      }

      private static boolean checkForUTMFileInMetaINF() {
        ZipFile myZipFile = null;
        try {
          final String sourceDir = V8Engine.getContext()
              .getPackageManager()
              .getPackageInfo(V8Engine.getContext().getPackageName(), 0).applicationInfo.sourceDir;
          myZipFile = new ZipFile(sourceDir);
          final InputStream utmInputStream =
              myZipFile.getInputStream(myZipFile.getEntry("META-INF/utm"));

          UTMFileParser utmFileParser = new UTMFileParser(utmInputStream);
          myZipFile.close();

          String utmSource = utmFileParser.valueExtracter(UTMFileParser.UTM_SOURCE);
          String utmMedium = utmFileParser.valueExtracter(UTMFileParser.UTM_MEDIUM);
          String utmCampaign = utmFileParser.valueExtracter(UTMFileParser.UTM_CAMPAIGN);
          String utmContent = utmFileParser.valueExtracter(UTMFileParser.UTM_CONTENT);
          String entryPoint = utmFileParser.valueExtracter(UTMFileParser.ENTRY_POINT);

          if (!utmSource.isEmpty()) {
            Analytics.Dimensions.setUTMSource(utmSource);
          }

          if (!utmMedium.isEmpty()) {
            Analytics.Dimensions.setUTMMedium(utmMedium);
          }

          if (!utmCampaign.isEmpty()) {
            Analytics.Dimensions.setUTMCampaign(utmCampaign);
          }

          if (!utmContent.isEmpty()) {
            Analytics.Dimensions.setUTMContent(utmContent);
          }

          if (!entryPoint.isEmpty()) {
            Analytics.Dimensions.setEntryPointDimension(entryPoint);
          }

          utmInputStream.close();
        } catch (IOException e) {
          Logger.d(TAG, "problem parsing utm/no utm file");
          return false;
        } catch (PackageManager.NameNotFoundException e) {
          Logger.d(TAG, "No package name utm file.");
          return false;
        } catch (NullPointerException e) {
          if (myZipFile != null) {
            try {
              myZipFile.close();
            } catch (IOException e1) {
              e1.printStackTrace();
              return false;
            }
            return false;
          }
          Logger.d(TAG, "No utm file.");
        }
        return true;
      }
    }

    public static class Activity {

      public static void onCreate(android.app.Activity activity) {

        if (!ACTIVATE_LOCALYTICS) return;
        Localytics.registerPush(BuildConfig.GOOGLE_SENDER_ID);
      }

      public static void onDestroy(android.app.Activity activity) {

        if (!ACTIVATE_LOCALYTICS) return;
      }

      public static void onResume(android.app.Activity activity) {

        if (!ACTIVATE_LOCALYTICS) return;

        Localytics.onActivityResume(activity);

        if (isFirstSession) {
          if (!AptoideAccountManager.isLoggedIn()) {
            Localytics.setCustomDimension(0, "Not Logged In");
          } else {
            Localytics.setCustomDimension(0, "Logged In");
          }
        }

        IdsRepositoryImpl idsRepository =
            new IdsRepositoryImpl(SecurePreferencesImplementation.getInstance(),
                DataProvider.getContext());

        String cpuid = idsRepository.getAptoideClientUUID();

        Localytics.setCustomerId(cpuid);

        //                String cpuid = PreferenceManager.getDefaultSharedPreferences(Aptoide.getContext())
        //                        .getString(EnumPreferences.APTOIDE_CLIENT_UUID.name(), "NoInfo");

        //                Localytics.setCustomerId(cpuid);
        //
        //                if (screenName != null) {
        //                    Localytics.tagScreen(screenName);
        //                }
        //
        Localytics.handleTestMode(activity.getIntent());
        //
        //                Logger.d("Analytics", "Event: CPU_ID: " + cpuid);
        //                Logger.d("Analytics", "Screen: " + screenName);

      }

      public static void onPause(android.app.Activity activity) {
        if (!ACTIVATE_LOCALYTICS && !isFirstSession) return;

        Localytics.onActivityPaused(activity);
      }

      public static void onStart(android.app.Activity activity) {

        if (!ACTIVATE_FLURRY) return;

        Logger.d(TAG, "FlurryAgent.onStartSession called");
        FlurryAgent.onStartSession(activity, BuildConfig.FLURRY_KEY);
      }

      public static void onStop(android.app.Activity activity) {

        if (!ACTIVATE_FLURRY) return;

        Logger.d(TAG, "FlurryAgent.onEndSession called");
        FlurryAgent.onEndSession(activity);
      }

      public static void onNewIntent(android.app.Activity activity, Intent intent) {
        if (!ACTIVATE_LOCALYTICS && !isFirstSession) {
          return;
        }
        Localytics.onNewIntent(activity, intent);
      }
    }
  }

  public static class Screens {

    public static void tagScreen(String screenName) {

      if (!ACTIVATE_LOCALYTICS) return;

      Logger.d(TAG, "Localytics: Screens: " + screenName);

      Localytics.tagScreen(screenName);
      Localytics.upload();
    }
  }

  // TODO
  public static class Tutorial {
    public static final String EVENT_NAME = "Tutorial";
    public static final String STEP_ACCOMPLISHED = "Step Accomplished";

    public static void finishedTutorial(int lastFragment) {
      try {
        track(EVENT_NAME, STEP_ACCOMPLISHED, Integer.toString(lastFragment), ALL);
      } catch (NullPointerException e) {
        e.printStackTrace();
      }
    }
  }

  public static class UserRegister {

    public static final String EVENT_NAME = "User Registered";

    public static void registered() {
      track(EVENT_NAME, ALL);
    }
  }

  // Novos
  public static class Rollback {

    private static final String EVENT_NAME = "Rollback";
    private static final String EVENT_NAME_DOWNGRADE_DIALOG = "Downgrade_Dialog";
    private static final String DOWNGRADED = "Downgraded";
    private static final String CLEAR = "Clear";

    public static void downgraded() {
      //            track(EVENT_NAME, ACTION, DOWNGRADED, ALL);
    }

    public static void downgradeDialogContinue() {
      track(EVENT_NAME_DOWNGRADE_DIALOG, ACTION, "Continue", FLURRY);
    }

    public static void downgradeDialogCancel() {
      track(EVENT_NAME_DOWNGRADE_DIALOG, ACTION, "Cancel", FLURRY);
    }

    public static void clear() {
      //            track(EVENT_NAME, ACTION, CLEAR, ALL);
    }
  }

  public static class ScheduledDownloads {
    public static final String EVENT_NAME = "Scheduled Downloads";
    private static final String CLICK_ON_INSTALL_SELECTED = "Clicked on Install Selected";
    private static final String CLICK_ON_INVERT_SELECTION = "Clicked on Invert Selection";
    private static final String CLICK_ON_REMOVE_SELECTED = "Clicked on Remove Selected";

    public static void clickOnInstallSelected() {
      //            track(EVENT_NAME, ACTION, CLICK_ON_INSTALL_SELECTED, ALL);
    }

    public static void clickOnInvertSelection() {
      //            track(EVENT_NAME, ACTION, CLICK_ON_INVERT_SELECTION, ALL);
    }

    public static void clickOnRemoveSelected() {
      //            track(EVENT_NAME, ACTION, CLICK_ON_REMOVE_SELECTED, ALL);
    }
  }

  public static class SendFeedback {

    public static final String EVENT_NAME = "Send Feedback";
    private static final String SEND_FEEDBACK = EVENT_NAME;

    public static void sendFeedback() {
      track(EVENT_NAME, ACTION, SEND_FEEDBACK, ALL);
    }
  }

  public static class ExcludedUpdates {
    private static final String EVENT_NAME = "Excluded Updates";
    private static final String RESTORE_UPDATES = "Restore Updates";

    public static void restoreUpdates() {
      track(EVENT_NAME, ACTION, RESTORE_UPDATES, ALL);
    }
  }

  /**
   * Incomplete
   */
  public static class Settings {

    public static final String EVENT_NAME = "Settings";
    private static final String CHECKED = "Checked";

    public static void onSettingChange(String s) {
      //            track(EVENT_NAME, ACTION, s, ALL);
    }

    public static void onSettingChange(String s, boolean checked) {
      //            track(EVENT_NAME, ACTION, s, ALL);
      //
      //            HashMap<String, String> objectObjectHashMap = new HashMap<>();
      //            objectObjectHashMap.put(ACTION, s);
      //            objectObjectHashMap.put(CHECKED, Boolean.valueOf(checked).toString());
    }
  }

  public static class Facebook {

    public static final String EVENT_NAME = "Facebook";

    public static final String JOIN = "Join";
    public static final String LOGIN = "Login";

    public static void join() {
      track(EVENT_NAME, ACTION, JOIN, ALL);
    }

    public static void login() {
      track(EVENT_NAME, ACTION, LOGIN, ALL);
    }
  }

  public static class BackupApps {
    public static final String EVENT_NAME = "Opened Backup Apps";

    public static void open() {
      track(EVENT_NAME, ALL);
    }
  }

  public static class Home {
    public static final String EVENT_NAME = "Home";

    public static final String CLICK_ON_MORE_ = "Click on More ";
    public static final String CLICK_ON_EDITORS_CHOISE = "Click On Editor's Choise";
    public static final String CLICK_ON_HIGHLIGHTED = "Click On Highlighted";
    public static final String CLICK_ON_HIGHLIGHTED_MORE = "Click On Highlighted More";
    public static final String CLICK_ON_APPLICATIONS = "Click On Applications";
    public static final String CLICK_ON_APPLICATIONS_MORE = "Click On Applications More";
    public static final String CLICK_ON_GAMES = "Click On Games";
    public static final String CLICK_ON_GAMES_MORE = "Click On Games More";
    public static final String CLICK_ON_REVIEWS = "Click On Reviews";
    public static final String CLICK_ON_REVIEWS_MORE = "Click On Reviews More";
    public static final String CLICK_ON_PUBLISHERS = "Click On Publishers";
    public static final String CLICK_ON_PUBLISHERS_MORE = "Click On Publishers More";
    public static final String CLICK_ON_APPS_ESSENTIALS = "Click On Apps Essentials";
    public static final String CLICK_ON_APPS_FOR_KIDS = "Click On Apps For Kids";

    public static void clickOnHighlighted() {
      //            track(EVENT_NAME, ACTION, CLICK_ON_HIGHLIGHTED_MORE, ALL);
    }

    public static void generic(String s) {
      track(EVENT_NAME, ACTION, s, ALL);
    }

    //        public static void clickOnApplicationsMore() {
    //            track(EVENT_NAME, ACTION, CLICK_ON_APPLICATIONS_MORE, ALL);
    //        }

    public static void clickOnReviewsMore() {
      //            track(EVENT_NAME, ACTION, CLICK_ON_REVIEWS_MORE, ALL);
    }

    public static void clickOnMoreWidget(String widgetname) {
      //            track(EVENT_NAME, ACTION, CLICK_ON_MORE_ + widgetname, ALL);
    }
  }

  public static class AdultContent {

    public static final String EVENT_NAME = "Adult Content";

    public static final String UNLOCK = "false";
    public static final String LOCK = "true";

    public static void lock() {
      track(EVENT_NAME, ACTION, LOCK, FLURRY);
    }

    public static void unlock() {
      track(EVENT_NAME, ACTION, UNLOCK, FLURRY);
    }
  }

  public static class Top {
    public static final String EVENT_NAME = "Top";

    public static final String CLICK_ON_LOCAL_TOP_APPS_MORE = "Click on Local Top Apps More";
    public static final String CLICK_ON_TOP_APPLICATIONS_MORE = "Click on Top Applications More";
    public static final String CLICK_ON_LOCAL_TOP_STORES_MORE = "Click on Local Top Stores More";
  }

  public static class Stores {
    public static final String EVENT_NAME = "Stores";

    public static final String STORE_NAME = "Store Name";

    public static void enter(String storeName) {
      try {
        HashMap<String, String> map = new HashMap<>();

        map.put(ACTION, "Enter");
        map.put(STORE_NAME, storeName);

        track(EVENT_NAME, map, LOCALYTICS);
      } catch (Exception e) {
        e.printStackTrace();
      }
    }

    public static void subscribe(String storeName) {
      try {
        HashMap<String, String> map = new HashMap<>();

        map.put(ACTION, "Subscribe");
        map.put(STORE_NAME, storeName);

        track(EVENT_NAME, map, LOCALYTICS);
      } catch (Exception e) {
        e.printStackTrace();
      }
    }
  }

  public static class Updates {
    public static final String EVENT_NAME = "Updates";

    public static final String CLICKED_ON_CREATE_REVIEW = "Create Review";
    public static final String CLICKED_ON_UPDATE = "Update";
    public static final String CLICKED_ON_UPDATE_ALL = "Update All";

    public static void update() {
      track(EVENT_NAME, ACTION, CLICKED_ON_UPDATE, LOCALYTICS);
    }

    public static void updateAll() {
      track(EVENT_NAME, ACTION, CLICKED_ON_UPDATE_ALL, LOCALYTICS);
    }

    public static void createReview() {
      track(EVENT_NAME, ACTION, CLICKED_ON_CREATE_REVIEW, LOCALYTICS);
    }
  }

  // TODO: Não está implementado na v6
  public static class DownloadManager {
    public static final String EVENT_NAME = "Download Manager";

    public static void clearDownloadComplete() {
      //            track(EVENT_NAME, ACTION, "Clear download complete", ALL);
    }

    public static void clickDownloadComplete() {
      //            track(EVENT_NAME, ACTION, "Click download complete", ALL);
    }

    public static void clearTopMenu() {
      //            track(EVENT_NAME, ACTION, "Clear topmenu", ALL);
    }
  }

  public static class Search {
    //event names
    public static final String EVENT_NAME_SEARCH_TERM = "Search Term";
    public static final String EVENT_NAME_NO_SEARCH_RESULTS = "No Search Result";

    //event attributes
    public static final String QUERY = "Query";

    public static void searchTerm(String query) {
      track(EVENT_NAME_SEARCH_TERM, QUERY, query, LOCALYTICS);
    }

    public static void noSearchResults(String query) {
      track(EVENT_NAME_NO_SEARCH_RESULTS, QUERY, query, ALL);
    }
  }

  public static class ApplicationInstall {
    public static final String EVENT_NAME = "Application Install";

    private static final String TYPE = "Type";
    private static final String PACKAGE_NAME = "Package Name";
    private static final String REFERRED = "Referred";
    private static final String TRUSTED_BADGE = "Trusted Badge";

    private static final String REPLACED = "Replaced";
    private static final String INSTALLED = "Installed";
    private static final String DOWNGRADED_ROLLBACK = "Downgraded Rollback";

    private static void innerTrack(String packageName, String type, String trustedBadge,
        int flags) {
      try {
        HashMap<String, String> stringObjectHashMap = new HashMap<>();

        stringObjectHashMap.put(TRUSTED_BADGE, trustedBadge);
        stringObjectHashMap.put(TYPE, type);
        stringObjectHashMap.put(PACKAGE_NAME, packageName);

        track(EVENT_NAME, stringObjectHashMap, flags);

        Bundle parameters = new Bundle();
        parameters.putString(PACKAGE_NAME, packageName);
        parameters.putString(TRUSTED_BADGE, trustedBadge);
        parameters.putString(TYPE, type);
      } catch (Exception e) {
        e.printStackTrace();
      }
    }

    public static void installed(String packageName, String trustedBadge) {
      innerTrack(packageName, INSTALLED, trustedBadge, ALL);
    }

    public static void replaced(String packageName, String trustedBadge) {
      innerTrack(packageName, REPLACED, trustedBadge, ALL);
    }

    public static void downgraded(String packageName, String trustedBadge) {
      innerTrack(packageName, DOWNGRADED_ROLLBACK, trustedBadge, ALL);
    }
  }

  public static class ApplicationLaunch {

    public static final String EVENT_NAME = "Application Launch";
    public static final String SOURCE = "Source";
    public static final String LAUNCHER = "Launcher";
    public static final String WEBSITE = "Website";
    public static final String NEW_UPDATES_NOTIFICATION = "New Updates Available";
    public static final String DOWNLOADING_UPDATES = "Downloading Updates";
    public static final String TIMELINE_NOTIFICATION = "Timeline Notification";
    public static final String NEW_REPO = "New Repository";
    public static final String URI = "Uri";

    public static void launcher() {
      track(EVENT_NAME, SOURCE, LAUNCHER, LOCALYTICS);
    }

    public static void website(String uri) {
      Logger.d(TAG, "website: " + uri);

      try {
        HashMap<String, String> map = new HashMap<>();
        map.put(SOURCE, WEBSITE);

        if (uri != null) {
          map.put(URI, uri.substring(0, uri.indexOf(":")));
        }

        track(EVENT_NAME, map, ALL);
      } catch (Exception e) {
        e.printStackTrace();
      }
    }

    public static void newUpdatesNotification() {
      track(EVENT_NAME, SOURCE, NEW_UPDATES_NOTIFICATION, ALL);
    }

    public static void downloadingUpdates() {
      track(EVENT_NAME, SOURCE, DOWNLOADING_UPDATES, ALL);
    }

    public static void timelineNotification() {
      track(EVENT_NAME, SOURCE, TIMELINE_NOTIFICATION, ALL);
    }

    public static void newRepo() {
      track(EVENT_NAME, SOURCE, NEW_REPO, ALL);
    }
  }

  public static class ClickedOnInstallButton {
    private static final String EVENT_NAME = "Clicked on install button";

    //attributes
    private static final String APPLICATION_NAME = "Application Name";
    private static final String WARNING = "Warning";
    private static final String APPLICATION_PUBLISHER = "Application Publisher";

    public static void clicked(GetAppMeta.App app) {
      try {
        HashMap<String, String> map = new HashMap<>();

        map.put(APPLICATION_NAME, app.getPackageName());
        map.put(APPLICATION_PUBLISHER, app.getDeveloper().getName());

        track(EVENT_NAME, map, ALL);

        Bundle parameters = new Bundle();
        parameters.putString(APPLICATION_NAME, app.getPackageName());
        parameters.putString(APPLICATION_PUBLISHER, app.getDeveloper().getName());
        logFacebookEvents(EVENT_NAME, parameters);
      } catch (Exception e) {
        e.printStackTrace();
      }
    }
  }

  public static class DownloadComplete {
    public static final String EVENT_NAME = "Download Complete";
    private static final String PACKAGE_NAME = "Package Name";
    private static final String TRUSTED_BADGE = "Trusted Badge";
    private static HashMap<Long, String> applicationsInstallClicked = new HashMap<>();

    public static void installClicked(long id) {
      int homeIndex = AppViewViewedFrom.STEPS.indexOf("home");
      if (homeIndex > 0) {
        applicationsInstallClicked.put(id, AppViewViewedFrom.STEPS.get(homeIndex - 1));
      }
    }

    public static void downloadComplete(GetAppMeta.App app) {
      try {
        HashMap<String, String> map = new HashMap<>();
        String step = applicationsInstallClicked.get(app.getId());
        if (!TextUtils.isEmpty(step)) {
          if (step.equals("apps-group-editors-choice")) {
            map.put("editors package name", app.getPackageName());
          } else {
            map.put("bundle package name", step + "_" + app.getPackageName());
            map.put("bundle category", step);
          }
        }
        map.put(PACKAGE_NAME, app.getPackageName());
        map.put(TRUSTED_BADGE, app.getFile().getMalware().getRank().name());

        if (map.containsKey("Source") && !containsUnwantedValues(map.get("Source"))) {
          track(EVENT_NAME, map, ALL);
        }

        Bundle parameters = new Bundle();
        parameters.putString(PACKAGE_NAME, app.getPackageName());
        parameters.putString(TRUSTED_BADGE, app.getFile().getMalware().getRank().name());
        logFacebookEvents(EVENT_NAME, parameters);
      } catch (Exception e) {
        e.printStackTrace();
      }
    }
  }

  public static class AppsTimeline {

    public static final String EVENT_NAME = "Apps Timeline";
    public static final String ACTION = "Action";
    public static final String PACKAGE_NAME = "Package Name";
    public static final String TITLE = "Title";
    public static final String PUBLISHER = "Publisher";
    public static final String CARD_TYPE = "Card Type";

    public static final String BLANK = "(blank)";
    public static final String OPEN_ARTICLE = "Open Article";
    public static final String OPEN_ARTICLE_HEADER = "Open Article Header";
    public static final String OPEN_VIDEO = "Open Video";
    public static final String OPEN_VIDEO_HEADER = "Open Video Header";
    public static final String OPEN_STORE = "Open Store";
    public static final String OPEN_APP_VIEW = "Open App View";
    public static final String UPDATE_APP = "Update Application";

    public static void clickOnCard(String cardType, String packageName, String title,
        String publisher, String action) {
      HashMap<String, String> map = new HashMap<>();

      map.put(ACTION, action);
      map.put(PACKAGE_NAME, packageName);
      map.put(TITLE, title);
      map.put(PUBLISHER, publisher);

      localyticsTrack(map, cardType);
      flurryTrack(map, cardType);
    }

    public static void pullToRefresh() {
      track("Pull-to-refresh_Apps Timeline", FLURRY);
    }

    public static void endlessScrollLoadMore() {
      track("Endless-scroll_Apps Timeline", FLURRY);
    }

    private static void flurryTrack(HashMap<String, String> map, String cardType) {
      String eventName = cardType + "_" + EVENT_NAME;
      track(eventName, map, FLURRY);
    }

    private static void localyticsTrack(HashMap<String, String> map, String cardType) {
      map.put(CARD_TYPE, cardType);
      track(EVENT_NAME, map, LOCALYTICS);
    }

    public static void openTimeline() {
      track("Open Apps Timeline", FLURRY);
    }
  }

  public static class ViewedApplication {

    public static final String EVENT_NAME = "Viewed Application";

    private static final String APPLICATION_NAME = "Application Name";
    private static final String TYPE = "Type";
    private static final String APPLICATION_PUBLISHER = "Application Publisher";
    private static final String SOURCE = "Source";
    private static final String TRUSTED_BADGE = "Trusted Badge";

    public static void view(String packageName, String trustedBadge) {
      try {
        HashMap<String, String> map = new HashMap<>();

        map.put(APPLICATION_NAME, packageName);
        map.put(TRUSTED_BADGE, trustedBadge);
        //TODO MISSING POP_UP AB TESTING

        track(EVENT_NAME, map, LOCALYTICS);
      } catch (Exception e) {
        e.printStackTrace();
      }
    }
  }

  public static class Dimensions {
    public static final String VERTICAL = V8Engine.getConfiguration().getVerticalDimension();
    public static final String PARTNER = V8Engine.getConfiguration().getPartnerDimension();
    public static final String UNKNOWN = "unknown";
    public static final String APKFY = "Apkfy";
    public static final String WEBSITE = "Website";
    public static final String INSTALLER = "Installer";

    private static void setDimension(int i, String s) {
      if (!ACTIVATE_LOCALYTICS && !isFirstSession) {
        return;
      }

      Logger.d(TAG, "Dimension: " + i + ", Value: " + s);

      Localytics.setCustomDimension(i, s);
    }

    public static void setPartnerDimension(String partner) {
      setDimension(1, partner);
    }

    public static void setVerticalDimension(String verticalName) {
      setDimension(2, verticalName);
    }

    public static void setGmsPresent(boolean b) {
      if (b) {
        setDimension(3, "GMS Present");
      } else {
        setDimension(3, "GMS Not Present");
      }
    }

    public static void setUTMSource(String utmSource) {
      setDimension(4, utmSource);
    }

    public static void setUTMMedium(String utmMedium) {
      setDimension(5, utmMedium);
    }

    public static void setUTMCampaign(String utmCampaign) {
      setDimension(6, utmCampaign);
    }

    public static void setUTMContent(String utmContent) {
      setDimension(7, utmContent);
    }

    public static void setUTMDimensionsToUnknown() {
      setDimension(4, UNKNOWN);
      setDimension(5, UNKNOWN);
      setDimension(6, UNKNOWN);
      setDimension(7, UNKNOWN);
    }

    public static void setSamplingTypeDimension(String samplingType) {
      setDimension(8, samplingType);
    }

    public static void setEntryPointDimension(String entryPoint) {
      setDimension(9, entryPoint);
    }
  }

  public static class AppViewViewedFrom {

    public static final String APP_VIEWED_OPEN_FROM_EVENT_NAME_KEY = "App_Viewed_Open_From";
    public static final int NUMBER_OF_STEPS_TO_RECORD = 5;
    public static final String HOME_SCREEN_STEP = "home";
    private static ArrayList<String> STEPS = new ArrayList<>();
    private static String lastStep;

    public static void appViewOpenFrom(String packageName, String developerName,
        String trustedBadge) {

      Collections.reverse(STEPS);
      if (STEPS.contains(HOME_SCREEN_STEP)) {
        String stringForSourceEvent = formatStepsToSingleEvent(STEPS);
        HashMap<String, String> map = new HashMap<>();
        map.put("Package Name", packageName);
        map.put("Source", stringForSourceEvent);
        map.put("Trusted Badge", trustedBadge);
        map.put("Application Publisher", developerName);
        int index = STEPS.indexOf(HOME_SCREEN_STEP);
        if (index > 0) {
          String source = STEPS.get(index - 1);
          if (source.equals("apps-group-editors-choice")) {
            map.put("editors package name", packageName);
          } else {
            map.put("bundle package name", source + "_" + packageName);
            map.put("bundle category", source);
          }
        }
        Logger.d("teste", "appViewOpenFrom: " + map);

        if (map.containsKey("Source") && !containsUnwantedValues(map.get("Source"))) {
          track(APP_VIEWED_OPEN_FROM_EVENT_NAME_KEY, map, FLURRY);
        }

        Bundle parameters = new Bundle();
        parameters.putString("Package Name", packageName);
        parameters.putString("Source", stringForSourceEvent);
        parameters.putString("Trusted Badge", trustedBadge);
        parameters.putString("Application Publisher", developerName);
        logFacebookEvents(APP_VIEWED_OPEN_FROM_EVENT_NAME_KEY, parameters);
      }
      STEPS.clear();
    }

    protected static boolean containsUnwantedValues(String source) {
      String[] sourceArray = source.split("_");
      for (String step : sourceArray) {
        if (Arrays.asList(unwantedValuesList).contains(step)) {
          return true;
        }
      }
      return false;
    }

    private static String formatStepsToSingleEvent(ArrayList<String> listOfSteps) {
      return TextUtils.join("_", listOfSteps.subList(0, listOfSteps.indexOf(HOME_SCREEN_STEP)));
    }

    public static void addStepToList(String step) {
      if (!TextUtils.isEmpty(step)) {
        STEPS.add(step.replace(" ", "-").toLowerCase());
        Logger.d(TAG, "addStepToList() called with: step = [" + step + "]");
        if (STEPS.size() > NUMBER_OF_STEPS_TO_RECORD) {
          STEPS.remove(0);
        }
        lastStep = step;
      }
    }

    static String getLastStep() {
      return lastStep;
    }
  }

  public static class LTV {
    public static void cpi(String packageName) {
      ltv("CPI Click", packageName);
    }

    //        public static void purchasedApp(String packageName, double revenue) {
    //            ltv("App Purchase", packageName, revenue);
    //        }

    private static void ltv(String eventName, String packageName) {
      if (!ACTIVATE_LOCALYTICS) {
        return;
      }

      try {
        HashMap<String, String> map = new HashMap<>();

        //                Double revenueDouble = Double.valueOf(revenue);
        //                Long value = revenueDouble.longValue();

        map.put("packageName", packageName);

        Logger.d(TAG, "LTV: " + eventName + ": " + packageName);

        Localytics.tagEvent(eventName, map);
      } catch (NumberFormatException e) {
        e.printStackTrace();
      }
    }
  }

  public static class HomePageEditorsChoice {

    public static final String HOME_PAGE_EDITORS_CHOICE = "Home_Page_Editors_Choice";

    public static void clickOnEditorsChoiceItem(int position, String packageName, boolean isHome) {
      HashMap<String, String> map = new HashMap<>();
      map.put("Application Name", packageName);
      if (isHome) {
        map.put("Search Position", "Home_" + Integer.valueOf(position).toString());
      } else {
        map.put("Search Position", "More_" + Integer.valueOf(position).toString());
      }

      track(HOME_PAGE_EDITORS_CHOICE, map, FLURRY);
    }
  }

  public static class LocalyticsSessionControl {
    public static void firstSession(SharedPreferences sPref) {
      SharedPreferences.Editor edit = sPref.edit();
      edit.putBoolean(Constants.IS_LOCALYTICS_FIRST_SESSION, false);
      Logger.d(TAG, "contains" + sPref.contains(Constants.IS_LOCALYTICS_ENABLE_KEY));
      if (!sPref.contains(Constants.IS_LOCALYTICS_ENABLE_KEY)) {
        Random random = new Random();
        int i = random.nextInt(10);
        Logger.d(TAG, "firstSession: " + i);
        edit.putBoolean(Constants.IS_LOCALYTICS_FIRST_SESSION, true);
        edit.putBoolean(Constants.IS_LOCALYTICS_ENABLE_KEY, i == 0);
      }
      edit.apply();
      Logger.d(TAG, "firstSession: IS_LOCALYTICS_FIRST_SESSION: " + sPref.getBoolean(
          Constants.IS_LOCALYTICS_FIRST_SESSION, false));
      Logger.d(TAG, "firstSession: IS_LOCALYTICS_ENABLE_KEY: " + sPref.getBoolean(
          Constants.IS_LOCALYTICS_ENABLE_KEY, false));
    }
  }

  public static class File {

    private static final String EVENT_NAME = "Download_99percent";
    private static final String ATTRIBUTE = "APK";

    public static void moveFile(String moveType) {
      track(EVENT_NAME, ATTRIBUTE, moveType, FLURRY);
    }
  }

  public static class SourceDownloadComplete {
    private static final String PARTIAL_EVENT_NAME = "_Download_Complete";
    private static final String PACKAGE_NAME = "Package Name";
    private static HashMap<Long, String> applicationsInstallClicked = new HashMap<>();

    public static void installClicked(long id) {
      String lastStep = Analytics.AppViewViewedFrom.getLastStep();
      applicationsInstallClicked.put(id, lastStep);
    }

    public static void downloadComplete(long id, String packageName) {
      String lastStep = applicationsInstallClicked.get(id);
      if (!TextUtils.isEmpty(lastStep) && lastStep.contains("editors-choice")) {
        track(lastStep.concat(PARTIAL_EVENT_NAME), PACKAGE_NAME, packageName, FLURRY);
      }
    }
  }

  public static class RootInstall {

    private static final String ROOT_INSTALL_EVENT_NAME = "ROOT_INSTALL";
    private static final String EXIT_CODE = "EXIT_CODE";
    private static final String IS_INSTALLED = "IS_INSTALLED";
    private static final String CONCAT = "CONCAT";
    private static final String IS_ROOT = "IS_ROOT";
    private static final String SETTING_ROOT = "SETTING_ROOT";
    private static final String IS_INSTALLATION_TYPE_EVENT_NAME = "INSTALLATION_TYPE";

    public static void rootInstallCompleted(int exitCode, boolean isInstalled) {
      Map<String, String> map = new HashMap<>();
      map.put(EXIT_CODE, String.valueOf(exitCode));
      map.put(IS_INSTALLED, String.valueOf(isInstalled));
      map.put(CONCAT, String.valueOf(isInstalled) + "_" + exitCode);

      logFabricEvent(ROOT_INSTALL_EVENT_NAME, map, FABRIC);
    }

    public static void installationType(boolean isRootAllowed, boolean isRoot) {
      Map<String, String> map = new HashMap<>();
      map.put(IS_ROOT, String.valueOf(isRoot));
      map.put(SETTING_ROOT, String.valueOf(isRootAllowed));
      map.put(CONCAT, String.valueOf(isRootAllowed) + "_" + String.valueOf(isRoot));

      logFabricEvent(IS_INSTALLATION_TYPE_EVENT_NAME, map, FABRIC);
    }
  }

  public static class AccountEvents {

    public static final String LOGGED_IN_EVENT = "Logged in";
    public static final String ACTION = "Action";
    public static final String USER_REGISTERED = "User Registered";

    public static void login(String action) {
      track(LOGGED_IN_EVENT, ACTION, action, LOCALYTICS);
    }

    public static void signUp() {
      track(USER_REGISTERED, LOCALYTICS);
    }
  }
}
